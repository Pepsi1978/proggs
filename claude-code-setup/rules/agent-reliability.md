# Agent-Zuverlaessigkeit (KRITISCH)

## Problem: Stille Agent-Abstuerze

Agenten koennen intern abstuerzen oder haengen bleiben, ohne dass eine Fehlermeldung erscheint.
Der Benutzer wartet dann stundenlang auf ein Ergebnis, das nie kommt.
**DAS DARF NIEMALS PASSIEREN.**

## Pflicht-Regeln fuer JEDEN Agent-Aufruf

### 1. Timeout-Erwartung setzen
Vor JEDEM Agent-Aufruf dem Benutzer mitteilen, wie lange es ungefaehr dauern sollte:
- Einfacher Agent (Recherche, kleiner Review): ~30-90 Sekunden
- Mittlerer Agent (Code-Aenderungen, Tests): ~2-5 Minuten
- Komplexer Agent (Architektur, Deep-Dive): ~5-10 Minuten
- **NIEMALS laenger als 15 Minuten** fuer einen einzelnen Agenten erwarten

### 2. Status-Updates bei langen Aufgaben
- Bei Aufgaben die voraussichtlich >3 Minuten dauern: Dem Benutzer regelmaessig Status geben
- Wenn ein Agent im Vordergrund laeuft und keine Fortschrittsmeldung kommt: NICHT endlos warten
- Nach spaetestens 10 Minuten ohne Output: Annehmen dass der Agent haengt

### 3. Bei Agent-Fehler: SOFORT reagieren
Wenn ein Agent einen Fehler zurueckgibt oder gar kein Ergebnis liefert:
- **SOFORT** dem Benutzer auf Deutsch erklaeren was passiert ist
- Den Fehler zeigen (nicht verstecken!)
- Einen neuen Versuch starten oder eine Alternative vorschlagen
- NIEMALS still weitermachen als waere nichts passiert

### 4. Parallele Agents absichern
Wenn mehrere Agents parallel laufen:
- Dem Benutzer zeigen, welche Agents gestartet wurden
- Wenn ein Agent fehlschlaegt: Die anderen NICHT abbrechen, aber den Fehler sofort melden
- Am Ende: Zusammenfassung welche Agents erfolgreich waren und welche nicht

### 5. Fehlgeschlagene Agents dokumentieren
Jeden Agent-Crash in `.claude/agent-memory/shared/MEMORY.md` dokumentieren:
- Welcher Agent
- Was er tun sollte
- Was schiefging
- Ob der Neuversuch erfolgreich war

## Beispiel: Korrektes Fehler-Handling

```
"Ich starte 3 Agents parallel..."
[Agent 1 laeuft... Ergebnis erhalten ✅]
[Agent 2 laeuft... FEHLER: Agent hat nach 5 Minuten kein Ergebnis geliefert]
→ "Agent 2 (code-reviewer) hat nicht reagiert. Ich starte ihn neu mit kleinerem Scope."
[Agent 2 (Neuversuch)... Ergebnis erhalten ✅]
[Agent 3 laeuft... Ergebnis erhalten ✅]
→ "Alle 3 Agents haben jetzt Ergebnisse geliefert."
```

## Was NIEMALS passieren darf
- ❌ Agent laeuft und der Benutzer sieht nichts — keine Updates, keine Fehlermeldung
- ❌ Agent schlaegt fehl und Claude macht einfach weiter ohne es zu erwaehnen
- ❌ Benutzer wartet mehr als 15 Minuten auf einen einzelnen Agenten
- ❌ Agent-Fehler wird in einer technischen Meldung versteckt die der Benutzer nicht versteht
