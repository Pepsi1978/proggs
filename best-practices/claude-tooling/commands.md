# Slash-Commands — Best Practices (Stand 2026-06-05, Claude Code 2.1.165)

> Offizielle Quelle: https://code.claude.com/docs/en/slash-commands  
> Changelog: https://code.claude.com/docs/en/changelog  
> Commands-Referenz: https://code.claude.com/docs/en/commands
> Skills-Dokumentation: https://code.claude.com/docs/en/skills

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Commands = Skills | vereint seit 2.1.3; ein Skill schlaegt gleichnamigen Command | Unified Model |
| 2 | `$ARGUMENTS` | bricht bei mehrzeiligem Input; NIE ungefiltert in `` !`bash` `` (Injection) | Argument-Handling |
| 3 | automatische Ausloesung | ueber die `description` (Trigger front-loaden) | description-Feld |
| 4 | Plugin-Command aufrufen | voll-qualifiziert `/plugin:command` | Namespacing |
| 5 | Tool-Vorfreigabe | `allowed-tools` (Genehmigungs-Bypass); `disallowed-tools` zum Sperren | allowed-tools |
| 6 | viele Skills/Commands | `/doctor` (Beschreibungs-Budget) | Beschreibungs-Budget |

---

## Unified Model: Commands sind jetzt Skills (seit 2.1.3)

- **Was:** Seit Version 2.1.3 (Januar 2026) wurden Custom Slash-Commands und Skills zu einem einheitlichen System zusammengeführt. Eine Datei unter `.claude/commands/deploy.md` und eine Skill-Datei unter `.claude/skills/deploy/SKILL.md` erzeugen beide den Befehl `/deploy` — identisches Verhalten.
- **Best Practice:** Neue Commands als Skills anlegen (Ordner mit `SKILL.md`), nicht als einzelne `.md`-Datei in `commands/`. Bestehende Commands-Dateien funktionieren weiter — keine Migration erzwungen.
- **Vorteil von Skills gegenüber Commands:** Skills unterstützen unterstützende Dateien (Templates, Skripte, Referenzdokumente), Live Change Detection, `context: fork` für Subagent-Ausführung, und automatische Auslösung durch Claude ohne expliziten `/`-Aufruf.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell), https://github.com/anthropics/claude-code/issues/17578 (offiziell/Changelog)
- **Stand:** 2026-01-09 (Version 2.1.3)

---

## Speicherorte und Priorität

- **Was:** Commands/Skills können auf vier Ebenen gespeichert werden.

| Ebene       | Pfad                                         | Gilt für               |
|-------------|----------------------------------------------|------------------------|
| Enterprise  | Managed Settings (via Admin)                 | Alle Nutzer der Org    |
| Persönlich  | `~/.claude/skills/<name>/SKILL.md`           | Alle eigenen Projekte  |
| Projekt     | `.claude/skills/<name>/SKILL.md`             | Nur dieses Projekt     |
| Plugin      | `<plugin>/skills/<name>/SKILL.md`            | Wo Plugin aktiv ist    |

- **Best Practice:** Task-spezifische Commands (Deploy, Commit, Test) ins Projekt-Verzeichnis — sie werden automatisch ins Version-Control übernommen. Persönliche Produktivitäts-Commands in `~/.claude/skills/`. Enterprise-weite Standards per Managed Settings.
- **Priorität bei Namenskonflikten:** Enterprise > Persönlich > Projekt. Plugin-Skills nutzen Namespace `plugin-name:skill-name` — kein Konflikt möglich.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## Frontmatter-Felder: Vollständige Referenz (inkl. 2.1.152)

- **Was:** YAML-Frontmatter zwischen `---`-Markierungen am Anfang der `SKILL.md` konfiguriert das Verhalten des Commands/Skills.

```yaml
---
name: my-command
description: Was dieser Command tut und wann Claude ihn verwenden soll.
when_to_use: Zusätzliche Trigger-Phrasen, z.B. "wenn der Nutzer nach X fragt"
argument-hint: [issue-number] [priority]
arguments: [issue, priority]
disable-model-invocation: true
user-invocable: true
allowed-tools: Bash(git add *) Bash(git commit *)
disallowed-tools: AskUserQuestion WebSearch
model: claude-sonnet-4-5
effort: high
context: fork
agent: Explore
paths: "src/**/*.ts, packages/**"
shell: powershell
---
```

