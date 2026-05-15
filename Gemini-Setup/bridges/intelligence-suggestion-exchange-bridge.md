# Intelligence Suggestion Exchange Bridge (fuer Gemini CLI)

Bruecken-Spezifikation fuer den Austausch von umgesetzten Intelligenz-Vorschlaegen
zwischen allen CLIs.

## Zweck

Wenn Gemini CLI einen Intelligenz-Vorschlag umsetzt (neuer Skill, neue Regel, neuer
Hook, neuer Workflow), wird die Umsetzung so dokumentiert, dass Claude Code und
Codex CLI sie ohne Session-Kontext verstehen und bei Bedarf nachbauen koennen.

## Scope

- NUR umgesetzte Vorschlaege fuer die Programmierumgebung
- NIEMALS Projektcode oder App-Features
- Jeder Eintrag muss selbsterklaerend sein fuer andere CLIs

## Pflichtformat fuer jeden umgesetzten Vorschlag

- **Vorschlagstext**: Was wurde vorgeschlagen?
- **Kontext**: Warum wurde es vorgeschlagen? (Ausloeser, Beobachtung)
- **Umsetzung**: Was wurde konkret gebaut/geaendert?
- **Nutzen**: Was ist der messbare Vorteil?
- **Portierbarkeit**: Koennen andere CLIs das uebernehmen? Wie?
- **Resilienz**: Ueberlebt die Umsetzung Updates und Plattformwechsel?

## Ledger-Adressen

- Gemini: `Gemini-Setup/state/implemented-intelligence-suggestions.json`
- Codex: `codex-setup/state/implemented-intelligence-suggestions.json`
- Claude Code: `claude-code-setup/state/implemented-intelligence-suggestions.json`

## Sicherheitsregel

- Gemini schreibt NUR in eigene Dateien
- Fremde Ledger sind read-only
- Jede Umsetzung muss den Resilient-Bugfixing-Standard erfuellen (Direktive 3)
