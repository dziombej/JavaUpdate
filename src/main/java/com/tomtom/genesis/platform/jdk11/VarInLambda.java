package com.tomtom.genesis.platform.jdk11;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Java 11 (JEP 323) - var w parametrach lambdy.
 *
 * Java 10 dala var dla zmiennych lokalnych, ale celowo pominela lambdy.
 * Java 11 domyka luke, glownie po to, zeby dalo sie dodac adnotacje
 * do parametru lambdy bez wypisywania pelnego typu.
 */
public class VarInLambda {

    /** Prosta adnotacja, zeby pokazac sens zmiany. */
    @java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface NieNull { }

    public static void main(String[] args) {
        trzyFormyZapisu();
        poCoToWogole();
        zasadaWszystkoAlboNic();
    }

    private static void trzyFormyZapisu() {
        naglowek("Trzy sposoby zapisu parametrow lambdy");

        List<String> miasta = List.of("Amsterdam", "Berlin", "Lodz");

        // 1. Typ pominiety (Java 8) - najkrotsze, najczestsze.
        String a = miasta.stream().map(m -> m.toUpperCase()).collect(Collectors.joining(", "));

        // 2. Typ jawny (Java 8) - gdy kompilator potrzebuje pomocy albo chcemy adnotacji.
        String b = miasta.stream().map((String m) -> m.toUpperCase()).collect(Collectors.joining(", "));

        // 3. var (Java 11) - typ nadal wnioskowany, ale skladniowo jest "cos" przed nazwa.
        String c = miasta.stream().map((var m) -> m.toUpperCase()).collect(Collectors.joining(", "));

        System.out.println("bez typu  : " + a);
        System.out.println("z typem   : " + b);
        System.out.println("z var     : " + c);
        System.out.println("wszystkie trzy daja identyczny bajtkod");
    }

    private static void poCoToWogole() {
        naglowek("Po co to bylo potrzebne - adnotacje");

        List<String> dane = List.of("a", "bb", "ccc");

        // Zeby dodac adnotacje do parametru lambdy PRZED Java 11, trzeba bylo napisac pelny typ:
        BiFunction<String, String, String> stary =
                (@NieNull String x, @NieNull String y) -> x + y;

        // Od Javy 11 wystarczy var - adnotacja jest, a typ nadal wnioskowany:
        BiFunction<String, String, String> nowy =
                (@NieNull var x, @NieNull var y) -> x + y;

        System.out.println("stary.apply(\"a\", \"b\") = " + stary.apply("a", "b"));
        System.out.println("nowy.apply(\"a\", \"b\")  = " + nowy.apply("a", "b"));

        String suma = dane.stream().reduce("", (@NieNull var x, @NieNull var y) -> x + y);
        System.out.println("reduce z adnotacjami   = " + suma);

        System.out.println();
        System.out.println("To ma znaczenie przy narzedziach analizy statycznej, ktore czytaja");
        System.out.println("adnotacje w rodzaju @NonNull / @Nullable (Checker Framework, SpotBugs).");
        System.out.println("Przy dlugich typach generycznych wersja z var jest znacznie krotsza:");
        System.out.println("  (@NonNull Map.Entry<String, List<Zamowienie>> e) -> ...");
        System.out.println("  (@NonNull var e) -> ...");
    }

    private static void zasadaWszystkoAlboNic() {
        naglowek("Zasada: w jednej lambdzie wszystkie parametry albo zadne");

        System.out.println("Legalne:");
        System.out.println("  (var x, var y) -> x + y");
        System.out.println("  (String x, String y) -> x + y");
        System.out.println("  (x, y) -> x + y");
        System.out.println();
        System.out.println("Bledy kompilacji:");
        System.out.println("  (var x, y) -> ...          // mieszanie var i pominiecia typu");
        System.out.println("  (var x, String y) -> ...   // mieszanie var i typu jawnego");
        System.out.println("  var x -> ...               // z var nawiasy sa OBOWIAZKOWE");
        System.out.println();
        System.out.println("Ostatnie jest najbardziej zaskakujace: x -> x + 1 dziala bez nawiasow,");
        System.out.println("ale (var x) -> x + 1 juz ich wymaga.");

        // Dowod, ze to dziala:
        java.util.function.Function<Integer, Integer> bezNawiasow = x -> x + 1;
        java.util.function.Function<Integer, Integer> zVar = (var x) -> x + 1;
        System.out.println();
        System.out.println("bezNawiasow.apply(1) = " + bezNawiasow.apply(1));
        System.out.println("zVar.apply(1)        = " + zVar.apply(1));
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
