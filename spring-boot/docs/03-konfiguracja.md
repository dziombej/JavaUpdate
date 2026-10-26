# Lekcja 3 — Konfiguracja

**Kod:** [`l03konfiguracja`](../src/main/java/com/tomtom/genesis/training/l03konfiguracja),
[`application.yml`](../src/main/resources/application.yml),
[`application-dev.yml`](../src/main/resources/application-dev.yml)
**Testy:** [`KonfiguracjaTest`](../src/test/java/com/tomtom/genesis/training/l03konfiguracja/KonfiguracjaTest.java)

## Zasada: ten sam artefakt, rozna konfiguracja

Jeden JAR idzie na dev, test i produkcje. Roznice (adresy, hasla, limity)
podaje sie z zewnatrz. Boot zbiera wlasciwosci z wielu zrodel w jedno
`Environment`.

## Kolejnosc zrodel (wyzsze wygrywa)

Uproszczona lista, od najwazniejszego:

1. argumenty linii polecen: `--szkolenie.kurs.nazwa=X`
2. `SPRING_APPLICATION_JSON`
3. zmienne srodowiskowe: `SZKOLENIE_KURS_NAZWA=X`
4. `application-{profil}.yml` **poza** JAR-em, potem w JAR-ze
5. `application.yml` poza JAR-em, potem w JAR-ze
6. `@DefaultValue` / wartosci domyslne w kodzie

To dlatego na Kubernetesie wystarczy zmienna srodowiskowa albo zamontowany
plik - nie trzeba przebudowywac obrazu.

## Relaxed binding

Te zapisy trafiaja w to samo pole `maksUczestnikow`:

```
szkolenie.kurs.maks-uczestnikow   # zalecany w plikach (kebab-case)
szkolenie.kurs.maksUczestnikow
SZKOLENIE_KURS_MAKSUCZESTNIKOW    # zmienna srodowiskowa: kropki -> _, myslniki znikaja
```

## `@ConfigurationProperties` zamiast `@Value`

```java
@ConfigurationProperties("szkolenie.kurs")
@Validated
public record KursProperties(
        @NotBlank String nazwa,
        @DefaultValue("12") @Min(1) @Max(30) int maksUczestnikow,
        @DefaultValue("45m") Duration czasLekcji,
        @DefaultValue List<String> prowadzacy,
        @Valid Sala sala) {

    public record Sala(@NotBlank String budynek, @Positive int pietro) { }
}
```

| | `@ConfigurationProperties` | `@Value("${...}")` |
|---|---|---|
| Grupowanie | caly prefiks w jednym obiekcie | pojedyncza wartosc |
| Typy | `Duration`, `DataSize`, listy, mapy, zagniezdzone rekordy | proste, reszta recznie |
| Walidacja | `@Validated` - blad = brak startu | brak |
| Podpowiedzi w IDE | tak (configuration-processor) | nie |
| Wyrazenia SpEL | nie | tak (`#{...}`) |

Rekord trzeba **zarejestrowac**:
[`KonfiguracjaConfig`](../src/main/java/com/tomtom/genesis/training/l03konfiguracja/KonfiguracjaConfig.java)
ma `@EnableConfigurationProperties(KursProperties.class)`. Alternatywa:
`@ConfigurationPropertiesScan` na klasie glownej.

Konwersje za darmo: `Duration` z `90m`, `2h`, `PT30S`; `DataSize` z `10MB`;
listy z `a,b,c` albo z listy YAML; enumy bez wzgledu na wielkosc liter.

## Profile

`application-dev.yml` laduje sie tylko z aktywnym profilem `dev` i **nadpisuje**
pojedyncze klucze - reszta zostaje z `application.yml`.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# albo: SPRING_PROFILES_ACTIVE=dev java -jar target/*.jar
```

Profil wlacza tez beany z `@Profile` (lekcja 2) i mozna je laczyc: `dev,pirat`.

## Uruchom

```bash
curl -s localhost:8080/api/konfiguracja | jq
```

```json
{
  "kurs": {
    "nazwa": "Spring Boot od podstaw",
    "maksUczestnikow": 12,
    "czasLekcji": "PT1H30M",
    "prowadzacy": ["Ala", "Olek"],
    "sala": { "budynek": "B2", "pietro": 3 }
  },
  "nazwaPrzezValue": "Spring Boot od podstaw",
  "brakujacaZDomyslna": "wartosc domyslna",
  "aktywneProfile": []
}
```

Gdy aplikacja dziala, zrodla zobaczysz tez w Actuatorze (lekcja 7):
`/actuator/configprops` i `/actuator/env/szkolenie.kurs.nazwa` - ten drugi
mowi, **z ktorego zrodla** przyszla wartosc.

## Eksperymenty

1. **Profil.** Uruchom z profilem `dev`. Co sie zmienilo w `/api/konfiguracja`,
   a co zostalo?
2. **Zmienna srodowiskowa bije plik.**
   `SZKOLENIE_KURS_NAZWA="Z env" mvn spring-boot:run`
3. **Argument bije zmienna.** Dodaj jeszcze
   `-Dspring-boot.run.arguments=--szkolenie.kurs.nazwa=Z_argumentu`.
4. **Walidacja przy starcie.** Ustaw `maks-uczestnikow: 500` i uruchom.
   Przeczytaj komunikat: `Binding to target ... failed ... must be less than or equal to 30`.
   Porownaj z testem `zla_konfiguracja_zatrzymuje_start`.
5. **Brakujacy klucz w `@Value`.** W `KonfiguracjaController` usun
   `:wartosc domyslna` z `@Value`. Co sie dzieje przy starcie?
6. **Literowka.** Zmien w yml `czas-lekcji` na `czas-lekcjii`. Aplikacja
   wystartuje z wartoscia domyslna 45m - nieznane klucze sa ignorowane!
   To argument za testami konfiguracji i podpowiedziami IDE.
7. **Podpowiedzi IDE.** Po `mvn compile` otworz `application.yml` w IntelliJ
   i zacznij pisac `szkolenie.kurs.` - podpowiedzi biora sie z
   `target/classes/META-INF/spring-configuration-metadata.json`.
8. **Nowe pole.** Dodaj do `KursProperties` pole `DataSize maksRozmiarMaterialow`
   z `@DefaultValue("5MB")`, wystaw je w kontrolerze i ustaw `1GB` w yml.

## Pulapki

- Sekrety w `application.yml` w repozytorium. Hasla podawaj przez zmienne
  srodowiskowe / sekrety Kubernetesa / Vault.
- `spring.profiles.active` w `application-{profil}.yml` jest zabronione
  (profil nie moze aktywowac profilu w ten sposob). Do grupowania sluzy
  `spring.profiles.group`.
- YAML: `on`, `off`, `yes`, `no` bez cudzyslowu to wartosci logiczne, a
  `010` moze byc liczba osemkowa. W razie watpliwosci - cudzyslow.

Dalej: [Lekcja 4 — REST w Spring MVC](04-rest.md)
