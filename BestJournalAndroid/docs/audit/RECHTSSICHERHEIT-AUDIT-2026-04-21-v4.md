# Rechtssicherheits-Audit BestJournalAndroid — v4

**Datum:** 2026-04-21
**Vorgaenger:** v3.1 (gleiches Datum, 5 iterative Laeufe, "RELEASE-FAEHIG")
**Anlass fuer v4:** Umfassende Recherche mit 5 parallelen Researchern zu aktuellem Rechtsstand (DE/EU, US/UK/Asien/LATAM, Google Play Policies, Formulierungs-Muster, Journaling-App-spezifische Risiken)
**Geprueft gegen:** DSGVO, TDDDG, DDG, UWG (EmpCo-RL), AI Act, DSA, CCPA/CPRA 2026, UK GDPR/DUAA 2025, PIPL, DPDP, APPI, PIPA, LGPD, IPP 3A (NZ), Google Play Policies 2026

---

## Disclaimer

Dieser Bericht ist eine **technische Pruefhilfe** und ersetzt KEINE anwaltliche Beratung.
Er dokumentiert Funde basierend auf oeffentlichen Quellen mit Stand 21.04.2026.
**Vor dem Release: Fachanwalt fuer IT-Recht konsultieren** — insbesondere fuer die Befunde H1, H3, H4 und M1.

---

## Zusammenfassung

| Dimension | v3.1 (Referenz) | v4 (neu) | Aenderung |
|-----------|-----------------|----------|-----------|
| Gesamtstatus | RELEASE-FAEHIG | **BEDINGT RELEASE-FAEHIG** | ⚠ Neue Befunde durch tiefere Pruefung + Rechtsanpassungen 2026 |
| KRITISCH | 0 | **0** | unveraendert |
| HOCH | 0 | **5** | NEU (Sprachen-Liste, ConsentScreen Dark Pattern, CCPA-Widerspruch, In-App KI-Kennzeichnung, Crisis-Intervention) |
| MITTEL | 0 | **8** | NEU |
| NIEDRIG | - | 4 | NEU |

**Wichtig:** Die HOCH-Befunde sind KEINE akuten Abmahnrisiken zum Zeitpunkt des Erst-Releases, aber einige treten zu Stichtagen in Kraft (AI Act 02.08.2026, § 356a BGB 19.06.2026, UWG EmpCo 27.09.2026). Das heisst: Release moeglich, aber vor diesen Stichtagen nachbessern.

---

## 1. Neue Erkenntnisse seit v3.1 (21.04.2026)

### 1.1 Neue Urteile/Gesetze (seit letzter Recherche 20.04.2026)

