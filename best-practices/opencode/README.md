# OpenCode CLI — Best-Practices-Wissensbasis (Index)

> **Stand: 2026-06-18.** Recherchiert mit 7 parallelen Researchern (offizielle Quellen zuerst:
> `opencode.ai/docs`, `openrouter.ai/docs`, GitHub `anomalyco/opencode` (früher `sst/opencode`),
> `agents.md`). Jeder Eintrag in den Dateien ist mit Quelle + `offiziell`/`extern`-Flag versehen.
> Plattformen: **Windows (nativ + WSL) und macOS** gleichermaßen abgedeckt.

**Worum es geht:** OpenCode ist ein quelloffener KI-Coding-Agent fürs Terminal (TUI), provider-agnostisch
(Anthropic, OpenAI, Google, OpenRouter, lokale Modelle). Diese Wissensbasis erklärt, wie man damit am besten
arbeitet, wie man ihn token-arm konfiguriert und wie man Gedächtnis, Agents, Plugins, MCP, Skills und
OpenRouter einrichtet.

## Dateien dieser Sammlung

| Datei | Inhalt |
|-------|--------|
| `grundlagen-installation.md` | Was ist OpenCode, Installation (Windows/macOS), CLI/TUI-Bedienung, Sessions, Slash-Befehle, Tastenkürzel, Windows-Stolperfallen, täglicher Workflow |
| `command-palette.md` | Vollständige Befehlsliste (Strg+P) 1:1: alle englischen Befehle in Anzeige-Reihenfolge mit Gruppen (Suggested·Session·Prompt·Agent·Provider·System·VCS), deutscher Erklärung, Quell-Dateien der Registrierung + Versions-Nuance (reiche `tui`-Route vs. schlanke `run`-Variante) |
| `konfiguration.md` | Alle Config-Dateien, vollständiges `opencode.json`-Schema, Pfade je OS, Variablen-Substitution, Permissions, Beispielkonfigurationen, Secrets |
| `agents-md-memory.md` | AGENTS.md-Standard, `/init`, Regeln/Precedence, CLAUDE.md-Migration, `instructions`-Glob, Gedächtnis/Sessions-Persistenz, Kontext-Management |
| `agents-modes.md` | Primary/Subagents, Plan/Build, Custom Agents (JSON + Markdown), Frontmatter-Felder, Permissions, pro-Agent-Modellwahl |
| `plugins-mcp-skills.md` | MCP-Server, Plugin-System (Hooks/Events), Custom Tools, native Skills (SKILL.md), Custom Slash-Commands, Sicherheit |
| `openrouter.md` | OpenRouter-Setup in OpenCode, Modell-ID-Format, Provider-Routing, Prompt-Caching, günstige+gute Coding-Modelle, Limits/Free/BYOK |
| `lokale-modelle-lmstudio.md` | Lokale Modelle über LM Studio: Mindestkontext 32768 (Systemprompt ~22000), `limit.context` = Wert aus `lms ps`, Speicher-Schutzschranken und wann Entladen unumkehrbar wird, **GGUF statt MLX** (safetensors ignoriert `--context-length`), Varianten am Herausgeber unterscheiden |
| `go-recherche-modelle.md` | OpenCode-Go-Abo (14 Modelle, Stand Juni 2026): Modell-Auswahl-Matrix für die Firecrawl-Recherche-Pipeline (Docs filtern + kritisch hinterfragen). Empfehlung **DeepSeek V4 Pro** (Faktentreue + 1M), V4 Flash als Vorfilter; Bewertung aller 13 Modelle, zwei API-Schemata, Abstain-Pflicht |
| `token-effizienz.md` | Token-armer Harness: alle Spar-Hebel priorisiert, Caching, günstige Modellstrategie, Kosten beobachten, vollständige Beispiel-`opencode.json` |

## Die wichtigsten Kern-Erkenntnisse (Schnellüberblick)

1. **Windows:** OpenCode läuft nativ, aber SST **empfiehlt offiziell WSL** (bessere Dateisystem-Performance,
   volle Terminal-/Tool-Kompatibilität). Auf nativem Windows ist `npm`-Installation fehleranfällig →
   **Scoop oder Chocolatey** nutzen. Pfade verlässlich mit `opencode debug paths` ermitteln.
2. **Konfiguration:** Eine `opencode.json`/`opencode.jsonc` (JSON mit Kommentaren erlaubt). Global unter
   `~/.config/opencode/`, projektweit im Repo-Root. Configs werden **gemerged, nicht ersetzt**.
3. **Gedächtnis:** OpenCode hat **kein** eingebautes Langzeit-Gedächtnis — nur persistente Sessions +
   statische Regeldateien (**AGENTS.md** + `instructions`). Echtes lernendes Memory nur über Community-Plugins.
4. **Skills sind NATIV** (kein Plugin nötig) und lesen sogar `.claude/skills/` ohne Änderung.
5. **OpenRouter ist eingebaut** (kein npm/baseURL nötig): `/connect` → Key → `/models`. Modell-ID-Format
   `openrouter/<author>/<model>`. Routing-Optionen via `provider.openrouter.models.<model>.options.provider`.
6. **Token sparen** (größter Hebel zuerst): MCP-Server minimieren → gezielte `@datei`-Mentions → schlanke
   AGENTS.md → günstiges `small_model` → Plan-Modus → Compaction-Prune → Prompt-Caching ausnutzen.

## Quellen-Rangordnung
1. **Offiziell** (opencode.ai/docs, openrouter.ai/docs, GitHub anomalyco/opencode, agents.md) = Grundwahrheit.
2. **Extern**