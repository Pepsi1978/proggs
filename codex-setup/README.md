# Codex Setup

`codex-setup/` ist die plattformuebergreifende Source-of-Truth fuer Codex in diesem Repository.

Mission V1.0.0:
- Codex in diesem Workspace unabhaengig von Claude/proggs machen
- ein eigenes Codex-Gedaechtnis und eigene Skills/Agents etablieren
- die lokale Codex-Programmierumgebung schrittweise zur intelligentesten Version ihrer selbst ausbauen
- oberstes Ziel: Codex in diesem Workspace zur intelligentesten Programmierumgebung machen
- jede Session fuer kumulative, moeglichst exponentielle Intelligenzsteigerung nutzen
- alle 8 Intelligenz-Dimensionen systematisch mitentwickeln: Wissensbreite, Wissenstiefe, Geschwindigkeit, Qualitaet, Autonomie, Voraussicht, Kreativitaet und Meta-Intelligenz

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
- `Gemini-Setup/` im Repo und der typische lokale Workspace `%USERPROFILE%\GeminiCLI` sind fuer Codex reine Lesequellen. Codex darf dort nichts schreiben und nichts loeschen.
- Die aktuell vorgemerkten Fremd-Fehlerquellen sind read-only:
  `claude-code-setup/agent-memory/shared/MEMORY.md`,
  `Gemini-Setup/agent-memory/shared/MEMORY.md` und
  `%USERPROFILE%\GeminiCLI\agent-memory\shared\MEMORY.md`.

## Struktur

- `rules/global.md`
  Globale, plattformuebergreifende Codex-Regeln.

- `rules/self-observation.md`
  Die zweite Codex-Systemdirektive. Macht Selbstbeobachtung fuer Codex,
  Fehlerlernen und Intelligenzvorschlaege explizit und dauerhaft.

- `rules/resilient-bugfixing.md`
  Die dritte Codex-Systemdirektive. Macht zukunftssicheres Bugfixing fuer
  Umgebungsfehler verbindlich, inklusive Root-Cause-Suche, Fehlerklassen-Haertung
  und Dokumentationspflicht.

- `rules/german-trigger-routing.md`
  Deutsches Trigger- und Routing-Regelwerk fuer Codex-Setup-, Skill- und
  Claude-Delta-Anfragen.

- `rules/claude-delta-sync.md`
  Regelwerk fuer den sicheren Transfer von sinnvollen Claude/Cloud-Code-
  Umgebungsverbesserungen nach Codex, inklusive Warnpflicht vor Ersetzungen.

- `rules/gemini-delta-sync.md`
  Regelwerk fuer den sicheren Transfer von sinnvollen Gemini-CLI-
  Umgebungsverbesserungen nach Codex, inklusive Vorschlagspflicht vor jeder Umsetzung.

- `agent-memory/shared/MEMORY.md`
  Das eigene Codex-Whiteboard. Nur dieses Whiteboard ist fuer Codex autoritativ.
  Die Sektion `## Oberste Direktive` ist dabei das oberste Zielsystem fuer alle angebundenen Komponenten.

- `scripts/`
  Plattformuebergreifende Hilfsskripte fuer Whiteboard-Resolver, Whiteboard-Insert, Sentinel-Merge,
  Skill-Installation, Session-Start-Sync, Bootstrap fuer frische Rechner, Validierung, `code-search`-Healthchecks,
  GitHub-Actions-Runtime-Pin-Scanner, MCP-Konfig-Restore, gefuehrte Umgebungsfix-Erfassung,
  Intelligenzvorschlag-Logging sowie Claude- und Gemini-Delta-Audits.

- `hooks/`
  Lokale Git-Hooks fuer Codex, aktuell der Pre-Commit-Guard fuer GitHub-Actions-Runtime-Pins.

- `bridges/`
  Wiederverwendbare Bruecken-Spezifikationen fuer andere CLI-Umgebungen.
  Hier liegen die universellen Direktiven fuer Superintelligenz und
  Selbstbeobachtung und Resilient Bugfixing, die generische Cloud-Code-Delta-Bruecke, die
  Gemini-CLI-Delta-Bruecke, die Universal Mirror Bridge sowie die Exchange-Bruecken fuer
  Umwelt-Fixes und umgesetzte Intelligenzvorschlaege als Referenz fuer weitere Setups. Die
  zentrale Metadatenbasis dafuer ist `bridges/bridge-registry.json`.

