# Claude-Code Konfiguration & Regeln-Integration — Best Practices (Stand 2026-07-02, Claude Code 2.1.198)

> **Wie man Regeln / eigene Vorstellungen / Verhalten optimal ins Claude-Code-System integriert.**
> Die positive Gegenseite ("wie macht man es von vornherein richtig") zum Bug-Almanach
> `~/proggs/bugs/claude-tooling/claude-config.md` ("was schiefgeht"). Spiegelgleich abgelegt,
> damit der `bug-almanac-guard` nach dem Almanach-Lesen automatisch hierher findet.
>
> Quellen-Rangordnung: offiziell (code.claude.com/docs, anthropic.com/engineering) = Grundwahrheit;
> extern (Blogs/Studien) = klar gelabelte Ergaenzung. Stand: Claude Code 2.1.198 (live ermittelt).
> Abgrenzung: Hooks im Detail → `01-hooks/`; MCP → `05-mcp/` + `best-practices-mcp-server.md`;
> Python → `best-practices-python-windows.md`.

> **Update 10.08.2026 (Claude-5-Generation):** §2 korrigiert — "Emphasis (YOU MUST)" und
> "3-5 Beispiele beilegen" sind fuer Opus 5 / Fable 5 **ueberholt** (Overtriggering bzw. Einengung).
> Ersatz: Begruendung statt Emphase, Schnittstellen-Design statt Beispiel-Listen. Modellspezifische
> Leitplanken vollstaendig in `modell-opus5.md`.

> **Update 2026-07-02:** Re-Recherche bestaetigt den bestehenden Entscheidungsbaum. Neu zu beachten: Symlink-Zielpfade laden bedingte Rules wieder korrekt, Hook-Matcher sind bei Bindestrich-Identifiern exact-match, und neue Env-Flags duerfen nur gezielt zur Diagnose/Policy genutzt werden.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Ich will X durchsetzen | Entscheidungsbaum: muss-immer → Hook/`deny`, sonst CLAUDE.md/Rule | §0 |
| 2 | Sicherheits-/Format-Garantie | NIE nur CLAUDE.md (advisory) — Hook oder `permissions.deny` | §1 |
| 3 | Regel soll besser befolgt werden | Begruendung + praezise Formulierung; KEIN "YOU MUST", keine Beispiel-Listen (Claude-5: Overtriggering) | §2 |
| 4 | CLAUDE.md schlank halten | < 200 Zeilen, Volltext in `~/.claude/rules/`; `@import` spart nichts | §3 |
| 5 | Tokens echt sparen | path-scoped Rule (`paths:` quoten) oder Skill (on-demand) | §3 |
| 6 | Rolle/Ton/Format dauerhaft | Output-Style (nicht CLAUDE.md), Projekt-Wissen bleibt in CLAUDE.md | §4 |
| 7 | Regel soll /compact ueberleben | Root-CLAUDE.md + unscoped Rules; NIE path-scopen/nesten | §5 |
| 8 | Subagent braucht die Regel | `SubagentStart`-Hook `additionalContext` (garantiert da) | §5 |
| 9 | Eine Vorstellung hart absichern | Defense-in-Depth: Rule + Skill + Hook + Memory | §6 |
| 10 | Regel wird wiederholt ignoriert | In einen Hook umwandeln statt CLAUDE.md verlaengern | §6 |
| 11 | Aktuelle 2.1.x-Features nutzen | Lean System Prompt, `reloadSkills`, `skillOverrides` | §7 |

---

## 0. Der Entscheidungsbaum — welches Werkzeug fuer welche Absicht (HERZSTUECK)

Offizielle Anthropic-Guidance (features-overview, "Build your setup over time"). Das ist die
Kernfrage "ich will X durchsetzen — wohin damit?":

