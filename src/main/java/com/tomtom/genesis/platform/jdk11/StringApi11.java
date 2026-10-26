package com.tomtom.genesis.platform.jdk11;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Java 11 (JEP 327 / JDK-8200378) - nowe metody na String:
 * isBlank(), strip(), stripLeading(), stripTrailing(), lines(), repeat().
 */
public class StringApi11 {

    public static void main(String[] args) {
        isBlank();
        stripKontraTrim();
        lines();
        repeat();
    }

    private static void isBlank() {
        naglowek("isBlank - pusty albo same biale znaki");

        System.out.println("\"\".isEmpty()        = " + "".isEmpty());
        System.out.println("\"   \".isEmpty()     = " + "   ".isEmpty() + "   <- isEmpty patrzy tylko na dlugosc");
        System.out.println("\"\".isBlank()        = " + "".isBlank());
        System.out.println("\"   \".isBlank()     = " + "   ".isBlank());
        System.out.println("\"\\t\\n\".isBlank()    = " + "\t\n".isBlank());
        System.out.println("\"a\".isBlank()       = " + "a".isBlank());

        System.out.println();
        System.out.println("Przed Java 11 pisalo sie: s == null || s.trim().isEmpty()");
        System.out.println("albo siegalo po StringUtils.isBlank z Apache Commons Lang.");
        System.out.println("UWAGA: String.isBlank NIE obsluguje nulla - to metoda instancyjna,");
        System.out.println("wiec na nullu dostaniesz NPE. Commons Lang isBlank(null) zwracalo true.");
    }

    private static void stripKontraTrim() {
        naglowek("strip kontra trim - roznica jest w definicji 'bialego znaku'");

        // U+2000 to EN QUAD - znak odstepu w Unicode, o kodzie WIEKSZYM niz spacja (U+0020).
        String zUnicode = "\u2000 Ala ma kota \u2000";

        System.out.println("wejscie (z U+2000) : [" + zUnicode + "]");
        System.out.println("trim()             : [" + zUnicode.trim() + "]");
        System.out.println("strip()            : [" + zUnicode.strip() + "]");

        System.out.println();
        System.out.println("trim()  usuwa znaki o kodzie <= U+0020 (definicja z 1995 roku, sprzed Unicode).");
        System.out.println("strip() usuwa wszystko, co Character.isWhitespace uznaje za bialy znak.");
        System.out.println("Character.isWhitespace('\\u2000') = " + Character.isWhitespace('\u2000'));

        String tekst = "   srodek   ";
        System.out.println();
        System.out.println("stripLeading()     : [" + tekst.stripLeading() + "]");
        System.out.println("stripTrailing()    : [" + tekst.stripTrailing() + "]");
        System.out.println("strip()            : [" + tekst.strip() + "]");
        System.out.println();
        System.out.println("trim() nie mial odpowiednikow jednostronnych - trzeba bylo pisac regex.");
    }

    private static void lines() {
        naglowek("lines - leniwy strumien linii");

        String wielolinijkowy = "pierwsza\ndruga\r\ntrzecia\rczwarta";

        List<String> linie = wielolinijkowy.lines().collect(Collectors.toList());
        System.out.println("wejscie zawiera \\n, \\r\\n oraz \\r");
        System.out.println("lines() = " + linie);

        System.out.println();
        System.out.println("lines() rozpoznaje wszystkie trzy konwencje koncow linii (Unix, Windows, stary Mac)");
        System.out.println("i - w odroznieniu od split - nie kompiluje wyrazenia regularnego.");
        System.out.println("Zwraca Stream, wiec dziala leniwie: mozna np. wziac 2 linie z ogromnego tekstu.");

        // Roznica wobec split w traktowaniu koncowych pustych linii:
        String zPustymKoncem = "a\nb\n\n\n";
        System.out.println();
        System.out.println("dla \"a\\nb\\n\\n\\n\":");
        System.out.println("  split(\"\\n\").length = " + zPustymKoncem.split("\n").length
                + "   <- split obcina koncowe puste");
        System.out.println("  lines().count()    = " + zPustymKoncem.lines().count()
                + "   <- lines tez, ale bez regexa");

        // Typowe zastosowanie: przetworzenie pliku konfiguracyjnego wpisanego w tekscie.
        String konfiguracja = "# komentarz\n"
                + "host = localhost\n"
                + "\n"
                + "port = 8080\n";

        List<String> wpisy = konfiguracja.lines()
                .map(String::strip)
                .filter(l -> !l.isBlank())
                .filter(l -> !l.startsWith("#"))
                .collect(Collectors.toList());
        System.out.println();
        System.out.println("odfiltrowane wpisy konfiguracji = " + wpisy);
        System.out.println("(w Javie 15+ ten tekst zapisalbys jako blok \"\"\"...\"\"\" - tu celowo");
        System.out.println(" uzyto konkatenacji, zeby caly plik kompilowal sie pod --release 11)");
    }

    private static void repeat() {
        naglowek("repeat - powtorzenie lancucha");

        System.out.println("\"-\".repeat(40)   = " + "-".repeat(40));
        System.out.println("\"ab\".repeat(3)   = " + "ab".repeat(3));
        System.out.println("\"ab\".repeat(0)   = [" + "ab".repeat(0) + "]  (pusty lancuch)");

        try {
            "ab".repeat(-1);
        } catch (IllegalArgumentException e) {
            System.out.println("\"ab\".repeat(-1)  -> IllegalArgumentException: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Przed Java 11: new String(new char[n]).replace('\\0', '-')");
        System.out.println("albo Collections.nCopies(n, \"-\") + String.join(\"\", ...).");
        System.out.println("repeat jest tez szybsze - alokuje docelowa tablice raz i kopiuje blokami.");

        // Praktyczne: ramka do wypisania w konsoli.
        String tytul = "RAPORT";
        int szerokosc = 30;
        System.out.println();
        System.out.println("+" + "-".repeat(szerokosc) + "+");
        System.out.println("|" + " ".repeat((szerokosc - tytul.length()) / 2) + tytul
                + " ".repeat(szerokosc - tytul.length() - (szerokosc - tytul.length()) / 2) + "|");
        System.out.println("+" + "-".repeat(szerokosc) + "+");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
