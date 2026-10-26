package com.tomtom.genesis.training.l05dane;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoweMiejsce(
        @NotBlank @Size(max = 100) String nazwa,
        @NotNull Kategoria kategoria,
        @NotBlank @Size(max = 60) String miasto,
        @DecimalMin("-90") @DecimalMax("90") double szerokosc,
        @DecimalMin("-180") @DecimalMax("180") double dlugosc) {
}
