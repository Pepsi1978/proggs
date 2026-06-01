#!/usr/bin/env python3
"""Health-Check fuer die Kopplung Bug-Almanach <-> Best-Practices.

Prueft pro Projekt-Code-Software, ob bugs/<software>.md UND
best-practices/projekt-code/<software>/best-practices.md beide die
wechselseitige Bezugs-Tabelle tragen (Marker '🔗' + 'Bezug').

Rein lesend — aendert NICHTS. Gibt einen Klartext-Report aus und endet
IMMER mit exit 0 (auch im Fehlerfall), damit der Check nirgends eine
Session blockiert (Direktive #3, Poka-Yoke). Drift wird per '[DRIFT]'-
Zeilen im Output gemeldet, nicht ueber den Exit-Code.

Aufruf (plattformuebergreifend):  python3 bugs/check-coupling.py
"""
import os
import sys


def has_table(path):
    """True wenn die Datei die Bezugs-Tabelle traegt, None wenn unlesbar."""
    try:
        with open(path, "r", encoding="utf-8") as fh:
            txt = fh.read()
    except Exception:
        return None
    return ("\U0001f517" in txt) and ("Bezug" in txt)  # 🔗 + 'Bezug'


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.dirname(here)  # bugs/ -> Repo-Wurzel
    bugs_dir = os.path.join(repo, "bugs")
    pc_dir = os.path.join(repo, "best-practices", "projekt-code")

    ok, drift, info = [], [], []

    softwares = []
    if os.path.isdir(pc_dir):
        for name in sorted(os.listdir(pc_dir)):
            if os.path.isfile(os.path.join(pc_dir, name, "best-practices.md")):
                softwares.append(name)

    for sw in softwares:
        bp = os.path.join(pc_dir, sw, "best-practices.md")
        bug = os.path.join(bugs_dir, sw + ".md")
        if not os.path.isfile(bug):
            info.append("%s: best-practices vorhanden, aber kein bugs/%s.md "
                        "(noch keine Paarung)" % (sw, sw))
            continue
        missing = []
        if not has_table(bp):
            missing.append("best-practices/projekt-code/%s/best-practices.md" % sw)
        if not has_table(bug):
            missing.append("bugs/%s.md" % sw)
        if missing:
            drift.append("%s: Bezugs-Tabelle FEHLT in: %s" % (sw, ", ".join(missing)))
        else:
            ok.append(sw)

    orphan_bugs = []
    if os.path.isdir(bugs_dir):
        for fn in sorted(os.listdir(bugs_dir)):
            if fn.endswith(".md") and fn not in ("README.md", "SYSTEM.md"):
                sw = fn[:-3]
                if sw not in softwares:
                    orphan_bugs.append(sw)

    print("=== Bug-Almanach <-> Best-Practices Kopplungs-Check ===")
    print("Projekt-Code-Softwares mit Best-Practices: %d" % len(softwares))
    for s in ok:
        print("  [OK]    %s: beide Dateien tragen die Bezugs-Tabelle" % s)
    for d in drift:
        print("  [DRIFT] %s" % d)
    for i in info:
        print("  [INFO]  %s" % i)
    if orphan_bugs:
        print("  [INFO]  Almanache ohne Projekt-Code-Best-Practices "
              "(ok, z.B. Harness/noch nicht recherchiert): %s"
              % ", ".join(orphan_bugs))
    print("")
    if drift:
        print("ERGEBNIS: %d Drift-Fall/-Faelle — Bezugs-Tabelle ergaenzen (siehe oben)."
              % len(drift))
    else:
        print("ERGEBNIS: Keine Drift. Alle gepaarten Dateien sind wechselseitig verlinkt.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # Direktive #3: nie crashen
        print("[check-coupling] unerwarteter Fehler (ignoriert): %s" % exc)
        sys.exit(0)
