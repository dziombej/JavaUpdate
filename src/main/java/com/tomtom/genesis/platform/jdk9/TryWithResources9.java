package com.tomtom.genesis.platform.jdk9;

/**
 * Java 9 (JEP 213) - try-with-resources na istniejacej zmiennej.
 *
 * W Javie 7/8 zasob MUSIAL byc zadeklarowany wewnatrz nawiasow try(...).
 * Od Javy 9 wystarczy, ze zmienna jest final albo efektywnie final.
 */
public class TryWithResources9 {

    /** Prosty zasob, ktory glosno mowi, co sie z nim dzieje. */
    static final class Polaczenie implements AutoCloseable {
        private final String nazwa;

        Polaczenie(String nazwa) {
            this.nazwa = nazwa;
            System.out.println("  otwarto: " + nazwa);
        }

        void uzyj() {
            System.out.println("  uzywam: " + nazwa);
        }

        @Override
        public void close() {
            System.out.println("  zamknieto: " + nazwa);
        }
    }

    public static void main(String[] args) {
        pojava8();
        pojava9();
        kolejnoscZamykania();
        pulapkaNull();
    }

    private static void pojava8() {
        System.out.println("=== Skladnia z Javy 7/8 - zasob deklarowany w nawiasach ===");
        try (Polaczenie p = new Polaczenie("db")) {
            p.uzyj();
        }
    }

    private static void pojava9() {
        System.out.println();
        System.out.println("=== Skladnia z Javy 9 - zmienna efektywnie final ===");

        // Zmienna powstaje wczesniej (np. dostajemy ja z metody albo z pola),
        // a my chcemy tylko objac ja zarzadzaniem zasobem.
        Polaczenie p = new Polaczenie("db-z-zewnatrz");

        try (p) {          // <- to jest nowosc; wczesniej trzeba bylo pisac try (Polaczenie q = p)
            p.uzyj();
        }

        System.out.println();
        System.out.println("Warunek: p nie moze byc przypisane ponownie. Gdyby ponizej byla linia");
        System.out.println("p = new Polaczenie(...), kompilator odrzucilby 'try (p)' z bledem");
        System.out.println("\"local variables referenced from a resource must be final or effectively final\".");
    }

    private static void kolejnoscZamykania() {
        System.out.println();
        System.out.println("=== Kolejnosc zamykania: odwrotna do otwierania ===");
        Polaczenie pierwsze = new Polaczenie("pierwsze");
        Polaczenie drugie = new Polaczenie("drugie");
        try (pierwsze; drugie) {   // kilka zasobow oddzielamy srednikiem
            pierwsze.uzyj();
            drugie.uzyj();
        }
        System.out.println("(zamkniete w kolejnosci: drugie, pierwsze)");
    }

    private static void pulapkaNull() {
        System.out.println();
        System.out.println("=== Pulapka: zasob moze byc null ===");

        Polaczenie brak = null;
        try (brak) {           // kompiluje sie!
            System.out.println("  wewnatrz try, zasob = null");
        }
        System.out.println("Wyjscie z bloku bez NPE - try-with-resources pomija close() dla null.");
        System.out.println("Ale uwaga: proba uzycia takiego zasobu w srodku bloku to juz zwykle NPE.");
    }
}
