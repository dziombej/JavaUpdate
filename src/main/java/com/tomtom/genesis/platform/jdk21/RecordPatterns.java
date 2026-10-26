package com.tomtom.genesis.platform.jdk21;

import java.util.List;

/**
 * Java 21 (JEP 440) - record patterns.
 *
 * Previewed in 19 and 20, standard in 21. A record pattern destructures a
 * record into its components in the same step that tests its type - and nests,
 * so a whole tree can be matched in one pattern.
 */
public class RecordPatterns {

    record Point(int x, int y) { }

    record Line(Point start, Point end) { }

    record ColouredLine(Line line, String colour) { }

    sealed interface Shape permits Circle, Rect { }

    record Circle(Point centre, double radius) implements Shape { }

    record Rect(Point topLeft, Point bottomRight) implements Shape { }

    public static void main(String[] args) {
        destructuring();
        nested();
        inSwitch();
        varAndGuards();
        theGap();
    }

    private static void destructuring() {
        header("Destructuring in instanceof");

        Object obj = new Point(3, 4);

        // Java 16: test and bind the whole object.
        if (obj instanceof Point p) {
            System.out.println("type pattern   : x=" + p.x() + " y=" + p.y());
        }

        // Java 21: test and bind the COMPONENTS directly.
        if (obj instanceof Point(int x, int y)) {
            System.out.println("record pattern : x=" + x + " y=" + y
                    + ", distance=" + Math.sqrt(x * x + y * y));
        }

        System.out.println();
        System.out.println("The pattern Point(int x, int y) does three things at once: checks that");
        System.out.println("obj is a Point, calls the accessors, and binds the results to x and y.");
        System.out.println();
        System.out.println("The component types must match the record's declaration - the pattern is");
        System.out.println("checked against the record's shape at compile time, not by name.");
    }

    private static void nested() {
        header("Nesting - where it stops being sugar");

        Object obj = new ColouredLine(new Line(new Point(0, 0), new Point(3, 4)), "red");

        if (obj instanceof ColouredLine(Line(Point(int x1, int y1), Point(int x2, int y2)), String colour)) {
            double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            System.out.println("colour = " + colour + ", from (" + x1 + "," + y1 + ") to ("
                    + x2 + "," + y2 + "), length = " + length);
        }

        System.out.println();
        System.out.println("Without record patterns the same code is five lines of accessor calls");
        System.out.println("and intermediate variables:");
        System.out.println("  ColouredLine cl = (ColouredLine) obj;");
        System.out.println("  Line l = cl.line();");
        System.out.println("  Point a = l.start();  Point b = l.end();");
        System.out.println("  int x1 = a.x();  int y1 = a.y();  ...");
        System.out.println();
        System.out.println("Nesting is where record patterns earn their place: matching a SHAPE of");
        System.out.println("data several levels deep, in one readable expression.");
    }

    private static void inSwitch() {
        header("Combined with sealed types and switch");

        List<Shape> shapes = List.of(
                new Circle(new Point(0, 0), 5),
                new Circle(new Point(10, 10), 1),
                new Rect(new Point(0, 0), new Point(4, 3)),
                new Rect(new Point(2, 2), new Point(2, 9)));

        for (Shape shape : shapes) {
            String description = switch (shape) {
                case Circle(Point(int x, int y), double r) when r > 3 ->
                        "big circle at (" + x + "," + y + ") radius " + r;
                case Circle(Point p, double r) ->
                        "small circle at " + p + " radius " + r;
                case Rect(Point(int x1, int y1), Point(int x2, int y2)) when x1 == x2 ->
                        "degenerate rectangle - zero width";
                case Rect(Point tl, Point br) ->
                        "rectangle " + (br.x() - tl.x()) + "x" + (br.y() - tl.y());
            };
            System.out.printf("%-52s -> %s%n", shape, description);
        }

        System.out.println();
        System.out.println("This is the combination the last six years of Java releases were building");
        System.out.println("towards: sealed interface (17) + records (16) + pattern switch (21) +");
        System.out.println("record patterns (21). Together they give you algebraic data types with");
        System.out.println("compiler-checked exhaustiveness.");
    }

    private static void varAndGuards() {
        header("var inside patterns, and a caution about types");

        Object obj = new Line(new Point(1, 2), new Point(3, 4));

        // var infers the component type - useful when the type is long or obvious.
        if (obj instanceof Line(var start, var end)) {
            System.out.println("with var: start=" + start + " end=" + end);
        }

        System.out.println();
        System.out.println("Two rules that catch people out:");
        System.out.println();
        System.out.println("1. No implicit widening or narrowing in a record pattern.");
        System.out.println("   For a record component declared int, the pattern must say int or var.");
        System.out.println("   Writing (long x) is a compile error, even though int widens to long");
        System.out.println("   everywhere else in the language.");
        System.out.println("   (Java 25 previews primitive type patterns, JEP 507, which relaxes this.)");
        System.out.println();
        System.out.println("2. A record pattern does NOT match null for the nested components.");
        System.out.println("   Point(int x, int y) fails to match if the value is null, so nested");
        System.out.println("   patterns quietly protect you from NullPointerException -");
        System.out.println("   but also silently skip the case, which can hide a bug.");

        Object nullInside = new Line(null, new Point(3, 4));
        if (nullInside instanceof Line(Point(int x1, int y1), Point end)) {
            System.out.println("matched - not reached");
        } else {
            System.out.println();
            System.out.println("Line(null, Point(3,4)) does NOT match Line(Point(..), Point(..))");
        }
    }

    private static void theGap() {
        header("What is still missing");

        System.out.println("Patterns only work for RECORDS. There is no way to destructure an");
        System.out.println("ordinary class, because a class does not declare a canonical shape.");
        System.out.println();
        System.out.println("The planned answer is deconstruction patterns for arbitrary classes,");
        System.out.println("which would let a class declare its own pattern. Not in any release yet.");
        System.out.println();
        System.out.println("Also absent: array patterns (matching new int[]{1, 2, 3} by shape) and");
        System.out.println("patterns in a plain assignment (let-style binding). Both have been");
        System.out.println("discussed in the Amber project but neither has shipped.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
