---
name: rechtssicherheit
description: "Prüft Android-Apps technisch auf Datenschutz-, Impressums-, Widerrufs-, Play-Compliance- und Abmahnrisiken. Nutze diesen Skill bei Rechtssicherheit, DSGVO, Abmahnungscheck, Data Safety, Play-Store-Compliance oder Release-Audit."
invocation: user
---

# Skill: Rechtssicherheit

> **Wichtiger Disclaimer (PFLICHT — am Anfang UND am Ende des Berichts wiederholen):**
> Dieser Skill ist eine **technische Pruefhilfe** und ersetzt **KEINE anwaltliche Beratung**.
> Er kann fehlende Pflichtangaben, technische Inkonsistenzen, Play-Policy-Risiken und
> typische Abmahn-Fallstricke markieren — er gibt **keine Garantie** fuer Rechtssicherheit
> oder Abmahnungssicherheit. Fuer eine verbindliche Rechtspruefung MUSS vor Release ein
> **Fachanwalt fuer IT-Recht** konsultiert werden.

---

## Grundsatz: Keine Garantien

**Verbotene Formulierungen:**

- ❌ "Die App ist rechtssicher."
- ❌ "Die App ist 100% abmahnungssicher."
- ❌ "Dieser Text reicht rechtlich aus."
- ❌ "Du kannst jetzt bedenkenlos releasen."

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
Play-Store-Release oder rechtliche Pflichttexte einer Android-App geht — auch
wenn der Benutzer den Skill-Namen nicht explizit nennt.

**Deutsche Trigger:**

- "starte den Skill Rechtssicherheit"
- "pruefe [App] auf Rechtssicherheit"
- "Rechtssicherheit fuer [App]"
- "DSGVO-Check fuer [App]"
- "ist [App] abmahnungssicher"
- "Abmahnungscheck [App]"
- "Play-Store rechtskonform pruefen"
- "Rechts-Audit [App]"
- "Data-Safety-Check"
- "Account-Loeschung pruefen"
- "Widerruf pruefen"
- "Impressum pruefen"

**Auch ohne exakten Trigger verwenden bei:**

- Datenschutzerklaerung, DSGVO/GDPR, CCPA/CPRA, UK-GDPR, PIPL, DPDP, APPI,
  PIPA, LGPD oder anderen Datenschutzpflichten.
- Impressum/Anbieterkennzeichnung, AGB/Nutzungsbedingungen, Widerrufsbelehrung,
  Consent, Account-/Datenloeschung, Support/Kontakt oder Pflichtlinks in der App.
- Google Play Data Safety, Play-Console-Deklarationen, Permissions, SDKs,
  Tracking, Ads, Analytics, Crashlytics, KI-APIs, Backups, Logs oder Secrets.

**Pflicht-Pruefumfang:**

1. App-Code und Manifest gegen Rechtstexte und Play-Console-Angaben abgleichen.
2. Parallel recherchieren: DSGVO/GDPR, CCPA/CPRA, UK-GDPR, PIPL, DPDP, APPI,
   PIPA, LGPD, Google Play Data Safety, DDG-Anbieterkennzeichnung,
   Widerrufsbelehrung und AGB.
3. Bestehende App-Dokumente pruefen: Datenschutzerklaerung, Nutzungsbedingungen,
   Impressum, Widerrufsbelehrung, Account-/Datenloeschung, Support/Kontakt.
4. Platzierung und Uebersetzung pruefen: Store Listing, Onboarding, Consent,
   Settings, About, Paywall/Checkout und alle App-Sprachen.
5. Technische Datenschutz- und Sicherheitsrisiken pruefen: Manifest-Permissions,
   SDKs, Backup-Regeln, lokale Speicherung, Logs, Secrets, TLS und Data Safety.

---

## Plattformneutrale Pfade (KRITISCH)

Die Wissensbasis liegt **workspace-lokal**, nicht in einem persoenlichen Home-Unterordner.

**Zielpfad (plattformneutral):** `<WORKSPACE_ROOT>/tools/rechtssicherheit.md`

`<WORKSPACE_ROOT>` ist das aktuelle Arbeitsverzeichnis bzw. der Repo-Root, in dem
die zu pruefende App liegt. Beispiele fuer die Aufloesung:

| Plattform | Beispielpfad |
|-----------|--------------|
| Windows   | `%USERPROFILE%\Codex\tools\rechtssicherheit.md` |
| macOS/Linux | `$HOME/Codex/tools/rechtssicherheit.md` |
| Generisch | `<repo-root>/tools/rechtssicherheit.md` |

**Verboten:** Harte Pfade wie `C:\Users\...`, `/Users/barwa/...` oder Bezuege auf
private Home-Unterordner als Standard. `~/Codex/`, `~/proggs/` oder andere
Home-Unterordner duerfen nicht als Default-Arbeitsverzeichnis verwendet werden,
wenn ein aktueller Workspace/Repo-Root verfuegbar ist.

**Wenn der Benutzer explizit eine Datei ausserhalb des Workspaces nennt:** Diese
konkrete Datei darf gelesen/geschrieben werden. Sonst bleibt der Skill im aktuellen
Workspace.

---

## Aktueller Recherche-Stand

Stand dieser Skill-Version: **2026-04-28**.

