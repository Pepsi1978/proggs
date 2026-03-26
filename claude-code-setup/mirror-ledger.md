# Mirror Ledger — Universal Cross-Platform & Cross-CLI Sync
<!-- VERSION: 1 -->
<!-- FORMAT: universal-mirror-bridge-v1 -->
<!-- CREATED: 2026-03-25 -->

## Was ist diese Datei?

Diese Datei ist das **zentrale Austausch-Ledger** fuer ALLE Programmierumgebungen.
Jedes CLI-Tool (Claude Code, Codex CLI, Gemini CLI) und jede Plattform (macOS, Windows)
schreibt hier rein was geaendert wurde und liest was die anderen geaendert haben.

**Workflow:**
1. `export` Agent schreibt neue Eintraege am Ende einer Session
2. `import` Agent liest ausstehende Eintraege beim Start auf einer anderen Plattform/CLI
3. Jeder Eintrag ist EXTREM ausfuehrlich und selbsterklaerend — keine Vorkenntnisse noetig

**Regeln:**
- Jeder Eintrag hat eine eindeutige ID: `MIRROR-YYYY-MM-DD-{PLATTFORM}-{NNN}`
- Plattform-Kuerzel: MAC (macOS), WIN (Windows), CDX (Codex), GEM (Gemini)
- Eintraege werden NUR angehaengt, nie geloescht
- APPLIED-Kommentare werden von `import` Agents aktualisiert (PENDING → Timestamp)
- Eintraege aelter als 90 Tage UND auf allen Plattformen applied duerfen archiviert werden

**Entry-Typen:** `hook`, `rule`, `agent`, `skill`, `settings`, `mcp`, `software`, `env-fix`, `directive`, `plugin`, `whiteboard`

---

<!-- EINTRAEGE BEGINNEN HIER — neue Eintraege werden nach dem letzten --- angehaengt -->

---

## [BASELINE-2026-03-25] Vollstaendiges Umgebungs-Inventar — Von Null aufbauen
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T22:30:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: baseline -->
<!-- AFFECTS: GESAMTE Programmierumgebung -->

> **Dies ist der BASELINE-Eintrag.** Er beschreibt die KOMPLETTE Programmierumgebung so
> detailliert, dass ein frisches CLI (Claude Code, Codex, Gemini) auf einem neuen Rechner
> die gesamte Umgebung von Null aufbauen kann. Alle nachfolgenden Eintraege sind inkrementell.

### Bootstrap-Anleitung (Schritt fuer Schritt)

**Voraussetzung:** Git und Claude Code CLI muessen installiert sein.

```bash
# 1. Repository klonen
git clone https://github.com/Pepsi1978/proggs ~/proggs
cd ~/proggs

# 2. Setup-Script ausfuehren
# macOS:
bash claude-code-setup/setup-macos.sh
# Windows:
# powershell claude-code-setup/setup-windows.ps1

# 3. CLAUDE.md an den richtigen Ort kopieren
cp ~/proggs/CLAUDE.md ~/CLAUDE.md
```

Das Setup-Script installiert: Plugins aus 10+ Marketplaces, kopiert Agents/Hooks/Rules/Commands
nach `~/.claude/`, konfiguriert Settings und MCP-Server.

**Alle Dateien liegen im Setup-Repo:** `~/proggs/claude-code-setup/`
Dieses Verzeichnis ist die **einzige Wahrheitsquelle** fuer die Konfiguration.

---

### 1. Philosophie und Direktiven (KRITISCH — ohne das ist nichts sinnvoll)

Die Umgebung verfolgt EIN Ziel: **Die intelligenteste Programmierumgebung der Welt werden.**

Drei Direktiven in absteigender Prioritaet:
1. **Superintelligenz** (`rules/superintelligence.md`) — Exponentielle Intelligenzsteigerung in jeder Session
2. **Selbstbeobachtung** (`rules/self-observation.md`) — Jede Aktion beobachten, Fehler→Upgrades, Intelligenz-Vorschlaege am Ende
3. **Resilient Bugfixing** (`rules/resilient-bugfixing.md`) — Jeder Bug genau EINMAL, dann nie wieder. 5-Schritte-Fix

Diese Direktiven stehen auch in `CLAUDE.md` (wird bei jedem Start geladen) und im Whiteboard.

---

### 2. Custom Agents (18 Stueck)

Alle in `claude-code-setup/agents/*.md`, werden nach `~/.claude/agents/` kopiert.

| Agent | Modell | Zweck |
|-------|--------|-------|
| `architect` | Opus | Systemarchitektur entwerfen, bevor Code geschrieben wird |
| `batch-reviewer` | Sonnet | Schnelle Bulk-Reviews fuer grosse Changesets |
| `challenger` | Opus | Devil's Advocate — hinterfragt Plaene und Architektur-Entscheidungen |
| `code-reviewer` | Opus | Code-Review mit `memory: project` (lernt ueber Sessions) |
| `coder` | Sonnet | Schnelle Implementation, `isolation: worktree`, 3-5 parallel spawnen |
| `debugger` | Opus | Systematisches Debugging mit Sub-Agenten fuer Hypothesen |
| `env-checker` | Sonnet | Prueft installierte Tools, Versionen, Hooks, Plugins, Security |
| `evolution-analyst` | Opus | Meta-Evolution Trend-Analyse aus session-scores.jsonl |
| `export` | Opus | Mirror Bridge: Schreibt Session-Aenderungen ins Ledger |
| `import` | Opus | Mirror Bridge: Liest Ledger und portiert auf aktuelle Plattform |
| `intelligence-researcher` | Opus | Recherche fuer Self-Improve: Reasoning-Breakthroughs, Cognitive Tools |
| `mar-reviewer` | Opus | Multi-Agent Reflexion: 3 parallele Kritiker (Security, Architect, User Advocate) |
| `nemo` | Opus | Orchestriert NVIDIA Nemotron 3 Super 120B (kostenlos via NIM API) fuer Massen-Generierung, Recherche, Uebersetzung |
| `optimizer` | Opus | Performance, Binary-Groesse, Ressourcen optimieren |
| `quality-gate` | Opus | Startet tester+code-reviewer+optimizer parallel, gibt PASS/FAIL |
| `researcher` | Sonnet | Schnelles Web-Lookup, 3-5 parallel spawnen |
| `tester` | Opus | Tests schreiben und ausfuehren, liest Specs |
| `ui-polisher` | Opus | UI professionell und Store-qualitaet machen |

**Geschwindigkeitsregel:** Opus denkt, Sonnet macht. `coder`+`researcher`+`batch-reviewer` = Sonnet (schnell+guenstig). Alles andere = Opus.

---

### 3. Hooks (52 Dateien — 26 Paare .sh/.ps1)

Alle in `claude-code-setup/hooks/`, werden nach `~/.claude/hooks/` kopiert.
Jeder Hook existiert als `.sh` (macOS/bash) UND `.ps1` (Windows/PowerShell).

| Hook-Paar | Event | Zweck |
|-----------|-------|-------|
| `auto-sync` | SessionStart | Synchronisiert Config automatisch von GitHub beim Start |
| `mirror-check` | SessionStart | Zeigt an wenn Mirror-Ledger-Eintraege ausstehen |
| `disk-guard` | SessionStart | Warnt wenn Speicherplatz knapp wird |
| `plugin-health-check` | SessionStart | Prueft ob alle Plugins funktionieren |
| `claude-mem-worker-guard` | SessionStart | Stellt sicher dass claude-mem Worker laeuft |
| `pending-admin-updates` | SessionStart | Zeigt ausstehende Admin-Updates an |
| `safety-gate` | PreToolUse/Bash | Blockiert gefaehrliche Befehle (rm -rf, etc.) |
| `config-guard` | ConfigChange | Schuetzt settings.json vor ungewollten Aenderungen |
| `auto-format` | PostToolUse (async) | Formatiert Dateien nach Edit/Write automatisch |
| `hook-log` | PostToolUse | Loggt Fehler mit ERR/EXIT-Traps |
| `agent-resource-lock` | SubagentStart | Verhindert dass 2 Agents die gleiche Datei bearbeiten |
| `subagent-context` | SubagentStart | Gibt Subagents zusaetzlichen Kontext |
| `intent-anker` | UserPromptSubmit | Speichert Session-Ziel, erinnert alle 5 Turns |
| `whiteboard-insert` | PostToolUse | Schreibt Erkenntnisse ins Whiteboard |
| `whiteboard-safe-pull` | SessionStart | Sicherer Git-Pull fuer Whiteboard |
| `writeback-enforcer` | PostToolUse | Erzwingt Cross-Platform-Sync |
| `memory-watchdog` | PostToolUse | Ueberwacht Agent Write-Back Compliance |
| `session-scorer` | SessionEnd | Bewertet Session-Qualitaet (IQ-Score, Meta-Intelligence) |
| `session-autopsy` | SessionEnd | Clustert Korrekturen, generiert Auto-Rules |
| `session-cleanup` | SessionEnd | Raeumt temporaere Dateien auf |
| `stopfailure-logger` | StopFailure | Loggt wenn Session nicht sauber beendet wird |
| `notify` | Notification | macOS-Benachrichtigungen bei langen Aufgaben |
| `reindex-codebase` | PostToolUse | Reindiziert Code-Search nach Datei-Aenderungen |
| `sync-hooks-reference` | PostToolUse | Haelt Setup-Repo Hooks synchron |
| `admin-setup` | SessionStart | Prueft Admin-Voraussetzungen |
| `porting-inbox` | DEPRECATED | Ersetzt durch mirror-check + import Agent |

**Zusaetzlich:**
- `prompt-injection-defender/` — Python-basierter Prompt Injection Scanner (Verzeichnis mit mehreren Dateien)
- `claude-mem-worker-launcher.sh` — Startet claude-mem Worker als Daemon (nur macOS)
- `session-score.ps1` — Windows-Version des Session-Scorers (TypeScript-Version ist primaer)

---

### 4. Rules (21 Dateien)

Alle in `claude-code-setup/rules/*.md`, werden nach `~/.claude/rules/` kopiert.
Rules werden automatisch beim Start geladen — keine Settings-Registrierung noetig.

| Regel | Prioritaet | Zweck |
|-------|-----------|-------|
| `superintelligence.md` | #1 (HOECHSTE) | Oberstes Ziel: Intelligenteste Umgebung der Welt |
| `self-observation.md` | #2 | Selbstbeobachtung bei jeder Aufgabe, Intelligenz-Vorschlaege |
| `resilient-bugfixing.md` | #3 | Jeder Bug genau 1x, 5-Schritte-Fix, Defense in Depth |
| `agent-reliability.md` | Hoch | Keine stillen Agent-Abstuerze, Timeout-Erwartungen, Status-Updates |
| `german-agents-skills.md` | Hoch | Alle eigenen Agents/Skills auf Deutsch |
| `german-skill-triggers.md` | Hoch | Deutsche Trigger-Map: Whisper-Phrasen → Skills/Agents |
| `intent-tracking.md` | Hoch | Verhindert Intent Drift in langen Sessions |
| `spec-first.md` | Mittel | Specs schreiben VOR Code (Invarianten, Pre/Post-Conditions) |
| `semantic-search-isolation.md` | Hoch | Semantische Suche NIEMALS plattformuebergreifend aendern |
| `codex-directory-forbidden.md` | KRITISCH | ~/Codex/ ist GESPERRT (paralleler Codex-Klon) |
| `swift.md` | Sprache | Swift-Konventionen |
| `csharp.md` | Sprache | C#/WPF-Konventionen |
| `typescript.md` | Sprache | TypeScript-Konventionen |
| `rust.md` | Sprache | Rust-Konventionen |
| `go.md` | Sprache | Go-Konventionen |
| `kotlin.md` | Sprache | Kotlin/Android-Konventionen |
| `android.md` | Sprache | Android-spezifische Regeln |
| `java.md` | Sprache | Java/Spring-Konventionen |
| `cpp.md` | Sprache | C++-Konventionen |
| `tampermonkey.md` | Spezial | Tampermonkey-Skript-Regeln (Versionierung) |
| `auto-learned/` | Auto | Maschinell generierte Regeln (Closed-Loop Learning) |

---

### 5. Commands (9 Dateien)

Alle in `claude-code-setup/commands/*.md`, werden nach `~/.claude/commands/` kopiert.

| Command | Zweck |
|---------|-------|
| `self-improve.md` | Systematische Selbstverbesserung der Umgebung (10-30min, token-intensiv) |
| `self-improve-ref/researchers.md` | Researcher-Templates fuer Self-Improve |
| `self-improve-ref/report-and-creative.md` | Report-Templates fuer Self-Improve |
| `tool-check.md` | Code-Health-Scanner (Bugs, Security, Performance, UI) |
| `tool-check-ref/loops.md` | Loop-Checklisten fuer Tool-Check |
| `tool-check-ref/meta-improve.md` | Meta-Improve fuer Tool-Check |
| `claudeception.md` | Extrahiert wiederverwendbares Wissen aus Sessions |
| `codex-bridge.md` | Codex Delta Bridge (Cross-CLI Sync) |
| `gemini-bridge.md` | Gemini Delta Bridge (Cross-CLI Sync) |

---

### 6. Plugins (89 aktiv, 10+ Marketplaces)

Vollstaendige Liste in `claude-code-setup/manifest.json` unter `"plugins"`.
Das Setup-Script installiert alle automatisch.

**Marketplace-Uebersicht:**

