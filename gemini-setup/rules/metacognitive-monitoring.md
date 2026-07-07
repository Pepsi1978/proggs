# Metacognitives Monitoring: Echtzeit-Selbstbeobachtung (KRITISCH)

<!-- Adapted from MIRROR-2026-03-31-WIN-001 by import agent on 2026-04-17 -->

> Quelle: Hyperagent-Pattern (Meta AI, arXiv 2603.19461) + SICA (Self-Improving Coding Agent)
> Direktive: #2 Selbstbeobachtung + #1 Superintelligenz
>
> Diese Regel ist die Implementierung der Metacognitions-Schicht. Sie ergaenzt die
> allgemeine Selbstbeobachtungs-Direktive (`self-observation.md`) mit konkreten Trackern,
> Alarmschwellen und dem Session-Score-System.

---

## 4 Echtzeit-Tracker (IMMER AKTIV)

Diese vier Sensoren laufen AUTOMATISCH in jeder Session. Sie sind die Implementierung
der Echtzeit-Ueberwachung aus Direktive #2 — konkret und messbar.

### Tracker 1: Retry-Zaehler

**Was er zaehlt:** Fehlgeschlagene Tool-Aufrufe und wiederholte Versuche fuer die GLEICHE Sache.

| Schwelle | Reaktion |
|----------|----------|
| >2 Retries fuer gleiche Aufgabe | **STOP** — Hypothesen-Loop starten (siehe debugging-and-verification.md) |
| >3 Retries gesamt in Session | Warnsignal im Intelligenz-Vorschlag am Ende |

### Tracker 2: Korrektur-Zaehler

**Was er zaehlt:** Benutzer-Korrekturen ("nein", "stop", "anders", Richtungsaenderungen).

| Schwelle | Reaktion |
|----------|----------|
| Erste Korrektur | Mental notieren, Ursache verstehen |
| Zweite Korrektur zum GLEICHEN Thema | **PFLICHT: Sofort als Regel oder Memory persistieren** |

### Tracker 3: Drift-Detektor

**Was er prueft:** Alle ~10 Tool-Calls mental fragen: "Arbeite ich noch am urspruenglichen Ziel?"

| Signal | Reaktion |
|--------|----------|
| Arbeit an etwas das nicht angefragt wurde | **Benutzer informieren und zuruecklenken** |
| Mehr als 5 Turns ohne messbaren Fortschritt am Ziel | Scope reduzieren oder Benutzer fragen |

### Tracker 4: Wissens-Vertrauen (optional, bei risikoreichen Aufgaben)

**Was er prueft:** Ist die verwendete Information (Pfade, Versionen, API-Parameter) noch aktuell?

| Signal | Reaktion |
|--------|----------|
| Information aus alter Session oder Training | Confidence-Ampel GELB/ROT — nachschlagen |
| Pfad/Version die sich geaendert haben koennte | Vor Verwendung verifizieren |

---

## Session-Score-System

Der Session-Score ist ein quantitatives Mass fuer die Session-Qualitaet. Er wird automatisch
vom `session-scorer.sh`-Hook am SessionEnd erfasst und in `~/.Gemini/session-scores.jsonl`
gespeichert.

### Die 4 Bewertungs-Dimensionen

| Dimension | Gewicht | Was bewertet wird |
|-----------|---------|------------------|
| **Intent-Treue** | 25% | Hat Gemini das urspruengliche Ziel erreicht? |
| **Effizienz** | 25% | Minimale Schritte, keine unnoetige Retries |
| **Memory-Aktualitaet** | 25% | Waren alle verwendeten Informationen aktuell? |
| **Lernertrag** | 25% | Wurden Erkenntnisse persistiert? |

### Bewertungsskala (1-5)

| Score | Bedeutung |
|-------|-----------|
| 5 | Perfekt — kein Fehler, kein Umweg, Ziel vollstaendig erreicht |
| 4 | Gut — kleine Umwege oder 1-2 Retries, Ziel erreicht |
| 3 | Akzeptabel — einige Umwege, Ziel teilweise erreicht |
| 2 | Verbesserungswuerdig — viele Retries, Ziel mit Abstrichen erreicht |
| 1 | Kritisch — Ziel verfehlt, viele Fehler, keine Lernergebnisse |

