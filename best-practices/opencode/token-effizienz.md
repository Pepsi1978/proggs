# Token-Effizienz & Kostenoptimierung — Best Practices (Stand 2026-06-18, erweitert 2026-06-26, OpenCode CLI)

> Ein **token-armer, gut funktionierender Harness** — besonders mit günstigen OpenRouter-Modellen.
> **Anker:** opencode=1.17.11 (Recherche 2026-06-26; Erst-Stand 1.17.8). Kern-Update 2026-06-26:
> Abschnitt 8 (MCP-Lazy-Loading-Stand + manuelle Methode als EINZIGER Hebel — OpenCode hat anders
> als Claude Code KEIN natives MCP-Tool-Lazy-Loading).
> Quellen: `offiziell` (opencode.ai/docs, openrouter.ai/docs) bzw. `extern`. Plattformneutral (Config ist
> JSON); einziger plattformrelevanter Punkt ist `shell` (`pwsh`/`cmd.exe` vs. `/bin/zsh`).

---

## 1. Wo die Token-Kosten entstehen (die Treiber)

Bei Coding-Workloads dominieren meist die **Prompt-Tokens (Input)**. Treiber: `extern`(TrueFoundry)/`offiziell`
- **System-Prompt + Rules (AGENTS.md/CLAUDE.md):** gehen in **jede** Anfrage. OpenCode lädt AGENTS.md (Projekt),
  `~/.config/opencode/AGENTS.md` (global), Fallback `CLAUDE.md`/`~/.claude/CLAUDE.md` — alle kombiniert. Großes
  Regelwerk kostet bei jedem Turn erneut. `offiziell`
- **Tool-Definitionen — besonders MCP:** jedes aktivierte Tool wird als Schema in **jeder** Anfrage mitgeschickt.
  Offiziell: *„MCP servers add to the context… the GitHub MCP server tends to add a lot of tokens and can easily
  exceed the context limit."* Ein GitHub-MCP frisst ~20k Tokens. `offiziell`/`extern`
- **Kontext-Historie:** bei jedem Turn wird die gesamte bisherige Konversation erneut gesendet → „exponential
  growth" bei langen Tasks. `extern`
- **Datei-Reads:** Code ist token-dicht; ganze Verzeichnisse „to be safe" mitzuschicken ist der größte
  Einzeltreiber. `extern`
- **Multi-Step-/Subagent-Workflows:** jeder Schritt re-evaluiert ggf. großen Kontext. `extern`

OpenCode kompaktiert bei Überlauf automatisch (Safety-Buffer 20.000 Tokens). `extern`

---

## 2. Konkrete Spar-Hebel — priorisiert nach Effekt (größter zuerst)

### Hebel A — MCP-Server minimieren (oft der größte Einzelhebel)
- **Nur einschalten, was man braucht:** `"enabled": false` ohne Entfernen aus der Config. `offiziell`
- **Per-Agent-MCP statt global:** global aus (`"tools": {"mymcp*": false}`), nur im benötigenden Agent an
  (`"tools": {"mymcp*": true}`). Glob `mymcpservername_*` deaktiviert alle Tools eines Servers. `offiziell`
- ⚠️ **Das ist der EINZIGE Hebel:** OpenCode hat (Stand 1.17.11) **KEIN natives MCP-Lazy-Loading**
  (anders als Claude Code seit v2.1.7). Es injiziert ALLE MCP-Tool-Schemas in JEDEN Prompt — die
  manuelle Per-Agent-Auslagerung ist die einzige Gegenmaßnahme. Details + Belege: **Abschnitt 8**.

### Hebel B — Context-Disziplin: gezielte `@datei`-Mentions statt ganze Ordner
- **Datei-/Funktions-Ebene statt Verzeichnis-Ebene.** „if a file is not required to reason about the change,
  it should not be part of the prompt." `extern`
