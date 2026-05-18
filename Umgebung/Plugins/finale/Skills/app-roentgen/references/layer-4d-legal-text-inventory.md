# Schicht 4d — Legal-Text-Inventar (Grundlage fuer den Rechtssicherheits-Skill)

> **FIX AA5 (Audit 10) — Kotlin + Java:** Patterns mit `--include='*.kt'` muessen bei Java-Hybrid-Apps um `--include='*.java'` ergaenzt werden (Permission-Requests via `requestPermissions(...)`, AccountManager-Java-APIs, klassische AlertDialog.Builder fuer Account-Deletion-Confirms). Reine Kotlin-Apps koennen den Java-Filter weglassen.

## Warum diese Schicht existiert

Die meisten Abmahnungen treffen Apps nicht bei den Marketing-Werbeaussagen (Layer 7), sondern bei rechtlich obligatorischen Texten:

| Rechtsfehler | Folge |
|--------------|-------|
| Permission-Rationale fehlt oder vage | Play-Store-Rejection + DSGVO Art. 13 Verstoss |
| Cookie-/Consent-Banner ungueltig (kein "Ablehnen" gleichrangig) | EU TDDDG + DSGVO — bis zu 4% Umsatz Bussgeld |
| Health-/AI-Disclaimer fehlt bei Health-/AI-Features | FTC Endorsement + EU AI Act |
| Account-Deletion-Dialog nicht eindeutig "unwiderruflich" | DSGVO Art. 17 — Abmahnungsrisiko |
| Newsletter-Opt-In ohne Double-Opt-In-Hinweis | UWG §7 — Abmahnung garantiert |
| Widerrufsbelehrung fehlt oder fehlerhaft | BGB §312g — Widerrufsfrist verlaengert auf 12 Monate |
| Standort-Begruendung fehlt | Play Console Rejection seit 2024 |
| Werbe-Markierungen fehlen (gesponsert/Werbung/Anzeige) | UWG §5a Schleichwerbung |

Layer 4d ist das vollstaendige Inventar dieser Texte mit 1:1-Wortlaut, sodass der Rechtssicherheits-Skill jeden Punkt gegen aktuelle Rechtsprechung pruefen kann.

**Coverage-Beitrag fuer "rechtssichere Wortlaute": 100 Prozent**

## 4d.1 Permission-Rationale-Dialoge (PFLICHT)

Pro Runtime-Permission der App MUSS dokumentiert sein:
- Welcher Bildschirm fragt die Permission an?
- Welchen Rationale-Text zeigt die App vor der System-Abfrage?
- Was passiert wenn der Nutzer "Ablehnen" waehlt?
- Was passiert bei "Niemals fragen"?
- Wird auf Settings verwiesen wenn dauerhaft abgelehnt?

### Such-Patterns

```bash
# Runtime-Permissions im Code
grep -rn 'rememberPermissionState\|rememberMultiplePermissionsState\|requestPermissions\|checkSelfPermission\|shouldShowRequestPermissionRationale' --include='*.kt' --include='*.java' .

# Rationale-Strings in strings.xml
grep -E '<string name="[^"]*(permission|rationale|allow)[^"]*"' app/src/main/res/values/strings.xml -i

# Permission-Beschreibungen aus dem Manifest
grep -A 2 '<uses-permission' app/src/main/AndroidManifest.xml
```

### Pflicht-Output

