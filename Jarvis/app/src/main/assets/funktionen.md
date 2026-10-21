<!-- stand: 207 1.8.196 -->
Was Jarvis in dieser Version kann, geprüft am Code. Werkzeugnamen in Backticks gelten für den eigenen Chat, die Agenten und ChatGPT (Plugin), soweit nicht anders vermerkt.

## Zugänge und Modelle

- Eigener Chat in der App mit Modellen aus der ChatGPT-Anmeldung (Codex-Gerätecode): GPT-6 Astra, GPT-6.1 Sol, GPT-6 Luna, GPT-5.6 Terra; Denkstufe wählbar; Schrittlimit je Anfrage 10–100 oder unbegrenzt.
- ChatGPT-Plugin (MCP-Server auf dem Handy, über eigenen Relay-Server erreichbar): dieselben Werkzeuge; dazu nur im Plugin `jarvis_kontext` (Startkontext/Gesamtbild), `jarvis_status`, `tagesauswertung_lesen`, `tagesdaten_lesen`. Ist das Handy offline, antwortet das Plugin „Handy nicht erreichbar“.
- Vordergrunddienst hält die Verbindung auch bei gesperrtem Handy, startet nach Neustart und Update von selbst.
- Gemini gibt es nur im Live-Gespräch, nicht als Chat-Modell. Keine Jarvis-App für Windows, macOS oder iOS.

## Angebundene Apps

- Geniale Aufgaben: `aufgaben_lesen`, `aufgabe_anlegen`, `aufgabe_aendern` (auch abhaken), `aufgabe_loeschen`.
- Genialer Wecker: `wecker_lesen`, `wecker_stellen` (anlegen, ändern, schalten, auslassen), `wecker_loeschen`.
- Geniale Ideen: `ideen_lesen` (mit id Volltext), `idee_speichern` (anlegen/ändern), `idee_loeschen`.
- Entropie Reductor, nur lesen: `biomarker_auswertung`, `biomarker_tag`, `biomarker_verlauf` (ohne Messgrößen = Katalog), `trainings_lesen`; `biomarker_aktualisieren` stößt den Abgleich an. Vor dem Lesen holt Jarvis frische Daten, wenn sie älter als 30 Minuten sind.
- Stack-Labor: `stacks_lesen`, `stack_mittel_setzen`, `stack_speichern`, `mittel_speichern`; `stacklabor_loeschen` nur mit Bestätigung. Agenten und Tagesauswertung lesen nur.
- Epigenetik: `epigenetik_lesen` (Auswertung, startet fehlende Recherchen, wartet bis 50 s), `epigenetik_recherchieren`, `epigenetik_schalter`, `epigenetik_uebersicht`.
- Kalender des Handys (alle synchronisierten): `kalender_lesen`, `dienstplan_lesen` (ausgewerteter Schichtplan), `kalender_eintragen` (ganztägig oder mit Uhrzeit, Farbe), `kalender_loeschen`.
- Tagebuch (über den Server aus Google Drive): `tagebuch_lesen` für Tag, Zeitraum oder Suchwort. Nur lesen.
- Wissens-Datenbank: `wissen_lesen` (Inhaltsverzeichnis, Datei, Suche).
- Gmail: `mail_lesen` (mit nr vollständig), `mail_senden` (ohne Empfänger an Frank; nur erlaubte Empfänger).
- GitHub-Repo, nur lesen: `repo_lesen` (Projekte, Dateien, Code-Suche, Verlauf). Jarvis ändert nie Code im Repo.
- Web: `web_suche` (Kurzantwort und Quellen), `web_seite_lesen`; auch im eigenen Chat, Sprachmodus und Live-Gespräch. Aktuelle Fragen beantwortet Jarvis damit sofort im Gespräch, kurz mit Quelle; im Gespräch zuerst die Websuche des ChatGPT-Modells (eine gezielte Suche, höchstens 20 s), sonst Tavily (höchstens 12 s); in Agenten und über ChatGPT/MCP gründlich mit Tavily zuerst. Geht keine Suche, sagt er das, statt einen Agenten zu starten. Daten aus Franks Apps kommen weiter aus deren Werkzeugen.

