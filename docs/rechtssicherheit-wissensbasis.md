# rechtssicherheit.md — Wissensbasis

**Letzte Recherche:** 2026-04-28
**Naechste Pflicht-Pruefung:** 2026-05-28 (+30 Tage fuer Play Policies) bzw. 2026-07-28 (+90 Tage allgemein)
**Aktueller Skill-Stand:** 2026-04-26 (Skill-Definition)

---

## Disclaimer

Diese Wissensbasis ist eine **technische Pruefhilfe** und ersetzt **keine anwaltliche Beratung**. Sie konsolidiert Recherche-Ergebnisse aus oeffentlichen Quellen. Vor jedem Play-Store-Release MUSS ein Fachanwalt fuer IT-Recht die App pruefen.

---

## Quellenregister (Stand 28.04.2026)

| Datum | Quelle | Thema | Relevanz |
|---|---|---|---|
| 28.04.2026 | gesetze-im-internet.de/ddg | DDG § 5 Anbieterkennzeichnung | DE-Pflicht |
| 28.04.2026 | it-recht-kanzlei.de | Impressumspflicht nach DDG | DE-Pflicht |
| 28.04.2026 | gesetze-im-internet.de/bgb/__356.html | § 356 BGB Widerrufsrecht | DE-Pflicht |
| 28.04.2026 | noerr.com | § 356a BGB Widerrufsbutton ab 19.06.2026 | DE-Stichtag |
| 28.04.2026 | anwalt.de BGH III ZR 59/24 (10.07.2025) | AGB-Verweis bei Vertragsschluss | BGH-Rechtsprechung |
| 28.04.2026 | anwalt.de BGH XI ZR 139/23 (19.11.2024) | Zustimmungsfiktion in AGB | BGH-Rechtsprechung |
| 28.04.2026 | datenschutzkanzlei.de | § 25 TDDDG Tracking-Consent | DE-Pflicht |
| 28.04.2026 | ai-act-law.eu | EU AI Act Art. 50 Transparenz | EU-Stichtag 02.08.2026 |
| 28.04.2026 | google.com/policies/frameworks | EU-US DPF Stand 2026 | Drittlandtransfer |
| 28.04.2026 | dlapiper.com | DPF EU-Klage abgewiesen 03.09.2025 | Stand DPF |
| 28.04.2026 | wilmerhale.com | EuGH-Berufung gegen DPF eingereicht 31.10.2025 | Risiko DPF |
| 28.04.2026 | support.google.com Play Console | Data Safety Form 2026 | Play-Pflicht |
| 28.04.2026 | support.google.com Play Console | Account Deletion Policy (Mai 2024) | Play-Pflicht |
| 28.04.2026 | support.google.com Play Console | Health Apps Policy (Aug 2025) | Play-Pflicht |
| 28.04.2026 | support.google.com Play Console | AI-Generated Content Policy | Play-Pflicht |
| 28.04.2026 | support.google.com Play Console | Subscription Disclosure | Play-Pflicht |
| 28.04.2026 | support.google.com Play Console | Permissions Declaration | Play-Pflicht |
| 28.04.2026 | mishcon.com | UK GDPR Art. 27 Representatives | UK-Pflicht |
| 28.04.2026 | oag.ca.gov | CCPA/CPRA Schwellenwerte 2026 | US-Bundesstaat |
| 28.04.2026 | priv.gc.ca | PIPEDA Mobile Apps | CA-Pflicht |
| 28.04.2026 | oaic.gov.au | Privacy Act 1988 + 2024 Reform | AU-Pflicht |
| 28.04.2026 | ftc.gov | COPPA Age Verification 2026 | US-Pflicht (Kinder) |
| 28.04.2026 | securityscientist.net | Japan APPI 2026 | JP-Empfehlung |
| 28.04.2026 | verasafe.com | Korea PIPA Foreign Business 2025 | KR-Pflicht (KO-Sprache) |
| 28.04.2026 | secureprivacy.ai | India DPDP Phase 1 | IN-Stichtag Mai 2027 |
| 28.04.2026 | iclg.com | Brazil LGPD 2025-2026 | BR-Pflicht |
| 28.04.2026 | hoganlovells.com | LGPD ANPD-SCC seit Aug 2025 | BR-Pflicht |
| 28.04.2026 | it-recht-kanzlei.de | Tracking-Cookies ohne Einwilligung | Abmahn |
| 28.04.2026 | datev-magazin.de | Google-Fonts-Abmahnwelle als rechtsmissbraeuchlich | DE-Abmahn |
| 28.04.2026 | datenschutzaufsicht.de / projekt29.de | EDPB-Pruefschwerpunkt 2026: Transparenz | DE-Aufsicht |

