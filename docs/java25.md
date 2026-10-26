# Java 25 — the current LTS

September 2025. The LTS that follows 21, and the version this repository is
built and verified against. It finalises four features that had been previewing
for years, and quietly makes Java approachable for a first program for the first
time since 1995.

---

## 1. Compact source files and instance main methods (JEP 512)

**Code:** [`examples/java25/Script.java`](../examples/java25/Script.java) — run with `java examples/java25/Script.java`

```java
void main() {
    IO.println("no class, no static, no System.out");
}
```

That is a complete, runnable Java program.

**What the compiler does:** wraps everything in an implicit, unnamed top-level
class; allows `main` to be an instance method, with or without the `String[]`
parameter; auto-imports `java.lang.IO`; and — since Java 25 — auto-imports the
`java.base` module, so `List`, `Stream` and `Duration` need no import either.

**How the launcher picks a main method**, in order:

1. `void main(String[] args)` — instance
2. `void main()` — instance
3. `static void main(String[] args)`
4. `static void main()`

So the classic `public static void main(String[] args)` still works everywhere it
always did. Nothing was taken away.

**Rules of an implicit class:** it has no name, so nothing can refer to it; it
cannot be in a named package; it may declare fields, methods and nested classes
normally; it always has a default no-arg constructor.

**Why this matters beyond teaching.** The first program a beginner writes
required them to accept `public`, `class`, `static`, `void`, `String[]` and a
fully qualified `System.out.println` on faith. That is six concepts before "hello
world". This removes them without creating a second dialect — the same file grows
into an ordinary class when it needs to.

The feature moved a lot before settling, which matters if you read older material:

| Version | Name | Status |
|---|---|---|
| 21 | unnamed classes and instance main methods (JEP 445) | preview |
| 22 | (JEP 463) | second preview |
| 23 | implicitly declared classes (JEP 477) | third preview; `IO` added |
| 24 | (JEP 495) | fourth preview |
| 25 | **compact source files and instance main methods (JEP 512)** | **standard** |

This is exactly what `src/main/java/com/tomtom/genesis/platform/Main.java` in
this repository uses — the file IntelliJ generated when the project was created,
and the reason the build targets 25 rather than 11.

---

## 2. Module import declarations (JEP 511)

**Code:** [`jdk25/ModuleImports`](../src/main/java/com/tomtom/genesis/platform/jdk25/ModuleImports.java)

```java
import module java.base;
```

One line replaces `java.util.*`, `java.util.stream.*`, `java.util.function.*`,
`java.nio.file.*`, `java.time.*`, `java.math.*` and around fifty more.

**What it means precisely:** an on-demand import of every package the module
**exports**, and transitively of everything it re-exports with
`requires transitive`. So `import module java.se` pulls in essentially the whole
SE API. A package the module does not export stays invisible, exactly as with
any other module access.

It does **not** add a dependency — in a modular project you still need `requires`
in `module-info.java`. This only affects name resolution. It works for your own
modules too: `import module com.example.core`.

**The ambiguity problem.** Importing many packages at once makes clashes more
likely — `java.util.List` versus `java.awt.List` is the famous pair. The fix is a
single-type import, which always wins:

```java
import module java.base;
import module java.desktop;
import java.util.List;        // resolves the ambiguity
```

Resolution order, strongest first: single-type imports, then on-demand package
imports, then module imports.

**Where it belongs:** single-file programs, JShell, teaching examples,
prototypes — pairing deliberately with compact source files. For a production
codebase, prefer explicit imports; a reader who sees `Duration` should not have
to consult the module graph, and most style guides already ban wildcard imports
for the same reason.

---

## 3. Flexible constructor bodies (JEP 513)

**Code:** [`jdk25/FlexibleConstructorBodies`](../src/main/java/com/tomtom/genesis/platform/jdk25/FlexibleConstructorBodies.java)

Until Java 22, `this(...)` or `super(...)` had to be the **first statement** in a
constructor. Now statements may precede it, as long as they do not touch the
object under construction.

### Three things this enables

**Validate before delegating:**

```java
Celsius(double degrees) {
    if (degrees < -273.15) throw new IllegalArgumentException("below absolute zero");
    super(degrees + 273.15);
}
```

The superclass constructor never runs for an invalid argument, so no half-built
object exists. The old workaround — a static helper called inside the `super`
argument list — worked but scattered one constructor's logic across the class.

**Compute once, use twice:**

