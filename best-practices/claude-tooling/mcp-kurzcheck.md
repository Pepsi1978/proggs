# MCP-Server Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | `.mcp.json`-Pfade | absolute Pfade/Interpreter; plattformspezifisch (NIE automatisch vereinheitlichen) | .mcp.json |
| 2 | Transport | HTTP ist Standard, SSE veraltet | Transport-Typen |
| 3 | viele Server | Tool Search (deferred Schemas) gegen Kontext-Ueberlauf | Tool Search |
| 4 | Remote-Server | OAuth 2.0; in Cowork Anthropic-IPs allowlisten | OAuth 2.0 |
| 5 | Subagent-MCP | Frontmatter-MCP ist policy-konform (2.1.153) | Subagent-Frontmatter-MCP |
| 6 | Scopes | local / project / user (Terminologie geaendert); Dedup ueber Scopes | Scopes |
| 7 | MCP-Hook fuer Policy | umgehbar bei Disconnect → harte Policy als `command`-Hook | Managed MCP |
