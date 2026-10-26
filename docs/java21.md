# Java 21 — the LTS that changed how Java code looks

September 2023. The most significant release since Java 8, and for most teams
the real destination after 11 or 17. Three years of previews land as standard on
the same day: virtual threads, pattern matching for switch, and record patterns.
Sequenced collections arrive as a quiet but overdue fix.

---

## 1. Virtual threads (JEP 444)

**Code:** [`jdk21/VirtualThreads`](../src/main/java/com/tomtom/genesis/platform/jdk21/VirtualThreads.java)

### The problem

A platform thread is a thin wrapper over an OS thread: roughly 1 MB of reserved
stack, about a millisecond to create, and a practical ceiling of a few thousand
per machine.

A typical request spends most of its life **blocked** — waiting on a database,
an HTTP call, a queue — holding a megabyte and doing nothing. That is why we
built thread pools, and then adopted reactive and async styles to avoid blocking
at all.

The cost of that escape was the programming model: callbacks, `Flux` and `Mono`,
`CompletableFuture` chains, stack traces that tell you nothing, and debuggers
that cannot follow the logical flow.

Virtual threads make blocking cheap again, so the simple code comes back.

### How it works

```java
Thread.ofVirtual().start(() -> doWork());

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(task);
}
```

Look at a virtual thread's `toString`:

```
VirtualThread[#26]/runnable@ForkJoinPool-1-worker-1
```

The part after the slash is the **carrier** — a platform thread from a
ForkJoinPool that is running this virtual thread right now. When the virtual
thread blocks, its stack is copied to the heap and the carrier is released to
run something else. That copy is the whole trick.

Virtual threads are **always daemon threads**, their priority is ignored, and
they cannot be inserted into a thread group.

### The demonstration

Real output from the example, 100 000 tasks each blocking for 100 ms:

```
tasks completed = 100000
wall clock      = 451 ms
```

With platform threads this needs 100 000 OS threads — around 100 GB of reserved
stack — or a pool, which turns it into 500 sequential rounds of 100 ms. Actual
memory per virtual thread starts at a few hundred bytes and grows with the stack,
because the stack lives on the heap.

### Five habits that become wrong

1. **Do not pool virtual threads.** A pool exists to amortise an expensive
   resource. Virtual threads are cheap, so a pool only reintroduces the limit
   you were escaping. `newVirtualThreadPerTaskExecutor` is not a pool — it
   creates one thread per task and never reuses it.

2. **Limit concurrency, not threads.** You still need to protect a database with
   20 connections. Use a `Semaphore` around the scarce resource instead of a
   small thread pool.

3. **`ThreadLocal` still works, but think about cost.** With a million threads, a
   `ThreadLocal` holding a 1 KB buffer is a gigabyte. Scoped values (Java 25) are
   the intended answer — see [java25.md](java25.md).

4. **They do not make CPU-bound work faster.** They solve blocking, not
   computation. For parallel number crunching, a fixed pool sized to the core
   count is still right.

5. **They are daemon threads**, so the JVM will not stay alive for them.
   Something has to join or await.

### Pinning — the one sharp edge

In Java 21 a virtual thread cannot unmount from its carrier inside a
`synchronized` block or a native (JNI) frame. The carrier stays blocked with it.
Enough pinned threads and you exhaust the carrier pool, which looks exactly like
a deadlock.

Diagnose with `-Djdk.tracePinnedThreads=full`. The Java 21 fix is to replace
`synchronized` with `ReentrantLock` around code that blocks — a lock does not pin.

**Important update:** Java 24 (JEP 491) removed the `synchronized` limitation.
From 24 onwards a virtual thread unmounts inside `synchronized` blocks too. If
you are on 21 this advice still applies; if you are on 24 or later, stop
rewriting `synchronized` for this reason. JNI frames still pin.

---

## 2. Pattern matching for switch (JEP 441)

**Code:** [`jdk21/PatternMatchingForSwitch`](../src/main/java/com/tomtom/genesis/platform/jdk21/PatternMatchingForSwitch.java)

Four previews (17, 18, 19, 20) before this became standard.

### Type patterns

```java
String description = switch (value) {
    case Integer i -> "int " + i;
    case String s  -> "string of length " + s.length();
    case List<?> l -> "list of " + l.size();
    default        -> "something else";
};
```

