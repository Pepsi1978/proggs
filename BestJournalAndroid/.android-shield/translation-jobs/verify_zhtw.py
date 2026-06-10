# -*- coding: utf-8 -*-
"""POST-VERIFICATION zh-rTW (FIN-051c).
Checks: all keys present, none identical to DE, XML valid, format args identical,
no naked %, only Traditional chars (no Simplified leak), Taiwan terminology."""
import json, os, re
import xml.etree.ElementTree as ET

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
TGT = os.path.join(APP, 'app/src/main/res/values-zh-rTW/strings.xml')
BASE = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')

with open(os.path.join(BASE, 'job-plan.json'), 'r', encoding='utf-8') as f:
    job = json.load(f)['languages']['zh-rTW']
with open(os.path.join(BASE, 'de-reference.json'), 'r', encoding='utf-8') as f:
    de = json.load(f)

de_strings = de.get('strings', {})
de_plurals = de.get('plurals', {})

errors = []
warnings = []

# 1) XML valid
try:
    tree = ET.parse(TGT)
    root = tree.getroot()
except Exception as e:
    print("XML PARSE ERROR:", e)
    raise SystemExit(1)

existing = {}
for el in root.findall('string'):
    existing[el.get('name')] = (el.text or '')
existing_pl = {}
for el in root.findall('plurals'):
    existing_pl[el.get('name')] = {it.get('quantity'): (it.text or '') for it in el.findall('item')}

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
def args_of(s):
    return sorted(re.findall(r'%\d+\$[sd]', s) + re.findall(r'(?<!%)%[sd](?!\w)', s))

# Simplified-character leak detector (common simplified forms that must NOT appear)
SIMPLIFIED = set('软为应试个习题对样动单据丰汉书证华专属带价亿仅从仓众优会伤伟传')
# Whitelist of correct Traditional usages we explicitly want present
def has_simplified(s):
    return [c for c in s if c in SIMPLIFIED]

# 2) all string keys present + not identical DE + format args match + no naked %
for k in job['strings']:
    if k not in existing:
        errors.append(f"MISSING string: {k}")
        continue
    val = existing[k]
    dev = de_strings.get(k, '')
    if val.strip() == dev.strip() and dev:
        errors.append(f"IDENTICAL to DE: {k}")
    # format args
    if args_of(val) != args_of(dev):
        errors.append(f"FORMAT-ARG MISMATCH {k}: tw={args_of(val)} de={args_of(dev)}")
    # naked %
    if fmt.sub('', val).count('%') != 0:
        errors.append(f"NAKED % in {k}")
    # simplified leak
    leak = has_simplified(val)
    if leak:
        errors.append(f"SIMPLIFIED LEAK in {k}: {leak}")

# 3) plurals: present, other only, not identical DE, format args
for k in job['plurals']:
    if k not in existing_pl:
        errors.append(f"MISSING plural: {k}")
        continue
    items = existing_pl[k]
    if set(items.keys()) != {'other'}:
        errors.append(f"PLURAL {k} should be 'other' only, got {list(items.keys())}")
    other = items.get('other', '')
    de_other = de_plurals.get(k, {}).get('other', '')
    if other.strip() == de_other.strip() and de_other:
        errors.append(f"PLURAL IDENTICAL to DE: {k}")
    if args_of(other) != args_of(de_other):
        errors.append(f"PLURAL FORMAT MISMATCH {k}: tw={args_of(other)} de={args_of(de_other)}")
    leak = has_simplified(other)
    if leak:
        errors.append(f"PLURAL SIMPLIFIED LEAK {k}: {leak}")

# 4) Taiwan terminology spot-check: 軟體 not 軟件; 自動續訂 present in monthly note
mn = existing.get('paywall_monthly_note', '')
if '自動續訂' not in mn:
    warnings.append(f"paywall_monthly_note missing 自動續訂: {mn!r}")

# Legal 1:1 checks
if 'Google 帳號本身將保留' not in existing.get('settings_delete_account_confirm_body', ''):
    errors.append("delete_account_confirm_body missing 'Google 帳號本身將保留'")
if '356a' not in existing.get('settings_revoke_subtitle', ''):
    errors.append("revoke_subtitle missing §356a BGB ref")
# do_not_sell must NOT contain a year like 2026
if re.search(r'20\d\d', existing.get('consent_toggle_do_not_sell_body', '')):
    errors.append("do_not_sell_body contains a year (should not)")

# whole-file simplified scan on the 36 units only (done above). Also check whole file for 软
whole = open(TGT, encoding='utf-8').read()
if '软' in whole:
    # locate
    for k, v in existing.items():
        if '软' in v:
            errors.append(f"'软' (simplified) found in {k}")

print("=== VERIFICATION RESULT ===")
print("strings present:", sum(1 for k in job['strings'] if k in existing), "/", len(job['strings']))
print("plurals present:", sum(1 for k in job['plurals'] if k in existing_pl), "/", len(job['plurals']))
print("ERRORS:", len(errors))
for e in errors:
    print("  X", e)
print("WARNINGS:", len(warnings))
for w in warnings:
    print("  !", w)
print("RESULT:", "PASS" if not errors else "FAIL")
