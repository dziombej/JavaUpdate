package com.tomtom.genesis.training.l03konfiguracja;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/**
 * Lekcja 3 - jak wlasciwosci wiaza sie z {@link KursProperties}.
 * Kazdy test to inna "konfiguracja" podana przez withPropertyValues.
 */
class KonfiguracjaTest {

    private final ApplicationContextRunner kontekst = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(KonfiguracjaConfig.class)
            .withPropertyValues(
                    "szkolenie.kurs.nazwa=Test",
                    "szkolenie.kurs.sala.budynek=A1",
                    "szkolenie.kurs.sala.pietro=1");

    @Test
    void wartosci_domyslne() {
        kontekst.run(ctx -> {
            var kurs = ctx.getBean(KursProperties.class);
            assertThat(kurs.maksUczestnikow()).isEqualTo(12);
            assertThat(kurs.czasLekcji()).isEqualTo(Duration.ofMinutes(45));
            assertThat(kurs.prowadzacy()).isEmpty();
        });
    }

    @Test
    void relaxed_binding_i_konwersja_typow() {
        // maks-uczestnikow, maksUczestnikow, MAKS_UCZESTNIKOW - wszystko trafia w to samo pole
        kontekst.withPropertyValues(
                        "szkolenie.kurs.maksUczestnikow=7",
                        "szkolenie.kurs.czas-lekcji=2h",
                        "szkolenie.kurs.prowadzacy=Ala,Olek,Ewa")
                .run(ctx -> {
                    var kurs = ctx.getBean(KursProperties.class);
                    assertThat(kurs.maksUczestnikow()).isEqualTo(7);
                    assertThat(kurs.czasLekcji()).isEqualTo(Duration.ofHours(2));
                    assertThat(kurs.prowadzacy()).containsExactly("Ala", "Olek", "Ewa");
                });
    }

    @Test
    void zla_konfiguracja_zatrzymuje_start() {
        kontekst.withPropertyValues("szkolenie.kurs.maks-uczestnikow=500")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(BindValidationException.class)
                        .hasMessageContaining("maksUczestnikow"));
    }

    @Test
    void walidacja_zagniezdzonego_obiektu() {
        kontekst.withPropertyValues("szkolenie.kurs.sala.budynek=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
