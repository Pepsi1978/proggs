# -*- coding: utf-8 -*-
import json, os, xml.etree.ElementTree as ET

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(ROOT, "app/src/main/res")
SHIELD = os.path.join(ROOT, ".android-shield")
LANGS = ["ja", "ko", "zh-rCN", "zh-rTW"]

def parse_strings(path):
    d = {}
    for el in ET.parse(path).getroot():
        if el.tag == "string":
            d[el.get("name")] = "".join(el.itertext())
    return d

de = parse_strings(os.path.join(RES, "values/strings.xml"))
L = {l: parse_strings(os.path.join(RES, f"values-{l}/strings.xml")) for l in LANGS}

with open(os.path.join(SHIELD, "worker-checkpoints", "l5-artifacts.json"), encoding="utf-8") as f:
    present = json.load(f)["presentLegalKeys"]

# ai_prompt_* keys = internal LLM system prompts -> only length-ratio sanity check
prompt_keys = [k for k in present if k.startswith("ai_prompt") or k.startswith("profile_") or k.startswith("ai_improved")]
short_keys = [k for k in present if k not in prompt_keys]

print("=== AI-PROMPT / INTERNAL KEYS: length ratio (target chars / DE chars) ===")
print("(CJK is denser; ratio ~0.4-0.8 is normal. <0.3 = suspicious truncation)")
for k in prompt_keys:
    dl = len(de.get(k,""))
    if dl == 0:
        continue
    ratios = {l: round(len(L[l].get(k,""))/dl, 2) for l in LANGS}
    flag = " <== CHECK" if any(r < 0.3 for r in ratios.values()) else ""
    print(f"  {k} (de={dl}): {ratios}{flag}")

# For ai_prompt_response_language: must instruct the model to answer in the user's language
print("\n=== ai_prompt_response_language (critical: language instruction) ===")
for l in ["de"] + LANGS:
    src = de if l == "de" else L[l]
    print(f"  {l}: {src.get('ai_prompt_response_language','<MISSING>')[:300]}")

print("\n=== ai_prompt_no_dates_rule ===")
for l in ["de"] + LANGS:
    src = de if l == "de" else L[l]
    print(f"  {l}: {src.get('ai_prompt_no_dates_rule','<MISSING>')[:200]}")

# Save list of short keys for the dump-in-batches step
with open(os.path.join(SHIELD, "worker-checkpoints", "l5-shortkeys.json"), "w", encoding="utf-8") as f:
    json.dump({"shortKeys": short_keys, "promptKeys": prompt_keys}, f, ensure_ascii=False, indent=1)
print(f"\nshort user-facing legal keys to eyeball: {len(short_keys)} | internal prompt keys: {len(prompt_keys)}")