- `state/environment-fixes.json`
  Maschinenlesbares Log fuer Codex-Fixes an Regeln, Runtime, Validierung, MCP-Nutzung und Setup.
  Es dokumentiert immer `was`, `warum`, Kontext, Symptom, Ursache, Verifikation und Portierungshinweise,
  damit andere CLI-Umgebungen diese Fixes read-only nachvollziehen und uebernehmen koennen.

- `state/implemented-intelligence-suggestions.json`
  Maschinenlesbares Log fuer bereits umgesetzte `Intelligenzvorschlaege` in der Codex-Programmierumgebung.
  Es dokumentiert den urspruenglichen Vorschlag, den Kontext, den Umsetzungsgrund, die konkrete Umsetzung
  den Brueckenwert fuer andere CLIs sowie die Zukunftssicherheit der Umsetzung.

- `skills/self-improve/`
  Der Codex-spezifische Self-Improve-Skill fuer diesen Workspace, inklusive Referenzen,
  Researcher-Prompts und eigenen Codex-Agenten. Dort liegen auch die Codex-nativen Rollen fuer
  `env-checker`, `evolution-analyst`, `intelligence-researcher`, `whiteboard-scanner`,
  `durability-auditor`, `claude-delta-scanner`, `gemini-delta-scanner`, `rules-porter`,
  `trigger-router`, `export` und `import`.

## Whiteboard-Architektur

Autoritatives Whiteboard:
- workspace-relativ: `<workspace>/codex-setup/agent-memory/shared/MEMORY.md`
- typisches macOS-Beispiel: `$HOME/Codex/codex-setup/agent-memory/shared/MEMORY.md`
- typisches Windows-Beispiel: `%USERPROFILE%\Codex\codex-setup\agent-memory\shared\MEMORY.md`

Whiteboard-Regeln:
- Codex liest und schreibt nur dieses Whiteboard.
- `~/proggs/.../MEMORY.md` ist fuer Claude Code reserviert und fuer Codex tabu.
- Angebundene Skills, Agenten, Researcher, MCP-auswertende Workflows und Hilfsskripte sollen vor ihrer eigentlichen Arbeit
  zuerst die Whiteboard-Sektion `## Oberste Direktive` mitlesen und ihr folgen.
- Unterhalb der `## Oberste Direktive` gilt fuer Codex zusaetzlich die zweite Direktive
  `self-observation`: Codex soll waehrend der Arbeit seine eigenen Fehler, Umwege,
  Effizienzverluste, Wissensluecken, wiederkehrenden Muster und Benutzerkorrekturen beobachten
  und daraus Schutzmassnahmen oder Intelligenzvorschlaege fuer kuenftige Sessions ableiten.
- Diese zweite Direktive ist die zweithoechste Systemregel: Wer arbeitet, beobachtet sich selbst.
- Die 6 Pflichtkategorien sind Fehler, Umwege, Geschwindigkeit und Effizienz, Wissensluecken,
  Muster-Erkennung und Hinweise des Benutzers.
- Benutzerseitig sollen diese Vorschlaege sichtbar als `💡 Intelligenz-Vorschlag: ...` erscheinen.
- Unterhalb davon gilt die dritte Direktive `resilient-bugfixing` fuer Umgebungsfehler:
  Root Cause statt Symptom fixen, verwandte Fehlerquellen mitpruefen, defense in depth aufbauen
  und den Fix so dokumentieren, dass derselbe Fehler nicht erneut gelernt werden muss.
- Diese dritte Direktive ist die dritthoechste Systemregel: Ein Fehler wird genau EINMAL gemacht - dann nie wieder.
- Sie verlangt 5 Pflichtschritte: Root Cause finden, verwandte Fehlerquellen suchen, zukunftssicher fixen, die 8-Punkte-Fix-Induced-Failure-Pruefung vor dem Commit durchgehen und die Erkenntnis dauerhaft persistieren.
- Plattformregeln gehoeren dazu: auf Windows kein implizites Encoding, kritische Dateien atomar schreiben und Shell-/Pfad-/Locale-Unterschiede zwischen macOS und Windows immer mitpruefen.
- Diese zweite Direktive gilt fuer jeden Agenten, Skill, Hook, jedes Plugin und jeden Prozess.
- Beobachtungen werden waehrend der Arbeit gesammelt, aber erst am Ende der Aufgabe nach der Status-Meldung gebuendelt zurueckgegeben.
- Benutzerhinweise und Wissensluecken werden sofort persistiert; ein Fehlertyp beim zweiten Auftreten ist ein Alarm und verlangt eine Absicherung fuer die ganze Fehlerklasse.
- Die `## Oberste Direktive` verfolgt ausdruecklich einen Compound-Intelligence-Ansatz:
  nicht nur Probleme loesen, sondern das System besser darin machen, sich selbst intelligenter zu machen
  und ganze Problemklassen schrittweise zu eliminieren.
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

