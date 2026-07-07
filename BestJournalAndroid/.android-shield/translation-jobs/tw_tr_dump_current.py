# -*- coding: utf-8 -*-
import os, re, json
app = os.path.expanduser('~/proggs/BestJournalAndroid')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
tr_file = os.path.join(app, 'app/src/main/res/values-tr/strings.xml')
jp = json.load(open(os.path.join(base,'job-plan.json'), encoding='utf-8'))
strings = jp['languages']['tr']['strings']
with open(tr_file, encoding='utf-8') as f:
    content = f.read()
# Dump short ones only (skip the giant ai_prompt ones to save context)
short = [k for k in strings if not k.startswith('ai_prompt')]
for k in short:
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(k), content, re.DOTALL)
    val = m.group(1) if m else '<<MISSING>>'
    # truncate
    if len(val) > 400: val = val[:400]+'...[TRUNC]'
    print(f"[{k}]\n{val!r}\n")
