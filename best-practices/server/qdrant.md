# Qdrant (Vektordatenbank) — Best Practices (wie man es richtig betreibt)

> **Zweite Seite der Medaille zum Bug-Almanach** `~/proggs/bugs/server/qdrant.md`: dort *was schiefgeht*,
> hier *wie man Qdrant von vornherein richtig dimensioniert, absichert und tunt* — als Vektor-Store hinter
> mem0 im "zweiten Gehirn". Quellen: qdrant.tech-Docs + Recherche 2026-06-22.
> **Anker:** Qdrant 1.18.2 · Docker · im Stack mit mem0 2.0.7 @1536.
> **Changelog-Abgleich 2026-06-22 (2 unabh. Recherchen):** v1.18.2 ist weiterhin neueste Version (kein 1.19+). Sicherheits-Grund fuers Pinnen: 1.18.1/1.18.2 buendelt Security-Fixes (#9031 Snapshot-Upload-Auth, #8676 gRPC-Auth, #8628, #8619) — §3.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Speicher dimensionieren | Bei grossen Collections Vektoren UND HNSW `on_disk: true` (beides!); Bulk mit `hnsw m=0` | §1 |
| 2 | Collection anlegen | `size` == Embedding-Dim (1536); Distanz bewusst (Cosine pre-normalisiert) | §2 |
| 3 | Absichern | `QDRANT__SERVICE__API_KEY` + an 127.0.0.1/VPN-IP binden + Volume; Image-Version pinnen | §3 |
| 4 | Client anbinden | korrekter Port (REST 6333), ohne TLS explizite `http://`-URL | §4 |
| 5 | Tunen | Quantisierung (Scalar int8/TurboQuant 1.18); `hnsw_ef` zur Suche steuern; Memory-Monitoring | §5 |
| 6 | Hierarchische Kategorien (Haupt/Unter) filtern | Pfad-String `Haupt/Unter` fuer den Anzeige-Wert + **separates `parent`-Feld** (Keyword-Index) fuer „alles unter Haupt" (`MatchValue`) — KEIN Praefix-Operator in Qdrant. Index VOR Ingest. | §6 |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach
| Best-Practice (hier) | Bug-Abschnitt (`bugs/server/qdrant.md`) |
|----------------------|------------------------------------------|
| §1 Speicher | §1 RAM/OOM (HNSW im RAM) |
| §2 Collection | §2 Dimension/Cosine |
| §3 Betrieb | §3 Docker/Auth/Backup |
| §4 Client | §4 WRONG_VERSION_NUMBER |
| §5 Tuning | §5 + §6 (1.18-Features) |
| §6 Hierarchie filtern | §7 Filter/Hierarchie (kein startsWith) |

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
  WireGuard-Tunnel davor. Key-Rotation ohne Downtime: `alt_api_key` (v1.17.0). TLS optional direkt im Container:
  `QDRANT__SERVICE__ENABLE_TLS=true` + `QDRANT__TLS__CERT=./tls/cert.pem` (+ `TLS__KEY`) — ein TLS-terminierender
  Proxy ist damit nicht zwingend (im reinen WireGuard-Setup auch verzichtbar).
- **Persistenz:** Volume `:/qdrant/storage` auf SSD/NVMe (kein NFS/S3 — block-level POSIX noetig).
- **Image pinnen (Sicherheit, nicht nur Reproduzierbarkeit):** feste Version `qdrant/qdrant:v1.18.2`, NIE `:latest`
  (optional zusaetzlich Digest-Pin `@sha256:…`). Die 1.18.1/1.18.2-Linie buendelt mehrere Security-Fixes (Auth-vor-
  Snapshot-Upload #9031, gRPC-Auth #8676, Snapshot-Restore-von-URL abschaltbar #8628, TLS-Deps #8619). `:latest` kann
  beim Pull unbemerkt eine andere/aeltere Version ziehen. (Unser `compose.yaml` nutzt noch `:latest` → auf `v1.18.2`
  pinnen.) Hinweis: zwei einzelne PRs (#9254 Auth-Bypass / #9268 Snapshot-OOB) nannte nur eine von zwei Recherchen —
  vor Zitat in den Release-Notes pruefen; das Pinnen-Argument steht unabhaengig davon.
- **Backup:** Snapshots einrichten + Restore testen; beim Restore `Content-Type` NICHT explizit setzen
  (sonst extrem langsam, siehe Almanach §3c).

## §4 Client-Anbindung
REST=6333, gRPC=6334 nicht verwechseln. Ohne echtes TLS eine explizite `http://`-URL uebergeben
(`url="http://host:6333"`), sonst erzwingt ein gesetzter `api_key` `https=True` → `WRONG_VERSION_NUMBER`.
Das ist genau unser mem0-api-Setup (`QDRANT_URL=http://sb-qdrant:6333`). Im Python-Client `path=` NIE gegen
einen Server setzen (das ist der lokale Test-Modus).

## §5 Tuning
HNSW `m` (8–64) und `ef_construct` (100–500) bei Bedarf erhoehen (genauer, mehr RAM). Zur Suche `hnsw_ef`
senken = schneller. Quantisierung: Scalar int8 (−75 %), Binary (40× schneller), **TurboQuant** (Qdrant 1.18.0,
laut Release-Notes bis **8× Kompression ggue. float32** „without the recall tax" ≈ 2× ggue. Scalar int8). Das **Memory-Monitoring** (1.18, Web-UI + API) nutzen, um RAM/Disk pro Collection-Komponente
zu beobachten; `max_resident_memory_percent` (Strict-Mode-Guardrail) als Schutz. **Monitoring:** Qdrant
liefert Prometheus-/OpenMetrics-Metriken unter `GET /metrics` (mit `?per_collection=true` je Collection) —
fuer RAM/Disk-Alarme anbinden. **Payload-Index:** fuer gefilterte Suchen ein `field_index` auf die
Filter-Felder anlegen (`keyword`/`integer`/`bool` …), sonst wird langsam gescannt.

---

## §6 Hierarchische Kategorien (Haupt/Unter) richtig filtern (Stand 2026-06-25)
Qdrant hat **keinen** Praefix-/`startsWith`-Operator (Feature-Request #5300 offen). Fuer 2-Ebenen-Kategorien
daher das robuste Muster:
- **Anzeige-Wert:** ein Keyword-Feld `category = "Haupt/Unter"` (z.B. `Programmieren/Best-Practices`). Exakter
  Filter auf eine Unterkategorie = `MatchValue(category="Programmieren/Best-Practices")`, mehrere = `MatchAny`.
- **„Alles unter Haupt":** ein **zweites** Keyword-Feld `parent = "Haupt"` (= Teil vor dem `/`) mitschreiben →
  `MatchValue(parent="Programmieren")`. Indexgestuetzt, schnell, keine Substring-Tricks. (Alternative ohne
  Zusatzfeld: `MatchAny` ueber alle bekannten `Haupt/*`-Werte aus der App-Registry.)
- **Index VOR Ingest:** `parent` als `keyword`-`field_index` anlegen, bevor Daten geschrieben werden; auf
  Bestand nachgezogen → Reindex einplanen (sonst nutzt der filterable HNSW die Filter-Kanten nicht).
- **NICHT** `nested`-Objekte fuer simple Kategorien (Community-Index-Probleme #2256); flache Felder sind hier
  einfacher und schneller. Vollstaendige Fallen-Liste: `bugs/server/qdrant.md` §7.

## Quellen
qdrant.tech (resource-optimization, storage, security, HNSW, blog 1.18 TurboQuant, filtering, text-search, indexing),
GitHub qdrant-client + #5300/#4679/#2256 · Recherche 2026-06-22 (Firecrawl+MiniMax) + 2026-06-25 (OpenRouter `:online`).
