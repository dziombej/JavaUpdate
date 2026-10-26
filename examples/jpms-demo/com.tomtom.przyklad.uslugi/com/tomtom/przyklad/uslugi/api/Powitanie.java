package com.tomtom.przyklad.uslugi.api;

/** Publiczne API modulu - ten pakiet jest wyeksportowany. */
public interface Powitanie {

    String przywitaj(String imie);

    /** Metoda fabryczna, zeby wolajacy nie musial znac klasy implementujacej. */
    static Powitanie domyslne() {
        return new com.tomtom.przyklad.uslugi.impl.PolskiePowitanie();
    }
}
