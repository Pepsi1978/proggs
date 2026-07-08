# Agent- & Researcher-Zuverlaessigkeit + Pre-Flight-Plan (KRITISCH)

## 1. Agent-Zuverlaessigkeit

- **Timeout-Erwartung** — vor JEDEM Agent-Aufruf sagen wie lange: einfach ~30-90 s, mittel ~2-5 Min,
  komplex ~5-10 Min. NIEMALS >15 Min.
- **Bei Agent-Fehler SOFORT** auf Deutsch erklaeren, Fehler ZEIGEN, Neuversuch/Alternative — NIE still weiter.
- **Parallele Agents absichern** — zeigen welche starten; faellt einer aus, andere NICHT abbrechen,
  Fehler melden; am Ende Zusammenfassung. Crashes in `MEMORY.md` dokumentieren.

## 2. Researcher-Limits (Opus 4.8 / 1M)

Kontext-Crash kein Thema mehr, altes "max 50"-Cap entfaellt — GROSSZUEGIG arbeiten, ALLE Findings
dokumentieren (bei Menge lossless auslagern). BLEIBT: Anfrage-Raten-Schutz. Max Laufzeit 10 Min · ~15
Web-Fetches/Researcher · Prompt ≤2000 Woerter · gleichzeitig 5-7 (~12 → Abstuerze).
**Continuous-Spawning (OBERSTE Regel):** ein Researcher fertig → SOFORT den naechsten, nie auf eine Welle
warten. Konstant: Engine C (Sonnet-5-Schwarm, `model:"sonnet"`) 7 · OpenRouter `:online` 7 · Firecrawl 2.

## 3. Batch-Edits: Python statt parallele Coder

**Gleiche Aenderung an 3+ Dateien → IMMER Python-Batch** (`glob`+`re.sub`, `encoding='utf-8'`,
`newline='\n'`), NIEMALS parallele Coder-Agents. (Vorfall: 5 Coder → 4 vergassen Eintraege.) Coder OK bei:
einmaligen datei-spezifischen Aenderungen, komplexen Refactorings. `sed` auf Windows meiden (kann `\u` nicht) → Python.

## 4. Pre-Flight-Plan

Gilt fuer ALLE Agenten UND den Haupt-Claude. Wer **3+ Dateien** aendert (oder neue erstellt), MUSS VORHER
einen kurzen Plan zeigen: `Geplante Aenderungen (N Dateien): 1. [Datei] — [Was+warum] … Risiko: [N/M/H]`.
NICHT noetig bei 1-2 Dateien, reiner Config, Build/Test/Commit. Der Plan ist TEXT (keine Permission-Abfrage)
— bei bypassPermissions faehrt der Agent fort, der Benutzer KANN "Stopp" sagen. NIEMALS: 5+ Dateien ohne
Plan · Plan der nur "aendere N Dateien" sagt (ohne WAS/WARUM) · Plan nachtraeglich zeigen.
