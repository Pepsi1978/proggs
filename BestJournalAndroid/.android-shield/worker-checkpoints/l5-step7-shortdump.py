# -*- coding: utf-8 -*-
import json, os, sys, xml.etree.ElementTree as ET
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
with open(os.path.join(SHIELD, "worker-checkpoints", "l5-shortkeys.json"), encoding="utf-8") as f:
    short = json.load(f)["shortKeys"]

batch = int(sys.argv[1]) if len(sys.argv) > 1 else 0
SIZE = 30
chunk = short[batch*SIZE:(batch+1)*SIZE]
print(f"=== SHORT LEGAL KEYS batch {batch} ({len(chunk)} of {len(short)}) ===")
for k in chunk:
    print("-"*60)
    print(f"K {k}")
    print(f" DE| {de.get(k,'')}")
    for l in LANGS:
        print(f" {l:5}| {L[l].get(k,'<MISSING>')}")
