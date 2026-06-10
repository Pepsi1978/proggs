#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Extrahiert die RECHTSKRITISCHEN String-Keys (Paywall-Pflichtangaben, Widerruf,
Consent/DSGVO, Permission-Rationale, Health/AI-Disclaimer, Account-Loeschung,
Legal-Links) aus values/strings.xml inkl. DE-Originaltext.
Diese Liste ist die fokussierte Eingabe fuer die 26 Cross-Lingual-Rechts-Worker.
Schreibt .android-shield/legal-keys.json + Konsolen-Uebersicht.
"""
import os, re, json
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
DE = os.path.join(APP, "app/src/main/res/values/strings.xml")
OUT = os.path.join(APP, ".android-shield/legal-keys.json")

# Kategorie -> Key-Regex (auf Key-Namen)
CATS = {
    "paywall_billing": r"(paywall|billing|subscription|abo|price|preis|trial|ai_limit|free_limit|premium_feature|upgrade|purchase|restore)",
    "churn_cancel": r"(churn|cancel|kuendig|unsubscribe|downgrade)",
    "widerruf_refund": r"(widerruf|withdraw|refund|geld_zurueck|money_back|rueckgabe)",
    "consent_privacy": r"(consent|privacy|datenschutz|gdpr|dsgvo|tracking|analytics_opt|cookie)",
    "permission_rationale": r"(permission_rationale|rationale_|disclosure)",
    "disclaimer": r"(disclaimer|ai_.*disclaimer|health|medical|crisis|krise|not_a_substitute|hinweis_)",
    "account_deletion": r"(delete_account|account_delet|loesch|erase_data|data_delet)",
    "legal_links": r"(impressum|_terms|_agb|terms_|agb_|legal_|nutzungsbeding|lizenz|imprint)",
}
CAT_RE = {k: re.compile(v, re.I) for k, v in CATS.items()}

root = ET.parse(DE).getroot()
strings = {}
for el in root:
    if el.tag == "string" and el.get("name"):
        strings[el.get("name")] = {
            "text": "".join(el.itertext()),
            "translatable": el.get("translatable", "true").lower() != "false",
        }

result = {"categories": {}, "allKeys": [], "totalKeys": 0}
seen = set()
for cat, rx in CAT_RE.items():
    hits = []
    for name, v in strings.items():
        if not v["translatable"]:
            continue
        if rx.search(name):
            hits.append({"key": name, "de": v["text"]})
            seen.add(name)
    hits.sort(key=lambda x: x["key"])
    result["categories"][cat] = hits

result["allKeys"] = sorted(seen)
result["totalKeys"] = len(seen)

with open(OUT, "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

print(f"Rechtskritische translatable Keys gesamt: {len(seen)}")
for cat in CATS:
    print(f"  {cat:24} {len(result['categories'][cat]):3} keys")
print(f"\nReport: {OUT}")
print("\n=== Stichprobe je Kategorie (Key + DE-Anfang) ===")
for cat in CATS:
    hits = result["categories"][cat]
    if hits:
        print(f"\n[{cat}]")
        for h in hits[:4]:
            print(f"  {h['key']}: {repr(h['de'][:70])}")
