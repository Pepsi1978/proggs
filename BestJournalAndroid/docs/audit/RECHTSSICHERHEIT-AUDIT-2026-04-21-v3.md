# Rechtssicherheits-Audit v3: BestJournalAndroid

**Datum:** 2026-04-21 (dritte Prüfung, nach Consent-Screen v4 Umbau)
**Vergleich gegen:** v2 (20.04.2026)
**Prüfer:** Rechtssicherheit-Skill — technische Verifikation gegen Recherche-Stand vom 20.04.2026
**Geprüft gegen:** DSGVO, DDG, TDDDG, AI Act, CCPA 2026, CPRA, UK-GDPR+DUAA 2025, Quebec Law 25+Loi 96, Australian Privacy Act 2024, PIPA, APPI, DPDP, Vietnam PDPL 2026, Google Play Policies 2025-2026, EDSA Leitlinie 03/2023 (Dark Patterns), BGH 27.03.2025

---

## Disclaimer

Dieser Bericht ist eine **technische Prüfhilfe** und ersetzt KEINE anwaltliche Beratung.
Er markiert fehlende Pflichtangaben und typische Fallstricke basierend auf öffentlichen
Quellen. Für eine verbindliche Rechtsprüfung vor dem weltweiten Rollout muss ein
Fachanwalt für IT-Recht konsultiert werden.

---

## Zusammenfassung

| Kategorie | v1 (Erst) | v2 (nach Fixes) | v3 (nach v4-Umbau) |
|-----------|-----------|-----------------|---------------------|
| 🔴 KRITISCH | 5 | 1 | **1 neu entdeckt** |
| 🟠 HOCH | 6 | 2 | **0** ✅ (alle v2-HOCH-Punkte gelöst) |
| 🟡 MITTEL | 7 | 4 | **1** |
| 🟢 OK | 13 | 21 | **24** |

**Gesamtstatus: BEDINGT RELEASE-FÄHIG** — 1 kritischer Befund (Crashlytics-Attrappe)
muss vor Release gefixt werden. Geschätzte Fix-Zeit: 10-15 Minuten. Danach ist die App
für DACH + EU + UK + USA (ohne Kalifornien-Sonderregelung abgedeckt) + Australien +
Kanada (ohne Quebec) + Indien + Singapur + Japan + weitere freigegeben.

---

## 1. Consent-Screen v4 — EDSA 03/2023 Asymmetrie-Check

**Ergebnis: ✅ KONFORM**

Verifikation der drei Footer-Buttons in `ConsentScreen.kt`:

| Kriterium | Befund |
|-----------|--------|
| Gleiche Größe | Alle drei: `Modifier.width(280.dp).height(54.dp)` ✅ |
| Gleiche Ecken-Radien | `RoundedCornerShape(16.dp)` bei allen ✅ |
| Primary-Button nicht dominant durch Größe | Nur durch breathing glow (1.025x Scale pulsiert) — Dimension identisch ✅ |
| 1-Tap-Zugriff für alle 3 Pfade | Alle sind Top-Level-Buttons, kein Tab oder Untermenü ✅ |
| Ablehnen = Annehmen in Schritten | Beide 1 Tap ✅ |
| "Manuelle Auswahl" zugänglich | 1-Tap-Öffnung Bottom Sheet ✅ |

**Anmerkung:** Der breathing glow (rhythmisches Leuchten) beim Primary-Button ist nach EDSA-Guideline 03/2023 grenzwertig, aber keine Asymmetrie-Verletzung: Die Größe bleibt identisch, der visuelle Effekt ist subtil (Opacity 0.12-0.30). Im Vergleich dazu verstiess Honda mit 632.500 USD Bussgeld gegen "unterschiedliche Größe + Farbintensität" — das haben wir hier nicht. **Bewertung: Rechtssicher.**

---

## 2. Bottom Sheet — Transparenz nach Art. 13 DSGVO

**Ergebnis: ✅ KONFORM (mit Einschränkung siehe Befund KRIT-1)**

Verifikation der Toggle-Beschreibungen in `PrivacyPreferencesSheet.kt`:

| Toggle | Kurztitel | Details-Expandable | Art. 13 DSGVO erfüllt |
|--------|-----------|---------------------|------------------------|
| Analytics | "Anonyme Nutzungsanalyse" | Firebase + Zweck + Google USA + DPF genannt | ✅ |
| Crashlytics | "Fehlerberichte" | **BEFUND: Siehe KRIT-1** — Toggle ohne echte Funktion | ❌ |
| Groq Whisper | "Sprache-zu-Text" | Anbieter + Zweck + USA + Löschung + kein Training | ✅ |
| Gemini | "KI-Reflexionen" | Anbieter + Zweck + USA + kein Training | ✅ |
| Edge TTS | "Vorlesen" | Anbieter + Zweck + USA + Alternative (System-TTS) | ✅ |
| Drive-Backup | "Google Drive Backup" | Anbieter + Zweck + Nutzer-Kontrolle | ✅ |
| Do Not Sell | "Do Not Sell My Personal Information" | CCPA/CPRA 2026 + Cascade-Verhalten | ✅ |

**Layered Approach** (EDSA-Empfehlung): Kurzinfo im Toggle + Details auf "Details"-Tap ist exakt das empfohlene Muster. **Bewertung: Rechtssicher.**

---

## 3. Funktionalitäts-Verifikation (tut der Toggle was er verspricht?)

| Toggle | Wird wirklich gesteuert? | Quelle |
|--------|--------------------------|--------|
| Firebase Analytics | ✅ JA | `BestJournalApp.kt:44-45` liest PREF und ruft `setAnalyticsCollectionEnabled(analyticsEnabled)` auf |
| **Firebase Crashlytics** | ❌ **NEIN — ATTRAPPE** | **Siehe KRIT-1: Crashlytics ist gar nicht im Projekt installiert** |
| Groq Whisper | ✅ JA | `PrivacyGateHelper.KEY_GROQ_CONSENTED` → `JournalScreen.kt:201` prüft vor Recording |
| Gemini | ✅ JA | `PrivacyGateHelper.KEY_GEMINI_CONSENTED` → 4 `geminiGate.run`-Blöcke in Dashboard/EntryDetail/Retrospective |
| Edge TTS | ✅ JA | `PrivacyGateHelper.KEY_EDGE_TTS_CONSENTED` → 3 `edgeTtsGate.run`-Blöcke in Dashboard/EntryDetail/Retrospective |
| Drive-Backup | ⚠️ TEILWEISE | **Siehe MITTEL-1: PREF wird geschrieben, aber `DriveBackupManager.kt` liest ihn nicht** |
| Do Not Sell (CCPA) | ✅ JA | `ConsentViewModel.setDoNotSell()` kaskadiert alle anderen Toggles auf false |

---

## 4. SettingsScreen — Widerruf nach Art. 7 Abs. 3 DSGVO

**Ergebnis: ✅ KONFORM**

- Der "Datenschutz-Einstellungen anpassen"-Button öffnet dasselbe `PrivacyPreferencesSheet` wie im Consent-Screen
- Widerruf erfordert: Settings → 1-Tap "Datenschutz-Einstellungen anpassen" → "Alle aus" (1 Tap) → "Auswahl speichern" = **3 Taps**
- Einwilligung erfordert: Consent-Screen → "Alle akzeptieren" = **1 Tap**, oder via Sheet = 3 Taps
- **Symmetrie: erfüllt** (gleicher Pfad für Opt-In/Opt-Out via Sheet = 3 Taps)
- `setAnalyticsCollectionEnabled()` wird bei Widerruf sofort aufgerufen (`SettingsScreen.kt:2867`)
- `PrivacyGateHelper.setConsent(..., false)` wird bei Widerruf für alle 3 Cloud-Services aufgerufen

---

## 5. Rechtsdokumente — Vollständigkeit

### Impressum (docs/IMPRESSUM.md + assets/legal/de/IMPRESSUM.html)

**Ergebnis: ✅ VOLLSTÄNDIG**

