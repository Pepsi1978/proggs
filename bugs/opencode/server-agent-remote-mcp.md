# Bekannte Bugs & Fallen: Eigenen Remote-MCP-Server + Server-Agent aus den CLIs anbinden (OpenCode & Claude Code)

> **PFLICHT-LESEN vor Arbeit an der CLIENT-/OPERATOR-Seite**: das selbst gehostete „zweite Gehirn"
> (sb-mcp) als Remote-MCP in **OpenCode** und **Claude Code** einbinden UND einen „server-only"-Agenten/
> Modus bauen, der nur mit/auf dem Server arbeitet. Loesungen funktionserhaltend (Direktive #3).
> **Zweite Seite:** `best-practices/opencode/server-agent-remote-mcp.md`.
>
> **Stand:** recherchiert am **2026-06-24** (Firecrawl + MiniMax M3, quellentreu; OpenCode-Docs +
> GitHub-Issues anthropics/claude-code, danny-avila/LibreChat, MCP-Spec — gh-OPEN/CLOSED-verifiziert).
> **Anker:** opencode=1.17.9 · claude-code=2.1.187 · mcp-sdk(server)>=1.27 · transport=streamable-http
> <!-- maschinenlesbar fuer check-version-anchor.py -->
> **Bezug:** sb-mcp unter `~/proggs/second-brain-server/mcp-server` (FastMCP Streamable-HTTP,
> `http://10.8.0.1:8001/mcp`, gebunden an die WireGuard-IP, KEINE Client-Auth — der Tunnel IST der Kanal).
>
> **Abgrenzung (wichtig):**
> - **SERVER-Bau-Seite** (FastMCP/Tools/Transport-Security im sb-mcp selbst) → `claude-tooling/mcp-server.md`.
> - **Generische OpenCode-MCP-Fallen** (connected-aber-keine-Tools, Docker-MCP-Regression, kein
>   Auto-Reconnect, Projekt-Config-MCP, OAuth-vs-API-Key, Desktop-App) → `opencode/opencode-cli.md`
>   **§59-§64a** + **§6** (Agents/Modes). DIESE Datei dupliziert sie NICHT, sondern verweist darauf und
>   ergaenzt die **eigenes-Gehirn-ueber-WireGuard + Claude-Code-Client + Server-only-Agent**-Spezifika.
> - **WireGuard selbst** (Tunnel, AllowedIPs) → `server/wireguard.md`. **Web-Schicht des Servers** → `server/fastapi.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ CLI „sieht" das Gehirn nicht / connection refused / timeout | **WireGuard-Tunnel aktiv?** Der Endpunkt `10.8.0.1:8001` existiert NUR im Tunnel. Ohne aktives `wg`-Interface → kein TCP → „refused"/Timeout. Tunnel pruefen: `ping 10.8.0.1`, `curl http://10.8.0.1:8001/mcp`. | §5 |
| 2 | ⭐ Verbindung steht, aber „invalid host header" / „Request validation failed" | DNS-Rebinding-Schutz des MCP-SDK prueft den `Host`-Header gegen `allowed_hosts`. Client-URL-Host/Port MUSS exakt drinstehen (`10.8.0.1:8001`). Anderer Hostname/Port → abgewiesen. | §5 |
| 3 | ⭐ Nach sb-mcp-/Container-Neustart: „No valid session ID" / Tools tot | Streamable-HTTP-Session ist ungueltig → Client MUSS neu initialisieren. Viele Clients haengen → **manuell reconnecten** (OpenCode: Server kurz `enabled:false`→`true` / `opencode mcp` neu; Claude: `/mcp` reconnect). | §6 |
| 4 | OpenCode `type:"remote"` verbindet nicht (Auth) | Header-Syntax: **`{env:VAR}` — NIEMALS `${env:VAR}`** (Dollar killt es). Bei API-Key-Servern `"oauth": false` + `headers`. Unser sb-mcp braucht GAR keinen Header (WireGuard). | §1 |
| 5 | `opencode mcp debug <name>` zeigt „failed", TUI nutzt das Tool aber | Bekannt irrefuehrend — Debug-Status ≠ TUI-Realitaet. Im Zweifel `/status` in der TUI + echten Tool-Aufruf testen. | §1 |
| 6 | Claude Code: Remote-MCP startet nicht (Windows) | NICHT `mcp-remote` als stdio-Wrapper nehmen (#3423: bricht auf Windows sofort ab). **Direkt** `type:"http"` + `url` in `.mcp.json` nutzen (kein npx-Child-Prozess). | §2 |
| 6a | Codex-App an das Gehirn anbinden / haengt beim ersten Verbinden | Einstellungen → MCP Server → **Streamable HTTP**, Name `second-brain`, URL `http://10.8.0.1:8001/mcp` (HTTP! Port 8001 spricht kein TLS — HTTPS gibt es nur fuers Cockpit an 443), Token/Header LEER. Haengt die App beim ersten Verbinden kurz: warten bzw. App neu starten — der Eintrag ist bereits in `~/.codex/config.toml` gespeichert und funktioniert danach. | §2a |
| 7 | ⭐ Tools des Gehirns tauchen gar nicht auf | Reihenfolge pruefen: `enabled:true`? global `tools:{...false}` aktiv? Tunnel/Host-Header ok? Bei Claude: 64-Zeichen-Limit `mcp__<server>__<tool>` (unser `second-brain` ist kurz genug). `/status`(OpenCode) bzw. `/mcp`(Claude) zeigt den Verbindungsstatus. | §3 |
| 8 | ⭐ Server-only-Agent soll NUR das Gehirn nutzen | OpenCode: global `tools:{"*":false}`-Aequivalent vermeiden — gezielt `"second-brain*": true` im Agent + andere Tools `deny`. Claude Code: MCP-Tools-nur-fuer-Subagent ist seit #6915 moeglich. | §4 |
| 9 | Unerwartet hohe Token / teuer beim Server-Agenten | Jedes MCP-Tool-Schema laedt in JEDEN Prompt (~10-20k+ bei vielen Servern). Claude Tool Search ab 2.1.7 mildert automatisch (ihr: 2.1.187). Nicht gebrauchte MCP pro Agent deaktivieren. | §7 |
| 10 | ⭐ `-32001 Request timed out` obwohl der Tunnel STEHT (erster recall nach Idle/Standby) | WireGuard-**Cold-Start** (~11 s Handshake nach Leerlauf) sprengt einen zu knappen Client-Timeout; der recall selbst ist schnell (~0,5 s). FIX: `PersistentKeepalive=25` (Tunnel warm) + Client-`timeout` großzügig (OpenCode + Claude `120000`; Default 30000 zu knapp). | §5 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt (hier) | Best-Practice (`best-practices/opencode/server-agent-remote-mcp.md`) |
|----------------------|---------------------------------------------------------------------|
| §1 OpenCode-Remote-Anbindung | §1 OpenCode `mcp` sauber konfigurieren |
| §2 Claude-Code-Remote-Anbindung | §2 Claude `.mcp.json` / `claude mcp add` |
| §3 Tools erscheinen nicht | §3 Sichtbarkeit pruefen |
| §4 Server-only-Agent/Modus | §4 Agent eng scopen |
| §5 WireGuard/Host-Header | §5 Erreichbarkeit absichern |
| §6 Session/Reconnect | §6 Reconnect-Disziplin |
| §7 Token/Kosten | §7 Token sparen |

---

## 1. OpenCode `mcp { type:"remote" }` verbindet nicht / falsche Auth
**Symptom:** Der Remote-Server bleibt stumm; Auth-Fehler („Authorization header is badly formatted",
„transport connection failed"); oder `opencode mcp debug` meldet „failed", obwohl es eigentlich geht.
**Ursachen & FIX (funktionserhaltend):**
- **Env-Variable-Syntax:** In OpenCode-Headern gilt **`{env:VAR}`** — **NICHT** `${env:VAR}` (das Dollar
  bricht es). Falsch: `"Authorization": "Bearer ${env:TOKEN}"`. Richtig: `"Bearer {env:TOKEN}"`.
  Ebenso `{file:~/SK/...}` fuer Dateien (wie beim firecrawl-Key in unserer Config).
- **OAuth vs. API-Key:** OpenCode startet bei 401 automatisch Dynamic Client Registration (RFC 7591).
  Bei API-Key-Servern stoert das → `"oauth": false` + Key via `headers`. (Vertieft: `opencode-cli.md` §63.)
- **`opencode mcp debug` ist irrefuehrend:** zeigt teils „nicht erfolgreich", obwohl das Tool in der TUI
  funktioniert. Verlass dich auf `/status` in der TUI + einen echten Tool-Aufruf, nicht allein auf debug.
- **Unser sb-mcp braucht KEINEN Header:** Der WireGuard-Tunnel ist der private Kanal. OpenCode 1.17.18
  verwendet wegen seines fehlerhaften Remote-/SSE-Fallbacks derzeit ausnahmsweise einen lokalen
  `type:"local"`-Eintrag mit gepinntem `mcp-remote@0.1.38`, `--transport http-only`, `--allow-http`,
  `--silent` und `timeout:30000`. Der Proxy zeigt weiterhin auf `http://10.8.0.1:8001/mcp` und braucht
  keine Header. Die allgemeine `type:"remote"`-Syntax bleibt für nicht betroffene OpenCode-Versionen
  korrekt. Werkzeuge erscheinen als `second-brain_remember`/`_recall`/…
**Versionen:** OpenCode 1.17.9 (Syntax `{env:}` stabil). **Quelle:** opencode.ai/docs/mcp-servers ·
github.com/github/github-mcp-server/issues/1396 (Header-Syntax, `mcp debug` irrefuehrend).

**Kategorie-Abrufe:** `get_category_item` nie auffächern. Index 1 liefert `total`; danach 2 bis `total`
streng nacheinander abrufen. Ein 15-facher Parallelburst kann lokal zwischen OpenCode-STDIO-Client und
Proxy hängen, bevor der Server einen Request sieht. Unabhängige Volltextabrufe sind davon getrennt:
Seit sb-mcp 1.3.3 antwortet der Server zustandslos direkt als JSON und wurde mit drei parallelen großen
Dokumenten produktiv verifiziert.

## 2. Claude Code: Remote-MCP-Eintrag in `.mcp.json` startet nicht
**Symptom:** `/mcp` zeigt `✘ failed`, `/tools` listet nur die eingebauten Tools; Child-Prozess
terminiert nach ~600 ms; `MCP error -32000: Connection closed` (v.a. auf Windows).
**Ursache:** Der Fehler tritt vor allem auf, wenn ein **stdio-Wrapper** (`npx -y mcp-remote <url>`) genutzt
wird — dessen Kindprozess-Management bricht auf Windows sofort ab (#3423, CLOSED/completed), obwohl der
manuelle `npx`-Aufruf klappt. Verwandt: #4097 (transport closes after init, DUPLICATE), #1663 (infinite hang SSE).
**FIX (funktionserhaltend):**
- **Direkten HTTP-Transport** nutzen statt `mcp-remote`-Wrapper:
  ```json
  { "mcpServers": { "second-brain": { "type": "http", "url": "http://10.8.0.1:8001/mcp" } } }
  ```
  bzw. `claude mcp add --transport http second-brain http://10.8.0.1:8001/mcp`. Kein npx-Kindprozess →
  die Windows-stdio-Falle entfaellt.
- **SSE ist deprecated** → immer `--transport http` (Streamable HTTP), was unser sb-mcp ohnehin spricht.
- **Scope bewusst:** `--scope local` (nur du, Default) / `project` (`.mcp.json` im Repo, Team — fragt vor
  Nutzung nach Freigabe, Reset: `claude mcp reset-project-choices`) / `user` (projektuebergreifend).
  Fuer ein privates Gehirn ist `user` oder `local` sinnvoll — NICHT `project` ins geteilte Repo (jede
  Session bekaeme die Freigabe-Frage; und die WireGuard-IP gehoert nicht in ein geteiltes `.mcp.json`).
- **Flag-Reihenfolge:** alle Flags (`--transport`/`--header`/`--scope`) VOR dem Servernamen; `--` trennt
  Server-Argumente.
**Versionen:** Claude Code 2.1.187; #3423 CLOSED/completed. **Quelle:** code.claude.com/docs (mcp) ·
github.com/anthropics/claude-code/issues/3423 · /issues/4097.

## 2a. Codex-App (OpenAI Desktop): Gehirn anbinden — bestaetigte Werte + Erst-Verbindungs-Haenger
**Symptom:** Beim Hinzufuegen des sb-mcp in der Codex-App (Einstellungen → Integrationen → MCP Server)
scheint die App sich aufzuhaengen; unklar, ob der Eintrag gespeichert wurde.
**Befund (live verifiziert 2026-07-04):** Der Dialog schreibt den Eintrag SOFORT nach
`~/.codex/config.toml` (`[mcp_servers.second-brain]` mit `enabled = true` + `url`), auch wenn die UI
noch haengt. Der Server war dabei nachweislich gesund (initialize-Handshake per curl in 0,08 s, HTTP 200).
Das Haengen ist ein transienter Erst-Verbindungs-Effekt der App — nach Warten/App-Neustart lief die
Verbindung ohne weitere Aenderung.
**Bestaetigte Eintrags-Werte (Codex-App-Dialog):**
- Name: `second-brain` · Umschalter: **Streamable HTTP** (nicht STDIO)
- URL: `http://10.8.0.1:8001/mcp` — **HTTP, nicht HTTPS** (Port 8001 spricht kein TLS; HTTPS existiert
  auf dem VPS nur fuer das Cockpit an `https://10.8.0.1:443` via Caddy → Mikrofon-secure-context)
- Bearer-Token-Umgebungsvariable / Header / Header-aus-Env: **alles leer** (WireGuard-Tunnel ist der Kanal)
**FIX (funktionserhaltend):** Werte wie oben eintragen, speichern; haengt die UI → kurz warten oder App
neu starten (Config ist schon geschrieben), NICHT den Eintrag loeschen/neu anlegen. Voraussetzung wie
immer §5: WireGuard-Tunnel aktiv.
**Versionen:** Codex-App 26.623.101652 (Windows), sb-mcp 1.3.2. **Quelle:** eigener Live-Vorfall
2026-07-04 (curl-Handshake + config.toml-Inspektion), als solcher gekennzeichnet.

## 3. Tools des Gehirns tauchen nicht auf / werden nicht genutzt
**Symptom:** Verbindung gilt als „connected", aber keine `second-brain_*`-Tools; oder das Modell ruft sie nie.
**Ursachen & FIX (funktionserhaltend):**
- **Generische OpenCode-Falle „connected, aber keine Tools":** siehe `opencode-cli.md` **§59** (SSE-
  Registrierung scheitert still → auf `streamable-http`/`type:local`-Proxy; unser Server ist bereits
  streamable-http → meist ok). `/status` (OpenCode) bzw. `/mcp` (Claude) zeigt den echten Status.
- **`enabled` / globale Deaktivierung:** In OpenCode laedt der Server nur bei `enabled:true`. Zusaetzlich
  kann ein globales `tools:{"second-brain*": false}` ODER eine Per-Agent-Beschraenkung die Tools
  ausblenden (siehe §4) — pruefen, ob du sie aus Versehen global deaktiviert hast.
- **Claude Code 64-Zeichen-Tool-Namen-Limit (#19882, NOT_PLANNED):** der Name `mcp__<server>__<tool>`
  darf max. 64 Zeichen haben, sonst API-400 + die ganze Session blockiert. Unser `second-brain` +
  kurze Tool-Namen (`remember`/`recall`/…) bleiben weit darunter → kein Problem. Bei langen Server-/
  Tool-Namen: Server-Name kurz halten. **Tool Search (ab Claude 2.1.7, ihr: aktiv)** entschaerft das,
  indem Schemata erst bei Bedarf geladen werden.
- **Modell ruft Tools nicht:** klare Tool-Beschreibungen (hat unser sb-mcp), und den Agenten im Prompt
  ausdruecklich aufs Gehirn hinweisen.
**Versionen:** Claude 2.1.187 / OpenCode 1.17.9. **Quelle:** github.com/anthropics/claude-code/issues/19882 ·
opencode.ai/docs/mcp-servers · `opencode-cli.md` §59.

## 4. Server-only-Agent/Modus bauen (nur das Gehirn, sonst nichts)
**Ziel:** Ein Agent/Modus, der ausschliesslich mit dem Server/Gehirn arbeitet (z.B. „erinnere/finde",
keine lokalen Datei-Edits).
**OpenCode (funktionserhaltend):**
- **Tool-Scope pro Agent** via `permission` (empfohlen) oder `tools` (deprecated, aber praktisch fuer
  Glob-Scoping). Per-Agent-Muster (offiziell dokumentiert): global aus, im Agent gezielt an:
  ```json
  { "tools": { "second-brain*": false },
    "agent": { "gehirn": {
        "mode": "primary",
        "tools": { "second-brain*": true, "write": false, "edit": false },
        "permission": { "edit": "deny", "bash": "deny" } } } }
  ```
- **Glob-Syntax:** `*`/`?`; `"second-brain*"` matcht alle Gehirn-Tools auf einmal.
- **Eingebaute Read-only-Subagenten** als Vorlage: `explore`/`scout` (read-only), `general` (voll).
  Markdown-Agenten unter `~/.config/opencode/agents/<name>.md`.
- **Lowercase-Keys:** Permission-/Tool-Keys MUESSEN klein sein — PascalCase wird STILL ignoriert
  (`opencode-cli.md` §18). Subagent erbt das Modell des Primary, nicht das globale Default → `model:`
  explizit setzen (`opencode-cli.md` §45, Kurzcheck-Bezug §6).
**Claude Code:** MCP-Tools NUR fuer einen Subagenten freizugeben war lange nicht moeglich — seit
**#6915 (CLOSED/completed)** geht es: dem Subagenten in seiner Definition gezielt die MCP-Tools zuweisen,
ohne sie dem Haupt-Agenten anzubieten. (Mechanik in der jeweiligen Agent-/Subagent-Konfiguration.)
**Versionen:** OpenCode 1.17.9 / Claude 2.1.187. **Quelle:** opencode.ai/docs/agents · /docs/mcp-servers
(„Per agent") · github.com/anthropics/claude-code/issues/6915.

## 5. ⭐ Nur ueber WireGuard erreichbar + DNS-Rebinding/Host-Header
**Symptom (a):** Die CLI „sieht nichts" / `connection refused` / Timeout, obwohl die Config stimmt.
**Symptom (b):** TCP steht, aber der Server antwortet „invalid host header" / „Request validation failed".
**Ursachen & FIX (funktionserhaltend):**
- **(a) Tunnel down:** `10.8.0.1:8001` existiert NUR im WireGuard-Tunnel (Docker published den Port
  ausschliesslich an die WG-IP). Ohne aktives `wg`-Interface gibt es keine Route → `connection refused`/
  Timeout — KEIN MCP-Bug. **Vor dem CLI-Start Tunnel sicherstellen:** `wg show` / `ping 10.8.0.1` /
  `curl -i http://10.8.0.1:8001/mcp` (erwartet eine MCP-Antwort, nicht „refused"). (Tunnel-Details:
  `server/wireguard.md`.) Bricht der Tunnel mitten in der Session → wie §6 (reconnect).
- **(b) DNS-Rebinding-Schutz:** Das MCP-Python-SDK validiert den `Host`-Header gegen `allowed_hosts`
  (SDK-Default nur `localhost`/`127.0.0.1`). Unser sb-mcp setzt bewusst
  `allowed_hosts=[10.8.0.1:8001, 10.8.0.1, 127.0.0.1:8001, localhost:8001]` (server.py). **Client-Falle:**
  Die in der CLI eingetragene URL muss einen Host/Port verwenden, der **exakt** in dieser Liste steht —
  also `http://10.8.0.1:8001/mcp`. Ein abweichender Hostname (DNS-Name), eine andere IP des Servers oder
  ein anderer Port → „invalid host header"/Request abgewiesen. Loesung: entweder die Client-URL auf
  `10.8.0.1:8001` halten ODER server-seitig den genutzten Host in `allowed_hosts` aufnehmen (Server-Seite:
  `claude-tooling/mcp-server.md` §3.13).
- **Pfad:** Streamable-HTTP-Endpunkt ist `/mcp` (nicht `/sse`, nicht `/`). URL ohne `/mcp` → 404/kein Tool.
- **(c) Cold-Start-Timeout — Tunnel AKTIV, aber erster Aufruf nach Idle/Standby läuft in den Client-Timeout:**
  Symptom `MCP error -32001: Request timed out` (OpenCode/Claude), obwohl der Tunnel steht. Ursache: nach
  Leerlauf (PC-Standby/Boot) braucht WireGuard beim ERSTEN Paket einen neuen Handshake (~11 s gemessen,
  danach 0,05 s). Der recall serverseitig ist schnell (~0,5 s warm, ~1,8 s kalt — brain-api `/search` =
  Gemini-Embedding + Qdrant), also NIE der Flaschenhals — nur der Cold-Start sprengt einen zu knappen
  Client-Timeout. **FIX (2 Schichten, funktionserhaltend):** (1) Tunnel warm halten:
  `PersistentKeepalive = 25` im Client-`[Peer]` (32-Byte-Paket alle 25 s, kein spürbarer Overhead;
  `server/wireguard.md` §4). (2) Client-Timeout großzügig: OpenCode `mcp.<name>.timeout: 120000`
  (Default 30000 war zu knapp), Claude Code per-Server `"timeout": 120000` in `.mcp.json`
  (überschreibt `MCP_TOOL_TIMEOUT`; Werte <1000 werden ignoriert). Normalfall bleibt schnell — der
  Timeout ist nur die Obergrenze. Belegt: #47780 (2026-07-10).
**Versionen:** mcp-sdk>=1.27 (server). **Quelle:** github.com/nicolargo/glances/issues/3467 (Host-Header-
Allowlist, „invalid host header") · server.py (`TransportSecuritySettings`) · `server/wireguard.md`.

## 6. ⭐ Session/Reconnect: Server-Neustart killt die Streamable-HTTP-Session
**Symptom:** Nach einem sb-mcp-/Container-Neustart (oder Idle) liefern Tool-Aufrufe
`HTTP 404 … No valid session ID provided` bzw. „Session not found"; der MCP-Indikator wird rot; viele
Clients geben nach wenigen Versuchen auf („Stopping reconnection attempts" / „Maximum reconnection
attempts (2) exceeded") und reconnecten NICHT von selbst.
**Ursache:** Per MCP-Spec darf der Server eine Session jederzeit beenden und MUSS danach mit 404 auf die
alte `Mcp-Session-Id` antworten; der **Client** MUSS dann eine neue Session per `InitializeRequest`
(ohne Session-ID) starten. Mehrere Clients (LibreChat #11868, Cursor) handhaben das fehlerhaft und bleiben
auf der toten Session haengen. OpenCode hat generell **keinen** Auto-Reconnect/Keepalive (`opencode-cli.md`
§61).
**FIX (funktionserhaltend):**
- **Manuell reconnecten** nach jedem Server-Neustart: OpenCode → Server kurz `enabled:false`→`true`
  (bzw. `opencode mcp`-Reconnect/TUI neu); Claude Code → `/mcp` → reconnect. (Wie OFF/ON in Cursor.)
- **Server stabil halten** (Container nicht unnoetig neustarten); bei geplantem Neustart die CLI-Session
  danach neu mit dem MCP verbinden.
- Betroffenen Server bei Startproblemen temporaer `"enabled": false` setzen, damit die CLI nicht haengt
  (`opencode-cli.md` §61).
**Versionen:** MCP-Spec 2025-06-18/2025-11-25; clientseitig OpenCode 1.17.9 / Claude 2.1.187.
**Quelle:** github.com/danny-avila/LibreChat/issues/11868 (CLOSED/completed) · MCP-Spec „Session Management" ·
`opencode-cli.md` §61.

## 7. Token/Kosten beim Server-Agenten
**Symptom:** Schon eine triviale Anfrage verbraucht viele Tausend Tokens; mit mehreren MCP-Servern eskaliert es.
**Ursache:** Jedes verbundene MCP-Tool-Schema wird in JEDEN Prompt injiziert (dokumentiert: ~10-20k+
Tokens bei mehreren Servern, Extremfaelle 50k+). Bei Claude Code war das vor Tool Search teuer (#3406,
CLOSED/completed).
**FIX (funktionserhaltend):**
- **Claude Tool Search** (ab 2.1.7, ihr: 2.1.187 → aktiv) laedt Tool-Schemata erst bei Bedarf, sobald sie
  >10% des Kontexts kosten — automatisch. `ENABLE_TOOL_SEARCH` steuert das (Default `auto`).
- **Nur gebrauchte MCP laden:** unser sb-mcp hat 8 schlanke Tools → moderat. Beim Server-Agenten andere
  MCP (z.B. firecrawl) per Per-Agent-Scope (§4) deaktivieren, wenn nicht gebraucht.
- **OpenCode:** `compaction.prune:true` + nicht gebrauchte MCP global aus / pro Agent an (`opencode-cli.md`
  §8 Token-Hinweis, §11).
**Versionen:** Claude 2.1.187 / OpenCode 1.17.9. **Quelle:** github.com/anthropics/claude-code/issues/3406
(CLOSED/completed) · code.claude.com/docs (Tool Search) · `opencode-cli.md` §8/§11.

---

## 🔧 Fix-Status (gh-verifiziert 2026-06-24)

| Frueheres Problem | Status | Bezug |
|-------------------|--------|-------|
| Claude Code: Windows-Remote-MCP via `mcp-remote`-stdio bricht ab (#3423) | CLOSED/completed — **Workaround bleibt:** direkten `type:"http"` statt stdio-Wrapper nutzen | §2 |
| Claude Code: MCP-Tools nur fuer Subagent (#6915) | **CLOSED/completed** — ist jetzt moeglich (war frueher nicht) | §4 |
| Claude Code: 64-Zeichen-Tool-Namen-Limit (#19882) | CLOSED/**NOT_PLANNED** — kein Fix; **Tool Search ab 2.1.7 mildert** + Server-Namen kurz halten | §3 |
| Claude Code: MCP-Schemas-Token-Last bei jedem Prompt (#3406) | CLOSED/completed — durch **Tool Search ab 2.1.7** entschaerft | §7 |
| MCP Streamable-HTTP Session-404 nach Restart (LibreChat #11868) | CLOSED/completed (clientseitig dort) — **Muster bleibt**: Client muss neu initialisieren; OpenCode ohne Auto-Reconnect | §6 |
| Claude Code: MCP disconnects after init (#4097) | CLOSED/**DUPLICATE** | §2 |
| OpenCode generische Remote-MCP-Fallen (§59-§64a) | siehe `opencode/opencode-cli.md` (eigener Fix-Status dort) | §1/§3/§6 |

**Ehrlichkeit zur Methodik:** Issue-Stati per `gh issue view` direkt geprueft. Viele Punkte sind
**per-Design** (Token-Injektion, Host-Header-Validierung, Session-Lifecycle, WireGuard-Erreichbarkeit) —
kein „in Version X behoben", sondern Verhalten, das man richtig handhaben muss. Die WireGuard-„connection
refused"-Mechanik (§5a) ist aus dem konkreten Setup abgeleitet (Docker published nur an 10.8.0.1), nicht
aus einer Fremdquelle — als solche gekennzeichnet.

---

## ✅ Pflicht-Checkliste vor dem Anbinden/Operieren

- [ ] **WireGuard-Tunnel aktiv** (`ping 10.8.0.1` / `curl http://10.8.0.1:8001/mcp`), BEVOR die CLI startet. (§5)
- [ ] Client-URL exakt `http://10.8.0.1:8001/mcp` (Host/Port wie in `allowed_hosts`, Pfad `/mcp`). (§5)
- [ ] OpenCode: `type:"remote"`, `enabled:true`, KEIN Header noetig; Env-Header (falls je) mit `{env:VAR}` (kein `$`). (§1)
- [ ] Claude Code: `type:"http"` direkt (NICHT `mcp-remote`-stdio), Scope `user`/`local` (nicht `project` ins geteilte Repo). (§2)
- [ ] Tools sichtbar? `/status` (OpenCode) bzw. `/mcp` (Claude); Server-Name kurz (64-Zeichen-Limit Claude). (§3)
- [ ] Server-only-Agent: Tools eng scopen (`second-brain*` an, Rest `deny`), lowercase Keys, Subagent-`model` explizit. (§4)
- [ ] Nach Server-/Container-Neustart: MCP **manuell reconnecten** (kein Auto-Reconnect). (§6)
- [ ] Token im Blick: nur gebrauchte MCP pro Agent; Tool Search aktiv lassen. (§7)
