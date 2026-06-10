# -*- coding: utf-8 -*-
"""L4: Pruefe ob consent_toggle_do_not_sell_body in ANDEREN Sprachen auch gekuerzt ist
   (systemisch?) oder nur in ta/te/th. Vergleicht Laenge gegen DE."""
import os
import xml.etree.ElementTree as ET
app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
key = 'consent_toggle_do_not_sell_body'
langs = ['', 'en','es','fr','it','hi','ar','ja','ko','tr','uk','nl','pl','ta','te','th','ml','kn','mr','bn','gu','ur']
def get1(sub, key):
    path = os.path.join(app, sub, 'strings.xml')
    if not os.path.exists(path): return None
    for el in ET.parse(path).getroot():
        if el.tag=='string' and el.get('name')==key:
            return ''.join(el.itertext())
    return '__ABSENT__'
de = get1('values', key)
print(f"DE len={len(de)}")
print(f"DE mentions 'Kalifornien'/'außerhalb': {'Kalifornien' in de}, {'außerhalb' in de}\n")
for l in langs:
    sub = 'values' if l=='' else f'values-{l}'
    v = get1(sub, key)
    if v is None: continue
    name = l if l else 'de'
    has2026 = '2026' in v
    # grobe Heuristik: hat es 2 Absaetze (\n\n im string)?
    paras = v.count('\n\n')
    print(f"  {name:7} len={len(v):4}  2026={has2026}  doubleNewline={paras}")
