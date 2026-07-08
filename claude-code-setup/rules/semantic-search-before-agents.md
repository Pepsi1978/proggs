# Suchstrategie: Semantische Suche vs. Grep vs. Agents (KRITISCH)

> Wird immer geladen + per `subagent-context`-Hook in jeden Subagent injiziert.

## Such-Reflex (VOR jeder Code-Suche 1 Satz)

> Kenne ich den exakten Namen/String/Regex? → **Grep/Glob**.
> Nur das Konzept, oder "welche Datei betrifft das ueberhaupt?" → **semantische Suche** (`code-search` MCP).

- Multi-Task-Start: erst semantisch orientieren (welche Dateien?), dann Grep fuer die genaue Zeile.
- Nach 2-3 erfolglosen Greps → semantisch wechseln. Datei >500 Zeilen NICHT per Agent editieren.

**Sichtbare Ansage (PFLICHT):** Bei JEDEM `code-search`-Aufruf zuerst die Zeile ausgeben:
`🔍 Semantische Suche — [kurzer Grund]`.

## Kernmissverstaendnis

Grep ist KEINE semantische Suche. Zwei verschiedene Werkzeuge:

| Werkzeug | Was | Wann |
|----------|-----|------|
| **Grep/Glob** | exakter Text/Muster (Name, String, Regex) | du WEISST wonach |
| **Semantische Suche** (`code-search` MCP) | Konzepte ueber Bedeutung (Embeddings) | du kennst nur das KONZEPT |

## Zwei unabhaengige Achsen

- **Achse A — Wer arbeitet? (Dateigroesse):** Datei >500 Zeilen → NIE Agent zum Editieren, direkt
  Such-Tool + Read + Edit. (Vorfall: 5 Agents auf 3000-Zeilen-Datei alle abgestuerzt, 50 Min weg;
  direkt mit Grep+Edit: <1 Min.) Agents OK bei <500 Zeilen, reiner Recherche, parallelen kleinen Dateien.
- **Achse B — Welches Suchwerkzeug? (Name vs. Konzept):** exakter Symbolname/String/Fehlercode/Regex/
  Rename/Suche in bekannter Datei → **Grep/Glob**. Nur Konzept/"wo ist X?"/unbekannter Code/nach 2-3
  erfolglosen Greps → **semantische Suche**.

## Zwei Phasen (Herzstueck)

| Phase | Frage | Werkzeug |
|-------|-------|----------|
| 1 — Orientierung | "Welche Dateien betrifft das?" | semantische Suche |
| 2 — Praezision | "Welche Zeile aendere ich?" | Grep + Read + Edit |

Reflex "erst mit Grep die Bereiche raten" ist in Phase 1 oft langsamer — semantisch findet die Dateien
in EINEM Aufruf.

## Schwellen & Batch

Agent ja/nein: 500 Zeilen · Grep→semantisch: nach 2-3 erfolglosen Greps · viele gleichartige
Aenderungen: bis 20 Stellen Grep+Edit, ab 20 Python-Batch-Script.

## Was NIEMALS passieren darf

- Grep "semantische Suche" nennen · Agent auf Datei >500 Zeilen zum Editieren
- Semantische Suche fuer exakte Symbolnamen/Strings/Regex/Rename oder INNERHALB einer bekannten Datei
