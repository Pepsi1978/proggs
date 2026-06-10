# -*- coding: utf-8 -*-
import json, os
import xml.etree.ElementTree as ET

base = os.path.expanduser('~/proggs/BestJournalAndroid')
res = os.path.join(base, 'app/src/main/res')
shield = os.path.join(base, '.android-shield')

with open(os.path.join(shield, '.l1-work', 'my-matrix-data.json'), 'r', encoding='utf-8') as f:
    mydata = json.load(f)

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

for lang in ['ar', 'ur', 'tr']:
    loc = parse_strings(os.path.join(res, f'values-{lang}', 'strings.xml'))
    idlist = mydata[lang]['identicalToDe']
    print(f'=== {lang}: {len(idlist)} identicalToDe ===')
    for k in idlist:
        v = loc.get(k, '<<MISSING>>')
        print(f'  [{k}] = {v[:90]}')
    print()
