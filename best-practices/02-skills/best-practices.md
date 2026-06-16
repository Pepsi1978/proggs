# Skills — Best Practices (Stand 2026-05-28, Claude Code 2.1.153)

> Quellen: code.claude.com/docs/en/skills (offiziell, Grundwahrheit), Agent-SDK-Doku,
> offizielles Changelog, docs.anthropic.com/en/docs/claude-code/cli-reference
> Externe/unbestätigte Angaben sind explizit markiert.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | `description` | einzeilig in Quotes, Trigger front-loaden, Kernzweck in ersten ~250 Zeichen | Description-Qualitaet |
| 2 | `paths:` im Skill | ENTFERNEN — macht den Skill undiscoverable | Frontmatter-Felder |
| 3 | Datei & Discovery | exakt `SKILL.md`, eine Ordnerebene; Discovery bei Session-Start (`/reload-skills`) | Speicherorte |
| 4 | viele Skills | Beschreibungs-Budget beachten (`SLASH_COMMAND_TOOL_CHAR_BUDGET`), `/doctor` | Description-Qualitaet |
| 5 | Detail auslagern | Progressive Disclosure (references/scripts), SKILL.md < 500 Zeilen | Progressive Disclosure |
| 6 | Skript-Pfad | `${CLAUDE_SKILL_DIR}` statt relativer Pfade | Skript-Referenzen |
| 7 | Tool-Sperre pro Skill | `disallowed-tools` (ab v2.1.152) | disallowed-tools |

---

## Überblick: Was sind Skills?

Skills sind wiederverwendbare Anweisungsblöcke, die Claude Code als `/skill-name`-Slash-Commands
zugänglich machen oder automatisch als Kontext laden. Sie liegen als `SKILL.md`-Dateien in
festgelegten Verzeichnissen und steuern über YAML-Frontmatter ihr Verhalten.

---

## Skill-Speicherorte (4 Ebenen, Prioritätsreihenfolge)

| Ebene | Pfad | Geltungsbereich |
|-------|------|-----------------|
| **Enterprise** | Via managed settings | Alle Benutzer der Organisation |
| **Global** | `~/.claude/skills/<name>/SKILL.md` | Alle Projekte |
| **Projekt** | `.claude/skills/<name>/SKILL.md` | Nur dieses Projekt |
| **Plugin** | Via Plugin-System | Wo Plugin aktiv ist |

Bei Namenskonflikten: Enterprise überschreibt Global, Global überschreibt Projekt.
Plugin-Skills nutzen `plugin-name:skill-name`-Namespace (kein Konflikt möglich).

---

## SKILL.md-Aufbau (Pflichtstruktur)

```markdown
---
name: mein-skill
description: Nutze diesen Skill wenn... (3. Person, präzise, auslösend)
---

# Inhalt

Hier steht was Claude tun soll wenn der Skill aktiviert wird.
```

Das YAML-Frontmatter zwischen `---` ist optional, aber `description` ist **stark empfohlen** —
ohne sie kann Claude den Skill nicht automatisch auswählen.

---

## Alle Frontmatter-Felder (vollständige Referenz, v2.1.153)