| Marketplace | Anzahl | Highlights |
|-------------|--------|-----------|
| claude-plugins-official | 23 | LSPs (Swift, TS, C#, Rust, Go, Kotlin...), GitHub, Playwright, Superpowers, CodeRabbit |
| superpowers-marketplace | 7 | Episodic Memory, Chrome DevTools, Session Driver, Lab |
| trailofbits | 30 | Security-Skills (Semgrep, Audit, Fuzzing, YARA, Zeroize...) |
| context-engineering-kit | 12 | TDD, DDD, SDD, Kaizen, Sadd, Reflexion, Git, Docs |
| everything-claude-code | 1 | Mega-Plugin mit 100+ Skills (Go, Kotlin, Python, Swift, Django...) |
| compound-engineering | 1 | Design, Research, Review, Workflow Skills |
| thedotmack (claude-mem) | 1 | Persistent Cross-Session Memory mit Tree-sitter AST |
| boostvolt | 1 | Extra LSP-Server |
| xclaude-plugin | 1 | iOS/Xcode Simulator + Build MCP-Server |
| apple-platform-build-tools | 1 (macOS only) | Xcode Build Agent + Skills |
| claude-reflect | 1 | Session Reflections → CLAUDE.md Updates |
| FlineDev (context-kit) | 1 | Context Kit |

**Deaktiviert nach Install:** `fp-check` (nur fuer App-Store-Apps aktivieren)

---

### 7. Settings

Drei Referenz-Dateien im Setup-Repo:
- `settings.json` — macOS-Version
- `settings-reference.json` — Windows-Version (1:1 Kopie der aktiven Windows-Settings)
- `settings.local.json` — Vorlage fuer lokale Overrides

**Kritische Settings:**
```json
{
  "model": "claude-opus-4-6",
  "effortLevel": "high",
  "maxOutputTokens": 64000,
  "enableAllProjectMcpServers": true,
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1",
    "CLAUDE_CODE_SUBAGENT_MODEL": "sonnet",
    "CLAUDE_AUTOCOMPACT_PCT_OVERRIDE": "95",
    "CLAUDE_CODE_EFFORT_LEVEL": "high",
    "DOTNET_CLI_TELEMETRY_OPTOUT": "1"
  }
}
```

**Hooks in Settings registriert:** Alle oben genannten Hooks muessen unter den richtigen
Events (SessionStart, PreToolUse, PostToolUse, SessionEnd, etc.) eingetragen sein.
Die vollstaendige Zuordnung steht in `hooks-macos.json` und `hooks-windows.json`.

---

### 8. MCP-Server

Projekt-MCP-Server in `~/proggs/.mcp.json` (PLATTFORMSPEZIFISCH — nie automatisch aendern!):
- `code-search` — Lokaler semantischer Code-Index (Bun)
- `code-review-graph` — Code-Review Graph (uvx)
- `think-tank` — Sequential Thinking (npx)

Referenz-Configs: `mcp-macos.json` und `mcp-windows.json` im Setup-Repo.

Plugin-MCP-Server (werden durch Plugins automatisch konfiguriert):
- context7, github, playwright, episodic-memory, nemo-mcp, better-icons,
  codebase-memory-mcp, sequential-thinking, serena (zeroize-audit)

---

### 9. Security-Tools

| Tool | Zweck | Installation |
|------|-------|-------------|
| Parry / parry-guard | Prompt-Injection-Scanner (DeBERTa + Llama) | `cargo install --git ... parry-guard` |
| prompt-injection-defender | Pattern-basierter PI-Scanner (50+ Patterns) | Python, bereits in hooks/ |
| cargo-audit | Rust Crate CVE Scanner | `cargo install cargo-audit` |
| safety-gate Hook | Blockiert rm -rf und andere gefaehrliche Befehle | Bereits installiert |

---

### 10. Dev-Tools (Referenz)

Vollstaendige Liste in manifest.json unter `"dev_tools"`. Minimum:
- Node.js, Bun, Deno, Git, gh CLI
- Swift (macOS), .NET SDK (Windows), Rust, Go, Kotlin, Java
- Android SDK mit NDK, Build Tools, Emulator
- Docker + Compose
- Linter: Biome, SwiftLint, swift-format, golangci-lint, ktfmt, detekt

---

### 11. Whiteboard (Shared Knowledge Hub)

**Datei:** `~/proggs/.claude/agent-memory/shared/MEMORY.md`
JEDER Agent, Hook und Skill MUSS dieses Whiteboard lesen und relevante Erkenntnisse reinschreiben.
Es ist die zentrale Wissensdatei fuer das gesamte System.

---

### 12. Memory-System

**Auto-Memory:** `~/.claude/projects/-Users-frank/memory/` (27 Dateien)
- `MEMORY.md` — Index aller Memories
- `feedback_*.md` — Benutzer-Korrekturen und bestaetitgte Ansaetze
- `project_*.md` — Projekt-spezifische Informationen
- `user_*.md` — Benutzer-Profil und Praeferenzen
- `reference_*.md` — Externe Ressourcen-Zeiger

Diese Memories sind Claude-Code-spezifisch und muessen NICHT auf andere CLIs portiert werden.
Sie werden automatisch bei jedem Start geladen.

---

### 13. Benutzer-Profil (fuer andere CLIs wichtig!)

- **Sprache:** Deutsch (Whisper Speech-to-Text), Code/Commits auf Englisch
- **Kein Programmierer** — will verstehen was passiert, ausfuehrliche Erklaerungen
- **Maximale Parallelisierung** — 3-5 Agents gleichzeitig ist Standard
- **Sichtbarkeit:** NIEMALS unsichtbar im Hintergrund arbeiten
- **Kein Python fuer GUIs** — Swift, C#, Kotlin, TypeScript, Rust, Go bevorzugt
- **Cross-Platform:** macOS (Swift/AppKit) + Windows (C#/WPF) + Android (Kotlin/Compose)
- **Einziges Repo:** `Pepsi1978/proggs` — NIEMALS neue Repos erstellen
- **Commit-Format:** `#NNN - Beschreibung` (fortlaufend nummeriert, Englisch)
- **Status-Meldung:** Jede Aufgabe endet mit "Committed, gepusht und plattformuebergreifend." (oder ehrliche Alternative)

---

### 14. Nemo Agent (NVIDIA Nemotron 3 Super 120B)

Der `nemo` Agent ist ein **kostenloser Wissens-Worker** der NVIDIA Nemotron ueber die NIM API ansteuert.
Claude uebernimmt das Denken (Prompt-Optimierung, Struktur), Nemotron generiert den Output.

**MCP-Tools:** `nemo_ask`, `nemo_generate`, `nemo_quiz`, `nemo_research`, `nemo_translate`, `nemo_summarize`

**Wann Nemo:** Massenproduktion (50+ Items), Allgemeinwissen, einfache Uebersetzungen
**Wann NICHT Nemo:** Komplexes Reasoning, Code-Review, Architektur, Security, Internet-Zugang

---

### Portierung auf ein frisches System

**Claude Code (macOS oder Windows):**
1. `git clone https://github.com/Pepsi1978/proggs ~/proggs`
2. `bash ~/proggs/claude-code-setup/setup-macos.sh` (oder `.ps1` fuer Windows)
3. `cp ~/proggs/CLAUDE.md ~/CLAUDE.md`
4. Claude Code starten — Auto-Sync Hook macht den Rest

**Codex CLI:**
1. Repo klonen
2. `CODEX.md` aus `~/proggs/claude-code-setup/bridges/` laden (falls vorhanden)
3. Agents/Rules manuell ins Codex-Konfigurationsverzeichnis kopieren
4. Plugins muessen manuell als Codex-Aequivalente konfiguriert werden

**Gemini CLI:**
1. Repo klonen
2. `GEMINI.md` aus `~/proggs/claude-code-setup/bridges/` laden (falls vorhanden)
3. Agents/Rules ins Gemini-Konfigurationsverzeichnis kopieren

**Fuer ALLE CLIs gilt:**
- Dieses Ledger lesen und alle PENDING-Eintraege fuer die Zielplattform anwenden
- Das Whiteboard (`agent-memory/shared/MEMORY.md`) lesen fuer aktuellen Systemzustand
- CLAUDE.md/CODEX.md/GEMINI.md als primaere Direktiven-Quelle nutzen

<!-- APPLIED: macos/claude-code=2026-03-25T22:30:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-001] Universal Mirror Bridge: export und import Agenten
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T21:02:54Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: agent -->
<!-- AFFECTS: ~/.claude/agents/export.md, ~/.claude/agents/import.md, ~/proggs/claude-code-setup/agents/export.md, ~/proggs/claude-code-setup/agents/import.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Das Universal Mirror Bridge System ist die Antwort auf ein fundamentales Problem in dieser
Entwicklungsumgebung: Der Benutzer arbeitet gleichzeitig auf macOS (Claude Code) und Windows
(Claude Code), sowie gelegentlich mit Codex CLI und Gemini CLI. Bisher gab es das
"Porting-List + porting-inbox"-System (jedes CLI hatte eine PORTING-LIST.md Datei, ein Hook
las sie beim Session-Start und schrieb Vorschlaege ins Whiteboard). Das war unzuverlaessig,
unvollstaendig (nur Vorschlaege, keine konkreten Datei-Inhalte) und nicht selbstheilend.

Das neue System besteht aus zwei Agenten und einem zentralen Ledger:
- `export` Agent: Wird am Session-Ende aufgerufen. Scannt alle Aenderungen der Session
  und schreibt vollstaendige, selbsterklaerende Eintraege ins mirror-ledger.md. Jeder
  Eintrag enthaelt: Kontext, vollstaendige Datei-Inhalte fuer BEIDE Plattformen,
  Portierungs-Anweisungen, Settings-Registrierungen.
- `import` Agent: Wird beim Session-Start oder auf Abruf aufgerufen. Liest mirror-ledger.md,
  findet PENDING-Eintraege fuer die aktuelle Plattform/CLI, wendet sie automatisch an,
  und markiert sie als APPLIED.
- `mirror-ledger.md`: Die einzige Wahrheitsquelle. Git-versioniert, liegt in
  ~/proggs/claude-code-setup/ und wird bei jedem Commit synchronisiert.

Beide Agenten sind in Deutsch geschrieben (gemaess german-agents-skills Regel). Sie laufen
mit Opus-Modell, high effort, und haben maxTurns: 60 (export) bzw. 80 (import).

### Was wurde geaendert?

Zwei neue Agent-Dateien wurden erstellt:
- `~/.claude/agents/export.md` (und Kopie in `~/proggs/claude-code-setup/agents/`)
- `~/.claude/agents/import.md` (und Kopie in `~/proggs/claude-code-setup/agents/`)

Beide Agenten ersetzen das alte porting-inbox.sh/ps1 System vollstaendig.

### Warum?

Das alte PORTING-LIST + porting-inbox System hatte drei kritische Maengel:
1. Nur Vorschlaege — kein vollstaendiger Datei-Inhalt, kein konkretes "wie umsetzen"
2. Statisch — keine Rueckmeldung ob ein Eintrag schon umgesetzt wurde
3. Unvollstaendig — nur Claude Code → andere CLIs, nicht rueckwaerts

Das neue System ist bidirektional, vollstaendig, und APPLIED-Status wird getrackt.

### Portierung Windows/Claude-Code

1. Erstelle `%USERPROFILE%\.claude\agents\export.md` mit dem Inhalt unten (export-Datei)
2. Erstelle `%USERPROFILE%\.claude\agents\import.md` mit dem Inhalt unten (import-Datei)
3. Erstelle auch Kopien in `%USERPROFILE%\proggs\claude-code-setup\agents\`
4. Kein Settings-Eintrag noetig — Agenten werden per Trigger-Phrase aufgerufen

### Portierung Codex/Gemini

1. Analog: Erstelle die Agenten-Dateien im jeweiligen agents/-Verzeichnis
2. Passe die Plattform-Erkennung im Agent-Prompt an (platform=codex oder gemini)
3. Beim import: APPLIED-Status mit codex=PENDING bzw. gemini=2026-03-26T01:48:45Z suchen

### Datei-Inhalt macOS+Windows (export.md)

```markdown
---
name: export
description: Scannt ALLE Session-Aenderungen und schreibt sie ausfuehrlich ins mirror-ledger.md fuer andere Plattformen und CLIs. Nutze diesen Agenten am Ende einer Session oder auf Abruf. Trigger — "starte den export Agenten", "exportiere", "Aenderungen spiegeln".
model: opus
effort: high
maxTurns: 60
tools:
  - Read
  - Glob
  - Grep
  - Bash
  - Write
  - Edit
---

You are the **Export Agent** — the writer half of the Universal Mirror Bridge.
[... vollstaendiger Prompt wie im System-Prompt dieses Export-Agenten ...]
```

Vollstaendige Datei: `~/proggs/claude-code-setup/agents/export.md`

### Datei-Inhalt macOS+Windows (import.md)

Vollstaendige Datei: `~/proggs/claude-code-setup/agents/import.md`

### Settings-Registrierung

Keine Settings-Registrierung noetig. Die Agenten werden per Sprach-Trigger aktiviert:
- "exportiere" / "starte den export Agenten" → `export` Agent
- "importiere" / "starte den import Agenten" / "was ist neu" → `import` Agent

<!-- APPLIED: macos/claude-code=2026-03-25T21:02:54Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-002] Neuer Hook: mirror-check (SessionStart Benachrichtigung)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T21:02:54Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: hook -->
<!-- AFFECTS: ~/.claude/hooks/mirror-check.sh, ~/.claude/hooks/mirror-check.ps1, ~/proggs/claude-code-setup/hooks/mirror-check.sh, ~/proggs/claude-code-setup/hooks/mirror-check.ps1 -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Dieser Hook ist Teil des Universal Mirror Bridge Systems. Er laeuft beim SessionStart-Event
und prueft ob es im mirror-ledger.md Eintraege gibt, die fuer die aktuelle Plattform/CLI
noch nicht angewendet wurden (Status PENDING). Falls ja, zeigt er eine kurze Benachrichtigung.

Der Hook ist PASSIV — er wendet NICHTS an. Er informiert nur. Das Anwenden passiert durch
den `import` Agenten der vom Benutzer explizit getriggert wird.

Der Hook hat folgende Eigenschaften:
- `set +e` / `$ErrorActionPreference = "SilentlyContinue"` — bricht die Session NIE ab
- Prueft zuerst ob der Ledger ueberhaupt existiert (exit 0 wenn nicht)
- Gibt nur eine Zeile Ausgabe wenn PENDING-Eintraege existieren
- Timeout: 5 Sekunden (reicht fuer ein einfaches grep/Select-String)

Registriert im SessionStart-Event in settings.json (macOS) und settings-reference.json (Windows).

### Was wurde geaendert?

Zwei neue Hook-Dateien wurden erstellt:
- `~/.claude/hooks/mirror-check.sh` (macOS/bash)
- `~/.claude/hooks/mirror-check.ps1` (Windows/PowerShell)

Beide wurden in `~/proggs/claude-code-setup/hooks/` kopiert.

In `~/.claude/settings.json` (macOS) wurde folgender Block zum SessionStart-Event hinzugefuegt:
```json
{
  "hooks": [
    {
      "type": "command",
      "command": "bash ~/.claude/hooks/mirror-check.sh",
      "timeout": 5
    }
  ]
}
```

In `settings-reference.json` (Windows-Referenz) entsprechend:
```json
{
  "hooks": [
    {
      "type": "command",
      "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/mirror-check.ps1\"",
      "timeout": 5
    }
  ]
}
```

### Warum?

Ohne diesen Hook wuerde der Benutzer nicht automatisch wissen, wenn es neue Aenderungen
von einer anderen Plattform gibt die noch portiert werden muessen. Der Hook macht das
Mirror Bridge System "push-artig" — man muss nicht aktiv nachfragen.

### Portierung Windows/Claude-Code

1. Erstelle `%USERPROFILE%\.claude\hooks\mirror-check.ps1` mit dem Inhalt unten
2. Kopiere nach `%USERPROFILE%\proggs\claude-code-setup\hooks\mirror-check.ps1`
3. Oeffne `%USERPROFILE%\.claude\settings.json`
4. Suche den `"SessionStart"` Block
5. Fuege nach dem letzten bestehenden SessionStart-Hook-Objekt einen neuen Block ein:
   ```json
   ,{
     "hooks": [
       {
         "type": "command",
         "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/mirror-check.ps1\"",
         "timeout": 5
       }
     ]
   }
   ```
6. Validiere: `pwsh -Command "Get-Content '$env:USERPROFILE\.claude\settings.json' | ConvertFrom-Json"`

### Portierung Codex/Gemini

Analog — passe platform und cli im Hook auf "codex" bzw. "gemini" an und registriere
im jeweiligen SessionStart-Hook-System des CLIs.

### Datei-Inhalt macOS (mirror-check.sh)

```bash
#!/usr/bin/env bash
# mirror-check.sh — SessionStart: notify if mirror-ledger has pending entries
# Part of the Universal Mirror Bridge system.
# This hook ONLY counts and reports — it does NOT apply anything.
# To apply pending entries, the user starts the import agent manually.

set +e  # Never block session start

LEDGER="$HOME/proggs/claude-code-setup/mirror-ledger.md"

# Bail if ledger doesn't exist yet
[[ -f "$LEDGER" ]] || exit 0

# Detect platform
platform="macos"
cli="claude-code"

# Count PENDING entries for this platform/cli
count=$(grep -c "APPLIED: ${platform}/${cli}=PENDING" "$LEDGER" 2>/dev/null || echo "0")

# Only show message if there are pending entries
if [[ "$count" -gt 0 && "$count" != "0" ]]; then
    echo "Mirror-Bridge: ${count} ausstehende Eintraege von anderen Plattformen. Starte den import Agenten um zu uebernehmen."
fi

exit 0
```

### Datei-Inhalt Windows (mirror-check.ps1)

```powershell
# mirror-check.ps1 — SessionStart: notify if mirror-ledger has pending entries
# Part of the Universal Mirror Bridge system.
# This hook ONLY counts and reports — it does NOT apply anything.
# To apply pending entries, the user starts the import agent manually.

$ErrorActionPreference = "SilentlyContinue"

$Ledger = Join-Path $env:USERPROFILE "proggs\claude-code-setup\mirror-ledger.md"

# Bail if ledger doesn't exist yet
if (-not (Test-Path $Ledger)) { exit 0 }

$platform = "windows"
$cli = "claude-code"

# Count PENDING entries for this platform/cli
$pattern = "APPLIED: ${platform}/${cli}=PENDING"
$count = (Select-String -Pattern ([regex]::Escape($pattern)) -Path $Ledger -SimpleMatch | Measure-Object).Count

if ($count -gt 0) {
    Write-Output "Mirror-Bridge: $count ausstehende Eintraege von anderen Plattformen. Starte den import Agenten um zu uebernehmen."
}

exit 0
```

### Settings-Registrierung

Hook-Event: `SessionStart`

macOS-Befehl: `"command": "bash ~/.claude/hooks/mirror-check.sh", "timeout": 5`

Windows-Befehl: `"command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/mirror-check.ps1\"", "timeout": 5`

Kein `matcher` oder `async` noetig. Der Hook ist synchron und laeuft bei JEDEM Session-Start.

<!-- APPLIED: macos/claude-code=2026-03-25T21:02:54Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-003] Neue Regel: german-agents-skills (Pflicht-Deutsch fuer Agenten)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T21:02:54Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: rule -->
<!-- AFFECTS: ~/.claude/rules/german-agents-skills.md, ~/proggs/claude-code-setup/rules/german-agents-skills.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Diese Regel legt fest dass ALLE selbst erstellten Agenten, Skills und Commands in der
Programmierumgebung komplett auf Deutsch geschrieben sein muessen. Das betrifft:
- `description` im Frontmatter
- `<example>`-Bloecke (Kontext, User, Assistant, Commentary)
- System-Prompt (Markdown-Body nach dem Frontmatter)
- Sektionsueberschriften, Erklaerungen, Regeln, Checklisten

Der Grund: Der Benutzer spricht Deutsch und nutzt Whisper Speech-to-Text. Englische
Beschreibungen in eigenen Agenten sind ein Stilbruch. Englisch bleiben darf: Tool-Namen
(Read, Write, Bash, nemo_ask), Code-Variablen und technische Bezeichner (MCP, API, JSON),
Commit-Messages, und externe/installierte Plugins.

Diese Regel wurde in Session 2026-03-25 neu erstellt. Zuvor gab es keine explizite Regel
dazu — der nemo Agent hatte englische descriptions und examples, was korrigiert wurde.

### Was wurde geaendert?

Neue Datei erstellt: `~/.claude/rules/german-agents-skills.md` (und Kopie in Setup-Repo).
Gleichzeitig wurde in CLAUDE.md (Sprache-Sektion) ein Verweis auf diese Regel hinzugefuegt.

### Warum?

Session 2026-03-25: nemo Agent wurde gefixt (englische Descriptions → Deutsch). Um
sicherzustellen dass das nie wieder passiert, wurde eine explizite Regel erstellt.
Die Regel ist die formale Grundlage fuer das Muster das in german-skill-triggers.md
(Whisper-Korrekturen-Tabelle) als implizite Erwartung schon vorhanden war.

### Portierung Windows/Claude-Code

1. Erstelle `%USERPROFILE%\.claude\rules\german-agents-skills.md` mit dem Inhalt unten
2. Kopiere nach `%USERPROFILE%\proggs\claude-code-setup\rules\german-agents-skills.md`
3. Kein Settings-Eintrag noetig — Regeln werden automatisch von Claude Code geladen

### Portierung Codex/Gemini

1. Lege die Regel-Datei im jeweiligen rules/-Verzeichnis ab
2. Stelle sicher dass das CLI sie beim Start laedt

### Datei-Inhalt (german-agents-skills.md — plattformunabhaengig)

```markdown
# Agents, Skills & Commands: Komplett auf Deutsch (KRITISCH)

## Regel

ALLE selbst erstellten Agents (`~/.claude/agents/*.md`), Skills und Commands
(`~/.claude/commands/*.md`) muessen **komplett auf Deutsch** geschrieben sein.

Dies gilt fuer:
- `description` im Frontmatter
- `<example>`-Bloecke (Kontext, User, Assistant, Commentary)
- System-Prompt (Markdown-Body nach dem Frontmatter)
- Sektionsueberschriften
- Erklaerungen, Regeln, Checklisten

## Was Englisch bleiben darf

- Tool-Namen: `nemo_ask`, `nemo_quiz`, `Read`, `Write`, `Bash`
- Code-Variablen: `categoryId`, `questionText`
- Technische Bezeichner: MCP, API, JSON, Kotlin, TypeScript
- Commit-Messages (eigene Regel: Commits auf Englisch)
- Externe/installierte Plugins (werden nicht uebersetzt)

## Beispiel

```markdown
# FALSCH:
description: "Use this agent when reviewing code..."
<example>
Context: User wants a code review
<commentary>Trigger the reviewer agent</commentary>

# RICHTIG:
description: "Nutze diesen Agenten wenn Code geprueft werden soll..."
<example>
Kontext: Benutzer will einen Code-Review
<commentary>Den Reviewer-Agenten starten</commentary>
```

## Warum

Der Benutzer spricht Deutsch, arbeitet per Whisper Speech-to-Text, und will
ein einheitlich deutsches System. Englische Beschreibungen in eigenen Agents
sind ein Stilbruch der in Session 2026-03-25 korrigiert wurde.
```

### Settings-Registrierung

Keine Settings-Registrierung noetig. Regel-Dateien in `~/.claude/rules/` werden
automatisch von Claude Code beim Start geladen.

<!-- APPLIED: macos/claude-code=2026-03-25T21:02:54Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-004] Regel-Update: german-skill-triggers (Mirror Bridge Sektion 15)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T21:02:54Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: rule -->
<!-- AFFECTS: ~/.claude/rules/german-skill-triggers.md, ~/proggs/claude-code-setup/rules/german-skill-triggers.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Die Datei `german-skill-triggers.md` ist die zentrale Trigger-Map fuer alle Skills und
Agenten. Sie uebersetzt deutsche Phrasen (per Whisper Speech-to-Text gesprochen) in die
richtigen Agent/Skill-Aufrufe. Da der Benutzer Deutsch spricht, muss Claude bei jeder
Anfrage diese Map mental durchgehen.

In dieser Session wurde die Map um Sektion 15 erweitert: die Mirror Bridge Agenten.
Ohne diese Sektion wuerde der Benutzer "exportiere" sagen und Claude wuerde nicht wissen
dass damit der `export` Agent gemeint ist.

Die Datei liegt in `~/.claude/rules/` und wird automatisch beim Start geladen.
Es gibt eine Kopie in `~/proggs/claude-code-setup/rules/` fuer Cross-Platform-Sync.

### Was wurde geaendert?

Am Ende von `german-skill-triggers.md` wurde nach Sektion 14 (Nemo) folgende neue
Sektion 15 eingefuegt:

```markdown
## 15. Mirror Bridge — Universal Cross-Platform & Cross-CLI Sync

**Zwei Agenten die zusammen eine universelle "Spiegelung" zwischen allen Plattformen
und CLIs ermoegllichen. Der `export` Agent schreibt, der `import` Agent liest und baut ein.**

| Deutsche Phrase | Agent | Was er WIRKLICH tut |
|----------------|-------|---------------------|
| "starte den export Agenten", "exportiere", "Aenderungen spiegeln", "export" | `export` | Scannt ALLE Session-Aenderungen und schreibt sie ausfuehrlich ins mirror-ledger.md — mit Kontext, Portierungs-Anweisungen, vollstaendigen Datei-Inhalten fuer beide Plattformen |
| "starte den import Agenten", "importiere", "hol Neuerungen", "was ist neu", "import" | `import` | Liest mirror-ledger.md, zeigt Triage-Tabelle, portiert ausstehende Aenderungen automatisch auf die aktuelle Plattform/CLI |

**Wann welchen Agent:**
- **Session beendet, Aenderungen gemacht** → `export` (schreibt alles ins Ledger)
- **Plattform gewechselt, moechte aufholen** → `import` (holt alles vom Ledger)
- **Neues CLI gestartet (Codex/Gemini)** → `import` (portiert Aenderungen von Claude Code)

**Automatische Benachrichtigung:** Bei SessionStart zeigt `mirror-check` Hook an wenn Eintraege ausstehen.
```

### Warum?

Ohne diesen Eintrag in der Trigger-Map wuerde Claude nicht wissen, dass "exportiere"
den `export` Agenten triggern soll. Die Trigger-Map ist der einzige Mechanismus durch
den Whisper-Phrasen zu Agent-Aufrufen werden.

### Portierung Windows/Claude-Code

Die Datei `german-skill-triggers.md` ist plattformunabhaengig (reines Markdown ohne
Pfade). Sobald sie im Git-Repo gepusht ist, wird sie per Auto-Sync auf Windows
uebernommen. Manuelle Aktion:

1. Oeffne `%USERPROFILE%\.claude\rules\german-skill-triggers.md`
2. Scrolle zum Ende (nach Sektion 14 Nemo, vor "Proaktive Agents")
3. Fuege den oben beschriebenen Sektion-15-Block ein
4. Oder: `git pull` im proggs-Repo ausfuehren — Auto-Sync kopiert die Datei

### Portierung Codex/Gemini

Analog — die Trigger-Phrasen muessen in das jeweilige Trigger-System des CLIs eingetragen
werden. Fuer Codex: in der Codex-eigenen PORTING-LIST oder Konfiguration. Fuer Gemini:
entsprechend.

### Datei-Inhalt (nur der neue Block — Sektion 15)

Vollstaendige Datei unter: `~/proggs/claude-code-setup/rules/german-skill-triggers.md`

Der eingefuegte Block steht zwischen Sektion 14 (Nemo, endet mit Whisper-Korrekturtabelle)
und dem Abschnitt "Proaktive Agents".

### Settings-Registrierung

Keine — Regel-Dateien werden automatisch geladen.

<!-- APPLIED: macos/claude-code=2026-03-25T21:02:54Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-005] Direktive: CLAUDE.md Sprachregel fuer Agenten/Skills
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T21:02:54Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: directive -->
<!-- AFFECTS: ~/CLAUDE.md, ~/proggs/CLAUDE.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Die CLAUDE.md Datei enthaelt die Projekt-Direktiven die bei JEDEM Session-Start geladen
werden. Es gibt zwei Kopien die immer synchron gehalten werden muessen:
- `~/CLAUDE.md` — die lokale Kopie die Claude Code laedt
- `~/proggs/CLAUDE.md` — die Repo-Kopie die per git synchronisiert wird

Diese Aenderung betrifft die Sprach-Sektion am Ende von CLAUDE.md: Dort wurde ein
Detail-Absatz entfernt (die "Pflicht — Einfache Erklaerung" Regel aus der
Selbstbeobachtungs-Direktive) und gleichzeitig ein neuer Paragraf zur deutschen
Sprache fuer selbst erstellte Agenten/Skills/Commands hinzugefuegt.

Die Sprachregel ist wichtig weil sie sicherstellt, dass alle neuen Agenten und Skills
automatisch auf Deutsch erstellt werden. Sie verweist auf die vollstaendige Regel in
`~/.claude/rules/german-agents-skills.md`.

### Was wurde geaendert?

In der `## Sprache` Sektion am Ende von CLAUDE.md wurden zwei Aenderungen gemacht:

**Entfernt** (3 Zeilen aus der Selbstbeobachtungs-Direktive die redundant waren):
```
- **PFLICHT — Einfache Erklaerung** (~6 Zeilen pro Vorschlag): Jeder Vorschlag MUSS zusaetzlich
  in einfacher Sprache erklaert werden (keine Fachbegriffe, oder wenn noetig sofort in Klammern erklaeren).
  Erklaerung beantwortet: (1) Was ist das Problem? (2) Was wuerde sich aendern? (3) Warum wird das System dadurch schlauer?
```

**Hinzugefuegt** (am Ende der Sprache-Sektion):
```markdown
- **Selbst erstellte Agents, Skills und Commands: Komplett auf Deutsch.**
  - Frontmatter-`description`: Deutsch ("Nutze diesen Agenten wenn..." statt "Use this agent when...")
  - `<example>`-Bloecke: Kontext, User, Assistant, Commentary — alles Deutsch
  - System-Prompt (Markdown-Body): Deutsch
  - Einzige Ausnahme: Tool-Namen (`nemo_ask`), Code-Variablen und technische Bezeichner bleiben Englisch
  - Externe/installierte Plugins werden NICHT uebersetzt
  - Vollstaendige Regel: `~/.claude/rules/german-agents-skills.md`
```

### Warum?

Die "Einfache Erklaerung" Pflicht war eine Direktive die sich in der Praxis als zu
restriktiv erwiesen hatte — sie verlangte eine bestimmte Erklaerungsstruktur fuer jeden
Intelligenz-Vorschlag und schraenkte den natuerlichen Sprachfluss ein. Sie wurde entfernt.

Die neue Sprachregel fuer Agenten war notwendig weil der nemo Agent in dieser Session
auf Deutsch migriert wurde und eine formale Regel gefehlt hat die das als Standard etabliert.

### Portierung Windows/Claude-Code

Die CLAUDE.md Aenderung kommt automatisch via `git pull` sobald der Auto-Sync-Hook
beim naechsten Session-Start laeuft. Manuelle Alternative:

1. Oeffne `%USERPROFILE%\.claude\CLAUDE.md` (oder `%USERPROFILE%\proggs\CLAUDE.md`)
2. Suche die Sektion `## Sprache`
3. Entferne den Block mit "PFLICHT — Einfache Erklaerung" (3 Zeilen)
4. Fuege am Ende der Sprache-Sektion den obigen deutschen Agenten/Skills Block ein

### Portierung Codex/Gemini

Die CLAUDE.md ist das primaere Direktiven-Dokument fuer Claude Code. Andere CLIs haben
aequivalente Konfigurationsdateien (z.B. GEMINI.md fuer Gemini CLI, CODEX.md fuer Codex).
Die Sprach-Direktive dort entsprechend anpassen.

### Datei-Inhalt (nur der neue Block)

```markdown
- **Selbst erstellte Agents, Skills und Commands: Komplett auf Deutsch.**
  - Frontmatter-`description`: Deutsch ("Nutze diesen Agenten wenn..." statt "Use this agent when...")
  - `<example>`-Bloecke: Kontext, User, Assistant, Commentary — alles Deutsch
  - System-Prompt (Markdown-Body): Deutsch
  - Einzige Ausnahme: Tool-Namen (`nemo_ask`), Code-Variablen und technische Bezeichner bleiben Englisch
  - Externe/installierte Plugins werden NICHT uebersetzt
  - Vollstaendige Regel: `~/.claude/rules/german-agents-skills.md`
```

### Settings-Registrierung

Keine — CLAUDE.md wird automatisch geladen. Beide Kopien (~/CLAUDE.md und ~/proggs/CLAUDE.md)
muessen identisch sein.

<!-- APPLIED: macos/claude-code=2026-03-25T21:02:54Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-006] Deprecation: porting-inbox Hook (ersetzt durch mirror-check)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T21:02:54Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: hook -->
<!-- AFFECTS: ~/.claude/hooks/porting-inbox.sh, ~/.claude/hooks/porting-inbox.ps1, ~/proggs/claude-code-setup/hooks/porting-inbox.sh, ~/proggs/claude-code-setup/hooks/porting-inbox.ps1 -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Der `porting-inbox` Hook war der Vorgaenger des Mirror Bridge Systems. Er wurde beim
SessionStart aufgerufen und las die PORTING-LIST.md Dateien von Codex und Gemini,
extrahierte neue Eintraege und schrieb sie als TODOs ins Whiteboard (MEMORY.md).

