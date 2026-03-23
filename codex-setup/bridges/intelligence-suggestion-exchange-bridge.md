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
- `portable_to`
- `artifacts`
- `created_at`
- `status`

## Erklaerungstiefe

- Andere CLIs duerfen nicht voraussetzen, dass sie die urspruengliche Session oder den ausloesenden Reibungspunkt kennen.
- Deshalb muss jeder Eintrag den noetigen Kontext, den eigentlichen Vorschlag, den Nutzen, die Umsetzung und die Portierungslogik in vollstaendigen Saetzen erklaeren.
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
  `C:\Users\barwa\GeminiCLI\state\implemented-intelligence-suggestions.json`

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
