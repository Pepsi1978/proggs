# Research 02: Werkzeuge (Tool Use / Function Calling) und MCP

Stand der Recherche: 04.10.2026. Alle Aussagen wurden durch Abruf der genannten URL am 04.10.2026 geprüft (WebFetch, `gh api`, `npm view`). Nichts stammt aus dem Gedächtnis.

## Kernaussagen

### A. Tool Use bei Anthropic (Messages API)

1. **Ablauf und Feldnamen.** Man übergibt `tools` (je `name`, `description`, `input_schema`). Will Claude ein Werkzeug nutzen, kommt `stop_reason: "tool_use"` und mindestens ein `tool_use`-Block mit `id`, `name`, `input`. Der eigene Code führt aus und schickt eine neue `user`-Nachricht mit `tool_result`-Block (`tool_use_id`, optional `content`, optional `is_error`). Client-Tools laufen in der eigenen Anwendung, Server-Tools (`web_search`, `web_fetch`, `code_execution`, `tool_search`) laufen bei Anthropic. Header: `anthropic-version: 2023-06-01`.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview und https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls

2. **Keine eigene Rolle für Werkzeuge.** Anders als bei anderen APIs gibt es keine Rolle `tool`/`function`. `tool_use` steckt in `assistant`-Nachrichten, `tool_result` in `user`-Nachrichten, beides als Content-Block-Arrays.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls (Abschnitt "Differences from other APIs")

3. **Formatregeln für `tool_result` (häufigste Fehlerquelle).** Das Ergebnis muss direkt auf die Nachricht mit dem `tool_use` folgen. Im `content`-Array müssen alle `tool_result`-Blöcke VOR jedem Text stehen. Text vor dem `tool_result` führt zu HTTP 400. Fehlermeldung bei Verstößen: "tool_use ids were found without tool_result blocks immediately after". Text nach `tool_result` ist formal erlaubt, kann aber leere Antworten provozieren (siehe Bug-Kandidat 2).
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls und https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons

4. **Stop-Reasons, die eine Agentenschleife behandeln muss:** `end_turn`, `max_tokens`, `stop_sequence`, `tool_use`, `pause_turn` (Server-Tool-Schleife hat ihr Iterationslimit, Standard 10, erreicht; Assistenten-Inhalt zurückschicken), `refusal`, `model_context_window_exceeded`. Stop-Reasons stehen in erfolgreichen 200-Antworten, Fehler sind 4xx/5xx. Bei `max_tokens` mit unvollständigem `tool_use`-Block muss man mit höherem `max_tokens` wiederholen.
   Quelle: https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons

5. **Fehlerrückgabe an das Modell.** Wirft das Werkzeug einen Fehler, schickt man `tool_result` mit `is_error: true` und einer instruktiven Meldung (was schiefging, was als Nächstes zu versuchen ist, z. B. "Rate limit exceeded. Retry after 60 seconds."). Bei ungültigen Aufrufen versucht Claude laut Doku 2 bis 3 Mal zu korrigieren, bevor es sich beim Nutzer entschuldigt.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls (Abschnitt "Handling errors with is_error")

6. **Parallele Werkzeugaufrufe.** Eine Assistenten-Antwort kann mehrere `tool_use`-Blöcke enthalten. Die API schreibt keine Ausführungsreihenfolge vor (gleichzeitig mit `Promise.all` oder nacheinander). Pflicht: pro `tool_use` genau ein `tool_result`, alle zusammen in EINER `user`-Nachricht. Wird ein Aufruf übersprungen, trotzdem ein `tool_result` mit `is_error: true` ("Not executed: ...") liefern. Abschalten per `tool_choice: {"type":"auto","disable_parallel_tool_use":true}`. Falsche Formatierung (jedes Ergebnis in eigener `user`-Nachricht) "lehrt" Claude, künftig weniger parallel aufzurufen.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/parallel-tool-use

7. **`tool_choice`-Werte:** `auto` (Standard bei vorhandenen Tools), `any`, `tool` (festes Werkzeug), `none`. Bei `any`/`tool` füllt die API die Assistenten-Nachricht vor, deshalb gibt es keinen erklärenden Text vor dem `tool_use`. Änderungen an `tool_choice` machen gecachte Nachrichtenblöcke ungültig (Tool-Definitionen und System-Prompt bleiben gecacht).
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools (Abschnitt "Forcing tool use")

8. **Erzwungener Werkzeugaufruf geht nicht mehr überall.** Laut Doku liefern `tool_choice: any` und `tool` bei Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 und Claude Mythos 5.1 einen 400-Fehler; auch bei manuellem Extended Thinking (`thinking: {type: "enabled"}`) nicht unterstützt. Ersatz: `auto` plus `strict: true`, oder Structured Outputs.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools

9. **Strict Tool Use.** `strict: true` im Tool-Objekt erzwingt per grammatikbeschränktem Sampling, dass `input` exakt zum `input_schema` passt und `name` gültig ist. Beispiel: `passengers: 2` statt `"2"`. Das Schema braucht `additionalProperties: false`; unterstützt ist nur eine Teilmenge von JSON Schema (siehe Structured-Outputs-Seite). Schemas werden bis 24 h gecacht; keine Gesundheitsdaten in Schemas.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/strict-tool-use

10. **Werkzeugdefinition: Regeln.** `name` muss `^[a-zA-Z0-9_-]{1,128}$` erfüllen. Beschreibung ist laut Doku "by far the most important factor": was es tut, wann (und wann nicht) nutzen, Bedeutung jedes Parameters, Einschränkungen; mindestens 3 bis 4 Sätze. Verwandte Operationen zu weniger Werkzeugen bündeln (statt `create_pr`/`review_pr`/`merge_pr` ein Werkzeug mit `action`-Parameter). Namespacing mit Präfix (`github_list_prs`). Antworten nur mit hochwertigen Informationen. Optional `input_examples` für komplexe Eingaben (kostet ca. 20 bis 50 bzw. 100 bis 200 Token je Beispiel). Parameter nicht nach "Reasoning" fragen, sondern nach kurzer Erklärung, sonst droht `reasoning_extraction`-Refusal.
    Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools

11. **Tool-Nutzung kostet Token.** Eingabe-Token enthalten `tools`-Parameter, `tool_use`- und `tool_result`-Blöcke; dazu kommt ein automatischer System-Prompt (z. B. 286 Token bei Claude Sonnet 5.5, `tool_choice` auto/none).
    Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview (Abschnitt "Pricing")

