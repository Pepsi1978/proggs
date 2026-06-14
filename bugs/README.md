# 🐛 INHALTSVERZEICHNIS BUGS

> **Der zentrale Bug-Almanach pro Technologie-Bereich.**
> Hier liegt fuer jeden Bereich, an dem gearbeitet wird, eine eigene `.md`-Datei mit
> den oeffentlich bekannten Bugs/Fallen **und ihren bewaehrten, funktionserhaltenden
> Loesungen**. Ziel: Bekannte Fehler werden VOR der Arbeit nachgeschlagen, statt
> hinterher teuer debuggt (Poka-Yoke Stufe 3). Das vollstaendige Systemverhalten
> steht in [`SYSTEM.md`](SYSTEM.md).

---

## Ordnerstruktur (seit 2026-06-03: nach Kategorien sortiert)

Die Almanache liegen in **Kategorie-Unterordnern** (`bugs/<kategorie>/<bereich>.md`),
gruppiert nach Software-Typ. Das haelt den wachsenden Bestand uebersichtlich. Die
beiden Such-/Schutz-Hooks (`bug-almanac-index`, `bug-almanac-guard`) und
`check-coupling.py` finden Almanache **rekursiv** — eine Datei darf jederzeit die
Kategorie wechseln, ohne dass ein Hook angefasst werden muss.

```
bugs/
├── README.md · SYSTEM.md · OFFENE-ALMANACHE-PROMPTS.md · check-coupling.py   (oben, kategorielos)
├── android/          Kotlin, Jetpack Compose, Android-Platform/SDK, Firebase/Billing, 3D (Filament/SceneView)
├── android-build/    Gradle/AGP, R8/ProGuard
├── desktop/          C#/.NET (Windows), Swift/AppKit (macOS), 3D (Metal/RealityKit, .NET/Stride, Rust wgpu/Bevy, Godot)
├── web/              Chrome-Erweiterungen, TypeScript/Node, 3D (Three.js/Babylon/WebGPU)
├── peripherie/       Elgato Stream-Deck-Plugin
├── claude-tooling/   Claude-Hooks, MCP-Server-Bau, Python (Windows-Scripting)
├── assets/           App-Icon-Building (Windows .ico, Android Adaptive, macOS .icns), 3D-Visuelle-Qualität (PBR/Licht/PostFX)
├── apis/             LLM-/HTTP-API-Integration + OAuth/Auth (OpenAI, Anthropic, Gemini, Groq, OAuth/Device-Code, ...)
└── agents/           Multi-Agenten-Systeme (Boss-/Orchestrator-Agent, Sub-Agent-Spawning, Intent, Tool-Calling)
```

Die Gegenseite (Best-Practices) spiegelt dieselben Kategorien:
`best-practices/projekt-code/<kategorie>/<software>/best-practices.md`.

---

## So funktioniert es (Kurzfassung)

1. **Vor** echter Arbeit an einem technischen Bereich: pruefen, ob es hier einen
   Almanach fuer den Bereich gibt (diese Liste).
2. **Almanach vorhanden** → den **Kurzcheck** lesen (`Read` mit `limit=80`; Hochrisiko-Bereiche
   r8/firebase-billing/claude-hooks/claude-config: VOLLTEXT), Versionen abgleichen, DANN arbeiten.
   Bei JEDEM Fehler im Bereich: VOLLTEXT nachlesen (Digest-Modell, [`SYSTEM.md`](SYSTEM.md) §11).
3. **Kein Almanach** → der Guard BLOCKIERT den Edit (seit 2026-06-07). Frank Bescheid geben
   und entscheiden: entweder nach seinem **OK** den Skill `bug-almanach-recherche` starten und
   einen Almanach anlegen, ODER (bei Kleinkram / bewusstem Verzicht) die Quittung
   `bug-almanac-ack-<slug>.flag` im TEMP anlegen. Auch komplett neue Sprachen werden generisch
   erkannt und blockiert.
4. **Neuen Bug erlebt** → in den passenden Almanach eintragen (Bug + Loesung + Version).

Vier Automatik-Schichten sorgen dafuer, dass das in **jeder** Session laeuft:
Session-Hook (`bug-almanac-index`) blendet diese Liste beim Start ein · Datei-Hook
(`bug-almanac-guard`) BLOCKIERT beim Anfassen bereichstypischer Dateien, bis der Almanach
(+ Best-Practices) gelesen ist bzw. — fehlt der Almanach — eine Quittung gesetzt ist ·
Fehler-Hook (`bug-case-auto-writer`) verweist bei neuen Fehlern auf den Almanach ·
Regel `known-bugs-before-coding.md` als Verhaltensschicht.

