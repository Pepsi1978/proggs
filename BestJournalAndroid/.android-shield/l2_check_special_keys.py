# -*- coding: utf-8 -*-
import os, re, xml.etree.ElementTree as ET
os.environ['PYTHONIOENCODING'] = 'utf-8'
base = os.path.expanduser('~/proggs/BestJournalAndroid')
RES = os.path.join(base, 'app/src/main/res')

def load_all(p):
    """Return dict for string, plurals, string-array."""
    r = ET.parse(p).getroot(); o = {}
    for el in r:
        nm = el.get('name')
        if el.tag == 'string':
            o[nm] = ('string', el.text or '')
        elif el.tag == 'plurals':
            o[nm] = ('plurals', [(it.get('quantity'), it.text or '') for it in el])
        elif el.tag == 'string-array':
            o[nm] = ('array', [it.text or '' for it in el])
    return o

de = load_all(os.path.join(RES, 'values/strings.xml'))
hi = load_all(os.path.join(RES, 'values-hi/strings.xml'))
bn = load_all(os.path.join(RES, 'values-bn/strings.xml'))
mr = load_all(os.path.join(RES, 'values-mr/strings.xml'))

CHECK = ['ai_prompt_rerank_user_focus_header', 'ai_prompt_rerank_actions_header',
         'ai_prompt_rerank_entries_header', 'profile_insight_desc', 'profile_insight_long',
         'achievements_title']
for k in CHECK:
    print('KEY', k)
    print('  DE :', de.get(k))
    print('  bn :', bn.get(k))
    print('  hi :', hi.get(k))
    print('  mr :', mr.get(k))
    print()
