---
name: rechtssicherheit
description: >
  Prueft Android-Apps vor Release als technische Pruefhilfe (KEINE anwaltliche Beratung)
  auf Datenschutz-, Impressums-, Widerrufs-, Google-Play-Compliance- und Abmahnrisiken.
  Vollscan der Codestruktur (Manifest, Permissions, SDKs, Backup, Logs, Secrets), Abgleich
  Code vs. Rechtstexte vs. Play Console, Recherche aktueller Pflichtangaben fuer alle
  Zielmaerkte (EU, UK, USA, Kanada, Australien, Neuseeland, Tuerkei, Asien, LATAM, MENA,
  Afrika — Russland und China-Mainland bewusst ausgeschlossen). Liest Roentgen-Output wenn
  vorhanden und ermoeglicht damit den UWG-Werbeaussagen-Check.
  Deutsche Trigger: "starte den Skill Rechtssicherheit", "pruefe [App] auf Rechtssicherheit",
  "Rechtssicherheit fuer [App]", "DSGVO-Check fuer [App]", "ist [App] abmahnungssicher",
  "Abmahnungscheck [App]", "Play-Store rechtskonform pruefen", "Rechts-Audit [App]",
  "Data-Safety-Check", "Account-Loeschung pruefen", "Widerruf pruefen", "Impressum pruefen",
  "AGB pruefen", "Datenschutz pruefen", "Consent pruefen", "Tracking pruefen",
  "SDKs pruefen", "Permissions pruefen", "Play Console pruefen", "Google Play Data Safety",
  "Android-App-Release", "rechtliche Pflichttexte", "Datenschutzerklaerung pruefen",
  "Nutzungsbedingungen pruefen".
  Auch ohne exakten Trigger nutzen bei Themen wie DSA, AI Act Art. 50, BFSG, KVKK (Tuerkei),
  LFPDPPP (Mexiko), PDPO (Bangladesch/Hongkong), PIPA, APPI, DPDP, LGPD, POPIA, PDPL,
  Datenschutz-Compliance, Abmahnrisiken oder Play-Store-Release einer Android-App.
  Markt-Details und Pflichtdokumente: siehe `references/`-Unterordner im Skill.
invocation: user
---

# Skill: Rechtssicherheit

> **Wichtiger Disclaimer (PFLICHT — am Anfang UND am Ende des Berichts wiederholen):**
> Dieser Skill ist eine **technische Pruefhilfe** und ersetzt **KEINE anwaltliche Beratung**.
> Er kann Luecken, Inkonsistenzen, Play-Policy-Risiken und typische Abmahn-Fallstricke
> markieren — er gibt **keine Garantie** fuer Rechtssicherheit oder Abmahnungssicherheit.
> Fuer eine verbindliche Rechtspruefung MUSS vor Release ein **Fachanwalt fuer IT-Recht**
> konsultiert werden.

## Inhaltsverzeichnis

