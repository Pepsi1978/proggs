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

Best Journal ist eine Tagebuch-App, die in erster Linie **lokal auf deinem Gerät**
arbeitet. Deine Tagebucheinträge werden standardmäßig **nicht in eine Cloud übertragen**
und **nicht auf unseren Servern gespeichert**.

Die App verarbeitet folgende Datenkategorien:

| Datenkategorie | Wo gespeichert | Zweck |
|----------------|----------------|-------|
| Tagebucheinträge (Text, Audio, Bilder) | Ausschließlich auf deinem Gerät | Kernfunktion der App |
| Einstellungen und Präferenzen | Ausschließlich auf deinem Gerät | App-Konfiguration |
| Geräteinformationen (Modell, OS-Version, App-Version) | Google-Server (Firebase) | Fehleranalyse, Stabilität |
| IP-Adresse | Google-Server (Firebase) — gekürzt/anonymisiert | Technisch erforderlich bei jeder Netzwerkverbindung |
| Android Werbe-ID (AAID) | Google-Server (Firebase Analytics) | Nur bei Einwilligung |
| Authentifizierungsdaten (optional) | Firebase Authentication | Account-Verwaltung |
| KI-Anfragen (optional) | Firebase AI / Google | KI-gestützte Funktionen |
| Kaufdaten (Google Play) | Google Play Billing | In-App-Käufe |

---

## 3. Berechtigungen der App und ihre Verwendung

Die App fordert folgende Android-Berechtigungen an. Jede Berechtigung wird **nur für die
angegebene Funktion** verwendet und kann in den Android-Systemeinstellungen jederzeit
widerrufen werden.

### 3.1 Internet (`INTERNET`) und Netzwerkstatus (`ACCESS_NETWORK_STATE`)
**Zweck:** Synchronisation (optional), KI-Funktionen (optional), In-App-Käufe,
Firebase-Dienste, Absturzberichte.
**Hinweis:** Ohne Internet funktioniert die App weiterhin — nur Cloud- und
KI-Funktionen sind dann deaktiviert.

### 3.2 Mikrofon (`RECORD_AUDIO`)
**Zweck:** Sprachaufnahmen für Tagebucheinträge (Diktierfunktion, Sprachnotizen).
**Verarbeitung:** Aufnahmen werden standardmäßig **lokal** auf deinem Gerät gespeichert.
Nur wenn du aktiv die Transkriptionsfunktion nutzt, werden Audiodaten zur Transkription
an den jeweiligen KI-Dienst übermittelt (siehe Abschnitt 5).
**Rechtsgrundlage:** Einwilligung durch aktives Auslösen der Funktion (Art. 6 Abs. 1
lit. a DSGVO).

### 3.3 Kamera (`CAMERA`)
**Zweck:** Foto-Anhänge zu Tagebucheinträgen.
**Verarbeitung:** Fotos werden ausschließlich lokal gespeichert und nicht automatisch
hochgeladen.

### 3.4 Ungefährer Standort (`ACCESS_COARSE_LOCATION`)
**Zweck:** Optionale Anzeige des ungefähren Ortes (Stadt/Region) bei einem
Tagebucheintrag.
**Verarbeitung:** Der Standort wird **nur auf Anfrage** ermittelt, lokal beim Eintrag
gespeichert und **nicht** an externe Dienste übermittelt.

### 3.5 Benachrichtigungen (`POST_NOTIFICATIONS`)
**Zweck:** Erinnerungen an das Schreiben von Einträgen (wenn du Reminder aktivierst).
**Verarbeitung:** Benachrichtigungen werden ausschließlich lokal vom Gerät erzeugt.

### 3.6 Autostart nach Neustart (`RECEIVE_BOOT_COMPLETED`)
**Zweck:** Reaktivierung geplanter Erinnerungen nach einem Geräte-Neustart.
**Verarbeitung:** Ausschließlich lokal, keine Datenübertragung.

---

## 4. Lokale Datenspeicherung

Alle Tagebuchinhalte (Texte, Sprachaufnahmen, Bilder, Stimmungen, Tags) werden in einer
lokalen SQLite-Datenbank (Android Room) ausschließlich auf deinem Gerät gespeichert.

