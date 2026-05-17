# Pflichtdokumente — Inhaltliche Checklisten

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Im Schritt 5 (Detail-Pruefung) und immer wenn ein konkretes
> Pflichtdokument geprueft wird. Diese Datei ist die zentrale inhaltliche Referenz fuer
> alle Pflichttexte einer App.

## Datenschutzerklaerung

### Pflicht-Inhalte

- Verantwortlicher (Name/Firma, Adresse, Kontakt, Datenschutzkontakt)
- App-Name und Package/Store-Bezug
- Kategorien personenbezogener Daten
- Zwecke der Verarbeitung
- Rechtsgrundlagen je Zweck
- Empfaenger/Dritte/SDKs
- Drittlandtransfer und Garantien
- Speicherdauer oder Kriterien
- Betroffenenrechte: Auskunft, Berichtigung, Loeschung, Einschraenkung, Portabilitaet,
  Widerspruch, Widerruf, Beschwerderecht
- Pflicht oder Freiwilligkeit der Bereitstellung
- Automatisierte Entscheidungen/Profiling, falls vorhanden
- Kinder/Jugendliche, falls relevant
- Sicherheitsmassnahmen knapp und realistisch
- Account- und Datenloeschung
- Stand/Version/Datum

### Release-Blocker

- Allgemeine Generator-Texte ohne App-/SDK-Bezug
- "Wir sammeln keine Daten" obwohl SDKs/Crashlytics/Analytics/Ads/Cloud laufen
- Fehlende Drittanbieter
- Fehlende Rechtsgrundlagen
- Fehlende Loesch-/Widerrufswege
- Falscher Verantwortlicher oder falsche App

## Nutzungsbedingungen / AGB

### Pflicht-Inhalte

- Anbieter, App-Name, Leistungsbeschreibung
- Nutzungsregeln und verbotene Nutzung
- UGC (Rechte, Moderation, Meldung, Entfernung, Sperrung, Beschwerdeweg) falls vorhanden
- Haftung und Gewaehrleistung ohne unzulaessige Pauschalausschluesse
- Verfuegbarkeit, Aenderungen, Kuendigung
- In-App-Kaeufe, Abos, Preise, Laufzeiten, Kuendigung
- Drittanbieter/Stores/Billing-Hinweise
- Rechtswahl/Gerichtsstand nur soweit zulaessig gegenueber Verbrauchern
- Kontakt und Beschwerdeweg

### Release-Blocker

- AGB schliessen Verbraucherrechte pauschal aus
- AGB widersprechen Store Listing, Paywall oder Privacy Policy
- Kostenpflichtige Features ohne klare Preis-/Abo-/Kuendigungsangaben

## Impressum / Anbieterkennzeichnung

**Rechtsgrundlage:** DDG §5 (Digitale-Dienste-Gesetz, ersetzt TMG §5 seit 14.05.2024).
Apps oder Texte die noch "§5 TMG" zitieren sind veraltet und MUESSEN auf "§5 DDG" aktualisiert
werden — der Skill prueft das aktiv.

Fuer DE/EU geschaeftsmaessige digitale Dienste:

- Vollstaendiger Name/Firma
- Ladungsfaehige Anschrift (kein reines Postfach)
- E-Mail und schnelle elektronische Kontaktmoeglichkeit
- Vertretungsberechtigte Person bei juristischen Personen
- Register, Registernummer, USt-ID, Aufsichtsbehoerde oder Berufsangaben falls einschlaegig
- Leicht erkennbar, unmittelbar erreichbar, staendig verfuegbar
- **In App dauerhaft erreichbar (Settings/About), nicht nur im Store Listing**

### Release-Blocker

- Kein Impressum trotz geschaeftsmaessigem Angebot
- Nur E-Mail ohne Anschrift
- Impressum nur in schwer auffindbarem Weblink oder totem Link
- Verweis auf "§5 TMG" statt "§5 DDG"

