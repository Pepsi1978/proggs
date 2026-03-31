# Tool-Planung: Plan vor komplexen Werkzeug-Ketten (KRITISCH)

> Quelle: Superintelligenz Finding 2 — ToolTree (ICLR 2026, arXiv 2603.12740)
> Direktive: #1 Superintelligenz

## Regel

Bei Aufgaben die voraussichtlich MEHR ALS 3 Tool-Aufrufe erfordern,
MUSS vor dem ersten Tool-Aufruf ein kurzer Plan formuliert werden.

## Warum

Ohne Plan greift Claude spontan zum naechsten Werkzeug — liest 5 Dateien,
merkt dann dass er die falsche gesucht hat, und faengt von vorne an.
Das ToolTree-Paper (ICLR 2026) zeigt: 10% bessere Performance durch
einfache Pre/Post-Checks bei Tool-Einsaetzen, ohne groesseres Modell.

## Format des Plans (vor komplexen Tool-Ketten)

Vor einer Kette mit >3 Tools kurz notieren (intern, nicht dem Benutzer zeigen):
```
Plan: [1-2 Saetze was erreicht werden soll]
Schritte: [Tool 1] → [Tool 2] → [Tool 3] → ...
Reihenfolge weil: [1 Satz Begruendung]
```

## Post-Check nach jedem Tool

Nach jedem Tool-Aufruf in der Kette eine 1-Satz-Pruefung:
- "Hat das Ergebnis geliefert was ich erwartet habe?"
- Wenn NEIN: Plan anpassen BEVOR der naechste Schritt kommt.
- NICHT blind weitermachen wenn ein Schritt fehlgeschlagen ist.

## Wann NICHT planen

- Einzelne Tool-Aufrufe (Read, Grep, Glob) → kein Plan noetig
- Einfache 2-3-Schritt-Ketten → kein Plan noetig
- Nur bei >3 Tools ODER wenn die Aufgabe komplex/mehrdeutig ist

## Zusammenspiel mit anderen Regeln

- **Intent-Tracking** (`intent-tracking.md`): Der Tool-Plan ist eine Verfeinerung
  des Session-Ziels auf Aufgaben-Ebene.
- **Metacognitive Self-Monitoring**: Der Post-Check nach jedem Tool ist eine
  Mikro-Version der alle-5-Tool-Calls-Pruefung.
