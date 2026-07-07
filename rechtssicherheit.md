# rechtssicherheit.md — Wissensbasis fuer Android-App-Rechtsprüfungen

**Letzte Recherche:** 2026-04-28 (v7 — 5 parallele Researcher mit Stand 04/2026, Re-Pruefung der drei DE-Rechtsdokumente, Code-Status der zwei v6-Hochrisiken)
**Naechste Pflicht-Pruefung:** 2026-07-28 (+90 Tage) oder bei Material-Changes
**Status BestJournalAndroid:** **TECHNISCH RELEASE-FAEHIG NACH ANWALTSPRUEFUNG** (0 KRIT + 1 HOCH + 4 MITTEL + 3 NIEDRIG). Die zwei v6-Hochrisiken (NB-v6-1 Bestellbutton + NB-v6-2 Sofortleistungs-Checkbox) sind GEFIXT. § 356a Widerrufsbutton GEFIXT. Verbleibend: KI-Badge EntryDetail (pruefen ob noetig), UK Art. 27, Health Apps Declaration, Korea PIPA, Brasilien ANPD-SCC. Vollbericht: `BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-28-v7.md`

---

## Quick-Reference: Was sich 2024/2025 geändert hat

| Altes Kuerzel | Neues Kuerzel | Seit |
|--------------|---------------|------|
| TTDSG | **TDDDG** (Telekommunikation-Digitale-Dienste-Datenschutz-Gesetz) | 14.05.2024 |
| TMG | **DDG** (Digitale-Dienste-Gesetz) | 14.05.2024 |

**Aktion:** Alle Rechtstexte muessen auf TDDDG/DDG umgestellt sein. Veraltete Verweise sind abmahnbar.

---

## Pflichtangaben-Matrix (Master)

### Datenschutzerklaerung EU/DSGVO (Art. 13 DSGVO)

Alle 14 Pflichtangaben (Abs. 1 + 2):
1. Identitaet und Kontaktdaten des Verantwortlichen
2. Datenschutzbeauftragter (oder Hinweis dass nicht erforderlich)
3. Verarbeitungszwecke + Rechtsgrundlagen (Art. 6 Abs. 1 lit. a-f)
4. Berechtigte Interessen + Interessenabwaegung (bei lit. f)
5. Empfaenger/Kategorien von Empfaengern
6. Drittlandubertragungen + Schutzgarantien (Art. 46 DSGVO)
7. Speicherdauer oder Kriterien
8. Betroffenenrechte (Auskunft, Berichtigung, Loeschung, Einschraenkung)
9. Widerspruchsrecht + Datenportabilitaet
10. Widerrufsrecht bei Einwilligung (Art. 7 Abs. 3)
11. Beschwerderecht bei Aufsichtsbehoerde
12. Pflicht zur Bereitstellung
13. Folgen bei Nichtbereitstellung
14. Automatisierte Entscheidungsfindung (Art. 22)

**BGH 27.03.2025 (I ZR 222/19 + 223/19):** DSGVO-Verstoesse sind UWG-Verstoesse.
Wettbewerber koennen ueber § 8 Abs. 3 Nr. 1 UWG abmahnen. Auch kleine Formfehler.

### Impressum § 5 DDG

| # | Angabe | Details |
|---|--------|---------|
| 1 | Vollstaendige ladungsfaehige Anschrift | Kein Postfach, Strasse + Hausnummer + PLZ + Ort |
| 2 | Name/Firma | Bei juristischen Personen: Rechtsform + Vertretungsberechtigter |
| 3 | E-Mail-Adresse | Pflicht |
| 4 | Schnelle elektronische Kontaktmoeglichkeit | Telefon ODER Kontaktformular ODER Chat/Antwortzeit-Zusage |
| 5 | Registereintragung | Handelsregister + Nummer (falls eingetragen) |
| 6 | Umsatzsteuer-ID | Falls vorhanden, sonst Kleinunternehmer-Hinweis (§ 19 UStG) |
| 7 | Wirtschafts-ID | Falls zugeteilt |
| 8 | Aufsichtsbehoerde | Bei reglementierten Taetigkeiten |
| 9 | Berufsrechtliche Angaben | Bei freien Berufen |

**c/o-Adresse bei Impressumservice:** Rechtssicher, wenn Empfangsvollmacht existiert
(BGH 07.07.2023, OLG Hamm 07.05.2015). Serioese Anbieter: Postflex, anschrift.net,
deinimpressum.com.

### DSA Kontaktstelle (Art. 11 DSA, seit 17.02.2024)

Auch fuer einfache Apps mit User-Content empfohlen:
- Kontaktstelle fuer Behoerden
- Kontaktstelle fuer Nutzer
- Kommunikationssprache angeben
- Kleinstunternehmen (<10 MA, <2 Mio EUR) befreit von Transparenzbericht-Pflicht (Art. 15)

### Widerrufsbelehrung (EU Verbrauchersachen)

