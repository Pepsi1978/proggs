"""Hängt an app/src/main/assets/versionslog.json genau einen Eintrag an: versionCode = letzter + 1,
versionName letzte Stelle + 1, stand = echte Systemzeit. Liest die Datei unmittelbar vor dem Schreiben,
damit eine parallele Sitzung, die inzwischen selbst einen Eintrag angehängt hat, nicht doppelt nummeriert wird.

Aufruf:  python -X utf8 Werkzeuge/versionslog/eintrag.py <App-Ordner> "<Notiz in einem Satz>"
Ausgabe: VERSIONSLOG_EINTRAG=<versionCode> <versionName>
"""
import datetime
import json
import pathlib
import re
import sys


def main() -> int:
    if len(sys.argv) != 3 or not sys.argv[2].strip():
        print('Aufruf: eintrag.py <App-Ordner> "<Notiz>"', file=sys.stderr)
        return 2
    datei = pathlib.Path(sys.argv[1]) / "app/src/main/assets/versionslog.json"
    # utf-8-sig: Eine von einem Editor mit BOM gespeicherte Datei bleibt lesbar; geschrieben wird ohne BOM (Profilregel 19).
    text = datei.read_text(encoding="utf-8-sig")
    eintraege = json.loads(text).get("eintraege") or []
    if not eintraege:
        print(f"VERSIONSLOG_FEHLER: {datei} hat keine Einträge; den ersten Eintrag von Hand anlegen", file=sys.stderr)
        return 3
    letzter = eintraege[-1]
    teile = str(letzter["versionName"]).split(".")
    m = re.match(r"(\d+)(.*)", teile[-1])
    if not m:
        print(f"VERSIONSLOG_FEHLER: versionName {letzter['versionName']!r} endet nicht auf eine Zahl", file=sys.stderr)
        return 3
    teile[-1] = str(int(m.group(1)) + 1)  # Zusatz wie "-beta" fällt beim neuen Eintrag weg
    eintrag = {
        "versionCode": int(letzter["versionCode"]) + 1,
        "versionName": ".".join(teile),
        "stand": datetime.datetime.now().strftime("%d.%m.%Y, %H:%M Uhr"),
        "notiz": sys.argv[2].strip(),
    }
    # Hinter den letzten Eintrag einfügen, Einrückung der Vorgängerzeile übernehmen, Rest der Datei unverändert.
    ende = text.rindex("}", 0, text.rindex("]"))
    zeile = text.rfind("\n", 0, ende)
    einrueckung = re.match(r"[ \t]*", text[zeile + 1:]).group(0)
    neu = text[: ende + 1] + ",\n" + einrueckung + json.dumps(eintrag, ensure_ascii=False) + text[ende + 1:]
    json.loads(neu)  # gültiges JSON, sonst Abbruch ohne Schreiben
    datei.write_text(neu, encoding="utf-8", newline="")
    print(f"VERSIONSLOG_EINTRAG={eintrag['versionCode']} {eintrag['versionName']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
