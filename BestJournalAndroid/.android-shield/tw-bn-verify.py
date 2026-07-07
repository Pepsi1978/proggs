# -*- coding: utf-8 -*-
"""Post-verification for tw-bn (FIN-051c)."""
import os, re, json
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
TARGET = os.path.join(APP, "app/src/main/res/values-bn/strings.xml")
DEREF = os.path.join(APP, ".android-shield/translation-jobs/de-reference.json")
JOB = os.path.join(APP, ".android-shield/translation-jobs/job-plan.json")

with open(DEREF, "r", encoding="utf-8") as f:
    deref = json.load(f)
with open(JOB, "r", encoding="utf-8") as f:
    job = json.load(f)["languages"]["bn"]

expected_strings = job["strings"]
expected_plurals = job["plurals"]

# 1. XML valid
errs = []
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
    xml_valid = True
except Exception as e:
    xml_valid = False
    errs.append(f"XML parse error: {e}")
    root = None

present_strings = {}
present_plurals = {}
if root is not None:
    for el in root.findall("string"):
        present_strings[el.get("name")] = "".join(el.itertext())
    for el in root.findall("plurals"):
        items = {it.get("quantity"): "".join(it.itertext()) for it in el.findall("item")}
        present_plurals[el.get("name")] = items

# 2. all keys present
missing_strings = [k for k in expected_strings if k not in present_strings]
missing_plurals = [k for k in expected_plurals if k not in present_plurals]

# 3. none identical to DE
de_strings = deref["strings"]
identical = []
for k in expected_strings:
    if k in present_strings and k in de_strings:
        # compare unescaped \n-normalized; ElementTree gives real newlines, de has literal \\n
        de_norm = de_strings[k].replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'")
        if present_strings[k].strip() == de_norm.strip():
            identical.append(k)

# 4. format args identical (count of %1$s/%1$d etc + %%)
fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')
def fmt_sig(s):
    return sorted(re.findall(r'%\d+\$[sd]', s))
fmt_mismatch = []
for k in expected_strings:
    if k in present_strings and k in de_strings:
        de_raw = de_strings[k]
        bn_raw = present_strings[k]
        # ElementTree resolved entities; for placeholders count positional ones
        if fmt_sig(de_raw) != fmt_sig(bn_raw):
            fmt_mismatch.append({"key": k, "de": fmt_sig(de_raw), "bn": fmt_sig(bn_raw)})

# plurals format args
de_plurals = deref["plurals"]
for pk in expected_plurals:
    if pk in present_plurals and pk in de_plurals:
        for q, txt in de_plurals[pk].items():
            if q in present_plurals[pk]:
                if fmt_sig(txt) != fmt_sig(present_plurals[pk][q]):
                    fmt_mismatch.append({"key": f"{pk}/{q}", "de": fmt_sig(txt), "bn": fmt_sig(present_plurals[pk][q])})

# 5. no bare % (unguarded)
bare_pct = []
for k, v in present_strings.items():
    # re-escape: ElementTree gives resolved text; placeholders still literal %1$s
    stripped = fmt.sub('', v)
    if stripped.count('%') > 0:
        bare_pct.append(k)
for pk, items in present_plurals.items():
    for q, v in items.items():
        if fmt.sub('', v).count('%') > 0:
            bare_pct.append(f"{pk}/{q}")

result = {
    "xml_valid": xml_valid,
    "expected_strings": len(expected_strings),
    "present_strings_total": len(present_strings),
    "missing_strings": missing_strings,
    "missing_plurals": missing_plurals,
    "identical_to_de": identical,
    "fmt_mismatch": fmt_mismatch,
    "bare_percent": bare_pct,
    "errors": errs,
    "all_target_keys_present": len(missing_strings) == 0 and len(missing_plurals) == 0,
}
print(json.dumps(result, ensure_ascii=False, indent=1))
PASS = (xml_valid and not missing_strings and not missing_plurals
        and not identical and not fmt_mismatch and not bare_pct)
print("VERIFY_PASS:", PASS)