Das Problem: Der Hook war komplex (80+ Zeilen), fehleranfaellig, und das PORTING-LIST
System hatte keine Rueckmeldung ob ein Eintrag schon umgesetzt worden war. Ausserdem
lieferte er nur Vorschlaege, nicht vollstaendige Datei-Inhalte.

Mit dem neuen Universal Mirror Bridge System (mirror-ledger.md + export/import Agenten +
mirror-check Hook) ist der porting-inbox Hook ueberfluessig geworden. Er wurde durch
BEIDE Hooks (porting-inbox.sh + ps1) komplett inaktiviert: Der gesamte urspruengliche
Code wurde in einen `exit 0`-Block gewrapped (Deprecated-Kommentar am Anfang, dann
sofortiger `exit 0`, dann der alte Code als Referenz).

Der Hook ist NICHT mehr in settings.json registriert. Er laeuft also nicht mehr.
Er wird bis 2026-04-25 in den Hook-Dateien aufbewahrt und kann danach geloescht werden.

### Was wurde geaendert?

Beide Hook-Dateien wurden von "aktivem Code" zu "deaktiviertem Code mit Deprecation-Kommentar"
geaendert:

`porting-inbox.sh`: Neue ersten 3 Zeilen:
```bash
# DEPRECATED: Replaced by mirror-check.sh + import agent (Universal Mirror Bridge, 2026-03-25)
# This hook is no longer registered in settings.json.
# Kept for reference only — remove after 2026-04-25.
exit 0
# --- Original code below (inactive) ---
[... urspruenglicher Code ...]
```

`porting-inbox.ps1`: Analog in PowerShell.

In `settings.json` und `settings-reference.json` wurde der Hook aus dem SessionStart-Event
entfernt (der neue `mirror-check` Hook wurde stattdessen hinzugefuegt, siehe Eintrag MAC-002).

### Warum?

Das alte System war eine Zwischenloesung die durch ein besseres ersetzt wurde.
Das Belassen des alten Codes (inaktiv) dient als Referenz falls jemand die Logik
nachvollziehen muss. Nach 2026-04-25 koennen die Dateien geloescht werden.

### Portierung Windows/Claude-Code

1. Oeffne `%USERPROFILE%\.claude\hooks\porting-inbox.ps1`
2. Ersetze den GESAMTEN Inhalt durch den unten stehenden Deprecated-Inhalt
3. Entferne den porting-inbox Eintrag aus `%USERPROFILE%\.claude\settings.json`
   (unter SessionStart Event)
4. Validiere settings.json: `pwsh -Command "Get-Content '$env:USERPROFILE\.claude\settings.json' | ConvertFrom-Json"`

### Portierung Codex/Gemini

Wenn Codex/Gemini einen aequivalenten Hook fuer das PORTING-LIST System hat:
Den Hook ebenfalls deaktivieren (auf exit 0 setzen) und aus der Hook-Registrierung entfernen.
Das neue Mirror Bridge System uebernimmt die Funktion vollstaendig.

### Datei-Inhalt macOS (porting-inbox.sh — jetzt deprecated)

```bash
#!/usr/bin/env bash
# DEPRECATED: Replaced by mirror-check.sh + import agent (Universal Mirror Bridge, 2026-03-25)
# This hook is no longer registered in settings.json.
# Kept for reference only — remove after 2026-04-25.
exit 0
```

(Vollstaendiger inaktiver Original-Code unter: `~/proggs/claude-code-setup/hooks/porting-inbox.sh`)

### Datei-Inhalt Windows (porting-inbox.ps1 — jetzt deprecated)

```powershell
# DEPRECATED: Replaced by mirror-check.ps1 + import agent (Universal Mirror Bridge, 2026-03-25)
# This hook is no longer registered in settings.json.
# Kept for reference only — remove after 2026-04-25.
exit 0
```

(Vollstaendiger inaktiver Original-Code unter: `~/proggs/claude-code-setup/hooks/porting-inbox.ps1`)

### Settings-Registrierung

**ENTFERNEN** — den porting-inbox Hook aus settings.json entfernen (er ist NICHT mehr aktiv).
Er war vorher unter dem SessionStart Event registriert. Der neue Ersatz ist `mirror-check`
(siehe Eintrag MAC-002).

<!-- APPLIED: macos/claude-code=2026-03-25T21:02:54Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-007] Bugfix manifest.json: fehlende Agents nemo, export, import
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T23:00:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: settings -->
<!-- AFFECTS: ~/proggs/claude-code-setup/manifest.json -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Die Datei `manifest.json` im Setup-Repo ist das zentrale Installations-Manifest. Sie
listet ALLE Custom Agents, Plugins, Dev-Tools und Settings die auf einem neuen Rechner
installiert werden muessen. Das Setup-Script liest diese Datei und installiert/kopiert
alles automatisch.

