# Codex CLI übernimmt stillschweigend die Claude-Code-Umgebung

Festgehalten am 10.09.2026, 11:02 Uhr. CLI 0.153.2 → 0.154.0.

## Falle

Ein frisch installiertes Codex CLI fragt beim ersten Start, ob man **68 Hooks** vertrauen will. Erwartet wurde eine jungfräuliche Umgebung ohne einen einzigen eigenen Hook.

## Ursache

`~/.codex` ist kein leeres Zuhause. Dort lagen:

- `hooks.json` mit 30 registrierten Hook-Aufrufen und 62 PowerShell-Skripten in `~/.codex/hooks/` — **byte-identische Kopien** der Claude-Code-Hooks aus `~/.claude/hooks/` (geprüft mit `cmp`: `bash-guard.ps1`, `bug-almanac-guard.ps1`, `redact-secrets.ps1` alle identisch). Die höhere Zahl in der Abfrage entsteht durch zusätzliche Hooks, die die Plugins mitbringen — nicht nachgezählt.
- Rund 40 Plugins, unter anderem aus `claude-plugins-official`.
- Vier MCP-Server.
- Eine 6 KB große globale `AGENTS.md` — inhaltlich die Claude-`CLAUDE.md`, bei der Übernahme wurde stumpf `.claude` durch `.Codex` ersetzt, sodass sie auf nicht existierende Pfade wie `~/.Codex/rules/` verweist.
- Eine angepasste Statuszeile unter `[tui] status_line`.

Wie diese Dateien dorthin kamen, ist nachträglich nicht mehr beweisbar. In `~/.codex/config.toml` steht ein Schalter, der genau das tut:

```toml
external-agent-import-sync-enabled = true
external-agent-import-sync-item-types = "all"
```

Damit importiert die Codex-Desktop-App fremde Agenten-Konfiguration automatisch und ohne Rückfrage. Möglich ist aber auch eine frühere eigene Sitzung: die globale `AGENTS.md` ist die Claude-`CLAUDE.md` mit einem stumpfen Suchen-Ersetzen `claude` → `Codex`, was eher nach einem eigenen Skript aussieht als nach OpenAI. Für die Lösung ist die Herkunft gleichgültig — ein eigenes `CODEX_HOME` sieht beides nicht.

## Wirkung

Codex lädt die globale `AGENTS.md` **zusätzlich** zur Projekt-`AGENTS.md`. Ein Werkzeug, das ein bestimmtes Profil starten will, bekommt also fremde Regeln obendrauf, die es nie gesetzt hat.

## Vorgehen

Für einen kontrollierten Start ein **eigenes `CODEX_HOME`** setzen, statt `~/.codex` zu beschneiden (das würde die Desktop-App treffen, die es wieder befüllt):

```powershell
$env:CODEX_HOME = "$env:LOCALAPPDATA\OpenLauncher\codex-home"
```

In diesem Zuhause:

- `AGENTS.md` leer anlegen — sonst schreibt Codex sich beim ersten Start selbst eine.
- `config.toml` minimal und **nur wenn sie fehlt** anlegen. Codex trägt dort selbst seine `[projects.*]`-Vertrauensstufen ein; bei jedem Neuschreiben käme der Vertrauensdialog zurück.
- `auth.json` aus `~/.codex` kopieren, aber **nur wenn die Quelle neuer ist** — sonst überschreibt ein alter Token einen im eigenen Zuhause erneuerten.

Nachweis mit `codex doctor` bei gesetztem `CODEX_HOME`: `MCP servers 0`, `auth is configured`, `config.toml` aus dem eigenen Pfad, keine Hook-Abfrage.

Was ein eigenes `CODEX_HOME` **nicht** entfernt: die kontogebundenen Remote-Connectors (Gmail, GitHub, Google Drive). Die hängen am ChatGPT-Konto, nicht am lokalen Zuhause.

## Zweite Falle: AGENTS.md-Kette

Codex liest `AGENTS.md` von der Git-Wurzel **bis zum** Arbeitsverzeichnis und hängt die Dateien aneinander. Liegt das Arbeitsverzeichnis in einem Unterordner, gilt eine höher liegende `AGENTS.md` also **vor** dem gewählten Profil — möglicherweise mit dem Profil vom letzten Start. Ein Werkzeug, das nur die Datei im Arbeitsverzeichnis schreibt, hat das Profil damit nicht sicher gesetzt.

Umgesetzt in OpenLauncher (`InstructionProfileService.AlignAncestorCodexAgents`): übergeordnete `AGENTS.md`, die erkennbar vom Launcher stammen, werden auf denselben Text gezogen; fremde bleiben unangetastet.
