/**
 * Modul biblioteczny.
 *
 * Deklaracja modulu to plik module-info.java w KORZENIU katalogu ze zrodlami.
 * Nazwa modulu nie musi odpowiadac nazwie pakietu, ale konwencja mowi,
 * zeby uzywac odwroconej domeny - tak jak dla pakietow.
 */
module com.tomtom.przyklad.uslugi {

    // exports = ten pakiet jest widoczny dla WSZYSTKICH modulow, ktore nas require.
    exports com.tomtom.przyklad.uslugi.api;

    // Pakiet ...impl NIE jest eksportowany. To jest sedno JPMS:
    // przed Java 9 kazda klasa publiczna byla publiczna dla calego swiata.
    // Teraz "public" znaczy "publiczny w obrebie modulu, chyba ze pakiet jest wyeksportowany".
    //
    // Gdyby odkomentowac ponizsza linie, aplikacja mogla by siegnac do implementacji:
    // exports com.tomtom.przyklad.uslugi.impl;

    // Warianty, o ktorych warto wiedziec:
    //   exports pakiet to inny.modul;   - eksport kwalifikowany, tylko dla wskazanego modulu
    //   opens   pakiet;                 - otwarcie na refleksje (Spring, Jackson, Hibernate)
    //   opens   pakiet to inny.modul;   - otwarcie kwalifikowane
    //   requires transitive inny.modul; - kto wymaga nas, dostaje tez inny.modul
    //   requires static inny.modul;     - potrzebny do kompilacji, opcjonalny w runtime
}