| Feld | Typ | Standard | Bedeutung |
|------|-----|----------|-----------|
| `name` | string | Verzeichnisname | Anzeigename des Skills (steuert nur Listing-Label, NICHT den Slash-Command-Namen — der kommt vom Verzeichnisnamen) |
| `description` | string | — | **Wichtigstes Feld.** Claude entscheidet anhand dessen ob der Skill passt. Muss in der 3. Person stehen. Max. 1.536 Zeichen für die Listing-Anzeige. |
| `when_to_use` | string | — | Erweiterte Trigger-Beschreibung; wird an `description` angehängt für die automatische Auswahl |
| `argument-hint` | string | — | Hilfetext der beim Tippen von `/skill-name ` im Terminal erscheint |
| `arguments` | list | — | Benannte Parameter die der Skill erwartet (`$name`-Substitution) |
| `disable-model-invocation` | bool | `false` | Bei `true`: kein LLM-Aufruf; Skill-Inhalt wird direkt als Text injiziert. Entfernt Skill auch aus der Listing-Kontext-Anzeige. |
| `user-invocable` | bool | `true` | Bei `false`: Skill erscheint NICHT in `/`-Autocomplete; kann aber von Claude oder anderen Skills aufgerufen werden |
| `allowed-tools` | list | alle | Werkzeuge die während des Skills ohne Bestätigung genutzt werden dürfen. Schränkt NICHT ein — andere Tools bleiben weiterhin nutzbar, brauchen aber normale Genehmigung. **Nur im CLI wirksam — im Agent SDK ignoriert.** |
| `disallowed-tools` | list | keine | **NEU in v2.1.152.** Werkzeuge die während des Skills NICHT verfügbar sind. Gut für autonome Skills die bestimmte Tools nie aufrufen sollen (z.B. `AskUserQuestion` in einem Hintergrund-Loop). Beschränkung endet beim nächsten Benutzer-Prompt. |
| `model` | string | Aktuelles Modell | Modell-Override für diesen Skill-Aufruf (z.B. `claude-opus-4-5`) |
| `effort` | string | Aktuelle Stufe | Effort-Override: `low`, `medium`, `high`, `xhigh`, `max` |
| `context` | string | — | `fork` = Skill läuft in eigenem isolierten Subagent-Context |
| `agent` | string | `general-purpose` | Subagent-Typ wenn `context: fork` gesetzt ist |
| `hooks` | map | — | Skill-lokale Lifecycle-Hooks (nur aktiv wenn dieser Skill läuft) |
| `paths` | list | — | Glob-Muster für pfad-spezifische Aktivierung (Skill nur geladen wenn passende Dateien bearbeitet werden) |
| `shell` | string/bool | — | `powershell` für Windows-`` !`cmd` ``-Blöcke; braucht `CLAUDE_CODE_USE_POWERSHELL_TOOL=1` |

### Kritischer Unterschied: `disable-model-invocation` vs. `user-invocable: false`

```yaml
# disable-model-invocation: true
# → Kein LLM-Aufruf, Inhalt direkt injiziert
# → Skill erscheint NICHT im Listing-Kontext (spart Tokens)
# → Ideal für: Templates, Boilerplate, reine Kontext-Injektion

# user-invocable: false
# → Kein /Autocomplete-Eintrag für den Benutzer
# → Claude kann den Skill trotzdem automatisch auswählen
# → Ideal für: interne Hilfsskills, automatische Kontext-Schichten
```

---

## NEU: `disallowed-tools` — Tool-Sperren pro Skill (ab v2.1.152)