12. **Fehlende Pflichtparameter.** Claude Opus fragt eher nach; Claude Sonnet kann Werte raten (Beispiel: erfindet `"New York, NY"` für `get_weather`). Nicht garantiert.
    Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview (Accordion "When required parameters are missing")

13. **Tool Results sind nicht vertrauenswürdig.** Inhalte aus Webseiten, E-Mails, Uploads, Drittanbieter-APIs können Anweisungen einschleusen (indirekte Prompt Injection). Untrusted Inhalt in `tool_result` belassen, nicht in `system`-Prompt oder Plain-`text`-Blöcke.
    Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls (Warning-Box)

14. **Tool Runner im SDK (Beta).** Das SDK kann die Agentenschleife übernehmen (Werkzeuge ausführen, Ergebnisse zurückschicken, Nachrichtenverlauf). TypeScript: `client.beta.messages.toolRunner(...)` mit `betaZodTool()` (Zod 3.25.0 oder höher) oder `betaTool()` (JSON Schema). Für Human-in-the-Loop oder eigenes Logging nutzt man die manuelle Schleife. Für die Lernkurs-Didaktik gilt: erst die manuelle Schleife bauen, dann den Runner zeigen.
    Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-runner

### B. Gute Werkzeuge entwerfen (Praktikerwissen von Anthropic)

15. **Wenige, durchdachte Werkzeuge für High-Impact-Workflows** statt API-Wrapper 1:1. Funktionalität bündeln. Namespacing nach Dienst (`asana_search`, `jira_search`) oder Ressource (`asana_projects_search`); Präfix versus Suffix per Evaluation testen, Wirkung ist modellabhängig.
    Quelle: https://www.anthropic.com/engineering/writing-tools-for-agents (veröffentlicht 11.09.2025)

16. **Beschreibungen wie für einen neuen Kollegen.** Implizites Wissen explizit machen, eindeutige Parameternamen (`user_id` statt `user`). Kleine Verfeinerungen der Beschreibung können "dramatische" Verbesserungen bringen.
    Quelle: https://www.anthropic.com/engineering/writing-tools-for-agents

17. **Rückgaben:** Kontextrelevanz vor Flexibilität; semantische Namen statt kryptischer UUIDs (senkt Halluzinationen); Pagination, Filter, Range-Selection oder Truncation mit sinnvollen Standardwerten; bei abgeschnittenen Antworten steuernder Hinweistext; ein `response_format`-Parameter (`concise`/`detailed`); Fehlermeldungen konkret und handlungsleitend statt undurchsichtiger Codes.
    Quelle: https://www.anthropic.com/engineering/writing-tools-for-agents

18. **Agent-Computer-Interface (ACI) und Poka-yoke.** Werkzeugspezifikation verdient so viel Aufmerksamkeit wie der Gesamt-Prompt. Parameter so gestalten, dass Fehler schwerer werden (Beispiel: absolute statt relative Dateipfade). Format nahe an natürlich vorkommendem Text halten, Modell "Denkraum" lassen, Formatierungs-Overhead vermeiden (z. B. Zeilen zählen). Anthropic investierte bei SWE-bench mehr Zeit in Werkzeuge als in den Gesamt-Prompt. Definition: Workflows = vordefinierte Codepfade, Agenten = LLM steuert Prozess und Werkzeugnutzung selbst.
    Quelle: https://www.anthropic.com/engineering/building-effective-agents (veröffentlicht 19.12.2024, Anhang 2)

19. **Viele Werkzeuge verschlechtern die Auswahl.** Laut Anthropic-Doku sinkt die Auswahlgenauigkeit, wenn man über 30 bis 50 Werkzeuge hat; ein typisches Multi-Server-Setup (GitHub, Slack, Sentry, Grafana, Splunk) braucht ca. 55k Token nur für Definitionen. Tool Search (serverseitig, `defer_loading: true`) senkt das laut Doku typischerweise um über 85 Prozent. Empfehlung: ab 10 oder mehr Werkzeugen oder über 10k Token Definitionen Tool Search erwägen; die 3 bis 5 meistgenutzten Werkzeuge nicht verzögern. OpenAI empfiehlt vergleichbar: anfangs weniger als 20 Funktionen.
    Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool und https://developers.openai.com/api/docs/guides/function-calling

20. **Messzahlen aus dem Anthropic-Engineering-Blog (Herstellerangaben, nicht unabhängig geprüft):** Tool Search: Opus 4 von 49 auf 74 Prozent, Opus 4.5 von 79,5 auf 88,1 Prozent; Programmatic Tool Calling: Token von 43.588 auf 27.297 (minus 37 Prozent); Tool-Beispiele: von 72 auf 90 Prozent bei komplexen Parametern. Veröffentlicht 24.11.2025.
    Quelle: https://www.anthropic.com/engineering/advanced-tool-use

21. **Code Execution mit MCP.** Wenn Agenten Hunderte Werkzeuge über viele MCP-Server laden und Zwischenergebnisse durch das Modell schleusen, entstehen Kontext-Overhead und Token-Kosten. Alternative: MCP-Server als Code-API anbieten, der Agent schreibt Code, der Filterung in einer Sandbox erledigt. Beispielzahl im Beitrag: 150.000 auf 2.000 Token (minus 98,7 Prozent). Veröffentlicht 04.11.2025.
    Quelle: https://www.anthropic.com/engineering/code-execution-with-mcp

### C. Tool Use bei OpenAI (Responses API)

22. **Format.** Werkzeugdefinition flach (intern getaggt): `type: "function"`, `name`, `description`, `parameters` (JSON Schema), `strict`. Ruft das Modell eine Funktion auf, enthält die Ausgabe ein Item `type: "function_call"` mit `call_id`, `name` und `arguments` (JSON-kodierter STRING). Ergebnis schickt man als Item `{"type":"function_call_output","call_id":"...","output":"..."}` zurück; `call_id` muss übereinstimmen. `output` ist ein String (JSON, Fehlercode oder Klartext).
    Quelle: https://developers.openai.com/api/docs/guides/function-calling

