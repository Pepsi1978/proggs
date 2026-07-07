# AGENTS.md, Gedächtnis/Memory, Regeln & Kontext — Best Practices (Stand 2026-06-18, OpenCode CLI)

> Quellen: `offiziell` (opencode.ai/docs, agents.md, GitHub anomalyco/opencode) bzw. `extern` (Drittquellen,
> GitHub-Issues, Community). Gilt für **Windows und macOS**.

---

## 1. AGENTS.md — was es ist und wie OpenCode es nutzt

**Der offene AGENTS.md-Standard** ist ein einfaches, offenes Markdown-Format zur Steuerung von
Coding-Agents — eine „README für Agenten": ein fester Ort für Kontext und Anweisungen. Über 60.000
Open-Source-Projekte nutzen es; verwaltet von der Agentic AI Foundation (Linux Foundation). Es ist
reines Markdown ohne Pflichtfelder. Kompatibel u.a. mit Codex, Cursor, Aider, goose, Zed, Gemini CLI,
GitHub Copilot **und opencode**. `offiziell` (agents.md)

**Wie OpenCode es nutzt:** OpenCode liest AGENTS.md als seine **Rules-/Custom-Instructions-Datei**
(vergleichbar mit Cursors Rules) — der Inhalt geht in den Kontext des LLM. `offiziell` (opencode.ai/docs/rules)

**Wo OpenCode AGENTS.md sucht (zwei Typen):** `offiziell`
1. **Projekt-Ebene:** `AGENTS.md` im Projektroot — ins Git committen, mit Team teilen.
2. **Global:** `~/.config/opencode/AGENTS.md` — gilt über alle Sessions, nicht versioniert (persönliche Regeln).

Beim Start **traversiert OpenCode vom aktuellen Verzeichnis nach oben** und sucht lokale Regeldateien
(`AGENTS.md`, `CLAUDE.md`).

