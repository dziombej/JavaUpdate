package com.tomtom.genesis.training.l07actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.tomtom.genesis.training.l05dane.MiejsceUtworzone;

/**
 * Lekcja 7 - wlasna metryka w Micrometerze, podpieta przez zdarzenie.
 *
 * MiejscaService (lekcja 5) nie wie o metrykach - publikuje tylko
 * {@link MiejsceUtworzone}. Ten bean slucha i zwieksza licznik.
 *
 * curl localhost:8080/actuator/metrics/szkolenie.miejsca.utworzone
 * curl 'localhost:8080/actuator/metrics/szkolenie.miejsca.utworzone?tag=kategoria:HOTEL'
 */
@Component
class MetrykiMiejsc {

    private final MeterRegistry rejestr;

    MetrykiMiejsc(MeterRegistry rejestr) {
        this.rejestr = rejestr;
    }

    @EventListener
    void utworzono(MiejsceUtworzone zdarzenie) {
        Counter.builder("szkolenie.miejsca.utworzone")
                .description("Liczba miejsc dodanych przez API")
                .tag("kategoria", zdarzenie.kategoria().name())
                .register(rejestr)
                .increment();
    }
}
