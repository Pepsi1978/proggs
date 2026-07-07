#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""L3: write final phase1b-xling-L3.json (atomic)."""
import os, sys, json, tempfile

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.expanduser('~/proggs/BestJournalAndroid')
OUT = os.path.join(ROOT, '.android-shield/worker-outputs/phase1b-xling-L3.json')

data = {
  "worker": "L3-cross-lingual-legal",
  "languages": ["gu", "kn", "ml"],
  "date": "2026-06-10",
  "scope": "AUDIT-ONLY (keine App-Datei geaendert)",

  "guCorruptionScan": {
    "scanned": 1170,
    "stringNodes": 1087,
    "pluralItems": 12,
    "arrayItems": 71,
    "hardCorruptionSignals": {
      "replacementCharFFFD": 0,
      "geometricShapes": 0,
      "boxDrawingMisuse": 0,
      "blockElements": 0,
      "emptyValuesGuOnly": 0
    },
    "suspicious": [
      {"key": "ai_limits_dialog_body", "reason": "box-drawing '─────' divider — ABER identisch in DE vorhanden (absichtliche Formatierung, KEINE Korruption)"},
      {"key": "ai_prompt_language_override", "reason": "leer — ABER DE ist ebenfalls leer (legitim, in allen Sprachen leer)"},
      {"key": "action_undo", "reason": "endet auf Halant U+0ACD — ABER 'પૂર્વવત્' ist ein korrektes Gujarati-Wort (false positive)"}
    ],
    "verdict": "sauber",
    "verdictDetail": "VOLLPRUEFUNG bestanden. Null harte Korruptions-Signaturen (FFFD/Geometric/Box/Block) in allen 1170 gu-Knoten inkl. 12 Plural-Items + 71 Array-Items (64 Prompt-Items). Der 2026-Korruptions-Vorfall (153 Strings, Fix #1288) ist VOLLSTAENDIG repariert und verifiziert. Die 3 'verdaechtigen' Treffer sind alle false positives (in DE ebenso vorhanden bzw. valide Orthographie)."
  },

  "perLanguage": {
    "gu": {
      "keyCount": 1085, "deKeyCount": 1090, "missingCount": 5, "extraCount": 0,
      "missingKeys": ["dev_seed_add_action","dev_seed_cancel_action","dev_seed_delete_all_action","dev_seed_dialog_message","dev_seed_dialog_title"],
      "missingKeysVerdict": "Nur dev_seed_* (Entwickler-Seed, nicht user-facing) — bekannt/erwartet, kein Release-Problem.",
      "identicalToDe": {"total": 33, "jsonKeyOrValue": 27, "brandOrUiToken": 6, "realUntranslated": 0,
        "brandTokens": ["achievements_title=Achievements","consent_footer_link_impressum=Impressum","consent_info_system_backup_title=Android System-Backup","label_premium=Premium","settings_about_version=Best Journal V0.21.9","settings_premium_section=Premium"],
        "verdict": "Alle 33 erklaert — KEIN echtes unuebersetztes Prosa-String. Die hohe Zahl = json_key/value-Tokens (muessen verbatim bleiben) + Markennamen."},
      "formatArgMismatch": 0, "nakedPercent": 0, "unescapedApostrophes": 0,
      "placeholderXliffMismatches": 0,
      "issues": [
        {"sev": "low", "key": "settings_revoke_subtitle", "issue": "DE='§ 356a BGB — Widerruf direkt per App' -> gu='Premium ખરીદી' (Premium-Kauf). §356a-Bezug entfaellt. Systematisch (gu/kn/ml gleich). Geschwister settings_revoke_email_subject/body korrekt als Widerruf uebersetzt. Da gu Nicht-DE-Markt: neutrales Label vertretbar, aber Inkonsistenz zur DE-Rechtsframing.", "legalKey": True},
        {"sev": "low", "key": "ai_prompt_no_dates_rule", "issue": "Datums-BEISPIELE leicht verschieden (de hat 1x mehr Wiederholung). Inhalt = AI-System-Prompt (nicht user-facing), die Regel selbst ist intakt.", "legalKey": True},
        {"sev": "low", "key": "ai_prompt_insight_* / ai_retro_*", "issue": "QUALITAETS-Hinweis: gu nutzt ROMANISIERTES Gujarati (Latein-Schrift, z.B. 'THAMAARI DRISHTIKON', 'NIYAMO') statt Devanagari-aehnlicher Gujarati-Schrift. Betrifft NUR interne AI-Prompts (nicht UI). kn/ml nutzen native Schrift. Funktional ok, aber inkonsistent.", "legalKey": True}
      ],
      "verdict": "SAUBER. Vollstaendigkeit ok (nur dev_seed_* fehlen), keine Korruption, keine Platzhalter-Fehler, identicalToDe vollstaendig erklaert."
    },

    "kn": {
      "keyCount": 1085, "deKeyCount": 1090, "missingCount": 5, "extraCount": 0,
      "missingKeys": ["dev_seed_add_action","dev_seed_cancel_action","dev_seed_delete_all_action","dev_seed_dialog_message","dev_seed_dialog_title"],
      "missingKeysVerdict": "Nur dev_seed_* — bekannt/erwartet.",
      "identicalToDe": {"total": 3, "jsonKeyOrValue": 0, "brandOrUiToken": 3, "realUntranslated": 0,
        "brandTokens": ["label_premium=Premium","settings_about_version=Best Journal V0.21.9","settings_premium_section=Premium"],
        "verdict": "Sauberste Sprache — nur 3 Marken-Tokens identisch. Kein unuebersetztes Prosa."},
      "formatArgMismatch": 0, "nakedPercent": 0, "unescapedApostrophes": 0,
      "placeholderXliffMismatches": 0,
      "issues": [
        {"sev": "medium", "key": "ai_limits_dialog_body", "issue": "INHALTS-AUSLASSUNG: kn enthaelt nur '150,600' statt '150,600,150' — der Satzteil 'plus 150/Tag fuer jedes eigene Profil das du erstellst' FEHLT. gu+ml vollstaendig. Premium-Kontingent-Disclosure unvollstaendig (kn-Nutzer erfaehrt nicht das per-Profil-Kontingent fuer eigene Profile).", "legalKey": True, "deNumbers": ["150","4","600","150"], "knNumbers": ["150","4","600"]},
        {"sev": "low", "key": "settings_revoke_subtitle", "issue": "Wie gu: DE §356a BGB -> kn 'Premium ಖರೀದಿ'. Systematisch.", "legalKey": True},
        {"sev": "low", "key": "privacy_gate_gemini_body", "issue": "'Art. 9 DSGVO'-Zitat entfaellt in kn; sensible-Daten-Beispiele (Gesundheit/Religion/Beziehungen) + Einwilligungs-Sinn BLEIBEN intakt. Matrix-'200,200' war false positive (= \\u200c ZWNJ). Zusatz: 'KI' (deutsche Abk.) statt lokalisiert.", "legalKey": True},
        {"sev": "low", "key": "ai_prompt_no_dates_rule", "issue": "Wie gu — Datums-Beispiele leicht reduziert, Regel intakt (AI-Prompt).", "legalKey": True}
      ],
      "verdict": "Fast sauber. EIN echter Punkt: ai_limits_dialog_body Kontingent-Auslassung (medium). Rest erklaert/false-positive."
    },

    "ml": {
      "keyCount": 1085, "deKeyCount": 1090, "missingCount": 5, "extraCount": 0,
      "missingKeys": ["dev_seed_add_action","dev_seed_cancel_action","dev_seed_delete_all_action","dev_seed_dialog_message","dev_seed_dialog_title"],
      "missingKeysVerdict": "Nur dev_seed_* — bekannt/erwartet.",
      "identicalToDe": {"total": 32, "jsonKeyOrValue": 27, "brandOrUiToken": 5, "realUntranslated": 0,
        "brandTokens": ["achievements_title=Achievements","consent_footer_link_impressum=Impressum","label_premium=Premium","settings_about_version=Best Journal V0.21.9","settings_premium_section=Premium"],
        "verdict": "Alle 32 erklaert (27 json_key/value + 5 Marken-Tokens). Kein echtes unuebersetztes Prosa."},
      "formatArgMismatch": 0, "nakedPercent": 0, "unescapedApostrophes": 0,
      "placeholderXliffMismatches": 0,
      "issues": [
        {"sev": "low", "key": "settings_revoke_subtitle", "issue": "Wie gu/kn: DE §356a BGB -> ml 'Premium വാങ്ങൽ'. Systematisch.", "legalKey": True},
        {"sev": "low", "key": "ai_prompt_no_dates_rule", "issue": "Wie gu — Datums-Beispiele leicht reduziert, Regel intakt (AI-Prompt).", "legalKey": True}
      ],
      "verdict": "SAUBER. ml nutzt durchgaengig native Malayalam-Schrift (auch in AI-Prompts), bessere Schrift-Konsistenz als gu. Keine Korruption, keine Platzhalter-Fehler."
    }
  },

  "legalKeysCheck": {
    "totalLegalKeys": 95,
    "checkedAsPlainStringInDe": 94,
    "notPlainString": 1,
    "zahlenCheck_ai_limits_dialog_body": {
      "expected": ["150","600","30","101","151","50","5"],
      "de": {"150": 4, "600": 1, "30": 2, "101": 2, "151": 1, "50": 1, "5": 3},
      "gu": {"150": 4, "600": 1, "30": 2, "101": 2, "151": 1, "50": 1, "5": 3, "verdict": "VOLLSTAENDIG — alle Zahlen wie DE"},
      "ml": {"150": 4, "600": 1, "30": 2, "101": 2, "151": 1, "50": 1, "5": 3, "verdict": "VOLLSTAENDIG — alle Zahlen wie DE"},
      "kn": {"150": 3, "600": 1, "30": 2, "101": 2, "151": 1, "50": 1, "5": 3, "verdict": "🟨 UNVOLLSTAENDIG — eine '150' fehlt (per-Profil-Kontingent-Klausel ausgelassen)"}
    },
    "crossLanguageLegalFindings": [
      {"sev": "medium", "affects": ["kn"], "key": "ai_limits_dialog_body", "summary": "kn laesst die '150/Tag pro eigenem Profil'-Klausel aus -> unvollstaendige Premium-Kontingent-Disclosure"},
      {"sev": "low", "affects": ["gu","kn","ml"], "key": "settings_revoke_subtitle", "summary": "§356a-BGB-Untertitel in allen 3 als 'Premium-Kauf' uebersetzt (Bezug entfaellt; Geschwister-Widerrufstexte korrekt)"},
      {"sev": "low", "affects": ["kn"], "key": "privacy_gate_gemini_body", "summary": "'Art. 9 DSGVO'-Zitat entfaellt in kn (Consent-Sinn + sensible-Daten-Beispiele intakt)"}
    ]
  },

  "summary": {
    "guCorruptionVerdict": "SAUBER — #1288-Reparatur vollstaendig verifiziert (0 harte Korruptionssignale in 1170 Knoten)",
    "overallVerdict": "Alle 3 Sprachen release-tauglich. Keine Korruption, keine fehlenden user-facing Keys, keine Platzhalter-/xliff-Fehler, identicalToDe vollstaendig erklaert (kein Padding mit Deutsch).",
    "realIssuesCount": 1,
    "realIssues": ["kn ai_limits_dialog_body: fehlende Kontingent-Klausel (medium)"],
    "systematicLowFindings": ["settings_revoke_subtitle §356a-Label in allen 3", "gu AI-Prompt-Transliteration (Latein statt Gujarati-Schrift)"],
    "falsePositivesResolved": ["matrix kn '200,200' = \\u200c ZWNJ", "transcription_model_groq Markenname", "follow_up_delete_confirm_text Zahlen = xliff-attribute", "action_undo Halant = valides Wort", "ai_limits_dialog_body box-drawing = DE ebenso"]
  },

  "selbstbeobachtung": [
    "Die Korruptions-Heuristik (latin_fragment + low-ratio) erzeugte 163 Roh-Treffer, davon ~160 false positives (Marken, Transliteration, xliff-Attribute). Lehre: bei dichten indischen Schriften ZUERST nur die harten Signaturen (FFFD/Geometric/Box/Block) als Korruptions-Beweis werten — Schrift-Anteil und Latein-Fragmente sind Qualitaets-, keine Korruptionssignale.",
    "Der Zahlen-Regex fing \\u200c (ZWNJ) als '200' -> erzeugte den scheinbaren kn-'200,200'-Treffer. Bei Zahlen-Checks in Sprachen mit ZWNJ/ZWJ die \\uXXXX-Escapes vor dem \\d+-Match strippen.",
    "Der finale-Plugin-Hook blockiert python -c mit Datei-Referenz korrekt (geschuetzte Dateien) — die .py-via-Write-Regel aus dem Prompt war essenziell; Heredoc/-c haette die Pruefung gar nicht erst erlaubt."
  ],

  "plugin_bugs_observed": [
    "Kein Bug. Hinweis: pretooluse-bash.sh blockiert JEDE Scripting-Sprache mit Code-Arg das eine GESCHUETZTE Datei referenziert — auch reine READ-Operationen (open(...,'r')). Fuer reine Audit-Worker etwas streng, aber sicherheitstechnisch korrekt (Defense-in-Depth). .py-Dateien via Write umgehen es sauber."
  ]
}

# atomic write
d = os.path.dirname(OUT)
with tempfile.NamedTemporaryFile('w', dir=d, suffix='.tmp', delete=False, encoding='utf-8') as tmp:
    json.dump(data, tmp, ensure_ascii=False, indent=1)
    tmp.write('\n')
    tmpname = tmp.name
os.replace(tmpname, OUT)
# validate
json.load(open(OUT, encoding='utf-8'))
print("OK wrote+validated", OUT)
print("size:", os.path.getsize(OUT), "bytes")