Bei jeder echten App-Pruefung aktuelle Quellen erneut pruefen, wenn:

- die letzte Recherche aelter als 30 Tage ist,
- Google Play Policies betroffen sind,
- Health, Kinder, Standort, Medien, Kontakte, SMS/Call Logs, Finanzdaten, KI/GenAI,
  Ads, Analytics, User Generated Content oder Accounts vorkommen,
- die App in neue Laender/Sprachen ausgerollt wird,
- neue Store-Laender, neue Monetarisierungsmodelle, neue KI-/UGC-/Health-/Kinder-
  Features oder neue SDKs hinzukommen,
- der Benutzer "aktuell", "neueste" oder "Release" sagt.

**Pflicht-Quellenklassen:**

- Google Play Developer Policy Center / Play Console Help.
- Android Developers Privacy & Security Dokumentation.
- EU-Kommission, GDPR-Text, DSA, AI Act, European Accessibility Act, Data Act,
  nationale Umsetzungsgesetze.
- Deutsche Gesetze: DDG fuer Anbieterkennzeichnung, TDDDG/ePrivacy fuer Tracking/
  Endgeraetezugriff, BGB/EGBGB fuer Widerruf und digitale Produkte, BFSG/BFSGV fuer
  Barrierefreiheit.
- Aufsichtsbehoerden: EDPB, Datenschutzkonferenz, ICO, Ofcom, FTC, CPPA/US State AGs,
  OAIC, OPC/Quebec CAI, ANPD, PPC Japan, PIPC Korea, PDPC Singapore, Information
  Regulator South Africa, SDAIA, relevante asiatische/lateinamerikanische Behoerden.
- OWASP MASVS/MASTG fuer technische Mobile-Security-Kontrollen.

**Quellenprioritaet:**

1. Primaerquellen: Gesetzestext, offizielle Regulierer, Google/Android-Policy.
2. Sekundaerquellen: Fachanwaltliche Kanzlei-Artikel, Fachverbands- oder
   Regulierer-Erlaeuterungen.
3. News/Blogs/Foren nur als Hinweis auf aktuelle Risiken, nie als alleinige Grundlage.

Wenn Quellen widersprechen, gilt die strengere oder offiziellere Quelle als
Release-Gate. Jede Aussage im Bericht bekommt Quelle + Abrufdatum. Ohne aktuelle Quelle
keine positive Freigabe, sondern "anwaltlich/aktuell zu pruefen".

### Markt- und Rechtsraum-Prioritaet

Audits werden immer in dieser Reihenfolge bewertet:

1. **Deutschland/EU zuerst**: DSGVO, DDG/Impressum, TDDDG/ePrivacy, BGB/EGBGB
   Widerruf/digitale Produkte, BFSG/EAA Barrierefreiheit, DSA bei UGC/Hosting/
   Marktplatzfunktionen, AI Act bei KI-Funktionen.
2. **Englischsprachige Zielmaerkte danach**: UK-GDPR/DPA/PECR/Online Safety Act,
   USA mit CCPA/CPRA plus relevanten State-Privacy-Laws, COPPA, FTC Act, Health
   Breach/HIPAA/FDA-Trigger, Kanada PIPEDA/Quebec Law 25, Australien Privacy Act/APPs.
3. **Internationale Zielmaerkte danach**: China PIPL, Indien DPDP, Japan APPI,
   Korea PIPA, Brasilien LGPD, Singapore PDPA, Suedafrika POPIA, Saudi PDPL,
   UAE PDPL und weitere Ziellaender der Store-Verteilung.

**Release-Gate-Regel:** Fuer jeden Zielmarkt muss im Bericht stehen:

- Ist die App in diesem Markt/Store-Land verfuegbar?
- Gibt es App-Locale, Store-Listing-Locale und Rechtstexte in einer fuer Nutzer
  verstaendlichen Sprache?
- Welche lokalen Pflichtangaben, Consent-, Kinder-, Health-, AI-, UGC-, Abo-,
  Zahlungs- oder Datenuebermittlungsregeln greifen?
- Was ist technisch nachweisbar, was ist nur juristisch zu klaeren?

Wenn ein Markt nicht bewertet werden kann, wird dieser Markt als **BLOCKER fuer Rollout
in diesem Markt** markiert, nicht still freigegeben.

### Recherche-Baseline 2026-04-28

Diese Baseline ist kein Ersatz fuer Schritt 3, sondern der aktuelle Mindeststand, gegen
den jede neue App-Pruefung abgeglichen wird:

- **Google Play User Data/Data Safety:** Privacy Policy ist fuer alle Apps erforderlich;
  Data Safety muss Code, SDKs und Drittanbieter-Datenfluesse korrekt abbilden.
  Account-Erstellung triggert In-App-Loeschpfad plus Weblink.
  Quelle: `https://support.google.com/googleplay/android-developer/answer/10144311`
  und `https://support.google.com/googleplay/android-developer/answer/10787469`