| Fundstelle | Datum | Kernsatz | Relevanz BestJournal |
|-----------|-------|---------|---------------------|
| **OLG Frankfurt 6 U 81/23** | 11.12.2025 | Third-Party-SDK-Setzer haften direkt nach § 25 TDDDG, auch wenn sie nur Dritte sind. Schmerzensgeld 100 EUR (reduziert wegen Provokation). | **HOCH**: Firebase Analytics (Opt-In ✓), App Check + Remote Config (berechtigtes Interesse — fraglich ob § 25 TDDDG Abs. 2 "unbedingt erforderlich" greift) |
| **EuGH C-526/24 "Brillen Rottler"** | 19.03.2026 | Auskunftsantrag nach Art. 15 DSGVO kann als rechtsmissbraeuchlich abgelehnt werden. | Entlastend, reduziert DSGVO-Hopping-Risiko |
| **BGH I ZR 96/25 "Hafenmieze"** | 11.03.2026 | Oeffentlich-rechtliche Abgaben = keine Marktverhaltensregeln nach § 3a UWG | Klaert UWG-Systematik, nicht direkt App-relevant |
| **UWG 3. Aenderungsgesetz (EmpCo-RL)** | 19.02.2026 (BGBl), Stichtag **27.09.2026** | Dark Patterns verboten: Hervorheben von Consent-Optionen, Wiederholungs-Anfragen, aufwendige Kuendigungen. § 5 Abs. 6 UWG-neu bereits ab 19.06.2026. | **HOCH**: ConsentScreen — "Accept all" Button visuell dominanter (Gradient+Glow) als "Minimum only"/"Manual Selection" |
| **Google Play Policy Update** | 15.04.2026 (Enforcement 15.05.2026) | Contacts/Location/Foreground-Services Anpassungen; Account Transfer Workflow Pflicht; Health Connect neue Kategorien | BestJournal nicht direkt betroffen (keine Contacts, keine Geofencing) |
| **COPPA Update** | 22.04.2026 (Enforcement) | Voiceprints + Government-IDs = PI; separate Eltern-Einwilligung bei 3rd-Party-Transfer; Info Security Program Pflicht | Niedrig: Whisper macht keine Voiceprint-Erstellung. DSE Section 10 "13+" greift als Safe Harbor |
| **California SB 243** | 01.01.2026 | Companion Chatbot Law — KI muss sich beim ersten Kontakt als KI zu erkennen geben; Crisis-Protokoll bei Suizid-Ideation | **MITTEL**: Grenzfall ob Gemini-Journaling drunter faellt. Vorsichtshalber Disclaimer |
| **Maryland MODPA Enforcement** | 01.04.2026 | Strengstes US-Gesetz; Data Minimization als echte Pflicht; Schwelle 35k Einwohner | Unter Schwelle, OK |
| **Neuseeland IPP 3A** | 01.05.2026 | Neue Informationspflicht bei indirekter Datenerhebung (z.B. Firebase aggregiert Drittdaten) | Bereits in EN DSE 8a.8 adressiert ✅ |
| **Chile LPPD** | Dez 2026 | 2-4% Umsatz-Bussgelder; neue DPA mit Sanktionsbefugnissen | NIEDRIG: Chile aktiver Markt, Vorbereitung bis Dez 2026 |
| **Australia Children's Code** | Konsultation bis 05.06.2026, Registrierung bis 10.12.2026 | Children-Fokus, $50M AUD Bussgelder moeglich | NIEDRIG: noch nicht final |

### 1.2 Strukturelle Befunde aus tieferer Code-/Text-Pruefung

| Fundstelle | Befund |
|-----------|--------|
| DSE Section 3a (DE+EN) | Erwaehnt "Tschechisch" und "Russisch" als unterstuetzte Sprachen — `values-cs/` existiert NICHT, Russland ist ausgeschlossen |
| EN PRIVACY 8a.1 | Widerspruechliche Aussage: "No Do Not Sell link needed" — App HAT aber Do-Not-Sell-Toggle in Settings (seit NH2-Fix) |
| Settings `settings_revoke_title` | Lautet "Widerruf" (DE) / "Withdrawal" (EN) — Implementation via mailto-Intent. § 356a BGB (ab 19.06.2026) verlangt direkten Widerrufsbutton (Klick → Widerruf, nicht E-Mail-Versand) |
| Kein In-App-AI-Disclaimer | Gemini-Output (Dashboard, Retrospective, Text-Improve) hat KEINE "KI-generiert"-Kennzeichnung direkt am Output. Nur in Rechtstext-DSE vorhanden. Art. 50 AI Act ab 02.08.2026 |
| Kein Crisis-Hinweis | Journaling-App ohne Mental-Health-Hotline/Notruf-Hinweis. Nicht gesetzlich Pflicht in DE, aber SB 243 (CA) und branchentypisch |
| DE DSE fehlt Quebec/NZ/Japan/South Africa | EN DSE hat 12 Landes-Abschnitte, DE DSE hat nur 4 (CCPA, US States gesamt, LGPD, PIPEDA, Australian Privacy Principles). CAVE: Quebec ist ausgeschlossen (OK), NZ und JP sind aktive Maerkte |

---

## 2. Befunde nach Schweregrad

### 🔴 KRITISCH (Release blockierend)

**Keine.** Der Auditor hat keine akut release-blockierenden Mangel gefunden.

### 🟠 HOCH (Vor Release fixen — oder zumindest vor jeweiligem Stichtag)

#### H1 — Falsche Sprachen-Liste in DSE Section 3a (DE + EN)

**Fundstelle:** `legal/de/DATENSCHUTZ.html` Zeile 104, `legal/en/PRIVACY.html` Zeilen 165-167
**Problem:** Beide Fassungen erwaehnen "Tschechisch" (cs) und "Russisch" (ru) als unterstuetzte Sprachen.
- `values-cs/` existiert NICHT im Projekt
- Russisch ist im Play-Console ausgeschlossen (RU auf Laender-Ausschluss-Liste)
- Faktisch vorhanden: 27 Sprachen (ar, bn, de, en, es, fr, gu, hi, id, it, ja, kn, ko, ml, mr, nl, pl, pt-BR, pt-PT, ta, te, th, tr, uk, ur, zh-CN, zh-TW)

