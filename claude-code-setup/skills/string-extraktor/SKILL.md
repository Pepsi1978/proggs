---
name: string-extraktor
description: Findet ALLE hardcodierten Strings in Android-Apps (Compose und XML) und erstellt sie nach deutschen Sprach- und Typografie-Regeln als strings.xml-Ressourcen. Pflicht-VORSTUFE zum uebersetzung-Skill — Qualität der deutschen Originale entscheidet über alle späteren Übersetzungen. Nutze diesen Skill IMMER wenn der Benutzer sagt "Strings finden", "Strings erstellen", "String-Extraktion", "hardcodierte Strings", "i18n Audit fuer App", "strings.xml auffüllen", "Strings überprüfen", "sind alle Strings da?" oder "Internationalisierung vorbereiten". Auch fuer Voice-Schreibweisen wie "String Extraktor", "String Extractor". Funktioniert fuer jedes Android-Projekt (Multi-Module unterstützt). Deckt auch Funktionalitäts-Checks ab ("prüfe ob Strings Funktionen kaputt machen" -> Phase 5) und Praevention-Setup ("Pre-Commit-Hook fuer i18n" -> Phase 6). NICHT verwenden fuer: Übersetzung deutscher Strings in andere Sprachen (-> uebersetzung-Skill), reine UI-Design-Audits (-> designer-Skill). Dieser Skill ERSTELLT deutsche Strings, der uebersetzung-Skill KONSUMIERT sie.
---

# String-Extraktor — Deutsche Strings finden, pruefen, erstellen

Dieser Skill arbeitet ein Android-Projekt systematisch ab um sicherzustellen,
dass **100% aller benutzersichtbaren Strings** als String-Ressourcen in
`strings.xml` vorliegen — auf korrektem Deutsch, typografisch sauber, mit
Plural-Unterstuetzung, bereit fuer den Uebersetzungs-Skill.

**Primaersprache:** Alle Apps werden zuerst auf Deutsch entwickelt. Die deutschen
Strings sind der Master — aus ihnen werden alle anderen Sprachen uebersetzt.
**Qualitaet der deutschen Originale entscheidet ueber die Qualitaet aller
Uebersetzungen.**

**Abgrenzung:** Dieser Skill ERSTELLT Strings in der Default-Sprache (Deutsch).
Er uebersetzt NICHT in andere Sprachen — dafuer gibt es den `uebersetzung`-Skill.

---

## Skill-Architektur

```
SKILL.md (diese Datei)         Kernablauf, Phasen 0-6 als Übersicht
├── references/                Detail-Inhalte (Progressive Disclosure)
│   ├── string-best-practices.md     Allgemeine Android-i18n-Patterns
│   ├── deutsche-sprache.md           Deutsche Orthographie, Typografie, UX
│   ├── escape-formen.md              Unicode-Escapes, HTML-Entities
│   ├── funktions-check.md            Phase 5 — Funktions-Regression-Checks
│   ├── praevention-setup.md          Phase 6 — Pre-Commit, CI, Lint, donottranslate
│   └── ast-scanning.md               Detekt-Rule fuer AST-basierten Compose-Scan
├── scripts/                   Ausführbare Werkzeuge
│   ├── scan-strings.sh               Phase-1-Scanner (9 Patterns + Unicode)
│   ├── validate.sh                   Cross-Platform-Wrapper (findet python3/python automatisch)
│   ├── validate_extracted.py         Post-Extraktion-Validator (Duplikate, Escapes, Format-Args)
│   └── pre-commit-hook.sh            Erweiterter Pre-Commit-Hook (Kotlin + XML)
└── evals/                     Regressions-Tests
    ├── eval-set.json                 6 Test-Szenarien
    └── README.md                     Anleitung wie Evals laufen
```

**Wann welche Referenz laden:**

- `deutsche-sprache.md` + `string-best-practices.md` — IMMER vor dem Start laden
- `escape-formen.md` — wenn Unicode-Escapes oder HTML-Entities vermutet werden
- `funktions-check.md` — vor Phase 5 oder bei eigenstaendigem "Funktions-Check"-Trigger
- `praevention-setup.md` — vor Phase 6 oder bei "Pre-Commit einrichten"-Trigger
- `ast-scanning.md` — wenn der Benutzer "Detekt einrichten" oder "AST-Scan" verlangt

