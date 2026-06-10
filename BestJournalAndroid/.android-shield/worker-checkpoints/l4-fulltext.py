# -*- coding: utf-8 -*-
"""L4: Volltext einzelner kritischer Keys (keine Kuerzung)."""
import json, os, sys
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'worker-checkpoints', 'l4-legal-pairs.json'), 'r', encoding='utf-8') as f:
    pairs = json.load(f)['pairs']
keys = sys.argv[1:] if len(sys.argv) > 1 else ['consent_toggle_do_not_sell_body']
for k in keys:
    if k not in pairs:
        print(f"!! {k} nicht vorhanden"); continue
    p = pairs[k]
    print(f"\n========== {k} ==========")
    for lang in ['de', 'ta', 'te', 'th']:
        print(f"\n--{lang}--")
        print(p[lang])
