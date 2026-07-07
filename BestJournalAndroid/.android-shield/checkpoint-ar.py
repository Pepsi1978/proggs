# -*- coding: utf-8 -*-
import os, json, datetime
cp = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/tw-ar.jsonl')
os.makedirs(os.path.dirname(cp), exist_ok=True)
rec = {"ts": datetime.datetime.now().isoformat(timespec='seconds'),
       "worker": "tw-ar", "lang": "ar",
       "msg": "applied 34 strings (1 new: paywall_monthly_note) + 2 plurals replaced, pre-verify"}
with open(cp, 'a', encoding='utf-8') as f:
    f.write(json.dumps(rec, ensure_ascii=False) + "\n")
print("checkpoint written")
