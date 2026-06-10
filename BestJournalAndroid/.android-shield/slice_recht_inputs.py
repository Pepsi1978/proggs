import json, os

shield = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield")
with open(os.path.join(shield, "roentgen-report.json"), encoding="utf-8") as f:
    r = json.load(f)

fnd = r["layer4b_wortlaut_findings"]

def write(name, data):
    p = os.path.join(shield, name)
    tmp = p + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    os.replace(tmp, p)
    print(f"{name}: {os.path.getsize(p)} B")

write("recht-input-privacy.json", {
    "findings": [x for x in fnd if x.get("category") in ("privacy-claim", "legal-text")],
    "manifest": r["layer1_manifest"],
    "dependencies": r["layer2_dependencies"],
    "externalEndpoints": r["externalEndpoints"],
    "roomEntities": r["layer3_architecture"]["roomEntities"],
    "diExternalServices": r["layer3_architecture"]["diExternalServices"],
    "accountDeletion": r["layer6_hidden_features"].get("accountDeletion"),
    "dataExport": r["layer6_hidden_features"].get("dataExport"),
    "assets": r["layer1_5_assets"],
})
write("recht-input-uwg.json", {
    "findings": [x for x in fnd if x.get("category") in ("marketing-claim", "ai-claim")],
    "freeVsPremium": r["layer5_paywall_deep"].get("freeVsPremium", []),
    "criticalObservations": r["critical_findings_raw"],
    "screens": [{"name": s.get("name"), "purpose": s.get("purpose")} for s in r["layer4_screens"]],
})
write("recht-input-abo.json", {
    "findings": [x for x in fnd if x.get("category") == "subscription"],
    "paywallDeep": r["layer5_paywall_deep"],
})
write("recht-input-hwg.json", {
    "findings": [x for x in fnd if x.get("category") in ("health-language", "ai-claim")],
    "aiServices": [s for s in r["layer3_architecture"]["diExternalServices"]],
})
