package com.tomtom.genesis.platform.jdk12;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Java 12 - Collectors.teeing.
 *
 * Feeds every element of a stream into TWO collectors at once and then merges
 * their results. Before this, computing two aggregates meant either traversing
 * the stream twice or writing a custom collector.
 */
public class CollectorsTeeing {

    /** Plain class rather than a record - records arrive in Java 16. */
    static final class Order {
        private final String customer;
        private final double amount;

        Order(String customer, double amount) {
            this.customer = customer;
            this.amount = amount;
        }

        String customer() {
            return customer;
        }

        double amount() {
            return amount;
        }
    }

    static final class Summary {
        private final long count;
        private final double total;

        Summary(long count, double total) {
            this.count = count;
            this.total = total;
        }

        @Override
        public String toString() {
            return "count=" + count + ", total=" + total + ", average=" + (count == 0 ? 0 : total / count);
        }
    }

    public static void main(String[] args) {
        theProblem();
        theSolution();
        practicalUses();
    }

    private static final List<Order> ORDERS = List.of(
            new Order("Ala", 120.0),
            new Order("Bartek", 80.0),
            new Order("Celina", 200.0),
            new Order("Ala", 50.0));

    private static void theProblem() {
        header("The problem: two aggregates, two traversals");

        long count = ORDERS.stream().filter(o -> o.amount() > 60).count();
        double total = ORDERS.stream().filter(o -> o.amount() > 60).mapToDouble(Order::amount).sum();

        System.out.println("count = " + count + ", total = " + total);
        System.out.println();
        System.out.println("The filter runs twice. For an in-memory list that is merely wasteful.");
        System.out.println("For a stream backed by a file, a database cursor or a network call,");
        System.out.println("the second traversal may be impossible - a stream can only be consumed once.");
    }

    private static void theSolution() {
        header("The solution: one traversal, two collectors");

        Summary summary = ORDERS.stream()
                .filter(o -> o.amount() > 60)
                .collect(Collectors.teeing(
                        Collectors.counting(),                              // first collector
                        Collectors.summingDouble(Order::amount),            // second collector
                        Summary::new));                                     // merge function

        System.out.println("summary = " + summary);

        System.out.println();
        System.out.println("teeing takes exactly three arguments:");
        System.out.println("  1. a downstream collector");
        System.out.println("  2. another downstream collector");
        System.out.println("  3. a BiFunction merging both results into one value");
        System.out.println();
        System.out.println("The name comes from a plumbing tee - one input pipe, two outputs.");
    }

    private static void practicalUses() {
        header("Things teeing is genuinely good at");

        // Min and max in a single pass.
        String range = ORDERS.stream()
                .collect(Collectors.teeing(
                        Collectors.minBy((a, b) -> Double.compare(a.amount(), b.amount())),
                        Collectors.maxBy((a, b) -> Double.compare(a.amount(), b.amount())),
                        (min, max) -> min.map(Order::amount).orElse(0.0)
                                + " .. " + max.map(Order::amount).orElse(0.0)));
        System.out.println("min .. max          = " + range);

        // Partition a stream into two named lists without a boolean-keyed map.
        String split = ORDERS.stream()
                .collect(Collectors.teeing(
                        Collectors.filtering(o -> o.amount() >= 100,
                                Collectors.mapping(Order::customer, Collectors.toList())),
                        Collectors.filtering(o -> o.amount() < 100,
                                Collectors.mapping(Order::customer, Collectors.toList())),
                        (big, small) -> "big=" + big + " small=" + small));
        System.out.println("split by threshold  = " + split);

        // Average computed from its own two parts, so no second pass and no boxing surprises.
        String avg = ORDERS.stream()
                .collect(Collectors.teeing(
                        Collectors.summingDouble(Order::amount),
                        Collectors.counting(),
                        (sum, n) -> String.format("%.2f", sum / n)));
        System.out.println("average             = " + avg);

        System.out.println();
        System.out.println("When NOT to use it: if one of the aggregates is all you need, teeing only");
        System.out.println("adds noise. And for the classic sum/count/min/max set there is already");
        System.out.println("DoubleSummaryStatistics via Collectors.summarizingDouble:");
        System.out.println("  " + ORDERS.stream().collect(Collectors.summarizingDouble(Order::amount)));
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
