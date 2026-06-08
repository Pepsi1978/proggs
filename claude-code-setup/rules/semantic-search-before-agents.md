# Suchstrategie: Semantische Suche vs. Grep vs. Agents (KRITISCH)

> Diese Regel verankert die richtige Werkzeugwahl beim Suchen im Code — nach Direktive #3
> (Resilient Bugfixing, Poka-Yoke + Defense in Depth). Sie wird IMMER automatisch geladen
> und zusaetzlich per subagent-context Hook in jeden Subagent injiziert (Schicht 3, nested
> hookSpecificOutput-Schema — verifiziert 2026-05-24).
>
> Aktualisiert 2026-05-24 nach 3-Researcher-Recherche. Korrigiert den frueheren Fehler,
> Grep faelschlich "semantische Suche" zu nennen.

---

## ⚡ Such-Reflex (das Wichtigste in einem Satz — VOR jeder Code-Suche)

**Halte VOR jeder Suche im Code kurz inne und beantworte EINEN Satz:**
> Kenne ich den exakten Namen/String/Regex? → **Grep/Glob**.
> Nur das Konzept, oder "welche Datei betrifft das ueberhaupt?" → **semantische Suche** (`code-search` MCP).

- Multi-Task-Start: erst semantisch orientieren (welche Dateien?), dann Grep fuer die genaue Zeile.
- Nach 2-3 erfolglosen Greps → semantisch wechseln. Datei >500 Zeilen NICHT per Agent editieren.

**Sichtbare Ansage (PFLICHT bei semantischer Suche):** Sobald die Wahl auf die semantische Suche
faellt, IMMER zuerst diese eine Zeile ausgeben — damit im Terminal sichtbar ist, dass und warum
sie genutzt wird:
> `🔍 Semantische Suche — [kurzer Grund in 1 Halbsatz]`

Gilt fuer den Hauptagenten bei JEDEM `code-search`-Aufruf. (Hinweis: Echte ANSI-Textfarbe geht im
Renderer nicht — das farbige Lupen-Emoji ist die korrekte, gut sichtbare Loesung.)

Das ist die aktive Kurzfassung. Die Begruendung und Details stehen darunter.

---

## Das Kernmissverstaendnis (zuerst lesen)

Frueher stand hier "IMMER semantische Suche (Grep)". Das war FALSCH: **Grep ist KEINE
semantische Suche.** Es gibt zwei voellig verschiedene Werkzeuge:

| Werkzeug | Was es tut | Wann |
|----------|-----------|------|
| **Grep / Glob** | Findet exakten **Text/Muster** (Name, String, Regex) | Wenn du WEISST wonach du suchst |
| **Semantische Suche** (`code-search` MCP) | Findet **Konzepte** ueber Bedeutung (Embeddings) | Wenn du nur das KONZEPT kennst, nicht den Namen |

---

## Die zwei Achsen (NICHT verwechseln)

Beim Suchen/Bearbeiten gibt es zwei UNABHAENGIGE Entscheidungen. Die Dateigroesse (Achse A)
sagt NICHTS darueber, ob Grep oder semantische Suche besser ist (Achse B):

| Achse | Frage | Entscheidet zwischen |
|-------|-------|---------------------|
| **A — Wer arbeitet?** | Wie gross ist die Datei? | Agent ODER direktes Tool |
| **B — Welches Suchwerkzeug?** | Kenne ich den Namen oder nur das Konzept? | Grep ODER semantische Suche |

---

## Achse A: Agent oder direktes Tool? (Dateigroesse)

Bei Dateien mit mehr als **500 Zeilen** NIEMALS Agents zum Editieren spawnen — direkt
mit Such-Tool + Read + Edit arbeiten.

### Warum (Vorfall 2026-04-08)
DashboardScreen.kt (~3000 Zeilen), ~15 Stellen zu aendern:
- 5 Agents gestartet → alle abgestuerzt (zu viel Input)
- 200+ Tool-Aufrufe, 50 Minuten verschwendet
- Danach direkt mit Grep + Edit: unter 1 Minute, 0 Fehler. Faktor 50x.

### Wann Agents OK sind
- Dateien unter 500 Zeilen
- Reine Recherche (WebSearch/WebFetch) ohne Datei-Edits
- Parallele Arbeit an VERSCHIEDENEN kleinen Dateien

### Wann Agents VERBOTEN sind
- ❌ Dateien ueber 500 Zeilen editieren
- ❌ Mehrere Agents auf die GLEICHE grosse Datei

---

## Achse B: Semantische Suche oder Grep? (Name vs. Konzept)

### Die EINE Leitfrage
> **Kenne ich den exakten Namen/String/Regex? → Grep. Kenne ich nur das Konzept/Verhalten? → semantische Suche.**

### Wann semantische Suche (`code-search` MCP)
- Du kennst nur das KONZEPT, nicht den Namen ("wo wird der Nutzer authentifiziert?")
- "Wo im Repo ist X?" — du weisst nicht welche Datei
- Erkundung von unbekanntem/fremdem Code
- Nach **2-3 erfolglosen Grep-Versuchen** (= 3-Iterationen-Stop)

### Wann Grep/Glob (NIEMALS semantisch)
- Exakter Symbolname (`JournalEntryCard`), String, Fehlercode (`ERROR_42`)
- Regex-Muster (`@Composable fun .*Screen`)
- Alle Aufrufstellen einer Funktion / Rename ueber alle Vorkommen
- Suche INNERHALB einer bereits bekannten (auch grossen) Datei

