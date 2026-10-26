package com.tomtom.genesis.platform.jdk16;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java 16 - Stream.toList and Stream.mapMulti.
 */
public class StreamAdditions16 {

    public static void main(String[] args) {
        toList();
        theNullTrap();
        mapMulti();
    }

    private static void toList() {
        header("Stream.toList");

        List<String> result = Stream.of("c", "a", "b").sorted().toList();
        System.out.println("Stream.of(...).sorted().toList() = " + result);

        System.out.println();
        System.out.println("Shorter than .collect(Collectors.toList()), and the returned list is");
        System.out.println("unmodifiable - which the old collector never promised:");
        try {
            result.add("d");
        } catch (UnsupportedOperationException e) {
            System.out.println("  result.add(\"d\") -> UnsupportedOperationException");
        }
    }

    private static void theNullTrap() {
        header("The trap: three similar methods, three different null policies");

        List<String> withNulls = Stream.of("a", null, "b").toList();
        System.out.println("Stream.toList() with nulls           = " + withNulls + "   <- accepted!");

        try {
            Stream.of("a", (String) null).collect(Collectors.toUnmodifiableList());
        } catch (NullPointerException e) {
            System.out.println("Collectors.toUnmodifiableList()      -> NullPointerException");
        }

        List<String> old = Stream.of("a", (String) null).collect(Collectors.toList());
        System.out.println("Collectors.toList() with nulls       = " + old + "   (modifiable, nulls ok)");

        System.out.println();
        System.out.println("Summary of the three:");
        System.out.println("  Collectors.toList()             (8)  modifiable in practice, nulls ok,");
        System.out.println("                                       but the contract guarantees neither");
        System.out.println("  Collectors.toUnmodifiableList() (10) unmodifiable, REJECTS nulls");
        System.out.println("  Stream.toList()                 (16) unmodifiable, ACCEPTS nulls");
        System.out.println();
        System.out.println("Stream.toList looks like a shorthand for the middle one but is not.");
        System.out.println("If a stream can yield null and you relied on toUnmodifiableList to blow up,");
        System.out.println("switching to toList silently changes behaviour.");
    }

    private static void mapMulti() {
        header("Stream.mapMulti - one element in, any number out");

        // flatMap allocates a Stream per element. mapMulti pushes results into a
        // consumer instead, so nothing is allocated for the common cases.
        List<String> viaFlatMap = Stream.of("a,b", "c", "d,e,f")
                .flatMap(s -> java.util.Arrays.stream(s.split(",")))
                .toList();

        List<String> viaMapMulti = Stream.of("a,b", "c", "d,e,f")
                .<String>mapMulti((s, consumer) -> {
                    for (String part : s.split(",")) {
                        consumer.accept(part);
                    }
                })
                .toList();

        System.out.println("flatMap  = " + viaFlatMap);
        System.out.println("mapMulti = " + viaMapMulti);

        // Where mapMulti really wins: filtering and mapping at once, emitting 0 or 1.
        List<Integer> parsed = Stream.of("1", "two", "3", "four")
                .<Integer>mapMulti((s, consumer) -> {
                    try {
                        consumer.accept(Integer.parseInt(s));
                    } catch (NumberFormatException ignored) {
                        // emit nothing
                    }
                })
                .toList();
        System.out.println("parse, skipping failures = " + parsed);

        System.out.println();
        System.out.println("Note the explicit type witness .<String>mapMulti(...) - the compiler");
        System.out.println("cannot infer the output type from a consumer's parameter, so you have to");
        System.out.println("say it. That awkwardness is the main reason mapMulti stays niche.");
        System.out.println();
        System.out.println("Reach for it when: you emit zero or one element per input, or the number");
        System.out.println("of results is large and allocating a Stream per element would hurt.");
        System.out.println("Otherwise flatMap reads better. Java 24 adds Gatherers for the harder");
        System.out.println("cases - see docs/java22-24.md.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