| Feld                       | Pflicht     | Beschreibung                                                                                          |
|----------------------------|-------------|-------------------------------------------------------------------------------------------------------|
| `name`                     | Nein        | Anzeigename; Standard: Verzeichnisname. Nur Kleinbuchstaben, Zahlen, Bindestriche (max 64 Zeichen)   |
| `description`              | Empfohlen   | Was der Skill tut. Claude liest dies, um zu entscheiden ob er den Skill lädt. Erstes Schlüsselwort vorn stellen |
| `when_to_use`              | Nein        | Zusätzlicher Kontext für Claude-Auslösung (Trigger-Phrasen). Wird an `description` angehängt; zusammen max 1.536 Zeichen |
| `argument-hint`            | Nein        | Platzhaltervorgabe in der Autovervollständigung, z.B. `[filename] [format]`. Nur kosmetisch — kein Parsing |
| `arguments`                | Nein        | Benannte Positionsargumente für `$name`-Substitution, z.B. `arguments: [issue, branch]`              |
| `disable-model-invocation` | Nein        | `true` = nur manuell per `/name` aufrufbar; Claude lädt den Skill nicht automatisch                  |
| `user-invocable`           | Nein        | `false` = nicht im `/`-Menü sichtbar; Claude kann trotzdem auslösen                                  |
| `allowed-tools`            | Nein        | Tools die ohne Genehmigungsdialog genutzt werden dürfen, wenn der Skill aktiv ist                    |
| `disallowed-tools`         | **NEU 2.1.152** | Tools die aus dem verfügbaren Pool entfernt werden, solange der Skill aktiv ist. Restriction gilt nur für den aktuellen Turn — beim nächsten eigenen Prompt wird sie aufgehoben. Accepts space-/comma-separated string oder YAML-Liste |
| `model`                    | Nein        | Modell-Override nur für diesen Turn; kehrt danach zur Session-Einstellung zurück                     |
| `effort`                   | Nein        | Effort-Override: `low`, `medium`, `high`, `xhigh`, `max`                                             |
| `context`                  | Nein        | `fork` = Skill läuft in isoliertem Subagenten                                                        |
| `agent`                    | Nein        | Welcher Subagent-Typ bei `context: fork`; z.B. `Explore`, `Plan`, `general-purpose`                 |
| `paths`                    | Nein        | Glob-Pattern; Claude lädt den Skill nur wenn passende Dateien bearbeitet werden                      |
| `shell`                    | Nein        | `bash` (Standard) oder `powershell` für `!`-Inline-Befehle                                          |

- **Quelle:** https://code.claude.com/docs/en/skills (offiziell), https://code.claude.com/docs/en/changelog (offiziell, 2.1.152)
- **Stand:** 2025-2026, `disallowed-tools` neu ab 2.1.152

---

## NEU 2.1.152: disallowed-tools — Tools für Skills sperren

- **Was:** `disallowed-tools` im Frontmatter entfernt Tools aus dem verfügbaren Pool, solange der Skill aktiv ist. Während `allowed-tools` Tools *erlaubt* (bypass Genehmigungsdialog), *sperrt* `disallowed-tools` Tools komplett.
- **Wichtig:** Die Sperre gilt nur für den aktuellen Turn (die Ausführung des Skills). Beim nächsten eigenen Prompt ist sie aufgehoben.

```yaml
---
name: background-loop
description: Führt automatisierte Checks ohne Nutzer-Interaktion durch.
context: fork
disallowed-tools: AskUserQuestion
---

Prüfe alle Tests und Linter-Regeln. Berichte nur, komm nicht zurück mit Fragen.
```

Weiteres Beispiel — Recherche-Skill ohne Web-Zugriff:

```yaml
---
name: local-review
description: Reviewt Änderungen im Working Tree, nur mit lokalem Kontext.
disallowed-tools: WebSearch WebFetch
allowed-tools: Read Bash(git diff *) Bash(git log *)
---
```

