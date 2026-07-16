#!/usr/bin/env python3
"""Aggregiert die Almanach-Trigger-Sonde (bug-almanac-triggers.jsonl) UND klassifiziert
jeden Block automatisch nach Trivialitaet (Abschalt-Kandidaten-Erkennung).

Liest die JSON-Lines-Aufzeichnung des bug-almanac-guard (inkl. rotierter .jsonl.1) und gibt aus:
  1. Verhaeltnis block/pass
  2. Blocks nach Bereich + Typ
  3. Passes nach Typ
  4. ABSCHALT-BILANZ — wie viele Blocks sind sicher/verdaechtig/berechtigt
  5. ABSCHALT-KANDIDATEN — gruppiert nach Bereich x Art, mit echten Beispielen
  6. BERECHTIGTE BLOCKS — echte Logik, zur Gegenkontrolle

Die Klassifikation pro Block (version-bump / string-only / comment-whitespace / import-only /
logic) ist die harte Datengrundlage fuer die Entscheidung, welche Guard-Unterbrechungen man
kuenftig weglassen kann. Sie ist bewusst KONSERVATIV: im Zweifel "logic" (berechtigt), damit
nie ein echter Bug-Trigger faelschlich als wegwerfbar erscheint.

Bewusst rein lesend — schreibt nichts, aendert den Guard nicht. Die Rohdatei wird hier
aggregiert, damit der Skill sie NICHT komplett in den Kontext laden muss (Lossless-Prinzip:
Details bleiben per Pfad in der .jsonl erreichbar).
"""
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

# Umlaute/Emoji auch auf Windows-Konsolen (cp1252) sauber ausgeben (python-windows §1.4/§1.7).
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

STATE = Path.home() / ".claude" / "state"
PRIMARY = STATE / "bug-almanac-triggers.jsonl"
ROTATED = STATE / "bug-almanac-triggers.jsonl.1"

# ── Trivial-Klassifikation aus der GEMEINSAMEN Quelle (DRY, Direktive #1) ──────
# classify() lebt in bugs/trivial_classify.py — dieselbe Definition nutzt der bug-almanac-guard
# fuer seinen Version-Bump-Filter. So laufen Auswertung (hier) und Durchsetzung (Guard) NIE
# auseinander. Pfad-Append, weil dieser Skill ausserhalb des Repos liegt.
sys.path.insert(0, str(Path.home() / "proggs" / "bugs"))
from trivial_classify import classify  # noqa: E402

# Klasse -> Klartext-Label fuer die Anzeige
TRIVIAL_LABELS = {
    "version-bump": "Version hochgezaehlt (versionCode/versionName)",
    "string-only": "nur Text-/String-Ressource",
    "comment-whitespace": "nur Kommentar/Leerzeile",
    "import-only": "nur Import-/Package-Kopf (Grenzfall)",
}
# Sicherheits-Stufen des Ausschlusses
SAFE_TO_DROP = {"version-bump"}                       # vollstaendiger, kurzer Edit -> sehr sicher
SUSPECT_DROP = {"string-only", "comment-whitespace"}  # wahrscheinlich, aber excerpt-begrenzt
BORDERLINE = {"import-only"}                           # nur Datei-Kopf sichtbar -> meist folgt Logik
TAGS = {
    "version-bump": "[SICHER]",
    "string-only": "[VERDACHT]",
    "comment-whitespace": "[VERDACHT]",
    "import-only": "[GRENZFALL]",
}


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


def short(excerpt, n=140):
    return excerpt.replace("\n", " ").strip()[:n]


def pct(part, total):
    return f"{(100.0 * part / total):.0f}%" if total else "0%"


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
    for r in blocks:
        r["_class"] = classify(r.get("change_excerpt", "") or "")

    total = len(blocks)

    print("=== ALMANACH-TRIGGER: VERHAELTNIS ===")
    print(f"Gesamt: {len(rows)}  |  Unterbrechungen (block): {total}  "
          f"|  Freigaben (pass): {len(passes)}")

    print("\n=== BLOCKS nach Bereich + Typ ===")
    by_area = Counter((r.get("slug", "?"), r.get("block_type", "?")) for r in blocks)
    for (slug, bt), n in by_area.most_common():
        print(f"  {n:4d}  {slug:18s} {bt}")
    if not by_area:
        print("  (keine Blocks)")

    print("\n=== PASSES nach Typ ===")
    by_pass = Counter(r.get("block_type", "?") for r in passes)
    for bt, n in by_pass.most_common():
        print(f"  {n:4d}  {bt}")
    if not by_pass:
        print("  (keine Passes)")

    # ── Abschalt-Bilanz ──────────────────────────────────────────────────────
    cls_count = Counter(r["_class"] for r in blocks)
    safe = sum(cls_count[c] for c in SAFE_TO_DROP)
    suspect = sum(cls_count[c] for c in SUSPECT_DROP)
    borderline = sum(cls_count[c] for c in BORDERLINE)
    legit = cls_count.get("logic", 0) + cls_count.get("leer", 0)

    print(f"\n=== ABSCHALT-BILANZ (von {total} Blocks) ===")
    print(f"  SICHER abschaltbar (Version-Bump):   {safe:4d}  ({pct(safe, total)})")
    print(f"  VERDACHT  (nur String/Kommentar):    {suspect:4d}  ({pct(suspect, total)})")
    print(f"  GRENZFALL (nur Import-/Datei-Kopf):  {borderline:4d}  ({pct(borderline, total)})")
    print(f"  BERECHTIGT (echte Logik):            {legit:4d}  ({pct(legit, total)})")

    # ── Abschalt-Kandidaten (Bereich x Klasse) ───────────────────────────────
    print("\n=== ABSCHALT-KANDIDATEN (gruppiert nach Bereich + Art) ===")
    cand = defaultdict(list)  # (slug, class) -> [(file, excerpt)]
    for r in blocks:
        if r["_class"] in TRIVIAL_LABELS:
            cand[(r.get("slug", "?"), r["_class"])].append(
                (Path(r.get("file", "")).name, short(r.get("change_excerpt", "") or "")))
    if cand:
        for (slug, cls), items in sorted(cand.items(), key=lambda kv: -len(kv[1])):
            print(f"\n{TAGS[cls]} {slug} — {TRIVIAL_LABELS[cls]}: {len(items)} Block(s)")
            for fname, ex in items[:3]:
                print(f"    {fname} :: {ex}")
            if len(items) > 3:
                print(f"    … und {len(items) - 3} weitere gleicher Art")
    else:
        print("  (keine trivialen Auslöser gefunden)")

    # ── Berechtigte Blocks (Gegenkontrolle) ──────────────────────────────────
    print("\n=== BERECHTIGTE BLOCKS (echte Logik — NICHT abschalten) ===")
    legit_by_slug = defaultdict(list)
    for r in blocks:
        if r["_class"] in ("logic", "leer"):
            legit_by_slug[r.get("slug", "?")].append(
                (Path(r.get("file", "")).name, short(r.get("change_excerpt", "") or "", 100)))
    if legit_by_slug:
        for slug, items in sorted(legit_by_slug.items(), key=lambda kv: -len(kv[1])):
            print(f"\n[{slug}] {len(items)} Block(s) mit echter Logik")
            for fname, ex in items[:2]:
                print(f"    {fname} :: {ex}")
            if len(items) > 2:
                print(f"    … und {len(items) - 2} weitere")
    else:
        print("  (keine)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
