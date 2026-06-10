#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
L3: Verify the genuine corruption concerns against DE reference (read-only).
1. ai_prompt_language_override empty in gu -> is DE also empty?
2. ai_limits_dialog_body box-drawing divider -> does DE have the same divider?
3. action_undo ends_with_halant -> confirm it's a valid Gujarati word (not truncation).
4. List ALL hard-corruption signals (FFFD/geometric/block) across gu/kn/ml.
Also classify the latin-fragment hits into: (a) brand/UI terms (acceptable),
(b) romanized-Gujarati transliteration in AI prompts (translation-quality, not corruption),
(c) leftover German schema keys inside JSON prompts (by design - field names).
"""
import os, sys, json, re
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
FILES = {
    'de': os.path.join(ROOT, 'app/src/main/res/values/strings.xml'),
    'gu': os.path.join(ROOT, 'app/src/main/res/values-gu/strings.xml'),
    'kn': os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml'),
    'ml': os.path.join(ROOT, 'app/src/main/res/values-ml/strings.xml'),
}

def inner_text(el):
    parts = []
    if el.text: parts.append(el.text)
    for ch in el:
        parts.append(ET.tostring(ch, encoding='unicode'))
        if ch.tail: parts.append(ch.tail)
    return ''.join(parts)

def load(path):
    root = ET.parse(path).getroot()
    d = {}
    for el in root:
        if el.tag == 'string':
            d[el.get('name')] = inner_text(el)
    return d

de = load(FILES['de'])
gu = load(FILES['gu'])
kn = load(FILES['kn'])
ml = load(FILES['ml'])

print("### CONCERN 1: ai_prompt_language_override empty in gu? ###")
for lang, d in [('de', de), ('gu', gu), ('kn', kn), ('ml', ml)]:
    v = d.get('ai_prompt_language_override', '<MISSING>')
    print(f"  {lang}: {v!r}")

print("\n### CONCERN 2: ai_limits_dialog_body divider — DE vs gu ###")
for lang, d in [('de', de), ('gu', gu)]:
    v = d.get('ai_limits_dialog_body', '<MISSING>')
    has_box = bool(re.search(r'[─-╿]', v))
    print(f"  {lang}: has_box_drawing={has_box}")
    print(f"     {v[:200]!r}")

print("\n### CONCERN 3: action_undo halant — valid word? ###")
print(f"  de: {de.get('action_undo')!r}")
print(f"  gu: {gu.get('action_undo')!r}  (પૂર્વવત્ = 'undo/revert', valid Gujarati ending in halant)")

# Hard corruption signals across all 3 target langs
print("\n### HARD CORRUPTION SIGNALS (FFFD / geometric / block) — gu/kn/ml ###")
GEO = re.compile(r'[■-◿]')
BLK = re.compile(r'[▀-▟]')
for lang, d in [('gu', gu), ('kn', kn), ('ml', ml)]:
    hits = []
    for k, v in d.items():
        if '�' in v: hits.append((k, 'FFFD'))
        if GEO.search(v): hits.append((k, 'geometric'))
        if BLK.search(v): hits.append((k, 'block'))
    print(f"  {lang}: {hits if hits else 'NONE'}")

# Empty strings across all 3
print("\n### EMPTY STRINGS — gu/kn/ml (excl. where DE also empty) ###")
for lang, d in [('gu', gu), ('kn', kn), ('ml', ml)]:
    empties = [k for k, v in d.items() if v.strip() == '']
    # which of these are empty in DE too?
    classified = [(k, 'DE-also-empty' if de.get(k, 'x').strip() == '' else 'GU-ONLY-EMPTY') for k in empties]
    print(f"  {lang}: {classified if classified else 'NONE'}")

# ai_limits_dialog_body number check (DE) — extract numbers
print("\n### ZAHLEN-CHECK: ai_limits_dialog_body numbers per language ###")
for lang, d in [('de', de), ('gu', gu), ('kn', kn), ('ml', ml)]:
    v = d.get('ai_limits_dialog_body', '')
    nums = re.findall(r'\d+', v)
    print(f"  {lang}: {nums}")
