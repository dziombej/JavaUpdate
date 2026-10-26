#!/usr/bin/env bash
#
# Demo JPMS (Java Platform Module System, JEP 261) - Java 9.
#
# Pokazuje trzy rzeczy:
#   1. kompilacje i uruchomienie dwoch modulow ze sciezka modulow,
#   2. prawdziwy blad kompilacji przy probie siegniecia do pakietu nieeksportowanego,
#   3. te sama klase uruchomiona na CLASSPATH - gdzie wszystkie zakazy znikaja.
#
set -u

KATALOG="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$KATALOG"
WYJSCIE="$KATALOG/out"

naglowek() {
    echo
    echo "################################################################"
    echo "# $1"
    echo "################################################################"
    echo
}

rm -rf "$WYJSCIE"
mkdir -p "$WYJSCIE"

# ---------------------------------------------------------------------------
naglowek "1. Kompilacja obu modulow"

echo "polecenie:"
echo "  javac -d out --module-source-path . \$(find . -name '*.java')"
echo

# --module-source-path wymaga, zeby katalog kazdego modulu nazywal sie
# dokladnie tak jak modul. Dlatego katalogi to com.tomtom.przyklad.uslugi
# i com.tomtom.przyklad.aplikacja, a nie "uslugi" / "aplikacja".
if javac -d "$WYJSCIE" --module-source-path . $(find com.tomtom.przyklad.* -name '*.java'); then
    echo "kompilacja OK"
    echo
    echo "struktura wyjsciowa (modul = katalog):"
    find "$WYJSCIE" -name '*.class' | sed "s|$WYJSCIE/|  |" | sort
else
    echo "kompilacja nie powiodla sie"
    exit 1
fi

# ---------------------------------------------------------------------------
naglowek "2. Uruchomienie ze sciezka modulow"

echo "polecenie:"
echo "  java --module-path out --module com.tomtom.przyklad.aplikacja/com.tomtom.przyklad.aplikacja.Start"
echo

java --module-path "$WYJSCIE" \
     --module com.tomtom.przyklad.aplikacja/com.tomtom.przyklad.aplikacja.Start

# ---------------------------------------------------------------------------
naglowek "3. Proba siegniecia do pakietu NIEEKSPORTOWANEGO (ma sie nie udac)"

cp zle-przyklady/DostepDoImpl.java.txt \
   com.tomtom.przyklad.aplikacja/com/tomtom/przyklad/aplikacja/DostepDoImpl.java

echo "dodano klase, ktora importuje com.tomtom.przyklad.uslugi.impl.PolskiePowitanie"
echo "komunikat kompilatora:"
echo

javac -d "$WYJSCIE.zly" --module-source-path . \
      $(find com.tomtom.przyklad.* -name '*.java') 2>&1 | sed 's/^/  /'

rm -f com.tomtom.przyklad.aplikacja/com/tomtom/przyklad/aplikacja/DostepDoImpl.java
rm -rf "$WYJSCIE.zly"

echo
echo "To jest sedno JPMS: 'public' nie znaczy juz 'dostepne dla wszystkich'."
echo "Klasa PolskiePowitanie jest public, ale jej pakiet nie jest wyeksportowany,"
echo "wiec dla innych modulow po prostu nie istnieje."

# ---------------------------------------------------------------------------
naglowek "4. Ta sama klasa na CLASSPATH - hermetyzacja znika"

echo "polecenie:"
echo "  java -cp out/com.tomtom.przyklad.uslugi:out/com.tomtom.przyklad.aplikacja \\"
echo "       com.tomtom.przyklad.aplikacja.Start"
echo

java -cp "$WYJSCIE/com.tomtom.przyklad.uslugi:$WYJSCIE/com.tomtom.przyklad.aplikacja" \
     com.tomtom.przyklad.aplikacja.Start

echo
echo "Zwroc uwage na roznice w sekcji 'Introspekcja modulu':"
echo "na classpath nazwa modulu to null (modul nienazwany), a refleksja do"
echo "pakietu impl DZIALA. Classpath ignoruje module-info.class."
echo
echo "Praktyczny wniosek: sama obecnosc module-info.java nic nie daje, jesli"
echo "aplikacja i tak startuje z -cp. Hermetyzacja wlacza sie dopiero wtedy,"
echo "gdy kod trafia na --module-path."

# ---------------------------------------------------------------------------
naglowek "5. Narzedzia do ogladania modulow"

echo "java --list-modules            - wszystkie moduly platformy"
echo "java --describe-module java.sql - opis jednego modulu"
echo
echo "opis naszego modulu uslug:"
java --module-path "$WYJSCIE" --describe-module com.tomtom.przyklad.uslugi | sed 's/^/  /'
echo
echo "jdeps - analiza zaleznosci, kluczowe narzedzie przy migracji z Javy 8:"
echo "  jdeps --jdk-internals mojaAplikacja.jar    # czy uzywam sun.misc.* ?"
echo "  jdeps --generate-module-info . mojaAplikacja.jar  # szkic module-info"
echo
echo "zaleznosci naszej aplikacji wedlug jdeps:"
jdeps --module-path "$WYJSCIE" "$WYJSCIE/com.tomtom.przyklad.aplikacja" 2>&1 \
    | sed 's/^/  /' | head -20

echo
echo "Gotowe. Katalog out/ mozesz usunac: rm -rf $WYJSCIE"
