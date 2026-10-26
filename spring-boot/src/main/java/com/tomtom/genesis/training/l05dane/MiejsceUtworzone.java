package com.tomtom.genesis.training.l05dane;

/**
 * Lekcja 5/7 - zdarzenie domenowe. Serwis je publikuje i nie wie, kto slucha.
 * Slucha m.in. MetrykiMiejsc z lekcji 7 (licznik w Micrometerze).
 */
public record MiejsceUtworzone(long id, Kategoria kategoria) {
}