- **Best Practice:**
  - `AskUserQuestion` sperren bei autonomen Background-Loop-Skills die nicht pausieren sollen.
  - `WebSearch, WebFetch` sperren bei Skills die nur lokalen Code analysieren sollen (Konsistenz + Kosten).
  - `disallowed-tools` und `allowed-tools` sind orthogonal: `allowed-tools` ist eine Whitelist für den Genehmigungsbypass, `disallowed-tools` ist eine Blacklist die Tools komplett unsichtbar macht. Beide können gleichzeitig gesetzt werden.
  - Für Security-relevante Sperren (Skills die nie schreiben sollen): `Write, Edit, Bash` in `disallowed-tools` eintragen — sicherer als `context: fork` mit `agent: Explore` allein.
- **Pitfall:** Die Sperre gilt nur für den Turn — nicht für die gesamte Session. Nach Rückkehr aus dem Skill sind alle Tools wieder verfügbar.
- **Quelle:** https://code.claude.com/docs/en/skills (offiziell), https://code.claude.com/docs/en/changelog (offiziell, 2.1.152)
- **Stand:** ab Version 2.1.152 (2026-05-27)

---

## NEU 2.1.152: /reload-skills — Skills ohne Session-Neustart laden

- **Was:** `/reload-skills` scannt alle Skill-Verzeichnisse neu und lädt neu erstellte oder veränderte Skills in die aktuelle Session — ohne `exit` + Neustart.
- **Workflow:**
  1. Neue `SKILL.md` anlegen oder bestehende bearbeiten.
  2. `/reload-skills` tippen.
  3. Neuer Skill erscheint sofort im `/`-Menü und ist automatisch auslösbar.

- **Best Practice:**
  - Beim iterativen Skill-Entwickeln immer `/reload-skills` statt Session-Neustart — spart den Kontext.
  - Nach jeder Änderung am Frontmatter (z.B. neue `disallowed-tools`, neue `description`) neu laden.
  - Kombinierbar mit `/skills`-Menü: erst `/reload-skills`, dann `/skills` um den aktualisierten Stand zu sehen.

- **NEU: SessionStart-Hook kann Skills automatisch laden (2.1.152)**
  `SessionStart`-Hooks können `reloadSkills: true` zurückgeben und lösen damit einen automatischen Skill-Rescan aus. Das macht Skills die der Hook installiert in derselben Session sofort verfügbar — ohne `/reload-skills` tippen zu müssen.

  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "SessionStart",
      "reloadSkills": true
    }
  }
  ```

- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell, 2.1.152)
- **Stand:** ab Version 2.1.152 (2026-05-27)

---

## NEU 2.1.152: /code-review --fix — Review-Findings direkt anwenden

- **Was:** `/code-review --fix` führt einen Code-Review durch und wendet die Ergebnisse **automatisch auf den Working Tree an** — kein manuelles Copy-Paste der Vorschläge. Besonders für Reuse-, Vereinfachungs- und Effizienz-Verbesserungen.
- **`/simplify` ist jetzt ein Alias für `/code-review --fix`** — der vertraute Befehl funktioniert weiter, intern läuft aber der neue Mechanismus.

### Vergleich: Normaler Review vs. --fix-Modus

| Befehl | Was passiert | Wann sinnvoll |
|--------|-------------|---------------|
| `/code-review` | Review-Bericht, nur lesen | Code durchschauen, PR vorbereiten, Lernzweck |
| `/code-review --fix` | Review + automatische Anwendung auf Working Tree | Refactoring, Vereinfachung, Effizienz-Optimierung vor Commit |
| `/code-review ultra --fix` | Ultrareview (Cloud, teurer) + automatische Anwendung | Tiefe Qualitätssicherung vor wichtigen Releases |
| `/simplify` | Equivalent zu `/code-review --fix` | Legacy-Alias, weiter nutzbar |
| `/code-review --comment` | Review + Posting als GitHub-PR-Kommentare | Feedback im PR-Workflow teilen |

### Effort-Level kombinieren

```
/code-review --fix               # Session-Effort (Standard)
/code-review high --fix          # Mehr Findings, auch unsichere
/code-review low --fix           # Wenige, hochkonfidente Fixes
/code-review ultra --fix         # Ultrareview-Cloud + Working-Tree-Anwendung
```

Niedrigere Effort-Level → weniger aber sicherere Fixes. Höhere Levels → breitere Abdeckung, möglicherweise unsicherere Vorschläge.

### Workflow-Empfehlung (vor jedem Commit)

```
1. Entwicklung abgeschlossen, Änderungen im Working Tree
2. /code-review          # Erst nur lesen/verstehen
3. /code-review --fix    # Wenn Bericht sinnvoll war: Fixes anwenden
4. git diff              # Angewandte Fixes prüfen
5. git add / commit      # Geprüfte Fixes in Commit aufnehmen
```

- **Best Practice:**
  - Vor dem ersten Einsatz: `/code-review` (ohne `--fix`) laufen lassen um ein Gefühl für die Qualität der Vorschläge zu bekommen.
  - `--fix` nicht blind auf unverstandenem Code einsetzen — `git diff` nach dem Fix immer prüfen.
  - Bei automatisierten Workflows (CI, Hooks): `/code-review --comment` statt `--fix` bevorzugen — direkte Working-Tree-Änderung in CI kann zu unerwarteten Zuständen führen.
  - Für tiefe Reviews vor Releases: `/code-review ultra --fix` (aber: höhere Kosten, Cloud-Ausführung).
- **Pitfall:** `--fix` ändert den Working Tree direkt. Nicht gesicherte Änderungen können überschrieben werden — `git stash` oder Commit vor dem Einsatz empfohlen.
- **Quelle:** https://code.claude.com/docs/en/code-review (offiziell), https://code.claude.com/docs/en/changelog (offiziell, 2.1.152), https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/ (extern)
- **Stand:** ab Version 2.1.152 (2026-05-27); `/simplify` war vorher Standard-Befehl ab 2.1.147

---

## Argument-Handling: $ARGUMENTS, $N, benannte Argumente

- **Was:** Commands können Argumente entgegennehmen und in der Skill-Body verwenden.

```yaml
# Einfach: alle Argumente
/fix-issue 123
→ $ARGUMENTS → "123"

