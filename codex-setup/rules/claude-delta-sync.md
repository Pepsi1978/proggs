# Claude-Delta-Sync fuer Codex

Diese Regel steuert unter der `## Oberste Direktive`, wie Codex sinnvolle Verbesserungen aus Claude Code fuer die eigene Programmierumgebung abfaengt.

## Scope

Erlaubte Vergleichsquellen:

- `CLAUDE.md`
- `claude-code-setup/**`

Nicht Teil dieses Syncs:

- normaler Projektcode
- App-Features oder Projektlogik
- Schreibzugriffe in Claude-Arbeitsordner, Claude-Setup oder Claude-Whiteboards

Claude-Dateien sind fuer Codex bei diesem Prozess strikt read-only.

## Pflichtablauf

1. `codex-setup/scripts/audit-claude-delta.*` ausfuehren
2. nur umgebungsbezogene Aenderungen betrachten
3. neue Regeln, Agenten, Prozesse und auch umgebungsbezogene Fehlerfixes oder Haertungslogik erfassen
4. Port-Kandidaten fuer Codex als `ADD`, `ADAPT` oder `REPLACE` klassifizieren
5. nur in `codex-setup/`, `AGENTS.md` oder Codex-Validierung weiterarbeiten

## Bedeutungen

- `ADD`: neue Idee, die Codex bisher nicht hat und additiv uebernommen werden kann
- `ADAPT`: Idee ist sinnvoll, muss aber fuer Codex uebersetzt oder strukturell angepasst werden
- `REPLACE`: eine bestehende Codex-Regel, ein Codex-Prompt oder ein Codex-Setup-Verhalten wuerde ersetzt

## Warnpflicht bei Ersetzungen

- Wenn `REPLACE` sichtbar wird, muss Codex vorher warnen.
- Ohne ausdrueckliche Benutzerfreigabe darf Codex bestehende Codex-Regeln oder bestehendes Codex-Setup-Verhalten nicht durch Claude-Text ersetzen.
- Wenn alte und neue Regel nicht identisch sind, aber beide nuetzlich wirken, soll Codex additive Integration bevorzugen.

## Bewaehrungsphase

- Portierte Regeln und Portierungslogiken gelten erst nach 5 realen Anwendungen als robust.
- Vorher sollen sie weiter beobachtet und bei Konflikten eher erweitert als ersetzt werden.

## Bugfix- und Haertungslogik

- Wenn Claude Code einen Fehler im Setup, in Regeln, in Hooks, in Agenten, Skills, Validierung oder Runtime-Glue robust behebt, soll Codex diesen Fix als Port-Kandidaten mit erfassen.
- Gemeint sind nicht Projekt-Bugfixes, sondern nur Verbesserungen der Programmierumgebung.
- Solche Kandidaten sollen im Delta-Bericht sichtbar als Bugfix- oder Haertungssignal markiert werden.
- Ziel ist, dass Codex von bereits bewaehrten Cloud-Code-Fixes profitiert und denselben Fehler moeglichst nicht erneut selbst lernen muss.

## State und Nachverfolgung

- Der Audit nutzt `codex-setup/state/claude-delta-state.json` als Codex-eigenen Merker fuer zuletzt gepruefte Claude-Setup-Commits.
- Dieser State darf nur Codex-relevante Metadaten fuer Setup-Deltas enthalten.
- Er ist keine operative Memory-Quelle; die operative Memory-Quelle bleibt das Codex-Whiteboard.

## Wiederverwendbare Bruecke

- Die generische, von anderen CLIs lesbare Bruecken-Spezifikation liegt unter `codex-setup/bridges/cloud-code-delta-bridge.*`.
- Andere CLI-Umgebungen sollen daraus dieselbe Klassifikation, dieselbe Warnlogik und dieselbe deutsche Berichtstruktur uebernehmen, aber nur in ihre eigenen Ziel-Workspaces schreiben.

## Ziel

Codex soll von Claude-Umgebungsverbesserungen profitieren, ohne dabei seine eigene Arbeitslogik, seine Whiteboard-Trennung oder gute bestehende Intelligenz zu verlieren.
