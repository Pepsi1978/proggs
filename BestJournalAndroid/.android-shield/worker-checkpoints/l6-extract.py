# -*- coding: utf-8 -*-
"""L6 extractor: pulls DE + en/es/fr/it values for legal keys + identicalToDe lists.
Parses strings.xml via ElementTree (NOT Read tool). Writes intermediate JSON for analysis."""
import json
import xml.etree.ElementTree as ET
import os
import re

ROOT = r'C:/Users/barwa/proggs/BestJournalAndroid/app/src/main/res'
SHIELD = r'C:/Users/barwa/proggs/BestJournalAndroid/.android-shield'

LANGS = ['en', 'es', 'fr', 'it']
LEGAL = json.load(open(SHIELD + r'/legal-keys-2026-06-10.json', 'r', encoding='utf-8'))['legalCriticalKeys']
# drop composite/illustrative entries containing ' / '
LEGAL_CLEAN = [k for k in LEGAL if ' / ' not in k]

MATRIX = json.load(open(SHIELD + r'/cross-lingual-matrix-2026-06-10.json', 'r', encoding='utf-8'))


def parse_strings(path):
    """Return dict key->text for <string> elements. Preserves raw inner text."""
    if not os.path.exists(path):
        return None
    tree = ET.parse(path)
    root = tree.getroot()
    out = {}
    for el in root.findall('string'):
        name = el.get('name')
        if name is None:
            continue
        # clean concatenation of all text nodes (incl. inside <xliff:g>/<b>) in document order.
        # itertext() yields each text/tail exactly once -> no duplication artifact.
        txt = ''.join(el.itertext())
        out[name] = txt
    return out


def vfile(lang):
    return os.path.join(ROOT, f'values-{lang}', 'strings.xml')


de = parse_strings(os.path.join(ROOT, 'values', 'strings.xml'))
langmaps = {l: parse_strings(vfile(l)) for l in LANGS}

# 1) Legal key pairs: de + each lang
legal_pairs = {}
for k in LEGAL_CLEAN:
    entry = {'de': de.get(k)}
    for l in LANGS:
        entry[l] = langmaps[l].get(k)
    legal_pairs[k] = entry

# 2) identicalToDe lists per lang, with de + target value
identical = {}
for l in LANGS:
    lst = MATRIX['locales'][l]['identicalToDe']
    identical[l] = {k: {'de': de.get(k), l: langmaps[l].get(k)} for k in lst}

# 3) ai_limits_dialog_body raw across all
numbers = {'de': de.get('ai_limits_dialog_body')}
for l in LANGS:
    numbers[l] = langmaps[l].get('ai_limits_dialog_body')

result = {
    'legal_clean_count': len(LEGAL_CLEAN),
    'legal_pairs': legal_pairs,
    'identical': identical,
    'numbers_ai_limits': numbers,
}
outp = SHIELD + r'/worker-checkpoints/l6-extracted.json'
with open(outp, 'w', encoding='utf-8') as f:
    json.dump(result, f, ensure_ascii=False, indent=1)
print('WROTE', outp)
print('legal clean keys:', len(LEGAL_CLEAN))
for l in LANGS:
    print(f'  identical {l}:', len(identical[l]))
