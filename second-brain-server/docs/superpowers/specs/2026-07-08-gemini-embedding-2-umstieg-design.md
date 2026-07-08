# Design: Umstieg des Cortex-Speichers auf Gemini Embedding 2 (Text, 3072 Dim)

> ⛔ **ARCHIVIERT / ABGESCHLOSSEN (Stand 08.07.2026, 20.57 Uhr).** Der Umstieg ist vollzogen und
> `gemini-embedding-001` wurde am 08.07.2026 VOLLSTAENDIG entfernt — Code, Dashboard-Rueckschalter und
> die alten 1536-Qdrant-Collections. Der hier beschriebene `-001`-Fallback-Schalter existiert im
> Live-System NICHT mehr. Dieses Dokument bleibt rein historisch als Design-/Wiederherstellungsreferenz.

> **Stand:** 08.07.2026, 18:24 (Europe/Berlin) · **Projekt:** `second-brain-server` (Cortex, "zweites Gehirn")
> **Umfang (Phase 1):** reines Text-Embedding-Upgrade. Multimodale Einspeisung (Bild/Audio/Video) ist
> bewusst **Phase 2** (eigenes Projekt) und NICHT Teil dieser Spec.
> **Freigaben (Frank, 08.07.2026):** Scope = "erst Text-Upgrade, Multimodal Phase 2"; Preview-Risiko =
> "Preview nutzen + Fallback-Schalter" (Modell ist inzwischen GA → Risiko klein, Fallback bleibt);
> Spec-First; Migration = Blau/Grün.

## 1. Ziel

Der Cortex-Speicher findet Einträge heute über `gemini-embedding-001` (Text-only, 1536 Dimensionen).
Er soll auf **`gemini-embedding-2`** (3072 Dimensionen) umgestellt werden — genauere Textsuche und die
Grundlage für spätere multimodale Suche. In dieser Phase ändert sich das gespeicherte Datenmodell NICHT
(weiter 1:1-Textspeicher); es wird nur die Vektorisierung getauscht.

## 2. Ausgangslage (Ist-Zustand, verifiziert im Code)

- **Nur `brain-api/app.py` bettet tatsächlich ein.** `librarian`, `dashboard`, `agent`, `mcp-server`
  rufen die brain-api auf (Proxy) — kein eigener Embedding-Code, keine eigene Dimensionsannahme.
- Live-Konfiguration in `second-brain-server/compose.yaml`:
  - `GEMINI_EMBED_MODEL: gemini-embedding-001`
  - `SB_EMBED_DIMS: "1536"`  (Kommentar dort: "MUSS zur Qdrant-Collection passen")
- Zwei Qdrant-Sammlungen, beide `VectorParams(size=EMBED_DIMS, distance=Cosine)`:
  - `brain` (Haupt-Dokumente), `brain_entities` (Entity-Register).
- Relevante Funktionen in `brain-api/app.py`:
  - `embed(text, task_type)` — Einzel-Embedding, nutzt `EmbedContentConfig(output_dimensionality, task_type)`.
  - `embed_many(texts, task_type)` — **bündelt `contents=[…]` in EINEN Call und erwartet N Vektoren**
    (Performance-Optimierung 1.20.0). Anzahl-Check `raise RuntimeError` bei Mismatch (kein stiller Fehler).
  - `embed_input(title, categories, text)` — baut den Dokument-Text (Titel/Kategorien prägen den Vektor mit).
  - `_init_store()` — legt Collections an **nur wenn nicht vorhanden** (`if COLLECTION not in _existing`).
  - `/reembed-all` — bettet Bestand in der BESTEHENDEN Collection neu ein (legt sie NICHT neu an).
  - `probe(EMBED_DIMS in (768,1536,3072), …)` — Dimensions-Sonde.
- CHUNK_CHARS=4000 (konservativ für das 2048-Token-Limit von `-001`).

## 3. Rechercheergebnisse (offizielle Google-Quellen, 08.07.2026)