### Werden mehrere (verschachtelte) AGENTS.md zusammengeführt? (häufiges Missverständnis)
- **Projekt-AGENTS.md + globale AGENTS.md werden KOMBINIERT** (nicht überschrieben) — beide gehen in den
  Kontext. `offiziell`/`extern` (opencode.ai/docs/config; Issue #9282)
- **Verschachtelte AGENTS.md in Unterordnern** („closest wins" laut agents.md-Standard): OpenCode lädt
  diese **bisher NICHT automatisch** beim Bearbeiten von Unterordner-Dateien. Das ist ein offener
  Feature-Request (Issue #7576). `extern`
  → **Praktische Konsequenz:** verschachtelte AGENTS.md am zuverlässigsten über `instructions`-Glob
  (`packages/*/AGENTS.md`) einbinden, nicht durch bloßes Ablegen in Unterordnern.

---

## 2. Der `/init`-Befehl — AGENTS.md automatisch erzeugen/aktualisieren `offiziell`

`/init` ist die geführte Einrichtung zum Erstellen/Aktualisieren von AGENTS.md:
- **scannt die wichtigen Dateien im Repo**, stellt ggf. gezielte Rückfragen, **erstellt/aktualisiert** dann
  AGENTS.md mit prägnanter projektspezifischer Anleitung.
- Fokus: Build-/Lint-/Test-Befehle, Verifikationsschritte, Architektur/Repo-Struktur (die nicht aus
  Dateinamen ersichtlich ist), Konventionen, Setup-Eigenheiten, Verweise auf Cursor-/Copilot-Rules.
- **Verbessert vorhandenes AGENTS.md „in place"** statt es blind zu ersetzen.
- Aufruf in der TUI: `/init`. **Best Practice: AGENTS.md ins Git committen.**

---

## 3. Globale vs. projektspezifische Regeln; Migration von CLAUDE.md / Cursor

### Precedence — welche Dateien OpenCode automatisch liest `offiziell`
1. **Lokale Dateien** (von cwd nach oben): `AGENTS.md`, `CLAUDE.md`
2. **Globale Datei:** `~/.config/opencode/AGENTS.md`
3. **Claude-Code-Datei:** `~/.claude/CLAUDE.md` (sofern nicht deaktiviert)

**In jeder Kategorie gewinnt die erste passende Datei.** Hat man `AGENTS.md` UND `CLAUDE.md` im Projekt,
wird **nur `AGENTS.md`** verwendet. Ebenso schlägt `~/.config/opencode/AGENTS.md` die `~/.claude/CLAUDE.md`.

### Claude-Code-Kompatibilität (CLAUDE.md-Migration) `offiziell`
- Projekt: `CLAUDE.md` als Fallback, *wenn kein AGENTS.md existiert*.
- Global: `~/.claude/CLAUDE.md` als Fallback, *wenn kein `~/.config/opencode/AGENTS.md`*.
- Skills: `~/.claude/skills/` wird ebenfalls gelesen.

Deaktivieren:
```bash
export OPENCODE_DISABLE_CLAUDE_CODE=1        # gesamten .claude-Support aus
export OPENCODE_DISABLE_CLAUDE_CODE_PROMPT=1 # nur ~/.claude/CLAUDE.md aus
export OPENCODE_DISABLE_CLAUDE_CODE_SKILLS=1 # nur .claude/skills aus
```

### Cursor-Rules-Migration
Nicht automatisch erkannt, aber über `instructions` einbindbar (`".cursor/rules/*.md"`). `offiziell`

---

## 4. Das `instructions`-Feld in `opencode.json` (zusätzliche Regeldateien per Glob) `offiziell`

```json
{ "$schema": "https://opencode.ai/config.json",
  "instructions": ["CONTRIBUTING.md", "docs/guidelines.md", ".cursor/rules/*.md"] }
```
- Nimmt ein **Array aus Pfaden und Glob-Mustern**.
- **Remote-URLs erlaubt** (Fetch-Timeout 5 s):
  ```json
  { "instructions": ["https://raw.githubusercontent.com/my-org/shared-rules/main/style.md"] }
  ```
- Alle Instructions-Dateien werden **mit** den AGENTS.md kombiniert (additiv).
- **Für Monorepos** ist `instructions` mit Glob (`"packages/*/AGENTS.md"`) der empfohlene, wartbare Weg,
  um verschachtelte AGENTS.md tatsächlich einzubinden.
- Datei-Substitution `{file:path}` / Env `{env:VAR}` zum Auslagern großer Blöcke.

### Alternative: manuelles @-Lazy-Loading in AGENTS.md `offiziell`
OpenCode parst @-Referenzen in AGENTS.md nicht automatisch, man kann dem Agenten aber Lazy-Loading beibringen:
```markdown
## External File Loading
CRITICAL: When you encounter a file reference (e.g., @rules/general.md),
use your Read tool to load it on a need-to-know basis...
- Do NOT preemptively load all references - use lazy loading based on actual need

## Development Guidelines
For TypeScript code style: @docs/typescript-guidelines.md
For React component architecture: @docs/react-patterns.md
```
Vorteile: modulare Regeldateien, AGENTS.md bleibt schlank, Laden nur bei Bedarf. Für Monorepos ist
`opencode.json` mit Glob aber wartbarer.

---

## 5. Gedächtnis / Memory — Sessions, Persistenz, Storage-Orte

### Was OpenCode „von Haus aus" hat
OpenCode hat **kein eingebautes semantisches Langzeitgedächtnis.** Das „Gedächtnis" besteht aus:
1. **Persistenten Sessions** (gespeicherte, wiederaufnehmbare Konversationen).
2. **Statischen Instruktionsdateien** (AGENTS.md + `instructions` + Notizdateien), die bei jeder Session
   frisch in den Kontext geladen werden.

Sessions werden automatisch auf Platte gespeichert. **Keine automatische Bereinigung** und (Stand der
Recherche) **kein offizieller Prune-Befehl** — Storage wächst unbegrenzt (offene Issues #22110, #14292). `extern`

### Storage-Orte (offiziell, je OS)
| Inhalt | macOS / Linux | Windows |
|---|---|---|
| Anwendungsdaten / Sessions | `~/.local/share/opencode/` | `%USERPROFILE%\.local\share\opencode` |
| Logs | `~/.local/share/opencode/log/` | `%USERPROFILE%\.local\share\opencode\log` |
| Provider-Cache | `~/.cache/opencode` | `%USERPROFILE%\.cache\opencode` |
| Globale Config | `~/.config/opencode/opencode.json(c)` | `%USERPROFILE%\.config\opencode\opencode.jsonc` |
| Globale AGENTS.md | `~/.config/opencode/AGENTS.md` | `%USERPROFILE%\.config\opencode\AGENTS.md` |

> Windows folgt nicht der klassischen `%APPDATA%`-Konvention, sondern legt `.local\share\opencode`,
> `.cache\opencode`, `.config\opencode` im User-Profil ab (XDG-artig). `offiziell`

### Innere Struktur von `~/.local/share/opencode/` `offiziell`
- `auth.json` — API-Keys, OAuth-Tokens
- `log/` — Logs
- `project/` — projektspezifische Session-Daten (Git-Repo: `./<project-slug>/storage/`; kein Git:
  `./global/storage/`)

Feinere Aufteilung in `storage/` (Community-verifiziert): `session/<projectID>/<sessionID>.json`,
`message/<sessionID>/<messageID>.json`, `part/<messageID>/<partID>.json`, `session_diff/<sessionID>.json`,
`share/<sessionID>.json`. `extern` (ccusage, Raycast OpenCode Sessions)

### Sessions verwalten (TUI) `offiziell`
- `/sessions` (`/resume`, `/continue`, `ctrl+x l`) — listen/wechseln.
- `/new` (`/clear`, `ctrl+x n`) — neue Session.
- `/undo` / `/redo` (`ctrl+x u` / `ctrl+x r`) — über **Git** (Projekt muss Git-Repo sein).
- `/export` (`ctrl+x x`) — als Markdown exportieren.

### So baut man sich ein funktionierendes „Gedächtnis" (ohne Plugin) `offiziell` (abgeleitet)
1. **AGENTS.md** = stabile projektweite Fakten (Architektur, Konventionen, Befehle).
2. **`instructions`-Glob** = modulare Detailregeln, die mitwachsen.
3. **Notiz-/Entscheidungsdateien** (`docs/decisions.md`, `NOTES.md`) per `instructions` oder @-Lazy-Loading
   einbinden — dort dokumentieren, was über Sessions „erinnert" werden soll. Ins Git committen.
4. Optional globale `~/.config/opencode/AGENTS.md` für persönliche, projektübergreifende Präferenzen.

### Echtes Langzeitgedächtnis nur via Plugins (extern — vor Installation Sicherheit prüfen!)
- **opencode-agent-memory** — Letta-inspirierte „Memory Blocks" (Markdown auf Platte, global + projektbezogen,
  überlebt Sessions und Kompaktierung). `extern` (joshuadavidthomas)
- **opencode-mem** — lokale Vektor-DB (SQLite + USearch), persistente Projektgedächtnisse, User-Profil-Lernen. `extern`
- **opencode-supermemory** — sessions-/projektübergreifendes Erinnern. `extern`
- **opencode-hindsight** (Vectorize) — Hooks (recall/retain/preserve) + Tools, injiziert Erinnerungen in den
  System-Prompt. `extern`

---

## 6. Kontext-Management

### `/compact`
`/compact` (Alias `/summarize`, `ctrl+x c`) fasst die aktuelle Session zusammen, behält die wichtigen Teile. `offiziell`

### Automatische Kompaktierung (Config) `offiziell`
```json
{ "compaction": {
    "auto": true,      // bei vollem Kontext automatisch (Default true)
    "prune": false,    // alte Tool-Outputs entfernen, um Tokens zu sparen (Default false)
    "reserved": 10000  // Token-Puffer gegen Overflow
} }
```

### Wie die Kompaktierung intern funktioniert (Architektur) `offiziell`(Repo/DeepWiki)
- **Overflow-Erkennung** nach jeder Assistant-Nachricht. Nutzbarer Kontext = Modell-Limit − reservierte
  Output-Tokens (Default 32.000) − Buffer (20.000).
- **Kompaktierung** über eigenen `compaction`-Agenten, strukturiert nach Goal/Instructions/Discoveries/Accomplished.
- **Tool-Output-Pruning** entfernt alte Tool-Ausgaben (behält die Tatsache des Aufrufs); letzte 2 User-Turns
  geschützt; **Skill-Tool-Outputs werden nie geprunt**. Pruning = logische Markierung, keine physische Löschung.

### @-Mentions & Shell-Output `offiziell`
- `@pfad/datei` → Fuzzy-Suche, Dateiinhalt automatisch in die Konversation. `@alias` fügt Referenz-Root hinzu.
- Nachricht mit `!` → Shell-Befehl, Ausgabe als Tool-Ergebnis.

### Kontext schlank halten (Empfehlungen)
- `compaction.prune: true` bei langen Sessions mit vielen Tool-Outputs.
- Große/selten gebrauchte Regeln per `instructions`-Glob oder @-Lazy-Loading auslagern.
- `/new` für getrennte Aufgaben statt eine Session unbegrenzt wachsen lassen.
- `"snapshot": false` bei sehr großen Repos (spart Disk/Index, kostet UI-Rollback).

---

## 7. Best Practices für eine gute AGENTS.md

### Aufbau (bewährte Abschnitte) `offiziell`
Project overview, Build/Test/Lint commands, Code style, Testing instructions, Security considerations,
Commit-/PR-Konventionen, Setup-Eigenheiten.

Minimalbeispiel:
```markdown
# AGENTS.md
## Setup commands
- Install deps: `pnpm install`
- Start dev server: `pnpm dev`
- Run tests: `pnpm test`

## Code style
- TypeScript strict mode
- Single quotes, no semicolons
- Use functional patterns where possible
```
Reales Vorbild ist die AGENTS.md des opencode-Repos selbst: sehr konkret und befehlsorientiert
(„avoid `else`, prefer early returns", „prefer `const` over `let`", präzise Test-Hinweise). `offiziell`

### Länge & Token-Effizienz (das Wichtigste)
- **Jeder Token im Root-AGENTS.md wird bei JEDER Anfrage geladen** — eine zu große Datei verkleinert
  direkt den verfügbaren Kontext und verschlechtert die Leistung. `extern`
- **Faustregel:** möglichst < ~150 Zeilen; ab ~200–400 Zeilen degradiert die Befolgung. Terse halten. `extern`

### Was rein / was raus
- **Rein:** alles, was man einem neuen Teammitglied sagen würde (Befehle, Architektur, Konventionen,
  Commit-Format, Sicherheits-Gotchas, Setup).
- **Raus aus dem Root:** package-spezifische Details → `packages/*/AGENTS.md` (per `instructions`-Glob);
  lange, selten gebrauchte Detailregeln → separate Dateien + Lazy-Loading.

### Damit das Modell zuverlässig folgt
- **Test-Befehle wirklich auflisten** (der Agent führt gelistete Checks automatisch aus und fixt Fehler). `offiziell`
- **Konfliktregel:** bei Widersprüchen gewinnt das nächstgelegene AGENTS.md; ein expliziter User-Prompt schlägt
  alles. (In OpenCode greift „nächstgelegen" nur, wenn die Datei tatsächlich geladen ist — Glob/instructions.)
- **AGENTS.md als lebende Dokumentation** behandeln und committen.

---

## Cheat-Sheet

| Zweck | Mechanismus | macOS/Linux | Windows |
|---|---|---|---|
| Projektregeln (geteilt) | `AGENTS.md` im Root | `<repo>/AGENTS.md` | `<repo>\AGENTS.md` |
| Persönliche globale Regeln | globale `AGENTS.md` | `~/.config/opencode/AGENTS.md` | `%USERPROFILE%\.config\opencode\AGENTS.md` |
| Claude-Fallback | `CLAUDE.md` / `~/.claude/CLAUDE.md` | analog | analog |
| Zusätzliche Regeln per Glob | `instructions` in `opencode.json` | `<repo>/opencode.json` | `<repo>\opencode.json` |
| Sessions/App-Daten | Storage | `~/.local/share/opencode/` | `%USERPROFILE%\.local\share\opencode` |
| AGENTS.md erzeugen | `/init` (TUI) | — | — |
| Kontext kompaktieren | `/compact` (`ctrl+x c`) | — | — |

**Merksätze:** (1) Pro Kategorie gewinnt die erste Datei (AGENTS.md > CLAUDE.md), aber Projekt+Global
werden kombiniert. (2) Verschachtelte AGENTS.md werden NICHT automatisch geladen → `instructions`-Glob nutzen.
(3) Root-AGENTS.md kurz halten. (4) Kein echtes Langzeitgedächtnis out-of-the-box — nur Sessions + statische
Dateien; für lernendes Memory braucht es Plugins.

## Quellen
**Offiziell:** opencode.ai/docs/rules, /config, /tui, /troubleshooting; agents.md; sst/opencode AGENTS.md;
DeepWiki (Context Management & Compaction).
**Extern:** GitHub-Issues #7576 (nested AGENTS.md), #9282 (combined not overridden), #22110/#14292 (session
prune); ccusage; Raycast OpenCode Sessions; DEV/Augment/gist (AGENTS.md-Länge); Memory-Plugins
opencode-agent-memory, opencode-mem, opencode-supermemory, Hindsight.
