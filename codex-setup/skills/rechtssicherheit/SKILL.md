---
name: rechtssicherheit
description: >
  Use for Android privacy, imprint, terms, withdrawal, security, consent,
  Google Play compliance, full-code legal-signal scans, and release-blocking
  legal-risk audits. Trigger for Datenschutz, Impressum, AGB,
  Nutzungsbedingungen, Widerruf, DSGVO, Data Safety, Abmahnung, Play-Store
  legal compliance, account deletion, consent, app permissions, SDK data
  collection, Android security, and release readiness.
invocation: user
---

# Skill: Rechtssicherheit

> Wichtiger Disclaimer: Dieser Skill ist eine technische Pruefhilfe und ersetzt
> keine anwaltliche Beratung. Er kann fehlende Pflichtangaben, technische
> Inkonsistenzen, Play-Policy-Risiken und typische Abmahn-Fallstricke markieren.
> Fuer verbindliche Rechtssicherheit muss ein Fachanwalt fuer IT-Recht pruefen.
> Den Benutzer am Anfang und am Ende jedes Berichts darauf hinweisen.

---

## Ziel

Eine Android-App vor Release so pruefen, dass sie technisch, dokumentarisch und
in der Play-Store-Deklaration so weit wie pruefbar abmahnungsresistent ist.

Der Skill prueft nicht nur Rechtstexte, sondern auch ob die App technisch das
tut, was Datenschutz, Nutzungsbedingungen, Impressum, Widerrufsbelehrung und
Google Play Data Safety behaupten.

Pflichtziele:

1. Alle Pflichtdokumente vorhanden: Datenschutzerklaerung, Nutzungsbedingungen,
   Impressum/Anbieterkennzeichnung, Widerruf bei kostenpflichtigen digitalen
   Inhalten, Support/Kontakt, ggf. Account-/Datenloeschung.
2. Pflichtdokumente enthalten die noetigen Angaben fuer Zielmaerkte und Features.
3. App verlinkt diese Dokumente korrekt: Store Listing, Onboarding, Consent,
   Settings, About, Account deletion, Paywall/Checkout.
4. Google Play Data Safety, Play-Console-Deklarationen, Manifest-Permissions,
   SDKs, Netzwerkverhalten und Rechtstexte sind konsistent.
5. Sicherheits- und Datenschutztechnik reduziert rechtliche Risiken: minimale
   Permissions, keine unnoetige Datenerhebung, sichere Speicherung, Backup-Regeln,
   TLS, keine Secrets im Repo, keine sensiblen Logs.
6. Alle relevanten Sprachen/Locales sind abgedeckt oder als Release-Blocker
   markiert.
7. Der Bericht trennt klar zwischen "rechtlich verbindlich durch Anwalt klaeren"
   und "technisch im Repo nachweisbar".

---

## Grundsatz: Keine Garantien

Niemals schreiben:

- "Die App ist rechtssicher."
- "Die App ist 100% abmahnungssicher."
- "Dieser Text reicht rechtlich aus."

Stattdessen schreiben:

- "Technisch wurden keine offensichtlichen Luecken in den geprueften Dateien gefunden."
- "Release aus technischer Sicht nur nach anwaltlicher Pruefung empfohlen."
- "Release blockieren, bis dieser Punkt korrigiert und juristisch geprueft ist."

---

## Aktueller Recherche-Stand

Stand dieser Skill-Version: 2026-04-26.

Bei jeder echten App-Pruefung aktuelle Quellen erneut pruefen, wenn:

- die letzte Recherche aelter als 30 Tage ist,
- Google Play Policies betroffen sind,
- Health, Kinder, Standort, Medien, Kontakte, SMS/Call Logs, Finanzdaten,
  KI/GenAI, Ads, Analytics, User Generated Content oder Accounts vorkommen,
- die App in neue Laender/Sprachen ausgerollt wird,
- der Benutzer "aktuell", "neueste" oder "Release" sagt.

Primaerquellen bevorzugen. Sekundaerquellen nur fuer Abmahn-Trends und
Praxisrisiken verwenden, nie als alleinige Rechtsgrundlage.

Pflicht-Quellenklassen:

