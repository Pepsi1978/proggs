---
paths:
  - "**/AndroidManifest.xml"
  - "**/src/main/java/**"
  - "**/src/main/kotlin/**"
  - "**/src/main/res/**"
  - "**/build.gradle.kts"
  - "**/build.gradle"
  - "**/gradle/**"
  - "**/*.apk"
---

# Android-Entwicklung: Rules & Phasen-System (KRITISCH)

> Konsolidiert aus: android.md, android-phase-trigger.md

---

## 1. Phasen-System: Automatischer Trigger (PFLICHT)

Wenn die Aufgabe ein Android-Projekt betrifft, MUESSEN SOFORT am Anfang
diese Schritte ausgefuehrt werden — BEVOR Code gelesen/geschrieben wird:

### Schritt 1: SESSION-RULES.md lesen

```
Read: ~/proggs/BestJournalAndroid/SESSION-RULES.md   (Android-Version)
Read: ~/proggs/BestJournalFrank/SESSION-RULES.md     (Frank-Version)
```
Dort steht: Phase, Version, Build-Befehle, Geraete-IDs, Farben, Firebase-Projekt.

### Schritt 2: Stand zeigen

"BestJournalAndroid ist in Phase 2 (Debug & Entwicklung), Version v1.0.2."

### Schritt 3: Erst DANN arbeiten

### Trigger-Woerter (JEDES genuegt)

**Projekt-Namen:** BestJournal, Journal-App, Tagebuch-App, Frank-Version, beide Versionen
**Technologie:** Android, Kotlin, Compose, Jetpack, Hilt, Gradle, gradlew, Firebase, APK, AAB, ADB
**Phasen:** Phase 1-6, Debug-Version, Release-Build
**Geraete:** Fold 6, S23 Ultra, auf dem Handy, installieren
**Implizite Trigger:** Dashboard-Probleme, APK/Logcat-Output, App-spezifische Features (Whisper, Entropie, Sprachaufnahme, Drive-Backup)

---

## 2. Build System

- Gradle mit Kotlin DSL (`build.gradle.kts`)
- Version Catalogs (`gradle/libs.versions.toml`)
- Target: API 36 / Android 16, minSdk 26+ (Android 8.0+)
- AGP 9.1.0+, Kotlin 2.3.0+, Compose BOM 2026.03.00
- Build: `./gradlew assembleDebug` (Debug) / `./gradlew assembleRelease` (Release)
- Bundle: `./gradlew bundleRelease` (AAB fuer Play Store)
- Format: `./gradlew spotlessApply` (Spotless + ktlint)
- Spotless Setup: `id("com.diffplug.spotless")` mit `kotlin { ktlint() }` in `build.gradle.kts`

## 3. Project Structure (Native Kotlin)

```
app/
  src/
    main/
      java/com/example/app/    # Kotlin source files
      res/
        layout/                 # XML layouts (or skip with Compose)
        values/                 # strings.xml, colors.xml, themes.xml
        drawable/               # Icons, images
        mipmap/                 # App launcher icons
      AndroidManifest.xml       # App manifest
    test/                       # Unit tests
    androidTest/                # Instrumented tests
  build.gradle.kts              # Module build config
build.gradle.kts                # Root build config
settings.gradle.kts             # Project settings
gradle/
  libs.versions.toml            # Version catalog
```

## 4. Development Paths

### Path 1: Native Kotlin (bevorzugt)
- Kotlin 2.3.0+ mit Jetpack Compose
- Gradle 8.14 + AGP 9.1.0
- Compose BOM `2026.03.00` (Compose UI 1.10.5, Material3 1.4.0)
- APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Test: `./gradlew test` (Unit) + `./gradlew connectedAndroidTest` (Device)
- Lint: `./gradlew lint`

### Path 2: .NET MAUI (Cross-Platform C#)
- Build: `dotnet build -f net10.0-android`
- APK: `dotnet publish -f net10.0-android -c Release`
- Run on emulator: `dotnet run -p:AdbTarget=-e`
- Test: `dotnet test`
- Output: `bin/Release/net10.0-android/publish/*.apk`
- Signing: `-p:AndroidKeyStore=true -p:AndroidSigningKeyStore=myapp.keystore -p:AndroidSigningKeyAlias=myapp`

### Path 3: TypeScript (React Native/Expo oder Tauri v2)
- Expo: `npx create-expo-app@latest myapp --template expo-template-blank-typescript`
- Expo APK: `eas build --profile preview --platform android`
- Tauri: `cargo tauri android init && cargo tauri android build --apk`
- Tauri uses TypeScript UI + Rust backend

### Path 4: Rust (cargo-ndk)
- Cross-compile: `cargo ndk -t arm64-v8a build --release`
- Targets: aarch64, armv7, x86_64, i686
- Full app: `cargo install cargo-apk && cargo apk build --release`

### Path 5: Go (gomobile + Gio UI)
- Build: `gomobile build -target android ./...` → direct .apk
- GUI: Gio UI (`gioui.org`) fuer Material Design Apps

## 5. Environment (Windows)

