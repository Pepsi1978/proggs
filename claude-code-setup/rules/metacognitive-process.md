# Meta-Kognition & Planung: Denken bevor Handeln (KRITISCH)

## 1. Tool-Planung vor komplexen Ketten

Bei >3 sequentiellen Tool-Aufrufen VORHER einen Plan (dem Benutzer sichtbar, 1-2 Zeilen):
```
Plan: [1-2 Saetze Ziel]
Schritte: [Tool 1] → [Tool 2] → [Tool 3] → ...
```
Ausnahme: rein parallele Tool-Aufrufe (ein Antwortblock) brauchen keinen Plan. Post-Check nach jedem
Tool: "Ergebnis wie erwartet?" — wenn nein, Plan anpassen bevor der naechste Schritt kommt.

## 2. Ensemble-Reasoning: 3 Loesungswege

Bei komplexen Aufgaben (Refactoring, tiefe Bugfixes, >3 Dateien): 3 Wege generieren (A minimaler Fix,
B architektur-konform, C Abstraktion/Refactoring) → gegen Syntax/Konventionen/Risiko pruefen → besten
waehlen + kurz begruenden. Pflicht wenn: >3 Dateien gleichzeitig, Debugger findet keine eindeutige
Root Cause, Architect entwirft neue Komponenten.

## 3. Intent-Tracking

Session-Ziel in `$TMPDIR`/`$env:TEMP`/`/tmp` `claude-session-goal.md`. Alle ~10 Tool-Calls mental
pruefen: "arbeite ich noch am urspruenglichen Ziel?" Nach jeder Teilaufgabe 1-Satz-Review. Bei Abdrift:
informieren, zuruecklenken.

## 4. Spec-First

Fuer nicht-triviale Features (mehr als einfacher Bugfix): vor dem Code eine Spec nach
`/tmp/current-spec.md` (Invarianten, Vor-/Nachbedingungen, Edge Cases). Tests kommen aus der Spec, nicht
aus dem Code. Ueberspringen bei: Bugfixes <10 Zeilen, Config/Settings, Doku, Version-Bumps.
Warum: Code der Tests besteht kann trotzdem das FALSCHE Problem loesen — Specs klaeren WAS vor WIE.
