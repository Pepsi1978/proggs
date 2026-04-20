# Datenschutzerklärung für Best Journal

**Stand:** 20. April 2026
**App:** Best Journal (Android)
**Entwickler:** Frank Barwandt

---

## 1. Verantwortlicher im Sinne der DSGVO

Verantwortlich für die Verarbeitung personenbezogener Daten in der App „Best Journal" ist:

**Frank Barwandt**
c/o Impressumservice Dein-Impressum
Stettiner Straße 41
35410 Hungen
Deutschland

E-Mail: dev.app.support@gmail.com

> Diese Adresse wird vom Dienst „Dein Impressum" bereitgestellt und dient als
> ladungsfähige Anschrift gemäß § 5 TMG und Art. 13 DSGVO.

---

## 2. Überblick: Welche Daten verarbeitet die App?

Best Journal ist eine Tagebuch-App, die in erster Linie **lokal auf deinem Gerät** arbeitet.
Deine Tagebucheinträge werden standardmäßig **nicht in eine Cloud übertragen** und
**nicht auf unseren Servern gespeichert**.

Die App verarbeitet folgende Datenkategorien:

| Datenkategorie | Wo gespeichert | Zweck |
|----------------|----------------|-------|
| Tagebucheinträge (Text, Audio, Bilder) | Ausschließlich auf deinem Gerät | Kernfunktion der App |
| Einstellungen und Präferenzen | Ausschließlich auf deinem Gerät | App-Konfiguration |
| Anonyme Nutzungsstatistiken (Firebase Analytics) | Google-Server (EU/USA) | Fehleranalyse, Produktverbesserung |
| Authentifizierungsdaten (optional, nur bei Anmeldung) | Firebase Authentication | Account-Verwaltung |
| KI-Anfragen (optional, nur bei aktiver Nutzung) | Firebase AI / Google | KI-gestützte Funktionen |
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
**Verarbeitung:** Aufnahmen werden standardmäßig **lokal** auf deinem Gerät
gespeichert. Nur wenn du aktiv die Transkriptionsfunktion nutzt, werden Audiodaten
zur Transkription an den jeweiligen KI-Dienst übermittelt (siehe Abschnitt 5).

### 3.3 Kamera (`CAMERA`)
**Zweck:** Foto-Anhänge zu Tagebucheinträgen.
**Verarbeitung:** Fotos werden ausschließlich lokal gespeichert und nicht automatisch
hochgeladen.

### 3.4 Ungefährer Standort (`ACCESS_COARSE_LOCATION`)
**Zweck:** Optionale Anzeige des ungefähren Ortes (Stadt/Region) bei einem Tagebucheintrag.
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
ungefähre Region (Land), anonymisierte Geräte-ID (Instance ID).
**Rechtsgrundlage:** Berechtigtes Interesse (Art. 6 Abs. 1 lit. f DSGVO) an stabiler
und funktionierender Software. In den App-Einstellungen kannst du Analytics deaktivieren.

### 5.2 Firebase Authentication
**Zweck:** Optionale Anmeldung (z. B. für Geräteübergreifende Nutzung).
**Erhobene Daten:** E-Mail-Adresse, Anmelde-ID.
**Hinweis:** Eine Anmeldung ist **nicht erforderlich**, um die App zu nutzen.

### 5.3 Firebase AI / Generative AI
**Zweck:** KI-gestützte Funktionen (z. B. Zusammenfassungen, Verbesserungsvorschläge,
Transkription).
**Erhobene Daten:** Nur die konkrete Anfrage, die du durch aktive Nutzung einer
KI-Funktion auslöst. Keine automatische Übermittlung deiner Tagebucheinträge.
**Rechtsgrundlage:** Einwilligung (Art. 6 Abs. 1 lit. a DSGVO) durch aktives Auslösen
der Funktion.

### 5.4 Firebase App Check (Play Integrity)
**Zweck:** Schutz vor Missbrauch und automatisierten Anfragen.
**Erhobene Daten:** Geräteintegritäts-Token von Google Play.

### 5.5 Firebase Remote Config
**Zweck:** Ferngesteuerte Konfiguration (z. B. Feature-Flags, Texte).
**Erhobene Daten:** Anonymisierte App-Instanz-ID.

**Datenschutzerklärung Google/Firebase:** https://policies.google.com/privacy
**Firebase-Datenschutz:** https://firebase.google.com/support/privacy

