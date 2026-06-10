#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Apply Kannada (kn) translations to values-kn/strings.xml.
FIN-048: bearbeitet die Ziel-Datei NUR per Python (kein Read-Tool).
Strategie:
  - Vorhandene <string name="K">: kompletten Tag-Inhalt durch kn ersetzen (regex, multiline).
  - Fehlende <string> (paywall_monthly_note): per rfind vor </resources> einfuegen.
  - <plurals>: ganzen Block durch kn-Version ersetzen (one/other).
Atomares Schreiben (temp->rename). XML-Validierung danach.
"""
import json, os, re, sys, tempfile
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
JOBS = os.path.join(ROOT, '.android-shield', 'translation-jobs')
KN = os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml')

with open(os.path.join(JOBS, 'kn-data.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)
strings = data['strings']
plurals = data['plurals']

with open(KN, 'r', encoding='utf-8') as f:
    content = f.read()

# Pre-check: XML schon vor dem Edit valide?
try:
    ET.fromstring(content)
except ET.ParseError as e:
    print(f"VORBESTEHENDER XML-FEHLER: {e}")
    sys.exit(2)

applied = []
inserted = []
skipped = []
errors = []

def esc_attr_safe(val):
    # Werte aus kn-data.json sind bereits XML-/Android-konform escaped (\n literal, \" als \\\").
    return val

# 1) STRINGS: ersetzen oder einfuegen
for key, val in strings.items():
    # Existiert der String? (multiline-tolerant)
    pat = re.compile(r'(<string name="' + re.escape(key) + r'"[^>]*>)(.*?)(</string>)', re.S)
    m = pat.search(content)
    if m:
        # ersetzen
        new_tag = m.group(1) + val + m.group(3)
        # Falls bereits identisch -> skip
        if m.group(0) == new_tag:
            skipped.append(key)
        else:
            content = content[:m.start()] + new_tag + content[m.end():]
            applied.append(key)
    else:
        # einfuegen via rfind
        idx = content.rfind('</resources>')
        if idx == -1:
            errors.append(f"{key}: kein </resources> fuer Insert")
            continue
        new_string = f'    <string name="{key}">{val}</string>\n'
        content = content[:idx] + new_string + content[idx:]
        inserted.append(key)

# 2) PLURALS: ganzen Block ersetzen
for pk, forms in plurals.items():
    pat = re.compile(r'<plurals name="' + re.escape(pk) + r'">.*?</plurals>', re.S)
    m = pat.search(content)
    items = ''.join(
        f'\n        <item quantity="{q}">{forms[q]}</item>' for q in ['one','other'] if q in forms
    )
    new_block = f'<plurals name="{pk}">{items}\n    </plurals>'
    if m:
        if m.group(0) == new_block:
            skipped.append(pk)
        else:
            content = content[:m.start()] + new_block + content[m.end():]
            applied.append(pk)
    else:
        idx = content.rfind('</resources>')
        new_block_ins = '    ' + new_block + '\n'
        content = content[:idx] + new_block_ins + content[idx:]
        inserted.append(pk)

# Post-check: XML valide nach dem Edit?
try:
    ET.fromstring(content)
except ET.ParseError as e:
    print(f"XML BESCHAEDIGT DURCH EDIT: {e}")
    sys.exit(2)

# Tags-in-Tags check
if re.search(r'<string[^>]*>[^<]*<string', content):
    print("KRITISCH: verschachtelte <string>-Tags!")
    sys.exit(2)

# Atomar schreiben
dir_name = os.path.dirname(KN)
with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp', delete=False,
                                 encoding='utf-8', newline='\n') as tmp:
    tmp.write(content)
    tmp_path = tmp.name
os.replace(tmp_path, KN)

print("APPLIED (ersetzt):", len(applied), applied)
print("INSERTED (neu):", len(inserted), inserted)
print("SKIPPED (identisch):", len(skipped), skipped)
print("ERRORS:", errors)
print("XML OK — Edit sauber")
