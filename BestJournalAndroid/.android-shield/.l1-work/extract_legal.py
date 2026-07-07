# -*- coding: utf-8 -*-
import json, os, re
import xml.etree.ElementTree as ET

base = os.path.expanduser('~/proggs/BestJournalAndroid')
res = os.path.join(base, 'app/src/main/res')
shield = os.path.join(base, '.android-shield')

LEGAL_PATH = os.path.join(shield, 'legal-keys-2026-06-10.json')
with open(LEGAL_PATH, 'r', encoding='utf-8') as f:
    legal_raw = json.load(f)['legalCriticalKeys']

# Clean the legal keys: some entries contain notes with " / " or wildcards.
# Expand into a set of concrete candidate keys; keep wildcards separately.
concrete = set()
wildcards = []  # patterns with *
for entry in legal_raw:
    # split on " / "
    parts = [p.strip() for p in entry.split('/')]
    for p in parts:
        if '*' in p:
            wildcards.append(p)
        elif p:
            concrete.add(p)

def parse_strings(path):
    """Return dict key->text for <string> elements (raw inner text incl. xliff tags)."""
    if not os.path.exists(path):
        return {}
    out = {}
    tree = ET.parse(path)
    root = tree.getroot()
    for el in root:
        if el.tag != 'string':
            continue
        name = el.get('name')
        if name is None:
            continue
        # Reconstruct full inner content including child tags (xliff:g etc.)
        inner = el.text or ''
        for child in el:
            # tag without namespace prefix expansion -> use raw-ish
            tag = child.tag
            # ElementTree expands ns to {uri}local; collapse to local for readability
            tag_local = tag.split('}')[-1]
            attrs = ''.join(f' {k.split("}")[-1]}="{v}"' for k, v in child.attrib.items())
            ctext = child.text or ''
            inner += f'<{tag_local}{attrs}>{ctext}</{tag_local}>'
            if child.tail:
                inner += child.tail
        out[name] = inner
    return out

de = parse_strings(os.path.join(res, 'values', 'strings.xml'))

result = {'concreteKeys': sorted(concrete), 'wildcards': wildcards, 'langs': {}}

for lang in ['ar', 'ur', 'tr']:
    loc = parse_strings(os.path.join(res, f'values-{lang}', 'strings.xml'))
    pairs = {}
    for k in sorted(concrete):
        de_v = de.get(k)
        loc_v = loc.get(k)
        if de_v is None:
            # key in legal list but not in DE -> note
            pairs[k] = {'de': None, 'loc': loc_v, 'note': 'NOT_IN_DE'}
        else:
            pairs[k] = {'de': de_v, 'loc': loc_v}
    result['langs'][lang] = pairs

outpath = os.path.join(shield, '.l1-work', 'legal-pairs.json')
with open(outpath, 'w', encoding='utf-8') as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

# Summary
print('Concrete legal keys:', len(concrete))
print('Wildcards:', wildcards)
print('Keys in legal list but NOT in DE strings.xml:')
for k in sorted(concrete):
    if de.get(k) is None:
        print('  -', k)
print()
# Per-lang: how many legal keys missing in target
for lang in ['ar', 'ur', 'tr']:
    loc = parse_strings(os.path.join(res, f'values-{lang}', 'strings.xml'))
    miss = [k for k in concrete if de.get(k) is not None and loc.get(k) is None]
    print(f'{lang}: legal keys present in DE but MISSING in target: {len(miss)} -> {miss}')
print(f'\nWritten to {outpath}')
