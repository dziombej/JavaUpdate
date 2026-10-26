# Java 11 — pierwsza wersja LTS po Javie 8

Wrzesien 2018. To jest **ta wersja**, do ktorej migrowala wiekszosc firm
schodzacych z Javy 8 — bo byla pierwszym wydaniem dlugoterminowego wsparcia
w nowym modelu.

Sama Java 11 wnosi mniej niz 9, ale robi trzy rzeczy o duzym znaczeniu:
standaryzuje **HttpClient**, porzadkuje **String**, i — najbolesniej —
**usuwa z JDK Java EE oraz CORBE**.

---

## 1. HttpClient jako standardowe API (JEP 321)

**Kod:** [`jdk11/HttpClientDemo.java`](../src/main/java/com/tomtom/genesis/platform/jdk11/HttpClientDemo.java)

Przyklad uruchamia **wlasny serwer HTTP w tym samym procesie**
(`com.sun.net.httpserver`), wiec dziala bez dostepu do internetu i daje
powtarzalne wyniki.

### Historia

| Wersja | Stan |
|---|---|
| 9 | inkubator, pakiet `jdk.incubator.http` |
| 10 | nadal inkubator, zmiany w API |
| 11 | **standard**, pakiet `java.net.http`, modul `java.net.http` |

### Dlaczego powstal

`HttpURLConnection` pochodzi z Javy 1.1 (1997) i ma wady nie do naprawienia:
projektowany pod protokoly, ktorych juz nie ma (gopher, ftp), tylko tryb
blokujacy, brak HTTP/2 i WebSocket, a konfiguracja odbywa sie przez settery,
tresc przez strumienie, natomiast kody bledow przez `getErrorStream()` zamiast
`getInputStream()`. Efekt: kazdy projekt dokladal Apache HttpClient albo OkHttp.

### Trzy filary API

```java
HttpClient klient = HttpClient.newHttpClient();          // dlugozyjacy, thread-safe

HttpRequest zapytanie = HttpRequest.newBuilder()         // niezmienny
        .uri(URI.create("https://example.com"))
        .header("Accept", "application/json")
        .GET()
        .build();

HttpResponse<String> odpowiedz =                          // typ zalezy od BodyHandlera
        klient.send(zapytanie, HttpResponse.BodyHandlers.ofString());
```

`HttpClient` trzyma pule polaczen — **twórz jeden na aplikacje**, nie jeden na
zapytanie.

**BodyHandlers** (odpowiedz): `ofString()`, `ofByteArray()`, `ofLines()`,
`ofFile(Path)`, `ofInputStream()`, `discarding()`.
**BodyPublishers** (zapytanie): `ofString()`, `ofByteArray()`, `ofFile(Path)`, `noBody()`.

### Wersja asynchroniczna

```java
klient.sendAsync(zapytanie, BodyHandlers.ofString())
      .thenApply(HttpResponse::body)
      .thenApply(this::parsuj)
      .exceptionally(e -> awaryjnaWartosc());
```

`sendAsync` zwraca `CompletableFuture`, wiec wpina sie w cale API z Javy 8.
Kilka zapytan rownolegle:

```java
CompletableFuture.allOf(przyszle.toArray(new CompletableFuture[0])).join();
```

W przykladzie trzy zapytania, z ktorych jedno trwa 300 ms, koncza sie lacznie
w 311 ms — czyli faktycznie rownolegle.

### Cztery pulapki

1. **Kod 4xx/5xx NIE jest wyjatkiem.** `send` zwraca normalna odpowiedz ze
   statusem 503. Status trzeba sprawdzic samemu — inaczej potraktujesz strone
   bledu jako poprawna tresc.
2. **`followRedirects` domyslnie to `NEVER`.** Serwer odpowiada 301, a ty
   dostajesz 301 zamiast zawartosci docelowej. Ustaw `Redirect.NORMAL`.
3. **Dwa rozne timeouty.** `HttpClient.connectTimeout` to czas na nawiazanie
   polaczenia TCP; `HttpRequest.timeout` to czas na cala wymiane
   (po nim `HttpTimeoutException`). Brak drugiego = zapytanie moze wisiec w nieskonczonosc.
4. **W `exceptionally`/`handle` wyjatek jest opakowany w `CompletionException`.**
   Prawdziwa przyczyna siedzi w `getCause()`. Przyklad pokazuje oba poziomy.

