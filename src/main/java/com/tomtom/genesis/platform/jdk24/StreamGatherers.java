package com.tomtom.genesis.platform.jdk24;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Java 24 (JEP 485) - stream gatherers.
 *
 * Previewed in 22 and 23, standard in 24. Gatherers are to INTERMEDIATE stream
 * operations what Collectors are to terminal ones: a way to plug in your own,
 * instead of being limited to the fixed set built into the Stream interface.
 */
public class StreamGatherers {

    public static void main(String[] args) {
        theGap();
        windowing();
        foldAndScan();
        mapConcurrent();
        writingOne();
        whenToUse();
    }

    private static void theGap() {
        header("The gap this fills");

        System.out.println("Stream has a fixed set of intermediate operations: map, filter, flatMap,");
        System.out.println("distinct, sorted, limit, skip, peek, takeWhile, dropWhile, mapMulti.");
        System.out.println("If what you need is not on that list, you cannot add it.");
        System.out.println();
        System.out.println("Things that were awkward or impossible:");
        System.out.println("  - group consecutive elements into windows of three;");
        System.out.println("  - a running total (each element replaced by the sum so far);");
        System.out.println("  - deduplicate by a key rather than by equals;");
        System.out.println("  - emit an element only when it differs from the previous one;");
        System.out.println("  - run a slow mapping function concurrently, in order.");
        System.out.println();
        System.out.println("Terminal operations had Collector as an extension point since Java 8.");
        System.out.println("Intermediate operations had nothing. Gatherer is that missing interface.");
    }

    private static void windowing() {
        header("Built-in: windowFixed and windowSliding");

        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7);

        System.out.println("input            = " + numbers);
        System.out.println("windowFixed(3)   = " + numbers.stream()
                .gather(Gatherers.windowFixed(3)).toList());
        System.out.println("windowSliding(3) = " + numbers.stream()
                .gather(Gatherers.windowSliding(3)).toList());

        // A moving average falls straight out of windowSliding.
        List<String> movingAverage = numbers.stream()
                .gather(Gatherers.windowSliding(3))
                .map(w -> String.format("%.1f", w.stream().mapToInt(Integer::intValue).average().orElse(0)))
                .toList();
        System.out.println();
        System.out.println("3-point moving average = " + movingAverage);

