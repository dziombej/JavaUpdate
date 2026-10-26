package com.tomtom.genesis.platform.jdk15;

/**
 * Java 15 (JEP 378) - text blocks.
 *
 * Previewed in 13 and 14, standard in 15. Together with them came three String
 * methods that only really make sense alongside text blocks: formatted(),
 * stripIndent() and translateEscapes().
 */
public class TextBlocks {

    public static void main(String[] args) {
        theProblem();
        basics();
        incidentalWhitespace();
        trailingNewline();
        escapes();
        companionMethods();
        noInterpolation();
    }

    private static void theProblem() {
        header("The problem");

        String json = "{\n"
                + "  \"name\": \"Ala\",\n"
                + "  \"city\": \"Lodz\"\n"
                + "}";
        System.out.println(json);
        System.out.println();
        System.out.println("Every quote is escaped, every line ends with \\n\" +, and the shape of the");
        System.out.println("real content is invisible. SQL and HTML are worse still.");
    }

    private static void basics() {
        header("The same thing as a text block");

        String json = """
                  {
                  "name": "Ala",
                  "city": "Lodz"
                }
                """;
        System.out.println(json);

        System.out.println();
        System.out.println("Rules of the syntax:");
        System.out.println("  - opens with three double quotes followed by a LINE TERMINATOR;");
        System.out.println("    \"\"\"content\"\"\" on one line is a compile error;");
        System.out.println("  - a double quote inside needs no escaping;");
        System.out.println("  - the result is an ordinary String - same class, same interning rules,");
        System.out.println("    no new type. The compiler does all the work.");
    }

    private static void incidentalWhitespace() {
        header("Incidental whitespace - the part that trips everyone up");

        // The closing delimiter sits at column 16, the content at column 16 too,
        // so nothing is stripped beyond the shared indentation.
        String aligned = """
                line one
                line two""";

        // Here the closing delimiter is pulled LEFT of the content, so the
        // content keeps four spaces of indentation.
        String indented = """
                    line one
                    line two
                """;

        System.out.println("aligned:");
        System.out.println("[" + aligned + "]");
        System.out.println("indented (closing delimiter moved left):");
        System.out.println("[" + indented + "]");

        System.out.println();
        System.out.println("The algorithm: take every non-blank content line AND the line holding the");
        System.out.println("closing delimiter, find the smallest leading-whitespace count among them,");
        System.out.println("and remove that many characters from every line.");
        System.out.println();
        System.out.println("Consequence: the position of the closing \"\"\" is not cosmetic. Moving it");
        System.out.println("left adds indentation to your string. This is the single most common");
        System.out.println("source of surprise with text blocks.");
        System.out.println();
        System.out.println("Trailing whitespace on each line is always stripped, which is why you");
        System.out.println("cannot accidentally ship invisible spaces at the end of a line.");
    }

    private static void trailingNewline() {
        header("Whether the string ends with a newline");

        String withNewline = """
                content
                """;
        String withoutNewline = """
                content""";

        System.out.println("closing \"\"\" on its own line : " + withNewline.length() + " chars -> "
                + withNewline.replace("\n", "\\n"));
        System.out.println("closing \"\"\" after content   : " + withoutNewline.length() + " chars -> "
                + withoutNewline.replace("\n", "\\n"));

        System.out.println();
        System.out.println("Put the closing delimiter on its own line and the block ends with \\n.");
        System.out.println("Put it right after the last character and it does not. For a file you are");
        System.out.println("writing out you usually want the newline; for a value you are comparing");
        System.out.println("in a test you usually do not.");
    }

    private static void escapes() {
        header("Two escapes that exist only for text blocks");

        // \ at end of line: join this line with the next, no newline inserted.
        String joined = """
                this is one long line \
                continued here \
                and here""";

        // \s: a space that survives trailing-whitespace stripping.
        String padded = """
                column1\s\s\s
                column2\s\s\s""";

        System.out.println("line continuation with backslash:");
        System.out.println("  [" + joined + "]");
        System.out.println("explicit spaces with \\s:");
        System.out.println("  [" + padded.replace("\n", "\\n") + "]");

        System.out.println();
        System.out.println("\\  at end of line - suppresses the line break. Useful for a long SQL");
        System.out.println("   statement you want to read on several lines but send as one.");
        System.out.println("\\s - a literal space that is NOT removed by trailing-whitespace stripping.");
        System.out.println("     It translates to a plain space; it is a marker for the compiler.");
        System.out.println();
        System.out.println("All the familiar escapes still work: \\n, \\t, \\\" and \\\\.");
        System.out.println("If you need three literal quotes in a row, escape one of them: \\\"\"\"");
    }

    private static void companionMethods() {
        header("String.formatted, stripIndent, translateEscapes");

        String template = """
                Customer: %s
                City:     %s
                """;
        System.out.print(template.formatted("Ala", "Lodz"));

        System.out.println();
        System.out.println("formatted(args) is String.format(this, args) with the receiver and the");
        System.out.println("format string swapped. That matters for readability: the format sits at");
        System.out.println("the top as a block and the arguments follow the closing delimiter,");
        System.out.println("instead of the whole block being buried inside a String.format(...) call.");

        String messy = "    line one\n    line two\n";
        System.out.println();
        System.out.println("stripIndent on a NORMAL string (same algorithm as a text block):");
        System.out.println("  before = [" + messy.replace("\n", "\\n") + "]");
        System.out.println("  after  = [" + messy.stripIndent().replace("\n", "\\n") + "]");

        String escaped = "a\\tb\\nc";
        System.out.println();
        System.out.println("translateEscapes turns escape SEQUENCES into the characters they denote:");
        System.out.println("  before = [" + escaped + "]        (backslash-t as two characters)");
        System.out.println("  after  = [" + escaped.translateEscapes().replace("\t", "<TAB>")
                .replace("\n", "<NL>") + "]");
        System.out.println();
        System.out.println("That is what you want when escape sequences arrive from a config file or");
        System.out.println("user input at runtime, where the compiler never saw them.");
    }

    private static void noInterpolation() {
        header("What text blocks are NOT");

        System.out.println("There is no variable interpolation. This does nothing special:");
        System.out.println("  String s = \"\"\"");
        System.out.println("             Hello ${name}\"\"\";      // ${name} is literal text");
        System.out.println();
        System.out.println("Use formatted() or concatenation instead.");
        System.out.println();
        System.out.println("Java 21 previewed String Templates (JEP 430) with a STR.\"Hello \\{name}\"");
        System.out.println("syntax, kept it in 22 - and then WITHDREW it in Java 23. The design was");
        System.out.println("reconsidered and the feature is not in any current release. If you read");
        System.out.println("a tutorial showing STR.\"...\", it describes a preview that no longer exists.");
        System.out.println();
        System.out.println("Text blocks are also not a new type. The result is a String, produced at");
        System.out.println("compile time, so it can be a constant, used in a switch, and interned.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
