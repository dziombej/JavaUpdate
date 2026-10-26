# Lekcja 9 — Nowosci Spring Boot 4 / Spring Framework 7

**Kod:** [`l09nowosci`](../src/main/java/com/tomtom/genesis/training/l09nowosci)
**Testy:** [`NowosciTest`](../src/test/java/com/tomtom/genesis/training/l09nowosci/NowosciTest.java)

Boot 4.0 wyszedl w listopadzie 2025 razem ze Spring Framework 7. Ten projekt
uzywa 4.1.2. Wymagania: **Java 17+** (zalecana 25), Jakarta EE 11
(Servlet 6.1), Kotlin 2.2, Hibernate 7, Jackson 3. Ponizej to, co zmienia
codzienna prace. Czesc juz widziales w poprzednich lekcjach.

## 1. Wersjonowanie API (Framework 7)

Do tej pory kazdy zespol wersjonowal po swojemu. Teraz jest to wbudowane:

```yaml
spring.mvc.apiversion:
  use.header: API-Version     # albo: use.query-parameter, use.path-segment, use.media-type-parameter
  default: 1.0
  supported: 1.0, 2.0, 3.0
```

```java
@GetMapping(path = "/api/nowosci/powitanie", version = "1.0")
String powitanieV1(...)

@GetMapping(path = "/api/nowosci/powitanie", version = "2.0+")   // 2.0 i nowsze
PowitanieV2 powitanieV2(...)
```

```bash
curl localhost:8080/api/nowosci/powitanie                          # Czesc, Swiecie  (default 1.0)
curl -H 'API-Version: 2.0' localhost:8080/api/nowosci/powitanie    # {"zwrot":"Czesc",...}
curl -H 'API-Version: 3.0' localhost:8080/api/nowosci/powitanie    # tez v2, bo "2.0+"
curl -H 'API-Version: 9.9' localhost:8080/api/nowosci/powitanie    # 400 Invalid API version
```

Mapowania bez `version` (cala reszta aplikacji) dzialaja dla kazdej wersji.
Klienci tez to znaja: `RestClient.builder().apiVersionInserter(...)`,
a w `@HttpExchange` atrybut `version`.

## 2. Resilience w rdzeniu: `@Retryable`, `@ConcurrencyLimit`

Wczesniej potrzebny byl osobny projekt `spring-retry` albo Resilience4j.
Teraz podstawy sa w `spring-context` - wystarczy `@EnableResilientMethods`
([`NowosciConfig`](../src/main/java/com/tomtom/genesis/training/l09nowosci/NowosciConfig.java)).

```java
@Retryable(includes = ChwilowaAwaria.class, maxRetries = 3, delay = 100, multiplier = 2)
public int pobierzDane() { ... }      // 1. proba, potem po 100, 200, 400 ms

@ConcurrencyLimit(2)
public void wolnaOperacja() { ... }   // naraz najwyzej 2 watki
```

```bash
curl localhost:8080/api/nowosci/niestabilna     # {"udanaProbaNr":3} - w logu widac proby 1, 2, 3
```

Jest tez programowe API: `RetryTemplate` i `RetryPolicy` w `org.springframework.core.retry`.
Circuit breaker nadal wymaga Resilience4j.

## 3. Watki wirtualne

```yaml
spring.threads.virtual.enabled: true
```

Jedna linia i Tomcat obsluguje kazde zadanie w watku wirtualnym (Java 21+,
w Javie 24+ bez przypinania na `synchronized`). Dotyczy tez `@Async`,
`@Scheduled` i listenerow. Patrz czesc o Javie 21 tego repozytorium.

```bash
curl localhost:8080/api/nowosci/watek
# {"watek":"VirtualThread[#108,tomcat-handler-25]/runnable@ForkJoinPool-1-worker-2","wirtualny":true}
```

## 4. Null-safety: JSpecify

