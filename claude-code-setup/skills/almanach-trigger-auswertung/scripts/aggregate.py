#!/usr/bin/env python3
"""Aggregiert die Almanach-Trigger-Sonde (bug-almanac-triggers.jsonl) kompakt.

Liest die JSON-Lines-Aufzeichnung des bug-almanac-guard (inkl. rotierter .jsonl.1) und
gibt eine kompakte Auswertung aus: Verhaeltnis block/pass, Haeufigkeit pro Bereich+Typ und
die change_excerpts der Block-Ereignisse je Bereich (Grundlage fuer die KI-Verdachtspruefung).

Bewusst rein lesend — schreibt nichts, aendert den Guard nicht. Die Rohdatei wird hier
aggregiert, damit der Skill sie NICHT komplett in den Kontext laden muss (Lossless-Prinzip:
Details bleiben per Pfad in der .jsonl erreichbar).
"""
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

# Umlaute auch auf Windows-Konsolen (cp1252) sauber ausgeben (python-windows §1.4/§1.7).
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

STATE = Path.home() / ".claude" / "state"
PRIMARY = STATE / "bug-almanac-triggers.jsonl"
ROTATED = STATE / "bug-almanac-triggers.jsonl.1"


def load_rows():
    rows = []
    for fn in (ROTATED, PRIMARY):  # rotierte zuerst -> chronologische Reihenfolge
        if not fn.exists():
            continue
        with open(fn, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    rows.append(json.loads(line))
                except json.JSONDecodeError:
                    pass  # eine kaputte Zeile darf die Auswertung nicht stoppen
    return rows


def main():
    rows = load_rows()
    if not rows:
        print("Keine Sonden-Daten gefunden.")
        print(f"Erwartet: {PRIMARY}")
        print("Die Almanach-Trigger-Sonde hat in dieser/diesen Session(s) noch nichts "
              "aufgezeichnet (oder der Guard wurde noch nicht ausgeloest).")
        return 0

    blocks = [r for r in rows if r.get("event") == "block"]
    passes = [r for r in rows if r.get("event") == "pass"]

    print("=== ALMANACH-TRIGGER: VERHAELTNIS ===")
    print(f"Gesamt: {len(rows)}  |  Unterbrechungen (block): {len(blocks)}  "
          f"|  Freigaben (pass): {len(passes)}")

    print("\n=== BLOCKS nach Bereich + Typ ===")
    by_area = Counter((r.get("slug", "?"), r.get("block_type", "?")) for r in blocks)
    if by_area:
        for (slug, bt), n in by_area.most_common():
            print(f"  {n:4d}  {slug:18s} {bt}")
    else:
        print("  (keine Blocks)")

    print("\n=== PASSES nach Typ ===")
    by_pass = Counter(r.get("block_type", "?") for r in passes)
    if by_pass:
        for bt, n in by_pass.most_common():
            print(f"  {n:4d}  {bt}")
    else:
        print("  (keine Passes)")

    print("\n=== BLOCK-change_excerpts je Bereich (fuer Verdachtspruefung) ===")
    ex_by_slug = defaultdict(list)
    for r in blocks:
        excerpt = (r.get("change_excerpt", "") or "").replace("\n", " ").strip()
        fname = Path(r.get("file", "")).name
        ex_by_slug[r.get("slug", "?")].append((fname, excerpt[:120]))
    if ex_by_slug:
        for slug, items in sorted(ex_by_slug.items(), key=lambda kv: -len(kv[1])):
            print(f"\n[{slug}] ({len(items)} Blocks)")
            for fname, excerpt in items[:25]:  # pro Bereich max 25 Beispiele
                print(f"  {fname} :: {excerpt}")
            if len(items) > 25:
                print(f"  … und {len(items) - 25} weitere (Details in {PRIMARY.name})")
    else:
        print("  (keine Blocks)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
