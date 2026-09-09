# Recherche-Strategie: Token-sparend recherchieren (KRITISCH)

> Fuer JEDE Web-Recherche + ALLE Researcher/Skills. **Volltext (Engines, Eskalation, Delegation):
> `claude-code-setup/docs/rules/research-strategy.md`.**

## Pflicht-Frage VOR jeder Web-Recherche (NIE ueberspringen)
Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbar) gefragt werden, WIE recherchiert
wird — nie automatisch. Drei sichtbare Schritte:
1. **Empfehlung** (welcher Weg, 1 Satz).
2. **Frage 1**: **A** Firecrawl-Quellen + DeepSeek V4 Flash @ DeepInfra (`mm-research.py`, Firecrawl-Free,
   2 parallel) · **B** dasselbe Modell mit `:online` (`or-research.py`, pay-per-use, 7 parallel) ·
   **C** Schwarm auf dem Host-Modell (Claude Code: Sonnet-5-Schwarm mit `model:"sonnet"`, teuer;
   OpenCode: aktuelles Session-Modell, kein `model:`-Override) · **D** Freitext.
3. Der **`research`-Skill** wird gestartet (sichtbare Researcher, Continuous-Spawning).
4. **Frage 2 (Eskalation)** automatisch NACH JEDER Firecrawl-Recherche (A).

Ausnahme: eine einzelne billige `WebSearch` zur Faktenpruefung mitten in einer Aufgabe bleibt frei.

## Auswerte-Modell (seit 09.09.2026)
A und B nutzen **dasselbe** Modell: `deepseek/deepseek-v4-flash-0731`, Anbieter **DeepInfra** gepinnt,
`reasoning effort: high`. Unterschied nur in Quellen (A = Firecrawl-Vollseiten, B = `:online`-Websuche)
und Parallelitaet (A = 2, B = 7). Pin steht als Default in den Skripten (`MM_PROVIDER`/`OR_PROVIDER`).

## Continuous-Spawning (OBERSTE Regel)
Einer fertig → SOFORT der naechste, nie auf eine Welle warten. Konstant: Firecrawl (A) **2** · `:online`
(B) **7** · Host-Modell-Schwarm (C) **7** (A/B via `research-swarm.py`). DeepSeek macht die token-schwere
Quellenarbeit (Rohdaten NIE in den Hauptagent-Kontext); der Hauptagent zahlt nur die ~2k-Synthese + arbeitet
sie ein.

## Engine C haengt vom Harness ab
`CLAUDECODE=1` gesetzt → **Claude Code** → Sonnet-5-Schwarm, `model:"sonnet"` PFLICHT pro Aufruf.
Nicht gesetzt (AGENTS.md geladen) → **OpenCode** → bis 7 Subagenten auf dem AKTUELLEN Session-Modell,
**kein** `model:`-Override; diese Modelle recherchieren mit ihrer eigenen Internet-Anbindung selbst.

## Was NIEMALS
- Crawl-Recherche OHNE das A/B/C/D-Protokoll · >2 Firecrawl parallel · auf eine Welle warten · Rohdaten
  ungefiltert in den Hauptagent-Kontext · Frage 2 nach Firecrawl weglassen.