**Abmahn-Risiko:** Irrefuehrung nach § 5 UWG (Werbung mit nicht verfuegbaren Features). Zwar harmlos im Detail, aber ein Abmahner kann daraus Beispiele ableiten.
**Fix (einfach, 10 Min):** Liste auf tatsaechliche Sprachen korrigieren oder abstrakt "25+ languages" ohne Aufzaehlung schreiben.

#### H2 — ConsentScreen Dark Pattern (UWG EmpCo-RL ab 27.09.2026)

**Fundstelle:** `ConsentScreen.kt` Zeilen 315-411
**Problem:** Drei Buttons sollen "gleichwertig" sein (EDSA 03/2023):
- **"Accept all"**: Copper-Gradient-Fill + breathing Glow-Effekt + Schatten (`elevation 10.dp`)
- **"Minimum only"**: Outline-Button, kein Glow
- **"Manual Selection"**: Outline-Button + Tune-Icon, kein Glow

Alle 3 sind gleich hoch (54dp) und gleich breit (280dp) — Groesse ist identisch. ABER: Die visuelle Dominanz des "Accept all"-Buttons durch Gradient+Glow erfuellt potenziell den UWG EmpCo-Tatbestand "Hervorheben bestimmter Auswahloptionen".

**Abmahn-Risiko:**
- Aktuell: niedrig (UWG EmpCo noch nicht in Kraft)
- Ab 19.06.2026: mittel (§ 5 Abs. 6 UWG-neu greift teilweise)
- Ab 27.09.2026: hoch (volle EmpCo-Umsetzung, aktive Mitbewerber-Abmahnungen)

**Fix (mittel, 1-2 Std):**
- Option A: Alle 3 Buttons visuell IDENTISCH (alle outlined oder alle filled)
- Option B: Nur Groesse und Label unterscheidet, keine Farb/Glow-Hierarchie
- EDSA 03/2023 Guideline: "equally prominent" — gleich prominent, nicht gleich gross

**Stichtag-Empfehlung:** Bis 19.06.2026 umsetzen (mit Puffer vor EmpCo-Pflicht-Datum).

#### H3 — CCPA-Widerspruch in EN PRIVACY 8a.1

**Fundstelle:** `legal/en/PRIVACY.html` Zeilen 567-571
**Problem:** Text sagt:
> "We do **not sell or share** your personal information in the meaning of the CCPA... Because there is no sale or sharing, **there is no "Do Not Sell or Share My Personal Information" link needed**, but you may still opt out of any analytics data processing under Section 5.7."

Das widerspricht der tatsaechlichen App: SettingsScreen HAT einen Do-Not-Sell-Toggle (bei en-US, NH2-Fix vom 21.04.2026).

**Abmahn-Risiko:** Kein direktes CCPA-Strafrisiko (Text ist Opt-In-Erklaerung), aber inkonsistent mit App-Verhalten. Wenn CA-Nutzer den Toggle sieht aber Text ihn verneint, Beschwerde bei CPPA moeglich.
**Fix (einfach, 15 Min):** Text umformulieren:
> "We do not sell or share your personal information in the meaning of the CCPA. As a transparency measure, we nevertheless provide a 'Do Not Sell or Share My Personal Information' toggle in Settings → Privacy, which revokes all optional cloud consents and disables analytics."

#### H4 — Fehlende In-App KI-Kennzeichnung (AI Act Art. 50 ab 02.08.2026)

**Problem:** Die DSE (Section 12a) erwaehnt Art. 50 AI Act, aber KEIN In-App-Disclaimer beim tatsaechlichen KI-Output:
- Dashboard-Zusammenfassung: keine "✨ KI-generiert"-Kennzeichnung
- Retrospective (Woche/Monat): keine Kennzeichnung
- Text-Improve: keine Kennzeichnung (User sieht improvten Text, aber nicht dass er von KI kam)
- Edge-TTS: Chatbot-Pflicht greift nicht (reine Sprachausgabe)

