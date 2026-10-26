# Lekcja 7 — Actuator i obserwowalnosc

**Kod:** [`l07actuator`](../src/main/java/com/tomtom/genesis/training/l07actuator)
**Testy:** [`ActuatorTest`](../src/test/java/com/tomtom/genesis/training/l07actuator/ActuatorTest.java)

## Co daje Actuator

`spring-boot-starter-actuator` dodaje endpointy operacyjne pod `/actuator`.
Domyslnie przez HTTP wystawione jest tylko `health`. W tym projekcie
(`management.endpoints.web.exposure.include`) wlaczylismy wiecej - **na
produkcji wystawiaj tylko to, co potrzebne, i za uwierzytelnieniem** (`env`
i `configprops` pokazuja konfiguracje; Boot maskuje hasla, ale i tak).

| Endpoint | Do czego |
|---|---|
| `/actuator/health` | stan aplikacji i zaleznosci (baza, dysk, nasze `miejsca`); Kubernetes: `/health/liveness`, `/health/readiness` |
| `/actuator/info` | wersja, git, Java, OS, dowolne `info.*` |
| `/actuator/metrics` | lista metryk Micrometera; `/metrics/{nazwa}?tag=k:v` |
| `/actuator/env` | wszystkie zrodla wlasciwosci i ich kolejnosc |
| `/actuator/configprops` | wszystkie beany `@ConfigurationProperties` z wartosciami |
| `/actuator/beans` | wszystkie beany i ich zaleznosci |
| `/actuator/conditions` | raport autokonfiguracji (to samo co `--debug`, ale na zywo) |
| `/actuator/mappings` | wszystkie mapowania URL -> metoda |
| `/actuator/loggers` | poziomy logowania - **zmiana w locie** przez POST |

## Health

```bash
curl -s localhost:8080/actuator/health | jq
```

```json
{
  "status": "UP",
  "components": {
    "db":       { "status": "UP", "details": { "database": "H2", "validationQuery": "isValid()" } },
    "diskSpace":{ "status": "UP", ... },
    "miejsca":  { "status": "UP", "details": { "liczbaMiejsc": 12 } },
    "livenessState":  { "status": "UP" },
    "readinessState": { "status": "UP" },
    ...
  }
}
```

`db` dodal Boot, bo jest `DataSource`. `miejsca` to nasz
[`MiejscaHealthIndicator`](../src/main/java/com/tomtom/genesis/training/l07actuator/MiejscaHealthIndicator.java)
(nazwa beana bez `HealthIndicator`). Status calosci to najgorszy ze skladnikow;
`DOWN` daje HTTP 503 - load balancer i Kubernetes reaguja na sam kod.

Boot 4: klasy health przeniesiono do modulu `spring-boot-health`, pakiet
`org.springframework.boot.health.contributor` (w Boot 3:
`org.springframework.boot.actuate.health`).

## Metryki: Micrometer

Micrometer to "SLF4J dla metryk": jeden API, wiele backendow (Prometheus,
Datadog, CloudWatch...). Boot sam mierzy JVM, Tomcata, Hikari, zadania HTTP
(`http.server.requests`), klientow HTTP (`http.client.requests`).

Wlasna metryka -
[`MetrykiMiejsc`](../src/main/java/com/tomtom/genesis/training/l07actuator/MetrykiMiejsc.java):
licznik `szkolenie.miejsca.utworzone` z tagiem `kategoria`. Serwis z lekcji 5
o metrykach nie wie - publikuje zdarzenie `MiejsceUtworzone`, a ten bean je
slucha (`@EventListener`). Tak dokladasz obserwowalnosc bez mieszania jej
z logika.

```bash
curl -s -H 'Content-Type: application/json' \
  -d '{"nazwa":"Hotel Morski","kategoria":"HOTEL","miasto":"Sopot","szerokosc":54.44,"dlugosc":18.57}' \
  localhost:8080/api/miejsca > /dev/null
curl -s localhost:8080/actuator/metrics/szkolenie.miejsca.utworzone | jq
curl -s 'localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/miejsca/{id}' | jq
```

## Info

`/actuator/info` laczy: `info.*` z `application.yml`, `management.info.java/os`
i nasz
[`SzkolenieInfoContributor`](../src/main/java/com/tomtom/genesis/training/l07actuator/SzkolenieInfoContributor.java).

## Eksperymenty

1. **DOWN.** Usun wszystkie miejsca (petla: `for i in $(seq 1 20); do curl -s -X DELETE localhost:8080/api/miejsca/$i; done`)
   i zobacz `curl -i localhost:8080/actuator/health` - status `DOWN` i HTTP 503.
   Dodaj jedno miejsce - wraca `UP`.
2. **Logi w locie.**
   ```bash
   curl -s localhost:8080/actuator/loggers/org.hibernate.SQL | jq
   curl -i -X POST -H 'Content-Type: application/json' \
        -d '{"configuredLevel":"DEBUG"}' localhost:8080/actuator/loggers/org.hibernate.SQL
   curl -s localhost:8080/api/miejsca > /dev/null    # zobacz SQL w konsoli aplikacji
   ```
   Bez restartu! Przywroc: `{"configuredLevel":null}`.
3. **Skad wartosc?** `curl -s localhost:8080/actuator/env/szkolenie.kurs.nazwa | jq`.
   Uruchom ponownie z profilem `dev` i porownaj `propertySources`.
4. **Grupy health.** Dodaj
   `management.endpoint.health.group.gotowosc.include: db,miejsca` i otworz
   `/actuator/health/gotowosc`.
5. **Prometheus.** Dodaj zaleznosc `io.micrometer:micrometer-registry-prometheus`,
   dopisz `prometheus` do `exposure.include` i otworz `/actuator/prometheus`.
   Znajdz `szkolenie_miejsca_utworzone_total`.
6. **Timer.** W `MetrykiMiejsc` albo w serwisie zmierz czas `statystyki()`
   uzywajac `Timer.builder(...).register(rejestr).record(() -> ...)`.
   Sprawdz `count`, `totalTime` i `max` w `/actuator/metrics`.
7. **Info z gita i builda.** Dodaj do `spring-boot-maven-plugin` cel
   `build-info` i plugin `io.github.git-commit-id:git-commit-id-maven-plugin`.
   `/actuator/info` pokaze wersje artefaktu i commit.

## Pulapki

- `exposure.include: "*"` na produkcji bez zabezpieczenia.
- Wolny `HealthIndicator` (zapytanie do zewnetrznego serwisu) spowalnia kazda
  probe Kubernetesa. Liveness nie powinien zalezec od zewnetrznych systemow.
- Tagi o wysokiej kardynalnosci (id uzytkownika, pelny URL) zapychaja pamiec
  i backend metryk. Tag = kilka-kilkanascie wartosci.

Dalej: [Lekcja 8 — Klienci HTTP](08-klienci.md)
