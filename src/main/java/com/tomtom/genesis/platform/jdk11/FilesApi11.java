package com.tomtom.genesis.platform.jdk11;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java 11 - Files.readString / Files.writeString oraz Path.of.
 * Przy okazji: Files.lines, Files.readAllLines, InputStream.readAllBytes (Java 9)
 * i Reader.transferTo (Java 10).
 */
public class FilesApi11 {

    public static void main(String[] args) throws IOException {
        Path katalog = Files.createTempDirectory("java11-demo");
        try {
            pathOf(katalog);
            readStringWriteString(katalog);
            czytanieLinii(katalog);
            czegoUnikac(katalog);
        } finally {
            usunRekurencyjnie(katalog);
        }
    }

    private static void pathOf(Path katalog) {
        naglowek("Path.of zamiast Paths.get");

        Path a = Path.of(katalog.toString(), "podkatalog", "plik.txt");
        Path b = Paths.get(katalog.toString(), "podkatalog", "plik.txt");

        System.out.println("Path.of(...)   = " + a);
        System.out.println("Paths.get(...) = " + b);
        System.out.println("rowne?         = " + a.equals(b));

        System.out.println();
        System.out.println("To ta sama funkcjonalnosc - Paths.get jest teraz tylko cienkim opakowaniem");
        System.out.println("na Path.of. Powod zmiany: klasa Paths istniala wylacznie po to, zeby");
        System.out.println("pomiescic metode fabryczna. Od Javy 9 interfejsy moga miec metody static,");
        System.out.println("wiec fabryka trafila tam, gdzie jej miejsce - do samego Path.");
        System.out.println("Paths nie jest @Deprecated, ale w nowym kodzie uzywaj Path.of.");

        System.out.println();
        System.out.println("Path.of z URI:      Path.of(URI.create(\"file:///tmp\"))");
        System.out.println("Sklejanie sciezek:  katalog.resolve(\"plik.txt\")  <- preferowane nad konkatenacja");
    }

    private static void readStringWriteString(Path katalog) throws IOException {
        naglowek("Files.writeString / Files.readString");

        Path plik = katalog.resolve("notatka.txt");

        // Jedna linia zamiast try-with-resources na BufferedWriter.
        Files.writeString(plik, "Ala ma kota\nkot ma Ale\n");
        String tresc = Files.readString(plik);

        System.out.println("zapisano do : " + plik.getFileName());
        System.out.println("odczytano   : " + tresc.replace("\n", "\\n"));
        System.out.println("dlugosc     : " + tresc.length() + " znakow");

        System.out.println();
        System.out.println("Domyslne kodowanie to UTF-8 - ZAWSZE, niezaleznie od file.encoding systemu.");
        System.out.println("To wazna roznica wobec starego new FileReader(plik), ktory az do Javy 18");
        System.out.println("uzywal domyslnego kodowania platformy i potrafil dac inny wynik na Windows.");

        // Kodowanie mozna podac jawnie:
        Files.writeString(plik, "zażółć gęślą jaźń", StandardCharsets.UTF_8);
        System.out.println("z polskimi znakami: " + Files.readString(plik, StandardCharsets.UTF_8));

        // Opcje otwarcia - np. dopisywanie:
        Files.writeString(plik, "\ndopisane", java.nio.file.StandardOpenOption.APPEND);
        System.out.println("po APPEND         : " + Files.readString(plik).replace("\n", "\\n"));

        System.out.println();
        System.out.println("Ograniczenie: readString wczytuje CALY plik do pamieci i rzuca");
        System.out.println("OutOfMemoryError dla plikow powyzej ok. 2 GB (limit dlugosci String).");
        System.out.println("Do duzych plikow uzywaj Files.lines albo strumieni.");
    }

    private static void czytanieLinii(Path katalog) throws IOException {
        naglowek("Trzy sposoby czytania linii i kiedy ktory");

        Path plik = katalog.resolve("dane.txt");
        Files.writeString(plik, "alfa\nbeta\ngamma\ndelta\n");

        // 1. Files.readAllLines - cala zawartosc do listy (Java 8, ale bez Charset od Javy 8 tez dziala).
        List<String> wszystkie = Files.readAllLines(plik);
        System.out.println("Files.readAllLines  = " + wszystkie + "   (cale w pamieci)");

        // 2. Files.readString + String.lines (Java 11) - cale w pamieci, ale leniwe przetwarzanie.
        List<String> przezLines = Files.readString(plik).lines()
                .filter(l -> l.length() > 4)
                .collect(Collectors.toList());
        System.out.println("readString + lines  = " + przezLines);

        // 3. Files.lines - leniwe CZYTANIE, plik nie musi sie miescic w pamieci.
        //    UWAGA: zwraca strumien trzymajacy otwarty deskryptor pliku - wymaga zamkniecia!
        try (Stream<String> strumien = Files.lines(plik)) {
            String pierwsza = strumien.findFirst().orElse("(pusty plik)");
            System.out.println("Files.lines         = " + pierwsza + " ...   (leniwe, wymaga close)");
        }

        System.out.println();
        System.out.println("Files.lines w try-with-resources to nie kosmetyka - bez tego wyciekaja");
        System.out.println("deskryptory plikow. To jeden z nielicznych strumieni, ktore trzeba zamykac.");
    }

    private static void czegoUnikac(Path katalog) throws IOException {
        naglowek("Czego juz nie pisac");

        Path plik = katalog.resolve("porownanie.txt");
        Files.writeString(plik, "tresc");

        System.out.println("Java 8 i wczesniej:");
        System.out.println("  try (BufferedReader r = new BufferedReader(new FileReader(plik))) {");
        System.out.println("      StringBuilder sb = new StringBuilder();");
        System.out.println("      String linia;");
        System.out.println("      while ((linia = r.readLine()) != null) { sb.append(linia).append('\\n'); }");
        System.out.println("      return sb.toString();");
        System.out.println("  }");
        System.out.println();
        System.out.println("Java 11:");
        System.out.println("  return Files.readString(plik);");
        System.out.println();
        System.out.println("wynik = " + Files.readString(plik));
    }

    private static void usunRekurencyjnie(Path katalog) throws IOException {
        try (Stream<Path> sciezki = Files.walk(katalog)) {
            sciezki.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }
        System.out.println();
        System.out.println("(katalog tymczasowy " + katalog + " zostal usuniety)");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
