# Claude Code — Desktop-App vs. CLI — Best Practices (Stand 2026-06-13)

> Quelle der Grundwahrheit: offizielle Anthropic-Doku `code.claude.com/docs/en/desktop`
> (Abschnitte „Feature comparison" + „What's not available in Desktop") sowie der
> Desktop-Redesign vom **14. April 2026** (Multi-Session-Sidebar, Pane-Layout, Routines).
> Externe Quellen sind als `extern` markiert und überstimmen nie das Offizielle.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Drei Tabs im Desktop | **Chat** (Gespräche), **Cowork** (Dispatch/lange Agenten-Arbeit), **Code** (Programmieren). Zum Coden → Tab **Code**. | §1 |
| 2 | Gleiche Engine | Desktop und CLI nutzen **dieselbe Engine** + dieselben Configs (CLAUDE.md, MCP, Hooks, Skills, settings.json). Parallel auf demselben Projekt erlaubt. | §2 |
| 3 | Stärke Desktop | Parallele Sessions in EINEM Fenster, automatische **Git-Worktrees**, visuelle Diffs, Vorschau-Browser, PR-Monitoring, Panes nebeneinander. | §3 |
| 4 | Stärke CLI | **Scripting & Automatisierung**: `--print`, `--output-format`, Agent SDK / Headless, Cron/CI, Agent-Teams, `dontAsk`-Modus. | §4 |
| 5 | NICHT im Desktop | Headless/Scripting, Agent-Teams (die sich Nachrichten schicken), `/agents` `/doctor` `/config` `/permissions`, Inline-Code-Vorschläge, Linux, Drittanbieter (Bedrock/Vertex/Foundry) standardmäßig. | §4 |
| 6 | CLI → Desktop wechseln | Im Terminal `/desktop` tippen → Session öffnet im Desktop, CLI beendet sich. (Nur mit Abo-Login, nicht mit API-Key/Bedrock/Vertex.) | §2 |
| 7 | Frank-spezifisch | Seine Hook-/Agent-Team-/Headless-Workflows sind **CLI-Kernland**. Im Desktop verschwinden Agent-Teams + Headless; Hooks/Skills/Subagenten bleiben. | §5 |

---

## §1 — Die drei Tabs und was „Programmieren in Claude Code" im Desktop bedeutet

Die **Claude Desktop App** (macOS + Windows, **nicht Linux**) hat drei Tabs:

- **Chat** — normale Unterhaltungen.
- **Cowork** — Dispatch und längere agentische Arbeit (genau hier läuft diese Sitzung). Recherche, Dokumente, Tabellen bleiben hier.
- **Code** — die eigentliche Software-Entwicklung. Das ist „programmieren in Claude Code in der Desktop-App".

Im Code-Tab ist jede Unterhaltung eine **Session** mit eigener Chat-Historie, eigenem Projektordner und eigenen Code-Änderungen. Vor dem ersten Prompt konfiguriert man vier Dinge: **Environment** (Local / Remote-Cloud / SSH), **Projektordner**, **Modell** (Dropdown, mitten in der Session änderbar) und **Permission-Mode**.

Voraussetzungen: bezahltes Abo (Pro, Max, Team oder Enterprise). Unter **Windows** ist **Git for Windows** Pflicht, sonst startet der Code-Tab nicht (danach App neu starten). Quelle: offizielle Doku, `offiziell`.

---

## §2 — Gemeinsame Basis (was 1:1 übertragbar ist)

Desktop ist dieselbe Engine mit grafischer Oberfläche. Beide lesen dieselben Konfigurationsdateien, der Setup trägt sich also herüber:

- **CLAUDE.md** und `CLAUDE.local.md` — von beiden genutzt (Projekt-Gedächtnis geteilt).
- **MCP-Server** in `~/.claude.json` / `.mcp.json` — funktionieren in beiden.
- **Hooks** und **Skills** aus den Settings — **gelten laut Doku in beiden**.
- **Settings** in `~/.claude.json` und `~/.claude/settings.json` — geteilt (Permission-Regeln, erlaubte Tools usw. greifen auch im Desktop).
- **Modelle** — dieselben; im Desktop über das Dropdown wählbar.

