#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vollstaendigkeits-Check: Vergleicht ALLE Sprach-strings.xml gegen die deutsche
Referenz (values/strings.xml). Findet:
  1. FEHLENDE Keys      - in DE (translatable) vorhanden, in Sprache X nicht
  2. LEERE Werte        - Key da, aber Wert leer
  3. UNUEBERSETZT       - Wert identisch mit DE UND enthaelt echten Text (verdaechtig)
  4. VERMUTLICH OK      - Wert identisch mit DE, aber nur Platzhalter/Zahlen/Symbol (meist legitim)
Reine Analyse, schreibt NICHTS an den App-Strings.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
RES = os.path.abspath(RES)
DE_DIR = os.path.join(RES, "values")

# Reihenfolge der Sprach-Verzeichnisse
LANGS = [
    "values-en", "values-es", "values-fr", "values-it", "values-nl", "values-pl",
    "values-pt-rBR", "values-pt-rPT", "values-ru", "values-uk", "values-tr",
    "values-ar", "values-ur", "values-hi", "values-bn", "values-gu", "values-kn",
    "values-ml", "values-mr", "values-ta", "values-te", "values-th",
    "values-in", "values-ja", "values-ko", "values-zh-rCN", "values-zh-rTW",
]


def parse_strings(path):
    """Liefert (dict key->value, set_nicht_uebersetzbarer_keys). Robust gegen MULTILINE."""
    values = {}
    untranslatable = set()
    tree = ET.parse(path)
    root = tree.getroot()
    for el in root.findall("string"):
        name = el.get("name")
        if name is None:
            continue
        # Vollstaendigen inneren Text inkl. Tags zusammensetzen
        text = el.text or ""
        for child in el:
            text += ET.tostring(child, encoding="unicode")
            if child.tail:
                text += child.tail
        if el.get("translatable", "true").lower() == "false":
            untranslatable.add(name)
        values[name] = text.strip()
    return values, untranslatable


# Heuristik: identischer Wert ist VERMUTLICH legitim (kein echter Uebersetzungstext)
_PLACEHOLDER_ONLY = re.compile(r"^[\s\d%$.,:;!?/_\-+#@()\[\]{}<>|*=&~`'\"\\]*$")
# enthaelt mindestens 2 zusammenhaengende Buchstaben (echtes Wort)
_HAS_WORD = re.compile(r"[A-Za-zÀ-ÿ]{2,}")
# bekannte Marken / Eigennamen / Akronyme, die in vielen Sprachen gleich bleiben
BRANDS = {
    "bestjournal", "strava", "whoop", "oura", "polar", "garmin", "fitbit", "amazfit",
    "google", "gemini", "android", "ios", "pdf", "csv", "json", "url", "ok", "id",
    "premium", "gps", "vo2max", "vo2", "hrv", "rmssd", "spo2", "bpm", "km", "kg",
    "ftp", "api", "e-mail", "email", "app", "apps", "ki", "ai", "okay",
}


def classify_identical(val):
    """Gibt 'ok' wenn identischer Wert vermutlich legitim ist, sonst 'suspect'."""
    v = val.strip()
    if v == "":
        return "ok"  # leer wird separat gemeldet
    if _PLACEHOLDER_ONLY.match(v):
        return "ok"
    if not _HAS_WORD.search(v):
        return "ok"
    # ganzer Wert (klein) ist eine bekannte Marke / Akronym
    if v.lower() in BRANDS:
        return "ok"
    # sehr kurz und marken-artig (z.B. "Strava-ID")
    words = re.findall(r"[A-Za-zÀ-ÿ]+", v.lower())
    if words and all(w in BRANDS for w in words):
        return "ok"
    return "suspect"


def main():
    de_values, de_untrans = parse_strings(os.path.join(DE_DIR, "strings.xml"))
    de_translatable = {k: v for k, v in de_values.items() if k not in de_untrans}

    print("=" * 76)
    print("VOLLSTAENDIGKEITS-CHECK  —  BestJournal Android")
    print("=" * 76)
    print(f"DE-Referenz (values/strings.xml): {len(de_values)} Strings gesamt")
    print(f"  davon translatable=false (nicht zu uebersetzen): {len(de_untrans)}")
    print(f"  davon zu uebersetzen: {len(de_translatable)}")
    if de_untrans:
        print(f"  translatable=false Keys: {', '.join(sorted(de_untrans))}")
    print("=" * 76)

    grand_missing = 0
    grand_empty = 0
    grand_suspect = 0
    per_lang_summary = []

    for lang in LANGS:
        path = os.path.join(RES, lang, "strings.xml")
        if not os.path.isfile(path):
            print(f"\n### {lang}: DATEI FEHLT KOMPLETT!")
            per_lang_summary.append((lang, "FEHLT", "-", "-"))
            continue
        lv, _ = parse_strings(path)

        missing = [k for k in de_translatable if k not in lv]
        empty = [k for k in de_translatable if k in lv and lv[k].strip() == ""]
        suspect = []
        ok_identical = 0
        for k, dval in de_translatable.items():
            if k in lv and lv[k].strip() != "" and lv[k].strip() == dval.strip():
                if classify_identical(dval) == "suspect":
                    suspect.append(k)
                else:
                    ok_identical += 1

        grand_missing += len(missing)
        grand_empty += len(empty)
        grand_suspect += len(suspect)
        per_lang_summary.append((lang, len(missing), len(empty), len(suspect)))

        if missing or empty or suspect:
            print(f"\n### {lang}  —  fehlend:{len(missing)}  leer:{len(empty)}  "
                  f"verdaechtig-unuebersetzt:{len(suspect)}  (identisch-aber-ok:{ok_identical})")
            if missing:
                print(f"  FEHLENDE KEYS ({len(missing)}):")
                for k in missing:
                    print(f"    - {k}")
            if empty:
                print(f"  LEERE WERTE ({len(empty)}):")
                for k in empty:
                    print(f"    - {k}")
            if suspect:
                print(f"  VERDAECHTIG (Wert == DE, enthaelt echten Text) ({len(suspect)}):")
                for k in suspect:
                    snippet = de_translatable[k].replace("\n", " ")[:70]
                    print(f"    - {k}  ::  \"{snippet}\"")
        else:
            print(f"\n### {lang}  —  OK (vollstaendig, keine Auffaelligkeiten; "
                  f"identisch-aber-ok:{ok_identical})")

    print("\n" + "=" * 76)
    print("GESAMT-ZUSAMMENFASSUNG")
    print("=" * 76)
    print(f"{'Sprache':<16}{'fehlend':>10}{'leer':>8}{'verdaechtig':>14}")
    print("-" * 76)
    for lang, m, e, s in per_lang_summary:
        print(f"{lang:<16}{str(m):>10}{str(e):>8}{str(s):>14}")
    print("-" * 76)
    print(f"{'SUMME':<16}{str(grand_missing):>10}{str(grand_empty):>8}{str(grand_suspect):>14}")
    print("=" * 76)
    if grand_missing == 0 and grand_empty == 0 and grand_suspect == 0:
        print("ERGEBNIS: Alle 27 Sprachen vollstaendig — keine fehlenden, leeren "
              "oder unuebersetzten Strings gefunden.")
    else:
        print("ERGEBNIS: Auffaelligkeiten gefunden — siehe Details oben.")


if __name__ == "__main__":
    main()
