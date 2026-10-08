# Session Handoff — 08.10.2026, ca. 18:45 Uhr

## Ziel (1-3 Saetze)
Jarvis als persoenlichen Assistenten auf Franks Handy bauen: Android-App `Jarvis/` (Paket `de.frank.jarvis`), die als MCP-Server laeuft und in ChatGPT als Plugin „Jarvis" eingetragen ist. Stand jetzt: Jarvis 1.8.1 mit 31 Werkzeugen, alles committet, gepusht, installiert, im Update-Ordner.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Letzte Aenderung: Rueckkehrzeiten nach dem Dienst (Tagdienst zu Hause gegen 18:15 Uhr, Nachtdienst gegen 5:50 Uhr) in `Jarvis/app/src/main/java/de/frank/jarvis/faehigkeit/KalenderFaehigkeit.kt` (Konstanten RUECKKEHR_TAG / RUECKKEHR_NACHT), Commit 5500b567a.
- Uncommitteter Arbeitsstand: keiner an Jarvis. (Im Repo liegen fremde uncommittete Dateien paralleler Sitzungen, z. B. `.claude/agent-memory/shared/*`, `OpenLauncher/models.json`, `SunoDownload/package-lock.json` — nicht anfassen, immer nur mit Pfaden committen.)

## Aktueller Status
- Erledigt (alles auf origin/main):
  - Jarvis-App: MCP-Server auf dem Handy, Vordergrunddienst (specialUse), Verbindung per WebSocket zum eigenen Relay.
  - Relay auf dem Hostinger-VPS: `/opt/jarvis` (Compose: `jarvis-relay` + `jarvis-caddy`), `https://srv1774016.hstgr.cloud`, Port 443 oeffentlich (IPv4 + IPv6, UFW-Regel fuer IPv6). Quelle im Repo: `Jarvis/server/`. Deploy: scp + `docker compose up -d --build`.
  - Bruecken (ContentProvider `JarvisBruecke`, Signatur-Erlaubnis) in: `Aufgaben` (lesen/schreiben, Erlaubnis `de.frank.aufgaben.permission.JARVIS`), `EntropieReductor` (Biomarker, nur lesen, plus `abgleich`), `GenialeIdeen` (lesen/schreiben, KI-Glaettung), `GenialerWecker` (lesen/stellen). Erlaubnis fuer die drei letzten: `de.frank.jarvis.permission.BRUECKE`.
  - Ohne Bruecke: Kalender + Dienstplan (Android CalendarContract, lesen und schreiben), Wetter (Open-Meteo), Tagebuch (Drive-Ordner ueber Nur-Lese-rclone auf dem Relay), Wissens-Datenbank (Ordner `Datenbank` im oeffentlichen Repo), Ablage, Agenten (Recherche, Machbarkeit, selbst angelegte; Tavily-Suche), Gmail (App-Passwort, von Frank eingetragen, funktioniert).
  - Tagesauswertung: automatisch 4:25, 12:00, 16:25 (exakter Wecker), 7 Abschnitte inkl. RUECKBLICK (Tagebuch) und Wetter; Tagesdatenbank; Mitdenken (`faehigkeit/Mitdenken.kt`).
  - App: Reiter Jarvis / Ablage / Aktivitaet / Einstellungen, Mikrofon (Groq), absatzweises Vorlesen (Modul aus GenialeIdeen kopiert), Fingerabdruck-Sperre, Aktualisieren-Knopf, „Berechtigung erneuern" fuer Drive.
- In Arbeit: nichts.
- Blockiert / wartet auf Frank:
  - Plugin in ChatGPT aktualisieren (Werkzeugnamen haben sich geaendert, jetzt 31).
  - Groq-Schluessel in Jarvis eintragen (Einstellungen → Spracheingabe), Stimme waehlen.
  - Rueckmeldung zur Oberflaeche: Mikrofon, Vorlesen, Ablage-Reiter, Sperre, Auge, Wetter-Ort, „Berechtigung erneuern" hat noch niemand bedient.
  - Ob der erste automatische Lauf um 4:25 Uhr am 09.10. ausgeloest hat.
  - Ob ein X im Google-Kalender wirklich in Franks Blau erscheint.

