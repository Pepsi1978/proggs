# -*- coding: utf-8 -*-
import json, os
p = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/ptpt-data.json')
raw = open(p, encoding='utf-8').read()
# On disk: backslash backslash doublequote -> want backslash backslash backslash doublequote
bad = chr(92) + chr(92) + chr(34)        # \ \ "
good = chr(92) + chr(92) + chr(92) + chr(34)  # \ \ \ "
n = raw.count(bad)
fixed = raw.replace(bad, good)
open(p, 'w', encoding='utf-8', newline='\n').write(fixed)
print('Replaced occurrences:', n)
d = json.load(open(p, encoding='utf-8'))
print('JSON OK strings=%d plurals=%d' % (len(d['strings']), len(d['plurals'])))
v = d['strings']['ai_prompt_custom_intro']
print('value contains backslash-quote?', (chr(92) + chr(34)) in v)
# also confirm the German-quote unicode escapes survived into a value
rv = d['strings']['settings_revoke_confirm_body']
print('revoke body starts:', repr(rv[:40]))