- Google Play Developer Policy Center / Play Console Help.
- Android Developers Privacy & Security Dokumentation.
- EU-Kommission, GDPR-Text, nationale Gesetzestexte.
- Deutsche Gesetze: DDG fuer Anbieterkennzeichnung, BGB/EGBGB fuer Widerruf.
- Aufsichtsbehoerden: z.B. EDPB, Datenschutzkonferenz, ICO, FTC/CPPA, OAIC,
  OPC, relevante asiatische Behoerden.
- OWASP MASVS/MASTG fuer technische Mobile-Security-Kontrollen.

---

## Ablauf

### Schritt 1 - Scope klaeren

Wenn der Benutzer die App nicht genannt hat, einmal kurz fragen:

> Welche App soll ich pruefen? (z.B. BestJournalAndroid, BestJournalFrank, QuizVerse)

Wenn fuer den Audit noetig und nicht aus dem Repo erkennbar, Fragen gesammelt
stellen, nicht einzeln:

1. In welchen Laendern/Sprachen soll die App veroeffentlicht werden?
2. Gibt es In-App-Kaeufe, Abos, Werbung, Affiliate-Links oder externe Zahlungen?
3. Gibt es Accounts, Cloud-Sync, Backups, Export, Import oder Datenloeschung?
4. Werden Firebase, Analytics, Crashlytics, Ads, KI-APIs oder andere SDKs genutzt?
5. Richtet sich die App an Kinder oder kann sie fuer Kinder attraktiv wirken?
6. Gibt es sensible Daten: Gesundheit, Tagebuch, Standort, Kontakte, Fotos,
   Audio, Kamera, Kalender, Finanzdaten, Religion, Sexualitaet, biometrische Daten?

Nicht auf Antworten warten, wenn der Repo-Zustand eine konservative Annahme
erlaubt. Unklare Punkte im Bericht als Annahmen markieren.

### Schritt 2 - Workspace und Wissensbasis

Die Wissensbasis liegt workspace-lokal, nicht in `~/Codex/`, `~/proggs/`
oder einem persoenlichen Home-Unterordner.

Plattformneutraler Zielpfad:

`<WORKSPACE_ROOT>/tools/rechtssicherheit.md`

`<WORKSPACE_ROOT>` ist das aktuelle Codex-Arbeitsverzeichnis bzw. der Root des
Repos, in dem die zu pruefende App liegt. Beispiele fuer die Aufloesung:

- Windows: `%USERPROFILE%\Codex CLI\tools\rechtssicherheit.md`
- macOS/Linux: `$HOME/Codex CLI/tools/rechtssicherheit.md`

Wenn sie existiert: lesen und nur mit neuen, belegten Erkenntnissen aktualisieren.
Wenn sie fehlt: nach der Recherche neu anlegen.

Wenn der Benutzer explizit eine Datei ausserhalb des Workspaces nennt, darf diese
konkrete Datei gelesen/geschrieben werden. Sonst bleibt der Skill im Codex-
Workspace.

### Schritt 3 - Aktuelle Recherche

Vor jedem Release-Audit aktuelle Quellen pruefen. Wenn Subagents im aktuellen
CLI erlaubt und vom Benutzer explizit gewuenscht sind, koennen unabhaengige
Researcher parallel gestartet werden. Sonst selbst recherchieren.

Mindestens diese Bereiche pruefen:

1. DE/EU: DSGVO Art. 12, 13, 14, 15-22, 7; DDG Par. 5 Anbieterkennzeichnung;
   TTDSG/TDDDG/ePrivacy fuer Tracking/Endgeraetezugriff; Verbraucherrecht und
   Widerruf bei digitalen Inhalten.
2. Google Play: User Data, Data Safety, Account Deletion, Prominent Disclosure
   & Consent, Permissions, Families, Ads, Health, Financial Services, AI-
   Generated Content, User Generated Content, Deceptive Behavior.
3. Android: Privacy checklist, Security best practices, SDK safety, runtime
   permissions, photo picker, background location, backup, network security,
   exported components, WebView, logs, secrets.
4. Ausland: UK-GDPR, CCPA/CPRA, PIPEDA, Privacy Act AU, LGPD, PIPL, DPDP,
   APPI, PIPA nur soweit Zielmaerkte betroffen sind.
5. Abmahn-/Enforcement-Trends: fehlerhaftes Impressum, fehlende oder falsche
   Datenschutzerklaerung, Google Fonts/Tracking/Analytics ohne Einwilligung,
   falsche Widerrufsbelehrung, unklare Preis-/Abo-Angaben, Kinder-/Ads-
   Verstoss, gebrochene Privacy-Policy-Links, Data-Safety-Widerspruch.

