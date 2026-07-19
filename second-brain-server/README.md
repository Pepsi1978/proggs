# Second-Brain-Server — Deployment-Konfiguration

Docker-Compose-Stack fuer Franks selbst gehostetes "zweites Gehirn" auf dem Hostinger VPS.
Strategie/Gesamtplan: `best-practices/second-brain/UMSETZUNGSPLAN.md`.

## Warum diese Dateien im Repo liegen
Portabilitaets-Versicherung fuer den geplanten Umzug (~2028): Der komplette Stack ist hier als
Code/Config gesichert und damit auf jedem Linux-Server 1:1 reproduzierbar. Beim Umzug genuegt:
neuen Server mieten -> diese Configs hochladen -> `.env` aus `~/SK/second-brain/` einspielen ->
`docker compose up -d --build` -> Daten-Volume `qdrant-data/` kopieren.

## Bausteine (Stand 2026-06-23)
| Dienst | Rolle | Port |
|--------|-------|------|
| Qdrant | Vektor-DB / "Such-Schrank" (semantische + gefilterte Suche) | 127.0.0.1:6333 (REST), 6334 (gRPC) |
| brain-api | **Wortwoertlicher 1:1-Dokument-Speicher** (qdrant-client + Gemini-Embedding direkt, KEIN mem0) | 10.8.0.1:8000 (WireGuard) |
| mcp (sb-mcp) | MCP-Server: macht das Gehirn als Werkzeuge fuer Claude Code/OpenCode verfuegbar | 10.8.0.1:8001 (WireGuard) |

**KI (NUR Embeddings, kein LLM im Speicher):** Google Gemini (Cloud) — `gemini-embedding-001` @ 1536 dim,
nur zum Wiederfinden. Seit 2026-06-23 KEIN LLM mehr (mem0 raus): Text geht 1:1 rein und 1:1 raus, keine
KI bearbeitet etwas im Speicher. Aufbereitung passiert VORHER client-seitig. (Ollama/BGE-M3 am 2026-06-22 entfernt.)

## REST-Endpunkte (brain-api)
Alle ausser `/health` und `/` brauchen den Header `Authorization: Bearer $SB_API_KEY`.

| Methode | Pfad | Zweck |
|---------|------|-------|
| GET    | `/health` | Status, Version, Qdrant-Erreichbarkeit, Anzahl Eintraege (ohne Auth) |
| POST   | `/store` | Text 1:1 speichern. Body: `{text, title?, category?, user_id}` (gleicher Titel ERSETZT) |
| GET    | `/by-title?title=…` | Exakt per Titel → ganzes Dokument 1:1 |
| GET    | `/by-category?category=…` | Alle Eintraege einer Kategorie |
| GET    | `/by-date?date=YYYY-MM-DD` | Eintraege eines Speichertags |
| POST   | `/search` | Semantische Suche. Body: `{query, user_id, limit, category?, date?, date_from?, date_to?}` |
| GET    | `/list?user_id=frank` | Titel/Kategorie/Groesse (ohne Volltexte) |
| DELETE | `/by-title?title=…` | Eintrag per Titel loeschen |

Speicher ist **wortwoertlich 1:1** (keine Fakten-Extraktion). Lange Texte werden NUR fuer die Suche
gechunkt; der volle Text liegt 1:1 im Payload. Schema-Details: `best-practices/second-brain/speicher-schema-1zu1.md`.

## Sicherheit
- Alle Dienst-Ports nur an `127.0.0.1` gebunden (nicht oeffentlich). Einziger offener Port nach
  aussen ist SSH; der externe Zugang zum Gehirn kommt spaeter ueber Reverse-Proxy (Caddy+TLS) oder VPN.
- brain-api laeuft als nicht-root-User, Bearer-Token-Auth auf allen Schreib-/Lese-Endpunkten.
- Secrets stehen in `.env` (NICHT im Repo, per globaler `.gitignore`-Regel `.env.*` ausgeschlossen —
  bewusst, damit nie ein echtes Secret committet wird). Backup der `.env` + SSH-Schluessel:
  `~/SK/second-brain/` auf Franks Rechner. Benoetigte Variablen (Vorlage, echte Werte nur lokal/Server):
  ```
  QDRANT_API_KEY=...        # langes Zufalls-Token (Such-Schrank)
  GEMINI_API_KEY=...        # Google Gemini, NUR fuer Embeddings (kein LLM mehr)
  GOOGLE_API_KEY=...        # = GEMINI_API_KEY (google-genai liest auch GOOGLE_API_KEY)
  SB_API_KEY=...            # REST-Bearer-Token: python -c "import secrets;print(secrets.token_urlsafe(48))"
  ```

## Zugang von außen (WireGuard-VPN)
Das Gehirn ist **öffentlich unsichtbar** — kein HTTP-Port ist im Internet offen. Der Zugriff
läuft ausschließlich über einen WireGuard-Tunnel (Split-Tunnel). Nur **UDP 51820** ist öffentlich.

- Server-Interface `wg0` = **10.8.0.1/24**. `brain-api` ist an `10.8.0.1:8000` gebunden → nur über
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
**Nie eine Client-Konfiguration auf ein zweites Gerät kopieren:** Jedes Gerät braucht zwingend ein
eigenes Schlüsselpaar und eine eigene `10.8.0.X`-Adresse. Zwei Geräte mit derselben Peer-Identität
lassen den Server-Endpoint fortlaufend umspringen; Antworten und SSE-Heartbeats landen dann am
falschen Gerät.

## Observability
brain-api schreibt strukturierte JSON-Lines nach `/app/logs/brain-api.jsonl` (Host: `./brain-logs/`,
rotierend) und spiegelt sie nach stdout. Live mitlesen auf dem Server:
```bash
docker compose logs -f brain-api                 # stdout-Strom
tail -f /opt/second-brain/brain-logs/brain-api.jsonl   # Logdatei
# nur Intent-Checkpoints (erwartet vs. tatsaechlich):
grep '"kind": "CHECKPOINT"' /opt/second-brain/brain-logs/brain-api.jsonl
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
  -d '{"text":"Frank trinkt morgens gerne gruenen Tee","title":"Lieblingsgetraenk","category":"persoenlich","user_id":"frank"}'
curl -s -X POST 10.8.0.1:8000/search -H "Authorization: Bearer $SB" \
  -H 'Content-Type: application/json' -d '{"query":"Was trinkt Frank?","user_id":"frank","limit":3}'
```

## Offene TODOs
- ✅ Qdrant auf `v1.18.2` gepinnt; WireGuard-Zugang + MCP-Endpunkt (sb-mcp) eingerichtet; mem0 raus -> brain-api 1:1.
- Harte Embedding-/Kosten-Caps in der Google AI Studio / Cloud Console setzen (Budget-Stopp, nicht nur Alert).
- **Offsite-Backup von `qdrant-data/`** einrichten + monatlicher Restore-Test (Flugplan Phase 1.1 — naechster Schritt).
- Eigener Gemini-Key fuers Gehirn (Kontingent vom geteilten TVO-Key trennen).
- RAG/Chunking-Ingest fuer Dokumente (z.B. die 3 Direktiven) + Bibliothekar-Agent (Schicht 3, liest nur) — Phase 4.
