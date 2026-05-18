#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Check 8 — Full-width Punctuation Validator fuer CJK-Sprachen (zh-Hans, zh-Hant, ja).

Erkennt half-width ASCII-Satzzeichen (`, . ! ? : ; ( )`) nach CJK-Zeichen und
ersetzt sie durch die full-width Aequivalente. Visuell sofort als unprofessionell
erkennbar; native CJK-Apps verwenden durchgaengig full-width.

Komma-Sonderfall: Japanisch nutzt `、` (U+3001), Chinesisch nutzt `，` (U+FF0C).

JSON-Schema-Strings werden NICHT angetastet (sonst bricht KI-Parsing in `ai_prompt_*`).

Usage:
    python3 check_cjk_punctuation.py --locale zh-rTW --path /path/to/values-zh-rTW/strings.xml

Exit-Codes:
    0 = OK (keine oder erfolgreich gefixte half-width Punctuation)
    2 = Fehler
"""
import argparse
import os
import re
import sys
import tempfile


# Unicode-Ranges fuer die CJK-Erkennung pro Locale.
# WICHTIG: Japanisch braucht zusaetzlich Hiragana (U+3040-U+309F) und Katakana
# (U+30A0-U+30FF), sonst werden reine Kana-Saetze uebersehen. Chinesisch nutzt
# fast ausschliesslich Kanji (U+4E00-U+9FFF), aber wir nehmen die Kana-Ranges
# defensiv mit auf — schadet nicht und faengt auch japanische Fragmente in
# Chinesisch-Strings.
KANJI = "一-鿿"
HIRAGANA = "぀-ゟ"
KATAKANA = "゠-ヿ"

LOCALE_CONFIG = {
    # locale: (komma_zeichen, label, cjk_range)
    "zh-Hans": ("，", "Simplified Chinese (zh-Hans)", KANJI),
    "zh-Hant": ("，", "Traditional Chinese (zh-Hant)", KANJI),
    "ja": ("、", "Japanese", KANJI + HIRAGANA + KATAKANA),
}

# Rueckwaerts-Kompatibilitaet: Android-Verzeichnis-Codes auf Sprach-Datei-Codes mappen.
# So funktionieren sowohl alte ("--locale zh-rCN") als auch neue ("--locale zh-Hans") Aufrufe.
LOCALE_ALIAS = {
    "zh-rCN": "zh-Hans",
    "zh-rTW": "zh-Hant",
}


def is_json_schema(s):
    """JSON-Strings duerfen NICHT gefixt werden — sonst bricht KI-Parsing."""
    if "JSON" in s and ('\\"' in s or "{" in s):
        return True
    if re.search(r'\\"[a-zA-Z_]+\\"\s*:', s):
        return True
    return False


# Full-width Zeichen als explizite Unicode-Konstanten — verhindert Encoding-Verlust beim Speichern
FW_COMMA_CN = "，"     # ， chinesisches Komma
FW_COMMA_JP = "、"     # 、 japanisches Komma
FW_PERIOD = "。"       # 。
FW_EXCLAIM = "！"      # !
FW_QUESTION = "？"     # ?
FW_COLON = "："        # :
FW_SEMI = "；"         # ;
FW_LPAREN = "（"       # (
FW_RPAREN = "）"       # )


def fix_string(text, comma, CJK_RANGE):
    if is_json_schema(text):
        return text, 0
    fixed = text
    n = 0
    # Komma nach CJK
    fixed, c = re.subn(rf"([{CJK_RANGE}]),", rf"\1{comma}", fixed)
    n += c
    # Punkt nach CJK (nicht in 1.0)
    fixed, c = re.subn(rf"([{CJK_RANGE}])\.(?!\d)", rf"\1{FW_PERIOD}", fixed)
    n += c
    # Ausrufezeichen, Fragezeichen
    fixed, c = re.subn(rf"([{CJK_RANGE}])!", rf"\1{FW_EXCLAIM}", fixed)
    n += c
    fixed, c = re.subn(rf"([{CJK_RANGE}])\?", rf"\1{FW_QUESTION}", fixed)
    n += c
    # Doppelpunkt (nicht in 12:30)
    fixed, c = re.subn(rf"([{CJK_RANGE}]):(?!\d{{2}})", rf"\1{FW_COLON}", fixed)
    n += c
    fixed, c = re.subn(rf"([{CJK_RANGE}]);", rf"\1{FW_SEMI}", fixed)
    n += c
    # Klammern: ( neben CJK -> （
    fixed, c = re.subn(rf"([{CJK_RANGE}])\s*\(", rf"\1{FW_LPAREN}", fixed)
    n += c
    # ) Paare schliessen
    fixed, c = re.subn(
        rf"{FW_LPAREN}([^{FW_LPAREN}{FW_RPAREN}()]*?)\)",
        rf"{FW_LPAREN}\1{FW_RPAREN}",
        fixed,
    )
    n += c
    # Kosmetisch: kein Space nach full-width Punctuation
    fixed = re.sub(rf"{FW_COLON}[ \t]+(\S)", rf"{FW_COLON}\1", fixed)
    fixed = re.sub(rf"{FW_PERIOD}[ \t]+(\S)", rf"{FW_PERIOD}\1", fixed)
    return fixed, n


def atomic_write(path, content):
    target_dir = os.path.dirname(os.path.abspath(path))
    with tempfile.NamedTemporaryFile(
        "w", dir=target_dir, suffix=".tmp", delete=False, encoding="utf-8"
    ) as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)


def main():
    parser = argparse.ArgumentParser(
        description="Full-width Punctuation Auto-Fix fuer CJK-Sprachen"
    )
    parser.add_argument(
        "--locale",
        required=True,
        choices=sorted(set(list(LOCALE_CONFIG.keys()) + list(LOCALE_ALIAS.keys()))),
        help="Locale: zh-Hans, zh-Hant, ja (oder Aliase zh-rCN, zh-rTW fuer Rueckwaerts-Kompatibilitaet)",
    )
    parser.add_argument(
        "--path", required=True, help="Absoluter Pfad zur strings.xml"
    )
    args = parser.parse_args()

    if not os.path.isfile(args.path):
        print(f"FEHLER: Datei nicht gefunden: {args.path}", file=sys.stderr)
        sys.exit(2)

    # Alias-Aufloesung (zh-rCN -> zh-Hans, etc.) fuer Rueckwaerts-Kompatibilitaet
    resolved_locale = LOCALE_ALIAS.get(args.locale, args.locale)
    comma, label, cjk_range = LOCALE_CONFIG[resolved_locale]

    try:
        with open(args.path, "r", encoding="utf-8") as f:
            content = f.read()
    except (OSError, UnicodeDecodeError) as e:
        print(f"FEHLER: Konnte {args.path} nicht lesen: {e}", file=sys.stderr)
        sys.exit(2)

    original = content
    total_fixes = 0
    strings_changed = 0

    def process(match):
        nonlocal total_fixes, strings_changed
        full = match.group(0)
        body = match.groups()[-1]
        new_body, n = fix_string(body, comma, cjk_range)
        if n > 0:
            total_fixes += n
            strings_changed += 1
            return full.replace(body, new_body)
        return full

    content = re.sub(
        r'<string name="([^"]+)"(?:[^>]*?)>(.*?)</string>',
        process,
        content,
        flags=re.DOTALL,
    )
    content = re.sub(
        r'<item(?:\s+quantity="[^"]+")?>(.*?)</item>',
        process,
        content,
        flags=re.DOTALL,
    )

    if content == original:
        print(f"OK: 0 half-width Punctuation in {args.locale} ({label})")
        sys.exit(0)

    try:
        atomic_write(args.path, content)
    except OSError as e:
        print(f"FEHLER: Konnte {args.path} nicht schreiben: {e}", file=sys.stderr)
        sys.exit(2)

    print(
        f"Fixed: {strings_changed} Strings, {total_fixes} Punctuation-Aenderungen in {args.locale}"
    )
    sys.exit(0)


if __name__ == "__main__":
    main()