Man kann CLI und Desktop **gleichzeitig** auf demselben Rechner und sogar demselben Projekt laufen lassen; getrennte Session-Historie, gemeinsame Konfiguration. Wechsel: im Terminal `/desktop` tippen → die laufende Session wird gespeichert und im Desktop geöffnet, die CLI beendet sich. Verfügbar auf macOS + Windows, **nur** mit Claude-Abo-Login (nicht mit API-Key, Bedrock, Vertex oder Foundry). Quelle: offizielle Doku, `offiziell`.

> Hinweis zu MCP: Der Desktop lädt zusätzlich Server aus `claude_desktop_config.json` in den Code-Tab. Die eigenständige CLI liest diese Datei **nicht** — dafür einmalig `claude mcp add-from-claude-desktop` (macOS/WSL).

---

## §3 — Was der Desktop besser/exklusiv kann (Stärken der GUI)

Belegt durch die offizielle Doku, `offiziell`:

1. **Parallele Sessions in einem Fenster** — Sidebar statt mehrerer Terminals. `Cmd/Ctrl+N` neue Session, `Ctrl+Tab` durchblättern. Zwei Sessions nebeneinander per `Cmd/Ctrl`-Klick.
2. **Automatische Git-Worktrees** — jede Session bekommt eine isolierte Projektkopie (`<project>/.claude/worktrees/`); Änderungen einer Session berühren andere erst nach Commit. In der CLI ist das der manuelle `--worktree`-Flag.
3. **Drag-and-Drop-Pane-Layout** — Panes frei anordnen: **chat, diff, preview, terminal, file, plan, tasks, subagent**. (Ab Desktop v1.2581.0.)
4. **Integriertes Terminal** (`Ctrl+`` `) — gleiches Arbeitsverzeichnis und gleiche Umgebung wie Claude. **Nur lokale Sessions.**
5. **In-App-Datei-Editor** — Dateien per Klick öffnen, Spot-Edits, speichern. Lokale + SSH-Sessions (Cloud: Claude bitten).
6. **Visueller Diff-Review** — Datei-für-Datei, Kommentare an einzelnen Zeilen (`Cmd/Ctrl+Enter` zum Absenden), Button **„Review code"** für eine Vor-Commit-Prüfung (Fokus: echte Fehler, Sicherheitslücken — kein Linter-Stil).
7. **App-Vorschau im eingebetteten Browser** — Claude startet den Dev-Server, macht Screenshots, klickt/füllt Formulare, prüft DOM und **auto-verifiziert** Änderungen (`.claude/launch.json`).
8. **PR-Monitoring** — CI-Statusleiste, **Auto-Fix** und **Auto-Merge** (Squash). Braucht GitHub CLI `gh` installiert + authentifiziert.
9. **Permission-Modi per Klick** — Ask, Auto accept edits, Plan, Auto (Research Preview, Opus 4.6+/Sonnet 4.6), Bypass.
10. **Computer Use** (Bildschirmsteuerung) — Research Preview auf macOS **und** Windows, Pro/Max (nicht Team/Enterprise).
11. **Side-Chat** (`Cmd/Ctrl+;` oder `/btw`) — Zwischenfrage mit Session-Kontext, ohne den Hauptlauf zu entgleisen.
12. **Environments** — Local, **Remote** (Cloud, läuft weiter wenn App zu ist), **SSH** (eigene VMs/Server).
13. **Connectors-/Skills-/Plugin-UI** — Installation per Knopf statt `/plugin`.
14. **Geplante Aufgaben** (Scheduled Tasks) + **Dispatch** (Tasks vom Handy → Code-Session).
15. **Datei-Anhänge** (Bilder, PDFs) und **@mention mit Autocomplete** (lokale + SSH-Sessions) — in der CLI gibt es Anhänge so nicht.
16. **Transcript-Ansichten** — Normal / Verbose / Summary (`Ctrl+O`).

---

## §4 — Was im Desktop NICHT geht (CLI-/Extension-exklusiv) — der Kern der Frage

