---
name: android-ninja
description: Create production-quality Android applications following Google's official Android architecture guidance with Kotlin, Jetpack Compose, MVVM architecture, Hilt dependency injection, Room database, and multi-module architecture. Triggers on requests to create Android projects, modules, screens, ViewModels, repositories, or when asked about Android architecture patterns and best practices.
---
# Android Kotlin Compose Development

Create production-quality Android applications following Google's official architecture guidance and best practices.
Use when building Android apps with Kotlin, Jetpack Compose, MVVM architecture, Hilt dependency injection, Room database, or Android multi-module projects.
Triggers on requests to create Android projects, screens, ViewModels, repositories, feature modules, or when asked about Android architecture patterns.


## Quick Reference

| Task                                                 | Reference File                                                  |
|------------------------------------------------------|-----------------------------------------------------------------|
| Project structure & modules                          | [modularization.md](references/modularization.md)               |
| Architecture layers (Presentation, Domain, Data, UI) | [architecture.md](references/architecture.md)                   |
| Compose patterns, animation, effects, modifiers      | [compose-patterns.md](references/compose-patterns.md)           |
| Accessibility & TalkBack support                     | [android-accessibility.md](references/android-accessibility.md) |
| Notifications & foreground services                  | [android-notifications.md](references/android-notifications.md) |
| Data sync & offline-first patterns                   | [android-data-sync.md](references/android-data-sync.md)         |
| Material 3 theming & dynamic colors                  | [android-theming.md](references/android-theming.md)             |
| Navigation3 & adaptive navigation                    | [android-navigation.md](references/android-navigation.md)       |
| Kotlin best practices                                | [kotlin-patterns.md](references/kotlin-patterns.md)             |
| Coroutines best practices                            | [coroutines-patterns.md](references/coroutines-patterns.md)     |
| Gradle & build configuration                         | [gradle-setup.md](references/gradle-setup.md)                   |
| Testing approach                                     | [testing.md](references/testing.md)                             |
| Internationalization & localization                  | [android-i18n.md](references/android-i18n.md)                   |
| Icons, graphics, and custom drawing                  | [android-graphics.md](references/android-graphics.md)           |
| Runtime permissions                                  | [android-permissions.md](references/android-permissions.md)     |
| Kotlin delegation patterns                           | [kotlin-delegation.md](references/kotlin-delegation.md)         |
| Crash reporting                                      | [crashlytics.md](references/crashlytics.md)                     |
| StrictMode guardrails                                | [android-strictmode.md](references/android-strictmode.md)       |
| Multi-module dependencies                            | [dependencies.md](references/dependencies.md)                   |
| Code quality (Detekt)                                | [code-quality.md](references/code-quality.md)                   |
| Code coverage (JaCoCo)                               | [android-code-coverage.md](references/android-code-coverage.md) |
| Security (encryption, biometrics, pinning)           | [android-security.md](references/android-security.md)           |
| Design patterns                                      | [design-patterns.md](references/design-patterns.md)             |
| Android performance, recomposition & app startup     | [android-performance.md](references/android-performance.md)     |

## Workflow Decision Tree

**Creating a new project?**
ÔåÆ Start with `templates/settings.gradle.kts.template` for settings and module includes  
ÔåÆ Start with `templates/libs.versions.toml.template` for the version catalog  
ÔåÆ Copy all files from `templates/convention/` to `build-logic/convention/src/main/kotlin/`  
ÔåÆ Create `build-logic/settings.gradle.kts` (see `templates/convention/QUICK_REFERENCE.md`)  
ÔåÆ Add `includeBuild("build-logic")` to root `settings.gradle.kts`  
ÔåÆ Add plugin entries to `gradle/libs.versions.toml` (see `templates/convention/QUICK_REFERENCE.md`)  
ÔåÆ Copy `templates/proguard-rules.pro.template` to `app/proguard-rules.pro`  
ÔåÆ Read [modularization.md](references/modularization.md) for structure and module types  
ÔåÆ Use [gradle-setup.md](references/gradle-setup.md) for build files and build logic  

