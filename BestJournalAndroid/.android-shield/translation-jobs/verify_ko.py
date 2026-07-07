# -*- coding: utf-8 -*-
"""Post-verification for ko (FIN-051c)."""
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
TARGET = os.path.join(ROOT, "app/src/main/res/values-ko/strings.xml")
DATA = os.path.join(ROOT, ".android-shield/translation-jobs/ko-data.json")
DEREF = os.path.join(ROOT, ".android-shield/translation-jobs/de-reference.json")

with open(DATA, "r", encoding="utf-8") as f:
    data = json.load(f)
with open(DEREF, "r", encoding="utf-8") as f:
    deref = json.load(f)

problems = []

# 1. XML valid
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
except Exception as e:
    print(json.dumps({"xml_valid": False, "error": str(e)}))
    sys.exit(1)

# collect parsed values
str_map = {}
plural_map = {}
for el in root:
    if el.tag == "string":
        str_map[el.get("name")] = "".join(el.itertext())
    elif el.tag == "plurals":
        plural_map[el.get("name")] = {it.get("quantity"): "".join(it.itertext()) for it in el}

fmt = re.compile(r"%\d+\$[sd]|%[sd]|%%")
DE = deref["strings"]

# 2. all keys present, not identical to DE, format-args match, no bare %
for key, val in data["strings"].items():
    if key not in str_map:
        problems.append(f"MISSING:{key}")
        continue
    actual = str_map[key]
    # identical-to-DE check (compare against DE source, normalizing \\n)
    de_src = DE.get(key, "").replace("\\n", "\n").replace('\\"', '"')
    if actual.strip() == de_src.strip():
        problems.append(f"IDENTICAL_TO_DE:{key}")
    # bare % check
    unguarded = fmt.sub("", actual).count("%")
    if unguarded != 0:
        problems.append(f"BARE_PERCENT:{key}")
    # format-arg parity vs DE
    de_args = sorted(re.findall(r"%\d+\$[sd]", de_src))
    ac_args = sorted(re.findall(r"%\d+\$[sd]", actual))
    if de_args != ac_args:
        problems.append(f"FMT_MISMATCH:{key} de={de_args} ko={ac_args}")

# 3. plurals present + 'other' exists + format parity
for pkey, forms in data["plurals"].items():
    if pkey not in plural_map:
        problems.append(f"PLURAL_MISSING:{pkey}")
        continue
    if "other" not in plural_map[pkey]:
        problems.append(f"PLURAL_NO_OTHER:{pkey}")
    for q, t in plural_map[pkey].items():
        if fmt.sub("", t).count("%") != 0:
            problems.append(f"PLURAL_BARE_PCT:{pkey}/{q}")
        if "%1$d" not in t:
            problems.append(f"PLURAL_NO_ARG:{pkey}/{q}")

print(json.dumps({
    "xml_valid": True,
    "total_strings_in_file": len(str_map),
    "total_plurals_in_file": len(plural_map),
    "checked_strings": len(data["strings"]),
    "checked_plurals": len(data["plurals"]),
    "problems": problems,
    "ok": len(problems) == 0,
}, ensure_ascii=False, indent=2))
