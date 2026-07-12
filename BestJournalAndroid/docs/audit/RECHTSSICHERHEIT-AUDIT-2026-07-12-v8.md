# Rechtssicherheits-Audit BestJournalAndroid — v8

**Datum:** 2026-07-12, 13:05 Uhr
**Skill-Stand:** 2026-05-17 (rechtssicherheit-Skill) + 7 Sonnet-5-Researcher mit Stand 12.07.2026
**Vorgänger:** v7 (2026-04-28) — dieser Bericht prüft App-Version 0.21.16 (versionCode 297, targetSdk 36)
**Methode:** Röntgen-Output (2026-06-10) + frischer Legal-Vollscan + gezielte Code-Verifikation aller v7-Punkte + 7 parallele Researcher (DE/EU, UK/US/CA/AU/NZ, Asien-A, Asien-B/Südasien, LATAM/MENA/TR/UA/ZA, Play-Policies, Abmahn-Trends)

---

## Disclaimer (PFLICHT)

Dieser Bericht ist eine **technische Prüfhilfe** und ersetzt **KEINE anwaltliche Beratung**.
Er markiert Lücken, Inkonsistenzen, Play-Policy-Risiken und typische Abmahn-Fallstricke —
er gibt **keine Garantie** für Rechtssicherheit oder Abmahnungssicherheit. Vor dem
Play-Store-Release MUSS ein **Fachanwalt für IT-Recht** die Rechtstexte, den Paywall-Flow
und die Widerrufs-Implementierung verbindlich prüfen.

---

## Scope und Annahmen

- **App/Package:** Best Journal / `com.bestjournal.app`, Version 0.21.16 (versionCode 297), minSdk 26, targetSdk 36
- **Zielmärkte:** weltweit, 27 Sprachen; Ausschluss-Liste siehe `play-store-metadata/country-exclusion.md` (+ neue Empfehlungen unten)
- **Monetarisierung:** Abo (Monat/Jahr) + Lifetime via Google Play Billing 7.1.1; 8-Tage-Trial; keine Werbung
- **Accounts:** Google Sign-In (optional), Firebase Auth; Account-Löschung in-app + Drive-Backup-Löschung
- **SDKs/Datenflüsse:** Firebase (Analytics opt-in, Remote Config, App Check, Auth, **Functions europe-west1 — NEU seit v7**, AI/Gemini), Groq (Cloud-Whisper, SCC-Text korrekt), Microsoft Edge-TTS (inoffizieller Endpoint — siehe C1), Google Drive (Backup in eigenes Nutzer-Drive), Gmail-API (Feedback + § 356a-Widerruf)
- **Sensible Daten:** Tagebuchinhalte = potenziell Art. 9 DSGVO (Gesundheit, Religion, Beziehungen); explizite Einwilligung via Privacy-Gates vorhanden
- **Feature-Gates:** Kein UGC, keine Ads, keine Kinder-Zielgruppe (13+), Health-Grenzfall Mood/Reflexion, KI ja (Gemini/Groq), Abo ja, kein Standort (seit v7 entfernt)

## Gesamtstatus (REVIDIERT v8.1 — 12.07.2026, nach Frank-Re-Check)

| Dimension | Wert |
|-----------|------|
| **Release-Empfehlung** | **3 HOCH-Punkte vor Submit beheben (C1 TTS, C3 Artefakte, E3 Trader-Telefon), dann TECHNISCH OK NACH ANWALTSPRÜFUNG** |
| 🔴 BLOCKER | **0** (E1 war ein Audit-Fehler — siehe Revision) |
| 🟠 HOCH | **3** (C1 Edge-TTS-AVV, C3 Compliance-Artefakte, E3 Trader-Telefonnummer) |
| 🟡 MITTEL | **6** (C2 DSE-Update, C4 MHMDA, E4 Billing v8, E5 Health-Declaration, Z1 Thailand, Z2 Japan) |
| 🟢 NIEDRIG | **8** (inkl. herabgestufter/erledigter Punkte) |
| Compliance-Reife | **Sehr hoch** — deutlich besser als in v8.0 dargestellt |

## REVISION v8.1 (12.07.2026) — Ergebnis der Nachprüfung mit Frank

