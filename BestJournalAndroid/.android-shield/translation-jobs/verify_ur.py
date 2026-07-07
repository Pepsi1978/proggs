# -*- coding: utf-8 -*-
"""FIN-051c post-verification for Urdu (ur)."""
import os, re, json
import xml.etree.ElementTree as ET

target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-ur/strings.xml')
deref = json.load(open(os.path.expanduser(
    '~/proggs/BestJournalAndroid/.android-shield/translation-jobs/de-reference.json'), encoding='utf-8'))

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

errors = []
warns = []

# 1. XML valid
try:
    tree = ET.parse(target)
    root = tree.getroot()
    print("XML: valid")
except Exception as e:
    errors.append(f"XML INVALID: {e}")
    print("XML INVALID:", e)
    raise SystemExit(1)

with open(target, encoding='utf-8') as f:
    raw = f.read()

# build value map from parsed XML
strmap = {}
for el in root.findall('string'):
    strmap[el.get('name')] = ''.join(el.itertext())

# 2. all keys present + not identical to DE + format args match
def fmt_args(s):
    return sorted(re.findall(r'%\d+\$[sd]|%[sd]', s))

for k in keys:
    if k not in strmap:
        errors.append(f"MISSING key: {k}")
        continue
    val = strmap[k]
    de = deref['strings'][k].replace('\\n', '\n')  # DE source uses literal \n
    if val.strip() == de.strip():
        errors.append(f"IDENTICAL to DE: {k}")
    # format args (compare the raw XML text incl escapes for placeholders)
    de_args = fmt_args(deref['strings'][k])
    ur_args = fmt_args(val)
    if de_args != ur_args:
        errors.append(f"FORMAT ARG MISMATCH {k}: DE={de_args} UR={ur_args}")

# 3. plurals present + not German + args ok
for p in plurals:
    pl = root.find(f"plurals[@name='{p}']")
    if pl is None:
        errors.append(f"MISSING plural: {p}")
        continue
    for it in pl.findall('item'):
        t = ''.join(it.itertext())
        if 'vor ' in t or 'Monat' in t or 'Jahr' in t:
            errors.append(f"GERMAN STALE in plural {p}/{it.get('quantity')}: {t!r}")
        if fmt_args(t) != ['%1$d']:
            errors.append(f"plural arg bad {p}/{it.get('quantity')}: {fmt_args(t)}")

# 4. naked % check (any % not part of %1$s/%1$d/%s/%d)
for k in keys + plurals:
    if k in strmap:
        v = strmap[k]
    else:
        pl = root.find(f"plurals[@name='{k}']")
        v = ' '.join(''.join(i.itertext()) for i in pl.findall('item')) if pl is not None else ''
    # remove valid format specs, then look for stray %
    stripped = re.sub(r'%\d+\$[sd]|%[sd]|%%', '', v)
    if '%' in stripped:
        errors.append(f"NAKED %% in {k}: {v!r}")

# 5. DSGVO Art. 9 with space in gemini body; no Hindi-style 'DSGVOآرٹ'
g = strmap.get('privacy_gate_gemini_body', '')
if 'DSGVO Art. 9' not in g:
    errors.append("gemini body: 'DSGVO Art. 9' (with space) NOT found")
if 'DSGVOآرٹ' in g or 'آرٹ.9' in g or 'آرٹ. 9' in g:
    errors.append("gemini body: leftover Urdu-script 'آرٹ' for Art.9")

# 6. legal-text spot checks
da = strmap.get('settings_delete_account_confirm_body', '')
if 'Google اکاؤنٹ بذات خود برقرار رہتا ہے' not in da:
    warns.append("delete_account: exact phrase 'Google اکاؤنٹ بذات خود برقرار رہتا ہے' not found verbatim")
rv = strmap.get('settings_revoke_subtitle', '')
if '356a' not in rv:
    errors.append("revoke_subtitle: §356a missing")
if '356a BGB' not in rv:
    warns.append("revoke_subtitle: 'BGB' label check")
dns = strmap.get('consent_toggle_do_not_sell_body', '')
if re.search(r'\b(2024|2025|2026)\b', dns):
    errors.append(f"do_not_sell: contains a year (should not): {re.findall(r'(20[0-9][0-9])', dns)}")

print("\n=== RESULT ===")
print("strings checked:", len(keys), "plurals:", len(plurals))
if errors:
    print("ERRORS (%d):" % len(errors))
    for e in errors:
        print("  ✗", e)
else:
    print("✓ NO ERRORS")
if warns:
    print("WARNINGS (%d):" % len(warns))
    for w in warns:
        print("  !", w)
print("VERDICT:", "PASS" if not errors else "FAIL")
