# Rechtssicherheits-Audit BestJournalAndroid — v6

**Datum:** 2026-04-28
**Vorgaenger:** v5 (2026-04-23, 1 KRITISCH + 3 HOCH + 6 MITTEL)
**Anlass v6:** Pre-Release Audit fuer Play-Store-Upload — Benutzer fragte explizit nach "abmahnungssicher und bereit fuer Hochladen". Erste Audit-Iteration die `tools/rechtssicherheit.md` Wissensbasis anlegt.
**Methode:** 5 parallele Researcher (DE/EU, US/UK/CA/AU, Asien/LatAm, Google Play Policies, Abmahn-Trends 2025-2026) + Manifest/Permissions/SDKs-Vollscan + 6 Rechtsdokumente in DE+EN + 25 Locale-Kurzfassungen + ConsentScreen + Paywall + Settings/Privacy + Backup/Network-Security + git-history Cross-Reference auf v5-Befunde.
**App-Version geprueft:** 0.14.13 (versionCode 188)
**Geprueft gegen:** DSGVO, TDDDG, DDG, UWG, EU AI Act Art. 50, DSA, BGB §§ 312j, 327ff, 355, 356, 356a, CCPA/CPRA 2026, UK GDPR, PIPEDA, Australia Privacy Act 1988 + 2024 Reform, APPI (JP), PIPA (KR), DPDP (IN), LGPD (BR), Google Play Policies April 2026, Health Apps Policy (Aug 2025), Account Deletion Policy (Mai 2024), Subscription Disclosure, AI-Generated Content Policy.

---

## Disclaimer (PFLICHT)

Dieser Bericht ist eine **technische Pruefhilfe** und ersetzt **KEINE anwaltliche Beratung**.
Er basiert auf oeffentlichen Quellen mit Stand 28.04.2026. **Vor dem Play-Store-Upload MUSS ein Fachanwalt fuer IT-Recht** alle Dokumente — insbesondere DE+EN-Rechtstexte, Paywall-Flow und Premium-Abo-Flow — pruefen.

Verbotene Behauptungen (in diesem Bericht NICHT enthalten): "abmahnungssicher", "100% rechtssicher", "release-fertig". Stattdessen: technisch begruendete Befundliste mit Schweregrad und Quellenangabe.

---

## 1. Gesamtstatus

| Dimension | Wert |
|-----------|------|
| **Release-Empfehlung** | **BEDINGT TECHNISCH OK** — nach Anwaltspruefung und nach Fix der 1 verbleibenden HOCH |
| KRITISCH (Release-Blocker) | **0** |
| HOCH | **2** (1 davon stichtagsgebunden, 1 inhaltlich) |
| MITTEL | **5** |
| NIEDRIG | **3** |
| Compliance-Reife | **Sehr gut** — die App hat ueberdurchschnittlich gruendliche DSE+ToS+Impressum, granulare Per-Service-Consents, opt-in Analytics+Drive-Backup, CCPA-Toggle, korrekte Network-Security-Config, saubere Backup-Regeln |

**Wichtigste verbleibende Punkte vor Release:**

1. 🟠 **§ 356a BGB Widerrufsbutton (Stichtag 19.06.2026 — 52 Tage)** — aktuell mailto-Intent, muss In-App-Versand werden
2. 🟠 **KI-Badge bei Text-Improve-Output** fehlt (DashboardScreen + RetrospectiveScreen haben den Badge, EntryDetail nicht)
3. 🟡 **Health Apps Declaration** beim Play-Console-Upload einreichen (Mood = Health Data laut Aug-2025-Policy)
4. 🟡 **UK GDPR Art. 27 Vertreter** — wenn UK aktiv vermarktet wird (~£100-300/Jahr)
5. 🟡 **§ 312j BGB Bestellbutton-Beschriftung** in Paywall verbessern (siehe NB-v6-1)

---

## 2. Status der v5-Befunde (was ist seit 23.04. passiert)

