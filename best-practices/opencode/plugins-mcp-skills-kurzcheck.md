# Plugins, Custom Tools, MCP-Server, Skills & Commands Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | MCP-Server | im `mcp`-Block: `type:"local"` mit `command`-Array bzw. `type:"remote"` mit `url`; alle Tools erscheinen mit Server-Präfix; OAuth startet automatisch bei 401 | §1 |
| 2 | MCP frisst Tokens | jedes MCP-Tool-Schema geht in JEDE Anfrage (GitHub-MCP ~20k Tokens) → global aus (`"tools":{"server*":false}`), nur im benötigenden Agent an | §1, §6 |
| 3 | Skills sind NATIV | OpenCode unterstützt `SKILL.md` first-party; `.claude/skills/` + `~/.claude/skills/` werden OHNE Änderung mitgelesen (kein Plugin-Hack nötig); on-demand geladen | §4 |
| 4 | Skill-Frontmatter | `name` (= Ordnername, `^[a-z0-9]+(-[a-z0-9]+)*$`) + `description` sind Pflicht; Agent lädt per `skill({ name })`; Skill-Permissions via `permission.skill` | §4 |
| 5 | Plugins (JS/TS) | in `.opencode/plugins/` bzw. `~/.config/opencode/plugins/` oder npm via `plugin`-Feld (auto-Install via Bun, Cache `~/.cache/opencode/node_modules/`) | §2 |
| 6 | Plugin-Falle | jede Plugin-Funktion empfängt ein KONTEXT-OBJEKT, nicht den Client direkt: richtig `async ({ client, $ }) => …`, falsch `async (client) => …` | §2 |
| 7 | Custom Tools | `.opencode/tools/*.ts`, Dateiname = Tool-Name, `tool.schema` ist Zod; Tool = einzelne LLM-Funktion (kein Hook), Plugin = volles Modul mit Hooks/Events | §3 |
| 8 | Slash-Commands | `.opencode/commands/*.md`, Dateiname = Command-Name; Platzhalter `$ARGUMENTS`, `$1`, `` !`cmd` `` (Shell), `@datei`; `subtask:true` erzwingt eigenen Kontext | §5 |
| 9 | Kein Triggerwort-Modell | Plugins sind event-getriggert (laufen nur bei ihrem Event), Commands manuell, Skills on-demand nach `description` — ein installiertes Plugin „stört" nicht bei jeder Anfrage | §8 |
| 10 | Sicherheit (KRITISCH) | npm-Plugins werden auto via Bun installiert + ausgeführt (voller FS-/Shell-Zugriff) → Quellcode prüfen, Paketname exakt (Typosquatting), nur `opencode.ai` ist offiziell | §6, §8 |
| 11 | Discovery | erst offizielles `opencode.ai/docs/ecosystem` (stärkstes Vertrauenssignal), dann `awesome-opencode`; MCP-Server: `registry.modelcontextprotocol.io` | §6 |
| 12 | Plugin deinstallieren | KEIN CLI-`remove`; Eintrag aus **ALLEN** Configs entfernen (auch `tui.json`, nicht nur `opencode.jsonc`) + OpenCode **schließen** + Cache `~/.cache/opencode/packages/<scope>` + plugin-Config (z.B. `dcp.jsonc`) löschen; ggf. `~/.config/opencode/package.json` zurücksetzen (append-only). Almanach §7 #55d | §2 |