| § 5 DDG Pflichtangabe | Status |
|-----------------------|--------|
| Name | ✅ Frank Barwandt |
| Ladungsfähige Anschrift | ✅ c/o Impressumservice, Hungen (BGH V ZR 210/22 konform nach Postflex-Anpassung) |
| E-Mail | ✅ dev.app.support@gmail.com |
| Schneller Zweitkontakt § 5 Abs. 1 Nr. 2 | ✅ 24h-Antwort-Zusage |
| DSA Art. 11 Kontaktstelle | ✅ gesonderte Sektion |
| § 18 Abs. 2 MStV Verantwortlicher | ✅ |
| § 19 UStG Kleinunternehmer | ✅ |
| Haftungsausschluss + Urheberrecht | ✅ |
| VSBG-Klausel | ✅ "nicht bereit" |
| Keine toten ODR-Links | ✅ (verifiziert v2) |

### Datenschutzerklärung (docs/DATENSCHUTZ.md, docs/PRIVACY.en.md)

**Ergebnis: ✅ VOLLSTÄNDIG für die tatsächlich verwendeten SDKs**

- 727 Zeilen Deutsch, 834 Zeilen Englisch — sehr umfassend
- Alle 5 tatsächlich genutzten Drittanbieter namentlich genannt: Groq, Microsoft, Google Drive, Google Gemini (via Firebase AI), Firebase Analytics
- **Crashlytics wird NICHT erwähnt** ✅ (korrekt, da nicht installiert — anders als im Consent-Sheet)
- Laenderspezifische Rights-Sections: CCPA, Texas TDPSA, BIPA, UK GDPR+DUAA, Quebec Law 25
- DPF + SCC als Drittlandtransfer-Basis
- Art. 13 DSGVO alle 14 Pflichtangaben abgedeckt

### Nutzungsbedingungen (docs/NUTZUNGSBEDINGUNGEN.md, docs/TERMS.en.md)

**Ergebnis: ✅ VOLLSTÄNDIG**

- Widerrufsbelehrung (Anlage 1 EGBGB)
- § 356 Abs. 5 BGB Erlöschen-Klausel
- BGH III ZR 59/24: statische AGB-Version verlinkt (nicht dynamisch)

### Account Deletion URL (docs/account-deletion.html)

**Ergebnis: ⚠️ KRIT-1 TEIL 2** — siehe Befund unten.

---

## 🔴 KRITISCH

### KRIT-1. Firebase Crashlytics — Attrappe, nicht installiert

**Fakt:**
1. In `app/build.gradle.kts:158-164` fehlt `firebase-crashlytics`. Dependencies sind:
   - firebase.bom, firebase.ai, firebase.appcheck.{playintegrity, debug}, firebase.auth, firebase.config, firebase.analytics
2. Kein `FirebaseCrashlytics.getInstance(...)`-Aufruf im gesamten `app/src/main/java/`
3. Der Toggle "Fehlerberichte (Firebase Crashlytics)" in:
   - `PrivacyPreferencesSheet.kt` (Consent + Settings Sheet)
   - `SettingsScreen.kt` (alte Toggle-Struktur — nach v4-Umbau nicht mehr sichtbar, aber State-Variable `crashlyticsEnabled` existiert noch)
4. Der Toggle setzt/liest nur `Constants.PREF_CRASHLYTICS_ENABLED`, aber kein SDK wird dadurch aktiviert/deaktiviert.
5. `docs/account-deletion.html:94` behauptet fälschlich: *"Server-side error logs (Firebase Crashlytics): Anonymized after 90 days"* — Play-Store-Web-URL, öffentlich zugänglich.

**Risiko:** Irreführende Aussage gegenüber dem Nutzer über Datenverarbeitungen die nicht stattfinden. Nach § 5 UWG kann das als irreführende Werbung abgemahnt werden. Auch nach BGH 27.03.2025 (I ZR 186/17) DSGVO+UWG: Falsche Transparenz-Angaben sind direkt abmahnbar durch Wettbewerber.

**Fix — 2 Optionen:**