| Punkt | Ergebnis | Quelle |
|-------|----------|--------|
| Modell-ID | **`gemini-embedding-2`** (ohne `-preview`); **GA** seit ~30.04.2026 | ai.google.dev/gemini-api/docs/embeddings; Google-DeepMind-Video |
| Verfügbarkeit | Über **Gemini Developer API mit API-Key** (`generativelanguage.googleapis.com`), wie bisher | offizielle Doku (REST-Beispiel) |
| `task_type` | **Entfernt** — durch **Text-Präfixe** ersetzt | Doku-Abschnitt "Task types with Embeddings 2" |
| Präfix Dokument | `title: {Titel} \| text: {Inhalt}` (ohne Titel: `title: none`) | offizielle Doku |
| Präfix Suchanfrage | `task: search result \| query: {Suchtext}` (weitere: question answering / fact checking / code retrieval / classification / clustering / sentence similarity) | offizielle Doku |
| **Batch-Verhalten** | **`contents=[t1,t2,…]` → EIN aggregierter Vektor** (NICHT N Vektoren wie bei `-001`). Für N separate Vektoren: pro Text ein Call, oder die asynchrone Batch API (`asyncBatchEmbedContent`, ~halber Preis) | offizieller NOTE-Block in `embeddings.md.txt` |
| Dimensionen | Default **3072**; empfohlen 768/1536/3072 (MRL) | Doku/Video |
| Normalisierung | <3072 wird NICHT auto-normalisiert; bei 3072 nicht nötig. Qdrant normalisiert bei **Cosine** ohnehin intern | Medium + Qdrant-Almanach §2 |
| Token-Limit | **8192** Token/Request (4× mehr als die 2048 von `-001`) | offizielle Community-Quelle |
| Preis | `-001` = $0,15/1M Token (Batch $0,075). Embedding-2-Preis nicht separat belegt; Migration umfasst wenige Mio. Token → **Cent-Bereich** | Google-Blog |
| `-001`-Status | Bleibt für Text verfügbar (kein Retirement genannt) → **taugt als Fallback** | offizielle Doku |

**Kernkonsequenz:** `embed_many` in seiner heutigen Bündel-Form ist mit Embedding 2 nicht mehr korrekt.

## 4. Design

### 4.1 Konfiguration
- `compose.yaml`: `GEMINI_EMBED_MODEL: gemini-embedding-2`, `SB_EMBED_DIMS: "3072"` (Cutover, s. 4.4).
- Code-Defaults in `app.py`: `EMBED_MODEL` default `gemini-embedding-2`, `EMBED_DIMS` default `3072`.
- **Fallback:** bleibt allein über die Env-Variablen steuerbar (`GEMINI_EMBED_MODEL=gemini-embedding-001` +
  `SB_EMBED_DIMS=…`). Der Code unterstützt BEIDE Modell-Pfade (s. 4.2), damit der Rückweg ohne Code-Änderung geht.
- Interne Erkennung: `IS_EMBED2 = EMBED_MODEL.startswith("gemini-embedding-2")`.

### 4.2 Embedding-Schicht (modell-bewusst, minimal-invasiv)
Die ~10 Aufrufstellen behalten ihre Signatur `embed(text, "RETRIEVAL_DOCUMENT"|"RETRIEVAL_QUERY")` /
`embed_many(texts, task_type)`. Der `task_type`-String bleibt als **interner Modus-Indikator**; die
Übersetzung passiert NUR in den Kern-Funktionen:

- **`gemini-embedding-001` (Fallback-Pfad):** unverändert — `task_type` als API-Parameter, `contents=[…]`
  gebündelt (N Vektoren), `output_dimensionality=EMBED_DIMS`.
- **`gemini-embedding-2` (Standardpfad):**
  - KEIN `task_type`-Parameter.
  - Absicht als Präfix: Dokument → `embed_input` liefert `title: … | text: …`; Query → `embed()` stellt
    `task: search result | query: …` voran.
  - **Ein Call pro Text** (Liste würde aggregieren). `embed_many` wird zu parallelen Einzel-Calls.
  - `output_dimensionality=3072`.
- `embed_input(title, categories, text)` wird modell-bewusst: für Embedding 2 baut es das offizielle
  `title: {Titel · Kategorien} | text: {chunk}`-Format (metadata-enriched retrieval bleibt erhalten,
  Kategorien fließen in den `title:`-Teil).

### 4.3 Parallelisierung & Chunking (Performance-Ausgleich)
- `embed_many` (Embedding-2-Pfad): Einzel-Calls über einen kleinen ThreadPool (Default `SB_EMBED_PARALLEL=8`),
  Reihenfolge streng erhalten (Ergebnis an Eingabe-Index binden), Anzahl-Check beibehalten, `probe` je Vektor.
- `CHUNK_CHARS` von 4000 → **12000** (nutzt das 8192-Token-Limit; ~3× weniger Chunks), `CHUNK_OVERLAP` 200 → 600.
  Beide über Env übersteuerbar; für den Fallback auf `-001` sinnvoll wieder auf 4000/200 stellen (das
  2048-Token-Limit gilt dort weiter) — daher CHUNK-Defaults ebenfalls modell-abhängig wählbar.
- Tages-Cap (`MAX_EMBED_CALLS_PER_DAY`) bleibt; bei Einzel-Calls steigt die Call-Zahl, daher Cap prüfen
  (größere Chunks senken die Chunk-Zahl gegen).

### 4.4 Blau/Grün-Migration (sicher, Rückweg erhalten)
Qdrant fixiert die Dimension bei Collection-Erstellung → 1536→3072 erzwingt neue Sammlungen. Ablauf:

1. **Vorbereitung:** neues eigenständiges Skript `second-brain-server/migrate_to_embedding2.py`
   (eigene schlanke Embedding-2-Logik über `google-genai`, liest `GEMINI_API_KEY`/`QDRANT_*` aus derselben
   Env). Es läuft, WÄHREND der Server unverändert auf `-001` online bleibt (echtes Blau/Grün).
