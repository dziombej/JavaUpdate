package com.tomtom.genesis.training.l03konfiguracja;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Lekcja 3 - rejestruje {@link KursProperties} jako bean.
 *
 * Alternatywa: {@code @ConfigurationPropertiesScan} na klasie glownej.
 * Tu celowo jest osobna klasa {@code @Configuration}, bo testy slice
 * jej nie wciagaja (zobacz komentarz w TrainingApplication).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KursProperties.class)
class KonfiguracjaConfig {
}
