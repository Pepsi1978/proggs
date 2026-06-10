import json, os, tempfile

# --- final checkpoint ---
cp_path = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/fa4.jsonl')
os.makedirs(os.path.dirname(cp_path), exist_ok=True)
with open(cp_path, 'a', encoding='utf-8') as f:
    f.write(json.dumps({
        "worker": "fa4", "findingId": "A7", "stage": "applied",
        "file": "app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt",
        "inserted_lines": "2320-2321",
        "validated": True
    }, ensure_ascii=False) + '\n')

# --- atomic output write ---
out_path = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-outputs/phase2-fa4.json')
os.makedirs(os.path.dirname(out_path), exist_ok=True)
payload = {
    "worker": "fa4",
    "applied": [{
        "findingId": "A7",
        "file": "app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt",
        "change": "AiOutputDisclaimer() (KI-Disclaimer) am Ende des AdviceDerivationDialog-Contents (in der scrollbaren Column, vor confirmButton) eingefuegt, plus Spacer(12.dp). Aufruf-Stil 1:1 wie bestehende Verwendung in derselben Datei (Z.1455, voll-qualifiziert) und wie in RetrospectiveScreen/EntryDetailScreen. Kein Import noetig (voll-qualifizierter Aufruf).",
        "lines": "2320-2321"
    }],
    "skipped": [],
    "plugin_bugs_observed": []
}
dir_name = os.path.dirname(out_path)
with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp', delete=False, encoding='utf-8') as tmp:
    json.dump(payload, tmp, ensure_ascii=False, indent=2)
    tmp.write('\n')
    tmp_path = tmp.name
os.replace(tmp_path, out_path)
print("output + final checkpoint written")
