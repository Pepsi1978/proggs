import json, os, tempfile

out_dir = r"C:/Users/barwa/proggs/BestJournalAndroid/.android-shield/worker-outputs"
os.makedirs(out_dir, exist_ok=True)
out_path = os.path.join(out_dir, "phase2-fa6.json")

data = {
    "worker": "fa6",
    "applied": [
        {
            "findingId": "C5",
            "change": "Added <meta-data android:name=\"firebase_analytics_collection_enabled\" android:value=\"false\" /> as a direct child of <application> (after the FileProvider) in app/src/main/AndroidManifest.xml. Poka-Yoke (Defense in Depth, TDDDG §25): closes the SDK-init window before BestJournalApp.onCreate; runtime consent flow (setAnalyticsCollectionEnabled(true) after consent) unchanged."
        }
    ],
    "validation": {"xmlValid": True},
    "plugin_bugs_observed": []
}

fd, tmp = tempfile.mkstemp(dir=out_dir, suffix=".tmp")
with os.fdopen(fd, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
    f.write("\n")
os.replace(tmp, out_path)

# checkpoint
cp_dir = r"C:/Users/barwa/proggs/BestJournalAndroid/.android-shield/worker-checkpoints"
os.makedirs(cp_dir, exist_ok=True)
with open(os.path.join(cp_dir, "fa6.jsonl"), "a", encoding="utf-8") as f:
    f.write(json.dumps({"worker": "fa6", "step": "done", "findingId": "C5", "xmlValid": True}, ensure_ascii=False) + "\n")

print("wrote", out_path)
