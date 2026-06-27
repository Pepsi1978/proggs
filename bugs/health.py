#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""bugs/health.py — gebuendelter Self-Test des Bug-Almanach-/Harness-Systems.

Buendelt die einzelnen Selbsttests in EINEN Lauf:
  1. check-coupling.py        — Almanach <-> Best-Practices-Kopplung (Bezugstabellen)
  2. check-guard-coverage.py  — jeder Almanach vom bug-almanac-guard erzwungen?
  3. check-version-anchor.py  — software-gebundene Almanache: Anker-Feld da + Live-Version passt?
  4. check-dead-paths.py      — tote Pfad-Erwaehnungen (Links UND Backtick-Pfade) in bugs/+best-practices/
  5. Stand-Verfall            — Almanache, deren 'Stand:'-Datum zu alt ist (Re-Recherche faellig)

Rein lesend — aendert NICHTS. Endet IMMER mit exit 0 (blockiert nirgends eine Session).
Aufrufbar manuell (`python bugs/health.py`) ODER aus einem Hook (SessionStart/PreCommit):
mit `--quiet` gibt es nur bei PROBLEMEN Output (sonst still), damit Hooks nicht spammen.

Cross-Platform (Windows/macOS/Linux): nur stdlib, alle Pfade ueber __file__.
"""
import os
import re
import sys
import glob
import subprocess
from datetime import date

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

BUGS = os.path.dirname(os.path.abspath(__file__))
STAND_MAX_AGE_DAYS = 180  # Almanach gilt ab hier als re-recherche-faellig


def run_subcheck(script):
    """Fuehrt einen Self-Test als Subprozess aus. Gibt (ok, summary_line) zurueck."""
    path = os.path.join(BUGS, script)
    if not os.path.isfile(path):
        return None, f"{script}: NICHT GEFUNDEN"
    try:
        r = subprocess.run(
            [sys.executable, path],
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=90,
        )
        out = (r.stdout or "") + (r.stderr or "")
        # Kompakte Summary: letzte nicht-leere Zeile, plus echte Problem-Marker.
        # Praezise Muster: "LUECKEN: 0" / "FEHLER: 0" sind KEINE Probleme (nur >=1 zaehlt).
        lines = [l for l in out.splitlines() if l.strip()]
        problems = [l for l in lines if re.search(
            r"(LUECKEN|FEHLER|FEHLT|MISMATCH|DRIFT|ANKER)\s*:?\s*[1-9]"
            r"|\b[1-9]\d*\s+(Drift|Luecke|Fehler|Fall|Faell|Mismatch|Anker-Problem|tote)"
            r"|\[LUECKE\]|\[FEHLT\]|\[!\]|✗", l, re.I)]
        tail = lines[-1] if lines else "(keine Ausgabe)"
        ok = (r.returncode == 0) and not problems
        if problems:
            return False, f"{script}: {len(problems)} Problem(e) — z.B. {problems[0][:100]}"
        return ok, f"{script}: OK ({tail[:80]})"
    except Exception as e:
        return None, f"{script}: Ausfuehrungsfehler — {e}"


def check_stand_verfall():
    """Almanache mit zu altem 'Stand:'-Datum. Gibt Liste (rel_pfad, alter_tage)."""
    stale = []
    today = date.today()
    for md in glob.glob(os.path.join(BUGS, "**", "*.md"), recursive=True):
        base = os.path.basename(md).lower()
        if base in ("readme.md", "system.md") or "audit" in base or base.endswith("-kurzcheck.md"):
            continue
        try:
            with open(md, encoding="utf-8") as f:
                head = f.read(2500)
        except Exception:
            continue
        m = re.search(r"Stand[:*\s]+[^\n]*?(\d{4})-(\d{2})-(\d{2})", head)
        if not m:
            continue
        try:
            d = date(int(m.group(1)), int(m.group(2)), int(m.group(3)))
        except ValueError:
            continue
        age = (today - d).days
        if age > STAND_MAX_AGE_DAYS:
            stale.append((os.path.relpath(md, BUGS).replace("\\", "/"), age))
    stale.sort(key=lambda t: -t[1])
    return stale


def main():
    quiet = "--quiet" in sys.argv[1:]
    report = []
    problems = 0

    for script in ("check-coupling.py", "check-guard-coverage.py", "check-version-anchor.py", "check-dead-paths.py"):
        ok, summary = run_subcheck(script)
        report.append(("ok" if ok else ("warn" if ok is False else "skip"), summary))
        if ok is False:
            problems += 1

    stale = check_stand_verfall()
    if stale:
        problems += 1
        top = ", ".join(f"{p} ({a}d)" for p, a in stale[:3])
        report.append(("warn", f"Stand-Verfall: {len(stale)} Almanach(e) > {STAND_MAX_AGE_DAYS}d — z.B. {top}"))
    else:
        report.append(("ok", f"Stand-Verfall: alle Almanache <= {STAND_MAX_AGE_DAYS}d"))

    if quiet and problems == 0:
        return 0

    print("=== bugs/health.py — Harness-/Almanach-Selbsttest ===")
    for status, line in report:
        mark = {"ok": "[OK]  ", "warn": "[WARN]", "skip": "[--]  "}.get(status, "[?]   ")
        print(f"{mark} {line}")
    print(f"--- {problems} Bereich(e) mit Problemen ---" if problems else "--- alles gruen ---")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        try:
            print(f"health.py: unerwarteter Fehler — {e}")
        except Exception:
            pass
        sys.exit(0)
