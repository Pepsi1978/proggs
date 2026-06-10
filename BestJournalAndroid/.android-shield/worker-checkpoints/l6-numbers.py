# -*- coding: utf-8 -*-
"""Number check for ai_limits_dialog_body across de/en/es/fr/it.
Required numbers (per L6 brief): 150, 600, 30, 101, 151, 50, 5.
Counts occurrences of each token as a standalone number."""
import json
import re

d = json.load(open(r'C:/Users/barwa/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/l6-extracted.json', 'r', encoding='utf-8'))
n = d['numbers_ai_limits']
REQUIRED = ['150', '600', '30', '101', '151', '50', '5']

print('Required numbers:', REQUIRED)
print('=' * 60)
for lang in ['de', 'en', 'es', 'fr', 'it']:
    txt = n[lang] or ''
    # extract all integer tokens (handle 30-min, 101., 151e, 101ª etc -> \d+)
    nums = re.findall(r'\d+', txt)
    print(f'\n[{lang}] all int tokens in order: {nums}')
    counts = {}
    for r in REQUIRED:
        # count exact standalone occurrences
        c = len(re.findall(r'(?<!\d)' + re.escape(r) + r'(?!\d)', txt))
        counts[r] = c
    missing = [r for r in REQUIRED if counts[r] == 0]
    print(f'   counts: {counts}')
    if missing:
        print(f'   *** MISSING required numbers: {missing} ***')
    else:
        print('   all 7 required numbers present')
