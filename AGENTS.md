# AGENTS.md — Agent-Optimized Context (Claude Code, OpenCode & jede andere CLI)

> This file is for AGENTS (coder, tester, reviewer, optimizer), not humans.
> It provides the minimum context needed for efficient autonomous work.
> Kurz halten: OpenCode befolgt AGENTS.md < ~150 Zeilen deutlich zuverlaessiger;
> ab ~200-400 Zeilen degradiert die Befolgung (best-practices/opencode/agents-md-memory.md).

## ⚡ Globale Kern-Regeln + Arbeitsregeln aus dem Gehirn

Es gelten die globalen OpenCode-Kernregeln aus `~/.config/opencode/AGENTS.md` (Sprache, Multi-Task,
Git, Secrets, Komprimierung, Arbeitsmodi und Direktive 3). Dort steht auch die Pflicht-Startaufgabe:
`Programmierung/Rules` vollständig per `get_category_item` von 1 bis N einzeln laden und danach
„N Regeln aus dem zweiten Gehirn eingelesen.“ bestätigen. Second-Brain-Einträge immer einzeln laden,
nie eine große Kategorie per `get_by_category`. Diese Datei ergänzt nur das proggs-Spezifische.

## Repository Structure

```
~/proggs/                          # Monorepo: Pepsi1978/proggs
  BestJournalAndroid/              # Kotlin/Compose journal app (Google Play)
  BestJournalFrank/                # Kotlin/Compose journal app (clone, no Firebase, no Premium)
  claude-code-setup/               # Claude Code config sync (hooks, rules, agents, settings)
  ClaudeCodexVoiceOverlay-macOS/   # Swift voice overlay
  ClaudeVoiceOverlay-Windows/      # C#/WPF voice overlay
  QuizVerse/                       # Kotlin quiz app
  mcp-code-search/                 # Local semantic search server
  tools/                           # Shared utilities
```

## Build Commands

| Project | Build | Test | Lint |
|---------|-------|------|------|
| BestJournalAndroid | `cd BestJournalAndroid && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| BestJournalFrank | `cd BestJournalFrank && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| C#/WPF | `dotnet build -c Release` | `dotnet test` | `dotnet format analyzers` |

## Files Agents Must NEVER Modify

- `~/.claude/rules/superintelligence.md` — Directive #1 (protected)
- `~/.claude/rules/self-observation.md` — Directive #2 (protected)
- `~/.claude/rules/resilient-bugfixing.md` — Directive #3 (protected)
- `~/.claude/rules/bypass-permissions-permanent.md` — Security (protected)
- `~/.claude/settings.json` permissions section — bypassPermissions must stay
- `.mcp.json` — Platform-specific, never auto-modify
- Any file in `~/Codex/` — Forbidden directory

## File Ownership Rules

- Two agents must NEVER edit the same file simultaneously
- Each agent gets assigned files in its prompt — stay within those files
- If you need to read a file another agent owns: READ only, never WRITE

## Android-Specific Conventions (BestJournal apps)

- Package: `com.bestjournal.app` (Android) / `com.entropyjournal` (Frank)
- Architecture: MVVM + Hilt DI + Room DB + Coroutines/Flow
- UI: Jetpack Compose, Material3, Dark Mode with 3 profiles
- Frank app: NO Firebase, NO Premium features, NO analytics
- Both apps must maintain feature parity (see feedback_frank_porting_pattern.md)
- Settings buttons: `wrapContentSize` + `Row(Center)`, never `fillMaxWidth`
- DB migrations: Always export schema JSON to `app/schemas/`
- After ADB install: Always auto-launch the app

## Commit Format

**Commit AND push automatically after EVERY finished task — immediately, of your own accord, WITHOUT
waiting for an instruction from Frank. This is the default, not optional.** Even a single small change
triggers commit+push. For app tasks: commit+push BEFORE build/install/deploy. Full rule: global
AGENTS.md §3 + brain `Programmierung/Rules`.
`#NNN - Description` (English). Number auto-incremented from last commit.

## Quality Requirements

- Quality Gates follow global AGENTS.md §6: Schnellmodus no pass, Normalmodus at most 2,
  Gruendlichkeitsmodus has no fixed limit and repeats until green; never turn review findings outside
  the user scope into a second task
