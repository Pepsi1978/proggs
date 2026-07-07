# Resume-Prompt: Boss-Agenten-System in VoiceAgent komplett intelligent machen

> **Anleitung fuer Frank:** Beim naechsten Mal einfach den Block unter „=== PROMPT ZUM EINFUEGEN ==="
> komplett kopieren und als erste Nachricht einfuegen. Claude macht dann genau an dieser Stelle
> weiter und ueberarbeitet das gesamte Boss-Agenten-System nach deinen Vorstellungen.

Stand der Vorarbeit (2026-06-09): Diagnose + Architektur-Analyse + Phasenplan fertig, Modell-Rollen-
Entscheidung getroffen (alles in den Einstellungen konfigurierbar). Noch KEIN Code geaendert.

---

## === PROMPT ZUM EINFUEGEN ===

Wir machen weiter mit der kompletten Ueberarbeitung des Boss-/Orchestrator-Agenten-Systems in meiner
App **VoiceAgent** (C#/.NET, `~/proggs/VoiceAgent/`). Ziel: der Hauptagent soll WIRKLICH intelligent
werden — alles korrekt verstehen und korrekt umsetzen, wie ein echter Boss-Agent. Setze das nach den
Almanachen und Best-Practices um, die wir dafuer erstellt haben.

### PFLICHT ZUERST LESEN (in dieser Reihenfolge), bevor du Code anfasst
1. `~/proggs/bugs/agents/orchestrator-agent.md` (Bug-Almanach Boss-/Orchestrator-Agent, inkl. Sektion 8 „externe/selbst gebaute Agenten")
2. `~/proggs/best-practices/agents/orchestrator-agent.md` (Best-Practices, inkl. Teil 7)
3. `~/proggs/bugs/desktop/dotnet-csharp.md` + `~/proggs/best-practices/desktop/dotnet-csharp.md` (Guard-Pflicht fuer `.cs`-Edits)
Diese vier sind die Wissensgrundlage. Halte dich strikt daran (funktionserhaltend, Direktive #3).

### MEINE VISION (so soll der Boss-Agent arbeiten)
- Er versteht JEDE natuerlichsprachliche Aussage KORREKT und setzt sie KORREKT um (kein Raten, kein Rohtext-Echo).
- Er hat die Freiheit/Moeglichkeiten, Aufgaben perfekt umzusetzen — spaeter jede Aufgabe im GESAMTEN Windows-System zuverlaessig (Computer Use).
- Er kann zuverlaessig EIGENE Unteragenten bauen und nutzen.
- Er kann vordefinierte Agenten ansteuern und perfekt nutzen.
- Er kann fuer VERSCHIEDENE Aufgaben VERSCHIEDENE Modelle benutzen. WICHTIG: ich will das ALLES in den
  EINSTELLUNGEN selbst zuweisen koennen — fuer JEDE Aufgabe/Rolle des Hauptagenten ein eigenes Modell
  (Dropdown pro Rolle). Sinnvolle Defaults: Agenten-BAU -> Codex (ChatGPT-Abo), SPRECHEN -> Gemini Flash
  (latenzkritisch), VERSTEHEN/ORCHESTRIEREN -> ein starkes Modell (NICHT Flash). Aber final entscheide ich
  pro Rolle in den Settings.

### DIAGNOSE (warum die Intelligenz aktuell schlecht ist) — schon erarbeitet, gilt weiter
Wurzel: Das **Verstehens-/Orchestrierungs-Gehirn ist Gemini Flash-lite** (billigstes/schwaechstes Modell) —
genau wovor Almanach 1.1/1.6 warnt. Belegtes Transkript-Desaster und Zuordnung:
- „nein, mach es SO …" (Korrektur) wird als ABBRUCH missverstanden -> `ConfirmationDetector` erkennt nur
  „nein" -> `Decision.No` -> „Alles klar, dann lasse ich das." Die Korrektur wird weggeworfen. (Almanach 5.5)
- Read-back echot ROHTEXT statt des verstandenen Intents („Soll ich dich um 09:00 Uhr daran erinnern: <Rohsatz>?"). (Almanach 1.3)
- Reminder-Slot-Extraktion ist rein Regex (`ExtractReminderText`/`ParseDueTime`) -> bricht bei Sprache/Transkriptionsfehlern (z. B. „rogen" statt „morgen"); Zeit faellt still auf 09:00 zurueck. (Almanach 1.7/1.8)
- Routing-Kollision: „anlegen" triggert faelschlich den Helfer-Baumeister. (Almanach 2.3)
- Konversationskontext geht in den Sub-Schritten verloren (jeder Turn standalone). (Almanach 5.5)

### ARCHITEKTUR-IST (damit du nicht alles neu erkunden musst)
Projekt: `~/proggs/VoiceAgent/src/VoiceAgent/`, TargetFramework **net10.0-windows**, WPF.
Kern in `Core/`:
- `BossAgent.cs` — Orchestrator. `DecideRouteAsync` ist die Gate-Logik (Z. 130-204): 0) awaitingFollowup,
  1) offene Rueckfrage `_pending` -> `ConfirmationDetector.Interpret` Ja/Nein/Unclear, 2) neue Aufgabe ->
  `SubAgentRouter` (LLM) dann Stichwort-Fallback `_subAgents.Find`, 3) sonst Hauptagent selbst. Synchroner
  Pfad `HandleAsync` + Stream-Pfad `HandleStreamingAsync` teilen `DecideRouteAsync`.
