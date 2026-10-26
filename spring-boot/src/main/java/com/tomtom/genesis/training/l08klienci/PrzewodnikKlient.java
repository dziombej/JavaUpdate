package com.tomtom.genesis.training.l08klienci;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.tomtom.genesis.training.l05dane.MiejsceDto;

/**
 * Lekcja 8 - imperatywny, "plynny" klient: {@link RestClient}.
 *
 * Nastepca RestTemplate. Zawsze buduj go z wstrzyknietego
 * {@link RestClient.Builder} - Boot podpina w nim JsonMapper, metryki,
 * tracing i timeouty z {@code spring.http.clients.*}.
 */
@Service
public class PrzewodnikKlient {

    private final RestClient klient;

    PrzewodnikKlient(RestClient.Builder budowniczy,
                     @Value("${szkolenie.przewodnik.base-url}") String adresBazowy) {
        this.klient = budowniczy.baseUrl(adresBazowy).build();
    }

    /** 404 zamieniamy na Optional.empty(), kazdy inny blad leci dalej jako wyjatek. */
    public Optional<MiejsceDto> znajdz(long id) {
        return klient.get()
                .uri("/api/miejsca/{id}", id)
                .exchange((zadanie, odpowiedz) -> {
                    if (odpowiedz.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Optional.empty();
                    }
                    if (odpowiedz.getStatusCode().isError()) {
                        throw new IllegalStateException("Serwer odpowiedzial " + odpowiedz.getStatusCode());
                    }
                    return Optional.ofNullable(odpowiedz.bodyTo(MiejsceDto.class));
                });
    }

    public String opis(long id) {
        return znajdz(id)
                .map(m -> "%s (%s, %s)".formatted(m.nazwa(), m.kategoria(), m.miasto()))
                .orElse("nie znam miejsca " + id);
    }
}