```markdown
### 4d.1 Permission-Rationale

| Permission | Trigger-Screen | Rationale-Title (1:1) | Rationale-Body (1:1) | Allow-Button (1:1) | Deny-Verhalten | Settings-Verweis |
|-----------|---------------|----------------------|---------------------|---------------------|---------------|------------------|
| `CAMERA` | EntryEditor | "Kamera-Zugriff" (`perm_camera_title`) | "Wir brauchen die Kamera, damit du Fotos zu deinen Eintraegen hinzufuegen kannst." (`perm_camera_body`) | "Erlauben" (`perm_camera_allow`) | Snackbar "Foto nicht moeglich" | "Einstellungen oeffnen" → Settings.ACTION_APPLICATION_DETAILS_SETTINGS |
| `RECORD_AUDIO` | VoiceInput | ... | ... | ... | ... | ... |
| `POST_NOTIFICATIONS` | Onboarding-Screen 4 | ... | ... | ... | ... | ... |
| `ACCESS_FINE_LOCATION` | (App fragt nicht — wird in Manifest deklariert aber nicht angefragt — UNKLAR, manuell pruefen) | — | — | — | — | — |
```

### Rechtssicherheits-Pruefpunkte

- Rationale-Text muss **konkret** sein ("damit du Fotos zu Eintraegen hinzufuegen kannst") — vage Texte ("zur Verbesserung der Nutzererfahrung") sind DSGVO Art. 13 Verstoss
- Bei optionalen Permissions: Muss klar sein, dass App ohne diese Permission funktioniert
- Bei sensiblen Permissions (Standort, Mikrofon, Kamera, Health Connect): Play-Console-Pflicht-Erklaerung im Store-Listing erforderlich
- Bei "Niemals fragen": App muss Settings-Verweis bieten

## 4d.2 Consent-Banner (Analytics, Tracking, Marketing)

### Such-Patterns

```bash
# Firebase Consent Mode v2
grep -rn 'setConsent\|setAnalyticsCollectionEnabled\|setAnalyticsConsent\|ConsentStatus\|ConsentRequestParameters' --include='*.kt' --include='*.java' .

# Custom Consent-Flows
grep -rn 'ConsentScreen\|ConsentDialog\|TrackingConsent\|CookieBanner\|GdprConsent' --include='*.kt' .

# Google UMP (User Messaging Platform)
grep -rn 'UserMessagingPlatform\|ConsentInformation' --include='*.kt' --include='*.java' .

# Strings
grep -E '<string name="[^"]*(consent|tracking|analytics|cookie|gdpr|datenschutz_zustimm)[^"]*"' app/src/main/res/values/strings.xml -i
```

### Pflicht-Output

```markdown
### 4d.2 Consent-Banner

Implementierung: Firebase Consent Mode v2 / Google UMP / Eigene Loesung / KEINE

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Banner-Title | | | EU TDDDG-konform? |
| Banner-Body | | | Erwaehnt Auftragsverarbeiter? |
| "Akzeptieren" Button | | | "Akzeptieren" muss gleich gross sein wie "Ablehnen" |
| "Ablehnen" Button | | | Pflicht seit BGH-Urteil 2024 |
| "Einstellungen" Button | | | Granularitaet anbieten |
| Granulare Optionen | | | Pro Kategorie eigener Toggle (Analytics, Marketing, Personalisierung) |
| Bestaetigungs-Bestaetigung | | | Erfolgsmeldung nach Auswahl |
```

### Rechtssicherheits-Pruefpunkte

- **"Akzeptieren"** und **"Ablehnen"** muessen **gleich prominent** sein (gleiche Groesse, gleicher Stil) — sonst Dark Pattern (BGH Az. I ZR 81/22)
- Kein "Akzeptieren" als Default-Vorauswahl bei nicht-essenziellen Tracking-Cookies
- Granulare Auswahl pro Tracking-Zweck (Analytics, Marketing, Personalisierung) anbieten
- Consent muss vor erstem Tracking-Call kommen — kein "Pre-Loading" vor Consent
- Widerrufs-Moeglichkeit so einfach wie Zustimmung (Settings → Datenschutz → "Einwilligung widerrufen")

## 4d.3 AGB-, Datenschutz- und Impressums-Links (PFLICHT)

### Such-Patterns

