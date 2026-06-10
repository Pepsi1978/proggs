# -*- coding: utf-8 -*-
import json, os, re, xml.etree.ElementTree as ET

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
SHIELD = os.path.join(ROOT, ".android-shield")
RES = os.path.join(ROOT, "app/src/main/res")
LANGS = ["ja", "ko", "zh-rCN", "zh-rTW"]

# 1. Matrix slice for my languages
with open(os.path.join(SHIELD, "cross-lingual-matrix-2026-06-10.json"), encoding="utf-8") as f:
    matrix = json.load(f)
my_matrix = {l: matrix["locales"][l] for l in LANGS}

# 2. Legal keys
with open(os.path.join(SHIELD, "legal-keys-2026-06-10.json"), encoding="utf-8") as f:
    legal = json.load(f)["legalCriticalKeys"]
# Clean keys that have " / " composite notations -> take first real key token
clean_legal = []
for k in legal:
    # split off composite refs like "ai_prompt_insight_intro / profile_insight_*"
    base = k.split(" / ")[0].split(" ")[0].strip()
    clean_legal.append(base)
clean_legal = [k for k in clean_legal if re.match(r'^[a-z0-9_]+$', k)]

def parse_strings(path):
    """Return dict of name->text for <string> elements (resolve XML, keep raw text)."""
    d = {}
    if not os.path.exists(path):
        return d
    tree = ET.parse(path)
    rootx = tree.getroot()
    for el in rootx:
        if el.tag == "string":
            name = el.get("name")
            # full inner text incl. nested
            txt = "".join(el.itertext())
            d[name] = txt if txt is not None else ""
    return d

de = parse_strings(os.path.join(RES, "values/strings.xml"))
langdata = {l: parse_strings(os.path.join(RES, f"values-{l}/strings.xml")) for l in LANGS}

# Summary of matrix slice
summary = {}
for l in LANGS:
    m = my_matrix[l]
    summary[l] = {
        "keyCount": m["keyCount"],
        "missingCount": m["missingCount"],
        "missingKeys": m["missingKeys"],
        "identicalToDe": m["identicalToDe"],
        "identicalCount": m["identicalCount"],
        "formatArgMismatch": m["formatArgMismatch"],
        "nakedPercentKeys": m["nakedPercentKeys"],
        "unescapedApostrophes": m["unescapedApostrophes"],
        "missingPlurals": m.get("missingPlurals", []),
        "missingArrays": m.get("missingArrays", []),
    }

print("=== MATRIX SLICE (my 4 langs) ===")
print(json.dumps(summary, ensure_ascii=False, indent=1))

# Which legal keys are actually present in DE?
present_legal = [k for k in clean_legal if k in de]
missing_in_de = [k for k in clean_legal if k not in de]
print("\n=== LEGAL KEYS ===")
print("clean legal keys:", len(clean_legal), "| present in DE:", len(present_legal), "| not in DE:", missing_in_de)

# Save artifacts for the next steps
art = {
    "myMatrix": summary,
    "cleanLegalKeys": clean_legal,
    "presentLegalKeys": present_legal,
    "missingInDe": missing_in_de,
    "deCount": len(de),
    "langCounts": {l: len(langdata[l]) for l in LANGS},
}
with open(os.path.join(SHIELD, "worker-checkpoints", "l5-artifacts.json"), "w", encoding="utf-8") as f:
    json.dump(art, f, ensure_ascii=False, indent=1)
print("\nartifacts saved.")
