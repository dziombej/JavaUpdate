// Java 25 (JEP 505) - Structured Concurrency. PREVIEW FEATURE (fifth preview).
//
// Run it with:  java --enable-preview --source 25 examples/preview/StructuredConcurrency.java
//
// The idea: if a task splits into subtasks, those subtasks must all finish
// before the task itself returns - so concurrency follows the same block
// structure as ordinary code, and a stack trace means something again.

import java.util.concurrent.StructuredTaskScope;
import java.time.Duration;
import java.util.List;

public class StructuredConcurrency {

    public static void main(String[] args) throws Exception {
        theProblem();
        allMustSucceed();
        firstToSucceed();
        cancellation();
        scopedValueInheritance();
        status();
    }

    static void theProblem() {
        header("The problem with ExecutorService");

        System.out.println("  var a = executor.submit(this::fetchUser);");
        System.out.println("  var b = executor.submit(this::fetchOrders);");
        System.out.println("  return combine(a.get(), b.get());");
        System.out.println();
        System.out.println("Three things go wrong here:");
        System.out.println();
        System.out.println("1. If fetchUser fails immediately, fetchOrders keeps running to");
        System.out.println("   completion. Nobody will ever read its result - it is pure waste,");
        System.out.println("   and it holds a connection while it burns.");
        System.out.println();
        System.out.println("2. If the caller is cancelled, neither subtask learns about it.");
        System.out.println("   They are orphans with no parent.");
        System.out.println();
        System.out.println("3. The stack trace of a failure inside fetchOrders shows the worker");
        System.out.println("   thread, not the caller that asked for the work. The relationship");
        System.out.println("   exists in your head and nowhere in the runtime.");
        System.out.println();
        System.out.println("Structured concurrency makes the relationship real.");
    }

    static void allMustSucceed() throws Exception {
        header("Fan out, wait for all, fail fast");

        long start = System.currentTimeMillis();

        try (var scope = StructuredTaskScope.open()) {
            var user = scope.fork(() -> fetch("user", 100));
            var orders = scope.fork(() -> fetch("orders", 150));
            var prefs = scope.fork(() -> fetch("preferences", 80));

            scope.join();       // waits for all three

            System.out.println("combined = " + user.get() + " + " + orders.get() + " + " + prefs.get());
        }

        System.out.println("elapsed  = " + (System.currentTimeMillis() - start)
                + " ms (the slowest subtask is 150 ms)");

        System.out.println();
        System.out.println("The try-with-resources block is the structure: nothing escapes it.");
        System.out.println("close() cannot return while a subtask is still running, so by the time");
        System.out.println("you are past the closing brace, every thread it started is finished.");
    }

    static void firstToSucceed() throws Exception {
        header("Race: take the first success, cancel the rest");

        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.<String>anySuccessfulResultOrThrow())) {
            scope.fork(() -> fetch("mirror-slow", 300));
            scope.fork(() -> fetch("mirror-fast", 50));
            scope.fork(() -> fetch("mirror-medium", 150));

            String winner = scope.join();
            System.out.println("winner = " + winner);
        }

        System.out.println();
        System.out.println("As soon as one subtask succeeds, the others are interrupted. This is the");
        System.out.println("shape you want for redundant requests to mirrors or replicas.");
        System.out.println();
        System.out.println("The Joiner decides the policy. The built-in ones cover 'all must succeed',");
        System.out.println("'first success wins', 'collect everything including failures', and you");
        System.out.println("can write your own.");
    }

    static void cancellation() {
        header("Fail fast: one failure cancels the siblings");

        long start = System.currentTimeMillis();
        try (var scope = StructuredTaskScope.open()) {
            scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(50));
                throw new IllegalStateException("the database is down");
            });
            scope.fork(() -> fetch("this-gets-cancelled", 2000));

            scope.join();
            System.out.println("not reached");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.println("failed after " + elapsed + " ms with: " + cause.getMessage());
            System.out.println();
            System.out.println("The second subtask asked for 2000 ms and never got there - it was");
            System.out.println("interrupted the moment its sibling failed. With a plain");
            System.out.println("ExecutorService it would have run the full two seconds.");
        }
    }

    static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    static void scopedValueInheritance() throws Exception {
        header("Why scoped values need this");

        ScopedValue.where(REQUEST_ID, "req-4711").call(() -> {
            try (var scope = StructuredTaskScope.open()) {
                var a = scope.fork(() -> "subtask A sees " + REQUEST_ID.get());
                var b = scope.fork(() -> "subtask B sees " + REQUEST_ID.get());
                scope.join();
                System.out.println("  " + a.get());
                System.out.println("  " + b.get());
            }
            return null;
        });

        System.out.println();
        System.out.println("Neither subtask received the request id as a parameter - they inherited");
        System.out.println("the binding from the scope that forked them.");
        System.out.println();
        System.out.println("This is the ONLY way a scoped value crosses a thread boundary. A thread");
        System.out.println("you start by hand, or a task submitted to an ExecutorService, sees an");
        System.out.println("unbound value and throws NoSuchElementException - try it in");
        System.out.println("src/main/java/com/tomtom/genesis/platform/jdk25/ScopedValues.java.");
        System.out.println();
        System.out.println("The restriction exists to keep the guarantee honest: a binding lasts");
        System.out.println("exactly as long as one call, and only structured concurrency can promise");
        System.out.println("that every thread reading it has finished by then.");
    }

    static void status() {
        header("Status and history");

        System.out.println("Structured concurrency has been in preview since Java 19 and the API has");
        System.out.println("changed substantially along the way:");
        System.out.println("  19-23  StructuredTaskScope.ShutdownOnFailure / ShutdownOnSuccess");
        System.out.println("  24     redesigned around Joiner");
        System.out.println("  25     fifth preview, StructuredTaskScope.open(...)");
        System.out.println();
        System.out.println("If you find a tutorial using ShutdownOnFailure, it describes the older");
        System.out.println("shape. Code written against it will not compile on 25.");
        System.out.println();
        System.out.println("It is the natural partner of virtual threads (docs/java21.md) and scoped");
        System.out.println("values (docs/java25.md): virtual threads make forking cheap, structured");
        System.out.println("concurrency makes it safe, scoped values carry context into the subtasks.");
    }

    static String fetch(String what, int millis) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(millis));
        return what;
    }

    static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
