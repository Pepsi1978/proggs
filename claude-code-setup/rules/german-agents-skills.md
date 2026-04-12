# Agents, Skills & Commands: Komplett auf Deutsch (KRITISCH)
<!-- Adapted from MIRROR-2026-03-25-MAC-003 by import agent on 2026-03-26 -->

## Regel

ALLE selbst erstellten Agents (`~/.claude/agents/*.md`), Skills und Commands
(`~/.claude/commands/*.md`) muessen **komplett auf Deutsch** geschrieben sein.

Dies gilt fuer:
- `description` im Frontmatter
- `<example>`-Bloecke (Kontext, User, Assistant, Commentary)
- System-Prompt (Markdown-Body nach dem Frontmatter)
- Sektionsueberschriften
- Erklaerungen, Regeln, Checklisten

## Was Englisch bleiben darf

- Tool-Namen: `nemo_ask`, `nemo_quiz`, `Read`, `Write`, `Bash`
- Code-Variablen: `categoryId`, `questionText`
- Technische Bezeichner: MCP, API, JSON, Kotlin, TypeScript
- Commit-Messages (eigene Regel: Commits auf Englisch)
- Externe/installierte Plugins (werden nicht uebersetzt)

## Beispiel

```markdown
# FALSCH:
description: "Use this agent when reviewing code..."
<example>
Context: User wants a code review
<commentary>Trigger the reviewer agent</commentary>

# RICHTIG:
description: "Nutze diesen Agenten wenn Code geprueft werden soll..."
<example>
Kontext: Benutzer will einen Code-Review
<commentary>Den Reviewer-Agenten starten</commentary>
```

## Warum

Der Benutzer spricht Deutsch, arbeitet per Whisper Speech-to-Text, und will
ein einheitlich deutsches System. Englische Beschreibungen in eigenen Agents
sind ein Stilbruch der in Session 2026-03-25 korrigiert wurde.
