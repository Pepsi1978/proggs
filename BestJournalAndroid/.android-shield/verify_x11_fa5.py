# -*- coding: utf-8 -*-
"""Read-only Verifikation fuer FA5/X11. Schreibt nichts."""
import os
import sys
import json
import xml.etree.ElementTree as ET

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
HI = os.path.join(APP, "app/src/main/res/values-hi/strings.xml")
MR = os.path.join(APP, "app/src/main/res/values-mr/strings.xml")
OUT = os.path.join(APP, ".android-shield/worker-outputs/phase2-fa5.json")

PREF = ("json_key_", "json_value_")


def vals(p):
    r = ET.parse(p).getroot()
    return {e.get("name"): (e.text or "") for e in r.findall("string")
            if e.get("name", "").startswith(PREF)}


# Output-JSON
d = json.load(open(OUT, encoding="utf-8"))
print("=== phase2-fa5.json ===")
print("worker:", d["worker"], "| count:", d["count"],
      "| xmlValid:", d["validation"]["xmlValid"],
      "| plugin_bugs:", d["plugin_bugs_observed"])

# 1:1-Abgleich
hi = vals(HI)
mr = vals(MR)
diff = [k for k in hi if hi[k] != mr.get(k)]
print("\n=== Abgleich mr vs hi ===")
print("hi keys:", len(hi), "| mr keys:", len(mr), "| mismatches:", len(diff))
print("ALL MATCH:", len(diff) == 0 and set(hi) == set(mr))

# Stichprobe: zeigt dass mr jetzt romanisiert ist (ASCII)
print("\n=== Stichprobe (mr-Wert ist jetzt ASCII/romanisiert) ===")
for k in list(sorted(mr))[:5]:
    print(f"  {k} = {mr[k]} (ascii={mr[k].isascii()})")

# Gesamt: ist die ganze mr-Datei wohlgeformt?
try:
    ET.parse(MR)
    print("\nmr-Datei wohlgeformt: OK")
except ET.ParseError as e:
    print("\nmr-Datei PARSE-FEHLER:", e)