**Abmahn-Risiko:**
- Aktuell: niedrig (Art. 50 ab 02.08.2026)
- Ab 02.08.2026: hoch — Bussgelder bis 15 Mio EUR oder 3% Umsatz

**Fix (mittel, 2-3 Std):**
- Badge "✨ KI-generiert" / "✨ AI-generated" ueber jedem KI-Output (Dashboard, Retrospective, Improved Text)
- Erste Gemini-Aktivierung: einmaliger Dialog "Du nutzt jetzt KI-Funktionen. KI kann Fehler machen."
- PrivacyGateDialog (gemini_body) enthaelt bereits implizite KI-Erwaehnung ✅, aber Art. 50 verlangt explizite Kennzeichnung am OUTPUT, nicht nur am Input

**Stichtag:** 02.08.2026

#### H5 — Fehlender Crisis-Intervention-Hinweis

**Problem:** Als Journaling-App mit KI-Analyse kann BestJournal in Grenzfaellen als "Companion Chatbot" unter California SB 243 fallen (seit 01.01.2026 in Kraft). SB 243 verlangt:
1. Kennzeichnung als KI beim ersten Kontakt
2. Crisis-Protokoll bei Suizid-Ideation
3. Keine "menschliche Beziehung"-Suggestion

Aktuell fehlt:
- Jeder Mental-Health-Disclaimer in der App (nur in ToS Section 4.1)
- Krisennummer (Telefonseelsorge DE: 0800 111 0 111, International: findahelpline.com)
- Notruf-Button/Link in Settings

Zwar keine DE/EU-Pflicht, aber:
- Haftungsrelevant (HWG-Problematik bei psychisch Gefaehrdeten)
- Play Store Health Content Policy (seit August 2025): Health apps declaration form pruefen
- SB 243 Risiko bei US-Nutzern

**Fix (mittel, 1-2 Std):**
- In Settings neuen Eintrag "Krisenhilfe / Crisis Support" mit Nummern DE/EN
- Footer-Hinweis in Retrospective-Screens: "Dies ersetzt keine professionelle Beratung. Notruf: 112 | Telefonseelsorge: 0800 111 0 111"
- In DSE und ToS gesonderter Absatz "Mental Health Disclaimer"

---

### 🟡 MITTEL (Sollten zeitnah gefixt werden)

#### M1 — § 356a BGB Widerrufsbutton (ab 19.06.2026)

**Fundstelle:** `SettingsScreen.kt` Zeilen 2658-2746
**Aktueller Stand:** Zweistufiger Dialog (Klick → AlertDialog → mailto-Intent) — oeffnet E-Mail-Client mit vorgefuelltem Text.

**Problem laut Researcher 1 + 4:** § 356a BGB verlangt einen **Button der den Widerruf direkt ausloest**, nicht einen Button der ein E-Mail-Programm oeffnet. Technisch-rechtliche Diskussion laeuft noch (Google Play Billing vs. BGB-Widerrufsbutton).

**Empfohlene Umsetzung laut Researcher 4:**
- Stufe 1: Button "Vertrag widerrufen" (nicht "Widerruf" oder "Kuendigen")
- Stufe 2: Bestaetigungs-Dialog mit "Widerruf bestaetigen" Button
- Stufe 3: Automatische Eingangsbestaetigung per E-Mail (via Gmail API, die bereits fuer Feedback genutzt wird)
- Ergebnis: Widerruf ist tatsaechlich angekommen, nicht nur eine E-Mail-Vorlage

**Abmahn-Risiko:** Ab 19.06.2026 hoch (UWG-Abmahnung durch Wettbewerber + Verbraucherverbaende)

**Fix (gross, 3-4 Std):**
- Label-Update: "Widerruf" → "Vertrag widerrufen" (DE), "Withdrawal" → "Withdraw contract" (EN)
- Neue Server-less Architektur: Gmail-API sendet die Widerrufs-E-Mail direkt an `dev.app.support@gmail.com` PLUS Bestaetigung an den User (gleicher Mechanismus wie Feedback-Feature)
- AlertDialog: Confirmation-Button "Widerruf bestaetigen" statt "OK"
- Empfangsbestaetigung auf einer Bestaetigungs-Seite in der App

**Stichtag:** 19.06.2026

#### M2 — OLG Frankfurt Cookie-Drittanbieter-Haftung fuer Firebase App Check + Remote Config

