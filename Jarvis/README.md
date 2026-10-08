# Jarvis

Persönlicher Assistent auf dem Handy (Android, Kotlin, Jetpack Compose). Paket `de.frank.jarvis`.
Zielbild und Gespräche dazu: `Datenbank/JARVIS/`.

Version 1 kann zwei Dinge:

1. **ChatGPT-Plugin (MCP).** Jarvis ist ein MCP-Server, der auf dem Handy läuft. In ChatGPT wird er als eigener
   Konnektor „Jarvis“ eingetragen. Danach genügt im Chat oder im Sprachmodus „Jarvis, speichere …“.
2. **Eigener Chat.** Jarvis hat ein eigenes Modell aus der ChatGPT-Anmeldung (Codex-Gerätecode), Modell und
   Denkstufe sind wählbar. Es bedient dieselben Werkzeuge selbstständig.

Angebundene Apps: **Geniale Aufgaben** (`de.frank.aufgaben`, lesen und schreiben) und der Biomarker-Bereich von
**Entropie Reductor** (`de.frank.entropyreducer`, nur lesen) , **Geniale Ideen** (`de.frank.genialeideen`, lesen und schreiben)
sowie der **Kalender** des Handys (alle synchronisierten
Kalender, also auch Google; nur lesen, Android-Erlaubnis `READ_CALENDAR`).

## Aufbau

```
ChatGPT (Cloud) ──HTTPS──> Jarvis-Relay (eigener Server) ──WebSocket──> Jarvis-App ──ContentProvider──> Geniale Aufgaben
                           srv1774016.hstgr.cloud                       └─ MCP-Server
```

- **Relay** (`server/`): kleiner Dienst auf dem Hostinger-Server, die öffentliche Gegenstelle. Er nimmt
  `POST /j/<Geheimnis>/mcp` an und reicht den Aufruf über eine WebSocket ans Handy weiter. Der MCP-Server selbst
  läuft auf dem Handy; der Relay kennt keine Werkzeuge. Ist das Handy kurz weg, wartet er 8 Sekunden, beantwortet
  `initialize` und `tools/list` aus dem Zwischenspeicher und meldet bei Werkzeugaufrufen „Handy nicht erreichbar“.
- **Verbindung** (`tunnel/Tunnel.kt`): Die App baut die WebSocket von sich aus auf (`wss://<Server>/geraet/ws`),
  weist sich mit dem Server-Schlüssel aus und nennt ihr Geheimnis. Bricht sie ab, wird sie neu aufgebaut
  (2 bis 30 Sekunden Pause, sofort bei zurückkehrendem Netz). Das Handy braucht keinen offenen Port.
- **MCP-Server** (`mcp/McpServer.kt`): zustandslos, nur JSON-Antworten (kein Server-Stream, keine Sitzungen).
- **Dienst** (`dienst/JarvisDienst.kt`): Vordergrunddienst (Typ `specialUse`), hält die Verbindung auch bei
  gesperrtem Handy, startet nach dem Einschalten und nach Updates von selbst.
- **Werkzeuge** (`faehigkeit/`): Jede angebundene App ist eine `Faehigkeit` mit einer Liste von `Werkzeug`en.
  Plugin, eigener Chat und Oberfläche lesen alle dasselbe `Register`.
- **Agent** (`agent/JarvisAgent.kt`): Schleife Modell → Werkzeug → Ergebnis, höchstens 8 Schritte. Werkzeugaufrufe
  laufen über ein festes JSON-Format im Antworttext, damit jedes Text-Modell Jarvis antreiben kann.

## Zugangsschutz

- **Plugin-Adresse:** `https://<Server>/j/<Geheimnis>/mcp`. Das Geheimnis (40 Zeichen) erzeugt die App; nur das
  zuletzt vom Handy gemeldete gilt. Alles andere beantwortet der Server mit 404. ChatGPT-Konnektoren arbeiten
  ohne Anmeldung, deshalb ist diese Adresse der Schlüssel. Erneuern: Einstellungen → Plugin-Adresse erneuern.
- **Server-Schlüssel:** Nur ein Handy mit dem Schlüssel aus `~/SK/Jarvis/relay.properties` (`host`, `token`) darf
  sich am Relay anmelden. Der Bau backt beides in die App; fehlt die Datei (Bau in der Cloud), trägt man Adresse
  und Schlüssel in der App unter Einstellungen ein.
- Am Server ist dafür Port 443 öffentlich (IPv4 und IPv6), aber nur dieser eine Dienst hängt daran. Second Brain,
  Werft und Dashboard bleiben an der WireGuard-Adresse.

## Werkzeuge im Plugin

