package com.tomtom.genesis.training.l09nowosci;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lekcja 9 - wersjonowanie API, watki wirtualne, resilience.
 */
@RestController
class NowosciController {

    private final NiestabilnaUsluga usluga;

    NowosciController(NiestabilnaUsluga usluga) {
        this.usluga = usluga;
    }

    // ---- Wersjonowanie API (Spring Framework 7) ----
    // Wersje czytamy z naglowka "API-Version" (application.yml, spring.mvc.apiversion.*).
    // curl localhost:8080/api/nowosci/powitanie                        -> wersja domyslna 1.0
    // curl -H 'API-Version: 2.0' localhost:8080/api/nowosci/powitanie  -> JSON
    // curl -H 'API-Version: 3.0' localhost:8080/api/nowosci/powitanie  -> JSON (bo "2.0+")
    // curl -H 'API-Version: 9.9' localhost:8080/api/nowosci/powitanie  -> 400 (nieobslugiwana)

    @GetMapping(path = "/api/nowosci/powitanie", version = "1.0")
    String powitanieV1(@RequestParam(defaultValue = "Swiecie") String imie) {
        return "Czesc, " + imie;
    }

    // "2.0+" = wersja 2.0 i kazda nowsza z listy supported, dopoki ktos nie zmapuje konkretnej.
    @GetMapping(path = "/api/nowosci/powitanie", version = "2.0+")
    PowitanieV2 powitanieV2(@RequestParam(defaultValue = "Swiecie") String imie,
                            @RequestParam(required = false) @Nullable String jezyk) {
        return new PowitanieV2("Czesc", imie, jezyk == null ? "pl" : jezyk);
    }

    record PowitanieV2(String zwrot, String imie, String jezyk) {
    }

    // ---- Watki wirtualne ----
    // spring.threads.virtual.enabled=true -> Tomcat obsluguje kazde zadanie w watku wirtualnym.
    @GetMapping("/api/nowosci/watek")
    Map<String, Object> watek() {
        var watek = Thread.currentThread();
        return Map.of("watek", watek.toString(), "wirtualny", watek.isVirtual());
    }

    // ---- @Retryable ----
    @GetMapping("/api/nowosci/niestabilna")
    Map<String, Integer> niestabilna() {
        return Map.of("udanaProbaNr", usluga.pobierzDane());
    }
}
