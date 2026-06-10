# -*- coding: utf-8 -*-
"""L4: Debugge warum profile_style_custom nicht als Format-Arg-Abweichung erkannt wurde."""
import os, re
import xml.etree.ElementTree as ET
app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
def get1(sub, key):
    for el in ET.parse(os.path.join(app, sub, 'strings.xml')).getroot():
        if el.tag == 'string' and el.get('name') == key:
            return ''.join(el.itertext())
    return None

ARG = re.compile(r'%(\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdfeEgGxXoc]')
def args(t):
    t2 = t.replace('%%', '')
    return sorted([m.group(0) for m in ARG.finditer(t2)])

for key in ['profile_style_custom']:
    de = get1('values', key)
    ta = get1('values-ta', key)
    te = get1('values-te', key)
    print(key)
    print('  DE raw %-count:', de.count('%'))
    print('  DE args:', args(de))
    print('  ta raw %-count:', ta.count('%'), ' ta args:', args(ta))
    print('  te args:', args(te))
    # Zeige alle %-Stellen in ta
    print('  ta %-Kontext:', re.findall(r'%.{0,4}', ta))
    print('  de %-Kontext:', re.findall(r'%.{0,4}', de))
