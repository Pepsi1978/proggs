---
name: string-extraktor
description: Findet ALLE hardcodierten Strings in Android-Apps (Compose und XML), prueft bestehende String-Ressourcen auf Uebersetzungskompatibilitaet und erstellt fehlende Strings nach internationalen Best Practices. Nutze diesen Skill IMMER wenn der Benutzer sagt "Strings finden", "Strings erstellen", "String-Extraktion", "hardcodierte Strings", "hardcoded Strings suchen", "i18n Audit", "i18n pruefen", "Strings ueberpruefen", "String-Qualitaet", "fehlende Strings", "Strings fehlen noch", "alle Strings extrahieren", "Internationalisierung vorbereiten", "strings.xml auffuellen", "String Extractor", "String Extraktor", "sind alle Strings erstellt?", "Strings komplett?", "i18n fertig?", "alle Texte externalisiert?", "noch nicht uebersetzte Strings". Auch wenn der Benutzer nach einem Feature fragt "pruefe ob alle Strings da sind" oder "bevor wir uebersetzen — sind die Strings vollstaendig?". Dieser Skill ist die VORSTUFE zum Uebersetzungs-Skill: erst String-Extraktor (Strings finden + erstellen), DANN Uebersetzung (Strings in andere Sprachen). Funktioniert fuer JEDES Android-Projekt, nicht nur ein bestimmtes.
---

# String-Extraktor: Hardcodierte Strings finden, pruefen und erstellen

Dieser Skill arbeitet ein Android-Projekt systematisch ab um sicherzustellen, dass
**100% aller benutzersichtbaren Strings** als String-Ressourcen in `strings.xml`
vorliegen — korrekt benannt, mit Platzhaltern, Kommentaren und Plural-Unterstuetzung,
bereit fuer die Uebersetzung durch den separaten Uebersetzungs-Skill.

**Abgrenzung:** Dieser Skill ERSTELLT Strings in der Default-Sprache (Deutsch).
Er uebersetzt NICHT in andere Sprachen — dafuer gibt es den `uebersetzung`-Skill.

---

## Referenzdatei laden

**Vor dem Start MUSS die Referenzdatei gelesen werden:**

```
Read: ~/proggs/string-best-practices.md
```

Diese Datei enthaelt 25 Kapitel mit allen Grep-Mustern, Naming-Konventionen,
Erstellungsregeln und Qualitaets-Checklisten. Die Kapitel werden im Folgenden
per Nummer referenziert.

Falls die Datei nicht existiert, liegt eine Kopie unter:
`references/string-best-practices.md` (gebundelt mit diesem Skill).

---

## Die 4 Phasen im Ueberblick

```
PHASE 1: SCAN ──────── Alle hardcodierten Strings finden (Referenz: Kap. 1-10)
    │
PHASE 2: AUDIT ─────── Bestehende strings.xml pruefen (Referenz: Kap. 11-22)
    │
PHASE 3: CREATE ────── Fehlende Strings erstellen (Referenz: Kap. 11-22)
    │
PHASE 4: VERIFY ────── Zweiter Durchlauf + Qualitaet (Referenz: Kap. 23-25)
    │
PHASE 5: FUNKTIONS-CHECK ── Strings duerfen nichts kaputt machen (NEU)
```

Jede Phase endet mit einem **Fortschritts-Report** an den Benutzer.
Der Benutzer soll nach jeder Phase sehen was gefunden/erstellt/geprueft wurde.

---

## Phase 1: SCAN — Alle hardcodierten Strings finden

### 1.1 Projekt erkennen

Das Android-Projektverzeichnis automatisch finden:

```
Glob: **/app/src/main/java/**/*.kt
Glob: **/app/src/main/res/values/strings.xml
```

Den Benutzer fragen welches Projekt gemeint ist, wenn mehrere gefunden werden.
Das `app/src/main/`-Verzeichnis ist die Basis fuer alle weiteren Operationen.

### 1.2 Screen-Inventar erstellen

Alle UI-relevanten Kotlin-Dateien finden und als Checkliste aufbauen:

```
Glob: **/ui/**/*Screen.kt
Glob: **/ui/**/*Dialog.kt
Glob: **/ui/**/*Overlay.kt
Glob: **/ui/**/*Sheet.kt
Glob: **/ui/**/*ViewModel.kt
Glob: **/ui/**/*Component*.kt
```

Dem Benutzer das Inventar zeigen:

```
Screen-Inventar [App-Name]:
[ ] HomeScreen.kt              + HomeViewModel.kt
[ ] SettingsScreen.kt          + SettingsViewModel.kt
[ ] DetailScreen.kt            + DetailViewModel.kt
...
Gefunden: N Screens, M ViewModels, K Dialoge/Overlays
```

