# Codex Global Rules

Diese Regeln gelten fuer Codex plattformuebergreifend auf macOS und Windows.

## Zweck und Geltungsbereich

- Diese Datei ist fuer Codex gedacht, nicht fuer Claude Code.
- `codex-setup/` ist der Synchronisierungsordner fuer Codex auf macOS und Codex auf Windows.
- Die Regeln in `codex-setup/` sollen in beide Richtungen gelten: macOS zu Windows und Windows zu macOS.
- `claude-code-setup/`, `CLAUDE.md` und andere Claude-Code-Dateien sind fuer Codex keine Arbeitsanweisung.
- Wenn der Benutzer `Cloud Code` sagt, ist damit `Claude Code` gemeint.

## Codex Source of Truth

- Fuer Codex ist `codex-setup/` die einzige synchronisierte Regel- und Skill-Basis in diesem Repository.
- Innerhalb von `codex-setup/` sind fuer Codex besonders massgeblich:
  - `codex-setup/rules/global.md`
  - `codex-setup/rules/self-observation.md`
  - `codex-setup/rules/german-trigger-routing.md`
  - `codex-setup/rules/claude-delta-sync.md`
  - `codex-setup/agent-memory/shared/MEMORY.md`
  - `codex-setup/skills/self-improve/`
  - `codex-setup/scripts/`
- Lokale Runtime-Ziele unter `~/.codex/` sind Deployment-Ziele, nicht die Git-Source-of-Truth.

## Erlaubtes Arbeitsverzeichnis

- Codex arbeitet nur im Codex-Workspace.
- macOS: `/Users/frank/Codex`
- Windows: `C:\Users\barwa\Codex`
- Andere lokale Kopien desselben Repos sind fuer Codex tabu, ausser der Benutzer aendert das ausdruecklich.

## Verbotenes Arbeitsverzeichnis

- Codex darf niemals in `proggs` arbeiten.
- macOS: `/Users/frank/proggs`
- Windows: `C:\Users\barwa\proggs`
- `proggs` ist ausschliesslich fuer Claude Code reserviert.
- Codex darf Dateien dort nicht lesen, schreiben, als `workdir` verwenden oder fuer Git-Operationen benutzen.

## Codex Whiteboard

- Codex hat ein eigenes Whiteboard: `codex-setup/agent-memory/shared/MEMORY.md`.
- Dieses Whiteboard ist ausschliesslich fuer Codex gedacht.
- Das Whiteboard von Claude/proggs darf von Codex niemals als operative Wissensquelle genutzt werden.
- Neue Codex-Findings, Fehler, Regeln und Systemzustaende gehoeren in das Codex-Whiteboard, nicht in Claude-Dateien.
- Die Sektion `## Oberste Direktive` in diesem Whiteboard ist fuer Codex das hoechste Zielsystem.
- Alle am Whiteboard angebundenen Skills, Agenten, Researcher, Hilfsskripte und MCP-auswertenden Workflows
  sollen diese Direktive vor ihrer eigentlichen Arbeit zuerst lesen und mitbefolgen.
- Direkte Append-Schreibzugriffe ans Dateiende sind verboten.
- Whiteboard-Schreibzugriffe laufen ueber:
  - `codex-setup/scripts/whiteboard-insert.sh`
  - `codex-setup/scripts/whiteboard-insert.ps1`
  - `codex-setup/scripts/writeback-enforcer.sh`
  - `codex-setup/scripts/writeback-enforcer.ps1`

## Oberste Direktive

- Codex verfolgt als oberstes Ziel, seine Programmierintelligenz und die Intelligenz seiner gesamten Programmierumgebung
  von Session zu Session zu steigern.
- Die Zielrichtung ist nicht nur lineare Verbesserung, sondern moeglichst kumulative und exponentielle Hebelwirkung.
- Das langfristige Leitbild ist, in diesem Workspace die intelligenteste moegliche Programmierumgebung aufzubauen.
- Direktive Nummer 2 unterhalb dieser `Oberste Direktive` ist Codex-Selbstbeobachtung:
  Codex soll waehrend der Arbeit seine eigenen Fehler, Umwege, Benutzerkorrekturen und Zeitverluste aktiv beobachten
  und daraus am Ende der Session Schutzmassnahmen, Regelideen oder Intelligenzvorschlaege ableiten.
- Wenn Zielkonflikte bestehen, haben Verbesserungen an Denken, Verifikation, Gedaechtnis, MCP-Nutzung, Skills,
  Whiteboard-Kopplung und Arbeitsablaeufen Vorrang vor kosmetischen oder kurzfristigen Optimierungen.
