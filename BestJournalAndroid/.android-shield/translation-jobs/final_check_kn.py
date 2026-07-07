#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Finaler Sanity-Check + Checkpoint fuer kn-Worker."""
import json, os
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
JOBS = os.path.join(ROOT, '.android-shield', 'translation-jobs')
KN = os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml')

# validate JSON outputs
for p in ['worker-outputs/phase3-tw-kn.json', 'kn-data.json']:
    json.load(open(os.path.join(JOBS, p), encoding='utf-8'))
    print(f"{p}: valid JSON")

# final XML parse on target
ET.parse(KN)
print("values-kn/strings.xml: parse OK")

# final checkpoint
cp = os.path.join(JOBS, 'worker-checkpoints', 'tw-kn.jsonl')
line = {"ts":"2026-06-10T23:20:00","step":"done","lang":"kn",
        "note":"35 strings + 2 plurals written; paywall_monthly_note neu; ai_limits 150x4; XML valide; verify+native-digits exit0",
        "keys_done":37}
with open(cp, 'a', encoding='utf-8', newline='\n') as f:
    f.write(json.dumps(line, ensure_ascii=False) + '\n')
print("checkpoint appended")