- `BossAgentPrompt.cs` — System-Prompt (sehr knapp).
- `IntentDetector.cs` — LLM-Klassifikation in nur 3 Toepfe (AUFGABE/FRAGE/PLAUDEREI). Zu grob.
- `ConfirmationDetector.cs` — rein heuristisch (kein LLM); „nein" als erstes Token -> No (Killer-Bug bei Korrekturen).
- `SubAgentRouter.cs` — LLM waehlt Helfer per Beschreibung; Fallback Stichwort.
- `ReminderSubAgent.cs` — Regex-Slot-Extraktion; `ConfirmationQuestion` ist SYNC (`string`).
- `AgentBuilderSubAgent.cs` — baut zur Laufzeit neue Helfer (`SubAgentDefinition` via LLM-JSON). SOLL Codex nutzen.
- `ComputerUseSubAgent.cs`, `CommandGuard.cs` — Computer Use (Stufen Aus/Sicher/Vollzugriff).
- `PromptSubAgent.cs` (custom Agents), `NoteSubAgent.cs`, `SubAgentRegistry.cs`, `ISubAgent.cs` (Interface;
  `ConfirmationQuestion` SYNC, `HandlesOwnConfirmation`, `AwaitingFollowup` default false), `ChatSession.cs`,
  `AgentMemory.cs`, `SessionManager.cs`, `ContextCompressor.cs`, `TimeContext.cs`.
- Diagnostics: `Log.cs`, `Probe.cs`, `TurnTrace.cs`, `LogFormatter.cs` (Observability-First ist schon da — nutzen!).
Verdrahtung in `MainWindow.xaml.cs` (Z. ~304-322): `SubAgentRegistry` wird gebaut; `ReminderSubAgent`,
`NoteSubAgent`, custom `PromptSubAgent`, `AgentBuilderSubAgent` (aktuell hart `new GeminiProvider(..., IntentModel)`!),
`ComputerUseSubAgent` (auch hart Gemini), `SubAgentRouter`, `EndpointDetector`, `IntentDetector` registriert.
Modelle: `Services/AppSettings.cs` -> `LlmProvider` ("gemini"|"claude"|"openai"|"codex"), `LlmModel`
(Default `gemini-3.1-flash-lite`), `CodexEffort`, `EndpointModel`, `IntentModel` (beide Flash-lite).
`Services/Llm/`: `ILlmProvider`, `IStreamingLlmProvider`, `GeminiProvider`, `ClaudeProvider`, `OpenAiProvider`,
`CodexProvider` (ChatGPT-Abo via `chatgpt.com/backend-api/codex`, `reasoning.effort`), `LlmProviderFactory.Create(settings)`.
UI: `Views/SettingsWindow.xaml(.cs)` — hier muessen die neuen Modell-pro-Rolle-Dropdowns rein.
Tests: `~/proggs/VoiceAgent/tests/VoiceAgent.Tests/` (ReminderTests, SubAgentRouterTests, AgentCapabilitiesTests,
SessionManagerTests u. a.) — gruen halten / sinnvoll erweitern.

