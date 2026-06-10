# -*- coding: utf-8 -*-
import json, os, sys
os.environ['PYTHONIOENCODING'] = 'utf-8'
shield = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
d = json.load(open(os.path.join(shield, 'l2_legal_pairs.json'), encoding='utf-8'))
# keys passed as args
keys = sys.argv[1:]
for k in keys:
    print('#' * 60)
    print('KEY:', k)
    print(' DE|', d['de'].get(k))
    print(' bn|', d['bn'].get(k))
    print(' hi|', d['hi'].get(k))
    print(' mr|', d['mr'].get(k))
