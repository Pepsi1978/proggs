# -*- coding: utf-8 -*-
"""Post-verification for ml (FIN-051c): keys present, none == DE, XML valid, format-args match, no bare %."""
import os, re, json
import xml.etree.ElementTree as ET

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(APP, 'app/src/main/res/values-ml/strings.xml')
DE = os.path.join(APP, '.android-shield/translation-jobs/de-reference.json')

EXPECT_STRINGS = [
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
 "settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle",
]
EXPECT_PLURALS = ["datetime_months_relative","datetime_years_relative"]

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
argpat = re.compile(r'%\d+\$[sd]|%[sd]')

problems = []

# Load DE
with open(DE, 'r', encoding='utf-8') as f:
    de = json.load(f)
de_strings = de['strings']
de_plurals = de['plurals']

# Parse XML — proves validity.
tree = ET.parse(TARGET)
root = tree.getroot()

xml_strings = {}
for el in root.findall('string'):
    xml_strings[el.get('name')] = ''.join(el.itertext())
xml_plurals = {}
for pl in root.findall('plurals'):
    forms = {it.get('quantity'): ''.join(it.itertext()) for it in pl.findall('item')}
    xml_plurals[pl.get('name')] = forms

# 1. all keys present
for k in EXPECT_STRINGS:
    if k not in xml_strings:
        problems.append(f"MISSING string {k}")
for k in EXPECT_PLURALS:
    if k not in xml_plurals:
        problems.append(f"MISSING plural {k}")

def norm_de(s):
    # de-reference uses literal \\n; the parsed XML keeps \n literal too (we never unescaped).
    return s

# 2. none identical to DE  +  3. format-args identical  +  4. no bare %
for k in EXPECT_STRINGS:
    if k not in xml_strings:
        continue
    val = xml_strings[k]
    de_val = de_strings.get(k, '')
    if val.strip() == de_val.strip():
        problems.append(f"IDENTICAL-TO-DE {k}")
    # bare % check
    unguarded = fmt.sub('', val).count('%')
    if unguarded != 0:
        problems.append(f"BARE-% {k}: {unguarded}")
    # format args identical (multiset)
    de_args = sorted(argpat.findall(de_val))
    ml_args = sorted(argpat.findall(val))
    if de_args != ml_args:
        problems.append(f"ARG-MISMATCH {k}: de={de_args} ml={ml_args}")

# plurals: args + bare %
for k in EXPECT_PLURALS:
    if k not in xml_plurals:
        continue
    for qty, val in xml_plurals[k].items():
        de_form = de_plurals[k].get(qty, de_plurals[k].get('other', ''))
        unguarded = fmt.sub('', val).count('%')
        if unguarded != 0:
            problems.append(f"BARE-% {k}/{qty}: {unguarded}")
        de_args = sorted(argpat.findall(de_form))
        ml_args = sorted(argpat.findall(val))
        if de_args != ml_args:
            problems.append(f"ARG-MISMATCH {k}/{qty}: de={de_args} ml={ml_args}")
        if val.strip() == de_form.strip():
            problems.append(f"IDENTICAL-TO-DE {k}/{qty}")

# 5. Malayalam numerals must NOT appear (൦-൯ = U+0D66..U+0D6F)
mal_num = re.compile('[൦-൯]')
for k in EXPECT_STRINGS:
    if k in xml_strings and mal_num.search(xml_strings[k]):
        problems.append(f"MALAYALAM-NUMERAL {k}")

if problems:
    print("VERIFY: FAIL")
    for p in problems:
        print("  -", p)
else:
    print("VERIFY: PASS")
    print(f"  strings checked: {len(EXPECT_STRINGS)}, plurals: {len(EXPECT_PLURALS)}")
    print(f"  XML valid: yes (ElementTree parsed {len(xml_strings)} strings, {len(xml_plurals)} plurals total)")
