# Migracja z Javy 8 na 11 — praktyczna lista problemow

Ten dokument nie opisuje nowych funkcji (to robia [java9.md](java9.md),
[java10.md](java10.md), [java11.md](java11.md)). Opisuje **co sie psuje**
przy podnoszeniu istniejacego projektu — w kolejnosci, w jakiej zwykle boli.

Dobra wiadomosc na poczatek: **nie musisz uzywac modulow.** Aplikacja moze dalej
startowac z classpath i dzialac jak w Javie 8. Wiekszosc projektow tak wlasnie robi.

---

## 1. Usuniete moduly Java EE i CORBA (JEP 320)

Najczestszy pierwszy blad kompilacji:

```
error: package javax.xml.bind does not exist
```

Te API nie zniknely ze swiata — zniknely z **JDK**. Trzeba je dodac jako zwykle
zaleznosci.

| Brakujacy pakiet | Zaleznosc Maven |
|---|---|
| `javax.xml.bind` (JAXB) | `jakarta.xml.bind:jakarta.xml.bind-api` + `org.glassfish.jaxb:jaxb-runtime` |
| `javax.activation` | `jakarta.activation:jakarta.activation-api` |
| `javax.annotation` (`@PostConstruct`) | `jakarta.annotation:jakarta.annotation-api` |
| `javax.xml.ws` (SOAP) | `jakarta.xml.ws:jakarta.xml.ws-api` + `com.sun.xml.ws:jaxws-rt` |
| `javax.transaction` | `jakarta.transaction:jakarta.transaction-api` |
| CORBA (`org.omg.*`) | brak nastepcy w JDK — `org.glassfish.corba` albo przepisanie |

> **Wazne rozroznienie:** dla Javy 11 wybieraj wersje z pakietami `javax.*`
> (Jakarta EE 8, np. `jakarta.xml.bind-api:2.3.x`). Jakarta EE 9+ przeniosla
> pakiety na `jakarta.*`, co wymaga zmian w kodzie. To osobna migracja —
> nie mieszaj jej z przejsciem na Jave 11.

---

## 2. `--illegal-access` i dostep do wnetrza JDK

To drugi najczestszy problem, bo dotyka **bibliotek**, nie twojego kodu.

Java 9 zamknela wewnetrzne pakiety JDK (`sun.misc`, `sun.nio.ch`, `jdk.internal.*`).
Zeby nie zabic calego ekosystemu naraz, wprowadzono tryb przejsciowy
`--illegal-access`, ktory w Javie 9-15 mial domyslnie wartosc `permit`:
dostep dziala, ale JVM wypisuje ostrzezenie:

```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.example.Foo to field java.lang.String.value
WARNING: All illegal access operations will be denied in a future release
```

**Na Javie 11 to tylko ostrzezenie i aplikacja dziala.** Ale to ostrzezenie jest
zapowiedzia — w Javie 16 domyslna wartoscia stalo sie `deny`, a w Javie 17
flage usunieto calkiem. Jesli planujesz kiedys isc dalej niz 11, potraktuj te
ostrzezenia jako liste zadan.

Gdy trzeba otworzyc dostep jawnie:

```bash
--add-opens java.base/java.lang=ALL-UNNAMED      # dostep refleksyjny (setAccessible)
--add-exports java.base/sun.nio.ch=ALL-UNNAMED   # dostep przy kompilacji i runtime
```

Te flagi trzeba podac **wszedzie**, gdzie startuje JVM: `java`, Surefire/Failsafe
w testach, `MAVEN_OPTS`, konfiguracja IDE, `ENTRYPOINT` w Dockerze.
Zapomniany Surefire to klasyk — aplikacja wstaje, testy nie.

### `sun.misc.Unsafe`

Nadal dziala na Javie 11 (jest w `jdk.unsupported`), ale kazde uzycie to dlug
techniczny. Nastepca to `VarHandle` (Java 9) i `MethodHandles`.

---

## 3. Zmiana formatu numeru wersji (JEP 223)

```
Java 8:  java.version = 1.8.0_382     java.specification.version = 1.8
Java 11: java.version = 11.0.21       java.specification.version = 11
```

Kod parsujacy wersje po kropkach albo zakladajacy prefiks `1.` przestaje dzialac.
Dotyczy to takze bibliotek — szczegolnie starych wersji ASM, cglib, Groovy
i skryptow budujacych.

Nowe API do sprawdzania wersji:

```java
Runtime.Version v = Runtime.version();   // Java 9+
v.feature();                             // 11
```

---

## 4. Wersje narzedzi i bibliotek

Bardzo czesty scenariusz: kod jest w porzadku, ale **build sie wywala**, bo
narzedzie nie rozumie nowego bajtkodu.

| Narzedzie | Minimalna sensowna wersja dla Javy 11 |
|---|---|
| Maven | 3.5.4+ (najlepiej 3.8+) |
| maven-compiler-plugin | 3.8+ (dla `<release>`) |
| maven-surefire-plugin | 2.22+ |
| Gradle | 5.0+ (najlepiej 7+) |
| ASM | 7.0+ |
| cglib / Byte Buddy | Byte Buddy 1.9+, cglib praktycznie martwy |
| Mockito | 2.23+ |
| Lombok | 1.18.4+ |
| Spring Framework | 5.1+ |
| Spring Boot | 2.1+ |
| Hibernate | 5.3+ |
| Jacoco | 0.8.2+ |
| Groovy | 2.5.4+ / 3.x |

### `release` zamiast `source`/`target`

```xml
<properties>
    <maven.compiler.release>11</maven.compiler.release>
</properties>
```

