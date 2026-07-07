# -*- coding: utf-8 -*-
"""identicalToDe classification across nl(39)/pl(12)/uk(2)/in(19).
Print DE value (which == loc value) for each identical key so we can judge
legit-identical (brand/loanword/month/JSON-key) vs untranslated-German."""
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'l7-parsed-strings.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)
with open(os.path.join(base, 'l7-matrix-slice.json'), 'r', encoding='utf-8') as f:
    sl = json.load(f)

de = data['de']
for lang in ['nl', 'pl', 'uk', 'in']:
    ids = sl[lang]['identicalToDe']
    print(f"\n{'='*70}\n{lang.upper()}  identicalToDe = {len(ids)}\n{'='*70}")
    for k in ids:
        dv = de.get(k, '<<no DE>>')
        lv = data[lang].get(k, '<<no LOC>>')
        same = "==" if dv == lv else "!! DIFFERS"
        short = dv if len(dv) < 90 else dv[:90] + '…'
        print(f"  [{same}] {k}: {repr(short)}")