**Problem:** OLG Frankfurt 6 U 81/23 (11.12.2025): § 25 TDDDG-Verstoss haftet Third-Party-SDK-Setzer direkt.

Aktueller Stand:
- **Firebase Analytics**: Opt-In via Consent ✅ (erfuellt § 25 TDDDG Abs. 1)
- **Firebase App Check**: "berechtigtes Interesse" (Art. 6 Abs. 1 lit. f) — fragwuerdig, ob "unbedingt erforderlich" (§ 25 TDDDG Abs. 2)
- **Firebase Remote Config**: "berechtigtes Interesse" — noch fragwuerdiger (Feature-Flags sind nicht "unbedingt erforderlich")

**Diskussion:**
- App Check ist fuer Anti-Abuse — **plausibel "unbedingt erforderlich"** (Firebase-Endpoints wuerden sonst von Bots bombardiert)
- Remote Config ist fuer Feature-Flags — NICHT unbedingt erforderlich fuer den Betrieb. Aber: keine personenbezogenen Daten (nur anonymisierte Instance-IDs)

**Fix-Optionen:**
- Option A: Beides bleibt unter "berechtigtem Interesse" — Argumentation dokumentieren (App Check: Sicherheit, Remote Config: nur Instance-ID ohne PII)
- Option B: Remote Config deaktivieren oder unter Consent verschieben
- Option C: DSE-Abschaetzung praeziser formulieren ("Bei App Check: geraeteunabhaengiger Token, keine Rueckverfolgbarkeit auf Person")

**Abmahn-Risiko:** Mittel — OLG Frankfurt hat Schmerzensgeld bei 100-1500 EUR angesiedelt, Massenabmahnung moeglich falls Industrie-Klaeger aktiv werden.

**Empfehlung:** Option A + C — praezise Abwaegung dokumentieren in DSE Sections 5.8 + 5.9.

#### M3 — DSE Kurzfassung in 25 Sprachen: Vollstaendigkeits-Pruefung

**Fundstelle:** `legal/es/|fr/|it/|ja/|ko/|zh-CN/|...` jeweils ~80 Zeilen (~6KB) gegenueber DE 442 Zeilen / EN 936 Zeilen
**Problem:** Die 25 Nicht-DE/EN-Sprachen sind Kurzfassungen mit der Klausel "in case of conflict, the full EN/DE version prevails". Rechtlich zulaessig unter DSGVO Art. 12 Abs. 1 (verstaendliche Sprache), solange die Kernangaben drin sind.

**Pruefung der ES-Version (Stichprobe):** ✅ enthaelt alle Pflichtangaben (Verantwortlicher, Daten-Kategorien, Rechtsgrundlagen, Drittland-Transfer, Speicherdauer, Betroffenenrechte, CCPA/LGPD/UK GDPR-Hinweise).

**Fazit:** OK fuer alle EU-Maerkte (ES, FR, IT, NL, PL, PT-PT, UK). Fuer ausserhalb-EU Maerkte:
- **Japan APPI**: Kurzfassung mit Hinweis auf Volltext EN — grenzwertig. APPI verlangt "in Japan verstaendliche Form". JA-Kurzfassung erfuellt das minimal, aber ein Anwaltsbrief an den PPC koennte zur Nachbesserung fuehren
- **Indien DPDP**: bis 13.05.2027 Uebergangsfrist, OK
- **Thailand PDPA**: English-Text reicht faktisch
- **Mexiko**: Simplified Privacy Notice in ES empfohlen — ES-Kurzfassung erfuellt das ✅

**Empfehlung:** OK fuer Release. Bei rechtlichen Anfragen einzelne Sprachen bei Bedarf ausbauen.

#### M4 — DE DSE fehlt 4 Landes-Abschnitte die EN hat

**Fundstelle:** `legal/de/DATENSCHUTZ.html` hat nur: CCPA, US States (zusammengefasst), LGPD, PIPEDA, Australian Privacy Principles
`legal/en/PRIVACY.html` hat zusaetzlich: Quebec Law 25 (8a.6), Illinois BIPA (8a.3), New Zealand IPP 3A (8a.8), Japan APPI (8a.10), South Africa POPIA (8a.11)

