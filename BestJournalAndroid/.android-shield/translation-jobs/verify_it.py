# -*- coding: utf-8 -*-
import re, json, sys
import xml.etree.ElementTree as ET

TF_WIN = r'C:\Users\barwa\proggs\BestJournalAndroid\app\src\main\res\values-it\strings.xml'
DEREF = r'C:\Users\barwa\proggs\BestJournalAndroid\.android-shield\translation-jobs\de-reference.json'

STRING_KEYS = [
 "ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro","ai_prompt_goals_rules",
 "ai_prompt_insight_attitude","ai_prompt_no_dates_rule","ai_prompt_rerank_system","churn_offer_feature_perspectives",
 "consent_card3_title","consent_toggle_analytics_body","consent_toggle_analytics_title","consent_toggle_do_not_sell_body",
 "onboarding_feature_secure","onboarding_premium_feature_noads","paywall_feature_noads","paywall_feature_profiles",
 "paywall_from_per_day","paywall_headline_clarity_sub","paywall_monthly_note","privacy_gate_groq_body",
 "privacy_gate_tts_body","privacy_gate_tts_title","profile_entropy_long","retro_benefit_weekly",
 "settings_analytics_title","settings_delete_account_confirm_body","settings_delete_account_subtitle",
 "settings_feedback_dialog_desc","settings_premium_feature_5_perspectives","settings_premium_feature_profiles",
 "settings_report_ai_confirm_body","settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle",
]
PLURAL_KEYS = ["datetime_months_relative","datetime_years_relative"]

with open(DEREF, 'r', encoding='utf-8') as f:
    de = json.load(f)
de_strings = de['strings']

problems = []

# 1. XML valid
try:
    tree = ET.parse(TF_WIN)
    root = tree.getroot()
except Exception as e:
    print("XML-INVALID:", e); sys.exit(1)

# build maps from parsed XML
xml_strings = {}
for el in root.findall('string'):
    name = el.get('name')
    # full inner text incl. escapes: re-read raw for format-arg check
    xml_strings[name] = el.text if el.text is not None else ''
xml_plurals = {}
for el in root.findall('plurals'):
    name = el.get('name')
    items = {it.get('quantity'): (it.text or '') for it in el.findall('item')}
    xml_plurals[name] = items

# raw content for format-arg + naked-% checks (escapes preserved)
with open(TF_WIN, 'r', encoding='utf-8') as f:
    raw = f.read()

def raw_value(key):
    m = re.search(r'<string name="'+re.escape(key)+r'"[^>]*>(.*?)</string>', raw, re.DOTALL)
    return m.group(1) if m else None

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
def fmt_args(s):
    return sorted(re.findall(r'%\d+\$[sd]', s))

# 2. all keys present + not identical to DE + format-args match + no naked %
for k in STRING_KEYS:
    if k not in xml_strings:
        problems.append(f"MISSING:{k}"); continue
    rv = raw_value(k)
    if rv is None:
        problems.append(f"RAW-MISS:{k}"); continue
    de_raw = de_strings.get(k, '')
    # de-reference stores JSON-escaped \\n; normalize for compare: both as literal-with-\n
    de_norm = de_raw.replace('\\\\', '\\')  # \\n -> \n
    if rv.strip() == de_norm.strip():
        problems.append(f"IDENTICAL-DE:{k}")
    # format args
    if fmt_args(rv) != fmt_args(de_norm):
        problems.append(f"FMTARG-MISMATCH:{k} it={fmt_args(rv)} de={fmt_args(de_norm)}")
    # naked %
    naked = fmt.sub('', rv).count('%')
    if naked != 0:
        problems.append(f"NAKED-%:{k} count={naked}")

# 3. plurals present + not identical + args
for p in PLURAL_KEYS:
    if p not in xml_plurals:
        problems.append(f"PLURAL-MISSING:{p}"); continue
    de_items = de['plurals'].get(p, {})
    for qty, txt in xml_plurals[p].items():
        de_txt = de_items.get(qty, '').replace('\\\\','\\')
        if txt.strip() == de_txt.strip() and de_txt.strip():
            problems.append(f"PLURAL-IDENTICAL-DE:{p}/{qty}")
        if fmt_args(txt) != fmt_args(de_txt):
            problems.append(f"PLURAL-FMT:{p}/{qty} it={fmt_args(txt)} de={fmt_args(de_txt)}")
        if fmt.sub('', txt).count('%') != 0:
            problems.append(f"PLURAL-NAKED-%:{p}/{qty}")

# legal spot-checks
checks = {
 "settings_delete_account_subtitle": "account Google rimane",
 "settings_delete_account_confirm_body": "account Google rimane",
 "settings_revoke_subtitle": "356a",
}
for k, needle in checks.items():
    rv = raw_value(k) or ''
    if needle not in rv:
        problems.append(f"LEGAL-MISS:{k} expected '{needle}'")

# do_not_sell must NOT contain a year
rv = raw_value('consent_toggle_do_not_sell_body') or ''
if re.search(r'\b(19|20)\d\d\b', rv):
    problems.append("DO-NOT-SELL-HAS-YEAR")

print("STRING_KEYS_PRESENT:", sum(1 for k in STRING_KEYS if k in xml_strings), "/", len(STRING_KEYS))
print("PLURALS_PRESENT:", sum(1 for p in PLURAL_KEYS if p in xml_plurals), "/", len(PLURAL_KEYS))
print("PROBLEMS:", len(problems))
for p in problems:
    print("  -", p)
print("RESULT:", "PASS" if not problems else "FAIL")
