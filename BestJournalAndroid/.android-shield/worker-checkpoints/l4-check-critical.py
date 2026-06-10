# -*- coding: utf-8 -*-
"""L4: Kritische Einzel-Keys zeigen: ai_limits_dialog_body, ai_prompt_language_override,
   sowie alle consent/churn/paywall Legal-Keys-Werte fuer Augenschein."""
import json, os
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'worker-checkpoints', 'l4-legal-pairs.json'), 'r', encoding='utf-8') as f:
    blob = json.load(f)
pairs = blob['pairs']

def show(key):
    if key not in pairs:
        print(f"  [{key}] NICHT in pairs")
        return
    p = pairs[key]
    print(f"\n### {key}")
    for lang in ['de', 'ta', 'te', 'th']:
        v = p[lang].replace('\n', '\\n')
        print(f"  {lang}: {v}")

# 1. ai_limits_dialog_body — ZAHLEN-CHECK
show('ai_limits_dialog_body')
# 2. ai_limit_yearly
show('ai_limit_yearly')
# 3. language override (war leer)
print("\n--- ai_prompt_language_override (aus DE direkt) ---")
# Diese ist kein legal key, separat lesen
import xml.etree.ElementTree as ET
app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
def get1(path, key):
    tree = ET.parse(path)
    for el in tree.getroot():
        if el.tag=='string' and el.get('name')==key:
            return ''.join(el.itertext())
    return '__ABSENT__'
for lang in ['de','ta','te','th']:
    sub = 'values' if lang=='de' else f'values-{lang}'
    val = get1(os.path.join(app, sub, 'strings.xml'), 'ai_prompt_language_override')
    print(f"  {lang}: '{val}'  (len={len(val)})")
