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

> **Research-Persistenz (NEU 2026-06-11):** Nach JEDEM Researcher-Einsatz pruefen, ob die
> Ergebnisse Best-Practices-tauglich sind → dann IMMER in `best-practices/` (+ Bugs in
> `bugs/`) einarbeiten, Kurzcheck UND Volltext. Vollstaendige Regel:
> `~/.claude/rules/research-persistence.md`. Recherchen duerfen nicht verkommen.

> Vorfall 2026-03-28: 5 Researcher mit je 100 Fragen → alle abgestuerzt.
> 5 Researcher mit je 50 Fragen → alle erfolgreich.

> **Update 2026-06-02 (Frank-Korrektur):** Mit Opus 4.8 / 1M-Kontext ist der *Kontext*-Crash kein
> Thema mehr — das alte "max 50 Ergebnisse"-Cap war Kontext-bedingt und entfaellt. Researcher sollen
> GROSSZUEGIG arbeiten und ALLE Findings dokumentieren (kappen waere lossy, siehe
> `lossless-context-principle.md`). Was BLEIBT, ist der ANFRAGE-RATEN-Schutz (RPM/429) — der ist
> unabhaengig vom Kontextfenster.

### Pflicht-Limits fuer JEDEN Researcher

| Limit | Wert | Warum |
|-------|------|-------|
| Max Ergebnisse/Findings | **KEIN Cap** | 1M-Kontext; alle Funde dokumentieren. Bei sehr vielen lossless in Datei auslagern + Summary zurueckgeben |
| Max Laufzeit | **10 Minuten** | Danach: Agent haengt wahrscheinlich |
| Max Web-Fetches | **~15** pro Researcher | Begrenzt die ANFRAGE-Rate (RPM), nicht die Findings-Zahl |
| Max Prompt-Laenge | **2000 Woerter** | Kurz und praezise, nur Kernfrage |
| Gleichzeitige Researcher | **5-7** (Continuous-Spawning) | RPM-Limit: 5 sicher, 7 ok (empirisch), ~12 → Abstuerze |

### Continuous-Spawning statt Wellen — OBERSTE Researcher-Regel (ALLE Engines)

**Sobald EIN Researcher fertig ist, wird SOFORT der naechste gestartet — NIE auf eine ganze Welle
warten.** Kein Zeitverlust durch "warten bis alle N fertig sind". Das ist die haeufigste und
teuerste Schwarm-Suende. Gilt fuer JEDE Engine (nicht nur Opus):

```
Start: so viele gleichzeitig wie die Engine erlaubt
→ Wird EINER fertig: SOFORT den naechsten starten (konstant die Engine-Zahl laufen lassen)
→ NICHT in Wellen warten bis alle fertig sind
→ Haelt Parallelitaet hoch UND RPM-Strom gleichmaessig (kein Burst)
→ Reicht der Scope nicht: mehr Researcher mit feineren Unterthemen (Duplikate kosten nichts)
```

| Engine | Konstant gleichzeitig | Quelle |
|--------|----------------------|--------|
| Sonnet-5-Schwarm (Research-Eskalation C, `model:"sonnet"`) | **7** (5 sicher, 7 ok empirisch, ~12 → Abstuerze) | diese Regel |
| OpenRouter (`or-research.py`, Such-Engine `parallel`) | **2** (empirisch 2026-06-21: mehr → kaputte Laeufe durch Last/Provider-Routing) | `research-strategy.md` §3a |
| Firecrawl (`mm-research.py`) | **2** (hartes Free-Limit) | `research-strategy.md` §3 |

Beispiel: laufen 7 und einer kommt zurueck → nur noch 6 → sofort einen neuen starten (wieder 7).
Ausfuehrung uebernimmt der zentrale `research`-Skill; diese Regel ist die systemweite Verankerung.

### Fehler-Praevention

| Problem | Praevention |
|---------|-------------|
| Agent haengt | Timeout nach 10 Min, Benutzer informieren |
| RPM / 429 (Server-Burst) | Max ~7 gleichzeitig; Retry mit exponential backoff (`retry-after`) |
| Netzwerk-Fehler | Graceful Degradation — was da ist zurueckgeben |
| Sehr viele Findings | NICHT kappen — lossless in Datei auslagern + kompakte Summary |

### Was NIEMALS passieren darf

- ❌ Researcher laeuft >10 Min ohne Ergebnis
- ❌ Echte Findings an einem kuenstlichen Cap abschneiden (alle dokumentieren — bei Menge lossless auslagern)
- ❌ Mehr als ~7 Researcher gleichzeitig (RPM-Absturz) — stattdessen Continuous-Spawning
- ❌ Researcher crasht und der Benutzer erfaehrt es nicht
- ❌ Riesige Rohdaten ungefiltert zurueckgegeben

---

## 3. Batch-Edits: Python statt parallele Coder-Agents

**Regel:** Gleiche Aenderung an 3+ Dateien → IMMER Python-Batch-Script. NIEMALS parallele Coder-Agents.

> Vorfall 2026-03-28: 5 parallele Coder-Agents fuer mehrere gleichartige Skript-Dateien.
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