---

## Die 6 Phasen im Ueberblick

```
PHASE 0: SPRACH-KONTEXT    Du/Sie, Gender, Region einmalig festlegen
    │
PHASE 1: SCAN              Alle hardcodierten Strings finden (Regex + Escapes)
    │
PHASE 2: AUDIT             Bestehende strings.xml pruefen + Glossar-Konsistenz
    │
PHASE 3: CREATE            Fehlende Strings nach DE-Regeln erstellen
    │
PHASE 4: VERIFY            Zweiter Durchlauf + Pseudolokalisierung
    │
PHASE 5: FUNKTIONS-CHECK   Strings duerfen nichts kaputt machen
    │                       (Details in references/funktions-check.md)
PHASE 6: PRAEVENTION       Pre-Commit + CI + Lint + donottranslate.xml
                            (Details in references/praevention-setup.md)
```

Jede Phase endet mit einem **Fortschritts-Report** an den Benutzer.

---

## Phase 0: SPRACH-KONTEXT

> Diese Entscheidungen MUESSEN einmalig fuer die gesamte App getroffen werden
> BEVOR der erste String erstellt wird. Ein String wie „Willkommen zurueck"
> kann fuer eine Banking-App komplett falsch sein (muesste Sie-Form heissen).

**0.1 Anrede (Du/Sie):**
- Consumer/Social/Spiele/Fitness/Streaming → **Du**
- Banking/Versicherung/Medizin/Behoerden/B2B → **Sie**
- Bei bestehender strings.xml: Autodetection via Grep nach `Sie\b` vs. `\bdu\b`.
  Bei Mix: ALARM. Details: `deutsche-sprache.md` Teil B.1.

**0.2 Gender-Strategie (Rangfolge, Details: `deutsche-sprache.md` B.2):**
1. Partizip-Substantive (`Nutzende`, `Lernende`) — EMPFOHLEN
2. Geschlechtsneutrale Substantive (`Person`, `Konto`) — EMPFOHLEN
3. Doppelpunkt-Notation (`Nutzer:innen`) — nur Fliesstext
4. Gender-Stern (`Nutzer*innen`) — nur wenn Zielgruppe es erwartet
5. Generisches Maskulinum — nur im Ausnahmefall

Default fuer neue Apps: Strategie 1+2.

**0.3 Region:** `values-de/` (DACH-Standard) als Basis. Nur bei expliziter
Zielregion `values-de-rAT/` (Jaenner) oder `values-de-rCH/` (kein ß, CHF) extra.

**0.4 Entscheidungen in strings.xml festschreiben:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Sprach-Konfiguration:
  - Anrede: Du (informell, modern)
  - Gender: Partizip-Substantive + geschlechtsneutrale Begriffe
  - Region: de (DACH-Standard)
-->
<resources xmlns:tools="http://schemas.android.com/tools"
           xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
           tools:locale="de">
```

**0.5 Phase-0-Report:** Anrede, Gender, Region kurz an den Benutzer melden.

---

## Phase 1: SCAN — Alle hardcodierten Strings finden

### 1.1 Projekt erkennen

```
Glob: **/app/src/main/java/**/*.kt
Glob: **/app/src/main/res/values/strings.xml
```

**Multi-Module-Apps:** Bei mehreren Modulen (`app/`, `feature-auth/` etc.) hat
**pro Modul eine eigene strings.xml**. Skill arbeitet pro Modul sequentiell.
Glob fuer Multi-Module: `**/src/main/res/values/strings.xml`.

### 1.2 Screen-Inventar

Alle UI-relevanten Kotlin-Dateien finden und als Checkliste aufbauen:

```
Glob: **/ui/**/*Screen.kt
Glob: **/ui/**/*Dialog.kt
Glob: **/ui/**/*Overlay.kt
Glob: **/ui/**/*Sheet.kt
Glob: **/ui/**/*ViewModel.kt
Glob: **/ui/**/*Component*.kt
```

### 1.3 Master-Scan via Script

```bash
# Aus dem Skill-Verzeichnis:
bash scripts/scan-strings.sh <APP_DIR>

