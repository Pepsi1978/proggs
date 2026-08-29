# -*- coding: utf-8 -*-
"""Setzt echte Umlaute in den Kommentaren der Kotlin-Dateien.

Drei Dinge bleiben dabei ausdruecklich unberuehrt:

  * Bezeichner im Code selbst (`eintraege`, `laedt`, …). Die duerfen laut Regel ASCII sein,
    und sie umzubenennen waere eine Aenderung am Programm, nicht an seiner Beschreibung.
  * Verweise in eckigen Klammern und Rueckstrichen (`[laedt]`, `` `laeuft` ``). Sie zeigen
    auf genau diese Bezeichner — mit Umlaut zeigten sie ins Leere.
  * Englische Woerter.

Uebrig bleibt der deutsche Fliesstext, und genau der bekommt seine Umlaute.
"""
import glob
import io
import re
import sys

sys.path.insert(0, __file__.rsplit("\\", 1)[0].rsplit("/", 1)[0])
from echte_umlaute import wandle_wort  # noqa: E402

# Kommentarzeilen und -bloecke.
KOMMENTAR = re.compile(r"(//[^\n]*)|(/\*[\s\S]*?\*/)")
# Was in einem Kommentar auf Code zeigt und deshalb wortgleich bleiben muss.
VERWEIS = re.compile(r"\[[^\]\n]+\]|`[^`\n]+`")
WORT = re.compile(r"[A-Za-zÄÖÜäöüß]+")


def sammle_bezeichner(dateien):
    """Alle im Code deklarierten Namen — auf die darf ein Kommentar verweisen."""
    namen = set()
    muster = re.compile(
        r"\b(?:val|var|fun|class|object|interface|enum class|data class)\s+([A-Za-z_][A-Za-z0-9_]*)"
        r"|([A-Za-z_][A-Za-z0-9_]*)\s*:\s*[A-Z]"
    )
    for pfad in dateien:
        inhalt = io.open(pfad, encoding="utf-8").read()
        for treffer in muster.finditer(inhalt):
            for gruppe in treffer.groups():
                if gruppe:
                    namen.add(gruppe)
    return namen


def wandle_kommentar(text, bezeichner, protokoll):
    """Ersetzt im Kommentartext, laesst Verweise als Ganzes stehen."""
    stuecke = []
    letzte = 0
    for verweis in VERWEIS.finditer(text):
        stuecke.append(wandle_fliesstext(text[letzte:verweis.start()], bezeichner, protokoll))
        stuecke.append(verweis.group(0))  # unveraendert
        letzte = verweis.end()
    stuecke.append(wandle_fliesstext(text[letzte:], bezeichner, protokoll))
    return "".join(stuecke)


def wandle_fliesstext(text, bezeichner, protokoll):
    def ersetze(treffer):
        alt = treffer.group(0)
        if alt in bezeichner:
            return alt
        neu = wandle_wort(alt)
        if neu != alt:
            protokoll[alt] = neu
        return neu
    return WORT.sub(ersetze, text)


if __name__ == "__main__":
    dateien = sorted(glob.glob(sys.argv[1], recursive=True))
    bezeichner = sammle_bezeichner(dateien)
    protokoll = {}
    geaendert = 0

    for pfad in dateien:
        inhalt = io.open(pfad, encoding="utf-8").read()
        neu = KOMMENTAR.sub(
            lambda t: wandle_kommentar(t.group(0), bezeichner, protokoll),
            inhalt,
        )
        if neu != inhalt:
            io.open(pfad, "w", encoding="utf-8", newline="\n").write(neu)
            geaendert += 1

    print("Dateien geaendert: %d von %d | Bezeichner geschuetzt: %d"
          % (geaendert, len(dateien), len(bezeichner)))
    print("Woerter mit echten Umlauten: %d" % len(protokoll))
    for alt in sorted(protokoll):
        print("  %-26s -> %s" % (alt, protokoll[alt]))