23. **Parallel und Auswahl.** Das Modell kann mehrere Funktionen pro Zug aufrufen; `parallel_tool_calls: false` erzwingt null oder eine. `tool_choice`: `"auto"` (Standard), `"required"`, konkrete Funktion `{"type":"function","name":"..."}`, oder `allowed_tools` (Teilmenge, ohne die Werkzeugliste zu ändern).
    Quelle: https://developers.openai.com/api/docs/guides/function-calling

24. **Strict-Modus bei OpenAI hat andere Regeln als bei Anthropic.** `strict: true` verlangt `additionalProperties: false` auf allen Objekten UND dass alle Felder in `required` stehen; optionale Felder werden über den Typ `null` modelliert.
    Quelle: https://developers.openai.com/api/docs/guides/function-calling

25. **OpenAI-Best-Practices für Funktionen:** ausführliche Beschreibungen, System-Prompt sagt wann welche Funktion; Enums gegen ungültige Zustände; Argumente, die man schon kennt, nicht vom Modell füllen lassen; aufeinanderfolgende Operationen in einer Funktion bündeln; unter 20 Funktionen anfangs.
    Quelle: https://developers.openai.com/api/docs/guides/function-calling

26. **Responses API ist die Empfehlung; Assistants-API ist abgeschaltet.** "While Chat Completions remains supported, Responses is recommended for all new projects." Die Assistants-API wurde am 26.08.2026 offiziell abgeschaltet. Responses unterscheidet Items (Funktionsaufruf und -ausgabe getrennt, verknüpft über `call_id`), Chat Completions packt beides in Nachrichten. OpenAI nennt für Responses 40 bis 80 Prozent bessere Cache-Nutzung (interne Tests, Herstellerangabe).
    Quelle: https://developers.openai.com/api/docs/guides/migrate-to-responses

27. **Function Calling versus Structured Outputs.** OpenAI: Werkzeuge/Funktionen/Daten anbinden, dann Function Calling; Antwort an den Nutzer strukturieren, dann `text.format` mit `type: "json_schema"` und `strict: true`. Safety-Ablehnungen erscheinen als `refusal`-Feld statt Schema-konformer Ausgabe.
    Quelle: https://developers.openai.com/api/docs/guides/structured-outputs

### D. Model Context Protocol (MCP)

28. **Aktuelle Spezifikationsversion ist 2026-07-28** (GitHub-Release 28.07.2026; Vorgänger 2025-11-25, 2025-06-18, 2025-03-26). Nutzt JSON-RPC 2.0, Hosts/Clients/Server, Primitive Tools, Resources, Prompts (Server) und Elicitation (Client).
    Quellen: https://modelcontextprotocol.io/specification/latest und `gh api repos/modelcontextprotocol/modelcontextprotocol/releases` (Tags `2026-07-28`, `2025-11-25`, `2025-06-18`, `2025-03-26`)

29. **Große Änderung in 2026-07-28: MCP ist zustandslos.** Handshake `initialize`/`notifications/initialized` und der Header `Mcp-Session-Id` entfallen. Jede Anfrage trägt Protokollversion und Client-Fähigkeiten in `_meta` (`io.modelcontextprotocol/protocolVersion`, `.../clientCapabilities`). Neu: Pflicht-RPC `server/discover`. Entfallen: `ping`, `logging/setLevel`, SSE-Wiederaufnahme (`Last-Event-ID`). Alle Ergebnisse haben ein Pflichtfeld `resultType` (`"complete"` oder `"input_required"`). Server-initiierte Anfragen laufen über das Muster "Multi Round-Trip Requests". Tasks sind eine Extension (`io.modelcontextprotocol/tasks`).
    Quelle: https://modelcontextprotocol.io/specification/2026-07-28/changelog

30. **Veraltet markiert (Deprecated) in 2026-07-28:** Roots, Sampling, Logging; HTTP+SSE-Transport (zugunsten von Streamable HTTP); Dynamic Client Registration (zugunsten von Client ID Metadata Documents). Mindestens 12 Monate Auslauffrist. Für neue Implementierungen heißt das: Sampling nicht mehr einbauen, direkt mit dem LLM-Anbieter sprechen.
    Quelle: https://modelcontextprotocol.io/specification/2026-07-28/changelog

31. **Tools in MCP.** `tools/list` und `tools/call`. Werkzeug hat `name`, optional `title`, `description`, `inputSchema` (JSON Schema, Standard 2020-12), optional `outputSchema`, `annotations`. Ergebnis: `content` (text, image, audio, resource_link, embedded resource), optional `structuredContent` (beliebiger JSON-Wert), `isError`. Namen: 1 bis 128 Zeichen, erlaubt A-Z a-z 0-9 `_` `-` `.`. Server SOLLEN Tools deterministisch sortieren (Caching, Prompt-Cache-Treffer).
    Quellen: https://modelcontextprotocol.io/specification/2026-07-28/server/tools und https://modelcontextprotocol.io/specification/2026-07-28/changelog

32. **Zwei Fehlerarten in MCP.** Protokollfehler (unbekanntes Tool, kaputte Anfrage; JSON-RPC-`error`, z. B. Code -32602) versus Tool-Ausführungsfehler (API-Fehler, Eingabevalidierung, Geschäftslogik) als normales Ergebnis mit `isError: true`. Clients SOLLEN Ausführungsfehler dem Modell zeigen, damit es sich korrigieren kann. Das entspricht dem `is_error`-Muster der Anthropic-API.
    Quelle: https://modelcontextprotocol.io/specification/2026-07-28/server/tools (Abschnitt "Error Handling")

33. **Zustand ohne Sitzung.** Ohne Protokoll-Session sollen Server Zustand über explizite Handles abbilden (Beispiel `create_basket` liefert `basket_id`, spätere Aufrufe nehmen sie als Argument). Das Handle ist ein Name, keine Berechtigung; bei jedem Aufruf Autorisierung prüfen; Lebensdauer in der Tool-Beschreibung nennen.
    Quelle: https://modelcontextprotocol.io/specification/2026-07-28/server/tools (Abschnitt "Stateful Tools", nicht normativ)

34. **Sicherheitspflichten.** Server MÜSSEN Eingaben validieren, Zugriffskontrolle umsetzen, Aufrufe ratenbegrenzen, Ausgaben bereinigen. Clients SOLLEN bei sensiblen Aktionen nachfragen, Tool-Eingaben dem Nutzer zeigen, Ergebnisse vor Weitergabe an das LLM prüfen, Timeouts setzen, protokollieren. Tool-Annotationen sind nicht vertrauenswürdig, außer der Server ist es. Es SOLL immer ein Mensch die Möglichkeit haben, Aufrufe abzulehnen.
    Quellen: https://modelcontextprotocol.io/specification/2026-07-28/server/tools und https://modelcontextprotocol.io/specification/latest

