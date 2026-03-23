# Deutsches Trigger-Routing fuer Codex

Dieses Routing gilt unter der `## Oberste Direktive` fuer Codex-Setup-, Skill- und Umgebungsfragen in diesem Workspace.
Es soll nur auf Faehigkeiten zeigen, die Codex hier wirklich besitzt.

## Grundregel

- Wenn der Benutzer `Cloud Code` sagt, ist fuer Codex damit `Claude Code` gemeint.
- `claude-code-setup/` und `CLAUDE.md` sind dabei nur Lese- und Vergleichsquellen.
- `claude-code-setup/agent-memory/shared/MEMORY.md` ist die aktuell vorgemerkte read-only Cloud-Code-Fehlerquelle.
- `Gemini-Setup/`, `Gemini-Setup/agent-memory/shared/MEMORY.md` und `C:\Users\barwa\GeminiCLI` sind fuer Codex ebenfalls nur Lesequellen.
- `C:\Users\barwa\GeminiCLI\agent-memory\shared\MEMORY.md` ist die aktuell vorgemerkte read-only Gemini-Fehlerquelle.
- Schreibarbeit passiert nur in `codex-setup/`, `AGENTS.md` und bei Bedarf in der Codex-Validierung.

## Triggergruppen

### Codex Self-Improve

- "verbessere dich"
- "optimiere deine Umgebung"
- "check dein Setup"
- "/self-improve"
- "mach dich schlauer"

Aktion:
- Codex-Self-Improve aus `codex-setup/skills/self-improve/` verwenden
- Whiteboard lesen
- `## Oberste Direktive` und `self-observation` aktiv halten

### Claude-/Cloud-Code-Delta

- "was ist neuer als bei dir"
- "uebernimm das von Cloud Code"
- "Starte bitte die Bruecke zu Cloud Code"
- "Claude Setup vergleichen"
- "schau im Cloud Code Repo nach"
- "welche Regeln sind dort neuer"
- "hol dir die neuesten Informationen von Cloud Code"
- "was wurde bei Cloud Code erneuert"
- "neueste Regeln von Cloud Code"
- "neueste Agenten von Cloud Code"
- "neueste Arbeitsprozesse von Cloud Code"
- "welche Fehler wurden bei Cloud Code gefixt"
- "welche robuste Fixlogik gibt es bei Cloud Code"
- "welche Haertungen sind bei Cloud Code neu"
- "was davon ist fuer Codex sinnvoll"

Aktion:
- zuerst `codex-setup/scripts/audit-claude-delta.*` ausfuehren
- dabei Regeln, Agents, Skills, Hooks, Arbeitsprozesse, Trigger, Programmierumgebung und umgebungsbezogene Fehlerfixes auswerten
- danach Port-Kandidaten als `ADD`, `ADAPT` oder `REPLACE` erklaeren
- bei `REPLACE` vor Umsetzung warnen und bestaetigen lassen
- der Satz `Starte bitte die Bruecke zu Cloud Code` ist ein direkter Ausloeser fuer diesen kompletten Ablauf

### Umgebungsfixe und Fix-Log

- "logge diesen Umgebungsfix"
- "welche Fehler hast du in deiner Umgebung gefixt"
- "warum hast du diesen Umgebungsfehler gefixt"
- "was kann Cloud Code von deinen Fixes lernen"
- "was kann Gemini CLI von deinen Fixes lernen"

Aktion:
- `codex-setup/state/environment-fixes.json` lesen oder aktualisieren
- zum Schreiben `codex-setup/scripts/register-environment-fix.*` verwenden
- nur Regeln, Runtime, MCP, Validierung, Skills, Agenten, Hooks oder Setup loggen, keinen Projektcode
- beim Schreiben immer Kontext, Symptom, Ursache, Verifikation und Portierungshinweis mit erfassen

### OpenAI- und Codex-Doku

- "OpenAI API"
- "welches Modell"
- "Codex Doku"
- "offizielle Doku"

Aktion:
- `openaiDeveloperDocs` bevorzugen
- nur offizielle OpenAI-Dokumentation als Primaerquelle

### Skill-Erstellung

- "neuer Skill"
- "Skill bauen"
- "Skill erstellen"
- "Skill verbessern"

Aktion:
- den Skill `skill-creator` verwenden

### Skill-Installation

- "Skill installieren"
- "welche Skills gibt es"
- "installiere einen Skill"

Aktion:
- den Skill `skill-installer` verwenden

### Semantische Suche und MCP-Status

- "code-search"
- "Indexierung"
- "semantische Suche"
- "Hintergrund-Reindex"

Aktion:
- `codex-setup/scripts/check-code-search-health.*` bevorzugen
- nicht nur aus Erinnerung antworten

### Regeln, Memory und Whiteboard

- "welche Regeln gelten"
- "was hast du gelernt"
- "trag das fuer spaeter ein"
- "was steht in deinem Whiteboard"

Aktion:
- Codex-Whiteboard und `codex-setup/rules/*.md` konsultieren
- Whiteboard nur ueber `whiteboard-insert.*` oder `writeback-enforcer.*` beschreiben

## Portierungs-Sicherheitsregel

- Wenn ein Claude-Delta nur zusaetzliche Intelligenz bringt, bevorzuge `ADD` oder additive `ADAPT`.
- Wenn bestehende Codex-Regeln oder Codex-Setup-Texte wirklich ersetzt werden muessten, markiere das als `REPLACE`.
- `REPLACE` braucht eine klare Warnung vor der Umsetzung.
