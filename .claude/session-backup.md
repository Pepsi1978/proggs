# Session Handoff — 2026-06-21 (Nachmittag)

## Ziel
Das Research-System rund machen. ERREICHT: zentraler research-Skill gebaut, Engine-B
stabilisiert, erste echte Recherche (Vibe-Coding) erfolgreich durchgelaufen.

## Laufende/unterbrochene Aufgabe
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Alles committet+gepusht bis #47038.
Keine eigenen uncommitteten Aenderungen.

## Aktueller Status (alles committet+gepusht)
- **research-Skill gebaut + angebunden (#47026-47030):** Neuer zentraler Orchestrator
  ~/.claude/skills/research/ (+ Spiegel claude-code-setup/skills/ + Umgebung/Skills/). Kern:
  verlustfreie Bruecke = benanntes Research-Auftrags-Schema + Rueckgabe-Schema-Katalog
  (references/rueckgabe-schemata.md). research-strategy.md §5 auf Delegation, §3a Continuous-Spawning.
  researcher-Agent 50-Item-Cap raus (lossless). Uebergabe-Block in 9 Einheiten (5 Skills + 4 Agenten).
  Spec: docs/superpowers/specs/2026-06-21-research-skill-design.md.
- **Engine-B-Problem behoben (#47034-47036):** Root Cause war LAST/PARALLELITAET (leere Response/
  Timeout/Provider-Routing-Varianz), NICHT das Modell. MiniMax M3 rehabilitiert. Fix in or-research.py:
  Leer-/Leak-Detektor + Retry+Backoff + alle Exceptions fangen + OR_OUTDIR/Provider-Sonde.
  Engine B auf max 2 parallel begrenzt (empirisch stabil; 7 parallel = 3/7 kaputt, max 2 = sauber).
  Almanach bugs/apis/openrouter-api.md §41 + Kurzcheck korrigiert.
- **Vibe-Coding-Recherche (#47037-47038):** ERSTER echter Einsatz des research-Skills. 8 Researcher
  Stufe A (Firecrawl+MiniMax) + 3 Eskalation Engine B (OpenRouter max 2). Engine-B-Realtest BESTANDEN
  (3/3 sauber, Retry griff bei E3 automatisch). Ergebnis: best-practices/opencode/vibe-coding-modelle.md
  + vibe-coding-rohergebnisse-2026-06-21.md. Kimi-K2.7-Code-Praxis-Hinweis ergaenzt.

## Wichtigste Erkenntnisse (aus der Vibe-Coding-Recherche)
- Thinking-an ist der HEBEL, nicht Thinking-hoch: grosser Sprung kein/wenig -> mittel (+~14 Punkte);
  high/xhigh beim Code-Editing fast nichts (+58% Token, 0 Accuracy) und over-editing. Sweet Spot MEDIUM.
- GLM-5.2 UND Kimi haben Thinking — muss in opencode.json per reasoningEffort AKTIVIERT werden.
  Frank-Korrektur: er nutzte Kimi K2.7 Code (non-thinking) -> daher schlecht; bestaetigt den Hebel.
- Reasoning-Modelle over-editen -> im Prompt explizit "minimale Aenderung, bestehenden Code erhalten".
- Benchmarks unzuverlaessig fuer Modellwahl (Harness/Prompt dominiert, kontaminiert, manipulierbar).
- MiMo-V2.5-Pro (Xiaomi, NICHT MiniMax!) guenstig+top fuer Vibe-Coding; GLM-5.2 stark Long-Horizon.

## Getroffene Entscheidungen (NICHT zurueckrudern)
- research-Skill ist zentral; alle Research-Skills/Agenten delegieren via Uebergabe-Block.
- Engine B = max 2 parallel + Retry; MiniMax M3 bleibt (kein Modellwechsel noetig).
- researcher 50-Cap entfernt (beobachten ob bei OpenRouter doch noetig).
- Recherche-Policy bleibt: A=Firecrawl Standard, B=OpenRouter Eskalation (max 2), C=Opus nur explizit.
  Vor JEDER Web-Recherche Empfehlung + Frage 1 (AskUserQuestion), research-approval-Hook + Flag.

## Fehlgeschlagene Ansaetze / Lessons (NICHT wiederholen)
- 3x voreilige Diagnose beim Engine-B-Bug (erst Modell, dann Workspace-Engine) — erst Franks
  Methodik (eine Variable reproduzieren) fand die echte Ursache (Last/Parallelitaet). LEHRE: bei
  intermittenten Bugs ERST kontrolliert reproduzieren, DANN diagnostizieren.
- mm-research.py (Engine A) hat KEINEN Retry/Leer-Detektor und keine getrennte Ausgabedatei pro Lauf
  -> R5 kam abgeschnitten zurueck (neu gestartet). Engine A braucht denselben Schutz (offen, siehe unten).

## Naechste Schritte / Offene Folge-Punkte (priorisiert, alle OPTIONAL — Frank entscheidet)
1. supermemory-Korrektur in best-practices/second-brain/memory-backends.md: der Test zeigte
   supermemory IST quelloffen (MIT) + nativ MCP — die gestrige Aussage (andere Session) war falsch.
2. mm-research.py (Engine A) denselben Schutz wie or-research.py geben: MM_OUTDIR-Override +
   Retry + Abschneide-/Leer-Detektor.
3. GLM-5.2 / MiMo-V2.5-Pro in Franks opencode.json mit reasoningEffort high/medium aktivieren + testen.
4. Continuous-Spawning-Helfer-Skript (max N parallel, einer fertig -> naechster) statt manueller
   run_in_background-Orchestrierung.
5. Android-KI-Code-Review-Checkliste (Coroutine-Scope/!!/StateFlow/Recomposition) als kurze Regel.

## Anker
- Branch: main
- Letzte EIGENE Commits (Achtung: parallele Session hat gleiche Nummern fuer EntropieReductor):
c4fd90a00 #47038 - best-practices(opencode): Kimi K2.7 Code Praxis-Befund
8c2bcccfc #47037 - best-practices(opencode): Vibe-Coding-Modelle + Rohergebnisse
a90e2bda1 #47036 - research engine-B stabilisieren: max 2 parallel
e4fa5a128 #47035 - or-research.py: Retry+Backoff + Sonde
(davor: #47026-47034 research-Skill-Bau + Engine-B-Detektor)