- **Google Play Spezial-Policies:** AI-generierte Inhalte brauchen Safety und In-App-
  Reporting; UGC braucht Terms-Akzeptanz, Reporting/Blocking und Moderation; Photo/
  Video-Broad-Access, Health, Families, Payments/Subscriptions und sensitive Permissions
  haben eigene Release-Gates. Quellen u.a.
  `https://support.google.com/googleplay/android-developer/answer/13985936`,
  `https://support.google.com/googleplay/android-developer/answer/9876937`,
  `https://support.google.com/googleplay/android-developer/answer/14115180`,
  `https://support.google.com/googleplay/android-developer/answer/9900533`
- **DE/EU:** DDG §5 verlangt Anbieterkennzeichnung fuer geschaeftsmaessige digitale
  Dienste; TDDDG/ePrivacy ist fuer Endgeraetezugriff/Tracking relevant; BFSG/EAA gilt
  seit 28.06.2025 fuer bestimmte digitale Verbraucherleistungen/eCommerce; DSA greift
  bei Vermittlungs-/Hosting-/UGC-/Plattformfunktionen; AI Act bei KI-Systemen und
  bestimmten Transparenz-/Risikopflichten. Quellen: `gesetze-im-internet.de/ddg`,
  `gesetze-im-internet.de/ttdsg`, `gesetze-im-internet.de/bfsg`,
  `eur-lex.europa.eu/eli/reg/2022/2065/oj`,
  `commission.europa.eu/news/ai-act-enters-force-2024-08-01_en`
- **Englischsprachige Maerkte:** UK braucht UK-GDPR/PECR und bei UGC/Chat ggf.
  Online Safety Act; Kalifornien verlangt CCPA/CPRA Notice-at-Collection und mobile
  App Privacy-Policy-Zugaenglichkeit; USA triggern bei Kindern COPPA und bei Health
  ggf. FTC Health Breach/HIPAA/FDA; Kanada verlangt meaningful consent nach PIPEDA
  und Quebec Law 25; Australien verlangt eine mobil brauchbare APP Privacy Policy.
  Quellen: `ico.org.uk`, `ofcom.org.uk`, `oag.ca.gov/privacy/ccpa`, `ftc.gov`,
  `priv.gc.ca`, `cai.gouv.qc.ca`, `oaic.gov.au`
- **Internationale Maerkte:** Indien DPDP Act/Rules 2025 ist operationalisiert;
  Brasilien LGPD/ANPD regelt Rechte und internationale Transfers; Suedafrika POPIA,
  Singapore PDPA, Japan APPI, Korea PIPA und Saudi PDPL verlangen eigene Notice-,
  Consent-, Kontakt-, Rechte-, Kinder-/sensible-Daten- und Transferpruefungen.
  Quellen: `meity.gov.in`, `pib.gov.in`, `gov.br/anpd`, `inforegulator.org.za`,
  `pdpc.gov.sg`, `ppc.go.jp`, `pipc.go.kr`, `sdaia.gov.sa`

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
7. Gibt es User Generated Content, Chat, Kommentare, Sharing, Marktplatz-, Dating-,
   Community-, Social-, Creator-, Empfehlungs- oder Moderationsfunktionen?

Nicht auf Antworten warten, wenn der Repo-Zustand eine konservative Annahme erlaubt.
Unklare Punkte im Bericht als Annahmen markieren.
Wenn Zielmaerkte unbekannt sind: Standardannahme **Deutschland/EU zuerst**, danach
**UK/USA/Kanada/Australien**, danach globale Datenschutz-Baseline. Alle nicht
geprueften Ziellaender als "nicht freigegeben" markieren.

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
max 2000 Woerter Prompt** (siehe `~/.codex/rules/agent-and-researcher-rules.md`).

Dem Benutzer vor dem Start sagen:
> "Ich starte 5 parallele Researcher fuer DE/EU, US/UK, Asien, Play-Store und
> Abmahn-Trends. Laufzeit: ~5-8 Minuten."

**Researcher-Aufteilung (fix):**

| # | Agent | Fokus | Wichtige Quellen |
|---|-------|-------|------------------|
| 1 | `researcher` | **DE/EU** — DSGVO aktueller Stand, DDG-Anbieterkennzeichnung, TDDDG/ePrivacy, BGB/EGBGB Widerruf/digitale Produkte, BFSG/EAA Barrierefreiheit, DSA bei UGC/Hosting/Marketplace, AI Act bei KI, aktuelle BGH/EuGH-Rechtsprechung | eur-lex.europa.eu, commission.europa.eu, gesetze-im-internet.de, edpb.europa.eu, datenschutzkonferenz-online.de, bundesjustizamt.de |
| 2 | `researcher` | **US/UK/CA/AU** — CCPA/CPRA, weitere US State Privacy Laws, FTC Act/Dark Patterns, COPPA, Health Breach/HIPAA/FDA-Trigger, UK-GDPR/DPA/PECR/Online Safety Act, PIPEDA/Quebec Law 25, Australia Privacy Act/APPs | oag.ca.gov, cppa.ca.gov, coag.gov, ftc.gov, hhs.gov, fda.gov, ico.org.uk, ofcom.org.uk, priv.gc.ca, cai.gouv.qc.ca, oaic.gov.au |
| 3 | `researcher` | **International** — PIPL (China), DPDP (Indien), APPI (Japan), PIPA (Korea), LGPD (Brasilien), Singapore PDPA, Suedafrika POPIA, Saudi PDPL/UAE PDPL inkl. Cross-Border-Transfer und Sprachanforderungen | cac.gov.cn, meity.gov.in, ppc.go.jp, pipc.go.kr, gov.br/anpd, pdpc.gov.sg, inforegulator.org.za, sdaia.gov.sa |
| 4 | `researcher` | **Google Play Policies** — Data Safety Form, User Data Policy, Account Deletion, Permissions/Sensitive APIs, Photo/Video, Health, Families, Ads, Payments/Subscriptions, AI-generated Content, UGC, Deceptive Behavior, Developer verification | support.google.com/googleplay/android-developer, developer.android.com, developers.google.com/android, play.google.com/console/about/policy |
| 5 | `researcher` | **Aktuelle Enforcement-/Abmahnwellen 2025/2026** — fehlendes Impressum, Cookie/SDK-Consent, Data-Safety-Diskrepanzen, Abo-/Dark-Pattern-Enforcement, KI-/Deepfake-/UGC-Pflichten, Barrierefreiheits-Rollout, Kinder-/Altersregeln | primaer offizielle Enforcement-Seiten, danach Fachkanzleien wie it-recht-kanzlei.de, dr-bahr.com, wbs.legal, juris.de |

