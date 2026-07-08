# Suchstrategie & Agent-Scope: Grep/Glob + Dateigroesse (KRITISCH)

> Semantische Suche (code-search MCP) ist im CLI **abgeschafft** — Code wird ausschliesslich ueber
> Grep/Glob gesucht. (Semantische Suche existiert nur noch server-seitig im Second Brain / Vektor-DB.)

## 1. Such-Reflex — Grep/Glob

Code-Suche laeuft ueber **Grep** (exakter Text/Regex: Symbolname, String, Fehlercode) und **Glob**
(Dateimuster). VOR der Suche kurz das Muster praezisieren; bei erfolglosen Greps das Muster
VERBREITERN (Wortstamm, case-insensitive) statt aufzugeben. Reihenfolge verlustfrei: Grep breit zuerst
(`output_mode: count`/`files_with_matches`), DANN gezielt `content` nur fuer die relevanten Treffer
(erst alle finden, dann gezielt lesen).

## 2. Agent-Scope nach Dateigroesse (Anti-Crash)

**Datei >500 Zeilen → NIE per Agent editieren** — direkt Grep + Read (mit Ranges) + Edit. (Vorfall:
5 Agents auf einer 3000-Zeilen-Datei alle abgestuerzt, 50 Min weg; direkt mit Grep+Edit: <1 Min.)
Agents sind OK bei: Dateien <500 Zeilen, reiner Recherche, mehreren parallelen kleinen Dateien. Grosse
Dateien NIE komplett in den Kontext lesen (das Read-Tool blockt ohnehin bei 256 KB) — per Python
(`open/read/write`) bearbeiten ODER Grep + Read mit `offset`/`limit`.

## 3. Batch-Schwelle

Viele gleichartige Aenderungen: bis ~20 Stellen einzeln per Grep+Edit; ab ~20 Stellen Python-Batch-Script
(siehe `agent-and-researcher-rules.md` §3).

## Was NIEMALS

Einen Agent auf eine Datei >500 Zeilen zum Editieren ansetzen · eine grosse Datei komplett in den
Kontext lesen statt per Range/Python · nach 2-3 erfolglosen Greps aufgeben statt das Muster zu verbreitern.