| Finding | v8.0 | v8.1 | Begründung |
|---|---|---|---|
| **E1** Pflicht-URLs | 🔴 BLOCKER | ✅ **ENTKRÄFTET** (Rest: 🟢 Doku) | Die App verlinkt `https://pepsi1978.github.io/proggs/bestjournal/` (strings.xml:1475-1486) — Seite EXISTIERT mit Privacy/AGB/Impressum (de/en/ko) + vollständiger `account-deletion.html` (Löschwege, Fristen, Datenkategorien). Audit-Fehler: Ich hatte nur die veraltete URL aus `data-safety-checklist.md` getestet statt die echten App-Links. Checkliste ist jetzt korrigiert. Quelle der Seiten: `proggs/docs/bestjournal/`. |
| **D1** Widerrufsbelehrung | 🟠 HOCH | ✅ **ENTKRÄFTET** (ℹ️ Anwalt bestätigen) | Google-Play-ToS (Primärquelle, de-Fassung): Vertragspartner ist **Google Commerce Limited**; beim Kauf digitaler Inhalte (= Lifetime) holt GOOGLE den Widerrufsverzicht selbst ein ("…bestätigen, dass Sie folglich auf Ihr gesetzliches Widerrufsrecht verzichten"); bei Abos gewährt Google 14 Tage Widerruf. Die NB-Klausel Abschnitt 16 beschreibt also korrekt den realen Google-Kaufprozess; NB 5.5 (14 Tage für Abos) passt ebenfalls. Kein Widerspruch Text↔Realität. Empfehlung: Anwalt die Google-Commerce-Konstruktion einmal bestätigen lassen. |
| **D2** § 312j Button-Lösung | 🟡 MITTEL | ✅ **ENTKRÄFTET** (ℹ️) | Folgt aus D1: Bestellabschluss findet bei Google Commerce Ltd. statt — Button-Lösung/Pflichtinfos im Checkout sind Googles Verantwortung als Verkäufer. |
| **E2** Data-Safety-Checkliste | 🟠 HOCH | ✅ **BEHOBEN** (in dieser Session) | Kein Live-Fehler — die App hat kein Crashlytics, nur die Repo-Checkliste (Submit-Vorlage) war falsch. Crashlytics-Zeile + beide URLs am 12.07.2026 korrigiert. |
| **C1** Edge-TTS | 🟠 HOCH | 🟠 **BESTÄTIGT, präzisiert** | Transparenz ist DA (DSE beschreibt MS-TTS mit Einwilligung + DPF + SCC) — Franks Einwand berechtigt. Verbleibender Kern: Die **SCC-Behauptung stimmt vertraglich nicht** (für den inoffiziellen `speech.platform.bing.com`-Consumer-Endpoint existiert kein Vertrag/AVV mit Microsoft) + Endpoint kann jederzeit gesperrt werden. Funktionserhaltender Fix, der Cloud-TTS BEHÄLT: auf offizielles **Azure Speech** umstellen (Free Tier 0,5 Mio. Zeichen/Monat; dann stimmen DPA/SCC wirklich; Gate-/DSE-Texte bleiben fast identisch). |
| **C2** DSE veraltet | 🟠 HOCH | 🟡 **MITTEL, bestätigt** | Firebase Functions verarbeitet NUR den Purchase-Token + Produkt-ID (Play-Abo-Verifikation, `functions/index.js`, EU-Region) — keine Inhalte, keine Analytics. Franks Einschätzung ("nichts Großartiges") stimmt weitgehend; trotzdem fehlt der Dienst in DSE (App + gehostete Version, beide Stand 20.04.). Kleiner Absatz + POLICY_VERSION-Bump reicht. |
| **C3** Compliance-Artefakte | 🟠 HOCH | 🟠 **BESTÄTIGT, erklärt** | VVT/DSFA/TIA sind INTERNE Unternehmer-Dokumente (nicht App-Inhalte) — können nicht "in der App drin" sein. Große Teile lassen sich aus der exzellenten DSE ableiten (Speicherdauer-Tabelle → Löschkonzept; Sicherheits-Sektion → TOMs). 1 Session Aufwand. Kein Abmahnrisiko, reines Behörden-Prüfrisiko. |
| **E3** Trader-Telefon | 🟠 HOCH | 🟠 **BESTÄTIGT** | Impressum ist vollständig (Telefon dort nicht nötig, EuGH). ABER: Play-Console-Trader-Verifikation (DSA Art. 30/31) verlangt separat Adresse + **Telefonnummer** + E-Mail; wird im Store-Listing angezeigt. Virtuelle Nummer vor Submit besorgen. |
| **C5** DPF-Absicherung | 🟡 MITTEL | 🟢 **NIEDRIG** | DSE enthält bereits 8× "Standardvertragsklauseln" (DPF+SCC-Doppelabsicherung textlich vorhanden). Nur die kurzen Gate-Texte (Gemini) könnten "+ SCC" ergänzen. |
| **D3** PAngV-Rabatte | 🟡 MITTEL | 🟢 **NIEDRIG** | Preise/Rabatte werden von Google Play als Verkäufer abgewickelt; In-App-Rabatt-CLAIMS bei künftigen Aktionen weiter beachten. |
| **Z3** Singapur DPO | 🟡 MITTEL | 🟢 **NIEDRIG** | DSE-Aussage "kein DSB erforderlich" ist für DE korrekt; SG-PDPA-DPO ist ein separates Konzept — 1 Satz in EN-Policy ergänzen. |
| C4, E4, E5, Z1, Z2 | 🟡 | 🟡 **unverändert** | MHMDA-Opt-ins, Billing v8 (31.08.2026), Health-Declaration/Org-Account-Frage, Thailand-Ausschluss, Japan-Rep — alle recherche-basiert bestätigt. |

