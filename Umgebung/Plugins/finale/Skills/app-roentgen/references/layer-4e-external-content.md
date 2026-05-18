# Schicht 4e — Externe Inhalte (ausserhalb des Code-Repos)

## Warum diese Schicht existiert

Apps haben oft mehr Wortlaute ausserhalb des Repos als innerhalb:

| Externe Quelle | Wo lebt sie | Warum sie wichtig ist |
|---------------|-------------|----------------------|
| Google Play Store Listing | Play Console (Web-UI) | Erstes was Nutzer sehen, UWG-Werbeaussagen-Pflicht |
| Firebase Remote Config | Firebase Console (Web-UI) | Aktive Texte koennen sich aendern ohne App-Update |
| Cloud Functions Notification-Templates | Firebase Functions Code | Server-getriggerte Pushes (Trial-End, Subscription-Status) |
| Email-Templates | Firebase Auth / Stripe / Sendgrid | Welcome, Trial-End, Password-Reset, Cancel-Bestaetigung |
| WebView-Inhalte | `assets/*.html`, externe URLs | Inhalte oft NICHT in strings.xml |
| PDF-Export-Vorlagen | Code-Templates, oft hardcoded | Werden nicht uebersetzt, oft DE-only |
| Customer-Support-Texte | Helpcenter, Intercom, Zendesk | Externes System mit eigenem Audit-Bedarf |
| Marketing-Material | Screenshots, Promo-Videos | Im Play-Store sichtbar, UWG-relevant |

Layer 4e listet diese Quellen mit klarem Action-Plan was Frank manuell beitragen muss bzw. was der Skill automatisch finden kann.

**Coverage-Beitrag fuer "vollstaendige App-Sicht": die letzten ~20 Prozent die sonst durchrutschen**

## 4e.1 Google Play Store Listing (Frank-Aufgabe, PFLICHT-Block)

### Inhalt der manuellen Beitrags-Aufforderung

```markdown
### 4e.1 Play Store Listing

Frank, bitte folgende Texte aus der Google Play Console eingeben (Store-Eintrag → Hauptangebot):

#### App-Titel
| Sprache | Wortlaut (max 30 Zeichen) | Audit |
|---------|--------------------------|-------|
| DE | (eingeben) | UWG-Pruefung in Schicht 7 |
| EN | (eingeben) | |
| ... | ... | |

#### Kurze Beschreibung (max 80 Zeichen)
| Sprache | Wortlaut (1:1) | Pruefung |
|---------|---------------|----------|
| DE | (eingeben) | Werbeaussagen pruefen |
| EN | (eingeben) | |
| ... | ... | |

#### Vollstaendige Beschreibung (max 4000 Zeichen)
| Sprache | Wortlaut (1:1, vollstaendig) |
|---------|-----------------------------|
| DE | (kompletter Text einfuegen) |
| EN | (kompletter Text einfuegen) |
| ... | ... |

#### Feature-Highlights / Bullet-Points
(Pro Sprache: jede Bullet 1:1 zitieren)

#### Screenshot-Texte
| Screenshot # | Sprache | Eingebetteter Text (1:1) | Pruefung |
|-------------|---------|--------------------------|----------|
| 1 | DE | (Text auf dem Screenshot) | Konsistent mit App? |
| 1 | EN | ... | |
| 2 | DE | ... | |
| ... | ... | ... | ... |

#### Promo-Video-Texte (falls vorhanden)
- Titel
- Beschreibung
- Eingeblendete Texte
```

### Auto-Erkennung was zu pruefen ist

```bash
# Anzeichen dass Store-Listing wichtig ist
grep -rn 'play\.google\.com.*details' --include='*.kt' --include='*.md' . | head -5
ls fastlane/metadata/ 2>/dev/null && echo "FOUND: fastlane-Metadata fuer Play Store"
find . -name 'short_description.txt' -o -name 'full_description.txt' -o -name 'title.txt' 2>/dev/null | head -10
```

**Falls `fastlane/metadata/` existiert** (Fastlane-Konvention fuer Play-Store-Texte), kann der Skill automatisch lesen:

```bash
# Fastlane-Metadata-Verzeichnisse
ls -d fastlane/metadata/android/*/ 2>/dev/null

# Pro Sprache die drei Pflicht-Texte
for d in fastlane/metadata/android/*/; do
  lang=$(basename "$d")
  echo "=== $lang ==="
  cat "$d/title.txt" 2>/dev/null
  echo "---"
  cat "$d/short_description.txt" 2>/dev/null
  echo "---"
  cat "$d/full_description.txt" 2>/dev/null | head -50
done
```

