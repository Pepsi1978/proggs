# Intelligenz-System: Selbstverbesserung & Lern-Infrastruktur (KRITISCH)

> Konsolidiert aus: ace-protected-zones, auto-thorough-escalation,
> directives-always-100-percent, intelligence-suggestions-format,
> intelligence-suggestions-quality, swarm-success-tracking,
> experience-and-trajectory
>
> Kerndirektiven: siehe ~/.claude/rules/superintelligence.md und
> ~/.claude/rules/self-observation.md

---

## 1. Direktiven: IMMER 100% beachten

Die drei Hauptdirektiven (#1 Superintelligenz, #2 Selbstbeobachtung, #3 Resilient Bugfixing)
muessen in JEDER Session bei JEDER Aufgabe vollstaendig beachtet werden — egal wie klein.

**Checkliste vor "fertig":**
1. Aufgabe erledigt?
2. Selbstbeobachtung gemacht?
3. Vorschlaege formuliert?
→ Erst wenn alle 3 JA sind, ist die Aufgabe wirklich fertig.

---

## 2. Intelligenz-Vorschlaege: Format & Qualitaet

### Format (6-7 Zeilen, einfaches Deutsch)

```
💡 **Intelligenz-Vorschlag**: [Titel]
Was passiert ist: [1 Satz]
Warum das ein Problem ist: [1 Satz]
Was ich vorschlage: [1 Satz, konkret]
Warum das System davon schlauer wird: [1 Satz]
Soll ich das umsetzen?
```

Muss fuer Nicht-Programmierer verstaendlich sein — keine technischen Einzeiler.

### Qualitaets-Regeln

Jeder Vorschlag MUSS diese 3 Fragen mit JA beantworten:
1. **NEU?** Wurde das noch nicht umgesetzt?
2. **AKTION?** Kann der Benutzer "ja, mach das" sagen?
3. **KONKRET?** Weiss der Benutzer genau was passiert?

**Kein Vorschlag ist besser als ein schlechter.** Qualitaet vor Quantitaet.

### Timing

| Wann | Regel |
|------|-------|
| Waehrend der Arbeit | Mental notieren, NICHT aussprechen |
| Am Ende der Aufgabe | Nach der Status-Meldung formulieren |
| NIEMALS | Mittendrin unterbrechen |

---

## 3. ACE — Geschuetzte Zonen & Selbstverbessernde Regeln

> Basierend auf ACE (Agentic Context Engineering), Stanford/UC Berkeley, arXiv 2510.04618

### Geschuetzte Zonen (ABSOLUT UNANTASTBAR)

**In CLAUDE.md:**
| Zone | Markierung |
|------|-----------|
| Franks Begruessung | `ACE-PROTECTED-ZONE-START/END: Franks Begruessung` |
| Die 3 Direktiven | `ACE-PROTECTED-ZONE-START/END: Die 3 Direktiven` |

**In ~/.claude/rules/:**
| Datei | Warum geschuetzt |
|-------|-----------------|
| `superintelligence.md` | Direktive #1 |
| `self-observation.md` | Direktive #2 |
| `resilient-bugfixing.md` | Direktive #3 |
| `bypass-permissions-permanent.md` | Sicherheitskritisch |

### Evolvable Zonen

| Bereich | Wer darf aendern |
|---------|-----------------|
| Operative Regeln in CLAUDE.md | ACE-Curator (mit Benutzer-Bestaetigung) |
| `~/.claude/rules/*.md` (ausser gesperrte) | ACE-Curator (mit Benutzer-Bestaetigung) |
| `~/.claude/agents/*.md` | ACE-Curator (mit Benutzer-Bestaetigung) |

### Defense in Depth (4 Schichten)

1. HTML-Kommentar-Marker in CLAUDE.md (sichtbare Grenzen)
2. Sperrliste im ACE-Curator-Agent
3. Benutzer-Bestaetigung fuer JEDEN Vorschlag
4. Git-Commit fuer jede Aenderung (rueckgaengig machbar)

### Was NIEMALS passieren darf

- ❌ ACE aendert eine geschuetzte Zone
- ❌ ACE aendert Regeln ohne Benutzer-Bestaetigung
- ❌ ACE erstellt Regeln basierend auf Einzelfaellen (min. 2x aufgetreten)
- ❌ ACE loescht bestehende Regeln (nur erweitern oder neue erstellen)

---

## 4. Auto-Thorough-Eskalation bei Qualitaets-Einbruch

Wenn /self-improve einen KOLLAPS in einer Kerndimension meldet:

| Dimension | Schwelle |
|-----------|----------|
| Meta-Intelligence | < 20% |
| Quality Score | < 7.0 |
| Corrections | > 5 pro Session |
| IQ-Score | Abfall > 15 Punkte |

→ Automatisch Standard → Thorough-Modus (alle Diagnostik-Tools, alle Fehler fixen).

---

## 5. Swarm-Erfolgs-Tracking (Pheromon-Tabelle)

> Quelle: Superintelligenz Finding 4 — Swarm Intelligence, Frontiers in AI 2025

### Ort
`.claude/agent-memory/shared/MEMORY.md` → Sektion "Bewaehrte Loesungsmuster"

### Format
```
| [Datum] | [Aufgabentyp] | [Ansatz der funktioniert hat] | [Erfolg: Hoch/Mittel] |
```

### Wann LESEN
Vor komplexen Aufgaben (>5 Tool-Calls): Tabelle lesen, bewaehrten Ansatz als ERSTEN Versuch.

### Wann SCHREIBEN
Nach erfolgreicher komplexer Aufgabe WENN: >5 Tool-Calls UND wiederverwendbar UND nicht schon drin.

### Limits
- Max 20 Eintraege. Bei Ueberschreiten: aeltesten "Mittel"-Eintrag entfernen.
- Eintraege aelter als 3 Monate: entfernen.

---

## 6. Experience Store & Trajectories

### 4 Speicherorte

| Speicher | Inhalt | Limit |
|----------|--------|-------|
| **Experience Store** | Task-Strategie + Erfolgs-Score pro Aufgabe | 200 Eintraege |
| **Trajectories** | Tool-Call-Sequenzen pro Session | 100 Eintraege |
| **Bug-Cases** | Fehler mit Root Causes | (siehe debugging-and-verification.md) |
| **LEARNINGS.md** | Projekt-uebergreifende technische Erkenntnisse | unbegrenzt |

### Schreib-Protokoll
- Nach jeder nicht-trivialen Aufgabe: Experience-Eintrag mit ehrlichem Success-Score
- Bei Muster-Erkennung in Trajectories: als Regel oder Skill-Kandidat formulieren

### Kritische Regeln
- NIEMALS Success-Scores faelschen
- NIEMALS JSONL-Dateien automatisch modifizieren (nur appenden)
- Trajectory-Analyse fuer Pattern-Erkennung nutzen (welche Tool-Sequenzen wiederholen sich?)