Jede Quelle mit URL und Abrufdatum notieren.

### Schritt 4 - App-Inventar erstellen

Alle folgenden Punkte als Inventar erfassen. Keine Bewertung ohne Inventar.
Der gesamte App-Code muss durchsucht werden, nicht nur offensichtliche
`privacy`, `terms` oder `legal` Dateien. Rechtlich relevante Hinweise koennen
in UI-Texten, ViewModels, Repository-Klassen, SDK-Initialisierung,
Build-Konfiguration, Store-Metadaten, Tests, Markdown-Dateien, Scripts und
Web-/Backend-Hilfsdateien stecken.

#### 4.0 Pflicht: Vollscan der Codestruktur

Vor Detailpruefungen immer eine vollstaendige Datei- und Signal-Inventur der
betroffenen App erstellen.

Datei-Inventur, plattformneutral mit `rg`:

```sh
rg --files [APP_DIR]
rg --files [APP_DIR] | rg -i "\.(kt|java|xml|gradle|kts|json|properties|md|html|js|ts|tsx|jsx|yaml|yml|txt|csv)$"
```

Generated/build Artefakte duerfen aus der Bewertung ausgeschlossen werden, wenn
sie klar reproduzierbar sind, z.B. `build/`, `.gradle/`, `node_modules/`,
`.idea/`, `captures/`. Store-Metadaten, Legal-Assets, Scripts und Configs
duerfen nicht ausgeschlossen werden.

Legal-Signal-Suche ueber die gesamte App, plattformneutral mit `rg`:

```sh
rg -n -i "privacy|datenschutz|dsgvo|gdpr|ccpa|consent|einwilligung|widerruf|withdraw|terms|nutzungsbedingungen|agb|impressum|anbieter|legal|policy|delete account|account deletion|datenloesch|loesch|support|kontakt|contact|billing|subscription|abo|refund|iap|in-app|admob|ads|advertising|analytics|crashlytics|firebase|sentry|tracking|telemetry|location|standort|camera|kamera|microphone|mikrofon|contacts|kontakte|calendar|kalender|health|gesundheit|journal|diary|tagebuch|ai|ki|openai|anthropic|gemini|children|kids|families|ugc|moderation|webview|cookie|font|google fonts|third party|drittanbieter|export|backup|sync|cloud|encryption|verschluessel|log\\.|timber|token|secret|api[_-]?key|http://" [APP_DIR]
```

Jeden Treffer klassifizieren:

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

Pflicht-Ergebnis dieses Vollscans:

- Liste aller geprueften Dateigruppen.
- Liste aller rechtlich relevanten Treffergruppen.
- Fuer jede Treffergruppe ein Abgleich: "in Dokumenten erwaehnt?", "in App
  verlinkt?", "in Data Safety/Play Console zu deklarieren?", "Consent noetig?",
  "Release-Blocker?".
- Wenn keine Treffer in einem erwarteten Bereich gefunden werden, explizit
  notieren, z.B. "Keine Account-Erstellung gefunden" oder "Keine Ads-SDKs
  gefunden".

#### 4a. Projekt und Package

Suchen, plattformneutral mit `rg`:

```sh
rg --files
rg -n "namespace |applicationId|package=" -g "*.gradle*" -g "*.kts" -g "AndroidManifest.xml" [APP_DIR]
```

Erfassen:

- App-Name, Package-ID, Variante/Flavor.
- minSdk, targetSdk, compileSdk.
- Store-Metadaten: `fastlane/metadata`, `play-store-metadata`, README, Website.
- Verwendete Sprachen/Locales: `app/src/main/res/values*`.

#### 4b. Rechtstexte und UI-Links

Suchen:

```sh
rg -n -i "datenschutz|privacy|privacy policy|terms|nutzungsbedingungen|agb|impressum|anbieter|widerruf|refund|refunds|deletion|loesch|delete account|support|kontakt|contact" [APP_DIR]
rg --files [APP_DIR] | rg -i "privacy|terms|impressum|legal|widerruf|refund|delete|deletion|support"
```

Pruefen:

- Datenschutzerklaerung in App und Store erreichbar.
- Nutzungsbedingungen/AGB erreichbar, falls angeboten.
- Impressum/Anbieterkennzeichnung dauerhaft erreichbar.
- Widerrufsbelehrung und Muster-Widerrufsformular bei paid apps, IAP, Abo,
  digitalen Inhalten oder externem Checkout.
- Support/Kontaktadresse vorhanden.
- Account-/Datenloeschung in App und als Weblink vorhanden, wenn Accounts
  erstellt werden koennen.
- Keine toten Links, Platzhalter, "TODO", falsche App-Namen, falsche Firma,
  alte Mailadressen, falsche Rechtsgrundlagen.

#### 4c. Manifest, Permissions und Android-Komponenten

Suchen:

```sh
rg -n "uses-permission|queries|provider|receiver|service|activity|exported|allowBackup|fullBackupContent|dataExtractionRules|networkSecurityConfig|usesCleartextTraffic|debuggable" -g "AndroidManifest.xml" [APP_DIR]
```

Besonders kritisch:

- `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`.
- `READ_CONTACTS`, `READ_CALENDAR`, `READ_PHONE_STATE`, SMS/Call-Log.
- `CAMERA`, `RECORD_AUDIO`.
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `MANAGE_EXTERNAL_STORAGE`.
- `POST_NOTIFICATIONS`, wenn sensitive Inhalte in Notifications erscheinen.
- `QUERY_ALL_PACKAGES` oder installierte Apps.
- `AD_ID`.
- Health Connect / Gesundheitsdaten.
- Exported Activities/Services/Receivers/Providers.
- `allowBackup`, `fullBackupContent`, `dataExtractionRules`.
- `usesCleartextTraffic` und Netzwerk-Sicherheitskonfiguration.

Jede Permission gegen Feature, Rechtstext, Prominent Disclosure, Runtime-
Permission-Dialog und Data Safety abgleichen.

#### 4d. SDKs, Dependencies und Datenabfluss

Suchen:

```sh
rg -n -i "firebase|analytics|crashlytics|admob|ads|facebook|meta|adjust|appsflyer|sentry|amplitude|mixpanel|onesignal|revenuecat|billing|stripe|openai|anthropic|googleapis|okhttp|retrofit|ktor|webview|javascript" [APP_DIR]
rg -n -i "http://|https://|Authorization|Bearer|apiKey|apikey|secret|token|client_secret" [APP_DIR]
```

Pruefen:

- Welche SDKs sammeln automatisch Daten?
- Werden IP-Adresse, Device IDs, Crash Logs, Analytics Events, Advertising ID,
  Firebase Installation ID oder Push Tokens verarbeitet?
- Gibt es Drittlandtransfer ausserhalb EU/EWR?
- Wird eine AVV/DPA oder SCC/Transfergrundlage benoetigt?
- Ist die Privacy Policy spezifisch genug fuer jeden SDK-Zweck?
- Sind Debug-/Test-/Staging-Endpunkte entfernt?
- Werden Secrets aus `$HOME/SK/` statt aus dem Repo geladen?

#### 4e. Lokale Speicherung, Logs und Backup

Suchen:

```sh
rg -n -i "SharedPreferences|DataStore|RoomDatabase|SQLite|File\\(|openFileOutput|cacheDir|externalCacheDir|getExternalFilesDir|Log\\.|Timber|println|printStackTrace|Encrypted|KeyStore|MasterKey" [APP_DIR]
```

Pruefen:

- Sensitive Daten verschluesselt oder mindestens nicht in Klartext in
  SharedPreferences/Files/Logs.
- Tagebuch-, Gesundheits-, Auth-, Token- und Profil-Daten nicht in Backups,
  Screenshots, Clipboard, Logs oder Crashreports geleakt.
- Backup-Regeln schliessen sensitive Daten aus oder verlangen Ende-zu-Ende-
  Schutz, wenn Backup erlaubt ist.
- Export/Import ist bewusst dokumentiert und sicher.
- Loeschfunktion loescht lokale Daten, Sync-Daten und Backups soweit moeglich.

#### 4f. Consent und Widerruf

Pruefen:

- Einwilligung vor nicht notwendiger Datenverarbeitung.
- Kein vorangekreuztes Consent.
- Widerruf so einfach wie Zustimmung.
- Consent-Zwecke getrennt: Analytics, Crashlytics, Ads, personalisierte Ads,
  Cloud-Sync, Newsletter/Marketing, KI-Verarbeitung.
