#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Inspektion: UK-Job-Plan auslesen + Vorhandensein der Keys in values-uk pruefen."""
import json, os, subprocess

base = os.path.dirname(os.path.abspath(__file__))
plan = json.load(open(os.path.join(base, 'job-plan.json'), encoding='utf-8'))
uk = plan['languages']['uk']
print("UK PLAN KEYS:", list(uk.keys()))
strings = uk.get('strings', [])
plurals = uk.get('plurals', [])
print("STRINGS COUNT:", len(strings))
print("PLURALS:", plurals)
print("STRINGS LIST:")
for s in strings:
    print("  -", s)

target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-uk/strings.xml')
print("\nTARGET EXISTS:", os.path.exists(target))
# Pre-Check: which keys already present
with open(target, encoding='utf-8') as f:
    content = f.read()
print("\nKEY PRESENCE in values-uk:")
for s in strings:
    cnt = content.count(f'name="{s}"')
    print(f"  {s}: {cnt}")
for p in plurals:
    cnt = content.count(f'name="{p}"')
    print(f"  PLURAL {p}: {cnt}")
