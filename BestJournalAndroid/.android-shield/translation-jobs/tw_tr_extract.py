# -*- coding: utf-8 -*-
import json, os, subprocess

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
de = json.load(open(os.path.join(base,'de-reference.json'), encoding='utf-8'))
jp = json.load(open(os.path.join(base,'job-plan.json'), encoding='utf-8'))
tr = jp['languages']['tr']

strings = tr['strings']
plurals = tr['plurals']
special = tr['special'][0]
legend_keys = special.split(': ',1)[1].split(',')

de_strings = de.get('strings', {})
de_plurals = de.get('plurals', {})

print("=== DE STRINGS (34) ===")
for k in strings:
    v = de_strings.get(k, '<<MISSING>>')
    print(f"[{k}]\n{v!r}\n")

print("=== DE PLURALS (2) ===")
for k in plurals:
    print(f"[{k}] {de_plurals.get(k, '<<MISSING>>')!r}\n")

print("=== LEGEND KEYS (15) ===")
print(legend_keys)
