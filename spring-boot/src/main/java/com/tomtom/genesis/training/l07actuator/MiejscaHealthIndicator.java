package com.tomtom.genesis.training.l07actuator;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.tomtom.genesis.training.l05dane.MiejsceRepository;

/**
 * Lekcja 7 - wlasny wskaznik zdrowia.
 *
 * Nazwa beana "miejscaHealthIndicator" -> komponent "miejsca" w /actuator/health
 * (Boot obcina koncowke "HealthIndicator").
 * Eksperyment: usun wszystkie miejsca (DELETE /api/miejsca/{id}) i zobacz,
 * jak /actuator/health zmienia status na DOWN i kod HTTP na 503.
 */
@Component
class MiejscaHealthIndicator implements HealthIndicator {

    private final MiejsceRepository repozytorium;

    MiejscaHealthIndicator(MiejsceRepository repozytorium) {
        this.repozytorium = repozytorium;
    }

    @Override
    public Health health() {
        long liczba = repozytorium.count();
        if (liczba == 0) {
            return Health.down().withDetail("powod", "brak miejsc w bazie").build();
        }
        return Health.up().withDetail("liczbaMiejsc", liczba).build();
    }
}
