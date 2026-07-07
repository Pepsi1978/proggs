# -*- coding: utf-8 -*-
import json, os, sys
os.environ['PYTHONIOENCODING'] = 'utf-8'
shield = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
d = json.load(open(os.path.join(shield, 'l2_legal_pairs.json'), encoding='utf-8'))
present = d['present']
start = int(sys.argv[1]) if len(sys.argv) > 1 else 0
end = int(sys.argv[2]) if len(sys.argv) > 2 else start + 25
batch = present[start:end]
for k in batch:
    print('#' * 70)
    print('KEY:', k)
    print('  [DE]', d['de'].get(k))
    print('  [bn]', d['bn'].get(k))
    print('  [hi]', d['hi'].get(k))
    print('  [mr]', d['mr'].get(k))
print('\n--- batch %d..%d of %d ---' % (start, end, len(present)))