---

## ✅ Vorhandene Almanache (nach Kategorie)

### 📱 `android/` — Android-App-Entwicklung

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Kotlin** (Sprache/K2 + Coroutines + Compose-Kontext) | [`android/kotlin.md`](android/kotlin.md) | 2026-06-02 | ~46 | `*.kt`, `*.kts` (ausser `build/settings.gradle.kts` → Gradle), `AndroidManifest.xml` · „Kotlin", „K2", „Coroutines", „Flow", „Compose", „Android" |
| **Jetpack Compose** (Android-UI) | [`android/jetpack-compose.md`](android/jetpack-compose.md) | 2026-06-02 | ~74 | `*.kt` mit `@Composable`/`setContent` · „Compose", „Recomposition", „remember", „rememberSaveable", „LazyColumn", „Modifier", „LaunchedEffect", „Material3", „navigation-compose" |
| **Android-Framework / Platform-SDK** (Runtime/Framework) | [`android/android-platform.md`](android/android-platform.md) | 2026-06-02 | 79 | `AndroidManifest.xml`, `*Service.kt`/`*Receiver.kt`/`*Worker.kt`/`*Database.kt`/`*Migration(s).kt` · „Lifecycle", „onDestroy", „Permission", „Foreground Service", „ANR", „WorkManager", „Doze", „Room", „Migration", „WAL", „PendingIntent", „AlarmManager", „Notification", „Scoped Storage", „targetSdk", „Edge-to-Edge", „16KB" |
| **Firebase / Crashlytics / Play Billing** (Google-Backend-Dienste) | [`android/firebase-billing.md`](android/firebase-billing.md) | 2026-06-02 | 138 | `google-services.json`, `*Billing*.kt`/`*Subscription*.kt`/`*Purchase*.kt`, `BillingClient`, Cloud Functions · „Firebase", „Crashlytics", „FCM", „Firestore", „Billing", „Paywall", „App Check", „Remote Config", „acknowledge", „Proration", „RTDN", „firebase-ai", „Gemini" · Best-Practices: `best-practices/projekt-code/android/firebase-billing/` |
| **Voice-Assistant-Auslösung + Wake-Word + Mic** (fremden Sprachassistenten per Weckwort starten) | [`android/voice-assistant-trigger.md`](android/voice-assistant-trigger.md) | 2026-06-11 | 10 | `*VoiceInteractionService*`, `*AccessibilityService*`, `*WakeWord*`/`*Hotword*`, `AndroidManifest.xml` mit `foregroundServiceType="microphone"` · „Wake-Word", „Hotword", „Weckwort", „Assist", „ACTION_ASSIST", „KEYCODE_ASSIST", „Shizuku", „VoiceInteractionService", „AudioRecord", „sherpa-onnx", „Porcupine", „openWakeWord", „Default-Assistant", „ChatGPT Voice", „Mikrofon" · Best-Practices: `best-practices/projekt-code/android/best-practices-voice-assistant-trigger.md` |
| **3D auf Android — Filament / SceneView** (PBR-Echtzeit-3D mit Kotlin/Compose) | [`android/3d-filament-android.md`](android/3d-filament-android.md) | 2026-06-13 | 14 | `*.kt` mit `Filament`/`SceneView`/`ModelViewer`/`Engine`/`Renderer`, `*.filamat`/`*.mat`, `.glb`/`.gltf`/`.ktx` · „Filament", „SceneView", „matc", „cmgen", „IBL", „16-KB", „pageAlignSharedLibraries", „Vulkan", „OpenGL ES", „SwapChain", „setViewport" · Best-Practices: `best-practices/projekt-code/android/best-practices-3d-filament-android.md` |
| **WorkManager & Notifications** (Reminder + Hintergrund-Backups) | [`android/workmanager-notifications.md`](android/workmanager-notifications.md) | 2026-06-14 | ~110 (tief, inkl. Fix-Status) | `*Worker.kt`/`*Reminder*.kt`/`*Receiver.kt`/`*Alarm*.kt`, `AndroidManifest.xml` (BOOT_COMPLETED/POST_NOTIFICATIONS/SCHEDULE_EXACT_ALARM), `WorkManager`/`AlarmManager`/`NotificationChannel` · „WorkManager", „PeriodicWork", „AlarmManager", „setRepeating", „setExactAndAllowWhileIdle", „exact alarm", „SCHEDULE_EXACT_ALARM", „USE_EXACT_ALARM", „Notification Channel", „POST_NOTIFICATIONS", „BOOT_COMPLETED", „Reschedule", „Doze", „App Standby", „UIDT", „dontkillmyapp", „OEM kill", „Reminder", „Benachrichtigung" · Best-Practices: `best-practices/projekt-code/android/best-practices-workmanager-notifications.md` |
| **Google-Drive-Backup & Cloud-Sync** (Room-DB + Fotos → appDataFolder) | [`android/google-drive-backup.md`](android/google-drive-backup.md) | 2026-06-14 | ~145 | `*Drive*.kt`/`*Backup*.kt`/`*Restore*.kt`/`*Sync*.kt`, `appDataFolder`, `DriveScopes`, `GoogleAuthUtil`, `AuthorizationClient`, `google-api-services-drive`, `*Worker.kt` · „Google Drive", „appDataFolder", „Backup", „Restore", „Orphan/Waisen", „md5Checksum", „Changes API", „generateIds", „Multi-Device", „drive.appdata", „Credential Manager", „AuthorizationClient", „storageQuotaExceeded", „resumable upload", „WorkManager/Foreground-Service", „WAL/Room restore", „identityHash" · Best-Practices: `best-practices/projekt-code/android/best-practices-google-drive-backup.md` |