```bash
# Link-Strings
grep -E '<string name="[^"]*(terms|privacy|imprint|impressum|agb|datenschutz|legal)[^"]*"' app/src/main/res/values/strings.xml -i

# URL-Strings (translatable=false-Kandidaten)
grep -E '<string[^>]*>https?://' app/src/main/res/values/strings.xml | head -20

# Aufruf im Code (oeffnen via Intent.ACTION_VIEW)
grep -rn 'Intent\.ACTION_VIEW' --include='*.kt' . | head -10
```

### Pflicht-Output

```markdown
### 4d.3 Rechtstexte-Links

| Dokument | URL | Link-Text (DE 1:1) | Wo erreichbar (Wege in der App) | Sprachvarianten verfuegbar |
|----------|-----|---------------------|--------------------------------|--------------------------|
| AGB / Nutzungsbedingungen | https://... | "Allgemeine Geschaeftsbedingungen" | Settings → Rechtliches; Paywall-Footer; Onboarding Seite 5 | DE/EN — andere fehlen |
| Datenschutzerklaerung | https://... | "Datenschutzerklaerung" | Settings → Datenschutz; Onboarding; Paywall-Footer | DE/EN |
| Impressum | https://... | "Impressum" | Settings → Rechtliches → Impressum | DE-only |
| Widerrufsbelehrung | (intern oder URL) | "Widerrufsrecht" | Paywall-Footer; Settings → Rechtliches | DE-only |
| Cookie-Richtlinie (falls Web-Komponente) | https://... | "Cookie-Richtlinie" | im Consent-Banner | DE/EN |
| Lizenzen Open-Source | (intern, OssLicensesMenuActivity) | "Open-Source-Lizenzen" | Settings → Ueber → Lizenzen | n/a (technisch) |
```

### Rechtssicherheits-Pruefpunkte

- URLs MUESSEN erreichbar sein (HTTP 200, nicht 404)
- Bei deutschsprachigem App-Angebot: Impressum DE-Pflicht (TMG/MStV) — auch fuer NPCs
- Datenschutzerklaerung MUSS die Auftragsverarbeiter benennen (Firebase, Google Ads, Analytics, etc.)
- Bei Online-Bezahlung: Widerrufsbelehrung Pflicht (BGB §312g, EU UCPD)
- Links muessen im Subscription-Flow VOR dem Kauf-Klick verfuegbar sein
- AGB muessen vom Nutzer akzeptiert werden (Checkbox + Link) bei erstem Login oder Onboarding

## 4d.4 Health-Disclaimer (bei Fitness-/Mental-Health-Apps)

### Auto-Erkennung

```bash
# Health-Indikatoren
grep -rln 'HealthConnect\|GoogleFit\|fitness\|workout\|meditation\|mental_health\|mood\|symptom\|medication\|therapy\|wellbeing' --include='*.kt' --include='*.java' . | head -20

# Aerztliche Begriffe in strings.xml
grep -iE 'diagnose|therapie|behandlung|medikament|krankheit|symptom|aerzt|arzt|psycholog' app/src/main/res/values/strings.xml | head -10
```

### Pflicht-Output

```markdown
### 4d.4 Health-Disclaimer

App-Typ: Fitness / Mental-Health / Wellness / Medizinprodukt / Keine Health-Features

Falls Health-relevant:

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Globaler Health-Disclaimer | | | Vorhanden? |
| AI-Antwort-Disclaimer (bei Health-Fragen) | | | Pro KI-Antwort Hinweis? |
| Notfall-Hinweis (Suizidpraevention, Krise) | | | Hotline 112/116111 verlinkt? |
| Datenschutz-Hinweis Health-Daten | | | Erwaehnt Health Connect / besondere Kategorien Art. 9 DSGVO? |
```

### Rechtssicherheits-Pruefpunkte