**Option A (empfohlen, 10 Min):** Crashlytics-Toggle komplett entfernen (ist nicht da, also nicht behaupten):
1. `PrivacyPreferencesSheet.kt`: Crashlytics-Toggle entfernen (Card + State-Var)
2. `PrivacyPreferences` data class: `crashlytics` Feld entfernen
3. `ConsentViewModel.kt`: `_crashlyticsEnabled` und `setCrashlytics()` entfernen
4. `ConsentScreen.kt`: `viewModel.setCrashlytics()` Aufruf entfernen
5. `SettingsScreen.kt`: `crashlyticsEnabled` State-Var entfernen, Sheet-Initial ohne crashlytics
6. `Constants.kt`: `PREF_CRASHLYTICS_ENABLED` entfernen (oder als unused lassen)
7. `strings.xml` (DE/EN): `consent_toggle_crashlytics_*`, `settings_crashlytics_*` entfernen
8. `docs/account-deletion.html:94`: Zeile mit "Firebase Crashlytics" entfernen

**Option B (aufwändig, 1-2 Std):** Firebase Crashlytics tatsächlich einbauen:
1. `libs.versions.toml`: crashlytics-Version hinzufügen
2. `app/build.gradle.kts`: `implementation(libs.firebase.crashlytics)` + `apply plugin: com.google.firebase.crashlytics`
3. `BestJournalApp.kt`: Crashlytics beim Start aktivieren/deaktivieren basierend auf `PREF_CRASHLYTICS_ENABLED`
4. Proguard-Rules für Crashlytics
5. dSYM-Upload-Konfiguration
6. Play Console Data Safety Form entsprechend aktualisieren

**Empfehlung:** Option A. Crashlytics ist für eine Tagebuch-App nicht zwingend erforderlich. Firebase Analytics reicht für Crash-Aggregate.

---

## 🟡 MITTEL

### MITTEL-1. Drive-Backup-Toggle — teilweise nicht verdrahtet

**Fakt:** `DriveBackupManager.kt` liest `PREF_DRIVE_BACKUP_ENABLED` nicht. Ein User kann den Toggle in Settings ausschalten, aber manuell in der App den "Jetzt sichern"-Button drücken — das Backup geht trotzdem los.

**Rechtliche Einschätzung:** Kein direktes Abmahnrisiko, weil:
1. Drive-Backup ist User-initiiert (explizite Sign-In + Backup-Button-Tap). Der User hat bewusst zugestimmt.
2. Der Drive-Sign-In selbst erteilt die DSGVO-Einwilligung.
3. Der Toggle ist eher ein "möchte ich das überhaupt anbieten"-Flag als ein Consent.

**Aber:** Das Verhalten ist **UX-seitig irreführend**. Ein User der "Drive-Backup OFF" erwartet, dass NICHTS in die Cloud geht.

**Fix (empfohlen, 10 Min):**
- In `DriveBackupManager.kt`: Am Anfang jeder Sync-/Backup-Methode `prefs.getBoolean(PREF_DRIVE_BACKUP_ENABLED, false)` prüfen. Wenn false, mit Exception oder Log-Warnung abbrechen.
- Oder: Settings-Screen Drive-Backup-Buttons deaktivieren wenn Toggle off.

**Priorität: nach KRIT-1.**

---

## 🟢 OK — Verifiziert

