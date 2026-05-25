# Best Practices: Skills (SKILL.md)

> Stand: 2026-05-25 | Claude Code v2.1.150
> Quellen: code.claude.com/docs/en/skills (offiziell), changelog-Analyse

---

## Was sind Skills?

Skills sind wiederverwendbare Anweisungsblöcke die Claude Code als `/skill-name`-Slash-Commands
oder als automatisch geladener Kontext zur Verfügung stehen. Sie liegen als Markdown-Dateien
(`SKILL.md`) in `~/.claude/skills/<name>/SKILL.md` (global) oder `.claude/skills/<name>/SKILL.md`
(projektlokal).

---

## SKILL.md Frontmatter — vollständige Feldreferenz

Alle Felder sind optional außer `description` (empfohlen). Felder werden im YAML-Block
zwischen `---` gesetzt.

| Feld | Typ | Bedeutung |
|------|-----|-----------|
| `name` | string | Anzeigename (Standard: Verzeichnisname) |
| `description` | string | **Empfohlen.** Was der Skill tut — Claude entscheidet anhand dessen ob er passt |
| `when_to_use` | string | Erweiterte Beschreibung für die automatische Skill-Auswahl |
| `argument-hint` | string | Hilfetext der beim Tippen von `/skill-name ` angezeigt wird |
| `arguments` | list | Benannte Parameter die der Skill erwartet |
| `disable-model-invocation` | bool | Bei `true`: kein LLM-Aufruf — Skill wird direkt als Text injiziert |
| `user-invocable` | bool | Bei `false`: Skill taucht NICHT in `/`-Autocomplete auf (nur automatisch nutzbar) |
| `allowed-tools` | list | Werkzeuge die der Skill nutzen darf (default: alle) |
| `model` | string | Modell für diesen Skill (z.B. `claude-opus-4-5`) |
| `effort` | string | Aufwandsstufe: `low`, `medium`, `high`, `xhigh` |
| `context` | string | `fork` = Skill läuft in eigenem Subagent-Context |
| `agent` | string | Subagent-Typ wenn `context: fork` gesetzt (default: `general-purpose`) |
| `hooks` | map | Hooks die nur in diesem Skill-Context aktiv sind |
| `paths` | list | Pfade zu unterstützenden Dateien die automatisch mitgeladen werden |
| `shell` | bool | Erlaubt Shell-Execution im Skill (`disableSkillShellExecution` Policy blockiert) |

### Wichtiger Unterschied: `disable-model-invocation` vs. `user-invocable: false`

```yaml
# disable-model-invocation: true
# → Skill injiziert Inhalt direkt ohne LLM-Aufruf
# → Sinnvoll für: Templates, Boilerplate, reine Kontext-Injektion

# user-invocable: false
# → Skill erscheint NICHT im /Autocomplete
# → Kann aber von Claude automatisch oder von anderen Skills gerufen werden
# → Sinnvoll für: interne Hilfsskills, auto-aktivierte Kontext-Schichten
```

---

## String-Substitutionen im Skill-Inhalt

Claude Code ersetzt diese Variablen automatisch im Skill-Text:

| Variable | Bedeutung |
|----------|-----------|
| `$ARGUMENTS` | Alle Argumente als ein String |
| `$ARGUMENTS[N]` | N-tes Argument (0-basiert) |
| `$N` | Kurzform für `$ARGUMENTS[N-1]` (1-basiert) |
| `$name` | Wert des benannten Arguments `name` aus `arguments:`-Liste |
| `${CLAUDE_SESSION_ID}` | Aktuelle Session-ID |
| `${CLAUDE_EFFORT}` | Aktuelle Effort-Stufe |
| `${CLAUDE_SKILL_DIR}` | Absoluter Pfad zum Skill-Verzeichnis |

---

## Dynamische Kontext-Injektion

Skill-Inhalte können Shell-Befehle einbetten die beim Laden ausgeführt werden:

```markdown
Aktueller Branch: !`git branch --show-current`
Letzte Commits: !`git log --oneline -5`
```

Die `` !`befehl` `` Syntax führt den Befehl aus und fügt das Ergebnis ein. Voraussetzung:
`shell: true` im Frontmatter (oder globale Shell-Execution erlaubt). Policy-Setting
`disableSkillShellExecution: true` blockiert alle Shell-Ausführungen in Skills.

---

## Skill-Lifecycle: Token-Budget und Compaction

- **Pro Skill**: max. 5.000 Tokens Inhalt
- **Kombiniertes Budget**: max. 25.000 Tokens für alle geladenen Skills zusammen
- **Nach Compaction**: Skill-Inhalte werden automatisch wieder dem Kontext hinzugefügt
- **Live-Änderungen**: Skills werden bei Änderung auf der Festplatte sofort neu geladen
  (kein Neustart von Claude Code nötig)

