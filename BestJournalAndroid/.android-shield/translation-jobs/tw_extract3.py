import json, os

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
with open(os.path.join(base, 'job-plan.json'), 'r', encoding='utf-8') as f:
    plan = json.load(f)

langs = plan['languages']
print("LANG-CODES:", list(langs.keys()))
print()
# zh-rTW
key = 'zh-rTW'
if key not in langs:
    # try variants
    for k in langs:
        if 'zh' in k.lower() or 'TW' in k:
            print("FOUND VARIANT:", k)
    raise SystemExit("zh-rTW not found")

job = langs[key]
print("zh-rTW job keys:", list(job.keys()) if isinstance(job, dict) else type(job))
print()
print(json.dumps(job, ensure_ascii=False, indent=2)[:6000])
