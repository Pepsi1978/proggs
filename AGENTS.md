# AGENTS.md — Agent-Optimized Context for Claude Code Agents

> This file is for AGENTS (coder, tester, reviewer, optimizer), not humans.
> It provides the minimum context needed for efficient autonomous work.
> Source: arXiv 2601.20404 — structured agent context reduces tool calls by 15-30%.

## Repository Structure

```
~/proggs/                          # Monorepo: Pepsi1978/proggs
  BestJournalAndroid/              # Kotlin/Compose journal app (Google Play)
  BestJournalFrank/                # Kotlin/Compose journal app (clone, no Firebase, no Premium)
  claude-code-setup/               # Claude Code config sync (hooks, rules, agents, settings)
  ClaudeCodexVoiceOverlay-macOS/   # Swift voice overlay
  ClaudeVoiceOverlay-Windows/      # C#/WPF voice overlay
  Tampermonkey/                    # Browser userscripts (JavaScript)
  QuizVerse/                       # Kotlin quiz app
  mcp-code-search/                 # Local semantic search server
  tools/                           # Shared utilities
```

## Build Commands

| Project | Build | Test | Lint |
|---------|-------|------|------|
| BestJournalAndroid | `cd BestJournalAndroid && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| BestJournalFrank | `cd BestJournalFrank && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| Tampermonkey | N/A | Manual browser test | `bunx biome check <file>` |
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

`#NNN - Description` (English). Number auto-incremented from last commit.

## Quality Requirements

- Every feature must pass `quality-gate` agent before commit
- Build must be green before commit (no broken builds pushed)
- Cross-platform: .ps1 hooks need .sh counterpart (and vice versa)
- Do not leave project files untracked. Anything an agent creates or edits in the repo must be committed and pushed to GitHub, except secrets/local/generated files which must be explicitly ignored or replaced with a safe template.

# Parallele Sessions — Commit & Push am geteilten main-Branch (KRITISCH)

Diese Regel gilt AUTOMATISCH in JEDER Session, auf ALLEN Plattformen
(Windows + macOS), unabhaengig davon welches CLI gerade laeuft (Codex,
Claude Code, Gemini CLI, Terminal). Der Benutzer arbeitet oft mit 4-5
gleichzeitig offenen Sessions am selben Repo. Alle Sessions pushen auf main.
Diese Regel stellt sicher, dass trotzdem nichts ueberschrieben wird und nichts
kaputtgeht.

## Grundprinzip

Es wird IMMER auf `main` committet und nach `origin/main` gepusht.
Keine Feature-Branches. Keine Worktrees. Kein Merge-Dance. Nur `main` —
aber mit diszipliniertem `fetch + rebase + push` vor jedem Push.

## Einmalige Git-Konfiguration (MUSS gesetzt sein)

Vor der ersten Session auf einem neuen Rechner diese drei Zeilen setzen.
Sie machen 90% der Konflikte unsichtbar:

    git config --global pull.rebase true        # Pull nutzt IMMER rebase, nie merge
    git config --global rebase.autoStash true   # Lokale Aenderungen automatisch stashen+poppen
    git config --global rerere.enabled true     # Konfliktloesungen werden gemerkt

Pflicht beim Session-Start: pruefen ob diese Werte gesetzt sind, falls nicht
einmalig setzen und kurz melden.

## Der Standard-Ablauf nach jeder Aufgabe

Nach JEDER abgeschlossenen Aufgabe wird SOFORT committet und gepusht.
Die Reihenfolge ist IMMER dieselbe — keine Abkuerzungen, keine Umwege:

    # 1. Nur eigene Dateien namentlich stagen — NIE git add -A oder git add .
    git add <datei-1> <datei-2> <datei-3>

    # 2. Commit mit fortlaufender Nummer
    git commit -m "#NNNN - Beschreibung auf Englisch"

    # 3. Vor dem Push IMMER synchronisieren
    git fetch origin
    git rebase origin/main

    # 4. Push
    git push

Die vier Schritte sind atomar. Kein Push ohne vorheriges fetch+rebase.

## Verhalten bei Push-Rejection

Eine andere Session war schneller und hat zwischenzeitlich gepusht. Die Reaktion
ist IMMER dieselbe — ruhig bleiben, nicht force-pushen:

    git fetch origin && git rebase origin/main && git push

Einfach wiederholen. Bei der zweiten Rejection wieder wiederholen.

Bei der dritten Rejection in Folge: Stop. Kurz 5-10 Sekunden warten, damit die
andere Session ihren Push-Burst fertig hat. Dann nochmal versuchen. Wenn das
immer noch fehlschlaegt: dem Benutzer melden, nicht stumm weiterprobieren.

Niemals als Ausweg: `--force`, `--force-with-lease`, `reset --hard`, neues
Repo klonen. Diese zerstoeren Arbeit der anderen Sessions.

