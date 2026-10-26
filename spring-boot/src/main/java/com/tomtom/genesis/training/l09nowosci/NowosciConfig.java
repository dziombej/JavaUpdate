package com.tomtom.genesis.training.l09nowosci;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Lekcja 9 - wlacza {@code @Retryable} i {@code @ConcurrencyLimit}.
 * W Spring Framework 7 to czesc rdzenia - nie trzeba juz spring-retry.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
class NowosciConfig {
}
