# Second-Brain-Server — Deployment-Konfiguration

Docker-Compose-Stack fuer Franks selbst gehostetes "zweites Gehirn" auf dem Hostinger VPS.
Strategie/Gesamtplan: `best-practices/second-brain/UMSETZUNGSPLAN.md`.

## Warum diese Dateien im Repo liegen
Portabilitaets-Versicherung fuer den geplanten Umzug (~2028): Der komplette Stack ist hier als
Code/Config gesichert und damit auf jedem Linux-Server 1:1 reproduzierbar. Beim Umzug genuegt:
neuen Server mieten -> diese Configs hochladen -> `.env` aus `~/SK/second-brain/` einspielen ->
`docker compose up -d --build` -> Daten-Volume `qdrant-data/` kopieren.

## Bausteine (Stand 2026-06-22)
| Dienst | Rolle | Port (nur 127.0.0.1) |
|--------|-------|----------------------|
| Qdrant | Vektor-DB / "Such-Schrank" (semantische + gefilterte Suche) | 6333 (REST), 6334 (gRPC) |
| mem0-api | "Bibliothekar": REST-Wrapper um Mem0 (Speichern/Abrufen) | 8000 |

**KI (Denken + Embeddings):** Google Gemini (Cloud) — `gemini-3.1-flash-lite` (LLM) +
`gemini-embedding-001` @ 1536 dim (Embeddings). Kein lokales Modell mehr (Ollama/BGE-M3 am
2026-06-22 entfernt: Datenabfluss ist egal -> Cloud ist schneller/besser und entlastet den 8-GB-Server).

## REST-Endpunkte (mem0-api)
Alle ausser `/health` brauchen den Header `Authorization: Bearer $SB_API_KEY`.

| Methode | Pfad | Zweck |
|---------|------|-------|
| GET  | `/health` | Status, Version, Qdrant-Erreichbarkeit, Modelle (ohne Auth) |
| POST | `/store` | Erinnerung speichern. Body: `{text|messages, user_id, metadata, infer}` |
| POST | `/recall` | Suchen. Body: `{query, user_id, limit}` |
| GET  | `/memories?user_id=frank` | Alle Erinnerungen eines Nutzers |

`infer=true` (Default) laesst Mem0 via Gemini Fakten extrahieren; `infer=false` speichert Rohtext.

## Sicherheit
- Alle Dienst-Ports nur an `127.0.0.1` gebunden (nicht oeffentlich). Einziger offener Port nach
  aussen ist SSH; der externe Zugang zum Gehirn kommt spaeter ueber Reverse-Proxy (Caddy+TLS) oder VPN.
- mem0-api laeuft als nicht-root-User, Bearer-Token-Auth auf allen Schreib-/Lese-Endpunkten.
- Secrets stehen in `.env` (NICHT im Repo, per globaler `.gitignore`-Regel `.env.*` ausgeschlossen —
  bewusst, damit nie ein echtes Secret committet wird). Backup der `.env` + SSH-Schluessel:
  `~/SK/second-brain/` auf Franks Rechner. Benoetigte Variablen (Vorlage, echte Werte nur lokal/Server):
  ```
  QDRANT_API_KEY=...        # langes Zufalls-Token (Such-Schrank)
  GEMINI_API_KEY=...        # Google Gemini, ein Key fuer LLM + Embeddings
  GOOGLE_API_KEY=...        # = GEMINI_API_KEY (Mem0 liest GOOGLE_API_KEY)
  SB_API_KEY=...            # REST-Bearer-Token: python -c "import secrets;print(secrets.token_urlsafe(48))"
  ```

## Zugang von außen (WireGuard-VPN)
Das Gehirn ist **öffentlich unsichtbar** — kein HTTP-Port ist im Internet offen. Der Zugriff
läuft ausschließlich über einen WireGuard-Tunnel (Split-Tunnel). Nur **UDP 51820** ist öffentlich.

