package com.tomtom.genesis.training.l02beany;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lekcja 2 - beany z metod {@code @Bean}.
 *
 * Uzywasz tego, gdy klasa nie jest Twoja (nie dopiszesz jej {@code @Component}),
 * albo gdy tworzenie wymaga logiki. Nazwa beana = nazwa metody ("zegar").
 */
@Configuration(proxyBeanMethods = false)
class BeanyConfig {

    @Bean
    Clock zegar() {
        return Clock.systemDefaultZone();
    }
}
