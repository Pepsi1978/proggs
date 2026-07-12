# rechtssicherheit.md - Wissensbasis

Letzte Recherche: 2026-07-12 (7 Sonnet-5-Researcher, Engine C)
Naechste Pflicht-Pruefung: 2026-08-11 (+30 Tage, Play Policies betroffen)

> Technische Pruefhilfe, KEINE anwaltliche Beratung. Angelegt beim v8-Audit BestJournalAndroid.

## Quellenregister

| Datum | Quelle | Thema | Klasse | Relevanz |
|---|---|---|---|---|
| 2026-07-12 | support.google.com/.../answer/10787469 | Play Data Safety (Service-Provider vs. Shared) | offiziell | hoch |
| 2026-07-12 | support.google.com/.../answer/11150561 | Prominent Disclosure + Consent | offiziell | hoch |
| 2026-07-12 | support.google.com/.../answer/13327111 | Account Deletion (In-App + Web-URL) | offiziell | hoch |
| 2026-07-12 | support.google.com/.../answer/14094294 | AI-Generated Content (Productivity-Ausnahme) | offiziell | hoch |
| 2026-07-12 | support.google.com/.../answer/14738291 + 13996367 | Health Apps Declaration (Pflicht fuer JEDE App) | offiziell | hoch |
| 2026-07-12 | developer.android.com/google/play/requirements/target-sdk | Neu-Apps API 36, Bestand API 35, Deadline 31.08.2026; Billing v8 | offiziell | hoch |
| 2026-07-12 | support.google.com/.../answer/14115180 | Photo/Video-Permissions (CAMERA unbetroffen, Photo Picker) | offiziell | mittel |
| 2026-07-12 | wbs.legal + ra-plutte.de zu BGH 27.03.2025 I ZR 186/17 | DSGVO-Verstoesse via UWG abmahnbar (Mitbewerber + Verbaende) | fachanwalt | hoch |
| 2026-07-12 | curia C-21/23 "Lindenapotheke" (04.10.2024) | Mitbewerber-Klagebefugnis DSGVO; Bestelldaten = Gesundheitsdaten | offiziell | hoch |
| 2026-07-12 | twobirds/noerr zu BGH 22.05.2025 I ZR 161/24 + OLG Koeln 6 U 62/24 | Kuendigungsbutton §312k weit; kein Login-Zwang | fachanwalt | mittel |
| 2026-07-12 | dr-datenschutz.de zu VG Hannover 10 A 5385/22 (19.03.2025) | "Alles ablehnen" erste Ebene Pflicht, kein Nudging; Analytics vor Consent verboten | fachanwalt | hoch |
| 2026-07-12 | artificialintelligenceact.eu/article/50 | AI Act Art. 50 ab 02.08.2026; GenAI-Bestand-Uebergang 02.12.2026 (AI-Omnibus) | offiziell-nah | hoch |
| 2026-07-12 | tww.law + seybold.de | KI-Kennzeichnung als §3a-UWG-Marktverhaltensregel → abmahnbar; DE-Aufsicht noch unbestimmt | fachanwalt | hoch |
| 2026-07-12 | ferner-alsdorf.de + btlj.org + dataprivacyframework.gov | DPF nach US Supreme Court "Trump v. Slaughter" (29.06.2026) wackelig; EuGH C-703/25 P anhaengig → SCC+TIA-Doppelabsicherung | fachanwalt/offiziell | hoch |
| 2026-07-12 | ico.org.uk DUAA 2025 + gdpr-info.eu Art. 27 | UK-Vertreter-Pflicht besteht trotz DUAA fort (DP-Teil in Kraft 19.06.2026) | offiziell | hoch |
| 2026-07-12 | RCW 19.373 + atg.wa.gov + eff.org | WA MHMDA: Journaling = consumer health data, KEINE Schwelle, Private Right of Action, separate Policy + getrennte Opt-ins | offiziell/extern | hoch |
| 2026-07-12 | federalregister.gov 2026-02866 + Gibson Dunn | FTC Click-to-Cancel vacated (8th Cir. 08.07.2025), ANPRM neu; ROSCA gilt | offiziell | mittel |
| 2026-07-12 | onetrust/cfib Quebec Law 25 + Bill 96 | Privacy Officer ohne QC-Sitz; Francization erst ab 25 MA | extern | mittel |
| 2026-07-12 | oaic.gov.au + iapp | AU Small-Business-Exemption aktiv (Tranche 1); Statutory Tort 10.06.2025 exemption-unabhaengig | offiziell | mittel |
| 2026-07-12 | privacy.org.nz IPP 3A | NZ indirekte Erhebung ab 01.05.2026 (Direkterhebung unberuehrt) | offiziell | niedrig |
| 2026-07-12 | india-briefing + PIB DPDP Rules | Indien: Consent-Manager ab 13.11.2026, substanzielle Pflichten 13.05.2027; Grievance Officer ohne Ausnahme; Eighth-Schedule-Sprachen | offiziell/extern | hoch |
| 2026-07-12 | loc.gov + safeaiforbusiness | Korea AI Basic Act (22.01.2026) groessenunabhaengig: GenAI-Vorabhinweis + Output-Labeling; PIPA 10%-Strafen ab 11.09.2026 | offiziell/extern | mittel (KR ausgeschlossen) |
| 2026-07-12 | bakermckenzie/omm | Japan APPI-Reform-Bill 07.04.2026, Wirkung ~2028; heute geltendes APPI: Domestic Rep fuer Auslands-Anbieter, jp. Policy | extern | mittel |
| 2026-07-12 | verasafe/raffles | Singapur: DPO-Pflicht ohne Schwelle, Kontakt in Notice | extern | mittel |
| 2026-07-12 | pdpathailand.com Sec. 37 + Tilleke | Thailand: Vertreter-Pflicht Sec. 37(5) fuer Auslands-Controller; 8 Bussgelder am 01.08.2025 (Enforcement-Wende) | extern | hoch |
| 2026-07-12 | DFDL + Vietnam Briefing | Vietnam PDPL 91/2025 ab 01.01.2026: DPIA-Dossier an A05 binnen 60 Tagen + Cross-Border-Assessment | extern | hoch (VN ausgeschlossen) |
| 2026-07-12 | Securiti + Digital Policy Alert | Bangladesch PDPA 2026 (deemed 06.11.2025), Enforcement ~05/2027; Localization nur restricted/CII | extern | niedrig |
| 2026-07-12 | Chambers PK + Mondaq | Pakistan: weiterhin KEIN Datenschutzgesetz (legal vacuum), nur PECA | extern | niedrig |
| 2026-07-12 | FJG de Saram | Sri Lanka: Inkrafttreten 18.03.2025 gekippt, Amendment 22/2025 → Anfang 2026 | extern | niedrig |
| 2026-07-12 | mayerbrown + iapp | Brasilien: ANPD-SCC Pflicht seit 23.08.2025 (EU-SCCs reichen NICHT); CDC Art. 31 Portugiesisch | extern | hoch (BR ausgeschlossen) |
| 2026-07-12 | gtlaw + whitecase | Mexiko: LFPDPPP neu 21.03.2025, INAI → Secretaría Anticorrupción y Buen Gobierno (Aviso-Referenzen anpassen!) | extern | mittel |
| 2026-07-12 | ibanet + esenyelpartners | Tuerkei: VERBIS schwellenlos fuer Auslaender; SCC + 5-Tage-Meldung; Bussgelder 2026 +25,49% | extern | hoch (TR ausgeschlossen) |
| 2026-07-12 | sk.ua + zakon.rada 2704-19 | Ukraine: UI/Verbraucherinfo auf Ukrainisch bei Vermarktung (Law 2704-VIII); Reform 8153 noch nicht Gesetz | extern/offiziell | mittel |
| 2026-07-12 | out2sol + sdaia.gov.sa | Saudi PDPL: Registrierung bei sensiblen Daten/Transfer; 48 Enforcement-Decisions 2025/26 | extern/offiziell | mittel (SA ausgeschlossen) |
| 2026-07-12 | clearcomply + inforegulator.org.za | Suedafrika: Information-Officer-Registrierung (Portal seit 04/2025) | extern | niedrig |
| 2026-07-12 | recordinglaw AR + AAIP Res. 132/2018 | Argentinien: RNBD-Registrierung auch fuer Auslands-Controller; Adequacy bestaetigt 01/2024 | extern | niedrig |
| 2026-07-12 | ihk-muenchen + haendlerbund | BFSG: Kleinstunternehmer-Ausnahme (<10 MA UND <2 Mio EUR) fuer Dienstleistungen = Einzelentwickler-App befreit; Status dokumentieren | fachanwalt | hoch |
| 2026-07-12 | srd-rechtsanwaelte | Push-Werbung §7 UWG: OS-Prompt reicht nicht, Infoscreen noetig; funktionale Push frei | fachanwalt | mittel |
| 2026-07-12 | euverify + taylorwessing | GPSR: Apps grundsaetzlich erfassbar (Kommissions-Leitlinien 11/2025), Einordnung umstritten; Kontaktangaben bereitstellen | fachanwalt | niedrig |
| 2026-07-12 | verasafe DSA-Trader + makaka.org | Play Trader-Status: Bezahl-App = Trader → Adresse+Telefon+E-Mail oeffentlich im Listing | extern | hoch |
| 2026-07-12 | testerscommunity | Developer Verification Enforcement ab 09/2026 (BR/SG/ID/TH zuerst); 12 Tester/14 Tage fuer neue Personal-Accounts | extern | mittel |
| 2026-07-12 | play.google.com/intl/de_de/about/play-terms/ | Play-ToS DE: Google Commerce Ltd = Vertragspartner; digitale Inhalte = Widerrufsverzicht durch Google eingeholt; Abos = 14 Tage Widerruf | offiziell | hoch |
| 2026-07-12 | developer.android.com/google/play/billing/deprecation-faq | Billing v7-Deadline 31.08.2026 (neue Apps + Updates), Verlaengerung 01.11.2026 beantragbar; Live-Apps laufen weiter | offiziell | hoch |
| 2026-07-12 | support.google.com/.../answer/10840893 + Chrome-WS-Trader-FAQ + DSA Art. 30/31 | Trader: Telefonnummer wird OEFFENTLICH im EU-Listing angezeigt (SMS-verifiziert); VOIP-/virtuelle Nummer zulaessig; E-Mail allein reicht NICHT; Nicht-Trader-Privatkonto: nur E-Mail oeffentlich | offiziell/extern | hoch |
| 2026-07-12 | dsgvo-gesetz.de/art-30 + lda.bayern.de DSK-Muss-Liste | VVT-Pflicht (Art.-30-Abs.-5-Ausnahme entfaellt bei Art.-9 + regelmaessig); DSFA-Pflicht via DSK-Positivliste "Art.-9 via mobile App + zentrale Aufbereitung" (Groesse ausdruecklich irrelevant) | offiziell | hoch |
| 2026-07-12 | github.com/rany2/edge-tts/issues/290 + azure.microsoft.com/pricing/speech | Edge-Consumer-Endpoint: Sec-MS-GEC-Sperrwellen seit Okt/Nov 2024 (Abschalt-Risiko); Azure Speech F0 Free Tier + $4-15/1M Zeichen danach; Alternative: Google Cloud TTS Chirp 3 HD (bestehender Google-AVV deckt ab, siehe bugs/apis/tts-provider.md) | offiziell/extern | hoch |
| 2026-07-12 | ldi.nrw.de VVT-Muster + lda.bayern.de DSFA-Muster + GDD TOMs | Kostenlose offizielle Vorlagen fuer die Compliance-Artefakte-Session | offiziell | mittel |

