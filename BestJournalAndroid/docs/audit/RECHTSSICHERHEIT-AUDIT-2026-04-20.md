# Rechtssicherheits-Tiefpruefung: BestJournalAndroid

**Datum:** 2026-04-20
**App-Version:** 0.12.29 (versionCode 139)
**Target SDK:** 35 (compileSdk 35, minSdk 26)
**Scope:** Weltweiter Play-Store-Rollout exkl. IR/KP/CN/RU/BY/CU/SY
**Pruefer:** Rechtssicherheit-Skill (technische Pruefhilfe, keine anwaltliche Beratung)

---

## Disclaimer

Dieser Bericht ist eine **technische Pruefhilfe** und ersetzt KEINE anwaltliche Beratung.
Er markiert fehlende Pflichtangaben und typische Fallstricke basierend auf oeffentlichen
Quellen. Fuer eine verbindliche Rechtspruefung muss ein Fachanwalt fuer IT-Recht
konsultiert werden.

---

## Zusammenfassung

| Kategorie | Anzahl |
|-----------|--------|
| KRITISCH (Release blockieren) | 5 |
| HOCH (vor Release fixen) | 6 |
| MITTEL (sollte fixen) | 7 |
| OK | 13 |

**Gesamtstatus: NICHT RELEASE-FAEHIG** — 5 kritische Befunde, 2 davon mit aktiver
Abmahnwelle seit Q3/2025.

---

## KRITISCH — MUSS SOFORT GEFIXT WERDEN

### K1. Tote ODR-Plattform-Links in 6 Dokumenten

**Fakt:** Die EU-Online-Streitbeilegungsplattform `ec.europa.eu/consumers/odr/` wurde
am **20.07.2025 abgeschaltet**. IT-Recht Kanzlei und WBS.legal berichten seit Q3/2025
eine aktive Abmahnwelle — toter Link = Irrefuehrung nach § 5 UWG.

**Betroffene Dateien:**
- `docs/IMPRESSUM.md:80` + `docs/IMPRESSUM.html:72`
- `docs/IMPRINT.en.md:78` + `docs/IMPRINT.en.html:72`
- `docs/NUTZUNGSBEDINGUNGEN.md:230` + `docs/NUTZUNGSBEDINGUNGEN.html:174`
- `docs/TERMS.en.md:241` + `docs/TERMS.en.html:174`

**Fix:** Kompletten ODR-Abschnitt ersatzlos streichen. VSBG-Klausel behalten.

---

### K2. `LegalDocumentScreen.kt` laedt falschen Pfad

**Fakt:** Code `LegalDocumentScreen.kt:108` laedt
`file:///android_asset/legal/${document.fileName}` — d.h. `legal/DATENSCHUTZ.html`.
**Tatsaechlich:** Dateien liegen unter `assets/legal/de/*.html`. Ordner `assets/legal/en/`
ist **komplett leer**.

**Konsequenz:** Leere WebView oder 404 beim Tippen auf "Datenschutz" / "Nutzungs-
bedingungen" / "Impressum". Faktisch = keine DSE vorhanden = Art. 12/13 DSGVO-Verstoss
= nach BGH 27.03.2025 UWG-abmahnbar.

**Fix:**
1. Englische HTMLs in `assets/legal/en/` kopieren.
2. `LegalDocumentScreen.kt` locale-aware: `Locale.getDefault().language == "en"` →
   `legal/en/PRIVACY.html` etc., sonst `legal/de/DATENSCHUTZ.html` etc.

---

### K3. Englische Datenschutzerklaerung (PRIVACY.en) fehlt

**Fakt:** `docs/` enthaelt `IMPRINT.en.*` und `TERMS.en.*`, aber keine `PRIVACY.en.*`.
Fuer US/UK/CA/AU/NZ/IE/ZA/KE/NG/SG/PH/MY/IN/HK: keine englische DSE verfuegbar.

**Fix:** PRIVACY.en.md + PRIVACY.en.html aus DATENSCHUTZ.md uebersetzen, inkl.
laenderspezifischer Anhaenge (siehe K5).

---

### K4. Consent-Screen: Kein granularer Opt-In fuer Gemini/Groq/Edge/Drive

**Fakt (Code-Analyse):**
- `ConsentScreen.kt` hat 2 Optionen: `AcceptAll` oder `OnlyRequired`.
- `ConsentViewModel.kt` steuert nur `setAnalyticsCollectionEnabled(true/false)`.
- Gemini, Groq, Edge TTS, Google Drive: kein separater Opt-In vor erster Nutzung.

