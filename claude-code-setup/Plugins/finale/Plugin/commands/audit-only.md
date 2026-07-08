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

- Diese Datei-Schutz-Pflicht wird durch den Pre-Hook `audit-only-write-guard` erzwungen (siehe `hooks/hooks.json`, registriert auf `PreToolUse` mit Matcher `Edit|Write|MultiEdit`). Der Hook prueft ob die Lock-Datei `<app-root>/.android-shield/.audit-only.lock` existiert. Wenn ja UND die zu schreibende Datei NICHT unter `.android-shield/` liegt, blockiert der Hook mit Exit 2.
- **Lock-Lifecycle (Orchestrator-Pflicht):** Beim Start des audit-only-Modus MUSS der Orchestrator die Lock-Datei `<app-root>/.android-shield/.audit-only.lock` anlegen mit folgenden 3 Feldern:
  ```
  timestamp: <ISO-8601>
  sessionToken: <UUID4>
  mode: audit-only
  ```
  Der `sessionToken` (UUID4, Wave 5 Umstellung 2026-05-21) ersetzt den frueheren `orchestratorPid` — LLM-Agenten haben keine stabile OS-PID. Der Orchestrator speichert den Token in seinem eigenen Kontext und vergleicht ihn beim Loeschen.

  Am Ende des Modus (regulaer ODER abgebrochen) MUSS der Orchestrator die Lock-Datei wieder loeschen.

  Bei Crash bleibt eine Stale-Lock zurueck — der `audit-only-write-guard`-Hook erkennt Locks aelter als 30 Minuten (Wave 6, 2026-05-21) und ignoriert sie (Schreibvorgang wird durchgelassen mit Warnung). Der Nutzer kann den Lock manuell loeschen mit `rm <app-root>/.android-shield/.audit-only.lock`.
- **Schreiben innerhalb `.android-shield/` ist immer erlaubt** — das ist die Plugin-Output-Domain (Reports, audit-log, skill-versions). Nur die App-Quellen (res/, src/, AndroidManifest.xml, build.gradle.kts etc.) sind gesperrt.
- Phase 0 muss trotzdem laufen. Tote Symlinks → Abbruch wie immer.
