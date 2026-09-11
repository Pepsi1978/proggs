# Recherche-Strategie: Token-sparend recherchieren (KRITISCH)

> Fuer JEDE Web-Recherche + ALLE Researcher/Skills. **Volltext (Engines, Eskalation, Delegation):
> `claude-code-setup/docs/rules/research-strategy.md`.**

> **Diese Regel ist NICHT in jedem Profil geladen** (`ClaudeCode/minimal` hat keinen `rules/`-Ordner).
> Alles Pflichtige steht darum auch im `research`-Skill selbst (Block 0.0/0.1) — der liegt in JEDEM
> Profil. Wer den Skill befolgt, befolgt automatisch diese Regel.

## Pflicht-Frage VOR jeder Web-Recherche (NIE ueberspringen)
Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbar) gefragt werden, WIE recherchiert
wird — nie automatisch. Drei sichtbare Schritte:
1. **Empfehlung** (welcher Weg, 1 Satz).
2. **Frage 1**: **A** Firecrawl-Quellen + DeepSeek V4 Flash @ Makora (`mm-research.py`, Firecrawl-Free,
   2 parallel) · **B** dasselbe Modell mit `:online` (`or-research.py`, pay-per-use, 7 parallel) ·
   **C** Schwarm auf dem Host-Modell (Claude Code: Sonnet-5-Schwarm mit `model:"sonnet"`, teuer;
   OpenCode: aktuelles Session-Modell, kein `model:`-Override) · **D** Freitext.
3. Der **`research`-Skill** wird gestartet (sichtbare Researcher, Continuous-Spawning).
4. **Frage 2 (Eskalation)** automatisch NACH JEDER Firecrawl-Recherche (A).

Ausnahme: eine einzelne billige `WebSearch` zur Faktenpruefung mitten in einer Aufgabe bleibt frei.

**Gilt in JEDEM Arbeitsmodus** — Frei, Schnell, Normal, Gruendlich. Der Schnellmodus ("ohne Nachfragen")
meint den Umsetzungsteil einer Programmieraufgabe; er hebt weder Frage 1 (Kosten-Freigabe) noch die
Persistenz auf. Im Schnellmodus beides KURZ halten, nie weglassen.

**Ohne `AskUserQuestion`** (OpenCode u.a.): die vier Optionen als nummerierte Klartext-Liste ausgeben
und den Zug BEENDEN. Nie selbst beantworten, nie die Freigabe-Flag ohne Franks Antwort setzen.

## Auswerte-Modell (seit 09.09.2026)
Engine A holt die Quellen bei Firecrawl; faellt Firecrawl aus oder liefert nichts Brauchbares, springt
**Tavily** automatisch ein (advanced, 20 Quellen, 3 Chunks/Quelle, Volltext; `MM_TAVILY=always` erzwingt
beide Wege). Key: `~/SK/Tavily/tavily-api-key.txt`.

A und B nutzen **dasselbe** Modell: `deepseek/deepseek-v4-flash-0731`, Anbieter **Makora** gepinnt,
`reasoning effort: high`. Unterschied nur in Quellen (A = Firecrawl-Vollseiten, B = `:online`-Websuche)
und Parallelitaet (A = 2, B = 7). Pin steht als Default in den Skripten (`MM_PROVIDER`/`OR_PROVIDER`).

## Continuous-Spawning (OBERSTE Regel)
Einer fertig → SOFORT der naechste, nie auf eine Welle warten. Konstant: Firecrawl (A) **2** · `:online`
(B) **7** · Host-Modell-Schwarm (C) **7** (A/B via `research-swarm.py`). DeepSeek macht die token-schwere
Quellenarbeit (Rohdaten NIE in den Hauptagent-Kontext); der Hauptagent zahlt nur die ~2k-Synthese + arbeitet
sie ein.

## Engine C haengt vom Harness UND vom Modell ab
`CLAUDECODE=1` **und** `ANTHROPIC_BASE_URL` leer/Anthropic → **Sonnet-5-Schwarm**, `model:"sonnet"`
PFLICHT pro Aufruf. In ALLEN anderen Faellen (OpenCode; oder Claude Code hinter
`ANTHROPIC_BASE_URL=https://openrouter.ai/api`, also auf einem Fremdmodell wie GPT) → bis 7 Subagenten
auf dem AKTUELLEN Session-Modell, **kein** `model:`-Override; der `sonnet`-Alias loest dort nicht auf.
Diese Modelle recherchieren mit ihrer eigenen Internet-Anbindung selbst.

## Persistenz ist Pflicht — in jedem Modus
Nach JEDER Recherche: taugliche Erkenntnisse nach `~/proggs/best-practices/<kat>/<bereich>.md`
(Kurzcheck UND Volltext, mit Stand-Datum, Versions-Anker, Quellen), Bugs/Fallen ZUSAETZLICH nach
`~/proggs/bugs/<kat>/<bereich>.md` (Symptom, Ursache, Versionen, Fix, Quelle). Nichts tauglich → in
EINEM Satz begruenden. Danach committen + pushen. Details: `research`-Skill, Schritt 8.

## Was NIEMALS
- Crawl-Recherche OHNE das A/B/C/D-Protokoll · >2 Firecrawl parallel · auf eine Welle warten · Rohdaten
  ungefiltert in den Hauptagent-Kontext · Frage 2 nach Firecrawl weglassen.
