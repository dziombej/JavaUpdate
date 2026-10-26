package com.tomtom.przyklad.uslugi.impl;

import com.tomtom.przyklad.uslugi.api.Powitanie;

/**
 * Klasa PUBLICZNA, ale w pakiecie, ktory NIE jest eksportowany.
 *
 * Efekt: z innego modulu nie da sie jej ani zaimportowac (blad kompilacji),
 * ani zaladowac przez refleksje (IllegalAccessException w runtime).
 * W swiecie classpath bylaby dostepna dla kazdego.
 */
public class PolskiePowitanie implements Powitanie {

    @Override
    public String przywitaj(String imie) {
        return "Czesc, " + imie + "!";
    }
}
