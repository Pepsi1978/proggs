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
- JAVA_HOME: C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot
- ANDROID_HOME: %LOCALAPPDATA%\Android\Sdk
- Kotlin: C:\Kotlin\kotlinc\bin
- Gradle: C:\Gradle\gradle-8.14\bin
- ADB: %ANDROID_HOME%\platform-tools\adb.exe
- sdkmanager: %ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat

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
- View device logs: `adb logcat`
- List connected devices: `adb devices`
- Clear app data: `adb shell pm clear com.example.app`
- Screenshot: `adb shell screencap /sdcard/screenshot.png && adb pull /sdcard/screenshot.png`
- Install SDK package: `sdkmanager "platforms;android-35"`
- Update all SDK: `sdkmanager --update`

## UI Design
- Prefer Jetpack Compose over XML layouts (modern, declarative)
- Follow Material Design 3 guidelines
- Support dark mode (`isSystemInDarkTheme()`)
- Test on multiple screen sizes
- Use ConstraintLayout for complex XML layouts
- App must look polished and professional — store-quality

## Performance
- Use R8 (not ProGuard) for release builds — 10-20% faster builds, better Kotlin support
- R8 is default since AGP 3.4+ — ProGuard rules files are still compatible
- Avoid blocking the main thread — use coroutines
- Use lazy loading for images (Coil or Glide)
- Profile with Android Profiler or `adb shell dumpsys`

## Emulator
- AVD: `avdmanager create avd -n Pixel9_API35 -k "system-images;android-35;google_apis;x86_64" --device "pixel_9"`
- Start: `emulator -avd Pixel9_API35`
- List devices: `avdmanager list device`
- Windows uses Hyper-V for acceleration (no HAXM needed)

## Security
- Never hardcode API keys or secrets
- Use EncryptedSharedPreferences for sensitive data
- Enable network security config for HTTPS-only
- Validate all user input
- Use SafetyNet/Play Integrity for device attestation
