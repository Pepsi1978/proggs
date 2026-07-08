# Harness-Spiegelung bei JEDER Aenderung ins Repo (KRITISCH)

> `~/.claude/` liegt NICHT im Repo — ohne Spiegelung hat keine andere Plattform den neuen Stand.

## Grundprinzip

Geaenderter Skill/Plugin/Hook/MCP/Agent/Command/Rule/Setting wird SOFORT in ZWEI Spiegelorte gespiegelt:
**`claude-code-setup/`** (granular pro Typ) + **`Umgebung/`** (nur Skills/Hooks/Plugins, leicht
vergessen). Nicht fertig, bevor beide aktuell + gepusht.

## Mapping (Aktiv → Spiegel)

| Komponente | Aktiv | `claude-code-setup/` | `Umgebung/` |
|------------|-------|----------------------|-------------|
| Skills | `~/.claude/skills/<n>/` | `skills/<n>/` | `Skills/<n>/` |
| Hooks (`.ps1`+`.sh`) | `~/.claude/hooks/` | `hooks/` | `Hooks/` |
| Plugins (eigene) | Plugin-Quelle | `Plugins/` | `Plugins/` |
| Agents/Commands/Rules | `~/.claude/{agents,commands,rules}/` | gleichnamig | — |
| MCP/Settings | `settings.json`/`.mcp.json` | `mcp-*.json` + 3-Dateien-Regel | — |

## Beim Spiegeln beachten

`__pycache__`/leeren `learned/` ausschliessen · LF + UTF-8 ohne BOM · `.mcp.json` plattformspezifisch (NIE
vereinheitlichen) · nur eigene Dateien · externe Plugins NICHT spiegeln. KEINE Spiegel: `gemini-setup/`,
`codex-setup/` (eigene CLI-Welten, NICHT anfassen).

## Was NIEMALS
- Harness aendern ohne BEIDE Spiegelorte · `Umgebung/` vergessen · bei einem Hook nur `.ps1` ODER `.sh`
  spiegeln · veraltete/gekuerzte Kopie statt 1:1 · Spiegelung auf spaeter verschieben · `.mcp.json` vereinheitlichen.
