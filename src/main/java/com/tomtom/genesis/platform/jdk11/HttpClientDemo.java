package com.tomtom.genesis.platform.jdk11;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Java 11 (JEP 321) - java.net.http.HttpClient jako standardowe API.
 *
 * Historia: API pojawilo sie jako inkubator w Javie 9 (jdk.incubator.http),
 * w Javie 10 nadal inkubowalo, a w Javie 11 zostalo ustandaryzowane
 * i przeniesione do pakietu java.net.http w module java.net.http.
 *
 * Zeby demo bylo powtarzalne i nie wymagalo internetu, uruchamiamy maly
 * serwer HTTP w tym samym procesie (com.sun.net.httpserver z modulu jdk.httpserver).
 */
public class HttpClientDemo {

    public static void main(String[] args) throws Exception {
        HttpServer serwer = uruchomLokalnySerwer();
        String baza = "http://localhost:" + serwer.getAddress().getPort();
        try {
            dlaczegoNieHttpUrlConnection();
            zapytanieSynchroniczne(baza);
            zapytanieAsynchroniczne(baza);
            postZTrescia(baza);
            rownolegleZapytania(baza);
            konfiguracja(baza);
        } finally {
            serwer.stop(0);
            System.out.println();
            System.out.println("(lokalny serwer zatrzymany)");
        }
    }

    // ------------------------------------------------------------------
    // Lokalny serwer testowy
    // ------------------------------------------------------------------

    private static HttpServer uruchomLokalnySerwer() throws IOException {
        HttpServer serwer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        serwer.createContext("/hello", wymiana -> odpowiedz(wymiana, 200, "Witaj z lokalnego serwera"));

        serwer.createContext("/echo", wymiana -> {
            String cialo = new String(wymiana.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            odpowiedz(wymiana, 200, "metoda=" + wymiana.getRequestMethod()
                    + " ct=" + wymiana.getRequestHeaders().getFirst("Content-Type")
                    + " cialo=" + cialo);
        });

        serwer.createContext("/wolno", wymiana -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            odpowiedz(wymiana, 200, "odpowiedz po 300 ms");
        });

        serwer.createContext("/blad", wymiana -> odpowiedz(wymiana, 503, "Service Unavailable"));

        serwer.start();
        System.out.println("lokalny serwer HTTP na porcie " + serwer.getAddress().getPort());
        return serwer;
    }

    private static void odpowiedz(HttpExchange wymiana, int status, String tresc) throws IOException {
        byte[] bajty = tresc.getBytes(StandardCharsets.UTF_8);
        wymiana.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        wymiana.sendResponseHeaders(status, bajty.length);
        wymiana.getResponseBody().write(bajty);
        wymiana.close();
    }

    // ------------------------------------------------------------------
    // Wlasciwe demo
    // ------------------------------------------------------------------

    private static void dlaczegoNieHttpUrlConnection() {
        naglowek("Dlaczego powstal nowy klient");

        System.out.println("HttpURLConnection pochodzi z Javy 1.1 (1997) i ma wady nie do naprawienia:");
        System.out.println("  - projektowany pod protokoly, ktorych juz nie ma (gopher, ftp),");
        System.out.println("  - tylko tryb blokujacy, jeden watek na zapytanie,");
        System.out.println("  - brak HTTP/2 i WebSocket,");
        System.out.println("  - trudne API: konfiguracja przez settery, tresc przez strumienie,");
        System.out.println("    a kody bledow przez getErrorStream zamiast getInputStream.");
        System.out.println();
        System.out.println("Efekt: kazdy projekt dokladal Apache HttpClient albo OkHttp. Od Javy 11");
        System.out.println("dla prostych przypadkow ta zaleznosc nie jest juz potrzebna.");
    }

    private static void zapytanieSynchroniczne(String baza) throws IOException, InterruptedException {
        naglowek("Zapytanie synchroniczne (send)");

        HttpClient klient = HttpClient.newHttpClient();

        HttpRequest zapytanie = HttpRequest.newBuilder()
                .uri(URI.create(baza + "/hello"))
                .GET()
                .build();

        HttpResponse<String> odpowiedz = klient.send(zapytanie, HttpResponse.BodyHandlers.ofString());

        System.out.println("status      = " + odpowiedz.statusCode());
        System.out.println("wersja HTTP = " + odpowiedz.version());
        System.out.println("Content-Type= " + odpowiedz.headers().firstValue("Content-Type").orElse("(brak)"));
        System.out.println("tresc       = " + odpowiedz.body());

        System.out.println();
        System.out.println("Trzy filary API:");
        System.out.println("  HttpClient  - dlugozyjacy, thread-safe, trzyma pule polaczen. Twórz JEDEN na aplikacje.");
        System.out.println("  HttpRequest - niezmienny, budowany builderem.");
        System.out.println("  HttpResponse<T> - typ T zalezy od BodyHandlera.");
        System.out.println();
        System.out.println("BodyHandlers.ofString() / ofByteArray() / ofLines() / ofFile(Path) / discarding()");
        System.out.println("- decyduja, jak tresc odpowiedzi zamienia sie na obiekt Javy.");

        // Kod bledu NIE jest wyjatkiem - w odroznieniu od niektorych bibliotek.
        HttpResponse<String> blad = klient.send(
                HttpRequest.newBuilder(URI.create(baza + "/blad")).build(),
                HttpResponse.BodyHandlers.ofString());
        System.out.println();
        System.out.println("odpowiedz 503: status = " + blad.statusCode() + ", tresc = " + blad.body());
        System.out.println("HttpClient NIE rzuca wyjatku dla 4xx/5xx - status trzeba sprawdzic samemu.");
    }

