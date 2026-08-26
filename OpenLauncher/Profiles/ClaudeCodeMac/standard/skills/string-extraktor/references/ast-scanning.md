# AST-basiertes Scanning fuer Compose und Kotlin

> Diese Referenz beschreibt wie man die Regex-basierten Scans aus Phase 1 durch
> AST-basierte Scans erweitert. AST bedeutet "Abstract Syntax Tree" — der Code
> wird vom Compiler-Frontend zerlegt und semantisch verstanden, statt nur
> Text-Pattern zu matchen.
>
> **Warum das wichtig ist:** Die Regex-Muster aus Phase 1 finden ca. 90-95%
> aller hardcodierten Strings. Die letzten 5-10% werden uebersehen, weil
> Regex bestimmte Konstrukte nicht zuverlaessig erkennen kann:
>
> - Template-Strings: `Text("Hallo $name")`
> - Multiline-Composables ueber mehrere Zeilen
> - Verschachtelte Lambdas: `items.forEach { Text(it.label) }`
> - Conditional-Expressions: `Text(if (a) "X" else "Y")`
> - Inline-Builder-Pattern: `Text(buildString { append("X") })`
>
> AST findet all das zuverlaessig, weil es die Struktur des Codes versteht.
>
> **Pseudolokalisierung deckt die uebrigen 5-10% auf** — siehe SKILL.md
> Phase 4.5. AST + Pseudolokalisierung = praktisch 100% Coverage.

---

## Inhalt

- 1. Detekt mit Custom Rule (empfohlen)
- 2. Android Lint mit Custom Rule (fuer XML + Code)
- 3. Konkrete Detekt-Rule-Implementierung
- 4. Integration in den Skill-Workflow
- 5. CI-Integration

---

## 1. Detekt mit Custom Rule (empfohlen)

**Detekt** ist der de-facto-Standard fuer statische Kotlin-Analyse. Er
analysiert den AST auf PSI-Ebene (Program Structure Interface — das gleiche
Modell das IntelliJ IDEA intern verwendet).

### Vorteile

- Findet auch Template-Strings, Multiline-Code, verschachtelte Aufrufe
- Schnell (laeuft in Sekunden auf grossen Codebases)
- CI-tauglich
- Open Source

### Setup

In `app/build.gradle.kts`:

```kotlin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

detekt {
    config.setFrom("$projectDir/detekt-config.yml")
    buildUponDefaultConfig = true
    parallel = true
}

dependencies {
    detektPlugins(project(":custom-detekt-rules"))
}
```

Die Custom Rule lebt in einem separaten Gradle-Modul, das im Skill-Workflow
einmalig angelegt wird.

---

## 2. Android Lint mit Custom Rule

Alternative fuer Teams die Detekt nicht einsetzen wollen.
Android Lint ist offiziell von Google und in jedem Android-Build integriert,
aber fuer Compose-Detection muss eine Custom Rule geschrieben werden — der
eingebaute `HardcodedText`-Lint findet NUR XML-Layouts.

### Custom Lint Rule fuer Compose

```kotlin
// HardcodedComposeTextDetector.kt
class HardcodedComposeTextDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            // Pruefe ob es ein Composable-Aufruf ist
            val name = node.methodName ?: return
            if (name !in COMPOSABLE_TEXT_FUNCTIONS) return

            // Pruefe ob das erste Argument ein String-Literal ist
            val firstArg = node.valueArguments.firstOrNull() ?: return
            if (firstArg.evaluate() !is String) return
            if (firstArg.asSourceString().contains("stringResource")) return

            context.report(
                ISSUE,
                node,
                context.getLocation(firstArg),
                "Hardcoded string in $name() — use stringResource() instead"
            )
        }
    }

    companion object {
        private val COMPOSABLE_TEXT_FUNCTIONS = setOf(
            "Text", "Button", "OutlinedTextField", "TextField",
            "Snackbar", "AlertDialog"
        )

        val ISSUE = Issue.create(
            id = "HardcodedComposeText",
            briefDescription = "Hardcoded string in Compose Text()",
            explanation = "Compose strings should use stringResource() for i18n",
            category = Category.I18N,
            severity = Severity.WARNING,
            implementation = Implementation(
                HardcodedComposeTextDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
```

---

## 3. Konkrete Detekt-Rule-Implementierung

Die elegantere Variante — Detekt arbeitet auf der gleichen PSI-Ebene und ist
einfacher zu integrieren.

### Custom Rule