| v5-Befund | v5-Status | Heute (28.04.) | Notiz |
|-----------|-----------|----------------|-------|
| 🔴 NB1 — Groq-DPF Falschaussage in DE DSE | OFFEN | ✅ **GEFIXT** | DE DSE Zeile 129: "Groq ist nach aktuellem Stand **nicht** unter dem EU-US Data Privacy Framework zertifiziert" |
| 🟠 NB8 — DSE-Aussage "ohne zusaetzliche Bestaetigung" | OFFEN | ✅ **GEFIXT** | DE DSE Zeile 210 sagt jetzt korrekt: erstmalige Nutzung → Bestaetigungs-Dialog (Pre-Usage-Gate), dann ohne erneute Nachfrage |
| 🟠 v4-M1 — § 356a BGB Widerrufsbutton (Stichtag 19.06.2026) | OFFEN | 🟠 **NOCH OFFEN** | SettingsScreen hat zweistufigen mailto-Intent — § 356a verlangt In-App-Versand (Gmail-API analog Feedback-Feature). Stichtag 19.06.2026 = **52 Tage**. |
| 🟠 v4-H4-Rest — KI-Badge bei Text-Improve | OFFEN | 🟠 **NOCH OFFEN** | `AiGeneratedBadgeInline` nur in Dashboard+Retrospective. Text-Improve-Output im EntryDetailScreen hat keinen Badge. AI-Act-Stichtag 02.08.2026. |
| 🟡 NB2 — ToS "nicht-kommerzielle Zwecke" Klausel | OFFEN | (nicht erneut geprueft — niedriges Risiko) | — |
| 🟡 v4-M3 / NB6 — Japanisch Kurzfassung | OFFEN | (Locale-Folder `ja/` existiert mit IMPRINT/PRIVACY/TERMS — Inhalt nicht inhaltlich geprueft) | — |
| 🟡 v4-M7 — Art. 9 DSGVO explizite Einwilligung | TEILWEISE | ✅ **GUT** | `privacy_gate_gemini_body` enthaelt: "Da deine Tagebucheinträge besonders sensible Daten enthalten können (Gesundheit, Religion, persönliche Beziehungen — Art. 9 DSGVO), bitten wir hier um deine ausdrückliche Zustimmung" |
| 🟡 v4-M8 — Health Apps Declaration | OFFEN | 🟡 **NOCH OFFEN** (Play-Console-Submit) | `docs/health-apps-declaration.md` liegt vor — bei Submit einreichen |

**Kurzfazit:** Seit v5 wurden 3 wichtige Befunde gefixt (NB1, NB8, M7-implizit). Die zwei v5-HOCH-Punkte (§ 356a Widerrufsbutton und KI-Badge bei Text-Improve) sind weiterhin offen — beide stichtagsgebunden.

---

## 3. NEUE Befunde (v6)

### 🟠 HOCH

#### NB-v6-1 — Bestellbutton-Beschriftung § 312j Abs. 3 BGB nicht eindeutig "zahlungspflichtig"

**Fundstelle:** `PaywallScreen.kt` Zeilen 447-475 (Yearly-CTA), 505-520 (Monthly-CTA), 558-570 (Lifetime-CTA)
```kotlin
OutlinedButton(
    onClick = { ... viewModel.launchPurchaseFlow(act, isYearly = true) },
    ...
) {
    Text(text = stringResource(R.string.paywall_from_per_day, dailyPrice))
    Text(text = stringResource(R.string.paywall_instead_per_month, displayMonthlyPrice))
}
```

**Strings:**
- `paywall_from_per_day`: "Ab %1$s pro Tag"
- `paywall_instead_per_month`: "statt %1$s pro Monat"

**Problem:** § 312j Abs. 3 BGB ("Bestellbutton-Pflicht") verlangt eine Beschriftung, die den Verbraucher **eindeutig** auf die Zahlungspflicht hinweist. Beispiele aus BGH-Rechtsprechung und IT-Recht-Kanzlei:
- ✅ "Jetzt zahlungspflichtig bestellen"
- ✅ "Zahlungspflichtig abonnieren"
- ✅ "Kostenpflichtig kaufen"
- ❌ Reine Preisangabe ("Ab 0,08 EUR pro Tag")
- ❌ "Weiter", "Bestellen" allein

**Mildernder Faktor:** Google Play Billing schiebt einen eigenen "Abonnieren"-Bottom-Sheet dazwischen, der den Preis und das Abrechnungsintervall klar zeigt — das ist Googles standardisierter Flow. Aber der Anbieter ist trotzdem fuer den **App-internen** Bestellbutton verantwortlich.

**Abmahn-Risiko:** MITTEL-HOCH.
- Wettbewerber-Abmahnung typisch 5.000-15.000 EUR Streitwert
- Klassisches Abmahnthema, IT-Recht-Kanzlei warnt aktiv 2025/2026

**Fix (15-30 Min):**