---

## Pflichtangaben-Matrix

### EU/DE — Datenschutzerklaerung

**Pflicht-Inhalte (DSGVO Art. 13/14):**
- Verantwortlicher: Name, Adresse, E-Mail, ggf. Datenschutzkontakt
- App-Name + Package-ID + Store-Bezug
- Datenkategorien
- Zwecke + Rechtsgrundlagen je Zweck (Art. 6)
- Empfaenger / Drittanbieter / SDKs einzeln
- Drittlandtransfer + Mechanismus (DPF / SCC / Einwilligung)
- Speicherdauer oder Kriterien
- Betroffenenrechte: Art. 15-21 + 7 Abs. 3 + 77 Beschwerderecht (Aufsichtsbehoerde benennen)
- Pflicht/Freiwilligkeit der Bereitstellung
- Profiling/automatisierte Entscheidung — falls vorhanden
- Kinder/Jugendliche — falls relevant
- Sicherheitsmassnahmen (knapp)
- Account-/Datenloeschung
- Stand/Datum

**Spezial fuer App:**
- Permissions einzeln + Zweck
- Gerätekennungen (AAID, Firebase Installation ID, Android ID)
- Drittanbieter-SDKs aufgelistet (Firebase, AdMob, Crashlytics, Analytics, Auth, AI-APIs, Drive)

**Release-Blocker:**
- Generator-Text ohne App-/SDK-Bezug
- "Wir sammeln keine Daten" trotz aktiver SDKs/Cloud
- Fehlende Drittanbieter
- Fehlende Rechtsgrundlagen
- Fehlender Loesch-/Widerrufsweg

### EU/DE — Impressum (DDG § 5, Mai 2024+)

**Pflicht (kommerzielle Apps):**
- Vollstaendiger Name (Privatperson: buergerlicher Name, kein Pseudonym)
- Ladungsfaehige Anschrift (kein reines Postfach — Briefkasten-Service erlaubt)
- E-Mail (immer Pflicht)
- Mind. ein weiterer Kommunikationsweg (Telefon ODER funktionierendes Kontaktformular — Telefon nicht zwingend per BGH 2019)
- USt-ID — wenn vorhanden
- Bei juristischen Personen: Vertretungsberechtigte
- Register (HRB) — falls einschlaegig
- Aufsichtsbehoerde / Berufsangaben — falls einschlaegig
- **In der App** dauerhaft erreichbar (max. 2 Klicks — BGH "Two-Click")
- Plus: Erreichbar im Play-Store-Listing unter "Entwicklerkontakt"

**Bei Privatperson + kommerzieller App (Premium-Abo, Werbung):**
- Pflicht ist Privatadresse → Briefkasten-Service zulaessig (~50 EUR/Jahr)
- Gewerbeanmeldung sobald Einnahmen — Kleinunternehmer-Hinweis im Impressum

**Bussgeld:** bis 50.000 EUR (§ 33 Abs. 2 Nr. 1 DDG)

### EU/DE — AGB (Premium-Abo)

**Pflichtinhalte:**
- Anbieter, App-Name, Leistungsbeschreibung
- Nutzungsregeln
- Preise, Abrechnungsintervall, Verlaengerung, Kuendigung
- Verbraucherrechte (Verweis auf Widerrufsbelehrung)
- Aenderungsvorbehalt — siehe Verbote unten

**Verbotene Klauseln (BGH-Rechtsprechung 2024-2025):**
- Zustimmungsfiktion bei AGB-Aenderungen (BGH XI ZR 139/23, 19.11.2024) — UNWIRKSAM
- Einseitige Preiserhoehungsklauseln ohne angemessenen Grund + Widerspruchsrecht
- AGB-Verweis "wie auf unserer Website" beim Vertragsschluss ohne tatsaechliche Einbeziehung (BGH III ZR 59/24, 10.07.2025) — UNWIRKSAM