**Configuring Gradle/build files?**
ÔåÆ Use [gradle-setup.md](references/gradle-setup.md) for module `build.gradle.kts` patterns  
ÔåÆ Copy convention plugins from `templates/convention/` to `build-logic/` in your project  
ÔåÆ See `templates/convention/QUICK_REFERENCE.md` for setup instructions and examples  
ÔåÆ Copy `templates/proguard-rules.pro.template` to `app/proguard-rules.pro` for R8 rules  

**Setting up code quality / Detekt?**
ÔåÆ Use [code-quality.md](references/code-quality.md) for Detekt convention plugin setup  
ÔåÆ Start from `templates/detekt.yml.template` for rules and enable Compose rules  

**Adding or updating dependencies?**
ÔåÆ Follow [dependencies.md](references/dependencies.md)  
ÔåÆ Update `templates/libs.versions.toml.template` if the dependency is missing  

**Adding a new feature/module?**
ÔåÆ Follow module naming in [modularization.md](references/modularization.md)  
ÔåÆ Implement Presentation in the feature module  
ÔåÆ Follow dependency flow: Feature ÔåÆ Core/Domain ÔåÆ Core/Data

**Building UI screens/components?**
ÔåÆ Read [compose-patterns.md](references/compose-patterns.md) for screen architecture, state, components, modifiers  
ÔåÆ Use [android-theming.md](references/android-theming.md) for Material 3 colors, typography, and shapes  
ÔåÆ **Always** align Kotlin code with [kotlin-patterns.md](references/kotlin-patterns.md)  
ÔåÆ Create Screen + ViewModel + UiState in the feature module  
ÔåÆ Use shared components from `core/ui` when possible

**Setting up app theme (colors, typography, shapes)?**
ÔåÆ Follow [android-theming.md](references/android-theming.md) for Material 3 theming and dynamic colors  
ÔåÆ Use semantic color roles from `MaterialTheme.colorScheme` (never hardcoded colors)  
ÔåÆ Support light/dark themes with user preference toggle  
ÔåÆ Enable dynamic color (Material You) for API 31+  

**Writing any Kotlin code?**
ÔåÆ **Always** follow [kotlin-patterns.md](references/kotlin-patterns.md)  
ÔåÆ Ensure practices align with [architecture.md](references/architecture.md), [modularization.md](references/modularization.md), and [compose-patterns.md](references/compose-patterns.md)

**Setting up data/domain layers?**
ÔåÆ Read [architecture.md](references/architecture.md)  
ÔåÆ Create Repository interfaces in `core/domain`
ÔåÆ Implement Repository in `core/data`
ÔåÆ Create DataSource + DAO in `core/data`

**Implementing offline-first or data synchronization?**
ÔåÆ Follow [android-data-sync.md](references/android-data-sync.md) for sync strategies, conflict resolution, and cache invalidation  
ÔåÆ Use Room as single source of truth with sync metadata (syncStatus, lastModified)  
ÔåÆ Schedule background sync with WorkManager  
ÔåÆ Monitor network state before syncing  

**Setting up navigation?**
ÔåÆ Follow [android-navigation.md](references/android-navigation.md) for Navigation3 architecture, state management, and adaptive navigation  
ÔåÆ See [modularization.md](references/modularization.md) for feature module navigation components (Destination, Navigator, Graph)  
ÔåÆ Configure navigation graph in the app module  
ÔåÆ Use feature navigation destinations and navigator interfaces  

**Adding tests?**
ÔåÆ Use [testing.md](references/testing.md) for patterns and examples  
ÔåÆ Keep test doubles in `core/testing`  

**Handling runtime permissions?**
ÔåÆ Follow [android-permissions.md](references/android-permissions.md) for manifest declarations and Compose permission patterns  
ÔåÆ Request permissions contextually and handle "Don't ask again" flows  

**Showing notifications or foreground services?**
ÔåÆ Use [android-notifications.md](references/android-notifications.md) for notification channels, styles, actions, and foreground services  
ÔåÆ Check POST_NOTIFICATIONS permission on API 33+ before showing notifications  
ÔåÆ Create notification channels at app startup (required for API 26+)  