---

## Die zwei Phasen (das Herzstueck)

Ein typischer Multi-Task-Auftrag laeuft in zwei Phasen — pro Phase ein anderes Werkzeug:

| Phase | Frage | Werkzeug |
|-------|-------|----------|
| **1 — Orientierung** | "Welche Dateien betrifft das ueberhaupt?" | **Semantische Suche** |
| **2 — Praezision** | "Welche Zeile in dieser Datei aendere ich?" | **Grep + Read + Edit** |

**Beispiel (5-Aufgaben-Auftrag):** Strava-Dauer, Aufgaben-Dropdowns, Loop-Bereich, Bottom-Navigation.
- Phase 1: 3-4 semantische Suchen ("Strava Trainingsdauer Mapping", "Aufgaben-Screen Heute/Morgen/Spaeter",
  "Loop-Screen Bereich", "Bottom-Navigation Tabs") → lokalisiert ALLE betroffenen Dateien auf einmal.
- Phase 2: pro gefundener Datei Grep + Read + Edit fuer die exakte Stelle (z.B. moving_time → elapsed_time).

Der Reflex "ich suche erst mit Grep die Bereiche" ist in Phase 1 oft langsamer — die semantische
Suche findet die Dateien in EINEM Aufruf statt mit geratenen Namen herumzuprobieren.

---

## Stack-Defaults (Franks Projekte)

| Stack | Semantische Suche | Default |
|-------|-------------------|---------|
| Kotlin/Compose (BestJournal, EntropieReductor) | Mittel | Konzept-Suche uebers Repo: ja. INNERHALB grosser Screen-Dateien (>400 Z.): Grep first |
| C# Code (PromptBoard, TVO) | Gut | Hybrid — semantisch fuer Logik/Patterns |
| XAML (Markup) | **Schlecht** | Grep-only (XML hat per Design keine Semantik) |
| TypeScript/Node | Sehr gut | Semantisch zuerst, Grep als Fallback |
| PowerShell-Hooks | **Schlecht** | Grep-only (kein gutes Embedding/Tool-Support) |
| Bash-Hooks | Mittel | Hybrid |
| Markdown/Regeln/Doku | Gut | Semantisch fuer Themensuche |

---

## Schwellenwerte (es gibt drei — aber nicht die erwartete)

| Schwelle | Wert | Wofuer |
|----------|------|--------|
| Agent ja/nein | **500 Zeilen** | Achse A — bei groesseren Dateien kein Agent |
| Lohnt sich der Index? | **~500 Dateien** im Repo | proggs hat 3760 → lohnt sich klar |
| Grep → semantisch wechseln | **nach 2-3 erfolglosen Greps** | = 3-Iterationen-Stop |

Es gibt KEINE Zeilen-Schwelle fuer Grep-vs-semantisch — das entscheidet allein Achse B (Name vs. Konzept).

---

## Bei vielen gleichartigen Aenderungen
- Bis 20 Stellen: Grep + Edit pro Stelle
- Ueber 20 Stellen: Python-Batch-Script (siehe agent-and-researcher-rules.md, Abschnitt 3: Batch-Edits)

---

## Verankerung (Direktive #3 — Defense in Depth)

Diese Regel ist mehrschichtig verankert, damit die richtige Werkzeugwahl IMMER passiert:

| Schicht | Wo | Wirkung |
|---------|-----|---------|
| 1 | Diese Rule (`~/.claude/rules/`) | Wird bei JEDER Session automatisch geladen (Hauptagent) |
| 2 | Repo-Kopie (`claude-code-setup/rules/`) | Cross-Platform-Sync (macOS + Windows) |
| 3 | `subagent-context` Hook (.ps1 + .sh) | Injiziert die Kompakt-Regel in JEDEN Subagent — verifiziert 2026-05-24. PFLICHT-Schema: `{hookSpecificOutput:{hookEventName:"SubagentStart",additionalContext:...}}`. Flaches `{additionalContext:...}` wird von Claude Code STILL ignoriert (war der alte Bug). PS1: `ConvertTo-Json -Depth 5` noetig. CLAUDE.md ist zusaetzliches Fallback |
| 4 | Pre-Learning-Memory | reference_semantic_search_vs_grep_heuristik.md |

**Bewusst KEINE blockierende Erzwingung (kein Guard-Hook):** Die Werkzeugwahl ist eine
kontextabhaengige Heuristik (kenne ich den Namen oder nur das Konzept?). Ein Hook kann das
nicht zuverlaessig beurteilen und wuerde nur Fehlalarme erzeugen (Fix-Induced Failure laut
Direktive #3, KISS-Prinzip). Die richtige Tiefe ist die immer-geladene Regel + Subagent-Injektion.

---

## Was NIEMALS passieren darf
- ❌ Grep "semantische Suche" nennen (der alte Fehler dieser Datei)
- ❌ Agent auf eine Datei >500 Zeilen zum Editieren
- ❌ Semantische Suche fuer exakte Symbolnamen, Strings, Regex oder Rename
- ❌ In Phase 1 (Orientierung) mit geratenen Glob/Grep-Namen herumprobieren statt semantisch zu suchen
- ❌ Semantische Suche INNERHALB einer bekannten grossen Datei (da Grep + Read)
