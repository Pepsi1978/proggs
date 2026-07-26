# Funktionalitaets-Check — Strings duerfen nichts kaputt machen

> Diese Referenz ist die ausgelagerte Detail-Version von **Phase 5** des
> string-extraktor-Skills. Sie kann auch eigenstaendig genutzt werden:
> Trigger sind Phrasen wie "pruefe ob Strings Funktionen kaputt machen",
> "Funktions-Check fuer extrahierte Strings", "string functional regression".
>
> **Warum diese Pruefung existiert:** String-Extraktion kann Funktionalitaet
> zerstoeren, wenn angezeigter Text im Code auch als Identifikator,
> Vergleichswert oder DB-Key verwendet wird. Diese Phase findet solche Stellen
> BEVOR die App auf einer anderen Sprache getestet wird — und BEVOR Bugs in
> Produktion landen.
>
> **Wann durchfuehren:**
> - Nach Phase 4 (VERIFY) als letzter Schritt bei der Erstextraktion
> - Eigenstaendig nach Sprachtest-Bugs ("die App crasht auf Englisch")
> - Vor jedem Release als Pflicht-Pruefung

---

## Inhalt

- 5.1 Text-als-Identifikator (KRITISCH — haeufigstes Problem)
- 5.2 Enum-Serialisierung (DATEN-VERLUST-RISIKO)
- 5.3 Format-String-Sicherheit (CRASH-PRAEVENTION)
- 5.4 Compose-Kontext-Fehler (COMPILE- + RUNTIME-BUGS)
- 5.5 Stiller Fallback-Check
- 5.6 Funktionalitaets-Report-Format

---

## 5.1 Text-als-Identifikator finden (KRITISCH)

Hardcodierter Text der gleichzeitig als Vergleichswert, Map-Key oder Routing-Ziel
verwendet wird, bricht in JEDER Sprache ausser der Default-Sprache.

### Grep-Muster zum Finden

```
# When/If-Vergleiche mit angezeigtem Text
Grep: == "[A-ZAEOEUE]
Grep: when\s*\(.*\)\s*\{[^}]*"[A-ZAEOEUE]

# HashMap/Map mit Text-Keys
Grep: mapOf\(.*"[A-ZAEOEUEa-zaeoeuess]+.*to\s

# SharedPreferences/Room mit angezeigtem Text als Wert
Grep: putString\(.*,\s*"[A-ZAEOEUE]
Grep: == getString\(
```

### Typische Anti-Patterns und Fixes

| Anti-Pattern (KAPUTT bei Sprachwechsel) | Fix |
|----------------------------------------|-----|
| `when(chip.text) { "Taeglich" -> DAILY }` | `when(chip.tag) { ScheduleType.DAILY -> ... }` |
| `if (button.text == "Speichern") onSave()` | `onClick = { onSave() }` direkt am Button |
| `mapOf("Speichern" to ::onSave)` | `mapOf(Action.SAVE to ::onSave)` mit Enum |
| `prefs.putString("mood", moodLabel)` | `prefs.putString("mood", mood.name)` (Enum-Name) |
| `navController.navigate(screenTitle)` | `navController.navigate(Route.SETTINGS)` |

**Faustregel:** Wenn ein String sowohl dem Benutzer ANGEZEIGT als auch im Code
VERGLICHEN wird, muss der Vergleich auf einen sprachunabhaengigen Identifier
umgestellt werden (Enum, sealed class, Int-ID, String-Konstante).

---

## 5.2 Enum-Serialisierungs-Check (DATEN-VERLUST-RISIKO)

Enums deren `displayName` sowohl fuer die UI-Anzeige als auch fuer die
Datenspeicherung (Room, SharedPreferences, JSON) verwendet wird, verursachen
**Datenverlust** bei Sprachwechsel.

### Grep-Muster

```
# Enums mit displayName/label Property
Grep: enum class.*val\s+(displayName|label|title|text)\s*[:=]

# Enum-Werte die in Room/SharedPreferences gespeichert werden
Grep: \.displayName.*putString\|putString.*\.displayName
Grep: \.label.*putString\|putString.*\.label
```

### Beispiel-Bug + Fix

```kotlin
// KAPUTT nach Extraktion:
enum class Mood(val displayName: String) {
    HAPPY("Gluecklich"), SAD("Traurig")  // In Room als "Gluecklich" gespeichert
}
// Nach Extraktion: displayName = getString(R.string.mood_happy) -> "Happy" in English
// Alte DB-Eintraege: "Gluecklich" -> Room findet keinen Match -> NULL/Crash

// FIX: Anzeige und Speicherung TRENNEN
enum class Mood(@StringRes val displayNameRes: Int) {
    HAPPY(R.string.mood_happy),
    SAD(R.string.mood_sad);
    // Room speichert enum.name ("HAPPY") -- sprachunabhaengig
    // UI zeigt stringResource(mood.displayNameRes) -- lokalisiert
}
```

**Pflicht-Pruefung:** Bei JEDEM Enum mit String-Property pruefen:
1. Wird die Property in Room/SharedPreferences/JSON gespeichert? -> `name` statt `displayName` verwenden
2. Wird die Property fuer Vergleiche benutzt? -> Auf Enum-Typ vergleichen