35. **Transporte.** Stdio (lokal, ein Client) und Streamable HTTP (remote, viele Clients, OAuth empfohlen). Typisch: lokaler Server per Stdio, Dienst-Anbieter (z. B. Sentry) per Streamable HTTP.
    Quelle: https://modelcontextprotocol.io/docs/learn/architecture

36. **MCP-Clients bei vielen Tools: Progressive Discovery.** Statt alle Definitionen zu laden, ein `search_tools`-Meta-Werkzeug (Katalog, Inspect, Execute). Schwelle als Anteil am Kontextfenster (Vorschlag 1 bis 5 Prozent). Hinzufügen/Entfernen von Tools mitten im Gespräch macht den Prompt-Cache ungültig. Zweites Muster: "Code Mode" (Modell schreibt Code in Sandbox, ruft Tools programmatisch auf).
    Quelle: https://modelcontextprotocol.io/docs/develop/clients/client-best-practices

37. **Claude-API-Anbindung an MCP ("MCP connector", Beta).** Beta-Header `mcp-client-2025-11-20` (Vorversion `mcp-client-2025-04-04` veraltet), nicht ZDR-fähig, nur Claude API / Claude Platform on AWS / Microsoft Foundry (nicht Bedrock, nicht Google Cloud). Einschränkung: Server muss öffentlich per HTTP (Streamable HTTP oder SSE) erreichbar sein; lokale Stdio-Server gehen nicht direkt. Dafür eigenen MCP-Client mit SDK-Helfern nutzen.
    Quelle: https://platform.claude.com/docs/en/agents-and-tools/mcp-connector

### E. SDK-Stände (Stichtag 04.10.2026)

38. **MCP TypeScript SDK: v2 ist die aktuelle Linie.** `@modelcontextprotocol/server` 2.3.0 und `@modelcontextprotocol/client` 2.3.0 (veröffentlicht 02.10.2026); die alte Einzelpaket-Linie `@modelcontextprotocol/sdk` steht bei 1.32.0 (ebenfalls 02.10.2026, npm-Tag `latest`). v1.x bekommt laut README mindestens 6 Monate nach dem v2-Release Bugfixes. Schemas über Standard Schema (Zod v4, Valibot, ArkType). Node.js 20 oder neuer. Server-Beispiel: `new McpServer({name, version})`, `server.registerTool('greet', {description, inputSchema: z.object(...)}, async (args) => ({content:[{type:'text', text}]}))`, Transport `StdioServerTransport` aus `@modelcontextprotocol/server/stdio`.
    Quellen: https://github.com/modelcontextprotocol/typescript-sdk , `npm view @modelcontextprotocol/server version` (2.3.0), `npm view @modelcontextprotocol/sdk version` (1.32.0), https://github.com/modelcontextprotocol/typescript-sdk/blob/main/docs/migration/upgrade-to-v2.md

39. **TS-SDK v2 spricht standardmäßig NICHT 2026-07-28.** Laut Migrationsanleitung legt v2 von sich aus kein 2026-07-28-Byte auf die Leitung; ein von Hand gebauter `Client`/`Server`/`McpServer` bleibt beim 2025er-Protokoll. 2026-07-28 ist explizites Opt-in. Es gibt ein Codemod: `npx @modelcontextprotocol/codemod@latest v1-to-v2 .`
    Quelle: https://github.com/modelcontextprotocol/typescript-sdk/blob/main/docs/migration/support-2026-07-28.md

40. **MCP Kotlin SDK: Version 0.15.0** (28.07.2026), Maven `io.modelcontextprotocol:kotlin-sdk`, gepflegt "in collaboration with JetBrains", Kotlin Multiplatform (JVM, Native, JS, Wasm). Transporte: STDIO, Streamable HTTP, SSE, WebSocket, ChannelTransport (nur Tests). Der Quellcode setzt `LATEST_PROTOCOL_VERSION = "2025-11-25"`; unterstützt `2025-11-25`, `2025-06-18`, `2025-03-26`, `2024-11-05`, also NICHT 2026-07-28. Beispiel: `Server(serverInfo = Implementation(...))`, `server.addTool(name, description) { CallToolResult(content = listOf(TextContent(...))) }`, Client `Client(...)`, `StreamableHttpClientTransport`, `client.connect`, `client.listTools()`. Release 0.15.0 enthält Breaking Changes: nebenläufige Verarbeitung eingehender Nachrichten, doppelte Tool-Namen werfen `IllegalArgumentException`. Die Versionsnummer 0.x deutet auf noch nicht stabile API hin.
    Quellen: https://github.com/modelcontextprotocol/kotlin-sdk , https://github.com/modelcontextprotocol/kotlin-sdk/releases/tag/0.15.0 , Quellcode `kotlin-sdk-core/src/commonMain/kotlin/io/modelcontextprotocol/kotlin/sdk/types/common.kt` (Zeilen 12 bis 22)

41. **Anthropic und OpenAI haben keine offiziellen Kotlin-SDKs.** Anthropic nennt offizielle Client-SDKs für Python, TypeScript, C#, Go, Java, PHP, Ruby (kein Kotlin). Das Java-SDK ist auf GitHub `anthropics/anthropic-sdk-java` (Release v2.68.0, 30.09.2026, Java 8 oder neuer) und lässt sich aus Kotlin nutzen. Für OpenAI existiert `openai/openai-java` (Release v4.76.0, 04.10.2026); dessen README enthält Kotlin-Beispiele. `anthropics/anthropic-sdk-kotlin` und `openai/openai-kotlin` liefern 404. Ob eines davon auf Android mit minSdk-Anforderungen einsetzbar ist, wurde NICHT geprüft.
    Quellen: https://platform.claude.com/docs/en/api/client-sdks (leitet auf die SDK-Übersicht), `gh api repos/anthropics/anthropic-sdk-java/releases`, `gh api repos/openai/openai-java/releases`, README beider Repos