**Problem:** DE-Nutzer die z.B. ein Jahr in NZ leben, haben keinen DE-Hinweis auf NZ-Rechte. Minor-Issue, weil DSGVO-Rechte universal gelten und DE-Nutzer meist in DE wohnen.

**Fix (optional, 30 Min):** 4-5 zusaetzliche Abschnitte in DE DSE 8a ergaenzen — Kurzfassung analog zu EN.

#### M5 — Kein § 327r BGB Aktualisierungspflicht-Hinweis in ToS

**Fundstelle:** `legal/de/NUTZUNGSBEDINGUNGEN.html`, `legal/en/TERMS.html`
**Problem:** Die Terms erwaehnen nicht die gesetzliche Update-Pflicht nach § 327r BGB (gilt seit 01.01.2022 fuer B2C-Vertraege ueber digitale Produkte).

**Fix (einfach, 15 Min):** Neue Sektion 10a oder 5.7 einfuegen mit dem Muster-Text aus Researcher 4 Finding 3.

#### M6 — California SB 243 Companion Chatbot — Grenzfall-Risiko

**Problem:** SB 243 gilt fuer "KI-Systeme die sustained interactions mit sozialen/emotionalen Beduerfnissen haben". Journaling-App mit Gemini-Rueckblicken ist Grenzfall.

**Risiko-Minderung:**
- Bei SB 243 gilt: Crisis-Hinweis-Text empfohlen (H5 deckt das ab)
- AI-Kennzeichnung beim ersten Kontakt (H4 deckt das ab)
- Keine "social relationship"-Suggestion

**Fix:** Durch H4 + H5 abgedeckt. Zusaetzlich: im PrivacyGateDialog-Gemini-Text klarstellen "Gemini ist eine KI, kein Mensch — keine emotionale Beziehung".

#### M7 — Art. 9 DSGVO Sonderkategorien — explizite Einwilligung

**Problem:** Tagebuchtexte koennen Gesundheit, Religion, Sexualleben offenbaren. Art. 9 Abs. 2 lit. a verlangt **explizite** Einwilligung (nicht nur allgemeine).

**Aktueller Stand:**
- DSE erwaehnt Art. 9 DSGVO bei Groq (Voice Recording) und bei Gemini ✅
- PrivacyGateDialog sagt "KI-Funktion aktivieren" mit "Zusammenfassungen, Rueckblicke, Dashboard-Einblicke und Textverbesserungen"

**Schwachstelle:** Die "Explizitaet" fuer Art. 9 Einwilligung ist nicht klar markiert. PrivacyPreferences-Sheet behandelt KI wie andere Features.

**Fix (einfach, 30 Min):** In `privacy_gate_gemini_body` ergaenzen:
> "Da deine Tagebucheintraege besonders sensible Daten enthalten koennen (Gesundheit, Religion, persoenliche Beziehungen — Art. 9 DSGVO), bitten wir hier um deine ausdrueckliche Zustimmung."

#### M8 — Play Console Health apps declaration form pruefen

**Problem:** Seit August 2025: Apps die Gesundheitsdaten verarbeiten (auch inferiert durch KI-Stimmungsanalyse), muessen Health apps declaration form im Play Console ausfuellen.

**Fix (technisch, 15 Min):** Beim Play-Console-Upload pruefen ob das Form auftaucht und korrekt ausfuellen. Dokumentation in `play-store-metadata/`.

---

### 🟢 NIEDRIG (Nice-to-have)

#### N1 — Target SDK Android 16 im Q3 2026

Google erwartet im August 2026 API 36 als neue Pflicht. Aktuell API 35. Bis dahin upgraden.

#### N2 — Chile LPPD Dez 2026

Chile wird aktiver Markt. Vorbereitung bis Dez 2026: ES-Kurzfassung bereits vorhanden ✅, ggf. LPPD-spezifischer Abschnitt ergaenzen.

#### N3 — Australia Children's Code (spaetestens Dez 2026 final)

Falls Code registriert wird: "actual knowledge"-Pruefung fuer Minder-Nutzer, Altersverifikation "reasonable steps".

#### N4 — Mexiko Simplified Privacy Notice

Mexiko-Reform Maerz 2026 verlangt "Simplified Privacy Notice" bei App-Datenerhebung. ES-Kurzfassung erfuellt das ✅ — Play Store-Beschreibung auf Mexiko-Nutzer anpassen.

