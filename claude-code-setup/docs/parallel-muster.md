# Parallelisierungs-Muster & Speed-Tiers (Referenz)

> Ausgelagert aus `CLAUDE.md` (2026-07-08, verlustfrei). Kernregeln der Parallelisierung stehen
> weiterhin in `~/proggs/CLAUDE.md`; dieses Dokument enthaelt die ausfuehrlichen Muster und die
> Agenten-Tabelle. Bei Bedarf per `Read` nachladen.

## Wann welches Parallelisierungs-Muster

**Parallele Tool-Calls (kein Agent noetig):**
- 2-5 unabhaengige Bash-Befehle, Datei-Reads, Glob/Grep-Suchen in EINEM Antwortblock.
- Beispiel: `brew outdated` + `rustup check` + `claude --version` gleichzeitig.

**Parallele Subagents (Agent-Tool, Foreground):**
- 2-5 unabhaengige Teilaufgaben die jeweils eigene Analyse/Arbeit brauchen.
- Beispiele: Code Review + Tests + UI Polish nach einem Feature; 3 Dateien gleichzeitig
  refactoren; Recherche zu 3 Themen parallel.
- Jeder Subagent bekommt vollen Kontext: Projekt, Dateien, Konventionen.

**Agent Teams (TeamCreate — NUR auf Ansage oder bei klarem Mehrwert):**
- Teams sind wie ein Buero mit Mitarbeitern die untereinander reden koennen — im Gegensatz zu
  normalen Subagents (Boten die nur Ergebnisse zurueckbringen).
- **3-4x so teuer** wie normale Subagents. Nur wenn Teammates wirklich kommunizieren muessen
  (z.B. Frontend baut auf Backend-API, beide muessen sich abstimmen).
- NICHT fuer unabhaengige Aufgaben (Researcher-Schwarm, Qualitaetsschleife) — dafuer normale
  parallele Subagents.
- Windows-Einschraenkung: kein Split-Screen-Modus (braucht tmux), Teammates laufen trotzdem.
- 5-6 Tasks pro Teammate, Datei-Ownership strikt trennen.

## Speed-Tiers: Agent → Modell

Alle Subagents laufen auf Opus 4.8 (1M) — Mechanik: `~/.claude/rules/highest-model-everywhere.md`.

| Aufgabe | Agent | Modell | Warum |
|---------|-------|--------|-------|
| Architektur, Design | `architect` | Opus | Tiefes Reasoning |
| Debugging | `debugger` | Opus | Komplexe Ursachenanalyse |
| Code Review (Sicherheit) | `code-reviewer` | Opus | Sicherheitsluecken erkennen |
| Performance | `optimizer` | Opus | Systemweites Verstaendnis |
| UI-Verbesserung | `ui-polisher` | Opus | Design-Expertise |
| Implementation | `coder` | Opus 4.8 (1M) | Hoechstes Modell, max. Stabilitaet |
| Bulk-Reviews | `batch-reviewer` | Opus 4.8 (1M) | Viele Dateien pruefen |
| Tests | `tester` | Opus | Qualitaet bei Tests wichtig |
| Recherche | `researcher` | Opus 4.8 (1M) | Web-Lookup |
| Web-Research-Eskalation C | `research`-Skill Engine C | Sonnet 5 (1M), Effort high | Einzige Ausnahme — `research-strategy.md` §4a |

**Faustregel:** 3-5 `coder`-Agents parallel spawnen, dann 1 `code-reviewer` (Opus) fuer die
Qualitaetskontrolle.

## Konkrete Parallel-Muster

**Nach jedem Feature (Qualitaetsschleife parallel):**
```
→ Gleichzeitig 3 Agents: tester (Build+Tests) | code-reviewer (Security+Quality) | optimizer + ui-polisher
→ Erst wenn alle 3 bestanden: Commit + Push
```

**Bei neuem Projekt:**
```
→ Gleichzeitig 2 Agents: architect (Architektur) | Recherche (Libs, APIs, Best Practices)
→ Ergebnisse zusammenfuehren, dann implementieren
```

**Bei Implementation (maximale Geschwindigkeit):**
```
→ Gleichzeitig 3-5 coder-Agents: je eine Datei/Schicht (Model, View, Controller ...)
→ Jeder Coder bekommt: Projektkontext, eigene Dateien, Konventionen
→ Danach: batch-reviewer (Bulk) → code-reviewer (Opus, tief)
```

**Bei Cross-Platform-Feature:**
```
→ Gleichzeitig 2 Agents: macOS (Swift/AppKit) | Windows (C#/WPF) → parallel testen
```

**Bei Recherche:**
```
→ 3-5 researcher-Agents parallel, je ein Thema → Ergebnisse zusammenfuehren
(Web-Recherche IMMER ueber das Protokoll in research-strategy.md!)
```

**Bei Code-Verbesserungen:**
```
→ Verschiedene Dateien von verschiedenen Agents — Datei-Ownership: NIE die gleiche Datei doppelt
```

**Bei grossen Migrationen:**
```
→ /batch Command (bis 10x schneller): zerlegt in unabhaengige Einheiten, parallele Worker in Worktrees
```
