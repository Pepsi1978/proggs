#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cross-Lingual-Rechtspruefung: die ABMAHNKRITISCHEN Keys ueber alle 26 Sprachen.
Deterministische Pruefung pro kritischem Key:
  - vorhanden in der Sprache?
  - uebersetzt (!= DE-Original)?
  - nicht leer + plausible Laenge (0.35x - 3.5x vom DE)?
Gibt NUR Auffaelligkeiten aus (fehlend/identisch/verdaechtig). Am Ende: Volltexte
ausgewaehlter Schluessel-Keys in lesbaren Sprachen fuer inhaltliche LLM-Bewertung.
"""
import os, json
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(APP, "app/src/main/res")
OUT = os.path.join(APP, ".android-shield/crosslingual-legal-report.json")

# Abmahnkritische Keys nach rechtlicher Wichtigkeit (kuratiert)
CRITICAL = {
    "Widerruf/AGB (§355 BGB)": ["paywall_legal_hint", "paywall_legal_hint_link"],
    "Auto-Renewal-Transparenz": ["churn_auto_renew_note"],
    "Account-Loeschung (DSGVO Art.17, 'unwiderruflich')": ["settings_delete_account_confirm", "settings_delete_account_confirm_body"],
    "Health-Disclaimer (kein Therapie-Ersatz)": ["ai_output_health_disclaimer", "ai_output_health_disclaimer_long"],
    "Permission-Rationale (DSGVO Art.13)": ["permission_rationale_camera", "permission_rationale_microphone", "permission_rationale_notifications"],
    "Consent gleichrangig (DSGVO)": ["consent_btn_accept_all", "consent_btn_minimum_only", "consent_accept_all"],
    "Limit-Transparenz (UWG)": ["ai_limit_title", "ai_limit_body", "ai_limits_disclaimer"],
}
# Schluessel-Keys deren Volltext ich inhaltlich pruefe (lesbare Sprachen)
DEEP = ["paywall_legal_hint", "settings_delete_account_confirm_body", "ai_output_health_disclaimer", "churn_auto_renew_note"]
READABLE = ["en", "fr", "es", "it", "nl", "pt-rBR", "pt-rPT"]

LOCALES = sorted(d.replace("values-", "") for d in os.listdir(RES)
                 if d.startswith("values-") and os.path.isfile(os.path.join(RES, d, "strings.xml")))

def load(loc):
    p = os.path.join(RES, "values" if loc == "de" else f"values-{loc}", "strings.xml")
    try:
        root = ET.parse(p).getroot()
    except Exception as e:
        return None
    return {el.get("name"): "".join(el.itertext()) for el in root if el.tag == "string"}

de = load("de")
data = {loc: load(loc) for loc in LOCALES}

report = {"criticalKeys": sum(len(v) for v in CRITICAL.values()), "anomalies": {}, "deepTexts": {}}
print("=== CROSS-LINGUAL RECHTSPRUEFUNG — abmahnkritische Keys ueber 26 Sprachen ===\n")

all_keys = [k for ks in CRITICAL.values() for k in ks]
# Existenz im DE pruefen
missing_de = [k for k in all_keys if k not in de]
if missing_de:
    print(f"⚠️  Diese kritischen Keys existieren NICHT in DE (evtl. anders benannt): {missing_de}\n")

total_anom = 0
for cat, keys in CRITICAL.items():
    print(f"[{cat}]")
    for k in keys:
        if k not in de:
            print(f"   {k}: (nicht in DE — uebersprungen)")
            continue
        dv = de[k]
        anom = []
        for loc in LOCALES:
            tr = data[loc]
            if tr is None:
                anom.append(f"{loc}:XML-FEHLER"); continue
            if k not in tr:
                anom.append(f"{loc}:FEHLT"); continue
            lv = tr[k]
            if not lv.strip():
                anom.append(f"{loc}:LEER"); continue
            if lv == dv and len(dv.strip()) > 8:
                anom.append(f"{loc}:=DE(unuebersetzt)"); continue
            ratio = len(lv) / max(len(dv), 1)
            if ratio < 0.35:
                anom.append(f"{loc}:zu-kurz({ratio:.2f})")
            elif ratio > 3.5:
                anom.append(f"{loc}:zu-lang({ratio:.2f})")
        status = "✅ alle 26 OK" if not anom else "⚠️  " + ", ".join(anom)
        report["anomalies"][k] = anom
        total_anom += len(anom)
        print(f"   {k}: {status}")
    print()

report["totalAnomalies"] = total_anom
print(f"GESAMT-Auffaelligkeiten ueber alle kritischen Keys: {total_anom}\n")

# Volltexte der Schluessel-Keys in lesbaren Sprachen
print("=" * 70)
print("=== VOLLTEXTE Schluessel-Keys (DE + lesbare Sprachen) fuer inhaltliche Pruefung ===\n")
for k in DEEP:
    if k not in de:
        continue
    report["deepTexts"][k] = {"de": de[k]}
    print(f"### {k}")
    print(f"   DE   : {de[k][:200]}")
    for loc in READABLE:
        if data[loc] and k in data[loc]:
            report["deepTexts"][k][loc] = data[loc][k]
            print(f"   {loc:6}: {data[loc][k][:200]}")
    print()

with open(OUT, "w", encoding="utf-8") as f:
    json.dump(report, f, ensure_ascii=False, indent=2)
print(f"Report: {OUT}")
