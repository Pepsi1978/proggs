# Rechtssicherheits-Audit v2: BestJournalAndroid

**Datum:** 2026-04-20 (zweite Pruefung, nach Fixes)
**Vergleich gegen:** RECHTSSICHERHEIT-AUDIT-2026-04-20.md (v1)
**Pruefer:** Rechtssicherheit-Skill (technische Pruefhilfe, keine anwaltliche Beratung)
**Geprueft gegen:** DSGVO, DDG, TDDDG, AI Act, CCPA 2026, CPRA, UK-GDPR+DUAA 2025, PIPEDA, Quebec Law 25+Loi 96, Australian Privacy Act 2024, PIPA, APPI, DPDP 2023, PDPL Vietnam 2026, Google Play Policies 2025-2026, aktuelle Abmahn-Rechtsprechung 2025-2026.

---

## Disclaimer

Dieser Bericht ist eine **technische Pruefhilfe** und ersetzt KEINE anwaltliche Beratung.
Er markiert fehlende Pflichtangaben und typische Fallstricke basierend auf oeffentlichen
Quellen (5 parallele Researcher, Stand April 2026). Fuer eine verbindliche Rechtspruefung
vor dem weltweiten Rollout muss ein Fachanwalt fuer IT-Recht konsultiert werden.

---

## Zusammenfassung

| Kategorie | v1 (Erstpruefung) | v2 (nach Fixes) |
|-----------|------------------|-----------------|
| 🔴 KRITISCH (Release blockieren) | 5 | **1** |
| 🟠 HOCH (vor Release fixen) | 6 | **2** |
| 🟡 MITTEL (sollte fixen) | 7 | **4** |
| 🟢 OK | 13 | **21** |

**Gesamtstatus: BEDINGT RELEASE-FAEHIG** — 1 kritischer Restbefund, 2 hohe neue Befunde.
Nach Fix dieser 3 Punkte kann die App fuer die in der Laender-Matrix dokumentierten Maerkte
produktiv gehen. Die grossen Rechtsluecken der Erstpruefung sind geschlossen.

---

## Verifikation der v1-Befunde

### ✅ K1. ODR-Plattform-Links — GEFIXT

Grep nach `ec.europa.eu/consumers/odr` findet KEINEN Treffer mehr in Rechtsdokumenten.
Einziger verbleibender Treffer ist der historische v1-Auditbericht selbst (korrekt). Die Abmahnwelle
seit Q3/2025 ist damit geschlossen.

### ✅ K2. LegalDocumentScreen locale-aware — GEFIXT

`LegalDocumentScreen.kt:67-71` prueft jetzt `Locale.getDefault().language` und laedt
`legal/de/$deFileName` oder `legal/en/$enFileName`. Beide Asset-Ordner sind befuellt
(`assets/legal/de/*.html` und `assets/legal/en/*.html`).

### ✅ K3. Englische Datenschutzerklaerung — GEFIXT

`docs/PRIVACY.en.md` + `docs/PRIVACY.en.html` + `assets/legal/en/PRIVACY.html` vorhanden.

### ⚠️ K4. Granulare Consent — TEILWEISE GEFIXT (siehe NK1)

`PrivacyGateDialog.kt` existiert und unterstuetzt Per-Service-Consent (Groq, Gemini, Edge TTS).
ABER: Im Code ist nur **Groq** in `JournalScreen.kt:184-225` verdrahtet. Gemini- und
Edge-TTS-Gates sind nicht getriggert. Siehe neuen Befund NK1.

### ✅ K5. Laenderspezifische Rights-Sections — GEFIXT

`PRIVACY.en.md` enthaelt:
- 8a.1 California CCPA/CPRA
- 8a.2 Texas TDPSA + weitere State Laws (mit GPC-Honoring)
- 8a.3 Illinois BIPA (Voice-Retention)
- 8a.4 UK GDPR + ICO-Beschwerdeweg + UK DUAA 2025 Referenz
- 8a.6 Quebec Law 25 + CAI

### ✅ H6. Account-Deletion Web-URL — GEFIXT

`docs/account-deletion.html` existiert, `play-store-metadata/country-exclusion.md`
dokumentiert `https://pepsi1978.github.io/bestjournal-deletion/` als Web-URL.

### ✅ M1. AI-Report-Mechanismus — GEFIXT

`SettingsScreen.kt:2541-2615` hat Two-Step-Dialog + Mailto-Intent fuer AI-Report.
Entspricht Google Play AI-Generated Content Policy.

### ✅ M2. AI-Act Art. 50 Hinweis — GEFIXT

