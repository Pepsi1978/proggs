# -*- coding: utf-8 -*-
import os, re
target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-ur/strings.xml')
with open(target, 'r', encoding='utf-8') as f:
    content = f.read()

keys = [
 "ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro","ai_prompt_goals_rules",
 "ai_prompt_insight_attitude","ai_prompt_no_dates_rule","ai_prompt_rerank_system",
 "churn_offer_feature_perspectives","consent_card3_title","consent_toggle_analytics_body",
 "consent_toggle_analytics_title","consent_toggle_do_not_sell_body","onboarding_feature_secure",
 "onboarding_premium_feature_noads","paywall_feature_noads","paywall_feature_profiles",
 "paywall_from_per_day","paywall_headline_clarity_sub","paywall_monthly_note",
 "privacy_gate_gemini_body","privacy_gate_groq_body","privacy_gate_tts_body","privacy_gate_tts_title",
 "profile_entropy_long","retro_benefit_weekly","settings_analytics_title",
 "settings_delete_account_confirm_body","settings_delete_account_subtitle","settings_feedback_dialog_desc",
 "settings_premium_feature_5_perspectives","settings_premium_feature_profiles","settings_report_ai_confirm_body",
 "settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle",
]
plurals = ["datetime_months_relative","datetime_years_relative"]

print("FILE SIZE:", len(content))
print("=== STRING KEY PRESENCE ===")
present = []
missing = []
for k in keys:
    c = content.count(f'name="{k}"')
    if c == 0:
        missing.append(k)
    elif c > 1:
        print(f"DUP({c}): {k}")
        present.append(k)
    else:
        present.append(k)
print("present:", len(present), "missing:", missing)
print("=== PLURALS PRESENCE ===")
for p in plurals:
    c = content.count(f'name="{p}"')
    print(p, "->", c)

print("=== CURRENT privacy_gate_gemini_body (audit flag check) ===")
m = re.search(r'<string name="privacy_gate_gemini_body">(.*?)</string>', content, re.DOTALL)
if m:
    val = m.group(1)
    print("HAS 'DSGVOآرٹ':", 'DSGVOآرٹ' in val or 'DSGVOآرٹ.9' in val)
    print("HAS 'Art. 9' style:", 'آرٹ.9' in val, "| 'GDPR':", 'GDPR' in val, "| 'DSGVO':", 'DSGVO' in val)
    print("LEN:", len(val))
print("=== sample existing string (RLM usage check) ===")
# show how brand name handled - find a string with Best Journal
for k in ["onboarding_feature_secure","privacy_gate_gemini_body"]:
    m = re.search(rf'<string name="{k}">(.*?)</string>', content, re.DOTALL)
    if m:
        v = m.group(1)
        print(f"[{k}] RLM(\\u200f) count: {v.count(chr(0x200f))} | LRM(\\u200e): {v.count(chr(0x200e))}")
        print(f"   first120: {repr(v[:120])}")
