# Harness-Spiegelung bei JEDER Aenderung ins Repo (KRITISCH)

> Der aktive Ort (`~/.claude/`) liegt NICHT im Repo — ohne Spiegelung hat keine andere Plattform den neuen Stand.

## Grundprinzip

Wird ein Skill/Plugin/Hook/MCP/Agent/Command/Rule/Setting geaendert, wird der Stand SOFORT in ZWEI
Spiegelorte gespiegelt (beide bei jeder Aenderung): **`claude-code-setup/`** (granular pro Typ) +
**`Umgebung/`** (transportables Paket, nur Skills/Hooks/Plugins — wird leicht vergessen). Teil der
"fertig"-Definition — nicht fertig, bevor beide aktuell + gepusht sind.

## Mapping (Aktiv → Spiegel)

| Komponente | Aktiv | `claude-code-setup/` | `Umgebung/` |
|------------|-------|----------------------|-------------|
| Skills | `~/.claude/skills/<n>/` | `skills/<n>/` | `Skills/<n>/` |
| Hooks (`.ps1`+`.sh`) | `~/.claude/hooks/` | `hooks/` | `Hooks/` |
| Plugins (eigene) | Plugin-Quelle | `Plugins/` | `Plugins/` |
| Agents/Commands/Rules | `~/.claude/{agents,commands,rules}/` | gleichnamig | — |
| MCP/Settings | `settings.json`/`.mcp.json` | `mcp-*.json` + 3-Dateien-Regel | — |

Rules/Agents/Commands/MCP/Settings nur in `claude-code-setup/` (nicht `Umgebung/`).

## Beim Spiegeln beachten

`__pycache__`/leeren `learned/` ausschliessen · LF + UTF-8 ohne BOM · `.mcp.json` plattformspezifisch
(NIE vereinheitlichen) · nur eigene Dateien · externe/installierte Plugins NICHT spiegeln. KEINE Spiegel:
`gemini-setup/`, `codex-setup/` (eigene CLI-Welten, bei Claude-Aenderungen NICHT anfassen).

## Was NIEMALS

- Harness aendern ohne BEIDE Spiegelorte · `Umgebung/` vergessen · bei einem Hook nur `.ps1` ODER `.sh`
  spiegeln · veraltete/gekuerzte Kopie statt 1:1 · Spiegelung auf spaeter verschieben · `.mcp.json` vereinheitlichen.