### 1.3 Die 9 Grep-Muster anwenden

Fuer JEDE .kt-Datei unter `app/src/main/java/` diese 9 Muster pruefen
(Details: Referenzdatei Kapitel 2):

| # | Pattern | Findet |
|---|---------|--------|
| 1 | `Text(\s*"[^"]*[a-zA-ZäöüÄÖÜß]` | Compose Text() mit Literal |
| 2 | `Text(\s*"[^"]*\$` und `"[^"]*" \+` | String-Templates und Konkatenation |
| 3 | `Toast\.makeText\([^,]+,\s*"` | Toast-Nachrichten |
| 4 | `showSnackbar\(\s*"` | Snackbar-Nachrichten |
| 5 | `contentDescription\s*=\s*"` | Accessibility-Texte |
| 6 | `placeholder\s*=.*Text(\s*"` | Eingabefeld-Platzhalter |
| 7 | `TopAppBar.*title\s*=.*Text(\s*"` | Navigation/AppBar |
| 8 | `error\(\s*"[^"]*[a-zA-Z]` | Benutzersichtbare Fehler |
| 9 | `enum class.*"[^"]*[a-zA-Z]` | Enum-Labels |

**Grep-Tool verwenden, NICHT Bash grep.** Ergebnisse sammeln, nicht sofort fixen.

### 1.3b PFLICHT — Unicode-Escape-Scan fuer deutsche Umlaute

> **Hintergrund-Vorfall 2026-04-16:** Das Wort `"Überblick"` war als
> `"\u00dcberblick"` (Unicode-Escape) in DashboardScreen.kt hardcoded. Die
> 9 Standard-Muster in 1.3 haben das **NICHT** gefunden, weil sie nach
> tatsaechlichen Umlauten wie `Ü` suchen — die Escape-Form `\u00dc` ist aber
> ein normaler ASCII-Text und rutscht durch.
>
> Als Konsequenz muss **JEDER Durchlauf** zusaetzlich diese Muster pruefen.
> Dies ist **keine Option**, sondern **PFLICHT** — auch bei "nur schnell
> checken"-Aufgaben. Die Suche dauert <5 Sekunden.

**Die 8 Pflicht-Muster fuer deutsche Umlaut-Escapes:**

| # | Pattern | Zeichen | Beispiel |
|---|---------|---------|----------|
| U1 | `\\u00dc` | Ü | `"\u00dcberblick"` = "Überblick" |
| U2 | `\\u00fc` | ü | `"M\u00fcll"` = "Müll" |
| U3 | `\\u00d6` | Ö | `"\u00d6ffentlich"` = "Öffentlich" |
| U4 | `\\u00f6` | ö | `"h\u00f6ren"` = "hören" |
| U5 | `\\u00c4` | Ä | `"\u00c4nderung"` = "Änderung" |
| U6 | `\\u00e4` | ä | `"w\u00e4hlen"` = "wählen" |
| U7 | `\\u00df` | ß | `"gr\u00fc\u00dfen"` = "grüßen" |
| U8 | `\\u[0-9a-fA-F]{4}` | JEDE Escape | Breites Netz — alle \uXXXX finden |

**Grep-Aufruf (mit dem Grep-Tool, Pattern als regex):**
```
Pattern: \\u00(dc|fc|d6|f6|c4|e4|df)
Path:    app/src/main/java/
Glob:    *.kt
```

Treffer werden wie normale hardcodierte Strings behandelt — als String-Ressource
extrahieren.

**Ausnahmen (technisch, nicht UI):**
- `Regex("\\u...")`, `"\\uFFFD"` als Unicode-Konstante in Parser-Code
- Kommentare wie `// \u00dc = U-Umlaut` (informativ)
- Test-Dateien

**Naming-Hinweis:** Wenn ein Treffer bei Muster U1-U7 entdeckt wird,
zeige dem Benutzer den DECODIERTEN Text in der Meldung:
```
Zeile 666: "\u00dcberblick" (= "Überblick")
```
Damit der Benutzer sofort versteht was hinter dem Escape steckt.

### 1.3c Weitere Escape-Formen (seltener, aber vollstaendigkeitshalber)

Diese kommen selten vor, aber auch pruefen:

| Form | Beispiel | Wo zu erwarten |
|------|----------|---------------|
| HTML-Entities | `&Uuml;`, `&uuml;`, `&szlig;` | In AnnotatedString mit HTML-Parsing |
| XML-Entities in strings.xml | `&#220;`, `&#252;` | Selten direkt in Code |
| JavaScript-Escape | `\x{00dc}` | Nur wenn JS-Code eingebettet |

Grep-Pattern fuer HTML-Entities (nur in .kt + .xml die HTML verarbeiten):
```
Pattern: &(Uuml|uuml|Ouml|ouml|Auml|auml|szlig);
```