`DATENSCHUTZ.md` § 12a "Hinweis auf KI-Systeme (Art. 50 KI-Verordnung)".

---

## 🔴 KRITISCH (neu oder Rest)

### NK1. Gemini + Edge-TTS Pre-Usage-Gate fehlt (K4-Rest)

**Fakt:** Der `PrivacyGateDialog` ist fuer Groq implementiert, aber:
- Kein `showGeminiPrivacyGate`-Trigger gefunden — Gemini-Aufrufe (Dashboard-KI, Rueckblicke)
  senden Daten an Google USA **ohne Pre-Usage-Hinweis**.
- Kein `showEdgePrivacyGate`-Trigger gefunden — Edge-TTS-Aufrufe senden Texte an Microsoft USA
  ohne Pre-Usage-Hinweis.

**Risiko:** EDSA-Leitlinie 03/2023 "Ablehnen gleich einfach wie Annehmen" + Art. 13 DSGVO
Transparenzpflicht. Besonders kritisch wegen **California SB 243 Companion Chatbot Law
(seit 01.01.2026)**: KI-gestuetzte Journal-Assistenz koennte als "Companion Chatbot" einzuordnen sein,
dann waere Disclosure beim ersten Kontakt Pflicht.

**Fix:**
1. In `DashboardScreen.kt` vor jedem Gemini-Call: `PrivacyGateHelper.hasConsented(GEMINI)` pruefen,
   sonst `PrivacyGateDialog` zeigen.
2. In der TTS-Spielstelle (vermutlich `TtsPlaybackScreen` / `RetrospectiveScreen`) analog fuer EdgeTTS.
3. Bereits vorhandene Strings `privacy_gate_gemini_*` und `privacy_gate_edge_*` pruefen und
   gegebenenfalls in `values/strings.xml` ergaenzen.

**Aufwand:** 30-60 Min (Dialog existiert, nur Verdrahtung fehlt).

---

## 🟠 HOCH

### NH1. Sprachdeckung der Rechtstexte vs. Play-Store-Locales

**Fakt:** Die App ist fuer 27 Locales lokalisiert:
`de, en, fr, es, pt-rBR, pt-rPT, it, nl, pl, uk, tr, ja, ko, zh-rCN, zh-rTW, ar, hi, ur, bn, ta, te, mr, ml, kn, gu, in, th`.
Rechtsdokumente existieren nur auf **Deutsch und Englisch**.

**Risiko pro Markt:**

| UI-Sprache | Markt | Rechtsrisiko | Empfehlung |
|------------|-------|-------------|------------|
| ko | Korea | 🔴 PIPC Corrective Order (DeepSeek-Praezedenz) | Land ausschliessen ODER ko-Rechtstexte erstellen |
| ar | Saudi-Arabien | 🔴 SDAIA bis 5 Mio SAR | Ausschliessen (siehe country-exclusion.md) |
| tr | Tuerkei | 🔴 VERBIS-Pflicht | Ausschliessen (siehe country-exclusion.md) |
| pt-rBR | Brasilien | 🔴 ANPD-SCCs seit 23.08.2025 | Ausschliessen |
| zh-rCN | China | — | Google Play ohnehin nicht verfuegbar |
| ja | Japan | 🟡 PPC Erwartung Japanisch | Bedingt freigeben, Japanisch empfohlen |
| fr | Frankreich + Quebec | 🟡 Quebec Loi 96 seit 06/2025 | Quebec ausschliessen ODER fr-Rechtstexte |
| it, nl, pl, es, pt-rPT | EU | 🟢 Englische DSE wird EU-weit akzeptiert | FREI |
| hi, ur, bn, ta, te, mr, ml, kn, gu, in, th | IN, BD, ID, TH | 🟢 Englisch genuegt | FREI |

**Fix:** Im Play Console beim Upload **zwingend** deaktivieren: TR, KR, SA, BR, VN
(plus automatisch: CN, RU, BY, IR, KP, CU, SY). Dokumentiert in
`play-store-metadata/country-exclusion.md` — **muss beim tatsaechlichen Upload eingehalten werden**.

Fuer Quebec (Kanada-Teil): Entweder **Quebec separat ausschliessen** (schwer technisch
machbar in Play Console — Kanada ist eine Einheit) ODER franzoesische Rechtstexte UND
franzoesische App-UI (App hat bereits `values-fr/strings.xml`, aber keine `values-fr-rCA`).
**Pragmatische Empfehlung:** Kanada zunaechst nicht ausliefern, oder mit Restrisiko starten und
Quebec-Franzoesisch bei erstem Feedback nachruesten.

