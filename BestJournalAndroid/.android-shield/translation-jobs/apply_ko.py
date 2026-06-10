# -*- coding: utf-8 -*-
"""Apply Korean (ko) translations to values-ko/strings.xml.
- Replace existing <string> keys in place (preserve order).
- Insert NEW key paywall_monthly_note via rfind before </resources>.
- Replace both plurals (drop deutsch-stale 'one', keep CLDR-only 'other').
- XML-escape values; placeholders/% are kept literally (data already has \\n etc).
"""
import json
import os
import re
import sys

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
TARGET = os.path.join(ROOT, "app/src/main/res/values-ko/strings.xml")
DATA = os.path.join(ROOT, ".android-shield/translation-jobs/ko-data.json")

with open(DATA, "r", encoding="utf-8") as f:
    data = json.load(f)

with open(TARGET, "r", encoding="utf-8") as f:
    content = f.read()

orig = content
applied = []
inserted = []
errors = []


def esc(v):
    # XML-escape for text content. & < > only (apostrophe/quote handled by Android
    # via \' \" already present in source; Korean rights texts use literal \" escapes
    # which we keep as-is since they are XML-safe inside element text).
    return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


# --- 1. Replace / insert <string> keys ---
for key, val in data["strings"].items():
    xval = esc(val)
    new_el = f'<string name="{key}">{xval}</string>'
    # match existing element (single line or multiline, non-greedy)
    pat = re.compile(
        r'<string name="' + re.escape(key) + r'">.*?</string>', re.DOTALL
    )
    if pat.search(content):
        content, n = pat.subn(lambda m: new_el, content, count=1)
        if n == 1:
            applied.append(key)
        else:
            errors.append(f"replace-failed:{key}")
    else:
        # NEW key -> insert before last </resources>
        idx = content.rfind("</resources>")
        if idx == -1:
            errors.append("no-resources-tag")
            break
        content = content[:idx] + "    " + new_el + "\n" + content[idx:]
        inserted.append(key)

# --- 2. Replace plurals (CLDR Korean: only 'other') ---
for pkey, forms in data["plurals"].items():
    items = "\n".join(
        f'        <item quantity="{q}">{esc(t)}</item>' for q, t in forms.items()
    )
    new_pl = f'<plurals name="{pkey}">\n{items}\n    </plurals>'
    pat = re.compile(
        r'<plurals name="' + re.escape(pkey) + r'">.*?</plurals>', re.DOTALL
    )
    if pat.search(content):
        content, n = pat.subn(lambda m: new_pl, content, count=1)
        if n == 1:
            applied.append(pkey)
        else:
            errors.append(f"plural-replace-failed:{pkey}")
    else:
        errors.append(f"plural-missing:{pkey}")

if content == orig:
    print("ERROR: no changes made")
    sys.exit(1)

with open(TARGET, "w", encoding="utf-8", newline="\n") as f:
    f.write(content)

print(json.dumps({
    "applied": applied,
    "inserted": inserted,
    "errors": errors,
    "applied_count": len(applied),
    "inserted_count": len(inserted),
}, ensure_ascii=False, indent=2))
