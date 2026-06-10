import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
with open(os.path.join(base, 'job-plan.json'), 'r', encoding='utf-8') as f:
    plan = json.load(f)

# Top-level structure
print("TOP-LEVEL KEYS:", list(plan.keys()) if isinstance(plan, dict) else type(plan))

# Find zh-rTW relevant data
def find_lang(obj, target):
    results = []
    if isinstance(obj, dict):
        for k, v in obj.items():
            if target in str(k):
                results.append((k, v))
            results.extend(find_lang(v, target))
    elif isinstance(obj, list):
        for item in obj:
            results.extend(find_lang(item, target))
    return results

# Print structure overview
if isinstance(plan, dict):
    for k, v in plan.items():
        t = type(v).__name__
        n = len(v) if isinstance(v, (list, dict)) else ''
        print(f"  {k}: {t} ({n})")
