# -*- coding: utf-8 -*-
# Post-verification for mr (FIN-051c): all keys present, none == DE, XML valid,
# format-args identical, no bare %, json_key_* still romanized (sample 3).
import os, re, json
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
TARGET = os.path.join(ROOT, 'app/src/main/res/values-mr/strings.xml')
JOBDIR = os.path.join(ROOT, '.android-shield/translation-jobs')

de = json.load(open(os.path.join(JOBDIR, 'de-reference.json'), encoding='utf-8'))
DES, DEP = de['strings'], de['plurals']

STR_KEYS = ['ai_prompt_custom_intro','ai_prompt_custom_rules','ai_prompt_entropy_intro','ai_prompt_goals_rules','ai_prompt_insight_attitude','ai_prompt_no_dates_rule','ai_prompt_rerank_system','churn_offer_feature_perspectives','consent_card3_title','consent_footer_link_impressum','consent_toggle_analytics_body','consent_toggle_analytics_title','consent_toggle_do_not_sell_body','onboarding_feature_secure','onboarding_premium_feature_noads','paywall_feature_noads','paywall_feature_profiles','paywall_from_per_day','paywall_headline_clarity_sub','paywall_monthly_note','privacy_gate_groq_body','privacy_gate_tts_body','privacy_gate_tts_title','profile_entropy_long','retro_benefit_weekly','settings_analytics_title','settings_delete_account_confirm_body','settings_delete_account_subtitle','settings_feedback_dialog_desc','settings_premium_feature_5_perspectives','settings_premium_feature_profiles','settings_report_ai_confirm_body','settings_revoke_confirm_body','settings_revoke_confirm_title','settings_revoke_subtitle']
PL_KEYS = ['datetime_months_relative','datetime_years_relative']

errors, warns = [], []

# 1) XML valid
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
except Exception as e:
    errors.append(f"XML PARSE FAIL: {e}")
    print(json.dumps({'PASS': False, 'errors': errors})); raise SystemExit(1)

strings = {el.get('name'): (el.text or '') for el in root.findall('string')}
plurals = {}
for pl in root.findall('plurals'):
    plurals[pl.get('name')] = {it.get('quantity'): (it.text or '') for it in pl.findall('item')}

fmt = re.compile(r'%\d+\$[sd]|%[sd]')
def args(s): return sorted(fmt.findall(s.replace('%%','')))
def bare_pct(s): return re.compile(r'%\d+\$[sd]|%[sd]|%%').sub('', s).count('%')

# 2) all keys present + not == DE + arg parity + no bare %
for k in STR_KEYS:
    if k not in strings:
        errors.append(f"MISSING string: {k}"); continue
    v = strings[k]
    deval = DES.get(k, '')
    if v.strip() == deval.strip() and deval.strip():
        errors.append(f"IDENTICAL TO DE: {k}")
    if args(v) != args(deval):
        errors.append(f"ARG MISMATCH {k}: mr={args(v)} de={args(deval)}")
    if bare_pct(v) != 0:
        errors.append(f"BARE % in {k}")

for k in PL_KEYS:
    if k not in plurals:
        errors.append(f"MISSING plural: {k}"); continue
    deforms = DEP.get(k, {})
    for q, t in plurals[k].items():
        if t.strip() == deforms.get(q, '').strip() and deforms.get(q):
            errors.append(f"PLURAL IDENTICAL TO DE: {k}/{q}")
        if args(t) != args(deforms.get(q, '')):
            errors.append(f"PLURAL ARG MISMATCH {k}/{q}")
        if bare_pct(t) != 0:
            errors.append(f"BARE % in plural {k}/{q}")

# 3) json_key_* sample 3 unchanged romanized (latin a-z only, no devanagari)
def is_roman(s): return bool(re.fullmatch(r'[a-z0-9_]+', s.strip()))
sample = ['json_key_overall_analysis','json_key_top_actions','json_key_categories']
for jk in sample:
    if jk in strings:
        if not is_roman(strings[jk]):
            errors.append(f"json_key NOT romanized anymore: {jk}={strings[jk]!r}")
    else:
        warns.append(f"json_key sample not found: {jk}")

# imprint consistency note
if strings.get('consent_footer_link_impressum') != strings.get('legal_title_impressum', '__none__'):
    warns.append(f"impressum link != legal_title_impressum (link={strings.get('consent_footer_link_impressum')!r}, title={strings.get('legal_title_impressum')!r})")

print(json.dumps({
    'PASS': len(errors) == 0,
    'errors': errors,
    'warnings': warns,
    'strings_verified': len([k for k in STR_KEYS if k in strings]),
    'plurals_verified': len([k for k in PL_KEYS if k in plurals]),
    'consent_footer_link_impressum': strings.get('consent_footer_link_impressum'),
}, ensure_ascii=False, indent=1))