### NH2. CCPA-2026-Pflicht: Opt-Out-Bestaetigung + Privacy Policy in App-Settings

**Fakt (neu seit 01.01.2026):**
- CCPA-Novelle verlangt: Nach einem Opt-Out des Nutzers muss die App eine **Bestaetigung** zeigen
  ("Opt-Out Request Honored").
- Privacy Policy MUSS aus den App-Settings verlinkt sein (nicht nur im Consent-Screen).
- Global Privacy Control (GPC)-Signale muessen als valider Opt-Out anerkannt werden.

**Prufergebnis:**
- Privacy Policy Link in Settings: ✅ vorhanden ("Datenschutz" in Settings-Menue → WebView).
- Opt-Out-Bestaetigung: ❌ nicht implementiert. Wenn ein Nutzer per "Nur Erforderliches" wegklickt,
  gibt es keinen expliziten "Deine Opt-Out-Anfrage wurde registriert"-Dialog.
- GPC-Signal-Anerkennung: ⚠️ nicht implementiert. In der englischen DSE wird GPC als "honored"
  angekuendigt — eine native Android-App hat aber keinen Browser-GPC-Header. Fuer US-Nutzer
  sollte ein In-App-"Do Not Sell My Personal Information"-Toggle in Settings ergaenzt werden.

**Fix:**
1. In ConsentViewModel nach `acceptWithoutAnalytics()` einen Toast oder Snackbar
   "Opt-Out gespeichert — Analytics deaktiviert" hinzufuegen.
2. In `SettingsScreen.kt` einen separaten "Do Not Sell My Personal Information (California)"-Toggle
   hinzufuegen, sichtbar wenn Locale en-US und US-Nutzer.

**Aufwand:** 60 Min.

---

## 🟡 MITTEL

### NM1. Data Processing Agreement (DPA) mit Groq / Gemini / Microsoft nicht dokumentiert

**Fakt:** Google Play Data Safety Form unterscheidet "Service Provider" (kein "shared") vs.
"Third Party" ("shared" — Pflichtangabe). Ein Dritter gilt nur dann als Service Provider, wenn
eine DPA / DPIA existiert, die das Training auf Nutzerdaten verbietet und einen Auftragsverarbeiter-
Status schafft.

**Prufergebnis:** Kein Hinweis auf dokumentierten DPA-Abschluss im Repo. Die `play-store-metadata/
country-exclusion.md` deklariert bereits alle Dienste als "geteilt" — das ist die sichere Variante,
aber verlangt granulare User-Einwilligung nach EDSA.

**Empfehlung:** DPAs abschliessen und in den Rechtstexten namentlich erwaehnen. OpenAI/Anthropic
haben Enterprise-DPAs, Google hat Cloud Data Processing Addendum, Microsoft hat OST + DPA.

### NM2. California SB 243 Companion Chatbot Law (seit 01.01.2026)

**Fakt:** KI mit "natuerlicher Sprachschnittstelle" die soziale/emotionale Beduerfnisse adressiert
muss als KI gekennzeichnet sein beim ersten Kontakt. Ein KI-Tagebuchassistent koennte darunter fallen.

**Prufergebnis:** DATENSCHUTZ.md § 12a erwaehnt Art. 50 KI-VO (EU). SB 243 ist separat und US-spezifisch.

**Fix:** In `PRIVACY.en.md` Abschnitt 8a.1 oder neu 8a.7 California SB 243 erwaehnen:
> "This app uses AI-assisted features (journaling prompts, summaries, retrospectives). You are interacting
> with an AI system, not a human. Under California SB 243 (effective January 1, 2026), this disclosure is
> required before any companion chatbot interaction."

### NM3. Australischer Privacy Tort seit Juni 2025 + MFA-Empfehlung APP 11

**Fakt:** Australien hat seit Juni 2025 einen klagbaren Privacy Tort (Statutory Tort). APP 11 verlangt
**technische Massnahmen** (MFA, verschluesselte Speicherung).

**Prufergebnis:** App verschluesselt Cloud-Backups (siehe DATENSCHUTZ.md), aber MFA nicht erzwungen
bei Cloud-Sync (Google Drive nutzt Google-Konto-Auth, das uebernimmt das).

**Empfehlung:** In Abschnitt 8a.5 (Australien-Rights) kurz APP 11 + OAIC-Beschwerdeweg bestaetigen.

### NM4. Widerrufsbutton § 356a BGB (ab 19.06.2026)

