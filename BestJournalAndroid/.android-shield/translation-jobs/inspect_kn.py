#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Inspektion fuer kn-Worker: liest job-plan kn-Keys, prueft Ist-Zustand in values-kn/strings.xml.
Laeuft im Subprozess -> belastet den LLM-Kontext NICHT (FIN-048)."""
import json, os, re, sys

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
JOBS = os.path.join(ROOT, '.android-shield', 'translation-jobs')
KN = os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml')

with open(os.path.join(JOBS, 'job-plan.json'), 'r', encoding='utf-8') as f:
    plan = json.load(f)
kn = plan['languages']['kn']
print("job-plan kn count field:", kn['count'])
print("strings list len:", len(kn['strings']))
print("plurals list len:", len(kn['plurals']))
print("STRINGS:", kn['strings'])
print("PLURALS:", kn['plurals'])

with open(os.path.join(JOBS, 'de-reference.json'), 'r', encoding='utf-8') as f:
    deref = json.load(f)
de_strings = deref['strings']
de_plurals = deref['plurals']

# Welche kn-Strings haben KEINE DE-Quelle in de-reference? (sollte 0 sein)
missing_src = [k for k in kn['strings'] if k not in de_strings]
print("kn-strings ohne DE-Quelle:", missing_src)
missing_pl = [k for k in kn['plurals'] if k not in de_plurals]
print("kn-plurals ohne DE-Quelle:", missing_pl)

# Aktuellen kn-Inhalt einlesen (NUR im Subprozess!)
with open(KN, 'r', encoding='utf-8') as f:
    content = f.read()

def get_string_val(name):
    # <string name="x">...</string>  (multiline-tolerant)
    m = re.search(r'<string name="' + re.escape(name) + r'"[^>]*>(.*?)</string>', content, re.S)
    return m.group(1) if m else None

print("\n=== IST-ZUSTAND kn fuer jeden Ziel-String ===")
for k in kn['strings']:
    v = get_string_val(k)
    if v is None:
        print(f"  [FEHLT] {k}")
    else:
        snippet = v.replace('\n',' ')[:70]
        print(f"  [DA len={len(v)}] {k}: {snippet}")

print("\n=== plurals (ganze Bloecke) ===")
for pk in kn['plurals']:
    m = re.search(r'<plurals name="' + re.escape(pk) + r'">(.*?)</plurals>', content, re.S)
    if m:
        print(f"--- {pk} ---")
        print(m.group(0))
    else:
        print(f"  [FEHLT plurals] {pk}")

# Sind ai_limits_dialog_body / paywall_monthly_note vorhanden?
print("\n=== Sonderfaelle ===")
for k in ['ai_limits_dialog_body','paywall_monthly_note']:
    v = get_string_val(k)
    print(f"{k}: {'DA' if v is not None else 'FEHLT'}", (("| 150-count="+str(v.count('150'))) if (k=='ai_limits_dialog_body' and v) else ''))

# ZWNJ-Nutzung im Bestand?
zwnj = content.count('‌')
print("\nZWNJ (U+200C) Vorkommen im kn-File gesamt:", zwnj)