- **Speicherort:** Interner App-Speicher (vom Betriebssystem geschützt)
- **Zugriff:** Nur die Best-Journal-App selbst
- **Löschung:** Durch Deinstallation der App oder über die Einstellungen („Alle Daten
  löschen")

---

## 5. Verarbeitung durch Drittanbieter (Firebase / Google)

Die App nutzt Dienste der **Google Ireland Limited** (Gordon House, Barrow Street,
Dublin 4, Irland) sowie deren Muttergesellschaft **Google LLC** (1600 Amphitheatre
Parkway, Mountain View, CA 94043, USA).

### 5.1 Firebase Analytics
**Zweck:** Anonyme Nutzungsstatistiken zur Fehleranalyse und Produktverbesserung.
**Erhobene Daten:** Gerätetyp, Betriebssystemversion, App-Version, Nutzungshäufigkeit,
ungefähre Region (Land), anonymisierte Geräte-ID (Firebase Instance ID), **IP-Adresse
(gekürzt)**, **Android Werbe-ID (AAID)**, Ereignisdaten (z. B. App geöffnet, Funktion
genutzt).
**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO, § 25 Abs. 1 TTDSG).
Analytics ist beim ersten Start **standardmäßig deaktiviert** und wird erst nach
ausdrücklicher Zustimmung aktiviert. Du kannst die Einwilligung jederzeit in den
App-Einstellungen unter **„Einstellungen → Datenschutz → Analytics"** widerrufen.
**Werbe-ID zurücksetzen:** In den Android-Systemeinstellungen unter
**„Einstellungen → Datenschutz → Werbung"** kannst du die Werbe-ID jederzeit
zurücksetzen oder deaktivieren.

### 5.2 Firebase Authentication (optional)
**Zweck:** Optionale Anmeldung (z. B. für geräteübergreifende Nutzung).
**Erhobene Daten:** E-Mail-Adresse, Anmelde-ID, IP-Adresse, Zeitstempel.
**Rechtsgrundlage:** Vertragserfüllung (Art. 6 Abs. 1 lit. b DSGVO), sofern du einen
Account anlegst.
**Hinweis:** Eine Anmeldung ist **nicht erforderlich**, um die App zu nutzen.

### 5.3 Firebase AI / Generative AI
**Zweck:** KI-gestützte Funktionen (z. B. Zusammenfassungen, Verbesserungsvorschläge,
Transkription).
**Erhobene Daten:** Nur die konkrete Anfrage, die du durch aktive Nutzung einer
KI-Funktion auslöst. Keine automatische Übermittlung deiner Tagebucheinträge.
**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO) durch aktives Auslösen
der Funktion.
**Wichtig:** Sende über KI-Funktionen keine besonders sensiblen personenbezogenen Daten
Dritter. Die Anfragen werden auf Google-Servern verarbeitet.

### 5.4 Firebase App Check (Play Integrity)
**Zweck:** Schutz vor Missbrauch und automatisierten Anfragen.
**Erhobene Daten:** Geräteintegritäts-Token von Google Play (Hash-basiert,
geräteunabhängig), App-Signatur.
**Rechtsgrundlage:** Berechtigtes Interesse an Missbrauchsschutz (Art. 6 Abs. 1 lit. f
DSGVO).

### 5.5 Firebase Remote Config
**Zweck:** Ferngesteuerte Konfiguration (z. B. Feature-Flags, Texte).
**Erhobene Daten:** Anonymisierte App-Instanz-ID, App-Version.
**Rechtsgrundlage:** Berechtigtes Interesse am ordnungsgemäßen Betrieb (Art. 6 Abs. 1
lit. f DSGVO).

**Weitere Informationen:**
- Datenschutzerklärung Google: https://policies.google.com/privacy
- Firebase-Datenschutz: https://firebase.google.com/support/privacy
- Play Integrity: https://developer.android.com/google/play/integrity

### 5.6 Übermittlung in Drittländer (USA)
Google kann Daten in den USA verarbeiten. Die Übermittlung erfolgt auf Grundlage der
**EU-Standardvertragsklauseln** (Art. 46 DSGVO) sowie des
**EU-US Data Privacy Framework** (Angemessenheitsbeschluss der EU-Kommission vom
10. Juli 2023).

---

## 6. In-App-Käufe (Google Play Billing)

Für optionale Premium-Funktionen nutzt die App die Zahlungsabwicklung **Google Play
Billing**.

- **Anbieter:** Google Ireland Limited
- **Erhobene Daten:** Nur die zum Kauf notwendigen Daten (Transaktions-ID,
  Kauf-Token, gekauftes Produkt, Zeitstempel)
