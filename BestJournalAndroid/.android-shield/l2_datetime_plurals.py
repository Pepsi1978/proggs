# -*- coding: utf-8 -*-
import os, re, xml.etree.ElementTree as ET
os.environ['PYTHONIOENCODING'] = 'utf-8'
base = os.path.expanduser('~/proggs/BestJournalAndroid')
RES = os.path.join(base, 'app/src/main/res')

def load_plurals(p):
    r = ET.parse(p).getroot(); o = {}
    for el in r:
        if el.tag == 'plurals':
            nm = el.get('name')
            for item in el:
                o['%s[%s]' % (nm, item.get('quantity'))] = (item.text or '')
    return o

de = load_plurals(os.path.join(RES, 'values/strings.xml'))
langs = {L: load_plurals(os.path.join(RES, 'values-%s/strings.xml' % L)) for L in ['bn', 'hi', 'mr']}

# All plural keys mentioning datetime/relative
dtkeys = sorted([k for k in de if 'relative' in k or 'datetime' in k or k.split('[')[0].endswith('_ago')])
print('Datetime/relative plural keys in DE:', len(dtkeys))
GER = re.compile(r'[äöüÄÖÜß]|\bvor\b|\bMonat|\bJahr|\bTag|\bWoche|\bStunde|\bMinute')
for k in dtkeys:
    dv = de.get(k, '')
    print(k, '| DE:', dv)
    for L in ['bn', 'hi', 'mr']:
        v = langs[L].get(k, '<<MISSING>>')
        flag = ' <-- GERMAN/UNTRANSLATED' if GER.search(v) else ''
        print('     %s: %s%s' % (L, v, flag))
