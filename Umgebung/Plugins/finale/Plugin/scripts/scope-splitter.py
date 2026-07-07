#!/usr/bin/env python3
"""finale Byte-Waechter (FIN-054).

Splittet einen Worker-Scope NACH BYTES in sichere Buckets, BEVOR ein Worker startet.
Verhindert verlustfrei, dass ein einzelner Worker eine zu grosse Portion bekommt und mit
"Prompt is too long" crasht. Nichts wird weggeworfen — nur in kleinere Haeppchen verteilt.

Hintergrund (FIN-048): Ein Subagent erbt ~70-120k Token Sockel und hat ~175-200k Limit.
Sicheres Read-Budget pro Worker daher ~40-50k Token. 1 Token ~= 4 Bytes (Code/Text-Heuristik),
also Default-Bucket-Grenze 180000 Bytes (~45k Token). Eine Einzeldatei groesser als die
Bucket-Grenze bekommt einen eigenen Bucket + Warnung (Worker MUSS sie per Range/Grep/Python
lesen, nie voll — das Read-Tool blockt ohnehin bei 256 KB/Aufruf).

Usage:
  python3 scope-splitter.py [--max-bytes N] [--glob '*.kt'] PATH [PATH ...]
  PYTHONIOENCODING=utf-8 python3 scope-splitter.py --glob '*.kt' app/src/main/java

Output (stdout, JSON):
  {"max_bytes":N,"bucket_count":K,"total_files":M,"oversize_files":[...],
   "buckets":[{"index":0,"bytes":..,"est_tokens":..,"file_count":..,"files":[...]}, ...]}

Exit 0 immer (auch bei leerem Scope -> bucket_count:0). Robuste Graceful Degradation.
"""
import os
import sys
import json
import fnmatch
import argparse


def iter_files(paths, pattern):
    for p in paths:
        p = os.path.expanduser(p)
        if os.path.isdir(p):
            for root, _dirs, files in os.walk(p):
                for f in files:
                    if pattern and not fnmatch.fnmatch(f, pattern):
                        continue
                    yield os.path.join(root, f)
        elif os.path.isfile(p):
            if pattern and not fnmatch.fnmatch(os.path.basename(p), pattern):
                continue
            yield p


def split_scope(paths, pattern, max_bytes):
    # Loop-2-Fix: nicht-existente Top-Level-Pfade NICHT still verschlucken (geschluckter Fehler)
    # -> als missing_paths melden, damit ein Tippfehler im Scope auffaellt statt unbemerkt zu fehlen.
    missing = [p for p in paths if not os.path.exists(os.path.expanduser(p))]
    files = []
    seen = set()
    for fp in iter_files(paths, pattern):
        # Loop-2-Fix: Duplikate (ueberlappende Scope-Pfade, z.B. Verzeichnis UND eine Datei darin)
        # nur EINMAL zaehlen -> verhindert Doppel-Bucketing/Doppel-Arbeit. Invariante "jede Datei
        # genau einmal" bleibt erhalten; ohne Duplikat-Input aendert sich nichts.
        key = os.path.normcase(os.path.abspath(fp))
        if key in seen:
            continue
        seen.add(key)
        try:
            files.append((fp, os.path.getsize(fp)))
        except OSError:
            continue  # nicht lesbar -> ueberspringen, niemals crashen

    # Loop-3-Fix: groesste zuerst, bei GLEICHER Groesse nach Pfad -> deterministische Reihenfolge.
    # Pflicht weil FIN-052-Resume denselben Scope erneut splittet und EXAKT dieselben Buckets
    # erhalten muss (sonst stimmt die "schon erledigt"-Zuordnung nicht). Bin-Packing-Ergebnis
    # identisch, nur die vorher unbestimmte Reihenfolge bei Groessengleichheit wird stabil.
    files.sort(key=lambda x: (-x[1], x[0]))

    buckets = []      # je Eintrag: [liste_dateien, summe_bytes]
    oversize = []
    for fp, sz in files:
        if sz > max_bytes:
            oversize.append({
                "file": fp,
                "bytes": sz,
                "note": "groesser als max_bytes -> Worker MUSS per Range/Grep/Python lesen, NIE voll",
            })
            buckets.append([[fp], sz])
            continue
        placed = False
        for b in buckets:
            if b[1] + sz <= max_bytes:
                b[0].append(fp)
                b[1] += sz
                placed = True
                break
        if not placed:
            buckets.append([[fp], sz])

    return {
        "max_bytes": max_bytes,
        "bucket_count": len(buckets),
        "total_files": len(files),
        "missing_paths": missing,
        "oversize_files": oversize,
        "buckets": [
            {
                "index": i,
                "bytes": b[1],
                "est_tokens": b[1] // 4,
                "file_count": len(b[0]),
                "files": b[0],
            }
            for i, b in enumerate(buckets)
        ],
    }


def main():
    ap = argparse.ArgumentParser(description="finale Byte-Waechter (FIN-054)")
    ap.add_argument("--max-bytes", type=int, default=180000,
                    help="max Bytes pro Bucket (~45k Token). Default 180000.")
    ap.add_argument("--glob", default=None, help="optionaler Datei-Filter, z.B. '*.kt'")
    ap.add_argument("paths", nargs="+")
    args = ap.parse_args()

    # Loop-2-Fix (Randfall): max-bytes <= 0 wuerde JEDE Datei als oversize markieren (unsinnig)
    # -> auf Default zuruecksetzen statt kaputtes Bucketing zu liefern.
    if args.max_bytes <= 0:
        sys.stderr.write("WARN: --max-bytes <= 0 ungueltig, nutze Default 180000\n")
        args.max_bytes = 180000

    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

    result = split_scope(args.paths, args.glob, args.max_bytes)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    sys.exit(0)


if __name__ == "__main__":
    main()
