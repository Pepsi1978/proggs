# Google Play Policies 2025/2026 — App-Release-Pflicht-Updates

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Vor JEDEM Play-Console-Release. Auch bei laufender App: alle 30
> Tage neu pruefen.

## April-2026-Policy-Update (Frist ~15.05.2026)

### Permissions

| Permission | Aenderung | Was zu tun |
|--|--|--|
| **Contacts** | Apps ohne breiten Kontaktzugang MUESSEN den **Android Contact Picker** nutzen | READ_CONTACTS-Permission entfernen, Picker integrieren |
| **Location** | Location Button als empfohlener Mindestscope fuer precise location | ACCESS_FINE_LOCATION durch Location Button ersetzen wo moeglich |
| **Foreground Services** | Geofencing entfernt als anerkannter Use Case → **Geofence API** stattdessen | Foreground Service-Workaround durch Geofence API ersetzen |

### Age-Restricted Content
Dating-Apps mit altersbeschraenkten Inhalten als "beilaeufigem Feature" brauchen keine
Restrict-Minor-Access mehr wenn alternative Altersverifikation vorhanden.

### News-Apps Self-Declaration
Bis **27.05.2026** MUSS Selbstdeklaration abgegeben werden.

### Prediction Markets Pilot
Enrollment bis 01.06.2026.

## Account Deletion (bestaetigt/verschaerft)

- Muss sowohl **in-app als auch ausserhalb der App (Web-URL)** erreichbar sein
- Bei Account-Loeschung MUESSEN zugehoerige Nutzerdaten ebenfalls geloescht werden
- Pflicht seit 2023 enforced

## Health & Fitness Apps (April 2026 Update)

- Android 16 granulare Permissions: Menstrual Cycle Phases, Alcohol Consumption, Symptoms in Health Connect — als "hochsensibel" klassifiziert
- **Verbotene Nutzung** von sensitiven Gesundheitsdaten fuer Beschaeftigungs-/Versicherungsentscheidungen
- **Health Apps Declaration Form** mandatory seit 08/2025
- Medical Disclaimer im Store-Listing Pflicht (erster Absatz der Beschreibung!)

## Generative AI Apps Policy (konsolidiert 2024/2025)

- **In-App Flagging Pflicht:** Apps mit KI-Content-Generierung MUESSEN In-App-Meldefunktion haben
  (Nutzer muss offensive Inhalte melden ohne App zu verlassen)
- Kein "Restricted Content" generierbar (Child Safety, Deception)
- "Rigorous testing" der KI-Modelle vorausgesetzt
- Gilt fuer Text-to-text Chatbots, Text/Voice-to-Image, KI-generierte Audio/Video
- **AI-Generated Content Declaration** in Play Console aktiv

## Subscription / Billing Updates

- **Cancel-Subscription-Button direkt in App** (Play Billing Policy seit 2024)
- Subscription Cancellation API verpflichtend
- Klare Preis-/Laufzeit-/Kuendigungsangaben
- Auto-Renewal explizit kommunizieren

## SDK Index (2025)

- Google fuehrt SDK Index — Daten von Drittanbieter-SDKs sind nachvollziehbar
- Apps mit SDKs muessen Data Safety Form entsprechend ausfuellen

## Crypto / Financial Services / Real-Money Gambling

- Erweiterung 2025 — wenn App finanzielle Dienste anbietet: Financial Services Declaration Pflicht
- Real-Money Gambling: erweiterte Pflichten 2025

## Pflicht-Deklarationen in Play Console (Master-Liste)

| Deklaration | Pflichtgrad | Hinweis |
|--|--|--|
| **Data Safety Form** | PFLICHT (Mandatory) | Alle Datentypen, SDK-Daten inkl., Android ID ab 2025 explizit deklarieren |
| **IARC Content Rating** | PFLICHT | Ohne kein DE-Release |
| **Target Audience Declaration** | PFLICHT | Bestimmt Families-Policy |
| **Permissions Declaration** | PFLICHT | Jede sensitive Permission begruenden |
| **Health Apps Declaration** | PFLICHT bei Health-Feature | Mandatory seit 08/2025, 01/2026 verschaerft |
| **Financial Services Declaration** | PFLICHT bei Finanzprodukten | — |
| **VPN Declaration** | PFLICHT bei VPN | — |
| **Real-Money Gambling Declaration** | PFLICHT bei Gluecksspiel | — |
| **Government Apps Declaration** | PFLICHT bei Behoerdenapp | — |
| **AI-Generated Content Declaration** | PFLICHT bei KI-Inhalten | Seit 2025 enforced |
| **News Apps Declaration** | PFLICHT bei News-Aggregation | Bis 27.05.2026 |

## Play-Console-Checkliste (Block in Berichtsvorlage)

- [ ] Data Safety passt zu Code und SDKs
- [ ] Privacy Policy URL erreichbar
- [ ] Account Deletion beantwortet + verlinkt (in-app + web-url)
- [ ] App Access korrekt
- [ ] Content Rating (IARC) korrekt
- [ ] Target Audience / Families korrekt
- [ ] Ads / Health / AI / Finance / UGC / Permissions Declarations korrekt
- [ ] Subscription-Cancel-Button in App
- [ ] Bei KI-Features: AI-Generated Content Declaration + In-App-Flagging
- [ ] Bei Health-Features: Health Apps Declaration
- [ ] Permissions: Contacts Picker (April-2026) statt voller READ_CONTACTS

## Quellen
- `support.google.com/googleplay/android-developer`
- `play.google.com/console/about/policy`
- `developer.android.com/privacy-and-security`
- Google Play Policy Update April 15, 2026
- ASOWorld Google Play Policy Updates
- Google Play Health Apps Update 01/2026
