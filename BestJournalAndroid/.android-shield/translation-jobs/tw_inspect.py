import json, os, re
import xml.etree.ElementTree as ET

app = os.path.expanduser('~/proggs/BestJournalAndroid')
tgt = os.path.join(app, 'app/src/main/res/values-zh-rTW/strings.xml')
print("EXISTS:", os.path.exists(tgt))
print("SIZE:", os.path.getsize(tgt) if os.path.exists(tgt) else 0)

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
with open(os.path.join(base, 'job-plan.json'), 'r', encoding='utf-8') as f:
    job = json.load(f)['languages']['zh-rTW']

string_keys = job['strings']
plural_keys = job['plurals']

# Parse target xml
tree = ET.parse(tgt)
root = tree.getroot()

existing_strings = {}
for el in root.findall('string'):
    existing_strings[el.get('name')] = (el.text or '')

existing_plurals = {}
for el in root.findall('plurals'):
    items = {it.get('quantity'): (it.text or '') for it in el.findall('item')}
    existing_plurals[el.get('name')] = items

print("\n=== STRING KEYS STATUS ===")
for k in string_keys:
    present = k in existing_strings
    print(f"{'PRESENT' if present else 'MISSING'}: {k}", end='')
    if present:
        print(f"  | val={existing_strings[k][:60]!r}")
    else:
        print()

print("\n=== PLURAL KEYS STATUS ===")
for k in plural_keys:
    if k in existing_plurals:
        print(f"PRESENT: {k} -> {existing_plurals[k]}")
    else:
        print(f"MISSING: {k}")