- **Keine medizinische Beratung**-Hinweis Pflicht bei mood/symptom-Tracking
- Bei Mental-Health-Themen: **Notfall-Nummern** (112, 116111 fuer Deutschland) ggf. einblenden
- Bei AI-generierten Health-Antworten: Disclaimer pro Antwort, nicht nur global
- Health-Daten sind besondere personenbezogene Daten (Art. 9 DSGVO) — explizite Einwilligung
- Falls App als Medizinprodukt klassifiziert: MDR-CE-Kennzeichnung erforderlich
- Bei Health Connect: Pflicht-Permissions-Begruendung gemaess Play-Console

## 4d.5 AI-Disclaimer (KI-generierte Inhalte)

### Auto-Erkennung

```bash
# KI-SDK-Indikatoren
grep -rln 'GenerativeModel\|GeminiClient\|OpenAI\|Anthropic\|generateContent\|chat\.completions\|GPT\|LLM' --include='*.kt' --include='*.java' . | head -20

# AI-Strings in strings.xml
grep -iE '<string name="[^"]*"[^>]*>[^<]*(KI|AI|kuenstliche Intelligenz|artificial intelligence|chatbot|assistent)' app/src/main/res/values/strings.xml | head -20
```

### Pflicht-Output

```markdown
### 4d.5 AI-Disclaimer

KI-SDK verwendet: Google Gemini / OpenAI / Anthropic / On-Device-ML / Keine

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Globaler KI-Hinweis (Onboarding/Settings) | | | Vorhanden? |
| Disclaimer pro KI-Antwort | | | Wird angezeigt? |
| KI-Generierung-Marker (z.B. "KI-Antwort") | | | Sichtbar erkennbar? |
| Hinweis auf KI-Fehlerrisiko | | | "kann Fehler enthalten" oder aehnlich? |
| Datenfluss-Hinweis (z.B. "wird an Google gesendet") | | | Erklaert wo die Daten landen? |
| Halluzinations-Warnung bei Health/Recht/Finanz | | | Pflicht bei sensiblen Domaenen? |
| Trainings-Daten-Hinweis (werden Eintraege zum Training verwendet?) | | | DSGVO-Pflicht-Erklaerung |
```

### Rechtssicherheits-Pruefpunkte

- **EU AI Act** (in Kraft 2025): Transparenzpflicht bei KI-generierten Inhalten
- **FTC Endorsement Guides**: KI-Behauptungen brauchen Substanz ("KI-gestuetzt" ohne LLM-Aufruf = irrefuehrend)
- Bei Health/Recht/Finanz-Domain: explizite Warnung dass KI-Antworten keine professionelle Beratung ersetzen
- Daten-Pipeline-Transparenz: Welche Daten gehen zu welchem Anbieter
- Opt-Out moeglich, wenn Anbieter Trainings-Nutzung erlaubt
- KI-generierte Bilder muessen technisch oder visuell als solche erkennbar sein (EU AI Act Art. 50)

## 4d.6 Werbe-Markierungen (Schleichwerbung verhindern)

### Such-Patterns

```bash
# Ad-SDKs
grep -rln 'AdMob\|AdSense\|MoPub\|FacebookAd\|InterstitialAd\|RewardedAd\|BannerAd' --include='*.kt' --include='*.java' .

# Werbe-Strings
grep -iE '<string name="[^"]*"[^>]*>[^<]*(werbung|anzeige|gesponsert|sponsored|ad |advertisement|werbeanzeige)' app/src/main/res/values/strings.xml | head -20

# AffiliateLink-Indikatoren
grep -rn 'affiliate\|sponsor\|partnerprogramm\|tracking_id\|utm_' --include='*.kt' --include='*.xml' . | head -20
```

### Pflicht-Output

