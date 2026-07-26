#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Check 10 — Apostroph-Auto-Escape fuer FR/IT/PT-BR/PT-PT/EN.

Android-Strings im Format <string name="..."/> MUESSEN Apostrophe mit \\' eskapieren
oder den ganzen String in doppelte Anfuehrungszeichen "..." einschliessen. Sonst
bricht das AAPT-Tool den Build mit "error: unclosed string" — bevor das APK
ueberhaupt zusammengesetzt wird.

Empirisch der HAEUFIGSTE Build-Killer bei automatischen Uebersetzungen:
  - Franzoesisch: l'application, c'est, aujourd'hui
  - Italienisch: l'app, un'altra volta, nell'elenco
  - Portugiesisch: vereinzelt
  - Englisch: you're, it's, don't, we'll

Regel-Set:
  1. Apostroph in <string>-Wert UND String NICHT in doppelten Quotes → escape zu \\'
  2. Apostroph in <string>-Wert UND String IST in doppelten Quotes (Wrap-Form "...")
     → bleibt unbehandelt (legal)
  3. Bereits eskapierte Apostrophe \\' → bleiben (nicht doppelt eskapieren)
  4. Apostrophe in <plurals><item> und <string-array><item> wie <string> behandelt
  5. Typographische Apostrophe ’ (U+2019) bleiben unangetastet (legal in Android-XML)
  6. CDATA-Bloecke <![CDATA[...]]> bleiben unangetastet (Apostrophe darin sind legal,
     auch ohne Escape — AAPT parsed CDATA-Inhalt nicht als XML)

Usage:
    python3 check_apostrophes.py --locale fr --path /path/to/values-fr/strings.xml

Exit-Codes:
    0 = OK (keine oder erfolgreich gefixte Apostrophe)
    2 = Fehler
"""
import argparse
import os
import re
import sys
import tempfile


def escape_apostrophes(value):
    """Eskapiere unescaped ' nur ausserhalb von:
    - doppelten Anfuehrungszeichen (Wrap-Form "...")
    - CDATA-Bloecken (<![CDATA[...]]>) — darin ist ' legal ohne Escape
    - bereits eskapierten \\'
    """
    stripped = value.strip()
    # Regel 2: String in doppelten Anfuehrungszeichen → unangetastet
    if len(stripped) >= 2 and stripped.startswith('"') and stripped.endswith('"'):
        return value, 0

    # Regel 6: CDATA-Bloecke temporaer extrahieren (Inhalt geschuetzt vor Escape).
    # Das ist wichtig weil <![CDATA[L'application]]> in Android legal ist —
    # AAPT parsed CDATA-Inhalt nicht als XML, Apostrophe brauchen kein Escape.
    cdata_marker = "\x00CDATA_BLOCK_{}\x00"
    cdata_matches = []

    def stash_cdata(m):
        cdata_matches.append(m.group(0))
        return cdata_marker.format(len(cdata_matches) - 1)

    protected = re.sub(
        r"<!\[CDATA\[.*?\]\]>", stash_cdata, value, flags=re.DOTALL
    )

    # Regel 3: \' ist schon eskapiert — ersetze temporaer durch Platzhalter
    placeholder = "\x00ESCAPED_APOS\x00"
    tmp = protected.replace("\\'", placeholder)
    fixes = tmp.count("'")
    if fixes == 0:
        # Keine Apostrophe ausserhalb von CDATA — original zurueck
        return value, 0

    # Regel 1: alle verbliebenen ' eskapieren
    tmp = tmp.replace("'", "\\'")
    tmp = tmp.replace(placeholder, "\\'")

    # CDATA-Bloecke wiederherstellen (Inhalt unveraendert)
    for i, cdata in enumerate(cdata_matches):
        tmp = tmp.replace(cdata_marker.format(i), cdata)

    return tmp, fixes


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
        description="Apostroph-Auto-Escape fuer Android strings.xml"
    )
    parser.add_argument(
        "--locale",
        required=True,
        help="Locale (fr, it, pt-rBR, pt-rPT, en) — fuer Reporting",
    )
    parser.add_argument(
        "--path", required=True, help="Absoluter Pfad zur strings.xml"
    )
    args = parser.parse_args()

    if not os.path.isfile(args.path):
        print(f"FEHLER: Datei nicht gefunden: {args.path}", file=sys.stderr)
        sys.exit(2)

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
        new_body, n = escape_apostrophes(body)
        if n > 0:
            total_fixes += n
            strings_changed += 1
            return full.replace(body, new_body)
        return full

    # Sowohl <string> als auch <item> in plurals/string-array
    content = re.sub(
        r"<string\b[^>]*>(.*?)</string>", process, content, flags=re.DOTALL
    )
    content = re.sub(
        r'<item(?:\s+quantity="[^"]+")?\s*>(.*?)</item>',
        process,
        content,
        flags=re.DOTALL,
    )

    if content == original:
        print(f"OK: 0 unescaped Apostrophe in {args.locale}")
        sys.exit(0)

    try:
        atomic_write(args.path, content)
    except OSError as e:
        print(f"FEHLER: Konnte {args.path} nicht schreiben: {e}", file=sys.stderr)
        sys.exit(2)

    print(
        f"Fixed: {strings_changed} Strings, {total_fixes} Apostrophe in {args.locale} eskapiert"
    )
    sys.exit(0)


if __name__ == "__main__":
    main()