- JAVA_HOME: `C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`
- ANDROID_HOME: `%LOCALAPPDATA%\Android\Sdk`
- ADB: `%ANDROID_HOME%\platform-tools\adb.exe`
- sdkmanager: `%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat`
- bundletool: `~/bin/bundletool` (v1.18.3, wraps bundletool-all.jar)
- Firebase CLI: `firebase` (v15.10.0, global npm)
- scrcpy: fuer Device Mirroring (winget)
- cargo-audit: `~/.cargo/bin/cargo-audit` (v0.22.1, Rust CVE scanning)
- ADB MCP: `android-adb` in Claude Code (npx android-debug-bridge-mcp)
- Kotlin/Gradle: gradlew aus Projekt verwenden (kein standalone Install noetig)

## 6. Common Commands

```bash
# APK installieren
adb install app-debug.apk

# Logcat filtern
adb logcat -s "MyTag"

# Verbundene Geraete
adb devices

# App-Daten loeschen
adb shell pm clear com.example.app

# Screenshot
adb shell screencap /sdcard/screenshot.png && adb pull /sdcard/screenshot.png

# Screen Recording
adb shell screenrecord /sdcard/demo.mp4 && adb pull /sdcard/demo.mp4

# SDK installieren
sdkmanager "platforms;android-36"

# Device spiegeln
scrcpy --serial emulator-5554

# AAB Test-Installation
bundletool build-apks --bundle=app.aab --output=app.apks && bundletool install-apks --apks=app.apks

# Memory profiling
adb shell dumpsys meminfo com.example.app
```

## 7. APK Signing

- **Debug:** auto-signed mit debug keystore (`~/.android/debug.keystore`)
- **Release:** `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000`
- v2 + v3 + v4 Signing: `apksigner sign --ks keystore.jks --v3-signing-enabled true --v4-signing-enabled true app.apk`
- NEVER commit keystores oder Signing-Passwoerter
- Signing-Config in `local.properties` (gitignored)
- Play Store: Google Play App Signing (Upload-Key von Google verwaltet)

## 8. UI Design — Multi-Device

- **Jetpack Compose** bevorzugt (modern, deklarativ)
- Material Design 3 Expressive (Android 16+)
- Dark Mode (`isSystemInDarkTheme()`)
- **3 Form-Faktoren:** Compact (Phone), Medium (Foldable), Expanded (Tablet)
- App muss auf ALLEN Screen-Groessen poliert und professionell aussehen — Store-Qualitaet
- `currentWindowAdaptiveInfo()` fuer WindowSizeClass — NIEMALS `Configuration.orientation`
- `NavigationSuiteScaffold` fuer Nav-Switching (Bottom Nav ↔ Navigation Rail)
- `NavigableListDetailPaneScaffold` fuer List-Detail auf Tablets

### Key Adaptive Layout Dependencies (alle in BOM)

```kotlin
// All managed by Compose BOM — no version needed
implementation(platform("androidx.compose:compose-bom:2026.03.00"))
implementation("androidx.compose.material3.adaptive:adaptive")
implementation("androidx.compose.material3.adaptive:adaptive-layout")
implementation("androidx.compose.material3.adaptive:adaptive-navigation")
implementation("androidx.compose.material3:material3-window-size-class")
implementation("androidx.window:window:1.5.1")  // Foldable posture APIs
```

### WindowSizeClass Breakpoints

- **Compact** (< 600dp): Phone, foldable folded → Single Pane, Bottom Nav
- **Medium** (600-840dp): Foldable unfolded, kleines Tablet → Optional Two Pane, Nav Rail
- **Expanded** (≥ 840dp): Tablet, Desktop → Two Pane, Persistent Nav Rail/Drawer

### Foldable Posture Detection

- `WindowInfoTracker.getOrCreate(context).windowLayoutInfo(activity)`
- `FoldingFeature.State.HALF_OPENED` + `Orientation.HORIZONTAL` = Tabletop Mode
- `FoldingFeature.State.HALF_OPENED` + `Orientation.VERTICAL` = Book Mode
- State in ViewModel — survives fold/unfold configuration changes

### Accompanist — DEPRECATED (nicht verwenden)

- Pager → `HorizontalPager`/`VerticalPager` (foundation)
- SwipeRefresh → `PullToRefreshBox` (Material3)
- Navigation Animation → native `NavHost` mit AnimatedContent
- Nur `accompanist-permissions` noch aktiv (kein offizieller Ersatz)

## 9. Audio & Sound (Professional Quality)

### API Decision Tree

- **Low-latency (<20ms):** Oboe (C++ NDK) — `com.google.oboe:oboe:1.9.0`
- **Kurze Effekte (<5s):** SoundPool — built-in, max 10 streams, OGG Vorbis bevorzugt
- **Music/Streaming:** Jetpack Media3 — `media3-exoplayer:1.6.0`
- **Procedural Audio:** TarsosDSP (Java/Kotlin) oder MWEngine (C++/Kotlin)
- **3D Spatial:** Spatializer API (API 32+)
- **Audio + Haptics Sync:** HapticGenerator auf AudioTrack (API 31+)