- App funktioniert soweit moeglich auch ohne freiwillige Einwilligungen.
- Alter/Kinderschutz, falls relevant.
- Prominent Disclosure direkt im Nutzungsfluss, nicht nur in Settings oder
  Privacy Policy, wenn Google Play dies verlangt.

#### 4g. Store Listing und Play Console

Pruefen, soweit Dateien/Notizen vorhanden oder vom Benutzer geliefert:

- Privacy Policy URL in Store Listing oeffentlich erreichbar.
- App-/Developer-Name in Privacy Policy stimmen mit Store Listing ueberein.
- Data Safety Form deckt echte Datenerhebung und SDKs ab.
- Account Deletion Form/Weblink vorhanden, wenn Account-Erstellung vorhanden.
- Content Rating, Target Audience, Families, Ads, Health, AI, Financial,
  Data Deletion, App Access, Permissions declarations korrekt.
- Screenshots/Marketingtexte versprechen nichts Falsches zu Datenschutz,
  Sicherheit, Medizin, Finanzen, KI oder Kinderfreundlichkeit.
- Abo-/Preisangaben und Trial-Hinweise klar.

---

## Pflichtdokumente: Inhaltliche Checklisten

### Datenschutzerklaerung

Muss je nach Scope mindestens pruefbar abdecken:

- Verantwortlicher: Name/Firma, Adresse, Kontakt, Datenschutzkontakt.
- App-Name und Package/Store-Bezug.
- Kategorien personenbezogener Daten.
- Zwecke der Verarbeitung.
- Rechtsgrundlagen je Zweck.
- Empfaenger/Dritte/SDKs.
- Drittlandtransfer und Garantien.
- Speicherdauer oder Kriterien.
- Betroffenenrechte: Auskunft, Berichtigung, Loeschung, Einschraenkung,
  Portabilitaet, Widerspruch, Widerruf, Beschwerderecht.
- Pflicht oder Freiwilligkeit der Bereitstellung.
- Automatisierte Entscheidungen/Profiling, falls vorhanden.
- Kinder/Jugendliche, falls relevant.
- Sicherheitsmassnahmen knapp und realistisch.
- Account- und Datenloeschung.
- Stand/Version/Datum.

Release-blocker:

- Allgemeine Generator-Texte ohne App-/SDK-Bezug.
- "Wir sammeln keine Daten", obwohl SDKs/Crashlytics/Analytics/Ads/Cloud laufen.
- Fehlende Drittanbieter.
- Fehlende Rechtsgrundlagen.
- Fehlende Loesch-/Widerrufswege.
- Falscher Verantwortlicher oder falsche App.

### Nutzungsbedingungen / AGB

Pruefen:

- Anbieter, App-Name, Leistungsbeschreibung.
- Nutzungsregeln und verbotene Nutzung.
- User Generated Content, falls vorhanden: Rechte, Moderation, Meldung,
  Entfernung, Sperrung, Beschwerdeweg.
- Haftung und Gewaehrleistung ohne unzulaessige Pauschalausschluesse.
- Verfuegbarkeit, Aenderungen, Kuendigung.
- In-App-Kaeufe, Abos, Preise, Laufzeiten, Kuendigung.
- Drittanbieter/Stores/Billing-Hinweise.
- Rechtswahl/Gerichtsstand nur soweit zulaessig gegenueber Verbrauchern.
- Kontakt und Beschwerdeweg.

Release-blocker:

- AGB schliessen Verbraucherrechte pauschal aus.
- AGB widersprechen Store Listing, Paywall oder Privacy Policy.
- Kostenpflichtige Features ohne klare Preis-/Abo-/Kuendigungsangaben.

### Impressum / Anbieterkennzeichnung

Fuer DE/EU geschaeftsmaessige digitale Dienste pruefen:

- Vollstaendiger Name/Firma.
- Ladungsfaehige Anschrift, keine reine Postfach-Loesung.
- E-Mail und schnelle elektronische Kontaktmoeglichkeit.
- Vertretungsberechtigte Person bei juristischen Personen.
- Register, Registernummer, Umsatzsteuer-ID, Aufsichtsbehoerde oder
  Berufsangaben, falls einschlaegig.
- Leicht erkennbar, unmittelbar erreichbar, staendig verfuegbar.
- In App dauerhaft erreichbar, z.B. Settings/About, nicht nur Store Listing.

