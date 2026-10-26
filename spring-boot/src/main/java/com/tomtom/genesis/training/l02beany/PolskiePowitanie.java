package com.tomtom.genesis.training.l02beany;

import java.time.Clock;
import java.time.LocalTime;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Lekcja 2 - implementacja domyslna.
 *
 * {@code @Primary}: gdy ktos prosi o "jakies" {@link Powitanie}, dostaje to.
 * Zaleznosc od {@link Clock} zamiast {@code LocalTime.now()} - dzieki temu
 * w tescie podstawisz staly czas bez Springa (zobacz PolskiePowitanieTest).
 */
@Component
@Primary
class PolskiePowitanie implements Powitanie {

    private final Clock zegar;

    PolskiePowitanie(Clock zegar) {
        this.zegar = zegar;
    }

    @Override
    public String jezyk() {
        return "pl";
    }

    @Override
    public String powitaj(String imie) {
        var teraz = LocalTime.now(zegar);
        var zwrot = teraz.isBefore(LocalTime.of(18, 0)) ? "Dzien dobry" : "Dobry wieczor";
        return zwrot + ", " + imie + "!";
    }
}
