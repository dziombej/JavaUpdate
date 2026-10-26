package com.tomtom.genesis.training.l09nowosci;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Lekcja 9 - wersjonowanie API i resilience w pelnym kontekscie.
 * {@code @AutoConfigureMockMvc}: caly kontekst, ale zadania przez MockMvc (bez portu).
 */
@SpringBootTest
@AutoConfigureMockMvc
class NowosciTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    NiestabilnaUsluga usluga;

    @Test
    void bez_naglowka_wersja_domyslna() {
        assertThat(mvc.get().uri("/api/nowosci/powitanie"))
                .hasStatusOk()
                .bodyText().isEqualTo("Czesc, Swiecie");
    }

    @Test
    void wersja_2_i_nowsze() {
        for (var wersja : new String[] {"2.0", "3.0"}) {
            assertThat(mvc.get().uri("/api/nowosci/powitanie").header("API-Version", wersja))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.jezyk").isEqualTo("pl");
        }
    }

    @Test
    void nieobslugiwana_wersja_to_400() {
        assertThat(mvc.get().uri("/api/nowosci/powitanie").header("API-Version", "9.9"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void retryable_ukrywa_chwilowe_awarie() {
        // Dwie pierwsze proby padaja, trzecia sie udaje - wywolujacy widzi tylko sukces.
        int proba = usluga.pobierzDane();

        assertThat(proba % 3).isZero();
    }

    @Test
    void concurrency_limit_pilnuje_rownoleglosci() throws Exception {
        try (var watki = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 20; i++) {
                watki.submit(() -> {
                    usluga.wolnaOperacja();
                    return null;
                });
            }
        }

        assertThat(usluga.maksRownolegle()).isLessThanOrEqualTo(2);
    }
}
