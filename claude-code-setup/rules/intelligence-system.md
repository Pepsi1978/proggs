# Intelligenz-System: Selbstverbesserung, Meta-Kognition & Lernen (KRITISCH)

> Mechanik-Schicht zu den 3 Direktiven. **Volltext (Denk-Prozess, Lern-Speicher, Near-Miss-Retention):
> `claude-code-setup/docs/rules/intelligence-system.md`.**

## Checkliste vor "fertig"
Aufgabe erledigt? Selbstbeobachtung gemacht? Intelligenz-Vorschlaege formuliert? → erst wenn alle 3 JA.

## Intelligenz-Vorschlaege (Format)
```
💡 **Intelligenz-Vorschlag**: [Titel]
Was passiert ist: … · Warum das ein Problem ist: … · Was ich vorschlage: … · Warum das System schlauer wird: …
Soll ich das umsetzen?
```
3x JA: NEU? AKTION (Benutzer kann "ja mach" sagen)? KONKRET? Timing: am Ende NACH der Status-Meldung,
NIEMALS mittendrin. Kein Vorschlag ist besser als ein schlechter.

## Echtzeit-Tracker (Ergaenzung zu Direktive #2)
**Retry** >2/Aufgabe → STOP, Hypothesen-Loop. **Korrektur** 2. zum gleichen Thema → sofort als
Regel/Memory persistieren. **Drift** alle ~10 Tool-Calls "noch am Ziel?", >5 Turns ohne Fortschritt →
Scope reduzieren. **Wissens-Vertrauen** aus alter Session → Confidence-Ampel, nachschlagen. Session-Score
via `session-scorer`-Hook (SessionEnd). Denk-Prozess (Tool-Planung, Ensemble-Reasoning, Spec-First): Volltext.

## ACE — Geschuetzte Zonen
UNANTASTBAR: Franks Begruessung + die 3 Direktiven + `bypass-permissions-permanent.md`. Evolvable (nur mit
Bestaetigung): operative Regeln. ACE darf NIE: geschuetzte Zone aendern, ohne Bestaetigung aendern, aus
Einzelfaellen ableiten (min. 2x), bestehende Regeln loeschen (nur erweitern). Lern-Speicher (Experience
Store 200, Trajectories 100, Pheromon-Tabelle) + Near-Miss-Retention: Volltext. NIEMALS Success-Scores faelschen.