### 5.6 Übermittlung in Drittländer
Google kann Daten in den USA verarbeiten. Die Übermittlung erfolgt auf Grundlage der
**EU-Standardvertragsklauseln** sowie des **EU-US Data Privacy Framework**.

---

## 6. In-App-Käufe (Google Play Billing)

Für optionale Premium-Funktionen nutzt die App die Zahlungsabwicklung **Google Play
Billing**.

- **Anbieter:** Google Ireland Limited
- **Erhobene Daten:** Nur die zum Kauf notwendigen Daten (Transaktions-ID,
  Kauf-Token, gekauftes Produkt)
- **Zahlungsdaten:** Werden **ausschließlich von Google** verarbeitet — wir erhalten
  keine Kreditkartennummern, PayPal-Zugänge oder Kontodaten
- **Datenschutzerklärung Google Play:** https://play.google.com/about/play-terms/

---

## 7. Deine Rechte nach DSGVO

Du hast jederzeit folgende Rechte:

| Recht | Artikel | Wie |
|-------|---------|-----|
| Auskunft | Art. 15 DSGVO | E-Mail an dev.app.support@gmail.com |
| Berichtigung | Art. 16 DSGVO | E-Mail oder in der App |
| Löschung | Art. 17 DSGVO | Daten in der App löschen oder App deinstallieren |
| Einschränkung | Art. 18 DSGVO | E-Mail an dev.app.support@gmail.com |
| Datenübertragbarkeit | Art. 20 DSGVO | Export-Funktion in der App |
| Widerspruch | Art. 21 DSGVO | E-Mail an dev.app.support@gmail.com |
| Widerruf der Einwilligung | Art. 7 Abs. 3 DSGVO | In den App-Einstellungen |
| Beschwerde | Art. 77 DSGVO | Bei der zuständigen Datenschutzaufsichtsbehörde |

**Zuständige Aufsichtsbehörde in Deutschland:**
Die für deinen Wohnsitz zuständige Landesdatenschutzbehörde. Eine Übersicht findest
du unter: https://www.bfdi.bund.de/DE/Service/Anschriften/Laender/Laender-node.html

---

## 8. Datensicherheit

- Lokale Datenbank im geschützten App-Speicherbereich (durch Android-Sandboxing)
- Verschlüsselte Übertragung (HTTPS/TLS) bei allen Netzwerkverbindungen
- Firebase App Check zum Schutz vor Missbrauch
- Optional: App-Sperre durch PIN oder Biometrie (Fingerabdruck, Gesichtserkennung)

---

## 9. Kinder und Jugendliche

Die App richtet sich an Nutzer ab **13 Jahren**. Für Kinder unter 16 Jahren ist eine
Einwilligung der Erziehungsberechtigten erforderlich (Art. 8 DSGVO), sofern eine
Anmeldung erfolgt oder KI-Funktionen genutzt werden.

---

## 10. Speicherdauer

| Daten | Dauer |
|-------|-------|
| Tagebucheinträge | Bis zur Löschung durch dich oder Deinstallation der App |
| Firebase Analytics | 14 Monate (Google-Standard), danach automatische Löschung |
| Firebase Authentication | Bis zur Löschung des Accounts |
| KI-Anfragen | Werden nicht dauerhaft gespeichert (siehe Google-Richtlinien) |
| Kaufdaten | Gemäß gesetzlicher Aufbewahrungsfristen (bis zu 10 Jahre, § 147 AO) |

---

## 11. Keine automatisierte Entscheidungsfindung

Es findet **keine automatisierte Entscheidungsfindung** im Sinne von Art. 22 DSGVO
statt, die dir gegenüber rechtliche Wirkung entfaltet oder dich erheblich beeinträchtigt.

---

## 12. Änderungen dieser Datenschutzerklärung

Diese Datenschutzerklärung kann bei Änderungen der App oder der gesetzlichen
Grundlagen angepasst werden. Die jeweils aktuelle Version ist in der App unter
**Einstellungen → Datenschutz** sowie im Google Play Store abrufbar.

**Letzte Aktualisierung:** 20. April 2026

---

## 13. Kontakt

Bei Fragen zum Datenschutz oder zur Ausübung deiner Rechte:

**E-Mail:** dev.app.support@gmail.com
**Postanschrift:** Siehe Abschnitt 1 (Impressum)
