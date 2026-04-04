# Trajectory-Analyse: Aus Arbeitsablaeufen lernen (KRITISCH)

> Basierend auf AutoRefine (arXiv 2601.22758): 20-73% weniger Schritte bei
> wiederkehrenden Aufgaben durch strukturiertes Wissen aus vergangenen Ablaeufen.
> Direktive: #2 Selbstbeobachtung + #3 Resilient Bugfixing

## Regel

Jede Session mit >10 Tool-Calls wird als Trajectory protokolliert.
Der `trajectory-analyzer` Agent analysiert periodisch alle Trajectories
und extrahiert daraus Verbesserungsvorschlaege.

## Was ist eine Trajectory?

Eine Trajectory ist die vollstaendige Sequenz von Tool-Aufrufen in einer Session —
wie ein "Arbeitsprotokoll". Beispiel:

```
Session: "Export-Feature implementieren"
Trajectory: Read → Read → Grep → Edit → Edit → Bash(build) → FEHLER → Read → Edit → Bash(build) → OK
Fehler: 1 (fehlender Import)
Korrekturen: 0
Dauer: 15 Min
Erfolg: Ja
```

## Wann Trajectories SCHREIBEN

Nach jeder Session mit >10 Tool-Calls, als LETZTER Schritt vor Session-Ende:

```json
{"date":"2026-04-04","task_description":"Export-Feature","tool_sequence":["Read","Read","Grep","Edit","Edit","Bash","Read","Edit","Bash"],"errors":["Build: missing import"],"corrections":[],"duration_minutes":15,"success":true,"tags":["android","feature","export"]}
```

## Wann Trajectories ANALYSIEREN

Der `trajectory-analyzer` Agent wird aufgerufen:
1. **Via /self-improve**: Als Stufe nach dem ACE-Curator
2. **Manuell**: "Trajectories analysieren", "Arbeitsablaeufe auswerten"
3. **Empfohlen**: Einmal pro Woche oder nach 10+ neuen Sessions

## Was der Analyzer sucht (3 Muster-Typen)

### 1. Wiederkehrende Sequenzen (→ Micro-Agent oder Skill)
Wenn die gleiche Tool-Sequenz (z.B. Read→Grep→Edit→Bash) in 3+ Trajectories vorkommt,
ist das ein Kandidat fuer einen automatisierten Ablauf.

### 2. Wiederkehrende Fehler (→ Regel oder Poka-Yoke)
Wenn der gleiche Fehlertyp in 2+ Trajectories auftaucht, ist das ein ALARM.
Direktive 3 verlangt: Kein Fehler zweimal. Die Trajectory-Analyse findet Fehler
die TROTZ bestehender Regeln noch auftreten.

### 3. Ineffiziente Ablaeufe (→ Workflow-Optimierung)
Wenn eine Trajectory 15 Tool-Calls fuer etwas braucht das in 5 loesbar waere,
ist das ein Kandidat fuer eine Regel die den kuerzeren Weg beschreibt.

## Zusammenspiel AutoRefine ↔ ACE

| System | Analysiert | Erzeugt | Frequenz |
|--------|-----------|---------|----------|
| **ACE-Curator** | Session-Scores, Bug-Cases, Feedback | Regel-Verbesserungen | Nach Sessions |
| **Trajectory-Analyzer** | Tool-Call-Sequenzen | Neue Agents, Skills, Regeln | Woechentlich |

ACE verbessert BESTEHENDE Regeln. AutoRefine entdeckt NEUE Regeln und Agents.
Sie ergaenzen sich — ACE ist reaktiv (aus Fehlern lernen), AutoRefine ist proaktiv
(aus Erfolgen lernen).

## Defense in Depth (Direktive 3)

| Schicht | Mechanismus | Was es schuetzt |
|---------|-------------|----------------|
| 1 | Nur-Lese-Analyse | Trajectories werden nur gelesen, nie geaendert |
| 2 | Haeufigkeits-Filter | Nur Muster mit 2+ Vorkommen werden vorgeschlagen |
| 3 | Benutzer-Bestaetigung | JEDER Vorschlag braucht Franks OK |
| 4 | ACE-Sperrliste | Geschuetzte Zonen und Dateien werden respektiert |
| 5 | JSONL-Format | Korrupte Zeilen zerstoeren nicht den Rest |
| 6 | Groessenlimit (100) | Alte Trajectories werden automatisch geloescht |

## Was NIEMALS passieren darf

- ❌ Trajectory-Analyzer aendert automatisch Regeln oder Agents
- ❌ Einzelfaelle werden als Muster behandelt (min. 2x)
- ❌ Trajectories werden fuer Sessions mit <10 Tool-Calls geschrieben (Rauschen)
- ❌ Geschuetzte Zonen (ACE-PROTECTED) werden in Vorschlaegen beruehrt
- ❌ trajectories.jsonl waechst ueber 100 Eintraege ohne Pruning
