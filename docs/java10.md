# Java 10 — pierwsze wydanie w cyklu polrocznym

Marzec 2018, pol roku po Javie 9. Wydanie krotkie i skromne — i taki byl zamysl.
Java 10 jest ciekawa glownie jako **dowod, ze nowy model wydawniczy dziala**:
zamiast czekac trzy lata na wielka wersje, funkcje trafiaja do jezyka wtedy,
gdy sa gotowe.

Dla programisty Java 10 to w praktyce **jedna duza zmiana skladniowa (`var`)
i kilka drobiazgow domykajacych tematy z Javy 9**.

> **Uwaga:** Java 10 nie jest wersja LTS i nie ma juz zadnego wsparcia.
> Traktuj ja jako etap w drodze do 11, a nie jako cel migracji.

---

## 1. `var` — wnioskowanie typu zmiennej lokalnej (JEP 286)

**Kod:** [`jdk10/LocalVarTypeInference.java`](../src/main/java/com/tomtom/genesis/platform/jdk10/LocalVarTypeInference.java)

```java
var tekst = "Ala ma kota";            // String
var lista = new ArrayList<String>();  // ArrayList<String>
var mapa = Map.of("a", 1);            // Map<String, Integer>

for (var i = 0; i < 3; i++) { ... }
for (var wpis : mapa.entrySet()) { ... }
try (var zasob = otworz()) { ... }
```

### Najwazniejsze zdanie o `var`

**To nie jest typowanie dynamiczne.** Typ jest ustalany przez kompilator
w miejscu deklaracji, na stale, i zapisywany w bajtkodzie. W pliku `.class`
nie ma zadnego sladu po slowie `var`. Zmienna `var x = "a"` jest typu `String`
i pozostanie nim do konca — `x = 42` to blad kompilacji.

### Gdzie `var` nie dziala

Wszystkie ponizsze to bledy kompilacji:

```java
var bezWartosci;                    // brak inicjalizatora - nie ma z czego wnioskowac
var nic = null;                     // typ null nie jest typem docelowym
var f = () -> System.out.print();   // lambda potrzebuje typu docelowego
var m = String::valueOf;            // to samo dla referencji do metody
var t = {1, 2, 3};                  // inicjalizator tablicowy bez new int[]
private var pole = 1;               // tylko zmienne LOKALNE
void metoda(var argument)           // nie dla parametrow metod (lambdy - patrz Java 11)
var metoda() { ... }                // nie dla typu zwracanego
catch (var e)                       // nie dla parametru catch
```

Zasada: `var` dziala tam, gdzie kompilator widzi jednoznaczna wartosc poczatkowa
i gdzie zasieg zmiennej ogranicza sie do jednej metody.

### Pulapka 1: diament daje typ ogolniejszy niz chcesz

```java
var zle = new ArrayList<>();        // ArrayList<Object>, nie ArrayList<String>!
zle.add("tekst");
zle.add(42);                        // kompiluje sie - to lista Object

var dobrze = new ArrayList<String>();
```

Z `var` parametr typu musi byc po **prawej** stronie, bo po lewej go juz nie ma.

Ta sama pulapka w wersji liczbowej: `var x = 1` to `int`, nie `long`.
Jesli potrzebujesz `long`, napisz `var x = 1L`.

### Pulapka 2 (a wlasciwie supermoc): typy nienazywalne

Typ klasy anonimowej nie ma nazwy, wiec nie da sie go zapisac po lewej stronie.
`var` potrafi go zapamietac:

```java
var licznik = new Object() {
    int wartosc = 0;
    void zwieksz() { wartosc++; }
};
licznik.zwieksz();
System.out.println(licznik.wartosc);   // 1 - dziala!
```

Gdyby napisac `Object licznik = new Object() {...}`, pole `wartosc` byloby
niedostepne — typ `Object` go nie zna.

Drugi taki przypadek to **typ przecieciowy** (*intersection type*) z wyrazenia
warunkowego:

```java
var kolekcja = true ? new ArrayList<String>() : new LinkedList<String>();
```

Operator `? :` bierze typy **statyczne** obu galezi i szuka ich najmniejszego
wspolnego nadtypu. Zadna z tych klas nie jest nadtypem drugiej, wiec wynikiem
jest przeciecie kilku typow naraz. Kompilator nazywa je tak:

