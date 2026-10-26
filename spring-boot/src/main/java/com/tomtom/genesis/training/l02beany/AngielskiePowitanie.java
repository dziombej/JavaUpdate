package com.tomtom.genesis.training.l02beany;

import org.springframework.stereotype.Component;

/**
 * Lekcja 2 - druga implementacja z jawna nazwa beana "angielskie".
 * Bez nazwy bean nazywalby sie "angielskiePowitanie" (nazwa klasy z mala litera).
 */
@Component("angielskie")
class AngielskiePowitanie implements Powitanie {

    @Override
    public String jezyk() {
        return "en";
    }

    @Override
    public String powitaj(String imie) {
        return "Hello, " + imie + "!";
    }
}