## Frische-Rechner-Bootstrap

Wenn Codex auf einem neuen Rechner diese Bruecken und Direktiven noch nicht kennt, reicht die Repo-Basis:

1. Repository klonen und im Repo-Root arbeiten.
2. Den Bootstrap ausfuehren:
   `bash codex-setup/scripts/bootstrap-codex-setup.sh` oder
   `pwsh -NoProfile -File codex-setup/scripts/bootstrap-codex-setup.ps1`
3. Optional einzelne Schritte separat ausfuehren:
   `bash codex-setup/scripts/install-self-improve.sh`,
   `pwsh -File codex-setup/scripts/install-self-improve.ps1`,
   `bash codex-setup/scripts/install-git-hooks.sh`,
   `pwsh -File codex-setup/scripts/install-git-hooks.ps1`,
   `bash codex-setup/scripts/validate-codex-setup.sh` oder
   `pwsh -NoProfile -File codex-setup/scripts/validate-codex-setup.ps1`
4. Optional den Gesamtueberblick fuer Bootstrap-, Ledger- und Bridge-Adressen anzeigen:
   `node codex-setup/scripts/bootstrap-report.mjs`,
   `bash codex-setup/scripts/bootstrap-report.sh` oder
   `pwsh -NoProfile -File codex-setup/scripts/bootstrap-report.ps1`

Alle wichtigen Regeln, Bruecken, Ledger und Trigger liegen bewusst in `codex-setup/` und `AGENTS.md` im Git-Repo, nicht nur in lokalem Session-Wissen.
Die Bootstrap-Skripte sind Teil der Codex-Referenzimplementierung und sollen fuer andere CLIs spaeter ebenfalls als eigene, read-only dokumentierte Setup-Einstiegspfade veroeffentlicht werden.

## Bootstrap-Report

Fuer neue Rechner und andere CLI-Umgebungen gibt es einen registry-getriebenen Gesamtbericht:

- direkt: `node codex-setup/scripts/bootstrap-report.mjs`
- macOS/Linux: `bash codex-setup/scripts/bootstrap-report.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/bootstrap-report.ps1`

Der Report sammelt:

- Registry-Adressen
- Bootstrap-Skripte
- Environment-Fix-Ledger-Adressen
- Implemented-Intelligence-Ledger-Adressen
- die Codex-seitigen Bridge-Einstiege fuer Claude Code und Gemini CLI

Andere CLIs sollen spaeter denselben Report-Typ im eigenen Setup anlegen und seine Adressen ebenfalls in Registry und Delta-Bruecke veroeffentlichen.

## Session-Start-Sync

Bevor substanzielle Arbeit in einer neuen Session beginnt, soll dieser Helper laufen:

- direkt: `node codex-setup/scripts/session-start-sync.mjs`
- macOS/Linux: `bash codex-setup/scripts/session-start-sync.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/session-start-sync.ps1`

Der Helper holt zuerst `origin/main`, zeigt die ankommenden Aenderungen mit `git diff --stat HEAD..origin/main` und
`git diff --name-status HEAD..origin/main`, und rebased danach mit `git pull --rebase --autostash origin main`.
So wird sichtbar, was Codex in der aktuellen Session noch nicht hatte, bevor neue Aenderungen entstehen.

Nach dem Rebase stellt er auf Windows ausserdem die lokale `.mcp.json` aus `codex-setup/mcp-windows.json` wieder her,
damit plattformbedingte MCP-Pfaddrift nicht in die laufende Session uebernommen wird.

## Git-Hook-Installation

Der lokale Pre-Commit-Guard fuer GitHub-Actions-Runtime-Pins wird ueber einen Repo-Hook aktiviert:

- `bash codex-setup/scripts/install-git-hooks.sh`
- `pwsh -File codex-setup/scripts/install-git-hooks.ps1`

