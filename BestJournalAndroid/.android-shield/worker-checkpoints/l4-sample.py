# -*- coding: utf-8 -*-
"""L4: 15 zufaellige (deterministisch geseedet) Strings pro Sprache, DE+Ziel,
   fuer allgemeine Qualitaets-Stichprobe. Schliesst json_key_/dev_seed_ aus."""
import json, os, random
import xml.etree.ElementTree as ET
app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')

def parse(sub):
    out = {}
    for el in ET.parse(os.path.join(app, sub, 'strings.xml')).getroot():
        if el.tag=='string' and el.get('name'):
            out[el.get('name')] = ''.join(el.itertext())
    return out

de = parse('values')
cand = [k for k in de if not k.startswith('json_key_') and not k.startswith('json_value_')
        and not k.startswith('dev_seed_') and not k.startswith('ai_prompt_')
        and len(de[k].strip()) >= 8]

for lang in ['ta','te','th']:
    tgt = parse(f'values-{lang}')
    random.seed(hash('L4-'+lang) & 0xffffffff)
    pick = random.sample(cand, 15)
    print(f"\n############### {lang} — 15 Sample ###############")
    for k in pick:
        d = de[k].replace('\n','\\n')[:90]
        t = tgt.get(k, '__MISSING__').replace('\n','\\n')[:120]
        print(f"@@ {k}\n   de: {d}\n   {lang}: {t}")
