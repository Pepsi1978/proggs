# Erfahrungsspeicher & Trajectory-Analyse (KRITISCH)

> Direktive: #1 Superintelligenz + #2 Selbstbeobachtung + #3 Resilient Bugfixing
> Das System lernt aus jeder Session — ohne Modell-Training.

## Die 4 Speicher-Orte

| Speicher | Pfad | Was er speichert |
|----------|------|------------------|
| **Experience Store** | `.claude/agent-memory/shared/experience-store.jsonl` | Strategie + Erfolg pro Aufgabe |
| **Trajectories** | `.claude/agent-memory/shared/trajectories.jsonl` | Tool-Call-Sequenzen pro Session |
| **Bug-Cases** | `.claude/agent-memory/shared/bug-cases.jsonl` | Fehler mit Root Cause und Fix |
| **LEARNINGS.md** | `~/proggs/LEARNINGS.md` | Projektuebergreifende technische Erkenntnisse |

## SCHREIBEN — nach Aufgaben und Sessions

### Nach jeder nicht-trivialen Aufgabe (>5 Tool-Calls) → Experience Store

Was speichern:
- **Was war die Aufgabe?** (task_type, task_description)
- **Welche Strategie hat funktioniert?** (strategy — z.B. "Screenshot → Inspect → Edit → Build")
- **Wie gut lief es?** (success_score 1-5, error_count)
- **Welche Tags passen?** (tags — z.B. ["android", "compose", "ui"])
- **Was wurde gelernt?** (learnings — z.B. "NestedScrollConnection war noetig")

Format: JSON-Zeile an experience-store.jsonl anhaengen:
```json
{"date":"YYYY-MM-DD","task_type":"...","strategy":"...","tool_count":N,"error_count":N,"success_score":N,"learnings":"...","tags":["..."]}
```

### Nach jeder Session mit >10 Tool-Calls → Trajectories

Was speichern:
- **Vollstaendige Tool-Sequenz** (tool_sequence — die Reihenfolge aller Tool-Aufrufe)
- **Aufgetretene Fehler** (errors — jeder Build-Fehler, jeder fehlgeschlagene Befehl)
- **Benutzer-Korrekturen** (corrections — jedes "nein", "nicht so", "anders")
- **Dauer und Erfolg** (duration_minutes, success)

Format: JSON-Zeile an trajectories.jsonl anhaengen:
```json
{"date":"YYYY-MM-DD","task_description":"...","tool_sequence":["Read","Grep","Edit","Bash"],"errors":[],"corrections":[],"duration_minutes":N,"success":true,"tags":["..."]}
```

### Bei grundlegend neuem technischem Wissen → LEARNINGS.md

Nur wenn das Learning PROJEKTUEBERGREIFEND nuetzlich ist und noch nicht in LEARNINGS.md steht.
Format: Ueberschrift → Problem → Loesung → Code-Beispiel → Fallstricke.

## LESEN — vor komplexen Aufgaben

### Wann lesen (PFLICHT)
- Vor jeder Aufgabe die voraussichtlich >5 Tool-Calls braucht
- Wenn eine Aufgabe "aehnlich" zu einer frueheren klingt
- Wenn ein Fehler auftritt (zuerst bug-cases.jsonl durchsuchen — siehe `cbr-bugfix-search.md`)

### Wie lesen
1. **Tags ableiten** aus der Aufgabenbeschreibung (z.B. "Android UI Fix" → tags: android, ui)
2. **Grep** nach diesen Tags in experience-store.jsonl und bug-cases.jsonl
3. **Bei Treffer**: Die bewaehrte Strategie als ERSTEN Ansatz verwenden
4. **Bei keinem Treffer**: Eigenen Ansatz waehlen, am Ende als neue Erfahrung speichern

### Was aus den Erfahrungen lesen
- **Experience Store**: Welche Strategie hat bei aehnlichen Aufgaben funktioniert? Welche Fehler traten auf?
- **Bug-Cases**: Gibt es einen bekannten Fehler mit dieser Fehlermeldung? Was war die Root Cause?
- **Trajectories**: Wie viele Tool-Calls brauchten aehnliche Aufgaben? (Effizienz-Benchmark)
- **LEARNINGS.md**: Gibt es ein dokumentiertes Learning zu diesem Thema?

## Trajectory-Analyse (was aus den Ablaeufen gelernt wird)

Trajectories werden periodisch (via /self-improve oder manuell) auf 3 Muster-Typen analysiert:
1. **Wiederkehrende Sequenzen** (gleiche Tool-Kette in 3+ Sessions) → Kandidat fuer Micro-Agent oder Skill
2. **Wiederkehrende Fehler** (gleicher Fehlertyp in 2+ Sessions) → ALARM, Poka-Yoke-Regel noetig
3. **Ineffiziente Ablaeufe** (15 Tool-Calls wo 5 gereicht haetten) → Workflow-Optimierung vorschlagen

## Groessenlimits (durch /self-improve geprueft)

| Speicher | Max. Eintraege | Pruning |
|----------|---------------|---------|
| experience-store.jsonl | 200 | Aelteste mit score < 3 zuerst |
| trajectories.jsonl | 100 | Aelteste zuerst |
| bug-cases.jsonl | 100 | Nie automatisch loeschen |

## Timing

- **Experience Store**: Nach Abschluss jeder nicht-trivialen Aufgabe sofort schreiben
- **Trajectories**: Als LETZTER Schritt vor Session-Ende schreiben (nach dem letzten Commit)
- **LEARNINGS.md**: Sofort wenn ein neues projektuebergreifendes Learning erkannt wird

## Was NIEMALS passieren darf

- ❌ Session beenden ohne Erfahrung zu speichern (bei >5 Tool-Calls)
- ❌ Komplexe Aufgabe starten ohne Erfahrungen zu pruefen
- ❌ success_score beschoenigen — ehrlich bewerten
- ❌ JSONL-Dateien mit sed/awk bearbeiten (nur Python oder Append)
- ❌ Trajectory-Analyzer darf NIEMALS automatisch Regeln oder Agents aendern — NUR vorschlagen