```kotlin
// HardcodedComposeStringRule.kt
class HardcodedComposeStringRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "HardcodedComposeString",
        severity = Severity.Maintainability,
        description = "Hardcoded strings in Composable functions should use stringResource()",
        debt = Debt.FIVE_MINS
    )

    private val composableTextFunctions = setOf(
        "Text", "Button", "OutlinedTextField", "TextField",
        "Snackbar", "AlertDialog", "Toast", "TopAppBar"
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName !in composableTextFunctions) return

        // Pruefe alle String-Literale in den Argumenten
        expression.valueArguments.forEach { arg ->
            val value = arg.getArgumentExpression()
            when (value) {
                is KtStringTemplateExpression -> {
                    // Auch Template-Strings ("Hallo $name") werden gefunden
                    if (!isLikelyTechnical(value.text)) {
                        report(CodeSmell(
                            issue,
                            Entity.from(value),
                            "Hardcoded string '${value.text}' in $callName() — extract to strings.xml"
                        ))
                    }
                }
            }
        }
    }

    private fun isLikelyTechnical(text: String): Boolean {
        // Ausschluesse: leere Strings, einzelne Zeichen, URLs, technische Keys
        val content = text.removeSurrounding("\"")
        return content.length < 3 ||
               content.startsWith("http") ||
               content.matches(Regex("^[A-Z_]+$")) ||  // Konstanten
               content.matches(Regex("^[a-z_]+$"))      // Identifier
    }
}
```

### Aktivierung in `detekt-config.yml`

```yaml
custom-rules:
  active: true
  HardcodedComposeString:
    active: true
    excludes:
      - '**/test/**'
      - '**/androidTest/**'
      - '**/build/**'
      - '**/*Preview*.kt'
```

---

## 4. Integration in den Skill-Workflow

### Wann AST-Scan einsetzen

| Projekt-Groesse | Empfehlung |
|----------------|------------|
| < 50 Kotlin-Dateien | Regex aus Phase 1 reicht |
| 50-500 Dateien | Detekt-Rule sehr empfohlen |
| > 500 Dateien | Detekt-Rule + CI-Integration PFLICHT |

### Workflow

1. **Erstextraktion:** Phase 1 mit Regex (schnell, gute Coverage)
2. **Verifikation:** Detekt-Rule laufen lassen → findet die letzten 5-10%
3. **CI-Sicherung:** Detekt im Pre-Commit + GitHub Action

### Aufruf

```bash
# Lokal
./gradlew detekt

# Mit Custom-Rule-Modul
./gradlew :custom-detekt-rules:detektMain

# Output anschauen
cat app/build/reports/detekt/detekt.html
```

---

## 5. CI-Integration

```yaml
# .github/workflows/detekt.yml
name: Detekt Static Analysis
on: [pull_request]
jobs:
  detekt:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: ./gradlew detekt
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: detekt-report
          path: '**/build/reports/detekt/'
```

---

## 6. Grenzen von AST-Scanning

AST kann immer noch nicht 100% finden. Diese Faelle erfordern Pseudolokalisierung:

| Versteckt sich in | Beispiel |
|------------------|----------|
| Lazy-Initialized Properties | `val title by lazy { "Hallo" }` |
| Externalisierte Konstanten in `BuildConfig` | `BuildConfig.WELCOME_TEXT` |
| Strings die ueber Reflektion gesetzt werden | `view.text = config.getString("welcome")` |
| Strings in dynamisch generierten Klassen | KSP-generierte Builder |
| Drittbibliotheken die intern Strings haben | Material-Components, Compose-Snackbar-Default |
| PDF-/HTML-Vorlagen die als Resources liegen | `R.raw.invoice_template` |

**Defense in Depth:** AST + Pseudolokalisierung (Phase 4.5) + manueller
Sprachtest = praktisch 100% Coverage.

---

## 7. Defekt vs Detekt — Nicht verwechseln

**Detekt** (mit "k") ist das hier beschriebene Kotlin-Linting-Tool.
**Defekt** ist ein Tippfehler. Beim Suchen nach Tools immer "detekt" eingeben:

- GitHub: https://github.com/detekt/detekt
- Docs: https://detekt.dev/

---

## 8. Wenn der Skill Detekt aufsetzen soll

Wenn der Benutzer explizit "AST-Scan einrichten", "Detekt fuer Compose
einrichten" oder "Custom Lint Rule fuer hardcoded Strings" sagt, dann:

1. Pruefen ob Detekt schon im Projekt ist (`grep -r 'detekt' build.gradle*`)
2. Wenn nicht: Einrichten gemaess Abschnitt 1
3. Custom-Rule-Modul generieren gemaess Abschnitt 3
4. Erstrun laufen lassen
5. Ergebnisse dem Benutzer zeigen
6. CI-Integration anbieten gemaess Abschnitt 5

Das ist eine optionale Erweiterung — der Skill funktioniert auch ohne AST-Scan
durch die Kombination Regex (Phase 1) + Pseudolokalisierung (Phase 4.5).