- Required builds must be green before task completion/deploy; the pre-build commit order from global
  AGENTS.md §3 remains binding, and build failures require an immediate follow-up fix
- Cross-platform: .ps1 hooks need .sh counterpart (and vice versa)
- Do not leave project files untracked. Anything an agent creates or edits in the repo must be committed and pushed to GitHub, except secrets/local/generated files which must be explicitly ignored or replaced with a safe template.

# Funktionierende Bugfixes ins zweite Gehirn (Cortex/Second Brain) abspeichern (KRITISCH)

> Gilt fuer JEDES Modell unter OpenCode (auch schwache). Ergaenzt **Direktive #3 (Resilient
> Bugfixing)** — die VOR jedem Bugfix gilt (Root Cause finden, funktionserhaltend fixen, verifizieren).
> Diese Regel ist der Ablage-Schritt DANACH. Gesetzt 2026-06-26 (Frank). Codex hat das Format
> eingefuehrt; ab jetzt machen es ALLE CLIs gleich, damit ein gemeinsames Fehler-Gedaechtnis entsteht.

**Die Regel:** Sobald ein Bugfix **bestaetigt FUNKTIONIERT**, schreibe ihn als EINEN Eintrag ueber den
`second-brain`-MCP (`remember`) ins Gehirn — im festen Format, unter `bugfixes/<passende Unterkategorie>`.

**Wann gilt "funktioniert"? (entscheidend — nur funktionierende Fixes ablegen)**
- Hast du es OBJEKTIV verifiziert (Build gruen, Tests ok, Deploy `healthy`, Symptom reproduzierbar weg)
  → speichern.
- Kann es nur der Benutzer beurteilen (Optik/Gefuehl) ODER bist du unsicher → EINMAL kurz fragen
  **"Hat der Fix funktioniert?"** → erst bei **Ja** speichern.
- Default = NICHT speichern, bis bestaetigt. So muss nie etwas geloescht werden. (Sagt der Benutzer
  spaeter "doch nicht funktioniert": den Eintrag per `forget` entfernen, nach echtem Fix neu schreiben.)

**Titel:** `Bugfix <App> <Bereich> <YYYY-MM-DD HH:MM>` — fuer einen Menschen sofort verstaendlich.
`<HH:MM>` = aktuelle LOKALE Uhrzeit (Europe/Berlin, 24h) im Moment des Speicherns -> sofort sichtbar,
WANN genau der Fix ins Gehirn kam (nicht nur an welchem Tag); macht Fixes am selben Bereich eindeutig.
(Beispiele: `Bugfix Cortex Vorlesen Toggle Layout 2026-06-26 14:23`, `Bugfix Cortex Gehirn Kategorie Drilldown 2026-06-26 15:07`.)

**Kategorie:** `bugfixes/<unterkategorie>` — per gezieltem `recall` prüfen, ob eine passende bestehende
Unterkategorie existiert; nur sonst eine neue sprechende anlegen (z.B. `bugfixes/cortex-dashboard`).

**Inhalt:** `Bugfix <YYYY-MM-DD HH:MM>: <App> <Bereich>. Symptom: … Root Cause: … Fix: … Verwandte Pruefung: …
Verifikation: … Funktionalitaets-Diff: … [Poka-Yoke: …]` (dieselben Bausteine wie Direktive #3).

**Danach** dem Benutzer in einem Satz melden: "Im Gehirn dokumentiert: <Titel> [<Kategorie>]."

**Sinn:** Spaeter "hatten wir sowas schon?" → unter `bugfixes/` + Unterkategorie eingrenzen (semantische
Suche `recall`), den Fall finden und wiederverwenden. Sammeln sich aehnliche Faelle → daraus wird eine
Best Practice. Der Repo-Bug-Almanach (`bugs/`) bleibt das proaktive Tech-Wissen; das Gehirn ist die
zentrale, CLI-uebergreifende Fall-Akte.

**NIEMALS:** unbestaetigte Fixes speichern · den Doku-Schritt weglassen · abweichendes Titel-Format ·
blind neue Unterkategorie trotz passender vorhandener · kryptische Titel.
