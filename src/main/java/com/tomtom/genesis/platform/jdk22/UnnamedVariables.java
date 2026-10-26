package com.tomtom.genesis.platform.jdk22;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Java 22 (JEP 456) - unnamed variables and patterns.
 *
 * The underscore becomes a way to say "there is a value here, and I deliberately
 * do not care about it". The compiler then stops warning, and the reader stops
 * looking for a use that does not exist.
 */
public class UnnamedVariables {

    record Point(int x, int y) { }

    record Line(Point start, Point end) { }

    public static void main(String[] args) {
        history();
        unusedLocals();
        inCatchAndLoops();
        inPatterns();
        rules();
    }

    private static void history() {
        header("A short history of the underscore");

        System.out.println("  up to Java 8  - '_' was an ordinary, legal identifier");
        System.out.println("  Java 8        - using it produced a warning");
        System.out.println("  Java 9        - it became a compile ERROR, reserving it for later");
        System.out.println("  Java 21       - usable in patterns (preview)");
        System.out.println("  Java 22       - standard, for patterns AND local variables");
        System.out.println();
        System.out.println("The removal in Java 9 was not cleanup for its own sake - it was a");
        System.out.println("deliberate thirteen-year plan to free up the character for this.");
    }

    private static void unusedLocals() {
        header("Values you must receive but do not need");

        Queue<String> queue = new LinkedList<>(List.of("a", "b", "c", "d", "e"));

        // Draining the first three items: the values are required by the API
        // but irrelevant here.
        for (int i = 0; i < 3; i++) {
            var _ = queue.poll();
        }
        System.out.println("after draining three: " + queue);

        // Side-effecting calls whose return value is genuinely uninteresting.
        Map<String, Integer> counts = new HashMap<>();
        counts.put("a", 1);
        var _ = counts.put("a", 2);      // the previous value, which we do not want
        System.out.println("counts = " + counts);

        System.out.println();
        System.out.println("The alternative used to be a variable named 'unused' or 'ignored', which");
        System.out.println("a reader still has to check, or discarding the value entirely, which");
        System.out.println("some static analysers then flag. '_' states the intent in one character.");
    }

    private static void inCatchAndLoops() {
        header("catch, for-each and try-with-resources");

        // The exception object is not needed - only the fact that it happened.
        List<String> inputs = List.of("1", "two", "3");
        List<Integer> parsed = new ArrayList<>();
        for (String input : inputs) {
            try {
                parsed.add(Integer.parseInt(input));
            } catch (NumberFormatException _) {
                parsed.add(-1);
            }
        }
        System.out.println("parsed with fallback = " + parsed);

        // Counting iterations without using the element.
        int count = 0;
        for (var _ : inputs) {
            count++;
        }
        System.out.println("iterations counted   = " + count);

        // A resource opened purely for its side effect (a lock, a span, a timer).
        try (var _ = openScope("request-42")) {
            System.out.println("inside the scope");
        }

        System.out.println();
        System.out.println("Several unnamed variables may coexist in the same scope - that is the");
        System.out.println("whole point. Two variables both named 'ignored' would not compile.");
    }

    /** AutoCloseable.close() throws Exception; narrowing it keeps the demo free of try/catch. */
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    private static Scope openScope(String name) {
        System.out.println("opened scope " + name);
        return () -> System.out.println("closed scope " + name);
    }

    private static void inPatterns() {
        header("Unnamed patterns - the original motivation");

        Object obj = new Line(new Point(1, 2), new Point(3, 4));

        // Only the x coordinates matter here; the rest is structural noise.
        if (obj instanceof Line(Point(int x1, _), Point(int x2, _))) {
            System.out.println("horizontal span = " + (x2 - x1));
        }

        // '_' alone matches any value of any type - the component type is dropped too.
        if (obj instanceof Line(var start, _)) {
            System.out.println("start = " + start);
        }

        // In a switch, this keeps the cases readable when a record is wide.
        for (Object value : List.of(new Point(0, 5), new Line(new Point(0, 0), new Point(1, 1)), "text")) {
            String description = switch (value) {
                case Point(int x, _) when x == 0 -> "point on the Y axis";
                case Point _ -> "some other point";
                case Line(Point(_, int y1), Point(_, int y2)) when y1 == y2 -> "horizontal line";
                case Line _ -> "some other line";
                default -> "not a shape";
            };
            System.out.printf("%-48s -> %s%n", value, description);
        }

        System.out.println();
        System.out.println("Compare the readability with and without it:");
        System.out.println("  case Line(Point(int x1, int y1), Point(int x2, int y2)) when y1 == y2 ->");
        System.out.println("  case Line(Point(_, int y1), Point(_, int y2)) when y1 == y2 ->");
        System.out.println();
        System.out.println("Without unnamed patterns, deeply nested record patterns force you to");
        System.out.println("name every component even when one in six is used. That is what made");
        System.out.println("record patterns unpleasant in Java 21 and why this followed immediately.");
    }

    private static void rules() {
        header("Rules and limits");

        System.out.println("Allowed:");
        System.out.println("  local variable declarations       var _ = f();");
        System.out.println("  for and for-each variables        for (var _ : list)");
        System.out.println("  catch parameters                  catch (Exception _)");
        System.out.println("  try-with-resources variables      try (var _ = scope())");
        System.out.println("  lambda parameters                 (a, _) -> a");
        System.out.println("  type patterns                     case Point _ ->");
        System.out.println("  record pattern components         Point(int x, _)");
        System.out.println();
        System.out.println("NOT allowed:");
        System.out.println("  fields              private int _;      // still an error");
        System.out.println("  method parameters   void m(int _)       // still an error");
        System.out.println("  reading it back     var _ = 1; print(_) // there is nothing to read");
        System.out.println();
        System.out.println("The key property: '_' declares no name at all, so it can appear many");
        System.out.println("times in one scope without clashing. It is not a variable called");
        System.out.println("underscore - it is the absence of a name.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