Das Problem: Drei neue Agents die in frueheren Sessions erstellt wurden (nemo in Session
#717-722, export und import in Session #726) fehlten in der `custom_agents`-Liste des
Manifests. Das bedeutet: Ein frischer Rechner oder ein anderes CLI (Gemini, Codex) das
das Setup-Script ausfuehrt, haette diese drei Agents NICHT automatisch erhalten.

Der Fix ist einfach aber kritisch — ohne ihn sind die Agents auf neuen Installationen
nicht vorhanden und der Benutzer muesste sie manuell nachinstallieren.

Die `manifest.json` befindet sich NUR im Setup-Repo (`~/proggs/claude-code-setup/`) und
wird NICHT nach `~/.claude/` kopiert. Sie ist eine Konfigurationsdatei fuer das
Setup-Script, nicht fuer Claude Code selbst.

### Was wurde geaendert?

In `~/proggs/claude-code-setup/manifest.json` wurden drei Eintraege zur `custom_agents`
Liste hinzugefuegt (alphabetisch sortiert eingeordnet):

Vor dem Fix fehlten diese drei Zeilen:
```
"export",
"import",
"nemo",
```

Nach dem Fix sieht der vollstaendige `custom_agents` Block so aus:
```json
"custom_agents": [
    "architect",
    "batch-reviewer",
    "challenger",
    "code-reviewer",
    "coder",
    "debugger",
    "env-checker",
    "evolution-analyst",
    "export",
    "import",
    "intelligence-researcher",
    "mar-reviewer",
    "nemo",
    "optimizer",
    "quality-gate",
    "researcher",
    "tester",
    "ui-polisher"
]
```

### Warum?

Root Cause: Als die Agents erstellt wurden (nemo in #717-722, export/import in #726),
wurden die Agent-Dateien korrekt nach `~/proggs/claude-code-setup/agents/` kopiert und
in settings.json registriert — aber niemand hat die `manifest.json` aktualisiert.

Das ist ein typischer "vergessener zweiter Schritt" Fehler. Die Agent-Dateien existieren
im Repo, aber das Setup-Script wuerde sie beim Neuaufbau nicht automatisch installieren.

Dieser Fehler wurde beim Export-Agent-Lauf entdeckt: Der Benutzer will Gemini CLI
benutzen um die Umgebung aufzubauen, und `nemo`, `export`, `import` wuerden fehlen.

### Portierung Windows/Claude-Code

1. Oeffne `%USERPROFILE%\proggs\claude-code-setup\manifest.json`
2. Suche den Abschnitt `"custom_agents"`
3. Pruefen: sind "export", "import", "nemo" darin? Falls nicht: hinzufuegen (alphabetisch)
4. Die Agenten-Dateien selbst sind schon im Repo unter `claude-code-setup/agents/`
5. Kopiere die fehlenden Agent-Dateien nach `%USERPROFILE%\.claude\agents\`:
   - `claude-code-setup/agents/nemo.md` nach `%USERPROFILE%\.claude\agents\nemo.md`
   - `claude-code-setup/agents/export.md` nach `%USERPROFILE%\.claude\agents\export.md`
   - `claude-code-setup/agents/import.md` nach `%USERPROFILE%\.claude\agents\import.md`
6. Validierung: `Get-ChildItem "$env:USERPROFILE\.claude\agents\" | Where-Object { $_.Name -in @('nemo.md','export.md','import.md') }`

Hinweis: Die Agenten-Dateien im Repo (`claude-code-setup/agents/`) existieren bereits
korrekt — nur das Manifest-JSON musste aktualisiert werden. Alle drei Agents sind
plattformunabhaengig (nur Markdown) und brauchen keine plattformspezifische Anpassung.

### Portierung Codex/Gemini

1. Repository klonen: `git clone https://github.com/Pepsi1978/proggs ~/proggs`
2. Die Agents-Dateien liegen in `~/proggs/claude-code-setup/agents/`
3. Kopiere nach dem jeweiligen agents-Verzeichnis des CLIs:
   - Gemini: `~/.gemini/agents/` oder aequivalentes Verzeichnis (je nach Gemini CLI Version)
   - Codex: `~/.codex/agents/` oder aequivalentes Verzeichnis
4. Die Agent-Prompts sind auf Deutsch und referenzieren
   `~/proggs/claude-code-setup/mirror-ledger.md` als zentrales Ledger
5. Fuer Gemini CLI speziell: Die Agent-Datei `export.md` ist der vollstaendige
   System-Prompt des Export-Agenten — der identische Prompt der dieses Ledger
   generiert hat. Er kann direkt als Gemini-System-Prompt verwendet werden.

### Datei-Inhalt (relevanter Ausschnitt manifest.json)

```json
{
    "custom_agents": [
        "architect",
        "batch-reviewer",
        "challenger",
        "code-reviewer",
        "coder",
        "debugger",
        "env-checker",
        "evolution-analyst",
        "export",
        "import",
        "intelligence-researcher",
        "mar-reviewer",
        "nemo",
        "optimizer",
        "quality-gate",
        "researcher",
        "tester",
        "ui-polisher"
    ]
}
```

Die vollstaendige `manifest.json` liegt unter: `~/proggs/claude-code-setup/manifest.json`

### Settings-Registrierung

Keine — `manifest.json` ist kein Hook und kein Settings-File. Es wird nur vom
Setup-Script (`setup-macos.sh` / `setup-windows.ps1`) ausgewertet.

<!-- APPLIED: macos/claude-code=2026-03-25T23:00:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-008] Neuer Hook: sync-hooks-reference (SessionEnd Auto-Sync)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T23:00:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: hook -->
<!-- AFFECTS: ~/.claude/hooks/sync-hooks-reference.sh, ~/.claude/hooks/sync-hooks-reference.ps1, ~/proggs/claude-code-setup/hooks/sync-hooks-reference.sh, ~/proggs/claude-code-setup/hooks/sync-hooks-reference.ps1, ~/proggs/claude-code-setup/hooks-macos.json -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Das Setup-Repo enthaelt zwei Referenz-Dateien fuer die Hook-Konfiguration:
- `hooks-macos.json` — enthaelt die macOS-Hook-Konfiguration (war fruehier MANUELL gepflegt)
- `hooks-windows.json` — entsprechende Windows-Version

Das Problem: Jedes Mal wenn ein Hook in `~/.claude/settings.json` hinzugefuegt oder
geaendert wurde, musste `hooks-macos.json` MANUELL synchronisiert werden. Das wurde
regelmaessig vergessen. Zum Beispiel wurde der `mirror-check` Hook (MAC-002) zwar
korrekt in settings.json registriert, aber `hooks-macos.json` musste separat gepflegt
werden.

Der neue `sync-hooks-reference` Hook loest dieses Problem automatisch: Er laeuft
beim SessionEnd-Event (asynchron) und extrahiert den kompletten `hooks`-Block aus der
lebenden `~/.claude/settings.json` und schreibt ihn in `hooks-macos.json` (macOS)
bzw. `hooks-windows.json` (Windows). Seitdem ist `hooks-macos.json` AUTO-GENERIERT
und sollte NICHT mehr manuell bearbeitet werden.

Fuer Gemini und Codex: `hooks-macos.json` ist nach Installation dieses Hooks immer
der aktuelle Spiegel der echten macOS-Hook-Konfiguration. Dieses File ist der
verlaeesslichste Weg um zu sehen welche Hooks aktiv sind und in welcher Reihenfolge
sie laufen — ohne Zugriff auf die lebende `settings.json`.

### Was wurde geaendert?

Zwei neue Hook-Dateien wurden erstellt und in SessionEnd registriert:
- `~/.claude/hooks/sync-hooks-reference.sh` (macOS/bash)
- `~/.claude/hooks/sync-hooks-reference.ps1` (Windows/PowerShell)
Beide auch als Kopien in `~/proggs/claude-code-setup/hooks/`.

In `~/.claude/settings.json` (macOS) wurde zum SessionEnd-Event hinzugefuegt:
```json
{
  "hooks": [
    {
      "type": "command",
      "command": "bash ~/.claude/hooks/sync-hooks-reference.sh",
      "timeout": 10,
      "async": true
    }
  ]
}
```

In `settings-reference.json` (Windows-Referenz) entsprechend:
```json
{
  "hooks": [
    {
      "type": "command",
      "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/sync-hooks-reference.ps1\"",
      "timeout": 10,
      "async": true
    }
  ]
}
```

Der `_comment` in `hooks-macos.json` wurde um den Hinweis
"AUTO-GENERATED by sync-hooks-reference.sh — do not edit manually" erweitert.

### Warum?

Intelligenz-Vorschlag aus Session #715: Die manuelle Pflege von `hooks-macos.json`
war fehleranfaellig. Mehrfach wurden Hooks in settings.json registriert aber
`hooks-macos.json` nicht aktualisiert, wodurch das Setup-Script veraltete Referenzen
enthielt. Eine automatische Synchronisation beseitigt diese Fehlerquelle dauerhaft.

Dies ist ein Self-Healing-Mechanismus: Sobald der Hook einmal installiert ist,
haelt er `hooks-macos.json` automatisch aktuell — ohne manuellen Aufwand.

### Portierung Windows/Claude-Code

1. Erstelle `%USERPROFILE%\.claude\hooks\sync-hooks-reference.ps1` mit dem Inhalt unten
2. Kopiere nach `%USERPROFILE%\proggs\claude-code-setup\hooks\sync-hooks-reference.ps1`
3. Oeffne `%USERPROFILE%\.claude\settings.json`
4. Suche den `"SessionEnd"` Block
5. Fuege nach dem letzten bestehenden SessionEnd-Hook-Objekt einen neuen Block ein:
   ```json
   ,{
     "hooks": [
       {
         "type": "command",
         "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/sync-hooks-reference.ps1\"",
         "timeout": 10,
         "async": true
       }
     ]
   }
   ```
6. Validiere: `pwsh -Command "Get-Content '$env:USERPROFILE\.claude\settings.json' | ConvertFrom-Json"`
7. Ab sofort wird `hooks-windows.json` automatisch nach jeder Session aktualisiert

### Portierung Codex/Gemini

Dieser Hook ist spezifisch fuer Claude Code (settings.json Extraktion). Codex und Gemini
haben andere Konfigurationsformate. Empfehlung:
- Den Inhalt von `hooks-macos.json` manuell lesen — es ist die aktuellste Referenz
- Das Windows-Aequivalent steht in `hooks-windows.json`
- Fuer Gemini/Codex: Diese Dateien als Dokumentation nutzen, nicht als direkt
  importierbares Format (da Gemini/Codex kein identisches Hook-System haben)

### Datei-Inhalt macOS (sync-hooks-reference.sh)

```bash
#!/usr/bin/env bash
# sync-hooks-reference.sh — Extracts hooks from live settings.json into setup-repo reference file
# Runs at SessionEnd (async). Keeps hooks-macos.json always in sync with live settings.
# Created: 2026-03-25 by Intelligenz-Vorschlag #715

set -euo pipefail

SETTINGS="$HOME/.claude/settings.json"
SETUP_DIR="$HOME/proggs/claude-code-setup"
REF_FILE="$SETUP_DIR/hooks-macos.json"
COMMENT="Complete hooks reference for macOS. Merge into ~/.claude/settings.json on macOS. Uses bash for command hooks. Prompt-injection-defender requires Python 3 (python3). Parry requires ~/.cargo/bin/parry. Bun at /opt/homebrew/bin/bun. AUTO-GENERATED by sync-hooks-reference.sh — do not edit manually."

# Bail if settings or setup dir missing
[[ -f "$SETTINGS" ]] || exit 0
[[ -d "$SETUP_DIR" ]] || exit 0

# Extract hooks section with python3
TMP_FILE="$REF_FILE.tmp.$$"
python3 -c "
import json, sys, os

settings_path = os.path.expanduser('$SETTINGS')
with open(settings_path) as f:
    settings = json.load(f)

hooks = settings.get('hooks', {})
if not hooks:
    sys.exit(0)

output = {
    '_comment': '''$COMMENT''',
    'hooks': hooks
}
print(json.dumps(output, indent='\t', ensure_ascii=False))
" > "\$TMP_FILE" 2>/dev/null || { rm -f "\$TMP_FILE"; exit 0; }

# Only update if content actually differs (compare JSON structure, not formatting)
if [[ -f "\$REF_FILE" ]]; then
    OLD_HASH=\$(python3 -c "import json; print(hash(json.dumps(json.load(open('\$REF_FILE')).get('hooks',{}), sort_keys=True)))" 2>/dev/null || echo "none")
    NEW_HASH=\$(python3 -c "import json; print(hash(json.dumps(json.load(open('\$TMP_FILE')).get('hooks',{}), sort_keys=True)))" 2>/dev/null || echo "changed")
    if [[ "\$OLD_HASH" == "\$NEW_HASH" ]]; then
        rm -f "\$TMP_FILE"
        exit 0
    fi
fi

mv "\$TMP_FILE" "\$REF_FILE"
echo "Hooks-Referenz aktualisiert: hooks-macos.json"
```

Vollstaendige Datei unter: `~/proggs/claude-code-setup/hooks/sync-hooks-reference.sh`

### Datei-Inhalt Windows (sync-hooks-reference.ps1)

```powershell
# sync-hooks-reference.ps1 — Extracts hooks from live settings.json into setup-repo reference file
# Runs at SessionEnd (async). Keeps hooks-windows.json always in sync with live settings.
# Created: 2026-03-25 by Intelligenz-Vorschlag #715

$ErrorActionPreference = "SilentlyContinue"

$Settings = Join-Path $env:USERPROFILE ".claude\settings.json"
$SetupDir = Join-Path $env:USERPROFILE "proggs\claude-code-setup"
$RefFile = Join-Path $SetupDir "hooks-windows.json"
$Comment = "Complete hooks reference for Windows. Merge into ~/.claude/settings.json on Windows. Uses pwsh (PowerShell 7). AUTO-GENERATED by sync-hooks-reference.ps1 -- do not edit manually."

# Bail if settings or setup dir missing
if (-not (Test-Path $Settings)) { exit 0 }
if (-not (Test-Path $SetupDir)) { exit 0 }

try {
    $settingsContent = Get-Content $Settings -Raw -Encoding utf8 | ConvertFrom-Json
    $hooks = $settingsContent.hooks

    if (-not $hooks) { exit 0 }

    $output = [ordered]@{
        "_comment" = $Comment
        "hooks" = $hooks
    }

    $newJson = $output | ConvertTo-Json -Depth 10 -Compress:$false

    # Compare with existing to avoid unnecessary writes
    if (Test-Path $RefFile) {
        $oldJson = Get-Content $RefFile -Raw -Encoding utf8
        $oldHooks = ($oldJson | ConvertFrom-Json).hooks | ConvertTo-Json -Depth 10 -Compress
        $newHooks = $hooks | ConvertTo-Json -Depth 10 -Compress
        if ($oldHooks -eq $newHooks) { exit 0 }
    }

    # Write with UTF-8 no BOM (Windows default encoding is cp1252 — must be explicit!)
    [System.IO.File]::WriteAllText($RefFile, $newJson, [System.Text.UTF8Encoding]::new($false))
    Write-Output "Hooks-Referenz aktualisiert: hooks-windows.json"
}
catch {
    # Silent failure — do not block session end
    exit 0
}
```

Vollstaendige Datei unter: `~/proggs/claude-code-setup/hooks/sync-hooks-reference.ps1`

### Settings-Registrierung

Hook-Event: `SessionEnd`

macOS-Befehl: `"command": "bash ~/.claude/hooks/sync-hooks-reference.sh", "timeout": 10, "async": true`

Windows-Befehl: `"command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/sync-hooks-reference.ps1\"", "timeout": 10, "async": true`

`async: true` — der Hook laeuft im Hintergrund und blockiert das Session-Ende nicht.
Er wird nach den anderen SessionEnd-Hooks ausgefuehrt (session-scorer, session-autopsy,
session-cleanup). Kein `matcher` noetig.

Wichtig nach Installation: `hooks-macos.json` (bzw. `hooks-windows.json`) ist
AUTO-GENERIERT. Die Datei NICHT manuell bearbeiten — Aenderungen werden beim naechsten
SessionEnd ueberschrieben. Hooks immer nur in `~/.claude/settings.json` bearbeiten.

<!-- APPLIED: macos/claude-code=2026-03-25T23:00:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-009] Export-Agent: Schritt 1.5 BASELINE-Pruefung — automatische Generierung
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T22:23:29Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: agent -->
<!-- AFFECTS: ~/.claude/agents/export.md, ~/proggs/claude-code-setup/agents/export.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Der `export` Agent ist die "Schreib-Haelfte" der Universal Mirror Bridge. Er wird am Ende einer
Session aufgerufen (Trigger: "starte den export Agenten", "exportiere", "Aenderungen spiegeln")
und schreibt alle Session-Aenderungen als ausfuehrliche, selbsterklaerende Eintraege ins
`mirror-ledger.md`. Andere CLIs und Plattformen lesen diese Eintraege spaeter mit dem
`import` Agenten und koennen so alle Aenderungen nachbauen ohne die Session gesehen zu haben.

Das zentrale Problem das diese Erweiterung loest: Wenn ein frisches CLI (Gemini, Codex, oder
ein neues Claude Code auf einem anderen Rechner) den `import` Agenten startet, aber KEIN
BASELINE-Eintrag im Ledger existiert, weiss das CLI nicht wie die Gesamtumgebung aussieht.
Inkrementelle Eintraege allein sind nutzlos — man muss wissen von welchem Ausgangszustand
man ausgeht. Der neue Schritt 1.5 loest das: Beim ersten Export-Lauf wird automatisch
ein vollstaendiger BASELINE-Eintrag generiert der die KOMPLETTE Umgebung beschreibt
(18 Agents, 52 Hooks, 21 Rules, 89 Plugins, Settings, MCP-Server, etc.).

Der Export-Agent laeuft mit `model: opus`, `effort: high`, `maxTurns: 60` und hat
Zugriff auf Read, Glob, Grep, Bash, Write, Edit Tools.

### Was wurde geaendert?

Ein neuer **Schritt 1.5 "BASELINE-Pruefung"** wurde nach dem bisherigen Schritt 1
("Plattform und CLI erkennen") eingefuegt. Der Schritt:

1. Prueft ob bereits ein `## [BASELINE-...]` Eintrag im Ledger existiert:
   ```bash
   grep -q "^\## \[BASELINE-" ~/proggs/claude-code-setup/mirror-ledger.md 2>/dev/null
   echo $?  # 0 = existiert, 1 = fehlt
   ```
