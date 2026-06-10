# -*- coding: utf-8 -*-
"""Read-only inspection of existing values-ja/strings.xml terminology.
Writes results to a non-protected JSON file so the worker can read them."""
import re, os, json

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
f = os.path.join(APP, 'app/src/main/res/values-ja/strings.xml')
with open(f, 'r', encoding='utf-8') as fh:
    c = fh.read()

keys = [
    'paywall_legal_hint', 'settings_revoke_subtitle', 'settings_revoke_confirm_title',
    'settings_revoke_confirm_body', 'settings_delete_account_subtitle',
    'settings_delete_account_confirm_body', 'delete_account',
    'paywall_from_per_day', 'paywall_monthly_note', 'settings_analytics_title',
    'consent_card3_title', 'paywall_subscribe_cta', 'paywall_terms',
    'settings_revoke_title', 'churn_offer_feature_perspectives',
    'settings_premium_feature_profiles', 'paywall_feature_profiles',
]
out = {}
for key in keys:
    m = re.search(r'<string name="' + re.escape(key) + r'"[^>]*>(.*?)</string>', c, re.S)
    out[key] = m.group(1) if m else None

# also list which of MY 34 target string keys already exist (for pre-check overview)
target = [
    "ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro",
    "ai_prompt_goals_rules","ai_prompt_insight_attitude","ai_prompt_no_dates_rule",
    "ai_prompt_rerank_system","churn_offer_feature_perspectives","consent_card3_title",
    "consent_toggle_analytics_body","consent_toggle_analytics_title",
    "consent_toggle_do_not_sell_body","onboarding_feature_secure",
    "onboarding_premium_feature_noads","paywall_feature_noads","paywall_feature_profiles",
    "paywall_from_per_day","paywall_headline_clarity_sub","paywall_monthly_note",
    "privacy_gate_groq_body","privacy_gate_tts_body","privacy_gate_tts_title",
    "profile_entropy_long","retro_benefit_weekly","settings_analytics_title",
    "settings_delete_account_confirm_body","settings_delete_account_subtitle",
    "settings_feedback_dialog_desc","settings_premium_feature_5_perspectives",
    "settings_premium_feature_profiles","settings_report_ai_confirm_body",
    "settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle",
]
present = {}
for key in target:
    cnt = len(re.findall(r'<string name="' + re.escape(key) + r'"', c))
    present[key] = cnt
plural_present = {}
for key in ["datetime_months_relative", "datetime_years_relative"]:
    cnt = len(re.findall(r'<plurals name="' + re.escape(key) + r'"', c))
    plural_present[key] = cnt

res = {'terminology': out, 'string_present_counts': present,
       'plural_present_counts': plural_present, 'filesize': os.path.getsize(f)}
outpath = os.path.join(APP, '.android-shield/translation-jobs/worker-checkpoints/tw-ja-existing.json')
with open(outpath, 'w', encoding='utf-8') as fh:
    json.dump(res, fh, ensure_ascii=False, indent=1)
print('WROTE', outpath)