| Werkzeug | Zweck |
|---|---|
| `aufgaben_lesen` | Aufgaben und Termine lesen (heute, morgen, Datum, demnächst, Eingang, offen, erledigt, alle; Suche) |
| `aufgabe_anlegen` | Neue Aufgabe; `text` und `datum` sind Pflicht, Erinnerung mit Vorlesen ist Vorgabe |
| `aufgabe_aendern` | Ändern und verschieben, per `id` oder Suchwort |
| `aufgabe_erledigen` | Abhaken oder wieder öffnen, per `id` oder Suchwort |
| `aufgabe_loeschen` | Löschen, per `id` oder Suchwort |
| `biomarker_auswertung` | Aktueller Wert jeder Messgröße gegen den Schnitt der Tage davor, Schnitte 7/30/90 Tage, Einordnung |
| `biomarker_tag` | Alle Werte eines Tages (Whoop, Oura, Waage) samt Trainings |
| `biomarker_verlauf` | Zeitreihen einzelner Messgrößen, tageweise oder als Wochen-/Monatsschnitt |
| `trainings_lesen` | Trainingsliste mit Pace, Puls, Distanz, Kalorien, VO2max und Summen |
| `training_details` | Ein Training mit Kilometer-Abschnitten |
| `biomarker_katalog` | Alle Messgrößen mit Einheit, Quelle und Zeitraum |
| `kalender_lesen` | Termine Tag für Tag mit Dienst-Zeile; mit Suchwort bis ein Jahr voraus |
| `dienstplan_lesen` | Je Tag Nachtdienst, Tagdienst, frei oder Urlaub mit Abfahrtszeit, nächster Dienst, nächste freie Tage |
| `ideen_lesen`, `idee_lesen`, `idee_anlegen`, `idee_aendern`, `idee_loeschen` | Geniale Ideen; neue Ideen glättet die Ideen-App selbst mit ihrer KI |
| `ablage_liste`, `ablage_lesen`, `ablage_schreiben`, `ablage_loeschen` | Eigene Textdateien von Jarvis (Recherchen, Ausarbeitungen, Notizen) |
| `agenten_liste`, `agent_starten`, `agent_anlegen`, `agent_loeschen` | Agenten, die im Hintergrund arbeiten und ihr Ergebnis in die Ablage legen |
| `mail_senden`, `mail_lesen`, `mail_details` | Gmail über App-Passwort; senden nur an freigegebene Empfänger |
| `wissen_inhalt`, `wissen_lesen`, `wissen_suchen` | Wissens-Datenbank: Ordner `Datenbank` des Repositories, bei jeder Synchronisierung aufs Handy geholt |
| `tagesdaten_lesen` | Die Tagesdatenbank: alle Daten der letzten Synchronisierung, ganz oder je Bereich |
| `tagesauswertung_lesen` | Die fertige Tagesauswertung samt frisch gelesenen Aufgaben; veraltete Fassungen werden nie als aktuell ausgegeben |
| `tagesauswertung_erstellen` | Stößt eine neue Auswertung im Hintergrund an (ein bis drei Minuten) |
| `jarvis_status` | Erreichbarkeit, Datum und Uhrzeit auf dem Handy, angebundene Apps |
| `jarvis_auftrag` | Freier Auftrag an den Agenten für Mehrschritt-Aufgaben (Zeitfenster 45 s) |

Fehlt beim Anlegen der Tag, obwohl eine Uhrzeit genannt wurde, fragt ChatGPT nach; ruft es trotzdem auf, lehnt die
Aufgaben-App mit einem Hinweis ab. Mehrere Treffer bei einem Suchwort führen zu einer Rückfrage statt zu einer
geratenen Änderung. Ein wiederholter identischer Anlege-Aufruf innerhalb von 90 Sekunden legt nichts doppelt an.

## Tagesauswertung

`auswertung/Tagesauswertung.kt` und `auswertung/Zeitplan.kt`. Jarvis erstellt zu bis zu drei einstellbaren Uhrzeiten
(Vorgabe 4:25, 12:00, 16:25) im Hintergrund eine Auswertung und legt sie unter `filesDir/tagesauswertung/` ab
(die letzten 30). Ablauf:

1. Abgleich in Entropie Reductor anstoßen (frische Daten von Whoop, Oura, Waage; höchstens 90 Sekunden).
2. Daten nach festen Regeln sammeln: Rahmen der nächsten sechs Tage (Arbeitstag oder frei, Schlaf- und freie
   Zeitfenster, `KalenderFaehigkeit.rahmen`), Termine, Biodaten des Tages, Vergleich gegen 7 Tage, den letzten Monat
   und alle bisherigen Tage, Trainings der letzten 14 Tage, offene Aufgaben als Planungshinweis.
