#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Findet KAPUTTE Uebersetzungen ueber alle 26 Sprachen:
Strings wo das DE-Original substanziell ist (>=15 Zeichen, >=3 Woerter), die
Uebersetzung aber verdaechtig: extrem kurz (<5 Zeichen / <=1 'Wort'-Einheit),
oder nur Symbole/Emoji/Satzzeichen, oder enthaelt das ♻-Korruptionszeichen.
CJK-bewusst: zaehlt CJK-Zeichen als volle Worteinheiten (kompakt ist normal).
"""
import os, re, json, unicodedata
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(APP, "app/src/main/res")
OUT = os.path.join(APP, ".android-shield/corrupt-strings-report.json")

LOCALES = sorted(d.replace("values-", "") for d in os.listdir(RES)
                 if d.startswith("values-") and os.path.isfile(os.path.join(RES, d, "strings.xml")))

def load(loc):
    p = os.path.join(RES, "values" if loc == "de" else f"values-{loc}", "strings.xml")
    try:
        root = ET.parse(p).getroot()
    except Exception:
        return None
    out = {}
    for el in root:
        if el.tag == "string" and el.get("name"):
            out[el.get("name")] = {
                "text": "".join(el.itertext()),
                "translatable": el.get("translatable", "true").lower() != "false",
            }
    return out

def is_substantive(s):
    return len(s.strip()) >= 15 and len(s.split()) >= 3

def letters_only(s):
    # zaehlt Buchstaben (inkl. CJK/Indic/Arab) ohne Symbole/Whitespace/Zahlen
    return sum(1 for c in s if unicodedata.category(c).startswith("L"))

SUSPECT_SYMBOLS = set("♻♲☺☻★☆▶◀◆●○■□▲▼")

de = load("de")
report = {"corruptByLocale": {}, "total": 0}
print("=== KAPUTTE/FRAGMENTIERTE Uebersetzungen ueber 26 Sprachen ===\n")
total = 0
for loc in LOCALES:
    tr = load(loc)
    if tr is None:
        print(f"[{loc}] XML-FEHLER"); continue
    bad = []
    for k, dv in de.items():
        if not dv["translatable"]:
            continue
        d = dv["text"].strip()
        if not is_substantive(d):
            continue
        if k not in tr:
            continue
        t = tr[k]["text"].strip()
        letters = letters_only(t)
        symbols = [c for c in t if c in SUSPECT_SYMBOLS]
        # Kriterien fuer "kaputt":
        if symbols:
            bad.append((k, t[:60], "SYMBOL:" + "".join(symbols)))
        elif letters < 3:
            bad.append((k, t[:60], f"nur {letters} Buchstaben"))
        elif len(t) < 5:
            bad.append((k, t[:60], "extrem kurz"))
    if bad:
        report["corruptByLocale"][loc] = [{"key": k, "text": t, "reason": r} for k, t, r in bad]
        total += len(bad)
        print(f"[{loc}] {len(bad)} KAPUTT:")
        for k, t, r in bad:
            print(f"    🔴 {k}: {repr(t)}  [{r}]")
report["total"] = total
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(report, f, ensure_ascii=False, indent=2)
print(f"\nGESAMT kaputte Strings: {total}")
print(f"Betroffene Sprachen: {list(report['corruptByLocale'].keys())}")
print(f"Report: {OUT}")
