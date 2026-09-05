# -*- coding: utf-8 -*-
"""Baut die drei JSON-Beigaben der App und traegt die Versionen aus dem Changelog ein.

ACHTUNG — nicht mehr die Quelle der Auslieferung. Die Beigaben stammen seit dem Stand
2.1.261 aus dem gewachsenen Wissen der App selbst; erzeugt werden sie von
`assets_aus_datenbank.py`. Dieses Skript hier baut nur noch die alte, handgepflegte
Grundfassung (Stand 2.1.251) und wuerde die Beigaben beim Ausfuehren zurueckwerfen.
Es bleibt als Herkunftsnachweis liegen — vor einem Lauf bitte pruefen, ob wirklich der
alte Stand gewollt ist.
"""
import io
import json
import os
import re
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)

from daten_slash_a_e import SLASH_A_E
from daten_slash_f_p import SLASH_F_P
from daten_slash_q_z import SLASH_Q_Z, SLASH_ENTFERNT
from daten_slash_rest import SLASH_REST
from daten_config_kern import CONFIG_KERN
from daten_config_rest1 import CONFIG_REST_1
from daten_config_rest2 import CONFIG_REST_2
from daten_envvars import ENV_VARS
from daten_praxis import PRAXIS
from echte_umlaute import wandle_text

CHANGELOG = os.path.join(HIER, "cc-changelog.md")
ZIEL = sys.argv[1] if len(sys.argv) > 1 else "."

# --- Changelog in Versions-Abschnitte zerlegen -------------------------------------------
text = io.open(CHANGELOG, encoding="utf-8").read()
marken = [(m.group(1), m.start()) for m in
          re.finditer(r"(?m)^##\s+([0-9]+\.[0-9]+\.[0-9]+)\s*$", text)]
marken.append(("", len(text)))
abschnitte = [(marken[i][0], text[marken[i][1]:marken[i + 1][1]])
              for i in range(len(marken) - 1)]

def vkey(v):
    return tuple(int(x) for x in v.split("."))

def suche(muster, nur_neu=False, nur_weg=False):
    """Findet Versionen, deren Abschnitt eine passende Aufzaehlungszeile enthaelt."""
    rx = re.compile(muster)
    treffer = []
    for version, koerper in abschnitte:
        for zeile in koerper.split("\n"):
            gestutzt = zeile.strip()
            if not gestutzt.startswith("-"):
                continue
            if nur_neu and not re.search(r"\b(Added|New|Introduced|Renamed)\b", gestutzt):
                continue
            if nur_weg and not re.search(r"\b(Removed|Deprecated|Retired|Deleted)\b", gestutzt):
                continue
            if rx.search(gestutzt):
                treffer.append((vkey(version), version, gestutzt[:220]))
                break
    return treffer

def seit_version(name, ist_slash):
    """Die aelteste Version, in der der Name auftaucht — das ist sein Einzug."""
    if ist_slash:
        muster = r"`?/" + re.escape(name.lstrip("/")) + r"(?![a-zA-Z0-9-])"
    else:
        muster = r"`" + re.escape(name) + r"`"
    treffer = suche(muster, nur_neu=True) or suche(muster)
    if not treffer:
        return "", ""
    treffer.sort()
    return treffer[0][1], treffer[0][2]

def entfernt_in(name, ist_slash):
    if ist_slash:
        muster = r"`?/" + re.escape(name.lstrip("/")) + r"(?![a-zA-Z0-9-])"
    else:
        muster = r"`" + re.escape(name) + r"`"
    treffer = suche(muster, nur_weg=True)
    if not treffer:
        return "", ""
    treffer.sort(reverse=True)
    return treffer[0][1], treffer[0][2]

def sortier(name):
    return name.lstrip("/").lower()

# --- Slash-Befehle -------------------------------------------------------------------------
slash = []
gesehen = set()
for name, kategorie, art, kurz, englisch, erklaerung in (
        SLASH_A_E + SLASH_F_P + SLASH_Q_Z + SLASH_REST):
    if name in gesehen:
        raise SystemExit("Doppelter Slash-Befehl: " + name)
    gesehen.add(name)
    version, beleg = seit_version(name, True)
    slash.append({
        "name": name,
        "kategorie": kategorie,
        "art": art,
        "kurz": kurz,
        "englisch": englisch,
        "erklaerung": erklaerung,
        "seit": version,
        "seitBeleg": beleg,
        "sortierName": sortier(name),
        "entfernt": False,
    })

