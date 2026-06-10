# -*- coding: utf-8 -*-
"""L4: Cross-check settings_delete_account_subtitle + settings_revoke_subtitle
   ueber alle Sprachen: systemisch oder ta/te/th-spezifisch?
   Heuristik: enthaelt der subtitle das Wort 'Google' (Konto-Loeschung-Widerspruch)
   bzw. eine §356a/Widerruf-Spur?"""
import os
import xml.etree.ElementTree as ET
app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
langs = ['', 'en','es','fr','it','hi','ar','ja','ko','tr','uk','nl','pl','ta','te','th','ml','kn','mr','bn','gu','ur']
def get1(sub, key):
    path = os.path.join(app, sub, 'strings.xml')
    if not os.path.exists(path): return None
    for el in ET.parse(path).getroot():
        if el.tag=='string' and el.get('name')==key:
            return ''.join(el.itertext())
    return '__ABSENT__'

for key in ['settings_delete_account_subtitle', 'settings_revoke_subtitle']:
    print(f"\n===== {key} =====")
    for l in langs:
        sub = 'values' if l=='' else f'values-{l}'
        v = get1(sub, key)
        if v is None: continue
        name = l if l else 'de'
        print(f"  {name:7} | {v[:90]}")
