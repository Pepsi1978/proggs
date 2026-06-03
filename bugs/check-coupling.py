#!/usr/bin/env python3
"""Health-Check fuer die Kopplung Bug-Almanach <-> Best-Practices.

Prueft pro Projekt-Code-Software, ob der Bug-Almanach UND die Best-Practices-Datei
beide die wechselseitige Bezugs-Tabelle tragen (Marker '🔗' + 'Bezug').

Kategorie-bewusst (2026-06-03): Beide Seiten liegen in Kategorie-Ordnern, die Datei
traegt den Software-Namen selbst (selbst-identifizierend, kein generischer Dateiname):
  - Almanach:        bugs/<kategorie>/<software>.md
  - Best-Practices:  best-practices/projekt-code/<kategorie>/best-practices-<software>.md
Die Paarung erfolgt ueber den <software>-Namen (rekursiv gesucht), NICHT ueber die
Kategorie — so bleibt der Check robust, falls eine Software die Kategorie wechselt.

Rein lesend — aendert NICHTS. Gibt einen Klartext-Report aus und endet IMMER mit
exit 0 (auch im Fehlerfall), damit der Check nirgends eine Session blockiert
(Direktive #3, Poka-Yoke). Drift wird per '[DRIFT]'-Zeilen im Output gemeldet,
nicht ueber den Exit-Code.

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


def rel(repo, path):
    """Pfad relativ zur Repo-Wurzel, mit Forward-Slashes (fuer den Report)."""
    return os.path.relpath(path, repo).replace(os.sep, "/")


def find_best_practices(pc_dir):
    """software-name -> Pfad der best-practices-<software>.md (rekursiv in projekt-code/).

    Der Software-Name steht selbst-identifizierend im Dateinamen (best-practices-<software>.md),
    direkt im Kategorie-Ordner — kein generischer 'best-practices.md' mehr, kein Software-Unterordner.
    """
    result = {}
    if not os.path.isdir(pc_dir):
        return result
    prefix, suffix = "best-practices-", ".md"
    for root, _dirs, files in os.walk(pc_dir):
        for fn in files:
            if fn.startswith(prefix) and fn.endswith(suffix):
                sw = fn[len(prefix):-len(suffix)]
                if sw:
                    result[sw] = os.path.join(root, fn)
    return result


def find_almanachs(bugs_dir):
    """software-name -> Pfad des Almanachs (rekursiv in bugs/, nur Kategorie-Unterordner).

    Dateien direkt in bugs/ (README/SYSTEM/OFFENE-*) sind keine Almanache und werden
    uebersprungen.
    """
    result = {}
    if not os.path.isdir(bugs_dir):
        return result
    bugs_abs = os.path.abspath(bugs_dir)
    for root, _dirs, files in os.walk(bugs_dir):
        if os.path.abspath(root) == bugs_abs:
            continue  # Root-Dateien ueberspringen (nur Unterordner = Almanache)
        for fn in files:
            if fn.endswith(".md"):
                result[fn[:-3]] = os.path.join(root, fn)
    return result


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.dirname(here)  # bugs/ -> Repo-Wurzel
    bugs_dir = os.path.join(repo, "bugs")
    pc_dir = os.path.join(repo, "best-practices", "projekt-code")

    bp_map = find_best_practices(pc_dir)
    bug_map = find_almanachs(bugs_dir)

    ok, drift, info = [], [], []

    for sw in sorted(bp_map):
        bp = bp_map[sw]
        bug = bug_map.get(sw)
        if not bug:
            info.append("%s: best-practices vorhanden (%s), aber kein bugs/<kategorie>/%s.md "
                        "(noch keine Paarung)" % (sw, rel(repo, bp), sw))
            continue
        missing = []
        if not has_table(bp):
            missing.append(rel(repo, bp))
        if not has_table(bug):
            missing.append(rel(repo, bug))
        if missing:
            drift.append("%s: Bezugs-Tabelle FEHLT in: %s" % (sw, ", ".join(missing)))
        else:
            ok.append(sw)

    # Almanache ohne Projekt-Code-Best-Practices (ok, z.B. Harness/noch nicht recherchiert)
    orphan_bugs = sorted(sw for sw in bug_map if sw not in bp_map)

    print("=== Bug-Almanach <-> Best-Practices Kopplungs-Check ===")
    print("Projekt-Code-Softwares mit Best-Practices: %d" % len(bp_map))
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