Variante A (minimal): String hinzufuegen + Pflicht-Footer-Text unter dem Button:
```xml
<string name="paywall_legal_footer">Mit Klick auf den Button schliesst du ein zahlungspflichtiges Abo ab.</string>
```

Variante B (besser): Button-Beschriftung selbst aendern:
```kotlin
Text(text = stringResource(R.string.paywall_subscribe_yearly_priced, displayPrice))
// "Jetzt zahlungspflichtig fuer %1$s/Jahr abonnieren"
```

**Empfehlung:** Variante B fuer alle 3 Buttons (Yearly, Monthly, Lifetime). Der zusaetzliche Footer "Auto-Verlaengerung — kuendbar in Google Play" steht ohnehin schon irgendwo in der Paywall (zu pruefen).

**Anwaltspruefung:** PFLICHT vor Release.

---

#### NB-v6-2 — Sofortleistungs-Verzicht (§ 356 Abs. 5 BGB) ohne separate Checkbox in Paywall

**Fundstelle:**
- AGB §16: "Wenn Sie im Kaufprozess (Google Play) der sofortigen Aktivierung der Premium-Funktionen zustimmen, erlischt Ihr Widerrufsrecht nach § 356 Abs. 5 BGB."
- PaywallScreen.kt: keine eigenstaendige Checkbox vor `launchPurchaseFlow()`

**Problem:** Damit das Widerrufsrecht nach § 356 Abs. 5 BGB wirksam erlischt, muss der Verbraucher VOR dem Kauf:
1. **ausdruecklich** zustimmen, dass die Leistung vor Ablauf der 14-Tage-Frist beginnt, und
2. **bestaetigen**, dass er weiss, dass er dadurch sein Widerrufsrecht verliert.

Der Google-Play-Billing-Flow erfuellt das **nicht eindeutig** — Google zeigt Preis + "Abonnieren" + AGB-Verweis, aber **keine zwei-stufige Erklaerung** wie das BGB sie fordert.

**Mildernder Faktor:** Es gibt eine etablierte Praxis, dass App-Anbieter sich auf Google Plays Flow verlassen. Eine BGH-Entscheidung speziell fuer App-Stores fehlt. Bei strikter Lesart waere die App-eigene Checkbox jedoch sicherer.

**Abmahn-Risiko:** NIEDRIG-MITTEL. Wenn jemand 14 Tage nach Kauf widerruft und Geld zurueckfordert mit Argument "ich habe nie ausdruecklich verzichtet", ist die Rechtslage unklar.

**Fix (1-2 Std):** Checkbox in Paywall einfuegen:
```kotlin
val agreesToImmediateExecution = remember { mutableStateOf(false) }
Checkbox(
    checked = agreesToImmediateExecution.value,
    onCheckedChange = { agreesToImmediateExecution.value = it }
)
Text(stringResource(R.string.paywall_356_consent))
// "Ich stimme zu, dass die Bereitstellung der Premium-Funktionen sofort
//  nach Bestellung beginnt. Mir ist bekannt, dass ich dadurch mein
//  14-tägiges Widerrufsrecht verliere."
```
Button `enabled = agreesToImmediateExecution.value`.

**Anwaltspruefung:** Stark empfohlen — diese Checkbox ist defensiver als die Verlassen-auf-Google-Play-Variante.

---

### 🟡 MITTEL

#### NB-v6-3 — UK GDPR Art. 27 Vertreter (Pflicht wenn UK-Markt)

**Quelle:** Researcher 4 (US/UK/AU/CA), Mishcon de Reya, ICO Guidance.

**Problem:** Die App soll laut Benutzer in 27 Sprachen und weltweit (ausser CA/Quebec) erscheinen — also auch in UK. UK GDPR Art. 27 verpflichtet Anbieter ohne UK-Niederlassung, einen **UK-Vertreter** zu benennen. Ausnahme greift NICHT, weil:
- Mood-Daten = Art. 9 UK GDPR (besondere Kategorie)
- Verarbeitung ist Kern-Usecase, nicht "gelegentlich"

**Befund-Ort im Repo:** Keine Erwaehnung eines UK-Vertreters in DE+EN+25 Locale-Texten gefunden.

**Risiko:** ICO-Verstoss, Bussgeld bis £17,5 Mio (max), praktisch fuer kleine Anbieter eher Konformitaets-Anweisung.

