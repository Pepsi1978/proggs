# Suchstrategie & Agent-Scope: Grep/Glob + Dateigroesse (KRITISCH)

> Semantische Suche (code-search MCP) ist im CLI **abgeschafft** — Code nur ueber Grep/Glob (semantisch nur noch server-seitig im Second Brain).

## 1. Such-Reflex — Grep/Glob

**Grep** = exakter Text/Regex (Symbolname, String, Fehlercode), **Glob** = Dateimuster. Bei erfolglosen
Greps das Muster VERBREITERN (Wortstamm, case-insensitive) statt aufzugeben. Verlustfrei: Grep breit
zuerst (`output_mode: count`/`files_with_matches`), DANN gezielt `content` nur fuer relevante Treffer.

## 2. Agent-Scope nach Dateigroesse (Anti-Crash)

**Datei >500 Zeilen → NIE per Agent editieren** — direkt Grep + Read (mit Ranges) + Edit. (Vorfall:
5 Agents auf 3000-Zeilen-Datei alle abgestuerzt, 50 Min weg; Grep+Edit: <1 Min.) Agents OK bei:
<500 Zeilen, reiner Recherche, parallelen kleinen Dateien. Grosse Dateien NIE komplett in den Kontext
lesen (Read-Tool blockt bei 256 KB) — per Python (`open/read/write`) ODER Grep + Read mit `offset`/`limit`.

## 3. Batch-Schwelle

Bis ~20 gleichartige Stellen einzeln per Grep+Edit; ab ~20 Python-Batch-Script (siehe `agent-and-researcher-rules.md` §3).

## Was NIEMALS

Agent auf Datei >500 Zeilen zum Editieren ansetzen · grosse Datei komplett in den Kontext lesen statt
per Range/Python · nach 2-3 erfolglosen Greps aufgeben statt das Muster zu verbreitern.
