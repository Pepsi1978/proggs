#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Klassifiziert die gu-Reparatur-Liste in FIX / KEEP-ENGLISH / DEFER-ROMANIZED.
Exportiert die FIX-Liste (UI-sichtbar kaputt) mit vollem DE-Text zum Uebersetzen."""
import os, json, re
APP = os.path.expanduser("~/proggs/BestJournalAndroid")
SH = os.path.join(APP, ".android-shield")
r = json.load(open(os.path.join(SH, "gu-repair-list.json"), encoding="utf-8"))

KEEP_KEYS = {
    "label_premium", "settings_premium_section", "settings_export", "settings_export_dialog_title",
    "settings_export_action", "achievements_title", "settings_about_version",
    "consent_toggle_do_not_sell_title", "settings_ccpa_do_not_sell_title", "consent_info_system_backup_title",
}
def has_gujarati(s): return any(0x0A80 <= ord(c) <= 0x0AFF for c in s)
def has_latin_letters(s): return bool(re.search(r"[A-Za-z]", s))

fix, keep, defer = [], [], []
for x in r:
    k, g, d = x["key"], x["gu_current"], x["de"]
    if "♻" in g:
        fix.append(x)                          # echte Korruption -> immer FIX
    elif k.startswith("json_") or k in KEEP_KEYS:
        keep.append(x)                         # legitim englisch / Schema-Keys
    elif k.startswith("ai_prompt_") or (k.startswith("ai_retro_") and ("_format" in k or "_rules" in k)):
        defer.append(x)                        # romanisierte KI-Prompts (nicht UI-sichtbar)
    elif not has_gujarati(g):
        # kein Gujarati-Zeichen, kein ♻ -> Satzzeichen-Fragment (retro '.') oder romanisiert
        # wenn fast nur Satzzeichen/Ziffern/kurze Latin-Tokens -> FIX (UI-kaputt)
        core = re.sub(r"[\s\.…,:;!?&/()\-0-9]|AI|PDF|Google Play|Premium", "", g).strip()
        if len(core) < 4:
            fix.append(x)
        else:
            defer.append(x)                    # laengeres romanisiertes -> defer
    else:
        keep.append(x)

json.dump(fix, open(os.path.join(SH, "gu-fix-list.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=2)
json.dump(defer, open(os.path.join(SH, "gu-defer-list.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(f"FIX (uebersetzen): {len(fix)}")
print(f"KEEP-ENGLISH (nicht anfassen): {len(keep)}")
print(f"DEFER-ROMANIZED (KI-Prompts, separat): {len(defer)}")
print(f"\nDEFER-Keys: {[x['key'] for x in defer]}")
