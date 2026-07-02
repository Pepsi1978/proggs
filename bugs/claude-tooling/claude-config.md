# Bekannte Bugs & Fallen: Claude Code Konfiguration & Regeln

> **PFLICHT-LESEN vor JEDER Arbeit an der Claude-Code-Harness-Konfiguration:**
> `CLAUDE.md`, `~/.claude/rules/*.md` (+ Repo-Spiegelung `claude-code-setup/rules/`),
> `settings.json` / `settings.local.json`, Skills (`SKILL.md`), Slash-Commands,
> Custom-Subagents/Agents, Auto-Memory/`MEMORY.md`, Plugins/Marketplaces.
>
> **Abgrenzung (NICHT hier — eigene Almanache):** Hooks → `claude-hooks.md` ·
> MCP-Server-Bau → `mcp-server.md` · Python-Scripting → `python-windows.md`.
> Bei Ueberschneidung (z.B. `settings.json`-hooks-Sektion) gilt dort der jeweilige Almanach.
>
> **Stand:** recherchiert am **2026-06-07**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax) fuer **Claude Code 2.1.198** (Windows/MINGW64,
> **Anker:** claude-code=2.1.198  <!-- maschinenlesbar fuer check-version-anchor.py -->
> Node 24.15). Issue-Status hart per `gh issue view` verifiziert. Quellen: offizielle Doku
> (code.claude.com/docs) + Changelog, GitHub-Issues (anthropics/claude-code), Community.
> Loesungen sind funktionserhaltend (nie "Feature weglassen").