Cale API Springa 7 jest oznaczone adnotacjami JSpecify (`@NullMarked`,
`@Nullable`) zamiast dawnych `org.springframework.lang.*`. IntelliJ i NullAway
pokazuja bledy zwiazane z `null` juz w edytorze. U nas:
[`package-info.java`](../src/main/java/com/tomtom/genesis/training/l09nowosci/package-info.java)
z `@NullMarked` i `@Nullable String jezyk` w `NowosciController`.

## 5. Jackson 3

Nowy pakiet `tools.jackson` (adnotacje zostaly w `com.fasterxml.jackson.annotation`).
Glowna klasa to niemutowalny `JsonMapper` (zamiast `ObjectMapper` konfigurowanego
setterami). Zmienione domyslne ustawienia, m.in. daty jako ISO-8601, a nie liczby.
Raport z lekcji 1 pokazuje `tools.jackson.databind.json.JsonMapper`.
Na czas migracji: `spring.jackson.use-jackson2-defaults=true`
i modul `spring-boot-jackson2`.

## 6. Modularny Boot

Kazda autokonfiguracja ma wlasny modul i starter: `spring-boot-starter-webmvc`,
`-restclient`, `-data-jpa`, `-flyway`... i odpowiednik testowy (`-webmvc-test`).
JAR-y sa mniejsze, a IDE nie podpowiada klas technologii, ktorych nie uzywasz.
Pakiety tez sie zmienily, np. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`,
`org.springframework.boot.health.contributor.HealthIndicator`.
Migracja: przejsciowo `spring-boot-starter-classic` przywraca stary uklad.

## 7. Testy

`RestTestClient` (zamiast `TestRestTemplate`), `MockMvcTester`, `@MockitoBean`
(`@MockBean` usuniete). Wszystko w lekcji 6.

## 8. Inne, wspomniane tylko z nazwy

- `@ImportHttpServices` i grupy klientow - lekcja 8;
- `BeanRegistrar` - programowa rejestracja beanow, przyjazna AOT;
- JUnit 6, Hibernate 7, Jakarta Persistence 3.2;
- usuniete: Undertow, `spring-boot-starter-web` jako jedyna nazwa (zostal jako alias), wsparcie dla Jackson 2 jako domyslnego.

## Eksperymenty

1. **Wersja w sciezce.** Zmien `use.header` na `use.path-segment: 1` i mapowania
   na `/api/{version}/nowosci/powitanie`. Wywolaj `/api/v2/...` i `/api/2.0/...`.
2. **Wersja wymagana.** Usun `default` i ustaw `required: true`. Co zwraca
   `curl localhost:8080/api/nowosci/powitanie` bez naglowka? A `/api/zadania`?
3. **Deprecation.** Oznacz wersje 1.0 jako przestarzala: bean
   `ApiVersionDeprecationHandler` (`StandardApiVersionDeprecationHandler`)
   dodaje naglowki `Deprecation` i `Sunset`. Sprawdz `curl -i`.
4. **Retry - koniec cierpliwosci.** Zmien `maxRetries` na `1`. Co zwraca
   endpoint? A z `includes = IllegalArgumentException.class`?
5. **Concurrency.** Zmien `@ConcurrencyLimit(2)` na `(5)` i popraw asercje w
   `NowosciTest`. Usun adnotacje - ile wynosi `maksRownolegle`?
6. **Watki platformowe.** Ustaw `spring.threads.virtual.enabled: false`
   i wywolaj `/api/nowosci/watek`.
7. **`@Retryable` przez `this`.** Dodaj w `NiestabilnaUsluga` metode, ktora
   wola `this.pobierzDane()`, i wystaw ja w kontrolerze. Czy ponawianie
   dziala? (to samo proxy co przy `@Transactional`, lekcja 5)
8. **Null-safety w IDE.** W `NowosciController` usun `@Nullable` przy `jezyk`
   i zobacz ostrzezenie IntelliJ na `jezyk == null`.

Koniec czesci I. Plan kolejnych czesci: [README](../README.md#co-dalej-kolejne-czesci).