2. **Ziel-Sammlungen anlegen:** `brain__e2` (size 3072) + `brain_entities__e2` (size 3072) inkl. aller
   Payload-Indizes (doc_id, title, category, categories, parent, parents, user_id, source, created_at;
   Entity: name_key, type, user_id) — Indizes VOR den Daten (Qdrant-Almanach §7/#11).
3. **Bestand neu einbetten:** alle Punkte aus `brain`/`brain_entities` seitenweise lesen (Payload, KEIN
   Vektor), `full_text`/Entity-Text mit Embedding 2 (Präfixe) neu vektorisieren, in die `__e2`-Sammlungen
   schreiben (Point-ID/Payload/created_at 1:1 erhalten). Batches wie `/reembed-all` (OOM-sicher).
4. **Verifikation:** `points_count` alt == neu je Sammlung; Stichproben-Roundtrip + eine Testsuche.
5. **Cutover:** `compose.yaml` auf `gemini-embedding-2`, `SB_EMBED_DIMS=3072`,
   `SB_COLLECTION=brain__e2`, `SB_ENTITY_COLLECTION=brain_entities__e2`; Container neu starten (Sekunden).
6. **Nachlauf:** Suche/Abruf live prüfen. Alte `brain`/`brain_entities` (1536) bleiben als Rückweg stehen
   und werden erst nach Franks Bestätigung gelöscht.
- **Hinweis Downtime/Delta:** Während Schritt 3 sollte Frank nichts Neues speichern (sonst landet der neue
  Eintrag nur in der alten Sammlung). Datenmenge klein → Lauf dauert wenige Minuten; alternativ kurzer
  Delta-Nachlauf. Für einen persönlichen Speicher vertretbar.

### 4.5 Sonden & Kommentare
- `probe(EMBED_DIMS in (768,1536,3072), …)` bleibt (3072 erlaubt); Kommentar/Meldung auf Embedding 2 anpassen.
- `probe(len(vec)==EMBED_DIMS)` bleibt (fängt Dimensions-Drift, auch aggregierte 1-Vektor-Antworten indirekt).
- Header-Doku (Z. 28-31) + `embed_many`-Kommentar (Batch→Einzel-Call) + `_init_store`-Log aktualisieren.
- `checkpoint`/`_log` Migrationslauf protokolliert Anzahl + Dauer (Observability-First).

## 5. Betroffene Dateien
1. `second-brain-server/brain-api/app.py` — embed/embed_many/embed_input modell-bewusst, Defaults, Präfixe,
   Parallelisierung, Chunk-Defaults, Sonden/Kommentare. (>500 Zeilen → gezielt per Grep+Edit, NICHT per Agent.)
2. `second-brain-server/compose.yaml` — Env-Cutover (Modell, Dims, Collection-Namen).
3. `second-brain-server/migrate_to_embedding2.py` — **neu**, einmaliges Blau/Grün-Migrationsskript.
4. `dashboard` — nur kosmetisch (zeigt `embed_model` aus `/health`; nichts zu ändern, erbt automatisch).

## 6. Verifikation / Tests
- `python3 -m py_compile` auf brain-api + Migrationsskript.
- Vor Cutover: Testsuche gegen `brain__e2` liefert plausible Treffer; `by-title`-Roundtrip 1:1.
- Nach Cutover: `/health` zeigt `embed_model=gemini-embedding-2`, `embed_dims=3072`, `ready=true`;
  `/list`-Anzahl == vorher; eine bekannte Suche liefert den erwarteten Eintrag.
- `tests/large_doc_roundtrip.py` gegen den neuen Stand laufen lassen (großes Dokument 1:1).

## 7. Risiken & Gegenmaßnahmen
- **Aggregations-Falle** (Liste → 1 Vektor): eliminiert durch Einzel-Calls + Anzahl-Check (`raise`).
- **RAM:** 3072 verdoppelt die Vektorgröße; bei wenigen Tausend Punkten unkritisch. Qdrant im Compose
  bei Bedarf `on_disk` + Quantisierung (Qdrant-Almanach §1) — vorerst nicht nötig.
- **Preview→GA:** Modell ist GA; Fallback-Pfad auf `-001` bleibt vollständig funktionsfähig.
- **Suchgranularität** durch größere Chunks: moderat (12000 statt 4000); bei schlechteren Treffern
  reduzierbar. Env-übersteuerbar.

## 8. Nicht-Ziele (Phase 2, separat)
- Multimodale Einspeisung (Bilder/Audio/Video speichern & durchsuchbar machen): Upload-Endpunkte,
  Payload-/Dashboard-Erweiterung, `Embedding aggregation`-Nutzung. Eigene Spec.
- Asynchrone Batch API (halber Preis) für Massen-Reembeds: optionale spätere Optimierung.
