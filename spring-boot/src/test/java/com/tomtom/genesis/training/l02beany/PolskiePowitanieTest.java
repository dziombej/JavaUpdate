package com.tomtom.genesis.training.l02beany;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * Lekcja 2 - zwykly test jednostkowy, BEZ Springa.
 *
 * To jest glowna korzysc z wstrzykiwania przez konstruktor: klase tworzysz
 * przez {@code new} i podajesz jej, co chcesz. Test trwa milisekundy.
 */
class PolskiePowitanieTest {

    private static final ZoneId WARSZAWA = ZoneId.of("Europe/Warsaw");

    @Test
    void rano_mowi_dzien_dobry() {
        var zegar = Clock.fixed(Instant.parse("2026-10-26T08:00:00Z"), WARSZAWA);

        assertThat(new PolskiePowitanie(zegar).powitaj("Ala")).isEqualTo("Dzien dobry, Ala!");
    }

    @Test
    void wieczorem_mowi_dobry_wieczor() {
        var zegar = Clock.fixed(Instant.parse("2026-10-26T19:30:00Z"), WARSZAWA);

        assertThat(new PolskiePowitanie(zegar).powitaj("Ala")).isEqualTo("Dobry wieczor, Ala!");
    }
}
