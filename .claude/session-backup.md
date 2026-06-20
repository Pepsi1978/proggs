# Session Handoff — 2026-06-20

## Ziel
Token-sparende Recherche-Pipeline ins System einbauen: MiniMax M3 (+ Websuche) macht die teure
Quellenarbeit, Opus zahlt nur die kompakte Synthese. PLANUNG ist abgeschlossen + committet — als
Naechstes folgt die UMSETZUNG nach `~/proggs/RESEARCH-PIPELINE-PLAN.md` (Punkte 1-8).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen (alles committet bis #47004).
**ABER der Restore ist KEIN Leerlauf — er startet sofort die Umsetzung:**
- **Allererste Aktion der neuen Session:** `~/proggs/RESEARCH-PIPELINE-PLAN.md` mit dem Read-Tool
  KOMPLETT lesen — das ist die vollstaendige, mit Frank abgestimmte Umsetzungs-Vorlage.
- **Dann Punkt 1-8 dieses Plans der Reihe nach abarbeiten.** Frank sagt beim Restore sinngemaess
  "weiter mit dem Plan" / "setze den Plan um".
- Start mit **Punkt 1** (Regel `research-strategy.md` um die A/B/C/D-AskUserQuestion + Frage 2 +
  "max Thinking bei A/B" + Opus-nur-explizit erweitern).

## Aktueller Status
- Erledigt (committet): Regel `~/.claude/rules/research-strategy.md` (+ Repo-Spiegelung);
  Skripte `~/proggs/mm-research.py` (Firecrawl->MiniMax, getestet) + `~/proggs/or-research.py`
  (OpenRouter web_search server-tool, getestet: MiniMax+parallel, 10 Suchen, 49 Quellen, $0.069,
  korrekter als Opus-Researcher); Doku `best-practices/opencode/go-recherche-modelle.md`,
  `bugs/opencode/opencode-cli.md` §14, `bugs/apis/firecrawl.md` + `best-practices/apis/firecrawl.md`;
  `RESEARCH-PIPELINE-PLAN.md` (#47004). Letzte Commits bis #47004 (Research-Strang).
- In Arbeit: nichts offen.
- Blockiert: nichts.

## Relevante Dateien
- `~/proggs/RESEARCH-PIPELINE-PLAN.md` — DIE Umsetzungs-Vorlage (Punkte 1-8). ZUERST lesen.
- `~/.claude/rules/research-strategy.md` (+ `claude-code-setup/rules/research-strategy.md`) — Regel, wird in Punkt 1 erweitert.
- `~/proggs/mm-research.py` — Firecrawl + MiniMax M3 (Stufe A). Punkt 3: thinking=adaptive.
- `~/proggs/or-research.py` — OpenRouter web_search server-tool (Stufe B). Punkt 3: Default engine=parallel, max_total_results=10.
- Umzubauende Skills (Punkt 4-6): `best-practices`, `bug-almanach-recherche`, `researcher`-Agent.

## Getroffene Entscheidungen (mit Frank, NICHT zurueckrudern)
- Frage 1 bei JEDER Recherche via AskUserQuestion: A) Firecrawl+MiniMax M3 (max Thinking) B) Eskalation MiniMax+parallel (max Thinking) C) Opus-Schwarm (nur explizit) D) Freitext.
- Frage 2 NACH einer Firecrawl-Research (immer): "zusaetzliche Eskalations-Research?" (Ja parallel / Nein / mit Opus / Freitext).
- "max Thinking" bei MiniMax M3 = `thinking:{type:"adaptive"}` (M3s Maximum; budget_tokens wird IGNORIERT).
- Opus = gleichwertige 3. Option, aber NUR auf explizite Opus-Ansage.
- Multi-Themen (z.B. Bug-Almanach 7 Aspekte): Skill ruft die Skripte MEHRFACH; Firecrawl max 2 parallel (dann naechste 2), or-research hoehere Parallelitaet.
- Durchsetzung: Regel + PreToolUse-Hook (blockt mm/or-research + Firecrawl-MCP bis Freigabe-Flag; via hook-forge bauen).
- Research-Skill = der `researcher`-Agent (wird mit umgebaut).

## Fehlgeschlagene Ansaetze / Fallen (NICHT wiederholen)
- OpenRouter `:online`/`plugins:[{id:web}]` ist DEPRECATED → neues Server-Tool `tools:[{"type":"openrouter:web_search","parameters":{...}}]`.
- MiniMax `/zen/go/v1/messages` (Anthropic-Schema) braucht `x-api-key`, NICHT `Authorization: Bearer` (gibt "Missing API key").
- Python urllib → Cloudflare 403/"error code 1010" → User-Agent `curl/8.5.0` setzen.
- Windows: kein `curl -o /tmp/...` (Pfad-Falle) → stdin-Pipe + Bash-Redirect; Python-Pfade via `os.path.expanduser`, nie `/c/Users/...`.
- MiniMax `budget_tokens` wird ignoriert → `adaptive` nutzen.
- Perplexity raus (nur 200k Kontext) — fuer grosse Recherchen 1M-Modell.

## Wichtige Recherche-Ergebnisse (verifiziert 2026-06-20)
- OpenRouter web_search Kosten: $0.005 pro Such-Anfrage (Exa/Parallel/Perplexity, bis 10 Treffer; >10 +$0.001/Treffer, max 25) + Modell-Token. "Treffer"=Snippet (~2-4k Zeichen), keine ganze Seite. Agentische Mehrfachsuche multipliziert → `max_total_results` deckeln.
- "parallel" = Such-Engine parallel.ai (NICHT parallele Ausfuehrung).
- Firecrawl Free: 1000 Credits/Mon, 2 concurrent, 5 Suchen/min.
- 3-Wege-Token-Test: MiniMax+Web ~100x weniger Opus-Token als Opus-Researcher, gleiche/bessere Qualitaet.

## Naechste Schritte (priorisiert)
1. `~/proggs/RESEARCH-PIPELINE-PLAN.md` komplett lesen.
2. Punkt 1: Regel `research-strategy.md` um Frage-1 (A/B/C/D AskUserQuestion) + Frage-2 + max-Thinking-bei-A/B + Opus-nur-explizit erweitern (+ Spiegelung).
3. Punkt 2: PreToolUse-Hook via `hook-forge` (.ps1+.sh) bauen.
4. Punkt 3: Skripte auf thinking=adaptive; or-research Default engine=parallel, max_total_results=10.
5. Punkt 4-6: Skills best-practices, bug-almanach-recherche, researcher-Agent umbauen.
6. Punkt 7-8: Cross-Platform-Spiegelung + Almanach-Konsolidierung.

## Offene Fragen
- Beim Hook (Punkt 2): genaues Freigabe-Flag-Schema (`$TEMP/research-approved-<ts>.flag`, TTL) beim Bau festlegen.
- Sonst keine — Plan ist mit Frank vollstaendig abgestimmt.

## Anker
- Branch: main
- Letzte Commits:
a78ee7fc3 #47004 - docs(research): RESEARCH-PIPELINE-PLAN.md - Umsetzungsplan als Restore-Vorlage
ba9c20344 #47006 - session restore: clear handoff backup (Phase B EntropieReductor)
2415ae9e5 #47005 - session backup: Phase-B-Direktstart nach Restore
e2250bf9e #47004 - session backup: EntropieReductor updatedAt Phase B handoff
39c2a3a51 #46998 - feat(EntropieReductor): DB-Migration 34->35 ideas.updatedAt; 0.17.21
