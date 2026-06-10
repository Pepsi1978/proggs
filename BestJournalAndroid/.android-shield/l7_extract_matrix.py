# -*- coding: utf-8 -*-
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)

# Show top-level structure
print("=== TOP-LEVEL KEYS ===")
print(list(matrix.keys()) if isinstance(matrix, dict) else "LIST len=" + str(len(matrix)))

if isinstance(matrix, dict):
    for k, v in matrix.items():
        if isinstance(v, dict):
            print(f"  {k}: dict keys={list(v.keys())[:20]}")
        elif isinstance(v, list):
            print(f"  {k}: list len={len(v)}")
        else:
            print(f"  {k}: {repr(v)[:120]}")