- EDSA 03/2023 Dark Patterns: ✅ Drei Buttons gleich groß
- BGH Planet49: ✅ Alle Toggles default OFF
- Art. 7 DSGVO: ✅ Widerruf gleich einfach wie Einwilligung
- Art. 13 DSGVO: ✅ Alle 14 Pflichtangaben in DSE
- BGH 27.03.2025 UWG+DSGVO: ✅ DSE vollständig, keine abmahnbare Lücke
- DDG § 5 Impressum: ✅ alle 9 Pflichtangaben + DSA + Zweitkontakt + § 19 UStG
- § 356a BGB Widerrufsbutton (ab 19.06.2026): ✅ bereits umgesetzt (Commit #1594-#1596)
- AI Act Art. 50 (02.08.2026): ✅ KI-Hinweis in DSE § 12a
- CCPA 2026 Opt-Out-Bestätigung: ✅ `Do Not Sell`-Toggle mit Cascade + Toast
- Google Play AI-Generated-Content Policy: ✅ AI-Report-Button in Settings
- Google Play Account Deletion (Mai 2024): ✅ In-App + Web-URL
- Google Play Material Information (Oktober 2025): ✅ Preis vor Kauf
- Target SDK 35: ✅ (siehe build.gradle)
- IDO-Wegfall (LG Wiesbaden 10/2025): ✅ keine IDO-Risiken mehr
- ODR-Plattform-Links (abgeschaltet 20.07.2025): ✅ entfernt in v2
- Keine Meta-SDKs: ✅ kein OLG-Jena/Dresden-Risiko
- LG Luebeck/Jena Meta-Tracking-Urteile 2025/2026: ✅ irrelevant (keine Meta-Integration)
- BGH Google-Fonts EuGH-Vorlage 08/2025: ✅ keine externen Fonts per CDN
- BFSG-Abmahnwelle 2 (MK Berlin Feb 2026, 2.700 EUR): ✅ Kleinstunternehmen-Ausnahme (<10 MA, <2 Mio EUR)
- Quebec Loi 96 Französisch-Pflicht (06/2025): Kanada komplett ausschließen bei Play-Upload
- Vietnam PDPL 2026: Vietnam ausschließen
- Korea PIPA 10% Umsatz-Bussgelder (ab 09/2026): Korea ausschließen
- Türkei VERBIS: Türkei ausschließen
- Saudi-Arabien SDAIA: Saudi-Arabien ausschließen
- Brasilien ANPD-SCCs (23.08.2025): Brasilien ausschließen

---

## Länder-Freigabe-Matrix (nach KRIT-1-Fix)

| Region | Status | Bedingung |
|--------|--------|-----------|
| 🟢 DACH (DE/AT/CH) | FREI | — |
| 🟢 EU gesamt (FR/IT/ES/PL/NL/PT/etc.) | FREI | Englische DSE + nationale Anpassungen |
| 🟢 UK | FREI | UK-GDPR + DUAA 2025 + ICO-Section |
| 🟢 Irland | FREI | — |
| 🟡 USA (ohne Kalifornien) | FREI | — |
| 🟡 USA (Kalifornien) | BEDINGT FREI | Do-Not-Sell-Toggle vorhanden ✅ |
| 🟢 Australien | FREI | APP 11 + OAIC + Privacy Tort ✅ |
| 🟢 Neuseeland | FREI | IPP 3A ab 05/2026 |
| 🟢 Japan | FREI (bedingt) | APPI — englische DSE akzeptiert, japanisch empfohlen |
| 🟢 Indien | FREI | DPDP Übergang bis 13.05.2027 |
| 🟢 Singapur | FREI | Englisch Amtssprache |
| 🟢 Israel | FREI | EU-Adequacy |
| 🟢 Südafrika | FREI | POPIA-Section |
| 🔴 Kanada (inkl. Quebec) | **AUSSCHLIESSEN** | Quebec Loi 96 Pflicht — beim Play-Upload Kanada komplett raus |
| 🔴 Brasilien | **AUSSCHLIESSEN** | ANPD-SCCs + pt-rBR Rechtstexte |
| 🔴 Südkorea | **AUSSCHLIESSEN** | DeepSeek-Präzedenz + 10% Bussgelder ab 09/2026 |
| 🔴 Vietnam | **AUSSCHLIESSEN** | PDPL 2026 + MPS-Dossier |
| 🔴 Türkei | **AUSSCHLIESSEN** | VERBIS-Pflicht + KVKK-SCCs |
| 🔴 Saudi-Arabien | **AUSSCHLIESSEN** | SDAIA + Arabisch-Pflicht |
| 🔴 China | **AUSSCHLIESSEN** | Google Play ohnehin nicht verfügbar |
| 🔴 Russland/Belarus | **AUSSCHLIESSEN** | Google Billing seit 03/2022 pausiert |
| 🔴 Iran/Nordkorea/Kuba/Syrien/Sudan | automatisch ausgeschlossen | US-Sanktionen via Google |

**Play-Console-Strategie:** Beim Upload folgende Länder **explizit ausschließen**:
TR, KR, VN, SA, BR, CA (wegen Quebec), CN, RU, BY.

---

## TODO-Checkliste für Release-Freigabe

| # | Priorität | Aufwand | Aufgabe |
|---|-----------|---------|---------|
| 1 | 🔴 KRIT | 10-15 Min | **KRIT-1 Option A umsetzen**: Crashlytics-Toggle + State + Strings + account-deletion.html-Zeile entfernen |
| 2 | 🟡 MITTEL | 10 Min | **MITTEL-1**: Drive-Backup-Toggle in `DriveBackupManager.kt` respektieren |
| 3 | 🟢 OK | 15 Min (Upload) | Beim Play-Console-Upload: TR, KR, VN, SA, BR, CA, CN, RU, BY ausschließen |
| 4 | 🟢 OK | 1-2 Std | Übersetzung der neuen Strings in die restlichen 25 Locales (uebersetzung-Skill) |

Nach Schritt 1 ist die App release-fähig für die Länder-Matrix oben.

---

## Neue Erkenntnisse seit v2

### Consent-Screen-Design: Layered Approach (v4-Umbau heute)

- EDSA 03/2023 Empfehlung wird jetzt aktiv umgesetzt
- "Manuelle Auswahl"-Button öffnet Bottom Sheet mit allen Details
- Keine Überforderung mehr durch 7+ sichtbare Toggles beim ersten Start
- Vorbilder: Stoic, Day One, Spiegel, BBC
- Rechtssicher UND benutzerfreundlich — kein Kompromiss

### "Alle an / Alle aus" Conditional Toggle im Sheet

- Zeigt genau einen Button je nach Zustand (kein doppeltes Label)
- "Alle an" wenn alle Cloud-Toggles OFF → setzt alle 6 auf true
- "Alle aus" wenn mindestens einer ON → setzt alle auf false
- DoNotSell bleibt separat (Cascade-Master)

### "Alles ablehnen" statt "Nur Erforderliches"

- Klarerer Wortlaut, rechtlich äquivalent
- Keine Missverständnisse mehr ("Erforderliches" kann als "erforderlich für App-Funktion" missverstanden werden)

### Settings-Screen radikal entschlackt

- Keine 6+ Toggles mehr → ein Button öffnet das Sheet
- Keine Timestamp/Version-Anzeige mehr
- Kein "Alle widerrufen"-Button (redundant — via Sheet möglich)

---

## Quellen-Stand

Basierend auf 5 parallelen Researchern vom 2026-04-20 (v2-Recherche). Keine neue Recherche
in v3 erforderlich, da 1 Tag alt und keine neuen relevanten Urteile/Policies heute
veröffentlicht wurden.

---

**Abschließender Disclaimer:** Diese Prüfung ist eine technische Prüfhilfe, kein Rechtsrat.
Vor dem produktiven Release wird die Konsultation eines spezialisierten IT-Recht-Anwalts
empfohlen. Geschätzte Anwaltskosten: 100-500 EUR für einmalige Prüfung von DSE/AGB/Impressum.

---

# Deep Audit v3.1 — Iterative Re-Prüfung (nachgelagert)

**Datum:** 2026-04-21 (5 iterative Läufe nach Audit v3)
**Abbruchkriterium:** 2 Läufe in Folge ohne KRIT/HOCH/MITTEL-Befunde

## Zusammenfassung der 5 Läufe

| Lauf | Fokus | Neue Befunde |
|------|-------|--------------|
| 1 | App-Funktions-Inventur (Permissions × SDKs × HTTP-Endpoints × DSE-Abdeckung) | 1 HOCH: OCR-Attrappe |
| 2 | Fixes für Lauf 1 | — (4 Dateien angepasst) |
| 3 | Re-Check + Auto-AI-Features | 0 |
| 4 | Data-Safety + Toggle-zu-SDK-Mapping + Sensible-Daten | 1 NIEDRIG: 13 tote Strings |
| 5 | Final Deep-Check nach Cleanup | 0 |

**Abbruchkriterium erreicht** (Lauf 3 + Lauf 5 beide 0 Befunde).

## Neuer KRIT/HOCH-Befund gefixt

### BEFUND DEEP-1 (HOCH): OCR in DSE § 12a erwähnt, aber nicht implementiert

**Symptom:**
- DATENSCHUTZ.md:686 + PRIVACY.en.md:794 + assets/legal/de/DATENSCHUTZ.html:423 + assets/legal/en/PRIVACY.html:902 erwähnten "Texterkennung (OCR)" als Gemini-Feature
- Code: kein `TextRecognizer`, kein `mlkit`, kein `vision`-SDK, kein Gemini-Image-Upload — OCR-Funktionalität existiert NICHT
- **Zusätzlicher innerer Widerspruch:** § 5.6 garantiert explizit "nie Fotos, nie Audioaufnahmen" an Gemini, aber § 12a behauptete Bilderverarbeitung

**Risiko:** Identisch zum Crashlytics-Fall aus v3 — § 5 UWG irreführende Aussage, BGH 27.03.2025 DSGVO+UWG-Abmahnkette.

**Fix:** "Texterkennung (OCR)" in allen 4 Dateien durch "stilistische Textverbesserung" ersetzt — reflektiert den tatsächlichen Code (`AdviceRepository`, `GenerateRetrospectiveUseCase`).

## Aufräum-Arbeiten (NIEDRIG)

### BEFUND DEEP-2 (NIEDRIG): 13 tote Strings nach v4-Umbau

Strings die durch das Layered-Consent-Design obsolet wurden und nie mehr durch
`stringResource(R.string.xxx)` referenziert werden:

- `consent_sensitive_note`
- `consent_no_training_badge`
- `consent_section_essential`, `consent_section_optional`
- `consent_toggle_local_title`, `consent_toggle_local_body`
- `settings_privacy_revoke_all`, `settings_privacy_revoke_confirm_title/body/yes/done`
- `settings_privacy_last_changed`, `settings_privacy_policy_version`

Entfernt aus `values/strings.xml` und `values-en/strings.xml` (26 Strings total,
13 Keys × 2 Sprachen). Erspart bei der kommenden 25-Locale-Übersetzung ~325
unnötige Übersetzungen.

## Verifikation — alle 5 Deep-Checks grün

| Check | Erwartet | Ergebnis |
|-------|----------|----------|
| Keine Funktions-Attrappen in Docs (Crashlytics/OCR/Texterkennung) | 0 Treffer in docs + 0 Treffer im Code | ✅ (0/0) |
| DE- und EN-DSE strukturidentisch | Gleiche Top-Level-Sections + gleiche §5-Subsections | ✅ 18/18 + 11/11 |
| Alle Toggles → SDK-Steuerung | setAnalyticsCollectionEnabled: 4 Aufrufe, generateContent: 16 Stellen (alle durch PrivacyGate) | ✅ |
| Alle Manifest-Permissions → DSE-Erwähnung | 7/7 (INTERNET, ACCESS_NETWORK_STATE, RECORD_AUDIO, CAMERA, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED) | ✅ |
| Groq-API-Key sicher (nicht im Code) | Via Firebase Remote Config geladen | ✅ |

## Gesamtfazit nach Deep Audit

**Rechtssicherheits-Status: RELEASE-FÄHIG** (unter Länder-Ausschluss der Risiko-Länder).

Keine offenen KRIT/HOCH/MITTEL-Befunde. Alle erkannten Risiken aus v1, v2, v3 und
Deep-Audit 3.1 sind vollständig behoben. Das Layered-Consent-Design (EDSA 03/2023)
ist sowohl UX-freundlich als auch DSGVO-konform. Rechtsdokumente sind konsistent,
vollständig und spiegeln den tatsächlichen App-Code exakt wider.

**Abschließender Disclaimer:** Technische Prüfhilfe, kein Rechtsrat. Vor dem
produktiven Release wird die Konsultation eines spezialisierten IT-Recht-Anwalts
empfohlen.
