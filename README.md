# JavaUpdate — Java 9 → 25

> **Nowe: [Czesc III — szkolenie ze Spring Boot 4](spring-boot/README.md)**
> (osobny projekt Maven w katalogu `spring-boot/`, 9 lekcji z kodem, testami i cwiczeniami).

Repozytorium do nauki: **uruchamialne przyklady** kazdej istotnej zmiany
w Javie od wersji 9 do 25, plus dokumenty tlumaczace, jak te rzeczy dzialaja
i gdzie sa pulapki.

> **Jezyk / Language.** Czesc o Javie 9–11 jest po polsku. Czesc o Javie 12–25
> jest po angielsku — to swiadomy podzial, zgodny z tym, jak repozytorium
> powstawalo. Sekcje ponizej sa oznaczone.
>
> The Java 9–11 material is in Polish; the Java 12–25 material is in English.
> Code comments follow the same split.

---

## Jak z tego korzystac

Kazda klasa ma wlasna metode `main()` i wypisuje wynik z komentarzem. Otworz
klase, uruchom, przeczytaj wyjscie, pozmieniaj kod, uruchom ponownie.

```bash
# kompilacja calosci
mvn compile

# uruchomienie jednego przykladu
java -cp target/classes com.tomtom.genesis.platform.jdk9.CollectionFactories

# albo bez kompilacji - pojedynczy plik zrodlowy (to samo w sobie funkcja Javy 11!)
java src/main/java/com/tomtom/genesis/platform/jdk11/StringApi11.java

# demo modulow (osobny build, poza Mavenem)
./examples/jpms-demo/run.sh

# funkcje preview z Javy 25 (wymagaja --enable-preview, wiec poza Mavenem)
./examples/preview/run.sh

# compact source file - program bez klasy i bez static main (Java 25)
java examples/java25/Script.java
```

---

# Czesc I — Java 9, 10, 11 (po polsku)

## Spis tresci

### [Java 9](docs/java9.md) — najwieksza zmiana od generykow

| Temat | Klasa |
|---|---|
| System modulow (JPMS) | [`examples/jpms-demo/`](examples/jpms-demo) — `./run.sh` |
| `List.of` / `Set.of` / `Map.of` | [`jdk9/CollectionFactories`](src/main/java/com/tomtom/genesis/platform/jdk9/CollectionFactories.java) |
| `takeWhile`, `dropWhile`, `iterate`, `ofNullable`, `Collectors.filtering` | [`jdk9/StreamApi9`](src/main/java/com/tomtom/genesis/platform/jdk9/StreamApi9.java) |
| `Optional.or` / `ifPresentOrElse` / `stream` | [`jdk9/OptionalApi9`](src/main/java/com/tomtom/genesis/platform/jdk9/OptionalApi9.java) |
| Prywatne metody w interfejsach | [`jdk9/PrivateInterfaceMethods`](src/main/java/com/tomtom/genesis/platform/jdk9/PrivateInterfaceMethods.java) |
| try-with-resources na zmiennej | [`jdk9/TryWithResources9`](src/main/java/com/tomtom/genesis/platform/jdk9/TryWithResources9.java) |
| `ProcessHandle` — PID, drzewo procesow, `onExit` | [`jdk9/ProcessApiDemo`](src/main/java/com/tomtom/genesis/platform/jdk9/ProcessApiDemo.java) |
| `StackWalker` | [`jdk9/StackWalkerDemo`](src/main/java/com/tomtom/genesis/platform/jdk9/StackWalkerDemo.java) |

Bez kodu, opisane w dokumencie: JShell, kompaktowe Stringi, G1 domyslnym GC,
`Flow`, `VarHandle`, `jlink`, wieloversyjne JAR-y, ujednolicone logowanie JVM.

### [Java 10](docs/java10.md) — `var` i domkniecie kolekcji

