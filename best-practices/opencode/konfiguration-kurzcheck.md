# Konfigurationsdateien & vollständiges Schema Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Config-Format | `opencode.json` / `opencode.jsonc` (JSONC = `//`-Kommentare + Trailing Commas); TUI getrennt in `tui.json`; `$schema`-Feld setzen (Autovervollständigung/Validierung) | §1 |
| 2 | Präzedenz | Configs werden GEMERGED (nicht ersetzt); spätere Quelle gewinnt nur für den Konflikt-Key: global → `OPENCODE_CONFIG` → Projekt → `.opencode/` → managed | §2 |
| 3 | Windows-Pfad-Falle | Doku nennt `~/.config/opencode/`, real nutzen manche Komponenten `%APPDATA%`/`%LOCALAPPDATA%` → VOR dem Anlegen `opencode debug paths` ausführen | §2 |
| 4 | Unterordner im Plural | `agents/`, `commands/`, `plugins/`, `skills/`, `tools/`, `themes/` (Singular nur noch abwärtskompatibel) | §2 |
| 5 | Variablen-Substitution | `{env:VAR}` für Env-Vars, `{file:path}` für Dateiinhalt → ideal für Secrets / große Instructions | §2 |
| 6 | Modell-Format | überall `provider/model-id` (z.B. `anthropic/claude-sonnet-4-5`); `small_model` separat für Leichtgewicht-Tasks; Auswahl-Priorität: `--model` > Config > zuletzt benutzt | §3, §4 |
| 7 | Permission-Block | `allow`/`ask`/`deny`; LETZTE passende Regel gewinnt → Catch-all `"*"` zuerst; `edit` deckt write/edit/patch; `.env` per Default `read:deny` lassen | §6 |
| 8 | Custom Provider / lokale Modelle | `provider.<id>` mit `npm:"@ai-sdk/openai-compatible"` + `options.baseURL` (Ollama/LM Studio); bei Custom-Providern `limit.context`/`.output` SELBST setzen | §5 |
| 9 | Secrets NIE im Klartext | API-Keys via `{env:VAR}` / `{file:~/.secrets/...}` oder `/connect` (außerhalb des Repos); Projekt-`opencode.json` + `AGENTS.md` ins Git committen | §13 |
| 10 | Deprecated meiden | `mode`→`agent`, `autoshare`→`share`, `tools`(an/aus)→`permission`, `maxSteps`→`steps`, Top-Level `theme`/`keybinds`→`tui.json` | §Deprecated |
| 11 | Große Repos / MCP | `snapshot:false` spart Disk (Preis: kein UI-Undo); MCP sparsam (per Agent statt global — frisst Tokens in jeder Anfrage) | §13 |
