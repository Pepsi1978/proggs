# Android String-Extraktion & String-Erstellung: Skill-Referenz

> **Zweck:** Diese Datei ist die REFERENZ fuer den String-Extraktions-Skill.
> Der Skill nutzt dieses Dokument um (1) alle hardcodierten Strings in einer
> Android-Codebase zu FINDEN und (2) sie als uebersetzungskompatible String-Ressourcen zu ERSTELLEN.
>
> **Abgrenzung:** Die eigentliche Uebersetzung in andere Sprachen ist NICHT Aufgabe dieses
> Dokuments — dafuer existiert der separate Uebersetzungs-Skill (`uebersetzung/SKILL.md`).
> Dieses Dokument endet dort wo die strings.xml korrekt befuellt ist.
>
> **Geltungsbereich:** Alle Android-Projekte (Compose und XML), projektunabhaengig.
>
> **Quellen:** Google Android Docs, Meta Engineering Blog, Netflix TechBlog, Airbnb Engineering,
> Duolingo Blog, Unicode CLDR, Phrase/Lokalise/Crowdin Dokumentation, Jeroen Mols Naming Convention.
> **Stand:** April 2026

---

## Skill-Workflow (Kurzfassung fuer den Skill)

Der Skill arbeitet ein Android-Projekt in dieser Reihenfolge ab:

```
PHASE 1: SCAN (Strings finden)
  1. Grep-Muster 1-9 auf alle .kt-Dateien anwenden (Kapitel 2-4)
  2. Treffer filtern: Log/Analytics/Keys/Tests ausschliessen (Kapitel 4)
  3. Screen-Inventar erstellen (Kapitel 6)
  4. Pro Screen: Kotlin + ViewModel + Composables pruefen (Kapitel 6)

PHASE 2: CREATE (Strings erstellen)
  5. Fuer jeden Treffer: String-Ressource nach Naming-Konvention erstellen (Kapitel 12)
  6. Platzhalter nummerieren, XLIFF-Tags setzen (Kapitel 13-14)
  7. Uebersetzer-Kommentar schreiben (Kapitel 16)
  8. Hardcodierten String durch stringResource() ersetzen
  9. Bei Mengenangaben: <plurals> statt if/else (Kapitel 15)

PHASE 3: VERIFY (Qualitaet pruefen)
  10. Build pruefen — kompiliert alles?
  11. Lint laufen lassen — 0 HardcodedText-Treffer?
  12. Abschluss-Checkliste durchgehen (Kapitel 24)
```

---

## Inhaltsverzeichnis

