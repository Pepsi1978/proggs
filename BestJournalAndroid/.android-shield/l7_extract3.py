# -*- coding: utf-8 -*-
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)

locales = matrix['locales']
out = {}
for lang in ['nl', 'pl', 'uk', 'in']:
    d = locales[lang]
    out[lang] = {
        'identicalToDe': d.get('identicalToDe', []),
        'formatArgMismatch': d.get('formatArgMismatch', []),
        'missingKeys': d.get('missingKeys', []),
        'nakedPercentKeys': d.get('nakedPercentKeys', []),
        'unescapedApostrophes': d.get('unescapedApostrophes', 0),
    }

# Save to file-as-memory
with open(os.path.join(base, 'l7-matrix-slice.json'), 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=1)

# Print compactly for inspection
for lang in ['nl', 'pl', 'uk', 'in']:
    print(f"\n========== {lang} ==========")
    print("formatArgMismatch:", out[lang]['formatArgMismatch'])
    print("missingKeys:", out[lang]['missingKeys'])
    print("identicalToDe (%d):" % len(out[lang]['identicalToDe']))
    for k in out[lang]['identicalToDe']:
        print("   ", k)
