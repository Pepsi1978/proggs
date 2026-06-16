# Neues / Horizont-Scan — Best Practices (Stand 2026-06-05, Claude Code 2.1.165)

> Kategorie 12: Ganz neue Claude-Code-Fähigkeiten der letzten ~6 Monate.
> Quellen: Ausschließlich offizielle Anthropic-Dokumentation (code.claude.com/docs/en/whats-new)
> sofern nicht anders angegeben.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Multi-Agent-Orchestrierung | Dynamic Workflows (v2.1.154) — deterministisch, Session-Modell | Dynamic Workflows |
| 2 | autonomer Modus | Auto Mode (kein Opt-in mehr, v2.1.152) | Auto Mode |
| 3 | Hintergrund-Arbeit | Background Agents / Agent View (Sessions-Dashboard) | Background Agents |
| 4 | Cloud-Review | /ultrareview — Multi-Agent-Review in der Cloud | /ultrareview |
| 5 | geplante Agents | Routines (Cloud-Cron) | Routines |
| 6 | Planung | Ultraplan (Cloud-Planungsphase) | Ultraplan |
| 7 | Wiederherstellung | Checkpoints & `/rewind` | Checkpoints & /rewind |

---

## Dynamic Workflows — Dynamische Multi-Agent-Orchestrierung (v2.1.154, Mai 2026)

### Was ist ein Dynamic Workflow?

Ein Dynamic Workflow ist ein JavaScript-Script, das Claude für deine Aufgabe schreibt. Die Workflow-Runtime
führt dieses Script im Hintergrund aus, während deine Session responsiv bleibt. Das ist der entscheidende
Unterschied zu normalen Subagents: Bei Subagents lebt der Plan im Kontext von Claude — bei Workflows
lebt der Plan im Script selbst.

| Eigenschaft | Subagents | Skills | Workflows |
|:---|:---|:---|:---|
| Was es ist | Worker den Claude spawnt | Anweisungen die Claude folgt | Script das die Runtime ausführt |
| Wer entscheidet was als nächstes läuft | Claude, Turn für Turn | Claude, dem Prompt folgend | Das Script |
| Wo leben Zwischenergebnisse | Claudes Kontextfenster | Claudes Kontextfenster | Script-Variablen |
| Was ist wiederholbar | Worker-Definition | Anweisungen | Die Orchestrierung selbst |
| Skalierung | Wenige delegierte Tasks pro Turn | Gleich wie Subagents | Dutzende bis Hunderte Agents pro Run |
| Unterbrechung | Startet den Turn neu | Startet den Turn neu | Innerhalb der Session fortsetzbar |

### Technische Limits (offiziell bestätigt)

| Constraint | Wert | Warum |
|:---|:---|:---|
| Max concurrent Agents | **16** (weniger auf Maschinen mit wenig CPU-Kernen) | Lokale Ressourcen-Begrenzung |
| Max Agents gesamt pro Run | **1.000** | Verhindert Endlosschleifen |
| Kein Mid-Run-Input vom User | — | Nur Agent-Permission-Prompts können pausieren |
| Kein direkter Filesystem-/Shell-Zugriff vom Workflow-Script selbst | — | Agents lesen/schreiben/führen aus; Script koordiniert nur |

### Bundled Workflow: /deep-research

Der einzige eingebaute Workflow. Fächert Web-Suchen über mehrere Winkel auf, holt Quellen,
lässt Agents adversarisch gegeneinander prüfen (Voting), filtert Behauptungen die
die Kreuzprüfung nicht überstehen, und liefert einen zitierten Bericht.

```text
/deep-research Was hat sich im Node.js Permission-Modell zwischen v20 und v22 geändert?
```

### Drei Wege um einen Workflow zu starten

**1. Keyword-Trigger:** Schreib das Wort `workflow` in deinen Prompt:
```text
Führe einen workflow aus um alle API-Endpunkte unter src/routes/ auf fehlende Auth-Checks zu prüfen
```

**2. `ultracode` Effort Level (Claude entscheidet selbst):**
```text
/effort ultracode
```
Mit ultracode plant Claude für jede substanzielle Aufgabe automatisch einen Workflow — ohne dass du
`workflow` schreiben musst. Eine einzelne Anfrage kann mehrere Workflows in Folge auslösen
(verstehen → ändern → verifizieren). Warnung: Deutlich mehr Tokens und Zeit als bei `xhigh`.
Nach Abschluss mit `/effort high` zurückschalten.

**3. Gespeicherten Workflow-Command aufrufen:**
Workflows die du als Command gespeichert hast erscheinen in der `/`-Autocomplete.

### Workflow speichern für Wiederverwendung

```text
/workflows
# → Run auswählen → s drücken
```
Zwei Speicherorte:
- `.claude/workflows/` im Projekt: wird mit dem Repo geteilt
- `~/.claude/workflows/` im Home: in jedem Projekt verfügbar, nur für dich sichtbar

### /workflows Dashboard-Tasten

| Taste | Aktion |
|:---|:---|
| `↑` / `↓` | Phase oder Agent auswählen |
| `Enter` oder `→` | In Phase/Agent hineinzoomen |
| `Esc` | Eine Ebene zurück |
| `p` | Run pausieren/fortsetzen |
| `x` | Gewählten Agent oder ganzen Workflow stoppen |
| `r` | Gewählten laufenden Agent neu starten |
| `s` | Run-Script als Command speichern |

### Permission-Verhalten bei Workflows

Die Subagents in einem Workflow laufen IMMER in `acceptEdits`-Modus — unabhängig vom
Session-Permission-Mode. Datei-Edits sind auto-approved. Shell-Commands, Web-Fetches und
MCP-Tools die nicht in der Allowlist sind können mid-run nach Erlaubnis fragen.
→ **Vor großen Runs: nötige Commands zur Allowlist hinzufügen.**

| Permission-Modus | Wann wird nach Erlaubnis gefragt |
|:---|:---|
| default, acceptEdits | Jeder Run (außer "Don't ask again" für diesen Workflow) |
| auto | Nur beim ersten Launch; danach automatisch |
| bypassPermissions, `claude -p`, Agent SDK | Nie — Run startet sofort |

### Kosten

Ein Workflow spawnt viele Agents — ein einziger Run kann deutlich mehr Tokens verbrauchen
als die gleiche Aufgabe im Gespräch. Runs zählen zu den Plan-Limits wie jede Session.
Laufenden Workflow jederzeit in `/workflows` stoppen — erledigte Arbeit geht nicht verloren.

### Wann lohnt sich ein Workflow?

- Codebase-weite Sicherheitsprüfungen (alle Dateien, nicht nur Stichproben)
- Große Migrationen (500+ Dateien)
- Recherche-Fragen die mehrere Quellen gegeneinander prüfen müssen
- Planung wo mehrere unabhängige Entwürfe gegeneinander abgewogen werden sollen
- Jede Aufgabe die mehr Agents braucht als eine Konversation koordinieren kann

### Workflows deaktivieren

```json
{ "disableWorkflows": true }
```
Oder: `CLAUDE_CODE_DISABLE_WORKFLOWS=1` setzen.
Oder: In `/config` → "Dynamic workflows" aus.