---

## 5.3 Format-String-Sicherheit (CRASH-PRAEVENTION)

Falsche Platzhalter fuehren zu Runtime-Crashes die NUR bei bestimmten Sprachen auftreten.

### Was pruefen

| Pruefung | Wie | Crash-Typ |
|----------|-----|-----------|
| **Argument-Anzahl** | Zaehle `%` in String vs. Argumente in `stringResource()`/`getString()` | `MissingFormatArgumentException` |
| **Argument-Typ** | `%d` erwartet Int, `%s` erwartet String -- stimmt der Code? | `IllegalFormatConversionException` |
| **Argument-Reihenfolge** | `%1$s` = erster Arg, `%2$d` = zweiter Arg -- stimmt die Reihenfolge? | Falscher Text (kein Crash) |
| **Doppelte Nummerierung** | `%1$s ... %1$s` ist OK (gleiches Arg 2x), aber `%1$s %3$s` ohne `%2$s` ist verdaechtig | `MissingFormatArgumentException` |

### Grep-Muster zum Finden

```
# Strings mit Platzhaltern in strings.xml
Grep: %[0-9]*\$?[sdfu]    (in strings.xml)

# Aufrufe mit stringResource/getString (im Code)
Grep: stringResource\(R\.string\.\w+,
Grep: getString\(R\.string\.\w+,
```

### Automatisierter Abgleich

Pro `stringResource()`-Stelle im Code den String-Key extrahieren, in `strings.xml`
die `%`-Anzahl zaehlen, gegen die Anzahl uebergebener Argumente im Aufruf
vergleichen. Bei Differenz: ALARM.

Das `scripts/validate_extracted.py` macht genau diesen Abgleich automatisch.

---

## 5.4 Compose-Kontext-Fehler (COMPILE- UND RUNTIME-BUGS)

| Problem | Grep-Muster | Fix |
|---------|-------------|-----|
| `stringResource` in `remember {}` | `Grep: remember\s*\{[^}]*stringResource` | `stringResource` aus `remember` rausziehen — sonst aktualisiert sich der Text nicht bei Sprachwechsel |
| `getString()` im ViewModel | `Grep: context\.getString\|context\.resources` | `@StringRes Int` oder `UiText` sealed class verwenden |
| `stringResource` in nicht-Composable-Lambda | `Grep: \.let\s*\{[^}]*stringResource\|\.map\s*\{[^}]*stringResource` | In der Composable-Funktion aufloesen, nicht in der Lambda |
| Verlorener Click-Handler nach Refactoring | Manuell pruefen: Hat jeder Button noch seinen `onClick`? | Vor/Nachher-Vergleich des Composable-Baums |

### Haeufigster Compose-Bug nach String-Extraktion

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

---

## 5.5 Stiller Fallback-Check (MEISTENS UEBERSEHEN)

Wenn ein String-Key in `values/strings.xml` existiert aber NICHT in
`values-en/strings.xml`, zeigt Android den deutschen Text in der englischen App.
**Kein Crash, kein Warning, keine Fehlermeldung** — der Benutzer sieht einfach
deutschen Text mitten in der englischen App.

### Wie pruefen (nach dem Uebersetzungs-Skill)

```bash
# Alle Keys aus der Default-Datei extrahieren
grep 'name="' values/strings.xml | sed 's/.*name="\([^"]*\)".*/\1/' | sort > /tmp/default-keys.txt

# Alle Keys aus der Ziel-Sprache extrahieren
grep 'name="' values-en/strings.xml | sed 's/.*name="\([^"]*\)".*/\1/' | sort > /tmp/en-keys.txt

# Fehlende Keys finden
comm -23 /tmp/default-keys.txt /tmp/en-keys.txt
```

### Oder per Android Lint

```bash
./gradlew lint 2>&1 | grep "MissingTranslation"
```

### Wann dieser Check laufen soll

- NACH dem Uebersetzungs-Skill (sind alle Sprachen komplett?)
- NACH dem Hinzufuegen neuer Strings (vergessen in andere Sprachen zu uebersetzen?)
- ALS Teil des Sprachtests (deutscher Text in englischer App gesehen?)

---

## 5.6 Funktionalitaets-Report (PFLICHT — dem Benutzer zeigen)

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

## Eigenstaendige Nutzung (ohne vollen Skill-Lauf)

Diese Pruefung kann eigenstaendig nach jedem Sprachtest aufgerufen werden.
Trigger sind Aussagen wie:

- "die App crasht auf Englisch nach String-Extraktion"
- "pruefe ob das Refactoring Funktionen kaputt gemacht hat"
- "Daten-Verlust nach Locale-Wechsel"
- "stringResource hat den onClick geschluckt"
- "Funktions-Check fuer die strings"

In dem Fall:
1. Direkt zu 5.1 springen (Grep-Muster anwenden)
2. Bei Treffern: Tabelle der Anti-Patterns durchgehen
3. 5.2 + 5.3 + 5.4 als Folge-Checks
4. 5.5 nur wenn schon Uebersetzungen existieren
5. Report wie in 5.6 zeigen
