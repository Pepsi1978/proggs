# Gemini-Delta-Sync fuer Codex

Diese Regel steuert unter der `## Oberste Direktive`, wie Codex sinnvolle Verbesserungen aus Gemini CLI fuer die eigene Programmierumgebung abfaengt.

## Scope

Erlaubte Vergleichsquellen:

- `Gemini-Setup/**`
- `Gemini-Setup/agent-memory/shared/MEMORY.md`
- `C:\Users\barwa\GeminiCLI\agent-memory\shared\MEMORY.md`

Nicht Teil dieses Syncs:

- normaler Projektcode
- App-Features oder Projektlogik
- Schreibzugriffe in Gemini-Arbeitsordner oder Gemini-Memory

Gemini-Dateien sind fuer Codex bei diesem Prozess strikt read-only.

## Pflichtablauf

1. `codex-setup/scripts/audit-gemini-delta.*` ausfuehren
2. nur umgebungsbezogene Aenderungen betrachten
3. neue Regeln, Prozesse, Memory-Muster, umgebungsbezogene Fehlerfixes, Haertungslogik und vorhandene Ledger fuer umgesetzte Intelligenzvorschlaege erfassen
4. Port-Kandidaten fuer Codex als `ADD`, `ADAPT` oder `REPLACE` klassifizieren
5. nur in `codex-setup/`, `AGENTS.md` oder Codex-Validierung weiterarbeiten

## Bedeutungen

- `ADD`: neue Idee, die Codex bisher nicht hat und additiv uebernommen werden kann
- `ADAPT`: Idee ist sinnvoll, muss aber fuer Codex uebersetzt oder strukturell angepasst werden
- `REPLACE`: eine bestehende Codex-Regel, ein Codex-Prompt oder ein Codex-Setup-Verhalten wuerde ersetzt

## Warnpflicht bei Ersetzungen

- Wenn `REPLACE` sichtbar wird, muss Codex vorher warnen.
- Ohne ausdrueckliche Benutzerfreigabe darf Codex bestehende Codex-Regeln oder bestehendes Codex-Setup-Verhalten nicht durch Gemini-Text ersetzen.
- Wenn alte und neue Logik beide nuetzlich wirken, soll Codex additive Integration bevorzugen.

## Vorschlagspflicht

- Ergebnisse aus dieser Bruecke sind immer nur Vorschlaege.
- Auch `ADD` und `ADAPT` duerfen nicht autonom umgesetzt werden.
- Codex muss erst eine gruppierte A1/B1/C1/D1-Liste mit kurzer Erklaerung erzeugen.
- Erst nach ausdruecklicher Benutzerfreigabe wie `A1 umsetzen`, `B2 umsetzen` oder `alles umsetzen` darf Codex die Vorschlaege in `codex-setup/` portieren.

## Bugfix- und Memory-Logik

- Wenn Gemini CLI einen Fehler im Setup, in Regeln, in seinem Whiteboard-Memory, in Validierung oder Runtime-Glue robust beschreibt oder behebt, soll Codex diesen Fix als Port-Kandidaten mit erfassen.
- Gemeint sind nicht Projekt-Bugfixes, sondern nur Verbesserungen der Programmierumgebung.
- Solche Kandidaten sollen im Delta-Bericht sichtbar als Bugfix- oder Haertungssignal markiert werden.

## Ledger fuer umgesetzte Intelligenzvorschlaege

- Falls Gemini CLI spaeter einen eigenen Ledger fuer umgesetzte `Intelligenzvorschlaege` veroeffentlicht, soll Codex diesen Ledger read-only als weitere Delta-Quelle behandeln.
- Die erwarteten Zieladressen fuer Repo und lokalen Gemini-Workspace werden in `codex-setup/bridges/gemini-cli-delta-bridge.*` und `codex-setup/bridges/intelligence-suggestion-exchange-bridge.*` vorgemerkt.
- Die Existenz oder Nichtexistenz dieser Quelle aendert nichts an der Proposal-only-Regel.

## State und Nachverfolgung

- Der Audit nutzt `codex-setup/state/gemini-delta-state.json` als Codex-eigenen Merker fuer zuletzt gepruefte Gemini-Setup-Commits.
- Dieser State darf nur Codex-relevante Metadaten fuer Setup-Deltas enthalten.
- Er ist keine operative Memory-Quelle; die operative Memory-Quelle bleibt das Codex-Whiteboard.

## Wiederverwendbare Bruecke

- Die von anderen CLIs lesbare Bruecken-Spezifikation liegt unter `codex-setup/bridges/gemini-cli-delta-bridge.*`.
- Die generische Exchange-Bruecke fuer bereits umgesetzte `Intelligenzvorschlaege` liegt unter `codex-setup/bridges/intelligence-suggestion-exchange-bridge.*`.

## Ziel

Codex soll von Gemini-Umgebungsverbesserungen profitieren, ohne dabei seine eigene Arbeitslogik oder gute bestehende Intelligenz zu verlieren.
