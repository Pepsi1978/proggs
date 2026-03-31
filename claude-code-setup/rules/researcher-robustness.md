# Researcher-Agenten: Robust und absturzsicher (KRITISCH)

## Regel: Researcher duerfen NIEMALS abstuerzen oder haengenbleiben

Researcher-Agenten (Web-Recherche, Quiz-Generierung, Daten-Sammlung) muessen
absturzsicher sein. Ein haengender Researcher blockiert den Benutzer und verschwendet Tokens.

## Pflicht-Limits fuer JEDEN Researcher

### 1. Maximale Ergebnis-Anzahl: 50 pro Researcher
- Ein einzelner Researcher darf **maximal 50 Ergebnisse** (Fragen, Fakten, Eintraege) sammeln
- Bei mehr als 50: Mehrere Researcher parallel starten, jeder mit eigenem 50er-Block
- Beispiel: 200 Quiz-Fragen → 4 Researcher parallel mit je 50 Fragen
- **Warum**: Bei >50 Ergebnissen wird der Kontext zu gross und der Agent crasht intern
  (bewiesen bei QuizVerse am 2026-03-28: 5 Researcher mit je 100 Fragen → alle abgestuerzt,
  5 Researcher mit je 50 Fragen → alle erfolgreich)

### 2. Maximale Laufzeit: 10 Minuten
- Kein Researcher darf laenger als 10 Minuten laufen
- Wenn nach 10 Minuten kein Ergebnis: Agent ist wahrscheinlich abgestuerzt
- Dem Benutzer SOFORT melden und neuen Versuch mit kleinerem Scope starten

### 3. Maximale Web-Fetches: 15 pro Researcher
- Ein Researcher darf maximal 15 WebFetch/WebSearch-Aufrufe machen
- Mehr als 15 Fetches fuehrt zu Kontext-Ueberlauf
- Bei groesseren Recherchen: Aufteilen auf mehrere Researcher

### 4. Maximale Token-Menge im Prompt: Researcher-Prompt unter 2000 Woerter
- Der Prompt an den Researcher muss kurz und praezise sein
- Keine riesigen Kontexte mitgeben — nur das Noetigste
- Ergebnisse zusammenfassen, nicht roh zurueckgeben

## Pflicht-Muster fuer Researcher-Aufrufe

### Kleiner Researcher (Standard)
```
- Scope: 1-50 Ergebnisse
- WebFetches: max 15
- Timeout-Erwartung: 1-5 Minuten
- Modell: Sonnet (schnell, guenstig)
```

### Grosser Researcher (Aufteilung PFLICHT)
```
- Scope: 51+ Ergebnisse
- Aufteilung: N Researcher parallel, je max 50
- Jeder Researcher bekommt eigenen Teilbereich
- Ergebnisse am Ende zusammenfuehren
```

## Fehler-Praevention

| Problem | Praevention |
|---------|-------------|
| Agent haengt | Timeout nach 10 Min, Benutzer informieren |
| Kontext zu gross | Max 50 Ergebnisse, max 15 Fetches |
| Prompt zu lang | Unter 2000 Woerter, nur Kernfrage |
| Ergebnis zu gross | Zusammenfassen statt roh zurueckgeben |
| Netzwerk-Fehler | Graceful Degradation — was da ist zurueckgeben |
| API-Rate-Limit | Retry mit Pause, max 3 Versuche |

## Gilt fuer

- `researcher` Agent (Web-Lookup)
- `intelligence-researcher` Agent
- Alle Agenten die WebSearch/WebFetch nutzen
- Quiz-Generierung (QuizVerse)
- Jede Aufgabe die >50 Datenpunkte aus dem Internet sammelt

## Was NIEMALS passieren darf

- ❌ Researcher laeuft >10 Minuten ohne Ergebnis
- ❌ Researcher sammelt >50 Ergebnisse in einem Durchlauf
- ❌ Researcher macht >15 Web-Fetches
- ❌ Researcher crasht und der Benutzer erfaehrt es nicht
- ❌ Researcher-Prompt ist laenger als 2000 Woerter
- ❌ Riesige Rohdaten werden ungefiltert zurueckgegeben

## Warum diese Regel existiert

Am 2026-03-28 sind 5 Researcher-Agenten bei QuizVerse abgestuerzt weil jeder 100 Fragen
generieren sollte. Der Kontext wurde zu gross, die Agenten liefen ewig und lieferten nie
ein Ergebnis. Nach Reduzierung auf 50 Fragen pro Researcher funktionierten alle 5 sofort.
Diese Grenze von 50 ist empirisch bewiesen und muss systemweit gelten.
