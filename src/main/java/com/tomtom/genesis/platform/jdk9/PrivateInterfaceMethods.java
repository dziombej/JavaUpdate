package com.tomtom.genesis.platform.jdk9;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Java 9 - prywatne metody w interfejsach (zwykle i statyczne).
 *
 * Kontekst historyczny:
 *   Java 7 - interfejs mial tylko metody abstrakcyjne i stale.
 *   Java 8 - doszly metody default i static (publiczne).
 *   Java 9 - doszly metody private i private static.
 *
 * Problem, ktory to rozwiazuje: w Javie 8 dwie metody default dzielace logike
 * musialy albo ja duplikowac, albo wystawic pomocnicza metode jako publiczne API.
 */
public class PrivateInterfaceMethods {

    interface Walidator {

        List<String> pola();

        // --- publiczne API interfejsu -------------------------------------

        default boolean czyPoprawny() {
            return bledy().isEmpty();
        }

        default List<String> bledy() {
            return pola().stream()
                    .filter(pole -> !czyNiepuste(pole))
                    .map(Walidator::komunikat)
                    .collect(Collectors.toList());
        }

        // --- szczegoly implementacyjne, niewidoczne z zewnatrz -------------

        /**
         * private: widoczna tylko w tym interfejsie, ma dostep do this.
         * Nie trafia do publicznego API, wiec implementacje jej nie dziedzicza
         * i nie moga jej nadpisac.
         */
        private boolean czyNiepuste(String pole) {
            return pole != null && !pole.isBlank();
        }

        /**
         * private static: to samo, ale bez dostepu do this - mozna wolac
         * takze z metod statycznych interfejsu.
         */
        private static String komunikat(String pole) {
            return "puste pole: '" + pole + "'";
        }
    }

    /** Zwykla klasa, a nie rekord - rekordy to Java 16, ten plik pokazuje Jave 9. */
    static final class Formularz implements Walidator {
        private final List<String> pola;

        Formularz(List<String> pola) {
            this.pola = pola;
        }

        @Override
        public List<String> pola() {
            return pola;
        }
    }

    public static void main(String[] args) {
        Walidator ok = new Formularz(List.of("Ala", "Kowalska"));
        Walidator zle = new Formularz(List.of("Ala", "   ", ""));

        System.out.println("ok.czyPoprawny()  = " + ok.czyPoprawny());
        System.out.println("ok.bledy()        = " + ok.bledy());
        System.out.println("zle.czyPoprawny() = " + zle.czyPoprawny());
        System.out.println("zle.bledy()       = " + zle.bledy());

        System.out.println();
        System.out.println("Zauwaz: Formularz implementuje tylko pola(). Cala reszta to metody default,");
        System.out.println("ktore dziela logike przez metody private - bez zasmiecania publicznego API.");
        System.out.println();
        System.out.println("Ograniczenia:");
        System.out.println("  - metody private w interfejsie MUSZA miec cialo (nie moga byc abstrakcyjne),");
        System.out.println("  - nie mozna ich nadpisac w klasie implementujacej,");
        System.out.println("  - nadal nie ma pol instancyjnych - interfejs to nie klasa abstrakcyjna.");
    }
}
