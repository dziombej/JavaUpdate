# Java 9 — najwieksza zmiana od wprowadzenia generykow

Wrzesien 2017, ponad trzy lata po Javie 8. Ostatnie wydanie w starym trybie
"duza wersja co kilka lat". Po Javie 9 przeszlismy na cykl polroczny.

Java 9 to tak naprawde dwie osobne historie: **moduly** (ogromna, ryzykowna zmiana
architektury platformy) i **reszta** (kilkanascie wygodnych dodatkow do API).
W codziennej pracy uzywasz "reszty". Ale to moduly tlumacza wiekszosc bolu
przy migracji z Javy 8 — o tym jest [migracja-8-11.md](migracja-8-11.md).

---

## 1. JPMS — system modulow (JEP 261)

**Kod:** [`examples/jpms-demo/`](../examples/jpms-demo) — uruchom `./run.sh`

### Problem, ktory rozwiazuje

Przed Java 9 platforma miala trzy nieprzyjemne wlasciwosci:

1. **`public` znaczylo "publiczny dla calego swiata".** Nie dalo sie napisac klasy
   publicznej widocznej tylko wewnatrz biblioteki. Kazdy szczegol implementacyjny
   byl czescia API, czy tego chciales, czy nie.
2. **Classpath byl plaska lista bez struktury.** JVM nie wiedzial, czego brakuje,
   dopoki nie probowal zaladowac klasy — stad `NoClassDefFoundError` w polowie
   dzialania aplikacji, a nie przy starcie.
3. **JDK byl monolitem.** `rt.jar` wazyl ponad 60 MB i ladowal sie w calosci,
   nawet w aplikacji, ktora nie dotyka Swinga ani CORBY.

### Jak to dziala

Modul to nazwana jednostka z jawna deklaracja w pliku `module-info.java`
w korzeniu katalogu ze zrodlami:

```java
module com.tomtom.przyklad.uslugi {
    exports com.tomtom.przyklad.uslugi.api;   // ten pakiet widza inni
    // pakiet ...impl nie jest wymieniony, wiec dla innych modulow NIE ISTNIEJE
}
```

```java
module com.tomtom.przyklad.aplikacja {
    requires com.tomtom.przyklad.uslugi;      // jawna zaleznosc
}
```

Kluczowe slowa kluczowe:

| Deklaracja | Znaczenie |
|---|---|
| `requires X` | potrzebuje modulu X do kompilacji i uruchomienia |
| `requires transitive X` | jw., a dodatkowo kazdy, kto wymaga mnie, dostaje tez X |
| `requires static X` | potrzebny przy kompilacji, opcjonalny w runtime |
| `exports P` | pakiet P jest dostepny dla wszystkich modulow |
| `exports P to M` | pakiet P jest dostepny tylko dla modulu M |
| `opens P` | pakiet P otwarty na **refleksje** (Spring, Jackson, Hibernate) |
| `opens P to M` | jw., ale tylko dla M |
| `uses S` / `provides S with I` | mechanizm `ServiceLoader` zadeklarowany w module |

Roznica `exports` vs `opens` jest istotna: `exports` daje dostep w czasie
kompilacji i runtime, ale **tylko do skladowych publicznych**. `opens` daje
peny dostep refleksyjny, lacznie z `setAccessible(true)` na polach prywatnych —
i nie daje dostepu przy kompilacji. Frameworki ORM i DI potrzebuja `opens`.

### Co zobaczysz uruchamiajac demo

`./run.sh` pokazuje piec rzeczy po kolei:

1. kompilacje dwoch modulow (`javac --module-source-path`),
2. dzialajacy dostep do pakietu wyeksportowanego,
3. **prawdziwy komunikat kompilatora** przy probie importu pakietu nieeksportowanego:
   ```
   error: package com.tomtom.przyklad.uslugi.impl is not visible
     (package ... is declared in module ..., which does not export it)
   ```
4. ten sam kod uruchomiony z `-cp` zamiast `--module-path` — i tu hermetyzacja
   **znika calkowicie**, refleksja przechodzi, a `getModule().getName()` zwraca `null`,
5. narzedzia: `--describe-module`, `--list-modules`, `jdeps`.

Punkt 4 jest najwazniejszy praktycznie: **sama obecnosc `module-info.java` nic nie
daje, jesli aplikacja startuje z classpath.** Moduly wlaczaja sie dopiero na
`--module-path`. Dlatego ogromna wiekszosc projektow, ktore "przeszly na Jave 11",
nie uzywa modulow wcale — i to jest w porzadku.

### Modul nienazwany i modul automatyczny

Dwa pojecia, ktore trzeba znac przy migracji:

- **Modul nienazwany** (*unnamed module*) — wszystko, co siedzi na classpath.
  Czyta kazdy inny modul, eksportuje i otwiera wszystkie swoje pakiety.
  To furtka wstecznej kompatybilnosci.
- **Modul automatyczny** (*automatic module*) — zwykly JAR bez `module-info`,
  ale polozony na `--module-path`. Dostaje nazwe z manifestu
  (`Automatic-Module-Name`) albo wyprowadzona z nazwy pliku, czyta wszystko
  i eksportuje wszystko. Pozwala migrowac stopniowo.

### Kiedy warto uzywac modulow

Szczerze: rzadko. Warto, gdy:
- budujesz biblioteke i chcesz twardo oddzielic API od implementacji,
- budujesz obraz runtime przez `jlink` (male kontenery, CLI),
- pracujesz nad samym JDK.

Nie warto, gdy masz typowa aplikacje Springowa z 200 zaleznosciami —
koszt (`opens` dla kazdego pakietu z encjami, biblioteki bez `Automatic-Module-Name`,
split packages) zwykle przewyzsza zysk.

---

## 2. Fabryki kolekcji niemodyfikowalnych (JEP 269)

**Kod:** [`jdk9/CollectionFactories.java`](../src/main/java/com/tomtom/genesis/platform/jdk9/CollectionFactories.java)

```java
List<String> lista = List.of("a", "b", "c");
Set<Integer>  zbior = Set.of(1, 2, 3);
Map<String, Integer> mapa = Map.of("a", 1, "b", 2);
Map<String, Integer> duza = Map.ofEntries(Map.entry("a", 1), Map.entry("b", 2));
```

Przedtem trzeba bylo pisac `Collections.unmodifiableList(Arrays.asList(...))`
albo siegac po Guave (`ImmutableList.of`).

### Trzy rzeczy, o ktorych trzeba wiedziec

**1. To nie jest `Collections.unmodifiableList`.** Tamto zwraca *widok* — jesli
ktos trzyma referencje do listy zrodlowej, "niemodyfikowalna" kolekcja zmienia sie
pod spodem. `List.of` i `List.copyOf` to realne kopie:

```
stan poczatkowy:
  zrodlo = [a, b], widok = [a, b], kopia = [a, b]
po zrodlo.add("NOWY"):
  zrodlo = [a, b, NOWY]
  widok  = [a, b, NOWY]   <- ZMIENIL SIE, bo to widok na zrodlo
  kopia  = [a, b]         <- bez zmian, bo to kopia
```

**2. `null` jest zabroniony wszedzie.** `List.of("a", null)` rzuca `NullPointerException`,
`Set.of("a", "a")` rzuca `IllegalArgumentException`. Zaskakujaco:
`List.of("a").contains(null)` **tez rzuca NPE** — to czesta przyczyna bledow
przy podmianie `Arrays.asList` na `List.of` w istniejacym kodzie.

**3. Kolejnosc iteracji `Set.of`/`Map.of` jest losowa i zmienia sie miedzy
uruchomieniami JVM.** `ImmutableCollections` uzywa ziarna (`SALT`) losowanego raz
przy starcie. To celowe — ma zapobiec pisaniu kodu zaleznego od kolejnosci.
Praktyczny skutek: test porownujacy `toString()` takiego zbioru bedzie losowo
czerwony. Uruchom przyklad kilka razy, zeby to zobaczyc.

### Dlaczego az 11 przeciazonych metod

`List.of` ma warianty od 0 do 10 argumentow plus varargs. Powod jest
wydajnosciowy: wariant varargs alokuje tablice, warianty stalo-argumentowe nie.
Dodatkowo dla 1 i 2 elementow JDK uzywa specjalnej klasy `List12`, ktora trzyma
elementy w polach zamiast w tablicy.

---

## 3. Rozszerzenia Stream API

**Kod:** [`jdk9/StreamApi9.java`](../src/main/java/com/tomtom/genesis/platform/jdk9/StreamApi9.java)

### `takeWhile` / `dropWhile`

```java
List.of(2, 4, 6, 7, 8, 10).stream().takeWhile(n -> n % 2 == 0)  // [2, 4, 6]
List.of(2, 4, 6, 7, 8, 10).stream().dropWhile(n -> n % 2 == 0)  // [7, 8, 10]
```

To **nie jest** `filter`. `filter` przeglada caly strumien i wybiera pasujace
(`[2, 4, 6, 8, 10]`). `takeWhile` przerywa przy pierwszym niepasujacym.
Dzieki temu dziala na strumieniach nieskonczonych:

```java
Stream.iterate(1, n -> n * 2).takeWhile(n -> n < 100)  // [1, 2, 4, 8, 16, 32, 64]
```

