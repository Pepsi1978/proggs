# Bekannte Bugs & Fallen: MCP-Server-Bau (Model Context Protocol)

> **PFLICHT-LESEN vor JEDER Arbeit an einem MCP-Server** (`.mcp.json`, Server-Quellcode
> mit `@modelcontextprotocol/sdk`/`McpServer`/`StdioServerTransport`/`FastMCP`,
> Tool-/Resource-/Prompt-Registrierung, Transport-Code).
> Kuratiert aus offizieller Quelle zuerst (modelcontextprotocol.io/specification,
> github.com/modelcontextprotocol typescript-sdk + python-sdk, code.claude.com/docs),
> dann GitHub-Issues, Community (mcpcat.io, auth0, dev.to) und eigenen Vorfaellen.
> Loesungen sind funktionserhaltend (nie „Feature weglassen").
>
> **Stand:** recherchiert am **2026-06-03**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax)
> fuer **MCP TypeScript-SDK 1.27.1** (Projekt
> `~/proggs/mcp-code-search`, `^1.27.1`, Bun 1.3.11, Node 24.15.0, TS 5.9.3, zod v4 ^4.3.6),
> Spec-Revisionen 2024-11-05 / 2025-03-26 / 2025-06-18 / 2025-11-25. **1.27.1 wurde am
> 2026-02-24 veroeffentlicht** — entscheidend fuer den Fix-Status (Sektion 9): einige
> Schema-/Error-Bugs sind erst in 1.28.0 (2026-03-25) / 1.29.0 (2026-03-30) gefixt und
> auf dem Anker 1.27.1 noch AKTIV.
>
> **⚡ GROSSER UMBRUCH VORAUS (Re-Recherche 2026-07-02) — siehe neue §8.8:** (1) **MCP TS-SDK v2**
> ist als `v2.0.0-beta.1` (30.06.2026) da, **stabiles v2 geplant 28.07.2026** — Package-Split
> (`@modelcontextprotocol/server` + `/client` statt `/sdk`), „bring your own schema" (Zod nicht mehr
> Pflicht), `serveStdio()`/`createMcpHandler()`, Codemod `v1-to-v2`. v1.x bekommt noch **≥6 Monate**
> Fixes. (2) **Spec-Revision `2026-07-28`** (Release Candidate, „groesste Revision seit Launch",
> Breaking Changes): **Stateless Core** (kein `initialize`-Handshake / keine `Mcp-Session-Id`),
> **Roots/Sampling/Logging deprecatet**. Frank bleibt vorerst auf v1/1.27.1 — das ist **Migrations-Vorwissen**.
>
> **Abgrenzung:** Dies ist die **Server-Autor-Seite** (einen MCP-Server BAUEN). Das
> *Konfigurieren/Verbinden* von Servern in Claude Code steht in `best-practices/claude-tooling/mcp.md`,
> die *Hook*-Seite (MCP-Matcher, MCP-Tool-als-Hook) in [`claude-hooks.md`](claude-hooks.md).
> Die positive Gegenseite zu DIESEM Almanach (*wie man es richtig baut*) steht in
> [`best-practices/claude-tooling/mcp-server.md`](../../best-practices/claude-tooling/mcp-server.md)
> — wechselseitige Bezugstabelle in Sektion 11. Details der Abgrenzung: Sektion 10.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `-32000 Connection closed` / „Invalid JSON-RPC" bei stdio | stdout NUR JSON-RPC; alle Logs auf stderr | §1.1 |
| 2 | Dependency druckt heimlich auf stdout (dotenv-Banner) | `dotenv {quiet:true}`; `console.log=console.error` oben | §1.2 |
| 3 | `.mcp.json` startet Server nicht / `spawn ENOENT` | Absolute Pfade; `npx`/`bunx` in `cmd /c … -y` wrappen | §5.1, §6.1 |
| 4 | `.mcp.json` laedt KEINEN Server | Kein Trailing-Comma, kein BOM; JSON validieren | §5.5 |
| 5 | Tool-Call gibt „Erfolg", tat aber nichts | Fehler nie verschlucken; loggen + `isError:true` | §4.1 |
| 6 | Fachlicher vs Protokoll-Fehler | Fachlich → `isError:true`; Protokoll → JSON-RPC-`error` | §4.2 |
| 7 | Server stirbt still, „disconnected" | `uncaughtException`/`unhandledRejection` loggen, nicht crashen | §4.4, §7.1 |
| 8 | Tool-Schema kommt leer an (`properties:{}`) | Top-Level flaches `z.object`; kein discriminatedUnion (1.27.1) | §3.2 |
| 9 | Tool wird vom Client abgelehnt | Tool-Name nur `[a-zA-Z0-9_]{1,64}` | §3.7 |
| 10 | `server.tool()` deprecated, Schema verschwindet | Auf `registerTool` migrieren | §3.3 |
| 11 | Langlaufendes Tool bricht nach 60s ab | `resetTimeoutOnProgress:true` + Progress senden (opt-in!) | §7.2 |
| 12 | Server-Start zu langsam → Session FAILED | Schwere Init lazy, nicht vor `initialize`-Response | §7.3 |
| 13 | Windows: Umlaute/Emoji im Output kaputt | UTF-8 explizit (`setEncoding('utf8')`/`PYTHONIOENCODING`) | §6.4 |
| 14 | `ERR_MODULE_NOT_FOUND` (ESM) | `"type":"module"`, lokale Imports mit `.js`-Endung | §8.7 |
| 15 | `npm audit` HIGH (UriTemplate-ReDoS) | SDK ≥1.25.2 — 1.27.1 sicher, NICHT downgraden | §8.1 |
| 16 | Python-FastMCP: `TypeError: issubclass()` beim ersten `@mcp.tool()` | KEIN `from __future__ import annotations` (macht Annotationen zu Strings → FastMCP-Introspektion bricht). Issue #1129 | §3.11 |
| 17 | Python-FastMCP: `lifespan`-Init laeuft mehrfach | FastMCP-`lifespan` ist PRO Client-Session, nicht pro Server. App-weite Init via ASGI-Lifespan (#1115) | §3.12 |
| 18 | Python-FastMCP: HTTP 421 / DNS-Rebinding | `host` IM Konstruktor setzen (nachtraegliches `settings.host` greift nicht → 421). Auto-Schutz erst ab SDK v1.23.0+ (CVE-2025-66416); Anker 1.12.4 davor → `transport_security`+`allowed_hosts` explizit | §3.13 |

---

## 1. stdio-Transport: stdout-Stream-Zerstoerung (DER #1-Bug)

### 1.1 `console.log`/`print` auf stdout zerstoert den JSON-RPC-Stream  ⭐ HAEUFIG / Nr.1-Ursache
**Symptom:** Client meldet sofort `Invalid JSON-RPC message` / `MCP error -32000: Connection
closed` / „disconnected". Der Server „laeuft", reagiert aber nie.
**Ursache:** Bei stdio nutzt der Server stdout AUSSCHLIESSLICH fuer newline-delimited
JSON-RPC-Frames. Spec: „The server MUST NOT write anything to its `stdout` that is not a
valid MCP message." Jede Zeile `console.log(...)` (Node) / `print(...)` (Python) korrumpiert
den Stream → Client verwirft die Verbindung.
**Versionen:** alle SDKs, alle Spec-Revisionen, transportbedingt (per Design).
**FIX:** Logging behalten, aber auf **stderr**: Node `console.error`/`console.warn` (gehen
auf stderr), notfalls ganz oben `console.log = console.error`. Python `print(..., file=sys.stderr)`
oder `logging` (Default-Handler → stderr) oder `tools/logging`-Capability statt eigenem stdout.
Sanity-Check: Server durch `cat` pipen — auf stdout darf NUR JSON erscheinen.
**Anker-Hinweis:** `mcp-code-search/src/index.ts` nutzt korrekt `console.warn`/`console.error`
(stderr) — niemals auf `console.log` umstellen.
**Quelle:** [Spec 2025-11-25 Transports](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports), [mcpcat.io -32000](https://mcpcat.io/guides/fixing-mcp-error-32000-connection-closed/), [Postman Community](https://community.postman.com/t/mcp-server-stdout-pollution-causing-invalid-json-rpc-messages-in-claude-desktop/89753).

### 1.2 Eine Abhaengigkeit schreibt heimlich auf stdout (dotenv-Banner u.a.)
**Symptom:** Wie 1.1, aber im eigenen Code steht kein `console.log` — trotzdem Parse-Fehler bei jedem Start.
**Ursache:** Eine Library gibt beim Laden ein Banner/Tip/Update-Notice auf stdout aus. Konkret:
neuere `dotenv`-Versionen drucken eine Tip-Zeile auf stdout (betraf `@postman/mcp-server`).
Telemetrie-/Update-Notifier sind haeufige Taeter.
**Versionen:** SDK-unabhaengig.
**FIX:** `dotenv` mit `{ quiet: true }` konfigurieren; jede frueh ladende Dependency auf
stdout-Writes pruefen; im Zweifel `console.log = console.error` als oberste Zeile setzen.
**Quelle:** [Postman Community](https://community.postman.com/t/mcp-server-stdout-pollution-causing-invalid-json-rpc-messages-in-claude-desktop/89753).

### 1.3 Eingebettete Newlines / Pretty-Print brechen das stdio-Framing
**Symptom:** Sporadische Parse-Fehler, abgeschnittene Messages, Haenger.
**Ursache:** stdio-Messages sind newline-delimited und „MUST NOT contain embedded newlines".
`JSON.stringify(obj, null, 2)` (Pretty-Print) oder Multiline-Strings im Payload erzeugen interne
`\n` → der Client liest eine Message als mehrere.
**Versionen:** alle Spec-Revisionen.
**FIX:** Immer kompaktes Single-Line-JSON serialisieren (kein `null, 2`-Indent auf stdout).
Newlines im *Content* dürfen escaped (`\n`) drin sein — das macht die Standard-JSON-
Serialisierung automatisch; nur kein Pretty-Indent. Encoding strikt UTF-8.
**Quelle:** [Spec 2025-11-25 Transports](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports).

### 1.4 LSP-Style `Content-Length:`-Framing statt NDJSON
**Symptom:** Server „spricht", verbindet aber nicht mit dem Client.
**Ursache:** Server nutzt das LSP-Header-Framing (`Content-Length:`-Praefix), MCP-stdio erwartet
aber reines newline-delimited JSON (ein JSON-Objekt + `\n`).
**Versionen:** alle.
**FIX:** Reines NDJSON ausgeben, keine Laengen-Header. Mit dem offiziellen SDK passiert das
automatisch — nur bei selbstgebautem Transport relevant.
**Quelle:** [codegraph #172](https://github.com/colbymchenry/codegraph/issues/172), [foojay.io Raw-STDIO](https://foojay.io/today/understanding-mcp-through-raw-stdio-communication/).

### 1.5 `\r\n` statt `\n` korrumpiert NDJSON (Windows, Python-SDK)
**Symptom:** Windows-Tool-Timeouts / Parse-Fehler; jede JSON-RPC-Zeile endet mit `\r\n`.
**Ursache:** Python-SDK `stdio_server()` erstellt `TextIOWrapper` ohne `newline=""` → Windows
uebersetzt `\n` zu `\r\n`. Verletzt das NDJSON-Wire-Format. Der JS-Client strippt trailing `\r`
zwar via `.replace(/\r$/,"")`, strikte `split("\n")`-Clients brechen aber.
**Versionen:** Windows, MCP Python-SDK — [python-sdk #2433](https://github.com/modelcontextprotocol/python-sdk/issues/2433) **OPEN** (verifiziert 2026-06-03).
**FIX:** `newline=""` an beide `TextIOWrapper` (deaktiviert Translation, bleibt Text-Modus).
Eigene Server generell mit `newline=""` schreiben. (TS-Server auf Windows: `process.stdout`
schreibt `\n` korrekt; Encoding trotzdem explizit setzen, s. 6.4.)

---

## 2. HTTP-Transport: SSE-Deprecation & Streamable HTTP

### 2.1 SSE-Transport ist deprecated — Verwechslung / falscher Endpoint
**Symptom:** Neue Clients (ab 2025) verbinden nicht; Server bietet nur den alten Zwei-Endpoint-SSE-Flow.
**Ursache:** HTTP+SSE (Spec 2024-11-05, getrennter GET-SSE-Stream + POST-Endpoint) wurde in
Revision **2025-03-26** durch **Streamable HTTP** (ein einziger `/mcp`-Endpoint, POST+GET) ersetzt.
SSE existiert nur noch fuer Rueckwaertskompatibilitaet.
**Versionen:** SSE deprecated seit Spec 2025-03-26; TS-SDK Streamable HTTP ab 1.10.0.
**FIX (funktionserhaltend):** Auf Streamable HTTP an EINEM Endpoint migrieren. Wer alte Clients
weiter bedienen muss: **beide** Transporte parallel hosten (kein Feature wegnehmen). Client-
Fallback laut Spec: erst `initialize` per POST; bei 400/404/405 auf GET-SSE-`endpoint`-Event zurueck.
**Quelle:** [Spec 2025-11-25 Backwards Compatibility](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports), [fka.dev](https://blog.fka.dev/blog/2025-06-06-why-mcp-deprecated-sse-and-go-with-streamable-http/), [python-sdk #2278](https://github.com/modelcontextprotocol/python-sdk/issues/2278) (OPEN).

### 2.2 Fehlender `text/event-stream` im Accept-Header → 406 Not Acceptable
**Symptom:** Spec-konformer Streamable-HTTP-Server antwortet `406`, alle Tool-Calls scheitern.
**Ursache:** Spec: Client MUSS `Accept: application/json, text/event-stream` senden. Viele
Clients senden nur `application/json` → strikter Server lehnt ab. Betraf Claude Agent SDK,
M365-Toolkit, Open WebUI.
**Versionen:** Spec ab 2025-03-26.
**FIX:** Client-seitig beide Accept-Typen listen. Server-seitig nicht stillschweigend ignorieren
(das ist ein Client-Bug) — aber für Server-Robustheit siehe 2.3.
**Quelle:** [claude-agent-sdk-typescript #202](https://github.com/anthropics/claude-agent-sdk-typescript/issues/202), [m3best-practices/claude-tooling/agents.md-toolkit #15421](https://github.com/OfficeDev/microsoft-3best-practices/claude-tooling/agents.md-toolkit/issues/15421).

### 2.3 TS-SDK: 406 trotz `enableJsonResponse:true` (Server-seitiger SDK-Bug)  🆕 OFFEN
**Symptom:** Server im reinen JSON-Modus (`enableJsonResponse:true`) antwortet `406`, wenn der
Client `Accept: application/json` OHNE `text/event-stream` sendet — obwohl gar kein SSE-Stream existiert.
**Ursache:** `StreamableHTTPServerTransport` verlangt `text/event-stream` im Accept auch im
JSON-Only-Modus, wo nichts zu verhandeln ist.
**Versionen:** [typescript-sdk #1944](https://github.com/modelcontextprotocol/typescript-sdk/issues/1944) **OPEN** (verifiziert 2026-06-03) — auf 1.27.1 aktiv.
**FIX (Workaround, funktionserhaltend):** Client trotzdem beide Accept-Typen senden lassen;
Issue beobachten, nach SDK-Update erneut testen.

### 2.4 Stateless-Modus: `sessionIdGenerator: undefined` → „Server not initialized"
**Symptom:** Nach erfolgreicher `initialize` schlaegt der erste echte Call fehl mit
`Bad Request: Server not initialized` / `Method not found` / `-32000`.
**Ursache:** Im stateless `StreamableHTTPServerTransport` (`sessionIdGenerator: () => undefined`
+ `enableJsonResponse:true`) wird ohne Session-ID der Init-State nicht ueber Requests gehalten;
`initialized`/Per-Request-Instanziierung wird nicht korrekt verknuepft.
**Versionen:** TS-SDK; Stateless-Grundbug [typescript-sdk #340](https://github.com/modelcontextprotocol/typescript-sdk/issues/340) ✅ CLOSED 2025-04-18 (in 1.27.1 gefixt). Begleit-Issues #408/#412/#553 pruefen.
**FIX (funktionserhaltend):** Stateless: pro Request eine FRISCHE Transport+Server-Instanz
erzeugen und `await server.connect(transport)` VOR dem Handling. Stateful: `sessionIdGenerator`
als echte UUID-Funktion setzen (nicht undefined). Lifecycle korrekt verdrahten, kein Feature entfernen.

### 2.5 `Mcp-Session-Id`-Lifecycle falsch behandelt → 400/404
**Symptom:** `400` (Session-ID fehlt) oder `404` (Session abgelaufen), Client gibt auf statt neu zu initialisieren.
**Ursache:** Spec-Regeln: Server mit Sessions MUSS Nicht-Init-Requests ohne `Mcp-Session-Id`
mit `400` beantworten; nach Terminierung `404`; Client MUSS bei `404` mit frischem
`initialize` (ohne Session-ID) neu starten. Session-ID nur sichtbare ASCII (0x21–0x7E).
**Versionen:** Spec ab 2025-03-26.
**FIX:** Client: bei `404` automatisch re-initialisieren. Server: 400/404 spec-konform; Session-ID
als kryptografische UUID. Bei Multi-Instance-Deployments Session in **shared storage** statt
Sticky-Sessions (sonst Bruch bei Deploy/Failover). Verwandt: [typescript-sdk #1658](https://github.com/modelcontextprotocol/typescript-sdk/issues/1658) **OPEN** — keine public API um session-aware Transport aus persistierter Session zu rekonstruieren.
**Quelle:** [Spec 2025-11-25 Session Management](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports), [auth0.com](https://auth0.com/blog/mcp-streamable-http/).

### 2.6 Fehlende Origin-Validierung → DNS-Rebinding (oder zu strikt → 403)
**Symptom:** (a) Sicherheitsloch: entfernte Webseite spricht lokalen MCP-Server an. (b) Fehlkonfig: legitime Clients bekommen `403`.
**Ursache:** Spec: Server MUSS `Origin`-Header validieren; bei ungueltigem Origin `403`. Vergessen → DNS-Rebinding; zu enge Allowlist → legitime Calls geblockt. **Verschaerfend im TS-SDK:** Der DNS-Rebinding-Schutz (`enableDnsRebindingProtection`) ist standardmaessig **AUS** — wer ihn nicht aktiv einschaltet, ist ungeschuetzt (Advisory GHSA-w48q-cv73-mx4w).
**Versionen:** Spec ab 2025-03-26; TS-SDK-Default-AUS per Advisory [GHSA-w48q-cv73-mx4w](https://github.com/modelcontextprotocol/typescript-sdk/security/advisories/GHSA-w48q-cv73-mx4w).
**FIX:** Im TS-SDK `enableDnsRebindingProtection: true` setzen UND `allowedHosts`/`allowedOrigins` konfigurieren (Defense in Depth). Origin-Allowlist korrekt setzen (nicht entfernen!), lokal an `127.0.0.1` binden statt `0.0.0.0`, Auth ergaenzen. Schutz aktivieren UND echte Client-Origins eintragen.
**Quelle:** [Spec Security Warning](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports), [GHSA-w48q-cv73-mx4w](https://github.com/modelcontextprotocol/typescript-sdk/security/advisories/GHSA-w48q-cv73-mx4w), [auth0.com](https://auth0.com/blog/mcp-streamable-http/).

### 2.7 CORS: `Mcp-Session-Id` nicht als Response-Header exponiert (Browser-Clients)
**Symptom:** Browser-Client kann die Session nach Init nicht weiterfuehren; Preflight scheitert oder ID ist im JS nicht lesbar.
**Ursache:** Streamable HTTP nutzt POST/GET/OPTIONS → CORS. Ohne `Access-Control-Expose-Headers: Mcp-Session-Id` kann der Browser den ID-Header nicht auslesen.
**Versionen:** Spec ab 2025-03-26.
**FIX:** CORS-Middleware: `Mcp-Session-Id` in `Access-Control-Allow-Headers` UND `Access-Control-Expose-Headers`, `OPTIONS` erlauben.
**Quelle:** [auth0.com](https://auth0.com/blog/mcp-streamable-http/).

### 2.8 HTTP/SSE-Verbindung nach ~60s Idle getrennt — kein Keep-Alive
**Symptom:** Streamable-HTTP/SSE-Connection bricht nach Idle ab; danach `Stream closed`.
**Ursache:** Server-/Proxy-Idle-Timeout; ohne periodisches Keep-Alive gilt die Verbindung als tot.
**Versionen:** FastMCP HTTP-Stream, diverse. [typescript-sdk #812](https://github.com/modelcontextprotocol/typescript-sdk/issues/812) (Idle Session Timeout) ✅ CLOSED 2026-03-23 (nach 1.27.1 → ggf. erst ab 1.28/1.29).
**FIX (funktionserhaltend):** Server sendet periodisch Keep-Alive (SSE-Comment/`ping`, Intervall < Idle-Timeout); Proxy/LB-Idle-Timeout erhoehen; Client: Heartbeat + Reconnect mit exponential backoff.
**Quelle:** [fastmcp #120](https://github.com/punkpeye/fastmcp/issues/120), [claude-code #30224](https://github.com/anthropics/claude-code/issues/30224).

---

## 3. Tool-/Input-Schema (Client lehnt das Tool ab)

### 3.1 `keyValidator._parse is not a function` — zod v4 vs altes SDK (HISTORISCH)
**Symptom:** Tool-Call wirft `MCP error -32603: keyValidator._parse is not a function`.
**Ursache:** SDK **<1.18** ruft interne zod-Methoden direkt auf, die zod v4 umbenannt/entfernt hat.
**Versionen:** SDK ≤1.17.5 betroffen; ab ~1.18 (Standard-Schema-Support) behoben. [typescript-sdk #925](https://github.com/modelcontextprotocol/typescript-sdk/issues/925) ✅ CLOSED 2025-11-21. **1.27.1 + zod v4 ist sicher** — kein Downgrade noetig.
**FIX:** SDK ≥1.18 (Anker 1.27.1 erfuellt das). Wer auf altem SDK feststeckt: SDK hochziehen oder zod auf v3 pinnen.

### 3.2 `z.discriminatedUnion()`/Union/transform wird STILL verworfen → `properties:{}`  ⭐ auf 1.27.1 AKTIV
**Symptom:** Tool registriert, aber Client sieht **leeres** Schema (`{type:"object",properties:{}}`).
Kein Fehler, keine Warnung, kein TS-Compile-Hinweis. Das Modell muss alle Parameter aus dem
Description-Text raten.
**Ursache:** `normalizeObjectSchema` akzeptiert nur `z.object()`; `discriminatedUnion`/Top-Level-
Union/`transform` fallen durch.
**Versionen:** [typescript-sdk #1643](https://github.com/modelcontextprotocol/typescript-sdk/issues/1643) ✅ CLOSED **2026-03-30** — also gefixt NACH 1.27.1 (2026-02-24). **Auf dem Anker 1.27.1 noch AKTIV**; Fix ab 1.28.0/1.29.0. Verwandt: transform lost [typescript-sdk #702](https://github.com/modelcontextprotocol/typescript-sdk/issues/702) (CLOSED 2025-10-06, eher per Design — Transforms sind in JSON-Schema nicht darstellbar).
**FIX (funktionserhaltend):** Top-Level immer flaches `z.object({...})`; Diskriminator als
enum-Feld + handlerseitige Verzweigung; Transforms aus dem advertisten Schema heraushalten und
serverseitig im Handler validieren. ODER SDK auf ≥1.29 heben.

### 3.3 `server.tool()` ist deprecated → `registerTool` (Schema verschwindet still)
**Symptom:** Schema kommt leer an, obwohl gesetzt; `title` vs `name` verwechselt.
**Ursache:** Die positionale `server.tool(name, desc, schema, handler)`-API ist deprecated und
behandelt rohe JSON-Schema-Objekte teils nicht korrekt. **Der Anker-Server `index.ts` nutzt genau
diese alte API.**
**Versionen:** [typescript-sdk #1284](https://github.com/modelcontextprotocol/typescript-sdk/issues/1284) ✅ CLOSED 2025-12-11 (Doku); Deprecation laeuft, noch nicht entfernt.
**FIX:** Auf `registerTool(name, { title, description, inputSchema: { feld: z.string() } }, handler)`
migrieren. Der `ZodRawShape`-Shorthand `{ x: z.number() }` wird automatisch in `z.object` gewrappt —
aber `discriminatedUnion` NICHT (s. 3.2). Funktion bleibt identisch.
**Quelle:** [docs/server.md](https://github.com/modelcontextprotocol/typescript-sdk/blob/main/docs/server.md).

### 3.4 SDK generiert JSON-Schema draft-07, moderne Clients verlangen draft-2020-12 → 400
**Symptom:** Claude Code (u.a.) lehnt das Tool mit HTTP 400 ab; Schema traegt `$schema: ".../draft-07"`.
**Ursache:** Aelterer `zod-to-json-schema@3.24.x`-Pfad gibt draft-07.
**Versionen:** [typescript-sdk #745](https://github.com/modelcontextprotocol/typescript-sdk/issues/745) ✅ CLOSED 2025-12-05 — in 1.27.1 adressiert (zod v4 nativ → 2020-12).
**FIX (zod v4):** `z.toJSONSchema(schema, { target: "draft-2020-12" })`. Mit 1.27.1 + zod v4 meist
schon korrekt — verifizieren was real advertised wird.

### 3.5 Gemini/strenge Clients lehnen `$ref`/`$defs`/`anyOf`-ohne-type/`oneOf` ab
**Symptom:** Client-API 400 `reference to undefined schema` bzw. `missing top-level type`.
**Ursache:** zod erzeugt bei wiederverwendeten/Union-Schemata `$defs`+`$ref` oder `anyOf` ohne `type` — manche Clients (Gemini) unterstuetzen das nicht.
**Versionen:** Client-abhaengig — [gemini-cli #13326](https://github.com/google-gemini/gemini-cli/issues/13326), [adk-python #3424](https://github.com/google/adk-python/issues/3424).
**FIX:** Schemata „inlinen" (keine `$ref`); Unions vermeiden oder `type` erzwingen; flache `z.object`.

### 3.6 `additionalProperties`/`default`/`format`/`minLength` — vendor-spezifisch ignoriert oder abgelehnt
**Symptom:** „additionalProperties not allowed"-Fehler; oder OpenAI/Gemini ignorieren `default`/`format` STILL → LLM-Halluzination. `exclusiveMinimum/Maximum` von OpenAI+Gemini nicht unterstuetzt.
**Ursache:** Vendor-JSON-Schema-Dialekte sind enger als der Standard.
**Versionen:** Client-abhaengig (per Design).
**FIX:** Nur breit unterstuetzte Keywords; Defaults/Constraints serverseitig im Handler durchsetzen statt sich aufs Schema zu verlassen.
**Quelle:** [Mastra compat-layer](https://mastra.ai/blog/mcp-tool-compatibility-layer), [samchon dev.to](https://dev.to/samchon/why-your-mcp-server-fails-how-to-make-100-successful-mcp-server-iem).

### 3.7 Tool-Name-Regex: Claude-Client strenger als die Spec
**Symptom:** `String should match pattern '^[a-zA-Z0-9_]{1,64}$'` — Tool mit `.`, `-` oder >64 Zeichen wird abgelehnt.
**Ursache:** Spec erlaubt mehr (1–128 + `_-.`), aber der Claude-Client erzwingt `[a-zA-Z0-9_]`, max 64; Doppelpunkt `:` verletzt sogar die Spec.
**Versionen:** Client-Regel (per Design) — [spec #1063](https://github.com/modelcontextprotocol/modelcontextprotocol/issues/1063).
**FIX (sicher fuer ALLE Clients):** Tool-Namen nur `[a-zA-Z0-9_]`, max 64 Zeichen, eindeutig — kein `.`/`-`/`:`.

### 3.8 `inputSchema: {}` vs `undefined` verhalten sich unterschiedlich
**Symptom:** Leeres Objekt `{}` und `undefined` erzeugen verschiedenes advertisetes Schema → manche Clients lehnen ab.
**Versionen:** [typescript-sdk #458](https://github.com/modelcontextprotocol/typescript-sdk/issues/458) ✅ CLOSED 2025-05-13.
**FIX:** Tools ohne Parameter: konsistent `z.object({})` ODER keinen inputSchema — nicht `{}` mischen.

### 3.9 Manueller `setRequestHandler(ListTools)` generiert kein `inputSchema`  🆕 OFFEN
**Symptom:** Bei eigenem `ListTools`-Handler fehlt `inputSchema` ganz.
**Ursache:** Manuelle Handler umgehen die automatische zod→JSON-Schema-Konvertierung von `registerTool`.
**Versionen:** [typescript-sdk #1028](https://github.com/modelcontextprotocol/typescript-sdk/issues/1028) **OPEN** (verifiziert 2026-06-03).
**FIX:** `registerTool` nutzen ODER im manuellen Handler selbst gueltiges JSON-Schema (`type:"object"`) liefern.

### 3.10 zod-v4-Description wird nicht ins JSON-Schema propagiert (HISTORISCH)
**Symptom:** `.describe()` auf Feldern landet nicht im inputSchema → Properties ohne `description`, LLM rateempfindlicher.
**Versionen:** [typescript-sdk #1143](https://github.com/modelcontextprotocol/typescript-sdk/issues/1143) ✅ CLOSED 2025-11-28 — in 1.27.1 gefixt.
**FIX:** SDK ≥ Fix-Version; pruefen ob Descriptions im advertisten Schema ankommen.

---

### 3.11 Python-FastMCP: `from __future__ import annotations` bricht die Tool-Registrierung  🆕 (eigener Vorfall 2026-06-22)
**Symptom:** Der Server-Prozess/Container crasht beim Start, sobald der erste `@mcp.tool()`-Decorator
ausgewertet wird: `TypeError: issubclass() arg 1 must be a class` (in `mcp/server/fastmcp/tools/base.py`,
`Tool.from_function`). Der Container landet in einer Restart-Schleife, `tools/list` zeigt nie Werkzeuge.
**Ursache:** `from __future__ import annotations` (PEP 563) macht ALLE Funktions-Annotationen zu **Strings**.
FastMCP introspiziert die Tool-Signatur und ruft je Parameter `issubclass(param.annotation, Context)` auf —
`param.annotation` ist dann der String `"str"` statt der Klasse `str` → `issubclass("str", …)` wirft den TypeError.
Trifft JEDES Tool mit annotierten Parametern.
**Versionen:** MCP Python-SDK `mcp==1.12.4` (FastMCP), Python 3.10+; verifiziert 2026-06-22 (Projekt `second-brain`, Container `sb-mcp`).
**FIX (funktionserhaltend):** Die Zeile `from __future__ import annotations` **entfernen**. In Python 3.10+
funktionieren `X | None`, `list[...]`, `dict | None` ohnehin nativ als Laufzeit-Annotationen — der Future-Import
ist unnoetig. Verifikation: nach dem Entfernen `initialize`+`tools/list` gegen den Server fahren (alle Tools sichtbar).
**Bestaetigt extern (Recherche 2026-06-22):** GitHub Issue **#1129** — Fix **PR #1336** ersetzt `param.annotation`
durch `typing.get_type_hints(fn)` (loest String-Annotationen korrekt auf). Belegt fuer SDK 1.7.1/1.11.0 + Py 3.11/3.13;
wir erlebten es auf **1.12.4** (dort also NICHT gefixt). Gleicher Bug in der eigenstaendigen FastMCP-v2-Lib
(PrefectHQ/fastmcp #905). **Besonders tueckisch wenn ein Tool einen `Context`-Param hat:** dann kein Crash beim
Registrieren, sondern erst zur Laufzeit mit kryptischer Meldung („cannot access context outside of a request").
**Quelle:** eigener Vorfall (second-brain MCP-Wrapper) + GitHub mcp/python-sdk #1129/#1336.

### 3.12 Python-FastMCP: `lifespan` laeuft PRO Client-Session, nicht pro Server  🆕 (Recherche 2026-06-22)
**Symptom:** Setup-/Teardown-Code im `lifespan`-Context-Manager laeuft mehrfach — bei JEDER neuen Client-Session
erneut (Startup + Shutdown), nicht einmalig beim Server-Start. Teure Init (DB-Pool, Modell-Load) wird pro Verbindung wiederholt.
**Ursache:** Anders als FastAPI/Starlette (lifespan = App-Lebensdauer) bindet das MCP-Python-SDK den `lifespan` an die
**Server-Session pro Client**. Maintainer (jlowin): „lifespan is executed when the server session starts/stops on a
client basis, not the server application as a whole."
**Versionen:** MCP Python-SDK / FastMCP (Issue #1115). Doku erwaehnt das kaum.
**FIX (funktionserhaltend):** Fuer **app-weite** (einmalige) Initialisierung das **ASGI-Lifespan-Protokoll** der
Starlette/FastAPI-App nutzen, in die der MCP-Server gemountet ist — NICHT den FastMCP-`lifespan`-Parameter. Reinen
Pro-Session-State weiter ueber FastMCP-`lifespan`.
**Quelle:** GitHub PrefectHQ/fastmcp #1115 (jlowin) · Recherche 2026-06-22.

> **Zwei FastMCP-Welten — nicht verwechseln (Recherche 2026-06-22):** (1) Das **offizielle** `mcp`-SDK mit
> `from mcp.server.fastmcp import FastMCP` (FastMCP 1.0 wurde ins offizielle Python-SDK eingegliedert; das nutzt
> second-brain auf `mcp==1.12.4`). (2) Die **eigenstaendige** `fastmcp`-Lib (PrefectHQ, „FastMCP 2.x/3.x",
> gofastmcp.com) — ein eigenes Framework DARUEBER mit eigenen Breaking Changes (3.0: `ui=`→`app=`, 16 Constructor-
> kwargs entfernt; 3.2.4: Background-Tasks auth- statt session-scoped). Beide teilen den `__future__`-Annotations-Bug
> (§3.11) und das Pro-Session-`lifespan`-Verhalten (§3.12). Beim Recherchieren/Pinnen immer klarstellen, WELCHE der
> beiden gemeint ist — die APIs driften auseinander. (Die in der eigenen `requirements.txt` notierte „2.x benennt
> FastMCP→MCPServer um" stammt aus dem offiziellen-SDK-`main` und ist nicht breit belegt — vorsichtig behandeln.)

### 3.13 Python-FastMCP: DNS-Rebinding sauber via `TransportSecuritySettings` (statt `host=0.0.0.0`-Trick)  🆕 (Recherche 2026-06-22)
**Kontext:** Bei Streamable-HTTP prueft das offizielle SDK den `Host`-Header (DNS-Rebinding-Schutz) und lehnt fremde
Hosts ab. Der schnelle Trick „an `0.0.0.0` binden deaktiviert den Schutz" funktioniert (so loest es second-brains
`server.py`), ist aber die grobe Variante.
**Best Practice (sauberer):** `TransportSecuritySettings` des offiziellen SDK nutzen — Felder
`enable_dns_rebinding_protection`, **`allowed_hosts`**, `allowed_origins`. Statt den Schutz abzuschalten, den
erlaubten Host explizit erlauben, z.B. `allowed_hosts=["10.8.0.1:8001","10.8.0.1"]` — dann bleibt der Schutz aktiv
UND der VPN-Zugriff geht. (Hinter dem WireGuard-Tunnel ist das Risiko klein; fuer ein oeffentliches Deployment waere
`allowed_hosts` aber Pflicht.)
**Zwei Praezisierungen (Changelog-Abgleich 2026-06-22):**
- **Der automatische DNS-Rebinding-Schutz greift erst ab SDK v1.23.0+** (Advisory **CVE-2025-66416** empfiehlt das
  Update). Unser Anker **`mcp==1.12.4` liegt DAVOR** → auf 1.12.4 wird der Schutz NICHT automatisch gesetzt; man muss
  `transport_security` explizit konfigurieren (oder bewusst `0.0.0.0`+WireGuard wie jetzt). Beim spaeteren Hochziehen
  des SDK pruefen, ob der dann aktive Auto-Schutz den `0.0.0.0`-Pfad blockt.
- **`host` MUSS im Konstruktor stehen, nicht nachtraeglich gesetzt werden:** Der Schutz wird **bei der Konstruktion**
  anhand von `host` entschieden. Eine spaetere Zuweisung `mcp.settings.host = "0.0.0.0"` (oder via Env nach `__init__`)
  aktualisiert `TransportSecuritySettings` NICHT mehr → Symptom **HTTP 421 „Misdirected Request - Invalid Host header"**.
  Loesung: `host`/`transport_security` direkt im `FastMCP(...)`-Konstruktor uebergeben.
**Konkrete Empfehlung (Eskalations-Recherche 2026-06-22):** CVE-2025-66416 ist **High**, veroeffentlicht 02.12.2025, betrifft
**alle Versionen < 1.23.0** (stdio NICHT betroffen, nur HTTP/streamable-http/SSE). Fix in **1.23.0**. Neueste v1-Linie:
**1.28.0**; v1.x ist Maintenance-Mode (kritische Fixes), v2 in Alpha (`2.0.0aN`) — offizielle Pin-Empfehlung `mcp>=1.27,<2`.
→ Fuer second-brain: **`mcp==1.12.4` auf 1.27.x/1.28.x hochziehen** (CVE gefixt + Auto-DNS-Schutz + RFC-8707-OAuth-Resource-
Validation/Idle-Timeout aus 1.27.0). Beim Hochziehen pruefen, ob der dann aktive Auto-Schutz den `0.0.0.0`-Pfad blockt
(dann `allowed_hosts` explizit setzen). FastMCP→MCPServer-Rename auch in der Eskalation NICHT belegt (Klasse heisst weiter FastMCP).
**Versionen:** offizielles MCP Python-SDK (Issue #1798; CVE-2025-66416 Fix ab v1.23.0, Anker `mcp==1.12.4` davor; neueste v1 1.28.0).
**Quelle:** GitHub modelcontextprotocol/python-sdk #1798, NVD/GitHub-Advisory GHSA-9h52-p55h-vw2f (CVE-2025-66416), PyPI mcp, dev.to MCP-Dev-Summit-2026 · Recherche 2026-06-22 (Firecrawl + OpenRouter-Eskalation).

## 4. Fehlerbehandlung & Protokoll-Fehler

### 4.1 Leeres `try/catch` verschluckt Fehler — Client sieht „Erfolg"  ⭐ HAEUFIG
**Symptom:** Tool-Call gibt Erfolg zurueck, das Tool hat aber nichts getan. Kein Log, in Produktion nicht debugbar.
**Ursache:** Generischer `catch {}` ohne Logging/Propagation.
**Versionen:** alle.
**FIX:** Fehler vollstaendig **loggen** (auf stderr, NICHT stdout — s. 1.1), dann **strukturiert
zurueckgeben**: `{ content:[{type:"text", text:"<sanitized msg>"}], isError:true }`. Funktion bleibt
erhalten, der Fehler wird sichtbar statt verschluckt (verletzt nie die Funktionserhaltungspflicht).
**Quelle:** [mcpcat.io Error Handling](https://mcpcat.io/guides/error-handling-custom-mcp-servers/).

### 4.2 `isError:true` (Tool-Fehler) vs JSON-RPC-`error` (Protokoll-Fehler) — die Kern-Unterscheidung
**Symptom:** Modell sieht den Fehler nicht / korrigiert sich nicht; oder der Client crasht statt das Tool zu wiederholen.
**Ursache:** Falsche Fehlerklasse gewaehlt. Spec-Regel:
- **Tool-Execution-Error** → `result` mit `isError:true` im `content` (API-Failure, Input-Validation,
  Business-Logic). Der Client liefert das ans LLM, damit es sich **selbst korrigieren** kann.
- **Protocol-Error** → JSON-RPC-`error`-Objekt (unknown tool, malformed request, interner Server-Fehler).
**Versionen:** alle.
**FIX:** Erwartbare/fachliche Fehler als `isError:true` mit klarer Message (was schiefging, wie
korrigieren). Echte Protokoll-Verletzungen als `McpError`/JSON-RPC-Error. Nie beides fuer denselben Fall.
**Quelle:** [Spec — Tools/Error Handling](https://modelcontextprotocol.io/specification/draft/server/tools), [mcpevals.io](https://www.mcpevals.io/blog/mcp-error-codes).

### 4.3 Aufruf eines nicht-existenten Tools gibt `isError` statt `-32601`/`-32602`
**Symptom:** Unbekanntes Tool liefert ein `result` mit `isError:true` statt eines JSON-RPC-`error` — entgegen der Spec.
**Ursache:** SDK behandelte „unknown tool" als Tool-Exec-Error statt als Protocol-Error.
**Versionen:** [typescript-sdk #1510](https://github.com/modelcontextprotocol/typescript-sdk/issues/1510) ✅ CLOSED **2026-03-26** — gefixt NACH 1.27.1. **Auf dem Anker 1.27.1 noch AKTIV**; Fix ab 1.28/1.29.
**FIX (funktionserhaltend):** Bis Upgrade eigene Routing-Schicht: Tool-Existenz pruefen und
`new McpError(ErrorCode.MethodNotFound, …)` werfen, BEVOR der Handler laeuft. Kein Tool entfernen — nur die Fehlerklasse korrigieren.

### 4.4 `uncaughtException`/`unhandledRejection` killt den Server still
**Symptom:** Server stirbt sofort, KEIN Error-Output; Client sieht nur „Connection closed". Tritt bei parallelen Calls oder Native-Module-Crashes auf.
**Ursache:** Kein Top-Level-Handler; eine Exception ausserhalb des Handler-`try/catch` beendet den Prozess.
**Versionen:** alle (Node/Bun).
**FIX:** `process.on('uncaughtException', …)` + `process.on('unhandledRejection', …)` installieren,
die auf **stderr** loggen und nur bei wirklich unrettbarem State kontrolliert beenden — sonst
weiterlaufen. Zusaetzlich jeden async-Handler komplett in `try/catch`. Recovery statt stillem Tod.
**Anker-Hinweis:** `index.ts` hat KEINEN solchen Handler — als Haertung ergaenzen.
**Quelle:** [mcpcat.io](https://mcpcat.io/guides/error-handling-custom-mcp-servers/).

### 4.5 Floating Promise: Tool meldet Erfolg, Hintergrund-Op scheitert still
**Symptom:** Request „erfolgreich", aber die eigentliche Arbeit (DB-Write, HTTP-Call) ist fehlgeschlagen.
**Ursache:** Fire-and-forget async-Aufruf ohne `await`/`.catch()` im Handler. Die Rejection landet im Void (oder spaeter als unhandledRejection → 4.4).
**Versionen:** alle.
**FIX:** Alle async-Operationen `await`en oder `.catch()` anhaengen. Erst nach Erfolg das Result zurueckgeben; bei Fehler `isError:true`.
**Quelle:** [mcpcat.io](https://mcpcat.io/guides/error-handling-custom-mcp-servers/).

### 4.6 Falsche/vermischte JSON-RPC-Codes & Custom-Codes im reservierten Bereich
**Symptom:** Client interpretiert Fehler falsch; Custom-Codes kollidieren mit Standard-Semantik.
**Ursache:** `isError`-Flag und JSON-RPC-Codes durcheinander; oder falscher Code (`-32603` Internal
wo `-32602` Invalid Params korrekt waere); oder Custom-Codes im reservierten Bereich **-32768…-32000**.
**Versionen:** alle (per Design).
**FIX:** Standard-Codes nur fuer ihre Bedeutung: `-32700` Parse, `-32600` Invalid Request, `-32601`
Method Not Found, `-32602` Invalid Params, `-32603` Internal. Eigene App-Codes AUSSERHALB der
reservierten Range. App-Fehler → `isError:true`, Protokoll-Verletzung → JSON-RPC-Code (4.2).
**Quelle:** [mcpevals.io](https://www.mcpevals.io/blog/mcp-error-codes).

### 4.7 zod-/Input-Validierungsfehler verschluckt oder als Crash
**Symptom:** Bad Input → kryptischer Crash ODER still ignoriert; das Modell bekommt keine verwertbare Rueckmeldung.
**Ursache:** Validierung uebersprungen oder `zod.parse()` wirft, ohne als Tool-Exec-Error formatiert zu werden.
**Versionen:** alle (TS-SDK nutzt zod fuers inputSchema).
**FIX:** Frueh validieren; zod-Fehler fangen und als `isError:true` mit konkretem Feld/erwartetem
Format zurueckgeben (Input-Validation-Errors sind laut Spec Tool-Exec-Errors → Self-Correction). Bei
strukturell falschem Request stattdessen `-32602`.
**Quelle:** [Spec — Tool Execution Errors](https://modelcontextprotocol.io/specification/draft/server/tools).

### 4.8 Python-SDK: `@app.call_tool`-Exception wird als „Success mit Plain-Text" verpackt  🆕 OFFEN
**Symptom:** Exception im Handler kommt beim Client als **erfolgreiche** Response an, die Message steckt als Plain-Text im `content`. Client/Modell erkennt nicht, dass es ein Fehler war.
**Ursache:** Dekorierte Handler uebersetzen geworfene Exceptions inkonsistent.
**Versionen:** [python-sdk #396](https://github.com/modelcontextprotocol/python-sdk/issues/396) **OPEN** (verifiziert 2026-06-03).
**FIX:** Im Handler bewusst entscheiden: erwartbare Fehler als `isError:true`, echte Protokoll-Fehler als `McpError`/JSON-RPC-Error werfen — nicht aufs SDK-Default-Verhalten verlassen.

---

## 5. `.mcp.json` / Client-Konfiguration / Server startet nicht

### 5.1 Nackter Befehlsname statt absoluter Pfad → Server startet nicht  ⭐ HAEUFIG (eigener Vorfall)
**Symptom:** Server taucht in `claude mcp list` auf, verbindet aber NIE; `spawn npx ENOENT` (Windows) bzw. `node/npx not found`.
**Ursache:** `"command":"npx"`/`"bun"`/`"node"` ohne absoluten Pfad. Der Spawn-/GUI-Kontext hat
einen anderen PATH als das interaktive Terminal; auf Windows ist `npx` der Batch-Shim `npx.cmd`,
den `child_process.spawn` ohne Shell nicht aufloest.
**Versionen:** Windows/macOS, transportbedingt.
**FIX:** `npx`/`bunx` in `cmd /c` wrappen (s. 6.1) ODER absolute Pfade verwenden.
**Eigener Vorfall (#1556, 2026-04-20):** debugbase-MCP hatte `command:"npx"` ohne Windows-Wrapper →
`/doctor`-Warnung. Fix: absoluter Pfad `C:/Program Files/nodejs/npx.cmd`, konsistent mit dem
think-tank-Pattern; in `~/proggs/.mcp.json` UND `claude-code-setup/mcp-windows.json` gespiegelt.
Best-Practice-Seite: `best-practices/claude-tooling/mcp.md` (Scopes, ENV-Expansion).

### 5.2 `env`-Block wird nicht an den Server-Prozess durchgereicht
**Symptom:** Server crasht beim Init mit leerem/ungueltigem Token; „0 tools".
**Ursache:** `env`-Werte aus `.mcp.json` erreichen den gespawnten Prozess nicht zuverlaessig.
**Versionen:** [claude-code #1254](https://github.com/anthropics/claude-code/issues/1254) **CLOSED — NOT_PLANNED** (won't-fix → Verhalten bleibt). Workaround dauerhaft noetig.
**FIX:** `${ENV_VAR}`-Expansion nutzen + echten Wert im Shell-Profil/`.env`; oder den Wert direkt in
`args` setzen. Im Server-Code Secrets bevorzugt aus `$HOME/SK/<projekt>/` lesen (Secrets-Regel).

### 5.3 `cwd`-Feld in `.mcp.json` wird komplett ignoriert
**Symptom:** Server startet im falschen Verzeichnis; relative Pfade brechen. Gilt auch fuer `${CLAUDE_PLUGIN_ROOT}`.
**Ursache:** Das `cwd`-Feld ist non-funktional.
**Versionen:** [claude-code #17565](https://github.com/anthropics/claude-code/issues/17565) **CLOSED — NOT_PLANNED** (won't-fix → dauerhaft). Python-Pendant [python-sdk #1520](https://github.com/modelcontextprotocol/python-sdk/issues/1520) CLOSED 2026-03-24.
**FIX:** NICHT auf `cwd` verlassen — im Server `process.env.CLAUDE_PROJECT_DIR` (Node) /
`os.environ["CLAUDE_PROJECT_DIR"]` (Python) lesen, ODER den `roots/list`-Request nutzen, ODER den
Pfad als Tool-Argument uebergeben (so macht es der Anker-Server: `directory` als absoluter Param).

### 5.4 Relative Pfade / `~` in `args` expandieren nicht
**Symptom:** Filesystem-Server findet das Verzeichnis nicht.
**Ursache:** Keine Tilde-/Relativpfad-Expansion; `cwd` unzuverlaessig (5.3).
**Versionen:** per Design.
**FIX:** IMMER absolute Pfade in `args` (`/Users/you/code/project`, nicht `~/code/project`).

### 5.5 Trailing Comma / UTF-8-BOM in `.mcp.json` → ganze Config still ignoriert
**Symptom:** KEIN Server laedt, oft ohne klare Fehlermeldung.
**Ursache:** Ein ueberschuessiges Komma oder ein BOM (3 unsichtbare Bytes am Dateianfang) macht das
JSON ungueltig. Verwandt mit der Hook-BOM-Falle (`claude-hooks.md` 12.1, `python-windows.md`).
**Versionen:** per Design.
**FIX:** Komma entfernen; als „UTF-8 ohne BOM" speichern; vor dem Start validieren:
`python -c "import json;json.load(open('.mcp.json'))"`. JSON nie mit `sed`/`echo`/`Out-File` schreiben.

### 5.6 Project-/User-/Local-Scope-Server werden nicht geladen/erkannt
**Symptom:** `.mcp.json` im Projekt wird ignoriert; Server fehlt in `claude mcp list`, obwohl er funktioniert; local-Scope verschwindet nach Neustart.
**Ursache:** Diverse Scope-Lade-/Listing-Bugs in einzelnen CC-Versionen.
**Versionen:** [claude-code #5963](https://github.com/anthropics/claude-code/issues/5963) (project-scope nicht gelistet) **CLOSED — NOT_PLANNED**; #35144 (user-scope v2.1.77) CLOSED DUPLICATE; #2156/#15215 (project-scope nicht geladen).
**FIX:** Funktion ueber die Session pruefen statt ueber `mcp list`; Server-Approval beim Session-Start
bestaetigen; ggf. Scope wechseln. Konfig-Details: `best-practices/claude-tooling/mcp.md` (Scopes).

### 5.7 `npx`/`bunx` ohne `-y` haengt / scheitert beim Erst-Download
**Symptom:** Server haengt am interaktiven Prompt oder scheitert beim ersten Start (leerer Cache).
**Ursache:** `npx`/`bunx` fragt ohne `-y` interaktiv nach.
**FIX:** Immer `-y` setzen (`["/c","npx","-y","<paket>"]`).

---

## 6. Cross-Platform-Start (Windows vs macOS/Linux)

### 6.1 Windows: `cmd /c`-Wrapper noetig fuer `npx`/`bunx`/`npm`/`pnpm`  ⭐ HAEUFIG
**Symptom:** stdio-Server mit `"command":"npx"` startet nicht, `spawn npx ENOENT`. Direktes `node`/`bun.exe` laeuft.
**Ursache:** `npx`/`bunx`/`npm`/`pnpm` sind auf Windows `.cmd`-Shims. `child_process.spawn()` fuehrt `.cmd` ohne `shell:true` nicht aus (kein Interpreter dazwischen).
**Versionen:** nur nativ Windows (WSL nicht betroffen). Offiziell dokumentiert.
**FIX:** `"command":"cmd"`, das Tool hinter `/c`: `"args":["/c","npx","-y","<paket>"]`. `node`/`bun.exe`
direkt (echte `.exe`) brauchen das NICHT.
**Quelle:** [code.claude.com Troubleshooting](https://code.claude.com/docs/en/troubleshooting), [fransiscuss.com](https://fransiscuss.com/2025/04/22/fix-spawn-npx-enoent-windowsbest-practices/claude-tooling/mcp.md-server/).

### 6.2 `claude mcp add` zerstoert `/c` zu `C:/` (CLI-Parser, won't-fix)
**Symptom:** Nach `claude mcp add ... cmd /c npx ...` steht in der Config `C:/` statt `/c` → Verbindung scheitert.
**Ursache:** Windows-Pfad-Normalisierung wandelt POSIX-`/c` in einen Laufwerks-Pfad.
**Versionen:** [claude-code #20061](https://github.com/anthropics/claude-code/issues/20061) **CLOSED — NOT_PLANNED**; #36808 (cmd /c connection fails) CLOSED NOT_PLANNED 2026-04-25. Verhalten bleibt.
**FIX:** Die Config-Datei direkt schreiben (Re-Parse umgehen); ODER das Tool als direkten `node <abs-pfad-server.js>` ohne `cmd /c` ansprechen (global installiert).

### 6.3 `spawn bun/node ENOENT` — nvm / keg-only-Brew / User-Install nicht im Spawn-PATH
**Symptom:** Im Terminal laeuft alles, via Claude `ENOENT`.
**Ursache:** Claude/GUI spawnt mit anderem Shell-Env; nvm-Init aus `.bashrc`/`.zshrc` wird nicht
geladen; Homebrew `node@22` ist keg-only (nicht nach `/opt/homebrew/bin` gesymlinkt); Bun liegt in `~/.bun/bin`.
**Versionen:** macOS/Linux + Windows-nvm.
**FIX:** Absoluten Pfad eintragen (`/Users/x/.nvm/versions/node/vXX/bin/node`, `~/.bun/bin/bun`),
ODER `ln -s /opt/homebrew/opt/node@22/bin/npx /opt/homebrew/bin/npx`. `bun <abs-pfad-server.ts>` /
`node <abs-pfad-server.js>` als direkter stdio-Runner ist am robustesten.
**Quelle:** [code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp), [oven-sh/bun #12177](https://github.com/oven-sh/bun/issues/12177).

### 6.4 Non-ASCII verstuemmelt (cp1252 statt UTF-8) auf Windows
**Symptom:** Tool-Output mit Umlauten/Emoji kommt kaputt an; teils `UnicodeEncodeError`/„charmap".
**Ursache:** Wenn stdout KEINE Konsole ist (Pipe — genau der MCP-Fall), nutzt Windows die Codepage `CP_ACP` (meist 1252) statt UTF-8.
**Versionen:** Windows (Pipe-Kontext).
**FIX (Node):** `process.stdout.setEncoding('utf8')` (+ stdin/stderr). Python: `PYTHONIOENCODING=utf-8`
bzw. `TextIOWrapper(..., encoding='utf-8')`. Funktionserhaltend, nur Encoding fixiert.
**Quelle:** [servers #2098](https://github.com/modelcontextprotocol/servers/issues/2098), [mcpcat.io Serialization](https://mcpcat.io/guides/debugging-message-serialization-errors/).

### 6.5 `#!/usr/bin/env node`-Server direkt als `command` → auf Windows nicht ausfuehrbar
**Symptom:** Server-Skript direkt gesetzt → „command not found" / nicht ausfuehrbar; PATH-Drift waehlt falsche Node-Version.
**Ursache:** Windows kennt keine Shebangs; `env node` re-resolved den PATH zur Laufzeit.
**Versionen:** Windows primaer; PATH-Drift auch macOS.
**FIX:** Server IMMER als `"command":"node"`/abs. node-Pfad + `"args":["<abs-pfad-server.js>"]` starten — nie das Skript direkt.

### 6.6 Windows-Backslash-Pfade brechen die JSON-Config
**Symptom:** Config laesst sich nicht speichern / Pfad falsch zusammengesetzt.
**Ursache:** Backslashes muessen in JSON escaped werden (`\\`); manche Tools escapen nicht.
**Versionen:** Windows.
**FIX:** Backslashes verdoppeln (`C:\\Users\\...`) ODER Forward-Slashes (`C:/Users/...`, von Node
akzeptiert). Jedes Argument als eigenes Array-Element, nie `"arg1 arg2"` als ein String.

---

## 7. Lifecycle / Crash / Timeout / Handshake / Shutdown / Concurrency

### 7.1 Server-Crash ohne Recovery — stdio-Server reconnecten NICHT automatisch  🆕 OFFEN
**Symptom:** Server-Prozess stirbt (Exception/OOM), Client zeigt nur „disconnected", kein Auto-Restart; die Tool-Liste ist fuer die ganze Session weg.
**Ursache:** stdio-Server bekommen kein Auto-Reconnect (anders als HTTP/SSE mit backoff). Stirbt der Child-Prozess, ist er weg.
**Versionen:** [claude-code #43177](https://github.com/anthropics/claude-code/issues/43177) **OPEN** (stdio never auto-reconnect); auch #1049 (stdio client crasht `-32000` wenn Server exitet) OPEN.
**FIX:** (1) Server-seitig `uncaughtException`/`unhandledRejection`-Handler die loggen statt crashen (4.4), kritische Pfade in `try/catch` mit Fehler-Response statt Prozess-Tod. (2) Optional Supervisor-Wrapper der bei Exit neu startet. Funktionalitaet bleibt — Fehler werden abgefangen.

### 7.2 Tool-Call timeoutet nach 60s — `resetTimeoutOnProgress` ist opt-in
**Symptom:** Langlaufendes Tool bricht nach exakt 60s mit `-32001 Request timed out` ab, obwohl ein hoeherer `timeout` gesetzt schien.
**Ursache:** Default `DEFAULT_REQUEST_TIMEOUT_MSEC = 60000`. Progress-Notifications resetten den
Timer nur, wenn `resetTimeoutOnProgress` aktiv ist — defaultet `false`.
**Versionen:** [typescript-sdk #245](https://github.com/modelcontextprotocol/typescript-sdk/issues/245) ✅ CLOSED 2025-09-17 (PR #849, ~1.18). Der Flag funktioniert auf 1.27.1 — bleibt aber **opt-in** (Gotcha per Design).
**FIX (funktionserhaltend):** Beim Call `{ timeout, resetTimeoutOnProgress:true, maxTotalTimeout }`
setzen UND einen `onprogress`-Callback mitgeben (das SDK injiziert den `progressToken` nur dann);
der Server muss `notifications/progress` periodisch senden. Immer ein Max-Timeout als Schutz behalten.
**Quelle:** [mcpcat.io -32001](https://mcpcat.io/guides/fixing-mcp-error-32001-request-timeout/).

### 7.3 Init-/Startup-Timeout: langsamer Server-Start blockt den Handshake → FAILED
**Symptom:** Server mit schwerem Start (DB oeffnen, Modelle/Chromium laden, npm-install on first run) ueberschreitet das Init-Fenster und wird fuer die ganze Session als FAILED markiert.
**Ursache:** Teure Arbeit passiert synchron im Init oder im ersten `tools/list` (z.B. Tool-Liste dynamisch aus DB/FS).
**Versionen:** Lazy-Init Feature-Request [claude-code #26666](https://github.com/anthropics/claude-code/issues/26666) CLOSED DUPLICATE.
**FIX (funktionserhaltend):** (1) Tool-Definitionen als statische Konstante zur Import-Zeit (keine
DB-/FS-Arbeit im `list_tools`-Handler — cachen). (2) Schwere Init **lazy** beim ersten echten Tool-
Call oder im Hintergrund nach `initialized`, NICHT vor der `initialize`-Response. (3) Connection-Pool
nach dem Boot primen. Alle Tools bleiben verfuegbar, nur die Reihenfolge aendert sich.
**Anker-Hinweis:** `index.ts` baut den Store lazy beim ersten Call (`getStore`) — gut.

### 7.4 protocolVersion-Mismatch beendet den Handshake
**Symptom:** Verbindung scheitert direkt bei `initialize`; nach Client-Update reagiert ein vorher
funktionierender Server nicht mehr (`-32602 Unsupported protocol version`).
**Ursache:** Client schickt neuere `protocolVersion`, Server echo't eine alte, die der Client nicht
unterstuetzt → Client disconnectet. Spec: Server MUSS bei Unsupported eine von IHM unterstuetzte
Version zurueckgeben (nicht den Client-Wert echoen).
**Versionen:** [claude-code #768](https://github.com/anthropics/claude-code/issues/768) **CLOSED — NOT_PLANNED**. Betrifft jedes SDK das eine Version hartkodiert.
**FIX:** SDK aktuell halten (verhandelt mehrere Revisionen). Bei eigenem Transport mehrere
Versionen akzeptieren und die Echo-Regel einhalten. Nichts entfernen — Negotiation robust machen.
**Quelle:** [Spec Lifecycle 2025-06-18](https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle).

### 7.5 `initialize` nicht implementiert / Calls vor abgeschlossenem Handshake
**Symptom:** „handshaking with MCP server failed: connection closed"; oder der Server schickt Requests, bevor `initialized` kam.
**Ursache:** (a) Server implementiert nur Tools, kein `initialize`. (b) Reihenfolge verletzt:
Client SOLL vor der initialize-Response nur `ping` senden; Server SOLL vor `notifications/initialized` nur `ping`/`logging` senden.
**Versionen:** [servers #2691](https://github.com/modelcontextprotocol/servers/issues/2691) u.a.
**FIX:** Vollen 3-Schritt-Handshake (initialize-Request → -Response mit capabilities → auf
`notifications/initialized` warten), erst danach Tool-/Resource-Requests verarbeiten. Keine
proaktiven Server-Requests vor `initialized`. Mit dem offiziellen SDK automatisch korrekt.

### 7.6 Blockierender Sync-Code im Handler friert den Event-Loop → alle Calls haengen
**Symptom:** Bei gleichzeitigen Tool-Calls reagiert der Server nicht mehr; ein Call „haengt forever"; andere timeouten.
**Ursache:** CPU-lastige/synchrone I/O (`readFileSync` auf grosse Dateien, dichte Loops, sync Crypto/Parsing) blockiert den Single-Thread-Event-Loop.
**Versionen:** alle Node/Bun-Server.
**FIX (funktionserhaltend):** Blocking durch async ersetzen (`fs.promises`, async DB-Driver),
CPU-Heavy in Worker-Threads, grosse Loops mit `setImmediate` chunken. Funktion bleibt, nur nicht-blockierend.
**Quelle:** [Node — Don't Block the Event Loop](https://nodejs.org/en/docs/guides/dont-block-the-event-loop).

### 7.7 Kein graceful Shutdown: SIGTERM/SIGINT killt vor Ressourcen-Cleanup
**Symptom:** Beim Beenden gehen In-Flight-Writes verloren / DB-Lock bleibt / WAL nicht gecheckpointet; oder der Server haengt beim Shutdown und muss SIGKILLt werden.
**Ursache:** Kein SIGTERM/SIGINT-Handler oder async-Cleanup wird nicht awaited vor `process.exit()`.
stdio-Shutdown-Sequenz: Client schliesst stdin → wartet → SIGTERM → (reasonable time) → SIGKILL.
**Versionen:** alle Node/Bun-Server.
**FIX:** `process.on('SIGTERM'|'SIGINT', async () => { await server.close(); await db.close(); process.exit(0) })`.
Async-Cleanup awaiten; Watchdog-Timeout (5–10s) der notfalls `process.exit(1)` erzwingt.
**Anker-Hinweis:** `index.ts` hat SIGTERM/SIGINT → `closeCachedStore()` + `process.exit(0)`. Das
schliesst den Store synchron (ok), beachtet aber kein In-Flight-Handling — fuer schwere DB-Writes ggf. await + Watchdog ergaenzen.

### 7.8 Race auf gemeinsamem State bei mehreren gleichzeitigen Tool-Calls
**Symptom:** Sporadisch falsche/korrupte Ergebnisse, wenn mehrere Calls denselben Server-State (Counter, Cache, offene Transaktion, geteilter DB-Cursor) anfassen.
**Ursache:** MCP erlaubt mehrere In-Flight-Requests (eigene `id`); ohne Synchronisation greifen Handler nebenlaeufig auf shared mutable State zu.
**Versionen:** alle.
**FIX:** Pro-Request isolierter State; geteilte Ressourcen ueber Connection-Pool (ein Connection pro
Request); kritische Abschnitte serialisieren (Mutex/Queue). Concurrency bleibt, nur datensicher.
**Eigener Vorfall (2026-05-24, code-search):** `.reindex.lock` wurde von der `.ps1` (detached
Start-Process) nie freigegeben → bis 30 Min blockiert. Fix: `cleanupLock()` im Prozess selbst
(`session-reindex.ts`) vor `process.exit`, nicht im startenden Hook. Lehre: Detached-Prozesse
muessen ihren Lock SELBST freigeben.

---

## 8. SDK-Version / Protokoll-Revision / Auth

### 8.1 UriTemplate-ReDoS (CVE-2026-0621) — in 1.27.1 bereits GEFIXT
**Symptom:** Boesartiger Input gegen Resource-URI-Templates blockiert den Event-Loop (DoS); `npm audit` meldet HIGH.
**Ursache:** `partToRegExp()` erzeugte fuer „exploded" Template-Variablen (`{/id*}`, `{?tags*}`) ein Regex mit verschachtelten Quantoren → catastrophic backtracking.
**Versionen:** alle SDK **<1.25.2**. [typescript-sdk #965](https://github.com/modelcontextprotocol/typescript-sdk/issues/965)/#1334 ✅ CLOSED, gefixt in **1.25.2** (2026-01-07). **Anker 1.27.1 ist sicher — NICHT downgraden.**
**FIX:** SDK ≥1.25.2 (1.27.1 erfuellt das). [GHSA-8r9q-7v3j-jr4g](https://github.com/advisories/GHSA-8r9q-7v3j-jr4g).

### 8.2 Transitive Dependency-CVEs (CI-Breaker)  🆕 OFFEN
**Symptom:** `npm audit --audit-level=high` bricht CI; keine Laufzeit-Fehlfunktion per se.
**Ursache:** SDK zieht verwundbare Transitive (ajv/fast-uri/hono/express-rate-limit/ip-address je nach Version).
**Versionen:** [typescript-sdk #2042](https://github.com/modelcontextprotocol/typescript-sdk/issues/2042) **OPEN** (verifiziert 2026-06-03); fuer 1.27.x gegenpruefen.
**FIX:** `overrides`/`resolutions` auf gepatchte Sub-Versionen setzen statt das SDK zu entfernen; SDK-Update abwarten.

### 8.3 JSON-RPC-Batching: 2025-03-26 eingefuehrt, 2025-06-18 wieder ENTFERNT
**Symptom:** Client schickt ein Array von Requests → Server rejectet ab Revision 2025-06-18.
**Ursache:** Whipsaw-Breaking-Change in der Spec.
**Versionen:** Revision 2025-06-18+.
**FIX:** Jeden JSON-RPC-Call als eigene Message senden; nie Arrays/Batches bauen.
**Quelle:** [Spec-Changelog 2025-06-18](https://modelcontextprotocol.io/specification/2025-06-18/changelog).

### 8.4 `MCP-Protocol-Version`-Header: ab 2025-06-18 Pflicht + Header/Body-Mismatch  🆕 OFFEN
**Symptom:** Folge-Requests ueber HTTP schlagen nach erfolgreichem `initialize` fehl; oder Header ≠ Body-`protocolVersion` wird still akzeptiert.
**Ursache:** Spec verlangt bei HTTP den `MCP-Protocol-Version: <version>`-Header auf ALLEN
Folge-Requests. Zusaetzlich validiert der Streamable-HTTP-Transport Header gegen Body nicht.
**Versionen:** Revision 2025-06-18+; [typescript-sdk #2108](https://github.com/modelcontextprotocol/typescript-sdk/issues/2108) **OPEN**.
**FIX:** Client setzt den verhandelten Header auf jedem Request; Server tolerant validieren (fehlenden
Header auf 2025-03-26 defaulten statt hart crashen); bei Mismatch optional eigene 400-Middleware.

### 8.5 `McpServer` ueberschreibt Tool-Capabilities nach `connect` (dynamische Registrierung)
**Symptom:** Bei Construction gesetzte Capabilities werden nach `connect` ueberschrieben; dynamisches Tool-Registrieren blockiert; Client sieht falsche/keine Capability.
**Ursache:** `McpServer` re-deklariert tool-Capabilities beim ersten `registerTool`.
**Versionen:** Grundbug [typescript-sdk #1488](https://github.com/modelcontextprotocol/typescript-sdk/issues/1488) ✅ CLOSED 2026-02-10 (in 1.27.1 adressiert). Das *dynamische* Re-Registrieren nach connect [typescript-sdk #893](https://github.com/modelcontextprotocol/typescript-sdk/issues/893) **OPEN**.
**FIX:** Tools VOR `connect` registrieren; bei dynamischem Bedarf Capabilities explizit erneut deklarieren bzw. SDK-Workaround abwarten.

### 8.6 OAuth / Auth (Streamable HTTP) — haeufige Fehlimplementierungen
**Symptom:** 401 trotz gueltigem Token; Client kann den Auth-Flow nicht starten.
**Ursache/FIX (funktionserhaltend):**
- **RFC 8707 Resource-Indicator-Mismatch:** `resource` (Token-Request) ≠ exakte MCP-Server-URL → audience-fail. → `resource`/Audience exakt auf die Server-URL setzen; keine generischen Audiences; `authInfo.scopes` in jedem Handler pruefen.
- **401 ohne `WWW-Authenticate` + Protected-Resource-Metadata (RFC 9728):** kein Discovery moeglich. → `/.well-known/oauth-protected-resource` publizieren; 401 mit `WWW-Authenticate: Bearer ... resource_metadata=...`.
- **Klassiker:** Token-Passthrough (fremdes Token weiterreichen), fehlende Audience-Validierung, Redirect-URI-Mismatch, fehlender CSRF-`state`, Tokens im Klartext. → Audience hart pruefen, `state` erzwingen, Tokens nie im Klartext, **PKCE** ist in OAuth 2.1 Pflicht.
- **Dynamic Client Registration (RFC 7591) ist im aktuellen Spec-Draft DEPRECATED** (Re-Recherche 2026-07-02) —
  nur noch fuer Abwaertskompatibilitaet mit AS ohne CIMD. Neu: **Client ID Metadata Documents (CIMD)** — eine
  HTTPS-URL dient als `client_id`, von der der AS die Client-Metadaten abruft. Praxis-Falle: DCR wird real kaum
  unterstuetzt (~4% der AS-Endpunkte) → fuer breite Kompatibilitaet CIMD einplanen, DCR nicht als einzigen Weg.
**Quelle:** [MCP Authorization Tutorial](https://modelcontextprotocol.io/docs/tutorials/security/authorization), [Stytch-Guide](https://stytch.com/blog/MCP-authentication-and-authorization-guide/), MCP-Spec-Draft Authorization (re-verifiziert 2026-07-02).

### 8.7 ESM: lokale Imports ohne `.js`-Endung brechen unter Bun/Node
**Symptom:** `ERR_MODULE_NOT_FOUND` / „Must use import to load ES Module".
**Ursache:** ESM (Bun + Node NodeNext) verlangt explizite `.js`-Endungen; TS fuegt sie nicht selbst hinzu.
**Versionen:** Node/Bun ESM.
**FIX:** `"type":"module"` in package.json, `"module"/"moduleResolution":"NodeNext"` in tsconfig,
lokale Imports MIT `.js`. SDK-Subpath-Imports immer mit `.js` (`@modelcontextprotocol/sdk/server/mcp.js`).
**Anker-Hinweis:** `index.ts` importiert korrekt mit `.js`-Endungen und `"type":"module"`.

### 8.8 Ausblick: SDK v2 (28.07.2026) + Spec-Revision 2026-07-28 — was auf Frank zukommt 🆕
**Kontext:** Der `mcp-code-search`-Server laeuft auf **v1 (1.27.1)** und bleibt es bis zu einer bewussten
Migration. Die folgenden Aenderungen sind **Migrations-Vorwissen**, kein akuter Bug — aber sie aendern das
Fundament, darum HIER dokumentiert.
- **SDK v2 (`v2.0.0-beta.1`, 30.06.2026; stabil geplant 28.07.2026):**
  - **Package-Split:** `@modelcontextprotocol/sdk` → `@modelcontextprotocol/server` + `@modelcontextprotocol/client`
    (+ optionale Adapter fuer Express/Hono/Fastify/Node-`http`). Imports/Deps aendern sich beim Umstieg.
  - **„Bring your own schema":** `inputSchema`/`outputSchema` akzeptieren jede **Standard-Schema**-Lib
    (Zod v4, ArkType nativ; Valibot via Adapter; pures JSON Schema via `fromJsonSchema`). **Zod ist nicht mehr Pflicht.**
  - **Serving = ein Aufruf:** `serveStdio()` (lokal) / `createMcpHandler()` (HTTP, Web-Standard `Request`/`Response`,
    laeuft auf Node/Bun/Deno/Workers). Typisierter `ctx` (Logging/Progress/Cancellation/Elicitation) statt v1-`extra`-Bag.
  - **Migration:** `npx @modelcontextprotocol/codemod@beta v1-to-v2 .`. **v1.x bekommt ≥6 Monate weiter Bug-/Security-Fixes.**
- **Spec-Revision 2026-07-28 (Release Candidate, groesste Revision seit Launch, BREAKING):**
  - **Stateless Core:** `initialize`/`initialized`-Handshake entfernt (SEP-2575), `Mcp-Session-Id` entfernt (SEP-2567) —
    jede Anfrage self-contained, Sessions nur noch opt-in (Explicit-Handle-Pattern, IDs vom Modell weitergereicht).
  - **Multi-Round-Trip statt SSE-Stream:** Elicitation/Server→Client via `InputRequiredResult` + `requestState` (SEP-2322);
    Server-Requests nur waehrend aktiver Verarbeitung einer Client-Request (SEP-2260, jetzt Requirement).
  - **Routing/Caching-Header:** `Mcp-Method`/`Mcp-Name` Pflicht (SEP-2243); `ttlMs`/`cacheScope`-Hints (SEP-2549).
  - **Deprecatet: Roots, Sampling, Logging.** Extensions first-class (Reverse-DNS-IDs), Tasks als Extension,
    „MCP Apps" (server-gerenderte UIs), Full JSON Schema 2020-12 fuer Tools, Authorization-Hardening (OAuth/OIDC).
- **Konkreter v1-Bug, den v2 fixt — Output-Schema-Crash (#1308, 16.12.2025):** Tool-Output-Validierung crasht
  (`Cannot read properties of undefined (reading '_zod')`), sobald `outputSchema` **kein plain `z.object({...})`** ist —
  `.optional()`, `.nullable()`, `.nullish()`, `z.union()` schlagen fehl (silent fail → `isError:true`). Root Cause:
  `normalizeObjectSchema` liefert `undefined` fuer Nicht-Object-Schemas. **Auf v1.x (inkl. 1.27.1) AKTIV**, in v2 gefixt.
  **Workaround auf v1:** `outputSchema` immer als flaches `z.object({...})` halten (kein optional/nullable/union auf oberster Ebene);
  Optionalitaet in einzelne Felder verlagern. Verwandt: #969 (discriminatedUnion), #594 (nur non-nullable).
**Quelle:** github.com/modelcontextprotocol/typescript-sdk (v2-Releases, Issue #1308), blog.modelcontextprotocol.io (2026-07-28 RC).

---

## 9. Fix-Status — was auf MCP TS-SDK 1.27.1 schon behoben ist (gh-verifiziert 2026-06-03)

> **1.27.1 = 2026-02-24.** Issues, die NACH diesem Datum geschlossen wurden, sind auf 1.27.1
> noch AKTIV (Fix erst ab 1.28.0/1.29.0). Status hart per `gh issue view` geprueft.

| Frueherer Bug | Repo/Issue | Status | In 1.27.1? | Almanach-Bezug |
|---------------|-----------|--------|-----------|----------------|
| zod v4 `_parse`-Crash | ts-sdk #925 | CLOSED 2025-11-21 (Fix ~1.18) | ✅ gefixt | 3.1 |
| UriTemplate ReDoS (CVE-2026-0621) | ts-sdk #965/#1334 | CLOSED (Fix 1.25.2) | ✅ gefixt | 8.1 |
| draft-07 statt 2020-12 | ts-sdk #745 | CLOSED 2025-12-05 | ✅ gefixt | 3.4 |
| zod-Description nicht propagiert | ts-sdk #1143 | CLOSED 2025-11-28 | ✅ gefixt | 3.10 |
| `inputSchema {}` vs `undefined` | ts-sdk #458 | CLOSED 2025-05-13 | ✅ gefixt | 3.8 |
| Stateless-Mode nicht unterstuetzt | ts-sdk #340 | CLOSED 2025-04-18 | ✅ gefixt | 2.4 |
| Capabilities-Overwrite (Grundbug) | ts-sdk #1488 | CLOSED 2026-02-10 | ✅ gefixt | 8.5 |
| **discriminatedUnion → leeres Schema** | ts-sdk #1643 | CLOSED **2026-03-30** | ❌ **noch aktiv** (ab 1.28/1.29) | **3.2** |
| **unknown tool → isError statt -32601** | ts-sdk #1510 | CLOSED **2026-03-26** | ❌ **noch aktiv** (ab 1.28/1.29) | **4.3** |
| Idle Session Timeout (Streamable HTTP) | ts-sdk #812 | CLOSED 2026-03-23 | ❌ ggf. ab 1.28 | 2.8 |
| Timeout-Reset-on-Progress (Flag) | ts-sdk #245 | CLOSED 2025-09-17 | ✅ Flag da (opt-in) | 7.2 |

### Noch NICHT gefixt (Workaround bleibt aktiv) — OPEN bzw. NOT_PLANNED
- **OPEN (echte Bugs):** ts-sdk #1944 (406 JSON-only Accept, 2.3), #893 (dynamische Capabilities, 8.5),
  #2108 (Protocol-Version Header/Body, 8.4), #2042 (transitive CVEs, 8.2), #1028 (manueller ListTools
  ohne inputSchema, 3.9), #1658 (Session-Rekonstruktion, 2.5), #1049 (stdio-Crash bei Server-Exit, 7.1),
  #1308 (**outputSchema non-object `_zod`-Crash** auf v1.x, 8.8 — in v2 gefixt), #852 (Browser-Session-Reuse); python-sdk #396 (Exception-Handling, 4.8), #2433 (CRLF Windows, 1.5),
  #2278 (SSE-Deprecation-Tracking, 2.1); claude-code #58510 (Windows Plugin-npx, 6.1), #43177 (kein
  stdio-Auto-Reconnect, 7.1).
- **NOT_PLANNED (won't-fix → Workaround DAUERHAFT):** claude-code #1254 (env nicht durchgereicht, 5.2),
  #17565 (`cwd` ignoriert, 5.3), #768 (protocolVersion-Validierung, 7.4), #20061 (`claude mcp add`
  zerstoert `/c`, 6.2), #36808 (Windows cmd /c, 6.2), #5963 (project-scope nicht gelistet, 5.6).

**Methodik:** Alle GitHub-Stati per `gh issue view <nr> --repo <org>/<repo> --json state,closedAt,stateReason`
hart geprueft (gh authentifiziert, 2026-06-03). „per Design"-Fallen (stdout-Hygiene, isError-vs-Error,
Tool-Name-Regex, cmd /c, ESM-`.js`) gelten versionsunabhaengig weiter.

---

## 10. Abgrenzung zu `claude-hooks.md` und `best-practices/claude-tooling/mcp.md`

Drei Dateien beruehren „MCP", mit klarer Aufgabentrennung:

| Datei | Perspektive | Was hier steht |
|-------|-------------|----------------|
| **`bugs/claude-tooling/mcp-server.md`** (diese) | **Server BAUEN — Fallen** | Transport-Impl, Tool-Schema, Error-Handling, Lifecycle, `.mcp.json`-Registrierung, SDK-Versions-Bugs |
| [`best-practices/claude-tooling/mcp-server.md`](../../best-practices/claude-tooling/mcp-server.md) | **Server BAUEN — richtige Seite** | Positive Gegenseite zu DIESER Datei: empfohlene Arbeitsweise, Do's & Don'ts (Transport-Wahl, Tool-Schema-Design, Fehler-Propagation, Setup, Sicherheit, Testing). Bezugstabelle in Sektion 11 |
| [`bugs/claude-tooling/claude-hooks.md`](claude-hooks.md) | **Claude-Code-Hooks** | Hooks generell. MCP nur am Rand: MCP-Matcher (`mcp__server__.*`, 9.2), MCP-Tool-als-Hook (14.1), absolute Pfade/BOM in `settings.json`/`.mcp.json` (12.1, 12.5) |
| `best-practices/claude-tooling/mcp.md` | **Server KONFIGURIEREN/VERBINDEN** (Harness-Seite) | Transport-Wahl in Claude Code, Scopes (local/project/user), `.mcp.json`-ENV-Expansion, OAuth-Setup, Managed MCP, Tool-Search |

**Ueberschneidungspunkte (bewusst, mit Querverweis statt Duplikat):**
- `.mcp.json` BOM/Trailing-Comma/absolute-Pfade: hier aus Server-Bringup-Sicht (5.1, 5.5), in
  `claude-hooks.md` aus Config-Datei-Sicht (12.1), in `best-practices/claude-tooling/mcp.md` aus Konfigurations-Best-Practice-Sicht.
- SSE-Deprecation/Streamable HTTP: hier die Server-Impl-Bugs (2.x), in `best-practices/claude-tooling/mcp.md` die Client-/Reconnect-Hinweise (inkl. v2.1.153-Regression-Fix).

**Faustregel:** Schreibe ich Server-CODE (Transport, Tools, Handler, Lifecycle) → hier. Konfiguriere/
verbinde ich einen Server in Claude Code → `best-practices/claude-tooling/mcp.md`. Baue ich einen Hook (der evtl. ein MCP-Tool
matcht) → `claude-hooks.md`.

---

## Pflicht-Checkliste vor MCP-Server-Arbeit
- [ ] Diese Datei komplett gelesen, Stand gegen die SDK-Version aus `package.json`/`Cargo.toml` abgeglichen?
- [ ] **stdio:** kein `console.log`/`print`/Pretty-Print auf stdout — alle Logs auf stderr (1.1)?
- [ ] `.mcp.json`: absolute Pfade bzw. `cmd /c` fuer npx/bunx, `-y` gesetzt, JSON valide, kein BOM (5.1, 5.5, 6.1)?
- [ ] Tool-Schema Top-Level `z.object`, kein discriminatedUnion (auf 1.27.1 noch leer!), Name `[a-zA-Z0-9_]{1,64}` (3.2, 3.7)?
- [ ] `server.tool` → `registerTool` migriert (3.3)?
- [ ] Fehler nie verschluckt: erwartbar → `isError:true`, Protokoll → JSON-RPC-Error (4.1, 4.2)?
- [ ] `uncaughtException`/`unhandledRejection`-Handler installiert (4.4, 7.1)?
- [ ] Langlaufende Tools: `resetTimeoutOnProgress:true` + `onprogress` + periodische Progress-Notifications (7.2)?
- [ ] Schwere Init lazy, nicht vor der `initialize`-Response (7.3)?
- [ ] Graceful Shutdown (SIGTERM/SIGINT → Cleanup awaiten, Watchdog) (7.7)?
- [ ] Cross-Platform: UTF-8-Encoding explizit, Server als `node/bun <abs-pfad>`, kein Shebang-Direktaufruf (6.4, 6.5)?
- [ ] ESM: `"type":"module"`, NodeNext, lokale Imports mit `.js` (8.7)?
- [ ] SDK ≥1.25.2 (ReDoS-sicher); fuer discriminatedUnion/unknown-tool-Fix ≥1.28/1.29 erwogen (8.1, 3.2, 4.3)?
- [ ] HTTP-Transport: Accept-Header, Origin-Validierung, CORS-Expose `Mcp-Session-Id`, Session-404-Reinit (2.x)?

---

## 🔗 11. Kopplung zur Best-Practices-Datei (wechselseitige Bezugstabelle)

Bug-Almanach (diese Datei) ↔ Best-Practices [`best-practices/claude-tooling/mcp-server.md`](../../best-practices/claude-tooling/mcp-server.md). Die identische Tabelle steht auch dort. Links die *richtige Arbeitsweise*, rechts die *Falle, die sie verhindert*.

| Best-Practice-Abschnitt (`best-practices/claude-tooling/mcp-server.md`) | Zugehoeriger Bug-Almanach-Abschnitt (hier) |
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

*Bei jedem neu erlebten Bug hier ergaenzen UND die Best-Practice-Gegenseite aktualisieren, Bezugstabelle synchron halten (Compound Intelligence, Direktive #1).*
