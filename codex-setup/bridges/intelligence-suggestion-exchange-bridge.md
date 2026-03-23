# Implemented Intelligence Suggestion Exchange Bridge

Dies ist die generische Bruecken-Spezifikation fuer bereits umgesetzte `Intelligenzvorschlaege`
zwischen CLI-Programmierumgebungen.

Ziel:

- nur umgesetzte Vorschlaege fuer Regeln, Setup, Runtime, Validierung, Skills, Agenten, Hooks und Arbeitsprozesse austauschen
- niemals Projektcode oder App-Features
- festhalten, welcher Vorschlag umgesetzt wurde, warum er wertvoll war und wie er konkret umgesetzt wurde
- jeden Eintrag so genau beschreiben, dass andere CLIs ihn ohne Session-Kontext verstehen und bei Bedarf nachbauen koennen
- die Speicheradressen dieser Ledger fuer andere CLIs frueh sichtbar machen, auch wenn die Fremd-CLI ihren Ledger erst spaeter anlegt

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

## Erwartete Ledger-Adressen

- Codex:
  `codex-setup/state/implemented-intelligence-suggestions.json`
- Cloud Code erwartet:
  `claude-code-setup/state/implemented-intelligence-suggestions.json`
- Gemini CLI erwartet:
  `Gemini-Setup/state/implemented-intelligence-suggestions.json`
- Lokale read-only Zusatzquelle fuer Gemini CLI:
  `%USERPROFILE%\GeminiCLI\state\implemented-intelligence-suggestions.json`

## Bridge-Registry

- Codex:
  `codex-setup/bridges/bridge-registry.json`
- Erwartete Cloud-Code-Registry:
  `claude-code-setup/bridges/bridge-registry.json`
- Erwartete Gemini-Repo-Registry:
  `Gemini-Setup/bridges/bridge-registry.json`
- Erwartete Gemini-Workspace-Registry:
  `%USERPROFILE%\GeminiCLI\bridges\bridge-registry.json`

## Triggerwoerter

Die Bruecke soll bei deutschen Formulierungen wie diesen aktiviert werden:

- "welche Intelligenzvorschlaege hast du umgesetzt"
- "wo ist deine Intelligenzvorschlag-Liste"
- "was koennen Cloud Code oder Gemini CLI von deinen Intelligenzvorschlaegen lernen"
- "welche umgesetzten Intelligenzvorschlaege kann ich in andere CLIs uebernehmen"

## Referenzimplementierung fuer Codex

- `codex-setup/state/implemented-intelligence-suggestions.json`
- `codex-setup/scripts/register-intelligence-suggestion.mjs`
- `codex-setup/scripts/register-intelligence-suggestion.sh`
- `codex-setup/scripts/register-intelligence-suggestion.ps1`
- `codex-setup/scripts/bootstrap-codex-setup.sh`
- `codex-setup/scripts/bootstrap-codex-setup.ps1`

## Erwartete Bootstrap-Adressen

- Codex:
  `codex-setup/scripts/bootstrap-codex-setup.sh`
  und
  `codex-setup/scripts/bootstrap-codex-setup.ps1`
- Cloud Code erwartet:
  `claude-code-setup/scripts/bootstrap-claude-code-setup.sh`
  und
  `claude-code-setup/scripts/bootstrap-claude-code-setup.ps1`
- Gemini CLI erwartet:
  `Gemini-Setup/scripts/bootstrap-gemini-setup.sh`
  und
  `Gemini-Setup/scripts/bootstrap-gemini-setup.ps1`
- Lokale read-only Zusatzquelle fuer Gemini CLI:
  `%USERPROFILE%\GeminiCLI\scripts/bootstrap-gemini-setup.sh`
  und
  `%USERPROFILE%\GeminiCLI\scripts/bootstrap-gemini-setup.ps1`

Andere CLIs sollen diese Bootstrap-Adressen in ihrer Registry und in ihrer Delta-Bruecke mit veroeffentlichen, damit frische Rechner dieselbe Setup-Logik ohne Session-Kontext nachziehen koennen.
