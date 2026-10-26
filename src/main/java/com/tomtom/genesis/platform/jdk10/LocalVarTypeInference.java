package com.tomtom.genesis.platform.jdk10;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java 10 (JEP 286) - var, czyli wnioskowanie typu zmiennej lokalnej.
 *
 * Najwazniejsze: to NIE jest typowanie dynamiczne. Typ jest ustalany przez
 * kompilator w miejscu deklaracji, na stale, i zapisywany w bajtkodzie.
 * W pliku .class nie ma zadnego sladu po slowie "var".
 */
public class LocalVarTypeInference {

    public static void main(String[] args) {
        gdzieWolno();
        gdzieNieWolno();
        pulapkaTypuOgolnego();
        typyNienazywalne();
        varNieJestSlowemKluczowym();
        kiedyUzywac();
    }

    private static void gdzieWolno() {
        naglowek("Gdzie var dziala");

        var tekst = "Ala ma kota";                 // String
        var liczba = 42;                           // int   (nie Integer!)
        var ulamek = 3.14;                         // double
        var lista = new ArrayList<String>();       // ArrayList<String>
        var mapa = Map.of("a", 1);                 // Map<String, Integer>
        var tablica = new int[] {1, 2, 3};         // int[]

        lista.add("element");

        System.out.println("tekst   : " + tekst.getClass().getSimpleName());
        System.out.println("liczba  : " + ((Object) liczba).getClass().getSimpleName() + "  (autoboxing przy wypisaniu; sama zmienna to prymityw int)");
        System.out.println("ulamek  : " + ((Object) ulamek).getClass().getSimpleName());
        System.out.println("lista   : " + lista.getClass().getSimpleName() + " -> " + lista);
        System.out.println("mapa    : " + mapa);
        System.out.println("tablica : dlugosc " + tablica.length);

        // W petlach - tu var czyta sie najlepiej:
        for (var i = 0; i < 3; i++) {
            System.out.print(i + " ");
        }
        System.out.println();
        for (var wpis : mapa.entrySet()) {
            // bez var byloby: Map.Entry<String, Integer> wpis
            System.out.println("wpis: " + wpis.getKey() + "=" + wpis.getValue());
        }

        // W try-with-resources tez:
        try (var zasob = new java.io.ByteArrayInputStream("dane".getBytes())) {
            System.out.println("odczytano bajtow: " + zasob.readAllBytes().length);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }

        System.out.println();
        System.out.println("Zastrzezenie do powyzszych linii: getClass() pokazuje klase OBIEKTU");
        System.out.println("w runtime, a nie typ zmiennej wywnioskowany przez kompilator. Tutaj oba");
        System.out.println("sie pokrywaja, wiec jako ilustracja to wystarcza - ale to przypadek,");
        System.out.println("nie regula. W sekcji o typach nienazywalnych zobaczysz przyklad, gdzie");
        System.out.println("te dwie rzeczy sa zupelnie rozne, i dlaczego mylenie ich jest najczestszym");
        System.out.println("nieporozumieniem wokol var.");
    }

    private static void gdzieNieWolno() {
        naglowek("Gdzie var NIE dziala (wszystkie ponizsze to bledy kompilacji)");

        System.out.println("  var bezWartosci;                 // brak inicjalizatora - nie ma z czego wnioskowac");
        System.out.println("  var nic = null;                  // typ null nie jest typem docelowym");
        System.out.println("  var f = () -> System.out.print(); // lambda potrzebuje typu docelowego");
        System.out.println("  var m = String::valueOf;          // to samo dla referencji do metody");
        System.out.println("  var t = {1, 2, 3};                // inicjalizator tablicowy bez new int[]");
        System.out.println("  private var pole = 1;             // tylko zmienne LOKALNE");
        System.out.println("  void metoda(var argument)         // nie dla parametrow metod (lambdy - patrz Java 11)");
        System.out.println("  var metoda() { ... }              // nie dla typu zwracanego");
        System.out.println("  catch (var e)                     // nie dla parametru catch");
        System.out.println();
        System.out.println("Zasada: var dziala tam, gdzie kompilator widzi jednoznaczna wartosc poczatkowa");
        System.out.println("i gdzie zasieg zmiennej jest ograniczony do jednej metody.");
    }

