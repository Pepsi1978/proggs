# MCP-Server-Bau — Best Practices (Stand 2026-06-03, MCP TS-SDK 1.27.1 im Projekt / 1.29.0 neueste, Spec 2025-11-25)

> **Die "richtige Seite der Medaille" zum Bug-Almanach [`~/proggs/bugs/claude-tooling/mcp-server.md`](../../bugs/claude-tooling/mcp-server.md).**
> Dort steht *was schiefgeht und wie man es umgeht* — hier steht *wie man einen MCP-Server
> von vornherein RICHTIG baut, damit der Bug gar nicht erst entsteht*. Die wechselseitige
> Abschnitts-Bezugstabelle steht unten ("Kopplung zum Bug-Almanach").
>
> **Abgrenzung (drei MCP-Dateien, klar getrennt):**
> - **Diese Datei** = **Server BAUEN** (Server-Autor-Seite, Projekt-Code) — Gegenstueck zu `bugs/claude-tooling/mcp-server.md`.
> - `best-practices/claude-tooling/mcp.md` = **Server KONFIGURIEREN/VERBINDEN in Claude Code** (Harness-Seite).
> - `bugs/claude-tooling/claude-hooks.md` = **Hooks** (MCP nur am Rand: Matcher, MCP-Tool-als-Hook).
>
> **Anker (live ermittelt 2026-06-03):** Projekt `~/proggs/mcp-code-search` nutzt
> `@modelcontextprotocol/sdk` **`^1.27.1`** (1.27.1 = 2026-02-24), **zod v4** (`^4.3.6`),
> **Bun 1.3.11**, **Node 24.15.0**, **TS 5.9.3**, ESM, stdio-Transport, lokaler Single-User-Server.
> Neueste SDK-Version: **1.29.0** (2026-03-30). Spec-Revision aktuell **2025-11-25**
> (auch 2025-06-18 / 2025-03-26 relevant). Einige Schema-/Error-Fixes landen erst in 1.28/1.29 —
> siehe Bug-Almanach Sektion 9.
>
> **Quellen-Rangordnung:** offiziell (modelcontextprotocol.io/specification + /docs,
> github.com/modelcontextprotocol, anthropic.com/engineering, code.claude.com/docs, zod.dev) =
> Grundwahrheit. Community/Blogs als `extern` gelabelt (sekundaer, ueberstimmen nie das Offizielle).
> Jeder nicht-triviale Eintrag traegt Quelle + Datum + `offiziell`/`extern`-Flag.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Transport waehlen | Lokal → stdio; Remote → Streamable HTTP; nie SSE | §A1 |
| 2 | Logging bei stdio | stdout NUR JSON-RPC; Logs auf stderr/logging-Capability | §D4 |
| 3 | Tool registrieren | `registerTool` statt deprecated `server.tool()` | §B1 |
| 4 | Tool-Beschreibung | Erklaeren wie einem Kollegen; jedes Feld `.describe()` | §B2, §B3 |
| 5 | Input-Schema | Top-Level flaches `z.object`; enums/Constraints; kein Union | §B4 |
| 6 | Tool-Name | Nur `[a-zA-Z0-9_]{1,64}`, snake_case, namespacen | §B6 |
| 7 | Wie viele Tools | Wenige Workflow-Tools statt API-1:1-Wrapper | §C1 |
| 8 | Grosse Antworten | Pagination/Limit; <25k Token (Claude Code kappt) | §C3 |
| 9 | Fehler propagieren | Fachlich → `isError:true`; Protokoll → `McpError`; nie leeres catch | §D1 |
| 10 | Prozess-Resilienz | `uncaughtException`/`unhandledRejection` loggen, nicht crashen | §D5 |
| 11 | Langlaufende Tools | `resetTimeoutOnProgress:true` explizit + Progress + maxTotalTimeout | §D7 |
| 12 | `.mcp.json` | Absolute Pfade; Windows `cmd /c … -y`; valide; Secrets `${VAR}` | §E2, §E8 |
| 13 | Lazy Init + Shutdown | Schwere Init lazy; SIGTERM/SIGINT-Cleanup awaiten | §A6, §A7 |
| 14 | Sicherheit | stdio: Secrets aus Umgebung; HTTP: OAuth2.1/PKCE/DNS-Rebinding aktiv | §F1, §F5 |
| 15 | Vor dem Client | Erst MCP Inspector testen; SDK exakt pinnen | §G1, §G5 |

---

## A. Transport-Wahl, Server-Architektur & Lifecycle

