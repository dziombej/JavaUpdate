package com.tomtom.genesis.training.l07actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Lekcja 7 - endpointy Actuatora sa zwyklym HTTP, wiec testujemy je jak API.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ActuatorTest {

    @Autowired
    RestTestClient klient;

    @Test
    void health_zawiera_nasz_wskaznik() {
        klient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components.miejsca.status").isEqualTo("UP")
                .jsonPath("$.components.miejsca.details.liczbaMiejsc").exists();
    }

    @Test
    void info_zawiera_dane_z_yml_i_z_contributora() {
        klient.get().uri("/actuator/info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.aplikacja.opis").isEqualTo("Szkolenie ze Spring Boot 4")
                .jsonPath("$.szkolenie.lekcje").isEqualTo(9)
                .jsonPath("$.java.version").exists();
    }

    @Test
    void wlasna_metryka_rosnie_po_dodaniu_miejsca() {
        klient.post().uri("/api/miejsca")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nazwa": "Parking Test", "kategoria": "PARKING", "miasto": "Lublin",
                         "szerokosc": 51.2, "dlugosc": 22.5}
                        """)
                .exchange()
                .expectStatus().isCreated();

        klient.get().uri("/actuator/metrics/szkolenie.miejsca.utworzone?tag=kategoria:PARKING")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.measurements[0].value").isEqualTo(1.0);
    }
}
