# Lekcja 8 — Klienci HTTP

**Kod:** [`l08klienci`](../src/main/java/com/tomtom/genesis/training/l08klienci)
**Testy:** [`PrzewodnikKlientTest`](../src/test/java/com/tomtom/genesis/training/l08klienci/PrzewodnikKlientTest.java),
[`MiejscaKlientTest`](../src/test/java/com/tomtom/genesis/training/l08klienci/MiejscaKlientTest.java)

Zeby nie zalezec od zewnetrznych serwisow, aplikacja **wola sama siebie** -
klienci rozmawiaja z API miejsc z lekcji 5.

## Ktory klient

| Klient | Styl | Status |
|---|---|---|
| `RestTemplate` | metody `getForObject`, `exchange` | utrzymywany, nowy kod - nie |
| `WebClient` | reaktywny (`Mono`, `Flux`), wymaga WebFluxa | gdy aplikacja jest reaktywna |
| **`RestClient`** | plynny API, blokujacy | **domyslny wybor** (od Spring 6.1) |
| **HTTP Interface** | interfejs z adnotacjami, proxy na `RestClient` | gdy wolasz serwis z wieloma endpointami |

Z watkami wirtualnymi (lekcja 9) blokujacy `RestClient` skaluje sie dobrze,
wiec powod, zeby siegac po `WebClient` w aplikacji MVC, prawie zniknal.

## RestClient

[`PrzewodnikKlient`](../src/main/java/com/tomtom/genesis/training/l08klienci/PrzewodnikKlient.java):

```java
PrzewodnikKlient(RestClient.Builder budowniczy, @Value("${szkolenie.przewodnik.base-url}") String adres) {
    this.klient = budowniczy.baseUrl(adres).build();
}

klient.get().uri("/api/miejsca/{id}", id)
      .retrieve()                       // 4xx/5xx -> wyjatek RestClientResponseException
      .body(MiejsceDto.class);
```

U nas zamiast `retrieve()` jest `exchange(...)`, zeby samemu zdecydowac, co
znaczy 404 (`Optional.empty()`), a co jest bledem. Inna opcja to
`retrieve().onStatus(status -> ..., (req, res) -> { throw ... })`.

**Zawsze buduj z wstrzyknietego `RestClient.Builder`**, nie przez
`RestClient.create()`. Builder od Boota ma: `JsonMapper` z Twoja konfiguracja
Jacksona, metryki `http.client.requests`, propagacje tracingu, timeouty
z `spring.http.clients.*`. `RestClient.create()` nie ma niczego z tego.

## HTTP Interface + `@ImportHttpServices` (nowosc Boot 4)

[`MiejscaKlient`](../src/main/java/com/tomtom/genesis/training/l08klienci/MiejscaKlient.java):

```java
@HttpExchange("/api/miejsca")
public interface MiejscaKlient {
    @GetExchange("/{id}")      MiejsceDto pobierz(@PathVariable long id);
    @GetExchange("/statystyki") List<LicznikKategorii> statystyki();
    @PostExchange              MiejsceDto utworz(@RequestBody NoweMiejsce nowe);
}
```

Rejestracja - [`KlienciConfig`](../src/main/java/com/tomtom/genesis/training/l08klienci/KlienciConfig.java):

```java
@ImportHttpServices(group = "miejsca", types = MiejscaKlient.class)
```

Konfiguracja grupy w `application.yml`:

```yaml
spring.http.serviceclient.miejsca:
  base-url: http://localhost:${server.port:8080}
  read-timeout: 5s
```

W Boot 3 trzeba bylo recznie skladac `HttpServiceProxyFactory` dla kazdego
interfejsu - zobacz `MiejscaKlientTest`, ktory robi to wlasnie tak, zeby
wskazac losowy port serwera testowego. Grupy pozwalaja wielu interfejsom
dzielic jeden skonfigurowany `RestClient` (adres, uwierzytelnienie, timeouty).

## Uruchom

```bash
curl -s localhost:8080/api/klienci/demo/1 | jq
curl -s localhost:8080/api/klienci/demo/999 | jq
curl -s 'localhost:8080/actuator/metrics/http.client.requests' | jq
```

## Testy klientow

- [`PrzewodnikKlientTest`](../src/test/java/com/tomtom/genesis/training/l08klienci/PrzewodnikKlientTest.java) -
  `@RestClientTest` + `MockRestServiceServer`: zadnego serwera, odpowiedzi
  ustawiasz sam. Dziala, bo klient jest budowany z wstrzyknietego Buildera.
- [`MiejscaKlientTest`](../src/test/java/com/tomtom/genesis/training/l08klienci/MiejscaKlientTest.java) -
  prawdziwy serwer na losowym porcie i reczne proxy.

## Eksperymenty

1. **Timeout.** W `MiejscaController.statystyki()` dodaj `Thread.sleep(7000)`
   i wywolaj `/api/klienci/demo/1`. Po 5 s klient zglosi blad - z ktorej
   wlasciwosci wzial sie limit? Zmien `read-timeout` na `10s`.
2. **`retrieve()` zamiast `exchange()`.** Przepisz `PrzewodnikKlient.znajdz`
   na `retrieve().body(...)` i wywolaj demo z `999`. Jaki wyjatek i jaki status
   dostaje klient naszego API? Dodaj `.onStatus(...)`, ktory zamieni 404 na
   `Optional.empty()`.
3. **Naglowek dla calej grupy.** Dodaj bean
   `RestClientHttpServiceGroupConfigurer`, ktory dla grupy `miejsca` dopisze
   naglowek `X-Klient: szkolenie`:
   ```java
   @Bean
   RestClientHttpServiceGroupConfigurer naglowki() {
       return grupy -> grupy.filterByName("miejsca")
               .forEachClient((grupa, builder) -> builder.defaultHeader("X-Klient", "szkolenie"));
   }
   ```
   Sprawdz go, dodajac `@RequestHeader(required = false) String xKlient` do kontrolera.
4. **Nowy endpoint w interfejsie.** Dodaj do `MiejscaKlient`
   `@GetExchange("/kategoria/{kategoria}") List<MiejsceDto> wKategorii(@PathVariable Kategoria kategoria)`
   i test w `MiejscaKlientTest`.
5. **Odpowiedz z naglowkami.** Zmien typ zwracany `pobierz` na
   `ResponseEntity<MiejsceDto>` i wypisz `Content-Type`.
6. **Prawdziwy serwis.** Utworz druga grupe, np. `github` z
   `base-url: https://api.github.com` i interfejs z
   `@GetExchange("/repos/{owner}/{repo}")` zwracajacym `Map<String, Object>`.

## Pulapki

- `RestClient.create()` / `new RestTemplate()` w kodzie - brak metryk,
  timeoutow i tracingu.
- Brak timeoutow = watek (lub watek wirtualny) czeka w nieskonczonosc na wolny serwis.
- Ponawianie (lekcja 9) zapytan, ktore nie sa idempotentne (`POST`).

Dalej: [Lekcja 9 — Nowosci Boot 4 / Framework 7](09-nowosci.md)