**Fakt:** Ab 19.06.2026 Pflicht: In-App-Widerrufsbutton fuer Abos. BestJournal hat bereits
"Widerruf / Kaufsupport"-Button im Paywall-/Settings-Bereich (siehe commit #1594-#1596).

**Prufergebnis:** Funktional erfuellt, aber Formatvorgabe der Umsetzungsverordnung zu § 356a BGB
(noch nicht veroeffentlicht — Stand 04/2026) muss bis 06/2026 abgeglichen werden.

**Empfehlung:** Im Juni 2026 die finale Verordnung pruefen und Button ggf. anpassen.

---

## 🟢 OK — VERIFIZIERT (Auswahl der wichtigsten Punkte)

- Target SDK 35 / compileSdk 35 / minSdk 26 ✅
- Impressum § 5 DDG vollstaendig ✅
- DSA Art. 11 Kontaktstelle ✅
- Widerrufsbelehrung § 355/356 BGB + Muster-Widerrufsformular ✅
- § 356 Abs. 5 BGB Erloeschen-Klausel ✅
- Drittanbieter-Kette namentlich in DSE (Groq, Edge TTS, Gemini, Drive, Firebase) ✅
- Rechtsgrundlagen Art. 6 DSGVO je Dienst ✅
- DPF + SCCs als Transfer-Basis ✅
- Konto-Loeschen In-App + Web-URL ✅
- AI-Report-Button (Google Play Policy 04/2024) ✅
- AI-Act Art. 50 Hinweis ✅
- ODR-Links entfernt (Abmahnwelle Q3/2025 geschlossen) ✅
- LegalDocumentScreen locale-aware ✅
- Laenderspezifische Rights-Sections (CCPA, Texas, BIPA, UK, Quebec) ✅
- Kein MANAGE_EXTERNAL_STORAGE ✅
- Keine Meta-SDKs eingebunden → keine LG Luebeck/LG Jena-Risiken ✅
- Kein IDO-Risiko mehr (LG Wiesbaden 10/2025: IDO-Aktivlegitimation entzogen) ✅
- Mit Abmahnfaehigkeit durch Wettbewerber (BGH 27.03.2025) kompatibel ✅

---

## Laender-Freigabe-Matrix (final nach NK1-Fix)

| Region | Status | Zusatz |
|--------|--------|--------|
| DACH (DE/AT/CH) | 🟢 FREI | — |
| EU gesamt (NL/FR/IT/ES/PL/etc.) | 🟢 FREI | Englische DSE wird akzeptiert |
| UK | 🟢 FREI | ICO-Section + UK DUAA 2025 |
| Irland | 🟢 FREI | — |
| USA | 🟡 BEDINGT | NH2-Fix (Opt-Out-Confirmation + Do-Not-Sell-Toggle), danach FREI |
| Kanada (ohne Quebec) | 🟢 FREI | OPC-Section |
| Quebec | 🔴 AUSSCHLIESSEN | Loi 96 seit 06/2025 — franzoesische App-UI Pflicht (nicht nur Rechtstexte) |
| Australien | 🟢 FREI | APP 8 + OAIC, Privacy Tort ab 06/2025 beruecksichtigt |
| Neuseeland | 🟢 FREI | IPP 3A ab 05/2026 |
| Brasilien | 🔴 AUSSCHLIESSEN | Portugiesisch + ANPD-SCCs |
| Mexiko | 🟡 BEDINGT | Aviso de Privacidad Spanisch empfohlen |
| Argentinien/Chile/Kolumbien | 🟡 BEDINGT | Englisch weitgehend akzeptiert |
| Japan | 🟡 BEDINGT | Japanische DSE empfohlen (PPC-Erwartung) |
| Suedkorea | 🔴 AUSSCHLIESSEN | DeepSeek-Praezedenz + PIPA 10% Bussgeld ab 09/2026 |
| Indonesien | 🟡 BEDINGT | Bahasa empfohlen, Englisch technisch OK |
| Vietnam | 🔴 AUSSCHLIESSEN | PDPL 2026 seit 01.01.2026 in Kraft, MPS-Dossier |
| Singapur | 🟢 FREI | Englisch ist Amtssprache |
| Indien | 🟢 FREI | Uebergangsfrist bis 13.05.2027 (DPDP) |
| Tuerkei | 🔴 AUSSCHLIESSEN | VERBIS + KVKK-SCCs |
| Saudi-Arabien | 🔴 AUSSCHLIESSEN | Arabisch + SDAIA |
| VAE | 🟡 BEDINGT | Arabisch empfohlen |
| Israel | 🟢 FREI | EU-Adequacy |
| Suedafrika | 🟢 FREI | POPIA-Section |
| Kenia/Nigeria/Aegypten/Marokko | 🟡 BEDINGT | Englisch reicht weitgehend |
| China | 🔴 AUSSCHLIESSEN | Google Play ohnehin nicht verfuegbar |
| Russland/Belarus | 🔴 AUSSCHLIESSEN | Google Play Billing seit 03/2022 pausiert |
| Iran/Nordkorea/Kuba/Syrien/Sudan | automatisch ausgeschlossen | US-Sanktionen via Google |

**Play-Console-Strategie (final):** Zwingend ausschliessen — TR, KR, VN, SA, BR, CN, RU, BY + CA Quebec
(via Kanada-Ausschluss wenn Quebec nicht separat moeglich). Rest nach Umsetzung von NK1, NH1, NH2 freigeben.

---

## Fix-Reihenfolge fuer Release-Freigabe (2-3 Stunden)

1. **NK1** (60 Min) — Gemini- und Edge-TTS-PrivacyGate im DashboardScreen / Retrospective / TTS-Playback verdrahten.
2. **NH1** (15 Min beim Upload) — Play-Console: TR, KR, VN, SA, BR, CA (oder wenn Quebec-Risiko akzeptiert: nur Rest) ausschliessen.
3. **NH2** (60 Min) — Opt-Out-Bestaetigung + Do-Not-Sell-Toggle fuer US-Nutzer.
4. NM1 (einmalig) — DPA-Pakete bei Groq, Google Cloud, Microsoft pruefen/abschliessen.
5. NM2 (10 Min) — SB 243 Disclosure in PRIVACY.en.md ergaenzen.
6. NM3 (5 Min) — Australien-Section um APP 11 erweitern.
7. NM4 (im Juni 2026) — Widerrufsbutton-Verordnung pruefen.

---

## Neue Rechtssicherheits-Hotspots seit v1 (April 2026)

1. **California CCPA 2026-Novelle** — Opt-Out-Bestaetigungspflicht seit 01.01.2026
2. **California SB 243 Companion Chatbot Law** — seit 01.01.2026 fuer soziale/emotionale KI
3. **Korea PIPA 10% Umsatz-Bussgelder** — ab 11.09.2026
4. **Quebec Loi 96 Franzoesisch-Pflicht fuer Apps** — seit 06/2025
5. **Vietnam PDPL 2026** — seit 01.01.2026 in Kraft
6. **UK DUAA 2025** — Royal Assent 06/2025, aendert UK-GDPR
7. **IDO Verband Aktivlegitimation entzogen** — LG Wiesbaden 10/2025 (Entlastung fuer Entwickler)
8. **BGH Google-Fonts EuGH-Vorlage 08/2025** — Massenabmahnungen derzeit ausgesetzt bis EuGH-Entscheidung
9. **Meta-Tracking-Urteile 3.000-5.000 EUR** (LG Luebeck 11/2025, LG Jena 03/2026) — betrifft BestJournal nicht (kein Meta-SDK)

---

## Quellen-Stand

Basierend auf 5 parallelen Researchern am 2026-04-20 (v2):
- DE/EU (DSGVO, DDG, TDDDG, AI Act, § 312j BGB, § 356a BGB, BGH 27.03.2025, IDO LG Wiesbaden 10/2025)
- US/UK/CA/AU (CCPA 2026-Novelle, Quebec Law 25 + Loi 96 06/2025, Australian Privacy Act 2024+Privacy Tort 06/2025, UK DUAA 2025, COPPA Status, California SB 942 + SB 243)
- Asien (PIPL 2026-Cert, DPDP Rules 11/2025, APPI LINE-Fall, PIPA 10% 09/2026 + DeepSeek 02/2025 + AliExpress 07/2024, Vietnam PDPL 01/2026)
- Google Play Policies (Data Safety, Service Provider vs. Third Party, AI-Generated Content, Account Deletion, Families Policy, Health/Mental Health Daten)
- Abmahn-Trends (Meta-Tracking-Urteile 2025/2026, IDO-Sperrung, EuGH-Vorlage Google Fonts, Meta-Pixel ohne Consent)

---

**Abschliessender Disclaimer:** Diese Pruefung ist eine technische Pruefhilfe, kein Rechtsrat.
Vor dem produktiven Release wird die Konsultation eines spezialisierten IT-Recht-Anwalts
empfohlen. Schaetzkosten: 100-500 EUR fuer einmalige Pruefung von DSE/AGB/Impressum durch
Anwalt oder Service wie IT-Recht-Kanzlei (ca. 7,90-29,90 EUR/Monat).
