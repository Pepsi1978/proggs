# Trae Ensemble-Reasoning & Test-time Scaling
# Adapted from MIRROR-2026-03-27-GEM-002 by import agent on 2026-03-27

Diese Regel formalisiert den 3-Stufen-Loop des Trae Agents (arXiv:2507.23370), um Kontext-Rauschen bei komplexen Repository-Fixes zu minimieren.

## Der 3-Stufen-Loop

Bei komplexen Aufgaben (z. B. Refactoring, tiefgreifende Bugfixes) MUSS der Agent folgendes Protokoll anwenden:

### 1. Generation (N Loesungsvorschlaege)
- Erstelle gedanklich oder explizit **3 unterschiedliche Loesungswege** (Pfad A, B und C).
- Variiere dabei die Ansaetze (z. B. Pfad A: Minimaler Fix, Pfad B: Architektur-konformer Umbau, Pfad C: Abstraktion/Refactoring).

### 2. Pruning (Syntaktische & Konventionelle Filterung)
- Evaluiere jeden Pfad gegen:
  - **Syntax**: Verursacht dieser Pfad Compile-Fehler oder Lint-Warnungen?
  - **Konventionen**: Entspricht der Pfad den Projekt-Regeln in CLAUDE.md?
  - **Risiko**: Verursacht der Pfad potenziell Seiteneffekte in anderen Modulen?
- Verwerfe Pfade, die diese Kriterien nicht erfuellen.

### 3. Selection (Globale Repository-Analyse)
- Waehle den Pfad aus, der am besten zur bestehenden Codebase passt.
- Begruende die Wahl kurz: "Pfad B gewaehlt, da er die bestehende Architektur nutzt statt einen neuen Wrapper einzufuehren."

## Wann ist dieser Loop Pflicht?
- Wenn mehr als 3 Dateien gleichzeitig geaendert werden muessen.
- Wenn der `debugger` Agent keine eindeutige Root Cause findet.
- Wenn der `architect` Agent neue Komponenten entwirft.

## Ziel
Minimierung von Fehlversuchen und Reduzierung von Kontext-Ueberlastung durch "Trial-and-Error".