- Muster: **Anlage 1 zu Art. 246a § 1 Abs. 2 EGBGB**
- Muster-Widerrufsformular: **Anlage 2**
- Widerrufsfrist: 14 Tage ab Vertragsabschluss
- Erloeschen bei digitalen Inhalten: § 356 Abs. 5 BGB (ausdrueckliche Zustimmung + Kenntnis des Erloeschens)
- **Widerrufsbutton ab 19.06.2026 Pflicht** (§ 356a BGB neu) - zweistufig, sichtbar, dauerhaft zugaenglich
- BGH 07.01.2026: AGB muessen zur Muster-Widerrufsbelehrung passen

### AGB-Recht

- **§ 307 BGB (Generalklausel)**: Unangemessene Benachteiligung unwirksam
- **§ 308 Nr. 5 BGB (Aenderungsklauseln)**: Mind. 6 Wochen Widerspruchsfrist + Hinweis auf Zustimmungsfiktion
- **§ 309 Nr. 7 BGB (Haftung)**: Kein Ausschluss bei Koerper/Gesundheit/Vorsatz/grobe Fahrlaessigkeit
- **BGH III ZR 59/24 (10.07.2025)**: Dynamische AGB-Verweise ohne Versionsangabe unwirksam. AGB muessen fixiert oder statisch mit Version eingebunden sein.

### TDDDG § 25 (Consent fuer Endgeraete-Speicherzugriff)

Einwilligungspflichtig:
- Android Advertising ID (AAID)
- Firebase Installation IDs (FIDs)
- Firebase Analytics App Instance IDs
- Persistente Gerateidentifikatoren

Nicht einwilligungspflichtig (technisch notwendig, § 25 Abs. 2):
- Session-IDs fuer Grundfunktion
- Sicherheitstoken ohne Tracking-Funktion

**BGH Planet49 (I ZR 7/16):** Vorausgewaehlt Ankreuzfeld = keine Einwilligung.
Aktive Opt-In Pflicht.

---

## Sprachmatrix (Landessprache-Pflicht bei Rechtstexten)

| Markt | Pflichtsprache | Reicht DSGVO-DE? | Quelle |
|-------|----------------|------------------|--------|
| DE, AT, CH | Deutsch | Ja, Basis | nationales Recht |
| EU gesamt | DSGVO-Landessprache empfohlen, Englisch meist OK | Ja | DSGVO |
| UK | Englisch | Ja, mit ICO-Verweis | UK GDPR |
| USA | Englisch | Ja, mit CCPA/State-Laws-Anhang | CCPA |
| Brasilien | **Portugiesisch (Pflicht)** | Nein | LGPD |
| Suedkorea | **Koreanisch (Pflicht)** | Nein | PIPA (DeepSeek-Urteil) |
| Tuerkei | **Tuerkisch (faktisch Pflicht)** | Nein + VERBIS | KVKK Art. 10 |
| Saudi-Arabien | **Arabisch (faktisch Pflicht)** | Nein + SDAIA | PDPL |
| Mexiko | **Spanisch (Aviso de Privacidad Pflicht)** | Nein | LFPDPPP |
| UAE | Arabisch (faktisch erwartet) | Weitgehend | Federal Decree-Law 45/2021 |
| Japan | Japanisch (faktisch erwartet) | Nein (eigene APPI-Logik) | APPI |
| Kanada | Englisch (Quebec: Franzoesisch!) | Ja + Quebec-Sonderheiten | PIPEDA/Law 25 |
| Australien | Englisch | Ja | Privacy Act 1988 |
| Indien | Englisch reicht (Hindi optional) | Uebergangsfrist bis 13.05.2027 | DPDP Act |

**Fazit Solo-Entwickler:** Bei weltweitem Rollout sind mindestens Deutsch + Englisch
faktisch Pflicht. Portugiesisch, Koreanisch, Tuerkisch, Arabisch, Spanisch sind harte
Sprachanforderungen in den jeweiligen Laendern, was in der Praxis oft durch Lander-
Ausschluss oder bewusste Akzeptanz des Restrisikos adressiert wird.

---

## Google Play Store Pflichten 2026

1. **Data Safety Form** — vollstaendig, konsistent mit DSE
2. **Privacy Policy URL** — oeffentlich, kein Login, ohne Fehler, App-spezifisch
3. **Account Deletion** — In-App + Web-Link (beide Pflicht seit Mai 2024)
4. **Target SDK 35 (Android 15)** — Pflicht fuer neue Apps und Updates seit August 2025
5. **Developer-Verifikation** — Personalausweis/DUNS seit Sept. 2023
6. **Permissions** — besondere Begruendung fuer CAMERA, RECORD_AUDIO, LOCATION, MEDIA
7. **AI-Generated Content Policy** (2024+) — Moderation + Report-Button falls KI-generierter Output fuer Nutzer

---

## Aktuelle Abmahn-Hotspots (Stand April 2026)