        System.out.println();
        System.out.println("windowFixed splits into non-overlapping chunks; the final chunk may be");
        System.out.println("short. Useful for batching: send 100 rows per request.");
        System.out.println("windowSliding moves one element at a time, so consecutive windows overlap.");
    }

    private static void foldAndScan() {
        header("Built-in: fold and scan");

        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // fold reduces to a single element - like reduce, but as an intermediate step.
        List<String> folded = numbers.stream()
                .gather(Gatherers.fold(() -> "", (acc, n) -> acc + n))
                .toList();
        System.out.println("fold  = " + folded + "   (one element, the accumulated value)");

        // scan emits every intermediate accumulation - a running total.
        List<Integer> runningTotal = numbers.stream()
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();
        System.out.println("scan  = " + runningTotal + "   (running total)");

        System.out.println();
        System.out.println("The difference from reduce: fold and scan are INTERMEDIATE, so the stream");
        System.out.println("continues afterwards. A running total followed by a filter followed by a");
        System.out.println("collect is one pipeline, not three.");
        System.out.println();
        System.out.println("Also unlike reduce, fold's accumulator does not have to be associative");
        System.out.println("or the same type as the elements - it is explicitly sequential.");
    }

    private static void mapConcurrent() {
        header("Built-in: mapConcurrent - the one with the biggest practical payoff");

        List<String> ids = List.of("a", "b", "c", "d", "e", "f");

        long start = System.currentTimeMillis();
        List<String> results = ids.stream()
                .gather(Gatherers.mapConcurrent(4, StreamGatherers::slowLookup))
                .toList();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("results = " + results);
        System.out.println("elapsed = " + elapsed + " ms for 6 lookups of 100 ms each, 4 at a time");

        System.out.println();
        System.out.println("mapConcurrent runs the mapping function on VIRTUAL THREADS, at most N at");
        System.out.println("once, and preserves the input order in the output. That combination -");
        System.out.println("bounded concurrency plus ordered results - is exactly what parallel()");
        System.out.println("could never give you for blocking I/O.");
        System.out.println();
        System.out.println("Use it instead of stream().parallel() whenever the work is I/O bound.");
        System.out.println("parallel() uses the common ForkJoinPool, which is sized for CPU work and");
        System.out.println("is shared with the rest of the JVM - blocking in it is a known trap.");
    }

    private static String slowLookup(String id) {
        try {
            Thread.sleep(Duration.ofMillis(100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return id.toUpperCase();
    }

    private static void writingOne() {
        header("Writing your own gatherer");

        // distinctBy: keep the first element for each key. Not expressible with
        // distinct(), which only knows equals().
        record Person(String name, String city) { }

        List<Person> people = List.of(
                new Person("Ala", "Lodz"),
                new Person("Bartek", "Berlin"),
                new Person("Celina", "Lodz"),
                new Person("Dorota", "Amsterdam"));

        List<Person> firstPerCity = people.stream()
                .gather(distinctBy(Person::city))
                .toList();
        System.out.println("first person per city = " + firstPerCity);

        // deduplicateConsecutive: drop an element if it equals the previous one.
        List<String> collapsed = Stream.of("a", "a", "b", "b", "b", "a", "c")
                .gather(deduplicateConsecutive())
                .toList();
        System.out.println("consecutive dedupe    = " + collapsed);

        System.out.println();
        System.out.println("The shape of a Gatherer, in the order the arguments appear:");
        System.out.println("  initializer - creates the mutable private state (or none);");
        System.out.println("  integrator  - called per element; pushes results to the downstream");
        System.out.println("                and returns false to stop the stream early;");
        System.out.println("  combiner    - merges state for parallel streams (omit to stay sequential);");
        System.out.println("  finisher    - runs after the last element, to flush anything buffered.");
    }

    /** Keeps the first element for each key produced by the extractor. */
    private static <T, K> Gatherer<T, ?, T> distinctBy(java.util.function.Function<T, K> keyExtractor) {
        return Gatherer.ofSequential(
                ArrayList<K>::new,                                  // state: keys seen so far
                (seen, element, downstream) -> {
                    K key = keyExtractor.apply(element);
                    if (!seen.contains(key)) {
                        seen.add(key);
                        return downstream.push(element);
                    }
                    return true;                                    // skip, keep going
                });
    }

    /** Drops an element when it equals its immediate predecessor. */
    private static <T> Gatherer<T, ?, T> deduplicateConsecutive() {
        return Gatherer.ofSequential(
                () -> new Object() { T previous; boolean seen; },
                (state, element, downstream) -> {
                    if (state.seen && java.util.Objects.equals(state.previous, element)) {
                        return true;
                    }
                    state.previous = element;
                    state.seen = true;
                    return downstream.push(element);
                });
    }

    private static void whenToUse() {
        header("When to reach for a gatherer");

        System.out.println("Good reasons:");
        System.out.println("  - the operation needs to remember something between elements");
        System.out.println("    (windows, running totals, deduplication, change detection);");
        System.out.println("  - you want bounded concurrency with ordered output (mapConcurrent);");
        System.out.println("  - the same custom step appears in several pipelines and deserves a name.");
        System.out.println();
        System.out.println("Bad reasons:");
        System.out.println("  - a plain map or filter would do;");
        System.out.println("  - the logic is clearer as an ordinary loop. Gatherers are powerful but");
        System.out.println("    the interface is dense, and a reader who has not met it before will");
        System.out.println("    spend longer on your gatherer than on the loop it replaced.");
        System.out.println();
        System.out.println("Related: Stream.mapMulti (Java 16) covers the simpler 'one in, many out'");
        System.out.println("case with no state - see docs/java16-17.md.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