```java
Square(int side) {
    int size = side * side;
    super(new int[size]);
    this.side = side;
}
```

**Close the constructor-calls-overridable-method hole.** This is the interesting
one. Assigning a field *before* `super()` means the superclass constructor cannot
observe it as zero. Verified output from the example:

```
UnsafeCircle (super() first):      Shape() calls describe() -> a circle of radius 0.0
SafeCircle   (field first):        Shape() calls describe() -> a circle of radius 5.0
```

That hazard has bitten every codebase that ever used a template-method base
class. This does not make calling overridable methods from constructors a good
idea — it makes the damage containable when you inherit from code that does.

### What the prologue may and may not do

The region before the constructor invocation is the **prologue**; after it, the
**epilogue**. Only the epilogue has a fully formed object.

**Allowed:** local variables, validation and throwing, static method calls,
**assigning fields declared in this class** (the part that became standard in 25),
and any control flow.

**Not allowed:** reading any field — including one you just assigned; calling any
instance method; using `this` or `super` as a value; assigning a superclass field.

The compiler enforces this; the error reads *"cannot reference this before
supertype constructor has been called"*.

---

## 4. Scoped values (JEP 506)

**Code:** [`jdk25/ScopedValues`](../src/main/java/com/tomtom/genesis/platform/jdk25/ScopedValues.java)

Previewed from 20 through 24. The replacement for `ThreadLocal` in the
virtual-thread world.

```java
private static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();

ScopedValue.where(CURRENT_USER, "lukasz").run(() -> {
    // CURRENT_USER.get() works anywhere below this point
});
// outside: isBound() == false, get() throws NoSuchElementException
```

**There is no `set()` and no `remove()`.** The value exists only inside the
lambda and is unbound automatically when it returns — normally or by exception.
Forgetting to clean up is impossible.

### What was wrong with ThreadLocal

1. **Unbounded lifetime.** `set()` without a matching `remove()` keeps the value
   alive as long as the thread. In a pooled thread that is forever, and it leaks
   across unrelated requests — a source of both memory leaks and data leaking
   between users.
2. **Mutable.** Anyone downstream can call `set()` and change what the caller
   established. There is no way to hand out read-only context.
3. **Expensive to inherit.** `InheritableThreadLocal` copies the whole map into
   every child thread. With a million virtual threads that is fatal.

### Rebinding

```java
ScopedValue.where(CURRENT_USER, "ala").where(REQUEST_ID, "req-1").call(() -> {
    ScopedValue.where(CURRENT_USER, "bartek").run(() -> {
        // user = bartek, request = req-1
    });
    // user = ala again - the inner binding is discarded
});
```

Rebinding is scoped, not destructive — precisely the guarantee `ThreadLocal.set()`
cannot make.

### Inheritance is narrower than you would expect

This is the part that is easy to get wrong, and the example demonstrates it:

```
manually started thread : NOT bound
ExecutorService task    : NOT bound
inside the binding      : bound to celina
```

A scoped value is inherited **only** by subtasks forked from a
`StructuredTaskScope`. Threads you start by hand, and tasks handed to a plain
`ExecutorService`, see nothing.

The reason is the guarantee itself. A binding is valid for the duration of one
call and no longer. That can only be enforced if the runtime knows every thread
that might read it will finish before the binding ends — which is exactly what
structured concurrency provides and an unstructured executor does not.

So the features are a set, not three independent additions:

- **virtual threads (21)** make forking cheap;
- **structured concurrency (preview)** makes forking safe and bounded;
- **scoped values (25)** carry context into those bounded subtasks.

A working demonstration of the inheriting case is in
[`examples/preview/StructuredConcurrency.java`](../examples/preview/StructuredConcurrency.java).

### Side by side

| | ThreadLocal | ScopedValue |
|---|---|---|
| lifetime | until `remove()` | the enclosing call only |
| mutability | `set()` any time | immutable once bound |
| cleanup | manual, easy to skip | automatic |
| inheritance | any child thread, by copying | only `StructuredTaskScope` subtasks, by sharing |
| cost per thread | proportional to map size | constant |
| unbound read | `get()` returns null | `get()` throws |

**ThreadLocal is still correct** when you genuinely need per-thread *mutable*
state — a reusable buffer, a `SimpleDateFormat` cache, a random generator.
Scoped values cannot express that and are not meant to.

Typical scoped-value candidates: the authenticated principal, a request or trace
id, a tenant identifier, a transaction handle — anything set once at the edge of
a request and read everywhere below it.

---

