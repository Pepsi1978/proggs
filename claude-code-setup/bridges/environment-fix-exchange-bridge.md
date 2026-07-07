<!-- DEPRECATED: 2026-03-25 — Replaced by Universal Mirror Bridge (mirror-ledger.md + export/import agents) -->
<!-- This file is kept for reference only. New cross-platform sync uses mirror-ledger.md -->

# Environment Fix Exchange Bridge (fuer Claude Code)

Dies ist Claude Codes Bruecken-Spezifikation fuer den Austausch von Umgebungs-Fixes zwischen CLI-Programmierumgebungen.

Ziel:

- nur Fixes fuer Regeln, Runtime, MCP-Nutzung, Validierung, Skills, Agents, Hooks und Setup
- niemals Projekt-Bugs oder App-Features
- jeden Fix mit `was wurde gefixt` und `warum wurde es gefixt` dokumentieren
- jeden Fix so genau beschreiben, dass andere CLIs ihn ohne Session-Kontext verstehen koennen
- so speichern, dass andere CLIs ihn read-only lesen und uebernehmen koennen

## Scope

Claude Code schreibt ausschliesslich in:

- `claude-code-setup/environment-fixes.md` — Markdown-Ledger fuer alle Umgebungs-Fixes

Read-only Quellen (Claude Code liest, schreibt NIEMALS dort):

- `codex-setup/state/environment-fixes.json` — Codex-Fehlerfixes (JSON)
- `Gemini-Setup/agent-memory/shared/MEMORY.md` — Gemini-Whiteboard (Fehler-Fixes, Erkenntnisse)

## Pflichtformat

Jeder Eintrag braucht mindestens:

- `id`
- `source_cli`
- `category`
- `summary`
- `context_for_other_clis`
- `symptom_before_fix`
- `root_cause`
- `what_was_fixed`
- `why_it_was_fixed`
- `exact_error_text`
- `root_cause_why_chain`
- `related_error_sources_checked`
- `wrong_pattern_example`
- `right_pattern_example`
- `avoidance_rule`
- `resilience_summary`
- `fix_induced_failure_review`
- `verification`
- `portability_notes`
- `portable_to`
- `artifacts`
- `created_at`
- `status`

## Erklaerungstiefe

- Andere CLIs duerfen nie voraussetzen, dass sie dieselbe Session, dieselben Logs oder denselben Fehlerkontext gesehen haben.
- Deshalb muss jeder Eintrag das Umfeld, das sichtbare Symptom, die technische Ursache, die konkrete Aenderung und die Verifikation in vollstaendigen Saetzen erklaeren.
- Resilientes Bugfixing verlangt zusaetzlich eine kurze Why-Chain, gepruefte verwandte Fehlerquellen, falsches und richtiges Muster, eine klare Vermeidungsregel sowie eine Failure-Review-Zusammenfassung.
- Kurze Notizen wie "Parser gefixt" oder "Windows-Problem geloest" reichen nicht.

## Sicherheitsregel

- Claude Code schreibt ausschliesslich in `claude-code-setup/environment-fixes.md`.
- Andere CLI-Workspaces duerfen dieses Log lesen, aber nicht ungefragt dort schreiben.
- Das Log ist nur fuer Programmierumgebung und Setup gedacht.
- Claude Code liest Codex- und Gemini-Quellen NUR read-only.

## Triggerwoerter

Die Bruecke soll bei deutschen Formulierungen wie diesen aktiviert werden:

- "logge diesen Umgebungsfix"
- "welche Fehler hast du in deiner Umgebung gefixt"
- "warum hast du diesen Umgebungsfehler gefixt"
- "was kann Codex von deinen Fixes lernen"
- "was kann Gemini CLI von deinen Fixes lernen"
- "trage diesen Fix in das Fix-Log ein"
- "dokumentiere diesen Fehler"

## Referenzimplementierung fuer Claude Code

- `claude-code-setup/environment-fixes.md`
- `claude-code-setup/scripts/register-environment-fix.mjs`
- `claude-code-setup/scripts/register-environment-fix.sh`

## Bridge-Registry

- Claude Code:
  `claude-code-setup/bridges/bridge-registry.json`
- Codex-Registry (read-only):
  `codex-setup/bridges/bridge-registry.json`
- Erwartete Gemini-Repo-Registry:
  `Gemini-Setup/bridges/bridge-registry.json`
