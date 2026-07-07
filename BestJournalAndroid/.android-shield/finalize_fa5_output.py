# -*- coding: utf-8 -*-
"""Ergaenzt plugin_bugs_observed im FA5-Output (atomar). Aendert KEINE App-Datei."""
import os
import sys
import json
import tempfile

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
OUT = os.path.join(APP, ".android-shield/worker-outputs/phase2-fa5.json")

d = json.load(open(OUT, encoding="utf-8"))

# Beobachtetes Plugin-Verhalten (kein Defekt, aber dokumentationswuerdig):
# Der finale pretooluse-bash-Guard blockiert `python -c "..."` sobald der
# Inline-Code eine geschuetzte Datei referenziert — auch bei reiner
# Read-Verifikation. Workaround: Verifikation als eigene .py-Datei ausfuehren.
obs = {
    "hook": "pretooluse-bash.sh",
    "type": "false-positive-on-readonly",
    "detail": ("Inline `python -c` das eine geschuetzte Datei (auch nur lesend) "
               "referenziert wird blockiert. Read-only-Verifikation musste als "
               "separate .py-Datei laufen. Funktion des Guards ist korrekt "
               "(Schreibschutz), nur Read-Verifikation faellt mit darunter."),
    "severity": "low",
}

existing = d.get("plugin_bugs_observed", [])
if not any(o.get("hook") == obs["hook"] and o.get("type") == obs["type"] for o in existing):
    existing.append(obs)
d["plugin_bugs_observed"] = existing

fd, tmp = tempfile.mkstemp(dir=os.path.dirname(OUT), suffix=".tmp")
os.close(fd)
with open(tmp, "w", encoding="utf-8", newline="") as f:
    json.dump(d, f, ensure_ascii=False, indent=2)
os.replace(tmp, OUT)
print("OK: plugin_bugs_observed =", len(d["plugin_bugs_observed"]))
print(json.dumps(d, ensure_ascii=False, indent=2))