42. **Weitere Pakete (npm, Stichtag 04.10.2026):** `@anthropic-ai/sdk` 0.131.0, `openai` 7.28.0.
    Quelle: `npm view <paket> version`

43. **Kurzvergleich Anthropic gegen OpenAI (Lernkurs-Tabelle):**
    | Aspekt | Anthropic Messages API | OpenAI Responses API |
    |---|---|---|
    | Werkzeugdefinition | `name`, `description`, `input_schema` | `type:"function"`, `name`, `description`, `parameters`, `strict` |
    | Aufruf vom Modell | Block `tool_use` mit `id`, `name`, `input` (Objekt) | Item `function_call` mit `call_id`, `name`, `arguments` (String) |
    | Ergebnis zurück | Block `tool_result` mit `tool_use_id`, in `user`-Nachricht | Item `function_call_output` mit `call_id`, `output` (String) |
    | Fehlerkennzeichnung | `is_error: true` | kein eigenes Feld, Fehlertext im `output`-String |
    | Schleifen-Signal | `stop_reason: "tool_use"` | Output enthält `function_call`-Items |
    | Parallel abschalten | `disable_parallel_tool_use: true` in `tool_choice` | `parallel_tool_calls: false` |
    | Strict | `strict: true`, `additionalProperties: false` | `strict: true`, alle Felder `required`, `additionalProperties: false` |
    Quellen: die oben genannten Doku-Seiten (Punkte 1, 6, 9, 22 bis 24). Zeile "Fehlerkennzeichnung OpenAI": aus der OpenAI-Doku nur abgeleitet ("Function outputs should be strings"), keine ausdrückliche Aussage zu einem Fehlerfeld gefunden.

## Beste Quellen

| Titel | Herausgeber | URL | Datum/Stand | Wofür | Vertrauen |
|---|---|---|---|---|---|
| Tool use with Claude (Overview) | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview | abgerufen 04.10.2026 | Gesamtbild, vollständige Runde mit Code in TS | offiziell |
| Define tools | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools | abgerufen 04.10.2026 | Schema, Beschreibungsregeln, `tool_choice`, Einschränkungen je Modell | offiziell |
| Handle tool calls | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls | abgerufen 04.10.2026 | `tool_result`-Format, `is_error`, 400-Fehler | offiziell |
| Parallel tool use | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/tool-use/parallel-tool-use | abgerufen 04.10.2026 | Parallelität, Ergebnisreihenfolge, Troubleshooting | offiziell |
| Handling stop reasons | Anthropic | https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons | abgerufen 04.10.2026 | Alle `stop_reason`-Werte für die Agentenschleife | offiziell |
| Strict tool use | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/tool-use/strict-tool-use | abgerufen 04.10.2026 | Garantierte Schema-Treue | offiziell |
| Tool runner (SDK) | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-runner | abgerufen 04.10.2026, Beta | Schleife automatisieren, `betaZodTool` | offiziell |
| Tool search tool | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool | abgerufen 04.10.2026 | Viele Werkzeuge, `defer_loading` | offiziell |
| MCP connector | Anthropic | https://platform.claude.com/docs/en/agents-and-tools/mcp-connector | abgerufen 04.10.2026, Beta `mcp-client-2025-11-20` | Remote-MCP direkt aus der Messages API | offiziell |
| Writing effective tools for agents | Anthropic Engineering | https://www.anthropic.com/engineering/writing-tools-for-agents | 11.09.2025 | Werkzeugdesign, Namespacing, Rückgaben, Fehlertexte | offiziell |
| Building effective agents | Anthropic Engineering | https://www.anthropic.com/engineering/building-effective-agents | 19.12.2024 | Workflows vs. Agenten, ACI, Poka-yoke | offiziell |
| Code execution with MCP | Anthropic Engineering | https://www.anthropic.com/engineering/code-execution-with-mcp | 04.11.2025 | Grenzen vieler MCP-Tools, Code-Muster | offiziell |
| Advanced tool use | Anthropic Engineering | https://www.anthropic.com/engineering/advanced-tool-use | 24.11.2025 | Tool Search, programmatische Aufrufe, Beispiele (Herstellerzahlen) | offiziell |
| Function calling (Responses API) | OpenAI | https://developers.openai.com/api/docs/guides/function-calling | abgerufen 04.10.2026 (alte URL platform.openai.com leitet per 301 hierher) | Feldnamen und Regeln der Responses-API | offiziell |
| Migrate to the Responses API | OpenAI | https://developers.openai.com/api/docs/guides/migrate-to-responses | abgerufen 04.10.2026 | Warum Responses, Assistants-Abschaltung 26.08.2026 | offiziell |
| Structured Outputs | OpenAI | https://developers.openai.com/api/docs/guides/structured-outputs | abgerufen 04.10.2026 | Abgrenzung zu Function Calling | offiziell |
| MCP Specification (latest = 2026-07-28) | Model Context Protocol Project | https://modelcontextprotocol.io/specification/latest | 2026-07-28 | Autoritative Protokollanforderungen | offiziell |
| MCP Key Changes (Changelog 2026-07-28) | MCP Project | https://modelcontextprotocol.io/specification/2026-07-28/changelog | 2026-07-28 | Unterschiede zu 2025-11-25 (zustandslos, Deprecations) | offiziell |
| MCP Spec: Tools | MCP Project | https://modelcontextprotocol.io/specification/2026-07-28/server/tools | 2026-07-28 | Tool-Definition, Ergebnisse, Fehler, Sicherheit | offiziell |
| MCP Architecture overview | MCP Project | https://modelcontextprotocol.io/docs/learn/architecture | abgerufen 04.10.2026 | Teilnehmer, Schichten, Transporte, Beispiel-Nachrichten | offiziell |
| MCP Client Best Practices | MCP Project | https://modelcontextprotocol.io/docs/develop/clients/client-best-practices | abgerufen 04.10.2026 | Progressive Discovery, Code Mode, Prompt-Cache | offiziell |
| MCP TypeScript SDK (Repo, Releases, Migration) | MCP Project | https://github.com/modelcontextprotocol/typescript-sdk | v2.3.0 und 1.32.0 vom 02.10.2026 | Installation, Beispiel, v1-zu-v2-Migration | offiziell |
| MCP Kotlin SDK (Repo, Releases) | MCP Project in Zusammenarbeit mit JetBrains | https://github.com/modelcontextprotocol/kotlin-sdk | 0.15.0 vom 28.07.2026 | Kotlin-Beispiele, Transporte, Breaking Changes | offiziell |
| anthropic-sdk-java / openai-java | Anthropic / OpenAI | https://github.com/anthropics/anthropic-sdk-java , https://github.com/openai/openai-java | v2.68.0 (30.09.2026) / v4.76.0 (04.10.2026) | Nutzbar aus Kotlin, da keine Kotlin-SDKs existieren | offiziell |