### PHASENPLAN (mit Commit+Push nach JEDER Phase, Version sichtbar hochzaehlen)
**Phase 1 — Verstehens-Gehirn + Modell-Rollen (groesster Hebel):**
- `ModelRoles`/`ModelRoleSettings`: pro Rolle ein Modell+Provider, in `AppSettings` persistiert und in
  `SettingsWindow` per Dropdown einstellbar. Rollen mindestens: Sprechen (Speech), Verstehen/Orchestrieren
  (Brain/Reasoning), Agenten-Bau (Builder), Computer-Use-Ableitung, Endpoint, Intent/Router. Defaults:
  Builder=Codex, Speech=Flash, Brain=stark (z. B. Codex oder Claude — Frank stellt final ein).
- `LlmProviderFactory.CreateForRole(settings, role)` statt nur globalem Create; alle Hardcodes in
  `MainWindow.xaml.cs` darauf umstellen (kein `new GeminiProvider(...)` mehr fuer Builder/ComputerUse/Router).
- Strukturierter „Understand"-Schritt (LLM mit JSON-Schema) auf dem Brain-Modell:
  `{kind, zielAgent?, inhalt, zeit?, confidence, rueckfrageNoetig}` — ersetzt die 3-Wort-Heuristik + ergaenzt
  die Reminder-Slot-Extraktion (Regex bleibt als Fallback, funktionserhaltend). Read-back formuliert den
  VERSTANDENEN Intent (nicht Rohtext).
- Negation-mit-Korrektur: in `BossAgent.DecideRouteAsync`, wenn `_pending != null` und Antwort „No" ist
  ABER zugleich eine neue Aufgabe enthaelt (intent==Task bzw. Understand erkennt neue Instruktion) ->
  als Korrektur behandeln (altes `_pending` verwerfen, korrigierte Aussage neu routen), NICHT abbrechen.
  Nur reines „nein/nein danke/lass" bricht ab.
- Konversationskontext (History/Summary) in Understand/Router/Reminder-Extraktion mitgeben.
**Phase 2 — Sub-Agenten-Bau + vordefinierte Agenten robust:**
- AgentBuilder ueber das Builder-Modell (Codex); strukturierte Definition robust; Routing-Kollision
  „anlegen"->Baumeister entschaerfen (LLM-Router Vorrang, Stichwort-Trigger enger).
- Vordefinierte/custom Agenten (`PromptSubAgent`) sauber ansteuern; klare disjunkte Beschreibungen.
**Phase 3 — Tool-Calling/Computer-Use-Haertung (Richtung „alles im Windows-System"):**
- Robuste Agent-Loop (Termination/Backstop, Tool-Args validieren, Tool-Wirkung verifizieren statt „done"
  glauben — Almanach 4.5/4.10), CommandGuard-Stufen, Least-Privilege. Schrittweise mehr Windows-Faehigkeiten.

### HARTE REGELN (Direktiven + Projektregeln)
- Funktionserhaltend (Direktive #3): nie Features entfernen; Regex-Pfade als Fallback behalten.
- Observability-First: neue Logik instrumentieren (Probe/Log/TurnTrace, Live-Logik-Sonden „erwartet vs. tatsaechlich").
- Commit+Push VOR Build; pro abgeschlossener Phase committen+pushen (fortlaufende #-Nummer); nur eigene Dateien stagen.
- Bauen NUR per `~/proggs/VoiceAgent/publish.ps1` (Frank nutzt `publish\VoiceAgent.exe`, ~80MB) — danach
  ist die neue Version fuer ihn sichtbar. Version sichtbar hochzaehlen (Regel feedback_version_bump_visible_always).
- Tests gruen halten (`dotnet test`), neue Logik testen.
- Deutsche Umlaute in allen deutschen Texten/UI-Strings. Keine Sampling-Parameter an Opus 4.8 (Almanach 4.11).
- Bei 3+ Datei-Aenderungen: kurzer Pre-Flight-Plan vorab.

### ERSTER SCHRITT JETZT
Beginne mit Phase 1. Lies zuerst die vier Pflichtdateien, dann `ChatSession.cs`, `AgentMemory.cs`,
`Services/Llm/LlmProviderFactory.cs`, `Services/Llm/CodexProvider.cs`, `Views/SettingsWindow.xaml(.cs)` und
`MainWindow.xaml.cs` (Verdrahtung), und setze dann das Modell-Rollen-System + den strukturierten
Understand-Schritt + die Negation-mit-Korrektur um. Zeig mir vor der ersten Datei-Aenderung kurz den
konkreten Pre-Flight-Plan (welche Dateien, was, Risiko), dann leg los.

## === ENDE PROMPT ===
