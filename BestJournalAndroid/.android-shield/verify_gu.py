# -*- coding: utf-8 -*-
"""Post-verification for gu (FIN-051c + corruption check)."""
import os, re, json
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-gu/strings.xml')
DEREF = os.path.join(ROOT, '.android-shield/translation-jobs/de-reference.json')

with open(DEREF, 'r', encoding='utf-8') as f:
    deref = json.load(f)
de_strings = deref['strings']
de_plurals = deref['plurals']

KEYS = ["ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro",
 "ai_prompt_goals_rules","ai_prompt_insight_attitude","ai_prompt_no_dates_rule",
 "ai_prompt_rerank_system","churn_offer_feature_perspectives","consent_card3_title",
 "consent_toggle_analytics_body","consent_toggle_analytics_title","consent_toggle_do_not_sell_body",
 "onboarding_feature_secure","onboarding_premium_feature_noads","paywall_feature_noads",
 "paywall_feature_profiles","paywall_from_per_day","paywall_headline_clarity_sub",
 "paywall_monthly_note","privacy_gate_groq_body","privacy_gate_tts_body","privacy_gate_tts_title",
 "profile_entropy_long","retro_benefit_weekly","settings_analytics_title",
 "settings_delete_account_confirm_body","settings_delete_account_subtitle",
 "settings_feedback_dialog_desc","settings_premium_feature_5_perspectives",
 "settings_premium_feature_profiles","settings_report_ai_confirm_body",
 "settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle"]
PLURAL_KEYS = ["datetime_months_relative","datetime_years_relative"]

issues = []

# 1. XML validity
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
    xml_valid = True
except Exception as e:
    xml_valid = False
    issues.append("XML_INVALID: %s" % e)
    print(json.dumps({"pass": False, "issues": issues}, ensure_ascii=False)); raise SystemExit

# Build maps from parsed XML
str_map = {}
plr_map = {}
for el in root:
    if el.tag == 'string' and el.get('name'):
        str_map[el.get('name')] = ''.join(el.itertext())
    elif el.tag == 'plurals' and el.get('name'):
        plr_map[el.get('name')] = {it.get('quantity'): ''.join(it.itertext()) for it in el}

def fmt_args(s):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', s))

def bare_percent(s):
    return re.sub(r'%\d+\$[sd]|%[sd]|%%', '', s).count('%')

# Gujarati range U+0A80-U+0AFF ; corruption: U+FFFD, Geometric Shapes U+25A0-25FF, Specials
guj = re.compile(r'[઀-૿]')
bad = re.compile(r'[�■-◿⬀-⯿]')

# 2. all string keys present, not identical to DE, args match, no bare %, script ok, no corruption
for k in KEYS:
    if k not in str_map:
        issues.append("MISSING_KEY: %s" % k); continue
    v = str_map[k]
    de = de_strings[k].replace('\\n', '\n').replace('\\"', '"').replace("\\'", "'")
    # XML-parsed value has real newlines/quotes; de_strings has escaped — normalize de for compare
    if v.strip() == de.strip():
        issues.append("IDENTICAL_TO_DE: %s" % k)
    if fmt_args(v) != fmt_args(de):
        issues.append("FMT_MISMATCH: %s  got=%s de=%s" % (k, fmt_args(v), fmt_args(de)))
    if bare_percent(v) != 0:
        issues.append("BARE_PERCENT: %s" % k)
    if not guj.search(v):
        issues.append("NO_GUJARATI_SCRIPT: %s" % k)
    if bad.search(v):
        issues.append("CORRUPTION_CHAR: %s" % k)

# 3. plurals present, replaced (not German), args match
for pk in PLURAL_KEYS:
    if pk not in plr_map:
        issues.append("MISSING_PLURAL: %s" % pk); continue
    for q, t in plr_map[pk].items():
        de_t = de_plurals[pk].get(q, '')
        de_tn = de_t.replace('\\n','\n')
        if t.strip() == de_tn.strip() and de_tn.strip():
            issues.append("PLURAL_IDENTICAL_TO_DE: %s/%s" % (pk, q))
        if fmt_args(t) != fmt_args(de_tn):
            issues.append("PLURAL_FMT_MISMATCH: %s/%s" % (pk, q))
        if not guj.search(t):
            issues.append("PLURAL_NO_GUJARATI: %s/%s" % (pk, q))
        if bad.search(t):
            issues.append("PLURAL_CORRUPTION: %s/%s" % (pk, q))

print(json.dumps({
    "pass": len(issues) == 0,
    "xml_valid": xml_valid,
    "string_keys_found": sum(1 for k in KEYS if k in str_map),
    "string_keys_expected": len(KEYS),
    "plurals_found": sum(1 for k in PLURAL_KEYS if k in plr_map),
    "issues": issues
}, ensure_ascii=False, indent=1))