    private static void pulapkaTypuOgolnego() {
        naglowek("Pulapka nr 1: var + diamond = typ ogolniejszy niz chcesz");

        var zleWywnioskowana = new ArrayList<>();   // ArrayList<Object>, nie ArrayList<String>!
        zleWywnioskowana.add("tekst");
        zleWywnioskowana.add(42);                   // kompiluje sie, bo to lista Object
        System.out.println("new ArrayList<>() pod var  = " + zleWywnioskowana + "  (ArrayList<Object>)");

        var dobrze = new ArrayList<String>();
        dobrze.add("tekst");
        System.out.println("new ArrayList<String>()    = " + dobrze + "  (ArrayList<String>)");

        System.out.println();
        System.out.println("Z var trzeba podac parametr typu po PRAWEJ stronie, bo po lewej go juz nie ma.");

        // Druga odmiana tej pulapki: literaly liczbowe.
        var mala = 1;          // int
        var duza = 1L;         // long
        var uamek = 1.0f;      // float
        System.out.println("1 -> int, 1L -> long, 1.0f -> float: " + mala + " " + duza + " " + uamek);
        System.out.println("Jesli potrzebujesz long, a napiszesz var x = 1, dostaniesz int.");
    }

    private static void typyNienazywalne() {
        naglowek("Pulapka nr 2 (a wlasciwie supermoc): typy nienazywalne");

        // Typ klasy anonimowej nie ma nazwy, wiec nie da sie go zapisac po lewej stronie.
        // var potrafi go zapamietac - i dzieki temu widzimy pole "licznik".
        var licznikAnonimowy = new Object() {
            int licznik = 0;

            void zwieksz() {
                licznik++;
            }
        };
        licznikAnonimowy.zwieksz();
        licznikAnonimowy.zwieksz();
        System.out.println("pole klasy anonimowej widoczne przez var: " + licznikAnonimowy.licznik);

        System.out.println();
        System.out.println("Gdyby napisac Object licznikAnonimowy = new Object() {...}, pole 'licznik'");
        System.out.println("byloby niedostepne - typ Object go nie zna. var zachowuje typ anonimowy.");
        System.out.println();
        System.out.println("To pierwszy z dwoch przypadkow, w ktorych var daje cos, czego nie da sie");
        System.out.println("napisac inaczej. Drugi jest ponizej.");
        System.out.println();
        System.out.println("--- drugi przypadek: typ przecieciowy z wyrazenia warunkowego ---");

        // Druga odmiana: typ PRZECIECIOWY (intersection type) z wyrazenia warunkowego.
        //
        // Operator ?: bierze typy STATYCZNE obu galezi i szuka ich najmniejszego
        // wspolnego nadtypu. Gdy zadna z galezi nie jest nadtypem drugiej,
        // tym nadtypem jest przeciecie kilku typow naraz - a takiego przeciecia
        // NIE DA SIE zapisac w Javie po lewej stronie deklaracji.
        var kolekcja = true ? new ArrayList<String>() : new java.util.LinkedList<String>();
        kolekcja.add("element");

        // Kompilator wywnioskowal typ, ktory jest jednoczesnie wszystkim ponizszym -
        // kazde z tych przypisan dziala bez rzutowania:
        // Te trzy linie sa cala pointa: kompiluja sie WSZYSTKIE, bez zadnego rzutowania.
        // To jedyny dowod na ksztalt wywnioskowanego typu - w runtime nie da sie go zobaczyc.
        java.util.AbstractList<String> jakoAbstractList = kolekcja;
        java.io.Serializable jakoSerializable = kolekcja;
        Cloneable jakoCloneable = kolekcja;
        assert jakoAbstractList != null && jakoSerializable != null && jakoCloneable != null;

        System.out.println("  var kolekcja = true ? new ArrayList<String>() : new LinkedList<String>();");
        System.out.println();
        System.out.println("kolekcja zachowuje sie jak lista: " + kolekcja
                + ", rozmiar " + kolekcja.size());
        System.out.println();
        System.out.println("Dowodu NIE da sie wypisac w runtime. Dowodem jest to, ze trzy ponizsze");
        System.out.println("przypisania SIE KOMPILUJA, kazde bez rzutowania:");
        System.out.println("  AbstractList<String> a = kolekcja;");
        System.out.println("  Serializable         b = kolekcja;");
        System.out.println("  Cloneable            c = kolekcja;");
        System.out.println("Gdyby wywnioskowany typ byl tylko jednym z nich, dwa pozostale by nie");
        System.out.println("przeszly przez kompilator. Te trzy linie naprawde stoja w tym pliku -");
        System.out.println("gdyby byly bledne, nie czytalbys teraz tego komunikatu.");
        System.out.println();
        System.out.println("Typ wywnioskowany przez kompilator to:");
        System.out.println("  AbstractList<String> & Serializable & Cloneable");
        System.out.println("Nie ma zadnej nazwy, ktora mozna wpisac po lewej stronie, zeby zachowac");
        System.out.println("wszystkie trzy naraz. Napisanie AbstractList<String> gubi Serializable");
        System.out.println("i Cloneable. Tylko var potrafi zapamietac calosc.");
        System.out.println();
        System.out.println("Chcesz zobaczyc te nazwe? Dopisz w tym miejscu celowy blad, np. 'int x = kolekcja;'");
        System.out.println("i skompiluj. javac odpowie:");
        System.out.println("  error: incompatible types: INT#1 cannot be converted to int");
        System.out.println("    where INT#1 is an intersection type:");
        System.out.println("      INT#1 extends AbstractList<String>,Serializable,Cloneable");
        System.out.println();
        System.out.println("UWAGA na czesta pomylke: typ STATYCZNY to nie to samo co klasa w RUNTIME.");
        System.out.println();
        System.out.println("kolekcja.getClass().getSimpleName() zwroci tutaj \"ArrayList\" - bo warunek");
        System.out.println("jest true, wiec powstal obiekt z pierwszej galezi. Ale to informacja");
        System.out.println("o OBIEKCIE, a nie o typie zmiennej. Typem zmiennej jest przeciecie powyzej,");
        System.out.println("i zadne wywolanie w runtime tego nie pokaze. Dlatego getClass() celowo");
        System.out.println("nie jest tu wypisywany - mieszanie tych dwoch rzeczy to najczestsze");
        System.out.println("zrodlo nieporozumien wokol var.");
        System.out.println();
        System.out.println("Gdyby lewa galaz byla zmienna zadeklarowana jako List<String> (a nie");
        System.out.println("wyrazeniem new ArrayList<>()), kompilator widzialby List<String>, a nie");
        System.out.println("ArrayList - i wspolnym nadtypem bylby po prostu List<String>, bo");
        System.out.println("LinkedList<String> jest jego podtypem. Zadne przeciecie by nie powstalo.");
    }

