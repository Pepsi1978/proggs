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
import subprocess
import sys


def entfernter_code(datei: pathlib.Path):
    """Letzter versionCode der Datei auf origin/main (nach kurzem fetch); None, wenn das nicht geht (fail-open)."""
    try:
        ordner = str(datei.parent)
        wurzel = subprocess.run(["git", "-C", ordner, "rev-parse", "--show-toplevel"], capture_output=True, text=True, timeout=5)
        if wurzel.returncode != 0:
            return None
        pfad = datei.resolve().relative_to(pathlib.Path(wurzel.stdout.strip()).resolve()).as_posix()
        subprocess.run(["git", "-C", ordner, "fetch", "--quiet", "origin", "main"], capture_output=True, timeout=20)
        r = subprocess.run(["git", "-C", ordner, "show", f"origin/main:{pfad}"], capture_output=True, timeout=5)
        if r.returncode != 0:
            return None
        e = json.loads(r.stdout.decode("utf-8-sig")).get("eintraege") or []
        return int(e[-1]["versionCode"]) if e else None
    except Exception:
        return None


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
    # Hat eine parallele Sitzung schon einen höheren Stand gepusht, gäbe der Eintrag beim Rebase einen Konflikt und eine doppelte Nummer.
    entfernt = entfernter_code(datei)
    if entfernt is not None and entfernt > int(letzter["versionCode"]):
        print(f"VERSIONSLOG_FEHLER: origin/main ist schon bei versionCode {entfernt}; zuerst auf origin/main rebasen "
              "(git pull --rebase --autostash bzw. im Worktree git rebase origin/main), dann erneut aufrufen", file=sys.stderr)
        return 4
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
    # build.gradle.kts reads the log via JsonSlurper, which the configuration cache does not track: without this the next
    # build keeps the old versionCode (jarvis-install / apk-update then refuse the APK as outdated).
    import shutil
    shutil.rmtree(pathlib.Path(sys.argv[1]) / ".gradle" / "configuration-cache", ignore_errors=True)
    print(f"VERSIONSLOG_EINTRAG={eintrag['versionCode']} {eintrag['versionName']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
