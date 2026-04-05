# Agent- und Researcher-Zuverlaessigkeit (KRITISCH)

> Konsolidiert aus: agent-reliability, researcher-robustness,
> batch-edits-python-not-agents

---

## 1. Agent-Zuverlaessigkeit: Stille Abstuerze verhindern

### Timeout-Erwartung setzen

Vor JEDEM Agent-Aufruf dem Benutzer mitteilen, wie lange es dauern sollte:
- Einfacher Agent (Recherche, kleiner Review): ~30-90 Sekunden
- Mittlerer Agent (Code-Aenderungen, Tests): ~2-5 Minuten
- Komplexer Agent (Architektur, Deep-Dive): ~5-10 Minuten
- **NIEMALS laenger als 15 Minuten** fuer einen einzelnen Agenten

### Bei Agent-Fehler: SOFORT reagieren

- **SOFORT** dem Benutzer auf Deutsch erklaeren was passiert ist
- Den Fehler zeigen (nicht verstecken!)
- Neuen Versuch starten oder Alternative vorschlagen
- NIEMALS still weitermachen als waere nichts passiert

### Parallele Agents absichern

- Dem Benutzer zeigen, welche Agents gestartet wurden
- Wenn ein Agent fehlschlaegt: Die anderen NICHT abbrechen, aber Fehler sofort melden
- Am Ende: Zusammenfassung welche Agents erfolgreich waren und welche nicht

### Fehlgeschlagene Agents dokumentieren

Jeden Agent-Crash in `.claude/agent-memory/shared/MEMORY.md` dokumentieren:
Agent, Aufgabe, Fehler, Neuversuch-Ergebnis.

---

## 2. Researcher-Limits: Absturzsicher durch Design

> Vorfall 2026-03-28: 5 Researcher mit je 100 Fragen → alle abgestuerzt.
> 5 Researcher mit je 50 Fragen → alle erfolgreich.

### Pflicht-Limits fuer JEDEN Researcher

| Limit | Wert | Warum |
|-------|------|-------|
| Max Ergebnisse | **50** pro Researcher | Kontext wird zu gross bei >50 |
| Max Laufzeit | **10 Minuten** | Danach: Agent haengt wahrscheinlich |
| Max Web-Fetches | **15** pro Researcher | >15 → Kontext-Ueberlauf |
| Max Prompt-Laenge | **2000 Woerter** | Kurz und praezise, nur Kernfrage |

### Grosser Researcher (>50 Ergebnisse): Aufteilung PFLICHT

```
Scope: 51+ Ergebnisse
→ N Researcher parallel, je max 50
→ Jeder bekommt eigenen Teilbereich
→ Ergebnisse am Ende zusammenfuehren
```

### Fehler-Praevention

| Problem | Praevention |
|---------|-------------|
| Agent haengt | Timeout nach 10 Min, Benutzer informieren |
| Kontext zu gross | Max 50 Ergebnisse, max 15 Fetches |
| Netzwerk-Fehler | Graceful Degradation — was da ist zurueckgeben |
| API-Rate-Limit | Retry mit Pause, max 3 Versuche |

### Was NIEMALS passieren darf

- ❌ Researcher laeuft >10 Min ohne Ergebnis
- ❌ Researcher sammelt >50 Ergebnisse in einem Durchlauf
- ❌ Researcher crasht und der Benutzer erfaehrt es nicht
- ❌ Riesige Rohdaten ungefiltert zurueckgegeben

---

## 3. Batch-Edits: Python statt parallele Coder-Agents

**Regel:** Gleiche Aenderung an 3+ Dateien → IMMER Python-Batch-Script. NIEMALS parallele Coder-Agents.

> Vorfall 2026-03-28: 5 parallele Coder-Agents fuer Tampermonkey-Skripte.
> 4 von 5 vergassen UI_IDS-Eintraege, 2 fuegten fehlerhaften Code ein.
> Python-Batch danach: 0 Fehler in allen 10 Dateien.

### Wann Python-Batch verwenden

- Gleiche Funktion/Variable in N Dateien hinzufuegen
- Gleichen Text in N Dateien ersetzen
- Gleichen Code-Block in N Dateien einfuegen
- Version-Bumps ueber mehrere Dateien

### Pattern

```python
import glob, re

for f in sorted(glob.glob('PFAD/*.EXTENSION')):
    with open(f, 'r', encoding='utf-8') as fh:
        content = fh.read()
    original = content
    content = re.sub(r'PATTERN', 'REPLACEMENT', content)
    if content != original:
        with open(f, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(content)
        print(f'Fixed: {f}')
```

### Wann Coder-Agents OK sind

- Einmalige, datei-spezifische Aenderungen (1 Datei, eigene Logik)
- Komplexe Refactorings die Verstaendnis der Dateistruktur brauchen
- Neue Features die pro Datei unterschiedlichen Code erfordern

### Zusatzregel: sed auf Windows vermeiden

`sed` auf Windows Git Bash kann `\u`-Escape-Sequenzen nicht korrekt verarbeiten.
IMMER Python `str.replace()` oder `re.sub()` verwenden.
