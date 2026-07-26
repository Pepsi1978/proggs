#!/usr/bin/env python3
"""Vergleicht die alte committe ZIP-SKILL.md mit der aktuellen skills-src-Quelle."""
import difflib
import io
import re
import subprocess
import sys
import zipfile

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

raw = subprocess.run(
    ["git", "-C", "C:/Users/barwa/proggs", "show", "HEAD:Cowork/bug-almanach-recherche.zip"],
    capture_output=True).stdout
z = zipfile.ZipFile(io.BytesIO(raw))
old = z.read("bug-almanach-recherche/SKILL.md").decode("utf-8")
with open("C:/Users/barwa/proggs/Cowork/skills-src/bug-almanach-recherche/SKILL.md",
          encoding="utf-8") as f:
    new = f.read()

old_pat = set(re.findall(r"projekt-code|best-practices-[a-z]", old))
new_pat = set(re.findall(r"projekt-code|best-practices-[a-z]", new))
print(f"alte ZIP-SKILL.md: {len(old.splitlines())} Zeilen, {len(old)} Zeichen")
print(f"aktuelle Quelle:   {len(new.splitlines())} Zeilen, {len(new)} Zeichen")
print(f"identisch:         {old == new}")
print(f"ALTE best-practices-Muster in der alten ZIP: {old_pat or 'KEINE'}")
print(f"ALTE best-practices-Muster in der Quelle:    {new_pat or 'KEINE'}")

# alte best-practices-Pfad-Erwaehnungen woertlich
print("\n--- best-practices-Pfade in der ALTEN ZIP-SKILL.md ---")
for ln in old.splitlines():
    for m in re.findall(r"best-practices[/-][A-Za-z0-9/<>_.-]*", ln):
        print(f"  {m}")

diff = [d for d in difflib.unified_diff(old.splitlines(), new.splitlines(), lineterm="", n=0)
        if d.startswith(("+", "-")) and not d.startswith(("+++", "---"))]
print(f"\n--- geaenderte Zeilen gesamt: {len(diff)} (erste 20) ---")
for d in diff[:20]:
    print(d[:120])