```markdown
### 4d.6 Werbe-Markierungen

Ad-SDK verwendet: AdMob / Meta / Keine
Affiliate-Links vorhanden: JA / NEIN

| Komponente | String-Key | Markierung-Wortlaut (DE 1:1) | Pruefung |
|------------|-----------|-----------------------------|----------|
| Banner-Ad-Label | | "Anzeige" / "Werbung" | Klar erkennbar? |
| Interstitial-Schliessen-Button | | "Schliessen" / "X" | 5 Sekunden Mindest-Wartezeit? |
| Native-Ad-Markierung | | "Gesponsert" | Erkennbar von echtem Content? |
| Affiliate-Link-Hinweis | | "*Provisionslink" / "Werbung" | Sichtbar markiert? |
| Subscription-Promo (NICHT Werbung im Sinne UWG) | n/a | n/a | (eigene Promo zaehlt nicht als Werbung) |
```

### Rechtssicherheits-Pruefpunkte

- **UWG §5a Abs. 4**: Kommerzieller Zweck muss erkennbar sein — "Werbung", "Anzeige", "Sponsored" sind Pflicht-Worte
- **TMG §6 (jetzt DDG §6)**: Anzeige als solche erkennbar
- Affiliate-Links: Mindestens **eine** der drei Kennzeichnungen ("Werbung", "Anzeige", "Provisionslink") direkt am Link
- Interstitials: Schliessen-Option muss nach max. 5 Sekunden erkennbar sein
- Native-Ads (wie Eintraege im Feed gemischt): "Gesponsert"-Label direkt am Eintrag
- Bei In-App-Promotions eigener Premium-Produkte: KEINE Werbe-Markierung notwendig

## 4d.7 Account-Deletion (DSGVO Art. 17 — KRITISCH)

### Such-Patterns

```bash
# Account-Deletion-Code
grep -rn 'deleteAccount\|removeUser\|clearAllData\|gdprDelete\|requestAccountDeletion\|FirebaseAuth.*delete' --include='*.kt' --include='*.java' .

# Strings
grep -iE '<string name="[^"]*(delete_account|account_delete|kontoloesch|konto_loesch|gdpr|datenloesch)[^"]*"' app/src/main/res/values/strings.xml | head -20
```

### Pflicht-Output

```markdown
### 4d.7 Account-Deletion-Flow

In-App-Loeschung vorhanden: JA / NEIN (KRITISCH wenn nein — Play-Store-Policy seit 2024)
Web-URL-Loeschung vorhanden: JA / NEIN (Pflicht seit 2024)

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Einstiegspunkt Settings-Item | | "Konto loeschen" | Sichtbar in Settings? |
| Einstiegspunkt Beschreibung | | "Alle Daten unwiderruflich loeschen" | Klar formuliert? |
| Confirm-Dialog Title | | "Konto wirklich loeschen?" | Eindeutig? |
| Confirm-Dialog Body | | "Alle deine Eintraege, Premium-Abo, Backups werden unwiderruflich geloescht. Dieser Vorgang kann nicht rueckgaengig gemacht werden." | Liste aller Datenkategorien? Wort "unwiderruflich"? |
| Confirm-Button | | "Ja, endgueltig loeschen" | Negative Bestaetigung? |
| Dismiss-Button | | "Abbrechen" | Default-Auswahl? |
| Erfolgs-Meldung | | "Dein Konto wurde geloescht." | Bestaetigung? |
| Email-Bestaetigung (falls vorhanden) | | "Wir haben deine Loeschung bestaetigt..." | Audit-Trail? |
| Backup/Drive-Loeschung-Hinweis | | "Dein Drive-Backup wird ebenfalls geloescht." | Verarbeiter mitloeschen? |
```

### Rechtssicherheits-Pruefpunkte

- **DSGVO Art. 17**: "Recht auf Vergessenwerden" — JEDE Datenkategorie muss geloescht werden
- **Google Play Policy 2024**: In-App-Loeschung UND Web-URL-Loeschung Pflicht
- Wort "unwiderruflich" oder "kann nicht rueckgaengig gemacht werden" MUSS im Confirm-Dialog stehen
- Confirm-Button sollte NICHT als Default markiert sein (Dark Pattern)
- Auftragsverarbeiter (Firebase, Cloud-Storage) muessen mitgeloescht werden
- 30-Tage-Loeschfrist (DSGVO konform), kein "soft delete" mit unbegrenzter Aufbewahrung

