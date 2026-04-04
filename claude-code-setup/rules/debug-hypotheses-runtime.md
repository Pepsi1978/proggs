# Debug-Hypothesen: Erst vermuten, dann messen, dann fixen (KRITISCH)

> Quelle: Cursor Debug Mode 2.2 (cursor.com/blog/debug-mode), arXiv 2604.00167 (Fault Localization)
> Direktive: #3 Resilient Bugfixing

## Regel

Bei nicht-trivialen Bugs (Root Cause nicht sofort sichtbar) MUSS der Debugger
diese 4 Schritte in genau dieser Reihenfolge durchlaufen — KEIN Fix ohne vorherige
Laufzeitdaten.

## Der 4-Schritte-Loop

### Schritt 1: HYPOTHESEN FORMULIEREN

Vor jeder Code-Aenderung mindestens 2, maximal 3 Hypothesen aufstellen.

**Format fuer jede Hypothese:**
```
Hypothese N: [Funktion X] macht [Y] falsch weil [Z]
```

**Regeln:**
- Die Hypothese benennt eine konkrete **Funktion** — nicht nur eine Datei oder Zeile
- Jede Hypothese benennt eine **andere** moegliche Ursache
- Hypothesen werden nach Wahrscheinlichkeit geordnet (wahrscheinlichste zuerst)

**Beispiele:**
```
Hypothese 1: saveUserData() speichert den State nicht atomisch, weil kein Mutex verwendet wird
Hypothese 2: loadUserData() liest eine veraltete Datei, weil der Cache nicht invalidiert wird
Hypothese 3: onPause() wird nicht aufgerufen, weil die Activity-Lifecycle-Kette unterbrochen ist
```

### Schritt 2: INSTRUMENTIEREN

Fuer jede Hypothese einen konkreten Messpunkt definieren — BEVOR Code veraendert wird.

**Logging nach Plattform:**

| Plattform | Methode |
|-----------|---------|
| Android / Kotlin | `Log.d("DEBUG", "Wert: $variable")` → Logcat filtern |
| Web / TypeScript | `console.log("Punkt X:", variable)` → DevTools Console |
| CLI / Python | `print(f"DEBUG: {variable}")` |
| C# / WPF | `Debug.WriteLine($"DEBUG: {variable}")` → Output-Fenster |
| iOS / Swift | `print("DEBUG: \(variable)")` → Xcode Console |

**Pflicht:** Den Benutzer bitten, die App/den Code auszufuehren und die Logs zu teilen —
NIEMALS einen Fix vorschlagen ohne Laufzeitdaten gesehen zu haben.

### Schritt 3: RUNTIME-DATEN ANALYSIEREN

Die echten Laufzeitdaten auswerten:
- Welche Hypothese wird durch die Logs **bestaetigt**?
- Welche wird durch die Logs **widerlegt**?
- Gibt es **unerwartete Werte** die auf eine neue Hypothese hindeuten?

Erst NACH dieser Analyse einen Fix vorschlagen.

**Wenn keine Hypothese bestaetigt wird:**
→ Maximal 1 weiterer Hypothesen-Runde (Schritt 1-3 wiederholen)
→ Nach 2 Runden ohne Ergebnis: Den Benutzer um einen Minimal-Reproduktions-Fall bitten

### Schritt 4: GEZIELTER FIX

Der Fix:
- Basiert auf den **echten Laufzeitdaten**, nicht auf Vermutungen
- Loest genau die bestaete Hypothese
- Enthalt einen Kommentar: `// Fix: [was wurde in den Logs beobachtet]`

Nach dem Fix: Logging-Statements die nur zum Debuggen eingebaut wurden ENTFERNEN.

## Wann diese Regel NICHT gilt

Diese 4 Schritte sind nur fuer nicht-triviale Bugs. Ausnahmen:

| Situation | Begruendung |
|-----------|-------------|
| Tippfehler, falscher Variablenname | Root Cause sofort sichtbar |
| Fehlender Import, fehlende Dependency | Compiler/Build-Fehler ist eindeutig |
| Build-Fehler mit klarer Fehlermeldung | Die Meldung BENENNT die Ursache |
| Fehler den der Benutzer direkt erklaert | Root Cause ist schon bekannt |

**Faustregel:** Wenn die Root Cause nach 30 Sekunden Lesen noch unklar ist → Hypothesen-Loop starten.

## Zusammenspiel mit anderen Regeln

- **`inspect-before-guessing.md`**: Ergaenzend — inspect-before-guessing gilt fuer UI/DOM-Struktur,
  diese Regel gilt fuer Laufzeit-Logik und State-Fehler. Beide verhindern Trial-and-Error.
- **`cbr-bugfix-search.md`**: ZUERST die Bug-Datenbank durchsuchen. Wenn ein aehnlicher Fall
  gefunden wird: den alten Fix als erste Hypothese verwenden.
- **`resilient-bugfixing.md`**: Nach dem Fix die Loesung in die Bug-Datenbank eintragen
  (symptom, root_cause, fix, tags).

## Was NIEMALS passieren darf

- ❌ Fix vorschlagen bevor Laufzeitdaten vorliegen
- ❌ Mehr als 3 Hypothesen aufstellen (zu viele → kein Fokus)
- ❌ Eine Hypothese formulieren die keine konkrete Funktion benennt ("irgendwo im State")
- ❌ Mehr als 2 Runden ohne Laufzeitdaten durchlaufen
- ❌ Logging-Statements nach dem Fix im Code lassen (ausser produktive Logs die sowieso geplant waren)
- ❌ Den Benutzer um Logs bitten ohne ihm GENAU zu sagen wo er filtern soll

## Warum

Ohne Hypothesen-Debugging probiert der Debugger Fixes blind aus — jeder Fehlversuch ist
ein Commit, ein Push, ein Build-Zyklus. Mit Laufzeitdaten trifft der erste Fix in 80%+
der Faelle direkt (Cursor Blog: +73% fix rate in debug mode).
Ein Bug der nicht reproduziert und gemessen wird, kann nicht sicher gefixt werden.