**Prompt-Muster pro Researcher:**

```
Recherchiere fuer [FOKUS] die aktuellen (Stand {Monat/Jahr}) rechtlichen
Pflichtangaben einer Android-App im Google Play Store.

Liefere strukturiert zurueck:
1. PFLICHTANGABEN-LISTE: Was muss zwingend in Datenschutz/ToS/Impressum/Widerruf/
   Account-Deletion stehen?
2. MUSTER-KLAUSELN: Offizielle oder weit verbreitete Formulierungen mit Quelle.
3. SPRACHANFORDERUNG: Muessen die Texte in der Landessprache vorliegen oder
   reicht Englisch?
4. SANKTIONEN: Bussgelder / Abmahnrisiko bei Verstoss.
5. AKTUELLE AENDERUNGEN: Was hat sich in den letzten 12 Monaten geaendert?
6. RELEASE-GATES: Welche Punkte blockieren Release im Zielmarkt?
7. QUELLEN: Offizielle URLs mit Abrufdatum.

Limits: max 50 Ergebnisse, max 15 Web-Fetches, max 10 Minuten.
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
rg -n -i "privacy|datenschutz|dsgvo|gdpr|ccpa|consent|einwilligung|widerruf|withdraw|terms|nutzungsbedingungen|agb|impressum|anbieter|legal|policy|delete account|account deletion|datenloesch|loesch|support|kontakt|contact|billing|subscription|abo|refund|iap|in-app|admob|ads|advertising|analytics|crashlytics|firebase|sentry|tracking|telemetry|location|standort|camera|kamera|microphone|mikrofon|contacts|kontakte|calendar|kalender|health|gesundheit|journal|diary|tagebuch|ai|ki|openai|anthropic|gemini|children|kids|families|ugc|moderation|webview|cookie|font|google fonts|third party|drittanbieter|export|backup|sync|cloud|encryption|verschluessel|log\\.|timber|token|secret|api[_-]?key|http://" [APP_DIR]
```

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
- Drittlandtransfer ausserhalb EU/EWR?
- AVV/DPA oder SCC/Transfergrundlage benoetigt?
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

#### 5h. Markt- und Jurisdiktions-Gates

Fuer jeden Zielmarkt eine eigene Gate-Zeile erstellen. **Deutschland/EU immer zuerst**,
auch wenn die App international erscheinen soll.

