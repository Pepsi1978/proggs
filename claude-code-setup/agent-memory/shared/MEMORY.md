# Shared Knowledge Hub — Zentrales Whiteboard

Das zentrale Nervensystem des Claude Code Systems. JEDE Komponente die hier arbeitet
(Agents, Skills, Hooks, MCP-Server, Plugins) MUSS hier lesen und schreiben.

**Zugriff**: Lesen + Schreiben fuer ALLE Komponenten — keine Ausnahmen.
**Gepflegt von**: /self-improve (raeumte auf, loest offene Fehler, haelt aktuell)
**Pfad**: `.claude/agent-memory/shared/MEMORY.md`

**Angeschlossene Systeme** (MUESSEN von /self-improve ebenfalls gelesen werden):
_Hinweis: Pfade mit `~/proggs/` liegen im Repo (autoritativ). Pfade mit `~/.claude/` sind lokal
und maschinenspezifisch (session-scores, cache, etc. — werden NICHT ueber Git synchronisiert)._
- **CLAUDE.md** (WICHTIGSTE DATEI): `~/proggs/CLAUDE.md` + `~/CLAUDE.md` (Sync-Kopie)
  Enthaelt ALLE Projektregeln, Konventionen und Workflow-Definitionen. Wenn /self-improve
  neue Regeln aus Fehlern oder Feedback ableitet → gehoeren sie in die CLAUDE.md.
  Wenn die CLAUDE.md geaendert wird → BEIDE Kopien synchron halten und pushen.
  Ist plattformuebergreifend — wird auf Windows UND macOS gelesen.
- **Benutzer-Feedback**: `~/.claude/projects/*/memory/feedback_*.md`
  (Pfad ist plattformabhaengig: Windows=C--Users-barwa, macOS=Users-frank)
  Enthaelt Korrekturen und Praeferenzen des Benutzers. /self-improve MUSS diese lesen
  und daraus neue Regeln oder Hooks ableiten, wenn ein Feedback-Muster systemweit gilt.
- **Projekt-Notizen**: `~/.claude/projects/*/memory/project_*.md`
  Enthaelt laufende Projekte und deren Status.
- **Memory-Index**: `~/.claude/projects/*/memory/MEMORY.md`
  Zeigt alle vorhandenen Memory-Dateien als Verzeichnis.
- **Session-Scores**: `~/.claude/session-scores.jsonl`
  Qualitaets-Trends ueber Sessions hinweg. /self-improve liest diese fuer Trend-Analyse
  und IQ-Score. Wenn Qualitaet sinkt → Ursache im Whiteboard suchen.
- **Self-Improve Cache**: `~/.claude/self-improve-cache/R1-R5_*.md`
  Gecachte Researcher-Ergebnisse mit TTL. Veraltet? → /self-improve aktualisiert sie.
- **Claude-Mem Observations**: `~/.claude/homunculus/projects/*/observations.jsonl`
  Cross-Session-Wissen vom claude-mem Plugin. Enthaelt Beobachtungen aus frueheren Sessions
  die fuer aktuelle Arbeit relevant sein koennen.

---

## Offene Fehler & Probleme (PRIORITAET fuer /self-improve)
<!-- /self-improve liest diese Sektion ZUERST und MUSS jeden OFFEN-Eintrag fixen. -->
<!-- PFLICHT-FORMAT (damit /self-improve den Fehler versteht und fixen kann): -->
<!--   ### DATUM — Quelle: [Komponententyp: Name] — Kurzbeschreibung -->
<!--   **Quelle:** Welche Komponente (Hook/Agent/MCP-Server/Plugin/Skill + Name) -->
<!--   **Symptom:** Was ist sichtbar schiefgegangen -->
<!--   **Ursache:** WARUM ist es passiert (Root Cause, nicht nur das Symptom!) -->
<!--   **Betroffene Dateien:** Welche Dateien muessen geaendert werden -->
<!--   **Reproduktion:** Wie kann man den Fehler nachstellen -->
<!--   **Fix-Vorschlag:** Was muesste konkret geaendert werden -->
<!--   **Status:** OFFEN | GEFIXT (Datum) -->
<!-- WICHTIG: Fehler MUESSEN ausfuehrlich genug beschrieben werden, dass ein -->
<!-- frischer /self-improve Lauf sie ohne zusaetzlichen Kontext fixen kann! -->

_Aktuell keine offenen Fehler._

