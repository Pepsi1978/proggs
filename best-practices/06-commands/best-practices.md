# Claude Code Best Practices — Slash-Commands & Skills

**Stand:** 2026-05-25 | **Zielversion:** Claude Code v2.1.150
**Quellen:** code.claude.com/docs (offiziell), Changelog v2.1.100–v2.1.150

---

## 1. Slash-Commands → Skills: Die Grundlegende Vereinheitlichung

Seit ca. v2.1.100 sind Custom Slash-Commands und Skills zu einem einheitlichen System
zusammengeführt. Das `/slash-commands`-Docs-URL leitet auf die Skills-Seite weiter.

**Kurzfassung:**
- `.claude/commands/` (alt) → Rückwärtskompatibel, weiter nutzbar
- `.claude/skills/<name>/SKILL.md` (neu, empfohlen) → Mehr Fähigkeiten, klarer strukturiert
- Beide Formate werden erkannt und funktionieren parallel

**Woher kommt der Name "Skills"?** Claude Code implementiert den offenen
[Agent Skills Standard](https://agentskills.io) — eine Spezifikation für austauschbare
KI-Fähigkeiten zwischen verschiedenen Systemen.

---

## 2. SKILL.md-Format: Vollständige Referenz

### Dateistruktur

```markdown
---
name: mein-skill
description: >
  Wann dieser Skill aufgerufen werden sollte. Dieser Text erscheint
  im /skills-Menü und wird für die Auto-Invociation verwendet.
  (Max 1.536 Zeichen)
# Alle weiteren Felder sind OPTIONAL:
model: claude-sonnet-4-5          # Welches Modell verwenden
effort: high                       # low|medium|high|xhigh
user-invocable: true               # Erscheint im /skills-Menü (default: true)
disable-model-invocation: false    # true = nur Shell ausführen, kein LLM
allowed-tools:                     # Tool-Whitelist (empty = alle erlaubt)
  - Bash
  - Read
  - Write
paths:                             # Kontextdateien automatisch einlesen
  - src/
  - docs/ARCHITECTURE.md
argument-hint: "[datei] [--flag]"  # Hint für Argument-Eingabe
arguments:                         # Benannte Argumente mit Defaults
  - name: datei
    description: Die zu prüfende Datei
    default: "src/main.ts"
  - name: verbose
    description: Ausführliche Ausgabe
    default: "false"
shell: true                        # Shell-Injection via !`cmd` erlauben
context:                           # Ausführungskontext
  - fork                           # In Subagent isolieren
agent: researcher                  # Welchen Agenten-Typ verwenden
hooks:                             # Skill-spezifische Hooks
  - matcher: ""
    hooks:
      - type: command
        command: "echo 'Skill gestartet'"
---

# Skill-Inhalt (Markdown-Body)

Hier kommt der eigentliche Prompt/Anweisung für den Skill.

$ARGUMENTS wird durch alle übergebenen Argumente ersetzt.
$1, $2, ... oder $name für benannte Argumente.
${CLAUDE_SESSION_ID} für die aktuelle Session-ID.
${CLAUDE_EFFORT} für den aktuellen Effort-Level.
${CLAUDE_SKILL_DIR} für das Verzeichnis dieser SKILL.md-Datei.
```

### String-Substitutionen (vollständige Liste)

| Variable | Bedeutung |
|----------|-----------|
| `$ARGUMENTS` | Alle übergebenen Argumente als String |
| `$ARGUMENTS[N]` | N-tes Argument (0-basiert) |
| `$N` | Kurzform für `$ARGUMENTS[N]` (z.B. `$1`) |
| `$name` | Benanntes Argument aus `arguments:` Frontmatter |
| `${CLAUDE_SESSION_ID}` | Aktuelle Claude Code Session-ID |
| `${CLAUDE_EFFORT}` | Aktueller Effort-Level (low/medium/high/xhigh) |
| `${CLAUDE_SKILL_DIR}` | Absoluter Pfad zum Skill-Verzeichnis (NEU v2.1.x) |

---

## 3. Dynamische Kontext-Injektion

### Inline-Befehl-Substitution

```markdown
Die aktuelle Git-Branch ist: !`git branch --show-current`

Heutige offene TODOs:
!`grep -r "TODO" src/ | head -20`
```

Der Befehl `` !`command` `` wird vor dem LLM-Aufruf ausgeführt. Stdout wird eingebettet.

### Fenced-Block-Injection

````markdown
```!
cat ~/.claude/project-context.md
```
````

Der gesamte Block wird durch die Ausgabe des Befehls ersetzt.

### Sicherheitshinweis: `disableSkillShellExecution`

```json
// ~/.claude/settings.json (Policy-Level)
{
  "permissions": {
    "disableSkillShellExecution": true
  }
}
```

Deaktiviert `` !`cmd` `` und ```` ```! ```` in Skills global. Wichtig in
Multi-User-Umgebungen oder wenn Skill-Quellen nicht vertrauenswürdig sind.

---

## 4. Built-in Commands: Neue und geänderte Befehle (v2.1.100–v2.1.150)

### Neu hinzugekommen

| Befehl | Ab Version | Funktion |
|--------|-----------|----------|
| `/goal` | v2.1.139 | Aktuelles Session-Ziel setzen/anzeigen — bleibt über Compaction hinweg erhalten |
| `/scroll-speed` | v2.1.139 | Scrollgeschwindigkeit im Terminal anpassen |
| `/ultrareview` | v2.1.111 | Tiefes Code-Review mit mehrfachen Analyse-Durchgängen |
| `/usage` | v2.1.149 | Token-Verbrauch mit Aufschlüsselung nach Kategorien |
| `/diff` | v2.1.149 | Git-Diff-Anzeige mit Keyboard-Navigation (j/k Scroll) |
| `/run-skill-generator` | v2.1.x | Interaktiver Assistent zum Erstellen neuer Skills |
| `/less-permission-prompts` | v2.1.x | Reduziert Permission-Abfragen für ruhigeres Arbeiten |

### Umbenannt/Geändert

| Alt | Neu | Ab Version | Hinweis |
|-----|-----|-----------|---------|
| `/simplify` | `/code-review` | v2.1.147 | Mehr Fähigkeiten, nicht nur Vereinfachung |
| `/skills` | `/skills` | v2.1.x | Zeigt Skill-Menü mit Spacebar-Toggle-Optionen |

### Weiterhin wichtige Built-ins

| Befehl | Funktion |
|--------|----------|
| `/run` | Aktuellen Skill oder Bash-Befehl ausführen |
| `/verify` | Verifikation nach Implementierung |
| `/batch` | Parallele Aufgaben in Worktrees aufteilen (bis 10x schneller) |
| `/debug` | Systematischer Debug-Modus |
| `/loop` | Iterativer Fix-Loop bis Bedingung erfüllt |
| `/compact` | Kontext komprimieren |
| `/effort` | Effort-Level umstellen (low/medium/high/xhigh) |
| `/clear` | Session zurücksetzen |
| `/cost` | Kosten der aktuellen Session anzeigen |

---

## 5. Skill-Lifecycle und Kontext-Verwaltung

### Nach Auto-Compaction

Skills bleiben über Komprimierungen hinweg aktiv:
- Bis zu **5.000 Tokens** des Skill-Inhalts werden nach jeder Komprimierung
  automatisch wieder in den Kontext eingefügt
- Gemeinsames Budget für alle aktiven Skills: **25.000 Tokens**
- Skills mit `disable-model-invocation: true` sind davon nicht betroffen

### `context: fork` für isolierte Ausführung

```yaml
context:
  - fork
agent: researcher
```

Mit `fork` läuft der Skill in einem isolierten Subagenten. Der Hauptkontext bleibt
sauber, der Skill hat eigenen Kontext. Ideal für:
- Ressourcen-intensive Recherchen
- Code-Reviews die viele Dateien lesen
- Aufgaben mit eigenem Tool-Set (`allowed-tools:`)

---

## 6. Skill-Konfiguration: Sichtbarkeit und Overrides

### `skillOverrides` — Welche Skills aktiv sind

```json
// ~/.claude/settings.json
{
  "skillOverrides": "user-invocable-only"
}
```

| Wert | Bedeutung |
|------|-----------|
| `"on"` | Alle Skills aktiv (Default) |
| `"name-only"` | Nur per Name aufrufbar, keine Auto-Invocation |
| `"user-invocable-only"` | Nur Skills mit `user-invocable: true` |
| `"off"` | Alle Skills deaktiviert |

Kann auch aus dem `/skills`-Menü per Leertaste interaktiv umgestellt werden.

### `skillListingBudgetFraction` — Wie viel Kontext für Skill-Listings

```json
{
  "skillListingBudgetFraction": 0.1
}
```

Anteil des Kontext-Budgets für Skill-Beschreibungen im System-Prompt. Default: 0.1 (10%).

### `maxSkillDescriptionChars` — Länge der Beschreibungen

```json
{
  "maxSkillDescriptionChars": 512
}
```

Kürzt `description`-Felder in Skills auf diesen Wert. Hard-Cap: 1.536 Zeichen.

---

## 7. Monorepo-Support und Entdeckung

### Nested `.claude/skills/` Verzeichnisse

In Monorepos werden Skills automatisch auf Demand entdeckt:

```
my-monorepo/
├── .claude/skills/     ← Repo-weite Skills
│   └── deploy/SKILL.md
├── backend/
│   └── .claude/skills/ ← Backend-spezifische Skills
│       └── migrate/SKILL.md
└── frontend/
    └── .claude/skills/ ← Frontend-spezifische Skills
        └── build/SKILL.md
```

Skills in Unterverzeichnissen werden geladen, wenn in dem Verzeichnis gearbeitet wird.

### Live Change Detection

Änderungen an SKILL.md-Dateien wirken **sofort** ohne Neustart von Claude Code.
Kein `/clear` oder Neustart nötig nach dem Bearbeiten eines Skills.

---

## 8. Best Practices (Praxis-Empfehlungen)

### 8.1 Skill statt Command

```bash
# Alt (funktioniert noch):
.claude/commands/mein-befehl.md

# Neu (empfohlen):
.claude/skills/mein-befehl/SKILL.md
```

Der SKILL.md-Format bietet mehr Kontrolle (Frontmatter, Hooks, Fork-Kontext).

### 8.2 Beschreibung für Auto-Invocation optimieren

Die `description:` in SKILL.md wird für die automatische Skill-Auswahl verwendet.
Gute Beschreibungen beantworten: "Wann soll Claude DIESEN Skill nutzen?"

```yaml
description: >
  Nutze diesen Skill wenn eine neue Android-App von Grund auf gebaut werden soll,
  oder wenn gefragt wird "mach eine neue App" / "neue Android App" / "android bauen".
  Nicht für Erweiterungen bestehender Apps.
```

### 8.3 `disable-model-invocation` für reine Shell-Skills

Wenn ein Skill nur einen Befehl ausführen soll ohne LLM:

```yaml
---
name: git-status
description: Zeigt den aktuellen Git-Status
disable-model-invocation: true
shell: true
---
!`git status --short`
```

Kein Token-Verbrauch, maximale Geschwindigkeit.

### 8.4 `allowed-tools` für sichere Subagenten

```yaml
allowed-tools:
  - Read
  - Glob
  - Grep
  - WebSearch
```

Verhindert dass ein Researcher-Skill versehentlich Code ändert.

### 8.5 `paths:` für automatischen Datei-Kontext

```yaml
paths:
  - docs/ARCHITECTURE.md
  - src/models/
  - CLAUDE.md
```

Diese Dateien werden vor dem LLM-Aufruf automatisch in den Kontext geladen.
Spart das manuelle "lies zuerst..." am Anfang des Skill-Texts.

### 8.6 `${CLAUDE_SKILL_DIR}` für mitgelieferte Scripts

```yaml
shell: true
---
Analysiere diesen Code mit dem mitgelieferten Skript:
!`python3 "${CLAUDE_SKILL_DIR}/analyze.py" $ARGUMENTS`
```

Scripts können direkt neben der SKILL.md abgelegt werden. Keine hardcodierten Pfade nötig.

### 8.7 Skill-Qualität: Häufige Fehler vermeiden

| Fehler | Besser |
|--------|--------|
| `description:` zu kurz/unklar | Ausführlich erklären WANN der Skill passt |
| Kein `user-invocable: false` für interne Skills | Interne Utilities verstecken |
| Shell-Injection ohne `shell: true` | Frontmatter-Feld setzen |
| Zu viele `paths:` | Nur wirklich benötigte Dateien einlesen |
| Keine `allowed-tools:` bei Subagent-Skills | Tool-Scope immer explizit begrenzen |

---

## 9. Changelog-Highlights (Slash-Commands / Skills)

| Version | Datum | Änderung |
|---------|-------|----------|
| v2.1.147 | Mai 2026 | `/simplify` → `/code-review` umbenannt mit erweitertem Scope |
| v2.1.149 | Mai 2026 | `/usage` per-Kategorie-Aufschlüsselung; `/diff` mit j/k-Keyboard-Navigation |
| v2.1.139 | Apr 2026 | `/goal` Command (Session-Ziel persistent); `/scroll-speed` Command |
| v2.1.139 | Apr 2026 | `${CLAUDE_SKILL_DIR}` Variable in Skills |
| v2.1.x | 2026 | `skillOverrides` Einstellung (on/name-only/user-invocable-only/off) |
| v2.1.x | 2026 | `disableSkillShellExecution` Policy-Setting |
| v2.1.x | 2026 | `skillListingBudgetFraction` + `maxSkillDescriptionChars` Settings |
| v2.1.x | 2026 | Monorepo nested `.claude/skills/` Entdeckung |
| v2.1.x | 2026 | Live-Änderungserkennung (kein Neustart nach Skill-Edit) |
| v2.1.111 | 2026 | `/ultrareview` built-in Command |
| v2.1.x | 2025 | Skills-System (Agent Skills Standard) als Unified Framework |

---

## 10. Fehler-Patterns und Lösungen

### "Skill wird nicht erkannt"

1. SKILL.md liegt in `.claude/skills/<name>/SKILL.md` (Unterverzeichnis nötig)
2. `name:` im Frontmatter stimmt mit Verzeichnisname überein
3. `user-invocable: false` — Skill ist intern, nicht im Menü sichtbar
4. `skillOverrides: "off"` in Settings — Skills global deaktiviert

### "Shell-Injection funktioniert nicht"

```yaml
# Vergessen: shell: true im Frontmatter
shell: true
```

Oder `disableSkillShellExecution: true` blockiert es system-weit (Policy-Setting).

### "Skill verliert Kontext nach Compaction"

Skill-Inhalt über 5.000 Tokens wird nach Compaction nicht vollständig re-injiziert.
Lösung: Skill-Inhalt kompakt halten, wichtige Anweisungen an den Anfang.

### "Argumente kommen nicht an"

```yaml
# argument-hint zeigt was erwartet wird:
argument-hint: "<datei> [--verbose]"
# Benannte Argumente definieren:
arguments:
  - name: datei
    description: Zieldatei
```

Im Skill-Body: `$datei` oder `$1` oder `$ARGUMENTS`.

---

## 11. Projekt-spezifische Hinweise (dieses Repo)

Bestehende Skills in `~/.claude/skills/` und `.claude/skills/` verwenden bereits das
SKILL.md-Format. Bei neuen Skills:

1. **IMMER** `/skill-creator:skill-creator` Skill verwenden (CLAUDE.md-Pflicht)
2. Skill-Beschreibungen auf Deutsch (CLAUDE.md: `german-skill-triggers.md`)
3. Bei Subagent-Skills `allowed-tools:` explizit setzen
4. Nach Erstellung: In `~/.claude/rules/german-skill-triggers.md` Trigger-Zeile eintragen

---

*Zuletzt aktualisiert: 2026-05-25 durch best-practices Researcher-Agent*
*Quellen: code.claude.com/docs/en/skills, offizieller Claude Code Changelog v2.1.100–v2.1.150*