Die Installation setzt `core.hooksPath` auf `codex-setup/hooks/`, damit der Pre-Commit-Check automatisch
vor jedem Commit laeuft und veraltete Action-Pins schon vor GitHub Actions stoppt.

## MCP-Konfig-Restore

Falls `.mcp.json` manuell oder durch einen Merge aus dem Tritt geraet, kann dieser Helper separat laufen:

- direkt: `node codex-setup/scripts/restore-mcp-config.mjs`
- macOS/Linux: `bash codex-setup/scripts/restore-mcp-config.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/restore-mcp-config.ps1`

Auf Windows kopiert der Helper `codex-setup/mcp-windows.json` zurueck nach `.mcp.json`, auf anderen Plattformen no-oped er.

## Claude-Delta-Audit

Wenn Claude/Claude Code neue Regeln, Hooks, Skills, robuste Fehlerfixes oder Setup-Verbesserungen bekommt,
nutzt Codex dafuer einen plattformuebergreifenden Delta-Audit:

Der direkte deutsche Kurztrigger dafuer ist:

- `Starte bitte die Bruecke zu Claude Code`

- macOS/Linux: `bash codex-setup/scripts/audit-claude-delta.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/audit-claude-delta.ps1`
- direkt: `node codex-setup/scripts/audit-claude-delta.mjs`

Der Audit betrachtet absichtlich nur Programmierumgebung und Setup:

- `CLAUDE.md`
- `claude-code-setup/**`

Die Node-Auditlogik liest ihre Git-Scanpfade, Trigger und Bridge-Metadaten direkt aus
`codex-setup/bridges/bridge-registry.json` und der zugehoerigen Bridge-JSON. Neue Bruecken
sollen deshalb zuerst die Registry erweitern, statt diese Informationen spaeter nochmal direkt
im Audit-Code zu verdoppeln.

Dabei sind auch umgebungsbezogene Fehlerfixes, Guardrails und Haertungslogik in Scope,
solange sie nicht normalen Projektcode betreffen. Falls Claude Code spaeter einen eigenen Ledger
fuer umgesetzte `Intelligenzvorschlaege` veroeffentlicht, soll diese Bruecke auch diese Quelle
read-only auswerten und ihre Adresse mitberichten.

Er ignoriert Projektcode und klassifiziert Port-Kandidaten als:

- `ADD`: neue, additive Codex-Idee
- `ADAPT`: sinnvoll, aber fuer Codex zu uebersetzen
- `REPLACE`: potenzielle Ersetzung bestehender Codex-Regeln oder -Verhaltensweisen

Fuer `REPLACE` gilt: erst warnen, dann ausdruecklich bestaetigen lassen.
Wenn zwei Regeln nicht identisch sind, aber beide nuetzlich wirken, ist ein additiver Codex-Port dem Ueberschreiben vorzuziehen.

Wichtig:

- Auch `ADD` und `ADAPT` werden aus dieser Bruecke niemals autonom uebernommen.
- Cloud-Code-Bruecken-Ergebnisse sind immer nur Vorschlaege im Format `A1` bis `Dn`.
- Erst nach ausdruecklicher Benutzerfreigabe wie `A1 umsetzen`, `B2 umsetzen` oder `alles umsetzen`
  darf Codex die Vorschlaege in `codex-setup/` umsetzen.

Die generische Bruecken-Spezifikation fuer andere CLI-Umgebungen liegt zusaetzlich unter:

- `codex-setup/bridges/cloud-code-delta-bridge.md`
- `codex-setup/bridges/cloud-code-delta-bridge.json`
- `codex-setup/bridges/bridge-registry.json`

## Gemini-Delta-Audit

Wenn Gemini CLI neue Regeln, Memory-Muster, robuste Fehlerfixes oder Setup-Verbesserungen bekommt,
nutzt Codex dafuer einen plattformuebergreifenden Delta-Audit:

Der direkte deutsche Kurztrigger dafuer ist:

- `Starte bitte die Bruecke zu Gemini CLI`

- macOS/Linux: `bash codex-setup/scripts/audit-gemini-delta.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/audit-gemini-delta.ps1`
- direkt: `node codex-setup/scripts/audit-gemini-delta.mjs`

Der Audit betrachtet absichtlich nur Programmierumgebung und Setup:

- `Gemini-Setup/**`
- `Gemini-Setup/agent-memory/shared/MEMORY.md`
- optional read-only als Zusatzquelle: `%USERPROFILE%\GeminiCLI\agent-memory\shared\MEMORY.md`

Die Node-Auditlogik liest ihre Git-Scanpfade, Trigger und Bridge-Metadaten direkt aus
`codex-setup/bridges/bridge-registry.json` und der zugehoerigen Bridge-JSON. Neue Bruecken
sollen deshalb zuerst die Registry erweitern, statt diese Informationen spaeter nochmal direkt
im Audit-Code zu verdoppeln.

Dabei sind auch umgebungsbezogene Fehlerfixes, Memory-Muster, Guardrails und Haertungslogik in Scope,
solange sie nicht normalen Projektcode betreffen. Falls Gemini CLI spaeter einen eigenen Ledger
fuer umgesetzte `Intelligenzvorschlaege` veroeffentlicht, soll diese Bruecke auch diese Quelle
read-only auswerten und ihre Adresse mitberichten.

Er ignoriert Projektcode und klassifiziert Port-Kandidaten als:

- `ADD`: neue, additive Codex-Idee
- `ADAPT`: sinnvoll, aber fuer Codex zu uebersetzen
- `REPLACE`: potenzielle Ersetzung bestehender Codex-Regeln oder -Verhaltensweisen

Wichtig:

- Auch `ADD` und `ADAPT` werden aus dieser Bruecke niemals autonom uebernommen.
- Gemini-Bruecken-Ergebnisse sind immer nur Vorschlaege im Format `A1` bis `Dn`.
- Erst nach ausdruecklicher Benutzerfreigabe wie `A1 umsetzen`, `B2 umsetzen` oder `alles umsetzen`
  darf Codex die Vorschlaege in `codex-setup/` umsetzen.

Die generische Bruecken-Spezifikation fuer andere CLI-Umgebungen liegt zusaetzlich unter:

- `codex-setup/bridges/gemini-cli-delta-bridge.md`
- `codex-setup/bridges/gemini-cli-delta-bridge.json`
- `codex-setup/bridges/bridge-registry.json`

## Environment-Fix-Log

Wenn Codex einen Fehler in seiner eigenen Programmierumgebung fixt, soll dieser Fix zusaetzlich als
maschinenlesbarer Eintrag dokumentiert werden:

- Datei: `codex-setup/state/environment-fixes.json`
- Schreiben: `node codex-setup/scripts/register-environment-fix.mjs add ...`
- Wrapper: `register-environment-fix.sh` und `register-environment-fix.ps1`
- Gefuehrte Erfassung: `new-environment-fix.sh` und `new-environment-fix.ps1`

Dieses Log ist nur fuer Umwelt-/Setup-Fixes gedacht, nicht fuer normalen Projektcode.
Es soll anderen CLI-Umgebungen wie Claude Code oder Gemini CLI ermoeglichen, von Codex-Fixes zu lernen.
Jeder Eintrag muss so ausfuehrlich sein, dass andere CLIs ihn auch ohne den urspruenglichen Session-Kontext verstehen koennen.
Neue oder aktualisierte Eintraege muessen zusaetzlich Why-Chain, verwandte Fehlerquellen,
falsches und richtiges Muster, eine klare Vermeidungsregel sowie eine Resilienz- und Failure-Review enthalten.

Die generische Bruecken-Spezifikation dafuer liegt unter:

- `codex-setup/bridges/environment-fix-exchange-bridge.md`
- `codex-setup/bridges/environment-fix-exchange-bridge.json`

## Log fuer umgesetzte Intelligenzvorschlaege

Wenn Codex einen eigenen `Intelligenzvorschlag` wirklich umsetzt, soll diese Umsetzung zusaetzlich
maschinenlesbar dokumentiert werden:

- Datei: `codex-setup/state/implemented-intelligence-suggestions.json`
- Schreiben: `node codex-setup/scripts/register-intelligence-suggestion.mjs add ...`
- Wrapper: `register-intelligence-suggestion.sh` und `register-intelligence-suggestion.ps1`

