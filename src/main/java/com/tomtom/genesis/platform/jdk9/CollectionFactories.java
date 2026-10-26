package com.tomtom.genesis.platform.jdk9;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 9 (JEP 269) - fabryki kolekcji niemodyfikowalnych: List.of / Set.of / Map.of.
 *
 * Kluczowa rzecz do zrozumienia: to NIE jest to samo, co
 * Collections.unmodifiableList(...). To osobna rodzina implementacji
 * (java.util.ImmutableCollections) o innym kontrakcie.
 */
public class CollectionFactories {

    public static void main(String[] args) {
        podstawy();
        czymSieRozniOdStaregoKodu();
        pulapkaNulli();
        pulapkaKolejnosciIteracji();
    }

    private static void podstawy() {
        naglowek("Podstawy");

        List<String> lista = List.of("Amsterdam", "Berlin", "Lodz");
        Set<Integer> zbior = Set.of(1, 2, 3);
        Map<String, Integer> mapa = Map.of("a", 1, "b", 2);

        // Map.of przyjmuje maksymalnie 10 par (10 przeciazonych metod).
        // Powyzej 10 par uzywamy Map.ofEntries + Map.entry:
        Map<String, Integer> duzaMapa = Map.ofEntries(
                Map.entry("a", 1),
                Map.entry("b", 2),
                Map.entry("c", 3));

        System.out.println("lista     = " + lista);
        System.out.println("zbior     = " + zbior);
        System.out.println("mapa      = " + mapa);
        System.out.println("duzaMapa  = " + duzaMapa);

        // Kazda proba modyfikacji konczy sie UnsupportedOperationException.
        // Dotyczy to TAKZE metod, ktore "nic nie zmieniaja", np. removeIf na pustym wyniku.
        try {
            lista.add("Warszawa");
        } catch (UnsupportedOperationException e) {
            System.out.println("lista.add(...)      -> UnsupportedOperationException (zgodnie z oczekiwaniem)");
        }
        try {
            lista.set(0, "Warszawa");
        } catch (UnsupportedOperationException e) {
            System.out.println("lista.set(0, ...)   -> UnsupportedOperationException");
        }
        try {
            lista.sort(null);
        } catch (UnsupportedOperationException e) {
            System.out.println("lista.sort(null)    -> UnsupportedOperationException");
        }
    }

    private static void czymSieRozniOdStaregoKodu() {
        naglowek("Roznice wobec Arrays.asList i Collections.unmodifiableList");

        // 1. Arrays.asList = lista o stalym ROZMIARZE, ale elementy mozna podmieniac.
        List<String> asList = Arrays.asList("a", "b", "c");
        asList.set(0, "ZMIENIONE");
        System.out.println("Arrays.asList po set(0, ...) = " + asList + "   <- set dziala!");
        try {
            asList.add("d");
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList.add(...)       -> UnsupportedOperationException (staly rozmiar)");
        }

        // 2. Collections.unmodifiableList to WIDOK na inna liste,
        //    a List.copyOf (Java 10) to prawdziwa KOPIA. Robimy oba w tym samym momencie
        //    i dopiero potem zmieniamy zrodlo - wtedy roznica jest jednoznaczna.
        List<String> zrodlo = new ArrayList<>(List.of("a", "b"));
        List<String> widok = Collections.unmodifiableList(zrodlo);
        List<String> kopia = List.copyOf(zrodlo);

        System.out.println("stan poczatkowy:");
        System.out.println("  zrodlo = " + zrodlo + ", widok = " + widok + ", kopia = " + kopia);

        zrodlo.add("NOWY");

        System.out.println("po zrodlo.add(\"NOWY\"):");
        System.out.println("  zrodlo = " + zrodlo);
        System.out.println("  widok  = " + widok + "   <- ZMIENIL SIE, bo to widok na zrodlo");
        System.out.println("  kopia  = " + kopia + "        <- bez zmian, bo to kopia");

        System.out.println();
        System.out.println("Wniosek: List.of/copyOf to realna niezmiennosc. unmodifiableList to tylko");
        System.out.println("zablokowanie API na widoku - zrodlo nadal moze sie zmienic pod spodem.");
    }

    private static void pulapkaNulli() {
        naglowek("Pulapka: null i duplikaty sa zabronione");

        // W Javie 8 pisalo sie Arrays.asList("a", null) i dzialalo. Tu - NPE.
        try {
            List.of("a", null);
        } catch (NullPointerException e) {
            System.out.println("List.of(\"a\", null)  -> NullPointerException");
        }
        try {
            Set.of("a", "a");
        } catch (IllegalArgumentException e) {
            System.out.println("Set.of(\"a\", \"a\")    -> IllegalArgumentException: " + e.getMessage());
        }
        try {
            Map.of("k", 1, "k", 2);
        } catch (IllegalArgumentException e) {
            System.out.println("Map.of z duplikatem klucza -> IllegalArgumentException: " + e.getMessage());
        }

        // Uwaga migracyjna: takze contains(null) rzuca NPE na List.of!
        try {
            List.of("a").contains(null);
        } catch (NullPointerException e) {
            System.out.println("List.of(\"a\").contains(null) -> NullPointerException  <- czesta niespodzianka");
        }
    }

    private static void pulapkaKolejnosciIteracji() {
        naglowek("Pulapka: kolejnosc iteracji Set.of / Map.of jest NIESTABILNA");

        Set<String> zbior = Set.of("alfa", "beta", "gamma", "delta");
        System.out.println("kolejnosc w tym uruchomieniu JVM: " + zbior);
        System.out.println();
        System.out.println("ImmutableCollections uzywa losowego ziarna (SALT) generowanego raz na start JVM.");
        System.out.println("Odpal te klase kilka razy - kolejnosc bedzie inna przy kazdym uruchomieniu.");
        System.out.println("To celowe: ma zapobiec pisaniu kodu zaleznego od kolejnosci. Testy, ktore");
        System.out.println("porownuja toString() takiego zbioru, beda losowo czerwone.");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
