# Codex Delta Bridge

Pruefe was Codex CLI seit dem letzten Checkpoint verbessert hat und portiere sinnvolle
Aenderungen in die Claude Code Umgebung.

## Ablauf

1. Lies `~/.claude/state/codex-delta-state.json` fuer den letzten geprueften Commit
2. Fuehre aus: `cd ~/proggs && git log --oneline <last_commit>..HEAD -- codex-setup/ AGENTS.md`
3. Wenn keine neuen Commits: "Kein Delta seit letztem Check." melden und beenden
4. Geaenderte Dateien lesen — NUR umgebungsbezogene Aenderungen betrachten (Regeln, Agents, Scripts, Fixes)
5. `codex-setup/state/environment-fixes.json` auf neue Eintraege pruefen
6. `codex-setup/agent-memory/shared/MEMORY.md` auf neue Erkenntnisse pruefen
7. Jeden Kandidaten klassifizieren und als strukturierte Liste praesentieren

## Klassifikation

- `ADD`: Neue Idee die Claude Code bisher nicht hat → als Empfehlung vorschlagen
- `ADAPT`: Sinnvoll, muss aber fuer Claude Code angepasst werden → Portierung erklaeren
- `REPLACE`: Wuerde Bestehendes ersetzen → VOR Umsetzung Freigabe holen

## Ausgabeformat (deutsch, nach Gruppen sortiert)

- `A1-An`: Regeln und Prompts
- `B1-Bn`: Agents, Skills und Arbeitsprozesse
- `C1-Cn`: Skripte, Hooks und Validierung
- `D1-Dn`: Runtime und Konfiguration
- `E1-En`: Fehlerfixes

Pro Punkt: Klasse, Quelle, Ziel, Grund, Empfehlung, Bugfix-Signal (ja/nein)

## Sicherheitsregeln

- NUR READ-ONLY Zugriff auf codex-setup/ — NIEMALS dort schreiben
- Portierte Regeln gelten erst nach 5 Anwendungen als robust
- Nach Benutzer-Approval: `codex-delta-state.json` und `claude-code-setup/state/codex-delta-state.json` aktualisieren
