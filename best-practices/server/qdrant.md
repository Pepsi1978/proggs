# Qdrant (Vektordatenbank) — Best Practices (wie man es richtig betreibt)

> **Zweite Seite der Medaille zum Bug-Almanach** `~/proggs/bugs/server/qdrant.md`: dort *was schiefgeht*,
> hier *wie man Qdrant von vornherein richtig dimensioniert, absichert und tunt* — als Vektor-Store hinter
> mem0 im "zweiten Gehirn". Quellen: qdrant.tech-Docs + Recherche 2026-06-22.
> **Anker:** Qdrant 1.18.2 · Docker · im Stack mit mem0 2.0.7 @1536.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Speicher dimensionieren | Bei grossen Collections Vektoren UND HNSW `on_disk: true` (beides!); Bulk mit `hnsw m=0` | §1 |
| 2 | Collection anlegen | `size` == Embedding-Dim (1536); Distanz bewusst (Cosine pre-normalisiert) | §2 |
| 3 | Absichern | `QDRANT__SERVICE__API_KEY` + an 127.0.0.1/VPN-IP binden + Volume; Image-Version pinnen | §3 |
| 4 | Client anbinden | korrekter Port (REST 6333), ohne TLS explizite `http://`-URL | §4 |
| 5 | Tunen | Quantisierung (Scalar int8/TurboQuant 1.18); `hnsw_ef` zur Suche steuern; Memory-Monitoring | §5 |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach
| Best-Practice (hier) | Bug-Abschnitt (`bugs/server/qdrant.md`) |
|----------------------|------------------------------------------|
| §1 Speicher | §1 RAM/OOM (HNSW im RAM) |
| §2 Collection | §2 Dimension/Cosine |
| §3 Betrieb | §3 Docker/Auth/Backup |
| §4 Client | §4 WRONG_VERSION_NUMBER |
| §5 Tuning | §5 + §6 (1.18-Features) |

---

## §1 Speicher richtig dimensionieren
Kleines Gehirn passt in den RAM (schnellste Variante). Sobald es waechst: Vektoren UND HNSW-Index auf Disk
legen (beides separat — `on_disk` fuer Vektoren laagert den Index NICHT mit aus):
```json
{ "vectors": { "size": 1536, "distance": "Cosine", "on_disk": true },
  "hnsw_config": { "on_disk": true } }
```
Bulk-Import: `hnsw_config.m=0` waehrend des Imports (vorhersehbarer RAM), danach `m=16/32` + Optimizer.
Scalar-Quantisierung (int8) spart 75 % RAM. Lokale SSD/NVMe (kein Netzwerk-Storage → 10× RPS). Unser
Compose deckelt Qdrant auf 2 GB — bei Wachstum on_disk + Quantisierung aktivieren.

## §2 Collection sauber anlegen
`size` exakt = Embedding-Dimension des Modells (gemini-embedding-001 → 1536). Distanz bewusst: `Cosine`
**normalisiert die Vektoren vor dem Speichern** (Original geht verloren) — wer Originale braucht, nimmt
`Dot`. Fuer Multi-User EINE Collection mit Payload-Partitionierung statt vieler Collections. Nach grossem
Insert auf Status `green` warten (sonst Brute-Force-Fallback).

## §3 Betrieb, Sicherheit, Backup
- **Auth Pflicht:** `QDRANT__SERVICE__API_KEY=<secret>` (Default ist OFFEN!). An `127.0.0.1`/VPN-IP binden,
  TLS oder WireGuard-Tunnel davor. Key-Rotation ohne Downtime: `alt_api_key` (v1.17.0).
- **Persistenz:** Volume `:/qdrant/storage` auf SSD/NVMe (kein NFS/S3 — block-level POSIX noetig).
- **Image pinnen:** feste Version (`qdrant/qdrant:1.18.2`), nicht `:latest`.
- **Backup:** Snapshots einrichten + Restore testen; beim Restore `Content-Type` NICHT explizit setzen
  (sonst extrem langsam, siehe Almanach §3c).

## §4 Client-Anbindung
REST=6333, gRPC=6334 nicht verwechseln. Ohne echtes TLS eine explizite `http://`-URL uebergeben
(`url="http://host:6333"`), sonst erzwingt ein gesetzter `api_key` `https=True` → `WRONG_VERSION_NUMBER`.
Das ist genau unser mem0-api-Setup (`QDRANT_URL=http://sb-qdrant:6333`). Im Python-Client `path=` NIE gegen
einen Server setzen (das ist der lokale Test-Modus).

## §5 Tuning
HNSW `m` (8–64) und `ef_construct` (100–500) bei Bedarf erhoehen (genauer, mehr RAM). Zur Suche `hnsw_ef`
senken = schneller. Quantisierung: Scalar int8 (−75 %), Binary (40× schneller), **TurboQuant** (Qdrant 1.18,
~2× Kompression). Das **Memory-Monitoring** (1.18, Web-UI + API) nutzen, um RAM/Disk pro Collection-Komponente
zu beobachten; `max_resident_memory_percent` (Strict-Mode-Guardrail) als Schutz. **Monitoring:** Qdrant
liefert Prometheus-/OpenMetrics-Metriken unter `GET /metrics` (mit `?per_collection=true` je Collection) —
fuer RAM/Disk-Alarme anbinden. **Payload-Index:** fuer gefilterte Suchen ein `field_index` auf die
Filter-Felder anlegen (`keyword`/`integer`/`bool` …), sonst wird langsam gescannt.

---

## Quellen
qdrant.tech (resource-optimization, storage, security, HNSW, blog 1.18 TurboQuant), GitHub qdrant-client · Recherche 2026-06-22 (Firecrawl+MiniMax).
