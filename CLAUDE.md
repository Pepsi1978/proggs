# Projektregeln

> Kompakte Steuerdatei. In `~/.claude/rules/` liegen die Regel-Kerne (je ≤1,5 KB, IMMER geladen);
> ausgelagerte Volltexte/Details in `claude-code-setup/docs/rules/` bei Bedarf per `Read` (nie `@`-Import).

## DIE 3 HAUPTDIREKTIVEN (HOECHSTE PRIORITAET — IMMER AKTIV)

Stehen UEBER allen Regeln, in jeder Session bei jeder Aufgabe. **Kern** in `~/.claude/rules/` (immer
geladen); **1:1-Volltext** in `claude-code-setup/docs/rules/` — auf Zuruf "nach Direktive 1/2/3" per
`Read`. Kern UND Volltext NIEMALS abschwaechen/entfernen.

| Rang | Direktive | Kern | Volltext auf Zuruf |
|------|-----------|------|--------------------|
| #1 | ⚡ Superintelligenz | Intelligenteste Umgebung der Welt. Compound Intelligence Effect. | `docs/rules/superintelligence.md` |
| #2 | 🔍 Selbstbeobachtung | Beobachten, Erkennen, Lernen. Rueckblick + Vorschlaege nach jeder Aufgabe. | `docs/rules/self-observation.md` |
| #3 | 🛡️ Resilient Bugfixing | Kein Fehler zweimal. Root Cause → Poka-Yoke → dokumentieren. Funktionserhaltend. | `docs/rules/resilient-bugfixing.md` |

- **Intelligenz-Vorschlaege** nach jeder Aufgabe (Format `intelligence-system.md`), nach der Status-Meldung, min. 1/Session.
- **Bug-Case Auto-Writer:** jeder Fehler → `.claude/agent-memory/shared/bug-cases.jsonl`; auto-Eintraege nach dem Fix mit Root Cause ergaenzen.

## Commit + Push nach JEDER Aenderung (FUNDAMENTAL)

**Aufgabe erledigt → Commit → Push → Status-Meldung. IMMER. SOFORT.** Commits = Rettungspunkte (lieber 5
kleine als 1 grosser). Gilt fuer JEDE Aenderung. Ablauf: Cross-Platform pruefen → `git add` (nur eigene
Dateien namentlich) → `git commit "#NNN - Text"` → `git fetch origin && git rebase origin/main` →
`git push` → Status-Meldung. NIEMALS: sammeln + einmal committen, "spaeter committen", committen ohne
Push, Session mit uncommitteten Aenderungen beenden, fragen "soll ich committen?". Details:
`parallel-sessions-git.md`, `git-workflow.md`, `commit-before-build.md`. Commit-Nr fortlaufend (aus Log);
Benutzer-Name → `#NNN - Name`.

## Status-Meldung (letzter Satz nach den Abschluss-Boxen)

**"Committed, gepusht und plattformuebergreifend."** (nur wenn Cross-Platform WIRKLICH umgesetzt) ·
**"Committed und gepusht."** (rein plattformunabhaengig) · **"…Cross-Platform: [was fehlt]."** ·
**"Ich habe weder committed noch gepusht."** Ehrlichkeit: "plattformuebergreifend" nur wenn wirklich gemacht.

## Cross-Platform-Pflicht (VOR dem Commit)