# Oder mit vollem Pfad (falls anderswo gerufen):
bash ~/.claude/skills/string-extraktor/scripts/scan-strings.sh <APP_DIR>
```

Das Skript fasst die 9 Grep-Muster aus `string-best-practices.md` Kap. 2
zusammen (Compose Text, Templates, Toasts, Snackbars, ContentDescriptions,
Placeholders, TopAppBar, Error-Calls, Enum-Labels) und erzeugt einen
strukturierten Bericht. Spart Token bei grossen Apps. Bei kleinen Apps oder
gezielter Prüfung einzelner Screens reichen die Patterns einzeln aus
`string-best-practices.md` Kap. 2.

### 1.4 Unicode-Escape-Scan (PFLICHT)

> Vorfall 2026-04-16: `"Überblick"` als `"Überblick"` hardcoded. Die
> 9 Standard-Muster fanden es NICHT. Dieser Scan ist seitdem PFLICHT.

```
Grep: \\u00(dc|fc|d6|f6|c4|e4|df)
Path: app/src/main/java/
Glob: *.kt
```

Treffer wie normale Strings extrahieren. Beim Anzeigen den decodierten Text
zeigen: `Zeile 666: "Überblick" (= "Überblick")`.

Vollstaendige Details (HTML-Entities, XML-Entities, URL-Encoding):
`references/escape-formen.md`.

### 1.5 Versteckte String-Quellen (8 Kategorien — `string-best-practices.md` Kap. 4)

Regex-Muster finden NICHT zuverlaessig: ViewModel-Properties, Data-Class-Defaults,
Companion-Konstanten, When-Ausdruecke, Listen-Inits, Modifier.semantics,
buildAnnotatedString, Notification-Strings. Gezielt in den relevanten Dateien suchen.

### 1.6 Filtern: Was NICHT extrahieren

`Log.*`, `Timber.*`, `analytics.log`, `getString("key")` bei SharedPreferences,
`jsonObject.getString("field")`, `Regex(...)`, `const val TAG`, alles in `src/test/`
und `src/androidTest/`, `@Preview`-Composables — sind nie benutzersichtbar oder
technische Identifier.

### 1.7 Scan-Report

```
Strings:       N gesamt
Prioritaet 1:  X (Titel, Navigation, Buttons — immer sichtbar)
Prioritaet 2:  Y (Dialoge, Toasts, Fehler — Interaktion)
Prioritaet 3:  Z (Onboarding, Empty States — sekundaer)
Prioritaet 4:  W (Content Descriptions — A11y)
Top-Screens:   SettingsScreen 23, DashboardScreen 18, ...
```

---

## Phase 2: AUDIT — Bestehende Strings pruefen

**Nur bei bereits vorhandener `strings.xml`.** Bei neuer App: direkt zu Phase 3.

**2.1 Qualitaets-Checks (11 Pruefpunkte, Details: `string-best-practices.md` Kap. 11-22):**
Naming-Konvention, nummerierte Platzhalter (`%1$s`), XLIFF-Tags (`<xliff:g>`),
Uebersetzer-Kommentare, Plurals mit `other`, `translatable="false"` bei URLs,
String Arrays mit `@string/`, HTML mit CDATA, `tools:locale`, verwaiste Strings,
Duplikate.

**2.2 Automatische Validierung:**
```bash
# Cross-Platform (empfohlen — findet python3/python automatisch):
bash scripts/validate.sh app/src/main/res/values/strings.xml --code-dir app/src/main/java

# Oder direkt (Plattform-abhängig):
python3 scripts/validate_extracted.py app/src/main/res/values/strings.xml --code-dir app/src/main/java  # macOS/Linux
python scripts/validate_extracted.py app/src/main/res/values/strings.xml --code-dir app/src/main/java   # Windows
```
Prüft alle 11 Punkte plus Format-String-Mismatches. Mit `--strict` werden auch
Warnungen als Fehler gewertet (sinnvoll fuer CI). Mit `--suggest-donottranslate`
listet das Script Kandidaten fuer `donottranslate.xml` auf.

**2.3 Glossar-Konsistenz** (Term Base) — Haeufigkeitsscan der Substantive zeigt
Synonym-Verdacht (z.B. "Eintrag" 87x vs "Notiz" 12x oder "Stimmung" 45x vs "Mood" 3x).
Bei Inkonsistenz dem Benutzer melden, **er entscheidet**:

```bash
grep -oE '<string name="[^"]+">[^<]+</string>' values/strings.xml \
  | sed 's/<[^>]*>//g' | grep -oE '\b[A-ZÄÖÜ][a-zäöüß]+\b' \
  | sort | uniq -c | sort -rn | head -30
