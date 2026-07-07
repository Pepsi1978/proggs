#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Wendet die sinnvollen MEDIUM/LOW DE-Fixes an. Jede Ersetzung muss exakt 1x matchen."""
import os, sys, xml.etree.ElementTree as ET

path = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values/strings.xml')
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# (alt, neu) — exakte String-Ersetzungen
fixes = [
    # B6 legend_insight_deep: verborgene -> wiederkehrende
    ('Tiefgehend, Verborgene Muster und Überzeugungen',
     'Tiefgehend, wiederkehrende Muster und Überzeugungen'),
    # B4 profile_insight_desc
    ('Deckt verborgene Denk- und Gefühlsmuster auf',
     'Macht wiederkehrende Denk- und Gefühlsmuster sichtbar'),
    # A6 profile_entropy_long: "sofort helfen" -> "im Alltag helfen"
    ('• Tipps, die dir sofort helfen können',
     '• Tipps, die dir im Alltag helfen können'),
    # B5 dashboard_weekly_review_upsell_body: Em-Dash + "was dich wirklich bewegt"
    ('Mit Premium siehst du die volle Analyse — erkenne Muster, entdecke Einsichten und verstehe, was dich wirklich bewegt.',
     'Mit Premium siehst du die volle Analyse: erkenne Muster, entdecke Einsichten und verstehe, was dich beschäftigt.'),
    # B10 retro_monthly_p3
    ('Zeigt Veränderungen die dir im Alltag nicht auffallen',
     'Zeigt Entwicklungen aus deinen Einträgen'),
    # B11 onboarding_how_step4_desc
    ('Das KI-Dashboard zeigt dir Zusammenhänge, die dir im Alltag verborgen bleiben.',
     'Das KI-Dashboard zeigt dir Zusammenhänge aus deinen Einträgen.'),
    # B9 onboarding_free_trial: "unverbindlich" entschaerfen
    ('<string name="onboarding_free_trial">Kostenlos und unverbindlich</string>',
     '<string name="onboarding_free_trial">Kostenlos, jederzeit kündbar</string>'),
    # B17 retro_yearly_p2: "erkennt" -> "fasst zusammen"
    ('Die KI erkennt die großen Themen deines Jahres',
     'Die KI fasst die großen Themen deines Jahres zusammen'),
    # B8 paywall_free_note: unvollstaendiger Satz (Bug)
    ('<string name="paywall_free_note">Die App funktioniert auch ohne Abo,</string>',
     '<string name="paywall_free_note">Die App funktioniert auch ohne Abo weiter.</string>'),
    # B16 paywall_most_popular: Superlativ ohne Beleg
    ('<string name="paywall_most_popular">Beliebteste Wahl</string>',
     '<string name="paywall_most_popular">Empfohlen</string>'),
    # C8 settings_delete_account_force_local: "vorerst" -> "bleibt erhalten"
    ('Nur lokal löschen (Drive-Backup bleibt vorerst)',
     'Nur lokal löschen (Drive-Backup bleibt erhalten)'),
    # D13 settings_premium_cancelled_desc: "jederzeit" raus
    ('Du kannst die Kündigung jederzeit über Google Play rückgängig machen.',
     'Du kannst die Kündigung über Google Play rückgängig machen.'),
    # D7 onboarding_try_premium: Em-Dash -> Komma
    ('8 Tage gratis — danach ab 3,99 €/Monat',
     '8 Tage gratis, danach ab 3,99 €/Monat'),
    # D6 paywall_yearly_note: Auto-Verlaengerung ergaenzen
    ('Danach %1$s pro Jahr\\nIn der Testphase jederzeit kündbar',
     'Danach %1$s pro Jahr, verlängert sich automatisch\\nIn der Testphase jederzeit kündbar'),
    # B13 churn_exclusive_offer: "Exklusiv" -> "fuer dich"
    ('<string name="churn_exclusive_offer">Exklusives Angebot</string>',
     '<string name="churn_exclusive_offer">Angebot für dich</string>'),
]

errors = []
for old, new in fixes:
    n = content.count(old)
    if n != 1:
        errors.append(f'  MATCH={n} (erwartet 1): {old[:60]}')
        continue
    content = content.replace(old, new)

if errors:
    print('FEHLER — diese Ersetzungen matchten nicht eindeutig:')
    print('\n'.join(errors))
    sys.exit(1)

tmp = path + '.tmp'
with open(tmp, 'w', encoding='utf-8') as f:
    f.write(content)
ET.parse(tmp)  # XML-Validierung vor dem Ersetzen
os.replace(tmp, path)
print(f'{len(fixes)} MEDIUM/LOW DE-Fixes angewendet. XML valide.')
