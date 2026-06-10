# -*- coding: utf-8 -*-
"""FIN-051c post-verification for te: all keys present, none identical to DE, XML valid, format-args match, no bare %."""
import os, re, json, sys
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TF = os.path.join(ROOT, 'app/src/main/res/values-te/strings.xml')
JOB = os.path.join(ROOT, '.android-shield/translation-jobs/job-plan.json')
DEREF = os.path.join(ROOT, '.android-shield/translation-jobs/de-reference.json')

job = json.load(open(JOB, encoding='utf-8'))['languages']['te']
de = json.load(open(DEREF, encoding='utf-8'))
errors = []

# 1) XML valid
try:
    tree = ET.parse(TF)
    root = tree.getroot()
except Exception as e:
    print("XML_PARSE_FAIL:", e); sys.exit(1)

strings = {}
plurals = {}
for el in root:
    name = el.get('name')
    if el.tag == 'string':
        strings[name] = ''.join(el.itertext())
    elif el.tag == 'plurals':
        plurals[name] = {it.get('quantity'): ''.join(it.itertext()) for it in el}

FMT = re.compile(r'%\d+\$[sd]|%[sd]|%%')

def fmt_args(t):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', t))

def bare_pct(t):
    return FMT.sub('', t).count('%')

# 2) all job string keys present + not identical to DE + format-args match + no bare %
for k in job['strings']:
    if k not in strings:
        errors.append(f"MISSING string {k}"); continue
    val = strings[k]
    deval = de['strings'].get(k, '')
    # de-reference stores literal \n as backslash-n; ET reads the file's literal backslash-n too -> compare raw
    if val == deval.replace('\\n', '\\n'):  # noop, kept for clarity
        pass
    if val.strip() == deval.strip():
        errors.append(f"IDENTICAL_TO_DE string {k}")
    # format args: compare against DE (the file value has real backslash-n; placeholders unaffected)
    if fmt_args(val) != fmt_args(deval):
        errors.append(f"FMT_MISMATCH {k}: te={fmt_args(val)} de={fmt_args(deval)}")
    if bare_pct(val) != 0:
        errors.append(f"BARE_PERCENT {k}: {val[:60]}")

# 3) plurals present + not identical + format-args
for k in job['plurals']:
    if k not in plurals:
        errors.append(f"MISSING plural {k}"); continue
    for q in ('one', 'other'):
        v = plurals[k].get(q, '')
        dv = de['plurals'][k].get(q, '')
        if v.strip() == dv.strip():
            errors.append(f"IDENTICAL_TO_DE plural {k}/{q}: {v}")
        if fmt_args(v) != fmt_args(dv):
            errors.append(f"FMT_MISMATCH plural {k}/{q}: te={fmt_args(v)} de={fmt_args(dv)}")
        if bare_pct(v) != 0:
            errors.append(f"BARE_PERCENT plural {k}/{q}: {v}")

# 4) legal phrasing sanity (info only)
checks = {
 'settings_delete_account_subtitle': 'Google',  # Google-Konto bleibt -> Google ఖాతా
 'settings_revoke_subtitle': '356a',
}
for k, needle in checks.items():
    if needle not in strings.get(k, ''):
        errors.append(f"LEGAL_CHECK {k} missing '{needle}'")

print("STRINGS_FOUND", len([k for k in job['strings'] if k in strings]), "/", len(job['strings']))
print("PLURALS_FOUND", len([k for k in job['plurals'] if k in plurals]), "/", len(job['plurals']))
if errors:
    print("VERIFY_FAIL", len(errors))
    for e in errors:
        print("  -", e)
    sys.exit(1)
else:
    print("VERIFY_OK — all keys present, none identical to DE, XML valid, format-args match, no bare %")
