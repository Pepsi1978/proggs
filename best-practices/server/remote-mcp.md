# Remote-MCP-Server ueber HTTP (Streamable HTTP) — Best Practices (Stand 2026-06-24)

> **Wie man einen REMOTE-MCP-Server (Python, Streamable HTTP, mit/ohne Auth) von vornherein richtig
> baut und betreibt.** Bezug: der Live-Server `sb-mcp` (`~/proggs/second-brain-server/mcp-server/server.py`),
> der Franks "zweites Gehirn" als MCP-Werkzeuge fuer Claude Code UND OpenCode bereitstellt.
> Funktionserhaltend (Direktive #3).
>
> **Abgrenzung (vier MCP-Dateien, klar getrennt — NICHT hier doppeln):**
> - **DIESE Datei** = der **Remote-HTTP-MCP-Server selbst** in **Python (`mcp.server.fastmcp`, SDK v1.x)**:
>   Transport, Session, Auth, Netz, Tool-Schema, Fehler, Versionierung, Reverse-Proxy.
> - `best-practices/claude-tooling/mcp-server.md` = **MCP-Server bauen allgemein** (Server-Autor-Seite,
>   TS-SDK/stdio-Fokus, lokaler Single-User). Die generischen Server-Autor-Prinzipien (Logging, Lifecycle,
>   Tool-Registrierung) stehen DORT — hier nur das HTTP-Remote-Spezifische.
> - `best-practices/claude-tooling/mcp.md` = **MCP in Claude Code KONFIGURIEREN/VERBINDEN** (Harness-Seite).
> - `best-practices/second-brain/multi-client-zugriff.md` = **Memory-Multi-Client-Architektur** (ein Store,
>   viele Clients; MCP + REST parallel; Auth-Wege). Hier dagegen der HTTP-Server-Bau selbst.
>
> **WICHTIG — zwei "FastMCP":** Es gibt das SDK-interne `mcp.server.fastmcp.FastMCP` (offizielles Python-SDK
> `mcp`, **v1.x**, das `sb-mcp` nutzt) UND das eigenstaendige Paket `fastmcp` (PrefectHQ/jlowin, **v2.x/3.x**,
> Doku gofastmcp.com). Sie teilen die `@mcp.tool()`-Decorator-Idee, sind aber NICHT versionsgleich. Funde
> aus gofastmcp.com (output_schema ab 2.10, icons ab 2.13 …) gelten fuer das v2/v3-Paket — fuer das
> SDK-interne v1.x gegen das jeweilige `mcp`-Release gegenpruefen.
>
> **Anker (live ermittelt):** Projekt `mcp>=1.27,<2` (httpx 0.27–0.29); neueste `mcp`=1.28.0 (2026-06-16);
> Spec-Revisionen **2025-03-26** (Streamable HTTP eingefuehrt) → **2025-06-18** (Auth-Spec, `MCP-Protocol-Version`-
> Header, structured output, RFC 8707, JSON-RPC-Batching entfernt) → **2025-11-25** (neueste stabile) →
> **2026-07-28** (Release Candidate, stateless-remote, breaking). Quellen: offizielle modelcontextprotocol.io +
> github.com/modelcontextprotocol/python-sdk (Engine-B-`:online`-Recherche 2026-06-24, viele Kernpunkte mit
> direktem Spec-Zitat) + Sekundaerquellen (klar als `extern` gelabelt). `offiziell`/`extern` pro Punkt.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Regel | Detail |
|---|-------|--------|
| 1 | **Transport: Streamable HTTP, NIE SSE** | Fuer neue Remote-Server `mcp.run(transport="streamable-http")`. HTTP+SSE ist seit Spec 2025-03-26 abgeloest. Ein `/mcp`-Endpoint (POST Client→Server, optional GET/SSE Server→Client), JSON-RPC UTF-8. |
| 2 | **Session: `Mcp-Session-Id` + 404→Re-Init** | Server vergibt die ID in der `initialize`-Antwort, Client schickt sie bei JEDEM Folge-Request. Server MAY die Session beenden → MUSS dann 404 antworten → Client MUSS neu initialisieren. Mit Re-Init/Neustart rechnen (Clients starten oft frische Sessions). |
| 3 | **Auth: spec = OAuth 2.1, pragmatisch = abwaegen** | HTTP-Transport SHOULD die Auth-Spec nutzen (OAuth 2.1 + PKCE, MCP=Resource-Server, RFC 9728 PRM, RFC 8707 Audience). Stdio: NIE OAuth (Env-Creds). Single-User hinter VPN: statischer Bearer ist unter 2025-06-18 ("SHOULD") kein harter Bruch, verliert aber Audience-Binding/Tool-Scopes — und Nov-2025 verschaerft auf "MUST". |
| 4 | **Netz: DNS-Rebinding-Schutz + Bind eng** | `TransportSecuritySettings(enable_dns_rebinding_protection=True, allowed_hosts=[…])` (Origin/Host-Allowlist; CVE-2025-66416, Fix ab `mcp` 1.23.0). An `127.0.0.1`/VPN-IP binden, nie blanko `0.0.0.0`. Kein Wildcard-CORS. TLS via Reverse-Proxy, wenn oeffentlich. |
| 5 | **Tool-Schema: klar, eng, annotiert** | Docstring → `description` (Pflichtfeld). `Annotated[..., Field(description, pattern, min/max, examples)]`. Tool-Annotations setzen: `readOnlyHint` (Lese-Tools), `destructiveHint` (Loesch-Tools). snake_case-Namen. Wenige klare Tools. |
| 6 | **Fehler: Protokoll vs. fachlich trennen** | Protokollfehler → JSON-RPC-Error (-32700…-32603). Fachlicher Tool-Fehler → Tool-Result mit `isError` (LLM kann sich korrigieren). Nie Internas (Stacktrace/Token) an den Client; intern strukturiert loggen. |
| 7 | **Streaming hinter Reverse-Proxy** | SSE/Streaming-Pufferung AUS: Caddy `flush_interval -1` (≠0 Pflicht fuer SSE), nginx `proxy_buffering off` + lange `proxy_read_timeout` + `proxy_http_version 1.1`. Client-Default-Timeout 60 s (max 3600 s). |
| 8 | **Version: `MCP-Protocol-Version`-Header** | Ab Spec 2025-06-18: Client MUSS den Header auf jedem HTTP-Request senden; Server MUSS bei unbekannter Version mit `400` ablehnen; fehlt er, SHOULD-Fallback `2025-03-26`. SDK pinnen: `mcp>=1.27,<2` (v2 ist Alpha mit API-Umbau). |
| 9 | **Stateful nur wenn noetig** | Single-Server-HTTP: In-Memory-State ok. Verteilt/serverlos: State extern (Redis) je `Mcp-Session-Id`, ODER stateless (`stateless_http`/`json_response`). Ab Spec 2026-07-28 wird stateless-remote (Round-Robin-LB, `tools/list`-Caching) offiziell. |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug
> Der zugehoerige Bug-Almanach **bugs/server/remote-mcp.md ist noch anzulegen** (per Skill
> `bug-almanach-recherche`, mit `gh`-Fix-Status-Pruefung) — siehe Schluss-Notiz. Bis dahin: verwandte
> Bug-Almanache sind `bugs/claude-tooling/mcp-server.md` (MCP allgemein, §3.13 DNS-Rebinding) und
> `bugs/opencode/server-agent-remote-mcp.md` (Remote-Connect aus OpenCode/Claude). Sobald der
> Almanach bugs/server/remote-mcp.md existiert, hier die wechselseitige Abschnitts-Tabelle eintragen.

---

## §1 — Transport: Streamable HTTP (offiziell)

- **Nur zwei Standard-Transporte** (Spec 2025-03-26/transports, `offiziell`): **stdio** und **Streamable HTTP**.
  Das alte **HTTP+SSE** ist seit 2025-03-26 abgeloest ("replaced the HTTP+SSE transport mechanism with
  Streamable HTTP starting from protocol version 2025-03-26", brightdata `extern`; Deprecation kommuniziert
  Mai 2025, auth0 `extern`). **Fuer einen NEUEN Remote-Server NIE HTTP+SSE waehlen.**
- **Ein `/mcp`-Endpoint:** POST (Client→Server, JSON-RPC) + **optionales GET** (Server→Client als SSE-Stream),
  alle Nachrichten UTF-8-JSON-RPC. HTTP/2 ist **nicht** Pflicht (funktioniert mit Starlette/ASGI auch ohne;
  github discussion #598 `extern`).
- **Python-SDK:** `from mcp.server.fastmcp import FastMCP` → `mcp.run(transport="streamable-http")`. Alternativ
  als ASGI-Sub-App einhaengen: `app.mount("/mcp-server", mcp.http_app(path="/mcp"))`.
- **SSE-Backward-Compat** nur fuer alte Clients noetig, die Streamable HTTP noch nicht sprechen — keine
  Spec-Pflicht; im Zweifel weglassen.

> **sb-mcp:** nutzt `mcp.run(transport="streamable-http")` — korrekt (Kommentar im Code: "NIE SSE"). Vorbildlich.

## §2 — Session-Management (offiziell + extern)

- **`Mcp-Session-Id`-Header:** Server vergibt die ID in der `initialize`-Antwort; Client schickt sie bei
  **jedem** Folge-Request mit (sonst HTTP 400 "Missing session ID"). Casing beachten: ausgehend
  `Mcp-Session-Id`, in Node/Express liest man `mcp-session-id` (lowercase). Beim `streamablehttp_client`
  verwaltet das SDK die ID automatisch.
- **`initialize`-Handshake zuerst:** Client `initialize` → Server antwortet mit Session-ID + Capabilities;
  danach `tools/list`, `tools/call` (jeweils mit Header).
- **Session-Ende → 404 → Re-Init (offiziell):** Der Server **MAY** die Session jederzeit beenden; danach
  **MUSS** er Requests mit dieser ID mit **HTTP 404** beantworten; der Client **MUSS** daraufhin eine **neue
  Session** starten (neuer `InitializeRequest` OHNE Session-ID). Robuste Server implementieren die saubere
  404-Antwort; robuste Anbindung plant Re-Init ein.
- **Mit haeufigem Re-Init rechnen:** ChatGPT/OpenAI-Connector startet teils **pro Tool-Call** eine frische
  Session; nach Server-Neustart sind stateful Session-IDs ungueltig (Session-404). → State dort halten, wo er
  ueberlebt, ODER stateless designen. Session-Lifetime ist laut **SEP-2567** offiziell **undefiniert**;
  Session-Resumption (`Last-Event-ID`) ist noch nicht spezifiziert.
- **Stateful vs. stateless:** Single-Server-HTTP → In-Memory-State ok. Verteilt/serverlos → State extern
  (Redis o.ae.) je `Mcp-Session-Id`, ODER `enableJsonResponse`/stateless-Pfad. Ab Spec **2026-07-28** wird
  stateless-remote offiziell (Round-Robin-LB statt Sticky-Sessions, `tools/list`-Caching mit `ttlMs`).

> **sb-mcp:** stateless im Effekt (jeder Tool-Call ruft synchron die brain-api; kein eigener Session-State im
> MCP-Server) → Session-404/Re-Init ist unkritisch. Vorbildlich fuer einen 1:1-Speicher.

## §3 — Authentifizierung: spec-konform UND pragmatisch (offiziell + extern)

**Spec 2025-06-18 (`offiziell`, modelcontextprotocol.io/specification/2025-06-18/basic/authorization):**
- **OAuth 2.1** ist der Standard (Auth-Server MUST OAuth 2.1; Client = OAuth-Client). **PKCE** Pflicht fuer
  Public Clients.
- **MCP-Server = reiner Resource-Server** (seit 2025-06-18 vom Auth-Server getrennt). Token-Validierung:
  "MCP servers MUST validate access tokens … ensuring the access token is issued specifically for the MCP server."
- **RFC 9728 Protected Resource Metadata (MUST):** Server publiziert `/.well-known/oauth-protected-resource`
  mit `authorization_servers`; bei `401` enthaelt der `WWW-Authenticate`-Header `resource_metadata=…`.
- **RFC 8707 Resource Indicators / Audience-Binding (MUST):** "MCP servers MUST only accept tokens
  specifically intended for themselves and MUST reject tokens that do not include them in the audience claim."
- **KEIN Token-Passthrough:** eingehende Client-Tokens NICHT ungeprueft an Downstream-APIs weiterreichen
  (eigene Security-Best-Practices-Seite seit 2025-06-18).
- **Transport-Weiche:** HTTP-Transport **SHOULD** die Auth-Spec nutzen; **stdio SHOULD NOT** (Credentials aus
  der Umgebung).
- **Spec-Drift:** 2025-06-18 = "SHOULD" fuer HTTP; die **2025-11-25**-Linie verschaerft Richtung "MUST"
  ("All remote MCP server connections must use OAuth 2.1", CSA `extern`).

**Pragmatik — Single-User-Server hinter VPN (`extern`/abgeleitet):** Keine Quelle bewertet das Szenario
explizit. Ableitung: Unter 2025-06-18 ist HTTP-Auth "SHOULD" → ein statischer Bearer (oder reine
Netz-Isolation via WireGuard) ist **kein harter MUST-Bruch**. Man verliert aber Audience-Binding,
**Tool-Level-Scopes** (ein Lese-Tool sollte keinen Loesch-Scope tragen) und PRM-Discovery — und ist auf die
naechste Spec ("MUST") nicht vorbereitet. **Entscheidungsbaum:**

| Szenario | Empfehlung |
|----------|-----------|
| stdio / lokal | KEIN OAuth (Env-Creds) |
| HTTP, oeffentlich / Multi-User / Drittclients | OAuth 2.1 + PKCE + PRM (RFC 9728) + Resource Indicators (RFC 8707), kein Token-Passthrough; DCR (RFC 7591) fuer offene Agent-Apps |
| HTTP, selbst gehostet, Single-User, hinter VPN/WireGuard | Pragmatisch: Netz-Isolation (VPN) ± statischer Bearer auf `/mcp`. Spec-treu waere OAuth-Resource-Server-Rolle (PRM + Audience-Check). |

> **sb-mcp:** Das `/mcp`-Endpoint selbst hat **keine** Auth — es verlaesst sich auf die **WireGuard-Isolation**
> (Bind an `10.8.0.1`, kein oeffentlicher Port). Nach downstream nutzt es einen Bearer gegen die brain-api
> (`SB_API_KEY`). Fuer Single-User-hinter-VPN ist das vertretbar (Auth = "SHOULD"). **Cheaper Haertungsschritt:**
> einen statischen Bearer-Check auf `/mcp` ergaenzen (zweite Schicht neben dem VPN), damit ein versehentlich
> exponierter Port nicht sofort offen ist.

## §4 — Netz-Sicherheit (offiziell + extern)

- **DNS-Rebinding-Schutz (Pflicht-Wissen):** Eine boesartige Webseite kann via DNS-Rebinding die Same-Origin-
  Policy umgehen und einen lokalen HTTP-MCP-Server ansprechen (**CVE-2025-66416**, Fix ab `mcp` **1.23.0**).
  Schutz explizit aktivieren — **Allowlist-basiert**:
  ```python
  from mcp.server.fastmcp import FastMCP
  from mcp.server.transport_security import TransportSecuritySettings
  mcp = FastMCP("server", transport_security=TransportSecuritySettings(
      enable_dns_rebinding_protection=True,
      allowed_hosts=["127.0.0.1:*", "10.8.0.1:*"],     # genau die erwarteten Hosts
      allowed_origins=["http://10.8.0.1:*"],
  ))
  ```
  Spec 2025-03-26 (`offiziell`): "Servers MUST validate the `Origin` header on all incoming connections to
  prevent DNS rebinding attacks." `enable_dns_rebinding_protection=False` NUR fuer lokale Entwicklung.
- **Bind eng (offiziell/`extern`):** "When running locally, the server SHOULD bind only to localhost
  (127.0.0.1) rather than all network interfaces (0.0.0.0)" — fuer VPN-only an die **WireGuard-IP** binden.
  Nie blanko `0.0.0.0` exponieren.
- **Kein Wildcard-CORS:** `Access-Control-Allow-Origin` auf den konkreten Origin setzen, nicht `*` (ein
  Wildcard untergraebt die Origin-Validierung des DNS-Rebinding-Schutzes; CISA/NSA-Guidance 2026-06-02 `extern`).
- **TLS via Reverse-Proxy:** Produktion → TLS/HTTPS; MCP-Server intern an `127.0.0.1`/VPN, ein Reverse-Proxy
  (Caddy/nginx/Traefik) terminiert TLS und ist der einzige oeffentliche Eingang. Stdio ist von der
  DNS-Rebinding-Klasse **nicht** betroffen — wo lokal moeglich, stdio bevorzugen.

> **sb-mcp:** setzt `TransportSecuritySettings(enable_dns_rebinding_protection=True, allowed_hosts=[10.8.0.1…])`
> mit defensivem Fallback (Security-Config crasht nie) und bindet an die WireGuard-IP — vorbildlich, genau das
> Allowlist-Muster. Kein Reverse-Proxy noetig (VPN-only).

## §5 — Tool-Schema-Design (offiziell + extern)

- **Beschreibung:** Docstring wird zu `description` (Pflichtfeld der Tool-Definition, Spec 2025-06-18/server/tools).
  Praezise, englisch, wie einem Kollegen erklaert; kein leerer/ generischer Docstring. Statisches
  Referenzmaterial in **Resources** auslagern, nicht in die Tool-Beschreibung.
- **Enge Typen:** `Annotated[str, Field(description=…, pattern=…, min_length=…, max_length=…, examples=[…])]`;
  enums fuer feste Kategorien; `limit: int = Field(default=20, ge=1, le=100)`. Aus Signatur+Annotation wird das
  `inputSchema` (JSON-Schema) generiert. `*args`/`**kwargs` werden nicht unterstuetzt.
- **Tool-Annotations (Spec 2025-03-26+):** `readOnlyHint` (Default false), `destructiveHint` (Default **true**),
  `idempotentHint` (Default false), `openWorldHint` (Default true). Clients **MUST** Annotations von
  untrusted Servern misstrauen; von **vertrauenswuerdigen** Servern treiben sie Confirmation-Prompts
  (`readOnlyHint:true` → ggf. Auto-Approve, `destructiveHint:true` → Bestaetigung). **Jedes Tool markieren** —
  eine Zeile, verhindert versehentlichen Datenverlust.
- **Structured Output:** `outputSchema` (seit Spec 2025-06-18) — im SDK aus dem Return-Type generiert. Fuer 1:1-
  Text-Speicher optional; bei strukturierten Treffern (Liste von Eintraegen) sinnvoll.
- **Granularitaet (`extern`):** Wenige klare Tools statt vieler roher CRUD-Endpunkte — "design tools around what
  the agent wants to achieve" (bigdataboutique `extern`). Lese-Lookups in ein workflow-orientiertes Such-Tool
  buendeln, atomare Schreib-/Loesch-Tools getrennt. (Die offizielle Spec macht hierzu KEINE harte Regel — als
  `extern`/abgeleitet behandeln.)
- **Namen:** `snake_case`, ggf. Service-Praefix gegen Kollisionen.

> **sb-mcp:** 8 Tools mit klaren Docstrings, snake_case, sinnvolle Granularitaet (Schreiben `remember`,
> Loeschen `forget`, Themensuche `recall`, atomare Lookups `get_by_title`/`get_by_category`/`get_by_date`,
> Uebersicht `list_memories`, `brain_health`). **Cheaper Haertungsschritt:** Tool-Annotations ergaenzen —
> `recall`/`get_by_*`/`list_memories`/`brain_health` → `readOnlyHint:true` (+ `idempotentHint:true`,
> `openWorldHint:false`); `forget` → `destructiveHint:true`; `remember` → `readOnlyHint:false`,
> `destructiveHint:false`. Optional: enge `Field`-Typen (`date` als `pattern=r"^\d{4}-\d{2}-\d{2}$"`).

## §6 — Fehler-Propagation + Streaming/Timeout hinter Reverse-Proxy

- **Zwei Fehlerkanaele sauber trennen** (`offiziell` JSON-RPC 2.0 + `extern`):
  - **Protokoll-/Transportfehler → JSON-RPC-Error** (`code`/`message`/optional `data`): Parse-Error -32700,
    Invalid Request -32600, Method not found -32601, Invalid params -32602, Internal error -32603. Oft in einer
    HTTP-200-Antwort, teils 4xx/5xx.
  - **Fachlicher Tool-Fehler → Tool-Result mit `isError`:** Tool korrekt aufgerufen, aber fachlich gescheitert
    ("nicht gefunden", "Validation failed"). So kann sich das LLM selbst korrigieren. (Die exakte `isError`-
    Mechanik ist in den Snippets nicht im Volltext zitiert — im SDK setzt eine im Tool **geworfene** Exception
    das `isError`-Flag; ein per `try/except` zurueckgegebener Fehler-**String** ist eine "weiche" Variante, die
    das LLM als normales Ergebnis sieht.)
- **Keine Internas an den Client:** keine Stacktraces/Tokens/Pfade in der Fehlermeldung; intern strukturiert
  loggen, nach aussen generisch.
- **Streaming hinter Reverse-Proxy:** SSE/Streaming braucht **deaktivierte Pufferung**:
  - **Caddy** (`offiziell`, reverse_proxy-Direktive): `flush_interval -1` (≠0 ist Pflicht fuer SSE — sonst
    puffert der Proxy den Stream; caddy#677 `extern`), dazu `stream_timeout`/`stream_close_delay`/`response_buffers`.
  - **nginx** (`extern`, etabliert): `proxy_buffering off;`, langes `proxy_read_timeout`, `proxy_http_version 1.1;`,
    ggf. `X-Accel-Buffering: no`.
- **Timeouts:** Client-Default 60 s (max 3600 s, Roo Code `extern`). Lang laufende Tools → Progress-Notifications
  + grosszuegige Proxy-Timeouts; 424/Timeout-Probleme sind ein bekannter Remote-MCP-Schmerzpunkt.

> **sb-mcp:** faengt pro Tool Exceptions ab und gibt einen Fehler-**String** zurueck (LLM-lesbar) + loggt
> strukturiert mit `exc_info` — robust, aber ohne `isError`-Flag (bewusst weiche Variante). httpx mit
> expliziten Timeouts (120 s POST, 30 s GET). Kein Reverse-Proxy (direkter VPN-Bind) → Caddy/nginx-Streaming-
> Tuning nur relevant, falls spaeter ein Proxy davor kommt.

## §7 — Versions- & Kompatibilitaets-Strategie (offiziell)

- **`MCP-Protocol-Version`-Header (Pflicht ab 2025-06-18):** "If using HTTP, the client MUST include the
  `MCP-Protocol-Version: <version>` HTTP header on all subsequent requests." Der Server **MUSS** bei
  ungueltiger/unsupported Version mit **`400 Bad Request`** antworten; fehlt der Header, SHOULD-Fallback auf
  **`2025-03-26`** (modelcontextprotocol.io/specification/2025-11-25/basic/transports, `offiziell`). Selbst
  offizielle SDKs setzen die Pruefung teils noch nicht durch (go-sdk#198 `extern`) — robuste Server validieren.
- **Capability-Negotiation** passiert im `initialize` (Detail-Tabelle in den Snippets nicht voll belegt → bei
  Bedarf `basic/lifecycle` der Spec lesen).
- **SDK-Pinning (offiziell, python-sdk README):** v1.x ist stable, **v2 ist Alpha** (API-Umbau, eigener
  Migration-Guide). Offizielle Empfehlung woertlich: "add a `<2` upper bound … for example `mcp>=1.27,<2`".
  Neuestes `mcp`=1.28.0 (2026-06-16). Ab der 2026-07-28-Spec bringt das SDK **version-gated wire validation**
  (drei Typ-Sets, `(method, version)`-Mapping).
- **Revisions-Horizont:** 2025-03-26 (Streamable HTTP) → 2025-06-18 (Auth, Header-Pflicht, structured output,
  RFC 8707, **JSON-RPC-Batching entfernt**) → 2025-11-25 (neueste stabile) → 2026-07-28 (RC, stateless-remote,
  breaking). Beim Bauen die anvisierte Revision bewusst waehlen.

> **sb-mcp:** pinnt `mcp>=1.27,<2` — **exakt die offizielle Empfehlung** (mit Begruendung im requirements.txt-
> Kommentar: v2 benennt FastMCP→MCPServer um + verschiebt host/port nach `run()`). Vorbildlich. Den
> `MCP-Protocol-Version`-Header validiert das SDK; eine eigene 400-Pruefung ist optional.

---

## So macht es `sb-mcp` richtig (Kurzbilanz) + cheap wins

**Vorbildlich:** Streamable HTTP (nie SSE) · DNS-Rebinding-Allowlist mit defensivem Fallback · Bind an
WireGuard-IP · `mcp>=1.27,<2` (offizielle Pinning-Empfehlung) · 8 klare snake_case-Tools mit Docstrings ·
strukturiertes JSON-Logging + Fehler-Faenger pro Tool + Logik-Sonde · effektiv stateless (Session-404 unkritisch) ·
Secrets aus Env, kein build-arg · kein `from __future__ import annotations` (PEP-563-Falle fuer FastMCP).

**Cheap Haertungsschritte (optional, alle additiv/funktionserhaltend):**
1. **Tool-Annotations** ergaenzen (`readOnlyHint`/`destructiveHint`) — eine Zeile je Tool, schuetzt vor
   versehentlichem `forget` und erlaubt Auto-Approve fuer Lese-Tools.
2. **Statischer Bearer auf `/mcp`** als zweite Schicht neben der VPN-Isolation (gegen versehentlich offenen Port).
3. Enge **`Field`-Typen** fuer `date`/`category`/`limit` (pattern/enum/ge-le).

---

## Pflicht-Checkliste vor "Remote-MCP-Server fertig/deployt"

- [ ] **Transport:** `streamable-http`, ein `/mcp`-Endpoint; kein neuer SSE-Server.
- [ ] **Session:** `Mcp-Session-Id` korrekt; 404 bei beendeter Session; Re-Init eingeplant; stateless wo moeglich.
- [ ] **Auth:** Szenario entschieden (oeffentlich → OAuth 2.1/PKCE/PRM/RFC 8707/kein Passthrough; VPN-Single-User → bewusst Bearer/Netz-Isolation).
- [ ] **Netz:** DNS-Rebinding-Schutz aktiv (Allowlist, `mcp>=1.23`); Bind an 127.0.0.1/VPN; kein Wildcard-CORS; TLS via Proxy wenn oeffentlich.
- [ ] **Tools:** klare Docstrings; enge `Field`-Typen; Annotations (`readOnlyHint`/`destructiveHint`); snake_case; wenige klare Tools.
- [ ] **Fehler:** Protokoll- vs. fachlicher Fehler getrennt; keine Internas nach aussen; strukturiert loggen.
- [ ] **Proxy:** bei Reverse-Proxy SSE-Pufferung aus (Caddy `flush_interval -1` / nginx `proxy_buffering off`) + lange Timeouts.
- [ ] **Version:** `MCP-Protocol-Version` validieren (400 bei unsupported); `mcp>=1.27,<2` pinnen.
- [ ] **Bug-Almanach:** bei Fehlern im Bereich den noch anzulegenden Almanach unter bugs/server/remote-mcp.md (Skill bug-almanach-recherche) lesen/ergaenzen.
