import json, os
wo = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield/worker-outputs")
uwg = json.load(open(os.path.join(wo, "phase1b-recht-uwg.json"), encoding="utf-8"))
hwg = json.load(open(os.path.join(wo, "phase1b-recht-hwg-ai.json"), encoding="utf-8"))
want = {"B1","B2","B3","B4","A1","A2","A3","A4"}
for src in (uwg, hwg):
    for f in src.get("findings", []):
        fid = f.get("findingId","?")
        if fid in want:
            print("="*70)
            print(f"{fid} | {f.get('riskLevel','')} | {f.get('category','')} | {f.get('stringKey', f.get('file',''))}")
            cur = f.get("currentText","")
            print(f"AKTUELL: {cur[:400]}")
            print(f"GRUND: {f.get('rationale','')[:300]}")
            for i, sf in enumerate(f.get("suggestedFixes", [])[:3], 1):
                t = sf.get("text","") if isinstance(sf, dict) else str(sf)
                d = sf.get("lengthDeltaPct","?") if isinstance(sf, dict) else "?"
                print(f"  [{i}] ({d:+}% if num) {t[:350]}" if isinstance(d,(int,float)) else f"  [{i}] {t[:350]}")
            print()
