# -*- coding: utf-8 -*-
"""Post-verification for tw-en (FIN-051c)."""
import os, re, json
import xml.etree.ElementTree as ET

TARGET = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-en/strings.xml')
DEREF = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/de-reference.json')

KEYS = [
 "ai_limits_dialog_body","ai_prompt_custom_intro","ai_prompt_custom_rules",
 "ai_prompt_entropy_intro","ai_prompt_goals_rules","ai_prompt_insight_attitude",
 "ai_prompt_no_dates_rule","ai_prompt_rerank_system","churn_offer_feature_perspectives",
 "consent_card3_title","consent_toggle_analytics_body","consent_toggle_analytics_title",
 "consent_toggle_do_not_sell_body","onboarding_feature_secure","onboarding_premium_feature_noads",
 "paywall_feature_noads","paywall_feature_profiles","paywall_from_per_day",
 "paywall_headline_clarity_sub","paywall_monthly_note","privacy_gate_groq_body",
 "privacy_gate_tts_body","privacy_gate_tts_title","profile_entropy_long","retro_benefit_weekly",
 "settings_analytics_title","settings_delete_account_confirm_body","settings_delete_account_subtitle",
 "settings_feedback_dialog_desc","settings_premium_feature_5_perspectives",
 "settings_premium_feature_profiles","settings_report_ai_confirm_body",
 "settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle",
]
PLURALS = ["datetime_months_relative","datetime_years_relative"]

problems = []

# 1) XML valid?
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
except Exception as e:
    print(json.dumps({"xml_valid": False, "error": str(e)}))
    raise SystemExit(1)

# Build maps
strings = {}
dup = {}
for el in root.findall('string'):
    nm = el.get('name')
    if nm in strings:
        dup[nm] = dup.get(nm, 1) + 1
    strings[nm] = ''.join(el.itertext())
plurals = {}
for el in root.findall('plurals'):
    nm = el.get('name')
    forms = {it.get('quantity'): ''.join(it.itertext()) for it in el.findall('item')}
    plurals[nm] = forms

# DE ref
de = json.load(open(DEREF, encoding='utf-8'))
de_s = de['strings']
de_p = de['plurals']

# 2) all keys present
missing = [k for k in KEYS if k not in strings]
if missing: problems.append({"missing_strings": missing})
missing_p = [p for p in PLURALS if p not in plurals]
if missing_p: problems.append({"missing_plurals": missing_p})

# 3) duplicates
if dup: problems.append({"duplicate_keys": dup})

# 4) none identical to DE; format args match; no bare %
fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
def args_of(s):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', s))
def bare_pct(s):
    return fmt.sub('', s).count('%')

for k in KEYS:
    if k not in strings: continue
    val = strings[k]
    # XML-parsed value has &amp; -> & and \\n stays as literal backslash-n (it's text)
    dval = de_s.get(k, '')
    # de-reference stores literal \n as \\n in JSON -> python loads as \n? No: JSON "\\n" -> backslash n
    if val == dval:
        problems.append({"identical_to_de": k})
    # format args: compare against DE
    if args_of(val) != args_of(dval):
        problems.append({"format_arg_mismatch": k, "en": args_of(val), "de": args_of(dval)})
    if bare_pct(val) != 0:
        problems.append({"bare_percent": k, "value_snippet": val[:60]})

# plurals format + not identical
for p in PLURALS:
    if p not in plurals: continue
    for q, t in plurals[p].items():
        dt = de_p.get(p, {}).get(q, '')
        if t == dt:
            problems.append({"plural_identical_to_de": p+"/"+q})
        if args_of(t) != args_of(dt):
            problems.append({"plural_format_mismatch": p+"/"+q, "en": args_of(t), "de": args_of(dt)})
        if bare_pct(t) != 0:
            problems.append({"plural_bare_percent": p+"/"+q})

# 5) ai_limits special: '150' must appear exactly 4 times; 600,30,101,151,50,5 present
ail = strings.get('ai_limits_dialog_body', '')
n150 = len(re.findall(r'\b150\b', ail))
checks_ail = {"150_count": n150, "needs": 4}
for num in ['600','30','101','151','50','5']:
    if num not in ail:
        problems.append({"ai_limits_missing_number": num})
if n150 != 4:
    problems.append({"ai_limits_150_count_wrong": n150})

# legal phrase checks
if 'your Google account' not in strings.get('settings_delete_account_confirm_body',''):
    problems.append({"legal_missing": "delete_account 'your Google account'"})
if '356a' not in strings.get('settings_revoke_subtitle',''):
    problems.append({"legal_missing": "revoke 356a"})

result = {
    "xml_valid": True,
    "strings_found": len([k for k in KEYS if k in strings]),
    "strings_expected": len(KEYS),
    "plurals_found": len([p for p in PLURALS if p in plurals]),
    "ai_limits_check": checks_ail,
    "problems": problems,
    "PASS": len(problems) == 0,
}
print(json.dumps(result, ensure_ascii=False, indent=1))