`source`/`target` mowia tylko o skladni i wersji bajtkodu — nie chronia przed
uzyciem API, ktorego w docelowej wersji nie ma. `release` (JEP 247, Java 9)
kompiluje **wobec sygnatur tej konkretnej wersji JDK**. Z `source/target=8`
na JDK 11 mozesz nieswiadomie wywolac `List.of` i dostac `NoSuchMethodError`
dopiero na produkcji z Java 8. Z `release=8` kompilator odmowi od razu.

---

## 5. Split packages

Jesli ten sam pakiet dostarczaja dwa JAR-y (klasyk: `javax.annotation`
z `jsr305` i z `javax.annotation-api`), na classpath dziala to jak dawniej,
ale na module path konczy sie bledem:

```
error: module X reads package javax.annotation from both Y and Z
```

Kolejny powod, zeby przy pierwszej migracji zostac na classpath.

---

## 6. G1 domyslnym garbage collectorem

Java 8 uzywala Parallel GC, Java 9+ uzywa G1. To zmiana **charakterystyki
wydajnosci**, nie bledu:

- G1 daje krotsze i bardziej przewidywalne pauzy,
- Parallel GC daje wyzsza przepustowosc przy malych stertach.

Aplikacje wsadowe, ktorym zalezy na przepustowosci, potrafia po migracji
zwolnic o kilka procent. Jesli to boli, mozna wrocic: `-XX:+UseParallelGC`.

Przy okazji: wszystkie flagi logowania GC z Javy 8 (`-XX:+PrintGCDetails`,
`-Xloggc:`) sa **usuniete**. Nowa skladnia (JEP 158/271):

```bash
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M
```

JVM ze starymi flagami **nie wstanie** — to czesta przyczyna "kontener nie
startuje po podbiciu wersji".

---

## 7. Kontenery i pamiec

Dobra zmiana, o ktorej warto wiedziec. Na Javie 8 (przed 8u191) JVM **nie
widziala limitow cgroups** — w kontenerze z `-m 512m` ustawiala sobie sterte
wedlug pamieci calego hosta i ginela od OOM-killera. Obejsciem bylo jawne
`-Xmx`.

Od Javy 10 (`-XX:+UseContainerSupport`, domyslnie wlaczone) JVM czyta limity
kontenera. Sensowna konfiguracja to dzis:

```bash
-XX:MaxRAMPercentage=75.0
```

zamiast sztywnego `-Xmx`. Sterta skaluje sie razem z limitem poda.

---

## 8. Certyfikaty w OpenJDK

Do Javy 10 OpenJDK mial **pusty** `cacerts`. Objaw: HTTPS dziala na Oracle JDK,
a na OpenJDK rzuca bledem walidacji certyfikatu. Java 10 (JEP 319) to naprawila.
Jesli migrujesz z Oracle JDK 8 na OpenJDK 11 — ten problem cie nie dotknie,
ale warto go kojarzyc, bo krazy w starych instrukcjach.

---

## 9. Drobiazgi, ktore lapia w praktyce

- **`_` przestal byc poprawnym identyfikatorem** (Java 9). `int _ = 5;` to blad.
- **Zmiany w formatowaniu dat.** Java 9 przeszla na dane lokalizacyjne CLDR
  zamiast wlasnych. Skutek: `SimpleDateFormat`/`DateTimeFormatter` potrafia
  wypisac inny separator albo inna nazwe miesiaca. Powrot: `-Djava.locale.providers=COMPAT,CLDR`.
- **`Class.newInstance()` jest `@Deprecated`** — zamiennik to
  `getDeclaredConstructor().newInstance()` (poprawnie propaguje wyjatki konstruktora).
- **`Thread.stop()`, `Thread.destroy()`, finalizery** — dalsza deprecjacja.
- **JavaFX nie jest juz czescia JDK** — od Javy 11 to osobny projekt OpenJFX
  z wlasnymi zaleznosciami.
- **Java Mission Control nie jest juz czescia JDK** — pobiera sie osobno.
- **`javah` usuniete** — naglowki JNI generuje `javac -h`.

---

## 10. Kolejnosc dzialan, ktora sie sprawdza

1. **Zostan na Javie 8 jako `release`, ale buduj JDK 11.** Najpierw podbij
   narzedzia i biblioteki, nie dotykajac kodu. Jesli build przechodzi —
   polowa pracy za toba.
2. **Puscic `jdeps`** na zbudowanych artefaktach:
   ```bash
   jdeps --jdk-internals --multi-release 11 mojaAplikacja.jar
   ```
   Dostaniesz liste uzyc wewnetrznych API JDK, razem z sugestiami zamiennikow.
3. **Podbij `release` do 11.** Teraz wyjda brakujace pakiety Java EE — dodaj
   zaleznosci z tabeli w punkcie 1.
4. **Uruchom testy i obejrzyj ostrzezenia `Illegal reflective access`.**
   Kazde wskazuje biblioteke do podbicia.
5. **Podbij flagi JVM w skryptach startowych i Dockerfile** — logowanie GC,
   `MaxRAMPercentage`, ewentualne `--add-opens`.
6. **Zostan na classpath.** Moduly rozwazaj jako osobny projekt, jesli w ogole.

---

## Czego ten dokument nie obejmuje

Migracji **z 11 dalej** (17, 21, 25). Tam glowne tematy to: ostateczne
zamkniecie `--illegal-access` (17), usuniecie Nashorna i apletow (15/17),
przejscie `javax.*` → `jakarta.*` w Spring Boot 3 (to nie jest zmiana Javy,
tylko Jakarta EE 9), silna hermetyzacja `sun.misc.Unsafe` i wirtualne watki (21).

To material na osobne opracowanie — daj znac, jesli ma powstac.
