---
paths:
  - "**/*.kt"
  - "**/*.kts"
  - "**/build.gradle.kts"
---

# Kotlin Rules

## Format
- Use `ktfmt` or IDE auto-format (Google style)
- 4-space indentation, no tabs
- Max line length: 120 characters

## Lint
- Use `detekt` for static analysis if configured in project
- Use Android Lint for Android-specific issues: `./gradlew lint`
- Use `kotlinc -Werror` to treat warnings as errors in CI

## Test
- Use `./gradlew test` for unit tests
- Use `./gradlew connectedAndroidTest` for instrumented tests
- Prefer JUnit 5 for pure Kotlin, JUnit 4 for Android (AndroidJUnit4)

## Build
- Use Gradle Kotlin DSL (`build.gradle.kts`) over Groovy DSL
- Target JVM 21 for non-Android, JVM 11+ for Android
- Use version catalogs (`libs.versions.toml`) for dependency management

## Conventions
- Prefer `data class` for DTOs and models
- Use `sealed class`/`sealed interface` for state hierarchies
- Prefer `val` over `var`, immutability by default
- Use coroutines for async work, not callbacks
- Use `Flow` for reactive streams
- File naming: PascalCase matching the primary class name
- Package naming: lowercase, no underscores