## Alltag

- Wetter für sieben Tage: `wetter_lesen`, mit Datum und Uhrzeit zugleich Abgleich mit Dienst, Schlaf und Terminen.
- Fahrzeit mit Verkehr (Google Routes): `fahrzeit_lesen`. Losfahr-Meldung an Arbeitstagen laut Dienstplan: `abfahrt_einstellen` (Ankunftszeiten, Vorlauf, Vorlesen); in der Meldung „Heute still“ und Navigation.
- Timer (`timer`): klingeln wie ein Wecker, überstehen Neustart. Stoppuhr (`stoppuhr`) mit laufender Benachrichtigung. Beide nur über Jarvis startbar, kein eigener Bildschirm.
- Einstellungen per Sprache lesen und ändern: `einstellungen` (Benachrichtigungen, Vorlesen, Stimme, Ruhezeit, Hey Jarvis, Wetter-Ort, Timer, Modell, Darstellung …). Zugangsschlüssel und Passwörter gehen damit bewusst nicht.

## Tagesauswertung

- Schreibt Jarvis selbst im Hintergrund (stündlich oder alle 2–6 Stunden, auf Wunsch Pause im Schlaf nach Dienstplan): Rückblick, Aktuell, Ausblick aus allen Apps. Ohne Modell nur Datenbericht. Archiv der früheren Auswertungen ist durchsuchbar.

## Gedächtnis

- Merkdateien, bei jeder Antwort vor Augen: Zusammenarbeit mit ChatGPT (Symbiose), Regeln, Ziele, Über Frank, Programmierregeln (nur für Jarvis, gehen nie an den Programmierer), Stil, Entscheidungen. `jarvis_merken`, `jarvis_vergessen`, `jarvis_gemerktes_lesen`; in der App ansehen, bearbeiten, einsprechen, KI-Korrektur, Rückgängig.
- Live-Gedächtnis: 18 Bereiche aus allen Apps (Gemerktes, Dienst, Kalender, Aufgaben, Wecker, Stack-Labor, Gespräche, Biodaten, Tagesauswertung, Ideen, Trainings, Wetter, 10-Tage-Rückblick, Ablage, E-Mail, Wissen, Tagebuch); ändert sich etwas in einer App, wird der Bereich nach Sekunden neu gelesen.
- `gedaechtnis_suchen`: Suche über alles, auch weit zurück (Tagebuch, Gespräche, Notizen, Gemerktes, Ideen, Ablage, frühere Auswertungen).
- Lernen: aus ruhenden Gesprächen nach 10 Minuten und aus dem Tagebuch; prüft Gemerktes etwa alle 6 Stunden, Programmierwünsche dabei gegen den Code. Abschaltbar.
- Diese Funktionsübersicht: `funktionen_lesen`, nur bei Bedarf, nicht im Live-Gedächtnis; pflegt sich nach installierten Updates aus dem Versionslog.
- `logbuch_lesen` (Fehler, Dauer, Abläufe der App), `protokoll_lesen` (was Jarvis, ChatGPT und Agenten zuletzt getan haben); im Plugin zusätzlich `jarvis_status` mit funktionen=true (rohe Werkzeugliste).

## Ablage, Dateien, Bilder

- Ablage für Texte und Dateien jeder Art: `ablage_lesen`, `ablage_schreiben`, `ablage_loeschen`, `ablage_datei_speichern` (ChatGPT-Dateien, https-Adresse, Text, kleines Base64), `in_ablage_uebernehmen` (vorläufiges Ergebnis behalten). `ablage_zeigen` zeigt eine Datei als Karte (Bild, PDF-Seite, Text), vor allem im ChatGPT-Gespräch.
- Gestaltetes PDF (Titelseite, Inhalt, Merkkästen, Tabellen, Abbildungen) nur auf ausdrücklichen Wunsch; `pdf_entwurf_pruefen` setzt probeweise. Recherchen sonst als formatierter Chattext.
- `bild_erzeugen`: Bild oder Infografik als PNG, auf Wunsch DIN-A4-PDF.
- In der App: Vorschau für Text, Code, Tabellen, Bilder, PDF (ohne Textsuche), Office als Textauszug, Audio, Video; Filter, Suche, Ansichten, Mehrfachauswahl, Teilen, Download.