| Auslöser / Absicht | Werkzeug | Verbindlichkeit |
|--------------------|----------|-----------------|
| Claude macht eine Konvention/Befehl **2x** falsch | **CLAUDE.md** | advisory |
| Themen-/pfadspezifische Regel, CLAUDE.md soll schlank bleiben | **`.claude/rules/`** (projekt) bzw. `~/.claude/rules/` (user) | advisory |
| Derselbe Start-Prompt / dasselbe Playbook zum **3.** Mal | **Skill** | advisory, on-demand |
| Daten aus einem System, das Claude nicht sieht | **MCP-Server** | — |
| Side-Task flutet die Konversation mit Output | **Subagent** | isolierter Kontext |
| Etwas soll **JEDES MAL ohne Nachfrage** passieren | **Hook** | **deterministisch (100%)** |
| Harte Tool-/Pfad-Sperre, vom Client erzwungen | **`settings.json` `permissions.deny`** | **hart erzwungen** |
| Durchgaengige Rolle/Ton/Format ueber Sessions | **Output-Style** | System-Prompt-Ebene |
| Einmalige System-Prompt-Ergaenzung (Skript/`-p`) | **`--append-system-prompt`** | System-Prompt-Ebene |
| Zweites Repo braucht dasselbe Setup | **Plugin** | — |