**Fix:** Entweder:
- **Variante A:** UK-Vertreter beauftragen (~£100-300/Jahr bei GDPR Local, EUverify, Captain Compliance), Name+Adresse+E-Mail in DSE+Impressum aufnehmen
- **Variante B:** UK in Play Console aus Distribution ausschliessen (analog zu Quebec)

**Empfehlung:** Variante A — UK ist ein wichtiger englischsprachiger Markt, und Vertreter ist die preiswerte Option.

---

#### NB-v6-4 — Health Apps Declaration (Google Play, Stand Aug-2025-Policy)

**Quelle:** Researcher 2 (Google Play Policies), `myappmonitor.com` Health-Update.

**Problem:** Die Google-Play-Health-Apps-Policy wurde im August 2025 erweitert auf "all apps with health-related functionality — not just dedicated medical apps". Mood-Tracking faellt explizit unter Health & Fitness → Health Info.

**Pflicht beim Submit:**
- Health Apps Declaration Form ausfuellen
- Privacy Policy oeffentlich zugaenglich, nicht editierbar, nicht geo-fenced
- Disclaimer "Diese App ist kein Medizinprodukt"
- Hinweis "Bitte konsultiere bei gesundheitlichen Fragen einen Arzt"

**Befund-Ort im Repo:**
- ✅ `docs/health-apps-declaration.md` existiert
- ✅ `CrisisHelpDialog.kt` (laut v5-Notiz) existiert
- 🟡 Disclaimer-Text in DSE/AGB nicht inhaltlich geprueft heute

**Fix:** Beim Play-Console-Upload das Health-Declaration-Formular ausfuellen. Inhalt von `docs/health-apps-declaration.md` als Vorlage nutzen.

---

#### NB-v6-5 — Korea PIPA: Koreanisch-Pflicht aber HTML-Kurzfassungen koennen veraltet sein

**Quelle:** Researcher 5 (Asien/LatAm), VeraSafe, Baker McKenzie.

**Problem:** Korea PIPA ist eines der wenigen Gesetze, die explizit eine **landessprachliche Privacy Policy** verlangen, sobald die App koreanische Nutzer aktiv anspricht. Die App ist in 27 Sprachen, inkl. Koreanisch — Play-Store-Listing entscheidet ob "aktiv ansprechen".

**Befund-Ort im Repo:**
- ✅ `app/src/main/assets/legal/ko/PRIVACY.html` existiert (mit IMPRINT.html + TERMS.html)
- 🟡 Inhalt heute nicht zeilenweise geprueft auf vollstaendige PIPA-Pflichtangaben (Drittlandsuebermittlung Empfaenger Google LLC, Zweck, Dauer, Rechtsausuebungsweg)

**Fix:** Vor Korea-Release die `legal/ko/PRIVACY.html` durch einen koreanischsprachigen Anwalt oder PIPA-spezialisierten Service pruefen lassen. Alternative: Korea aus Distribution ausschliessen.

---

#### NB-v6-6 — Brasilien LGPD: SCCs seit 23.08.2025 Pflicht fuer USA-Transfers

**Quelle:** Researcher 5 (Asien/LatAm), Hogan Lovells, ANPD-Resolution.

**Problem:** Seit 23.08.2025 verlangt die brasilianische Datenschutzbehoerde ANPD explizite **ANPD-genehmigte Standard Contractual Clauses (SCCs)** fuer Transfers in Nicht-Adequacy-Laender (= USA fuer Brasilien). Die DE+EN-DSE erwaehnt EU-SCCs (Art. 46 DSGVO), aber NICHT die ANPD-SCCs.

**Mildernder Faktor:** Google Cloud DPA (das Google standardmaessig anbietet) deckt LGPD-SCCs ab. Wer Google's DPA akzeptiert hat, ist abgesichert.

**Fix:** In `legal/pt-BR/PRIVACY.html` einen Hinweis ergaenzen: *"Para transferencias para os EUA (Google LLC, Microsoft, Groq) sao usadas as Clausulas-Padrao Contratuais aprovadas pela ANPD em conformidade com a LGPD Art. 33."* Oder im DE+EN-Text den ANPD-SCC explizit erwaehnen.

---

#### NB-v6-7 — Australien: AI-Disclosure-Pflicht ab Dezember 2026 vorbereiten

**Quelle:** Researcher 4 (US/UK/AU/CA), OAIC Privacy Act 2024 Reform.

