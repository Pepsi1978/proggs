# -*- coding: utf-8 -*-
"""Dump legal-key DE/BR/PT triples + identicalToDe lists for manual review.
Writes to a JSON file so we don't flood the LLM context; prints compact summary."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from l8_lib import parse_strings  # noqa

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
root = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')

with open(os.path.join(base, 'legal-keys-2026-06-10.json'), 'r', encoding='utf-8') as f:
    legal = json.load(f)['legalCriticalKeys']

# Clean composite legal keys like "ai_prompt_insight_intro / profile_insight_*"
clean_keys = []
for k in legal:
    if '/' in k:
        for part in k.split('/'):
            part = part.strip()
            if part and '*' not in part:
                clean_keys.append(part)
    else:
        clean_keys.append(k.strip())
# dedupe preserve order
seen = set(); legal_keys = []
for k in clean_keys:
    if k not in seen:
        seen.add(k); legal_keys.append(k)

de_s, de_p, de_a = parse_strings(os.path.join(root, 'values/strings.xml'))
br_s, br_p, br_a = parse_strings(os.path.join(root, 'values-pt-rBR/strings.xml'))
pt_s, pt_p, pt_a = parse_strings(os.path.join(root, 'values-pt-rPT/strings.xml'))

triples = {}
present = 0
for k in legal_keys:
    de = de_s.get(k)
    br = br_s.get(k)
    pt = pt_s.get(k)
    if de is None and k in de_p:
        de = '[PLURAL] ' + json.dumps(de_p[k], ensure_ascii=False)
        br = '[PLURAL] ' + json.dumps(br_p.get(k, {}), ensure_ascii=False)
        pt = '[PLURAL] ' + json.dumps(pt_p.get(k, {}), ensure_ascii=False)
    triples[k] = {'de': de, 'br': br, 'pt': pt}
    if de is not None:
        present += 1

with open(os.path.join(base, 'l8-legal-triples.json'), 'w', encoding='utf-8') as f:
    json.dump(triples, f, ensure_ascii=False, indent=1)

# identicalToDe full lists (from matrix)
with open(os.path.join(base, 'cross-lingual-matrix-2026-06-10.json'), 'r', encoding='utf-8') as f:
    matrix = json.load(f)
br_ident = matrix['locales']['pt-rBR']['identicalToDe']
pt_ident = matrix['locales']['pt-rPT']['identicalToDe']

ident = {'br': {}, 'pt': {}}
for k in br_ident:
    ident['br'][k] = de_s.get(k, de_p.get(k))
for k in pt_ident:
    ident['pt'][k] = de_s.get(k, de_p.get(k))
with open(os.path.join(base, 'l8-identical.json'), 'w', encoding='utf-8') as f:
    json.dump(ident, f, ensure_ascii=False, indent=1)

print(f"Legal keys cleaned: {len(legal_keys)}; present in DE: {present}")
print(f"Missing-in-DE legal keys: {[k for k in legal_keys if triples[k]['de'] is None]}")
print(f"\nBR identicalToDe ({len(br_ident)}): {br_ident}")
print(f"\nPT identicalToDe ({len(pt_ident)}): {pt_ident}")
# also note which legal keys are missing in BR/PT
miss_br = [k for k in legal_keys if triples[k]['de'] is not None and triples[k]['br'] is None]
miss_pt = [k for k in legal_keys if triples[k]['de'] is not None and triples[k]['pt'] is None]
print(f"\nLegal keys present in DE but MISSING in BR: {miss_br}")
print(f"Legal keys present in DE but MISSING in PT: {miss_pt}")
