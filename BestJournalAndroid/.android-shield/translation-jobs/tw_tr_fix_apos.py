# -*- coding: utf-8 -*-
import os, re, json
app = os.path.expanduser('~/proggs/BestJournalAndroid')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
tr_file = os.path.join(app, 'app/src/main/res/values-tr/strings.xml')
jp = json.load(open(os.path.join(base,'job-plan.json'), encoding='utf-8'))
strings = jp['languages']['tr']['strings']

with open(tr_file, 'r', encoding='utf-8') as f:
    content = f.read()

# Escape bare ' -> \' only within each JOB key's <string> value (don't touch others)
fixed = {}
for k in strings:
    pat = re.compile(r'(<string name="%s">)(.*?)(</string>)' % re.escape(k), re.DOTALL)
    m = pat.search(content)
    if not m:
        continue
    inner = m.group(2)
    # Escape any ' that is NOT already preceded by a backslash
    new_inner = re.sub(r"(?<!\\)'", r"\\'", inner)
    if new_inner != inner:
        cnt = inner.count("'") - inner.count("\\'")
        fixed[k] = cnt
        content = content[:m.start()] + m.group(1) + new_inner + m.group(3) + content[m.end():]

with open(tr_file, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

print("APOSTROPHES_FIXED:", fixed)
