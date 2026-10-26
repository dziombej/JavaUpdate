package com.tomtom.genesis.platform.jdk11;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Java 11 - Predicate.not(...).
 *
 * Metoda statyczna, ktora zwraca zanegowany predykat. Powod istnienia:
 * referencji do metody nie da sie zanegowac operatorem !.
 */
public class PredicateNot {

    public static void main(String[] args) {
        problem();
        rozwiazanie();
        lancuchowanie();
    }

    private static void problem() {
        naglowek("Problem: ! nie dziala na referencji do metody");

        System.out.println("  .filter(String::isBlank)     // dziala - referencja do metody");
        System.out.println("  .filter(!String::isBlank)    // BLAD KOMPILACJI - ! dziala na wartosci boolean,");
        System.out.println("                               //   a String::isBlank to nie boolean, tylko Predicate");
        System.out.println();
        System.out.println("Przed Java 11 byly dwa obejscia:");
        System.out.println("  .filter(s -> !s.isBlank())              // rozwiniecie do lambdy");
        System.out.println("  .filter(((Predicate<String>) String::isBlank).negate())  // rzutowanie + negate");
    }

    private static void rozwiazanie() {
        naglowek("Rozwiazanie: Predicate.not");

        List<String> linie = List.of("alfa", "", "   ", "beta", "\t");

        List<String> niepuste = linie.stream()
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toList());

        System.out.println("wejscie  = " + linie.stream()
                .map(s -> "[" + s.replace("\t", "\\t") + "]")
                .collect(Collectors.joining(", ")));
        System.out.println("niepuste = " + niepuste);

        System.out.println();
        System.out.println("Implementacja jest trywialna - to jedna linia w JDK:");
        System.out.println("  static <T> Predicate<T> not(Predicate<? super T> target) {");
        System.out.println("      return (Predicate<T>) target.negate();");
        System.out.println("  }");
        System.out.println();
        System.out.println("Cala wartosc polega na tym, ze argument ma typ DOCELOWY Predicate<T>,");
        System.out.println("wiec kompilator potrafi wywnioskowac typ referencji do metody.");
        System.out.println("Przy .negate() referencja stoi po lewej stronie kropki - i wnioskowanie nie dziala.");

        // Ladnie czyta sie ze statycznym importem:
        //   import static java.util.function.Predicate.not;
        //   .filter(not(String::isBlank))
    }

    private static void lancuchowanie() {
        naglowek("Laczenie z and / or / negate");

        List<String> nazwy = List.of("Ala", "bartek", "Celina", "", "dorota", "Ewa");

        Predicate<String> zaczynaSieWielka = s -> !s.isEmpty() && Character.isUpperCase(s.charAt(0));
        Predicate<String> krotka = s -> s.length() <= 3;

        System.out.println("wielka litera            = " + filtruj(nazwy, zaczynaSieWielka));
        System.out.println("NIE wielka litera        = " + filtruj(nazwy, Predicate.not(zaczynaSieWielka)));
        System.out.println("wielka ORAZ krotka       = " + filtruj(nazwy, zaczynaSieWielka.and(krotka)));
        System.out.println("wielka LUB krotka        = " + filtruj(nazwy, zaczynaSieWielka.or(krotka)));
        System.out.println("niepuste i nie na wielka = " + filtruj(nazwy,
                Predicate.not(String::isBlank).and(Predicate.not(zaczynaSieWielka))));

        System.out.println();
        System.out.println("Uwaga na kolejnosc: p.and(q).or(r) to (p AND q) OR r - metody lacza sie");
        System.out.println("od lewej, bez priorytetu AND nad OR znanego z operatorow. Nawiasuj jawnie.");

        // Predicate.isEqual (Java 8) dla porzadku:
        System.out.println();
        System.out.println("Predicate.isEqual(\"Ala\")  = " + filtruj(nazwy, Predicate.isEqual("Ala")));
    }

    private static List<String> filtruj(List<String> dane, Predicate<String> warunek) {
        return dane.stream().filter(warunek).collect(Collectors.toList());
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
