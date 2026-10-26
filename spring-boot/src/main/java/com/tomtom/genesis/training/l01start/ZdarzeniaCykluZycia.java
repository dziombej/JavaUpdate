package com.tomtom.genesis.training.l01start;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lekcja 1 - cykl zycia aplikacji jako zdarzenia.
 *
 * Kolejnosc: ApplicationStartedEvent -> (ApplicationRunner/CommandLineRunner)
 * -> ApplicationReadyEvent -> ... -> ContextClosedEvent przy Ctrl+C.
 * Porownaj w logu, kiedy pojawia sie RAPORT STARTOWY.
 */
@Component
class ZdarzeniaCykluZycia {

    private static final Logger log = LoggerFactory.getLogger(ZdarzeniaCykluZycia.class);

    @EventListener
    void wystartowala(ApplicationStartedEvent zdarzenie) {
        log.info("[cykl zycia] ApplicationStartedEvent - kontekst odswiezony, runnery jeszcze nie ruszyly");
    }

    @EventListener
    void gotowa(ApplicationReadyEvent zdarzenie) {
        log.info("[cykl zycia] ApplicationReadyEvent - aplikacja gotowa, start trwal {} ms",
                zdarzenie.getTimeTaken().toMillis());
    }

    @EventListener
    void zamykanie(ContextClosedEvent zdarzenie) {
        log.info("[cykl zycia] ContextClosedEvent - zamykanie kontekstu");
    }
}
