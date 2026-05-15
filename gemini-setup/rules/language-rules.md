# Programmiersprachen-Regeln (Referenz)

> Konsolidiert aus: cpp, csharp, go, java, kotlin, rust, swift, typescript, tampermonkey

---

## C/C++

- Modern C++ (C++20+) bevorzugen
- Smart Pointers statt Raw Pointers (`unique_ptr`, `shared_ptr`)
- `std::string_view` fuer non-owning References
- Build: `cmake -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build`
- Warnings: `-Wall -Wextra -Werror` (MSVC: `/W4 /WX`)
- Cross-compile Windows: MinGW oder CMake Toolchain
- Format: `clang-format -i <file>`
- Lint: `clang-tidy <file> -- -std=c++20`
- Test: `ctest --test-dir build`
- Audit: AddressSanitizer (`-fsanitize=address`)

---

## C# / WPF

- .NET 10+, C# 14 (primary constructors, collection expressions)
- Target Windows 10+ mit WPF, MVVM mit CommunityToolkit.Mvvm
- app.manifest fuer DPI-Awareness
- Build: `dotnet build -c Release`
- Publish: `dotnet publish -c Release -r win-x64 --self-contained`
- Fluent Design System
- Format: `csharpier .` oder `dotnet format`
- Lint: `dotnet format analyzers`
- Test: `dotnet test`
- Vulnerabilities: `dotnet list package --vulnerable --include-transitive`
- Outdated: `dotnet outdated`
- Signing: `signtool sign /fd SHA256 /a MyApp.exe`

---

## Go

- Go 1.26+ Features, Standard Library bevorzugen
- Error Handling: `fmt.Errorf` wrapping
- Build: `-ldflags="-s -w"`, Cross-compile fuer Windows/macOS
- Lint: `golangci-lint`
- Format: `gofmt`

---

## Java

- Google Java Format, 4-Space Indentation, max 120 Chars
- Lint: `./gradlew lint`, SpotBugs
- Test: JUnit 5 (pure Java) / JUnit 4 (Android)
- Build: JDK 21 (non-Android) / JDK 11+ (Android)
- Naming: PascalCase Klassen, camelCase Methoden, UPPER_SNAKE_CASE Konstanten

---

## Kotlin

- ktfmt Formatting, 4-Space Indentation, max 120 Chars
- Lint: detekt + Android Lint
- Test: JUnit 5 (pure Kotlin) / JUnit 4 (Android)
- Build: Gradle Kotlin DSL (`build.gradle.kts`), Version Catalogs
- Bevorzugen: `data class`, `sealed class`, `val` ueber `var`, Coroutines, Flow

---

## Rust

- Rust 2024 Edition, latest stable Features
- Errors: `thiserror` (Library), `anyhow` (Application)
- CLI: `clap` fuer Argument Parsing
- Build Release: `cargo build --release`
- Cross-compile Windows: `cargo build --release --target x86_64-pc-windows-gnu`
- Lint: `cargo clippy -- -D warnings`
- Format: `cargo fmt`
- Test: `cargo nextest run` (schneller) oder `cargo test`
- Watch: `cargo watch -x check -x test`
- `unsafe` vermeiden ausser performance-kritisch
- Serialization: `serde`, Async: `tokio`
- Audit: `cargo audit` (CVE-Check), `cargo deny check` (Lizenzen)

---

## Swift / macOS

- Swift 6.2+, structured concurrency, actors, sendable
- Target macOS 26+ mit AppKit (SwiftUI nur wenn explizit angefragt)
- Entitlements fuer sandboxed Apps
- Build: `swiftc -O` (Release), `swift build` (SPM)
- Sign: `codesign --force --sign -` (ad-hoc)
- Format: `swift-format --in-place`
- Lint: `swiftlint`
- Test: `swift test` (SPM) / `xcodebuild test` (Xcode)
- Apple Human Interface Guidelines fuer UI

---

## TypeScript

- TypeScript 5.9+ strict mode, kein `any`
- Runtime: Bun bevorzugt ueber Node.js
- Web UI: React/Svelte/Vanilla + Tailwind CSS
- Electron/Tauri: Native Look & Feel
- CLI: Minimale Dependencies, compiled single binary wenn moeglich
- Type-Check: `tsc --noEmit`
- ESM bevorzugt ueber CommonJS
- Test: `bun test` oder `npx vitest`
- Lint: `bunx biome check .`
- Format: `bunx biome format --write .` oder `npx prettier --write .`

---

## Tampermonkey Scripts

- **IMMER** Version-Nummern bumpen bei JEDER Aenderung (sowohl @name als auch @version)
- Commit-Format: `#NNN - Description` (sequentielle Nummerierung)
- Repository: https://github.com/Pepsi1978/proggs
- Gemeinsame UI-Patterns (Buttons, Overlays) konsistent halten
- Lint: `bunx biome check <file>`
- Format: `bunx biome format --write <file>`
- Test: Manuell im Browser testen (kein automatisiertes Framework)