## Sprach-Anforderungen pro Markt (Kurzfassung)

| Markt | Pflichttexte in Landessprache? | Quelle |
|---|---|---|
| DE/AT | Deutsch PFLICHT (DSGVO Transparenz) | DSGVO Art. 12 |
| BR | Portugiesisch PFLICHT (CDC Art. 31) | sedconrj CDC |
| MX | Spanisch PFLICHT (Aviso) | gtlaw |
| UA | Ukrainisch bei Vermarktung (Law 2704-VIII) | sk.ua |
| TR | Tuerkisch (Aydınlatma) | ibanet |
| JP | Japanisch praktisch Pflicht | knowledgelib |
| KR | Koreanisch Pflicht | consentstack |
| IN | EN oder Eighth-Schedule-Sprache, Umschalter VOR Consent | certinal |
| ID | Bahasa fuer Verbraucherinfo (UU 24/2009) | ASEAN Briefing |
| TH | Thai empfohlen (Verstaendlichkeit) | DLA Piper TH |
| Quebec | Franzoesisch (Verbrauchervertraege); Francization ab 25 MA | cfib |
| UK/US/AU/NZ/SG/ZA/HK | Englisch genuegt | div. |

## Aktuelle Abmahn-Hotspots (Stand 2026-07)

| Thema | Kern | Empfehlung |
|---|---|---|
| Analytics ohne Consent | #1-Risiko; BGH 27.03.2025 macht DSGVO UWG-abmahnbar; VG Hannover: Reject gleichrangig | Consent-Gate VOR jedem Tracking, Analytics-Init hart aus |
| Kuendigungsbutton §312k | BGH 22.05.2025 weit; Login-Zwang verboten | Login-freier Play-Abos-Link + In-App-Pfad |
| Impressum/DSE fehlt/veraltet | Klassiker; §5a UWG | Oeffentliche URL + In-App, DDG-Referenzen (nie TMG) |
| Falsche Widerrufsbelehrung | Erloeschens-Behauptung ohne echten Verzichts-Mechanismus | Klausel nur behaupten, was der Kauf-Flow wirklich tut |
| KI-Kennzeichnung (ab 02.08.2026) | Art. 50 als Marktverhaltensregel → Abmahnung realer als Bussgeld (DE-Aufsicht unbestimmt) | Badges + DSE-Hinweis + Doku |
| Push-Werbung §7 UWG | OS-Prompt reicht nicht | Werbliche Push nur mit Infoscreen-Einwilligung |
| PAngV §11 Rabatte | 30-Tage-Bestpreis auch bei Abo-Aktionen | Referenzpreis dokumentieren |
| Health-Claims (HWG) | Wirkversprechen "lindert/heilt" abmahnfaehig | Reflexions-/Wohlbefindens-Sprache |
| BFSG | Erste Verfahren Q1 2026; Konkurrenten-Abmahnung moeglich | Kleinstunternehmer-Status dokumentieren |

