import json, os, tempfile

out_dir = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield/worker-outputs")
os.makedirs(out_dir, exist_ok=True)
out_path = os.path.join(out_dir, "phase2-fa2.json")

data = {
    "worker": "fa2",
    "applied": [
        {
            "findingId": "F1",
            "file": "app/src/main/java/com/bestjournal/app/ui/screens/settings/ChurnFlowDialog.kt",
            "change": "Cancel button ('Zu Google Play') changed from a low-emphasis TextButton at alpha 0.6 to a full-emphasis OutlinedButton at alpha 1.0, fillMaxWidth, same shape (RoundedCornerShape 14dp), same contentPadding and titleSmall text style as the stay button. Stay button kept but breathing pulse removed (.scale(stayButtonScale) dropped; stayButtonScale animateFloat + its intent-doc comment deleted). Visual-competition comment above the cancel button removed. Added missing OutlinedButton import (alphabetical).",
            "lines": "imports:48; anim removed ~1011-1024; buttons 1048-1081"
        },
        {
            "findingId": "E3",
            "file": "app/src/main/java/com/bestjournal/app/ui/screens/settings/ChurnFlowDialog.kt",
            "change": "Fallback retention price neutralized: 'retentionPrice ?: run { Constants.RETENTION_YEARLY_PRICE / RETENTION_MONTHLY_PRICE }' replaced with 'retentionPrice ?: \"…\"'. Live Play price (elvis precedence) unchanged. Constants.kt left untouched per mandate; RETENTION_*_PRICE consts were used ONLY at this display site (verified by grep, no billing-logic usage).",
            "lines": "689-691"
        }
    ],
    "skipped": [],
    "validation": {
        "visualEquivalence": "PASS - both stay (Button) and cancel (OutlinedButton) are full-width, RoundedCornerShape(14dp), identical contentPadding (28dp/10dp), titleSmall; cancel at alpha 1.0 (onSurface); pulse animation removed; no orphan stayButtonScale ref; OutlinedButton import added; brackets balanced (Column 1008-1082, fn closes 1083).",
        "fallbackNeutralized": "PASS - no hardcoded EUR shown when live price missing; placeholder '…' matches main paywall; live-price elvis logic intact; subscriptionType/isYearly still used for periodLabel (no orphan params)."
    },
    "plugin_bugs_observed": []
}

fd, tmp = tempfile.mkstemp(dir=out_dir, suffix=".tmp")
with os.fdopen(fd, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
    f.write("\n")
os.replace(tmp, out_path)
print("WROTE", out_path)

cp = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield/worker-checkpoints/fa2.jsonl")
with open(cp, "a", encoding="utf-8") as f:
    f.write(json.dumps({"ts":"2026-06-10T22:42:00Z","worker":"fa2","step":"done","applied":["F1","E3"],"validated":True,"import_added":"OutlinedButton","output":out_path}, ensure_ascii=False) + "\n")
print("CHECKPOINT done")
