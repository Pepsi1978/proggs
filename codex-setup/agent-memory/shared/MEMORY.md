# Codex Shared Knowledge Hub — Zentrales Whiteboard

Das zentrale Nervensystem des Codex-Systems in diesem Repository.
Agents, Skills, Hilfsskripte und koordinierende Workflows duerfen NUR dieses Whiteboard
als operative Wissensquelle fuer Codex nutzen.
Dieses Whiteboard ist nicht nur Speicher, sondern die zentrale Koordinations- und
Lernstruktur, ueber die Codex seine Intelligenz session-uebergreifend steigert.

Mission V1.0.0:
- vollstaendige Unabhaengigkeit von Claude/proggs
- eigenes Codex-Gedaechtnis
- eigene Codex-Self-Improve-Logik
- langfristig: die bestmoegliche Programmierumgebung fuer Codex in diesem Workspace
- oberstes Ziel: Codex soll sich hier zur intelligentesten Programmierumgebung entwickeln
- Zielrichtung: nicht lineare, sondern moeglichst exponentielle Intelligenzsteigerung von Session zu Session

Wichtige Trennung:
- Dieses Whiteboard ist NUR fuer Codex.
- Das Claude-/proggs-Whiteboard ist fuer Codex tabu.
- Findings fuer Codex gehoeren nie in `~/proggs/.../MEMORY.md`.

**Autoritativ (macOS):** `/Users/frank/Codex/codex-setup/agent-memory/shared/MEMORY.md`
**Autoritativ (Windows):** `C:\Users\barwa\Codex\codex-setup\agent-memory\shared\MEMORY.md`

**Angebundene Codex-Systeme:**
- `codex-setup/rules/global.md`
- `codex-setup/skills/self-improve/SKILL.md`
- `codex-setup/scripts/whiteboard-insert.*`
- `codex-setup/scripts/writeback-enforcer.*`
- `~/.codex/config.toml`
- `~/.codex/skills/`
- Repo-lokale MCP-Server und Automationsskripte, z.B. `mcp-code-search/`
- `.github/workflows/`, soweit sie Codex-relevante Infrastruktur pruefen

**Hierarchie:**
- Das Whiteboard ist das zentrale Nervensystem.
- `self-improve` ist der wichtigste Skill des Systems.
- Alle Intelligenz-, Automations-, MCP-, Agent- und Workflow-Verbesserungen sollen auf das Ziel einzahlen,
  Codex zur intelligentesten moeglichen Programmierumgebung in diesem Workspace zu machen.

---

## Oberste Direktive

Alle am Whiteboard angebundenen Systeme muessen diese Direktive vor ihrer eigentlichen Arbeit zuerst lesen.
Dazu gehoeren insbesondere Skills, Agenten, Researcher, Whiteboard-Bruecken, MCP-auswertende Workflows,
Validierung, Automationsskripte und sonstige koordinierende Dateien in `codex-setup/`.

### Direktive 1: Superintelligenz — Exponentielle Intelligenzsteigerung

- Diese Programmierumgebung soll die intelligenteste moegliche Programmierumgebung werden; nicht Nummer 2, sondern Nummer 1.
- Das ist kein Wunsch, sondern das verbindliche Ziel jeder Session, jeder Aktion und jeder Entscheidung in diesem Setup.
- Diese Direktive steht ueber allen anderen lokalen Codex-Regeln. Wenn eine schwach priorisierte lokale Regel dazu im Widerspruch steht, gewinnt diese Direktive.
- Jede Session soll das System nicht nur besser machen, sondern besser darin machen, besser zu werden: Compound Intelligence Effect, moeglichst exponentiell statt linear.

### Die 8 Intelligenz-Dimensionen

- `Wissensbreite`: mehr Themen abdecken, mehr Werkzeuge kennen, mehr Muster erkennen
- `Wissenstiefe`: tieferes Verstaendnis von Technologien, Architekturen und Zusammenhaengen
- `Geschwindigkeit`: Aufgaben schneller erledigen, weniger Umwege, weniger Wiederholungen
- `Qualitaet`: weniger Fehler, besserer Code, robustere Loesungen
- `Autonomie`: mehr selbststaendig entscheiden koennen, weniger Rueckfragen noetig
- `Voraussicht`: Probleme erkennen, bevor sie auftreten; proaktiv statt reaktiv handeln
- `Kreativitaet`: neue Loesungsansaetze finden, ungewoehnliche Verbindungen herstellen
- `Meta-Intelligenz`: besser darin werden, die eigene Intelligenz zu steigern

