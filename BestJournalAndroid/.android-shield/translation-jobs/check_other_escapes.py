# -*- coding: utf-8 -*-
"""Prueft ob \\u00A0 / \\u202F Escape-Strings im Ziel-XML in MEINEN 34 Keys vorkommen
(duerfen NICHT) oder nur in anderen Bestands-Strings (egal, nicht mein Scope)."""
import json, os, re
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-fr/strings.xml')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/fr-data.json')

d = json.load(open(DATA, encoding='utf-8'))
mine = set(d['strings'].keys()) | set('plural:' + k for k in d['plurals'])
root = ET.parse(TARGET).getroot()

lit_a0 = re.compile(r'\\u00A0')
lit_202f = re.compile(r'\\u202F')

mine_with_literal = []
other_with_literal = 0
for el in root.findall('string'):
    n = el.get('name')
    txt = el.text or ''
    if lit_a0.search(txt) or lit_202f.search(txt):
        if n in d['strings']:
            mine_with_literal.append(n)
        else:
            other_with_literal += 1

print('MY_KEYS_with_literal_uXXXX_escapes=%s' % mine_with_literal)
print('OTHER_existing_strings_with_literal_escapes=%d (not my scope)' % other_with_literal)

# Also: do my keys carry REAL NBSP/NNBSP where expected?
for k in ['settings_revoke_confirm_title', 'settings_revoke_subtitle', 'settings_revoke_confirm_body']:
    for el in root.findall('string'):
        if el.get('name') == k:
            txt = el.text or ''
            cps = sorted(set(c for c in (0x00A0, 0x202F) if chr(c) in txt))
            print('%s real_nbsp_codepoints=%s' % (k, [hex(c) for c in cps]))
