# -*- coding: utf-8 -*-
"""Backup-Apostroph-Check + Spot-Check gespeicherter fr-Werte."""
import json, os, re
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-fr/strings.xml')
DATA = os.path.join(ROOT, '.android-shield/translation-jobs/fr-data.json')

d = json.load(open(DATA, encoding='utf-8'))
root = ET.parse(TARGET).getroot()

# Backup apostrophe check: unescaped ' outside double-quoted strings, in our keys
bad = []
apos = re.compile(r"(?<!\\)'")
for el in root.findall('string'):
    n = el.get('name')
    if n in d['strings']:
        txt = el.text or ''
        quoted = txt.startswith('"') and txt.endswith('"')
        if apos.findall(txt) and not quoted:
            bad.append((n, len(apos.findall(txt))))
print('backup_unescaped_apostrophe_keys=%s' % bad)

# Spot-check: show stored value for 3 representative keys
for k in ['paywall_monthly_note', 'settings_revoke_subtitle', 'settings_revoke_confirm_title', 'onboarding_premium_feature_noads']:
    for el in root.findall('string'):
        if el.get('name') == k:
            print('--- %s ---' % k)
            print(repr(el.text))
# plurals spot-check
for el in root.findall('plurals'):
    if el.get('name') in d['plurals']:
        print('--- plural %s ---' % el.get('name'))
        for it in el.findall('item'):
            print('  [%s] %s' % (it.get('quantity'), repr(it.text)))