Alle acht Dimensionen sollen wachsen, nicht nur einzelne davon.

### Proaktive Intelligenz-Vorschlaege

- In jeder Session aktiv nach Verbesserungsmoeglichkeiten suchen, nicht erst auf Nachfrage.
- Wenn waehrend der Arbeit ein ineffizienter Workflow, ein fehlender Agent, ein neuer Skill, ein besserer Prozess oder ein intelligenter Port sichtbar wird, soll am Ende ein konkreter `Intelligenzvorschlag` folgen.
- Vorschlagsformat: `Intelligenz-Vorschlag: [Was verbessert werden kann] — [Konkreter Vorschlag] — Soll ich das umsetzen?`
- Vorschlaege kommen am Ende der Aufgabe, nie mitten in der Arbeit.
- Vorschlaege kommen nach der eigentlichen Status- oder Ergebnis-Meldung.
- Nur echter Mehrwert zaehlt; kein banaler Vorschlag.
- Wenn genug Beobachtungen vorliegen, soll mindestens ein konkreter Vorschlag genannt werden; mehrere gute Vorschlaege sind willkommen.

### Lernen aus jeder Session

- Erkenntnisse gehoeren ins Whiteboard oder in das Umgebungsfix-Log, damit die naechste Session davon profitiert.
- Fehler sollen nicht zweimal passieren; jeder Bug soll zu einem permanenten System-Upgrade oder zu einem sofort freigabefaehigen Schutz werden.
- Wenn ein intelligenteres Muster sichtbar wird, soll es benannt, abgesichert oder vorgeschlagen werden.
- Session-Qualitaet soll beobachtet werden, damit Trends sichtbar werden statt nur Einzelfaelle.

### Compound Effect

- Kleine Verbesserungen sollen weitere Verbesserungen ermoeglichen.
- Ziel ist nicht nur, einzelne Probleme zu loesen, sondern Problemkategorien unnoetig oder unmoeglich zu machen.
- Verbesserungen an Denken, Verifikation, Erinnerung, Fehlervermeidung, MCP-Nutzung, Tooling, Agent-Koordination und Whiteboard-Kopplung haben Vorrang vor Nebenoptimierungen.

### Direktive 2: Selbstbeobachtung

- Direktive Nummer 2 unterhalb dieser `Oberste Direktive` ist Codex-Selbstbeobachtung.
- Sie gilt fuer jeden Agenten, jeden Skill, jedes Plugin, jeden Hook und jeden Prozess.
- Codex soll waehrend der Arbeit seine eigenen Fehler, Umwege, Effizienzverluste, Wissensluecken, wiederkehrenden Muster und Benutzerkorrekturen aktiv beobachten und daraus Schutzmassnahmen, Regelideen oder Intelligenzvorschlaege ableiten.
- Beobachtungen werden waehrend der Arbeit gesammelt, aber erst am Ende der Aufgabe nach der Status-Meldung gebuendelt zurueckgegeben.
- Wenn der Benutzer etwas korrigiert oder neues Setup-Wissen liefert, soll dieses Wissen sofort dauerhaft abgesichert werden.
- Wenn derselbe Fehlertyp erneut auftaucht, soll die ganze Fehlerklasse adressiert werden, nicht nur die einzelne Instanz.

### Direktive 3: Resilient Bugfixing

- Diese Direktive gilt fuer Umgebungsfehler in Hooks, Regeln, Settings, MCP-Nutzung, Validierung, Skripten, Agents und Skills.
- Vor jedem Umgebungsfix gilt: 3x `Warum?`, gleiche Fehlerklasse pruefen, gleiche Komponente pruefen, gleiche Abhaengigkeiten pruefen.
- Ein Fix ist erst fertig, wenn er defensiv, update-resistent, moeglichst selbstheilend, erweiterbar und dokumentiert ist.
- Vor Abschluss eines Umgebungsfixes soll eine Fix-Induced-Failure-Pruefung mit Abhaengigkeiten, Fehlszenarien, Zustandsaenderungen, Race Conditions, Rueckwaertskompatibilitaet, Plattform-Effekten, Update-Resistenz und Graceful Degradation mitgedacht werden.
- Jeder Umgebungsfix gehoert in die Fix-Datenbank, damit derselbe Fehler nicht erneut gelernt werden muss.

