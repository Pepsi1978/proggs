# Session Handoff — 2026-06-07 ~20:15

## Ziel
Franks EIGENES sprachgesteuertes Agent-System "VoiceAgent" (Alternative zu Hermes Agent) bauen
und iterativ verbessern. WPF/.NET 10, Windows-only. Baustein 1 = Hauptagent ("Boss") mit voller
Voice-Erfahrung: Mic-immer-an -> Groq Whisper STT -> Gemini-Gehirn -> Google Chirp3-HD TTS.
Liegt unter ~/proggs/VoiceAgent/.

## Aktueller Status
- ERLEDIGT: Baustein 1 KOMPLETT (Etappe 0-6, Commits #41595-#41602): Geruest, Diagnose-Logging,
  Config (Keys im SK-Ordner), LLM-Schicht (Gemini default + Claude/OpenAI voll), BossAgent +
  System-Prompt, Google Chirp3-HD TTS (30 Stimmen) + NAudio-Wiedergabe, Groq-Whisper-STT +
  AlwaysOnListener (Dauer-Mic), Settings-UI, single-exe Release. 22 Unit-Tests gruen.
- Semantische Endpunkt-Erkennung (FERTIG/WEITER, eigenes billiges Gemini-Modell) #41604.
- Voice-Timing-Fix: Stille 900->3000ms + Pre-Roll 400ms gegen verschluckte Wortanfaenge #41606.
- App-Icon eingebunden #41608, schwarze Ecken -> transparent #41609.
- Icon-Building-Almanach erstellt (7 Researcher) #41611: bugs/assets/icon-building.md + README + best-practices.
- LIVE GETESTET von Frank (Keys in ~/SK/VoiceAgent/keys.json): Voice-Loop laeuft, Endpointing
  funktioniert, Icon ist jetzt RUND (bestaetigt). App laeuft als publish/VoiceAgent.exe.

## In Arbeit / Offen
- Observability-Logging-Schicht auf VOLLEN Standard heben (JSON-Sonden + globaler Fehler-Faenger).
  Aktuell nur einfaches Text-Log in Diagnostics/Log.cs. Neue Regel ~/.claude/rules/observability-first.md verlangt den vollen Standard.
- Voice-Empfindlichkeit (Pausen-Dauer, Stille-Schwelle) in die Settings-UI als Schieberegler holen.
- Regel known-bugs-before-coding.md verschaerfen: bei JEDEM Fehler in almanach-losem Bereich PROAKTIV
  (von selbst) Almanach anbieten. Frank-Korrektur 2026-06-07. Memory: feedback_proactive_almanac_creation.md.
  Danach nach ~/proggs/claude-code-setup/rules/ spiegeln.
- 2 offene Intelligenz-Vorschlaege: (a) bug-almanac-guard.{ps1,sh} um Icon-Datei-Muster (.ico/.icns/
  ic_launcher*.xml) ergaenzen; (b) restart-voiceagent.ps1 Helfer (beenden+publish+Get-CimInstance-Check).

## Relevante Dateien
- VoiceAgent/docs/2026-06-07-hauptagent-design.md + 2026-06-07-baustein1-plan.md — Spec + Plan.
- VoiceAgent/src/VoiceAgent/MainWindow.xaml.cs — Voice-Loop-Orchestrierung (Akkumulation, Endpoint, Sicherheitsnetz).
- VoiceAgent/src/VoiceAgent/Services/Audio/AlwaysOnListener.cs — Mic + Stille-Chunking + Pre-Roll.
- VoiceAgent/src/VoiceAgent/Core/EndpointDetector.cs — semantisches FERTIG/WEITER.
- VoiceAgent/src/VoiceAgent/Services/Llm/ — Gemini/Claude/OpenAI-Provider + Factory.
- VoiceAgent/publish.ps1 — single-exe Build. App-Icon: src/VoiceAgent/assets/voiceagent.ico.
- bugs/assets/icon-building.md — neuer Almanach (Win/.ico, Android, macOS).
- Keys: ~/SK/VoiceAgent/keys.json (NICHT im Repo). Settings: %LOCALAPPDATA%\VoiceAgent\settings.json
  (SilenceMs=3000, Stimme de-DE-Chirp3-HD-Sadachbia, EndpointModel gemini-2.5-flash-lite). Log: %LOCALAPPDATA%\VoiceAgent\logs\voiceagent.log.

## Getroffene Entscheidungen
- .NET 10 statt 8 (nur .NET 10 SDK installiert + entspricht language-rules). .slnx-Format.
- Endpoint-Check auf separatem, billigem Gemini-Modell (entkoppelt vom Haupt-Gehirn) — Franks Idee.
- Auf main gearbeitet, kein Branch/Worktree (Franks parallel-sessions-Regel).
- Frank-Praeferenzen: durchbauen OHNE Zwischenfragen; Diagnose-Logging IMMER einbauen (Memory feedback_always_build_in_logging);
  proaktiv Almanache erstellen (Memory feedback_proactive_almanac_creation).

## Fehlgeschlagene Ansaetze (NICHT wiederholen)
- Icon-Cache: ie4uinit -show / -ClearIconCache + .lnk-Neuerstellung am GLEICHEN .ico-Pfad -> bringt NICHTS.
  LOESUNG die WIRKT: IconLocation auf VoiceAgent.exe,0 (neuer Cache-Schluessel) + iconcache_*.db/thumbcache_*.db/
  IconCache.db loeschen (Explorer vorher beenden) + Explorer neu starten. Hat sofort gewirkt.
- Get-Process VoiceAgent findet die App UNZUVERLAESSIG -> immer Get-CimInstance Win32_Process -Filter "Name='VoiceAgent.exe'".
- Frisch gebaute single-file .exe startet langsam (entpackt) -> nach Start ~6-10s warten vor Prozess-Check.

## Wichtige Recherche-Ergebnisse
- bugs/assets/icon-building.md hat ALLE Icon-Antworten. Kernsatz: schwarze eckige Ecken = fehlendes
  Alpha in der Quelle (RGBA + Ecken Alpha=0). OS maskiert selbst (Android/iOS/macOS-Tahoe = volles Quadrat
  liefern); Windows .ico + klassisches macOS .icns = Form einbacken. Konverter flachen Alpha auf Schwarz ab
  (Pillow convert RGBA + sizes; ImageMagick kein -flatten/-background).
- WPF: ApplicationIcon (.exe) + Window.Icon (Fenster), Icon-Datei als Resource (nicht Content) sonst single-file-Bruch.

## Naechste Schritte (priorisiert)
1. Frank fragen, womit weiter: (a) Observability-Logging-Schicht vervollstaendigen [verbindliche neue Regel],
   (b) Voice-Empfindlichkeit in Einstellungen, oder (c) Baustein 2 (Unteragenten-System).
2. Regel known-bugs-before-coding.md um proaktiven Almanach-Anstoss verschaerfen + nach claude-code-setup/rules/ spiegeln.
3. Optional die 2 Intelligenz-Vorschlaege (Guard-Icon-Muster, restart-voiceagent.ps1).

## Offene Fragen
- Womit als Naechstes weiter (Observability / Voice-Einstellungen / Baustein 2)?
- Die 2 Intelligenz-Vorschlaege umsetzen?

## Anker
- Branch: main
- Letzte Commits:
bab52c22 #41611 - bugs: new assets/ category + icon-building almanac (Win/.ico, Android adaptive, macOS .icns) + best-practices, from 7-researcher sweep + VoiceAgent black-corners/cache incident
0915b78c #41610 - Neuer Bug-Almanach: Claude-Code-Konfiguration & Regeln (claude-config)
8e96b808 #41609 - VoiceAgent: icon - make black corners transparent (proper rounded icon, no square)
bbe4f226 #41608 - VoiceAgent: app icon (window + exe) from Frank's artwork (multi-resolution .ico)
7623239e #41607 - bug-almanac-guard: BP-Scan lazy machen (Performance-Debugging)
a55d77c2 #41606 - VoiceAgent: voice timing fix - 3s silence threshold + pre-roll buffer (no more swallowed word starts)
13eec06f #41605 - bug-almanac-guard: Freigabe-Meldung praezisieren (Korrektheits-Debugging)
7c64c14d #41604 - VoiceAgent: semantic endpointing (FERTIG/WEITER) — wait for thought-pauses, dedicated cheap Gemini model, configurable, 22 tests green
