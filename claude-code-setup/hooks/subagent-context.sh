#!/bin/bash
# SubagentStart Hook: Inject global project rules into every subagent
# This ensures all subagents know the core rules even without conversation history
#
# DESIGN: Output MUST be valid JSON on stdout. Any error → empty output + exit 0.

cat <<'CONTEXT'
{
  "additionalContext": "## Globale Projekt-Regeln (automatisch injiziert)\n\n### Repository\n- EINZIGES Repo: Pepsi1978/proggs — NIEMALS neue Repos erstellen\n- Lokaler Pfad: ~/proggs/ — GitHub: https://github.com/Pepsi1978/proggs\n- Jeder Push geht ausschliesslich nach Pepsi1978/proggs\n\n### Sprachen & Plattformen\n- Bevorzugt: Swift, C#, Kotlin, TypeScript, Rust, Go (in dieser Reihenfolge)\n- KEIN Python fuer GUIs oder Desktop-Apps\n- Cross-Platform: macOS (Swift/AppKit) + Windows (C#/WPF) + Android (Kotlin/Compose)\n\n### Commits & Kommunikation\n- Commit-Format: #NNN - Beschreibung (fortlaufend nummeriert, Englisch)\n- Kommunikation mit Benutzer auf Deutsch, Code auf Englisch\n- Committen und Pushen immer direkt — nicht vorher fragen\n- Letzter Satz IMMER: 'Committed und gepusht.' / 'Committed.' / 'Ich habe weder committed noch gepusht.'\n\n### Qualitaet\n- UI muss aussehen wie gekaufte Software — poliert, professionell\n- Nach jedem Feature: quality-gate Agent starten (tester + code-reviewer + optimizer)\n- Bei Fehlern selbststaendig debuggen und fixen"
}
CONTEXT
exit 0
