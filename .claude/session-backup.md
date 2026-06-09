# Session Handoff — 2026-06-09

## Ziel (1-3 Saetze)
Das GESAMTE Boss-/Orchestrator-Agenten-System in der App **VoiceAgent** (C#/.NET, `~/proggs/VoiceAgent/`)
grundlegend intelligent ueberarbeiten — alles korrekt verstehen + korrekt umsetzen, eigene Unteragenten
bauen, vordefinierte Agenten steuern, pro Aufgabe/Rolle ein eigenes Modell (in den Einstellungen
konfigurierbar). Strikt nach den Almanachen + Best-Practices fuer Orchestrator-Agenten.

## Aktueller Status
- Erledigt: Bug-Almanach `bugs/agents/orchestrator-agent.md` + Best-Practices
  `best-practices/projekt-code/agents/best-practices-orchestrator-agent.md` erstellt+erweitert (intern+extern, Sektion 8) — Commits #46663, #46664, #46665.
- Erledigt: VoiceAgent-Boss-Agent ANALYSIERT, Root Cause gefunden, vollstaendiger Resume-Prompt +
  Phasenplan + Architektur-Ist als Datei gespeichert — Commit #46666:
  `~/proggs/VoiceAgent/docs/ORCHESTRATOR-UEBERARBEITUNG-RESUME-PROMPT.md`.
- In Arbeit: Phase 1 der Ueberarbeitung — NOCH KEIN CODE GEAENDERT. Naechster Schritt ist der Start.
- Blockiert: nichts. Zwei Modell-Defaults stehen (Bau=Codex, Sprechen=Flash, Verstehen=stark);
  ALLE Modell-Zuweisungen sollen pro Rolle in den Einstellungen konfigurierbar werden (Frank-Wunsch).

## Relevante Dateien
- `~/proggs/VoiceAgent/docs/ORCHESTRATOR-UEBERARBEITUNG-RESUME-PROMPT.md` — DER Plan. Zuerst lesen, exakt danach arbeiten.
- `~/proggs/bugs/agents/orchestrator-agent.md` + `~/proggs/best-practices/projekt-code/agents/best-practices-orchestrator-agent.md` — Wissensgrundlage (PFLICHT lesen).
- `~/proggs/bugs/desktop/dotnet-csharp.md` + `~/proggs/best-practices/projekt-code/desktop/best-practices-dotnet-csharp.md` — Guard-Pflicht fuer `.cs`-Edits.
- VoiceAgent Core: `BossAgent.cs` (DecideRouteAsync Z.130-204), `BossAgentPrompt.cs`, `IntentDetector.cs`,
  `ConfirmationDetector.cs`, `SubAgentRouter.cs`, `ReminderSubAgent.cs`, `AgentBuilderSubAgent.cs`,
  `ComputerUseSubAgent.cs`, `ISubAgent.cs`, `ChatSession.cs`, `AgentMemory.cs`, `TimeContext.cs`.
- `Services/AppSettings.cs` (LlmProvider/LlmModel/CodexEffort/EndpointModel/IntentModel),
  `Services/Llm/LlmProviderFactory.cs`, `Services/Llm/CodexProvider.cs`, `Views/SettingsWindow.xaml(.cs)`,
  `MainWindow.xaml.cs` (Verdrahtung Z.~304-322).

## Getroffene Entscheidungen
- Modell-Rollen statt einem globalen Modell. Defaults: Agenten-Bau=Codex (ChatGPT-Abo), Sprechen=Flash
  (latenzkritisch), Verstehen/Orchestrieren=stark (NICHT Flash). Final pro Rolle in Settings einstellbar.
- Root Cause der schlechten Intelligenz = Gemini Flash-lite als Verstehens-Gehirn (Almanach 1.1/1.6).
- Funktionserhaltend (Direktive #3): bestehende Regex-Pfade als Fallback behalten, nichts wegwerfen.

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- Noch keine gescheiterten Code-Ansaetze (es wurde noch kein Code geaendert).
- NICHT: ReminderSubAgent ist NICHT der Hauptfehler — der ist eigentlich ok gebaut. Der Killer sitzt
  im BossAgent/ConfirmationDetector (Negation-mit-Korrektur -> faelschlich Abbruch) und darin, dass
  Flash das Verstehens-Gehirn ist. Nicht in die Reminder-Regex-Falle verrennen.

## Wichtige Recherche-Ergebnisse
- Transkript-Failure-Modes gegen Almanach gemappt: 5.5 (Negation-mit-Korrektur als Abbruch),
  1.3 (Read-back echot Rohtext), 1.7/1.8 (Regex-Slot-Extraktion bricht bei Sprache/Transkription),
  2.3 (Routing-Kollision "anlegen"->Helfer-Baumeister), 1.1/1.6 (schwaches Modell fuers Verstehen).
- CodexProvider existiert bereits (ChatGPT-Abo, reasoning.effort). AgentBuilder/ComputerUse sind aktuell
  hart auf billiges Gemini-Flash verdrahtet -> auf Modell-Rollen umstellen.

## Naechste Schritte (priorisiert)
1. Resume-Prompt-Datei lesen (`VoiceAgent/docs/ORCHESTRATOR-UEBERARBEITUNG-RESUME-PROMPT.md`) + die 4 Pflicht-Wissensdateien.
2. Phase 1 starten: `ModelRoles` in AppSettings + `LlmProviderFactory.CreateForRole(...)` + Settings-UI-Dropdowns
   pro Rolle; alle Hardcodes in MainWindow.xaml.cs darauf umstellen. Erst lauffaehiges Geruest, dann testen.
3. Strukturierter "Understand"-Schritt (LLM, JSON-Schema) auf dem Brain-Modell + Read-back des verstandenen Intents.
4. Negation-mit-Korrektur in BossAgent.DecideRouteAsync (No + neue Aufgabe -> als Korrektur re-routen).
5. Vor erster Datei-Aenderung kurzen Pre-Flight-Plan zeigen. Bauen NUR per `publish.ps1`, Version sichtbar
   hochzaehlen, commit+push pro Phase, Tests gruen halten.

## Offene Fragen
- Welches starke Modell wird Default fuers Verstehens-Gehirn (Codex vs. Claude)? Frank entscheidet final in Settings;
  als Default sinnvoll: Codex (Abo, keine API-Kosten). Beim Start kurz bestaetigen lassen.

## Anker
- Branch: main
- Letzte Commits:
6dcb2db68 #46666 - VoiceAgent: Resume-Prompt fuer komplette Boss-Agenten-Ueberarbeitung
f98c35e91 #46665 - TVO+CVO macOS: layer 3 per-segment audio alignment (Swift)
85abcc1bd #46664 - TVO+CVO Windows: layer 3 per-segment audio alignment + version 1.1.0
255faba87 #46665 - agents-Almanach + Best-Practices: EXTERNE/selbst gebaute Boss-Agenten (Sektion 8 / Teil 7)
4dca885f3 #46658 - VoiceAgent v1.0.2: alle 90 Toene in jedem Picker
