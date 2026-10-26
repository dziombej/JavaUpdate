package com.tomtom.genesis.training.l08klienci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.tomtom.genesis.training.l05dane.Kategoria;
import com.tomtom.genesis.training.l05dane.NoweMiejsce;

/**
 * Lekcja 8 - HTTP Interface "od kuchni".
 *
 * W aplikacji proxy tworzy {@code @ImportHttpServices}. Tutaj robimy to recznie,
 * wskazujac losowy port serwera testowego - widac, ze to tylko RestClient
 * opakowany w interfejs.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MiejscaKlientTest {

    @LocalServerPort
    int port;

    MiejscaKlient klient;

    @BeforeEach
    void zbudujProxy() {
        var restClient = RestClient.create("http://localhost:" + port);
        klient = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(MiejscaKlient.class);
    }

    @Test
    void tworzy_i_pobiera() {
        var nowe = klient.utworz(new NoweMiejsce("Hotel Klient", Kategoria.HOTEL, "Rzeszow", 50.0, 22.0));

        assertThat(klient.pobierz(nowe.id()).nazwa()).isEqualTo("Hotel Klient");
        assertThat(klient.statystyki()).isNotEmpty();
    }

    @Test
    void blad_http_to_wyjatek() {
        assertThatThrownBy(() -> klient.pobierz(999_999))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}
