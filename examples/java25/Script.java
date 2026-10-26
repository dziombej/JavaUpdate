// Java 25 (JEP 512) - compact source files and instance main methods.
//
// Run it with:   java examples/java25/Script.java
//
// There is no class declaration, no public static void main(String[] args),
// and no System.out. All three were removed for the same reason: they are the
// first things a beginner must type and the last things they can explain.
//
// What the compiler does with this file:
//   - wraps everything in an implicit, unnamed top-level class;
//   - lets main be an INSTANCE method, with or without the String[] parameter;
//   - auto-imports java.lang.IO, so println and readln are available directly;
//   - since Java 25, also auto-imports the java.base module (JEP 511),
//     which is why List and Stream below need no import.

void main() {
    IO.println("=== Compact source files (JEP 512) ===");
    IO.println("");
    IO.println("This file has no class declaration and no static main.");
    IO.println("");

    // java.base is implicitly imported in a compact source file, so these work
    // without a single import statement.
    var cities = List.of("Amsterdam", "Berlin", "Lodz");
    var longest = cities.stream().max(Comparator.comparingInt(String::length)).orElseThrow();

    IO.println("cities  = " + cities);
    IO.println("longest = " + longest);
    IO.println("");

    describeTheRules();
    describeIO();
}

// Ordinary instance methods - no static needed, because main is an instance method
// and the implicit class is instantiated before it is called.
void describeTheRules() {
    IO.println("=== How the launcher picks a main method ===");
    IO.println("Candidates are tried in this order:");
    IO.println("  1. void main(String[] args)   instance");
    IO.println("  2. void main()                instance");
    IO.println("  3. static void main(String[] args)");
    IO.println("  4. static void main()");
    IO.println("");
    IO.println("So a classic 'public static void main(String[] args)' still works");
    IO.println("everywhere it always did. Nothing was taken away.");
    IO.println("");
    IO.println("Rules of an implicit class:");
    IO.println("  - it has no name, so nothing else can refer to it;");
    IO.println("  - it cannot be in a named package;");
    IO.println("  - it may declare fields, methods and nested classes normally;");
    IO.println("  - it always has a default no-arg constructor.");
    IO.println("");
}

void describeIO() {
    IO.println("=== java.lang.IO ===");
    IO.println("Three methods: println(Object), print(Object), readln(String prompt).");
    IO.println("They wrap System.out and System.in, and IO is auto-imported here.");
    IO.println("");
    IO.println("Version history of this feature, because it moved a lot:");
    IO.println("  21 - preview as 'unnamed classes and instance main methods' (JEP 445)");
    IO.println("  22 - second preview (JEP 463)");
    IO.println("  23 - renamed 'implicitly declared classes' (JEP 477), IO added");
    IO.println("  24 - fourth preview (JEP 495)");
    IO.println("  25 - STANDARD as 'compact source files and instance main methods' (JEP 512)");
    IO.println("");
    IO.println("This is what src/main/java/com/tomtom/genesis/platform/Main.java uses -");
    IO.println("the file IntelliJ generated when this project was created.");
}
