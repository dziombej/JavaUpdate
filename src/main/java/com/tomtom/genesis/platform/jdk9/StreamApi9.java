package com.tomtom.genesis.platform.jdk9;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java 9 - rozszerzenia Stream API: takeWhile, dropWhile, 3-argumentowe iterate,
 * Stream.ofNullable oraz nowe kolektory Collectors.filtering / Collectors.flatMapping.
 */
public class StreamApi9 {

    public static void main(String[] args) {
        takeWhileIDropWhile();
        roznicaWobecFilter();
        iterateZWarunkiem();
        ofNullable();
        noweKolektory();
    }

    private static void takeWhileIDropWhile() {
        naglowek("takeWhile / dropWhile");

        List<Integer> liczby = List.of(2, 4, 6, 7, 8, 10);

        // takeWhile: bierz elementy DOPOKI predykat jest prawdziwy, potem przerwij.
        List<Integer> take = liczby.stream()
                .takeWhile(n -> n % 2 == 0)
                .collect(Collectors.toList());

        // dropWhile: pomijaj elementy dopoki predykat jest prawdziwy, potem bierz WSZYSTKO.
        List<Integer> drop = liczby.stream()
                .dropWhile(n -> n % 2 == 0)
                .collect(Collectors.toList());

        System.out.println("wejscie   = " + liczby);
        System.out.println("takeWhile = " + take + "   (zatrzymal sie na 7)");
        System.out.println("dropWhile = " + drop + "      (od 7 wlacznie, mimo ze 8 i 10 sa parzyste)");
    }

    private static void roznicaWobecFilter() {
        naglowek("Dlaczego to NIE jest filter");

        List<Integer> liczby = List.of(2, 4, 6, 7, 8, 10);

        System.out.println("filter(parzyste)    = "
                + liczby.stream().filter(n -> n % 2 == 0).collect(Collectors.toList()));
        System.out.println("takeWhile(parzyste) = "
                + liczby.stream().takeWhile(n -> n % 2 == 0).collect(Collectors.toList()));
        System.out.println();
        System.out.println("filter przeglada CALY strumien i wybiera pasujace.");
        System.out.println("takeWhile konczy prace przy pierwszym elemencie, ktory nie pasuje -");
        System.out.println("dzieki temu dziala na strumieniach nieskonczonych:");

        List<Integer> zNieskonczonego = Stream.iterate(1, n -> n * 2)
                .takeWhile(n -> n < 100)
                .collect(Collectors.toList());
        System.out.println("  Stream.iterate(1, n -> n*2).takeWhile(n < 100) = " + zNieskonczonego);

        System.out.println();
        System.out.println("UWAGA: semantyka 'dopoki' ma sens tylko dla strumieni UPORZADKOWANYCH.");
        System.out.println("Na strumieniu nieuporzadkowanym (np. z HashSet) albo rownoleglym wynik");
        System.out.println("jest niedeterministyczny - JDK moze wybrac dowolny pasujacy prefiks.");
    }

    private static void iterateZWarunkiem() {
        naglowek("Stream.iterate z warunkiem stopu (odpowiednik petli for)");

        // Java 8: Stream.iterate(seed, next) - nieskonczony, trzeba limit()/takeWhile().
        // Java 9: Stream.iterate(seed, hasNext, next) - 1:1 odpowiednik petli for.
        //         for (int i = 1; i <= 5; i++)  ==  Stream.iterate(1, i -> i <= 5, i -> i + 1)
        String wynik = Stream.iterate(1, i -> i <= 5, i -> i +1)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        System.out.println("Stream.iterate(1, i -> i <= 5, i -> i + 1) = " + wynik);

        // Nie musi byc liczbowe - dowolna iteracja z warunkiem:
        String polowienie = Stream.iterate(1024, n -> n > 1, n -> n / 2)
                .map(String::valueOf)
                .collect(Collectors.joining(" -> "));
        System.out.println("polowienie 1024: " + polowienie);
    }

    private static void ofNullable() {
        naglowek("Stream.ofNullable");

        // Java 8: Stream.of(null) daje strumien z jednym elementem null (albo NPE przy tablicy).
        // Java 9: Stream.ofNullable(x) daje strumien pusty dla null, jednoelementowy dla nie-null.
        System.out.println("Stream.ofNullable(\"x\").count() = " + Stream.ofNullable("x").count());
        System.out.println("Stream.ofNullable(null).count() = " + Stream.ofNullable(null).count());

        // Typowe zastosowanie: plaskie mapowanie po mapie, gdzie klucza moze nie byc.
        Map<String, String> konfiguracja = Map.of("host", "localhost", "port", "8080");
        List<String> odczytane = Stream.of("host", "port", "timeout")
                .flatMap(klucz -> Stream.ofNullable(konfiguracja.get(klucz)))
                .collect(Collectors.toList());
        System.out.println("odczytane (timeout nie istnieje) = " + odczytane);
    }

    private static void noweKolektory() {
        naglowek("Collectors.filtering i Collectors.flatMapping");

        List<Pracownik> ludzie = List.of(
                new Pracownik("backend", "Ala", List.of("java", "go")),
                new Pracownik("backend", "Bartek", List.of("java")),
                new Pracownik("frontend", "Celina", List.of("ts", "js")));

        // filtering: filtruje JUZ PO pogrupowaniu, wiec puste grupy zostaja.
        Map<String, List<String>> tylkoNaB = ludzie.stream()
                .collect(Collectors.groupingBy(Pracownik::dzial,
                        Collectors.filtering(p -> p.imie().startsWith("B"),
                                Collectors.mapping(Pracownik::imie, Collectors.toList()))));
        System.out.println("groupingBy + filtering  = " + tylkoNaB);

        // Dla porownania - filter PRZED groupingBy gubi cala grupe "frontend":
        Map<String, List<String>> filterPrzed = ludzie.stream()
                .filter(p -> p.imie().startsWith("B"))
                .collect(Collectors.groupingBy(Pracownik::dzial,
                        Collectors.mapping(Pracownik::imie, Collectors.toList())));
        System.out.println("filter przed groupingBy = " + filterPrzed + "   <- brak klucza frontend");

        // flatMapping: rozplaszcza kolekcje wewnatrz grupy.
        Map<String, List<String>> jezykiWDziale = ludzie.stream()
                .collect(Collectors.groupingBy(Pracownik::dzial,
                        Collectors.flatMapping(p -> p.jezyki().stream(), Collectors.toList())));
        System.out.println("groupingBy + flatMapping = " + jezykiWDziale);

        Optional<Pracownik> pierwszy = ludzie.stream().findFirst();
        System.out.println("findFirst = " + pierwszy.map(Pracownik::imie).orElse("brak"));
    }

    /**
     * Zwykla klasa zamiast rekordu - rekordy to Java 16, a ten plik ma pokazywac Jave 9.
     */
    private static final class Pracownik {
        private final String dzial;
        private final String imie;
        private final List<String> jezyki;

        Pracownik(String dzial, String imie, List<String> jezyki) {
            this.dzial = dzial;
            this.imie = imie;
            this.jezyki = jezyki;
        }

        String dzial() {
            return dzial;
        }

        String imie() {
            return imie;
        }

        List<String> jezyki() {
            return jezyki;
        }
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
