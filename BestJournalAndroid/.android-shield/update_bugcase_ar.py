#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Update or append the auto-captured bug-case for the re.subn backslash issue."""
import json
import os

path = os.path.join(os.path.dirname(__file__), "..", "..", ".claude", "agent-memory", "shared", "bug-cases.jsonl")
path = os.path.normpath(path)

root_cause = (
    "re.subn() treats backslashes in string replacement arguments as regex escape sequences. "
    "Arabic strings contain Unicode characters whose Python repr uses backslash-u notation, "
    "which the regex engine tries to parse as regex escapes and raises PatternError."
)
fix = (
    "Pass a callable (lambda or inner function) as the repl parameter to re.subn() instead of a string. "
    "The callable receives the match object and returns the replacement directly, "
    "bypassing regex escape interpretation entirely. "
    "Pattern: pattern.subn(lambda m: m.group(1) + value + m.group(3), content)"
)

lines = open(path, encoding="utf-8").readlines()
updated_lines = []
found = False

for line in lines:
    line = line.rstrip()
    if not line:
        updated_lines.append(line)
        continue
    try:
        obj = json.loads(line)
        symptom = obj.get("symptom", "")
        if obj.get("auto_captured") and ("bad escape" in symptom or "subn" in symptom or "unicode" in symptom.lower()):
            obj["root_cause"] = root_cause
            obj["fix"] = fix
            obj["auto_captured"] = False
            found = True
        updated_lines.append(json.dumps(obj, ensure_ascii=False))
    except Exception:
        updated_lines.append(line)

if not found:
    new_entry = {
        "date": "2026-05-26",
        "symptom": "re.subn PatternError: bad escape backslash-u in replacement string",
        "root_cause": root_cause,
        "fix": fix,
        "files": ["translate_ar.py"],
        "tags": ["python", "regex", "arabic", "unicode", "backslash"],
        "severity": "mittel",
        "auto_captured": False,
    }
    updated_lines.append(json.dumps(new_entry, ensure_ascii=False))
    print("New bug-case entry appended.")
else:
    print("Auto-captured bug-case entry updated with root_cause and fix.")

with open(path, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(updated_lines) + "\n")
