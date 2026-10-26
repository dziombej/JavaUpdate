package com.tomtom.genesis.training.l05dane;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Lekcja 5 - test warstwy danych: {@code @DataJpaTest}.
 *
 * Startuje tylko JPA + baze wbudowana. KAZDY test jest w transakcji, ktora
 * na koncu jest wycofywana - testy nie widza nawzajem swoich danych.
 * Na poczatku czyscimy dane z data.sql, zeby test nie zalezal od seeda.
 */
@DataJpaTest
class MiejsceRepositoryTest {

    @Autowired
    MiejsceRepository repozytorium;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void dane() {
        repozytorium.deleteAll();
        em.persist(new Miejsce("Muzeum Kolejnictwa", Kategoria.MUZEUM, "Warszawa", 52.2, 21.0));
        em.persist(new Miejsce("Muzeum Sztuki", Kategoria.MUZEUM, "Lodz", 51.7, 19.4));
        em.persist(new Miejsce("Hotel Centrum", Kategoria.HOTEL, "Warszawa", 52.2, 21.0));
        em.flush();
    }

    @Test
    void derived_query_po_kategorii() {
        assertThat(repozytorium.findByKategoria(Kategoria.MUZEUM))
                .extracting(Miejsce::getNazwa)
                .containsExactlyInAnyOrder("Muzeum Kolejnictwa", "Muzeum Sztuki");
    }

    @Test
    void ignore_case_i_sortowanie() {
        assertThat(repozytorium.findByMiastoIgnoreCaseOrderByNazwaAsc("WARSZAWA"))
                .extracting(Miejsce::getNazwa)
                .containsExactly("Hotel Centrum", "Muzeum Kolejnictwa");
    }

    @Test
    void stronicowanie() {
        var strona = repozytorium.findByNazwaContainingIgnoreCase("muzeum",
                PageRequest.of(0, 1, Sort.by("nazwa")));

        assertThat(strona.getTotalElements()).isEqualTo(2);
        assertThat(strona.getTotalPages()).isEqualTo(2);
        assertThat(strona.getContent()).extracting(Miejsce::getNazwa).containsExactly("Muzeum Kolejnictwa");
    }

    @Test
    void zapytanie_jpql_z_rekordem() {
        assertThat(repozytorium.policzWgKategorii()).containsExactly(
                new LicznikKategorii(Kategoria.MUZEUM, 2),
                new LicznikKategorii(Kategoria.HOTEL, 1));
    }

    @Test
    void prepersist_ustawia_date() {
        var zapisane = repozytorium.save(new Miejsce("Parking", Kategoria.PARKING, "Gdansk", 54.3, 18.6));

        assertThat(zapisane.getId()).isNotNull();
        assertThat(zapisane.getUtworzono()).isNotNull();
    }
}
