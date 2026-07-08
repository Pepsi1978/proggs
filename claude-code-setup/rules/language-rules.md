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

# Programmiersprachen-Regeln (path-scoped)

- **C/C++:** C++20+. Build `cmake -B build -DCMAKE_BUILD_TYPE=Release`. Warnings `-Wall -Wextra -Werror` (MSVC `/W4 /WX`). Format `clang-format`, Lint `clang-tidy`, Test `ctest`, ASan.
- **C#/WPF:** .NET 10+, C# 14, WPF+MVVM. Build `dotnet publish -c Release -r win-x64 --self-contained`. Format `csharpier`/`dotnet format`, Test `dotnet test`, `dotnet list package --vulnerable`, Sign `signtool`.
- **Go:** 1.26+. Build `-ldflags="-s -w"`. Lint `golangci-lint`, Format `gofmt`.
- **Java:** Google Java Format, 4-Space, max 120. Lint `./gradlew lint`+SpotBugs, Test JUnit 5/4, JDK 21 (non-Android)/11+ (Android).
- **Kotlin:** ktfmt, 4-Space, max 120. Lint detekt+Android Lint, Test JUnit 5/4, Build Gradle KTS + Version Catalogs.
- **Rust:** 2024 Edition. Errors `thiserror`(lib)/`anyhow`(app), CLI `clap`. Build `cargo build --release` (cross `--target x86_64-pc-windows-gnu`). Lint `cargo clippy -- -D warnings`, Format `cargo fmt`, Test `cargo nextest`, Audit `cargo audit`+`cargo deny`.
- **Swift/macOS:** 6.2+. Target macOS 26+ AppKit. Build `swift build`/`swiftc -O`, Sign `codesign`, Format `swift-format`, Lint `swiftlint`, Test `swift test`.
- **TypeScript:** 5.9+ strict, Bun. Type-Check `tsc --noEmit`, ESM, Test `bun test`/`vitest`, Lint/Format `biome`.
