# Best-Practices — Projekt-Code

Diese Sektion ergaenzt die Harness-Kategorien (`01-hooks` … `12-neues`) um Best-Practices
fuer die **Software/Sprachen, die in den Projekten benutzt werden** — Kotlin, Swift,
Gradle, .NET/WPF, TypeScript, Rust usw. Sie ist die zweite Seite der Medaille zum
Bug-Almanach (`~/proggs/bugs/`): der Almanach sammelt *was schiefgeht und wie man es
umgeht*, diese Sektion sammelt *wie man es von vornherein richtig macht, damit der Bug
gar nicht erst entsteht*.

## Wer schreibt hier rein

- **`bug-almanach-recherche`-Skill:** traegt bei jeder Bug-Recherche die allgemeingueltige
  Praevention/Best-Practice einer gefundenen Loesung hier ein (gezielt, pro Bug).
- **`best-practices`-Skill:** kann eine ganze Software gezielt aufrollen
  ("recherchiere Best-Practices nur fuer Kotlin") und die neuesten Empfehlungen pflegen.

## Struktur (seit 2026-06-03: nach Kategorie gruppiert, selbst-identifizierende Dateinamen)

Jede Best-Practice-Datei liegt **direkt im Kategorie-Ordner** und traegt den Software-Namen
**im Dateinamen** (`best-practices-<software>.md`) — kein generischer `best-practices.md`
mehr und kein Software-Unterordner. So ist sofort am Dateinamen erkennbar, worum es geht,
auch wenn eine Datei mal woandershin geraet. Dieselben Kategorien wie der Bug-Almanach
(`bugs/<kategorie>/`), damit beide Seiten der Medaille deckungsgleich sind. Ein blosser
Kategorie-Wechsel ist unkritisch: `check-coupling.py` paart ueber den Software-Namen
(aus dem Dateinamen, rekursiv gesucht), nicht ueber die Kategorie.

```
projekt-code/
├── README.md                                  ← diese Datei
├── agents/
│   └── best-practices-orchestrator-agent.md   ← Boss-/Orchestrator-Agent im Multi-Agenten-System (Stand 2026-06-09) — Gegenstueck zu bugs/agents/orchestrator-agent.md
├── android/
│   ├── best-practices-kotlin.md
│   ├── best-practices-jetpack-compose.md      ← Android-UI (Stand 2026-06-02, BOM 2025.01.01 / 2026.03.00)
│   ├── best-practices-android-platform.md     ← Framework/Platform-SDK (Stand 2026-06-02, targetSdk 36, Room 2.7.0, WorkManager 2.10.0)
│   └── best-practices-firebase-billing.md     ← Firebase + Play Billing (Stand 2026-06-02, Billing 7.1.1, Firebase BOM 34.11.0)
├── android-build/
│   ├── best-practices-gradle.md               ← Gradle/AGP/R8/KSP (Stand 2026-06-02)
│   └── best-practices-r8.md                    ← R8 Shrinker/Optimizer — Play-Store-Release-Fokus, A–J (Stand 2026-06-03, AGP 8.7.3 / 8.10.0)
├── apis/                                       ← LLM-/Provider-APIs (Stand 2026-06-09) — 1:1-Mirror zu bugs/apis/
│   ├── best-practices-api-integration-general.md  ← anbieteruebergreifend (Resilienz/Rate-Limit/SSE/Timeout/Secrets)
│   ├── best-practices-multi-provider.md        ← Provider-Gateway/Fallback-Architektur (ungepaart, kein Bug-Almanach)
│   ├── best-practices-openai-api.md            ← Responses API/Structured Outputs/Caching
│   ├── best-practices-anthropic-api.md         ← Messages API/Tool Use/Prompt Caching
│   ├── best-practices-google-gemini-api.md     ← generateContent/responseSchema/thinkingLevel
│   ├── best-practices-groq-api.md              ← OpenAI-Layer/Batch/Whisper
│   ├── best-practices-openrouter-api.md        ← Gateway/Provider-Routing/Fallback
│   ├── best-practices-xai-grok-api.md          ← grok-4.x/reasoning_effort/Live Search
│   ├── best-practices-mistral-api.md           ← json_schema/Codestral-FIM/prompt_cache_key
│   ├── best-practices-deepseek-api.md          ← deepseek-reasoner/reasoning_content/KV-Cache
│   ├── best-practices-local-openai-compatible.md  ← Ollama/llama.cpp/LM Studio/vLLM
│   ├── best-practices-other-llm-apis.md        ← Cohere/Together/Fireworks/Perplexity/Bedrock/Azure/Cerebras/Vertex/HF
│   ├── best-practices-oauth-device-code.md     ← OAuth-Geraetecode-Flow
│   └── best-practices-cli-impersonation-subscription-auth.md  ← CLI-Abo-Auth (ToS beachten)
├── desktop/
│   ├── best-practices-dotnet-csharp.md
│   ├── best-practices-swift-appkit.md         ← macOS Swift/AppKit Overlay-Apps (Stand 2026-06-02, Swift 6.3.2 / Xcode 26.5, macOS 13+)
│   ├── best-practices-3d-metal-scenekit-macos.md  ← 3D nativ macOS (Stand 2026-06-13, Metal 4 / macOS 26 Tahoe) — SceneKit soft-deprecated, RealityKit ist der Weg
│   ├── best-practices-3d-dotnet-directx-windows.md  ← 3D nativ Windows C#/.NET (Stand 2026-06-13, .NET 10 / Stride 4.3) — Stride/Silk.NET statt verwaistem Veldrid
│   ├── best-practices-3d-rust-wgpu-bevy.md     ← 3D cross-platform Rust (Stand 2026-06-13, Bevy 0.18 / wgpu 29.x)
│   └── best-practices-3d-godot.md              ← 3D cross-platform Godot (Stand 2026-06-13, Godot 4.6) — Forward+ Desktop, Mobile-Renderer Android
├── web/
│   ├── best-practices-chrome-extensions.md    ← Chrome/Edge MV3 (Stand 2026-06-02, Chrome 148)
│   ├── best-practices-typescript.md           ← TypeScript/Node.js (Stand 2026-06-03, Node 24.15.0 / TS 6.0.2 / npm 11.12.0 / Bun 1.3.11)
│   └── best-practices-3d-threejs-webgpu.md     ← 3D im Web/TS (Stand 2026-06-13, Three.js r842 / Babylon 9.2.1) — Tauri (Desktop) + Capacitor (Android)
├── assets/
│   ├── best-practices-icon-building.md
│   └── best-practices-3d-visual-quality.md     ← engine-übergreifende 3D-Optik (Stand 2026-06-13) — PBR/IBL/Tonemapping/PostFX/glTF, edel vs. billig
├── peripherie/
│   └── best-practices-stream-deck.md          ← Elgato Stream Deck Plugins (Stand 2026-06-03, Stream Deck 7.4.2 / @elgato/streamdeck 2.1.0 / SDKVersion 2+3)
└── claude-tooling/
    ├── best-practices-mcp-server.md           ← MCP-Server-Bau (Stand 2026-06-03, MCP TS-SDK 1.27.1/1.29.0, zod v4, Spec 2025-11-25) — Gegenstueck zu bugs/claude-tooling/mcp-server.md
    ├── best-practices-python-windows.md       ← Python auf Windows / Cross-Platform-Scripting (Stand 2026-06-02, CPython 3.13.13)
    ├── best-practices-cowork.md               ← Cowork-Desktop-App nutzen (Stand 2026-06-13, Research-Preview/GA) — noch ungepaart (kein Bug-Almanach)
    └── best-practices-claude-code-desktop-vs-cli.md  ← Claude Code Desktop-App vs. CLI: was geht, was nur in der CLI (Stand 2026-06-13, Desktop-Redesign 14.04.2026) — Gegenstück zu bugs/claude-tooling/claude-code-desktop-vs-cli.md
```