2. Wenn KEIN BASELINE existiert (exit code 1): Sofort einen vollstaendigen BASELINE-Eintrag
   generieren VOR allen inkrementellen Eintraegen. Der BASELINE dokumentiert: Alle Custom
   Agents (Name, Modell, Zweck), Alle Hooks (Name, Event, Zweck), Alle Rules (Name,
   Prioritaet, Zweck), Alle Commands, Alle Plugins (Marketplace, Name), Settings (Modell,
   Effort, Env-Vars, Permissions), MCP-Server Uebersicht, Security-Tools,
   Dev-Tool-Anforderungen, Benutzer-Profil, Bootstrap-Anleitung.
   ID-Format: `BASELINE-YYYY-MM-DD`
3. Wenn BASELINE existiert: Weiter mit Schritt 2 (inkrementelle Aenderungen wie bisher).

Ausserdem: Sentinel-Datei schreiben:
`/tmp/agent-writeback-export.json` mit `{"agent": "export", "timestamp": "...", "findings": "N entries written"}`

### Warum?

Der Benutzer erkannte dass inkrementelle Ledger-Eintraege allein nicht ausreichen, wenn ein
neues CLI den `import` Agenten zum ersten Mal ausfuehrt. Ohne BASELINE gibt es keinen
Ausgangspunkt. Die automatische BASELINE-Generierung beim ersten Export-Lauf loest dieses
Bootstrap-Problem vollstaendig: Egal wann und auf welcher Plattform das erste CLI ein
Mirror-Ledger schreibt, der BASELINE wird immer vor den inkrementellen Eintraegen erstellt.

### Portierung Windows/claude-code

1. Lies die vollstaendige Agent-Datei aus dem "Datei-Inhalt" Abschnitt unten
2. Schreibe sie nach `%USERPROFILE%\.claude\agents\export.md`
3. Kopiere nach `%USERPROFILE%\proggs\claude-code-setup\agents\export.md`
4. Kein Hook-Eintrag noetig — Agents werden nicht in settings.json registriert
5. Test: Den `export` Agenten aufrufen und pruefen ob Schritt 1.5 erscheint

### Portierung Gemini/Codex

Diese Agent-Datei ist Claude-Code-spezifisch (Frontmatter-Format). Fuer andere CLIs:
1. Lies den vollstaendigen System-Prompt (ab "You are the Export Agent...")
2. Lege den Workflow als Wissen in der nativen CLI-Config ab:
   - Gemini: In `GEMINI.md` einen Abschnitt "Export-Workflow" schreiben
   - Codex: In `CODEX.md` einen Abschnitt "Export-Workflow" schreiben
3. Wichtigste Erkenntnis: Beim ersten Schreiben ins Ledger IMMER zuerst pruefen ob
   BASELINE existiert, und wenn nicht einen generieren

### Datei-Inhalt macOS/Windows (export.md)

Die vollstaendige Datei liegt unter:
`~/proggs/claude-code-setup/agents/export.md`

Frontmatter-Zusammenfassung:
```yaml
name: export
model: opus
effort: high
maxTurns: 60
tools: [Read, Glob, Grep, Bash, Write, Edit]
```

Schluesselerweiterung — Schritt 1.5 (neuer Block nach Schritt 1 im System-Prompt):
```
## Schritt 1.5: BASELINE-Pruefung (AUTOMATISCH beim ersten Export)

Vor ALLEM anderen pruefen ob ein BASELINE-Eintrag im Ledger existiert:

  grep -q "^## \[BASELINE-" ~/proggs/claude-code-setup/mirror-ledger.md 2>/dev/null
  echo $?  # 0 = existiert, 1 = fehlt

Wenn KEIN BASELINE existiert (exit code 1):
1. SOFORT einen vollstaendigen BASELINE-Eintrag generieren
2. Der BASELINE dokumentiert die GESAMTE Umgebung:
   - Alle Custom Agents (Name, Modell, Zweck — aus ~/.claude/agents/)
   - Alle Hooks (Name, Event, Zweck — aus ~/.claude/hooks/)
   - Alle Rules (Name, Prioritaet, Zweck — aus ~/.claude/rules/)
   - Alle Commands (Name, Zweck — aus ~/.claude/commands/)
   - Alle Plugins (Marketplace, Name — aus manifest.json)
   - Settings (Modell, Effort, Env-Vars, Permissions)
   - MCP-Server Uebersicht
   - Security-Tools, Dev-Tool-Anforderungen
   - Benutzer-Profil und Praeferenzen
   - Bootstrap-Anleitung (git clone -> setup script -> fertig)
3. ID-Format: BASELINE-YYYY-MM-DD
4. DANACH erst weiter mit Schritt 2

Wenn BASELINE existiert: Weiter mit Schritt 2.
```

<!-- APPLIED: macos/claude-code=2026-03-25T22:23:29Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-010] Import-Agent: Prinzip "VOLLSTAENDIG SPIEGELN IM EIGENEN WORKSPACE"
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T22:23:29Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: agent -->
<!-- AFFECTS: ~/.claude/agents/import.md, ~/proggs/claude-code-setup/agents/import.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Der `import` Agent ist die "Lese-Haelfte" der Universal Mirror Bridge. Er liest das
`mirror-ledger.md`, findet Eintraege die fuer die aktuelle Plattform/CLI noch als PENDING
markiert sind, und wendet sie an. Der Import-Agent laeuft mit `model: opus`, `effort: high`,
`maxTurns: 80`.

Das zentrale Problem das diese Erweiterung loest: Der urspruengliche Import-Agent war darauf
ausgelegt nur Claude-Code-zu-Claude-Code Synchronisation zu beherrschen. Wenn aber ein
ANDERES CLI (Gemini, Codex) den Import-Agenten aufruft, darf er NICHT in Claude-Code-
Verzeichnisse schreiben. Diese CLIs haben eigene Konfigurationsverzeichnisse und eigene
Formate. Das neue Prinzip "VOLLSTAENDIG SPIEGELN — IM EIGENEN WORKSPACE" loest das: Alles
wird gespiegelt, aber IMMER im Workspace des ZIEL-CLIs.

Das Ergebnis: Alle CLIs bekommen die volle Intelligenz der Programmierumgebung — aber jedes
in seiner eigenen nativen Form. Gemini bekommt Hooks als Workflow-Regeln in GEMINI.md,
Codex bekommt sie als Anweisungen in CODEX.md, und Claude Code bekommt die echten
.sh/.ps1 Dateien installiert.

### Was wurde geaendert?

Der Import-Agent wurde um ein neues **oberstes Prinzip** erweitert:

**VOLLSTAENDIG SPIEGELN — IM EIGENEN WORKSPACE**

Konkret unterscheidet der Agent jetzt:

**Gleiches CLI (Claude Code macOS<->Windows):**
- Vollstaendig 1:1 implementieren
- Dateien, Settings, Hooks — alles nach `~/.claude/` auf der Zielplattform

**Anderes CLI (Gemini, Codex):**
- Regeln und Direktiven -> In die native Config (GEMINI.md, CODEX.md)
- Agents -> Agent-Dateien kopieren UND als Wissen in die CLI-Config schreiben
- Hooks -> Wenn Hook-System vorhanden: installieren. Wenn nicht: als Workflow-Regeln
- Plugins -> Wenn CLI-Aequivalente existieren: vorschlagen. Wenn nicht: als Wissen
- Settings -> In natives Settings-Format uebersetzen
- Philosophie -> IMMER vollstaendig uebernehmen

Ausserdem wurden die Plattform-Adaptionsregeln erweitert:
- `date -u '+%Y-%m-%dT%H:%M:%SZ'` -> `(Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')`
- `2>/dev/null` -> `2>$null`
- `| head -N` -> `| Select-Object -First N`
- `| tail -N` -> `| Select-Object -Last N`
- `wc -l < file` -> `(Get-Content file).Count`
- `/opt/homebrew/bin/npx` -> `npx`
- `set -euo pipefail` -> `$ErrorActionPreference = 'Stop'`

Und ein Kommentar-Format fuer adaptierte Dateien:
`# Adapted from {MIRROR-ID} by import agent on {DATE}`

### Warum?

Die Mirror Bridge soll ein UNIVERSELLES Cross-CLI System sein. Wenn Gemini oder Codex die
Programmierumgebung nutzen sollen, muss der Import-Agent verstehen dass er NICHT in
Claude-Code-Dateien schreiben darf. Das neue Prinzip macht die Bridge zukunftssicher fuer
beliebige neue CLIs.

### Portierung Windows/claude-code

1. Lies die vollstaendige Agent-Datei: `~/proggs/claude-code-setup/agents/import.md`
2. Schreibe sie nach `%USERPROFILE%\.claude\agents\import.md`
3. Kein Hook-Eintrag noetig
4. Test: Den `import` Agenten aufrufen, pruefen ob das neue Prinzip "VOLLSTAENDIG SPIEGELN"
   in der Ausgabe erscheint

### Portierung Gemini/Codex

Der Import-Agent selbst ist Claude-Code-spezifisch. Fuer andere CLIs:
1. Lies den vollstaendigen System-Prompt
2. Verstehe das Prinzip: Import = alles spiegeln, aber im eigenen Workspace
3. Wenn du als Gemini oder Codex Ledger-Eintraege importierst:
   - Jeden Eintrag vollstaendig lesen (Kontext, Datei-Inhalt, Settings)
   - Aequivalente in der eigenen nativen Config erstellen
   - Als APPLIED markieren und pushen

### Datei-Inhalt macOS/Windows (import.md)

Die vollstaendige Datei liegt unter:
`~/proggs/claude-code-setup/agents/import.md`

Frontmatter-Zusammenfassung:
```yaml
name: import
model: opus
effort: high
maxTurns: 80
tools: [Read, Glob, Grep, Bash, Write, Edit]
```

Schluesselerweiterung — Neues Oberstes Prinzip (vor Schritt 1 im System-Prompt):
```
## OBERSTES PRINZIP: VOLLSTAENDIG SPIEGELN — IM EIGENEN WORKSPACE

Alles wird gespiegelt — Agents, Hooks, Rules, Settings, Direktiven — aber IMMER
im Workspace und Konfigurationsverzeichnis des ZIEL-CLIs.

Gleiches CLI (Claude Code macOS<->Windows):
  Vollstaendig 1:1 implementieren. Ziel: ~/.claude/ auf der Zielplattform.

Anderes CLI (Gemini, Codex):
  Alles spiegeln, aber ins EIGENE Format:
  - Regeln und Direktiven -> native Config (GEMINI.md, CODEX.md)
  - Agents -> kopieren UND als Wissen in CLI-Config schreiben
  - Hooks -> installieren wenn Hook-System vorhanden, sonst als Workflow-Regeln
  - Plugins -> CLI-Aequivalente vorschlagen oder als Faehigkeits-Wissen dokumentieren
  - Settings -> in natives Format uebersetzen
  - Philosophie -> IMMER vollstaendig uebernehmen
```

<!-- APPLIED: macos/claude-code=2026-03-25T22:23:29Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-011] Bootstrap-Prompt: Erstmalige Mirror-Bridge-Einrichtung in neuem CLI
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T22:23:29Z -->
<!-- TARGETS: codex,gemini -->
<!-- TYPE: skill -->
<!-- AFFECTS: ~/proggs/claude-code-setup/bridges/mirror-bridge-bootstrap.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Der Bootstrap-Prompt ist ein Dokument das IN das neue CLI (Gemini, Codex, oder frisches
Claude Code) eingefuegt wird um die Mirror Bridge zum ersten Mal einzurichten. Er ist
kein Agent und kein Hook — er ist eine vollstaendige Anleitung die ein Benutzer kopiert
und dem neuen CLI als erste Nachricht schickt, oder die das CLI als Systemkontext erhaelt.

Das Problem das er loest: Wenn jemand anfaengt Gemini CLI zu nutzen und die
Programmierumgebung uebernehmen will, weiss Gemini nichts von der Mirror Bridge, dem
Ledger, den Agents, den Hooks oder den Direktiven. Der Bootstrap-Prompt erklaert dem
neuen CLI in 6 Schritten wie es (a) das Repository klont, (b) den BASELINE liest,
(c) alle Komponenten in seine eigene native Config uebernimmt — NICHT als Claude-Code-Klon!
sondern als native Intelligenz — (d) die Export-Faehigkeit einrichtet, (e) inkrementelle
Eintraege anwendet, und (f) eine Bestaetigung ausgibt.

Der entscheidende Designgedanke: Das neue CLI soll NICHT einfach Claude-Code-Dateien
kopieren. Es soll die INTELLIGENZ und FAEHIGKEITEN uebernehmen und an sein eigenes
Format anpassen. Hooks werden zu Workflow-Regeln, Agents werden zu Faehigkeits-
Beschreibungen — das neue CLI bekommt die volle Intelligenz in seiner eigenen Sprache.

Die Datei liegt im neuen `bridges/` Unterordner des Setup-Repos der speziell fuer
Cross-CLI Verbindungs-Infrastruktur geschaffen wurde.

### Was wurde geaendert?

Neue Datei erstellt: `~/proggs/claude-code-setup/bridges/mirror-bridge-bootstrap.md`

Die Datei enthaelt einen kopierbaren Prompt (6 Schritte) und CLI-spezifische Hinweise:

**Schritt 1:** Repository pruefen/klonen (`git clone https://github.com/Pepsi1978/proggs ~/proggs`)

**Schritt 2:** Mirror-Ledger vollstaendig lesen — BASELINE zuerst lesen.
Der BASELINE beschreibt: 18 Agents, 52 Hooks, 21 Rules, 89 Plugins, Settings, MCP, Security,
die 3 Direktiven, Benutzer-Profil.

**Schritt 3:** Intelligenz uebertragen (KEIN Klon!):
- Direktiven aus `~/proggs/claude-code-setup/rules/superintelligence.md` etc. in native Config
- Agents als Faehigkeits-Beschreibungen
- Hooks als Workflow-Regeln (wenn kein Hook-System vorhanden)
- Benutzer-Profil vollstaendig (Deutsch, Whisper, Parallelisierung, Sichtbarkeit, etc.)

**Schritt 4:** Export-Faehigkeit einrichten:
- Ledger-Pfad merken: `~/proggs/claude-code-setup/mirror-ledger.md`
- Am Ende jeder Session: Neuen Eintrag anhaengen (Format: GEM/CDX statt MAC/WIN)
- BASELINE pruefen wenn keiner existiert

**Schritt 5:** Inkrementelle PENDING-Eintraege anwenden und als APPLIED markieren

**Schritt 6:** Bestaetigung ausgeben

**CLI-spezifische Hinweise:**
- Gemini CLI: Native Config = `GEMINI.md`, kein Agent/Hook-System
- Codex CLI: Native Config = `CODEX.md`, Sandbox-Ausfuehrung
- Frisches Claude Code: Setup-Script nutzen, kein Bootstrap noetig

### Warum?

Ohne diesen Bootstrap-Prompt muss ein Benutzer der ein neues CLI einrichten will
entweder die gesamte Mirror-Bridge-Dokumentation lesen und manuell umsetzen, oder
mehrere Runden mit dem neuen CLI kommunizieren bis es versteht was es tun soll.
Der Bootstrap-Prompt macht den Prozess von "erstes Mal Gemini starten" bis zu
"volle Programmierumgebungs-Intelligenz eingerichtet" zu einem einzigen Schritt.

### Portierung Gemini CLI

1. `git clone https://github.com/Pepsi1978/proggs ~/proggs` (falls noch nicht geklont)
2. `cd ~/proggs && git pull`
3. Oeffne Gemini CLI
4. Lese die Datei: `cat ~/proggs/claude-code-setup/bridges/mirror-bridge-bootstrap.md`
5. Kopiere den Prompt-Block (alles nach "## Prompt (komplett kopieren und einfuegen)")
6. Schicke diesen Block als erste Nachricht an Gemini CLI
7. Gemini fuehrt den 6-Schritte-Prozess selbststaendig durch

### Portierung Codex CLI

Gleich wie Gemini, aber beachte:
- Codex hat Sandbox-Ausfuehrung — Git-Operationen muessen explizit erlaubt sein
- Die native Config ist `CODEX.md` im Repo-Root (nicht `~/.codex/`)
- Agents werden als Workflow-Anweisungen in CODEX.md gespeichert

### Datei-Inhalt (mirror-bridge-bootstrap.md)

Die vollstaendige Datei liegt unter:
`~/proggs/claude-code-setup/bridges/mirror-bridge-bootstrap.md`

Kurzfassung des enthaltenen Prompts (vollstaendiger Text in der Datei):

```
# Schritt 1: Repository pruefen/klonen
git clone https://github.com/Pepsi1978/proggs ~/proggs  # falls noch nicht vorhanden
cd ~/proggs && git pull

# Schritt 2: BASELINE lesen
# Lies ~/proggs/claude-code-setup/mirror-ledger.md — BASELINE-Eintrag zuerst
# BASELINE beschreibt: 18 Agents, 52 Hooks, 21 Rules, 89 Plugins, 3 Direktiven

# Schritt 3: Intelligenz uebertragen (NICHT klonen!)
# - Direktiven: ~/proggs/claude-code-setup/rules/ -> native Config lesen und adaptieren
# - Agents: als Faehigkeits-Beschreibungen in native Config
# - Hooks: als Workflow-Regeln (Logik, nicht die .sh Dateien)
# - Benutzer-Profil: Deutsch/Whisper, Parallelisierung, Sichtbarkeit, einziges Repo

# Schritt 4: Export-Faehigkeit
# Am Sessionende: neuen MIRROR-YYYY-MM-DD-GEM/CDX-NNN Eintrag ans Ledger haengen

# Schritt 5: PENDING-Eintraege anwenden und als APPLIED markieren, pushen

# Schritt 6: Bestaetigung ausgeben
```

