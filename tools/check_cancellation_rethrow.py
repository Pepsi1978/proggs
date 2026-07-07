#!/usr/bin/env python3
"""Pre-Commit-Guard (Poka-Yoke Stufe 2): CancellationException darf nie verschluckt werden.

Hintergrund (CortexAndroid-Debugging 2026-07-02): `catch (e: Exception)` in Coroutine-Code
faengt auch kotlinx.coroutines.CancellationException und bricht damit die strukturierte
Cancellation (Almanach bugs/android/kotlin.md 2.1). Der Fehler war mit 29 Stellen der
haeufigste Fund des Debugging-Durchlaufs — dieses Skript verhindert ihn beim Committen.

Regel: In Kotlin-Dateien, die kotlinx.coroutines importieren, muss vor jedem
`catch (e: Exception)` / `catch (_: Exception)` ein CancellationException-Rethrow stehen
(innerhalb der 4 Zeilen davor), ODER die catch-Zeile traegt den bewussten Marker
`// no-cancellation-rethrow` (fuer nachweislich nicht-suspendierende try-Bloecke).

Aufruf:  python3 tools/check_cancellation_rethrow.py <datei.kt> [...]
Exit 0 = sauber, Exit 1 = Verstoss (Stellen werden gelistet).
"""
import re
import sys

CATCH_RE = re.compile(r"^\s*\}?\s*catch\s*\(\s*(?:\w+|_)\s*:\s*(?:kotlin\.)?Exception\s*\)")
MARKER = "no-cancellation-rethrow"
LOOKBACK = 4


def check_file(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            lines = fh.readlines()
    except OSError:
        return []
    content = "".join(lines)
    if "kotlinx.coroutines" not in content:
        return []   # Datei ohne Coroutine-Bezug: Regel greift nicht
    violations = []
    for i, line in enumerate(lines):
        if not CATCH_RE.search(line):
            continue
        if MARKER in line:
            continue
        window = "".join(lines[max(0, i - LOOKBACK):i])
        if "CancellationException" in window:
            continue
        violations.append((i + 1, line.strip()))
    return violations


def main():
    bad = False
    for path in sys.argv[1:]:
        if not path.endswith(".kt"):
            continue
        for lineno, snippet in check_file(path):
            bad = True
            print(f"{path}:{lineno}: catch (Exception) ohne CancellationException-Rethrow")
            print(f"    {snippet}")
    if bad:
        print()
        print("FIX: davor einfuegen:   } catch (e: CancellationException) { throw e }")
        print("     (Almanach bugs/android/kotlin.md 2.1) — oder bei bewusst nicht-suspendierendem")
        print("     try-Block den Marker anhaengen:   // no-cancellation-rethrow")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