**Problem:** Australiens Privacy-Act-Reform (Tranche 2) bringt **ab Dezember 2026** eine Automated Decision-Making Disclosure-Pflicht. Apps mit AI-Features die Nutzer "significantly affect", muessen offenlegen.

**Mildernder Faktor:** Stichtag liegt 7 Monate nach Release-Ziel. Mood-Tracking-Coaching wird wahrscheinlich nicht als "significant impact" eingestuft.

**Fix:** Reminder im Backlog setzen — Dezember 2026 erneut pruefen. Aktuell kein akuter Handlungsbedarf.

---

### 🟢 NIEDRIG

#### NB-v6-8 — ACCESS_COARSE_LOCATION rein fuer Theme-Feature

**Fundstelle:** `AndroidManifest.xml` Zeile 6, DSE Section 3.4

**Problem:** Standort-Permission ausschliesslich fuer "Sonnenstand-Theme" (Dunkelmodus passt sich an Sonnenauf-/-untergang an). Das ist datenschutzfreundlich umgesetzt:
- Einmalige Abfrage beim Aktivieren
- Lokale Speicherung
- Keine Uebertragung

**Risiko:** Sehr niedrig — DSE dokumentiert das transparent. Aber: Google Play Permissions Declaration koennte trotzdem Erklaerung verlangen, weil "core feature" hier nicht offensichtlich ist.

**Fix:** Bei Play-Console-Submit Begruendung mitliefern: "Used solely for theme adjustment based on local sunrise/sunset times. No location data leaves the device." Alternative: User koennte Stadt manuell eingeben (PLZ-Picker) — aber UX-Verschlechterung.

---

#### NB-v6-9 — DSE-Stand 20.04.2026 ist 8 Tage alt — frisch

**Fundstelle:** DSE Zeile 3 "Stand: 20. April 2026"

**Beobachtung:** Datum ist aktuell. Nach jedem Material-Change der DSE (neue SDKs, neue Permissions, neue Verarbeitung) muss `CURRENT_POLICY_VERSION` in `Constants.kt` (aktuell "3.0") gebumpt werden, damit `PREF_CONSENT_POLICY_VERSION` einen Re-Consent triggert. Das ist sauber implementiert.

**Empfehlung:** Bei jeder DSE-Aenderung das Datum + Policy-Version syncron halten.

---

#### NB-v6-10 — Premium-Preis-Display als Hardcoded-Fallback

**Fundstelle:** `Constants.kt` Zeilen MONTHLY_PRICE_DISPLAY="3,99 EUR", YEARLY_PRICE_DISPLAY="29,99 EUR", LIFETIME_PRICE_DISPLAY="79,99 EUR"

**Problem:** Hardcoded Preise als Fallback, falls Google Play Billing keine lokalisierten Preise zurueckgibt. Bei Preisaenderung muessen Code-Aenderung + Release-Build + Play-Store-Aenderung syncron sein.

**Risiko:** Wenn Code-Fallback und Play-Store-Echtpreis abweichen, koennte ein Verbraucher das in der Paywall sehen → Irrefuehrung nach UWG.

**Fix:** Sicherstellen dass Hardcoded-Werte exakt mit den Play-Store-Preisen uebereinstimmen, oder Fallback-Anzeige unterdruecken bis Billing geladen ist.

---

## 4. Code-vs-Text-vs-Play-Matrix

