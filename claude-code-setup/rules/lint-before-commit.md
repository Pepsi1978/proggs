# Lint/Build VOR jedem Commit — nicht erst in CI (KRITISCH)

## Regel

VOR JEDEM Commit von Code-Dateien MUSS der passende Linter/Build lokal laufen.
Bei Errors: NICHT committen. Erst fixen, dann committen.

## Warum

Am 2026-03-28 wurden 3 aufeinanderfolgende Commits mit ESLint-Errors nach GitHub
gepusht. Der Benutzer bekam Fehler-E-Mails und musste nachfragen. Jeder Fix brauchte
einen Extra-Commit. Die Fehler waeren ALLE lokal sofort sichtbar gewesen.

## Projekt-spezifische Lint-Befehle

| Projekt/Dateien | Lint-Befehl | Wann |
|----------------|-------------|------|
| `Tampermonkey/*.user.js` | `npx eslint Tampermonkey/*.user.js` | Vor jedem TM-Commit |
| C#/WPF Projekte | `dotnet build -c Release` | Vor jedem C#-Commit |
| TypeScript Projekte | `npx tsc --noEmit` | Vor jedem TS-Commit |
| Rust Projekte | `cargo check` | Vor jedem Rust-Commit |
| Go Projekte | `go vet ./...` | Vor jedem Go-Commit |
| Python Projekte | `python -m py_compile DATEI` | Vor jedem Python-Commit |
| Kotlin/Gradle | `./gradlew build` | Vor jedem Kotlin-Commit |

## Ablauf

1. Code aendern
2. **Linter lokal ausfuehren** (passend zum Dateityp)
3. Bei Errors: sofort fixen, zurueck zu Schritt 2
4. Bei 0 Errors: committen und pushen

## Was NIEMALS passieren darf

- ❌ Committen ohne vorherigen lokalen Lint/Build-Check
- ❌ "Der CI wird das schon pruefen" — das ist zu spaet
- ❌ Lint-Warnings ignorieren die auf echte Probleme hindeuten (z.B. `no-undef`)
- ❌ Mehrere fehlerhafte Commits hintereinander pushen
