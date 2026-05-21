---
description: Run the finale full closed-loop pipeline at maximum intelligence. Always verifies skill freshness in Phase 0 before any work, then runs audit → interactive non-invasive fix workflow → delta translation → re-audit until openFindingsCount=0. Trigger auch bei natürlichen deutschen Anfragen wie "starte finale", "starte das finale Plugin", "starte finale über [App-Name]", "starte finale für [App-Name]", "finale auf [App-Name]", "lass finale über [App-Name] laufen", "[App-Name] mit finale prüfen".
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, Task, WebFetch, WebSearch
model: opus
effort: max
argument-hint: [optional pfad-zur-app oder app-name wie "Best Journal Android"]
---

# /finale:run

Du startest jetzt die vollständige Closed-Loop-Pipeline des `finale`-Plugins. Diese Pipeline durchleuchtet eine deutsche Android-App, macht sie rechtssicher und übersetzt sie layout-stabil in alle Zielsprachen — **ausschließlich durch Text-Änderungen**, niemals durch Funktions- oder Layout-Eingriffe (außer der Nutzer stimmt explizit pro Fall zu).

## Aufruf-Varianten

Direkter Slash-Command:
```
/finale:run [optional: pfad-zur-app oder app-name]
```

Natürlichsprachlich (gleicher Trigger):
- „Starte finale" — App-Root = aktuelles Verzeichnis
- „Starte finale über der App Best Journal Android" — App-Name wird aufgelöst (siehe unten)
- „Starte finale für [App-Name]"
- „Lass finale über [App-Name] laufen"
- „[App-Name] mit finale prüfen"

## App-Namen-Auflösung (PFLICHT vor dem Orchestrator-Spawn)

Wenn der Nutzer einen App-Namen genannt hat (oder ein Verzeichnis-Hinweis im Argument steht), löst du den Pfad VORHER auf:

1. **Genauer Pfad gegeben?** (`$ARGUMENTS` startet mit `/`, `~/`, `./` oder ist ein absoluter Windows-Pfad) → direkt nehmen.
2. **App-Name gegeben?** Versuche in dieser Reihenfolge:
   - `~/proggs/<exakter-name>/` (z. B. `BestJournalAndroid`, `EntropieReductor`)
   - Fuzzy-Match gegen `ls ~/proggs/`: Leerzeichen entfernen, case-insensitive vergleichen
     - „Best Journal Android" → `BestJournalAndroid`
     - „Best Journal Frank" → `BestJournalFrank`
     - „Entropie Reductor" → `EntropieReductor`
   - Wenn mehrere passen: Multiple-Choice-Frage mit den Treffern.
   - Wenn keiner passt: ein Treffer-Vorschlag pro Wort, sonst Multiple-Choice mit allen Apps unter `~/proggs/` die `AndroidManifest.xml` enthalten.
3. **Kein Argument?** App-Root = aktuelles Verzeichnis (mit Hinweis im Pre-Flight-Plan dass der Default genommen wird).
4. **Verifikation:** vor dem Spawn prüfen, dass im aufgelösten Pfad ein `AndroidManifest.xml` oder `settings.gradle` / `settings.gradle.kts` liegt. Wenn nicht: Nutzer-Rückfrage „Pfad <X> sieht nicht nach einer Android-App aus — trotzdem fortfahren?".

## Was du jetzt tust

1. **Spawne den `orchestrator`-Agent** via Task tool mit folgendem Kontext:

```yaml
mode: default
appRoot: "<aufgelöster Pfad>"   # nach App-Namen-Resolution oben
pluginRoot: "${CLAUDE_PLUGIN_ROOT}"
trigger: "/finale:run"
phases: [0, 1, 2, 3, 4, 5]      # Closed Loop bis openFindingsCount=0
```

Der Orchestrator-Prompt lebt unter `${CLAUDE_PLUGIN_ROOT}/agents/orchestrator.md` und enthält die komplette Phasen-Logik, alle Karten-Layouts, das Audit-Log-Format und die Subagent-Delegation.

2. **Warte auf den Pre-Flight-Plan** des Orchestrators und gib die Nutzer-Eingabe (`[F]` / `[A]` / `[X]`) direkt an ihn zurück.

3. **Während der Pipeline läuft, mach NICHTS außerhalb:** keine direkten Edits, kein eigener Bash, keine Recherche an deiner Stelle. Du bist nur der Eintritts-Wrapper.

4. **Nach Abschluss** zeig die finale Status-Meldung des Orchestrators (3-Punkte-Schema + Statistik) und nichts weiter.

## Wichtig

- Du läufst selbst auf `model: opus` mit `effort: max`. Das wird mit dem Subagent-Frontmatter abgeglichen — wenn Anthropic `effort` im Frontmatter noch nicht respektiert, sollte vor dem Aufruf `/effort xhigh` oder höher gesetzt sein. Das ist im README dokumentiert.
- Die vier Skills werden über Symlinks aus `${CLAUDE_PLUGIN_ROOT}/skills/` geladen. Phase 0 des Orchestrators verifiziert ihre Auflösbarkeit.
- Bei totem Symlink bricht der Orchestrator SOFORT ab — das ist kein Bug, das ist Schutz vor inkonsistenten Skill-Versionen.