| Daten/Feature | Code/SDK/Permission | DSE | Data Safety | Consent/UI | Status |
|---|---|---|---|---|---|
| Lokale Tagebuchdatenbank | Room SQLite, EncryptedSharedPrefs | ✅ Section 4 | "Collected: User content" | Onboarding | ✅ konsistent |
| Lokale Whisper STT | Sherpa-ONNX (auf Geraet) | ✅ Section 5.2 "keine Datenuebermittlung" | Audio: not collected | — | ✅ konsistent |
| Cloud-Whisper (Groq) | GroqApi via Retrofit | ✅ Section 5.1 | Audio: shared with Groq | PrivacyGate Dialog | ✅ konsistent |
| Firebase AI / Gemini | firebase-ai BOM, GenerativeBackend.googleAI() | ✅ Section 5.6 | Text: shared with Google | PrivacyGate Dialog (Art. 9) | ✅ konsistent |
| Google Drive Backup | google-drive-api + Credentials | ✅ Section 5.3 | User content: stored | Settings opt-in | ✅ konsistent |
| Edge-TTS | OkHttp WebSocket Bing Speech | ✅ Section 5.2a | Text: shared (audio-out) | PrivacyGate Dialog | ✅ konsistent |
| Firebase Analytics | firebase-analytics | ✅ Section 5.7 (Opt-In) | Analytics: shared with Google | Consent + Settings Toggle | ✅ konsistent (default OFF) |
| Firebase App Check | firebase-appcheck-playintegrity | ✅ Section 5.8 (berechtigtes Interesse) | Device ID | — | ✅ konsistent |
| Firebase Remote Config | firebase-config | ✅ Section 5.9 | Device ID | — | ✅ konsistent |
| Google Sign-In (optional) | credentials + google-id | ✅ Section 5.4 | Email, Account ID | Settings | ✅ konsistent |
| In-App-Kauf | play-billing | ✅ Section 6 | Purchases | Paywall | 🟠 Bestellbutton-Beschriftung (NB-v6-1) |
| Mikrofon | RECORD_AUDIO | ✅ Section 3.2 | Audio recordings | Runtime | ✅ konsistent |
| Kamera | CAMERA | ✅ Section 3.3 | Photos | Runtime | ✅ konsistent |
| Standort (Theme) | ACCESS_COARSE_LOCATION | ✅ Section 3.4 | not shared | Settings opt-in | 🟢 (NB-v6-8 Play Console Begruendung) |
| Notifications | POST_NOTIFICATIONS | ✅ Section 3.5 | not shared | Runtime | ✅ konsistent |
| Boot-Reminder | RECEIVE_BOOT_COMPLETED | ✅ Section 3.6 | not shared | — | ✅ konsistent |

---

## 5. Android-Sicherheitscheck

| Kontrolle | Ergebnis | Risiko | Notiz |
|-----------|----------|--------|-------|
| Permissions minimal | ✅ Nur 7 Permissions, alle begruendet | Niedrig | Standort fuer Theme — siehe NB-v6-8 |
| Backup-Regeln (Auto-Backup + DataExtraction) | ✅ Sensitive DBs ausgeschlossen, EncryptedPrefs ausgeschlossen | Niedrig | sehr saubere `backup_rules.xml` + `data_extraction_rules.xml` |
| TLS / Cleartext-Traffic | ✅ `cleartextTrafficPermitted="false"` | Niedrig | `network_security_config.xml` minimal+korrekt |
| Sensitive Logs | (nicht geprueft heute — siehe Empfehlung) | Niedrig | empfohlen: vor Release `Log.d` mit User-Daten in Release entfernen — pruefen ob R8/ProGuard das macht |
| Secrets im Repo | ✅ SK-Folder-Pattern | Niedrig | google-services.json + Keystores via `~/SK/BestJournalAndroid/`. GROQ_API_KEY via Firebase Remote Config. Google OAuth Web-Client-ID ist public-OK. |
| Exported Components | ✅ Nur MainActivity (LAUNCHER) und BootReminderReceiver (BOOT_COMPLETED) | Niedrig | Standard-Pattern, nichts kritisches |
| WebView | (nicht geprueft heute — App scheint keine WebView zu nutzen) | — | DOC-Asset HTML wird via FileProvider gelesen |
| App Check / Play Integrity | ✅ aktiviert | Niedrig | Schutz fuer Firebase-Endpunkte gegen Missbrauch |
| EncryptedSharedPreferences | ✅ via security-crypto | Niedrig | androidx.security:security-crypto fuer Tokens |

---

## 6. Sprach- und Marktfreigabe-Matrix