---

## 3. Sprachen-Matrix (aktiv released)

| Markt | Sprache | Rechtstext | Pflicht-Sprache? | Bewertung |
|-------|---------|------------|------------------|-----------|
| DE, AT, CH | Deutsch | Volltext 442 Zeilen ✅ | DE: Pflicht | OK |
| EU gesamt | 25 Sprachen Kurz + EN Volltext | EN 936 Zeilen ✅ | DSGVO: Landessprache empfohlen | OK (Day-One-Modell) |
| UK | Englisch Volltext | ✅ | Pflicht | OK |
| USA | Englisch Volltext + CCPA/Staaten | ✅ | Pflicht | OK |
| JP | Japanisch Kurzfassung + EN | ⚠ | APPI verlangt JA — Kurzfassung grenzwertig | Akzeptabel |
| Indien | Hindi Kurzfassung + EN | ✅ bis 13.05.2027 | Pflicht ab 13.05.2027 | OK bis 2027 |
| Mexiko | Spanisch Kurzfassung + EN | ✅ | Pflicht (LFPDPPP) | OK |
| Chile | Spanisch Kurzfassung + EN | ✅ ab Dez 2026 | LPPD ab Dez 2026 | OK |
| Australien | Englisch | ✅ | Englisch ausreichend | OK |
| NZ | Englisch | ✅ | Englisch ausreichend | OK |

**Ausgeschlossene Maerkte:** CA, RU, IR, KP, TR, KR, SA, BR, VN, CN, BY (keine Sprachpflicht relevant)

---

## 4. Laender-Matrix (Release-Freigabe)

| Land | DSGVO/Lokal | Rechtstext | Consent-Screen | Freigabe-Status |
|------|-------------|------------|----------------|-----------------|
| DE/AT/CH | ✅ | Volltext DE | ✅ DE | 🟢 FREI |
| EU (24 weitere) | ✅ | Kurzfassung + DE/EN Volltext | ✅ in Lokalsprache | 🟢 FREI |
| UK | ✅ | EN Volltext mit UK-Spezifika | ✅ EN | 🟢 FREI |
| USA | ✅ | EN Volltext mit CCPA/18 States | ✅ EN mit Do-Not-Sell | 🟢 FREI |
| Japan | ⚠ | JA Kurz + EN | ✅ JA UI | 🟡 FREI mit Restrisiko |
| Australien | ✅ | EN mit APPs | ✅ EN | 🟢 FREI |
| Neuseeland | ✅ | EN mit IPP 3A | ✅ EN | 🟢 FREI |
| Indien | ✅ | HI Kurz + EN (bis 2027) | ✅ HI UI | 🟢 FREI |
| Thailand | ✅ | TH Kurz + EN | ✅ TH UI | 🟢 FREI |
| Indonesien | ✅ | IN Kurz + EN | ✅ IN UI | 🟢 FREI |
| Mexiko | ✅ | ES Kurz + EN | ✅ ES UI | 🟢 FREI |
| Chile | ⚠ Dez 2026 | ES + EN | ✅ ES | 🟡 FREI bis Dez 2026 |
| Argentinien | ✅ | ES + EN | ✅ ES | 🟢 FREI |

**Ausgeschlossen (wie bisher):** TR, KR, SA, BR, VN, CN, RU, BY, IR, KP, CA (Kanada wg. Quebec)

---

## 5. Google Play Data Safety Check

**Vorhandene Deklaration:** Status laut v3.1 geprueft, alle 7 Manifest-Permissions in DSE dokumentiert.

**Neue Pruefpunkte 2026:**
| Punkt | Bewertung |
|-------|-----------|
| Data types collected | ✅ deckungsgleich mit DSE Section 2 |
| Third parties | Groq, Google, Microsoft — alle mit DPA (Enterprise) — deklarieren als "Data shared with third party: Yes" |
| Voice recordings | ⚠ WICHTIG: Muss als "Audio: Voice or sound recordings" deklariert sein mit "Optional, encrypted in transit, deleted after processing" |
| Analytics | ✅ "App activity: Events + diagnostic" mit opt-in-Hinweis |
| AI-generated content | Seit 2024 Kategorie vorhanden — deklarieren |
| Health apps declaration | ⚠ M8: pruefen ob auszufuellen |
| Account Transfer (ab 27.05.2026) | Pflicht-Workflow nutzen falls Account verkauft |

