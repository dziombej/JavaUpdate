# Lekcja 5 — Dane: Spring Data JPA

**Kod:** [`l05dane`](../src/main/java/com/tomtom/genesis/training/l05dane),
[`data.sql`](../src/main/resources/data.sql)
**Testy:** [`MiejsceRepositoryTest`](../src/test/java/com/tomtom/genesis/training/l05dane/MiejsceRepositoryTest.java),
[`MiejscaServiceTest`](../src/test/java/com/tomtom/genesis/training/l05dane/MiejscaServiceTest.java)

## Warstwy

```
MiejscaController  (HTTP, DTO, walidacja)
      |
MiejscaService     (logika, @Transactional, zdarzenia)
      |
MiejsceRepository  (Spring Data - interfejs bez implementacji)
      |
Hibernate 7 (JPA) -> HikariCP -> H2 w pamieci
```

Starter `spring-boot-starter-data-jpa` + sterownik `h2` wystarcza, zeby Boot
skonfigurowal `DataSource` (Hikari), `EntityManagerFactory`,
`JpaTransactionManager` i utworzyl tabele z encji.

## Encja

[`Miejsce`](../src/main/java/com/tomtom/genesis/training/l05dane/Miejsce.java) -
zwykla klasa z `@Entity` i `@Id`. **Nie rekord**, bo Hibernate potrzebuje:
konstruktora bez argumentow (moze byc `protected`), mutowalnych pol (dirty
checking) i mozliwosci tworzenia proxy (lazy loading).

Mapowanie nazw: pole `szerokosc` -> kolumna `szerokosc`, `utworzonoDnia` ->
`utworzono_dnia` (snake_case). `@Enumerated(EnumType.STRING)` - zapisuj enum
jako tekst; domyslny `ORDINAL` zapisuje numer i psuje dane po dopisaniu
nowej wartosci w srodku enuma.

## Repozytorium

```java
public interface MiejsceRepository extends JpaRepository<Miejsce, Long> {
    List<Miejsce> findByKategoria(Kategoria kategoria);
    List<Miejsce> findByMiastoIgnoreCaseOrderByNazwaAsc(String miasto);
    Page<Miejsce> findByNazwaContainingIgnoreCase(String fragment, Pageable stronicowanie);
    long countByKategoria(Kategoria kategoria);

    @Query("select new ...LicznikKategorii(m.kategoria, count(m)) from Miejsce m group by m.kategoria ...")
    List<LicznikKategorii> policzWgKategorii();
}
```

- `JpaRepository` daje gotowe: `save`, `findById`, `findAll(Pageable)`,
  `count`, `deleteById`, `existsById`...
- **Derived queries**: Spring Data parsuje nazwe metody. Slowa kluczowe:
  `And`, `Or`, `Between`, `LessThan`, `Like`, `Containing`, `StartingWith`,
  `IgnoreCase`, `OrderBy...Asc/Desc`, `In`, `IsNull`, `Top3`/`First`.
  Literowka w nazwie pola = blad **przy starcie**.
- **`@Query`** - JPQL (na encjach, nie tabelach), gdy nazwa robi sie za dluga.
  `select new pakiet.Rekord(...)` mapuje wynik prosto na rekord.
- **`Pageable` / `Page`** - stronicowanie i sortowanie: zapytanie z
  `LIMIT/OFFSET` plus osobne `COUNT`.

## Transakcje

[`MiejscaService`](../src/main/java/com/tomtom/genesis/training/l05dane/MiejscaService.java):

```java
@Service
@Transactional(readOnly = true)        // domyslnie dla kazdej metody
public class MiejscaService {

    @Transactional                     // zapis - nadpisuje readOnly
    public MiejsceDto zmienNazwe(long id, String nowaNazwa) {
        var miejsce = repozytorium.findById(id).orElseThrow(...);
        miejsce.zmienNazwe(nowaNazwa);   // <- i tyle. Bez save()!
        return MiejsceDto.z(miejsce);
    }
}
```

**Dirty checking**: encja pobrana w transakcji jest *zarzadzana*. Przy commicie
Hibernate porownuje ja ze stanem z chwili odczytu i sam wysyla `UPDATE`.
Dowod: test `dirty_checking_zapisuje_zmiane_bez_save`.

Zasady `@Transactional`:

- dziala przez **proxy** - tylko wywolania z zewnatrz beana; `this.metoda()`
  omija transakcje;
- domyslnie wycofuje przy `RuntimeException`, **nie** przy checked exception;
- na metodach `public` (od Spring 6 takze protected/package-private przy proxy klasowym,
  ale trzymaj sie public).

## Open Session in View - wylaczone

`spring.jpa.open-in-view: false`. Domyslnie Boot trzyma sesje Hibernate otwarta
przez cale zadanie HTTP, zeby lazy loading "dzialal" w kontrolerze i Jacksonie.
Koszt: zapytania SQL z warstwy widoku i polaczenie z puli trzymane do konca
zadania. Boot ostrzega o tym w logu. Z DTO budowanym w serwisie nie jest potrzebne.