**§ 312j Abs. 3 BGB Bestellbutton-Pflicht:**
- Eindeutig "zahlungspflichtig bestellen" / "kostenpflichtig kaufen" / "zahlungspflichtig abonnieren"
- Reine Preisangabe ("Ab 0,08 EUR / Tag") REICHT NICHT
- "Weiter", "Bestellen" allein reichen NICHT

### EU/DE — Widerrufsrecht (BGB §§ 355, 356)

**Pflichten:**
- 14-Tage-Frist VOR Kaufabschluss erlaeutern
- Bei sofortiger digitaler Lieferung (§ 356 Abs. 5 BGB): Verzicht braucht ZWEI Schritte:
  1. Ausdrueckliche Zustimmung zum sofortigen Beginn vor Fristablauf
  2. Bestaetigung dass dadurch Widerrufsrecht erlischt
- Beides als Pflicht-Checkbox (nicht vorausgefuellt!) in Paywall **vor** dem Bestellbutton
- Muster-Widerrufsformular optional erreichbar (BMJV-Muster)
- E-Mail-Bestaetigung "auf dauerhaftem Datenträger" mit Hinweis auf Erloeschen

**Stichtag 19.06.2026: § 356a BGB Widerrufsbutton-Pflicht**
- Button "Vertrag widerrufen" (nicht "Widerruf", nicht "Kuendigen")
- Hervorgehoben, nicht versteckt
- Eigene Widerruf-Seite mit Formular (Name, Vertragsnummer, E-Mail) — keine Pflicht-Begruendung
- Bestaetigungs-Button "Widerruf bestaetigen"
- Automatische E-Mail-Bestaetigung mit Datum+Uhrzeit
- Verfuegbar waehrend gesamter Frist
- Bussgeld bis 50.000 EUR / 4% Jahresumsatz

### EU — § 25 TDDDG Tracking-Consent

**Einwilligung Pflicht fuer:**
- Firebase Analytics, Crashlytics, Sentry, Amplitude, Mixpanel
- AdMob, Werbe-IDs
- KI-APIs wenn personenbezogene Daten uebertragen

**Ausgenommen (technisch unbedingt notwendig):**
- Google Play Billing (technisch noetig)
- Authentication / Session
- App Check / Integrity Token (berechtigtes Interesse)
- Remote Config (anonymisiert, berechtigtes Interesse)

**Widerruf so einfach wie Zustimmung. Vor erstem SDK-Init. Granulare Toggles empfohlen.**

### EU — AI Act Art. 50 (Stichtag 02.08.2026)

**Pflichten Consumer-Apps:**
- KI-Interaktion offenlegen ("Powered by AI" / "Diese Antwort wurde von KI generiert")
- KI-Output sichtbar markieren (UI-Badge "KI-generiert")
- DSE-Ergaenzung welche KI-Systeme + welche Modelle
- In-App-Hinweis vor erster KI-Nutzung (nicht nur in Settings versteckt)

**Sanktion:** bis 15 Mio EUR / 3% Weltumsatz

### Google Play — Pflichten

**Vor Submit:**
- Privacy Policy URL erreichbar, oeffentlich, nicht geo-fenced, nicht editierbar
- Account Deletion: In-App-Pfad + funktionale Web-URL (in Data Safety Form eintragen)
- Data Safety Form vollstaendig (App Activity, Audio, Health Info, Financial, Device IDs etc.)
- Health Apps Declaration **wenn Mood/Health-Features** (seit Aug 2025 erweitert)
- AI-Generated Content Policy: nur wenn KI-Core-Feature mit freier Generierung
- Subscription Disclosure: Preis, Intervall, Auto-Renewal, Cancellation in Paywall sichtbar
- targetSdk = aktuelle API-35 (Android 15) Pflicht fuer neue Apps
- Play Billing Library 7+
- Sensitive Permissions Declaration (Standort, Kontakte, SMS, READ_MEDIA)

**Top-Rejection-Gruende 2025:**
1. Data Safety inaccurate (SDK-Datenuebertragungen vergessen)
2. Fehlende Account Deletion (Google Sign-In zaehlt!)
3. Health Declaration vergessen (Mood = Health)
4. Privacy Policy broken link
5. Subscription Disclosure unvollstaendig
6. targetSdk zu niedrig
7. Drittanbieter (z.B. KI-API) nicht in Data Safety
8. AI-Coaching nicht gekennzeichnet (sobald AI Act greift)

