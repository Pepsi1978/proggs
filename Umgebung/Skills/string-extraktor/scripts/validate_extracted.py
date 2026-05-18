#!/usr/bin/env python3
# validate_extracted.py — Post-Extraktion-Validator fuer den string-extraktor Skill
#
# Prueft eine bestehende strings.xml (und optional donottranslate.xml) auf:
#   - Duplikate (gleicher Wert unter verschiedenen Keys)
#   - Doppelte Keys (gleicher Key mehrfach definiert)
#   - Unicode-Escapes (Ü statt echtem Ü)
#   - Veraltete deutsche Rechtschreibung (daß, muß, Tip)
#   - Falsche Anfuehrungszeichen (Schreibmaschinen-Quotes in sichtbaren Strings)
#   - Falsche Platzhalter (%s statt %1$s)
#   - Fehlende XLIFF-Tags bei Platzhaltern
#   - Fehlende translatable="false" bei URLs und technischen Strings
#   - donottranslate.xml-Kandidaten (URLs, app_name, brand_name in strings.xml)
#   - Format-String-Mismatches (wenn Code-Aufrufe gegeben sind)
#
# Aufruf (Cross-Platform):
#   bash scripts/validate.sh <strings.xml>           # automatisch python3/python finden
#   python3 validate_extracted.py <strings.xml>      # macOS/Linux direkt
#   python validate_extracted.py <strings.xml>       # Windows direkt
#
# Optionen:
#   --donottranslate <donottranslate.xml>   # Zusaetzliche XML pruefen
#   --suggest-donottranslate                # Vorschlaege fuer donottranslate.xml
#   --code-dir <app/src/main/java>          # Format-Arg-Cross-Check gegen Code
#   --strict                                # WARN-Issues auch als Fehler werten
#
# Exit-Code:
#   0 = alle Checks bestanden (keine Issues)
#   1 = Issues gefunden (Details im Output)
#   2 = Aufruf-Fehler (Datei nicht gefunden o.ae.)

import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict


# Veraltete deutsche Rechtschreibung (vor 1996/2006)
LEGACY_SPELLING = {
    'daß': 'dass',
    'muß': 'muss',
    'mußt': 'musst',
    'Haß': 'Hass',
    'Fluß': 'Fluss',
    'Tip': 'Tipp',
    'Tips': 'Tipps',
    'numerieren': 'nummerieren',
    'numeriert': 'nummeriert',
}

# Denglisch-Kandidaten
DENGLISH_PATTERNS = [
    (r'\bgeshared\b', 'geteilt'),
    (r'\bgeliked\b', 'gefaellt-Markierung gesetzt'),
    (r'\bgecheckt\b', 'geprueft'),
    (r'\bgepushed\b', 'gesendet/hochgeladen'),
    (r'\bgedownloaded\b', 'heruntergeladen'),
    (r'\bcanceln\b', 'abbrechen'),
]

# URL/Brand-Patterns die nach donottranslate.xml gehoeren
DONOTTRANSLATE_PATTERNS = [
    (r'^https?://', 'URL'),
    (r'^mailto:', 'Email-Link'),
    (r'^tel:', 'Telefon-Link'),
    (r'^[a-z]+://', 'URI-Scheme'),
    (r'^\+?\d[\d\s\-\(\)]{4,}$', 'Telefonnummer'),
    (r'^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$', 'Email-Adresse'),
]

# Keys die typischerweise nicht uebersetzt werden
DONOTTRANSLATE_KEY_PATTERNS = [
    r'^app_name$',
    r'^brand_',
    r'^url_',
    r'^api_',
    r'^endpoint_',
    r'^firebase_',
    r'^locale_code',
    r'^country_code',
]


