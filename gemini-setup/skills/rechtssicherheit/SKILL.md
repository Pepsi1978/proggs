---
name: rechtssicherheit
description: >
  Prueft Android-Apps vor Release als technische Pruefhilfe (KEINE anwaltliche Beratung)
  auf Abmahn-, Datenschutz-, Impressums-, Nutzungsbedingungen-, Widerrufs-, Google-Play-
  Data-Safety- und App-Sicherheitsrisiken. Vollscan der Codestruktur, Manifest-/Permissions-
  /SDK-/Backup-/Logs-/Secrets-Pruefung, Abgleich Code vs. Rechtstexte vs. Play Console.
  Recherchiert parallel (5 Researcher) aktuelle Pflichtangaben fuer DSGVO/GDPR (EU),
  DDG, TDDDG/ePrivacy, BFSG/EAA, DSA, AI Act, BGB/EGBGB Widerruf,
  CCPA/CPRA (USA), COPPA, FTC Act, Health Breach/HIPAA/FDA,
  UK-GDPR/PECR/Online Safety Act, PIPEDA/Quebec Law 25, Australia Privacy Act/APPs,
  PIPL (China), DPDP (Indien), APPI (Japan), PIPA (Korea), LGPD (Brasilien),
  Singapore PDPA, POPIA (Suedafrika), Saudi/UAE PDPL,
  Google Play Data Safety, User Data Policy, Account Deletion, Permissions,
  Health/Families/Ads/AI/UGC/Payments-Special-Policies, OWASP MASVS/MASTG.
  Gleicht Befunde gegen bestehende App-Dokumente (Datenschutzerklaerung, Nutzungs-
  bedingungen, Impressum, Widerrufsbelehrung, Account-/Datenloeschung) und deren
  Platzierung/Uebersetzung in der App und im Store ab.
  Erstellt/pflegt `<WORKSPACE_ROOT>/tools/rechtssicherheit.md` als workspace-lokale
  Wissensbasis (plattformneutral, keine harten Home-Pfade).
  Deutsche Trigger: "starte den Skill Rechtssicherheit", "pruefe [App] auf Rechtssicherheit",
  "Rechtssicherheit fuer [App]", "DSGVO-Check fuer [App]", "ist [App] abmahnungssicher",
  "Abmahnungscheck [App]", "Play-Store rechtskonform pruefen", "Rechts-Audit [App]",
  "Data-Safety-Check", "Account-Loeschung pruefen", "Widerruf pruefen", "Impressum pruefen",
  "AGB pruefen", "Datenschutz pruefen", "Consent pruefen", "Tracking pruefen",
  "SDKs pruefen", "Permissions pruefen", "Play Console pruefen", "Google Play Data Safety",
  "Android-App-Release", "rechtliche Pflichttexte", "Datenschutzerklaerung pruefen",
  "Nutzungsbedingungen pruefen".
  Nutze diesen Skill immer wenn es um Datenschutz-Compliance, Abmahnungsrisiken, Play-
  Store-Release oder rechtliche Pflichttexte einer App geht — auch wenn der Benutzer den
  Skill-Namen nicht explizit nennt, aber von Datenschutzerklaerung, Impressum, AGB,
  Widerruf, Consent, Account-Loeschung, Data Safety, Permissions, SDKs, Tracking,
  Ads, Analytics, Crashlytics, KI/GenAI, UGC, Kinder, Health, Barrierefreiheit oder
  DSGVO-Konformitaet spricht.
invocation: user
---

# Skill: Rechtssicherheit

> **Wichtiger Disclaimer (PFLICHT — am Anfang UND am Ende des Berichts wiederholen):**
> Dieser Skill ist eine **technische Pruefhilfe** und ersetzt **KEINE anwaltliche Beratung**.
> Er kann Luecken, Inkonsistenzen, Play-Policy-Risiken und typische Abmahn-Fallstricke
> markieren — er gibt **keine Garantie** fuer Rechtssicherheit oder Abmahnungssicherheit.
> Fuer eine verbindliche Rechtspruefung MUSS vor Release ein **Fachanwalt fuer IT-Recht**
> konsultiert werden.

---

## Grundsatz: Keine Garantien

**Verbotene Formulierungen:**

- ❌ "Die App ist rechtssicher."
- ❌ "Die App ist 100% abmahnungssicher."
- ❌ "Dieser Text reicht rechtlich aus."
- ❌ "Du kannst (jetzt) bedenkenlos releasen."

**Erlaubte Formulierungen:**

- ✅ "Technisch wurden keine offensichtlichen Luecken in den geprueften Dateien gefunden."
- ✅ "Release aus technischer Sicht nur nach anwaltlicher Pruefung empfohlen."
- ✅ "Release blockieren, bis dieser Punkt korrigiert und juristisch geprueft ist."
- ✅ "Die folgenden Pflichtangaben fehlen oder sind unvollstaendig: ..."

---

## Ziel

Eine Android-App vor Release so pruefen, dass sie technisch, dokumentarisch und
in der Play-Store-Deklaration so weit wie pruefbar abmahnungsresistent ist. Der
Skill prueft nicht nur Rechtstexte, sondern auch ob die App technisch das tut,
was Datenschutz, Nutzungsbedingungen, Impressum, Widerrufsbelehrung und Google
Play Data Safety behaupten.

**Pflichtziele:**

1. Pflichtdokumente vorhanden: Datenschutzerklaerung, Nutzungsbedingungen, Impressum/
   Anbieterkennzeichnung, Widerruf bei kostenpflichtigen digitalen Inhalten,
   Support/Kontakt, Account-/Datenloeschung wenn Accounts existieren.
2. Pflichtdokumente enthalten die noetigen Angaben fuer Zielmaerkte und Features.
3. App verlinkt diese Dokumente korrekt: Store Listing, Onboarding, Consent,
   Settings, About, Account deletion, Paywall/Checkout.
4. Google Play Data Safety, Play-Console-Deklarationen, Manifest-Permissions, SDKs,
   Netzwerkverhalten und Rechtstexte sind konsistent.
5. Sicherheits- und Datenschutztechnik reduziert rechtliche Risiken: minimale
   Permissions, keine unnoetige Datenerhebung, sichere Speicherung, Backup-Regeln,
   TLS, keine Secrets im Repo, keine sensiblen Logs.
6. Alle relevanten Sprachen/Locales sind abgedeckt oder als Release-Blocker markiert.
7. Der Bericht trennt klar zwischen "rechtlich verbindlich durch Anwalt klaeren" und
   "technisch im Repo nachweisbar".

---

## Trigger und Pruefumfang (KRITISCH)

Nutze diesen Skill immer, wenn es um Datenschutz-Compliance, Abmahnungsrisiken,
Play-Store-Release oder rechtliche Pflichttexte einer Android-App geht, auch
wenn der Benutzer den Skill-Namen nicht explizit nennt.

**Deutsche Trigger:**

- "starte den Skill Rechtssicherheit", "Rechtssicherheit fuer [App]"
- "pruefe [App] auf Rechtssicherheit", "Rechts-Audit [App]"
- "DSGVO-Check fuer [App]", "Datenschutz pruefen"
- "ist [App] abmahnungssicher", "Abmahnungscheck [App]"
- "Play-Store rechtskonform pruefen", "Play Console pruefen"
- "Data-Safety-Check", "Google Play Data Safety"
- "Account-Loeschung pruefen", "Widerruf pruefen", "Impressum pruefen"
- "AGB pruefen", "Nutzungsbedingungen pruefen", "Datenschutzerklaerung pruefen"
- "Consent pruefen", "Tracking pruefen", "SDKs pruefen", "Permissions pruefen"
- "Android-App-Release", "rechtliche Pflichttexte"

**Auch ohne exakten Trigger verwenden bei Themen wie:**

- Datenschutzerklaerung, DSGVO/GDPR, CCPA/CPRA, UK-GDPR, PIPL, DPDP, APPI, PIPA,
  LGPD, POPIA, PDPA, PDPL oder anderen Datenschutzpflichten.
- Impressum/Anbieterkennzeichnung, AGB/Nutzungsbedingungen, Widerrufsbelehrung,
  Consent, Account-/Datenloeschung, Support/Kontakt oder Pflichtlinks in der App.
- Google Play Data Safety, Play-Console-Deklarationen, Permissions, SDKs,
  Tracking, Ads, Analytics, Crashlytics, KI/GenAI-APIs, Backups, Logs oder Secrets.
- DSA, AI Act, BFSG/EAA, TDDDG/ePrivacy, COPPA, Online Safety Act, Health, Kinder,
  UGC/Moderation, Barrierefreiheit, Cross-Border-Transfer, Abos/IAP/Refund.

**Pflicht-Pruefumfang (Android-Apps vor Release):**

- Datenschutz/DSGVO/GDPR und internationale Aequivalente
- Google Play Data Safety, User Data Policy, Spezial-Policies
- Permissions, SDKs, Datenfluesse, Drittlandtransfers
- Consent, Tracking, Ads, Analytics, Crashlytics
- Account-Erstellung und Account-/Datenloeschung
- Impressum/Anbieterkennzeichnung
- AGB/Nutzungsbedingungen
- Widerruf bei digitalen Inhalten, IAP und Abos
- internationale Zielmaerkte und Sprachpflichten
- Barrierefreiheit (BFSG/EAA, WCAG, EN 301 549) bei Legal-/Checkout-/Account-Flows

