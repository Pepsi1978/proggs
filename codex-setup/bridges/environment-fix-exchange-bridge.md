# Environment Fix Exchange Bridge

Dies ist die generische Bruecken-Spezifikation fuer den Austausch von Umwelt-Fixes zwischen CLI-Programmierumgebungen.

Ziel:

- nur Fixes fuer Regeln, Runtime, MCP-Nutzung, Validierung, Skills, Agenten, Hooks und Setup
- niemals Projekt-Bugs oder App-Features
- jeden Fix mit `was wurde gefixt` und `warum wurde es gefixt` dokumentieren
- jeden Fix so genau beschreiben, dass andere CLIs ihn ohne Session-Kontext verstehen koennen
- so speichern, dass andere CLIs ihn read-only lesen und uebernehmen koennen

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

- Nur die eigene Setup-Basis darf beschrieben werden.
- Andere CLI-Workspaces duerfen dieses Log lesen, aber nicht ungefragt dort schreiben.
- Das Log ist nur fuer Programmierumgebung und Setup gedacht.

## Triggerwoerter

Die Bruecke soll bei deutschen Formulierungen wie diesen aktiviert werden:

- "logge diesen Umgebungsfix"
- "welche Fehler hast du in deiner Umgebung gefixt"
- "warum hast du diesen Umgebungsfehler gefixt"
- "was kann Cloud Code von deinen Fixes lernen"
- "was kann Gemini CLI von deinen Fixes lernen"

## Referenzimplementierung fuer Codex

- `codex-setup/state/environment-fixes.json`
- `codex-setup/scripts/register-environment-fix.mjs`
- `codex-setup/scripts/register-environment-fix.sh`
- `codex-setup/scripts/register-environment-fix.ps1`
