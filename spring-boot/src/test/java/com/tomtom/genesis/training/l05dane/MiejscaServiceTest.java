package com.tomtom.genesis.training.l05dane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Lekcja 5 - serwis z prawdziwym JPA. {@code @Import} doklada serwis do slice'a.
 */
@DataJpaTest
@Import(MiejscaService.class)
@RecordApplicationEvents
class MiejscaServiceTest {

    @Autowired
    MiejscaService serwis;

    @Autowired
    TestEntityManager em;

    @Autowired
    ApplicationEvents zdarzenia;

    @Test
    void dirty_checking_zapisuje_zmiane_bez_save() {
        var id = em.persistAndFlush(new Miejsce("Stara nazwa", Kategoria.HOTEL, "Opole", 50.6, 17.9)).getId();
        em.clear();

        serwis.zmienNazwe(id, "Nowa nazwa");
        em.flush();   // wymus UPDATE teraz (normalnie przy commicie)
        em.clear();   // zapomnij cache, czytaj z bazy

        assertThat(em.find(Miejsce.class, id).getNazwa()).isEqualTo("Nowa nazwa");
    }

    @Test
    void utworzenie_publikuje_zdarzenie() {
        var dto = serwis.utworz(new NoweMiejsce("Muzeum Wsi", Kategoria.MUZEUM, "Kielce", 50.8, 20.6));

        assertThat(zdarzenia.stream(MiejsceUtworzone.class))
                .containsExactly(new MiejsceUtworzone(dto.id(), Kategoria.MUZEUM));
    }

    @Test
    void brak_miejsca_to_wyjatek_z_404() {
        assertThatThrownBy(() -> serwis.pobierz(-1))
                .isInstanceOf(MiejsceNieZnalezioneException.class)
                .hasMessageContaining("404");
    }
}