**Offizielle Faustformel:** *"Use settings for technical enforcement and CLAUDE.md for behavioral guidance."*
Always-wahre Regel → CLAUDE.md; manchmal-relevantes Verfahren → Skill; muss-immer-halten → Hook/`deny`.
Quelle: [features-overview](https://code.claude.com/docs/en/features-overview), [memory](https://code.claude.com/docs/en/memory) · **offiziell**

### Pro Werkzeug: Eignung · Verbindlichkeit · Token · Persistenz

| Werkzeug | Wofuer | Verbindlichkeit | Token-Kosten | Persistenz |
|----------|--------|-----------------|--------------|------------|
| **CLAUDE.md** | Fakten, die JEDE Session braucht | advisory (~70-90%) | **voll, jeder Request** | jede Session, ueberlebt /compact |
| **`.claude/rules/` (mit `paths:`)** | pfadspezifische Regeln | advisory | nur bei Datei-Match (spart Kontext) | bei Match (projekt-Ebene!) |
| **Skill** | Mehrschritt-Verfahren, Referenz | advisory | **~0 bis Aufruf** (nur Beschreibung) | on-demand |
| **Hook** | "zero exceptions"-Aktion | **deterministisch** | **0** (extern; nur Output zaehlt) | auf Event |
| **Subagent** | viele Dateien lesen, Spezialfokus | eigene Allowlist | **isoliert** vom Hauptkontext | pro Aufruf (opt. `memory:`) |
| **Output-Style** | Rolle/Ton/Format | System-Prompt | im System-Prompt | sessionuebergreifend (Datei) |
| **settings `permissions.deny`** | harte Tool-/Pfad-Sperre | **hart, client-seitig** | 0 | dauerhaft |

---

## 1. Verbindlichkeits-Spektrum (das zentrale Konzept)

- **Offiziell:** *"Unlike CLAUDE.md instructions which are advisory, hooks are deterministic and guarantee the action happens."* CLAUDE.md/Regeln werden als User-Message NACH dem System-Prompt injiziert — **keine Erzwingung**. Quelle: [best-practices](https://code.claude.com/docs/en/best-practices), [memory](https://code.claude.com/docs/en/memory) · **offiziell**
- **Skala:** advisory (CLAUDE.md/rules ~70-90% Befolgung, "can skip under context pressure") → deterministisch (Hook 100%, "execute at system level, outside the LLM's reasoning chain"). Zahlen: [dotzlaw.com](https://www.dotzlaw.com/insights/claude-hooks/) · **extern**
- **Regel:** Sicherheits-/Destruktiv-/Format-Garantien NIE nur in CLAUDE.md. Was zu 100% gelten muss → Hook (`PreToolUse deny`) oder `settings.permissions.deny`. Stil-/Workflow-Hinweise → CLAUDE.md genuegt. · **offiziell**
- **Instruction-Budget:** Frontier-Modelle befolgen ~150-200 Instruktionen zuverlaessig; der CC-System-Prompt belegt schon ~50 → nur ~100-150 frei fuer CLAUDE.md + Regeln + User-Messages. Mehr → **gleichmaessige** Degradation ueber ALLE Regeln (nicht selektiv). Quelle: [humanlayer.dev](https://www.humanlayer.dev/blog/writing-a-good-claude-md) · **extern**

---

## 2. Befolgungs-Techniken (hoehere Adherence) — alle offiziell

- ⚠️ **Emphasis — fuer die Claude-5-Generation ueberholt (Stand 10.08.2026).** Die alte Empfehlung
  lautete: *"adding emphasis (e.g. 'IMPORTANT' or 'YOU MUST') to improve adherence."* Sie stammt aus
  der 4.x-Zeit. Ab Opus 4.5/4.6 und erst recht bei Opus 5 fuehrt aggressive Sprache zu
  **Overtriggering**; Anthropic woertlich: *"Where you might have said 'CRITICAL: You MUST use this
  tool when...', you can use more normal prompting like 'Use this tool when...'."*
  ([prompting-best-practices](https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/claude-prompting-best-practices))
  → Details und was stattdessen gilt: `modell-opus5.md` §7.
- **Begruendung mitgeben — der Ersatz fuer Emphase.** Constraint mit "warum" erhoeht Befolgung
  (z.B. "never use ellipses because this will be read by text-to-speech"); Anthropic: *"Claude is
  smart enough to generalize from the explanation."* Das wirkt bei Claude 5 besser als jede
  Grossschreibung.
- **Spezifisch statt vage:** *"The more precise your instructions, the fewer corrections you'll need."*
  Aber: Opus 5 nimmt Anweisungen **woertlicher** als Vorgaenger — eine zu eng gefasste Einschraenkung
  ("nur High-Severity melden") wird exakt befolgt und schadet dann. Praezise heisst nicht eng.
- **"Right altitude":** spezifisch genug zum Steuern, flexibel genug als Heuristik — beide Extreme (zu brittle / zu vage) scheitern. ([effective-context-engineering](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents))
- ⚠️ **Beispiele — ebenfalls ueberholt.** Die alte Empfehlung "3-5 konkrete Beispiele beilegen" gilt
  fuer die Claude-5-Generation nicht mehr; das Claude-Code-Team: *"Adding examples to a system prompt
  is no longer best practice for models like Fable 5 or even Opus 4.8"* — Beispiele schraenken die
  neueren Modelle eher ein. Stattdessen: Schnittstellen/Werkzeuge selbsterklaerend gestalten.
- **Ambiguitaet eliminieren** (fragt Claude nach etwas, das in CLAUDE.md steht → Formulierung ist mehrdeutig); **Colleague-Test** (verwirrt es einen Menschen ohne Kontext, verwirrt es auch Claude).
- Quelle: [best-practices](https://code.claude.com/docs/en/best-practices), Anthropic Prompt-Guide (be-clear-and-direct) · **offiziell**

---

## 3. Token-/Zwei-Schichten-Architektur (Regeln kontext-schonend ablegen)

- **CLAUDE.md < 200 Zeilen.** Wird IMMER voll geladen (egal wie lang) → laenger = mehr Token + schlechtere Adherence. Faustregel pro Zeile: *"Would removing this cause Claude to make mistakes? If not, cut it."* · **offiziell** ([memory](https://code.claude.com/docs/en/memory))
- **Zwei-Schichten-Architektur:** kompakter Verweis in CLAUDE.md + Volltext ausgelagert in `~/.claude/rules/<thema>.md` (fokussierte Datei) bzw. on-demand in einem Skill. Genau das Muster der 3 Hauptdirektiven + Observability-First.
- **`@import` spart KEINE Tokens:** *"imports help organization but do not reduce context, since imported files load at launch"* (max. 4 Hops). Echte Ersparnis nur ueber path-scoped rules oder Skills. · **offiziell**
- **path-scoped rules:** NUR mit `paths:`-Frontmatter laden sie lazy (bei Datei-Match); OHNE `paths:` jede Session (wie CLAUDE.md). Glob-Muster **quoten** (`"**/*.ts"`). Wichtig: `paths:` ist eine **Projekt-Ebenen**-Funktion. · **offiziell**
- **Skills on-demand:** Body laedt erst bei Nutzung — *"long reference material costs almost nothing until you need it"*. `disable-model-invocation: true` fuer manuelle Workflows mit Seiteneffekten. · **offiziell**
- **HTML-Block-Kommentare kosten 0 Token** (werden vor Injection gestrippt, beim Read sichtbar) → ideal fuer Maintainer-Notizen + Schutz-Marker (ACE-Zonen). · **offiziell**
- **MEMORY.md (Auto-Memory):** nur erste 200 Zeilen / 25 KB werden geladen — Index kurz, Detail in Topic-Dateien (on-demand). · **offiziell**

---

## 4. Output-Styles & System-Prompt-Ebene

**Vier Ebenen** (WO sie ansetzen):

| Mechanismus | Setzt an | Coding-Defaults erhalten? | Persistenz |
|-------------|----------|---------------------------|------------|
| **Output-Style** | System-Prompt (Ende) | nur mit `keep-coding-instructions: true` | Datei, sessionuebergreifend |
| **CLAUDE.md** | User-Message nach System-Prompt | ja (aendert SP nicht) | pro Projekt, via Git |
| **`--append-system-prompt`** | haengt an System-Prompt an | ja | nur diese Invocation |
| **`--system-prompt`** (replace) | ersetzt System-Prompt komplett | **nein** (Tools/Safety selbst liefern) | nur diese Invocation |

- **Output-Style** nur fuer Rolle/Ton/Format, nie fuer Projekt-Wissen. Eingebaut: `Default`, `Proactive`, `Explanatory`, `Learning`. Speicherort: `~/.claude/output-styles/` (user) oder `.claude/output-styles/` (projekt).
- **`--append-system-prompt`** fuer per-Invocation-Regeln in Skripten (layert auf das `claude_code`-Preset, nichts geht verloren) → niedrigstes Risiko.
- **Entscheidung:** andere Rolle/Format jede Runde → Output-Style · Projekt-Konventionen → CLAUDE.md · einmalige Ergaenzung → `--append-system-prompt` · voellig andere Identitaet → `--system-prompt`.
- Quelle: [output-styles](https://code.claude.com/docs/en/output-styles), [modifying-system-prompts](https://code.claude.com/docs/en/agent-sdk/modifying-system-prompts) · **offiziell**

---

## 5. Robustheit: Compaction · Subagents · Cross-Machine

**Compaction ueberleben (offiziell, context-window#what-survives-compaction):**
- Re-injected von Disk: **Project-root CLAUDE.md + unscoped rules** und **Auto-Memory**. → Kritische Regeln dorthin.
- VERLOREN: Rules mit `paths:` (bis Trigger-Datei wieder gelesen), nested CLAUDE.md, reine Chat-Anweisungen. → kritische Regeln NIE path-scopen/nesten.
- `## Compact Instructions`-Sektion in CLAUDE.md steuert, was die Zusammenfassung behaelt.
- Absolute Persistenz: System-Prompt-Ebene (`--append-system-prompt`/Output-Style) — nicht Teil der Message-History. `PreCompact`-Hook kann Compaction sogar blocken.

**Subagent-Vererbung (WICHTIG — offizielle Korrektur):**
- Offiziell: *"Explore and Plan skip your CLAUDE.md files. Every other built-in and custom subagent loads both [CLAUDE.md + rules]."* → normale Subagents laden CLAUDE.md VOLL. ([sub-agents](https://code.claude.com/docs/en/sub-agents)) · **offiziell**
- ABER ein OPEN-Issue (#29423) berichtet, dass `.claude/rules` in Task-Subagents NICHT laden. Status: umstritten/zu verifizieren.
- **Sichere Praxis (deckt beide Faelle ab):** Regeln explizit per **`SubagentStart`-Hook** `hookSpecificOutput.additionalContext` injizieren (Franks `subagent-context`-Hook). Unabhaengig davon, ob Subagents laden — die Regel ist garantiert da. Das ist die robuste, verlustfreie Loesung.

**Cross-Machine / Verifizieren:**
- `~/.claude/` liegt nicht im Repo → eigene Regeln zusaetzlich nach `claude-code-setup/` spiegeln (committen). `.claude/rules/` unterstuetzt Symlinks (zirkulaere werden erkannt). `@~/.claude/datei.md`-Import funktioniert auch ueber Worktrees.
- `/memory` zeigt alle geladenen Instruktions-Dateien; `InstructionsLoaded`-Hook (audit-only) loggt, welche Regel wann/warum laedt.
- **Geschuetzte Regeln:** managed-policy-CLAUDE.md ist unausschliessbar (Org-Schutz); HTML-Kommentar-Marker fuer Schutzhinweise (0 Token).
- Quelle: [context-window](https://code.claude.com/docs/en/context-window), [memory](https://code.claude.com/docs/en/memory), [hooks](https://code.claude.com/docs/en/hooks) · **offiziell** (+ #29423 extern/umstritten)

---

## 6. Defense-in-Depth: eine Vorstellung mehrschichtig absichern

- **`PreToolUse permissionDecision:"deny"` ueberschreibt sogar `bypassPermissions`** — *"a hard security boundary … users cannot bypass by changing their permission mode."* Die staerkste Schicht. · **offiziell**
- **Mehrere Hooks am selben Event: restriktivste gewinnt** (deny > ask > allow > defer); ein erlaubender Hook hebt einen deny NICHT auf → sicheres Stapeln. · **offiziell**
- **Schichten-Muster fuer EINE Vorstellung:** Regel (WARUM/Guideline) + Skill (Checkliste) + Hook (Erzwingung) + Memory (Erinnerung). Genau das Muster der 3 Hauptdirektiven. Community-Empfehlung: mit `action: warn` starten, dann zu `block` eskalieren.
- **SessionStart-Hook = Self-Healing** (Config bei jedem Start in Soll-Zustand bringen — Franks `session-guard`). **PostToolUse** = reaktive Schicht. **`additionalContext`** = sanfte Erinnerungs-Schicht ohne Block.
- **Offizielle Empfehlung:** *Claude schreibt Hooks selbst* ("Write a hook that blocks writes to the migrations folder"). **Anti-Pattern:** eine wiederholt ignorierte CLAUDE.md-Regel immer weiter verlaengern statt sie **in einen Hook umzuwandeln**.
- **Wann lohnt der Mehraufwand:** nur wenn (a) deterministisch/zero-exceptions noetig ODER (b) eine Regel wiederholt ignoriert wird. Sonst Over-Engineering vermeiden — nicht jede Vorstellung braucht 4 Schichten.
- Quelle: [hooks](https://code.claude.com/docs/en/hooks), [hooks-guide](https://code.claude.com/docs/en/hooks-guide), [best-practices](https://code.claude.com/docs/en/best-practices) · **offiziell** (+ paddo.dev **extern**)

---

## 7. Neueste 2.1.x-Features fuers Regeln-Integrieren

- **Lean System Prompt** (Default ab 2.1.154 fuer Opus 4.8): weniger Boilerplate-Sockel → eigene Regeln **wiegen mehr**, muessen aber praeziser sein. · **offiziell**
- **`disallowed-tools`** im Skill/Command-Frontmatter (2.1.152): Tools sperren, solange der Skill aktiv ist (endet bei naechster User-Nachricht — keine dauerhafte Sperre, dafuer `permissions.deny`). · **offiziell**
- **`skillOverrides`** (`off`/`user-invocable-only`/`name-only`, ab 2.1.147): Skill-Sichtbarkeit steuern; `name-only` haelt viele Skills ohne Kontext-Flut (Context-Rot-Schutz). · **offiziell**
- **`reloadSkills`** (SessionStart-Hook) + **`/reload-skills`** (2.1.152): neue Regel-als-Skill sofort aktiv, kein Neustart. · **offiziell**
- **`Stop`/`SubagentStop` koennen `additionalContext` zurueckgeben** (2.1.163): Regeln am Turn-Ende nachschieben (z.B. "Boxen ausgegeben?") ohne `decision:block`. · **offiziell**
- **Workflow-Keyword-Trigger abschaltbar** (`/config`, 2.1.158): verhindert versehentliches Workflow-Triggern durch das Wort "workflow". · **offiziell**
- **Auto Mode** (2.1.152, ohne Opt-in): KI-Permission-Modus — **NICHT mit `bypassPermissions` kombinieren** (wuerde wieder Entscheidungen treffen). Bei bypassPermissions bleiben. · **offiziell/abgeleitet**

---

## 8. 🔗 Kopplung zum Bug-Almanach (Bezugs-Tabelle Best-Practice ↔ Bug)

Gegenstueck: `~/proggs/bugs/claude-tooling/claude-config.md`. Jeder BP-Abschnitt hier adressiert
die entsprechende Bug-Klasse dort:

| Best-Practice-Abschnitt (hier) | Bug-Abschnitt (claude-config.md) |
|--------------------------------|----------------------------------|
| 0. Entscheidungsbaum | 1.1 (CLAUDE.md nicht erzwungen), 5.x (Commands/Agents) |
| 1. Verbindlichkeits-Spektrum | 1.1 (advisory), 3.6 (allow ≠ Whitelist) |
| 2. Befolgungs-Techniken | 1.1, 1.2 (Context-Rot) |
| 3. Token-/Zwei-Schichten | 1.2, 1.3 (@import), 2.x (rules), 6.1 (MEMORY.md) |
| 4. Output-Styles & System-Prompt | (neu — Almanach hat nur Settings/Skills) |
| 5. Robustheit (Compact/Subagent/Sync) | 1.5 (nested lazy), 1.6 (Compact), 2.4 (Subagents/#29423), 2.5 (Sync) |
| 6. Defense-in-Depth | 1.1, 3.1/3.3 (JSON/Permission killt Config), 5.1/5.2 (Agent/Modell) |
| 7. Neueste 2.1.x-Features | 4.x (Skills), 9 (Fix-Status) |
| 3. (BOM/JSON) | 3.1, 3.2 (settings-BOM/Syntax) |

---

## Quellen (gesamt)
- [best-practices](https://code.claude.com/docs/en/best-practices) · [memory](https://code.claude.com/docs/en/memory) · [features-overview](https://code.claude.com/docs/en/features-overview) · [hooks](https://code.claude.com/docs/en/hooks) · [hooks-guide](https://code.claude.com/docs/en/hooks-guide) · [sub-agents](https://code.claude.com/docs/en/sub-agents) · [skills](https://code.claude.com/docs/en/skills) · [output-styles](https://code.claude.com/docs/en/output-styles) · [modifying-system-prompts](https://code.claude.com/docs/en/agent-sdk/modifying-system-prompts) · [context-window](https://code.claude.com/docs/en/context-window) · [effective-context-engineering](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents) — alle **offiziell**
- extern: humanlayer.dev (Instruction-Budget), dotzlaw.com (Compliance-Zahlen), paddo.dev (Hook-Guardrails), dev.classmethod.jp (Changelog 2.1.154)
