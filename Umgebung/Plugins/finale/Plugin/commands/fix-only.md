---
description: Run the finale fix-only mode. Skips Phase 1 (audit), loads existing audit reports from .android-shield/, then runs Phase 2 (interactive fix) → Phase 4 (re-audit) → Phase 5 (loop). Use this when an audit has already been performed and only fixes are pending. Trigger auch bei natürlichen deutschen Anfragen: "finale fix-only", "fix mit finale", "finale fix für [App-Name]", "offene findings mit finale fixen", "finale fix-only über [App-Name]".
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, Task, WebFetch, WebSearch
model: opus
effort: max
argument-hint: [optional pfad-zur-app oder app-name wie "Best Journal Android"]
---

# /finale:fix-only

Nutze diesen Modus wenn die App schon ein vollständiges Audit hinter sich hat (z. B. via `audit-only` oder einem unterbrochenen `run`) und du jetzt nur noch die offenen Findings abarbeiten willst — ohne das komplette Audit nochmal laufen zu lassen.

## Aufruf

```
/finale:fix-only [optional: pfad-zum-android-root]
```


## App-Namen-Auflösung (PFLICHT vor dem Orchestrator-Spawn)

Identische Logik wie in `/finale:run` (siehe run.md): wenn der Nutzer einen App-Namen
nennt statt eines Pfads, vor dem Spawn auflösen via `~/proggs/<fuzzy-match>/`. Beispiele:
- „Best Journal Android" → `~/proggs/BestJournalAndroid/`
- „Best Journal Frank" → `~/proggs/BestJournalFrank/`
- „Entropie Reductor" → `~/proggs/EntropieReductor/`

Bei Mehrdeutigkeit Multiple-Choice-Frage. Bei nicht-Android-Pfad Rückfrage.

## Architektur-Hinweis

Identisch wie `/finale:run` (siehe `run.md`): Dieser Slash-Command laedt
`${CLAUDE_PLUGIN_ROOT}/agents/orchestrator.md` als **System-Anweisung an den
aktuellen Hauptagent**, NICHT als separat gespawnten Subagent. Begruendung
in `run.md` Architektur-Hinweis (Loop 3 Klarstellung 2026-05-21).

## Was du jetzt tust

Lade orchestrator.md als Kontext und folge dessen Phasen mit folgender Konfiguration:

```yaml
mode: fix-only
appRoot: "$ARGUMENTS"
pluginRoot: "${CLAUDE_PLUGIN_ROOT}"
trigger: "/finale:fix-only"
phases: [0, 2, "3a-strings", 4, 5]
requirePreexistingAudit: true
```

Hinweis: Phase `3a-strings` (nicht reine `3`) — fix-only soll nur neu entstandene
Hardcodes nach `strings.xml` migrieren, KEINE Uebersetzung der 26 Sprachen
ausloesen. Die Vollphase-`3` enthielte 3a + 3b + 3c und wuerde alle Sprachen
neu uebersetzen — was im fix-only-Modus semantisch falsch ist.

Der Orchestrator:
1. Phase 0 wie immer (Skill-Verifikation, Pre-Flight).
2. **Vor Phase 2:** liest `<app-root>/.android-shield/recht-report.json`. Wenn nicht vorhanden oder älter als 7 Tage: fragt nach (Multiple-Choice: `[V] Erst Vollscan starten` / `[T] Trotzdem mit altem Audit weitermachen` / `[X] Abbruch`).
3. Phase 2 mit den offenen Findings aus dem geladenen Report.
4. Phase 3 nur wenn neue Strings durch Phase-2-Fixes entstanden sind.
5. Phase 4 (Re-Audit) + Phase 5 (Loop) bis `openFindingsCount=0`.

## Wann das nützlich ist

- Audit ist groß (>100 Findings) und du willst in Sessions à 30 Findings durcharbeiten.
- Vorheriger Lauf wurde mit Option `[7]` unterbrochen → einfach `fix-only` aufrufen und der Orchestrator nimmt am `resume-state.json` wieder auf.
- Nach Skill-Update: du willst sehen ob bekannte Findings jetzt anders behandelt werden, ohne den Vollscan zu wiederholen (Phase 0 erkennt den geänderten Skill automatisch).