## Umgang mit fremden Aenderungen im Working Tree

Beim Session-Start kann der Working Tree uncommittete Dateien von einer
frueheren Session oder einer parallel laufenden Session enthalten. Diese
Aenderungen duerfen NIEMALS mitcommittet werden — sie gehoeren dieser
Session nicht.

### Vor dem Commit: Nur eigene Dateien stagen

    # FALSCH — greift alles was im Working Tree liegt
    git add -A
    git add .

    # RICHTIG — nur die Dateien die DIESE Session geaendert hat
    git add pfad/zu/datei1.kt pfad/zu/datei2.xml

Falls versehentlich fremde Dateien im Index landen:

    git restore --staged <fremde-datei>

### Bei Rebase-Blockade durch fremde unstaged Aenderungen

Fehler: `error: cannot rebase: You have unstaged changes.`

Vorgehen:

    # Fremde Aenderungen kurz beiseite packen
    git stash push -u -m "hold-NNNN"

    # Jetzt laeuft Rebase + Push durch
    git fetch origin && git rebase origin/main && git push

    # Fremde Aenderungen wieder zurueckholen
    git stash pop

Die fremden Aenderungen liegen dann wieder unversioniert im Working Tree und
koennen spaeter von ihrer eigenen Session committet werden.

## Die absoluten Tabus

Diese Aktionen sind in parallelen Session-Setups VERBOTEN. Sie zerstoeren
garantiert die Arbeit anderer Sessions:

- `git push --force` — ueberschreibt Commits anderer Sessions auf origin/main
- `git push --force-with-lease` — schuetzt nur gegen lokale Konflikte, nicht
  gegen parallele Sessions
- `git reset --hard` ohne explizite Benutzer-Freigabe — zerstoert unversionierte
  Arbeit anderer Sessions
- `git add -A` / `git add .` — greift fremde In-Flight-Dateien ab
- `git commit --amend` auf bereits gepushte Commits — zwingt zum Force-Push
- `git rebase -i origin/main` (interaktiver Rebase ueber bereits gepushte
  Commits) — veraendert oeffentliche History
- Warten ohne Grund ("sleep bis der andere fertig ist") — blockiert eigene
  Arbeit ohne Nutzen

## Commit-Granularitaet

Ein Commit = ein abgeschlossener Zweck.

- 1 Datei, 1 Fix                 → 1 Commit
- 1 Feature, mehrere Dateien     → 1 Commit nach Abschluss
- Groesseres Feature (>15 Min)   → Mehrere Commits nach logischen Teilschritten

Faustregel: Lieber 5 kleine Commits als 1 grosser. Jeder kleine Commit ist ein
Rettungspunkt und reduziert die Wahrscheinlichkeit eines Rebase-Konflikts mit
anderen Sessions.

Keine Sammel-Commits mit mehreren unabhaengigen Aufgaben. Das erschwert
Revert und erhoeht Konfliktwahrscheinlichkeit.

## Unsicherheit: Was machen die anderen Sessions gerade?

Wenn unklar ist ob eine andere Session gerade dieselben Dateien bearbeitet,
diese drei Befehle nacheinander laufen lassen:

    # 1. Welche Dateien hat DIESE Session geaendert?
    git status --short

    # 2. Hat eine andere Session schon weiter gepusht?
    git fetch origin && git log HEAD..origin/main --oneline

    # 3. Gibt es neue Remote-Commits die dieselben Dateien anfassen?
    git fetch origin && git diff --name-only HEAD origin/main

Wenn die dritte Ausgabe eine Datei zeigt die auch diese Session geaendert hat:
Besonders klein committen und sofort pushen, damit der Konflikt schnell
aufgeloest wird statt zu wachsen.

## Commit-Nachrichten (Format)

Jede Commit-Nachricht beginnt mit einer fortlaufenden Nummer:

    #NNNN - Beschreibung auf Englisch

Die Nummerierung wird anhand der existierenden Commits im Repo automatisch
ermittelt (letzte Nummer + 1).

## Zusammenfassung in einem Satz

Parallele Sessions (auch ueber verschiedene CLIs hinweg) sind OK, solange
jede Session vor dem Push `git fetch + git rebase origin/main` ausfuehrt,
nur ihre eigenen Dateien namentlich staged und bei Rejection einfach
`fetch + rebase + push` wiederholt — statt zu force-pushen oder zu resetten.

## Geltungsbereich

Diese Regel ist CLI-agnostisch. Sie gilt identisch fuer Codex, Claude Code,
Gemini CLI und jede andere Session die auf demselben Git-Repo arbeitet.
Jedes CLI muss sie unabhaengig kennen und befolgen, weil die Sessions nicht
miteinander kommunizieren koennen — nur der sauber disziplinierte
Git-Workflow verhindert Kollisionen.
