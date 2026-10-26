package com.tomtom.genesis.training.l08klienci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * Lekcja 8 - test klienta HTTP bez serwera: {@code @RestClientTest}.
 *
 * {@link MockRestServiceServer} przechwytuje zadania wychodzace z RestClient
 * (zbudowanego z wstrzyknietego Buildera!) i odpowiada tym, co ustawisz.
 */
@RestClientTest(PrzewodnikKlient.class)
class PrzewodnikKlientTest {

    @Autowired
    PrzewodnikKlient przewodnik;

    @Autowired
    MockRestServiceServer serwer;

    @Test
    void opisuje_znalezione_miejsce() {
        serwer.expect(requestTo("http://localhost:8080/api/miejsca/1"))
                .andRespond(withSuccess("""
                        {"id": 1, "nazwa": "Wawel", "kategoria": "MUZEUM", "miasto": "Krakow",
                         "szerokosc": 50.05, "dlugosc": 19.93, "utworzono": "2026-01-01T00:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(przewodnik.opis(1)).isEqualTo("Wawel (MUZEUM, Krakow)");
        serwer.verify();
    }

    @Test
    void zamienia_404_na_pusty_optional() {
        serwer.expect(requestTo("http://localhost:8080/api/miejsca/5")).andRespond(withResourceNotFound());

        assertThat(przewodnik.znajdz(5)).isEmpty();
    }
}
