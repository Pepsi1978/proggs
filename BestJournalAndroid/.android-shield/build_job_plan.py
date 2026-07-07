# Build per-language translation job plan (FIN-038) + DE reference texts.
import json, os, re
import xml.etree.ElementTree as ET

app = os.path.expanduser("~/proggs/BestJournalAndroid")
res = os.path.join(app, "app/src/main/res")
shield = os.path.join(app, ".android-shield")

ALL_LANGS = ["ar","bn","en","es","fr","gu","hi","in","it","ja","kn","ko","ml","mr",
             "nl","pl","pt-rBR","pt-rPT","ta","te","th","tr","uk","ur","zh-rCN","zh-rTW"]

# Keys whose DE text changed in phase 2 (or which are stale everywhere) -> ALL languages.
CHANGED_ALL = [
    "retro_benefit_weekly", "settings_premium_feature_profiles", "paywall_feature_profiles",
    "settings_premium_feature_5_perspectives", "churn_offer_feature_perspectives",
    "onboarding_feature_secure", "ai_prompt_no_dates_rule", "ai_prompt_entropy_intro",
    "ai_prompt_custom_intro", "ai_prompt_custom_rules", "ai_prompt_rerank_system",
    "profile_entropy_long", "ai_prompt_goals_rules", "ai_prompt_insight_attitude",
    "consent_toggle_analytics_body", "consent_toggle_analytics_title", "settings_analytics_title",
    "consent_card3_title", "privacy_gate_tts_body", "onboarding_premium_feature_noads",
    "paywall_feature_noads", "paywall_headline_clarity_sub", "settings_feedback_dialog_desc",
    "settings_report_ai_confirm_body", "paywall_monthly_note",
    # X1/X2/X3/X4 stale clusters (stale in essentially all languages):
    "settings_delete_account_subtitle", "settings_delete_account_confirm_body",
    "paywall_from_per_day",
    "settings_revoke_subtitle", "settings_revoke_confirm_title", "settings_revoke_confirm_body",
    "consent_toggle_do_not_sell_body", "privacy_gate_tts_title", "privacy_gate_groq_body",
]

# Plurals stale in all 26 languages (X5).
PLURALS_ALL = ["datetime_months_relative", "datetime_years_relative"]

# Language-specific extras from X6-X12.
EXTRA = {
    "pl": ["ai_prompt_custom_schema", "ai_prompt_rerank_entries_header", "ai_prompt_rerank_actions_header",
            "ai_prompt_rerank_user_focus_header", "dashboard_profile_focus_summary",
            "dashboard_profile_focus_entropy", "dashboard_profile_focus_goals", "dashboard_profile_focus_insight"],
    "nl": ["ai_prompt_custom_schema", "ai_prompt_rerank_entries_header", "ai_prompt_rerank_actions_header",
            "ai_prompt_rerank_user_focus_header"],
    "pt-rBR": [],  # rerank_system already in CHANGED_ALL
    "hi": ["ai_prompt_rerank_entries_header", "ai_prompt_rerank_actions_header",
            "ai_prompt_rerank_user_focus_header", "ai_limits_dialog_body"],
    "in": ["ai_prompt_rerank_entries_header", "ai_prompt_rerank_actions_header",
            "ai_prompt_rerank_user_focus_header"],
    "kn": ["ai_limits_dialog_body"],
    "en": ["ai_limits_dialog_body"],
    "es": ["ai_limits_dialog_body"],
    "ta": ["prompt_clear_confirm_text"],
    "te": ["prompt_clear_confirm_text", "consent_footer_link_impressum"],
    "mr": ["consent_footer_link_impressum"],
    "pt-rPT": ["consent_footer_link_impressum"],
    "ur": ["privacy_gate_gemini_body"],  # DSGVO spacing fix opportunity (X12)
}

# tr: mechanical %%-fix for legend_* (X9) — handled as special task, find keys now.
de_path = os.path.join(res, "values/strings.xml")
tree = ET.parse(de_path)
root = tree.getroot()
de_strings, de_plurals = {}, {}
for el in root:
    if el.tag == "string" and el.get("translatable") != "false":
        de_strings[el.get("name")] = "".join(el.itertext())
    elif el.tag == "plurals":
        de_plurals[el.get("name")] = {i.get("quantity"): "".join(i.itertext()) for i in el}

# Verify all keys exist in DE.
missing = [k for k in CHANGED_ALL if k not in de_strings]
extra_missing = {l: [k for k in ks if k not in de_strings] for l, ks in EXTRA.items()}
extra_missing = {l: v for l, v in extra_missing.items() if v}

# tr legend keys with %% in tr file:
tr_raw = open(os.path.join(res, "values-tr/strings.xml"), encoding="utf-8").read()
tr_legend = sorted(set(re.findall(r'name="(legend_[^"]+)"[^<]*<', tr_raw)))
tr_pct = [k for k in tr_legend if re.search('name="' + k + '">[^<]*%%', tr_raw)]

plan = {"generatedAt": "2026-06-10", "languages": {}}
for lang in ALL_LANGS:
    keys = list(CHANGED_ALL) + EXTRA.get(lang, [])
    plan["languages"][lang] = {
        "strings": sorted(set(keys)),
        "plurals": PLURALS_ALL,
        "special": (["legend-percent-fix: " + ",".join(tr_pct)] if lang == "tr" else []),
        "count": len(set(keys)) + len(PLURALS_ALL),
    }

# DE reference file (texts the workers translate FROM) — keys union of everything.
union = sorted(set(CHANGED_ALL) | {k for ks in EXTRA.values() for k in ks})
de_ref = {"strings": {k: de_strings[k] for k in union if k in de_strings},
          "plurals": {k: de_plurals[k] for k in PLURALS_ALL if k in de_plurals}}

for name, data in (("translation-jobs/job-plan.json", plan), ("translation-jobs/de-reference.json", de_ref)):
    p = os.path.join(shield, name)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p + ".tmp", "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
    os.replace(p + ".tmp", p)

print(f"Job-Plan: {len(ALL_LANGS)} Sprachen, Basis-Keys: {len(CHANGED_ALL)} strings + {len(PLURALS_ALL)} plurals")
print(f"DE-Referenz: {len(de_ref['strings'])} strings, {len(de_ref['plurals'])} plurals, {os.path.getsize(os.path.join(shield,'translation-jobs/de-reference.json'))} B")
print(f"Fehlende DE-Keys (sollte leer sein): {missing}")
print(f"Extra-Missing: {extra_missing}")
print(f"tr legend %%-Keys: {len(tr_pct)}")
total = sum(v["count"] for v in plan["languages"].values())
print(f"Gesamt-Uebersetzungseinheiten: {total}")
