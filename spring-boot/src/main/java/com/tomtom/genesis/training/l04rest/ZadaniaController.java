package com.tomtom.genesis.training.l04rest;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Lekcja 4 - pelny CRUD w Spring MVC.
 *
 * {@code @RestController} = {@code @Controller} + {@code @ResponseBody}:
 * wartosc zwracana z metody idzie do ciala odpowiedzi jako JSON.
 */
@RestController
@RequestMapping("/api/zadania")
class ZadaniaController {

    private final ZadaniaStore store;

    ZadaniaController(ZadaniaStore store) {
        this.store = store;
    }

    // GET /api/zadania?priorytet=WYSOKI&tylkoOtwarte=true
    @GetMapping
    List<Zadanie> lista(@RequestParam(required = false) Priorytet priorytet,
                        @RequestParam(defaultValue = "false") boolean tylkoOtwarte) {
        return store.wszystkie().stream()
                .filter(z -> priorytet == null || z.priorytet() == priorytet)
                .filter(z -> !tylkoOtwarte || !z.zrobione())
                .toList();
    }

    // Wyjatek nie jest tu lapany - zamienia go na 404 BledyRestHandler.
    @GetMapping("/{id}")
    Zadanie jedno(@PathVariable long id) {
        return store.znajdz(id).orElseThrow(() -> new ZadanieNieZnalezioneException(id));
    }

    // 201 Created + naglowek Location: /api/zadania/{id}
    @PostMapping
    ResponseEntity<Zadanie> utworz(@Valid @RequestBody NoweZadanie nowe) {
        var zadanie = store.dodaj(nowe);
        var adres = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(zadanie.id()).toUri();
        return ResponseEntity.created(adres).body(zadanie);
    }

    @PutMapping("/{id}")
    Zadanie zamien(@PathVariable long id, @Valid @RequestBody NoweZadanie dane) {
        return store.zamien(id, dane).orElseThrow(() -> new ZadanieNieZnalezioneException(id));
    }

    // PATCH /api/zadania/1/zrobione?wartosc=true
    @PatchMapping("/{id}/zrobione")
    Zadanie zrobione(@PathVariable long id, @RequestParam(defaultValue = "true") boolean wartosc) {
        return store.oznaczZrobione(id, wartosc).orElseThrow(() -> new ZadanieNieZnalezioneException(id));
    }

    // 204 No Content
    @DeleteMapping("/{id}")
    ResponseEntity<Void> usun(@PathVariable long id) {
        if (!store.usun(id)) {
            throw new ZadanieNieZnalezioneException(id);
        }
        return ResponseEntity.noContent().build();
    }
}