**Pulapka:** semantyka "dopoki" ma sens tylko dla strumieni **uporzadkowanych**.
Na strumieniu z `HashSet` albo rownoleglym wynik jest niedeterministyczny —
specyfikacja pozwala wtedy zwrocic dowolny pasujacy prefiks.

### `Stream.iterate` z warunkiem stopu

```java
Stream.iterate(1, i -> i <= 5, i -> i + 1)   // dokladny odpowiednik for (i = 1; i <= 5; i++)
Stream.iterate(1024, n -> n > 1, n -> n / 2) // 1024 -> 512 -> ... -> 2
```

Wczesniej trzeba bylo skladac `iterate` z `limit` albo `takeWhile`.

### `Stream.ofNullable`

Zwraca strumien pusty dla `null`, jednoelementowy dla nie-`null`.
Idealne do `flatMap` po mapie, gdzie klucza moze nie byc:

```java
.flatMap(klucz -> Stream.ofNullable(konfiguracja.get(klucz)))
```

### `Collectors.filtering` i `Collectors.flatMapping`

Roznica `filtering` wobec zwyklego `filter` jest subtelna, ale realna:

```java
// filtering - filtruje PO pogrupowaniu, puste grupy zostaja
{backend=[Bartek], frontend=[]}

// filter przed groupingBy - grupa frontend znika calkiem
{backend=[Bartek]}
```

Jesli raport ma pokazac wszystkie dzialy, takze te bez wynikow — potrzebujesz
`filtering`.

---

## 4. Optional: `or`, `ifPresentOrElse`, `stream`

**Kod:** [`jdk9/OptionalApi9.java`](../src/main/java/com/tomtom/genesis/platform/jdk9/OptionalApi9.java)

```java
Optional<String> wynik = zKonfiguracji()
        .or(this::zeZmiennejSrodowiskowej)
        .or(this::zDomyslnych);          // lancuch zrodel, leniwy
```

`or` rozni sie od `orElseGet` typem zwracanym: `orElseGet` zwraca `T`, `or`
zwraca `Optional<T>` — dlatego da sie lancuchowac. Kazdy `Supplier` jest
wywolywany tylko wtedy, gdy poprzedni zwrocil puste.

```java
opt.ifPresentOrElse(v -> loguj(v), () -> loguj("brak"));   // if/else bez isPresent()
```

```java
strumien.map(this::parsuj).flatMap(Optional::stream)       // zamiast
strumien.map(this::parsuj).filter(Optional::isPresent).map(Optional::get)
```

---

## 5. Prywatne metody w interfejsach

**Kod:** [`jdk9/PrivateInterfaceMethods.java`](../src/main/java/com/tomtom/genesis/platform/jdk9/PrivateInterfaceMethods.java)

Domkniecie tego, co zaczela Java 8. Chronologia:

| Wersja | Co interfejs moze zawierac |
|---|---|
| do 7 | metody abstrakcyjne, stale |
| 8 | + `default`, + `static` (oba publiczne) |
| 9 | + `private`, + `private static` |

Problem z Javy 8: dwie metody `default` dzielace logike musialy ja albo
duplikowac, albo wystawic pomocnicza metode jako publiczne API interfejsu.
Od Javy 9 mozna ja schowac.

Ograniczenia: metoda `private` musi miec cialo, nie da sie jej nadpisac
w klasie implementujacej, a interfejs nadal nie ma pol instancyjnych.

---

## 6. try-with-resources na istniejacej zmiennej (JEP 213)

**Kod:** [`jdk9/TryWithResources9.java`](../src/main/java/com/tomtom/genesis/platform/jdk9/TryWithResources9.java)

```java
Polaczenie p = pobierzZPuli();
try (p) {            // Java 9 - wystarczy, ze p jest final albo efektywnie final
    p.uzyj();
}
```

W Javie 7/8 trzeba bylo pisac sztuczne `try (Polaczenie q = p)`, co tworzylo
druga nazwe na ten sam obiekt.

Dwie rzeczy warte zapamietania: zasoby sa zamykane w **odwrotnej** kolejnosci
niz otwierane, a zasob `null` jest po cichu pomijany przy zamykaniu
(`try (brak)` gdzie `brak == null` nie rzuca NPE).

---

## 7. Process API (JEP 102)

**Kod:** [`jdk9/ProcessApiDemo.java`](../src/main/java/com/tomtom/genesis/platform/jdk9/ProcessApiDemo.java)

Przed Java 9 nie dalo sie w standardowym API pobrac wlasnego PID-u. Idiom brzmial:

```java
ManagementFactory.getRuntimeMXBean().getName().split("@")[0]   // "12345@hostname"
```

Teraz:

