package com.tomtom.genesis.training.l08klienci;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Lekcja 8 - nowosc Spring Framework 7 / Boot 4: {@code @ImportHttpServices}.
 *
 * Rejestruje proxy dla interfejsow w grupie "miejsca". Boot konfiguruje grupe
 * z wlasciwosci {@code spring.http.serviceclient.miejsca.*} (base-url, timeouty...).
 * Wczesniej trzeba bylo recznie skladac HttpServiceProxyFactory - zobacz
 * MiejscaKlientTest, gdzie jest to pokazane.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "miejsca", types = MiejscaKlient.class)
class KlienciConfig {
}