    private static void varNieJestSlowemKluczowym() {
        naglowek("var to nie slowo kluczowe, tylko zarezerwowana nazwa typu");

        // Dlatego stary kod, ktory ma zmienna / metode / pakiet o nazwie "var", nadal sie kompiluje.
        int var = 10;
        System.out.println("zmienna o nazwie 'var' nadal legalna: " + var);

        System.out.println();
        System.out.println("Zakazane jest tylko nazwanie 'var' KLASY albo INTERFEJSU.");
        System.out.println("Dzieki temu wprowadzenie var w Javie 10 nie zepsulo zadnego istniejacego kodu.");
    }

    private static void kiedyUzywac() {
        naglowek("Kiedy var pomaga, a kiedy szkodzi");

        System.out.println("POMAGA - typ i tak jest widoczny po prawej stronie:");
        System.out.println("  var klient = new HttpClientBuilderFactory();");
        System.out.println("  var wpisy = new HashMap<String, List<Zamowienie>>();");
        System.out.println();
        System.out.println("SZKODZI - typ znika z pola widzenia:");
        System.out.println("  var wynik = serwis.przetworz(dane);   // co to jest? trzeba isc do definicji");
        System.out.println("  var x = 0;                            // int czy long? intencja niejasna");
        System.out.println();
        System.out.println("Oficjalna wskazowka JDK (Local-Variable Type Inference Style Guidelines):");
        System.out.println("czytelnosc kodu liczy sie bardziej niz krotkosc deklaracji - nazwa zmiennej");
        System.out.println("ma nadrabiac to, co znika razem z jawnym typem.");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
