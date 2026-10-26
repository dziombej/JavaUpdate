package com.tomtom.genesis.training.l03konfiguracja;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Lekcja 3 - typowana konfiguracja zamiast rozsianych {@code @Value}.
 *
 * Wszystko spod prefiksu {@code szkolenie.kurs} w application.yml trafia tutaj.
 * <ul>
 *   <li>rekord = niemutowalna konfiguracja, wiazana przez konstruktor;</li>
 *   <li>{@code @DefaultValue} - wartosc, gdy klucza nie ma w zadnym zrodle;</li>
 *   <li>{@code Duration} z tekstu "45m", "2h", "PT30S" - konwersja za darmo;</li>
 *   <li>{@code @Validated} - zla konfiguracja = aplikacja NIE wystartuje
 *       (lepiej przy starcie niz o 3 w nocy).</li>
 * </ul>
 * Rejestracja: {@link KonfiguracjaConfig}.
 */
@ConfigurationProperties("szkolenie.kurs")
@Validated
public record KursProperties(
        @NotBlank String nazwa,
        @DefaultValue("12") @Min(1) @Max(30) int maksUczestnikow,
        @DefaultValue("45m") Duration czasLekcji,
        @DefaultValue List<String> prowadzacy,
        @Valid Sala sala) {

    public record Sala(@NotBlank String budynek, @Positive int pietro) {
    }
}