## 5. Preview features worth trying

**Code:** [`examples/preview/`](../examples/preview) — run `./run.sh`

These need `--enable-preview`, which is why they live outside the Maven build.

### Stable values (JEP 502)

```java
private final StableValue<Logger> logger = StableValue.of();
Logger logger() { return logger.orElseSet(this::createLogger); }
```

A value set at most once, which the JVM may then constant-fold as if it were
`final`. It gives you the performance of a final field with the timing of a
lazily initialised one — the combination no existing idiom offered:

- a non-final field with a null check — not thread safe, not foldable;
- double-checked locking with `volatile` — correct, verbose, and the volatile
  read still blocks constant folding;
- the holder-class idiom — fast, but needs an extra class and takes no parameter.

`orElseSet` is atomic: with threads racing, the supplier runs exactly once.
There are also stable collections, whose elements are computed on first access.
One detail from the example output worth noticing:

```
whole list = [.unset, 1, .unset, 9, .unset]
```

Even `toString()` does not force a computation — the list stays lazy right up to
the moment an element is actually read.

### Structured concurrency (JEP 505, fifth preview)

```java
try (var scope = StructuredTaskScope.open()) {
    var user = scope.fork(() -> fetchUser());
    var orders = scope.fork(() -> fetchOrders());
    scope.join();
    return combine(user.get(), orders.get());
}
```

If a task splits into subtasks, they all finish before the task returns — so
concurrency follows the same block structure as ordinary code.

What an `ExecutorService` gets wrong, and this fixes:

1. If one subtask fails immediately, the others keep running to completion.
   Nobody will read their results — pure waste, holding connections meanwhile.
2. If the caller is cancelled, the subtasks never learn. They are orphans.
3. A failure's stack trace shows the worker thread, not the caller that asked for
   the work. The relationship exists in your head and nowhere in the runtime.

Verified from the example — one subtask fails after 50 ms while its sibling asked
for 2000 ms:

```
failed after 55 ms with: the database is down
```

The sibling was interrupted, not left running.

**The API has changed a lot across previews.** Java 19–23 used
`StructuredTaskScope.ShutdownOnFailure`/`ShutdownOnSuccess`; Java 24 redesigned
it around `Joiner`; Java 25 uses `StructuredTaskScope.open(...)`. Code written
against a tutorial from 2023 will not compile on 25.

### Primitive types in patterns (JEP 507, third preview)

Relaxes the rule that a record pattern component must match the declared type
exactly, and allows `instanceof int` and primitive cases in `switch`. Still
preview in 25.

---

## 6. Performance and operations

| Change | Note |
|---|---|
| **Compact object headers** (JEP 519) | Now a product feature (experimental in 24). Object headers 12 → 8 bytes; typical heap savings of 10–20%, enabled with `-XX:+UseCompactObjectHeaders` |
| **Generational Shenandoah** (JEP 521) | Shenandoah gains generations, as ZGC did in 21 |
| **AOT command-line ergonomics** (JEP 514) | `-XX:AOTCacheOutput` / `-XX:AOTCache` — a single training run produces a cache that cuts startup substantially |
| **AOT method profiling** (JEP 515) | The cache also carries JIT profiles, so the JVM reaches peak performance sooner after a restart |
| **JFR CPU-time profiling** (JEP 509) | Real CPU-time sampling on Linux, not just wall-clock |
| **JFR cooperative sampling** (JEP 518) | Safer, lower-overhead stack sampling |
| **JFR method timing and tracing** (JEP 520) | Instrument specific methods without an external agent |
| **PEM encodings** (JEP 470) | Preview — read and write PEM keys and certificates without third-party code or hand-rolled Base64 |
| **Key Derivation Function API** (JEP 510) | Standard: HKDF and friends |
| **32-bit x86 port removed** (JEP 503) | |
| **Vector API** (JEP 508) | Tenth incubator. Waiting on Project Valhalla |

---

## What this repository does not cover

**Java 26 and later.** The machine this material was written and verified on runs
JDK 25, and every claim here was checked by running the code. Rather than
describe a release the examples were never executed against, the material stops
at 25. Extending it is a matter of adding a package and a document in the same
shape.

**Project Valhalla** (value classes, specialised generics), **Project Leyden**
beyond the AOT cache already shipped, and **Project Babylon** — all in progress,
none delivered as standard features.

---

## Where to go next

[migration-11-to-25.md](migration-11-to-25.md) — what actually breaks when you
move an existing Java 11 application forward.