## 4d.8 Newsletter- und Marketing-Opt-In

### Such-Patterns

```bash
grep -rn 'newsletter\|mailingList\|marketing_opt_in\|emailOptIn' --include='*.kt' --include='*.xml' . -i

grep -iE '<string name="[^"]*(newsletter|mailing|marketing|opt_in)[^"]*"' app/src/main/res/values/strings.xml | head -20
```

### Pflicht-Output

```markdown
### 4d.8 Newsletter / Marketing-Opt-In

Newsletter-Feature vorhanden: JA / NEIN

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Opt-In-Checkbox-Label | | "Ich moechte den Newsletter erhalten" | Klar Opt-In, nicht Opt-Out? |
| Hinweis-Text (Erlaeuterung) | | "Du kannst dich jederzeit abmelden..." | Widerrufshinweis enthalten? |
| Double-Opt-In-Bestaetigungs-Email-Hinweis | | "Wir haben dir eine Bestaetigungs-E-Mail gesendet..." | Hinweis dass Email folgt? |
| Abmelde-Link in Email (falls bekannt) | | "Abmelden" | Funktioniert? Einklick? |
| Abmelde-Erfolg | | "Du wurdest abgemeldet" | Bestaetigung? |
```

### Rechtssicherheits-Pruefpunkte

- **UWG §7 Abs. 2**: Double-Opt-In ZWINGEND fuer kommerzielle E-Mails an Privatpersonen
- Checkbox darf NICHT vorab angekreuzt sein
- Abmelde-Link in JEDER E-Mail Pflicht
- Bei "Bonus fuer Anmeldung": Klar machen dass Anmeldung gleichzeitig Werbe-Einwilligung ist
- Zweck der Verarbeitung in Opt-In-Hinweis nennen ("Produkt-Updates, Tipps, Aktionen")

## 4d.9 In-App-Kauf-Confirmation

### Such-Patterns

```bash
# Billing-Confirmation-Strings
grep -iE '<string name="[^"]*(purchase|kauf|abo_confirm|subscription_confirm)[^"]*"' app/src/main/res/values/strings.xml | head -20
```

### Pflicht-Output

```markdown
### 4d.9 In-App-Kauf-Bestaetigungs-Texte

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Pre-Purchase-Bestaetigung (Title) | | | Vor System-Sheet eigener Dialog? |
| Preis mit Waehrung | | "4,99 € / Monat" | Exakter Preis sichtbar? |
| Abrechnungszeitraum | | "monatlich" | Klar? |
| Auto-Renewal-Hinweis | | "Verlaengert sich automatisch..." | Pflicht-Hinweis? |
| Cancel-Hinweis | | "Jederzeit kuendbar in Google Play" | Klar wo gekuendigt wird? |
| Trial-Hinweis (falls Trial) | | "7 Tage kostenlos, danach 4,99 €/Monat" | Folgepreis sichtbar? |
| Post-Purchase-Bestaetigung | | "Du bist jetzt Premium!" | Erfolgsmeldung? |
| Steuerhinweis | | "inkl. MwSt." | Brutto-Preis? |
```

### Rechtssicherheits-Pruefpunkte

- **Google Play Subscriptions Policy**: Preis, Intervall, Auto-Renewal, Cancel-Hinweis Pflicht VOR Kauf
- **EU Geo-Blocking-Verordnung**: Preise in lokaler Waehrung
- **BGB §312j**: Button-Text muss Zahlungsverpflichtung erkennen lassen ("Zahlungspflichtig bestellen" oder Aequivalent)
- Bei Trial: Folgepreis + Trial-Endedatum sichtbar
- Brutto-Preis (inkl. MwSt) — keine "ab"-Preise ohne Endpreis

