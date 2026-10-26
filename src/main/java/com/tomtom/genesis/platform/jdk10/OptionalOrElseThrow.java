package com.tomtom.genesis.platform.jdk10;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Java 10 - Optional.orElseThrow() bez argumentu.
 *
 * Drobna zmiana, ale porzadkuje najczestszy blad w uzyciu Optional:
 * wolanie get() na czyms, co moze byc puste.
 */
public class OptionalOrElseThrow {

    public static void main(String[] args) {
        nowaMetoda();
        dlaczegoGetJestZly();
        pelnaDrabinka();
    }

    private static void nowaMetoda() {
        naglowek("orElseThrow() - nowy, czytelny odpowiednik get()");

        Optional<String> jest = Optional.of("wartosc");
        System.out.println("jest.orElseThrow() = " + jest.orElseThrow());

        Optional<String> puste = Optional.empty();
        try {
            puste.orElseThrow();
        } catch (NoSuchElementException e) {
            System.out.println("puste.orElseThrow() -> NoSuchElementException: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Zachowanie jest DOKLADNIE takie samo jak get() - ten sam wyjatek,");
        System.out.println("ten sam komunikat. Zmienila sie tylko nazwa, zeby czytajac kod widziec,");
        System.out.println("ze ta linia moze rzucic wyjatkiem.");
    }

    private static void dlaczegoGetJestZly() {
        naglowek("Dlaczego get() jest odradzane");

        System.out.println("  String v = optional.get();          // nazwa sugeruje 'po prostu wez'");
        System.out.println("  String v = optional.orElseThrow();  // nazwa mowi 'albo rzuc'");
        System.out.println();
        System.out.println("Autorzy JDK (Stuart Marks, Brian Goetz) przyznali publicznie, ze nazwa get()");
        System.out.println("byla bledem projektowym z Javy 8 - zachecala do pisania kodu bez sprawdzenia.");
        System.out.println("get() nie jest formalnie oznaczone jako @Deprecated (zlamaloby to zbyt duzo");
        System.out.println("kodu), ale IDE i linter zwykle je zglaszaja. Traktuj je jak przestarzale.");
    }

    private static void pelnaDrabinka() {
        naglowek("Cala rodzina metod wyjsciowych z Optional - kiedy ktora");

        Optional<String> puste = Optional.empty();

        System.out.println("orElse(\"X\")               = " + puste.orElse("X")
                + "     <- wartosc domyslna, liczona ZAWSZE");
        System.out.println("orElseGet(() -> \"X\")      = " + puste.orElseGet(() -> "X")
                + "     <- wartosc domyslna, liczona LENIWIE");
        System.out.println("orElseThrow()             -> NoSuchElementException");
        System.out.println("orElseThrow(Ex::new)      -> wlasny wyjatek (Java 8)");
        System.out.println("or(() -> Optional.of(\"X\")) = " + puste.or(() -> Optional.of("X"))
                + "  <- zwraca Optional, da sie lancuchowac (Java 9)");

        System.out.println();
        System.out.println("Pulapka orElse vs orElseGet: argument orElse jest wyliczany ZAWSZE,");
        System.out.println("nawet gdy Optional ma wartosc. Widac to tutaj:");

        Optional<String> jest = Optional.of("prawdziwa");
        System.out.println("  orElse    : " + jest.orElse(kosztowne()));
        System.out.println("  orElseGet : " + jest.orElseGet(OptionalOrElseThrow::kosztowne));

        System.out.println();
        System.out.println("Zasada: jesli domyslna wartosc to staly literal - orElse.");
        System.out.println("Jesli jej policzenie cos kosztuje (zapytanie do bazy, nowy obiekt) - orElseGet.");

        // Drobna, ale przydatna uwaga praktyczna:
        List<String> puste2 = List.of();
        System.out.println();
        System.out.println("Dla kolekcji zwykle lepiej zwracac pusta kolekcje niz Optional<List>: "
                + puste2);
    }

    private static String kosztowne() {
        System.out.println("    (kosztowne() zostalo wywolane!)");
        return "domyslna";
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