```
error: incompatible types: INT#1 cannot be converted to int
  where INT#1 is an intersection type:
    INT#1 extends AbstractList<String>,Serializable,Cloneable
```

Zmienna jest jednoczesnie `AbstractList<String>`, `Serializable` **i**
`Cloneable`, a zadna pojedyncza nazwa tego nie wyrazi — napisanie
`AbstractList<String>` po lewej stronie gubi dwa pozostale. Tylko `var`
zapamietuje calosc.

**Uwaga na czesta pomylke.** To dziala tylko dlatego, ze obie galezie to
wyrazenia `new ...`. Gdyby lewa byla zmienna zadeklarowana jako `List<String>`:

```java
List<String> lista = new ArrayList<>();
var cos = true ? lista : new LinkedList<String>();   // typ: List<String>
```

kompilator widzi `List<String>`, a nie `ArrayList` — bo liczy sie typ
**zadeklarowany**, nie klasa obiektu w pamieci. `LinkedList<String>` jest
podtypem `List<String>`, wiec wspolnym nadtypem jest zwyczajne `List<String>`
i zaden typ nienazywalny nie powstaje. Wypisanie `cos.getClass()` da wtedy
`ArrayList`, ale to juz informacja z **runtime**, nie typ statyczny zmiennej.

To dwa jedyne miejsca, gdzie `var` daje cos, czego nie da sie napisac inaczej.

### `var` to nie slowo kluczowe

`var` jest **zarezerwowana nazwa typu**, nie slowem kluczowym. Dlatego:

```java
int var = 10;      // nadal legalne!
```

Zakazane jest tylko nazwanie `var` **klasy** albo **interfejsu**. Dzieki temu
wprowadzenie `var` nie zepsulo zadnego istniejacego kodu — a to byl twardy
wymog projektowy.

### Kiedy uzywac

Autorzy JDK wydali osobny dokument (*Local-Variable Type Inference: Style
Guidelines*). Sedno: **czytelnosc liczy sie bardziej niz krotkosc deklaracji**.

Pomaga, gdy typ i tak widac po prawej stronie:
```java
var klient = new HttpClientBuilderFactory();
var wpisy = new HashMap<String, List<Zamowienie>>();
```

Szkodzi, gdy typ znika z pola widzenia:
```java
var wynik = serwis.przetworz(dane);   // co to jest? trzeba isc do definicji
var x = 0;                            // int czy long? intencja niejasna
```

Praktyczna wskazowka: jesli po uzyciu `var` musisz nadac zmiennej dluzsza,
bardziej opisowa nazwe — zrob to. Nazwa ma nadrobic to, co znika razem z typem.

---

## 2. `List.copyOf` / `Set.copyOf` / `Map.copyOf`

**Kod:** [`jdk10/UnmodifiableCopies.java`](../src/main/java/com/tomtom/genesis/platform/jdk10/UnmodifiableCopies.java)

Java 9 dala fabryki dla literalow (`List.of("a", "b")`). Java 10 domyka temat:
jak zrobic niemodyfikowalna kopie **istniejacej** kolekcji.

```java
List<String> kopia = List.copyOf(zrodlo);
```

Przedtem: `Collections.unmodifiableList(new ArrayList<>(zrodlo))`.

### Sprytna optymalizacja

`copyOf` na kolekcji, ktora **juz** jest niemodyfikowalna, nie kopiuje —
zwraca ten sam obiekt:

```java
List<String> a = List.of("a", "b");
List<String> b = List.copyOf(a);
a == b   // true
```

Dlatego `List.copyOf` mozna bezpiecznie wolac w konstruktorze klasy niezmiennej:
nie placisz za kopie, gdy wolajacy juz przekazal `List.of(...)`.

### Pulapka: to jest kopia PLYTKA

```java
List<StringBuilder> kopia = List.copyOf(zrodlo);
kopia.get(0).append(" ZMIENIONA");   // dziala!
```

`copyOf` zamraza **strukture** listy, nie stan elementow. Prawdziwa niezmiennosc
wymaga, zeby same elementy byly niezmienne.

---

## 3. `Collectors.toUnmodifiableList` / `toUnmodifiableSet` / `toUnmodifiableMap`

```java
strumien.collect(Collectors.toUnmodifiableList());
```

Warto znac roznice miedzy trzema podobnymi rzeczami:

