# -*- coding: utf-8 -*-
import os
import xml.etree.ElementTree as ET
from collections import Counter

TGT = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-zh-rTW/strings.xml')
t = ET.parse(TGT)
r = t.getroot()
names = [e.get('name') for e in r.findall('string')]
pl = [e.get('name') for e in r.findall('plurals')]
dups = [k for k, c in Counter(names).items() if c > 1]
pdups = [k for k, c in Counter(pl).items() if c > 1]
print('total strings:', len(names), 'unique:', len(set(names)))
print('total plurals:', len(pl), 'unique:', len(set(pl)))
print('STRING DUPS:', dups)
print('PLURAL DUPS:', pdups)
print('XML VALID: yes')