```

**2.4 Audit-Report:** Strings bestehend / korrekt / Issues automatisch gefixt /
Issues offen (brauchen Entscheidung).

---

## Phase 3: CREATE — Fehlende Strings erstellen

**3.1 Reihenfolge — Screen fuer Screen** (nicht alle auf einmal). Reihenfolge:
Haupt-Screens → Dialoge/Overlays → Onboarding → Edge-Cases.

### 3.2 Fuer jeden String: 6 Schritte

**1. Key nach Schema `<screen>_<element>_<beschreibung>` (snake_case):**
`settings_title_screen`, `journal_button_save`, `onboarding_label_email`,
`search_hint_query`, `journal_error_save_failed`, `delete_dialog_title` /
`delete_dialog_message`, `journal_toast_saved`, `mic_button_desc`,
`journal_empty_no_entries`, `all_button_ok` (app-weit).

**2. Platzhalter nummerieren** (auch bei nur einem):
```xml
<!-- FALSCH --> Hallo %s, du hast %d Eintraege
<!-- RICHTIG --> Hallo %1$s, du hast %2$d Eintraege
```

**3. XLIFF-Tags setzen** (`id` + `example` PFLICHT):
```xml
<string name="greeting">
    Hallo, <xliff:g id="user_name" example="Maria">%1$s</xliff:g>!
    Du hast <xliff:g id="entry_count" example="42">%2$d</xliff:g> Eintraege.
</string>
```

**4. Uebersetzer-Kommentar (Englisch)** — Pflicht bei Platzhaltern,
einwortigen/mehrdeutigen Strings, Strings mit Zeichenbegrenzung:
```xml
<!-- Shown in settings screen. Max 30 chars — fits in tab label. -->
<string name="settings_title_screen">Einstellungen</string>
```

**5. Im Code ersetzen:**
```kotlin
Text(stringResource(R.string.settings_title_screen))
Text(stringResource(R.string.greeting, userName, entryCount))
```
Fuer ViewModels: **Nie** `context.getString()` — stattdessen `@StringRes Int`
oder `UiText` sealed class. Fuer Compose 1.6+: `LocalResources.current.getString()`
in nicht-Composable-Lambdas.

**6. Deutsche Sprach-Validierung (PFLICHT)** — Top-5 (Vollliste:
`deutsche-sprache.md` Teil D):
1. Anrede konsistent mit Phase-0 (Du/Sie)
2. Umlaute direkt (kein Unicode-Escape, kein `ae/oe/ue`)
3. Typografische deutsche Anführungszeichen statt Schreibmaschinen-Quotes
4. Kein Punkt bei Buttons/Labels/Titeln
5. Kein Denglisch (`geshared`, `geliked`, `canceln`)

**Nach jedem Screen kurz validieren** (Mini-Check, Sekunden):
```bash
bash scripts/validate.sh app/src/main/res/values/strings.xml
```
Das fängt Duplikate, falsche Anführungszeichen und unnummerierte Platzhalter
sofort ab — bevor der Fehler in weiteren Screens kopiert wird.

### 3.3 Mengenangaben: Plurals statt if/else

```kotlin
// FALSCH:
val text = if (count == 1) "1 Eintrag" else "$count Eintraege"

// RICHTIG:
val text = pluralStringResource(R.plurals.journal_entries, count, count)
```

```xml
<plurals name="journal_entries">
    <item quantity="one">%d Eintrag</item>
    <item quantity="other">%d Eintraege</item>
