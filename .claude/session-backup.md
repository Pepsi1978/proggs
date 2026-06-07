# Session Handoff — 2026-06-07 ~22:45

## Ziel
Franks eigenes sprachgesteuertes Agent-System "VoiceAgent" (WPF/.NET 10, Windows-only,
~/proggs/VoiceAgent/) iterativ ausbauen und Bugs fixen. Frank testet die App live (publish/VoiceAgent.exe)
und gibt Feature-Wuensche per Voice/Text.

## Aktueller Status
- ALLES committet+gepusht+gebaut+getestet (54 Tests gruen). publish/VoiceAgent.exe ist aktuell.
- Heute gebaut (Commits #41615-#41629):
  - Observability-Schicht: JSON-Lines-Log (Diagnostics/Log.cs), Probe-Sonden (Probe.cs), globaler
    Fehler-Faenger (App.xaml.cs, DispatcherUnhandled e.Handled=true), Live-Turn-Trace (TurnTrace.cs,
    turn:N-Korrelation), In-App Log-Viewer (Views/LogViewerWindow, "Live-Log"-Button).
  - LLM-Intent-Detektor (Core/IntentDetector.cs: AUFGABE/FRAGE/PLAUDEREI, eigenes billiges Gemini,
    method:llm; klassifiziert VOR der Antwort).
  - Voice-Empfindlichkeit als Schieberegler in Settings (+ RebuildListener uebernimmt live).
  - Gedaechtnis Stufe 1 (Core/AgentMemory.cs: Fakten + letzte 12 Runden, JSON, in System-Prompt).
  - Unteragenten-Grundgeruest (ISubAgent/SubAgentRegistry; NoteSubAgent + ReminderSubAgent;
    BossAgent.HandleAsync delegiert bei Intent=Task, Trace-Zeile DELEGIERT).
  - Uhr oben im Fenster; farbige Gespraechsanzeige (RichTextBox, Du=Cyan/Agent=Orange, in Settings
    via ColorPalette waehlbar); Zeitzone in Settings (auto/manuell).
  - Proaktive Erinnerung + Enterprise-Sound (Services/Audio/Chime.cs NAudio-Zweiton). ZWEI-SCHRITT-
    Dialog: bei Faelligkeit ertoent der Chime und WIEDERHOLT sich jede Minute (Mic an, KEINE Ansage),
    bis Frank reagiert; seine naechste Aeusserung (Voice/Text) loest erst die Ansage aus
    (MainWindow: _pendingReminder, _reminderPingTimer, TryStartReminderPing/StartPinging/
    OnReminderPing/StopPinging/AnnounceReminderAsync; Queue fuer mehrere faellige).
  - SELBST-AUSKUNFT: Core/AgentCapabilities.cs liest externe src/VoiceAgent/capabilities.md (Content,
    liegt neben EXE, beim Start gecacht). Nur bei "kannst du...?"-Fragen (IsCapabilityQuestion) in
    den Prompt — nicht bei jedem Turn (Kontext sparen).
  - Fixes: Settings-Crash (NullRef in Slider-ValueChanged waehrend XAML-Load -> _ready-Guard,
    Almanach dotnet-csharp 2.10); LLM-Zeit-Halluzination (Core/TimeContext.cs faedelt Echtzeit+Zeitzone
    in jeden System-Prompt); BossAgentTests-Regression (StartsWith statt Equal).
- In Arbeit: NICHTS offen. Alle Aufgaben abgeschlossen.

## Relevante Dateien
- VoiceAgent/src/VoiceAgent/MainWindow.xaml(.cs) — Voice-Loop, Uhr, Farben (Append/RichTextBox),
  Reminder-Zwei-Schritt-Flow, Clock+Reminder-Timer.
- Core/: BossAgent (HandleAsync+Memory+Capabilities+Time), IntentDetector, EndpointDetector,
  AgentMemory, ReminderService, ReminderSubAgent, NoteSubAgent, AgentCapabilities, TimeContext, ColorPalette.
- Diagnostics/: Log, Probe, TurnTrace, LogFormatter.
- Services/: Config, AppSettings, GroqWhisperClient, Llm/(Gemini/Claude/OpenAi/Factory), GoogleTtsClient,
  Audio/(AlwaysOnListener, AudioPlayer, Chime).
- Views/: SettingsWindow, LogViewerWindow.
- src/VoiceAgent/capabilities.md — Faehigkeiten-Datei (PFLEGE-REGEL: bei JEDEM Feature aktualisieren!).
- docs/2026-06-07-hauptagent-design.md = Manifest. Keys: ~/SK/VoiceAgent/keys.json.

## Getroffene Entscheidungen
- .NET 10, WPF, auf main (kein Branch). Grosse Features bewusst als "Stufe 1" (ehrlich kommuniziert):
  Gedaechtnis ohne Vektor-Retrieval; Unteragenten ohne Computer Use.
- LLM kennt Uhrzeit UND eigene Faehigkeiten NICHT von sich aus -> beides per Kontext-Block in den
  System-Prompt (TimeContext IMMER; AgentCapabilities NUR bei Frage, kontextschonend = lossless).
- Bei .NET test-vor-commit (siehe Fehlgeschlagene Ansaetze).

## Fehlgeschlagene Ansaetze (NICHT wiederholen)
- commit-before-build strikt befolgt -> 2x rote Commits gepusht (BossAgentTests: System-Prompt um
  Zeit-/Faehigkeiten-Block erweitert, aber Tests pruefen mit Assert.Equal exakt -> brach).
  LEHRE: Bei VoiceAgent (.NET, Solo-Session, kein Deploy) `dotnet test` LOKAL VOR dem Commit, dann
  committen+pushen. Die commit-before-build-Regel zielt auf parallele Android-AAB-Builds; .NET-Desktop
  ist davon nicht betroffen. (Offener Harness-Vorschlag, von Frank noch nicht bestaetigt.)
- Bei System-Prompt-Aenderungen IMMER pruefen, ob BossAgentTests (Equal auf msgs[0].Text) brechen ->
  StartsWith nutzen.

## Wichtige Recherche-Ergebnisse
- WPF: Slider/CheckBox/ComboBox-Event-Handler feuern WAEHREND InitializeComponent (Property-Coercion).
  Guard mit _ready-Flag (nicht nur ein Control auf null pruefen). -> Almanach bugs/desktop/dotnet-csharp.md 2.10.
- Single-File: capabilities.md neben der EXE via Environment.ProcessPath finden (nicht Assembly.Location).
- NAudio Chime: SignalGenerator + .Take(TimeSpan) + ConcatenatingSampleProvider + WaveOutEvent (Feld halten).

## Naechste Schritte (priorisiert)
1. (offener Intelligenz-Vorschlag 1) Mic waehrend des Chime (~300ms) kurz muten, damit der Chime sich
   nicht selbst als Reaktion triggern kann (Zwei-Schritt-Dialog "zu 100%").
2. (offener Intelligenz-Vorschlag 2) capabilities.md-Pflege als kurze VoiceAgent-Projektregel festschreiben.
3. Reminder-Liste in der App (anstehende sehen/loeschen); wiederkehrende Erinnerungen.
4. Latenz: Endpoint- + Intent-Check in EINEN Gemini-Call buendeln (aktuell bis 3 Calls/Turn).
5. Grosse Stufen: Gedaechtnis Stufe 2 (Vektor-Retrieval, Hermes-Niveau); Unteragenten Stufe 2 (Computer Use, abgesichert).

## Offene Fragen
- Frank wollte live testen (kannst du... / Uhrzeit / Erinnerung). Die 2 Intelligenz-Vorschlaege
  (Mic-Mute, Pflege-Regel) warten auf Franks Zustimmung.

## Anker
- Branch: main
- Letzte Commits:
4e93b9de #41629 - VoiceAgent: two-step reminder dialog (chime repeats every min until reaction -> readout)
7118cf07 #41628 - VoiceAgent: agent self-awareness via external capabilities.md (only on "kannst du" asks)
a3cb3ba8 #41627 - VoiceAgent: proactive reminders stage 1 (ReminderSubAgent + chime + proactive TTS)
88b5252d #41626 - VoiceAgent: color-coded conversation (RichTextBox), you=cyan/agent=orange, selectable
b5b779f7 #41625 - VoiceAgent: live clock in main window header
