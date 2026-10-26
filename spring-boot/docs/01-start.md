# Lekcja 1 — Start i autokonfiguracja

**Kod:** [`l01start`](../src/main/java/com/tomtom/genesis/training/l01start),
[`TrainingApplication`](../src/main/java/com/tomtom/genesis/training/TrainingApplication.java),
[`pom.xml`](../pom.xml)

## Po co jest Spring Boot

Spring Framework to kontener, ktory tworzy obiekty (beany) i laczy je ze soba.
Sam w sobie wymaga jednak duzo konfiguracji: serwer WWW, pula polaczen do bazy,
menedzer transakcji, Jackson, walidator... Spring Boot robi te rzeczy za Ciebie,
**zgadujac na podstawie tego, co masz na classpath**, i pozwala kazda z nich
nadpisac.

Trzy filary:

1. **Startery** - zaleznosci "pakietami". `spring-boot-starter-webmvc` przynosi
   Spring MVC, Tomcata, Jacksona i ich zgodne wersje.
2. **Autokonfiguracja** - setki klas `@AutoConfiguration` z warunkami typu
   "jesli jest klasa X i nie ma beana Y, utworz Y".
3. **Uruchamialny JAR** - `mvn package` daje jeden plik `java -jar app.jar`
   z wbudowanym serwerem.

## `@SpringBootApplication` - trzy adnotacje w jednej

```java
@SpringBootApplication          // = @SpringBootConfiguration
public class TrainingApplication //  + @EnableAutoConfiguration
{                                //  + @ComponentScan (ten pakiet i podpakiety!)
    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}
```

Konsekwencja `@ComponentScan`: klasa glowna musi lezec w pakiecie **nad**
wszystkimi Twoimi klasami. Klasa w `com.tomtom.inne` nie zostanie znaleziona.

## `pom.xml` - co tam jest i dlaczego

- `spring-boot-starter-parent` - BOM z wersjami setek bibliotek. Dlatego przy
  zaleznosciach **nie piszemy wersji**.
- **Boot 4: startery sa modularne.** Dawny `spring-boot-starter-web` nazywa sie
  teraz `spring-boot-starter-webmvc`; RestClient ma wlasny
  `spring-boot-starter-restclient`; konsola H2 to `spring-boot-h2console`.
  Kazdy starter ma tez swoj odpowiednik testowy (`...-webmvc-test`,
  `...-data-jpa-test`), ktory przynosi wlasciwe adnotacje slice.
- `spring-boot-configuration-processor` - generuje metadane, dzieki ktorym IDE
  podpowiada nasze wlasne klucze w `application.yml` (lekcja 3).

## Co widac przy starcie

Uruchom `mvn spring-boot:run` i znajdz w logu **RAPORT STARTOWY**
([`RaportStartowy`](../src/main/java/com/tomtom/genesis/training/l01start/RaportStartowy.java)):

```
Java            : 25+36-3489
Spring Framework: 7.0.10
Spring Boot     : 4.1.2
Liczba beanow   : 443
Watki wirtualne : true
--- beany, ktorych NIE napisalismy, a sa (autokonfiguracja) ---
DataSource                   -> com.zaxxer.hikari.HikariDataSource
PlatformTransactionManager   -> org.springframework.orm.jpa.JpaTransactionManager
JsonMapper                   -> tools.jackson.databind.json.JsonMapper
Builder                      -> org.springframework.web.client.DefaultRestClientBuilder
```

Napisalismy kilkadziesiat klas, a w kontekscie jest ponad 400 beanow. Reszte
dolozyla autokonfiguracja.

`RaportStartowy` sam jest warunkowy:

```java
@ConditionalOnProperty(name = "szkolenie.start.raport", havingValue = "true", matchIfMissing = true)
```

To dokladnie ten sam mechanizm, ktorym Boot decyduje o swoich beanach. Inne
popularne warunki: `@ConditionalOnClass`, `@ConditionalOnMissingBean`,
`@ConditionalOnWebApplication`.

### Cykl zycia

[`ZdarzeniaCykluZycia`](../src/main/java/com/tomtom/genesis/training/l01start/ZdarzeniaCykluZycia.java)
loguje kolejne etapy. Kolejnosc w logu:

```
Started TrainingApplication in 2.6 seconds
[cykl zycia] ApplicationStartedEvent - ...
RAPORT STARTOWY                                    <- ApplicationRunner
[cykl zycia] ApplicationReadyEvent - ...
...Ctrl+C...
[cykl zycia] ContextClosedEvent - ...
[lekcja 2] PowitaniaService konczy prace          <- @PreDestroy
```

## Eksperymenty

1. **Raport warunkow.** Uruchom z `--debug`:
   `mvn spring-boot:run -Dspring-boot.run.arguments=--debug`.
   Znajdz `CONDITIONS EVALUATION REPORT`. Sekcja *Positive matches* mowi, ktora
   autokonfiguracja zadzialala i dlaczego, *Negative matches* - dlaczego nie.
   Poszukaj `DataSourceAutoConfiguration` i `H2ConsoleAutoConfiguration`.
2. **Wylacz bean warunkowy.** W `application.yml` ustaw
   `szkolenie.start.raport: false`. Raport zniknie, bo bean nie powstanie wcale
   (a nie "powstanie i nic nie zrobi").
3. **Bez serwera WWW.** Dodaj argument `--spring.main.web-application-type=none`.
   Aplikacja wystartuje, wypisze raport... i sie zakonczy, bo nic jej nie trzyma
   przy zyciu.
4. **Usun zaleznosc.** Zakomentuj w `pom.xml` zaleznosc `h2` i uruchom.
   Zobaczysz czytelny blad `Failed to configure a DataSource` z podpowiedzia,
   co zrobic - to `FailureAnalyzer` Boota.
5. **Wylacz konkretna autokonfiguracje.** Ustaw
   `spring.autoconfigure.exclude: org.springframework.boot.h2console.autoconfigure.H2ConsoleAutoConfiguration`
   i sprawdz, ze `/h2-console` zwraca 404.
6. **Baner.** Utworz `src/main/resources/banner.txt` z dowolnym tekstem albo
   ustaw `spring.main.banner-mode: off`.
7. **Gruby JAR.** `mvn package -DskipTests && java -jar target/spring-boot-training-1.0-SNAPSHOT.jar`.
   Zajrzyj do srodka: `jar tf target/*.jar | head -30` - zobaczysz `BOOT-INF/lib`.

## Pulapki

- Klasa glowna w zlym pakiecie = "nie widzi moich beanow".
- `@EnableXxx` na klasie glownej dziala tez w testach slice (lekcja 6). Lepiej
  trzymac je w osobnych klasach `@Configuration` - tak jak w tym projekcie.
- Autokonfiguracja **ustepuje** Twoim beanom (`@ConditionalOnMissingBean`).
  Jesli zdefiniujesz wlasny `DataSource`, Boot nie utworzy swojego - i nie
  dostaniesz tez jego wlasciwosci `spring.datasource.*`.

Dalej: [Lekcja 2 — Beany i wstrzykiwanie zaleznosci](02-beany.md)
