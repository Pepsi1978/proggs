# Codex Setup

`codex-setup/` ist die plattformuebergreifende Source-of-Truth fuer Codex in diesem Repository.

Mission V1.0.0:
- Codex in diesem Workspace unabhaengig von Claude/proggs machen
- ein eigenes Codex-Gedaechtnis und eigene Skills/Agents etablieren
- die lokale Codex-Programmierumgebung schrittweise zur intelligentesten Version ihrer selbst ausbauen
- oberstes Ziel: Codex in diesem Workspace zur intelligentesten Programmierumgebung machen
- jede Session fuer kumulative, moeglichst exponentielle Intelligenzsteigerung nutzen

Grundsaetze:
- nur fuer Codex
- nicht fuer Claude Code
- nicht fuer `proggs`
- synchron zwischen macOS und Windows
- lokal im `Codex`-Workspace autoritativ

Wichtigste Trennung:
- `codex-setup/` ist fuer Codex die massgebliche Regel-, Skill- und Whiteboard-Basis.
- `claude-code-setup/` ist Projektinhalt, aber keine Regelquelle fuer Codex.
- Das Whiteboard von Claude/proggs darf von Codex nicht als operative Wissensquelle genutzt werden.

## Struktur

- `rules/global.md`
  Globale, plattformuebergreifende Codex-Regeln.

- `agent-memory/shared/MEMORY.md`
  Das eigene Codex-Whiteboard. Nur dieses Whiteboard ist fuer Codex autoritativ.
  Die Sektion `## Oberste Direktive` ist dabei das oberste Zielsystem fuer alle angebundenen Komponenten.

- `scripts/`
  Plattformuebergreifende Hilfsskripte fuer Whiteboard-Resolver, Whiteboard-Insert, Sentinel-Merge,
  Skill-Installation, Validierung und `code-search`-Healthchecks.

- `skills/self-improve/`
  Der Codex-spezifische Self-Improve-Skill fuer diesen Workspace, inklusive Referenzen,
  Researcher-Prompts und eigenen Codex-Agenten.

## Whiteboard-Architektur

Autoritatives Whiteboard:
- macOS: `/Users/frank/Codex/codex-setup/agent-memory/shared/MEMORY.md`
- Windows: `C:\Users\barwa\Codex\codex-setup\agent-memory\shared\MEMORY.md`

Whiteboard-Regeln:
- Codex liest und schreibt nur dieses Whiteboard.
- `~/proggs/.../MEMORY.md` ist fuer Claude Code reserviert und fuer Codex tabu.
- Angebundene Skills, Agenten, Researcher, MCP-auswertende Workflows und Hilfsskripte sollen vor ihrer eigentlichen Arbeit
  zuerst die Whiteboard-Sektion `## Oberste Direktive` mitlesen und ihr folgen.
- Whiteboard-Schreibzugriffe laufen sektionsbasiert ueber `codex-setup/scripts/whiteboard-insert.*`
  oder `codex-setup/scripts/writeback-enforcer.*`.
- Agents/Researcher schreiben Sentinel-JSON-Dateien; die Merge-Logik fuehrt sie in das Codex-Whiteboard zusammen.
- MCP-Server schreiben nicht direkt ins Whiteboard. Ihr Ergebnis wird vom koordinierenden Skill oder Agenten
  zusammengefasst und dann via Whiteboard-Bruecke eingetragen.

## Deployment in die lokale Codex-Umgebung

Der Skill lebt im Repo, wird aber lokal nach `~/.codex/skills/self-improve/` deployed.

Installation:
- macOS/Linux: `bash codex-setup/scripts/install-self-improve.sh`
- Windows: `pwsh -File codex-setup/scripts/install-self-improve.ps1`

## Git-Regel fuer diesen Ordner

- `codex-setup/` ist der einzige Ordner im Repository, den Codex nach erfolgreicher lokaler Validierung eigenstaendig committen und nach `origin/main` pushen soll.
- Wenn eine solche Synchronisation zusaetzlich `AGENTS.md` oder `.github/workflows/codex-setup-validate.yml` braucht, duerfen diese Dateien im selben Sync-Commit mitgefuehrt werden.
- Fuer alle anderen Ordner gilt weiter: `git push` nur nach ausdruecklicher Benutzeranweisung.

## GitHub-Validierung

Der Ordner ist so ausgelegt, dass er separat auf GitHub geprueft werden kann:
- keine operative Abhaengigkeit von `proggs`
- eigene Whiteboard-Datei
- eigene Skills und Agenten
- eigene Bruecken- und Validierungsskripte

## code-search-Healthcheck

Fuer die semantische Suche gibt es einen plattformuebergreifenden Healthcheck:

- macOS/Linux: `bash codex-setup/scripts/check-code-search-health.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/check-code-search-health.ps1`

Der Report zeigt in einem Schritt:

- frischen `search_status`-Smoke-Test in einer neuen `codex exec`-Session
- frische Testquery gegen den `code-search`-MCP
- aktive DB, Dateianzahl und Chunk-Anzahl
- letzten Laufmodus und den Zeitpunkt des letzten erfolgreichen Laufs
- zusaetzlich `Last write mode` und `Last write at`, damit `noop`-Runs von echten Schreiblaeufen getrennt sichtbar sind
- explizit, ob der letzte Lauf und der letzte Schreiblauf erfolgreich bzw. inkrementell waren