### 1.4 Versteckte String-Quellen pruefen

Diese 8 Kategorien werden von den Grep-Mustern NICHT zuverlaessig gefunden
(Details: Referenzdatei Kapitel 4 und 9):

1. **ViewModel-Properties**: `val text = "..."` die in der UI angezeigt werden
2. **Data-Class-Defaults**: `data class X(val label: String = "...")`
3. **Companion-Object-Konstanten**: `const val TITLE = "..."` wenn UI-sichtbar
4. **When-Ausdruecke**: `Mood.HAPPY -> "Gut gelaunt"` mit String-Rueckgabe
5. **Listen-Initialisierungen**: `listOf("Tipp 1", "Tipp 2")`
6. **Modifier.semantics**: `contentDescription = "..."` und `stateDescription = "..."`
7. **buildAnnotatedString**: `append("Text")` innerhalb von AnnotatedString
8. **Notification-Strings**: `setContentTitle("...")`, `setContentText("...")`

Fuer jede Kategorie gezielt in den relevanten Dateien suchen.

### 1.5 Ergebnisse filtern

Treffer die NICHT extrahiert werden duerfen (Details: Referenzdatei Kapitel 4):

| AUSSCHLIESSEN | Warum |
|--------------|-------|
| `Log.d/e/w/i(...)` | Nie benutzersichtbar |
| `Timber.d/e(...)` | Nie benutzersichtbar |
| `analytics.log(...)` | Technische Bezeichner |
| `getString("key")` bei SharedPreferences | Interne Keys |
| `jsonObject.getString("field")` | Daten-Keys |
| `Regex("pattern")` | Technisch |
| `const val TAG = "..."` | Nur fuer Logging |
| Alles in `src/test/` oder `src/androidTest/` | Nicht in Produktion |
| `@Preview`-Composables | Nur fuer IDE-Vorschau |

### 1.6 Ergebnisse priorisieren und dem Benutzer zeigen

```
SCAN-ERGEBNIS:
═══════════════════════════════════════
Gefundene hardcodierte Strings: N

Prioritaet 1 (Immer sichtbar):     X Strings
  - Screen-Titel, Navigation, Haupt-Buttons
Prioritaet 2 (Interaktion):        Y Strings
  - Dialoge, Toasts, Snackbars, Fehlermeldungen
Prioritaet 3 (Sekundaer):          Z Strings
  - Onboarding, Empty States, Beschreibungen
Prioritaet 4 (Accessibility):      W Strings
  - Content Descriptions, State Descriptions

Screens mit den meisten Treffern:
  1. SettingsScreen.kt — 23 Strings
  2. DashboardScreen.kt — 18 Strings
  3. ...
═══════════════════════════════════════
```

---

## Phase 2: AUDIT — Bestehende Strings pruefen

**Diese Phase laeuft NUR wenn `strings.xml` bereits Eintraege hat.**
Bei einer komplett neuen App ohne Strings: direkt zu Phase 3 springen.

### 2.1 Bestehende strings.xml lesen

```
Read: app/src/main/res/values/strings.xml
```

Anzahl vorhandener Strings zaehlen und dem Benutzer zeigen.

### 2.2 Qualitaets-Checks (11 Pruefpunkte)

Jeden bestehenden String gegen diese Regeln pruefen
(Details: Referenzdatei Kapitel 11-22):

| # | Check | Referenz-Kapitel | Was gesucht wird |
|---|-------|-----------------|-----------------|
| 1 | Naming-Konvention | Kap. 12 | `screen_element_beschreibung` Schema eingehalten? |
| 2 | Nummerierte Platzhalter | Kap. 13 | `%1$s` statt `%s`? |
| 3 | XLIFF-Tags | Kap. 14 | `<xliff:g id="..." example="...">` bei Platzhaltern? |
| 4 | Uebersetzer-Kommentare | Kap. 16 | Kommentar bei mehrdeutigen Strings, Platzhaltern, kurzen Strings? |
| 5 | Plurals korrekt | Kap. 15 | `other`-Kategorie vorhanden? `getQuantityString(count, count)`? |
| 6 | translatable="false" | Kap. 17 | URLs, Brand-Namen, Keys markiert? |
| 7 | String Arrays | Kap. 18 | `@string/`-Referenzen statt direktem Text? |
| 8 | HTML korrekt | Kap. 19 | CDATA bei HTML + Platzhaltern? |
| 9 | tools:locale | Kap. 23 | Im `<resources>`-Tag gesetzt? |
| 10 | Verwaiste Strings | — | In strings.xml definiert aber im Code nicht referenziert? |
| 11 | Duplikate | — | Gleicher Text unter verschiedenen Keys? |

