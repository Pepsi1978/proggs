# -*- coding: utf-8 -*-
import json, os, sys

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, '.l1-work', 'legal-pairs.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)

lang = sys.argv[1]            # ar | ur | tr
start = int(sys.argv[2])      # batch start index
end = int(sys.argv[3])        # batch end index (exclusive)

pairs = data['langs'][lang]
keys = sorted(pairs.keys())
batch = keys[start:end]
print(f'### {lang} batch [{start}:{end}] of {len(keys)} legal keys ###\n')
for k in batch:
    p = pairs[k]
    de_v = p['de']
    loc_v = p['loc']
    if de_v is None:
        print(f'[{k}] (NOT IN DE — note entry)')
        print(f'  LOC: {loc_v}')
        print()
        continue
    if loc_v is None:
        print(f'[{k}] *** MISSING IN {lang.upper()} ***')
        print(f'  DE : {de_v}')
        print()
        continue
    print(f'[{k}]')
    print(f'  DE : {de_v}')
    print(f'  {lang.upper()}: {loc_v}')
    print()
