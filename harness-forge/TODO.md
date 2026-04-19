# Harness Forge — Implementierungsplan (Swift 6)

> **14-Schritte-Plan nach dem "Build to Delete"-Prinzip.**
> Jeder Schritt liefert einen lauffaehigen Teilstand — abbrechbar, testbar, rueckbaubar.
> Plattform: macOS 14+, Swift 6.0, Swift Package Manager.

---

## Fortschritts-Uebersicht

```
Gesamt:  [##............................]   7%   (1 von 14 Schritten)

Step 1:  [##############################] 100%   Projektstruktur & Swift-Package-Manifest
Step 2:  [..............................]   0%   Kern: LLMClient-Protokoll + Backends
Step 3:  [..............................]   0%   Trajektorien- & Lessons-Persistenz (SwiftData)
Step 4:  [..............................]   0%   Die 6 Harness-Schichten als Swift-Module
Step 5:  [..............................]   0%   Task-Analyzer (Herzstueck)
Step 6:  [..............................]   0%   Builder: Pure Prompt
Step 7:  [..............................]   0%   Builder: Python CLI
Step 8:  [..............................]   0%   Builder: Tauri Desktop
Step 9:  [..............................]   0%   Builder: Android Kotlin
Step 10: [..............................]   0%   Builder: Claude Subagent
Step 11: [..............................]   0%   Prompt-Editor mit Git-Versioning
Step 12: [..............................]   0%   CLI-Frontend (swift-argument-parser)
Step 13: [..............................]   0%   SwiftUI-App (macOS 14+)
Step 14: [..............................]   0%   End-to-End-Test & Dogfooding
```

---

## Step 1 — Projektstruktur & Swift-Package-Manifest  `[ERLEDIGT]`

Grundgeruest anlegen, das alle folgenden Schritte tragen kann.

- [x] `Package.swift` — SPM-Manifest, Swift 6.0, macOS 14+
- [x] `AGENTS.md` — die Verfassung auf Deutsch
- [x] `README.md` mit Mermaid-Architektur-Diagramm
- [x] `.gitignore` inkl. `.forge/prompts.git`- und `.build/`-Ausschluss
- [x] `.swift-version` — 6.0
- [x] Modul-Struktur: `Sources/{HarnessForgeCore, HarnessForgeLayers, HarnessForgeBuilders, ForgeCLI, HarnessForgeApp}`
- [x] Test-Struktur: `Tests/{HarnessForgeCoreTests, HarnessForgeLayersTests, HarnessForgeBuildersTests}`

**Pruefung**: `swift build` muss ohne Fehler durchlaufen. `swift test` muss den Smoke-Test gruen ausgeben.

---

## Step 2 — Kern: LLMClient-Protokoll + Backends  `[OFFEN]`

Einheitliche Schnittstelle zu allen KI-Backends via URLSession.

- [ ] `HarnessForgeCore/LLMClient.swift` — `protocol LLMClient` mit async/await API
- [ ] `HarnessForgeCore/Models/ChatMessage.swift` — Codable-Model
- [ ] `HarnessForgeCore/Models/LLMResponse.swift` — inkl. Token-/Kosten-Metriken
- [ ] `HarnessForgeCore/Backends/AnthropicBackend.swift`
- [ ] `HarnessForgeCore/Backends/OpenAIBackend.swift`
- [ ] `HarnessForgeCore/Backends/GeminiBackend.swift`
- [ ] `HarnessForgeCore/Backends/LMStudioBackend.swift`
- [ ] `HarnessForgeCore/Router.swift` — waehlt Backend nach Task-Typ + Kostenbudget
- [ ] `HarnessForgeCore/KeychainStorage.swift` — API-Keys in macOS Keychain
- [ ] `Tests/HarnessForgeCoreTests/LLMClientTests.swift` — mit URLProtocol-Mocks
- [ ] `Tests/HarnessForgeCoreTests/RouterTests.swift`

**Dataflow**: `Router.chat(task: Task) → LLMClient.send(messages: [ChatMessage]) → LLMResponse`.

---

## Step 3 — Trajektorien- & Lessons-Persistenz  `[OFFEN]`

Jede Interaktion wird zum Dataset fuer spaeteres Fine-Tuning.

- [ ] `HarnessForgeCore/Persistence/TrajectoryStore.swift` — SwiftData `@Model`
- [ ] `HarnessForgeCore/Persistence/Trajectory.swift` — Root-Entity
- [ ] `HarnessForgeCore/Persistence/Interaction.swift` — einzelne Tool-Calls
- [ ] `HarnessForgeCore/Persistence/LessonsDB.swift` — gelernte Fehlerregeln
- [ ] `HarnessForgeCore/Reflection/ReflectionLoop.swift` — alle N Interaktionen
- [ ] `Tests/HarnessForgeCoreTests/TrajectoryStoreTests.swift`

---

## Step 4 — Die 6 Harness-Schichten als Swift-Module  `[OFFEN]`

Die fundamentalen Bausteine, die jeder generierte Harness bekommt.

- [ ] `HarnessForgeLayers/Constraint/ConstraintLayer.swift` — L1: Linter, Schema-Validierung
- [ ] `HarnessForgeLayers/Context/ContextLayer.swift` — L2: AGENTS.md-Loader, Progressive Disclosure
- [ ] `HarnessForgeLayers/Execution/ExecutionLayer.swift` — L3: Tool-Registry, Sandbox
- [ ] `HarnessForgeLayers/Verification/VerificationLayer.swift` — L4: Silent-Success-Hooks
- [ ] `HarnessForgeLayers/Lifecycle/LifecycleLayer.swift` — L5: Loop-Detection, Cost-Tracking
- [ ] `HarnessForgeLayers/Meta/MetaLayer.swift` — Meta: Hyperagent, Skill-Extraktion
- [ ] `Tests/HarnessForgeLayersTests/` — pro Schicht eigene Testsuite