| Thema | Abmahnrisiko | Details |
|-------|--------------|---------|
| Veraltete Gesetzes-Kuerzel (TTDSG, TMG) | **Mittel** | Leicht zu beheben, aber klare Formulierung erwartet |
| Fehlerhafte Cookie-Banner/Consent | **Sehr hoch** | § 25 TDDDG, seit BGH 03/2025 Mitbewerber-abmahnbar |
| Unvollstaendige Art. 13 DSGVO | **Hoch** | Kleine Formfehler genuegen |
| Fehlendes Impressum / fehlende Zweit-Kontaktmoeglichkeit | **Mittel** | § 5 DDG Art. 1 Nr. 2 |
| Dynamische AGB-Verweise (BGH 07/2025) | **Neu, mittel** | Versionsfixierung Pflicht |
| KI-Kennzeichnung (AI Act Art. 50) | **Steigend, ab 08/2026** | Erste Abmahnwellen Herbst 2026 erwartet |
| Widerrufsbutton (ab 06/2026) | **Neu, aufkommend** | § 356a BGB |
| Fehlerhafte Widerrufsbelehrung bei Abos | **Hoch** | Klassisches Abmahnthema |

---

## CCPA/Internationale Risiken fuer Solo-Entwickler

| Gesetz | Schwellen | Risiko Solo-Entwickler |
|--------|-----------|------------------------|
| CCPA/CPRA (CA) | $26.6M Umsatz / 100K CA-Nutzer / 50% Umsatz aus Datenverkauf | **Sehr niedrig** (Schwellen nicht erreicht) |
| Texas TDPSA | **Keine Schwelle** | **Mittel** - gilt bei jedem texanischen Nutzer |
| Oregon OCPA | 100K OR-Nutzer | Niedrig |
| UK GDPR | Alle UK-Nutzer | Niedrig (ICO-Beschwerde moeglich) |
| Quebec Law 25 | Alle Quebec-Nutzer | Niedrig-Mittel (25 Mio CAD Strafe) |
| PIPEDA (CA Bund) | Kommerzielle Aktivitaet | Niedrig |
| Australian Privacy Act | Small Business Exemption bis 07/2026 | Steigend |
| LGPD (BR) | Alle BR-Nutzer | Niedrig, Enforcement selten |
| PIPA (KR) | Alle KR-Nutzer | Mittel (Koreanisch + Domestic Rep) |
| APPI (JP) | Alle JP-Nutzer | Niedrig-Mittel |

**Fuer deutsche Solo-Entwickler:** Hauptrisiko bleibt DSGVO + UWG-Abmahnung durch
deutsche Wettbewerber. Internationale Enforcement-Aktionen gegen Solo-Entwickler ohne
Praesenz im Zielland sind praktisch nahezu null.

---

## Geprufte Apps-Log

