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

**Wartung (Poka-Yoke Stufe 3):** `python3 bugs/check-guard-coverage.py` prueft, ob JEDER
Almanach vom `bug-almanac-guard` auch wirklich erzwungen wird — meldet `[OK]` / `[BEWUSST]`
(Querschnitt, Allowlist) / `[LUECKE]`. Nach jedem neuen Almanach ausfuehren, damit kein Almanach
unbemerkt ohne Erzwingung bleibt. Details in [`SYSTEM.md`](SYSTEM.md).

---

## ✅ Vorhandene Almanache (nach Kategorie)

### 📱 `android/` — Android-App-Entwicklung

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Kotlin** (Sprache/K2 + Coroutines + Compose-Kontext) | [`android/kotlin.md`](android/kotlin.md) | 2026-06-02 | ~46 | `*.kt`, `*.kts` (ausser `build/settings.gradle.kts` → Gradle), `AndroidManifest.xml` · „Kotlin", „K2", „Coroutines", „Flow", „Compose", „Android" |
| **Jetpack Compose** (Android-UI) | [`android/jetpack-compose.md`](android/jetpack-compose.md) | 2026-06-02 | ~74 | `*.kt` mit `@Composable`/`setContent` · „Compose", „Recomposition", „remember", „rememberSaveable", „LazyColumn", „Modifier", „LaunchedEffect", „Material3", „navigation-compose" |
| **Android-Framework / Platform-SDK** (Runtime/Framework) | [`android/android-platform.md`](android/android-platform.md) | 2026-06-02 | 79 | `AndroidManifest.xml`, `*Service.kt`/`*Receiver.kt`/`*Worker.kt` · „Lifecycle", „onDestroy", „Permission", „Foreground Service", „ANR", „WorkManager", „Doze", „PendingIntent", „AlarmManager", „Notification", „Scoped Storage", „targetSdk", „Edge-to-Edge", „16KB" · Hinweis: Room-Runtime hat seit 2026-06-15 einen **eigenen, dedizierten** Almanach → `android/room.md` (§5 hier ist nur noch Kurz-Uebersicht) |
| **Room-Persistenz** (Entity/DAO/Database/Migration/TypeConverter/@Relation, DB-Backup-WAL) | [`android/room.md`](android/room.md) | 2026-06-15 | ~63 (inkl. Fix-Status) | `*.kt` mit `@Entity`/`@Dao`/`@Database`/`Migration`, `*Dao.kt`/`*Database.kt`/`*Migration(s).kt` · „Room", „@Entity", „@Dao", „@Database", „Migration", „@AutoMigration", „AutoMigrationSpec", „fallbackToDestructiveMigration", „identityHash", „Room cannot verify the data integrity", „migration … was required but not found", „Cannot access database on the main thread", „allowMainThreadQueries", „withTransaction", „distinctUntilChanged", „InvalidationTracker", „@Relation", „@Junction", „@Embedded", „TypeConverter", „@ProvidedTypeConverter", „@Upsert", „OnConflictStrategy", „@ForeignKey", „WAL", „wal_checkpoint", „JournalMode", „enableMultiInstanceInvalidation", „room.schemaLocation", „room.generateKotlin", „MigrationTestHelper", „KSP" · Anker: Room 2.7.0 · KSP 2.1.0-1.0.29 · Kotlin 2.1.0 · AGP 8.7.3 |
| **Firebase / Crashlytics / Play Billing** (Google-Backend-Dienste) | [`android/firebase-billing.md`](android/firebase-billing.md) | 2026-06-02 | 138 | `google-services.json`, `*Billing*.kt`/`*Subscription*.kt`/`*Purchase*.kt`, `BillingClient`, Cloud Functions · „Firebase", „Crashlytics", „FCM", „Firestore", „Billing", „Paywall", „App Check", „Remote Config", „acknowledge", „Proration", „RTDN", „firebase-ai", „Gemini" · Best-Practices: `best-practices/projekt-code/android/firebase-billing/` |
| **Voice-Assistant-Auslösung + Wake-Word + Mic** (fremden Sprachassistenten per Weckwort starten) | [`android/voice-assistant-trigger.md`](android/voice-assistant-trigger.md) | 2026-06-11 | 10 | `*VoiceInteractionService*`, `*AccessibilityService*`, `*WakeWord*`/`*Hotword*`, `AndroidManifest.xml` mit `foregroundServiceType="microphone"` · „Wake-Word", „Hotword", „Weckwort", „Assist", „ACTION_ASSIST", „KEYCODE_ASSIST", „Shizuku", „VoiceInteractionService", „AudioRecord", „sherpa-onnx", „Porcupine", „openWakeWord", „Default-Assistant", „ChatGPT Voice", „Mikrofon" · Best-Practices: `best-practices/projekt-code/android/best-practices-voice-assistant-trigger.md` |
| **3D auf Android — Filament / SceneView** (PBR-Echtzeit-3D mit Kotlin/Compose) | [`android/3d-filament-android.md`](android/3d-filament-android.md) | 2026-06-13 | 14 | `*.kt` mit `Filament`/`SceneView`/`ModelViewer`/`Engine`/`Renderer`, `*.filamat`/`*.mat`, `.glb`/`.gltf`/`.ktx` · „Filament", „SceneView", „matc", „cmgen", „IBL", „16-KB", „pageAlignSharedLibraries", „Vulkan", „OpenGL ES", „SwapChain", „setViewport" · Best-Practices: `best-practices/projekt-code/android/best-practices-3d-filament-android.md` |
| **WorkManager & Notifications** (Reminder + Hintergrund-Backups) | [`android/workmanager-notifications.md`](android/workmanager-notifications.md) | 2026-06-14 | ~110 (tief, inkl. Fix-Status) | `*Worker.kt`/`*Reminder*.kt`/`*Receiver.kt`/`*Alarm*.kt`, `AndroidManifest.xml` (BOOT_COMPLETED/POST_NOTIFICATIONS/SCHEDULE_EXACT_ALARM), `WorkManager`/`AlarmManager`/`NotificationChannel` · „WorkManager", „PeriodicWork", „AlarmManager", „setRepeating", „setExactAndAllowWhileIdle", „exact alarm", „SCHEDULE_EXACT_ALARM", „USE_EXACT_ALARM", „Notification Channel", „POST_NOTIFICATIONS", „BOOT_COMPLETED", „Reschedule", „Doze", „App Standby", „UIDT", „dontkillmyapp", „OEM kill", „Reminder", „Benachrichtigung" · Best-Practices: `best-practices/projekt-code/android/best-practices-workmanager-notifications.md` |
| **Google-Drive-Backup & Cloud-Sync** (Room-DB + Fotos → appDataFolder) | [`android/google-drive-backup.md`](android/google-drive-backup.md) | 2026-06-14 | ~145 | `*Drive*.kt`/`*Backup*.kt`/`*Restore*.kt`/`*Sync*.kt`, `appDataFolder`, `DriveScopes`, `GoogleAuthUtil`, `AuthorizationClient`, `google-api-services-drive`, `*Worker.kt` · „Google Drive", „appDataFolder", „Backup", „Restore", „Orphan/Waisen", „md5Checksum", „Changes API", „generateIds", „Multi-Device", „drive.appdata", „Credential Manager", „AuthorizationClient", „storageQuotaExceeded", „resumable upload", „WorkManager/Foreground-Service", „WAL/Room restore", „identityHash" · Best-Practices: `best-practices/projekt-code/android/best-practices-google-drive-backup.md` |
| **Hilt/Dagger DI + KSP** (Dependency Injection) | [`android/hilt-dagger.md`](android/hilt-dagger.md) | 2026-06-14 | ~59 (inkl. Fix-Status) | `*.kt` mit `@HiltAndroidApp`/`@AndroidEntryPoint`/`@HiltViewModel`/`@Module`/`@InstallIn`/`@HiltWorker`, `*Module.kt` · „Hilt", „Dagger", „@Inject", „@Provides", „@Binds", „@InstallIn", „Component", „Scope", „Qualifier", „hiltViewModel", „hilt-navigation-compose", „HiltWorker", „HiltWorkerFactory", „Configuration.Provider", „MissingBinding", „DuplicateBindings", „IncompatiblyScopedBindings", „EntryPoint", „EntryPointAccessors", „kotlinx-metadata", „KSP2", „enableAggregatingTask", „HiltAndroidRule", „HiltTestApplication" · Anker: Hilt 2.55 · hilt-navigation-compose/hilt-work 1.2.0 · KSP 2.1.0-1.0.29 · Kotlin 2.1.0 |
| **Retrofit + OkHttp + Moshi** (Networking/API-Layer) | [`android/retrofit-okhttp-moshi.md`](android/retrofit-okhttp-moshi.md) | 2026-06-14 | ~64 (Fokus R8-Release-Crash, inkl. Fix-Status) | `*.kt` mit `retrofit2`/`okhttp3`/`@GET`/`@POST`/`@Body`/`@Query`/`OkHttpClient`/`Interceptor`/`@JsonClass`/`Moshi`/`CertificatePinner` · „Retrofit", „OkHttp", „Moshi", „converter-moshi", „@JsonClass", „KotlinJsonAdapterFactory", „Unable to create converter", „R8/ProGuard keep", „retrofit2.pro", „moshi.pro", „suspend HttpException", „Response<Unit>", „CancellationException", „errorBody", „Authenticator", „callTimeout", „ResponseBody leaked", „HttpLoggingInterceptor", „redactHeader", „CertificatePinner", „Backup-Pin", „@FormUrlEncoded", „@Path", „baseUrl" · Anker: Retrofit 2.11.0 · OkHttp 4.12.0 · Moshi 1.15.1 · KSP 2.1.0-1.0.29 · R8 full mode |
| **Media3 / ExoPlayer** (Audio-Wiedergabe, TTS in Compose, teils Hintergrund) | [`android/media3-exoplayer.md`](android/media3-exoplayer.md) | 2026-06-14 | ~60 (inkl. Fix-Status) | `*.kt` mit `media3`/`ExoPlayer`/`MediaSession`/`MediaItem`/`PlayerView`/`MediaController`/`DefaultLoadControl` · „Media3", „ExoPlayer", „MediaSession", „MediaSessionService", „MediaController", „MediaItem", „player.release", „DisposableEffect", „wrong thread", „applicationLooper", „AudioAttributes", „handleAudioFocus", „AUDIO_CONTENT_TYPE_SPEECH", „setHandleAudioBecomingNoisy", „foregroundServiceType mediaPlayback", „ForegroundServiceStartNotAllowed", „ForegroundServiceDidNotStartInTime", „setMediaItem prepare play", „STATE_ENDED", „ByteArrayDataSource", „DefaultLoadControl", „bufferForPlaybackMs" · Anker: media3 1.5.1 (exoplayer/common/session) · Compose · Android 12–15 |
| **Coil 3** (Bild-/Video-Laden in Compose) | [`android/coil3.md`](android/coil3.md) | 2026-06-14 | ~53 (inkl. Fix-Status) | `*.kt` mit `coil3`/`AsyncImage`/`ImageLoader`/`rememberAsyncImagePainter`/`SubcomposeAsyncImage`/`ImageRequest`/`SingletonImageLoader` · „Coil", „Coil 3", „io.coil-kt.coil3", „AsyncImage", „ImageLoader", „SingletonImageLoader", „PlatformContext", „coil-network-okhttp", „Unable to create a fetcher", „VideoFrameDecoder", „coil-video", „SvgDecoder", „GifDecoder", „AnimatedImageDecoder", „crossfade", „placeholder", „DiskCache", „MemoryCache", „allowHardware", „Size.ORIGINAL", „bitmapConfig", „AsyncImagePreviewHandler", „dontwarn coil3.PlatformContext", „Keyer" · Anker: coil 3.0.4 (coil-compose + coil-video; coil-network-okhttp FEHLT) · Compose · aktuell 3.5.0 |

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
| **Text-Injection in Electron/Chromium-Felder (Windows, C#/WPF, .NET 10)** (ClaudeVoiceOverlay → Claude Desktop 1.12603.1 = Electron 41/Chromium 146; Chat/Code/Cowork; macOS klappt, Windows nicht) | [`desktop/windows-electron-text-injection.md`](desktop/windows-electron-text-injection.md) | 2026-06-15 | ~65 (Kurzcheck + Fix-Status; +.NET-10-Single-File, Win11 24H2, Version-Bestimmung) | `*.cs`/`*.csproj` mit Text-Einfuegen in fremde Fenster · „Text einfuegen“, „SendInput“, „keybd_event“, „KEYEVENTF_SCANCODE“, „Ctrl+V kommt nicht an“, „contenteditable“, „Chrome_RenderWidgetHostHWND“, „FindWindowEx“, „EnumChildWindows“, „SetForegroundWindow“, „AttachThreadInput“, „UIPI“, „uiAccess“, „CLIPBRD_E_CANT_OPEN“, „Clipboard Race“, „SetDataObject copy:true“, „UI Automation“, „FlaUI“, „ValuePattern“, „TextPattern“, „force-renderer-accessibility“, „WM_GETOBJECT“, „PerMonitorV2“, „DWMWA_EXTENDED_FRAME_BOUNDS“, „Electron 41“, „Chromium 146“, „Claude Desktop“, „Codex“, „Cursor“, „.NET 10“, „self-contained single-file“, „PublishSingleFile“, „Trimming“, „app.manifest“, „Windows 11 24H2“, „Administrator Protection“ · Best-Practices: getrennter Lauf (folgt) |
| **macOS-Desktop — Swift / AppKit** (Overlay-Apps, swiftc-CLI-Builds) | [`desktop/swift-appkit.md`](desktop/swift-appkit.md) | 2026-06-02 | ~58 | `*.swift`, `*.xcodeproj`, `Info.plist`, `*.entitlements`, `build.sh` · „Swift", „AppKit", „NSWindow", „NSPanel", „Overlay", „Accessibility", „AXIsProcessTrusted", „CGEventTap", „RegisterEventHotKey", „Hotkey", „Mikrofon", „AVAudioEngine", „TCC", „Notarization", „Sandbox", „setActivationPolicy" |
| **macOS-Overlay-Fenster (Swift/AppKit)** (ClaudeCodexVoiceOverlay/TerminalVoiceOverlay: schwebend, Hotkey, Mikrofon) | [`desktop/macos-overlay.md`](desktop/macos-overlay.md) | 2026-06-14 | ~50 | `*.swift`/`Info.plist`/`*.entitlements` · „NSPanel", „nonactivatingPanel", „floating", „collectionBehavior", „canJoinAllSpaces", „fullScreenAuxiliary", „orderFrontRegardless", „activate ignoringOtherApps", „ignoresMouseEvents", „CGEventTap", „RegisterEventHotKey", „Input Monitoring", „Accessibility", „AXIsProcessTrusted", „CGPreflightListenEventAccess", „SMAppService", „LSUIElement", „NSMicrophoneUsageDescription", „AVAudioEngine", „Notarization", „Hardened Runtime", „Developer ID", „TCC", „Tahoe", „Sequoia" · Best-Practices: `best-practices/projekt-code/desktop/best-practices-macos-overlay.md` |
| **Wake-Word / Keyword-Spotting (.NET, C#/WPF)** (sherpa-onnx, Porcupine, openWakeWord) | [`desktop/wake-word.md`](desktop/wake-word.md) | 2026-06-08 | 33 | `.cs`/`.csproj` mit `sherpa`/`KeywordSpotter`/`WakeWordListener`/`Porcupine`/`onnxruntime`, `keywords.txt` · „Wake Word", „Weckwort", „Keyword-Spotting", „KWS", „sherpa-onnx", „Porcupine", „openWakeWord", „NanoWakeWord", „text2token", „NAudio Resampler" · Hinweis: `.cs`-Erzwingung laeuft aktuell ueber `dotnet-csharp.md` (Content-Probe-Zweig optional) · Best-Practices: `best-practices/projekt-code/desktop/best-practices-wake-word.md` |
| **Groq-Transkription (Whisper large-v3 / turbo)** (Speech-to-Text API, Always-On-Voice) | [`desktop/groq-transkription.md`](desktop/groq-transkription.md) | 2026-06-08 | ~25 | `.cs` mit `GroqWhisperClient`/`audio/transcriptions`/`whisper-large-v3`/`AlwaysOnListener` · „Groq", „Whisper", „Transkription", „Speech-to-Text", „STT", „no_speech_prob", „Halluzination bei Stille", „Vielen Dank bei Stille", „verbose_json", „VAD" · Hinweis: `.cs`-Erzwingung laeuft ueber `dotnet-csharp.md` · Best-Practices: `best-practices/projekt-code/desktop/best-practices-groq-transkription.md` |
| **On-Device-Whisper / lokale Transkription** (whisper.cpp / faster-whisper, Mac+Windows, lokal vs. Groq) | [`desktop/whisper-stt-lokal.md`](desktop/whisper-stt-lokal.md) | 2026-06-14 | ~105 (tief, inkl. Fix-Status; +Audio-Pipeline, Whisper.net/WhisperKit) | `*.swift`/`*.cs`/`*.py`/`CMakeLists.txt` · „whisper.cpp", „faster-whisper", „CTranslate2", „Whisper.net", „WhisperKit", „ggml", „large-v3-turbo", „primeline-german", „Metal", „Core ML", „ANE", „CUDA", „cuDNN", „Vulkan", „int8", „q5_0", „Silero VAD", „vad_filter", „condition_on_previous_text", „Halluzination bei Stille", „ZDF Untertitel", „Streaming", „whisper-stream", „WhisperLiveKit", „SimulStreaming", „LocalAgreement", „language de", „lokal vs Groq", „DSGVO", „ZDR" · Best-Practices: `best-practices/projekt-code/desktop/best-practices-whisper-stt-lokal.md` |
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
| **Claude-Harness — Hooks** (PowerShell/Bash) | [`claude-tooling/claude-hooks.md`](claude-tooling/claude-hooks.md) | 2026-06-15 | ~53 | `~/.claude/hooks/*.ps1`, `*.sh`, `settings.json` hooks-Sektion · „Hook", „SessionStart", „PreToolUse", „PostToolUse", „SubagentStop" · Best-Practices: `best-practices/01-hooks/best-practices.md` |
| **MCP-Server-Bau** (Model Context Protocol, TS-SDK 1.27.1 + python-sdk) | [`claude-tooling/mcp-server.md`](claude-tooling/mcp-server.md) | 2026-06-03 | ~59 | `.mcp.json`, MCP-Server-Quellcode (`*.ts`/`*.py` mit `@modelcontextprotocol/sdk`/`McpServer`/`StdioServerTransport`/`FastMCP`/`stdio_server` — per Content-Probe) · „MCP", „Model Context Protocol", „stdio", „Streamable HTTP", „tool schema", „registerTool", „isError", „inputSchema" · Best-Practices: `best-practices/projekt-code/claude-tooling/mcp-server/` (Server-Bau-Seite) + `best-practices/05-mcp/` (Client/Konfig-Seite) |
| **Python auf Windows** (Encoding & Cross-Platform-Scripting) | [`claude-tooling/python-windows.md`](claude-tooling/python-windows.md) | 2026-06-02 | ~36 | `*.py` · „Python", „Encoding", „cp1252", „BOM", „UnicodeEncodeError", „encoding=utf-8", „os.replace", „venv", „PATH" |
| **Cowork (Claude Desktop App)** (agentischer Modus macOS/Windows: VM-Workspace, Connectors/MCP, Skills/Plugins, Scheduled Tasks, Live-Artefakte, Computer-Use) | [`claude-tooling/cowork.md`](claude-tooling/cowork.md) | 2026-06-13 | ~70 | Cowork-Tab, verbundene Ordner, Connectors, eigene Skills/Plugins (ZIP-Upload), `/schedule`/Scheduled Tasks, Live-Artefakte, Computer-Use/Chrome · „Cowork", „VM service not running", „EXDEV", „workspace unavailable", „Always allow", „skill not mounted", „validation failed", „Catch-up", „callMcpTool", „Prompt-Injection", „PromptArmor", „iCloud Datenverlust", „Computer Use" · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-cowork.md` |
| **Cowork Git-Push (über Mount-Brücke)** (committen/pushen aus der Cowork-VM: virtiofs/FUSE-Arbeitsbaum, Lock/fileMode/Symlink/LFS/CRLF/Mount-Truncation, non-fast-forward, Plumbing) | [`claude-tooling/cowork-git-push.md`](claude-tooling/cowork-git-push.md) | 2026-06-15 | 22 (inkl. Fix-Status) | `cowork-git.sh`, Git aus Cowork · „index.lock Operation not permitted", „could not read Username", „should have been pointers", „CRLF will be replaced", „fetch first", „cannot rebase unstaged changes", „dubious ownership", „Stale file handle", virtiofs-Truncation · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-cowork-git-push.md` |
| **Cowork — Geplante & wiederkehrende Aufgaben** (Scheduled Tasks / Routines: System-Wahl lokal vs. Cloud-Routine vs. `/loop`, Catch-up/Missed-runs, High-Freq-Cron-Freeze, Zombie/Skip-Cascade, Cron/Zeitzone/DST, Permissions, MCP-Warm-up, Quota, Mobile/Dispatch) | [`claude-tooling/cowork-scheduled-tasks.md`](claude-tooling/cowork-scheduled-tasks.md) | 2026-06-15 | ~70 (Kurzcheck + 3-System-Tabelle + Fix-Status) | geplante/wiederkehrende Aufgabe anlegen (Routines → New routine → Local/Remote), „Scheduled", „/schedule", `create_scheduled_task`/`update_scheduled_task` · „Failed to create scheduled task", „Cannot create scheduled tasks from within a scheduled task session", `scheduled-tasks.json`, High-Frequency-Cron-Freeze, Catch-up/Missed runs, „Keep computer awake", Cloud-Routine, `/loop`, `fireAt`, `host_not_allowed`, `wakeScheduler`, „Always allow"/bypassPermissions, MCP-Warm-up/Subagent, Dispatch/Mobile · Vertieft `cowork.md` §7 · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-cowork.md` §5 |
| **Claude Code Desktop-App vs. CLI** (Code-Tab macOS/Windows: Installation, PATH/Env, Hooks, Permissions, MCP, Worktrees, Computer-Use, Preview, Cloud/SSH, fehlende Features) | [`claude-tooling/claude-code-desktop-vs-cli.md`](claude-tooling/claude-code-desktop-vs-cli.md) | 2026-06-13 | ~45 | Code-Tab, „Git is required", „Git LFS", PATH/PowerShell-Profil, „bypassPermissions", „PostToolUse feuert nicht", `.claude/worktrees/`, „localhost was blocked", Accessibility/Screen-Recording, „isn't available in this environment", `--print`/Headless, Agent-Teams, `/agents`/`/doctor` · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-claude-code-desktop-vs-cli.md` |
| **Agenten-Wissens-/Lern-System** (Harness-Selbstverbesserung: Almanach/BP-Struktur, Ausloeser/Hooks, Lern-DBs, Direktiven) | [`claude-tooling/agent-knowledge-system.md`](claude-tooling/agent-knowledge-system.md) | 2026-06-15 | ~10 Fallen | **Querschnitt — KEIN Datei-Trigger** (wie `apis/`/`agents/`, in `check-guard-coverage.py`-Allowlist) · greift bei Arbeit an `bug-almanac-*`/`bug-case-auto-writer`/`subagent-context`-Hooks, `experience-store`/`trajectories`/`session-scores`/Pheromon, Almanach-/BP-Struktur, Direktiven-Umsetzung · „Wissenssystem", „Progressive Disclosure", „Memory-Governance", „Staleness", „semantischer Trigger", „Lernschleife", „Compound Intelligence", „Hook-Schema", „Tool-Drift" · Best-Practices: `best-practices/projekt-code/claude-tooling/best-practices-agent-knowledge-system.md` |

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
|---------|-------|-------|------|---------------------------------|
| **Boss-/Orchestrator-Agent** (Multi-Agenten-Systeme, Sub-Agent-Spawning, Intent, Tool-Calling) | [`agents/orchestrator-agent.md`](agents/orchestrator-agent.md) | 2026-06-09 | ~64 | „Orchestrator", „Boss-Agent", „Multi-Agenten", „Sub-Agent", „Worker", „Intent", „Tool-Calling", „LangGraph", „CrewAI", „Semantic Kernel", „Agent SDK", „VoiceAgent" · Best-Practices: `best-practices/projekt-code/agents/best-practices-orchestrator-agent.md` |