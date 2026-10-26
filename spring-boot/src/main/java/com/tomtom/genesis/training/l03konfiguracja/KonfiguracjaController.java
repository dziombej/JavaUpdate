package com.tomtom.genesis.training.l03konfiguracja;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lekcja 3 - pokazuje, jaka konfiguracja faktycznie "wygrala".
 *
 * curl localhost:8080/api/konfiguracja
 * SZKOLENIE_KURS_NAZWA="Z env" mvn spring-boot:run      # zmienna srodowiskowa
 * mvn spring-boot:run -Dspring-boot.run.arguments=--szkolenie.kurs.nazwa=Z_argumentu
 */
@RestController
class KonfiguracjaController {

    private final KursProperties kurs;
    private final Environment srodowisko;
    private final String nazwaPrzezValue;
    private final String brakujacaZDomyslna;

    KonfiguracjaController(KursProperties kurs,
                           Environment srodowisko,
                           // @Value: pojedyncza wartosc, bez walidacji i bez podpowiedzi IDE.
                           @Value("${szkolenie.kurs.nazwa}") String nazwaPrzezValue,
                           // Skladnia ${klucz:domyslna} - bez ":" brak klucza = blad startu.
                           @Value("${szkolenie.nie-ma-takiego-klucza:wartosc domyslna}") String brakujacaZDomyslna) {
        this.kurs = kurs;
        this.srodowisko = srodowisko;
        this.nazwaPrzezValue = nazwaPrzezValue;
        this.brakujacaZDomyslna = brakujacaZDomyslna;
    }

    @GetMapping("/api/konfiguracja")
    Odpowiedz konfiguracja() {
        return new Odpowiedz(kurs, nazwaPrzezValue, brakujacaZDomyslna,
                Arrays.asList(srodowisko.getActiveProfiles()));
    }

    record Odpowiedz(KursProperties kurs, String nazwaPrzezValue,
                     String brakujacaZDomyslna, List<String> aktywneProfile) {
    }
}
