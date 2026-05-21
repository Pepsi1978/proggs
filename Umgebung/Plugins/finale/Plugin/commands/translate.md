---
description: Run the finale translate-only mode. Executes Phase 0 (verify skills) + auto-detects target locales from values-*/-Verzeichnissen + invokes uebersetzer-skill in parallel workers per language with ±15% length budget + cross-lingual legal recheck. No fresh audit, no fix workflow on DE strings. Trigger auch bei natürlichen deutschen Anfragen: "finale translate", "starte finale translate", "übersetze mit finale", "finale translate für [App-Name]", "finale übersetzung für [App-Name]".
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, Task, WebFetch
model: opus
effort: max
argument-hint: [optional pfad-zur-app oder app-name wie "Best Journal Android"]
---

# /finale:translate

Reine Übersetzungsphase: nimmt die existierenden DE-Strings aus `values/strings.xml` und übersetzt sie in alle Zielsprachen, mit Längenbudget ±15% und Cross-Lingual-Rechtsprüfung. **Kein** neues Audit der DE-Originale (Annahme: DE ist bereits rechtssicher).

## Aufruf

```
/finale:translate [optional: pfad-zum-android-root]
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
mode: translate-only
appRoot: "$ARGUMENTS"
pluginRoot: "${CLAUDE_PLUGIN_ROOT}"
trigger: "/finale:translate"
phases: [0, 3b, 3c]
skipFullAudit: true
skipFixWorkflow: true
```

Der Orchestrator:
1. **Phase 0** wie immer (Skill-Verifikation).
2. **Auto-Detection der Zielsprachen** aus `values-*/-Verzeichnissen`. Wenn nur `values-en/` existiert: Multiple-Choice `[US] [UK] [EU]` für die Jurisdiktion. Bei mehreren: nur die existierenden Sprachen.
3. **Delta-Erkennung:** welche Strings sind neu / geändert seit der letzten Übersetzung? (Vergleich `values/strings.xml` mit `values-xx/strings.xml` pro Sprache).
4. **Parallel pro Sprache** einen Übersetzer-Subagent via Task tool spawnen (Opus, effort: max, bis ~15 gleichzeitig — FIN-023 Continuous-Spawning, siehe Orchestrator).
5. **Cross-Lingual-Rechtsprüfung:** jeder übersetzte Text-Block wird vom Rechtssicherheits-Skill (`Capability: Einzelprüfung`) gegen die Jurisdiktion der Zielsprache geprüft.
   - Bei `acceptable: true`: anwenden.
   - Bei `acceptable: false` (z. B. weil ein in DE harmloses Heilversprechen in der FR-Übersetzung unter den französischen Werbe-Gesundheitsregeln eskaliert): zurück in den Fix-Workflow (Phase 2) mit diesem neuen Finding.
6. Iteration bis alle Sprachen rechtskonform übersetzt sind.

## Wichtig

- Wenn die DE-Originale rechtliche Probleme haben (z. B. Werbeaussagen ohne Beleg, Pflichthinweise fehlen): diese werden in der Übersetzung NICHT korrigiert. Dafür ist `run` oder `audit-only` da. `translate` geht davon aus dass DE bereits sauber ist.
- Längenbudget ±15% wird vom Übersetzer-Skill durchgesetzt — bei knapp überschrittenen Strings versucht er Re-Phrasings. Bei harten Konflikten (z. B. eine deutsche Abkürzung dehnt sich im Türkischen auf das Doppelte): Karte mit Alternativ-Vorschlägen.
- Strings-XML-Apostroph-Escape ist Pflicht (Build-Sicherheit) — `fix-applier` macht das.