**Lerneffekt für künftige Audits (in Wissensbasis übernommen):** (1) IMMER die tatsächlich in der App verlinkten URLs testen, nie nur Doku-Dateien. (2) Bei Play-Billing-Apps zuerst die Google-Commerce-Vertragskonstruktion prüfen, bevor Fernabsatz-Pflichten dem Entwickler zugerechnet werden.

**Kernaussage:** Die App hat ein überdurchschnittliches Compliance-Fundament (Consent-First-Architektur, Privacy-Gates pro Cloud-Dienst, § 356a-Widerrufsbutton, KI-Badges, saubere Werbeaussagen). Die gefährlichsten Punkte sind KEINE fehlenden Dokumente, sondern **Drift**: Der Code wurde weiterentwickelt (Paywall-Redesign, Firebase Functions), die Rechtstexte und Submit-Dokumente hinken hinterher.

---

## Befunde

### 🔴 BLOCKER

#### [E1] 🔴 Play-Store-Policy — Pflicht-URLs (Privacy Policy + Account-Löschung) sind nicht gehostet
- **Nachweis:** `https://pepsi1978.github.io/bestjournal-deletion/` → HTTP 404 (geprüft 12.07.2026). GitHub-Konto `Pepsi1978` hat nur das Repo `proggs`, keine GitHub-Pages-Site. `firebase.json` enthält nur `functions`, kein Hosting. `docs/account-deletion.html`, `docs/PRIVACY.en.html` etc. existieren nur im Repo.
- **Risiko:** Ohne erreichbare Privacy-Policy-URL wird das Store-Listing abgelehnt; ohne funktionierende Deletion-Web-URL (Pflicht, weil die App Google-Sign-In-Accounts anbietet) wird der Data-Safety-Abschnitt abgelehnt bzw. bei Falschangabe enforced (2025: >255k Apps gestoppt). Zusätzlich DE-Abmahnrisiko: fehlende öffentlich erreichbare Datenschutzerklärung ist Abmahn-Hotspot #2.
- **Fix:** GitHub-Pages-Repo (z.B. `bestjournal-legal`) oder Firebase Hosting aufsetzen; `docs/PRIVACY.en.html`, `DATENSCHUTZ.html`, `account-deletion.html` deployen; URLs in `data-safety-checklist.md` und Play Console eintragen. Aufwand: <1 Stunde.
- **Quelle:** support.google.com/googleplay/android-developer/answer/13327111 (Account Deletion, offiziell, 12.07.2026)

### 🟠 HOCH

#### [D1] 🟠 BGB/Widerruf — Widerrufsbelehrung behauptet ein Erlöschen, das nie eintritt
- **Nachweis:** `assets/legal/de/NUTZUNGSBEDINGUNGEN.html` Abschnitt 16: *"Wenn Sie im Kaufprozess (Google Play) der sofortigen Aktivierung der Premium-Funktionen zustimmen, erlischt Ihr Widerrufsrecht nach § 356 Abs. 5 BGB."* — Der v6/v7-Verzichts-Dialog (`paywall_consent_dialog_checkbox` + Button "Jetzt zahlungspflichtig abonnieren") wurde beim Paywall-Redesign **entfernt** (0 Treffer für "zahlungspflichtig" im gesamten Code/Strings; bestätigt r3-abo-Befund vom 11.06.). Weder App noch Google-Billing-Sheet holen die ausdrückliche Zustimmung + Kenntnisnahme-Bestätigung ein.
- **Risiko:** Fehlinformation über Verbraucherrechte. Ein Verbraucher könnte glauben, sein Widerrufsrecht sei mit dem Kauf erloschen. Seit BGH 27.03.2025 (I ZR 186/17 u.a.) sind Verbraucherinformations-Verstöße über § 5a UWG durch Mitbewerber UND Verbraucherverbände abmahnbar. Zudem dogmatisch schief: Bei Abos (dauerhafte digitale Dienstleistung) gilt § 356 Abs. 4, nicht Abs. 5.
- **Fix (Variante a, empfohlen):** Klausel ersetzen — Widerrufsrecht besteht 14 Tage uneingeschränkt (die App bietet mit dem § 356a-Button ohnehin aktiven Widerruf); Hinweis auf Wertersatz bei Nutzungsbeginn (§ 357a BGB). Wirtschaftliche Folge: 14-Tage-Erstattungsrisiko bleibt bewusst bestehen.
- **Fix (Variante b):** Verzichts-Dialog aus v7 wieder einbauen (Checkbox + Kenntnisnahme + "Jetzt zahlungspflichtig abonnieren") — löst auch D2 mit.
- **Anwaltsprüfung:** PFLICHT für die finale Formulierung.
- **Quelle:** wbs.legal zu BGH 27.03.2025; twobirds.com §312k/§356-Übersicht (12.07.2026)

