package com.tomtom.genesis.platform.jdk14;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Java 14 (JEP 361) - switch expressions.
 *
 * Previewed in 12 and 13, standard in 14. Four separate improvements arrived
 * in one feature: arrow labels, switch as an expression, multiple constants
 * per label, and exhaustiveness checking.
 */
public class SwitchExpressions {

    enum Status { NEW, IN_PROGRESS, DONE, CANCELLED }

    public static void main(String[] args) {
        theOldForm();
        arrowLabels();
        switchAsExpression();
        yieldKeyword();
        exhaustiveness();
    }

    private static void theOldForm() {
        header("What the old switch got wrong");

        System.out.println("  switch (day) {");
        System.out.println("      case MONDAY:");
        System.out.println("      case TUESDAY:");
        System.out.println("          result = \"early week\";");
        System.out.println("          break;              // forget this and you fall through");
        System.out.println("      case SATURDAY:");
        System.out.println("          result = \"weekend\";");
        System.out.println("          break;");
        System.out.println("      default:");
        System.out.println("          result = \"other\";");
        System.out.println("  }");
        System.out.println();
        System.out.println("Three concrete problems:");
        System.out.println("  1. Fall-through is the DEFAULT. A missing break is a silent bug.");
        System.out.println("  2. It is a statement, so producing a value needs a mutable variable");
        System.out.println("     declared outside and assigned in every branch.");
        System.out.println("  3. Every case shares one scope - a variable declared in one case is");
        System.out.println("     visible in the others.");
    }

    private static void arrowLabels() {
        header("Arrow labels - no fall-through, no break");

        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.WEDNESDAY)) {
            switch (day) {
                case MONDAY, TUESDAY -> System.out.println(day + " -> early week");
                case SATURDAY, SUNDAY -> System.out.println(day + " -> weekend");
                default -> System.out.println(day + " -> other");
            }
        }

        System.out.println();
        System.out.println("Only the matching branch runs. There is no break and no fall-through -");
        System.out.println("not as a convention, but as a language rule. Several constants share");
        System.out.println("one label via a comma.");
        System.out.println();
        System.out.println("Each arrow branch also gets its OWN scope, so this now compiles:");
        System.out.println("  case A -> { int x = 1; ... }");
        System.out.println("  case B -> { int x = 2; ... }   // same name, different scope");
    }

    private static void switchAsExpression() {
        header("switch as an expression - it produces a value");

        for (Status status : Status.values()) {
            int priority = switch (status) {
                case NEW -> 1;
                case IN_PROGRESS -> 2;
                case DONE, CANCELLED -> 0;
            };
            System.out.println(status + " -> priority " + priority);
        }

        System.out.println();
        System.out.println("The variable can now be final, assigned exactly once:");
        System.out.println("  int priority = switch (status) { ... };");
        System.out.println();
        System.out.println("Note the semicolon after the closing brace - it is an expression,");
        System.out.println("so the assignment statement still needs to be terminated.");
    }

    private static void yieldKeyword() {
        header("yield - returning a value from a block branch");

        for (Status status : List.of(Status.NEW, Status.DONE)) {
            String description = switch (status) {
                case NEW -> "just created";
                case IN_PROGRESS -> "being worked on";
                case DONE, CANCELLED -> {
                    // A block branch needs an explicit yield to produce its value.
                    String suffix = status == Status.DONE ? "successfully" : "by the user";
                     yield "finished " + suffix;
                }
            };
            System.out.println(status + " -> " + description);
        }

        System.out.println();
        System.out.println("yield is to a switch expression what return is to a method: it supplies");
        System.out.println("the value of the enclosing switch, not of the method.");
        System.out.println();
        System.out.println("yield is a RESTRICTED IDENTIFIER, not a keyword - just like var.");
        System.out.println("Existing code with a variable or method named 'yield' still compiles.");
        System.out.println();
        System.out.println("The old colon form can also yield, which is how mixed code migrates:");
        System.out.println("  case NEW: yield 1;");
        System.out.println("But you cannot mix arrow and colon labels in one switch.");
    }

    private static void exhaustiveness() {
        header("Exhaustiveness - the compiler checks the cases for you");

        System.out.println("A switch EXPRESSION must cover every possible value. Over an enum the");
        System.out.println("compiler verifies this and rejects an incomplete switch:");
        System.out.println();
        System.out.println("  int p = switch (status) {");
        System.out.println("      case NEW -> 1;");
        System.out.println("      case IN_PROGRESS -> 2;");
        System.out.println("  };   // error: the switch expression does not cover all possible input values");
        System.out.println();
        System.out.println("This is the most valuable part of the feature. Add a constant to the enum");
        System.out.println("and every exhaustive switch over it stops compiling - you get a list of");
        System.out.println("places to update instead of a runtime surprise.");
        System.out.println();
        System.out.println("Practical advice: over an enum, deliberately OMIT the default branch.");
        System.out.println("A default swallows new constants and hands you the fallback value at");
        System.out.println("runtime; without it, the compiler tells you at build time.");
        System.out.println();
        System.out.println("A switch STATEMENT (one that produces no value) is not required to be");
        System.out.println("exhaustive - the rule applies to expressions only.");
        System.out.println();
        System.out.println("Java 21 extends all of this to types and patterns - see docs/java21.md.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