### Oboe Setup (C++ Low-Latency)

```cmake
# CMakeLists.txt
find_package(oboe REQUIRED CONFIG)
target_link_libraries(native-lib oboe::oboe)
```

```kotlin
// build.gradle.kts
android {
    defaultConfig {
        externalNativeBuild {
            cmake { arguments("-DANDROID_STL=c++_static"); cppFlags("-std=c++17") }
        }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
}
dependencies { implementation("com.google.oboe:oboe:1.9.0") }
```

- Golden Rules fuer Oboe Callback: KEINE Memory-Allokation, KEIN I/O, KEINE Mutexes, KEIN Sleep — nur pure Mathematik
- `PerformanceMode::LowLatency`, `SharingMode::Exclusive`, Sample Rate 48000 Hz
- Buffer Size: `stream->getFramesPerBurst() * 2`

### SoundPool (Short Effects)

```kotlin
val soundPool = SoundPool.Builder()
    .setMaxStreams(10)
    .setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
    .build()
val soundId = soundPool.load(context, R.raw.click, 1)
soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
```

### Jetpack Media3 (Music/Streaming)

```kotlin
val media3Version = "1.6.0"
implementation("androidx.media3:media3-exoplayer:$media3Version")
implementation("androidx.media3:media3-session:$media3Version")
implementation("androidx.media3:media3-common-ktx:$media3Version") // Kotlin Coroutines
```

### Audio-Formate (Best for Android)

- **OGG Vorbis** (128-192kbps): Beste Qualitaet/Groesse fuer Effekte und Musik — nativ seit API 1
- **OGG Opus**: Beste fuer Speech/Low-Bitrate — nativ seit API 21
- **WAV**: Nur Development/Raw — zu gross fuer Produktion
- **FLAC**: Lossless Musik — nativ seit API 12

### Audio Focus (PFLICHT fuer Playback-Apps)

- IMMER AudioFocus anfordern vor Playback, freigeben wenn fertig
- Media3 handhabt das automatisch bei Verwendung von MediaSession
- AUDIOFOCUS_LOSS_TRANSIENT behandeln (duck oder pause) und AUDIOFOCUS_LOSS (stop)

### Free Sound Resources (Commercial Use OK)

- **Zapsplat.com**: 160.000+ Royalty-Free Sounds
- **Freesound.org**: Creative Commons, OGG Downloads
- **SONNISS GameAudioGDC**: Jaehrliches Free Pack, Top-Qualitaet
- **Mixkit.co**: Saubere Kategorien, Game-fokussiert

## 10. Screenshot Testing (kein Emulator noetig)

- **Roborazzi** (bevorzugt): `./gradlew recordRoborazziDebug` / `./gradlew verifyRoborazziDebug`
- **Paparazzi**: `./gradlew recordPaparazziDebug` / `./gradlew verifyPaparazziDebug`
- Mehrere Form-Faktoren via `@Config(qualifiers = "w600dp-h960dp")` fuer Foldable, `"w800dp-h1280dp"` fuer Tablet

## 11. Performance

- R8 (nicht ProGuard) fuer Release-Builds — ProGuard rules-Dateien kompatibel
- R8 ist Default seit AGP 3.4+, ProGuard `proguard-android.txt` deprecated in AGP 9.0
- `getDefaultProguardFile("proguard-android-optimize.txt")` fuer R8
- Main-Thread-Blocking vermeiden — Coroutines verwenden
- Lazy Loading: Coil 3.x (bevorzugt) oder Glide
- Profiling: `adb shell dumpsys meminfo com.example.app`

## 12. Emulator & AVDs (macOS Apple Silicon)

| AVD | Zweck |
|-----|-------|
| `Pixel7_API35` | Compact Layout Testing |
| `PixelFold_API36` | Fold/Unfold, Hinge-Angle, Medium Layout |
| `PixelTablet_API36` | Expanded Layout, Two-Pane, Landscape |
| `Resizable_API36` | Quick-Switching alle Form-Faktoren |

- Alle ARM64 + Google Play System Images
- Start: `emulator -avd PixelFold_API36`
- Fold/Unfold via Telnet: `telnet localhost 5554` → `auth <token>` → `fold` / `unfold`
- Hinge Angle: `sensor set hinge-angle0 90:0:0` (Tabletop Mode)

## 13. Security

- Keine API-Keys hardcoden — BuildConfig oder local.properties
- **EncryptedSharedPreferences deprecated** → DataStore + Tink
- AndroidKeystore mit `setIsStrongBoxBacked(true)` fuer Hardware-backed Encryption
- Network Security Config: `cleartextTrafficPermitted="false"`
- Certificate Pinning: nur fuer High-Risk Apps, immer Backup Pin + Expiration
- Play Integrity API (SafetyNet deprecated)
- `dependencyGuard` Plugin fuer Supply Chain Protection
- `org.owasp.dependencycheck` fuer CVE Scanning
- Static Analysis: `detekt` mit Security-Rules + Android Lint Security Checks
