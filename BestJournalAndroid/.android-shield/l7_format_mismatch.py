# -*- coding: utf-8 -*-
"""KRITISCH: Format-argument mismatch analysis for the 7 affected keys."""
import json, os, re

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
with open(os.path.join(base, 'l7-parsed-strings.json'), 'r', encoding='utf-8') as f:
    data = json.load(f)

de = data['de']

# The mismatch list: (key, lang)
targets = [
    ('dashboard_profile_focus_summary', 'pl'),
    ('dashboard_profile_focus_entropy', 'pl'),
    ('dashboard_profile_focus_goals', 'pl'),
    ('dashboard_profile_focus_insight', 'pl'),
    ('ai_prompt_rerank_system', 'pl'),
    ('ai_prompt_custom_schema', 'pl'),
    ('ai_prompt_custom_schema', 'nl'),
]

FMT = re.compile(r'%(\d+\$)?[sdfx]')  # android format args

def fmt_args(s):
    return FMT.findall(s) and FMT.pattern, [m.group(0) for m in FMT.finditer(s)]

for key, lang in targets:
    print("=" * 90)
    print(f"KEY: {key}   LANG: {lang}")
    dev = de.get(key, "<<MISSING IN DE>>")
    locv = data[lang].get(key, "<<MISSING IN LOC>>")
    de_args = [m.group(0) for m in FMT.finditer(dev)]
    loc_args = [m.group(0) for m in FMT.finditer(locv)]
    print(f"  DE  fmt-args: {de_args}")
    print(f"  {lang} fmt-args: {loc_args}")
    print(f"  --- DE FULL ({len(dev)} chars) ---")
    print(dev)
    print(f"  --- {lang} FULL ({len(locv)} chars) ---")
    print(locv)
    print()
