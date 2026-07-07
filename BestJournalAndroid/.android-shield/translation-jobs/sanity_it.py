# -*- coding: utf-8 -*-
import re, json
TF = r'C:\Users\barwa\proggs\BestJournalAndroid\app\src\main\res\values-it\strings.xml'
json.load(open(r'C:\Users\barwa\proggs\BestJournalAndroid\.android-shield\translation-jobs\worker-outputs\phase3-tw-it.json', encoding='utf-8'))
print('OUTPUT-JSON-VALID')
raw = open(TF, encoding='utf-8').read()
print('paywall_monthly_note count:', len(re.findall(r'name="paywall_monthly_note"', raw)))
print('mese fa present:', 'mese fa' in raw)
print('anno fa present:', 'anno fa' in raw)
