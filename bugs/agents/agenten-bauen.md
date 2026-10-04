# KI-Agenten selbst bauen — Bug-Almanach (Stand 04.10.2026, 23:30 Uhr)

> Fallen beim Eigenbau von Agenten: Schleife, Werkzeuge, MCP, Kontext, Kosten, Rechte, Evals, Abo-Anmeldung.
> Gegenstück: `best-practices/agents/agenten-bauen.md`. Quelle: Recherche vom 04.10.2026 (sechs Sonnet-5.5-Researcher).
> Alle 80+ Einzelfunde mit Quelle stehen verlustfrei in `~/proggs/Lernen/ki-agenten/research/01` bis `06`
> (jeweils Abschnitt „BUG-KANDIDATEN“). Hier stehen die Fallen, die beim Eigenbau am ehesten zuschlagen.
> Abgrenzung: Dauerbetrieb der Schleife → `agents/loop-engineering.md`; Mehr-Agenten → `agents/orchestrator-agent.md`;
> Abo-OAuth → `apis/cli-impersonation-subscription-auth.md`.
>
> **Versions-Anker (04.10.2026):** Claude Messages API (`anthropic-version: 2023-06-01`), Claude Agent SDK 0.3.289,
> Codex SDK/CLI 0.160.0, MCP-Spezifikation 2026-07-28, MCP TypeScript SDK 2.3.0, MCP Kotlin SDK 0.15.0,
> anthropic-java 2.68.0, Vercel AI SDK 7.0.127.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | 400 „tool_use ids were found without tool_result blocks“ | Jeder Werkzeugwunsch braucht sein Ergebnis, direkt in der nächsten `user`-Nachricht, Ergebnisblöcke zuerst | §1 |
| 2 | Agent läuft endlos, Kosten steigen | Eigenes Rundenlimit und Kostenlimit setzen, Standard ist unbegrenzt | §1 |
| 3 | 400 bei `tool_choice: any` oder `tool` | Bei den aktuellen Claude-Modellen nicht erlaubt, `auto` verwenden | §2 |
| 4 | Halber Werkzeugwunsch bei `max_tokens` | Nie ausführen, Grenze erhöhen und wiederholen | §1 |
| 5 | OpenAI: Werkzeugargumente sind ein String | `arguments` erst als JSON parsen; strict-Schema verlangt alle Felder als `required` | §2 |
| 6 | MCP-Beispiel aus Tutorial läuft nicht | v1-SDK/Sessions passen nicht zu 2026-07-28; TS v2: pro Anfrage eigene Instanz | §3 |
| 7 | Kosten springen plötzlich | Cache gebrochen: Werkzeuge oder Modell mitten in der Sitzung geändert, Zeitstempel im Präfix | §4 |
| 8 | Agent nutzt Bash trotz `allowedTools: ["Read"]` | `allowedTools` begrenzt `bypassPermissions` nicht, `disallowedTools` nehmen | §5 |
| 9 | Annahme „Befehle laufen in der Sandbox“ | Unter nativem Windows gibt es keine Sandbox; sie deckt nie Dateitools, MCP, Hooks | §5 |
| 10 | Agent gibt nach Lesen fremder Inhalte Daten preis | Indirekte Prompt Injection; Lethal Trifecta auflösen | §5 |
| 11 | Eval zeigt 0 % oder bestraft gute Lösungen | Erst Aufgabe und Prüfer prüfen; Ergebnis prüfen, nicht den Weg | §6 |
| 12 | Agent SDK / Codex SDK in Android-App | Geht nicht: beide brauchen die CLI als Unterprozess | §7 |
| 13 | Abo wird ignoriert, API wird abgerechnet | Gesetztes `ANTHROPIC_API_KEY` schlägt das Abo-Login | §7 |
| 14 | „refresh token was already used“ (Codex) | Eine `auth.json` pro Rechner, nie parallel oder mit altem Stand überschreiben | §7 |
| 15 | SIWC-Anfrage abgelehnt | `store:false`, `stream:true`; keine `temperature`, `max_output_tokens`, `previous_response_id` | §7 |

## 1. Schleife

**400 „tool_use ids were found without tool_result blocks immediately after“.** Ursache: zwischen Assistenten-Nachricht mit `tool_use` und User-Nachricht mit `tool_result` liegt eine andere Nachricht, Text steht vor den Ergebnisblöcken, oder ein Werkzeugwunsch blieb ohne Ergebnis (typisch: Code bearbeitet nur den ersten Block, das Modell rief aber mehrere Werkzeuge parallel auf). Fix: alle Blöcke bearbeiten, alle Ergebnisse in eine Nachricht, oder bewusst `disable_parallel_tool_use: true`. Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls

**Endlosschleife und Kostenexplosion.** Ursache: kein Limit. Tutorial-Schleife hat keine Obergrenze, Claude Agent SDK hat für `maxTurns` und `maxBudgetUsd` standardmäßig keine. Dazu das ReAct-Fehlbild: das Modell wiederholt dieselbe Aktion. Fix: eigener Zähler, Kostenlimit, Wiederholungserkennung. Quellen: https://code.claude.com/docs/en/agent-sdk/agent-loop · https://arxiv.org/pdf/2210.03629

