# Datenschutzerklärung für Best Journal

**Stand:** 20. April 2026
**App:** Best Journal (Android)
**Entwickler:** Frank Barwandt

---

## 1. Verantwortlicher im Sinne der DSGVO

Verantwortlich für die Verarbeitung personenbezogener Daten in der App „Best Journal"
ist:

**Frank Barwandt**
c/o Impressumservice Dein-Impressum
Stettiner Straße 41
35410 Hungen
Deutschland

E-Mail: dev.app.support@gmail.com

Ein Datenschutzbeauftragter ist gesetzlich nicht erforderlich (Art. 37 DSGVO, § 38 BDSG).

---

## 2. Überblick: Welche Daten verarbeitet die App?

Best Journal ist eine Tagebuch-App. Der Grundzustand ist **lokal auf deinem Gerät**,
Tagebucheinträge werden in einer geschützten SQLite-Datenbank gespeichert und verlassen
dein Gerät nur, wenn du aktiv eine der folgenden optionalen Funktionen nutzt:

- Cloud-Transkription (Groq) von Sprachaufnahmen
- Cloud-Backup (Google Drive)
- KI-Funktionen (Firebase AI / Google Gemini)
- Vorlesefunktion (Microsoft Edge TTS)
- Anmeldung (Google Sign-In / Firebase Authentication)
- Nutzungsstatistiken (Firebase Analytics, nur mit Einwilligung)

Die App verarbeitet folgende Datenkategorien:

| Datenkategorie | Wo gespeichert | Zweck | Optional? |
|----------------|----------------|-------|-----------|
| Tagebucheinträge (Text, Audio, Bilder) | Lokal auf deinem Gerät | Kernfunktion | Kern |
| Einstellungen und Präferenzen | Lokal auf deinem Gerät | App-Konfiguration | Kern |
| Sprachaufnahmen (Cloud-Transkription) | Groq, Inc. (USA) | Umwandlung Sprache → Text | **Optional** |
| Tagebuchdaten (Backup) | Google Drive (App-Data-Ordner) | Wiederherstellung, Geräteübergang | **Optional** |
| Textausschnitte (Vorlesen) | Microsoft Bing Speech (USA) | Umwandlung Text → Sprache | **Optional** |
| KI-Anfragen (manuell + automatisch) | Firebase AI / Google Gemini (USA) | Dashboard, Wochen-/Monats-/Jahresrückblicke | **Optional** |
| E-Mail, Anmelde-ID | Google / Firebase Authentication | Account | **Optional** |
| Geräteinfo, IP-Adresse, Werbe-ID | Firebase Analytics | Nutzungsstatistik | **Opt-In** |
| Kaufdaten | Google Play Billing | In-App-Käufe | Nur bei Kauf |

---

## 3. Berechtigungen der App und ihre Verwendung

Die App fordert folgende Android-Berechtigungen an. Jede Berechtigung wird **nur für die
angegebene Funktion** verwendet und kann in den Android-Systemeinstellungen jederzeit
widerrufen werden.

### 3.1 Internet (`INTERNET`) und Netzwerkstatus (`ACCESS_NETWORK_STATE`)
**Zweck:** Cloud-Transkription (Groq), Cloud-Backup (Google Drive), KI-Funktionen,
In-App-Käufe, Firebase-Dienste.
**Hinweis:** Ohne Internet funktioniert die App weiterhin, nur Cloud- und
Online-KI-Funktionen sind dann deaktiviert. Die lokale Spracherkennung (siehe 5.2)
funktioniert auch offline.

### 3.2 Mikrofon (`RECORD_AUDIO`)
**Zweck:** Sprachaufnahmen für Tagebucheinträge (Diktierfunktion, Sprachnotizen).
**Verarbeitung:** Aufnahmen werden standardmäßig **lokal** auf deinem Gerät
gespeichert. Zur Transkription hast du die Wahl zwischen **lokaler** Erkennung
(siehe 5.2) und **Cloud-Transkription** (siehe 5.1).
**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO).

### 3.3 Kamera (`CAMERA`)
**Zweck:** Foto- und Video-Anhänge zu Tagebucheinträgen.
**Verarbeitung:** Fotos und Videos werden ausschließlich lokal gespeichert (App-privater
Ordner `filesDir/photos/`) und nicht automatisch hochgeladen. Bei aktivem
Google-Drive-Backup werden sie als Teil des Backups verschlüsselt in den
Drive-App-Data-Ordner übertragen.

### 3.4 Ungefährer Standort (`ACCESS_COARSE_LOCATION`)
**Zweck:** Ausschließlich für das optionale Design-Feature **„Sonnenauf-/-untergang"**
in den Einstellungen (Dunkelmodus passt sich automatisch an den lokalen
Sonnenstand an).
**Verarbeitung:** Der Standort wird **einmalig** beim Aktivieren des Schalters
ermittelt, lokal in den App-Einstellungen gespeichert und **niemals** an externe
Dienste übermittelt. Sonnenauf- und -untergangszeiten werden rein rechnerisch auf
deinem Gerät bestimmt.

### 3.5 Benachrichtigungen (`POST_NOTIFICATIONS`)
**Zweck:** Erinnerungen an das Schreiben von Einträgen (wenn du Reminder aktivierst).
**Verarbeitung:** Ausschließlich lokal vom Gerät erzeugt.