| Datum | App | Gesamtstatus | Kritisch | Hoch | Befunde |
|-------|-----|-------------|----------|------|---------|
| 2026-04-20 | BestJournalAndroid | Release-faehig nach Fixes | 0 | 1 | TTDSG zu TDDDG (behoben), Zweit-Kontakt (behoben), DSA-Kontaktstelle (behoben) |
| 2026-04-20 | BestJournalAndroid (Tiefpruefung) | **NICHT release-faehig** | **5** | **6** | K1 ODR-Links (aktive Abmahnwelle), K2 LegalDocumentScreen laedt falschen Pfad, K3 PRIVACY.en fehlt, K4 Consent nicht granular, K5 Laenderspezifische Rights-Sections. Vollbericht: `BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20.md` |
| 2026-04-20 | BestJournalAndroid (v2 nach Fixes) | **BEDINGT release-faehig** | **1** | **2** | K1-K5 + H6 + M1 + M2 gefixt. Rest: NK1 Gemini+Edge-TTS Gate fehlt, NH1 Sprachdeckung (TR/KR/SA/BR/VN/CN ausschliessen), NH2 CCPA-2026 Opt-Out-Bestaetigung. Vollbericht: `BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20-v2.md` |
| 2026-04-21 | BestJournalAndroid (v3 nach Consent-v4-Umbau) | **BEDINGT release-faehig** | **1** | **0** | NK1/NH1/NH2 alle gefixt. NEU: KRIT-1 Crashlytics-Attrappe (Toggle existiert, SDK nicht) + falsche Erwaehnung in account-deletion.html:94. MITTEL-1: Drive-Backup-Toggle wird im Manager nicht geprueft. Gesamt 1 KRIT-Fix (~10-15 Min), dann release-faehig. Vollbericht: `BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v3.md` |
| 2026-04-21 | BestJournalAndroid (v3.1 Deep Audit 5 Laeufe) | **RELEASE-FAEHIG** | **0** | **0** | 5 iterative Laeufe durchgefuehrt. Neu gefunden: OCR-Attrappe in DSE 12a (DE+EN+HTML-Assets — innerer Widerspruch zu 5.6 "nie Fotos/Audios" an Gemini). Gefixt in 4 Dateien (MD+HTML). 13 tote Strings nach v4-Umbau entfernt (erspart 325 unnoetige Uebersetzungen in 25 Locales). Lauf 3+5 beide 0 Befunde → Abbruchkriterium erreicht. DE/EN DSE-Struktur identisch (18/18 Top-Level + 11/11 §5-Subsections). Alle 7 Manifest-Permissions in DSE dokumentiert. Alle Toggles → SDK-Steuerung verifiziert (4 setAnalyticsCollectionEnabled + 16 Gemini-Gate-Stellen). |
| 2026-04-22 | BestJournalAndroid (Groq-DPF-Fix) | **unveraendert** | **0** | **-1** | **Externer Cross-Check:** Eine zweite KI fand dass `consent_card2_body` und `privacy_gate_groq_body` falschlich DPF-Deckung fuer Groq behaupten. Verifiziert gegen Groqs eigenes DPA (`console.groq.com/docs/legal/customer-data-processing-addendum`): Groq ist **NICHT DPF-zertifiziert** — nutzt ausschliesslich EU SCCs Module 2/3, UK IDTA und Swiss FADP-SCCs. Fix: Beide Strings in allen 27 `strings.xml`-Dateien angepasst — DPF auf Gemini+Edge beschraenkt, Groq auf eigenen Satz mit "Standard Contractual Clauses (EU SCCs)" gesetzt. Verhindert Art. 5(1)(a) + Art. 13(1)(f) DSGVO-Verletzung. Python-Batch: `BestJournalAndroid/fix_groq_dpf_claim.py`. |
| 2026-04-21 | BestJournalAndroid (v4 5 parallele Researcher) | **BEDINGT RELEASE-FAEHIG** | **0** | **5** | Umfassende Recherche mit 5 parallelen Agenten. 5 HOCH-Funde: H1 DSE Section 3a erwaehnt nicht-existierende Sprachen (Tschechisch/Russisch), H2 ConsentScreen Dark Pattern (Accept-All Gradient+Glow dominant — UWG EmpCo ab 27.09.2026), H3 CCPA-Widerspruch in EN PRIVACY 8a.1 (sagt "no Do-Not-Sell link needed" obwohl Toggle existiert), H4 In-App KI-Kennzeichnung fehlt (AI Act Art. 50 ab 02.08.2026), H5 Crisis-Intervention-Hinweis fehlt (SB 243). 8 MITTEL-Funde: M1 § 356a Widerrufsbutton ab 19.06.2026 (mailto-Implementation nicht konform), M2 OLG Frankfurt 12/2025 Firebase App Check/Remote Config Haftung, M3 Japanisch-Kurzfassung grenzwertig, M4 DE-DSE fehlt Quebec/NZ/JP/ZA/BIPA, M5 § 327r BGB fehlt in ToS, M6 SB 243 Companion Chatbot Grenzfall, M7 Art. 9 DSGVO explizite Einwilligung, M8 Play Console Health declaration pruefen. Vollbericht: `BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v4.md` |
| 2026-04-22 | BestJournalAndroid (gezielter Konto-/Loesch-Fix) | **TEILFIX UMGESETZT** | **0** | **0** | Firebase-Auth-Attrappe aus Code und Gradle entfernt: keine echte Firebase-Anmeldung vorhanden, nur Google-Sign-In + lokales Profil fuer Drive-Backup. Delete-Titel in allen App-Sprachen auf App-Daten/Backup praezisiert. Audit-Notiz: behebt Terminologie- und Code-Widerspruch, ersetzt aber keinen Voll-Rechtsaudit; die v5-Web-/Policy-Baustellen bleiben separat offen. |
| 2026-04-28 | BestJournalAndroid (v6 — heute Vormittag) | **BEDINGT TECHNISCH OK** | **0** | **2** | NB-v6-1 Bestellbutton § 312j BGB, NB-v6-2 Sofortleistungs-Checkbox § 356 Abs. 5 BGB. v5-NB1 (Groq DPF) + v5-NB8 (Pre-Usage-Gate) gefixt. Vollbericht: `RECHTSSICHERHEIT-AUDIT-2026-04-28-v6.md` |
| 2026-04-28 | **BestJournalAndroid (v7 — Pre-Release-Audit)** | **TECHNISCH RELEASE-FAEHIG NACH ANWALTSPRUEFUNG** | **0** | **1** | Re-Audit nach Benutzer-Anfrage "abmahnungssicher fuer Hochladen". 5 parallele Researcher Stand 04/2026 + Re-Pruefung der 3 DE-Rechtsdokumente + Code-Status. NB-v6-1 + NB-v6-2 + § 356a Widerrufsbutton sind GEFIXT (Paywall hat "Jetzt zahlungspflichtig abonnieren" als finalen Klickbutton + Sofortleistungs-Verzicht-Checkbox + Gmail-API-Versand). Verbleibend: v7-H1 KI-Badge im EntryDetailScreen (1-2 Std oder 5 Min wenn nicht noetig), v7-M1 UK Art. 27 Vertreter, v7-M2 Health Apps Declaration (Submit), v7-M3 Korea PIPA, v7-M4 Brasilien ANPD-SCC. Vollbericht: `RECHTSSICHERHEIT-AUDIT-2026-04-28-v7.md` |
| 2026-05-27 | **BestJournalAndroid (finale audit-only — Verifikation v0.21.9)** | **KEINE Code-BLOCKER, 3 HOCH offen** | **0** | **3** | finale-Plugin audit-only, Verifikation gegen aktuellen Code (NICHT nur alte Reports). 5 parallele Worker (3 crashten via FIN-048-Kontextueberlauf → Orchestrator selbst uebernommen). **BESTAETIGT KORREKT (FIN-050):** DSGVO-Verweis-Architektur — 22 Kurz-PRIVACY.html verweisen per href auf DE+EN-Vollversion, alle 9 Online-URLs leben (200), DE/EN/KO-Vollversionen komplett (Art.9/Standort/Churn). 4-vs-5-Perspektiven geloest, §356a-Widerrufsbutton + §356-Checkbox + zahlungspflichtig-Buttons + Permission-Rationales + HWG-Disclaimer + AI-Act-Art.50-Kennzeichnung + Churn-Dark-Pattern-Fix alle vorhanden. Schwere UWG/HWG-Uebertreibungen (verborgene Muster etc.) entschaerft. **3 HOCH offen:** Z1 values-ru enthaelt Ukrainisch (alle 1106 Strings), E1 verwaiste ACCESS_COARSE_LOCATION, G1 Account-Loesch-Web-URL fehlt. MITTEL: Crashlytics-Inkonsistenz, sekundaere §312j-CTAs, AI-Prompt-Leak 7 Sprachen. NIEDRIG: milde Werbe-Claims, Gemini-Marke, R8 aus. Detail: `.android-shield/verify-summary-2026-05-27.json` |