This replaces the if/else-if chain of `instanceof` patterns from Java 16. The
compiler can also compile it into a jump rather than a linear sequence of type
checks, using the `typeSwitch` invokedynamic bootstrap.

### Guards with `when`

```java
case Purchase p when p.amount() > 1000 -> "flag for manual review";
case Purchase p                        -> "process normally";
```

**Order matters.** Cases are tried top to bottom, so the guarded case must come
first. The other way round, the compiler rejects it with *"this case label is
dominated by a preceding case label"* — a genuinely useful error.

`when` is a contextual keyword; a variable named `when` still compiles.

### `null` — the rule that finally changed

A traditional switch throws `NullPointerException` on a null selector. A pattern
switch lets you opt in to handling it:

```java
switch (value) {
    case null      -> "it was null";
    case String s  -> "string " + s;
    default        -> "something else";
}

switch (value) {
    case Integer i     -> "int";
    case null, default -> "null or anything else";
}
```

Without a `case null`, the old behaviour is preserved and it still throws. That
is the deliberate compromise: nothing silently changes, but you can now handle
null inside the switch instead of guarding it outside.

### Exhaustiveness

Over a sealed hierarchy, no `default` is needed and none should be written:

```java
String user = switch (event) {
    case Login l    -> l.user();
    case Logout l   -> l.user();
    case Purchase p -> p.user();
    case Error e    -> "(system)";
};
```

Add a fifth `Event` type and every switch like this stops compiling until you
handle it. A `default` would swallow it and hand you the fallback at runtime.

A subtlety: an exhaustive switch without a `default` still gets a hidden fallback
that throws `MatchException`. It fires only if the sealed hierarchy changed
*after* this class was compiled — a class-file version skew. You will never see
it in a consistent build.

### What it replaces

The visitor pattern. A sealed interface plus a pattern switch expresses the same
thing in a fraction of the code, with the exhaustiveness guarantee the visitor
pattern was invented to provide.

The expression problem has not gone away, though:

- **visitor** — easy to add an operation, hard to add a type;
- **sealed + switch** — easy to add a type (the compiler finds every switch), and
  operations live outside the hierarchy.

Use sealed types when the *set of types* is stable and the set of operations
keeps growing — which describes most domain modelling.

---

## 3. Record patterns (JEP 440)

**Code:** [`jdk21/RecordPatterns`](../src/main/java/com/tomtom/genesis/platform/jdk21/RecordPatterns.java)

```java
if (obj instanceof Point(int x, int y)) {
    // tested, destructured and bound in one step
}
```

**Nesting is where they earn their place:**

```java
if (obj instanceof ColouredLine(Line(Point(int x1, int y1), Point(int x2, int y2)), String colour)) {
    ...
}
```

Without them that is five lines of accessor calls and intermediate variables.

Combined with sealed types and switch, this is the combination the last six
years of releases were building towards:

```java
String description = switch (shape) {
    case Circle(Point(int x, int y), double r) when r > 3 -> "big circle at (" + x + "," + y + ")";
    case Circle(Point p, double r)                        -> "small circle at " + p;
    case Rect(Point(int x1, _), Point(int x2, _)) when x1 == x2 -> "zero width";
    case Rect(Point tl, Point br)                         -> "rectangle";
};
```

sealed interface (17) + records (16) + pattern switch (21) + record patterns (21)
= algebraic data types with compiler-checked exhaustiveness.

### Two rules that catch people out

**No implicit widening or narrowing.** For a component declared `int`, the
pattern must say `int` or `var`. Writing `(long x)` is a compile error, even
though `int` widens to `long` everywhere else in the language. (Java 25 previews
primitive type patterns, JEP 507, which relaxes this.)

**A record pattern does not match null components.** `Line(Point(int x, int y), ...)`
fails to match if `start` is null — so nested patterns quietly protect you from
`NullPointerException`, but also silently skip the case, which can hide a bug.

### What is still missing

Patterns work only for **records**. There is no way to destructure an ordinary
class, because a class does not declare a canonical shape. Deconstruction
patterns for arbitrary classes, array patterns, and let-style binding have all
been discussed in Project Amber; none has shipped.

---

## 4. Sequenced collections (JEP 431)

**Code:** [`jdk21/SequencedCollections`](../src/main/java/com/tomtom/genesis/platform/jdk21/SequencedCollections.java)

