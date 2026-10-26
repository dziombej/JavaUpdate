package com.tomtom.genesis.platform.jdk21;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Java 21 (JEP 444) - virtual threads.
 *
 * Previewed in 19 and 20, standard in 21. The headline feature of the release:
 * threads that are scheduled by the JVM rather than the operating system, so
 * blocking one costs almost nothing.
 */
public class VirtualThreads {

    public static void main(String[] args) throws Exception {
        theProblem();
        creatingThem();
        theScaleDemonstration();
        executors();
        whatToUnlearn();
        pinning();
    }

    private static void theProblem() {
        header("The problem: one platform thread per request does not scale");

        System.out.println("A platform thread is a thin wrapper over an OS thread. That means:");
        System.out.println("  - roughly 1 MB of reserved stack each;");
        System.out.println("  - creation costs about a millisecond;");
        System.out.println("  - a few thousand of them is the practical ceiling on a normal machine.");
        System.out.println();
        System.out.println("A typical request spends most of its life BLOCKED - waiting on a database,");
        System.out.println("an HTTP call, a queue. The thread sits there holding a megabyte and doing");
        System.out.println("nothing. That is why we built thread pools, and why we then adopted");
        System.out.println("reactive and async styles to avoid blocking at all.");
        System.out.println();
        System.out.println("The cost of that escape was the programming model: callbacks, Flux and");
        System.out.println("Mono, CompletableFuture chains, stack traces that tell you nothing, and");
        System.out.println("debuggers that cannot follow the logical flow.");
        System.out.println();
        System.out.println("Virtual threads make blocking cheap again, so the simple code comes back.");
    }

    private static void creatingThem() throws InterruptedException {
        header("Creating one");

        Thread virtual = Thread.ofVirtual().name("my-virtual").start(() ->
                System.out.println("  inside: " + Thread.currentThread()));
        virtual.join();

        Thread platform = Thread.ofPlatform().name("my-platform").start(() ->
                System.out.println("  inside: " + Thread.currentThread()));
        platform.join();

        Thread unstarted = Thread.ofVirtual().unstarted(() -> { });
        System.out.println();
        System.out.println("isVirtual()  = " + unstarted.isVirtual());
        System.out.println("isDaemon()   = " + unstarted.isDaemon() + "   <- virtual threads are ALWAYS daemon");
        System.out.println("getPriority()= " + unstarted.getPriority() + "   <- priority is ignored");

        System.out.println();
        System.out.println("Note the toString: VirtualThread[#NN]/runnable@ForkJoinPool-1-worker-N.");
        System.out.println("The part after the slash is the CARRIER - a platform thread from a");
        System.out.println("ForkJoinPool that is running this virtual thread right now. When the");
        System.out.println("virtual thread blocks, its stack is copied to the heap and the carrier");
        System.out.println("is released to run something else. That copy is the whole trick.");
    }

    private static void theScaleDemonstration() throws InterruptedException {
        header("The point: 100 000 concurrent blocking tasks");

        int taskCount = 100_000;
        AtomicInteger completed = new AtomicInteger();

        long start = System.currentTimeMillis();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, taskCount).forEach(i -> executor.submit(() -> {
                try {
                    Thread.sleep(Duration.ofMillis(100));   // pretend this is a database call
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }   // close() waits for every task to finish
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("tasks completed = " + completed.get());
        System.out.println("wall clock      = " + elapsed + " ms");
        System.out.println();
        System.out.println("Each task blocked for 100 ms, and all " + taskCount + " of them ran");
        System.out.println("concurrently. With platform threads this would need " + taskCount);
        System.out.println("OS threads - around 100 GB of reserved stack - or a pool, which would");
        System.out.println("turn it into " + (taskCount / 200) + " sequential rounds of 100 ms.");
        System.out.println();
        System.out.println("Actual memory per virtual thread starts at a few hundred bytes and grows");
        System.out.println("with the stack, because the stack lives on the heap.");
    }

    private static void executors() throws Exception {
        header("ExecutorService: same interface, different meaning");

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<String>> tasks = List.of(
                    () -> fetch("orders", 60),
                    () -> fetch("customers", 40),
                    () -> fetch("inventory", 80));

            long start = System.currentTimeMillis();
            List<Future<String>> futures = executor.invokeAll(tasks);
            for (Future<String> f : futures) {
                System.out.println("  " + f.get());
            }
            System.out.println("total = " + (System.currentTimeMillis() - start)
                    + " ms (the slowest task alone takes 80 ms)");
        }

        System.out.println();
        System.out.println("Two things to notice:");
        System.out.println("  1. ExecutorService is now AutoCloseable (Java 19), so try-with-resources");
        System.out.println("     replaces the shutdown / awaitTermination / shutdownNow dance.");
        System.out.println("  2. 'newVirtualThreadPerTaskExecutor' is not a pool. It creates one new");
        System.out.println("     virtual thread per task and never reuses it. Pooling something this");
        System.out.println("     cheap would only add contention.");
    }

    private static String fetch(String what, int millis) throws InterruptedException {
        Thread.sleep(millis);
        return what + " fetched on " + Thread.currentThread();
    }

    private static void whatToUnlearn() {
        header("Habits that become wrong");

        System.out.println("1. DO NOT POOL VIRTUAL THREADS.");
        System.out.println("   A pool exists to amortise an expensive resource. Virtual threads are");
        System.out.println("   cheap, so a pool only reintroduces the limit you were escaping.");
        System.out.println("   Create one per task.");
        System.out.println();
        System.out.println("2. Limit CONCURRENCY, not threads.");
        System.out.println("   You still need to protect a database with 20 connections. Use a");
        System.out.println("   Semaphore around the scarce resource instead of a small thread pool.");
        System.out.println();
        System.out.println("3. ThreadLocal still works, but think about cost.");
        System.out.println("   With a million threads, a ThreadLocal holding a 1 KB buffer is a");
        System.out.println("   gigabyte. Scoped values (Java 25, JEP 506) are the intended answer.");
        System.out.println();
        System.out.println("4. Virtual threads do NOT make CPU-bound work faster.");
        System.out.println("   They solve blocking, not computation. For parallel number crunching");
        System.out.println("   a fixed pool sized to the core count is still right.");
        System.out.println();
        System.out.println("5. They are always daemon threads.");
        System.out.println("   The JVM will not stay alive for them. Something has to join or await.");
    }

    private static void pinning() {
        header("Pinning - the one sharp edge in Java 21");

        System.out.println("A virtual thread normally unmounts from its carrier when it blocks.");
        System.out.println("In Java 21 there are two situations where it CANNOT, and the carrier");
        System.out.println("stays blocked with it - that is called pinning:");
        System.out.println();
        System.out.println("  - inside a synchronized block or method;");
        System.out.println("  - inside a native frame (JNI).");
        System.out.println();
        System.out.println("Enough pinned threads and you exhaust the carrier pool, which looks");
        System.out.println("exactly like a deadlock. To find it:");
        System.out.println("  -Djdk.tracePinnedThreads=full");
        System.out.println();
        System.out.println("The fix in Java 21 is to replace synchronized with ReentrantLock around");
        System.out.println("code that blocks - a lock does not pin.");
        System.out.println();
        System.out.println("IMPORTANT UPDATE: Java 24 (JEP 491) removed the synchronized limitation.");
        System.out.println("From 24 onwards a virtual thread unmounts inside synchronized blocks too,");
        System.out.println("so most of this advice is now historical. JNI frames still pin.");
        System.out.println("If you are on 21, it still applies; if you are on 24+, stop rewriting");
        System.out.println("synchronized for this reason.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
