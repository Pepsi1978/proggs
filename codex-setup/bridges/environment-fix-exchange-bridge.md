# Environment Fix Exchange Bridge

Dies ist die generische Bruecken-Spezifikation fuer den Austausch von Umwelt-Fixes zwischen CLI-Programmierumgebungen.

Ziel:

- nur Fixes fuer Regeln, Runtime, MCP-Nutzung, Validierung, Skills, Agenten, Hooks und Setup
- niemals Projekt-Bugs oder App-Features
- jeden Fix mit `was wurde gefixt` und `warum wurde es gefixt` dokumentieren
- so speichern, dass andere CLIs ihn read-only lesen und uebernehmen koennen

## Pflichtformat

Jeder Eintrag braucht mindestens:

- `id`
- `source_cli`
- `category`
- `summary`
- `what_was_fixed`
- `why_it_was_fixed`
- `portable_to`
- `artifacts`
- `created_at`
- `status`

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