## App-Audit-Log

| Datum | App | Version | Status | Blocker | Hoch | Commit/Notiz |
|---|---|---|---|---:|---:|---|
| 2026-07-12 | BestJournalAndroid | 0.21.16 (vc 297) | BEDINGT (Fix E1 + 6 HOCH → dann anwaltsreif) | 1 | 6 | v8-Bericht `docs/audit/RECHTSSICHERHEIT-AUDIT-2026-07-12-v8.md`; Regressionen seit v7: Verzichts-Dialog entfernt (D1/D2), DSE nicht nachgefuehrt (C2); neu: E1 URLs 404, E3 Trader-Telefon, C1 Edge-TTS, TH-Ausschluss-Empfehlung |
| 2026-07-12 | BestJournalAndroid | 0.21.16 (vc 297) | **v8.1-REVISION nach Frank-Re-Check: 0 BLOCKER, 3 HOCH** (C1 TTS-AVV, C3 Artefakte, E3 Trader-Telefon) | 0 | 3 | E1 ENTKRAEFTET (App verlinkt existierende Pages-Seite `pepsi1978.github.io/proggs/bestjournal/`, Quelle `docs/bestjournal/`); D1+D2 ENTKRAEFTET (Google Commerce Ltd = Vertragspartner, holt Widerrufsverzicht bei digitalen Inhalten selbst ein, Abos 14 Tage via Google); E2 direkt behoben (Checkliste korrigiert); C2 auf MITTEL (Functions verarbeitet nur Purchase-Token, EU) |

