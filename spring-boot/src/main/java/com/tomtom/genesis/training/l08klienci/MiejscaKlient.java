package com.tomtom.genesis.training.l08klienci;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.tomtom.genesis.training.l05dane.LicznikKategorii;
import com.tomtom.genesis.training.l05dane.MiejsceDto;
import com.tomtom.genesis.training.l05dane.NoweMiejsce;

/**
 * Lekcja 8 - deklaratywny klient HTTP (HTTP Interface).
 *
 * Sam interfejs, bez implementacji - Spring generuje proxy, ktore pod spodem
 * uzywa RestClient. Odpowiednik Feigna, ale wbudowany w Spring Framework.
 * Rejestracja i adres bazowy: {@link KlienciConfig} + application.yml.
 */
@HttpExchange("/api/miejsca")
public interface MiejscaKlient {

    @GetExchange("/{id}")
    MiejsceDto pobierz(@PathVariable long id);

    @GetExchange("/statystyki")
    List<LicznikKategorii> statystyki();

    @PostExchange
    MiejsceDto utworz(@RequestBody NoweMiejsce nowe);
}