#### [C1] 🟠 DSGVO — Edge-TTS nutzt inoffiziellen Microsoft-Endpoint ohne Auftragsverarbeitungsvertrag
- **Nachweis:** `EdgeTtsPlayer.kt:98` → `wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1` (Consumer-Endpoint des Edge-Browsers, kein API-Vertragsprodukt). `privacy_gate_tts_body` (strings.xml:630) stützt die Übermittlung auf Microsofts DPF-Zertifizierung.
- **Risiko:** Für diese Nutzung existiert kein AVV/DPA (Art. 28 DSGVO) — es werden Tagebuch-TEXTE (potenziell Art. 9) an Microsoft übermittelt, ohne Vertragsverhältnis; Microsofts DPF-Commitment deckt die inoffizielle Endpoint-Nutzung kaum. Zusätzlich ToS-Bruch-Risiko → Microsoft kann den Endpoint jederzeit sperren (Funktionsausfall).
- **Fix:** Auf offizielles Azure Speech (mit Microsoft-DPA + echtem DPF/SCC-Rahmen) umstellen ODER On-Device-TTS als Default und Cloud-TTS entfernen ODER (Übergang) Gate-Text auf Art. 49 Abs. 1 lit. a (ausdrückliche Einwilligung nach Risikoaufklärung) umstellen — Anwalt entscheiden lassen.
- **Quelle:** Art. 28/44 ff. DSGVO; Researcher 1 (DPF-Lage nach Trump v. Slaughter, 29.06.2026)

