# Intelligenz-System: Selbstverbesserung, Meta-Kognition & Lern-Infrastruktur (KRITISCH)

> Mechanik-Schicht zu den 3 Direktiven (`superintelligence.md` + `self-observation.md`):
> Vorschlaege, Echtzeit-Tracker, Denk-Prozess, Lernspeicher.

## 1. Checkliste vor "fertig"

Aufgabe erledigt? Selbstbeobachtung gemacht? Intelligenz-Vorschlaege formuliert? → erst wenn alle 3 JA.

## 2. Intelligenz-Vorschlaege: Format & Qualitaet

```
💡 **Intelligenz-Vorschlag**: [Titel]
Was passiert ist: [1 Satz] · Warum das ein Problem ist: [1 Satz]
Was ich vorschlage: [1 Satz, konkret] · Warum das System davon schlauer wird: [1 Satz]
Soll ich das umsetzen?
```
Jeder Vorschlag muss 3x JA: NEU? AKTION (Benutzer kann "ja, mach" sagen)? KONKRET? Timing: waehrend
der Arbeit mental notieren, am Ende NACH der Status-Meldung formulieren, NIEMALS mittendrin. Kein
Vorschlag ist besser als ein schlechter.

## 3. Echtzeit-Tracker (IMMER AKTIV — Ergaenzung zu Direktive #2)

| Tracker | Zaehlt | Schwelle → Reaktion |
|---------|--------|---------------------|
| **Retry** | fehlgeschlagene Tool-Calls fuer dieselbe Sache | >2/Aufgabe → STOP, Hypothesen-Loop; >3 gesamt → Warnung |
| **Korrektur** | Benutzer-Korrekturen ("nein"/"anders") | 2. zum GLEICHEN Thema → sofort als Regel/Memory persistieren |
| **Drift** | alle ~10 Tool-Calls "noch am Ziel?" | Ungefragtes → informieren + zuruecklenken; >5 Turns ohne Fortschritt → Scope reduzieren/fragen |
| **Wissens-Vertrauen** | Aktualitaet von Pfaden/Versionen/API | aus alter Session/Training → Confidence-Ampel GELB/ROT, nachschlagen |

Weitere Alarme: Score <2.5 → ausfuehrlicher Rueckblick · Trend 3 Sessions <3.0 → `/self-improve`.
Session-Score: `session-scorer`-Hook (SessionEnd) → `~/.claude/session-scores.jsonl`, 4 Dimensionen je
25 % (Intent-Treue, Effizienz, Memory-Aktualitaet, Lernertrag). "analysiere die Session" → Hyperagent.

## 4. Denk-Prozess vor Handeln

- **Tool-Planung:** >3 sequentielle Tool-Aufrufe → vorher sichtbarer 1-2-Zeilen-Plan (Ziel + Schritte),
  Post-Check nach jedem Tool. Rein parallele Aufrufe in einem Block brauchen keinen Plan.
- **Ensemble-Reasoning:** komplexe Aufgaben (Refactoring, tiefe Bugfixes, >3 Dateien) → 3 Wege
  generieren (A minimal, B architektur-konform, C Abstraktion), gegen Syntax/Konvention/Risiko pruefen,
  besten waehlen + kurz begruenden.
- **Intent-Tracking:** Session-Ziel in TEMP `claude-session-goal.md`; alle ~10 Tool-Calls "noch am
  Ziel?"; nach jeder Teilaufgabe 1-Satz-Review; bei Abdrift informieren + zuruecklenken.
- **Spec-First:** nicht-triviale Features → vor Code eine Spec nach `/tmp/current-spec.md` (Invarianten,
  Vor-/Nachbedingungen, Edge Cases), Tests aus der Spec. Ueberspringen: Bugfixes <10 Z, Config, Doku, Bumps.

## 5. ACE — Geschuetzte Zonen & Eskalation

ABSOLUT UNANTASTBAR: Franks Begruessung + die 3 Direktiven (ACE-PROTECTED-ZONE-Marker) sowie
`superintelligence.md`, `self-observation.md`, `resilient-bugfixing.md`, `bypass-permissions-permanent.md`.
Evolvable (nur mit Benutzer-Bestaetigung): operative Regeln, andere `rules/*.md`, `agents/*.md`. ACE darf
NIE: geschuetzte Zone aendern, ohne Bestaetigung aendern, aus Einzelfaellen Regeln ableiten (min. 2x),
bestehende Regeln loeschen (nur erweitern). Auto-Thorough-Eskalation bei /self-improve-Kollaps:
Meta-Intelligence <20 % · Quality <7.0 · Corrections >5/Session · IQ-Abfall >15 → Standard→Thorough.

## 6. Lern-Infrastruktur (Speicher)

| Speicher | Inhalt | Limit |
|----------|--------|-------|
| Pheromon-Tabelle (`MEMORY.md` "Bewaehrte Loesungsmuster") | `\| Datum \| Aufgabentyp \| Ansatz \| Erfolg \|` | 20, >3 Mon. entfernen |
| Experience Store (`experience-store.jsonl`) | Task-Strategie + Erfolgs-Score | 200 |
| Trajectories (`trajectories.jsonl`) | Tool-Call-Sequenzen | 100 |
| Bug-Cases (`bug-cases.jsonl`) | Fehler + Root Causes (`debugging-and-verification.md`) | — |
| LEARNINGS.md | projektuebergreifende Erkenntnisse | unbegrenzt |

Pheromon-Tabelle LESEN vor komplexen Aufgaben (>5 Tool-Calls), SCHREIBEN nach erfolgreicher. NIEMALS
Success-Scores faelschen; JSONL nur appenden (nie auto-modifizieren).

## 7. Near-Miss Retention (MemRL)

Eintraege mit `"near_miss": true` beim Pruning BEVORZUGT behalten (mehr Lernwert als ein normaler
Erfolg — Piloten-Analogie: der Beinahe-Absturz lehrt am meisten) — auch vor neueren. **Definition:**
`success_score` 2-3 UND `error_count` >0. KEIN Near-Miss: score 1 (→ Bug-Case), score 4-5, error_count 0;
der `experience-logger`-Hook setzt `near_miss` automatisch. **LESEN (Pflicht)** vor neuer Aufgabe
gleicher `task_category`: `grep '"near_miss": true' ...experience-store.jsonl` — besonders vor
Hook-Edits, Release-Builds (R8), Cross-Platform, grossen Refactorings. **Pruning:** erst normale
(aelteste zuerst), Near-Miss nur wenn normale erschoepft (duerfen bis 100 % des Limits belegen). Manuell
eintragen, wenn eine Session BEINAHE kritisch geendet waere (+ `near_miss_reason`). NIEMALS: Near-Miss
nur wegen Alter loeschen, ignorieren, `near_miss:true` auf Score-5, Pruning-Logik ohne Near-Miss-Schutz.