### Teil A: Strings FINDEN (100% Abdeckung)
1. [Systematische Suche — Ueberblick](#1-systematische-suche--ueberblick)
2. [Suchmuster fuer Kotlin/Compose-Code](#2-suchmuster-fuer-kotlincompose-code)
3. [Suchmuster fuer XML-Layouts](#3-suchmuster-fuer-xml-layouts)
4. [Versteckte String-Quellen (leicht zu uebersehen)](#4-versteckte-string-quellen-leicht-zu-uebersehen)
5. [Android Lint als automatischer Detektor](#5-android-lint-als-automatischer-detektor)
6. [Screen-fuer-Screen-Workflow](#6-screen-fuer-screen-workflow)
7. [Priorisierung — welche Strings zuerst](#7-priorisierung--welche-strings-zuerst)
8. [Pseudolokalisierung — die zuverlaessigste Methode](#8-pseudolokalisierung--die-zuverlaessigste-methode)
9. [Die "letzten 5%" — Strings die IMMER vergessen werden](#9-die-letzten-5--strings-die-immer-vergessen-werden)
10. [Praevention — neue hardcodierte Strings verhindern](#10-praevention--neue-hardcodierte-strings-verhindern)

### Teil B: Strings ERSTELLEN (uebersetzungskompatibel)
11. [Die goldenen Regeln](#11-die-goldenen-regeln)
12. [String-Naming-Konvention](#12-string-naming-konvention)
13. [Platzhalter und Format-Specifier](#13-platzhalter-und-format-specifier)
14. [XLIFF-Tags fuer nicht-uebersetzbare Inhalte](#14-xliff-tags-fuer-nicht-uebersetzbare-inhalte)
15. [Plurals korrekt anlegen](#15-plurals-korrekt-anlegen)
16. [Uebersetzer-Kommentare](#16-uebersetzer-kommentare)
17. [Nicht-uebersetzbare Strings](#17-nicht-uebersetzbare-strings)
18. [String Arrays](#18-string-arrays)
19. [HTML und Styled Text in Strings](#19-html-und-styled-text-in-strings)
20. [Compose-spezifische Patterns](#20-compose-spezifische-patterns)
21. [Dateiorganisation bei grossen Apps](#21-dateiorganisation-bei-grossen-apps)
22. [Anti-Patterns — was NIEMALS gemacht werden darf](#22-anti-patterns--was-niemals-gemacht-werden-darf)

### Teil C: Qualitaetssicherung
23. [Automatische Qualitaetspruefung (Lint und CI)](#23-automatische-qualitaetspruefung-lint-und-ci)
24. [Abschluss-Checkliste](#24-abschluss-checkliste)
25. [Vollstaendiges Beispiel: Best-Practice strings.xml](#25-vollstaendiges-beispiel-best-practice-stringsxml)

---

# TEIL A: STRINGS FINDEN

## 1. Systematische Suche — Ueberblick

### Das Problem

In einer grossen Android-App verstecken sich hardcodierte Strings an dutzenden Stellen:
Kotlin-Code, Compose-Funktionen, XML-Layouts, ViewModels, Data-Klassen, Error-Handler,
Toast-Aufrufe, Snackbar-Nachrichten, Content Descriptions, Dialog-Texte, und mehr.

Ein einfaches `grep "\"` findet zu viel (auch Nicht-UI-Strings wie Log-Statements und Keys)
und zu wenig (erkennt keine String-Templates wie `"Hallo $name"`).

### Die 3-Schichten-Strategie

| Schicht | Methode | Was sie findet | Abdeckung |
|---------|---------|---------------|-----------|
| **1. Automatisch** | Android Lint (`HardcodedText`, `SetTextI18n`) | Hardcodierte Strings in XML und einfache Java/Kotlin-Faelle | ~60% |
| **2. Gezielte Grep-Muster** | Regex-Suche nach bekannten Code-Patterns | Compose `Text()`, Toasts, Snackbars, contentDescription, Dialoge | ~30% |
| **3. Manuell pro Screen** | App durchklicken und jeden sichtbaren Text pruefen | Dynamisch zusammengebaute Strings, Randfaelle, Edge Cases | ~10% |

**Alle drei Schichten sind noetig.** Keine einzelne Methode findet alles.

### Die 4-Phasen-Methode (wie professionelle Teams arbeiten)

```
Phase 1: STATISCHE ANALYSE (30 Minuten)
  → Lint laufen lassen → Low-hanging Fruit sofort fixen
  → Grep-Muster pro Dateityp → systematisch alle Code-Patterns abdecken

Phase 2: PSEUDOLOKALISIERUNG (1-2 Stunden) ← WICHTIGSTE METHODE
  → App auf Pseudolocale "en-XA" stellen
  → Jeden Screen durchklicken
  → Unakzentuierter Text = vergessener String (sofort sichtbar!)

Phase 3: SCREEN-FUER-SCREEN WALKTHROUGH (2-4 Stunden)
  → Jede Kotlin-Datei oeffnen und Grep-Muster anwenden
  → Besonders: ViewModels, Dialoge, Error-Handler, Enums

Phase 4: VERIFIKATION (30 Minuten)
  → Nochmal Lint laufen → 0 Treffer bei HardcodedText?
  → Nochmal Pseudolocale → Alles akzentuiert?
  → Coverage-Script laufen lassen
```

### Kritische Erkenntnis: Standard-Lint findet Compose NICHT

**Das ist die groesste Luecke im Android-Tooling:** Die Standard-Lint-Checks `HardcodedText`
und `SetTextI18n` erkennen **NUR** Strings in XML-Layouts und `TextView.setText()`.
Compose `Text("hardcoded")` wird von keinem eingebauten Lint-Check gefunden!

| Lint-Check | XML Layouts | `setText()` | Compose `Text()` |
|-----------|-------------|-------------|-------------------|
| `HardcodedText` | JA | NEIN | **NEIN** |
| `SetTextI18n` | NEIN | JA | **NEIN** |

Deshalb sind Grep-Muster und Pseudolokalisierung unverzichtbar fuer Compose-Apps.

---

## 2. Suchmuster fuer Kotlin/Compose-Code

### Pattern 1: Compose Text()-Aufrufe mit hardcodierten Strings

```
Grep-Pattern: Text\(\s*"[^"]*[a-zA-ZäöüÄÖÜß]
Dateien:      **/*.kt
```

Findet:
```kotlin
Text("Einstellungen")                    // TREFFER — hardcodiert
Text("OK")                              // TREFFER
Text(text = "Noch keine Eintraege")     // TREFFER
```

Findet NICHT (korrekt ignoriert):
```kotlin
Text(stringResource(R.string.settings)) // OK — schon extrahiert
Text(text = formattedDate)              // OK — Variable, kein Literal
```

### Pattern 2: String-Templates und Konkatenation

```
Grep-Pattern: Text\(\s*"[^"]*\$        (Dollar-Zeichen = Template)
Grep-Pattern: Text\(\s*"[^"]*" \+      (Konkatenation)
```

Findet:
```kotlin
Text("Hallo $userName")                  // TREFFER — muss als getString() mit %1$s
Text("Du hast " + count + " Eintraege") // TREFFER — muss als getString() mit %1$d
```

### Pattern 3: Toast- und Snackbar-Nachrichten

```
Grep-Pattern: Toast\.makeText\([^,]+,\s*"
Grep-Pattern: showSnackbar\(\s*"
Grep-Pattern: SnackbarHostState.*showSnackbar\(\s*"
```

Findet:
```kotlin
Toast.makeText(context, "Gespeichert!", Toast.LENGTH_SHORT)  // TREFFER
snackbarHostState.showSnackbar("Keine Verbindung")           // TREFFER
```

### Pattern 4: Dialog-Texte

```
Grep-Pattern: AlertDialog.*title\s*=\s*"
Grep-Pattern: AlertDialog.*text\s*=\s*"
Grep-Pattern: confirmValueChange.*"
Grep-Pattern: dismiss.*"
```

Findet:
```kotlin
AlertDialog(
    title = { Text("Wirklich loeschen?") },       // TREFFER
    text = { Text("Das kann nicht rueckgaengig...") }, // TREFFER
    confirmButton = { TextButton(onClick = ...) { Text("Ja") } }, // TREFFER
    dismissButton = { TextButton(onClick = ...) { Text("Nein") } } // TREFFER
)
```

### Pattern 5: Content Descriptions (Accessibility)

```
Grep-Pattern: contentDescription\s*=\s*"
Grep-Pattern: semantics\s*\{[^}]*"
Grep-Pattern: Modifier\.semantics.*contentDescription
```

Findet:
```kotlin
Icon(Icons.Default.Add, contentDescription = "Neuer Eintrag")  // TREFFER
Image(painter, contentDescription = "Profilbild")               // TREFFER
```

### Pattern 6: Placeholder/Hint-Texte

```
Grep-Pattern: placeholder\s*=\s*\{?\s*Text\(\s*"
Grep-Pattern: label\s*=\s*\{?\s*Text\(\s*"
Grep-Pattern: supportingText\s*=\s*\{?\s*Text\(\s*"
```

Findet:
```kotlin
OutlinedTextField(
    placeholder = { Text("Suchen...") },           // TREFFER
    label = { Text("E-Mail-Adresse") },             // TREFFER
    supportingText = { Text("Mindestens 8 Zeichen") } // TREFFER
)
```

### Pattern 7: TopAppBar und Navigation

```
Grep-Pattern: TopAppBar.*title\s*=\s*\{?\s*Text\(\s*"
Grep-Pattern: NavigationBarItem.*label\s*=\s*\{?\s*Text\(\s*"
Grep-Pattern: TabRow.*Tab.*text\s*=\s*"
Grep-Pattern: BottomNavigation.*label\s*=\s*"
```

### Pattern 8: Fehler- und Exception-Texte (fuer Benutzer sichtbar)

```
Grep-Pattern: \.message\s*=\s*"[^"]*[a-zA-ZäöüÄÖÜß]
Grep-Pattern: throw.*Exception\(\s*"[^"]*[a-zA-ZäöüÄÖÜß]
Grep-Pattern: error\(\s*"[^"]*[a-zA-ZäöüÄÖÜß]
```

**Achtung:** Nicht alle Exception-Messages sind benutzersichtbar! Nur extrahieren wenn:
- Der Text in einer UI-Komponente angezeigt wird
- Der Text in einem Snackbar/Toast/Dialog erscheint

Log-Messages (`Log.d`, `Log.e`, `Timber.d`) werden NICHT extrahiert — die sieht der Benutzer nie.

### Pattern 9: Enum-Labels und Sealed-Class-Texte

```
Grep-Pattern: enum class.*\{[\s\S]*"[^"]*[a-zA-ZäöüÄÖÜß]
Grep-Pattern: sealed.*\{[\s\S]*"[^"]*[a-zA-ZäöüÄÖÜß]
```

Findet:
```kotlin
enum class Mood(val label: String) {
    HAPPY("Gut gelaunt"),      // TREFFER — wenn label in der UI angezeigt wird
    SAD("Traurig"),            // TREFFER
    NEUTRAL("Neutral")        // TREFFER
}
```

### Zusammenfassung: Grep-Befehle in der Praxis

```bash
# Alle Kotlin-Dateien im Projekt durchsuchen
# Anpassung: Pfad zum src-Verzeichnis

# 1. Compose Text() mit hardcodierten Strings
grep -rn 'Text(\s*"[^"]*[a-zA-ZäöüÄÖÜß]' app/src/main/java/ --include="*.kt"

# 2. String-Templates in Text()
grep -rn 'Text(\s*"[^"]*\$' app/src/main/java/ --include="*.kt"

# 3. Toasts
grep -rn 'Toast\.makeText.*"' app/src/main/java/ --include="*.kt"

# 4. Snackbars
grep -rn 'showSnackbar\s*(\s*"' app/src/main/java/ --include="*.kt"

# 5. Content Descriptions
grep -rn 'contentDescription\s*=\s*"' app/src/main/java/ --include="*.kt"

# 6. Placeholder/Labels
grep -rn 'placeholder\s*=.*Text(\s*"' app/src/main/java/ --include="*.kt"
grep -rn 'label\s*=.*Text(\s*"' app/src/main/java/ --include="*.kt"

# 7. AlertDialog-Texte
grep -rn 'AlertDialog' app/src/main/java/ --include="*.kt" -A 10 | grep '"[^"]*[a-zA-Z]'

# FILTER: Was NICHT extrahiert werden soll
# Log-Statements (nicht benutzersichtbar):
# Log.d, Log.e, Log.w, Log.i, Timber.d, Timber.e, println
```

---

## 3. Suchmuster fuer XML-Layouts

In reinen Compose-Apps gibt es weniger XML, aber fuer Hybrid-Apps oder aeltere Screens:

### Pattern 1: Direkte Texte in Layouts

```
Grep-Pattern: android:text="[^@][^"]*[a-zA-ZäöüÄÖÜß]
Grep-Pattern: android:hint="[^@][^"]*[a-zA-ZäöüÄÖÜß]
Grep-Pattern: android:contentDescription="[^@][^"]*[a-zA-ZäöüÄÖÜß]
Grep-Pattern: android:title="[^@][^"]*[a-zA-ZäöüÄÖÜß]
Grep-Pattern: android:summary="[^@][^"]*[a-zA-ZäöüÄÖÜß]
```

Das `[^@]` am Anfang schliesst Strings aus die bereits `@string/` referenzieren.

### Pattern 2: Menu-Items

```xml
<!-- Oft vergessen: Menu-Titel und Tooltips -->
<item android:title="Einstellungen" />  <!-- TREFFER -->
```

### Pattern 3: Preferences/Settings-XML

```xml
<!-- PreferenceScreen-Titel werden leicht uebersehen -->
<Preference android:title="Benachrichtigungen" android:summary="Push aktivieren" />
```

---

## 4. Versteckte String-Quellen (leicht zu uebersehen)

### Kategorie 1: ViewModel und State-Klassen

ViewModels sollten keine Strings resolven (Lifecycle-Problem), aber manche tun es trotzdem:

```kotlin
// OFT UEBERSEHEN: Strings die im ViewModel zusammengebaut werden
class JournalViewModel : ViewModel() {
    val emptyStateText = "Noch keine Eintraege"  // TREFFER!
    val errorMessage = "Speichern fehlgeschlagen" // TREFFER!
    
    fun getGreeting(): String {
        return "Guten Morgen, $userName"           // TREFFER!
    }
}
```

**Suchmuster:**
```
Grep-Pattern: val\s+\w+\s*=\s*"[^"]*[a-zA-ZäöüÄÖÜß]   (in ViewModel-Dateien)
Grep-Pattern: return\s*"[^"]*[a-zA-ZäöüÄÖÜß]           (in ViewModel-Dateien)
```

### Kategorie 2: Data Classes mit Display-Text

```kotlin
data class OnboardingGoal(
    val id: String,
    val displayName: String = "Bessere Laune"  // TREFFER!
)
```

### Kategorie 3: Companion Objects und Konstanten

```kotlin
companion object {
    const val DEFAULT_TITLE = "Mein Tagebuch"     // TREFFER wenn in UI sichtbar!
    const val ERROR_MSG = "Ein Fehler ist aufgetreten" // TREFFER!
}
```

### Kategorie 4: When/Switch-Ausdruecke mit String-Rueckgabe

```kotlin
fun getMoodLabel(mood: Mood): String = when (mood) {
    Mood.HAPPY -> "Gut gelaunt"      // TREFFER!
    Mood.SAD -> "Traurig"            // TREFFER!
    Mood.NEUTRAL -> "Neutral"        // TREFFER!
}
```

**Suchmuster:**
```
Grep-Pattern: ->\s*"[^"]*[a-zA-ZäöüÄÖÜß]    (in when-Bloecken)
```

### Kategorie 5: Liste/Array-Initialisierungen

```kotlin
val tips = listOf(
    "Schreibe jeden Tag einen Satz",         // TREFFER!
    "Nutze die Sprachaufnahme fuer unterwegs", // TREFFER!
    "Fuege Fotos hinzu fuer mehr Erinnerungen" // TREFFER!
)
```

### Kategorie 6: Compose-Modifier mit Text

```kotlin
Modifier.semantics {
    contentDescription = "Zurueck-Button"   // LEICHT ZU UEBERSEHEN!
    stateDescription = "Ausgewaehlt"        // LEICHT ZU UEBERSEHEN!
}
```

### Kategorie 7: String.format() und buildString

```kotlin
val text = String.format("Eintrag %d von %d", current, total) // TREFFER!
val text = buildString {
    append("Erstellt am ")                                       // TREFFER!
    append(dateString)
}
```

### Kategorie 8: Annotated Strings in Compose

```kotlin
buildAnnotatedString {
    append("Durch Fortfahren stimmst du unseren ")  // TREFFER!
    pushStringAnnotation(tag = "terms", annotation = "...")
    withStyle(SpanStyle(color = Color.Blue)) {
        append("Nutzungsbedingungen")                // TREFFER!
    }
    pop()
    append(" zu.")                                   // TREFFER!
}
```

### Was NICHT extrahiert werden darf

| Kategorie | Beispiel | Warum nicht |
|-----------|---------|-------------|
| Log-Statements | `Log.d("TAG", "User logged in")` | Nie benutzersichtbar |
| Analytics-Events | `analytics.log("screen_view")` | Technische Bezeichner |
| JSON-Keys | `jsonObject.getString("name")` | Daten-Keys, nicht UI |
| SharedPreferences-Keys | `prefs.getString("theme", "dark")` | Interne Keys |
| URL-Pfade | `"https://api.example.com/v1"` | Technisch |
| Regex-Pattern | `Regex("[a-z]+")` | Technisch |
| Tag-Konstanten | `const val TAG = "JournalVM"` | Nur fuer Logging |
| Test-Strings | In `src/test/` oder `src/androidTest/` | Nicht in Produktion |

---

### Kategorie 9: Unicode-Escape-Sequenzen fuer Umlaute (PFLICHT-Kapitel)

> **Entstanden durch Vorfall 2026-04-16:** Das Wort `"Überblick"` war als
> `"\u00dcberblick"` hardcoded in DashboardScreen.kt. Der Skill hat es nicht
> gefunden, weil die Standard-Grep-Muster nach echten Umlauten wie `Ü` suchen —
> die Escape-Form `\u00dc` rutscht durch als harmloser ASCII-Text. Ergebnis:
> Der Benutzer sah "Überblick" in der englischen App, obwohl UI und KI-Antwort
> komplett englisch waren.

**Problem:** Kotlin-Compiler und Android akzeptieren Unicode-Escapes als
aequivalent zu echten Zeichen. Entwickler schreiben manchmal aus Copy-Paste-
Fehlern oder Legacy-Gruenden `"\u00dcberblick"` statt `"Überblick"`. Beide
sind zur Laufzeit identisch, aber Grep nach `Ü` findet die Escape-Form nicht.

**Die 7 deutschen Umlaut-Escape-Sequenzen:**

| Zeichen | Unicode-Escape | HTML-Entity | Beispiel-Wort im Code |
|---------|---------------|-------------|----------------------|
| Ü | `\u00dc` | `&Uuml;` | `"\u00dcberblick"` = "Überblick" |
| ü | `\u00fc` | `&uuml;` | `"M\u00fcll"` = "Müll" |
| Ö | `\u00d6` | `&Ouml;` | `"\u00d6ffentlich"` = "Öffentlich" |
| ö | `\u00f6` | `&ouml;` | `"h\u00f6ren"` = "hören" |
| Ä | `\u00c4` | `&Auml;` | `"\u00c4nderung"` = "Änderung" |
| ä | `\u00e4` | `&auml;` | `"w\u00e4hlen"` = "wählen" |
| ß | `\u00df` | `&szlig;` | `"gr\u00fc\u00dfen"` = "grüßen" |

**PFLICHT-Grep-Muster fuer JEDEN Durchlauf:**

```
Pattern: \\u00(dc|fc|d6|f6|c4|e4|df)
Path:    app/src/main/java/
Glob:    *.kt
Output:  content (Zeilen mit Kontext)
```

Alle Treffer sind **potenzielle** hardcodierte Strings. Pruefen ob sie
UI-Text sind (sichtbar fuer Benutzer) oder technisch (Regex, Konstante).

**Breiterer Scan fuer alle Unicode-Escapes (niedrige Prioritaet):**

```
Pattern: \\u[0-9a-fA-F]{4}
```

Findet auch `\u0020` (Leerzeichen), `\u00a0` (non-breaking space) etc.
Die meisten davon sind technisch, aber manche koennten UI-Text sein
(z.B. `\u2013` = "–" en-dash, das oft statt `-` in UI verwendet wird).

**Wie Treffer dem Benutzer zeigen:**

Format: `Datei:Zeile: "ESCAPE_FORM" (= "DECODIERTE_FORM")`

Beispiel:
```
DashboardScreen.kt:666: "\u00dcberblick" (= "Überblick")
```

Damit der Benutzer sofort versteht was hinter dem Escape steckt, ohne
selbst dekodieren zu muessen.

**Ausnahmen (NICHT extrahieren, obwohl sie den Pattern matchen):**

| Kontext | Grund |
|---------|-------|
| `Regex("\\u[0-9]+")` als Pattern | Technisch, dekodiert zur Laufzeit andere Strings |
| `"\uFFFD"` (Replacement Character) | Technisch, steht fuer ungueltige Zeichen |
| `"\u0020"` (Leerzeichen), `"\u00a0"` (NBSP) | Formatierung, keine UI-Worte |
| Kommentare wie `// \u00dc = U-Umlaut` | Informativ, nicht ausgefuehrt |
| JavaScript-Escape `\x{00dc}` | Nur relevant wenn JS-Code eingebettet |
| Test-Dateien | `src/test/`, `src/androidTest/` |

**Bonus: HTML-Entities (seltener, aber pruefen bei Apps mit HTML-Rendering):**

```
Pattern: &(Uuml|uuml|Ouml|ouml|Auml|auml|szlig);
```

Diese kommen in `AnnotatedString`, HTML-Preview-Screens und WebView-Code vor.

---

## 5. Android Lint als automatischer Detektor

### Relevante Lint-Checks

| Lint-ID | Was es findet | Wo |
|---------|-------------|-----|
| `HardcodedText` | Hardcodierter Text in XML-Attributen (`android:text="..."`) | XML-Layouts |
| `SetTextI18n` | `textView.text = "..."` direkt im Kotlin/Java-Code | Kotlin/Java |

### Lint ausfuehren

```bash
# Vollstaendiger Lint-Lauf:
./gradlew lint

# Nur i18n-relevante Issues filtern:
./gradlew lint 2>&1 | grep -E "HardcodedText|SetTextI18n"

# HTML-Report generieren:
./gradlew lintDebug
# → build/reports/lint-results-debug.html
```

### Lint-Limitierungen (warum Grep zusaetzlich noetig ist)

Lint findet NICHT:
- Strings in Compose `Text("...")` — kein Lint-Check dafuer
- Strings in Toast/Snackbar-Aufrufen
- Strings in `when`-Ausdruecken die String zurueckgeben
- Strings in Data-Klassen und Sealed Classes
- Content Descriptions die direkt als String gesetzt werden
- Strings in ViewModel-Properties

**Fazit:** Lint ist ein guter Startpunkt, aber deckt nur ~60% ab.
Die restlichen 40% muessen per Grep und manuellem Walkthrough gefunden werden.

---

## 6. Screen-fuer-Screen-Workflow

### Der effektivste Ablauf fuer eine grosse App

```
Fuer JEDEN Screen der App:

1. SCREEN OEFFNEN (mental oder auf dem Geraet)
   → Jeden sichtbaren Text notieren

2. KOTLIN-DATEI OEFFNEN (ScreenName.kt / ScreenNameScreen.kt)
   → Grep-Muster 1-9 anwenden
   → Jeden Treffer als String-Ressource erfassen

3. VIEWMODEL OEFFNEN (ScreenNameViewModel.kt)
   → Nach Strings suchen die in der UI landen
   → Besonders: Error-Messages, State-Texte, Formatierungen

4. ZUGEHOERIGE COMPOSABLES PRUEFEN (Components, Dialogs, BottomSheets)
   → Oft werden Unter-Composables vergessen

5. STRINGS ERSTELLEN
   → Naming-Konvention anwenden (screen_element_beschreibung)
   → In strings.xml eintragen mit Kommentar
   → Hardcodierten String durch stringResource() ersetzen

6. BUILD PRUEFEN
   → Kompiliert es noch?
   → Keine fehlenden Imports (stringResource braucht R-Import)

7. NAECHSTER SCREEN
```

### Screen-Inventar erstellen (empfohlen)

Vor dem Start ein Inventar aller Screens automatisch generieren:

```bash
# Alle Screen-Dateien im Projekt finden:
find app/src/main/java -name "*Screen.kt" -o -name "*Dialog.kt" -o -name "*Overlay.kt" \
  | sort

# Zugehoerige ViewModels finden:
find app/src/main/java -name "*ViewModel.kt" | sort
```

Dann als Checkliste formatieren:

```
Screen-Inventar [Projektname]:
[ ] HomeScreen.kt              + HomeViewModel.kt
[ ] SettingsScreen.kt          + SettingsViewModel.kt
[ ] DetailScreen.kt            + DetailViewModel.kt
[ ] OnboardingScreen.kt        + OnboardingViewModel.kt
[ ] DeleteDialog.kt
[ ] ShareDialog.kt
[ ] RecordingOverlay.kt
```

### Reihenfolge der Screen-Bearbeitung

1. **Haupt-Screens zuerst** (Journal, Dashboard, Settings) — meiste Strings, groesster Impact
2. **Dialoge und Overlays** (Delete-Dialog, Share-Dialog, Recording-Overlay) — wenige aber wichtige Strings
3. **Onboarding** — einmalig sichtbar, aber fuer erste Impression kritisch
4. **Fehler- und Edge-Case-Screens** (Empty States, Error States) — leicht zu vergessen

---

## 7. Priorisierung — welche Strings zuerst

### Prioritaet 1: Immer sichtbare UI-Texte (SOFORT)

- Screen-Titel (TopAppBar)
- Navigation-Labels (BottomNav, Tabs)
- Haeufig angezeigte Buttons (Speichern, Abbrechen, Loeschen)
- Hauptinhalt-Texte auf jedem Screen

### Prioritaet 2: Interaktions-Texte (HOCH)

- Dialog-Titel und -Nachrichten
- Toast/Snackbar-Nachrichten
- Fehler-Meldungen die der Benutzer sieht
- Eingabefeld-Labels und Hints

### Prioritaet 3: Sekundaere Texte (MITTEL)

- Einstellungs-Beschreibungen
- Onboarding-Texte
- Empty-State-Nachrichten ("Noch keine Eintraege")
- Tooltips und Info-Texte

### Prioritaet 4: Accessibility und Meta (NIEDRIG, aber wichtig)

- Content Descriptions fuer Icons und Bilder
- State Descriptions fuer Screenreader
- Semantik-Annotationen

### Was NICHT extrahiert wird (Prioritaet 0 — ignorieren)

- Log-Statements
- Analytics-Event-Namen
- SharedPreferences-Keys
- JSON/API-Keys
- Interne Konstanten die nie in der UI erscheinen
- Test-Strings

---

## 8. Pseudolokalisierung — die zuverlaessigste Methode

### Warum das die wichtigste Methode ist

Bei aktiviertem Pseudolocale (`en-XA`) erscheinen korrekt externalisierte Strings mit
Akzenten und Sonderzeichen: `"Submit"` wird zu `"[Šübmïƭ_____]"`.
**Hardcodierte Strings bleiben unveraendert** — sie erscheinen als normaler Klartext
zwischen akzentiertem Text. Das ist visuell SOFORT erkennbar.

### Einrichten (einmalig)

```kotlin
// build.gradle.kts
android {
    buildTypes {
        getByName("debug") {
            isPseudoLocalesEnabled = true
        }
    }
}
```

### Durchfuehrung

1. App im Debug-Modus starten
2. Geraet/Emulator auf **"English (XA)"** stellen
3. JEDEN Screen durchklicken — besonders:
   - Fehlerzustaende ausloesen (offline gehen, falsches Passwort)
   - Seltene Dialoge oeffnen (Account-Loeschung, Datenexport)
   - Onboarding neu durchlaufen
   - Notifications empfangen
   - Empty States provozieren (alle Eintraege loeschen)
4. **Jeder unakzentuierte Text = vergessener String** → Screenshot + Notiz

### Zwei Pseudolocales

| Locale | Name | Was es tut | Findet |
|--------|------|-----------|--------|
| `en-XA` | English (XA) | Fuegt Akzente hinzu + expandiert Text 30-40% | Hardcodierte Strings (bleiben unakzentuiert!), Layout-Overflow |
| `ar-XB` | Arabic (XB) | Spiegelt Text (RTL-Simulation) | RTL-Layout-Fehler, BIDI-Probleme |

### Achtung: resConfigs kann Pseudolocales herausfiltern!

```kotlin
// FALSCH: Das filtert Pseudolocales aus dem APK!
defaultConfig {
    resourceConfigurations += setOf("de", "en")
}

// RICHTIG: Pseudolocales explizit einschliessen (oder resConfigs nur im Release setzen)
```

---

## 9. Die "letzten 5%" — Strings die IMMER vergessen werden

### Kategorie 1: Notification-Strings

```kotlin
// FAST IMMER VERGESSEN:
NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle("Taegliche Erinnerung")  // hardcoded!
    .setContentText("Zeit zum Schreiben!")     // hardcoded!
```

### Kategorie 2: Enum-Labels und When-Ausdruecke

```kotlin
enum class Mood(val label: String) {
    HAPPY("Gut gelaunt"),   // hardcoded!
    SAD("Traurig"),         // hardcoded!
}

// FIX: StringRes-Annotation
enum class Mood(@StringRes val labelRes: Int) {
    HAPPY(R.string.mood_happy),
    SAD(R.string.mood_sad),
}
```

### Kategorie 3: ViewModel-Strings die in die UI fliessen

```kotlin
class JournalViewModel : ViewModel() {
    fun getStreakMessage(days: Int): String {
        return if (days == 0) "Starte deine Serie heute!" // hardcoded!
        else "$days Tage Serie!"                           // hardcoded!
    }
}
```

### Kategorie 4: Dynamisch zusammengebaute Strings

```kotlin
// Wird von KEINEM Tool automatisch erkannt:
val message = "Hallo " + userName + ", du hast " + count + " Eintraege"
val title = "Woche $weekNumber Zusammenfassung"
```

### Kategorie 5: AnnotatedString in Compose

```kotlin
buildAnnotatedString {
    append("Durch Fortfahren stimmst du unseren ")  // hardcoded!
    withStyle(SpanStyle(color = Color.Blue)) {
        append("Nutzungsbedingungen")                // hardcoded!
    }
    append(" zu.")                                   // hardcoded!
}
```

### Kategorie 6: Content Descriptions (Accessibility)

```kotlin
Icon(Icons.Default.Edit, contentDescription = "Eintrag bearbeiten") // hardcoded!
Image(painter, contentDescription = "Profilbild")                    // hardcoded!
```

### Kategorie 7: String-Initialisierungen in Listen

```kotlin
val tips = listOf(
    "Schreibe jeden Tag einen Satz",            // hardcoded!
    "Nutze die Sprachaufnahme fuer unterwegs",  // hardcoded!
)
```

### Kategorie 8: Debug-Menu-Strings die in Produktion landen

```kotlin
if (BuildConfig.DEBUG) {
    items.add(MenuItem("Force Crash", ...))     // Kann in Produktion leaken!
    items.add(MenuItem("Clear All Data", ...))  // Wenn BuildConfig falsch konfiguriert
}
```

### Kategorie 9: Companion-Object-Konstanten

```kotlin
companion object {
    const val DEFAULT_TITLE = "Mein Tagebuch"          // hardcoded wenn in UI!
    const val ERROR_MSG = "Ein Fehler ist aufgetreten"  // hardcoded!
}
```

### Kategorie 10: Implizite Format-Strings

```kotlin
// Das Pattern ist nicht uebersetzbar:
val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

// BESSER: System-lokalisiertes Format nutzen:
val formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
```

---

## 10. Praevention — neue hardcodierte Strings verhindern

### Pre-Commit Hook (verhindert neue Fehler)

```bash
#!/bin/bash
# .git/hooks/pre-commit
STAGED_KT=$(git diff --cached --name-only --diff-filter=ACM | grep '\.kt$')
ERRORS=0

for FILE in $STAGED_KT; do
    VIOLATIONS=$(git diff --cached "$FILE" | grep "^+" | \
        grep -E 'Text\("[A-Za-z ]{3,}"\)|title\s*=\s*"[A-Za-z ]{3,}"' | \
        grep -v '//\|stringResource\|@Suppress\|@Preview')
    if [ -n "$VIOLATIONS" ]; then
        echo "FEHLER: Hardcoded strings in $FILE:"
        echo "$VIOLATIONS"
        ERRORS=$((ERRORS + 1))
    fi
done

if [ $ERRORS -gt 0 ]; then
    echo "Bitte stringResource() verwenden statt hardcodierter Strings."
    exit 1
fi
exit 0
```

### CI-Check (GitHub Actions)

```yaml
name: i18n String Check
on: [pull_request]
jobs:
  check-hardcoded-strings:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Check for hardcoded Compose strings
        run: |
          git diff origin/main...HEAD --name-only --diff-filter=ACM \
            | grep '\.kt$' \
            | xargs -I{} grep -n 'Text(\s*"[^"]\{3,\}"' {} \
            | grep -v 'stringResource\|@Preview' \
            | tee /tmp/findings.txt
          [ -s /tmp/findings.txt ] && exit 1 || exit 0
```

### i18n-Coverage messen

```bash
#!/bin/bash
# i18n_coverage.sh
HARDCODED=$(grep -rn --include="*.kt" \
    -E 'Text\("([^"]{3,})"\)' app/src/main/ | \
    grep -v 'stringResource\|@Preview' | wc -l)
EXTERNALIZED=$(grep -c '<string name=' app/src/main/res/values/strings.xml)
TOTAL=$((HARDCODED + EXTERNALIZED))
COVERAGE=$(echo "scale=1; $EXTERNALIZED * 100 / $TOTAL" | bc)
echo "i18n Coverage: $COVERAGE% ($EXTERNALIZED externalisiert, $HARDCODED noch hardcodiert)"
```

### String-Freeze vor Releases (wie Mozilla, Brave, Nextcloud)

Grosse Projekte frieren Strings 1-2 Wochen vor einem Release ein:
- Keine neuen Strings nach dem Freeze
- Uebersetzer haben garantiert Zeit alle Strings zu uebersetzen
- CI blockiert PRs die Strings in `strings.xml` aendern

---

# TEIL B: STRINGS ERSTELLEN

## 11. Die goldenen Regeln

### Regel 1: JEDER benutzersichtbare Text gehoert in strings.xml

```xml
<!-- FALSCH — Android kann das nicht uebersetzen -->
android:text="Einstellungen speichern"

<!-- RICHTIG -->
android:text="@string/settings_button_save"
```

### Regel 2: Die Default-Datei muss VOLLSTAENDIG sein

`res/values/strings.xml` ist der Fallback fuer ALLE Sprachen. Fehlt dort ein String,
crasht die App bei Nutzern deren Sprache nicht unterstuetzt wird.

### Regel 3: Nie Saetze zusammenbauen — immer vollstaendige Saetze als String

```kotlin
// FALSCH — bricht in JEDER nicht-englischen Sprache
val message = "Willkommen " + userName + "!"

// RICHTIG — Uebersetzer kann Wortstellung aendern
val message = getString(R.string.welcome_user, userName)
```

```xml
<string name="welcome_user">Willkommen, %1$s!</string>
```

**Warum:** In Tuerkisch wird "in" zum Suffix am Wort. In Japanisch steht der Name
am Satzanfang. Nur vollstaendige Saetze mit Platzhaltern ermoeglichen korrekte Uebersetzung.

### Regel 4: Niemals den gleichen String in verschiedenen Kontexten wiederverwenden

Das Wort "Fertig" kann auf Englisch "Done" (Aufgabe abgeschlossen) oder "Ready"
(bereit) bedeuten — in vielen Sprachen komplett verschiedene Woerter.

```xml
<!-- FALSCH: Ein String, verschiedene Bedeutungen -->
<string name="done">Fertig</string>

<!-- RICHTIG: Eigener String pro Kontext -->
<!-- Button after completing an entry edit -->
<string name="journal_button_done">Fertig</string>
<!-- Status label when recording is ready -->
<string name="recording_label_ready">Bereit</string>
```

### Regel 5: Plural-Logik IMMER ueber das Plural-System, NIE im Code

```kotlin
// FALSCH — funktioniert nicht in Russisch, Arabisch, Polnisch...
val text = if (count == 1) "1 Eintrag" else "$count Eintraege"

// RICHTIG
val text = resources.getQuantityString(R.plurals.journal_entries, count, count)
```

---

## 12. String-Naming-Konvention

### Das bewaehrte Schema: `<WO>_<ELEMENT>_<BESCHREIBUNG>`

```
<SCREEN/MODUL>_<ELEMENT_TYP>_<BESCHREIBUNG>
```

### Konkrete Beispiele

```xml
<!-- Screen-spezifische Strings: Prefix = Screen-Name -->
<string name="onboarding_title_welcome">Willkommen</string>
<string name="onboarding_button_next">Weiter</string>
<string name="journal_title_new_entry">Neuer Eintrag</string>
<string name="journal_button_save">Speichern</string>
<string name="journal_error_save_failed">Eintrag konnte nicht gespeichert werden</string>
<string name="settings_title_screen">Einstellungen</string>
<string name="settings_label_language">Sprache</string>
<string name="settings_dialog_title_logout">Abmelden?</string>

<!-- App-weite Strings: Prefix = "all_" -->
<string name="all_button_ok">OK</string>
<string name="all_button_cancel">Abbrechen</string>
<string name="all_error_network">Keine Internetverbindung</string>
<string name="all_loading">Laden\u2026</string>
```

### Element-Typ-Suffixe

| Suffix | Verwendung | Beispiel |
|--------|-----------|---------|
| `_title` | Screen-Titel, Dialog-Titel | `journal_title_entries` |
| `_label` | Beschriftungen, Felder | `settings_label_language` |
| `_hint` | Eingabefeld-Platzhalter | `onboarding_hint_email` |
| `_button` | Button-Text | `journal_button_save` |
| `_error` | Fehlermeldungen | `settings_error_save_failed` |
| `_dialog_title` | Dialog-Kopfzeile | `logout_dialog_title` |
| `_dialog_message` | Dialog-Inhalt | `logout_dialog_message` |
| `_toast` | Toast-Nachrichten | `journal_toast_saved` |
| `_snack` | Snackbar-Text | `journal_snack_undo` |
| `_desc` | Content Descriptions (Accessibility) | `mic_button_desc` |
| `_empty` | Empty-State-Texte | `journal_empty_no_entries` |

### Was NIEMALS gemacht werden darf

```xml
<!-- SCHLECHT: bedeutungslos -->
<string name="text1">Speichern</string>
<string name="error7">Fehler aufgetreten</string>

<!-- SCHLECHT: zu generisch, kein Kontext fuer Uebersetzer -->
<string name="save">Speichern</string>
```

---

## 13. Platzhalter und Format-Specifier

### IMMER nummerierte Positionen verwenden

```xml
<!-- FALSCH — Reihenfolge kann nicht veraendert werden -->
<string name="user_action">%s hat %s ein Foto geliked</string>

<!-- RICHTIG — Uebersetzer kann Reihenfolge anpassen -->
<string name="user_action">%1$s hat das Foto von %2$s geliked</string>
```

### Format-Specifier-Referenz

| Specifier | Typ | Beispiel | Hinweis |
|-----------|-----|---------|---------|
| `%1$s` | String | `%1$s hat geantwortet` | Fuer Namen, Texte |
| `%1$d` | Integer | `%1$d Nachrichten` | Fuer ganze Zahlen |
| `%1$f` | Float | `%1$f MB` | Selten direkt verwenden |
| `%1$.2f` | Float, 2 Nachkommastellen | `%1$.2f MB` | Fuer Preise, Groessen |

**Immer nummerieren, auch bei nur einem Platzhalter** — Konsistenz und Zukunftssicherheit.

---

## 14. XLIFF-Tags fuer nicht-uebersetzbare Inhalte

### Namespace deklarieren

```xml
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
```

### Platzhalter schuetzen

```xml
<!-- Benutzername -->
<string name="greeting">
    Hallo, <xliff:g id="user_name" example="Maria">%1$s</xliff:g>!
</string>

<!-- Zeitangabe -->
<string name="countdown">
    Noch <xliff:g id="time_remaining" example="5 Tage">%1$s</xliff:g> bis zum Urlaub
</string>

<!-- Brand-Name -->
<string name="powered_by">
    Powered by <xliff:g id="brand" example="Whisper AI">Whisper AI</xliff:g>
</string>
```

### Beide Attribute sind PFLICHT

| Attribut | Zweck | Beispiel |
|----------|-------|---------|
| `id` | Erklaert dem Uebersetzer WAS das ist | `"user_name"`, nicht `"x"` |
| `example` | Zeigt WIE der Wert zur Laufzeit aussieht | `"Maria"`, `"5 Tage"` |

---

## 15. Plurals korrekt anlegen

### Default-Sprache (Deutsch): Immer `one` und `other`

```xml
<plurals name="journal_entries">
    <item quantity="one">%d Eintrag</item>
    <item quantity="other">%d Eintr\u00e4ge</item>
</plurals>
```

### `other` ist PFLICHT — immer. Ohne `other` crasht die App.

### Korrekter Abruf im Code

```kotlin
// count wird ZWEIMAL uebergeben!
val text = resources.getQuantityString(
    R.plurals.journal_entries,
    count,   // <-- Kategorie-Auswahl (one/few/many/other)
    count    // <-- Wert fuer %d
)
```

### Welche Kategorien spaeter in Uebersetzungen gebraucht werden

Diese Tabelle ist Referenz fuer den Uebersetzer — in der Default-Sprache (Deutsch)
reichen `one` + `other`. Die anderen Kategorien werden erst in den jeweiligen
Sprach-Dateien (`values-ar/`, `values-ru/` etc.) ergaenzt.

| Sprache | Kategorien | Bemerkung |
|---------|-----------|-----------|
| Deutsch/Englisch | `one`, `other` | Standard |
| Russisch/Polnisch | `one`, `few`, `many`, `other` | Komplex |
| Arabisch | `zero`, `one`, `two`, `few`, `many`, `other` | Alle 6! |
| Japanisch/Chinesisch/Tuerkisch | `other` | Nur 1 Form |

---

## 16. Uebersetzer-Kommentare

### Kommentar unmittelbar VOR dem String-Element

```xml
<!-- Shown on the journal entry screen below the title. Max 60 characters. -->
<string name="journal_label_entry_count">Du hast %1$d Eintraege</string>

<!-- Button to start a new voice recording. Keep short — fits on a 100dp button. -->
<string name="journal_button_record">Aufnehmen</string>

<!-- Error shown when saving fails. %1$s = error reason from server (e.g., "Timeout") -->
<string name="journal_error_save_failed">Speichern fehlgeschlagen: %1$s</string>
```

### Was ein guter Kommentar enthaelt

| Element | Beispiel |
|---------|---------|
| **Wo** der String erscheint | `"Shown on the journal entry screen"` |
| **Platzhalter-Erklaerung** | `"%1$s = username, %2$d = entry count"` |
| **Zeichenbegrenzung** | `"Max 40 chars — fits in header bar"` |
| **Ton/Register** | `"Use informal tone (du, not Sie)"` |
| **Mehrdeutigkeit aufklaeren** | `"'Fertig' here means 'completed', not 'ready'"` |

### Wann ein Kommentar PFLICHT ist

- Jeder String mit einem Platzhalter (`%1$s`, `%1$d`)
- Jeder einwortige String (mehrdeutig ohne Kontext)
- Jeder String mit Zeichenbegrenzung (Buttons, Tabs)
- Jeder String der in einem Dialog erscheint (Kontext unklar)

### Wann KEIN Kommentar noetig ist

- `all_button_ok` → "OK" ist universell klar
- `app_name` → wird nie uebersetzt
- Langer Fliesstext der sich selbst erklaert

---

## 17. Nicht-uebersetzbare Strings

### `translatable="false"` fuer permanente Ausnahmen

```xml
<string name="app_name" translatable="false">MyApp</string>
<string name="firebase_project_id" translatable="false">myapp-prod</string>
<string name="url_privacy_policy" translatable="false">https://example.com/privacy</string>
<string name="date_format_iso" translatable="false">yyyy-MM-dd</string>
```

### Eigene Datei: `donottranslate.xml` (Google-Methode)

Android Lint behandelt ALLE Strings in einer Datei namens `donottranslate.xml` automatisch
als nicht uebersetzbar:

```xml
<!-- res/values/donottranslate.xml -->
<resources>
    <string name="app_name">MyApp</string>
    <string name="firebase_project_id">myapp-prod</string>
    <string name="support_email">support@example.com</string>
</resources>
```

---

## 18. String Arrays

### Immer Referenzen statt direkten Text verwenden

```xml
<!-- SICHER — Verweise auf einzelne Strings -->
<string name="mood_option_terrible">Furchtbar</string>
<string name="mood_option_bad">Schlecht</string>
<string name="mood_option_neutral">Neutral</string>

<string-array name="mood_options">
    <item>@string/mood_option_terrible</item>
    <item>@string/mood_option_bad</item>
    <item>@string/mood_option_neutral</item>
</string-array>
```

**Warum:** Wenn ein Array-Item in einer Uebersetzung fehlt, verschwindet der
gesamte Eintrag ohne Fallback. Bei `@string/`-Referenzen greift der Fallback.

---

## 19. HTML und Styled Text in Strings

### Einfaches HTML

```xml
<string name="welcome_message">Willkommen bei <b>MyApp</b>!</string>
```

### HTML mit Platzhaltern: CDATA verwenden

```xml
<string name="welcome_with_name"><![CDATA[Willkommen, <b>%1$s</b>!]]></string>
```

### Annotation-Spans fuer uebersetzbare Links

```xml
<string name="terms_text">
    Mit dem Fortfahren stimmst du unseren
    <annotation link="terms">Nutzungsbedingungen</annotation>
    und unserer
    <annotation link="privacy">Datenschutzerklaerung</annotation> zu.
</string>
```

### Unterstuetzte HTML-Tags

`<b>`, `<i>`, `<u>`, `<strike>`, `<sup>`, `<sub>`, `<big>`, `<small>`,
`<font color="#...">`, `<br>`, `<a href="...">`

---

## 20. Compose-spezifische Patterns

### `stringResource()` fuer Composables

```kotlin
@Composable
fun MyScreen() {
    Text(text = stringResource(R.string.journal_title))
    Text(text = stringResource(R.string.greeting, userName))
}
```

### `pluralStringResource()` fuer Plurals

```kotlin
Text(text = pluralStringResource(R.plurals.journal_entries, count, count))
```

### ViewModel: String-IDs statt aufgeloeste Strings

```kotlin
// FALSCH: String im ViewModel resolven
class MyViewModel {
    val text = context.getString(R.string.title) // Lifecycle-Problem!
}

// RICHTIG: Nur die ID weitergeben, UI resolved
class MyViewModel {
    val titleResId: Int = R.string.title
}
// In Compose:
Text(text = stringResource(viewModel.titleResId))
```

### Dynamische Strings: UiText-Pattern (Best Practice)

Fuer ViewModels die verschiedene String-Typen zurueckgeben muessen:

```kotlin
sealed class UiText {
    data class StringResource(val resId: Int, val args: List<Any> = emptyList()) : UiText()
    data class DynamicString(val value: String) : UiText()

    @Composable
    fun asString(): String = when (this) {
        is StringResource -> stringResource(resId, *args.toTypedArray())
        is DynamicString -> value
    }
}

// Im ViewModel:
val errorMessage: UiText = UiText.StringResource(R.string.journal_error_save_failed, listOf(reason))

// In Compose:
Text(text = viewModel.errorMessage.asString())
```

---

## 21. Dateiorganisation bei grossen Apps

### Ab ~200 Strings aufteilen

Android merged alle XML-Dateien in `res/values/` automatisch.

```
res/values/
  strings.xml               <-- app_name + globale Strings
  strings_common.xml         <-- Shared Actions, Errors, A11y Labels
  strings_journal.xml        <-- Journal-Screen
  strings_settings.xml       <-- Settings-Screen
  strings_onboarding.xml     <-- Onboarding-Flow
  strings_dashboard.xml      <-- Dashboard-Screen
  strings_recording.xml      <-- Recording-Feature
  donottranslate.xml         <-- Alle nicht-uebersetzbaren Strings
```

### Zuordnungsregel

| Wohin | Welche Strings |
|-------|---------------|
| `strings.xml` | Nur `app_name` und Strings die von >3 Features genutzt werden |
| `strings_common.xml` | `all_button_ok`, `all_error_network`, `cd_back_button` |
| `strings_<feature>.xml` | Alles was einem Screen/Feature gehoert |
| `donottranslate.xml` | URLs, Firebase-IDs, Brand-Namen, Format-Templates |

---

## 22. Anti-Patterns — was NIEMALS gemacht werden darf

### 1. String-Konkatenation (HAEUFIGSTER Fehler!)

```kotlin
val msg = "Willkommen " + userName + "!"           // FALSCH
val msg = getString(R.string.welcome) + " " + name // FALSCH
val msg = getString(R.string.welcome_user, userName) // RICHTIG
```

### 2. Plural-Logik im Code

```kotlin
val text = if (count == 1) "1 Eintrag" else "$count Eintraege" // FALSCH
val text = resources.getQuantityString(R.plurals.entries, count, count) // RICHTIG
```

### 3. UI-Element-Namen im String

```xml
<!-- FALSCH: "Einstellungen" heisst in jeder Sprache anders -->
<string name="instruction">Tippe auf den Einstellungen-Button</string>
```

### 4. Datum/Zeit/Waehrung hardcodieren

```kotlin
// FALSCH: Format ist kulturspezifisch
getString(R.string.posted_on, "17.04.2026")

// RICHTIG: Locale-aware Formatierung
val formatted = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(date)
getString(R.string.posted_on, formatted)
```

### 5. Brand-Namen ohne XLIFF-Schutz

```xml
<!-- FALSCH -->
<string name="about">Powered by Whisper AI</string>

<!-- RICHTIG -->
<string name="about">
    Powered by <xliff:g id="brand" example="Whisper AI">Whisper AI</xliff:g>
</string>
```

### 6. Fehlende `other`-Pluralform

Ohne `other` crasht die App. `other` ist die EINZIGE Pflicht-Kategorie.

### 7. Falsches Verzeichnis-Naming

```
res/values-pt-BR/    <-- FALSCH (Android ignoriert das still!)
res/values-pt-rBR/   <-- RICHTIG (kleines r ist Pflicht)
```

### 8. Accessibility-Strings vergessen

```kotlin
contentDescription = "share"                          // FALSCH
contentDescription = getString(R.string.cd_share)     // RICHTIG
```

### 9. Nicht-nummerierte Platzhalter

```xml
<string name="info">%s hat %s geliked</string>           <!-- FALSCH -->
<string name="info">%1$s hat %2$s geliked</string>       <!-- RICHTIG -->
```

### 10. Strings im ViewModel resolven

```kotlin
val text = context.getString(R.string.title) // FALSCH (im ViewModel)
val titleResId = R.string.title              // RICHTIG (nur ID)
```

---

# TEIL C: QUALITAETSSICHERUNG

## 23. Automatische Qualitaetspruefung (Lint und CI)

### Lint verschaerfen in build.gradle.kts

```kotlin
android {
    lint {
        error("MissingTranslation")
        error("HardcodedText")
        warning("MissingQuantity")
        htmlReport = true
        htmlOutput = file("build/reports/lint-results.html")
        abortOnError = true
    }
}
```

### Default-Locale im strings.xml angeben

```xml
<resources xmlns:tools="http://schemas.android.com/tools"
    xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
    tools:locale="de">
```

### Pseudolokalisierung aktivieren

```kotlin
android {
    buildTypes {
        debug {
            isPseudoLocalesEnabled = true
        }
    }
}
```

- **`English (XA)`**: Simuliert +30-40% Textexpansion, markiert hardcodierte Strings
- **`AR (XB)`**: Simuliert RTL-Layout

### resConfigs — nur unterstuetzte Sprachen

```kotlin
android {
    defaultConfig {
        resourceConfigurations += setOf("de", "en", "fr", "es", "hi", "ar", "zh", "ja", "ko", "pt", "ru", "tr")
    }
}
```

---

## 24. Abschluss-Checkliste

### Nach dem Finden (Teil A)

| # | Pruefpunkt |
|---|-----------|
| 1 | Lint `HardcodedText` meldet 0 Treffer in XML? |
| 2 | Grep-Muster 1-9 auf `.kt`-Dateien angewendet? |
| 3 | **PFLICHT:** Unicode-Escape-Scan (Kategorie 9) durchgefuehrt mit Pattern `\\u00(dc\|fc\|d6\|f6\|c4\|e4\|df)`? |
| 4 | Alle Screens im Screen-Inventar abgehakt? |
| 5 | ViewModels auf benutzersichtbare Strings geprueft? |
| 6 | Content Descriptions fuer alle Icons/Bilder vorhanden? |

### Nach dem Erstellen (Teil B)

| # | Pruefpunkt |
|---|-----------|
| 6 | Naming-Konvention `screen_element_beschreibung` konsistent? |
| 7 | Alle Platzhalter nummeriert (`%1$s`, nicht `%s`)? |
| 8 | `xliff:g` fuer alle Platzhalter mit `id` + `example`? |
| 9 | Uebersetzer-Kommentare fuer mehrdeutige Strings? |
| 10 | `translatable="false"` fuer URLs, Brand-Namen, Keys? |
| 11 | Alle `<plurals>` haben `other`-Kategorie? |
| 12 | String Arrays nutzen `@string/`-Referenzen? |
| 13 | `tools:locale="de"` im `<resources>`-Tag gesetzt? |
| 14 | Build kompiliert ohne Fehler? |

---

## 25. Vollstaendiges Beispiel: Best-Practice strings.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
    xmlns:tools="http://schemas.android.com/tools"
    tools:locale="de">

    <!-- ═══════════════════════════════════════════════════
         APP-WEITE STRINGS
         ═══════════════════════════════════════════════════ -->

    <!-- App name — do not translate (replace with your app name) -->
    <string name="app_name" translatable="false">MyApp</string>

    <!-- Generic OK confirmation button -->
    <string name="all_button_ok">OK</string>
    <!-- Generic cancel button — keep short, max 10 chars -->
    <string name="all_button_cancel">Abbrechen</string>
    <!-- Generic loading indicator text -->
    <string name="all_loading">Laden\u2026</string>
    <!-- Generic network error — shown as a Snackbar -->
    <string name="all_error_network">Keine Internetverbindung.</string>


    <!-- ═══════════════════════════════════════════════════
         JOURNAL SCREEN
         ═══════════════════════════════════════════════════ -->

    <!-- Main screen title in the top app bar -->
    <string name="journal_title_screen">Mein Tagebuch</string>

    <!-- Empty state: shown when user has no entries yet. \n = line break. -->
    <string name="journal_empty_no_entries">Noch keine Eintraege.\nTippe auf + um deinen ersten zu schreiben.</string>

    <!-- Save button in the entry editor. Fits on a 100dp button. -->
    <string name="journal_button_save">Speichern</string>

    <!-- Delete confirmation dialog title.
         %1$s = entry title (a short text string, e.g. "Mein Tag"). -->
    <string name="journal_dialog_title_delete">
        \u201c<xliff:g id="entry_title" example="Mein Tag">%1$s</xliff:g>\u201d loeschen?
    </string>

    <!-- Delete confirmation dialog body — keep concise, 1-2 sentences max. -->
    <string name="journal_dialog_message_delete">Dieser Eintrag wird dauerhaft geloescht. Das kann nicht rueckgaengig gemacht werden.</string>

    <!-- Toast shown after successful save -->
    <string name="journal_toast_saved">Eintrag gespeichert.</string>

    <!-- Error shown when saving fails.
         %1$s = error reason from server (e.g., "Timeout"). -->
    <string name="journal_error_save_failed">
        Speichern fehlgeschlagen: <xliff:g id="error_reason" example="Timeout">%1$s</xliff:g>
    </string>

    <!-- Accessibility: content description for the floating action button (microphone) -->
    <string name="journal_desc_mic_button">Sprachaufnahme starten</string>


    <!-- ═══════════════════════════════════════════════════
         PLURALS
         ═══════════════════════════════════════════════════ -->

    <!-- Entry count for plural display. %d = count.
         Always pass count twice: getQuantityString(R.plurals.x, count, count) -->
    <plurals name="journal_entries">
        <item quantity="one">%d Eintrag</item>
        <item quantity="other">%d Eintr\u00e4ge</item>
    </plurals>

    <plurals name="recording_seconds">
        <item quantity="one">%d Sekunde</item>
        <item quantity="other">%d Sekunden</item>
    </plurals>


    <!-- ═══════════════════════════════════════════════════
         STRING ARRAYS (always use @string/ references!)
         ═══════════════════════════════════════════════════ -->

    <string name="mood_option_terrible">Furchtbar</string>
    <string name="mood_option_bad">Schlecht</string>
    <string name="mood_option_neutral">Neutral</string>
    <string name="mood_option_good">Gut</string>
    <string name="mood_option_great">Super</string>

    <string-array name="mood_options">
        <item>@string/mood_option_terrible</item>
        <item>@string/mood_option_bad</item>
        <item>@string/mood_option_neutral</item>
        <item>@string/mood_option_good</item>
        <item>@string/mood_option_great</item>
    </string-array>

</resources>
```

---

## Quellen

- [Android String Resources](https://developer.android.com/guide/topics/resources/string-resource)
- [Localize your app](https://developer.android.com/guide/topics/resources/localization)
- [Test with pseudolocales](https://developer.android.com/guide/topics/resources/pseudolocales)
- [Resources in Compose](https://developer.android.com/develop/ui/compose/resources)
- [CLDR Language Plural Rules](https://www.unicode.org/cldr/charts/42/supplemental/language_plural_rules.html)
- [Meta: Language Packs for Android](https://engineering.fb.com/2022/05/09/android/language-packs/)
- [Airbnb: Internationalization Platform](https://medium.com/airbnb-engineering/building-airbnbs-internationalization-platform-45cf0104b63c)
- [Netflix: Streaming In Your Language](https://netflixtechblog.medium.com/now-streaming-in-your-language-the-technology-behind-netflixs-global-interface-e0c732c69b16)
- [Phrase: Ultimate Guide to Android Localization](https://phrase.com/blog/posts/best-practices-for-android-localization-revisited-and-expanded/)
- [Jeroen Mols: XML Resource Naming](https://jeroenmols.com/blog/2016/03/07/resourcenaming/)
- [Localazy: How to Provide Comments](https://localazy.com/docs/android/how-to-provide-comments-for-strings)
- [MissingTranslation Lint Check](https://googlesamples.github.io/android-custom-lint-rules/checks/MissingTranslation.md.html)