**Sharing logic across ViewModels or avoiding base classes?**
ÔåÆ Use delegation via interfaces as described in [kotlin-delegation.md](references/kotlin-delegation.md)  
ÔåÆ Prefer small, injected delegates for validation, analytics, or feature flags  

**Adding crash reporting / monitoring?**
ÔåÆ Follow [crashlytics.md](references/crashlytics.md) for provider-agnostic interfaces and module placement  
ÔåÆ Use DI bindings to swap between Firebase Crashlytics or Sentry  

**Enabling StrictMode guardrails?**
ÔåÆ Follow [android-strictmode.md](references/android-strictmode.md) for app-level setup and Compose compiler diagnostics  
ÔåÆ Use Sentry/Firebase init from [crashlytics.md](references/crashlytics.md) to ship StrictMode logs  

**Choosing design patterns for a new feature, business logic, or system?**
ÔåÆ Use [design-patterns.md](references/design-patterns.md) for Android-focused pattern guidance  
ÔåÆ Align with [architecture.md](references/architecture.md) and [modularization.md](references/modularization.md)  

**Measuring performance regressions or startup/jank?**
ÔåÆ Use [android-performance.md](references/android-performance.md) for Macrobenchmark setup and commands  
ÔåÆ Keep benchmark module aligned with `benchmark` build type in [gradle-setup.md](references/gradle-setup.md)  

**Setting up app initialization or splash screen?**
ÔåÆ Follow [android-performance.md](references/android-performance.md) ÔåÆ "App Startup & Initialization" for App Startup library, lazy init, and splash screen  
ÔåÆ Avoid ContentProvider-based auto-initialization - use `Initializer` interface instead  
ÔåÆ Use `installSplashScreen()` with `setKeepOnScreenCondition` for loading state  

**Adding icons, images, or custom graphics?**
ÔåÆ Use [android-graphics.md](references/android-graphics.md) for Material Symbols icons and custom drawing  
ÔåÆ Download icons via Iconify API or Google Fonts (avoid deprecated `Icons.Default.*` library)  
ÔåÆ Use `Modifier.drawWithContent`, `drawBehind`, or `drawWithCache` for custom graphics  

**Creating custom UI effects (glow, shadows, gradients)?**
ÔåÆ Check [android-graphics.md](references/android-graphics.md) for Canvas drawing, BlendMode, and Palette API patterns  
ÔåÆ Use `rememberInfiniteTransition` for animated effects  

**Ensuring accessibility compliance (TalkBack, touch targets, color contrast)?**
ÔåÆ Follow [android-accessibility.md](references/android-accessibility.md) for semantic properties and WCAG guidelines  
ÔåÆ Provide `contentDescription` for all icons and images  
ÔåÆ Ensure 48dp ├ù 48dp minimum touch targets  
ÔåÆ Test with TalkBack and Accessibility Scanner  

**Working with images and color extraction?**
ÔåÆ Use [android-graphics.md](references/android-graphics.md) for Palette API and Coil3 integration  
ÔåÆ Extract colors from images for dynamic theming  

**Implementing complex coroutine flows or background work?**
ÔåÆ Follow [coroutines-patterns.md](references/coroutines-patterns.md) for structured concurrency patterns  
ÔåÆ Use appropriate dispatchers (IO, Default, Main) and proper cancellation handling  
ÔåÆ Prefer `StateFlow`/`SharedFlow` over channels for state management  
ÔåÆ Use `callbackFlow` to wrap Android callback APIs (connectivity, sensors, location) into Flow  
ÔåÆ Use `suspendCancellableCoroutine` for one-shot callbacks (Play Services tasks, biometrics)  
ÔåÆ Use `combine()` to merge multiple Flows in ViewModels, `shareIn` to share expensive upstream  
ÔåÆ Handle backpressure with `buffer`, `conflate`, `debounce`, or `sample`  

