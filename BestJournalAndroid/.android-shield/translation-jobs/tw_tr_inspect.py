# -*- coding: utf-8 -*-
import os, re, json
import xml.etree.ElementTree as ET

app = os.path.expanduser('~/proggs/BestJournalAndroid')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
tr_file = os.path.join(app, 'app/src/main/res/values-tr/strings.xml')
de_file = os.path.join(app, 'app/src/main/res/values/strings.xml')

jp = json.load(open(os.path.join(base,'job-plan.json'), encoding='utf-8'))
tr = jp['languages']['tr']
strings = tr['strings']
plurals = tr['plurals']
legend_keys = tr['special'][0].split(': ',1)[1].split(',')

print("tr_file exists:", os.path.exists(tr_file))

with open(tr_file, encoding='utf-8') as f:
    content = f.read()

# Which job strings already present (as <string name="...">)?
print("\n=== STRING KEYS present in values-tr ===")
present, missing = [], []
for k in strings:
    if re.search(r'<string name="%s"' % re.escape(k), content):
        present.append(k)
    else:
        missing.append(k)
print("PRESENT (%d): %s" % (len(present), present))
print("MISSING (%d): %s" % (len(missing), missing))

print("\n=== PLURALS present in values-tr ===")
for k in plurals:
    m = re.search(r'<plurals name="%s">.*?</plurals>' % re.escape(k), content, re.DOTALL)
    print(f"[{k}] present={bool(m)}")
    if m: print(m.group(0))

print("\n=== LEGEND keys in values-tr (current %% state) ===")
for k in legend_keys:
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(k), content, re.DOTALL)
    val = m.group(1) if m else '<<MISSING>>'
    has_dbl = '%%' in val
    print(f"[{k}] dbl%%={has_dbl} :: {val!r}")

print("\n=== LEGEND keys in DE (values/strings.xml) ===")
with open(de_file, encoding='utf-8') as f:
    de_content = f.read()
for k in legend_keys:
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(k), de_content, re.DOTALL)
    val = m.group(1) if m else '<<MISSING>>'
    print(f"[{k}] :: {val!r}")

# Check existing tr terminology: KI -> YZ? Search for both
print("\n=== Terminology check in values-tr ===")
print("YZ occurrences:", len(re.findall(r'\bYZ\b', content)))
print("'YZ ' or 'YZ' substring count:", content.count('YZ'))
print("'KI' substring count:", content.count('KI'))
print("'YZ Profil' present:", 'YZ Profil' in content or 'YZ profil' in content.lower())
# revoke / cayma terms
print("'cayma' count:", content.lower().count('cayma'))
