# Voice-Agent-Sprachpipeline (Spracheingabe → Verstehen → Sprachausgabe) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Fenster laeuft ab waehrend Nutzer spricht ⭐⭐ | Timer NUR im Idle ticken, ab Antwort-Ende zaehlen | §1.1 |
| 2 | Fertige Aufnahme im Schlaf still geloescht ⭐⭐ | Im Fenster begonnene Rede IMMER verarbeiten, nie verwerfen | §1.2 |
| 3 | Aufnahme bleibt endlos offen (Luefter/TV) ⭐⭐ | Harter Max-Deckel 15–30 s, dann finalisieren UND verarbeiten | §2.1 |
| 4 | Agent traege ODER schneidet im Satz ab | Stille 300–550 ms Dialog, 1000–2000 ms Diktat + semantisches Netz | §2.2 |
| 5 | Erste Antwort dauert viele Sekunden ⭐ | Zwischenschritte aufs kleinste Modell/niedrigsten Effort, parallelisieren | §4.1 |
| 6 | Follow-up/zweiter Befehl wird ignoriert | Fenster nach JEDER Antwort oeffnen, History kurz (3–5) | §1.3 |
| 7 | Fremdgespraech als Befehl, Session bricht ab | Wake-Word 3+ Silben, Confidence-Gate im Fenster | §1.4 |
| 8 | Leise Wort-Enden fehlen / Status flattert | Hysterese (Dual-Threshold), Frames 20–30 ms | §2.3 |
| 9 | Schwelle stimmt nach lauter TTS nicht mehr | AGC/Boost an 3 Stellen pruefen, relative Schwellen statt fester | §2.4 |
| 10 | DataAvailable versiegt still, Mikro taub | Watchdog: kein Event N s → Capture neu starten | §3.1 |
| 11 | AccessViolation/Deadlock bei Stop/USB-Abzug | Stop/Dispose serialisieren, NIE im Callback disposen | §3.2 |
| 12 | Nutzer kann Agent nicht unterbrechen | Partial Ducking statt Mute; echtes Barge-in braucht AEC | §5.1 |
| 13 | Klicks/Luecken zwischen Saetzen | EIN offener Output, BufferedWaveProvider, PCM statt MP3 | §4.3 |
| 14 | Einzelwort ("ja") in falscher Sprache | `language=de` IMMER explizit, nie Auto-Detect | §6.1 |
| 15 | Haengender STT friert Turn 60 s ein | Groq-Timeout auf 5–10 s, EIN statischer HttpClient | §6.2 |
| 16 | Diktat-Live-Vorschau ueberschreibt finale Fassung / springt im Feld ⭐⭐ | Vorschau getrennt vom Zielfeld; `previewActive`-Riegel: nach Stopp schreibt NUR die finale Engine | §7 |
| 17 | Kurzer Start-/Stoppton kommt Sekunden später oder stottert ⭐⭐ | Output dauerhaft offen halten; PCM puffern; Latenz nicht unter die Treibergrenze drücken | §4.4 |
| 18 | Stop-Klick friert das Overlay ein, Prozess lebt weiter ⭐⭐ | `StopRecording()` nie auf dem UI-Thread; Stop, Event-Wartezeit und Cleanup separat begrenzen | §3.2 |
| 19 | Mikrofon-Klick verpufft stumm, Log zeigt `BadDeviceId calling waveInOpen` ⭐⭐ | `waveInGetNumDevs`-Preflight VOR dem Start; Fehlstart IMMER sichtbar (rot + Ton + Tooltip + Tray); `WM_DEVICECHANGE` → Gerät weg/wieder da erkennen | §3.5 |
