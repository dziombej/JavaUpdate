package com.tomtom.genesis.platform.jdk10;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Java 10 - List.copyOf / Set.copyOf / Map.copyOf oraz
 * Collectors.toUnmodifiableList / toUnmodifiableSet / toUnmodifiableMap.
 *
 * Java 9 dala fabryki dla literalow (List.of("a","b")).
 * Java 10 domyka temat: jak zrobic niemodyfikowalna kopie ISTNIEJACEJ kolekcji
 * i jak zebrac strumien od razu do kolekcji niemodyfikowalnej.
 */
public class UnmodifiableCopies {

    public static void main(String[] args) {
        copyOf();
        copyOfNieKopiujeNiepotrzebnie();
        kolektory();
        plytkaKopia();
    }

    private static void copyOf() {
        naglowek("List.copyOf - kopia, nie widok");

        List<String> zrodlo = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> kopia = List.copyOf(zrodlo);

        zrodlo.add("d");

        System.out.println("zrodlo po dodaniu 'd' = " + zrodlo);
        System.out.println("kopia                 = " + kopia + "   <- nie widzi zmiany");

        try {
            kopia.add("e");
        } catch (UnsupportedOperationException e) {
            System.out.println("kopia.add(...)        -> UnsupportedOperationException");
        }

        System.out.println();
        System.out.println("Do Javy 9 typowy idiom brzmial:");
        System.out.println("  Collections.unmodifiableList(new ArrayList<>(zrodlo))");
        System.out.println("Od Javy 10 wystarczy:");
        System.out.println("  List.copyOf(zrodlo)");

        Set<String> zbior = Set.copyOf(zrodlo);
        Map<String, Integer> mapa = Map.copyOf(Map.of("x", 1, "y", 2));
        System.out.println("Set.copyOf  = " + zbior);
        System.out.println("Map.copyOf  = " + mapa);

        // Ta sama pulapka co w Java 9: null jest zabroniony.
        try {
            List.copyOf(Arrays.asList("a", null));
        } catch (NullPointerException e) {
            System.out.println("List.copyOf na liscie z nullem -> NullPointerException");
        }
    }

    private static void copyOfNieKopiujeNiepotrzebnie() {
        naglowek("Optymalizacja: copyOf na juz niemodyfikowalnej kolekcji nie kopiuje");

        List<String> juzNiemodyfikowalna = List.of("a", "b");
        List<String> kopia = List.copyOf(juzNiemodyfikowalna);

        System.out.println("czy to ta sama instancja? " + (juzNiemodyfikowalna == kopia));
        System.out.println();
        System.out.println("Jesli argument jest juz instancja ImmutableCollections, copyOf zwraca go");
        System.out.println("bez kopiowania. Dlatego List.copyOf mozna bezpiecznie wolac w konstruktorze");
        System.out.println("klasy niezmiennej - nie placisz za kopie, gdy wolajacy juz przekazal List.of.");
    }

    private static void kolektory() {
        naglowek("Collectors.toUnmodifiable*");

        List<String> miasta = List.of("Amsterdam", "Berlin", "Amsterdam", "Lodz");

        List<String> lista = miasta.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toUnmodifiableList());
        Set<String> zbior = miasta.stream()
                .collect(Collectors.toUnmodifiableSet());
        Map<String, Integer> dlugosci = miasta.stream()
                .distinct()
                .collect(Collectors.toUnmodifiableMap(m -> m, String::length));

        System.out.println("toUnmodifiableList = " + lista);
        System.out.println("toUnmodifiableSet  = " + zbior);
        System.out.println("toUnmodifiableMap  = " + dlugosci);

        try {
            lista.add("Gdansk");
        } catch (UnsupportedOperationException e) {
            System.out.println("proba modyfikacji     -> UnsupportedOperationException");
        }

        System.out.println();
        System.out.println("Uwaga na roznice wobec Collectors.toList():");
        System.out.println("  toList()             - kontrakt NIE gwarantuje typu ani modyfikowalnosci");
        System.out.println("                         (w praktyce ArrayList, ale nie wolno na tym polegac)");
        System.out.println("  toUnmodifiableList() - gwarantuje niemodyfikowalnosc i odrzuca nulle");
        System.out.println("  Stream.toList()      - dopiero Java 16, zwraca liste niemodyfikowalna,");
        System.out.println("                         ale ta AKCEPTUJE nulle - to nie to samo!");

        try {
            java.util.stream.Stream.of("a", null).collect(Collectors.toUnmodifiableList());
        } catch (NullPointerException e) {
            System.out.println("toUnmodifiableList z nullem -> NullPointerException");
        }
    }

    private static void plytkaKopia() {
        naglowek("Pulapka: to jest kopia PLYTKA");

        List<StringBuilder> zrodlo = new ArrayList<>(List.of(new StringBuilder("pierwotna")));
        List<StringBuilder> kopia = List.copyOf(zrodlo);

        // Lista jest niemodyfikowalna, ale obiekty w srodku - niekoniecznie.
        kopia.get(0).append(" ZMIENIONA");

        System.out.println("kopia.get(0) = " + kopia.get(0));
        System.out.println();
        System.out.println("List.copyOf zamraza STRUKTURE listy, nie stan elementow.");
        System.out.println("Prawdziwa niezmiennosc wymaga, zeby same elementy byly niezmienne.");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