## Wiederverwendbare Befundmuster

| Muster | App-Klasse | Empfohlener Fix |
|---|---|---|
| Rechtstext beschreibt Kauf-/Consent-Mechanik, die im Code laengst entfernt wurde (Compliance-Drift nach Redesign) | jede App mit Paywall | Bei JEDEM Paywall-/Consent-Redesign die Rechtstexte im selben PR mitziehen; Audit-Diff Code vs. Text |
| Play-Pflicht-URLs (Privacy/Deletion) existieren nur im Repo, nie deployed | Indie-Apps | GitHub Pages/Firebase Hosting als fester Release-Schritt + URL-Check (HTTP 200) vor Submit |
| Data-Safety-Checkliste nennt SDKs, die nie eingebaut wurden (Copy-Paste aus Vorlage) | alle | Checkliste gegen gradle/libs.versions.toml generieren, nicht von Hand |
| Inoffizielle Gratis-Endpoints (Edge-TTS) ohne AVV fuer sensible Daten | Apps mit Cloud-TTS/AI | Offizielles API-Produkt mit DPA oder On-Device |
| Skill-Skript check-compliance-artifacts.sh liefert False-Positives, wenn alte Audit-Berichte im Repo liegen (Keyword-Match) | rechtssicherheit-Skill | Skript sollte docs/audit/ + Berichts-Dateien excluden; bis dahin manuell verifizieren |
| DPF-only-Transfergrundlage in Gate-Texten | Apps mit US-Cloud | "DPF + SCC (DPA) + TIA" formulieren — DPF politisch volatil |
| Audit testet Doku-/Checklisten-URLs statt der in der App verlinkten URLs → falscher 404-Blocker | Audit-Methodik | IMMER zuerst `grep https://` in strings.xml/Code, DANN WebFetch auf genau diese URLs; Doku-URLs nur als Sekundaerquelle |
| Fernabsatz-Pflichten (Widerruf, § 312j-Button) dem Entwickler zugerechnet, obwohl Google Commerce Ltd. Vertragspartner ist | Play-Billing-Apps | Play-ToS pruefen: Google ist Verkaeufer, holt Widerrufsverzicht bei digitalen Inhalten selbst ein (Abos: 14 Tage via Google); Entwickler-AGB muessen nur konsistent dazu sein |
| Interne Compliance-Artefakte (VVT/DSFA/TIA) mit App-Inhalten verwechselt | Solo-Devs | Erklaeren: interne Unternehmer-Dokumente, groesstenteils aus vorhandener DSE ableitbar (Speicherdauer-Tabelle → Loeschkonzept, Sicherheits-Sektion → TOMs) |

## Muster-Klauseln (Kernpunkte, mit Quelle — Anwalt formuliert final)

| Klausel | Bereich | Quelle | Stand |
|---|---|---|---|
| UK-Vertreter-Nennung in DSE ("Unser Vertreter im UK gem. Art. 27...") — nur falls UK aktiviert | UK | gdpr-info.eu/art-27 | 2026-07 |
| MHMDA: separat verlinkte Consumer-Health-Data-Policy + getrennte Opt-ins Erhebung/Weitergabe | USA/WA | RCW 19.373, eff.org | 2026-07 |
| Quebec: "Privacy Officer: [Name/Kontakt]" (kein QC-Sitz noetig) — falls CA aktiviert | Kanada | onetrust Law 25 | 2026-07 |
| SG: "Data Protection Officer: [Kontakt] (Sec. 11(3) PDPA)" in Notice | Singapur | verasafe | 2026-07 |
| Widerruf Abo: 14 Tage uneingeschraenkt + Wertersatz-Hinweis (§ 357a) statt Erloeschens-Fiktion | DE | BGB §§ 355-357a; BGH 27.03.2025 (UWG-Abmahnbarkeit) | 2026-07 |
