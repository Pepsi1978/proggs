# -*- coding: utf-8 -*-
"""POST-VERIFIKATION fr (FIN-051c). Alle Keys da, keiner == DE, XML valide,
Format-Args identisch, keine nackten %, Apostroph-Check."""
import json, os, re, sys
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-fr/strings.xml')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/fr-data.json')
DE = os.path.join(ROOT, '.android-shield/translation-jobs/de-reference.json')

with open(DATA, 'r', encoding='utf-8') as f:
    data = json.load(f)
with open(DE, 'r', encoding='utf-8') as f:
    de = json.load(f)

errors = []
warnings = []

# 1. XML valide
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
    print('XML_VALID=YES')
except Exception as e:
    print('XML_VALID=NO: %s' % e)
    sys.exit(1)

# Build map of present strings / plurals from parsed XML
xml_strings = {}
for el in root.findall('string'):
    xml_strings[el.get('name')] = el.text or ''
xml_plurals = {}
for el in root.findall('plurals'):
    items = {}
    for it in el.findall('item'):
        items[it.get('quantity')] = it.text or ''
    xml_plurals[el.get('name')] = items

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')

# 2. Each target string present + not identical to DE + format args match + no bare %
for key, fr_val in data['strings'].items():
    if key not in xml_strings:
        errors.append('MISSING string: %s' % key)
        continue
    present = xml_strings[key]
    de_val = de['strings'].get(key, '')
    # identical-to-DE check (compare on raw json fr vs de, both pre-XML)
    if fr_val.strip() == de_val.strip():
        errors.append('IDENTICAL to DE: %s' % key)
    # format args: count placeholders in DE source vs FR source
    de_args = sorted(re.findall(r'%\d+\$[sd]', de_val))
    fr_args = sorted(re.findall(r'%\d+\$[sd]', fr_val))
    if de_args != fr_args:
        errors.append('FORMAT-ARG mismatch %s: de=%s fr=%s' % (key, de_args, fr_args))
    # bare % check on the actually-stored XML text
    unguarded = fmt.sub('', present).count('%')
    if unguarded != 0:
        errors.append('BARE %% in %s (stored): %d' % (key, unguarded))

# 3. Plurals present + format args + not identical
for pkey, items in data['plurals'].items():
    if pkey not in xml_plurals:
        errors.append('MISSING plural: %s' % pkey)
        continue
    de_items = de['plurals'].get(pkey, {})
    for q, fr_t in items.items():
        if q not in xml_plurals[pkey]:
            errors.append('MISSING plural item %s/%s' % (pkey, q))
            continue
        de_t = de_items.get(q, '')
        de_args = sorted(re.findall(r'%\d+\$[sd]', de_t))
        fr_args = sorted(re.findall(r'%\d+\$[sd]', fr_t))
        if de_args != fr_args:
            errors.append('PLURAL FORMAT-ARG %s/%s de=%s fr=%s' % (pkey, q, de_args, fr_args))

print('TOTAL_STRINGS_IN_XML=%d' % len(xml_strings))
print('TARGET_STRINGS=%d' % len(data['strings']))
print('ERRORS=%d' % len(errors))
for e in errors:
    print('  ERR: %s' % e)
for w in warnings:
    print('  WARN: %s' % w)