| Temat | Klasa |
|---|---|
| `var` i wszystkie jego pulapki | [`jdk10/LocalVarTypeInference`](src/main/java/com/tomtom/genesis/platform/jdk10/LocalVarTypeInference.java) |
| `List.copyOf`, `Collectors.toUnmodifiable*` | [`jdk10/UnmodifiableCopies`](src/main/java/com/tomtom/genesis/platform/jdk10/UnmodifiableCopies.java) |
| `Optional.orElseThrow()` i cala rodzina | [`jdk10/OptionalOrElseThrow`](src/main/java/com/tomtom/genesis/platform/jdk10/OptionalOrElseThrow.java) |

Bez kodu: swiadomosc kontenerow (cgroups), rownolegly full GC dla G1, AppCDS,
domyslne certyfikaty w OpenJDK.

### [Java 11](docs/java11.md) — pierwsza LTS po Javie 8

| Temat | Klasa |
|---|---|
| `HttpClient` — sync, async, POST, rownolegle | [`jdk11/HttpClientDemo`](src/main/java/com/tomtom/genesis/platform/jdk11/HttpClientDemo.java) |
| `isBlank`, `strip`, `lines`, `repeat` | [`jdk11/StringApi11`](src/main/java/com/tomtom/genesis/platform/jdk11/StringApi11.java) |
| `Files.readString` / `writeString`, `Path.of` | [`jdk11/FilesApi11`](src/main/java/com/tomtom/genesis/platform/jdk11/FilesApi11.java) |
| `var` w lambdzie | [`jdk11/VarInLambda`](src/main/java/com/tomtom/genesis/platform/jdk11/VarInLambda.java) |
| `toArray(String[]::new)` | [`jdk11/CollectionToArray`](src/main/java/com/tomtom/genesis/platform/jdk11/CollectionToArray.java) |
| `Predicate.not` | [`jdk11/PredicateNot`](src/main/java/com/tomtom/genesis/platform/jdk11/PredicateNot.java) |
| Nest-Based Access Control | [`jdk11/NestBasedAccess`](src/main/java/com/tomtom/genesis/platform/jdk11/NestBasedAccess.java) |

Bez kodu: ZGC, Epsilon GC, Flight Recorder, TLS 1.3, uruchamianie pojedynczego
pliku zrodlowego, usuniecie Java EE i CORBY, licencjonowanie.

### [Migracja 8 → 11](docs/migracja-8-11.md)

Praktyczna lista tego, co sie psuje: brakujace pakiety Java EE, ostrzezenia
o nielegalnym dostepie refleksyjnym, zmiana formatu wersji, wymagane wersje
narzedzi, split packages, G1 i flagi logowania GC, kontenery i pamiec.

---

# Part II — Java 12 to 25 (in English)

Each class has its own `main()` and prints commented output. Open it, run it,
read the output, change the code, run it again.

```bash
java -cp target/classes com.tomtom.genesis.platform.jdk21.VirtualThreads
```

## [Java 12 – 15](docs/java12-15.md) — the road to records and text blocks

| Topic | Class |
|---|---|
| `Collectors.teeing` | [`jdk12/CollectorsTeeing`](src/main/java/com/tomtom/genesis/platform/jdk12/CollectorsTeeing.java) |
| `String.indent`/`transform`, `Files.mismatch`, compact numbers | [`jdk12/SmallAdditions12`](src/main/java/com/tomtom/genesis/platform/jdk12/SmallAdditions12.java) |
| Switch expressions, `yield`, exhaustiveness | [`jdk14/SwitchExpressions`](src/main/java/com/tomtom/genesis/platform/jdk14/SwitchExpressions.java) |
| Helpful NullPointerExceptions | [`jdk14/HelpfulNullPointerExceptions`](src/main/java/com/tomtom/genesis/platform/jdk14/HelpfulNullPointerExceptions.java) |
| Text blocks, incidental whitespace, `formatted`/`stripIndent` | [`jdk15/TextBlocks`](src/main/java/com/tomtom/genesis/platform/jdk15/TextBlocks.java) |

Documented without code: how previews work, Shenandoah, CDS, the Socket API
rewrite, hidden classes, the removal of CMS and Nashorn.

## [Java 16 – 17](docs/java16-17.md) — records, patterns, sealed types

