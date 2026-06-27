# Remote-MCP-Server ueber HTTP (Streamable HTTP) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
