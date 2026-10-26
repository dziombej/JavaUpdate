package com.tomtom.genesis.training.l05dane;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Lekcja 5 - repozytorium Spring Data. Zero implementacji!
 *
 * Z {@link JpaRepository} dostajesz: save, findById, findAll (tez z
 * sortowaniem i stronicowaniem), count, deleteById, existsById...
 *
 * Metody ponizej Spring Data generuje Z NAZWY ("derived queries").
 * Literowka w nazwie pola = blad przy starcie, nie w runtime.
 */
public interface MiejsceRepository extends JpaRepository<Miejsce, Long> {

    // SELECT ... WHERE kategoria = ?
    List<Miejsce> findByKategoria(Kategoria kategoria);

    // SELECT ... WHERE upper(miasto) = upper(?) ORDER BY nazwa
    List<Miejsce> findByMiastoIgnoreCaseOrderByNazwaAsc(String miasto);

    // SELECT ... WHERE upper(nazwa) LIKE upper('%?%') + LIMIT/OFFSET + osobny COUNT
    Page<Miejsce> findByNazwaContainingIgnoreCase(String fragment, Pageable stronicowanie);

    long countByKategoria(Kategoria kategoria);

    // Gdy nazwa metody robi sie za dluga - JPQL wprost.
    @Query("""
            select new com.tomtom.genesis.training.l05dane.LicznikKategorii(m.kategoria, count(m))
            from Miejsce m
            group by m.kategoria
            order by count(m) desc, m.kategoria
            """)
    List<LicznikKategorii> policzWgKategorii();
}
