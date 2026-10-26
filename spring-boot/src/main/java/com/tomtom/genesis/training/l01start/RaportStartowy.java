package com.tomtom.genesis.training.l01start;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

/**
 * Lekcja 1 - co Boot zrobil za nas przy starcie.
 *
 * {@link ApplicationRunner} uruchamia sie raz, gdy kontekst jest gotowy,
 * ale zanim aplikacja zglosi "ready". Dobre miejsce na kod startowy.
 *
 * {@code @ConditionalOnProperty} - ten bean powstaje tylko, gdy
 * {@code szkolenie.start.raport=true} (albo wlasciwosci nie ma wcale).
 * Dokladnie tym mechanizmem dziala cala autokonfiguracja Boota.
 */
@Component
@ConditionalOnProperty(name = "szkolenie.start.raport", havingValue = "true", matchIfMissing = true)
class RaportStartowy implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RaportStartowy.class);

    private final ApplicationContext kontekst;
    private final Environment srodowisko;

    // Jeden konstruktor = Spring wstrzykuje zaleznosci bez @Autowired.
    RaportStartowy(ApplicationContext kontekst, Environment srodowisko) {
        this.kontekst = kontekst;
        this.srodowisko = srodowisko;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("""

                ===================== RAPORT STARTOWY (lekcja 1) =====================
                Java            : {}
                Spring Framework: {}
                Spring Boot     : {}
                Aktywne profile : {}
                Liczba beanow   : {}
                Watki wirtualne : {}
                Argumenty       : {}
                --- beany, ktorych NIE napisalismy, a sa (autokonfiguracja) ---
                {}
                =====================================================================
                """,
                Runtime.version(),
                SpringVersion.getVersion(),
                SpringBootVersion.getVersion(),
                Arrays.toString(srodowisko.getActiveProfiles()),
                kontekst.getBeanDefinitionCount(),
                srodowisko.getProperty("spring.threads.virtual.enabled", "false"),
                Arrays.toString(args.getSourceArgs()),
                autokonfigurowaneBeany());
    }

    private String autokonfigurowaneBeany() {
        var typy = List.of(DataSource.class, PlatformTransactionManager.class,
                JsonMapper.class, RestClient.Builder.class);
        var wynik = new StringBuilder();
        for (Class<?> typ : typy) {
            String[] nazwy = kontekst.getBeanNamesForType(typ);
            String opis = nazwy.length == 0
                    ? "(brak)"
                    : kontekst.getBean(nazwy[0]).getClass().getName();
            wynik.append("%-28s -> %s%n".formatted(typ.getSimpleName(), opis));
        }
        return wynik.toString().stripTrailing();
    }
}