## Relevante Dateien
- `Jarvis/README.md` — vollstaendige Beschreibung von Aufbau, Werkzeugen, Server, Tagesauswertung. ZUERST LESEN.
- `Jarvis/app/src/main/java/de/frank/jarvis/faehigkeit/` — je App eine `*Faehigkeit.kt`; `Faehigkeit.kt` enthaelt `Register` und die Buendel-Helfer `als` / `mit` / `gesetzt`.
- `Jarvis/app/src/main/java/de/frank/jarvis/mcp/McpServer.kt` — MCP-Protokoll, eigene Werkzeuge (tagesauswertung_lesen, tagesdaten_lesen, agenten, agent_starten, jarvis_status), Anweisungstext an ChatGPT.
- `Jarvis/app/src/main/java/de/frank/jarvis/auswertung/Tagesauswertung.kt` + `Zeitplan.kt` — Sammeln, Auftrag ans Modell, Wecker.
- `Jarvis/app/src/main/java/de/frank/jarvis/dienst/JarvisDienst.kt` — Vordergrunddienst, Agentenlaeufe, Auswertung.
- `Jarvis/app/src/main/java/de/frank/jarvis/tunnel/Tunnel.kt` (WebSocket zum Relay), `DriveFreigabe.kt` (OAuth-Erneuerung).
- `Jarvis/server/relay/relay.py`, `Jarvis/server/compose.yaml`, `Jarvis/server/Caddyfile`.
- Bruecken: `Aufgaben/.../bruecke/JarvisBruecke.kt`, `EntropieReductor/.../data/bruecke/JarvisBruecke.kt`, `GenialeIdeen/.../bridge/JarvisBruecke.kt`, `GenialerWecker/.../wecker/JarvisBruecke.kt`.
- Geheimnisse (nie ins Repo): `~/SK/Jarvis/relay.properties` (host, token), `~/SK/Jarvis/server.env`, `~/SK/rclone/gdrive-lesen.conf` (Nur-Lese-Drive), `~/SK/rclone/google-oauth-client.txt`, `~/SK/Tavily/tavily-api-key.txt`, SSH `~/SK/second-brain/id_ed25519`.
- Merkzettel: `OpenLauncher/Profiles/ClaudeCode/minimal/projects/C--Users-barwa-proggs/memory/jarvis-relay-und-chatgpt-plugin.md` und `handy-nicht-blind-fernsteuern.md`.

