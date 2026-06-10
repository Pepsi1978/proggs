#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""L3: Inspect settings_revoke_subtitle + kn privacy_gate_gemini_body (read-only)."""
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

def load(path):
    root = ET.parse(path).getroot()
    return {el.get('name'): inner_text(el) for el in root if el.tag == 'string'}

de = load(os.path.join(ROOT, 'app/src/main/res/values/strings.xml'))
gu = load(os.path.join(ROOT, 'app/src/main/res/values-gu/strings.xml'))
kn = load(os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml'))
ml = load(os.path.join(ROOT, 'app/src/main/res/values-ml/strings.xml'))

print("### settings_revoke_subtitle (context: sibling settings_revoke_* keys) ###")
for k in ['settings_revoke_subtitle', 'settings_revoke_email_subject', 'settings_revoke_email_body']:
    print(f"\n  KEY={k}")
    for lang, d in [('de', de), ('gu', gu), ('kn', kn), ('ml', ml)]:
        v = d.get(k, '<MISSING>')
        print(f"    {lang}: {v[:140]!r}")

print("\n\n### kn privacy_gate_gemini_body: DE '9' vs kn '200,200' ###")
for lang, d in [('de', de), ('kn', kn)]:
    v = d.get('privacy_gate_gemini_body', '<MISSING>')
    print(f"\n  {lang} (full):\n    {v!r}")
