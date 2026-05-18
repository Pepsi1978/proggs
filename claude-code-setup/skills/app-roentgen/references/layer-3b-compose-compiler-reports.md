# Schicht 3b — Compose Compiler Reports + KSP-NavGraph (Goldstandard 2026)

> **FIX AA2 (Audit 10) — Compose-Kotlin-Hinweis:** Diese Schicht ist per Design Kotlin-only (Jetpack Compose hat keine Java-API). Falls die App KEINE Compose-Komponenten nutzt (klassische View-Layouts in XML), kann diese Schicht ganz uebersprungen werden — die Compose Compiler Reports waeren leer.

## Warum diese Schicht optional, aber empfohlen ist

Die grep-basierte Architektur-Extraktion aus Schicht 3 ist robust und funktioniert ohne Build-Tools — aber sie hat eine prinzipielle Schwaeche: Sie kann nicht zuverlaessig unterscheiden zwischen einem echten `@Composable Screen` und einem in einem Kommentar, einem String-Literal oder einer Test-Datei. Wenn die App zugaenglich ist und gebaut werden kann, liefern die **Compose Compiler Reports** ein nachweislich vollstaendiges Composable-Inventar — direkt vom Compiler, ohne Heuristik.

Diese Schicht ist OPTIONAL und wird durchgefuehrt wenn:
- Das Projekt lokal baubar ist (Gradle, JDK, Android SDK vorhanden)
- Zeit fuer einen Build-Lauf (~2-5 Minuten) eingeplant werden kann
- Maximale Vollstaendigkeit verlangt wird (z.B. fuer juristischen Audit)

Bei einem grossen `audit-report-template.md` Audit wird Schicht 3b als Stuetze von Schicht 3 und Schicht 4 verwendet, NICHT als Ersatz — Layer 3 grep-Patterns bleiben die Default-Methode.

## 3b.1 Compose Compiler Reports aktivieren

### Schritt 1 — Gradle-Konfiguration

In `app/build.gradle.kts` (oder pro Modul wenn Multi-Module):

```kotlin
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}
```

Voraussetzung: Kotlin 2.0+ und Compose Compiler Gradle Plugin >= 1.5.10.
Auf juengeren Setups (vor Kotlin 2.0) gilt die alte freeCompilerArgs-Variante:

```kotlin
kotlinOptions {
    freeCompilerArgs += listOf(
        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
              project.layout.buildDirectory.dir("compose_compiler").get().asFile.absolutePath,
        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
              project.layout.buildDirectory.dir("compose_compiler").get().asFile.absolutePath
    )
}
```

### Schritt 2 — Build ausfuehren

```bash
./gradlew :app:assembleRelease
# oder fuer Debug-Build:
./gradlew :app:assembleDebug
```

### Schritt 3 — Reports lesen

```bash
# Verzeichnis pruefen
find . -type d -name 'compose_compiler' -not -path '*/build/intermediates/*'

# Standard-Pfad bei single-module:
ls app/build/compose_compiler/

# Typische Dateien (pro Modul + Variant):
# <modul>-composables.txt   — Alle @Composable mit Stability/Inline/Restartable-Flag
# <modul>-composables.csv   — Maschinenlesbar, gut fuer Auswertung
# <modul>-classes.txt       — Stabilitaet aller Klassen (Stable/Unstable/Runtime)
# <modul>-module.json       — Modul-Metadata
```

## 3b.2 Was extrahiert werden kann

### Aus `composables.txt`

```
restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun DashboardScreen(
  stable viewModel: DashboardViewModel
  stable onNavigateToDetail: Function1<Long, Unit>
)
restartable skippable fun EntryCard(
  unstable entry: JournalEntry  ← Performance-Risiko!
)
```

Damit ist nachweisbar:
- WELCHE Composables existieren (vollstaendig — kein Miss)
- Welche Parameter `unstable` sind (Recompose-Risiko)
- Welche skippable/restartable sind
- Welche Scheme-Annotationen sie haben