3. Das eigene Modell schreibt daraus die Auswertung in sechs Abschnitten (HEUTE, ERHOLUNG UND SCHLAF, KÖRPER UND
   TRAINING, EINSCHÄTZUNG, EMPFEHLUNG FÜR HEUTE, AUSBLICK). Ist das Modell nicht erreichbar, bleibt der Datenbericht.

Zeitplan: immer genau ein exakter Wecker für den nächsten Lauf (`setExactAndAllowWhileIdle`); neu gestellt nach jedem
Lauf, beim Start, nach dem Einschalten und nach Zeit- oder Zeitzonenänderung. Ein verpasster Lauf wird beim nächsten
Dienststart nachgeholt, wenn er höchstens sechs Stunden zurückliegt. Fehlt beim Lauf noch der Schlafwert des Tages,
folgt einmalig ein Zusatzlauf 90 Minuten später. Beim Abruf werden die Aufgaben immer frisch gelesen; liegt nur eine
veraltete Fassung vor, liefert der Abruf sofort einen frischen Datenbericht und stößt die volle Auswertung an.

Grenzen: Nach „Stopp erzwingen“ gehen Wecker verloren, bis die App einmal geöffnet wird. Die Vergleiche trennen
nicht zwischen Nacht- und Tagschlaf; das Modell bekommt dazu nur eine Regel mit.

## Tagesdatenbank

Bei jeder Tagesauswertung (also zu den drei Uhrzeiten) legt Jarvis die gesammelten Daten aller Apps ab: Rahmen der
nächsten Tage, Termine, Biodaten des Tages und im Vergleich, Trainings und alle offenen Ideen (gekürzt). Das Werkzeug
`tagesdaten_lesen` gibt sie ganz oder je Bereich zurück. Aufgaben sind nicht enthalten, sie werden immer frisch gelesen.
Die einzelnen Werkzeuge der Apps lesen weiterhin live vom Handy; die Tagesdatenbank spart die vielen Einzelaufrufe.

## Ablage und Agenten

- **Ablage** (`faehigkeit/AblageFaehigkeit.kt`): Markdown-Dateien unter `filesDir/ablage/`, die Jarvis anlegt, liest
  und löscht.
- **Agenten** (`agent/Agenten.kt`): Ein Agent ist ein Name plus eine Rolle. Eingebaut sind „Recherche“ und
  „Machbarkeit“; mit `agent_anlegen` entstehen weitere (Dateien unter `filesDir/agenten/`). `agent_starten` kehrt
  sofort zurück; der Lauf (höchstens 14 Schritte, 12 Minuten) passiert im Dienst mit dem eigenen Modell, den
  Werkzeugen aller Apps und der Internet-Suche. Das Ergebnis landet in der Ablage, auf Wunsch zusätzlich per Mail,
  und eine Benachrichtigung meldet es.
- **Internet-Suche** (`faehigkeit/WebFaehigkeit.kt`): Tavily, Schlüssel aus `~/SK/Tavily/tavily-api-key.txt` (beim Bau
  eingebacken). Nur für Jarvis und seine Agenten, nicht im ChatGPT-Plugin.

## E-Mail

`faehigkeit/MailFaehigkeit.kt`: Gmail über SMTP (senden) und IMAP (lesen) mit einem App-Passwort, das Frank in den
Einstellungen einträgt. Gesendet wird nur an die eigene Adresse und an ausdrücklich freigegebene Empfänger. Der Inhalt
eingegangener Mails wird den Modellen als fremde Information gekennzeichnet, nie als Anweisung.

## Sprache

Übernommen aus Geniale Ideen (Pakete `audio/`, `tts/`, `speech/`, `text/`, `observability/`, `data/settings/SecureSettings.kt`):

- **Mikrofon:** Solange das Eingabefeld leer ist, ist der Knopf daneben das Mikrofon. Aufnahme mit `MicRecorder`, Mitschrift
  über Groq (`whisper-large-v3-turbo`) mit den vier Filtern gegen erfundene Texte bei Stille; lange Aufnahmen werden geteilt.
  Der Text geht direkt an Jarvis, die Antwort wird vorgelesen (abschaltbar).
- **Vorlesen:** `speech/Vorleser.kt` liest absatzweise und lässt die nächsten zwei Absätze schon synthetisieren
  (Edge ohne Schlüssel, Google Chirp 3 HD, Alibaba, eigene Stimme). `VorleseDienst` hält die Wiedergabe im Hintergrund.
  Lautsprecher-Knöpfe gibt es an Antworten, an der Tagesauswertung und in der Ablage; Agenten-Ergebnisse werden auf
  Wunsch von selbst vorgelesen (Schalter in den Einstellungen oder `vorlesen` bei `agent_starten`).
- Schlüssel (Groq, Google, Alibaba) trägt man in den Einstellungen ein. Sie liegen in `jarvis_sprache_prefs`, getrennt von
  den übrigen Einstellungen, damit das übernommene Modul unverändert bleibt.

