# Metacognitives Monitoring: Echtzeit-Selbstbeobachtung (KRITISCH)

> Implementierung der Metacognitions-Schicht. Ergaenzt Direktive #2 (`self-observation.md`) mit
> konkreten Trackern, Alarmschwellen und dem Session-Score-System.

## 4 Echtzeit-Tracker (IMMER AKTIV)

| Tracker | Zaehlt | Schwelle → Reaktion |
|---------|--------|---------------------|
| **Retry** | fehlgeschlagene Tool-Calls, Wiederholungen fuer die GLEICHE Sache | >2 fuer eine Aufgabe → STOP, Hypothesen-Loop; >3 gesamt → Warnung |
| **Korrektur** | Benutzer-Korrekturen ("nein"/"stop"/"anders") | 2. Korrektur zum GLEICHEN Thema → sofort als Regel/Memory persistieren |
| **Drift** | alle ~10 Tool-Calls: "arbeite ich noch am Ziel?" | Arbeit an Ungefragtem → informieren, zuruecklenken; >5 Turns ohne Fortschritt → Scope reduzieren/fragen |
| **Wissens-Vertrauen** (optional) | Aktualitaet von Pfaden/Versionen/API | aus alter Session/Training → Confidence-Ampel GELB/ROT, nachschlagen |

## Session-Score

Automatisch vom `session-scorer`-Hook (SessionEnd) in `~/.claude/session-scores.jsonl`. 4 Dimensionen
(je 25 %): Intent-Treue, Effizienz, Memory-Aktualitaet, Lernertrag. Skala 1 (Ziel verfehlt) - 5 (perfekt).

## Alarmschwellen

| Alarm | Bedingung | Aktion |
|-------|-----------|--------|
| Retry | >3 fuer gleiche Sache | Debugging-Hypothesen-Loop |
| Drift | >5 Turns ohne Fortschritt | informieren, Scope reduzieren |
| Korrektur | 2. zum gleichen Thema | sofort als Regel persistieren |
| Score | letzter < 2.5 | ausfuehrlicher Rueckblick + Ursachenanalyse |
| Trend | 3 Sessions in Folge < 3.0 | `/self-improve` |

## Manuell Hyperagent spawnen

"analysiere die Session" / "wie war die Session?" → Hyperagent (tiefe 5-Stufen-Analyse).
"Session-Trend" → `~/.claude/session-scores.jsonl` lesen.

## Was NIEMALS passieren darf

- Session beenden ohne Session-Scorer · Drift-Alarm ignorieren · 2. Korrektur nicht persistieren
- Session-Scores faelschen · Echtzeit-Tracker als "nice to have" behandeln (sie sind Pflicht)
