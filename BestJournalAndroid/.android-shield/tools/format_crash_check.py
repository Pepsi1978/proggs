#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PRAEZISER FIN-049 Format-String-Crash-Check.
Nur Strings die im Code via getString(id, ARG) / stringResource(id, ARG) aufgerufen
werden (= String.format aktiv) koennen durch ungeschuetzte % oder fehlende/verstuemmelte
Platzhalter crashen. Dieser Check:
  1. Findet ALLE format-aufgerufenen Keys im Kotlin-Code (Aufruf MIT Argument).
  2. Prueft pro Sprache fuer diese Keys:
     a) FIN-049: nach Entfernen von %%+Specifiern darf KEIN % uebrig sein (sonst Crash)
     b) Positionale Specifier-Menge identisch zu DE (kein verstuemmelter/fehlender Platzhalter)
Output: nur ECHTE Crash-Risiken.
"""
import os, re, json, glob
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(APP, "app/src/main/res")
JAVA = os.path.join(APP, "app/src/main/java")
OUT = os.path.join(APP, ".android-shield/format-crash-report.json")

# 1. Format-aufgerufene Keys aus Code finden: getString(R.string.KEY, ...) ODER stringResource(R.string.KEY, ...)
#    Das Komma nach dem Key signalisiert Argumente => String.format aktiv.
call_re = re.compile(r"(?:getString|stringResource|getQuantityString|pluralStringResource)\s*\(\s*R\.string\.([A-Za-z0-9_]+)\s*,")
format_keys = set()
for kt in glob.glob(os.path.join(JAVA, "**/*.kt"), recursive=True):
    try:
        with open(kt, encoding="utf-8") as f:
            src = f.read()
    except Exception:
        continue
    for m in call_re.finditer(src):
        format_keys.add(m.group(1))

# echte Specifier (positional + simpel)
SPEC_RE = re.compile(r"%\d+\$[sdfxX]|%[sdfxX]")
# FIN-049: nach Entfernen von %% UND allen Specifiern darf kein % uebrig sein
STRIP_RE = re.compile(r"%%|%\d+\$[sdfxX]|%[sdfxX]")

def specs(text):
    return sorted(SPEC_RE.findall(text))

def bare_pct(text):
    return STRIP_RE.sub("", text).count("%")

def load(path):
    try:
        root = ET.parse(path).getroot()
    except Exception as e:
        return None, str(e)
    return {el.get("name"): "".join(el.itertext()) for el in root if el.tag == "string"}, None

de, _ = load(os.path.join(RES, "values/strings.xml"))
# nur format-keys die wirklich in DE existieren
fkeys = sorted(k for k in format_keys if k in de)
print(f"Format-aufgerufene Keys (mit Args) gesamt: {len(format_keys)}, davon in strings.xml: {len(fkeys)}")
# DE-Baseline: welche format-keys haben in DE selbst schon ein nacktes %? (DE-Bug)
de_bare = {k: de[k] for k in fkeys if bare_pct(de[k]) > 0}
if de_bare:
    print(f"\n⚠️  DE-ORIGINAL hat ungeschuetztes % in {len(de_bare)} Format-Keys (DE-Bug, crasht ggf. schon auf Deutsch):")
    for k, v in de_bare.items():
        print(f"    {k}: {repr(v[:90])}")

LOCALE_DIRS = sorted(d.replace("values-", "") for d in os.listdir(RES)
                     if d.startswith("values-") and os.path.isfile(os.path.join(RES, d, "strings.xml")))

report = {"formatKeysCount": len(fkeys), "deBareKeys": list(de_bare.keys()), "locales": {}}
total_crash = 0
print("\n" + "=" * 70)
for loc in LOCALE_DIRS:
    tr, err = load(os.path.join(RES, f"values-{loc}/strings.xml"))
    if tr is None:
        report["locales"][loc] = {"xmlError": err}; print(f"[{loc:8}] XML-FEHLER {err}"); continue
    crashes = []   # ungeschuetztes % (FIN-049)
    specmis = []   # Specifier-Menge weicht von DE ab
    for k in fkeys:
        if k not in tr:
            continue
        dv, lv = de[k], tr[k]
        if bare_pct(lv) > 0 and bare_pct(dv) == 0:   # loc hat nacktes %, DE nicht => Uebersetzungsfehler
            crashes.append({"key": k, "loc": lv[:100]})
        if specs(dv) != specs(lv):
            specmis.append({"key": k, "de": specs(dv), "loc": specs(lv), "locText": lv[:100]})
    report["locales"][loc] = {"crashRiskBarePct": crashes, "specifierMismatch": specmis}
    n = len(crashes) + len(specmis)
    total_crash += n
    flag = "✅" if n == 0 else "🔴"
    print(f"[{loc:8}] {flag} crashRisk(%)={len(crashes)}  specifierMismatch={len(specmis)}")
    for c in crashes:
        print(f"            🔴 {c['key']}: {repr(c['loc'][:80])}")
    for s in specmis:
        print(f"            ⚠️  {s['key']}: DE={s['de']} LOC={s['loc']}")

report["totalCrashRiskFindings"] = total_crash
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(report, f, ensure_ascii=False, indent=2)
print("=" * 70)
print(f"ECHTE Format-Crash-Risiken gesamt: {total_crash}")
print(f"Report: {OUT}")
