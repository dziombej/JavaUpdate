package com.tomtom.genesis.training.l07actuator;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import com.tomtom.genesis.training.l04rest.ZadaniaStore;

/**
 * Lekcja 7 - wlasne dane w /actuator/info (obok info.* z application.yml,
 * wersji Javy i systemu operacyjnego).
 */
@Component
class SzkolenieInfoContributor implements InfoContributor {

    private final ZadaniaStore zadania;

    SzkolenieInfoContributor(ZadaniaStore zadania) {
        this.zadania = zadania;
    }

    @Override
    public void contribute(Info.Builder info) {
        info.withDetail("szkolenie", Map.of(
                "liczbaZadan", zadania.liczba(),
                "lekcje", 9));
    }
}
