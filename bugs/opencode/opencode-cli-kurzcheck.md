# OpenCode CLI Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Die häufigsten/teuersten Fallen. Bei einem Fehler im Bereich: **Volltext** dieser Datei lesen
> (Stufe B). Volltext-Spalte = Abschnitt unten.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ Windows nativ: npm-Wrapper, kaputte Umlaute, Paste tot, Bun-Segfault | Offiziell **WSL nutzen** (`opencode.ai/docs/windows-wsl`). Nativ ist Fallback. | §1, §2, §12 |
| 2 | ⭐ Kontext/Token läuft voll, viele MCP-Server aktiv | Jeder MCP lädt sein Tool-Schema in JEDEN Prompt (GitHub-MCP ~15–20k Tok; Extrem 147k). **KEIN natives Lazy-Loading** (Stand 1.17.11, anders als Claude Code) → manuelle Per-Agent-Auslagerung ist der EINZIGE Hebel: global `"tools":{"servername*":false}`, im Agent `true`. | §8 (#56) |
| 3 | ⭐ Agent ändert/committet ungefragt | Defaults sind permissiv (`edit`/`bash` = allow). `permission: {edit:"ask", bash:"ask"}`. Permission-Keys **nur lowercase** — PascalCase (`"Bash"`) wird STILL ignoriert! | §6 |
| 4 | ⭐ OpenCode startet nicht: `ConfigInvalidError unrecognized keys` | Top-Level-Config ist **strict** → nur dokumentierte Keys. Anweisungen via `instructions`/`AGENTS.md`, nicht erfundene Keys. `opencode debug config`. | §3 |
| 5 | „Model not found" erst beim ersten Request | ID-Format ist `provider/model`; OpenRouter **doppelt**: `openrouter/<author>/<model>`. `opencode models` listet gültige IDs. | §10 |
| 6 | Globale + Projekt-`AGENTS.md` — globale Regeln „fehlen" | Werden NICHT sauber gemergt; globale wird teils still ignoriert. In Projekt-`AGENTS.md` `@~/.config/opencode/AGENTS.md` referenzieren. | §4 |
| 7 | Unerwartet teuer / teures Modell für Nebenaufgaben | `small_model` explizit auf billiges Modell; bewusstes (günstiges) Default-`model`; teure Modelle gezielt zuweisen. | §10, §11 |
| 8 | Lange Session: `context_length_exceeded` / „prompt too long" | Auto-Compaction triggert zu spät / verliert Details. `compaction.prune:true`, `reserved` hoch, Fakten in `AGENTS.md`, große Outputs in Dateien. | §5 |
| 9 | `ProviderInitError` / `AI_APICallError` | Provider-Cache leeren `~/.cache/opencode` (Pakete neu laden); bei korrupter Config `~/.local/share/opencode` (Achtung: löscht Auth/Sessions), dann `/connect`. | §10 |
| 10 | Subagents: versteckte Kosten / falsches Modell | Subagent erbt Modell des Primary, nicht das globale → explizit `model:` setzen. Subagent-Token zählt der TUI-Counter NICHT. Step-Limit setzen. | §6, §11 |
| 11 | Agent/Command/Plugin/Skill „wird nicht erkannt" | Verzeichnisse sind **Plural**: `agents/ commands/ plugins/ skills/ tools/`. `agent create` schreibt fälschlich `agent/` (Singular). | §6, §7, §9 |
| 12 | `opencode upgrade` meldet „unknown" (Windows) | Detection scheitert an `npm.cmd`. Manuell per Paketmanager updaten (`npm i -g opencode-ai@latest` / `scoop update opencode`). | §1 |
| 13 | ⭐ OpenCode-Go: API-Fehler je Modell / GLM früh „aufgebraucht" / Modell erfindet Fakten | **Zwei Endpunkt-Schemata:** DeepSeek/GLM/Kimi/MiMo = OpenAI (`/zen/go/v1/chat/completions`), Qwen/MiniMax = Anthropic (`/zen/go/v1/messages`). GLM-5.x im Go-Tier nur ~4.300 Req/Mo (nicht für Masse). DeepSeek V4 Pro halluziniert bei Nichtwissen → Abstain-Prompt. | §14 |
| 14 | ⭐ OpenCode-Go-Modell einrichten / Thinking aktivieren | Go ist **eingebaut** → `/connect`, KEIN Custom-`@ai-sdk/anthropic`-Block (Key-Verlust #21737); nur MERGE-Block für Optionen. Thinking-Keys in `docs/models` (nicht config): Anthropic `options.thinking.budgetTokens`, OpenAI `reasoningEffort`. MiniMax denkt nativ. | §14.4–14.5 |
| 15 | ⭐ Direkter Python-Call an Go-Gateway/OpenRouter → Cloudflare 403/„1010" | urllib-Default-UA wird geblockt → `User-Agent: curl/8.5.0` setzen (so `mm/or-research _post()`). Erinnerung: `/messages`=`x-api-key`, Thinking `{type:enabled,budget_tokens:N}` (NICHT `adaptive` — das nur bei `/chat/completions`) | §14.6–14.8 |
