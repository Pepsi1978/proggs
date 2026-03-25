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
<!-- APPLIED: gemini=PENDING -->

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
3. Beim import: APPLIED-Status mit codex=PENDING bzw. gemini=PENDING suchen

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
<!-- APPLIED: gemini=PENDING -->

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
<!-- APPLIED: gemini=PENDING -->

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
<!-- APPLIED: gemini=PENDING -->

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
<!-- APPLIED: gemini=PENDING -->

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
<!-- APPLIED: gemini=PENDING -->

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
<!-- APPLIED: gemini=PENDING -->
