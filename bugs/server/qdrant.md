# Bekannte Bugs & Fallen: Qdrant (Vektordatenbank im selbst gehosteten Memory-Stack)

> **PFLICHT-LESEN vor Arbeit an einer selbst gehosteten Qdrant-Instanz** (Docker, Collection-Config,
> Client-Anbindung, RAM-/Performance-Tuning) — z.B. als Vektor-Store hinter mem0 im "zweiten Gehirn".
> Loesungen funktionserhaltend (Direktive #3). **Zweite Seite der Medaille:**
> `best-practices/server/qdrant.md`.
>
> **Stand:** recherchiert am **2026-06-22** (Firecrawl + MiniMax M3, quellentreu; offizielle qdrant.tech-
> Docs/Blog + GitHub-Issues). **Anker:** Qdrant **1.18.2** (live: `curl http://127.0.0.1:6333/` → version
> 1.18.2), Docker-Image `qdrant/qdrant:latest`, im Stack mit mem0 2.0.7 (Embeddings @1536).
> Verwandt: [`self-hosted-ai-agent-server.md`](self-hosted-ai-agent-server.md), [`mem0.md`](mem0.md), [`wireguard.md`](wireguard.md).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ OOM trotz `vectors.on_disk=true` | Der **HNSW-Graph bleibt standardmaessig im RAM** — `hnsw_config.on_disk: true` MUSS separat gesetzt werden. Beim Bulk-Upload `hnsw_config.m=0`, danach reaktivieren. | §1 |
| 2 | ⭐ `WRONG_VERSION_NUMBER` / SSL-Handshake-Fehler | Port-/Protokoll-Mismatch: Client spricht TLS/falschen Port gegen Klartext-HTTP. **REST=6333, gRPC=6334.** Bei `api_key` ohne TLS → explizite `http://`-URL nutzen (genau unser mem0-api-Fix). | §4 |
| 3 | ⭐ Daten weg nach Container-Restart | Volume `-v …:/qdrant/storage` Pflicht. **NFS/S3 funktionieren NICHT** (Qdrant braucht block-level POSIX, SSD/NVMe). | §3 |
| 4 | ⭐ Qdrant ist offen erreichbar | Default: KEINE Auth, alle Interfaces. `QDRANT__SERVICE__API_KEY` setzen + an `127.0.0.1`/VPN-IP binden + TLS/Tunnel. | §3 |
| 5 | Dimension-Mismatch beim Upsert | `VectorParams(size=…)` MUSS == Embedding-Dim (z.B. 1536). Sonst `could not broadcast … into shape`. | §2 |
| 6 | Original-Vektoren weichen vom Upload ab | `Cosine` **pre-normalisiert** die Vektoren. Wer die Originale braucht → `Dot`. Metrik-Wechsel aendert Rankings (ausser unit-normalisiert). | §2 |
| 7 | Snapshot-Restore extrem langsam (Minuten statt Sek.) | `Content-Type: multipart/form-data` NICHT explizit setzen (zerstoert die boundary-Berechnung) — `requests`/Client selbst generieren lassen. | §3 |
| 8 | Suche langsam | `hnsw_ef` zur Suche senken (schneller, weniger Recall); Quantisierung (Scalar int8 −75%, Binary 40× schneller); 1.18 TurboQuant. | §5 |
| 9 | `:latest`-Image im Compose | Auf feste Version (1.18.2) pinnen — reproduzierbar, kein stilles Major-Upgrade beim Pull. | §3 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt (hier) | Best-Practice (`best-practices/server/qdrant.md`) |
|----------------------|---------------------------------------------------|
| §1 RAM/OOM | §1 Speicher-Dimensionierung (on_disk + HNSW) |
| §2 Collection-Config | §2 Collection richtig anlegen |
| §3 Docker/Betrieb/Backup/Auth | §3 Betrieb + Sicherung |
| §4 Client/TLS | §4 Client-Anbindung |
| §5 Performance | §5 Tuning |

---

## 1. ⭐ RAM/OOM — der HNSW-Graph bleibt im RAM (haeufigster Stolperstein)
**Symptom:** OOM-Crash bei wachsender Collection, OBWOHL `vectors.on_disk: true` gesetzt ist.
**Ursache:** `on_disk` fuer Vektoren laagert NUR die Roh-Vektoren aus — der **HNSW-Index-Graph bleibt
standardmaessig komplett im RAM**. 1 Mio Vektoren à 1536 dim (float32) ≈ 6 GB Rohdaten, mit Index-Overhead
deutlich mehr. Bei Bulk-Uploads zusaetzlich: Vektoren liegen erst im RAM, bis der Optimizer sie auf Disk schiebt.
**Versionen:** strukturell (alle 1.x). `on_disk` fuer Vektoren ab v1.2.0.
**FIX (funktionserhaltend):**
- Vektoren UND Index auf Disk legen — beides separat:
  ```http
  PUT /collections/{name}
  { "vectors": { "size": 1536, "distance": "Cosine", "on_disk": true },
    "hnsw_config": { "on_disk": true } }
  ```
  („enabling on_disk for vectors does not automatically store their indexes on disk".)
- **Bulk-Ingestion:** HNSW deaktivieren (`hnsw_config.m = 0`) waehrend des Massen-Imports → vorhersehbarer
  RAM-Verbrauch, danach `m` zurueck auf 16/32 und Optimizer einmal laufen lassen.
- **Quantisierung** (Scalar int8) spart 75 % RAM; quantisierte Vektoren bleiben (klein) im RAM, Originale `on_disk`.
- `memmap_threshold` == `indexing_threshold` (Default 10.000) fuer ausgewogene Faelle; bei wenig RAM/viel Write niedriger.
- **Disk-Speed ist ein versteckter Faktor:** lokale SSD/NVMe ist ~10× schneller als Netzwerk-Storage (RPS!).
- **Unser Stack:** Qdrant ist im Compose auf `memory: 2G` gedeckelt; bei wachsendem Gehirn `on_disk` (Vektoren+HNSW)
  + Scalar-Quantisierung vorsehen und das **Memory-Monitoring (1.18, §5)** beobachten.
**Quelle:** qdrant.tech (resource-optimization, storage), GitHub-Diskussionen · Recherche 2026-06-22.

## 2. Collection-Config-Fallen
**2a Dimension-Mismatch:** `VectorParams(size=N)` MUSS exakt der Embedding-Dimension entsprechen. Sonst beim
Upsert `ValueError: could not broadcast input array from shape (1536,) into shape (2000,)`. Dimension wird bei
Collection-Erstellung fixiert und gilt fuer alle Punkte. Max 65.535 dims. **Unser Stack: 1536 (gemini-embedding-001).**
**2b Cosine pre-normalisiert:** Bei `distance: Cosine` normalisiert Qdrant die Vektoren VOR dem Speichern
(schnellere Distanzberechnung). Wer die **Original-Vektoren** unveraendert zurueck braucht → `Dot` verwenden.
Metrik-Wechsel aendert Rankings, ausser die Vektoren sind bereits unit-normalisiert.
**2c HNSW-Index noch im Aufbau:** Frisch migrierte/grosse Collections fallen temporaer auf Brute-Force zurueck.
`indexed_vectors_count` vs `points_count` pruefen; Collection-Status `green` abwarten, bevor Recall gemessen wird.
**2d Upsert ohne Dedup:** Qdrant prueft VOR einem Upsert KEINE Aehnlichkeit — identische Re-Upserts markieren die
alte Version als geloescht und legen eine neue Kopie an (Soft-Delete-Bitmask, kein Index-Rebuild). Dedup ist Sache
der App (bei uns mem0).
**2e Multitenancy:** Lieber EINE Collection mit Payload-Partitionierung als viele kleine (Ressourcen).
**Quelle:** qdrant.tech (collections, optimization), GitHub-Issue (Dimension-Mismatch) · Recherche 2026-06-22.

## 3. Docker-Betrieb, Persistenz, Backup, Auth
**3a Persistenz:** Ohne Volume gehen Daten bei Container-Restart/Recreate verloren. `-v ./qdrant-data:/qdrant/storage`.
**NFS und S3/Object-Storage funktionieren NICHT** — Qdrant braucht block-level Zugriff auf ein POSIX-Filesystem
(SSD/NVMe empfohlen). **Unser Stack hat `./qdrant-data:/qdrant/storage` ✓.**
**3b Ports:** 6333 (HTTP/REST), 6334 (gRPC), 6335 (distributed). Im Compose nur die noetigen exponieren, an `127.0.0.1`.
**3c Snapshot-Restore-Bug:** Restore/Upload von Snapshots ist langsam — und ein API-Client-Bug verschlimmert es:
wird der Header `Content-Type: multipart/form-data` **explizit** gesetzt, berechnet `requests` die `boundary` nicht
selbst → Restore dauert Minuten statt Sekunden (real: 20 min mit TLS vs 15 sec ohne). **FIX:** `Content-Type` NICHT
explizit setzen, vom Client generieren lassen (`files={'snapshot': open(...)}`, nur `api-key`-Header).
**3d Auth (Default offen!):** Selbst gehostetes Qdrant ist standardmaessig OHNE Auth an allen Interfaces erreichbar.
`QDRANT__SERVICE__API_KEY=<secret>` setzen, an `127.0.0.1`/VPN-IP binden, TLS oder VPN-Tunnel davor. Key-Rotation
ohne Downtime: `alt_api_key` (ab v1.17.0) + Rolling Restart. Key-Typen: Admin / Read-Only (v1.7.0) / JWT-RBAC (v1.9.0).
**3e Local vs Server Mode (Python-Client):** `QdrantClient(path=…)` schaltet in den Local Mode (Single-Instance, kein
concurrent access, nur Tests). Gegen einen Docker-Server `path` NIE setzen — `url`/`host` verwenden.
**3f `:latest`-Tag:** auf feste Version (1.18.2) pinnen → reproduzierbar, kein ueberraschendes Major beim naechsten Pull.
**Quelle:** qdrant.tech (security, administration, docker), GitHub-Issue (Snapshot-Restore) · Recherche 2026-06-22.

## 4. Client-Anbindung & `WRONG_VERSION_NUMBER`
**Symptom:** `SSL routines … WRONG_VERSION_NUMBER` beim Verbinden.
**Ursache:** Port-/Protokoll-Mismatch — der Client spricht TLS bzw. den falschen Port gegen einen Klartext-HTTP-
Endpunkt (oder REST-Port 6333 vs gRPC-Port 6334 verwechselt). **Genau unser eigener Fall:** der qdrant-client nimmt
bei gesetztem `api_key` `https=True` an und spricht TLS gegen das Klartext-HTTP-Qdrant → WRONG_VERSION_NUMBER.
**FIX:** Korrekten Port + Protokoll verwenden — REST=6333, gRPC=6334. Ohne echtes TLS eine **explizite `http://`-URL**
uebergeben (`url="http://host:6333"` statt `host=`+`api_key`). Das ist exakt der Fix in unserem `mem0-api/app.py`
(`QDRANT_URL = http://sb-qdrant:6333`). Echtes TLS nur mit Server-Optionen `enable_tls`/`tls.cert`/`tls.key`/`tls.ca_cert`.
**Versionen:** qdrant-client 1.10–1.15+ betroffen (Issue #770).
**Quelle:** GitHub qdrant-client #770, qdrant.tech/security · Recherche 2026-06-22 + eigener Vorfall.

## 5. Performance-Tuning
- **HNSW `m`** (8–64): hoeher = genauer, mehr RAM/Build-Zeit. **`ef_construct`** (100–500): hoeher = genauerer Build,
  langsamer. Zur **Suche** `hnsw_ef` senken = schneller (weniger Recall).
- **Quantisierung:** Scalar int8 = −75 % Speicher, <1 % Fehler, `always_ram` haelt sie im RAM (schnell). Binary = bis
  40× schneller / 32× kleiner. **Qdrant 1.18: TurboQuant** (Google Research) — ~doppelte Kompression ggue. Scalar bei
  aehnlichem Recall, mit SIMD; unterstuetzt Cosine/Dot/L2.
- `full_scan_threshold`/`indexing_threshold`: kleine Segmente werden per Brute-Force durchsucht, bis Indizierung lohnt.
**Quelle:** qdrant.tech (resource-optimization, HNSW, blog 1.18) · Recherche 2026-06-22.

## 6. Was 1.18 Neues bringt (nutzbar) + Fix-Status
**Neue 1.18-Features (qdrant.tech/blog/qdrant-1.18.x):** TurboQuant (s.o.) · **Memory-Monitoring** (Web-UI + API-Endpoint:
Disk-/RAM-/Page-Cache-Nutzung pro Collection-Komponente — direkt nuetzlich gegen §1-OOM) · Named Vectors zur Laufzeit
hinzufuegen/entfernen ohne Recreate · Audit-Logging (Query-API + Request-Tracing-IDs `x-request-id`/`traceparent`) ·
Per-Collection-Metriken (`/metrics?per_collection=true`) · **Strict-Mode-Guardrails** (u.a. `max_resident_memory_percent`).
**Fix-Status (ehrlich):** Die gefundenen Punkte sind ueberwiegend **Betriebs-/Konfigurations-Fallen** (kein versions-
abhaengiger Code-Bug mit „gefixt ab"). Der `WRONG_VERSION_NUMBER`-Fall (#770) ist ein Port/Protokoll-Konfig-Thema, kein
Server-Bug. Versions-spezifische 1.18-„pitfalls/breaking changes" waren in den Quellen NICHT belegt (mehrere Quellen-
Auszuege endeten abgeschnitten) — daher hier KEINE 1.18-Breaking-Changes behauptet.

---

## Pflicht-Checkliste vor Qdrant-Betrieb
- [ ] Volume `:/qdrant/storage` gemountet, SSD/NVMe (kein NFS/S3)?
- [ ] `QDRANT__SERVICE__API_KEY` gesetzt + an 127.0.0.1/VPN-IP gebunden (nicht offen)?
- [ ] Image auf feste Version gepinnt (1.18.2), nicht `:latest`?
- [ ] Collection: `size` == Embedding-Dim (1536)? Distanz bewusst (Cosine pre-normalisiert)?
- [ ] Bei grossem Gehirn: `vectors.on_disk` UND `hnsw_config.on_disk`? Quantisierung erwogen?
- [ ] Client: korrekter Port (REST 6333) + `http://`-URL ohne echtes TLS (gegen WRONG_VERSION_NUMBER)?
- [ ] Snapshot-Backup eingerichtet, Restore getestet (Content-Type NICHT explizit)?
- [ ] Memory-Monitoring (1.18) im Blick, `max_resident_memory_percent` als Guardrail?

---

## Quellen (Stand 2026-06-22)
Firecrawl + MiniMax M3 (quellentreu): qdrant.tech (resource-optimization, storage, security, administration, HNSW,
collections, blog 1.18 TurboQuant), GitHub qdrant-client #770 (WRONG_VERSION_NUMBER), GitHub-Diskussionen (Snapshot-
Restore, Performance). mem0-spezifische Qdrant-Fallen waren in den Quellen nicht enthalten (→ `mem0.md`).