```java
ProcessHandle ja = ProcessHandle.current();
ja.pid();                                    // 21482
ja.info().command();                         // Optional<String>
ja.info().totalCpuDuration();                // Optional<Duration>
ja.parent();                                 // Optional<ProcessHandle>
ProcessHandle.allProcesses();                // Stream<ProcessHandle> - procesy systemu
proces.children() / proces.descendants();    // drzewo procesow
```

Wszystkie pola `ProcessHandle.Info` sa `Optional`, bo system operacyjny nie musi
ich udostepniac — nie zakladaj, ze `command()` zawsze cos zwroci.

Najbardziej praktyczny dodatek to `onExit()`, ktore zwraca
`CompletableFuture<Process>` — mozna zareagowac na zakonczenie procesu bez
blokowania watku na `waitFor()`.

---

## 8. StackWalker (JEP 259)

**Kod:** [`jdk9/StackWalkerDemo.java`](../src/main/java/com/tomtom/genesis/platform/jdk9/StackWalkerDemo.java)

```java
StackWalker.getInstance().walk(strumien -> strumien.limit(2).collect(toList()));
StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass();
```

Problem ze starym API: `new Throwable().getStackTrace()` **zawsze** materializuje
caly slad stosu do tablicy `StackTraceElement[]`, nawet gdy potrzebujesz jednej
ramki. StackWalker chodzi po stosie leniwie.

Pomiar z przykladu (JDK 25, 20 000 powtorzen, z rozgrzewka):

```
glebokosc stosu ~  9 ramek | Throwable.getStackTrace:   41,0 ms | StackWalker.limit(1):  26,6 ms
glebokosc stosu ~206 ramek | Throwable.getStackTrace:  922,3 ms | StackWalker.limit(1):  14,9 ms
```

Na plytkim stosie roznica jest niewielka. Na glebokim — dwa rzedy wielkosci,
bo koszt starego API rosnie liniowo z glebokoscia, a StackWalkera nie.
Dlatego Log4j 2 (od 2.9) przeszlo na StackWalkera: w aplikacji Springowej stos
ma czesto 100+ ramek, a do zalogowania nazwy klasy potrzebna jest jedna.

**Wazne ograniczenie:** `Stream<StackFrame>` przekazany do `walk` jest wazny
**tylko w trakcie tego wywolania**. Nie wolno go zwrocic ani zapisac do pola —
ramki sa pobierane leniwie z zywego stosu.

---

## 9. Reszta, ktora warto znac z nazwy

| Zmiana | Na czym polega |
|---|---|
| **JShell** (JEP 222) | REPL w JDK. Uruchom `jshell` i wklejaj fragmenty kodu — najszybszy sposob sprawdzenia, jak zachowuje sie jakies API |
| **Diament z klasa anonimowa** | `new Comparator<>() { ... }` — wczesniej trzeba bylo wypisac typ |
| **Kompaktowe Stringi** (JEP 254) | `String` trzyma `byte[]` zamiast `char[]`; teksty ASCII zajmuja polowe pamieci. Zero zmian w kodzie, sam zysk |
| **G1 domyslnym GC** (JEP 248) | Zmiana z Parallel GC. Inne charakterystyki pauz — patrz [migracja-8-11.md](migracja-8-11.md) |
| **`@Deprecated(since, forRemoval)`** | Adnotacja dostala pola; `forRemoval = true` daje ostrzezenie w kazdym uzyciu |
| **`Flow`** (JEP 266) | Interfejsy Reactive Streams w `java.util.concurrent.Flow` — `Publisher`, `Subscriber`, `Processor`, `Subscription`. Sam JDK nie dostarcza implementacji (poza HttpClientem) |
| **`VarHandle`** (JEP 193) | Typowany, bezpieczny zamiennik `sun.misc.Unsafe` do operacji atomowych i dostepu z barierami pamieci |
| **`jlink`** (JEP 282) | Budowanie obrazu runtime zawierajacego tylko potrzebne moduly. Podstawa malych obrazow kontenerowych |
| **Wieloversyjne JAR-y** (JEP 238) | Jeden JAR moze zawierac rozne wersje klasy dla roznych wersji Javy (`META-INF/versions/11/`) |
| **Ujednolicone logowanie JVM** (JEP 158) | `-Xlog:gc*:file=gc.log` zamiast kilkunastu osobnych flag `-XX:+PrintGC...` |
| **`_` przestal byc identyfikatorem** | `int _ = 5;` to blad kompilacji (przygotowanie pod pozniejsze uzycie `_` jako wzorca) |

---

## Kolejny krok

[java10.md](java10.md) — `var` i domkniecie tematu kolekcji niemodyfikowalnych.
