# Lekcja 2 — Beany i wstrzykiwanie zaleznosci

**Kod:** [`l02beany`](../src/main/java/com/tomtom/genesis/training/l02beany)
**Testy:** [`PolskiePowitanieTest`](../src/test/java/com/tomtom/genesis/training/l02beany/PolskiePowitanieTest.java),
[`BeanyKontekstTest`](../src/test/java/com/tomtom/genesis/training/l02beany/BeanyKontekstTest.java)

## Bean i kontener

**Bean** to obiekt, ktorym zarzadza Spring: tworzy go, wstrzykuje mu
zaleznosci, a na koncu niszczy. Zbior beanow to **kontekst aplikacji**
(`ApplicationContext`).

Dwa sposoby na bean:

| Sposob | Kiedy | Przyklad |
|---|---|---|
| Adnotacja na klasie: `@Component`, `@Service`, `@Repository`, `@RestController` | Twoja klasa | `PowitaniaService` |
| Metoda `@Bean` w klasie `@Configuration` | cudza klasa albo tworzenie z logika | `BeanyConfig.zegar()` zwraca `Clock` |

`@Service`, `@Repository` i `@Controller` to `@Component` z nazwa mowiaca
o roli. `@Repository` dodatkowo tlumaczy wyjatki bazy na `DataAccessException`.

## Wstrzykiwanie przez konstruktor

```java
@Service
public class PowitaniaService {
    PowitaniaService(Powitanie domyslne,                          // 1
                     @Qualifier("angielskie") Powitanie angielskie, // 2
                     List<Powitanie> wszystkie) { ... }           // 3
}
```

- Jeden konstruktor = Spring uzywa go bez `@Autowired`.
- Pola moga byc `final`, obiekt jest kompletny od pierwszej chwili.
- **Mozesz go utworzyc bez Springa** - patrz `PolskiePowitanieTest`, ktory
  podaje staly `Clock` przez `new` i dziala w milisekundy.

Wstrzykiwanie w pole (`@Autowired private Foo foo;`) dziala, ale ukrywa
zaleznosci i utrudnia testy. Nie uzywaj go w nowym kodzie.

## Gdy pasuje kilka beanow

Mamy trzy implementacje `Powitanie`. Kto trafi do parametru `Powitanie domyslne`?

| Mechanizm | Efekt |
|---|---|
| `@Primary` na `PolskiePowitanie` | wygrywa, gdy ktos prosi o "jakies" `Powitanie` |
| `@Qualifier("angielskie")` | wskazanie konkretnego beana po nazwie |
| `List<Powitanie>` / `Map<String, Powitanie>` | wszystkie naraz (mapa: nazwa beana -> bean) |
| `@Profile("pirat")` na `PirackiePowitanie` | bean istnieje tylko przy aktywnym profilu |

Bez `@Primary` i bez kwalifikatora start konczy sie bledem
`NoUniqueBeanDefinitionException` - z czytelnym opisem, ktore beany pasuja.

## Zasiegi

- **singleton** (domyslny) - jedna instancja na kontekst. Musi byc bezstanowy
  albo bezpieczny watkowo - korzysta z niego wiele zadan HTTP naraz.
- **prototype** - nowa instancja przy kazdym pobraniu (`LicznikZadan`).
- **request / session** - jedna na zadanie HTTP / sesje (rzadko potrzebne).

## Cykl zycia beana

`@PostConstruct` uruchamia sie po wstrzyknieciu zaleznosci, `@PreDestroy`
przy zamykaniu kontekstu. Zobacz logi `[lekcja 2]` w `PowitaniaService`.

## Uruchom

```bash
curl 'localhost:8080/api/powitania?imie=Ala'
# {"en":"Hello, Ala!","pl":"Dzien dobry, Ala!"}

mvn spring-boot:run -Dspring-boot.run.profiles=pirat
curl 'localhost:8080/api/powitania?imie=Jack'
# {"en":"Hello, Jack!","pirat":"Ahoj, Jack, szczurze ladowy!","pl":"Dzien dobry, Jack!"}

mvn test -Dtest='PolskiePowitanieTest,BeanyKontekstTest'
```

## Eksperymenty

1. **Usun `@Primary`** z `PolskiePowitanie` i uruchom aplikacje. Przeczytaj
   blad do konca - mowi, co zrobic.
2. Zamiast `@Primary` zmien nazwe parametru konstruktora na `polskiePowitanie`.
   Zadziala! Gdy typ jest niejednoznaczny, Spring probuje dopasowac **nazwe
   parametru** do nazwy beana. Wygodne, ale kruche - dlatego wolimy jawne adnotacje.
3. **Mapa zamiast listy.** Zmien `List<Powitanie>` na `Map<String, Powitanie>`
   i wypisz klucze w `@PostConstruct`. Jakie sa nazwy beanow?
4. **Prototype.** Usun `@Scope` z `LicznikZadan` i uruchom `BeanyKontekstTest` -
   ktory test pada i dlaczego?
5. **Cykl zaleznosci.** Wstrzyknij `PowitaniaService` do `AngielskiePowitanie`.
   Boot odmowi startu z rysunkiem cyklu. (Cykle sa domyslnie zabronione od Boot 2.6.)
6. **Zle miejsce.** Przenies `AngielskiePowitanie` do pakietu `com.tomtom.inne`.
   Co sie dzieje i dlaczego? (wskazowka: lekcja 1, `@ComponentScan`)
7. **Bez Springa.** Napisz w `PolskiePowitanieTest` test dla godziny 17:59:59
   i 18:00:00. Ile trwa caly test?

## Pulapki

- Stan w singletonie (np. zwykle pole `int licznik`) = wyscig miedzy watkami.
- `@Bean` w klasie bez `@Configuration` (tzw. lite mode) nie przechodzi przez
  proxy: wywolanie jednej metody `@Bean` z drugiej tworzy NOWY obiekt.
  My uzywamy `proxyBeanMethods = false` swiadomie i nigdy nie wolamy metod `@Bean`
  bezposrednio - tak tez robi sam Boot.
- `new PowitaniaService(...)` w kodzie produkcyjnym = obiekt poza kontenerem:
  bez transakcji, bez `@Retryable`, bez proxy.

Dalej: [Lekcja 3 — Konfiguracja](03-konfiguracja.md)
