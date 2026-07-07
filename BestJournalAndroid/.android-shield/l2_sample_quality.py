# -*- coding: utf-8 -*-
import os, re, json, random, xml.etree.ElementTree as ET
os.environ['PYTHONIOENCODING'] = 'utf-8'
base = os.path.expanduser('~/proggs/BestJournalAndroid')
RES = os.path.join(base, 'app/src/main/res')
shield = os.path.join(base, '.android-shield')

legal = json.load(open(os.path.join(shield, 'legal-keys-2026-06-10.json'), encoding='utf-8'))['legalCriticalKeys']
legal_norm = set()
for k in legal:
    for p in k.split('/'):
        legal_norm.add(p.strip())

def load(p):
    r = ET.parse(p).getroot(); o = {}
    for el in r:
        if el.tag == 'string':
            o[el.get('name')] = (el.text or '')
    return o

de = load(os.path.join(RES, 'values/strings.xml'))
langs = {L: load(os.path.join(RES, 'values-%s/strings.xml' % L)) for L in ['bn', 'hi', 'mr']}

# Non-legal keys present in DE, exclude json_key/value, ai_prompt, dev_seed
def is_eligible(k):
    if k in legal_norm: return False
    if k.startswith('json_key_') or k.startswith('json_value_'): return False
    if k.startswith('ai_prompt_'): return False
    if k.startswith('dev_seed'): return False
    v = de.get(k, '')
    if len(v) < 12: return False  # want substantive strings
    return True

random.seed(42)
pool = sorted([k for k in de if is_eligible(k)])
sample = random.sample(pool, 15)
for L in ['bn', 'hi', 'mr']:
    print('=' * 60)
    print('SAMPLE', L)
    for k in sample:
        print(' KEY', k)
        print('   DE:', de.get(k)[:110])
        print('   %s:' % L, (langs[L].get(k) or '<<MISSING>>')[:110])