## Neue Hotspots-Erkenntnisse 2026-04-21 (v4-Recherche)

### OLG Frankfurt 6 U 81/23 (11.12.2025) — Cookie-Drittanbieter-Haftung

Third-Party-SDK-Setzer haften direkt nach § 25 TDDDG, auch wenn sie nur Dritte sind.
Schmerzensgeld 100 EUR (reduziert wegen Provokation des Klaegers, ausgehend von 1.500 EUR).
**Praxis-Folge:** Firebase/Google Analytics/Remote Config als Third-Party-SDK-Setzer
haften direkt. Opt-In Consent ist die Pflicht-Antwort. "Berechtigtes Interesse" nach
Art. 6 Abs. 1 lit. f reicht NICHT fuer § 25 TDDDG Abs. 1 — nur "unbedingt erforderlich"
nach Abs. 2 greift (App Check plausibel, Remote Config fragwuerdig).
Quelle: kpw.law, piltz.legal.

### EuGH C-526/24 "Brillen Rottler" (19.03.2026)

Auskunftsantrag nach Art. 15 DSGVO kann als rechtsmissbraeuchlich abgelehnt werden
wenn er erkennbar Schadensverschaffung bezweckt (z.B. DSGVO-Hopping). Entlastet Solo-
Entwickler gegen Industrie-Klaeger.
Quelle: Curia, KPMG-Law.

### UWG 3. Aenderungsgesetz (EmpCo-RL) — Stichtag 27.09.2026

BGBl 19.02.2026. Dark Patterns konkret verboten:
- Hervorheben bestimmter Auswahloptionen (farbig markierte Consent-Buttons)
- Wiederholungsanfragen nach bereits getroffenen Entscheidungen
- Unverhaelinismaessig aufwendige Kuendigungsprozesse
§ 5 Abs. 6 UWG-neu bereits ab 19.06.2026 teilweise in Kraft.
**Praxis-Folge:** Consent-Dialoge muessen gleich-prominente Buttons haben.
Gradient/Glow/Elevation auf "Accept all" = Abmahn-Risiko ab 19.06.2026.
Quelle: Wettbewerbszentrale, Shopbetreiber-Blog.

### COPPA-Update — Enforcement 22.04.2026

Voiceprints + Government-ID-Nummern sind jetzt "personal information".
Separate Eltern-Einwilligung bei Drittanbieter-Datenweitergabe Pflicht.
Formales Information Security Program Pflicht.
**Praxis-Folge fuer Journaling-Apps:** Whisper (Groq) ohne Speaker-Diarization = kein
Voiceprint = kein COPPA-Trigger. ABER: "This app is not intended for children under 13"
sollte deutlich in App-Store-Beschreibung stehen (Mixed-Audience Safe Harbor).
Quelle: Toy Association, FTC.

### Maryland MODPA — Enforcement 01.04.2026

Strengstes US-Datenschutzgesetz: Data Minimization als echte Pflicht.
Schwelle: 35.000 Einwohner/Jahr. Unter Schwelle fuer Solo-Entwickler.

### Universal Opt-Out (GPC) — neue Staaten 01.01.2026

Connecticut, Oregon honor GPC zusaetzlich zu bestehender Liste (CA, CO, DE, MD, MN, MT, NJ, NH, TX).
**Praxis:** Native Apps senden keine GPC-Header — EN DSE sollte klarstellen dass GPC
nur bei Web-Interaktionen relevant ist.

### Texas Responsible AI Governance Act (HB 149, 01.01.2026)

Wendet TDPSA auf KI-Datenverarbeitung an. Begrenzte biometrische Ausnahmen fuer
KI-Modelltraining. TDPSA keine Schwelle = gilt bei jedem TX-Nutzer.

