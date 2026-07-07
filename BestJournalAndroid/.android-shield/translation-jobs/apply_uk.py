#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""finale Phase 3 — Translation-Worker uk (Ukrainisch).
Ersetzt 33 String-Keys in values-uk, fuegt paywall_monthly_note NEU ein,
ersetzt 2 Plurals (datetime_months/years_relative) mit CLDR uk one/few/many/other.
Nur values-uk/strings.xml + .android-shield/ werden beruehrt. Lambda-Replacement (kein $-Backref).
"""
import json, os, re, html

base = os.path.dirname(os.path.abspath(__file__))
data = json.load(open(os.path.join(base, 'uk-data.json'), encoding='utf-8'))
target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-uk/strings.xml')

with open(target, 'r', encoding='utf-8') as f:
    content = f.read()

applied = []
skipped_already_present = []
inserted_new = []
plurals_replaced = []
errors = []

strings = data['strings']

# --- 1) String-Keys: replace value in-place (or insert if absent) ---
ckpt_path = os.path.join(base, 'worker-checkpoints', 'tw-uk.jsonl')
os.makedirs(os.path.dirname(ckpt_path), exist_ok=True)

def write_ckpt(stage, info):
    with open(ckpt_path, 'a', encoding='utf-8') as cf:
        cf.write(json.dumps({"stage": stage, "info": info}, ensure_ascii=False) + "\n")

write_ckpt("start", {"target": target, "n_strings": len(strings)})

# escape XML special chars in value but PRESERVE literal \n sequences and \' and \" as-is
def xml_escape(val):
    # The source already uses \\n (literal backslash-n) and \\\" -> in JSON loaded -> \n stays as backslash+n?
    # In uk-data.json we wrote \\n -> json.load gives '\\n' (backslash + n literal). Good for Android.
    # Only &, <, > need escaping for XML text. Quotes inside: source uses \" already escaped form.
    val = val.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    return val

for key, raw in strings.items():
    val = xml_escape(raw)
    pat = re.compile(r'(<string name="' + re.escape(key) + r'"[^>]*>).*?(</string>)', re.DOTALL)
    if pat.search(content):
        content = pat.sub(lambda m, v=val: m.group(1) + v + m.group(2), content, count=1)
        applied.append(key)
    else:
        # not present -> Pre-Check then rfind insert (paywall_monthly_note case)
        if content.count(f'name="{key}"') > 0:
            skipped_already_present.append(key)
            continue
        idx = content.rfind('</resources>')
        if idx == -1:
            errors.append(f"{key}: kein </resources>")
            continue
        new_line = f'    <string name="{key}">{val}</string>\n'
        content = content[:idx] + new_line + content[idx:]
        inserted_new.append(key)

write_ckpt("strings_done", {"applied": len(applied), "inserted": inserted_new, "skipped": skipped_already_present})

# --- 2) Plurals: replace whole <plurals> block ---
for pname, forms in data['plurals'].items():
    items = ""
    for q in ('one', 'few', 'many', 'other'):
        if q in forms:
            iv = xml_escape(forms[q])
            items += f'        <item quantity="{q}">{iv}</item>\n'
    block = f'    <plurals name="{pname}">\n{items}    </plurals>'
    ppat = re.compile(r'<plurals name="' + re.escape(pname) + r'">.*?</plurals>', re.DOTALL)
    if ppat.search(content):
        content = ppat.sub(lambda m, b=block: b, content, count=1)
        plurals_replaced.append(pname)
    else:
        errors.append(f"plural {pname}: nicht gefunden")

write_ckpt("plurals_done", {"replaced": plurals_replaced})

with open(target, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

result = {
    "applied": applied,
    "inserted_new": inserted_new,
    "skipped_already_present": skipped_already_present,
    "plurals_replaced": plurals_replaced,
    "errors": errors,
}
print(json.dumps(result, ensure_ascii=False, indent=2))
write_ckpt("write_done", {"errors": errors})
