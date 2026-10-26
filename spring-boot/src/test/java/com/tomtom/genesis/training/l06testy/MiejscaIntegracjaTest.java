package com.tomtom.genesis.training.l06testy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.tomtom.genesis.training.l05dane.Kategoria;
import com.tomtom.genesis.training.l05dane.MiejsceDto;
import com.tomtom.genesis.training.l05dane.NoweMiejsce;

/**
 * Lekcja 6 - test integracyjny calej aplikacji.
 *
 * {@code RANDOM_PORT}: prawdziwy Tomcat na wolnym porcie, prawdziwa baza H2,
 * wszystkie beany. Najwolniejszy rodzaj testu, ale najblizszy produkcji.
 *
 * {@link RestTestClient} (Spring 7) - nastepca TestRestTemplate, z tym samym
 * API co WebTestClient, ale bez zaleznosci od WebFluxa.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MiejscaIntegracjaTest {

    @Autowired
    RestTestClient klient;

    @Test
    void tworzy_i_odczytuje_miejsce_przez_http() {
        var utworzone = klient.post().uri("/api/miejsca")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new NoweMiejsce("Muzeum Bursztynu", Kategoria.MUZEUM, "Gdansk", 54.35, 18.65))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MiejsceDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(utworzone).isNotNull();

        klient.get().uri("/api/miejsca/{id}", utworzone.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nazwa").isEqualTo("Muzeum Bursztynu")
                .jsonPath("$.miasto").isEqualTo("Gdansk");
    }

    @Test
    void stronicowanie_przez_parametry_url() {
        klient.get().uri("/api/miejsca?szukaj=hotel&size=1&sort=nazwa,desc")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.page.size").isEqualTo(1);
    }

    @Test
    void nieznane_miejsce() {
        klient.get().uri("/api/miejsca/{id}", 999_999)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Miejsce nie istnieje");
    }
}