## Agenten und Hintergrundaufträge

- `agenten` (ansehen, anlegen, löschen), `agent_starten` (kehrt sofort mit Vorgangskennung zurück); vorgegeben: Recherche, Machbarkeit, Code-Analyse (nur lesend). Agenten, allen voran Recherche, starten nur, wenn Frank ausdrücklich eine Recherche oder den Agenten verlangt oder einem Angebot dazu zustimmt (im Chat und Live an seinen Worten geprüft; über ChatGPT/MCP nur per Anweisung). Höchstens 2 gleichzeitig; Agenten starten keine Agenten und keine Programmieraufträge.
- `vorgaenge` (Liste, Ergebnis lesen, als mitgeteilt bestätigen), `vorgang_abbrechen`.

## Programmieren

- `programmieren`: übergibt einen Auftrag an Claude Code, zuerst an die PC-Brücke (Windows-PC, sonst Mac als Ersatz), sonst an die Cloud-Routine; auf Wunsch mit Code-Review. Jarvis programmiert nie selbst.
- `pc_status` (Windows-PC und Mac erreichbar?), `programmierauftraege` (Stand, Link), `programmier_antwort` (Rückfragen beantworten, auch per Knopf im Vollbild), `programmier_nachtrag` (nur auf ausdrücklichen Wunsch in die genannte Sitzung).
- Fortschritt der Sitzung erscheint im Ursprungsgespräch; Benachrichtigung bei Rückfrage, fertig und Fehler. Fertig gemeldet heißt nicht installiert.
- Jedes Gespräch mit Programmierauftrag (eigenes Auftragsgespräch, ein mit „Neuer Programmierauftrag“ oder „Programmierer“ neu begonnenes Gespräch oder ein normales Gespräch, in dem der Auftrag selbst läuft) ist dauerhaft als Programmiergespräch markiert und steht nur im Leistenreiter „Programmierung“; der Bestand wird beim Laden an Auftragsbuch, Auftrags-Vorspann und Sitzungsmeldungen erkannt und nachmarkiert.

## Sprache

- Diktat und Spracheingabe über Groq Whisper (eigener Schlüssel nötig), mit Spracherkennung des Endes; kein Offline-Diktat.
- Vorlesen mit Microsoft Edge, Google Chirp 3 HD, Alibaba Qwen oder geklonter eigener Stimme; Tempo, Probe, Medien-Steuerung (Pause/Weiter/Stopp). Keine Android-Systemstimme.
- „Hey Jarvis“ oder eigene englische Weckphrase, auch bei gesperrtem Handy; Empfindlichkeit; „Nur auf meine Stimme hören“ mit eingesprochenen Proben. Startet die klassische Schleife (Diktat → Modell → Vorlesen), nicht das Live-Gespräch.
- `sprachsession_beenden`: beendet Sprachsession und Live-Gespräch.
- Andere eigene Apps (z. B. UpdateStation) können Ansagen vorlesen lassen.

## Live-Gespräch