def parse_strings_xml(path):
    """Liest strings.xml und gibt eine Liste von (key, value, line, translatable, has_xliff) zurueck."""
    if not os.path.exists(path):
        print(f"FEHLER: Datei nicht gefunden: {path}", file=sys.stderr)
        sys.exit(2)

    # Wir parsen sowohl mit ElementTree (fuer Struktur) als auch zeilenweise (fuer Line-Numbers)
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    entries = []
    try:
        tree = ET.parse(path)
        root = tree.getroot()
        for elem in root.iter('string'):
            key = elem.get('name', '')
            translatable = elem.get('translatable', 'true') != 'false'
            # Inner-Text inklusive aller Sub-Elemente (z.B. xliff:g)
            value = ''.join(elem.itertext())
            has_xliff = bool(list(elem.iter())) and len(list(elem.iter())) > 1
            # Line-Number suchen
            line_num = 0
            for i, line in enumerate(lines):
                if f'name="{key}"' in line:
                    line_num = i + 1
                    break
            entries.append({
                'key': key,
                'value': value,
                'line': line_num,
                'translatable': translatable,
                'has_xliff': has_xliff,
                'raw_xml': ET.tostring(elem, encoding='unicode'),
            })
    except ET.ParseError as e:
        print(f"FEHLER: XML-Parse-Fehler in {path}: {e}", file=sys.stderr)
        sys.exit(2)

    return entries, lines


def check_duplicate_keys(entries):
    """Sucht nach doppelt definierten Keys."""
    issues = []
    seen = defaultdict(list)
    for entry in entries:
        seen[entry['key']].append(entry['line'])
    for key, line_nums in seen.items():
        if len(line_nums) > 1:
            issues.append({
                'severity': 'ERROR',
                'category': 'duplicate-key',
                'key': key,
                'message': f"Key '{key}' ist mehrfach definiert (Zeilen: {', '.join(map(str, line_nums))})",
            })
    return issues


def check_duplicate_values(entries):
    """Sucht nach gleichen Werten unter verschiedenen Keys."""
    issues = []
    by_value = defaultdict(list)
    for entry in entries:
        if entry['translatable'] and entry['value'].strip():
            by_value[entry['value'].strip()].append(entry['key'])
    for value, keys in by_value.items():
        if len(keys) > 1:
            issues.append({
                'severity': 'WARN',
                'category': 'duplicate-value',
                'key': keys[0],
                'message': f"Gleicher Wert '{value[:40]}...' unter {len(keys)} Keys: {', '.join(keys)}",
            })
    return issues


def check_unicode_escapes(entries):
    """Sucht nach Unicode-Escapes fuer deutsche Umlaute."""
    issues = []
    pattern = re.compile(r'\\u00(dc|fc|d6|f6|c4|e4|df)', re.IGNORECASE)
    for entry in entries:
        if pattern.search(entry['value']):
            issues.append({
                'severity': 'ERROR',
                'category': 'unicode-escape',
                'key': entry['key'],
                'message': f"Unicode-Escape fuer Umlaut in '{entry['key']}' (Zeile {entry['line']}) — echte Umlaute verwenden",
            })
    return issues


def check_legacy_spelling(entries):
    """Sucht nach veralteter Rechtschreibung."""
    issues = []
    for entry in entries:
        if not entry['translatable']:
            continue
        for old, new in LEGACY_SPELLING.items():
            if re.search(rf'\b{old}\b', entry['value']):
                issues.append({
                    'severity': 'ERROR',
                    'category': 'legacy-spelling',
                    'key': entry['key'],
                    'message': f"Veraltete Rechtschreibung in '{entry['key']}': '{old}' -> '{new}' (Zeile {entry['line']})",
                })
    return issues


def check_denglish(entries):
    """Sucht nach Denglisch-Mustern."""
    issues = []
    for entry in entries:
        if not entry['translatable']:
            continue
        for pattern, suggestion in DENGLISH_PATTERNS:
            if re.search(pattern, entry['value'], re.IGNORECASE):
                issues.append({
                    'severity': 'WARN',
                    'category': 'denglish',
                    'key': entry['key'],
                    'message': f"Denglisch in '{entry['key']}': empfohlen '{suggestion}' (Zeile {entry['line']})",
                })
    return issues


