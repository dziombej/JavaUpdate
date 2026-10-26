package com.tomtom.genesis.platform.jdk11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Java 11 - Collection.toArray(IntFunction<T[]> generator).
 *
 * Trzecia - i wreszcie sensowna - forma zamiany kolekcji na tablice.
 */
public class CollectionToArray {

    public static void main(String[] args) {
        trzyFormy();
        dlaczegoStareSaKlopotliwe();
        pulapkaToArrayBezArgumentu();
    }

    private static void trzyFormy() {
        naglowek("Trzy warianty toArray");

        List<String> miasta = List.of("Amsterdam", "Berlin", "Lodz");

        // 1. Java 1.2 - zwraca Object[], trzeba rzutowac element po elemencie.
        Object[] jakoObject = miasta.toArray();

        // 2. Java 1.2 - z tablica wzorcowa. Idiom "new String[0]".
        String[] zeWzorcem = miasta.toArray(new String[0]);

        // 3. Java 11 - referencja do konstruktora tablicy.
        String[] zGeneratorem = miasta.toArray(String[]::new);

        System.out.println("toArray()              = " + Arrays.toString(jakoObject)
                + "  typ: " + jakoObject.getClass().getSimpleName());
        System.out.println("toArray(new String[0]) = " + Arrays.toString(zeWzorcem)
                + "  typ: " + zeWzorcem.getClass().getSimpleName());
        System.out.println("toArray(String[]::new) = " + Arrays.toString(zGeneratorem)
                + "  typ: " + zGeneratorem.getClass().getSimpleName());

        System.out.println();
        System.out.println("Wariant 3 czyta sie najlepiej i jest spojny ze Stream.toArray(String[]::new),");
        System.out.println("ktore istnieje od Javy 8. Java 11 wyrownala API kolekcji do API strumieni.");
    }

    private static void dlaczegoStareSaKlopotliwe() {
        naglowek("Dlaczego stare warianty sa klopotliwe");

        List<String> miasta = new ArrayList<>(List.of("Amsterdam", "Berlin"));

        // toArray() gubi typ - kompiluje sie, ale wybucha w runtime.
        Object[] tablica = miasta.toArray();
        try {
            String[] proba = (String[]) tablica;
            System.out.println(proba.length);
        } catch (ClassCastException e) {
            System.out.println("(String[]) na wyniku toArray() -> ClassCastException");
            System.out.println("  bo faktyczny typ to Object[], mimo ze w srodku sa same Stringi");
        }

        // Wariant z tablica wzorcowa ma dwa uzycia, ktore latwo pomylic:
        String[] zaMala = miasta.toArray(new String[0]);   // JDK alokuje nowa tablice wlasciwego rozmiaru
        String[] zaDuza = miasta.toArray(new String[10]);  // JDK uzywa TEJ tablicy i wstawia null jako terminator
        System.out.println();
        System.out.println("toArray(new String[0])  -> dlugosc " + zaMala.length + "  " + Arrays.toString(zaMala));
        System.out.println("toArray(new String[10]) -> dlugosc " + zaDuza.length + "  " + Arrays.toString(zaDuza));
        System.out.println("  druga wersja zwraca tablice z nullami na koncu - rzadko o to chodzi");

        System.out.println();
        System.out.println("Ciekawostka wydajnosciowa: intuicja podpowiada, ze new String[size] jest");
        System.out.println("szybsze niz new String[0], bo unika drugiej alokacji. Pomiary JMH pokazuja");
        System.out.println("odwrotnie - new String[0] jest zwykle szybsze, bo JIT potrafi pominac");
        System.out.println("zerowanie tablicy, ktora i tak zaraz zostanie nadpisana.");
    }

    private static void pulapkaToArrayBezArgumentu() {
        naglowek("Gdzie to sie przydaje w praktyce");

        Set<Integer> zbior = Set.of(3, 1, 2);

        Integer[] posortowane = zbior.toArray(Integer[]::new);
        Arrays.sort(posortowane);
        System.out.println("posortowana tablica ze zbioru = " + Arrays.toString(posortowane));

        // Czeste uzycie: przekazanie do API oczekujacego varargs albo tablicy.
        List<String> argumenty = List.of("-v", "--out", "plik.txt");
        System.out.println("String.join na tablicy = " + String.join(" ", argumenty.toArray(String[]::new)));

        System.out.println();
        System.out.println("UWAGA na typy prymitywne: toArray(int[]::new) sie NIE skompiluje,");
        System.out.println("bo generyki nie obsluguja prymitywow. Dla int[] uzyj strumienia:");
        List<Integer> liczby = List.of(1, 2, 3);
        int[] prymitywy = liczby.stream().mapToInt(Integer::intValue).toArray();
        System.out.println("  liczby.stream().mapToInt(Integer::intValue).toArray() = "
                + Arrays.toString(prymitywy));
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
