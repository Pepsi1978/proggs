# -*- coding: utf-8 -*-
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)

# Inspect top-level structure
print("=== TOP-LEVEL KEYS ===")
if isinstance(matrix, dict):
    for k in matrix.keys():
        v = matrix[k]
        t = type(v).__name__
        n = len(v) if hasattr(v, '__len__') else ''
        print(f"  {k}: {t} (len={n})")
else:
    print("  matrix is a", type(matrix).__name__, "len", len(matrix))
    if len(matrix) > 0:
        print("  first element type:", type(matrix[0]).__name__)
        if isinstance(matrix[0], dict):
            print("  first element keys:", list(matrix[0].keys()))
