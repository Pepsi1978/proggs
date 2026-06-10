import json, sys, os
cp = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/w9-code-verify.jsonl")
os.makedirs(os.path.dirname(cp), exist_ok=True)
with open(cp, "a", encoding="utf-8", newline="\n") as f:
    f.write(json.dumps({"claim": sys.argv[1], "note": sys.argv[2]}, ensure_ascii=False) + "\n")
print("checkpoint:", sys.argv[1])