- Gleichzeitig zuhören und sprechen über OpenAI GPT-Live, OpenAI Realtime oder Google Gemini Live (mit eigenen API-Schlüsseln, nicht über das ChatGPT-Abo); Fragen an Jarvis gehen über ein Werkzeug an den Jarvis-Agenten.
- Schnellwahl-Profile, Stimmen mit Hörprobe, Kostenschätzung und Kostenbuch; Ton bevorzugt über Headset oder Auto; Medientasten werden währenddessen abgefangen.
- Echosperre im Medienton: Mikrofon zu, solange Jarvis zu hören ist; über Bluetooth (Auto) wartet sie die gemessene Ausgabelatenz ab (mindestens 1 s), damit das Ende der eigenen Antwort nicht als Franks Worte zurückkommt. Je Gemini-Beitrag eine Logbuch-Zeile „Live-Mitschrift (Gemini)“ (Sperrzustand, Latenz, Echoverdacht, ohne Inhalt).
- Gemini „Jarvis direkt“ (Standard bei Gemini, Profil „Gemini 3.8 Live“): Die App selbst gibt jeden abgeschlossenen Beitrag mit Franks erkannten Worten (endgültige Eingabe-Mitschrift, auch Smalltalk und bloße Mitteilungen) genau einmal an den Jarvis-Agenten, auch wenn Gemini schweigt oder selbst antworten will; hörbar ist Jarvis' Antwort, die Gemini natürlich in eigenen Worten ausformulieren darf (Fakten, Zeiten, Bestätigungen aus Jarvis' Antwort; vor dem Abspielen nicht prüfbar), Geminis eigene Sachantworten bleiben stumm. Spricht Frank weiter, bevor eine Antwort gesprochen ist, geht sie mit dem neuen Beitrag an Jarvis statt vorgelesen zu werden; Jarvis-Ausfall oder nicht vorgelesene Antworten werden gemeldet (Antwort dann im Chat). Bis Jarvis antwortet, höchstens ein kurzes neutrales Signal je Antwort („Mhm“, „Okay“, „Alles klar“, „Moment“, „Ich schau nach“ …): Die App puffert Geminis eigenen Ton und spielt ihn nur, wenn die Mitschrift ein solches Signal ist, nie dasselbe zweimal hintereinander; Ja/Nein, „Klar“, Erledigungen, Inhalt, zu Langes, Äußerungen ohne Mitschrift und von Franks Weitersprechen, Unterbrechen oder der fertigen Antwort überholte Signale bleiben stumm. Keine Wortgarantie: Erkennungsfehler der Mitschrift möglich.

## Oberfläche

- Reiter: Jarvis (Chat, Tagesauswertung, Live-Gedächtnis, Einrichtungs-Checkliste), Ablage, Aktivität (Protokoll), Einstellungen (Jarvis, Benachrichtigungen, Stimme, Alltag und Timer, Konten und Apps, System).
- Gesprächsleiste mit gespeicherten Gesprächen; Wechsel per Sprache („neue Session“, „letzte Session“) ohne Modell. Oben Reiter „Gespräche“, „Programmierung“ und „Projekte“. „Programmierung“: alle Programmiergespräche nach Zeit gruppiert, „Neuer Programmierauftrag“ öffnet ein neues Gespräch mit dem Programmierer-Vorspann im Feld; diese Gespräche stehen nicht unter „Gespräche“ (Projektgespräche mit Auftrag zusätzlich in ihrem Projekt). „Projekte“: „Neues Projekt“ legt ein benanntes Projekt an (dauerhaft gespeichert); Antippen öffnet seinen Bereich mit „Neues Gespräch im Projekt“ und nur dessen Gesprächen. Projektgespräche stehen nicht unter „Gespräche“. Projekte umbenennen oder löschen gibt es nicht.
- Unter jeder Antwort: Kopieren, Daumen hoch/runter (daraus lernt die Stil-Datei), Vorlesen, Teilen, in neuem Chat fortsetzen, erneut versuchen.
- Eingabe: Text, Kamera, Fotos, Dateien, Diktat, Modellwahl, Live-Gespräch. Plus-Menü „Plugins“ ist noch leer.
- Plus-Menü „Programmierer“: setzt „Übergib bitte folgende Aufgabe an den Programmierer:“ ins Feld (Cursor am Ende, sendet nichts); abgeschickt geht die Aufgabe als neuer Auftrag über `programmieren`, ohne Aufgabe startet nichts.
- Benachrichtigungen je Art einstellbar (Ton, wie ein Wecker, Vorlesen, Lautstärke), Ruhezeit; schwebende Sprachblase bei Hey Jarvis aus anderen Apps.
- Fingerabdruck-Sperre, Hell/Dunkel. Kein Widget, keine Schnellkachel, kein Teilen an Jarvis aus anderen Apps.