---

## Alarmschwellen und automatische Interventionen

| Alarm | Bedingung | Automatische Aktion |
|-------|-----------|---------------------|
| **Retry-Alarm** | >3 Retries fuer gleiche Sache | Debugging-Hypothesen-Loop starten |
| **Drift-Alarm** | >5 Turns ohne Fortschritt am Ziel | Benutzer informieren, Scope reduzieren |
| **Korrektur-Alarm** | 2. Korrektur zum gleichen Thema | Sofort als Regel persistieren |
| **Score-Alarm** | Letzter Session-Score < 2.5 | Ausfuehrlichen Rueckblick mit Ursachenanalyse |
| **Trend-Alarm** | 3 aufeinander folgende Sessions < 3.0 | `/self-improve` ausfuehren |

---

## Defense in Depth: 6 Absicherungsschichten

| Schicht | Mechanismus | Was sie sichert |
|---------|-------------|-----------------|
| 1 | `hyperagent-stop.sh` Hook (Stop-Event) | Injiziert metacognitive Erinnerung nach jedem >5-Turn-Schritt |
| 2 | `session-scorer.sh` Hook (SessionEnd) | Schreibt quantitative Metriken in JSONL |
| 3 | Echtzeit-Tracker 1-4 (diese Datei) | Erkennt Probleme WAEHREND der Arbeit |
| 4 | Intelligenz-Vorschlaege (Direktive #2) | Persistiert Erkenntnisse NACH der Arbeit |
| 5 | Hyperagent (agents/hyperagent.md) | Tiefe 5-Stufen-Analyse bei Bedarf |
| 6 | Trend-Analyse (session-scores.jsonl) | Erkennt systemische Probleme ueber Sessions hinweg |

---

## Compound-Gains-Theorie

> "Jede Analyse produziert mindestens 1 Verbesserung. Ueber Sessions akkumulieren sich
> diese Verbesserungen exponentiell — der Compound Intelligence Effect in Aktion."

### Wie die Gains entstehen

1. Session-Scorer schreibt Metriken → Trend wird sichtbar
2. Hyperagent analysiert Muster → Verbesserungen werden identifiziert
3. Echtzeit-Tracker erkennen Probleme → Sofortige Korrekturen moeglich
4. Persistierte Regeln verhindern Wiederholungen → Naechste Session startet besser

### Was NIEMALS passieren darf

- ❌ Session beenden ohne dass der Session-Scorer gelaufen ist
- ❌ Drift-Alarm ignorieren und blind weitermachen
- ❌ Korrektur-Alarm ignorieren — zweite Korrektur MUSS persistiert werden
- ❌ Session-Scores faelschen (hoeher setzen als berechnet)
- ❌ Echtzeit-Tracker als "Nice-to-Have" behandeln — sie sind PFLICHT

---

## Manuell Hyperagent spawnen

Der Hyperagent kann jederzeit manuell fuer eine tiefe 5-Stufen-Analyse gespawnt werden:

| Trigger | Was passiert |
|---------|-------------|
| "analysiere die Session" | Hyperagent mit vollstaendiger 5-Stufen-Analyse |
| "wie war die Session?" | Gleich — mit Session-Score-Ausgabe |
| "Session-Trend zeigen" | Liest `~/.Gemini/session-scores.jsonl` und zeigt Trend |

---

## Zusammenspiel mit anderen Systemen

| System | Zusammenspiel |
|--------|--------------|
| `self-observation.md` | Diese Datei ERGAENZT (konkrete Tracker/Schwellen) — nicht ersetzen |
| `hyperagent.md` | Fuer tiefe Analyse wenn Tracker Alarm schlagen |
| `session-scores.jsonl` | Datenquelle fuer Trend-Analyse und Selbstverbesserung |
| `sica-utility-metric.md` | Ergaenzende Metrik (utility_score) — liest session-scores als Input |
| `intelligence-system.md` | Pheromon-Tabelle wird mit Erkenntnissen bestaetigt/erganzt |
| `/self-improve` Skill | Konsumiert Session-Score-Trend als Diagnose-Grundlage |

