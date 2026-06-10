#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Ersetzt komplette <plurals>/<string-array>-Bloecke in values-gu/strings.xml.
Input-JSON: {"plurals": {name: {quantity: text}}, "arrays": {name: [items]}}"""
import os, re, sys, json
APP = os.path.expanduser("~/proggs/BestJournalAndroid")
GU = os.path.join(APP, "app/src/main/res/values-gu/strings.xml")

def esc(s):
    s = re.sub(r"&(?!(amp|lt|gt|quot|apos|#\d+|#x[0-9A-Fa-f]+);)", "&amp;", s)
    s = s.replace("<", "&lt;").replace(">", "&gt;")
    s = re.sub(r"(?<!\\)'", r"\\'", s)
    s = re.sub(r'(?<!\\)"', r'\\"', s)
    return s

def main():
    data = json.load(open(sys.argv[1], encoding="utf-8"))
    content = open(GU, encoding="utf-8").read()
    ok = 0
    for name, items in data.get("plurals", {}).items():
        inner = "\n".join(f'        <item quantity="{q}">{esc(v)}</item>' for q, v in items.items())
        block = f'<plurals name="{name}">\n{inner}\n    </plurals>'
        pat = re.compile(r'<plurals name="' + re.escape(name) + r'">.*?</plurals>', re.DOTALL)
        content, n = pat.subn(lambda m: block, content)
        if n == 1: ok += 1
        else: print(f"  WARN plural {name}: {n} Treffer")
    for name, items in data.get("arrays", {}).items():
        inner = "\n".join(f'        <item>{esc(v)}</item>' for v in items)
        block = f'<string-array name="{name}">\n{inner}\n    </string-array>'
        pat = re.compile(r'<string-array name="' + re.escape(name) + r'">.*?</string-array>', re.DOTALL)
        content, n = pat.subn(lambda m: block, content)
        if n == 1: ok += 1
        else: print(f"  WARN array {name}: {n} Treffer")
    tmp = GU + ".tmp"
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.replace(tmp, GU)
    print(f"Angewendet: {ok} Bloecke")

if __name__ == "__main__":
    main()