**Need to share behavior across multiple classes?**
ÔåÆ Use [kotlin-delegation.md](references/kotlin-delegation.md) for interface delegation patterns  
ÔåÆ Avoid base classes; prefer composition with delegated interfaces  
ÔåÆ Examples: Analytics, FormValidator, CrashReporter  

**Refactoring existing code or improving architecture?**
ÔåÆ Review [architecture.md](references/architecture.md) for layer responsibilities  
ÔåÆ Check [design-patterns.md](references/design-patterns.md) for applicable patterns  
ÔåÆ Follow [kotlin-patterns.md](references/kotlin-patterns.md) for Kotlin-specific improvements  
ÔåÆ Ensure compliance with [modularization.md](references/modularization.md) dependency rules  

**Debugging performance issues or memory leaks?**
ÔåÆ Enable [android-strictmode.md](references/android-strictmode.md) for development builds  
ÔåÆ Use [android-performance.md](references/android-performance.md) for profiling and benchmarking  
ÔåÆ Check [coroutines-patterns.md](references/coroutines-patterns.md) for coroutine cancellation patterns  

**Setting up CI/CD or code quality checks?**
ÔåÆ Use [code-quality.md](references/code-quality.md) for Detekt baseline and CI integration  
ÔåÆ Use [gradle-setup.md](references/gradle-setup.md) for build cache and convention plugins  
ÔåÆ Use [testing.md](references/testing.md) for test organization and coverage  

**Handling sensitive data or privacy concerns?**
ÔåÆ Follow [crashlytics.md](references/crashlytics.md) for data scrubbing patterns  
ÔåÆ Use [android-permissions.md](references/android-permissions.md) for proper permission justification  
ÔåÆ Check [android-strictmode.md](references/android-strictmode.md) for detecting cleartext network traffic  

**Migrating legacy code (LiveData, Fragments, Accompanist)?**
ÔåÆ Replace LiveData with StateFlow using [coroutines-patterns.md](references/coroutines-patterns.md)  
ÔåÆ Replace Fragments with Compose screens using [compose-patterns.md](references/compose-patterns.md)  
ÔåÆ Replace Accompanist with official APIs per [compose-patterns.md](references/compose-patterns.md) ÔåÆ "Deprecated Patterns & Migrations"  
ÔåÆ Update navigation to Navigation3 using [android-navigation.md](references/android-navigation.md)  
ÔåÆ Follow [architecture.md](references/architecture.md) for modern MVVM patterns  

**Adding Compose animations?**
ÔåÆ Use [compose-patterns.md](references/compose-patterns.md) ÔåÆ "Animation" for `AnimatedVisibility`, `AnimatedContent`, `animate*AsState`, `Animatable`, shared elements  
ÔåÆ Use `graphicsLayer` for GPU-accelerated transforms (no recomposition)  
ÔåÆ Always provide `label` parameter for Layout Inspector debugging  

**Using side effects (LaunchedEffect, DisposableEffect)?**
ÔåÆ Use [compose-patterns.md](references/compose-patterns.md) ÔåÆ "Side Effects" for effect selection guide  
ÔåÆ `LaunchedEffect(key)` for state-driven coroutines, `rememberCoroutineScope` for event-driven  
ÔåÆ `DisposableEffect` for listener/resource cleanup, always include `onDispose`  
ÔåÆ `LifecycleResumeEffect` for onResume/onPause work (camera, media), `LifecycleStartEffect` for onStart/onStop (location, sensors)  

**Working with Modifier ordering or custom modifiers?**
ÔåÆ Use [compose-patterns.md](references/compose-patterns.md) ÔåÆ "Modifiers" for chain ordering rules and patterns  
ÔåÆ Use `Modifier.Node` for custom modifiers (not deprecated `Modifier.composed`)  
ÔåÆ Order: size ÔåÆ padding ÔåÆ drawing ÔåÆ interaction  

**Migrating from Accompanist or deprecated Compose APIs?**
ÔåÆ Use [compose-patterns.md](references/compose-patterns.md) ÔåÆ "Deprecated Patterns & Migrations"  
ÔåÆ Replace Accompanist libraries with official Foundation/Material3 equivalents  
ÔåÆ Use `collectAsStateWithLifecycle` instead of `collectAsState`  
ÔåÆ Use `mutableIntStateOf` instead of `mutableStateOf(0)` for primitives  