| Rechtsraum | Pflichtpruefung | Typische Release-Blocker |
|---|---|---|
| DE/EU | DSGVO-Transparenz, Rechtsgrundlagen, AVV/DPA/SCC, DDG-Impressum, TDDDG/ePrivacy, BGB/EGBGB Widerruf, BFSG/EAA, DSA/AI Act falls relevant | kein Impressum, fehlende Rechtsgrundlage, Tracking ohne Consent, kein Widerruf, UGC ohne Moderations-/Meldeweg, KI ohne Transparenz/Safety, nicht barrierefreier eCommerce-Flow |
| UK | UK-GDPR/DPA, PECR fuer Tracking/Marketing, ICO Children's Code, Online Safety Act bei User-to-User/Search/UGC | keine UK-kompatible Privacy Notice, Cookie/SDK-Tracking ohne PECR-Consent, Kinderangebot ohne Age-Appropriate-Design, UGC/Chat ohne Safety-Prozess |
| USA | CCPA/CPRA und weitere State Privacy Laws je Schwelle/Zielmarkt, FTC Act gegen unfair/deceptive practices, COPPA unter 13, Health Breach/HIPAA/FDA bei Health/Medical | fehlender Notice-at-Collection, Opt-out/Do-Not-Sell/Share fehlt, manipulative Paywall, Kindertracking, Health-Datenleck, medizinische Claims ohne Pruefung |
| Kanada | PIPEDA, Quebec Law 25, meaningful consent, Datenschutzkontakt, grenzueberschreitende Verarbeitung | Consent nicht verstaendlich, sensible Daten ohne ausdrueckliche Einwilligung, kein Kontakt/Grievance-Prozess, Quebec nicht bewertet |
| Australien/Neuseeland | Privacy Act/APPs, mobile-gerechte Privacy Policy, Direktmarketing/Spam, sensible Daten | Policy nicht mobil lesbar, Zweck/Empfaenger/Rechte fehlen, sensibler Datentyp ohne klare Zustimmung |
| China | PIPL, separate/eindeutige Einwilligung fuer sensible Daten/Transfer, lokale App-/SDK-Regeln, Cross-Border-Mechanismus | keine China-spezifische Notice, kein separater Consent, ungeklaerte Auslandsuebermittlung, Kinder-/sensible Daten ungeprueft |
| Indien | DPDP Act/Rules, Notice + Consent, Kinder/Verifiable Parental Consent, Grievance-Mechanismus, Datenloeschung | keine klare Notice, kein Beschwerdeweg, Kindertracking/Targeted Ads, keine Loesch-/Korrekturwege |
| Japan | APPI, Zweckangabe, Drittweitergabe, Auslandsuebermittlung, Anfragen/Beschwerden | Zweck/Empfaenger unklar, Opt-out/Third-Party-Transfer nicht erklaert, kein Kontakt |
| Korea | PIPA, Datenschutz-Policy, Einwilligung, sensitive/unique identifiers, Drittweitergabe/Auslandstransfer | keine PIPA-spezifische Policy, empfindliche Daten ohne getrennte Einwilligung, Auslandsuebermittlung ungeprueft |
| Brasilien | LGPD, Rechtsgrundlage, Betroffenenrechte, Datenschutzkontakt, internationale Transfers nach ANPD-Regeln | keine Rechte-/Kontaktangaben, keine Rechtsgrundlage, Transfermechanismus fehlt |
| Singapore | PDPA, Consent/Notification, Purpose Limitation, Protection, Retention, DPO/Kontakt | kein DPO/Kontakt, Sammlung ohne Zweck, keine Retention/Loeschung |
| Suedafrika | POPIA, 8 Conditions, Information Officer/Responsible Party, Children/Special PI, Prior Authorisation falls relevant | keine Responsible-Party-Angaben, Kinder-/Special-PI ohne Autorisierung/Grundlage, kein Rechteprozess |
| Saudi/UAE | PDPL/UAE PDPL, Consent/Notice, Transferregeln, lokale Sprache/Contact je Zielmarkt | keine lokale Notice, ungeklaerte Transfers, keine Betroffenenrechte/Kontaktwege |

**Regel:** Eine globale Datenschutzerklaerung darf lokale Besonderheiten nur abdecken,
wenn sie konkret genug ist. "GDPR-style" allein reicht nicht als Freigabe fuer alle
Laender. Nicht bewertete Laender werden im Bericht als nicht freigegeben markiert.

#### 5i. Spezialregulierungen nach Feature

- **UGC/Community/Chat/Sharing:** Google-UGC-Policy, DSA/Online Safety Act, Terms-
  Akzeptanz vor Upload, Melden/Blockieren, Moderation, Beschwerde-/Appeal-Prozess,
  Child-Safety, Notice-and-Action, Kontaktstelle.
- **KI/GenAI:** Google AI-Generated-Content-Policy, User-Reporting in App, Safety-
  Filter, Deepfake/Manipulated-Media-Regeln, AI Act Transparenz/Risiko-Klassifizierung,
  Offenlegung von Datenweitergabe an KI-Anbieter, keine medizinischen/finanziellen/
  rechtlichen Garantien durch KI.
- **Health/Wellbeing/Medical:** Google Health-Apps-Form, Health Connect/Data Safety,
  FTC Health Breach/HIPAA/FDA/Medical-Device-Trigger, klare Abgrenzung "keine Diagnose/
  keine Behandlung" nur wenn sachlich zutreffend, keine irrefuehrenden Heilsversprechen.
- **Kinder/Jugendliche:** Google Families/Target Audience, COPPA, DSGVO-K, UK
  Children's Code, DPDP/POPIA-Kinderregeln, keine personalisierte Werbung/Tracking
  ohne tragfaehige Rechtsgrundlage, kindgerechte Sprache.
- **Abo/IAP/Paywall:** Google Payments/Subscriptions, Preis, Laufzeit, Trial-Ende,
  automatische Verlaengerung, Kuendigung, Refund/Widerruf, keine Dark Patterns.
- **Barrierefreiheit/eCommerce:** BFSG/EAA pruefen, wenn die App digitale
  Verbraucherleistungen oder eCommerce-Funktionen anbietet; Paywall, Checkout,
  Rechtstextlinks und Account-Loeschung muessen bedienbar und lesbar sein.

#### 5j. Formulierungspruefung

Alle Rechtstexte, Store-Texte, Paywall-Texte und Consent-Texte auf diese Punkte
pruefen:

- Keine Garantien: "rechtssicher", "abmahnungssicher", "100% sicher", "medizinisch
  bewiesen", "garantierter Erfolg" vermeiden.
- Konkreter App-Bezug: App-Name, Developer/Firma, Package/Store-Kontext,
  Kontakt, Datenschutzkontakt und ggf. DPO/Information Officer muessen stimmen.
- Keine Widersprueche: Store Listing, Screenshots, Paywall, In-App-Texte, Privacy
  Policy, Terms, Data Safety und Code muessen dieselbe Wahrheit sagen.
