package com.tomtom.genesis.platform.jdk12;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Java 12 - the smaller API additions: String.indent, String.transform,
 * Files.mismatch and compact number formatting.
 */
public class SmallAdditions12 {

    public static void main(String[] args) throws IOException {
        indent();
        transform();
        filesMismatch();
        compactNumbers();
    }

    private static void indent() {
        header("String.indent - shift every line by n spaces");

        String block = "first\nsecond\nthird";

        System.out.println("original:");
        System.out.print(block.indent(0));
        System.out.println("indented by 4:");
        System.out.print(block.indent(4));

        System.out.println();
        System.out.println("Two behaviours that surprise people:");
        System.out.println("  1. indent ALWAYS appends a trailing newline, even indent(0).");
        System.out.println("     That is why the calls above use print, not println.");
        System.out.println("  2. A negative argument REMOVES leading whitespace, up to n characters:");
        System.out.print("     " + "      deep".indent(-3));
        System.out.println();
        System.out.println("It also normalises line endings to \\n. Combined with text blocks (Java 15)");
        System.out.println("this is the usual way to re-indent a generated snippet.");
    }

    private static void transform() {
        header("String.transform - turn a chain of calls into a pipeline");

        // transform applies a Function to the string and returns whatever it produces.
        // The value is that the call reads left to right, in execution order.
        int parsed = " 42 ".transform(String::strip).transform(Integer::parseInt);
        System.out.println("\" 42 \".transform(strip).transform(parseInt) = " + parsed);

        String csv = "ala,bartek,celina";
        String result = csv.transform(s -> s.split(","))
                .length + " names";
        System.out.println("split via transform = " + result);

        System.out.println();
        System.out.println("Compare the reading order:");
        System.out.println("  Integer.parseInt(\" 42 \".strip())        // inside-out");
        System.out.println("  \" 42 \".transform(String::strip)          // left to right");
        System.out.println("        .transform(Integer::parseInt)");
        System.out.println();
        System.out.println("Note the signature: transform returns R, not String - so the chain can");
        System.out.println("change type at any step. That is what makes it more than syntactic sugar.");
    }

    private static void filesMismatch() throws IOException {
        header("Files.mismatch - compare two files byte by byte");

        Path dir = Files.createTempDirectory("java12-demo");
        try {
            Path a = Files.writeString(dir.resolve("a.txt"), "same content");
            Path b = Files.writeString(dir.resolve("b.txt"), "same content");
            Path c = Files.writeString(dir.resolve("c.txt"), "same conXent");
            Path d = Files.writeString(dir.resolve("d.txt"), "same content plus more");

            System.out.println("a vs b (identical)      = " + Files.mismatch(a, b));
            System.out.println("a vs c (differ at 8)    = " + Files.mismatch(a, c));
            System.out.println("a vs d (a is a prefix)  = " + Files.mismatch(a, d));

            System.out.println();
            System.out.println("Returns -1 when the files are identical, otherwise the index of the");
            System.out.println("first differing byte. If one file is a prefix of the other, it returns");
            System.out.println("the length of the shorter one.");
            System.out.println();
            System.out.println("It short-circuits on the first difference, so it is far cheaper than");
            System.out.println("Arrays.equals(Files.readAllBytes(a), Files.readAllBytes(b)) - which");
            System.out.println("loads both files into memory before comparing anything.");
        } finally {
            try (var paths = Files.walk(dir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            }
        }
    }

    @SuppressWarnings("deprecation")   // new Locale(...) - see the note at the end of this method
    private static void compactNumbers() {
        header("Compact number formatting");

        NumberFormat shortEn = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        NumberFormat longEn = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.LONG);
        NumberFormat shortPl = NumberFormat.getCompactNumberInstance(new Locale("pl", "PL"), NumberFormat.Style.SHORT);

        long[] values = {999, 1_000, 15_300, 2_400_000, 7_000_000_000L};

        String table = java.util.Arrays.stream(values)
                .mapToObj(v -> String.format("%,15d | %8s | %-16s | %8s",
                        v, shortEn.format(v), longEn.format(v), shortPl.format(v)))
                .collect(Collectors.joining("\n"));

        System.out.println("          value | SHORT en | LONG en          | SHORT pl");
        System.out.println(table);

        System.out.println();
        System.out.println("Useful for dashboards and mobile-width UIs, where '2.4M' beats '2,400,000'.");
        System.out.println("The patterns come from CLDR locale data, so the abbreviations are correct");
        System.out.println("per language rather than hand-rolled.");
        System.out.println();
        System.out.println("Unrelated note worth carrying forward: the new Locale(\"pl\", \"PL\") constructor");
        System.out.println("used above was deprecated in Java 19 in favour of Locale.of(\"pl\", \"PL\").");
        System.out.println("This file deliberately sticks to the Java 12 spelling - see the README note");
        System.out.println("about every example compiling under its own --release.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
