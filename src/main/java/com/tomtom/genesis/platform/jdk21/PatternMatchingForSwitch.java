package com.tomtom.genesis.platform.jdk21;

import java.util.List;

/**
 * Java 21 (JEP 441) - pattern matching for switch.
 *
 * Previewed across 17, 18, 19 and 20; standard in 21. switch can now match on
 * TYPES and PATTERNS, not just constants - and the compiler checks exhaustiveness.
 */
public class PatternMatchingForSwitch {

    sealed interface Event permits Login, Logout, Purchase, Error { }

    record Login(String user) implements Event { }

    record Logout(String user, int sessionMinutes) implements Event { }

    record Purchase(String user, double amount) implements Event { }

    record Error(String message, int code) implements Event { }

    public static void main(String[] args) {
        typePatterns();
        guards();
        nullHandling();
        exhaustiveness();
        theOldEquivalent();
    }

    private static void typePatterns() {
        header("Type patterns in switch");

        for (Object value : List.of(42, "text", 3.14, List.of(1, 2), 'x')) {
            String description = switch (value) {
                case Integer i -> "int " + i + ", doubled " + (i * 2);
                case String s -> "string of length " + s.length();
                case Double d -> "double " + d;
                case List<?> l -> "list of " + l.size();
                default -> "something else: " + value.getClass().getSimpleName();
            };
            System.out.printf("%-8s -> %s%n", value, description);
        }

        System.out.println();
        System.out.println("This replaces the if/else-if chain of instanceof patterns from Java 16.");
        System.out.println("The compiler can also optimise it into a jump rather than a linear");
        System.out.println("sequence of type checks, using the invokedynamic-based typeSwitch bootstrap.");
    }

    private static void guards() {
        header("Guarded patterns with when");

        for (Event event : List.of(
                new Purchase("ala", 15.0),
                new Purchase("bartek", 5000.0),
                new Logout("celina", 3),
                new Logout("dorota", 240),
                new Error("timeout", 504),
                new Error("not found", 404))) {

            String action = switch (event) {
                case Purchase p when p.amount() > 1000 -> "flag for manual review: " + p.amount();
                case Purchase p -> "process normally: " + p.amount();
                case Logout l when l.sessionMinutes() > 120 -> "long session, ask for feedback";
                case Logout l -> "ordinary logout after " + l.sessionMinutes() + " min";
                case Error e when e.code() >= 500 -> "page the on-call engineer: " + e.message();
                case Error e -> "log and continue: " + e.message();
                case Login l -> "welcome " + l.user();
            };
            System.out.printf("%-34s -> %s%n", event, action);
        }

        System.out.println();
        System.out.println("ORDER MATTERS. Cases are tried top to bottom, so the guarded case must");
        System.out.println("come before the unguarded one for the same type. Put them the other way");
        System.out.println("round and the compiler rejects it: 'this case label is dominated by a");
        System.out.println("preceding case label' - a genuinely useful error.");
        System.out.println();
        System.out.println("'when' is a contextual keyword, so a variable named when still compiles.");
    }

    private static void nullHandling() {
        header("null: the rule that finally changed");

        Object value = null;

        // A traditional switch throws NullPointerException on a null selector.
        // A pattern switch lets you handle null explicitly with 'case null'.
        String result = switch (value) {
            case null -> "it was null";
            case String s -> "string " + s;
            default -> "something else";
        };
        System.out.println("switch on null with 'case null'  -> " + result);

        // Without 'case null' the old behaviour is preserved - it throws.
        try {
            String ignored = switch ((Object) null) {
                case String s -> "string";
                default -> "other";
            };
        } catch (NullPointerException e) {
            System.out.println("switch on null without it        -> NullPointerException");
        }

        // The combined label, for when null should share the default branch.
        String combined = switch (value) {
            case Integer i -> "int";
            case null, default -> "null or anything else";
        };
        System.out.println("case null, default               -> " + combined);

        System.out.println();
        System.out.println("This is the deliberate compromise: existing switches keep throwing on");
        System.out.println("null, so nothing silently changes behaviour, but you can now opt in to");
        System.out.println("handling null inside the switch instead of guarding it outside.");
    }

    private static void exhaustiveness() {
        header("Exhaustiveness over a sealed hierarchy");

        Event event = new Login("ala");

        // No default branch. The compiler verifies all four permitted types are covered.
        String user = switch (event) {
            case Login l -> l.user();
            case Logout l -> l.user();
            case Purchase p -> p.user();
            case Error e -> "(system)";
        };
        System.out.println("user for " + event + " = " + user);

        System.out.println();
        System.out.println("Remove one case and it will not compile. Add a fifth Event type and");
        System.out.println("every switch like this one stops compiling until you handle it.");
        System.out.println();
        System.out.println("A subtlety worth knowing: an exhaustive switch without a default still");
        System.out.println("gets a hidden fallback that throws MatchException. It fires only if the");
        System.out.println("sealed hierarchy changed AFTER this class was compiled - a class-file");
        System.out.println("version skew. You will never see it in a consistent build.");
    }

    private static void theOldEquivalent() {
        header("What this replaces");

        System.out.println("The visitor pattern. Every 'operation over a closed set of types' used to");
        System.out.println("mean an interface with one accept method, an visitor interface with one");
        System.out.println("visit method per type, and an implementation per operation.");
        System.out.println();
        System.out.println("A sealed interface plus a pattern switch expresses the same thing in a");
        System.out.println("fraction of the code, with the exhaustiveness guarantee the visitor");
        System.out.println("pattern was invented to provide.");
        System.out.println();
        System.out.println("The trade-off is the classic expression problem, and it has not gone away:");
        System.out.println("  - visitor  : easy to add an operation, hard to add a type;");
        System.out.println("  - sealed + switch: easy to add a type (the compiler finds every switch),");
        System.out.println("                     and operations live outside the hierarchy.");
        System.out.println("Use sealed types when the SET OF TYPES is stable and the set of operations");
        System.out.println("keeps growing - which describes most domain modelling.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
