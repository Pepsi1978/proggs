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

# Android Development Rules

## Build System
- Use Gradle with Kotlin DSL (`build.gradle.kts`)
- Use version catalogs (`gradle/libs.versions.toml`) for all dependencies
- Target latest stable SDK (API 36 / Android 16) with minSdk 26+ (Android 8.0+)
- Use AGP 9.1.0+ (Android Gradle Plugin), Kotlin 2.3.0+, Compose BOM 2026.03.00
- Build command: `./gradlew assembleDebug` (debug APK) or `./gradlew assembleRelease` (signed release)
- Bundle command: `./gradlew bundleRelease` (AAB for Play Store)
- Format command: `./gradlew spotlessApply` (requires Spotless Gradle plugin with ktlint)
- Add Spotless to `build.gradle.kts`: `id("com.diffplug.spotless")` with `kotlin { ktlint() }` config

## Two Development Paths

### Path 1: Native Kotlin (preferred for Android-only apps)
- Language: Kotlin 2.3.0+ with Jetpack Compose (modern UI toolkit)
- Build: Gradle 8.14 + Android Gradle Plugin 9.1.0
- Compose: Use BOM `2026.03.00` (Compose UI 1.10.5, Material3 1.4.0)
- APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Test: `./gradlew test` (unit) + `./gradlew connectedAndroidTest` (device)
- Lint: `./gradlew lint`

### Path 2: .NET MAUI (for cross-platform C# apps)
- Language: C# with .NET MAUI XAML or Blazor Hybrid
- Build: `dotnet build -f net10.0-android`
- APK: `dotnet publish -f net10.0-android -c Release`
- Run on emulator: `dotnet run -p:AdbTarget=-e`
- Test: `dotnet test`
- Output: `bin/Release/net10.0-android/publish/*.apk`
- Signing: `-p:AndroidKeyStore=true -p:AndroidSigningKeyStore=myapp.keystore -p:AndroidSigningKeyAlias=myapp`

### Path 3: TypeScript (React Native/Expo or Tauri v2)
- Expo: `npx create-expo-app@latest myapp --template expo-template-blank-typescript`
- Expo APK: `eas build --profile preview --platform android`
- Tauri: `cargo tauri android init && cargo tauri android build --apk`
- Tauri uses TypeScript UI + Rust backend

### Path 4: Rust (cargo-ndk for libs, cargo-apk for full apps)
- Cross-compile: `cargo ndk -t arm64-v8a build --release`
- Targets installed: aarch64, armv7, x86_64, i686
- Full app: `cargo install cargo-apk && cargo apk build --release`

### Path 5: Go (gomobile + Gio UI)
- Build: `gomobile build -target android ./...` → direct .apk
- GUI: Use Gio UI (`gioui.org`) for Material Design apps

## Environment (Windows)
- JAVA_HOME: C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot
- ANDROID_HOME: %LOCALAPPDATA%\Android\Sdk
- ADB: %ANDROID_HOME%\platform-tools\adb.exe
- sdkmanager: %ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat
- bundletool: ~/bin/bundletool (v1.18.3, wraps bundletool-all.jar)
- Firebase CLI: firebase (v15.10.0, global npm)
- scrcpy: scrcpy (for device mirroring, installed via winget)
- cargo-audit: ~/.cargo/bin/cargo-audit (v0.22.1, for Rust dependency CVE scanning)
- ADB MCP: registered as 'android-adb' in Claude Code (npx android-debug-bridge-mcp)
- Kotlin/Gradle: use gradlew from project (no standalone install needed)

## APK Signing
- Debug: auto-signed with debug keystore (~/.android/debug.keystore)
- Release: create keystore with `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000`
- Use v2 + v3 + v4 signing: `apksigner sign --ks keystore.jks --v3-signing-enabled true --v4-signing-enabled true app.apk`
- NEVER commit keystores or signing passwords to git
- Store signing config in `local.properties` (gitignored)
- For Play Store: use Google Play App Signing (upload key managed by Google)

## Project Structure (Native Kotlin)
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

## Common Commands
- Install APK to device: `adb install app-debug.apk`
- View device logs: `adb logcat -s "MyTag"`
- List connected devices: `adb devices`
- Clear app data: `adb shell pm clear com.example.app`
- Screenshot: `adb shell screencap /sdcard/screenshot.png && adb pull /sdcard/screenshot.png`
- Screen record: `adb shell screenrecord /sdcard/demo.mp4 && adb pull /sdcard/demo.mp4`
- Install SDK package: `sdkmanager "platforms;android-36"`
- Mirror device: `scrcpy --serial emulator-5554`
- AAB test install: `bundletool build-apks --bundle=app.aab --output=app.apks && bundletool install-apks --apks=app.apks`

## UI Design — Multi-Device (Phone, Foldable, Tablet)
- Prefer Jetpack Compose over XML layouts (modern, declarative)
- Follow Material Design 3 Expressive guidelines (Android 16+)
- Support dark mode (`isSystemInDarkTheme()`)
- **ALWAYS design for 3 form factors**: Compact (phone), Medium (foldable), Expanded (tablet)
- Use `currentWindowAdaptiveInfo()` for WindowSizeClass — NEVER use `Configuration.orientation`
- Use `NavigationSuiteScaffold` for auto-switching Bottom Nav ↔ Navigation Rail
- Use `NavigableListDetailPaneScaffold` for list-detail patterns on tablets
- App must look polished and professional — store-quality on ALL screen sizes