def check_placeholder_numbering(entries):
    """Sucht nach unnummerierten Platzhaltern (%s statt %1$s)."""
    issues = []
    unnumbered = re.compile(r'(?<!\d\$)(?<!\$)%[sdfu]')
    for entry in entries:
        # XLIFF-Inhalte zaehlen nicht
        if unnumbered.search(entry['value']):
            issues.append({
                'severity': 'ERROR',
                'category': 'unnumbered-placeholder',
                'key': entry['key'],
                'message': f"Unnummerierter Platzhalter in '{entry['key']}': '%s' verwenden -> '%1$s' (Zeile {entry['line']})",
            })
    return issues


def check_missing_xliff(entries):
    """Sucht nach Strings mit Platzhaltern aber ohne XLIFF-Tags."""
    issues = []
    placeholder_pattern = re.compile(r'%\d+\$[sdfu]|%[sdfu]')
    for entry in entries:
        if not entry['translatable']:
            continue
        if placeholder_pattern.search(entry['value']) and '<xliff:g' not in entry['raw_xml']:
            issues.append({
                'severity': 'WARN',
                'category': 'missing-xliff',
                'key': entry['key'],
                'message': f"Platzhalter ohne XLIFF-Tag in '{entry['key']}' (Zeile {entry['line']}) — xliff:g hinzufuegen",
            })
    return issues


def check_typewriter_quotes(entries):
    """Sucht nach Schreibmaschinen-Quotes in sichtbaren Strings."""
    issues = []
    for entry in entries:
        if not entry['translatable']:
            continue
        # Suche nach " innerhalb des Wertes (nicht am Anfang/Ende, das sind XML-Quotes)
        # Wir suchen tatsaechliche "..." Strukturen
        if re.search(r'\\?"[^"]+\\?"', entry['value']):
            issues.append({
                'severity': 'WARN',
                'category': 'typewriter-quotes',
                'key': entry['key'],
                'message': f"Schreibmaschinen-Anfuehrungszeichen in '{entry['key']}' -- typografische deutsche Anfuehrungszeichen verwenden (Zeile {entry['line']})",
            })
    return issues


def check_donottranslate_candidates(entries):
    """Findet Strings die nach donottranslate.xml gehoeren."""
    candidates = []
    for entry in entries:
        if not entry['translatable']:
            continue  # Bereits korrekt markiert

        # URL/Email/Telefon-Patterns
        for pattern, kind in DONOTTRANSLATE_PATTERNS:
            if re.match(pattern, entry['value'].strip()):
                candidates.append({
                    'key': entry['key'],
                    'value': entry['value'][:60],
                    'reason': kind,
                    'line': entry['line'],
                })
                break
        else:
            # Key-Patterns pruefen
            for pattern in DONOTTRANSLATE_KEY_PATTERNS:
                if re.match(pattern, entry['key']):
                    candidates.append({
                        'key': entry['key'],
                        'value': entry['value'][:60],
                        'reason': 'Konventions-Key',
                        'line': entry['line'],
                    })
                    break

    return candidates


def check_format_string_args(entries, code_dir):
    """Prueft ob stringResource-Aufrufe die richtige Anzahl Argumente uebergeben."""
    if not code_dir or not os.path.isdir(code_dir):
        return []

    issues = []
    placeholder_pattern = re.compile(r'%(\d+)\$[sdfu]|%[sdfu]')
    # String-Key -> Anzahl Platzhalter
    key_to_args = {}
    for entry in entries:
        matches = placeholder_pattern.findall(entry['value'])
        # matches sind Tupel mit gruppen — leere strings zaehlen auch
        max_arg = 0
        unnumbered = 0
        for m in matches:
            if m and m.isdigit():
                max_arg = max(max_arg, int(m))
            else:
                unnumbered += 1
        key_to_args[entry['key']] = max_arg if max_arg > 0 else unnumbered

    # Code-Dir nach stringResource(R.string.X, arg1, arg2)-Aufrufen scannen
    call_pattern = re.compile(r'stringResource\(\s*R\.string\.(\w+)((?:\s*,\s*[^)]+)*)\s*\)')
    for root, _, files in os.walk(code_dir):
        if 'build' in root or 'test' in root:
            continue
        for f in files:
            if not f.endswith('.kt'):
                continue
            path = os.path.join(root, f)
            try:
                with open(path, 'r', encoding='utf-8') as fp:
                    content = fp.read()
            except (OSError, UnicodeDecodeError):
                continue
            for match in call_pattern.finditer(content):
                key = match.group(1)
                args_str = match.group(2)
                # Argumente zaehlen (grobe Schaetzung — durch Kommas)
                if args_str.strip():
                    num_args = args_str.count(',')
                else:
                    num_args = 0

                expected = key_to_args.get(key, 0)
                if expected != num_args:
                    line_num = content[:match.start()].count('\n') + 1
                    issues.append({
                        'severity': 'ERROR',
                        'category': 'format-arg-mismatch',
                        'key': key,
                        'message': f"Argument-Mismatch: R.string.{key} erwartet {expected} Args, Aufruf hat {num_args} (Datei {path}, Zeile {line_num})",
                    })
    return issues


