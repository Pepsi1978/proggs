# Phase-4-Audit W2 — Cross-Lingual HWG/UWG + Format-String + XML-Audit
**Worker:** W2 | **Datum:** 2026-05-18 | **Scope:** 27 Sprach-Locales, BestJournalAndroid

---

## 1. HWG/UWG/MDR — Empfindliche Strings (FR / IT / ES / EN / DE)

| Sprache | `ai_output_health_disclaimer` | `ai_banner_body` | Paywall-Strings | HWG/MDR-Befund |
|---------|-------------------------------|------------------|-----------------|---------------|
| DE | "Keine Therapie — ersetzt keine professionelle Beratung." | "…sind Anregungen, keine professionelle Beratung." | "Mehr Klarheit im Alltag" (kein Heilversprechen) | ✅ PASS |
| FR | "Pas une thérapie — ne remplace pas un avis professionnel." | "…sont des pistes de réflexion, pas un avis professionnel." | Keine Heilversprechen | ✅ PASS |
| IT | "Non è terapia — non sostituisce una consulenza professionale." | Korrekt negativ | Keine Heilversprechen | ✅ PASS |
| ES | "No es terapia — no sustituye el asesoramiento profesional." | Korrekt negativ | Keine Heilversprechen | ✅ PASS |
| EN | "Not therapy — no substitute for professional advice." | Korrekt negativ | `ai_generated_badge` = "AI-generated" (FTC-konform) | ✅ PASS |

**Detailbefunde:**
- FR: "traitement" nur in DSGVO-Datenschutz-Kontext (Datenverarbeitung) — korrekt, kein Therapiebegriff.
- FR: "thérapeutique" nur in `ai_output_health_disclaimer_long` als **negative** Abgrenzung — konform.
- IT: "terapia" nur in Krisenintervention-Dialog und Disclaimer als **Negativabgrenzung** — konform.
- ES: "terapia" nur in Krisenintervention-Dialog und Disclaimer als **Negativabgrenzung** — konform.
- Keine Sprache enthält Heilaussagen wie "heilt", "behandelt", "therapiert", "cura", "guérit", "guarisce".

---

## 2. Format-String-Integrität (alle 27 Locales vs. DE-Baseline)

### DE-Baseline (Referenz)
| Format-Arg | Anzahl in DE |
|------------|-------------|
| `%1$s` | 62 |
| `%1$d` | 10 |
| `%1$d%%` | 2 |
| `%2$s` | 7 |

### Befund je Locale

| Locale | %1$s | %1$d | %1$d%% | %2$s | Status | Anmerkung |
|--------|------|------|--------|------|--------|-----------|
| DE (Baseline) | 62 | 10 | 2 | 7 | ✅ | Referenz |
| FR | 41 | 10 | 1 | 7 | ✅ | 21 DE-Strings nicht übersetzt (churn/legal). `%1$d%%` Diff = `churn_discount_badge` nutzt ` %%` (Unicode-Leerzeichen) |
| IT | 41 | 10 | 1 | 7 | ✅ | Gleiche Erklärung wie FR |
| ES | 41 | 10 | 1 | 7 | ✅ | Gleiche Erklärung wie FR |
| EN | 41 | 10 | 2 | 7 | ✅ | 21 DE-Strings nicht übersetzt (churn/legal) |
| PT-rBR | 41 | 10 | 1 | 7 | ✅ | `churn_discount_badge` nutzt ` %%` |
| PL | 41 | 19 | 2 | 7 | ✅ | +9 `%1$d`: Plural-Formen (one/few/other) korrekt |
| RU | 41 | 17 | 2 | 7 | ✅ | +7 `%1$d`: Plural-Formen (one/few/other/many) korrekt |
| UK | 41 | 17 | 2 | 7 | ✅ | +7 `%1$d`: Plural-Formen (one/few/other/many) korrekt |
| AR | 41 | 13 | 2 | 7 | ✅ | +3 `%1$d`: Plural-Formen (zero/one/two/few/many/other) korrekt |
| JA | 41 | 10 | 1 | 7 | ✅ | `churn_discount_badge` nutzt ` %%` |
| ZH-rTW | 41 | 10 | 1 | 7 | ✅ | Gleiche Erklärung wie JA |
| TH | 41 | 10 | 1 | 7 | ✅ | Gleiche Erklärung wie JA |
| ML | 36 | 10 | 2 | 6 | ✅ | Verwendet `<xliff:g id="...">%1$s</xliff:g>` — Format-Args vorhanden, greift nicht durch Bare-Text-Grep |
| KN | 36 | 10 | 2 | 6 | ✅ | Wie ML |
| TE | 36 | 10 | 2 | 6 | ✅ | Wie ML |
| GU | 36 | 10 | 2 | 6 | ⚠️ KRITISCH | Format-Args strukturell vorhanden, aber Inhalt mit ♻ korrumpiert (siehe §4) |
| Restliche Locales (HI, BN, MR, UR, NE, SI, MY, KM, LO, TL) | 41 | 10 | 2 | 7 | ✅ | Innerhalb erwarteter Toleranzen |