### 2026-03-20 — Hook: reindex-codebase.ps1 — Stille Indexierungsfehler + Timeout
**Quelle:** Hook: reindex-codebase.ps1 (SessionStart, async)
**Symptom:** Semantischer Index 16+ Stunden veraltet, spaeter ExitCode 143 (Timeout)
**Ursache:** 1) Bun Import-Pfade relativ zur Datei, nicht CWD — Temp in %TEMP% fand ./src/ nicht.
2) Timeout 180s zu knapp bei 600+ Dateien.
**Fix:** Temp-Datei ins mcp-code-search/ verschoben, Timeout auf 300s erhoeht, finally{}-Cleanup.
**Status:** GEFIXT (2026-03-20)

### 2026-03-20 — Hook: reindex-codebase.ps1 — Stille Indexierungsfehler
**Problem:** Bun loeste Imports relativ zur Temp-Datei in %TEMP% statt zum mcp-code-search/ Verzeichnis auf. `catch {}` verschluckte den Fehler — Index war 16+ Stunden veraltet.
**Fix:** Temp-Datei ins mcp-code-search/ Verzeichnis verschoben. Catch-Bloecke loggen jetzt hierher.
**Status:** GEFIXT (2026-03-20)
**Regel abgeleitet:** Hooks duerfen NIEMALS Fehler still verschlucken. Jeder catch-Block MUSS ins Whiteboard loggen.

---

## Systemzustand (aktuell)
<!-- Wird von /self-improve und env-checker aktualisiert -->
<!-- Zeigt den aktuellen Stand des Programmiersystems -->

- **Plattform:** Windows 11 Home, Claude Code v2.1.80, Opus 4.6 (1M context)
- **Sprachen:** Swift, C#, TypeScript, Rust, Go, Kotlin
- **Semantic Search:** Aktiv (wird bei jeder Session automatisch aktualisiert via reindex-Hook)
- **Ollama:** v0.18.2, nomic-embed-text Modell, Fenster versteckt (nur Tray)
- **Quality Gate:** quality-gate Agent fuer kombiniertes test+review+optimize
- **Agents:** 15 aktiv (code-reviewer hat memory:project, coder hat isolation:worktree)
- **Hooks:** 15 Events registriert (auto-sync, disk-guard, safety-gate, reindex, etc.)
- **Plugins:** 92 Eintraege, 89 aktiv
- **Preferred Patterns:** MVVM (Swift), Fluent Design (C#), strict mode (TypeScript)

---

## Erkenntnisse aus Code Reviews
<!-- code-reviewer, mar-reviewer, batch-reviewer schreiben hierher -->
_Noch keine Eintraege._

## Erkenntnisse aus Tests
<!-- Writer: tester Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Architektur-Entscheidungen
<!-- Writer: architect, challenger Agents | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Debugging-Muster
<!-- Writer: debugger Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Performance & Optimierung
<!-- Writer: optimizer Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## UI/UX-Patterns
<!-- Writer: ui-polisher Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Forschung & Intelligence
<!-- researcher, intelligence-researcher schreiben hierher -->
_Noch keine Eintraege._

---

## Regeln & Konventionen
<!-- Gilt fuer alle Komponenten — von allen lesbar, von /self-improve gepflegt -->

- Kein Python fuer User-Interfaces
- Commit-Format: #NNN - Beschreibung (Englisch)
- Kommunikation: Deutsch, Code-Kommentare: Englisch
- quality-gate MUSS nach jedem Feature/Projekt laufen
- Fehler NIEMALS still verschlucken — immer ins Whiteboard loggen
- Neue Dateien/Strukturen: Pruefen ob Whiteboard-Eintrag noetig
- Einziges Repository: Pepsi1978/proggs
- Cross-Platform: Jede Aenderung MUSS auf beiden Plattformen funktionieren
- Status-Meldung: "Committed, gepusht und plattformuebergreifend" nur wenn ehrlich

### 2026-03-20 14:10 — Hook: reindex-codebase.ps1 — Erneuter ExitCode 143 (Duplikat)
**Hinweis:** Dieser Fehler wurde automatisch vom Hook eingetragen, aber das Timeout war bereits
auf 300s erhoeht. Der Hook lief mit der alten Session-Konfiguration (180s). Kein neuer Fehler.
**Status:** GEFIXT (2026-03-20) — Timeout bereits auf 300s, wirkt ab naechster Session