**Risiko:** EDSA-Leitlinie 03/2023 "Ablehnen gleich einfach wie Annehmen" — Akzeptieren-
Button ist visuell dominanter (Glow, Breathing). Dark-Pattern-Argumentation moeglich.
Beim ersten Mikrofon-Tap wird Audio an Groq USA gesendet ohne Pre-Usage-Consent.

**Fix (Minimum):** Pre-Usage-Dialog beim ersten Tap auf Mikrofon/Dashboard/TTS:
"Deine Aufnahme/Anfrage wird an [Groq/Gemini/Microsoft] (USA) gesendet. Einverstanden?"

**Fix (rechtssicher):** Granulare Opt-In-Liste im Consent-Screen mit Togglern pro
Dienst.

---

### K5. Laenderspezifische Rights-Sections fehlen

**Fakt:** DATENSCHUTZ.md Abschnitt 8a listet Jurisdiktionen auf Deutsch. Es fehlt:

| Jurisdiktion | Fehlt |
|--------------|-------|
| California CCPA/CPRA | "Rights of California Residents" + Do-Not-Sell-Link + GPC-Honor |
| Texas TDPSA | GPC-Pflicht (keine Mindestschwelle) |
| Delaware/Rhode Island | Dritte namentlich, nicht nur Kategorien |
| UK GDPR | ICO-Beschwerdeweg, UK-IDTA, RLI-Grundlage |
| Quebec Law 25 | CAI-Beschwerde, Portabilitaet, Franzoesisch |
| Australien APP 8 | OAIC-Beschwerde, kein generelles Loeschrecht |
| NZ IPP 3A (ab 01.05.2026) | Indirekte Erhebung durch KI-Dienste |
| BIPA Illinois | Voiceprint-Einwilligung + Retention |

---

## HOCH — VOR RELEASE FIXEN

### H1. Tuerkei: VERBIS-Pflicht + KVKK-SCCs
Empfehlung: Tuerkei im Play Console ausschliessen.

### H2. Brasilien: Portugiesische DSE + ANPD-SCCs seit 23.08.2025 Pflicht
Empfehlung: Brasilien ausschliessen oder ANPD-SCCs unterzeichnen.

### H3. Suedkorea: Koreanische DSE + Empfaenger namentlich
Praezedenzfall DeepSeek (02/2025). Empfehlung: Korea ausschliessen.

### H4. Saudi-Arabien: Arabische DSE + Voice als biometrisch
SDAIA vollstreckungsbefugt seit 14.09.2024. Empfehlung: Saudi-Arabien ausschliessen.

### H5. Vietnam: Impact-Assessment-Dossier beim MPS
Neues PDP-Gesetz ab 01.01.2026. Empfehlung: Vietnam ausschliessen.

### H6. Google Play Account-Deletion: Web-URL im Play-Console-Listing
Seit 31.05.2024 Pflicht: In-App (OK) UND externer Web-Link. Kein
`play-store-metadata/`-Ordner im Repo, keine dokumentierte Web-URL.
**Fix:** github.io-Seite mit Mailto-Link vor Upload anlegen.

---

## MITTEL — SOLLTE GEFIXT WERDEN

| # | Befund | Fix |
|---|--------|-----|
| M1 | Generative-AI-Report-Mechanismus fehlt (Google Play 04/2024) | "Unangemessene KI-Antwort melden"-Button |
| M2 | AI-Act Art. 50 Hinweis fehlt (ab 02.08.2026 Pflicht) | Klausel vorsorglich einbauen |
| M3 | Play Console Kategorie | "Lifestyle" waehlen, nicht "Health & Fitness" |
| M4 | `foregroundServiceType` nicht deklariert in Manifest | Falls Voice-Background: `microphone` setzen |
| M5 | Firebase Analytics IP-Anonymisierung nicht explizit | In Firebase Console aktivieren + DSE erwaehnen |
| M6 | § 356a BGB Widerrufsbutton ab 19.06.2026 | Optional: In-App-Widerrufsbutton |
| M7 | Locale-Fallback bei Nicht-Deutsch/Englisch-System | Englisch als Fallback setzen |

---

## OK — VERIFIZIERT

