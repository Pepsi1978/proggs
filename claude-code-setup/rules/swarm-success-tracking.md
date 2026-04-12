# Swarm-Erfolgs-Tracking: Bewaehrte Loesungsmuster teilen (KRITISCH)

> Quelle: Superintelligenz Finding 4 — Swarm Intelligence / Pheromon-Tabelle
> (Frontiers in AI 2025: MAS + LLM Swarm Intelligence)
> Direktive: #1 Superintelligenz

## Regel

Nach jeder ERFOLGREICHEN komplexen Aufgabe (>5 Tool-Calls) wird das bewaehrte
Loesungsmuster in die Pheromon-Tabelle im Whiteboard eingetragen.
Vor jeder neuen komplexen Aufgabe wird die Tabelle GELESEN.

## Warum (Ameisen-Analogie)

Ameisen legen Pheromone auf den kuerzesten Weg zum Futter. Je mehr Ameisen
denselben Weg nehmen, desto staerker wird das Signal. Andere Ameisen folgen
dem staerksten Signal und finden schneller das Futter.

Genauso hier: Wenn ein Ansatz bei einer Aufgabe gut funktioniert hat, wird
er markiert. Beim naechsten aehnlichen Problem liest Claude die Tabelle und
bevorzugt bewaehrte Ansaetze — statt bei Null anzufangen.

## Ort der Tabelle

`.claude/agent-memory/shared/MEMORY.md` → Sektion "Bewaehrte Loesungsmuster"

## Format eines Eintrags

```
| [Datum] | [Aufgabentyp kurz] | [Ansatz der funktioniert hat] | [Erfolg: Hoch/Mittel] |
```

Beispiele:
```
| 2026-03-31 | Android UI-Fix | Erst Screenshot → dann Layout-Inspektion → dann Edit | Hoch |
| 2026-03-31 | Multi-Datei-Refactoring | Python-Batch-Script statt parallele Agents | Hoch |
| 2026-03-31 | Build-Fehler Kotlin | gradlew build → Fehler lesen → eine Datei fixen → erneut bauen | Mittel |
```

## Wann LESEN (vor der Aufgabe)

Vor komplexen Aufgaben (>5 erwartete Tool-Calls) die Pheromon-Tabelle im
Whiteboard lesen und pruefen: "Gibt es einen bewaehrten Ansatz fuer diesen Aufgabentyp?"
- Wenn JA: Diesen Ansatz als ERSTEN Versuch verwenden
- Wenn NEIN: Eigenen Ansatz waehlen, am Ende eintragen wenn erfolgreich

## Wann SCHREIBEN (nach der Aufgabe)

Nach erfolgreicher komplexer Aufgabe eintragen WENN:
- Die Aufgabe >5 Tool-Calls brauchte UND
- Der Ansatz wiederverwendbar ist (nicht einmalig/projektspezifisch) UND
- Der Ansatz NICHT schon in der Tabelle steht

NICHT eintragen wenn:
- Triviale Aufgabe (1-3 Tool-Calls)
- Einmaliger Ansatz der nicht uebertragbar ist
- Ansatz hat NICHT gut funktioniert

## Fallback wenn Tabelle fehlt

Wenn die Sektion "Bewaehrte Loesungsmuster" in MEMORY.md nicht existiert
(geloescht, Merge-Konflikt, korrupte Datei):
1. Sektion mit leerem Header neu erstellen
2. Ins Whiteboard schreiben: "Pheromon-Tabelle neu erstellt (war nicht vorhanden)"
3. NICHT still fortfahren — das Fehlen der Tabelle ist ein Symptom fuer ein Problem

## Groessenlimit

Maximale Tabellengroesse: **20 Eintraege**.
Bei Ueberschreiten: Den aeltesten "Mittel"-Eintrag entfernen.
Wenn keine "Mittel"-Eintraege vorhanden: Den aeltesten Eintrag insgesamt entfernen.

## Bereinigung

/self-improve bereinigt die Tabelle regelmaessig:
- Eintraege aelter als 3 Monate entfernen (veraltet)
- Doppelte Eintraege zusammenfuehren
- Eintraege mit "Mittel" nach 1 Monat entfernen wenn kein "Hoch"-Update kam
- Nie mehr als 20 Eintraege (sonst verdraengt die Tabelle andere Whiteboard-Sektionen)
