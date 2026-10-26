/**
 * Modul aplikacji.
 */
module com.tomtom.przyklad.aplikacja {

    // requires = deklarujemy zaleznosc. Bez tej linii kompilator nie widzi
    // ZADNEJ klasy z modulu uslugi, nawet z pakietu wyeksportowanego.
    requires com.tomtom.przyklad.uslugi;

    // java.base jest dodawany automatycznie do kazdego modulu - nie trzeba go wymieniac.
}
