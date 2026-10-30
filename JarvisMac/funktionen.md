<!-- stand: JarvisMac 1.0.30 -->
Was Jarvis für macOS kann (geprüft am Code).

## Gehirn und Gedächtnis
- Chat mit Claude Opus 5.5, Sonnet 5.5, Haiku 5.5, Fable 5.1 (Anthropic-Schlüssel) oder GPT-6 Astra, GPT-6.1 Sol, GPT-6 Luna, GPT-6.1 Terra (eigene ChatGPT-Anmeldung am Mac per Gerätecode oder OpenAI-Schlüssel); Denkstufe und Schrittlimit wählbar; Websuche des Modells.
- Alle Werkzeuge des Handy-Jarvis über dessen MCP-Relay (Aufgaben, Wecker, Ideen, Kalender, Dienstplan, Biodaten, Stack-Labor, Epigenetik, Ablage, Prompts, Mail, Tagebuch, Wissen, Tagesauswertung, Agenten, Vorgänge, Programmieraufträge, Timer, Wetter, Fahrzeit, Handy-Einstellungen …). Ist das Handy aus, sagt Jarvis das.
- Gemeinsames Gedächtnis: Live-Gedächtnis (`jarvis_kontext`) zu jedem Gesprächsbeginn, Neues mit `jarvis_merken`; „Lernen am Mac“ gibt nach 10 Minuten Ruhe neue Angaben aus Mac-Gesprächen zurück (abschaltbar).
- Gespräche lokal gespeichert, durchsuchbar, automatischer Titel; Bilder (Datei, Ziehen, Bildschirmfoto) anhängbar.

## Agentenkanal mit Jarvis am Handy
- Allgemeiner Kanal über den Relay in beide Richtungen, solange der Mac verbunden ist: `handy_jarvis_sprechen` (frage, info), `handy_jarvis_lesen`. Nachrichten überstehen Abwesenheit und Neustarts (Quittung, id gegen Doppel, Eingang auf der Platte).
- Fragen vom Handy (Frage, Stand der Programmieraufgaben, Begutachtung des gesamten Geschehens am Mac) beantwortet Jarvis mit vollem Mac-Zugriff im eigenen Gespräch „Jarvis am Handy (Agentenkanal)“.
- Das Handy kann die App über die Jarvis-Brücke starten („Starte Jarvis Mac OS“); eine Frage an den Mac startet sie im Hintergrund, eine bloße Mitteilung nicht. Läufe für den Kanal: je Nachricht frisches Modellgespräch mit Kanal-Auszug, höchstens 10 Minuten, ohne Merken, Mail und Programmieraufträge, kein Lernen daraus.

## Mac (Vollzugriff, abschaltbar)
- `mac_befehl` (zsh), `mac_applescript`, `mac_oeffnen`, `mac_programme` (liste/vordergrund/beenden), `mac_bildschirm` (Bildschirmfoto ans Modell), `mac_bedienen` (Computer Use: klicken, ziehen, scrollen in Bildpixeln des letzten Fotos, danach neues Foto; braucht Bedienungshilfen), `mac_aufnahme` (Bildschirmvideo nach ~/Movies/Jarvis), `mac_tastatur`, `mac_zwischenablage`, `mac_mitteilung`, `mac_lautstaerke`, `mac_datei`, `mac_status`. Ist der Vollzugriff aus, gehen nur noch Öffnen, Mitteilungen, Programmliste und Status.
- `mac_screenshot` („Erstelle einen Screenshot“: ganzer Bildschirm als PNG am Bildschirmfoto-Ort, zusätzlich in der Zwischenablage), `screenshot_ins_cli` (Screenshot per Ctrl+V oder als Pfad in die zuletzt benutzte Terminal-Sitzung, optional mit Text und Absenden).
- `mac_einstellungen` (eigene Mac-Einstellungen per Sprache lesen/ändern), `mac_funktionen_lesen` (diese Übersicht).
- `terminal_befehl` (sichtbares Terminal-Fenster), `openlauncher_sitzung` (OpenLauncher `--jarvis-auftrag` mit CLI, Modell, Profil, Modus, Effort), `updater_zentrale` (Programmliste, öffnen), `chatgpt_app` (Nachricht in die ChatGPT-App).