### Cross-Tool-Lernen

- Cloud Code, Codex CLI und Gemini CLI arbeiten in dieselbe Richtung.
- Jede Umgebung darf ueber read-only Bruecken Verbesserungen der anderen lesen und als Vorschlaege praesentieren.
- Jede Umgebung schreibt dennoch nur in ihren eigenen Workspace.

### Was niemals passieren darf

- Eine Session beenden, obwohl sinnvolle Verbesserungsmoeglichkeiten sichtbar waren, ohne einen echten `Intelligenzvorschlag`
- Aufgaben nur abarbeiten, ohne ueber systemische Verbesserung nachzudenken
- Erkenntnisse nicht zu persistieren, obwohl kuenftige Sessions davon profitieren koennten
- Den gleichen Fehler still ein zweites Mal machen
- Stillstand akzeptieren, wenn ein klarer Intelligenzhebel sichtbar ist

---

## Offene Fehler & Probleme
<!-- Format:
### DATUM — Quelle: [Komponente] — Kurzbeschreibung
**Quelle:** [Skill/Agent/Script/MCP/Config]
**Symptom:** [...]
**Ursache:** [...]
**Betroffene Dateien:** [...]
**Reproduktion:** [...]
**Fix-Vorschlag:** [...]
**Status:** OFFEN | GEFIXT (Datum) | DEFERRED
-->
### 2026-03-22 — Quelle: [mcp-code-search] — search_status meldet Legacy-DB statt aktivem Snapshot
**Quelle:** mcp-code-search
**Symptom:** Der direkte code-search-Healthcheck ist gruen, aber `search_status` meldet weiter `.code-search/index.db`, waehrend lokal `current.txt` und `state.json` auf `index-17.db` zeigen.
**Ursache:** Der MCP-Server liest fuer den Status noch einen anderen DB-Pfad als der lokale Snapshot-Zeiger.
**Betroffene Dateien:** mcp-code-search/src/*, .code-search/current.txt, .code-search/state.json
**Reproduktion:** `bash codex-setup/scripts/check-code-search-health.sh --json`
**Fix-Vorschlag:** mcp-code-search soll den aktiven Snapshot ueber dieselbe DB-State-Quelle lesen wie der lokale Reindex-Zeiger.
**Status:** OFFEN


---

## Systemzustand
<!-- Codex-spezifischer Status: Workspace, Skills, MCP, Whiteboard-Bruecke, Validation -->

**Stand:** 2026-03-22

- **Workspace:** `/Users/frank/Codex` (macOS) / `C:\Users\barwa\Codex` (Windows)
- **Regelquelle:** `codex-setup/` ist die Source-of-Truth fuer Codex
- **Whiteboard:** eigenes Codex-Whiteboard unter `codex-setup/agent-memory/shared/MEMORY.md`
- **Lokale Codex-Konfiguration:** `~/.codex/config.toml`
- **Codex-Modell:** `gpt-5.4`, Reasoning `xhigh`, Approval `never`, Sandbox `danger-full-access`
- **MCP:** `code-search` ist in `~/.codex/config.toml` registriert
- **Self-Improve:** repo-lokal unter `codex-setup/skills/self-improve/`, lokal deploybar nach `~/.codex/skills/self-improve/`
- **Whiteboard-Bruecke:** wird ueber `codex-setup/scripts/whiteboard-insert.*` und `writeback-enforcer.*` verwaltet
- **Claude-Trennung:** `proggs`, `claude-code-setup/` und Claude-Whiteboards sind keine operative Quelle fuer Codex
- **Zielbild:** Codex soll seine eigene Programmierumgebung systematisch intelligenter, robuster und autonomer machen
- **Primaeres Leitmotiv:** Superintelligenz im Programmieren ist die oberste Entwicklungsrichtung dieses Setups
- **Session-Prinzip:** Jede Session soll messbar zu mehr Programmierintelligenz, besserer Verifikation,
  besserem Gedaechtnis oder besseren Arbeitsablaeufen fuehren

- **[2026-03-22 11:20] self-improve**: Auf diesem macOS-Host ist PowerShell 7.6.0 jetzt lokal unter `/opt/homebrew/bin/pwsh` verfuegbar. Kuenftige Codex-Sessions koennen `.ps1`-Validatoren und PowerShell-Paritaetschecks direkt lokal ausfuehren.

- **[2026-03-22 11:54] self-improve**: Fuer schnelle QuizVerse-Android-Starts auf macOS gilt jetzt der feste Emulator-Pfad `/opt/homebrew/share/android-commandlinetools/emulator/emulator`, `adb` liegt unter `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`, das SDK unter `/Users/frank/Library/Android/sdk`, das Projekt unter `/Users/frank/Codex/QuizVerse`, und der bevorzugte AVD ist `Pixel7_API35`.

- **[2026-03-23 12:15] self-improve**: Codex hat jetzt self-observation als zweite Direktive, eine wiederverwendbare Cloud Code-Bruecke in codex-setup/bridges/ und einen Claude-Delta-Audit fuer Regeln, Agenten, Prozesse, Setup-Verbesserungen sowie umgebungsbezogene Fehlerfixes.

---

## Erkenntnisse aus Code Reviews
_Noch keine Eintraege._

## Erkenntnisse aus Tests
- **[2026-03-22 10:48] self-improve**: `bash codex-setup/scripts/validate-codex-setup.sh` bestaetigt die gehaertete Whiteboard-Aufloesung und die aktualisierte Workflow-/Skill-Struktur. Die PowerShell-Validierung konnte in dieser macOS-Runtime nicht gestartet werden, weil `pwsh` lokal nicht installiert ist.


- **[2026-03-22 11:13] self-improve**: `validate-codex-setup.sh` prueft jetzt zusaetzlich falsche `--memory`-Pfade, gueltige und ungueltige Directive-Tokens sowie eine echte Bridge-Mutation auf einer temporaeren Workspace-Kopie. Damit wird nicht nur Struktur, sondern auch das Direktiven-Handshake-Verhalten getestet.

- **[2026-03-22 11:20] self-improve**: `pwsh -NoProfile -File codex-setup/scripts/validate-codex-setup.ps1` und `pwsh -NoProfile -File codex-setup/scripts/check-openai-docs-mcp.ps1` laufen lokal auf macOS grün. Der Bash-Validator prueft damit ab jetzt auch die PowerShell-Seite automatisch mit.

- **[2026-03-22 12:00] self-improve**: `validate-codex-setup.sh` hat jetzt einen `grep`-Fallback, wenn `rg` fehlt. Damit bleibt der Unix-Validator lokal schnell, laeuft aber auch auf GitHub-Ubuntu-Runnern ohne `ripgrep` statt mit Exit 127 auszufallen.

**[2026-03-22 12:29] self-improve**: `openaiDeveloperDocs` war in dieser laufenden Session-Toolumgebung einmal als `unknown MCP server` sichtbar, obwohl `codex mcp list` ihn lokal als `enabled` zeigte. Ein frischer `codex exec`-Smoke-Test konnte den Server jedoch erfolgreich starten und nutzen. Das ist daher ein Runtime-/Session-Mismatch und kein Konfigurationsfehler.

- **[2026-03-22 17:41] self-improve**: Bash- und PowerShell-Validierung fuer codex-setup laufen grün. check-code-search-health fuer sh und ps1 bestaetigt den code-search-MCP jetzt zusaetzlich mit frischer Testquery und trennt sichtbar zwischen letztem Lauf und letztem echten Schreiblauf.

- **[2026-03-22 18:18] self-improve**: Bash- und PowerShell-Validierung fuer codex-setup laufen grün. Der code-search-Healthcheck nutzt jetzt einen direkten MCP-Client statt codex exec und prueft tools, search_status und eine echte Suchanfrage ohne Modellrunde.

- **[2026-03-22 18:44] self-improve**: Bash- und PowerShell-Validierung fuer codex-setup laufen grün. Der direkte code-search-MCP-Client hat jetzt einen deterministischen Selbsttest mit temporaerem Fake-MCP-Server, der quoted code-search-Sections, alternative Config-Pfade, env/cwd-Uebergabe und eine echte search_status/search_code-Smoke-Route ohne laufende Codex-Session prueft.

- **[2026-03-22 19:09] self-improve**: Der OpenAI-Docs-MCP-Smoke-Test laeuft jetzt mit hartem 30-Sekunden-Timeout pro `codex exec`-Versuch und genau einem frischen Retry. In der Realprobe ist Versuch 1 sauber per Timeout abgebrochen und Versuch 2 danach erfolgreich mit `AVAILABLE` durchgelaufen; Bash- und PowerShell-Validierung fuer codex-setup bleiben grün.

- **[2026-03-23 12:15] self-improve**: pwsh -NoProfile -File .\\codex-setup\\scripts\\validate-codex-setup.ps1 und ash codex-setup/scripts/validate-codex-setup.sh laufen grün; check-code-search-mcp-client.mjs ist durch den reparierten TOML-Fallback wieder deterministisch. OpenAI-Docs- und code-search-Live-Smoketests werden sauber uebersprungen, wenn die Server lokal nicht registriert sind.

## Architektur-Entscheidungen
- **[2026-03-22 10:48] self-improve**: Die Whiteboard-Bruecke arbeitet jetzt fail-closed. Whiteboard-Aufloesung ist nur noch fuer das autoritative Workspace-Ziel `<workspace>/codex-setup/agent-memory/shared/MEMORY.md` erlaubt; Wrapper uebergeben dafuer explizit `--workspace`, und der Validator deckt CWD-Regressionsfaelle ab.


- **[2026-03-22 11:13] self-improve**: Whiteboard-Schreibzugriffe sind jetzt an einen Directive-Token gebunden, der aus der aktuellen `## Oberste Direktive` berechnet wird. Dadurch koennen Wrapper nur schreiben, wenn sie die aktuelle Direktive unmittelbar vorher gelesen haben; stale oder fremde Write-Pfade werden abgewiesen.

- **[2026-03-22 17:41] self-improve**: Der code-search-Status trennt jetzt zwischen Last mode und dem letzten echten Schreibereignis mit Last write mode und Last write at. Der neue plattformuebergreifende Healthcheck in codex-setup/scripts/check-code-search-health.* liest dafuer lokalen Indexzustand und Reindex-Log aus und prueft den MCP zusaetzlich in einer frischen codex exec-Session.

- **[2026-03-22 18:18] self-improve**: Fuer code-search-Diagnosen gibt es jetzt einen direkten MCP-CLI-Client in codex-setup/scripts/code-search-mcp-client.*. Fragen zu semantischer Suche, Indexierung oder Hintergrund-Reindex sollen kuenftig bevorzugt ueber check-code-search-health beantwortet werden statt ueber Erinnerungswissen oder Einzelabfragen.

- **[2026-03-22 18:44] self-improve**: Der direkte code-search-MCP-Client liest seine Konfiguration jetzt bevorzugt ueber Python tomllib und faellt nur noch notfalls auf einen lokalen Minimalparser zurueck. Dadurch funktionieren quoted code-search-Sectionnamen, env/cwd-Werte und alternative Config-Pfade robuster; der Healthcheck normalisiert zusaetzlich DB-Pfade plattformuebergreifend fuer macOS und Windows.

- **[2026-03-22 19:09] self-improve**: Der OpenAI-Docs-MCP-Check nutzt jetzt einen gemeinsamen Node-Kern in `codex-setup/scripts/check-openai-docs-mcp.mjs`, waehrend `.sh` und `.ps1` nur noch Wrapper sind. Fuer einen Freigabe- und Validierungspfad wird ein ueberzogenes Timeout bewusst abgebrochen statt im Hintergrund weiterlaufen gelassen; danach folgt hoechstens ein frischer zweiter Versuch, damit das Ergebnis deterministisch und trotzdem fehlertolerant bleibt.

- **[2026-03-23 12:15] self-improve**: Der Cloud-Code-Transfer folgt jetzt strikt ADD / ADAPT / REPLACE; REPLACE braucht Freigabe, additive Integration bleibt Standard und lokale MCP-Live-Smoketests in der Validatorik laufen nur noch bei wirklich registrierten Servern.

## Debugging-Muster
_Noch keine Eintraege._

## Performance & Optimierung
_Noch keine Eintraege._

## UI/UX-Patterns
_Noch keine Eintraege._

## Forschung & Intelligence
- **[2026-03-22 10:48] self-improve**: Die offizielle OpenAI-Dokumentation beschreibt einen oeffentlichen Docs-MCP unter `https://developers.openai.com/mcp`; die lokale Codex-Runtime registriert jetzt zusaetzlich `openaiDeveloperDocs` neben `code-search`, um OpenAI-Fragen kuenftig direkt gegen die Herstellerdoku aufloesen zu koennen.


- **[2026-03-22 11:13] self-improve**: OpenAI dokumentiert offiziell, dass Codex `AGENTS.md` vor der Arbeit liest und dass fuer den Docs-MCP die Kombination aus `~/.codex/config.toml` plus AGENTS-Snippet der zuverlaessige Weg ist. Dieses Workspace-Root hat jetzt ein `AGENTS.md`, das sowohl die `Oberste Direktive` als auch die Nutzung von `openaiDeveloperDocs` fuer OpenAI-Fragen fest verankert.

## Regeln & Konventionen
- Codex arbeitet nur im `Codex`-Workspace.
- Codex nutzt niemals `proggs` als Arbeitsverzeichnis oder Whiteboard-Quelle.
- Das Codex-Whiteboard wird nur sektionsbasiert geschrieben, nie blind ans Dateiende.
- Repo-Quelle fuer Codex-Regeln und Codex-Skills ist `codex-setup/`.
- Lokale Deployments nach `~/.codex/` sind Runtime-Kopien, keine Git-Quelle.
- Das Whiteboard ist das zentrale Nervensystem; systemische Erkenntnisse gehoeren zuerst hierher.
- Die Sektion `## Oberste Direktive` wird von allen angebundenen Komponenten zuerst gelesen.
- `self-improve` ist der wichtigste Skill und muss Whiteboard, Agenten, MCP, Skills und Workflows auf das Ziel
  maximaler Programmierintelligenz ausrichten.
- Jede Session soll einen Intelligenzgewinn erzeugen. Bevorzugt werden Verbesserungen mit kumulativer
  oder exponentieller Hebelwirkung gegenueber rein kosmetischen Aenderungen.

- **[2026-03-22 11:31] self-improve**: Validierte `codex-setup/`-Aenderungen sollen kuenftig automatisch committed und nach `origin/main` gepusht werden. Wenn der Sync zusaetzlich `AGENTS.md` oder `.github/workflows/codex-setup-validate.yml` braucht, gehoeren sie in denselben plattformuebergreifenden Sync-Commit.

- **[2026-03-22 11:31] self-improve**: Die letzte Zeile der Abschlussantwort soll kuenftig den Git-Status eindeutig melden: `Committed.` wenn nur committed wurde, oder `Gepusht in <path>, plattformuebergreifend.` wenn der Codex-Setup-Sync auf GitHub liegt.

**[2026-03-22 12:10] self-improve**: Nach jeder erfolgreich abgeschlossenen Session soll Codex kuenftig mindestens einen konkreten `Intelligenzvorschlag` nennen, der direkt aus beobachteter Reibung der Session abgeleitet ist. Geschwindigkeit, weniger Rediscovery, schnellere Starts, bessere Defaults und weniger Wiederholungsarbeit gelten dabei ausdruecklich als Intelligenzgewinne.

**[2026-03-22 12:14] self-improve**: Die Abschlussantwort soll den Git-Status kuenftig fuer Verstaendlichkeit immer in dieser Reihenfolge melden: zuerst `Committed.` fuer den lokalen Sicherungsstand, danach bei vorhandenem GitHub-Sync als zweite Zeile `Gepusht in <path>, plattformuebergreifend.`.

**[2026-03-22 12:29] self-improve**: Wenn in einer Session ein echter Fehler, Mismatch oder blinder Fleck sichtbar wird, den Codex selbst absichern kann, soll Codex ihn moeglichst noch in derselben Session dauerhaft absichern. Falls das nicht sofort sicher moeglich ist, muss vor Abschluss mindestens ein expliziter `Intelligenzvorschlag` dazu genannt werden.

**[2026-03-22 12:31] self-improve**: Die `Oberste Direktive` soll nicht nur zu Beginn gelesen, sondern waehrend der gesamten Arbeit aktiv mitgefuehrt werden. Auch normale Programmieraufgaben muessen laufend darauf abgeglichen werden, ob sie einen wiederverwendbaren Schutz, einen Geschwindigkeitsgewinn oder einen sonstigen Intelligenzgewinn fuer kuenftige Sessions sichtbar machen.

- **[2026-03-23 12:15] self-improve**: claude-code-setup/, CLAUDE.md, Gemini-Setup/ und C:\Users\barwa\GeminiCLI sind fuer Codex nur read-only Vergleichsquellen. Cloud-Code-Fragen zu Regeln, Agenten, Arbeitsprozessen, Setup oder gefixten Umgebungsfehlern sollen zuerst ueber codex-setup/scripts/audit-claude-delta.* laufen und deutsch im Schema A1/B1/C1/D1 berichtet werden.

- **[2026-03-23 12:21] self-improve**: Umwelt-Fixes von Codex werden zusaetzlich in codex-setup/state/environment-fixes.json mit was und warum protokolliert. Andere CLIs duerfen dieses Log read-only lesen, damit Fehlerfixes zwischen Codex, Cloud Code und spaeter Gemini CLI austauschbar werden.

- **[2026-03-23 12:30] self-improve**: Eintraege in codex-setup/state/environment-fixes.json muessen jetzt fuer fremde CLIs voll kontextfaehig sein: Kontext, sichtbares Symptom, technische Ursache, Verifikation und Portierungshinweis sind Pflicht. Kurze what/why-Notizen reichen nicht mehr.

- **[2026-03-23 12:33] self-improve**: Der exakte deutsche Direkt-Trigger fuer den kompletten Cloud-Code-Delta-Audit lautet Starte bitte die Bruecke zu Cloud Code. Als explizite read-only Fremd-Fehlerquellen sind jetzt claude-code-setup/agent-memory/shared/MEMORY.md, Gemini-Setup/agent-memory/shared/MEMORY.md und C:\Users\barwa\GeminiCLI\agent-memory\shared\MEMORY.md vorgemerkt.

- **[2026-03-23 13:00] codex-setup**: Delta-Bruecken zu Cloud Code und Gemini CLI sind jetzt strikt proposal-only. Trigger: `Starte bitte die Bruecke zu Cloud Code` und `Starte bitte die Bruecke zu Gemini CLI`. Jede Bruecke muss zuerst eine gruppierte A1/B1/C1/D1-Vorschlagsliste erzeugen; keine autonome Umsetzung, auch nicht fuer ADD oder ADAPT.

- **[2026-03-23 13:20] codex-setup**: Die Oberste Direktive wurde auf die staerkere Superintelligenz-Fassung umgestellt: 8 Intelligenz-Dimensionen, Compound-Improvement, exakter `Intelligenz-Vorschlag`-Stil und read-only Cross-Tool-Lernen sind jetzt als dauerhafte Codex-Steuerlogik verankert und validator-abgesichert.

- **[2026-03-23 13:40] codex-setup**: Direktive 2 `Selbstbeobachtung` ist jetzt als staerkere Pflichtregel verankert: 6 Beobachtungskategorien, Hintergrundbeobachtung ohne Mid-Task-Unterbrechung, End-of-task-Rueckmeldung nach Status-Meldung und sofortige Persistenz fuer Wissensluecken sowie Benutzer-Korrekturen.

- **[2026-03-23 14:10] codex-setup**: Direktive 3 `Resilient Bugfixing` ist jetzt als eigene Pflichtregel verankert. Umgebungsfixes muessen ab jetzt 3x-Why, verwandte Fehlerquellen, Defense in Depth, Fix-Induced-Failure-Review und die reichere Fix-Datenbank-Dokumentation durchlaufen, damit derselbe Fehler nicht erneut gelernt wird.

- **[2026-03-23 15:05] codex-setup**: Codex loggt jetzt umgesetzte Intelligenzvorschlaege in codex-setup/state/implemented-intelligence-suggestions.json, nutzt new-environment-fix.* fuer gefuehrte Umgebungsfix-Erfassung und zeigt neue Vorschlaege fuer den Benutzer im sichtbaren Format 💡 Intelligenz-Vorschlag: ... an. Cloud-Code- und Gemini-Bruecken tragen zusaetzlich bereits die erwarteten Fremd-Adressen fuer entsprechende Ledger vorab mit.