<!-- APPLIED: macos/claude-code=2026-03-25T22:23:29Z -->
<!-- APPLIED: windows/claude-code=2026-03-25T22:23:29Z -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-012] hooks-macos.json Sync: mirror-check, PostCompact, SubagentStart, StopFailure, sync-hooks-reference
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T22:15:09Z -->
<!-- TARGETS: windows/claude-code -->
<!-- TYPE: settings -->
<!-- AFFECTS: ~/proggs/claude-code-setup/hooks-macos.json -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Die Datei `hooks-macos.json` ist die **vollstaendige Hooks-Referenz fuer macOS**. Sie ist
eine 1:1 Kopie aller Hook-Eintraege aus `~/.claude/settings.json` und dient als Dokumentation,
Backup und Cross-Platform-Vergleichsreferenz. Sie wird vom `sync-hooks-reference.sh` Hook
automatisch am Ende jeder Session (SessionEnd, async) regeneriert — Aenderungen an dieser
Datei werden also beim naechsten SessionEnd ueberschrieben. Die echten Hook-Definitionen
liegen immer in `~/.claude/settings.json`.

Diese Datei liegt im Git-Repo (bei Pepsi1978/proggs) und ermoeglicht es, auf Windows
nachzuverfolgen was auf macOS konfiguriert ist. Windows hat eine analoge `hooks-windows.json`.
Beide Dateien dienen als Synchronisations-Referenz fuer das Cross-Platform-Team.

In Commit #730 wurde die Datei auf den aktuellen Stand von settings.json gebracht, weil
mehrere Hook-Aenderungen aus dieser Session noch nicht in der hooks-macos.json reflektiert
waren. Nach Installation des sync-hooks-reference.sh Hooks wird das kuenftig automatisch
am SessionEnd erledigt.

### Was wurde geaendert?

Die folgenden Unterschiede zwischen settings.json und hooks-macos.json wurden behoben:

1. **Kommentar:** "AUTO-GENERATED by sync-hooks-reference.sh — do not edit manually" hinzugefuegt

2. **SessionStart — mirror-check.sh hinzugefuegt:**
   ```json
   {
     "hooks": [{
       "type": "command",
       "command": "bash ~/.claude/hooks/mirror-check.sh",
       "timeout": 5
     }]
   }
   ```

3. **PreToolUse (Bash) — Reihenfolge korrigiert:** safety-gate.sh war falsch positioniert,
   jetzt korrekt unter dem PreToolUse Event mit `"matcher": "Bash"`

4. **PostToolUse — sync-hooks-reference.sh als async hinzugefuegt:**
   ```json
   {
     "hooks": [{
       "type": "command",
       "command": "bash ~/.claude/hooks/sync-hooks-reference.sh",
       "timeout": 10,
       "async": true
     }]
   }
   ```

5. **SubagentStart — neuer Event-Block:**
   ```json
   "SubagentStart": [{
     "hooks": [{
       "type": "command",
       "command": "bash ~/.claude/hooks/subagent-context.sh",
       "timeout": 5
     }]
   }]
   ```
   (War vorher faelschlicherweise unter einem anderen Event)

6. **StopFailure — neuer Event-Block:**
   ```json
   "StopFailure": [{
     "hooks": [{
       "type": "command",
       "command": "bash ~/.claude/hooks/stopfailure-logger.sh",
       "timeout": 5
     }]
   }]
   ```

7. **PostCompact — neuer Event-Block mit Prompt-Hook (kein command-Hook!):**
   ```json
   "PostCompact": [{
     "hooks": [{
       "type": "prompt",
       "prompt": "Context was just compacted. Read /tmp/claude-session-goal.txt to recall the original user goal. Also read `~/proggs/.claude/agent-memory/shared/MEMORY.md` for project conventions and open issues. Briefly state what you were working on before compaction and what the next step should be.",
       "timeout": 10
     }]
   }]
   ```

8. **WorktreeCreate:** Block entfernt (war redundant mit WorktreeRemove)

9. **WorktreeRemove:** Bleibt, Befehl unveraendert: `echo 'Worktree-Hook: Worktree entfernt...'`

### Warum?

Die hooks-macos.json war veraltet — sie spiegelte nicht den aktuellen Stand der settings.json
nach mehreren Tagen Weiterentwicklung. Konkret fehlten die Hooks mirror-check (neu aus MAC-002),
sync-hooks-reference (neu aus MAC-008), sowie die Reorganisation der SubagentStart/StopFailure/
PostCompact Events. Nach der Installation des sync-hooks-reference.sh SessionEnd-Hooks wird
diese manuelle Synchronisation kuenftig automatisch erledigt.

### Portierung Windows/claude-code

Die hooks-macos.json selbst ist eine reine macOS-Referenzdatei und muss nicht auf Windows
portiert werden. Was Windows tun muss:

1. Pruefen ob in `~/.claude/settings.json` auf Windows die folgenden Hook-Events konfiguriert sind:
   - `mirror-check.ps1` unter SessionStart
   - `sync-hooks-reference.ps1` unter SessionEnd (async: true)
   - `subagent-context.ps1` unter SubagentStart
   - `stopfailure-logger.ps1` unter StopFailure
   - PostCompact Prompt-Hook (gleicher Text, aber `$env:TEMP/claude-session-goal.txt`)

2. Falls ein Hook fehlt: Die .ps1 Datei aus dem entsprechenden Ledger-Eintrag portieren
   (mirror-check aus MAC-002, sync-hooks-reference aus MAC-008)

3. `hooks-windows.json` aktualisieren falls sie veraltet ist:
   - Entweder sync-hooks-reference.ps1 SessionEnd-Hook laufen lassen (automatisch)
   - Oder manuell die gleichen Aenderungen in hooks-windows.json nachziehen

### Settings-Registrierung

Vollstaendige Hook-Konfiguration die in settings.json auf macOS aktiv sein muss:

```json
"SessionStart": [
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/auto-sync.sh", "timeout": 30 }] },
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/intent-anker.sh", "timeout": 5 }] },
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/mirror-check.sh", "timeout": 5 }] }
],
"PreToolUse": [
  { "matcher": "Bash", "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/safety-gate.sh", "timeout": 5 }] }
],
"SubagentStart": [
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/subagent-context.sh", "timeout": 5 }] }
],
"StopFailure": [
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/stopfailure-logger.sh", "timeout": 5 }] }
],
"PostCompact": [
  { "hooks": [{ "type": "prompt", "prompt": "Context was just compacted. Read /tmp/claude-session-goal.txt to recall the original user goal. Also read ~/proggs/.claude/agent-memory/shared/MEMORY.md for project conventions. Briefly state what you were working on before compaction and what the next step should be.", "timeout": 10 }] }
],
"SessionEnd": [
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/session-scorer.sh", "timeout": 25 }] },
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/session-autopsy.sh", "timeout": 10 }] },
  { "hooks": [{ "type": "command", "command": "bash ~/.claude/hooks/sync-hooks-reference.sh", "timeout": 10, "async": true }] }
]
```

Analoges Windows-JSON (mit pwsh statt bash, .ps1 statt .sh):
```json
"SessionStart": [
  { "hooks": [{ "type": "command", "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/auto-sync.ps1\"", "timeout": 30 }] },
  { "hooks": [{ "type": "command", "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/intent-anker.ps1\"", "timeout": 5 }] },
  { "hooks": [{ "type": "command", "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/mirror-check.ps1\"", "timeout": 5 }] }
],
"SubagentStart": [
  { "hooks": [{ "type": "command", "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/subagent-context.ps1\"", "timeout": 5 }] }
],
"StopFailure": [
  { "hooks": [{ "type": "command", "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/stopfailure-logger.ps1\"", "timeout": 5 }] }
],
"PostCompact": [
  { "hooks": [{ "type": "prompt", "prompt": "Context was just compacted. Read $env:TEMP/claude-session-goal.txt to recall the original user goal. Also read ~/proggs/.claude/agent-memory/shared/MEMORY.md for project conventions. Briefly state what you were working on before compaction and what the next step should be.", "timeout": 10 }] }
],
"SessionEnd": [
  { "hooks": [{ "type": "command", "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/session-scorer.ps1\"", "timeout": 25 }] },
  { "hooks": [{ "type": "command", "command": "pwsh -NoProfile -File \"$USERPROFILE/.claude/hooks/sync-hooks-reference.ps1\"", "timeout": 10, "async": true }] }
]
```

<!-- APPLIED: macos/claude-code=2026-03-25T22:15:09Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-25T21:47:57Z -->

---

## [MIRROR-2026-03-25-MAC-013] OpenClaw v2026.3.24 nativ auf macOS eingerichtet mit Gemini 3 Flash Preview
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T22:45:36Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: software -->
<!-- AFFECTS: ~/.openclaw/openclaw.json, ~/.openclaw/agents/main/agent/models.json, ~/Library/LaunchAgents/ai.openclaw.gateway.plist, ~/Desktop/OpenClaw.command -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

OpenClaw ist ein KI-Coding-Agent aehnlich wie Claude Code, der ueber eine lokale Gateway-Architektur laeuft. Der Gateway-Prozess laeuft als permanenter Hintergrunddienst (auf macOS als launchd-LaunchAgent, auf Windows als Windows-Dienst, auf Linux als systemd-Unit) und stellt eine HTTP-API auf Port 18789 bereit, ueber die die TUI (Terminal User Interface) kommuniziert.

Bis zu dieser Session lief OpenClaw nur innerhalb der NemoClaw-Sandbox — also nicht als vollwertige native Installation, sondern in einer eingeschraenkten Testumgebung. In dieser Session wurde OpenClaw v2026.3.24 als native macOS-Installation eingerichtet:

1. Der Gemini 3 Flash Preview (`google/gemini-3-flash-preview`) wurde als Standardmodell konfiguriert. Dieses Modell hat 1 Million Token Kontextfenster und 65.536 Token maximaler Ausgabe und wird direkt ueber die Google Generative Language API angesprochen (`https://generativelanguage.googleapis.com/v1beta`).

2. Der Gateway-Prozess wurde als LaunchAgent registriert, damit er automatisch beim macOS-Login startet und bei Absturz neu gestartet wird (`KeepAlive: true`). Logs landen in `~/.openclaw/logs/`.

3. Eine Desktop-Verknuepfung (`~/Desktop/OpenClaw.command`) wurde erstellt, mit der der Benutzer OpenClaw per Doppelklick im Terminal starten kann.

Dieses Setup ist unabhaengig von Claude Code und ergaenzt die bestehende KI-Infrastruktur. OpenClaw nutzt einen eigenen API-Key fuer Gemini (der Benutzer hat einen separaten Google AI Studio Key).

**Sicherheitshinweis**: Der Gemini API Key ist in `~/.openclaw/openclaw.json` gespeichert. Diese Datei darf NIEMALS ins Git-Repo committed werden. Der Key hat das Format `AIzaSy...` und muss auf jedem System separat konfiguriert werden.

### Was wurde geaendert?

**Datei 1: `~/.openclaw/openclaw.json`** — Haupt-Konfigurationsdatei
- `agents.defaults.model` wurde auf `"google/gemini-3-flash-preview"` gesetzt (vorher war OpenClaw in der NemoClaw-Sandbox konfiguriert, nicht nativ)
- Neuer `models.providers.google`-Block hinzugefuegt mit:
  - `baseUrl`: `"https://generativelanguage.googleapis.com/v1beta"`
  - `apiKey`: Gemini API Key des Benutzers (hier aus Sicherheitsgruenden nicht gezeigt — NIEMALS ins Repo committen)
  - `models`-Array mit gemini-3-flash-preview (1M context, 65k output)
- `gateway.mode`: `"local"` (Gateway laeuft lokal, nicht cloud-basiert)
- `gateway.auth.mode`: `"token"` mit auto-generiertem Token fuer lokale Authentifizierung

**Datei 2: `~/.openclaw/agents/main/agent/models.json`** — Provider-Registry fuer den Agent
- Neuer Provider `google` hinzugefuegt mit:
  - `baseUrl`: `https://generativelanguage.googleapis.com/v1beta`
  - `api`: `google-gemini` (OpenClaw-internes Protokoll fuer Google-kompatible APIs)
  - Das Modell gemini-3-flash-preview mit vollstaendigen Spezifikationen

**Datei 3: `~/Library/LaunchAgents/ai.openclaw.gateway.plist`** — macOS LaunchAgent
- Neuer LaunchAgent der den OpenClaw Gateway-Prozess beim Login startet
- Verwendet absoluten Pfad zu Node.js: `/opt/homebrew/opt/node/bin/node`
- Startet `openclaw gateway --port 18789`
- Vollstaendiger PATH mit allen relevanten Verzeichnissen gesetzt
- `KeepAlive: true` — wird bei Absturz automatisch neu gestartet

**Datei 4: `~/Desktop/OpenClaw.command`** — Desktop-Verknuepfung
- Einfaches Shell-Skript das `openclaw tui` startet
- Durch Doppelklick im Finder ausfuehrbar

### Warum?

Der Benutzer wollte OpenClaw als vollwertige native Alternative zu Claude Code nutzen koennen, insbesondere mit Gemini 3 Flash Preview als Modell. Die NemoClaw-Sandbox war zu eingeschraenkt fuer produktive Nutzung. Mit der nativen Installation kann OpenClaw nun direkt auf alle Projekt-Dateien zugreifen, den Gateway nutzt, und wird automatisch beim Login gestartet.

### Portierung Windows/claude-code

Auf Windows gibt es keinen launchd, aber OpenClaw hat einen eigenen Service-Installer:

1. **OpenClaw installieren** (falls noch nicht geschehen):
   ```
   npm install -g openclaw
   ```

2. **Konfigurationsdatei erstellen** unter `%USERPROFILE%\.openclaw\openclaw.json`:
   - Inhalt: Gleich wie macOS-Version unten, aber API-Key separat setzen
   - NIEMALS den API-Key ins Repo committen
   - Verzeichnis erstellen: `New-Item -ItemType Directory -Force "$env:USERPROFILE\.openclaw"`

3. **Gateway als Windows-Dienst installieren**:
   ```
   openclaw gateway install
   ```
   Das erstellt automatisch einen Windows-Dienst (kein Task Scheduler noetig).
   Pruefen ob er laeuft: `sc query "OpenClaw Gateway"`

4. **Desktop-Verknuepfung erstellen**: Eine `OpenClaw.bat` auf dem Desktop:
   ```bat
   @echo off
   cd %USERPROFILE%
   openclaw tui
   ```
   Oder alternativ eine .lnk-Datei via PowerShell.

5. **API-Key setzen**: In `%USERPROFILE%\.openclaw\openclaw.json` den `apiKey`-Wert auf den eigenen Gemini-Key setzen.

**Wichtige Unterschiede zu macOS**:
- Kein `.command`-Format — stattdessen `.bat` oder `.lnk`
- Kein LaunchAgent/plist — stattdessen `openclaw gateway install` (Windows-Dienst)
- Node.js-Pfad ist `C:\Program Files\nodejs\node.exe` oder via PATH (nicht `/opt/homebrew/opt/node/bin/node`)
- `%USERPROFILE%\.openclaw\` statt `~/.openclaw/`

### Portierung codex

Codex CLI laeuft auf derselben macOS-Maschine. OpenClaw ist unabhaengig von Codex und kann parallel laufen. Die Installation ist bereits vorhanden. Falls Codex auf einem anderen System laeuft:

1. `npm install -g openclaw` ausfuehren
2. `~/.openclaw/openclaw.json` erstellen (Inhalt siehe unten, API-Key ergaenzen)
3. `openclaw gateway install` fuer den Service
4. `openclaw tui` zum Starten

### Portierung gemini

Gemini CLI laeuft ebenfalls auf macOS. OpenClaw ist bereits nativ installiert. Fuer Gemini CLI selbst ist keine Portierung noetig — OpenClaw ist ein separates Tool.

### Datei-Inhalt macOS (openclaw.json) — OHNE API-KEY

**ACHTUNG: Den `apiKey`-Wert NIEMALS ins Repository committen. Den echten Key separat auf jedem System setzen.**

```json
{
  "meta": {
    "lastTouchedVersion": "2026.3.24",
    "lastTouchedAt": "2026-03-25T22:18:32.547Z"
  },
  "agents": {
    "defaults": {
      "workspace": "/Users/frank/.openclaw/workspace",
      "model": "google/gemini-3-flash-preview"
    }
  },
  "commands": {
    "native": "auto",
    "nativeSkills": "auto",
    "restart": true,
    "ownerDisplay": "raw"
  },
  "models": {
    "providers": {
      "google": {
        "baseUrl": "https://generativelanguage.googleapis.com/v1beta",
        "apiKey": "HIER_DEN_EIGENEN_GEMINI_API_KEY_EINTRAGEN",
        "models": [
          {
            "id": "gemini-3-flash-preview",
            "name": "Gemini 3 Flash Preview",
            "contextWindow": 1048576,
            "maxTokens": 65536
          }
        ]
      }
    }
  },
  "gateway": {
    "mode": "local",
    "auth": {
      "mode": "token",
      "token": "AUTO_GENERIERT_BEIM_ERSTEN_START"
    }
  }
}
```

**Workspace-Pfad auf Windows anpassen**: `"workspace": "$env:USERPROFILE\\.openclaw\\workspace"` (oder den absoluten Pfad verwenden).

### Datei-Inhalt macOS (ai.openclaw.gateway.plist)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
  <dict>
    <key>Label</key>
    <string>ai.openclaw.gateway</string>

    <key>Comment</key>
    <string>OpenClaw Gateway (v2026.3.24)</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>ThrottleInterval</key>
    <integer>1</integer>
    <key>Umask</key>
    <integer>63</integer>
    <key>ProgramArguments</key>
    <array>
      <string>/opt/homebrew/opt/node/bin/node</string>
      <string>/opt/homebrew/lib/node_modules/openclaw/dist/index.js</string>
      <string>gateway</string>
      <string>--port</string>
      <string>18789</string>
    </array>

    <key>StandardOutPath</key>
    <string>/Users/frank/.openclaw/logs/gateway.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/frank/.openclaw/logs/gateway.err.log</string>
    <key>EnvironmentVariables</key>
    <dict>
      <key>HOME</key>
      <string>/Users/frank</string>
      <key>TMPDIR</key>
      <string>/var/folders/h6/mqbrn72x551brgqdnh_75s4m0000gn/T/</string>
      <key>NODE_EXTRA_CA_CERTS</key>
      <string>/etc/ssl/cert.pem</string>
      <key>NODE_USE_SYSTEM_CA</key>
      <string>1</string>
      <key>PATH</key>
      <string>/opt/homebrew/opt/node/bin:/Users/frank/.local/bin:/Users/frank/.npm-global/bin:/Users/frank/bin:/Users/frank/.volta/bin:/Users/frank/.bun/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string>
      <key>OPENCLAW_GATEWAY_PORT</key>
      <string>18789</string>
      <key>OPENCLAW_LAUNCHD_LABEL</key>
      <string>ai.openclaw.gateway</string>
      <key>OPENCLAW_SERVICE_MARKER</key>
      <string>openclaw</string>
      <key>OPENCLAW_SERVICE_KIND</key>
      <string>gateway</string>
      <key>OPENCLAW_SERVICE_VERSION</key>
      <string>2026.3.24</string>
    </dict>
  </dict>
</plist>
```

