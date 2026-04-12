---
paths:
  - "**/*.swift"
  - "**/Package.swift"
  - "**/*.xcodeproj/**"
---

# Swift/macOS Development Rules

- Use Swift 6.2+ features (structured concurrency, actors, sendable)
- Target macOS 26+ with AppKit for desktop apps
- Use SwiftUI only when explicitly requested, default to AppKit
- Always add proper entitlements for sandboxed apps
- Build with: `swiftc -O` for release, `swift build` for SPM projects
- Sign with ad-hoc: `codesign --force --sign -`
- Format code: `swift-format --in-place` (installed via Homebrew)
- Lint code: `swiftlint` (installed via Homebrew)
- Run tests: `swift test` (SPM projects) or `xcodebuild test` (Xcode projects)
- Follow Apple Human Interface Guidelines for UI
