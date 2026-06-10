# -*- coding: utf-8 -*-
import os, random, re, xml.etree.ElementTree as ET
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

# pool: keys present in all langs, length 15-120 (skip huge prompts + tiny labels)
pool = [k for k in de if all(k in L[l] for l in LANGS) and 15 <= len(de[k]) <= 120
        and not k.startswith("ai_prompt") and not k.startswith("dev_seed")]
random.seed(55)
sample = random.sample(pool, 15)
print("=== RANDOM 15-STRING SAMPLE (naturalness + punctuation) ===")
for k in sample:
    print("-"*58)
    print(f"K {k}")
    print(f" DE| {de[k]}")
    for l in LANGS:
        print(f" {l:5}| {L[l][k]}")

# Latin-punctuation-in-CJK detector: ASCII . or , directly between CJK chars
print("\n=== ASCII '.'/',' between CJK chars (should use 。、in ja, ，。in zh) ===")
cjk = r'[぀-ヿ一-鿿가-힯]'
for l in LANGS:
    bad = []
    for k, v in L[l].items():
        # ASCII period/comma flanked by CJK (not in numbers, not %s)
        if re.search(cjk + r'\.' + cjk, v) or re.search(cjk + r',' + cjk, v):
            bad.append(k)
    print(f"{l}: {len(bad)} keys with ASCII .,  between CJK -> {bad[:8]}")

# \n inside a CJK token without surrounding space (info only; CJK has no spaces so this is normal)
print("\n=== keys where \\n appears mid-line in legal-ish content (info) ===")
for l in LANGS:
    cnt = sum(1 for v in L[l].values() if "\\n" in v)
    print(f"{l}: {cnt} keys contain literal backslash-n")