### Rechtssicherheits-Pruefpunkte

- Werbeaussagen im Store-Listing unterliegen UWG genauso wie In-App-Aussagen
- Title-Limit: 30 Zeichen — fuer Uebersetzungen besonders eng
- Short-Description: 80 Zeichen — kritisch fuer Lokalisierung
- Feature-Graphic (Banner-Bild): falls Text drauf, MUSS auditiert werden
- Screenshots mit eingebettetem Text: gelten als Werbeaussage
- Promo-Video: gelten als Werbeaussage, brauchen ggf. Untertitel

## 4e.2 Firebase Remote Config (Server-gesteuerte Texte)

### Such-Patterns

```bash
# Remote Config-Aufrufe
grep -rn 'remoteConfig\.getString\|remoteConfig\.fetchAndActivate\|FirebaseRemoteConfig' --include='*.kt' --include='*.java' .

# Default-Werte in XML
find . -name 'remote_config_defaults.xml' -not -path '*/build/*'
find . -name 'config_defaults*.xml' -not -path '*/build/*'

# Alle Remote-Config-Keys im Code
grep -roE 'remoteConfig\.getString\("[^"]+"\)' --include='*.kt' . | sed 's/remoteConfig\.getString("//' | sed 's/")//' | sort -u
```

### Pflicht-Output

```markdown
### 4e.2 Firebase Remote Config

| Remote-Key | Default-Wert (DE 1:1) | Aktueller Server-Wert (Frank manuell aus Firebase Console) | Wo angezeigt | Audit |
|------------|----------------------|-----------------------------------------------------------|--------------|-------|
| `paywall_headline_v2` | "Premium starten" | "Jetzt 50% Rabatt sichern!" | Paywall-Hauptbildschirm | Werbeaussage UWG pruefen |
| `onboarding_step2_body` | "..." | "..." | Onboarding Seite 2 | Konsistent? |
| `subscription_promo_label` | "Sonderaktion" | "Heute 30% gratis" | Paywall-Plan-Card | Streichpreis-Realitaet? |
```

### Audit-Pflichten

- Defaults aus `remote_config_defaults.xml` automatisch extrahieren
- Frank-Aufgabe: Live-Werte aus Firebase Console kopieren (kann sich taeglich aendern!)
- Sicherstellen dass Live-Werte nicht WIRTSCHAFTLICHE Werbeaussagen ohne Pflichtangaben enthalten

## 4e.3 Cloud Functions Notification-Templates

### Such-Patterns

```bash
# Cloud-Functions-Verzeichnis erkennen
find . -name 'functions' -type d -maxdepth 3 -not -path '*/node_modules/*' 2>/dev/null
find . -name 'firebase.json' -maxdepth 2 -not -path '*/build/*'

# In TS/JS-Funktionscode nach Notification-Templates suchen
grep -rn 'sendNotification\|messaging\.send\|admin\.messaging\|notification:' --include='*.ts' --include='*.js' functions/ 2>/dev/null | head -20

# Title/Body-Strings in Functions
grep -rnE '(title|body):\s*["`]' --include='*.ts' --include='*.js' functions/ 2>/dev/null | head -20
```

### Pflicht-Output

```markdown
### 4e.3 Cloud Functions Push-Templates

Cloud Functions Verzeichnis: <Pfad> / NICHT VORHANDEN

| Funktion | Trigger | Notification-Title (1:1) | Body (1:1) | Audit |
|----------|---------|--------------------------|------------|-------|
| `notifyTrialEnding` | 1 Tag vor Trial-Ende | "Dein Trial endet morgen" | "Behalte Premium fuer nur 4,99 €/Monat..." | UWG-Pruefung |
| `notifySubscriptionPaused` | Subscription paused | "Dein Abo ist pausiert" | "Es startet automatisch am ..." | DSGVO-Hinweis? |
| `notifyAccountDeleted` | Account-Loeschung | "Konto geloescht" | "..." | DSGVO Art. 17 Bestaetigung |
| `notifyPasswordReset` | Password Reset | "Passwort-Reset" | "..." | Security-Best-Practice |
```

### Audit-Pflichten

- Cloud-Functions-Texte sind oft auf Englisch hardcoded — i18n-Luecke fuer DE-User
- Trial-End-Reminder muss Auto-Renewal-Hinweis enthalten (Google Play Pflicht)
- Push-Texte muessen Werbeaussagen-Audit unterzogen werden

## 4e.4 Email-Templates (Firebase Auth, Stripe, Sendgrid)

### Such-Patterns

```bash
# Firebase Auth Email-Verifikation (oft mit Custom-Domain)
grep -rn 'sendEmailVerification\|sendPasswordResetEmail\|applyActionCode' --include='*.kt' --include='*.ts' --include='*.js' .

