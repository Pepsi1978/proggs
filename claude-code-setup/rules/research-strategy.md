# Recherche-Strategie: Token-sparend recherchieren (KRITISCH)

> Gilt fuer JEDE Web-Recherche + ALLE Researcher/Skills. **Volltext (Engines, Eskalation, Delegation,
> Werkzeuge): `claude-code-setup/docs/rules/research-strategy.md`.**

## Pflicht-Frage VOR jeder Web-Recherche (NIE ueberspringen)

Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbar) gefragt werden, WIE recherchiert
wird — nie automatisch losrecherchieren. Drei sichtbare Schritte, immer in dieser Reihenfolge:
1. **Empfehlung** (welcher Weg, 1 Satz Begruendung).
2. **Frage 1** (anklickbar): **A** Firecrawl+MiniMax (`mm-research.py`, Free-Credits) · **B** Eskalation
   OpenRouter `:online` (`or-research.py`, pay-per-use, bis 7 parallel) · **C** Sonnet-5-Schwarm
   (`model:"sonnet"`, teuer, nur auf Wahl) · **D** Freitext. Kommt IMMER.
3. Der **`research`-Skill** wird gestartet (beschriftete, sichtbare Researcher, Continuous-Spawning).
4. **Frage 2 (Eskalation)** automatisch NACH JEDER abgeschlossenen Firecrawl-Recherche (Engine A).

Ausnahme: eine einzelne billige `WebSearch` zur Faktenpruefung mitten in einer Aufgabe bleibt frei.

## Continuous-Spawning (OBERSTE Regel, alle Engines)

Ein Researcher fertig → SOFORT den naechsten, nie auf eine Welle warten. Konstant gleichzeitig: Firecrawl
(A) **2** (hartes Free-Limit) · OpenRouter (B) **7** · Sonnet-5-Schwarm (C) **7**. Engine A/B laufen ueber
`research-swarm.py` (haelt konstant N parallel). A+B mit max Thinking. MiniMax macht die token-schwere
Quellenarbeit (Rohdaten NIE in den Hauptagent-Kontext); der Hauptagent zahlt nur die ~2k-Synthese und
arbeitet sie in Almanach/Best-Practices ein (`research-persistence.md`).

## Was NIEMALS
- Eine Crawl-Recherche starten OHNE das A/B/C/D-Protokoll · >2 Firecrawl parallel · auf eine Welle warten
  statt Continuous-Spawning · Rohdaten ungefiltert in den Hauptagent-Kontext laden · Frage 2 nach Firecrawl weglassen.