### 3.6 Autostart nach Neustart (`RECEIVE_BOOT_COMPLETED`)
**Zweck:** Reaktivierung geplanter Erinnerungen nach einem Geräte-Neustart.
**Verarbeitung:** Ausschließlich lokal, keine Datenübertragung.

---

## 3a. Geltungsbereich und internationale Verfügbarkeit

Die App ist in **27 Sprachen** (Deutsch, Englisch, Französisch, Spanisch, Italienisch,
Niederländisch, Polnisch, Portugiesisch [Brasilien und Portugal], Ukrainisch, Türkisch,
Arabisch, Hindi, Bengalisch, Gujarati, Kannada, Malayalam, Marathi, Tamil, Telugu, Urdu,
Indonesisch, Thailändisch, Japanisch, Koreanisch, Chinesisch [vereinfacht und traditionell])
im Google Play Store verfügbar. Die Sprache wird automatisch auf Basis deiner
Android-Systemsprache gewählt (`Locale.getDefault()`), die Zeitzone aus der
Android-Systemeinstellung (`TimeZone.getDefault()`). Beides erfolgt ausschließlich
**auf dem Gerät**, **es findet keine zusätzliche Standort-, Sprach- oder
Zeitzonen-Abfrage bei Servern statt**.

Weitere Sprachen und Länder werden fortlaufend ergänzt, damit die App in immer mehr
lokalen Play-Store-Regionen in der jeweiligen Landessprache verfügbar ist.

Diese Datenschutzerklärung gilt unabhängig von deinem Wohnsitz. Für Nutzer innerhalb
der **Europäischen Union** und des **EWR** kommt die DSGVO zur Anwendung; für Nutzer
außerhalb gelten die hier genannten Schutzstandards freiwillig als Selbstverpflichtung
des Verantwortlichen.

---

## 4. Lokale Datenspeicherung

Alle Tagebuchinhalte (Texte, Sprachaufnahmen, Bilder, Stimmungen, Tags) werden in einer
lokalen SQLite-Datenbank (Android Room) ausschließlich auf deinem Gerät gespeichert.

- **Speicherort:** Interner App-Speicher (vom Betriebssystem geschützt)
- **Zugriff:** Nur die Best-Journal-App selbst
- **Löschung:** Durch Deinstallation der App oder über die Einstellungen („Alle Daten
  löschen")

### 4.1 PDF-Export (lokal)

Die App bietet einen **PDF-Export** für Tagebucheinträge inklusive eingebetteter Fotos.
Die Umwandlung findet **vollständig auf deinem Gerät** statt. Die erzeugte PDF-Datei
wird im lokalen App- oder Download-Ordner abgelegt. Es findet **keine Datenübermittlung
an Dritte** durch den Export selbst statt.

Wenn du die PDF anschließend über das Android-Teilen-Menü an eine andere App oder
einen Cloud-Dienst sendest (z. B. E-Mail, WhatsApp, Google Drive), ist das deine
eigene Entscheidung. Der Empfänger-Dienst verarbeitet die Daten dann nach seinen
eigenen Datenschutzbestimmungen.

---

## 5. Optionale Cloud-Dienste und Drittanbieter

Die folgenden Dienste werden **nur verwendet, wenn du sie aktiv nutzt oder aktivierst**.
Die App ist auch ohne diese Dienste vollständig nutzbar.

### 5.1 Groq, Inc. | Cloud-Transkription (optional)

**Anbieter:** Groq, Inc., 400 Castro Street, Mountain View, CA 94041, USA
**Zweck:** Umwandlung deiner Sprachaufnahmen in Text (Whisper-Transkription).
**Erhobene Daten:** Die konkrete Audiodatei, die du zur Transkription hochlädst.
Metadaten: Dateigröße, Format, Sprache, IP-Adresse.
**Übermittlung:** Nur wenn du in den Einstellungen **„Cloud-Transkription"** aktiviert
hast oder explizit die Cloud-Variante bei einer Aufnahme wählst. Standard ist die
**lokale On-Device-Transkription** (siehe 5.2).
**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO).
**Drittlandübermittlung:** Die Verarbeitung erfolgt in den USA. Groq ist nach eigenen
Angaben nach dem **EU-US Data Privacy Framework** zertifiziert. Zusätzlich werden
Standardvertragsklauseln (Art. 46 DSGVO) angewendet.
**Speicherdauer bei Groq:** Anfragen werden laut Anbieter nicht zu Trainingszwecken
verwendet und nach der Verarbeitung gelöscht.
**Widerruf:** Wechsle in den App-Einstellungen auf **„Lokale Transkription"**, dann
werden keine Audiodaten mehr an Groq übermittelt.
**Datenschutzerklärung Groq:** https://groq.com/privacy-policy/

> **Wichtiger Hinweis:** Sprachaufnahmen können besonders sensible personenbezogene
> Daten enthalten (Art. 9 DSGVO). Nutze die Cloud-Transkription nur, wenn du mit der
> Übermittlung deiner Aufnahme an Groq in die USA einverstanden bist. Die lokale
> Transkription ist die datenschutzfreundlichere Alternative.

### 5.2 Lokale Spracherkennung (On-Device)

Die App enthält eine **lokale Offline-Spracherkennung** auf Basis des Open-Source-Projekts
**sherpa-onnx**. Die Transkription findet vollständig auf deinem Gerät statt.
**Datenübertragung:** Keine. Kein Server ist beteiligt.
**Voraussetzung:** Einmaliger Download des Sprachmodells (~100 MB).

