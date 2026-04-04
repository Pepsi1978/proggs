# Erfahrungsspeicher: Training-Free Learning (KRITISCH)

> Basierend auf JitRL (arXiv 2601.18510) und MemRL (arXiv 2601.03192):
> Trainingsfreies Lernen aus vergangenen Erfahrungen.
> Direktive: #1 Superintelligenz + #2 Selbstbeobachtung + #3 Resilient Bugfixing

## Regel

Das System lernt aus jeder Session — ohne Modell-Training.
Nach jeder nicht-trivialen Session wird ein Erfahrungs-Eintrag gespeichert.
Vor jeder komplexen Aufgabe werden relevante Erfahrungen abgerufen.

## Die 3 Speicher (komplementaer, nicht redundant)

| Speicher | Pfad | Was er speichert | Granularitaet |
|----------|------|------------------|---------------|
| **Experience Store** | `.claude/agent-memory/shared/experience-store.jsonl` | Aufgaben-Erfahrungen (Strategie, Erfolg, Tags) | Pro Aufgabe |
| **Trajectories** | `.claude/agent-memory/shared/trajectories.jsonl` | Tool-Call-Sequenzen pro Session | Pro Session |
| **Bug-Cases** | `.claude/agent-memory/shared/bug-cases.jsonl` | Fehler mit Root Cause und Fix | Pro Bug |

Zusaetzlich: **LEARNINGS.md** (`~/proggs/LEARNINGS.md`) als manuell gepflegte,
projektuebergreifende Wissensbasis mit detaillierten technischen Learnings.

## Wann SCHREIBEN (nach der Aufgabe)

### Experience Store — nach jeder nicht-trivialen Aufgabe (>5 Tool-Calls)

Format:
```json
{"date":"2026-04-04","task_type":"android-ui-fix","task_description":"ScrollBug in Pager","strategy":"Screenshot → Inspect → Edit → Build","tools_used":["Read","Bash","Edit","Bash"],"tool_count":8,"error_count":1,"success_score":4,"learnings":"NestedScrollConnection war noetig","tags":["android","compose","ui"]}
```

Pflicht-Felder: date, task_type, strategy, tool_count, error_count, success_score (1-5), tags
Optional: task_description, tools_used, learnings

### Trajectories — nach jeder Session mit >10 Tool-Calls

Format:
```json
{"date":"2026-04-04","session_id":"abc","task_description":"Feature X","tool_sequence":["Read","Grep","Edit","Bash"],"errors":[],"corrections":[],"duration_minutes":15,"success":true,"tags":["android","feature"]}
```

### LEARNINGS.md — bei grundlegend neuem technischem Wissen

Nur wenn ein Learning PROJEKTUEBERGREIFEND nuetzlich ist und noch nicht in
LEARNINGS.md steht. Format: Ueberschrift → Problem → Loesung → Code-Beispiel → Fallstricke.

## Wann LESEN (vor der Aufgabe)

### Automatisch (durch Claude selbst, ohne Agent)
Bei Aufgaben die "aehnlich" zu frueheren klingen:
1. Tags aus der Aufgabenbeschreibung ableiten
2. `grep` nach Tags in experience-store.jsonl
3. Bei Treffer: Kurzhinweis in die Antwort einbauen

### Via Experience Retriever Agent
Bei komplexen Aufgaben (>5 erwartete Tool-Calls) den `experience-retriever` Agent starten.
Der durchsucht ALLE 4 Quellen systematisch.

## Groessenlimits und Pruning

| Speicher | Max. Eintraege | Pruning-Strategie |
|----------|---------------|-------------------|
| experience-store.jsonl | 200 | Aelteste mit score < 3 zuerst loeschen |
| trajectories.jsonl | 100 | Aelteste zuerst loeschen |
| bug-cases.jsonl | 100 | Nie automatisch loeschen (Fehler-Wissen ist wertvoll) |
| LEARNINGS.md | Unbegrenzt | Manuell durch Benutzer gepflegt |

Pruning wird durch /self-improve durchgefuehrt.

## Defense in Depth (Direktive 3)

| Schicht | Mechanismus | Was es schuetzt |
|---------|-------------|----------------|
| 1 | 4 unabhaengige Speicher | Faellt einer aus, liefern die anderen noch Wissen |
| 2 | JSONL-Format | Jede Zeile ist eigenstaendig — eine korrupte Zeile zerstoert nicht den Rest |
| 3 | Graceful Degradation | Fehlende/leere Datei → Info-Meldung, kein Crash |
| 4 | Git-Versionierung | Alle Speicher im Repo → jederzeit wiederherstellbar |
| 5 | Score-Filter | Schlechte Erfahrungen (score < 3) werden nicht als "bewaehrt" empfohlen |
| 6 | Groessenlimit | Speicher wachsen nicht unkontrolliert |

## Was NIEMALS passieren darf

- ❌ Session beenden ohne Erfahrung zu speichern (bei >5 Tool-Calls)
- ❌ Komplexe Aufgabe starten ohne Erfahrungen zu pruefen (bei >5 erwarteten Tool-Calls)
- ❌ Falsche Erfahrungen speichern (success_score muss ehrlich sein)
- ❌ LEARNINGS.md duplizieren — nur NEUE Learnings eintragen
- ❌ Speicher ueber Groessenlimit wachsen lassen ohne Pruning
- ❌ experience-store.jsonl oder trajectories.jsonl mit sed/awk bearbeiten (nur Python oder Append)

## Integration mit bestehenden Systemen

- **CBR (bug-cases.jsonl)**: Experience Store ergaenzt Bug-Cases — CBR fuer Fehler,
  Experience Store fuer erfolgreiche Strategien
- **Pheromon-Tabelle**: Bewaehrte Muster aus dem Experience Store fliessen in die Tabelle
- **ACE-Curator**: Liest Experience Store als Datenquelle fuer Regelverbesserungen
- **Trajectory-Analyzer**: Analysiert Trajectories fuer Muster-Erkennung
- **Session-Scores**: Quantitative Ergaenzung zu den qualitativen Erfahrungen
