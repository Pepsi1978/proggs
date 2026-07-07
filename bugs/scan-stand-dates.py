#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""scan-stand-dates.py — Stand-Datum-Scan für Bug-Almanache.

Findet echte Almanache unter bugs/<kategorie>/*.md, liest das neueste maschinenlesbare
Stand-Datum aus der Stand-Zeile und meldet fehlende oder ältere Einträge. Das Skript ist
rein lesend und endet immer mit exit 0, damit es Wartungsläufe unterstützt, aber nie
eine Session blockiert.

Standard: 21 Tage, passend für die regelmäßige Almanach-Update-Welle.
Aufruf:   python bugs/scan-stand-dates.py [--max-age-days 21]
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from datetime import date
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass


BUGS_DIR = Path(__file__).resolve().parent
NON_ALMANAC = {"readme.md", "system.md", "offene-almanache-prompts.md"}
DATE_RE = re.compile(r"(\d{4})-(\d{2})-(\d{2})")


@dataclass(frozen=True)
class AlmanachStand:
    rel: str
    stand: date | None
    age_days: int | None


def is_almanach(path: Path) -> bool:
    rel = path.relative_to(BUGS_DIR).as_posix()
    if "/" not in rel:
        return False
    name = path.name.lower()
    if name in NON_ALMANAC or name.endswith("-kurzcheck.md") or "audit" in name:
        return False
    return True


def parse_stand(path: Path) -> date | None:
    try:
        head = path.read_text(encoding="utf-8-sig", errors="replace")[:2500]
    except Exception:
        return None
    found: list[date] = []
    for line in head.splitlines():
        if "stand" not in line.lower():
            continue
        for match in DATE_RE.finditer(line):
            try:
                found.append(date(int(match.group(1)), int(match.group(2)), int(match.group(3))))
            except ValueError:
                pass
    return max(found) if found else None


def collect(today: date) -> list[AlmanachStand]:
    entries: list[AlmanachStand] = []
    for path in sorted(BUGS_DIR.rglob("*.md")):
        if not is_almanach(path):
            continue
        stand = parse_stand(path)
        rel = path.relative_to(BUGS_DIR).as_posix()
        age = (today - stand).days if stand else None
        entries.append(AlmanachStand(rel=rel, stand=stand, age_days=age))
    return entries


def main() -> int:
    parser = argparse.ArgumentParser(description="Stand-Datum-Scan für bugs/**/*.md")
    parser.add_argument("--max-age-days", type=int, default=21, help="Grenze für Update-Kandidaten (Default: 21)")
    args = parser.parse_args()

    today = date.today()
    entries = collect(today)
    missing = [entry for entry in entries if entry.stand is None]
    stale = [entry for entry in entries if entry.age_days is not None and entry.age_days > args.max_age_days]
    stale.sort(key=lambda entry: entry.age_days or -1, reverse=True)

    print("=== bugs/scan-stand-dates.py — Almanach-Stand-Datum-Scan ===")
    print(f"Geprüft: {len(entries)} Almanache")
    print(f"Grenze:  > {args.max_age_days} Tage")
    print(f"Fehlendes Stand-Datum: {len(missing)}")
    print(f"Update-Kandidaten: {len(stale)}")

    if missing:
        print("\n## Fehlendes Stand-Datum")
        for entry in missing:
            print(f"- {entry.rel}")

    if stale:
        print("\n## Älter als Grenze")
        for entry in stale:
            print(f"- {entry.rel}: {entry.age_days} Tage (Stand {entry.stand.isoformat()})")

    if not missing and not stale:
        print("\nAlles innerhalb der Grenze.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