---

## Plattformneutrale Pfade (KRITISCH)

Die Wissensbasis liegt **workspace-lokal**, nicht in einem persoenlichen Home-Unterordner.

**Zielpfad (plattformneutral):** `<WORKSPACE_ROOT>/tools/rechtssicherheit.md`

`<WORKSPACE_ROOT>` ist das aktuelle Arbeitsverzeichnis bzw. der Repo-Root, in dem
die zu pruefende App liegt. Beispiele fuer die Aufloesung:

| Plattform   | Beispielpfad                                       |
|-------------|----------------------------------------------------|
| Windows     | `%USERPROFILE%\proggs\tools\rechtssicherheit.md`   |
| macOS/Linux | `$HOME/proggs/tools/rechtssicherheit.md`           |
| Generisch   | `<repo-root>/tools/rechtssicherheit.md`            |

**Verboten:** Harte Pfade wie `C:\Users\...`, `/Users/barwa/...` oder Bezuege auf
private Home-Unterordner als Standard. `~/Codex/`, `~/proggs/` oder andere
Home-Unterordner duerfen nicht als Default-Arbeitsverzeichnis verwendet werden,
wenn ein aktueller Workspace/Repo-Root verfuegbar ist.

**Wenn der Benutzer explizit eine Datei ausserhalb des Workspaces nennt:** Diese
konkrete Datei darf gelesen/geschrieben werden. Sonst bleibt der Skill im aktuellen
Workspace.

---

## Aktueller Recherche-Stand und Quellenprioritaet

**Skill-Stand: 2026-04-28.**

Bei jeder echten App-Pruefung aktuelle Quellen erneut pruefen, wenn:

- die letzte Recherche aelter als 30 Tage ist,
- Google Play Policies betroffen sind,
- Health, Kinder, Standort, Kontakte, Medien, SMS/Call Logs, Finanzdaten, KI/GenAI,
  Ads, Analytics, User Generated Content oder Accounts vorkommen,
- die App in neue Laender/Sprachen/SDKs/Monetarisierungsmodelle ausgerollt wird,
- der Benutzer "aktuell", "neueste" oder "Release" sagt.

### Quellenprioritaet (verbindliche Reihenfolge)

1. **Primaerquellen** — Gesetzestexte, offizielle Regulierer, Google/Android-Policy.
2. **Sekundaerquellen** — Fachanwaelte, Fachverbaende, Behoerden-Erklaerungen.
3. **News/Blogs** — nur als Hinweis, NIE alleinige Grundlage.

### Pflicht-Quellenklassen

**Plattform & Android:**

- Google Play Developer Policy Center / Play Console Help
- Android Developers Privacy & Security Dokumentation
- OWASP MASVS / MASTG (technische Mobile-Security-Kontrollen)

**EU/DE Recht:**

- EU-Kommission, GDPR-Volltext, EDPB-Guidelines
- DSA (Digital Services Act), AI Act, Data Act
- European Accessibility Act (EAA) / BFSG / BFSGV
- DDG (Digitale-Dienste-Gesetz, Anbieterkennzeichnung)
- TDDDG / ePrivacy (Cookie/Tracking-Einwilligung)
- BGB / EGBGB (Widerruf, digitale Produkte, Verbraucherrechte)
- Datenschutzkonferenz (DSK), Aufsichtsbehoerden der Bundeslaender

**UK / USA / CA / AU:**

