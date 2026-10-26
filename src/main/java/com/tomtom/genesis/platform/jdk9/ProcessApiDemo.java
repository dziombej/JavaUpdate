package com.tomtom.genesis.platform.jdk9;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Java 9 (JEP 102) - Process API.
 *
 * Przed Java 9 nie dalo sie w standardowym API pobrac PID wlasnego procesu,
 * wylistowac procesow systemu ani dostac powiadomienia o zakonczeniu procesu
 * potomnego bez blokujacego waitFor(). ProcessHandle to zalatwia.
 */
public class ProcessApiDemo {

    public static void main(String[] args) throws Exception {
        wlasnyProces();
        procesyWSystemie();
        procesPotomnyIOnExit();
    }

    private static void wlasnyProces() {
        naglowek("Informacje o wlasnym procesie");

        ProcessHandle ja = ProcessHandle.current();
        System.out.println("PID              = " + ja.pid());

        ProcessHandle.Info info = ja.info();
        // Wszystkie pola Info sa Optional - system operacyjny nie musi ich udostepniac.
        System.out.println("polecenie        = " + info.command().orElse("(nieznane)"));
        System.out.println("uzytkownik       = " + info.user().orElse("(nieznany)"));
        System.out.println("start            = " + info.startInstant().map(Object::toString).orElse("(nieznany)"));
        System.out.println("czas CPU         = " + info.totalCpuDuration()
                .map(Duration::toMillis).map(ms -> ms + " ms").orElse("(nieznany)"));

        System.out.println();
        System.out.println("Przed Java 9 PID pobieralo sie hackiem przez refleksje na");
        System.out.println("java.lang.UNIXProcess albo przez parsowanie ManagementFactory");
        System.out.println("  .getRuntimeMXBean().getName()  -> \"12345@hostname\".");
    }

    private static void procesyWSystemie() {
        naglowek("Przegladanie procesow systemu");

        long ile = ProcessHandle.allProcesses().count();
        System.out.println("liczba widocznych procesow = " + ile);

        String kilka = ProcessHandle.allProcesses()
                .limit(5)
                .map(ph -> ph.pid() + " " + ph.info().command().orElse("(nieznane)"))
                .collect(Collectors.joining("\n  "));
        System.out.println("pierwszych 5:");
        System.out.println("  " + kilka);

        // Drzewo procesow: parent(), children(), descendants()
        System.out.println("rodzic tego procesu = " + ProcessHandle.current().parent()
                .map(ph -> ph.pid() + " " + ph.info().command().orElse(""))
                .orElse("(brak / niewidoczny)"));
    }

    private static void procesPotomnyIOnExit() throws IOException, InterruptedException, ExecutionException {
        naglowek("Proces potomny i onExit() - reakcja bez blokowania");

        Process proces = new ProcessBuilder("sh", "-c", "sleep 0.3; echo 'potomek skonczyl'")
                .redirectErrorStream(true)
                .start();

        System.out.println("uruchomiono potomka, PID = " + proces.pid());

        // onExit() zwraca CompletableFuture<Process> - mozna podpiac reakcje
        // zamiast blokowac watek na waitFor().
        CompletableFuture<String> poZakonczeniu = proces.onExit()
                .thenApply(p -> "potomek " + p.pid() + " zakonczyl sie kodem " + p.exitValue());

        System.out.println("watek glowny nie jest zablokowany, robie cos innego...");

        // Java 8 dodalo waitFor(timeout, unit); tu czekamy tylko po to, zeby demo bylo deterministyczne.
        boolean zdazyl = proces.waitFor(5, TimeUnit.SECONDS);
        System.out.println("waitFor z timeoutem zwrocil = " + zdazyl);
        System.out.println(poZakonczeniu.get());

        String wyjscie = new String(proces.getInputStream().readAllBytes()).trim();
        System.out.println("stdout potomka = " + wyjscie + "   (readAllBytes - tez Java 9)");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
