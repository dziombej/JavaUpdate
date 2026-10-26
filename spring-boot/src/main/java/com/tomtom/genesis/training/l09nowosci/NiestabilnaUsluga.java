package com.tomtom.genesis.training.l09nowosci;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Lekcja 9 - odpornosc (resilience) wbudowana w Spring Framework 7.
 */
@Service
public class NiestabilnaUsluga {

    private static final Logger log = LoggerFactory.getLogger(NiestabilnaUsluga.class);

    private final AtomicInteger proby = new AtomicInteger();
    private final AtomicInteger wTrakcie = new AtomicInteger();
    private final AtomicInteger maksRownolegle = new AtomicInteger();

    /**
     * Pada co trzecie wywolanie... a wlasciwie dwa razy na trzy.
     * {@code @Retryable} ponawia do 3 razy, z opoznieniem 100 ms * 2^n.
     * Wywolujacy widzi tylko sukces (albo ostatni wyjatek, gdy limit sie skonczy).
     */
    @Retryable(includes = ChwilowaAwaria.class, maxRetries = 3, delay = 100, multiplier = 2)
    public int pobierzDane() {
        int proba = proby.incrementAndGet();
        log.info("[lekcja 9] proba nr {}", proba);
        if (proba % 3 != 0) {
            throw new ChwilowaAwaria("awaria przy probie " + proba);
        }
        return proba;
    }

    /**
     * {@code @ConcurrencyLimit(2)}: naraz najwyzej 2 watki w tej metodzie,
     * pozostale czekaja. Swietne z watkami wirtualnymi - zamiast malej puli
     * watkow chronisz konkretny zasob.
     */
    @ConcurrencyLimit(2)
    public void wolnaOperacja() throws InterruptedException {
        int teraz = wTrakcie.incrementAndGet();
        maksRownolegle.accumulateAndGet(teraz, Math::max);
        try {
            Thread.sleep(50);
        } finally {
            wTrakcie.decrementAndGet();
        }
    }

    public int maksRownolegle() {
        return maksRownolegle.get();
    }

    public static class ChwilowaAwaria extends RuntimeException {
        public ChwilowaAwaria(String komunikat) {
            super(komunikat);
        }
    }
}
