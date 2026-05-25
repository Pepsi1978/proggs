# Slash-Commands — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Offizielle Quelle: https://code.claude.com/docs/en/slash-commands  
> Changelog: https://code.claude.com/docs/en/changelog  
> Commands-Referenz: https://code.claude.com/docs/en/commands

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

## Frontmatter-Felder: Vollständige Referenz

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
| `model`                    | Nein        | Modell-Override nur für diesen Turn; kehrt danach zur Session-Einstellung zurück                     |
| `effort`                   | Nein        | Effort-Override: `low`, `medium`, `high`, `xhigh`, `max`                                             |
| `context`                  | Nein        | `fork` = Skill läuft in isoliertem Subagenten                                                        |
| `agent`                    | Nein        | Welcher Subagent-Typ bei `context: fork`; z.B. `Explore`, `Plan`, `general-purpose`                 |
| `paths`                    | Nein        | Glob-Pattern; Claude lädt den Skill nur wenn passende Dateien bearbeitet werden                      |
| `shell`                    | Nein        | `bash` (Standard) oder `powershell` für `!`-Inline-Befehle                                          |

- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2025-2026

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
  - Tool-**Verbote** über `/permissions` deny-Regeln, nicht über `allowed-tools` (das ist nur für Freigaben).
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
| `/review`          | Code-Review (auch via Skill Tool aufrufbar)                           |
| `/security-review` | Sicherheits-Review                                                    |

### Bundled Skills (Prompt-basiert, wie eigene Skills)

| Skill                  | Funktion                                                              |
|------------------------|-----------------------------------------------------------------------|
| `/code-review`         | Code-Review mit detaillierten Instruktionen                           |
| `/debug`               | Systematisches Debugging                                              |
| `/loop`                | Wiederholte Ausführung                                                |
| `/claude-api`          | Claude-API-Nutzung                                                    |
| `/run`                 | App starten und Änderungen validieren (ab v2.1.145)                  |
| `/verify`              | Build + Lauf ohne Fallback auf Tests (ab v2.1.145)                   |
| `/run-skill-generator` | Einmal ausführen um `/run`/`/verify` projektspezifisch zu machen     |

- **Best Practice:** Bundled Skills sind anpassbar — eigene Skills können gleiche Namen überschreiben wenn nötig. `/run-skill-generator` einmal pro Projekt ausführen um zuverlässige Build-Rezepte zu erstellen.
- **Quelle:** https://code.claude.com/docs/en/commands (offiziell), https://code.claude.com/docs/en/slash-commands (offiziell)
- **Stand:** 2026

---

## Skill Content Lifecycle: Was nach dem Aufruf passiert

- **Was:** Wenn ein Skill aufgerufen wird, landet der gerenderte `SKILL.md`-Inhalt als einzelne Message in der Konversation und bleibt für die gesamte Session sichtbar. Claude liest die Datei **nicht erneut** bei späteren Turns.
- **Best Practice:**
  - Skill-Inhalte als "stehende Anweisungen" formulieren, nicht als Einmal-Schritte.
  - `SKILL.md` unter 500 Zeilen halten — Detailmaterial in Unterdateien auslagern.
  - Bei Auto-Compaction: Skills werden mit max 5.000 Token pro Skill re-attached (Budget: 25.000 Token gesamt). Ältere Skills können entfallen → Skill nach Compaction erneut aufrufen wenn nötig.
  - Für Deep-Reasoning in einem Skill: `ultrathink` irgendwo in den Skill-Body schreiben.
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

- **2.1.149:** `argument-hint` wird bei Overflow nicht mehr abgeschnitten; Tab-Completion zeigt Hint auch wenn `name:` im Frontmatter vom Verzeichnisnamen abweicht.
- **2.1.147:** Unbekannte Slash-Commands gaben in Headless/SDK-Modus kein Feedback — jetzt Error-Message.
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
- **Quelle:** https://code.claude.com/docs/en/slash-commands (offiziell), https://perevillega.com/posts/2026-04-01-claude-code-skills-2-what-changed-what-works-what-to-watch-out-for/ (extern)
- **Stand:** 2026