</plurals>
```

`other` ist PFLICHT (sonst Crash). `count` wird ZWEIMAL uebergeben (Kategorie + Wert).

### 3.4 Fortschritt nach jedem Screen

```
Screen: SettingsScreen.kt
  Gefunden:  23 hardcodierte Strings
  Erstellt:  21 neue String-Ressourcen
  Plurals:   2
  Build:     OK
```

---

## Phase 4: VERIFY — Zweiter Durchlauf + Pseudolokalisierung

**4.1 Zweiter Scan:** Die 9 Grep-Muster aus Phase 1 PLUS den Unicode-Escape-Scan
aus Phase 1.4 nochmal anwenden. Keine Treffer mehr erwartet.

**4.2 Build-Check:** `./gradlew assembleDebug 2>&1 | tail -20`

**4.3 Automatischer Validator:**
```bash
bash scripts/validate.sh \
    app/src/main/res/values/strings.xml \
    --code-dir app/src/main/java --suggest-donottranslate
```
Prüft Duplikate, Escapes, Format-Args, donottranslate-Kandidaten. Mit `--strict`
werden Warnungen auch als Fehler gewertet (CI-Modus).

**4.4 Pseudolokalisierung (`en-XA`)** — deckt die letzten 5% auf, die Regex
nicht findet (dynamisch zusammengebaut, in Drittbibliotheken, in PDF-Vorlagen):

```kotlin
android { buildTypes { getByName("debug") { isPseudoLocalesEnabled = true } } }
```

**Wichtig:** `resConfigs` darf Pseudolocales NICHT herausfiltern. Debug-APK
installieren → Geraet auf "English (XA)" → JEDEN Screen durchklicken → jeder
unakzentuierte Text = vergessener String. `en-XA` Pflicht, `ar-XB` (RTL)
empfohlen fuer Nahost-Markt.

**4.5 Abschluss-Report:**
```
Phase 1 SCAN:    X Strings gefunden
Phase 2 AUDIT:   M bestehende, I gefixt
Phase 3 CREATE:  A neue Strings, B Plurals
Phase 4 VERIFY:  0 verbleibende, Build OK
Naechster Schritt: /uebersetzung [App-Name]
```

---

## Phase 5: FUNKTIONS-CHECK

> String-Extraktion kann Funktionalitaet zerstoeren wenn angezeigter Text auch
> als Identifikator, Vergleichswert oder DB-Key verwendet wird. Diese Phase
> findet solche Stellen BEVOR Bugs in Produktion landen.

**Vollstaendige Details:** `references/funktions-check.md`

Phase 5 prueft 5 Risiko-Klassen:

1. **Text-als-Identifikator** — `when(chip.text) { "Taeglich" -> ... }` bricht bei Sprachwechsel
2. **Enum-Serialisierung** — `displayName` in Room + UI = Datenverlust bei Lokalisierung
3. **Format-String-Sicherheit** — Argument-Mismatch fuehrt zu Runtime-Crashes
4. **Compose-Kontext** — `stringResource` in `remember{}` aktualisiert sich nicht
5. **Stiller Fallback** — fehlender Key in `values-en/` zeigt deutschen Text in englischer App

**Diese Phase ist auch eigenstaendig aufrufbar** mit Triggern wie "pruefe ob
Strings Funktionen kaputt machen", "Funktions-Check fuer extrahierte Strings",
"die App crasht auf Englisch". In dem Fall direkt `funktions-check.md` laden
und durcharbeiten.

---

## Phase 6: PRAEVENTION

> Eine perfekt extrahierte strings.xml ist nichts wert wenn beim naechsten
> Feature wieder hardcodierte Strings dazukommen. Diese Phase wird **einmalig
> pro Projekt** eingerichtet.

**Vollstaendige Details:** `references/praevention-setup.md`

Phase 6 richtet 6 Guards ein:

1. **Pre-Commit-Hook** — `scripts/pre-commit-hook.sh` mit allen 9 Patterns
2. **GitHub Action** — gleicher Scan im PR
3. **Lint-Verschaerfung** — `error("HardcodedText")` + `error("MissingTranslation")`
4. **String-Freeze vor Releases** — strings.xml-Aenderungen 1-2 Wochen vor Release blockieren
5. **donottranslate.xml** — App-Name, URLs, Marken trennen von uebersetzbaren Strings
6. **Optional: Tolgee/Lokalise/Crowdin** — Translation-Management-System

**Diese Phase ist auch eigenstaendig aufrufbar** mit Triggern wie
"Pre-Commit-Hook fuer i18n einrichten", "donottranslate.xml einrichten",
"verhindere neue hardcodierte Strings".

---

## donottranslate.xml — Pflicht-Trennung

Eigennamen, URLs, Marken duerfen NIE uebersetzt werden. Profi-Workflows
trennen sie in eine separate Datei (Crowdin/Phrase/Lokalise-Standard).

**Datei:** `res/values/donottranslate.xml`

```xml
<resources xmlns:tools="http://schemas.android.com/tools"
           tools:ignore="MissingTranslation">
    <string name="app_name" translatable="false">Mein Tagebuch</string>
    <string name="url_privacy" translatable="false">https://example.com/privacy</string>
    <string name="brand_name" translatable="false">MeinTagebuch</string>
    <string name="api_base_url" translatable="false">https://api.example.com/v1</string>
