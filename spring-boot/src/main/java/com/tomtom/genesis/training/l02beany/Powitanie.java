package com.tomtom.genesis.training.l02beany;

/**
 * Lekcja 2 - kontrakt, pod ktory Spring podstawia rozne implementacje.
 * Kod zalezy od interfejsu; ktora implementacja trafi do konstruktora,
 * decyduje kontener (@Primary, @Qualifier, @Profile).
 */
public interface Powitanie {

    String jezyk();

    String powitaj(String imie);
}
