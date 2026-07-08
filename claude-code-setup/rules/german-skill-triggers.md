# Deutsche Trigger-Map fuer Skills und Plugins (KRITISCH)

> Frank spricht Deutsch (oft via Whisper). Bei JEDER Anfrage mental pruefen (schon bei 1 % Wahrscheinlichkeit
> den passenden Skill aufrufen). **Vollstaendige Map (alle Kategorien, Whisper-Korrekturen, proaktive
> Agents): `claude-code-setup/docs/rules/german-skill-triggers.md`.**

## Wichtigste Trigger (haeufig)
- "committe/pushe/PR" → `commit-commands:*` · "rückgängig/undo" → `undo-changes`
- "Strings finden/hardcodiert" → `string-extraktor` (erstellt dt.) · "übersetze die Strings/i18n/
  Lokalisierung" → `uebersetzung` (konsumiert dt.) — NICHT verwechseln
- "finde den Bug/geht nicht" → `superpowers:systematic-debugging` · "Tiefen-Debugging/alle Bugs suchen" → `tiefen-debugging`
- "recherchiere/such im Web" → `research` (Protokoll: Empfehlung + Frage 1 A/B/C/D) · "Best-Practices" → `best-practices`
- "Cortex Update/ins Gehirn" → `cortex-update` · "reviewe den PR" → `code-review:code-review`
- "erstelle einen Skill" → `skill-creator:skill-creator` · "erstelle/fixe Hook" → `hook-forge` (ZUERST)
- "portiere zu cowork" → `cowork-portierung` (Muss) · "weiter/fortsetzen" → `aufgaben-bruecke`
- "Android/Kotlin/Compose" → `android-dev` · "Sound-Effekt" → `sound-search` · "Web-UI bauen" → `frontend-design`

## Whisper-Korrekturen (haeufigste)
"Cloud" → **Claude** · "Self improve" → `self-improve` · "Tool check" → `tool-check` · "Code Rabbit" →
`coderabbit` · "Reflektion"/"Reflect" → nachfragen (`claude-reflect` lernen vs. `reflexion` bewerten).

Fuer seltenere Trigger, die vollstaendigen Kategorien (Second Brain, Session-Backup, Ledger …) und die
proaktiven Agents (code-simplifier, auto-verify, hyperagent-stop …) den Volltext lesen.
