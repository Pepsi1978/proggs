<!-- stand: JarvisMac 1.0.2 -->
Was Jarvis für macOS kann (geprüft am Code).

## Gehirn und Gedächtnis
- Chat mit Claude Opus 5.5, Sonnet 5.5, Haiku 5.5, Fable 5.1 (Anthropic-Schlüssel) oder GPT-6 Astra, GPT-6.1 Sol, GPT-6 Luna, GPT-5.6 Terra (eigene ChatGPT-Anmeldung am Mac per Gerätecode oder OpenAI-Schlüssel); Denkstufe und Schrittlimit wählbar; Websuche des Modells.
- Alle Werkzeuge des Handy-Jarvis über dessen MCP-Relay (Aufgaben, Wecker, Ideen, Kalender, Dienstplan, Biodaten, Stack-Labor, Epigenetik, Ablage, Prompts, Mail, Tagebuch, Wissen, Tagesauswertung, Agenten, Vorgänge, Programmieraufträge, Timer, Wetter, Fahrzeit, Handy-Einstellungen …). Ist das Handy aus, sagt Jarvis das.
- Gemeinsames Gedächtnis: Live-Gedächtnis (`jarvis_kontext`) zu jedem Gesprächsbeginn, Neues mit `jarvis_merken`; „Lernen am Mac“ gibt nach 10 Minuten Ruhe neue Angaben aus Mac-Gesprächen zurück (abschaltbar).
- Gespräche lokal gespeichert, durchsuchbar, automatischer Titel; Bilder (Datei, Ziehen, Bildschirmfoto) anhängbar.

## Mac (Vollzugriff, abschaltbar)
- `mac_befehl` (zsh), `mac_applescript`, `mac_oeffnen`, `mac_programme` (liste/vordergrund/beenden), `mac_bildschirm` (Bildschirmfoto ans Modell), `mac_tastatur`, `mac_zwischenablage`, `mac_mitteilung`, `mac_lautstaerke`, `mac_datei`, `mac_status`. Ist der Vollzugriff aus, gehen nur noch Öffnen, Mitteilungen, Programmliste und Status.
- `terminal_befehl` (sichtbares Terminal-Fenster), `openlauncher_sitzung` (OpenLauncher `--jarvis-auftrag` mit CLI, Modell, Profil, Modus, Effort), `updater_zentrale` (Programmliste, öffnen), `chatgpt_app` (Nachricht in die ChatGPT-App).

## Sprache
- Weckwort „Hey Jarvis“ (einstellbar) offline am Mac, schwebende Sprach-Einblendung über allen Programmen; ⌥⌘J überall sprechen, ⌥⌘K Fenster.
- Sprachschleife: zuhören → Jarvis → vorlesen → 8 s Folgefenster; „Danke“, „Stopp“, „Hör auf“ beenden.
- Diktat ins Eingabefeld (endet mit „ ; “); Transkription Apple offline oder Groq Whisper.
- Vorlesen mit Google Chirp 3 HD (30 Stimmen), Rückfall auf die beste deutsche Systemstimme; Tempo, Hörprobe.

## Live-Gespräch
- Gleichzeitig zuhören und sprechen mit OpenAI GPT-Live/Realtime oder Gemini Live (eigene Schlüssel), Echounterdrückung des Macs, Unterbrechen möglich; Sachfragen gehen an dasselbe Jarvis-Gehirn.

## Oberfläche
- Zentrale: Kugel, Begrüßung, Schnellfrage, Tagesauswertung (vorlesbar), Aufgaben heute, Termine, Wetter, Mac-Kacheln.
- Bereiche: Gespräch, Live, Ablage (lesen, suchen, neuer Texteintrag), Gedächtnis (Gemerktes, Live-Gedächtnis, Merken), Aktivität (Mac-Logbuch, Handy-Protokoll), Einstellungen (Kopf, Stimme, Sprache, Live, Mac & Rechte, Konten, Über).
- Menüleisten-Symbol mit Schnellfrage, Sprechen, Live. Fenster-Kürzel ⌘N, ⌘D, ⌘L, ⌘1, ⌘,.
