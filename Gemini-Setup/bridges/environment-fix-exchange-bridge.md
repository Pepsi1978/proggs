# Environment Fix Exchange Bridge (fuer Gemini CLI)

Bruecken-Spezifikation fuer den Austausch von Umgebungs-Fixes zwischen allen CLIs.

## Zweck

Wenn Gemini CLI einen Fehler in der Programmierumgebung fixt (Hooks, Regeln, Scripts,
Agents, Skills, Runtime), wird der Fix so dokumentiert, dass Claude Code und Codex CLI
ihn ohne Session-Kontext verstehen und uebernehmen koennen.

## Scope

- NUR Fixes fuer die Programmierumgebung (Regeln, Runtime, Hooks, Skills, Agents, Setup)
- NIEMALS Projekt-Bugs oder App-Features
- Jeder Fix muss fuer andere CLIs selbsterklaerend sein

## Pflichtformat fuer jeden Fix

Jeder Eintrag im Gemini-Whiteboard (MEMORY.md) braucht mindestens:

- **Kontext**: Was wurde gemacht als der Fehler auftrat?
- **Symptom**: Was sichtbar schiefging (exakte Fehlermeldung)
- **Root Cause**: WARUM es passiert ist (tiefste Ursache, 3x Warum)
- **Fix**: Was konkret geaendert wurde (falsches vs richtiges Muster)
- **Vermeidungsregel**: "Wenn [Situation], dann IMMER [Aktion]"
- **Verwandte Fehlerquellen**: Welche aehnlichen Stellen wurden geprueft?
- **Plattform**: Windows/macOS/beide

## Quellen zum Lesen (read-only)

- Codex: `codex-setup/state/environment-fixes.json`
- Claude Code: `claude-code-setup/environment-fixes.md`

## Ziel zum Schreiben (eigener Workspace)

- `Gemini-Setup/agent-memory/shared/MEMORY.md` (Sektion "Fehlerfixes")

## Sicherheitsregel

- Gemini schreibt NUR in eigene Dateien
- Fremde Fix-Datenbanken sind read-only
- Jeder Fix muss den Resilient-Bugfixing-Standard erfuellen (Direktive 3)
