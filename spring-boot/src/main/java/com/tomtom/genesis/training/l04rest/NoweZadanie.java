package com.tomtom.genesis.training.l04rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lekcja 4 - DTO wejsciowe. Osobny typ niz {@link Zadanie}, bo klient
 * NIE ustawia id, dat ani statusu. Walidacja uruchamia sie dzieki {@code @Valid}
 * w kontrolerze; blad = 400 z ProblemDetail (RFC 9457).
 */
public record NoweZadanie(
        @NotBlank @Size(max = 100) String tytul,
        @Size(max = 1000) String opis,
        @NotNull Priorytet priorytet) {
}