for name, kategorie, kurz, entfernt, ersatz, erklaerung in SLASH_ENTFERNT:
    version, beleg = seit_version(name, True)
    weg_version, weg_beleg = entfernt_in(name, True)
    slash.append({
        "name": name,
        "kategorie": kategorie,
        "art": "Entfernt",
        "kurz": kurz,
        "englisch": "",
        "erklaerung": erklaerung,
        "seit": version,
        "seitBeleg": beleg,
        "sortierName": sortier(name),
        "entfernt": True,
        "entferntIn": entfernt or weg_version,
        "entferntBeleg": weg_beleg,
        "ersatz": ersatz,
    })

slash.sort(key=lambda e: e["sortierName"])

# --- Config: Einstellungen und Umgebungsvariablen ------------------------------------------
config = []
gesehen = set()
for name, kategorie, quelle, kurz, englisch, erklaerung in (
        CONFIG_KERN + CONFIG_REST_1 + CONFIG_REST_2):
    if name in gesehen:
        raise SystemExit("Doppelte Einstellung: " + name)
    gesehen.add(name)
    version, beleg = seit_version(name, False)
    config.append({
        "name": name,
        "kategorie": kategorie,
        "art": quelle,
        "kurz": kurz,
        "englisch": englisch,
        "erklaerung": erklaerung,
        "seit": version,
        "seitBeleg": beleg,
        "sortierName": name.lower(),
        "entfernt": False,
    })

for name, kategorie, kurz, englisch, erklaerung in ENV_VARS:
    if name in gesehen:
        raise SystemExit("Doppelte Variable: " + name)
    gesehen.add(name)
    version, beleg = seit_version(name, False)
    config.append({
        "name": name,
        "kategorie": kategorie,
        "art": "Umgebungsvariable",
        "kurz": kurz,
        "englisch": englisch,
        "erklaerung": erklaerung,
        "seit": version,
        "seitBeleg": beleg,
        "sortierName": name.lower(),
        "entfernt": False,
    })

# Einstellungen, die es gab und die weggefallen sind.
CONFIG_ENTFERNT = [
    ("teammateDefaultModel", "Zusammenarbeit", "2.1.234",
     "Legte fest, mit welchem Modell die Mitglieder eines Agenten-Teams arbeiten.",
     "Ersatzlos entfallen: Die Mitglieder eines Teams folgen jetzt automatisch dem Modell der "
     "leitenden Sitzung. Wer sie auf einem eigenen Modell haben will, gibt es beim Anlegen des "
     "jeweiligen Agenten an.",
     "Frueher konnte man ein eigenes Modell fuer die Mitglieder eines Agenten-Teams "
     "einstellen.\n\n"
     "In Version 2.1.234 ist die Einstellung entfallen. Die Mitglieder nehmen jetzt dasselbe "
     "Modell wie die leitende Sitzung.\n\n"
     "Das ist in den meisten Faellen auch das, was man will: Ein Team, in dem die Mitglieder "
     "unterschiedlich stark denken, liefert schwer vergleichbare Ergebnisse.\n\n"
     "Steht der Eintrag noch in deiner Einstellungsdatei, schadet er nicht — er wirkt nur nicht "
     "mehr."),
    ("disableArtifact", "Erweiterungen", "",
     "Schaltete das Artefakt-Werkzeug ab.",
     "Ersetzt durch `enableArtifact`. Der neue Weg ist strenger: Ein `false` aus JEDER Ebene "
     "schaltet ab, und keine Ebene kann es wieder einschalten. Braucht Claude Code 2.1.242 oder "
     "neuer.",
     "Mit dieser Einstellung liess sich das Veroeffentlichen von Artefakten abschalten.\n\n"
     "Sie gilt als veraltet. An ihre Stelle ist `enableArtifact` getreten.\n\n"
     "Der Unterschied ist wichtig: Beim neuen Weg schaltet ein `false` aus jeder beliebigen "
     "Ebene ab, und nichts kann es wieder einschalten. Damit laesst sich die Sperre nicht mehr "
     "versehentlich aufweichen.\n\n"
     "Der alte Name wird weiterhin beachtet, damit bestehende Einrichtungen nicht plötzlich "
     "wieder veroeffentlichen koennen."),
    ("includeCoAuthoredBy", "Git und Herkunft", "",
     "Bestimmte, ob Claude eine Mitautoren-Zeile an Commits anhaengt.",
     "Ersetzt durch `attribution`. Der neue Weg kann mehr: Er trennt zwischen Commit und Pull "
     "Request und erlaubt es, den Text zu aendern statt ihn nur wegzulassen.",
     "Frueher gab es nur ein Ja oder Nein zur Mitautoren-Zeile im Commit.\n\n"
     "Die Einstellung `attribution` hat das abgeloest und ist feiner: Sie unterscheidet "
     "zwischen Commit und Pull Request, erlaubt eigenen Text und kann den Sitzungslink "
     "getrennt weglassen.\n\n"
     "Gerade der Sitzungslink ist in einem oeffentlichen Projekt oft unerwuenscht — mit der "
     "alten Einstellung liess er sich nicht einzeln abschalten.\n\n"
     "Der alte Eintrag wird noch beachtet, sollte aber ersetzt werden."),
]
for name, kategorie, entfernt, kurz, ersatz, erklaerung in CONFIG_ENTFERNT:
    version, beleg = seit_version(name, False)
    weg_version, weg_beleg = entfernt_in(name, False)
    config.append({
        "name": name,
        "kategorie": kategorie,
        "art": "Entfernt",
        "kurz": kurz,
        "englisch": "",
        "erklaerung": erklaerung,
        "seit": version,
        "seitBeleg": beleg,
        "sortierName": name.lower(),
        "entfernt": True,
        "entferntIn": entfernt or weg_version,
        "entferntBeleg": weg_beleg,
        "ersatz": ersatz,
    })