- Server-Interface `wg0` = **10.8.0.1/24**. `mem0-api` ist an `10.8.0.1:8000` gebunden → nur über
  den Tunnel erreichbar (nicht via `eth0`/öffentlich; bewusst NICHT `0.0.0.0` wegen Docker-UFW-Falle).
- Clients erreichen das Gehirn dann unter **`http://10.8.0.1:8000`** (z.B. `…/health`, `…/store`).
- Boot-Reihenfolge abgesichert: `docker.service` startet nach `wg-quick@wg0` (systemd-drop-in
  `/etc/systemd/system/docker.service.d/wait-for-wireguard.conf`).
- **Kein** IP-Forwarding/MASQUERADE nötig (Dienst läuft AUF dem Host = Split-Tunnel; siehe
  `bugs/server/wireguard.md` #1). Server-Config: `/etc/wireguard/wg0.conf` (chmod 600).
- Secrets (Server- + Client-Keys, `.conf`, QR) liegen NUR auf dem Server + Backup
  `~/SK/second-brain/wireguard/` — niemals im Repo.

**Neues Gerät hinzufügen** (auf dem Server): Client-Keypair mit `umask 077; wg genkey | tee priv | wg
pubkey > pub` erzeugen, in `/etc/wireguard/wg0.conf` einen `[Peer]` mit `PublicKey` + freier
`AllowedIPs = 10.8.0.X/32` ergänzen, `wg syncconf wg0 <(wg-quick strip wg0)` (oder `systemctl restart
wg-quick@wg0`), Client-`.conf` (Address `10.8.0.X/24`, `Endpoint 168.231.83.205:51820`,
`AllowedIPs = 10.8.0.0/24`, `PersistentKeepalive = 25`) erzeugen und ans Gerät geben.

## Observability
mem0-api schreibt strukturierte JSON-Lines nach `/app/logs/mem0-api.jsonl` (Host: `./mem0-logs/`,
rotierend) und spiegelt sie nach stdout. Live mitlesen auf dem Server:
```bash
docker compose logs -f mem0-api                 # stdout-Strom
tail -f /opt/second-brain/mem0-logs/mem0-api.jsonl   # Logdatei
# nur Intent-Checkpoints (erwartet vs. tatsaechlich):
grep '"kind": "CHECKPOINT"' /opt/second-brain/mem0-logs/mem0-api.jsonl
```

## Deployment (auf dem Server, Verzeichnis /opt/second-brain)
```bash
docker compose up -d --build   # bauen + starten
docker compose ps              # Status
docker compose logs -f         # Logs
docker compose down            # stoppen
```

## Schnelltest (auf dem Server)
```bash
SB=$(grep '^SB_API_KEY=' .env | cut -d= -f2-)
curl -s 10.8.0.1:8000/health | python3 -m json.tool
curl -s -X POST 10.8.0.1:8000/store -H "Authorization: Bearer $SB" \
  -H 'Content-Type: application/json' \
  -d '{"text":"Frank trinkt morgens gerne gruenen Tee","user_id":"frank","metadata":{"category":"praeferenz"}}'
curl -s -X POST 10.8.0.1:8000/recall -H "Authorization: Bearer $SB" \
  -H 'Content-Type: application/json' -d '{"query":"Was trinkt Frank?","user_id":"frank"}'
```

## Offene TODOs
- Qdrant- und mem0-api-Image nach erstem Lauf auf feste Versionen pinnen (statt `latest`/Rebuild).
- Externen Zugang (Caddy+TLS+Domain ODER WireGuard) einrichten + MCP-Endpunkt fuer die CLIs.
- Harte LLM-Kosten-Caps in der Google AI Studio / Cloud Console setzen (Budget-Stopp, nicht nur Alert).
- Zweiter Speicher-Weg: RAG/Chunking fuer Dokumente (z.B. die 3 Direktiven) neben Mem0-Fakten.
- Offsite-Backup von `qdrant-data/` einrichten + monatlicher Restore-Test.