Release-blocker:

- Kein Impressum trotz geschaeftsmaessigem Angebot.
- Nur E-Mail ohne Anschrift, wenn Anbieterkennzeichnungspflicht greift.
- Impressum nur in schwer auffindbarem Weblink oder totem Link.

### Widerruf / digitale Inhalte / Abos

Pruefen, wenn paid app, IAP, Abo oder externe digitale Inhalte:

- Widerrufsbelehrung vor Kaufabschluss erreichbar.
- Muster-Widerrufsformular vorhanden, wenn erforderlich.
- Erloschen des Widerrufsrechts bei sofortiger digitaler Leistung nur mit
  ausdruecklicher Zustimmung und Bestaetigung der Kenntnis.
- Abo-Laufzeit, Preis, Testphase, automatische Verlaengerung und Kuendigung
  klar in Paywall/Store/Terms.
- Google Play Billing Regeln eingehalten, wenn digitale Inhalte in der App
  verkauft werden.

Release-blocker:

- Kostenpflichtige digitale Inhalte ohne Widerrufsinformation.
- "Kein Widerruf" ohne korrekte Zustimmung/Belehrung.
- Paywall widerspricht Terms oder Store Listing.

### Account- und Datenloeschung

Pruefen, wenn Account-Erstellung moeglich:

- In-App-Pfad zur Accountloeschung.
- Weblink fuer Loeschanfrage ohne App-Installation.
- Link ist funktional, nennt App oder Developer, und fuehrt direkt zum
  Loeschprozess oder klaren Antrag.
- Erklaert, welche Daten geloescht, behalten oder anonymisiert werden und warum.
- Data Safety Form beantwortet Data deletion Fragen konsistent.

Release-blocker:

- Account-Erstellung ohne Accountloeschung.
- Weblink fuehrt nur zu Support-Homepage ohne klaren Loeschweg.
- Privacy Policy verspricht Loeschung, App bietet sie nicht.

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

---

## Bewertung

Schweregrade:

| Grad | Bedeutung |
|---|---|
| BLOCKER | Release stoppen. Hohe Abmahn-, Bussgeld- oder Play-Enforcement-Gefahr. |
| HOCH | Vor Release korrigieren. Wesentliche Pflichtangabe oder technische Inkonsistenz. |
| MITTEL | Risiko reduzieren, moeglichst vor Release korrigieren. |
| NIEDRIG | Best Practice, Klarheit, Wartbarkeit. |
| INFO | Beobachtung ohne direkten Befund. |

BLOCKER-Beispiele:

- Keine Datenschutzerklaerung in App/Store.
- Privacy Policy sagt "keine Daten", aber App nutzt Analytics/Ads/Crash/Cloud.
- Keine Anbieterkennzeichnung bei geschaeftsmaessigem DE/EU-Angebot.
- Account-Erstellung ohne Loeschweg.
- Kinderzielgruppe mit verbotenen IDs/Ads/Tracking.
- Health/Medical Claims ohne korrekte Deklaration und Disclaimer.
- Sensitive Permissions ohne Core-Feature, Disclosure oder Play Declaration.
- Widerrufsbelehrung fehlt bei kostenpflichtigen digitalen Inhalten.
- Store Data Safety widerspricht Code/SDKs.

---

## Berichtsvorlage

```markdown
# Rechtssicherheits-Audit: [App]
Datum: YYYY-MM-DD
Skill-Stand: 2026-04-26

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
- BLOCKER:
- HOCH:
- MITTEL:
- Wichtigste Risiken:

## Befunde
### BLOCKER
1. [Titel]
   - Nachweis: [Datei:Zeile oder Quelle]
   - Risiko: [konkret]
   - Fix: [konkret]
   - Quelle: [URL, Abrufdatum]

### HOCH
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

## Play-Console-Checkliste
- [ ] Data Safety passt zu Code und SDKs
- [ ] Privacy Policy URL erreichbar
- [ ] Account deletion beantwortet und verlinkt
- [ ] App Access korrekt
- [ ] Content Rating korrekt
- [ ] Target Audience/Families korrekt
- [ ] Ads/Health/AI/Finance/Permissions Declarations korrekt

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

## Wissensbasis aktualisieren

Nach einer App-Pruefung `<WORKSPACE_ROOT>/tools/rechtssicherheit.md`
aktualisieren oder neu anlegen.

Struktur:

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

## App-Audit-Log
| Datum | App | Version | Status | Blocker | Hoch | Commit/Notiz |
|---|---|---|---|---:|---:|---|

## Wiederverwendbare Befundmuster
```

