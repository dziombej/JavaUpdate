package com.tomtom.genesis.training.l04rest;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Lekcja 4 - jedno miejsce na tlumaczenie wyjatkow na odpowiedzi HTTP.
 *
 * {@link ProblemDetail} to standard RFC 9457 (application/problem+json):
 * type, title, status, detail, instance + wlasne pola.
 *
 * Dziedziczenie po {@link ResponseEntityExceptionHandler} daje ProblemDetail
 * dla WSZYSTKICH standardowych bledow Spring MVC (zly JSON, 405, 415...).
 * Bez tej klasy zrobilby to Boot - dzieki {@code spring.mvc.problemdetails.enabled=true}
 * - ale wtedy blad walidacji mowi tylko "Invalid request content.", bez pol.
 */
@RestControllerAdvice
class BledyRestHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    ProblemDetail brakZadania(ZadanieNieZnalezioneException wyjatek) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, wyjatek.getMessage());
        problem.setTitle("Zadanie nie istnieje");
        problem.setType(URI.create("https://szkolenie.example/bledy/brak-zadania"));
        problem.setProperty("id", wyjatek.id());
        return problem;
    }

    /** Dokladamy do odpowiedzi mape "pole -> komunikat", zeby klient wiedzial, co poprawic. */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException wyjatek, HttpHeaders naglowki,
            HttpStatusCode status, WebRequest zadanie) {
        Map<String, String> bledy = new TreeMap<>();
        wyjatek.getBindingResult().getFieldErrors()
                .forEach(b -> bledy.put(b.getField(), b.getDefaultMessage()));
        var problem = wyjatek.getBody();
        problem.setTitle("Niepoprawne dane");
        problem.setProperty("bledy", bledy);
        return handleExceptionInternal(wyjatek, problem, naglowki, status, zadanie);
    }
}
