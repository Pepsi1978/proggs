#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Wendet Gujarati-Uebersetzungen chirurgisch auf values-gu/strings.xml an.
Liest ein JSON {key: raw_gujarati_text} (roh, mit literalem \\n als 2 Zeichen + %1$s erhalten),
escaped XML-/Android-korrekt und ersetzt NUR den Inhalt der betroffenen <string>-Elemente.
Alle anderen Strings bleiben byte-identisch (minimaler git-Diff).
Aufruf: python3 apply_gu_translations.py <translations.json>
"""
import os, re, sys, json

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
GU = os.path.join(APP, "app/src/main/res/values-gu/strings.xml")

def esc(s):
    # XML-Escapes (& zuerst, aber bestehende Entities nicht doppeln)
    s = re.sub(r"&(?!(amp|lt|gt|quot|apos|#\d+|#x[0-9A-Fa-f]+);)", "&amp;", s)
    s = s.replace("<", "&lt;").replace(">", "&gt;")
    # Android: Apostroph + doppelte Quote escapen (falls nicht schon escaped)
    s = re.sub(r"(?<!\\)'", r"\\'", s)
    s = re.sub(r'(?<!\\)"', r'\\"', s)
    return s

def main():
    trans = json.load(open(sys.argv[1], encoding="utf-8"))
    with open(GU, encoding="utf-8") as f:
        content = f.read()
    ok, warn = 0, []
    for key, raw in trans.items():
        val = esc(raw)
        pat = re.compile(r'(<string name="' + re.escape(key) + r'"[^>]*>).*?(</string>)', re.DOTALL)
        new, n = pat.subn(lambda m: m.group(1) + val + m.group(2), content)
        if n == 1:
            content = new; ok += 1
        else:
            warn.append(f"{key}: {n} Treffer (erwartet 1)")
            print(f"  WARN {key}: {n} Treffer (erwartet 1)")
    # atomar schreiben
    tmp = GU + ".tmp"
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.replace(tmp, GU)
    print(f"Angewendet: {ok}/{len(trans)} Strings")

if __name__ == "__main__":
    main()
