# Große Dateien: `rg` + gezielte Edits statt Agents (KRITISCH)

## Regel

Bei Dateien mit mehr als 500 Zeilen NIEMALS Agents spawnen.
IMMER `rg`/gezielte Suche + gezieltes Lesen + `apply_patch` oder strukturierte Batch-Edits verwenden.

## Die richtige Methode (unter 1 Minute)

1. `rg -n` mit gezieltem Pattern → findet alle Zeilennummern
2. gezielter Dateiread um die Fundstelle → liest nur die relevanten 5-15 Zeilen
3. `apply_patch` → aendert genau die gefundene Stelle
4. Wiederholen für jede weitere Stelle

## Die falsche Methode (50+ Minuten, Abstürze)

- Agent spawnen der die gesamte Datei liest
- Agent stürzt ab wegen zu viel Input
- Neuen Agent spawnen → liest wieder die gesamte Datei
- Wieder Absturz → nächster Agent → gleicher Fehler

## Warum (Vorfall 2026-04-08)

DashboardScreen.kt (~3000 Zeilen) musste an ~15 Stellen geändert werden.
- **5 Agents** gestartet → alle abgestürzt oder abgebrochen
- **200+ Tool-Aufrufe** durch die Agents
- **50 Minuten** verschwendet ohne Ergebnis
- Danach mit `rg` + `apply_patch`: **unter 1 Minute**, 0 Fehler

Faktor **50x schneller**, **100x weniger Tokens**.

## Wann Agents OK sind

- Dateien unter 500 Zeilen
- Reine Recherche mit Web-Werkzeugen ohne Datei-Edits
- Parallele Arbeit an VERSCHIEDENEN kleinen Dateien
- Neue kleine Dateien erstellen

## Wann Agents VERBOTEN sind

- ❌ Dateien über 500 Zeilen editieren
- ❌ Mehrere Agents auf die GLEICHE große Datei
- ❌ Agent der eine große Datei "analysieren und dann editieren" soll

## Bei vielen gleichartigen Änderungen

- Bis 20 Stellen: `rg` + `apply_patch` pro Stelle
- Über 20 Stellen: Python-Batch-Script (siehe batch-edits-python-not-agents.md)