### 2.3 Audit-Report erstellen

```
AUDIT-ERGEBNIS:
═══════════════════════════════════════
Bestehende Strings: N
Davon korrekt:     M (X%)
Issues gefunden:    K

Naming-Fehler:              A Strings (falsches Schema)
Fehlende Nummerierung:      B Strings (%s statt %1$s)
Fehlende XLIFF-Tags:        C Strings (Platzhalter ungeschuetzt)
Fehlende Kommentare:        D Strings (mehrdeutig oder Platzhalter)
Plural-Probleme:            E Strings (fehlendes "other" oder falscher Aufruf)
Fehlende translatable:      F Strings (URLs/Keys nicht markiert)
Verwaiste Strings:          G Strings (nicht mehr referenziert)
Duplikate:                  H Strings (gleicher Text, verschiedene Keys)
═══════════════════════════════════════
```

### 2.4 Issues automatisch korrigieren

Die meisten Audit-Issues koennen automatisch gefixt werden:

- **Fehlende Nummerierung**: `%s` → `%1$s` (Edit-Tool)
- **Fehlende Kommentare**: Kommentar ueber dem String einfuegen (Edit-Tool)
- **Fehlende tools:locale**: Im `<resources>`-Tag ergaenzen (Edit-Tool)
- **Fehlende translatable="false"**: Attribut hinzufuegen (Edit-Tool)

**Bei 3+ gleichartigen Fixes: Python-Batch-Script verwenden** statt manueller Edits.

Was NICHT automatisch gefixt wird (braucht Kontext-Entscheidung):
- Naming-Fehler (umbenennnen wuerde R.string-Referenzen brechen)
- Verwaiste Strings (vielleicht dynamisch referenziert?)
- Duplikate (welcher Key soll bleiben?)

Diese dem Benutzer als Liste zeigen und um Entscheidung bitten.

---

## Phase 3: CREATE — Fehlende Strings erstellen

### 3.1 Reihenfolge: Screen fuer Screen

Die Strings aus Phase 1 werden **Screen fuer Screen** abgearbeitet, nicht alle auf einmal.
Das verhindert Fehler und macht den Fortschritt sichtbar.

**Reihenfolge:**
1. Haupt-Screens (meiste Strings, groesster Impact)
2. Dialoge und Overlays
3. Onboarding
4. Edge-Case-Screens (Empty States, Error States)

### 3.2 Fuer jeden hardcodierten String: 5 Schritte

**Schritt 1: String-Key nach Naming-Konvention erstellen**

Schema: `<screen>_<element>_<beschreibung>`

| Praefix | Element-Typ | Beispiel |
|---------|------------|---------|
| Screen-Name | `_title` | `settings_title_screen` |
| Screen-Name | `_button` | `journal_button_save` |
| Screen-Name | `_label` | `onboarding_label_email` |
| Screen-Name | `_hint` | `search_hint_query` |
| Screen-Name | `_error` | `journal_error_save_failed` |
| Screen-Name | `_dialog_title` | `delete_dialog_title` |
| Screen-Name | `_dialog_message` | `delete_dialog_message` |
| Screen-Name | `_toast` | `journal_toast_saved` |
| Screen-Name | `_desc` | `mic_button_desc` |
| Screen-Name | `_empty` | `journal_empty_no_entries` |
| `all_` | beliebig | `all_button_ok` (app-weit) |

**Schritt 2: Platzhalter nummerieren**

```xml
<!-- FALSCH -->
<string name="greeting">Hallo %s, du hast %d Eintraege</string>

<!-- RICHTIG -->
<string name="greeting">Hallo %1$s, du hast %2$d Eintraege</string>
```

Auch bei nur EINEM Platzhalter nummerieren (Konsistenz + Zukunftssicherheit).

**Schritt 3: XLIFF-Tags setzen (bei Platzhaltern)**

```xml
<string name="greeting">
    Hallo, <xliff:g id="user_name" example="Maria">%1$s</xliff:g>!
    Du hast <xliff:g id="entry_count" example="42">%2$d</xliff:g> Eintraege.
</string>
```

Beide Attribute sind PFLICHT:
- `id`: Beschreibt WAS der Platzhalter ist (nicht "x" oder "var")
- `example`: Zeigt einen konkreten Beispielwert

**Schritt 4: Uebersetzer-Kommentar schreiben**

Kommentar UNMITTELBAR VOR dem String-Element. Pflicht bei:
- Jedem String mit Platzhalter
- Jedem einwortigen oder mehrdeutigen String
- Jedem String mit Zeichenbegrenzung (Buttons, Tabs)