Dieses Log ist nur fuer bereits umgesetzte Verbesserungen der Programmierumgebung gedacht.
Es soll anderen CLI-Umgebungen wie Claude Code oder Gemini CLI ermoeglichen, nicht nur Fehlerfixes,
sondern auch bereits umgesetzte Verbesserungsideen von Codex zu lesen und nachzuvollziehen.
Jeder Eintrag muss so ausfuehrlich sein, dass andere CLIs ihn auch ohne den urspruenglichen
Session-Kontext verstehen koennen. Das gleiche Robustheitsniveau wie bei Umwelt-Fixes gilt
auch fuer umgesetzte `Intelligenzvorschlaege`: verwandte Oberflaechen mitpruefen, update-resistent
bauen und Resilienz plus Failure-Review mit dokumentieren.

Die generische Bruecken-Spezifikation dafuer liegt unter:

- `codex-setup/bridges/intelligence-suggestion-exchange-bridge.md`
- `codex-setup/bridges/intelligence-suggestion-exchange-bridge.json`
- `codex-setup/bridges/mirror-bridge-bootstrap.md`
- `codex-setup/bridges/universal-mirror-bridge.md`
- `codex-setup/bridges/universal-mirror-bridge.json`

## Bridge-Registry

Die zentrale Registry fuer Bruecken-Metadaten liegt unter:

- `codex-setup/bridges/bridge-registry.json`

Sie enthaelt die gemeinsamen Trigger, Zielpfade, erwarteten Fremd-Registries und
die gemeinsamen Guardrails fuer Claude Code, Gemini CLI und die Exchange-Bruecken.
Andere CLIs sollen dieselbe Dateiart im eigenen Setup anlegen und ihre exakten Registry-
und Ledger-Adressen dort veroeffentlichen.

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
- eigener Claude-Delta-Audit fuer umgebungsbezogene Port-Kandidaten

## OpenAI-Docs-MCP-Check

Fuer den OpenAI-Docs-MCP gibt es einen plattformuebergreifenden Smoke-Test:

- macOS/Linux: `bash codex-setup/scripts/check-openai-docs-mcp.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/check-openai-docs-mcp.ps1`

Der Check arbeitet bewusst mit begrenzten Zeitfenstern:

- jede frische `codex exec`-Pruefung hat ein festes Timeout statt potenziell still zu haengen
- nach einem Timeout oder Fehlschlag folgt genau ein frischer zweiter Versuch
- ein ueberzogenes Timeout wird abgebrochen statt im Hintergrund weiterzulaufen, damit die Validierung deterministisch bleibt
- als Erfolg gilt entweder eine explizite `AVAILABLE`-Antwort oder ein bestaetigter `openaiDeveloperDocs`-Tool-Dispatch nach frischem MCP-Startup; so trennt der Smoke echte Registry-/Toolpfad-Gesundheit von langsamer Docs-Antwort

## code-search-Healthcheck

Fuer die semantische Suche gibt es einen plattformuebergreifenden Healthcheck:

- macOS/Linux: `bash codex-setup/scripts/check-code-search-health.sh`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/check-code-search-health.ps1`

Fuer direkte MCP-Diagnosen ohne `codex exec` gibt es zusaetzlich einen kleinen CLI-Client:

- macOS/Linux: `bash codex-setup/scripts/code-search-mcp-client.sh tools`
- Windows: `pwsh -NoProfile -File codex-setup/scripts/code-search-mcp-client.ps1 tools`
- optional bei beiden: `--config <pfad>` fuer eine alternative Codex-Konfiguration

Der Report zeigt in einem Schritt:

- direkten MCP-Handshake gegen `code-search` ohne Modellrunde und ohne `codex exec`
- frischen `search_status`-Smoke-Test ueber den direkten MCP-Client
- frische Testquery gegen den `code-search`-MCP ueber denselben Direktpfad
- aktive DB, Dateianzahl und Chunk-Anzahl
- letzten Laufmodus und den Zeitpunkt des letzten erfolgreichen Laufs
- zusaetzlich `Last write mode` und `Last write at`, damit `noop`-Runs von echten Schreiblaeufen getrennt sichtbar sind
- explizit, ob der letzte Lauf und der letzte Schreiblauf erfolgreich bzw. inkrementell waren

Zur robusten Verifikation gibt es ausserdem einen deterministischen Direkt-Client-Selbsttest:

- `node codex-setup/scripts/check-code-search-mcp-client.mjs`
- startet einen temporaeren Fake-MCP-Server mit eigener Test-Konfiguration
- prueft Konfigurationslesen, MCP-Handshake, Tool-Liste und eine echte `search_status`-/`search_code`-Smoke-Route ohne Abhaengigkeit von einer laufenden Codex-Session