## Widerruf / digitale Inhalte / Abos

Bei paid app, IAP, Abo oder externen digitalen Inhalten:

- Widerrufsbelehrung VOR Kaufabschluss erreichbar
- Muster-Widerrufsformular vorhanden, wenn erforderlich
- Erloeschen des Widerrufsrechts bei sofortiger digitaler Leistung nur mit
  ausdruecklicher Zustimmung und Bestaetigung der Kenntnis
- Abo-Laufzeit, Preis, Testphase, automatische Verlaengerung und Kuendigung
  klar in Paywall/Store/Terms
- Google Play Billing Regeln eingehalten
- BGB §312k Kuendigungsbutton (so einfach wie Vertragsabschluss)

### Release-Blocker

- Kostenpflichtige digitale Inhalte ohne Widerrufsinformation
- "Kein Widerruf" ohne korrekte Zustimmung/Belehrung
- Paywall widerspricht Terms oder Store Listing

## Account- und Datenloeschung

Wenn Account-Erstellung moeglich:

- **In-App-Pfad zur Accountloeschung** (Pflicht laut Google Play)
- **Weblink fuer Loeschanfrage ohne App-Installation** (Pflicht laut Google Play)
- Link funktional, nennt App oder Developer, fuehrt direkt zum Loeschprozess
- Erklaert welche Daten geloescht/behalten/anonymisiert werden und warum
- Data Safety Form beantwortet Data deletion Fragen konsistent

### Release-Blocker

- Account-Erstellung ohne Accountloeschung
- Weblink fuehrt nur zu Support-Homepage ohne klaren Loeschweg
- Privacy Policy verspricht Loeschung, App bietet sie nicht

## Interne Compliance-Artefakte (nicht oeffentlich, aber Pflicht zu fuehren)

Diese Dokumente werden vom Anbieter intern gefuehrt und MUESSEN existieren — der Skill prueft,
ob die App sie systematisch erstellt/aktualisiert oder ob sie fehlen. Im Bericht als 🟠 HOCH
markieren, wenn der Repo keine Hinweise auf das Vorhandensein gibt (z.B. `docs/datenschutz/ROPA.md`,
`docs/AVVs/`, etc.).

