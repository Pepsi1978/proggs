# Debugging & Verifikation: Systematisch statt Trial-and-Error (KRITISCH)

> Konsolidiert aus: confidence-check, inspect-before-guessing,
> cbr-bugfix-search, debug-hypotheses-runtime
> Ergaenzt: ~/.claude/rules/resilient-bugfixing.md (fuer den eigentlichen Fix)

---

## 1. Confidence-Ampel: Unsicherheit erkennen (KRITISCH)

> Quelle: Superintelligenz Finding 5 — arXiv 2503.15850, ICLR 2025

Bei technisch praezisen Aussagen MUSS Claude seine eigene Sicherheit bewerten:

| Farbe | Bedeutung | Aktion |
|-------|-----------|--------|
| **Gruen** | In diesem Antwortblock gelesen/ausgefuehrt ODER max 5 Turns zurueck | Sicher verwenden |
| **Gelb** | >5 Turns zurueck (gleiche Session) ODER aus frueherer Session/Training | Im Zweifel nachschlagen |
| **Rot** | Information ist Vermutung oder Schaetzung | **STOP** — genau 1 Nachschlage-Aufruf (Read/Grep/WebSearch). Danach immer noch unklar? → Explizit als Schaetzung markieren |

### Wann die Ampel PFLICHT ist

- Versionsnummern (Kotlin, Gradle, Android API Level, SDK)
- Dateipfade (Existenz pruefen!)
- API-Parameter (Funktionssignaturen, Rueckgabetypen)
- CLI-Befehle (Flags, Syntax)
- Konfiguration (JSON-Keys, YAML-Strukturen)

### Wann NICHT noetig

- Allgemeine Konzepte, Architektur-Entscheidungen, Erklaerungen in Alltagssprache

---

## 2. Inspect Before Guessing: Erst anschauen, dann aendern (KRITISCH)

**Regel:** IMMER den tatsaechlichen Zustand inspizieren bevor Code geaendert wird.

### Inspektions-Methoden nach Kontext

| Kontext | Methode |
|---------|---------|
| Web/Browser | DevTools → Elements-Tab, Console |
| Electron | DevTools → gleiche Methoden |
| Desktop-App | UI-Inspector, Accessibility-Tree |
| API | Tatsaechliche Response lesen, nicht Doku annehmen |
| Filesystem | `ls`, `stat`, tatsaechliche Datei lesen |
| Prozesse | `ps`, Task Manager, Port-Belegung pruefen |

### Haeufige Falschannahmen (NICHT raten!)

- contenteditable-Felder sehen anders aus als angenommen
- Feldhoehe/Breite ist nicht was der Code suggeriert
- aria-label kann in anderer Sprache sein
- Eltern-Element ist nicht das erwartete Tag
- CSS-Klassen koennen sich seit dem letzten Lesen geaendert haben

---

## 3. Bug-Datenbank durchsuchen VOR dem Debuggen (KRITISCH)

> Quelle: Superintelligenz Finding 6 — Case-Based Reasoning, arXiv 2504.06943

**Regel:** Bevor ein neuer Fehler debuggt wird, ZUERST die Bug-Datenbank durchsuchen:
`~/proggs/.claude/agent-memory/shared/bug-cases.jsonl`

### Die 4 CBR-Phasen

1. **Retrieve**: Bug-Datenbank durchsuchen (Grep nach Symptom/Fehlermeldung)
2. **Reuse**: Den alten Fix als ersten Loesungsansatz verwenden
3. **Revise**: Falls nicht 1:1 passend, anpassen
4. **Retain**: Den NEUEN Fall in die Datenbank eintragen

### Datenbank-Format (bug-cases.jsonl)

```json
{"date":"2026-03-31","symptom":"Push rejected: non-fast-forward","root_cause":"Kein fetch+rebase vor push","fix":"git fetch origin && git rebase origin/main vor jedem push","files":["beliebig"],"tags":["git","push","rebase"],"severity":"hoch"}
```

### Wann durchsuchen
- Bei JEDEM Build-Fehler, fehlgeschlagenen Befehl, unklaren Fehlermeldung

### Wann schreiben
- Nach JEDEM Bugfix der >5 Minuten dauerte
- Nach JEDEM Fehler der zum zweiten Mal auftrat (ALARM!)

### Zusammenspiel
- **Resilient Bugfixing** (`~/.claude/rules/resilient-bugfixing.md`): CBR formalisiert den "Verwandte Fehlerquellen suchen"-Schritt
- **Error-Antigens** (`error-antigens.jsonl`): Nutzt Bug-Cases als Grundlage fuer Praevention

---

## 4. Hypothesen-basiertes Debugging (KRITISCH)

> Quelle: Cursor Debug Mode 2.2, arXiv 2604.00167 (Fault Localization)

Bei nicht-trivialen Bugs (Root Cause nicht sofort sichtbar) dieser 4-Schritte-Loop:

### Schritt 1: HYPOTHESEN FORMULIEREN (2-3 Stueck)

```
Hypothese N: [Funktion X] macht [Y] falsch weil [Z]
```
- Jede Hypothese benennt eine konkrete **Funktion**
- Nach Wahrscheinlichkeit geordnet

### Schritt 2: INSTRUMENTIEREN

Fuer jede Hypothese Messpunkte definieren BEVOR Code veraendert wird:

| Plattform | Methode |
|-----------|---------|
| Android/Kotlin | `Log.d("DEBUG", "Wert: $variable")` → Logcat |
| Web/TypeScript | `console.log("Punkt X:", variable)` → DevTools |
| CLI/Python | `print(f"DEBUG: {variable}")` |
| C#/WPF | `Debug.WriteLine($"DEBUG: {variable}")` |
| iOS/Swift | `print("DEBUG: \(variable)")` |

### Schritt 3: RUNTIME-DATEN ANALYSIEREN

- Welche Hypothese bestaetigt? Welche widerlegt?
- Unerwartete Werte → neue Hypothese?
- Max 2 Runden. Danach: Minimal-Reproduktions-Fall anfordern.

### Schritt 4: GEZIELTER FIX

- Basiert auf echten Laufzeitdaten, nicht Vermutungen
- Kommentar: `// Fix: [was in den Logs beobachtet wurde]`
- Debug-Logging nach Fix ENTFERNEN

### Wann NICHT noetig

- Tippfehler, falscher Variablenname (Root Cause sofort sichtbar)
- Fehlender Import (Compiler-Fehler ist eindeutig)
- Fehler den der Benutzer direkt erklaert

**Faustregel:** Root Cause nach 30 Sekunden noch unklar? → Hypothesen-Loop starten.

### Was NIEMALS passieren darf

- ❌ Fix vorschlagen bevor Laufzeitdaten vorliegen
- ❌ Mehr als 3 Hypothesen (kein Fokus) oder mehr als 2 Runden ohne Daten
- ❌ Hypothese ohne konkrete Funktion ("irgendwo im State")
- ❌ Debug-Logging im Code lassen nach dem Fix
