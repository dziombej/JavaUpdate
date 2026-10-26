package com.tomtom.genesis.training.l02beany;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lekcja 2 - okienko do eksperymentow z beanami. Szczegoly REST: lekcja 4.
 *
 * curl 'localhost:8080/api/powitania?imie=Ala'
 */
@RestController
class PowitaniaController {

    private final PowitaniaService powitania;

    PowitaniaController(PowitaniaService powitania) {
        this.powitania = powitania;
    }

    @GetMapping("/api/powitania")
    Map<String, String> powitania(@RequestParam(defaultValue = "Swiecie") String imie) {
        return powitania.powitajWszystkimi(imie);
    }
}
