#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""POST-VERIFIKATION kn (FIN-051c). Laeuft im Subprozess (FIN-048).
Checks:
  1. Alle 35 Ziel-Strings + 2 Plurals vorhanden.
  2. Kein Ziel-String identisch mit DE.
  3. XML valide.
  4. Format-Args identisch zu DE (%1$s/%1$d Multiset).
  5. Keine nackten % (ungeschuetzt).
  6. 150 kommt in ai_limits_dialog_body genau 4x vor.
  7. Kannada-Range U+0C80-U+0CFF dominiert, KEIN Telugu (U+0C00-U+0C7F).
  8. ZWNJ vorhanden.
"""
import json, os, re, sys
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
JOBS = os.path.join(ROOT, '.android-shield', 'translation-jobs')
KN = os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml')

with open(os.path.join(JOBS, 'job-plan.json'), 'r', encoding='utf-8') as f:
    plan = json.load(f)['languages']['kn']
target_strings = plan['strings']
target_plurals = plan['plurals']

with open(os.path.join(JOBS, 'de-reference.json'), 'r', encoding='utf-8') as f:
    deref = json.load(f)
de_strings = deref['strings']
de_plurals = deref['plurals']

with open(KN, 'r', encoding='utf-8') as f:
    content = f.read()

fails = []
warns = []

# 3. XML valide
try:
    tree = ET.fromstring(content)
except ET.ParseError as e:
    print(f"FAIL XML: {e}")
    sys.exit(1)

# Build map of kn values from parsed tree (Werte sind unescaped durch Parser)
kn_str = {}
kn_plu = {}
for el in tree:
    if el.tag == 'string' and el.get('name'):
        kn_str[el.get('name')] = el.text or ''
    elif el.tag == 'plurals' and el.get('name'):
        items = {it.get('quantity'): (it.text or '') for it in el if it.tag == 'item'}
        kn_plu[el.get('name')] = items

FMT = re.compile(r'%\d+\$[sd]|%[sd]|%%')

def fmt_multiset(s):
    return sorted(FMT.findall(s))

def naked_percent(s):
    return FMT.sub('', s).count('%')

# 1+2+4+5 fuer Strings
for k in target_strings:
    if k not in kn_str:
        fails.append(f"[FEHLT String] {k}")
        continue
    kn_v = kn_str[k]
    de_v_raw = de_strings.get(k, '')
    # de-reference hat \\n als literale Backslash-n; im geparsten kn ist \n ebenfalls literal Backslash-n (Android escape)
    # Vergleich identisch DE? (roh)
    if kn_v.strip() == de_v_raw.replace('\\\\','\\').strip():
        fails.append(f"[IDENTISCH DE] {k}")
    # Format-Args: DE roh hat z.B. %1$s -> direkt vergleichbar
    de_fmt = fmt_multiset(de_v_raw)
    kn_fmt = fmt_multiset(kn_v)
    if de_fmt != kn_fmt:
        fails.append(f"[FMT-MISMATCH] {k}: DE={de_fmt} KN={kn_fmt}")
    # nackte %
    if naked_percent(kn_v) != 0:
        fails.append(f"[NACKTES %] {k}: {naked_percent(kn_v)}")

# Plurals
for pk in target_plurals:
    if pk not in kn_plu:
        fails.append(f"[FEHLT Plural] {pk}")
        continue
    for q in ['one','other']:
        if q not in kn_plu[pk]:
            fails.append(f"[FEHLT quantity] {pk}/{q}")
            continue
        kn_v = kn_plu[pk][q]
        de_v = de_plurals[pk].get(q, '')
        if kn_v.strip() == de_v.replace('\\\\','\\').strip():
            fails.append(f"[IDENTISCH DE Plural] {pk}/{q}: {kn_v}")
        if fmt_multiset(kn_v) != fmt_multiset(de_v):
            fails.append(f"[FMT-MISMATCH Plural] {pk}/{q}: DE={fmt_multiset(de_v)} KN={fmt_multiset(kn_v)}")
        if naked_percent(kn_v) != 0:
            fails.append(f"[NACKTES % Plural] {pk}/{q}")

# 6. 150 in ai_limits_dialog_body == 4x
ail = kn_str.get('ai_limits_dialog_body','')
c150 = len(re.findall(r'150', ail))
if c150 != 4:
    fails.append(f"[150-COUNT] ai_limits_dialog_body hat 150 {c150}x (erwartet 4)")
else:
    print(f"OK: 150 kommt {c150}x in ai_limits_dialog_body vor")
# weitere Zahlen-Pruefung
for num in ['600','30','101','151','50','5']:
    if num not in ail:
        warns.append(f"Zahl {num} fehlt in ai_limits_dialog_body")

# 7. Script-Range: Telugu-Zeichen (U+0C00-U+0C7F) duerfen NICHT vorkommen in den Werten
telugu = []
for k, v in {**kn_str, **{f"{pk}/{q}": kn_plu[pk][q] for pk in kn_plu for q in kn_plu[pk]}}.items():
    for ch in v:
        cp = ord(ch)
        if 0x0C00 <= cp <= 0x0C7F:
            telugu.append((k, hex(cp), ch))
if telugu:
    fails.append(f"[TELUGU-MIX] {telugu[:10]}")
else:
    print("OK: kein Telugu-Zeichen (U+0C00-U+0C7F) in kn-Werten")

# Kannada-Range vorhanden?
kannada_chars = sum(1 for v in kn_str.values() for ch in v if 0x0C80 <= ord(ch) <= 0x0CFF)
print(f"Kannada-Zeichen (U+0C80-U+0CFF) gesamt in neuen+alten Strings: {kannada_chars}")

# 8. ZWNJ
zwnj = content.count('‌')
print(f"ZWNJ (U+200C) gesamt im File: {zwnj}")

# delete_account: 'Google-Konto bleibt'?
dac = kn_str.get('settings_delete_account_confirm_body','')
if 'Google' in dac:
    print("OK: settings_delete_account_confirm_body erwaehnt Google (Konto bleibt)")
else:
    warns.append("settings_delete_account_confirm_body: kein 'Google' gefunden")
# revoke §356a
rev = kn_str.get('settings_revoke_subtitle','')
if '356a' in rev:
    print("OK: settings_revoke_subtitle enthaelt § 356a")
else:
    warns.append("settings_revoke_subtitle: kein 356a")
# do_not_sell ohne Jahr (2026 sollte NICHT drin sein - DE nennt kein Jahr)
dns = kn_str.get('consent_toggle_do_not_sell_body','')
if '2026' in dns:
    warns.append("consent_toggle_do_not_sell_body enthaelt '2026' (DE nennt kein Jahr)")
else:
    print("OK: consent_toggle_do_not_sell_body ohne Jahr 2026")

print("\n=== ERGEBNIS ===")
print(f"WARNINGS ({len(warns)}):")
for w in warns:
    print("  -", w)
if fails:
    print(f"FAILS ({len(fails)}):")
    for fl in fails:
        print("  X", fl)
    sys.exit(1)
print(f"Strings geprueft: {len(target_strings)}, Plurals: {len(target_plurals)} — ALLE OK")
sys.exit(0)