- Target SDK 35 / compileSdk 35 / minSdk 26
- Impressum § 5 DDG vollstaendig (c/o-Adresse, 24h-Kontaktversprechen)
- DSA Art. 11 Kontaktstelle
- Kleinunternehmer § 19 UStG (neue Schwellen 25k/100k)
- Widerrufsbelehrung § 355/356 BGB + Muster-Widerrufsformular (NUTZUNGSBEDINGUNGEN § 16)
- § 356 Abs. 5 BGB Erloeschen-Klausel
- Medizinischer Disclaimer (NUTZUNGSBEDINGUNGEN § 4.1)
- Drittanbieter-Kette namentlich in DSE (Groq, Edge TTS, Gemini, Drive, Analytics)
- Rechtsgrundlagen Art. 6 DSGVO je Dienst
- DPF + SCCs als Transfer-Basis mit Datum 10.07.2023
- Konto-Loeschen-Button (FirebaseAuth.currentUser.delete())
- TTDSG→TDDDG, TMG→DDG umbenannt
- ACCESS_COARSE_LOCATION in DSE Abschnitt 3.4 dokumentiert

---

## Laender-Freigabe-Matrix (nach Fixes K1–K5)

| Region | Status | Zusatz |
|--------|--------|--------|
| DACH (DE/AT/CH) | FREI | — |
| EU (NL/FR/IT/ES/PL/etc.) | FREI | — |
| UK | FREI | ICO-Section |
| Irland | FREI | — |
| USA | BEDINGT | CCPA-Section + GPC |
| Kanada (exkl. Quebec) | FREI | OPC-Section |
| Quebec | BEDINGT | Franzoesisch + CAI |
| Australien | FREI | APP 8 + OAIC |
| Neuseeland | FREI | IPP 3A ab 05/2026 |
| Brasilien | AUSSCHLIESSEN | Portugiesisch + ANPD-SCCs |
| Mexiko | BEDINGT | Aviso de Privacidad Spanisch |
| Japan | BEDINGT | Japanische DSE |
| Suedkorea | AUSSCHLIESSEN | Koreanisch + Praezedenzfall DeepSeek |
| Indonesien | BEDINGT | Bahasa-DSE |
| Vietnam | AUSSCHLIESSEN | MPS-Dossier |
| Singapur | FREI | — |
| Indien | FREI bis 05/2027 | Englisch reicht |
| Tuerkei | AUSSCHLIESSEN | VERBIS + KVKK-SCCs |
| Saudi-Arabien | AUSSCHLIESSEN | Arabisch + SDAIA |
| VAE | BEDINGT | Arabisch empfohlen |
| Suedafrika | FREI | POPIA-Section |
| Kenia/Nigeria/Aegypten/Marokko | BEDINGT | Englisch reicht weitgehend |
| Israel | FREI | EU-Adequacy |

**Play-Console-Strategie:** Ausschliessen: TR, KR, VN, SA, BR (zusaetzlich zu
IR/KP/CN/RU/BY/CU/SY). Rest nach K1–K5 freigeben.

---

## Fix-Reihenfolge (4–6 Stunden Aufwand)

1. K1 — ODR-Links aus 8 Dateien entfernen (Python-Batch)
2. K2 — LegalDocumentScreen.kt locale-aware machen
3. K3 — PRIVACY.en.md + .html erstellen
4. K2-Follow-up — englische HTMLs nach assets/legal/en/ kopieren
5. K4 — Granulare Opt-In-Liste ODER Pre-Usage-Dialogs
6. K5 — CCPA/UK/AU/Quebec-Sections in PRIVACY.en
7. H1–H5 — Laenderausschluss beim Play-Console-Upload
8. H6 — Account-Deletion-Web-URL (github.io)
9. M1 — Report-KI-Button
10. M2 — AI-Act-Klausel in DSE

---

## Quellen-Stand

Basierend auf 5 parallelen Researchern am 2026-04-20:
- DE/EU (DSGVO, TDDDG, DDG, AI Act, ODR-Abschaltung, BGH 27.03.2025)
- US/UK/CA/AU (CCPA 2026, UK DUA Act 2025, PIPEDA, Quebec Law 25, Australian PA Reform 2024, BIPA)
- Asien/LATAM (APPI, PIPA/DeepSeek, DPDP 2027, LGPD ANPD-SCCs 08/2025, KVKK VERBIS)
- Google Play Policies (Data Safety, Target SDK 35, Account Deletion 05/2024, AI-Policy)
- Abmahn-Trends 2025/2026 (BGH 27.03.2025, ODR-Plattform, § 356a BGB 06/2026)
