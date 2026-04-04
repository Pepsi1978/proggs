# Android-Phasen-System: Automatischer Trigger (KRITISCH)

## Regel: Bei Android-Arbeit IMMER die Phasen-Regel laden

Wenn die Aufgabe ein Android-Projekt betrifft, MUSS die vollstaendige Phasen-Regel
gelesen werden BEVOR mit der Arbeit begonnen wird:

**Datei**: `~/proggs/claude-code-setup/references/development-phases.md`

## Trigger-Woerter (bei JEDEM dieser Woerter die Phasen-Regel laden)

- "BestJournal", "Journal", "Tagebuch-App"
- "Android", "Kotlin", "Compose", "Jetpack"
- "Gradle", "gradlew", "assembleDebug", "assembleRelease"
- "R8", "ProGuard", "minifyEnabled", "Release-Build"
- "Play Store", "Google Play", "AAB", "APK"
- "Phase 1", "Phase 2", "Phase 3", "Phase 4", "Phase 5", "Phase 6"
- "weiter mit [Projektname]" wenn SESSION-RULES.md das Projekt als Android identifiziert
- Jedes Projekt in `~/proggs/` das eine `build.gradle` oder `build.gradle.kts` enthaelt

## Was nach dem Laden passiert

1. SESSION-RULES.md des Projekts lesen (Phasen-Block)
2. Dem Benutzer den aktuellen Stand zeigen (Phase, Version, naechster Schritt)
3. Erst dann mit der Arbeit beginnen

## Warum als Trigger statt als Auto-Loaded Rule

Die Phasen-Regel ist 12.900 Tokens gross und wird NUR bei Android-Arbeit gebraucht.
Bei Tampermonkey-Skripten, C#-Projekten oder Umgebungs-Arbeit waeren diese Tokens verschwendet.
Durch den Trigger wird sie nur geladen wenn sie wirklich gebraucht wird.
