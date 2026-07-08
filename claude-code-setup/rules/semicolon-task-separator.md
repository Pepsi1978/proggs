# Semikolon-Trenner für mehrere Aufgaben (KRITISCH)

> ` ; ` trennt MEHRERE Aufgaben. **Detail:
> `claude-code-setup/docs/rules/semicolon-task-separator.md`.**

## Erkennung

Prompt am ` ; ` splitten; **Anzahl Aufgaben = nicht-leere Teile** (abschließendes ` ; ` zählt nicht).
Nur `Leerzeichen+Semikolon+Leerzeichen` — Semikola in Code/SQL sind KEINE Trenner. Pre/Post-Prompt-
Blöcke aussortieren.

## Die 7-Schritte-Pipeline

1. **ERKENNEN**. 2. **SORTIEREN**: Gruppieren, Abhängigkeiten, optimale
Reihenfolge; **Konflikt** gleicher Stelle → STOP + nachfragen. 3. **ANZEIGEN** ab 2 Aufgaben:
**TaskCreate-Liste PFLICHT**. 4. **ABARBEITEN** (sequenziell, KEINE Subagents): je Aufgabe `in_progress
→ umsetzen → commit+push → Commit-Marker → abhaken`; nur EIGENE Dateien namentlich.
5. **BAUEN** nur EINMAL nach der letzten. 6. **INSTALLIEREN** nur EINMAL. 7. **VERIFIZIEREN**: jede
gegen Ergebnis, Untergegangenes nachholen, alle `completed` → Abschluss-Boxen. Schritt 5+6 nur bei
App; 1-4+7 IMMER.

## Commit-Marker (PFLICHT)

Nach Commit+Push: Linie 80×`━`, `💾 Aufgabe N: <Kurz> — committed und gepusht`, Linie 80×`━`.

## Was NIEMALS
- Multi-Task als eine Aufgabe missverstehen oder eine in der Mitte überspringen · bei 2+ Aufgaben keine
  TaskCreate-Liste oder Subagents nutzen · Tasks gesammelt abhaken · Commit-Marker weglassen · `git add
  -A`/`.` · nach JEDER Aufgabe bauen statt einmal · Semikola in Code als Trenner werten.
