# MCP-Server-Bau (Model Context Protocol) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
