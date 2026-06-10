# -*- coding: utf-8 -*-
import os, re, xml.etree.ElementTree as ET
os.environ['PYTHONIOENCODING'] = 'utf-8'
base = os.path.expanduser('~/proggs/BestJournalAndroid')
RES = os.path.join(base, 'app/src/main/res')

def load(p):
    r = ET.parse(p).getroot(); o = {}
    for el in r:
        if el.tag == 'string':
            o[el.get('name')] = (el.text or '')
    return o

de = load(os.path.join(RES, 'values/strings.xml'))
mr = load(os.path.join(RES, 'values-mr/strings.xml'))
bn = load(os.path.join(RES, 'values-bn/strings.xml'))
hi = load(os.path.join(RES, 'values-hi/strings.xml'))

keys = sorted([k for k in de if k.startswith('json_key_') or k.startswith('json_value_')])
for k in keys:
    print(k)
    print('   DE: %-28s bn: %-22s hi: %-22s mr: %s' % (de.get(k), bn.get(k), hi.get(k), mr.get(k)))
