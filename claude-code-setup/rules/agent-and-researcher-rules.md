# Agent- und Researcher-Zuverlaessigkeit (KRITISCH)

## 1. Agent-Zuverlaessigkeit: Stille Abstuerze verhindern

- **Timeout-Erwartung setzen** — vor JEDEM Agent-Aufruf dem Benutzer sagen, wie lange es dauert:
  einfach ~30-90 s, mittel ~2-5 Min, komplex ~5-10 Min. NIEMALS >15 Min fuer einen Agenten.
- **Bei Agent-Fehler SOFORT reagieren** — auf Deutsch erklaeren was passiert ist, den Fehler ZEIGEN
  (nicht verstecken), Neuversuch/Alternative. NIE still weitermachen.
- **Parallele Agents absichern** — zeigen welche gestartet wurden; faellt einer aus, die anderen NICHT
  abbrechen, Fehler sofort melden; am Ende Zusammenfassung (welche erfolgreich/nicht).
- Jeden Agent-Crash in `.claude/agent-memory/shared/MEMORY.md` dokumentieren (Agent, Aufgabe, Fehler, Neuversuch).

## 2. Researcher-Limits: Absturzsicher durch Design

> Opus 4.8 / 1M-Kontext: der Kontext-Crash ist kein Thema mehr, das alte "max 50 Ergebnisse"-Cap
> entfaellt. Researcher arbeiten GROSSZUEGIG und dokumentieren ALLE Findings (bei Menge lossless in
> Datei auslagern + Summary). Was BLEIBT: der ANFRAGE-RATEN-Schutz (RPM/429).

| Limit | Wert |
|-------|------|
| Max Findings | KEIN Cap (1M-Kontext; bei Menge lossless auslagern) |
| Max Laufzeit | 10 Min (danach haengt der Agent wahrscheinlich) |
| Max Web-Fetches | ~15 pro Researcher (begrenzt RPM, nicht Findings) |
| Max Prompt | 2000 Woerter (nur Kernfrage) |
| Gleichzeitige Researcher | 5-7 (Continuous-Spawning; ~12 → Abstuerze) |

## Continuous-Spawning statt Wellen — OBERSTE Researcher-Regel (ALLE Engines)

**Sobald EIN Researcher fertig ist, SOFORT den naechsten starten — NIE auf eine ganze Welle warten.**
Konstant die Engine-Zahl laufen lassen (kein Burst, kein Leerlauf). Reicht der Scope nicht: mehr
Researcher mit feineren Unterthemen (Duplikate kosten nichts).

| Engine | Konstant gleichzeitig |
|--------|----------------------|
| Sonnet-5-Schwarm (Research-Eskalation C, `model:"sonnet"`) | 7 (5 sicher, 7 ok) |
| OpenRouter (`or-research.py`, `:online`) | 7 |
| Firecrawl (`mm-research.py`) | 2 (hartes Free-Limit) |

## 3. Batch-Edits: Python statt parallele Coder-Agents

**Gleiche Aenderung an 3+ Dateien → IMMER Python-Batch-Script, NIEMALS parallele Coder-Agents.**
(Vorfall: 5 parallele Coder → 4 vergassen Eintraege, 2 fehlerhaft; Python-Batch danach: 0 Fehler in 10 Dateien.)

```python
import glob, re
for f in sorted(glob.glob('PFAD/*.EXT')):
    with open(f, 'r', encoding='utf-8') as fh: content = fh.read()
    original = content
    content = re.sub(r'PATTERN', 'REPLACEMENT', content)
    if content != original:
        with open(f, 'w', encoding='utf-8', newline='\n') as fh: fh.write(content)
```

Coder-Agents OK bei: einmaligen datei-spezifischen Aenderungen, komplexen Refactorings, neuen Features
mit pro Datei unterschiedlichem Code. **`sed` auf Windows vermeiden** (kann `\u`-Escapes nicht) → Python
`str.replace()`/`re.sub()`. Vor Annotation-/Modifier-Einfuegung VOR eine Signatur: 5 Zeilen darueber
pruefen (existiert es schon → False Positive, Edit entfaellt).
