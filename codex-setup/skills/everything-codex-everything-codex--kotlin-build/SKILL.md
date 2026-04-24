---
name: kotlin-build
description: Fix Kotlin/Gradle build errors, compiler warnings, and dependency issues incrementally. Invokes the kotlin-build-resolver agent for minimal, surgical fixes.
---

# Kotlin Build and Fix

This command invokes the **kotlin-build-resolver** agent to incrementally fix Kotlin build errors with minimal changes.

## What This Command Does

1. **Run Diagnostics**: Execute `./prompts:gradlew build`, `detekt`, `ktlintCheck`
2. **Parse Errors**: Group by file and sort by severity
3. **Fix Incrementally**: One error at a time
4. **Verify Each Fix**: Re-run build after each change
5. **Report Summary**: Show what was fixed and what remains

## When to Use

Use `/prompts:everything-codex-everything-codex--kotlin-build` when:
- `./prompts:gradlew build` fails with errors
- Kotlin compiler reports errors
- `./prompts:gradlew detekt` reports violations
- Gradle dependency resolution fails
- After pulling changes that break the build

## Diagnostic Commands Run

```bash
# Primary build check
./prompts:gradlew build 2>&1

# Static analysis
./prompts:gradlew detekt 2>&1 || echo "detekt not configured"
./prompts:gradlew ktlintCheck 2>&1 || echo "ktlint not configured"

# Dependency issues
./prompts:gradlew dependencies --configuration runtimeClasspath 2>&1 | head -100

# Optional deep refresh when caches or dependency metadata are suspect
./prompts:gradlew build --refresh-dependencies
```

## Example Session

````text
User: /prompts:everything-codex-everything-codex--kotlin-build

Agent:
# Kotlin Build Resolution

## Initial Diagnostics

```bash
$ ./prompts:gradlew build
e: src/main/kotlin/com/example/service/UserService.kt:25:15 Unresolved reference: UserRepository
e: src/main/kotlin/com/example/routes/UserRoutes.kt:42:9 Type mismatch: inferred type is String but Int was expected
e: src/main/kotlin/com/example/routes/UserRoutes.kt:58:5 'when' expression must be exhaustive
```

Errors found: 3

## Fix 1: Unresolved Reference

File: src/main/kotlin/com/example/service/UserService.kt:25
Error: Unresolved reference: UserRepository
Cause: Missing import

```kotlin
// Added import
import com.example.repository.UserRepository
```

```bash
$ ./prompts:gradlew build
# 2 errors remaining
```

## Fix 2: Type Mismatch

File: src/main/kotlin/com/example/routes/UserRoutes.kt:42
Error: Type mismatch: inferred type is String but Int was expected

```kotlin
// Changed
val count = call.parameters["count"]
// To
val count = call.parameters["count"]?.toIntOrNull()
    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid count")
```

```bash
$ ./prompts:gradlew build
# 1 error remaining
```

## Fix 3: Non-Exhaustive When

File: src/main/kotlin/com/example/routes/UserRoutes.kt:58
Error: 'when' expression must be exhaustive

```kotlin
// Added missing branch
when (user.role) {
    Role.ADMIN -> handleAdmin(user)
    Role.USER -> handleUser(user)
    Role.MODERATOR -> handleModerator(user) // Added
}
```

```bash
$ ./prompts:gradlew build
# Build successful!
```

## Final Verification

```bash
$ ./prompts:gradlew detekt
# No issues

$ ./prompts:gradlew test
# All tests passed
```

## Summary

| Metric | Count |
|--------|-------|
| Build errors fixed | 3 |
| Detekt issues fixed | 0 |
| Files modified | 2 |
| Remaining issues | 0 |

Build Status: ✅ SUCCESS
````

## Common Errors Fixed

| Error | Typical Fix |
|-------|-------------|
| `Unresolved reference: X` | Add import or dependency |
| `Type mismatch` | Fix type conversion or assignment |
| `'when' must be exhaustive` | Add missing sealed class branches |
| `Suspend function can only be called from coroutine` | Add `suspend` modifier |
| `Smart cast impossible` | Use local `val` or `let` |
| `None of the following candidates is applicable` | Fix argument types |
| `Could not resolve dependency` | Fix version or add repository |

## Fix Strategy

1. **Build errors first** - Code must compile
2. **Detekt violations second** - Fix code quality issues
3. **ktlint warnings third** - Fix formatting
4. **One fix at a time** - Verify each change
5. **Minimal changes** - Don't refactor, just fix

## Stop Conditions

The agent will stop and report if:
- Same error persists after 3 attempts
- Fix introduces more errors
- Requires architectural changes
- Missing external dependencies

## Related Commands

- `/prompts:everything-codex-everything-codex--kotlin-test` - Run tests after build succeeds
- `/prompts:everything-codex-everything-codex--kotlin-review` - Review code quality
- `/prompts:everything-codex-everything-codex--verify` - Full verification loop

## Related

- Agent: `agents/kotlin-build-resolver.md`
- Skill: `skills/kotlin-patterns/`