## 4d.10 Widerrufsbelehrung (bei Online-Kaeufen)

### Such-Patterns

```bash
grep -iE '<string name="[^"]*(widerruf|withdraw|refund|cancellation_right)[^"]*"' app/src/main/res/values/strings.xml | head -20
```

### Pflicht-Output

```markdown
### 4d.10 Widerrufsbelehrung

Widerrufsbelehrung vorhanden: JA / NEIN (Pflicht bei Online-Kaeufen in DE!)

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Widerrufstext (kompletter Standardtext gemaess BGB Anlage 2) | | "Widerrufsrecht..." | Original-Wortlaut gemaess Anlage? |
| Widerrufsformular-Verlinkung | | "Muster-Widerrufsformular" | Verlinkt? |
| Erloeschen-Hinweis (bei digitalen Inhalten) | | "Das Widerrufsrecht erlischt, wenn die Ausfuehrung begonnen hat und du dem ausdruecklich zugestimmt hast." | BGB §356 Abs. 5 erfuellt? |
| Frist-Hinweis | | "14 Tage" | Korrekt? |
```

### Rechtssicherheits-Pruefpunkte

- **BGB §312g**: Widerrufsbelehrung Pflicht bei B2C-Online-Kaeufen
- Bei fehlender Belehrung: Widerrufsfrist verlaengert sich auf **12 Monate + 14 Tage**
- Digitale Inhalte: Pflicht-Erloeschenshinweis MUSS Nutzer vor Beginn der App-Nutzung zustimmen lassen
- Bei Abos: Widerrufsrecht gilt fuer Vertragsabschluss, nicht fuer jede Verlaengerung

## 4d.11 Standort-Begruendung (Play-Console seit 2024)

### Such-Patterns

```bash
# Standort-Permissions im Manifest
grep -E 'android.permission.ACCESS_(FINE|COARSE|BACKGROUND)_LOCATION' app/src/main/AndroidManifest.xml

# Code-Aufrufe
grep -rn 'LocationManager\|FusedLocationProviderClient\|getLastLocation\|requestLocationUpdates' --include='*.kt' --include='*.java' . | head -10

# Strings
grep -iE '<string name="[^"]*(location|standort|gps)[^"]*"' app/src/main/res/values/strings.xml | head -10
```

### Pflicht-Output

```markdown
### 4d.11 Standort-Verwendung

Standort-Permissions im Manifest: FINE / COARSE / BACKGROUND / Keine

| Permission | App-Verwendung | Begruendung in App | Play-Store-Begruendung | Pruefung |
|-----------|---------------|---------------------|------------------------|----------|
| `ACCESS_FINE_LOCATION` | (z.B. fuer Eintrags-Geo-Tag) | "..." (`location_rationale`) | (manuell aus Play Console) | Begruendung passt? |
| `ACCESS_BACKGROUND_LOCATION` | n/a | n/a | n/a | Wenn NICHT verwendet: aus Manifest entfernen |
```

### Rechtssicherheits-Pruefpunkte

- **Play Console seit 2024**: Bei FINE oder BACKGROUND_LOCATION explizite Begruendung im Store-Listing Pflicht
- Background-Location nur wenn wirklich noetig (Play-Store-Review-Trigger)
- Rationale in App muss konkret sein ("Standort fuer Eintraege-Map") nicht vage ("Verbesserung des Service")
- DSGVO Art. 13: Verarbeitungszweck nennen

## 4d.12 Altersfreigabe-Anzeige in App

### Such-Patterns

```bash
# USK/PEGI/IARC-Aufrufe
grep -rn 'IARC\|USK\|PEGI\|ageRating\|contentRating' --include='*.kt' --include='*.xml' .

# Adult-Content-Marker
grep -iE '<string name="[^"]*(age_rating|altersfreigabe|adult|erwachsen)[^"]*"' app/src/main/res/values/strings.xml
```

