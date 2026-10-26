package com.tomtom.genesis.training.l02beany;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Lekcja 2 - zasieg (scope) beana.
 *
 * Domyslny zasieg to singleton: jedna instancja na kontekst.
 * Prototype: nowa instancja przy KAZDYM pobraniu z kontenera.
 * Eksperyment: usun {@code @Scope} i uruchom BeanyKontekstTest.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LicznikZadan {

    private final AtomicInteger licznik = new AtomicInteger();

    public int nastepny() {
        return licznik.incrementAndGet();
    }
}
