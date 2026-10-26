package com.tomtom.genesis.platform.jdk14;

import java.util.HashMap;
import java.util.Map;

/**
 * Java 14 (JEP 358) - helpful NullPointerException messages.
 *
 * Off by default in 14 (-XX:+ShowCodeDetailsInExceptionMessages),
 * ON BY DEFAULT since Java 15.
 */
public class HelpfulNullPointerExceptions {

    static final class Address {
        private final String city;

        Address(String city) {
            this.city = city;
        }

        String city() {
            return city;
        }
    }

    static final class Customer {
        private final Address address;

        Customer(Address address) {
            this.address = address;
        }

        Address address() {
            return address;
        }
    }

    public static void main(String[] args) {
        chainedCalls();
        arraysAndMaps();
        whatItCannotDo();
    }

    private static void chainedCalls() {
        header("The classic: which part of the chain was null?");

        Customer customer = new Customer(null);

        try {
            System.out.println(customer.address().city().toUpperCase());
        } catch (NullPointerException e) {
            System.out.println("message: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Before Java 14 the message was simply null, and the stack trace gave you");
        System.out.println("a line number. On a line with three dereferences that is not enough -");
        System.out.println("you had to split the line or attach a debugger to find out which one failed.");
        System.out.println();
        System.out.println("The message now names the exact expression AND what you were trying to do");
        System.out.println("with it. Note that it also tells you the return value of address() was null,");
        System.out.println("not the field - the JVM reconstructs this from the bytecode.");
    }

    private static void arraysAndMaps() {
        header("It works for arrays, fields and unboxing too");

        String[][] grid = new String[2][];

        try {
            System.out.println(grid[1][0]);
        } catch (NullPointerException e) {
            System.out.println("array:    " + e.getMessage());
        }

        Map<String, Integer> counts = new HashMap<>();
        try {
            int value = counts.get("missing");   // unboxing null
            System.out.println(value);
        } catch (NullPointerException e) {
            System.out.println("unboxing: " + e.getMessage());
        }

        Customer customer = new Customer(null);
        try {
            System.out.println(customer.address.city);
        } catch (NullPointerException e) {
            System.out.println("field:    " + e.getMessage());
        }

        System.out.println();
        System.out.println("The unboxing one is worth remembering. Map.get returns null for a missing");
        System.out.println("key, and assigning it to an int calls Integer.intValue() on null. That NPE");
        System.out.println("used to point at a line with no visible method call at all.");
    }

    private static void whatItCannotDo() {
        header("Limits");

        System.out.println("1. The message is built from the bytecode, so it needs LOCAL VARIABLE names");
        System.out.println("   from the class file. Code compiled without debug information (javac -g:none)");
        System.out.println("   produces a message with <local1> instead of a real name.");
        System.out.println();
        System.out.println("2. It describes only what the bytecode did - it cannot tell you WHY the");
        System.out.println("   value was null.");
        System.out.println();
        System.out.println("3. It costs a little to compute, which is why it is built lazily, only when");
        System.out.println("   getMessage() is actually called.");
        System.out.println();
        System.out.println("Turn it off if a message might leak sensitive data into logs:");
        System.out.println("  -XX:-ShowCodeDetailsInExceptionMessages");
        System.out.println();
        System.out.println("Version history: opt-in in Java 14, on by default since Java 15.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
