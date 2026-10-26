# Lekcja 4 — REST w Spring MVC

**Kod:** [`l04rest`](../src/main/java/com/tomtom/genesis/training/l04rest)
**Testy:** [`ZadaniaControllerTest`](../src/test/java/com/tomtom/genesis/training/l04rest/ZadaniaControllerTest.java)

Lekcja uzywa magazynu w pamieci
([`ZadaniaStore`](../src/main/java/com/tomtom/genesis/training/l04rest/ZadaniaStore.java)),
zeby cwiczyc HTTP bez bazy. Baza przyjdzie w lekcji 5.

## Droga zadania

```
HTTP -> Tomcat -> DispatcherServlet -> HandlerMapping (ktora metoda?)
     -> konwersja parametrow / JSON -> walidacja -> Twoja metoda
     -> wynik -> Jackson -> HTTP
     \-> wyjatek -> @RestControllerAdvice -> ProblemDetail -> HTTP
```

## Mapowania i parametry

| Adnotacja | Skad wartosc | Przyklad |
|---|---|---|
| `@PathVariable` | z sciezki | `/api/zadania/{id}` |
| `@RequestParam` | z query stringa | `?priorytet=WYSOKI&tylkoOtwarte=true` |
| `@RequestBody` | z ciala (JSON) | `POST` z `NoweZadanie` |
| `@RequestHeader` | z naglowka | `@RequestHeader("X-Trace") String t` |

Konwersja typow jest automatyczna: `long`, enumy (`Priorytet`), daty,
`boolean`. Zla wartosc (`/api/zadania/abc`) = 400, nie 500.

## Co zwracac

- **obiekt / rekord** - 200 + JSON (najczesciej);
- **`ResponseEntity<T>`** - gdy potrzebujesz statusu i naglowkow, np.
  `201 Created` + `Location` w `POST`;
- **`@ResponseStatus(HttpStatus.NO_CONTENT)`** na metodzie `void` - inny
  status bez `ResponseEntity` (uzyte w lekcji 5).

## DTO wejsciowe i walidacja

`NoweZadanie` to osobny rekord niz `Zadanie`: klient nie ustawia `id`, daty ani
statusu. Adnotacje Jakarta Validation (`@NotBlank`, `@Size`, `@NotNull`)
dzialaja, bo w kontrolerze jest `@Valid @RequestBody`.

## Bledy: ProblemDetail (RFC 9457)

Standardowy format bledu `application/problem+json`:

```json
{
  "type": "https://szkolenie.example/bledy/brak-zadania",
  "title": "Zadanie nie istnieje",
  "status": 404,
  "detail": "Nie ma zadania o id 99",
  "instance": "/api/zadania/99",
  "id": 99
}
```

[`BledyRestHandler`](../src/main/java/com/tomtom/genesis/training/l04rest/BledyRestHandler.java):

- `@RestControllerAdvice` - globalny handler dla wszystkich kontrolerow;
- `@ExceptionHandler` - "ten wyjatek zamieniam na taka odpowiedz";
- dziedziczy po `ResponseEntityExceptionHandler`, wiec standardowe bledy MVC
  (zly JSON, 405, 415, walidacja) tez sa ProblemDetail. Nadpisujemy walidacje,
  zeby dodac mape `bledy` z polami.

W lekcji 5 jest drugi sposob - wyjatek dziedziczacy po `ErrorResponseException`
sam niesie status i tresc.

## Uruchom

```bash
curl -s localhost:8080/api/zadania | jq
curl -s 'localhost:8080/api/zadania?priorytet=WYSOKI' | jq

curl -si -H 'Content-Type: application/json' \
     -d '{"tytul":"Nauczyc sie REST","priorytet":"WYSOKI"}' \
     localhost:8080/api/zadania
# HTTP/1.1 201, Location: http://localhost:8080/api/zadania/3

curl -s -X PATCH 'localhost:8080/api/zadania/3/zrobione' | jq
curl -s 'localhost:8080/api/zadania?tylkoOtwarte=true' | jq
curl -si -X DELETE localhost:8080/api/zadania/3          # 204
curl -s localhost:8080/api/zadania/3 | jq                # 404 ProblemDetail

curl -s -H 'Content-Type: application/json' -d '{"tytul":"","priorytet":null}' \
     localhost:8080/api/zadania | jq
# {"title":"Niepoprawne dane","status":400,"bledy":{"priorytet":"must not be null","tytul":"must not be blank"}, ...}
```

Lista wszystkich mapowan aplikacji: `curl -s localhost:8080/actuator/mappings | jq`.

## Eksperymenty

1. **Zly typ.** `curl -i localhost:8080/api/zadania/abc` i
   `curl -i 'localhost:8080/api/zadania?priorytet=PILNY'`. Jaki status i dlaczego nie 500?
2. **Zly JSON.** Wyslij `-d '{"tytul": '`. Kto wygenerowal odpowiedz?
3. **Zla metoda.** `curl -i -X PUT localhost:8080/api/zadania` - 405 i naglowek `Allow`.
4. **Wlasna walidacja.** Dodaj do `NoweZadanie` warunek "tytul nie moze
   zaczynac sie od spacji" (`@Pattern(regexp = "^\\S.*")`). Dopisz test
   w `ZadaniaControllerTest`.
5. **Komunikaty po polsku.** Utworz `src/main/resources/ValidationMessages.properties`
   z `jakarta.validation.constraints.NotBlank.message=nie moze byc puste`.
6. **Wylacz nasz handler.** Zakomentuj `extends ResponseEntityExceptionHandler`
   i metode `handleMethodArgumentNotValid`. Blad walidacji nadal bedzie
   ProblemDetail (dzieki `spring.mvc.problemdetails.enabled`), ale bez listy pol.
   Potem wylacz tez te wlasciwosc - jak wyglada blad wtedy?
7. **Nowy endpoint.** Dodaj `GET /api/zadania/statystyki` zwracajacy rekord
   `(long wszystkie, long zrobione, Map<Priorytet, Long> wgPriorytetu)`
   i test w `@WebMvcTest`.
8. **Format daty.** Pole `utworzono` to `Instant` w ISO-8601. Ustaw
   `spring.jackson.datatype.datetime.write-dates-as-timestamps: true`
   i zobacz roznice.

## Pulapki

- Zwracanie encji JPA prosto z kontrolera: wycieka model bazy, lazy loading
  wybucha poza transakcja, a zmiana encji zmienia API. Stosuj DTO.
- `@RequestParam` bez `required = false` i bez `defaultValue` jest wymagany -
  brak = 400.
- Kontroler to singleton - zadnego stanu zwiazanego z zadaniem w polach.

Dalej: [Lekcja 5 — Dane: Spring Data JPA](05-dane.md)