- Klare Zwecke statt Sammelbegriffe: "Verbesserung der App" reicht nicht fuer
  sensible/unerwartete Verarbeitung; Zweck, Datenkategorie, Empfaenger, Rechtsgrundlage
  und Opt-out/Widerruf nennen.
- Plain Language: rechtlich praezise, aber fuer Nutzer verstaendlich; bei Kindern/
  Jugendlichen kindgerechte Zusatzinformationen.
- Keine versteckten Einschraenkungen: Verbraucherrechte, Widerruf, Kuendigung,
  Loeschung, Support und Beschwerdewege duerfen nicht pauschal ausgeschlossen werden.
- Sprache und Locale: Rechtstexte muessen zur Zielgruppe passen. Bei lokalisierten
  Store Listings/App-Sprachen fehlende Rechtstext-Uebersetzungen als Befund markieren.

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
- Consent- und Widerrufsmechanismus je Zweck
- Internationale Markt-/Transferhinweise je Zielmarkt
- Kinder-/Health-/AI-/UGC-/Finanzdaten-Sonderhinweise, falls relevant
- Beschwerde-/Kontaktweg fuer Datenschutzanfragen
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
- DSA/Online-Safety-kompatible Kontakt-, Melde-, Moderations- und Appeal-Prozesse,
  falls Nutzerinhalte, Chat, Kommentare, Profile oder Sharing vorhanden sind
- Haftung und Gewaehrleistung ohne unzulaessige Pauschalausschluesse
- Verfuegbarkeit, Aenderungen, Kuendigung
- In-App-Kaeufe, Abos, Preise, Laufzeiten, Kuendigung
- Trial-/Intro-Offer-Regeln: Preis nach Trial, Abrechnungsfrequenz, Auto-Renewal,
  Kuendigung und Refund/Widerruf muessen vor Kauf klar sichtbar sein
- Drittanbieter/Stores/Billing-Hinweise
- Rechtswahl/Gerichtsstand nur soweit zulaessig gegenueber Verbrauchern
- Kontakt und Beschwerdeweg

**Release-Blocker:**

- AGB schliessen Verbraucherrechte pauschal aus
- AGB widersprechen Store Listing, Paywall oder Privacy Policy
- Kostenpflichtige Features ohne klare Preis-/Abo-/Kuendigungsangaben
- UGC ohne Terms-Akzeptanz, Melde-/Blockierweg oder Moderationsregeln
- Paywall/Trial mit manipulativer Gestaltung oder verstecktem Kuendigungsweg

### Impressum / Anbieterkennzeichnung

Fuer DE/EU geschaeftsmaessige digitale Dienste:

- Vollstaendiger Name/Firma
- Ladungsfaehige Anschrift (kein reines Postfach)
- E-Mail und schnelle elektronische Kontaktmoeglichkeit
- Vertretungsberechtigte Person bei juristischen Personen
- Register, Registernummer, USt-ID, Aufsichtsbehoerde oder Berufsangaben falls einschlaegig
- Leicht erkennbar, unmittelbar erreichbar, staendig verfuegbar
- **In App dauerhaft erreichbar (Settings/About), nicht nur im Store Listing**
- Bei UGC/DSA-relevanten Diensten: Kontaktstelle und ggf. zusaetzliche DSA-
  Kontakt-/Beschwerdeinformationen getrennt pruefen

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
- Abo-Verwaltung/Cancellation-Link in Account-/Settings-Bereich erreichbar
- Keine erzwungenen Umwege, versteckten Schliessen-Buttons oder Dark Patterns

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
- Loeschung entfernt nur Account, aber nicht zuordenbare Daten, obwohl die Policy
  vollstaendige Datenloeschung behauptet

---

## Spezielle Android-/Play-Risikomatrix