#### [C2] 🟠 DSGVO — Datenschutzerklärung (Stand 20.04.2026) hinkt dem Code hinterher
- **Nachweis:** DSE erwähnt Firebase **Functions** (SubscriptionStatusService, `FirebaseModule.kt:51`, europe-west1 — Abo-Verifikations-Backend, neu seit v7) mit keinem Wort (0 Grep-Treffer). TTS-Gate-Text wurde geändert, DSE-Abschnitt 5.2a nicht gegengeprüft.
- **Risiko:** Art.-13-Transparenzlücke; Widerspruch DSE ↔ tatsächliche Verarbeitung ist als § 5a-UWG-Verstoß abmahnbar (Abmahn-Hotspot #4).
- **Fix:** DSE-Abschnitt für Firebase Functions ergänzen (Zweck: Abo-Status-Verifikation, Region EU, Rechtsgrundlage Art. 6 Abs. 1 lit. b/f), TTS-Abschnitt angleichen, Stand-Datum aktualisieren, `CURRENT_POLICY_VERSION` (Constants.kt:220, aktuell "3.1") bumpen → Re-Consent. Alle 27 Sprachfassungen nachziehen (uebersetzung-Skill).

#### [C3] 🟠 DSGVO — Interne Compliance-Artefakte fehlen als echte Dokumente
- **Nachweis:** Kein VVT/ROPA, keine DSFA, keine TIA, keine TOMs-Doku, keine AVV-Liste, kein Löschkonzept, kein Datenpannen-Meldeplan, keine AI-Act-Risikoklassifizierung im Repo auffindbar. (Das Skill-Skript meldete fälschlich "alle vorhanden" — es matchte Keywords in den alten Audit-Berichten. DSA-Kontaktstelle ✓ vorhanden im Impressum.)
- **Risiko:** Kein Abmahnrisiko, aber Behörden-Prüfrisiko: VVT ist Pflicht (Art. 30 — die Kleinbetriebs-Ausnahme greift NICHT, weil Art.-9-Daten nicht nur gelegentlich verarbeitet werden); DSFA bei KI-Analyse sensibler Daten (Art. 35) sehr wahrscheinlich Pflicht; TIA für US-Transfers (Groq-SCCs) nötig — besonders seit das DPF wackelt (US Supreme Court "Trump v. Slaughter", 29.06.2026, untergräbt FTC-Aufsicht).
- **Fix:** Einmalig 8 kompakte Dokumente unter `docs/compliance/` anlegen (VVT, DSFA, TIA, TOMs, AVV-Liste [Google Cloud DPA, Groq DPA, Status Microsoft = offen siehe C1], Löschkonzept, Datenpannen-Plan mit 72h-Ablauf, AI-Act-Art.-50-Klassifizierung "limited risk"). Aufwand: 1 Session.

#### [E2] 🟠 Play-Store-Policy — Data-Safety-Checkliste enthält Falschangaben
- **Nachweis:** `play-store-metadata/data-safety-checklist.md` Abschnitt 1.4 deklariert "Crash logs: Yes (Firebase Crashlytics)" — **Crashlytics ist nicht eingebaut** (kein Eintrag in `gradle/libs.versions.toml`, Röntgen bestätigt). Deletion-URL in Abschnitt 5 inkonsistent (GitHub-README vs. mailto vs. 404-Pages-URL).
- **Risiko:** Data-Safety-Falschdeklaration ist Policy-Verstoß → Nachbesserung/Removal. Google gleicht die Angaben automatisiert mit echtem Netzwerkverkehr ab.
- **Fix:** Checkliste korrigieren: Crash logs = No; Deletion-URL auf die neue gehostete URL (E1) vereinheitlichen; Groq-Audio-Deklaration als "service provider, ephemeral processing" mit Anwalt/Play-Doku gegenprüfen [Klärung nötig]; Firebase Functions ergänzt keine neue Kategorie (nur Purchase-Token).

#### [E3] 🟠 Play-Store-Policy — DSA-Trader-Status: veröffentlichbare Telefonnummer fehlt
- **Nachweis:** Bezahl-App eines gewerblichen Einzelentwicklers = **Trader** (Pflicht-Deklaration seit 17.02.2025, bei Neu-Einreichung erzwungen). Als Trader werden Anschrift + **Telefonnummer** + E-Mail im Store-Listing veröffentlicht. Das Impressum (c/o Impressumservice, Hungen) hat bewusst keine Telefonnummer.
- **Risiko:** Ohne Trader-Deklaration keine EU-Verbreitung/Updates; private Telefonnummer würde öffentlich.
- **Fix:** Virtuelle Rufnummer (z.B. sipgate, ~5 EUR/Monat) oder Impressumservice-Zusatzleistung besorgen und in Play Console + Impressum verwenden. Vor dem ersten Submit erledigen.
- **Quelle:** verasafe.com DSA-Trader-Classification; Play Console Hilfe (12.07.2026)

### 🟡 MITTEL

#### [D2] 🟡 BGB — § 312j-Button-Lösung ruht vollständig auf dem Google-Billing-Sheet
- Kein eigener finaler Bestellschritt mehr in der App (v6/v7 hatten "Jetzt zahlungspflichtig abonnieren"-Dialog, wurde entfernt; `BillingManager.kt:1152/1265` startet das Sheet direkt). Google-Sheet zeigt Preis/Laufzeit + "Abonnieren". Das ist Marktstandard, aber deutsche Rechtsprechung zur Button-Lösung bei In-App-Käufen ist nicht höchstrichterlich geklärt (OLG Düsseldorf hielt "Abonnieren" allein für unzureichend). **Anwalt fragen; Variante D1-b löst das mit.**

#### [D3] 🟡 UWG/PAngV — Rabatt-Darstellungen (Exit-Offer "2 Monate zum halben Preis", Churn-Angebote)
- § 11 PAngV (30-Tage-Bestpreis) gilt bei Preisermäßigungen. Solange der Referenzpreis der dauerhafte Play-Regulärpreis ist, unkritisch — bei künftigen Preisaktionen dokumentieren. Formulierungen VOR Store-Launch einmal mit Anwalt durchgehen.

#### [E4] 🟡 Play — Billing Library 7.1.1 → v8 Pflicht ab 31.08.2026
- `gradle/libs.versions.toml`: playBilling = "7.1.1". Neue Einreichungen/Updates brauchen ab 31.08.2026 v8 (Verlängerung bis 01.11.2026 beantragbar). Upgrade einplanen (Breaking Changes in v8 beachten).

#### [E5] 🟡 Play — Health-Apps-Declaration + mögliche Organization-Account-Pflicht [Klärung nötig]
- Mood-/Wellness-Framing → Kategorie "Stress Management, Relaxation, Mental Acuity" (Vorlage `docs/health-apps-declaration.md` ✓). **Neu Jan 2026:** Health-Apps können auf verifizierte **Organization-Accounts** beschränkt sein — Reichweite für die Wellness-Kategorie unklar. **clarificationQuestion:** Beim Submit prüfen, ob die Console bei "Stress Management" einen Organization-Account verlangt; falls ja: Health-Framing minimieren (Kategorie "Lifestyle/Productivity") oder D-U-N-S-Weg gehen.

#### [C4] 🟡 USA/Washington MHMDA — Minimallösung vorhanden, strenge Lesart verlangt mehr
- EN-Policy hat MHMDA/MODPA-Sektion mit Opt-in-Hinweis + Ausübungsweg ✓. Strenge Lesart verlangt eine **separat verlinkte** Consumer-Health-Data-Policy und **getrennte** Opt-ins für Erhebung und Weitergabe (Privacy-Gates holen eine kombinierte Zustimmung). Wegen Private Right of Action: separaten Anker-Link "Consumer Health Data Privacy Policy" + zweistufiges Häkchen im Gate erwägen.

#### [C5] 🟡 DPF-Absicherung — Gates/DSE stützen Google-Dienste auf das wackelnde DPF
- `privacy_gate_gemini_body`/`privacy_gate_tts_body` nennen das EU-US DPF als Absicherung. Nach "Trump v. Slaughter" (29.06.2026) + anhängiger EuGH-Berufung (C-703/25 P): Texte auf "DPF, zusätzlich abgesichert durch EU-Standardvertragsklauseln (Google Cloud DPA)" erweitern + TIA dokumentieren (→ C3).

#### [Z1] 🟡 Thailand — Vertreter-Pflicht (Sec. 37(5) PDPA) + Enforcement-Wende 08/2025
- v7 gab Thailand frei ("Englisch reicht") — die Recherche zeigt: ausländische Controller brauchen einen **Vertreter in Thailand**, PDPC verhängte am 01.08.2025 acht Bußgelder an einem Tag. **Empfehlung: TH beim Initial-Release ausschließen** (Country-Exclusion-Liste ergänzen), wie UK/TR/KR/VN/SA/BR.

#### [Z2] 🟡 Japan — Domestic-Representative-Erwartung (APPI)
- Auslands-Anbieter sollen einen Vertreter in Japan benennen; japanische Policy ✓ vorhanden. Enforcement gegen Solo-Devs selten. Optionen: Risiko dokumentiert akzeptieren (v7-Linie) oder JP ausschließen. Anwalts-/Geschäftsentscheidung.

#### [Z3] 🟡 Singapur — DPO-Kontakt veröffentlichen
- PDPA verlangt DPO ohne Schwelle; Fix ist billig: Frank als DPO benennen + Kontakt in Privacy Notice (EN) aufnehmen.

### 🟢 NIEDRIG

- **[Z4]** Log-Hygiene: `TranscriptionRepository.kt:153` loggt bis zu 40 Zeichen VERWORFENER Transkript-Segmente via Log.d — in Release-Builds per R8 `-assumenosideeffects` strippen.
- **[Z5]** Key-Altlast `settings_premium_feature_5_perspectives` (Text sagt korrekt "Alle 4") — bei Gelegenheit umbenennen.
- **[C6]** Indien DPDP: Grievance-Officer-Kontakt (dev.app.support@gmail.com) explizit in Policy ausweisen; substanzielle Pflichten erst ab 13.05.2027 — Wiedervorlage.

### ℹ️ INFO

- Play AI-Content-Policy: Best Journal fällt sehr wahrscheinlich unter die Productivity-Ausnahme (KI verbessert bestehende Features, kein zentraler Chatbot); Report-Button ist trotzdem vorhanden ✓.
- BFSG: Kleinstunternehmer-Ausnahme (<10 MA, <2 Mio EUR) greift für Einzelentwickler-Dienstleistung → Status intern dokumentieren (C3-Artefakte).
- COPPA: general audience, 13+ konsistent in allen Texten ✓.
- US-State-Laws (CCPA & Co.): Schwellen (25k-100k Nutzer) werden nicht erreicht; CalOPPA-Policy-Pflicht ist durch gehostete Policy (E1) erfüllt.
- Australien: Small-Business-Exemption aktiv; Statutory Tort (06/2025) gilt trotzdem → sichere Datenhaltung ist der Schutz (vorhanden).
- Play-Testing: Falls der Personal-Developer-Account nach 13.11.2023 erstellt wurde: 12 Tester / 14 Tage Closed Test vor Production-Zugang.
- Push-Notifications: nur funktionale Reminder (keine Werbe-Pushes) → § 7 UWG unkritisch. Bei künftigen Marketing-Pushes: separater Info-Screen + Einwilligung.
- Kündigungspfad: login-freier Play-Abos-Link in Settings + ChurnFlow ✓ (OLG Köln 6 U 62/24 konform).

---

## Dokumentenmatrix

| Dokument | In App | Store/Web | Inhalt OK | Sprache OK | Befund |
|---|---:|---:|---:|---:|---|
| Datenschutzerklärung | ✅ 27 Locales, WebView | ❌ nicht gehostet | 🟡 Functions fehlt (C2) | ✅ 27 | E1, C2 |
| Nutzungsbedingungen | ✅ 27 Locales | ❌ nicht gehostet | 🟠 Widerrufsklausel (D1) | ✅ 27 | D1 |
| Impressum | ✅ 27 Locales | ❌ nicht gehostet | ✅ DDG vollständig + DSA Art. 11 | ✅ 27 | E3 (Telefon für Trader) |
| Widerruf | ✅ NB §16 + § 356a-Button | — | 🟠 D1 | ✅ | D1 |
| Account-/Datenlöschung | ✅ In-App vollständig | ❌ Web-URL 404 | ✅ | ✅ | E1 |

## Interne Compliance-Artefakte

| Dokument | Vorhanden | Befund |
|---|---:|---|
| VVT/ROPA (Art. 30) | ❌ | C3 — Pflicht (Art.-9-Daten regelmäßig) |
| DSFA/DPIA (Art. 35) | ❌ | C3 — bei KI + Art. 9 sehr wahrscheinlich Pflicht |
| TIA (Art. 44 ff.) | ❌ | C3 — für Groq/US-SCCs nötig, DPF wackelt |
| TOMs (Art. 32) | ❌ | C3 |
| AVV-Liste (Art. 28) | ❌ | C3 — Google DPA ✓ implizit, Groq DPA prüfen, Microsoft = C1 |
| SCCs/DPF | 🟡 | In Texten behauptet; Verträge in AVV-Liste nachweisen |
| Löschkonzept | ❌ | C3 — technisch implementiert, nicht dokumentiert |
| Datenpannen-Plan (72h) | ❌ | C3 |
| AI-Risikoklassifizierung | ❌ | C3 — Art. 50 "limited risk" dokumentieren |
| DSA-Kontaktstelle (Art. 11) | ✅ | Impressum ✓ |

## Code-vs-Text-vs-Play-Matrix (Delta zu v7 — nur Änderungen)

| Daten/Feature | Code | DSE | Data Safety | Consent/UI | Status v8 |
|---|---|---|---|---|---|
| Firebase Functions (Abo-Verify) | ✅ NEU `SubscriptionStatusService` | ❌ fehlt | 🟡 nur Purchase-Token | — | 🟠 C2 |
| Standort | ❌ entfernt | ✅ entfernt | entfällt | entfällt | ✅ konsistent bereinigt |
| Sofortleistungs-Verzicht | ❌ entfernt | NB behauptet ihn noch | — | ❌ | 🟠 D1 |
| Crashlytics | ❌ nie eingebaut | ✅ nicht erwähnt | ❌ Checkliste sagt Ja | — | 🟠 E2 |
| KI-Badge EntryDetail/FollowUp | ✅ NEU (Z. 343, 436, 213) | ✅ 12a | — | ✅ | ✅ v7-H1 GEFIXT |
| Groq-Transfergrundlage | — | ✅ SCC (DPF-Claim entfernt) | ✅ | ✅ Gate | ✅ GEFIXT |
| KI-Limits-Werbung | 150/Tag `AiRateLimiter` | — | — | ✅ "bis zu 150/Tag" transparent | ✅ GEFIXT |

## Werbeaussagen-vs-Feature-Matrix (UWG § 5/§ 5a, aus Röntgen + frischen Strings)

| Werbeaussage | Quelle | Code-Realität | Stimmt? | Befund |
|---|---|---|---:|---|
| "Mehr Klarheit im Alltag" u.ä. Headlines | Paywall | neutral-reflektiv, keine Wirkversprechen | ✅ | HWG-sauber |
| "Unbegrenzte Nachträge" | Paywall/Settings | manuelle Nachträge ohne Limit | ✅ | ok |
| "Alle Wochenrückblicke … im Rahmen deines KI-Tageskontingents" | Paywall | Limit transparent | ✅ | vorbildlich |
| "bis zu 150 pro Tag" | KI-Limit-Dialog | `AiRateLimiter` 150/Tag | ✅ | ok |
| "Kostenlos starten" (Trial) | Onboarding/Paywall | 8-Tage-Trial-Timeline sichtbar, Preis im Play-Sheet | 🟡 | D2 (Anwalt) |
| "Deine Einträge bleiben auf dem Gerät; KI-Anfragen verschlüsselt" | Onboarding | Local-first + Opt-in-Cloud | ✅ | ehrlich |
| Store-Listing-Texte | — | nicht im Repo | — | vor Launch nach denselben Regeln schreiben |

## Android-Sicherheitscheck

| Kontrolle | Ergebnis |
|---|---|
| Permissions minimal | ✅ 6, alle zweckgebunden, Standort entfernt |
| Backup-Regeln | ✅ sensitive Prefs + regenerierbare DBs ausgeschlossen, DSE legt Journal-Backup offen |
| TLS/Cleartext | ✅ cleartext verboten, System-Trust-Anchors |
| Sensitive Logs | 🟢 Z4 (40-Zeichen-Fragmente verworfener Segmente) |
| Secrets im Repo | ✅ SK-Ordner + Remote Config |
| Exported Components | ✅ nur MainActivity + BOOT-Receiver (systembedingt) |
| WebView | ✅ nur lokale Assets (LegalDocumentScreen) |

## Jurisdiktions-Gates (v8)

| Rechtsraum | Bewertung | Release-Blocker? |
|---|---|---|
| **DE/EU** | Nach D1+C1+C2+E3-Fix: gut. AI Act Art. 50 ab 02.08.2026 erfüllt (Badges + DSE 12a) | E1/E3 vor Submit |
| **UK** | **AUSGESCHLOSSEN** (Franks Entscheidung 28.04., Skript bestätigt Option B; DUAA 2025 ändert Art. 27 NICHT) | Nein (wenn raus) |
| **USA** | Gut; MHMDA-Sektion ✓, C4 empfohlen; State-Law-Schwellen unterschritten | Nein |
| **Kanada** | **AUSGESCHLOSSEN** (Option A, Quebec) — Wiedervorlage geplant | Nein |
| **Australien/NZ** | OK (Small-Business-Exemption; IPP 3A nicht einschlägig) | Nein |
| **Japan** | 🟡 Z2 Domestic-Rep-Frage | Nein (Entscheidung) |
| **Korea** | **AUSGESCHLOSSEN** (PIPA + AI Basic Act 01/2026 bestätigen) | Nein |
| **Indien** | OK bis 05/2027 (dann Grievance-Officer/Consent-Manager-Pflichten); Sprachen ✓ | Nein |
| **Thailand** | 🟡 Z1 NEU: **Ausschluss empfohlen** (Vertreter-Pflicht) | Nein (wenn raus) |
| **Indonesien** | OK (id-Policy ✓, Behörde nicht operativ) | Nein |
| **Vietnam** | **AUSGESCHLOSSEN** (PDPL 2026 DPIA-Dossier bestätigt) | Nein |
| **Sri Lanka/Pakistan/Bangladesch** | OK/legal vacuum (PK) — Englisch genügt, Enforcement minimal | Nein |
| **Türkei** | **AUSGESCHLOSSEN** (VERBIS + SCC-Meldepflicht bestätigt; Bußgelder 2026 +25%) | Nein |
| **Ukraine** | OK (uk-Locale + Rechtstexte ✓; Reform 8153 noch nicht Gesetz) | Nein |
| **Brasilien** | **AUSGESCHLOSSEN** (ANPD-SCC seit 23.08.2025; pt-BR-Texte ✓ vorhanden — Wiedervorlage möglich) | Nein |
| **Mexiko** | OK (es ✓; DSE-Verweis auf Nachfolgebehörde der INAI prüfen) | Nein |
| **Argentinien** | 🟢 RNBD-Registrierung formal offen (Enforcement gegen Kleine selten) | Nein |
| **Saudi/VAE** | SA **AUSGESCHLOSSEN**; VAE OK (ar ✓) | Nein |
| **Südafrika** | 🟢 Information-Officer-Registrierung formal offen | Nein |
| **Singapur** | 🟡 Z3 DPO-Kontakt ergänzen | Nein |
| **China/Russland/Belarus** | **AUSGESCHLOSSEN** (Programmumgebungs-Vorgabe/Sanktionen) | — |

## Play-Console-Checkliste (vor Submit)

- [ ] **E1**: Privacy-Policy-URL + Deletion-URL hosten und eintragen
- [ ] **E2**: Data Safety exakt nach korrigierter Checkliste (ohne Crashlytics)
- [ ] **E3**: Trader-Status JA + veröffentlichbare Telefonnummer
- [ ] **E5**: Health-Declaration "Stress Management" (Org-Account-Frage klären)
- [ ] Country-Exclusion setzen: UK, CA, KR, TR, VN, SA, BR, CN, RU, BY (+ **TH neu**, JP nach Entscheidung)
- [ ] AI-Declaration: Productivity-Ausnahme dokumentieren, Report-Button ✓
- [ ] Content-Rating (IARC), App Access (Testkonto für Review), Ads = No
- [ ] Testing-Anforderung prüfen (12 Tester/14 Tage bei neuem Personal-Account)

## Fix-Reihenfolge

1. **E1** URLs hosten (BLOCKER, <1 Std)
2. **D1** Widerrufsklausel korrigieren ODER Verzichts-Dialog reaktivieren (+ D2 mitlösen) — mit Anwalt
3. **C2** DSE aktualisieren (Functions, TTS, DPF→+SCC C5) + POLICY_VERSION-Bump + 27 Sprachen
4. **C1** Edge-TTS-Entscheidung (Azure/on-device/Anwalt)
5. **E2+E3** Submit-Dokumente korrigieren, Telefonnummer besorgen
6. **C3** Compliance-Artefakte-Session (VVT, DSFA, TIA, TOMs, AVV, Löschkonzept, Datenpannen-Plan, AI-Doku)
7. **E4** Billing v8 vor 31.08.2026
8. **Z1-Z3, C4, C6** vor/kurz nach Rollout-Erweiterung
9. **Anwaltsprüfung** DE+EN-Texte + Paywall-Flow + Widerruf — **PFLICHT vor Release**

## Quellen (v8-Researcher, Abruf 12.07.2026)

Siehe vollständiges Quellenregister in `tools/rechtssicherheit.md` (Wissensbasis). Kernquellen:
- support.google.com/googleplay/android-developer/answer/10787469, 11150561, 13327111, 14094294, 14738291, 9900533, 14115180 (Play, offiziell)
- developer.android.com/google/play/requirements/target-sdk (offiziell)
- BGH 27.03.2025 I ZR 186/17 (DSGVO via UWG abmahnbar); EuGH C-21/23 "Lindenapotheke"; BGH 22.05.2025 I ZR 161/24 (§312k); OLG Köln 6 U 62/24; VG Hannover 10 A 5385/22
- artificialintelligenceact.eu/article/50 (AI Act, 02.08.2026; GenAI-Übergang 02.12.2026)
- dataprivacyframework.gov + btlj.org (DPF nach Trump v. Slaughter 29.06.2026)
- RCW 19.373 (WA MHMDA); ico.org.uk (DUAA 2025); pdpathailand.com Sec. 37; DFDL/Tilleke (VN PDPL 91/2025)

---

## Abschluss-Disclaimer

**Dieser Bericht ist eine technische Prüfhilfe und ersetzt KEINE anwaltliche Beratung.**
Ohne Prüfung durch einen Fachanwalt für IT-Recht ist der Release nicht zu empfehlen.
Mit behobenem BLOCKER (E1), den 6 HOCH-Punkten und anwaltlicher Freigabe ist die App aus
technischer Sicht in einem überdurchschnittlich guten Compliance-Zustand — es wurden
keine strukturellen Abmahn-Fallen gefunden, die über die genannten Befunde hinausgehen.

*Audit-Stand: 12.07.2026 v8 | nächste Pflicht-Iteration: nach D1/C1/C2-Fixes bzw. vor Play-Store-Submit*
