package com.tomtom.genesis.training.l04rest;

import java.time.Instant;

/**
 * Lekcja 4 - model zwracany przez API. Rekord serializuje sie do JSON
 * bez zadnych adnotacji (Jackson 3 zna rekordy).
 */
public record Zadanie(long id, String tytul, String opis, Priorytet priorytet,
                      boolean zrobione, Instant utworzono) {

    Zadanie zrobione(boolean zrobione) {
        return new Zadanie(id, tytul, opis, priorytet, zrobione, utworzono);
    }
}
