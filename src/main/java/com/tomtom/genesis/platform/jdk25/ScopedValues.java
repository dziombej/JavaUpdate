package com.tomtom.genesis.platform.jdk25;

import java.util.concurrent.Executors;

/**
 * Java 25 (JEP 506) - scoped values.
 *
 * Previewed from 20 through 24, standard in 25. A scoped value shares immutable
 * data from a caller to its callees - including code running on threads it
 * starts - without threading the value through every method signature.
 *
 * Think of it as the replacement for ThreadLocal in the virtual-thread world.
 */
public class ScopedValues {

    /** Conventionally a static final field, upper case, like a constant. */
    private static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();
    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        theProblemWithThreadLocal();
        basicUsage();
        rebinding();
        inheritance();
        comparison();
    }

    private static void theProblemWithThreadLocal() {
        header("What is wrong with ThreadLocal");

        System.out.println("ThreadLocal has three structural problems:");
        System.out.println();
        System.out.println("1. UNBOUNDED LIFETIME. set() with no matching remove() keeps the value");
        System.out.println("   alive as long as the thread. In a pooled thread that is forever, and");
        System.out.println("   it leaks across unrelated requests - a classic source of both memory");
        System.out.println("   leaks and data leaking between users.");
        System.out.println();
        System.out.println("2. MUTABLE. Anyone downstream can call set() and change what the caller");
        System.out.println("   established. There is no way to hand out read-only context.");
        System.out.println();
        System.out.println("3. EXPENSIVE TO INHERIT. InheritableThreadLocal copies the whole map into");
        System.out.println("   every child thread. With a million virtual threads that is fatal.");
        System.out.println("   (Scoped values trade this for a narrower rule - see the section below.)");
        System.out.println();
        System.out.println("Scoped values fix all three: the binding lasts exactly as long as one");
        System.out.println("call, the value is immutable, and inheritance shares rather than copies.");
    }

    private static void basicUsage() {
        header("Binding a value for the duration of one call");

        ScopedValue.where(CURRENT_USER, "lukasz").run(() -> {
            System.out.println("inside run(): user = " + CURRENT_USER.get());
            deeplyNestedMethod();
        });

        System.out.println();
        System.out.println("outside the scope, isBound() = " + CURRENT_USER.isBound());
        try {
            CURRENT_USER.get();
        } catch (java.util.NoSuchElementException e) {
            System.out.println("outside the scope, get()     -> NoSuchElementException");
        }

        System.out.println();
        System.out.println("There is no set() and no remove(). The value exists only inside the");
        System.out.println("lambda passed to run(), and it is unbound automatically when that lambda");
        System.out.println("returns - normally or by exception. Forgetting to clean up is impossible.");
    }

    private static void deeplyNestedMethod() {
        // Three frames down, with nothing passed as a parameter.
        System.out.println("  three calls deeper: user = " + CURRENT_USER.get()
                + ", orElse for an unbound value = " + REQUEST_ID.orElse("(none)"));
    }

    private static void rebinding() throws Exception {
        header("Rebinding and multiple values");

        // Several values bound at once by chaining where().
        String result = ScopedValue.where(CURRENT_USER, "ala")
                .where(REQUEST_ID, "req-1")
                .call(() -> {
                    System.out.println("outer: user=" + CURRENT_USER.get() + " request=" + REQUEST_ID.get());

                    // A nested binding shadows the outer one for the duration of the inner call.
                    ScopedValue.where(CURRENT_USER, "bartek").run(() ->
                            System.out.println("  inner: user=" + CURRENT_USER.get()
                                    + " request=" + REQUEST_ID.get() + "   (request unchanged)"));

                    System.out.println("outer again: user=" + CURRENT_USER.get()
                            + "   (the outer binding is back)");
                    return "returned from call()";
                });

        System.out.println("call() returns a value: " + result);

        System.out.println();
        System.out.println("run() takes a Runnable, call() takes something that returns a value and");
        System.out.println("may throw. Rebinding is scoped, not destructive - the inner binding is");
        System.out.println("discarded when the inner call ends, which is precisely the guarantee");
        System.out.println("ThreadLocal.set() cannot make.");
    }

    private static void inheritance() throws Exception {
        header("Inheritance by child threads - narrower than you might expect");

        ScopedValue.where(CURRENT_USER, "celina").run(() -> {
            // A thread you start yourself does NOT inherit the binding.
            try {
                Thread child = Thread.ofVirtual().start(() ->
                        System.out.println("  manually started thread : "
                                + (CURRENT_USER.isBound() ? "bound to " + CURRENT_USER.get() : "NOT bound")));
                child.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Neither does a task submitted to an ordinary ExecutorService.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var future = executor.submit(() ->
                        CURRENT_USER.isBound() ? "bound to " + CURRENT_USER.get() : "NOT bound");
                System.out.println("  ExecutorService task    : " + future.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            System.out.println("  inside the binding      : bound to " + CURRENT_USER.get());
        });

        System.out.println();
        System.out.println("This is the part people get wrong, including at first reading of the JEP.");
        System.out.println("A scoped value is inherited ONLY by subtasks forked from a");
        System.out.println("StructuredTaskScope (JEP 505, still preview in 25). Threads you start by");
        System.out.println("hand, and tasks handed to a plain ExecutorService, see nothing.");
        System.out.println();
        System.out.println("The reason is the guarantee. A scoped value is valid for the duration of");
        System.out.println("one call and no longer. That can only be enforced if the runtime knows");
        System.out.println("every thread that might read it will finish before the binding ends -");
        System.out.println("which is exactly what structured concurrency provides and an unstructured");
        System.out.println("executor does not.");
        System.out.println();
        System.out.println("So the two features are a pair, not two independent additions:");
        System.out.println("  virtual threads (21)        make forking cheap");
        System.out.println("  structured concurrency (preview) makes forking safe and bounded");
        System.out.println("  scoped values (25)          carry context into those bounded subtasks");
        System.out.println();
        System.out.println("Working demonstration of the inheriting case, using the preview API:");
        System.out.println("  examples/preview/StructuredConcurrency.java");
    }

    private static void comparison() {
        header("Side by side");

        System.out.println("                     ThreadLocal            ScopedValue");
        System.out.println("  lifetime           until remove()         the enclosing call only");
        System.out.println("  mutability         set() any time         immutable once bound");
        System.out.println("  cleanup            manual, easy to skip   automatic");
        System.out.println("  inheritance        any child thread,      ONLY subtasks of a");
        System.out.println("                     by copying the map     StructuredTaskScope, by sharing");
        System.out.println("  cost per thread    proportional to size   constant");
        System.out.println("  reading            get() returns null     get() throws if unbound");
        System.out.println();
        System.out.println("When ThreadLocal is still correct: you genuinely need per-thread MUTABLE");
        System.out.println("state - a reusable buffer, a SimpleDateFormat cache, a random generator.");
        System.out.println("Scoped values cannot express that, and are not meant to.");
        System.out.println();
        System.out.println("Typical scoped-value candidates: the authenticated principal, a request");
        System.out.println("or trace id, a tenant identifier, a transaction handle - anything set");
        System.out.println("once at the edge of a request and read everywhere below it.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
