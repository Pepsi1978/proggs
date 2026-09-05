# -*- coding: utf-8 -*-
"""Erzeugt die drei JSON-Beigaben aus einer vom Geraet gezogenen App-Datenbank.

Warum es das gibt: `baue_assets.py` baut die Auslieferung aus den handgepflegten
Python-Datendateien und bleibt damit auf dem Stand stehen, den diese Dateien haben.
Die App selbst laeuft aber weiter — der Aktualisieren-Knopf holt neue Eintraege und
schreibt deutsche Erklaerungen dazu. Nach einer Weile ist die Datenbank auf dem Geraet
der bessere Wissensstand als die Beigabe. Frisch installiert muesste die App dann
hunderte Eintraege nachziehen, was lange dauert.

Dieses Werkzeug dreht die Richtung um: Es nimmt den gewachsenen Stand aus der
Datenbank und macht ihn zur neuen Auslieferung.

Benutzung:
    adb exec-out "run-as de.frank.claudekompass cat databases/claude-kompass.db" > db
    (dazu auch die Dateien db-wal und db-shm ziehen, sonst fehlen die letzten Aenderungen)
    python tools/assets_aus_datenbank.py <db-pfad> [ziel-ordner]

Der Changelog-Beleg (`seitBeleg`) steht nicht in der Datenbank — die App speichert ihn
nicht. Er wird deshalb aus den vorhandenen Beigaben uebernommen, soweit der Name passt.
"""
import io
import json
import os
import sqlite3
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
STANDARD_ZIEL = os.path.join(HIER, "..", "app", "src", "main", "assets")

DATEIEN = (
    ("slash", "slash_befehle.json"),
    ("config", "config_einstellungen.json"),
    ("praxis", "best_practices.json"),
)


def lese_belege(ziel):
    """Die alten Changelog-Belege, damit sie beim Umbau nicht verloren gehen."""
    belege = {}
    for bereich, datei in DATEIEN:
        pfad = os.path.join(ziel, datei)
        if not os.path.exists(pfad):
            continue
        alt = json.load(io.open(pfad, encoding="utf-8"))
        for eintrag in alt.get("eintraege", []):
            if eintrag.get("seitBeleg"):
                belege["%s:%s" % (bereich, eintrag["name"])] = eintrag["seitBeleg"]
    return belege


def lese_datenbank(pfad):
    verbindung = sqlite3.connect(pfad)
    verbindung.row_factory = sqlite3.Row
    zeilen = [dict(z) for z in verbindung.execute("SELECT * FROM eintraege")]
    stand = ""
    for zeile in verbindung.execute(
        "SELECT cliVersion FROM aktualisierungen WHERE status = 'fertig' AND cliVersion <> ''"
        " ORDER BY id DESC LIMIT 1"
    ):
        stand = zeile["cliVersion"]
    verbindung.close()
    return zeilen, stand


def main():
    db_pfad = sys.argv[1]
    ziel = os.path.abspath(sys.argv[2] if len(sys.argv) > 2 else STANDARD_ZIEL)
    zeilen, stand_aus_db = lese_datenbank(db_pfad)
    stand = sys.argv[3] if len(sys.argv) > 3 else stand_aus_db
    if not stand:
        raise SystemExit("Kein Stand gefunden — bitte als drittes Argument angeben.")
    belege = lese_belege(ziel)

    for bereich, datei in DATEIEN:
        eigene = [z for z in zeilen if z["bereich"] == bereich]
        # Dieselbe Reihenfolge wie in der App: erst der Bestand, dann das Entfernte.
        eigene.sort(key=lambda z: (bool(z["entfernt"]), z["sortierName"], z["name"]))
        eintraege = []
        for zeile in eigene:
            eintraege.append({
                "name": zeile["name"],
                "kategorie": zeile["kategorie"] or "",
                "art": zeile["art"] or "",
                "kurz": zeile["kurz"] or "",
                "englisch": zeile["quelleEnglisch"] or "",
                "erklaerung": zeile["erklaerung"] or "",
                "seit": zeile["seitVersion"] or "",
                "seitBeleg": belege.get("%s:%s" % (bereich, zeile["name"]), ""),
                "sortierName": zeile["sortierName"] or zeile["name"].lstrip("/").lower(),
                "entfernt": bool(zeile["entfernt"]),
                "entferntIn": zeile["entferntInVersion"] or "",
                "ersatz": zeile["ersatz"] or "",
            })
        inhalt = {"bereich": bereich, "standVersion": stand, "eintraege": eintraege}
        io.open(os.path.join(ziel, datei), "w", encoding="utf-8", newline="\n").write(
            json.dumps(inhalt, ensure_ascii=False, indent=1)
        )
        print("%-26s %4d Eintraege (%d entfernt), Stand %s"
              % (datei, len(eintraege), sum(1 for e in eintraege if e["entfernt"]), stand))


if __name__ == "__main__":
    main()