### CCPA/CPRA (Kalifornien) — Schwellenwerte 2026

**Anwendbar nur bei MIND. EINEM:**
- Jahresumsatz > 26,625,000 USD, ODER
- Daten von 100,000+ Kaliforniern jaehrlich, ODER
- 50%+ Umsatz aus Datenverkauf

**Kleine Privatentwickler i.d.R. EXEMPT.** Trotzdem prophylaktisch "Right to Know / Delete / Correct / Limit Use of Sensitive PI / Non-Discrimination" und "Do Not Sell" in Privacy Policy aufnehmen.

**Sensitive PI** umfasst Mood-Daten ("mental or physical health condition") — bei Trigger CCPA auch "Right to Limit Use" Pflicht.

### UK GDPR Art. 27 — Vertreter

**Pflicht** wenn UK-Markt aktiv vermarktet (Play Store fuer UK = Targeting):
- Ausnahme greift NICHT bei Art. 9-Daten (Mood = Gesundheitsdaten besondere Kategorie)
- Vertreter-Service ~£100-300/Jahr (GDPR Local, EUverify, Captain Compliance)
- Name + Adresse + E-Mail in DSE
- IDTA / UK-SCC fuer USA-Transfer

### PIPEDA (Kanada ohne Quebec)

**Gilt fuer alle kommerziellen Apps** (kein Schwellenwert).
- 10 Fair Information Principles
- Englisch ausreichend (Quebec=Franzoesisch — separat behandeln)
- Drittstaaten-Transfer-Hinweis Pflicht
- Beschwerdestelle: OPC (priv.gc.ca)

### Australia Privacy Act 1988 + 2024 Reform

**Tranche 1 (Dez 2024):** Erhoehte Strafen, Privacy Tort, OAIC-Befugnisse — aktiv.
**Tranche 2 (2026-2027):** AU$3M-Schwelle faellt — KMU werden anwendbar.
**Stand 28.04.2026:** Schwelle noch aktiv. Privacy Tort gilt aber bereits.

**AI-Disclosure Pflicht ab Dezember 2026** — Apps mit AI die Nutzer "significantly affect".

### APPI (Japan)

- Gilt sofort fuer jeden japanischen Nutzer (kein Schwellenwert)
- Englisch rechtlich ausreichend (Japanisch empfohlen)
- USA-Transfer: Opt-in Einwilligung ODER Vertrag mit US-Empfaenger (Google DPA reicht)
- Strafen: bis ¥100 Mio.

### PIPA (Korea)

- Koreanisch-Pflicht in Privacy Policy
- Cross-Border-Transfer offenlegen: Land + Empfaenger + Zweck + Dauer
- AI-Output Labeling Pflicht (seit Jan 2026)
- Lokaler Vertreter nur ab 1 Mio Nutzer/Tag (i.d.R. nicht relevant)

### DPDP (Indien)

- **Enforcement erst ab 13.05.2027** (18 Monate Uebergang seit 13.11.2025)
- Englisch ausreichend
- DPO nur fuer "Significant Data Fiduciaries"
- Grievance Officer (kann Entwickler selbst sein)
- Aktuell USA-Transfer erlaubt

### LGPD (Brasilien)

- Gilt fuer alle Apps mit BR-Nutzern
- **ANPD-SCCs Pflicht fuer USA-Transfers seit 23.08.2025**
- Klein-DPO-Ausnahme fuer Microunternehmen
- Portugiesisch praktisch unvermeidlich (DPO kommuniziert mit ANPD auf Portugiesisch)
- Google Cloud DPA deckt LGPD-SCCs ab

### China — PIPL

**Empfehlung: Im Play Store ausschliessen.** Google Play in VR China gesperrt — kein Markt. PIPL-Compliance extrem aufwaendig (lokaler Vertreter, Datenlokalisierung, Sicherheitsbewertung).

---

## Sprach-Anforderungen pro Markt