### 5.2a Microsoft Edge Text-to-Speech (optional)

**Anbieter:** Microsoft Corporation, One Microsoft Way, Redmond, WA 98052, USA
**Dienst:** Bing Speech Service (Endpoint: `speech.platform.bing.com`)
**Zweck:** Umwandlung von Text in natürliche Sprachausgabe („Vorlesefunktion"), z. B.
zum Anhören deiner Einträge oder Retrospektiven.
**Ablauf der Datenübertragung:**
1. Du löst die Vorlesefunktion aus (z. B. auf dem Dashboard oder im Eintrag).
2. Der zu sprechende Text wird über eine verschlüsselte WebSocket-Verbindung an
   Microsoft-Server in den **USA** übermittelt.
3. Die erzeugte Audiodatei wird zurückgesendet und lokal auf deinem Gerät abgespielt.
**Erhobene Daten:** Der an den Dienst übermittelte Text, ausgewählte Sprachstimme,
technische Metadaten (IP-Adresse, Zeitstempel).
**Übermittlung:** Nur wenn du aktiv die Vorlesefunktion nutzt. Ohne Nutzung erfolgt
keine Datenübermittlung an Microsoft.
**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO) durch aktives Auslösen.
**Drittlandübermittlung:** USA, auf Grundlage des
**EU-US Data Privacy Framework** (Microsoft ist zertifiziert) sowie von
**EU-Standardvertragsklauseln** (Art. 46 DSGVO).
**Widerruf:** Nutze die Vorlesefunktion einfach nicht, oder deaktiviere sie in den
Einstellungen. Android bietet zusätzlich eine systemeigene On-Device-TTS als
Alternative.
**Datenschutzerklärung Microsoft:**
https://privacy.microsoft.com/de-de/privacystatement

> **Hinweis:** Übergib der Vorlesefunktion keine besonders sensiblen
> personenbezogenen Daten Dritter.

### 5.3 Google Drive | Cloud-Backup (optional)

**Anbieter:** Google Ireland Limited, Gordon House, Barrow Street, Dublin 4, Irland
**Zweck:** Verschlüsseltes Backup deiner Tagebuchdaten zur Wiederherstellung auf einem
neuen Gerät oder nach Deinstallation.
**Erhobene Daten:** Komplettes Backup deiner Tagebuchdatenbank (Einträge, Audios,
Bilder, Einstellungen) als einzelne Datei.
**Speicherort:** **App-Data-Ordner deines persönlichen Google-Drive-Kontos** (Scope:
`DRIVE_APPDATA`). Dieser Ordner ist von Google geschützt und ausschließlich für
Best Journal zugänglich, andere Apps und selbst du über die normale Drive-Oberfläche
kannst darauf nicht zugreifen.
**Aktivierung:** Nur wenn du in den Einstellungen **„Google-Drive-Backup"** aktivierst
und der Zugriffsberechtigung explizit zustimmst.
**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO).
**Widerruf:** Deaktiviere das Backup in den App-Einstellungen. Zusätzlich kannst du in
deinem Google-Konto unter **„Apps mit Kontozugriff"** die Verbindung komplett aufheben.
**Löschung des Backups:** Über **„Einstellungen → Backup → Cloud-Backup löschen"** in
der App.

#### 5.3a Android-System-Backup (automatisch)

Zusätzlich zum App-internen Drive-Backup unterstützt die App das **Android-System-
Backup** (`allowBackup="true"` im Manifest). Ist bei dir unter
**Android-Einstellungen → Google → Sicherung** die automatische Sicherung aktiviert,
werden die Haupt-Tagebuchdatenbank (`entropy_journal_db`) und normale App-Einstellungen
automatisch im Google-Drive-Speicher deines Kontos gesichert (verschlüsselt, max. 25 MB).

**Was ausgeschlossen ist:** Die Backup-Regeln (`backup_rules.xml` /
`data_extraction_rules.xml`) schließen die Dashboard-Datenbank, die Rückblick-Datenbank
und die verschlüsselten Zugangsdaten (EncryptedSharedPreferences mit Google-Tokens)
vom Android-System-Backup aus, diese sensiblen Daten landen also nicht im System-Backup.

**Was mitgesichert wird:** Tagebuchtexte, Stimmungs-Tags und normale App-Einstellungen.
Fotos und Videos werden vom Android-System-Backup wegen der Größenbeschränkung (25 MB)
in der Regel nicht erfasst, für Medien brauchst du das separate Google-Drive-Backup
aus der App (5.3).

**Deaktivierung:** In den Android-Systemeinstellungen unter „Google → Sicherung".
**Rechtsgrundlage:** Einwilligung durch Google-Kontoeinstellungen (Art. 6 Abs. 1 lit. a
DSGVO).

### 5.4 Google Sign-In (optional)

**Anbieter:** Google Ireland Limited
**Zweck:** Komfortable Anmeldung mit deinem Google-Konto (über Android Credential
Manager).
**Erhobene Daten:** E-Mail-Adresse, Google-Konto-ID, öffentlicher Name, Profilbild-URL.
**Rechtsgrundlage:** Vertragserfüllung (Art. 6 Abs. 1 lit. b DSGVO), sofern du dich
anmeldest.
**Hinweis:** Die Anmeldung ist **nicht erforderlich**, um die App zu nutzen.

