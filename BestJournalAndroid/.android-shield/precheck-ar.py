# -*- coding: utf-8 -*-
import os, re, json, xml.etree.ElementTree as ET

target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-ar/strings.xml')
de_ref = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/de-reference.json')

job_strings = [
 "ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro",
 "ai_prompt_goals_rules","ai_prompt_insight_attitude","ai_prompt_no_dates_rule",
 "ai_prompt_rerank_system","churn_offer_feature_perspectives","consent_card3_title",
 "consent_toggle_analytics_body","consent_toggle_analytics_title","consent_toggle_do_not_sell_body",
 "onboarding_feature_secure","onboarding_premium_feature_noads","paywall_feature_noads",
 "paywall_feature_profiles","paywall_from_per_day","paywall_headline_clarity_sub",
 "paywall_monthly_note","privacy_gate_groq_body","privacy_gate_tts_body","privacy_gate_tts_title",
 "profile_entropy_long","retro_benefit_weekly","settings_analytics_title",
 "settings_delete_account_confirm_body","settings_delete_account_subtitle","settings_feedback_dialog_desc",
 "settings_premium_feature_5_perspectives","settings_premium_feature_profiles","settings_report_ai_confirm_body",
 "settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle"
]
job_plurals = ["datetime_months_relative","datetime_years_relative"]

with open(target, 'r', encoding='utf-8') as f:
    content = f.read()

# Which string keys exist (count occurrences of name="key")
existing = {}
for k in job_strings:
    cnt = len(re.findall(r'<string\s+name="%s"' % re.escape(k), content))
    existing[k] = cnt

plural_exist = {}
for k in job_plurals:
    cnt = len(re.findall(r'<plurals\s+name="%s"' % re.escape(k), content))
    plural_exist[k] = cnt

# Extract current ar plural quantities to detect staleness (German leftover)
def extract_plural(name):
    m = re.search(r'<plurals\s+name="%s">(.*?)</plurals>' % re.escape(name), content, re.DOTALL)
    if not m:
        return None
    body = m.group(1)
    items = re.findall(r'<item\s+quantity="(\w+)">(.*?)</item>', body, re.DOTALL)
    return items

plural_current = {k: extract_plural(k) for k in job_plurals}

# Check resources tag count
res_close = content.count('</resources>')

print("=== STRING KEY EXISTENCE (count of name=) ===")
for k in job_strings:
    print(f"{existing[k]}  {k}")
print()
print("=== PLURALS EXISTENCE ===")
for k in job_plurals:
    print(f"{plural_exist[k]}  {k}")
print()
print("=== CURRENT AR PLURAL CONTENT ===")
for k in job_plurals:
    print(k, "->", plural_current[k])
print()
print("</resources> count:", res_close)
print("paywall_monthly_note exists:", existing.get("paywall_monthly_note"))