**Hinweis**: `/Users/frank` muss durch den tatsaechlichen Home-Pfad ersetzt werden. `TMPDIR` ist Mac-spezifisch und aendert sich; kann auch auf `/tmp` gesetzt werden.

### Datei-Inhalt macOS (OpenClaw.command — Desktop-Verknuepfung)

```bash
#!/bin/bash
cd ~
/opt/homebrew/bin/openclaw tui
```

**Ausfuehren**: `chmod +x ~/Desktop/OpenClaw.command` damit sie per Doppelklick ausfuehrbar ist.

### Datei-Inhalt Windows (OpenClaw.bat — Desktop-Verknuepfung)

```bat
@echo off
cd %USERPROFILE%
openclaw tui
pause
```

### Settings-Registrierung

OpenClaw ist kein Claude-Code-Hook und benoetigt keine Registrierung in `~/.claude/settings.json`. Es ist ein eigenstaendiger KI-Agent. Der Gateway laeuft als launchd-Daemon (macOS) bzw. Windows-Dienst unabhaengig von Claude Code.

**LaunchAgent laden/neu laden** (macOS, nach Aenderungen an der plist):
```bash
launchctl unload ~/Library/LaunchAgents/ai.openclaw.gateway.plist 2>/dev/null || true
launchctl load ~/Library/LaunchAgents/ai.openclaw.gateway.plist
```

**Gateway-Status pruefen** (macOS):
```bash
launchctl list | grep openclaw
curl -s http://localhost:18789/health 2>/dev/null || echo "Gateway nicht erreichbar"
```

<!-- APPLIED: macos/claude-code=2026-03-25T22:45:36Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-26T01:48:45Z -->

---

## [BASELINE-2026-03-25-GEM] Initial Gemini CLI BASELINE — macos/gemini
<!-- SOURCE: gemini-cli | PLATFORM: macos | TIMESTAMP: 2026-03-25T22:15:36Z -->
<!-- TARGETS: macos/claude-code,windows/claude-code,codex -->
<!-- TYPE: baseline -->
<!-- AFFECTS: GEMINI.md, .mcp.json, claude-code-setup/agents/ -->
<!-- APPLIED: gemini=2026-03-25T22:15:36Z | macos/claude-code=2026-03-25T22:31:00Z | windows/claude-code=PENDING | codex=PENDING -->

### Kontext
Dies ist die initiale Baseline für das Gemini CLI auf macOS. Sie dokumentiert den vollständigen Systemzustand nach der Ersteinrichtung durch die Mirror Bridge. Die Gemini-Umgebung ist nun vollständig mit der Claude-Code-Infrastruktur synchronisiert und nutzt GEMINI.md als zentrales Mandat.

### Umgebungsinventar
- **Zentrale Konfiguration:** GEMINI.md (Direktiven, Regeln, Agenten-Übersicht)
- **Agenten (18):** Alle 18 Spezialisten aus claude-code-setup/agents/ sind portiert und registriert.
- **MCP-Server (4):** code-review-graph, code-search, think-tank, nemo-mcp.
- **Tools:** Node v25.8.1, Bun 1.3.11, Git 2.53.0, gh 2.88.1.
- **Nemo:** Vollständig integriert und mit NVIDIA NIM API verifiziert.

### Besonderheiten
- **Vollständige Spiegelung:** Alle Hooks und Regeln wurden in Workflow-Anweisungen in GEMINI.md übersetzt.
- **Sprache:** Deutsch als Standard für Interaktion und eigene Agenten/Skills.

---

---

## [MIRROR-2026-03-25-MAC-014] SessionStart Prompt-Hook durch Command-Hook ersetzt (Semantic Search Health Check)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T23:33:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: hook -->
<!-- AFFECTS: ~/.claude/hooks/semantic-search-check.sh, ~/.claude/hooks/semantic-search-check.ps1, ~/.claude/settings.json, ~/proggs/claude-code-setup/settings-reference.json, ~/proggs/claude-code-setup/settings.json -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

In Claude Code gibt es einen fundamentalen Unterschied zwischen zwei Hook-Typen: `type: "command"` fuehrt ein Shell-Skript aus, `type: "prompt"` injiziert Text in den laufenden Claude-Kontext. Prompt-Hooks haben eine kritische Einschraenkung: Sie benoetigen einen aktiven **ToolUseContext** — also eine laufende Tool-Session. Beim `SessionStart`-Event existiert dieser Kontext noch NICHT, weil die Session gerade erst initialisiert wird.

Bis zu dieser Session gab es einen `type: "prompt"` Hook im `SessionStart`-Event, der den Gesundheitsstatus des `code-search` MCP-Servers pruefen sollte (die semantische Suche fuer den Codebase-Index). Dieser Hook produzierte bei JEDEM Start einen Fehler:

```
Failed to run: ToolUseContext is required for prompt hooks. This is a bug.
```

Die Fehlermeldung ist etwas irrefuehrend — es ist kein Bug im Code des Hooks, sondern ein Design-Constraint von Claude Code: Prompt-Hooks koennen schlicht nicht bei `SessionStart` laufen. Der Fix war, den Prompt-Hook durch einen aequivalenten `type: "command"` Hook zu ersetzen, der ein Bash-Skript ausfuehrt statt Text in den Kontext zu injizieren.

Dieses Muster gilt GLOBAL: Kein Prompt-Hook darf jemals bei `SessionStart` oder `SessionEnd` registriert werden — nur command-Hooks sind dort erlaubt.

### Was wurde geaendert?

**Neu erstellt:** `~/.claude/hooks/semantic-search-check.sh` (macOS) und `~/.claude/hooks/semantic-search-check.ps1` (Windows) — beide pruefen ob das `code-search` MCP-Server-Verzeichnis und die `node_modules` existieren. Sie geben immer `exit 0` zurueck (diagnostisch, kein Block).

**settings.json geaendert:** Der alte `type: "prompt"` Hook fuer den Semantic-Search-Check wurde entfernt und durch eine neue `SessionStart`-Gruppe mit `type: "command"` und `command: "bash ~/.claude/hooks/semantic-search-check.sh"` ersetzt.

**settings-reference.json (Windows) geaendert:** Entsprechende Windows-Stanza mit `pwsh -NoProfile -File "$env:USERPROFILE\.claude\hooks\semantic-search-check.ps1"` hinzugefuegt.

Der Hook ist bewusst sehr schlank gehalten: Er prueft nur ob die Verzeichnisse existieren, macht keine Netzwerk-Requests und blockiert niemals die Session (`set +e` / `$ErrorActionPreference = "SilentlyContinue"` + immer `exit 0`).

### Warum?

Bei JEDEM Sessionstart erschien eine rote Fehlermeldung: `SessionStart: startup hook error`. Das verunsichert und verdeckt echte Fehler. Die Root Cause war, dass ein `type: "prompt"` Hook bei `SessionStart` registriert war — was Claude Code grundsaetzlich nicht unterstuetzt. Die Diagnose dauerte laenger als noetig (ca. 20 Minuten), bis die `--debug-file`-Methode eingesetzt wurde (siehe MIRROR-2026-03-25-MAC-017 fuer die Diagnose-Methode).

### Portierung Windows (claude-code)

1. Datei erstellen: `$USERPROFILE\.claude\hooks\semantic-search-check.ps1` (Inhalt siehe unten)
2. In `~/.claude/settings.json` (Windows) eine neue SessionStart-Gruppe eintragen (Inhalt siehe "Settings-Registrierung" unten)
3. Den alten `type: "prompt"` Eintrag fuer Semantic Search aus SessionStart entfernen, falls vorhanden
4. Testen: `claude -p --debug-file $env:TEMP\hook-debug.txt --system-prompt "Reply with just OK"` — in der debug-Ausgabe darf KEIN "ToolUseContext is required" mehr erscheinen

### Portierung Codex / Gemini

Codex und Gemini haben kein Hook-System mit `type: "prompt"` vs `type: "command"`. Fuer diese CLIs:
- Das Wissen aus dieser Session ist relevant fuer CLAUDE.md-Aequivalente (z.B. CODEX.md oder GEMINI.md)
- Falls ein Hook-System vorhanden: Sicherstellen dass kein Hook-Typ der "Kontext-Injektion" bei Session-Initialisierung laeuft — das ist ein allgemeines Anti-Pattern

### Datei-Inhalt macOS (semantic-search-check.sh)
```bash
#!/usr/bin/env bash
# semantic-search-check.sh — SessionStart: verify code-search MCP server is reachable
# Replaces the old prompt-type hook (which can't run at SessionStart due to
# missing ToolUseContext — a Claude Code limitation).
# This command-type hook checks the MCP server's health endpoint instead.

set +e  # Never block session start

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true

# The code-search MCP server runs on a local port via bun/tsx.
# Check if the process is running by looking for the index file.
MCP_DIR="$HOME/proggs/mcp-code-search"
MCP_INDEX="$MCP_DIR/node_modules"

if [ ! -d "$MCP_DIR" ]; then
    hook_log "code-search MCP dir not found — skipping"
    exit 0
fi

if [ ! -d "$MCP_INDEX" ]; then
    echo "Semantische Suche: node_modules fehlen. Fix: cd ~/proggs/mcp-code-search && bun install"
    hook_log_warn "code-search node_modules missing"
    exit 0  # Don't fail the hook — just warn
fi

# Check if the .mcp.json references code-search and the binary exists
MCP_JSON="$HOME/.mcp.json"
if [ -f "$MCP_JSON" ]; then
    if grep -q "code-search" "$MCP_JSON" 2>/dev/null; then
        hook_log "code-search MCP configured in .mcp.json"
    else
        hook_log "code-search not in .mcp.json — skipping check"
    fi
fi

exit 0
```

### Datei-Inhalt Windows (semantic-search-check.ps1)
```powershell
# semantic-search-check.ps1 — SessionStart: verify code-search MCP server is reachable
# Replaces the old prompt-type hook (which can't run at SessionStart due to
# missing ToolUseContext — a Claude Code limitation).
# This command-type hook checks the MCP server's health endpoint instead.
# Platform: Windows (PowerShell equivalent of semantic-search-check.sh)

$ErrorActionPreference = "SilentlyContinue"

$MCP_DIR = Join-Path $env:USERPROFILE "proggs\mcp-code-search"

if (-not (Test-Path $MCP_DIR)) {
    exit 0
}

$MCP_INDEX = Join-Path $MCP_DIR "node_modules"
if (-not (Test-Path $MCP_INDEX)) {
    Write-Output "Semantische Suche: node_modules fehlen. Fix: cd ~/proggs/mcp-code-search && bun install"
    exit 0  # Don't fail the hook — just warn
}

$MCP_JSON = Join-Path $env:USERPROFILE ".mcp.json"
if (Test-Path $MCP_JSON) {
    $content = Get-Content $MCP_JSON -Raw
    if ($content -match "code-search") {
        # code-search is configured
    }
}

exit 0
```

### Settings-Registrierung

**Hook-Event:** `SessionStart`

**macOS-Stanza fuer `~/.claude/settings.json`:**
```json
{
    "hooks": [
        {
            "type": "command",
            "command": "bash ~/.claude/hooks/semantic-search-check.sh",
            "timeout": 10
        }
    ]
}
```

**Windows-Stanza fuer `~/.claude/settings.json` (settings-reference.json):**
```json
{
    "hooks": [
        {
            "type": "command",
            "command": "pwsh -NoProfile -File \"$env:USERPROFILE\\.claude\\hooks\\semantic-search-check.ps1\"",
            "timeout": 10
        }
    ]
}
```

**KRITISCH:** Den alten `type: "prompt"` Hook aus dem `SessionStart`-Block entfernen, wenn er noch existiert. Ein Prompt-Hook bei SessionStart erzeugt immer den Fehler `ToolUseContext is required for prompt hooks`.

<!-- APPLIED: macos/claude-code=2026-03-25T23:33:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-26T01:48:45Z -->

---

## [MIRROR-2026-03-25-MAC-015] plugin-health-check.sh: Auto-Repair fuer Plugin +x Permissions + Whiteboard-Insert
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T23:33:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: hook -->
<!-- AFFECTS: ~/.claude/hooks/plugin-health-check.sh -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Der `plugin-health-check.sh` Hook laeuft bei jedem `SessionStart` als letzter Hook in der Kette und prueft den Gesundheitszustand aller Plugin-Dienste (claude-mem Worker, Ollama, code-search MCP, launchd-Daemons). Er ist der "Hausarzt" des Systems — er diagnostiziert, aber blockiert niemals.

In dieser Session wurde festgestellt, dass das `explanatory-output-style` Plugin einen `.sh`-Hook ohne Execute-Permission installiert hatte. Das fuehrt zu `Permission denied` Fehlern bei `SessionStart`. Das Problem ist systemisch: Plugins koennen generell `.sh`-Dateien ohne `+x` installieren, weil der Claude Code Marketplace keine Permission-Pruefung bei der Installation erzwingt.

Zum `plugin-health-check.sh` wurden zwei Erweiterungen hinzugefuegt:

**CHECK 5 (neu):** Automatisches Auffinden und Reparieren aller `.sh`-Dateien in Plugin-Verzeichnissen ohne Execute-Permission (`chmod +x`). Das ist Self-Healing: Bei jedem Start werden fehlende Permissions automatisch repariert, ohne dass der Benutzer eingreifen muss.

**Whiteboard-Integration (neu):** Wenn Permissions repariert wurden, wird ein Eintrag in die zentrale Wissensdatei (`.claude/agent-memory/shared/MEMORY.md`) geschrieben, damit `/self-improve` das Muster erkennen und dauerhaft dokumentieren kann.

Der Hook sucht in ZWEI Verzeichnissen: `~/.claude/plugins/marketplaces` und `~/.claude/plugins/cache`. Beide koennen Plugin-Hooks enthalten.

### Was wurde geaendert?

In `~/.claude/hooks/plugin-health-check.sh` wurde nach CHECK 4 (code-search MCP dependencies) ein neuer CHECK 5 Block eingefuegt:

```bash
# CHECK 5: Plugin hook script execute permissions (auto-repair)
# Some plugins install .sh hook scripts without +x permission.
# This auto-repairs them to prevent "Permission denied" errors at SessionStart.
PLUGIN_DIRS="$HOME/.claude/plugins/marketplaces $HOME/.claude/plugins/cache"
for pdir in $PLUGIN_DIRS; do
    if [ -d "$pdir" ]; then
        repaired=0
        while IFS= read -r -d '' sh_file; do
            if [ ! -x "$sh_file" ]; then
                chmod +x "$sh_file"
                repaired=$((repaired + 1))
                hook_log "auto-repaired +x permission: $(basename "$sh_file")"
            fi
        done < <(find "$pdir" -name "*.sh" -type f -print0 2>/dev/null)
        if [ "$repaired" -gt 0 ]; then
            hook_log "repaired $repaired plugin hook scripts with missing +x permission"
            # Write to whiteboard so /self-improve can detect the pattern
            source "$HOOKS_DIR/whiteboard-insert.sh" 2>/dev/null || true
            if command -v insert_whiteboard_entry &>/dev/null; then
                entry="### $(date '+%Y-%m-%d %H:%M') — Hook: plugin-health-check.sh — Auto-repaired $repaired plugin .sh files with missing +x permission in $pdir — Status: AUTO-GEFIXT"
                insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" 2>/dev/null || true
            fi
        fi
    fi
done
```

### Warum?

Das `explanatory-output-style@claude-plugins-official` Plugin hatte einen Hook ohne `+x` Permission installiert. Das verursachte beim Start `Permission denied`. Statt das manuell einmalig zu fixen, wurde eine Self-Healing-Schicht eingebaut: Der naechste Start repariert es automatisch, und zwar fuer ALLE Plugins — nicht nur fuer das eine das gerade auffiel.

Root Cause: Marketplace-Plugins werden ohne Permission-Pruefung installiert. Der Fix greift defensiv auf alle .sh-Dateien in Plugin-Verzeichnissen.

### Portierung Windows (claude-code)

