// Java 25 (JEP 502) - Stable Values. PREVIEW FEATURE.
//
// Run it with:  java --enable-preview --source 25 examples/preview/StableValues.java
//
// A stable value is set at most once and, after that, the JVM may treat it as
// if it were final - constant-folding it into compiled code. It gives you the
// performance of a final field with the timing of a lazily initialised one.

import java.util.List;

public class StableValues {

    public static void main(String[] args) {
        theProblem();
        basics();
        lists();
        comparison();
    }

    static void theProblem() {
        header("The problem: final is fast, lazy is flexible, you cannot have both");

        System.out.println("A final field is initialised in the constructor. The JIT knows it can");
        System.out.println("never change, so it constant-folds reads of it. That is fast - but it");
        System.out.println("forces the value to exist before the object does.");
        System.out.println();
        System.out.println("Lazy initialisation defers the cost, but every pattern for it gives");
        System.out.println("something up:");
        System.out.println("  - a non-final field with a null check: the JIT cannot fold it, and");
        System.out.println("    it is not thread safe;");
        System.out.println("  - double-checked locking with volatile: correct, verbose, and the");
        System.out.println("    volatile read still blocks constant folding;");
        System.out.println("  - the holder-class idiom: fast, but needs a whole extra class and");
        System.out.println("    cannot take a parameter;");
        System.out.println("  - a Supplier memoised by hand: an extra indirection on every read.");
    }

    static void basics() {
        header("StableValue");

        StableValue<String> config = StableValue.of();

        System.out.println("isSet() before  = " + config.isSet());

        String first = config.orElseSet(() -> {
            System.out.println("  supplier running - this line appears once");
            return "loaded-configuration";
        });
        String second = config.orElseSet(() -> {
            System.out.println("  supplier running again - this line never appears");
            return "something-else";
        });

        System.out.println("first  = " + first);
        System.out.println("second = " + second);
        System.out.println("isSet() after   = " + config.isSet());

        System.out.println();
        System.out.println("orElseSet is atomic: with many threads racing, the supplier runs exactly");
        System.out.println("once and everyone receives the same value. No synchronized, no volatile,");
        System.out.println("no double-checked locking.");
        System.out.println();
        System.out.println("trySet returns false instead of overwriting: "
                + config.trySet("ignored"));
        System.out.println("orElse for an unset value: " + StableValue.of().orElse("fallback"));
    }

    static void lists() {
        header("Stable collections - lazily computed elements");

        // Every element is computed on first access and cached from then on.
        List<Integer> squares = StableValue.list(5, index -> {
            System.out.println("  computing square of " + index);
            return index * index;
        });

        System.out.println("get(3) = " + squares.get(3));
        System.out.println("get(3) again = " + squares.get(3) + "   (no recomputation)");
        System.out.println("get(1) = " + squares.get(1));
        System.out.println("whole list = " + squares);

        System.out.println();
        System.out.println("Look closely at the printed list: it shows .unset for the slots nobody");
        System.out.println("has asked for. Even toString() does not force a computation - the list");
        System.out.println("stays lazy right up to the moment an element is actually read.");
        System.out.println();
        System.out.println("There is also StableValue.map(keys, mapper) with the same behaviour");
        System.out.println("keyed by an arbitrary set, and StableValue.supplier(...) which wraps a");
        System.out.println("Supplier so it computes once.");
        System.out.println();
        System.out.println("Note that the list is UNMODIFIABLE in the ordinary sense - you cannot");
        System.out.println("set an element. Each slot is filled by its own computation, once.");
    }

    static void comparison() {
        header("What it replaces");

        System.out.println("Instead of this:");
        System.out.println("  private volatile Logger logger;");
        System.out.println("  Logger logger() {");
        System.out.println("      Logger result = logger;");
        System.out.println("      if (result == null) {");
        System.out.println("          synchronized (this) {");
        System.out.println("              result = logger;");
        System.out.println("              if (result == null) logger = result = createLogger();");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("      return result;");
        System.out.println("  }");
        System.out.println();
        System.out.println("you write this:");
        System.out.println("  private final StableValue<Logger> logger = StableValue.of();");
        System.out.println("  Logger logger() { return logger.orElseSet(this::createLogger); }");
        System.out.println();
        System.out.println("and the JIT can fold the result into compiled code, which the volatile");
        System.out.println("version prevents.");
        System.out.println();
        System.out.println("STATUS: preview in Java 25. The API may change before it is finalised,");
        System.out.println("so do not put it in production code yet - but it is worth knowing about,");
        System.out.println("because it removes one of the last reasons to hand-write concurrency.");
    }

    static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
