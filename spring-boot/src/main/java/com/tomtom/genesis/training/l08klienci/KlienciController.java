package com.tomtom.genesis.training.l08klienci;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.tomtom.genesis.training.l05dane.LicznikKategorii;

/**
 * Lekcja 8 - aplikacja wola SAMA SIEBIE przez HTTP, oboma klientami.
 * Dzieki temu nie potrzeba zadnego zewnetrznego serwisu.
 *
 * curl localhost:8080/api/klienci/demo/1
 * curl localhost:8080/api/klienci/demo/999
 */
@RestController
class KlienciController {

    private final PrzewodnikKlient przewodnik;
    private final MiejscaKlient miejsca;

    KlienciController(PrzewodnikKlient przewodnik, MiejscaKlient miejsca) {
        this.przewodnik = przewodnik;
        this.miejsca = miejsca;
    }

    @GetMapping("/api/klienci/demo/{id}")
    Demo demo(@PathVariable long id) {
        return new Demo(przewodnik.opis(id), miejsca.statystyki());
    }

    record Demo(String przezRestClient, List<LicznikKategorii> przezHttpInterface) {
    }
}
