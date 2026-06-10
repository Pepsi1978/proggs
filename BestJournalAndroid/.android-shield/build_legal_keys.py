import json, os, glob
shield = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield")

# 1) Keys aus allen Recht-Findings sammeln
keys = set()
for f in glob.glob(os.path.join(shield, "worker-outputs", "phase1b-recht-*.json")):
    d = json.load(open(f, encoding="utf-8"))
    for fi in d.get("findings", []):
        k = fi.get("stringKey")
        if k: keys.add(k)
        # manche Worker haben file=strings.xml + key im currentText-Kontext
# 2) Aus Roentgen-String-Findings die rechtsrelevanten Kategorien
r = json.load(open(os.path.join(shield, "roentgen-report.json"), encoding="utf-8"))
for fi in r["layer4b_wortlaut_findings"]:
    if fi.get("category") in ("subscription", "legal-text", "privacy-claim") or fi.get("risk") in ("KRITISCH", "HOCH"):
        if fi.get("key"): keys.add(fi["key"])

out = {"legalCriticalKeys": sorted(keys), "count": len(keys)}
p = os.path.join(shield, "legal-keys-2026-06-10.json")
with open(p + ".tmp", "w", encoding="utf-8") as fh:
    json.dump(out, fh, indent=1, ensure_ascii=False)
os.replace(p + ".tmp", p)
print(f"Legal-Keys: {len(keys)}")
