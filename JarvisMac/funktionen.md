<!-- stand: JarvisMac 1.0.9 -->
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
- Start (beim Öffnen): große Kugel, Begrüßung, Reiter als Kacheln, Tagesauswertung, Schnellverknüpfungen, unten die Sprechblase mit Modell und Kosten.
- Zentrale: Kugel, Begrüßung, Schnellfrage, Tagesauswertung (vorlesbar, neu erstellen lassen), Aufgaben heute, Termine, Wetter, Mac-Kacheln.
- Leiste mit Reitern Gespräche, Programmierung, Projekte (oranger Punkt, wenn dort gearbeitet wird); Projekte anlegen, Gespräche im Projekt.
- Unter jeder Antwort: Kopieren, Vorlesen, Daumen hoch/runter (daraus lernt die Stil-Datei), Teilen, in neuem Gespräch fortsetzen, erneut fragen. Anthropic-Restguthaben vor dem Claude-Modellnamen.
- Bereiche: Start, Zentrale, Gespräch, Live, Programmierung (alle Sessions, neuer Auftrag, Rückfrage beantworten, Nachtrag), Ablage (Explorer mit Bereichen, Volltextsuche, Vorschau, Öffnen, Sichern, Löschen), Gedächtnis (Gemerktes, Live-Gedächtnis, Merken), Aktivität (Mac-Logbuch, Handy-Protokoll), Einstellungen (Kopf, Stimme, Sprache, Live, Mac & Rechte, Konten, Über).
- Menüleisten-Symbol mit Schnellfrage, Sprechen, Live. Fenster-Kürzel ⌘N, ⌘D, ⌘L, ⌘1 Start, ⌘2 Zentrale, ⌘,.

## Neu seit 1.0.5
- Zentrale: Am Mac als Funktionsboard über die volle Breite zuerst, darunter Begrüßung mit Kugel, Tagesauswertung und Aufgaben, Termine und Wetter, zuletzt die Schnellverknüpfungen.
- Kugel rund ohne Rahmen, seitlicher Schimmer, vibriert beim Sprechen, beim Zuhören Wellen und Mikrofon.
- Tresor: alle Schlüssel und die ChatGPT-Anmeldung verschlüsselt, gebunden an den Secure Enclave; Anzeige nur nach Touch ID, höchstens eine Minute. Jarvis' Werkzeuge weisen Zugriffe auf Zugangsdaten ab (Textprüfung, keine vollständige Sperre); Schlüssel werden in Werkzeugergebnissen und Logbuch geschwärzt.
- Ablage als Explorer mit Bereichen (Bilder, PDFs, Prompts, Recherchen, Texte, Dokumente, Tabellen, Präsentationen, Audio, Video, Sonstige), Vorschau, Öffnen, Sichern, Löschen.
- Gedächtnis: Langzeitkontext (Textdatei, synchron mit dem Handy, Entwurf bleibt erhalten, bei Konflikt entscheidet Frank), Gemerktes und Live-Gedächtnis eingeklappt, Live-Gedächtnis aktualisiert sich jede Minute.
- Logbuch: JSON je Ereignis (was, wo, wann, Werkzeug, Eingabe, Ergebnis, Fehler, Dauer; private Inhalte nur als Länge) unter Application Support/JarvisMac/Logbuch, höchstens 30 Tage; Werkzeug mac_logbuch.

## Neu seit 1.0.7
- Darstellung: Dunkelmodus Schwarz mit Orange, Hellmodus warm mit Orange. Umschalter ganz unten links: Hell → Automatik → Dunkel. Die Automatik wechselt bei Sonnenauf- und -untergang in Berlin, auf dem Mac gerechnet. Kennung `darstellung` in `mac_einstellungen`.
- Prompts: Mikrofon im Prompt-Bereich und beim Programmierauftrag; jede Einsprechung endet mit „ ; “, dazwischen steht immer „ ; “. „Mit KI verbessern“ verbessert jede Aufgabe zwischen den Trennern einzeln und behält die Trenner, mit Rückgängig. Bearbeiten öffnet einen Editor über das ganze Fenster im Vollbildmodus von macOS und ändert den Prompt selbst.
- Guthaben: Einstellungen → Guthaben mit Anthropic und Google (wie am Handy, die Anmeldung bleibt dort). In jeder Eingabeblase Guthaben, Kosten der letzten Claude-Aufgabe (Guthaben vorher minus nachher) und Modell.
- Zentrale: Sprechblase über den Schnellverknüpfungen, diese untereinander in voller Breite, Bereich schmaler.
- Gedächtnis: zusätzlich Funktionsübersicht Mac, Funktionsübersicht Handy, Handy-Reiter und CLI, Regeln für ChatGPT Live.
- `repo_lesen` liest am Mac direkt aus ~/proggs (ohne Vollzugriff wie bisher über das Handy), `cli_frage` fragt im Hintergrund das CLI, nur lesend.