---

## 6. TODO-Checkliste (Prioritaet, Reihenfolge)

### Vor Erst-Release (wenn Release vor 19.06.2026)

- [ ] **H1** Sprachen-Liste in DSE Section 3a korrigieren (DE + EN + ggf. Kurzfassungen)
- [ ] **H3** CCPA-Do-Not-Sell-Text in EN PRIVACY 8a.1 umformulieren
- [ ] **H5** Crisis-Intervention: Neuer Settings-Eintrag "Krisenhilfe" + Footer-Hinweis in Retrospective + neuer DSE/ToS-Absatz
- [ ] **M3** Japanisch-Kurzfassung optional erweitern (nur bei JA-Fokus)
- [ ] **M7** `privacy_gate_gemini_body` Art. 9 DSGVO explizit erwaehnen
- [ ] **M8** Health apps declaration form im Play Console pruefen

### Vor 19.06.2026 (§ 356a BGB + § 5 Abs. 6 UWG)

- [ ] **M1** Widerrufsbutton auf § 356a-konform umbauen (Gmail-API-Versand statt mailto)
- [ ] **H2** ConsentScreen Dark-Pattern-Entschaerfung: Alle 3 Buttons visuell identisch

### Vor 02.08.2026 (AI Act Art. 50)

- [ ] **H4** In-App "KI-generiert"-Kennzeichnung bei Dashboard/Retrospective/Text-Improve

### Vor 27.09.2026 (UWG EmpCo)

- [ ] **H2** ConsentScreen — falls nicht bereits bis 19.06.2026 fertig
- [ ] **M6** California SB 243-Disclaimer falls per AI-Tagebuch einsatzrelevant

### Laufend zu beobachten

- [ ] **M2** OLG Frankfurt: Remote Config + App Check Argumentation nachschaerfen
- [ ] **M4** DE DSE um Quebec/NZ/Japan/South Africa/BIPA ergaenzen
- [ ] **M5** § 327r BGB Aktualisierungspflicht in ToS einfuegen
- [ ] **N1** Target SDK 36 bis Q3 2026
- [ ] **N2** Chile LPPD vorbereiten bis Dez 2026
- [ ] **N3** Australia Children's Code nach Finalisierung pruefen

---

## 7. Was sich seit v3.1 NICHT geaendert hat (weiter OK)

- ✅ TDDDG/DDG-Kuerzel (nicht veraltet TTDSG/TMG)
- ✅ DSA-Kontaktstelle vorhanden
- ✅ VSBG-Klausel korrekt
- ✅ Keine ODR-Links (seit 07/2025 abgeschaltet)
- ✅ BGH III ZR 59/24: AGB statisch eingebunden (kein dynamischer Verweis)
- ✅ § 19 UStG Kleinunternehmer
- ✅ c/o-Adresse mit Empfangsvollmacht
- ✅ 7 Manifest-Permissions matchen 1:1 DSE
- ✅ Account Deletion mit ehrlicher Drive-Fehlerbehandlung
- ✅ PrivacyGateDialog Pro-Service-Consent (Groq, Gemini, EdgeTts)
- ✅ Do-Not-Sell-Toggle bei en-US (NH2)
- ✅ Opt-Out-Toast (NH2)
- ✅ setAnalyticsCollectionEnabled verdrahtet
- ✅ Art. 50 AI Act in DSE 12a erwaehnt
- ✅ CCPA/CPRA, UK GDPR, PIPEDA, Australian Privacy Principles in EN DSE abgedeckt
- ✅ IL BIPA, Quebec Law 25, NZ IPP 3A in EN DSE abgedeckt
- ✅ IDO-Aktivlegitimation-Schutz durch LG Wiesbaden 10/2025

---

## Disclaimer (Ende)

Dieser Bericht ist eine technische Pruefhilfe, **keine anwaltliche Beratung**.
Vor Release und vor den genannten Stichtagen (19.06.2026, 02.08.2026, 27.09.2026):
**Fachanwalt fuer IT-Recht konsultieren.**

**Autor:** Claude Code Rechtssicherheits-Skill (5 parallele Researcher)
**Recherche-Datum:** 21.04.2026
**Naechste Pflicht-Pruefung:** 2026-07-21 (+90 Tage)