1. [Grundsatz: Keine Garantien](#grundsatz-keine-garantien)
2. [Ziel](#ziel)
3. [Trigger und Pruefumfang](#trigger-und-pruefumfang)
4. [Plattformneutrale Pfade](#plattformneutrale-pfade)
5. [Skill-Stand und Recherche-Aktualitaet](#skill-stand-und-recherche-aktualitaet)
6. [Markt-Prioritaet und Reference-Karte](#markt-prioritaet-und-reference-karte)
7. [Ablauf (8 Hauptschritte + Schritt 1.5)](#ablauf-8-hauptschritte-schritt-15)
8. [Schweregrade](#schweregrade)
9. [Bundle-Ressourcen](#bundle-ressourcen)
10. [Was niemals passieren darf](#was-niemals-passieren-darf)
11. [Zusammenspiel mit anderen Skills](#zusammenspiel-mit-anderen-skills)
12. [Beispiel-Dialog](#beispiel-dialog)
13. [Abschluss-Meldung](#abschluss-meldung)

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

## Ziel

Eine Android-App vor Release so pruefen, dass sie technisch, dokumentarisch und in der
Play-Store-Deklaration so weit wie pruefbar abmahnungsresistent ist. Der Skill prueft nicht
nur Rechtstexte, sondern auch ob die App technisch das tut, was Datenschutz, Nutzungs-
bedingungen, Impressum, Widerrufsbelehrung und Google Play Data Safety behaupten.

**Pflichtziele:**

1. Pflichtdokumente vorhanden: Datenschutzerklaerung, Nutzungsbedingungen, Impressum/
   Anbieterkennzeichnung, Widerruf bei kostenpflichtigen digitalen Inhalten, Support/Kontakt,
   Account-/Datenloeschung wenn Accounts existieren.
2. Pflichtdokumente enthalten die noetigen Angaben fuer Zielmaerkte und Features.
3. App verlinkt diese Dokumente korrekt: Store Listing, Onboarding, Consent, Settings,
   About, Account deletion, Paywall/Checkout.
4. Google Play Data Safety, Play-Console-Deklarationen, Manifest-Permissions, SDKs,
   Netzwerkverhalten und Rechtstexte sind konsistent.
5. Sicherheits- und Datenschutztechnik reduziert rechtliche Risiken: minimale Permissions,
   keine unnoetige Datenerhebung, sichere Speicherung, Backup-Regeln, TLS, keine Secrets
   im Repo, keine sensiblen Logs.
6. Alle relevanten Sprachen/Locales sind abgedeckt oder als Release-Blocker markiert.
7. Der Bericht trennt klar zwischen "rechtlich verbindlich durch Anwalt klaeren" und
   "technisch im Repo nachweisbar".

## Trigger und Pruefumfang

Nutze diesen Skill immer, wenn es um Datenschutz-Compliance, Abmahnungsrisiken, Play-Store-
Release oder rechtliche Pflichttexte einer Android-App geht — auch ohne expliziten Trigger.

**Auslosend sind insbesondere Themen wie:**

- Datenschutzerklaerung, DSGVO/GDPR, CCPA/CPRA, UK-GDPR, PIPL, DPDP, APPI, PIPA, LGPD, POPIA, PDPA, PDPL, KVKK, LFPDPPP, PDPO oder anderen Datenschutzpflichten
- Impressum/Anbieterkennzeichnung, AGB/Nutzungsbedingungen, Widerrufsbelehrung, Consent, Account-/Datenloeschung, Support/Kontakt oder Pflichtlinks in der App
- Google Play Data Safety, Play-Console-Deklarationen, Permissions, SDKs, Tracking, Ads, Analytics, Crashlytics, KI/GenAI-APIs, Backups, Logs oder Secrets
- DSA, AI Act, BFSG/EAA, TDDDG/ePrivacy, COPPA, Online Safety Act, DPDP/PIPL/LGPD/POPIA/PDPL ignorieren
- UGC, Chat, KI, Health, Kinder, Abos, Ads oder sensitive Permissions

**Pflicht-Pruefumfang (Android-Apps vor Release):**

- Datenschutz/DSGVO/GDPR und internationale Aequivalente
- Google Play Data Safety, User Data Policy, Spezial-Policies
- Permissions, SDKs, Datenfluesse, Drittlandtransfers
- Consent, Tracking, Ads, Analytics, Crashlytics
- Account-Erstellung und Account-/Datenloeschung
- Impressum/Anbieterkennzeichnung
- AGB/Nutzungsbedingungen
- Widerruf bei digitalen Inhalten, IAP und Abos
- Internationale Zielmaerkte und Sprachpflichten
- Barrierefreiheit (BFSG/EAA, WCAG, EN 301 549) bei Legal-/Checkout-/Account-Flows

## Plattformneutrale Pfade

Die Wissensbasis liegt **workspace-lokal**, nicht in einem persoenlichen Home-Unterordner.

**Zielpfad (plattformneutral):** `<WORKSPACE_ROOT>/tools/rechtssicherheit.md`

`<WORKSPACE_ROOT>` ist das aktuelle Arbeitsverzeichnis bzw. der Repo-Root, in dem die zu
pruefende App liegt.

| Plattform   | Beispielpfad                                       |
|-------------|----------------------------------------------------|
| Windows     | `%USERPROFILE%\proggs\tools\rechtssicherheit.md`   |
| macOS/Linux | `$HOME/proggs/tools/rechtssicherheit.md`           |
| Generisch   | `<repo-root>/tools/rechtssicherheit.md`            |

**Verboten:** Harte Pfade wie `C:\Users\...` oder `/Users/barwa/...` als Default. Wenn der
Benutzer eine konkrete Datei ausserhalb des Workspaces nennt, darf der Skill dort lesen/schreiben.

**Platzhalter im Skill** (gelten in SKILL.md, allen References und allen Skript-Aufrufen):

| Platzhalter | Bedeutung | Beispiel |
|---|---|---|
| `<WORKSPACE_ROOT>` | Repo-/Workspace-Root — Eltern-Verzeichnis aller geprueften Apps | `$HOME/proggs/` (macOS/Linux), `%USERPROFILE%\proggs\` (Windows) |
| `<APP_DIR>` | Absoluter Pfad zum App-Wurzelverzeichnis (enthaelt `app/`, `gradle/`, `settings.gradle.kts`, etc.) | `$HOME/proggs/BestJournalAndroid/` |
| `<APP_NAME>` | Letztes Pfad-Segment von `<APP_DIR>` (ohne Trennzeichen) | `BestJournalAndroid` |

Beim Skript-Aufruf wird `<APP_DIR>` als Argument uebergeben. `<APP_NAME>` leitet sich automatisch
ab. `<WORKSPACE_ROOT>` kann der Skill aus `<APP_DIR>/..` errechnen oder aus der Umgebungsvariable
`WORKSPACE_ROOT` lesen, falls gesetzt.

## Skill-Stand und Recherche-Aktualitaet

**Skill-Stand: 2026-05-17.**

Aktualisierungen seit dem alten Skill-Stand (2026-04-28) sind komplett in
`references/markt-de-eu.md` (EU AI Act Art. 50, EU Data Act, BFSG, TDDDG-PIMS, DPF) und den
weiteren `markt-*.md`-Dateien dokumentiert.

**Pflicht-Aktualisierung** der Recherche-Quellen bei:

- Letzter Recherche aelter als 30 Tage
- Google Play Policies betroffen
- Health, Kinder, Standort, Kontakte, Medien, SMS/Call Logs, Finanzdaten, KI/GenAI, Ads,
  Analytics, User Generated Content oder Accounts vorkommen
- App in neue Laender/Sprachen/SDKs/Monetarisierungsmodelle ausgerollt wird
- Benutzer sagt "aktuell", "neueste" oder "Release"

**Quellenprioritaet (verbindlich):**

1. **Primaerquellen** — Gesetzestexte, offizielle Regulierer, Google/Android-Policy
2. **Sekundaerquellen** — Fachanwaelte, Fachverbaende, Behoerden-Erklaerungen
3. **News/Blogs** — nur als Hinweis, NIE alleinige Grundlage

## Markt-Prioritaet und Reference-Karte

Die vollstaendige Master-Tabelle (Jurisdiktions-Gates, Sprachpflichten, Release-Blocker pro
Markt) und die Reference-Karte stehen in `references/markt-uebersicht.md`. Beim Audit-Start
diese Datei lesen.

**Bewertungsreihenfolge:**

1. **DE/EU** (hoechste Prioritaet) — `references/markt-de-eu.md`
2. **Englischsprachige Maerkte** (UK, USA, Kanada, Australien, Neuseeland) — `references/markt-uk-us-ca-au-nz.md`
3. **Internationale Maerkte** — `references/markt-tuerkei-osteuropa.md`, `markt-asien.md`, `markt-sa-pakistan-bangladesch.md`, `markt-latam.md`, `markt-mena-afrika.md`

**Ausgeschlossen in dieser Programmierumgebung:**

- Russland (ru-Locale)
- China-Mainland (zh-Hans-Locale, PIPL)

**Wenn ein Markt nicht bewertet werden kann: BLOCKER fuer Rollout in diesem Markt.**

## Ablauf (8 Hauptschritte + Schritt 1.5)

Strikt in dieser Reihenfolge. Direktive 3 — wenn ein Schritt fehlschlaegt, melden, nicht still
weitermachen. Schritt 5 enthaelt die zehn Sub-Schritte 5a-5j; das eigentliche Berichtsschreiben
beginnt erst in Schritt 6, wenn alle Pruefungen aus Schritt 5 inkl. der Compliance-Artefakte
(5j) abgeschlossen sind.

### Schritt 1 — Scope klaeren

Wenn der Benutzer den App-Namen nicht genannt hat, **einmal kurz fragen**:

> Welche App soll ich pruefen? (z.B. BestJournalAndroid, BestJournalFrank, QuizVerse)

Wenn weitere Infos noetig sind und nicht aus dem Repo erkennbar, Fragen **gesammelt** stellen
(nicht einzeln):

1. In welchen Laendern/Sprachen soll die App veroeffentlicht werden?
2. Gibt es In-App-Kaeufe, Abos, Werbung, Affiliate-Links oder externe Zahlungen?
3. Gibt es Accounts, Cloud-Sync, Backups, Export, Import oder Datenloeschung?
4. Werden Firebase, Analytics, Crashlytics, Ads, KI-APIs oder andere SDKs genutzt?
5. Richtet sich die App an Kinder oder kann sie fuer Kinder attraktiv wirken?
6. Gibt es sensible Daten: Gesundheit, Tagebuch, Standort, Kontakte, Fotos, Audio, Kamera, Kalender, Finanzdaten, Religion, Sexualitaet, biometrische Daten?
7. Gibt es UGC, Chat, Sharing, Moderation, KI-generierte Inhalte?
8. Welche Barrierefreiheits-Stufe ist geplant (BFSG/EAA betrifft B2C-Dienste)?

Nicht auf Antworten warten, wenn der Repo-Zustand eine konservative Annahme erlaubt. Unklare
Punkte im Bericht als Annahmen markieren.

### Schritt 1.5 — Roentgen-Integration

**Detail siehe `references/roentgen-integration.md`.**

Skript: `scripts/check-roentgen-output.sh <APP_DIR>` ausfuehren.

| Ergebnis | Aktion |
|---|---|
| Roentgen-Output vorhanden | Komplett einlesen, Schritt 4 (Vollscan) DARF entfallen, Werbeaussagen-vs-Feature-Matrix uebernehmen |
| Roentgen-Output fehlt | Empfehlen: Roentgen-Skill zuerst starten. Falls Benutzer ablehnt: Vollscan selbst durchfuehren |
| Roentgen-Output veraltet | Vollscan selbst, Roentgen-Datum markieren |

Nur mit Roentgen-Output ist der **UWG-Werbeaussagen-Check (§5/§5a)** moeglich.

### Schritt 2 — Wissensbasis laden

Pruefe ob `<WORKSPACE_ROOT>/tools/rechtssicherheit.md` existiert.

| Zustand | Aktion |
|---------|--------|
| Existiert | Komplett einlesen — Wissensbasis aus frueheren Sessions, wird am Ende aktualisiert |
| Fehlt | Nach der Recherche (Schritt 3) zum ersten Mal anlegen |

Dem Benutzer kurz melden: *"Lese Wissensbasis aus tools/rechtssicherheit.md..."* oder
*"Lege Wissensbasis neu an."*

### Schritt 3 — Internet-Recherche (6 Researcher parallel)

**Pflicht: 6 Researcher-Agenten in EINER Nachricht parallel starten** (nicht sequentiell).
Jeder Researcher: max 50 Ergebnisse, max 15 Web-Fetches, max 10 Minuten Laufzeit, max 2000
Woerter Prompt (siehe `~/.claude/rules/agent-and-researcher-rules.md`). Die Aufteilung auf
6 statt 5 ist bewusst — Researcher 4 deckte vorher 11 Rechtsraeume ab und kam mit dem
50-Ergebnis-Limit nicht in die Tiefe. Jetzt liegt die Last gleichmaessiger.

Dem Benutzer vor dem Start sagen:
> "Ich starte 6 parallele Researcher fuer DE/EU (inkl. AI Act + Data Act + BFSG + DPF),
> US/UK/CA/AU/NZ, Asien-A (IN/JP/KR/TW/HK/SG), Asien-B + Suedasien (TH/ID/VN/LK/PK/BD),
> LATAM + MENA + Afrika + TR/UA, Play-Store-Policies + Enforcement-/Abmahn-Trends.
> Laufzeit: ~5-8 Minuten."

**Researcher-Aufteilung:**

| # | Fokus | Reference fuer Details |
|---|-------|------------------------|
| 1 | DE/EU + AI Act + Data Act + BFSG + DPF-Status | `markt-de-eu.md` + `ai-act-art-50.md` |
| 2 | UK + USA + Kanada + Australien + Neuseeland | `markt-uk-us-ca-au-nz.md` + `uk-vertreter-pflicht.md` |
| 3 | Indien + Japan + Korea + Taiwan + Hongkong + Singapore | `markt-asien.md` |
| 4 | Thailand + Indonesien + Vietnam + Sri Lanka + Pakistan + Bangladesch | `markt-asien.md` + `markt-sa-pakistan-bangladesch.md` |
| 5 | Brasilien + Mexiko + Argentinien + Tuerkei + Ukraine + MENA (Saudi/UAE) + Suedafrika | `markt-latam.md` + `markt-tuerkei-osteuropa.md` + `markt-mena-afrika.md` |
| 6 | Google Play Policies + Enforcement-/Abmahn-Trends | `play-policies.md` + `enforcement-trends.md` |

**Prompt-Muster pro Researcher:**

```
Recherchiere fuer [FOKUS] die aktuellen (Stand {Monat/Jahr}) rechtlichen
Pflichtangaben einer Android-App im Google Play Store.

Liefere strukturiert zurueck:
1. PFLICHTANGABEN-LISTE
2. MUSTER-KLAUSELN mit Quelle
3. SPRACHANFORDERUNG
4. SANKTIONEN (Bussgelder, Abmahnrisiko, Play-Enforcement)
5. AKTUELLE AENDERUNGEN (letzte 12 Monate)
6. SPEZIALREGULIERUNG (Kinder, Health, KI, UGC, Ads, Abos, Barrierefreiheit, Cross-Border)
7. QUELLEN (URLs + Abrufdatum + Quellenklasse)

Limits: max 50 Ergebnisse, max 15 Web-Fetches, max 10 Minuten.
Quellenprioritaet: Primaer > Sekundaer > News.
Bei Netzwerkfehlern: das zurueckgeben was da ist, nicht crashen.
```

Wenn ein Researcher fehlschlaegt: SOFORT dem Benutzer auf Deutsch melden, die anderen
weiterlaufen lassen, nicht still weitermachen.

### Schritt 4 — Vollscan der Codestruktur

> Nur ausfuehren wenn Schritt 1.5 keinen Roentgen-Output gefunden hat.

Skript: `scripts/scan-legal-signals.sh <APP_DIR>` ausfuehren. Es liefert kategorisierte
Treffer fuer Legal-Dokumente, Permissions, SDKs, Datenverarbeitung, Health, KI, Kinder, UGC,
Tracking, Ads, Cleartext, Secrets, WebViews.

**Pflicht: Treffer klassifizieren** nach Tabelle:

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

**Pflicht-Ergebnis pro Treffergruppe:** "in Dokumenten erwaehnt?", "in App verlinkt?",
"in Data Safety/Play Console zu deklarieren?", "Consent noetig?", "Release-Blocker?".

Wenn keine Treffer in einem erwarteten Bereich: explizit notieren ("Keine Account-Erstellung
gefunden" o.ae.).

### Schritt 5 — Detail-Pruefung

> *Schritt 5 prueft 10 Aspekte:*
> *5a Projekt/Package · 5b Manifest+Permissions · 5c SDKs · 5d Lokale Speicherung ·*
> *5e Rechtstexte · 5f Consent · 5g Store Listing · 5h UK-Vertreter ·*
> *5i UGC/KI/Barrierefreiheit · 5j Compliance-Artefakte (muss vor Schritt 6 laufen)*

**Vorbereitung — IMMER zuerst lesen:** `references/enforcement-trends.md` enthaelt die
aktuellen Abmahn-Hotspots und Bussgeld-Trends in DE/EU plus internationale Enforcement-News.
Lies diese Datei am Anfang von Schritt 5 — sie praegt die Risiko-Einschaetzung (Schweregrad)
jedes Befunds. Beispiel: Wenn die Trends-Datei "Cookie-Nudging seit 2025 abmahnfaehig"
ausweist und du im Code einen optisch dominanten Accept-Button findest, ist das ein
🔴 BLOCKER (nicht 🟡 MITTEL).

> Hinweis: Die folgenden Sub-Schritte 5a-5d klassifizieren die Treffer aus Schritt 4
> (`scripts/scan-legal-signals.sh`) bzw. den Roentgen-Output. Die rg-Suchpattern stehen
> zentral im Skript — die SKILL.md listet hier die Pruef-Aspekte, nicht die Suchbefehle.

#### 5a. Projekt und Package

Aus dem Scan erfassen: App-Name, Package-ID, Variante/Flavor, minSdk, targetSdk, compileSdk,
Store-Metadaten (`fastlane/metadata`, `play-store-metadata`, README, Website), verwendete
Sprachen/Locales (`app/src/main/res/values*`). Abgleichen mit Privacy Policy
(Anbieter-Name + App-Name muessen stimmen) und mit den Markt-Pflichtsprachen aus den
`references/markt-*.md`-Dateien.

#### 5b. Manifest, Permissions, Android-Komponenten

Aus dem Scan erfassen: alle `uses-permission`-Eintraege, Exported-Komponenten, Backup-Regeln,
Network Security Config, Cleartext-Traffic, debuggable-Flag.

**Besonders kritisch und einzeln im Bericht zu fuehren:**
- `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- `READ_CONTACTS` (ab April-2026 nur noch via Android Contact Picker — siehe `references/play-policies.md`)
- `CAMERA`, `RECORD_AUDIO`
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `MANAGE_EXTERNAL_STORAGE`
- `POST_NOTIFICATIONS` bei sensiblen Inhalten
- `QUERY_ALL_PACKAGES`
- `AD_ID`
- Health Connect / Gesundheitsdaten
- Exported Activities/Services/Receivers/Providers
- `allowBackup`, `fullBackupContent`, `dataExtractionRules`
- `usesCleartextTraffic` und Network Security Config

Jede Permission gegen Feature, Rechtstext, Prominent Disclosure, Runtime-Permission-Dialog
und Data Safety abgleichen — keine Permission darf "verwaist" sein.

#### 5c. SDKs, Dependencies, Datenabfluss

Aus dem Scan erfassen: alle SDKs (Firebase, Crashlytics, AdMob, Adjust, Sentry, OpenAI,
Anthropic, etc.) sowie alle Netzwerk-/Token-/Secret-Treffer.

Pruef-Aspekte:
- Welche SDKs sammeln automatisch welche Datenkategorien (IP, Device-IDs, Crash-Logs,
  Analytics Events, Advertising ID, Firebase Installation ID, Push Token)?
- Drittlandtransfer ausserhalb EU/EWR? Welche Transfergrundlage (SCC/DPF/PIPL/DPDP/LGPD/PDPL)?
- AVV/DPA fuer jeden Dienst vorhanden (siehe `references/pflichtdokumente.md` interne Artefakte)?
- Privacy Policy spezifisch genug fuer jeden SDK-Zweck?
- Debug-/Test-/Staging-Endpunkte aus Release entfernt?
- Werden Secrets aus zentraler Secrets-Ablage (`$HOME/SK/`) statt aus dem Repo geladen?

#### 5d. Lokale Speicherung, Logs, Backup

Aus dem Scan erfassen: SharedPreferences, DataStore, RoomDatabase, File-Zugriffe,
Log-Aufrufe, KeyStore/Encrypted-Hinweise.

Pruef-Aspekte:
- Sensitive Daten verschluesselt (KeyStore, MasterKey, EncryptedSharedPreferences)?
- Tagebuch-/Gesundheits-/Auth-/Token-Daten nicht in Backups/Screenshots/Clipboard/Logs/Crashreports?
- Backup-Regeln schliessen sensitive Daten aus oder verlangen E2E-Schutz?
- Export/Import bewusst dokumentiert und sicher?
- Loeschfunktion loescht lokale Daten + Sync-Daten + Backups soweit moeglich?

#### 5e. Rechtstexte und UI-Links

##### Pre-Existence-Check vor missingDocs-Findings (FIN-010)

> Diesen Check IMMER ausfuehren BEVOR ein `missingDocs`-Finding erstellt wird.
> Verhindert False-Positive-Befunde wenn Legal-Dokumente in `assets/` liegen
> (FIN-014: 4 von 6 missingDocs-Findings waren False Positives bei BestJournalAndroid).

**Ablauf:**

1. Pruefe ob `roentgen-report.json` einen `layer1_5_assets`-Block enthaelt.
   - Wenn JA: Lies die Liste der dort inventarisierten Dateien.
   - Wenn NEIN: Fuehre selbst einen Quick-Glob durch:
     `Glob app/src/main/assets/**/*.{html,htm,md,txt}` am `<APP_DIR>`.

2. Fuer jeden potenziellen `missingDocs`-Befund (Datenschutz, AGB, Impressum, Widerruf usw.):
   a. Prüfe ob ein passender Eintrag im Assets-Inventar existiert
      (Typ: privacy, imprint, terms, widerruf, help, sonstige).
   b. Wenn **im Assets-Inventar vorhanden**:
      - KEIN `missingDocs`-Finding erstellen.
      - Stattdessen: Pruefe die Abdeckung der Locales.
        - Alle Pflicht-Locales enthalten? → kein Finding.
        - Mindestens eine Pflicht-Locale fehlt? → 🟡 **MITTEL** `coverage-gap`-Finding
          mit expliziter Liste der fehlenden Locales.
          Beispiel-Format:
          ```
          type: coverage-gap
          doc: NUTZUNGSBEDINGUNGEN
          existsIn: [de, en, fr, es, ...]
          missingIn: [ar, hi, pt-BR, ...]
          severity: 🟡 MITTEL
          note: "Dokument existiert in assets/legal/, fehlt aber in N Locales."
          ```
   c. Wenn **nicht im Assets-Inventar vorhanden**:
      - Regulaeres `missingDocs`-Finding mit dem normalen Schweregrad-Schema erstellen.

**Was NIEMALS passieren darf (FIN-010):**
- `missingDocs`-Finding erstellen ohne vorherigen Assets-Glob oder Pruefung von
  `layer1_5_assets` im Roentgen-Output
- Dokument als fehlend markieren nur weil es nicht in `res/values/strings.xml` vorkommt
- `coverage-gap`-Findings mit 🔴 BLOCKER bewerten, wenn das Dokument grundsaetzlich
  existiert — BLOCKER ist nur zulassig wenn das Dokument vollstaendig fehlt

**Inhaltliche Pflichtangaben pro Dokument** (Datenschutzerklaerung, AGB, Impressum, Widerruf,
Account-/Datenloeschung): vollstaendige Checklisten in `references/pflichtdokumente.md`.
Diese Checklisten sind die Grundlage fuer die Bewertung jedes Pflichtdokuments — pruefe jedes
Dokument der App gegen die jeweilige Checkliste, nicht nur gegen das "Pflicht-Platzierung"-Schema
unten.

Pflicht-Platzierungen:

| Pflicht-Platzierung | Typisches Muster |
|---|---|
| Onboarding/Consent-Screen | Link zur DSE VOR Datenerhebung |
| Settings-Screen | "Datenschutz", "Nutzungsbedingungen", "Impressum" dauerhaft erreichbar |
| About-Screen | Impressum-Link mit Kontaktdaten |
| Consent-Widerruf | Benutzer kann Zustimmung nachtraeglich widerrufen (DSGVO Art. 7 Abs. 3) |
| Paywall/Checkout | Widerrufsbelehrung VOR Kaufabschluss |
| Account-Deletion | In-App-Pfad UND Weblink |

Sprach-Pruefung: `app/src/main/res/values-XX/strings.xml` auflisten. Abgleich mit Play-
Store-Release-Sprachen + Markt-Pflichtsprachen aus `references/markt-*.md`.

#### 5f. Consent und Widerruf

- Einwilligung VOR nicht notwendiger Datenverarbeitung
- Kein vorangekreuztes Consent
- Widerruf so einfach wie Zustimmung
- Consent-Zwecke getrennt: Analytics, Crashlytics, Ads, personalisierte Ads, Cloud-Sync, Newsletter, KI-Verarbeitung
- App funktioniert auch ohne freiwillige Einwilligungen
- Alter/Kinderschutz, falls relevant
- Prominent Disclosure direkt im Nutzungsfluss (nicht nur Settings/PP)

#### 5g. Store Listing und Play Console

Siehe `references/play-policies.md` fuer komplette Pflicht-Deklarations-Liste +
Play-Console-Checkliste.

#### 5h. UK-Vertreter-Pflicht (UK-GDPR Art. 27)

Vollstaendiges Verfahren mit 3-Trigger-Bedingung + Repo-Pruefung + Option-A/B-Empfehlung:
**`references/uk-vertreter-pflicht.md`**.

Skript: `scripts/check-uk-data-processing.sh <APP_DIR>` automatisiert die Repo-Pruefung.
Wenn Skript Exit-Code 0 (Datenverarbeitung gefunden): Option B (UK ausschliessen) empfehlen.
Wenn Exit-Code 1: UK darf in Country-Availability bleiben.

#### 5i. UGC, KI/GenAI, Moderation, Barrierefreiheit

- UGC: Terms-Akzeptanz, Report-/Blockier-Funktion, Moderationsflow, DSA-Pflichten
- KI: Siehe `references/ai-act-art-50.md` (insb. ab 02.08.2026)
- Google Play AI Content Policy: In-App-Flagging-Button
- Barrierefreiheit: BFSG/EAA seit 28.06.2025, WCAG 2.1 AA via EN 301 549, KMU-Ausnahme <10 MA & <2 Mio. EUR

#### 5j. Interne Compliance-Artefakte

Skript: `scripts/check-compliance-artifacts.sh <APP_DIR>` ausfuehren. Es prueft ob 10 interne
Compliance-Artefakte im Repo nachweisbar sind: VVT/ROPA (DSGVO Art. 30), DSFA/DPIA (Art. 35),
TIA (Art. 44 ff.), TOMs (Art. 32), AVV-Liste (Art. 28), SCCs/DPF/BCR (Art. 46),
Loeschkonzept (Art. 5 Abs. 1e + Art. 17), Datenpannen-Meldeplan (Art. 33/34),
AI-System-Risikoklassifizierung (AI Act Art. 6/9/13), DSA-Beschwerde-/Kontaktstelle (Art. 11).

Details + Pruefkriterien siehe `references/pflichtdokumente.md`.

Skript-Ergebnis fuer den Bericht (Schritt 6) verwenden:
- Exit-Code 0 (alle 10 nachweisbar): Block "Interne Compliance-Artefakte" im Bericht-Template OK markieren
- Exit-Code 1 (mindestens 1 fehlt): jedes fehlende Artefakt im Bericht als 🟠 HOCH einzeln auflisten

Wichtig: Dieser Schritt 5j MUSS vor Schritt 6 (Bericht) laufen, damit die Compliance-Befunde
in den Bericht-Block aufgenommen werden koennen.

### Schritt 5k — Cluster-Clarification-Logik (FIN-008)

> Diesen Schritt IMMER nach Abschluss aller Sub-Schritte 5a–5j und VOR dem
> Berichtsschreiben (Schritt 6) durchfuehren.

Wenn mehrere Strings oder Findings inhaltlich widersprüchlich wirken
(z.B. "4 Perspektiven" in einem String vs. "5 Perspektiven" in einem anderen,
oder zwei Befunde die sich gegenseitig ausschliessen), gilt folgende Logik:

**Erkennungsmerkmal:** Ein Cluster-Finding liegt vor wenn
- mindestens 2 Befunde denselben Feature-Bereich betreffen UND
- die beobachteten Werte unvereinbar sind (z.B. verschiedene Zahlen, ja/nein-Widerspruch) UND
- die Code-Realitaet aus dem Repo-Scan nicht eindeutig aufloest welcher Wert korrekt ist.

**Pflicht-Vorgehen bei erkanntem Cluster-Finding:**

1. `needs-clarification: true` Flag im Finding setzen (NICHT auf Basis einer Vermutung
   einen Schweregrad vergeben).
2. Schweregrad vorlaeufig auf 🟡 **MITTEL** setzen (niemals sofort 🔴 BLOCKER).
3. Eine `clarificationQuestion`-Sektion in den Finding-Eintrag einbauen mit einer
   konkreten, einzeiligen Frage an den Benutzer. Beispiel-Format:
   ```
   clarificationQuestion: "Was ist die echte App-Realitaet — 4 oder 5 Perspektiven?
   Bitte Quellcode-Stelle oder Screenshot nennen."
   ```
4. Den Finding im Bericht als **[Klaerung noetig]**-Badge kennzeichnen und an den Anfang
   der MITTEL-Gruppe setzen, damit der Benutzer ihn nicht übersieht.
5. Erst nachdem der Benutzer geantwortet hat: Schweregrad auf Basis der geklärten
   Code-Realitaet neu bewerten (kann dann auch 🔴 BLOCKER werden).

**Was NIEMALS passieren darf (FIN-008):**
- Widersprüchliche Strings blind auf 🔴 hochstufen, nur weil ein Wert abweicht
- Cluster-Finding still verwerfen weil "wahrscheinlich korrekt"
- `clarificationQuestion` fehlt, obwohl `needs-clarification: true` gesetzt

### Schritt 6 — Berichtsvorlage

Verwende **`assets/berichtsvorlage.template.md`** als Template. Beim Erstellen des Berichts:

1. Datum + Skill-Stand einsetzen
2. Disclaimer am Anfang UND am Ende
3. Befunde nach Schweregrad sortieren (BLOCKER zuerst)
4. Jurisdiktions-Gates komplett ausfuellen (auch Markt mit "Nicht bewertet" oder "OK")
5. Werbeaussagen-vs-Feature-Matrix nur ausfuellen wenn Roentgen-Output verfuegbar war

### Schritt 7 — Wissensbasis aktualisieren

`<WORKSPACE_ROOT>/tools/rechtssicherheit.md` updaten oder neu anlegen. Struktur und
Diff-Logik siehe **`references/wissensbasis-template.md`**.

**Zwei Faelle — der Skill muss zwischen ihnen unterscheiden:**

| Fall | Was tun |
|--|--|
| **Wissensbasis existiert nicht** | Komplettes Template aus `references/wissensbasis-template.md` anlegen. Alle aktuellen Recherche-Erkenntnisse + heutige App-Audit-Eintrag eintragen. |
| **Wissensbasis existiert** | NICHT komplett ueberschreiben. Nur diese Sektionen aktualisieren: (a) "App-Audit-Log" — neue Zeile ANFUEGEN mit Datum, App, Version, Status, Blocker-/Hoch-Zahl, Commit-Notiz. (b) "Quellenregister" — neue Quellen aus Researcher-Runs ANFUEGEN. (c) "Aktuelle Abmahn-Hotspots" — falls neue Erkenntnisse, mit Stand-Datum ergaenzen. (d) "Wiederverwendbare Befundmuster" — falls neu, ANFUEGEN. Bestehende Eintraege bleiben unangetastet. |

**Warum diese Unterscheidung wichtig ist:** Bei mehrfachen Audits derselben App
oder unterschiedlichen Apps in derselben Wissensbasis wuerden Erkenntnisse sonst
ueberschrieben statt akkumuliert. Compound-Wissen ist das Ziel — die Wissensbasis
ist append-only fuer Logs und akkumulativ fuer Erkenntnisse.

### Schritt 8 — Commit + Push

`<WORKSPACE_ROOT>/tools/rechtssicherheit.md` committen und pushen — gemaess
`~/.claude/rules/parallel-sessions-git.md`:

1. Nur eigene Dateien namentlich stagen (NIE `git add -A`)
2. Commit mit fortlaufender Nummer: `#NNNN - rechtssicherheit audit [App]`
3. `git fetch origin && git rebase origin/main`
4. `git status --short` pruefen
5. `git push`

## Schweregrade

| Grad | Bedeutung |
|------|-----------|
| 🔴 **BLOCKER** | Release stoppen. Hohe Abmahn-, Bussgeld- oder Play-Enforcement-Gefahr |
| 🟠 **HOCH** | Vor Release korrigieren. Wesentliche Pflichtangabe oder Inkonsistenz |
| 🟡 **MITTEL** | Risiko reduzieren, moeglichst vor Release |
| 🟢 **NIEDRIG** | Best Practice, Klarheit, Wartbarkeit |
| ℹ️ **INFO** | Beobachtung ohne direkten Befund |

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

## Bundle-Ressourcen

| Pfad | Zweck |
|---|---|
| `references/markt-uebersicht.md` | Master-Index aller Maerkte + Reference-Karte |
| `references/markt-de-eu.md` | DE/EU, BFSG, DSA, AI Act, Data Act, DPF |
| `references/markt-uk-us-ca-au-nz.md` | Englischsprachige Maerkte |
| `references/markt-tuerkei-osteuropa.md` | TR (KVKK), UA |
| `references/markt-asien.md` | IN, JP, KR, TW, HK, SG, TH, ID, VN, LK |
| `references/markt-sa-pakistan-bangladesch.md` | PK (legal vacuum), BD (PDPO 2025) |
| `references/markt-latam.md` | BR, MX, AR |
| `references/markt-mena-afrika.md` | SA, AE, ZA |
| `references/uk-vertreter-pflicht.md` | UK-GDPR Art. 27 Detail |
| `references/ai-act-art-50.md` | KI-Transparenzpflichten ab 02.08.2026 |
| `references/play-policies.md` | Google Play 2025/2026 Updates |
| `references/pflichtdokumente.md` | DSE/AGB/Impressum/Widerruf + interne Artefakte |
| `references/roentgen-integration.md` | Schritt 1.5 Detail |
| `references/enforcement-trends.md` | Aktuelle Abmahn-Hotspots |
| `references/wissensbasis-template.md` | Schritt 7 Template |
| `assets/berichtsvorlage.template.md` | Schritt 6 Berichts-Template |
| `scripts/scan-legal-signals.sh` | Vollscan (Schritt 4) |
| `scripts/check-roentgen-output.sh` | Roentgen-Discovery (Schritt 1.5) |
| `scripts/check-uk-data-processing.sh` | UK-Vertreter-Pflicht-Check (Schritt 5h-UK) |
| `scripts/check-compliance-artifacts.sh` | Interne-Artefakte-Pruefung (Schritt 5j) |

## Was niemals passieren darf

- Rechtliche Garantie geben
- App ohne aktuelle Quellenlage als "rechtssicher" freigeben
- Nur Datenschutztext lesen und Code/SDKs/Permissions ignorieren
- Nur nach Legal-Dateinamen suchen und Code, Ressourcen, Store-Metadaten, Build-Dateien uebersehen
- Einen gefundenen Datenfluss akzeptieren, ohne ihn gegen Privacy Policy, Terms, Data Safety, Consent, App-UI und Loesch-/Widerrufswege abzugleichen
- Google Play Data Safety ungeprueft uebernehmen
- Gebrochene Links, Platzhalter oder falsche App-/Firmennamen uebersehen
- Account-Erstellung ohne Loeschpfad akzeptieren
- Sensitive Daten in Logs, Backups oder Crashreports ignorieren
- Unnoetige Permissions als harmlos einstufen
- Sequentielle Researcher statt parallel (kostet 5x so lange)
- Researcher ohne Limits (max 50 Ergebnisse / 15 Fetches / 10 Min / 2000 Woerter)
- Wissensbasis am Ende nicht aktualisieren (dann lernt das System nicht dazu)
- App-Pruefung ohne vorherige Recherche (dann fehlen Pflichtangaben-Kriterien)
- Rechtstexte nur in Deutsch+Englisch belassen ohne Pruefung welche Laender die Landessprache zwingend verlangen
- DSA, EU Data Act, AI Act Art. 50 (02.08.2026), BFSG/EAA, TDDDG/ePrivacy + PIMS, COPPA, Online Safety Act, DPDP/PIPL/LGPD/POPIA/PDPL ignorieren
- Tuerkei KVKK, Mexiko LFPDPPP-Reform 03/2025, Vietnam PDPL 2026, Bangladesch PDPO 2025, Thailand PDPA, Indonesien UU PDP, Taiwan PDPA, Hongkong PDPO, Neuseeland Privacy Act 2020, Argentinien-Adequacy oder Sri Lanka PDPA-Status uebersehen
- AI Act Art. 50 (02.08.2026) als reines Stichwort behandeln — konkrete UI-Pflicht im Bericht pruefen
- Roentgen-Output ignorieren, wenn er existiert
- Werbeaussagen-Check (UWG §5/§5a) ohne Roentgen-Output durchfuehren — als "nicht durchgefuehrt" markieren
- Pakistan als rechtssicher behandeln ohne den "legal vacuum"-Hinweis
- Interne Compliance-Artefakte (ROPA, DSFA, TIA, TOMs, Loeschkonzept, AVV-Liste, Datenpannen-Plan, AI-Risikoklassifizierung, DSA-Kontaktstelle) als reine Empfehlung behandeln — fehlen sie, ist es 🟠 HOCH
- UGC, Chat, KI, Health, Kinder, Abos, Ads oder sensitive Permissions ohne Sonderpolicy-Gate
- Nicht bewertete Store-Laender still freigeben
- App in UK ausliefern ohne Vertreter (Art. 27) UND ohne UK-Ausschluss (Standard-Empfehlung dieses Skills: Option B = UK ausschliessen)
- Fremde oder unklare Aenderungen im Repo mitcommitten
- Dateien ausserhalb des aktuellen Workspaces bearbeiten, ausser explizit angefordert
- `~/Codex/`, `~/proggs/` oder andere Home-Unterordner als Standard-Arbeitsverzeichnis wenn ein Workspace verfuegbar ist
- Konkrete Rechtstexte als anwaltlich fertige Texte verkaufen (max. Muster-Hinweise mit Quelle und Empfehlung "durch Anwalt pruefen lassen")

## Zusammenspiel mit anderen Skills

- **`app-roentgen` (PFLICHT-Vorstufe wenn moeglich):** Liefert Screens, Klick-Pfade, Paywall-Stufen, Permissions-Mapping, Werbeaussagen-vs-Feature-Matrix. Siehe `references/roentgen-integration.md`. Ohne Roentgen-Output ist der UWG-Werbeaussagen-Check nicht moeglich.
- **`uebersetzung`:** Bei fehlenden Sprachversionen — Markt-Pflichtsprachen aus `references/markt-*.md` pruefen.
- **`string-extraktor`:** Bei hardcodierten Rechtstexten statt in strings.xml.
- **`app-monetizer`:** Bei Paywall-/Abo-Aenderungen — Widerrufsbelehrung + BGB §312k + UWG-konforme Werbeaussagen.
- **Direktiven #1+#2+#3:** Neue Erkenntnisse in `tools/rechtssicherheit.md` persistieren (Compound Intelligence Effect). Wiederkehrende Befundmuster als systematischen Repo-Check vorschlagen.

## Beispiel-Dialog

**Benutzer:** "starte den Skill Rechtssicherheit und pruefe BestJournalAndroid"

**Claude:**
> Ich starte ein Rechtssicherheits-Audit fuer BestJournalAndroid.
>
> **Disclaimer:** Das ist eine technische Pruefhilfe und ersetzt keine anwaltliche Beratung.
> Vor Release ist eine Pruefung durch einen Fachanwalt fuer IT-Recht Pflicht.
>
> Ablauf:
> 1. Pruefe ob ein Roentgen-Output vorhanden ist (`scripts/check-roentgen-output.sh`).
> 1.5 Roentgen-Output einlesen oder Empfehlung an dich, Roentgen zuerst zu starten.
> 2. Lese Wissensbasis aus `tools/rechtssicherheit.md` (oder lege sie neu an).
> 3. Starte 6 parallele Researcher (DE/EU, US/UK/CA/AU/NZ, Asien-A, Asien-B + SA-Sub, LATAM + MENA + TR/UA, Play+Enforcement). Laufzeit: ~5-8 Minuten.
> 4. Vollscan der Codestruktur (`scripts/scan-legal-signals.sh`) wenn kein Roentgen-Output.
> 5. Detail-Pruefung (Manifest, SDKs, Rechtstexte, Consent, Store, UGC, AI, Health, Kinder, Barrierefreiheit, UK-Vertreter via `scripts/check-uk-data-processing.sh`, Jurisdiktions-Gates, Compliance-Artefakte via `scripts/check-compliance-artifacts.sh`).
> 6. Bericht nach `assets/berichtsvorlage.template.md` (mit Compliance-Befunden aus 5j).
> 7. Wissensbasis aktualisieren.
> 8. Commit + Push.
>
> Loslegen?

## Abschluss-Meldung

Am Ende IMMER:

1. Bericht ausgeben (Disclaimer am Anfang UND Ende)
2. Geaenderte Dateien nennen
3. Ausgefuehrte Checks/Builds nennen
4. Nicht pruefbare Punkte klar markieren
5. `<WORKSPACE_ROOT>/tools/rechtssicherheit.md` commit+push
6. Disclaimer wiederholen
7. Intelligenz-Vorschlaege (Direktive #2) falls Muster erkannt — z.B. "alle geprueften Apps haben denselben Impressum-Fehler — soll ich einen Hook bauen der das automatisch checkt?"

---

## Output-Konventionen (Frank-Direktive 2026-05-18 FIN-025)

### Kategorie-ID-Schema (A–G + Z)

Jedes Finding bekommt eine **kategorisierte ID** bestehend aus Buchstabe + fortlaufender Nummer.
Die Nummerierung beginnt innerhalb jeder Kategorie bei 1 (A1, A2, … B1, B2, … usw.).

| Präfix | Kategorie | Rechtsgrundlage / Thema |
|--------|-----------|------------------------|
| **A** | HWG / Heilversprechen / Gesundheit | HWG, Heilmittelwerbegesetz, Health-Claims |
| **B** | UWG / Werbung / Irreführung | UWG §5, §5a, §7, Werbeaussagen, Dark-Ads |
| **C** | DSGVO / Datenschutz / Consent | DSGVO, GDPR, CCPA, PIPL, PDPA, LGPD, POPIA & Äquivalente |
| **D** | BGB / Widerruf / Vertrag | BGB §312k, Widerrufsbelehrung, AGB, IAP, Abo-Laufzeiten |
| **E** | Play-Store-Policy | Google Play User Data Policy, Enforcement, Data Safety |
| **F** | Dark-Pattern / UX-Tricks | Consent-Nudging, vorgewählte Checkboxen, Abonnement-Fallen |
| **G** | Missing-Docs | Fehlende Pflichtdokumente (DSE, AGB, Impressum, Widerruf, Deletion) |
| **Z** | Sonstige / Cross-cutting | Alles was keiner Kategorie A–G eindeutig zuzuordnen ist |

**Pflicht-Mapping im Synthesizer-Schritt (vor Schritt 6):**

```
HWG-Findings       → A
UWG-Findings       → B
DSGVO-Findings     → C
BGB-Findings       → D
Play-Store-Findings → E
Dark-Pattern-Findings → F
MissingDoc-Findings → G
Alles andere       → Z
```

Wenn ein Finding mehrere Kategorien berührt (z.B. DSGVO + Dark-Pattern): primäre
Rechtsverletzung entscheidet. Das andere wird als `crossRef`-Feld im Finding vermerkt.

### ANSI-Farb-Codes (CLI-Output)

Alle Finding-Karten verwenden ANSI-Escape-Codes fuer farbige Header im Terminal.

| Schweregrad | ANSI-Sequenz | Beispiel |
|-------------|-------------|---------|
| 🔴 BLOCKER / high | `\033[1;91m` … `\033[0m` | roter, fetter Text |
| 🟧 HOCH / medium | `\033[1;93m` … `\033[0m` | oranger, fetter Text |
| 🟨 MITTEL / low | `\033[33m` … `\033[0m` | gelber Text |
| ✅ COMPLIANT / positiv | `\033[32m` … `\033[0m` | grüner Text |

Vollständige Escape-Beispiele:

```bash
# BLOCKER (rot, fett):
echo -e "\033[1;91m[B3] 🔴 BLOCKER — UWG §5 Irreführung\033[0m"

# HOCH (orange, fett):
echo -e "\033[1;93m[C7] 🟧 HOCH — DSGVO Consent fehlt\033[0m"

# MITTEL (gelb):
echo -e "\033[33m[G2] 🟨 MITTEL — Impressum unvollständig\033[0m"

# COMPLIANT (grün):
echo -e "\033[32m[D1] ✅ COMPLIANT — Widerruf korrekt platziert\033[0m"
```

Diese Codes gelten für CLI-Ausgaben und Script-Outputs. Im Markdown-Bericht
(Schritt 6, `berichtsvorlage.template.md`) werden stattdessen die Emoji-Symbole
(🔴 / 🟧 / 🟨 / ✅) verwendet — ANSI-Codes werden in gerenderten Markdown-Dateien
nicht korrekt dargestellt.

### Karten-Layout-Template (Phase-2-Karten, Frank-Standard)

Jedes Finding wird als Karte ausgegeben. Das Plugin übernimmt dieses Format direkt.

```
┌─────────────────────────────────────────────────────────────────┐
│ [ANSI-Farbe je nach Severity] [ID] [Severity-Symbol] [Kategorie]│
│ Datei: <pfad>:<zeile>                                           │
├─────────────────────────────────────────────────────────────────┤
│ AKTUELL: "<wörtlicher Text oder Code-Zeile>"                    │
│ PROBLEM: <ein-Satz-Begründung>                                  │
├─────────────────────────────────────────────────────────────────┤
│ [a] "<Vorschlag a>" (Δ -X% kürzer)                              │
│ [b] "<Vorschlag b>" (Δ +Y% stärker)                             │
│ [c] Eigene Formulierung                                         │
│ [skip] Überspringen                                             │
└─────────────────────────────────────────────────────────────────┘
```

**Befüllungsregeln:**

| Feld | Inhalt |
|------|--------|
| `[ID]` | Kategorie-Buchstabe + fortlaufende Nummer, z.B. `B3`, `C12`, `G1` |
| `[Severity-Symbol]` | `🔴` / `🟧` / `🟨` / `✅` entsprechend Schweregrad |
| `[Kategorie]` | Ausgeschriebene Kategorie, z.B. `UWG / Werbung` |
| `Datei:` | Relativer Pfad ab `<APP_DIR>`, mit Zeilennummer wenn bekannt; `n/a` wenn nicht lokalisierbar |
| `AKTUELL:` | Wörtlicher Text aus dem Quell-String oder der Code-Zeile (in Anführungszeichen) |
| `PROBLEM:` | Genau ein Satz. Keine Floskeln. Direkte Rechtsnorm oder Risiko benennen |
| `[a]` / `[b]` | Konkrete alternative Formulierungen. `Δ` zeigt Längenveränderung in Prozent |
| `[c]` | Immer als Option anbieten — Frank will eigene Formulierung wählen können |
| `[skip]` | Immer als Option anbieten — nicht jedes Finding muss sofort gefixt werden |

**Beispiel (ausgefüllt):**

```
┌─────────────────────────────────────────────────────────────────┐
│ \033[1;91mB3 🔴 BLOCKER — UWG / Werbung\033[0m                  │
│ Datei: app/src/main/res/values/strings.xml:247                  │
├─────────────────────────────────────────────────────────────────┤
│ AKTUELL: "Werde in 7 Tagen zum Experten!"                       │
│ PROBLEM: Qualitative Erfolgsgarantie ohne Nachweis (UWG §5 I)   │
├─────────────────────────────────────────────────────────────────┤
│ [a] "Übe täglich mit deinem KI-Coach." (Δ -40%)                 │
│ [b] "Verbessere dein Schreiben Schritt für Schritt." (Δ -20%)   │
│ [c] Eigene Formulierung                                         │
│ [skip] Überspringen                                             │
└─────────────────────────────────────────────────────────────────┘
```

**Was NIEMALS passieren darf (FIN-025):**
- Finding-ID ohne Kategorie-Buchstaben ausgeben (z.B. nur `#3` statt `B3`)
- ANSI-Farb-Code und Emoji-Symbol mischen (ANSI für CLI, Emoji für Markdown — nie beide gleichzeitig im selben Output)
- `AKTUELL:`-Feld leer lassen oder mit paraphrasiertem statt wörtlichem Text befüllen
- `PROBLEM:`-Feld mit mehr als einem Satz befüllen
- Optionen `[c]` oder `[skip]` weglassen — Frank entscheidet, nicht der Skill
- Karten-Layout für Compliance-Status-Meldungen verwenden (nur für Findings mit Handlungsbedarf)
