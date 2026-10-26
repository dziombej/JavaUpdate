package com.tomtom.genesis.training.l05dane;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lekcja 5 - warstwa serwisu i transakcje.
 *
 * {@code @Transactional(readOnly = true)} na klasie: kazda metoda dziala
 * w transakcji tylko do odczytu (Hibernate pomija dirty checking).
 * Metody zapisujace nadpisuja to swoim {@code @Transactional}.
 *
 * Transakcja dziala przez PROXY: wywolanie metody z wnetrza tej samej klasy
 * (this.cos()) omija proxy i adnotacje. To najczestsza pulapka.
 */
@Service
@Transactional(readOnly = true)
public class MiejscaService {

    private final MiejsceRepository repozytorium;
    private final ApplicationEventPublisher zdarzenia;

    MiejscaService(MiejsceRepository repozytorium, ApplicationEventPublisher zdarzenia) {
        this.repozytorium = repozytorium;
        this.zdarzenia = zdarzenia;
    }

    public Page<MiejsceDto> szukaj(String fragment, Pageable stronicowanie) {
        return repozytorium.findByNazwaContainingIgnoreCase(fragment, stronicowanie).map(MiejsceDto::z);
    }

    public MiejsceDto pobierz(long id) {
        return repozytorium.findById(id).map(MiejsceDto::z)
                .orElseThrow(() -> new MiejsceNieZnalezioneException(id));
    }

    public List<MiejsceDto> wKategorii(Kategoria kategoria) {
        return repozytorium.findByKategoria(kategoria).stream().map(MiejsceDto::z).toList();
    }

    public List<MiejsceDto> wMiescie(String miasto) {
        return repozytorium.findByMiastoIgnoreCaseOrderByNazwaAsc(miasto).stream().map(MiejsceDto::z).toList();
    }

    public List<LicznikKategorii> statystyki() {
        return repozytorium.policzWgKategorii();
    }

    @Transactional
    public MiejsceDto utworz(NoweMiejsce nowe) {
        var zapisane = repozytorium.save(new Miejsce(nowe.nazwa(), nowe.kategoria(), nowe.miasto(),
                nowe.szerokosc(), nowe.dlugosc()));
        zdarzenia.publishEvent(new MiejsceUtworzone(zapisane.getId(), zapisane.getKategoria()));
        return MiejsceDto.z(zapisane);
    }

    /**
     * Zwroc uwage: NIE ma tu repozytorium.save(...). Encja pobrana w transakcji
     * jest "zarzadzana" - Hibernate sam wykryje zmiane i zrobi UPDATE przy commicie.
     */
    @Transactional
    public MiejsceDto zmienNazwe(long id, String nowaNazwa) {
        var miejsce = repozytorium.findById(id).orElseThrow(() -> new MiejsceNieZnalezioneException(id));
        miejsce.zmienNazwe(nowaNazwa);
        return MiejsceDto.z(miejsce);
    }

    @Transactional
    public void usun(long id) {
        if (!repozytorium.existsById(id)) {
            throw new MiejsceNieZnalezioneException(id);
        }
        repozytorium.deleteById(id);
    }
}
