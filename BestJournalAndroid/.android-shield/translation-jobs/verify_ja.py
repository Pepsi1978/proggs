# -*- coding: utf-8 -*-
"""finale post-verification for ja (FIN-051c + FIN-049).
Checks: (1) XML valid via ElementTree, (2) all 34 strings + 2 plurals present,
(3) none identical to DE source, (4) format-args identical to DE, (5) no naked %."""
import re, os, json
import xml.etree.ElementTree as ET

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
F = os.path.join(APP, 'app/src/main/res/values-ja/strings.xml')
JOBS = os.path.join(APP, '.android-shield/translation-jobs')

with open(os.path.join(JOBS, 'job-plan.json'), 'r', encoding='utf-8') as fh:
    ja = json.load(fh)['languages']['ja']
with open(os.path.join(JOBS, 'de-reference.json'), 'r', encoding='utf-8') as fh:
    de = json.load(fh)

target_strings = ja['strings']
target_plurals = ja['plurals']

problems = []

# (1) XML valid
try:
    tree = ET.parse(F)
    root = tree.getroot()
    xml_ok = True
except Exception as e:
    xml_ok = False
    problems.append('XML_PARSE_ERROR: %s' % e)
    root = None

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
arg = re.compile(r'%\d+\$[sd]|%[sd]')

ja_strings = {}
ja_plurals = {}
if root is not None:
    for el in root.findall('string'):
        ja_strings[el.get('name')] = (el.text or '')
    for pl in root.findall('plurals'):
        items = {it.get('quantity'): (it.text or '') for it in pl.findall('item')}
        ja_plurals[pl.get('name')] = items

# (2) presence + (3) not identical to DE + (4) format args + (5) naked %
missing = []
identical = []
fmt_mismatch = []
naked_pct = []

def argset(s):
    return sorted(arg.findall(s or ''))

for k in target_strings:
    if k not in ja_strings:
        missing.append(k); continue
    jv = ja_strings[k]
    dv = (de['strings'].get(k) or '').replace('\\n', '\n')  # de-ref stores literal \n; normalize for compare
    jv_norm = jv  # ElementTree already turned literal \n into chars? No: XML body has literal backslash-n -> stays as 2 chars '\','n'
    # For identical check compare raw bodies (both still contain literal \n sequences)
    if jv.strip() == (de['strings'].get(k) or '').strip():
        identical.append(k)
    # format args: compare against DE source (which has the placeholders)
    if argset(jv) != argset(de['strings'].get(k) or ''):
        fmt_mismatch.append({'key': k, 'ja': argset(jv), 'de': argset(de['strings'].get(k) or '')})
    # naked %
    if fmt.sub('', jv).count('%') != 0:
        naked_pct.append(k)

for k in target_plurals:
    if k not in ja_plurals:
        missing.append('PLURAL:' + k); continue
    for q, t in ja_plurals[k].items():
        if fmt.sub('', t).count('%') != 0:
            naked_pct.append('PLURAL:%s/%s' % (k, q))
        # plural format arg should match DE 'other'
        de_other = (de['plurals'].get(k, {}) or {}).get('other', '')
        if argset(t) != argset(de_other):
            fmt_mismatch.append({'key': 'PLURAL:%s/%s' % (k, q), 'ja': argset(t), 'de': argset(de_other)})

if missing: problems.append({'missing': missing})
if identical: problems.append({'identical_to_de': identical})
if fmt_mismatch: problems.append({'fmt_mismatch': fmt_mismatch})
if naked_pct: problems.append({'naked_percent': naked_pct})

result = {
    'xml_valid': xml_ok,
    'strings_present': len([k for k in target_strings if k in ja_strings]),
    'strings_expected': len(target_strings),
    'plurals_present': len([k for k in target_plurals if k in ja_plurals]),
    'plurals_expected': len(target_plurals),
    'paywall_monthly_note_present': 'paywall_monthly_note' in ja_strings,
    'all_other_plurals_only': all(set(ja_plurals.get(k, {}).keys()) == {'other'} for k in target_plurals),
    'problems': problems,
    'PASS': xml_ok and not missing and not identical and not fmt_mismatch and not naked_pct,
}
print(json.dumps(result, ensure_ascii=False, indent=1))
