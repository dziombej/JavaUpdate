package com.tomtom.genesis.platform.jdk18;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java 18 (JEP 400) - UTF-8 by default.
 *
 * Arguably the most useful "invisible" change of the last decade: the default
 * charset of the Java platform is now UTF-8 everywhere, instead of depending
 * on the operating system's locale.
 */
public class Utf8ByDefault {

    public static void main(String[] args) throws IOException {
        theOldBehaviour();
        whatChanged();
        provingIt();
        migrationNotes();
    }

    private static void theOldBehaviour() {
        header("The bug this fixes");

        System.out.println("Up to Java 17, APIs that did not take an explicit Charset used");
        System.out.println("'the default charset', derived from the OS locale:");
        System.out.println();
        System.out.println("  Linux / macOS with LANG=en_US.UTF-8  -> UTF-8");
        System.out.println("  Windows, Polish locale               -> windows-1250");
        System.out.println("  Windows, Japanese locale             -> windows-31j");
        System.out.println("  a container with no LANG set         -> US-ASCII (!)");
        System.out.println();
        System.out.println("So this line read the same file differently on different machines:");
        System.out.println("  new FileReader(\"data.txt\")");
        System.out.println();
        System.out.println("That is the mechanism behind the classic bug report: 'accented characters");
        System.out.println("are fine locally and broken in the Docker image'. Nothing in the code");
        System.out.println("differed - only the environment did.");
    }

    private static void whatChanged() {
        header("What Java 18 changed");

        System.out.println("Charset.defaultCharset()                  = " + Charset.defaultCharset());
        System.out.println("System.getProperty(\"file.encoding\")       = " + System.getProperty("file.encoding"));
        System.out.println("System.getProperty(\"native.encoding\")     = " + System.getProperty("native.encoding"));
        System.out.println("System.getProperty(\"stdout.encoding\")     = " + System.getProperty("stdout.encoding"));

        System.out.println();
        System.out.println("file.encoding is now UTF-8 regardless of the platform locale.");
        System.out.println();
        System.out.println("native.encoding is NEW in Java 18. It reports what the OS actually uses,");
        System.out.println("which is what you want when talking to native tools - a process you spawn,");
        System.out.println("a console, a file produced by another program on the same machine.");
        System.out.println();
        System.out.println("stdout.encoding / stderr.encoding (Java 19) are separate on purpose:");
        System.out.println("a Windows console may still be cp852 even though files are UTF-8.");
    }

    private static void provingIt() throws IOException {
        header("Proof on a real file");

        Path dir = Files.createTempDirectory("java18-demo");
        try {
            Path file = dir.resolve("polish.txt");
            String text = "zazolc gesla jazn - zażółć gęślą jaźń";

            // Written with an explicit charset, so we know exactly what is on disk.
            Files.writeString(file, text, StandardCharsets.UTF_8);
            System.out.println("bytes on disk        = " + Files.size(file) + " (chars: " + text.length() + ")");

            // No charset argument anywhere below - these all use the default.
            System.out.println("Files.readString     = " + Files.readString(file));

            try (FileReader reader = new FileReader(file.toFile())) {
                char[] buffer = new char[256];
                int read = reader.read(buffer);
                System.out.println("new FileReader       = " + new String(buffer, 0, read));
            }

            Path out = dir.resolve("written.txt");
            try (PrintWriter writer = new PrintWriter(out.toFile())) {
                writer.print(text);
            }
            System.out.println("round trip via PrintWriter -> " + Files.readString(out, StandardCharsets.UTF_8));

            System.out.println();
            System.out.println("On Java 17 and earlier, running this on a machine with a non-UTF-8");
            System.out.println("locale would have produced mojibake in the FileReader and PrintWriter");
            System.out.println("lines while Files.readString stayed correct - because that one has");
            System.out.println("always been UTF-8 (see docs/java11.md).");
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

    private static void migrationNotes() {
        header("Migration notes");

        System.out.println("This change can BREAK working systems, which is why it took until 18.");
        System.out.println("If an application wrote files with the platform charset on Java 11 and");
        System.out.println("reads them back on Java 18, the encodings no longer match.");
        System.out.println();
        System.out.println("Escape hatches, in order of preference:");
        System.out.println("  1. Pass an explicit Charset to every API that accepts one. Permanent fix.");
        System.out.println("  2. -Dfile.encoding=COMPAT      restores the Java 17 behaviour exactly.");
        System.out.println("  3. -Dfile.encoding=windows-1250  pins one specific legacy charset.");
        System.out.println();
        System.out.println("There is no compiler flag that finds the risky call sites - the");
        System.out.println("practical tool is a grep for the constructors that take no charset:");
        System.out.println("  FileReader, FileWriter, InputStreamReader, OutputStreamWriter,");
        System.out.println("  PrintWriter, PrintStream, Formatter, Scanner, String.getBytes(),");
        System.out.println("  new String(byte[]), URLEncoder/URLDecoder, Properties.load(InputStream).");
        System.out.println();
        System.out.println("Note that javac itself was NOT changed by JEP 400 - source files are read");
        System.out.println("with the platform charset unless you pass -encoding UTF-8. Maven's");
        System.out.println("project.build.sourceEncoding handles that, and this project sets it.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
