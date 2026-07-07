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

## Architektur-Hinweis (Loop 3 Klarstellung 2026-05-21)

**WICHTIG:** Dieser Slash-Command laedt `${CLAUDE_PLUGIN_ROOT}/agents/orchestrator.md`
als **System-Anweisung an den aktuellen Hauptagenten**, NICHT als separat
gespawnten Subagent. Grund: Phase 2 (interaktiver Karten-Workflow) erfordert
User-Interaktion ueber mehrere Turns. Ein per Task-Tool gespawnter Subagent
laeuft einmalig bis fertig und kann nicht ueber Turns hinweg auf Nutzer-Input
warten — daher MUSS der Hauptagent (mit `model: opus, effort: max` aus diesem
Frontmatter) die Orchestrator-Logik selbst fuehren.

Subagents (fix-applier, researcher, url-checker, Uebersetzer-Worker) werden
vom Hauptagent via Task-Tool gespawnt fuer abgeschlossene Teilaufgaben — das ist
die korrekte Verwendung des Task-Tools.

## Was du jetzt tust

1. **Lade Orchestrator-Logik als deinen Kontext:**
   - Lies `${CLAUDE_PLUGIN_ROOT}/agents/orchestrator.md` (Read-Tool)
   - Behandle den Inhalt als deine eigenen System-Anweisungen
   - Loese `FINALE_PLUGIN_ROOT` selbst auf (siehe orchestrator.md Phase 0 Schritt 0
     mit 5 Fallback-Ebenen). NICHT `${CLAUDE_PLUGIN_ROOT}` als Variable uebergeben —
     die ist in Subagent-Bash-Umgebungen leer.

2. **Setze Lauf-Parameter (mental):**

```yaml
mode: default
appRoot: "<aufgeloester Pfad>"    # nach App-Namen-Resolution oben
trigger: "/finale:run"
phases: [0, 1, 2, 3, 4, 5]        # Closed Loop bis openFindingsCount=0
```

3. **Phase 0 starten:** verify-skills.sh ausfuehren (via Bash-Tool), Pre-Flight-Plan zeigen.

4. **Auf `[F]`/`[A]`/`[X]`-Eingabe warten:** der Nutzer tippt nach dem Pre-Flight-Plan
   eine Auswahl. Erst dann zur naechsten Phase.

5. **Phasen 1-5 abarbeiten:** wie in orchestrator.md spezifiziert.
   Subagents (fix-applier, researcher etc.) per Task-Tool spawnen — DAS sind
   die echten Subagent-Spawns.

6. **Nach Abschluss:** finale Status-Meldung (3-Punkte-Schema + Statistik) ausgeben.

## Wichtig

- Du läufst selbst auf `model: opus` mit `effort: max`. Das wird mit dem Subagent-Frontmatter abgeglichen — wenn Anthropic `effort` im Frontmatter noch nicht respektiert, sollte vor dem Aufruf `/effort xhigh` oder höher gesetzt sein. Das ist im README dokumentiert.
- Die vier Skills werden über Symlinks aus `${CLAUDE_PLUGIN_ROOT}/skills/` geladen. Phase 0 des Orchestrators verifiziert ihre Auflösbarkeit.
- Bei totem Symlink bricht der Orchestrator SOFORT ab — das ist kein Bug, das ist Schutz vor inkonsistenten Skill-Versionen.