- **Task-scoped Prompts** („modify only this function") statt „analyze the codebase".
- Built-in `read`/`grep`/`glob` (ripgrep) für **on-demand-Retrieval** statt Context-Stuffing. `offiziell`

### Hebel C — Schlanke AGENTS.md statt Mega-Regelwerk
- **Kurz halten** (nur Build-/Test-Befehle, Architektur, Konventionen, Gotchas — genau das, was `/init` erzeugt).
- **Lazy-Loading externer Regeln** (Detail-Dateien nur bei Bedarf per `@`/Read laden). `offiziell`
- **Migration-Hinweis:** eine große `~/.claude/CLAUDE.md` wird bei JEDEM Turn mitbezahlt. Per
  `OPENCODE_DISABLE_CLAUDE_CODE_PROMPT=1` abschalten und schlanke `~/.config/opencode/AGENTS.md` nutzen. `offiziell`

### Hebel D — `small_model` für Titel/Nebenaufgaben
Explizit ein sehr günstiges `small_model` setzen → spart bei Titel-/Summary-/Compaction-Generierung
(versteckte System-Agenten title/summary/compaction). `offiziell`

### Hebel E — Günstige Subagent-Modelle pro Agent
Pro Agent ein eigenes `model`. Subagents erben sonst das (teure) Primary-Modell → Explore/Plan/Research auf
billiges Modell, Build auf das Hauptmodell. `offiziell`

### Hebel F — Plan-Modus zum Denken, Build sparsam
Plan ist restriktiv (edits/bash auf `ask`/`deny`) → analysieren/planen ohne teure Schreib-/Bash-Iterationen.
`"default_agent": "plan"`, mit Tab zu Build wechseln. `offiziell`

### Hebel G — Kontext schlank halten: kompaktieren + neue Sessions
- `compaction.auto` (Default true), `compaction.prune: true` (entfernt alte Tool-Outputs → spart Tokens),
  `compaction.reserved` (z.B. 10000). `offiziell`
- **Neue Sessions** für neue Aufgaben statt eine Mega-Session ewig wachsen lassen.
- Pruning nicht abschalten (`OPENCODE_DISABLE_PRUNE` existiert — Default-Verhalten beibehalten). `offiziell`

### Hebel H — Tools/Permissions beschränken
Ungenutzte Built-ins global aus (`"tools": {"write": false, "bash": false}`) oder via Permissions `deny`.
Weniger aktive Tools = kleinere Tool-Schema-Payload pro Anfrage. Read-only-Agenten `edit`/`bash`/`webfetch` denyen. `offiziell`

### Hebel I — Agentische Iterationen begrenzen (`steps`)
`"steps": N` setzt eine Obergrenze für agentische Iterationen (jede re-sendet Kontext). `offiziell`

### Hebel J — Snapshots/LSP/Formatter bei Bedarf aus
`"snapshot": false` für große Repos; `lsp`/`formatter` nur wo gebraucht (Overhead-/Index-Hebel). `offiziell`

### Hebel K — Reasoning/Verbosity drosseln
Reasoning-Tokens zählen als Output. OpenAI-Reasoning: `reasoningEffort: "low"` + `textVerbosity: "low"`;
Anthropic: `thinking.budgetTokens` klein; Google-Variants `low`. Über Agent-Additional-Options oder Variants.
Output deckeln via `OPENCODE_EXPERIMENTAL_OUTPUT_TOKEN_MAX`. `offiziell`

---

## 8. MCP-Lazy-Loading-Stand + Harness-Token-Befunde (Recherche 2026-06-26, opencode 1.17.11) `extern`/`offiziell`

> Firecrawl+MiniMax-Recherche zu MCP/Skills/Plugins/Agenten. Kern: **OpenCode hat KEIN natives
> MCP-Lazy-Loading** — die manuelle Per-Agent-Auslagerung (Hebel A) ist der einzige Hebel.

### 8.1 MCP — kein natives Lazy-Loading (der zentrale Befund)
- **Default-Verhalten:** OpenCode injiziert das komplette `input_schema` **aller** verbundenen
  MCP-Tools bei **jedem** Session-Start („Token Tax"). Gemessenes Extrem: ein einzelner MCP-Server
  (lark-mcp-docx) fraß **147k Token** (21k → 168k). `extern` (GitHub Issue #17482)
- **Lazy-Loading ist offen/ungelöst:** Feature-Requests **#8277** („Lazy/dynamic loading for mcp
  tools"), **#8625** („mcp search tool, reduce mcp tool context"), **#9350** („MCP Tool Search —
  Lazy Loading for 85% Token Reduction"), **#16206** („Two-Step MCP Tool Discovery"). #17482 wurde
  als Duplikat von #8625 geschlossen; Maintainer rekram1-node zugewiesen (März 2026), aber **keine
  Implementierung in 1.17.x belegt**. `extern`
- **Kontrast Claude Code:** hat seit **v2.1.7** einen „MCP Tool Search", der automatisch greift, wenn
  MCP-Tool-Descriptions >10 % des Kontexts verbrauchen (~85 % Tokenreduktion). OpenCode **nicht**. `extern`
- **Konsequenz / Best Practice:** Die manuelle Methode aus Hebel A (global `"tools":{"server*":false}`,
  pro Agent `true`) ist aktuell die EINZIGE wirksame Gegenmaßnahme — kein Warten auf ein natives Feature.
  Quelle/Beispiel: genau so für Firecrawl umgesetzt (`opencode.jsonc` Punkt 9 + `agents/researcher.md`).

### 8.2 Skills — bereits lazy/on-demand (gute Nachricht, kein Handlungsbedarf)
- Nur **Skill-Name + Kurzbeschreibung** stehen konstant in der nativen Skill-Tool-Description; die
  volle `SKILL.md` wird **erst bei Referenz/Match** geladen. `extern` (Composio; YouTube baGKgnbQUq8)
- **Minimierung = Disziplin, kein nativer Filter:** Es gibt **keinen** dokumentierten
  `skill disable`/Allow-Denylist-Befehl. Hebel: nur installieren was gebraucht wird; projekt- vs.
  globale Skill-Pfade bewusst nutzen (`.opencode/skills/`, `~/.config/opencode/skills/`, kompatibel
  `.claude/skills/`, `.agents/skills/`); Skill-`description` kurz halten (zählt zum Dauer-Overhead).

### 8.3 Plugins — Liste knapp halten + Compaction-Hook
- Nur die in `opencode.json` `"plugin":[...]` **gelisteten** Plugins werden geladen → Liste minimal
  halten; jedes von einem Plugin via `tool()` registrierte **Custom-Tool** vergrößert das Tool-Schema.
- **Compaction-Hook** `experimental.session.compacting` (Plugin): hält den bei Komprimierung
  erhaltenen Kontext gezielt klein (`output.context.push(...)` ergänzt, `output.prompt="..."` ersetzt
  den Default-Compaction-Prompt komplett). `offiziell` (opencode.ai/docs/plugins)
- Sichtbarkeit: Community-Plugin `/context` (IgorWarzocha/Opencode-Context-Analysis-Plugin) zeigt die
  Token-Verteilung — misst nur, reduziert nicht. `extern`

### 8.4 Agenten — per-Agent `tools`-Whitelist bestätigt
- Per-Agent `tools`-Whitelist (`tools: [Read, Write, Bash, Task]` bzw. Glob `"firecrawl*": true`) und
  `mode`/`model` pro Agent sind der Weg, Subagenten schlank zu halten. `extern` (Medium, Hightower)
- **Tool-Vererbung Primary→Subagent ist NICHT offiziell dokumentiert** — im Zweifel pro Agent explizit
  eine `tools`-Whitelist setzen, statt auf Vererbung zu vertrauen.

### 8.5 Changelog-Splitter 1.17.10/1.17.11 (token-/kontext-relevant)
- **v1.17.10 (24.06.2026):** „Added MCP server instructions to session context"; **`--mini` CLI mode**
  (klingt token-relevant — Quellen sagen NICHT genau was er tut → **noch prüfen**); V2-Plugin-API. `extern`
- **v1.17.11 (25.06.2026):** Session-Snapshots + Revert-Controls; MCP-OAuth-URL immer ausgeben. `extern`
- **Keine** expliziten Compaction-/Pruning-/Tool-Schema-Token-Optimierungen in 1.17.10/1.17.11 belegt.

### 8.6 Offen / noch zu prüfen
- Was `--mini` (v1.17.10) genau bewirkt (möglicher Token-Hebel).
- Ob ein späteres 1.17.x doch MCP-Lazy-Loading bringt (Issues #8277/#8625/#9350/#16206 beobachten).

---

## 3. Prompt Caching über OpenRouter — drastischster Hebel bei Wiederholkontext `offiziell`

- **Provider Sticky Routing (automatisch):** nach einer gecachten Anfrage routet OpenRouter Folgeanfragen
  desselben Modells zum selben Provider (Cache warm). Bei manuellem `provider.order` deaktiviert.
- **Cache-Read-Preise:** DeepSeek **0,1×** (automatisch); Anthropic **0,1×** (braucht `cache_control`,
  Min-Cache je Modell); Gemini 2.5 Flash/Pro **0,25×** (implizit automatisch); OpenAI 0,25–0,5× (automatisch).
- **Konsequenz:** DeepSeek + Gemini Flash cachen **automatisch** → für ein token-armes Setup besonders attraktiv.
- **OpenCode-Eingriff:** für Anthropic `"setCacheKey": true` (Cache-Key erzwingen). `offiziell`
- **Konsistenz-Trick:** Anfang der Nachrichten stabil halten → stabile, kurze AGENTS.md = höhere Cache-Hit-Rate.
- **Cache-Sichtbarkeit:** `usage.prompt_tokens_details.cached_tokens` / `cache_discount`.

---

## 4. Provider Routing: `sort:"price"`, `:free`, Trade-offs `offiziell`

| Parameter | Wirkung |
|---|---|
| `sort:"price"` | billigster Provider (Shortcut `:floor`); deaktiviert Load-Balancing |
| `sort:"throughput"` | höchster Durchsatz (Shortcut `:nitro`) |
| `max_price:{prompt,completion}` | harte Preisobergrenze (bricht Request ab, wenn nichts darunter) |
| `order:[...]` | Reihenfolge erzwingen (deaktiviert Sticky-Routing + Load-Balancing) |
| `allow_fallbacks:false` | nur primärer Provider |
| `data_collection:"deny"` | Provider ausschließen, die Daten speichern/trainieren |
| `require_parameters:true` | nur Provider mit voller Parameter-Unterstützung (Tool-Calling!) |

> Mit `tools`/`tool_choice` routet OpenRouter **automatisch nur zu Tool-fähigen Providern** — wichtig für OpenCode.

**`:free` Trade-offs:** 20 Req/Min; 50/Tag (<10 Credits) bzw. 1000/Tag (≥10 Credits); geringere
Verfügbarkeit/Throughput; Provider-Datennutzung realistisch → **nicht für sensiblen Code**. Eignet sich für
Experimente, Nebenaufgaben, `small_model`-Rollen. Für verlässliche tägliche Arbeit ist ein **sehr günstiges
bezahltes** Modell stabiler (keine Tageslimits, automatisches Caching, bessere Verfügbarkeit).

---

## 5. Modellstrategie günstig + gut (Tool-Calling-tauglich)

| Modell (OpenRouter-ID) | In/Out (ca.) | Caching | Rolle |
|---|---|---|---|
| **DeepSeek V3.2** | ~$0,23 / ~$0,34 | auto, Read 0,1× | Solides Hauptmodell, starkes agentisches Tool-Use |
| **DeepSeek V4 Flash** | ~$0,09 / ~$0,18 | auto | Sehr günstig, 1M-Kontext → top `small_model` + günstiges Haupt |
| **Gemini 2.5 Flash** | günstig | implizit auto (0,25×) | Schnell, Tool-Calling, Cache ohne Config |
| **GLM 4.6 / Qwen3 Coder** | günstig | je nach Provider | Coding-spezialisiert (Qwen-Caching Read 0,1×) |
| **GPT-5 Nano** (OpenCode Zen) | Free | Free | permanent free, privacy-safe → idealer `small_model` |

**Strategie:**
- **Hauptmodell** mittelpreisig + toolfähig: DeepSeek V3.2 / V4 Flash oder Gemini 2.5 Flash (auto-Caching).
- **`small_model`** sehr günstig: DeepSeek V4 Flash oder GPT-5 Nano (free) — für Titel/Summary/Compaction.
- **Plan/Explore/Research-Subagenten** auf das billige Modell, **Build** auf das Hauptmodell.
- **Premium punktuell** (Opus/Sonnet/GPT-5 Codex): nur für schwierige Architektur/Debug per `--model` —
  nicht als Default. (Ein günstiges Modell mit mehreren fehlerhaften Iterationen re-sendet jedes Mal Kontext
  → kann teurer werden als ein guter Premium-Turn.)

---

## 6. Kosten beobachten & Budget kontrollieren

- **OpenCode CLI `opencode stats`** — Token-Usage + Kosten pro Session (`--days`, `--tools`, `--models`,
  `--project`). TUI `/sessions` zeigt Usage/Cost. `offiziell`/`extern`
- **OpenRouter:** pro Antwort `usage`-Objekt (`prompt_tokens`, `completion_tokens`, `cost`,
  `prompt_tokens_details.cached_tokens`); Activity-Dashboard + `/api/v1/generation` zeigen `cache_discount`. `offiziell`
- **Budget-Hard-Limits:** OpenRouter Workspace Budgets + API-Key-Limits. `offiziell`
- **Drittanbieter-Tools (`extern`):** opencode-tokenscope, tokscale, opencode-quota, ocmonitor;
  opencode-dynamic-context-pruning (ersetzt nicht mehr benötigte Tool-Outputs durch Platzhalter).

---

## 7. Empfohlene Beispiel-Konfiguration: token-armer Harness

Vollständige `opencode.json` (z.B. global). Modell-IDs ggf. mit `opencode models openrouter --refresh` prüfen.

```jsonc
{
  "$schema": "https://opencode.ai/config.json",

  // --- Modelle: mittelpreisig+toolfähig als Haupt, sehr günstig als small ---
  "model": "openrouter/deepseek/deepseek-v3.2",
  "small_model": "openrouter/deepseek/deepseek-v4-flash",

  // --- Provider-Routing: billig-zuerst, nur Tool-fähige Provider, Privacy, Anthropic-Cache aktiv ---
  // WICHTIG: Routing-Optionen gehören PRO MODELL unter options.provider (nicht global "extraBody").
  "provider": {
    "openrouter": {
      "options": { "setCacheKey": true },   // hält Caching auf Anthropic-Routen aktiv
      "models": {
        "deepseek/deepseek-v3.2": {
          "options": { "provider": { "sort": "price", "require_parameters": true, "data_collection": "deny" } } },
        "deepseek/deepseek-v4-flash": {
          "options": { "provider": { "sort": "price", "require_parameters": true, "data_collection": "deny" } } }
      }
    }
  },

  // --- Plan als Default: erst denken, dann (sparsam) bauen ---
  "default_agent": "plan",

  // --- Kontext schlank halten ---
  "compaction": { "auto": true, "prune": true, "reserved": 10000 },

  // --- Globale Tool-/Permission-Diät ---
  "permission": { "edit": "ask", "bash": "ask", "webfetch": "deny" },

  // --- Agents: Iterationen deckeln, günstige Modelle pro Rolle ---
  "agent": {
    "plan":  { "mode": "primary", "model": "openrouter/deepseek/deepseek-v4-flash", "temperature": 0.1,
               "steps": 20, "permission": { "edit": "deny", "bash": "deny" } },
    "build": { "mode": "primary", "model": "openrouter/deepseek/deepseek-v3.2",
               "permission": { "edit": "ask", "bash": "ask" } },
    "explore": { "mode": "subagent", "model": "openrouter/deepseek/deepseek-v4-flash",
               "permission": { "edit": "deny", "bash": "deny", "webfetch": "deny" } }
  },

  // --- MCP: per Default ALLE aus, nur per Agent zuschalten ---
  "mcp":   { "context7": { "type": "remote", "url": "https://mcp.context7.com/mcp", "enabled": false } },
  "tools": { "context7*": false },

  // --- Schlanke Rule-Datei statt Mega-Regelwerk ---
  "instructions": ["AGENTS.md"],

  // --- Nebenkosten/Overhead runter ---
  "snapshot": false,
  "autoupdate": "notify",

  // --- Plattform-Shell (Windows: "pwsh"; macOS: weglassen -> Auto /bin/zsh) ---
  "shell": "pwsh"
}
```

**Begründung jeder Entscheidung:**
- **`sort:"price"` + `require_parameters:true`:** billigsten Provider nehmen, aber nur Tool-fähige (sonst
  scheitert der agentische Loop).
- **`data_collection:"deny"`:** Code-Privatsphäre ohne Mehrkosten.
- **`setCacheKey:true`:** hält Caching auf Anthropic-Routen aktiv (DeepSeek/Gemini cachen ohnehin automatisch).
- **DeepSeek V3.2 / V4 Flash:** mittelpreisig+toolstark als Haupt, ultra-günstig fürs Small; beide auto-Cache 0,1×.
- **`default_agent:"plan"`:** Plan denkt ohne teure Edit-/Bash-Iterationen.
- **`compaction.prune:true`:** entfernt alte Tool-Outputs aus dem gesendeten Kontext.
- **`steps`-Limit:** verhindert Runaway-Iterationen.
- **MCP `enabled:false` + `tools` glob-off:** kein Tool-Schema-Overhead pro Anfrage; gezielt pro Agent zuschalten.
- **Schlanke AGENTS.md + `snapshot:false`:** höhere Cache-Hit-Rate, weniger Dauer-Tokens, weniger Index-Overhead.

> **Korrektur-Hinweis:** Die Routing-Optionen (`sort`, `max_price` …) müssen **pro Modell** unter
> `provider.openrouter.models.<model>.options.provider` stehen — das ist die offiziell belegte Durchreiche
> (opencode.ai/docs/providers + openrouter cookbook). Ein globales `provider.openrouter.options.extraBody` ist
> NICHT die dokumentierte Form. Siehe `openrouter.md` §3.

**Ergänzende Workflow-Disziplin (größter Praxis-Effekt, kein Config-Eintrag):**
1. Gezielte `@datei`-Mentions statt ganze Ordner.
2. Aufgaben eng formulieren („nur diese Funktion ändern").
3. Für neue Aufgaben neue Sessions; `/compact` bewusst nutzen.
4. Premium-Modell nur punktuell per `--model`.
5. Regelmäßig `opencode stats` + OpenRouter-Activity prüfen.

## Quellen
**Offiziell:** opencode.ai/docs/config, /models, /agents, /mcp-servers, /rules, /cli, /tools, /zen;
openrouter.ai/docs guides/best-practices/prompt-caching, guides/routing/provider-selection,
guides/routing/model-variants/free, cookbook/administration/usage-accounting, api/reference/limits.
**Extern:** TrueFoundry (OpenCode Token Usage), OpenRouter Zendesk (Rate Limits), Datastudios (Free limits),
Portkey (Token/Costs/Access-Control), opencode-dynamic-context-pruning, opencode-tokenscope.

> Modell-IDs/Preise ändern sich häufig — vor Übernahme `opencode models openrouter --refresh` + openrouter.ai/models.
> Die Mechanik-Hebel selbst (MCP-Diät, schlanke AGENTS.md, small_model, Plan-Default, Compaction-Prune,
> sort:price, Caching) sind stabil und modellunabhängig.
