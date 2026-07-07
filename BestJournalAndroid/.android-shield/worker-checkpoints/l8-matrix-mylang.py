# -*- coding: utf-8 -*-
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)

print("deTranslatableKeys:", matrix['deTranslatableKeys'])
print("dePlurals:", matrix['dePlurals'])
print("deArrays:", matrix['deArrays'])
print("=== ALL LOCALE NAMES ===")
print(sorted(matrix['locales'].keys()))

for loc in ['pt-rBR', 'pt-rPT']:
    print(f"\n=== LOCALE {loc} ===")
    if loc not in matrix['locales']:
        print("  NOT FOUND")
        continue
    d = matrix['locales'][loc]
    if isinstance(d, dict):
        for k, v in d.items():
            t = type(v).__name__
            if isinstance(v, list):
                print(f"  {k}: list(len={len(v)}) -> {v[:8]}{' ...' if len(v)>8 else ''}")
            elif isinstance(v, dict):
                print(f"  {k}: dict(len={len(v)}) keys={list(v.keys())[:10]}")
            else:
                print(f"  {k}: {t} = {v}")