### 5.5 Firebase Authentication (optional)

**Anbieter:** Google Ireland Limited
**Zweck:** Benutzerkonto-Verwaltung (verknüpft Google-Sign-In mit der App).
**Erhobene Daten:** E-Mail-Adresse, Anmelde-ID, IP-Adresse, Zeitstempel.
**Rechtsgrundlage:** Vertragserfüllung (Art. 6 Abs. 1 lit. b DSGVO).

### 5.6 Firebase AI / Google Gemini, Manuelle und automatische KI-Verarbeitung

**Anbieter:** Google Ireland Limited / Google LLC
**KI-Modell:** Google Gemini (Firebase AI Logic SDK)
**Serverstandort:** USA

Die App nutzt Google Gemini sowohl für **manuelle** als auch für **automatische**
KI-Funktionen. Beide führen zu einer Übermittlung von Textausschnitten an
Google-Server in den USA.

#### 5.6.1 Manuell ausgelöste KI-Funktionen

Wenn du aktiv eine KI-Funktion startest (z. B. „Text verbessern", „Zusammenfassen",
„Rückfrage"):
1. Der relevante Textausschnitt wird an Google Gemini in den **USA** übermittelt.
2. Das Ergebnis wird an dein Gerät zurückgesendet und dir zur Übernahme angezeigt.

#### 5.6.2 Automatisch ausgelöste KI-Funktionen

Die App erzeugt **automatisch** bestimmte KI-generierte Inhalte, sobald bestimmte
Ereignisse eintreten. Dabei werden deine relevanten Tagebuchdaten **automatisch** an
Google Gemini in den USA gesendet:

| Trigger | Was passiert |
|---------|-------------|
| **Neuer Tagebucheintrag** | Dashboard-Aktualisierung, die relevanten Einträge werden automatisch an Gemini gesendet, um eine neue Dashboard-Zusammenfassung zu erzeugen (nur wenn unter „Einstellungen → KI-Automatisierungen → Auto-Dashboard-Update" aktiviert) |
| **Neue Sprach-Transkription** (bei aktivierter Auto-Textverbesserung) | Der transkribierte Text wird automatisch durch Gemini stilistisch/grammatikalisch verbessert |
| **Profil-Wechsel im Dashboard** | Dashboard wird neu generiert mit dem neuen Profil-Prompt |

**Nicht automatisch, nur auf deinen Anstoß:**
- **Wöchentliche, monatliche und jährliche Rückblicke** werden *nicht* automatisch im
  Hintergrund erzeugt. Am Ende der jeweiligen Periode bekommst du eine lokale
  Benachrichtigung. Der Rückblick selbst wird erst generiert, wenn du die App öffnest
  und die Rückblick-Ansicht aufrufst. Ohne dein Handeln findet keine Übermittlung statt.

**Erhobene Daten:** Textausschnitte deiner Tagebucheinträge (nie Fotos, nie
Audioaufnahmen), Zeitraum, Modellparameter, technische Metadaten (IP-Adresse,
Zeitstempel).

**Deaktivierung der automatischen KI-Funktionen:**
Unter **„Einstellungen → KI-Automatisierungen"** kannst du einzeln deaktivieren:
- Automatische Dashboard-Aktualisierung (Standard: aktiv)
- Automatische Textverbesserung nach Transkription (Standard: aus)

Unter **„Einstellungen → Erinnerungen"** steuerst du, ob du für Wochen-/Monats-/
Jahresrückblicke eine Benachrichtigung bekommst (Standard: aktiv). Unabhängig davon
wird der Rückblick erst beim Öffnen durch dich erzeugt.

Nach dem Deaktivieren findet für die jeweilige Funktion **keine Übermittlung an
Google Gemini mehr statt**. Die App bleibt voll nutzbar, du verzichtest nur auf die
KI-generierten Zusammenfassungen und Rückblicke.

**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO). Die Einwilligung
wird beim ersten App-Start mit einem deutlich erkennbaren Hinweis eingeholt und
umfasst sowohl manuelle als auch automatische KI-Verarbeitung. Sie kann jederzeit
in den Einstellungen widerrufen werden (Art. 7 Abs. 3 DSGVO).

**Drittlandübermittlung:** Verarbeitung erfolgt in den USA auf Grundlage des
**EU-US Data Privacy Framework** (Angemessenheitsbeschluss der EU-Kommission) sowie
von **EU-Standardvertragsklauseln** (Art. 46 DSGVO).

**Speicherdauer bei Google:** Laut Firebase-AI-Richtlinien werden Anfragen nicht zu
Trainingszwecken verwendet und nach der Verarbeitung gelöscht. Details:
https://firebase.google.com/support/privacy

> **Wichtig:** Sende über manuelle KI-Funktionen keine besonders sensiblen
> personenbezogenen Daten Dritter (z. B. Gesundheitsdaten anderer Personen, Namen
> dritter Personen ohne deren Einwilligung). Auch für die automatischen Rückblicke
> gilt: Schreibe keine Inhalte ins Tagebuch, die du nicht an Google Gemini
> übermitteln willst, oder deaktiviere die automatischen KI-Funktionen.

### 5.7 Firebase Analytics (Opt-In beim ersten Start)

**Anbieter:** Google Ireland Limited / Google LLC
**Zweck:** Anonyme Nutzungsstatistiken zur Fehleranalyse und Produktverbesserung.
**Erhobene Daten:** Gerätetyp, Betriebssystemversion, App-Version, Nutzungshäufigkeit,
ungefähre Region (Land), Firebase Instance ID, **IP-Adresse (gekürzt)**,
**Android Werbe-ID (AAID)**, Ereignisdaten (z. B. Eintrag erstellt, Dashboard geöffnet,
Premium-Kauf, Fehler in der Sprachausgabe).

**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO, § 25 Abs. 1 TDDDG).