### 🔧 `android-build/` — Build-Kette

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Build — Gradle / AGP / R8·ProGuard / KSP** | [`android-build/gradle.md`](android-build/gradle.md) | 2026-06-02 | ~67 | `build.gradle*`, `settings.gradle*`, `gradle.properties`, `gradle/*`, `libs.versions.toml` · „Gradle", „AGP", „R8", „ProGuard", „KSP", „Daemon", „Version-Catalog" |
| **R8** (Code-Shrinker/Optimizer/Obfuscator, ProGuard-Nachfolger) | [`android-build/r8.md`](android-build/r8.md) | 2026-06-03 | ~50 | `proguard-rules.pro`, `consumer-rules.pro`, `*.keep.xml` · „R8", „minifyEnabled", „shrinkResources", „keep-Regel", „Full-Mode", „Release-Crash", „Obfuscation", „ProGuard", „missing_rules", „mapping.txt" · Best-Practices: `best-practices/projekt-code/android-build/r8/` |
| **Play-Store-Release & Policy** (Veröffentlichung, Reject-Gründe) | [`android-build/play-store-release.md`](android-build/play-store-release.md) | 2026-06-14 | ~130 | Play Console, App-Veröffentlichung, `AndroidManifest.xml` (foregroundServiceType/Accessibility/Permissions), `versionCode`/`targetSdk` in `build.gradle*` · „Play Store", „Release", „Track", „Closed Testing", „Staged Rollout", „versionCode", „Data Safety", „Foreground Service Deklaration", „AccessibilityService", „Permissions Declaration", „Pre-Launch Report", „App Signing", „Play Integrity", „targetSdk", „16 KB", „ASO", „Metadata Policy", „Reject" · Best-Practices: `best-practices/projekt-code/android-build/best-practices-play-store-release.md` |

