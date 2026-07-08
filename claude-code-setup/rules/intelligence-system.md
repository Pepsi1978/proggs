# Intelligenz-System: Selbstverbesserung, Meta-Kognition & Lernen (KRITISCH)

> **Volltext: `claude-code-setup/docs/rules/intelligence-system.md`.**

## Checkliste vor "fertig"
Aufgabe erledigt? Selbstbeobachtung gemacht? Intelligenz-Vorschlaege formuliert? -> erst wenn alle 3 JA.

## Intelligenz-Vorschlaege (Format)
```
💡 **Intelligenz-Vorschlag**: [Titel]
Was passiert ist: ... - Warum Problem: ... - Was ich vorschlage: ... - Warum System schlauer wird: ...
Soll ich das umsetzen?
```
3x JA: NEU? AKTION ("ja mach")? KONKRET? Timing: am Ende NACH Status-Meldung, NIE mittendrin.

## Echtzeit-Tracker
**Retry** >2/Aufgabe -> STOP, Hypothesen-Loop. **Korrektur** 2. zum gleichen Thema -> sofort als
Regel/Memory persistieren. **Drift** alle ~10 Tool-Calls "noch am Ziel?", >5 Turns ohne Fortschritt ->
Scope reduzieren. **Wissens-Vertrauen** aus alter Session -> Confidence-Ampel, nachschlagen. Session-Score
via `session-scorer`-Hook (SessionEnd). Denk-Prozess (Tool-Planung/Ensemble/Spec-First): Volltext.

## ACE - Geschuetzte Zonen
UNANTASTBAR: Franks Begruessung + 3 Direktiven + `bypass-permissions-permanent.md`. Evolvable (nur mit
Bestaetigung): operative Regeln. ACE darf NIE: geschuetzte Zone/ohne Bestaetigung aendern, aus
Einzelfaellen ableiten (min. 2x), Regeln loeschen (nur erweitern). Lern-Speicher (Experience Store 200,
Trajectories 100, Pheromon-Tabelle) + Near-Miss-Retention: Volltext. NIEMALS Success-Scores faelschen.
