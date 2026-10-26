package com.tomtom.genesis.training.l06testy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.tomtom.genesis.training.l05dane.Kategoria;
import com.tomtom.genesis.training.l05dane.LicznikKategorii;
import com.tomtom.genesis.training.l05dane.MiejscaController;
import com.tomtom.genesis.training.l05dane.MiejscaService;
import com.tomtom.genesis.training.l05dane.MiejsceDto;
import com.tomtom.genesis.training.l05dane.NoweMiejsce;

/**
 * Lekcja 6 - kontroler w izolacji, serwis podmieniony na mock.
 *
 * {@code @MockitoBean} (Spring 6.2+) zastepuje dawne {@code @MockBean} z Boota:
 * w kontekscie zamiast prawdziwego MiejscaService siedzi mock Mockito.
 * Kontroler nie wie o tym - dostaje go przez konstruktor jak zawsze.
 */
@WebMvcTest(MiejscaController.class)
class MiejscaControllerMockTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    MiejscaService serwis;

    @Test
    void zwraca_to_co_serwis() {
        given(serwis.statystyki()).willReturn(List.of(new LicznikKategorii(Kategoria.HOTEL, 99)));

        assertThat(mvc.get().uri("/api/miejsca/statystyki"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].liczba").isEqualTo(99);
    }

    @Test
    void niepoprawne_dane_nie_docieraja_do_serwisu() {
        assertThat(mvc.post().uri("/api/miejsca")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nazwa": "X", "kategoria": "HOTEL", "miasto": "Y", "szerokosc": 123, "dlugosc": 0}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST);

        then(serwis).shouldHaveNoInteractions();
    }

    @Test
    void poprawne_dane_ida_do_serwisu() {
        given(serwis.utworz(any())).willReturn(
                new MiejsceDto(7, "X", Kategoria.HOTEL, "Y", 1, 2, Instant.EPOCH));

        assertThat(mvc.post().uri("/api/miejsca")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nazwa": "X", "kategoria": "HOTEL", "miasto": "Y", "szerokosc": 1, "dlugosc": 2}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.id").isEqualTo(7);

        then(serwis).should().utworz(new NoweMiejsce("X", Kategoria.HOTEL, "Y", 1, 2));
    }
}
