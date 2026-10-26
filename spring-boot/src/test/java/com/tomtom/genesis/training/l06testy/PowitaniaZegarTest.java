package com.tomtom.genesis.training.l06testy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.convention.TestBean;

import com.tomtom.genesis.training.l02beany.PowitaniaService;

/**
 * Lekcja 6 - podmiana JEDNEGO beana w pelnym kontekscie: {@code @TestBean}.
 *
 * Pole {@code zegar} zastepuje bean o tej samej nazwie/typie. Wartosc daje
 * statyczna metoda o tej samej nazwie. Reszta aplikacji jest prawdziwa.
 *
 * {@code WebEnvironment.NONE} - pelny kontekst, ale bez serwera WWW.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class PowitaniaZegarTest {

    @TestBean
    Clock zegar;

    static Clock zegar() {
        return Clock.fixed(Instant.parse("2026-12-24T20:00:00Z"), ZoneId.of("Europe/Warsaw"));
    }

    @Autowired
    PowitaniaService powitania;

    @Test
    void wigilijny_wieczor() {
        assertThat(powitania.powitaj("Mikolaju")).isEqualTo("Dobry wieczor, Mikolaju!");
    }
}