### A1. stdio vs Streamable HTTP — die Grundsatzentscheidung
- **Empfehlung:** Lokaler Single-User-Server, der nur vom Client (Claude Code/Desktop/Cursor) erreichbar sein soll → **stdio**. Remote, Multi-User, Cloud, mehrere gleichzeitige Clients → **Streamable HTTP** (ein einziger `/mcp`-Endpoint, POST+GET).
- **Vermeiden:** Fuer einen neuen Remote-Server **HTTP+SSE** (alter Zwei-Endpoint-Flow) waehlen — ist seit Spec 2025-03-26 deprecated und existiert nur fuer Rueckwaerts-Kompatibilitaet.
- **Warum:** stdio bindet den Zugriff implizit an genau den einen lokalen Client (keine Netzwerk-Angriffsflaeche). Streamable HTTP loest SSE ab und vereinfacht Infrastruktur (ein Endpoint, optionales Streaming).
- Quelle: [MCP Spec — Transports (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports) — 2025-06-18 — `offiziell`; [Why MCP deprecated SSE](https://blog.fka.dev/blog/2025-06-06-why-mcp-deprecated-sse-and-go-with-streamable-http/) — 2025-06-06 — `extern`

### A2. Stateless vs stateful (nur Streamable HTTP)
- **Empfehlung:** **Stateless** (pro Request frische Transport+Server-Instanz, `sessionIdGenerator: undefined`) fuer horizontal skalierbare/serverlose Deployments ohne geteilten Zustand. **Stateful** (echte UUID als `sessionIdGenerator`) wenn der Server Sitzungszustand braucht; dann Session in **shared storage** statt Sticky-Sessions (sonst Bruch bei Deploy/Failover).
- **Vermeiden:** stdio ueberhaupt "stateless/stateful" zu fragen — die Unterscheidung gilt nur fuer HTTP.
- **Warum:** Stateless skaliert trivial (jeder Request unabhaengig), kostet aber Re-Init-Overhead; stateful spart Re-Init, braucht aber Session-Persistenz.
- Quelle: [MCP Spec — Transports / Session Management (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports) — 2025-06-18 — `offiziell`; SDK `docs/server.md` (Stateless/Stateful-Beispiele) — 2026 — `extern`

### A3. McpServer (High-Level) vs Low-Level Server
- **Empfehlung:** Den High-Level **`McpServer`** mit `registerTool`/`registerResource`/`registerPrompt` nutzen — er uebernimmt Schema-Konvertierung, Capability-Deklaration und Routing korrekt. Nur fuer Sonderfaelle (eigene Handler, exotisches Routing) auf die Low-Level-`Server`-Klasse + `setRequestHandler` gehen.
- **Vermeiden:** Manuelle `setRequestHandler(ListTools)` ohne Not — dabei faellt die automatische zod→JSON-Schema-Konvertierung weg (`inputSchema` fehlt dann, Bug-Almanach 3.9).
- **Warum:** Der High-Level-Pfad ist der getestete Standard; manuelles Routing reproduziert leicht bekannte Fallen.
- Quelle: [MCP Docs — Build an MCP server](https://modelcontextprotocol.io/docs/develop/build-server) — 2026-06 — `offiziell`

### A4. Tools vs Resources vs Prompts — das richtige Primitive
- **Empfehlung:** **Tools** = Aktionen, **modell-gesteuert** (das LLM entscheidet, wann es aufruft) — z.B. "suche Code", "lege Datei an". **Resources** = Daten/Kontext, **applikations-/client-gesteuert** — z.B. Dateiinhalte, Schema-Dumps, die der Client gezielt lädt. **Prompts** = vom **User** ausgewaehlte Templates/Workflows (z.B. Slash-Command-artige Vorlagen).
- **Vermeiden:** Alles als Tool bauen. Wenn das LLM nur Daten lesen soll (kein Seiteneffekt), ist eine **Resource** oft passender (und billiger im Tool-Budget).
- **Warum:** Die drei Primitive haben unterschiedliche Control-Modelle; richtige Zuordnung verbessert Auffindbarkeit und reduziert die Tool-Liste.
- Quelle: [MCP Docs — Architecture / Concepts](https://modelcontextprotocol.io/docs/concepts/architecture) — 2026-06 — `offiziell`

### A5. Lifecycle/Handshake sauber
- **Empfehlung:** Vollstaendigen 3-Schritt-Handshake einhalten: `initialize`-Request → `initialize`-Response (mit Capabilities) → auf `notifications/initialized` warten; **erst danach** Tool-/Resource-Requests verarbeiten. Bei protocolVersion-Mismatch eine vom **Server** unterstuetzte Version zuruckgeben (nicht den Client-Wert blind echoen). Mit dem offiziellen SDK passiert das automatisch korrekt.
- **Vermeiden:** Proaktive Server-Requests vor `initialized`; hartkodierte protocolVersion.
- **Warum:** Verletzte Handshake-Reihenfolge oder Versions-Echo beendet die Verbindung sofort (Bug-Almanach 7.4/7.5).
- Quelle: [MCP Spec — Lifecycle (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle) — 2025-06-18 — `offiziell`

### A6. Schneller Startup — schwere Arbeit lazy
- **Empfehlung:** Tool-Definitionen als statische Konstante zur Import-Zeit (keine DB-/FS-Arbeit im `list_tools`-Handler). Schwere Initialisierung (DB oeffnen, Modelle/Embeddings laden) **lazy** beim ersten echten Tool-Call oder im Hintergrund nach `initialized` — **nicht** vor der `initialize`-Response.
- **Vermeiden:** DB/Connection/Modell synchron im Konstruktor oder vor dem Handshake laden.
- **Warum:** Langsamer Start ueberschreitet das Init-Fenster → der Server wird fuer die ganze Session als FAILED markiert (Bug-Almanach 7.3).
- **Anker (`mcp-code-search/src/index.ts`):** ✅ baut den `VectorStore` lazy beim ersten Call (`getStore`) — vorbildlich.
- Quelle: [MCP Spec — Lifecycle](https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle) — 2025-06-18 — `offiziell`

### A7. Graceful Shutdown
- **Empfehlung:** `SIGTERM`/`SIGINT`-Handler installieren, der `await server.close()` + Ressourcen-Cleanup (DB schliessen, WAL checkpointen, In-Flight-Writes abschliessen) **awaitet**, dann `process.exit(0)`. Watchdog-Timeout (5–10s), der notfalls `process.exit(1)` erzwingt.
- **Vermeiden:** Async-Cleanup nicht awaiten (Datenverlust/DB-Lock); Shutdown ohne Watchdog (haengt).
- **Warum:** stdio-Shutdown ist: Client schliesst stdin → wartet → SIGTERM → SIGKILL. Ohne sauberen Handler gehen Writes verloren (Bug-Almanach 7.7).
- **Anker:** `index.ts` hat SIGTERM/SIGINT → `closeCachedStore()` + `exit(0)` (synchroner Close, ok). Fuer schwere DB-Writes ggf. `await` + Watchdog ergaenzen.
- Quelle: [MCP Spec — Transports (stdio Shutdown)](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports) — 2025-06-18 — `offiziell`

---

## B. Tool-Schema-Design & Beschreibungen (Input/Output)

### B1. `registerTool` statt deprecated `server.tool()`
- **Empfehlung:** Tools mit `server.registerTool(name, config, handler)` registrieren — `config` buendelt `title`, `description`, `inputSchema`, `outputSchema`, `annotations` an einem Ort, typsicher, zukunftsfest.
- **Vermeiden:** Die alte positionale `server.tool(name, desc, schema, handler)`-Signatur (deprecated, kein sauberer Platz fuer outputSchema/annotations/title).
- **Warum:** `registerTool` ist die offiziell empfohlene API; nimmt jede Standard-Schema-Library (zod v4 unveraendert) und macht alle Metadaten explizit.
- **Anker:** `index.ts` nutzt noch die deprecated `server.tool(...)`-Form fuer alle drei Tools (`index_codebase`, `search_code`, `search_status`) → auf `registerTool` migrieren (Funktion bleibt identisch, Bug-Almanach 3.3).

```typescript
server.registerTool(
  "search_code",
  {
    title: "Semantic Code Search",
    description: "Search an indexed codebase with a natural-language query. Returns the most semantically similar code chunks. The directory MUST have been indexed via index_codebase first.",
    inputSchema: {
      query: z.string().describe("Natural-language query, e.g. 'database connection handling'"),
      directory: z.string().describe("Absolute path to the codebase root (must be indexed)"),
      limit: z.number().int().min(1).max(50).default(10).describe("Max results to return (default 10)"),
    },
    annotations: { readOnlyHint: true, openWorldHint: false },
  },
  async ({ query, directory, limit }) => { /* ... */ }
);
```
- Quelle: [typescript-sdk docs/server.md + Releases](https://github.com/modelcontextprotocol/typescript-sdk) — 2026 — `offiziell`; [SDK Issue #1284 (server.tool deprecated)](https://github.com/modelcontextprotocol/typescript-sdk/issues/1284) — 2025-12-11 — `offiziell`

### B2. Klare Tool-Beschreibungen — DER zentrale Best-Practice
- **Empfehlung:** `description` schreiben, wie man das Tool **einem neuen Teammitglied** erklaeren wuerde: impliziten Kontext explizit machen (spezielle Query-Formate, Nischen-Terminologie, Beziehungen zwischen Ressourcen, Grenzen zu anderen Tools, Vorbedingungen). Bei komplexen/verschachtelten Inputs konkrete Beispiel-Eingaben angeben.
- **Vermeiden:** Vage Zweck-Saetze; Domaenen-Konventionen als "selbstverstaendlich" voraussetzen; Mehrdeutigkeit darueber, was das Tool tut.
- **Warum:** Anthropic-Faustregel — so viel Aufwand in das **Agent-Computer-Interface (ACI)** stecken wie in ein Human-Computer-Interface. "Put yourself in the model's shoes." Schon kleine Verfeinerungen an Tool-Beschreibungen brachten messbar grosse Verbesserungen (bis SOTA auf SWE-bench).
- Quelle: [Anthropic — Writing effective tools for AI agents](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`

### B3. Pro-Feld-Beschreibungen mit `.describe()`
- **Empfehlung:** Jedes Feld mit `.describe()` annotieren — erwartetes Format, Einheit, Wertebereich, Konvention, Beispiel (z.B. `.describe("ISO 8601 date, e.g. 2026-06-03")`, `.describe("Temperature in celsius")`). Parameter unmissverstaendlich benennen: `user_id` statt `user`.
- **Vermeiden:** Generische Parameternamen ohne Kontext; nackte Typen ohne Beschreibung; implizite Einheiten.
- **Warum:** `.describe()` wird zu `description` im JSON-Schema und landet direkt im Tool-Kontext des LLM. (Auf altem SDK propagierte zod-v4-`.describe()` nicht — auf 1.27.1+ gefixt, Bug-Almanach 3.10.)
- Quelle: [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`; [SDK Issue #1143](https://github.com/modelcontextprotocol/typescript-sdk/issues/1143) — 2025-11-28 — `offiziell`

### B4. Enge Typen — enums, Constraints, flaches Top-Level
- **Empfehlung:** Top-Level immer flaches `z.object({})`. `z.enum(["concise","detailed"])` statt freier Strings; Constraints (`.min()/.max()/.int()/.length()`); klare Required vs Optional; sinnvolle `.default()`.
- **Vermeiden:** Freitext-Strings wo eine endliche Menge gilt; `z.discriminatedUnion`/Top-Level-Union/`transform` als Tool-Input (wird auf 1.27.1 **still zu leerem Schema** verworfen, Bug-Almanach 3.2 — Diskriminator als enum-Feld + handlerseitige Verzweigung); "alles optional".
- **Warum:** Strikte Datenmodelle reduzieren Mehrdeutigkeit und Fehlaufrufe. Breit unterstuetzte JSON-Schema-Keywords (`type`, `enum`, `properties`, `required`, `description`, `minimum/maximum`, `minLength/maxLength`, `default`) funktionieren ueber OpenAI/Gemini/Claude hinweg; exotische Keywords (`$ref`/`oneOf`/`exclusiveMinimum`) meiden.
- Quelle: [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`; [MCP Spec — Tools (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/server/tools) — 2025-06-18 — `offiziell`

### B5. `outputSchema` & structured content
- **Empfehlung:** Bei maschinenlesbaren Ergebnissen `outputSchema` deklarieren und `structuredContent` (JSON-Objekt) zurueckgeben; fuer Rueckwaerts-Kompatibilitaet denselben JSON zusaetzlich als serialisierten Text-Block im `content` mitliefern. Nur hochwertige Felder ausgeben.
- **Vermeiden:** Nur Text zurueckgeben, wenn die Daten klar strukturiert sind; `outputSchema` deklarieren, aber nicht-konformes Ergebnis liefern (der Server MUSS konform sein); Kontext mit irrelevanten Metadaten (`uuid`, `mime_type`, `256px_image_url`) fluten.
- **Warum:** `outputSchema` ermoeglicht strikte Validierung, Typ-Info und besseres Parsing durch LLM/Client.
- Quelle: [MCP Spec — Tools / Structured Content](https://modelcontextprotocol.io/specification/2025-06-18/server/tools) — 2025-06-18 — `offiziell`; [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`

### B6. Tool-Namen, `title` vs `name`, Namespacing
- **Empfehlung:** `name` = stabiler, programmatischer Identifier (fuer `tools/call`), nur `[a-zA-Z0-9_]`, max 64 Zeichen, `snake_case`, sprechend und eindeutig. Verwandte Tools mit konsistentem Prefix namespacen (`asana_projects_search`, `asana_users_search`). `title` = optionaler, menschenlesbarer Display-Name fuer die UI.
- **Vermeiden:** `.`/`-`/`:`/Leerzeichen im `name` (Claude-Client lehnt ab, Bug-Almanach 3.7); Display-Text in `name` pressen; `name` nachtraeglich umbenennen (bricht Aufrufer).
- **Warum:** Clients matchen/rufen ueber `name`, zeigen `title`. Trennung haelt die API stabil. Namespacing markiert Grenzen zwischen mehreren Servern.
- Quelle: [MCP Spec — Tools](https://modelcontextprotocol.io/specification/2025-06-18/server/tools) — 2025-06-18 — `offiziell`; [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`

### B7. Tool-Annotations (Verhaltens-Hints)
- **Empfehlung:** Annotations bewusst setzen: `readOnlyHint: true` fuer reine Lese-Tools, `destructiveHint: false` wenn nicht-destruktiv, `idempotentHint: true` wenn Wiederholung gefahrlos, `openWorldHint` je nach externem Zugriff. Spec-Defaults kennen: `readOnly=false`, `destructive=true`, `idempotent=false`, `openWorld=true`.
- **Vermeiden:** Annotations weglassen (dann gelten die konservativen Defaults — das Tool wird z.B. als potenziell destruktiv behandelt); Annotations als Sicherheitsgarantie missverstehen (Clients muessen sie als untrusted behandeln).
- **Warum:** Die ersten drei Hints beantworten die Preflight-Frage "muss der Client vor dem Aufruf nachfragen?". `readOnlyHint:true` fuer reine Such-/Status-Tools macht sie fuer den Client unbedenklich.
- Quelle: [MCP Blog — Tool Annotations as Risk Vocabulary](https://blog.modelcontextprotocol.io/posts/2026-03-16-tool-annotations/) — 2026-03-16 — `offiziell`; [MCP Spec — Tools](https://modelcontextprotocol.io/specification/2025-06-18/server/tools) — 2025-06-18 — `offiziell`

### B8. JSON-Schema draft-2020-12 mit zod v4
- **Empfehlung:** zod v4 nutzen — `z.toJSONSchema(schema)` erzeugt standardmaessig **draft-2020-12** (explizit: `{ target: "draft-2020-12" }`). SDK 1.27.1+ macht das nativ; SDK aktuell halten.
- **Vermeiden:** Altes `zod-to-json-schema` (seit Nov 2025 ungepflegt; erzeugte draft-07 → moderne Clients wie Claude Code antworten 400, Bug-Almanach 3.4).
- **Warum:** Moderne Clients verlangen strikte draft-2020-12-Konformitaet; zod v4 generiert JSON-Schema nativ (eine Abhaengigkeit weniger, korrekter Output).
- Quelle: [Zod — JSON Schema](https://zod.dev/json-schema) — 2025 — `offiziell`; [SDK Issue #745](https://github.com/modelcontextprotocol/typescript-sdk/issues/745) — 2025-12-05 — `offiziell`

---

## C. Tool-Granularitaet, API-Design & Token-Effizienz

### C1. Wenige maechtige Workflow-Tools
- **Empfehlung:** Wenige, durchdachte Tools fuer konkrete High-Impact-Workflows bauen, die sich an echten Aufgaben orientieren — nicht die komplette API abbilden. Verwandte Operationen konsolidieren: ein `schedule_event` (prueft Verfuegbarkeit + bucht) statt `list_users`+`list_events`+`create_event`.
- **Vermeiden:** Jeden API-Endpunkt 1:1 als Tool wrappen (CRUD-Mapping). "More tools don't always lead to better outcomes."
- **Warum:** Zu viele/ueberlappende Tools lenken den Agenten von effizienten Strategien ab; konsolidierte Tools sparen Round-Trips und Kontext.
- Quelle: [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`

### C2. Tool-Anzahl-Budget & Discoverability
- **Empfehlung:** Tool-Zahl bewusst begrenzen und an Eval-Aufgaben ausrichten. Bei mehreren Servern Praefix-Namespacing nach Service/Ressource, um Grenzen zu markieren und Namensraum-Konflikte zu vermeiden.
- **Vermeiden:** Dutzende Server / hunderte Tools ungefiltert anbieten — das fuellt den Kontext mit Tool-Definitionen und verwirrt das Modell bei der Tool-Wahl.
- **Warum:** Agenten bekommen potenziell hunderte Tools; klare Grenzen + sprechende Namen verbessern die Auswahlpraezision. (Claude Code mildert das mit **Tool-Search** — siehe G7 — aber ein schlankes Tool-Set bleibt besser.)
- Quelle: [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`

### C3. Token-effiziente Antworten
- **Empfehlung:** Bei potenziell grossen Outputs **Pagination + Range-Selection + Filtering + sinnvolle Default-Limits** kombinieren. Bei Truncation den Agenten mit Hinweisen zu gezielteren Suchen lenken (statt Broad-Retrieval).
- **Vermeiden:** Riesige JSON-Dumps ungefiltert; unbegrenzte Result-Mengen.
- **Warum:** Claude Code begrenzt Tool-Antworten standardmaessig auf **25.000 Token** (Warnung ab 10.000; per `MAX_MCP_OUTPUT_TOKENS` erhoehbar) — wer das nicht respektiert, verliert Antworten/Kontext.
- **Anker:** `search_code` hat einen `limit`-Default (10) ✅; bei sehr grossen Chunks zusaetzlich pro-Result-Laenge begrenzen.
- Quelle: [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`; [code.claude.com/docs/en/mcp — Output limits](https://code.claude.com/docs/en/mcp) — 2026-06 — `offiziell`

### C4. Response-Format-Steuerung (concise vs detailed)
- **Empfehlung:** Einen `response_format`-Enum-Parameter anbieten (`"concise"` Default vs `"detailed"`), mit dem der Agent die Ausfuehrlichkeit waehlt (Beispiel-Messung Anthropic: 72 vs 206 Token). Detailed liefert z.B. IDs fuer Folge-Calls.
- **Vermeiden:** Immer den vollen Detail-Output liefern, obwohl der Agent nur eine Kurzform braucht.
- **Warum:** Spart Kontext, ohne den Zugriff auf Detaildaten zu verlieren.
- Quelle: [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`

### C5. Antwort-Design fuer Agenten (semantische IDs, gute Fehlertexte)
- **Empfehlung:** Semantisch sprechende Identifier zurueckgeben (UUIDs in interpretierbare Namen oder einfache indizierte IDs aufloesen). Fehlermeldungen mit konkreten, umsetzbaren Hinweisen formulieren, die den Agenten zum richtigen Retry fuehren.
- **Vermeiden:** Low-Level-IDs ausgeben, wenn sie fuer Folge-Operationen nicht gebraucht werden; opake Fehlercodes.
- **Warum:** Rauschen kostet Token; gute Fehlertexte ermoeglichen Selbstkorrektur ohne menschliches Eingreifen.
- Quelle: [Anthropic — Writing effective tools](https://www.anthropic.com/engineering/writing-tools-for-agents) — 2025 — `offiziell`

### C6. Pagination nach Spec (cursor-basiert)
- **Empfehlung:** Cursor-basierte Pagination: Server gibt aktuelle Seite + optional `nextCursor` (opaker Token) zurueck; Client paginiert mit `cursor`-Param bis kein `nextCursor` mehr kommt. Gilt auch fuer `tools/list`. Page-Size bestimmt der Server.
- **Vermeiden:** Nummerierte Seiten oder feste Page-Size annehmen; Cursor-Inhalt parsen (er ist opak).
- **Warum:** Opake Cursor entkoppeln Client von Server-internem Paging und vermeiden inkonsistente Annahmen.
- Quelle: [MCP Spec — Pagination (2025-03-26, unveraendert 2025-11-25)](https://modelcontextprotocol.io/specification/2025-03-26/server/utilities/pagination) — `offiziell`

### C7. Code Execution / Programmatic Tool Calling (fuer sehr grosse Server)
- **Empfehlung:** Bei sehr grossen Servern (hunderte/tausende Tools ueber mehrere MCP-Server) bzw. grossen Datasets MCP-Tools als **Code-API** exponieren und den Agenten Code schreiben lassen: Progressive Disclosure (Tools on-demand) + Filtern/Transformieren im Code, bevor Daten ins Modell zurueckfliessen.
- **Vermeiden:** Diese Komplexitaet bei einfachen sequenziellen Single-Call-Workflows (Sandbox-/Monitoring-Overhead lohnt nicht).
- **Warum:** Bis ~98,7% Token-Ersparnis im Anthropic-Beispiel; ~20–40% bei 10–49 Tools. Erfordert aktiviertes Code-Execution-Tool. Fuer einen kleinen 3-Tool-Server (wie `mcp-code-search`) **nicht** noetig.
- Quelle: [Anthropic — Code execution with MCP](https://www.anthropic.com/engineering/code-execution-with-mcp) — 2025 — `offiziell`; [Claude API — Programmatic tool calling](https://platform.claude.com/docs/en/agents-and-tools/tool-use/programmatic-tool-calling) — 2025/2026 — `offiziell`

---

## D. Fehler-Propagation, Logging & Laufzeit-Robustheit

### D1. `isError:true` (Tool-Error) vs JSON-RPC `error` (Protokoll-Fehler) — die Kern-Unterscheidung
- **Empfehlung:** Erwartbare/fachliche Fehler (API-Ausfall, ungueltige Eingabedaten, Business-Logik) als normales Ergebnis `{ content:[{type:"text", text:"…"}], isError:true }` zurueckgeben — das Modell sieht den Fehler und kann sich **selbst korrigieren**. Echte Protokoll-Verletzungen (unbekanntes Tool, malformed Request, interner Fehler) als JSON-RPC-`McpError`.
- **Vermeiden:** Fachliche Fehler als JSON-RPC-`error` werfen (Modell sieht den Text oft nicht); Protokoll-Verletzungen als `isError` verstecken; **nie beides** fuer denselben Fall.
- **Warum:** Die Spec unterscheidet bewusst beide Mechanismen; nur `isError`-Inhalte fliessen als Kontext zurueck ins Modell.
- Quelle: [MCP Spec — Tools / Error Handling (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/server/tools) — 2025-06-18 — `offiziell`

### D2. Standard-JSON-RPC-Fehlercodes — wann welcher
- **Empfehlung:** Standardcodes nur fuer ihre Bedeutung: `-32700` Parse, `-32600` Invalid Request, `-32601` Method not found, `-32602` Invalid Params, `-32603` Internal Error. Eigene App-Codes **ausserhalb** der reservierten Range −32768…−32000.
- **Vermeiden:** Alles pauschal als `-32603`; Custom-Codes im reservierten Bereich.
- **Warum:** Standardcodes lassen Clients generisch korrekt reagieren.
- Quelle: [MCP Spec — Logging / Error Codes (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/logging) — 2025-06-18 — `offiziell`; [mcpevals.io — MCP error codes](https://www.mcpevals.io/blog/mcp-error-codes) — 2025 — `extern`

### D3. Gute, aktionable Fehlermeldungen ohne Leaks
- **Empfehlung:** Jede Meldung beantwortet "Was ging schief?" + "Wie korrigieren?" — spezifisch (`Invalid date format. Use YYYY-MM-DD`). Spezifische Exceptions fangen und in eine sanitisierte High-Level-Message wrappen.
- **Vermeiden:** Stacktraces, DB-Credentials, interne Pfade oder rohe Low-Level-Exceptions ans Modell durchreichen (`sqlite3.OperationalError: no such table: …`).
- **Warum:** Die Spec verlangt explizit, sensible Infos (Credentials, PII, interne Systemdetails) zu entfernen; das Modell soll genug erfahren, um sich zu korrigieren, aber nie den Stacktrace sehen.
- Quelle: [MCP Spec — Logging (Security)](https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/logging) — 2025-06-18 — `offiziell`; [mcpcat.io — Error handling](https://mcpcat.io/guides/error-handling-custom-mcp-servers/) — 2025 — `extern`

### D4. Logging-Disziplin — niemals stdout bei stdio
- **Empfehlung:** Bei stdio ALLE Diagnostik auf **stderr** (`console.error`/`console.warn`). Fuer client-sichtbares, level-steuerbares Logging die **logging-Capability** nutzen: `capabilities.logging`, dann `notifications/message` mit RFC-5424-Leveln (debug…emergency), optionalem `logger`-Namen und JSON-`data`; auf `logging/setLevel` reagieren. Strukturiert loggen.
- **Vermeiden:** `console.log`/`print`/Banner/ANSI-Codes/Pretty-Print auf **stdout** — korrumpiert den JSON-RPC-Stream (der **#1-Bug**, Bug-Almanach 1.1). Bei Streamable HTTP greift stderr nicht beim Client → dort die logging-Capability nutzen.
- **Warum:** Spec: "The server MUST NOT write anything to its `stdout` that is not a valid MCP message"; stderr ist explizit erlaubt.
- **Anker:** `index.ts` nutzt korrekt `console.warn`/`console.error` (stderr) ✅ — niemals auf `console.log` umstellen.
- Quelle: [MCP Spec — Transports](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports) — 2025-06-18 — `offiziell`; [MCP Spec — Logging](https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/logging) — 2025-06-18 — `offiziell`

### D5. Top-Level-Resilienz (nicht crashen)
- **Empfehlung:** Globale `process.on('uncaughtException', …)` + `process.on('unhandledRejection', …)` installieren, die auf **stderr** loggen statt den Prozess sterben zu lassen; nur bei wirklich unrettbarem State kontrolliert beenden. Jeden async-Handler in `try/catch`. Floating Promises vermeiden (immer `await`/`.catch()`).
- **Vermeiden:** Unbehandelte Rejections/Exceptions (reissen die ganze MCP-Verbindung ab — stdio reconnectet **nicht** automatisch, Bug-Almanach 7.1); fire-and-forget ohne Fehlerpfad.
- **Warum:** Resilienz auf Prozessebene haelt den Server am Leben und macht Fehler diagnostizierbar (Direktive #3 — loggen statt schlucken).
- **Anker:** `index.ts` hat **KEINEN** solchen Handler → als Haertung ergaenzen (loggt auf stderr, bleibt am Leben).
- Quelle: [mcpcat.io — Error handling](https://mcpcat.io/guides/error-handling-custom-mcp-servers/) — 2025 — `extern`

### D6. Input-Validierung
- **Empfehlung:** Frueh validieren (zod), Validierungsfehler fangen und als `isError:true` mit konkretem Feld/erwartetem Format zurueckgeben (Input-Validation-Errors sind laut Spec Tool-Exec-Errors → Self-Correction). Bei strukturell falschem Request stattdessen `-32602`.
- **Vermeiden:** Validierung ueberspringen; `zod.parse()` ungefangen werfen lassen (kryptischer Crash).
- **Warum:** Das Modell bekommt verwertbare Rueckmeldung statt Crash/Stille.
- Quelle: [MCP Spec — Tool Execution Errors](https://modelcontextprotocol.io/specification/2025-06-18/server/tools) — 2025-06-18 — `offiziell`

### D7. Timeout & Progress bei langlaufenden Tools
- **Empfehlung:** Fuer langlaufende Tools `resetTimeoutOnProgress: true` setzen **(explizit, unabhaengig vom SDK-Default)** + `onprogress`-Callback mitgeben + periodisch `notifications/progress` senden (streng steigender `progress`, optional `total`/`message`). Immer `maxTotalTimeout` als harte Obergrenze.
- **Vermeiden:** Lange Tools ohne Progress (Default-Timeout ~60s → Abbruch `-32001`); Progress-Notifications fluten; `progress`-Wert nicht erhoehen; nach Abschluss weiter senden.
- **Warum:** Ohne Progress + Reset brechen Clients langlaufende Calls ab. **Hinweis:** Auf dem Projekt-Anker 1.27.1 ist `resetTimeoutOnProgress` **opt-in/false** (Bug-Almanach 7.2); spaetere SDKs aenderten den Default — darum **immer explizit** setzen, dann ist es versionsunabhaengig korrekt.
- Quelle: [MCP Spec — Progress (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/progress) — 2025-06-18 — `offiziell`; [SDK PR #849 / Issue #245](https://github.com/modelcontextprotocol/typescript-sdk/pull/849) — 2025 — `extern`

### D8. Cancellation respektieren
- **Empfehlung:** `notifications/cancelled` verarbeiten: das `AbortSignal` aus `RequestHandlerExtra.signal` im Handler abfragen/weiterreichen (z.B. an `fetch`), bei Abbruch Verarbeitung stoppen, Ressourcen freigeben, **keine** Antwort mehr fuer den abgebrochenen Request senden. Abbruch-Reason loggen.
- **Vermeiden:** Cancellation ignorieren und weiterrechnen; nach Abbruch trotzdem eine Response schicken.
- **Warum:** Spec verlangt "Stop processing / Free resources / Not send a response". Beide Seiten muessen die Race-Condition graceful behandeln (die Notification kann nach Abschluss eintreffen).
- Quelle: [MCP Spec — Cancellation (2025-06-18)](https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/cancellation) — 2025-06-18 — `offiziell`

---

## E. `.mcp.json`, Cross-Platform-Start, Projekt-Setup & Distribution

### E1. `.mcp.json`-Struktur & Scopes
- **Empfehlung:** stdio-Server mit `type:"stdio"`, `command` (absoluter Pfad), `args` (Array), `env` (Objekt). Scope bewusst waehlen: **`local`** (Default, `~/.claude.json`) fuer persoenliche/experimentelle Server mit Credentials; **`project`** (`.mcp.json` im Repo) fuer geteilte Team-Server; **`user`** fuer persoenliche Utility-Server ueber alle Projekte.
- **Vermeiden:** Credentials in einen eingecheckten `project`-`.mcp.json`; annehmen, dass Felder ueber Scopes gemergt werden (werden sie nicht — bei Namensgleichheit gewinnt der hoechste Scope ganz).
- **Warum:** Praezedenz `local > project > user > plugin`. Project-Server brauchen einmalige Genehmigung.

```json
{
  "mcpServers": {
    "code-search": {
      "type": "stdio",
      "command": "C:/Users/barwa/.bun/bin/bun.exe",
      "args": ["C:/Users/barwa/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```
- Quelle: [code.claude.com/docs/en/mcp — Scopes / .mcp.json](https://code.claude.com/docs/en/mcp) — 2026-06 — `offiziell`

### E2. Absolute Pfade & `cmd /c` (Windows) — der robusteste Start
- **Empfehlung:** stdio-Server am robustesten als `node <abs-pfad>` bzw. `bun <abs-pfad>` (absoluter Interpreter-Pfad, kein nackter Befehlsname). Auf **nativem Windows** `npx`/`bunx`/`npm`/`pnpm` **MUSS** in `cmd /c … -y` gewrappt werden (es sind `.cmd`-Shims, die `spawn` ohne Shell nicht aufloest).
- **Vermeiden:** `"command":"npx"`/`"bun"`/`"node"` ohne absoluten Pfad (GUI-/Spawn-Kontext hat anderen PATH → `spawn ENOENT`, Bug-Almanach 5.1/6.1); auf `cwd` verlassen (Feld wird ignoriert, 5.3).
- **Warum:** stdio-Server reconnecten bei Fehlstart **nicht** — ein falscher Pfad killt den Server fuer die ganze Session.
- Quelle: [code.claude.com/docs/en/troubleshooting](https://code.claude.com/docs/en/troubleshooting) — 2026-06 — `offiziell`; eigener Vorfall #1556 (Bug-Almanach 5.1)

### E3. Projektpfad statt `cwd` — `CLAUDE_PROJECT_DIR`
- **Empfehlung:** Projekt-Root ueber `process.env.CLAUDE_PROJECT_DIR` lesen oder den MCP-`roots/list`-Request nutzen, ODER den Pfad als Tool-Argument uebergeben. In `${CLAUDE_PROJECT_DIR}` ausserhalb von Plugins einen Default setzen: `${CLAUDE_PROJECT_DIR:-.}`.
- **Vermeiden:** Projektpfad ueber `process.cwd()` raten.
- **Warum:** Der Server-Prozess startet nicht zwingend mit dem Projektverzeichnis als cwd; `cwd` in `.mcp.json` ist non-funktional (Bug-Almanach 5.3).
- **Anker:** `index.ts` nimmt `directory` als **absoluten Tool-Parameter** und `resolve()`t ihn ✅ — der robusteste Weg.
- Quelle: [code.claude.com/docs/en/mcp — Add a local stdio server](https://code.claude.com/docs/en/mcp) — 2026-06 — `offiziell`

### E4. Cross-Platform-Start (Escaping, UTF-8, kein Shebang-Direktaufruf)
- **Empfehlung:** In JSON Backslashes doppeln (`C:\\…`) **oder** Forward-Slashes verwenden. Server immer ueber den Interpreter starten (`node script.js`/`bun script.ts`), nie das Skript per Shebang direkt. UTF-8 explizit: Node `process.stdout.setEncoding('utf8')`; Python `PYTHONIOENCODING=utf-8`.
- **Vermeiden:** Einzelne Backslashes in JSON (ungueltiges Escape); Shebang-Direktaufruf (Windows kennt keine Shebangs, Bug-Almanach 6.5); Non-ASCII ohne UTF-8 (cp1252-Verstuemmelung im Pipe-Kontext, 6.4).
- **Warum:** Pipe-Kontext nutzt auf Windows sonst die ANSI-Codepage; ein Interpreter-Aufruf ist plattformneutral.
- Quelle: [MCP Docs — Build an MCP server (Windows-Note)](https://modelcontextprotocol.io/docs/develop/build-server) — 2026-06 — `offiziell`

### E5. `env` & Secrets
- **Empfehlung:** Secrets via `${VAR}`-Expansion (in `command`/`args`/`env`/`url`/`headers`) aus der Umgebung ziehen; `${VAR:-default}` als sicheren Fallback. Secrets aus Umgebung/sicherem Speicher lesen (Projekt-Konvention: `$HOME/SK/<projekt>/`), nie hardcoden.
- **Vermeiden:** Pflicht-Variablen ohne Default (Claude Code scheitert dann am Parsen der ganzen Config); Tokens in einen eingecheckten `env`-Block.
- **Warum:** Variable-Expansion erlaubt geteilte Configs bei maschinenspezifischen Secrets; ein fehlendes defaultloses `${VAR}` bricht die gesamte Config.
- Quelle: [code.claude.com/docs/en/mcp — env expansion](https://code.claude.com/docs/en/mcp) — 2026-06 — `offiziell`

### E6. package.json / tsconfig (sauberes ESM-Skelett)
- **Empfehlung:** `"type":"module"`; `module`/`moduleResolution: "NodeNext"` (oder `Node16`), `target: ES2022`, `strict: true`, `outDir`/`rootDir`. Lokale **und** SDK-Subpath-Imports mit `.js`-Endung (`@modelcontextprotocol/sdk/server/mcp.js`). Fuer npm-Distribution: `bin`-Feld, `files:["build"]`-Allowlist, Build-Skript `tsc` (auf Unix `chmod 755`).
- **Vermeiden:** `.js`-Endung weglassen (bricht unter NodeNext, Bug-Almanach 8.7); CommonJS mischen; Build-Output ohne `files`-Allowlist publishen.
- **Warum:** MCP-TS-SDK ist ESM; NodeNext verlangt explizite Endungen.
- **Anker:** `mcp-code-search` ist `"type":"module"`, importiert mit `.js` ✅; laeuft via Bun direkt aus `.ts` (kein tsc-Build noetig fuer den lokalen Runner).
- Quelle: [MCP Docs — Build an MCP server (TS-Setup)](https://modelcontextprotocol.io/docs/develop/build-server) — 2026-06 — `offiziell`

### E7. Distribution — npx-Publish vs lokaler Runner
- **Empfehlung:** Breite Verteilung → als npm-Paket mit `bin`-Feld publishen, Nutzer starten per `npx -y <paket>` (Windows: `cmd /c npx -y <paket>`); tsc-Build mitliefern (Konsumenten erwarten JS). Eigener lokaler Server (privates Tooling) → direkter `node <abs-build>` bzw. `bun <abs-src>`-Eintrag, kein Publish noetig (robuster: kein Netzwerk, kein Resolver-Risiko).
- **Vermeiden:** Lokalen Server als nackten Pfad ohne absoluten Pfad; npx-Paket ohne `bin`/`files`.
- **Warum:** `npx`+`bin` = Ein-Befehl-Setup fuer Fremde; lokaler Runner ist fuer Single-User-Tools am stabilsten.
- **Hinweis (`extern`):** Fuer portable lokale Server gibt es das **`.mcpb`-Bundle-Format** (MCP Bundle) als Alternative — [Adopting the MCP Bundle format](https://blog.modelcontextprotocol.io/posts/2025-11-20-adopting-mcpb/) — 2025-11-20 — `extern`.
- Quelle: [MCP Docs — Build an MCP server (bin/files)](https://modelcontextprotocol.io/docs/develop/build-server) — 2026-06 — `offiziell`

### E8. `.mcp.json` vor Start validieren
- **Empfehlung:** Valides JSON halten (kein BOM, kein Trailing-Comma), vor Start pruefen: `python -c "import json;json.load(open('.mcp.json'))"`. Mit `claude mcp list`/`get` + `/mcp` verifizieren. Optional per-Server `"timeout"` (ms) / `MCP_TIMEOUT` fuer langsamen Start.
- **Vermeiden:** Datei mit `sed`/`echo >>`/`Out-File` editieren (zerstoert JSON); BOM-Praefix; reservierten Servernamen `workspace`.
- **Warum:** Ein Trailing-Comma/BOM macht das JSON ungueltig → **alle** Server dieser Datei fallen still aus (Bug-Almanach 5.5).
- Quelle: [code.claude.com/docs/en/mcp — Managing servers](https://code.claude.com/docs/en/mcp) — 2026-06 — `offiziell`

---

## F. Sicherheit & Authorization

### F1. Transport-Sicherheit: stdio vs Streamable HTTP
- **Empfehlung:** Bei **stdio** (lokal) KEINE OAuth-Spec implementieren — Credentials aus der Umgebung lesen. OAuth 2.1 nur bei HTTP-Transport. Lokale Server, die nur vom Client erreichbar sein sollen, bewusst stdio waehlen.
- **Vermeiden:** OAuth-Flows in einen stdio-Server pressen; lokalen HTTP-Server ungeschuetzt offen lassen.
- **Warum:** Spec: stdio SHOULD NOT der Authorization-Spec folgen (Credentials aus Umgebung); HTTP SHOULD ihr folgen.
- Quelle: [MCP Spec — Authorization (2025-11-25)](https://modelcontextprotocol.io/specification/draft/basic/authorization) — 2025-11-25 — `offiziell`

### F2. OAuth 2.1 + PKCE (Remote-Server)
- **Empfehlung:** MCP-Server agiert als OAuth-2.1-Resource-Server; PKCE mit `S256` ist Pflicht; auf einen (ggf. separaten) Authorization Server delegieren.
- **Vermeiden:** Eigenes Token-/Login-System bauen; PKCE/`S256` weglassen.
- **Warum:** PKCE verhindert Authorization-Code-Interception/-Injection.
- Quelle: [MCP Spec — Authorization (Standards Compliance)](https://modelcontextprotocol.io/specification/draft/basic/authorization) — 2025-11-25 — `offiziell`

### F3. Protected Resource Metadata (RFC 9728) + 401/WWW-Authenticate
- **Empfehlung:** RFC 9728 implementieren: `/.well-known/oauth-protected-resource` mit `authorization_servers`. Bei `401` die Metadata-URL im `WWW-Authenticate`-Header (`resource_metadata=…`) zurueckgeben, optional `scope="…"`.
- **Vermeiden:** AS-Discovery per Konvention raten; 401 ohne `WWW-Authenticate`.
- **Warum:** PRM ist der vorgeschriebene Discovery-Mechanismus, damit der Client den richtigen AS findet.
- Quelle: [MCP Spec — Authorization Server Discovery](https://modelcontextprotocol.io/specification/draft/basic/authorization) — 2025-11-25 — `offiziell`

### F4. Resource Indicators (RFC 8707) & Token-Hygiene
- **Empfehlung:** Tokens an die kanonische Server-URI binden (Client setzt `resource`-Param; Server **prueft hart**, dass das Token genau fuer ihn als Audience ausgestellt wurde). `Authorization: Bearer <token>` in JEDEM Request (nie im URI-Query). Ungueltig/abgelaufen → 401.
- **Vermeiden:** **Token-Passthrough** (fremdes Token an Downstream weiterreichen) — fuer Upstream ein SEPARATES eigenes Token holen; Tokens nie loggen/klartext.
- **Warum:** Token-Passthrough umgeht Rate-Limiting/Validierung, zerstoert den Audit-Trail und macht den Server zum Exfiltrations-Proxy.
- Quelle: [MCP Spec — Token Audience Binding](https://modelcontextprotocol.io/specification/draft/basic/authorization) — 2025-11-25 — `offiziell`; [MCP Security Best Practices](https://modelcontextprotocol.io/docs/tutorials/security/security_best_practices) — 2025-11-25 — `offiziell`

### F5. Origin-Validierung & DNS-Rebinding-Schutz (lokale HTTP-Server)
- **Empfehlung:** Bei lokalem HTTP-Transport `enableDnsRebindingProtection: true` setzen UND `allowedHosts`/`allowedOrigins` konfigurieren (beides = Defense in Depth). An `127.0.0.1` binden, nicht `0.0.0.0`. Origin-Header validieren.
- **Vermeiden:** Lokalen HTTP-Server ohne Schutz laufen lassen — im **TS-SDK ist der DNS-Rebinding-Schutz standardmaessig AUS** (GHSA-w48q-cv73-mx4w); Bindung an `0.0.0.0` macht den Server netzwerkweit erreichbar.
- **Warum:** Ohne Schutz kann eine boesartige Webseite per DNS-Rebinding die Same-Origin-Policy umgehen und den lokalen Server ansprechen.
- Quelle: [GHSA-w48q-cv73-mx4w (TS-SDK Advisory)](https://github.com/modelcontextprotocol/typescript-sdk/security/advisories/GHSA-w48q-cv73-mx4w) — `offiziell`; [MCP Security Best Practices](https://modelcontextprotocol.io/docs/tutorials/security/security_best_practices) — 2025-11-25 — `offiziell`

### F6. Least Privilege & Tool-Sicherheit
- **Empfehlung:** Tools mit minimalen Rechten; destruktive Aktionen via `annotations` markieren und ggf. Consent erzwingen; **Allowlists** statt Denylists. Pfade gegen Path-Traversal absichern, Command-/SQL-Injection durch Parametrisierung/Escaping verhindern. Lokale Server in Sandbox (Container/chroot/App-Sandbox) mit beschraenktem Datei-/Netzzugriff.
- **Vermeiden:** IP-/Pfad-Validierung von Hand stricken (Encoding-Tricks); gefaehrliche Befehle ohne Consent.
- **Warum:** Lokale Server laufen mit Client-Privilegien → arbitrary code execution, Exfiltration und Datenverlust sind reale Risiken.
- Quelle: [MCP Security Best Practices — Local Server Compromise](https://modelcontextprotocol.io/docs/tutorials/security/security_best_practices) — 2025-11-25 — `offiziell`

### F7. Confused Deputy & SSRF (Proxy zu Drittanbietern)
- **Empfehlung:** Bei Proxy-Servern per-Client-Consent VOR dem Drittanbieter-Flow erzwingen; `redirect_uri` per exaktem String-Match validieren; kryptografisch sicheren single-use `state` (erst NACH Consent setzen). SSRF-Schutz: HTTPS erzwingen (ausser loopback), private/reservierte IP-Ranges blocken (inkl. `169.254.169.254` Cloud-Metadata), Redirects validieren.
- **Vermeiden:** Statische Client-ID + dynamische Registrierung ohne per-Client-Consent; Discovery-URLs ungeprueft folgen.
- **Warum:** Sonst kann ein Angreifer per Consent-Cookie die Zustimmung ueberspringen (Confused Deputy) bzw. SSRF auf interne Dienste/Cloud-Credentials ausloesen.
- Quelle: [MCP Security Best Practices — Confused Deputy / SSRF](https://modelcontextprotocol.io/docs/tutorials/security/security_best_practices) — 2025-11-25 — `offiziell`

### F8. Session-Sicherheit & Prompt-Injection ueber Inhalte
- **Empfehlung:** Sessions NIE zur Authentifizierung verwenden — jeden Request eigenstaendig per Token verifizieren. Session-IDs als CSPRNG-UUID, rotieren/ablaufen lassen, an User-Info binden (`<user_id>:<session_id>`, user_id aus dem Token). Tool-/Resource-Inhalte als **Daten** behandeln, nicht als Instruktionen (externe Inhalte saubern/kennzeichnen).
- **Vermeiden:** Vorhersehbare Session-IDs; user_id vom Client uebernehmen; fremde Inhalte ungefiltert als vertrauenswuerdigen Kontext zurueckgeben.
- **Warum:** Verhindert Session-Hijacking und indirekte Prompt-Injection.
- Quelle: [MCP Security Best Practices — Session Hijacking](https://modelcontextprotocol.io/docs/tutorials/security/security_best_practices) — 2025-11-25 — `offiziell`

### F9. Secrets-Management
- **Empfehlung:** Secrets/API-Keys aus Umgebung/sicherem Speicher laden (bei stdio der Standard-Weg); nie ins Repo committen, nie ins Tool-Input-/Output-Schema, nie in Logs.
- **Vermeiden:** API-Keys hardcoden, in JSON-Configs einchecken, als Tool-Parameter exponieren.
- **Warum:** Secrets im Schema/Log/Repo sind direkte Leak-Vektoren.
- Quelle: [MCP Spec — Authorization (stdio → Environment)](https://modelcontextprotocol.io/specification/draft/basic/authorization) — 2025-11-25 — `offiziell`

---

## G. Testing, Debugging, Observability & Workflow

### G1. MCP Inspector — vor dem Client testen
- **Empfehlung:** Inspector ohne Installation starten: `npx @modelcontextprotocol/inspector node path/to/server/index.js` (UI auf `http://localhost:6274`). Tools/Resources/Prompts mit eigenen Eingaben aufrufen, Ergebnis + Notifications/Logs sofort sehen. CLI-Modus (`--cli --method tools/list`/`tools/call`) fuer Skripte/CI. Die UI kann die Config als `mcp.json` exportieren.
- **Vermeiden:** Einen frisch gebauten Server direkt in Claude Code haengen, ohne ihn im Inspector verifiziert zu haben; nach Code-Aenderungen Rebuild/Reconnect vergessen.
- **Warum:** Der Inspector isoliert Protokoll-/Schema-Fehler vom Client und macht Capability-Negotiation + Fehlerpfade sichtbar, bevor ein LLM ins Spiel kommt.
- Quelle: [MCP Docs — Inspector](https://modelcontextprotocol.io/docs/tools/inspector) — 2026-06-03 — `offiziell`; [github.com/modelcontextprotocol/inspector](https://github.com/modelcontextprotocol/inspector) — 2026-06-03 — `offiziell`

### G2. Claude Code als Test-Client
- **Empfehlung:** `claude mcp add --transport stdio --env KEY=val <name> -- <cmd> <args>` (Optionen VOR dem Namen, dann `--`). Mit `claude mcp list`/`get <name>` verifizieren (zeigt `⏸ Pending approval`/`✗ Rejected`). In der Session `/mcp` fuer Status + Tool-Anzahl je Server; bei "connected, 0 Tools" Reconnect, sonst `claude --debug mcp` fuer den stderr-Output. `/doctor` validiert die Config-Dateien.
- **Vermeiden:** Reservierten Servernamen `workspace`; Optionen NACH dem Servernamen.
- **Warum:** Claude Code ist der reale Ziel-Client; `/mcp` + `--debug mcp` + `/doctor` sind die schnellsten Diagnose-Pfade.
- Quelle: [code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) — 2026-06-03 — `offiziell`

### G3. Evals & Tests (3-Layer-Pyramide)
- **Empfehlung:** (1) **Unit** — Tool-Handler als reine Funktionen (Schema-Validierung, Logik); (2) **Integration** — Test-Client per **In-Memory-Transport** direkt an den Server koppeln (kein Prozess-Spawn) und echte `tools/call`-Roundtrips; (3) **Evals** — echte Prompts an ein LLM, verifizieren dass das richtige Tool mit richtigen Argumenten gewaehlt wird. Wenn nur eins moeglich: Integrationstests auf den Handlern.
- **Vermeiden:** Nur Unit-Tests der Logik ohne Tool-Auswahl-Evals (die Frage "versteht das LLM die Beschreibung?" bleibt ungetestet).
- **Warum:** Funktionale Korrektheit (Handler) und KI-Qualitaet (Tool-Auswahl) sind verschiedene Versagensklassen; nur die Pyramide deckt beide ab.
- Quelle: [MCP App Testing Strategy (sunpeak.ai)](https://sunpeak.ai/blogs/mcp-app-testing-strategy/) — 2026-05 — `extern`; [Unit Testing MCP Servers (mcpcat.io)](https://mcpcat.io/guides/writing-unit-tests-mcp-servers/) — 2026 — `extern`

### G4. Observability
- **Empfehlung:** stdio → Logs auf stderr (UTF-8), strukturiert (pino/winston). Transport-unabhaengig Logs an den Client ueber die **logging-Capability** (`notifications/message`) — noetig bei Streamable HTTP, wo stderr nicht beim Client ankommt. Remote-Server zusaetzlich Metriken/Tracing + dediziertes Logfile.
- **Vermeiden:** `console.log`/Banner/ANSI auf stdout bei stdio (korrumpiert JSON-RPC).
- **Warum:** stdout ist bei stdio der Protokoll-Kanal.
- Quelle: [MCP Docs — Debugging](https://modelcontextprotocol.io/docs/tools/debugging) — 2026-06-03 — `offiziell`; [modelcontextprotocol PR #670 (stderr-Logging)](https://github.com/modelcontextprotocol/modelcontextprotocol/pull/670) — 2026-06-03 — `offiziell`

### G5. SDK-Versionierung & Update-Disziplin
- **Empfehlung:** `@modelcontextprotocol/sdk` mit **exakter Version** pinnen (z.B. `1.27.1`) statt `^1`, weil das SDK vor 2.0 ist (0/1.x-Semver bringt regelmaessig Verhaltensaenderungen). Releases-Seite verfolgen, Updates bewusst nachvollziehen. Sicherheitsrelevante Updates (CVE-Fixes, z.B. ReDoS in <1.25.2) zeitnah einspielen und gegen die Test-Pyramide laufen lassen. Spec-Revision im Blick (SSE deprecated → Streamable HTTP).
- **Vermeiden:** Blindes `npm update` mit Caret-Range im Prod-Server; ungetestete Sprung-Upgrades (aendern Fehlercodes/Tool-Registrierung).
- **Warum:** Vor 2.0 sind Minor-Releases potenziell breaking; ein Pin + bewusster Changelog-Abgleich verhindert stille Regressionen. (Hinweis: `mcp-code-search` nutzt `^1.27.1` — fuer einen Prod-Server auf exakt pinnen erwaegen; Fixes fuer discriminatedUnion/unknown-tool kommen erst ab 1.28/1.29, Bug-Almanach 3.2/4.3.)
- Quelle: [github.com/modelcontextprotocol/typescript-sdk + Releases](https://github.com/modelcontextprotocol/typescript-sdk/releases) — 2026-06-03 — `offiziell`

### G6. Entwicklungs-Workflow (empfohlene Reihenfolge)
- **Empfehlung:** (1) Mit offiziellem Quickstart/`build-server` starten oder scaffolden lassen (Claude Code: `mcp-server-dev`-Plugin); (2) Schema/Tools lokal im **Inspector** pruefen; (3) erst dann in Claude Code einbinden (`claude mcp add`) + `/mcp` verifizieren; (4) Tools iterativ verfeinern (klare Namen/Schemas, Edge-Cases). HTTP als Default fuer Remote (OAuth, breite Unterstuetzung), nie SSE fuer Neues.
- **Vermeiden:** Server "von Hand" bauen und ungetestet in den Client schieben.
- **Warum:** Der gestufte Pfad (scaffold → Inspector → Client) faengt Fehler frueh und billig ab — vor dem teuren LLM-Loop.
- Quelle: [code.claude.com/docs/en/mcp — build / mcp-server-dev](https://code.claude.com/docs/en/mcp) — 2026-06-03 — `offiziell`; [MCP Docs — Build an MCP server](https://modelcontextprotocol.io/docs/develop/build-server) — 2026-06-03 — `offiziell`

### G7. Doku, Wartbarkeit & Beschreibungs-Budget
- **Empfehlung:** **Server-Instructions** und Tool-Beschreibungen praezise und KURZ halten — Claude Code schneidet beide bei **2 KB** ab; kritische Details an den Anfang. Mit aktiviertem **Tool-Search** (Default) entscheiden die Server-Instructions, WANN Claude die Tools des Servers ueberhaupt sucht (wirken wie Skill-Beschreibungen). Pro Server eine README (Zweck, Tools/Resources/Prompts, Transport, Env-Vars, Start-/Inspector-Befehl). Nur Capabilities deklarieren, die wirklich erfuellt werden. `alwaysLoad: true` nur fuer Tools, die jeden Turn gebraucht werden (kosten Kontext).
- **Vermeiden:** Vage/zu lange Beschreibungen (werden truncatet → Tool-Search findet die Tools nicht); leere Capabilities ankuendigen (von `/mcp` geflaggt).
- **Warum:** Beschreibungs-Qualitaet bestimmt heute direkt die Auffindbarkeit (Tool-Search) — Doku ist Teil der Laufzeit-Funktion.
- Quelle: [code.claude.com/docs/en/mcp — Tool Search / server instructions](https://code.claude.com/docs/en/mcp) — 2026-06-03 — `offiziell`

---

## Anker-Server-Kurzbilanz (`~/proggs/mcp-code-search/src/index.ts`)

Konkrete Anwendung dieser Best-Practices auf den eigenen Server (3 Tools, stdio, Bun, SDK `^1.27.1`):

| Bereich | Status | Empfehlung |
|---------|--------|-----------|
| stderr-Logging (`console.warn/error`) | ✅ vorbildlich | so lassen — nie `console.log` |
| Lazy-Init (`getStore`) | ✅ vorbildlich | so lassen (A6) |
| ESM `.js`-Imports + `"type":"module"` | ✅ korrekt | so lassen (E6) |
| `directory` als absoluter Tool-Param statt cwd | ✅ vorbildlich | so lassen (E3) |
| SIGTERM/SIGINT-Shutdown | ✅ vorhanden | bei schweren Writes `await` + Watchdog (A7) |
| `server.tool(...)` (deprecated) | ⚠️ veraltet | auf `registerTool` migrieren (B1) |
| `uncaughtException`/`unhandledRejection` | ❌ fehlt | Top-Level-Handler ergaenzen, loggen statt crashen (D5) |
| Tool-Annotations | ❌ fehlt | `readOnlyHint:true` fuer `search_code`/`search_status` (B7) |
| `outputSchema`/structured content | — optional | Such-Ergebnisse koennten strukturiert zurueck (B5) |
| SDK-Version `^1.27.1` | ⚠️ Range | fuer Prod exakt pinnen; 1.28/1.29 bringt Schema-/Error-Fixes (G5) |

---

## Pflicht-Checkliste vor MCP-Server-Arbeit (positive Seite)

- [ ] Transport bewusst gewaehlt (lokal → stdio, remote → Streamable HTTP, nie SSE fuer Neues)? (A1)
- [ ] Richtiges Primitive (Tool=Aktion / Resource=Daten / Prompt=User-Template)? (A4)
- [ ] `registerTool` mit klarer `description` + `.describe()` pro Feld + engen Typen? (B1–B4)
- [ ] `outputSchema`/annotations gesetzt wo sinnvoll, Tool-Name `[a-zA-Z0-9_]{1,64}`? (B5–B7)
- [ ] Wenige Workflow-Tools, token-effiziente Antworten (Limit/Pagination, <25k Token)? (C)
- [ ] Fehler richtig: fachlich → `isError:true`, Protokoll → `McpError`; nie leeres catch? (D1–D3)
- [ ] stdout sauber (alle Logs stderr/logging-Capability)? (D4)
- [ ] `uncaughtException`/`unhandledRejection`-Handler + try/catch + kein floating Promise? (D5)
- [ ] Langlaufende Tools: `resetTimeoutOnProgress:true` explizit + Progress + maxTotalTimeout? (D7)
- [ ] `.mcp.json`: absolute Pfade / `cmd /c -y` (Windows), valide (kein BOM/Comma), Secrets via `${VAR}`? (E)
- [ ] Lazy-Init + Graceful Shutdown? (A6/A7)
- [ ] Sicherheit: stdio-Secrets aus Umgebung; HTTP → OAuth2.1/PKCE/Audience/Origin/DNS-Rebinding-Schutz aktiv; nie Token-Passthrough? (F)
- [ ] Vor dem Client mit MCP Inspector getestet, SDK-Version gepinnt? (G1/G5)

---

## 🔗 Kopplung zum Bug-Almanach (wechselseitige Bezugstabelle)

Best-Practices (diese Datei) ↔ Bug-Almanach [`~/proggs/bugs/claude-tooling/mcp-server.md`](../../bugs/claude-tooling/mcp-server.md). Die identische Tabelle steht auch dort. Links die *richtige Arbeitsweise*, rechts die *Falle, die sie verhindert*.

| Best-Practice-Abschnitt (hier) | Zugehoeriger Bug-Almanach-Abschnitt (`bugs/claude-tooling/mcp-server.md`) |
|--------------------------------|------------------------------------------------------------|
| A1 Transport-Wahl / SSE deprecated | 2.1 SSE-Deprecation / Streamable HTTP |
| A2 Stateless vs stateful | 2.4 Stateless „Server not initialized", 2.5 Session-Lifecycle |
| A3 McpServer vs Low-Level | 3.9 manueller ListTools ohne inputSchema |
| A5 Lifecycle/Handshake | 7.4 protocolVersion-Mismatch, 7.5 Handshake-Reihenfolge |
| A6 Lazy Startup | 7.3 Init-/Startup-Timeout |
| A7 Graceful Shutdown | 7.7 SIGTERM/SIGINT-Cleanup |
| B1 registerTool | 3.3 `server.tool` deprecated |
| B3 `.describe()` | 3.10 zod-v4-Description nicht propagiert |
| B4 Enge Typen / flaches z.object | 3.2 discriminatedUnion → leeres Schema, 3.5/3.6 vendor-Keywords |
| B6 Tool-Namen | 3.7 Tool-Name-Regex; 3.8 `inputSchema {}` vs `undefined` |
| B8 draft-2020-12 | 3.4 draft-07 → 400; 3.1 zod-v4 `_parse` (historisch) |
| C3 Token-effiziente Antworten | (Client-Limit 25k — Praevention, kein Bug) |
| D1/D2 isError vs JSON-RPC error | 4.1 leeres catch, 4.2 Fehlerklasse, 4.3 unknown-tool, 4.6 Codes, 4.8 Python |
| D4 stdout-Hygiene / Logging | 1.1 console.log auf stdout (#1), 1.2 dotenv-Banner, 1.3 Pretty-Print |
| D5 Prozess-Resilienz | 4.4 uncaughtException, 4.5 floating Promise, 7.1 kein Auto-Reconnect, 7.6 Event-Loop-Block |
| D7 Timeout & Progress | 7.2 resetTimeoutOnProgress opt-in |
| D8 Cancellation | (7.x Lifecycle — Praevention) |
| E1 `.mcp.json`/Scopes | 5.6 Scope-Lade-Bugs |
| E2 Absolute Pfade / cmd /c | 5.1 nackter Befehl, 6.1 cmd /c, 6.2 `claude mcp add` zerstoert `/c`, 6.3 ENOENT, 5.7 npx -y |
| E3 Projektpfad statt cwd | 5.3 cwd ignoriert, 5.4 Tilde/Relativpfad |
| E4 Cross-Platform-Start | 6.4 cp1252/UTF-8, 6.5 Shebang, 6.6 Backslash-Pfade, 1.5 CRLF (Python) |
| E5 env & Secrets | 5.2 env nicht durchgereicht |
| E6 ESM-Setup | 8.7 ESM `.js`-Imports |
| E8 JSON valide | 5.5 Trailing-Comma/BOM |
| F2–F4 OAuth/Token | 8.6 OAuth-Fehlimplementierungen |
| F5 Origin/DNS-Rebinding | 2.6 Origin-Validierung (+ GHSA-w48q-cv73-mx4w), 2.7 CORS Mcp-Session-Id |
| G5 SDK-Versionierung | 8.1 ReDoS CVE, 8.2 transitive CVEs, 8.3 Batching, 8.4 Protocol-Version-Header, 8.5 Capabilities-Overwrite; Sektion 9 Fix-Status |
| (HTTP-Robustheit allg.) | 2.2 Accept-Header 406, 2.3 JSON-only 406, 2.8 Idle-Timeout/Keep-Alive |

*Neue belegte Best-Practices hier ergaenzen (mit Quelle + Datum + `offiziell`/`extern`). Bei jedem neu erlebten Bug zusaetzlich `bugs/claude-tooling/mcp-server.md` ergaenzen und diese Bezugstabelle synchron halten. Bei SDK-Versionssprung (1.30+, Spec-Revision) Re-Check von B (Schema-Verhalten), D7 (Timeout-Default) und G5 (Fix-Status).*
