# Second-Brain-Server — Deployment-Konfiguration

Docker-Compose-Stack fuer Franks selbst gehostetes "zweites Gehirn" auf dem Hostinger VPS.
Strategie/Gesamtplan: `best-practices/second-brain/UMSETZUNGSPLAN.md`.

## Warum diese Dateien im Repo liegen
Portabilitaets-Versicherung fuer den geplanten Umzug (~2028): Der komplette Stack ist hier als
Code/Config gesichert und damit auf jedem Linux-Server 1:1 reproduzierbar. Beim Umzug genuegt:
neuen Server mieten -> diese Configs hochladen -> `docker compose up -d` -> Daten-Volume kopieren.

## Bausteine (Stand laufend)
| Dienst | Rolle | Port (nur 127.0.0.1) |
|--------|-------|----------------------|
| Qdrant | Vektor-DB / "Such-Schrank" (semantische + gefilterte Suche) | 6333 (REST), 6334 (gRPC) |

(Mem0 = "Bibliothekar" folgt als naechster Baustein.)

## Sicherheit
- Alle Dienst-Ports nur an `127.0.0.1` gebunden (nicht oeffentlich). Einziger offener Port nach
  aussen ist SSH; der externe Zugang zum Gehirn kommt spaeter ueber Reverse-Proxy (Caddy+TLS) oder VPN.
- Secrets stehen in `.env` (NICHT im Repo, per `.gitignore` ausgeschlossen). Backup der `.env` und
  des SSH-Schluessels: `~/SK/second-brain/` auf Franks Rechner.

## Deployment (auf dem Server, Verzeichnis /opt/second-brain)
```bash
docker compose up -d        # starten
docker compose ps           # Status
docker compose logs -f      # Logs
docker compose down         # stoppen
```

## Offene TODOs
- Qdrant-Image nach erstem Lauf auf feste Version pinnen (statt `latest`).
- Mem0 ergaenzen (Bibliothekar) inkl. Embedding-/LLM-Entscheidung.
- Externen Zugang (Caddy+TLS+Domain ODER WireGuard) einrichten.
- Harte LLM-Kosten-Caps setzen, sobald die LLM-API eingebunden ist.
- Offsite-Backup von `qdrant-data/` einrichten.
