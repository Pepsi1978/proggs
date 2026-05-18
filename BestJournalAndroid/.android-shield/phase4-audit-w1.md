# Phase 4 W1 — Code + DE-Re-Audit

**Datum:** 2026-05-18
**Modus:** delta
**Scope:** DE-Strings + 5 modifizierte Screens + 1 neue Composable
**Worker:** W1 (Phase-4-Worker, finale-Plugin)

---

## Validierung der 27 Phase-2-Fixes

| # | Finding-ID | Beschreibung | Erwartet | Tatsaechlich | Zeile | Status |
|---|-----------|-------------|---------|-------------|-------|--------|
| 1 | AM-001 | KI-Profile-Anzahl in settings premium feature 5 | "4 vordefiniert + unbegrenzt eigene" | "Alle KI-Profile (4 vordefiniert + unbegrenzt eigene)" | strings.xml:497 | ✅ PASS |
| 2 | AM-002 | KI-Profile-Anzahl in churn offer | "4 vordefiniert + unbegrenzt eigene" | "Alle KI-Profile (4 vordefiniert + unbegrenzt eigene)" | strings.xml:1185 | ✅ PASS |
| 3 | AM-003 | Onboarding perspectives title neutral | kein Marketing-Versprechen | "Verschiedene Blickwinkel auf\ndein Leben" | strings.xml:1092 | ✅ PASS |
| 4 | AM-004 | ai_limits_dialog_body — Tageskontingent korrekt | "150 Anfragen pro Tag und Profil" + "4 vordefinierten" | Beides vorhanden | strings.xml:1207 | ✅ PASS |
| 5 | AM-005 | ai_limits_dialog_body — "Die meisten Nutzer" korrekt | "Die meisten Nutzer im Alltag" (nicht "fast alle") | "Die meisten Nutzer im Alltag" | strings.xml:1207 | ✅ PASS |
| 6 | AM-006 | paywall_consent_dialog_body — Anbieter-Angabe | "Frank Barwandt" als Anbieter | "Anbieter der Premium-Funktionen: Frank Barwandt" | strings.xml:1105 | ✅ PASS |
| 7 | AM-007 | paywall_exit_start_discount — §356 Abs.5 BGB | "Zahlungspflichtig" als Prefix | "Zahlungspflichtig — 2 Monate zum halben Preis" | strings.xml:1152 | ✅ PASS |
| 8 | AM-008 | paywall_yearly_savings — Kündigungshinweis | "Kündigung jederzeit möglich zum jeweiligen Vertragsende" | Vorhanden | strings.xml:1119 | ✅ PASS |
| 9 | AM-009 | churn_offer_subtitle — neutral | "Unser Angebot für dich" (kein Superlativ) | "Unser Angebot für dich" | strings.xml:1183 | ✅ PASS |
| 10 | AM-010 | onboarding_try_premium — Preis-Pflicht | Preis + "danach ab X €/Monat" | "8 Tage gratis — danach ab 3,99 €/Monat" | strings.xml:1095 | ✅ PASS |
| 11 | AM-011 | paywall_subtitle — neutral | "Schalte alle Premium-Funktionen frei" | "Schalte alle Premium-Funktionen frei" | strings.xml:1473 | ✅ PASS |
| 12 | AM-012 | ai_limits_disclaimer — kein "Großzügiges" | Kein "Großzügiges" im String | "KI-Tageskontingent für intensive Nutzung." | strings.xml:1205 | ✅ PASS |
| 13 | AM-013 | settings_premium_feature_improve_desc — "auf Wunsch" | "auf Wunsch" in Beschreibung | Vorhanden ("auf Wunsch") | strings.xml:494 | ✅ PASS |
| 14 | AM-014 | settings_premium_feature_patterns_desc — "sichtbar machen" | "sichtbar machen" statt Versprechen | Vorhanden ("sichtbar machen") | strings.xml:502 | ✅ PASS |
| 15 | AM-015 | ai_banner_body — Disclaimer vorhanden | "Anregungen, keine professionelle Beratung" | Vorhanden | strings.xml:1211 | ✅ PASS |
| 16 | AM-016 | paywall_headline_stress — neutral | "Mehr Klarheit im Alltag" | "Mehr Klarheit im Alltag" | strings.xml:1136 | ✅ PASS |
| 17 | AM-017 | settings_premium_feature_dashboard_desc — neutral | Neutraler Text, kein Versprechen | "Dein Dashboard wächst mit deinen Einträgen." | strings.xml:496 | ✅ PASS |
| 18 | T-001 | Permission rationale strings — 4 Keys | microphone, camera, location, notifications | Alle 4 vorhanden | strings.xml:698–701 | ✅ PASS |
| 19 | T-002a | consent_info_system_backup_title + _body | Backup-Transparenz UI-Strings | Beide vorhanden | strings.xml:686–687 | ✅ PASS |
| 20 | T-002b | settings_open_system_backup_settings_* (2 Keys) + error | Settings-Strings für Backup | Alle 3 vorhanden | strings.xml:725–727 | ✅ PASS |
| 21 | T-002c | DATENSCHUTZ.html — Android-System-Backup-Sektion | Dedizierter Abschnitt 5.3a | "5.3a Android-System-Backup (automatisch)" | DATENSCHUTZ.html:169 | ✅ PASS |
| 22 | T-002d | DATENSCHUTZ.html — Datentabellen-Eintrag | Tabellenzeile im Backup-Kontext | "Tagebuch-Datenbank (Android System-Backup)" | DATENSCHUTZ.html:68 | ✅ PASS |
| 23 | T-007 | ChurnFlowDialog alpha 0.35f → 0.8f | Kein 0.35f mehr vorhanden | alpha = 0.8f, 0.35f nicht gefunden | ChurnFlowDialog.kt:508 | ✅ PASS |
| 24 | PS-001 | R8 minification deferred — isMinifyEnabled = false | false im release-Build | isMinifyEnabled = false + Kommentar | build.gradle.kts:98 | ✅ PASS |
| 25 | MD-001/002 | legal_url_* strings — 9 Keys (DE/EN/KO × 3 URLs) | Alle 9 Keys vorhanden | Alle 9 vorhanden | strings.xml:1490–1498 | ✅ PASS |
| 26 | MD-003 | ai_output_health_disclaimer + ConsentLegalFooter | Disclaimer-String + Footer-Links | ai_output_health_disclaimer + consent_footer_link_* | strings.xml:1501–1507 | ✅ PASS |
| 27 | MD-004/005 | AiOutputDisclaimer.kt + AiGeneratedBadge-Einsatz | Composable in 5 Screens | DashboardScreen (L1455), Retrospective (L675), EntryDetail (L392, L636), FollowUp (L290) — AiGeneratedBadge: EntryDetail L343/L436, FollowUp L213 | Multiple | ✅ PASS |

