package com.tomtom.genesis.training.l05dane;

import java.time.Instant;

/** Lekcja 5 - to, co widzi klient API. Odciete od encji i od Hibernate. */
public record MiejsceDto(long id, String nazwa, Kategoria kategoria, String miasto,
                         double szerokosc, double dlugosc, Instant utworzono) {

    static MiejsceDto z(Miejsce m) {
        return new MiejsceDto(m.getId(), m.getNazwa(), m.getKategoria(), m.getMiasto(),
                m.getSzerokosc(), m.getDlugosc(), m.getUtworzono());
    }
}
