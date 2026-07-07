# -*- coding: utf-8 -*-
import os, json, datetime
import xml.etree.ElementTree as ET

app = os.path.expanduser('~/proggs/BestJournalAndroid')
base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs')
tr_file = os.path.join(app, 'app/src/main/res/values-tr/strings.xml')
ckpt_dir = os.path.join(base, 'worker-checkpoints')
out_dir = os.path.join(base, 'worker-outputs')

# final XML parse confirmation
ET.parse(tr_file)

ts = datetime.datetime.now().isoformat()

# checkpoint
ckpt = {
    "worker": "tw-tr", "lang": "tr", "ts": ts, "status": "done",
    "translated": 34, "pluralsDone": 2, "legendPercentFixed": 15,
    "newKeys": ["paywall_monthly_note"],
    "apostrophesFixed": {"ai_prompt_custom_intro":1,"ai_prompt_no_dates_rule":4,"ai_prompt_rerank_system":1,
                          "consent_toggle_do_not_sell_body":1,"privacy_gate_groq_body":1,"privacy_gate_tts_body":1}
}
with open(os.path.join(ckpt_dir, 'tw-tr.jsonl'), 'a', encoding='utf-8') as f:
    f.write(json.dumps(ckpt, ensure_ascii=False) + '\n')

# output JSON
output = {
    "worker": "tw-tr",
    "lang": "tr",
    "translated": 34,
    "pluralsDone": 2,
    "legendPercentFixed": 15,
    "newKeys": ["paywall_monthly_note"],
    "verification": {
        "xmlValid": True,
        "allKeysPresent": True,
        "noneIdenticalToDe": True,
        "placeholdersMatch": True,
        "legendSinglePercent": True,
        "apostropheValidator": "OK: 0 unescaped",
        "apostrophesAutoFixed": 9,
        "errorCount": 0
    },
    "plugin_bugs_observed": [
        {
            "symptom": "Bestehende values-tr-Strings waren teils mit 'KI' statt dem Bestands-Term 'YZ' uebersetzt (z.B. profile_entropy_long, mehrere ai_prompt_*), parallel zu 55 'YZ'-Vorkommen — inkonsistente KI-Terminologie im selben File.",
            "evidence": "grep: 55x 'YZ' vs. 'KI'-Substrings in profile_entropy_long/ai_prompt_*; job-plan flaggte genau diese Keys als delta",
            "suggestion": "Beim Delta-Scan auch Terminologie-Konsistenz (KI->YZ) als Aenderungsgrund erfassen; Worker hat alle 34 jetzt auf 'YZ' vereinheitlicht."
        },
        {
            "symptom": "Frische tr-Uebersetzungen enthielten 6 Strings mit nicht-escapten Apostrophen (Groq'a, ABD'nin, ABD'deki, 2'sinin, 23.04.'te etc.) — haetten den Android-Resource-Build gebrochen.",
            "evidence": "Post-Verify meldete 6 UNESCAPED-apostrophe-Faelle; nach \\' -Escaping: offizieller check_apostrophes.py = 'OK: 0 unescaped'",
            "suggestion": "tr gehoert wie fr/it in die Apostroph-Validator-Pflicht (FIN-044) — tuerkische Suffixe nach Eigennamen/Zahlen (Groq'a, ABD'nin) erzeugen genauso bare ' wie romanische l'/d'."
        }
    ]
}
with open(os.path.join(out_dir, 'phase3-tw-tr.json'), 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print("Checkpoint + output written. XML valid.")
print(json.dumps(output, ensure_ascii=False))