**Wie die Einwilligung funktioniert:**
- Beim **ersten App-Start** erscheint vor dem Onboarding ein Datenschutz-Bildschirm.
- Wählst du „Loslegen", willigst du in die anonyme Nutzungsstatistik ein.
- Wählst du „Statistiken deaktivieren", bleibt Firebase Analytics ausgeschaltet,
  technisch via `setAnalyticsCollectionEnabled(false)`.
- Du kannst deine Entscheidung jederzeit unter **„Einstellungen → Datenschutz →
  Anonyme Statistik"** umschalten.

**Werbe-ID zurücksetzen:** In den Android-Systemeinstellungen unter
**„Einstellungen → Datenschutz → Werbung"**.

### 5.8 Firebase App Check (Play Integrity)

**Zweck:** Schutz vor Missbrauch und automatisierten Anfragen.
**Erhobene Daten:** Geräteintegritäts-Token von Google Play, App-Signatur.
**Rechtsgrundlage:** Berechtigtes Interesse an Missbrauchsschutz (Art. 6 Abs. 1 lit. f
DSGVO).
**Abwägung:** Das berechtigte Interesse am Schutz der Firebase-Endpunkte vor
automatisierten Angriffen überwiegt die minimale Beeinträchtigung des Nutzers, da
das Integritäts-Token geräteunabhängig ist und keine persönliche Identifikation
ermöglicht. Eine datenschutzschonendere Alternative ist technisch nicht verfügbar.
**Drittlandübermittlung:** Verarbeitung in den USA auf Grundlage des EU-US Data
Privacy Framework und Standardvertragsklauseln (Art. 46 DSGVO).

### 5.9 Firebase Remote Config

**Zweck:** Ferngesteuerte Konfiguration (z. B. Feature-Flags, Texte).
**Erhobene Daten:** Anonymisierte App-Instanz-ID, App-Version.
**Rechtsgrundlage:** Berechtigtes Interesse am ordnungsgemäßen Betrieb (Art. 6 Abs. 1
lit. f DSGVO).
**Abwägung:** Das berechtigte Interesse an reibungsloser App-Konfiguration überwiegt,
da nur anonymisierte Instanz-IDs übertragen werden und keine Rückverfolgung auf
Einzelpersonen möglich ist.
**Drittlandübermittlung:** Verarbeitung in den USA auf Grundlage des EU-US Data
Privacy Framework und Standardvertragsklauseln (Art. 46 DSGVO).

### 5.9a Feedback-Funktion (Gmail-API, optional)

Unter **„Einstellungen → Feedback senden"** kannst du uns deine Rückmeldung zur App
schicken.

**Technischer Ablauf:**
1. Du gibst deinen Feedback-Text in der App ein und bestätigst das Senden.
2. Die App bittet dich einmalig um die Google-Berechtigung **„E-Mails senden"**
   (Gmail-API-Scope `https://www.googleapis.com/auth/gmail.send`).
3. Nach deiner Zustimmung werden **zwei E-Mails über dein eigenes Google-Konto**
   via Gmail-API versendet:
   - **E-Mail 1 an uns:** `dev.app.support@gmail.com`, enthält deinen Feedback-Text
     und deine Google-Konto-E-Mail-Adresse als Absender.
   - **E-Mail 2 an dich selbst:** Bestätigung mit dem Feedback-Text, den du
     abgeschickt hast, damit du eine Kopie für dich hast.

**Wer verarbeitet was:**
- **Google** (Gmail-Infrastruktur): transportiert beide E-Mails über seine
  Mailserver.
- **Wir** (Frank Barwandt, Kontaktadresse siehe Abschnitt 1): empfangen die erste
  E-Mail in unserem `dev.app.support@gmail.com`-Postfach und speichern sie zur
  Bearbeitung deiner Rückmeldung.

**Erhobene/übermittelte Daten:**
- Deine Google-Konto-E-Mail-Adresse (als Absender beider E-Mails)
- Der von dir eingegebene Feedback-Text
- Zeitstempel der Versendung

**Rechtsgrundlage:**
- Für die Übermittlung an uns: Einwilligung (Art. 6 Abs. 1 lit. a DSGVO) durch
  aktives Versenden des Feedbacks.
- Für die Bearbeitung deiner Anfrage: Berechtigtes Interesse an der Verbesserung
  der App (Art. 6 Abs. 1 lit. f DSGVO).

**Speicherdauer bei uns:** Deine Feedback-E-Mail bleibt maximal 24 Monate in unserem
Postfach gespeichert, danach wird sie gelöscht. Bei konkretem Bezug zu einem
Bugfix oder Feature kann die Speicherung auch länger erfolgen, du kannst jederzeit
per E-Mail Löschung verlangen.

**Widerruf der Gmail-Berechtigung:** Du kannst den Gmail-Send-Zugriff jederzeit
aufheben unter **https://myaccount.google.com/permissions** → Best Journal entfernen.
Nach Entzug funktioniert die Feedback-Funktion nicht mehr, die App ist sonst
uneingeschränkt nutzbar.

