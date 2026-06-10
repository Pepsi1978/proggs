# -*- coding: utf-8 -*-
import os, xml.etree.ElementTree as ET
ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(ROOT, "app/src/main/res")
LANGS = ["ja", "ko", "zh-rCN", "zh-rTW"]
def parse_strings(path):
    d = {}
    for el in ET.parse(path).getroot():
        if el.tag == "string":
            d[el.get("name")] = "".join(el.itertext())
    return d
de = parse_strings(os.path.join(RES, "values/strings.xml"))
L = {l: parse_strings(os.path.join(RES, f"values-{l}/strings.xml")) for l in LANGS}

# Check the "50 percent" core rule survives in custom_intro
print("=== ai_prompt_custom_intro: does the '50%' / profile-link rule survive? ===")
for l in ["de"]+LANGS:
    src = (de if l=="de" else L[l]).get("ai_prompt_custom_intro","")
    has50 = ("50" in src)
    print(f"  {l}: has '50'={has50} | len={len(src)}")
print("\n--- ja full custom_intro ---")
print(L["ja"].get("ai_prompt_custom_intro",""))
print("\n--- ko full custom_intro ---")
print(L["ko"].get("ai_prompt_custom_intro",""))
