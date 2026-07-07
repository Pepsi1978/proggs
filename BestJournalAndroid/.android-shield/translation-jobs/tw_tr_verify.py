# -*- coding: utf-8 -*-
import os, re, json
import xml.etree.ElementTree as ET

app = os.path.expanduser('~/proggs/BestJournalAndroid')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
tr_file = os.path.join(app, 'app/src/main/res/values-tr/strings.xml')

de = json.load(open(os.path.join(base,'de-reference.json'), encoding='utf-8'))
de_strings = de['strings']
de_plurals = de['plurals']
jp = json.load(open(os.path.join(base,'job-plan.json'), encoding='utf-8'))
tr = jp['languages']['tr']
strings = tr['strings']
plurals = tr['plurals']
legend_keys = tr['special'][0].split(': ',1)[1].split(',')

errors = []

# 1. XML valid
try:
    ET.parse(tr_file)
    xml_valid = True
except Exception as e:
    xml_valid = False
    errors.append(f"XML INVALID: {e}")

with open(tr_file, encoding='utf-8') as f:
    content = f.read()

# 2. all string keys present + not identical to DE + placeholders match
fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
def placeholders(s):
    # count of positional placeholders %1$s etc.
    return sorted(re.findall(r'%\d+\$[sd]', s))

for k in strings:
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(k), content, re.DOTALL)
    if not m:
        errors.append(f"MISSING key: {k}")
        continue
    val = m.group(1)
    de_val = de_strings.get(k, '')
    if val == de_val:
        errors.append(f"IDENTICAL to DE: {k}")
    # placeholder parity vs DE
    if placeholders(val) != placeholders(de_val):
        errors.append(f"PLACEHOLDER mismatch {k}: tr={placeholders(val)} de={placeholders(de_val)}")
    # no bare % in format-arg strings: if DE has placeholders, ensure no naked %
    if placeholders(de_val):
        unguarded = fmt.sub('', val).count('%')
        if unguarded != 0:
            errors.append(f"NAKED % in format string {k}: {val!r}")

# 3. plurals present + not identical to DE + placeholder parity
for k in plurals:
    m = re.search(r'<plurals name="%s">(.*?)</plurals>' % re.escape(k), content, re.DOTALL)
    if not m:
        errors.append(f"MISSING plural: {k}")
        continue
    block = m.group(1)
    items = re.findall(r'<item quantity="(\w+)">(.*?)</item>', block, re.DOTALL)
    qs = {q:v for q,v in items}
    if 'one' not in qs or 'other' not in qs:
        errors.append(f"PLURAL {k} missing one/other: {list(qs)}")
    de_pl = de_plurals.get(k, {})
    for q,v in qs.items():
        de_v = de_pl.get(q, '')
        if v == de_v:
            errors.append(f"PLURAL {k}/{q} IDENTICAL to DE")
        if placeholders(v) != placeholders(de_v):
            errors.append(f"PLURAL placeholder mismatch {k}/{q}: tr={placeholders(v)} de={placeholders(de_v)}")

# 4. legend keys: must show SINGLE % (no %%), and not identical to DE
for k in legend_keys:
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(k), content, re.DOTALL)
    if not m:
        errors.append(f"MISSING legend: {k}")
        continue
    val = m.group(1)
    if '%%' in val:
        errors.append(f"LEGEND still has %% : {k} :: {val!r}")
    if '%' not in val:
        errors.append(f"LEGEND lost % : {k} :: {val!r}")
    if val == de_strings.get(k, ''):
        errors.append(f"LEGEND identical to DE: {k}")

# 5. apostrophe escaping sanity: bare ' inside non-quoted strings would break build.
# Android allows ' only if escaped \' OR whole string wrapped in "..."
# Check job string + new key for unescaped apostrophes
def check_apos(val, key):
    # remove escaped \' then look for remaining bare '
    tmp = val.replace("\\'", "")
    if "'" in tmp:
        # it's only a problem if string is not wholly double-quoted; our values aren't quote-wrapped
        errors.append(f"UNESCAPED apostrophe possible in {key}: {val!r}")
for k in strings:
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(k), content, re.DOTALL)
    if m: check_apos(m.group(1), k)

translated = len(strings)
print("xml_valid:", xml_valid)
print("strings_checked:", translated)
print("legend_count:", len(legend_keys))
print("ERRORS:", len(errors))
for e in errors:
    print("  -", e)

# write summary for output JSON
summary = {
    "xmlValid": xml_valid,
    "allKeysPresent": all(re.search(r'<string name="%s">' % re.escape(k), content) for k in strings),
    "noneIdenticalToDe": not any("IDENTICAL" in e for e in errors),
    "placeholdersMatch": not any("PLACEHOLDER" in e or "NAKED" in e for e in errors),
    "legendSinglePercent": not any("LEGEND" in e for e in errors),
    "apostropheClean": not any("UNESCAPED" in e for e in errors),
    "errorCount": len(errors),
}
json.dump(summary, open(os.path.join(base,'tr-verify-summary.json'),'w',encoding='utf-8'), ensure_ascii=False, indent=2)
print("SUMMARY:", json.dumps(summary, ensure_ascii=False))