### Im Audit-Bericht (Schicht 3 ergaenzen)

```markdown
### 3b.X Composable-Inventar (aus Compose Compiler Reports)

| Modul | Composable | Restartable | Skippable | Unstable Params | Datei |
|-------|-----------|-------------|-----------|-----------------|-------|
| app | DashboardScreen | JA | JA | — | DashboardScreen.kt |
| app | EntryCard | JA | JA | entry (1) | EntryCard.kt |
| feature_paywall | PaywallScreen | JA | JA | — | PaywallScreen.kt |
| ... | ... | ... | ... | ... | ... |

Gesamt: N Composables (vs. M durch grep gefunden — Differenz: K)
```

Wenn die Compiler-Reports MEHR oder WENIGER Composables zeigen als grep gefunden hat, ist das ein Audit-Befund — die grep-Patterns muessen angepasst werden ODER es gibt versteckte/generierte Composables.

## 3b.3 HTML-Report fuer schnelle Browser-Auswertung

Optional: `compose-report-to-html` (Shreyas Patil) konvertiert die 4 Compiler-Report-Formate in eine durchsuchbare HTML-Seite.

```kotlin
// In build.gradle.kts (Project-Level oder pro App-Modul)
plugins {
    id("dev.shreyaspatil.compose-compiler-report-generator") version "1.3.1"
}
```

Aufruf:
```bash
./gradlew :app:htmlComposeCompilerReport
# Output: app/build/compose_reports/index.html
```

Die HTML-Datei kann direkt mit dem JSON-Export des Roentgen-Skills referenziert werden — Pfad in `app-roentgen-export.json` aufnehmen (Feld `compose_report_html_path`).

## 3b.4 KSP-NavGraph-Extraktion (compose-destinations)

