package com.tomtom.genesis.platform.jdk21;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Java 19-21 - the small API additions that are easy to miss but save real code.
 */
public class SmallAdditions21 {

    public static void main(String[] args) throws Exception {
        mathClamp();
        stringBuilderRepeat();
        splitWithDelimiters();
        emoji();
        futureState();
        sleepWithDuration();
    }

    private static void mathClamp() {
        header("Math.clamp (21)");

        System.out.println("Math.clamp(15, 1, 10)     = " + Math.clamp(15, 1, 10));
        System.out.println("Math.clamp(-5, 1, 10)     = " + Math.clamp(-5, 1, 10));
        System.out.println("Math.clamp(7, 1, 10)      = " + Math.clamp(7, 1, 10));
        System.out.println("Math.clamp(1.5, 0.0, 1.0) = " + Math.clamp(1.5, 0.0, 1.0));

        System.out.println();
        System.out.println("Replaces Math.max(min, Math.min(max, value)) - a line everyone has");
        System.out.println("written and half of us have got the wrong way round at least once.");
        System.out.println();
        System.out.println("It throws IllegalArgumentException when min > max, which the nested");
        System.out.println("max/min version silently would not:");
        try {
            Math.clamp(5, 10, 1);
        } catch (IllegalArgumentException e) {
            System.out.println("  Math.clamp(5, 10, 1) -> IllegalArgumentException: " + e.getMessage());
        }
    }

    private static void stringBuilderRepeat() {
        header("StringBuilder.repeat (21)");

        StringBuilder sb = new StringBuilder("header").append('\n');
        sb.repeat('-', 6).append('\n');
        sb.repeat("ab", 3);
        System.out.println(sb);

        System.out.println();
        System.out.println("String.repeat has existed since Java 11 (see docs/java11.md). The point");
        System.out.println("of the StringBuilder version is that it appends in place with no");
        System.out.println("intermediate String - it matters inside a loop building a large document.");
    }

    private static void splitWithDelimiters() {
        header("String.splitWithDelimiters (21)");

        String input = "a1b22c333d";

        System.out.println("split(\"\\\\d+\")                = " + Arrays.toString(input.split("\\d+")));
        System.out.println("splitWithDelimiters(\"\\\\d+\", 0) = "
                + Arrays.toString(input.splitWithDelimiters("\\d+", 0)));

        System.out.println();
        System.out.println("The ordinary split throws the separators away. The new one keeps them,");
        System.out.println("interleaved with the parts, which is exactly what a tokeniser needs -");
        System.out.println("and what previously forced people to write a Matcher loop by hand.");
        System.out.println();
        System.out.println("The limit parameter behaves as in split: 0 discards trailing empty");
        System.out.println("strings, a negative value keeps them.");
    }

    private static void emoji() {
        header("Emoji predicates on Character (21)");

        int[] codePoints = {'A', 0x1F600, 0x1F1F5, '9'};
        for (int cp : codePoints) {
            System.out.printf("U+%04X %-3s isEmoji=%-5s isEmojiPresentation=%-5s%n",
                    cp, new String(Character.toChars(cp)),
                    Character.isEmoji(cp), Character.isEmojiPresentation(cp));
        }

        System.out.println();
        System.out.println("Look at the last row: the DIGIT 9 reports isEmoji = true.");
        System.out.println("That is not a bug. Unicode marks 0-9, # and * as Emoji because they are");
        System.out.println("the base characters of keycap sequences like 9 + U+FE0F + U+20E3, which");
        System.out.println("renders as a boxed nine. isEmojiPresentation is false for them, meaning");
        System.out.println("'do not render this as an emoji by default' - and that is the predicate");
        System.out.println("you almost always want when filtering text.");
        System.out.println();
        System.out.println("Six predicates in total, straight from the Unicode emoji properties.");
        System.out.println("Note they take an int CODE POINT, not a char - most emoji are outside");
        System.out.println("the Basic Multilingual Plane and do not fit in a single char.");
        System.out.println("That is also why 'x'.length() on an emoji string is usually 2.");
    }

    private static void futureState() throws Exception {
        header("Future.state / resultNow / exceptionNow (19)");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> ok = executor.submit(() -> "done");
            Future<String> failed = executor.submit(() -> {
                throw new IllegalStateException("boom");
            });

            Thread.sleep(Duration.ofMillis(100));   // let both finish

            System.out.println("ok.state()       = " + ok.state());
            System.out.println("ok.resultNow()   = " + ok.resultNow());
            System.out.println("failed.state()   = " + failed.state());
            System.out.println("failed.exceptionNow() = " + failed.exceptionNow());
        }

        System.out.println();
        System.out.println("state() returns RUNNING, SUCCESS, FAILED or CANCELLED. Before this you");
        System.out.println("had to call get() inside a try/catch just to discover which happened -");
        System.out.println("and get() blocks, which is not what you want when inspecting a batch");
        System.out.println("of futures that have already completed.");
        System.out.println();
        System.out.println("resultNow and exceptionNow never block: they throw IllegalStateException");
        System.out.println("if the future is not in the matching state.");
    }

    private static void sleepWithDuration() {
        header("Thread.sleep(Duration) (19)");

        System.out.println("  Thread.sleep(Duration.ofSeconds(2))     // reads as intended");
        System.out.println("  Thread.sleep(2000)                      // 2000 what?");
        System.out.println();
        System.out.println("Part of a slow, wide cleanup: more and more APIs now accept Duration");
        System.out.println("instead of a bare long plus a TimeUnit. HttpClient (11) already did.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
