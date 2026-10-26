package com.tomtom.genesis.training.l02beany;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Lekcja 2 - kontener Springa "na sucho".
 *
 * {@link ApplicationContextRunner} buduje maly kontekst tylko z tego, co
 * mu podasz - bez serwera WWW, bazy i reszty aplikacji. Tak Boot testuje
 * wlasna autokonfiguracje. Idealne do eksperymentow z beanami.
 */
class BeanyKontekstTest {

    @Configuration
    @ComponentScan(basePackageClasses = Powitanie.class)
    static class TylkoLekcja2 {
    }

    private final ApplicationContextRunner kontekst = new ApplicationContextRunner()
            .withUserConfiguration(TylkoLekcja2.class);

    @Test
    void primary_wygrywa_gdy_jest_wiele_implementacji() {
        kontekst.run(ctx -> {
            assertThat(ctx).getBeans(Powitanie.class).hasSize(2);
            assertThat(ctx.getBean(Powitanie.class)).isInstanceOf(PolskiePowitanie.class);
            assertThat(ctx.getBean("angielskie")).isInstanceOf(AngielskiePowitanie.class);
        });
    }

    @Test
    void profil_dodaje_beana() {
        kontekst.withPropertyValues("spring.profiles.active=pirat").run(ctx -> {
            assertThat(ctx).hasSingleBean(PirackiePowitanie.class);
            assertThat(ctx.getBean(PowitaniaService.class).powitajWszystkimi("Jack"))
                    .containsKeys("pl", "en", "pirat")
                    .containsEntry("pirat", "Ahoj, Jack, szczurze ladowy!");
        });
    }

    @Test
    void bez_profilu_pirata_nie_ma() {
        kontekst.run(ctx -> assertThat(ctx).doesNotHaveBean(PirackiePowitanie.class));
    }

    @Test
    void singleton_vs_prototype() {
        kontekst.run(ctx -> {
            // singleton: dwa pobrania = ten sam obiekt
            assertThat(ctx.getBean(PowitaniaService.class)).isSameAs(ctx.getBean(PowitaniaService.class));
            // prototype: kazde pobranie = nowy obiekt, wiec liczniki sa niezalezne
            var pierwszy = ctx.getBean(LicznikZadan.class);
            var drugi = ctx.getBean(LicznikZadan.class);
            assertThat(pierwszy).isNotSameAs(drugi);
            pierwszy.nastepny();
            assertThat(drugi.nastepny()).isEqualTo(1);
        });
    }
}