| Markt | Pflicht-Sprache | Empfehlung |
|---|---|---|
| Deutschland | DE | DE Pflicht |
| Oesterreich | DE | DE Pflicht |
| Schweiz | DE | DE Pflicht |
| Frankreich | FR | FR empfohlen, EN OK |
| UK | EN | EN reicht |
| USA | EN | EN reicht — Spanisch nicht Pflicht |
| Kanada (ohne Quebec) | EN | EN reicht |
| Quebec | FR | aktuell ausgeschlossen — separate Session |
| Australien | EN | EN reicht |
| Japan | (EN ausreichend) | JA empfohlen |
| Korea | **KO Pflicht** | KO Pflicht |
| Indien | EN | EN reicht (DPDP-Enforcement Mai 2027) |
| Brasilien | (EN tolerabel) | PT-BR praktisch unvermeidlich |
| Singapur | EN | EN reicht |
| Vietnam/Indonesien | EN | Landessprache empfohlen, EN tolerierbar |
| China | — | **AUSSCHLIESSEN** |

---

## Aktuelle Abmahn-Hotspots (Stand April 2026)

1. **Fehlendes Impressum** (DDG) — Wettbewerber, Verbraucherzentrale, Streitwert 5.000-15.000 EUR
2. **Tracking ohne Einwilligung** (TDDDG § 25) — bis 300.000 EUR
3. **Unvollstaendige DSE / fehlende Drittland-Infos** — EDPB-Pruefschwerpunkt 2026
4. **Falsche/fehlende Widerrufsbelehrung** — Haendlerbund/IDO-Vertragsstrafen
5. **Fehlende Bestellbutton-Beschriftung § 312j BGB** — klassisches Abmahnthema
6. **AGB-Klauseln** (Zustimmungsfiktion, Preiserhoehung) — BGH-Urteile 2024/2025 schaffen neue Abmahngrundlagen
7. **EU AI Act Art. 50** ab 02.08.2026 — neue Abmahnflaeche
8. **§ 356a BGB Widerrufsbutton** ab 19.06.2026 — 50.000 EUR Bussgeld bei Verstoss
9. **Health Claims** (HWG) — VSW e.V. besonders aktiv, 10.000-25.000 EUR
10. **Dark Patterns in Paywall** — vzbv beobachtet aktiv (Temu, Eventim)

**Aktive Abmahner 2025-2026:**
- Wettbewerber (haeufigste Quelle)
- Verbraucherzentrale Bundesverband (vzbv) — Dark Patterns
- Verband Sozialer Wettbewerb (VSW) — HWG
- Datenschutzbehoerden — DSGVO/TDDDG
- IDO Verband — nur Altfaelle (nicht mehr abmahnberechtigt seit BGH/UWG 2021)
- IT-Recht-Kanzlei berichtet aktive Welle

---

## App-Audit-Log

| Datum | App | Version | Status | Blocker | Hoch | Mittel | Niedrig | Bericht |
|---|---|---|---|---:|---:|---:|---:|---|
| 2026-04-20 | BestJournalAndroid | (vor 0.14.x) | initial | 0 | (v1) | — | — | docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20.md |
| 2026-04-21 | BestJournalAndroid | 0.13-x | v4 | 0 | 5 | 8 | 4 | docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v4.md |
| 2026-04-23 | BestJournalAndroid | 0.14-x | v5 | 1 | 3 | 6 | 4 | docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-23-v5.md |
| 2026-04-28 | BestJournalAndroid | 0.14.13 (vc 188) | **v6** | **0** | **2** | **5** | **3** | docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-28-v6.md |

---

## Wiederverwendbare Befundmuster (fuer kuenftige Audits)

### Muster 1 — "Bestellbutton ohne klare Zahlungspflicht-Beschriftung"

- Symptom: CTA-Button in Paywall zeigt nur Preis ("Ab X EUR/Tag")
- Norm: § 312j Abs. 3 BGB
- Fix: Button-Beschriftung "Jetzt zahlungspflichtig fuer X abonnieren" ODER Pflicht-Footer "Mit Klick schliesst du ein zahlungspflichtiges Abo ab"
- Muss fuer jeden CTA (monthly, yearly, lifetime, churn-offer)

### Muster 2 — "Sofort-Verzicht ohne separate Checkbox"