## App-Sperre

Mit eingeschalteter Sperre (Vorgabe) verlangt Jarvis beim Öffnen und nach 30 Sekunden außerhalb der App den Fingerabdruck
oder die Gerätesperre (`MainActivity`, `androidx.biometric`). Der Hintergrunddienst, das Plugin und die Tagesauswertung
laufen davon unberührt weiter. Ohne eingerichtete Gerätesperre bleibt die App offen.

## Dienstplan

Der Dienstplan steht als Ganztagstermine im Kalender und wird in `faehigkeit/KalenderFaehigkeit.kt` ausgewertet:
„Nacht 1“ bis „Nacht 4“ = Nachtdienst (Abfahrt etwa 16:00 Uhr), „Tag 1“ bis „Tag 4“ = Tagdienst (Abfahrt etwa 4:30 Uhr).
Steht am selben Tag „X“ oder „F“, ist frei, bei „U“ Urlaub; der Diensteintrag bleibt im Kalender stehen und entfällt.
Tage ohne Diensteintrag sind frei. Die Abfahrtszeiten stehen als Konstanten in derselben Datei.

## Auswertungen: wer rechnet

Die Biomarker-Auswertung rechnet Entropie Reductor selbst (Durchschnitte, Abweichung, Einordnung) und gibt nur
Kennzahlen zurück. Das Sprachmodell in ChatGPT deutet und formuliert. So bleibt eine Antwort im Sprachmodus unter
einer Sekunde Werkzeugzeit, und es geht nie die ganze Historie über die Leitung. Der eigene Agent von Jarvis
(`jarvis_auftrag`) ist für mehrstufige Aufträge gedacht und für Sprache zu langsam.

Nach neuen Werkzeugen muss ChatGPT die Werkzeugliste neu laden (Plugin öffnen → aktualisieren bzw. neu verbinden).

## Einrichten

1. Jarvis öffnen, mit ChatGPT verbinden (Gerätecode), Modell wählen.
2. Unter Einstellungen die Plugin-Adresse kopieren. Am PC auf chatgpt.com: Einstellungen → Apps und Konnektoren →
   Erweitert → Entwicklermodus → Erstellen: Name „Jarvis“, MCP-Server-URL = Plugin-Adresse, Authentifizierung „Keine“.
3. Akku-Sparen für Jarvis ausschalten (Knopf in der App); bei Samsung Jarvis zusätzlich unter
   „Nie in Standby versetzen“ eintragen.

## Server ausrollen

Wie beim Second Brain liegt auf dem Server kein Git. Dateien hochladen und neu bauen:

```
scp -i ~/SK/second-brain/id_ed25519 server/compose.yaml server/Caddyfile root@168.231.83.205:/opt/jarvis/
scp -i ~/SK/second-brain/id_ed25519 server/relay/relay.py server/relay/Dockerfile root@168.231.83.205:/opt/jarvis/relay/
ssh -i ~/SK/second-brain/id_ed25519 root@168.231.83.205 "cd /opt/jarvis && docker compose up -d --build"
```

`/opt/jarvis/.env` enthält `JARVIS_GERAET_TOKEN` (Kopie: `~/SK/Jarvis/server.env`). Für IPv6 gibt es die
UFW-Regel „Jarvis-Relay (IPv6)“; IPv4 läuft über Dockers Portfreigabe an der Firewall vorbei.
Prüfen von außen: `curl -4 -o /dev/null -w "%{http_code}" https://srv1774016.hstgr.cloud/` muss 404 liefern.

## Eine weitere App anbinden

1. In der Ziel-App einen ContentProvider nach dem Muster `Aufgaben/.../bruecke/JarvisBruecke.kt` anlegen, geschützt
   mit der Signatur-Erlaubnis.
2. In Jarvis eine Klasse `XyFaehigkeit : Faehigkeit` schreiben und in `Register.alle` eintragen, dazu den
   `<queries>`-Eintrag im Manifest.

## Signierung

Jarvis und die angebundenen Apps müssen mit demselben Schlüssel signiert sein (gemeinsamer Debug-Key aus
`~/SK/Android/`), sonst verweigert Android den Zugriff auf die Brücke. Beide Apps erklären die Erlaubnis
`de.frank.aufgaben.permission.JARVIS`, deshalb ist die Installationsreihenfolge egal.

## Noch offen

- Ob der ChatGPT-Sprachmodus eigene Konnektoren aufruft und ob er Schreibaktionen ohne Bestätigungsdialog ausführt,
  muss am Gerät geprüft werden. Falls nicht: GPT Live direkt in Jarvis einbauen (Werkzeuge und Agent stehen bereit).
- Bei tiefem Stromsparmodus ohne Ladekabel kann Android die Verbindung trotz Dienst kappen.