# Positionell: $ARGUMENTS[N] oder $N (0-basiert)
/migrate SearchBar React Vue
→ $0 = "SearchBar", $1 = "React", $2 = "Vue"

# Benannt (via arguments-Frontmatter)
# arguments: [issue, branch]
→ $issue = erstes Arg, $branch = zweites Arg

# Multi-Word-Argumente: in Anführungszeichen
/my-skill "hello world" second
→ $0 = "hello world", $1 = "second"
```

- **Best Practice:**
  - `$ARGUMENTS` für einfache Single-Argument-Commands.
  - `$ARGUMENTS[N]`/`$N` für Multi-Argument-Commands (klar und präzise).
  - Benannte Argumente (`arguments:`-Frontmatter + `$name`) für komplexe Commands mit >2 Argumenten — macht den Skill selbst-dokumentierend.
  - `argument-hint` IMMER setzen wenn der Command Argumente erwartet — erscheint in der Autovervollständigung als Vorschau.
  - Wenn kein `$ARGUMENTS` im Body: Claude Code hängt `ARGUMENTS: <Eingabe>` automatisch ans Ende an.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## description-Feld: Schlüssel für automatische Auslösung

- **Was:** Claude liest alle Skill-Beschreibungen beim Session-Start. Hat eine Nutzerfrage semantische Übereinstimmung, lädt Claude den Skill automatisch — ohne dass der Nutzer `/name` tippt.
- **Best Practice:**
  - Den wichtigsten Anwendungsfall **zuerst** nennen (nicht: "This skill helps with...").
  - Trigger-Phrasen, die Nutzer tatsächlich sagen würden, direkt verwenden: "Use when the user asks what changed, wants a commit message, or asks to review their diff."
  - `when_to_use` für zusätzliche Trigger-Phrasen nutzen (wird an `description` angehängt).
  - Gesamtlimit: 1.536 Zeichen für `description` + `when_to_use` zusammen.
  - Bei zu vielen Skills: `/doctor` zeigt ob das Beschreibungs-Budget überläuft und welche Skills betroffen sind.
- **Pitfall:** Zu generische Beschreibungen führen zu unkontrollierter automatischer Auslösung. Zu spezifische Beschreibungen verhindern, dass Claude den Skill erkennt.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## disable-model-invocation vs. user-invocable: Kontrolle wer auslöst

- **Was:** Zwei Frontmatter-Felder steuern wer/was einen Skill auslösen darf.

| Frontmatter                      | Nutzer kann `/` tippen | Claude löst automatisch aus | In Context geladen |
|----------------------------------|------------------------|-----------------------------|--------------------|
| (Standard)                       | Ja                     | Ja                          | Ja (Beschreibung)  |
| `disable-model-invocation: true` | Ja                     | Nein                        | Nein               |
| `user-invocable: false`          | Nein                   | Ja                          | Ja (Beschreibung)  |

- **Best Practice:**
  - **Deploy, Commit, Release:** `disable-model-invocation: true` — Claude soll nicht selbst entscheiden zu deployen.
  - **Hintergrundwissen/Konventionen:** `user-invocable: false` — Claude lädt automatisch wenn relevant, aber kein unsinniger `/legacy-context`-Befehl im Menü.
  - **Normale Workflows:** Standard-Einstellungen — Claude hilft, Nutzer kann übersteuern.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## allowed-tools: Genehmigungs-Bypass für Skills

- **Was:** `allowed-tools` im Frontmatter erlaubt spezifische Tool-Aufrufe ohne Genehmigungsdialog, solange der Skill aktiv ist.

```yaml
---
name: commit
description: Stage and commit the current changes
disable-model-invocation: true
allowed-tools: Bash(git add *) Bash(git commit *) Bash(git status *)
---
```

- **Best Practice:**
  - So spezifisch wie möglich formulieren — `Bash(git add *)` statt `Bash` (zu weit).
  - `allowed-tools` bei Projekt-Skills erst nach Workspace-Trust-Dialog aktiv (Sicherheitscheck bei fremden Repos).
  - Tool-**Verbote** → `disallowed-tools` (NEU ab 2.1.152) oder `/permissions` deny-Regeln. `allowed-tools` ist nur für Freigaben.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## Dynamischer Kontext: !`command`-Syntax

- **Was:** `!`-Präfix am Zeilenanfang in der `SKILL.md` führt einen Shell-Befehl aus **bevor** Claude den Skill sieht. Die Ausgabe ersetzt den Befehl direkt.

```yaml
---
name: summarize-changes
description: Summarizes uncommitted changes and flags anything risky.
---

