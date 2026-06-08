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
├── android/          Kotlin, Jetpack Compose, Android-Platform/SDK, Firebase/Billing
├── android-build/    Gradle/AGP, R8/ProGuard
├── desktop/          C#/.NET (Windows), Swift/AppKit (macOS)
├── web/              Chrome-Erweiterungen, TypeScript/Node  (+ Tampermonkey geplant)
├── peripherie/       Elgato Stream-Deck-Plugin
├── claude-tooling/   Claude-Hooks, MCP-Server-Bau, Python (Windows-Scripting)
└── assets/           App-Icon-Building (Windows .ico, Android Adaptive, macOS .icns)
```

Die Gegenseite (Best-Practices) spiegelt dieselben Kategorien:
`best-practices/projekt-code/<kategorie>/<software>/best-practices.md`.

---

## So funktioniert es (Kurzfassung)

1. **Vor** echter Arbeit an einem technischen Bereich: pruefen, ob es hier einen
   Almanach fuer den Bereich gibt (diese Liste).
2. **Almanach vorhanden** → komplett lesen, Versionen abgleichen, DANN arbeiten.
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

### 🔧 `android-build/` — Build-Kette

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Build — Gradle / AGP / R8·ProGuard / KSP** | [`android-build/gradle.md`](android-build/gradle.md) | 2026-06-02 | ~67 | `build.gradle*`, `settings.gradle*`, `gradle.properties`, `gradle/*`, `libs.versions.toml` · „Gradle", „AGP", „R8", „ProGuard", „KSP", „Daemon", „Version-Catalog" |
| **R8** (Code-Shrinker/Optimizer/Obfuscator, ProGuard-Nachfolger) | [`android-build/r8.md`](android-build/r8.md) | 2026-06-03 | ~50 | `proguard-rules.pro`, `consumer-rules.pro`, `*.keep.xml` · „R8", „minifyEnabled", „shrinkResources", „keep-Regel", „Full-Mode", „Release-Crash", „Obfuscation", „ProGuard", „missing_rules", „mapping.txt" · Best-Practices: `best-practices/projekt-code/android-build/r8/` |

### 🖥️ `desktop/` — Desktop-Apps

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **C# / .NET 8** (WPF, WinUI 3, Konsole, Backend) | [`desktop/dotnet-csharp.md`](desktop/dotnet-csharp.md) | 2026-06-02 | ~130 | `*.cs`, `*.csproj`, `*.xaml` · „WPF", „WinUI", „.NET", „C#", „Overlay" |
| **macOS-Desktop — Swift / AppKit** (Overlay-Apps, swiftc-CLI-Builds) | [`desktop/swift-appkit.md`](desktop/swift-appkit.md) | 2026-06-02 | ~58 | `*.swift`, `*.xcodeproj`, `Info.plist`, `*.entitlements`, `build.sh` · „Swift", „AppKit", „NSWindow", „NSPanel", „Overlay", „Accessibility", „AXIsProcessTrusted", „CGEventTap", „RegisterEventHotKey", „Hotkey", „Mikrofon", „AVAudioEngine", „TCC", „Notarization", „Sandbox", „setActivationPolicy" |
| **Wake-Word / Keyword-Spotting (.NET, C#/WPF)** (sherpa-onnx, Porcupine, openWakeWord) | [`desktop/wake-word.md`](desktop/wake-word.md) | 2026-06-08 | 33 | `.cs`/`.csproj` mit `sherpa`/`KeywordSpotter`/`WakeWordListener`/`Porcupine`/`onnxruntime`, `keywords.txt` · „Wake Word", „Weckwort", „Keyword-Spotting", „KWS", „sherpa-onnx", „Porcupine", „openWakeWord", „NanoWakeWord", „text2token", „NAudio Resampler" · Hinweis: `.cs`-Erzwingung laeuft aktuell ueber `dotnet-csharp.md` (Content-Probe-Zweig optional) · Best-Practices: `best-practices/projekt-code/desktop/best-practices-wake-word.md` |
| **Groq-Transkription (Whisper large-v3 / turbo)** (Speech-to-Text API, Always-On-Voice) | [`desktop/groq-transkription.md`](desktop/groq-transkription.md) | 2026-06-08 | ~25 | `.cs` mit `GroqWhisperClient`/`audio/transcriptions`/`whisper-large-v3`/`AlwaysOnListener` · „Groq", „Whisper", „Transkription", „Speech-to-Text", „STT", „no_speech_prob", „Halluzination bei Stille", „Vielen Dank bei Stille", „verbose_json", „VAD" · Hinweis: `.cs`-Erzwingung laeuft ueber `dotnet-csharp.md` · Best-Practices: `best-practices/projekt-code/desktop/best-practices-groq-transkription.md` |

### 🌐 `web/` — Web & Browser

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Browser-Erweiterungen** (Chrome/Edge, MV3) | [`web/chrome-extensions.md`](web/chrome-extensions.md) | 2026-06-02 | 73 | `manifest.json` (mit `manifest_version`), `background.js`, `service-worker.js`, `*/overlays/*`, `chrome.*`-APIs, `getUserMedia`/Mikrofon · „Erweiterung", „Extension", „Overlay", „Mikrofon" |
| **Web — TypeScript / Node** (+ npm, Bun) | [`web/typescript.md`](web/typescript.md) | 2026-06-02 | 89 | `*.ts`, `*.tsx`, `tsconfig.json`, `package.json` · „TypeScript", „Node", „npm", „ESM", „CommonJS", „require(esm)", „Bun", „tsconfig", „strict", „moduleResolution", „peer dependency", „ERESOLVE", „unhandled rejection", „@types", „better-sqlite3" |

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

### 🎨 `assets/` — Icons & Medien-Assets

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **App-Icon-Building** (Windows `.ico`, Android Adaptive Icons, macOS `.icns`) | [`assets/icon-building.md`](assets/icon-building.md) | 2026-06-07 | ~30 | `*.ico`, `*.icns`, `ic_launcher*.xml`, `*.iconset/`, `<ApplicationIcon>` · „Icon", „App-Icon", „.ico", „.icns", „Adaptive Icon", „mipmap", „Icon-Cache", „schwarze/transparente Ecken", „Verknuepfung/Shortcut-Icon", „iconutil", „Squircle", „Pillow ICO" |

---

## ⬜ Bereiche ohne Almanach (bei erster echter Arbeit: recherchieren — erst Franks OK)

> Diese Liste ist die Landkarte der erwarteten Bereiche. Sobald an einem davon
> echte Arbeit beginnt und noch kein Almanach existiert, wird er im passenden
> Kategorie-Ordner angelegt.

| Prio | Bereich | (geplante Datei) | Erkennungs-Trigger (Dateien / Stichworte) |
|------|---------|------------------|-------------------------------------------|
| · | **Tampermonkey / Userscripts** | `web/tampermonkey.md` | `*.user.js` · „Tampermonkey", „Userscript", „Greasemonkey" |

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