## Dane startowe

`data.sql` uruchamia sie po utworzeniu tabel, bo
`spring.jpa.defer-datasource-initialization: true`. Dla baz wbudowanych Boot
domyslnie ustawia `ddl-auto=create-drop`. Na produkcji: **Flyway lub Liquibase**
i `ddl-auto=validate` (temat kolejnej czesci).

## Uruchom

```bash
curl -s 'localhost:8080/api/miejsca' | jq                              # strona 0, 5 elementow, sort po nazwie
curl -s 'localhost:8080/api/miejsca?szukaj=hotel&size=1&page=1' | jq
curl -s 'localhost:8080/api/miejsca?sort=miasto,desc&sort=nazwa' | jq
curl -s localhost:8080/api/miejsca/kategoria/MUZEUM | jq
curl -s localhost:8080/api/miejsca/miasto/krakow | jq
curl -s localhost:8080/api/miejsca/statystyki | jq

curl -s -H 'Content-Type: application/json' \
  -d '{"nazwa":"Zamek Krolewski","kategoria":"MUZEUM","miasto":"Warszawa","szerokosc":52.248,"dlugosc":21.015}' \
  localhost:8080/api/miejsca | jq

curl -s -X PUT -H 'Content-Type: application/json' -d '{"nazwa":"Nowa nazwa"}' \
  localhost:8080/api/miejsca/1/nazwa | jq
```

Konsola bazy: <http://localhost:8080/h2-console>, JDBC URL skopiuj z logu
(`Database available at 'jdbc:h2:mem:...'`), uzytkownik `sa`, haslo puste.

## Eksperymenty

1. **Zobacz SQL.** Uruchom z profilem `dev` (`show-sql: true`) albo odkomentuj
   w `application.yml` `org.hibernate.SQL: DEBUG` i `org.hibernate.orm.jdbc.bind: TRACE`.
   Wywolaj stronicowanie - policz zapytania (powinny byc dwa: dane i `count`).
2. **Dirty checking na zywo.** Z wlaczonym SQL wywolaj `PUT /api/miejsca/1/nazwa`.
   Znajdz `update miejsce set ...` - mimo braku `save()`.
3. **Usun `@Transactional`** z `zmienNazwe` i powtorz eksperyment 2.
   `UPDATE` znika: metoda dziedziczy `readOnly = true` z klasy, a w transakcji
   tylko do odczytu Hibernate nie robi flusha. Teraz uruchom
   `MiejscaServiceTest` - **nadal przechodzi**. Dlaczego? `@DataJpaTest` otwiera
   wlasna transakcje (zapisowa), serwis do niej *dolacza*, a `readOnly`
   dolaczajacej metody nic nie zmienia. Test nie zawsze sprawdza to, co myslisz.
4. **Nowa derived query.** Dodaj `List<Miejsce> findTop3ByKategoriaOrderByNazwaDesc(Kategoria k)`
   i test w `MiejsceRepositoryTest`. Potem zrob literowke (`findByKategorja`)
   i uruchom aplikacje.
5. **Projekcja na rekord.** Dodaj `record NazwaIMiasto(String nazwa, String miasto)`
   i metode `List<NazwaIMiasto> findByKategoriaOrderByNazwa(Kategoria k)` -
   Spring Data sam wybierze tylko dwie kolumny (sprawdz w SQL).
6. **Stronicowanie bez count.** Dodaj do repozytorium
   `Slice<Miejsce> findByMiasto(String miasto, Pageable p)` i test. Z wlaczonym
   SQL zobaczysz, ze nie ma zapytania `count` - `Slice` wie tylko, czy jest
   nastepna strona (pobiera o jeden wiersz wiecej).
7. **Rollback.** Dodaj w `utworz` po `save` linie
   `if (nowe.nazwa().equals("BUM")) throw new IllegalStateException("BUM");`.
   Wyslij POST z nazwa `BUM` (dostaniesz 500), potem sprawdz `/statystyki` -
   miejsca nie ma. Teraz dopisz `throws java.io.IOException` do metody (i do metody kontrolera) i rzuc
   `IOException` (checked). Miejsce **zostaje** w bazie: domyslnie rollback
   jest tylko dla wyjatkow niekontrolowanych. Napraw:
   `@Transactional(rollbackFor = Exception.class)`.
8. **Open-in-view.** Ustaw `open-in-view: true` i zajrzyj do logu startowego.

## Pulapki

- `@Transactional` na metodzie prywatnej albo wolanej przez `this` - cicho nie dziala.
- `ddl-auto=create-drop` na prawdziwej bazie = utrata danych.
- `findAll()` bez stronicowania na duzej tabeli.
- Relacje `@OneToMany` i problem N+1 - celowo poza ta lekcja, wrocimy do nich.

Dalej: [Lekcja 6 — Testowanie](06-testy.md)
