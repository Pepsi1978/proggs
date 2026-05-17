#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Check 7 — Native-Ziffern-Validator fuer indische Sprachen.

Erkennt und ersetzt native Ziffern (Bengali, Devanagari, Telugu, Tamil, Gujarati,
Kannada, Malayalam, Extended Arabic-Indic) durch arabische Ziffern 0-9.

Die Referenzdatei `references/languages/[code].md` schreibt fuer ALLE indischen
Sprachen explizit vor: "Use Arabic numerals (0-9), NOT [native] digits."
LLMs ignorieren diese Regel haeufig — dieser Check faengt das ab.

Usage:
    python3 check_native_digits.py --locale bn --path /path/to/values-bn/strings.xml

Exit-Codes:
    0 = OK, keine nativen Ziffern gefunden
    0 = OK, native Ziffern gefunden UND automatisch ersetzt
    2 = Fehler (Datei nicht lesbar, falsches Locale, etc.)
"""
import argparse
import os
import sys
import tempfile

LANG_DIGITS = {
    "bn": ("০১২৩৪৫৬৭৮৯", "Bengali"),
    "hi": ("०१२३४५६७८९", "Devanagari"),
    "mr": ("०१२३४५६७८९", "Devanagari (Marathi)"),
    "te": ("౦౧౨౩౪౫౬౭౮౯", "Telugu"),
    "ta": ("௦௧௨௩௪௫௬௭௮௯", "Tamil"),
    "gu": ("૦૧૨૩૪૫૬૭૮૯", "Gujarati"),
    "kn": ("೦೧೨೩೪೫೬೭೮೯", "Kannada"),
    "ml": ("൦൧൨൩൪൫൬൭൮൯", "Malayalam"),
    "ur": ("۰۱۲۳۴۵۶۷۸۹", "Extended Arabic-Indic (Urdu)"),
    "fa": ("۰۱۲۳۴۵۶۷۸۹", "Extended Arabic-Indic (Persian)"),
}


def atomic_write(path, content):
    """Atomares Schreiben via tempfile + os.replace. Verhindert kaputte Dateien."""
    target_dir = os.path.dirname(os.path.abspath(path))
    with tempfile.NamedTemporaryFile(
        "w", dir=target_dir, suffix=".tmp", delete=False, encoding="utf-8"
    ) as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)


def main():
    parser = argparse.ArgumentParser(
        description="Native-Ziffern-Auto-Fix fuer indische Android-Lokalisierungen"
    )
    parser.add_argument(
        "--locale",
        required=True,
        choices=sorted(LANG_DIGITS.keys()),
        help="Sprach-Code (bn, hi, mr, te, ta, gu, kn, ml, ur)",
    )
    parser.add_argument(
        "--path", required=True, help="Absoluter Pfad zur strings.xml"
    )
    args = parser.parse_args()

    if not os.path.isfile(args.path):
        print(f"FEHLER: Datei nicht gefunden: {args.path}", file=sys.stderr)
        sys.exit(2)

    native, label = LANG_DIGITS[args.locale]

    try:
        with open(args.path, "r", encoding="utf-8") as f:
            content = f.read()
    except (OSError, UnicodeDecodeError) as e:
        print(f"FEHLER: Konnte {args.path} nicht lesen: {e}", file=sys.stderr)
        sys.exit(2)

    count = sum(1 for c in content if c in native)

    if count == 0:
        print(f"OK: 0 {label}-Ziffern in {args.locale} (Arabic-only)")
        sys.exit(0)

    print(f"FEHLER: {count} {label}-Ziffern gefunden — ersetze automatisch zu 0-9")
    fixed = content.translate(str.maketrans(native, "0123456789"))

    try:
        atomic_write(args.path, fixed)
    except OSError as e:
        print(f"FEHLER: Konnte {args.path} nicht schreiben: {e}", file=sys.stderr)
        sys.exit(2)

    print(f"Fixed: {count} Ziffern in {args.path} ersetzt")
    sys.exit(0)


if __name__ == "__main__":
    main()
