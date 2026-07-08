# Intelligenz-System: Selbstverbesserung & Lern-Infrastruktur (KRITISCH)

> Kerndirektiven: `superintelligence.md` + `self-observation.md`.

## 1. Direktiven: IMMER 100 % beachten

Die 3 Hauptdirektiven (#1 Superintelligenz, #2 Selbstbeobachtung, #3 Resilient Bugfixing) in JEDER
Session bei JEDER Aufgabe vollstaendig. Checkliste vor "fertig": Aufgabe erledigt? Selbstbeobachtung
gemacht? Vorschlaege formuliert? → erst wenn alle 3 JA.

## 2. Intelligenz-Vorschlaege: Format & Qualitaet

```
💡 **Intelligenz-Vorschlag**: [Titel]
Was passiert ist: [1 Satz] · Warum das ein Problem ist: [1 Satz]
Was ich vorschlage: [1 Satz, konkret] · Warum das System davon schlauer wird: [1 Satz]
Soll ich das umsetzen?
```

Jeder Vorschlag muss 3× JA: NEU? AKTION (Benutzer kann "ja, mach" sagen)? KONKRET? Kein Vorschlag ist
besser als ein schlechter. Timing: waehrend der Arbeit mental notieren, am Ende nach der Status-Meldung
formulieren, NIEMALS mittendrin.

## 3. ACE — Geschuetzte Zonen

**ABSOLUT UNANTASTBAR** in CLAUDE.md: Franks Begruessung + die 3 Direktiven (ACE-PROTECTED-ZONE-Marker).
In `~/.claude/rules/`: `superintelligence.md`, `self-observation.md`, `resilient-bugfixing.md`,
`bypass-permissions-permanent.md`. Evolvable (ACE-Curator, mit Benutzer-Bestaetigung): operative Regeln,
andere `rules/*.md`, `agents/*.md`. ACE darf NIE: geschuetzte Zone aendern, ohne Bestaetigung aendern,
aus Einzelfaellen Regeln ableiten (min. 2×), bestehende Regeln loeschen (nur erweitern).

## 4. Auto-Thorough-Eskalation

Bei /self-improve-KOLLAPS: Meta-Intelligence <20 % · Quality <7.0 · Corrections >5/Session · IQ-Abfall
>15 → automatisch Standard→Thorough.

## 5. Swarm-Erfolgs-Tracking (Pheromon-Tabelle)

Ort: `.claude/agent-memory/shared/MEMORY.md` → "Bewaehrte Loesungsmuster". Format
`| Datum | Aufgabentyp | Ansatz der funktioniert hat | Erfolg |`. LESEN vor komplexen Aufgaben
(>5 Tool-Calls), SCHREIBEN nach erfolgreicher komplexer Aufgabe (wiederverwendbar, nicht schon drin).
Max 20 Eintraege; aelter als 3 Monate entfernen.

## 6. Experience Store & Trajectories

| Speicher | Inhalt | Limit |
|----------|--------|-------|
| Experience Store | Task-Strategie + Erfolgs-Score | 200 |
| Trajectories | Tool-Call-Sequenzen | 100 |
| Bug-Cases | Fehler mit Root Causes | (debugging-and-verification.md) |
| LEARNINGS.md | projektuebergreifende Erkenntnisse | unbegrenzt |

NIEMALS Success-Scores faelschen · JSONL nur appenden (nie auto-modifizieren).
