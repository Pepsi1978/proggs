# Token-Effizienz & Kostenoptimierung Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Größte Kostentreiber | System-Prompt + Rules (jede Anfrage), Tool-/MCP-Schemas (jede Anfrage), wachsende Kontext-Historie, „to be safe"-Datei-Reads ganzer Ordner | §1 |
| 2 | Hebel A — MCP minimieren (größter) | OpenCode hat KEIN natives MCP-Lazy-Loading (Stand 1.17.11) → global aus (`"tools":{"server*":false}`), per Agent an. Aktuell die EINZIGE Gegenmaßnahme | §2, §8 |
| 3 | Hebel B — Context-Disziplin | gezielte `@datei`-Mentions statt ganze Ordner; task-scoped Prompts („nur diese Funktion ändern"); `read`/`grep`/`glob` für on-demand-Retrieval | §2 |
| 4 | Hebel C — schlanke AGENTS.md | kurz halten (nur Befehle/Architektur/Konventionen); große `~/.claude/CLAUDE.md` per `OPENCODE_DISABLE_CLAUDE_CODE_PROMPT=1` abschalten | §2 |
| 5 | Hebel D/E — günstige Modelle | `small_model` für Titel/Summary/Compaction; pro Subagent eigenes (billiges) `model`, sonst erbt es das teure Primary; `steps` deckelt Iterationen | §2 |
| 6 | Hebel F/G — Plan + Compaction | `default_agent:"plan"` (denkt ohne teure Edit-/Bash-Iterationen); `compaction.prune:true` (entfernt alte Tool-Outputs); neue Sessions statt Mega-Session | §2 |
| 7 | Skills sind schon lazy | nur Name + Kurzbeschreibung dauerhaft im Kontext, volle SKILL.md erst bei Match → kein Handlungsbedarf, aber `description` kurz halten | §8 |
| 8 | Prompt Caching (OpenRouter) | DeepSeek + Gemini Flash cachen AUTOMATISCH (Read 0,1× / 0,25×); Anthropic braucht `setCacheKey:true`; stabile, kurze AGENTS.md = höhere Cache-Hit-Rate | §3 |
| 9 | Provider Routing | `sort:"price"` (billigster) + `require_parameters:true` (nur Tool-fähige Provider, sonst bricht der Agent-Loop) + `data_collection:"deny"` — PRO MODELL unter `options.provider` | §4 |
| 10 | Modellstrategie | Haupt mittelpreisig + toolfähig (DeepSeek V3.2/V4 Flash, Gemini Flash); Premium (Opus/Sonnet) nur punktuell per `--model`; `:free` nicht für sensiblen Code | §5 |
| 11 | Kosten beobachten | `opencode stats` (`--days`/`--models`/`--project`); OpenRouter `usage`-Objekt + Activity-Dashboard; Budget-Hard-Limits setzen | §6 |