**Gesamtergebnis: 27/27 PASS**

---

## Neue Findings im Code

### NF-001 — `ai_output_health_disclaimer_long` nicht konsumiert (MINOR)

**Typ:** Unused Resource / Unvollständige Integration  
**Schwere:** MINOR (kein Compliance-Risiko, kein Regression)  
**Ort:** `strings.xml` Zeile 1502  

**Befund:**  
Der String `ai_output_health_disclaimer_long` ist in `strings.xml` definiert (Zeile 1502), wird jedoch in keiner Kotlin-Datei via `stringResource(R.string.ai_output_health_disclaimer_long)` aufgerufen. Nur die Kurzversion `ai_output_health_disclaimer` (Zeile 1501) wird von `AiOutputDisclaimer.kt` verwendet.

**Konsequenz:**  
Die lange Variante existiert als toter String. Dies ist kein Compliance-Problem, da die Kurzversion angezeigt wird. Möglicherweise war eine kontextabhängige Anzeige (kurz im Badge, lang als expandierter Dialog) geplant aber nicht fertiggestellt.

**Empfehlung:**  
Entweder (a) den langen Disclaimer in einem erweiterbaren Dialog nutzen (z.B. bei Tap auf AiOutputDisclaimer) oder (b) den String bis zur konkreten Nutzung im Code kommentieren. Kein Blocking-Issue für Phase 5.

---

## Build-Stabilität

### AiOutputDisclaimer.kt

- **Datei:** `app/src/main/java/com/bestjournal/app/ui/components/AiOutputDisclaimer.kt`
- **Länge:** 44 Zeilen
- **Syntaxprüfung:** Vollständig — alle Imports vorhanden (Row, Arrangement, fillMaxWidth, padding, MaterialTheme, Text, Composable, Modifier, stringResource, TextAlign, dp, R)
- **Hardcoded Strings:** Keine — ausschließlich `stringResource(R.string.ai_output_health_disclaimer)` ✅
- **Build-Risiko:** KEINS

### ConsentScreen.kt — ConsentLegalFooter

- **Datei:** `app/src/main/java/com/bestjournal/app/ui/screens/consent/ConsentScreen.kt`
- **ConsentLegalFooter:** Zeilen 560–648 (89 Zeilen), syntaktisch vollständig
- **Alle Imports vorhanden:** Intent, Uri, LocalContext, TextButton, PaddingValues, FontWeight, sp ✅
- **URL-Handling:** Via `urlFor()` locale-aware Helper — DE/EN/KO korrekt geroutet ✅
- **Hardcoded Strings:** Nur Separator `"·"` (Interpunktion, kein i18n-Bedarf) ✅
- **Build-Risiko:** KEINS

### ChurnFlowDialog.kt

- **Datei:** `app/src/main/java/com/bestjournal/app/ui/screens/settings/ChurnFlowDialog.kt`
- **Alpha-Wert:** `0.35f` nicht vorhanden, `0.8f` bestätigt (Zeile 508) ✅
- **Build-Risiko:** KEINS

### build.gradle.kts

- **PS-001:** `isMinifyEnabled = false` für release-Build, `isShrinkResources = false` ebenfalls gesetzt
- **Kommentar:** Explizite Dokumentation der PS-001-Entscheidung im Code vorhanden ✅
- **Build-Risiko:** KEINS (R8-Deaktivierung ist bewusste Entscheidung, keine versehentliche Fehlkonfiguration)

---

## Zusammenfassung

| Kategorie | Ergebnis |
|-----------|---------|
| Phase-2-Fixes validiert | 27 / 27 |
| Neue Blocking-Issues | 0 |
| Neue Minor-Issues | 1 (NF-001: ai_output_health_disclaimer_long unused) |
| Build-Stabilitäts-Risiken | 0 |
| Compliance-Risiken (DE) | 0 |

## Status

- **resolved:** 27/27
- **new_findings:** 1 (NF-001 — ai_output_health_disclaimer_long unused, MINOR, non-blocking)
- **build_stability:** STABIL — alle 4 geprüften Dateien syntaktisch vollständig, keine Kompilierungsrisiken
- **recommendation:** PHASE 5 SUCCESS — alle Pflicht-Fixes korrekt umgesetzt, NF-001 optional in separatem Commit adressierbar
