package com.tomtom.genesis.platform.jdk25;

/**
 * Java 25 (JEP 513) - flexible constructor bodies.
 *
 * Previewed in 22, 23 and 24; standard in 25. Statements may now appear BEFORE
 * an explicit this(...) or super(...) call, as long as they do not touch the
 * object under construction.
 */
public class FlexibleConstructorBodies {

    // ------------------------------------------------------------------
    // 1. Validating arguments before handing them to the superclass
    // ------------------------------------------------------------------

    static class Temperature {
        protected final double kelvin;

        Temperature(double kelvin) {
            System.out.println("    Temperature(" + kelvin + ") running");
            this.kelvin = kelvin;
        }
    }

    static class Celsius extends Temperature {
        Celsius(double degrees) {
            // Before Java 22 this validation had to be inlined into the super call
            // or hidden in a static helper method.
            if (degrees < -273.15) {
                throw new IllegalArgumentException("below absolute zero: " + degrees);
            }
            System.out.println("  validated " + degrees + " before calling super");
            super(degrees + 273.15);
            System.out.println("  Celsius constructor finished");
        }
    }

    // ------------------------------------------------------------------
    // 2. Computing a value once instead of repeating an expression
    // ------------------------------------------------------------------

    static class Matrix {
        protected final int[] cells;

        Matrix(int[] cells) {
            this.cells = cells;
        }
    }

    static class Square extends Matrix {
        private final int side;

        Square(int side) {
            // The expensive expression is evaluated once and reused.
            int size = side * side;
            super(new int[size]);
            this.side = side;
            System.out.println("  Square(" + side + ") allocated " + size + " cells");
        }
    }

    // ------------------------------------------------------------------
    // 3. Preventing an unsafe call from a superclass constructor
    // ------------------------------------------------------------------

    static class Shape {
        Shape() {
            // Calling an overridable method from a constructor is a known hazard.
            System.out.println("    Shape() calls describe() -> " + describe());
        }

        String describe() {
            return "a shape";
        }
    }

    static class SafeCircle extends Shape {
        private final double radius;

        SafeCircle(double radius) {
            // Assigning the field BEFORE super() means the superclass constructor
            // can no longer observe it as 0. This is the headline change: field
            // initialisation is allowed in the prologue.
            this.radius = radius;
            super();
        }

        @Override
        String describe() {
            return "a circle of radius " + radius;
        }
    }

    static class UnsafeCircle extends Shape {
        private final double radius;

        UnsafeCircle(double radius) {
            super();                 // Shape() runs first and sees radius == 0.0
            this.radius = radius;
        }

        @Override
        String describe() {
            return "a circle of radius " + radius;
        }
    }

    public static void main(String[] args) {
        theOldRule();
        validation();
        computation();
        theInitialisationHazard();
        rules();
    }

    private static void theOldRule() {
        header("The rule that changed");

        System.out.println("Until Java 22, an explicit this(...) or super(...) had to be the FIRST");
        System.out.println("statement in a constructor. Nothing could precede it.");
        System.out.println();
        System.out.println("The workaround was the static-helper trick:");
        System.out.println("  Celsius(double degrees) {");
        System.out.println("      super(validate(degrees) + 273.15);      // helper must be static");
        System.out.println("  }");
        System.out.println("  private static double validate(double d) { ... }");
        System.out.println();
        System.out.println("It works, but it scatters the logic of one constructor across the class");
        System.out.println("and reads backwards.");
    }

    private static void validation() {
        header("1. Validate before delegating");

        new Celsius(25);

        try {
            new Celsius(-300);
        } catch (IllegalArgumentException e) {
            System.out.println("  new Celsius(-300) -> IllegalArgumentException: " + e.getMessage());
        }

        System.out.println();
        System.out.println("The superclass constructor never runs for an invalid argument, so no");
        System.out.println("half-built object ever exists. With the old rule, super() had already");
        System.out.println("executed by the time you could check anything.");
    }

    private static void computation() {
        header("2. Compute once, use twice");

        new Square(4);

        System.out.println();
        System.out.println("Previously you would either repeat side * side inside the super call and");
        System.out.println("again afterwards, or add another static helper.");
    }

    private static void theInitialisationHazard() {
        header("3. Field initialisation before super() - the interesting one");

        System.out.println("UnsafeCircle: super() first, then assign the field");
        new UnsafeCircle(5);
        System.out.println();
        System.out.println("SafeCircle: assign the field first, then super()");
        new SafeCircle(5);

        System.out.println();
        System.out.println("Look at the two 'Shape() calls describe()' lines. In the unsafe version");
        System.out.println("the superclass constructor sees radius = 0.0, because the subclass has");
        System.out.println("not assigned it yet. This is the classic 'do not call an overridable");
        System.out.println("method from a constructor' hazard, and it has bitten every Java codebase");
        System.out.println("that ever used a template-method base class.");
        System.out.println();
        System.out.println("Java 25 lets you close the hole by assigning the field in the PROLOGUE -");
        System.out.println("the region before super(). The field is written before the superclass");
        System.out.println("constructor can observe it.");
        System.out.println();
        System.out.println("This does not make calling overridable methods from constructors a good");
        System.out.println("idea. It makes the damage containable when you inherit from code that does.");
    }

    private static void rules() {
        header("What the prologue may and may not do");

        System.out.println("ALLOWED before this(...) / super(...):");
        System.out.println("  - declare and use local variables;");
        System.out.println("  - validate arguments and throw;");
        System.out.println("  - call static methods of any class;");
        System.out.println("  - ASSIGN fields declared in THIS class (the new part in 25);");
        System.out.println("  - any control flow - if, loops, try/catch.");
        System.out.println();
        System.out.println("NOT ALLOWED - the object does not exist yet:");
        System.out.println("  - READ any field, including ones you just assigned;");
        System.out.println("  - call any instance method, of this class or inherited;");
        System.out.println("  - use this or super as a value;");
        System.out.println("  - assign a field declared in a SUPERCLASS.");
        System.out.println();
        System.out.println("The compiler enforces this with a dedicated analysis; the error message");
        System.out.println("is 'cannot reference this before supertype constructor has been called'.");
        System.out.println();
        System.out.println("Terminology from the JEP: the part before the constructor invocation is");
        System.out.println("the PROLOGUE, the part after is the EPILOGUE. Only the epilogue has a");
        System.out.println("fully formed object.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