| Bereich | Typische technische Signale | Zusaetzliche Pflichtpruefung |
|---|---|---|
| Analytics/Crashlytics | Firebase, Sentry, Amplitude, Events | Consent, SDK-Daten, Drittlandtransfer, Data Safety |
| Ads/AdMob | AD_ID, ads SDKs | Ads Policy, Consent, personalisierte Ads, Kinder |
| Kinder/Families | Target age, kindliche Assets, Games | Families Policy, COPPA/GDPR-K, keine verbotenen IDs |
| Standort | Fine/Background Location | Core Feature, Prominent Disclosure, Permission Declaration |
| Kontakte | READ_CONTACTS, contact picker, social graph | Core Feature, Contact-Declaration falls gefordert, keine Ad-/Profiling-Nutzung ohne klare Grundlage |
| Fotos/Videos | READ_MEDIA_* | Photo Picker bevorzugen, Broad Access Declaration |
| Gesundheit | Health Connect, Mood, Fitness, Medical claims | Health Declaration, Datenschutz, kein irrefuehrender Medizinclaim |
| KI/GenAI | OpenAI/Anthropic/Gemini, image/text generation | AI Content Policy, Reporting, Safety, Datenweitergabe |
| UGC/Community | Posts, Kommentare, Sharing, Chat, Profile | Moderation, Melden/Blockieren, Terms, DSA/Online-Safety-Risiko |
| Finanzen | Budget, Payment, Trading, Kredit | Financial Services Policy, Disclaimer, Lizenzen |
| Abos/IAP | BillingClient, RevenueCat, Paywall | Google Payments/Subscriptions, Preis/Trial/Kuendigung/Widerruf, Dark-Pattern-Check |
| Tagebuch/Private Daten | Room, local DB, cloud sync | Verschluesselung, Backup, Export, Loeschung, Screenshots |
| WebView | JavaScript, file access, remote URLs | Mixed content, JS bridge, Tracking/Cookies, externe Inhalte |
| Push | FCM, notification permissions | Token-Daten, sensitive Notification-Inhalte, Consent |
| Barrierefreiheit | Paywall, Checkout, Account, Legal Links | BFSG/EAA, WCAG/EN 301 549, TalkBack/Keyboard/Contrast |
| Automatisierte Entscheidungen | Ranking, Scoring, Profiling, KI-Empfehlungen | Transparenz, Widerspruch/Rechte, AI-Act/GDPR/State-Privacy-Check |
| Cross-Border Transfer | Cloud/API ausserhalb Zielmarkt | SCC/DPF/Transfer Impact, lokale Transferregeln (PIPL/DPDP/LGPD/PDPL) |

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
- UGC/Chat/Community ohne Melde-, Blockier- und Moderationsprozess
- Generative KI ohne In-App-Reporting/Safety gegen verbotene Inhalte
- Zielmarkt mit lokaler Pflicht (z.B. China/Indien/Brasilien/Suedafrika/Saudi) ohne
  bewertete Notice, Consent- und Transfergrundlage

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
| Prioritaet | Rechtsraum | Relevante Regeln | Gilt wegen | Status | Blocker/Fix |
|---:|---|---|---|---|---|
| 1 | DE/EU | DSGVO, DDG, TDDDG, BGB/EGBGB, BFSG/EAA, DSA, AI Act | | | |
| 2 | UK/USA/CA/AU | UK-GDPR/PECR/OSA, CCPA/CPRA/State Laws, COPPA/FTC, PIPEDA/Law25, APPs | | | |
| 3 | International | PIPL, DPDP, APPI, PIPA, LGPD, PDPA, POPIA, PDPL | | | |

## Formulierungs-Check
| Text/Screen | Problematische Formulierung | Risiko | Korrektur-Hinweis |
|---|---|---|---|

## Play-Console-Checkliste
- [ ] Data Safety passt zu Code und SDKs
- [ ] Privacy Policy URL erreichbar
- [ ] Account deletion beantwortet und verlinkt
- [ ] App Access korrekt
- [ ] Content Rating korrekt
- [ ] Target Audience/Families korrekt
- [ ] Ads/Health/AI/Finance/Permissions Declarations korrekt
- [ ] UGC-/AI-/Health-/Photo-Video-/Contact-/Location-/Accessibility-/Subscription-Sonderregeln geprueft
- [ ] Zielmaerkte und Rechtstext-Sprachen bewertet

## Fix-Reihenfolge
1. BLOCKER zuerst.
2. HOCH vor Release.
3. MITTEL vor Rollout in weitere Laender.
4. NIEDRIG bei naechster Pflege.

## Quellen
- [URL] - [Thema] - abgerufen am YYYY-MM-DD

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
| Datum | Quelle | Thema | Relevanz |
|---|---|---|---|

## Pflichtangaben-Matrix
### EU/DE - Datenschutz
### EU/DE - Impressum
### EU/DE - Widerruf
### Google Play - Data Safety/User Data
### Android - Security/Privacy Controls
### Spezialfaelle - Kinder, Health, AI, Ads, UGC, Finance
### DE/EU - DSA, AI Act, BFSG/EAA, TDDDG
### US/UK/CA/AU - State Privacy, COPPA, FTC, PECR, Online Safety, PIPEDA, APPs
### International - PIPL, DPDP, APPI, PIPA, LGPD, PDPA, POPIA, PDPL

## Sprach-Anforderungen pro Markt

## Aktuelle Abmahn-Hotspots (Stand YYYY-MM)

## App-Audit-Log
| Datum | App | Version | Status | Blocker | Hoch | Commit/Notiz |
|---|---|---|---|---:|---:|---|

## Wiederverwendbare Befundmuster

## Muster-Klauseln (mit Quelle)
```

**Diff-Logik:** Neue Erkenntnisse gegenueber dem gespeicherten Stand hervorheben
("**Aenderungen seit letzter Recherche**"). Veraltete Eintraege (>90 Tage) als
"zu verifizieren" markieren. Jede Pflichtangabe mit Quell-URL + Abrufdatum.

**Keine Secrets, echten Kundendaten, privaten Adressen oder Token in diese Datei
schreiben**, ausser der Benutzer verlangt explizit genau diese Ablage.

### Schritt 8 — Commit + Push

`<WORKSPACE_ROOT>/tools/rechtssicherheit.md` committen und pushen — gemaess
`~/.codex/rules/parallel-sessions-git.md`:

1. Nur eigene Dateien namentlich stagen (NIE `git add -A`)
2. Commit mit fortlaufender Nummer: `#NNNN - rechtssicherheit audit [App]`
3. `git fetch origin && git rebase origin/main`
4. `git status --short` pruefen
5. `git push`