> **Update 2026-07-02:** Neue relevante Deltas seit 2.1.168: bedingte `.claude/rules/` laden bei Symlink-Zielpfaden wieder korrekt (2.1.198), Hook-Matcher mit Bindestrich-Identifiern matchen nicht mehr versehentlich als Substring (2.1.195), Organisation-Default-Models und neue Env-Flags (`CLAUDE_ENABLE_STREAM_WATCHDOG=0`, `CLAUDE_CODE_DISABLE_MOUSE_CLICKS`, `CLAUDE_CODE_DISABLE_BG_SHELL_PRESSURE_REAP=1`) sind dokumentiert. Bestehende Grundregeln bleiben: harte Garantien in Hooks/Settings, nicht nur in CLAUDE.md.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): **Hochrisiko-Bereich (Stufe C)** — vor echter
> Arbeit hier ist der VOLLTEXT Pflicht (`Read` ohne `limit`); dieser Kurzcheck dient nur der
> Schnell-Orientierung. Bei JEDEM Fehler im Bereich gilt ebenfalls Volltext-Pflicht (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Verhalten MUSS immer laufen | Hook statt CLAUDE.md (CLAUDE.md ist advisory) | §1.1 |
| 2 | CLAUDE.md waechst | Ziel < 200 Zeilen, Detail in Rules/Skills (Context-Rot) | §1.2 |
| 3 | Token sparen via `@import` | Spart KEINE Tokens — laedt voll; nur path-scoped/Skill spart | §1.3 |
| 4 | settings.json schreiben | Ein JSON-Fehler ODER BOM killt ALLES still — validieren | §3.1 |
| 5 | Windows: settings/.mcp.json | UTF-8-BOM bricht Parse — BOM-frei speichern | §3.2 |
| 6 | `MEMORY.md` pflegen | Nur erste ~200 Zeilen / 25 KB laden — Index kurz | §6.1 |
| 7 | User-Rule mit `paths:` | NIE `paths:` user-level (ignoriert) | §2.1 |
| 8 | path-scoped Rule | Triggert nur bei Read, nicht Write; Globs quoten | §2.2 |
| 9 | Skill mit `paths:` | ENTFERNEN — macht Skill undiscoverable | §4.1 |
| 10 | Skill `description` | Einzeilig in Quotes, Trigger front-loaden | §4.2 |
| 11 | Subagent braucht Kontext | Erbt CLAUDE.md/Rules nicht — per Hook injizieren | §2.4 |
| 12 | Custom-Agent starten | `general-purpose` + Prompt, nie Custom-`subagent_type` | §5.1 |
| 13 | `allow`-Liste setzen | Keine Whitelist — Sperren nur via `deny` | §3.6 |
| 14 | Config-/Skill-Datei (Win) | LF halten (CRLF bricht Edit-Tool) | §8.1 |
| 15 | Subagent crasht (0 Token) | `ENABLE_TOOL_SEARCH` + `tools:`-Whitelist | §5.4 |

---

## 1. CLAUDE.md

### 1.1 CLAUDE.md wird "ignoriert" / nicht strikt befolgt  ⭐ HAEUFIG / per Design
**Symptom:** Klare Regeln werden missachtet, teils mehrfach pro Session; Claude bestaetigt die Regel und bricht sie trotzdem.
**Ursache:** CLAUDE.md/Rules werden als **User-Message NACH dem System-Prompt** geliefert, NICHT als erzwungene Config. Doku woertlich: "treats them as context, not enforced configuration. There's no guarantee of strict compliance, especially for vague or conflicting instructions."
**Versionen:** per Design, alle Versionen.
**Beleg (quantitativ, Recherche 2026-06-27):** Instruction-Compliance sinkt **linear mit der Regel-Anzahl** ("double the instructions, halve the compliance"); selbst beste Modelle befolgen **< 30 %** der Instructions in Agent-Szenarien perfekt, Frontier-Thinking-Modelle saettigen bei **~150-200** Instructions (Jaroslawicz et al. 2025). Praxistest: eine `AGENTS.md` mit nur **2** simplen Regeln → **4 von 5 Modellen ignorierten sie**. Kernsatz: *"Rules in prompts are requests. Hooks in code are laws."* → bestaetigt die FIX-Richtung. Vertiefung: `best-practices/agents/anti-halluzination-regeln.md` §7.
**FIX:** Verhalten, das IMMER laufen muss → **Hook** (PreToolUse/Lifecycle, laeuft als Shell-Befehl unabhaengig von Claudes Entscheidung — siehe `claude-hooks.md`). System-Prompt-Ebene → `--append-system-prompt`. Instruktionen **spezifisch + knapp** formulieren, **wenige statt viele** (lineare Degradation), Widersprueche zwischen Regeln entfernen. Siehe `best-practices/claude-tooling/arbeitsweise.md` + `best-practices/claude-tooling/kontext.md`.
**Quelle:** docs/memory · #19635 · #34197 · #60346 · Jaroslawicz et al. 2025 · #15443/#6120/#18660

### 1.2 Context-Rot: zu grosse CLAUDE.md senkt Genauigkeit  ⭐ HAEUFIG
**Symptom:** Je laenger die CLAUDE.md, desto schlechter Befolgung/Genauigkeit — Qualitaet faellt schon ab ~50 % Kontextfuellung, nicht erst bei 100 %.
**Ursache:** CLAUDE.md wird bei JEDER Session **in voller Laenge** geladen (egal wie lang) → fixer Token-Sockel, sinkendes Signal-Rausch-Verhaeltnis.
**Versionen:** per Design.
**FIX:** Ziel **< 200 Zeilen**. Zwei-Schichten-Architektur: kompakter Verweis in CLAUDE.md, Volltext in `~/.claude/rules/<thema>.md` (ebenfalls immer geladen, aber als fokussierte Datei) oder in Skills (laden on-demand). Genau das Muster der 3 Hauptdirektiven + der Observability-First-Regel. Siehe `best-practices/claude-tooling/kontext.md` + `best-practices/claude-tooling/token-effizienz.md`.
**Quelle:** docs/memory (Size) · #29971 (Context-Bloat-Tracker)

### 1.3 `@import` reduziert KEINE Tokens (haeufiges Missverstaendnis)
**Symptom:** Erwartung: `@path`-Import laedt nur bei Bedarf → spart Kontext. Realitaet: kein Token-Gewinn.
**Ursache:** Importierte Dateien werden bei Launch **voll expandiert** geladen, genau wie Inline-Text. Import dient nur der Organisation.
**Versionen:** per Design (Lazy-Loading nur Feature-Request, #11759).
**FIX:** Fuer echte Ersparnis path-scoped Rules (projektweit) oder Skills statt `@import`.
**Quelle:** docs/memory · #11759

### 1.4 `@import` — Syntax, Limits, Pfad-Aufloesung (Referenz + Fallen)
**Fakten:** Syntax `@pfad`. Relative Pfade loesen relativ zur **importierenden Datei** auf (nicht CWD). **Max. 4 Hops** Rekursion. Erster externer Import zeigt Approval-Dialog — bei "Decline" bleiben Imports **dauerhaft deaktiviert**, der Dialog kommt nie wieder.
**Fallen:** `@`-Referenzen in Code-Bloecken/Spans werden ggf. nicht als Import erkannt; nicht existierende Pfade werden still nicht expandiert. `@~/.claude/datei.md` (Home-`~`) laedt teils nicht (#8765, NOT_PLANNED) → **absoluten Pfad** verwenden oder Inhalt nach `~/.claude/rules/` legen.
**Versionen:** aktuell; `@~`-Bug per Design/won't-fix.
**Quelle:** docs/memory · #8765

### 1.5 Nested CLAUDE.md (Unterordner) laedt NICHT zuverlaessig on-demand
**Symptom:** Doku sagt: Unterordner-CLAUDE.md laedt beim Lesen von Dateien dort. Praxis: oft wird nur die Root-CLAUDE.md beim Start geladen, Unterordner-Dateien nie injiziert.
**Ursache:** Lazy-Loading fuer Unterordner greift unzuverlaessig. `CLAUDE_CODE_DISABLE_ATTACHMENTS=1` schaltet diese Injection zusaetzlich ganz ab (#48031).
**Versionen:** #24987 CLOSED **NOT_PLANNED** (per Design / won't-fix) — Workaround bleibt aktiv.
**FIX:** Verhaltenskritische Regeln in die **Projekt-Root-CLAUDE.md** (ueberlebt auch Compaction) oder in **path-scoped Rules** (`<projekt>/.claude/rules/` mit gequotetem `paths:`) — die triggern beim Lesen passender Dateien. `InstructionsLoaded`-Hook + `/memory` zum Verifizieren, was wirklich laedt.
**Quelle:** #24987 · #2571 · #48031

### 1.6 CLAUDE.md "vergessen" nach `/compact`
**Symptom:** Nach Compaction weicht Claude von CLAUDE.md-Regeln ab.
**Ursache:** Compaction-Summary ist lossy. Root-CLAUDE.md SOLL nach `/compact` von Disk neu gelesen werden — passiert nicht immer; nested CLAUDE.md wird NICHT re-injiziert; reine Chat-Instruktionen gehen ganz verloren.
**Versionen:** #6354 — teils per Design (lossy summary).
**FIX:** Alles Wichtige in CLAUDE.md schreiben (nicht nur im Chat sagen) → ueberlebt durch Disk-Reread. Den `## Compact Instructions`-Abschnitt in CLAUDE.md pflegen (definiert, was die Zusammenfassung behalten MUSS). Pflicht-Verhalten in Hooks.
**Quelle:** docs/memory · #6354

### 1.7 HTML-Block-Kommentare werden vor Injection gestrippt
**Symptom:** `<!-- ... -->` in CLAUDE.md taucht nicht im Kontext auf.
**Ursache:** Gewollt — werden vor Injection entfernt (sparen Tokens); INNERHALB von Code-Bloecken bleiben sie erhalten; per Read-Tool sichtbar.
**Versionen:** per Design.
**FIX:** Maintainer-Notizen bewusst in `<!-- -->` (sparen Kontext). Inhalt, der WIRKEN soll, nie in HTML-Kommentare. (Relevant fuer die ACE-PROTECTED-ZONE-Marker — die sind absichtlich Kommentare und damit unsichtbar im Kontext, aber per Read schuetzbar.)
**Quelle:** docs/memory

### 1.8 Hierarchie / Lade-Reihenfolge (Referenz) + `claudeMdExcludes`
**Fakten:** Reihenfolge breit→spezifisch, alle **konkateniert** (kein Override): Managed-Policy → User (`~/.claude/CLAUDE.md`) → Project (`./CLAUDE.md` bzw. `./.claude/CLAUDE.md`) → Local (`./CLAUDE.local.md`). Im Baum root→cwd. Bei Widerspruch waehlt Claude "arbitrarily" → Regeln entschlacken.
**Falle (Monorepo/Worktree):** Ancestor-CLAUDE.md fremder Teams werden mitgezogen; im Worktree teils Parent+Worktree doppelt.
**FIX:** `claudeMdExcludes` (Glob, absolute Pfade) in beliebigem Settings-Layer. Managed-Policy-CLAUDE.md ist NICHT exkludierbar (gewollt).
**Quelle:** docs/memory · #23565

### 1.9 AGENTS.md wird nicht gelesen
**Symptom:** Repos mit `AGENTS.md` (fuer andere Agenten) werden ignoriert.
**Ursache:** Claude liest nur `CLAUDE.md`.
**FIX:** `CLAUDE.md` anlegen mit `@AGENTS.md` als erstem Import. Symlink geht auch — aber **Windows-Symlink braucht Admin/Developer-Mode** → dort den `@AGENTS.md`-Import nutzen.
**Quelle:** docs/memory

---

## 2. Regel-Dateien (`~/.claude/rules/`) & path-scoped Rules

### 2.1 `paths:`-Frontmatter in USER-Level-Rules wird ignoriert
**Symptom:** Eine `~/.claude/rules/`-Regel mit `paths:` taucht in `/memory` nicht auf und greift nie.
**Ursache:** `paths:`-Scoping funktioniert nur auf **Projekt-Ebene** (`<projekt>/.claude/rules/`), nicht user-weit.
**Versionen:** #21858 CLOSED **COMPLETED 2026-03-24** — auf 2.1.168 evtl. behoben; im Zweifel verifizieren.
**FIX (robust, versionsunabhaengig):** User-Level-Regeln IMMER **ohne** `paths:` lassen → sie laden dann unconditional (genau das gewuenschte Verhalten fuer Franks globale Regeln). Path-Scoping nur projektweit.
**Quelle:** #21858 · #22170

### 2.2 path-scoped Rules triggern nur bei READ, nicht bei WRITE
**Symptom:** Eine Regel fuer `src/api/**` greift, wenn Claude eine API-Datei liest, aber NICHT, wenn er eine neue anlegt.
**Versionen:** #23478 CLOSED **NOT_PLANNED** (per Design) — bleibt aktiv.
**FIX:** Wichtige Constraints zusaetzlich unconditional (ohne `paths:`) hinterlegen, oder per Hook erzwingen.
**Quelle:** #23478

### 2.3 `paths:` bricht still bei YAML-Quote-Syntax — Patterns quoten
**Symptom:** Doku-konformes `paths:` laedt nicht, keine Fehlermeldung.
**Ursache:** Glob-Muster, die mit `*` oder `{` beginnen, sind in YAML reservierte Indikatoren und MUESSEN gequotet werden (`"**/*.ts"`), sonst still kaputt. Teils funktioniert `globs:` zuverlaessiger als `paths:` (Doku-Inkonsistenz).
**Versionen:** #17204 CLOSED NOT_PLANNED (Doku) — Workaround bleibt.
**FIX:** Glob-Patterns IMMER quoten. Mit `InstructionsLoaded`-Hook / `/memory` verifizieren, dass die Regel laedt.
**Quelle:** #17204 · #13905

### 2.4 Subagents laden CLAUDE.md + `~/.claude/rules/` NICHT  ⭐ wichtig
**Symptom:** Ein Task-Subagent arbeitet ohne Projekt-Kontext; CLAUDE.md/Rules gelten dort nicht.
**Versionen:** #29423 **OPEN** — aktiver Bug.
**FIX (funktionserhaltend):** Regeln per **`subagent-context`-Hook** (SubagentStart) in jeden Subagent injizieren — nested `hookSpecificOutput.additionalContext`-Schema Pflicht (siehe `claude-hooks.md`). Genau der bestehende Mechanismus im Repo.
**Quelle:** #29423

### 2.5 Cross-Machine-Sync von Regeln (Repo-Spiegelung)
**Fakten/FIX:** `~/.claude/` liegt NICHT im Repo → eigene Regeln zusaetzlich nach `claude-code-setup/rules/` spiegeln (1:1) und committen, sonst hat die andere Plattform sie nicht. `.claude/rules/` unterstuetzt Symlinks (zirkulaere werden erkannt). `--add-dir` laedt CLAUDE.md NICHT, ausser `CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD=1`.
**Quelle:** docs/memory · Repo-Regel `git-workflow.md`/`cross-platform`

---

## 3. settings.json / settings.local.json & Permissions

### 3.1 Ein JSON-Syntaxfehler macht die GANZE Datei still ungueltig  ⭐ HAEUFIG / Nr.1
**Symptom:** Ein trailing comma / eine fehlende Klammer → alle Settings/Permissions/Hooks weg, keine Warnung (auch nicht mit `--debug`). Fallback auf Defaults oder gecachte Version.
**Versionen:** #2835 / #24823 — per Design / offen.
**FIX:** `"$schema": "https://json.schemastore.org/claude-code-settings.json"` setzen; nach JEDER Aenderung `python -c "import json;json.load(open(...))"` validieren; `/doctor` laufen lassen. JSON **nie** mit `sed`/`awk`/`echo >>` bearbeiten — Edit-Tool oder Python-`json`-Modul. Siehe `best-practices/claude-tooling/settings.md`.
**Quelle:** #2835 · #24823

### 3.2 UTF-8-BOM bricht den JSON-Parse (Windows)  ⭐ HAEUFIG (uns 2x getroffen)
**Symptom:** `/doctor` meldet settings.json als malformed, obwohl JSON korrekt aussieht; `Unrecognized token '﻿'`. BOM ist unsichtbar.
**Ursache:** PowerShell `Out-File`/`>`/`Set-Content` und Notepad schreiben UTF-8 **mit BOM**. Betrifft auch `.mcp.json`.
**Versionen:** #9906 CLOSED **NOT_PLANNED** — per Design, Workaround bleibt dauerhaft.
**FIX:** BOM-frei speichern. PowerShell: `[System.IO.File]::WriteAllText($p,$json,(New-Object System.Text.UTF8Encoding $false))`. Python schreiben: `encoding='utf-8'` (NICHT `utf-8-sig`); Python lesen: `encoding='utf-8-sig'` (BOM-tolerant). VS Code: "UTF-8" statt "UTF-8 with BOM".
**Eigener Vorfall (2026-06-15):** Nach einem `Edit` auf `settings.json` tauchte ein BOM auf — ein
PostToolUse-Hook (Formatter/Guard) fuegte es re-ein (exakter Einfueger nicht eindeutig isoliert;
`biome` als Einzeltest ausgeschlossen, schreibt BOM-frei). **Poka-Yoke (Stufe 2):**
`poka-yoke-json-validate.{ps1,sh}` strippt seitdem nach JEDEM Edit ein BOM aus Claude-Config-JSON
(`settings*.json`, `.mcp.json`, alles unter `.claude/`/`claude-code-setup/`) automatisch,
Root-Cause-unabhaengig. Beim eigenen Config-Edit: BOM per **Bash/Python** strippen (utf-8-sig lesen,
utf-8 ohne BOM schreiben) — NICHT per Edit/Write-Tool (triggert den Formatter erneut).
**Quelle:** #9906 · eigener Vorfall 2026-06-15

### 3.3 Falsches Permission-Pattern verwirft die GANZE Datei
**Symptom:** Ein einziges falsch formatiertes Pattern (`Bash(mkdir*)` statt `Bash(mkdir:*)`, oder unescapte Quotes in einem persistierten Befehl) → komplette settings(.local).json wird still rejected ("Files with errors are skipped entirely"). Claude Code persistiert eigene allow-Eintraege teils selbst fehlerhaft (#33650).
**Versionen:** #12468 · #10096 · #33650 — offen.
**FIX:** Pattern-Syntax exakt (siehe 3.6); via `/doctor` validieren; fehlerhaft persistierte Zeile manuell entfernen/escapen.
**Quelle:** #12468 · #33650

### 3.4 `.claude.json` Race-Korruption bei parallelen Sessions (Windows) — GEFIXT
**Symptom:** Mehrere gleichzeitige Sessions (Franks 4-5-Setup) → `.claude.json` mid-write truncated → `JSON Parse error: Unexpected EOF`, Config schrumpft auf Stub.
**Ursache:** Non-atomic write; Windows-`rename()` EPERM bei gehaltenem Handle.
**Versionen:** ✅ **gefixt ab v2.1.61** — auf 2.1.168 nicht mehr relevant.
**FIX (falls eigene Config-Writer):** atomar schreiben (temp + `os.replace`). Recovery: `~/.claude/backups/` (5 timestamped Backups automatisch).
**Quelle:** #28806

### 3.5 `bypassPermissions` schuetzt `.claude/`, `.git/` etc. trotzdem
**Symptom:** Trotz `defaultMode: bypassPermissions` + expliziter allow-Regeln kommen Prompts bei Writes nach `.git/`, `.claude/`, `.vscode/`.
**Ursache:** Ab **v2.1.78** triggern Writes in geschuetzte Verzeichnisse einen Prompt auch bei aktivem Bypass — Ausnahmen: `.claude/commands`, `.claude/agents`, `.claude/skills`.
**Versionen:** Verhalten ab v2.1.78 (bewusste Schutzmassnahme).
**FIX (funktionserhaltend):** Fuer noetige `.claude/`-Writes die erlaubten Unterordner nutzen oder PreToolUse-Hook. Kein "Fix" — gewollt.
**Quelle:** #38806 · #34923

### 3.6 `allow`-Liste ist KEINE Whitelist + Bash-`:*` nur am Ende
**Symptom:** Eine `allow`-Liste blockt nicht-gelistete Tools NICHT; unter `bypassPermissions` hat sie keinen Whitelist-Effekt und kann stoeren. `Bash(git diff)` matcht nur exakt; `Bash(git:* push)` matcht GAR nichts (Doppelpunkt mitten im Pattern = literal).
**Ursache:** Eval-Reihenfolge **deny > ask > allow**, first-match; allow erlaubt nur, sperrt nie. `:*` ist nur am Pattern-Ende ein Trailing-Wildcard.
**Versionen:** per Design.
**FIX:** Sperren nur via `deny`. Fast immer `Bash(cmd:*)` (mit Wortgrenze). Franks `session-guard`/`config-guard` entfernen die allow-Liste bei bypassPermissions zu Recht.
**Quelle:** docs/permissions · #18961 · #20254 · `best-practices/claude-tooling/settings.md`

### 3.7 `effortLevel`: `/model` ueberschreibt still + `max` persistiert nicht
**Symptom:** Der `/model`-Picker schreibt still den effortLevel als globalen Default; `effortLevel: "max"` ueberlebt keine Session; effortLevel beim Start teils ignoriert (faellt auf medium).
**Ursache:** `max` ist Modell-gated (nur Opus) + Session-only; UI ueberschreibt nicht-gelistete Werte.
**Versionen:** #49076 · #45453 — offen.
**FIX:** `low/medium/high/xhigh` ueber das `effortLevel`-Setting steuern. Effort NIEMALS ueber `CLAUDE_CODE_EFFORT_LEVEL`-Env setzen — die Env-Var blockiert dann `/effort`-Aenderungen (deckt sich mit Franks CLAUDE.md-Regel). Nach `/model` den effortLevel pruefen.
**Quelle:** #49076 · #45453 · #43322

### 3.8 Praezedenz & Live-Reload (Referenz)
**Fakten:** Managed > CLI-args > local (`.claude/settings.local.json`) > project (`.claude/settings.json`) > user (`~/.claude/settings.json`). **Ausnahme:** Permission-Regeln **mergen** ueber alle Scopes (concat + dedupe) statt zu ueberschreiben. `model` und `outputStyle` werden NICHT live reloaded (Neustart noetig); andere Settings werden gewatcht. Managed-Settings-Windows-Pfad ab v2.1.75: `C:\Program Files\ClaudeCode\managed-settings.json` (Legacy `C:\ProgramData\...` tot). `NO_COLOR`/`FORCE_COLOR` im `env`-Block wirken ab v2.1.143 nur fuer Subprozesse, nicht fuer Claudes eigene UI.
**Quelle:** docs/settings · docs/permissions

> **3-Dateien-Settings-Regel (Repo):** Aenderung an `~/.claude/settings.json` → alle drei Setup-Repo-Dateien aktualisieren (`settings-reference.json`, macOS-`settings.json`, `settings.local.json`-Vorlage). Secrets vorher redaktieren (siehe `git-workflow.md`).

---

## 4. Skills (`SKILL.md`)

### 4.1 `paths:`-Frontmatter macht den Skill komplett undiscoverable  ⭐
**Symptom:** Skill mit `paths:`-Feld erscheint nicht im `/`-Autocomplete, nicht in der Liste; Direktaufruf "Unknown skill".
**Versionen:** #49835 **OPEN** — aktiver Bug.
**FIX:** `paths:` aus dem Skill ENTFERNEN; Triggering ueber die `description` steuern. Path-scoped Verhalten ggf. ueber eine projektweite Rule loesen.
**Quelle:** #49835

### 4.2 `description` bricht das Frontmatter-Parsing (mehrzeilig / Literal-Block)
**Symptom:** Skill laedt nicht / wird nicht erkannt, obwohl YAML "valide" aussieht — oft nachdem ein Formatter (Prettier) die lange `description` umgebrochen hat; oder `description: |` gibt das literale `|` an Claude weiter.
**Ursache:** Der Frontmatter-Parser ist streng/eigen — mehrzeilige description ohne YAML-Operator bzw. Literal-Block wird falsch geparst.
**Versionen:** #11322 · #9817 · #12971 — offen.
**FIX:** `description` IMMER **einzeilig** in Anfuehrungszeichen, kein `|`/`>`. `.claude/skills/**/SKILL.md` in `.prettierignore` aufnehmen (bzw. `# prettier-ignore`).
**Quelle:** #11322 · #12971 · scottspence.com

### 4.3 `description` > ~250-Zeichen-Kernzweck wird nicht getriggert / Char-Budget
**Symptom:** Skill ist gelistet, feuert aber nicht fuer seinen Kernzweck; bei vielen Skills "kennt" Claude manche gar nicht.
**Ursache:** Trigger-relevante Begriffe nur in den ersten Zeichen wirksam; ab v2.0.70 Gesamt-Budget der Skill-/Command-Descriptions im System-Prompt (Default 15000 Zeichen) → Ueberlauf laesst Skills herausfallen.
**Versionen:** per Design (2.0.70+).
**FIX:** Trigger-Woerter + Kernzweck **front-loaden**, direktiv formulieren ("Nutze IMMER wenn …"). Bei vielen Skills `SLASH_COMMAND_TOOL_CHAR_BUDGET=30000` setzen ODER descriptions kuerzen. Siehe `best-practices/claude-tooling/skills.md`.
**Quelle:** agensi.io · blog.fsck.com · Medium (650-Trials)

### 4.4 Discovery nur bei Session-Start (+ exakter Dateiname)
**Symptom:** Frisch geschriebener Skill wird in laufender Session nicht erkannt; `skill.md`/`Skill.md` werden ignoriert; doppelt-genesteter Ordner (`skill/skill/SKILL.md`) wird nicht gefunden.
**Ursache:** Skill-Discovery laeuft beim Session-Start; Dateiname case-sensitiv **`SKILL.md`**.
**Versionen:** per Design. (Ab v2.1.157 werden `.claude/skills/` auto-geladen; ein SessionStart-Hook kann `reloadSkills:true` zurueckgeben — sonst neue Session.)
**FIX:** Neue Session starten bzw. `reloadSkills`. Exakt `SKILL.md`, eine Ordnerebene (`~/.claude/skills/<name>/SKILL.md`).
**Quelle:** docs/skills · agensi.io · blog.fsck.com

### 4.5 Praezedenz & Kollisionen (Referenz)
**Fakten:** Gleichnamig: **Enterprise > Personal (`~/.claude/skills`) > Project (`.claude/skills`)**. Plugin-Skills sind `plugin:skill` namespaced (kollidieren nie). Ein Skill **schlaegt** einen gleichnamigen Command. Ueberlappende Skills fuer dieselbe Aufgabe → falscher Skill triggert. `disallowedTools` im Frontmatter ab v2.1.152 moeglich.
**Quelle:** docs/skills

---

## 5. Slash-Commands & Custom-Subagents / Agents

### 5.1 Custom-Agents werden NICHT als `subagent_type` erkannt  ⭐
**Symptom:** "Agent type not found"; Agents aus `~/.claude/agents/*.md` werden vom Task-Tool ignoriert.
**Versionen:** #20931 CLOSED DUPLICATE — Bug besteht.
**FIX (funktionserhaltend):** Custom-Agent mit `subagent_type: "general-purpose"` starten und den Agent-Prompt manuell im `prompt`-Parameter uebergeben (genau wie CLAUDE.md es fuer `export`/`import` vorschreibt). NIE `subagent_type: "<custom-name>"`.
**Quelle:** #20931 · #24439

### 5.2 `model:`-Frontmatter wird ignoriert — Subagent erbt Parent/Override
**Symptom:** Pro-Agent-Modell via `model:`-Frontmatter wirkungslos.
**Ursache:** `CLAUDE_CODE_SUBAGENT_MODEL` (env) hat Vorrang vor `model:`-Param, Frontmatter und `/model` — wird bei jedem CLI-Spawn injiziert.
**Versionen:** #44385 CLOSED DUPLICATE — Bug besteht.
**FIX:** Modell global ueber `CLAUDE_CODE_SUBAGENT_MODEL` steuern (genau Franks `highest-model-everywhere`-Mechanismus: bewusst auf `opus[1m]`). Achtung Drittsysteme: Cowork setzt die Var hart auf Haiku.
**Quelle:** #44385 · #47488 · #55712

### 5.3 `tools:`-Whitelist ist additiv, keine harte Grenze
**Symptom:** Agent bekommt mehr Tools als gelistet; `--allowedTools` haengt an die Default-Safe-List an statt zu ersetzen.
**Versionen:** #62608 · #12392 — offen/per Design.
**FIX:** `tools:` taugt zum **Verkleinern des Start-Sockels** (Crash-Proofing), NICHT als Security-Grenze. Echte Restriktion via `disallowedTools`.
**Quelle:** #62608 · #57507

### 5.4 Subagent "Prompt is too long" / kein Auto-Compact
**Symptom:** Subagent crasht sofort (oft 0 Token), Parent friert ein.
**Ursache:** Subagent erbt ALLE MCP-Tool-Schemas (>200k Token moeglich) bzw. grosse Ergebnisse ueberlaufen; **kein Auto-Compact im Subagent**.
**Versionen:** aktiv (2.1.x).
**FIX:** `ENABLE_TOOL_SEARCH` (deferred MCP-Schemas), `tools:`-Whitelist, Ergebnisse per Datei (Pfad+Summary), Orchestrator-Resume bei Crash — die `subagent-crash-proofing`-Regel.
**Quelle:** #37793 · #50284 · `~/.claude/rules/subagent-crash-proofing.md`

### 5.5 Commands: Discovery, `$ARGUMENTS`, Namespacing
**Symptome/Fakten:**
- Commands aus `.claude/commands/` tauchen nicht im Autocomplete auf / "laufen" ohne Wirkung (Discovery-Bug, versionsabhaengig — auf aktuelle Version updaten). (#9926, #8831)
- `$ARGUMENTS` bricht bei **mehrzeiligem** Input (#28033); `$1`/`$2` nur in **manuell** aufgerufenen Commands, model-invoked Skills nur `$ARGUMENTS` (seit Skill/Command-Merge v2.1.3, #19355).
- `$ARGUMENTS` wird fuer `!`-Bash NICHT escaped → **nie** ungefiltert in `!`-Bash interpolieren (Injection, #16163).
- Plugin-Commands voll-qualifiziert aufrufen (`/plugin:command`); Subdirectory-Namespacing (`frontend/x.md` → `/frontend:x`) in 2.1 wiederhergestellt.
**FIX:** Argumente einzeilig oder `$1/$2`; Input vor `!`-Bash validieren; voll-qualifizierte Namen. Siehe `best-practices/claude-tooling/commands.md`.
**Quelle:** #28033 · #16163 · #19355 · #15882

---

## 6. Auto-Memory / `MEMORY.md`

### 6.1 `MEMORY.md` laedt nur ~200 Zeilen / 25 KB — stille Truncation  ⭐ BETRIFFT UNS
**Symptom:** Nur die ersten 200 Zeilen ODER 25 KB werden bei Session-Start geladen; Rest faellt lautlos weg. Session-Start-Warnung "MEMORY.md is 27.2KB (limit 24.4KB) — only part loaded".
**Ursache:** Per Design im Loader. Limit gilt NUR fuer `MEMORY.md` (CLAUDE.md laedt immer voll).
**Versionen:** per Design (Auto-Memory ab v2.1.59).
**FIX (funktionserhaltend):** Index unter 200 Zeilen / 25 KB halten; Detail in Topic-Dateien (werden on-demand gelesen → kein Funktionsverlust). Index-Zeilen kurz (< ~200 Zeichen). Genau die offene Aufgabe im aktuellen `proggs`-Memory.
**Quelle:** docs/memory

### 6.2 Speicherort / Eigenheiten (Referenz)
**Fakten:** `~/.claude/projects/<projekt>/memory/`, git-abgeleitet, **machine-local** (NICHT ueber Git/Cloud geteilt), alle Worktrees teilen ein Verzeichnis. Umlegbar via `autoMemoryDirectory`. Toggle: `/memory`, `autoMemoryEnabled:false`, `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`. `#`-Shortcut: war zeitweise defekt (#14868), **gefixt COMPLETED 2026-01-02** — funktioniert wieder; speichert in Auto-Memory, nicht zwingend CLAUDE.md.
**Quelle:** docs/memory · #14868

---

## 7. Plugins & Marketplaces

### 7.1 Externer Plugin-/MCP-Code als Prompt-Injection-Vektor  ⭐ Sicherheit
**Symptom:** Eingebettete Injections in Plugin-Commands/Marketplace koennen Claude zu Datei-Loeschung/Reverse-Shell verleiten; kompromittierte MCP-Server injizieren am Tool-Layer (unsichtbar im Chat).
**FIX:** Externen Code VOR Installation komplett lesen, Publisher/Stars/Commits pruefen, Scanner (Parry) laufen lassen, nur vertrauenswuerdige Quellen — exakt die CLAUDE.md-Regel "Sicherheit bei externem Code".
**Quelle:** promptarmor.com · truefoundry.com · `best-practices/claude-tooling/plugins.md`

### 7.2 Plugin-Hook-Skripte verlieren das Execute-Bit (+x)
**Symptom:** `Permission denied` fuer `.sh`-Hooks nach git-clone / Cloud-Sync / `claude plugin update`.
**Ursache:** `.sh` ohne +x committed bzw. Sync uebertraegt keine Unix-Permissions (→ 644).
**Versionen:** #20818 CLOSED DUPLICATE — besteht.
**FIX:** Im Repo `git update-index --chmod=+x hooks/*.sh`; lokal `find ~/.claude -name "*.sh" -exec chmod +x {} +` (plugin-health-check-Pattern).
**Quelle:** #20818

### 7.3 Plugin-`command`-Hooks fuer Pre/PostToolUse werden still verworfen
**Symptom:** In `hooks/hooks.json` eines Plugins registrierte **command**-Hooks fuer Pre/PostToolUse laden nicht; **prompt**-Hooks am selben Event laden.
**Versionen:** #34573 CLOSED NOT_PLANNED — per Design.
**FIX:** Solche Hooks in der user-`settings.json` definieren statt im Plugin. (Detail: `claude-hooks.md`.)
**Quelle:** #34573

### 7.4 Plugin-SessionStart-`additionalContext` wird nicht durchgereicht
**Symptom:** Plugin-SessionStart-Hook laeuft, aber `hookSpecificOutput.additionalContext` erreicht Claude nicht.
**Versionen:** #16538 — offen/teilweise.
**FIX:** Bei SessionStart plain stdout nutzen (surfaced) oder ueber UserPromptSubmit injizieren; nested Schema beachten. (Detail: `claude-hooks.md`.)
**Quelle:** #16538

### 7.5 `claude plugin update` zieht stale Marketplace-Clone
**Symptom:** meldet faelschlich "already at latest", installiert veraltete Version.
**Ursache:** macht `git fetch`, aber kein `git pull`/`merge` auf den lokalen Marketplace-Clone.
**Versionen:** #29071 — offen.
**FIX:** Marketplace neu hinzufuegen ODER Clone manuell `git pull`. Hinweis: deaktivierte Plugins fuehren ihre Hooks teils trotzdem aus (#39307).
**Quelle:** #29071 · #39307

---

## 8. Plattform-Fallen (Windows / macOS) bei Config-Dateien

### 8.1 CRLF bricht das Edit-Tool bei Config-/Skill-Dateien
**Symptom:** Edit schlaegt fehl ("File has been unexpectedly modified" / `old_string` matcht nicht), obwohl der Text identisch aussieht.
**Ursache:** Auf Windows geschriebene Dateien (CLAUDE.md, rules, SKILL.md) bekommen CRLF; das Edit-Tool normalisiert `\r\n` nicht.
**Versionen:** #13456 · #27718 — offen.
**FIX:** Config-/Skill-Dateien auf **LF** halten (`.gitattributes`: `* text=auto eol=lf`); bei Fehler `sed -i 's/\r$//' <datei>` bzw. Python `.replace('\r\n','\n')`.
**Quelle:** #13456 · #27718

### 8.2 cp1252 / Encoding zerstoert Umlaute in Config
**Symptom:** Umlaute in deutschen Config-/Regel-Texten werden zerstoert.
**Ursache:** Windows-Default cp1252 statt UTF-8 (besonders PowerShell 5.1 / `powershell.exe`).
**FIX:** UTF-8 (ohne BOM, s. 3.2). PowerShell: `pwsh` (nicht `powershell.exe`), `[Console]::OutputEncoding=[Text.Encoding]::UTF8`. (Detail: `python-windows.md` fuer Python-Encoding.)
**Quelle:** zenn.dev Windows-Troubleshoot

### 8.3 `.mcp.json`: relative Pfade / nackte Befehlsnamen
**Symptom:** MCP-Server startet nicht / Skill laedt nicht; `npx`/`bun`/`node` "not found" trotz Terminal-Verfuegbarkeit.
**Ursache:** Subprozesse starten mit minimalem PATH/undefiniertem CWD.
**FIX:** **Absolute Pfade** fuer `command`; auf Windows `npx` als `cmd /c npx ...` wrappen. Deckt sich mit `platform-and-paths.md` §6 (`.mcp.json` plattformspezifisch, nie automatisch vereinheitlichen). Detail: `mcp-server.md`.
**Quelle:** docs/settings · buildfastwithai.com

### 8.4 `~/.claude` unter Cloud-Sync (OneDrive/iCloud) → Lock/Permission
**Symptom:** Config-/Skill-Schreibfehler, gesperrte Dateien, sporadische ConfigChange-Events; +x bei `.sh` geht verloren; CRLF leakt Windows→mac.
**FIX:** `~/.claude` aus Cloud-Sync ausschliessen; nach Sync auf mac `chmod +x` + LF wiederherstellen.
**Quelle:** agensi.io · ediary.site

---

## 9. Fix-Status — was auf 2.1.168 bereits behoben ist

> Diese Eintraege waren frueher Bugs, sind aber in v2.1.168 GEFIXT (Changelog-/gh-belegt).
> Nur fuer aeltere Versionen relevant — auf 2.1.168 NICHT mehr als aktive Bugs behandeln.

| Frueherer Bug | Gefixt ab / Status | Abschnitt |
|---------------|--------------------|-----------|
| `.claude.json`-Korruption bei parallelen Sessions (Win) | **v2.1.61** | 3.4 |
| `#`-Memory-Shortcut speichert nicht | **#14868 COMPLETED 2026-01-02** | 6.2 |
| Windows: CLAUDE.md doppelt geladen (Drive-Letter-Case) | **#25756 COMPLETED 2026-02-16** | (war 1.x) |
| `paths:` in user-level Rules ignoriert | **#21858 COMPLETED 2026-03-24** (im Zweifel pruefen) | 2.1 |
| Plugin-Skills nicht im `/`-Menue (Regression v2.1.110) | **#48963 COMPLETED 2026-04-16** | 4.x |
| Subagents fanden Project-/User-/Plugin-Skills nicht | **v2.1.149** | 5.x |
| `/effort` einer Session aenderte Effort ANDERER Sessions | **v2.1.133** | 3.7 |
| `.claude/skills/` werden auto-geladen (kein Marketplace noetig) | **v2.1.157** | 4.4 |
| Windows-Permission-Rules (Backslash/Case) matchten nie; Deny auf `$HOME` | **v2.1.162** | 3.6 |
| Managed-settings: 1 ungueltiger Eintrag deaktivierte still ALLE Policies | **v2.1.166** | 3.8 |

### Noch NICHT gefixt (Workaround bleibt aktiv)
- **OPEN:** Skill mit `paths:` undiscoverable (#49835, 4.1) · Subagents laden CLAUDE.md/Rules nicht (#29423, 2.4).
- **NOT_PLANNED / per Design:** UTF-8-BOM (#9906, 3.2) · `@~`-Import (#8765, 1.4) · nested CLAUDE.md lazy (#24987, 1.5) · path-rules nur Read (#23478, 2.2) · `paths:`-Doku/globs (#17204, 2.3) · Plugin-command-Hooks dropped (#34573, 7.3) · JSON-Syntax/Permission-Pattern killt Config still (#2835/#12468, 3.1/3.3).
- **DUPLICATE (Bug besteht):** Custom-Agent `subagent_type` (#20931, 5.1) · `model:`-Frontmatter (#44385, 5.2) · Plugin +x (#20818, 7.2).

**Ehrlichkeits-Hinweis:** Issue-Status wurde am 2026-06-07 per `gh issue view` hart geprueft (OPEN/CLOSED + stateReason). `COMPLETED` = per Code-Fix geschlossen; `NOT_PLANNED`/`DUPLICATE` = der zugrundeliegende Effekt besteht meist weiter (Workaround behalten). Changelog-Zeilen (vX.Y.Z) stammen aus den offiziellen Release-Notes; nicht jede einzelne wurde gegen den Quellcode gegengeprueft.

---

## 10. Kopplung zur Best-Practices-Seite (zwei Seiten einer Medaille)

**Dediziertes Gegenstueck (seit 2026-06-07):** `best-practices/claude-tooling/claude-config.md`
— spiegelgleich abgelegt, damit der `bug-almanac-guard` nach dem Lesen DIESES Almanachs automatisch
auch die Best-Practices-Seite erzwingt (erst Almanach, dann Best Practices). Dort steht "wie macht man
es von vornherein richtig" inkl. **Entscheidungsbaum** (CLAUDE.md vs. rule vs. Hook vs. Skill vs.
Output-Style vs. settings), Verbindlichkeits-Spektrum, Befolgungs-Techniken, Defense-in-Depth.
Ergaenzend komponentenweise: `best-practices/claude-tooling/skills.md`, `best-practices/claude-tooling/agents.md`, `best-practices/claude-tooling/commands.md`, `best-practices/claude-tooling/settings.md`,
`best-practices/claude-tooling/kontext.md`, `best-practices/claude-tooling/token-effizienz.md`, `best-practices/claude-tooling/arbeitsweise.md`.

**🔗 Bezugs-Tabelle (Bug-Abschnitt ↔ Best-Practice-Abschnitt):**

| Bug-Abschnitt (hier) | Best-Practice-Abschnitt (best-practices-claude-config.md) |
|----------------------|----------------------------------------------------------|
| 1. CLAUDE.md | 0. Entscheidungsbaum · 2. Befolgung · 3. Token/Zwei-Schichten · 5. Robustheit |
| 2. Regel-Dateien / path-scoped | 3. Token/Zwei-Schichten · 5. Robustheit |
| 3. settings.json / Permissions | 1. Verbindlichkeits-Spektrum · 6. Defense-in-Depth |
| 4. Skills | 0. Entscheidungsbaum · 7. Neueste 2.1.x-Features |
| 5. Commands & Agents | 0. Entscheidungsbaum · 5. Robustheit (Subagent-Vererbung) |
| 6. Auto-Memory / MEMORY.md | 3. Token/Zwei-Schichten |
| 7. Plugins | 6. Defense-in-Depth (externer Code) |
| 8. Plattform-Fallen | 3. (BOM/JSON) |
| 9. Fix-Status | 7. Neueste 2.1.x-Features |

---

## Pflicht-Checkliste vor Konfigurations-Arbeit

- [ ] Diese Datei + Stand-Datum gegen `claude --version` abgeglichen?
- [ ] **CLAUDE.md:** bleibt sie < 200 Zeilen? Volltext in Rule/Skill ausgelagert statt inline? Geschuetzte ACE-Zonen unberuehrt?
- [ ] **Regel-Datei:** ohne `paths:` (user-level)? Nach `claude-code-setup/rules/` gespiegelt?
- [ ] **settings.json:** nach Aenderung JSON validiert (`python -m json.tool`) + BOM-frei? 3-Dateien-Regel beachtet? Keine allow-Whitelist-Annahme?
- [ ] **Skill:** `description` einzeilig + Trigger front-loaded? Kein `paths:`? Exakt `SKILL.md`?
- [ ] **Agent:** mit `general-purpose` + Prompt gestartet (nicht Custom-`subagent_type`)? Modell via `CLAUDE_CODE_SUBAGENT_MODEL`?
- [ ] **MEMORY.md:** Index < 200 Zeilen / 25 KB, Zeilen kurz, Detail ausgelagert?
- [ ] **Plugin:** externer Code vor Installation gelesen/gescannt? `.sh` mit +x?
- [ ] **Plattform:** Config-/Skill-Dateien LF + UTF-8 (ohne BOM)? `.mcp.json` mit absoluten Pfaden?
- [ ] Aenderung committed + gepusht (+ Cross-Platform gespiegelt)?


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [orchestrator-agent](../agents/orchestrator-agent.md)
- [cowork](cowork.md)
