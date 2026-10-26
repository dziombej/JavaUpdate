package com.tomtom.genesis.training.l04rest;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

/**
 * Lekcja 4 - magazyn w pamieci, zeby REST dalo sie cwiczyc bez bazy.
 * Prawdziwa baza (JPA) jest w lekcji 5. ConcurrentHashMap, bo kontroler
 * obsluguje wiele zadan HTTP naraz.
 */
@Repository
public class ZadaniaStore {

    private final Map<Long, Zadanie> zadania = new ConcurrentHashMap<>();
    private final AtomicLong sekwencja = new AtomicLong();
    private final Clock zegar;

    public ZadaniaStore(Clock zegar) {
        this.zegar = zegar;
        dodaj(new NoweZadanie("Przeczytac docs/04-rest.md", "Najpierw teoria", Priorytet.WYSOKI));
        dodaj(new NoweZadanie("Wyslac POST przez curl", null, Priorytet.SREDNI));
    }

    public List<Zadanie> wszystkie() {
        return zadania.values().stream()
                .sorted(Comparator.comparingLong(Zadanie::id))
                .toList();
    }

    public Optional<Zadanie> znajdz(long id) {
        return Optional.ofNullable(zadania.get(id));
    }

    public Zadanie dodaj(NoweZadanie nowe) {
        long id = sekwencja.incrementAndGet();
        var zadanie = new Zadanie(id, nowe.tytul(), nowe.opis(), nowe.priorytet(), false, Instant.now(zegar));
        zadania.put(id, zadanie);
        return zadanie;
    }

    public Optional<Zadanie> zamien(long id, NoweZadanie dane) {
        return Optional.ofNullable(zadania.computeIfPresent(id, (_, stare) ->
                new Zadanie(id, dane.tytul(), dane.opis(), dane.priorytet(), stare.zrobione(), stare.utworzono())));
    }

    public Optional<Zadanie> oznaczZrobione(long id, boolean zrobione) {
        return Optional.ofNullable(zadania.computeIfPresent(id, (_, stare) -> stare.zrobione(zrobione)));
    }

    public boolean usun(long id) {
        return zadania.remove(id) != null;
    }

    public long liczba() {
        return zadania.size();
    }
}