- Symptom: AGB erwaehnen § 356 Abs. 5 BGB Verzicht, aber Paywall hat keine eigene Checkbox
- Norm: § 356 Abs. 5 BGB i.V.m. Art. 246a § 1 Abs. 2 EGBGB
- Fix: Checkbox in Paywall mit zwei-Punkte-Erklaerung (sofort + Verlust Widerrufsrecht), Pflicht-Feld

### Muster 3 — "DSE behauptet DPF-Zertifizierung ohne Verifikation"

- Symptom: DSE sagt "Anbieter X ist nach DPF zertifiziert"
- Verifikation: dataprivacyframework.gov Liste pruefen
- Fix: Falls nicht zertifiziert, auf SCC umformulieren

### Muster 4 — "AGB-Zustimmungsfiktion (BGH 2024)"

- Symptom: "Wenn Sie nicht widersprechen, gelten die neuen AGB als angenommen"
- Norm: BGH XI ZR 139/23, 19.11.2024
- Fix: Aktive Zustimmung verlangen (in-App Re-Consent bei Aenderung)

### Muster 5 — "AI-Output ohne Kennzeichnung"

- Symptom: KI-generierter Text/Antwort wird ohne Badge angezeigt
- Norm: EU AI Act Art. 50 (ab 02.08.2026), Korea PIPA AI Basic Act (jetzt)
- Fix: AiGeneratedBadgeInline / "KI-generiert"-Label an JEDEM KI-Output (nicht nur Hauptscreen)

### Muster 6 — "Account Deletion nur in App, kein Web-Link"

- Symptom: Loeschen nur via Settings — Nutzer der App deinstalliert hat hat kein Loeschpfad
- Norm: Google Play Account Deletion Policy (Mai 2024 Pflicht)
- Fix: Statisch gehostete account-deletion.html mit funktionierender E-Mail-Anfrage

### Muster 7 — "DSE deckt nicht alle SDKs ab"

- Symptom: build.gradle.kts hat SDK X, DSE erwaehnt es nicht
- Norm: DSGVO Art. 13 + Google Play Data Safety
- Fix: SDK-Inventur per `grep implementation app/build.gradle.kts` + DSE-Section pro SDK

---

## Muster-Klauseln (mit Quelle)

### Impressum (Privatperson + Briefkasten + Kleinunternehmer)

```
Angaben gemaess § 5 DDG:

[Vollstaendiger Name]
c/o [Briefkasten-Service]
[Strasse]
[PLZ Ort]
Deutschland

E-Mail: [E-Mail]

Umsatzsteuer:
Es wird gemaess § 19 UStG (Kleinunternehmerregelung) keine Umsatzsteuer erhoben
und folglich auch nicht ausgewiesen.

Verbraucherstreitbeilegung:
Ich bin nicht bereit und nicht verpflichtet, an Streitbeilegungsverfahren vor
einer Verbraucherschlichtungsstelle teilzunehmen.
```

Quelle: it-recht-kanzlei.de DDG-Muster, BMJV Verbraucherstreitbeilegungsgesetz

### Drittlandtransfer USA

```
Diese Daten werden in die USA uebertragen. Die Uebertragung erfolgt auf Grundlage von:
- EU-US Data Privacy Framework (Angemessenheitsbeschluss der EU-Kommission vom
  10. Juli 2023), fuer Anbieter X (DPF-Liste pruefen!)
- EU-Standardvertragsklauseln (Art. 46 Abs. 2 lit. c DSGVO), fuer Anbieter Y

Weitere Informationen: https://policies.google.com/privacy/frameworks
```

Quelle: Google Cloud DSGVO, ec.europa.eu DPF-Beschluss

### Widerrufsbelehrung digitale Inhalte

```
Sie haben das Recht, binnen vierzehn Tagen ohne Angabe von Gruenden diesen
Vertrag zu widerrufen.

[Adresse Anbieter]
[E-Mail]

Das Widerrufsrecht erlischt bei einem Vertrag zur Bereitstellung von nicht auf
einem koerperlichen Datentraeger befindlichen digitalen Inhalten, wenn wir mit
der Ausfuehrung des Vertrags begonnen haben, nachdem Sie
1. ausdruecklich zugestimmt haben, dass wir mit der Ausfuehrung vor Ablauf der
   Widerrufsfrist beginnen, und
2. Ihre Kenntnis davon bestaetigt haben, dass Sie durch Ihre Zustimmung mit
   Beginn der Ausfuehrung Ihr Widerrufsrecht verlieren.
```

