# Multi-Client-Zugriff (MCP + REST-API) — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens. Behandelt, wie EIN Memory-Server gleichzeitig von vielen Clients genutzt
> wird: CLIs (Claude Code, OpenCode) via MCP UND eigene Apps (z. B. Sprach-App im Auto) via REST.
> Quellen: `extern` (MCP-Praxisberichte, MCP-Spec-Timeline). Ergänzt das bestehende
> [[../opencode/self-hosted-memory-server]] (das die supermemory-MCP-Anbindung + Remote-MCP-Fallen schon hat).
> Schwester-Dateien: [[memory-backends]], [[server-infrastruktur]], [[schreibpfad-ingestion]].

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Mehrere Clients, ein Store? | **MCP ist designbedingt client-agnostisch** — ein Server bedient Claude Code, OpenCode, Cursor, ChatGPT … gleichzeitig ("one memory, many clients") |
| Protokoll | MCP = JSON-RPC 2.0, drei Primitive: **Tools** (Aktionen), **Resources** (lesbare Daten), **Prompts** (Templates) |
| Eigene Apps (Voice etc.) | **Separate REST-Schicht** auf demselben Store (recall/store/context) — MCP ist NICHT REST; beide Wege parallel anbieten |
| Auth pragmatisch | jeder MCP-Client reicht Credentials anders durch → drei Wege gleichzeitig unterstützen: URL-Query `?key=`, Custom-Header (`x-brain-key`), Bearer-Token |
| Auth spec-konform | OAuth 2.1 (seit MCP 03/2025): Remote-MCP = OAuth Resource Server, `.well-known`-Metadata, Bearer-Validierung, Step-up für Hochrisiko-Tools |
| Credentials | eng scopen, dedizierte Keys mit Minimal-Rechten, Secrets als Env-Var nicht in Config (→ [[server-infrastruktur]]) |
| Remote-MCP-Falle | Transport wandert schnell (SSE → Streamable HTTP …); Session-State skaliert nicht stateless; **Session-404 nach Server-Neustart** (stateful Session-ID + Restart) → Re-Init nötig |
| Voice-App-Anbindung | STT → REST-`store`/`recall` → TTS-Bestätigung; Schreib-Call async (siehe [[schreibpfad-ingestion]] §5) |

---

## 1. Multi-Client-Architektur (`extern`)
MCP-Rollen: **Host** (Anwendung, führt aus), **Client** (Komponente im Host, verbindet zu Servern),
**Server** (exponiert Tools/Daten). Regel: "Jeder Client hält eine 1:1-Verbindung zu jedem Server; ein
Host kann viele Server gleichzeitig verbinden." Gleichzeitiger Zugriff verschiedener Client-Typen auf
denselben Store ist genau das, wofür MCP gebaut ist ("one memory, four clients, zero re-explanation").
Für Frank: Der Memory-Server exponiert EINEN MCP-Endpunkt; Claude Code und OpenCode hängen sich beide dran.

**Lücke:** Mechanismen für nebenläufige Schreib-/Lesekonflikte (Locking, Transaktionen, Versionierung) auf
dem geteilten Store nennen die Quellen NICHT — beim Backend (Mem0/Qdrant) klären (siehe [[memory-backends]]).

## 2. API-Design (`extern` + abgeleitet)
- **MCP-Seite (belegt):** JSON-RPC 2.0, Methoden `initialize`, `tools/list`, `tools/call`, `resources/list`,
  `prompts/list`, `ping`. Ein minimaler Server ist ~60 Zeilen ohne SDK (GET = SSE-Discovery, POST = Aufrufe).
- **REST-Seite (abgeleitet — Quellen beschreiben nur MCP):** Für eigene Apps eine schlanke REST-Schicht mit
  drei Kern-Endpunkten parallel zum MCP-Server: `POST /store` (neues Wissen + Scope/Kategorie),
  `GET /recall?q=…&scope=…` (hybride Suche), `GET /context?scope=…` (Profil/Kontext-Injektion). supermemory
  nennt genau diese drei MCP-Tools (`memory`/`recall`/`context`, siehe [[../opencode/self-hosted-memory-server]] §4) —
  die REST-Endpunkte spiegeln sie. Das hält MCP und REST funktionsgleich auf demselben Store.