| Topic | Class |
|---|---|
| Records, compact constructors, defensive copying | [`jdk16/Records`](src/main/java/com/tomtom/genesis/platform/jdk16/Records.java) |
| Pattern matching for `instanceof`, flow scoping | [`jdk16/PatternMatchingForInstanceof`](src/main/java/com/tomtom/genesis/platform/jdk16/PatternMatchingForInstanceof.java) |
| `Stream.toList` (and how it differs from the other two), `mapMulti` | [`jdk16/StreamAdditions16`](src/main/java/com/tomtom/genesis/platform/jdk16/StreamAdditions16.java) |
| Sealed types, `final`/`sealed`/`non-sealed`, exhaustiveness | [`jdk17/SealedTypes`](src/main/java/com/tomtom/genesis/platform/jdk17/SealedTypes.java) |
| `RandomGenerator`, splittable and jumpable generators | [`jdk17/RandomGenerators`](src/main/java/com/tomtom/genesis/platform/jdk17/RandomGenerators.java) |

Documented without code: strong encapsulation of JDK internals (JEP 403) — the
main reason an application that ran on 11 may not start on 17.

## [Java 18 – 20](docs/java18-20.md) — UTF-8, a web server, previews

| Topic | Class |
|---|---|
| UTF-8 by default, `native.encoding`, migration hatches | [`jdk18/Utf8ByDefault`](src/main/java/com/tomtom/genesis/platform/jdk18/Utf8ByDefault.java) |
| `jwebserver` and the `SimpleFileServer` API | [`jdk18/SimpleWebServerDemo`](src/main/java/com/tomtom/genesis/platform/jdk18/SimpleWebServerDemo.java) |

Java 19 and 20 were almost entirely previews and are covered in the document only.

## [Java 21](docs/java21.md) — the LTS that changed how Java code looks

| Topic | Class |
|---|---|
| Virtual threads, 100 000 concurrent tasks, pinning | [`jdk21/VirtualThreads`](src/main/java/com/tomtom/genesis/platform/jdk21/VirtualThreads.java) |
| Pattern matching for switch, guards, `case null` | [`jdk21/PatternMatchingForSwitch`](src/main/java/com/tomtom/genesis/platform/jdk21/PatternMatchingForSwitch.java) |
| Record patterns, nesting, the null rule | [`jdk21/RecordPatterns`](src/main/java/com/tomtom/genesis/platform/jdk21/RecordPatterns.java) |
| Sequenced collections, `reversed()` as a view | [`jdk21/SequencedCollections`](src/main/java/com/tomtom/genesis/platform/jdk21/SequencedCollections.java) |
| `Math.clamp`, `splitWithDelimiters`, emoji, `Future.state` | [`jdk21/SmallAdditions21`](src/main/java/com/tomtom/genesis/platform/jdk21/SmallAdditions21.java) |

## [Java 22 – 24](docs/java22-24.md) — filling in the gaps

| Topic | Class |
|---|---|
| Unnamed variables and patterns (`_`) | [`jdk22/UnnamedVariables`](src/main/java/com/tomtom/genesis/platform/jdk22/UnnamedVariables.java) |
| Stream gatherers, `mapConcurrent`, writing your own | [`jdk24/StreamGatherers`](src/main/java/com/tomtom/genesis/platform/jdk24/StreamGatherers.java) |
| Class-File API — reading and generating bytecode | [`jdk24/ClassFileApiDemo`](src/main/java/com/tomtom/genesis/platform/jdk24/ClassFileApiDemo.java) |

Documented without code: the Foreign Function & Memory API, the withdrawal of
String Templates, Markdown Javadoc, virtual threads no longer pinning on
`synchronized`, compact object headers, AOT caching.

## [Java 25](docs/java25.md) — the current LTS

| Topic | Class |
|---|---|
| Scoped values, and why inheritance is narrower than expected | [`jdk25/ScopedValues`](src/main/java/com/tomtom/genesis/platform/jdk25/ScopedValues.java) |
| Flexible constructor bodies, the prologue/epilogue rules | [`jdk25/FlexibleConstructorBodies`](src/main/java/com/tomtom/genesis/platform/jdk25/FlexibleConstructorBodies.java) |
| Module import declarations | [`jdk25/ModuleImports`](src/main/java/com/tomtom/genesis/platform/jdk25/ModuleImports.java) |
| Compact source files, instance `main`, `java.lang.IO` | [`examples/java25/Script.java`](examples/java25/Script.java) |
| **Preview:** stable values, structured concurrency | [`examples/preview/`](examples/preview) — `./run.sh` |