```xml
<!-- Shown in the settings screen. Max 30 chars — fits in tab label. -->
<string name="settings_title_screen">Einstellungen</string>

<!-- Error when save fails. %1$s = error message from server (e.g., "Timeout"). -->
<string name="journal_error_save_failed">
    Speichern fehlgeschlagen: <xliff:g id="error_reason" example="Timeout">%1$s</xliff:g>
</string>
```

Sprache der Kommentare: **Englisch** (internationaler Standard fuer Uebersetzer-Tools).

**Schritt 5: Im Code ersetzen**

```kotlin
// VORHER:
Text("Einstellungen")

// NACHHER (Compose):
Text(stringResource(R.string.settings_title_screen))

// NACHHER (mit Platzhaltern):
Text(stringResource(R.string.greeting, userName, entryCount))
```

Fuer ViewModels: **Nie** `context.getString()` verwenden. Stattdessen:

```kotlin
// Option A: @StringRes Int
@StringRes val titleRes: Int = R.string.settings_title_screen

// Option B: UiText sealed class (bei dynamischen Strings)
sealed class UiText {
    data class StringResource(val resId: Int, val args: List<Any> = emptyList()) : UiText()
    data class DynamicString(val value: String) : UiText()
}
```

### 3.3 Mengenangaben: Plurals statt if/else

Wenn ein String eine Zahl enthaelt die sich auf eine Menge bezieht:

```kotlin
// VORHER (FALSCH):
val text = if (count == 1) "1 Eintrag" else "$count Eintraege"

// NACHHER:
val text = pluralStringResource(R.plurals.journal_entries, count, count)
```

```xml
<!-- In strings.xml: -->
<plurals name="journal_entries">
    <item quantity="one">%d Eintrag</item>
    <item quantity="other">%d Eintr\u00e4ge</item>
</plurals>
```

`other` ist PFLICHT — ohne `other` crasht die App.
`count` wird ZWEIMAL uebergeben: einmal fuer Kategorie-Auswahl, einmal fuer `%d`.

### 3.4 Bei grossen Dateien: Effiziente Bearbeitung

- **Dateien ueber 500 Zeilen**: Grep + gezieltes Read (offset+limit) + Edit. KEINE Agents.
- **3+ gleichartige Aenderungen**: Python-Batch-Script statt manuelle Edits.
- **Screen fuer Screen**: Nie mehr als eine grosse Datei gleichzeitig bearbeiten.

### 3.5 Fortschritt nach jedem Screen zeigen

```
Screen: SettingsScreen.kt
  Gefunden:  23 hardcodierte Strings
  Erstellt:  21 neue String-Ressourcen
  Plurals:   2 (Eintraege-Zaehler, Tage-Zaehler)
  Uebersprungen: 0
  Build: OK ✓
```

---

## Phase 4: VERIFY — Zweiter Durchlauf + Qualitaetssicherung

### 4.1 Zweiter Scan-Durchlauf (PFLICHT)

Die gleichen 9 Grep-Muster aus Phase 1 nochmal auf alle .kt-Dateien anwenden.
Diesmal sollten KEINE Treffer mehr erscheinen. Wenn doch:

- Strings die beim ersten Durchlauf uebersehen wurden → jetzt fixen
- Neue Strings die durch Phase 3 entstanden sind (selten, aber moeglich)

### 4.2 Pruefen ob Phase 3 Probleme eingefuehrt hat

- Fehlende Imports? (`import androidx.compose.ui.res.stringResource`)
- Falsche R-Klasse importiert? (muss die des eigenen Moduls sein)
- Platzhalter-Anzahl stimmt? (String hat %1$s und %2$d, Aufruf hat 2 Argumente?)
- Compose-Funktionen die jetzt `@Composable` sein muessen?

### 4.3 Build-Check

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Wenn der Build fehlschlaegt: Fehler analysieren und fixen bevor weitergegangen wird.

### 4.4 Abschluss-Checkliste (14 Punkte)

Alle Punkte muessen mit JA beantwortet werden (Details: Referenzdatei Kapitel 24):

**Nach dem Finden:**
- [ ] Grep-Muster 1-9 auf alle .kt-Dateien angewendet?
- [ ] Alle Screens im Inventar abgehakt?
- [ ] ViewModels auf benutzersichtbare Strings geprueft?
- [ ] Content Descriptions fuer alle Icons/Bilder vorhanden?

**Nach dem Erstellen:**
- [ ] Naming-Konvention `screen_element_beschreibung` konsistent?
- [ ] Alle Platzhalter nummeriert (`%1$s`, nicht `%s`)?
- [ ] `xliff:g` fuer alle Platzhalter mit `id` + `example`?
- [ ] Uebersetzer-Kommentare fuer mehrdeutige Strings?
- [ ] `translatable="false"` fuer URLs, Brand-Namen, Keys?
- [ ] Alle `<plurals>` haben `other`-Kategorie?
- [ ] String Arrays nutzen `@string/`-Referenzen?
- [ ] `tools:locale="de"` im `<resources>`-Tag gesetzt?
- [ ] XLIFF-Namespace im `<resources>`-Tag deklariert?
- [ ] Build kompiliert ohne Fehler?