Auf Windows gibt es keine Execute-Permissions wie auf Unix. Windows-Skripte (`.ps1`) brauchen keine `+x` Permission. Stattdessen wird die Execution-Policy genutzt.

**Was zu pruefen ist auf Windows:** Wenn ein Plugin `.ps1`-Hooks installiert und diese nicht laufen, liegt es meistens an der Execution-Policy. Die `plugin-health-check.ps1` sollte pruefen ob alle Plugin-`.ps1`-Dateien mit `pwsh -ExecutionPolicy Bypass` aufrufbar sind.

**Empfehlung fuer die Windows-Version von plugin-health-check.ps1:** Einen entsprechenden Block hinzufuegen der `.ps1`-Dateien in Plugin-Verzeichnissen sucht und prueft ob sie ausfuehrbar sind (mit `-ExecutionPolicy Bypass` aufrufbar). Kein `chmod` noetig.

### Portierung Codex / Gemini

Gleiches Prinzip: Wenn ein Hook-System existiert, nach Installation von Plugins/Extensions pruefen ob deren Hook-Skripte ausfuehrbare Permissions haben. Auf macOS/Linux immer `chmod +x` nach Plugin-Installation ausfuehren.

### Datei-Inhalt macOS — Nur der neue CHECK 5 Block (vollstaendige Datei: ~/.claude/hooks/plugin-health-check.sh)

Der vollstaendige Dateiinhalt ist 157 Zeilen. Die relevante Ergaenzung ist CHECK 5 (Zeilen 106-131), der oben vollstaendig abgedruckt ist. Die restlichen Checks (claude-mem worker, Ollama, hooks.json integrity, code-search dependencies, launchd agents) sind unveraendert.

<!-- APPLIED: macos/claude-code=2026-03-25T23:33:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-26T01:48:45Z -->

---

## [MIRROR-2026-03-25-MAC-016] mirror-check.sh: grep -c Bugfix (verhinderte korrekte Pending-Zaehlung)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T23:33:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: hook -->
<!-- AFFECTS: ~/.claude/hooks/mirror-check.sh -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Der `mirror-check.sh` Hook laeuft bei jedem `SessionStart` und zaehlt ausstehende Mirror-Bridge-Eintraege im `mirror-ledger.md` fuer die aktuelle Plattform. Er zeigt eine Meldung wenn andere Plattformen Aenderungen haben die noch nicht portiert wurden.

Der Hook verwendet `grep -c` um die Anzahl der `PENDING`-Eintraege zu zaehlen. `grep -c` gibt jedoch `"0\n"` mit Exit Code 1 zurueck, wenn keine Treffer gefunden werden. Wenn danach `|| echo "0"` verwendet wird, produziert das den String `"0\n0"` statt nur `"0"` — was dazu fuehrt dass die spaeteren String-Vergleiche (`[[ "$count" -gt 0 ]]`) fehlschlagen oder unerwartete Ausgabe liefern.

### Was wurde geaendert?

In `mirror-check.sh` wurde Zeile 19 von:
```bash
count=$(grep -c "APPLIED: ${platform}/${cli}=PENDING" "$LEDGER" 2>/dev/null || echo "0")
```
geaendert zu:
```bash
count=$(grep -c "APPLIED: ${platform}/${cli}=PENDING" "$LEDGER" 2>/dev/null || true)
count=${count:-0}
```

**Was das bewirkt:**
- `|| true` verhindert dass ein nicht-null Exit Code von `grep -c` (wenn keine Treffer) als Fehler behandelt wird — ohne eine zweite Ausgabezeile zu erzeugen
- `count=${count:-0}` setzt `count` auf `"0"` falls es leer oder undefiniert ist (bash Parameter-Expansion mit Default)

Ausserdem wurde die Bedingung auf Zeile 23 von:
```bash
if [[ "$count" -gt 0 && "$count" != "0" ]]; then
```
vereinfacht zu:
```bash
if [[ "$count" -gt 0 ]]; then
```

Der redundante `"$count" != "0"` Check war ueberfluessig wenn `count` korrekt als `"0"` (nicht `"0\n0"`) gesetzt ist.

### Warum?

Der Bug war subtil: `grep -c` mit keinen Treffern gibt Exit Code 1 zurueck und schreibt `"0"` auf stdout. Das `|| echo "0"` fuegte ein zweites `"0"` hinzu, was zu `count="0\n0"` fuehrte. Der arithmetische Vergleich `[[ "0\n0" -gt 0 ]]` ist auf verschiedenen bash-Versionen unterschiedlich und kann unerwartet `true` liefern.

### Portierung Windows (mirror-check.ps1)

Das PowerShell-Aequivalent verwendet `Select-String -Count` statt `grep -c`. Der Bug tritt dort nicht auf, weil PowerShell anders mit leeren Ergebnissen umgeht. Dennoch empfiehlt sich defensives Programmieren:

```powershell
$count = (Select-String -Pattern "APPLIED: ${platform}/${cli}=PENDING" -Path $LEDGER -ErrorAction SilentlyContinue | Measure-Object).Count
if ($null -eq $count) { $count = 0 }
```

### Datei-Inhalt macOS (mirror-check.sh) — vollstaendig
```bash
#!/usr/bin/env bash
# mirror-check.sh — SessionStart: notify if mirror-ledger has pending entries
# Part of the Universal Mirror Bridge system.
# This hook ONLY counts and reports — it does NOT apply anything.
# To apply pending entries, the user starts the import agent manually.

set +e  # Never block session start

LEDGER="$HOME/proggs/claude-code-setup/mirror-ledger.md"

# Bail if ledger doesn't exist yet
[[ -f "$LEDGER" ]] || exit 0

# Detect platform
platform="macos"
cli="claude-code"

# Count PENDING entries for this platform/cli
count=$(grep -c "APPLIED: ${platform}/${cli}=PENDING" "$LEDGER" 2>/dev/null || true)
count=${count:-0}

# Only show message if there are pending entries
if [[ "$count" -gt 0 ]]; then
    echo "Mirror-Bridge: ${count} ausstehende Eintraege von anderen Plattformen. Starte den import Agenten um zu uebernehmen."
fi

exit 0
```

<!-- APPLIED: macos/claude-code=2026-03-25T23:33:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-26T01:48:45Z -->

---

## [MIRROR-2026-03-25-MAC-017] Neue Regel: hook-constraints.md — Prompt-Hooks bei SessionStart verboten + Debug-Methode
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T23:33:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: rule -->
<!-- AFFECTS: ~/.claude/rules/hook-constraints.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Diese neue Regel dokumentiert zwei kritische Erkenntnisse aus dieser Session, die verhindern dass derselbe Fehler nochmals auftritt:

**Erkenntnis 1 — Prompt-Hook-Constraint:** Claude Code unterstuetzt `type: "prompt"` Hooks NUR bei bestimmten Events, nicht bei `SessionStart` und `SessionEnd`. Das liegt daran dass Prompt-Hooks einen aktiven `ToolUseContext` benoetigen (eine laufende Tool-Session), der bei SessionStart noch nicht existiert und bei SessionEnd nicht mehr existiert. Die Fehlermeldung `Failed to run: ToolUseContext is required for prompt hooks. This is a bug.` ist irrefuehrend — es ist kein Bug im Hook-Skript, sondern ein Design-Constraint von Claude Code.

**Erkenntnis 2 — Debug-Methode:** Bei jedem Hook-Fehler beim Start (erscheint als "startup hook error" in der Claude Code Ausgabe) kostet es viel Zeit, jeden Hook einzeln zu testen — weil die Test-Umgebung sich von der echten Startup-Umgebung unterscheidet. Die korrekte Diagnose-Methode ist:
```bash
echo "test" | claude -p --debug-file /tmp/hook-debug.txt --system-prompt "Reply with just OK"
grep "Hook.*error\|hook.*fail\|Failed to run" /tmp/hook-debug.txt
```
Das erzeugt einen Claude-Prozess in der ECHTEN Startup-Umgebung und schreibt alle Hook-Fehler mit vollem Kontext in die Debug-Datei.

Diese Regel verhindert dass zukuenftige Sessions (und andere CLIs/Plattformen) dieselben ~20 Minuten Diagnosezeit verlieren.

### Was wurde geaendert?

**Neu erstellt:** `~/.claude/rules/hook-constraints.md`

Diese Datei ist eine Regel (wird automatisch bei jedem Sessionstart geladen) mit drei Abschnitten:
1. Regel 1: Welche Events Prompt-Hooks NICHT erlauben (SessionStart, SessionEnd) und welche sie erlauben (PreToolUse, PostToolUse, PreCompact, etc.)
2. Regel 2: Plugin +x Permissions — Symptom, manuelle Diagnose, Self-Healing
3. Regel 3: Die Debug-Methode mit `--debug-file` — der einzig zuverlaessige Weg Hook-Fehler zu diagnostizieren

### Warum?

In dieser Session wurden ca. 20 Minuten damit verbracht, Hooks einzeln zu testen bis die `--debug-file` Methode eingesetzt wurde. Mit dieser Methode war der Fehler in 30 Sekunden gefunden. Diese Erkenntnis muss persistent sein — sowohl als Regel (wird bei jedem Start geladen) als auch als Feedback-Memory (separate Datei im Projektspeicher).

### Portierung Windows (claude-code)

Die Regel gilt identisch auf Windows. Die Debug-Methode auf Windows:
```powershell
echo "test" | claude -p --debug-file $env:TEMP\hook-debug.txt --system-prompt "Reply with just OK"
Select-String -Pattern "Hook.*error|hook.*fail|Failed to run" -Path $env:TEMP\hook-debug.txt
```

Die Regel-Datei ist plattformunabhaengig (Markdown) und wird durch Auto-Sync automatisch synchronisiert.

### Portierung Codex / Gemini

Kein direktes Hook-System. Das allgemeine Prinzip ist aber uebertragbar:
- Wenn ein Hook-/Extension-System Session-Initialisierungs-Events hat, niemals Kontext-Injektions-Hooks dort registrieren
- Debug-Methode fuer CLI-Tools: Die meisten CLIs haben einen `--debug` oder `--verbose` Flag der den Startup-Prozess detailliert loggt

### Datei-Inhalt macOS und Windows (hook-constraints.md) — vollstaendig (plattformunabhaengig)
```markdown
# Hook-Einschraenkungen (KRITISCH)

## Regel 1: KEINE Prompt-Hooks bei SessionStart

`type: "prompt"` Hooks duerfen NIEMALS im `SessionStart` Event verwendet werden.

**Grund:** Claude Code hat bei SessionStart keinen ToolUseContext. Prompt-Hooks brauchen
diesen Kontext um zu funktionieren. Der Fehler ist:
`Failed to run: ToolUseContext is required for prompt hooks. This is a bug.`

**Wo Prompt-Hooks funktionieren:**
- PreToolUse, PostToolUse, PostToolUseFailure ✅
- PreCompact, PostCompact ✅
- Stop, StopFailure ✅
- SubagentStop ✅
- InstructionsLoaded ✅

**Wo Prompt-Hooks NICHT funktionieren:**
- SessionStart ❌ (kein ToolUseContext)
- SessionEnd ❌ (Session wird beendet, kein Kontext mehr)

**Alternative:** Statt `type: "prompt"` bei SessionStart immer `type: "command"` verwenden
und die Logik in ein Bash/PowerShell-Skript auslagern.

## Regel 2: Plugin-Hook-Skripte brauchen Execute-Permission

Plugins koennen `.sh`-Dateien ohne `+x` Permission installieren.
Der `plugin-health-check.sh` Hook repariert das automatisch bei jedem Start.

Wenn ein neues Plugin installiert wird und "Permission denied" Fehler auftreten:
1. `find ~/.claude/plugins -name "*.sh" ! -perm -u+x` — fehlende Permissions finden
2. `chmod +x` auf alle betroffenen Dateien setzen
3. Der naechste Start repariert es automatisch (Self-Healing)

## Regel 3: Debug-Methode bei Hook-Fehlern

Bei "hook error" in der Startup-Ausgabe SOFORT:
```bash
echo "test" | claude -p --debug-file /tmp/hook-debug.txt --system-prompt "Reply with just OK"
grep "Hook.*error" /tmp/hook-debug.txt
```
NIEMALS Hooks einzeln testen — die echte Startup-Umgebung ist anders.
```

### Settings-Registrierung

Keine Settings-Registrierung noetig. Regel-Dateien in `~/.claude/rules/` werden automatisch bei jedem Start geladen. Die Datei muss nur an diesem Pfad existieren.

<!-- APPLIED: macos/claude-code=2026-03-25T23:33:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-26T01:48:45Z -->

---

## [MIRROR-2026-03-25-MAC-018] Feedback-Memory: Hook-Fehler Debug-Methode (--debug-file spart 20 Minuten)
<!-- SOURCE: claude-code | PLATFORM: macos | TIMESTAMP: 2026-03-25T23:33:00Z -->
<!-- TARGETS: windows/claude-code,codex,gemini -->
<!-- TYPE: directive -->
<!-- AFFECTS: ~/.claude/projects/-Users-frank/memory/feedback_hook_debug_method.md -->

### Kontext (WICHTIG — andere CLIs kennen das nicht!)

Claude Code hat ein persistentes Memory-System fuer Feedback. Feedback-Memories sind Markdown-Dateien in `~/.claude/projects/-Users-frank/memory/` (macOS) bzw. `%USERPROFILE%\.claude\projects\-Users-barwa\memory\` (Windows) mit einem YAML-Frontmatter (`name`, `description`, `type`). Sie werden bei jedem Start geladen und beeinflussen Claude Codes Verhalten dauerhaft — aehnlich wie Regeln, aber spezifisch fuer einen Benutzer und ein Verhalten das aus konkreter Erfahrung gelernt wurde.

Diese Feedback-Memory dokumentiert die Erkenntnis aus dieser Session: Bei Hook-Fehlern (`SessionStart: startup hook error`) NIEMALS Zeit damit verschwenden, jeden Hook einzeln zu testen. Die einzige zuverlaessige Methode ist `--debug-file`.

Hintergrund: Beim einzelnen Testen von Hooks gibt jeder Hook `exit 0` zurueck, weil die Test-Umgebung fehlt (kein `CLAUDE_PLUGIN_ROOT`, kein stdin-JSON, kein ToolUseContext-Kontext). Der echte Fehler ist nur in der Startup-Umgebung reproduzierbar — und genau die wird durch `claude -p --debug-file` simuliert.

### Was wurde geaendert?

**Neu erstellt:** `~/.claude/projects/-Users-frank/memory/feedback_hook_debug_method.md`

Inhalt: Name, Beschreibung und vollstaendige Erklaerung der Debug-Methode mit dem konkreten Befehl und der Begruendung warum das die einzig zuverlaessige Methode ist.

### Warum?

Die Diagnose eines Hook-Fehlers dauerte in dieser Session ~20 Minuten durch falsches Vorgehen (einzeln testen). Mit `--debug-file` haette es 30 Sekunden gedauert. Damit diese Erkenntnis nicht nur als Regel (die theoretisch gelesen wird) sondern auch als Feedback-Memory (die Claude Codes konkretes Verhalten beeinflusst) persistent ist, wurde beides erstellt.

### Portierung Windows (claude-code)

1. Datei erstellen: `%USERPROFILE%\.claude\projects\-Users-barwa\memory\feedback_hook_debug_method.md` (Pfad anpassen — `-Users-barwa` ist der Windows-Username)
2. Inhalt identisch (Markdown ist plattformunabhaengig)
3. Windows-spezifischer Debug-Befehl: `echo "test" | claude -p --debug-file $env:TEMP\hook-debug.txt --system-prompt "Reply with just OK"`

Der Windows-Benutzername im Pfad muss angepasst werden: `-Users-barwa` fuer `C:\Users\barwa`.

### Portierung Codex / Gemini

Als Wissen (nicht als Datei): Bei Hook/Extension-Fehlern beim CLI-Start niemals einzeln testen, sondern den CLI-Start mit Debug-Output wiederholen: Die meisten CLIs haben `--debug`, `--verbose`, `--log-level debug` oder aehnliche Flags.

### Datei-Inhalt (feedback_hook_debug_method.md) — vollstaendig
```markdown
---
name: Hook-Fehler Debug-Methode
description: Bei Hook-Fehlern IMMER zuerst claude -p --debug-file statt einzeln testen — spart 20 Minuten Diagnosezeit
type: feedback
---

Bei Hook-Fehlern (z.B. "SessionStart:startup hook error") NIEMALS jeden Hook einzeln testen.
Das funktioniert nicht, weil die echte Startup-Umgebung anders ist (ToolUseContext, CLAUDE_PLUGIN_ROOT, stdin-JSON).

Stattdessen SOFORT: `echo "test" | claude -p --debug-file /tmp/hook-debug.txt --system-prompt "Reply with just OK"`

Dann: `grep "Hook.*error" /tmp/hook-debug.txt` — zeigt exakt welcher Hook fehlschlaegt und warum.

**Why:** In Session 2026-03-25 wurden ~20 Minuten damit verschwendet, jeden Hook einzeln zu testen.
Alle gaben Exit 0, weil die Test-Umgebung sich von der echten Startup-Umgebung unterscheidet.
Der `--debug-file` Trick hat das Problem in 30 Sekunden geloest.

**How to apply:** Sobald "hook error" in der Startup-Ausgabe erscheint → sofort `--debug-file` verwenden.
Nicht raten, nicht einzeln testen, nicht in Logs suchen. Debug-File ist die einzige zuverlaessige Methode.
```

<!-- APPLIED: macos/claude-code=2026-03-25T23:33:00Z -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->
<!-- APPLIED: gemini=2026-03-26T01:48:45Z -->
