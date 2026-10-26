package com.tomtom.genesis.training.l06testy;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Lekcja 6 - {@code @TestConfiguration}: beany tylko dla testow.
 *
 * Testy slice ({@code @WebMvcTest}) nie skanuja {@code @Configuration},
 * wiec nie ma w nich {@link Clock} z BeanyConfig (lekcja 2). Tym go dokladamy.
 */
@TestConfiguration(proxyBeanMethods = false)
public class BeanyConfigDlaTestow {

    @Bean
    Clock zegar() {
        return Clock.systemUTC();
    }
}