| Markt | App-Locale | Rechtstexte (DE+EN Lang + Locale-Kurz) | Pflicht/Empfehlung | Stand |
|---|---|---|---|---|
| Deutschland | de | ✅ Lang + Kurz | DE Pflicht | ✅ |
| Oesterreich | de | ✅ Lang + Kurz | DE-Lang reicht | ✅ |
| Schweiz | de | ✅ Lang + Kurz | DE-Lang reicht (revDSG kurz erwaehnt empfohlen) | ✅ |
| EU restlich | en + 25 Locales | ✅ Locale-Kurz | Landessprache empfohlen | ✅ |
| UK | en | ✅ EN-Lang | EN reicht; **🟡 Art. 27 Vertreter** (NB-v6-3) | 🟡 |
| USA | en | ✅ EN-Lang inkl. CCPA-Section | EN reicht | ✅ |
| Kalifornien | en | ✅ Section 8a.1 mit GPC-Hinweis | EN reicht | ✅ |
| Kanada (ohne Quebec) | en | ✅ Section 8a.6 PIPEDA | EN reicht | ✅ |
| Quebec | — | — | **AUSGESCHLOSSEN in Play Console** | ✅ (Memory-Notiz `country-exclusion.md`) |
| Australien | en | ✅ Section 8a.7 APP | EN reicht; AI-Disclosure ab Dez 2026 (NB-v6-7) | ✅ |
| Neuseeland | en | ✅ erwaehnt | EN reicht | ✅ |
| Japan | ja | ✅ Locale-Kurz | JA empfohlen | ✅ (Inhalt nicht zeilenweise geprueft) |
| Korea | ko | ✅ Locale-Kurz | **KO Pflicht** | 🟡 (NB-v6-5 Pruefung empfohlen) |
| Indien | en + 7 Indien-Sprachen (hi, bn, gu, kn, ml, mr, ta, te, ur) | ✅ Locale-Kurz | EN reicht (DPDP enforcement erst Mai 2027) | ✅ |
| Brasilien | pt-BR | ✅ Locale-Kurz | PT-BR empfohlen | 🟡 (NB-v6-6 ANPD-SCC ergaenzen) |
| China | — | — | **AUSSCHLIESSEN empfehlen** (Play Store dort gesperrt, PIPL-Aufwand extrem) | 🟡 (Memory-Notiz pruefen) |

---

## 7. Play-Console-Checkliste

