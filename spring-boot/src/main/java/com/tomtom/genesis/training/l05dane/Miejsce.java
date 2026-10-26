package com.tomtom.genesis.training.l05dane;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

/**
 * Lekcja 5 - encja JPA: punkt POI na mapie.
 *
 * Encja NIE moze byc rekordem: Hibernate potrzebuje konstruktora bez
 * argumentow, mutowalnych pol (dirty checking) i proxy (lazy loading).
 * Dlatego na zewnatrz API wystawiamy {@link MiejsceDto}, a nie encje.
 *
 * Tabela "miejsce" powstaje sama (spring.jpa.hibernate.ddl-auto=create-drop
 * dla bazy wbudowanej), a dane startowe laduje src/main/resources/data.sql.
 */
@Entity
public class Miejsce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nazwa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kategoria kategoria;

    @Column(nullable = false, length = 60)
    private String miasto;

    private double szerokosc;
    private double dlugosc;

    @Column(nullable = false, updatable = false)
    private Instant utworzono;

    protected Miejsce() {
        // dla JPA
    }

    public Miejsce(String nazwa, Kategoria kategoria, String miasto, double szerokosc, double dlugosc) {
        this.nazwa = nazwa;
        this.kategoria = kategoria;
        this.miasto = miasto;
        this.szerokosc = szerokosc;
        this.dlugosc = dlugosc;
    }

    @PrePersist
    void przedZapisem() {
        if (utworzono == null) {
            utworzono = Instant.now();
        }
    }

    public void zmienNazwe(String nowaNazwa) {
        this.nazwa = nowaNazwa;
    }

    public Long getId() {
        return id;
    }

    public String getNazwa() {
        return nazwa;
    }

    public Kategoria getKategoria() {
        return kategoria;
    }

    public String getMiasto() {
        return miasto;
    }

    public double getSzerokosc() {
        return szerokosc;
    }

    public double getDlugosc() {
        return dlugosc;
    }

    public Instant getUtworzono() {
        return utworzono;
    }
}
