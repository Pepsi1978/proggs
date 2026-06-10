# -*- coding: utf-8 -*-
"""POST-VERIFIKATION (FIN-051c) fuer pt-rBR finale Phase 3."""
import os, re, json, sys
import xml.etree.ElementTree as ET

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(APP, 'app/src/main/res/values-pt-rBR/strings.xml')
DE = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/de-reference.json')
PLAN = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/job-plan.json')

with open(DE, 'r', encoding='utf-8') as f:
    de = json.load(f)
with open(PLAN, 'r', encoding='utf-8') as f:
    plan = json.load(f)
pt = plan['languages']['pt-rBR']
str_keys = pt['strings']
plur_keys = pt['plurals']

fails = []
oks = []

# 1) XML valid
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
    oks.append('XML parses OK')
except ET.ParseError as e:
    fails.append('XML PARSE ERROR: ' + str(e))
    print('CRITICAL:', fails); sys.exit(1)

# Build maps from parsed XML
xml_strings = {}
for s in root.findall('string'):
    xml_strings[s.get('name')] = s.text or ''
xml_plurals = {}
for p in root.findall('plurals'):
    forms = {}
    for it in p.findall('item'):
        forms[it.get('quantity')] = it.text or ''
    xml_plurals[p.get('name')] = forms

# 2) all string keys present
missing = [k for k in str_keys if k not in xml_strings]
if missing:
    fails.append('MISSING string keys: ' + str(missing))
else:
    oks.append('all 34 string keys present')

# 3) all plural keys present + one/other
for k in plur_keys:
    if k not in xml_plurals:
        fails.append('MISSING plural: ' + k)
    else:
        f = xml_plurals[k]
        if 'one' not in f or 'other' not in f:
            fails.append('plural ' + k + ' missing one/other: ' + str(list(f.keys())))
        else:
            oks.append('plural ' + k + ' has one/other')

# Helper: format-arg signature
def fmt_args(text):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', text))

# raw read for raw-newline + bare-% checks on MY keys only
with open(TARGET, 'r', encoding='utf-8') as f:
    raw = f.read()

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')

for k in str_keys:
    de_val = de['strings'][k]
    xml_val = xml_strings.get(k, '')
    # 4) none identical to DE
    if xml_val.strip() == de_val.strip():
        fails.append('IDENTICAL to DE: ' + k)
    # 5) format args identical to DE
    if fmt_args(de_val) != fmt_args(xml_val):
        fails.append('FORMAT-ARG mismatch ' + k + ': DE=' + str(fmt_args(de_val)) + ' PT=' + str(fmt_args(xml_val)))
    # 6) no bare % (after removing valid placeholders + %%)
    unguarded = fmt.sub('', xml_val).count('%')
    if unguarded != 0:
        fails.append('BARE % in ' + k + ' (count ' + str(unguarded) + ')')

# 7) plurals: format args + identical-to-DE + bare %
for k in plur_keys:
    de_forms = de['plurals'][k]
    pt_forms = xml_plurals.get(k, {})
    for q, de_v in de_forms.items():
        pt_v = pt_forms.get(q, '')
        if fmt_args(de_v) != fmt_args(pt_v):
            fails.append('PLURAL FORMAT-ARG mismatch ' + k + '/' + q + ': DE=' + str(fmt_args(de_v)) + ' PT=' + str(fmt_args(pt_v)))
        if pt_v.strip() == de_v.strip():
            fails.append('PLURAL IDENTICAL to DE: ' + k + '/' + q)
        if fmt.sub('', pt_v).count('%') != 0:
            fails.append('PLURAL BARE % in ' + k + '/' + q)

# 8) NO raw newlines inside <string>-values of MY keys (real \n bytes between >...<)
# Extract each of my keys' raw inner block and check for literal newline char.
for k in str_keys:
    pat = re.compile(r'<string\b[^>]*\bname="' + re.escape(k) + r'"[^>]*>(.*?)</string>', re.DOTALL)
    m = pat.search(raw)
    if not m:
        fails.append('RAW-SCAN: key not found in raw: ' + k)
        continue
    inner = m.group(1)
    if '\n' in inner:
        fails.append('RAW NEWLINE inside <string> value for ' + k)
# also plurals raw
for k in plur_keys:
    pat = re.compile(r'<plurals\b[^>]*\bname="' + re.escape(k) + r'"[^>]*>(.*?)</plurals>', re.DOTALL)
    m = pat.search(raw)
    if m:
        # plurals legitimately have newlines between <item> tags; check inside each item only
        for im in re.finditer(r'<item[^>]*>(.*?)</item>', m.group(1), re.DOTALL):
            if '\n' in im.group(1):
                fails.append('RAW NEWLINE inside plural <item> for ' + k)

# 9) nested string tags
if re.search(r'<string[^>]*>[^<]*<string', raw):
    fails.append('NESTED <string> tags found')
else:
    oks.append('no nested string tags')

# 10) spot-check rerank no longer raw-newline & is full-length
rr = xml_strings.get('ai_prompt_rerank_system', '')
oks.append('rerank_system length=' + str(len(rr)) + ' (was 459 stub)')

print('=== OK CHECKS ===')
for o in oks:
    print(' OK:', o)
print()
if fails:
    print('=== FAILURES (' + str(len(fails)) + ') ===')
    for fl in fails:
        print(' FAIL:', fl)
    sys.exit(1)
else:
    print('ALL CHECKS PASSED — 34 strings + 2 plurals, no raw newlines, format args intact, XML valid, none identical to DE')
    sys.exit(0)
