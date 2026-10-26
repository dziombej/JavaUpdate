package com.tomtom.genesis.training.l05dane;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Lekcja 5 - drugi sposob na bledy HTTP (porownaj z lekcja 4).
 *
 * Wyjatek dziedziczy po {@link ErrorResponseException}, wiec SAM niesie status
 * i ProblemDetail. Nie potrzeba {@code @ExceptionHandler} - Spring MVC wie,
 * co z nim zrobic.
 */
public class MiejsceNieZnalezioneException extends ErrorResponseException {

    public MiejsceNieZnalezioneException(long id) {
        super(HttpStatus.NOT_FOUND, problem(id), null);
    }

    private static ProblemDetail problem(long id) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Nie ma miejsca o id " + id);
        problem.setTitle("Miejsce nie istnieje");
        problem.setProperty("id", id);
        return problem;
    }
}
