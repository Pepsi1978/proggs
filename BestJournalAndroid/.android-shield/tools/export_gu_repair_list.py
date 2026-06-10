#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Exportiert alle zu reparierenden Gujarati-Strings (Key + voller DE-Text + aktueller gu-Wert)."""
import os, json
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(APP, "app/src/main/res")
OUT = os.path.join(APP, ".android-shield/gu-repair-list.json")

def load(path):
    root = ET.parse(path).getroot()
    out = {}
    for el in root:
        if el.tag == "string" and el.get("name"):
            out[el.get("name")] = {
                "text": "".join(el.itertext()),
                "translatable": el.get("translatable", "true").lower() != "false",
            }
    return out

de = load(os.path.join(RES, "values/strings.xml"))
gu = load(os.path.join(RES, "values-gu/strings.xml"))

def has_gujarati(s):
    return any(0x0A80 <= ord(c) <= 0x0AFF for c in s)
def has_letters(s):
    import unicodedata
    return any(unicodedata.category(c).startswith("L") for c in s)

repair = []
for k, dv in de.items():
    if not dv["translatable"]:
        continue
    if k not in gu:
        continue
    g = gu[k]["text"]
    d = dv["text"]
    needs = False
    if "♻" in g:
        needs = True
    elif has_letters(d) and not has_gujarati(g) and g.strip():
        # gu hat keine Gujarati-Zeichen, DE hat aber Buchstaben -> verdaechtig
        # Ausnahme: reine Tech/Marken (Google Play, PDF, AI ohne sonstigen Text) -> trotzdem pruefen
        needs = True
    if needs:
        repair.append({"key": k, "de": d, "gu_current": g})

with open(OUT, "w", encoding="utf-8") as f:
    json.dump(repair, f, ensure_ascii=False, indent=2)
print(f"Zu reparierende gu-Strings: {len(repair)}")
print(f"Report: {OUT}")
# Kategorien-Uebersicht (nach Key-Praefix)
from collections import Counter
pref = Counter(k["key"].split("_")[0] for k in repair)
print("\nNach Praefix:")
for p, c in pref.most_common():
    print(f"  {p:18} {c}")
