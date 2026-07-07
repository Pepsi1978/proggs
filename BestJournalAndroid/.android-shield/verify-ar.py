# -*- coding: utf-8 -*-
import os, re, json
import xml.etree.ElementTree as ET

target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-ar/strings.xml')
de_ref_path = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/de-reference.json')

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

with open(de_ref_path, 'r', encoding='utf-8') as f:
    de = json.load(f)
de_strings = de["strings"]
de_plurals = de["plurals"]

fmt = re.compile(r'%\d+\$[sdf]|%[sdf]|%%')

def args_of(s):
    return sorted(fmt.findall(s or ""))

def naked_percent(s):
    return fmt.sub('', s or "").count('%')

errors = []

# Parse XML
try:
    tree = ET.parse(target)
    root = tree.getroot()
    xml_valid = True
except Exception as e:
    xml_valid = False
    errors.append(f"XML parse error: {e}")
    root = None

# Build maps from raw text (placeholders unescaped by ET so compare on raw too)
with open(target, 'r', encoding='utf-8') as f:
    content = f.read()

def get_string_raw(key):
    m = re.search(r'<string\s+name="%s"[^>]*>(.*?)</string>' % re.escape(key), content, re.DOTALL)
    return m.group(1) if m else None

all_present = True
none_identical = True
format_ok = True
no_naked = True

# 1) all strings present + 2) not identical to DE + 4) format args match + 5) no naked %
for k in job_strings:
    raw = get_string_raw(k)
    if raw is None:
        all_present = False
        errors.append(f"MISSING string: {k}")
        continue
    de_val = de_strings.get(k)
    if de_val is not None:
        # de-reference stores \\n etc as literal backslash sequences in JSON -> json.load gives single backslash-n?
        # Actually JSON "\\n" decodes to literal backslash+n. raw in xml also has literal backslash+n. compare directly.
        if raw == de_val:
            none_identical = False
            errors.append(f"IDENTICAL to DE: {k}")
        if args_of(raw) != args_of(de_val):
            format_ok = False
            errors.append(f"FORMAT ARG MISMATCH {k}: ar={args_of(raw)} de={args_of(de_val)}")
    if naked_percent(raw) != 0:
        no_naked = False
        errors.append(f"NAKED % in {k}: {raw[:60]}")

# Plurals present + not identical + arg sanity
for p in job_plurals:
    m = re.search(r'<plurals\s+name="%s">(.*?)</plurals>' % re.escape(p), content, re.DOTALL)
    if not m:
        all_present = False
        errors.append(f"MISSING plural: {p}")
        continue
    items = re.findall(r'<item\s+quantity="(\w+)">(.*?)</item>', m.group(1), re.DOTALL)
    if not items:
        errors.append(f"plural {p}: no items")
        continue
    # staleness: none of the AR items should equal the DE item text
    de_items = de_plurals.get(p, {})
    for q, t in items:
        if q in de_items and t == de_items[q]:
            none_identical = False
            errors.append(f"PLURAL {p} quantity={q} IDENTICAL to DE: {t}")
        if naked_percent(t) != 0:
            no_naked = False
            errors.append(f"NAKED % in plural {p}/{q}: {t}")
        # only allowed placeholder is %1$d (matches DE's placeholder), never a foreign one
        for a in args_of(t):
            if a not in ("%1$d", "%%"):
                format_ok = False
                errors.append(f"FOREIGN placeholder {a} in plural {p}/{q}")

print(json.dumps({
  "allPresent": all_present,
  "noneIdenticalToDe": none_identical,
  "xmlValid": xml_valid,
  "formatArgsOk": format_ok,
  "noNakedPercent": no_naked,
  "errorCount": len(errors),
}, ensure_ascii=False, indent=2))
if errors:
    print("\n--- ERRORS ---")
    for e in errors:
        print(e)
