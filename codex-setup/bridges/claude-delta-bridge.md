<!-- DEPRECATED: 2026-03-25 — Replaced by Universal Mirror Bridge (mirror-ledger.md + export/import agents) -->
<!-- This file is kept as Codex-native audit metadata. -->

# Claude Code Delta Bridge (für Codex)

Dies ist Codex' Brücken-Spezifikation, um sinnvolle Verbesserungen aus Claude Code für die eigene Programmierumgebung zu erfassen.

## Scope

Read-only Quellen (Codex liest, schreibt NIEMALS dort):

- `claude-code-setup/**` — Regeln, Skills, Scripts, State, Bridges
- `claude-code-setup/environment-fixes.md` — Claude-Code-Umgebungsfixes
- `claude-code-setup/agent-memory/shared/MEMORY.md` — Claude-Code-Whiteboard
- `CLAUDE.md` — Claude-Code-Agenten-Definitionen

Nicht Teil dieses Syncs:
- normaler Projektcode, App-Features, Projektlogik
- Codex schreibt NIEMALS in Claude-Code-Verzeichnisse

## Pflichtablauf

1. `claude-delta-state.json` lesen, um den letzten geprüften Commit zu ermitteln.
2. `git log --oneline <last_commit>..HEAD -- claude-code-setup/ CLAUDE.md` ausführen.
3. Geänderte Dateien lesen und nur umgebungsbezogene Änderungen betrachten.
4. Claude-Umgebungsfixes und Intelligenzvorschläge auf portable Muster prüfen.
5. Port-Kandidaten für Codex als `ADD`, `ADAPT` oder `REPLACE` klassifizieren.
6. Additive Kandidaten Codex-native integrieren; REPLACE-Fälle vorher ausdrücklich bestätigen lassen.

## Sicherheit

- Claude-Dateien sind für Codex read-only.
- Codex-Ziel ist additive Integration, nicht blindes Spiegeln.
- Portierte Regeln gelten erst nach 5 realen Anwendungen als robust.
