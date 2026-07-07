# -*- coding: utf-8 -*-
import json, os, xml.etree.ElementTree as ET

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(ROOT, "app/src/main/res")
LANGS = ["ja", "ko", "zh-rCN", "zh-rTW"]

def parse_strings(path):
    d = {}
    tree = ET.parse(path)
    for el in tree.getroot():
        if el.tag == "string":
            d[el.get("name")] = "".join(el.itertext())
    return d

de = parse_strings(os.path.join(RES, "values/strings.xml"))
L = {l: parse_strings(os.path.join(RES, f"values-{l}/strings.xml")) for l in LANGS}

# Priority keys to eyeball semantically
KEYS = [
    "ai_limits_dialog_body",
    "paywall_yearly_note", "paywall_legal_hint", "paywall_free_note", "paywall_first_payment",
    "onboarding_free_after_trial", "onboarding_free_trial", "onboarding_start_free", "onboarding_try_premium",
    "churn_auto_renew_note", "churn_offer_auto_renew", "churn_cancel_sub", "churn_google_play_redirect",
    "settings_premium_cancelled_until", "settings_premium_cancelled_desc",
    "consent_card1_body", "consent_toggle_do_not_sell_body", "consent_confirmation",
    "privacy_gate_gemini_body", "privacy_gate_groq_body", "settings_crisis_dialog_body",
    "settings_delete_account_confirm_body", "permission_rationale_microphone",
]

for k in KEYS:
    print("="*70)
    print(f"KEY: {k}")
    print(f"  DE   | {de.get(k,'<MISSING>')}")
    for l in LANGS:
        print(f"  {l:6}| {L[l].get(k,'<MISSING>')}")