### Key Adaptive Layout Dependencies (all in BOM)
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
- **Compact** (< 600dp): Phone portrait, foldable folded → single pane, bottom nav
- **Medium** (600–840dp): Foldable unfolded, small tablet → optional two pane, nav rail
- **Expanded** (≥ 840dp): Tablet, desktop → two pane, persistent nav rail/drawer

### Foldable Posture Detection
- Use `WindowInfoTracker.getOrCreate(context).windowLayoutInfo(activity)`
- Detect `FoldingFeature.State.HALF_OPENED` + `Orientation.HORIZONTAL` = Tabletop mode
- Detect `FoldingFeature.State.HALF_OPENED` + `Orientation.VERTICAL` = Book mode
- State in ViewModel — survives fold/unfold configuration changes

### Accompanist — DEPRECATED (do not use)
- Pager → use `HorizontalPager`/`VerticalPager` from foundation
- SwipeRefresh → use `PullToRefreshBox` from Material3
- Navigation Animation → native `NavHost` with AnimatedContent
- Only `accompanist-permissions` still active (no official replacement yet)

## Performance
- Use R8 (not ProGuard) for release builds — ProGuard rules files still compatible
- R8 is default since AGP 3.4+, ProGuard `proguard-android.txt` deprecated in AGP 9.0
- Use `getDefaultProguardFile("proguard-android-optimize.txt")` for R8
- Avoid blocking the main thread — use coroutines
- Use lazy loading for images (Coil 3.x preferred, or Glide)
- Profile with `adb shell dumpsys meminfo com.example.app`

## Emulator & AVDs (macOS Apple Silicon)
- **Phone**: `Pixel7_API35` (existing) — Compact layout testing
- **Foldable**: `PixelFold_API36` — fold/unfold, hinge-angle sensor, Medium layout
- **Tablet**: `PixelTablet_API36` — Expanded layout, two-pane, landscape
- **Resizable**: `Resizable_API36` — quick switching between all form factors
- Start: `emulator -avd PixelFold_API36`
- Fold/unfold via telnet: `telnet localhost 5554` → `auth <token>` → `fold` / `unfold`
- Hinge angle: `sensor set hinge-angle0 90:0:0` (tabletop mode)
- All AVDs use ARM64 + Google Play system images (no x86 on Apple Silicon)

## Screenshot Testing (no emulator needed)
- **Roborazzi** (preferred): `./gradlew recordRoborazziDebug` / `./gradlew verifyRoborazziDebug`
- **Paparazzi**: `./gradlew recordPaparazziDebug` / `./gradlew verifyPaparazziDebug`
- Test multiple form factors via `@Config(qualifiers = "w600dp-h960dp")` for foldable, `"w800dp-h1280dp"` for tablet

## Audio & Sound (Professional Quality)

### API Decision Tree
- **Low-latency (< 20ms)**: Oboe (C++ NDK) — `implementation("com.google.oboe:oboe:1.9.0")`
- **Short sound effects (< 5s)**: SoundPool — built-in, max 10 streams, OGG Vorbis preferred
- **Music/Streaming/Podcasts**: Jetpack Media3 — `implementation("androidx.media3:media3-exoplayer:1.6.0")`
- **Procedural audio**: TarsosDSP (Java/Kotlin) or MWEngine (C++/Kotlin)
- **3D spatial audio**: Spatializer API (API 32+)
- **Audio + haptics sync**: HapticGenerator on AudioTrack (API 31+)

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
- Golden rules for Oboe callback: NO memory allocation, NO I/O, NO mutexes, NO sleep — pure math only
- Set `PerformanceMode::LowLatency`, `SharingMode::Exclusive`, sample rate 48000 Hz
- Buffer size: `stream->getFramesPerBurst() * 2`

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

### Audio Formats (Best for Android)
- **OGG Vorbis** (128-192kbps): Best quality/size for effects and music — native since API 1
- **OGG Opus**: Best for speech/low-bitrate — native since API 21
- **WAV**: Development/raw only — too large for production
- **FLAC**: Lossless music — native since API 12

### Audio Focus (MANDATORY for playback apps)
- Always request AudioFocus before playback, release when done
- Media3 handles this automatically when using MediaSession
- Handle AUDIOFOCUS_LOSS_TRANSIENT (duck or pause) and AUDIOFOCUS_LOSS (stop)

### Free Sound Resources (Commercial Use OK)
- Zapsplat.com: 160,000+ royalty-free sounds
- Freesound.org: Creative Commons, OGG downloads
- SONNISS GameAudioGDC: Annual free pack, top quality
- Mixkit.co: Clean categories, game-focused

## Security
- Never hardcode API keys or secrets — use BuildConfig fields or local.properties
- **EncryptedSharedPreferences is deprecated** — use DataStore + Tink encryption for new code
- Use AndroidKeystore with `setIsStrongBoxBacked(true)` for hardware-backed encryption
- Enable network security config (`res/xml/network_security_config.xml`) — `cleartextTrafficPermitted="false"`
- Certificate pinning: only for high-risk apps, always set backup pin + expiration
- Use Play Integrity API (SafetyNet is deprecated)
- Use `dependencyGuard` plugin for supply chain protection
- Use `org.owasp.dependencycheck` for CVE scanning
- Static analysis: `detekt` with security rules + Android Lint security checks