- **Zahlungsdaten:** Werden **ausschließlich von Google** verarbeitet — wir erhalten
  keine Kreditkartennummern, PayPal-Zugänge oder Kontodaten
- **Rechtsgrundlage:** Vertragserfüllung (Art. 6 Abs. 1 lit. b DSGVO)
- **Datenschutzerklärung Google Play:** https://play.google.com/about/play-terms/

---

## 7. Kontolöschung und Datenlöschung

Wenn du ein Nutzerkonto angelegt hast (optional über Firebase Authentication):

### 7.1 Konto in der App löschen
**Einstellungen → Konto → Konto löschen**

Beim Löschen werden unwiderruflich entfernt:
- Authentifizierungsdaten (Firebase Auth)
- Alle mit dem Konto verknüpften Cloud-Inhalte
- E-Mail-Adresse und Profilinformationen

### 7.2 Lokale Tagebuchdaten löschen
**Einstellungen → Daten → Alle Daten löschen** oder **App deinstallieren**

### 7.3 Löschung per E-Mail anfordern
Falls du keinen Zugriff mehr auf die App hast, kannst du die Löschung schriftlich
anfordern: **dev.app.support@gmail.com** — Betreff: „Kontolöschung Best Journal".
Bearbeitungsfrist: 30 Tage.

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

### Zuständige Aufsichtsbehörde

**Der Hessische Beauftragte für Datenschutz und Informationsfreiheit**
Gustav-Stresemann-Ring 1
65189 Wiesbaden
Telefon: +49 611 1408-0
E-Mail: poststelle@datenschutz.hessen.de
Website: https://datenschutz.hessen.de

(Zuständig, da Sitz des Verantwortlichen in Hessen)

Du kannst dich auch an die für deinen Wohnsitz zuständige Aufsichtsbehörde wenden.
Eine Übersicht aller deutschen Aufsichtsbehörden:
https://www.bfdi.bund.de/DE/Service/Anschriften/Laender/Laender-node.html

---

## 9. Datensicherheit

- Lokale Datenbank im geschützten App-Speicherbereich (durch Android-Sandboxing)
- Verschlüsselte Übertragung (HTTPS/TLS 1.2+) bei allen Netzwerkverbindungen
- Firebase App Check zum Schutz vor Missbrauch
- Optional: App-Sperre durch PIN oder Biometrie (Fingerabdruck, Gesichtserkennung)
- Keine Speicherung unverschlüsselter Zugangsdaten
- Automatische Sicherheitsupdates über den Google Play Store

---

## 10. Kinder und Jugendliche

Die App richtet sich an Nutzer **ab 13 Jahren**. Für Kinder unter 16 Jahren ist eine
Einwilligung der Erziehungsberechtigten erforderlich (Art. 8 DSGVO), sofern eine
Anmeldung erfolgt oder KI-Funktionen genutzt werden. Die App erhebt wissentlich keine
Daten von Kindern unter 13 Jahren.

---

## 11. Speicherdauer

| Daten | Dauer |
|-------|-------|
| Lokale Tagebucheinträge | Bis zur Löschung durch dich oder Deinstallation der App |
| Firebase Analytics | 14 Monate (Google-Standard), danach automatische Löschung |
| Firebase Authentication | Bis zur Löschung des Kontos |
| KI-Anfragen | Werden gemäß Google-Richtlinien nicht dauerhaft gespeichert |
| Kaufdaten | Gemäß gesetzlicher Aufbewahrungsfristen (bis zu 10 Jahre, § 147 AO) |
| Serverlogs (IP-Adressen) | Maximal 30 Tage, danach automatische Löschung |

---

## 12. Keine automatisierte Entscheidungsfindung

Es findet **keine automatisierte Entscheidungsfindung** im Sinne von Art. 22 DSGVO statt,
die dir gegenüber rechtliche Wirkung entfaltet oder dich erheblich beeinträchtigt.
KI-Funktionen erstellen lediglich Texte oder Zusammenfassungen, die dich in keiner
Weise rechtlich binden.

---

## 13. Pflicht zur Bereitstellung personenbezogener Daten

Du bist nicht verpflichtet, uns personenbezogene Daten bereitzustellen. Die App ist
auch ohne Anmeldung und ohne Analytics vollständig nutzbar. Ohne die optionalen
Cloud-Dienste stehen dir lediglich die entsprechenden Komfortfunktionen (Sync,
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
