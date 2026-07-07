# -*- coding: utf-8 -*-
import sys, json, os, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
CP = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/r3-abo.jsonl')
def log(checkpoint, note=""):
    rec = {"worker": "r3-abo", "checkpoint": checkpoint, "note": note}
    with open(CP, 'a', encoding='utf-8') as f:
        f.write(json.dumps(rec, ensure_ascii=False) + "\n")
    print("CP:", checkpoint)
if __name__ == '__main__':
    log(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "")