# Stripe Email-Templates (im Webhook)
grep -rn 'invoice\.upcoming\|invoice\.payment_failed\|customer\.subscription\.deleted' --include='*.ts' --include='*.js' .

# Sendgrid/Mailgun
grep -rn 'sendgrid\|mailgun\|@sendgrid\|sgMail' --include='*.ts' --include='*.js' . -i
```

### Pflicht-Output

```markdown
### 4e.4 Email-Templates

Email-Anbieter: Firebase Auth (Standard) / Sendgrid / Mailgun / Stripe Customer Portal

#### Frank-Aufgabe (manuelle Erfassung)

Bitte aus Firebase Console / Stripe / Sendgrid-Templates kopieren:

| Email-Typ | Sprachen | Subject (1:1) | Body (1:1, kompletter Text) | Audit |
|-----------|----------|---------------|----------------------------|-------|
| Email-Verifikation (Firebase Auth) | DE/EN | "Verifiziere deine Email-Adresse" | "Hallo,\n\nbitte klicke auf diesen Link, um deine Email-Adresse zu verifizieren..." | Absender-Adresse? Spam-Score? |
| Password-Reset (Firebase Auth) | DE/EN | "Passwort zuruecksetzen" | "..." | Phishing-Resistenz? |
| Account-Loeschungs-Bestaetigung | DE/EN | "Konto geloescht" | "..." | DSGVO Art. 17 Bestaetigungsmail? |
| Trial-Endet-Warnung (Stripe/Custom) | DE/EN | "Dein Trial endet bald" | "..." | UWG-Pflichtangaben? |
| Subscription-Cancelled-Bestaetigung | DE/EN | "Abo gekuendigt" | "..." | "Bis X bleibt aktiv"-Hinweis? |
| Invoice (Stripe) | DE/EN | "Deine Rechnung" | "..." | MwSt-konform? |
| Newsletter-Welcome (falls Newsletter) | DE/EN | "Willkommen!" | "..." | Abmelde-Link enthalten? |
| Newsletter-Double-Opt-In-Bestaetigung | DE/EN | "Bitte bestaetige deine Anmeldung" | "..." | Klares Opt-In? |
```

### Audit-Pflichten

- **UWG §7**: Werbe-E-Mails brauchen Abmelde-Link in JEDER Email
- **DSGVO Art. 17**: Account-Loeschungs-Bestaetigung als Audit-Trail nuetzlich (nicht Pflicht aber empfohlen)
- **Pflichtangaben in Trial-End-Warnung**: Preis, Datum, Cancel-Anleitung
- Absender-Adresse muss auf Domain der App matchen (Spam-Score, Impressum-Pflicht)
- Email-Texte sind oft englisch hardcoded — Lokalisierung pruefen

## 4e.5 WebView-Inhalte (HTML in assets/ oder extern)

### Such-Patterns

```bash
# WebView-Komponenten im Code
grep -rln 'WebView\|loadUrl\|loadData\|loadDataWithBaseURL' --include='*.kt' --include='*.java' .

# Compose WebView
grep -rln 'AndroidView.*WebView\|com\.google\.accompanist\.web' --include='*.kt' .

# Asset-HTML-Dateien
find . -path '*/assets/*.html' -not -path '*/build/*'
find . -path '*/assets/*' -name '*.html' -o -name '*.htm' 2>/dev/null

# Asset-Markdown-Dateien (manchmal fuer Onboarding)
find . -path '*/assets/*.md' -not -path '*/build/*'
```

### Pflicht-Output

```markdown
### 4e.5 WebView-Inhalte

WebView-Komponenten im Code: <N>
Asset-HTML-Dateien: <N>
Asset-Markdown-Dateien: <N>

#### Lokale HTML-Inhalte (assets/)