## Sprache
- Weckwort „Hey Jarvis“ (einstellbar) offline am Mac; ⌥⌘J überall sprechen, ⌥⌘K Fenster. Ist Jarvis vorn, geht das offene Gespräch weiter; sonst beginnt ein neues im Hintergrund und oben mittig erscheint die orange Sprachpille wie am Handy (Pegel-Spektrum, nimmt nie den Fokus, nicht auf Bildschirmfotos; Klick öffnet Jarvis, X, lange drücken, Rechtsklick oder nach oben ziehen beendet).
- Gesprächsmodus wie am Handy: Piepton, zuhören bis 4,5 s Stille nach dem letzten Wort (höchstens 120 s), Jarvis, vorlesen, wieder von vorn; kommt nach dem Piepton nichts (7 s, ab der zweiten Runde 9 s), ist Schluss. Ausbefehle wie am Handy („Danke“, „Stopp“, „Das war's“, „Sprachsession beenden“ …) gehen nicht ans Modell. Mikrofon tippen: beendet die Aufnahme oder unterbricht Jarvis; Stopp in der Blase beendet das Gespräch.
- Diktat ins Eingabefeld (endet mit „ ; “; wie am Handy: Tipp startet, zweiter Tipp beendet, höchstens 10 Minuten, kein Abbruch bei Pausen; bei Apple ohne SpeechAnalyzer höchstens 55 Sekunden; ⏎ während der Aufnahme schreibt erst fertig und sendet dann). Transkription wie am Handy: Parakeet primeline offline am Mac (sherpa-onnx, geteiltes Modell der Voice-Overlays, Lade-/Rechenzeit und Speicher gemessen, nie stiller Rückfall, mit eigenem abschaltbarem Wörterbuch: bis 100 Begriffe gehen als Wortliste in die Beam-Suche, per Sprache über mac_einstellungen änderbar), Groq Whisper Large V3 Turbo/Large V3 mit Wörterbuch und den vier Halluzinationsfiltern, Apple offline mit Live-Vorschau.
- Vorlesen wie am Handy: Microsoft Edge, Google Chirp 3 HD, Gemini 3.8 Flash TTS und Flash-Lite TTS (kostenlos/bezahlt, Reihenfolge), Azure Neural (17 + Seraphina/Florian HD), Alibaba Qwen, eigene geklonte Stimme, Systemstimme; Favoriten mit Stern, Hörprobe je Stimme, Tempo je Anbieter, „Immer deutsch“, sortierbare Rückfallkette.

## Live-Gespräch
- Gleichzeitig zuhören und sprechen mit OpenAI GPT-Live/Realtime oder Gemini Live (eigene Schlüssel), Echounterdrückung des Macs, Unterbrechen möglich; startet die Echounterdrückung nicht (Fehler -10875), zweiter Versuch und danach Echosperre statt Abbruch, auch wenn der Ton nach dem Start nicht anläuft (kein Absturz mehr); Sachfragen gehen an dasselbe Jarvis-Gehirn. Solange Live läuft, steht oben immer die Sprachpille; ihr X beendet das Gespräch sofort. „Bei Stille beenden“ in den Einstellungen: nach 2, 5 (Vorgabe) oder 10 Minuten oder nie, gilt ab dem nächsten Gespräch; Sprechen am Mikrofon zählt nicht als Stille, das Ende wird angezeigt.

## Oberfläche
- Start (beim Öffnen): große Kugel, Begrüßung, Tagesauswertung, Schnellverknüpfungen, unten die Sprechblase mit Modell und Kosten.
- Sprechblase überall gleich wie am Handy: rechts Mikrofon und daneben genau ein Knopf: Live, sobald Text oder Anhang da ist der Pfeil, während einer Antwort Stopp. Neues Gespräch wird erst mit der ersten Nachricht gespeichert, ⌘N legt keine leeren an; alte leere Gespräche verschwinden beim Start.
- Zentrale: Sprechblase wie im Gespräch über den Schnellverknüpfungen, Kugel, Begrüßung, Tagesauswertung (vorlesbar, neu erstellen lassen), Aufgaben heute, Termine, Wetter, Mac-Kacheln.
- Leiste mit Reitern Gespräche, Programmierung, Projekte (oranger Punkt, wenn dort gearbeitet wird); Projekte anlegen, Gespräche im Projekt.
- Unter jeder Antwort: Kopieren, Vorlesen, Daumen hoch/runter (daraus lernt die Stil-Datei), Teilen, in neuem Gespräch fortsetzen, erneut fragen. Anthropic-Restguthaben vor dem Claude-Modellnamen.
- Leiste: Start, Zentrale, Einstellungen. Der Live-Bildschirm öffnet sich über den Knopf „Live“ in der Blase des Startbildschirms; läuft ein Gespräch, steht „Live läuft“ in der Leiste. ⌘L startet oder beendet Live direkt. Neben Programmierung, Ablage, Gedächtnis und Aktivität schrumpft die Einstellungsliste auf Symbole. Einstellungen: Kopf, Guthaben, Stimme, Sprache, Live, Mac & Rechte, Konten, Programmierung (alle Sessions, neuer Auftrag, Rückfrage beantworten, Nachtrag), Ablage (Explorer mit Bereichen, Volltextsuche, Vorschau, Öffnen, Sichern, Löschen), Gedächtnis (Merken, Langzeitkontext, Gemerktes, Live-Gedächtnis, Funktionsübersicht Mac und Handy, Handy-Reiter und CLI, Regeln für ChatGPT Live), Aktivität (Mac-Logbuch, Handy-Protokoll), Über.
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
- Prompts: Mikrofon im Prompt-Bereich und beim Programmierauftrag; jede Einsprechung endet mit „ ; “, dazwischen steht immer „ ; “. „Mit KI verbessern“ verbessert jede Aufgabe zwischen den Trennern einzeln und behält die Trenner, mit Rückgängig. Bearbeiten öffnet einen Editor über das ganze Fenster im Vollbildmodus von macOS und ändert den Prompt selbst. Nach „Senden“ fragt Jarvis, ob der Prompt gelöscht werden soll: Ja löscht nur den Eintrag, der Text in der Eingabe bleibt; Nein lässt ihn stehen.
- Guthaben: Einstellungen → Guthaben mit Anthropic und Google. Link „Bei der Anthropic-Konsole anmelden“ dort und im Modell-Menü: eigene Anmeldung im Fenster, die Sitzung liegt verschlüsselt im Tresor und bleibt bis zum eigenen Abmelden gespeichert (lehnt die Konsole sie ab, kommt nur zusätzlich der Link), danach liest der Mac das Anthropic-Guthaben selbst; ohne sie oder bei einer Störung über das Handy. Die Google-Anmeldung bleibt am Handy. In jeder Eingabeblase Guthaben, Kosten der letzten Claude-Aufgabe (aus den Tokens zum Listenpreis, ohne Websuche) und Modell.
- Zentrale: Sprechblase über den Schnellverknüpfungen, diese untereinander in voller Breite, Bereich schmaler.
- Gedächtnis: zusätzlich Funktionsübersicht Mac, Funktionsübersicht Handy, Handy-Reiter und CLI, Regeln für ChatGPT Live.
- `repo_lesen` liest am Mac versionierte Dateien direkt aus ~/proggs (ohne Vollzugriff wie bisher über das Handy) und markiert Gekürztes. `cli_frage` fragt im Hintergrund das CLI: nur Lesen und Suchen, ~/SK gesperrt, ohne Hooks und MCP-Server.

## Neu seit 1.0.23
- Tempo: Live-Gedächtnis wird schon beim Weckwort und neuen Gespräch vorgeladen, Werkzeugliste bleibt warm, Handy aus kostet keine Wartezeit; Gespräche, Logbuch, Mikrofon-Start und Aufnahme-Analyse laufen neben dem Hauptfaden, Gespräche laden im Hintergrund, gestreamter Text höchstens 20-mal je Sekunde und ohne Neurechnen alter Nachrichten; Animationen ruhen im Hintergrund und während Sprache und Live. Stopp beendet auch laufende Mac-Befehle. Scheitert eine Mitschrift, versucht Jarvis es ein zweites Mal und bietet danach neben dem Mikrofon „erneut umwandeln“ an, die Aufnahme bleibt bis zum nächsten Diktat erhalten. Text aus einer verschwundenen Blase übernimmt die nächste Blase.
