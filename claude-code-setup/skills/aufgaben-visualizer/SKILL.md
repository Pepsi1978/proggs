---
name: aufgaben-visualizer
description: Zeigt den Active-Task-Ledger (`~/proggs/.claude/agent-memory/shared/active-tasks.jsonl`) als farbige Tabelle — was wurde gemacht, was ist offen, was ist gepusht. Nutze diesen Skill IMMER wenn der Benutzer sagt "zeig die offenen Aufgaben", "Aufgaben-Uebersicht", "Ledger zeigen", "Task-Ledger zeigen", "was steht alles offen", "zeig alle Aufgaben", "Aufgaben-Liste", "Aufgaben-Tabelle", "was ist alles in Arbeit", "welche Aufgaben sind committed", "was ist noch nicht gepusht", "Status der Aufgaben", "Uebersicht ueber Aufgaben", "alles was offen ist". Ergaenzt den `aufgaben-bruecke`-Skill: bruecke ist fuer "ich mache jetzt weiter", visualizer ist fuer "ich will mir nur einen Ueberblick verschaffen".
---

# Aufgaben-Visualizer — Skill

Zeigt den gesamten Active-Task-Ledger als kompakte, farbige Tabelle.
Im Gegensatz zu `aufgaben-bruecke` (der nur die letzten 10 Resume-Kandidaten zeigt
und einen davon vorschlaegt) gibt der Visualizer die volle Uebersicht — alle
Eintraege, alle Status, alle Sessions.

## Ablauf

### Schritt 1: Ledger einlesen

```bash
PYTHONIOENCODING=utf-8 python3 ~/.claude/hooks/task-ledger-helper.py list
```

Optional mit Status-Filter:

```bash
PYTHONIOENCODING=utf-8 python3 ~/.claude/hooks/task-ledger-helper.py list in_progress
PYTHONIOENCODING=utf-8 python3 ~/.claude/hooks/task-ledger-helper.py list paused
PYTHONIOENCODING=utf-8 python3 ~/.claude/hooks/task-ledger-helper.py list committed
PYTHONIOENCODING=utf-8 python3 ~/.claude/hooks/task-ledger-helper.py list done
```

### Schritt 2: Tabelle ausgeben

Standard-Spalten:

| # | Letzte Aktivitaet | Status | Aufgabe (Wortlaut-Auszug) | Files | Commits | Push |
|---|-------------------|--------|---------------------------|-------|---------|------|

- **Letzte Aktivitaet**: `timestamp_last_update` umgerechnet auf "vor X Stunden / Tagen"
- **Status**: Symbol gemaess Map (siehe unten)
- **Aufgabe**: erste Zeile des `prompt_text`, max 80 Zeichen
- **Files**: Anzahl `files_changed`
- **Commits**: Anzahl `commits`
- **Push**: ✅ wenn `pushed=true`, sonst leer

Status-Symbol-Map:

| Status | Symbol | Bedeutung |
|--------|--------|-----------|
| `open` | 🔴 | nichts gestartet |
| `in_progress` | 🟡 | mittendrin (oft die aktuelle Session) |
| `paused` | ⏸️ | Stop ohne Commit — vermutlich abgebrochen |
| `committed` | 🟢 | lokal committed, aber nicht gepusht |
| `done` | ✅ | committed UND gepusht |

### Schritt 3: Zusammenfassung am Ende

Unter der Tabelle eine 1-Zeilen-Zusammenfassung:

```
Insgesamt: 42 Eintraege | offen: 1 🔴 | in Arbeit: 0 🟡 | pausiert: 3 ⏸️ | committed: 8 🟢 | erledigt: 30 ✅
```

### Schritt 4: Aktion anbieten

Direkt nach der Tabelle:

```
Soll ich:
  1. eine bestimmte Aufgabe im Detail anzeigen (Nummer nennen)?
  2. mit einer der offenen Aufgaben weitermachen (-> aufgaben-bruecke)?
  3. alte erledigte Eintraege archivieren (status=done)?
```

## Filter-Heuristik

Wenn die Trigger-Phrase Status-spezifisch ist, automatisch filtern:

| Phrase | Filter |
|--------|--------|
| "was ist noch offen" / "was ist nicht erledigt" | nicht `done` |
| "was ist gerade in Arbeit" | `in_progress` |
| "was ist committed aber nicht gepusht" | `committed` |
| "was ist fertig" / "was ist erledigt" | `done` |
| "zeig alles" / "Aufgaben-Uebersicht" | alles, kein Filter |

## Was NIEMALS passieren darf

- ❌ Den Ledger ueberschreiben (Visualizer ist read-only)
- ❌ Eintraege heimlich loeschen — fuer Archivierung explizit Benutzer fragen
- ❌ Wortlaut paraphrasieren — `prompt_text` immer wortwoertlich kuerzen (Mitte abschneiden statt umschreiben)
- ❌ Bei leerem Ledger einen leeren Tabellenkopf zeigen — stattdessen: "Ledger ist leer. Bei der naechsten Aufgabe schreiben die Hooks automatisch."

## Compound Intelligence Effect

- Mit `aufgaben-bruecke` zusammen die beiden Augen des Konto-Wechsel-Systems:
  visualizer fuer Ueberblick, bruecke fuer Fortsetzung.
- Liefert Daten fuer `/self-improve` und Hyperagent: zeigt welche Aufgabentypen
  oft "paused" enden — Hinweis auf Workflow-Probleme.
