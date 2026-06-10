# -*- coding: utf-8 -*-
"""POST-VERIFICATION (FIN-051c) for lang 'in'.
Checks: all keys present, none identical to DE, XML valid, format-args identical, no bare %."""
import re, sys, io, json
import xml.etree.ElementTree as ET

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
TARGET = "app/src/main/res/values-in/strings.xml"
DEREF = ".android-shield/translation-jobs/de-reference.json"

with open(DEREF, 'r', encoding='utf-8') as f:
    deref = json.load(f)
de_strings = deref["strings"]
de_plurals = deref["plurals"]

KEYS = [
"ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro","ai_prompt_goals_rules",
"ai_prompt_insight_attitude","ai_prompt_no_dates_rule","ai_prompt_rerank_actions_header",
"ai_prompt_rerank_entries_header","ai_prompt_rerank_system","ai_prompt_rerank_user_focus_header",
"churn_offer_feature_perspectives","consent_card3_title","consent_toggle_analytics_body",
"consent_toggle_analytics_title","consent_toggle_do_not_sell_body","onboarding_feature_secure",
"onboarding_premium_feature_noads","paywall_feature_noads","paywall_feature_profiles",
"paywall_from_per_day","paywall_headline_clarity_sub","paywall_monthly_note","privacy_gate_groq_body",
"privacy_gate_tts_body","privacy_gate_tts_title","profile_entropy_long","retro_benefit_weekly",
"settings_analytics_title","settings_delete_account_confirm_body","settings_delete_account_subtitle",
"settings_feedback_dialog_desc","settings_premium_feature_5_perspectives","settings_premium_feature_profiles",
"settings_report_ai_confirm_body","settings_revoke_confirm_body","settings_revoke_confirm_title",
"settings_revoke_subtitle",
]
PLURAL_KEYS = ["datetime_months_relative","datetime_years_relative"]

errors = []
warns = []

# 1) XML valid
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
    print("XML: valid")
except Exception as e:
    errors.append(f"XML PARSE ERROR: {e}")
    print("XML: INVALID", e)
    print(json.dumps({"ok": False, "errors": errors}))
    sys.exit(1)

# Build maps from parsed XML
str_map = {}
plural_map = {}
for el in root:
    if el.tag == "string":
        nm = el.get("name")
        str_map[nm] = "".join(el.itertext())
    elif el.tag == "plurals":
        nm = el.get("name")
        items = {}
        for it in el:
            items[it.get("quantity")] = "".join(it.itertext())
        plural_map[nm] = items

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')

def fmt_args(s):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', s))

def bare_percent(s):
    return fmt.sub('', s).count('%')

# 2) all string keys present + not identical to DE + format-args identical + no bare %
present = 0
identical = []
for k in KEYS:
    if k not in str_map:
        errors.append(f"MISSING key: {k}")
        continue
    present += 1
    val = str_map[k]
    # de source: stored with literal \n as backslash-n; ET gives us the actual rendered text
    de_raw = de_strings.get(k, "")
    # Compare on the ACTUAL stored text. de_raw has escaped \\n; our file stores literal \n too.
    # ET resolves entities but \n in xml text is literal backslash-n (Android escape) -> stays.
    if val == de_raw:
        identical.append(k)
    # format-arg check vs DE (placeholders must match exactly)
    a_de = fmt_args(de_raw)
    a_tr = fmt_args(val)
    if a_de != a_tr:
        errors.append(f"FORMAT-ARG mismatch {k}: DE={a_de} TR={a_tr}")
    # bare percent check
    bp = bare_percent(val)
    if bp != 0:
        errors.append(f"BARE % in {k}: {bp}")

if identical:
    errors.append(f"IDENTICAL to DE: {identical}")

# 3) plurals present, format-arg, not identical
for k in PLURAL_KEYS:
    if k not in plural_map:
        errors.append(f"MISSING plural: {k}")
        continue
    items = plural_map[k]
    if "other" not in items:
        errors.append(f"plural {k} missing 'other'")
    de_other = de_plurals.get(k, {}).get("other", "")
    for q, txt in items.items():
        a_de = fmt_args(de_plurals.get(k, {}).get(q, de_other))
        a_tr = fmt_args(txt)
        if a_tr != ['%1$d']:
            errors.append(f"plural {k}/{q} format-arg unexpected: {a_tr}")
        if txt == de_other:
            errors.append(f"plural {k}/{q} identical to DE")
        if bare_percent(txt) != 0:
            errors.append(f"plural {k}/{q} bare %")

# 4) no duplicate keys (grep count)
import subprocess
for k in KEYS + PLURAL_KEYS:
    tag = "string" if k in KEYS else "plurals"
    cnt = subprocess.run(['grep', '-c', f'<{tag} name="{k}"', TARGET],
                         capture_output=True, text=True).stdout.strip()
    if int(cnt or '0') != 1:
        errors.append(f"DUPLICATE/MISSING {k}: count={cnt}")

print(f"KEYS present: {present}/{len(KEYS)}")
print(f"PLURALS present: {sum(1 for k in PLURAL_KEYS if k in plural_map)}/{len(PLURAL_KEYS)}")
print("ERRORS:", len(errors))
for e in errors:
    print("  -", e)
print("RESULT:", "PASS" if not errors else "FAIL")
print(json.dumps({"ok": not errors, "keysPresent": present, "pluralsPresent": sum(1 for k in PLURAL_KEYS if k in plural_map), "errors": errors}))
