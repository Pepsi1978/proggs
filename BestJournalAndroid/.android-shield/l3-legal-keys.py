#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
L3: Legal-key deep check (read-only) for gu/kn/ml.
- Pair DE + target for the 95 legal keys.
- For each: present? identical-to-DE? placeholder set match? number set match?
  Gujarati/Kannada/Malayalam script present (translated)?
- Classify identicalToDe 'other' entries.
Outputs machine-readable JSON for the final stage.
"""
import os, sys, json, re
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
SHIELD = os.path.join(ROOT, '.android-shield')

legal = json.load(open(os.path.join(SHIELD, 'legal-keys-2026-06-10.json'), encoding='utf-8'))
# Clean keys: some entries have " / " alias notation -> take primary token
raw_keys = legal['legalCriticalKeys']
keys = []
for k in raw_keys:
    primary = k.split(' / ')[0].split(' /')[0].strip()
    keys.append(primary)

def inner_text(el):
    parts = []
    if el.text: parts.append(el.text)
    for ch in el:
        parts.append(ET.tostring(ch, encoding='unicode'))
        if ch.tail: parts.append(ch.tail)
    return ''.join(parts)

def load(path):
    root = ET.parse(path).getroot()
    d = {}
    for el in root:
        if el.tag == 'string':
            d[el.get('name')] = inner_text(el)
    return d

de = load(os.path.join(ROOT, 'app/src/main/res/values/strings.xml'))
langs = {
    'gu': (load(os.path.join(ROOT, 'app/src/main/res/values-gu/strings.xml')), (0x0A80, 0x0AFF)),
    'kn': (load(os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml')), (0x0C80, 0x0CFF)),
    'ml': (load(os.path.join(ROOT, 'app/src/main/res/values-ml/strings.xml')), (0x0D00, 0x0D7F)),
}

PLACEHOLDER = re.compile(r'%\d*\$?[a-z]')
def phset(s): return sorted(PLACEHOLDER.findall(s or ''))
def numset(s): return sorted(re.findall(r'\d+', s or ''))
def has_script(s, lo, hi):
    return any(lo <= ord(c) <= hi for c in (s or ''))

results = {}
for lang, (d, (lo, hi)) in langs.items():
    issues = []
    checked = 0
    for k in keys:
        dv = de.get(k)
        tv = d.get(k)
        if dv is None:
            # legal key not a plain <string> in DE (could be plural/array/alias) -> skip silently
            continue
        checked += 1
        if tv is None:
            issues.append({'key': k, 'sev': 'high', 'issue': 'MISSING in target'})
            continue
        # placeholder mismatch
        if phset(dv) != phset(tv):
            issues.append({'key': k, 'sev': 'high', 'issue': f'placeholder mismatch de={phset(dv)} {lang}={phset(tv)}'})
        # number mismatch (only flag when DE has numbers)
        if numset(dv) and numset(dv) != numset(tv):
            issues.append({'key': k, 'sev': 'medium', 'issue': f'number mismatch de={numset(dv)} {lang}={numset(tv)}',
                           'deSample': dv[:90], 'tgtSample': tv[:90]})
        # identical to DE (only suspicious if DE has script letters i.e. real prose, not pure token/number)
        de_has_letters = any(c.isalpha() for c in dv)
        if tv == dv and de_has_letters and not k.startswith(('json_', 'json_value', 'json_key')):
            issues.append({'key': k, 'sev': 'low', 'issue': 'identical to DE (untranslated?)', 'sample': dv[:60]})
        # no target-script at all but DE has prose (>3 letters) -> possibly untranslated/transliterated
        elif tv and not has_script(tv, lo, hi):
            letters = sum(1 for c in tv if c.isalpha())
            if letters > 6 and de_has_letters:
                issues.append({'key': k, 'sev': 'low', 'issue': f'no {lang}-script ({letters} latin letters) - transliteration or brand-only', 'sample': tv[:60]})
    results[lang] = {'legalKeysCheckedAsString': checked, 'issues': issues}

# print summary
for lang, r in results.items():
    print(f"\n### {lang}: {r['legalKeysCheckedAsString']} legal keys checked as <string> ###")
    if not r['issues']:
        print("  NO ISSUES")
    for it in r['issues']:
        extra = ''
        if 'deSample' in it: extra = f"\n        DE:  {it['deSample']!r}\n        TGT: {it['tgtSample']!r}"
        elif 'sample' in it: extra = f"  sample={it['sample']!r}"
        print(f"  [{it['sev']}] {it['key']}: {it['issue']}{extra}")

with open(os.path.join(SHIELD, 'l3-legal-keys-result.json'), 'w', encoding='utf-8') as f:
    json.dump(results, f, ensure_ascii=False, indent=1)
print("\nWROTE l3-legal-keys-result.json")
print("Note: %d/%d legal keys exist as plain <string> in DE (rest are plurals/arrays/aliases)." % (
    results['gu']['legalKeysCheckedAsString'], len(set(keys))))
