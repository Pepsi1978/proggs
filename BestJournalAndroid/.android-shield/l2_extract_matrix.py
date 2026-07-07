# -*- coding: utf-8 -*-
import json, os
os.environ['PYTHONIOENCODING'] = 'utf-8'
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
d = json.load(open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), encoding='utf-8'))
loc = d['locales']
for L in ['bn', 'hi', 'mr']:
    v = loc[L]
    print('=' * 60)
    print('LANG', L)
    print('  keyCount', v.get('keyCount'), 'deKeyCount', v.get('deKeyCount'))
    print('  missingCount', v.get('missingCount'), 'extraCount', v.get('extraCount'))
    print('  missingKeys:', v.get('missingKeys'))
    print('  missingPlurals:', v.get('missingPlurals'))
    print('  missingArrays:', v.get('missingArrays'))
    print('  identicalCount', v.get('identicalCount'))
    idl = v.get('identicalToDe', [])
    print('  identicalToDe (', len(idl), '):')
    for k in idl:
        print('     ', k)
    print('  formatArgMismatch:', json.dumps(v.get('formatArgMismatch'), ensure_ascii=False))
    print('  nakedPercentKeys:', v.get('nakedPercentKeys'))
    print('  unescapedApostrophes:', v.get('unescapedApostrophes'))
