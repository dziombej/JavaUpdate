package com.tomtom.genesis.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lekcja 1 - punkt wejscia calej aplikacji szkoleniowej.
 *
 * {@code @SpringBootApplication} to trzy adnotacje w jednej:
 * <ul>
 *   <li>{@code @SpringBootConfiguration} - ta klasa jest klasa konfiguracyjna;</li>
 *   <li>{@code @EnableAutoConfiguration} - Boot patrzy na classpath i wlasciwosci
 *       i sam tworzy beany (DataSource, serwer WWW, JsonMapper...);</li>
 *   <li>{@code @ComponentScan} - skanuje TEN pakiet i wszystkie podpakiety
 *       w poszukiwaniu {@code @Component}, {@code @Service}, {@code @RestController}...</li>
 * </ul>
 *
 * Celowo NIE ma tu zadnych {@code @EnableXxx}. Kazda lekcja wlacza swoje funkcje
 * we wlasnej klasie {@code @Configuration} - dzieki temu testy "slice"
 * ({@code @WebMvcTest}, {@code @DataJpaTest}) nie wciagaja niepotrzebnych rzeczy.
 * Wiecej w docs/06-testy.md.
 */
@SpringBootApplication
public class TrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}