Getting the first element, before Java 21:

```
List          -> list.get(0)
Deque         -> deque.getFirst()
SortedSet     -> set.first()
LinkedHashSet -> set.iterator().next()      <- no method at all
```

Four spellings for one idea, and one case with no answer — even though
`LinkedHashSet` has had a defined order since Java 1.4.

Three new interfaces were retrofitted into the hierarchy:

```
Collection -> SequencedCollection -> List, Deque, SequencedSet
Map        -> SequencedMap        -> LinkedHashMap, SortedMap
```

with a small, obvious vocabulary: `getFirst`, `getLast`, `addFirst`, `addLast`,
`removeFirst`, `removeLast`, `reversed` — plus `firstEntry`, `lastEntry`,
`putFirst`, `putLast`, `pollFirstEntry`, `pollLastEntry`, `sequencedKeySet`,
`sequencedValues`, `sequencedEntrySet` on maps.

### `reversed()` is a view, not a copy

Verified output:

```
original = [a, b, c]         reversed = [c, b, a]
after original.add("d"):
  reversed = [d, c, b, a]    <- the view followed the change
after reversed.addFirst("z"):
  original = [a, b, c, d, z] <- writing through the view works too
```

`Collections.reverse(list)` mutates in place and costs O(n). `reversed()`
allocates a lightweight view in O(1) and shares storage — but is therefore not a
snapshot. For a genuine copy: `List.copyOf(list.reversed())`.

### Caveats

- `getFirst`/`getLast` throw `NoSuchElementException` on an empty collection;
  they do not return null.
- `HashSet` and `HashMap` are **not** sequenced — they have no order to expose.
- `putFirst`/`putLast` on a sorted map throw `UnsupportedOperationException` —
  position is decided by the comparator.
- The one real migration hazard: a class of yours implementing `List` now
  inherits new default methods. If it already declared an incompatible
  `reversed()` or `getFirst()`, it will fail to compile on 21. Rare, but real.

---

## 5. Smaller additions in 19–21

**Code:** [`jdk21/SmallAdditions21`](../src/main/java/com/tomtom/genesis/platform/jdk21/SmallAdditions21.java)

```java
Math.clamp(15, 1, 10)                       // 10; throws IllegalArgumentException if min > max
new StringBuilder().repeat("ab", 3)         // appends in place, no intermediate String
"a1b22c".splitWithDelimiters("\\d+", 0)     // [a, 1, b, 22, c] - keeps the separators
Character.isEmoji(codePoint)
Thread.sleep(Duration.ofSeconds(2))
future.state() / resultNow() / exceptionNow()
```

`splitWithDelimiters` keeps the separators interleaved with the parts, which is
what a tokeniser needs and what previously forced a `Matcher` loop by hand.

One curiosity from the emoji predicates, straight from the example output:

```
U+0039 9   isEmoji=true  isEmojiPresentation=false
```

The **digit 9 reports `isEmoji = true`**. Not a bug: Unicode marks `0–9`, `#`
and `*` as Emoji because they are the base characters of keycap sequences like
`9 U+FE0F U+20E3`, which renders as a boxed nine. `isEmojiPresentation` is false
for them — and that is the predicate you almost always want when filtering text.

---

## 6. Also in 21

| Change | Note |
|---|---|
| **Generational ZGC** (JEP 439) | Separate young and old generations. Large throughput gain; default from Java 23 |
| **Key Encapsulation Mechanism API** (JEP 452) | Foundation for post-quantum key exchange, delivered in 24 |
| **String Templates** (JEP 430) | Preview — and **withdrawn in 23**. Ignore tutorials showing `STR."..."` |
| **Unnamed classes and instance main** (JEP 445) | Preview — standard in 25 as JEP 512 |
| **Unnamed patterns and variables** (JEP 443) | Preview — standard in 22 |
| **Scoped values** (JEP 446) | Preview — standard in 25 |
| **Structured concurrency** (JEP 453) | Preview — still preview in 25 |
| **Vector API** (JEP 448) | Sixth incubator |
| **Windows 32-bit x86 deprecated** (JEP 449) | Removed in 24 |
| **Applet API removed** | Finally |

---

## Next

[java22-24.md](java22-24.md) — unnamed variables, the Foreign Function & Memory
API, stream gatherers and the Class-File API.