Vorhandene Kategorien & Dateien:
**agents** (-orchestrator-agent) ·
**android** (best-practices-kotlin, -jetpack-compose, -android-platform, -firebase-billing) ·
**android-build** (-gradle, -r8) ·
**apis** (-api-integration-general, -multi-provider, -openai-api, -anthropic-api, -google-gemini-api, -groq-api, -openrouter-api, -xai-grok-api, -mistral-api, -deepseek-api, -local-openai-compatible, -other-llm-apis, -oauth-device-code, -cli-impersonation-subscription-auth) ·
**desktop** (-dotnet-csharp, -swift-appkit, -3d-metal-scenekit-macos, -3d-dotnet-directx-windows, -3d-rust-wgpu-bevy, -3d-godot) ·
**web** (-chrome-extensions, -typescript, -3d-threejs-webgpu) ·
**assets** (-icon-building, -3d-visual-quality) ·
**peripherie** (-stream-deck) ·
**claude-tooling** (-mcp-server, -python-windows, -cowork, -claude-code-desktop-vs-cli)
(jede Datei mit Bezugs-Tabelle zum passenden Bug-Almanach in `~/proggs/bugs/<kategorie>/`).

Dateiname-Konvention: `best-practices-<software>.md`. Inhalt beginnt mit
`# <Software> — Best Practices (Stand JJJJ-MM-TT, Version X)`.

## Unterschied zu den Harness-Kategorien (wichtig)

| | Harness-Kategorien (01–12) | Projekt-Code (diese Sektion) |
|---|----------------------------|------------------------------|
| Thema | Claude-Code-Werkzeuge (Hooks, Skills, MCP, Settings …) | Software in den Projekten (Kotlin, Swift, Gradle …) |
| Changelog-Quelle | offizieller **Claude-Code**-Changelog (`update-changelog.ps1`) | der **eigene** Changelog der Software (Kotlin-Releases, Swift-Releases …) — KEIN Claude-Script |
| Versions-Anker | installierte Claude-Code-Version | live ermittelte Version der jeweiligen Software |

## Quellen-Rangordnung (wie im Rest des Ordners)

Offizielle Hersteller-Quelle (JetBrains/Kotlin, Apple/Swift, Gradle, Microsoft/.NET …) =
Grundwahrheit. Community/Blogs = gelabelte `extern`-Alternative, ueberstimmt nie das
Offizielle. Jeder Eintrag traegt Quelle + Datum + `offiziell`/`extern`-Flag.