---

## Sichtbarkeits-Steuerung via `skillOverrides` (settings.json)

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
| `"name-only"` | Nur Name wird gezeigt, kein Beschreibungstext |
| `"user-invocable-only"` | Erscheint nur in Benutzer-Autocomplete, nicht auto-aktiviert |
| `"off"` | Skill komplett deaktiviert |

---

## Budget-Einstellungen (settings.json)

```json
{
  "skillListingBudgetFraction": 0.3,
  "maxSkillDescriptionChars": 200
}
```

- `skillListingBudgetFraction`: Anteil des Kontextfensters für Skill-Listings (Standard ~30%)
- `maxSkillDescriptionChars`: Maximale Länge der `description` die angezeigt wird

---

## Skill in Subagent ausführen (`context: fork` + `agent:`)

```yaml
---
name: deep-analyse
description: Führe eine tiefe Code-Analyse durch
context: fork
agent: general-purpose
model: claude-opus-4-5
effort: high
---

Analysiere den Code auf...
```

Mit `context: fork` läuft der Skill in einem eigenen Subagent-Kontext. Das `agent:`-Feld
bestimmt welcher Subagent-Typ genutzt wird. So bleibt der Hauptchat-Kontext sauber.

---

## Unterstützende Dateien via `paths:`

```yaml
---
name: code-review
description: Vollständiger Code-Review
paths:
  - review-checklist.md
  - security-rules.md
---

Führe einen Review durch. Nutze die Checkliste und Sicherheitsregeln.
```

Dateien in `paths:` werden automatisch mitgeladen wenn der Skill aktiviert wird.
Pfade sind relativ zum Skill-Verzeichnis.

---

## Monorepo-Support via geschachtelte Verzeichnisse

Skills können in Unterverzeichnissen verschachtelt werden:

```
.claude/skills/
  backend/
    api-review/SKILL.md    → /backend/api-review
    db-schema/SKILL.md     → /backend/db-schema
  frontend/
    component/SKILL.md     → /frontend/component
```

---

## Eingebaute Skills (ab v2.1.145)

Diese Skills sind in Claude Code eingebaut (kein manuelles Erstellen nötig):

| Skill | Funktion |
|-------|----------|
| `/run` | Führt Shell-Befehle aus und zeigt Output |
| `/verify` | Verifiziert Assertions über den aktuellen Codestand |
| `/run-skill-generator` | Erstellt neue Skills interaktiv |

**Voraussetzung**: v2.1.145 oder neuer.

---

## Invocation Control — Übersicht

| Szenario | Konfiguration |
|----------|--------------|
| Slash-Command für Benutzer | `user-invocable: true` (Standard) |
| Nur automatisch durch Claude | `user-invocable: false` |
| Direkte Text-Injektion ohne LLM | `disable-model-invocation: true` |
| Im eigenen Subagent-Context | `context: fork` |
| Bestimmte Tools erlauben | `allowed-tools: [Read, Glob, WebSearch]` |

---

## Changelog-Highlights (Skills)

| Version / Datum | Änderung |
|-----------------|---------|
| v2.1.145 | Eingebaute Skills: `/run`, `/verify`, `/run-skill-generator` |
| v2.1.63 | Skill-Ausführung via `context: fork` + Subagent stabil |
| 2026-01 | SKILL.md-Support eingeführt, Session-Forking |
| 2025-12 | `.claude/rules/` Verzeichnis + Background-Kontext |

---

## Best Practice Zusammenfassung

1. **`description` immer setzen** — Claude nutzt es zur automatischen Skill-Auswahl
2. **`when_to_use` für klare Abgrenzung** — wann der Skill sinnvoll ist vs. andere Skills
3. **`user-invocable: false`** für interne Hilfsskills die Benutzer nicht direkt brauchen
4. **`disable-model-invocation: true`** für reine Kontext-Injektionen (spart Tokens)
5. **`context: fork`** wenn der Skill den Hauptchat-Kontext nicht verschmutzen soll
6. **`paths:`** für umfangreiche Referenzdokumente statt alles in den Skill-Body
7. **Shell-Injection** `` !`befehl` `` für dynamischen Kontext (Datum, Branch, Status)
8. **Skill-Token-Budget beachten**: 5.000 Token pro Skill, 25.000 gesamt
9. **`skillOverrides`** um problematische Skills temporär zu deaktivieren ohne zu löschen

---

> Quellen: [skills — code.claude.com](https://code.claude.com/docs/en/skills) (offiziell)
> Externe/unbestätigte Angaben: keine in diesem Dokument
