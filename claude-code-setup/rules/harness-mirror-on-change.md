# Harness-Spiegelung bei JEDER Aenderung ins Repo (KRITISCH)

> Gilt automatisch in jeder Session. Der aktive Ort (`~/.claude/`) liegt NICHT im Repo — ohne
> Spiegelung hat keine andere Plattform/kein anderer Rechner den neuen Stand.

## Grundprinzip

Wird ein Skill/Plugin/Hook/MCP-Server/Agent/Command/Rule/Setting neu gebaut oder geaendert, wird der
aktuelle Stand SOFORT 1:1 in ZWEI Spiegelorte gespiegelt (beide bei jeder Aenderung nachziehen):

1. **`~/proggs/claude-code-setup/`** — granular pro Komponententyp.
2. **`~/proggs/Umgebung/`** — transportables Komplett-Paket (Skills/Hooks/Plugins). **Wird leicht vergessen.**

Die Spiegelung ist Teil der "fertig"-Definition — nicht fertig, bevor beide Spiegelorte aktuell + gepusht sind.

## Mapping (Aktiv → Spiegel 1 → Spiegel 2)

| Komponente | Aktiv (nicht im Repo) | `claude-code-setup/` | `Umgebung/` |
|------------|------------------------|----------------------|-------------|
| Skills | `~/.claude/skills/<name>/` | `skills/<name>/` | `Skills/<name>/` |
| Hooks (`.ps1`+`.sh`) | `~/.claude/hooks/` | `hooks/` | `Hooks/` |
| Plugins (eigene) | Plugin-Quelle | `Plugins/` | `Plugins/` |
| Agents | `~/.claude/agents/<name>.md` | `agents/` | — |
| Commands | `~/.claude/commands/<name>.md` | `commands/` | — |
| Rules | `~/.claude/rules/<name>.md` | `rules/` | — |
| MCP-Config | `settings.json` / `.mcp.json` | `mcp-windows.json` + `mcp-macos.json` | — |
| Settings | `settings.json`/`.local.json` | 3-Dateien-Regel | — |

`Umgebung/` = nur Skills/Hooks/Plugins. Rules/Agents/Commands/MCP/Settings nur in `claude-code-setup/`.

## Ablauf

1. Aenderung am aktiven Ort fertig. 2. Spiegel 1 aktualisieren (bei Hooks BEIDE `.ps1`+`.sh`;
Settings 3-Dateien-Regel). 3. Spiegel 2 aktualisieren (Skills/Hooks/Plugins — nicht vergessen).
4. Bei Unsicherheit `diff` (wirklich 1:1?). 5. Nur eigene Dateien committen → fetch+rebase → push.
6. Status: "Committed, gepusht und plattformuebergreifend."

## Beim Spiegeln beachten

`__pycache__` ausschliessen · leeren `skills/learned/` ignorieren · LF + UTF-8 ohne BOM ·
`.mcp.json` plattformspezifisch (NIE vereinheitlichen) · nur eigene Dateien · externe/installierte
Plugins NICHT spiegeln.

## KEINE Spiegel (separate CLI-Welten)

`~/proggs/gemini-setup/` und `~/proggs/codex-setup/` — eigene Logik, bei Claude-Aenderungen NICHT anfassen.

## Was NIEMALS passieren darf

- Harness aendern ohne BEIDE Spiegelorte nachzuziehen · nur `claude-code-setup/` und `Umgebung/` vergessen
- Bei einem Hook nur `.ps1` ODER nur `.sh` spiegeln · veraltete/gekuerzte Kopie statt 1:1
- Spiegelung auf spaeter verschieben oder uncommitted lassen · `__pycache__`/`learned/` mitspiegeln
- `.mcp.json` vereinheitlichen · gemini-setup/codex-setup als Claude-Spiegel mitpflegen
