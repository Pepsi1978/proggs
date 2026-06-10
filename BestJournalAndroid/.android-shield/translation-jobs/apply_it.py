# -*- coding: utf-8 -*-
import re, sys, subprocess, json, importlib.util
spec = importlib.util.spec_from_file_location("it_data", "it-data.py")
it_data = importlib.util.module_from_spec(spec)
spec.loader.exec_module(it_data)
STRINGS, PLURALS = it_data.STRINGS, it_data.PLURALS

TF = '/c/Users/barwa/proggs/BestJournalAndroid/app/src/main/res/values-it/strings.xml'
TF_WIN = r'C:\Users\barwa\proggs\BestJournalAndroid\app\src\main\res\values-it\strings.xml'

# read target (Python IO, not Read-tool)
with open(TF_WIN, 'r', encoding='utf-8') as f:
    content = f.read()

applied = []
skipped_already_present = {}
inserted = []
errors = []

# --- 1. Replace existing string keys ---
for key, val in STRINGS.items():
    if key == 'paywall_monthly_note':
        continue  # handled as insert below
    # match <string name="KEY" ...optional attrs...>...</string> (DOTALL for multiline)
    pat = re.compile(
        r'(<string name="' + re.escape(key) + r'"[^>]*>)(.*?)(</string>)',
        re.DOTALL
    )
    m = pat.search(content)
    if not m:
        errors.append(f"REPLACE-MISS:{key}")
        continue
    new_el = m.group(1) + val + m.group(3)
    content = content[:m.start()] + new_el + content[m.end():]
    applied.append(key)

# --- 2. Insert paywall_monthly_note via rfind (NEW key) ---
nkey = 'paywall_monthly_note'
# pre-check inside content
if re.search(r'<string name="' + re.escape(nkey) + r'"', content):
    skipped_already_present.setdefault('it', []).append(nkey)
else:
    new_strings = f'    <string name="{nkey}">{STRINGS[nkey]}</string>'
    idx = content.rfind('</resources>')
    if idx == -1:
        raise RuntimeError(f"{TF}: kein </resources> gefunden — Datei korrupt")
    content = content[:idx] + new_strings + '\n' + content[idx:]
    inserted.append(nkey)
    applied.append(nkey)

# --- 3. Replace plurals (de-stale) ---
for pkey, items in PLURALS.items():
    pat = re.compile(
        r'(<plurals name="' + re.escape(pkey) + r'"[^>]*>)(.*?)(</plurals>)',
        re.DOTALL
    )
    m = pat.search(content)
    if not m:
        errors.append(f"PLURAL-MISS:{pkey}")
        continue
    inner = '\n'
    for qty, txt in items.items():
        inner += f'        <item quantity="{qty}">{txt}</item>\n'
    inner += '    '
    new_el = m.group(1) + inner + m.group(3)
    content = content[:m.start()] + new_el + content[m.end():]
    applied.append(pkey)

# --- write back ---
with open(TF_WIN, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

print("APPLIED:", len(applied))
print("INSERTED:", inserted)
print("SKIPPED:", skipped_already_present)
print("ERRORS:", errors)