`.ps1`-Hook geaendert → `.sh` SOFORT mitziehen (+ umgekehrt). `~/.claude/settings.json` geaendert → alle
DREI Setup-Dateien (`settings-reference.json` 1:1 Windows, `settings.json` macOS, `settings.local.json`
Vorlage). Harness → BEIDE Spiegelorte (`harness-mirror-on-change.md`). Alle Projekte laufen macOS
(Swift/AppKit) UND Windows (C#/WPF).

## Sichtbarkeit + Arbeitsweise

- NIEMALS unsichtbar arbeiten (kein `context: fork`, keine stillen Subagents); vor jeder Aktion kurz auf
  Deutsch erklaeren, danach Ergebnis zeigen. **Parallele Agenten erwuenscht.**
- **CLAUDE_AUTOCOMPACT_PCT_OVERRIDE ist IMMER 100** (nie unter 85; config-guard sichert ab).
- Nie nach Erlaubnis fragen fuer Standardaktionen (Build/Test/Commit/Push/Edit); Terminal-Befehle selbst
  ausfuehren. Bei Fehlern selbst debuggen. Ergebnisse ausfuehrlich erklaeren (Benutzer ist kein Programmierer).
- **Effort:** Standard "high" bei echtem Neustart; `/effort`-Override gilt Session-Rest. Nur via
  `effortLevel`-Setting, NIE via `CLAUDE_CODE_EFFORT_LEVEL`-Env. Effort NIEMALS selbst aendern.
- **Session-Backup/Restore:** NUR auf Franks manuelle Ansage (`session`-Skill).
- **Shell-/Terminal-Updates:** allerletzter Schritt, vorher warnen; danach PATH-Verifikation (`platform-and-paths.md` §4).

## Repository + Parallelisierung

ALLES in `Pepsi1978/proggs` (lokal `~/proggs/`); NIEMALS neue GitHub-Repos; neue Projekte = Unterordner;
CLAUDE.md existiert NUR im Repo. — 2+ unabhaengige Aufgaben → SOFORT parallel (3-5 Agents Sweet Spot);
**Datei-Ownership heilig** (nie dieselbe Datei von 2 Agenten); Agents erben keine Historie → vollen
Kontext in den Prompt. **Modell:** alle Subagents `opus[1m]` (ad-hoc explizit `model:"opus[1m]"`),
Ausnahme Research-Engine C = Sonnet 5 (`highest-model-everywhere.md`); Absturzsicherheit
`subagent-crash-proofing.md`. Muster: `claude-code-setup/docs/parallel-muster.md`. Such-Reflex: Grep/Glob
(kein code-search im CLI), Agent-Scope nach Dateigroesse `search-and-agent-scope.md`.

## Qualitaet + Konventionen

- Nach jedem Feature **`quality-gate`**-Agent (tester+code-reviewer+optimizer, PASS/FAIL); KEIN Commit
  ohne Gate (ausser reine Config-/CLAUDE.md-/Memory-Aenderungen). Neues Projekt: `architect` + Recherche. Bugs: `debugger`.
- **Whiteboard** `.claude/agent-memory/shared/MEMORY.md` — EINZIGE zentrale Wissensdatei; jeder liest +
  schreibt Erkenntnisse/Fehler hinein (Fehler NIE still verschlucken).
- Sprachen: Swift, C#, TypeScript, Rust, Go. KEIN Python fuer GUIs/Desktop (unvermeidbar → fragen). UI wie
  gekaufte Software, Auslieferung einzelne `.app`/`.exe`. Jedes Projekt: ausfuehrliche `README.md`.
- Skills NUR ueber `/skill-creator:skill-creator`. Erster Repo-Start: `claude-code-setup/manifest.json` pruefen.
- **Observability-First** bei qualifizierten Projekten ZUERST (`observability-first.md`). **Cowork:** nie
  nacktes git — `bash ~/proggs/cowork-git.sh` (`cowork-git-push.md`). **Externer Code:** vor Installation lesen/pruefen.

## Compact Instructions

Bei Komprimierung MUSS bleiben: Geltung der 3 Direktiven; Ziel + Stand der laufenden Aufgabe (Commit-#,
offene Schritte, Multi-Task-Listen); uncommittete Dateien; noch nicht persistierte Benutzer-Korrekturen;
Commit+Push- + Cross-Platform- + Status-Meldungs-Pflicht. Wegfallen: Tool-Ausgaben-Detail, committete Zwischenschritte, Datei-Dumps.

## Sprache

Mit dem Benutzer Deutsch, echte Umlaute (nie "ae/oe/ue/ss"). Code-Kommentare + Commits englisch. Eigene
Agents/Skills/Commands komplett deutsch; externe Plugins nicht uebersetzen. Detail: `communication-and-language.md`.