### Czego HttpClient nie ma

Wbudowanego JSON-a (potrzebny Jackson/Gson), automatycznych ponowien,
circuit breakera, cache. Do tego nadal potrzebna biblioteka. Dla prostych
wywolan REST-owych — wystarcza w zupelnosci.

---

## 2. Nowe metody `String`

**Kod:** [`jdk11/StringApi11.java`](../src/main/java/com/tomtom/genesis/platform/jdk11/StringApi11.java)

```java
"   ".isBlank()          // true  - pusty albo same biale znaki
"  x  ".strip()          // "x"   - swiadome Unicode
"  x  ".stripLeading()   // "x  "
"  x  ".stripTrailing()  // "  x"
"a\nb".lines()           // Stream<String> - leniwy
"-".repeat(40)           // "----------------------------------------"
```

### `strip` kontra `trim` — to nie to samo

To najczesciej mylona para w calej Javie 11.

- **`trim()`** usuwa znaki o kodzie `<= U+0020`. Definicja z 1995 roku, sprzed
  powszechnego Unicode.
- **`strip()`** usuwa wszystko, co `Character.isWhitespace` uznaje za bialy znak.

Roznica jest realna. Znak `U+2000` (EN QUAD) to odstep o kodzie **wiekszym** niz
spacja, wiec `trim()` go nie rusza:

```
wejscie (z U+2000) : [  Ala ma kota  ]
trim()             : [  Ala ma kota  ]   <- nic nie usunal
strip()            : [Ala ma kota]
```

Tekst wklejony z Worda, PDF-a albo strony WWW regularnie zawiera takie znaki.
**W nowym kodzie uzywaj `strip()`.**

Dodatkowo `strip` ma warianty jednostronne (`stripLeading`, `stripTrailing`),
ktorych `trim` nigdy nie mial — trzeba bylo pisac regex.

### `isBlank` a `null`

```java
s.isBlank()                     // NPE, jesli s == null
StringUtils.isBlank(null)       // true (Apache Commons Lang)
```

To metoda instancyjna, wiec **nie zastapi** `StringUtils.isBlank` jeden do
jednego. Przy podmianie sprawdz, czy kod nie polegal na obsludze `null`.

### `lines()` kontra `split("\n")`

`lines()` rozpoznaje wszystkie trzy konwencje koncow linii (`\n`, `\r\n`, `\r`),
nie kompiluje wyrazenia regularnego i dziala **leniwie** — mozna wziac dwie
pierwsze linie z ogromnego tekstu bez przetwarzania reszty.

### `repeat(n)`

Przedtem: `new String(new char[n]).replace('\0', '-')` albo
`String.join("", Collections.nCopies(n, "-"))`. `repeat` jest tez szybsze —
alokuje docelowa tablice raz i kopiuje blokami. `repeat(-1)` rzuca
`IllegalArgumentException`, `repeat(0)` zwraca pusty lancuch.

---

## 3. `Files.readString` / `Files.writeString` / `Path.of`

**Kod:** [`jdk11/FilesApi11.java`](../src/main/java/com/tomtom/genesis/platform/jdk11/FilesApi11.java)

```java
Files.writeString(plik, "tresc");
String tresc = Files.readString(plik);
Path p = Path.of("katalog", "plik.txt");
```

Cala klasyczna petla z `BufferedReader` i `StringBuilder` zwija sie do jednej linii.

### Kodowanie: to wazniejsze, niz wyglada

`readString`/`writeString` uzywaja **zawsze UTF-8**, niezaleznie od
`file.encoding` systemu. To istotna roznica wobec starego `new FileReader(plik)`,
ktory az do Javy 18 uzywal domyslnego kodowania platformy i potrafil dac inny
wynik na Windows niz na Linuksie. Jesli kiedys scigales blad "polskie znaki
psuja sie tylko na produkcji" — to byla ta przyczyna.

### Ograniczenie

`readString` wczytuje **caly** plik do pamieci i rzuca `OutOfMemoryError`
dla plikow powyzej ok. 2 GB (limit dlugosci `String`). Do duzych plikow uzywaj
`Files.lines`.

### `Path.of` a `Paths.get`

Ta sama funkcjonalnosc. `Paths.get` jest teraz cienkim opakowaniem na `Path.of`.
Powod zmiany: klasa `Paths` istniala wylacznie po to, zeby pomiescic metode
fabryczna. Od Javy 9 interfejsy moga miec metody `static`, wiec fabryka trafila
tam, gdzie jej miejsce — do samego `Path`. `Paths` nie jest `@Deprecated`,
ale w nowym kodzie uzywaj `Path.of`.