def main():
    parser = argparse.ArgumentParser(description='Validiert eine Android strings.xml nach dem string-extraktor-Skill.')
    parser.add_argument('strings_xml', help='Pfad zur strings.xml')
    parser.add_argument('--donottranslate', help='Pfad zur donottranslate.xml (optional)')
    parser.add_argument('--suggest-donottranslate', action='store_true',
                        help='Schlaegt vor welche Strings nach donottranslate.xml gehoeren')
    parser.add_argument('--code-dir', help='Pfad zum Kotlin-Code-Verzeichnis fuer Format-String-Check')
    parser.add_argument('--strict', action='store_true', help='WARN-Issues auch als Fehler werten')
    args = parser.parse_args()

    print(f"Validiere: {args.strings_xml}")
    print("=" * 60)

    entries, lines = parse_strings_xml(args.strings_xml)
    print(f"Eintraege gefunden: {len(entries)}")

    all_issues = []
    all_issues.extend(check_duplicate_keys(entries))
    all_issues.extend(check_duplicate_values(entries))
    all_issues.extend(check_unicode_escapes(entries))
    all_issues.extend(check_legacy_spelling(entries))
    all_issues.extend(check_denglish(entries))
    all_issues.extend(check_placeholder_numbering(entries))
    all_issues.extend(check_missing_xliff(entries))
    all_issues.extend(check_typewriter_quotes(entries))

    if args.code_dir:
        all_issues.extend(check_format_string_args(entries, args.code_dir))

    # Issues nach Severity gruppieren
    errors = [i for i in all_issues if i['severity'] == 'ERROR']
    warnings = [i for i in all_issues if i['severity'] == 'WARN']

    print(f"\nFehler:    {len(errors)}")
    print(f"Warnungen: {len(warnings)}")
    print("=" * 60)

    if errors:
        print("\nFEHLER (muessen behoben werden):")
        for issue in errors:
            print(f"  [{issue['category']}] {issue['message']}")

    if warnings:
        print("\nWARNUNGEN (sollten geprueft werden):")
        for issue in warnings:
            print(f"  [{issue['category']}] {issue['message']}")

    if args.suggest_donottranslate:
        candidates = check_donottranslate_candidates(entries)
        if candidates:
            print("\nKANDIDATEN FUER donottranslate.xml:")
            for c in candidates:
                print(f"  - {c['key']} ({c['reason']}): {c['value']!r}")
            print(f"\nBeispiel-donottranslate.xml-Eintrag:")
            for c in candidates[:3]:
                print(f"  <string name=\"{c['key']}\" translatable=\"false\">{c['value']}</string>")
        else:
            print("\nKeine donottranslate.xml-Kandidaten gefunden.")

    print("\n" + "=" * 60)
    if errors or (args.strict and warnings):
        print("VALIDIERUNG: FEHLGESCHLAGEN")
        sys.exit(1)
    else:
        print("VALIDIERUNG: BESTANDEN")
        sys.exit(0)


if __name__ == '__main__':
    main()
