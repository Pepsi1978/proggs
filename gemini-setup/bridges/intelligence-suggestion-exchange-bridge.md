<!-- DEPRECATED: 2026-03-25 — Replaced by Universal Mirror Bridge (mirror-ledger.md + export/import agents) -->
<!-- This file is kept for reference only. New cross-platform sync uses mirror-ledger.md -->

# Implemented Intelligence Suggestion Exchange Bridge (fuer Gemini CLI)

Dies ist Gemini CLIs Bruecken-Spezifikation fuer bereits umgesetzte `Intelligenzvorschlaege`
zwischen CLI-Programmierumgebungen.

Ziel:

- nur umgesetzte Vorschlaege fuer Regeln, Setup, Runtime, Validierung, Skills, Agents, Hooks und Arbeitsprozesse austauschen
- niemals Projektcode oder App-Features
- festhalten, welcher Vorschlag umgesetzt wurde, warum er wertvoll war und wie er konkret umgesetzt wurde
- jeden Eintrag so genau beschreiben, dass andere CLIs ihn ohne Session-Kontext verstehen und bei Bedarf nachbauen koennen
- die Speicheradressen dieser Ledger fuer andere CLIs frueh sichtbar machen, auch wenn die Fremd-CLI ihren Ledger erst spaeter anlegt

## Scope

Gemini CLI Ledger (schreibt nur hier):

- `gemini-setup/state/implemented-intelligence-suggestions.json`

Peer Ledger (read-only):

- `codex-setup/state/implemented-intelligence-suggestions.json`
- `gemini-setup/state/implemented-intelligence-suggestions.json`

## Pflichtformat

Jeder Eintrag braucht mindestens:

- `id`
- `source_cli`
- `summary`
- `proposal_text`
- `context_for_other_clis`
- `why_it_was_suggested`
- `why_it_was_implemented`
- `how_it_was_implemented`
- `bridge_value`
- `adoption_guidance`
- `resilience_summary`
- `future_failure_review`
- `portable_to`
- `artifacts`
- `created_at`
- `status`

## Erklaerungstiefe

- Andere CLIs duerfen nicht voraussetzen, dass sie die urspruengliche Session oder den ausloesenden Reibungspunkt kennen.
- Deshalb muss jeder Eintrag den noetigen Kontext, den eigentlichen Vorschlag, den Nutzen, die Umsetzung und die Portierungslogik in vollstaendigen Saetzen erklaeren.
- Umgesetzte `Intelligenzvorschlaege` sollen denselben Zukunftsstandard wie Umwelt-Fixes haben. Deshalb braucht jeder Eintrag auch eine Resilienz-Zusammenfassung und ein Future-Failure-Review.
- Kurze Notizen wie "Helper gebaut" oder "Bridge verbessert" reichen nicht.

## Brueckenpflicht fuer andere CLIs

- Jede CLI soll ihren eigenen Ledger fuer umgesetzte `Intelligenzvorschlaege` im eigenen Workspace anlegen.
- Jede CLI soll die Adresse dieses Ledgers zusaetzlich in ihrer Delta-Bruecke veroeffentlichen.
- Andere CLIs duerfen diese Ledger read-only lesen, aber niemals ungefragt dort schreiben.
- Wenn eine Fremd-CLI den Ledger noch nicht angelegt hat, soll die Bruecke trotzdem die erwartete Zieladresse kennen und spaeter nachziehen koennen.

## Ledger-Adressen

- Gemini CLI:
  `gemini-setup/state/implemented-intelligence-suggestions.json`
- Codex (read-only):
  `codex-setup/state/implemented-intelligence-suggestions.json`
- Gemini CLI erwartet:
  `gemini-setup/state/implemented-intelligence-suggestions.json`

## Bridge-Registry

- Gemini CLI:
  `gemini-setup/bridges/bridge-registry.json`
- Codex-Registry (read-only):
  `codex-setup/bridges/bridge-registry.json`
- Erwartete Gemini-Repo-Registry:
  `gemini-setup/bridges/bridge-registry.json`

## Triggerwoerter

Die Bruecke soll bei deutschen Formulierungen wie diesen aktiviert werden:

- "welche Intelligenzvorschlaege hast du umgesetzt"
- "wo ist deine Intelligenzvorschlag-Liste"
- "was koennen Codex oder Gemini CLI von deinen Intelligenzvorschlaegen lernen"
- "welche umgesetzten Intelligenzvorschlaege kann ich in andere CLIs uebernehmen"
- "trage diesen Intelligenzvorschlag in den Ledger ein"
- "dokumentiere diesen umgesetzten Vorschlag"

## Referenzimplementierung fuer Gemini CLI

- `gemini-setup/state/implemented-intelligence-suggestions.json`
- `gemini-setup/scripts/register-intelligence-suggestion.mjs`
- `gemini-setup/scripts/register-intelligence-suggestion.sh`
- `gemini-setup/scripts/bootstrap-gemini-setup.sh`
- `gemini-setup/scripts/bootstrap-gemini-setup.ps1`

## Bootstrap-Adressen

- Gemini CLI:
  `gemini-setup/scripts/bootstrap-gemini-setup.sh`
  und
  `gemini-setup/scripts/bootstrap-gemini-setup.ps1`
- Codex (read-only):
  `codex-setup/scripts/bootstrap-codex-setup.sh`
  und
  `codex-setup/scripts/bootstrap-codex-setup.ps1`
- Gemini CLI erwartet:
  `gemini-setup/scripts/bootstrap-gemini-setup.sh`
  und
  `gemini-setup/scripts/bootstrap-gemini-setup.ps1`

Andere CLIs sollen diese Bootstrap-Adressen in ihrer Registry und in ihrer Delta-Bruecke mit veroeffentlichen, damit frische Rechner dieselbe Setup-Logik ohne Session-Kontext nachziehen koennen.

## Bootstrap-Report-Adressen

- Gemini CLI:
  `gemini-setup/scripts/bootstrap-report.mjs`
  `gemini-setup/scripts/bootstrap-report.sh`
  `gemini-setup/scripts/bootstrap-report.ps1`
- Codex (read-only):
  `codex-setup/scripts/bootstrap-report.mjs`
  `codex-setup/scripts/bootstrap-report.sh`
  `codex-setup/scripts/bootstrap-report.ps1`
- Gemini CLI erwartet:
  `gemini-setup/scripts/bootstrap-report.mjs`
  `gemini-setup/scripts/bootstrap-report.sh`
  `gemini-setup/scripts/bootstrap-report.ps1`

Andere CLIs sollen diesen Bootstrap-Report ebenfalls in Registry und Delta-Bruecke veroeffentlichen, damit die Gesamtstruktur fuer neue Rechner und Fremd-CLIs auf einen Blick sichtbar ist.

