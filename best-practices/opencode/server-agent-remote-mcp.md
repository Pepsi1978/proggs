# Eigenen Remote-MCP-Server + Server-Operator-Agent aus den CLIs anbinden — Best Practices (Stand 2026-06-24)

> **Zweite Seite der Medaille zu `bugs/opencode/server-agent-remote-mcp.md`.** Der Almanach sagt
> *was beim Anbinden/Operieren schiefgeht*; hier steht, *wie man es von vornherein richtig macht.*
> Beide Kurzchecks vor der Arbeit lesen. Funktionserhaltend (Direktive #3). CLIENT-/Operator-Seite
> (OpenCode + Claude Code) des selbst gehosteten „zweiten Gehirns" (sb-mcp, `http://10.8.0.1:8001/mcp`).
>
> **Anker:** opencode=1.17.9 · claude-code=2.1.187 · transport=streamable-http.
> Quellen: opencode.ai/docs/{mcp-servers,agents,permissions,config} · code.claude.com/docs/mcp ·
> Firecrawl+MiniMax + OpenRouter-`:online`-Recherche 2026-06-24 (Funde mit `offiziell`/`extern` gelabelt).
>
> **Abgrenzung / Nachbarn (NICHT duplizieren — dort steht das Generische):**
> - Generisches Agent/Mode/Permission-Wissen → `best-practices/opencode/agents-modes.md` (§6 Pro-Agent-Scope).
> - Generische MCP-Server-Config + Token-Caveat → `best-practices/opencode/plugins-mcp-skills.md` §1/§8.
> - „Welcher Memory-Server als Gehirn?" (supermemory/mem0-Entscheidung) → `best-practices/opencode/self-hosted-memory-server.md`.
> - WireGuard-Tunnel selbst (AllowedIPs/Split-Tunnel) → `best-practices/server/wireguard.md`.
> - Server-Bau-Seite des MCP (FastMCP/Transport-Security) → `best-practices/claude-tooling/mcp.md` + `bugs/claude-tooling/mcp-server.md`.
> DIESE Datei = der konkrete Operator-Leitfaden fuer GENAU diesen Stack (eigenes Gehirn ueber VPN, beide CLIs, Server-Operator-Modus).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Regel | Detail |
|---|-------|--------|
| 1 | **Tunnel zuerst** | WireGuard aktiv pruefen, BEVOR die CLI startet (`ping 10.8.0.1`, `curl http://10.8.0.1:8001/mcp`). API nur ueber VPN → Tunnel = Voraussetzung. |
| 2 | **URL exakt** | `http://10.8.0.1:8001/mcp` — Host/Port wie in `allowed_hosts`, Pfad `/mcp`, kein DNS-Name. |
| 3 | **OpenCode-Eintrag** | `type:"remote"`, `enabled:true`; kein Header noetig (Tunnel ist der Kanal). Env-Header (falls je) `{env:VAR}` / `{file:~/SK/...}` (NIE `${...}`); bei API-Key `oauth:false`. |
| 4 | **Claude-Eintrag** | `claude mcp add --transport http second-brain http://10.8.0.1:8001/mcp --scope user`; NIE `mcp-remote`-stdio. `streamable-http`=Alias fuer `http`. |
| 5 | **Aufgabenteilung** | Trenne **Leser** (nur `second-brain_recall/get_*/list_memories/brain_health`) von **Schreiber** (zusaetzlich `remember/forget`). Scope „enforced by architecture, not instruction". |
| 6 | **Server-Operator-Modus** | Eigener Markdown-Agent `~/.config/opencode/agents/<name>.md` (Dateiname=Name), Frontmatter `description`(Pflicht)/`mode`/`model`/`permission`. `permission` statt `tools` (deprecated). |
| 7 | **Reconnect-Disziplin** | Nach jedem sb-mcp-/Container-Neustart UND nach Tunnel-Abbruch manuell reconnecten (weder OpenCode noch WireGuard reconnecten von selbst). |
| 8 | **Token-Disziplin** | Nur gebrauchte MCP pro Agent (Per-Agent-Scope); Claude Tool Search aktiv; mit `/context`/`/doctor` pruefen; OpenCode `compaction.prune:true`. |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug
| Best-Practice (hier) | Bug-Abschnitt (`bugs/opencode/server-agent-remote-mcp.md`) |
|----------------------|-----------------------------------------------------------|
| §1 OpenCode-Eintrag | §1 OpenCode-Remote-Anbindung |
| §2 Claude-Eintrag | §2 Claude-Code-Remote-Anbindung |
| §3 Sichtbarkeit | §3 Tools erscheinen nicht |
| §4 Tool-Scoping + Aufgabenteilung (Leser/Schreiber) | §4 Server-only-Agent/Modus |
| §5 Server-Operator-Modus definieren | §4 Server-only-Agent/Modus |
| §6 Erreichbarkeit (WireGuard/Host-Header) | §5 WireGuard/Host-Header |
| §7 Reconnect/Verfuegbarkeit | §6 Session/Reconnect |
| §8 Token/Kosten | §7 Token/Kosten |

---

## §1 — OpenCode-Eintrag sauber konfigurieren `offiziell`
**Empfohlene Form fuers eigene Gehirn ueber WireGuard** (so steht es in `~/.config/opencode/opencode.jsonc`):
```jsonc
"mcp": {
  "second-brain": {
    "type": "remote",
    "url": "http://10.8.0.1:8001/mcp",
    "enabled": true
  }
}
```
- **Streamable HTTP, nicht SSE** (SSE hat bekannte OpenCode-Issues). Remote-Server nutzen `url` (String);
  local-Server `command` (Array) — **nie mischen** (opencode.school).
- **Kein Header noetig**, weil der WireGuard-Tunnel der private Kanal ist. Falls je ein Token gebraucht wird:
  `headers:{"Authorization":"Bearer {env:SB_TOKEN}"}` — OpenCode-Syntax **`{env:VAR}`** / **`{file:~/SK/...}`**,
  NIE `${env:VAR}` (Dollar bricht es). Bei reinem API-Key `"oauth": false` (sonst startet OpenCode bei 401
  automatisch Dynamic Client Registration RFC 7591). OAuth-Server: `"oauth": {}`.
- **Org-Default (optional):** Mehrere Maschinen koennen eine Default-Config ueber `.well-known/opencode`
  beziehen (Lade-Reihenfolge: Remote zuerst, lokal/Projekt ueberschreibt). Muster: Server org-weit
  `enabled:false` ausliefern, pro Entwickler lokal aktivieren.
- **Diagnose:** `opencode mcp debug` ist nur ein Indiz (zeigt teils faelschlich „failed") — Wahrheit ist
  `/status` in der TUI + ein echter Tool-Aufruf.

## §2 — Claude-Code-Eintrag sauber konfigurieren `offiziell`
```bash
claude mcp add --transport http second-brain http://10.8.0.1:8001/mcp --scope user
```
bzw. in `.mcp.json`/`~/.claude.json`:
```json
{ "mcpServers": { "second-brain": { "type": "http", "url": "http://10.8.0.1:8001/mcp" } } }
```
- **HTTP ist der empfohlene Remote-Transport** (SSE deprecated). In JSON ist `streamable-http` ein **Alias**
  fuer `http` — aus Server-Doku kopierte Configs laufen unveraendert.
- **Scope bewusst (Praezedenz local > project > user):**
  | Scope | Ort | Sichtbarkeit | fuers Gehirn? |
  |-------|-----|--------------|---------------|
  | `user` | `~/.claude.json` global | du, alle Projekte | **empfohlen** (privates Utility) |
  | `local` | projektgebunden | nur du, dieses Projekt | ok zum Testen |
  | `project` | `.mcp.json` im Repo (Git) | Team | **NICHT** — WG-IP gehoert nicht ins geteilte Repo, jede Session bekaeme die Freigabe-Frage |
- **Team-Muster** (falls je geteilt): `.mcp.json` mit Server committen, Auth pro Entwickler ueber
  local-scoped `~/.claude.json` (lokal ueberschreibt → Servername bleibt konsistent, Credentials privat),
  Secrets via `${VAR}`. **Nie** `mcp-remote`-stdio-Wrapper (Windows-Falle, Almanach §2). Flags VOR dem Namen.

## §3 — Sichtbarkeit pruefen `offiziell`
- Status: `/status` (OpenCode) bzw. `/mcp` (Claude) — beide erlauben Reconnect/Auth.
- Tool-Namen: Claude bildet `mcp__<server>__<tool>` (max **64 Zeichen**) — Server-Name kurz halten;
  `second-brain` + `remember`/`recall`/… bleibt weit drunter. OpenCode: `second-brain_<tool>`.
- Erscheinen keine Tools: pruefen ob `enabled:true`, ob NICHT global (`tools:{"second-brain*":false}`)
  oder per Agent ausgeblendet, und ob der Tunnel/Host-Header stimmt (§6).
- Token-Sicht: Claude `/context` (zeigt MCP-Token-Anteil) + `/doctor` (warnt ab ~25k MCP-Token).

## §4 — Tool-Scoping + Aufgabenteilung Leser/Schreiber `offiziell` `extern`
**Prinzip (extern, foojay/Best-Practice-Artikel):** „Scope is enforced by architecture, not just by
instruction" — ein Agent, der `remember`/`forget` gar nicht hat, KANN nicht schreiben, egal was das
Modell denkt. Genau das ist die sichere Aufgabenteilung.

**Mechanik (offiziell, opencode.ai/docs/permissions + /agents):**
- `permission` ist der empfohlene Weg; `tools` ist **deprecated** (Mapping: `true`≈`{"*":"allow"}`,
  `false`≈`{"*":"deny"}`). Agent-Config ueberschreibt global. Wildcards bündeln einen ganzen Server
  (`second-brain*`).
- **Per-Agent-Muster** (global aus, gezielt an) — die offizielle Art, EINEN MCP nur einem Agenten zu geben:
  ```jsonc
  "tools": { "second-brain*": false },              // global aus (alle Agenten)
  "agent": {
    "gehirn-leser":    { "tools": { "second-brain_recall": true, "second-brain_get_by_title": true,
                                    "second-brain_get_by_category": true, "second-brain_get_by_date": true,
                                    "second-brain_list_memories": true, "second-brain_brain_health": true },
                         "permission": { "edit": "deny", "bash": "deny" } },
    "gehirn-schreiber":{ "tools": { "second-brain*": true },     // zusaetzlich remember/forget
                         "permission": { "second-brain_forget": "ask" } }   // Loeschen nur nach Rueckfrage
  }
  ```
- **Empfohlene Aufgabenteilung fuer das 1:1-Gehirn:**
  - **Leser-Agent** (Default/Recherche): nur `recall`/`get_by_*`/`list_memories`/`brain_health` — kann
    abrufen, aber NICHT veraendern. Ideal als ständiger Begleiter (kein Risiko, das Gehirn zu verschmutzen).
  - **Schreiber-Agent** (bewusst gewaehlt): zusaetzlich `remember` (+ `forget` auf `ask`, nie blind). Nur
    starten, wenn wirklich etwas abgelegt/aktualisiert werden soll.
  - Lowercase-Keys (PascalCase wird still ignoriert); Subagent erbt Primary-Modell → `model:` explizit.
- Eingebaute Read-only-Vorlagen: `explore`/`scout` (Generisches in `agents-modes.md` §6).
- Claude Code: MCP-Tools nur fuer einen Subagenten sind seit **#6915 (completed)** moeglich.

## §5 — „Server-Operator"-Modus in OpenCode definieren `offiziell`
Ein eigener Agent, der gezielt Server-/Gehirn-Aufgaben macht. **AGENTS.md ist NICHT die Agent-Definition**
(das ist die projektweite Instruktionsdatei) — ein Agent wird als Markdown-Datei definiert:
`~/.config/opencode/agents/<name>.md` (global) bzw. `.opencode/agents/<name>.md` (Projekt);
**Dateiname = Agent-Name**. Beispiel `server-operator.md`:
```markdown
---
description: Operiert das zweite Gehirn + serverbezogene Aufgaben (recherchiert/erinnert, keine lokalen Edits)
mode: primary
model: anthropic/claude-sonnet-4-5
temperature: 0.1
permission:
  edit: deny
  bash: ask
tools:
  second-brain*: true
  write: false
  edit: false
---
Du bist der Server-Operator fuer Franks zweites Gehirn (sb-mcp ueber WireGuard).
Nutze recall/get_by_title/get_by_category/list_memories zum Abrufen; remember nur auf klare Ansage,
forget nur nach Rueckfrage. Arbeite NICHT an lokalem Projektcode.
```
- `description` ist **Pflicht** (sagt, wann der Agent zu nutzen ist). `mode:primary` → per Tab waehlbar;
  `subagent` → per `@server-operator` aufrufbar. `temperature` niedrig (0.0–0.2) fuer deterministische Arbeit.
- „**One job per agent**" (extern): ein fokussierter Operator statt Generalist → konsistenter, sicherer.
- Per-Agent-System-Prompt via Frontmatter-Body oder `prompt:"{file:./prompts/operator.txt}"`.
- Team-weit teilbar: Agent-Datei im `.opencode/`-Ordner mit dem Repo ausliefern (generisch: `agents-modes.md`).

## §6 — Erreichbarkeit absichern (WireGuard/Host-Header) `extern`
- **Tunnel-first:** Der Endpunkt `10.8.0.1:8001` existiert NUR im WireGuard-Tunnel — ohne aktives
  Interface gibt es keine Route (`connection refused`/Timeout), das ist KEIN MCP-Bug. Vor jedem CLI-Start
  pruefen: `wg show` / `ping 10.8.0.1` / `curl -i http://10.8.0.1:8001/mcp`.
- **Split-Tunnel sauber:** in `AllowedIPs` nur die noetigen Subnetze (das Server-/VPN-Subnetz), nicht
  `0.0.0.0/0`, wenn der restliche Verkehr lokal bleiben soll. **DNS** im Client ggf. aufs Tunnel-Gateway
  setzen, sonst werden Tunnel-interne Hosts nicht aufgeloest (Quelle: netgate-Forum). Details: `server/wireguard.md`.
- **`PersistentKeepalive`** (z.B. 25 s) auf der Client-Peer-Seite hilft, die Verbindung hinter NAT/Firewall
  offen zu halten (reduziert Idle-Drops — loest aber NICHT das fehlende Auto-Reconnect, §7).
- **Host-Header:** Die Client-URL muss einen Host/Port nutzen, der server-seitig in `allowed_hosts` steht
  (`10.8.0.1:8001`) — sonst „invalid host header". Neuer Zugriffs-Host → server-seitig ergaenzen
  (`bugs/claude-tooling/mcp-server.md` §3.13).

## §7 — Reconnect-/Verfuegbarkeits-Strategie `extern`
- **Beide Schichten reconnecten NICHT von selbst:** OpenCode hat kein Auto-Reconnect/Keepalive/Pre-Use-
  Health-Check fuer Remote-MCP (Issue #15209, *not planned*); der WireGuard-Client reconnectet nach einem
  Drop ebenfalls nicht automatisch (bekannte Schwaeche). Folge: Tunnel-Drop ODER sb-mcp-Neustart →
  Tools „failed", bis **manuell** neu verbunden wird.
- **Empfohlene Disziplin:**
  - Nach sb-mcp-/Container-Neustart UND nach Tunnel-Abbruch manuell reconnecten: OpenCode Server kurz
    `enabled:false`→`true` bzw. `opencode mcp`-Reconnect; Claude `/mcp` → reconnect.
  - **Pre-Use-Health-Check** als Gewohnheit: vor einer Gehirn-Session `curl http://10.8.0.1:8001/mcp`
    (bzw. das `brain_health`-Tool) aufrufen — erst wenn gruen, arbeiten.
  - Optional ein **Proxy/LB, der die Verbindung warmhaelt** (Issue-Empfehlung), oder ein kleiner
    Watchdog, der Tunnel + Endpunkt pingt und bei Bedarf neu verbindet.
  - sb-mcp stabil halten (Container nicht unnoetig neustarten); geplante Neustarts → CLI-Session danach
    bewusst neu verbinden.

## §8 — Token-/Kosten-Disziplin `offiziell` `extern`
- **Jedes MCP-Tool-Schema laedt in JEDEN Turn** (nicht einmal pro Session) — bei mehreren Servern schnell
  10–20k+ Token/Turn. Fuer 2–3 schlanke Server (unser sb-mcp: 8 Tools) reichen die eingebauten Optimierungen.
- **Claude Tool Search** (ab 2.1.7, ihr: 2.1.187) laedt Schemata erst bei Bedarf (spart 13k+); aktiv lassen
  (`ENABLE_TOOL_SEARCH=auto`). Mit `/context` den MCP-Anteil sehen, `/doctor` warnt ab ~25k.
- **Per-Agent-Scope** (§4): beim Server-Operator andere MCP (z.B. firecrawl) deaktivieren, damit nur die
  Gehirn-Tools im Kontext sind. OpenCode: `compaction.prune:true` + nur gebrauchte Server `enabled`.
- **Server-seitig** (falls du sb-mcp erweiterst): Tools konsolidieren, Beschreibungen knapp, Standard-
  Parameternamen — weniger/kompaktere Schemata = weniger Token (gilt fuers Tool-Design, `mcp-server.md`).