### 4.5 Ergebnis-Report (PFLICHT — dem Benutzer zeigen)

```
STRING-EXTRAKTION ABGESCHLOSSEN
═══════════════════════════════════════════════════

Phase 1 — SCAN:
  Screens analysiert:           N
  Hardcodierte Strings gefunden: X

Phase 2 — AUDIT (bestehende Strings):
  Vorhandene Strings:           M
  Davon korrekt:                K (Y%)
  Issues gefixt:                I
  Issues offen (brauchen Entscheidung): J

Phase 3 — CREATE (neue Strings):
  Neue String-Ressourcen:       A
  Neue Plurals:                 B
  Code-Ersetzungen:             C

Phase 4 — VERIFY (zweiter Durchlauf):
  Verbleibende hardcodierte Strings: 0 ✓
  Build-Status: OK ✓
  Checkliste: 14/14 ✓

Phase 5 — FUNKTIONS-CHECK:
  Text-als-Identifikator:       0 kritische Stellen ✓
  Enum-Serialisierung:          0 Risiken ✓
  Format-String-Sicherheit:     0 Mismatches ✓
  Compose-Kontext:              0 Probleme ✓
  Stille Fallbacks:             0 fehlende Keys ✓

Naechster Schritt: Uebersetzungs-Skill starten
  → /uebersetzung [App-Name]
═══════════════════════════════════════════════════
```

---

## Phase 5: FUNKTIONALITAETS-CHECK — Strings duerfen nichts kaputt machen

> **Warum diese Phase existiert:** String-Extraktion kann Funktionalitaet zerstoeren,
> wenn angezeigter Text im Code auch als Identifikator, Vergleichswert oder DB-Key
> verwendet wird. Diese Phase findet solche Stellen BEVOR die App auf einer anderen
> Sprache getestet wird — und BEVOR Bugs in Produktion landen.
>
> **Wann diese Phase laufen soll:**
> - Nach Phase 4 (VERIFY) als letzter Schritt bei der Erstextraktion
> - Auch EIGENSTAENDIG aufrufbar: "pruefe ob Strings Funktionen kaputt machen"
> - Empfohlen: NACH dem Sprachtest (z.B. App auf Englisch getestet, Bugs gefunden)

### 5.1 Text-als-Identifikator finden (KRITISCH — haeufigstes Problem)

Hardcodierter Text der gleichzeitig als Vergleichswert, Map-Key oder Routing-Ziel
verwendet wird, bricht in JEDER Sprache ausser der Default-Sprache.

**Grep-Muster zum Finden:**

```
# When/If-Vergleiche mit angezeigtem Text
Grep: == "[A-ZÄÖÜ]
Grep: when\s*\(.*\)\s*\{[^}]*"[A-ZÄÖÜ]

# HashMap/Map mit Text-Keys
Grep: mapOf\(.*"[A-ZÄÖÜa-zäöüß]+.*to\s

# SharedPreferences/Room mit angezeigtem Text als Wert
Grep: putString\(.*,\s*"[A-ZÄÖÜ]
Grep: == getString\(
```

**Typische Beispiele und Fixes:**

| Anti-Pattern (KAPUTT bei Sprachwechsel) | Fix |
|----------------------------------------|-----|
| `when(chip.text) { "Täglich" -> DAILY }` | `when(chip.tag) { ScheduleType.DAILY -> ... }` |
| `if (button.text == "Speichern") onSave()` | `onClick = { onSave() }` direkt am Button |
| `mapOf("Speichern" to ::onSave)` | `mapOf(Action.SAVE to ::onSave)` mit Enum |
| `prefs.putString("mood", moodLabel)` | `prefs.putString("mood", mood.name)` (Enum-Name) |
| `navController.navigate(screenTitle)` | `navController.navigate(Route.SETTINGS)` |

**Faustregel:** Wenn ein String sowohl dem Benutzer ANGEZEIGT als auch im Code
VERGLICHEN wird, muss der Vergleich auf einen sprachunabhaengigen Identifier
umgestellt werden (Enum, sealed class, Int-ID, String-Konstante).

### 5.2 Enum-Serialisierungs-Check (DATEN-VERLUST-RISIKO)

Enums deren `displayName` sowohl fuer die UI-Anzeige als auch fuer die
Datenspeicherung (Room, SharedPreferences, JSON) verwendet wird, verursachen
**Datenverlust** bei Sprachwechsel.

