---
paths:
  - "**/*.java"
  - "**/pom.xml"
  - "**/build.gradle"
---

# Java Rules

## Format
- Use Google Java Format or `clang-format` with Google style
- 4-space indentation, no tabs
- Max line length: 120 characters

## Lint
- Use `./gradlew lint` for Android projects
- Use SpotBugs or Error Prone for static analysis
- Use `javac -Xlint:all` for compiler warnings

## Test
- Use JUnit 5 for pure Java, JUnit 4 for Android
- Test command: `./gradlew test` or `mvn test`
- Use Mockito for mocking

## Build
- Prefer Gradle over Maven for Android projects
- Target JDK 21 for non-Android, JDK 11+ for Android
- Use `javac --release 11` for Android compatibility

## Conventions
- Class names: PascalCase
- Method/variable names: camelCase
- Constants: UPPER_SNAKE_CASE
- Package names: lowercase (com.example.project)
- One public class per file, filename matches class name
- Prefer Kotlin over Java for new Android code