## Current changes

!`git diff HEAD`

## Instructions
Summarize the changes above...
```

Mehrzeilige Befehle mit Fenced Block:

```
` ` `!
node --version
npm --version
git status --short
` ` `
```

- **Best Practice:**
  - Für Echtzeit-Kontext der sich ändert (git diff, aktuelle Logs, API-Status).
  - Ausgabe wird **einmal** vor Übermittlung an Claude eingefügt — kein rekursives Expandieren.
  - `!` muss am Zeilenanfang oder nach Leerzeichen stehen — mitten im Text (`KEY=!cmd`) wird ignoriert.
  - Auf Windows: `shell: powershell` im Frontmatter setzen + `CLAUDE_CODE_USE_POWERSHELL_TOOL=1`.
  - Kann per `"disableSkillShellExecution": true` in Settings organisationsweit deaktiviert werden.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## context: fork — Skills als Subagenten ausführen

- **Was:** `context: fork` lässt den Skill in einem isolierten Subagenten laufen. Der Skill-Body wird zum Prompt des Subagenten; Konversationshistorie ist nicht sichtbar.
- **Best Practice:**
  - Nur für Skills mit expliziten Aufgaben-Instruktionen verwenden — nicht für reine Referenz-Skills.
  - `agent: Explore` für Read-only-Recherche (lädt keine CLAUDE.md, kleiner Kontext).
  - `agent: Plan` für Planungsaufgaben ohne Side Effects.
  - `agent: general-purpose` (Standard) für allgemeine Tasks mit Schreibzugriff.
  - Mit `$ARGUMENTS` kombinieren: `/deep-research "async Rust patterns"` — Subagent recherchiert, Ergebnis kommt zurück in Hauptkonversation.
  - `disallowed-tools` (NEU 2.1.152) ist eine sicherere Ergänzung zu `context: fork` für autonome Skills: auch wenn der Subagent theoretisch Tools hat, sperrt `disallowed-tools` explizit benannte heraus.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## Eingebaute Commands: Übersicht (built-in vs. bundled Skills)

- **Was:** Claude Code unterscheidet zwischen echten Built-in-Commands (feste Logik, kein Prompt) und Bundled Skills (prompt-basiert, nutzbar wie eigene Skills).

### Wichtige Built-in-Commands (feste Logik)

| Command            | Funktion                                                              |
|--------------------|-----------------------------------------------------------------------|
| `/help`            | Listet alle verfügbaren Commands und Beschreibungen                   |
| `/clear`           | Löscht Konversationshistorie                                          |
| `/compact`         | Komprimiert Kontext (Zusammenfassung); erhält Skills mit Token-Budget |
| `/context`         | Zeigt Kontext-Window-Nutzung                                          |
| `/model`           | Wechselt das Modell für die Session                                   |
| `/effort`          | Ändert Effort-Level (`low`, `medium`, `high`, `xhigh`, `max`)         |
| `/permissions`     | Verwaltet Tool-Genehmigungen                                          |
| `/mcp`             | Zeigt/konfiguriert MCP-Server                                         |
| `/agents`          | Verwaltet Subagent-Konfigurationen                                    |
| `/tasks`           | Listet laufende Hintergrundaufgaben                                   |
| `/background`      | Trennt Session als Background Agent                                   |
| `/batch`           | Zerteilt große Änderungen in parallele Worktrees                      |
| `/plan`            | Wechselt in Plan-Mode vor großen Änderungen                           |
| `/memory`          | Verwaltet CLAUDE.md-Dateien                                           |
| `/init`            | Generiert Starter-CLAUDE.md für das Projekt                           |
| `/doctor`          | Diagnostiziert Konfigurationsprobleme (Skill-Budget, etc.)            |
| `/btw`             | Quick-Aside — geht nicht in Konversationshistorie                     |
| `/skills`          | Zeigt/verwaltet verfügbare Skills (Space zum Umschalten)              |
| `/reload-skills`   | **NEU 2.1.152** — Scannt Skill-Verzeichnisse neu ohne Session-Neustart |
| `/review`          | Code-Review (auch via Skill Tool aufrufbar)                           |
| `/security-review` | Sicherheits-Review                                                    |

### Bundled Skills (Prompt-basiert, wie eigene Skills)

| Skill                  | Funktion                                                              |
|------------------------|-----------------------------------------------------------------------|
| `/code-review`         | Code-Review mit detaillierten Instruktionen; `--fix` wendet Findings an Working Tree an (**NEU 2.1.152**) |
| `/simplify`            | Alias für `/code-review --fix` (**seit 2.1.152**; davor eigenständiger Befehl)           |
| `/debug`               | Systematisches Debugging                                              |
| `/loop`                | Wiederholte Ausführung                                                |
| `/claude-api`          | Claude-API-Nutzung                                                    |
| `/run`                 | App starten und Änderungen validieren (ab v2.1.145)                  |
| `/verify`              | Build + Lauf ohne Fallback auf Tests (ab v2.1.145)                   |
| `/run-skill-generator` | Einmal ausführen um `/run`/`/verify` projektspezifisch zu machen     |

- **Best Practice:** Bundled Skills sind anpassbar — eigene Skills können gleiche Namen überschreiben wenn nötig. `/run-skill-generator` einmal pro Projekt ausführen um zuverlässige Build-Rezepte zu erstellen. `/reload-skills` nach dem Überschreiben eines Bundled Skills aufrufen.
- **Quelle:** https://code.claude.com/docs/en/commands (offiziell), https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2026, `/reload-skills` neu ab 2.1.152

---

## Skill Content Lifecycle: Was nach dem Aufruf passiert

- **Was:** Wenn ein Skill aufgerufen wird, landet der gerenderte `SKILL.md`-Inhalt als einzelne Message in der Konversation und bleibt für die gesamte Session sichtbar. Claude liest die Datei **nicht erneut** bei späteren Turns.
- **Best Practice:**
  - Skill-Inhalte als "stehende Anweisungen" formulieren, nicht als Einmal-Schritte.
  - `SKILL.md` unter 500 Zeilen halten — Detailmaterial in Unterdateien auslagern.
  - Bei Auto-Compaction: Skills werden mit max 5.000 Token pro Skill re-attached (Budget: 25.000 Token gesamt). Ältere Skills können entfallen → Skill nach Compaction erneut aufrufen wenn nötig.
  - Für Deep-Reasoning in einem Skill: `ultrathink` irgendwo in den Skill-Body schreiben.
  - Skill geändert während der Session? → `/reload-skills` lädt die neue Version, aber der Skill-Content der aktuellen Session bleibt bis zum nächsten Aufruf erhalten.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## Skill-Beschreibungs-Budget: /doctor ist Pflicht bei vielen Skills

- **Was:** Alle Skill-Beschreibungen werden beim Session-Start in den Kontext geladen. Bei vielen Skills läuft das Budget über — wenig genutzte Skills verlieren ihre Beschreibung, was automatische Auslösung verhindert.
- **Best Practice:**
  - `/doctor` regelmäßig ausführen wenn >8-10 Skills installiert sind.
  - `skillListingBudgetFraction` in Settings erhöhen (Standard: 1% des Kontext-Windows; z.B. `0.02` für 2%).
  - Wenig genutzte Skills auf `"name-only"` in `skillOverrides` setzen — kein Beschreibungstext, aber noch im Menü.
  - `maxSkillDescriptionChars` anpassen (Standard-Cap pro Skill: 1.536 Zeichen).
  - Beschreibung mit wichtigstem Schlüsselwort beginnen — bei Kürzung geht das Ende verloren, nicht der Anfang.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell), https://perevillega.com/posts/2026-04-01-claude-code-skills-2-what-changed-what-works-what-to-watch-out-for/ (extern)
- **Stand:** 2026

---

## Skill-Sichtbarkeit: skillOverrides in Settings

- **Was:** `skillOverrides` in `.claude/settings.local.json` (oder Settings) steuert Skill-Sichtbarkeit ohne die `SKILL.md` selbst zu bearbeiten.

```json
{
  "skillOverrides": {
    "legacy-context": "name-only",
    "deploy": "off"
  }
}
```

| Wert                    | In Claude-Kontext     | Im `/`-Menü |
|-------------------------|-----------------------|-------------|
| `"on"` (Standard)       | Name + Beschreibung   | Ja          |
| `"name-only"`           | Nur Name              | Ja          |
| `"user-invocable-only"` | Versteckt             | Ja          |
| `"off"`                 | Versteckt             | Nein        |

- **Best Practice:** `/skills`-Menü öffnen → Skill markieren → `Space` zum Durchschalten der Zustände → `Enter` speichert in `.claude/settings.local.json`. Plugin-Skills sind von `skillOverrides` nicht betroffen — über `/plugin` verwalten.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## Namespacing bei Plugins

- **Was:** Plugin-Skills nutzen automatisch den Namespace `plugin-name:skill-name` um Konflikte zu verhindern.
- **Best Practice:** Beim direkten Aufruf den vollen Namen tippen: `/plugin-name:skill-name`. Im Menü werden eigene und Plugin-Skills gemeinsam angezeigt.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

---

## Wichtige Bugfixes in 2.1.x (relevant für Troubleshooting)

- **2.1.152:** `/code-review --fix` wendet Review-Findings auf Working Tree an; `/simplify` ist jetzt Alias dafür.
- **2.1.152:** `/reload-skills` hinzugefügt; SessionStart-Hooks können `reloadSkills: true` zurückgeben.
- **2.1.152:** `disallowed-tools` in Skill-Frontmatter — sperrt Tools für die Dauer des Skill-Turns.
- **2.1.149:** `argument-hint` wird bei Overflow nicht mehr abgeschnitten; Tab-Completion zeigt Hint auch wenn `name:` im Frontmatter vom Verzeichnisnamen abweicht.
- **2.1.147:** `/simplify` umbenannt zu `/code-review`; unbekannte Slash-Commands gaben in Headless/SDK-Modus kein Feedback — jetzt Error-Message.
- **2.1.136:** Argument-Namen mit Regex-Sonderzeichen (z.B. `+`, `*`) brachen die Substitution — gefixt.
- **2.1.120:** Slash-Command-Picker zeigt übereinstimmende Zeichen farblich hervorgehoben; lange Beschreibungen umbrechen statt abzuschneiden.
- **2.1.119:** `${CLAUDE_EFFORT}` steht in Skill-Body zur Verfügung (aktueller Effort-Level).
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2025-2026

---

## Typische Fallstricke

- **Pitfall 1 — Kein argument-hint gesetzt:** Nutzer wissen nicht was für Argumente erwartet werden. Lösung: immer `argument-hint: [was erwartet wird]` setzen.
- **Pitfall 2 — Zu generische description:** Claude löst den Skill bei jeder vagen Frage aus. Lösung: `disable-model-invocation: true` + explizite Aufrufe.
- **Pitfall 3 — YAML-Formatierung:** Formatierungstools (Prettier etc.) können YAML-Frontmatter umbrechen → Skill wird unsichtbar. Lösung: Frontmatter von Auto-Formatierung ausschließen.
- **Pitfall 4 — Skill zu groß:** Lange Skills bleiben permanent im Kontext, kosten bei jeder Session Tokens. Lösung: Body unter 500 Zeilen halten, Details in Unterdateien.
- **Pitfall 5 — context: fork ohne Task:** Subagent bekommt nur Referenzinhalte ohne Aufgabe → kehrt ohne Output zurück. Lösung: `context: fork` nur für Skills mit klaren Aktions-Instruktionen.
- **Pitfall 6 — /code-review --fix ohne Prüfung:** `--fix` ändert den Working Tree direkt. Lösung: immer `git diff` nach dem Einsatz prüfen, vorher `git stash` oder Commit.
- **Pitfall 7 — disallowed-tools für Session-weite Sperren:** `disallowed-tools` gilt nur für den aktuellen Turn, nicht für die Session. Lösung: Für dauerhafte Sperren `/permissions deny` verwenden.
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell), https://perevillega.com/posts/2026-04-01-claude-code-skills-2-what-changed-what-works-what-to-watch-out-for/ (extern), https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026

---

### Update 2026-06-05 (Claude Code 2.1.165) — Slash-Commands & Skill-Command-Bodies

**1. Autocomplete-Klick fuellt Prompt statt sofort auszufuehren (2.1.162)**
- **Was:** Klick auf einen Slash-Command im Autocomplete fuellt ihn in den Prompt (Enter zum Ausfuehren) statt sofort zu starten.
- **Best Practice:** Destruktive Commands (`/reset`, `/compact`) beruhigt anwaehlen und den befuellten Prompt kurz lesen, bevor Enter. Kein versehentliches Ausloesen durch Klick.
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**2. `\$`-Escape-Syntax in Skill-/Command-Bodies (2.1.163)**
- **Was:** In SKILL.md- und `.claude/commands/`-Bodies fuegt `\$` ein *literales* `$` vor einer Ziffer ein — ohne Expansion als Argument-Platzhalter (`$1`, `$ARGUMENTS[0]`).
- **Best Practice:** `\$1` schreiben, wenn ein literales `$1` im Prompt-Text/Code-Snippet stehen soll (Shell-Variablen, Preise wie `\$1.99`, Regex-Backreferences). Falsche Expansion ist ein STILLER Fehler (kein Error, falscher Output) — Skill-Bodies mit `$`+Ziffer pruefen.
- **Quelle:** code.claude.com/docs/en/skills `[offiziell]`

**3. `/btw` "c to copy" (2.1.163)** — Taste `c` kopiert die rohe Markdown-Antwort (statt gerenderten Text); nuetzlich zum Einfuegen in Markdown/Issues ohne Formatverlust. `[offiziell]`

**Betrifft eigene Werkzeuge:** Punkt 2 — alle Skills in `~/.claude/skills/` und `~/proggs/.claude/skills/` sowie `.claude/commands/`: pruefen, ob `$`+Ziffer (Shell-Snippets, Regex, Preise) faelschlich als Argument expandiert; ggf. `\$` setzen.
