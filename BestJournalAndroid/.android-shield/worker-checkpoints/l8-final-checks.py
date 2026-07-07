# -*- coding: utf-8 -*-
"""Final checks: umlaut/German-leak scan, identical-to-DE classification, SCC presence, impressum footer."""
import json, os, sys, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from l8_lib import parse_strings  # noqa

root = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
de_s, de_p, de_a = parse_strings(os.path.join(root, 'values/strings.xml'))
br_s, br_p, br_a = parse_strings(os.path.join(root, 'values-pt-rBR/strings.xml'))
pt_s, pt_p, pt_a = parse_strings(os.path.join(root, 'values-pt-rPT/strings.xml'))

# --- German-leak scan: strings containing German-only words/umlauts (excluding known shared) ---
# Portuguese uses ã õ ç á é í ó ú â ê ô à — NOT ä ö ü ß. Those are German leaks.
german_umlauts = re.compile(r'[äöüÄÖÜß]')
def german_leak(strings):
    hits = {}
    for k, v in strings.items():
        if german_umlauts.search(v):
            hits[k] = v[:70]
    return hits
print("=== GERMAN UMLAUT LEAKS (ä/ö/ü/ß) ===")
print("  pt-rBR:")
for k,v in german_leak(br_s).items(): print(f"    {k}: {v}")
print("  pt-rPT:")
for k,v in german_leak(pt_s).items(): print(f"    {k}: {v}")
# plurals
print("  PLURALS German leaks:")
for lang,plr in [('BR',br_p),('PT',pt_p)]:
    for k,items in plr.items():
        for q,t in items.items():
            if german_umlauts.search(t) or t.lower().startswith('vor ') or ' Monat' in t or ' Jahr' in t or 'Tag' in t and 'vor' in t.lower():
                print(f"    {lang} {k}[{q}]: {t}")

# --- ALL plurals comparison BR/PT vs DE (are any untranslated?) ---
print("\n=== ALL PLURALS (DE vs BR vs PT) ===")
for k in de_p:
    print(f"  [{k}]")
    print(f"    DE: {de_p[k]}")
    print(f"    BR: {br_p.get(k,'[MISSING]')}")
    print(f"    PT: {pt_p.get(k,'[MISSING]')}")

# --- SCC presence in privacy gates ---
print("\n=== SCC / Standardvertragsklauseln presence ===")
scc_pat = re.compile(r'cl[áa]usulas contratuais|SCC|standardvertrag|data privacy framework|contratuais.tipo|contratuais padr', re.IGNORECASE)
for k in ['privacy_gate_tts_body','privacy_gate_groq_body','privacy_gate_gemini_body']:
    for lang,d in [('DE',de_s),('BR',br_s),('PT',pt_s)]:
        v=d.get(k,'')
        print(f"  {k} [{lang}]: SCC/DPF mention={bool(scc_pat.search(v))}")

# --- identical-to-DE classification ---
print("\n=== identicalToDe classification ===")
ident_br = ['entry_cd_photo_page','follow_up_tab_original','label_original','label_photo','label_premium','retro_cd_photo','retro_photo_n','settings_about_version','settings_export_photos','settings_feedback','settings_premium_section']
ident_pt = ['consent_footer_link_impressum','entry_cd_photo_page','follow_up_tab_original','label_original','label_photo','label_premium','retro_cd_photo','retro_photo_n','settings_about_version','settings_export_photos','settings_feedback','settings_premium_section']
for lang, lst, src in [('BR',ident_br,br_s),('PT',ident_pt,pt_s)]:
    print(f"  [{lang}]")
    for k in lst:
        print(f"    {k} = {repr(src.get(k,'')[:55])}")