Offizielle „What's not available in Desktop"-Liste + Feature-Vergleichstabelle, `offiziell`:

| Fehlt im Desktop | Details | Wo stattdessen |
|------------------|---------|----------------|
| **Scripting & Automatisierung** | `--print`, `--output-format`, **Agent SDK / Headless-Mode**. Desktop ist **rein interaktiv**. | CLI / Agent SDK |
| **Agent-Teams** | Parallele Claude-Code-Sessions, **die sich gegenseitig Nachrichten schicken**. | CLI (`/en/agent-teams`). Im Desktop für Multi-Agent in EINER Session → **Dynamic Workflows** |
| **Terminal-Dialog-Befehle** | `/permissions`, `/config`, `/agents`, `/doctor` antworten mit „isn't available in this environment". | Settings-Dateien direkt editieren oder eigenständige CLI |
| **`dontAsk`-Permission-Mode** | Nur-vorab-genehmigte-Tools-Modus. | CLI |
| **`--allowedTools` / `--disallowedTools`** | Kein Pro-Session-Äquivalent (Permission-Regeln aus Settings gelten weiter). | settings.json |
| **Inline-Code-Vorschläge** | Kein Autocomplete-Stil; nur konversationelle Prompts + explizite Änderungen. | VS-Code-Extension |
| **Linux** | Desktop nur macOS + Windows. | CLI |
| **Drittanbieter standardmäßig** | Bedrock / Vertex AI / Foundry. Desktop nutzt standardmäßig Anthropics API (Enterprise kann Vertex/Gateway konfigurieren; Ausnahme: „Cowork on 3P"-Preview). | CLI |
| **`--print` / `--output-format`** | Nicht verfügbar — Desktop ist interaktiv. | CLI |

Außerdem als Einschränkung (kein kompletter Wegfall, aber begrenzt):
- **Integriertes Terminal** und **@mention** gibt es **nicht in Cloud-Sessions**; Terminal nur lokal, File-Pane lokal+SSH.

**Hooks-Nuance (wichtig für Frank):** Laut offizieller Doku **gelten Hooks aus den Settings in beiden Umgebungen**. In Community-Foren kursieren Berichte über nicht ausgelöste Hooks in bestimmten Desktop-/API-Modi (`extern`, nicht bestätigt) — die Grundwahrheit bleibt: offiziell sind Hooks geteilt. Was definitiv fehlt, sind die **interaktiven Verwaltungs-Befehle** (`/agents`, `/doctor` …), nicht die Hook-Ausführung selbst.

---

## §5 — Frank-spezifische Übersetzung seines CLI-Workflows in den Desktop

Sein `~/proggs/CLAUDE.md`-Setup ist stark CLI-zentriert. Mapping:

| Sein Workflow-Element | Im Desktop? | Anmerkung |
|-----------------------|-------------|-----------|
| Hooks (PostToolUseFailure-Bug-Writer, session-guard) | ✅ offiziell geteilt | Greifen über `settings.json`; Verwaltung aber nur per Datei (kein `/hooks`-Dialog) |
| Parallele **Subagenten** (quality-gate, coder, researcher) | ✅ | tasks- + subagent-Pane, **Dynamic Workflows** für Multi-Agent in einer Session |
| **Agent-Teams** (`TeamCreate`, Teammates reden miteinander) | ❌ | **CLI-only** — im Desktop nicht verfügbar |
| **Headless / Skript-Automatisierung** | ❌ | `--print` / Agent SDK nur CLI; Routines (Cloud) sind der Desktop-Ersatz für „läuft ohne Laptop" |
| `CLAUDE_CODE_SUBAGENT_MODEL`, Effort-Level | ✅ | Env via lokalem Environment-Editor; Effort-Menü `Cmd/Ctrl+Shift+E` |
| `/effort`, `/compact` | ✅ | `/compact` vorhanden; Effort über Menü |
| `/agents`, `/doctor`, `/permissions`, `/config` | ❌ | Settings-Dateien direkt editieren |
| Commit + Push nach jeder Änderung | ✅ | Integriertes Terminal + Diff-View + PR-Monitoring machen das sogar komfortabler |
| Stream-Deck-Shortcuts für PowerShell-CLI | ⚠️ | Im Desktop weniger relevant (GUI); stattdessen native Keyboard-Shortcuts/Global-Hotkeys |
| Observability/Live-Logging-Tails | ✅ | Integriertes Terminal für `tail -f` / `adb logcat`; nur lokale Sessions |

**Empfehlung für Frank:** Hybrid fahren. Tägliche Feature-Arbeit + visueller Review → **Desktop Code-Tab** (Worktrees + Diff + Preview sind ein echter Gewinn). Seine **Automatisierungs-Maschinerie** (Hook-getriebene Bug-Almanach-Pipelines, `/self-improve`-Loops, Agent-Teams, Headless-Batch-Jobs) bleibt **CLI**. Beide gleichzeitig auf `~/proggs` möglich.

---

## §6 — Best Practices für das Programmieren im Desktop Code-Tab

1. **Komplexe Aufgaben in Plan-Mode starten** → Plan prüfen → dann Auto accept edits/Ask. (Offizielle „explore → plan → code"-Empfehlung.)
2. **Worktrees nutzen statt Branches von Hand** — parallele Experimente kollidieren nicht; fertige Session per Archiv-Icon aufräumen, `.worktreeinclude` für `.env` & Co.
3. **Preview + Auto-Verify anlassen** für UI-Arbeit — Claude testet sich selbst (Screenshots, DOM, Formulare).
4. **Diff-Review + „Review code"** vor jedem Commit — fängt echte Logik-/Sicherheitsfehler, kein Stil-Rauschen.
5. **PR-Monitoring mit Auto-Fix** anschalten (braucht `gh`), CI-Fehler werden iterativ selbst behoben.
6. **Lange Läufe → Remote/Cloud-Session** (läuft weiter, auch wenn App zu ist) statt Laptop offen lassen.
7. **Side-Chat (`/btw`)** für Zwischenfragen, damit der Hauptlauf fokussiert bleibt.
8. **Env-Variablen** über den lokalen Environment-Editor setzen (verschlüsselt) — der Desktop erbt **nicht** die volle Shell-Umgebung (liest unter macOS nur PATH + feste Variablen aus dem Profil; unter Windows keine PowerShell-Profile).
9. **Für Skript-/CI-/Batch-Arbeit bewusst in die CLI wechseln** — das ist kein Workaround, sondern die richtige Umgebung dafür.

---

## §7 — Bezug zum Bug-Almanach (Gegenseite)

Gegenseite: `bugs/claude-tooling/claude-code-desktop-vs-cli.md` (45+ belegte Fallen, mit Fix-Status).

| Best-Practice-Abschnitt (hier) | Bug-Abschnitt (dort) |
|--------------------------------|----------------------|
| §1 Tabs/Voraussetzungen | §A (Installation/Start) |
| §2 gemeinsame Configs | §C (Hooks/Settings), §E4 (MCP-Config-Trennung) |
| §3.2/§6.2 Worktrees | §F (Worktrees) |
| §3.7/§6.3 Preview | §H (Preview) |
| §3.10 Computer Use | §G (Computer Use) |
| §3.12/§6.6 Environments/Cloud | §I (Cloud/SSH/Session) |
| §4 „NICHT im Desktop" | §J (Feature-Lücken) |
| §5 Frank-Mapping (Hooks/Agent-Teams) | §C1/§C5, §J2 |
| §6.8 Env-Editor | §B (PATH/Env, stille Killer) |
| §6.9 für Skript/Batch CLI | §J1, §K1 |

## Quellen

- Offizielle Doku „Desktop application", code.claude.com/docs/en/desktop — Feature-Vergleich + „What's not available in Desktop" (`offiziell`, abgerufen 2026-06-13)
- Anthropic Desktop-Redesign 14.04.2026 (Multi-Session, Panes, Routines) — mehrere Berichte (`extern`, Datum bestätigt)
- VentureBeat / MindStudio / vibecoding zur Desktop-vs-CLI-Praxis (`extern`, sekundär)
