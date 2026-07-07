import json, os, tempfile, time

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
ck_dir = os.path.join(base, 'worker-checkpoints')
out_dir = os.path.join(base, 'worker-outputs')
os.makedirs(ck_dir, exist_ok=True)
os.makedirs(out_dir, exist_ok=True)

ts = time.strftime('%Y-%m-%dT%H:%M:%S')

applied = [
    {"findingId": "B7", "key": "settings_report_ai_confirm_body", "line": 639,
     "change": "Service-Zusage zu Erwartungs-Aussage: 'Wir antworten in der Regel innerhalb von 24 Stunden an Werktagen.'",
     "patchKind": "text-replace"},
    {"findingId": "C1/C2", "key": "consent_card3_title", "line": 615,
     "change": "'Anonyme Statistiken' -> 'Pseudonyme Statistiken' (Firebase Analytics ist pseudonym)",
     "patchKind": "text-replace"},
    {"findingId": "D1", "key": "paywall_monthly_note", "line": 1496,
     "change": "Preis sichtbar gemacht: 'Danach %1$s pro Monat, verlaengert sich automatisch\\nJederzeit kuendbar' (analog yearly-Note)",
     "patchKind": "text-replace"},
]

validation = {
    "xml_parse": "OK",
    "checks": ["xml-parse", "text-present-x3", "format-string-integrity"],
    "fix1_present": True,
    "fix2_present": True,
    "fix3_present": True,
    "fix3_placeholder_count": 1,
    "fix3_newline_literal": True,
    "status": "passed",
    "retries": 0
}

# Checkpoint (append JSONL)
ck_entry = {"ts": ts, "worker": "fa7", "phase": "phase2-strings",
            "applied_keys": ["settings_report_ai_confirm_body", "consent_card3_title", "paywall_monthly_note"],
            "validation": "passed", "all_ok": True}
ck_path = os.path.join(ck_dir, 'fa7.jsonl')
with open(ck_path, 'a', encoding='utf-8') as f:
    f.write(json.dumps(ck_entry, ensure_ascii=False) + "\n")

# Output JSON (atomic write)
output = {
    "worker": "fa7",
    "file": "app/src/main/res/values/strings.xml",
    "applied": applied,
    "validation": validation,
    "commitGemacht": False,
    "plugin_bugs_observed": []
}
out_path = os.path.join(out_dir, 'phase2-fa7.json')
fd, tmp = tempfile.mkstemp(dir=out_dir, suffix='.tmp')
with os.fdopen(fd, 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)
    f.write("\n")
os.replace(tmp, out_path)

print("Checkpoint:", ck_path)
print("Output:", out_path)
print(json.dumps(output, ensure_ascii=False, indent=2))
