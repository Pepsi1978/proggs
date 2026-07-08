# Recherche-Strategie: Token-sparend recherchieren (KRITISCH)

> Fuer JEDE Web-Recherche + ALLE Researcher/Skills. **Volltext (Engines, Eskalation, Delegation):
> `claude-code-setup/docs/rules/research-strategy.md`.**

## Pflicht-Frage VOR jeder Web-Recherche (NIE ueberspringen)
Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbar) gefragt werden, WIE recherchiert
wird — nie automatisch. Drei sichtbare Schritte:
1. **Empfehlung** (welcher Weg, 1 Satz).
2. **Frage 1**: **A** Firecrawl+MiniMax (`mm-research.py`, Free) · **B** OpenRouter `:online`
   (`or-research.py`, pay-per-use) · **C** Sonnet-5-Schwarm (`model:"sonnet"`, teuer) · **D** Freitext.
3. Der **`research`-Skill** wird gestartet (sichtbare Researcher, Continuous-Spawning).
4. **Frage 2 (Eskalation)** automatisch NACH JEDER Firecrawl-Recherche (A).

Ausnahme: eine einzelne billige `WebSearch` zur Faktenpruefung mitten in einer Aufgabe bleibt frei.

## Continuous-Spawning (OBERSTE Regel)
Einer fertig → SOFORT der naechste, nie auf eine Welle warten. Konstant: Firecrawl (A) **2** · OpenRouter
(B) **7** · Sonnet-5 (C) **7** (via `research-swarm.py`). MiniMax macht die token-schwere Quellenarbeit
(Rohdaten NIE in den Hauptagent-Kontext); der Hauptagent zahlt nur die ~2k-Synthese + arbeitet sie ein.

## Was NIEMALS
- Crawl-Recherche OHNE das A/B/C/D-Protokoll · >2 Firecrawl parallel · auf eine Welle warten · Rohdaten
  ungefiltert in den Hauptagent-Kontext · Frage 2 nach Firecrawl weglassen.
