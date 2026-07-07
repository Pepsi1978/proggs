# Qdrant (Vektordatenbank im selbst gehosteten Memory-Stack) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
