package com.tomtom.przyklad.aplikacja;

import com.tomtom.przyklad.uslugi.api.Powitanie;

public class Start {

    public static void main(String[] args) {
        naglowek("Dostep do pakietu WYEKSPORTOWANEGO");

        Powitanie powitanie = Powitanie.domyslne();
        System.out.println(powitanie.przywitaj("Lukasz"));
        System.out.println("dziala, bo modul uslugi ma: exports com.tomtom.przyklad.uslugi.api;");

        naglowek("Proba dostepu do pakietu NIEEKSPORTOWANEGO");

        System.out.println("Ta linia sie NIE kompiluje:");
        System.out.println("  new com.tomtom.przyklad.uslugi.impl.PolskiePowitanie()");
        System.out.println("  blad: package com.tomtom.przyklad.uslugi.impl is not visible");
        System.out.println();
        System.out.println("A czy refleksja obejdzie zakaz? Sprobujmy:");

        try {
            Class<?> klasa = Class.forName("com.tomtom.przyklad.uslugi.impl.PolskiePowitanie");
            Object obiekt = klasa.getDeclaredConstructor().newInstance();
            System.out.println("  udalo sie utworzyc: " + obiekt);
            System.out.println("  <- na --module-path to by sie NIE udalo (IllegalAccessException)");
        } catch (ClassNotFoundException e) {
            System.out.println("  ClassNotFoundException - klasa niewidoczna");
        } catch (IllegalAccessException e) {
            System.out.println("  IllegalAccessException: " + e.getMessage());
            System.out.println("  <- refleksja NIE obchodzi hermetyzacji modulow.");
            System.out.println("     Na classpath ten sam kod by zadzialal - sekcja 4 skryptu to pokazuje.");
        } catch (ReflectiveOperationException e) {
            System.out.println("  " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        naglowek("Introspekcja modulu w runtime");

        Module mojModul = Start.class.getModule();
        Module modulUslug = Powitanie.class.getModule();

        boolean nazwany = modulUslug.isNamed();

        System.out.println("nazwa mojego modulu          = " + mojModul.getName()
                + (mojModul.isNamed() ? "" : "   <- null znaczy: modul NIENAZWANY (classpath)"));
        System.out.println("nazwa modulu uslug           = " + modulUslug.getName());
        System.out.println("czy czytam modul uslug?      = " + mojModul.canRead(modulUslug));
        System.out.println("czy uslugi eksportuja api?   = "
                + modulUslug.isExported("com.tomtom.przyklad.uslugi.api"));
        System.out.println("czy uslugi eksportuja impl?  = "
                + modulUslug.isExported("com.tomtom.przyklad.uslugi.impl"));
        System.out.println("czy uslugi OTWIERAJA impl?   = "
                + modulUslug.isOpen("com.tomtom.przyklad.uslugi.impl"));
        System.out.println();

        if (nazwany) {
            System.out.println("Modul jest NAZWANY - module-info.class zostal odczytany, wiec");
            System.out.println("reguly exports/opens obowiazuja i dostep do impl zostal zablokowany.");
        } else {
            System.out.println("Modul jest NIENAZWANY - kod zostal wczytany z CLASSPATH, a module-info.class");
            System.out.println("zignorowany. Modul nienazwany eksportuje i otwiera WSZYSTKIE swoje pakiety,");
            System.out.println("dlatego isExported i isOpen zwracaja true, a refleksja wyzej sie powiodla.");
            System.out.println("To ta sama klasa i ten sam module-info - inny tylko sposob uruchomienia.");
        }
        System.out.println();
        System.out.println("nasze moduly w warstwie startowej:");
        long nasze = ModuleLayer.boot().modules().stream()
                .map(Module::getName)
                .filter(n -> n != null && n.startsWith("com.tomtom"))
                .sorted()
                .peek(n -> System.out.println("  " + n))
                .count();
        if (nasze == 0) {
            System.out.println("  (zadnych - na classpath nasz kod nie tworzy modulow)");
        }
        System.out.println("  lacznie modulow w warstwie startowej: "
                + ModuleLayer.boot().modules().size());
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
