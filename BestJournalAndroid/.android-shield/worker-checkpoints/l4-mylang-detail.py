# -*- coding: utf-8 -*-
"""L4: Detail meiner 3 Sprachen ta/te/th + Checkpoint schreiben"""
import json, os, tempfile

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)

mylangs = ['ta', 'te', 'th']
detail = {}
for lang in mylangs:
    d = matrix['locales'][lang]
    detail[lang] = {
        'keyCount': d['keyCount'],
        'missingKeys': d['missingKeys'],
        'identicalToDe': d['identicalToDe'],
        'formatArgMismatch': d['formatArgMismatch'],
        'nakedPercentKeys': d['nakedPercentKeys'],
        'unescapedApostrophes': d['unescapedApostrophes'],
    }
    print(f"=== {lang} ===")
    print(f"  missingKeys ({d['missingCount']}): {d['missingKeys']}")
    print(f"  identicalToDe ({d['identicalCount']}):")
    for k in d['identicalToDe']:
        print(f"    - {k}")
    print(f"  formatArgMismatch: {d['formatArgMismatch']}")
    print()

# Checkpoint schreiben
ckpt = os.path.join(base, 'worker-checkpoints', 'l4-ta-te-th.jsonl')
with open(ckpt, 'a', encoding='utf-8') as f:
    f.write(json.dumps({'step': 'matrix-extract', 'detail': detail}, ensure_ascii=False) + '\n')
print("Checkpoint geschrieben:", ckpt)
