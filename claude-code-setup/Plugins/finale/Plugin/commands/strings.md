---
description: Run the finale strings-only mode. Executes Phase 0 (verify skills) + Phase 1 (roentgen scan only — no recht audit) + Strings-Skill to extract hardcodes into strings.xml with semantic keys and translatable flags. No legal audit, no translation. Trigger auch bei natürlichen deutschen Anfragen: "finale strings", "starte finale strings", "strings.xml mit finale extrahieren", "finale strings für [App-Name]", "hardcodes extrahieren mit finale".
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, Task
model: opus
effort: max
argument-hint: [optional pfad-zur-app oder app-name wie "Best Journal Android"]
---

# /finale:strings

Reine String-Hygiene: scannt die App nach hardcodierten Texten (Compose-Literale, XML-Attribute, Kotlin-Strings, etc.) und extrahiert sie nach `res/values/strings.xml` mit semantischen Keys. **Kein** Rechtsaudit. **Keine** Übersetzung. Ideal als Vorbereitungs-Lauf bevor du den vollständigen Closed Loop startest.

## Aufruf

```
/finale:strings [optional: pfad-zum-android-root]
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
mode: strings-only
appRoot: "$ARGUMENTS"
pluginRoot: "${CLAUDE_PLUGIN_ROOT}"
trigger: "/finale:strings"
phases: [0, 1-roentgen-only, 3a-strings]
skipRechtAudit: true
skipTranslation: true
```

Der Orchestrator:
1. Phase 0 wie immer.
2. Phase 1 nur mit dem **Roentgen-Skill** (kein Recht-Audit).
3. Phase 3a: **Strings-Skill** wird angewendet — Hardcodes nach `values/strings.xml`.
4. Pro Migration: `fix-applier` mit `patchKind: strings-xml-insert` und `patchKind: text-replace` (am Verwendungsort wird Hardcode durch `stringResource(R.string.…)` oder `getString(R.string.…)` ersetzt).
5. Da Wertgleichheit garantiert ist (kein semantischer Wechsel), läuft das automatisch ohne Karten — EXCEPT wenn der Strings-Skill einen Duplikat-Konflikt entdeckt (zwei verschiedene Hardcodes wollen denselben Key) oder einen mehrsprachigen Konflikt erkennt — dann interaktive Rückfrage.

## Ergebnis

- `res/values/strings.xml` ist vollständig: alle Hardcodes drin.
- Compose- und XML-Code referenziert nur noch String-Resources.
- App ist bereit für `translate` oder für vollen `run`.
- Bericht: `<app-root>/.android-shield/strings-plan.json`

## Wichtig

- KEIN Recht-Audit hier — wenn du Rechtsrisiken in den Texten hast, werden sie 1:1 in die strings.xml übernommen. Dafür ist `run` da.
- Falls schon strings.xml existieren: Strings-Skill arbeitet im Delta-Modus, dedupliziert und vergibt nur neue Keys.
