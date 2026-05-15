# ÔÜí Trae Ensemble-Reasoning & Test-time Scaling

Diese Regel formalisiert den 3-Stufen-Loop des Trae Agents (arXiv:2507.23370), um Kontext-Rauschen bei komplexen Repository-Fixes zu minimieren.

## Der 3-Stufen-Loop

Bei komplexen Aufgaben (z. B. Refactoring, tiefgreifende Bugfixes) MUSS der Agent folgendes Protokoll anwenden:

### 1. Generation (N L├Âsungsvorschl├ñge)
- Erstelle gedanklich oder explizit **3 unterschiedliche L├Âsungswege** (Pfad A, B und C).
- Variiere dabei die Ans├ñtze (z. B. Pfad A: Minimaler Fix, Pfad B: Architektur-konformer Umbau, Pfad C: Abstraktion/Refactoring).

### 2. Pruning (Syntaktische & Konventionelle Filterung)
- Evaluiere jeden Pfad gegen:
  - **Syntax**: Verursacht dieser Pfad Compile-Fehler oder Lint-Warnungen?
  - **Konventionen**: Entspricht der Pfad den Projekt-Regeln in `GEMINI.md`?
  - **Risiko**: Verursacht der Pfad potenziell Seiteneffekte in anderen Modulen?
- Verwerfe Pfade, die diese Kriterien nicht erf├╝llen.

### 3. Selection (Globale Repository-Analyse)
- W├ñhle den Pfad aus, der am besten zur bestehenden Codebase passt.
- Begr├╝nde die Wahl kurz: "Pfad B gew├ñhlt, da er die bestehende Architektur nutzt statt einen neuen Wrapper einzuf├╝hren."

## Wann ist dieser Loop Pflicht?
- Wenn mehr als 3 Dateien gleichzeitig ge├ñndert werden m├╝ssen.
- Wenn der `debugger` Agent keine eindeutige Root Cause findet.
- Wenn der `architect` Agent neue Komponenten entwirft.

## Ziel
Minimierung von Fehlversuchen und Reduzierung von Kontext-├£berlastung durch "Trial-and-Error".
