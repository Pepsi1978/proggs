# -*- coding: utf-8 -*-
"""KRITISCH zahlen-check for ai_limits_dialog_body across nl/pl/uk/in.
The mandated numbers: 150, 600, 30, 101, 151, 50, 5 (also 15).
Print full text + ordered number sequence per lang."""
import json, os, re

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'l7-parsed-strings.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)

key = 'ai_limits_dialog_body'
de = data['de'][key]
NUM = re.compile(r'\d+')

# Mandated numbers from briefing
mandated = ['150', '600', '30', '101', '151', '50', '5']

def num_seq(s):
    return NUM.findall(s)

print("===== DE FULL =====")
print(de.replace('\\n', '\n'))
print("\nDE number sequence:", num_seq(de))
print("Mandated:", mandated)

for lang in ['nl', 'pl', 'uk', 'in']:
    v = data[lang][key]
    seq = num_seq(v)
    print(f"\n\n===== {lang} FULL =====")
    print(v.replace('\\n', '\n'))
    print(f"\n{lang} number sequence:", seq)
    missing = [m for m in mandated if m not in seq]
    print(f"{lang} MANDATED-MISSING:", missing if missing else "none — all present")
