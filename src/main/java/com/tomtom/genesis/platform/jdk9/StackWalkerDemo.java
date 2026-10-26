package com.tomtom.genesis.platform.jdk9;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Java 9 (JEP 259) - StackWalker.
 *
 * Problem z Thread.currentThread().getStackTrace() i new Throwable().getStackTrace():
 * obie metody materializuja CALY slad stosu do tablicy StackTraceElement[], nawet jesli
 * potrzebujesz jednej ramki. StackWalker chodzi po stosie leniwie.
 */
public class StackWalkerDemo {

    public static void main(String[] args) {
        poziomA();
    }

    private static void poziomA() {
        poziomB();
    }

    private static void poziomB() {
        poziomC();
    }

    private static void poziomC() {
        leniwyPrzeglad();
        kilkaPierwszychRamek();
        ktoMnieZawolal();
        porownanieZeStarymAPI();
    }

    private static void leniwyPrzeglad() {
        naglowek("StackWalker - przeglad stosu jako strumien");

        List<String> ramki = StackWalker.getInstance()
                .walk(strumien -> strumien
                        .map(f -> f.getClassName() + "#" + f.getMethodName() + ":" + f.getLineNumber())
                        .collect(Collectors.toList()));

        System.out.println("wszystkie ramki:");
        ramki.forEach(r -> System.out.println("  " + r));

        System.out.println();
        System.out.println("walk(...) dostaje Stream<StackFrame> wazny TYLKO w trakcie wywolania.");
        System.out.println("Nie wolno go zwrocic na zewnatrz ani zapisac do pola - ramki sa");
        System.out.println("pobierane leniwie z zywego stosu i po wyjsciu z walk() sa nieaktualne.");
    }

    private static void kilkaPierwszychRamek() {
        naglowek("Pobranie tylko 2 ramek - reszta stosu nie jest w ogole materializowana");

        List<String> dwie = StackWalker.getInstance()
                .walk(s -> s.limit(2).map(StackFrame::getMethodName).collect(Collectors.toList()));
        System.out.println("2 pierwsze metody = " + dwie);

        // Pomijanie wlasnych ramek (typowe dla bibliotek logujacych):
        List<String> zPominieciem = StackWalker.getInstance()
                .walk(s -> s.skip(2).limit(3).map(StackFrame::getMethodName).collect(Collectors.toList()));
        System.out.println("po pominieciu 2   = " + zPominieciem);
    }

    private static void ktoMnieZawolal() {
        naglowek("getCallerClass - kto mnie wywolal");

        // getCallerClass wymaga opcji RETAIN_CLASS_REFERENCE, bo zwraca Class<?>,
        // a nie samo String z nazwa. To operacja chroniona uprawnieniem.
        Class<?> wolajacy = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        System.out.println("klasa wolajacego = " + wolajacy.getName());

        System.out.println();
        System.out.println("To zamiennik dla dawnego, niestandardowego sun.reflect.Reflection.getCallerClass().");
    }

    private static void porownanieZeStarymAPI() {
        naglowek("Porownanie kosztu - i dlaczego glebokosc stosu ma znaczenie");

        // Kluczowa przewaga StackWalkera ujawnia sie na GLEBOKIM stosie, gdy potrzebujemy
        // tylko kilku pierwszych ramek. Na plytkim stosie roznica jest niewielka - narzut
        // na utworzenie strumienia zjada wiekszosc zysku.
        zmierzNaGlebokosci(3);
        zmierzNaGlebokosci(200);

        System.out.println();
        System.out.println("Interpretacja:");
        System.out.println("  new Throwable().getStackTrace() kosztuje proporcjonalnie do CALEJ glebokosci stosu -");
        System.out.println("  zawsze materializuje kazda ramke do obiektu StackTraceElement.");
        System.out.println("  StackWalker z limit(1) kosztuje mniej wiecej tyle samo niezaleznie od glebokosci,");
        System.out.println("  bo schodzi po stosie leniwie i przerywa po pierwszej ramce.");
        System.out.println();
        System.out.println("  Efekt: na plytkim stosie roznica jest niewielka i moze sie wahac miedzy");
        System.out.println("  uruchomieniami. Na glebokim stosie StackWalker wygrywa o rzedy wielkosci,");
        System.out.println("  i przewaga rosnie liniowo z glebokoscia.");
        System.out.println();
        System.out.println("  To wlasnie dlatego biblioteki logujace (Log4j 2 od wersji 2.9) przeszly");
        System.out.println("  na StackWalkera - w aplikacji z Springiem stos ma czesto 100+ ramek,");
        System.out.println("  a do zalogowania nazwy klasy potrzebna jest jedna.");
        System.out.println();
        System.out.println("Zastrzezenie: to mikrobenchmark z prosta rozgrzewka, ale bez izolacji od GC");
        System.out.println("i bez powtorzonych forkow. Traktuj liczby jako ilustracje trendu, a nie");
        System.out.println("jako pomiar - rzetelny pomiar wymaga JMH.");
    }

    private static void zmierzNaGlebokosci(int glebokosc) {
        if (glebokosc > 0) {
            zmierzNaGlebokosci(glebokosc - 1);
            return;
        }

        int powtorzenia = 20_000;

        // Rozgrzewka, zeby JIT skompilowal obie sciezki przed pomiarem.
        for (int i = 0; i < 5_000; i++) {
            zuzyj(new Throwable().getStackTrace().length);
            zuzyj(WALKER.walk(s -> s.limit(1).count()));
        }

        long start = System.nanoTime();
        for (int i = 0; i < powtorzenia; i++) {
            zuzyj(new Throwable().getStackTrace().length);
        }
        long czasStare = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < powtorzenia; i++) {
            zuzyj(WALKER.walk(s -> s.limit(1).count()));
        }
        long czasNowe = System.nanoTime() - start;

        int realnaGlebokosc = new Throwable().getStackTrace().length;
        System.out.printf("glebokosc stosu ~%3d ramek | Throwable.getStackTrace: %6.1f ms "
                        + "| StackWalker.limit(1): %6.1f ms%n",
                realnaGlebokosc, czasStare / 1_000_000.0, czasNowe / 1_000_000.0);
    }

    private static final StackWalker WALKER = StackWalker.getInstance();

    /** Blackhole - zeby JIT nie usunal pomiaru jako martwego kodu. */
    private static long suma;

    private static void zuzyj(long wartosc) {
        suma += wartosc;
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
