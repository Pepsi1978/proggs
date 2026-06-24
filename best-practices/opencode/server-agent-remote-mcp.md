# Eigenen Remote-MCP-Server + Server-Agent aus den CLIs anbinden — Best Practices (Stand 2026-06-24)

> **Zweite Seite der Medaille zu `bugs/opencode/server-agent-remote-mcp.md`.** Der Almanach sagt
> *was beim Anbinden/Operieren schiefgeht*; hier steht, *wie man es von vornherein richtig macht.*
> Beide Kurzchecks vor der Arbeit lesen. Funktionserhaltend (Direktive #3). CLIENT-/Operator-Seite
> (OpenCode + Claude Code) des selbst gehosteten „zweiten Gehirns" (sb-mcp, `http://10.8.0.1:8001/mcp`).
>
> **Anker:** opencode=1.17.9 · claude-code=2.1.187 · transport=streamable-http.
> Quellen: opencode.ai/docs · code.claude.com/docs · MCP-Spec · GitHub-Issues (gh-verifiziert),
> Firecrawl+MiniMax-Recherche 2026-06-24. Generische OpenCode-MCP-Best-Practices: `best-practices/opencode/`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Regel | Detail |
|---|-------|--------|
| 1 | **Tunnel zuerst** | WireGuard aktiv pruefen, BEVOR die CLI startet (`ping 10.8.0.1`, `curl http://10.8.0.1:8001/mcp`). |
| 2 | **URL exakt** | `http://10.8.0.1:8001/mcp` — Host/Port wie in `allowed_hosts`, Pfad `/mcp`, kein DNS-Name. |
| 3 | **OpenCode** | `type:"remote"`, `enabled:true`, kein Header noetig; Env-Header (falls je) `{env:VAR}` (kein `$`); `oauth:false` bei API-Key. |
| 4 | **Claude Code** | `type:"http"` direkt (NIE `mcp-remote`-stdio); Scope `user`/`local`, NICHT `project` ins geteilte Repo (WG-IP + Freigabe-Frage). |
| 5 | **Server-only-Agent** | Tools eng scopen: `second-brain*` an, Rest `deny`; lowercase Keys; Subagent-`model` explizit setzen. |
| 6 | **Reconnect-Disziplin** | Nach jedem Server-/Container-Neustart MCP manuell reconnecten (kein Auto-Reconnect). |
| 7 | **Token sparen** | Nur gebrauchte MCP pro Agent; Claude Tool Search aktiv lassen; OpenCode `compaction.prune:true`. |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug
| Best-Practice (hier) | Bug-Abschnitt (`bugs/opencode/server-agent-remote-mcp.md`) |
|----------------------|-----------------------------------------------------------|
| §1 OpenCode konfigurieren | §1 OpenCode-Remote-Anbindung |
| §2 Claude `.mcp.json` | §2 Claude-Code-Remote-Anbindung |
| §3 Sichtbarkeit | §3 Tools erscheinen nicht |
| §4 Agent eng scopen | §4 Server-only-Agent/Modus |
| §5 Erreichbarkeit | §5 WireGuard/Host-Header |
| §6 Reconnect | §6 Session/Reconnect |
| §7 Token | §7 Token/Kosten |

---

## §1 — OpenCode `mcp` sauber konfigurieren
- `type:"remote"`, `url:"http://10.8.0.1:8001/mcp"`, `enabled:true`. Unser sb-mcp braucht **keinen** Header
  (WireGuard ist der private Kanal). So steht es in `~/.config/opencode/opencode.jsonc`.
- Falls je ein Token noetig: `headers:{"Authorization":"Bearer {env:VAR}"}` — **`{env:}`**, nie `${env:}`;
  Dateien via `{file:~/SK/...}`. Bei API-Key-Servern `"oauth": false` (sonst stoert RFC-7591-Autoversuch).
- `opencode mcp debug` ist nur ein Indiz (zeigt teils faelschlich „failed") — Wahrheit ist `/status` + echter Tool-Aufruf.

## §2 — Claude `.mcp.json` / `claude mcp add`
- **Direkter HTTP-Transport:** `claude mcp add --transport http second-brain http://10.8.0.1:8001/mcp`
  bzw. `{"mcpServers":{"second-brain":{"type":"http","url":"http://10.8.0.1:8001/mcp"}}}`.
- **Nie** den `mcp-remote`-stdio-Wrapper (npx) — bricht auf Windows ab. SSE ist deprecated → HTTP.
- Scope `user` (projektuebergreifend, privat) oder `local`; **nicht** `project` (WG-IP gehoert nicht ins
  geteilte Repo, und jede Session bekaeme die Freigabe-Frage). Flags VOR dem Servernamen.

## §3 — Sichtbarkeit pruefen
- `/status` (OpenCode) bzw. `/mcp` (Claude) zeigt den Verbindungsstatus + erlaubt Reconnect.
- Server-Namen **kurz** halten (Claude 64-Zeichen-Limit `mcp__<server>__<tool>`); `second-brain` ist ok.
- Prüfen, dass die Tools nicht global (`tools:{"second-brain*":false}`) oder per Agent ausgeblendet sind.

## §4 — Agent eng scopen (server-only)
- OpenCode Per-Agent-Muster: global `tools:{"second-brain*":false}` + im Ziel-Agent `tools:{"second-brain*":true}`;
  oder direkt im Agent nur die Gehirn-Tools an + `permission:{edit:"deny",bash:"deny"}`.
- Lowercase-Keys (PascalCase wird still ignoriert); Subagent erbt Primary-Modell → `model:` explizit.
- Read-only-Vorlagen: eingebaute `explore`/`scout`. Claude Code: MCP-Tools-nur-fuer-Subagent seit #6915 moeglich.

## §5 — Erreichbarkeit absichern
- WireGuard-Tunnel ist Voraussetzung: ohne ihn `connection refused`/Timeout (kein MCP-Bug). Vor CLI-Start pruefen.
- Client-URL-Host/Port MUSS in den server-seitigen `allowed_hosts` stehen (`10.8.0.1:8001`), sonst
  „invalid host header". Neuer Zugriffs-Host → server-seitig in `allowed_hosts` aufnehmen (`mcp-server.md` §3.13).

## §6 — Reconnect-Disziplin
- Streamable-HTTP-Session stirbt bei Server-/Container-Neustart → Client muss neu initialisieren; OpenCode
  hat keinen Auto-Reconnect. Nach jedem Neustart manuell reconnecten (OpenCode `enabled` togglen / Claude `/mcp`).
- Server stabil halten; geplante Neustarts → CLse-Session danach neu verbinden.

## §7 — Token sparen
- Jedes MCP-Tool-Schema laedt in jeden Prompt. Claude Tool Search (ab 2.1.7) laedt Schemata erst bei Bedarf —
  aktiv lassen (`ENABLE_TOOL_SEARCH=auto`). Nur gebrauchte MCP pro Agent; OpenCode `compaction.prune:true`.
- sb-mcp hat 8 schlanke Tools → moderat; beim Server-Agenten andere MCP (firecrawl) per Scope deaktivieren.
