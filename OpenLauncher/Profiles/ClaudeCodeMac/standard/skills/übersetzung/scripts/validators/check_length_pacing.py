#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Check 11 — Laengen-Pacing-Validator (alle Sprachen).

Vergleicht Source- vs Translation-Laengen pro String und warnt wenn eine
Uebersetzung deutlich laenger ist als das deutsche Original. Wichtig fuer
UI-Layouts (Buttons, Tabs, Listen-Eintraege werden sonst abgeschnitten).

Empirische Daten (Recherche 2026):
  - Polnisch, Russisch, Tuerkisch, Bengali, Tamil, Malayalam: +25 bis +40%
  - Chinesisch, Japanisch, Koreanisch: -30 bis -50% (kein Risiko)

Schwellenwerte:
  - Standard:    1.40 (also +40% Laengenzuwachs)
  - Mindestlaenge Quelle: 5 Zeichen (sonst trivialerweise hohe Faktoren)

Ausnahmen:
  - Strings mit nur Platzhaltern (%1$s, {name})
  - Strings mit bereits vorhandenem <!-- SHORTER: --> Kommentar

Kein Auto-Fix (anders als Check 7-10): das LLM muss in einem Verbesserungs-Pass
eine kuerzere Alternative ergaenzen — automatisches Kuerzen ist linguistisch
nicht machbar.

Usage:
    python3 check_length_pacing.py --source /path/to/values/strings.xml \\
        --target /path/to/values-pl/strings.xml --locale pl

Exit-Codes:
    0 = OK (alle Strings innerhalb Toleranz)
    1 = Warnung (mind. 1 String ueberschreitet Schwelle — Bericht in stdout)
    2 = Fehler
"""
import argparse
import os
import re
import sys


def parse_strings(path):
    """Liefert dict: name -> raw text content (inkl. Platzhalter)."""
    result = {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
    except (OSError, UnicodeDecodeError) as e:
        print(f"FEHLER: Konnte {path} nicht lesen: {e}", file=sys.stderr)
        sys.exit(2)

    for m in re.finditer(
        r'<string\s+name="([^"]+)"(?:[^>]*)>(.*?)</string>',
        content,
        flags=re.DOTALL,
    ):
        name = m.group(1)
        body = m.group(2)
        # SHORTER-Kommentar -> als bereits-bewertet ueberspringen
        if "<!-- SHORTER:" in body:
            continue
        # Reine Platzhalter-Strings ueberspringen
        cleaned = re.sub(r"%\d*\$?[sd]", "", body).strip()
        cleaned = re.sub(r"\{[^}]+\}", "", cleaned).strip()
        if len(cleaned) == 0:
            continue
        result[name] = body
    return result


def main():
    parser = argparse.ArgumentParser(
        description="Laengen-Pacing-Vergleich Source vs Translation"
    )
    parser.add_argument(
        "--source", required=True, help="Pfad zur Quell-strings.xml (Deutsch)"
    )
    parser.add_argument(
        "--target", required=True, help="Pfad zur Ziel-strings.xml (uebersetzt)"
    )
    parser.add_argument(
        "--locale", required=True, help="Locale-Bezeichnung fuer Reporting (z.B. pl)"
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=1.40,
        help="Verhaeltnis-Schwellenwert (Standard: 1.40 = +40%%)",
    )
    parser.add_argument(
        "--min-source-length",
        type=int,
        default=5,
        help="Minimale Quell-Laenge in Zeichen (Standard: 5)",
    )
    parser.add_argument(
        "--max-violations",
        type=int,
        default=20,
        help="Maximal angezeigte Violations (Standard: 20)",
    )
    args = parser.parse_args()

    for label, path in [("source", args.source), ("target", args.target)]:
        if not os.path.isfile(path):
            print(f"FEHLER: {label}-Datei nicht gefunden: {path}", file=sys.stderr)
            sys.exit(2)

    src = parse_strings(args.source)
    dst = parse_strings(args.target)

    violations = []
    for name, src_text in src.items():
        if name not in dst:
            continue
        src_len = len(src_text.strip())
        if src_len < args.min_source_length:
            continue
        dst_len = len(dst[name].strip())
        if src_len == 0:
            continue
        ratio = dst_len / src_len
        if ratio >= args.threshold:
            violations.append((name, ratio, src_text.strip(), dst[name].strip()))

    violations.sort(key=lambda v: -v[1])

    pct = (args.threshold - 1) * 100
    if not violations:
        print(
            f"OK: Laengen-Pacing fuer {args.locale} im Rahmen (Schwelle: +{pct:.0f}%)"
        )
        sys.exit(0)

    print(f"WARN: {len(violations)} Strings ueber +{pct:.0f}% Laenge in {args.locale}:")
    for name, ratio, src_text, dst_text in violations[: args.max_violations]:
        delta = (ratio - 1) * 100
        print(f"  [{delta:+.0f}%] {name}")
        print(f"    DE:        {src_text[:80]}")
        print(f"    {args.locale}: {dst_text[:80]}")
    if len(violations) > args.max_violations:
        print(f"  ... und {len(violations) - args.max_violations} weitere")
    print(
        "\nAktion: SHORTER-Alternativen im LLM-Verbesserungs-Pass ergaenzen "
        "(<!-- SHORTER: kuerzere Variante --> hinter dem String)."
    )
    sys.exit(1)


if __name__ == "__main__":
    main()