## Getroffene Entscheidungen
- Kein ngrok, kein fremder Tunnel-Anbieter: Frank will keine Fremdanbieter-Konten. Gegenstelle ist der eigene Hostinger-Server; der MCP-Server bleibt auf dem Handy.
- Zugangsschutz gegenueber ChatGPT: geheime Adresse `/j/<40 Zeichen>/mcp`, „Keine Authentifizierung" im Plugin. Das Handy weist sich am Relay mit dem Geraete-Token aus.
- Auswertungen rechnen auf dem Handy (Entropie Reductor / Kalender), ChatGPT deutet nur. Der eigene Agent ist fuer Sprache zu langsam.
- Werkzeuge gebuendelt (39 → 28, jetzt 31): bei neuen Faehigkeiten zuerst pruefen, ob sie in ein vorhandenes Werkzeug passen. `jarvis_auftrag` gestrichen, Agenten ersetzen ihn.
- Mail sendet nur an Frank und freigegebene Adressen; Mail-Inhalte gelten als fremde Information.
- Tagebuch ueber NUR-LESE-Zugang (`drive.readonly`, Wurzel = Tagebuch-Ordner) auf dem Server; Frank hat das so gewaehlt.
- Vorlese-Modul aus GenialeIdeen (absatzweise mit Vorausladen), nicht aus Aufgaben (liest am Stueck).
- Dienstregeln: Nacht 1–4 Abfahrt 16:00, zu Hause 5:50, Schlaf 6–15; Tag 1–4 Aufstehen 4:00, Abfahrt 4:30, zu Hause 18:15, am Vorabend Schlafen 20 Uhr; X/F = frei, U = Urlaub, kein Eintrag = frei.
- Commits: direkt auf main, Deutsch, imperativ, IMMER nur mit Pfaden (parallele Sitzungen).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- ngrok-SDK (`com.ngrok:ngrok-java`) in der App: liess sich technisch loesen (native Bibliothek nicht in der APK, JNI_OnLoad meldet Version 1.8 → Patch beim Bau; steht in `bugs/android/android-platform.md` Abschnitt 11), wurde aber verworfen, weil Frank kein ngrok-Konto will. NICHT wieder vorschlagen.
- Handy per `adb shell input` oder `am start` bedienen, waehrend Frank es nutzt: zweimal passiert (Eingaben landeten in seinem Browser; Jarvis ueber ChatGPT geoeffnet). NICHT machen. Testen von aussen ueber den Relay; bei Bedarf Frank bitten, selbst zu tippen. Erlaubt und unkritisch: `adb install -r`, `logcat`, `pm grant`, `dumpsys`.
- Bash-Heredocs mit einzelnen Apostrophen im Text brechen in diesem Werkzeug ab („unexpected EOF"), auch bei einfach-quotiertem Delimiter. Laengere Dateien mit dem Write-Tool schreiben (auch diese Uebergabe-Notiz: erst ins Scratchpad, dann kopieren), Aenderungen ueber ein Python-Skript im Scratchpad.
- Python-urllib von diesem PC ohne IPv4-Zwang: haengt 90 s je Aufruf (totes IPv6). Immer `curl -4` bzw. getaddrinfo auf AF_INET filtern.
- `screencap` ueber `exec-out` liefert auf dem Fold eine unbrauchbare Datei (zwei Displays); wenn noetig `screencap -d <display-id> -p /sdcard/...` und pullen.
- Tagesauswertung direkt nach `adb install -r` anstossen: der Lauf wird durch die Neuinstallation abgebrochen; nach der Installation ~10 s warten (Handy verbindet sich neu), dann anstossen.
- Computer Use fuer Desktop-Programme steht in dieser Umgebung nicht zur Verfuegung, nur Claude in Chrome.

## Wichtige Recherche-Ergebnisse
- ChatGPT: Plugins → Hinzufuegen → „Benutzerdefinierten MCP-Server erstellen" (nicht „Plugin erstellen"), Verbindung „Server-URL", „Keine Authentifizierung". ChatGPT meldet sich als `openai-mcp/1.0.0`, ruft zuerst `server/discover` auf (Jarvis antwortet „unbekannte Methode", danach `initialize`). Aufrufe funktionieren; in der Handy-App erscheint Jarvis nicht unter „Installiert", arbeitet aber.
- Fable-Rat zur Tagesauswertung (umgesetzt): exakter Wecker statt WorkManager, Nachholen bis 6 h, veraltete Fassung nie als aktuell ausgeben, Nachbesserung +90 min bei fehlendem Schlafwert. Nicht umgesetzt: Zahlenpruefung des Modelltexts, getrennte Statistik fuer Tag- und Nachtschlaf.
- Repo `Pepsi1978/proggs` ist OEFFENTLICH — der Ordner `Datenbank` (Ziele usw.) ist damit oeffentlich lesbar. Frank wurde darauf hingewiesen, noch keine Entscheidung.
- Geraet: `192.168.0.155:5555` (WLAN-adb). Installierte Varianten: `de.frank.entropyreducer.debug`, `de.frank.genialeideen` (Build `schnell`: `./gradlew :app:assembleSchnell`), `de.frank.genialerwecker` (Projekt `GenialerWecker`, NICHT GenialerWeckerAndroid = `.app`).

## Testen ohne Handy-Bedienung
Geheimnis holen und Aufrufe wie ChatGPT schicken (Skripte lagen im Sitzungs-Scratchpad und sind nicht dauerhaft; bei Bedarf neu schreiben):
- Geheimnis: per ssh (`-i ~/SK/second-brain/id_ed25519 root@168.231.83.205`) im Container `jarvis-relay` aus `/data/zustand.json` das Feld `geheimnis` lesen (`docker exec jarvis-relay python -c ...`).
- Aufruf: POST `https://srv1774016.hstgr.cloud/j/<geheimnis>/mcp`, JSON-RPC `tools/call` mit `{"name": ..., "arguments": {...}}`, IPv4 erzwingen. Das Geheimnis nie ausgeben.
- Relay-Log: `docker logs jarvis-relay` (eine Zeile je Anfrage, ohne Geheimnis).
- Testdaten immer wieder loeschen (Testaufgabe, Testwecker, Testtermin, Testidee, Ablage-Testdatei) — bisher alles bereinigt.

## Naechste Schritte (priorisiert)
1. Franks Rueckmeldung zur Oberflaeche und zum Sprachmodus abwarten und Fehler beheben.
2. Aus der Vorschlagsliste (von Frank noch nicht alle entschieden): Morgen-/Abfahrts-Ansage (Tagesauswertung von selbst vorlesen), Wecker passend zum Dienstplan schalten, echte Anmeldung fuers Plugin (OAuth) vor weiteren schreibenden Faehigkeiten, Tagebuch per Jarvis schreiben, Wochenrueckblick.
3. Dienst- und Schlafzeiten in den Einstellungen einstellbar machen (stehen als Konstanten in `KalenderFaehigkeit.kt`).
4. Getrennte Vergleichswerte fuer Nachtschlaf und Tagschlaf nach Nachtdienst.
5. Falls der ChatGPT-Sprachmodus das Plugin nicht zuverlaessig nutzt: GPT Live direkt in Jarvis (Werkzeuge und Agent stehen bereit).

## Offene Fragen
- Soll das Repo bzw. der Ordner `Datenbank` oeffentlich bleiben?
- Funktioniert der ChatGPT-Sprachmodus am Handy mit dem Plugin, auch bei Schreibaktionen ohne stoerende Bestaetigung? (Text-Chat ist bestaetigt: Aufgabe abhaken und Termine abfragen klappten.)
- Erscheint die Kalenderfarbe richtig?

## Abschluss-Pflichten bei jeder Code-Aenderung (aus dem Profil)
bauen → Versionslog-Eintrag unten anhaengen (`app/src/main/assets/versionslog.json`, Zeit per Befehl holen) → committen (nur Pfade) → `git pull --rebase --autostash` → `git push origin HEAD:main` → `adb -s 192.168.0.155:5555 install -r` → `apk-update.ps1 -Projekt <Projekt>`. Versionen jetzt: Jarvis 1.8.1 (vc 12), Aufgaben 1.0.31 (vc 32), EntropieReductor 0.29.14 (vc 349), GenialeIdeen 1.7.15 (vc 46), GenialerWecker 1.1.115 (vc 110).

## Anker
- Branch: main
- Letzte Commits:
5500b567a Jarvis: rechne mit den genannten Rückkehrzeiten nach Tag- und Nachtdienst
81204a1fb Jarvis: ergänze Wettervorhersage, Termine in den Kalender eintragen und das Mitdenken gegen Dienstplan, Termine und Wetter
8be0c5b66 Jarvis: binde den Genialen Wecker an, fasse die Werkzeuge auf 28 zusammen, ergänze den Knopf zum Erneuern der Drive-Berechtigung; Genialer Wecker: Jarvis-Brücke
03f4403dd Jarvis: hole das Tagebuch über einen Nur-Lese-Zugang vom Server und fasse die letzten Tage in der Tagesauswertung zusammen
210a169d9 Jarvis: ergänze Mikrofon mit Groq-Mitschrift, absatzweises Vorlesen, Ablage-Reiter, Aktualisieren-Knopf, Wissens-Datenbank aus dem Repo, Fingerabdruck-Sperre und Passwort-Auge
a8fbeeb8e Jarvis: binde Geniale Ideen an, ergänze Tagesdatenbank, Ablage, Agenten mit Internet-Recherche und E-Mail über Gmail; Geniale Ideen: Jarvis-Brücke