**Abgeschnittener Werkzeugwunsch bei `stop_reason: "max_tokens"`.** Ursache: Grenze zu niedrig für die Argumente (ganze Dateiinhalte). Fix: Grenze erhöhen, wiederholen, den halben Aufruf nicht ausführen. Quelle: https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons

**Leere Antwort nach einem Werkzeugergebnis.** Ursache: Text hinter dem `tool_result` im selben Block. Fix: nur das Ergebnis senden. Quelle: wie oben.

**`pause_turn` und `refusal` nicht behandelt.** Symptom: scheinbarer Abbruch mitten in der Arbeit. Fix: `pause_turn` unverändert zurückschicken, bei `refusal` den Grund auswerten. Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works

**Einmalige `query()`-Schleife wirft nach `error_max_turns` eine Ausnahme (Agent SDK).** Gewolltes Verhalten. Fix: try/catch. Quelle: https://code.claude.com/docs/en/agent-sdk/agent-loop

## 2. Werkzeuge

**`tool_choice: any` oder `tool` ergibt HTTP 400** bei Opus 5.5, Sonnet 5.5, Fable 5.1, Mythos 5.1. Fix: `auto` und die Wahl über Prompt und Beschreibung steuern. Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools

**Modell rät fehlende Pflichtparameter.** Fix: Beschreibung schärfen, zum Nachfragen anweisen, `strict`-Schema. Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview

**Relative Pfade nach Verzeichniswechsel führen zu falschen Dateien.** Fix: absolute Pfade verlangen. Quelle: https://www.anthropic.com/engineering/building-effective-agents

**OpenAI: `arguments` ist ein String; strict-Schema wird abgelehnt.** Fix: JSON parsen; alle Felder `required`, optionale als `null`-Typ, `additionalProperties: false`. Assistants-API ist seit 26.08.2026 abgeschaltet, Responses API nutzen. Quellen: https://developers.openai.com/api/docs/guides/function-calling · https://developers.openai.com/api/docs/guides/migrate-to-responses

**Zu viele Werkzeuge verwirren das Modell** (Auswahl wird ab etwa 30 bis 50 ungenau, Definitionen fressen Kontext). Fix: bündeln, Tool Search. Dabei muss mindestens ein Werkzeug `defer_loading=false` haben, sonst 400. Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool

## 3. MCP

**Alte Tutorials (v1-SDK, Sessions, `initialize`) passen nicht zur Spezifikation 2026-07-28.** Quelle: https://modelcontextprotocol.io/specification/2026-07-28/changelog

**TS-SDK 2.x: ein `McpServer`/Transport für mehrere Anfragen geteilt** führt zu Fehlern. Fix: pro Anfrage eigene Instanz. Quelle: https://github.com/modelcontextprotocol/typescript-sdk

**Kotlin-SDK 0.15.0: doppelter Werkzeugname wirft eine Ausnahme.** Quelle: https://github.com/modelcontextprotocol/kotlin-sdk

**MCP-Connector der Claude-API erreicht lokalen Stdio-Server nicht.** Er spricht nur entfernte Server an. Quelle: https://platform.claude.com/docs/en/agents-and-tools/mcp-connector

## 4. Kontext und Kosten

**Cache-Miss oder plötzlicher Kostensprung.** Ursache: Wechselndes (Zeitstempel, Nutzereingabe) im gecachten Präfix, Werkzeuge oder Modell mitten in der Sitzung geändert, Context Editing ohne `clear_at_least`. Fix: Statisches zuerst, Cache-Trefferquote messen. Quelle: https://platform.claude.com/docs/en/build-with-claude/prompt-caching

**Compaction: Block falsch zurückgegeben oder leere Antwort trotz HTTP 200.** Fix: Block unverändert samt Signatur zurückgeben, `stop_reason` prüfen, Kosten aus `usage.iterations` lesen. Quelle: https://platform.claude.com/docs/en/build-with-claude/compaction

**Thinking-Block verändert → API-Fehler nach Werkzeugergebnis.** Der zum Werkzeugwunsch gehörende Block muss unverändert zurück. Quelle: https://platform.claude.com/docs/en/build-with-claude/context-windows

**Agent hört wegen vermeintlich knappem Kontext zu früh auf.** Fix: im Prompt sagen, dass automatisch verdichtet wird. Quelle: https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/claude-prompting-best-practices

**Kosten falsch gezählt (Agent SDK).** Parallele Werkzeugaufrufe liefern mehrere Assistenten-Nachrichten mit derselben ID und gleicher `usage`; `usage` zählt Subagenten nicht; beim Fortsetzen enthalten Ergebnisse schon frühere Ausgaben. Fix: `total_cost_usd` bzw. `modelUsage` aus dem Ergebnis nehmen. Quelle: https://code.claude.com/docs/en/agent-sdk/cost-tracking

**Path Traversal im Memory-Tool-Handler.** Fix: Pfad auf den Speicherordner begrenzen, kanonisieren, `..` auch URL-codiert ablehnen. Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool

