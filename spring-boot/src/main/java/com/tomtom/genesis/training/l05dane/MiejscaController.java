package com.tomtom.genesis.training.l05dane;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lekcja 5 - REST nad baza danych.
 *
 * curl 'localhost:8080/api/miejsca?szukaj=muzeum&page=0&size=3&sort=miasto,desc'
 */
@RestController
@RequestMapping("/api/miejsca")
public class MiejscaController {

    private final MiejscaService miejsca;

    MiejscaController(MiejscaService miejsca) {
        this.miejsca = miejsca;
    }

    // Pageable wypelnia sie sam z ?page=&size=&sort=. PagedModel = stabilny format JSON strony.
    @GetMapping
    PagedModel<MiejsceDto> szukaj(@RequestParam(defaultValue = "") String szukaj,
                                  @PageableDefault(size = 5, sort = "nazwa", direction = Sort.Direction.ASC)
                                  Pageable stronicowanie) {
        return new PagedModel<>(miejsca.szukaj(szukaj, stronicowanie));
    }

    @GetMapping("/{id}")
    MiejsceDto jedno(@PathVariable long id) {
        return miejsca.pobierz(id);
    }

    @GetMapping("/kategoria/{kategoria}")
    List<MiejsceDto> wKategorii(@PathVariable Kategoria kategoria) {
        return miejsca.wKategorii(kategoria);
    }

    @GetMapping("/miasto/{miasto}")
    List<MiejsceDto> wMiescie(@PathVariable String miasto) {
        return miejsca.wMiescie(miasto);
    }

    @GetMapping("/statystyki")
    List<LicznikKategorii> statystyki() {
        return miejsca.statystyki();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MiejsceDto utworz(@Valid @RequestBody NoweMiejsce nowe) {
        return miejsca.utworz(nowe);
    }

    @PutMapping("/{id}/nazwa")
    MiejsceDto zmienNazwe(@PathVariable long id, @Valid @RequestBody ZmianaNazwy zmiana) {
        return miejsca.zmienNazwe(id, zmiana.nazwa());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void usun(@PathVariable long id) {
        miejsca.usun(id);
    }

    record ZmianaNazwy(@NotBlank String nazwa) {
    }
}
