# Self-hosted Memory-Server für OpenCode UND Claude Code — Bauplan (Stand 2026-06-19)

> Ziel (Frank): EIN selbst gemieteter Server (VPS), der GLEICHZEITIG Claude Code und OpenCode
> mit dauerhaftem, semantischem Gedächtnis versorgt — alle Daten bleiben bei Frank.
> Recherchiert am 2026-06-19 (6-Researcher-Schwarm). Quellen je Abschnitt verlinkt.
> Gegenstück (Fallen): `bugs/opencode/opencode-cli.md` (Abschnitt „Self-hosted Memory-Server").
> Verwandt: `best-practices/opencode/plugins-mcp-skills.md` §8 (Kurzfassung + Verweis hierher).
> **Server/Infra (NEU 2026-06-19):** Anbieter-Wahl (Hostinger/Hetzner, Dimensionierung) →
> `best-practices/server/vps-hosting.md`; sicherer Zugriff per VPN statt öffentlicher Exposition →
> `best-practices/server/wireguard.md` (Split-Tunnel, Dienst nur über WireGuard).

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Frage | Antwort |
|-------|---------|
| Geht „ein Server, beide CLIs"? | **Ja.** Beide hängen sich an denselben MCP-Endpunkt (`…/mcp`): Claude Code via `mcpServers`/`claude mcp add`, OpenCode via `mcp { type:"remote" }`. |
| Beste Basis? | **supermemory self-hosted** (MIT, **Single-Binary**, kein Docker/Postgres nötig, lokale WASM-Embeddings, MCP-nativ, Port 6767). |
| Ist supermemory cloud-only? | **NEIN** (verbreiteter Irrtum). Offizieller Self-Host: `npx supermemory local` / `curl …/install`. |
| Bleiben die Daten wirklich lokal? | Embeddings ja (on-device WASM). **ABER** der LLM-Extraktions-/Summary-Step nutzt per Default `gpt-5.1` (Cloud) → für volle Datenhoheit auf **lokales Ollama** umbiegen (`OPENAI_BASE_URL`). |
| Server-Größe? | supermemory-Binary genügsam (RAM-Ceiling default 1 GB). Mit lokalem Ollama-LLM mehr RAM. Komfort: 8-GB-VPS. |
| Wichtigste Sicherheits-Falle | Port **NIE** an `0.0.0.0`; Docker umgeht UFW → an `127.0.0.1` binden + Reverse-Proxy (Caddy, TLS+Bearer) + Cloud-Firewall. |

---

## 1. Optionen-Vergleich (recherchiert)

| Lösung | Self-Host-Aufwand | MCP nativ | Embeddings lokal | LLM-Step lokal möglich | Lizenz | Eignung „ein Server, beide CLIs" |
|--------|-------------------|-----------|------------------|------------------------|--------|----------------------------------|
| **supermemory (self-host)** | **niedrig** (Single-Binary, kein Docker/DB) | ja (HTTP `/mcp`) | ja (WASM on-device) | ja (Ollama via `OPENAI_BASE_URL`) | MIT | **beste** — fertige Plugins für BEIDE CLIs + reiner MCP-Weg |
| mem0 (self-host) | mittel-hoch (Qdrant + Neo4j + Ollama) | ja (Community-MCP `mem0-mcp-selfhosted`) | ja (Ollama) | ja | Apache-2.0 | gut, aber 3 Dienste zu betreiben |
| mcp-memory-service (doobidoo) | **niedrig** (SQLite-vec + ONNX, <100 MB) | ja | ja (ONNX) | n/a (kein LLM-Step nötig) | Apache-2.0 | gut für schlank/Claude-Code; „zwei CLIs"-Story schwächer |
| Hindsight (Vectorize) | niedrig (1 Docker-Cmd, embedded Postgres) | ja (MCP-first) | **unklar** (vor Einsatz prüfen) | LLM via Ollama mgl. | MIT | reichhaltig, aber Embedding-Lokalität offen |
| basic-memory | niedrig (pip, Markdown, stdio) | ja (~20 Tools) | ja (zero-config) | n/a | AGPL-3.0 | Markdown-transparent, aber stdio (lokal), nicht remote-server-typisch |

**Empfehlung:** **supermemory self-hosted** als Primärweg (genau auf „ein MCP-Server, beide CLIs"
ausgelegt, geringster Infra-Aufwand, lokale Embeddings). Wenn maximal-lokal + Graph gewünscht und
mehr Infra ok: **mem0**. Wenn nur Claude Code + minimal: **mcp-memory-service**.

Quellen: [supermemory self-hosting](https://supermemory.ai/docs/self-hosting/overview) ·
[mem0 self-host](https://mem0.ai/blog/self-host-mem0-docker) ·
[mcp-memory-service](https://deepwiki.com/doobidoo/mcp-memory-service) ·
[Hindsight](https://hindsight.vectorize.io/guides/2026/04/16/guide-run-hindsight-as-a-local-mcp-server) ·
[basic-memory](https://www.plugable.io/mcp/basicmachines-co/basic-memory)

---

## 2. Empfohlene Architektur (supermemory self-hosted)

```
Internet
  → Hetzner Cloud-Firewall (nur 443 offen)
  → Caddy (Auto-HTTPS/Let's Encrypt + Bearer-Token-Auth)
  → 127.0.0.1:6767  supermemory-server (Single-Binary)
       ├─ eingebettete Graph-Engine + lokale WASM-Embeddings (Daten in ./.supermemory)
       └─ LLM-Step (Summary/Chunking/Extraktion) → lokales Ollama (für volle Datenhoheit)
```

- **Single-Binary, kein Docker/Postgres nötig** (eingebettete Graph-Engine, on-device WASM-Embeddings).
  Postgres+pgvector ist NUR der Enterprise-/Cloudflare-Skalierungspfad — für einen Einzelnutzer-VPS
  unnötig. (`s11ngh/supermemory-selfhosted` ist ein SEPARATER Community-Fork — nicht das offizielle Binary.)
- Embeddings laufen lokal. Der **LLM-Step** (Memories extrahieren/zusammenfassen) braucht ein
  LLM — per Default `gpt-5.1` (Cloud). Für „Daten bleiben bei mir" auf **Ollama** umbiegen.

Quelle: [self-hosting/overview](https://supermemory.ai/docs/self-hosting/overview), [configuration](https://supermemory.ai/docs/self-hosting/configuration)

---

## 3. Bauplan Schritt für Schritt

### 3.1 VPS mieten
- **Empfehlung: Hetzner Cloud CX33** (4 vCPU, 8 GB RAM, 80 GB SSD, ~6,49 €/Mon, EU/DE → Datenhoheit).
  4 GB würde technisch reichen; 8 GB gibt Luft (Page-Cache, kleines Ollama-Modell).
- Alternativen: Contabo (mehr Disk), Netcup. (Orientierung Stand 2026.)
- Achtung: 8-GB-Ollama reicht NICHT für große Modelle (z.B. `gpt-oss:20b`) — für den LLM-Step ein
  **kleines** Modell wählen (z.B. `llama3.2:3b`/`qwen2.5:3b`) ODER den LLM-Step bei einer billigen
  Cloud-API belassen (nur Extraktion, Embeddings/Memories bleiben lokal).

Quelle: [bitdoze Hetzner-Preise](https://www.bitdoze.com/hetzner-cloud-cost-optimized-plans/)

### 3.2 Grundhärtung (vor allem anderen)
- SSH key-only, Root-Login aus, PasswordAuth aus. Fail2ban (SSH-Brute-Force).
- **Cloud-Firewall des Hosters** default-deny, nur 22/80/443 (greift VOR Docker/iptables — umgeht
  die Docker-UFW-Falle komplett).

### 3.3 supermemory installieren
```bash
curl -fsSL https://supermemory.ai/install | bash      # oder: npx supermemory local / bunx supermemory local
supermemory-server                                     # gibt URL http://localhost:6767, API-Key sm_..., Org-ID, Datenpfad ./.supermemory
```
- Port: `6767` (env `PORT`/`SUPERMEMORY_PORT`). Daten: `./.supermemory` (env `SUPERMEMORY_DATA_DIR`).
  API-Keys verschlüsselt in `~/.supermemory/env`.

### 3.4 LLM-Step auf lokales Ollama (für Datenhoheit)
```bash
# Ollama installieren + kleines Modell ziehen
ollama pull llama3.2:3b          # klein genug für 8-GB-VPS
```
env für supermemory (OpenAI-kompatibel → Ollama):
```
OPENAI_BASE_URL=http://localhost:11434/v1
OPENAI_API_KEY=ollama
OPENAI_MODEL=llama3.2:3b
SUPERMEMORY_DISABLE_TELEMETRY=1
```
(Embeddings sind ohnehin lokal/WASM; RAM-Ceiling `SUPERMEMORY_EMBEDDING_RAM_LIMIT` default `1gb`.)

### 3.5 Dauerbetrieb (kein offizielles systemd-Unit — selbst bauen)
`/etc/systemd/system/supermemory.service`:
```ini
[Unit]
Description=supermemory server
After=network.target
[Service]
ExecStart=/usr/local/bin/supermemory-server
Restart=always
EnvironmentFile=/etc/supermemory.env
WorkingDirectory=/var/lib/supermemory
[Install]
WantedBy=multi-user.target
```
`systemctl enable --now supermemory`.

### 3.6 TLS + Auth via Caddy (Reverse-Proxy)
`Caddyfile`:
```
memory.dein-vps.de {
    @auth header Authorization "Bearer DEIN_LANGES_RANDOM_TOKEN"
    handle @auth { reverse_proxy 127.0.0.1:6767 }
    respond "Unauthorized" 401
}
```
- Caddy macht Auto-HTTPS (Let's Encrypt, Renewal automatisch). supermemory bleibt an `127.0.0.1`
  (NIE `0.0.0.0`). Bearer-Token lang + zufällig.

### 3.7 Backup
- supermemory speichert in `./.supermemory` → dieses Verzeichnis sichern (tar + Offsite, z.B.
  Hetzner Storage Box/S3). Embeddings sind nicht trivial neu generierbar → Offsite ist Pflicht.
- (Falls doch der Postgres-Pfad genutzt wird: `pg_dump -Fc` per Cron + Offsite + getesteter Restore.)

Quellen: [Caddy reverse_proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy) ·
[Docker-UFW-Falle](https://www.jeffgeerling.com/blog/2020/be-careful-docker-might-be-exposing-ports-world/) ·
[self-hosting/configuration](https://supermemory.ai/docs/self-hosting/configuration)

---

## 4. Beide CLIs anbinden (ein Server, zwei Tools)

### Claude Code (MCP nativ, remote HTTP)
```bash
claude mcp add --transport http memory https://memory.dein-vps.de/mcp \
  --header "Authorization: Bearer ${MEMORY_TOKEN}" --scope user
```
oder direkt in der MCP-Config:
```json
{ "mcpServers": { "memory": {
  "url": "https://memory.dein-vps.de/mcp",
  "headers": { "Authorization": "Bearer sm_dein_key", "x-sm-project": "proggs" }
}}}
```

### OpenCode (remote MCP — empfohlen, umgeht die Plugin/Pro-Frage)
`~/.config/opencode/opencode.jsonc`:
```jsonc
{ "mcp": { "memory": {
  "type": "remote",
  "url": "https://memory.dein-vps.de/mcp",
  "enabled": true,
  "oauth": false,
  "headers": { "Authorization": "Bearer {file:~/SK/OpenCode/memory-token}" }
}}}
```
- Alternativ das OpenCode-**Plugin** `opencode-supermemory` (`bunx opencode-supermemory@latest install`,
  `"plugin":["opencode-supermemory"]`) — bringt Auto-Trigger mit, verlangt laut Doku aber den
  **Pro-Plan**. Beim Self-Host daher i.d.R. der **remote-MCP-Weg** (oben). (Exakter Plugin-Paketname
  vor Einsatz prüfen — Quellen nennen `opencode-supermemory` UND `@supermemory/opencode`.)

### Scoping (Trennung pro Tool/User/Projekt)
- `x-sm-project: <projekt-id>` (z.B. Verzeichnis-Hash) + eigener `sm_`-Key/OAuth-Account pro „Identität"
  (z.B. aus git-Email). Beide CLIs können denselben Server parallel nutzen (zustandslos pro Request).

### MCP-Tools von supermemory
`memory` (speichern/entfernen), `recall` (suchen, gibt Memories + Profil), `context` (volles Profil injizieren).

Quellen: [supermemory MCP setup](https://supermemory.ai/docs/supermemory-mcp/setup) ·
[OpenCode integration](https://supermemory.ai/docs/integrations/opencode) ·
[OpenCode MCP docs](https://opencode.ai/docs/mcp-servers) · [Claude Code MCP](https://code.claude.com/docs)

---

## 5. Datenschutz & Trigger
- **`<private>…</private>`-Tags**: Inhalt darin wird (plugin-seitig) vor dem Speichern entfernt
  („never persists"). Beim reinen MCP-Weg ist diese Redaction NICHT serverseitig garantiert →
  vorab selbst schwärzen oder per Hook.
- **Trigger** (Plugin-Weg): Auto-Read bei Session-Start, Keyword-Save („remember"/„save this"),
  Compaction-Save bei ~80 % Kontext (`compaction threshold 0.80`). **Reiner MCP-Weg (Claude Code)
  hat diese Auto-Trigger NICHT** — per Hook nachbauen (SessionStart-Read, Stop/Compaction-Save),
  sonst speichert nichts automatisch.
- Memory-Typen: project-config, architecture, error-solution, preference, learned-pattern, conversation.
  Scopes: `user` (cross-project) / `project` (default, isoliert).

---

## 6. Fallen (Kurz; Volltext im Almanach `bugs/opencode/opencode-cli.md`)
- supermemory ist NICHT cloud-only (Self-Host = Single-Binary); npm `supermemory` ist die SDK/CLI-Lib, NICHT der Server.
- LLM-Step default Cloud (`gpt-5.1`) → für Datenhoheit auf Ollama umbiegen.
- Port NIE an `0.0.0.0`; Docker umgeht UFW → `127.0.0.1` + Caddy + Cloud-Firewall.
- Remote-MCP: Claude Code ignoriert SSE-Timeout-Config (~5-min-Abbruch) → HTTP-Transport nutzen;
  OpenCode reconnectet nicht bei Idle-Disconnect; Session-404 nach VPS-Neustart erzwingt Re-Init.
- OpenCode-Plugin `opencode-supermemory` verlangt angeblich Pro-Plan → remote-MCP-Weg nehmen.

---

## Quellen (Stand 2026-06-19)
- supermemory: self-hosting [overview](https://supermemory.ai/docs/self-hosting/overview) / [quickstart](https://supermemory.ai/docs/self-hosting/quickstart) / [configuration](https://supermemory.ai/docs/self-hosting/configuration); [MCP setup](https://supermemory.ai/docs/supermemory-mcp/setup); [OpenCode](https://supermemory.ai/docs/integrations/opencode) / [Claude Code](https://supermemory.ai/docs/integrations/claude-code) integration; [npm](https://www.npmjs.com/package/supermemory) (4.24.12).
- Alternativen: [mem0 self-host](https://mem0.ai/blog/self-host-mem0-docker), [mcp-memory-service](https://deepwiki.com/doobidoo/mcp-memory-service), [Hindsight](https://hindsight.vectorize.io/guides/2026/04/16/guide-run-hindsight-as-a-local-mcp-server), [basic-memory](https://www.plugable.io/mcp/basicmachines-co/basic-memory).
- Infra/Security: [Hetzner-Preise](https://www.bitdoze.com/hetzner-cloud-cost-optimized-plans/), [Caddy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy), [Docker-UFW-Falle](https://www.jeffgeerling.com/blog/2020/be-careful-docker-might-be-exposing-ports-world/), [pgvector/HNSW](https://www.crunchydata.com/blog/hnsw-indexes-with-postgres-and-pgvector), [Ollama nomic-embed](https://ollama.com/library/nomic-embed-text), [Postgres-Docker-Backup](https://serversinc.io/blog/automated-postgresql-backups-in-docker-complete-guide-with-pg-dump/).
- MCP-Verbindung: [OpenCode MCP](https://opencode.ai/docs/mcp-servers), [Claude Code Docs](https://code.claude.com/docs).
