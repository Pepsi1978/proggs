#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Orchestrator-eigene Verifikation (W1 HWG/UWG-Ersatz nach Worker-Crash).
Sucht die 26.-Lauf-Marketing-Claims im AKTUELLEN values/strings.xml und gibt
key + voller Wert + Zeile aus. Crashfrei (laeuft im 1M-Orchestrator-Kontext)."""
import re, sys, io, os
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

APP = os.path.expanduser('~/proggs/BestJournalAndroid')
SX = os.path.join(APP, 'app/src/main/res/values/strings.xml')
raw = open(SX, encoding='utf-8').read()

# Map: Position -> Zeilennummer
def line_of(idx):
    return raw.count('\n', 0, idx) + 1

# Alle <string name="X">...</string> (auch multiline)
strings = {}
for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', raw, re.DOTALL):
    strings[m.group(1)] = (m.group(2), line_of(m.start()))

# HWG/UWG-Claims aus dem 26.-Lauf (fragment, id, kategorie, alt-severity)
claims = [
    ("sofort helfen", "S1-001", "HWG", "🟧"),
    ("alles zu viel wird", "S1-002", "HWG", "🟧"),
    ("Von Klarheit bis Selbsterkenntnis", "S2-003", "HWG", "🟥"),
    ("Klarheit und Veränderung", "S3b-001", "HWG", "🟥"),
    ("Stress abbauen", "S3b-004", "HWG", "🟥"),
    ("erzählt deine Geschichte", "S2-004", "HWG", "🟧"),
    ("zukünftiges Ich wird dir danken", "S4-012", "HWG", "🟨"),
    ("Stärken, die dir nicht bewusst", "S1-003", "UWG", "🟥"),
    ("verborgene Denk- und Gefühlsmuster", "S1-004/S3b-002", "UWG", "🟥"),
    ("verborgene Muster in deinem Denken", "S3b-003", "UWG", "🟥"),
    ("Verstehe verborgene Muster", "S3b-003b", "UWG", "🟥"),
    ("was dich wirklich bewegt", "S1-005", "UWG", "🟧"),
    ("Verborgene Muster und Überzeugungen", "S1-006", "UWG", "🟧"),
    ("Höhere Qualität für deine Spracheinträge", "S2-005", "UWG", "🟧"),
    ("im Alltag nicht auffallen", "S3b-009", "UWG", "🟧"),
    ("im Alltag verborgen bleiben", "S3b-010", "UWG", "🟧"),
    ("Beliebteste Wahl", "S3b-012", "UWG", "🟨"),
    ("großen Themen deines Jahres", "S3b-013", "UWG", "🟨"),
    ("persönlicher KI-Begleiter", "S4-013", "UWG", "🟨"),
    ("desto besser werden die Ergebnisse", "S2-001", "UWG", "🟨"),
    ("Persönliche Stärken", "S1-003b", "UWG", "🟥"),
    ("Tiefgehend", "S1-006b", "UWG", "🟨"),
]

print("=" * 70)
print("HWG/UWG-CLAIM-VERIFIKATION gegen aktuellen Code (v0.21.9)")
print("=" * 70)
found = 0
for frag, cid, cat, sev in claims:
    hits = []
    for k, (v, ln) in strings.items():
        if frag in v:
            hits.append((k, ln, v))
    if hits:
        found += 1
        for k, ln, v in hits[:3]:
            vclean = re.sub(r'\s+', ' ', v).strip()[:150]
            print(f"\n[{cid}] {sev} {cat} — NOCH VORHANDEN")
            print(f"  key={k} (Zeile {ln})")
            print(f"  WERT: {vclean}")
    else:
        print(f"\n[{cid}] {sev} {cat} — nicht gefunden (evtl. entschaerft/umformuliert): '{frag}'")

print("\n" + "=" * 70)
print("HWG-DISCLAIMER-CHECK")
print("=" * 70)
disc_keys = ["ai_output_health_disclaimer", "ai_output_health_disclaimer_long",
             "settings_crisis_dialog_body", "ai_banner_body", "ai_disclaimer",
             "privacy_gate_gemini_body"]
for k in disc_keys:
    if k in strings:
        v = re.sub(r'\s+', ' ', strings[k][0]).strip()[:160]
        print(f"  ✓ {k} (Z.{strings[k][1]}): {v}")
    else:
        print(f"  ✗ {k}: FEHLT")

# Disclaimer-Phrasen gesamt
print("\n  Disclaimer-Phrasen-Vorkommen:")
for phrase in ["ersetzt keine professionelle", "Keine Therapie", "keine professionelle Beratung", "Anregungen"]:
    cnt = raw.count(phrase)
    print(f"    '{phrase}': {cnt}x")

print("\n" + "=" * 70)
print(f"ZUSAMMENFASSUNG: {found}/{len(claims)} Claim-Fragmente noch im Code vorhanden")
print("=" * 70)
