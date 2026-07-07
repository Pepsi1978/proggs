# Eigenen Remote-MCP-Server + Server-Agent aus den CLIs anbinden (OpenCode & Claude Code) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ CLI „sieht" das Gehirn nicht / connection refused / timeout | **WireGuard-Tunnel aktiv?** Der Endpunkt `10.8.0.1:8001` existiert NUR im Tunnel. Ohne aktives `wg`-Interface → kein TCP → „refused"/Timeout. Tunnel pruefen: `ping 10.8.0.1`, `curl http://10.8.0.1:8001/mcp`. | §5 |
| 2 | ⭐ Verbindung steht, aber „invalid host header" / „Request validation failed" | DNS-Rebinding-Schutz des MCP-SDK prueft den `Host`-Header gegen `allowed_hosts`. Client-URL-Host/Port MUSS exakt drinstehen (`10.8.0.1:8001`). Anderer Hostname/Port → abgewiesen. | §5 |
| 3 | ⭐ Nach sb-mcp-/Container-Neustart: „No valid session ID" / Tools tot | Streamable-HTTP-Session ist ungueltig → Client MUSS neu initialisieren. Viele Clients haengen → **manuell reconnecten** (OpenCode: Server kurz `enabled:false`→`true` / `opencode mcp` neu; Claude: `/mcp` reconnect). | §6 |
| 4 | OpenCode `type:"remote"` verbindet nicht (Auth) | Header-Syntax: **`{env:VAR}` — NIEMALS `${env:VAR}`** (Dollar killt es). Bei API-Key-Servern `"oauth": false` + `headers`. Unser sb-mcp braucht GAR keinen Header (WireGuard). | §1 |
| 5 | `opencode mcp debug <name>` zeigt „failed", TUI nutzt das Tool aber | Bekannt irrefuehrend — Debug-Status ≠ TUI-Realitaet. Im Zweifel `/status` in der TUI + echten Tool-Aufruf testen. | §1 |
| 6 | Claude Code: Remote-MCP startet nicht (Windows) | NICHT `mcp-remote` als stdio-Wrapper nehmen (#3423: bricht auf Windows sofort ab). **Direkt** `type:"http"` + `url` in `.mcp.json` nutzen (kein npx-Child-Prozess). | §2 |
| 7 | ⭐ Tools des Gehirns tauchen gar nicht auf | Reihenfolge pruefen: `enabled:true`? global `tools:{...false}` aktiv? Tunnel/Host-Header ok? Bei Claude: 64-Zeichen-Limit `mcp__<server>__<tool>` (unser `second-brain` ist kurz genug). `/status`(OpenCode) bzw. `/mcp`(Claude) zeigt den Verbindungsstatus. | §3 |
| 8 | ⭐ Server-only-Agent soll NUR das Gehirn nutzen | OpenCode: global `tools:{"*":false}`-Aequivalent vermeiden — gezielt `"second-brain*": true` im Agent + andere Tools `deny`. Claude Code: MCP-Tools-nur-fuer-Subagent ist seit #6915 moeglich. | §4 |
| 9 | Unerwartet hohe Token / teuer beim Server-Agenten | Jedes MCP-Tool-Schema laedt in JEDEN Prompt (~10-20k+ bei vielen Servern). Claude Tool Search ab 2.1.7 mildert automatisch (ihr: 2.1.187). Nicht gebrauchte MCP pro Agent deaktivieren. | §7 |