---

## Typische Fix-Hinweise

Der Skill darf konkrete technische Fixes vorschlagen oder implementieren:

- Settings/About Links zu Datenschutz, Terms, Impressum, Loeschung
- Consent-Screen vor Analytics/Ads/Cloud/KI
- Toggles fuer Analytics/Crashlytics/Ads inkl. Widerruf
- Runtime-Permission-Erklaerungen an Feature-Kontext koppeln
- Unnoetige Permissions entfernen
- Android Photo Picker statt breiter Medien-Permissions
- UGC Terms-Akzeptanz, Report-/Blockier-Funktion und Moderationsflow ergaenzen
- KI-In-App-Reporting, Safety-Filter und AI-/Deepfake-Hinweise ergaenzen
- Abo-/Trial-/Paywall-Texte klarer formulieren und Cancellation-Link einbauen
- Barrierefreiheitsprobleme in Legal-/Checkout-/Account-Flows als Release-Blocker behandeln
- Backup-Regeln fuer sensitive Daten
- `usesCleartextTraffic=false` oder Network Security Config bereinigen
- Sensitive Logs entfernen
- Secrets in `$HOME/SK/` verlagern und Repo bereinigen
- Store-/Data-Safety-Checkliste als Markdown erzeugen
- Rechtstext-Platzhalter oder falsche App-Namen korrigieren

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
- ❌ Researcher ohne Limits (max 50 Ergebnisse / 15 Fetches / 10 Min)
- ❌ Wissensbasis nicht updaten am Ende (dann lernt das System nicht dazu)
- ❌ App-Pruefung ohne vorherige Recherche (dann fehlen Pflichtangaben-Kriterien)
- ❌ Rechtstexte nur in Deutsch+Englisch belassen ohne Pruefung welche Laender
  die Landessprache zwingend verlangen
- ❌ DSA, AI Act, BFSG/EAA, TDDDG/ePrivacy, COPPA/State-Privacy, Online Safety,
  DPDP/PIPL/LGPD/POPIA/PDPL ignorieren, wenn Features oder Zielmaerkte sie triggern
- ❌ UGC, Chat, Sharing, KI, Health, Kinder, Abo, Ads oder sensible Permissions als
  "normale App-Funktion" behandeln, ohne Sonderpolicy-Gate
- ❌ Fehlende Uebersetzungen oder nicht bewertete Store-Laender als still freigegeben
  behandeln
- ❌ Fremde oder unklare Aenderungen im Repo mitcommitten
- ❌ Dateien ausserhalb des aktuellen Workspaces bearbeiten, ausser explizit angefordert
- ❌ `~/Codex/`, `~/proggs/` oder andere Home-Unterordner als Standard-Arbeitsverzeichnis
  verwenden, wenn ein Workspace/Repo-Root verfuegbar ist
- ❌ Konkrete Rechtstexte generieren die wie eine Anwaltsdienstleistung wirken
  (max. Muster-Hinweise mit Quelle und Empfehlung "durch Anwalt pruefen lassen")

---

## Beispiel-Dialog

**Benutzer:** "starte den Skill Rechtssicherheit und pruefe BestJournalAndroid"

**Codex:**
> Ich starte ein Rechtssicherheits-Audit fuer BestJournalAndroid.
>
> **Disclaimer:** Das ist eine technische Pruefhilfe und ersetzt keine anwaltliche
> Beratung. Vor Release ist eine Pruefung durch einen Fachanwalt fuer IT-Recht Pflicht.
>
> Ablauf:
> 1. Lese Wissensbasis aus `tools/rechtssicherheit.md` (oder lege sie neu an).
> 2. Starte 5 parallele Researcher (DE/EU, US/UK/CA/AU, International,
>    Play-Store-Policies, Enforcement-/Abmahn-Trends). Laufzeit: ~5-8 Minuten.
> 3. Vollscan der Codestruktur in `BestJournalAndroid/` (Datei-Inventur + Legal-Signale).
> 4. Detail-Pruefung: Manifest/Permissions, SDKs, lokale Speicherung, Logs, Backup,
>    Rechtstexte und deren Platzierung, Consent, Store-Metadaten.
> 5. Erstelle Befund-Bericht mit Codestruktur-Vollscan-Matrix, Code-vs-Text-vs-Play-Matrix,
>    Android-Sicherheitscheck und Play-Console-Checkliste.
> 6. Aktualisiere Wissensbasis und committe.
>
> Loslegen?

[Researcher laufen parallel → Konsolidierung → Vollscan → Detail-Pruefung →
Bericht → Wissensbasis-Update → Commit+Push]

---

## Zusammenspiel mit anderen Skills

- **`uebersetzung`**: Bei fehlenden Sprachversionen — Vorschlag, den Uebersetzungs-
  Skill zu starten.
- **`string-extraktor`**: Wenn Rechtstexte hardcodiert im Code stehen statt in strings.xml.
- **`superintelligenz` / `selbstbeobachtung`** (Direktiven #1+#2): Neue Erkenntnisse
  (z.B. neues Abmahn-Urteil, neue Play-Policy) werden in `tools/rechtssicherheit.md`
  persistiert — Compound Intelligence Effect.
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