| Metoda | Wersja | Modyfikowalna? | `null` w elementach? |
|---|---|---|---|
| `Collectors.toList()` | 8 | kontrakt **nie gwarantuje** (w praktyce `ArrayList`) | dozwolony |
| `Collectors.toUnmodifiableList()` | 10 | nie | **NPE** |
| `Stream.toList()` | 16 | nie | **dozwolony** |

Ostatni wiersz zaskakuje najczesciej: `Stream.toList()` z Javy 16 wyglada jak
skrot do `toUnmodifiableList()`, ale **akceptuje nulle**. To nie jest ta sama metoda.

---

## 4. `Optional.orElseThrow()` bez argumentu

**Kod:** [`jdk10/OptionalOrElseThrow.java`](../src/main/java/com/tomtom/genesis/platform/jdk10/OptionalOrElseThrow.java)

```java
String v = optional.orElseThrow();   // dokladnie to samo co get()
```

Zachowanie jest identyczne z `get()` — ten sam `NoSuchElementException`, ten sam
komunikat. Zmienila sie **tylko nazwa**, zeby czytajac kod widziec, ze ta linia
moze rzucic wyjatkiem. Autorzy JDK (Stuart Marks, Brian Goetz) przyznali
publicznie, ze nazwa `get()` byla bledem projektowym z Javy 8.

`get()` nie jest formalnie `@Deprecated` (zlamaloby to zbyt duzo kodu), ale
traktuj je jak przestarzale — IDE i linter zwykle je zglaszaja.

### Cala rodzina, dla porzadku

| Metoda | Zwraca | Uwaga |
|---|---|---|
| `orElse(x)` | `T` | `x` liczone **zawsze**, nawet gdy Optional ma wartosc |
| `orElseGet(() -> x)` | `T` | `x` liczone leniwie |
| `orElseThrow()` | `T` | `NoSuchElementException` (Java 10) |
| `orElseThrow(Ex::new)` | `T` | wlasny wyjatek (Java 8) |
| `or(() -> Optional.of(x))` | `Optional<T>` | da sie lancuchowac (Java 9) |

Pulapka `orElse` vs `orElseGet` jest realna i kosztowna: argument `orElse` jest
wyliczany zawsze. Jesli to zapytanie do bazy albo nowy obiekt — placisz za nie
przy kazdym wywolaniu, takze gdy Optional ma wartosc. Uruchom przyklad, zeby
zobaczyc to na wlasne oczy.

---

## 5. Zmiany w JVM warte odnotowania

| Zmiana | Znaczenie praktyczne |
|---|---|
| **Swiadomosc kontenerow** (JDK-8146115) | JVM czyta limity cgroups i widzi `-m 512m` z Dockera jako realny limit pamieci, a nie pamiec calego hosta. Przed Java 10 (i przed backportem do 8u191) kontener z limitem 512 MB potrafil dostac `OutOfMemoryError` od OOM-killera, bo JVM ustawil sobie heap wedlug pamieci hosta. **To jedna z najwazniejszych praktycznych roznic 8 vs 10+.** Wlaczane przez `-XX:+UseContainerSupport` (domyslnie wlaczone) |
| **Rownolegly full GC dla G1** (JEP 307) | Pelne zbieranie w G1 przestalo byc jednowatkowe. Skraca najgorsze pauzy w aplikacjach, ktore doprowadzaja G1 do full GC |
| **Wspoldzielenie danych klas aplikacji** (JEP 310) | `-Xshare` / AppCDS — archiwum wstepnie przetworzonych klas przyspiesza start JVM i pozwala wspoldzielic pamiec miedzy procesami |
| **Domyslne certyfikaty w OpenJDK** (JEP 319) | Do Javy 10 OpenJDK mial **pusty** `cacerts`. Skutek: HTTPS dzialalo na Oracle JDK, a na OpenJDK rzucalo bledem walidacji certyfikatu. To bylo zrodlo klasycznego "u mnie dziala" |
| **Uchwyty watkow** (JEP 312) | JVM moze zatrzymac pojedynczy watek zamiast calego swiata. Fundament pod pozniejsze niskopauzowe GC |
| **Usuniecie `javah`** (JEP 313) | Naglowki JNI generuje teraz `javac -h` |

---

## Kolejny krok

[java11.md](java11.md) — pierwsza wersja LTS po Javie 8 i realny cel migracji.