| Dokument | Pflichtgrad | Rechtsgrundlage / Quelle | Pruef-Hinweis |
|---|---|---|---|
| **VVT / ROPA** (Verzeichnis Verarbeitungstaetigkeiten) | PFLICHT | DSGVO Art. 30 | Pro Verarbeitung: Zweck, Datenkategorie, Empfaenger, Speicherdauer, Drittlandtransfer |
| **DSFA / DPIA** (Datenschutz-Folgenabschaetzung) | PFLICHT bei Hochrisiko | DSGVO Art. 35 | Trigger: Gesundheit, Kinder, biometrische Daten, automatisierte Entscheidungen, grossflaechiges Profiling, sensitive Kategorien (Art. 9), Tagebuch/Stimmungsdaten |
| **TIA** (Transfer Impact Assessment) | PFLICHT bei Drittlandtransfer | DSGVO Art. 44 ff., EDPB Recommendation 01/2020 | Pflicht bei Firebase, Crashlytics, OpenAI, Anthropic, Sentry, Adjust ausserhalb EU/EWR. Inhalte: Rechtslage Empfaengerland, behoerdlicher Zugriff, technische Zusatzmassnahmen, DPF-Status |
| **TOMs** (Technisch-Organisatorische Massnahmen) | PFLICHT | DSGVO Art. 32 | Verschluesselung, Pseudonymisierung, Backup, Zugangskontrolle, Vorfallreaktion, Schulungen, Wiederherstellbarkeit |
| **AVV-Liste** (pro Auftragsverarbeiter ein signierter AVV/DPA) | PFLICHT | DSGVO Art. 28 | Pro SDK/Cloud-Dienst: Firebase, OpenAI, Anthropic, Adjust, Sentry, Crashlytics, AdMob, Meta SDK, RevenueCat, etc. Archivkopie zwingend |
| **SCCs / DPF / BCR** (Transfergrundlage Drittland) | PFLICHT bei jedem Drittlandtransfer | DSGVO Art. 46 / DPF / Art. 47 | EU-Kommissions-SCCs 2021, DPF-Listung des US-Anbieters pruefen (EuG 03.09.2025 / EuGH-Berufung — SCCs als Backup) |
| **Loeschkonzept** | PFLICHT (Rechenschaftspflicht) | DSGVO Art. 5 Abs. 1e + Art. 17 | Pro Datenkategorie: Loeschfristen, Loeschmechanismus, Anonymisierung, Backup-Bereinigung |
| **Datenpannen-Meldeplan + Logbuch** | PFLICHT | DSGVO Art. 33/34 | 72-Stunden-Frist; Meldekriterien; Eskalationspfad; Logbuch aller Vorfaelle |
| **AI-System-Risikoklassifizierung** | PFLICHT bei eingesetztem KI-System | EU AI Act Art. 6, 9, 13 (ab 02.08.2026 voll enforced) | Pro KI-Feature: Risikoklasse, Anwender-/Anbieter-Rolle, Konformitaetsbewertung, Transparenzhinweise (Art. 50) |
| **DSA-Beschwerde-/Kontaktstelle** | PFLICHT fuer Vermittlungsdienste/Plattformen | DSA Art. 11 (Single Point of Contact), umgesetzt in DDG | E-Mail oder Formular, oeffentlich verlinkt, separat vom normalen Support |
| **Schulungsnachweise + Auditprotokoll** | Empfehlung / de-facto bei Audit | DSGVO Art. 5 Abs. 2, Art. 39 (DSB) | Regelmaessige Schulungen Datenschutz, KI Act, OWASP |

### Pruefung im Repo

Skript: `scripts/check-compliance-artifacts.sh [APP_DIR]`

Inline-Pruefung:
```sh
rg --files [APP_DIR] | rg -i "rope|ropa|verarbeitungstaetigkeit|dpia|dsfa|tia|transfer.impact|avv|dpa.template|loeschkonzept|tom|toms|breach.plan|datenpannen"
# Auch Doku-Verzeichnisse pruefen
ls [APP_DIR]/docs/datenschutz/ 2>/dev/null
ls [APP_DIR]/docs/compliance/ 2>/dev/null
ls [APP_DIR]/docs/avv/ 2>/dev/null
```

**Wenn Repo keine Hinweise enthaelt:** Im Bericht als 🟠 HOCH markieren — "interne Compliance-
Artefakte im Repo nicht nachweisbar; vor Release durch Datenschutzbeauftragten verifizieren lassen".
Es ist nicht Aufgabe des Skills, diese Dokumente zu erstellen.

## Templates / Muster-Quellen (KEINE eigene Erstellung)

- **DSE-Generator (DE):** eRecht24, IT-Recht-Kanzlei — anwaltlich geprueft, App-Modul verfuegbar
- **AVV-Muster:** Bitkom DPA-Vorlage (kostenlos)
- **Widerrufsbelehrung:** Haendlerbund-Muster oder IT-Recht-Kanzlei
- **DSFA-Muster:** Bayerisches Landesamt fuer Datenschutzaufsicht (BayLDA)
- **TOM-Vorlage:** GDD e.V.
- **SCC-Text (2021):** EU-Kommission offiziell
- **AI Act Compliance Guide:** ki-kanzlei.de

**WICHTIG:** Der Skill darf KEINE anwaltlich wirkenden endgueltigen Rechtstexte als "fertig"
verkaufen. Er darf Entwuerfe, Lueckenlisten, Musterhinweise mit Quellen und anwaltliche
Pruefpunkte erstellen.