Keine Secrets, echten Kundendaten, privaten Adressen oder Token in diese Datei
schreiben, ausser der Benutzer verlangt explizit genau diese Ablage und bestaetigt
den Umgang mit sensiblen Daten.

---

## Typische Fix-Hinweise

Der Skill darf konkrete technische Fixes vorschlagen oder implementieren:

- Settings/About Links zu Datenschutz, Terms, Impressum, Loeschung.
- Consent-Screen vor Analytics/Ads/Cloud/KI.
- Toggles fuer Analytics/Crashlytics/Ads, inkl. Widerruf.
- Runtime-Permission-Erklaerungen an Feature-Kontext koppeln.
- Unnoetige Permissions entfernen.
- Android Photo Picker statt breiter Medien-Permissions.
- Backup-Regeln fuer sensitive Daten.
- `usesCleartextTraffic=false` oder Network Security Config bereinigen.
- Sensitive Logs entfernen.
- Secrets in `$HOME/SK/` verlagern und Repo bereinigen.
- Store-/Data-Safety-Checkliste als Markdown erzeugen.
- Rechtstext-Platzhalter oder falsche App-Namen korrigieren.

Der Skill darf keine anwaltlich wirkenden endgueltigen Rechtstexte als "fertig"
verkaufen. Er darf Entwuerfe, Lueckenlisten, Musterhinweise mit Quellen und
anwaltliche Pruefpunkte erstellen.

---

## Was niemals passieren darf

- Rechtliche Garantie geben.
- App ohne aktuelle Quellenlage als "rechtssicher" freigeben.
- Nur Datenschutztext lesen und Code/SDKs/Permissions ignorieren.
- Nur nach Legal-Dateinamen suchen und dabei rechtlich relevante Logik in Code,
  Ressourcen, Store-Metadaten, Build-Dateien, Scripts oder UI-Flows uebersehen.
- Einen gefundenen Datenfluss akzeptieren, ohne ihn gegen Privacy Policy,
  Nutzungsbedingungen, Data Safety, Consent, App-UI und Loesch-/Widerrufswege
  abzugleichen.
- Google Play Data Safety ungeprueft uebernehmen.
- Gebrochene Links, Platzhalter oder falsche App-/Firmennamen uebersehen.
- Account-Erstellung ohne Loeschpfad akzeptieren.
- Sensitive Daten in Logs, Backups oder Crashreports ignorieren.
- Unnoetige Permissions als harmlos einstufen.
- Fremde oder unklare Aenderungen im Repo mitcommitten.
- Dateien ausserhalb des Codex-Workspaces bearbeiten, ausser sie wurden vom
  Benutzer konkret angefordert.
- `~/Codex/`, `~/proggs/` oder andere Home-Unterordner als Arbeitsverzeichnis
  verwenden, wenn der aktuelle Codex-Workspace/Repo-Root verfuegbar ist.

---

## Beispiel-Start

Benutzer:

> Starte den Skill Rechtssicherheit und pruefe BestJournalAndroid.

Antwort:

> Ich starte ein Rechtssicherheits-Audit fuer BestJournalAndroid.
> Disclaimer: Das ist eine technische Pruefhilfe und ersetzt keine anwaltliche
> Beratung.
>
> Ich pruefe zuerst aktuelle Quellen und die lokale Wissensbasis, dann Code,
> Manifest, SDKs, Rechtstexte, In-App-Verlinkung, Store-/Data-Safety-Konsistenz
> und Android-Sicherheitsrisiken.

---

## Abschluss

Am Ende immer:

1. Bericht ausgeben.
2. Geaenderte Dateien nennen.
3. Ausgefuehrte Checks/Builds nennen.
4. Nicht pruefbare Punkte klar markieren.
5. Disclaimer wiederholen.
6. Falls wiederkehrende Muster sichtbar sind, einen konkreten Automatisierungs-
   vorschlag machen, z.B. "Soll ich einen Repo-Check bauen, der fehlende
   Impressum-/Privacy-/Account-Deletion-Links und Data-Safety-Widersprueche
   automatisch meldet?"
