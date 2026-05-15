---
name: string-extraktor
description: Findet ALLE hardcodierten Strings in Android-Apps (Compose und XML), prueft bestehende String-Ressourcen und erstellt fehlende Strings nach deutschen Sprach- und Typografie-Regeln. Primaersprache ist Deutsch — der Skill kennt deutsche Orthographie (ß, Umlaute, neue Rechtschreibung), Typografie („deutsche Anfuehrungszeichen", Halbgeviert –, geschuetzte Leerzeichen), Substantiv-Grossschreibung, Du/Sie-Konsistenz, gendergerechte Sprache, Kompositum-Laengen-Strategien, deutsche Zahlen-/Datums-/Zeit-/Waehrungs-Formate (DIN 5008), CLDR-Plurals (one/other fuer Deutsch) und die DACH-Regionsunterschiede. Nutze diesen Skill IMMER wenn der Benutzer sagt "Strings finden", "Strings erstellen", "String-Extraktion", "hardcodierte Strings", "hardcoded Strings suchen", "i18n Audit", "i18n pruefen", "Strings ueberpruefen", "String-Qualitaet", "fehlende Strings", "Strings fehlen noch", "alle Strings extrahieren", "Internationalisierung vorbereiten", "strings.xml auffuellen", "String Extractor", "String Extraktor", "sind alle Strings erstellt?", "Strings komplett?", "i18n fertig?", "alle Texte externalisiert?", "noch nicht uebersetzte Strings", "deutsche Strings erstellen", "deutsche Sprache pruefen", "sind die Strings korrekt deutsch?". Auch wenn der Benutzer nach einem Feature fragt "pruefe ob alle Strings da sind" oder "bevor wir uebersetzen — sind die Strings vollstaendig?". Dieser Skill ist die VORSTUFE zum Uebersetzungs-Skill: erst String-Extraktor (deutsche Strings finden + erstellen nach DE-Regeln), DANN Uebersetzung (DE-Strings in andere Sprachen). Funktioniert fuer JEDES Android-Projekt, nicht nur ein bestimmtes.
---

# String-Extraktor: Deutsche Strings finden, pruefen und erstellen

Dieser Skill arbeitet ein Android-Projekt systematisch ab um sicherzustellen, dass
**100% aller benutzersichtbaren Strings** als String-Ressourcen in `strings.xml`
vorliegen — **auf korrektem Deutsch**, typografisch sauber, konsistent in der
Anrede (Du/Sie), gender-inklusiv wo sinnvoll, mit korrekter Grossschreibung,
Platzhaltern, Kommentaren und Plural-Unterstuetzung, bereit fuer die
Uebersetzung durch den separaten Uebersetzungs-Skill.

**Primaersprache:** Alle Apps werden zuerst auf **Deutsch** entwickelt.
Die deutschen Strings sind der Master — aus ihnen werden alle anderen Sprachen
uebersetzt. **Qualitaet der deutschen Originale entscheidet ueber die Qualitaet
aller Uebersetzungen.** Deshalb hat Deutsch in diesem Skill hoechste Prioritaet.

**Abgrenzung:** Dieser Skill ERSTELLT Strings in der Default-Sprache (Deutsch).
Er uebersetzt NICHT in andere Sprachen — dafuer gibt es den `uebersetzung`-Skill.

---

## Referenzdateien laden

**Vor dem Start MUESSEN beide Referenzdateien gelesen werden:**

```
Read: references/string-best-practices.md     (Grep-Muster, Naming, Struktur)
Read: references/deutsche-sprache.md          (Deutsche Rechtschreibung, Stil, Formate)
```

`string-best-practices.md` enthaelt 25 Kapitel mit Grep-Mustern, Naming-Konventionen,
Erstellungsregeln und Qualitaets-Checklisten (allgemein fuer Android).

`deutsche-sprache.md` enthaelt die komplette deutsche Sprach-Referenz:
- **Teil A** — Orthographie & Typografie (ß, Umlaute, Anfuehrungszeichen, Striche, Grossschreibung, Rechtschreibung, Zeichensetzung, Abkuerzungen)
- **Teil B** — UX & Stil (Du/Sie, Gendern, Kompositum-Laenge, System-Strings, Button-Labels, Fehlermeldungen, Anglizismen)
- **Teil C** — Zahlen, Datum, Zeit, Waehrung, Einheiten, Plurals, Listen, Regional (DE/AT/CH)
- **Teil D** — Kotlin-Utility + 20-Punkte-Checkliste

Falls die Hauptdatei nicht an der zentralen Stelle liegt, nutze die gebundelten
Kopien im `references/`-Verzeichnis dieses Skills.

---

## Die 5 Phasen im Ueberblick

```
PHASE 0: SPRACH-KONTEXT ── Du/Sie-Entscheidung, Gender-Strategie, Region (NEU)
    │
PHASE 1: SCAN ──────────── Alle hardcodierten Strings finden
    │
PHASE 2: AUDIT ─────────── Bestehende strings.xml pruefen (+ deutsche Qualitaet)
    │
PHASE 3: CREATE ────────── Fehlende Strings nach DE-Regeln erstellen
    │
PHASE 4: VERIFY ────────── Zweiter Durchlauf + DE-Qualitaets-Checkliste (20 Punkte)
    │
PHASE 5: FUNKTIONS-CHECK ── Strings duerfen nichts kaputt machen
```

Jede Phase endet mit einem **Fortschritts-Report** an den Benutzer.
Der Benutzer soll nach jeder Phase sehen was gefunden/erstellt/geprueft wurde.

---

## Phase 0: SPRACH-KONTEXT — Deutsche Sprach-Entscheidungen festlegen

> **Warum diese Phase EXISTIERT:** Ein String wie „Willkommen zurueck" kann fuer
> eine Banking-App komplett falsch sein (muesste Sie-Form heissen), und
> „Benutzer" ist fuer eine moderne Consumer-App nicht mehr inklusiv (besser
> „Nutzende"). Diese Entscheidungen MUESSEN einmalig fuer die gesamte App
> getroffen werden, bevor auch nur ein String erstellt wird.

**Wann diese Phase laeuft:**
- Bei JEDEM Erstdurchlauf (wenn noch keine `strings.xml` existiert)
- Wenn `strings.xml` bereits existiert: schnell-pruefen ob die Entscheidungen
  konsistent umgesetzt wurden, dann zu Phase 1
- Bei spaeteren Aufrufen: Gespeicherte Entscheidungen laden (siehe 0.4)

### 0.1 Anrede-Entscheidung: Du oder Sie?

Die wichtigste und verbindlichste Entscheidung. Gilt fuer die gesamte App.

**Faustregel fuer Default:**
- Consumer/Social/Spiele/Fitness/Streaming → **Du**
- Banking/Versicherung/Medizin/Behoerden/B2B → **Sie**
- Unklar → **Du** ist der moderne Standard der 2020er

Aus Teil B.1 der Referenz fuer die detaillierte Entscheidungsmatrix.

**Wenn `strings.xml` bereits existiert:** Autodetection durch Stichprobe:
```
Grep in values/strings.xml nach "Sie " und "\bdu\b" / "\bdir\b" / "\bdein"
```
- Ueberwiegend „Sie/Ihr/Ihnen" → App nutzt Sie-Form
- Ueberwiegend „du/dir/dein/deine" → App nutzt Du-Form
- Gemischt → **ALARM** — Inkonsistenz, muss bereinigt werden bevor weitergemacht wird

**Wenn `strings.xml` neu ist:** Benutzer fragen (mit begruendetem Vorschlag):
```
Diese App sieht nach [KONTEXT aus Projektname/Features] aus.
Meine Empfehlung: [Du/Sie]. Grund: [...]
Ist das okay, oder soll ich eine andere Anrede verwenden?
```

### 0.2 Gender-Strategie festlegen

**Empfohlene Rangfolge (siehe Teil B.2):**
1. **Partizip-Substantive** (Nutzende, Lernende) — empfohlen
2. **Geschlechtsneutrale Substantive** (Person, Konto, Mitglied) — empfohlen
3. **Doppelpunkt-Notation** (Nutzer:innen) — nur Fliesstext, nie Buttons
4. **Gender-Stern** (Nutzer*innen) — nur wenn Zielgruppe das erwartet
5. **Generisches Maskulinum** (Nutzer) — nur noch im absoluten Ausnahmefall

**Default fuer neue Apps:** Strategie 1+2 (Partizip + Neutral).

**Wenn `strings.xml` existiert:** Autodetection via Grep nach `\*in`, `:innen`,
`_innen`, `/innen` in bestehenden Strings.

### 0.3 Region festlegen

| Region | values-Ordner | Besonderheiten |
|--------|--------------|---------------|
| Deutschland (Standard) | `values-de/` | ß, EUR, DIN 5008 |
| Oesterreich | `values-de-rAT/` | ß, EUR, regionaler Wortschatz (Jaenner) |
| Schweiz | `values-de-rCH/` | Kein ß (ss), CHF, Apostroph-Trenner |

**Default:** `values-de` fuer alle DACH-Regionen (Deutschland als Basis).
Nur bei expliziter Zielregion Schweiz/Oesterreich einen zusaetzlichen
Ordner anlegen.

### 0.4 Entscheidungen in der App festschreiben

Damit spaetere Skill-Aufrufe die Entscheidungen nicht neu treffen muessen,
werden sie im Kommentar-Header der `strings.xml` hinterlegt:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Sprach-Konfiguration:
  - Anrede: Du (informell, modern)
  - Gender: Partizip-Substantive + geschlechtsneutrale Begriffe
  - Region: de (DACH-Standard, Schweiz separat in values-de-rCH)
  - Stil-Referenz: ~/.Gemini/skills/string-extraktor/references/deutsche-sprache.md
-->
<resources xmlns:tools="http://schemas.android.com/tools"
           xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
           tools:locale="de">
```

### 0.5 Phase-0-Report

```
SPRACH-KONTEXT FESTGELEGT:
═══════════════════════════════════════
Anrede:       Du / Sie
Gender:       Partizip + Neutral / Gender-Stern / ...
Region:       Deutschland (values-de)
Quell-App:    [App-Name]
Referenz:     references/deutsche-sprache.md
═══════════════════════════════════════

Alle nachfolgenden Strings werden gemaess dieser Konfiguration erstellt.
```

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

**Schritt 6: Deutsche Sprach-Validierung (PFLICHT fuer jeden neuen String)**

> **Warum dieser Schritt PFLICHT ist:** Primaersprache ist Deutsch. Ein schlecht
> formulierter deutscher String sabotiert ALLE spaeteren Uebersetzungen. Dieser
> Schritt fliesst direkt aus `references/deutsche-sprache.md`.

Fuer JEDEN neu erstellten deutschen String diese 10 Validierungen durchgehen
(Details je Punkt in der DE-Referenz):

| # | Pruefung | DE-Ref | Haeufige Fehler |
|---|---------|--------|-----------------|
| 1 | Anrede konsistent mit Phase-0-Entscheidung (Du oder Sie, nie gemischt) | B.1 | `Sie` + `dein` mischen |
| 2 | Sie/Ihnen/Ihr grossgeschrieben (nur bei Sie-App) | A.3, B.1 | `ihre Daten` statt `Ihre Daten` |
| 3 | Substantive gross — aber KEINE englische Title Case | A.3 | `Einstellungen Speichern` |
| 4 | ß vs. ss korrekt (ß nach lang, ss nach kurz, dass/muss) | A.1 | `daß`, `muß`, `Fluß` |
| 5 | Umlaute direkt (ä, ö, ü, ß) — nie `\u00dc` oder `ae/oe/ue` im Fliesstext | A.1 | `Ueberblick`, `\u00dc` |
| 6 | Typografische Anfuehrungszeichen „..." wenn Text zitiert wird | A.2 | `"Text"` statt `„Text"` |
| 7 | Gedankenstrich – (U+2013) mit Leerzeichen fuer Einschuebe; Bis-Strich ohne | A.2 | `9-17 Uhr` statt `9–17 Uhr` |
| 8 | Kein Punkt bei Buttons, Labels, Titeln; Punkt bei vollstaendigen Saetzen | A.5 | `Speichern.`, `Speichere!` |
| 9 | Gender: Partizip (`Nutzende`) oder neutral (`Person`) bei neuen Strings | B.2 | neu: `Benutzer` ohne Begruendung |
| 10 | Kein Denglisch (`geshared`, `geliked`, `canceln`) — deutsche Form | B.7 | `geshared` statt `geteilt` |

**Zusaetzliche Validierung bei bestimmten String-Typen:**

| String-Typ | Zusaetzliche Pruefung | DE-Ref |
|-----------|----------------------|--------|
| Button | Infinitiv, nicht Imperativ; max 20 Zeichen; aktionsorientiert | B.5 |
| Dialog-Button-Paar | Spezifisch statt "Ja"/"Nein" (`Loeschen`/`Behalten`) | B.5 |
| Fehlermeldung | Nuetzlich + verstaendlich + knapp + empathisch; keine Schuldzuweisung | B.6 |
| Tab/AppBar-Titel | Unter Laengenlimit; bei langem Kompositum Soft-Hyphen einplanen | B.3 |
| Zahl/Datum/Zeit/Waehrung | DIN-5008-konform; `GermanFormatter`-Helper nutzen | C.1-C.5 |
| Plural-String | Nur `one` + `other`; 0 faellt unter `other`; Empty-State separat | C.6 |
| Technische Abk. (URL, API, PDF) | Immer Grossbuchstaben | A.6 |
| Geschuetztes Leerzeichen | Vor Einheiten (`100\u00A0MB`), in `z.\u00A0B.` | A.6 |

**Bei Verstoss:** Direkt den String ueberarbeiten. Nicht erst am Ende sammeln —
der Fix ist waehrend des Schreibens 10x schneller als nachtraeglich.

**Automatisierbare DE-Checks (Grep-basiert):**

```
# Verstoss: englische Title Case in Composita
Grep: "[a-zäöüß]+\s+[A-ZÄÖÜ][a-zäöüß]+\s+[A-ZÄÖÜ]"  (in strings.xml, kontextabhaengig)

# Verstoss: veraltete Rechtschreibung
Grep: "daß|muß|Haß|Fluß|Tip\b"

# Verstoss: Schreibmaschinen-Anfuehrungszeichen
Grep: >[^<]*"[^"]*"[^<]*<    (in strings.xml, indikativ)

# Verstoss: Bindestrich statt Bis-Strich
Grep: "\d+\s*-\s*\d+\s*Uhr"

# Verstoss: Denglisch
Grep: "ge(shared|liked|checked|pushed|downloaded)"

# Verstoss: Unicode-Escape fuer deutsche Umlaute im Code
Grep: "\\u00(dc|fc|d6|f6|c4|e4|df)"

# Verstoss: fehlendes geschuetztes Leerzeichen vor Einheit
Grep: "\d+(MB|GB|KB|kg|km|°C|%)\b"    (ohne NBSP oder Leerzeichen davor)
```

Diese Grep-Muster laufen automatisch in Phase 4 (VERIFY).

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

### 4.4 Abschluss-Checkliste (14 Struktur + 20 Deutsch = 34 Punkte)

Alle Punkte muessen mit JA beantwortet werden (Details: Referenzdateien).

**Nach dem Finden (Struktur):**
- [ ] Grep-Muster 1-9 auf alle .kt-Dateien angewendet?
- [ ] Alle Screens im Inventar abgehakt?
- [ ] ViewModels auf benutzersichtbare Strings geprueft?
- [ ] Content Descriptions fuer alle Icons/Bilder vorhanden?

**Nach dem Erstellen (Struktur):**
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

**Deutsche Sprach-Qualitaet — 20 Punkte (aus references/deutsche-sprache.md, Teil D.2):**

*Orthographie & Typografie (6 Punkte)*
- [ ] ß nach langem Vokal / nach Diphthong; ss nach kurzem Vokal (kein `daß`, `muß`, `Haß`, `Fluß`)
- [ ] Umlaute ä/ö/ü/ß direkt in UTF-8 — KEIN `\u00dc`-Escape im Kotlin-Code
- [ ] Typografische Anfuehrungszeichen `„..."` statt `"..."`
- [ ] Gedankenstrich `–` (U+2013) mit Leerzeichen fuer Einschuebe; ohne fuer Bis-Spannen (`9–17 Uhr`)
- [ ] Substantive IMMER gross, auch in Ueberschriften (kein `Einstellungen Speichern`)
- [ ] Auslassungspunkte `…` (U+2026) als 1 Zeichen, nicht drei Punkte

*Stil & UX (4 Punkte)*
- [ ] Du/Sie konsistent in der GESAMTEN App — keine Mischung
- [ ] Sie/Ihnen/Ihr IMMER grossgeschrieben (bei Sie-Apps)
- [ ] Gender-neutrale Formen bei neuen Strings bevorzugt (Partizip: `Nutzende`, `Lernende`)
- [ ] Keine Genderzeichen (`*`, `:`, `_`) in Button-Labels (Screenreader-Stoerung!)

*Buttons & Labels (5 Punkte)*
- [ ] Buttons im Infinitiv, nicht Imperativ (`Speichern`, nicht `Speichere!`)
- [ ] Kein Punkt bei Buttons, Titeln, Labels, Menue-Eintraegen
- [ ] Dialog-Buttons spezifisch statt `Ja`/`Nein` (`Loeschen`/`Behalten`)
- [ ] Technische Abkuerzungen IMMER gross (URL, API, PDF, ID)
- [ ] Max. Laengen beachtet (Tab: 12, Button: 20, AppBar: 25, Menue: 30)

*Formate (5 Punkte)*
- [ ] Dezimaltrennzeichen `,` — NIE `.` (`3,14`, nicht `3.14`)
- [ ] Datum `TT.MM.JJJJ` (`17.04.2026`) oder ausgeschrieben `17. April 2026`
- [ ] Zeit 24-Stunden-Format (`14:30`), nie 12h mit PM
- [ ] Waehrung `10,50 €` (Symbol NACH Betrag, mit geschuetztem Leerzeichen `\u00A0`)
- [ ] Plurals: NUR `one` + `other` fuer Deutsch (kein `zero`/`few`/`many`)

**Automatisierte DE-Grep-Checks in Phase 4:**
```
# Veraltete Rechtschreibung
Grep: (daß|muß|mußt|Haß|Fluß|Tip\b)    → muss 0 Treffer sein

# Unicode-Escape-Umlaute im Code
Grep: \\u00(dc|fc|d6|f6|c4|e4|df)       → muss 0 Treffer sein

# Denglisch
Grep: ge(shared|liked|checked|pushed|downloaded)   → 0 Treffer

# Bindestrich statt Bis-Strich bei Zeitspannen
Grep: \d+\s*-\s*\d+\s*Uhr                → muss 0 Treffer sein

# Schreibmaschinen-Anfuehrungszeichen in sichtbaren Strings
Grep in strings.xml: >[^<]*\"[^\"]*\"[^<]*<    → pruefen

# Fehlendes NBSP vor Einheit
Grep: \d+(MB|GB|kg|km|°C)\b              → pruefen
```

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

**Struktur/Technik:**
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

**Deutsche Sprache (zusaetzlich):**
- ❌ Du und Sie in der gleichen App mischen — muss app-weit konsistent sein
- ❌ Sie/Ihnen/Ihr klein schreiben (das ist 3. Person Plural, nicht Anrede)
- ❌ Veraltete Rechtschreibung (`daß`, `muß`, `Haß`, `Fluß`, `Tip`) verwenden
- ❌ Unicode-Escapes `\u00dc` statt `Ü` im Kotlin-Code — muss als String-Ressource extrahiert werden
- ❌ Englische Title Case in deutschen Ueberschriften (`Einstellungen Speichern` statt `Einstellungen speichern`)
- ❌ Schreibmaschinen-Anfuehrungszeichen `"..."` statt typografischer `„..."` in sichtbaren Strings
- ❌ Bindestrich `-` statt Bis-Strich `–` bei Zeitspannen (`9-17 Uhr`)
- ❌ Drei Punkte `...` statt Auslassungspunkte `…` (U+2026)
- ❌ Generisches Maskulinum (`Benutzer`) in NEUEN Strings ohne Begruendung — bevorzugt Partizip (`Nutzende`) oder Neutral (`Person`)
- ❌ Genderzeichen (`*`, `:`, `_`) in Button-Labels (stoert Screenreader)
- ❌ Denglisch-Formen (`geshared`, `geliked`, `downgeloaded`, `canceln`, `forwarden`)
- ❌ Punkt am Ende von Buttons, Titeln, Labels, Menue-Eintraegen
- ❌ Imperativ-Form statt Infinitiv (`Speichere!` statt `Speichern`)
- ❌ Vage Dialog-Buttons (`Ja`/`Nein`) statt spezifischer Aktionen (`Loeschen`/`Behalten`)
- ❌ Fehlendes geschuetztes Leerzeichen zwischen Zahl und Einheit (`100MB` statt `100\u00A0MB`)
- ❌ Englisches Zahlenformat (`3.14`, `1,000.50`) statt deutschem (`3,14`, `1.000,50`)
- ❌ 12-Stunden-Zeitformat mit AM/PM — immer 24h (`14:30`)
- ❌ Waehrungssymbol VOR dem Betrag (`€10,50`) statt DANACH (`10,50 €`)
- ❌ `quantity="zero"` in Plurals verwenden — fuer Deutsch wirkungslos, Empty-State separat
- ❌ `Benutzer` durch `Benutzer*in` in Button-Label ersetzen — nutze `Person` oder `Nutzende`

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

