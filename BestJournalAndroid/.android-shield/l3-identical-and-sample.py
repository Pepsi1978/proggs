#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""L3: classify identicalToDe 'other' + xliff/placeholder check + 15 random samples (read-only)."""
import os, sys, json, re, random
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
SHIELD = os.path.join(ROOT, '.android-shield')
mat = json.load(open(os.path.join(SHIELD, 'cross-lingual-matrix-2026-06-10.json'), encoding='utf-8'))

def inner_text(el):
    parts = []
    if el.text: parts.append(el.text)
    for ch in el:
        parts.append(ET.tostring(ch, encoding='unicode'))
        if ch.tail: parts.append(ch.tail)
    return ''.join(parts)

def load(path):
    root = ET.parse(path).getroot()
    return {el.get('name'): inner_text(el) for el in root if el.tag == 'string'}

de = load(os.path.join(ROOT, 'app/src/main/res/values/strings.xml'))
langs = {
    'gu': load(os.path.join(ROOT, 'app/src/main/res/values-gu/strings.xml')),
    'kn': load(os.path.join(ROOT, 'app/src/main/res/values-kn/strings.xml')),
    'ml': load(os.path.join(ROOT, 'app/src/main/res/values-ml/strings.xml')),
}

# --- classify identicalToDe ---
print("### identicalToDe CLASSIFICATION ###")
ident_summary = {}
for lang in ['gu', 'kn', 'ml']:
    ident = mat['locales'][lang]['identicalToDe']
    jsonk = [k for k in ident if k.startswith(('json_key_', 'json_value_'))]
    other = [k for k in ident if not k.startswith(('json_key_', 'json_value_'))]
    # For 'other', judge if value is a proper noun / short token (legit) or real prose (untranslated)
    real_untranslated = []
    legit_token = []
    for k in other:
        v = de.get(k, '')
        wc = len(v.split())
        # legit: brand/proper noun / single-word UI token / version label
        if wc <= 2 or k in ('label_premium', 'settings_premium_section', 'settings_about_version',
                             'achievements_title', 'consent_footer_link_impressum',
                             'consent_info_system_backup_title'):
            legit_token.append((k, v))
        else:
            real_untranslated.append((k, v))
    ident_summary[lang] = {'total': len(ident), 'jsonKey': len(jsonk),
                           'legitToken': [k for k, _ in legit_token],
                           'realUntranslated': [k for k, _ in real_untranslated]}
    print(f"\n  {lang}: total={len(ident)} jsonKey={len(jsonk)} legitToken={len(legit_token)} realUntranslated={len(real_untranslated)}")
    for k, v in legit_token:
        print(f"      [legit]  {k} = {v!r}")
    for k, v in real_untranslated:
        print(f"      [CHECK]  {k} = {v!r}")

# --- xliff / placeholder embedding check across ALL strings (not just legal) ---
print("\n\n### XLIFF / PLACEHOLDER EMBEDDING (all strings, mismatch vs DE) ###")
PH = re.compile(r'%\d*\$?[a-z]')
XLIFF = re.compile(r'<ns0:g|<xliff:g|id="[^"]+"')
ph_issues = {}
for lang, d in langs.items():
    bad = []
    for k, dv in de.items():
        if k not in d:
            continue
        tv = d[k]
        dph, tph = sorted(PH.findall(dv)), sorted(PH.findall(tv))
        if dph != tph:
            bad.append({'key': k, 'de': dph, lang: tph})
    ph_issues[lang] = bad
    print(f"  {lang}: {len(bad)} placeholder mismatches" + (f" -> {[b['key'] for b in bad]}" if bad else ""))

# --- 15 random samples per language ---
print("\n\n### 15 RANDOM SAMPLES PER LANGUAGE ###")
random.seed(42)
common = [k for k in de.keys() if all(k in langs[l] for l in langs)]
samples = {}
for lang in ['gu', 'kn', 'ml']:
    pick = random.sample(common, 15)
    samples[lang] = pick
    print(f"\n  --- {lang} ---")
    for k in pick:
        print(f"    {k}: {langs[lang][k][:70]!r}")

# dump
out = {'identicalSummary': ident_summary,
       'placeholderIssues': ph_issues,
       'randomSampleKeys': samples}
with open(os.path.join(SHIELD, 'l3-identical-sample-result.json'), 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=1)
print("\nWROTE l3-identical-sample-result.json")
