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

Vor einer SEQUENTIELLEN Kette mit >3 Tools kurz notieren (dem Benutzer sichtbar, 1-2 Zeilen):
```
Plan: [1-2 Saetze was erreicht werden soll]
Schritte: [Tool 1] → [Tool 2] → [Tool 3] → ...
```

**Ausnahme**: Rein parallele Tool-Aufrufe (alle im gleichen Antwortblock, z.B. 4x Read gleichzeitig)
brauchen KEINEN Plan — Parallelisierung ist immer erwuenscht und soll nicht gebremst werden.

## Post-Check nach jedem Tool

Nach jedem Tool-Aufruf in der Kette eine 1-Satz-Pruefung:
- "Hat das Ergebnis geliefert was ich erwartet habe?"
- Wenn NEIN: Plan anpassen BEVOR der naechste Schritt kommt.
- NICHT blind weitermachen wenn ein Schritt fehlgeschlagen ist.

## Wann NICHT planen

- Einzelne Tool-Aufrufe (Read, Grep, Glob) → kein Plan noetig
- Einfache 2-3-Schritt-Ketten → kein Plan noetig
- Nur bei >3 Tools ODER wenn die Aufgabe komplex/mehrdeutig ist

## Konkrete Beispiele

### Beispiel 1 — GUTER Plan (Bug-Fix in Android-App)
```
Plan: Build-Fehler in SplashScreen.kt fixen — fehlender Import.
Schritte: Read(SplashScreen.kt) → Grep(fehlende Klasse) → Edit(Import hinzufuegen) → Bash(gradlew build)
Reihenfolge: Erst die Datei lesen um den Fehler zu sehen, dann den fehlenden Import finden, einfuegen, und Build pruefen.
```
Post-Check nach Read: "Ja, ich sehe den fehlenden Import in Zeile 15."
Post-Check nach Grep: "Gefunden: Die Klasse liegt in androidx.compose.foundation."
Post-Check nach Edit: "Import eingefuegt. Jetzt Build pruefen."
Post-Check nach Bash: "Build erfolgreich — Fehler behoben."

### Beispiel 2 — SCHLECHTER Ablauf (ohne Plan)
```
Read(SplashScreen.kt) → Read(Theme.kt) → Read(Navigation.kt) → Grep("import") →
Read(build.gradle) → Edit(SplashScreen.kt) → Bash(build) → FEHLER → Read(SplashScreen.kt) →
Edit(SplashScreen.kt) → Bash(build) → OK
```
10 Tool-Calls statt 4 — weil ohne Plan ziellos gesucht wurde.

### Beispiel 3 — Wann KEIN Plan noetig ist
```
Benutzer: "Was steht in der README?"
→ Read(README.md)  ← 1 Tool, kein Plan noetig
```

## Zusammenspiel mit anderen Regeln

- **Intent-Tracking** (`intent-tracking.md`): Der Tool-Plan ist eine Verfeinerung
  des Session-Ziels auf Aufgaben-Ebene.
- **Metacognitive Self-Monitoring**: Der Post-Check nach jedem Tool ist eine
  Mikro-Version der alle-5-Tool-Calls-Pruefung.
- **Confidence-Ampel** (`confidence-check.md`): Wenn ein Tool-Ergebnis unsicher ist
  (gelb/rot), den Plan anpassen und nachschlagen statt weitermachen.
- **Pheromon-Tabelle** (`swarm-success-tracking.md`): Nach einer komplexen Aufgabe
  den erfolgreichen Plan als Muster in die Tabelle eintragen.