Wenn die App die [compose-destinations](https://github.com/raamcosta/compose-destinations) Library nutzt (~9k GitHub-Stars, weit verbreitet), wird die komplette Navigations-Karte zur Compile-Zeit generiert. Das ist die ZUVERLAESSIGSTE Methode um den Navigations-Graph zu extrahieren — keine grep-Heuristik noetig.

### Erkennung im Projekt

```bash
# Library als Dependency?
grep -rE 'compose-destinations|raamcosta\.android-destinations' \
  --include='*.gradle*' --include='*.toml' .

# Annotation im Code?
grep -rn '@Destination\|@RootNavGraph\|@NavGraph' --include='*.kt' . | head -5

# Generierter NavGraphs.kt erwarteter Pfad nach Build:
find . -name 'NavGraphs.kt' -path '*/generated/ksp/*' 2>/dev/null
find . -name 'Destination.kt' -path '*/generated/ksp/*' 2>/dev/null | head -10
```

### Wenn gefunden — was extrahieren

Aus `NavGraphs.kt` (generierte Datei):
- Alle Top-Level Graph-Objekte mit ihren `startRoute` und `destinations`
- Alle Sub-Graphs (Nested Navigation)

Aus den generierten `<Screen>Destination.kt`-Dateien:
- Exakte Route-Strings (kein grep-Match-Risiko)
- Typsichere Argument-Definitionen (welche Daten werden uebergeben)
- Deep-Link-Specs falls per `@Destination(deepLinks = ...)` deklariert

### Alternative: Navigation 3 + nav3ksp

Falls die App die neue [Navigation 3](https://klibs.io/project/fopwoc/nav3ksp) (ab 2025) nutzt:
```bash
grep -rn 'NavDisplay\|nav3ksp\|@Nav3Destination' --include='*.kt' .
```

Output ist analog — generierte Code-Dateien sind die Quelle der Wahrheit.

## 3b.5 ast-grep als grep-Ersatz (strukturelle Suche)

Wenn der Build nicht moeglich ist und grep zu viele False-Positives liefert, ist [`ast-grep`](https://ast-grep.github.io/) (Rust + tree-sitter-kotlin) eine Bruecke: strukturelle Patterns statt Regex.

### Installation

```bash
# macOS
brew install ast-grep
# Windows (via cargo)
cargo install ast-grep
# Linux (via cargo)
cargo install ast-grep
```

### Nuetzliche Patterns fuer Roentgen-Audit

```bash
# Alle @Composable-Funktionen (egal wo definiert)
sg --lang kotlin --pattern '@Composable
fun $NAME($$$) { $$$ }' src/

# Alle Click-Handler in Composables (nicht im Kommentar, nicht im String!)
sg --lang kotlin --pattern 'onClick = { $$$ }' src/

# Alle @StringRes-Verwendungen
sg --lang kotlin --pattern '@StringRes $VAR: Int' src/

# Premium-Gates (Funktionsaufruf mit isPremium-Check)
sg --lang kotlin --pattern 'if (isPremium()) { $$$ }' src/

# Hardcoded Strings in Compose Text (echte Literal-Matches)
sg --lang kotlin --pattern 'Text($STR)' src/ | grep -E 'Text\("[^"]+"\)'
```

Vorteil gegenueber grep: 0 False-Positives aus Kommentaren oder Strings, kein Build-Ausgang noetig, ~10x schneller als grep auf grossen Projekten.

## 3b.6 mobsfscan (CI/CD-Integration)

Fuer wiederkehrende Audits in CI/CD: [`mobsfscan`](https://github.com/MobSF/mobsfscan) kann Kotlin-Quellcode mit Semgrep-Regeln pruefen. Custom Rules fuer Feature-Gates, Premium-Checks, Hardcoded-Secrets, Werbe-SDK-Aufrufe sind wiederverwendbar.

### Installation

```bash
pip install mobsfscan
# oder als Docker:
docker pull opensecurity/mobsfscan
```

### Beispiel-Aufruf

```bash
mobsfscan --json -o mobsfscan-report.json /path/to/android-app
```

Custom-Rules-Verzeichnis (Semgrep-YAML) kann zum Skill-Repository gehoeren wenn der Skill CI/CD-Integration braucht.

## 3b.7 Quellen

- [Compose Compiler Metrics (JetBrains)](https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/compiler-metrics.md)
- [Compose Compiler Gradle Plugin (Android Developers)](https://developer.android.com/develop/ui/compose/compiler)
- [compose-report-to-html (Shreyas Patil)](https://github.com/PatilShreyas/compose-report-to-html/)
- [compose-destinations (Rafael Costa)](https://github.com/raamcosta/compose-destinations)
- [nav3ksp (Multiplatform KSP fuer Navigation 3)](https://klibs.io/project/fopwoc/nav3ksp)
- [ast-grep — Structural Search Tool](https://ast-grep.github.io/catalog/kotlin/)
- [mobsfscan — Mobile Security Scanner](https://github.com/MobSF/mobsfscan)
- [Diagnose stability issues (Android Developers)](https://developer.android.com/develop/ui/compose/performance/stability/diagnose)

## 3b.8 Empfohlene Reihenfolge im Audit

1. **IMMER**: Schicht 3 grep-Patterns durchfuehren (Default, ohne Build)
2. **OPTIONAL (wenn Build moeglich)**: Compose Compiler Reports aktivieren + Composable-Inventar mit grep-Ergebnis abgleichen
3. **OPTIONAL (wenn compose-destinations vorhanden)**: NavGraphs.kt aus generated/ksp/ lesen statt grep ueber `@Composable.*Screen`
4. **OPTIONAL (bei vielen Hardcoded-False-Positives)**: ast-grep auf strukturelle Patterns umstellen
5. **OPTIONAL (fuer CI/CD)**: mobsfscan-Job einrichten

Die OPTIONAL-Schritte werden im Audit-Bericht als "Layer 3b — angereichert mit Compose Compiler Reports" markiert, wenn sie durchgefuehrt wurden. Ansonsten gilt Layer 3 als alleinige Datenquelle.
