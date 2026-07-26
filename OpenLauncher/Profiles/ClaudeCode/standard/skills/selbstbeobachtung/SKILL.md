---
name: selbstbeobachtung
description: >
  Nutze diesen Skill AUTOMATISCH nach jeder abgeschlossenen Aufgabe — wenn Commit+Push erledigt 
  ist, wenn eine Aufgabe als "fertig" gemeldet wird, am Ende jeder Session, oder nach >20 Tool-Calls.
  Deutsche Trigger: "was hast du beobachtet", "Rueckblick", "was lief gut", "was lief schlecht",
  "Selbstbeobachtung", "was hast du gelernt", "Session-Rueckblick", "Analyse", "Bewertung",
  "wie war die Session", "Vorschlaege", "Intelligenz-Vorschlag", "was kannst du verbessern".
  Englische Trigger: "review", "retrospective", "what did you learn", "session review".
  AUCH nach trivialen Aufgaben: Selbstbeobachtung ist IMMER aktiv, nicht nur bei grossen Features.
  Dieser Skill liefert die Checkliste fuer Direktive #2 (Selbstbeobachtung).
invocation: auto
---

# Zweite Direktive: Selbstbeobachtung — Aufgaben-Rueckblick

> **Wer arbeitet, beobachtet sich selbst. Ausnahmslos.**
> Dieser Skill wird nach jeder Aufgabe aktiv — nicht erst am Session-Ende.

## Wann dieser Skill aktiv wird

- Nach jedem Commit+Push (Aufgabe abgeschlossen)
- Nach jedem "Fertig"-Signal an den Benutzer
- Wenn der Benutzer nach Rueckblick fragt
- Bei >20 Tool-Calls in einer Aufgabe

## Die 3 Tracker auswerten

### 1. Retry-Zaehler
- Wie oft habe ich den gleichen Befehl wiederholt?
- War ein Circuit Breaker noetig (>3 Retries)?
- Wenn ja: Was war die Root Cause fuer die Wiederholungen?

### 2. Korrektur-Zaehler
- Hat der Benutzer mich korrigiert? Wie oft?
- **Bei >1 Korrektur zum gleichen Thema:** SOFORT als Memory oder Regel persistieren
- Der Benutzer soll NIEMALS das Gleiche zweimal sagen muessen

### 3. Drift-Detektor
- Habe ich am urspruenglichen Ziel gearbeitet?
- Gab es Abschweifungen? Warum?

## Muster erkennen (5 Typen)

| Muster-Typ | Alarmstufe | Aktion |
|------------|------------|--------|
| **Wiederholung** (gleicher Fehler nochmal) | 🔴 HOCH | SOFORT als Regel persistieren |
| **Fehlerklasse** (mehrere Fehler gleiche Ursache) | 🟡 MITTEL | Als Vorschlag formulieren |
| **Benutzer-Praeferenz** (2x korrigiert) | 🔴 HOCH | SOFORT als Memory speichern |
| **Effizienz-Muster** (kuerzerer Weg erkannt) | 🟢 NIEDRIG | Vorschlag wenn sinnvoll |
| **Erfolgs-Muster** (fruehere Regel hat geholfen) | 🟢 NIEDRIG | Bestaetigung: Compound Effect! |

## Intelligenz-Vorschlaege formulieren

Nach der Status-Meldung ("Committed und gepusht"), NICHT mittendrin:

```
💡 **Intelligenz-Vorschlag**: [Was beobachtet/erkannt wurde]
   → [Konkreter Vorschlag] — Soll ich das umsetzen?
```

### Qualitaetsregeln
- Jeder Vorschlag muss **echten Mehrwert** bringen — kein Fuellmaterial
- Jeder Vorschlag muss **in einem Satz erklaerbar** sein
- Jeder Vorschlag muss **umsetzbar** sein ("ich kann jetzt X tun")
- **Kein Vorschlag ist besser als ein schlechter Vorschlag**

### Kategorien
| Kategorie | Wann |
|-----------|------|
| **Praevention** | Fehler beobachtet der verhindert werden kann |
| **Automatisierung** | Wiederkehrender manueller Schritt erkannt |
| **Wissens-Persistierung** | Neues Wissen das gespeichert werden muss |
| **Effizienz** | Kuerzerer Weg erkannt |
| **Bestaetigung** | Frueheres Lernen hat geholfen (Compound Effect) |

## Lernen und Persistieren

| Format | Wann | Wo |
|--------|------|-----|
| Memory-Eintrag | Fakten, Praeferenzen | MEMORY.md |
| Neue Regel | Verhaltensaenderung ab sofort | `~/.claude/rules/` |
| Bug-Case | Fehler mit Root Cause | bug-cases.jsonl |
| Skill-Kandidat | Wiederverwendbares Muster | Vorschlag an Benutzer |

## Goldene Regel

> Der Benutzer soll NIEMALS das Gleiche zweimal sagen muessen.
> Wenn er eine Korrektur macht, wird dieses Wissen SOFORT und DAUERHAFT gespeichert.
> Beim zweiten Mal ist es kein Versehen mehr — es ist ein Systemversagen.