### Trzy sposoby czytania linii — kiedy ktory

| Metoda | Pamiec | Kiedy |
|---|---|---|
| `Files.readAllLines` | caly plik | male pliki, potrzebna lista |
| `Files.readString().lines()` | caly plik | male pliki, przetwarzanie strumieniowe |
| `Files.lines` | leniwa | **duze pliki** — ale wymaga `try-with-resources`! |

`Files.lines` trzyma otwarty deskryptor pliku. To jeden z nielicznych strumieni,
ktore trzeba zamykac — bez `try-with-resources` deskryptory wyciekaja.

---

## 4. `var` w parametrach lambdy (JEP 323)

**Kod:** [`jdk11/VarInLambda.java`](../src/main/java/com/tomtom/genesis/platform/jdk11/VarInLambda.java)

```java
(var x, var y) -> x + y
```

Java 10 dala `var` dla zmiennych lokalnych, ale celowo pominela lambdy.
Java 11 domyka luke. Bajtkod jest identyczny jak dla `(x, y) -> x + y` —
**jedynym realnym powodem istnienia tej funkcji sa adnotacje**:

```java
// przed Java 11 - trzeba wypisac pelny typ
(@NonNull Map.Entry<String, List<Zamowienie>> e) -> ...

// od Java 11
(@NonNull var e) -> ...
```

Ma to znaczenie przy narzedziach analizy statycznej czytajacych `@NonNull` /
`@Nullable` (Checker Framework, SpotBugs).

### Zasada wszystko-albo-nic

```java
(var x, var y) -> x + y          // OK
(String x, String y) -> x + y    // OK
(x, y) -> x + y                  // OK

(var x, y) -> ...                // BLAD - mieszanie var i pominiecia
(var x, String y) -> ...         // BLAD - mieszanie var i typu jawnego
var x -> ...                     // BLAD - z var nawiasy sa OBOWIAZKOWE
```

Ostatnie zaskakuje najbardziej: `x -> x + 1` dziala bez nawiasow,
ale `(var x) -> x + 1` juz ich wymaga.

---

## 5. `Collection.toArray(IntFunction)`

**Kod:** [`jdk11/CollectionToArray.java`](../src/main/java/com/tomtom/genesis/platform/jdk11/CollectionToArray.java)

```java
String[] tablica = lista.toArray(String[]::new);
```

Trzecia — i wreszcie sensowna — forma zamiany kolekcji na tablice.

| Wariant | Wersja | Problem |
|---|---|---|
| `toArray()` | 1.2 | zwraca `Object[]`; rzutowanie na `String[]` daje `ClassCastException` |
| `toArray(new String[0])` | 1.2 | dziala, ale idiom jest nieoczywisty |
| `toArray(new String[10])` | 1.2 | uzywa **tej** tablicy i wstawia `null` jako terminator |
| `toArray(String[]::new)` | **11** | czytelne, spojne ze `Stream.toArray` |

Ciekawostka wydajnosciowa: intuicja podpowiada, ze `new String[size]` jest
szybsze niz `new String[0]`, bo unika drugiej alokacji. Pomiary JMH pokazuja
**odwrotnie** — `new String[0]` jest zwykle szybsze, bo JIT potrafi pominac
zerowanie tablicy, ktora i tak zaraz zostanie nadpisana.

**Uwaga na prymitywy:** `toArray(int[]::new)` sie nie skompiluje — generyki nie
obsluguja prymitywow. Dla `int[]` uzyj `lista.stream().mapToInt(Integer::intValue).toArray()`.

---

## 6. `Predicate.not`

**Kod:** [`jdk11/PredicateNot.java`](../src/main/java/com/tomtom/genesis/platform/jdk11/PredicateNot.java)

```java
.filter(Predicate.not(String::isBlank))
```

Problem: `!` dziala na wartosci `boolean`, a `String::isBlank` to `Predicate`,
wiec `!String::isBlank` sie nie kompiluje. Obejscia z Javy 8 byly brzydkie:

```java
.filter(s -> !s.isBlank())
.filter(((Predicate<String>) String::isBlank).negate())
```

Implementacja w JDK to jedna linia:

