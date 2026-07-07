#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""L3: Deep-dive ai_limits_dialog_body number discrepancy in kn (read-only)."""
import os, sys, re
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')

def inner_text(el):
    parts = []
    if el.text: parts.append(el.text)
    for ch in el:
        parts.append(ET.tostring(ch, encoding='unicode'))
        if ch.tail: parts.append(ch.tail)
    return ''.join(parts)

def get(path, key):
    root = ET.parse(path).getroot()
    for el in root:
        if el.tag == 'string' and el.get('name') == key:
            return inner_text(el)
    return None

KEY = 'ai_limits_dialog_body'
de = get(os.path.join(ROOT, 'app/src/main/res/values/strings.xml'), KEY)
kn = get(os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml'), KEY)
gu = get(os.path.join(ROOT, 'app/src/main/res/values-gu/strings.xml'), KEY)
ml = get(os.path.join(ROOT, 'app/src/main/res/values-ml/strings.xml'), KEY)

# Split into logical lines (on \n escape) and show line-by-line with numbers
def lines(s):
    return s.split('\\n')

print("=== DE full text (line by line, with numbers per line) ===")
for i, ln in enumerate(lines(de)):
    nums = re.findall(r'\d+', ln)
    if nums or ln.strip():
        print(f"  D{i:02d} {nums}: {ln.strip()[:120]}")

print("\n=== KN full text (line by line, with numbers per line) ===")
for i, ln in enumerate(lines(kn)):
    nums = re.findall(r'\d+', ln)
    if nums or ln.strip():
        print(f"  K{i:02d} {nums}: {ln.strip()[:120]}")

# Focused: the 4 critical magnitudes that must appear: 150, 600, 30, 101, 151, 50, 5
print("\n=== CRITICAL NUMBER PRESENCE (expected per prompt: 150,600,30,101,151,50,5) ===")
crit = ['150', '600', '30', '101', '151', '50', '5']
for lang, s in [('de', de), ('gu', gu), ('kn', kn), ('ml', ml)]:
    allnums = re.findall(r'\d+', s)
    cnt = {c: allnums.count(c) for c in crit}
    print(f"  {lang}: {cnt}  (total tokens={len(allnums)})")