### Pflicht-Output

```markdown
### 4d.12 Altersfreigabe

App-Klassifizierung (Play Console): USK 0 / 6 / 12 / 16 / 18, PEGI 3/7/12/16/18, IARC

| Slot | String-Key | Wortlaut (DE 1:1) | Pruefung |
|------|-----------|-------------------|----------|
| Altersfreigabe-Anzeige in Settings (Ueber) | | | Anzeige der Klassifizierung |
| Adult-Content-Warnung (falls 18+) | | | Beim ersten Start? |
| Eltern-Hinweis bei Kinder-/Familien-Apps | | | Play-Store Designed-for-Families-Programm? |
```

### Rechtssicherheits-Pruefpunkte

- **JuSchG / Jugendschutzgesetz**: Bei USK 18+ Anzeige der Altersfreigabe Pflicht
- **Designed for Families**: Bei Kinder-Apps zusaetzliche Audit-Punkte (kein 3rd-party-Tracking, keine externen Links ohne Eltern-Tor)
- IARC-Rating muss in Play Console korrekt deklariert sein

## 4d.13 Vollstaendigkeits-Statistik

| Metrik | Wert |
|--------|------|
| Runtime-Permissions im Code | N |
| Permissions mit Rationale-Dialog | N |
| Permissions OHNE Rationale (KRITISCH) | N (Liste) |
| Consent-Banner vorhanden | JA / NEIN |
| Consent: "Akzeptieren" / "Ablehnen" gleich prominent | JA / NEIN / N/A |
| AGB-URL vorhanden + erreichbar | JA / NEIN |
| Datenschutzerklaerung URL vorhanden + erreichbar | JA / NEIN |
| Impressum (bei DE-Angebot) | JA / NEIN |
| Health-Disclaimer (falls Health-App) | JA / NEIN / N/A |
| AI-Disclaimer (falls KI-App) | JA / NEIN / N/A |
| Werbe-Markierungen (falls Ads) | JA / NEIN / N/A |
| In-App-Account-Loeschung | JA / NEIN |
| Web-URL-Account-Loeschung | JA / NEIN |
| Newsletter-Double-Opt-In | JA / NEIN / N/A |
| Widerrufsbelehrung (falls Online-Kauf) | JA / NEIN / N/A |
| Standort-Begruendung | JA / NEIN / N/A |
| Altersfreigabe-Anzeige | JA / NEIN |

## 4d.14 Typische Fehlerquellen

- **Permission im Manifest deklariert aber nie angefragt**: zerschiesst Play-Store-Audit
- **Datenschutzerklaerung-URL ist 404**: passiert nach Webseiten-Umzug oft
- **Consent-Banner zeigt "Akzeptieren" gross, "Ablehnen" als Text-Link**: Dark Pattern, BGH-rechtswidrig
- **Account-Loeschung loescht nicht das Drive-Backup**: DSGVO Art. 17 nicht erfuellt
- **AI-Antworten ohne Disclaimer in Health-Kontext**: FTC + EU AI Act Verstoss
- **Werbung nicht gekennzeichnet weil "ist ja unsere eigene App"**: stimmt nur fuer eigene Premium-Promo, NICHT fuer Cross-Promo zu anderen Apps
- **Newsletter-Opt-In vorab angekreuzt**: UWG §7 + DSGVO Art. 7 Verstoss
- **Auto-Renewal-Hinweis nur im Footer in 8pt-Schrift**: Pflicht-Disclosures muessen prominent sein

## 4d.15 Was diese Schicht NICHT macht

- Sie pruefen NICHT, ob ein Text rechtlich KORREKT formuliert ist — das macht der Rechtssicherheits-Skill mit aktueller Rechtsprechung
- Sie erstellen KEINE Rechtstexte — sie inventarisieren nur die vorhandenen
- Sie ersetzen KEINE anwaltliche Beratung — sind nur die technische Pruefhilfe
