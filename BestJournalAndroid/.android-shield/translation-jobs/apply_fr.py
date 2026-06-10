# -*- coding: utf-8 -*-
"""Applier fuer fr (Franzoesisch) - finale Phase 3 translation-worker.
Liest fr-data.json, ersetzt vorhandene String-Keys, fuegt paywall_monthly_note NEU ein,
ersetzt die 2 Plurals. Apostroph-Escaping: ' -> \\' (Android XML Build-Blocker).
Alle Datei-IO per Python. rfind fuer Insert. Pre-Check ist im Bash-Schritt erfolgt.
"""
import json, os, re, sys

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-fr/strings.xml')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/fr-data.json')

with open(DATA, 'r', encoding='utf-8') as f:
    data = json.load(f)

with open(TARGET, 'r', encoding='utf-8') as f:
    content = f.read()

applied_strings = []
applied_plurals = []
skipped = []
report = {}

def esc_apos(text):
    """Escape unescaped apostrophes for Android XML (non-quoted strings).
    Our data has \\u202F / \\u00A0 already as real chars (json decoded them);
    literal newlines are encoded as the two chars backslash+n in the json source
    (\\\\n), which json decodes to backslash+n -> stays as \\n in XML. Good.
    Only apostrophe needs escaping. Avoid double-escaping an already-\\' ."""
    return re.sub(r"(?<!\\)'", r"\\'", text)

# --- 1. Replace / insert string keys ---
new_key = 'paywall_monthly_note'
for key, value in data['strings'].items():
    xml_val = esc_apos(value)
    element = '<string name="%s">%s</string>' % (key, xml_val)
    # pattern matches the existing element (DOTALL for multiline bodies)
    pat = re.compile(r'<string name="%s"[^>]*>.*?</string>' % re.escape(key), re.DOTALL)
    if pat.search(content):
        content = pat.sub(lambda m: element, content, count=1)
        applied_strings.append(key)
    elif key == new_key:
        # NEW: insert before </resources>
        idx = content.rfind('</resources>')
        if idx == -1:
            raise RuntimeError('kein </resources> gefunden')
        content = content[:idx] + '    ' + element + '\n' + content[idx:]
        applied_strings.append(key + ' (NEW)')
    else:
        skipped.append(key)

# --- 2. Replace plurals ---
for pkey, items in data['plurals'].items():
    quantities = ''.join(
        '\n        <item quantity="%s">%s</item>' % (q, esc_apos(t))
        for q, t in items.items()
    )
    element = '<plurals name="%s">%s\n    </plurals>' % (pkey, quantities)
    pat = re.compile(r'<plurals name="%s">.*?</plurals>' % re.escape(pkey), re.DOTALL)
    if pat.search(content):
        content = pat.sub(lambda m: element, content, count=1)
        applied_plurals.append(pkey)
    else:
        skipped.append('plural:' + pkey)

with open(TARGET, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

print('applied_strings=%d' % len(applied_strings))
print('applied_plurals=%d' % len(applied_plurals))
print('skipped=%s' % skipped)
