# -*- coding: utf-8 -*-
"""FIN-051c post-verification for th translation worker."""
import re
import json
import os
import xml.etree.ElementTree as ET

TARGET = os.path.join("app", "src", "main", "res", "values-th", "strings.xml")
DE_REF = os.path.join(".android-shield", "translation-jobs", "de-reference.json")
JOB = os.path.join(".android-shield", "translation-jobs", "job-plan.json")

STRING_KEYS = [
    "ai_prompt_custom_intro", "ai_prompt_custom_rules", "ai_prompt_entropy_intro",
    "ai_prompt_goals_rules", "ai_prompt_insight_attitude", "ai_prompt_no_dates_rule",
    "ai_prompt_rerank_system", "churn_offer_feature_perspectives", "consent_card3_title",
    "consent_toggle_analytics_body", "consent_toggle_analytics_title",
    "consent_toggle_do_not_sell_body", "onboarding_feature_secure",
    "onboarding_premium_feature_noads", "paywall_feature_noads", "paywall_feature_profiles",
    "paywall_from_per_day", "paywall_headline_clarity_sub", "paywall_monthly_note",
    "privacy_gate_groq_body", "privacy_gate_tts_body", "privacy_gate_tts_title",
    "profile_entropy_long", "retro_benefit_weekly", "settings_analytics_title",
    "settings_delete_account_confirm_body", "settings_delete_account_subtitle",
    "settings_feedback_dialog_desc", "settings_premium_feature_5_perspectives",
    "settings_premium_feature_profiles", "settings_report_ai_confirm_body",
    "settings_revoke_confirm_body", "settings_revoke_confirm_title",
    "settings_revoke_subtitle",
]
PLURAL_KEYS = ["datetime_months_relative", "datetime_years_relative"]

fmt_re = re.compile(r'%\d+\$[sd]|%[sd]|%%')
arg_re = re.compile(r'%\d+\$[sd]')

errors = []
warnings = []

# Load DE reference + parse target
de = json.load(open(DE_REF, encoding="utf-8"))
de_strings = de["strings"]
de_plurals = de["plurals"]
src = open(TARGET, encoding="utf-8").read()

# XML validity
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
except Exception as e:
    errors.append("XML parse FAIL: %s" % e)
    root = None

xml_strings = {}
xml_plurals = {}
if root is not None:
    for el in root.findall("string"):
        # inner text incl. nested -> use raw text via tostring fallback
        xml_strings[el.get("name")] = el.text if el.text is not None else ""
    for el in root.findall("plurals"):
        items = {}
        for it in el.findall("item"):
            items[it.get("quantity")] = it.text if it.text is not None else ""
        xml_plurals[el.get("name")] = items

# 1) Presence (count via raw grep-style) + duplicates
for k in STRING_KEYS:
    c = len(re.findall(r'<string name="%s"' % re.escape(k), src))
    if c == 0:
        errors.append("MISSING string key: %s" % k)
    elif c > 1:
        errors.append("DUPLICATE string key (%d): %s" % (c, k))
for k in PLURAL_KEYS:
    c = len(re.findall(r'<plurals name="%s"' % re.escape(k), src))
    if c == 0:
        errors.append("MISSING plural: %s" % k)
    elif c > 1:
        errors.append("DUPLICATE plural (%d): %s" % (c, k))

# 2) None identical to DE + Thai script present + format-arg parity + no naked %
def thai_ratio(s):
    th = sum(1 for ch in s if '฀' <= ch <= '๿')
    letters = sum(1 for ch in s if ch.isalpha())
    return th, letters

for k in STRING_KEYS:
    if k not in xml_strings:
        continue
    val = xml_strings[k] or ""
    de_val = de_strings.get(k, "")
    # de_val uses literal \\n (JSON) -> python has single backslash-n already as text "\n"? json.load turns \\n into \n (2 chars backslash n)
    if val.strip() == de_val.strip():
        errors.append("IDENTICAL to DE: %s" % k)
    # Thai script check (skip pure-placeholder/ascii structural? all should have thai)
    th, letters = thai_ratio(val)
    if th == 0:
        warnings.append("NO Thai chars in: %s" % k)
    # format-arg parity (positional %1$s etc.)
    de_args = sorted(arg_re.findall(de_val))
    th_args = sorted(arg_re.findall(val))
    if de_args != th_args:
        errors.append("FORMAT-ARG mismatch %s: de=%s th=%s" % (k, de_args, th_args))
    # naked % check
    naked = fmt_re.sub('', val).count('%')
    if naked != 0:
        errors.append("NAKED %% in %s (count=%d)" % (k, naked))
    # leftover escaped/raw double-quote check (file convention: none)
    if '"' in val:
        warnings.append("contains raw doublequote: %s" % k)

# 3) Plurals: th must be 'other' only, args parity
for k in PLURAL_KEYS:
    if k not in xml_plurals:
        continue
    qs = set(xml_plurals[k].keys())
    if qs != {"other"}:
        errors.append("PLURAL %s quantities != {other}: %s" % (k, qs))
    de_other = de_plurals[k].get("other", "")
    th_other = xml_plurals[k].get("other", "")
    de_args = sorted(arg_re.findall(de_other))
    th_args = sorted(arg_re.findall(th_other))
    if de_args != th_args:
        errors.append("PLURAL FORMAT-ARG mismatch %s: de=%s th=%s" % (k, de_args, th_args))
    if th_other.strip() == de_other.strip():
        errors.append("PLURAL IDENTICAL to DE: %s/other" % k)
    naked = fmt_re.sub('', th_other).count('%')
    if naked != 0:
        errors.append("PLURAL NAKED %% in %s" % k)
    th, _ = thai_ratio(th_other)
    if th == 0:
        warnings.append("PLURAL no Thai chars: %s" % k)

# 4) Legal/numeric spot checks (1:1 requirement)
legal_checks = {
    "settings_delete_account_subtitle": "Google",   # 'Google-Konto bleibt' notion -> must mention Google account staying
    "settings_revoke_subtitle": "356a",             # §356a present
    "consent_toggle_do_not_sell_body": "CCPA",      # California law; do_not_sell w/o year
}
for k, needle in legal_checks.items():
    v = xml_strings.get(k, "")
    if needle not in v:
        warnings.append("LEGAL spot-check: '%s' not found in %s" % (needle, k))
# do_not_sell must NOT contain a year like 2026
if "2026" in xml_strings.get("consent_toggle_do_not_sell_body", ""):
    errors.append("do_not_sell contains a YEAR (2026) — must be year-free")

print(json.dumps({
    "errors": errors,
    "warnings": warnings,
    "string_keys_checked": len(STRING_KEYS),
    "plural_keys_checked": len(PLURAL_KEYS),
    "ok": len(errors) == 0,
}, ensure_ascii=False, indent=2))
