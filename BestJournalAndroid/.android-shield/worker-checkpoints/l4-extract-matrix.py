# -*- coding: utf-8 -*-
"""L4: Extrahiere ta/te/th aus cross-lingual-matrix-2026-06-10.json"""
import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)

print("=== TOP-LEVEL KEYS ===")
print(list(matrix.keys()))
print()

# Struktur erkunden
def describe(obj, depth=0, maxd=3):
    pad = "  " * depth
    if depth > maxd:
        return
    if isinstance(obj, dict):
        for k in list(obj.keys())[:25]:
            v = obj[k]
            if isinstance(v, (dict, list)):
                n = len(v)
                print(f"{pad}{k}: {type(v).__name__}[{n}]")
                if depth < 2:
                    describe(v, depth+1, maxd)
            else:
                sval = str(v)[:60]
                print(f"{pad}{k}: {sval}")
    elif isinstance(obj, list):
        print(f"{pad}list[{len(obj)}], first item type: {type(obj[0]).__name__ if obj else 'empty'}")
        if obj and isinstance(obj[0], dict):
            print(f"{pad}  first item keys: {list(obj[0].keys())}")

describe(matrix)
