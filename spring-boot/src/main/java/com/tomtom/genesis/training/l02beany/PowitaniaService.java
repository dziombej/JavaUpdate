package com.tomtom.genesis.training.l02beany;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Lekcja 2 - wstrzykiwanie przez konstruktor, trzy warianty naraz:
 * <ol>
 *   <li>{@code Powitanie domyslne} - jedna z wielu implementacji, wygrywa {@code @Primary};</li>
 *   <li>{@code @Qualifier("angielskie")} - konkretna, wskazana po nazwie;</li>
 *   <li>{@code List<Powitanie>} - WSZYSTKIE implementacje, ktore sa w kontekscie.</li>
 * </ol>
 */
@Service
public class PowitaniaService {

    private static final Logger log = LoggerFactory.getLogger(PowitaniaService.class);

    private final Powitanie domyslne;
    private final Powitanie angielskie;
    private final List<Powitanie> wszystkie;

    PowitaniaService(Powitanie domyslne,
                     @Qualifier("angielskie") Powitanie angielskie,
                     List<Powitanie> wszystkie) {
        this.domyslne = domyslne;
        this.angielskie = angielskie;
        this.wszystkie = wszystkie;
    }

    @PostConstruct
    void poUtworzeniu() {
        log.info("[lekcja 2] PowitaniaService gotowy, znane jezyki: {}",
                wszystkie.stream().map(Powitanie::jezyk).toList());
    }

    @PreDestroy
    void przedZniszczeniem() {
        log.info("[lekcja 2] PowitaniaService konczy prace");
    }

    public String powitaj(String imie) {
        return domyslne.powitaj(imie);
    }

    public String powitajPoAngielsku(String imie) {
        return angielskie.powitaj(imie);
    }

    public Map<String, String> powitajWszystkimi(String imie) {
        var wynik = new TreeMap<String, String>();
        wszystkie.forEach(p -> wynik.put(p.jezyk(), p.powitaj(imie)));
        return wynik;
    }
}
