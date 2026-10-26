package com.tomtom.genesis.training.l02beany;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Lekcja 2 - bean istnieje TYLKO z aktywnym profilem "pirat".
 * Uruchom: mvn spring-boot:run -Dspring-boot.run.profiles=pirat
 */
@Component
@Profile("pirat")
class PirackiePowitanie implements Powitanie {

    @Override
    public String jezyk() {
        return "pirat";
    }

    @Override
    public String powitaj(String imie) {
        return "Ahoj, " + imie + ", szczurze ladowy!";
    }
}