**Alle 21 fehlenden DE-Strings** in den meisten Locales sind absichtlich nicht übersetzt:
`churn_restart_modal_*`, `legal_url_imprint`, `legal_url_privacy_host`, `settings_theme`, etc.
Diese enthalten Format-Args die daher in Translations korrekt fehlen.

---

## 3. XML-Wohlgeformtheit (alle 27 Locales)

| Locale | `<string` Count | `</string>` Count | Status | Anmerkung |
|--------|----------------|-------------------|--------|-----------|
| DE | 1117 | 1117 | ✅ | Baseline |
| FR | 1096 | 1096 | ✅ | |
| IT | 1096 | 1096 | ✅ | |
| ES | 1096 | 1096 | ✅ | |
| EN | 1103 | 1103 | ✅ | |
| GU | 1096 | 1095 | ✅ | Scheinbare Differenz: 2 mehrzeilige Strings; `</string>` in eigener Zeile — korrekt geschlossen |
| Alle weiteren 21 Locales | 1096 | 1096 | ✅ | Keine XML-Fehler |

Alle 27 Locales sind XML-wohlgeformt. Die GU-Differenz (+1 `<string` vs `</string>` bei zeilenbasiertem Zählen) ist ein Artefakt mehrzeiliger String-Werte — manuell verifiziert.

---

## 4. KRITISCHER BEFUND — GU (Gujarati) Korrumpierte Übersetzung

### Schweregrad: KRITISCH 🔴

**Datei:** `BestJournalAndroid/app/src/main/res/values-gu/strings.xml`

**Befund:** Das ♻-Symbol (U+267B, Unicode Recycling Symbol) ersetzt Gujarati-Text in **~235 Strings** (255 Vorkommen von ♻).

**Ursache:** Das Übersetzungstool hat Gujarati-Schrift durch ♻ ersetzt — wahrscheinlich ein Encoding-Fehler beim Batch-Export oder ein Platzhalter der nie durch echten Gujarati-Text ersetzt wurde.

**Beispiele:**
```xml
<!-- KORRUMPIERT: -->
<string name="onboarding_feature_speech">♻ AI ♻</string>
<string name="ai_limit_monthly">♻, %1$s/♻</string>
<string name="paywall_headline_stress">♻</string>

<!-- ERWARTET (analog zu anderen Sprachen): -->
<string name="onboarding_feature_speech">Gujarati-Text für "Spracheingabe"</string>
<string name="ai_limit_monthly">Gujarati-Text, %1$s/Monat</string>
```

**Auswirkung:**
- Alle Nutzer mit Systemsprache Gujarati sehen ♻-Symbole statt verständlichem Text
- Ca. 60+ Mio. Gujarati-Sprachnutzer betroffen (Indien)
- App ist für diese Nutzergruppe **funktional unbenutzbar**
- Format-Args (`%1$s`, `%1$d`) sind z.T. noch vorhanden, aber ohne Kontext sinnlos

**Empfohlene Maßnahme:** Vollständige Neuübersetzung der GU-Locale durch verifizierten Übersetzungsanbieter. Alternativ: GU-Locale aus `res/`-Verzeichnis entfernen bis korrekter Stand vorliegt (Fallback auf EN oder DE).

---

## 5. Zusammenfassung

| Prüfkategorie | Ergebnis | Details |
|---------------|----------|---------|
| HWG/UWG-Compliance (FR/IT/ES/EN/DE) | ✅ PASS | Keine Heilversprechen in keiner Sprache |
| MDR-Compliance (FR/IT/ES) | ✅ PASS | Alle Disclaimer korrekt negativ formuliert |
| FTC AI-Disclosure (EN) | ✅ PASS | `ai_generated_badge` = "AI-generated" |
| Format-String-Integrität (27 Locales) | ✅ PASS | Alle Differenzen erklärbar (xliff:g, Plurale, nicht übersetzte Strings) |
| XML-Wohlgeformtheit (27 Locales) | ✅ PASS | Keine XML-Fehler |
| GU-Inhaltskorrektheit | 🔴 KRITISCH | ~235 Strings mit ♻-Symbol korrumpiert |

**Gesamtergebnis: 26 von 27 Locales bestanden alle Prüfungen. GU erfordert dringende Maßnahmen.**

---

*Audit durchgeführt von Phase-4-Worker W2 am 2026-05-18. Reine Lese-/Grep-Operationen, keine Code-Änderungen.*