**Quelle:** [Changelog code.claude.com](https://code.claude.com/docs/en/changelog) + [Skills-Doku](https://code.claude.com/docs/en/skills) — offiziell

### Was es tut

`disallowed-tools` entfernt bestimmte Tools aus dem verfügbaren Pool während ein Skill aktiv ist.
Im Gegensatz zu `allowed-tools` (Allowlist: pre-approved Tools) ist es eine **Denylist**:
alle anderen Tools bleiben verfügbar, nur die gelisteten werden gesperrt.

Die Sperre gilt für **die Dauer des Skill-Aufrufs** und endet automatisch beim nächsten Benutzer-Prompt.

### Verhältnis zu `allowed-tools`

| Feld | Mechanismus | Wirkung |
|------|-------------|---------|
| `allowed-tools` | Allowlist | Gelistete Tools: vorab genehmigt (kein Prompt). Andere Tools: weiterhin nutzbar, brauchen Genehmigung |
| `disallowed-tools` | Denylist | Gelistete Tools: komplett gesperrt. Alle anderen: verfügbar wie normal |

**Wichtig:** Laut offizieller Doku (Stand vor 2.1.152) hieß es: "To block a skill from using certain
tools, add deny rules in your permission settings instead." Ab v2.1.152 gibt es `disallowed-tools`
als direkten Skill-Level-Mechanismus dafür.

### Syntax

```yaml
---
name: background-monitor
description: Überwacht den Build-Status im Hintergrund
disallowed-tools:
  - AskUserQuestion
  - WebSearch
---
```

Auch als Leerzeichen- oder Komma-separierter String:
```yaml
disallowed-tools: AskUserQuestion WebSearch
# oder:
disallowed-tools: AskUserQuestion, WebSearch, Bash(rm *)
```

### Wann `disallowed-tools` sinnvoll ist

| Anwendungsfall | Welche Tools sperren |
|----------------|---------------------|
| Autonomer Hintergrund-Loop (soll nicht interagieren) | `AskUserQuestion` |
| Read-only Review-Skill (darf nichts schreiben) | `Write`, `Edit`, `MultiEdit` |
| Sicherheits-Audit (darf keinen Code ausführen) | `Bash`, `Computer` |
| Datenanalyse-Skill ohne Netzwerk | `WebSearch`, `WebFetch` |

### Bash-spezifische Sperren (Subcommand-Pattern)

```yaml
disallowed-tools:
  - Bash(rm *)
  - Bash(git push --force *)
  - Bash(sudo *)
```

Das Pattern `Bash(muster)` sperrt nur Bash-Aufrufe die auf das Muster passen,
nicht Bash insgesamt.

---

## NEU: `/reload-skills` und `reloadSkills: true` in SessionStart-Hooks (ab v2.1.152)

**Quelle:** [Changelog code.claude.com](https://code.claude.com/docs/en/changelog) — offiziell

### Das Problem das gelöst wird

Vor v2.1.152: Wenn ein SessionStart-Hook einen neuen Skill installiert hat, war dieser Skill
**erst in der nächsten Session** verfügbar (Claude Code musste neu gestartet werden).

Ab v2.1.152: Zwei Mechanismen lösen das:

### `/reload-skills` Command

Ein neuer eingebauter Slash-Command der die Skill-Verzeichnisse in der laufenden Session
neu einliest — **ohne Session-Neustart**:

```text
/reload-skills
```

**Wann sinnvoll:**
- Während der Skill-Entwicklung (SKILL.md oft ändern, sofort testen)
- Nach manuellem Installieren eines neuen Skills in einem Terminal-Fenster
- Nach Plugin-Aktivierung die neue Skills mitbringt
- Wenn ein Hook Skills installiert hat und man sie sofort nutzen möchte

**Hinweis:** Claude Code überwacht Skill-Verzeichnisse bereits auf Datei-Änderungen (Live-Change-Detection).
`/reload-skills` ist nützlich wenn ein **neues Skill-Verzeichnis** erstellt wurde, das beim
Session-Start noch nicht existiert hat.

### `reloadSkills: true` in SessionStart-Hooks

SessionStart-Hooks können jetzt im JSON-Output `reloadSkills: true` zurückgeben, um Claude Code
anzuweisen, die Skill-Verzeichnisse nach dem Hook-Run neu zu scannen:

```json
{
  "reloadSkills": true
}
```

**Anwendungsfall — Hook installiert Skills dynamisch:**

```powershell
# SessionStart-Hook: Skill aus Repo klonen wenn nicht vorhanden
$skillDir = "$HOME/.claude/skills/mein-neuer-skill"
if (-not (Test-Path $skillDir)) {
    git clone https://example.com/skills/mein-neuer-skill.git $skillDir
    # Reload-Signal zurückgeben:
    Write-Output '{"reloadSkills": true}'
} else {
    Write-Output '{}'
}
```

```bash
# Bash-Variante:
SKILL_DIR="$HOME/.claude/skills/mein-neuer-skill"
if [ ! -d "$SKILL_DIR" ]; then
    git clone https://example.com/skills/mein-neuer-skill.git "$SKILL_DIR"
    echo '{"reloadSkills": true}'
else
    echo '{}'
fi
```

**Warum das wichtig ist:**
- Skills die der Hook installiert sind **in derselben Session** sofort nutzbar
- Kein "bitte starte Claude Code neu nach der Installation"
- Automatisches Setup-Pattern: Hook prüft Voraussetzungen, installiert fehlende Skills, Reload erfolgt automatisch

---

## String-Substitutionen in Skills

Claude Code ersetzt diese Variablen automatisch im Skill-Inhalt bevor er Claude übergeben wird:

| Variable | Bedeutung |
|----------|-----------|
| `$ARGUMENTS` | Alle Argumente als ein String |
| `$ARGUMENTS[N]` | N-tes Argument (0-basiert) |
| `$N` | Kurzform für `$ARGUMENTS[N-1]` (1-basiert, also `$1` = erstes Argument) |
| `$name` | Wert des benannten Arguments `name` aus der `arguments:`-Liste |
| `${CLAUDE_SESSION_ID}` | Aktuelle Session-ID |
| `${CLAUDE_EFFORT}` | Aktuelle Effort-Stufe |
| `${CLAUDE_SKILL_DIR}` | **Absoluter Pfad zum Skill-Verzeichnis** — ermöglicht stabile Skript-Referenzen unabhängig vom Install-Level |

### `${CLAUDE_SKILL_DIR}` richtig nutzen

```markdown
---
name: mein-tool
description: Führt mein Analyse-Skript aus
---

Führe dieses Skript aus:
!`python "${CLAUDE_SKILL_DIR}/analyse.py" $ARGUMENTS`
```

So funktioniert der Pfad unabhängig davon ob der Skill global (`~/.claude/skills/`) oder
projektlokal (`.claude/skills/`) installiert ist.

---

## Description-Qualität: Trigger-Phrasen richtig schreiben

Die `description` ist das wichtigste Feld. Claude liest alle Skill-Descriptions und
entscheidet anhand semantischer Ähnlichkeit welcher Skill passt.

**Schlechte Description (zu vage):**
```yaml
description: Hilft bei Code-Aufgaben
```

**Gute Description (spezifisch, auslösend, 3. Person):**
```yaml
description: >
  Nutze diesen Skill wenn der Benutzer einen Code-Review für Kotlin/Android-Code
  anfordert, nach Sicherheitslücken fragt oder Verbesserungen am Clean-Architecture-
  Muster wünscht. Deckt ab: ktfmt-Formatierung, detekt-Regeln, Coroutine-Patterns.
```

**Regeln für gute Descriptions:**
- **3. Person**: "Nutze diesen Skill wenn..." oder "Dieser Skill wird verwendet für..."
- **Konkrete Auslöser**: Welche Phrasen, Anfragen, Situationen aktivieren den Skill?
- **Abgrenzung**: Was deckt dieser Skill ab, was nicht? (verhindert False-Positives)
- **Max. 1.536 Zeichen** für den Listing-Kontext (längere werden abgeschnitten)
- `when_to_use` für zusätzlichen Trigger-Text verwenden (wird angehängt, zählt zum Budget)

---

## Aufruf-Kontrolle: Wer darf den Skill nutzen?

| Szenario | Konfiguration |
|----------|--------------|
| Slash-Command für Benutzer + automatisch | Kein extra Feld nötig (Standard) |
| Nur automatisch durch Claude (kein `/menu`) | `user-invocable: false` |
| Direkte Text-Injektion ohne LLM-Aufruf | `disable-model-invocation: true` |
| Im eigenen Subagent-Context isoliert | `context: fork` |
| Bestimmte Tools pre-approvt | `allowed-tools: [Read, Glob, Bash]` |
| Bestimmte Tools blockiert | `disallowed-tools: [AskUserQuestion, Write]` (**NEU v2.1.152**) |
| Nur für bestimmte Modelle | `model: claude-opus-4-5` |

---

## `context: fork` — Wann und wie

```yaml
---
name: tiefe-analyse
description: Führe eine tiefe, isolierte Code-Analyse durch ohne den Hauptchat zu belasten
context: fork
agent: general-purpose
model: claude-opus-4-5
effort: high
---

Analysiere den gesamten Code auf...
```

**Wann `context: fork` nutzen:**
- Skill generiert viel Output der den Hauptkontext "verschmutzen" würde
- Skill braucht eigenes Modell oder Effort-Level
- Skill soll als eigenständiger Subagent laufen (z.B. Recherche, Deep-Dive)
- Skill ruft weitere Tools extensiv auf

**Verfügbare `agent:`-Typen:** `general-purpose`, `explorer`, `planner`, eigene Custom Agents

---

## Dynamische Kontext-Injection: `` !`cmd` ``-Syntax

Skills können Shell-Befehle einbetten, die beim Laden ausgeführt werden:

```markdown
---
name: git-context
description: Gibt Claude den aktuellen Git-Kontext
---

Aktueller Branch: !`git branch --show-current`
Letzte 5 Commits: !`git log --oneline -5`
Offene Änderungen: !`git status --short`
```

**Voraussetzungen:**
- `shell: true` im Frontmatter (oder globale Shell-Execution erlaubt)
- Policy-Setting `disableSkillShellExecution: true` blockiert alle Shell-Ausführungen
- Auf Windows: `shell: powershell` + Env-Variable `CLAUDE_CODE_USE_POWERSHELL_TOOL=1`

**Best Practice:** Dynamische Injection für Kontext der sich ändert (Branch, Datum, Status).
Statischer Inhalt gehört direkt in den Skill-Body.

---

## `allowed-tools`: Tool-Vorfreigaben

```yaml
---
name: code-search
description: Durchsucht den Code semantisch und zeigt Ergebnisse
allowed-tools:
  - Read
  - Glob
  - Grep
  - WebSearch
---
```

**Wichtig:** `allowed-tools` funktioniert **nur im Claude Code CLI**. Im Agent SDK wird das
Feld ignoriert — dort gelten die Tool-Einstellungen des SDK-Aufrufs.

Wenn `allowed-tools` gesetzt ist, werden nur die gelisteten Tools pre-approved. Andere Tools
können trotzdem genutzt werden, brauchen aber normale Bestätigung (oder `bypassPermissions`).

---

## `${CLAUDE_SKILL_DIR}`: Skript-Referenzen richtig lösen

**Problem:** Ein Skill in `~/.claude/skills/mein-skill/SKILL.md` referenziert
`scripts/run.sh` — aber der absolute Pfad ist je nach Install-Level unterschiedlich.

**Lösung:**
```markdown
!`bash "${CLAUDE_SKILL_DIR}/scripts/run.sh" $ARGUMENTS`
```

`${CLAUDE_SKILL_DIR}` löst sich immer zum Verzeichnis der SKILL.md auf, unabhängig von:
- Global vs. projektlokal
- Verschiedenen Betriebssystemen
- Verschiedenen Benutzer-Pfaden

---

## Progressive Disclosure: Unterverzeichnis-Struktur

Bei komplexen Skills nicht alles in die SKILL.md packen — stattdessen Hilfsdateien
referenzieren:

```
~/.claude/skills/mein-skill/
  SKILL.md           ← Haupt-Skill mit Überblick
  scripts/
    analyse.sh       ← Wird via ${CLAUDE_SKILL_DIR} referenziert
  references/
    api-guide.md     ← Via paths: eingebunden
    security-rules.md ← Via paths: eingebunden
```

```yaml
---
name: mein-skill
description: Komplexer Skill mit Unterstruktur
paths:
  - references/api-guide.md
  - references/security-rules.md
---

Hier der Kerninhalt. Die Referenzen sind bereits geladen.
```

**Vorteil:** Die `paths:`-Dateien werden nur geladen wenn der Skill aktiv ist — spart
Token-Budget wenn der Skill gerade nicht gebraucht wird.

> **Hinweis:** Die offizielle Doku verwendet `paths:` (v2.1.153) für Glob-Muster zur
> pfad-spezifischen Aktivierung. Die Semantik "Hilfsdateien automatisch laden" sollte
> via Markdown-Links im Skill-Body umgesetzt werden (Claude lädt sie bei Bedarf).

---

## Skill-Inhalt: Lebenszyklus und Komprimierung

| Aspekt | Wert |
|--------|------|
| **Token-Limit pro Skill** | 5.000 Tokens (nach Compaction: erste 5.000 Tokens werden re-attached) |
| **Kombiniertes Budget** | 25.000 Tokens für alle aktiven Skills nach Compaction |
| **Listing-Budget** | ~1% des Kontextfensters (steuerbar via `skillListingBudgetFraction`) |
| **Nach Compaction** | Skill-Inhalte werden automatisch wieder eingebettet (neueste zuerst) |
| **Live-Updates** | Skills werden bei Datei-Änderung sofort neu geladen (kein Neustart nötig) |
| **Description-Limit** | 1.536 Zeichen in der Listing-Ansicht |

---

## Eingebaute/Gebündelte Skills (ab v2.1.145)

| Skill | Funktion | Seit |
|-------|----------|------|
| `/run` | Shell-Befehle ausführen und Output anzeigen | v2.1.145 |
| `/verify` | Assertions über aktuellen Codestand verifizieren | v2.1.145 |
| `/run-skill-generator` | Neue Skills interaktiv erstellen | v2.1.145 |
| `/code-review` | Vollständiger Code-Review-Workflow | eingebaut |
| `/batch` | Parallele Aufgaben in eigenen Worktrees | eingebaut |
| `/debug` | Strukturierter Debug-Workflow | eingebaut |
| `/loop` | Iterativer Verbesserungs-Loop | eingebaut |
| `/claude-api` | Claude API-Nutzung direkt aus Skills | eingebaut |
| `/reload-skills` | Skill-Verzeichnisse ohne Session-Neustart neu einlesen | **v2.1.152** |

---

## skill-creator: Skills mit Claude erstellen

Der Skill Creator (`skill-creator:skill-creator`) führt durch den Erstellungsprozess:

1. **Trigger:** "erstelle einen Skill", "neuer Skill", "baue einen Skill"
2. **Prozess:** Der Skill Creator stellt Fragen und generiert SKILL.md
3. **Qualitätsprüfung:** Checkt Frontmatter, Description, Beispiele
4. **Ausgabe:** Fertiger Skill im richtigen Verzeichnis

**Pflicht laut CLAUDE.md:** Niemals Skills manuell erstellen ohne den Skill Creator zu nutzen.

---

## Naming Conventions

| Aspekt | Empfehlung |
|--------|-----------|
| **Verzeichnisname** | Kleinbuchstaben, Bindestriche: `code-review`, `db-migrator` |
| **`name`-Feld** | Nur Anzeige-Label — NICHT der Command-Name |
| **Namespace via Plugin** | `pluginname:skillname` (Doppelpunkt als Trenner) |
| **Projekt-Namespace** | `.claude/skills/team-skill/` — klar vom globalen trennen |
| **Keine Sonderzeichen** | Nur a-z, 0-9, `-`, `_` |

---

## Skill-Sichtbarkeit aus Settings überschreiben (`skillOverrides`)

Ohne die SKILL.md zu ändern lässt sich ein Skill in `settings.json` steuern:

```json
{
  "skillOverrides": {
    "skill-name": "off",
    "anderer-skill": "name-only",
    "dritter-skill": "user-invocable-only"
  }
}
```

| Wert | Bedeutung |
|------|-----------|
| `"on"` | Normal sichtbar (Standard) |
| `"name-only"` | Nur Name im Listing, keine Beschreibung |
| `"user-invocable-only"` | Nur in Benutzer-Autocomplete, nicht auto-aktiviert |
| `"off"` | Skill komplett deaktiviert |

**Budget-Settings:**
```json
{
  "skillListingBudgetFraction": 0.3,
  "maxSkillDescriptionChars": 200
}
```

---

## Neuerungen der letzten 6 Monate (Nov 2025 – Mai 2026)

| Version / Zeitraum | Änderung |
|--------------------|---------|
| **v2.1.153 (aktuell)** | Stable: alle unten genannten Features |
| **v2.1.152** | `disallowed-tools` Frontmatter: Tools pro Skill blockieren |
| **v2.1.152** | `/reload-skills` Command: Skill-Dirs ohne Neustart neu einlesen |
| **v2.1.152** | `reloadSkills: true` in SessionStart-Hooks: Skills sofort nach Installation verfügbar |
| **v2.1.150** | Vorige stabile Basis |
| **v2.1.145** | Eingebaute Skills: `/run`, `/verify`, `/run-skill-generator` |
| **v2.1.100+** | `paths:` Frontmatter für pfad-spezifische Aktivierung |
| **v2.1.63** | `context: fork` + `agent:` stabil für Subagent-Isolation |
| **Anfang 2026** | `hooks:` Frontmatter für skill-lokale Lifecycle-Hooks |
| **Anfang 2026** | `shell: powershell` für Windows `` !`cmd` ``-Blöcke |
| **Anfang 2026** | `${CLAUDE_SKILL_DIR}`, `${CLAUDE_SESSION_ID}`, `${CLAUDE_EFFORT}` String-Substitutionen |
| **Jan 2026** | SKILL.md-Format stabilisiert, agentskills.io Cross-Tool-Standard |
| **Dez 2025** | `.claude/rules/` Verzeichnis + Background-Kontext eingeführt |

---

## Anti-Patterns

| Anti-Pattern | Problem | Besser |
|-------------|---------|--------|
| Kein `description`-Feld | Claude wählt den Skill nie automatisch | Immer `description` setzen |
| Zu vage Description ("hilft mit Code") | False-Positives und Missed-Triggers | Konkrete Auslöser-Phrasen |
| Alles in SKILL.md packen | Überschreitet 5.000-Token-Limit | Markdown-Links auf Hilfsdateien |
| Hardcoded Skript-Pfade | Bricht bei anderem Install-Level | `${CLAUDE_SKILL_DIR}` verwenden |
| `allowed-tools` im Agent SDK | Wird ignoriert, gibt falsches Sicherheitsgefühl | SDK-Tool-Settings nutzen |
| `disable-model-invocation: true` für interaktive Skills | Kein LLM = keine Anpassung an Kontext | Nur für statische Kontext-Injektion |
| Skill ohne `user-invocable: false` der intern bleiben soll | Erscheint im Benutzer-Menü | `user-invocable: false` setzen |
| Shell-Injection ohne `shell: true` | Befehl wird als Literal angezeigt | Frontmatter-Flag setzen |
| `disallowed-tools` setzen aber `allowed-tools` auch aktiv | Verhalten unklar (beide wirken) | Bewusst entscheiden: Allowlist ODER Denylist |

---

## Skill-Zugriff aus dem Agent SDK

Beim Einsatz via Agent SDK (Python/TypeScript) gelten andere Regeln:

- Skills werden via `settingSources` aus dem Dateisystem geladen
- `allowed-tools` Frontmatter wird **ignoriert** (SDK-seitige Tool-Konfiguration gilt)
- `disallowed-tools` Frontmatter: Verhalten im Agent SDK **nicht offiziell dokumentiert** (Stand 2026-05-28)
- `context: fork` funktioniert (Subagent-Isolation)
- `disable-model-invocation: true` funktioniert
- Shell-Execution (`` !`cmd` ``) ist standardmäßig deaktiviert (`disableSkillShellExecution`)

---

## Checkliste vor Skill-Veröffentlichung

- [ ] `description` gesetzt, in 3. Person, konkrete Auslöser-Phrasen enthalten?
- [ ] `when_to_use` für Grenzfälle/Abgrenzung ergänzt?
- [ ] Skill-Inhalt unter 5.000 Tokens? (Umfangreiche Refs als Hilfsdateien)
- [ ] Skript-Referenzen via `${CLAUDE_SKILL_DIR}` (nicht Hardcoded-Pfade)?
- [ ] `user-invocable: false` wenn der Skill intern bleiben soll?
- [ ] `context: fork` wenn der Skill den Hauptkontext isolieren soll?
- [ ] `allowed-tools` korrekt gesetzt (und bewusst, dass im SDK ignoriert)?
- [ ] `disallowed-tools` gesetzt wo Skill autonomes Verhalten hat? (**NEU v2.1.152**)
- [ ] Shell-Blöcke via `shell: true` / `shell: powershell` freigegeben?
- [ ] Naming Convention eingehalten (Kleinbuchstaben, Bindestriche)?
- [ ] Mit `skill-creator` erstellt oder geprüft?

---

> **Quellen (Stand 2026-05-28):**
> - [Skills — code.claude.com/docs/en/skills](https://code.claude.com/docs/en/skills) — **offiziell**
> - [Changelog — code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) — **offiziell**
> - [Agent SDK Skills — docs.anthropic.com](https://docs.anthropic.com/en/docs/claude-code/agent-sdk/skills) — **offiziell**
> - [CLI Reference — docs.anthropic.com](https://docs.anthropic.com/en/docs/claude-code/cli-reference) — **offiziell**
> - [SKILL.md Spec (agensi.io)](https://www.agensi.io/learn/skill-md-format-reference) — extern (nicht für v2.1.152-Features aktualisiert, Stand 2026-05-28)
> - [releasebot.io Claude Code Updates May 2026](https://releasebot.io/updates/anthropic/claude-code) — extern
