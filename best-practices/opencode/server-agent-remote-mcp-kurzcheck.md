# Eigenen Remote-MCP-Server + Server-Operator-Agent aus den CLIs anbinden Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Regel | Detail |
|---|-------|--------|
| 1 | **Tunnel zuerst** | WireGuard aktiv pruefen, BEVOR die CLI startet (`ping 10.8.0.1`, `curl http://10.8.0.1:8001/mcp`). API nur ueber VPN → Tunnel = Voraussetzung. |
| 2 | **URL exakt** | `http://10.8.0.1:8001/mcp` — Host/Port wie in `allowed_hosts`, Pfad `/mcp`, kein DNS-Name. |
| 3 | **OpenCode-Eintrag** | Allgemein `type:"remote"`; unser OpenCode 1.17.18 nutzt wegen Remote-/SSE-HTTP-400 ausnahmsweise `type:"local"` über gepinntes `mcp-remote@0.1.38 --transport http-only --allow-http --silent`, `timeout:30000`. |
| 4 | **Claude-Eintrag** | `claude mcp add --transport http second-brain http://10.8.0.1:8001/mcp --scope user`; NIE `mcp-remote`-stdio. `streamable-http`=Alias fuer `http`. |
| 5 | **Aufgabenteilung** | Trenne **Leser** (nur `second-brain_recall/get_*/list_memories/brain_health`) von **Schreiber** (zusaetzlich `remember/forget`). Scope „enforced by architecture, not instruction". |
| 6 | **Server-Operator-Modus** | Eigener Markdown-Agent `~/.config/opencode/agents/<name>.md` (Dateiname=Name), Frontmatter `description`(Pflicht)/`mode`/`model`/`permission`. `permission` statt `tools` (deprecated). |
| 7 | **Reconnect-Disziplin** | Nach jedem sb-mcp-/Container-Neustart UND nach Tunnel-Abbruch manuell reconnecten (weder OpenCode noch WireGuard reconnecten von selbst). |
| 8 | **Token-Disziplin** | Nur gebrauchte MCP pro Agent (Per-Agent-Scope); Claude Tool Search aktiv; mit `/context`/`/doctor` pruefen; OpenCode `compaction.prune:true`. |
| 9 | **Kategorie sequenziell** | `get_category_item`: Index 1 lesen, `total` übernehmen, dann 2 bis `total` strikt einzeln. Unabhängige Volltexte sind seit sb-mcp 1.3.3 separat parallel abgesichert. |