**Grep-Muster:**

```
# Enums mit displayName/label Property
Grep: enum class.*val\s+(displayName|label|title|text)\s*[:=]

# Enum-Werte die in Room/SharedPreferences gespeichert werden
Grep: \.displayName.*putString\|putString.*\.displayName
Grep: \.label.*putString\|putString.*\.label
```

**Beispiel-Bug:**

```kotlin
// KAPUTT nach Extraktion:
enum class Mood(val displayName: String) {
    HAPPY("Glücklich"), SAD("Traurig")  // In Room als "Glücklich" gespeichert
}
// Nach Extraktion: displayName = getString(R.string.mood_happy) → "Happy" in English
// Alte DB-Eintraege: "Glücklich" → Room findet keinen Match → NULL/Crash

// FIX: Anzeige und Speicherung TRENNEN
enum class Mood(@StringRes val displayNameRes: Int) {
    HAPPY(R.string.mood_happy),
    SAD(R.string.mood_sad);
    // Room speichert enum.name ("HAPPY") — sprachunabhaengig
    // UI zeigt stringResource(mood.displayNameRes) — lokalisiert
}
```

**Pflicht-Pruefung:** Bei JEDEM Enum mit String-Property pruefen:
1. Wird die Property in Room/SharedPreferences/JSON gespeichert? → `name` statt `displayName` verwenden
2. Wird die Property fuer Vergleiche benutzt? → Auf Enum-Typ vergleichen

### 5.3 Format-String-Sicherheit (CRASH-PRAEVENTION)

Falsche Platzhalter fuehren zu Runtime-Crashes die NUR bei bestimmten Sprachen auftreten.

**Was pruefen:**

| Pruefung | Wie | Crash-Typ |
|----------|-----|-----------|
| **Argument-Anzahl** | Zaehle `%` in String vs. Argumente in `stringResource()`/`getString()` | `MissingFormatArgumentException` |
| **Argument-Typ** | `%d` erwartet Int, `%s` erwartet String — stimmt der Code? | `IllegalFormatConversionException` |
| **Argument-Reihenfolge** | `%1$s` = erster Arg, `%2$d` = zweiter Arg — stimmt die Reihenfolge? | Falscher Text (kein Crash) |
| **Doppelte Nummerierung** | `%1$s ... %1$s` ist OK (gleiches Arg 2x), aber `%1$s %3$s` ohne `%2$s` ist verdaechtig | `MissingFormatArgumentException` |

**Grep-Muster zum Finden:**

```
# Strings mit Platzhaltern in strings.xml
Grep: %[0-9]*\$?[sdfu]    (in strings.xml)

# Aufrufe mit stringResource/getString (im Code)
Grep: stringResource\(R\.string\.\w+,
Grep: getString\(R\.string\.\w+,
```

**Automatisierter Abgleich (Python-Script):**

```python
# Fuer jede stringResource()-Stelle im Code:
# 1. String-Key extrahieren (R.string.xyz)
# 2. In strings.xml die Anzahl Platzhalter zaehlen
# 3. Im Code die Anzahl uebergebener Argumente zaehlen
# 4. Wenn ungleich: ALARM
```

### 5.4 Compose-Kontext-Fehler (COMPILE- UND RUNTIME-BUGS)

**Was pruefen:**

| Problem | Grep-Muster | Fix |
|---------|-------------|-----|
| `stringResource` in `remember {}` | `Grep: remember\s*\{[^}]*stringResource` | `stringResource` aus `remember` rausziehen — sonst aktualisiert sich der Text nicht bei Sprachwechsel |
| `getString()` im ViewModel | `Grep: context\.getString\|context\.resources` | `@StringRes Int` oder `UiText` sealed class verwenden |
| `stringResource` in nicht-Composable-Lambda | `Grep: \.let\s*\{[^}]*stringResource\|\.map\s*\{[^}]*stringResource` | In der Composable-Funktion aufloesen, nicht in der Lambda |
| Verlorener Click-Handler nach Refactoring | Manuell pruefen: Hat jeder Button noch seinen `onClick`? | Vor/Nachher-Vergleich des Composable-Baums |

**Haeufigster Compose-Bug nach String-Extraktion:**

```kotlin
// VORHER (funktioniert):
Button(onClick = { onSave() }) {
    Text("Speichern")
}

// KAPUTT (onClick verloren beim Refactoring):
Text(stringResource(R.string.save_button))  // Button und onClick sind weg!

// RICHTIG:
Button(onClick = { onSave() }) {
    Text(stringResource(R.string.save_button))
}
```

### 5.5 Stiller Fallback-Check (MEISTENS UEBERSEHEN)

