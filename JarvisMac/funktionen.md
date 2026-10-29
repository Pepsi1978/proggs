<!-- stand: JarvisMac 1.0.3 -->
Was Jarvis für macOS kann (geprüft am Code).

## Gehirn und Gedächtnis
- Chat mit Claude Opus 5.5, Sonnet 5.5, Haiku 5.5, Fable 5.1 (Anthropic-Schlüssel) oder GPT-6 Astra, GPT-6.1 Sol, GPT-6 Luna, GPT-5.6 Terra (eigene ChatGPT-Anmeldung am Mac per Gerätecode oder OpenAI-Schlüssel); Denkstufe und Schrittlimit wählbar; Websuche des Modells.
- Alle Werkzeuge des Handy-Jarvis über dessen MCP-Relay (Aufgaben, Wecker, Ideen, Kalender, Dienstplan, Biodaten, Stack-Labor, Epigenetik, Ablage, Prompts, Mail, Tagebuch, Wissen, Tagesauswertung, Agenten, Vorgänge, Programmieraufträge, Timer, Wetter, Fahrzeit, Handy-Einstellungen …). Ist das Handy aus, sagt Jarvis das.
- Gemeinsames Gedächtnis: Live-Gedächtnis (`jarvis_kontext`) zu jedem Gesprächsbeginn, Neues mit `jarvis_merken`; „Lernen am Mac“ gibt nach 10 Minuten Ruhe neue Angaben aus Mac-Gesprächen zurück (abschaltbar).
- Gespräche lokal gespeichert, durchsuchbar, automatischer Titel; Bilder (Datei, Ziehen, Bildschirmfoto) anhängbar.

## Mac (Vollzugriff, abschaltbar)
- `mac_befehl` (zsh), `mac_applescript`, `mac_oeffnen`, `mac_programme` (liste/vordergrund/beenden), `mac_bildschirm` (Bildschirmfoto ans Modell), `mac_tastatur`, `mac_zwischenablage`, `mac_mitteilung`, `mac_lautstaerke`, `mac_datei`, `mac_status`. Ist der Vollzugriff aus, gehen nur noch Öffnen, Mitteilungen, Programmliste und Status.
- `mac_screenshot` („Erstelle einen Screenshot“: ganzer Bildschirm als PNG am Bildschirmfoto-Ort, zusätzlich in der Zwischenablage), `screenshot_ins_cli` (Screenshot per Ctrl+V oder als Pfad in die zuletzt benutzte Terminal-Sitzung, optional mit Text und Absenden).
- `mac_einstellungen` (eigene Mac-Einstellungen per Sprache lesen/ändern), `mac_funktionen_lesen` (diese Übersicht).
- `terminal_befehl` (sichtbares Terminal-Fenster), `openlauncher_sitzung` (OpenLauncher `--jarvis-auftrag` mit CLI, Modell, Profil, Modus, Effort), `updater_zentrale` (Programmliste, öffnen), `chatgpt_app` (Nachricht in die ChatGPT-App).

## Sprache
- Weckwort „Hey Jarvis“ (einstellbar) offline am Mac, schwebende Sprach-Einblendung über allen Programmen; ⌥⌘J überall sprechen, ⌥⌘K Fenster.
- Sprachschleife: zuhören → Jarvis → vorlesen → 8 s Folgefenster; „Danke“, „Stopp“, „Hör auf“ beenden.
- Diktat ins Eingabefeld (endet mit „ ; “). Transkription wie am Handy: Parakeet primeline offline am Mac (sherpa-onnx, geteiltes Modell der Voice-Overlays, Lade-/Rechenzeit und Speicher gemessen, nie stiller Rückfall), Groq Whisper Large V3 Turbo/Large V3 mit Wörterbuch und den vier Halluzinationsfiltern, Apple offline mit Live-Vorschau.
- Vorlesen wie am Handy: Microsoft Edge, Google Chirp 3 HD, Gemini 3.8 Flash TTS und Flash-Lite TTS (kostenlos/bezahlt, Reihenfolge), Azure Neural (17 + Seraphina/Florian HD), Alibaba Qwen, eigene geklonte Stimme, Systemstimme; Favoriten mit Stern, Hörprobe je Stimme, Tempo je Anbieter, „Immer deutsch“, sortierbare Rückfallkette.

## Live-Gespräch
- Gleichzeitig zuhören und sprechen mit OpenAI GPT-Live/Realtime oder Gemini Live (eigene Schlüssel), Echounterdrückung des Macs, Unterbrechen möglich; startet die Echounterdrückung nicht (Fehler -10875), zweiter Versuch und danach Echosperre statt Abbruch; Sachfragen gehen an dasselbe Jarvis-Gehirn.

## Oberfläche
- Zentrale: Kugel, Begrüßung, Schnellfrage, Tagesauswertung (vorlesbar, neu erstellen lassen), Aufgaben heute, Termine, Wetter, Mac-Kacheln.
- Leiste mit Reitern Gespräche, Programmierung, Projekte (oranger Punkt, wenn dort gearbeitet wird); Projekte anlegen, Gespräche im Projekt.
- Unter jeder Antwort: Kopieren, Vorlesen, Daumen hoch/runter (daraus lernt die Stil-Datei), Teilen, in neuem Gespräch fortsetzen, erneut fragen. Anthropic-Restguthaben vor dem Claude-Modellnamen.
- Bereiche: Gespräch, Live, Programmierung (alle Sessions, neuer Auftrag, Rückfrage beantworten, Nachtrag), Ablage (Einträge lesen, suchen, neuer Texteintrag; Reiter Prompts mit Speichern, KI-Umformulieren, Kopieren, Löschen, Senden an den Programmierer), Gedächtnis (Gemerktes, Live-Gedächtnis, Merken), Aktivität (Mac-Logbuch, Handy-Protokoll), Einstellungen (Kopf, Stimme, Sprache, Live, Mac & Rechte, Konten, Über).
- Menüleisten-Symbol mit Schnellfrage, Sprechen, Live. Fenster-Kürzel ⌘N, ⌘D, ⌘L, ⌘1, ⌘,.
