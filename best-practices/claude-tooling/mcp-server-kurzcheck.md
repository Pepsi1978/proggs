# MCP-Server-Bau Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
