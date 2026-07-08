---
paths:
  - "**/*.kt"
  - "**/*.kts"
  - "**/*.cs"
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.js"
  - "**/*.rs"
  - "**/*.go"
  - "**/*.swift"
  - "**/*.java"
  - "**/*.cpp"
  - "**/*.c"
  - "**/*.h"
---

# Programmiersprachen-Regeln (Referenz, path-scoped)

- **C/C++:** C++20+, Smart Pointers, `std::string_view`. Build `cmake -B build -DCMAKE_BUILD_TYPE=Release`.
  Warnings `-Wall -Wextra -Werror` (MSVC `/W4 /WX`). Format `clang-format`, Lint `clang-tidy`, Test `ctest`, ASan.
- **C#/WPF:** .NET 10+, C# 14, WPF + MVVM (CommunityToolkit.Mvvm), app.manifest DPI. Build
  `dotnet publish -c Release -r win-x64 --self-contained`. Format `csharpier`/`dotnet format`, Test
  `dotnet test`, `dotnet list package --vulnerable`, Sign `signtool`.
- **Go:** 1.26+, Std-Lib, `fmt.Errorf`-Wrapping. Build `-ldflags="-s -w"`, cross-compile. Lint `golangci-lint`, Format `gofmt`.
- **Java:** Google Java Format, 4-Space, max 120. Lint `./gradlew lint`+SpotBugs, Test JUnit 5/4, JDK 21
  (non-Android)/11+ (Android). PascalCase/camelCase/UPPER_SNAKE.
- **Kotlin:** ktfmt, 4-Space, max 120. Lint detekt+Android Lint, Test JUnit 5/4, Build Gradle KTS + Version
  Catalogs. Bevorzugen `data class`, `sealed class`, `val`, Coroutines, Flow.
- **Rust:** 2024 Edition. Errors `thiserror`(lib)/`anyhow`(app), CLI `clap`. Build `cargo build --release`
  (cross `--target x86_64-pc-windows-gnu`). Lint `cargo clippy -- -D warnings`, Format `cargo fmt`, Test
  `cargo nextest`. `unsafe` meiden, `serde`/`tokio`, Audit `cargo audit`+`cargo deny`.
- **Swift/macOS:** 6.2+, structured concurrency, actors, sendable. Target macOS 26+ AppKit (SwiftUI nur
  auf Anfrage). Build `swift build`/`swiftc -O`, Sign `codesign`, Format `swift-format`, Lint `swiftlint`, Test `swift test`. HIG.
- **TypeScript:** 5.9+ strict, kein `any`, Bun bevorzugt. Web React/Svelte + Tailwind, Electron/Tauri
  native. Type-Check `tsc --noEmit`, ESM, Test `bun test`/`vitest`, Lint/Format `biome`.
