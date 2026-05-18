import os
import re

keys = [
    # GEAENDERTE (17)
    'settings_premium_feature_5_perspectives',
    'churn_offer_feature_perspectives',
    'onboarding_perspectives_title',
    'paywall_consent_dialog_body',
    'paywall_exit_start_discount',
    'paywall_yearly_savings',
    'onboarding_try_premium',
    'paywall_subtitle',
    'churn_offer_subtitle',
    'ai_limits_dialog_body',
    'ai_limits_disclaimer',
    'settings_premium_feature_improve_desc',
    'settings_premium_feature_patterns_desc',
    'ai_banner_body',
    'paywall_headline_stress',
    'paywall_headline_stress_sub',
    'settings_premium_feature_dashboard_desc',
    # NEUE (14)
    'permission_rationale_microphone',
    'permission_rationale_camera',
    'permission_rationale_location',
    'permission_rationale_notifications',
    'consent_info_system_backup_title',
    'consent_info_system_backup_body',
    'settings_open_system_backup_settings_label',
    'settings_open_system_backup_settings_desc',
    'error_no_backup_settings_available',
    'consent_footer_link_datenschutz',
    'consent_footer_link_impressum',
    'consent_footer_link_agb',
    'ai_output_health_disclaimer',
    'ai_output_health_disclaimer_long',
]

locales = [
    'ar', 'bn', 'en', 'es', 'fr', 'gu', 'hi', 'in', 'it', 'ja',
    'kn', 'ko', 'ml', 'mr', 'nl', 'pl', 'pt-rBR', 'pt-rPT', 'ru',
    'ta', 'te', 'th', 'tr', 'uk', 'ur', 'zh-rCN', 'zh-rTW'
]

base = 'C:/Users/barwa/proggs/BestJournalAndroid/app/src/main/res'

results = {}
for locale in locales:
    path = os.path.join(base, f'values-{locale}', 'strings.xml')
    if not os.path.exists(path):
        results[locale] = {'found': 0, 'missing': list(keys)}
        continue
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    found = []
    missing = []
    for k in keys:
        pattern = r'name="' + re.escape(k) + r'"'
        if re.search(pattern, content):
            found.append(k)
        else:
            missing.append(k)
    results[locale] = {'found': len(found), 'missing': missing}

# Build markdown report
lines = []
lines.append('# Phase 4 W3 — 27-Sprachen Coverage-Audit')
lines.append('')
lines.append('| Locale | Gefunden | Fehlend | Status |')
lines.append('|--------|----------|---------|--------|')

total_complete = 0
gaps = []

for locale in locales:
    r = results[locale]
    found_count = r['found']
    missing = r['missing']
    missing_count = 31 - found_count
    if found_count == 31:
        status = 'OK'
        missing_str = '—'
        total_complete += 1
    else:
        status = f'LUECKE'
        missing_str = ', '.join(missing)
        gaps.append((locale, missing))
    lines.append(f'| {locale} | {found_count}/31 | {missing_str} | {status} |')

lines.append('')
lines.append('## Zusammenfassung')
lines.append('')
lines.append(f'- Vollstaendig abgedeckt: {total_complete}/27 Locales')
lines.append(f'- Locales mit Luecken: {len(gaps)}')
total_missing = sum(len(g[1]) for g in gaps)
lines.append(f'- Insgesamt fehlende Strings (alle Locales summiert): {total_missing}')
lines.append('')
if gaps:
    lines.append('### Liste der Luecken')
    lines.append('')
    for locale, missing in gaps:
        lines.append(f'**{locale}:** ' + ', '.join(missing))
        lines.append('')
else:
    lines.append('Keine Luecken gefunden — alle 27 Locales vollstaendig.')

report = '\n'.join(lines)
out_path = 'C:/Users/barwa/proggs/BestJournalAndroid/.android-shield/phase4-audit-w3.md'
os.makedirs(os.path.dirname(out_path), exist_ok=True)
with open(out_path, 'w', encoding='utf-8') as f:
    f.write(report)

print('DONE')
print(f'Vollstaendig: {total_complete}/27')
print(f'Locales mit Luecken: {len(gaps)}')
print(f'Fehlende Strings gesamt: {total_missing}')
if gaps:
    print('Luecken:')
    for locale, missing in gaps:
        print(f'  {locale}: {", ".join(missing)}')
