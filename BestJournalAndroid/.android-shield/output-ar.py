# -*- coding: utf-8 -*-
import os, json
out = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-outputs/phase3-tw-ar.json')
os.makedirs(os.path.dirname(out), exist_ok=True)
data = {
  "worker": "tw-ar",
  "lang": "ar",
  "translated": 34,
  "pluralsDone": 2,
  "newKeys": ["paywall_monthly_note"],
  "verification": {
    "allPresent": True,
    "noneIdenticalToDe": True,
    "xmlValid": True,
    "formatArgsOk": True
  },
  "skipped_already_present": [],
  "plugin_bugs_observed": []
}
tmp = out + ".tmp"
with open(tmp, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(data, f, ensure_ascii=False, indent=1)
    f.write("\n")
os.replace(tmp, out)
print("output written:", out)