</resources>
```

**Auto-Detektion:** `validate_extracted.py --suggest-donottranslate` schlaegt
Kandidaten vor. Volle Details: `references/praevention-setup.md` Abschnitt 6.5.

---

## Wichtige Regeln

### Was NIEMALS gemacht werden darf

**Struktur/Technik (Vollliste in `string-best-practices.md` Kap. 11-22):**
- ❌ Bestehende Strings loeschen (nur neue hinzufuegen oder bestehende korrigieren)
- ❌ Log-Statements, Analytics-Events, `@Preview`-Composables extrahieren
- ❌ Alle Strings auf einmal bearbeiten (immer Screen fuer Screen)
- ❌ `context.getString()` in ViewModels (Lifecycle-Problem)
- ❌ Unnummerierte Platzhalter, `<plurals>` ohne `other`
- ❌ Gleichen String-Key fuer verschiedene Kontexte wiederverwenden
- ❌ Angezeigten Text als Vergleichswert/Key/Identifier (Phase 5.1)
- ❌ Enum-displayName fuer UI UND Datenspeicherung gleichzeitig (Phase 5.2)
- ❌ `stringResource()` in `remember{}` (aktualisiert sich nicht)
- ❌ Click-Handler beim Refactoring verlieren

**Deutsche Sprache (Vollliste in `deutsche-sprache.md` Teil D):**
- ❌ Du und Sie mischen, Sie/Ihnen/Ihr klein
- ❌ Veraltete Rechtschreibung: `daß`, `muß`, `Tip`
- ❌ Unicode-Escapes, englische Title Case, Schreibmaschinen-Quotes
- ❌ Denglisch, Imperativ bei Buttons, Punkt am Ende von Buttons/Titeln
- ❌ Genderzeichen `*`/`:`/`_` in Button-Labels (Screenreader-Stoerung)

### Effiziente Bearbeitung

- Dateien > 500 Zeilen: Grep → gezieltes Read (offset+limit) → Edit
- 3+ gleichartige Aenderungen pro Datei: Python-Batch-Script
- Build pruefen nach jedem Screen (nicht erst am Ende)
- Commit nach jedem abgeschlossenen Screen (Rettungspunkte)

---

## Zusammenspiel mit dem uebersetzung-Skill

Dieser Skill ist die VORSTUFE:

```
String-Extraktor (dieser Skill)     uebersetzung-Skill
─────────────────────────────       ─────────────────────
1. Hardcodierte Strings finden       1. strings.xml lesen
2. Bestehende Strings pruefen        2. Sprache fuer Sprache uebersetzen
3. Fehlende Strings erstellen        3. Verifizieren + Committen
4. Qualitaet sicherstellen
5. Funktionalitaet pruefen           Sprachtest (Englisch)
6. Praevention einrichten
         ↓                                   ↓
   strings.xml ist komplett    →     Uebersetzer kann starten
   + keine Funktions-Bugs              + alle Sprachen OK
```

**Pflicht-Reihenfolge:** Dieser Skill ZUERST, dann `uebersetzung`. Niemals
umgekehrt — sonst werden schlecht formulierte deutsche Strings in 26 Sprachen
zementiert.
