package com.tomtom.genesis.platform.jdk11;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Java 11 (JEP 181) - Nest-Based Access Control.
 *
 * Zmiana niewidoczna w skladni, ale wazna dla refleksji, frameworkow i bajtkodu.
 *
 * Problem: w kodzie zrodlowym klasa zewnetrzna i jej klasy zagniezdzone widza
 * nawzajem swoje prywatne skladowe. W bajtkodzie to jednak OSOBNE klasy, a JVM
 * nie pozwala klasie A siegnac do private klasy B. Do Javy 10 javac obchodzil
 * to, generujac syntetyczne mosty o nazwach access$000, access$100 itd.
 *
 * Java 11 wprowadza pojecie "gniazda" (nest) na poziomie JVM: klasy z jednego
 * pliku zrodlowego naleza do wspolnego gniazda i moga siegac do swoich private
 * bezposrednio. Syntetyczne mosty znikaja.
 */
public class NestBasedAccess {

    private String sekretZewnetrzny = "prywatne pole klasy zewnetrznej";

    private void metodaPrywatnaZewnetrzna() {
        System.out.println("  wywolano prywatna metode klasy zewnetrznej");
    }

    static class Wewnetrzna {
        private String sekretWewnetrzny = "prywatne pole klasy wewnetrznej";

        private void metodaPrywatnaWewnetrzna() {
            System.out.println("  wywolano prywatna metode klasy wewnetrznej");
        }

        void siegnijDoZewnetrznej(NestBasedAccess zewnetrzna) {
            // Legalne od zawsze na poziomie zrodla - dawniej przez most access$000.
            System.out.println("  z wewnetrznej widze: " + zewnetrzna.sekretZewnetrzny);
            zewnetrzna.metodaPrywatnaZewnetrzna();
        }
    }

    public static void main(String[] args) throws Exception {
        new NestBasedAccess().wzajemnyDostep();
        gniazdoWRefleksji();
        refleksjaBezSetAccessible();
        jakToSprawdzic();
    }

    private void wzajemnyDostep() {
        naglowek("Wzajemny dostep do prywatnych skladowych");

        Wewnetrzna w = new Wewnetrzna();
        System.out.println("z zewnetrznej widze: " + w.sekretWewnetrzny);
        w.metodaPrywatnaWewnetrzna();
        w.siegnijDoZewnetrznej(this);

        System.out.println();
        System.out.println("Skladniowo nic sie nie zmienilo - ten kod kompilowal sie tez w Javie 8.");
        System.out.println("Zmienilo sie to, CO generuje kompilator i jak JVM sprawdza dostep.");
    }

    private static void gniazdoWRefleksji() {
        naglowek("Nowe API refleksji: getNestHost / getNestMembers / isNestmateOf");

        System.out.println("gospodarz gniazda dla Wewnetrzna = "
                + Wewnetrzna.class.getNestHost().getSimpleName());
        System.out.println("gospodarz gniazda dla NestBasedAccess = "
                + NestBasedAccess.class.getNestHost().getSimpleName());

        String czlonkowie = Arrays.stream(NestBasedAccess.class.getNestMembers())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        System.out.println("czlonkowie gniazda = " + czlonkowie);

        System.out.println("NestBasedAccess.isNestmateOf(Wewnetrzna) = "
                + NestBasedAccess.class.isNestmateOf(Wewnetrzna.class));
        System.out.println("NestBasedAccess.isNestmateOf(String)     = "
                + NestBasedAccess.class.isNestmateOf(String.class));

        System.out.println();
        System.out.println("W pliku .class informacja siedzi w dwoch nowych atrybutach:");
        System.out.println("  NestHost    - w klasach zagniezdzonych, wskazuje gospodarza");
        System.out.println("  NestMembers - w gospodarzu, wylicza czlonkow gniazda");
    }

    private static void refleksjaBezSetAccessible() throws Exception {
        naglowek("Najbardziej praktyczny skutek: refleksja bez setAccessible(true)");

        Wewnetrzna w = new Wewnetrzna();
        Field pole = Wewnetrzna.class.getDeclaredField("sekretWewnetrzny");

        // Brak pole.setAccessible(true) - a mimo to dziala, bo klasa wolajaca
        // (NestBasedAccess) jest w tym samym gniezdzie co Wewnetrzna.
        Object wartosc = pole.get(w);
        System.out.println("odczytano przez refleksje bez setAccessible: " + wartosc);

        System.out.println();
        System.out.println("W Javie 8 ta sama linia rzucala IllegalAccessException, mimo ze");
        System.out.println("zwykly odczyt w.sekretWewnetrzny kompilowal sie bez problemu.");
        System.out.println("Refleksja byla NIESPOJNA z regulami jezyka. JEP 181 to naprawil.");

        // Dla porownania: pole spoza gniazda nadal wymaga setAccessible.
        try {
            Field obce = String.class.getDeclaredField("hash");
            obce.get("tekst");
        } catch (IllegalAccessException e) {
            System.out.println();
            System.out.println("pole spoza gniazda (String.hash) -> IllegalAccessException, zgodnie z oczekiwaniem");
        } catch (NoSuchFieldException e) {
            System.out.println();
            System.out.println("(pole String.hash niedostepne w tej wersji JDK - pomijam ten fragment)");
        }
    }

    private static void jakToSprawdzic() {
        naglowek("Jak zobaczyc roznice na wlasne oczy");

        System.out.println("Skompiluj te klase i obejrzyj bajtkod:");
        System.out.println("  javap -p -c target/classes/com/tomtom/genesis/platform/jdk11/NestBasedAccess.class");
        System.out.println();
        System.out.println("Na Javie 11+ zobaczysz atrybuty NestHost / NestMembers i BRAK metod access$000.");
        System.out.println("Na Javie 8 zobaczylbys syntetyczne 'static String access$000(NestBasedAccess)'.");
        System.out.println();
        System.out.println("Dlaczego to wazne poza ciekawostka:");
        System.out.println("  - mniej wygenerowanego kodu, mniejsze pliki .class,");
        System.out.println("  - syntetyczne mosty byly PACKAGE-PRIVATE, wiec prywatne pole dalo sie");
        System.out.println("    odczytac z innej klasy w tym samym pakiecie - to byla dziura w hermetyzacji,");
        System.out.println("  - narzedzia generujace bajtkod (Lombok, Mockito, proxy Springa, kompilatory");
        System.out.println("    Kotlina i Scali) moga generowac prostszy i szybszy kod,");
        System.out.println("  - to fundament, na ktorym pozniej zbudowano klasy zapieczetowane (Java 17)");
        System.out.println("    i klasy ukryte (hidden classes, Java 15).");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
