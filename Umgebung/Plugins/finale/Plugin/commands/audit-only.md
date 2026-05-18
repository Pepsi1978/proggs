---
description: Run the finale audit-only mode. Executes Phase 0 (skill verification) + Phase 1 (roentgen + rechtssicherheits audit), then stops with a full report. No fix workflow, no translation, no file changes to the app. Trigger auch bei natürlichen deutschen Anfragen: "finale audit", "finale audit für [App-Name]", "audit-only mit finale", "finale prüfen ohne Fix", "nur audit mit finale", "lass finale audit über [App-Name] laufen".
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, Task, WebFetch, WebSearch
model: opus
effort: max
argument-hint: [optional pfad-zur-app oder app-name wie "Best Journal Android"]
---

# /finale:audit-only

Read-Only-Lauf: vollständige Durchleuchtung der App, vollständiger Rechtsbericht — aber **kein Fix-Workflow**, keine Übersetzung, keine Änderung an App-Dateien. Ideal als Erstbestandsaufnahme oder als regelmäßiger Compliance-Check vor jedem Release-Kandidaten.

## Aufruf

```
/finale:audit-only [optional: pfad-zum-android-root]
```


## App-Namen-Auflösung (PFLICHT vor dem Orchestrator-Spawn)

Identische Logik wie in `/finale:run` (siehe shield.md): wenn der Nutzer einen App-Namen
nennt statt eines Pfads, vor dem Spawn auflösen via `~/proggs/<fuzzy-match>/`. Beispiele:
- „Best Journal Android" → `~/proggs/BestJournalAndroid/`
- „Best Journal Frank" → `~/proggs/BestJournalFrank/`
- „Entropie Reductor" → `~/proggs/EntropieReductor/`

Bei Mehrdeutigkeit Multiple-Choice-Frage. Bei nicht-Android-Pfad Rückfrage.

## Was du jetzt tust

Spawne den `orchestrator`-Agent via Task tool mit:

```yaml
mode: audit-only
appRoot: "$ARGUMENTS"
pluginRoot: "${CLAUDE_PLUGIN_ROOT}"
trigger: "/finale:audit-only"
phases: [0, 1]
stopAfter: "phase1-report"
writeAllowedPaths:
  - ".android-shield/**"
  # NICHT: app-source, manifest, res/**
```

Der Orchestrator führt Phase 0 und Phase 1 aus, erzeugt:
- `<app-root>/.android-shield/roentgen-report.json`
- `<app-root>/.android-shield/recht-report.json`
- `<app-root>/.android-shield/audit-log.md` (Append)

…und gibt eine Zusammenfassung am Schluss. KEINE Karten, KEIN Fix, KEINE Übersetzung.

## Wichtig

- Diese Datei-Schutz-Pflicht wird durch den Pre-Hook `block-app-writes-in-audit-only` erzwungen (siehe `hooks/hooks.json`). Versucht ein Subagent dennoch in `res/`, `src/`, `AndroidManifest.xml` zu schreiben, wird der Versuch blockiert und im Audit-Log mit `phase-violation` markiert.
- Phase 0 muss trotzdem laufen. Tote Symlinks → Abbruch wie immer.
