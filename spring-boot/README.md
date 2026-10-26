# Spring Boot 4 — szkolenie praktyczne

Czesc III repozytorium JavaUpdate. Jedna dzialajaca aplikacja, podzielona na
lekcje. Kazda lekcja to **pakiet z kodem**, **testy** i **dokument** z teoria
i cwiczeniami. Najpierw to, czego uzywa sie codziennie; szczegoly i rzeczy
zaawansowane przyjda w kolejnych czesciach.

Stan zweryfikowany na: **Spring Boot 4.1.2, Spring Framework 7.0.10,
Hibernate 7.4, Jackson 3.1, OpenJDK 25**.

## Start

```bash
cd spring-boot

mvn test                 # 42 testy, ok. 10 s - wszystko powinno byc zielone
mvn spring-boot:run      # aplikacja na http://localhost:8080

# z profilami (lekcje 2 i 3)
mvn spring-boot:run -Dspring-boot.run.profiles=dev,pirat

# jeden test / jedna klasa testow
mvn test -Dtest=ZadaniaControllerTest
```

W IntelliJ: otworz `spring-boot/pom.xml` jako projekt (albo dodaj go jako modul
Maven) i uruchom `TrainingApplication`.

Domena przez caly kurs: **miejsca na mapie (POI)** i prosta lista **zadan**.

## Lekcje

| # | Temat | Kod | Najwazniejsze pojecia |
|---|---|---|---|
| 1 | [Start i autokonfiguracja](docs/01-start.md) | `l01start` | `@SpringBootApplication`, startery, `@Conditional*`, raport warunkow, cykl zycia |
| 2 | [Beany i wstrzykiwanie zaleznosci](docs/02-beany.md) | `l02beany` | `@Component`, `@Bean`, konstruktor, `@Primary`, `@Qualifier`, `@Profile`, zasiegi |
| 3 | [Konfiguracja](docs/03-konfiguracja.md) | `l03konfiguracja` | `application.yml`, `@ConfigurationProperties` na rekordach, walidacja, profile, kolejnosc zrodel |
| 4 | [REST w Spring MVC](docs/04-rest.md) | `l04rest` | `@RestController`, mapowania, `@Valid`, `ResponseEntity`, `ProblemDetail`, `@RestControllerAdvice` |
| 5 | [Dane: Spring Data JPA](docs/05-dane.md) | `l05dane` | encje, repozytoria, derived queries, `@Query`, stronicowanie, `@Transactional`, dirty checking |
| 6 | [Testowanie](docs/06-testy.md) | `src/test/.../l06testy` | test slices, `@SpringBootTest`, `MockMvcTester`, `RestTestClient`, `@MockitoBean`, `@TestBean` |
| 7 | [Actuator i obserwowalnosc](docs/07-actuator.md) | `l07actuator` | health, info, metrics, wlasny `HealthIndicator`, Micrometer, zmiana logow w locie |
| 8 | [Klienci HTTP](docs/08-klienci.md) | `l08klienci` | `RestClient`, HTTP Interfaces, `@ImportHttpServices`, `MockRestServiceServer` |
| 9 | [Nowosci Boot 4 / Framework 7](docs/09-nowosci.md) | `l09nowosci` | wersjonowanie API, `@Retryable`, `@ConcurrencyLimit`, watki wirtualne, JSpecify, Jackson 3, modularne startery |

Kod: `src/main/java/com/tomtom/genesis/training/lNN...`,
testy: `src/test/java/com/tomtom/genesis/training/lNN...`.

## Jak pracowac z lekcja

1. Przeczytaj dokument (10-15 minut).
2. Otworz pakiet z kodem - komentarze w klasach odsylaja do konkretnych miejsc.
3. Uruchom testy lekcji, potem aplikacje i komendy `curl` z dokumentu.
4. Zrob **eksperymenty** z konca dokumentu. Kazdy mowi, co zmienic i co
   powinienes zobaczyc. Jesli wynik jest inny - to najlepszy moment na nauke.
5. `git stash` / `git checkout .` przywraca stan wyjsciowy.

## Sciagawka adresow (aplikacja uruchomiona)

```
GET  /api/powitania?imie=Ala            lekcja 2
GET  /api/konfiguracja                  lekcja 3
     /api/zadania[/{id}]                lekcja 4 (GET, POST, PUT, PATCH, DELETE)
     /api/miejsca[/{id}]                lekcja 5 (+ /kategoria/{k}, /miasto/{m}, /statystyki)
     /h2-console                        lekcja 5 (adres bazy w logu startowym)
     /actuator                          lekcja 7
GET  /api/klienci/demo/{id}             lekcja 8
GET  /api/nowosci/{powitanie,watek,niestabilna}   lekcja 9
```

## Co dalej (kolejne czesci)

Zostawione celowo na pozniej, zeby najpierw opanowac podstawy:

- **Spring Security** - uwierzytelnianie, JWT/OAuth2 Resource Server, testy z `@WithMockUser`;
- **Dane na powaznie** - Flyway, relacje i problem N+1, `@EntityGraph`, blokady, `JdbcClient`, propagacja transakcji;
- **Testcontainers i `@ServiceConnection`**, Docker Compose support;
- **Obserwowalnosc** - tracing (OpenTelemetry), `@Observed`, Prometheus/Grafana, structured logging;
- **Komunikacja asynchroniczna** - `@Async`, `@Scheduled`, Kafka, zdarzenia transakcyjne;
- **Pod maska autokonfiguracji** - wlasny starter, `@AutoConfiguration`, `BeanRegistrar` (Framework 7);
- **Wydajnosc startu i wdrozenie** - obrazy OCI (buildpacks), CDS / AOT cache z JDK 25, GraalVM native image;
- **WebFlux** - kiedy reaktywnie, a kiedy wystarcza watki wirtualne.
