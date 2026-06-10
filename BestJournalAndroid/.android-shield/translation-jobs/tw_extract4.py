import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
with open(os.path.join(base, 'job-plan.json'), 'r', encoding='utf-8') as f:
    plan = json.load(f)
job = plan['languages']['zh-rTW']

with open(os.path.join(base, 'de-reference.json'), 'r', encoding='utf-8') as f:
    de = json.load(f)

print("DE-REF top type:", type(de).__name__)
if isinstance(de, dict):
    print("DE-REF keys sample:", list(de.keys())[:8])

# de-reference structure: try to find each key
def get_de(key):
    if isinstance(de, dict):
        if key in de:
            return de[key]
        # maybe nested under 'strings'/'plurals'
        for sub in ('strings', 'plurals', 'values'):
            if sub in de and isinstance(de[sub], dict) and key in de[sub]:
                return de[sub][key]
    return None

print("\n=== STRINGS DE-SOURCE ===")
for k in job['strings']:
    v = get_de(k)
    print(f"\n[{k}]")
    print(repr(v))

print("\n=== PLURALS DE-SOURCE ===")
for k in job['plurals']:
    v = get_de(k)
    print(f"\n[{k}]")
    print(repr(v))
