import json, datetime, os, sys

cp_dir = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/worker-checkpoints')
os.makedirs(cp_dir, exist_ok=True)
phase = sys.argv[1] if len(sys.argv) > 1 else 'unknown'
status = sys.argv[2] if len(sys.argv) > 2 else 'unknown'
note = sys.argv[3] if len(sys.argv) > 3 else ''
cp = {
    'ts': datetime.datetime.now().isoformat(),
    'worker': 'tw-ptbr',
    'phase': phase,
    'status': status,
    'units': 36,
    'strings': 34,
    'plurals': 2,
    'target': 'values-pt-rBR/strings.xml',
    'note': note,
}
with open(os.path.join(cp_dir, 'tw-ptbr.jsonl'), 'a', encoding='utf-8') as f:
    f.write(json.dumps(cp, ensure_ascii=False) + '\n')
print('checkpoint written:', phase, status)
