import json, os, tempfile

cp_path = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/fa4.jsonl')
os.makedirs(os.path.dirname(cp_path), exist_ok=True)
entry = {
    "worker": "fa4",
    "findingId": "A7",
    "stage": "located",
    "file": "app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt",
    "dialog_line": 2221,
    "content_end_line": 2319,
    "existing_usage_line": 1455,
    "existing_call_style": "com.bestjournal.app.ui.components.AiOutputDisclaimer()",
    "import_needed": False,
    "note": "fully-qualified call, no import needed"
}
with open(cp_path, 'a', encoding='utf-8') as f:
    f.write(json.dumps(entry, ensure_ascii=False) + '\n')
print("checkpoint written")
