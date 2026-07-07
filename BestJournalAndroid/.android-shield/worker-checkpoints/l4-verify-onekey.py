# -*- coding: utf-8 -*-
"""L4: Read-only Verifikation eines einzelnen Keys DE vs ta (profile_style_custom)."""
import os
import xml.etree.ElementTree as ET
app = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
def get1(sub, key):
    for el in ET.parse(os.path.join(app, sub, 'strings.xml')).getroot():
        if el.tag == 'string' and el.get('name') == key:
            return ''.join(el.itertext())
    return None
for lang in ['', 'ta', 'te', 'th']:
    sub = 'values' if lang == '' else 'values-' + lang
    v = get1(sub, 'profile_style_custom')
    print('--' + (lang or 'de') + '-- len=' + str(len(v)))
    print(repr(v[:400]))
    print()