**Optimizing Compose recomposition or stability?**
ÔåÆ Use [compose-patterns.md](references/compose-patterns.md) for `@Immutable`/`@Stable` annotations  
ÔåÆ Use [android-performance.md](references/android-performance.md) ÔåÆ "Compose Recomposition Performance" for three phases, deferred state reads, Strong Skipping Mode  
ÔåÆ Check [gradle-setup.md](references/gradle-setup.md) for Compose Compiler metrics and stability reports  
ÔåÆ Use [kotlin-patterns.md](references/kotlin-patterns.md) for immutable data structures  

**Working with databases (Room)?**
ÔåÆ Define DAOs and entities in `core/database` per [modularization.md](references/modularization.md)  
ÔåÆ Use [testing.md](references/testing.md) for in-memory database testing and migration tests  
ÔåÆ Follow [architecture.md](references/architecture.md) for repository patterns with Room  

**Need internationalization/localization (i18n/l10n)?**
ÔåÆ Use [android-i18n.md](references/android-i18n.md) for string resources, plurals, and RTL support  
ÔåÆ Follow [compose-patterns.md](references/compose-patterns.md) for RTL-aware Compose layouts  
ÔåÆ Use [testing.md](references/testing.md) for locale-specific testing  

**Implementing network calls (Retrofit)?**
ÔåÆ Define API interfaces in `core/network` per [modularization.md](references/modularization.md)  
ÔåÆ Use [architecture.md](references/architecture.md) for RemoteDataSource patterns  
ÔåÆ Follow [dependencies.md](references/dependencies.md) for Retrofit, OkHttp, and serialization setup  
ÔåÆ Handle errors with generic `Result<T>` from [kotlin-patterns.md](references/kotlin-patterns.md)  

**Creating custom lint rules or code checks?**
ÔåÆ Use [code-quality.md](references/code-quality.md) for Detekt custom rules  
ÔåÆ Follow [gradle-setup.md](references/gradle-setup.md) for convention plugin setup  
ÔåÆ Check [android-strictmode.md](references/android-strictmode.md) for runtime checks

**Need code coverage reporting?**
ÔåÆ Use [android-code-coverage.md](references/android-code-coverage.md) for JaCoCo setup
ÔåÆ Follow [testing.md](references/testing.md) for test strategies
ÔåÆ Check [gradle-setup.md](references/gradle-setup.md) for convention plugin integration

**Implementing security features (encryption, biometrics, pinning)?**
ÔåÆ Use [android-security.md](references/android-security.md) for comprehensive security guide
ÔåÆ Follow [android-permissions.md](references/android-permissions.md) for runtime permissions
ÔåÆ Check [crashlytics.md](references/crashlytics.md) for PII scrubbing and data privacy

## Shared Knowledge Hub Integration

**Whiteboard**: `C:\Users\barwa\GeminiCLI/Gemini-Setup/agent-memory/shared/MEMORY.md` (EINZIGE zentrale Wissensdatei)

**Lesen**: Vor der Ausfuehrung das Whiteboard lesen fuer bekannte Muster, Arch-Entscheidungen und offene Probleme.

**Schreiben bei Fehlern**: Jeden Build- oder Setup-Fehler in den Abschnitt "Offene Fehler & Probleme" eintragen:
- Quelle: `android-kotlin-compose`
- Symptom: [Fehlermeldung]
- Ursache: [Root Cause]
- Betroffene Dateien: [Pfade]
- Fix-Vorschlag: [Loesungsidee]
- Status: OFFEN

**Schreiben bei Erkenntnissen**: Architekturentscheidungen in "Architektur-Entscheidungen" eintragen:
- Welches Modul-Muster gewaehlt wurde (z.B. feature:api+impl vs. merged)
- Abweichungen vom Standard-NowInAndroid-Layout und warum
- Projektspezifische Hilt-/Room-/Compose-Konfigurationen

