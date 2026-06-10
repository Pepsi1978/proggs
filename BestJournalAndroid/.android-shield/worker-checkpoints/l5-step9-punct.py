# -*- coding: utf-8 -*-
import os, re, xml.etree.ElementTree as ET
ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(ROOT, "app/src/main/res")
LANGS = ["zh-rCN", "zh-rTW", "ja", "ko"]
def parse_strings(path):
    d = {}
    for el in ET.parse(path).getroot():
        if el.tag == "string":
            d[el.get("name")] = "".join(el.itertext())
    return d
L = {l: parse_strings(os.path.join(RES, f"values-{l}/strings.xml")) for l in LANGS}

cjk = r'[一-鿿぀-ヿ가-힯]'
# USER-FACING only: exclude ai_prompt_* (internal JSON schema) -> ASCII , or . adjacent to CJK
print("=== USER-FACING zh keys with ASCII ',' or '.' adjacent to CJK (cosmetic) ===")
for l in ["zh-rCN", "zh-rTW"]:
    hits = []
    for k, v in L[l].items():
        if k.startswith("ai_prompt"):
            continue
        # ASCII comma/period with a CJK char on at least one side, excluding decimal numbers & %s & version dots
        for m in re.finditer(r'[,\.]', v):
            i = m.start()
            left = v[i-1] if i > 0 else ""
            right = v[i+1] if i+1 < len(v) else ""
            if re.match(cjk, left) or re.match(cjk, right):
                # skip if part of number like 3.99 or %1$s
                if v[i] == '.' and left.isdigit() and right.isdigit():
                    continue
                hits.append((k, v[max(0,i-6):i+6]))
                break
    print(f"\n--{l}: {len(hits)} user-facing keys--")
    for k, ctx in hits[:25]:
        print(f"   {k}: …{ctx}…")