config.sort(key=lambda e: e["sortierName"])

# --- Best Practices ------------------------------------------------------------------------
praxis = []
for index, (titel, kategorie, kurz, erklaerung) in enumerate(PRAXIS):
    praxis.append({
        "name": titel,
        "kategorie": kategorie,
        "art": "Best Practice",
        "kurz": kurz,
        "englisch": "",
        "erklaerung": erklaerung,
        "seit": "",
        "seitBeleg": "",
        # Best Practices stehen nach Kategorie und dann nach der gedachten Lesereihenfolge,
        # nicht alphabetisch: Reihenfolge ist hier Inhalt.
        "sortierName": "%s %03d" % (kategorie.lower(), index),
        "entfernt": False,
    })

# --- Schreiben ---------------------------------------------------------------------------
# Die deutschen Felder bekommen echte Umlaute. Bezeichner (`name`), die englischen
# Originalbeschreibungen und die Changelog-Belege bleiben unberuehrt — dort waere eine
# Umwandlung schlicht falsch.
DEUTSCHE_FELDER = ("kurz", "erklaerung", "kategorie", "ersatz")


def setze_umlaute(eintraege, auch_name=False):
    """Wandelt die deutschen Felder. [auch_name] gilt nur fuer die Best Practices, deren
    `name` ein Artikeltitel ist — bei Slash und Config waere er ein Bezeichner."""
    protokoll = {}
    for eintrag in eintraege:
        felder = DEUTSCHE_FELDER + ("name",) if auch_name else DEUTSCHE_FELDER
        for feld in felder:
            if eintrag.get(feld):
                eintrag[feld] = wandle_text(eintrag[feld], protokoll)
    return protokoll


def schreibe(dateiname, eintraege, bereich):
    pfad = os.path.join(ZIEL, dateiname)
    ersetzt = setze_umlaute(eintraege, auch_name=(bereich == "praxis"))
    inhalt = {
        "bereich": bereich,
        "standVersion": "2.1.251",
        "eintraege": eintraege,
    }
    io.open(pfad, "w", encoding="utf-8", newline="\n").write(
        json.dumps(inhalt, ensure_ascii=False, indent=1)
    )
    mit_version = sum(1 for e in eintraege if e["seit"])
    print("%-26s %4d Eintraege, %3d mit belegter Version, %3d Woerter auf echte Umlaute"
          % (dateiname, len(eintraege), mit_version, len(ersetzt)))
    return ersetzt

alle_ersetzungen = {}
alle_ersetzungen.update(schreibe("slash_befehle.json", slash, "slash"))
alle_ersetzungen.update(schreibe("config_einstellungen.json", config, "config"))
alle_ersetzungen.update(schreibe("best_practices.json", praxis, "praxis"))

print()
print("Auf echte Umlaute gesetzt (%d verschiedene Woerter):" % len(alle_ersetzungen))
for alt in sorted(alle_ersetzungen):
    print("  %-30s -> %s" % (alt, alle_ersetzungen[alt]))