| Datei | Sprachvariante | Zweck | Eingebettete Wortlaute (1:1) |
|-------|---------------|-------|----------------------------|
| `assets/help.html` (Beispiel) | DE | Help-Center In-App | (Pflicht: kompletten HTML-Inhalt zitieren oder Inline-Texte extrahieren) |
| `assets/<beliebiger-name>.md` (Beispiel) | DE | Onboarding-Schritt 3 | (FIX X11: Platzhalter — der echte Dateiname haengt von der App ab) |

#### Externe URLs

| URL | Geladen in | Zweck | Audit |
|-----|------------|-------|-------|
| `https://help.bestjournal.app` | Help-WebView | Help-Center | Erreichbar? UWG-Pruefung? |
| `https://bestjournal.app/community` | Community-WebView | Community-Plattform | Externes Audit? |
```

### Audit-Pflichten

- HTML in `assets/` ist NICHT in strings.xml — wird oft bei i18n vergessen
- WebView mit externem URL: Cookie-Banner-Pflicht fuer EU-Nutzer (TDDDG)
- WebView ohne `setJavaScriptEnabled(false)` = Sicherheitsrisiko
- Bei Markdown-Inhalten in Assets: Inhalte gegen Werbeaussagen-Liste pruefen

## 4e.6 PDF-Export-Vorlagen

### Such-Patterns

```bash
# PDF-Generierung
grep -rln 'PdfDocument\|iText\|itextpdf\|com\.itextpdf\|PrintAttributes\|PrintDocumentAdapter' --include='*.kt' --include='*.java' .

# Custom-PDF-Vorlagen (oft hardcoded)
grep -rn 'PdfDocument\.PageInfo\|drawText\|drawString' --include='*.kt' --include='*.java' . | head -10
```

### Pflicht-Output

```markdown
### 4e.6 PDF-Export-Vorlagen

PDF-Generierung verwendet: PdfDocument / iText / Keine

| Export-Typ | Datei (Source) | Sprache | Texte im PDF (1:1) | Audit |
|-----------|---------------|---------|---------------------|-------|
| Tagebuch-Export (Eintraege) | `PdfExporter.kt:42` | DE-hardcoded | "Tagebuch-Eintraege", "Erstellt am %s", "Seite %d / %d" | i18n-Luecke |
| Subscription-Rechnung | `InvoiceGenerator.kt:30` | DE-hardcoded | "Rechnung Nr. %s", "MwSt 19%", "Betrag: %s" | MwSt-konform? |
| Backup-Manifest | `BackupGenerator.kt:55` | EN-hardcoded | "Backup created on %s" | Internationalisierung? |
```

### Audit-Pflichten

- PDF-Inhalte sind fast IMMER hardcoded und werden bei i18n vergessen
- Bei Rechnungen: MwSt-Pflichtangaben, Anschrift, Steuernummer
- Bei Backups: Datenschutz-Hinweis ueber persoenliche Daten

## 4e.7 Customer-Support-System

### Such-Patterns

```bash
# Intercom, Zendesk, Helpshift, Crisp
grep -rn 'Intercom\|Zendesk\|Helpshift\|Crisp\|Tawk' --include='*.kt' --include='*.java' --include='*.gradle*' . | head -10
```

### Pflicht-Output

```markdown
### 4e.7 Customer-Support-System

Externes System: Intercom / Zendesk / Helpshift / Email-only / Keines

Frank-Aufgabe: Aus dem System manuell zitieren:

| Slot | Wortlaut (1:1) | Sprache | Audit |
|------|----------------|---------|-------|
| Auto-Reply nach erster Nachricht | "Danke fuer deine Nachricht. Wir antworten innerhalb 24h..." | DE/EN | Realitaetscheck Wartezeit? |
| FAQ-Top-5 (am haeufigsten gefragte Themen) | (Frank kopiert aus Intercom) | | UWG-Pruefung |
| Help-Center-Artikel-Titel | | | i18n? |
```

## 4e.8 Marketing-Materialien (Frank-Aufgabe)

```markdown
### 4e.8 Marketing-Materialien

Frank-Aufgabe: Alle externen Marketing-Texte sammeln, die NICHT in der App leben:

