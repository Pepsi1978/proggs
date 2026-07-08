# Deutsche Trigger-Map fuer Skills und Plugins (KRITISCH)

> Frank spricht Deutsch (oft Whisper). Bei JEDER Anfrage mental pruefen (ab 1 % den Skill aufrufen).
> **Vollstaendige Map (alle Kategorien, Whisper-Korrekturen, proaktive Agents):
> `claude-code-setup/docs/rules/german-skill-triggers.md`.**

## Wichtigste Trigger
- "committe/pushe/PR" → `commit-commands:*` · "rückgängig/undo" → `undo-changes`
- "Strings finden/hardcodiert" → `string-extraktor` (erstellt dt.) · "übersetze Strings/i18n/
  Lokalisierung" → `uebersetzung` (konsumiert dt.) — NICHT verwechseln
- "finde den Bug/geht nicht" → `superpowers:systematic-debugging` · "Tiefen-Debugging/alle Bugs" → `tiefen-debugging`
- "recherchiere/such im Web" → `research` (Protokoll: Empfehlung + Frage 1 A/B/C/D) · "Best-Practices" → `best-practices`
- "Cortex Update/ins Gehirn" → `cortex-update` · "reviewe den PR" → `code-review:code-review`
- "erstelle einen Skill" → `skill-creator:skill-creator` · "erstelle/fixe Hook" → `hook-forge` (ZUERST)
- "portiere zu cowork" → `cowork-portierung` (Muss) · "weiter/fortsetzen" → `aufgaben-bruecke`
- "Android/Kotlin/Compose" → `android-dev` · "Sound-Effekt" → `sound-search` · "Web-UI bauen" → `frontend-design`

## Whisper-Korrekturen
"Cloud" → **Claude** · "Self improve" → `self-improve` · "Tool check" → `tool-check` · "Code Rabbit" →
`coderabbit` · "Reflektion"/"Reflect" → nachfragen (`claude-reflect` lernen vs. `reflexion` bewerten).
