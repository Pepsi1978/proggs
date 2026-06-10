# -*- coding: utf-8 -*-
import json

p = r'C:/Users/barwa/proggs/BestJournalAndroid/.android-shield/cross-lingual-matrix-2026-06-10.json'
d = json.load(open(p, 'r', encoding='utf-8'))

print('TOP-LEVEL TYPES:')
for k, v in d.items():
    t = type(v).__name__
    if isinstance(v, (dict, list)):
        print(f'  {k}: {t} len={len(v)}')
    else:
        print(f'  {k}: {t} = {v!r}')

loc = d.get('locales')
print('\nLOCALES TYPE:', type(loc).__name__)
if isinstance(loc, dict):
    print('LOCALE KEYS:', list(loc.keys()))
    for lang in ['en', 'es', 'fr', 'it']:
        sub = loc.get(lang)
        if sub is None:
            print(f'\n{lang}: MISSING')
            continue
        print(f'\n=== {lang} ===  type={type(sub).__name__}')
        if isinstance(sub, dict):
            for sk, sv in sub.items():
                if isinstance(sv, (dict, list)):
                    print(f'   {sk}: {type(sv).__name__} len={len(sv)}')
                    if isinstance(sv, list) and sv[:3]:
                        print(f'       sample: {sv[:5]}')
                else:
                    print(f'   {sk}: {sv!r}')
elif isinstance(loc, list):
    print('LOCALE LIST sample:', loc[:2])
