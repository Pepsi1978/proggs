import json, os
wo = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield/worker-outputs")
hwg = json.load(open(os.path.join(wo, "phase1b-recht-hwg-ai.json"), encoding="utf-8"))
for f in hwg.get("findings", []):
    if f.get("findingId") in ("A1","A2","A3","A4"):
        print("="*70)
        print(f.get("findingId"), "|", f.get("stringKey"))
        sf = f.get("suggestedFixes", [])
        print(json.dumps(sf, ensure_ascii=False, indent=1)[:2600])
        print()