### Chile LPPD — Dez 2026

Neue DPA mit Sanktionsbefugnissen. Bussgelder 2-4% Jahresumsatz. Nationales
Infraktions-Register. Vorbereitung fuer Solo-Entwickler empfohlen.

### Neuseeland IPP 3A — Seit 01.05.2026

Neue Informationspflicht bei indirekter Datenerhebung von Dritten/oeffentlichen Quellen.
**Praxis-Folge:** Wenn Firebase/Analytics Drittdaten aggregiert, Benachrichtigung der
betroffenen Person. Ausnahme wenn Benachteiligung unwahrscheinlich.

### Australia Children's Online Privacy Code

OAIC Exposure Draft April 2026. Konsultation bis 05.06.2026. Registrierung bis
10.12.2026. Gilt fuer "online services likely accessed by children". Bussgelder bis
$50 Mio AUD oder 30% Umsatz.

### Mexiko LFPDPPP-Reform Maerz 2026

Neue Behoerde SABG ersetzt INAI. Pflicht "Simplified Privacy Notice" bei App-Datenerhebung.

### California SB 243 — Companion Chatbot Law (01.01.2026)

KI mit "sustained interactions" zu sozialen/emotionalen Beduerfnissen muss:
1. beim ersten Kontakt als KI gekennzeichnet sein
2. Crisis-Protokoll bei Suizid-Ideation
3. keine "menschliche Beziehung"-Suggestion
**Praxis:** Journaling-App mit Gemini-Rueckblicken ist Grenzfall. Vorsichtshalber
Crisis-Hinweis + klare KI-Kennzeichnung. Auch ohne SB 243 haftungs- und rufrelevant.

### § 356a BGB Widerrufsbutton — Stichtag 19.06.2026

Pflicht-Label Stufe 1: **"Vertrag widerrufen"** (NICHT "Widerruf", "Stornieren", "Kuendigen")
Pflicht-Label Stufe 2: **"Widerruf bestaetigen"**
Pflicht: automatische Eingangsbestaetigung per E-Mail
**Praxis-Folge:** mailto-Intent reicht wahrscheinlich NICHT — Widerruf muss direkt
ausgeloest werden. Fuer App-Entwickler: Gmail-API-Versand aus der App heraus ist
pragmatische Loesung (App hat bereits Gmail-API fuer Feedback). Rechtssicherheit noch
nicht gerichtlich bestaetigt.
Quelle: Noerr, IT-Recht Kanzlei, Datenschutz-Generator, TelemetryDeck.

### Google Play Policy Update 15.04.2026 (Enforcement 15.05.2026)

- Contacts Permissions: Android Contact Picker Pflicht bei Teil-Zugriff
- Location: Location Button empfohlen, Geofencing NICHT mehr als Foreground-Service
- Account Transfer: Nur offizieller Play Console Workflow (ab 27.05.2026)
- Health Connect: neue Kategorien Menstruationszyklus/Alkoholkonsum/Symptome
- News/Magazine Self-Declaration Pflicht (ab 27.05.2026)
Fuer Journaling-App meist nicht direkt relevant.

### Google Play Health Content Policy (seit August 2025)

Health apps declaration form im Play Console Pflicht wenn App Gesundheitsdaten
verarbeitet — auch INFERIERT durch KI-Stimmungsanalyse. Bei Journaling-Apps mit
KI-Analyse: Form ausfuellen empfohlen.

### Flo-Health-Praezedenz ($56M Urteil 2025)

Meta jury verdict fuer Gesichtserkennung/Biometrik bei Flo-Period-Tracker.
Warnsignal fuer jede App mit (auch inferierten) Gesundheitsdaten.

---

## Neue Hotspots-Erkenntnisse 2026-04-20

### ODR-Plattform seit 20.07.2025 abgeschaltet — AKTIVE Abmahnwelle seit Q3/2025

Alle Rechtsdokumente MUESSEN den Link zu `ec.europa.eu/consumers/odr/` entfernt haben.
Toter Link = Irrefuehrung § 5 UWG. Quelle: WBS.legal, IT-Recht Kanzlei.
VSBG-Klausel ("weder verpflichtet noch bereit") bleibt.

### BIPA Illinois + Voice Recording

Voice Recording mit Stimmprofil-Erstellung (Speaker-Diarization) = biometrische Daten
nach BIPA. Groq Whisper Standard-Transkription erstellt keinen Voiceprint = kein
BIPA-Trigger, ABER Retention-Klausel in englischer DSE empfohlen.

### ANPD-SCCs Brasilien seit 23.08.2025 Pflicht

EU-SCCs reichen NICHT fuer Brasilien. Separate ANPD-SCC-Form erforderlich plus
portugiesische DSE auf oeffentlicher Website. Empfehlung Solo-Entwickler: BR
ausschliessen.

### Tuerkei VERBIS-Pflicht bei extraterritorialem Controller