## 5. Rechte und Sicherheit

**`allowedTools` begrenzt `bypassPermissions` nicht; Subagenten erben den Modus des Elternteils; `canUseTool` wird von Allow-Regeln übersprungen.** Fix: `disallowedTools`, Modus ausdrücklich setzen (seit Agent SDK 0.3.286 greift bei weggelassenem `permissionMode` die Einstellung aus den Settings). Quelle: https://code.claude.com/docs/en/agent-sdk/permissions

**Bash-Deny-Regel umgehbar** (Aufruf per Pfad, `sh -c`). Fix: Hook oder Sandbox. **`.claudeignore` hat keine Wirkung**, nötig sind `Read`-Deny-Regeln. Quelle: https://code.claude.com/docs/en/permissions

**Sandbox fehlt unter nativem Windows** („runs commands unsandboxed“), und sie deckt Dateitools, MCP und Hooks nie ab. Node `fetch()` ignoriert `HTTP_PROXY` (ab Node 24 `NODE_USE_ENV_PROXY=1`). Quelle: https://code.claude.com/docs/en/sandboxing

**Indirekte Prompt Injection über Werkzeugergebnisse.** Der Agent befolgt Anweisungen aus Webseite, Mail oder Issue. Verteidigungen, die gegen feste Angriffe wirken, fallen gegen angepasste. Fix: Lethal Trifecta auflösen, menschliche Freigabe vor folgenschweren Aktionen. Quellen: https://simonwillison.net/2025/Jun/16/the-lethal-trifecta/ · https://simonwillison.net/2025/Nov/2/new-prompt-injection-papers/

## 6. Evals

**0 % Erfolg über viele Läufe:** meist ist die Aufgabe oder der Prüfer kaputt, nicht der Agent. **Zu starrer Prüfer** bestraft gültige Lösungen (Weg statt Ergebnis geprüft). **LLM-Richter** bevorzugt längere und zuerst genannte Antworten. **Rubrik vor Fehleranalyse:** Punkte steigen, Nutzer merken nichts. Quellen: https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents · https://developers.openai.com/api/docs/guides/evaluation-best-practices · https://arxiv.org/abs/2404.12272

## 7. Baukästen und Abo-Anmeldung

**Claude Agent SDK oder Codex SDK in einer Android-App:** nicht möglich, beide starten die CLI als Unterprozess. Fix: Schleife in der App selbst bauen (Eigenbau oder Koog), SDKs nur am PC/Server. Quellen: https://code.claude.com/docs/en/agent-sdk/hosting · https://github.com/openai/codex/blob/main/sdk/typescript/README.md

**Agent SDK „Claude Code not found“:** `npm ci --omit=optional` überspringt das Binary-Paket. **SDK liest `.env` nicht.** **Codex SDK bricht ohne Git-Repository ab** (`skipGitRepoCheck: true`). Quellen: https://code.claude.com/docs/en/agent-sdk/quickstart · https://github.com/openai/codex/blob/main/sdk/typescript/README.md

**Abo wird ignoriert, API-Abrechnung läuft:** gesetztes `ANTHROPIC_API_KEY` oder `ANTHROPIC_AUTH_TOKEN` schlägt das Abo-Login; `CLAUDE_CODE_OAUTH_TOKEN` wird im `--bare`-Modus nicht gelesen. Fix: Variable entfernen, mit `/status` prüfen. Quelle: https://code.claude.com/docs/en/authentication

**Codex: „refresh token was already used“ / „revoked“.** Dieselbe `auth.json` auf zwei Rechnern oder mit altem Stand überschrieben; Refresh-Token rotieren. Fix: eine Datei pro Rechner, neu anmelden. Quelle: https://learn.chatgpt.com/docs/auth/ci-cd-auth

**SIWC (Sign in with ChatGPT): Anfrage abgelehnt oder `response.failed` im Strom.** Ursache: `store`/`stream` falsch, nicht erlaubte Felder (`temperature`, `max_output_tokens`, `previous_response_id`, System-Rolle als Nachricht), nicht unterstützte Werkzeuge (Datei-Suche, Code Interpreter, gehostetes MCP) oder Wochenlimit. **Redirect-Fehler:** Pfad muss `/auth/callback` auf `http://127.0.0.1:<Port>` sein, Listener vor dem Browser starten. **Token lebt 1 Stunde**, Refresh-Token rotiert. Quellen: https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations.md · https://developers.openai.com/siwc/token-sharing-open-source/sign-in.md

**Vercel-AI-SDK-Community-Provider für Claude Code / Codex CLI rufen eigene Zod-Werkzeuge nicht auf.** Fix: Werkzeuge als MCP-Server anbieten. Quelle: https://ai-sdk.dev/providers/community-providers/claude-code

**Konto gesperrt, wenn ein eigener Client die Claude-Code-Identität nachahmt** (Sperre seit 09.01.2026). Fix: nur das unveränderte Binary oder einen API-Schlüssel nutzen. Quelle: https://code.claude.com/docs/en/legal-and-compliance (offiziell), Chronologie extern.
