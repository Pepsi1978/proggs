# Bekannte Bugs & Fallen: Qdrant (Vektordatenbank im selbst gehosteten Memory-Stack)

> **PFLICHT-LESEN vor Arbeit an einer selbst gehosteten Qdrant-Instanz** (Docker, Collection-Config,
> Client-Anbindung, RAM-/Performance-Tuning) — z.B. als Vektor-Store hinter mem0 im "zweiten Gehirn".
> Loesungen funktionserhaltend (Direktive #3). **Zweite Seite der Medaille:**
> `best-practices/server/qdrant.md`.
>
> **Stand:** recherchiert am **2026-06-22** (Firecrawl + MiniMax M3, quellentreu; offizielle qdrant.tech-
> Docs/Blog + GitHub-Issues). **Changelog-Abgleich 2026-06-22:** v1.18.2 (4. Juni 2026) ist weiterhin die
> **neueste stabile Version** — kein 1.19+. **Ergaenzt 2026-07-02** (Tiefen-Debugging second-brain-server):
> NEU §9 Payload-Schema-Drift bei Einzelfeld→Array-Migration (Kurzcheck #14). **Anker:** Qdrant **1.18.2** (live: `curl http://127.0.0.1:6333/` → version
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
| 9 | ⭐ `:latest`-Image / Version < 1.18.2 | Auf **`v1.18.2` pinnen** — nicht nur Reproduzierbarkeit, sondern **Sicherheit**: die 1.18.1/1.18.2-Linie buendelt mehrere Security-Fixes (Auth-vor-Snapshot-Upload #9031, gRPC-Auth #8676, Snapshot-Restore-von-URL abschaltbar #8628, TLS-Deps #8619). `:latest` kann unbemerkt eine andere Version ziehen. | §3, §6 |
| 10 | ⭐ Hierarchie/„alles unter X" per **Praefix-Filter** gewollt | **KEIN nativer `startsWith`/Praefix-Operator** auf Keyword-Feldern (offener Feature-Request #5300). Loesungen: (a) separates `parent`-Payload-Feld (Keyword-Index) → exakter `MatchValue`; (b) `MatchAny` ueber die bekannten Unterpfade (App kennt die Liste); (c) `MatchText` + **Text-Index** (Substring, nicht fuer Hierarchie gedacht). | §7 |
| 11 | Payload-Index NACH dem Ingest angelegt | Der **filterable HNSW** zieht nur Filter-Kanten, wenn der Payload-Index **VOR** den Daten existiert. Nachtraeglich angelegt → HNSW muss neu gebaut werden. Neues Filterfeld (z.B. `parent`) auf Bestand → Index anlegen + Reindex einplanen. | §7 |
| 12 | `nested`-Filter auf Array-von-Objekten | `nested` ist noetig, damit mehrere Bedingungen **dasselbe** Array-Element treffen (sonst array-uebergreifend). Aber: Community berichtet **unzuverlaessige/langsamere Index-Nutzung** bei `nested` (#2256); „alle diese Werte gleichzeitig"-Subset-Match ist offener Feature-Request (#4679). Fuer einfache Kategorien lieber flache Felder. | §7 |
| 13 | ⭐ `client.scroll(..., with_payload=True)` ueber viele/grosse Payloads | Laedt ALLE Payload-Felder ALLER Punkte gleichzeitig in den **Client**-RAM (nicht Qdrant-RAM!). Bei grossen Feldern (z.B. Volltext 1:1 pro Chunk) + periodischem Aufruf → Client-Container-OOM-Loop. Fuer Zaehl-/Listen-Operationen NUR die noetigen Felder laden: `with_payload=["feld1","feld2"]` statt `True`. Container-Memory-Limit hochsetzen hilft NICHT (Last waechst unbeschraenkt). | §8 |
| 14 | Payload-Schema um ein ARRAY neben dem alten Einzelfeld erweitert (z.B. `categories` + `category`) | JEDER Verwaltungs-/Schreibweg muss BEIDE pflegen — ein `set_payload` nur aufs Einzelfeld laesst das Array stehen → der Lesepfad (bevorzugt das Array) zeigt weiter den ALTEN Wert; rename/detach wirken „gar nicht". Auch Export/Trash-Roundtrips muessen das Array mitnehmen. | §9 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt (hier) | Best-Practice (`best-practices/server/qdrant.md`) |
|----------------------|---------------------------------------------------|
| §1 RAM/OOM | §1 Speicher-Dimensionierung (on_disk + HNSW) |
| §2 Collection-Config | §2 Collection richtig anlegen |
| §3 Docker/Betrieb/Backup/Auth | §3 Betrieb + Sicherung |
| §4 Client/TLS | §4 Client-Anbindung |
| §5 Performance | §5 Tuning |
| §7 Filter/Hierarchie | §6 Hierarchische Kategorien filtern |
| §8 Client-scroll-OOM (with_payload) | §5 Tuning / §1 Speicher-Dimensionierung |

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
**3f `:latest`-Tag (Sicherheits-Relevanz, nicht nur Reproduzierbarkeit):** auf **`v1.18.2`** pinnen (optional zusaetzlich
Digest-Pin `@sha256:…`). Die 1.18.2-Release-Notes buendeln mehrere fuer einen exponierten Server relevante Security-/
Hardening-Fixes: **Authorize-before-Snapshot-Upload** (#9031), **API-Key/JWT-Enforcement auf internen gRPC-Endpoints**
(#8676), **Snapshot-Restore-von-URL abschaltbar** (#8628, Default-Haertung), **TLS-Dependencies gebumpt** (#8619).
⚠️ **Quellen-Diskrepanz (ehrlich):** Eine erste Recherche (Firecrawl) nannte zusaetzlich einen **REST-Auth-Whitelist-Bypass
(#9254)** + **Heap-OOB-Read via Snapshot (#9268)**; eine zweite, unabhaengige Recherche (OpenRouter) konnte genau diese
beiden PR-Nummern NICHT verifizieren (nennt stattdessen #9031/#8676/#8628/#8619). Vor einer harten Aussage zu #9254/#9268
direkt `github.com/qdrant/qdrant/releases` (v1.18.2) pruefen. Unabhaengig von den exakten PR-Nummern gilt: **`:latest` kann
beim naechsten Pull unbemerkt eine andere (ggf. aeltere/verwundbare) Version ziehen — auf v1.18.2 pinnen.**
**Quelle:** qdrant.tech (security, administration, docker), GitHub qdrant releases v1.18.2 (zwei unabhaengige Recherchen 2026-06-22), GitHub-Issue (Snapshot-Restore).

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
  40× schneller / 32× kleiner. **Qdrant 1.18.0: TurboQuant** (Google Research) — laut Release-Notes **bis 8× Kompression
  ggue. float32 „without the recall tax"** (≈ 2× ggue. Scalar int8, da int8 bereits 4× spart), mit SIMD; unterstuetzt Cosine/Dot/L2.
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

**Security-Fixes in der 1.18.x-Linie (Changelog-Abgleich 2026-06-22, ZWEI unabhaengige Recherchen):**
In BEIDEN Recherchen belegt (1.18.1/1.18.2-Linie) — fuer einen exponierten Server relevant:
- **Authorize-before-Snapshot-Upload** (#9031) — exakt der Pfad, den ein Docker-Container exponiert.
- **API-Key/JWT-Enforcement auf internen gRPC-Endpoints** (#8676).
- **Snapshot-Restore-von-URL per Config abschaltbar** (#8628, Default-Haertung).
- **TLS-Dependencies gebumpt** (#8619), Leervektor-Panic-Fix (#9070), TurboQuant-Heap-Under-Reporting (#9099).
- Betriebs-relevant: `on_disk`-Flag-Wechsel ohne Payload-Index-Rebuild (#9138), Timeout fuer Shard-Snapshot-Streaming (#9239).
⚠️ **Nur in EINER Recherche (Firecrawl), in der zweiten NICHT verifiziert:** REST-Auth-Whitelist-Bypass (#9254) +
Heap-OOB-Read via Snapshot (#9268). Vor harter Aussage `github.com/qdrant/qdrant/releases` (v1.18.2) pruefen.
→ Konsequenz (robust, quellenunabhaengig): **`v1.18.2` ist die Mindest-Version zum Pinnen** (mehrere Security-Fixes
gebuendelt). Nach v1.18.2 existiert (Stand 2026-06-22) noch kein Release — Anker bleibt aktuell.

## 7. Filter, Hierarchische Kategorien & Metadaten (recherchiert 2026-06-25)
**Symptom/Frage:** Man will Unterkategorien (`Programmieren/Best-Practices`) modellieren und „alles unter
Programmieren" filtern — und sucht einen Praefix-Filter.
**Fakten (offizielle Doku + GitHub):**
- ❌ **Kein nativer Praefix/`startsWith`-Operator** auf Keyword-Feldern. Nur Feature-Request
  [#5300](https://github.com/qdrant/qdrant/issues/5300) (offen). Ein Pfad-String allein laesst sich also NICHT
  effizient praefix-filtern.
- ✅ **Exakte Werte:** `keyword`-Index + `MatchValue` (ein Wert) / `MatchAny` (Wertliste). Schnell, indexgestuetzt.
- ✅ **„Alles unter X" – die zwei sauberen Wege:**
  (a) **Separates Feld** `parent`/`category_main` (Keyword-Index) → exakter `MatchValue(parent="Programmieren")`.
  Klarste, schnellste Loesung. (b) `MatchAny` ueber die bekannten Unterpfade — geht, wenn die App die
  Kategorienliste kennt (z.B. aus einer Registry).
- ⚠️ **Substring:** `MatchText` braucht einen **Text-Index** (nicht denselben wie keyword); fuer Hierarchie-Traversal
  nur Notloesung, nicht als Best Practice ausgewiesen.
- ⚠️ **`nested`-Objekte** (Array von `{parent,child}`): noetig fuer „dasselbe Array-Element", aber Community
  meldet unzuverlaessige Index-Nutzung ([#2256](https://github.com/orgs/qdrant/discussions/2256)); Subset-Match
  („alle diese gleichzeitig") ist offen ([#4679](https://github.com/qdrant/qdrant/issues/4679)). Fuer einfache
  Kategorien daher **flache Felder + separates parent-Feld** bevorzugen.
- ⭐ **Index-Reihenfolge:** Payload-Index **VOR** dem Ingest anlegen, sonst muss der filterable HNSW neu gebaut
  werden, um Filter-Kanten zu erhalten. Neues Filterfeld auf Bestand → Index anlegen + Reindex einplanen.
**Versionen:** Qdrant 1.18.2, qdrant-client>=1.18 (strukturell, alle 1.x).
**Quelle:** qdrant.tech (filtering, text-search, indexing, vector-search-filtering), GitHub #5300/#4679/#2256,
StackOverflow (nested) · OpenRouter-`:online`-Recherche 2026-06-25.

---

## 8. ⭐ Client-seitiger OOM: `scroll(with_payload=True)` zieht ALLE Payloads in den RAM (live 2026-06-26)
**Symptom:** Der **Client**-Container (FastAPI/brain-api), nicht Qdrant, geht in einen OOM-Neustart-Loop —
hier alle ~20s (`docker events`: `oom → die → start`; Kernel: `cgroup out of memory: Killed process
(uvicorn) anon-rss ~1 GB+`). Nach aussen: „0 Eintraege" / „Server momentan nicht erreichbar", weil der
Dienst staendig kurz weg ist. Tritt erst auf, wenn die Daten **gross** werden (hier: 60-70 grosse Almanache).
**Ursache:** `qdrant_client.scroll(collection, scroll_filter=…, limit=N, with_payload=True)` laedt **ALLE
Payload-Felder ALLER N Punkte gleichzeitig** in den Client-RAM. Liegt ein grosses Feld redundant in jedem
Punkt (hier: `full_text` 1:1 in JEDEM Chunk — ein 600k-Doc = ~150 Chunks × 600k), summiert sich das auf
Gigabytes — obwohl die Operation nur ein paar Metadaten braucht (z.B. Kategorien zaehlen, Titel listen).
Verschaerfend: ein **periodischer** Aufruf (Dashboard-Übersicht pollte `/api/overview` → `/list` alle 20s)
macht daraus einen Dauer-Loop. **Container-Memory-Limit hochsetzen hilft NICHT** (live getestet: 1G→2G,
Loop lief weiter — die geladene Datenmenge waechst unbeschraenkt mit dem Bestand).
**Versionen:** strukturell (qdrant-client alle 1.x; analog `query_points`/`retrieve` mit `with_payload=True`).
**FIX (funktionserhaltend):** Fuer Zaehl-/Listen-Operationen NUR die benoetigten Felder laden —
`with_payload` nimmt eine **Feldliste** statt `True`:
```python
# vorher: laedt full_text aller Chunks -> RAM-Explosion
points, _ = qc.scroll(collection_name=COL, scroll_filter=flt, limit=10000, with_payload=True)
# nachher: nur die Zaehl-/Listen-Felder -> winziger RAM
points, _ = qc.scroll(collection_name=COL, scroll_filter=flt, limit=10000,
                      with_payload=["doc_id", "title", "category"])
```
Endpoints, die den Volltext WIRKLICH anzeigen (Einzel-Abruf/Drawer/Suche), laden ihn weiterhin gezielt.
Brauchst du eine abgeleitete Groesse (z.B. Zeichenzahl) in einer Liste: ein kleines `text_len`-Payload-Feld
beim Schreiben mitspeichern, statt den Volltext zum Zaehlen zu laden. **Belegt:** second-brain brain-api
1.13.1/1.13.2 (`category-counts` + `/list` → Metadaten-only); RAM-Spitze von >2 GB auf ~240 MB, Loop weg.

**Zwei verwandte Fallen derselben Klasse (live 2026-06-26, brain-api 1.14.0):**
- **Auch der EINZEL-Abruf eines grossen Docs sprengt den RAM.** Ein Endpoint, der EIN Dokument liefert
  (`by-title`), aber ALLE seine Chunks laedt (`limit=1000`), zieht bei einem 1,4-Mio-Zeichen-Doc N Kopien
  des `full_text` (jeder Chunk traegt ihn 1:1) → OOM **beim Abruf** (Doc liess sich nicht abrufen, Container
  stuerzte ab). Fix: **`limit=1`** — ein Chunk genuegt, weil `full_text` in jedem Chunk identisch ist.
  Gilt analog fuer Listen-Endpoints, die je Doc den Volltext zeigen: nur **Chunk 0** filtern
  (`FieldCondition(key="chunk_index", match=MatchValue(value=0))`) statt alle Chunks.
- **`scroll` ohne Paginierung schneidet still ab.** `qc.scroll(..., limit=N)` liefert nur die ERSTE Seite
  (bis N) und ein `next_page_offset` — wer das offset ignoriert, bekommt bei >N Punkten **unbemerkt
  unvollstaendige** Ergebnisse. Fuer „alles laden" eine Schleife: `while: pts,off = scroll(..., offset=off);
  if off is None: break`. (Tipp: eine **abgeleitete Groesse** wie Zeichenzahl als kleines Payload-Feld
  `text_len` beim Schreiben mitspeichern — dann muss man sie nie durch Laden des Volltexts berechnen.)

## Pflicht-Checkliste vor Qdrant-Betrieb
- [ ] Volume `:/qdrant/storage` gemountet, SSD/NVMe (kein NFS/S3)?
- [ ] `QDRANT__SERVICE__API_KEY` gesetzt + an 127.0.0.1/VPN-IP gebunden (nicht offen)?
- [ ] Image auf feste Version gepinnt (1.18.2), nicht `:latest`?
- [ ] Collection: `size` == Embedding-Dim (1536)? Distanz bewusst (Cosine pre-normalisiert)?
- [ ] Bei grossem Gehirn: `vectors.on_disk` UND `hnsw_config.on_disk`? Quantisierung erwogen?
- [ ] Client: korrekter Port (REST 6333) + `http://`-URL ohne echtes TLS (gegen WRONG_VERSION_NUMBER)?
- [ ] Snapshot-Backup eingerichtet, Restore getestet (Content-Type NICHT explizit)?
- [ ] Memory-Monitoring (1.18) im Blick, `max_resident_memory_percent` als Guardrail?

## 9. Payload-Schema-Drift: Einzelfeld→Array-Migration ohne Nachzug ALLER Schreibwege
**Symptom:** Kategorie umbenennen/loeschen „wirkt nicht": die Uebersicht zaehlt weiter unter dem alten
Namen, `by-category` findet den Eintrag unter altem UND neuem Namen; nach einem Papierkorb-Restore
fehlen sekundaere Kategorien.
**Ursache:** Ein Payload-Schema wurde abwaertskompatibel um ein Array erweitert (`categories` neben
`category`, `parents` neben `parent`), und der LESE-Pfad bevorzugt das Array. Verwaltungs-Operationen
(`set_payload` bei rename/detach) schrieben aber nur das ALTE Einzelfeld um — das Array (und die
abgeleiteten Felder) blieben stehen. Zusaetzlich matchten die Filter nur das Einzelfeld → Eintraege,
die den Wert NUR im Array trugen, wurden gar nicht erst gefunden. Gleiches Muster beim
Export/Roundtrip (Trash speicherte nur das Einzelfeld → Restore verlor die Array-Werte).
**Versionen:** strukturell (App-Logik, jede Qdrant-Version).
**Betrifft unseren Code:** `brain-api/app.py` rename-category/detach-category/delete_entry(Trash) —
Multi-Category-Regression seit 1.11.0, GEFIXT 2026-07-02 (1.19.0, Tiefen-Debugging): Filter matchen
Einzelfeld ODER Array (`should`), `set_payload` schreibt das komplette Feld-Set (`category`,
`categories`, `parent`, `parents`) je Punkt neu (gebuendelt je identischem Ziel-Payload), Trash
speichert `categories` mit.
**FIX (funktionserhaltend, verallgemeinert):** Bei JEDER Einzelfeld→Array-Migration eine Checkliste
ueber ALLE Schreib-/Verwaltungs-/Export-Wege ziehen (store/update/rename/detach/move/trash/restore/
backfill) — jeder Weg muss das VOLLE abgeleitete Feld-Set schreiben; jeder Filter muss beide Formen
matchen (`should`). Lesepfad-Praeferenz (Array vor Einzelfeld) dokumentieren.
**Quelle:** Tiefen-Debugging second-brain-server 2026-07-02 (statischer Fund).

---

## Quellen (Stand 2026-06-22)
Firecrawl + MiniMax M3 (quellentreu): qdrant.tech (resource-optimization, storage, security, administration, HNSW,
collections, blog 1.18 TurboQuant), GitHub qdrant-client #770 (WRONG_VERSION_NUMBER), GitHub-Diskussionen (Snapshot-
Restore, Performance). mem0-spezifische Qdrant-Fallen waren in den Quellen nicht enthalten (→ `mem0.md`).
