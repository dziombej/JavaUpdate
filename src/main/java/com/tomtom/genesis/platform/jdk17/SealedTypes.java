package com.tomtom.genesis.platform.jdk17;

import java.util.Arrays;
import java.util.List;

/**
 * Java 17 (JEP 409) - sealed classes and interfaces.
 *
 * Previewed in 15 and 16, standard in 17. A sealed type declares exactly which
 * types may extend or implement it - a middle ground between final (nobody) and
 * public (anybody).
 */
public class SealedTypes {

    /**
     * The permits clause is the whole feature: this hierarchy is closed and the
     * compiler knows every member of it.
     */
    sealed interface Shape permits Circle, Rectangle, Triangle { }

    record Circle(double radius) implements Shape { }

    record Rectangle(double width, double height) implements Shape { }

    record Triangle(double base, double height) implements Shape { }

    /**
     * Each permitted subtype must choose one of three modifiers.
     * final - closes the hierarchy here.
     * sealed - continues it under a new permits clause.
     * non-sealed - reopens it to anyone.
     */
    sealed interface Payment permits Card, Transfer, Cash { }

    record Card(String number) implements Payment { }

    /** sealed: the hierarchy continues, still closed. */
    sealed interface Transfer extends Payment permits Sepa, Swift { }

    record Sepa(String iban) implements Transfer { }

    record Swift(String bic) implements Transfer { }

    /** non-sealed: anyone may extend Cash - the seal stops here. */
    non-sealed interface Cash extends Payment { }

    public static void main(String[] args) {
        whyItExists();
        exhaustiveSwitch();
        theThreeModifiers();
        reflection();
        rules();
    }

    private static void whyItExists() {
        header("The problem it solves");

        System.out.println("Before sealed types, modelling 'a Shape is exactly one of these three'");
        System.out.println("was impossible to express. Your options were:");
        System.out.println("  - a public interface - anyone can add a fourth shape you never handle;");
        System.out.println("  - a package-private interface - closed, but then it cannot be part");
        System.out.println("    of your public API at all;");
        System.out.println("  - an enum - closed, but each constant cannot carry different data.");
        System.out.println();
        System.out.println("sealed gives you the third thing: public to use, closed to extend.");
    }

    private static void exhaustiveSwitch() {
        header("The payoff: the compiler can check a switch is complete");

        List<Shape> shapes = List.of(
                new Circle(2),
                new Rectangle(3, 4),
                new Triangle(6, 2));

        for (Shape shape : shapes) {
            // No default branch, and it compiles - the compiler knows the three
            // permitted subtypes and can see all of them are covered.
            double area = switch (shape) {
                case Circle c -> Math.PI * c.radius() * c.radius();
                case Rectangle r -> r.width() * r.height();
                case Triangle t -> t.base() * t.height() / 2;
            };
            System.out.printf("%-28s area = %.2f%n", shape, area);
        }

        System.out.println();
        System.out.println("Remove one case and compilation fails with");
        System.out.println("  'the switch expression does not cover all possible input values'.");
        System.out.println();
        System.out.println("Add a fourth shape to the permits clause and every exhaustive switch");
        System.out.println("over Shape stops compiling. You get a task list at build time instead");
        System.out.println("of an UnsupportedOperationException in production.");
        System.out.println();
        System.out.println("Deliberately do NOT add a default branch here - a default makes the");
        System.out.println("switch trivially exhaustive and throws that guarantee away.");
        System.out.println();
        System.out.println("Note this example uses pattern matching for switch, which is standard");
        System.out.println("only from Java 21. In Java 17 itself the switch above was a preview");
        System.out.println("feature; sealed types alone were standard.");
    }

    private static void theThreeModifiers() {
        header("final / sealed / non-sealed");

        List<Payment> payments = List.of(
                new Card("4111..."),
                new Sepa("PL61..."),
                new Swift("DEUTDEFF"));

        for (Payment p : payments) {
            String label = switch (p) {
                case Card c -> "card ending " + c.number().substring(c.number().length() - 3);
                case Sepa s -> "SEPA " + s.iban();
                case Swift s -> "SWIFT " + s.bic();
                case Cash ignored -> "cash";
            };
            System.out.println(label);
        }

        System.out.println();
        System.out.println("Payment permits Card, Transfer, Cash:");
        System.out.println("  Card     is a record -> implicitly final, hierarchy closed here");
        System.out.println("  Transfer is sealed   -> continues, permitting Sepa and Swift");
        System.out.println("  Cash     is non-sealed -> reopened; anyone may implement it");
        System.out.println();
        System.out.println("Because Cash is non-sealed, the switch above MUST handle it - the");
        System.out.println("compiler cannot enumerate its subtypes. That is the price of reopening.");
    }

    private static void reflection() {
        header("Reflection support");

        System.out.println("Shape.class.isSealed()            = " + Shape.class.isSealed());
        System.out.println("Shape.class.getPermittedSubclasses() =");
        Arrays.stream(Shape.class.getPermittedSubclasses())
                .forEach(c -> System.out.println("    " + c.getSimpleName()));
        System.out.println("Circle.class.isRecord()           = " + Circle.class.isRecord());
        System.out.println("Cash.class.isSealed()             = " + Cash.class.isSealed());

        System.out.println();
        System.out.println("The permits list lives in a PermittedSubclasses attribute in the class");
        System.out.println("file, so the JVM enforces it at class-load time - not just the compiler.");
        System.out.println("Generating a subclass at runtime with ASM or Byte Buddy fails too.");
    }

    private static void rules() {
        header("Rules to remember");

        System.out.println("  - Every permitted subtype must be final, sealed or non-sealed.");
        System.out.println("    Omitting the modifier is a compile error - there is no default.");
        System.out.println("  - Permitted subtypes must be in the same MODULE, or, for code on the");
        System.out.println("    classpath, in the same PACKAGE.");
        System.out.println("  - If all subtypes live in the same source file, the permits clause");
        System.out.println("    may be omitted - the compiler infers it.");
        System.out.println("  - sealed and non-sealed are CONTEXTUAL keywords, so existing code with");
        System.out.println("    a variable named 'sealed' still compiles. ('non-sealed' is the only");
        System.out.println("    hyphenated keyword in the language.)");
        System.out.println();
        System.out.println("The combination that makes this worth learning:");
        System.out.println("  sealed interface + records + pattern matching for switch");
        System.out.println("gives you algebraic data types - see docs/java21.md.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