Hinweis: Unter "extern" wurde nichts aufgenommen. Praktiker-Blogs wurden nicht geprüft, weil die offiziellen Quellen für alle Punkte reichten. Wer eine unabhängige Gegenprobe zu den Herstellerzahlen (Punkt 20, 21, 26) will, muss gesondert recherchieren.

## Offen oder widersprüchlich

1. **MCP-Stand: Spezifikation gegen SDKs.** Die Spezifikation 2026-07-28 ist zustandslos (kein `initialize`, keine Sessions). Die README des TS-SDK sagt, v2 "implementiert" die 2026-07-28-Spezifikation, die Migrationsanleitung sagt dagegen, 2026-07-28 sei Opt-in und Standard bleibe das 2025er-Protokoll. Das Kotlin-SDK 0.15.0 unterstützt laut Quellcode nur bis 2025-11-25 (und der Release-Text spricht weiter von einem "initialization handshake"). Folge für den Kurs: Praktisch lernt man heute das Modell mit Handshake und Sitzungen (2025-11-25), obwohl die neueste Spezifikation anders aussieht. Die Wirkung beim Verbinden (ein 2026-Client gegen einen 2025-Server und umgekehrt) wurde nicht praktisch getestet.
2. **Kotlin-SDK auf Android.** Die README erwähnt Android nicht (nur JVM, Native, JS, Wasm). Ob ein MCP-Client in einer Android-App mit diesem SDK läuft (minSdk, Ktor-Engine, Stdio nicht verfügbar), ist nicht belegt.
3. **Kotlin-SDK für die Modellaufrufe.** Weder Anthropic noch OpenAI bieten ein Kotlin-SDK. Ob man im Kurs das Java-SDK nutzt oder die HTTP-API direkt mit Ktor/kotlinx.serialization aufruft, ist eine Entscheidung für den Kurs, nicht eine Faktenfrage. Für Android ist die Java-SDK-Eignung ungeprüft.
4. **Forced Tool Use.** Die Doku nennt Einschränkungen für bestimmte Modelle (Opus 5.5, Sonnet 5.5, Fable 5.1, Mythos 5.1), aber die Überblicksseite zeigt weiter Beispiele mit `claude-opus-5-5`. Die Doku-Beispiele sind in sich nicht widersprüchlich (sie nutzen dort `auto`), Lernende könnten aber `tool_choice: tool` aus älteren Tutorials kopieren (siehe Bug-Kandidat 1).
5. **Herstellerzahlen.** Genauigkeits- und Token-Zahlen (Tool Search, Programmatic Tool Calling, 98,7 Prozent bei Code Execution mit MCP, 40 bis 80 Prozent Cache bei Responses) stammen jeweils vom Hersteller und sind Beispielrechnungen oder interne Tests. Nicht unabhängig bestätigt.
6. **Zeitlose Regel "wenige starke Werkzeuge".** Anthropic rät zu wenigen, gebündelten Werkzeugen (Punkt 10, 15). Gleichzeitig empfiehlt Anthropic bei sehr vielen Werkzeugen Tool Search und MCP-Server mit vielen Tools. Das ist kein Widerspruch, sondern eine Abwägung (Auswahlgenauigkeit leidet ab ca. 30 bis 50 Tools), sollte im Kurs aber als solche erklärt werden.
7. **"Wann MCP statt eigener Werkzeuge?"** Eine ausdrückliche, offizielle Entscheidungsregel wurde nicht gefunden. Belegt ist nur: MCP standardisiert die Anbindung (ein Server für viele Hosts, z. B. Sentry per Streamable HTTP, Claude Code als Host); eigene Werkzeuge im Prozess sind einfacher und brauchen keinen Server (Tool Runner, `tools` im Request). Die Empfehlung "eigene Werkzeuge für die eigene App, MCP wenn ein Werkzeug von mehreren Hosts oder von Dritten genutzt werden soll" ist Schlussfolgerung aus der Architektur-Doku, keine zitierbare Aussage.
8. **OpenAI-Fehlerfeld.** In der OpenAI-Doku wurde kein eigenes Fehlerfeld für `function_call_output` gefunden; der Fehlertext geht in `output`. Ob das Responses-API-Schema ein Feld wie `status` für Funktionsausgaben vorsieht, wurde nicht in der API-Referenz geprüft.
9. **TS SDK Tool Runner und MCP.** Die Anthropic-Doku erwähnt SDK-Helfer zur Umwandlung zwischen MCP-Typen und Claude-API-Typen (Zeilen im MCP-connector-Dokument). Details für TypeScript wurden nicht im Einzelnen gelesen.

## BEST-PRACTICES-KANDIDATEN:

1. **Agentenschleife mit Tool Use (Anthropic Messages API, TypeScript).** Stand 04.10.2026. Schleife: `messages.create` mit `tools`; bei `stop_reason === "tool_use"` alle `tool_use`-Blöcke ausführen (parallel, wenn unabhängig und nur lesend), pro Block ein `tool_result` mit gleicher `tool_use_id` in EINER `user`-Nachricht, `tool_result` zuerst, Fehler mit `is_error: true` und handlungsleitendem Text; Schleife bis `end_turn`; `max_tokens`, `pause_turn`, `refusal`, `model_context_window_exceeded` behandeln; Iterationslimit setzen. Anthropic-API-Version `2023-06-01`, `@anthropic-ai/sdk` 0.131.0.
   URLs: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls , https://platform.claude.com/docs/en/agents-and-tools/tool-use/parallel-tool-use , https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons
2. **Werkzeugdesign.** Wenige gebündelte Werkzeuge pro Workflow, Namespacing, ausführliche Beschreibungen (3 bis 4 Sätze Minimum, wann nutzen und wann nicht), eindeutige Parameternamen, Enums gegen ungültige Zustände, `strict: true` mit `additionalProperties: false`, optionale `input_examples`, Rückgaben kurz und semantisch (keine UUIDs), `response_format` concise/detailed, Pagination mit Standardwerten, Fehlertexte handlungsleitend, Ergebnisse als untrusted behandeln. Quellen Stand 11.09.2025 (Engineering-Blog) und 04.10.2026 (Doku).
   URLs: https://www.anthropic.com/engineering/writing-tools-for-agents , https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools , https://platform.claude.com/docs/en/agents-and-tools/tool-use/strict-tool-use
3. **OpenAI Responses API, Function Calling (TypeScript/Kotlin).** Flache Werkzeugdefinition mit `strict: true` (alle Felder `required`, optionale Felder als `null`-Typ, `additionalProperties: false`); `function_call` verarbeiten (`arguments` ist ein JSON-String, erst parsen), `function_call_output` mit gleichem `call_id`. Responses empfohlen, Assistants-API seit 26.08.2026 abgeschaltet. SDK-Stand: `openai` (npm) 7.28.0, `openai-java` v4.76.0.
   URLs: https://developers.openai.com/api/docs/guides/function-calling , https://developers.openai.com/api/docs/guides/migrate-to-responses
4. **MCP-Server und -Client mit den SDKs.** TypeScript: v2-Linie `@modelcontextprotocol/server` / `@modelcontextprotocol/client` 2.3.0 (Node 20+, Standard Schema/Zod v4); Zustand über explizite Handles; Tool-Fehler mit `isError: true`; pro Anfrage einen Server und Transport erzeugen (laut v2.3.0-Hinweis); `maxToolInputElements` und `expectedResource` (Token-Audience) aktivieren; Eingaben validieren, Aufrufe ratenbegrenzen. Kotlin: `io.modelcontextprotocol:kotlin-sdk` 0.15.0, Protokoll 2025-11-25. Spezifikation 2026-07-28 (zustandslos, `server/discover`, `resultType`). Bei Remote-Server: Streamable HTTP mit OAuth; HTTP+SSE meiden (deprecated).
   URLs: https://modelcontextprotocol.io/specification/2026-07-28/server/tools , https://modelcontextprotocol.io/specification/2026-07-28/changelog , https://github.com/modelcontextprotocol/typescript-sdk/releases , https://github.com/modelcontextprotocol/kotlin-sdk/releases/tag/0.15.0
5. **Viele Werkzeuge skalieren.** Ab etwa 10 Werkzeugen oder 10k Token Definitionen: Tool Search (`defer_loading: true`, `tool_search_tool_bm25_20251119` oder `..._regex_...`), 3 bis 5 Häufigste nicht verzögern; MCP-Hosts: Progressive Discovery mit Schwelle 1 bis 5 Prozent des Kontextfensters; Tool-Reihenfolge deterministisch halten (Prompt-Cache); Code-Mode erst mit Sandbox.
   URLs: https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool , https://modelcontextprotocol.io/docs/develop/clients/client-best-practices

## BUG-KANDIDATEN:

1. **`tool_choice: any` oder `tool` ergibt HTTP 400.**
   Symptom: Anfrage mit erzwungenem Werkzeug schlägt mit 400 fehl ("Forced tool use not supported"), obwohl derselbe Code früher lief.
   Ursache: Bei Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 und Claude Mythos 5.1 sowie bei manuellem Extended Thinking (`thinking: {type: "enabled"}`) sind `any`/`tool` nicht unterstützt.
   Abhilfe: `auto` mit `strict: true` oder Structured Outputs; Prompt steuert die Auswahl.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools (Abschnitt "Forcing tool use", Tabelle)

2. **400-Fehler "tool_use ids were found without tool_result blocks immediately after".**
   Symptom: Antwort 400 beim Zurückschicken des Ergebnisses.
   Ursache: `tool_result` nicht direkt nach dem `tool_use`, Text vor dem `tool_result` im Content-Array, oder Nachricht dazwischen. Auch: Text nach `tool_result` kann zu leeren Modellantworten führen (Stop-Reason-Seite führt das als Pitfall). Bei gemischten Client- und Server-Tools darf die `user`-Nachricht nur `tool_result`-Blöcke enthalten.
   Abhilfe: `tool_result`-Blöcke zuerst, direkt nach der Assistenten-Nachricht; keinen Zusatztext beifügen.
   Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls , https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons

3. **Claude ruft Werkzeuge nicht mehr parallel auf.**
   Symptom: Nur noch ein `tool_use` je Antwort, obwohl mehrere unabhängige Aufrufe sinnvoll wären.
   Ursache: Ergebnisse parallel erzeugter Aufrufe wurden in getrennten `user`-Nachrichten zurückgegeben; das "lehrt" das Modell sequenzielles Verhalten.
   Abhilfe: Alle `tool_result`-Blöcke in einer einzigen `user`-Nachricht; Prompt-Hinweis zu parallelen Aufrufen.
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/parallel-tool-use (Abschnitt "Troubleshooting")

4. **Abgeschnittene `tool_use`-Eingabe bei `max_tokens`.**
   Symptom: `stop_reason: "max_tokens"`, letzter Block ist ein unvollständiger `tool_use`; Parser bricht oder Aufruf hat fehlende Argumente.
   Ursache: Ausgabelimit erreicht, bevor die Werkzeugargumente fertig sind.
   Abhilfe: Anfrage mit höherem `max_tokens` wiederholen, unvollständige Aufrufe nie ausführen.
   Quelle: https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons

5. **Modell rät fehlende Pflichtparameter.**
   Symptom: `tool_use` enthält erfundene Werte (z. B. `"New York, NY"`), obwohl der Nutzer nichts angegeben hat; tritt bei Sonnet eher auf als bei Opus.
   Ursache: Modellverhalten bei mehrdeutigen Prompts, nicht garantiert.
   Abhilfe: Beschreibung präzisieren, im Prompt Rückfragen verlangen, Eingaben im Code validieren, `strict: true` löst das Raten NICHT (es sichert nur Typen).
   Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview (Accordion "When required parameters are missing")

