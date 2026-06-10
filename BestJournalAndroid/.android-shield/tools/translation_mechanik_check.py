#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Deterministischer Uebersetzungs-Mechanik-Check fuer alle Locales.
Prueft pro Sprache (KEIN LLM noetig — rein mechanisch):
  1. Vollstaendigkeit: welche translatable DE-Keys fehlen in der Sprache?
  2. Unuebersetzt: welche translatable Keys sind identisch mit DE (durchgerutscht)?
  3. XML-Validitaet (ElementTree-Parse)
  4. Format-Arg-Erhalt: %1$s/%s/%d-Specifier in gleicher Menge wie DE?
  5. FIN-049: nackte/ungeschuetzte % in Format-Arg-Strings (Laufzeit-Crash-Risiko)
  6. <xliff:g>-Erhalt
Output: JSON nach .android-shield/translation-mechanik-report.json + lesbare Zusammenfassung.
"""
import os, re, json, sys
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
RES = os.path.join(APP, "app/src/main/res")
DE_FILE = os.path.join(RES, "values/strings.xml")
OUT = os.path.join(APP, ".android-shield/translation-mechanik-report.json")

# Android-Locale-Verzeichnisse (ohne values/ selbst)
LOCALE_DIRS = sorted(
    d.replace("values-", "")
    for d in os.listdir(RES)
    if d.startswith("values-") and os.path.isfile(os.path.join(RES, d, "strings.xml"))
)

# Format-Specifier: %1$s, %2$d, %s, %d, %f, %x, %% (escaped)
FMT_RE = re.compile(r"%(?:\d+\$)?[sdfxX]|%%")
# nacktes % das KEIN gueltiger Specifier und KEIN %% ist
BARE_PCT_RE = re.compile(r"%(?!(?:\d+\$)?[sdfxX]|%)")

def load_strings(path):
    """Gibt (ok, dict[key]->{'text':..,'translatable':bool,'xliff':int,'raw':..}, error)."""
    try:
        tree = ET.parse(path)
    except Exception as e:
        return False, {}, f"XML-PARSE-FEHLER: {e}"
    root = tree.getroot()
    out = {}
    for el in root:
        if el.tag != "string":
            continue
        name = el.get("name")
        if not name:
            continue
        translatable = el.get("translatable", "true").lower() != "false"
        # voller Textinhalt inkl. xliff:g-Inhalte
        text = "".join(el.itertext())
        xliff = len(el.findall(".//{urn:oasis:names:tc:xliff:document:1.2}g")) + \
                len([c for c in el.iter() if c.tag.endswith("}g") or c.tag == "g"])
        out[name] = {
            "text": (text or "").strip(),
            "translatable": translatable,
            "xliff": xliff,
        }
    return True, out, None

def fmt_set(text):
    return sorted(FMT_RE.findall(text))

def main():
    ok, de, err = load_strings(DE_FILE)
    if not ok:
        print("FATAL: DE-Referenz nicht lesbar:", err)
        sys.exit(2)
    # nur translatable DE-keys sind Pflicht-Ziel
    de_keys = {k for k, v in de.items() if v["translatable"]}
    print(f"DE-Referenz: {len(de)} strings, davon {len(de_keys)} translatable")
    print(f"Locales: {len(LOCALE_DIRS)} -> {', '.join(LOCALE_DIRS)}")
    print("=" * 70)

    report = {"deTranslatableKeys": len(de_keys), "locales": {}}
    grand_problems = 0

    for loc in LOCALE_DIRS:
        path = os.path.join(RES, f"values-{loc}/strings.xml")
        ok, tr, err = load_strings(path)
        entry = {
            "xmlValid": ok, "xmlError": err,
            "missing": [], "untranslated": [], "fmtMismatch": [],
            "barePct": [], "xliffLost": [],
            "translatedCount": 0,
        }
        if not ok:
            entry["status"] = "XML-INVALID"
            report["locales"][loc] = entry
            print(f"[{loc:8}] ❌ XML INVALID: {err}")
            grand_problems += 1
            continue

        present = set(tr.keys())
        entry["translatedCount"] = len(present & de_keys)

        # 1. fehlende keys
        missing = de_keys - present
        entry["missing"] = sorted(missing)

        # pro vorhandenem translatable key pruefen
        for k in (de_keys & present):
            de_v = de[k]; loc_v = tr[k]
            dt, lt = de_v["text"], loc_v["text"]
            # 2. unuebersetzt (identisch, nicht-trivial)
            if dt and lt and dt == lt:
                # Heuristik: kurze (<=3 Zeichen) / reine Zahlen / reine Symbole ignorieren
                core = re.sub(r"[\s\d%$.,:;!?\-_/()]+", "", dt)
                if len(core) > 3:
                    entry["untranslated"].append(k)
            # 4. Format-Arg-Erhalt
            if fmt_set(dt) != fmt_set(lt):
                entry["fmtMismatch"].append({"key": k, "de": fmt_set(dt), "loc": fmt_set(lt)})
            # 5. FIN-049: nacktes % nur relevant wenn DE Format-Args hat (String.format-Nutzung)
            if FMT_RE.search(dt) and BARE_PCT_RE.search(lt):
                entry["barePct"].append({"key": k, "loc_text": lt[:80]})
            # 6. xliff:g-Erhalt
            if de_v["xliff"] > 0 and loc_v["xliff"] < de_v["xliff"]:
                entry["xliffLost"].append({"key": k, "de": de_v["xliff"], "loc": loc_v["xliff"]})

        nprob = (len(entry["missing"]) + len(entry["untranslated"]) +
                 len(entry["fmtMismatch"]) + len(entry["barePct"]) + len(entry["xliffLost"]))
        entry["problemCount"] = nprob
        entry["status"] = "OK" if nprob == 0 else "PROBLEME"
        report["locales"][loc] = entry
        grand_problems += nprob

        flag = "✅" if nprob == 0 else "⚠️"
        print(f"[{loc:8}] {flag} {entry['translatedCount']}/{len(de_keys)} keys | "
              f"fehlend={len(entry['missing'])} unuebers={len(entry['untranslated'])} "
              f"fmt={len(entry['fmtMismatch'])} barePct={len(entry['barePct'])} xliff={len(entry['xliffLost'])}")

    report["grandProblemCount"] = grand_problems
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print("=" * 70)
    print(f"GESAMT-Probleme ueber alle Sprachen: {grand_problems}")
    print(f"Report: {OUT}")

if __name__ == "__main__":
    main()