- UK ICO (Information Commissioner's Office), DPA 2018, PECR, Online Safety Act
- US FTC, CPPA (California Privacy Protection Agency), State AGs
- US COPPA (Kinderdatenschutz), Health Breach Notification Rule, HIPAA, FDA
- Kanada OPC (Office of the Privacy Commissioner), Quebec CAI / Law 25
- Australien OAIC (Office of the Australian Information Commissioner) / APPs

**International:**

- China CAC (Cyberspace Administration of China) / PIPL
- Indien MeitY / DPDP Act / Rules 2025
- Japan PPC / APPI
- Korea PIPC / PIPA
- Brasilien ANPD / LGPD
- Singapore PDPC / PDPA
- Suedafrika Information Regulator / POPIA
- Saudi-Arabien SDAIA / PDPL
- VAE UAE Data Office / PDPL

### Recherche-Baseline 2026-04-28

Mindestens beruecksichtigen:

**Google Play (Pflicht-Baseline):**

- User Data / Data Safety: Privacy Policy fuer ALLE Apps Pflicht. Data Safety Form
  muss Code, SDKs und Datenfluesse korrekt abbilden.
- Account-Erstellung triggert In-App-Loeschpfad UND Weblink (beides Pflicht).
- Spezial-Policies: AI-generated Content, UGC, Photo/Video Broad Access, Health,
  Families, Payments/Subscriptions, Sensitive Permissions, Deceptive Behavior.

**DE/EU:**

- DDG §5 Anbieterkennzeichnung
- TDDDG / ePrivacy fuer Tracking/Cookies/Storage
- BFSG / EAA seit 28.06.2025 in Kraft (Barrierefreiheit fuer digitale Dienste)
- DSA fuer Hosting/UGC/Marketplace
- AI Act (gestaffelte Pflichten je nach Risikoklasse)

**UK / USA / CA / AU:**

- UK-GDPR / PECR / Online Safety Act
- CCPA / CPRA, COPPA, FTC Act
- Health Breach Notification Rule, HIPAA / FDA bei Health-Apps
- Kanada PIPEDA + Quebec Law 25
- Australia APP Privacy Policy

**International:**

- Indien DPDP Act + Rules 2025
- Brasilien LGPD / ANPD
- Suedafrika POPIA
- Singapore PDPA
- Japan APPI
- Korea PIPA
- Saudi PDPL

---

## Markt- und Rechtsraum-Prioritaet

Audits **immer in dieser Reihenfolge** bewerten:

### 1. Deutschland / EU (hoechste Prioritaet)

DSGVO, DDG/Impressum, TDDDG/ePrivacy, BGB/EGBGB Widerruf/digitale Produkte,
BFSG/EAA Barrierefreiheit, DSA bei UGC/Hosting/Marketplace, AI Act bei KI-Features.

### 2. Englischsprachige Zielmaerkte

- **UK**: UK-GDPR, DPA 2018, PECR, Online Safety Act, UK-GDPR Art. 27 (UK-Vertreter-Pflicht bei Datenverarbeitung von UK-Buergern ohne UK-Sitz)
- **USA**: CCPA/CPRA, weitere State Privacy Laws, COPPA, FTC Act,
  Health Breach Notification, HIPAA, FDA
- **Kanada**: PIPEDA, Quebec Law 25
- **Australien**: Privacy Act / APPs

### 3. Internationale Zielmaerkte

China PIPL, Indien DPDP, Japan APPI, Korea PIPA, Brasilien LGPD,
Singapore PDPA, Suedafrika POPIA, Saudi PDPL, UAE PDPL.

### Pflichtfragen pro Zielmarkt

Fuer jeden Zielmarkt MUSS geprueft werden:

1. Ist die App dort verfuegbar (Store-Listing, Geo-Filter, Backend-Erreichbarkeit)?
2. Gibt es App-Locale + Store-Listing-Locale + verstaendliche Rechtstexte in der
   Landessprache?
3. Welche lokalen Pflichtangaben, Consent-, Kinder-, Health-, AI-, UGC-, Abo-,
   Zahlungs- oder Transferregeln greifen?
4. Was ist technisch nachweisbar (Code, Manifest, SDK, Logs)?
5. Was muss juristisch geklaert werden?

**Wenn ein Markt nicht bewertet werden kann: BLOCKER fuer Rollout in diesem Markt.**

---

## Ablauf (8 Schritte — strikt in dieser Reihenfolge)

### Schritt 1 — Scope klaeren

Wenn der Benutzer den App-Namen nicht genannt hat, **einmal kurz fragen**:

> Welche App soll ich pruefen? (z.B. BestJournalAndroid, BestJournalFrank, QuizVerse)

Wenn fuer den Audit weitere Infos noetig und nicht aus dem Repo erkennbar sind,
Fragen **gesammelt** stellen, nicht einzeln:

1. In welchen Laendern/Sprachen soll die App veroeffentlicht werden?
2. Gibt es In-App-Kaeufe, Abos, Werbung, Affiliate-Links oder externe Zahlungen?
3. Gibt es Accounts, Cloud-Sync, Backups, Export, Import oder Datenloeschung?
4. Werden Firebase, Analytics, Crashlytics, Ads, KI-APIs oder andere SDKs genutzt?
5. Richtet sich die App an Kinder oder kann sie fuer Kinder attraktiv wirken?
6. Gibt es sensible Daten: Gesundheit, Tagebuch, Standort, Kontakte, Fotos, Audio,
   Kamera, Kalender, Finanzdaten, Religion, Sexualitaet, biometrische Daten?
7. Gibt es UGC, Chat, Sharing, Moderation, KI-generierte Inhalte?
8. Welche Barrierefreiheits-Stufe ist geplant (BFSG/EAA betrifft B2C-Dienste)?

Nicht auf Antworten warten, wenn der Repo-Zustand eine konservative Annahme erlaubt.
Unklare Punkte im Bericht als Annahmen markieren.

### Schritt 2 — Wissensbasis laden

Pruefe ob `<WORKSPACE_ROOT>/tools/rechtssicherheit.md` existiert.

| Zustand | Aktion |
|---------|--------|
| **Existiert** | Komplett einlesen. Diese Datei ist die Wissensbasis aus frueheren Sessions und wird am Ende aktualisiert. |
| **Fehlt** | Nach der Recherche (Schritt 3) wird sie zum ersten Mal angelegt. |

Dem Benutzer kurz melden: *"Lese Wissensbasis aus tools/rechtssicherheit.md..."*
oder *"Lege Wissensbasis neu an."*

### Schritt 3 — Internet-Recherche (5 Researcher parallel)

**Pflicht: 5 Researcher-Agenten in EINER Nachricht parallel starten** (nicht sequentiell).
Jeder Researcher: **max 50 Ergebnisse, max 15 Web-Fetches, max 10 Minuten Laufzeit,
max 2000 Woerter Prompt** (siehe `~/.Gemini/rules/agent-and-researcher-rules.md`).

Dem Benutzer vor dem Start sagen:
> "Ich starte 5 parallele Researcher fuer DE/EU, US/UK/CA/AU, Asien/International,
> Play-Store-Policies und Enforcement-/Abmahn-Trends. Laufzeit: ~5-8 Minuten."

**Researcher-Aufteilung (fix):**

| # | Agent | Fokus | Wichtige Quellen |
|---|-------|-------|------------------|
| 1 | `researcher` | **DE/EU** — DSGVO aktueller Stand, DDG-Anbieterkennzeichnung, TDDDG/ePrivacy, BGB/EGBGB Widerruf/digitale Produkte, BFSG/EAA, DSA, AI Act, BGH/EuGH-Rechtsprechung | dsgvo-gesetz.de, gesetze-im-internet.de, edpb.europa.eu, ec.europa.eu, datenschutzkonferenz-online.de, bundesjustizamt.de |
| 2 | `researcher` | **US/UK/CA/AU** — CCPA/CPRA, weitere State Privacy Laws, COPPA, FTC Act, Health Breach/HIPAA/FDA, UK-GDPR/PECR/Online Safety Act, PIPEDA/Quebec Law 25, Australia APPs | oag.ca.gov, cppa.ca.gov, ftc.gov, hhs.gov, fda.gov, ico.org.uk, ofcom.org.uk, priv.gc.ca, cai.gouv.qc.ca, oaic.gov.au |
| 3 | `researcher` | **Asien/LatAm/MEA** — PIPL (China), DPDP (Indien), APPI (Japan), PIPA (Korea), LGPD (Brasilien), Singapore PDPA, POPIA (Suedafrika), Saudi/UAE PDPL inkl. Cross-Border-Transfer | cac.gov.cn, meity.gov.in, ppc.go.jp, pipc.go.kr, gov.br/anpd, pdpc.gov.sg, justice.gov.za, sdaia.gov.sa |
| 4 | `researcher` | **Google Play Policies** — Data Safety Form, User Data Policy, Permissions, Sensitive Permissions, Families Policy, AI-generated Content, Deceptive Behavior, Health, Financial Services, Account Deletion, UGC, Photo/Video Broad Access, Payments/Subscriptions | support.google.com/googleplay/android-developer, play.google.com/console/about/policy, developer.android.com/privacy-and-security |
| 5 | `researcher` | **Enforcement-/Abmahn-Trends 2025/2026** — Impressum, Cookie-Consent-Urteile, fehlende Datenschutzerklaerung, SDK-Diskrepanzen, Data-Safety-Falschangaben, Abos/Dark Patterns, KI-Risiken, UGC-/DSA-Pflichten, Barrierefreiheit, Kinder/Schule | it-recht-kanzlei.de, dr-bahr.com, wbs.legal, juris.de, haendlerbund.de, edpb.europa.eu (Decisions) |

**Prompt-Muster pro Researcher:**

```
Recherchiere fuer [FOKUS] die aktuellen (Stand {Monat/Jahr}) rechtlichen
Pflichtangaben einer Android-App im Google Play Store.

Liefere strukturiert zurueck:
1. PFLICHTANGABEN-LISTE: Was muss zwingend in Datenschutz/ToS/Impressum/Widerruf/
   Account-Deletion/Consent/UI stehen?
2. MUSTER-KLAUSELN: Offizielle oder weit verbreitete Formulierungen mit Quelle.
3. SPRACHANFORDERUNG: Muessen die Texte in der Landessprache vorliegen oder
   reicht Englisch?
4. SANKTIONEN: Bussgelder / Abmahnrisiko / Play-Enforcement bei Verstoss.
5. AKTUELLE AENDERUNGEN: Was hat sich in den letzten 12 Monaten geaendert?
6. SPEZIALREGULIERUNG: Kinder, Health, KI, UGC, Ads, Abos, Barrierefreiheit,
   Cross-Border-Transfer — soweit fuer den Fokusbereich relevant.
7. QUELLEN: Offizielle URLs mit Abrufdatum, Quellenklasse (Primaer/Sekundaer/News).

Limits: max 50 Ergebnisse, max 15 Web-Fetches, max 10 Minuten.
Quellenprioritaet: Primaer > Sekundaer > News. News nie als alleinige Grundlage.
Bei Netzwerkfehlern: das zurueckgeben was da ist, nicht crashen.
```

**Wenn ein Researcher fehlschlaegt:** Sofort dem Benutzer auf Deutsch melden, die
anderen weiterlaufen lassen, nicht still weitermachen.

### Schritt 4 — Pflicht: Vollscan der Codestruktur

> **Vor jeder Bewertung MUSS die gesamte App durchsucht werden — nicht nur Dateien
> mit Namen wie `privacy`, `terms` oder `legal`. Rechtlich relevante Hinweise
> stecken in UI-Texten, ViewModels, Repository-Klassen, SDK-Initialisierung,
> Build-Konfiguration, Store-Metadaten, Tests, Markdown-Dateien, Scripts und
> Web-/Backend-Hilfsdateien.**

#### 4.0 Datei-Inventur

```sh
rg --files [APP_DIR]
rg --files [APP_DIR] | rg -i "\.(kt|java|xml|gradle|kts|json|properties|md|html|js|ts|tsx|jsx|yaml|yml|txt|csv)$"
```

Generated/build Artefakte duerfen ausgeschlossen werden, wenn sie klar
reproduzierbar sind: `build/`, `.gradle/`, `node_modules/`, `.idea/`, `captures/`.
**Store-Metadaten, Legal-Assets, Scripts und Configs duerfen NICHT ausgeschlossen
werden.**

#### 4.1 Legal-Signal-Suche ueber die gesamte App

```sh
rg -n -i "privacy|datenschutz|dsgvo|gdpr|ccpa|cpra|consent|einwilligung|widerruf|withdraw|terms|nutzungsbedingungen|agb|impressum|anbieter|legal|policy|delete account|account deletion|datenloesch|loesch|support|kontakt|contact|billing|subscription|abo|refund|iap|in-app|admob|ads|advertising|analytics|crashlytics|firebase|sentry|tracking|telemetry|location|standort|camera|kamera|microphone|mikrofon|contacts|kontakte|calendar|kalender|health|gesundheit|journal|diary|tagebuch|ai|ki|openai|anthropic|gemini|children|kids|families|ugc|moderation|webview|cookie|font|google fonts|third party|drittanbieter|export|backup|sync|cloud|encryption|verschluessel|log\\.|timber|token|secret|api[_-]?key|http://" [APP_DIR]
```

**Pflicht: Auch Manifest, Build-Dateien, Store-Metadaten, Scripts, Ressourcen,
UI-Texte, SDK-Initialisierung, Repositorys, ViewModels, Datenbank, Netzwerk,
Export/Import, Backup und Logs durchsuchen** — nicht nur Legal-Dateinamen.

#### 4.2 Treffer klassifizieren

| Klasse | Beispiele | Abgleich gegen |
|---|---|---|
| Dokument/Legal-Text | Privacy, Terms, Impressum, Widerruf, Support | Pflichtdokumente, Sprache, Aktualitaet |
| UI-Link/Navigation | Settings, About, Onboarding, Paywall | Erreichbarkeit, Platzierung, Timing |
| Datenverarbeitung | Repository, API, Sync, Export, Import | Privacy Policy, Consent, Data Safety |
| SDK/Third Party | Firebase, Ads, Analytics, Crash, AI APIs | Dritte, Transfer, Data Safety, Consent |
| Permission/Sensor | Location, Camera, Audio, Media, Contacts | Manifest, Runtime Permission, Prominent Disclosure |
| Account/Deletion | Login, Signup, Delete, Auth | Loeschweg, Data Safety, Privacy Policy |
| Monetarisierung | Billing, Subscription, Ads, Paywall | Terms, Widerruf, Preis-/Abo-Info |
| Sicherheit | Backup, Logs, Secrets, TLS, WebView | Android Security, Datenschutzversprechen |
| Kinder/Health/AI/UGC | Families, Health, GenAI, Moderation | Spezial-Policies, Disclaimer, Meldesystem |
| Barrierefreiheit | Content-Description, Focus, Reader | BFSG/EAA, WCAG, EN 301 549 |

**Pflicht-Ergebnis:** Fuer jede Treffergruppe ein Abgleich:
- "in Dokumenten erwaehnt?",
- "in App verlinkt?",
- "in Data Safety/Play Console zu deklarieren?",
- "Consent noetig?",
- "Release-Blocker?".

**Wenn keine Treffer in einem erwarteten Bereich gefunden werden, explizit notieren**,
z.B. "Keine Account-Erstellung gefunden" oder "Keine Ads-SDKs gefunden".

### Schritt 5 — Detail-Pruefung

#### 5a. Projekt und Package

```sh
rg -n "namespace |applicationId|package=" -g "*.gradle*" -g "*.kts" -g "AndroidManifest.xml" [APP_DIR]
```

Erfassen: App-Name, Package-ID, Variante/Flavor, minSdk, targetSdk, compileSdk,
Store-Metadaten (`fastlane/metadata`, `play-store-metadata`, README, Website),
verwendete Sprachen/Locales (`app/src/main/res/values*`).

#### 5b. Manifest, Permissions und Android-Komponenten

```sh
rg -n "uses-permission|queries|provider|receiver|service|activity|exported|allowBackup|fullBackupContent|dataExtractionRules|networkSecurityConfig|usesCleartextTraffic|debuggable" -g "AndroidManifest.xml" [APP_DIR]
```

**Besonders kritisch:**

- `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- `READ_CONTACTS`, `READ_CALENDAR`, `READ_PHONE_STATE`, SMS/Call-Log
- `CAMERA`, `RECORD_AUDIO`
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `MANAGE_EXTERNAL_STORAGE`
- `POST_NOTIFICATIONS` bei sensiblen Inhalten
- `QUERY_ALL_PACKAGES`
- `AD_ID`
- Health Connect / Gesundheitsdaten
- Exported Activities/Services/Receivers/Providers
- `allowBackup`, `fullBackupContent`, `dataExtractionRules`
- `usesCleartextTraffic` und Network Security Config

**Jede Permission gegen Feature, Rechtstext, Prominent Disclosure, Runtime-Permission-
Dialog und Data Safety abgleichen.**

#### 5c. SDKs, Dependencies und Datenabfluss

```sh
rg -n -i "firebase|analytics|crashlytics|admob|ads|facebook|meta|adjust|appsflyer|sentry|amplitude|mixpanel|onesignal|revenuecat|billing|stripe|openai|anthropic|googleapis|okhttp|retrofit|ktor|webview|javascript" [APP_DIR]
rg -n -i "http://|https://|Authorization|Bearer|apiKey|apikey|secret|token|client_secret" [APP_DIR]
```

**Pruefen:**
- Welche SDKs sammeln automatisch Daten?
- Werden IP-Adresse, Device IDs, Crash Logs, Analytics Events, Advertising ID,
  Firebase Installation ID oder Push Tokens verarbeitet?
- Drittlandtransfer ausserhalb EU/EWR? SCC/DPF/PIPL/DPDP/LGPD/PDPL-Grundlage?
- AVV/DPA oder Transfergrundlage benoetigt?
- Privacy Policy spezifisch genug fuer jeden SDK-Zweck?
- Debug-/Test-/Staging-Endpunkte entfernt?
- Werden Secrets aus zentraler Secrets-Ablage (z.B. `$HOME/SK/`) statt aus dem Repo geladen?

#### 5d. Lokale Speicherung, Logs und Backup

```sh
rg -n -i "SharedPreferences|DataStore|RoomDatabase|SQLite|File\\(|openFileOutput|cacheDir|externalCacheDir|getExternalFilesDir|Log\\.|Timber|println|printStackTrace|Encrypted|KeyStore|MasterKey" [APP_DIR]
```

**Pruefen:**
- Sensitive Daten verschluesselt oder mindestens nicht in Klartext?
- Tagebuch-, Gesundheits-, Auth-, Token- und Profil-Daten nicht in Backups,
  Screenshots, Clipboard, Logs oder Crashreports geleakt?
- Backup-Regeln schliessen sensitive Daten aus oder verlangen Ende-zu-Ende-Schutz?
- Export/Import bewusst dokumentiert und sicher?
- Loeschfunktion loescht lokale Daten, Sync-Daten und Backups soweit moeglich?

#### 5e. Rechtstexte und UI-Links

```sh
rg -n -i "datenschutz|privacy|privacy policy|terms|nutzungsbedingungen|agb|impressum|anbieter|widerruf|refund|deletion|loesch|delete account|support|kontakt|contact" [APP_DIR]
rg --files [APP_DIR] | rg -i "privacy|terms|impressum|legal|widerruf|refund|delete|deletion|support"
```

**Platzierungs-Pruefung:**

| Pflicht-Platzierung | Typisches Muster |
|--------------------|------------------|
| **Onboarding/Consent-Screen** | `ConsentScreen.kt`, Link zur Datenschutzerklaerung VOR Datenerhebung |
| **Settings-Screen** | Menupunkte "Datenschutz", "Nutzungsbedingungen", "Impressum" dauerhaft erreichbar |
| **About-Screen** | Impressum-Link mit Kontaktdaten |
| **Consent-Widerruf** | Benutzer kann Zustimmung nachtraeglich widerrufen (DSGVO Art. 7 Abs. 3) |
| **Paywall/Checkout** | Widerrufsbelehrung VOR Kaufabschluss |
| **Account-Deletion** | In-App-Pfad UND Weblink |

**Sprach-Pruefung:** `app/src/main/res/values-XX/strings.xml` auflisten. Welche
Locales hat die App? Abgleich mit Play-Store-Release-Sprachen. Fehlende
Uebersetzungen der Rechtstexte als Befund notieren.

#### 5f. Consent und Widerruf

- Einwilligung VOR nicht notwendiger Datenverarbeitung.
- Kein vorangekreuztes Consent.
- Widerruf so einfach wie Zustimmung.
- Consent-Zwecke getrennt: Analytics, Crashlytics, Ads, personalisierte Ads,
  Cloud-Sync, Newsletter/Marketing, KI-Verarbeitung.
- App funktioniert soweit moeglich auch ohne freiwillige Einwilligungen.
- Alter/Kinderschutz, falls relevant.
- Prominent Disclosure direkt im Nutzungsfluss (nicht nur in Settings/Privacy Policy).

#### 5g. Store Listing und Play Console

- Privacy Policy URL in Store Listing oeffentlich erreichbar.
- App-/Developer-Name in Privacy Policy stimmen mit Store Listing ueberein.
- Data Safety Form deckt echte Datenerhebung und SDKs ab.
- Account Deletion Form/Weblink vorhanden, wenn Account-Erstellung vorhanden.
- Content Rating, Target Audience, Families, Ads, Health, AI, Financial,
  Data Deletion, App Access, Permissions declarations korrekt.
- Screenshots/Marketingtexte versprechen nichts Falsches.
- Abo-/Preisangaben und Trial-Hinweise klar.

#### 5h-UK. UK-Vertreter-Pflicht (UK-GDPR Art. 27) — Standard-Empfehlung: Option B

**Gesetz:** UK-GDPR Art. 27 verlangt einen UK-Vertreter, sobald personenbezogene
Daten von Personen in UK verarbeitet werden UND das Unternehmen keinen Sitz in
UK hat. Eine App im UK Play Store mit Sitz ausserhalb UK (z.B. Deutschland)
verarbeitet zwangslaeufig solche Daten.

**Was ein UK-Vertreter ist:**

- Person/Firma mit Sitz in UK
- Anlaufstelle fuer ICO und UK-Nutzer (kein Haftungsuebernehmer, "Briefkasten")
- MUSS in Datenschutzerklaerung mit Name + UK-Adresse + Kontakt-E-Mail genannt werden

**Zwei Optionen — der Skill empfiehlt IMMER Option B:**

| Option | Aufwand | Kosten | Empfehlung dieses Skills |
|--------|---------|--------|--------------------------|
| **A — UK-Vertreter buchen** (z.B. gdprlocal.com, verasafe.com, captaincompliance.com) | Vertrag, Adressuebernahme in DSE | ~£100-300/Jahr laufend | **NICHT empfohlen** |
| **B — UK aus Distribution ausschliessen** | Play Console → App content → Country availability → United Kingdom abhaken | 0 | **STANDARD-EMPFEHLUNG** |

**Trigger-Bedingungen (ALLE drei muessen gleichzeitig zutreffen — sonst kein Befund):**

1. App ist im UK Play Store verfuegbar (oder soll dort verfuegbar gemacht werden)
2. App-Anbieter sitzt nicht in UK (z.B. Deutschland)
3. App verarbeitet personenbezogene Daten von UK-Nutzern — **PFLICHT-Pruefung im Repo,
   keine Annahme**. Was als Datenverarbeitung zaehlt:
   - Crash-Logs/Crashlytics/Sentry (IP-Adresse + Device-IDs)
   - Analytics (Firebase Analytics, Amplitude, Mixpanel, etc.)
   - Push-Notifications (FCM Token = personenbezogen)
   - Account-/Login-System (E-Mail, Username, Auth-Token)
   - Cloud-Sync, Backup-Server, Server-API-Calls
   - Werbe-IDs (Advertising ID), Ads SDKs
   - WebView mit Login/Cookie-Tracking
   - Datenbank-Backups in Cloud-Speicher
   - Server-Logs auf Backend (auch IP)

**WICHTIG — Wenn keine Datenverarbeitung nachgewiesen werden kann:**

Wenn der Skill nach gruendlicher Repo-Pruefung KEINE der oben genannten Datenverarbeitungen
findet (echte 100%-Offline-App, keine Permissions ausser ggf. STORAGE, kein Backend, keine
SDKs ausser Build-Tools), dann darf die App in UK bleiben — **es gibt keinen Befund und
keine Empfehlung zum Ausschluss**. Im Bericht wird notiert:

> "UK-Vertreter-Pflicht (Art. 27) greift hier nicht, weil im Repo keine personenbezogene
> Datenverarbeitung von UK-Nutzern nachweisbar ist. UK darf in Country Availability bleiben."

**Pflicht-Pruefung im Repo (vor jeder UK-Empfehlung):**

```sh
# Datenverarbeitung indirekt nachweisen
rg -n -i "firebase|crashlytics|analytics|sentry|amplitude|mixpanel|fcm|admob|adsense|advertising[-_ ]id|ad_id|ads-sdk" [APP_DIR]
# Netzwerk-/Backend-Aktivitaet
rg -n -i "INTERNET|http://|https://|retrofit|okhttp|ktor|api-key|bearer" [APP_DIR]
# Account-/Login-Indikatoren
rg -n -i "login|signup|signin|account|firebase[-_ ]auth|google[-_ ]signin" [APP_DIR]
# Cloud-Sync / Backup
rg -n -i "drive|dropbox|s3|gcs|cloud[-_ ]sync|backup[-_ ]server|sync[-_ ]server" [APP_DIR]
```

Wenn EINE dieser Suchen Treffer in produktivem Code ergibt → Datenverarbeitung gegeben →
Trigger 3 erfuellt → Option B empfehlen.

**Pflicht-Handlung des Skills (NUR wenn alle 3 Trigger erfuellt):**

Wenn alle drei Trigger zutreffen, MUSS der Skill in den Bericht aufnehmen:

- Befund-Schweregrad: **🟠 HOCH** (mindestens) bzw. **🔴 BLOCKER** falls die
  App bereits live in UK ist und kein Vertreter benannt ist
- Empfehlung: **Option B — UK aus Country Availability entfernen**
- Konkrete Schritte fuer Play Console (UK abhaken, Release-Notes pruefen)
- Hinweis fuer Datenschutzerklaerung: Wenn UK ausgeschlossen wird, MUSS sichergestellt
  sein dass die DSE keine UK-Vertreter-Sektion mehr enthaelt (sonst widersprueche)
- Hinweis fuer Store-Listing-Sprachen: UK-Englisch (en-GB) als Pflichtsprache faellt weg
- Hinweis: Auch andere Distribution-Kanaele pruefen (GitHub Releases, F-Droid,
  Amazon Appstore, Galaxy Store, Sideload-Anleitungen) — UK MUSS dort ebenfalls
  ausgeschlossen werden, sonst greift Art. 27 trotzdem
- Wenn der Benutzer ausdruecklich Option A waehlt: Vermerken aber NICHT eigenstaendig
  Vertreter-Daten in die DSE eintragen (juristische Pruefung noetig)

**Pruefung im Repo:**

```sh
# Privacy Policy auf "UK Representative" / "UK-Vertreter" pruefen
rg -n -i "uk[ -]representative|uk[ -]vertreter|article[ ]27|art\\.[ ]27|gdprlocal|verasafe|captaincompliance" [APP_DIR]
# Country availability / Distribution-Listen
rg -n -i "country[ -]availability|distribution|target[ -]markets|countries" [APP_DIR] -g "*.md" -g "*.txt" -g "*.yaml" -g "*.yml" -g "fastlane/**"
```

**Was NIEMALS passieren darf:**

- ❌ App in UK ausliefern, ohne Vertreter UND ohne Ausschluss
- ❌ Vertreter-Daten erfinden oder Platzhalter einsetzen
- ❌ Pseudo-Adresse (z.B. die eines Kollegen ohne Vertretungsvereinbarung)
- ❌ Annahme "App sammelt keine Daten" ohne harte Pruefung — Crash-Logs/IP/Token zaehlen schon

**Empfehlung in der Berichts-Box:**

> UK ist nicht im Country-Availability-Set freigegeben. Wenn UK ausgerollt werden
> soll, ist Option A (UK-Vertreter buchen) noetig — bis dahin gilt Option B
> (UK ausgeschlossen) als verbindliche Standard-Loesung dieses Skills.

#### 5h. UGC, KI/GenAI und Moderation

- Bei Chat/Posts/Sharing/Kommentaren: Terms-Akzeptanz, Report-/Blockier-Funktion,
  Moderationsflow, DSA-Pflichten (Hosting/Marketplace).
- Bei KI/GenAI: Safety-Filter, In-App-Reporting, AI-/Deepfake-Hinweise,
  Pflichtangaben nach AI Act je Risikoklasse, Google Play AI Content Policy.

#### 5i. Barrierefreiheit (BFSG/EAA, WCAG, EN 301 549)

- Legal-Links, Checkout, Paywall, Account-Anlage, Account-Loeschung, Consent-Screen
  muessen barrierefrei sein.
- Mindestens: ContentDescription, Touch-Target-Groessen, Kontrast, Screenreader-
  Support, Tastaturbedienung, Fokus-Management.
- BFSG/EAA gilt fuer B2C-digitale-Dienste seit 28.06.2025.

---

## Markt- und Jurisdiktions-Gates (Matrix)

Der Skill erstellt fuer jeden geplanten Zielmarkt einen Eintrag in folgender Matrix:

| Rechtsraum | Pflichtpruefung | Typische Release-Blocker |
|---|---|---|
| **DE/EU** | DSGVO, DDG, TDDDG, BGB/EGBGB, BFSG/EAA, DSA, AI Act | Fehlendes Impressum, fehlende Datenschutzerklaerung in DE, Cookie-/Tracking-Consent fehlt, kein Widerruf bei IAP/Abos, BFSG-Verstoesse |
| **UK** | UK-GDPR, DPA 2018, PECR, Online Safety Act, **UK-GDPR Art. 27 (UK-Vertreter-Pflicht NUR bei Datenverarbeitung von UK-Buergern)** | PECR-Consent fehlt, OSA-Pflichten bei UGC, Privacy Policy ohne UK-Bezug, **kein UK-Vertreter benannt trotz Datenverarbeitung** — Standard-Empfehlung dieses Skills: **UK aus Distribution ausschliessen (Option B)**. Wenn echte Offline-App ohne Datenverarbeitung: UK darf bleiben. |
| **USA** | CCPA/CPRA, State Privacy Laws, COPPA, FTC Act, Health Breach/HIPAA/FDA | "Do Not Sell"-Pflichten, COPPA bei Kindern, Health-Claims ohne FDA |
| **Kanada** | PIPEDA, Quebec Law 25 | Quebec-Sprachpflicht, Privacy-Officer-Pflicht, Data-Transfer-Disclosure |
| **Australien/NZ** | Privacy Act / APPs | Fehlende Privacy Policy mit AU-Bezug, Cross-Border-Disclosure |
| **China** | PIPL | Fehlende Cross-Border-Transfer-Grundlage, Lokalisierung, Lizenz-/Registrierungspflichten |
| **Indien** | DPDP Act / Rules 2025 | Consent-Manager-Pflicht, Notice-Pflichten, Data-Fiduciary-Status |
| **Japan** | APPI | Cross-Border-Transfer-Disclosure, Sensitive-Daten-Consent |
| **Korea** | PIPA | Strikte Consent-Pflichten, Notification-Pflichten, Data-Protection-Officer |
| **Brasilien** | LGPD / ANPD | Rechtsgrundlagen-Disclosure, ANPD-Registrierung, Cross-Border-Transfer |
| **Singapore** | PDPA | DNC-Register, Data-Protection-Officer, Consent-Pflichten |
| **Suedafrika** | POPIA | Information-Officer-Pflicht, Cross-Border-Transfer |
| **Saudi/UAE** | PDPL | Cross-Border-Transfer-Genehmigung, Lokalisierungsanforderungen |

**Wenn ein Markt nicht bewertet werden kann: BLOCKER fuer Rollout in diesem Markt.**

---

## Spezialregulierungen nach Feature (Feature-Gates)

Pro App pruefen, welche Feature-Gates greifen:

| Feature | Pflicht-Pruefung | Typische Quelle |
|---|---|---|
| **Kinder/Families** | COPPA, GDPR-K, Google Play Families Policy, keine verbotenen IDs/Ads, Altersgate | FTC, EDPB, Play Console |
| **Health/Medical** | Health Connect, Health Declaration, Medical Claims, kein irrefuehrender Disclaimer, ggf. HIPAA/FDA | Google Play Health Policy, FDA, EU MDR |
| **KI/GenAI** | AI Act je Risikoklasse, Google Play AI-Generated Content Policy, Deepfake-/Safety-Hinweise | EU AI Act, Play Policy |
| **UGC/Chat/Sharing/Kommentare** | DSA, Online Safety Act, Moderation, Melden/Blockieren, Beschwerdeweg | EU DSA, UK Ofcom |
| **Ads/AdMob** | Advertising ID, Consent (insb. EU/UK/CA), personalisierte Ads, Kinder-Ausschluss | Play Ads Policy, IAB TCF |
| **Billing/Abos/IAP/Trials** | Widerruf, Dark-Pattern-Verbot, klare Preis-/Laufzeit-/Kuendigungsangaben, Refund-Hinweise | Play Payments, BGB, EU Consumer Law |
| **Standort (Background)** | Core-Feature-Begruendung, Prominent Disclosure, Permission Declaration | Play Permissions |
| **Kontakte** | Permission Declaration, Privacy Policy, sehr enge Zwecke | Play Permissions |
| **Fotos/Videos (Broad Access)** | Photo Picker bevorzugen, sonst Broad Access Declaration | Android Privacy |
| **WebView/Cookies/JS Bridge** | Mixed Content, JS-Bridge-Sicherheit, externe Inhalte, Cookie-Consent | Android Security, ePrivacy |
| **Push/FCM** | Token-Daten, sensitive Notification-Inhalte, Consent | Play, GDPR |
| **Tagebuch/private Daten** | Verschluesselung, Backup-Regeln, Export, Loeschung, Screenshots | DSGVO, Android Privacy |
| **Barrierefreiheit** | BFSG/EAA, WCAG, EN 301 549 — Checkout, Paywall, Account, Legal Links | EU EAA, BFSG |
| **Cross-Border-Transfers** | SCC, EU-US DPF, PIPL/DPDP/LGPD/PDPL Transfergrundlage | EDPB, nationale Behoerden |
| **UK-Vertreter (Art. 27)** | App in UK Store + Sitz ausserhalb UK + personenbezogene Daten → **Standard-Empfehlung Option B (UK ausschliessen)** | UK-GDPR Art. 27, ICO |

---

## Pflichtdokumente: Inhaltliche Checklisten

### Datenschutzerklaerung

Pflicht-Inhalte:

- Verantwortlicher (Name/Firma, Adresse, Kontakt, Datenschutzkontakt)
- App-Name und Package/Store-Bezug
- Kategorien personenbezogener Daten
- Zwecke der Verarbeitung
- Rechtsgrundlagen je Zweck
- Empfaenger/Dritte/SDKs
- Drittlandtransfer und Garantien
- Speicherdauer oder Kriterien
- Betroffenenrechte: Auskunft, Berichtigung, Loeschung, Einschraenkung,
  Portabilitaet, Widerspruch, Widerruf, Beschwerderecht
- Pflicht oder Freiwilligkeit der Bereitstellung
- Automatisierte Entscheidungen/Profiling, falls vorhanden
- Kinder/Jugendliche, falls relevant
- Sicherheitsmassnahmen knapp und realistisch
- Account- und Datenloeschung
- Stand/Version/Datum

**Release-Blocker:**

- Allgemeine Generator-Texte ohne App-/SDK-Bezug
- "Wir sammeln keine Daten", obwohl SDKs/Crashlytics/Analytics/Ads/Cloud laufen
- Fehlende Drittanbieter
- Fehlende Rechtsgrundlagen
- Fehlende Loesch-/Widerrufswege
- Falscher Verantwortlicher oder falsche App

### Nutzungsbedingungen / AGB

- Anbieter, App-Name, Leistungsbeschreibung
- Nutzungsregeln und verbotene Nutzung
- User Generated Content (Rechte, Moderation, Meldung, Entfernung, Sperrung,
  Beschwerdeweg) falls vorhanden
- Haftung und Gewaehrleistung ohne unzulaessige Pauschalausschluesse
- Verfuegbarkeit, Aenderungen, Kuendigung
- In-App-Kaeufe, Abos, Preise, Laufzeiten, Kuendigung
- Drittanbieter/Stores/Billing-Hinweise
- Rechtswahl/Gerichtsstand nur soweit zulaessig gegenueber Verbrauchern
- Kontakt und Beschwerdeweg

**Release-Blocker:**

- AGB schliessen Verbraucherrechte pauschal aus
- AGB widersprechen Store Listing, Paywall oder Privacy Policy
- Kostenpflichtige Features ohne klare Preis-/Abo-/Kuendigungsangaben

### Impressum / Anbieterkennzeichnung

Fuer DE/EU geschaeftsmaessige digitale Dienste:

- Vollstaendiger Name/Firma
- Ladungsfaehige Anschrift (kein reines Postfach)
- E-Mail und schnelle elektronische Kontaktmoeglichkeit
- Vertretungsberechtigte Person bei juristischen Personen
- Register, Registernummer, USt-ID, Aufsichtsbehoerde oder Berufsangaben falls einschlaegig
- Leicht erkennbar, unmittelbar erreichbar, staendig verfuegbar
- **In App dauerhaft erreichbar (Settings/About), nicht nur im Store Listing**

**Release-Blocker:**

- Kein Impressum trotz geschaeftsmaessigem Angebot
- Nur E-Mail ohne Anschrift
- Impressum nur in schwer auffindbarem Weblink oder totem Link

### Widerruf / digitale Inhalte / Abos

Bei paid app, IAP, Abo oder externen digitalen Inhalten:

- Widerrufsbelehrung VOR Kaufabschluss erreichbar
- Muster-Widerrufsformular vorhanden, wenn erforderlich
- Erloeschen des Widerrufsrechts bei sofortiger digitaler Leistung nur mit
  ausdruecklicher Zustimmung und Bestaetigung der Kenntnis
- Abo-Laufzeit, Preis, Testphase, automatische Verlaengerung und Kuendigung
  klar in Paywall/Store/Terms
- Google Play Billing Regeln eingehalten

**Release-Blocker:**

- Kostenpflichtige digitale Inhalte ohne Widerrufsinformation
- "Kein Widerruf" ohne korrekte Zustimmung/Belehrung
- Paywall widerspricht Terms oder Store Listing

### Account- und Datenloeschung

Wenn Account-Erstellung moeglich:

- In-App-Pfad zur Accountloeschung
- Weblink fuer Loeschanfrage ohne App-Installation
- Link funktional, nennt App oder Developer, fuehrt direkt zum Loeschprozess
- Erklaert, welche Daten geloescht/behalten/anonymisiert werden und warum
- Data Safety Form beantwortet Data deletion Fragen konsistent

**Release-Blocker:**

- Account-Erstellung ohne Accountloeschung
- Weblink fuehrt nur zu Support-Homepage ohne klaren Loeschweg
- Privacy Policy verspricht Loeschung, App bietet sie nicht

---

## Formulierungs-Pruefung (Texte und UI-Strings)

Der Skill prueft Rechtstexte UND UI-Texte auf riskante Formulierungen:

**Zu vermeiden:**

- Garantien wie "100% sicher", "rechtssicher", "abmahnungssicher", "DSGVO-konform garantiert",
  "medizinisch bewiesen", "heilt/diagnostiziert/behandelt".
- Falsche "Wir sammeln keine Daten"-Aussagen bei aktiven SDKs/Cloud/Crash/Analytics.
- Pauschale Ausschluesse von Verbraucherrechten ("keine Gewaehrleistung", "kein Widerruf").
- Versteckte Kuendigungs-/Widerrufs-/Loeschhuerden (Dark Patterns).
- Marketing-Aussagen, die Leistungsversprechen ueber den Funktionsumfang hinaus erzeugen.

**Pflicht-Mindeststandard:**

- Klare Zwecke, Datenkategorien, Empfaenger, Rechtsgrundlagen, Speicherdauer.
- Plain Language, verstaendlich fuer Zielgruppe (insb. bei Kindern: kindgerechte
  Zusatzinformationen).
- Lokale Sprache/Locale passend zum Zielmarkt (Quebec FR, Indien EN+lokale Sprache,
  Korea KO, Japan JA, China ZH, Saudi AR — Pruefung pro Markt).
- Ehrliche Free-Tier-Kommunikation (App funktioniert kostenlos weiter), keine
  irrefuehrenden Trial-/Abo-Hinweise.

---

## Spezielle Android-/Play-Risikomatrix

| Bereich | Typische technische Signale | Zusaetzliche Pflichtpruefung |
|---|---|---|
| Analytics/Crashlytics | Firebase, Sentry, Amplitude, Events | Consent, SDK-Daten, Drittlandtransfer, Data Safety |
| Ads/AdMob | AD_ID, ads SDKs | Ads Policy, Consent, personalisierte Ads, Kinder |
| Kinder/Families | Target age, kindliche Assets, Games | Families Policy, COPPA/GDPR-K, keine verbotenen IDs |
| Standort | Fine/Background Location | Core Feature, Prominent Disclosure, Permission Declaration |
| Fotos/Videos | READ_MEDIA_* | Photo Picker bevorzugen, Broad Access Declaration |
| Gesundheit | Health Connect, Mood, Fitness, Medical claims | Health Declaration, Datenschutz, kein irrefuehrender Medizinclaim |
| KI/GenAI | OpenAI/Anthropic/Gemini, image/text generation | AI Content Policy, Reporting, Safety, Datenweitergabe |
| UGC/Community | Posts, Kommentare, Sharing | Moderation, Melden/Blockieren, Terms, DSA-Risiko |
| Finanzen | Budget, Payment, Trading, Kredit | Financial Services Policy, Disclaimer, Lizenzen |
| Tagebuch/Private Daten | Room, local DB, cloud sync | Verschluesselung, Backup, Export, Loeschung, Screenshots |
| WebView | JavaScript, file access, remote URLs | Mixed content, JS bridge, Tracking/Cookies, externe Inhalte |
| Push | FCM, notification permissions | Token-Daten, sensitive Notification-Inhalte, Consent |
| Barrierefreiheit | ContentDescription, Touch-Target | BFSG/EAA, WCAG, EN 301 549 |

---

## Schweregrade

| Grad | Bedeutung |
|------|-----------|
| 🔴 **BLOCKER** | Release stoppen. Hohe Abmahn-, Bussgeld- oder Play-Enforcement-Gefahr. |
| 🟠 **HOCH** | Vor Release korrigieren. Wesentliche Pflichtangabe oder technische Inkonsistenz. |
| 🟡 **MITTEL** | Risiko reduzieren, moeglichst vor Release korrigieren. |
| 🟢 **NIEDRIG** | Best Practice, Klarheit, Wartbarkeit. |
| ℹ️ **INFO** | Beobachtung ohne direkten Befund. |

**BLOCKER-Beispiele:**

- Keine Datenschutzerklaerung in App/Store
- Privacy Policy sagt "keine Daten", aber App nutzt Analytics/Ads/Crash/Cloud
- Keine Anbieterkennzeichnung bei geschaeftsmaessigem DE/EU-Angebot
- Account-Erstellung ohne Loeschweg
- Kinderzielgruppe mit verbotenen IDs/Ads/Tracking
- Health/Medical Claims ohne korrekte Deklaration und Disclaimer
- Sensitive Permissions ohne Core-Feature, Disclosure oder Play Declaration
- Widerrufsbelehrung fehlt bei kostenpflichtigen digitalen Inhalten
- Store Data Safety widerspricht Code/SDKs
- UGC ohne Moderation/Meldesystem in DSA-Geltungsbereich
- KI-Features ohne AI-Act-konforme Hinweise (je Risikoklasse)
- Markt im Rollout, der nicht bewertet werden konnte (Sprache/Recht)

---

## Schritt 6 — Berichtsvorlage

```markdown
# Rechtssicherheits-Audit: [App]
Datum: YYYY-MM-DD
Skill-Stand: 2026-04-28

## Disclaimer
Technische Pruefhilfe, keine anwaltliche Beratung. Vor Release Fachanwalt fuer
IT-Recht konsultieren.

## Scope und Annahmen
- App/Package:
- Zielmaerkte:
- Monetarisierung:
- Accounts:
- SDKs:
- Sensible Daten:
- Feature-Gates (Kinder/Health/AI/UGC/Ads/Abo/Standort/Barrierefreiheit):

## Gesamtstatus
- Release-Empfehlung: [BLOCKIEREN | BEDINGT | TECHNISCH OK NACH ANWALTSPRUEFUNG]
- BLOCKER: N
- HOCH: N
- MITTEL: N
- Wichtigste Risiken:

## Befunde
### 🔴 BLOCKER
1. [Titel]
   - Nachweis: [Datei:Zeile oder Quelle]
   - Risiko: [konkret]
   - Fix: [konkret]
   - Quelle: [URL, Abrufdatum]

### 🟠 HOCH
...
### 🟡 MITTEL
...
### 🟢 NIEDRIG
...
### ℹ️ INFO
...

## Dokumentenmatrix
| Dokument | In App | Store/Web | Inhalt OK | Sprache OK | Befund |
|---|---:|---:|---:|---:|---|
| Datenschutzerklaerung | | | | | |
| Nutzungsbedingungen | | | | | |
| Impressum | | | | | |
| Widerruf | | | | | |
| Account-/Datenloeschung | | | | | |

## Codestruktur-Vollscan
| Treffergruppe | Dateien/Beispiele | Rechtliche Relevanz | Abgleich | Status |
|---|---|---|---|---|
| SDKs/Dritte | | Datenschutz, Transfer, Data Safety | | |
| Permissions/Sensoren | | Prominent Disclosure, Consent | | |
| Account/Sync/Loeschung | | DSGVO, Google Account Deletion | | |
| Billing/Ads/Abo | | Widerruf, Terms, Ads Policy | | |
| Security/Backup/Logs | | Datenschutz, Sicherheitsversprechen | | |
| Health/Kinder/AI/UGC | | Spezial-Policies | | |
| Barrierefreiheit | | BFSG/EAA, WCAG | | |

## Code-vs-Text-vs-Play-Matrix
| Daten/Feature | Code/SDK/Permission | Privacy Policy | Data Safety | Consent/UI | Status |
|---|---|---|---|---|---|

## Android-Sicherheitscheck
| Kontrolle | Ergebnis | Risiko | Fix |
|---|---|---|---|
| Permissions minimal | | | |
| Backup-Regeln | | | |
| TLS/Cleartext | | | |
| Sensitive Logs | | | |
| Secrets im Repo | | | |
| Exported Components | | | |
| WebView sicher | | | |

## Sprach- und Marktfreigabe
| Markt | App-Locale | Rechtstexte | Pflicht/Empfehlung | Freigabe |
|---|---|---|---|---|

## Jurisdiktions-Gates
| Rechtsraum | Pflichtpruefung | Bewertung | Release-Blocker? |
|---|---|---|---|
| DE/EU | DSGVO, DDG, TDDDG, BGB, BFSG, DSA, AI Act | | |
| UK | UK-GDPR, PECR, Online Safety Act | | |
| USA | CCPA/CPRA, COPPA, FTC, Health Breach | | |
| Kanada | PIPEDA, Quebec Law 25 | | |
| Australien | Privacy Act / APPs | | |
| China | PIPL | | |
| Indien | DPDP | | |
| Japan | APPI | | |
| Korea | PIPA | | |
| Brasilien | LGPD | | |
| Singapore | PDPA | | |
| Suedafrika | POPIA | | |
| Saudi/UAE | PDPL | | |

## Formulierungs-Check
| Textstelle | Datei:Zeile | Risikoart | Empfehlung |
|---|---|---|---|
| (z.B. "100% sicher") | | Garantie | Entfernen, Plain-Language-Alternative |

## Play-Console-Checkliste
- [ ] Data Safety passt zu Code und SDKs
- [ ] Privacy Policy URL erreichbar
- [ ] Account deletion beantwortet und verlinkt
- [ ] App Access korrekt
- [ ] Content Rating korrekt
- [ ] Target Audience/Families korrekt
- [ ] Ads/Health/AI/Finance/UGC/Permissions Declarations korrekt

## Fix-Reihenfolge
1. BLOCKER zuerst.
2. HOCH vor Release.
3. MITTEL vor Rollout in weitere Laender.
4. NIEDRIG bei naechster Pflege.

## Quellen
- [URL] - [Thema] - [Quellenklasse: Primaer/Sekundaer/News] - abgerufen am YYYY-MM-DD

## Abschluss-Disclaimer
Technische Pruefhilfe, keine anwaltliche Beratung. Vor Release Fachanwalt fuer
IT-Recht konsultieren.
```

---

## Schritt 7 — Wissensbasis aktualisieren

`<WORKSPACE_ROOT>/tools/rechtssicherheit.md` updaten oder neu anlegen.

**Struktur:**

```markdown
# rechtssicherheit.md - Wissensbasis
Letzte Recherche: YYYY-MM-DD
Naechste Pflicht-Pruefung: YYYY-MM-DD (+30 Tage bei Play Policies, +90 Tage sonst)

## Quellenregister
| Datum | Quelle | Thema | Quellenklasse | Relevanz |
|---|---|---|---|---|

## Pflichtangaben-Matrix

### EU/DE - Datenschutz (DSGVO)
### EU/DE - Impressum (DDG)
### EU/DE - Widerruf (BGB/EGBGB, digitale Produkte)
### EU/DE - TDDDG / ePrivacy (Tracking/Cookies/Storage)
### EU/DE - DSA (Hosting/UGC/Marketplace)
### EU/DE - AI Act (KI-Features je Risikoklasse)
### EU/DE - BFSG / EAA (Barrierefreiheit)

### UK - UK-GDPR / DPA / PECR / Online Safety Act
### USA - CCPA/CPRA, State Privacy Laws, COPPA, FTC, Health Breach/HIPAA/FDA
### Kanada - PIPEDA, Quebec Law 25
### Australien - Privacy Act / APPs

### International
- China PIPL
- Indien DPDP
- Japan APPI
- Korea PIPA
- Brasilien LGPD
- Singapore PDPA
- Suedafrika POPIA
- Saudi PDPL
- UAE PDPL

### Google Play
- Data Safety / User Data
- Permissions / Sensitive Permissions
- Account Deletion
- Families / Kinder
- Health
- AI-generated Content
- UGC / Deceptive Behavior
- Ads
- Payments / Subscriptions

### Android Security/Privacy Controls
- Photo Picker, Storage Scoped, Permissions
- Backup-Regeln, dataExtractionRules
- Network Security Config, TLS, Cleartext
- WebView, JS Bridge

### Spezialfaelle
- Kinder
- Health
- AI/GenAI/Deepfake
- Ads / Advertising ID
- UGC / Moderation / Beschwerdeweg
- Finance

## Sprach-Anforderungen pro Markt
| Markt | Sprache | Pflicht? | Quelle |
|---|---|---|---|

## Aktuelle Abmahn-Hotspots (Stand YYYY-MM)
| Thema | Quelle | Empfehlung |
|---|---|---|

## App-Audit-Log
| Datum | App | Version | Status | Blocker | Hoch | Commit/Notiz |
|---|---|---|---|---:|---:|---|

## Wiederverwendbare Befundmuster
| Muster | App-Klasse | Empfohlener Fix |
|---|---|---|

## Muster-Klauseln (mit Quelle)
| Klausel | Bereich | Quelle | Stand |
|---|---|---|---|
```

**Diff-Logik:** Neue Erkenntnisse gegenueber dem gespeicherten Stand hervorheben
("**Aenderungen seit letzter Recherche**"). Veraltete Eintraege (>90 Tage) als
"zu verifizieren" markieren. Jede Pflichtangabe mit Quell-URL + Abrufdatum +
Quellenklasse.

**Keine Secrets, echten Kundendaten, privaten Adressen oder Token in diese Datei
schreiben**, ausser der Benutzer verlangt explizit genau diese Ablage.

### Schritt 8 — Commit + Push

`<WORKSPACE_ROOT>/tools/rechtssicherheit.md` committen und pushen — gemaess
`~/.Gemini/rules/parallel-sessions-git.md`:

1. Nur eigene Dateien namentlich stagen (NIE `git add -A`)
2. Commit mit fortlaufender Nummer: `#NNNN - rechtssicherheit audit [App]`
3. `git fetch origin && git rebase origin/main`
4. `git status --short` pruefen
5. `git push`

---

## Typische technische Fix-Hinweise

Der Skill darf konkrete technische Fixes vorschlagen oder implementieren:

- Settings/About Links zu Datenschutz, Terms, Impressum, Loeschung
- Consent-Screen vor Analytics/Ads/Cloud/KI mit getrennten Zwecken und Widerruf
- Toggles fuer Analytics/Crashlytics/Ads inkl. Widerruf
- Runtime-Permission-Erklaerungen an Feature-Kontext koppeln
- Unnoetige Permissions entfernen
- Android Photo Picker statt breiter Medien-Permissions
- UGC: Terms-Akzeptanz vor erstem Post, Report-/Blockier-Funktion, Moderationsflow,
  Beschwerdeweg gemaess DSA/Online Safety Act
- KI/GenAI: In-App-Reporting fuer problematische Outputs, Safety-Filter,
  AI-/Deepfake-Hinweise gemaess AI Act und Google Play AI Policy
- Abo-/Trial-/Paywall-Texte klarer machen, sichtbarer Cancellation-Link in der App
- Barrierefreiheits-Probleme in Legal-/Checkout-/Account-Flows fixen
  (ContentDescription, Touch-Target, Kontrast, Fokus)
- Backup-Regeln fuer sensitive Daten setzen (`fullBackupContent`, `dataExtractionRules`)
- `usesCleartextTraffic=false` oder Network Security Config bereinigen
- Sensitive Logs entfernen
- Secrets in `$HOME/SK/` verlagern und Repo bereinigen
- Store-/Data-Safety-Checkliste als Markdown erzeugen
- Falsche App-Namen, Platzhalter oder tote Links korrigieren

**Der Skill darf KEINE anwaltlich wirkenden endgueltigen Rechtstexte als "fertig"
verkaufen.** Er darf Entwuerfe, Lueckenlisten, Musterhinweise mit Quellen und
anwaltliche Pruefpunkte erstellen.

---

## Was NIEMALS passieren darf

- ❌ Rechtliche Garantie geben
- ❌ App ohne aktuelle Quellenlage als "rechtssicher" freigeben
- ❌ Nur Datenschutztext lesen und Code/SDKs/Permissions ignorieren
- ❌ Nur nach Legal-Dateinamen suchen und dabei rechtlich relevante Logik in Code,
  Ressourcen, Store-Metadaten, Build-Dateien, Scripts oder UI-Flows uebersehen
- ❌ Einen gefundenen Datenfluss akzeptieren, ohne ihn gegen Privacy Policy,
  Nutzungsbedingungen, Data Safety, Consent, App-UI und Loesch-/Widerrufswege abzugleichen
- ❌ Google Play Data Safety ungeprueft uebernehmen
- ❌ Gebrochene Links, Platzhalter oder falsche App-/Firmennamen uebersehen
- ❌ Account-Erstellung ohne Loeschpfad akzeptieren
- ❌ Sensitive Daten in Logs, Backups oder Crashreports ignorieren
- ❌ Unnoetige Permissions als harmlos einstufen
- ❌ Sequentielle Researcher statt parallel (kostet 5x so lange)
- ❌ Researcher ohne Limits (max 50 Ergebnisse / 15 Fetches / 10 Min / 2000 Woerter)
- ❌ Wissensbasis am Ende nicht aktualisieren (dann lernt das System nicht dazu)
- ❌ App-Pruefung ohne vorherige Recherche (dann fehlen Pflichtangaben-Kriterien)
- ❌ Rechtstexte nur in Deutsch+Englisch belassen ohne Pruefung welche Laender
  die Landessprache zwingend verlangen
- ❌ DSA, AI Act, BFSG/EAA, TDDDG/ePrivacy, COPPA, Online Safety Act,
  DPDP/PIPL/LGPD/POPIA/PDPL ignorieren
- ❌ UGC, Chat, KI, Health, Kinder, Abos, Ads oder sensitive Permissions ohne
  Sonderpolicy-Gate behandeln
- ❌ Nicht bewertete Store-Laender still freigeben
- ❌ App in UK ausliefern ohne Vertreter (Art. 27) UND ohne UK-Ausschluss (Standard-Empfehlung dieses Skills bei UK-Vertreter-Pflicht ist IMMER Option B = UK ausschliessen)
- ❌ Fremde oder unklare Aenderungen im Repo mitcommitten
- ❌ Dateien ausserhalb des aktuellen Workspaces bearbeiten, ausser explizit angefordert
- ❌ `~/Codex/`, `~/proggs/` oder andere Home-Unterordner als Standard-Arbeitsverzeichnis
  verwenden, wenn ein Workspace/Repo-Root verfuegbar ist
- ❌ Konkrete Rechtstexte als anwaltlich fertige Texte verkaufen
  (max. Muster-Hinweise mit Quelle und Empfehlung "durch Anwalt pruefen lassen")

---

## Beispiel-Dialog

**Benutzer:** "starte den Skill Rechtssicherheit und pruefe BestJournalAndroid"

**Gemini:**
> Ich starte ein Rechtssicherheits-Audit fuer BestJournalAndroid.
>
> **Disclaimer:** Das ist eine technische Pruefhilfe und ersetzt keine anwaltliche
> Beratung. Vor Release ist eine Pruefung durch einen Fachanwalt fuer IT-Recht Pflicht.
>
> Ablauf:
> 1. Lese Wissensbasis aus `tools/rechtssicherheit.md` (oder lege sie neu an).
> 2. Starte 5 parallele Researcher (DE/EU, US/UK/CA/AU, Asien/International,
>    Play-Store-Policies, Enforcement-/Abmahn-Trends). Laufzeit: ~5-8 Minuten.
> 3. Vollscan der Codestruktur in `BestJournalAndroid/` (Datei-Inventur + Legal-Signale).
> 4. Detail-Pruefung: Manifest/Permissions, SDKs, lokale Speicherung, Logs, Backup,
>    Rechtstexte und deren Platzierung, Consent, Store-Metadaten, UGC/AI/Health/
>    Kinder/Barrierefreiheit, Jurisdiktions-Gates pro Markt.
> 5. Erstelle Befund-Bericht mit Codestruktur-Vollscan, Code-vs-Text-vs-Play-Matrix,
>    Android-Sicherheitscheck, Jurisdiktions-Gates, Formulierungs-Check,
>    Play-Console-Checkliste.
> 6. Aktualisiere Wissensbasis und committe.
>
> Loslegen?

[Researcher laufen parallel → Konsolidierung → Vollscan → Detail-Pruefung →
Bericht → Wissensbasis-Update → Commit+Push]

---

## Zusammenspiel mit anderen Skills

- **`uebersetzung`**: Bei fehlenden Sprachversionen — Vorschlag, den Uebersetzungs-
  Skill zu starten (Markt-Pflichtsprachen pruefen).
- **`string-extraktor`**: Wenn Rechtstexte hardcodiert im Code stehen statt in strings.xml.
- **`superintelligenz` / `selbstbeobachtung`** (Direktiven #1+#2): Neue Erkenntnisse
  (z.B. neues Abmahn-Urteil, neue Play-Policy, neue Behoerden-Guidelines) werden
  in `tools/rechtssicherheit.md` persistiert — Compound Intelligence Effect.
- **`resilient-bugfixing`** (Direktive #3): Wiederkehrende Befundmuster (z.B. immer
  fehlendes Impressum) als systematischen Repo-Check vorschlagen.

---

## Abschluss-Meldung

Am Ende IMMER:

1. Bericht ausgeben (Disclaimer am Anfang UND Ende)
2. Geaenderte Dateien nennen
3. Ausgefuehrte Checks/Builds nennen
4. Nicht pruefbare Punkte klar markieren
5. `<WORKSPACE_ROOT>/tools/rechtssicherheit.md` commit+push
6. Disclaimer wiederholen
7. Intelligenz-Vorschlaege (Direktive #2) falls Muster erkannt — z.B. "alle
   geprueften Apps haben denselben Impressum-Fehler — soll ich einen Hook bauen
   der das automatisch checkt?"