| Punkt | Status | Notiz |
|---|---|---|
| Privacy Policy URL erreichbar | 🟡 (zu pruefen) | URL muss live, nicht geo-fenced sein |
| Account Deletion Web-URL | 🟡 (zu pruefen) | `docs/account-deletion.html` existiert, muss gehostet werden |
| Data Safety Form ausgefuellt | 🟡 (Submit-Aufgabe) | App-Activity, Audio, Health Info, Financial, Device IDs |
| App Access (Login-Demo) | (zu erfassen) | Google Sign-In Test-Account fuer Reviewer |
| Content Rating (IARC) | (zu erfassen) | Erwartet: Teen/12 wg. sensitive themes (Mental-Health-Touch) |
| Target Audience | ✅ | App fuer Erwachsene; Age-Gate-Strategie pruefen |
| Ads Declaration | ✅ Nein | Keine Ads-SDKs |
| Health Apps Declaration | 🟡 PFLICHT (NB-v6-4) | `docs/health-apps-declaration.md` als Vorlage |
| AI-Generated Content | (siehe Researcher #2) | App nutzt AI nur zur Verbesserung bestehender Features → wahrscheinlich exempt |
| Permissions Declaration | 🟡 (Standort-Begruendung) | NB-v6-8 |
| Data Deletion Frage | ✅ | Account deletion implementiert |
| targetSdkVersion | ✅ 36 (Android 16) | uebertrifft Pflicht-API 35 |
| Play Billing Library Version | (zu erfassen — `libs.play.billing` aufloesen) | Pflicht: Version 7+ |
| Lokalisierte Store-Listings | ✅ 27 Sprachen | sehr gruendlich |
| Quebec-Ausschluss | ✅ | siehe `play-store-metadata/country-exclusion.md` |

---

## 8. Fix-Reihenfolge

### Reihenfolge 1 — vor JEDEM Submit-Versuch (Hoch + stichtagsgebunden)

1. **NB-v6-1: Bestellbutton-Beschriftung** in Paywall (15-30 Min Code + 27 String-Locales)
2. **v5-Resterledigung: KI-Badge bei Text-Improve-Output** im EntryDetailScreen (1-2 Std)

### Reihenfolge 2 — vor Play-Store-Submit

3. **NB-v6-4: Health Apps Declaration** ausfuellen (Play Console)
4. **NB-v6-2: Sofortleistungs-Verzicht-Checkbox** in Paywall (1-2 Std + Anwaltspruefung)
5. **NB-v6-8: Standort-Permission-Begruendung** in Play Console mitliefern

### Reihenfolge 3 — kurz nach Erst-Release

6. **§ 356a Widerrufsbutton (Stichtag 19.06.2026)** — Mailto → Gmail-API-Versand (3-4 Std). Kann vor Submit umgesetzt werden, blockiert aber nicht den Submit selbst, sofern bis 19.06. live.
7. **NB-v6-3: UK Art. 27 Vertreter** (extern bestellen ~£200/Jahr) ODER UK ausschliessen
8. **NB-v6-5: Korea PIPA-Anwaltspruefung** der `legal/ko/PRIVACY.html`
9. **NB-v6-6: Brasilien ANPD-SCC-Hinweis** in `legal/pt-BR/PRIVACY.html`

### Reihenfolge 4 — Backlog (>3 Monate)

10. **NB-v6-7: Australien AI-Disclosure** vorbereiten fuer Dezember 2026
11. **NB-v6-9/10: Preis-Hardcoded-Fallback + DSE-Stand-Sync** als kontinuierliche Pflege
12. **TODO LEGAL** Quebec/Kanada-Update (separate Session laut Memory-Notiz `project_quebec_canada_future_update.md`)

---

## 9. Compliance-Diff (was wurde rechtlich angepasst seit v5)

Da diese Audit-Iteration **keine Code-Aenderungen** vornimmt, gibt es kein Diff-Output. Stattdessen:

**Status-Updates** (was wurde seit v5 gefixt vom Benutzer/in anderen Sessions):
- ✅ Groq-DPF-Falschaussage in DE DSE entfernt (NB1 v5)
- ✅ DSE-Aussage zu "ohne zusaetzliche Bestaetigung" auf Pre-Usage-Gate angepasst (NB8 v5)
- ✅ Art. 9 DSGVO explizite Einwilligung in `privacy_gate_gemini_body` korrekt formuliert

**Offene Punkte** werden durch diesen Bericht zur Wissensbasis hinzugefuegt — siehe `tools/rechtssicherheit.md`.

---

## 10. Quellen (Auszug — vollstaendige Liste in tools/rechtssicherheit.md)

- DDG § 5 Anbieterkennzeichnung — gesetze-im-internet.de/ddg/__5.html (abgerufen 28.04.2026)
- BGB § 312j Bestellbutton-Pflicht — gesetze-im-internet.de
- BGB § 356a Widerrufsbutton ab 19.06.2026 — noerr.com (abgerufen 28.04.2026)
- BGH XI ZR 139/23 (19.11.2024) Zustimmungsfiktion unwirksam
- BGH III ZR 59/24 (10.07.2025) AGB-Verweis bei Vertragsschluss unwirksam
- EU AI Act Art. 50 Transparenzpflichten — ai-act-law.eu/de/artikel/50/
- Google Play Health Apps Policy (Aug 2025) — myappmonitor.com Health-Update
- Google Play Account Deletion Policy — support.google.com/googleplay/android-developer/answer/13327111
- Google Play Data Safety Form — support.google.com/googleplay/android-developer/answer/10787469
- Google Play AI-Generated Content Policy — support.google.com/googleplay/android-developer/answer/14094294
- UK GDPR Art. 27 Representatives — mishcon.com/uk-gdpr/article-27
- CCPA/CPRA 2026 — oag.ca.gov/privacy/ccpa
- LGPD Brasilien ANPD-SCCs Aug 2025 — hoganlovells.com
- Korea PIPA Foreign Business Guidelines — Baker McKenzie
- Australia Privacy Act 1988 + 2024 Reform — oaic.gov.au

---

## Abschluss-Disclaimer

**Dieser Bericht ist eine technische Pruefhilfe und ersetzt KEINE anwaltliche Beratung.** Vor dem Play-Store-Release MUSS ein Fachanwalt fuer IT-Recht alle Dokumente pruefen — insbesondere:
1. Bestellbutton-Beschriftung in Paywall (NB-v6-1)
2. Sofortleistungs-Verzicht-Checkbox (NB-v6-2)
3. § 356a BGB Widerrufsbutton (Stichtag 19.06.2026)
4. AGB §11 Zustimmungsfiktion (BGH 19.11.2024 — Klausel pruefen)

Die App hat ein **ueberdurchschnittlich gruendliches Compliance-Setup** — DSE+ToS+Impressum, granulare Per-Service-Consents, Opt-in Analytics+Drive-Backup, CCPA-Toggle, EU AI Act Art. 9 explizite Einwilligung, korrekte Network-Security-Config, saubere Backup-Regeln, 27 Locale-Versionen der Rechtstexte, Account-Deletion via UI+Web. Das technische Fundament ist solide.

**Ohne Anwaltspruefung der 4 oben genannten Punkte ist der Release nicht zu empfehlen.**

---

*Audit-Stand: 28.04.2026 | naechste Pflicht-Iteration: vor Play-Store-Submit oder bei Material-Changes*