---

## Step 5 — Task-Analyzer (Herzstueck)  `[OFFEN]`

Nimmt die Nutzeraufgabe entgegen, recherchiert, entscheidet Harness-Form.

- [ ] `HarnessForgeBuilders/TaskAnalyzer/TaskAnalyzer.swift` — 4-stufiger Pipeline
- [ ] `HarnessForgeBuilders/TaskAnalyzer/WebSearchTool.swift` — Tavily/Brave Search API
- [ ] `HarnessForgeBuilders/TaskAnalyzer/DecisionMatrix.swift` — 5 Typen x 6 Achsen
- [ ] `HarnessForgeBuilders/TaskAnalyzer/SlugGenerator.swift` — Task → `kebab-case-name`
- [ ] `Tests/HarnessForgeBuildersTests/TaskAnalyzerTests.swift`

---

## Step 6 — Builder: Pure Prompt  `[OFFEN]`

Einfachster Builder — generiert optimierten Markdown-Prompt.

- [ ] `HarnessForgeBuilders/PurePromptBuilder/PurePromptBuilder.swift`
- [ ] Template-Dateien als `Resources/` im Target

---

## Step 7 — Builder: Python CLI  `[OFFEN]`

Generiert vollstaendiges Python-Projekt mit Typer + Rich.

- [ ] `HarnessForgeBuilders/PythonCLIBuilder/PythonCLIBuilder.swift`
- [ ] Template-Dateien: `pyproject.toml`, `__main__.py`, README

---

## Step 8 — Builder: Tauri Desktop  `[OFFEN]`

Generiert Tauri-v2-Projekt (Rust-Backend + Svelte-Frontend).

- [ ] `HarnessForgeBuilders/TauriDesktopBuilder/TauriDesktopBuilder.swift`

---

## Step 9 — Builder: Android Kotlin  `[OFFEN]`

Generiert vollstaendiges Android-Studio-Projekt mit Compose + Material 3.

- [ ] `HarnessForgeBuilders/AndroidKotlinBuilder/AndroidKotlinBuilder.swift`

---

## Step 10 — Builder: Claude Subagent  `[OFFEN]`

Generiert Claude-Code-Subagent + Skill-Bundle.

- [ ] `HarnessForgeBuilders/ClaudeSubagentBuilder/ClaudeSubagentBuilder.swift`

---

## Step 11 — Prompt-Editor mit Git-Versioning  `[OFFEN]`

Visueller Editor fuer alle System-Prompts, mit automatischer Versionierung.

- [ ] `HarnessForgeCore/PromptEditor/PromptVersioning.swift` — Shell-Calls an `git`
- [ ] `HarnessForgeCore/PromptEditor/DiffViewer.swift`
- [ ] `HarnessForgeCore/PromptEditor/RollbackEngine.swift`

---

## Step 12 — CLI-Frontend  `[OFFEN]`

swift-argument-parser-basiertes Terminal-Interface.

- [ ] `ForgeCLI/ForgeApp.swift` — `@main`
- [ ] `ForgeCLI/Commands/NewCommand.swift` — `forge new "<task>"`
- [ ] `ForgeCLI/Commands/ListCommand.swift`
- [ ] `ForgeCLI/Commands/EditCommand.swift`
- [ ] `ForgeCLI/Commands/RollbackCommand.swift`
- [ ] `ForgeCLI/Wizard/InteractiveWizard.swift`

---

## Step 13 — SwiftUI-App  `[OFFEN]`

macOS-native Desktop-App.

- [ ] `HarnessForgeApp/HarnessForgeApp.swift` — `@main`
- [ ] `HarnessForgeApp/Views/SidebarView.swift` — Liste aller Harnesses
- [ ] `HarnessForgeApp/Views/TaskInputView.swift` — Grosses Textfeld
- [ ] `HarnessForgeApp/Views/ReasoningStreamView.swift` — Live-Stream
- [ ] `HarnessForgeApp/Views/DecisionMatrixView.swift`
- [ ] `HarnessForgeApp/Views/SettingsView.swift` — API-Keys (Keychain)
- [ ] App-Bundle-Build-Strategie: `swift-bundler` oder Shell-Script

---

## Step 14 — End-to-End-Test & Dogfooding  `[OFFEN]`

Der Beweis: Harness Forge baut seinen ersten echten Harness.

- [ ] Testaufgabe: "Baue mir einen Harness fuer einen Schweden-Packrafting-Trip-Planer"
- [ ] Alle Phasen durchlaufen: Analyse → Recherche → Matrix → Empfehlung → Generierung
- [ ] Generierten Harness testen
- [ ] Auto-Commit + Push nach `Pepsi1978/proggs` als `~/proggs/<slug>/`
- [ ] Dokumentation in `docs/first-harness.md`

---

## Meta: Was nach Step 14 kommt

| Phase | Ziel |
|-------|------|
| Phase 2 | Reflection-Loop produziert erste automatische Skill-Extraktion |
| Phase 3 | Plugin-Marketplace fuer Community-Plugins |
| Phase 4 | Export-Funktion: Trajektorien → Fine-Tuning-Dataset (MLX-Format) |
| Phase 5 | Multi-Agent-Orchestrierung: mehrere Harnesses reden untereinander |
