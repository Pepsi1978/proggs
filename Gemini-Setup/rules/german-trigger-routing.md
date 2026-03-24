# Deutsches Trigger-Routing fuer Gemini CLI

Dieses Routing gilt unter der `## Oberste Direktive` fuer Gemini-Setup- und Umgebungsfragen in diesem Workspace.

## Grundregel
- Schreibrechte bestehen nur in `Gemini-Setup/`.
- `codex-setup/` und `claude-code-setup/` sind reine Lesequellen (Brücken).
- Das zentrale Whiteboard ist `C:\Users\barwa\GeminiCLI/Gemini-Setup/agent-memory/shared/MEMORY.md`.

## Triggergruppen

### Codex-Delta (Brücke)
- "was ist neuer bei Codex"
- "uebernimm das von Codex"
- "Starte bitte die Bruecke zu Codex"
- "Codex Setup vergleichen"
- "was wurde bei Codex erneuert"

Aktion:
- `node Gemini-Setup/scripts/audit-codex-delta.mjs` ausführen.
- Kandidaten als `ADD`, `ADAPT` oder `REPLACE` klassifizieren.
- Alles sind nur Vorschläge (💡), keine autonome Umsetzung.

### Claude-Code-Delta (Brücke)
- "was ist neuer bei Claude Code"
- "uebernimm das von Claude Code"
- "Starte bitte die Bruecke zu Claude Code"
- "Claude Code Setup vergleichen"
- "was wurde bei Claude Code erneuert"

Aktion:
- `node Gemini-Setup/scripts/audit-claude-delta.mjs` ausführen.
- Kandidaten als `ADD`, `ADAPT` oder `REPLACE` klassifizieren.
- Alles sind nur Vorschläge (💡), keine autonome Umsetzung.

### Umgebungsfixe (Ledger)
- "logge diesen Umgebungsfix"
- "welche Fehler hast du in deiner Umgebung gefixt"
- "was kann Codex von meinen Fixes lernen"

Aktion:
- `node Gemini-Setup/scripts/register-environment-fix.mjs add` verwenden.
- Pflichtfelder: Context, Symptom, Root-Cause, Why-Chain, Resilience-Summary.

### Intelligenzvorschläge (Ledger)
- "welche Intelligenzvorschlaege hast du umgesetzt"
- "logge diesen umgesetzten Intelligenzvorschlag"

Aktion:
- `node Gemini-Setup/scripts/register-intelligence-suggestion.mjs add` verwenden.

### System-Check & Bootstrap
- "check dein Setup"
- "zeige den Bootstrap-Report"
- "validiere Gemini"

Aktion:
- `bash Gemini-Setup/scripts/validate-gemini-setup.sh` zur Validierung.
- `node Gemini-Setup/scripts/bootstrap-report.mjs` für den Report.
- `node Gemini-Setup/scripts/check-code-search-health.mjs` für die Suche.

### Memory & Whiteboard
- "was steht in deinem Whiteboard"
- "trag das im Memory ein"

Aktion:
- `Gemini-Setup/agent-memory/shared/MEMORY.md` lesen/schreiben.
- Regeln in `Gemini-Setup/rules/` beachten.
