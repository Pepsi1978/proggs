# Agent- & Researcher-Zuverlaessigkeit + Pre-Flight-Plan (KRITISCH)

## 1. Agent-Zuverlaessigkeit: Stille Abstuerze verhindern

- **Timeout-Erwartung setzen** — vor JEDEM Agent-Aufruf dem Benutzer sagen, wie lange es dauert:
  einfach ~30-90 s, mittel ~2-5 Min, komplex ~5-10 Min. NIEMALS >15 Min fuer einen Agenten.
- **Bei Agent-Fehler SOFORT reagieren** — auf Deutsch erklaeren, den Fehler ZEIGEN (nicht verstecken),
  Neuversuch/Alternative. NIE still weitermachen.
- **Parallele Agents absichern** — zeigen welche gestartet wurden; faellt einer aus, die anderen NICHT
  abbrechen, Fehler sofort melden; am Ende Zusammenfassung (welche erfolgreich/nicht).
- Jeden Agent-Crash in `.claude/agent-memory/shared/MEMORY.md` dokumentieren.

## 2. Researcher-Limits: Absturzsicher durch Design

Opus 4.8 / 1M-Kontext: der Kontext-Crash ist kein Thema mehr, das alte "max 50 Ergebnisse"-Cap
entfaellt — Researcher arbeiten GROSSZUEGIG und dokumentieren ALLE Findings (bei Menge lossless in Datei
+ Summary). Was BLEIBT: der ANFRAGE-RATEN-Schutz (RPM/429).

| Limit | Wert |
|-------|------|
| Max Findings | KEIN Cap (bei Menge lossless auslagern) |
| Max Laufzeit | 10 Min (danach haengt der Agent wahrscheinlich) |
| Max Web-Fetches | ~15 pro Researcher (begrenzt RPM, nicht Findings) |
| Max Prompt | 2000 Woerter (nur Kernfrage) |
| Gleichzeitige Researcher | 5-7 (~12 → Abstuerze) |

**Continuous-Spawning (OBERSTE Researcher-Regel, ALLE Engines):** sobald EIN Researcher fertig ist,
SOFORT den naechsten starten — nie auf eine ganze Welle warten. Konstant gleichzeitig: Sonnet-5-Schwarm
(Engine C, `model:"sonnet"`) 7 · OpenRouter (`or-research.py … :online`) 7 · Firecrawl (`mm-research.py`) 2.

## 3. Batch-Edits: Python statt parallele Coder-Agents

**Gleiche Aenderung an 3+ Dateien → IMMER Python-Batch-Script, NIEMALS parallele Coder-Agents.**
(Vorfall: 5 parallele Coder → 4 vergassen Eintraege, 2 fehlerhaft; Python-Batch danach: 0 Fehler.)

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
mit pro Datei unterschiedlichem Code. **`sed` auf Windows meiden** (kann `\u`-Escapes nicht) → Python
`str.replace()`/`re.sub()`. Vor Annotation-/Modifier-Einfuegung VOR eine Signatur: 5 Zeilen darueber
pruefen (existiert es schon → False Positive).

## 4. Pre-Flight-Plan: grosse Aenderungen vorher ankuendigen

Gilt fuer ALLE Agenten UND den Haupt-Claude. Wer plant, **3 oder mehr Dateien** zu aendern (oder neue
zu erstellen), MUSS VORHER einen kurzen Plan praesentieren (Benutzer soll wissen, was passiert, BEVOR es
passiert):
```
Geplante Aenderungen (N Dateien):
1. [Dateiname] — [Was + warum, 1-2 Saetze]  …
Risiko: [Niedrig/Mittel/Hoch] — [1 Satz warum]
```
Pflicht bei: 3+ Dateien gleichzeitig, neue Dateien. NICHT noetig bei: 1-2 Dateien, reiner Config
(1 Datei), Build/Test/Commit. Der Plan ist TEXT (keine Permission-Abfrage) — bei bypassPermissions
faehrt der Agent fort, der Benutzer KANN aber "Stopp"/"Datei X nicht" sagen. Subagenten schreiben den
Plan als ERSTEN Block ihrer Antwort. NIEMALS: 5+ Dateien ohne Plan · Plan der nur "aendere N Dateien"
sagt (ohne WAS/WARUM) · Plan nachtraeglich zeigen.
