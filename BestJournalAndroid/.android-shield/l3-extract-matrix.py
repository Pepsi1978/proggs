#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""L3: Extract gu/kn/ml slice from cross-lingual matrix."""
import json, os, sys

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
mat = json.load(open(os.path.join(ROOT, '.android-shield/cross-lingual-matrix-2026-06-10.json'), encoding='utf-8'))

print("GLOBAL: deTranslatableKeys=%s dePlurals=%s deArrays=%s" % (
    mat['deTranslatableKeys'], mat['dePlurals'], mat['deArrays']))
print("="*70)

for lang in ['gu', 'kn', 'ml']:
    L = mat['locales'][lang]
    print("\n### %s ###" % lang)
    print("  keyCount=%s deKeyCount=%s" % (L['keyCount'], L['deKeyCount']))
    print("  missingCount=%s  extraCount=%s" % (L['missingCount'], L['extraCount']))
    print("  identicalCount=%s" % L['identicalCount'])
    print("  formatArgMismatch (%d): %s" % (len(L['formatArgMismatch']), L['formatArgMismatch']))
    print("  nakedPercentKeys (%d): %s" % (len(L['nakedPercentKeys']), L['nakedPercentKeys']))
    print("  unescapedApostrophes: %s" % L['unescapedApostrophes'])
    print("  missingPlurals: %s" % L['missingPlurals'])
    print("  missingArrays: %s" % L['missingArrays'])
    print("  missingKeys (%d): %s" % (len(L['missingKeys']), L['missingKeys'][:20]))
    # extra keys
    if L['extraKeys']:
        print("  extraKeys (%d): %s" % (len(L['extraKeys']), L['extraKeys'][:20]))
    # identical list — classify json_key_ prefix
    ident = L['identicalToDe']
    json_keys = [k for k in ident if k.startswith('json_key_')]
    other = [k for k in ident if not k.startswith('json_key_')]
    print("  identicalToDe -> json_key_*=%d  OTHER=%d" % (len(json_keys), len(other)))
    print("    OTHER list: %s" % other)
