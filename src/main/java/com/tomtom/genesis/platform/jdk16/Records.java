package com.tomtom.genesis.platform.jdk16;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Java 16 (JEP 395) - records.
 *
 * Previewed in 14 and 15, standard in 16. A record is a transparent carrier
 * for an immutable group of values: you declare WHAT it holds, and the compiler
 * generates the rest.
 */
public class Records {

    /**
     * One line replaces roughly sixty lines of boilerplate: private final fields,
     * a canonical constructor, accessors, equals, hashCode and toString.
     */
    record Point(int x, int y) { }

    /**
     * A COMPACT CONSTRUCTOR validates or normalises arguments. Note there is no
     * parameter list and no explicit assignment - the compiler assigns the fields
     * from the (possibly reassigned) parameters after this body runs.
     */
    record Range(int from, int to) {
        Range {
            if (from > to) {
                throw new IllegalArgumentException("from > to: " + from + " > " + to);
            }
        }

        int length() {
            return to - from;
        }

        static Range ofLength(int from, int length) {
            return new Range(from, from + length);
        }
    }

    /** A record can implement interfaces and declare static members. */
    record Money(String currency, long cents) implements Comparable<Money> {
        static final Money ZERO = new Money("PLN", 0);

        Money {
            Objects.requireNonNull(currency, "currency");
        }

        @Override
        public int compareTo(Money other) {
            return Long.compare(cents, other.cents);
        }

        @Override
        public String toString() {          // any generated member can be overridden
            return String.format("%d.%02d %s", cents / 100, Math.abs(cents % 100), currency);
        }
    }

    /** Defensive copying is YOUR job - the compiler will not do it. */
    record Team(String name, List<String> members) {
        Team {
            members = List.copyOf(members);   // reassigning the parameter is the idiom
        }
    }

    public static void main(String[] args) {
        whatYouGetForFree();
        compactConstructor();
        limits();
        defensiveCopying();
        whenToUse();
    }

    private static void whatYouGetForFree() {
        header("What the compiler generates");

        Point a = new Point(1, 2);
        Point b = new Point(1, 2);

        System.out.println("toString()  = " + a);
        System.out.println("accessors   = " + a.x() + ", " + a.y() + "   (note: x(), not getX())");
        System.out.println("equals      = " + a.equals(b) + "   (component by component)");
        System.out.println("hashCode    = " + (a.hashCode() == b.hashCode()));

        System.out.println();
        System.out.println("Sorting and collections work as expected because equals/hashCode are correct:");
        List<Point> points = List.of(new Point(3, 1), new Point(1, 5), new Point(2, 2));
        System.out.println("  sorted by x = " + points.stream()
                .sorted(Comparator.comparingInt(Point::x)).toList());

        System.out.println();
        System.out.println("The accessor name is the component name with no get prefix. This is");
        System.out.println("deliberate - a record is not a JavaBean, and frameworks that insist on");
        System.out.println("getX() need explicit configuration (Jackson handles records natively");
        System.out.println("since 2.12).");
    }

    private static void compactConstructor() {
        header("Compact constructor - validation and normalisation");

        Range ok = new Range(3, 10);
        System.out.println("new Range(3, 10)        = " + ok + ", length = " + ok.length());
        System.out.println("Range.ofLength(5, 4)    = " + Range.ofLength(5, 4));

        try {
            new Range(10, 3);
        } catch (IllegalArgumentException e) {
            System.out.println("new Range(10, 3)        -> IllegalArgumentException: " + e.getMessage());
        }

        Money m = new Money("PLN", 12345);
        System.out.println("overridden toString     = " + m);
        System.out.println("ZERO constant           = " + Money.ZERO);

        System.out.println();
        System.out.println("Inside a compact constructor you can REASSIGN the parameters. Whatever");
        System.out.println("they hold when the body ends is what gets stored in the fields. That is");
        System.out.println("the hook for trimming strings, clamping numbers or copying collections.");
    }

    private static void limits() {
        header("What a record cannot do");

        System.out.println("  - cannot extend a class (every record extends java.lang.Record);");
        System.out.println("  - is implicitly final - nothing can extend it;");
        System.out.println("  - cannot declare instance fields beyond its components;");
        System.out.println("  - its components are final - there are no setters, ever.");
        System.out.println();
        System.out.println("It CAN: implement interfaces, declare static fields and methods, declare");
        System.out.println("instance methods, override any generated member, be generic, be nested,");
        System.out.println("be declared local to a method, and carry annotations.");
        System.out.println();
        System.out.println("Reflection knows about them: " + java.util.Arrays.toString(Point.class.getRecordComponents()));
        System.out.println("Point.class.isRecord() = " + Point.class.isRecord());
    }

    private static void defensiveCopying() {
        header("The trap: shallow immutability");

        java.util.ArrayList<String> source = new java.util.ArrayList<>(List.of("Ala", "Bartek"));
        Team team = new Team("backend", source);

        source.add("INTRUDER");

        System.out.println("source after mutation = " + source);
        System.out.println("team.members()        = " + team.members() + "   <- unaffected");

        System.out.println();
        System.out.println("That only works because Team's compact constructor calls List.copyOf.");
        System.out.println("Without it the record would hold a reference to the caller's mutable list");
        System.out.println("and 'immutable record' would be a comfortable lie. A record guarantees");
        System.out.println("that the REFERENCES cannot be reassigned - nothing more.");

        try {
            team.members().add("x");
        } catch (UnsupportedOperationException e) {
            System.out.println("team.members().add(..) -> UnsupportedOperationException");
        }
    }

    private static void whenToUse() {
        header("When a record is the right tool");

        System.out.println("Good fits:");
        System.out.println("  - DTOs at an API or messaging boundary;");
        System.out.println("  - value objects: money, coordinates, ranges, identifiers;");
        System.out.println("  - multiple return values from a method;");
        System.out.println("  - keys for maps and sets (correct equals/hashCode come for free);");
        System.out.println("  - the leaves of a sealed hierarchy - see docs/java17.md and java21.md,");
        System.out.println("    where record patterns make this combination genuinely powerful.");
        System.out.println();
        System.out.println("Poor fits:");
        System.out.println("  - JPA entities - they need a no-arg constructor and mutable state;");
        System.out.println("  - anything whose identity matters more than its value;");
        System.out.println("  - a type you expect to extend.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