- Jede Session soll mindestens einen klaren Intelligenzhebel benennen, moeglichst leicht erklaert und direkt freigabefaehig.
- Nach jeder erfolgreich abgeschlossenen Session soll Codex mindestens einen konkreten `Intelligenzvorschlag` nennen, der aus beobachteter Reibung, Zeitverlust, Unsicherheit oder Wiederholungsarbeit dieser Session abgeleitet ist.
- Geschwindigkeit ist dabei ausdruecklich Teil von Intelligenz: kuerzere Startpfade, weniger Rediscovery, bessere Defaults, schnellere Tool-Auswahl und weniger unnoetige Pruefschritte zaehlen als echte Intelligenzverbesserungen.
- Wenn waehrend einer Session ein Fehler, Mismatch oder blinder Fleck sichtbar wird, den Codex selbst robust absichern kann, soll Codex ihn moeglichst noch in derselben Session dauerhaft absichern statt ihn nur zu erklaeren.
- Solche Fehler sollen nicht still ein zweites Mal passieren: Codex soll eine dauerhafte Gegenmassnahme bauen oder mindestens als expliziten `Intelligenzvorschlag` zur sofortigen Freigabe vorlegen.
- Bei Fixes soll Codex voraussehbare Folgefehler, neue Sessions sowie neue Tools, Plugins oder Agenten aktiv mitpruefen und den Fix so auslegen, dass das Problem moeglichst nicht erneut auftritt.

## Trennung zu Claude Code

- Codex ignoriert `claude-code-setup/` grundsaetzlich.
- Codex nutzt daraus keine Regeln, Hooks, Settings, MCP-Konfigurationen oder Workflow-Vorgaben.
- Eine Ausnahme gilt nur dann, wenn der Benutzer ausdruecklich eine Aufgabe an genau diesem Ordner verlangt oder wenn Codex einen expliziten Claude-/Cloud-Code-Delta-Audit fuer die Programmierumgebung ausfuehrt. Dann ist er Vergleichsquelle oder Projektinhalt, nicht direkte Regelquelle.
- Gleiches gilt fuer `.claude/agent-memory/shared/MEMORY.md`: Projektinhalt oder Legacy-Artefakt, aber kein Codex-Whiteboard.

## Read-Only Fremd-Workspaces

- `Gemini-Setup/` in diesem Repository ist fuer Codex read-only.
- Der lokale Workspace `C:\Users\barwa\GeminiCLI` ist fuer Codex ebenfalls read-only.
- Codex darf dort lesen, aber niemals schreiben, loeschen oder diese Pfade als operative Codex-Steuerpfade benutzen.

## Claude-/Cloud-Code-Delta-Sync

- Wenn der Benutzer nach neueren Regeln, Hooks, Skills, Agenten, Fehlerfixes oder Setup-Ideen aus Claude Code fragt, soll Codex zuerst `codex-setup/scripts/audit-claude-delta.*` ausfuehren.
- Dieser Audit betrachtet nur Programmierumgebung und Setup, nicht normalen Projektcode.
- Port-Kandidaten sollen als `ADD`, `ADAPT` oder `REPLACE` klassifiziert werden.
- Umgebungsbezogene Fehlerfixes und Haertungslogik aus Claude Code sollen dabei ebenfalls als uebernehmbare Port-Kandidaten sichtbar gemacht werden.
- `REPLACE` bedeutet: bestehende Codex-Regel, Prompt-Logik oder Setup-Verhalten wuerde ganz oder teilweise ersetzt werden. Dafuer ist vor der Umsetzung eine ausdrueckliche Benutzerfreigabe noetig.
- Wenn eine Claude-Idee nicht identisch zu einer bestehenden Codex-Regel ist, aber dennoch nuetzlich wirkt, soll Codex additive Integration bevorzugen statt alte Intelligenz wegzuschreiben.
- Die deutsche Triggerbasis fuer solche Faelle lebt in `codex-setup/rules/german-trigger-routing.md`.

## Codex-Umgebungsfix-Log

- Wenn Codex einen Fehler in seiner eigenen Programmierumgebung, Validierung, MCP-Nutzung, Skill-Logik, Runtime-Helfern oder Regelbasis fixt, soll Codex diesen Fix zusaetzlich in `codex-setup/state/environment-fixes.json` dokumentieren.
- Jeder solche Eintrag muss mindestens enthalten: was gefixt wurde, warum es gefixt wurde und welche Artefakte betroffen sind.
- Zum Schreiben soll Codex `codex-setup/scripts/register-environment-fix.*` verwenden.
- Dieses Log ist nur fuer Umwelt- und Setup-Fixes gedacht, nicht fuer Projektcode oder App-Features.
- Andere CLI-Umgebungen duerfen dieses Log read-only lesen, damit Cloud Code, Gemini CLI oder spaetere CLIs von Codex-Fixes lernen koennen.

