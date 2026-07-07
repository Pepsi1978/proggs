# -*- coding: utf-8 -*-
"""Post-verification for tw-es (FIN-051c)."""
import os, re, json, xml.etree.ElementTree as ET

F = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-es/strings.xml')
DE = json.load(open(os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/translation-jobs/de-reference.json'), encoding='utf-8'))

job_strings = ["ai_limits_dialog_body","ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro",
"ai_prompt_goals_rules","ai_prompt_insight_attitude","ai_prompt_no_dates_rule","ai_prompt_rerank_system",
"churn_offer_feature_perspectives","consent_card3_title","consent_toggle_analytics_body","consent_toggle_analytics_title",
"consent_toggle_do_not_sell_body","onboarding_feature_secure","onboarding_premium_feature_noads","paywall_feature_noads",
"paywall_feature_profiles","paywall_from_per_day","paywall_headline_clarity_sub","paywall_monthly_note",
"privacy_gate_groq_body","privacy_gate_tts_body","privacy_gate_tts_title","profile_entropy_long","retro_benefit_weekly",
"settings_analytics_title","settings_delete_account_confirm_body","settings_delete_account_subtitle",
"settings_feedback_dialog_desc","settings_premium_feature_5_perspectives","settings_premium_feature_profiles",
"settings_report_ai_confirm_body","settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle"]
job_plurals = ["datetime_months_relative","datetime_years_relative"]

errors = []
with open(F, 'r', encoding='utf-8') as fh:
    content = fh.read()

# 1) XML valid
try:
    ET.fromstring(content)
except Exception as e:
    errors.append('XML invalid: %s' % e)

# helper to extract raw value
def getval(key):
    m = re.search(r'<string name="%s"[^>]*>(.*?)</string>' % re.escape(key), content, re.S)
    return m.group(1) if m else None

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')

for k in job_strings:
    v = getval(k)
    if v is None:
        errors.append('MISSING string: %s' % k); continue
    de = DE['strings'].get(k, '')
    if v == de:
        errors.append('IDENTICAL to DE: %s' % k)
    # format args must match DE
    de_args = sorted(re.findall(r'%\d+\$[sd]|%[sd]', de))
    es_args = sorted(re.findall(r'%\d+\$[sd]|%[sd]', v))
    if de_args != es_args:
        errors.append('FMT-ARG mismatch %s: DE=%s ES=%s' % (k, de_args, es_args))
    # no bare %
    unguarded = fmt.sub('', v).count('%')
    if unguarded != 0:
        errors.append('BARE %% in %s (count=%d)' % (k, unguarded))
    # unescaped apostrophe (not preceded by backslash)
    if re.search(r"(?<!\\)'", v):
        errors.append('UNESCAPED apostrophe in %s' % k)

# plurals
for p in job_plurals:
    m = re.search(r'<plurals name="%s">(.*?)</plurals>' % re.escape(p), content, re.S)
    if not m:
        errors.append('MISSING plural: %s' % p); continue
    body = m.group(1)
    de_one = DE['plurals'][p]['one']; de_other = DE['plurals'][p]['other']
    if de_one in body or de_other in body:
        errors.append('PLURAL still DE: %s' % p)
    for itm in re.findall(r'<item quantity="\w+">(.*?)</item>', body, re.S):
        if '%1$d' not in itm:
            errors.append('PLURAL %s item missing %%1$d: %r' % (p, itm))
        u = fmt.sub('', itm).count('%')
        if u != 0:
            errors.append('PLURAL %s bare %%: %r' % (p, itm))

# ai_limits special: 150 must appear >=4 times (DE has 4)
ail = getval('ai_limits_dialog_body') or ''
if ail.count('150') < 4:
    errors.append('ai_limits_dialog_body: 150 appears %d times (<4)' % ail.count('150'))

# legal-faithfulness spot checks
if 'cuenta de Google se mantiene' not in (getval('settings_delete_account_subtitle') or ''):
    errors.append('delete_account_subtitle: missing "cuenta de Google se mantiene"')
if '356a' not in (getval('settings_revoke_subtitle') or ''):
    errors.append('revoke_subtitle: missing § 356a')

print('ERRORS:' if errors else 'ALL CHECKS PASSED')
for e in errors:
    print(' -', e)
print('SUMMARY: strings=%d plurals=%d' % (len(job_strings), len(job_plurals)))