```java
static <T> Predicate<T> not(Predicate<? super T> target) {
    return (Predicate<T>) target.negate();
}
```

Cala wartosc polega na tym, ze argument ma **typ docelowy** `Predicate<T>`, wiec
kompilator potrafi wywnioskowac typ referencji do metody. Przy `.negate()`
referencja stoi po lewej stronie kropki — i wnioskowanie nie dziala.

Czyta sie to najlepiej ze statycznym importem:
```java
import static java.util.function.Predicate.not;
...
.filter(not(String::isBlank))
```

**Uwaga na kolejnosc:** `p.and(q).or(r)` to `(p AND q) OR r`. Metody lacza sie
od lewej, bez priorytetu AND nad OR znanego z operatorow. Nawiasuj jawnie.

---

## 7. Nest-Based Access Control (JEP 181)

**Kod:** [`jdk11/NestBasedAccess.java`](../src/main/java/com/tomtom/genesis/platform/jdk11/NestBasedAccess.java)

Zmiana niewidoczna w skladni, ale wazna dla refleksji, frameworkow i bajtkodu.

### Problem

W kodzie zrodlowym klasa zewnetrzna i jej klasy zagniezdzone widza nawzajem
swoje prywatne skladowe. W bajtkodzie to jednak **osobne klasy**, a JVM nie
pozwala klasie A siegnac do `private` klasy B. Do Javy 10 `javac` obchodzil to,
generujac syntetyczne mosty o nazwach `access$000`, `access$100` itd.

### Rozwiazanie

Java 11 wprowadza pojecie **gniazda** (*nest*) na poziomie JVM. Klasy z jednego
pliku zrodlowego naleza do wspolnego gniazda, zapisanego w dwoch nowych
atrybutach pliku `.class`: `NestHost` (w klasach zagniezdzonych) i `NestMembers`
(w gospodarzu). Syntetyczne mosty znikaja.

```java
Wewnetrzna.class.getNestHost()          // NestBasedAccess
NestBasedAccess.class.getNestMembers()  // [NestBasedAccess, NestBasedAccess$Wewnetrzna]
A.class.isNestmateOf(B.class)
```

### Najbardziej praktyczny skutek

Refleksja stala sie **spojna z regulami jezyka**:

```java
Field pole = Wewnetrzna.class.getDeclaredField("sekretWewnetrzny");
Object wartosc = pole.get(instancja);     // dziala BEZ setAccessible(true)
```

W Javie 8 ta sama linia rzucala `IllegalAccessException`, mimo ze zwykly odczyt
`w.sekretWewnetrzny` kompilowal sie bez problemu.

### Dlaczego to wazne poza ciekawostka

- mniej wygenerowanego kodu, mniejsze pliki `.class`;
- syntetyczne mosty byly **package-private**, wiec prywatne pole dalo sie
  odczytac z innej klasy w tym samym pakiecie — to byla dziura w hermetyzacji;
- narzedzia generujace bajtkod (Lombok, Mockito, proxy Springa, kompilatory
  Kotlina i Scali) moga generowac prostszy kod;
- to fundament, na ktorym pozniej zbudowano klasy ukryte (Java 15)
  i klasy zapieczetowane (Java 17).

Zobacz to sam:
```bash
javap -p -c target/classes/com/tomtom/genesis/platform/jdk11/NestBasedAccess.class
```

---

## 8. Uruchamianie pojedynczego pliku zrodlowego (JEP 330)

```bash
java Skrypt.java          # kompilacja w pamieci + uruchomienie, bez javac
```

Przedtem trzeba bylo `javac Skrypt.java && java Skrypt`. Teraz `java` na pliku
`.java` kompiluje go w pamieci i od razu uruchamia — nie powstaje zaden `.class`.

Dziala tylko dla **jednego** pliku (klasy pomocnicze moga byc w tym samym pliku).
Razem z shebangiem pozwala pisac skrypty w Javie:

```java
#!/usr/bin/java --source 11
public class Skrypt {
    public static void main(String[] args) { System.out.println("czesc"); }
}
```

W tym repozytorium mozesz sprobowac:
```bash
java src/main/java/com/tomtom/genesis/platform/jdk11/StringApi11.java
```

---

## 9. Zmiany w JVM, bezpieczenstwie i narzedziach

