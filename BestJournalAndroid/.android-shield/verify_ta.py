# -*- coding: utf-8 -*-
"""FIN-051c post-verification for ta. Checks: all keys present, none identical to DE,
XML valid, format-args identical to DE, no bare %, no Tamil-script digits, plurals
have one+other and are not German-stale."""
import os, re, json
import xml.etree.ElementTree as ET
os.chdir(os.path.expanduser('~/proggs/BestJournalAndroid'))
TARGET = 'app/src/main/res/values-ta/strings.xml'
JOB = '.android-shield/translation-jobs/job-plan.json'
DEREF = '.android-shield/translation-jobs/de-reference.json'

plan = json.load(open(JOB, encoding='utf-8'))['languages']['ta']
de = json.load(open(DEREF, encoding='utf-8'))
des = de['strings']; dep = de['plurals']
str_keys = plan['strings']; pl_keys = plan['plurals']

problems = []

# 1) XML valid
try:
    tree = ET.parse(TARGET); root = tree.getroot()
except ET.ParseError as e:
    print('FATAL XML PARSE ERROR:', e); raise SystemExit(2)

ta_strings = {}; ta_plurals = {}
for el in root:
    if el.tag == 'string':
        ta_strings[el.get('name')] = ''.join(el.itertext())
    elif el.tag == 'plurals':
        d = {}
        for it in el:
            d[it.get('quantity')] = ''.join(it.itertext())
        ta_plurals[el.get('name')] = d

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')

def fmt_args(s):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', s))

# 2) strings: present, not identical to DE, format-args match, no bare %, no Tamil digits
for k in str_keys:
    if k not in ta_strings:
        problems.append('MISSING string: ' + k); continue
    v = ta_strings[k]
    deval = des.get(k, '')
    # XML itertext gives \n as the 2-char backslash-n? No: file stores literal \n,
    # ElementTree returns it literally as backslash+n. DE source also has \\n -> \n. OK.
    if v.strip() == deval.strip():
        problems.append('IDENTICAL-TO-DE: ' + k)
    # format args (compare on \n-normalized): DE uses \\n in json => \n literal; both consistent
    if fmt_args(v) != fmt_args(deval):
        problems.append('FMT-ARG-MISMATCH %s : ta=%s de=%s' % (k, fmt_args(v), fmt_args(deval)))
    # bare %
    unguarded = fmt.sub('', v).count('%')
    if unguarded != 0:
        problems.append('BARE-PERCENT in ' + k)
    # Tamil-script digits U+0BE6-U+0BEF
    if re.search('[௦-௯]', v):
        problems.append('TAMIL-DIGITS in ' + k)

# 3) plurals
for k in pl_keys:
    if k not in ta_plurals:
        problems.append('MISSING plural: ' + k); continue
    forms = ta_plurals[k]
    for q in ('one', 'other'):
        if q not in forms:
            problems.append('PLURAL %s missing quantity %s' % (k, q))
    # not German-stale
    for q, val in forms.items():
        if 'vor' in val and ('Monat' in val or 'Jahr' in val):
            problems.append('PLURAL %s/%s still German: %s' % (k, q, val))
        # format arg present
        deval = dep.get(k, {}).get(q, '')
        if fmt_args(val) != fmt_args(deval):
            problems.append('PLURAL FMT mismatch %s/%s ta=%s de=%s' % (k, q, fmt_args(val), fmt_args(deval)))
        if fmt.sub('', val).count('%') != 0:
            problems.append('PLURAL BARE-PERCENT %s/%s' % (k, q))

# 4) no nested <string> (corruption guard)
with open(TARGET, encoding='utf-8') as f:
    raw = f.read()
if re.search(r'<string[^>]*>[^<]*<string', raw):
    problems.append('NESTED <string> tags')

print('STRINGS checked:', len([k for k in str_keys if k in ta_strings]), '/', len(str_keys))
print('PLURALS checked:', len([k for k in pl_keys if k in ta_plurals]), '/', len(pl_keys))
if problems:
    print('PROBLEMS:', len(problems))
    for p in problems:
        print(' -', p)
    raise SystemExit(1)
print('ALL CHECKS PASSED')