- **Quelle:** [https://code.claude.com/docs/en/workflows](https://code.claude.com/docs/en/workflows) (offiziell), [https://claude.com/blog/introducing-dynamic-workflows-in-claude-code](https://claude.com/blog/introducing-dynamic-workflows-in-claude-code) (offiziell)
- **Stand:** Mai 2026, Research Preview, erfordert v2.1.154+, alle bezahlten Pläne inkl. Bedrock/Vertex/Foundry

---

## Auto Mode auf Bedrock / Vertex / Foundry (v2.1.158, Mai 2026)

### Was ist neu in v2.1.158?

Auto mode ist jetzt auch auf Amazon Bedrock, Google Cloud Vertex AI und Microsoft Azure Foundry
verfügbar — für Opus 4.7 und Opus 4.8. Opt-in über Umgebungsvariable:

```bash
CLAUDE_CODE_ENABLE_AUTO_MODE=1
```

Außerdem in v2.1.158: verbesserter Classifier für Daten-Exfiltration-Erkennung
(besonders Bulk-Transfer von Repository-Inhalten).

### Was Auto Mode ist (offiziell)

Auto Mode lässt Claude ohne Permission-Prompts ausführen. Ein separater Classifier-Model
prüft Aktionen bevor sie ausgeführt werden. Der Classifier blockiert alles was:
- über den ursprünglichen Request hinausgeht (Scope Creep)
- auf nicht erkannte Infrastruktur zielt
- durch hostile Content in gelesenen Dateien gesteuert zu werden scheint

**Zwei Schutzschichten:**
1. Server-seitiger Probe scannt eingehende Tool-Results auf verdächtigen Content
   (bevor Claude sie liest — verhindert direkte Prompt-Injection via Dateiinhalt)
2. Transcript-Classifier mit Chain-of-Thought für jede nicht-triviale Aktion

### Verhältnis zu bypassPermissions (KRITISCH für Franks Setup)

| Eigenschaft | `bypassPermissions` | `auto` Mode |
|:---|:---|:---|
| Permission-Prompts | Keine | Keine |
| Sicherheits-Classifier | Keiner | Aktiv im Hintergrund |
| Prompt-Injection-Schutz | Kein | Vorhanden (2 Schichten) |
| Für welche Umgebung | Nur isolierte Container / VMs | Normaler Rechner OK |
| Blanket-Allow-Regeln (`Bash(*)`) | Bleiben aktiv | Werden beim Betreten fallen gelassen |
| `defaultMode` in Projekt-Settings | Möglich | Wird ignoriert (nur `~/.claude/settings.json`) |
| Auf Bedrock/Vertex/Foundry | Ja | Ab v2.1.158 mit `CLAUDE_CODE_ENABLE_AUTO_MODE=1` |

**Klartext:** Frank's aktuelle `bypassPermissions`-Konfiguration bietet **keinen Classifier-Schutz**.
Auto Mode wäre sicherer, aber erfordert Anthropic-API oder seit v2.1.158 Bedrock/Vertex/Foundry.
Auto Mode und bypassPermissions sind **komplementär** — kein Ersatz füreinander.

### Was der Classifier blockiert (Standard)

Geblockt:
- `curl | bash` (Download + sofortige Ausführung)
- Sensible Daten an externe Endpunkte senden
- Production Deploys und Migrationen
- Massenlöschung auf Cloud Storage
- IAM- oder Repo-Berechtigungen erteilen
- Force-Push, oder Push direkt auf `main`

Erlaubt:
- Lokale Datei-Operationen im Working Directory
- Dependencies aus Lockfiles installieren
- `.env`-Credentials an die passende API senden
- Read-only HTTP-Requests
- Push auf aktuellen Branch oder Branch den Claude erstellt hat

### Fallback-Logik

- 3 aufeinanderfolgende Blocks → Auto Mode pausiert, normal prompting resumiert
- 20 Blocks gesamt in der Session → Auto Mode pausiert
- Jede erlaubte Aktion setzt den consecutiven Zähler zurück; der Gesamt-Zähler läuft bis zum Limit

### Boundaries in der Konversation

Wenn du sagst "pushe nicht" oder "warte auf mein Review" behandelt der Classifier das als
Block-Signal — auch wenn die Default-Regeln die Aktion erlauben würden. Caveat: Boundaries
können bei Context Compaction verloren gehen. Für harte Garantien: `hardDeny`-Regel anlegen.

### Classifier-Kosten

Classifier-Aufrufe zählen zum Token-Verbrauch. Reads und Working-Directory-Edits (außer
Protected Paths) überspringen den Classifier → Overhead hauptsächlich bei Shell-Commands
und Netzwerk-Operationen.

### Wichtig: Docs-Seite noch nicht aktualisiert

Die offizielle Docs-Seite zu `permission-modes` (Stand 2026-05-30) listet Auto Mode
noch als "Anthropic API only, not available on Bedrock/Vertex/Foundry". v2.1.158 hat
das geändert, aber nur via `CLAUDE_CODE_ENABLE_AUTO_MODE=1` opt-in.
Die Docs werden erfahrungsgemäß verzögert aktualisiert.

- **Quelle (Docs):** [https://code.claude.com/docs/en/permission-modes](https://code.claude.com/docs/en/permission-modes) (offiziell)
- **Quelle (Engineering):** [https://www.anthropic.com/engineering/claude-code-auto-mode](https://www.anthropic.com/engineering/claude-code-auto-mode) (offiziell)
- **Quelle (v2.1.158):** [https://github.com/anthropics/claude-code/releases](https://github.com/anthropics/claude-code/releases) (offiziell)
- **Stand:** Mai 2026, Research Preview

---

## Background Agents & `claude agents` — Reifegrad und Best Practices (Stand 2.1.153)

### Was sind Background Agents?

Background Agents sind vollständige Claude-Code-Sessions, die ohne angehängtes Terminal
weiterlaufen. Öffnet man `claude agents`, sieht man alle laufenden Sessions auf einem Bildschirm
gruppiert nach Status: *Working*, *Needs input*, *Ready for review*, *Completed*. Der Supervisor-Prozess
läuft im Hintergrund; Sessions überleben Sleep, aber nicht Shutdown.

### Reifegrad (2.1.139 → 2.1.153)

- **2.1.139** (Mai 2026): Research Preview — `claude agents` + `/goal` + Background Sessions eingeführt.
- **2.1.141–2.1.148**: Stufenweise Stabilisierung: `--cwd`-Filter, `--permission-mode`/`--model`/
  `--effort` Flags, `worktree.bgIsolation`-Setting, Pinning, MCP-Config-Flags.
- **2.1.152–2.1.153** (27./28. Mai 2026): Signifikante UX-Verbesserungen und Bugfixes — erster
  Release-Cluster der auf Stabilisierung der gesamten Background-Session-Infrastruktur abzielt.

### Neue Fähigkeiten in 2.1.153

| Feature | Was sich ändert |
|---------|----------------|
| `/bg` während Response | Wenn Claude noch antwortet und man `/bg` ausführt, wird die Response in der Background-Session **fortgesetzt** statt abgebrochen |
| `/btw` in Background Sessions | Keyboard-Shortcuts für `/btw` funktionieren jetzt auch wenn eine Background-Session aktiv einen Task ausführt |
| `Ctrl+T` Pinning | Gepinnte Sessions bleiben im Idle bestehen, werden bei Updates neu gestartet und nur unter extremem Speicherdruck nach nicht-gepinnten Sessions gestoppt |
| `EnterWorktree` sofort verfügbar | Kein `ToolSearch`-Aufruf als Voraussetzung mehr nötig |
| macOS Privacy & Security | Background Agents erscheinen als "Claude Code" in Datenschutz-Einstellungen; Permission-Grants bleiben nach Upgrades erhalten |
| PR-Spalte | Zeigt `PR #N` (einzeln) oder `N PRs` (mehrere) statt generischem Label |
| Autocomplete Dispatch | Schlägt jetzt native Slash Commands **und** bundled Skills vor, nicht nur Project Skills |
| Temp-File-Permissions | Background Sessions, die Temp-Files in `$CLAUDE_JOB_DIR` schreiben, triggern keine "sensitive file" Permission-Prompts mehr |

### Neue Fähigkeiten in 2.1.152

| Feature | Was sich ändert |
|---------|----------------|
| Workflow-Tool Progress | Vereinfachte Inline-Progress-Anzeige — live Agent-Counts nur noch in der persistenten Workflow-Status-Zeile unter dem Prompt |
| Post-Response Timer | "Waiting for N background agents/workflows to finish" wenn backgrounded Agents oder Workflows noch laufen |

### Wie man Background Agents heute am besten nutzt

**Einstieg — drei Wege:**
```bash
# 1. Agent View öffnen, Prompt eintippen → Enter
claude agents

# 2. Direkt aus der Shell starten
claude --bg "untersuche den flaky SettingsChangeDetector-Test"

# 3. Laufende Session backgrounden
/bg run the test suite and fix any failures
```

**Dispatch-Flags mitgeben (ab 2.1.142):**
```bash
claude agents --permission-mode bypassPermissions --model opus --effort high
```

**Best Practices (Stand 2.1.153):**

- **Mehrere unabhängige Tasks parallel starten** (Bug-Fix, PR-Review, Test-Untersuchung) statt sequenziell.
- **`Ctrl+T` für wichtige Sessions pinnen** — damit laufen sie auch nach einer Stunde Idle weiter und werden bei Auto-Updates neu gestartet statt gestoppt.
- **`/bg` mit Nachricht** nutzen: `/bg run tests and push when green` — Claude führt den nächsten Schritt erst in der Background-Session aus.
- **`Space` zum Peek** reicht für die meisten Check-ins; nur bei Bedarf mit `Enter`/`→` vollständig attachen.
- **`--cwd <path>`** nutzen wenn man nur Sessions eines Projekts sehen will.
- **Worktree-Isolation** ist Standard für git-Repos: jede Background-Session schreibt in ihr eigenes Worktree unter `.claude/worktrees/`. Deaktivieren per `worktree.bgIsolation: "none"` in `.claude/settings.json` nur wenn git-Worktrees nicht praktikabel sind.
- **Quota beachten**: Jede Background-Session verbraucht Subscription-Quota wie eine interaktive Session.
- **Shutdown-Recovery**: Nach Rechner-Neustart Sessions einfach attachen oder antworten — der Supervisor startet sie neu vom letzten Stand.

**Was `/bg`, `/btw` und `claude agents` unterscheidet:**

| Befehl | Zweck |
|--------|-------|
| `claude agents` | Dashboard für ALLE Background-Sessions |
| `/bg` | Aktuelle Session in Background schieben (trägt Config-Flags mit) |
| `--bg` Flag | Session von Anfang an als Background starten |
| `/btw` | Schnelle Frage ohne Kontext-Einfluss (kein Tooling, sieht vollen Kontext) |

- **Quelle:** [https://code.claude.com/docs/en/agent-view](https://code.claude.com/docs/en/agent-view) (offiziell), [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell)
- **Stand:** Mai 2026, ab v2.1.139 (Research Preview), signifikant reifer ab 2.1.152/153

---

## Auto Mode (v2.1.83, März 2026)

- **Was:** Classifier-basiertes Permission-System als Mittelweg zwischen manuellem Review und `--dangerously-skip-permissions`. Sichere Aktionen laufen durch, riskante werden blockiert. Zwei Schichten: Server-seitiger Prompt-Injection-Probe (Input) + Transcript-Classifier mit Sonnet 4.6 (Output). Stage 1: schnelle Ja/Nein-Entscheidung. Stage 2: Chain-of-Thought nur für markierte Aktionen. Ab 2.1.152: kein Opt-in Consent mehr nötig.
- **Best Practice:** Mit Default-Einstellungen beginnen (konservative Konfiguration). `autoMode.allow`, `soft_deny`, und `environment` anpassen. `"$defaults"` in die Listen aufnehmen, um eigene Regeln zusätzlich zu den eingebauten hinzuzufügen (nicht zu ersetzen). `autoMode.hardDeny` nutzen für absolut verbotene Aktionen unabhängig von Ausnahmen. Für Max-Abonnenten auf Opus 4.7 ist Auto Mode ohne `--enable-auto-mode`-Flag verfügbar. 0,4% False-Positive-Rate auf echtem Traffic; ~17% der unsicheren Aktionen werden nicht erkannt — kein Ersatz für eigene Urteilsfähigkeit.
- **Quelle:** [https://www.anthropic.com/engineering/claude-code-auto-mode](https://www.anthropic.com/engineering/claude-code-auto-mode) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w13](https://code.claude.com/docs/en/whats-new/2026-w13) (offiziell)
- **Stand:** März 2026 (Research Preview → ab April 2026 für Max ohne Flag)

---

## Computer Use im CLI (v2.1.86, März/April 2026)

- **Was:** Claude kann native Apps öffnen, durch UI klicken, eigene Änderungen testen und Fehler beheben — alles aus dem Terminal heraus. Zuvor nur im Desktop-App verfügbar. Ermöglicht Verifikationsschleifen für Apps ohne API (iOS-Simulator, native macOS-Apps, GUI-Tools). Research Preview, noch mit rauen Kanten.
- **Best Practice:** Über `/mcp` aktivieren → `computer-use` einschalten. Nur für GUI-only-Verifikation einsetzen, wo keine API existiert. Nicht für einfache Code-Tasks verwenden, die per Bash lösbar sind — Computer Use ist langsamer und teurer. Ideal für End-to-End-Tests nativer Apps nach Code-Änderungen. Beispiel: "Open the iOS simulator, tap through onboarding, and screenshot each step."
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w14](https://code.claude.com/docs/en/whats-new/2026-w14) (offiziell), [https://code.claude.com/docs/en/computer-use](https://code.claude.com/docs/en/computer-use) (offiziell)
- **Stand:** März/April 2026 (Research Preview)

---

## /ultrareview — Cloud-basierter Multi-Agent-Review (v2.1.114, April 2026)

- **Was:** Fleet von Bug-hunting-Agents in der Cloud analysiert den aktuellen Branch oder einen bestimmten PR. Befunde landen automatisch in CLI oder Desktop. Umfasst parallele Multi-Agent-Analyse und einen adversarischen Critique-Pass. Kein lokales Rechnen nötig.
- **Best Practice:** Vor Merges kritischer Änderungen einsetzen (Auth, Datenmigration, Security-Code). Aktuellen Branch: `/ultrareview`. Bestimmten PR: `/ultrareview 1234`. Auch aus CI und Scripts via `claude ultrareview` (ohne Slash, ab v2.1.120) aufrufbar. Nicht für triviale Änderungen — der Ressourcenaufwand ist erheblich. Ergänzt, ersetzt nicht den lokalen Code-Reviewer-Agent.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w17](https://code.claude.com/docs/en/whats-new/2026-w17) (offiziell), [https://code.claude.com/docs/en/ultrareview](https://code.claude.com/docs/en/ultrareview) (offiziell)
- **Stand:** April 2026 (Research Preview → ab Woche 18 auch in CI)

---

## Routines — Geplante Cloud-Agents (v2.1.105–113, April 2026)

- **Was:** Templated Cloud-Agents die auf Zeitplan, GitHub-Event oder API-Call feuern. Definition über Claude Code Web: Prompt + erlaubte Repos + Connectors. Trigger: schedule, PR-opened, release-published, eigene Webhooks. Jede Routine bekommt einen tokenisierten `/fire`-Endpunkt für externe Systeme.
- **Best Practice:** Tägliche PR-Reviews automatisieren: `/schedule daily PR review at 9am`. Repetitive Aufgaben (Nightly-Tests, Auto-Triage von Issues) als Routine statt manueller Ausführung. Auf Claude Code Web erstellen oder via CLI scaffolden. Rechte der Routine auf die minimal nötigen Repos beschränken. GitHub-Event-Filter nutzen, um unnötige Auslösungen zu vermeiden.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell), [https://code.claude.com/docs/en/routines](https://code.claude.com/docs/en/routines) (offiziell)
- **Stand:** April 2026

---

## Native Binaries (v2.1.113, April 2026)

- **Was:** Das `claude`-CLI verwendet jetzt pro Plattform eine native Binary statt gebündeltem JavaScript. Node.js wird beim Aufruf nicht mehr involviert. Das npm-Paket lädt die passende Binary via optionale Abhängigkeit (z. B. `@anthropic-ai/claude-code-darwin-arm64`). Schnellerer Start, geringerer Overhead.
- **Best Practice:** `claude update` ausführen und mit `claude --version` verifizieren. Kein Änderungsbedarf am Install-Befehl. Auf Windows gilt: PowerShell ist nun der Shell-Fallback wenn Git Bash fehlt (ab v2.1.120 kein Git for Windows mehr erforderlich). MCP-Server-Configs müssen weiterhin absolute Pfade verwenden.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026 (v2.1.113)

---

## Ultraplan — Cloud-basierte Planungsphase (v2.1.92–v2.1.101, April 2026)

- **Was:** Planentwurf in der Cloud, Review und Kommentierung im Web-Editor, dann Ausführung remote oder lokal. Erster Run erstellt automatisch eine Cloud-Umgebung. Trennt Planungsphase von Ausführung.
- **Best Practice:** Für komplexe Architektur-Änderungen oder mehrstufige Migrationen einsetzen (nicht für einfache Tasks). Beispiel: `/ultraplan migrate the auth service from sessions to JWTs`. Plan im Browser überprüfen und kommentieren bevor Ausführung startet. Remote-Ausführung für lang laufende Migrationen wählen, um lokale Ressourcen zu schonen.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w15](https://code.claude.com/docs/en/whats-new/2026-w15) (offiziell)
- **Stand:** April 2026 (Early Preview)

---

## Agent View — Zentrales Sessions-Dashboard (v2.1.139, Mai 2026)

- **Was:** `claude agents` öffnet einen einzigen Bildschirm für alle laufenden Claude-Code-Sessions: was läuft, was wartet auf Input, was ist fertig. An jede Zeile anknüpfen für das vollständige Gespräch, `←` zurück zur Liste. Hintergrund-Sessions laufen ohne angehängtes Terminal weiter.
- **Best Practice:** Mehrere parallele Sessions starten (Bug-Fix, PR-Review, Test-Untersuchung) und über `claude agents` steuern. Dispatch-Flags mitgeben: `--add-dir`, `--settings`, `--model`, `--effort`, `--permission-mode`. `--cwd <path>` für projektbezogene Session-Ansicht nutzen. Nur bei Bedarf in eine Session einsteigen — kein ständiges Überwachen nötig. Gut kombinierbar mit `/goal` für autonome Background-Tasks.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell), [https://code.claude.com/docs/en/agent-view](https://code.claude.com/docs/en/agent-view) (offiziell)
- **Stand:** Mai 2026 (Research Preview, v2.1.139)

---

## /goal — Autonome Zielverfolgung (v2.1.139, Mai 2026)

- **Was:** Setzt eine Completion-Bedingung; Claude arbeitet über mehrere Turns selbstständig weiter bis sie erfüllt ist. Nach jedem Turn prüft ein schnelles Modell ob die Bedingung gilt. Funktioniert im interaktiven Modus, `-p` und Remote Control. Ziel wird nach Erfüllung automatisch gelöscht.
- **Best Practice:** Verifiable End States definieren (nicht vage Ziele): `/goal all tests in test/auth pass and the lint step is clean`. Für umfangreiche Arbeit mit klarer Fertigstellungsbedingung einsetzen (Modul-Migration, Refactoring bis alle Call-Sites kompilieren). Mit `claude agents` kombinieren: `/goal` in einer Background-Session starten, selbst weiterarbeiten. Zu breite Ziele vermeiden — Claude braucht eine prüfbare Bedingung.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell), [https://code.claude.com/docs/en/goal](https://code.claude.com/docs/en/goal) (offiziell)
- **Stand:** Mai 2026 (v2.1.139)

---

## Fast Mode auf Opus 4.7 (Research Preview, Mai 2026)

- **Was:** `/fast` läuft jetzt auf Opus 4.7 statt Opus 4.6. Gleiche Modellqualität bei ~2,5× Geschwindigkeit gegen höhere Token-Kosten. Preis unverändert ($30/$150 pro MTok wie Opus 4.6 Fast Mode). Für schnelle Iteration und Live-Debugging gedacht.
- **Best Practice:** `/fast` für Rapid-Iteration-Phasen und interaktives Debugging nutzen. Für finale, qualitätskritische Arbeit wieder auf Standard-Modus zurückschalten. Pin auf Opus 4.6: `CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE=1` setzen. Nicht als Dauereinstellung — Fast Mode kostet mehr pro Token. **VERALTET ab 06/01/2026:** `CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE` wird entfernt (deprecated in v2.1.158).
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell), [https://code.claude.com/docs/en/fast-mode](https://code.claude.com/docs/en/fast-mode) (offiziell)
- **Stand:** Mai 2026 (Research Preview)

---

## Checkpoints & /rewind (v2.0, Ende 2025 — erweitert Mai 2026)

- **Was:** Automatische Snapshots des Code-Zustands vor jeder Änderung. Drei Wiederherstellungs-Modi: Chat only (Chat zurückspulen, Code behalten), Code only (Dateiänderungen revertieren, Chat behalten), Both (vollständige Wiederherstellung). Checkpoints 30 Tage aufbewahrt. Rewind-Menü bietet seit Mai 2026 "Summarize up to here" für Kontext-Kompression älterer Turns. `/undo` ist Alias für `/rewind`.
- **Best Practice:** Vor riskanten Operationen (Refactoring, Migrations) auf Checkpoint verlassen. `Esc Esc` oder `/rewind` bei Fehlschlag. "Summarize up to here" nutzen wenn ältere Turns Platz im Kontextfenster verbrauchen aber nicht mehr relevant sind. "Code only"-Modus für Experimente: Code revertieren ohne Gesprächskontext zu verlieren.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell)
- **Stand:** Ende 2025 (v2.0), erweitert Mai 2026

---

## Monitor-Tool — Echtzeit-Event-Streaming (v2.1.98, April 2026)

- **Was:** Hintergrund-Watcher streamt Events als Transcript-Messages in die Konversation. Claude reagiert sofort auf jedes Event ohne Sleep-Loops. Für Log-Tailing, CI-Überwachung, Dev-Server-Crashes geeignet.
- **Best Practice:** Für Szenarien nutzen wo Claude auf externe Ereignisse reagieren soll: "Tail server.log in the background and tell me the moment a 5xx shows up." Plugins können Hintergrund-Watcher via `monitors`-Schlüssel im Manifest definieren (automatisch beim Session-Start aktiviert). Mit `/loop` kombinieren wenn periodische Checks nötig sind; Monitor bevorzugen wenn Push-Events verfügbar sind.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w15](https://code.claude.com/docs/en/whats-new/2026-w15) (offiziell)
- **Stand:** April 2026 (v2.1.98)

---

## /recap — Session-Zusammenfassung (April 2026)

- **Was:** Zeigt eine einzeilige Zusammenfassung dessen was passierte, während ein Terminal nicht fokussiert war. Nützlich beim Betreiben mehrerer paralleler Sessions. Automatisch beim Zurückkehren zur Session; manuell per `/recap`; in `/config` deaktivierbar.
- **Best Practice:** Nicht deaktivieren wenn man mit mehreren Sessions arbeitet — spart Zeit beim Wiedereinstieg. `/recap` manuell aufrufen nach Unterbrechungen. Bei sehr langen Sessions: `/recap` gibt Überblick vor dem Vertiefen in Details.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w17](https://code.claude.com/docs/en/whats-new/2026-w17) (offiziell)
- **Stand:** April 2026

---

## /usage — Verbrauchs-Analyse (v2.1.105–113, April 2026)

- **Was:** Zeigt was die Limits antreibt: parallele Sessions, Subagents, Cache-Misses, langer Kontext — jeweils mit Prozentzahl der letzten 24h und Optimierungstipp. `d`/`w` für Tag/Woche-Ansicht. Vereinigt die alten `/cost` und `/stats` Befehle (alte Namen noch als Shortcuts verfügbar). Ab 2.1.152: per-Category Breakdown von Limits-Nutzung — Skills, Subagents, Plugins, Pro MCP-Server Cost.
- **Best Practice:** Regelmäßig prüfen wenn Limits spürbar werden. Cache-Miss-Prozentsatz beobachten — hohe Werte deuten auf suboptimalen Prompt-Aufbau hin. Parallelisierungs-Overhead gegen Zeitgewinn abwägen. Wochenansicht für Trend-Erkennung nutzen. Category-Breakdown (neu 2.1.152) nutzen um zu sehen ob Background Agents den größten Anteil verursachen.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026, per-Category Breakdown neu in 2.1.152

---

## Mobile Push Notifications (v2.1.105–113, April 2026)

- **Was:** Mit Remote Control verbunden sendet Claude Push-Benachrichtigungen ans Handy wenn ein langer Task endet oder eine Entscheidung benötigt wird. In `/config` aktivieren: "Push when Claude decides". Auch per Prompt-Anweisung: "notify me when the tests pass."
- **Best Practice:** Für lang laufende Agent-Runs aktivieren und vom Terminal weggehen — kein manuelles Polling nötig. Remote Control muss eingerichtet sein. Nicht bei kurzen Tasks aktivieren (unnötiger Lärm). Gut kombinierbar mit `claude agents` + `/goal` für vollständig autonome Workflows.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026

---

## Windows ohne Git Bash (v2.1.120, April/Mai 2026)

- **Was:** Git for Windows ist nicht mehr erforderlich. Claude Code nutzt PowerShell als Shell-Tool wenn Bash fehlt. Erleichtert Windows-Setup erheblich. Native PowerShell-Tool seit Woche 13 verfügbar.
- **Best Practice:** Bestehende Windows-Installationen können Git Bash behalten — kein Breaking Change. Für Neu-Installationen: kein Git for Windows mehr nötig. PowerShell-Hooks (`.ps1`) funktionieren nun als primäre Shells. MCP-Server-Configs weiterhin mit absoluten Pfaden konfigurieren.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w18](https://code.claude.com/docs/en/whats-new/2026-w18) (offiziell)
- **Stand:** April/Mai 2026 (v2.1.120)

---

## /powerup — Interaktive In-App-Lernlektionen (v2.1.90, April 2026)

- **Was:** Animierte Demos zu Claude-Code-Features direkt im Terminal. Verhindert das Verpassen von Features bei den häufigen Releases.
- **Best Practice:** Nach größeren Updates ausführen: `/powerup`. Besonders für neue Teammitglieder geeignet. Ergänzend zu `/team-onboarding` nutzen (erzeugt replaybare Onboarding-Guides aus eigenem Usage-Muster für das Team).
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w14](https://code.claude.com/docs/en/whats-new/2026-w14) (offiziell)
- **Stand:** April 2026 (v2.1.90)

---

## Conditional Hooks & Hook-Neuerungen (Woche 13–20, 2026)

- **Was:** `if`-Hooks feuern nur unter Bedingungen. `continueOnBlock` bei `PostToolUse`: Ablehnungsgrund wird Claude mitgeteilt und der Turn weitergeführt statt abzubrechen. `terminalSequence`-Feld in Hook-JSON für Desktop-Benachrichtigungen ohne Terminal. `args: string[]` exec-Form spawnt Befehle direkt ohne Shell (kein Quoting-Problem bei Pfad-Platzhaltern). `PreCompact`-Hooks können Kompaktierung via exit 2 oder `{"decision":"block"}` blockieren. Hooks können nun MCP-Tools direkt via `type: "mcp_tool"` aufrufen. Neu ab 2.1.152: `MessageDisplay`-Hook-Event um Assistant-Message-Text beim Anzeigen zu transformieren oder zu verstecken. `SessionStart`-Hooks können `reloadSkills: true` zurückgeben.
- **Best Practice:** `if`-Bedingungen nutzen um Hook-Overhead zu reduzieren (nur bei relevanten Tool-Calls feuern). `continueOnBlock` für Hooks die Claude auf Alternativen hinweisen sollen statt den Turn hart abzubrechen. `type: "mcp_tool"` für Hooks die auf bereits verbundene MCP-Server zugreifen müssen — effizienter als Prozess-Spawn. `PreCompact`-Block nur einsetzen wenn wichtige In-Progress-Daten gesichert werden müssen. `args: string[]`-Form bevorzugen bei Pfaden mit Leerzeichen.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w13](https://code.claude.com/docs/en/whats-new/2026-w13) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w17](https://code.claude.com/docs/en/whats-new/2026-w17) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell)
- **Stand:** März–Mai 2026

---

## Claude Opus 4.7 + xhigh Effort Level (v2.1.105, April 2026)

- **Was:** Stärkstes Coding-Modell (Default auf Max und Team Premium). Neuer `xhigh` Effort-Level zwischen `high` und `max` — empfohlen für die meisten Coding- und Agentic-Tasks. `/effort` öffnet interaktiven Slider ohne Argumente. Default-Effort für Pro/Max-Abonnenten jetzt `high` (war `medium`). Opus 4.7 nutzt nativ 1M-Kontextfenster (behebt fehlerhafte `/context`-Prozentzahlen früherer Versionen).
- **Best Practice:** `/model opus` dann `/effort xhigh` für anspruchsvolle Tasks. Interaktiven Slider mit `/effort` (ohne Argumente) nutzen. `max`-Level für Aufgaben sparen wo Qualität absolut kritisch ist. Fast Mode für Rapid-Iteration; Standard für Produktion. Hinweis: `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE`-Schwellwert wurde durch Opus 4.7's 1M-Fenster faktisch neu kalibriert.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w16](https://code.claude.com/docs/en/whats-new/2026-w16) (offiziell)
- **Stand:** April 2026 (v2.1.105)

---

## Plugin-Neuerungen: ZIP-Archive, URLs, bin/-Verzeichnis (Woche 16–19, 2026)

- **Was:** `--plugin-dir` akzeptiert `.zip`-Dateien; `--plugin-url` lädt Plugin-Archive von einer URL für die aktuelle Session. Executables im `bin/`-Verzeichnis eines Plugins werden automatisch zum Bash-`PATH` der Session hinzugefügt. `claude plugin tag` erstellt Release-Tags mit Versions-Validierung. `claude plugin details <name>` zeigt Komponenten-Inventar und Token-Kosten-Schätzung. Plugins mit Root-Level `SKILL.md` ohne `skills/`-Unterverzeichnis werden als Skill erkannt.
- **Best Practice:** Plugins als ZIP-Archive verteilen für einfachere Installation ohne Repo-Zugriff. `bin/`-Verzeichnis nutzen um CLI-Helfer direkt neben Hooks/Commands zu paketieren — kein absoluter Pfad oder Wrapper-Script nötig. `claude plugin details <name>` vor Aktivierung prüfen um Token-Kosten zu kennen. Plugin-Releases mit `claude plugin tag` versionieren.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w19](https://code.claude.com/docs/en/whats-new/2026-w19) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w14](https://code.claude.com/docs/en/whats-new/2026-w14) (offiziell), [https://code.claude.com/docs/en/whats-new/2026-w20](https://code.claude.com/docs/en/whats-new/2026-w20) (offiziell)
- **Stand:** April/Mai 2026

---

## /autofix-pr, /team-onboarding, /loop (v2.1.98–101, April 2026)

- **Was:** `/autofix-pr` — leitet aus dem aktuellen Branch den offenen PR ab, aktiviert Auto-Fix auf Claude Code Web, überwacht CI und pusht Fixes bis grün. `/team-onboarding` — generiert einen Ramp-up-Guide aus eigenen Usage-Mustern, den Teammates replizieren können. `/loop` — selbstpacend wenn Intervall weggelassen; `/proactive` ist Alias.
- **Best Practice:** `/autofix-pr` nach `git push` ausführen und vom Terminal weggehen — Claude übernimmt CI-Reparaturen. `/team-onboarding` in gut bekannten Projekten ausführen für standardisierte Dokumentation. `/loop` ohne Intervall-Parameter für intelligente Auto-Pacing-Nutzung; Monitor-Tool bevorzugen wenn Push-Events verfügbar.
- **Quelle:** [https://code.claude.com/docs/en/whats-new/2026-w15](https://code.claude.com/docs/en/whats-new/2026-w15) (offiziell)
- **Stand:** April 2026

---

## Skills: disallowed-tools + /reload-skills (v2.1.152, Mai 2026)

- **Was:** Skills können jetzt `disallowed-tools` in ihrem Frontmatter setzen um Tools für die Dauer des Skills zu deaktivieren (z.B. Web-Search unterdrücken). Neuer `/reload-skills`-Befehl scannt Skill-Verzeichnisse erneut ohne Neustart. `SessionStart`-Hooks können `reloadSkills: true` zurückgeben.
- **Best Practice:** `disallowed-tools` für fokussierte Skills nutzen die keine bestimmten Tools brauchen (z.B. reine Code-Analyse-Skills die kein Web-Search benötigen). `/reload-skills` nach dem Hinzufügen neuer Skills ausführen statt die Session zu schließen.
- **Quelle:** [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell)
- **Stand:** Mai 2026 (v2.1.152)

---

*Recherchiert 2026-05-30 · 3 WebSearches + 2 WebFetches (code.claude.com/docs/en/workflows, code.claude.com/docs/en/permission-modes) · Alle Kernangaben zu Dynamic Workflows und Auto Mode v2.1.158 offiziell bestätigt.*

<!-- CHECKPOINT: fertig — Dynamic Workflows (2.1.154) und Auto Mode Bedrock/Vertex (2.1.158) vollständig dokumentiert. Empfehlung: Eigene Kategorie für Dynamic Workflows. Nächste Recherche-Session: Claude Opus 4.8 Details (neu in v2.1.158). -->

---

### Update 2026-06-05 (Claude Code 2.1.165) — Neues / Horizont

**1. Dynamic-Workflow-Trigger umbenannt: `workflow` -> `ultracode` (2.1.160)**
- **Was:** Das Literal-Keyword fuer einen Dynamic Workflow heisst jetzt `ultracode` (violett hervorgehoben). Das Wort "workflow" triggert keinen Run mehr; natuerlichsprachlich ("use a workflow") geht weiter.
- **Was sind Dynamic Workflows:** JavaScript-Skripte, die Claude fuer die Aufgabe schreibt und ein eigener Runtime-Prozess im Hintergrund ausfuehrt; das Skript haelt Schleife + Zwischenergebnisse, Claude's Kontext enthaelt am Ende nur das Resultat. Bis 16 Agenten gleichzeitig, bis 1000 pro Run; resumebar; gespeicherte Workflows werden zu `/`-Commands.
- **Best Practice:** `ultracode: <aufgabe>` in den Prompt schreiben fuer einen Einzel-Run. Deaktivieren via `"disableWorkflows": true` oder `CLAUDE_CODE_DISABLE_WORKFLOWS=1`. Builtin `/deep-research` als Einstieg.
- **Quelle:** code.claude.com/docs/en/workflows `[offiziell]`

**2. `/effort ultracode` = xhigh + Auto-Orchestrierung (2.1.160)**
- **Was:** Kombiniert xhigh-Reasoning mit automatischer Workflow-Orchestrierung fuer jede substantielle Aufgabe der Session; setzt sich beim naechsten Start zurueck; nur auf xhigh-faehigen Modellen.
- **Best Practice:** Fuer Routine `/effort high` — ultracode verbraucht deutlich mehr Token.
- **Quelle:** code.claude.com/docs/en/workflows `[offiziell]`

**3. Horizont-Scan (Querverweise):** Stop/SubagentStop `additionalContext` (-> 01-hooks), `requiredMinimumVersion` (-> 07-settings), `/plugin list` (-> 04-plugins), Grep/Glob via `--tools` (-> 03-agents). `[offiziell]`

**Betrifft eigene Werkzeuge:** Das `ultracode`-Keyword ist der neue offizielle Trigger fuer Dynamic Workflows — die Parallelisierungs-Sektion in CLAUDE.md und `semicolon-task-separator.md` koennen es erwaehnen. `additionalContext`-Rueckgabe (2.1.163) ist relevant fuer `writeback-enforcer`/`memory-watchdog`.

---

# Cowork (Desktop-App) — Best Practices (fokussierter Lauf, Stand 2026-06-13)

> Fokussierter Best-Practices-Lauf zum **Cowork-Modus der Claude-Desktop-App** (nicht Teil des
> Claude-Code-Changelog-Deltas; eigener Versions-Anker = Stand der offiziellen Support-/Doku-Seiten,
> April–Juni 2026). Quellen-Rangordnung wie üblich: offiziell (support.claude.com, claude.com/docs,
> code.claude.com) = Grundwahrheit; extern nur ergänzend und gelabelt. Recherchiert mit 7 parallelen
> Researchern (Opus, je ~7–13 Quellen).
>
> **Status-Hinweis:** Cowork startete Januar 2026 als Research Preview; die Produktseite
> (claude.com/product/cowork) nennt es inzwischen "generally available", einzelne Teilfunktionen
> (Computer-Use, Handy/Dispatch) bleiben Research Preview. Offizielle Quellen sind hier leicht uneinheitlich.

## 1. Überblick & Einrichtung

- **Was Cowork ist:** bringt die agentische Architektur von Claude Code in die Desktop-App, ohne Terminal — für nicht-programmierende Wissensarbeit. Claude plant mehrstufige Aufgaben, zerlegt sie in Sub-Agenten und liefert fertige Outputs direkt ins Dateisystem. Quelle: support.claude.com/en/articles/13345190 · Stand 2026-06 · `[offiziell]`
- **Voraussetzungen:** nur Claude-Desktop-App für **macOS oder Windows** (kein Web, kein eigenständiges Mobile), bezahltes Abo (Pro, Max, Team, Enterprise), durchgehende Internetverbindung. App muss offen bleiben und der Rechner wach, sonst stoppt die Aufgabe. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Best Practice Einrichtung (4 Schritte):** 1) Cowork-Tab öffnen/App aktualisieren; 2) unter "Customize" die täglich genutzten Connectors aktivieren (+ optional Claude in Chrome); 3) einen **Arbeitsort** geben — lokaler Ordner ODER Projekt; 4) Aufgabe stellen. Je mehr verbunden, desto vollständiger Ende-zu-Ende. `/setup-cowork` startet die geführte Einrichtung. Quelle: claude.com/resources/tutorials/get-started-in-claude-cowork-in-three-steps · 2026-04-27 · `[offiziell]`
- **Ordner vs. Projekt:** Ordner = Claude liest/schreibt dort, eng oder breit zuschneidbar. Projekt = Workspace mit eigenen Dateien, Instruktionen und **Memory**, das über Sessions bleibt. Memory gibt es NUR in Projekten, nicht über Standalone-Sessions. Quelle: support.claude.com/en/articles/14116274 · 2026-04-09 · `[offiziell]`
- **Globale + Ordner-Instruktionen:** Unter Settings > Cowork globale Anweisungen (Tonfall, Format, Rolle) für jede Session; "Folder instructions" ergänzen projektspezifischen Kontext und kann Claude während der Session selbst aktualisieren. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Architektur (Sicherheits-Kern):** Agent-Loop + Dateioperationen laufen **nativ** auf dem Gerät (Permission-System auf App-Ebene); nur Shell/Code läuft in einer **isolierten Linux-VM** (Apple Virtualization.framework / Hyper-V). Fällt die VM aus, laufen Datei-/Web-Tools weiter, nur Shell/Code meldet "workspace unavailable". Quelle: support.claude.com/en/articles/14479288 · 2026-04-24 · `[offiziell]`
- **Berechtigungsmodi:** "Ask before acting" (empfohlen, fragt vor jeder Aktion) vs. "Act without asking" (schneller, riskanter, nur unter Aufsicht + vertrauten Quellen). In BEIDEN Modi fragt Claude immer vor endgültigem **Löschen**. Quelle: support.claude.com/en/articles/13345190 + /13364135 · `[offiziell]`

## 2. Skills & Plugins

- **Skill = Verzeichnis + SKILL.md**, dynamisch geladen über **Progressive Disclosure** (Stufe 1: nur Name+Beschreibung ~100 Token; Stufe 2: voller SKILL.md; Stufe 3: References/Skripte bei Bedarf). Quelle: claude.com/docs/skills/overview · `[offiziell]`
- **Voraussetzung:** "Code execution and file creation" muss aktiv sein (Settings > Capabilities; Team/Enterprise: Owner unter Organization settings > Skills). Quelle: support.claude.com/en/articles/12512180 · `[offiziell]`
- **SKILL.md-Pflichtfelder:** YAML-Frontmatter `name` (nur a-z/0-9/Bindestrich, = Verzeichnisname) + `description`. **Beschreibungs-Limit in Claude.ai = 200 Zeichen** (obwohl Agent-Skills-Standard 1024 erlaubt) — Trigger-Beschreibungen knapp und keyword-stark halten. Struktur mit `scripts/`, `references/`, `assets/`; SKILL.md < 500 Zeilen, Detail auslagern und in SKILL.md erwähnen. Quelle: claude.com/docs/skills/how-to · `[offiziell]`
- **Eigene Skills hochladen:** Customize > Skills > "+" > "Upload a skill" als **ZIP** (Ordnername = name-Feld, Skill-Ordner muss im ZIP enthalten sein), dann per Toggle aktivieren. Custom Skills sind privat zum Account. Quelle: support.claude.com/en/articles/12512180 · `[offiziell]`
- **Plugin = Paket** aus Skills + MCP-Connectors + Sub-Agents + Slash-Commands + Hooks; file-based (Markdown + JSON, kein Build). Plugins laufen in Cowork und Code, **nicht in Chat** (dort Hooks/Sub-Agents ausgegraut, Skills funktionieren). Quelle: claude.com/docs/cowork/guide/plugins + github.com/anthropics/knowledge-work-plugins · `[offiziell]`
- **Marketplaces:** Default "Knowledge Work" vorinstalliert; weitere Anthropic-Marktplätze (Financial Services, Legal, Life Sciences) und eigene **Git-Repos** als Marketplace (`owner/repo` oder URL) hinzufügbar. Limits: Plugin ≤200 MB/5.000 Dateien, ≤500 Plugins/Marketplace, ≤25 Marketplaces. Quelle: claude.com/docs/cowork/guide/plugins · `[offiziell]`
- **Plugins/Skills in Cowork selbst bauen:** Plugin "Plugin Create" / `cowork-plugin-management` führt durch Discovery→Planung→Design→Implementierung→Packaging und liefert eine installierbare `.plugin`-Datei. "Customize" an einem Plugin öffnet eine neue Cowork-Session, in der Claude die Skills auf die tatsächlich genutzten Tools umschreibt. **Nur Desktop** (bearbeitet lokale Dateien). Quelle: claude.com/resources/tutorials/how-to-customize-plugins-in-cowork · `[offiziell]`

## 3. Connectors & MCP

- **Grundprinzip:** Claude **erbt pro Person die Rechte des Quellsystems** — kein Zugriff dort = kein Zugriff hier. Quelle: support.claude.com/en/articles/11176164 · `[offiziell]`
- **Zwei Typen:** Remote-Connectors (Standard, überall inkl. Cowork, für Cloud/SaaS) vs. Desktop-Extensions (lokal, nur Claude Desktop + Code, NICHT Cowork-Web/claude.ai). Quelle: support.claude.com/en/articles/11725091 · 2026-04-15 · `[offiziell]`
- **Wichtig für Cowork:** Remote-Connectors laufen über **Anthropics Cloud**, nicht über das lokale Netz — ein eigener MCP-Server muss aus Anthropics IP-Ranges über das öffentliche Internet erreichbar sein (sonst Anthropic-IPs in der Firewall allowlisten). Quelle: support.claude.com/en/articles/11175166 · 2026-04-02 · `[offiziell]`
- **Gmail/Drive/Calendar Funktionsumfang:** Gmail = lesen/suchen, Entwürfe erstellen (**kein Versand durch Claude**), Labels/Threads; nur Anhang-Metadaten. Calendar = Events sehen/anlegen/ändern/löschen. Drive = Docs/Sheets/Slides/PDFs/Office lesen, Ordner/Upload; nur Textextraktion. Jede Aktion braucht Freigabe. Quelle: support.claude.com/en/articles/10166901 · `[offiziell]`
- **Auth:** delegiertes Pro-Nutzer-OAuth, keine Service-Accounts; org-weite Aktivierung macht den Connector nur verfügbar, jeder Nutzer authentifiziert sich selbst. Tokens verschlüsselt, pro Nutzer gescoped. Quelle: support.claude.com/en/articles/14503689 · 2026-06 · `[offiziell]`
- **Admin-Kontrollen (Team/Enterprise):** pro Connector "Always allow / Needs approval / Blocked" je Aktionskategorie, org-weit erzwungen. Bei 10+ aktiven Connectors "On demand"-Tool-Access nutzen, um Kontext zu sparen. Quelle: support.claude.com/en/articles/11176164 · `[offiziell]`

## 4. Datei-Arbeit & Ergebnis-Dokumente

- **Direktzugriff:** liest/schreibt in verbundenen Ordnern ohne Up-/Download; Ergebnisse landen direkt im Dateisystem. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Drei Mount-Modi pro Ordner:** read-only, read-write, read-write-no-delete — "Blast Radius" granular begrenzen. Quelle: anthropic.com/engineering/how-we-contain-claude · `[offiziell]`
- **Erzeugbare Formate:** Excel (.xlsx mit echten Formeln/VLOOKUP/bedingter Formatierung/mehreren Tabs), PowerPoint (.pptx), Word (.docx), PDF, PNG-Visualisierungen, Python-Skripte. Konvertierung und Multi-Step-Pipelines (CSV→Modell→Memo→Deck). Quelle: support.claude.com/en/articles/12111783 · 2026-04-29 · `[offiziell]`
- **Markdown "Edit with Claude":** Text markieren → gezielt an der Stelle editieren lassen, ohne die Passage im Thread zu beschreiben. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`
- **Best Practice:** dediziertes Arbeitsverzeichnis statt breitem Zugriff; sensible Dateien (Finanzen, Zugangsdaten, Personenakten) NICHT verbinden; wichtige Dateien sichern. Datei-Limit bei Erstellung 30 MB. Quelle: support.claude.com/en/articles/13364135 + /12111783 · `[offiziell]`

## 5. Geplante Aufgaben & Live-Artefakte

- **Scheduled Tasks** via `/schedule` oder Seitenleiste "Scheduled" > "+ New task". Kadenzen: stündlich, täglich, wöchentlich, werktags, manuell; Sonstiges (z.B. alle 6 h, Monatserster, einmaliger Lauf) per natürlicher Sprache. Cron in **lokaler Zeitzone**. Jeder Lauf startet **frisch ohne Erinnerung** → Prompt muss selbst-enthaltend sein (Connectors, Format, Präferenzen). Quelle: support.claude.com/en/articles/13854387 · 2026-04-09 + code.claude.com/docs/en/desktop-scheduled-tasks · `[offiziell]`
- **Catch-up-Fallstrick:** Beim Aufwachen/App-Start wird **genau ein** verpasster Lauf nachgeholt (zuletzt verpasster Zeitpunkt der letzten 7 Tage), ältere verworfen → eine 9-Uhr-Aufgabe kann um 23 Uhr laufen. Gegenmittel: Zeit-Guardrails in den Prompt ("nur heutige Daten; nach 17 Uhr überspringen"). Quelle: code.claude.com/docs/en/desktop-scheduled-tasks · `[offiziell]`
- **Live-Artefakte:** persistente, interaktive HTML-Seiten (Tracker, Dashboard), die sich beim Öffnen mit frischen Connector-/Datei-Daten aktualisieren; eigener "Live artifacts"-Tab mit Versionshistorie. Self-contained HTML, nur Chart.js/Grid.js/Mermaid per CDN erlaubt; Daten über `window.cowork.callMcpTool()`. Quelle: support.claude.com/en/articles/14729249 · 2026-04-24 · `[offiziell]`
- **Artefakt-Vorsicht:** Artefakte nutzen freigegebene Connectors **ohne erneute Rückfrage** → bei datenverändernden Connectors aufpassen. Artefakte sind lokal, (noch) nicht teilbar. Quelle: support.claude.com/en/articles/14729249 · `[offiziell]`

## 6. Computer-Steuerung & Browser

- **Tool-Hierarchie (Best Practice):** 1) dedizierter **Connector** (schnell, präzise, API) → 2) **Claude in Chrome** (Web-App ohne Connector, DOM-bewusst) → 3) **Computer Use** (native Desktop-Apps, App-übergreifend; breitestes, langsamstes Mittel). Quelle: support.claude.com/en/articles/14128542 · 2026-04-24 · `[offiziell]`
- **Computer Use:** Research Preview, **nur Pro/Max** (Team/Enterprise kein Zugriff). Aktivierung: Settings > General > "Computer use". **Tier-Modell:** Browser/Trading = "read" (nur sehen), Terminals/IDEs = "click" (nur klicken, kein Tippen), alles andere = "full". Keine Sandbox — läuft auf dem echten Desktop. Quelle: support.claude.com/en/articles/14128542 + code.claude.com/docs/en/computer-use · `[offiziell]`
- **Claude in Chrome:** Beta, alle Bezahlpläne, **nur Google Chrome**. In Cowork: Settings > Connectors > Claude in Chrome > Configure; pro Konversation manuell aktivieren. JavaScript-Ausführung braucht separate Pro-Domain-Freigabe (zentrale Schutzschicht). Pro: nur Haiku 4.5. Quelle: support.claude.com/en/articles/12012173 + /12902446 · `[offiziell]`
- **Link-Sicherheit (kritisch):** Web-Links in E-Mails/Nachrichten/Dokumenten gelten als verdächtig. **Niemals Links mit Computer-Use anklicken** — URL über die Chrome-MCP öffnen; volle URL vorher prüfen. Quelle: support.claude.com/en/articles/14128542 + MCP-Instruktionen · `[offiziell]`
- **Keine Finanztransaktionen:** Claude führt nie Trades/Orders/Überweisungen aus; Trading-/Krypto-Apps standardmäßig blockiert. Quelle: support.claude.com/en/articles/14128542 + /12902446 · `[offiziell]`

## 7. Grenzen, Datenschutz, Sicherheit

- **Compliance-Blindspot:** Cowork-Aktivität ist NICHT in Compliance API / Audit-Logs / Daten-Exporten erfasst (alle Pläne inkl. Enterprise). Ersatz nur via OpenTelemetry (Team/Enterprise) — laut Anthropic "kein Ersatz für Audit-Logging". EDR sieht nicht in die VM. Quelle: support.claude.com/en/articles/14479288 + /13455879 · `[offiziell]`
- **Lokale Datenhaltung:** Verlauf + Projektdaten (Tasks, Memory) liegen lokal, unterliegen NICHT der Standard-Retention und sind von Admins nicht zentral verwalt-/exportierbar. Task-Löschung: sofort aus Verlauf, Backend binnen 30 Tagen. Quelle: support.claude.com/en/articles/13455879 + /13345190 · `[offiziell]`
- **Admin-Kontrolle grob:** Cowork org-weit an/aus (Organization settings > Capabilities), keine native Pro-Nutzer-Granularität (Enterprise nur über Gruppen/Custom Roles). Geräte-Kontrollen (lokale MCP / Desktop-Extensions abschalten) nur via **MDM** (`isLocalDevMcpEnabled`, `isDesktopExtensionEnabled`). Quelle: support.claude.com/en/articles/13455879 + /14479288 · `[offiziell]`
- **Mehrschichtige Verteidigung:** RL-Training gegen bösartige Anweisungen, Content-Classifiers gegen Prompt-Injection, Lösch-Bestätigung, Computer-Use-Per-App-Freigabe. Anthropic betont ausdrücklich: "Risiko ist nicht null". Quelle: support.claude.com/en/articles/13364135 · `[offiziell]`
- **Höherer Verbrauch:** Cowork verbraucht deutlich mehr Usage als Chat (Sub-Agenten, viele Tool-Calls) → einfache Aufgaben im normalen Chat, verwandte Arbeit in einer Session bündeln. Quelle: support.claude.com/en/articles/13345190 · `[offiziell]`

## Stolpersteine / Fallstricke (Kurzliste)

- **App muss offen + Rechner wach** bleiben, sonst stoppen (auch geplante) Aufgaben. `[offiziell]`
- **Windows-Installationsfallen:** "VM service not running" (alter .exe/Squirrel-Installer statt MSIX bzw. gestoppter CoworkVMService) und "EXDEV: cross-device link not permitted" (VM-Image kreuzt Laufwerksgrenze → Speicherort auf C:\ zurücksetzen). `[offiziell]`
- **Prompt-Injection real:** PromptArmor demonstrierte ~48 h nach Launch eine Exfiltration über weißen 1-pt-Text in einem Word-Dokument (Upload von Finanzdaten via Files API). Bestätigt "Risiko nicht null". Quelle: winbuzzer.com / promptarmor.com · `[extern]`
- **Möglicher Bug (unbestätigt):** GitHub-Issue zu "Personal plugin skills not mounted in Cowork container despite enabled in UI". `[extern, nicht verifiziert]` — relevant für Franks Frage "sind meine Skills in Cowork installiert".

## Betrifft eigene Werkzeuge / Kopplung

- **Bug-Almanach-Rückkopplung:** Es gibt KEINEN `bugs/<kategorie>/cowork.md`-Almanach. Per Kopplungsregel A wird hier NICHT im Vorbeigehen ein halber Almanach angelegt — stattdessen Vorschlag, mit `bug-almanach-recherche` einen Cowork-Almanach zu erstellen (Kandidaten: Windows-Install-Fehler, Prompt-Injection, Catch-up-Timing, Skill-Mount-Bug).
- **Taxonomie:** Cowork wurde als Sammeleintrag in 12-neues abgelegt. Bei wachsender Bedeutung Promotion zu eigener Kategorie (z.B. `12-cowork`, "Neues" rückt auf 13) — siehe Vorschläge.
