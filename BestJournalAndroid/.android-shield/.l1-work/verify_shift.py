# -*- coding: utf-8 -*-
import json, os
import xml.etree.ElementTree as ET

base = os.path.expanduser('~/proggs/BestJournalAndroid')
res = os.path.join(base, 'app/src/main/res')

def parse_strings(path):
    if not os.path.exists(path): return {}
    out = {}
    tree = ET.parse(path); root = tree.getroot()
    for el in root:
        if el.tag != 'string': continue
        name = el.get('name')
        if name is None: continue
        inner = el.text or ''
        for child in el:
            tag_local = child.tag.split('}')[-1]
            attrs = ''.join(f' {k.split("}")[-1]}="{v}"' for k, v in child.attrib.items())
            inner += f'<{tag_local}{attrs}>{child.text or ""}</{tag_local}>'
            if child.tail: inner += child.tail
        out[name] = inner
    return out

de = parse_strings(os.path.join(res, 'values', 'strings.xml'))

# Keys to cross-check across all 3 + their semantic neighbors
check = [
    'privacy_gate_tts_title',
    'privacy_gate_tts_body',
    'settings_revoke_confirm_title',
    'settings_revoke_confirm_body',
    'settings_revoke_subtitle',
    'settings_revoke_title',
    'settings_report_ai_no_email',
]
for lang in ['ar','ur','tr']:
    loc = parse_strings(os.path.join(res, f'values-{lang}', 'strings.xml'))
    print(f'===== {lang} =====')
    for k in check:
        print(f'[{k}]')
        print(f'  DE : {de.get(k,"<<no DE>>")[:110]}')
        print(f'  {lang.upper()}: {loc.get(k,"<<MISSING>>")[:130]}')
    print()
