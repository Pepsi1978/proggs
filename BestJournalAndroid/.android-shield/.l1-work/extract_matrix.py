# -*- coding: utf-8 -*-
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
matrix_path = os.path.join(base, 'cross-lingual-matrix-2026-06-10.json')

with open(matrix_path, 'r', encoding='utf-8') as f:
    d = json.load(f)

out = {}
for lang in ['ar', 'ur', 'tr']:
    e = d['locales'][lang]
    out[lang] = {
        'keyCount': e['keyCount'],
        'missingKeys': e['missingKeys'],
        'identicalToDe': e['identicalToDe'],
        'formatArgMismatch': e['formatArgMismatch'],
        'nakedPercentKeys': e['nakedPercentKeys'],
        'unescapedApostrophes': e['unescapedApostrophes'],
    }

outpath = os.path.join(base, '.l1-work', 'my-matrix-data.json')
with open(outpath, 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=2)

# Print summary
for lang in ['ar', 'ur', 'tr']:
    e = out[lang]
    print(f'=== {lang} ===')
    print(f'  keyCount: {e["keyCount"]}')
    print(f'  missingKeys ({len(e["missingKeys"])}): {e["missingKeys"]}')
    print(f'  identicalToDe ({len(e["identicalToDe"])}): {e["identicalToDe"]}')
    print(f'  formatArgMismatch ({len(e["formatArgMismatch"])}): {e["formatArgMismatch"]}')
print(f'\nWritten to {outpath}')