## 3. Authentifizierung & Scoping (`extern`)
- **Pragmatisch (persönliches System):** drei Auth-Wege gleichzeitig anbieten (URL-Query, Custom-Header,
  Bearer), weil jeder Client anders übergibt. Key serverseitig pro Request prüfen.
  - Claude/Claude.ai: URL mit Key-Parameter in Connector-Settings; **Claude Code erbt die Claude.ai-Config**.
  - Cursor: URL in `mcp.json`. ChatGPT: wählerisch bei Auth-Headern (kann Sonderbehandlung brauchen).
- **Spec-konform (OAuth 2.1, seit 03/2025, verfeinert 06+11/2025):** Remote-MCP = OAuth Resource Server,
  Protected-Resource-Metadata unter `.well-known`, Delegation an Auth-Server (Auth0/Keycloak),
  Bearer-Validierung, Step-up-Authorization für privilegierte Tools; ab 11/2025 Client-ID-Metadata-Documents
  statt Dynamic Client Registration.
- **Härtung:** Credentials eng scopen, dedizierte Minimal-Rechte-Keys, niemals Prod-Credentials wiederverwenden,
  Secrets als Env-Var.
- **Lücke:** Projekt-Header-Scoping (`X-Project-Id`) nicht belegt — aber supermemory nutzt `x-sm-project`
  (siehe [[../opencode/self-hosted-memory-server]] §4); für Frank: pro Kategorie/Domäne einen Scope-Header.

## 4. Versionierung & Rate-Limits (`extern` / Lücke)
- **Protokoll-Versionen:** SDKs pflegen Rückwärtskompatibilität (mcp-go: Spec 2025-11-25 + zurück bis
  2024-11-05). Eigenes Backend: API versionieren (URL-Version oder `Accept`-Header) — Detail nicht belegt.
- **Rate-Limits:** in den Quellen NICHT behandelt — für ein Einzelnutzer-System unkritisch, aber ein
  einfaches Per-Key-Limit am Reverse-Proxy (siehe [[server-infrastruktur]]) ist sinnvoll.

## 5. Remote-MCP-Fallen (`extern`, teils indirekt)
- Transport-Layer wandert schnell (SSE → Streamable HTTP → …) — "keine Prod-Infra auf eine Spec bauen, die
  ihren Transport in 6 Monaten deprecatet". Roadmap 03/2026: stateless horizontal scaling (heutiges
  Session-Handling skaliert NICHT stateless).
- **Session-404 nach Server-Neustart:** plausibel (stateful Session-IDs + Restart = ungültige Sessions),
  in den Quellen dieses Laufs nur indirekt — ABER im bestehenden [[../opencode/self-hosted-memory-server]] §6
  bereits als belegte Falle dokumentiert (Claude Code ignoriert SSE-Timeout → HTTP-Transport; OpenCode
  reconnectet nicht bei Idle-Disconnect; Session-404 nach VPS-Neustart erzwingt Re-Init). → HTTP-Transport
  bevorzugen, Reconnect/Re-Init im Client einplanen.

## 6. Voice-App-Anbindung (`extern` / abgeleitet)
Quellen zur konkreten STT/TTS-Anbindung: keine. Abgeleitet aus dem Mem0/ElevenLabs-Muster
([[schreibpfad-ingestion]] §5): Sprach-App nutzt die REST-Schicht — STT erzeugt Text → `POST /store`
(async, klassifiziert) bzw. `GET /recall` → TTS liest Antwort/Bestätigung vor. User-ID aus der App-Auth.

## Offene Lücken
REST-Design-Details, Projekt-Header-Scoping, Rate-Limits, vollständige Voice-Integration und konkrete
Timeout-/Reconnect-Defaults sind in den Quellen dieses Laufs nicht belegt (teils im bestehenden
self-hosted-memory-server-Almanach vorhanden) — als "abgeleitet" markiert. Zusätzliche Primärquellen
(MCP-Spec, OAuth-2.1-RFCs, FastAPI/Starlette-Doku) für die Detailumsetzung heranziehen.

## Quellen (`extern`, 2025-2026)
MCP-Praxisbericht "one memory many clients" (Auth-Wege, OAuth-2.1-Timeline, scharfe Kanten); MCP-Architektur-
Erklärung (Host/Client/Server, JSON-RPC, Primitives, Security-Empfehlungen); mcp-go (Spec-Kompatibilität,
Session-Management). Ergänzend: bestehender Almanach `best-practices/opencode/self-hosted-memory-server.md`.