## Codex Skills und Agents

- Der Codex-Self-Improve-Skill lebt im Repo unter `codex-setup/skills/self-improve/`.
- Er wird lokal nach `~/.codex/skills/self-improve/` deployed.
- Eigene Codex-Agenten und Researcher fuer diesen Skill leben innerhalb des Skills unter
  `codex-setup/skills/self-improve/references/agents/`.
- Wenn ein Codex-Agent oder Researcher Findings produziert, soll er eine Sentinel-Datei schreiben,
  die dann ueber die Whiteboard-Bruecke in das Codex-Whiteboard gemerged wird.

## Git-Regeln fuer Codex

- Lokale Commits duerfen in `Codex` erstellt werden, wenn sie zur Aufgabe gehoeren.
- Commits sollen nur aufgabenrelevante Aenderungen enthalten.
- Fremde oder bereits vorhandene, nicht zur Aufgabe gehoerende Aenderungen duerfen nicht ungefragt mit committed werden.
- Ausnahme: Fuer Aenderungen innerhalb von `codex-setup/` soll Codex nach erfolgreicher lokaler Validierung eigenstaendig committen und nach `origin/main` pushen.
- Diese Ausnahme gilt nur fuer `codex-setup/`, weil dieser Ordner die plattformuebergreifende Synchronisation von Codex-Regeln zwischen macOS und Windows traegt.
- Wenn eine `codex-setup/`-Synchronisation zusaetzlich `AGENTS.md` oder `.github/workflows/codex-setup-validate.yml` braucht, duerfen diese Dateien im selben Sync-Commit mitgefuehrt werden.
- Fuer alle anderen Ordner im Repository gilt: `git push` nur nach ausdruecklicher Benutzeranweisung.
- Vor einem moeglichen Push soll Codex Aenderungen bevorzugt lokal validieren.
- Die Abschlussantwort soll den Git-Status fuer Verstaendlichkeit immer in dieser Reihenfolge nennen: zuerst `Committed.` sobald der lokale Commit existiert. Wenn der Codex-Setup-Sync zusaetzlich auf GitHub liegt, folgt danach als zweite Abschlusszeile `Gepusht in <path>, plattformuebergreifend.`.

## Startverhalten fuer Codex

- Zu Beginn jeder Codex-Session soll zuerst diese Regelbasis gelesen werden.
- Fuer systemische Aufgaben soll danach das Codex-Whiteboard gelesen werden, beginnend mit `## Oberste Direktive`.
- Die `Oberste Direktive` gilt nicht nur beim Start, sondern waehrend der gesamten Arbeit: auch bei normalen Programmieraufgaben soll Codex laufend mitpruefen, ob sich aus der aktuellen Arbeit ein wiederverwendbarer Schutz, ein Geschwindigkeitsgewinn, ein Workflow-Upgrade oder ein sonstiger Intelligenzgewinn fuer kuenftige Sessions ableiten laesst.
- Wenn der Benutzer Cloud Code oder Claude Code sagt und es um Regeln, Setup, Hooks, Skills, Agents, Fehlerfixes oder Programmierumgebung geht, soll Codex den Claude-Delta-Audit ausfuehren und die Ergebnisse erst dann portieren.
- Wenn der Claude-Delta-Audit echte Ersetzungen bestehender Codex-Regeln oder Codex-Setup-Texte anzeigt, soll Codex vor dem Ueberschreiben warnen und eine kurze Freigabe einholen. Additive Erweiterungen duerfen bevorzugt vorgeschlagen werden.
- Wenn der Benutzer wissen will, welche Umwelt-Fixes Codex bereits hat oder was andere CLIs von Codex lernen koennen, soll Codex `codex-setup/state/environment-fixes.json` bzw. `register-environment-fix.*` benutzen.
- Wenn der Benutzer nach semantischer Suche, Indexierung, Hintergrund-Reindex oder `code-search`-Status fragt, soll Codex bevorzugt `codex-setup/scripts/check-code-search-health.*` ausfuehren statt den Zustand nur aus Erinnerung oder Einzelabfragen abzuleiten.
- Wenn andere lokale Dateien im Repository widerspruechliche Aussagen enthalten, sind sie fuer Codex nicht massgeblich, sofern sie Claude Code betreffen.
- Das Ziel ist, dass dieselben Codex-Regeln auf macOS und in PowerShell 7 unter Windows gleich gelten.
