# Semikolon-Trenner für mehrere Aufgaben (KRITISCH)

> ` ; ` (Leerzeichen-Semikolon-Leerzeichen) trennt MEHRERE eigenständige Aufgaben (Franks Voice-Overlay
> hängt es an). **Detail (Commit-Marker-Beispiel, Pre/Post-Prompts):
> `claude-code-setup/docs/rules/semicolon-task-separator.md`.**

## Erkennung

Prompt am ` ; ` splitten. **Anzahl Aufgaben = nicht-leere Teile** (ein abschließendes ` ; ` zählt nicht).
Nur `Leerzeichen+Semikolon+Leerzeichen` — Semikola in Code/SQL (`const x = 5;`) sind KEINE Trenner.
Pre/Post-Prompt-Blöcke aussortieren (keine Aufgaben).

## Die 7-Schritte-Pipeline

1. **ERKENNEN** (splitten). 2. **SORTIEREN** (Pre-Flight ~10 s: Gruppieren, Abhängigkeiten, optimale
Reihenfolge; **Konflikt** zweier Aufgaben an derselben Stelle → STOP + nachfragen). 3. **ANZEIGEN** (ab 2
Aufgaben: Übersicht + **TaskCreate-Liste PFLICHT**). 4. **ABARBEITEN** (sichtbar, sequenziell, KEINE
Subagents): pro Aufgabe `in_progress → umsetzen → committen+pushen → Commit-Marker → sofort abhaken`. Nur
EIGENE Dateien `git add <pfad>`, NIE `-A`/`.`. 5. **BAUEN** (nur EINMAL, nach der letzten, baubare App).
6. **INSTALLIEREN** (nur EINMAL). 7. **VERIFIZIEREN** (End-Check: jede Aufgabe gegen Ergebnis,
Untergegangenes nachholen; alle `completed`? → Abschluss-Boxen). Schritt 5+6 nur bei App; 1-4 + 7 IMMER.

## Commit-Marker pro committender Aufgabe (PFLICHT)

Direkt nach Commit+Push: Linie 80×`━`, Zeile `💾 Aufgabe N: <Kurz> — committed und gepusht`, Linie 80×`━`.

## Was NIEMALS
- Multi-Task als eine Aufgabe missverstehen oder eine in der Mitte überspringen · bei 2+ Aufgaben keine
  TaskCreate-Liste oder Subagents nutzen · Tasks gesammelt abhaken · Commit-Marker weglassen · `git add
  -A`/`.` · nach JEDER Aufgabe bauen statt einmal · Semikola in Code als Trenner werten.
