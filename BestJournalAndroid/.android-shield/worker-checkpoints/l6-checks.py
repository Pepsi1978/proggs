# -*- coding: utf-8 -*-
"""Apostrophe spot-check, formality consistency, random 15-string sample for es/fr/it/en."""
import json
import xml.etree.ElementTree as ET
import os
import re
import random

ROOT = r'C:/Users/barwa/proggs/BestJournalAndroid/app/src/main/res'


def parse(path):
    tree = ET.parse(path)
    root = tree.getroot()
    out = {}
    for el in root.findall('string'):
        n = el.get('name')
        if n:
            out[n] = ''.join(el.itertext())
    return out


maps = {l: parse(os.path.join(ROOT, f'values-{l}', 'strings.xml')) for l in ['en', 'es', 'fr', 'it']}

# 1) APOSTROPHE SPOT-CHECK (es/fr/it): find strings with apostrophes, check escaping pattern
print('=' * 70)
print('APOSTROPHE SPOT-CHECK (es/fr/it) — looking for elided forms l\'/d\'/n\'/etc')
print('=' * 70)
# common romance elisions
PAT = re.compile(r"\b([ldsnctjmLDSNCTJM]|qu|all|dell|nell|sull|un|nessun)['’]", re.IGNORECASE)
for l in ['es', 'fr', 'it']:
    hits = []
    for k, v in maps[l].items():
        for m in PAT.finditer(v):
            hits.append((k, m.group(0)))
    print(f'\n[{l}] strings with elided apostrophe forms: {len(hits)} total')
    # show 8 samples
    for k, frag in hits[:8]:
        # show a small window
        v = maps[l][k]
        idx = v.lower().find(frag.lower())
        win = v[max(0, idx - 5):idx + 25].replace('\n', ' ')
        print(f'   {k}: ...{win}...')

# 2) FORMALITY CONSISTENCY
print('\n' + '=' * 70)
print('FORMALITY CHECK — informal markers per language')
print('=' * 70)
MARK = {
    'es': {'informal_tu': [r'\btú\b', r'\bquieres\b', r'\btus\b', r'\btu\b', r'\bpuedes\b', r'\btoca(r|ndo)\b', r'\bestás\b'],
           'formal_usted': [r'\busted\b', r'\bsu\b(?! )', r'\bpuede\b', r'\bquiere\b', r'\bsus aplicaciones\b']},
    'fr': {'informal_tu': [r'\btu\b', r'\bton\b', r'\btes\b', r'\bt[’\']', r'\bveux\b', r'\bpeux\b'],
           'formal_vous': [r'\bvous\b', r'\bvotre\b', r'\bvos\b']},
    'it': {'informal_tu': [r'\btu\b', r'\btuo\b', r'\btuoi\b', r'\btue\b', r'\bvuoi\b', r'\bpuoi\b', r'\bti\b'],
           'formal_lei': [r'\bLei\b', r'\bSuo\b', r'\bVoi\b']},
}
for l in ['es', 'fr', 'it']:
    allv = ' \n '.join(maps[l].values())
    print(f'\n[{l}]')
    for label, pats in MARK[l].items():
        total = sum(len(re.findall(p, allv)) for p in pats)
        print(f'   {label}: {total} marker hits')

# 3) RANDOM 15-STRING SAMPLE per language
print('\n' + '=' * 70)
print('RANDOM 15-STRING SAMPLE (en/es/fr/it) with DE reference')
print('=' * 70)
de = parse(os.path.join(ROOT, 'values', 'strings.xml'))
random.seed(606)  # deterministic
common = [k for k in de.keys() if all(k in maps[l] for l in ['en', 'es', 'fr', 'it'])
          and not k.startswith('json_key') and len(de[k]) > 8 and len(de[k]) < 120]
for l in ['en', 'es', 'fr', 'it']:
    sample = random.sample(common, 15)
    print(f'\n----- {l} sample (15) -----')
    for k in sample:
        print(f'  [{k}]')
        print(f'    de: {de[k][:110]!r}')
        print(f'    {l}: {maps[l][k][:110]!r}')