## [Migration 11 → 25](docs/migration-11-to-25.md)

What actually breaks: strong encapsulation in 17, the UTF-8 switch in 18, removed
collectors and engines, library version floors, `javax` → `jakarta`, and a
migration order that works.

---

# Czesc III — Spring Boot 4 (po polsku)

Osobny projekt Maven w [`spring-boot/`](spring-boot) (Spring Boot 4.1.2, Java 25).
Jedna aplikacja, dziewiec lekcji: start i autokonfiguracja, beany, konfiguracja,
REST, Spring Data JPA, testowanie, Actuator, klienci HTTP i nowosci Boot 4.
Spis tresci i instrukcja: [spring-boot/README.md](spring-boot/README.md).

```bash
cd spring-boot && mvn test && mvn spring-boot:run
```

---

## Uwagi techniczne / Technical notes

**Projekt kompiluje sie pod JDK 25, nie pod 11.** `pom.xml` ma
`maven.compiler.source/target = 25`, bo `Main.java` (wygenerowany przez IntelliJ)
uzywa `IO.println` i bezargumentowego `main()` — skladni z JDK 25. Przyklady
z Javy 9/10/11 dzialaja pod 25 bez zmian; to, ktora wersja co wprowadzila,
opisuja dokumenty.

**Same przyklady sa jednak czyste wersyjnie / Examples are version-pure.**
Kazdy pakiet `jdkNN` kompiluje sie pod `--release NN`, wiec masz pewnosc, ze
uzywa wylacznie API dostepnego w opisywanej wersji. Sprawdzisz to tak:

```bash
for v in 10 11 12 14 15 16 18 21 22 24 25; do
    d=src/main/java/com/tomtom/genesis/platform/jdk$v
    javac --release $v -d /tmp/rel$v $(find $d -name '*.java') && echo "jdk$v OK"
done
```

Trzy swiadome wyjatki, kazdy opisany komentarzem w kodzie:

| Pakiet | Dlaczego nie przechodzi `--release` wlasnej wersji |
|---|---|
| `jdk9` | celowo pokazuje `List.copyOf` i `orElseThrow()` z Javy 10 jako kontrast. Caly zestaw 9/10/11 przechodzi `--release 11` |
| `jdk17` | `SealedTypes` uzywa pattern matching for switch, ktory byl w 17 funkcja preview, a standardem stal sie w 21. Bez tego przyklad nie pokazywalby sedna sealed types |
| `Main.java` | plik wygenerowany przez IntelliJ, uzywa skladni z JDK 25 |

Gdybys chcial przestawic caly projekt na starsza Jave, usun `Main.java` i ustaw
odpowiedni `<maven.compiler.release>`.

**Komentarze: pakiety `jdk9`–`jdk11` po polsku, `jdk12`–`jdk25` po angielsku.**
Bez polskich znakow diakrytycznych w kodzie zrodlowym — zeby nie zalezec od
kodowania konsoli przy uruchamianiu.

---

## Weryfikacja / Verification

Stan zweryfikowany na **OpenJDK 25+36, macOS arm64**:

- `mvn clean compile` — przechodzi;
- **40 klas** przykladowych uruchamia sie z kodem wyjscia 0;
- `examples/jpms-demo/run.sh` — pelny cykl: kompilacja modulow → uruchomienie →
  celowy blad kompilacji → uruchomienie z classpath → `jdeps`;
- `examples/preview/run.sh` — oba przyklady preview z `--enable-preview`;
- `java examples/java25/Script.java` — compact source file.

Liczby w dokumentach (czasy, rozmiary, komunikaty bledow, listy algorytmow)
pochodza z rzeczywistych uruchomien na tej maszynie, nie z pamieci.
Kilka przykladow poprawiono wlasnie dlatego, ze wynik przeczyl komentarzowi —
m.in. benchmark `StackWalker` i dziedziczenie scoped values.