Wenn ein String-Key in `values/strings.xml` existiert aber NICHT in
`values-en/strings.xml`, zeigt Android den deutschen Text in der englischen App.
**Kein Crash, kein Warning, keine Fehlermeldung** — der Benutzer sieht einfach
deutschen Text mitten in der englischen App.

**Wie pruefen (nach dem Uebersetzungs-Skill):**

```bash
# Alle Keys aus der Default-Datei extrahieren
grep 'name="' values/strings.xml | sed 's/.*name="\([^"]*\)".*/\1/' | sort > /tmp/default-keys.txt

# Alle Keys aus der Ziel-Sprache extrahieren
grep 'name="' values-en/strings.xml | sed 's/.*name="\([^"]*\)".*/\1/' | sort > /tmp/en-keys.txt

# Fehlende Keys finden
comm -23 /tmp/default-keys.txt /tmp/en-keys.txt
```

**Oder per Android Lint:**
```bash
./gradlew lint 2>&1 | grep "MissingTranslation"
```

**Wann dieser Check laufen soll:**
- NACH dem Uebersetzungs-Skill (sind alle Sprachen komplett?)
- NACH dem Hinzufuegen neuer Strings (vergessen in andere Sprachen zu uebersetzen?)
- ALS Teil des Sprachtests (deutscher Text in englischer App gesehen?)

### 5.6 Funktionalitaets-Report (PFLICHT — dem Benutzer zeigen)

```
FUNKTIONALITAETS-CHECK
═══════════════════════════════════════════════════

5.1 Text-als-Identifikator:
  Gefundene Stellen:          N
  Davon kritisch:             X (Vergleiche die bei Sprachwechsel brechen)
  Gefixt:                     Y
  Offen (brauchen Entscheidung): Z

5.2 Enum-Serialisierung:
  Enums mit displayName:      A
  Davon in Room/Prefs:        B (DATEN-VERLUST-RISIKO!)
  Gefixt:                     C

5.3 Format-Strings:
  Strings mit Platzhaltern:   D
  Argument-Mismatch:          E
  Typ-Mismatch:               F

5.4 Compose-Kontext:
  stringResource in remember: G
  getString in ViewModel:     H
  Verlorene Click-Handler:    I

5.5 Stille Fallbacks:
  Fehlende Keys in values-en: J
  Fehlende Keys in anderen:   K
═══════════════════════════════════════════════════
```

---

## Wichtige Regeln (IMMER beachten)

### Was NIEMALS gemacht werden darf

- ❌ Bestehende Strings loeschen (nur neue hinzufuegen oder bestehende korrigieren)
- ❌ Log-Statements, Analytics-Events oder technische Keys extrahieren
- ❌ Strings in `@Preview`-Composables extrahieren (nur IDE-Vorschau)
- ❌ Alle Strings auf einmal bearbeiten statt Screen fuer Screen
- ❌ `context.getString()` in ViewModels (Lifecycle-Problem)
- ❌ Unnummerierte Platzhalter (`%s` statt `%1$s`)
- ❌ `<plurals>` ohne `other`-Kategorie
- ❌ String Arrays mit direktem Text statt `@string/`-Referenzen
- ❌ Gleichen String-Key fuer verschiedene Kontexte wiederverwenden
- ❌ Angezeigten Text als Vergleichswert/Key/Identifier verwenden (bricht bei Sprachwechsel)
- ❌ Enum-displayName gleichzeitig fuer UI-Anzeige UND Datenspeicherung nutzen
- ❌ `stringResource()` in `remember {}` cachen (aktualisiert sich nicht bei Sprachwechsel)
- ❌ Click-Handler beim Refactoring von Text() zu stringResource() verlieren

### Effiziente Bearbeitung bei grossen Projekten

- **Dateien ueber 500 Zeilen**: Grep → gezieltes Read (offset+limit) → Edit
- **3+ gleichartige Aenderungen pro Datei**: Python-Batch-Script
- **Immer Build pruefen** nach jedem Screen (nicht erst am Ende)
- **Commit nach jedem abgeschlossenen Screen** (Rettungspunkte!)

### Zusammenspiel mit dem Uebersetzungs-Skill

Dieser Skill ist die VORSTUFE:

```
String-Extraktor (dieser Skill)     Uebersetzungs-Skill
─────────────────────────────       ─────────────────────
1. Hardcodierte Strings finden      1. strings.xml lesen
2. Bestehende Strings pruefen       2. Sprache fuer Sprache uebersetzen
3. Fehlende Strings erstellen       3. Verifizieren + Committen
4. Qualitaet sicherstellen
5. Funktionalitaet pruefen          Sprachtest (Englisch)
         ↓                                   ↓
   strings.xml ist komplett    →    Uebersetzer kann starten
   + keine Funktions-Bugs              + alle Sprachen OK
```