Selbst fuer Solo-Entwickler ohne Sitz in TR: VERBIS-Registrierung Pflicht wenn
tuerkische Nutzer bedient werden. Strafen bis 17 Mio. TRY. Empfehlung: TR
ausschliessen.

### Suedkorea DeepSeek-Praezedenzfall (02/2025)

PIPC hat fehlende koreanische DSE explizit als Grund fuer Corrective Order genannt.
Empfaenger muessen namentlich gelistet sein (nicht nur "Cloud-Anbieter in USA").

### Play Console Account-Deletion Web-URL (seit 31.05.2024)

Zusaetzlich zu In-App-Loeschung MUSS eine oeffentliche Web-URL im Data-Safety-
Formular angegeben werden. Reicht: github.io-Seite mit Mailto-Formular.

### Play Console Material Information (Oktober 2025)

Neue Clarifications: alle wesentlichen Abo-Konditionen (Preis, Laufzeit, auto-
Verlaengerung) muessen VOR dem Kauf sichtbar sein. Play Billing haelt sich daran
automatisch, aber App-eigene Premium-Screens pruefen.

---

## Neue Erkenntnisse v2-Recherche (2026-04-20)

### CCPA 2026-Novelle seit 01.01.2026 — Opt-Out-Bestaetigungspflicht

Nach einem Opt-Out muss die App eine Bestaetigung zeigen ("Opt-Out Request Honored").
Privacy Policy MUSS aus den App-Settings verlinkt sein. GPC-Signale muessen anerkannt werden.
Quelle: consentmo.com/ccpa-2026-update-opt-out-confirmation-mandatory, gtlaw.com 2026.

### California SB 243 Companion Chatbot Law seit 01.01.2026

KI mit natuerlicher Sprachschnittstelle die soziale/emotionale Beduerfnisse adressiert muss
beim ersten Kontakt als KI gekennzeichnet sein. Ein KI-Tagebuchassistent fallt moeglicherweise
darunter — zusaetzlich zu Art. 50 AI Act (EU) separaten US-Disclaimer einbauen.
Quelle: troutmanprivacy.com SB 243 (01/2026).

### Quebec Loi 96 Franzoesisch-Pflicht fuer Apps seit 06/2025

Loi 96 (Charter of the French Language Reform) ist seit Juni 2025 voll in Kraft. Websites und
mobile Apps die Quebec-Nutzer ansprechen MUESSEN auf Franzoesisch verfuegbar sein. Die
franzoesische Version muss in Qualitaet und Zugaenglichkeit gleichwertig zur englischen sein.
**Praktische Konsequenz:** Quebec separat ausschliessen oder vollstaendige fr-rCA-Version bereitstellen.
Quelle: DLA Piper 2025-06.

### Vietnam PDPL 2026 seit 01.01.2026 in Kraft

Das neue Personal Data Protection Law (verabschiedet 26.06.2025) ersetzt Decree 13/2023. Neu:
- Extraterritoriale Anwendung
- Data Localization fuer "wichtige" personenbezogene Daten (Kategorienliste in Folge-Verordnungen)
- Cross-Border-Transfers mit expliziter Einwilligung + Behoerdenmeldung
- Strafen: bis 5% Jahresumsatz fuer unzulaessige Cross-Border-Transfers
Quelle: EY Vietnam 2025-07, Mori Hamada.

### UK DUAA 2025 — Royal Assent 06/2025

Data (Use and Access) Act 2025 aendert Teile von UK-GDPR und ePrivacy. Flexiblere DPIA,
keine DPO-Pflicht fuer alle Unternehmen. ICO erwartet "just-in-time"-Datenschutzinformationen
bei App-Datenerfassung. TCF v2.3 + Google Consent Mode v2 sind 2026-Standards.
Quelle: Secure Privacy 2026, ICO 2024.

### Korea PIPA 10% Umsatz-Bussgelder ab 11.09.2026 + CEO-Haftung ab 03/2026

Neue Schwelle: bis zu 10% des Gesamtumsatzes bei vorsaetzlichen/grobfahrlaessigen
Wiederholungsverloetzungen oder 10+ Mio. Betroffenen. Praezedenzfaelle 2024-2025:
AliExpress (englischsprachige Loeschseiten), KakaoPay+Apple, Temu, Golfzon (5,47 Mio USD).
Quelle: Hunton Privacy Blog, Loeb Koreas PIPA Dec 2025.

### IDO Verband Aktivlegitimation entzogen — LG Wiesbaden 10/2025

IDO ist nicht mehr in der qualifizierten Verbandsliste nach § 8 Abs. 3 Nr. 2 UWG. Abmahnungen
abwehrbar, bestehende Unterlassungserklaerungen kuendbar. Entlastung fuer Solo-Entwickler.
Quelle: cornea-franz.de, wbs.legal, exali.de.

### BGH Google Fonts EuGH-Vorlage 08/2025

BGH VI ZR 258/24 legt EuGH drei Kernfragen vor (IP-Adressen als PII, Rechtsmissbrauch bei
provoziertem Verstoss, Schadensersatz). Massenabmahnungen 2025/2026 derzeit ausgesetzt bis
EuGH-Entscheidung. Altfaelle aus 2022-Welle verjaehrten 31.12.2025.
Quelle: IT-Recht Kanzlei, dr-bahr.com.

