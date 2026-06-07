#!/usr/bin/env python3
"""Wertet das Block-Log des bug-almanac-guard aus und schlaegt Almanach-Kandidaten vor.

Der Guard protokolliert jeden Block nach `~/.claude/state/bug-almanac-blocks.log`
(eine Zeile je Block: `YYYY-MM-DD HH:MM <slug> [marker]`). Drei Block-Arten — der
Marker sagt bereits alles, ohne dass dieses Skript ein slug->Datei-Mapping
duplizieren muss:

  - `(kein-almanach)`   -> ein Bereich wurde bearbeitet, fuer den (noch) KEIN Almanach
                           existiert  -> STARKER Kandidat fuer `bug-almanach-recherche`.
  - `(best-practices)`  -> Almanach existiert + gelesen, Best-Practices-Pflicht.
  - (kein Marker)       -> normaler Almanach-Lese-Block (Almanach existiert).

Auswertung: Welche Bereiche werden am haeufigsten angefasst (= wichtigste Almanache),
und welche brauchen ueberhaupt erst einen Almanach? Rein lesend, keine Seiteneffekte.

Aufruf:
  python3 analyze-blocks.py            # voller Bericht
  python3 analyze-blocks.py --top 10   # nur die Top-N aktivsten Bereiche

Cross-Platform, Encoding-sicher (siehe best-practices-python-windows.md §1, §3).
"""
from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
except Exception:
    pass

LOG_PATH = Path.home() / ".claude" / "state" / "bug-almanac-blocks.log"

# "YYYY-MM-DD HH:MM <slug> [optionaler (marker)]"
_LINE = re.compile(r"^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})\s+(\S+)(?:\s+\((.+)\))?\s*$")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Block-Log auswerten, Almanach-Kandidaten vorschlagen.")
    parser.add_argument("--top", type=int, default=0, help="Nur die Top-N aktivsten Bereiche zeigen (0 = alle).")
    parser.add_argument("--path", type=Path, default=LOG_PATH, help="Pfad zum Block-Log.")
    args = parser.parse_args(argv)

    if not args.path.exists():
        print(f"Kein Block-Log gefunden ({args.path}). Es entsteht, sobald der Guard das erste Mal blockt.")
        return 0

    total = defaultdict(int)        # slug -> Gesamtzahl Blocks
    kein_almanach = defaultdict(int)  # slug -> Anzahl kein-almanach-Blocks
    best_practices = defaultdict(int)  # slug -> Anzahl best-practices-Blocks
    normal = defaultdict(int)        # slug -> Anzahl normale Almanach-Blocks
    last_seen: dict[str, str] = {}   # slug -> letztes Datum
    parsed = 0

    for raw in args.path.read_text(encoding="utf-8-sig").splitlines():
        if not raw.strip():
            continue
        m = _LINE.match(raw.strip())
        if not m:
            continue
        parsed += 1
        date, _time, slug, marker = m.group(1), m.group(2), m.group(3), m.group(4)
        total[slug] += 1
        last_seen[slug] = max(date, last_seen.get(slug, ""))
        if marker == "kein-almanach":
            kein_almanach[slug] += 1
        elif marker == "best-practices":
            best_practices[slug] += 1
        else:
            normal[slug] += 1

    if parsed == 0:
        print("Block-Log vorhanden, aber keine auswertbaren Zeilen.")
        return 0

    print(f"Block-Log: {parsed} Blocks ueber {len(total)} Bereiche ({args.path})\n")

    # 1. Almanach-Kandidaten: Bereiche mit kein-almanach-Blocks (brauchen einen Almanach).
    if kein_almanach:
        print("ALMANACH-KANDIDATEN (bearbeitet, aber KEIN Almanach -> bug-almanach-recherche erwaegen):")
        for slug, cnt in sorted(kein_almanach.items(), key=lambda kv: -kv[1]):
            print(f"  {cnt:4d}x  {slug}   (zuletzt {last_seen[slug]})")
        print()
    else:
        print("ALMANACH-KANDIDATEN: keine -- jeder bearbeitete Bereich hatte einen Almanach. (gut)\n")

    # 2. Best-Practices-Signal: viele BP-Blocks = die BP-Datei wird oft gebraucht (Pflege lohnt).
    if best_practices:
        print("BEST-PRACTICES-AKTIVITAET (Almanach gelesen, BP-Pflicht ausgeloest):")
        for slug, cnt in sorted(best_practices.items(), key=lambda kv: -kv[1]):
            print(f"  {cnt:4d}x  {slug}")
        print()

    # 3. Aktivste Bereiche insgesamt = wichtigste Almanache (Pflege/Aktualitaet priorisieren).
    print("AKTIVSTE BEREICHE (Gesamt-Blocks = wie oft angefasst):")
    rows = sorted(total.items(), key=lambda kv: -kv[1])
    if args.top > 0:
        rows = rows[: args.top]
    for slug, cnt in rows:
        flags = []
        if kein_almanach.get(slug):
            flags.append(f"{kein_almanach[slug]} ohne-almanach")
        if best_practices.get(slug):
            flags.append(f"{best_practices[slug]} bp")
        suffix = f"   [{', '.join(flags)}]" if flags else ""
        print(f"  {cnt:4d}x  {slug}   (zuletzt {last_seen[slug]}){suffix}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
