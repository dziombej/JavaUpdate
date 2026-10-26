package com.tomtom.genesis.platform.jdk25;

// Java 25 (JEP 511) - a module import declaration. This single line makes every
// package EXPORTED by java.base available, as if each had its own import-on-demand.
// That covers java.util, java.util.stream, java.util.function, java.nio.file,
// java.time, java.net, java.math, java.text and around fifty more.
import module java.base;

/**
 * Java 25 (JEP 511) - module import declarations.
 *
 * Previewed in 23 and 24, standard in 25. Aimed at scripts, teaching material
 * and small programs, where the import block is often longer than the code.
 */
public class ModuleImports {

    public static void main(String[] args) throws Exception {
        theSyntax();
        whatItCovers();
        conflicts();
        whenToUse();
    }

    private static void theSyntax() {
        // None of these types are imported by name anywhere in this file.
        List<String> cities = List.of("Amsterdam", "Berlin", "Lodz");
        Map<String, Integer> lengths = cities.stream()
                .collect(Collectors.toMap(Function.identity(), String::length));
        Path temp = Path.of(System.getProperty("java.io.tmpdir"));
        Duration timeout = Duration.ofSeconds(30);
        BigDecimal price = new BigDecimal("19.99");

        header("One import line, many packages");

        System.out.println("List / Map      (java.util)          = " + cities + " " + new TreeMap<>(lengths));
        System.out.println("Collectors      (java.util.stream)   = used above");
        System.out.println("Function        (java.util.function) = used above");
        System.out.println("Path            (java.nio.file)      = " + temp.getFileName());
        System.out.println("Duration        (java.time)          = " + timeout);
        System.out.println("BigDecimal      (java.math)          = " + price);

        System.out.println();
        System.out.println("The alternative, written out:");
        System.out.println("  import java.util.*;");
        System.out.println("  import java.util.stream.*;");
        System.out.println("  import java.util.function.*;");
        System.out.println("  import java.nio.file.*;");
        System.out.println("  import java.time.*;");
        System.out.println("  import java.math.*;");
    }

    private static void whatItCovers() {
        header("What 'import module X' actually means");

        System.out.println("It is equivalent to an on-demand import of every package that module X");
        System.out.println("EXPORTS - and, transitively, of everything X re-exports with");
        System.out.println("'requires transitive'.");
        System.out.println();
        System.out.println("Consequences worth knowing:");
        System.out.println("  - 'import module java.se' pulls in essentially the whole Java SE API,");
        System.out.println("    because java.se requires transitive every other SE module;");
        System.out.println("  - a package the module does not export stays invisible, exactly as");
        System.out.println("    with any other access to a module (see docs/java9.md);");
        System.out.println("  - it works for YOUR modules too: 'import module com.example.core'.");
        System.out.println();
        System.out.println("It does NOT add a dependency. In a modular project you still need");
        System.out.println("'requires' in module-info.java; this only affects name resolution.");
    }

    private static void conflicts() {
        header("The ambiguity problem, and how to fix it");

        System.out.println("Importing many packages at once makes clashes more likely. The famous");
        System.out.println("pair is java.util.List and java.awt.List:");
        System.out.println();
        System.out.println("  import module java.base;     // brings java.util.List");
        System.out.println("  import module java.desktop;  // brings java.awt.List");
        System.out.println("  List<String> x = ...;        // error: reference to List is ambiguous");
        System.out.println();
        System.out.println("The fix is a single-type import, which always wins over any on-demand");
        System.out.println("or module import:");
        System.out.println();
        System.out.println("  import module java.base;");
        System.out.println("  import module java.desktop;");
        System.out.println("  import java.util.List;       // resolves the ambiguity");
        System.out.println();
        System.out.println("Resolution order, strongest first:");
        System.out.println("  1. single-type imports        import java.util.List;");
        System.out.println("  2. on-demand package imports  import java.util.*;");
        System.out.println("  3. module imports             import module java.base;");
        System.out.println();
        System.out.println("Also note: java.util.Date and java.sql.Date, java.util.Timer and");
        System.out.println("javax.swing.Timer are the other pairs that bite in practice.");
    }

    private static void whenToUse() {
        header("Where this belongs - and where it does not");

        System.out.println("Designed for:");
        System.out.println("  - single-file programs run with 'java Script.java';");
        System.out.println("  - JShell sessions and teaching examples;");
        System.out.println("  - prototypes and throwaway tools.");
        System.out.println();
        System.out.println("It pairs deliberately with compact source files (JEP 512), where an");
        System.out.println("implicit class already removes the boilerplate around main -");
        System.out.println("see examples/java25/Script.java in this repository.");
        System.out.println();
        System.out.println("For a production codebase, prefer explicit imports. A reader who sees");
        System.out.println("'Duration' should be able to tell where it comes from without consulting");
        System.out.println("the module graph, and most teams' style guides already ban wildcard");
        System.out.println("imports for the same reason.");
        System.out.println();
        System.out.println("This file uses it to demonstrate the feature, not as a recommendation.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