    private static void zapytanieAsynchroniczne(String baza) throws Exception {
        naglowek("Zapytanie asynchroniczne (sendAsync)");

        HttpClient klient = HttpClient.newHttpClient();

        CompletableFuture<String> przyszla = klient
                .sendAsync(HttpRequest.newBuilder(URI.create(baza + "/wolno")).build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(String::toUpperCase);

        System.out.println("zapytanie wyslane, watek glowny nie czeka");
        System.out.println("robie cos innego...");
        System.out.println("wynik (po join) = " + przyszla.join());

        System.out.println();
        System.out.println("sendAsync zwraca CompletableFuture, wiec wpina sie w cale API z Javy 8:");
        System.out.println("thenApply, thenCompose, exceptionally, allOf, orTimeout (Java 9)...");

        // Obsluga bledu w stylu funkcyjnym:
        String zFallbackiem = klient
                .sendAsync(HttpRequest.newBuilder(URI.create("http://localhost:1/nie-istnieje")).build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                // UWAGA: exceptionally dostaje wyjatek OPAKOWANY w CompletionException.
                // Zeby zobaczyc prawdziwa przyczyne, trzeba siegnac po getCause().
                .exceptionally(e -> "fallback po bledzie: " + e.getClass().getSimpleName()
                        + ", przyczyna = " + e.getCause().getClass().getSimpleName())
                .join();
        System.out.println();
        System.out.println(zFallbackiem);
        System.out.println("To czesta pulapka: w exceptionally/handle nigdy nie dostajesz");
        System.out.println("wyjatku wprost - zawsze jest zawiniety w CompletionException.");
    }

    private static void postZTrescia(String baza) throws IOException, InterruptedException {
        naglowek("POST z trescia");

        HttpClient klient = HttpClient.newHttpClient();

        HttpRequest zapytanie = HttpRequest.newBuilder()
                .uri(URI.create(baza + "/echo"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"imie\":\"Ala\"}"))
                .build();

        HttpResponse<String> odpowiedz = klient.send(zapytanie, HttpResponse.BodyHandlers.ofString());
        System.out.println("odpowiedz serwera = " + odpowiedz.body());

        System.out.println();
        System.out.println("BodyPublishers.ofString() / ofByteArray() / ofFile(Path) / noBody()");
        System.out.println("- decyduja, skad brana jest tresc zapytania.");
        System.out.println("Metody: GET(), POST(bp), PUT(bp), DELETE(), method(\"PATCH\", bp).");
    }

    private static void rownolegleZapytania(String baza) {
        naglowek("Kilka zapytan rownolegle");

        HttpClient klient = HttpClient.newHttpClient();
        List<String> sciezki = List.of("/hello", "/wolno", "/echo");

        long start = System.currentTimeMillis();

        List<CompletableFuture<String>> przyszle = sciezki.stream()
                .map(s -> klient.sendAsync(
                                HttpRequest.newBuilder(URI.create(baza + s)).build(),
                                HttpResponse.BodyHandlers.ofString())
                        .thenApply(r -> s + " -> " + r.statusCode()))
                .collect(Collectors.toList());

        // allOf czeka na wszystkie; potem zbieramy wyniki (join juz nie blokuje).
        CompletableFuture.allOf(przyszle.toArray(new CompletableFuture[0])).join();

        przyszle.forEach(f -> System.out.println("  " + f.join()));
        System.out.println("laczny czas = " + (System.currentTimeMillis() - start)
                + " ms  (a /wolno samo trwa 300 ms - wiec poszly rownolegle)");
    }

    private static void konfiguracja(String baza) throws IOException, InterruptedException {
        naglowek("Konfiguracja klienta i zapytania");

        HttpClient klient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)          // domyslnie HTTP_2, z fallbackiem do 1.1
                .followRedirects(HttpClient.Redirect.NORMAL) // domyslnie NEVER!
                .connectTimeout(Duration.ofSeconds(5))       // timeout NAWIAZANIA polaczenia
                .build();

        HttpRequest zapytanie = HttpRequest.newBuilder(URI.create(baza + "/hello"))
                .timeout(Duration.ofSeconds(2))              // timeout CALEGO zapytania
                .header("Accept", "text/plain")
                .header("User-Agent", "JavaUpdate/1.0")
                .build();

        HttpResponse<String> odpowiedz = klient.send(zapytanie, HttpResponse.BodyHandlers.ofString());
        System.out.println("status = " + odpowiedz.statusCode() + ", tresc = " + odpowiedz.body());

        System.out.println();
        System.out.println("Dwa timeouty, ktore latwo pomylic:");
        System.out.println("  HttpClient.connectTimeout - czas na nawiazanie polaczenia TCP");
        System.out.println("  HttpRequest.timeout       - czas na cala wymiane; po nim HttpTimeoutException");
        System.out.println();
        System.out.println("Domyslne followRedirects to NEVER - jesli serwer odpowie 301/302,");
        System.out.println("dostaniesz ten status, a nie zawartosc docelowa. To czesta niespodzianka.");
        System.out.println();
        System.out.println("Czego HttpClient nie ma: wbudowanego JSON-a (potrzebny Jackson/Gson),");
        System.out.println("automatycznych ponowien, circuit breakera. Do tego nadal potrzebna biblioteka.");
    }

    private static void naglowek(String tytul) {
        System.out.println();
        System.out.println("=== " + tytul + " ===");
    }
}