### Meta-Tracking-Schadensersatz 3000-5000 EUR

LG Luebeck 27.11.2025: 5.000 EUR pro Fall. LG Jena 02.03.2026: 3.000 EUR pro Klaeger.
BestJournal betrifft das nicht (kein Meta-SDK eingebunden). Aber Warnung fuer andere Apps.
Quelle: sbs-legal.de, mueller.legal, ra-plutte.de.

### BGH Facebook Scraping VI ZR 10/24 (18.11.2024)

Richtwert 100 EUR pro Fall fuer immateriellen Schaden schon bei blossem kurzzeitigem
Datenkontrollverlust — kein Nachweis von konkretem Schaden noetig.
Quelle: BRAK-Pressemitteilung.

### Google Play Data Safety Service Provider vs. Third Party

Ein Dritter gilt nur dann als Service Provider (kein "shared"), wenn DPA/DPIA existiert die
Training auf Nutzerdaten verbietet. Ohne DPA = shared = Pflichtangabe. OpenAI/Anthropic haben
Enterprise-DPAs verfuegbar.

### California SB 942 AI Transparency Act — verzoegert bis 02.08.2026

Gilt nur fuer "Covered Provider" > 1 Mio monatliche CA-Nutzer. Pflicht: kostenloses
Content-Detection-Tool + Wasserzeichen-Option. Fuer Solo-Entwickler irrelevant bis zu diesem Scale.

### Australien Privacy Act 2024-Reform + Statutory Privacy Tort seit 06/2025

Klagbares Recht fuer Einzelpersonen bei Privacy-Verletzungen. MFA + Verschluesselung verstaerkt
unter APP 11. Children's Online Privacy Code bis 12/2026 erwartet. Bussgelder bis 50 Mio AUD
oder 30% des Umsatzes im Tatzeitraum.
Quelle: Norton Rose Fulbright 2024, Recording Law 2026.

---

## Quellen-Register (Stand April 2026)

| URL | Thema | Abrufdatum |
|-----|-------|------------|
| https://dsgvo-gesetz.de/art-13-dsgvo/ | Art. 13 DSGVO | 2026-04-20 |
| https://www.gesetze-im-internet.de/ddg/__5.html | § 5 DDG Impressum | 2026-04-20 |
| https://www.it-recht-kanzlei.de/tmg-ttdsg-ausser-kraft-impressum-datenschutz.html | DDG ersetzt TMG | 2026-04-20 |
| https://www.bundesgerichtshof.de/SharedDocs/Pressemitteilungen/DE/2025/2025059.html | BGH DSGVO abmahnbar 03/2025 | 2026-04-20 |
| https://www.anwalt.de/rechtstipps/verweis-auf-online-agb-laut-bgh-unzulaessig-bgh-urteil-vom-10-07-2025-iii-zr-59-24-257243.html | BGH dynamische AGB 07/2025 | 2026-04-20 |
| https://www.btl-recht.de/blog/pflicht-zum-widerrufsbutton-ab-2026/ | § 356a BGB Widerrufsbutton | 2026-04-20 |
| https://ki-kanzlei.de/kennzeichnungspflichten-fuer-ki-inhalte | AI Act Art. 50 | 2026-04-20 |
| https://ico.org.uk | UK ICO | 2026-04-20 |
| https://oag.ca.gov/privacy/ccpa | CCPA/CPRA | 2026-04-20 |
| https://support.google.com/googleplay/android-developer/answer/10787469 | Google Play Data Safety | 2026-04-20 |

---

## Muster-Klauseln (Sammlung)

### Drittlandubermittlung USA (EU-US DPF + SCC)

> Verarbeitung erfolgt in den USA auf Grundlage des EU-US Data Privacy Framework
> (Angemessenheitsbeschluss der EU-Kommission vom 10. Juli 2023) sowie von
> EU-Standardvertragsklauseln (Art. 46 DSGVO).

### Interessenabwaegung bei lit. f

> Das berechtigte Interesse an [Zweck] ueberwiegt die minimale Beeintraechtigung
> des Nutzers, da [kurze Begruendung]. Eine datenschutzschonendere Alternative
> ist technisch nicht verfuegbar.

### Zweit-Kontaktmoeglichkeit nach § 5 Abs. 1 Nr. 2 DDG

> Schnelle elektronische Kontaktaufnahme: Zusaetzlich zur E-Mail bemuehen wir uns,
> Anfragen innerhalb von 24 Stunden an Werktagen zu beantworten.

### DSA-Kontaktstelle Art. 11

> Fuer Behoerden und Nutzer-Anfragen nach dem Digital Services Act ist folgende
> Kontaktstelle erreichbar: [E-Mail]. Kommunikationssprache: Deutsch und Englisch.

---

**Disclaimer:** Diese Wissensbasis ist keine anwaltliche Beratung. Sie sammelt oeffentlich
verfuegbare Informationen. Vor dem Release: Fachanwalt konsultieren.