6. **Server-Tool-`srvtoolu_`-ID bekommt ein `tool_result`.**
   Symptom: 400 bei Fortsetzung, wenn Tool Search oder `web_search` im Spiel sind.
   Ursache: Für Server-Tools (ID beginnt mit `srvtoolu_`) darf man kein `tool_result` zurückgeben; `server_tool_use`- und `tool_search_tool_result`-Blöcke müssen unverändert im Verlauf bleiben. Außerdem: nicht alle Tools mit `defer_loading: true` markieren (400) und kein `cache_control` an verzögerten Tools.
   Abhilfe: Nur `tool_result` für eigene Client-Tools senden; Assistenten-Inhalt unverändert zurückschicken; mindestens ein Tool nicht verzögern.
   Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool , https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls

7. **OpenAI strict-Schema wird abgelehnt oder optionale Felder fehlen.**
   Symptom: Schema-Fehler bei `strict: true`, oder Felder, die "optional" sein sollten, kommen immer.
   Ursache: Bei OpenAI müssen alle Felder `required` sein und `additionalProperties: false` gelten; Optionalität geht nur über `null` als Typ. Das Anthropic-Schema (Pflichtfelder wirklich optional) lässt sich nicht 1:1 übernehmen.
   Abhilfe: Schemas pro Anbieter getrennt erzeugen.
   Quelle: https://developers.openai.com/api/docs/guides/function-calling (Abschnitt Strict Mode)

8. **OpenAI: `arguments` ist ein String, kein Objekt.**
   Symptom: Zugriff auf Felder liefert `undefined`/Typfehler; Anthropic liefert dagegen `input` als Objekt.
   Ursache: `function_call.arguments` ist ein JSON-kodierter String; er muss geparst werden (und bei Nicht-strict-Betrieb validiert).
   Abhilfe: `JSON.parse` mit Fehlerbehandlung; ungültiges JSON als Fehlertext in `function_call_output` zurückgeben.
   Quelle: https://developers.openai.com/api/docs/guides/function-calling

9. **Alte OpenAI-Pfade nicht mehr nutzbar.**
   Symptom: Aufrufe der Assistants-API schlagen fehl; Tutorials zeigen `tool`-Rolle und Chat-Completions-Format.
   Ursache: Assistants-API seit 26.08.2026 abgeschaltet; Chat Completions bleibt unterstützt, aber das Funktionsformat ist ein anderes (extern getaggt, Funktionsaufruf in Nachricht statt eigenes Item).
   Abhilfe: Responses API nutzen.
   Quelle: https://developers.openai.com/api/docs/guides/migrate-to-responses

10. **MCP: Ein `McpServer`/Transport für mehrere Anfragen geteilt (TS-SDK 2.x).**
    Symptom: `Server.connect()` wird abgelehnt, solange die Instanz verbunden ist; Fehler bei zustandslosem Streamable HTTP unter Last.
    Ursache: Seit v2.3.0 darf eine Instanz nicht für mehrere Anfragen geteilt werden; ein zustandsloser Transport bearbeitet genau eine Anfrage.
    Abhilfe: `McpServer` und Transport pro Anfrage im Handler (oder in der `createMcpHandler`-Fabrik) erzeugen (billig seit PR #2889).
    Quelle: https://github.com/modelcontextprotocol/typescript-sdk/releases (v2.3.0, "Upgrade notes")

11. **MCP Kotlin 0.15.0: doppelter Tool-Name wirft Ausnahme.**
    Symptom: `IllegalArgumentException` beim zweiten `addTool` mit gleichem Namen (früher stilles Überschreiben).
    Ursache: Breaking Change in 0.15.0; Duplikate werden bei der Registrierung abgelehnt.
    Abhilfe: zuerst `server.removeTool("name")`, dann `addTool`. Außerdem laufen eingehende Nachrichten jetzt nebenläufig (`handlerCoroutineContext`, Standard `Dispatchers.Default`); `RequestHandlerExtra` ist nicht mehr selbst konstruierbar, Zugriff über `currentRequestHandlerExtra()`.
    Quelle: https://github.com/modelcontextprotocol/kotlin-sdk/releases/tag/0.15.0

12. **MCP: alte Tutorials (v1-SDK, Sessions) passen nicht zu 2026-07-28.**
    Symptom: Code aus Tutorials mit `@modelcontextprotocol/sdk`, `initialize`-Handshake oder `Mcp-Session-Id` verhält sich anders als die aktuelle Spezifikation; Importpfade `@modelcontextprotocol/sdk/...` existieren in v2 nicht.
    Ursache: Paket in `@modelcontextprotocol/server`/`client`/`core` aufgeteilt; Spezifikation 2026-07-28 ohne Sitzungen und Handshake; SDKs sprechen 2026 nur per Opt-in.
    Abhilfe: Für neue Projekte v2 nehmen, Codemod `npx @modelcontextprotocol/codemod@latest v1-to-v2 .` nutzen, Protokollstand bewusst wählen.
    Quellen: https://github.com/modelcontextprotocol/typescript-sdk/blob/main/docs/migration/upgrade-to-v2.md , https://modelcontextprotocol.io/specification/2026-07-28/changelog

13. **MCP-Connector der Claude-API erreicht lokalen Stdio-Server nicht.**
    Symptom: Verbindung zu lokalem MCP-Server über `mcp_servers` in der Messages API gelingt nicht.
    Ursache: Der Connector braucht einen öffentlich per HTTP erreichbaren Server (Streamable HTTP oder SSE); Beta-Header `mcp-client-2025-11-20` nötig.
    Abhilfe: Eigener MCP-Client mit SDK; Ergebnisse selbst in `tool_use`/`tool_result` übersetzen (SDK-Helfer vorhanden).
    Quelle: https://platform.claude.com/docs/en/agents-and-tools/mcp-connector (Abschnitt "Limitations")

14. **Hinzufügen oder Entfernen von Tools mitten im Gespräch entwertet den Prompt-Cache.**
    Symptom: Plötzlich höhere Kosten und Latenz, obwohl weniger Tool-Definitionen geladen sind.
    Ursache: Präfix-Cache umfasst das `tools`-Array; Änderung (auch Neusortierung) macht Treffer ungültig. Außerdem entwertet geändertes `tool_choice` gecachte Nachrichtenblöcke.
    Abhilfe: Neue Definitionen nach dem Cache-Breakpoint anhängen, Tool-Reihenfolge deterministisch halten, `tool_choice` nicht ständig wechseln, oder einen stabilen `call_tool({name,args})`-Meta-Aufruf.
    Quellen: https://modelcontextprotocol.io/docs/develop/clients/client-best-practices (Abschnitt "Interaction with Prompt Caching") , https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools
