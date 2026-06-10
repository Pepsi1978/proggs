# -*- coding: utf-8 -*-
import json, os
import xml.etree.ElementTree as ET

base = os.path.expanduser('~/proggs/BestJournalAndroid')
res = os.path.join(base, 'app/src/main/res')

def parse_strings(path):
    if not os.path.exists(path):
        return {}
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

# find all keys matching onboarding_feature_secure* and profile_insight*
print('=== onboarding_feature_secure* in DE ===')
for k in sorted(de):
    if k.startswith('onboarding_feature_secure'):
        print(f'  {k} = {de[k][:120]}')
print('=== profile_insight* in DE ===')
for k in sorted(de):
    if k.startswith('profile_insight'):
        print(f'  {k} = {de[k][:120]}')

# Also check the targets for these
for lang in ['ar','ur','tr']:
    loc = parse_strings(os.path.join(res, f'values-{lang}', 'strings.xml'))
    print(f'=== {lang}: onboarding_feature_secure* ===')
    for k in sorted(loc):
        if k.startswith('onboarding_feature_secure'):
            print(f'  {k} = {loc[k][:120]}')
