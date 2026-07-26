# Agent- & Researcher-Zuverlaessigkeit + Pre-Flight-Plan (KRITISCH)

## 1. Agent-Zuverlaessigkeit
- **Timeout** vor JEDEM Agent-Aufruf ansagen: einfach ~30-90 s, mittel ~2-5 Min, komplex ~5-10 Min, NIE >15 Min.
- **Bei Fehler SOFORT** auf Deutsch erklaeren + Fehler ZEIGEN + Neuversuch — NIE still weiter.
- **Parallele Agents:** zeigen welche starten; faellt einer aus, andere nicht abbrechen, am Ende Zusammenfassung.

## 2. Researcher-Limits (Opus 4.8 / 1M)
"max 50"-Cap entfaellt — GROSSZUEGIG, ALLE Findings dokumentieren (bei Menge lossless auslagern). Max 10
Min · ~15 Web-Fetches · Prompt ≤2000 Woerter · gleichzeitig 5-7. **Continuous-Spawning:** einer fertig →
SOFORT der naechste, nie auf eine Welle warten. Konstant: Engine C (Sonnet-5) 7 · OpenRouter `:online` 7 · Firecrawl 2.

## 3. Batch-Edits: Python statt parallele Coder
**Gleiche Aenderung an 3+ Dateien → IMMER Python-Batch** (`re.sub`, `encoding='utf-8'`, `newline='\n'`),
NIEMALS parallele Coder (Vorfall: 5 Coder → 4 vergassen Eintraege). Coder OK bei datei-spezifischen
Refactorings. `sed` auf Windows meiden → Python.

## 4. Pre-Flight-Plan
Wer **3+ Dateien** aendert (oder neue erstellt), MUSS VORHER kurz zeigen (je Datei Was/warum + Risiko).
NICHT bei 1-2 Dateien/Config/Build. Plan ist TEXT — bei bypassPermissions faehrt der Agent fort, Benutzer
kann "Stopp" sagen. NIEMALS: 5+ Dateien ohne Plan · Plan nachtraeglich zeigen.
