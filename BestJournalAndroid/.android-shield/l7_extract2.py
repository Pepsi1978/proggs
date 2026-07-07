# -*- coding: utf-8 -*-
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)

locales = matrix['locales']
print("ALL LOCALE KEYS:", list(locales.keys()))
print("uk present?:", 'uk' in locales)

# My 4 langs: nl, pl, uk, in
for lang in ['nl', 'pl', 'uk', 'in']:
    print(f"\n=== {lang} ===")
    if lang not in locales:
        print("  NOT IN MATRIX!")
        continue
    d = locales[lang]
    if isinstance(d, dict):
        for k, v in d.items():
            if isinstance(v, list):
                print(f"  {k}: list len={len(v)}")
            elif isinstance(v, dict):
                print(f"  {k}: dict keys={list(v.keys())[:10]}")
            else:
                print(f"  {k}: {repr(v)[:100]}")
