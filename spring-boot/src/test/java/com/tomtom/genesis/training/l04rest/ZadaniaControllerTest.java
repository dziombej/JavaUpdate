package com.tomtom.genesis.training.l04rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.tomtom.genesis.training.l06testy.BeanyConfigDlaTestow;

/**
 * Lekcja 4 - test warstwy web: {@code @WebMvcTest}.
 *
 * Startuje tylko Spring MVC (kontrolery, advice, Jackson, walidacja).
 * Bez Tomcata - zadania ida przez MockMvc w pamieci. Bez bazy, bez serwisow.
 * ZadaniaStore dokladamy recznie przez {@code @Import}.
 *
 * {@link MockMvcTester} - nowy (Spring 6.2+) styl z asercjami AssertJ.
 */
@WebMvcTest(ZadaniaController.class)
@Import({ZadaniaStore.class, BeanyConfigDlaTestow.class})
class ZadaniaControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void lista_zwraca_zadania_startowe() {
        assertThat(mvc.get().uri("/api/zadania"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].tytul").isEqualTo("Przeczytac docs/04-rest.md");
    }

    @Test
    void filtr_po_priorytecie() {
        assertThat(mvc.get().uri("/api/zadania").param("priorytet", "SREDNI"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()").isEqualTo(1);
    }

    @Test
    void post_tworzy_zadanie_i_zwraca_location() {
        assertThat(mvc.post().uri("/api/zadania")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tytul": "Napisac test", "priorytet": "NISKI"}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/api/zadania/3")
                .bodyJson()
                .extractingPath("$.zrobione").isEqualTo(false);
    }

    @Test
    void brak_zadania_to_404_z_problem_detail() {
        assertThat(mvc.get().uri("/api/zadania/{id}", 42))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.id").isEqualTo(42);
    }

    @Test
    void walidacja_zwraca_400_z_lista_bledow() {
        assertThat(mvc.post().uri("/api/zadania")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tytul": "", "priorytet": null}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.bledy").asMap().containsOnlyKeys("tytul", "priorytet");
    }
}