| Material | Sprache | Wortlaut (1:1) | Wo verwendet | Audit |
|----------|---------|----------------|-------------|-------|
| Promo-Video Titel | DE/EN | (eingeben) | YouTube, App-Store | UWG |
| Promo-Video Beschreibung | DE/EN | (eingeben) | YouTube, App-Store | UWG |
| Promo-Video eingeblendete Texte | DE/EN | (eingeben) | YouTube | UWG, Untertitel? |
| Landing-Page-Hero-Text | DE/EN | (eingeben) | bestjournal.app | UWG, Cookie-Banner? |
| Landing-Page-CTA-Buttons | DE/EN | (eingeben) | bestjournal.app | BGB §312j? |
| Blog-Posts (Top 5 relevante) | DE | (Links + Auszuege) | Blog | UWG, Affiliate-Markierungen? |
| Press-Releases (falls vorhanden) | DE | (eingeben) | Presse-Bereich | Behauptbarkeit? |
| Social-Media-Bio (Twitter, Instagram, TikTok) | DE/EN | (eingeben) | Bio-Texte | UWG-Werbeaussagen |
| Newsletter-Archiv (letzte 5) | DE | (Links + Subject-Lines) | Mailchimp/Sendgrid | UWG |
```

## 4e.9 Vollstaendigkeits-Statistik

| Metrik | Wert | Quelle |
|--------|------|--------|
| Play-Store-Titel pro Sprache | N | Frank manuell |
| Short-Description pro Sprache | N | Frank manuell |
| Long-Description pro Sprache | N | Frank manuell |
| Screenshots mit Text | N | Frank manuell |
| Remote-Config-Keys im Code | N | Automatisch |
| Remote-Config Default-Werte | N | Automatisch |
| Remote-Config Live-Werte erfasst | N | Frank manuell |
| Cloud-Functions-Notification-Templates | N | Automatisch (falls Functions-Code im Repo) |
| Email-Templates erfasst | N | Frank manuell |
| Asset-HTML-Dateien | N | Automatisch |
| Asset-Markdown-Dateien | N | Automatisch |
| Externe WebView-URLs | N | Automatisch |
| PDF-Vorlagen mit hardcoded Texten | N | Automatisch |
| Customer-Support-System | (Name oder "Keines") | Automatisch |
| Marketing-Materialien dokumentiert | N | Frank manuell |

## 4e.10 Typische Fehlerquellen

- **Play-Store-Long-Description nicht synchron mit App**: Werbeaussagen aus der Description sind nicht in der App umgesetzt → UWG §5
- **Remote-Config-Live-Wert ueberschreibt Pflichthinweise**: Marketing-Team setzt aggressive Live-Werte ohne Auto-Renewal-Hinweis
- **Cloud-Functions auf Englisch fuer alle Nutzer**: deutsche User bekommen englische Push-Notifications
- **Email-Templates in Firebase Auth Default-Sprache**: bei Sprachwechsel wird Email immer in Default-Sprache geschickt
- **Asset-HTML nicht in strings.xml**: Help-Center-Inhalte fehlen bei Uebersetzung
- **PDF-Export DE-hardcoded**: EN-Nutzer bekommen deutsche Rechnungen
- **Marketing-Material auf Webseite ist juenger als das Audit**: Wortlaute aendern sich oft schneller als Frank den Skill triggert

## 4e.11 Was diese Schicht NICHT kann

- **Sie sieht nicht in fremde Webseiten** — Frank muss Inhalte manuell zitieren
- Sie kann NICHT live in Firebase Console schauen — Defaults aus dem Repo, Live-Werte braucht Frank
- Sie kann NICHT in Stripe-Dashboard schauen — Email-Templates braucht Frank
- Sie kann NICHT Promo-Videos auswerten — Frank muss Untertitel/eingeblendete Texte zitieren

## 4e.12 Frank-Aufgaben-Checkliste (am Ende von Schicht 4e)

```markdown
### Frank-Aufgaben fuer vollstaendigen Audit-Bericht

- [ ] Google Play Console → Store-Eintrag → alle Sprachen → Title, Short, Long, Screenshots-Texte kopieren
- [ ] Firebase Console → Remote Config → alle Live-Werte fuer Schluessel <Liste> kopieren
- [ ] Firebase Console → Authentication → Email-Templates → alle Sprachen → Subject + Body kopieren
- [ ] Stripe Customer Portal (falls verwendet) → Email-Templates kopieren
- [ ] Intercom / Zendesk → Auto-Replies + Top-FAQ kopieren
- [ ] Webseite → Hero, CTAs, Footer-Texte kopieren
- [ ] Social-Media-Bios (Twitter, Instagram, TikTok) kopieren
- [ ] Letzten Newsletter-Issues (5 Subject-Lines + 1 vollstaendigen Body) kopieren
- [ ] Promo-Videos: Untertitel + eingeblendete Texte transkribieren
- [ ] Press-Releases (falls vorhanden) sammeln
```
