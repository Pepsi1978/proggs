"""Insert per-locale translations into values-XX/strings.xml safely.

Usage:
    python3 insert_translations.py <locale-dir> <translations-json>

Reads JSON of {key: value}, inserts each <string name="key">value</string>
right before the </resources> closing tag (or replaces existing entries).
Skips keys that already exist with non-empty content.
"""
import json
import os
import re
import sys
import tempfile

LOCALE_DIR = sys.argv[1]  # e.g. "values-fr"
JSON_PATH = sys.argv[2]

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
TARGET = os.path.join(ROOT, LOCALE_DIR, "strings.xml")

with open(JSON_PATH, "r", encoding="utf-8") as f:
    translations = json.load(f)

with open(TARGET, "r", encoding="utf-8") as f:
    content = f.read()

original = content
inserted = 0
skipped = 0
replaced = 0


def has_xliff(value):
    return "<xliff:g" in value


def build_string_tag(key, value):
    return f'    <string name="{key}">{value}</string>'


for key, value in translations.items():
    pattern_existing = re.compile(
        rf'<string name="{re.escape(key)}"[^>]*>([^<]*(?:<(?!/string>)[^<]*)*)</string>',
        re.DOTALL,
    )
    match = pattern_existing.search(content)
    new_tag = build_string_tag(key, value)
    if match:
        existing_value = match.group(1).strip()
        if existing_value:
            skipped += 1
            continue
        content = content[: match.start()] + new_tag + content[match.end():]
        replaced += 1
    else:
        # Insert before </resources>
        idx = content.rfind("</resources>")
        if idx == -1:
            print(f"FATAL: no </resources> in {TARGET}")
            sys.exit(1)
        # Find indentation of </resources>
        line_start = content.rfind("\n", 0, idx) + 1
        indent_part = content[line_start:idx]
        # Insert with newline + tag + newline
        prefix = content[:idx]
        # Trim trailing newlines before </resources>, then add our tag
        prefix_stripped = prefix.rstrip()
        # Add a blank line before our block if the file doesn't already end with one
        content = prefix_stripped + "\n" + new_tag + "\n" + indent_part + content[idx:]
        inserted += 1

if content != original:
    d = os.path.dirname(os.path.abspath(TARGET))
    with tempfile.NamedTemporaryFile(
        "w", dir=d, suffix=".tmp", delete=False, encoding="utf-8", newline="\n"
    ) as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    os.replace(tmp_path, TARGET)

print(f"{LOCALE_DIR}: inserted={inserted} replaced={replaced} skipped={skipped}")
