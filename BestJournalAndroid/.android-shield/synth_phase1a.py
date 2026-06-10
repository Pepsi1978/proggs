import json, os, datetime, glob

shield = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield")
out = {}
for f in glob.glob(os.path.join(shield, "worker-outputs", "phase1a-*.json")):
    with open(f, encoding="utf-8") as fh:
        out[os.path.basename(f)] = json.load(fh)

def g(name):
    return out.get(name, {})

mb = g("phase1a-manifest-build.json")
sa, sb, sc = g("phase1a-strings-a.json"), g("phase1a-strings-b.json"), g("phase1a-strings-c.json")
pw = g("phase1a-paywall.json")
sca = g("phase1a-screens-arch.json")
ha = g("phase1a-hidden-assets.json")

all_string_findings = (sa.get("findings", []) + sb.get("findings", []) + sc.get("findings", []))
risk_order = {"KRITISCH": 0, "HOCH": 1, "MITTEL": 2, "NIEDRIG": 3}
all_string_findings.sort(key=lambda x: risk_order.get(x.get("risk", "NIEDRIG"), 9))

report = {
    "schema_version": "2.1-normalized",
    "_schemaVariant": "B-normalized",
    "appName": "BestJournalAndroid",
    "scanTimestamp": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "scanMode": "full",
    "app": {"package": "com.bestjournal.app", "version_name": "0.21.10", "version_code": 291, "min_sdk": 26, "target_sdk": 36, "r8": True},
    "layer1_manifest": mb.get("layer1_manifest", {}),
    "layer1_5_assets": ha.get("layer1_5_assets", {}),
    "layer2_dependencies": mb.get("layer2_dependencies", {}),
    "externalEndpoints": mb.get("externalEndpoints", []),
    "layer3_architecture": {
        "viewModels": sca.get("viewModels", []),
        "roomEntities": sca.get("roomEntities", []),
        "useCases": sca.get("useCases", []),
        "repositories": sca.get("repositories", []),
        "diExternalServices": sca.get("diExternalServices", []),
        "backgroundJobs": sca.get("backgroundJobs", []),
    },
    "layer4_screens": sca.get("screens", []),
    "navigation": sca.get("navigation", {}),
    "dialogs": sca.get("dialogs", []),
    "layer4b_wortlaut_findings": all_string_findings,
    "duSie": {"verdict": "durchgaengig Du-Form, konsistent", "perWorker": [sa.get("duSieStats"), sb.get("duSieStats"), sc.get("duSieStats")]},
    "layer5_paywall_deep": pw,
    "layer6_hidden_features": ha.get("hiddenFeatures", {}),
    "critical_findings_raw": (mb.get("criticalObservations", []) + pw.get("criticalObservations", [])
                              + sca.get("criticalObservations", []) + ha.get("criticalObservations", [])),
    "stringsTotal": 1103,
    "stringsReviewed": sum(x.get("stringsReviewed", 0) for x in (sa, sb, sc)),
    "plugin_bugs_observed_all": {k: v.get("plugin_bugs_observed", []) for k, v in out.items()},
}

tmp = os.path.join(shield, "roentgen-report.json.tmp")
final = os.path.join(shield, "roentgen-report.json")
with open(tmp, "w", encoding="utf-8") as f:
    json.dump(report, f, indent=2, ensure_ascii=False)
os.replace(tmp, final)

n_high = sum(1 for x in all_string_findings if x.get("risk") in ("KRITISCH", "HOCH"))
print(f"roentgen-report.json geschrieben: {os.path.getsize(final)} Bytes")
print(f"String-Findings gesamt: {len(all_string_findings)} (KRITISCH/HOCH: {n_high})")
print(f"Strings reviewed: {report['stringsReviewed']}/1103")
