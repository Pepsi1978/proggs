# -*- coding: utf-8 -*-
import json, os
import xml.etree.ElementTree as ET
TARGET = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-pt-rBR/strings.xml')
PLAN = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/job-plan.json')
with open(PLAN, encoding='utf-8') as f:
    plan = json.load(f)
mykeys = set(plan['languages']['pt-rBR']['strings'])
tree = ET.parse(TARGET); root = tree.getroot()
hits_mine = []; hits_other = []
for s in root.findall('string'):
    if 'definições' in (s.text or '').lower():
        (hits_mine if s.get('name') in mykeys else hits_other).append(s.get('name'))
print('definicoes in MY keys:', hits_mine if hits_mine else 'NONE (good)')
print('definicoes in OTHER pre-existing keys (NOT my job):', hits_other)
print('--- my keys using configuracoes (BR-correct) ---')
for s in root.findall('string'):
    if s.get('name') in mykeys and 'configuraç' in (s.text or '').lower():
        print('  ', s.get('name'))