Quelle: BGB § 356 Abs. 5, BMJV Muster-Widerrufsbelehrung Anlage 2 Art. 246a EGBGB

### KI-Hinweis (Art. 50 EU AI Act, ab 02.08.2026)

```
Hinweis: Diese Antwort wurde mit Hilfe von Kuenstlicher Intelligenz erzeugt.
Sie ist eine algorithmisch generierte Vorschlag und ersetzt keine professionelle
Beratung durch eine Fachperson.
```

Quelle: caralegal.eu Transparenzpflichten, ai-act-law.eu

### CCPA-Rights-Statement (Privacy Policy)

```
California residents have the following rights under the California Consumer
Privacy Act (CCPA), as amended by CPRA:
- Right to Know which personal information we have collected, used and shared
- Right to Delete your personal information
- Right to Correct inaccurate personal information
- Right to Opt-Out of Sale or Sharing of personal information
- Right to Limit Use of Sensitive Personal Information
- Right to Non-Discrimination for exercising these rights

We do not sell or share personal information for cross-context behavioral
advertising. To exercise rights: dev.app.support@gmail.com, subject "CCPA Request".
```

Quelle: oag.ca.gov CCPA-Pflichttexte

---

## Audit-Checkliste fuer kuenftige Apps (Quick-Check vor Release)

**Phase 1 — Manifest + SDKs (5 Min):**
- [ ] AndroidManifest.xml — alle Permissions begruendet?
- [ ] build.gradle.kts — alle SDKs in DSE erwaehnt?
- [ ] cleartextTrafficPermitted="false"
- [ ] backup_rules.xml + data_extraction_rules.xml — sensitive Daten ausgeschlossen?
- [ ] targetSdk = aktuelle API-Pflicht
- [ ] Play Billing Library 7+

**Phase 2 — Rechtsdokumente (15 Min):**
- [ ] DSE: alle 13 Pflichtfelder DSGVO Art. 13
- [ ] Impressum: § 5 DDG vollstaendig + im App + im Store erreichbar
- [ ] AGB: keine BGH-2024-Verbote (Zustimmungsfiktion, AGB-Verweis bei Vertragsschluss)
- [ ] Widerrufsbelehrung: § 356 Abs. 5 mit Verzicht-Erklaerung
- [ ] Account-Deletion: in App + Web-URL
- [ ] DSE-Stand-Datum aktuell (< 90 Tage)

**Phase 3 — UI/Consent (15 Min):**
- [ ] ConsentScreen vor erstem Datenabfluss
- [ ] Per-Service Consent (Analytics, Crashlytics, AI, Cloud) granular
- [ ] Widerrufsweg in Settings genauso einfach wie Zustimmung
- [ ] Paywall: Bestellbutton mit "zahlungspflichtig"-Beschriftung
- [ ] Paywall: Verzichts-Checkbox vor Klick
- [ ] AI-Badge bei jedem KI-Output
- [ ] § 356a Widerrufsbutton in Settings (Stichtag 19.06.2026)

**Phase 4 — Play Console (10 Min):**
- [ ] Privacy Policy URL erreichbar (nicht geo-fenced)
- [ ] Account Deletion Web-URL erreichbar
- [ ] Data Safety Form: alle Datenkategorien + Drittanbieter
- [ ] Health Apps Declaration (wenn Health/Mood/Wellness)
- [ ] AI-Generated Content Policy (wenn KI-Core-Feature)
- [ ] Subscription Disclosure auf Paywall sichtbar
- [ ] Permissions Declaration (Sensitive Permissions)
- [ ] Content Rating (IARC)
- [ ] Quebec/CN ausschliessen falls relevant

---

## Zusammenspiel mit anderen Skills

- `uebersetzung` — bei Aenderung der Rechtstexte → 27 Locales aktualisieren
- `string-extraktor` — wenn Rechtstexte hardcoded statt strings.xml
- `superintelligenz` / `selbstbeobachtung` — Befundmuster persistieren
- `resilient-bugfixing` — Wiederkehrende Befunde (Impressum-Fehler etc.) als systematischen Repo-Check

---

*Wissensbasis erstellt: 28.04.2026 | naechste Pflicht-Pruefung: 28.05.2026*