| Zmiana | Znaczenie |
|---|---|
| **ZGC** (JEP 333) | Eksperymentalny GC o pauzach ponizej 10 ms niezaleznie od rozmiaru sterty. W 11 tylko Linux x64 i tylko eksperymentalnie (`-XX:+UnlockExperimentalVMOptions -XX:+UseZGC`). Produkcyjnie dopiero od Javy 15 |
| **Epsilon GC** (JEP 318) | GC, ktory **nic nie zbiera**. Brzmi absurdalnie, ale ma sens: benchmarki bez szumu GC, krotkie zadania, testy wykrywajace nadmierne alokacje (`OutOfMemoryError` zamiast cichego zwolnienia) |
| **Flight Recorder** (JEP 328) | Do Javy 11 komercyjna funkcja Oracle. Od 11 czescia OpenJDK, za darmo. Profilowanie produkcji z narzutem ~1%: `java -XX:StartFlightRecording=duration=60s,filename=r.jfr` |
| **TLS 1.3** (JEP 332) | Szybszy handshake (1-RTT), usuniete slabe szyfry. Uwaga migracyjna: stare serwery i biblioteki potrafia sie z nim nie dogadac; w razie problemow `-Djdk.tls.client.protocols=TLSv1.2` |
| **Unicode 10** (JEP 327) | +16 000 znakow, w tym emoji |
| **ChaCha20 i Poly1305** (JEP 329) | Nowoczesne szyfry, szybsze od AES na sprzecie bez akceleracji |
| **Dynamiczne stale w pliku klasy** (JEP 309) | `CONSTANT_Dynamic` — infrastruktura pod pozniejsze funkcje jezyka (rekordy, wzorce) i pod kompilatory innych jezykow |
| **Nashorn `@Deprecated`** (JEP 335) | Silnik JavaScript oznaczony do usuniecia (usuniety w 15) |
| **Applety `@Deprecated`** | Koniec ery. Usuniete w Javie 17 |

---

## 10. Co Java 11 USUNELA — i dlaczego to boli

**To jest glowne zrodlo bolu przy migracji z Javy 8.** Szczegoly i konkretne
zaleznosci Mavena: [migracja-8-11.md](migracja-8-11.md).

| Usuniete (JEP 320 i inne) | Co przestaje dzialac |
|---|---|
| `java.xml.bind` (JAXB) | `@XmlRootElement`, `JAXBContext`, marshalling XML |
| `java.xml.ws` (JAX-WS) | klienci i serwery SOAP |
| `java.activation` (JAF) | `DataHandler`, zalaczniki MIME |
| `java.corba` | CORBA, `org.omg.*` |
| `java.transaction` | `javax.transaction.*` (czesc) |
| `java.se.ee` | modul zbiorczy dla powyzszych |
| `javax.annotation` (czesc) | `@PostConstruct`, `@PreDestroy`, `@Resource` |
| `com.sun.awt.AWTUtilities` | niepubliczne API AWT |

Wszystkie te API zyja dalej jako **zwykle zaleznosci z Maven Central** — trzeba
je tylko jawnie dodac do `pom.xml`. Typowy objaw: kod kompiluje sie na Javie 8,
a na 11 wita cie `package javax.xml.bind does not exist`.

---

## Licencjonowanie — rzecz spoza jezyka, o ktora zawsze ktos pyta

Od Javy 11 Oracle rozdzielil dwie dystrybucje:

- **Oracle JDK** — licencja komercyjna, platna do uzytku produkcyjnego
  (od Javy 17 znow darmowa na licencji NFTC, ale z ograniczeniami czasowymi);
- **OpenJDK** — GPLv2 z Classpath Exception, darmowy.

Popularne darmowe buildy OpenJDK: **Eclipse Temurin** (dawniej AdoptOpenJDK),
**Amazon Corretto**, **Azul Zulu**, **Red Hat**, **Microsoft Build of OpenJDK**.
Kod jest praktycznie ten sam — roznice sa w harmonogramie latek i wsparciu.

---

## Kolejny krok

[migracja-8-11.md](migracja-8-11.md) — praktyczna lista problemow, ktore napotkasz
przy podnoszeniu istniejacego projektu z Javy 8.

Dalej, juz po angielsku: [java12-15.md](java12-15.md) — droga do rekordow
i blokow tekstu, a stamtad przez [java16-17.md](java16-17.md),
[java18-20.md](java18-20.md), [java21.md](java21.md) i [java22-24.md](java22-24.md)
do [java25.md](java25.md).