**Alternative:** Du kannst uns auch direkt per E-Mail erreichen unter
**dev.app.support@gmail.com**, ganz ohne App-Berechtigungen.

### 5.10 Google Play In-App Review API

**Zweck:** Anzeige des Bewertungsdialogs im Google Play Store.
**Erhobene Daten:** Technische Metadaten (App-Version, Paketname) zur Anzeige des
Dialogs. Keine Erfassung der Bewertung selbst durch die App.
**Rechtsgrundlage:** Berechtigtes Interesse (Art. 6 Abs. 1 lit. f DSGVO).
**Abwägung:** Das berechtigte Interesse an Nutzerfeedback überwiegt, da nur
technische Metadaten ohne Personenbezug verarbeitet werden und die eigentliche
Bewertung ausschließlich in deinem Google-Play-Konto erfolgt.
**Drittlandübermittlung:** Verarbeitung in den USA auf Grundlage des EU-US Data
Privacy Framework und Standardvertragsklauseln (Art. 46 DSGVO).

**Weitere Informationen zu Google-Diensten:**
- Google-Datenschutzerklärung: https://policies.google.com/privacy
- Firebase-Datenschutz: https://firebase.google.com/support/privacy
- Google Drive: https://policies.google.com/privacy#infocollect

### 5.11 Übermittlung in Drittländer (USA)

Bei aktivierten Cloud-Diensten (Groq, Firebase/Gemini, Google Drive, Microsoft
Edge TTS) werden Daten in den USA verarbeitet. Die Übermittlung erfolgt auf Grundlage:

- **EU-US Data Privacy Framework** (Angemessenheitsbeschluss der EU-Kommission vom
  10. Juli 2023), für Google/Firebase/Gemini und Microsoft
- **EU-Standardvertragsklauseln** (Art. 46 DSGVO), für Groq

---

## 6. In-App-Käufe (Google Play Billing)

Für optionale Premium-Funktionen nutzt die App die Zahlungsabwicklung **Google Play
Billing**.

- **Anbieter:** Google Ireland Limited
- **Erhobene Daten:** Transaktions-ID, Kauf-Token, gekauftes Produkt, Zeitstempel
- **Zahlungsdaten:** Werden **ausschließlich von Google** verarbeitet, wir erhalten
  keine Kreditkartennummern, PayPal-Zugänge oder Kontodaten
- **Rechtsgrundlage:** Vertragserfüllung (Art. 6 Abs. 1 lit. b DSGVO)
- **Datenschutzerklärung Google Play:** https://play.google.com/about/play-terms/

---

## 7. Kontolöschung und Datenlöschung

Nach Art. 17 DSGVO und den Richtlinien von Google Play hast du jederzeit das Recht,
deine Daten löschen zu lassen.

### 7.1 Konto und Cloud-Daten löschen (in der App)
**Einstellungen → Konto → Konto löschen**

Beim Löschen werden unwiderruflich entfernt:
- Lokale Tagebuchdatenbanken (Haupt-DB, Dashboard-DB, Rückblick-DB)
- Lokale Fotos und Videos der Einträge
- Verschlüsselte App-Einstellungen und gespeicherte Google-Tokens
- Firebase-Authentifizierungs-Konto (`FirebaseAuth.currentUser.delete()`)
- Google-Drive-App-Data-Backup (wird aus Drive entfernt)

Nach Abschluss startet die App neu und verhält sich wie eine frische Installation.
Das Android-System-Backup wird durch diesen Vorgang **nicht** gelöscht, dieses kannst
du separat in deinem Google-Konto unter
**myaccount.google.com → Daten und Datenschutz → Sicherungen** entfernen.

### 7.2 Lokale Tagebuchdaten löschen
**Einstellungen → Daten → Alle Daten löschen** oder **App deinstallieren**

### 7.3 Drive-Backup manuell löschen
Falls das Backup erhalten bleiben soll auch nach App-Deinstallation, kannst du es
separat unter **„Einstellungen → Backup → Cloud-Backup löschen"** entfernen. Nach
App-Deinstallation kannst du den App-Data-Ordner in deinem Google-Konto unter
**myaccount.google.com → Daten und Datenschutz → Apps mit Kontozugriff** löschen.

### 7.4 Löschung per E-Mail anfordern
Falls du keinen Zugriff mehr auf die App hast: **dev.app.support@gmail.com**,
Betreff: „Kontolöschung Best Journal". Bearbeitungsfrist: 30 Tage.

---

## 8. Deine Rechte nach DSGVO

Du hast jederzeit folgende Rechte:

| Recht | Artikel | Wie ausüben |
|-------|---------|-------------|
| Auskunft | Art. 15 DSGVO | E-Mail an dev.app.support@gmail.com |
| Berichtigung | Art. 16 DSGVO | E-Mail oder direkt in der App |
| Löschung („Recht auf Vergessen") | Art. 17 DSGVO | In der App unter „Daten löschen" oder App deinstallieren |
| Einschränkung der Verarbeitung | Art. 18 DSGVO | E-Mail an dev.app.support@gmail.com |
| Datenübertragbarkeit | Art. 20 DSGVO | Export-Funktion in der App oder E-Mail |
| Widerspruch | Art. 21 DSGVO | E-Mail an dev.app.support@gmail.com |
| Widerruf der Einwilligung | Art. 7 Abs. 3 DSGVO | In den App-Einstellungen unter „Datenschutz" |
| Beschwerde bei Aufsichtsbehörde | Art. 77 DSGVO | Siehe unten |

### Zuständige Aufsichtsbehörden

**Deutschland / EU / EWR (Haupt-Aufsichtsbehörde):**

Der Hessische Beauftragte für Datenschutz und Informationsfreiheit
Gustav-Stresemann-Ring 1, 65189 Wiesbaden
Telefon: +49 611 1408-0
E-Mail: poststelle@datenschutz.hessen.de
Website: https://datenschutz.hessen.de

(Zuständig, da Sitz des Verantwortlichen in Hessen.)

EU-/EWR-Nutzer können sich auch an die für ihren Wohnsitz zuständige Aufsichtsbehörde
wenden.

---

## 8a. Internationale Nutzer, zusätzliche Rechte nach lokalem Recht

Best Journal wird weltweit über den Google Play Store angeboten. Die oben genannten
Betroffenenrechte nach DSGVO gelten für Nutzer in allen Ländern. Für Nutzer aus
bestimmten Jurisdiktionen gelten **zusätzliche** oder **abweichende** Rechte nach
lokalem Recht:

### Kalifornien, USA (CCPA / CPRA)

Nutzer mit Wohnsitz in Kalifornien haben nach dem California Consumer Privacy Act
(CCPA) in der Fassung des California Privacy Rights Act (CPRA) folgende zusätzliche
Rechte:

- **Right to Know:** Welche personenbezogenen Daten wir erhoben, verwendet und geteilt haben
- **Right to Delete:** Löschung deiner Daten
- **Right to Correct:** Berichtigung unrichtiger Daten
- **Right to Opt-Out of Sale/Sharing:** Wir verkaufen und teilen keine personenbezogenen
  Daten im Sinne des CCPA. Es findet insoweit kein Opt-Out statt, weil kein Verkauf erfolgt.
- **Right to Limit Use of Sensitive Personal Information:** Einschränkung der Verarbeitung
  sensibler Daten (z. B. Inhalte deiner Tagebucheinträge)
- **Right to Non-Discrimination:** Keine Benachteiligung bei Ausübung der Rechte

Ausübung per E-Mail an dev.app.support@gmail.com mit Betreff „CCPA Request".

### Weitere US-Bundesstaaten (VCDPA, CPA, CTDPA, UCPA, OCPA, MCDPA, IACDPA, TDPSA, ...)

Nutzer aus Virginia, Colorado, Connecticut, Utah, Oregon, Montana, Iowa, Texas und
weiteren US-Bundesstaaten mit eigenem Datenschutzgesetz haben vergleichbare Rechte
(Auskunft, Löschung, Berichtigung, Opt-Out von Targeted Advertising). Diese Rechte
werden in gleicher Weise wie unter CCPA respektiert.

### Brasilien (LGPD)

Nutzer in Brasilien haben nach der Lei Geral de Proteção de Dados (LGPD, Lei Nr.
13.709/2018) Rechte auf Auskunft, Berichtigung, Anonymisierung, Sperrung oder
Löschung, Datenportabilität und Widerspruch. Diese werden entsprechend der DSGVO-Rechte
behandelt. Aufsichtsbehörde: ANPD (Autoridade Nacional de Proteção de Dados).

### Kanada (PIPEDA)

Nutzer in Kanada haben nach dem Personal Information Protection and Electronic
Documents Act (PIPEDA) Rechte auf Zugang und Korrektur. Beschwerdestelle: Office of
the Privacy Commissioner of Canada (OPC), https://www.priv.gc.ca.

### Australien (Australian Privacy Principles)

Nutzer in Australien haben nach dem Privacy Act 1988 und den Australian Privacy
Principles (APPs) Rechte auf Zugang, Berichtigung und Beschwerde bei der Office of
the Australian Information Commissioner (OAIC), https://www.oaic.gov.au.

### Andere Jurisdiktionen

Für Nutzer aus Ländern ohne speziell benannten Abschnitt gelten die oben
aufgeführten DSGVO-konformen Rechte als freiwillige Selbstverpflichtung, sowie alle
zwingenden Rechte nach dem jeweiligen lokalen Recht.

Bei Unklarheiten oder Wünschen zur Ausübung lokaler Rechte schreibe bitte an
dev.app.support@gmail.com.

---

---

## 9. Datensicherheit

- Lokale Datenbank im geschützten App-Speicherbereich (durch Android-Sandboxing)
- Verschlüsselte Übertragung (HTTPS/TLS 1.2+) bei allen Netzwerkverbindungen
- Google Drive Backup nur im geschützten App-Data-Ordner (kein Zugriff durch andere Apps)
- Firebase App Check zum Schutz vor Missbrauch
- Lokale Verschlüsselung sensibler Daten (AndroidX Security Crypto)
- Keine Speicherung unverschlüsselter Zugangsdaten
- Automatische Sicherheitsupdates über den Google Play Store

### 9.1 Biometrische App-Sperre (optional)

Du kannst die App optional durch eine **biometrische Authentifizierung** (Fingerabdruck,
Gesichtserkennung) oder alternativ durch einen **Geräte-PIN** schützen
(AndroidX Biometric Library).

**Wichtig:** Biometrische Daten (z. B. Fingerabdruck-Muster oder Gesichtsmerkmale)
werden **ausschließlich vom Android-Betriebssystem in der gesicherten Hardware-Enklave
deines Geräts** (Trusted Execution Environment / Secure Element) verarbeitet. Sie
verlassen dein Gerät **niemals** und werden der App **nicht zugänglich** gemacht. Die
App erhält vom System lediglich die Information „Authentifizierung erfolgreich" oder
„Authentifizierung fehlgeschlagen", keine biometrischen Merkmale selbst.

**Rechtsgrundlage:** Einwilligung durch Aktivierung der Sperre (Art. 6 Abs. 1 lit. a
DSGVO).
**Widerruf:** Deaktiviere die Sperre in den App-Einstellungen oder entferne deine
Biometrie aus den Android-Systemeinstellungen.

---

## 10. Kinder und Jugendliche

Die App richtet sich an Nutzer **ab 13 Jahren**. Für Kinder unter 16 Jahren ist eine
Einwilligung der Erziehungsberechtigten erforderlich (Art. 8 DSGVO), sofern eine
Anmeldung erfolgt oder Cloud-Dienste (Groq, Drive, KI) genutzt werden. Die App erhebt
wissentlich keine Daten von Kindern unter 13 Jahren.

---

## 11. Speicherdauer

| Daten | Dauer |
|-------|-------|
| Lokale Tagebucheinträge | Bis zur Löschung durch dich oder Deinstallation der App |
| Google-Drive-Backup | Bis zur Löschung durch dich oder Widerruf der Drive-Berechtigung |
| Android-System-Backup | Nach Google-Richtlinien (typisch bis zur Deaktivierung der Systemsicherung) |
| Microsoft Edge TTS | Nach Verarbeitung gelöscht (laut Microsoft-Richtlinien) |
| Groq-Transkriptionsanfragen | Nach Verarbeitung gelöscht (laut Groq-Richtlinien) |
| Firebase Analytics | 14 Monate (Google-Standard), danach automatische Löschung |
| Firebase Authentication | Bis zur Löschung des Kontos |
| KI-Anfragen (Firebase AI) | Werden gemäß Google-Richtlinien nicht dauerhaft gespeichert |
| Kaufdaten | Gemäß gesetzlicher Aufbewahrungsfristen (bis zu 10 Jahre, § 147 AO) |
| Feedback-E-Mails | Maximal 24 Monate in unserem Postfach, danach Löschung |
| Serverlogs (IP-Adressen) | Maximal 30 Tage, danach automatische Löschung |

---

## 12. Keine automatisierte Entscheidungsfindung

Es findet **keine automatisierte Entscheidungsfindung** im Sinne von Art. 22 DSGVO
statt, die dir gegenüber rechtliche Wirkung entfaltet oder dich erheblich
beeinträchtigt. KI-Funktionen erstellen lediglich Texte oder Zusammenfassungen, die
dich in keiner Weise rechtlich binden.

---

## 12a. Hinweis auf KI-Systeme (Art. 50 KI-Verordnung)

Diese App verwendet KI-Systeme zur Verarbeitung deiner Eingaben:

- **Google Gemini** (Firebase AI): KI-gestützte Textanalyse, Zusammenfassungen,
  Dashboard, Rückblicke und stilistische Textverbesserung
- **Groq Whisper**: Sprach-zu-Text-Transkription
- **Microsoft Edge Text-to-Speech**: Text-zu-Sprache-Vorlesefunktion

Gemäß Art. 50 der Verordnung (EU) 2024/1689 (KI-Verordnung) weisen wir darauf hin,
dass die genannten Funktionen auf künstlicher Intelligenz basieren. KI-Systeme können
**ungenaue, unvollständige oder irreführende Ausgaben** erzeugen ("Halluzinationen").
Prüfe wichtige Aussagen selbstständig und verlasse dich bei gesundheitlichen,
rechtlichen oder finanziellen Fragen auf qualifizierte Fachkräfte.

Die KI-Ausgaben sind **keine verbindlichen Aussagen**, sondern Vorschläge und
Zusammenfassungen zur Unterstützung deiner eigenen Reflexion.

---

## 13. Pflicht zur Bereitstellung personenbezogener Daten

Du bist nicht verpflichtet, uns personenbezogene Daten bereitzustellen. Die App ist
auch ohne Anmeldung, ohne Cloud-Dienste und ohne Analytics vollständig nutzbar. Ohne
die optionalen Cloud-Funktionen stehen dir lediglich die entsprechenden
Komfortfunktionen (Cloud-Transkription, Cloud-Backup, geräteübergreifende Nutzung,
KI-Funktionen) nicht zur Verfügung.

---

## 14. Änderungen dieser Datenschutzerklärung

Diese Datenschutzerklärung kann bei Änderungen der App oder der gesetzlichen
Grundlagen angepasst werden. Die jeweils aktuelle Version ist in der App unter
**Einstellungen → Datenschutz** sowie im Google Play Store abrufbar. Bei wesentlichen
Änderungen wirst du in der App darauf hingewiesen.

**Letzte Aktualisierung:** 20. April 2026

---

## 15. Kontakt

Bei Fragen zum Datenschutz oder zur Ausübung deiner Rechte:

**E-Mail:** dev.app.support@gmail.com
**Postanschrift:** Siehe Abschnitt 1 (Verantwortlicher)