### 🖥️ `desktop/` — Desktop-Apps

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **C# / .NET 8** (WPF, WinUI 3, Konsole, Backend) | [`desktop/dotnet-csharp.md`](desktop/dotnet-csharp.md) | 2026-06-02 | ~130 | `*.cs`, `*.csproj`, `*.xaml` · „WPF", „WinUI", „.NET", „C#", „Overlay" |
| **Windows-Overlay-Fenster (C#/WPF)** (TVO/ClaudeVoiceOverlay: always-on-top, Hotkeys, transparent) | [`desktop/windows-overlay.md`](desktop/windows-overlay.md) | 2026-06-14 | ~110 (tief, inkl. Fix-Status; +WindowChrome/DWM, .NET-9/10-Deployment) | `*.cs`/`*.xaml`/`app.manifest` · „Overlay", „Topmost", „always on top", „WS_EX_NOACTIVATE", „WS_EX_TOOLWINDOW", „WS_EX_TRANSPARENT", „WS_EX_LAYERED", „click-through", „SetWindowPos", „SetForegroundWindow", „RegisterHotKey", „MOD_NOREPEAT", „WH_KEYBOARD_LL", „Push-to-Talk", „AllowsTransparency", „WindowChrome", „DWM", „Mica", „Acrylic", „PerMonitorV2", „DpiChanged", „NotifyIcon", „Tray", „Autostart", „Single-Instance", „Mutex", „AppBar", „FlashWindowEx", „uiAccess" · Best-Practices: `best-practices/projekt-code/desktop/best-practices-windows-overlay.md` |
| **macOS-Desktop — Swift / AppKit** (Overlay-Apps, swiftc-CLI-Builds) | [`desktop/swift-appkit.md`](desktop/swift-appkit.md) | 2026-06-02 | ~58 | `*.swift`, `*.xcodeproj`, `Info.plist`, `*.entitlements`, `build.sh` · „Swift", „AppKit", „NSWindow", „NSPanel", „Overlay", „Accessibility", „AXIsProcessTrusted", „CGEventTap", „RegisterEventHotKey", „Hotkey", „Mikrofon", „AVAudioEngine", „TCC", „Notarization", „Sandbox", „setActivationPolicy" |
| **Wake-Word / Keyword-Spotting (.NET, C#/WPF)** (sherpa-onnx, Porcupine, openWakeWord) | [`desktop/wake-word.md`](desktop/wake-word.md) | 2026-06-08 | 33 | `.cs`/`.csproj` mit `sherpa`/`KeywordSpotter`/`WakeWordListener`/`Porcupine`/`onnxruntime`, `keywords.txt` · „Wake Word", „Weckwort", „Keyword-Spotting", „KWS", „sherpa-onnx", „Porcupine", „openWakeWord", „NanoWakeWord", „text2token", „NAudio Resampler" · Hinweis: `.cs`-Erzwingung laeuft aktuell ueber `dotnet-csharp.md` (Content-Probe-Zweig optional) · Best-Practices: `best-practices/projekt-code/desktop/best-practices-wake-word.md` |
| **Groq-Transkription (Whisper large-v3 / turbo)** (Speech-to-Text API, Always-On-Voice) | [`desktop/groq-transkription.md`](desktop/groq-transkription.md) | 2026-06-08 | ~25 | `.cs` mit `GroqWhisperClient`/`audio/transcriptions`/`whisper-large-v3`/`AlwaysOnListener` · „Groq", „Whisper", „Transkription", „Speech-to-Text", „STT", „no_speech_prob", „Halluzination bei Stille", „Vielen Dank bei Stille", „verbose_json", „VAD" · Hinweis: `.cs`-Erzwingung laeuft ueber `dotnet-csharp.md` · Best-Practices: `best-practices/projekt-code/desktop/best-practices-groq-transkription.md` |
| **Voice-Agent-Sprachpipeline** (VAD/Endpointing, Wake-Wachfenster, Turn-Taking, Latenz, Barge-in) | [`desktop/voice-pipeline.md`](desktop/voice-pipeline.md) | 2026-06-10 | ~20 | `.cs` mit `AlwaysOnListener`/`WakeWordController`/`EndpointDetector`/`StreamingSpeaker` · „Voice Agent", „Sprachpipeline", „Endpointing", „VAD", „Wachfenster", „hoert zu aber nichts passiert", „Turn-Taking", „Barge-in", „Latenz Sprachassistent", „Stille-Erkennung" · Hinweis: `.cs`-Erzwingung laeuft ueber wake-word/groq/dotnet-Zweige · Best-Practices: `best-practices/projekt-code/desktop/best-practices-voice-pipeline.md` |
| **3D auf macOS — Metal / SceneKit / RealityKit** (nativ schöne 3D-Apps, Swift) | [`desktop/3d-metal-scenekit-macos.md`](desktop/3d-metal-scenekit-macos.md) | 2026-06-13 | 18 | `*.swift` mit `Metal`/`MTKView`/`SceneKit`/`SCNView`/`RealityKit`/`RealityView`/`MetalFX`, `*.usdz`/`*.usd` · „Metal", „MetalFX", „SceneKit", „RealityKit", „RealityView", „USDZ", „EDR", „HDR", „IBL", „ImageBasedLight", „nextDrawable", „Tahoe Auto-Brightness" · Best-Practices: `best-practices/projekt-code/desktop/best-practices-3d-metal-scenekit-macos.md` |
| **3D auf Windows — C#/.NET (DirectX/Stride/Silk.NET)** (nativ schöne 3D-Apps) | [`desktop/3d-dotnet-directx-windows.md`](desktop/3d-dotnet-directx-windows.md) | 2026-06-13 | 14 | `*.cs`/`*.csproj` mit `Stride`/`Silk.NET`/`Vortice`/`Veldrid`/`SharpDX`/`Direct3D`/`SwapChain`, `*.sdsl` · „DirectX 12", „D3D12", „Stride", „Silk.NET", „Vortice", „Veldrid", „MonoGame", „Helix", „NativeAOT", „PublishTrimmed", „DEVICE_REMOVED", „Flip-Model sRGB" · Best-Practices: `best-practices/projekt-code/desktop/best-practices-3d-dotnet-directx-windows.md` |
| **3D mit Rust — wgpu / Bevy** (cross-platform schöne 3D-Apps) | [`desktop/3d-rust-wgpu-bevy.md`](desktop/3d-rust-wgpu-bevy.md) | 2026-06-13 | 13 | `*.rs` mit `bevy`/`wgpu`/`Camera3d`/`Mesh3d`/`MeshMaterial3d`/`PbrBundle`, `Cargo.toml` mit `bevy`/`wgpu` · „Bevy", „wgpu", „Solari", „TonyMcMapface", „Bloom", „hdr: true", „glTF #Scene0", „Required Components", „cargo-apk", „AAB", „xbuild" · Best-Practices: `best-practices/projekt-code/desktop/best-practices-3d-rust-wgpu-bevy.md` |
| **3D mit Godot 4** (cross-platform schöne 3D-Apps) | [`desktop/3d-godot.md`](desktop/3d-godot.md) | 2026-06-13 | 12 | `*.gd`/`*.tscn`/`*.tres`/`project.godot`/`*.gdshader` · „Godot", „Forward+", „Mobile-Renderer", „Compatibility", „LightmapGI", „SDFGI", „VoxelGI", „SSR", „D3D12 Black-Screen", „Notarisierung", „UV2", „C# kein Web" · Best-Practices: `best-practices/projekt-code/desktop/best-practices-3d-godot.md` |

### 🌐 `web/` — Web & Browser

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Browser-Erweiterungen** (Chrome/Edge, MV3) | [`web/chrome-extensions.md`](web/chrome-extensions.md) | 2026-06-02 | 73 | `manifest.json` (mit `manifest_version`), `background.js`, `service-worker.js`, `*/overlays/*`, `chrome.*`-APIs, `getUserMedia`/Mikrofon · „Erweiterung", „Extension", „Overlay", „Mikrofon" |
| **Web — TypeScript / Node** (+ npm, Bun) | [`web/typescript.md`](web/typescript.md) | 2026-06-02 | 89 | `*.ts`, `*.tsx`, `tsconfig.json`, `package.json` · „TypeScript", „Node", „npm", „ESM", „CommonJS", „require(esm)", „Bun", „tsconfig", „strict", „moduleResolution", „peer dependency", „ERESOLVE", „unhandled rejection", „@types", „better-sqlite3" |
| **3D im Web/TS — Three.js / Babylon / WebGPU** (schöne 3D-Apps, verpackt mit Tauri/Capacitor) | [`web/3d-threejs-webgpu.md`](web/3d-threejs-webgpu.md) | 2026-06-13 | 14 | `*.ts`/`*.tsx`/`*.js` mit `three`/`@babylonjs`/`@react-three/fiber`/`WebGPURenderer`/`GLTFLoader`/`KTX2Loader`, Tauri/Capacitor-Config · „Three.js", „Babylon.js", „WebGPU", „WebGL2", „R3F", „PMREMGenerator", „outputColorSpace", „ACESFilmic", „DRACOLoader", „KTX2", „Tauri", „Capacitor", „convertFileSrc", „TSL" · Best-Practices: `best-practices/projekt-code/web/best-practices-3d-threejs-webgpu.md` |

### 🎛️ `peripherie/` — Hardware-Peripherie

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Elgato Stream-Deck-Plugin** (klassisches WebSocket/JS-SDK + Node-SDK) | [`peripherie/stream-deck.md`](peripherie/stream-deck.md) | 2026-06-03 | ~85 | `*.sdPlugin/*`, Stream-Deck-`manifest.json` (mit `SDKVersion`/`Actions`/`States`), `propertyInspector`, `code.js`/`plugin.html`/`inspector.html` im Plugin-Webview, `@elgato/streamdeck` · „Stream Deck", „Elgato", „sdPlugin", „Property Inspector", „willAppear", „keyDown", „setState", „DisableAutomaticStates" · Best-Practices: `best-practices/projekt-code/peripherie/stream-deck/` |

### 🛠️ `claude-tooling/` — Claude-Code-Werkzeuge

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Claude-Harness — Konfiguration & Regeln** (CLAUDE.md, Rules, Settings, Skills, Commands, Agents, Memory, Plugins) | [`claude-tooling/claude-config.md`](claude-tooling/claude-config.md) | 2026-06-07 | ~40 | `CLAUDE.md`, `~/.claude/rules/*.md`, `settings.json`/`settings.local.json`, `SKILL.md`, `~/.claude/commands/*`, `~/.claude/agents/*`, `MEMORY.md`, Plugins/Marketplace · „CLAUDE.md", „Regel/Rule", „settings.json", „Permission", „Skill", „Command", „Agent", „Memory", „Plugin", „Konfiguration", „Context-Rot", „BOM" · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-claude-config.md` (Entscheidungsbaum + ergaenzend 02-skills/03-agents/06-commands/07-settings/08-kontext/09-token-effizienz/10-arbeitsweise) |
| **Claude-Harness — Hooks** (PowerShell/Bash) | [`claude-tooling/claude-hooks.md`](claude-tooling/claude-hooks.md) | 2026-06-01 | ~45 | `~/.claude/hooks/*.ps1`, `*.sh`, `settings.json` hooks-Sektion · „Hook", „SessionStart", „PreToolUse", „PostToolUse", „SubagentStop" |
| **MCP-Server-Bau** (Model Context Protocol, TS-SDK 1.27.1 + python-sdk) | [`claude-tooling/mcp-server.md`](claude-tooling/mcp-server.md) | 2026-06-03 | ~59 | `.mcp.json`, MCP-Server-Quellcode (`*.ts`/`*.py` mit `@modelcontextprotocol/sdk`/`McpServer`/`StdioServerTransport`/`FastMCP`/`stdio_server` — per Content-Probe) · „MCP", „Model Context Protocol", „stdio", „Streamable HTTP", „tool schema", „registerTool", „isError", „inputSchema" · Best-Practices: `best-practices/projekt-code/claude-tooling/mcp-server/` (Server-Bau-Seite) + `best-practices/05-mcp/` (Client/Konfig-Seite) |
| **Python auf Windows** (Encoding & Cross-Platform-Scripting) | [`claude-tooling/python-windows.md`](claude-tooling/python-windows.md) | 2026-06-02 | ~36 | `*.py` · „Python", „Encoding", „cp1252", „BOM", „UnicodeEncodeError", „encoding=utf-8", „os.replace", „venv", „PATH" |
| **Cowork (Claude Desktop App)** (agentischer Modus macOS/Windows: VM-Workspace, Connectors/MCP, Skills/Plugins, Scheduled Tasks, Live-Artefakte, Computer-Use) | [`claude-tooling/cowork.md`](claude-tooling/cowork.md) | 2026-06-13 | ~70 | Cowork-Tab, verbundene Ordner, Connectors, eigene Skills/Plugins (ZIP-Upload), `/schedule`/Scheduled Tasks, Live-Artefakte, Computer-Use/Chrome · „Cowork", „VM service not running", „EXDEV", „workspace unavailable", „Always allow", „skill not mounted", „validation failed", „Catch-up", „callMcpTool", „Prompt-Injection", „PromptArmor", „iCloud Datenverlust", „Computer Use" · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-cowork.md` |
| **Claude Code Desktop-App vs. CLI** (Code-Tab macOS/Windows: Installation, PATH/Env, Hooks, Permissions, MCP, Worktrees, Computer-Use, Preview, Cloud/SSH, fehlende Features) | [`claude-tooling/claude-code-desktop-vs-cli.md`](claude-tooling/claude-code-desktop-vs-cli.md) | 2026-06-13 | ~45 | Code-Tab, „Git is required", „Git LFS", PATH/PowerShell-Profil, „bypassPermissions", „PostToolUse feuert nicht", `.claude/worktrees/`, „localhost was blocked", Accessibility/Screen-Recording, „isn't available in this environment", `--print`/Headless, Agent-Teams, `/agents`/`/doctor` · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-claude-code-desktop-vs-cli.md` |

### 🎨 `assets/` — Icons & Medien-Assets

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **App-Icon-Building** (Windows `.ico`, Android Adaptive Icons, macOS `.icns`) | [`assets/icon-building.md`](assets/icon-building.md) | 2026-06-07 | ~30 | `*.ico`, `*.icns`, `ic_launcher*.xml`, `*.iconset/`, `<ApplicationIcon>` · „Icon", „App-Icon", „.ico", „.icns", „Adaptive Icon", „mipmap", „Icon-Cache", „schwarze/transparente Ecken", „Verknuepfung/Shortcut-Icon", „iconutil", „Squircle", „Pillow ICO" |
| **Visuelle Qualität für 3D** (engine-übergreifend: PBR/Licht/PostFX/Assets) | [`assets/3d-visual-quality.md`](assets/3d-visual-quality.md) | 2026-06-13 | 13 | Normal-/Roughness-/Metallic-Maps, `.hdr`/`.exr`, `.gltf`/`.glb`/`.ktx2` · „PBR", „IBL", „HDRI", „Tonemapping", „ACES", „AgX", „PBR Neutral", „Linear/sRGB", „Color Management", „Normal Map Y-Flip", „TAA", „Shadow Acne", „KTX2", „Draco", „glTF-Transform" · Best-Practices: `best-practices/projekt-code/assets/best-practices-3d-visual-quality.md` |

---

### 🔌 `apis/` — LLM-/HTTP-API-Integration & Authentifizierung

> Querschnitts-Bereich (kein sauberes Datei-Pattern): wird NICHT vom `bug-almanac-guard` erzwungen,
> sondern über diesen Index + die Stichwort-Trigger gefunden. Vor Arbeit an einer API-Anbindung die
> passende Datei lesen. Reihenfolge bei eigenem Code: zuerst `api-integration-general.md` (gilt immer),
> dann die anbieterspezifische Datei, bei Login `oauth-device-code.md`.

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Stichworte) |
|---------|-------|-------|------|----------------------------------|
| **Anbieterübergreifend** (Rate-Limit/Retry/SSE/Timeout/HttpClient/Secrets) | [`apis/api-integration-general.md`](apis/api-integration-general.md) | 2026-06-08 | ~20 | „API integrieren", „429", „Rate Limit", „Retry", „Backoff", „SSE", „Streaming", „Timeout", „HttpClient", „API-Key", „Idempotency" |
| **OpenAI API** | [`apis/openai-api.md`](apis/openai-api.md) | 2026-06-08 | 26 | „OpenAI", „GPT", „Responses API", „Chat Completions", „o1/o3/o4", „reasoning_effort", „max_completion_tokens", „Azure OpenAI" |
| **Anthropic Claude API** | [`apis/anthropic-api.md`](apis/anthropic-api.md) | 2026-06-08 | 30 | „Anthropic", „Claude API", „Messages API", „anthropic-version", „x-api-key", „tool_use", „prompt caching", „extended thinking" |
| **Google Gemini API** | [`apis/google-gemini-api.md`](apis/google-gemini-api.md) | 2026-06-08 | 22 | „Gemini", „google-genai", „generativelanguage", „thinkingBudget", „safetySettings", „Vertex" |
| **Groq API** | [`apis/groq-api.md`](apis/groq-api.md) | 2026-06-08 | 21 | „Groq", „LPU", „api.groq.com", „model_decommissioned", „whisper-large-v3", „TPM" |
| **OpenRouter** (Aggregator) | [`apis/openrouter-api.md`](apis/openrouter-api.md) | 2026-06-08 | 18 | „OpenRouter", „openrouter.ai", „HTTP-Referer", „provider routing", „:free", „:nitro" |
| **xAI Grok API** | [`apis/xai-grok-api.md`](apis/xai-grok-api.md) | 2026-06-08 | 15 | „Grok", „xAI", „api.x.ai", „grok-4", „Live Search" |
| **Mistral API** | [`apis/mistral-api.md`](apis/mistral-api.md) | 2026-06-08 | 24 | „Mistral", „api.mistral.ai", „Codestral", „FIM", „tool_call_id 9 Zeichen", „La Plateforme" |
| **DeepSeek API** | [`apis/deepseek-api.md`](apis/deepseek-api.md) | 2026-06-08 | 14 | „DeepSeek", „deepseek-reasoner", „reasoning_content", „deepseek-chat", „V3/V4" |
| **Lokale OpenAI-kompatible Server** (Ollama, LM Studio, vLLM, llama.cpp) | [`apis/local-openai-compatible.md`](apis/local-openai-compatible.md) | 2026-06-08 | 20 | „Ollama", „LM Studio", „vLLM", „llama.cpp", „localhost:11434", „num_ctx", „local LLM" |
| **Weitere LLM-APIs** (Cohere, Together, Fireworks, Perplexity, Bedrock, Azure, Cerebras, Vertex, HF) | [`apis/other-llm-apis.md`](apis/other-llm-apis.md) | 2026-06-08 | Survey | „Cohere", „Together", „Fireworks", „Perplexity", „Bedrock", „Cerebras", „Vertex AI", „SigV4" |
| **OAuth / Device-Code / PKCE / Token-Refresh** | [`apis/oauth-device-code.md`](apis/oauth-device-code.md) | 2026-06-08 | ~30 | „OAuth", „Device Code", „RFC 8628", „PKCE", „Token Refresh", „refresh token rotation", „Login einbauen" |
| **CLI-Impersonation / Abo-OAuth** (Codex-Abo, Hermes-Trick, Anthropic-Ban) | [`apis/cli-impersonation-subscription-auth.md`](apis/cli-impersonation-subscription-auth.md) | 2026-06-08 | — | „als Codex CLI ausgeben", „ChatGPT-Abo per Geraetekode", „Hermes", „Codex device-auth", „Abo statt API-Key", „Copilot-Token" |
| **TTS-Provider** (Edge-TTS, Google Chirp 3 HD, Android-native, Chrome-MV3) | [`apis/tts-provider.md`](apis/tts-provider.md) | 2026-06-14 | ~104 | „TTS", „Text-to-Speech", „Vorlesen", „Vorlese-Funktion", „vorlese-overlay", „Edge-TTS", „Sec-MS-GEC", „No audio received", „Chirp 3 HD", „de-DE-Chirp3-HD", „SSML", „markup/pause", „TextToSpeech", „MediaPlayer", „Offscreen-Document", „Stimme/Voice", „ElevenLabs" |

---

### 🤖 `agents/` — Multi-Agenten-Systeme & Orchestrierung

> Querschnitts-/Konzept-Bereich (kein sauberes Datei-Pattern, wie `apis/`): wird NICHT vom
> `bug-almanac-guard` erzwungen, sondern ueber diesen Index + Stichwort-Trigger gefunden. Vor dem
> Bau eines Haupt-/Boss-/Orchestrator-Agenten ZUERST den Almanach, DANN die Best-Practices lesen.

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Stichworte) |
|---------|-------|-------|------|----------------------------------|
| **Boss-/Orchestrator-Agent** (Multi-Agenten-System, INTERN in Claude Code UND EXTERN selbst gebaut: Intent-Verstehen, Delegation/Routing, Sub-Agent-Spawning, Tool-Calling, menschlicher Dialog, State/Reliability/Security + Sektion 8: from-scratch-Loop, C#/.NET/Semantic Kernel, Voice-Orchestrierung, lokal/Multi-Provider, TS/JS) | [`agents/orchestrator-agent.md`](agents/orchestrator-agent.md) | 2026-06-09 | ~80 | „Boss-Agent", „Orchestrator", „Supervisor-Agent", „Multi-Agent", „Sub-Agent bauen/spawnen", „Agent baut Agenten", „Intent verstehen", „Delegation", „Handoff", „eigener Agent in meiner App", „from scratch Agent-Loop", „VoiceAgent Boss", „Semantic Kernel", „MS Agent Framework", „LangGraph", „CrewAI", „AutoGen", „OpenAI Agents SDK", „Vercel AI SDK", „Mastra", „Claude Agent SDK", „natuerliche Sprache verstehen", „menschlich antworten" · Best-Practices: `best-practices/projekt-code/agents/best-practices-orchestrator-agent.md` |

---

## ⬜ Bereiche ohne Almanach (bei erster echter Arbeit: recherchieren — erst Franks OK)

> Diese Liste ist die Landkarte der erwarteten Bereiche. Sobald an einem davon
> echte Arbeit beginnt und noch kein Almanach existiert, wird er im passenden
> Kategorie-Ordner angelegt.

| Prio | Bereich | (geplante Datei) | Erkennungs-Trigger (Dateien / Stichworte) |
|------|---------|------------------|-------------------------------------------|

> **Fertige Recherche-Prompts** fuer alle offenen Bereiche (Almanach + Best-Practices,
> Copy-Paste fuer parallele Sessions): siehe [`OFFENE-ALMANACHE-PROMPTS.md`](OFFENE-ALMANACHE-PROMPTS.md).
>
> **Moegliche Vertiefung** als Abschnitt statt eigener Datei: PowerShell-Scripting allgemein
> → `claude-tooling/claude-hooks.md` (bei genug Eigenleben spaeter ausgliedern).

(Liste waechst mit. Neue Bereiche hier ergaenzen, sobald sie auftauchen. Das Pfad-Mapping
im `bug-almanac-guard`-Hook ist kategorie-robust — bei einem NEUEN Dateimuster dort einen
Zweig ergaenzen; ein blosser Kategorie-Wechsel einer bestehenden Datei braucht KEINE
Hook-Aenderung.)

---

## Aufbau jeder Almanach-Datei (Format-Vorlage)

```
# Bekannte Bugs: <Thema>
> PFLICHT-LESEN vor Arbeit an <Thema>.
> Stand: zuletzt recherchiert am <Datum> fuer Version <X>.

## N. <Bug-Titel>   [⭐ HAEUFIG falls oft]
Symptom:    Was man sieht
Ursache:    Der wahre Grund
Versionen:  betrifft V1-V3, gefixt ab V4   (oder „per Design / unabhaengig")
FIX:        Beste funktionserhaltende Loesung (NIE „Feature weg")
Quelle:     Link / eigener Vorfall
```
