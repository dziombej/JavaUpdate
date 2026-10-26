package com.tomtom.genesis.platform.jdk16;

import java.util.List;

/**
 * Java 16 (JEP 394) - pattern matching for instanceof.
 *
 * Previewed in 14 and 15, standard in 16. A small feature on its own; the
 * foundation for record patterns and pattern matching for switch in Java 21.
 */
public class PatternMatchingForInstanceof {

    public static void main(String[] args) {
        theOldWay();
        theNewWay();
        flowScoping();
        inConditions();
        equalsMethod();
    }

    private static void theOldWay() {
        header("The old three-step dance");

        System.out.println("  if (obj instanceof String) {        // 1. test");
        System.out.println("      String s = (String) obj;        // 2. cast - repeating the type");
        System.out.println("      if (s.length() > 5) { ... }     // 3. finally use it");
        System.out.println("  }");
        System.out.println();
        System.out.println("The type is written twice and the cast cannot fail - the compiler just");
        System.out.println("has no way to know that. It is pure ceremony.");
    }

    private static void theNewWay() {
        header("Type pattern - test, cast and bind in one step");

        for (Object obj : List.of("a longish string", 42, List.of(1, 2, 3), 3.14)) {
            String description;
            if (obj instanceof String s) {
                description = "String of length " + s.length();
            } else if (obj instanceof Integer i && i > 10) {
                description = "Integer greater than ten: " + (i * 2);
            } else if (obj instanceof List<?> list) {
                description = "List with " + list.size() + " elements";
            } else {
                description = "something else: " + obj;
            }
            System.out.println(pad(obj.getClass().getSimpleName()) + " -> " + description);
        }

        System.out.println();
        System.out.println("'s' is a PATTERN VARIABLE. It exists only where the compiler can prove");
        System.out.println("the test succeeded, and it is effectively final within that region.");
    }

    private static void flowScoping() {
        header("Flow scoping - the scope follows the logic, not the braces");

        Object obj = "hello";

        // The && case: the right-hand side only runs when the pattern matched,
        // so the variable is in scope there.
        if (obj instanceof String s && s.length() > 3) {
            System.out.println("&&      : matched, s = " + s);
        }

        // The negated case: after an early return the compiler knows the pattern
        // MUST have matched, so the variable stays in scope for the rest of the method.
        System.out.println("negation: " + describe(obj));
        System.out.println("negation: " + describe(42));

        System.out.println();
        System.out.println("Two rules worth internalising:");
        System.out.println("  obj instanceof String s && s.isEmpty()   // legal - && short-circuits");
        System.out.println("  obj instanceof String s || s.isEmpty()   // ERROR - s may not exist");
        System.out.println();
        System.out.println("This is unlike any other scoping rule in Java: the scope of a pattern");
        System.out.println("variable is computed from the control flow, not from the nearest block.");
    }

    private static String describe(Object obj) {
        if (!(obj instanceof String s)) {
            return "not a string";
        }
        // Reaching this line proves obj is a String, so s is in scope here.
        return "string of length " + s.length() + ": " + s.toUpperCase();
    }

    private static void inConditions() {
        header("Where it pays off most: equals and guard clauses");

        System.out.println("A hand-written equals used to look like this:");
        System.out.println("  @Override public boolean equals(Object o) {");
        System.out.println("      if (this == o) return true;");
        System.out.println("      if (o == null || getClass() != o.getClass()) return false;");
        System.out.println("      Person other = (Person) o;");
        System.out.println("      return age == other.age && Objects.equals(name, other.name);");
        System.out.println("  }");
        System.out.println();
        System.out.println("and now collapses to:");
        System.out.println("  @Override public boolean equals(Object o) {");
        System.out.println("      return o instanceof Person p && age == p.age && Objects.equals(name, p.name);");
        System.out.println("  }");
        System.out.println();
        System.out.println("Note that instanceof already returns false for null, so the null check");
        System.out.println("disappears too.");
    }

    private static void equalsMethod() {
        header("Proof that the short form behaves");

        Person a = new Person("Ala", 30);
        Person b = new Person("Ala", 30);
        Person c = new Person("Bartek", 30);

        System.out.println("a.equals(b)     = " + a.equals(b));
        System.out.println("a.equals(a)     = " + a.equals(a));
        System.out.println("a.equals(c)     = " + a.equals(c));
        System.out.println("a.equals(null)  = " + a.equals(null));
        System.out.println("a.equals(\"Ala\") = " + a.equals("Ala"));

        System.out.println();
        System.out.println("In practice you would write this as a record and get equals for free -");
        System.out.println("see Records.java. The hand-written version is here to show the pattern.");
    }

    static final class Person {
        private final String name;
        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Person p && age == p.age && java.util.Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age);
        }
    }

    private static String pad(String s) {
        return String.format("%-12s", s);
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
