package com.tomtom.genesis.platform.jdk9;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Java 9 - trzy nowe metody na Optional: or(), ifPresentOrElse(), stream().
 */
public class OptionalApi9 {

    public static void main(String[] args) {
        or();
        ifPresentOrElse();
        optionalStream();
    }

    private static void or() {
        naglowek("Optional.or - lancuch zrodel z leniwym fallbackiem");

        // orElseGet zwraca WARTOSC (T). or zwraca OPTIONAL - wiec da sie lancuchowac.
        Optional<String> wynik = zKonfiguracji()
                .or(OptionalApi9::zeZmiennejSrodowiskowej)
                .or(OptionalApi9::zDomyslnych);
        System.out.println("wartosc = " + wynik.orElseThrow());

        System.out.println();
        System.out.println("Kazdy Supplier jest wywolywany LENIWIE - tylko jesli poprzedni zwrocil empty.");
        System.out.println("W Javie 8 pisalo sie to tak:");
        System.out.println("  Optional<String> w = a.isPresent() ? a : (b.isPresent() ? b : c);");
    }

    private static Optional<String> zKonfiguracji() {
        System.out.println("  -> sprawdzam konfiguracje...  (pusto)");
        return Optional.empty();
    }

    private static Optional<String> zeZmiennejSrodowiskowej() {
        System.out.println("  -> sprawdzam zmienne srodowiskowe...  (pusto)");
        return Optional.empty();
    }

    private static Optional<String> zDomyslnych() {
        System.out.println("  -> biore wartosc domyslna");
        return Optional.of("localhost:8080");
    }

    private static void ifPresentOrElse() {
        naglowek("Optional.ifPresentOrElse - if/else bez isPresent()");

        Optional<String> jest = Optional.of("Ala");
        Optional<String> puste = Optional.empty();

        jest.ifPresentOrElse(
                v -> System.out.println("znaleziono: " + v),
                () -> System.out.println("nie znaleziono"));

        puste.ifPresentOrElse(
                v -> System.out.println("znaleziono: " + v),
                () -> System.out.println("nie znaleziono"));

        System.out.println();
        System.out.println("Drugi argument to Runnable (akcja bez argumentu), nie Supplier.");
        System.out.println("To wersja 'do czegos', a nie 'zwroc cos' - odpowiednik if/else, nie ?:.");
    }

    private static void optionalStream() {
        naglowek("Optional.stream - filtrowanie pustych w jednym kroku");

        List<String> identyfikatory = List.of("1", "nie-liczba", "3", "cztery", "5");

        // Optional.stream() daje strumien 0- lub 1-elementowy, wiec flatMap odsiewa puste.
        List<Integer> sparsowane = identyfikatory.stream()
                .map(OptionalApi9::sparsuj)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        System.out.println("wejscie    = " + identyfikatory);
        System.out.println("sparsowane = " + sparsowane);

        System.out.println();
        System.out.println("Wersja z Javy 8 wymagala dwoch krokow:");
        System.out.println("  .filter(Optional::isPresent).map(Optional::get)");
        System.out.println("Wersja z Javy 9 to jeden krok:");
        System.out.println("  .flatMap(Optional::stream)");
    }

    private static Optional<Integer> sparsuj(String tekst) {
        try {
            return Optional.of(Integer.valueOf(tekst));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
