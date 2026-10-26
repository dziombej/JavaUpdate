# Lekcja 6 — Testowanie

**Testy:** [`l06testy`](../src/test/java/com/tomtom/genesis/training/l06testy) oraz testy
wszystkich pozostalych lekcji.

Uruchom wszystko: `mvn test` (42 testy). Jedna klasa: `mvn test -Dtest=MiejscaIntegracjaTest`.

## Piramida w Spring Boot

| Rodzaj | Adnotacja | Co startuje | Czas | Przyklad |
|---|---|---|---|---|
| Jednostkowy | brak | nic - `new` | ms | `PolskiePowitanieTest` |
| Kontekst "na sucho" | `ApplicationContextRunner` | tylko podane klasy | ~10 ms | `BeanyKontekstTest`, `KonfiguracjaTest` |
| Slice web | `@WebMvcTest` | MVC, Jackson, walidacja, advice | ~0,5 s | `ZadaniaControllerTest`, `MiejscaControllerMockTest` |
| Slice danych | `@DataJpaTest` | JPA, baza wbudowana, transakcja z rollbackiem | ~1 s | `MiejsceRepositoryTest` |
| Slice klienta | `@RestClientTest` | RestClient + `MockRestServiceServer` | ~0,3 s | `PrzewodnikKlientTest` |
| Pelny kontekst | `@SpringBootTest` | wszystko, bez serwera albo z MockMvc | 2-3 s | `PowitaniaZegarTest`, `NowosciTest` |
| Pelny z HTTP | `@SpringBootTest(webEnvironment = RANDOM_PORT)` | wszystko + Tomcat | 2-3 s | `MiejscaIntegracjaTest`, `ActuatorTest` |

Zasada: **najwezszy test, ktory sprawdza to, co chcesz**. Slice'y sa szybkie,
bo nie laduja reszty aplikacji.

## Jak dziala slice

`@WebMvcTest(ZadaniaController.class)` skanuje tylko komponenty webowe
(kontrolery, `@ControllerAdvice`, konwertery, filtry). **Nie** laduje
`@Service`, `@Repository` ani zwyklych `@Configuration`. Brakujace beany:

- dokladasz recznie: `@Import(ZadaniaStore.class)`;
- albo podmieniasz mockiem: `@MockitoBean MiejscaService serwis;`;
- albo dajesz testowa konfiguracje: `@TestConfiguration`
  ([`BeanyConfigDlaTestow`](../src/test/java/com/tomtom/genesis/training/l06testy/BeanyConfigDlaTestow.java)).

Dlatego w tym projekcie `@EnableConfigurationProperties`, `@ImportHttpServices`
i `@EnableResilientMethods` sa w osobnych klasach `@Configuration`, a nie na
`TrainingApplication` - klasa glowna jest ladowana przez KAZDY slice.

## Narzedzia

**`MockMvcTester`** (Spring 6.2+) - MockMvc z asercjami AssertJ, bez
`perform().andExpect()`:

```java
assertThat(mvc.get().uri("/api/zadania/{id}", 42))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson().extractingPath("$.id").isEqualTo(42);
```

**`RestTestClient`** (Spring 7) - plynny klient testowy po prawdziwym HTTP
(lub po MockMvc). Zastepuje `TestRestTemplate`. Wymaga `@AutoConfigureRestTestClient`.

```java
klient.get().uri("/api/miejsca/{id}", id)
        .exchange()
        .expectStatus().isOk()
        .expectBody().jsonPath("$.nazwa").isEqualTo("Muzeum Bursztynu");
```

**`@MockitoBean` / `@MockitoSpyBean`** (Spring 6.2+) - zastepuja wycofane
`@MockBean` / `@SpyBean` z Boota (usuniete w Boot 4).

**`@TestBean`** - podmiana beana na wartosc ze statycznej metody, bez Mockito.
[`PowitaniaZegarTest`](../src/test/java/com/tomtom/genesis/training/l06testy/PowitaniaZegarTest.java)
ustawia zegar na wigilijny wieczor w pelnym kontekscie.

**`@RecordApplicationEvents`** - zbiera zdarzenia opublikowane w tescie
(`MiejscaServiceTest`).

## Cache kontekstow

Spring trzyma zbudowane konteksty miedzy klasami testow. Dwie klasy z ta sama
konfiguracja (te same adnotacje, propertiesy, mocki) dziela kontekst - druga
startuje natychmiast. **Kazdy inny `@MockitoBean`, `@TestBean` czy
`properties=` tworzy nowy kontekst.** Sto klas z roznymi mockami = sto startow.

Zobacz w logu `mvn test`, ile razy pojawia sie baner Spring Boot.

## Eksperymenty

1. **Brakujacy bean w slice.** Usun `@Import(...)` z `ZadaniaControllerTest`.
   Przeczytaj blad: ktorego beana brakuje i dlaczego?
2. **Mock zamiast prawdziwego.** Przepisz `ZadaniaControllerTest` tak, zeby
   `ZadaniaStore` byl `@MockitoBean`. Ktore testy musisz przepisac i czego
   sie przez to nauczyles o roznicy mock vs. prawdziwy obiekt?
3. **Bez serwera.** W `MiejscaIntegracjaTest` zamien `RANDOM_PORT` na `MOCK`.
   Testy dalej przechodza: `RestTestClient` sam przelacza sie na MockMvc, wiec
   ten sam kod testu dziala z prawdziwym HTTP i bez niego. Czego **nie**
   sprawdza wersja MOCK? (filtry serwera, prawdziwa serializacja na sieci,
   konfiguracja Tomcata)
4. **Rollback w `@DataJpaTest`.** Usun `repozytorium.deleteAll()` z
   `MiejsceRepositoryTest`. Ktore testy padaja? Dlaczego reszta dalej przechodzi
   mimo, ze kazdy test dodaje dane?
5. **Brak rollbacku w `RANDOM_PORT`.** `MiejscaIntegracjaTest` i `ActuatorTest`
   dodaja miejsca, ktore **zostaja** w bazie - zadanie HTTP idzie w innym
   watku, wiec transakcja testu go nie obejmuje. Napisz test, ktory na tym
   polega (np. liczy miejsca) i zobacz, ze zalezy od kolejnosci testow.
6. **Cache kontekstow.** Dodaj do `PowitaniaZegarTest`
   `@SpringBootTest(properties = "szkolenie.kurs.nazwa=Inna")` i zobacz
   w logu dodatkowy start.
7. **Test konfiguracji.** Dopisz do `KonfiguracjaTest` przypadek: pusta lista
   `prowadzacy` w yml nadal daje pusta liste, nie `null`.

## Pulapki

- `@SpringBootTest` wszedzie "bo dziala" - wolne testy, ktorych nikt nie odpala.
- Zaleznosc testow od danych z `data.sql` - zmiana seeda psuje niezwiazane testy.
- `@DirtiesContext` jako lek na wszystko - zabija cache kontekstow.

Dalej: [Lekcja 7 — Actuator](07-actuator.md)
