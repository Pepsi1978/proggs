# Meta-Kognition & Planung: Denken bevor Handeln (KRITISCH)

> Konsolidiert aus: tool-planning, trae-ensemble-reasoning,
> intent-tracking, spec-first

---

## 1. Tool-Planung vor komplexen Ketten (KRITISCH)

> Quelle: Superintelligenz Finding 2 — ToolTree, ICLR 2026, arXiv 2603.12740

**Regel:** Bei >3 sequentiellen Tool-Aufrufen MUSS vorher ein Plan formuliert werden.

### Format (dem Benutzer sichtbar, 1-2 Zeilen)

```
Plan: [1-2 Saetze was erreicht werden soll]
Schritte: [Tool 1] → [Tool 2] → [Tool 3] → ...
```

**Ausnahme:** Rein parallele Tool-Aufrufe (alle im gleichen Antwortblock) brauchen KEINEN Plan.

### Post-Check nach jedem Tool

- "Hat das Ergebnis geliefert was ich erwartet habe?"
- Wenn NEIN: Plan anpassen BEVOR der naechste Schritt kommt
- NICHT blind weitermachen wenn ein Schritt fehlgeschlagen ist

### Wann NICHT planen

- Einzelne Tool-Aufrufe, einfache 2-3-Schritt-Ketten
- Nur bei >3 Tools ODER komplexen/mehrdeutigen Aufgaben

### Zusammenspiel

- **Intent-Tracking** (Abschnitt 3): Tool-Plan = Verfeinerung des Session-Ziels
- **Confidence-Ampel** (debugging-and-verification.md): Unsicheres Tool-Ergebnis → Plan anpassen
- **Pheromon-Tabelle** (intelligence-system.md): Erfolgreichen Plan als Muster eintragen

---

## 2. Ensemble-Reasoning: 3 Loesungswege evaluieren

> Quelle: Trae Agent, arXiv 2507.23370

Bei komplexen Aufgaben (Refactoring, tiefgreifende Bugfixes) MUSS der 3-Stufen-Loop angewendet werden:

### Stufe 1: Generation — 3 unterschiedliche Loesungswege

- **Pfad A**: Minimaler Fix
- **Pfad B**: Architektur-konformer Umbau
- **Pfad C**: Abstraktion/Refactoring

### Stufe 2: Pruning — Syntaktische & Konventionelle Filterung

Jeden Pfad evaluieren gegen:
- **Syntax**: Verursacht Compile-Fehler oder Lint-Warnungen?
- **Konventionen**: Entspricht den Projekt-Regeln?
- **Risiko**: Seiteneffekte in anderen Modulen?

### Stufe 3: Selection — Globale Repository-Analyse

Besten Pfad waehlen und kurz begruenden:
"Pfad B gewaehlt, da er die bestehende Architektur nutzt statt einen neuen Wrapper einzufuehren."

### Wann Pflicht

- Wenn >3 Dateien gleichzeitig geaendert werden muessen
- Wenn der Debugger keine eindeutige Root Cause findet
- Wenn der Architect neue Komponenten entwirft

---

## 3. Intent-Tracking: Session-Ziel im Blick behalten

### Ziel-Datei

| Plattform | Pfad |
|-----------|------|
| macOS | `$TMPDIR/claude-session-goal.md` |
| Windows | `$env:TEMP/claude-session-goal.md` |
| Linux | `/tmp/claude-session-goal.md` |

### Metacognitive Self-Monitoring

Alle ~10 Tool-Calls mental pruefen: **"Arbeite ich noch am urspruenglichen Ziel?"**

Nach jeder abgeschlossenen Teil-Aufgabe: 1-Satz-Review:
"Ziel war X. Aktueller Stand: Y erledigt, Z steht noch aus."

Bei Abdrift: Benutzer informieren und zuruecklenken.

---

## 4. Spec-First: Spezifizieren vor Codieren

Fuer nicht-triviale Features (mehr als einfacher Bugfix):

### Vor dem Code: Spec nach `/tmp/current-spec.md`

1. **Invarianten**: Was muss IMMER wahr sein?
2. **Vorbedingungen**: Was muss der Aufrufer garantieren?
3. **Nachbedingungen**: Was garantiert die Funktion?
4. **Edge Cases**: Was wird explizit NICHT unterstuetzt?

### Tests kommen aus Specs, nicht aus Code

Der Tester liest `/tmp/current-spec.md` und generiert Tests die INVARIANTEN verifizieren.

### Wann ueberspringen

- Bugfixes unter 10 Zeilen
- Config/Settings-Aenderungen
- Dokumentation, Version-Bumps

**Warum:** Code der Tests besteht kann trotzdem das FALSCHE Problem loesen.
Specs erzwingen Klaerung von WAS bevor WIE. (Paper: arXiv 2511.17330 — AutoRocq)
