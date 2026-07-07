# Speicher-Schema (1:1-Layer, AS-BUILT) — Best Practices

> **Das tatsaechlich implementierte Schema des "zweiten Gehirns"** (Schicht 1, stummer 1:1-Speicher,
> `second-brain-server/brain-api`). Hier steht, WIE Daten konkret abgelegt und abgerufen werden —
> die verbindliche Grundlage fuer den spaeteren Bibliothekar-Agenten (Phase 4) und jeden Client.
>
> **Stand:** **2026-06-23** (brain-api v1.1.0, mem0-frei). Anker: Qdrant 1.18.2, Collection `brain`,
> Gemini-Embedding `gemini-embedding-001` @1536 (Cosine).
>
> **Abgrenzung:** [[datenmodell]] ist die **Planungs-/Research-Seite** (externe Theorie: Confidence,
> Graph, bi-temporal, Cortex/Zep/mem0). Davon wurde bewusst **NICHTS** uebernommen — mem0 ist raus
> (es dichtete/halluzinierte), das Gehirn ist ein **wortwoertlicher 1:1-Dokument-Speicher**, keine
> KI bearbeitet etwas im Speicher. DIESE Datei beschreibt, was wirklich gebaut wurde.
> Schwester: [[orchestrator-und-suche]], [[memory-backends]], [[multi-client-zugriff]].

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Frage / Situation | Sofort-Regel |
|---|-------------------|--------------|
| 1 | Was wird gespeichert? | **Text 1:1** (PFLICHT) + **Titel** (optional, = Schluessel) + **Kategorie** (optional). Keine KI-Bearbeitung |
| 2 | Etiketten in den Text einweben? | **NIE.** Nur reiner Inhalt in den Vektor; Titel/Kategorie/Datum GETRENNT ins Payload |
| 3 | Gleicher Titel nochmal speichern | **Ersetzt** den alten Eintrag (`doc_id = t_<sha1(user::titel)>`). Ohne Titel → neue UUID (`d_<uuid>`) |
| 4 | Langer Text | Chunking **nur fuer die Suche** (4000 Zeichen / 200 Overlap); der **volle Text liegt 1:1 im Payload JEDES Chunks** → exakter Abruf gibt das ganze Dokument |
| 5 | Abruf-Wege (alle liefern 1:1) | `by-title` (exakt), `by-category`, `by-date`, `search` (semantisch + Filter), `list`, `forget` |
| 6 | Gefilterte Suche | **Erst Payload-Filter (Kategorie/Datum) eingrenzen, DANN semantisch** — nicht hinterher. `category` Keyword-indiziert, `created_at` Datetime-indiziert |
| 7 | Embedding asymmetrisch | Speichern `RETRIEVAL_DOCUMENT`, Suchen `RETRIEVAL_QUERY` (bessere Treffer); `output_dimensionality=1536` EXPLIZIT |
| 8 | Kategorie-Frage ("alle meine Ziele") | Auf `by-category` routen (VOLLSTAENDIG), nicht semantisch top-5 |
| 9 | Kategorienamen | ASCII-lowercase, kurz (siehe feste Liste §5). Agent pflegt die Liste mitwachsend (Phase 4) |

---

## 1. Datenmodell (wie es wirklich abgelegt ist)

Ein **Eintrag** = ein Dokument. Felder:

| Feld | Pflicht | Bedeutung |
|------|---------|-----------|
| `text` (→ `full_text` im Payload) | **JA** | Der wortwoertliche Inhalt, 1:1 rein/raus |
| `title` | nein | Eindeutiger Schluessel je Nutzer. **Gleicher Titel ersetzt** den alten Eintrag |
| `category` | nein | Schublade (z.B. `ziele-2026`) — fuer Sammelabruf + Filter |
| `user_id` | (default `frank`) | Besitzer |

**doc_id-Logik** (`brain-api/app.py:make_doc_id`):
- Mit Titel → `t_<sha1(user_id::titel.lower())[:24]>` → deterministisch → **Update by Titel**.
- Ohne Titel → `d_<uuid4>` → immer neuer Eintrag.

**Qdrant-Payload je Punkt (Chunk):**
`doc_id, user_id, title, category, chunk_index, chunk_count, chunk_text, full_text, created_at, updated_at`.
- `full_text` = der **komplette** 1:1-Text (in JEDEM Chunk wiederholt) → ein Treffer auf irgendeinen Chunk liefert das ganze Dokument unveraendert.
- `chunk_text` = nur dieser Abschnitt (fuer den "Treffer-Ausschnitt" in der Suche).
- `created_at`/`updated_at` = RFC3339 (`YYYY-MM-DDTHH:MM:SSZ`). Beim Ueberschreiben bleibt `created_at` erhalten, nur `updated_at` neu.

**Chunking** beruehrt den 1:1-Text NICHT — es zerlegt nur fuer das Embedding (Gemini-Input-Limit), der volle Text bleibt separat im Payload.

---

## 2. Schema-Grundsatz: Inhalt rein, Metadaten getrennt (KRITISCH)

- **Reiner 1:1-Inhalt in den Vektor** (`full_text`/`chunk_text`). Kategorie/Titel/Datum kommen **GETRENNT**
  ins Payload — **NIEMALS** als "Kategorie: …"-Etikett in den Inhaltstext einweben. Sonst kommt
  "verschmutzter" Text zurueck, und der Antwort-Agent soll selbst entscheiden, ob er Metadaten nennt.
- Inhaltstext trotzdem **natuerlich + reichhaltig** formulieren (ergibt einen besseren Vektor) — nur ohne
  kuenstliche Tags. Die Aufbereitung (z.B. eingesprochene Gedanken glaetten) passiert **VORHER
  client-seitig** in Franks Apps; der Server bekommt fertige Texte.
- **Benutzen macht das Gehirn NICHT schlauer** — nur Speichern + Struktur (+ ggf. spaeteres Embedding-Upgrade).

---

## 3. Abruf-Wege (alle liefern 1:1)

| Endpunkt | MCP-Tool | Zweck |
|----------|----------|-------|
| `GET /by-title` | `get_by_title` | Exakt per Titel → ganzes Dokument 1:1 |
| `GET /by-category` | `get_by_category` | Alle Eintraege einer Kategorie (vollstaendig, dedupliziert) |
| `GET /by-date` | `get_by_date` | Eintraege eines Speichertags (`YYYY-MM-DD`, Praefix auf `created_at`) |
| `POST /search` | `recall` | Semantische Suche + **optionale Filter** (Kategorie/Datum) |
| `GET /list` | `list_memories` | Titel/Kategorie/Groesse (ohne Volltexte, kompakt) |
| `DELETE /by-title` | `forget` | Eintrag per Titel loeschen |

---

## 4. Gefilterte Vektorsuche (NEU 2026-06-23, Phase 3.1)

Franks Beispiel "war ich letzten Monat angeln": **erst Payload-Filter eingrenzen, DANN semantisch darin
suchen** — der Filter schraenkt den Suchraum ein, BEVOR der Vektor-Vergleich laeuft (nicht hinterher).

- `POST /search` akzeptiert optional: `category`, `date` (einzelner Tag) ODER `date_from`/`date_to` (Bereich).
- Umsetzung Qdrant-nativ: `category` als Keyword-`FieldCondition`, Datum als **`DatetimeRange`** auf
  `created_at` (Datetime-Payload-Index, idempotent angelegt, arbeitet auf den Bestandswerten — kein Backfill).
- Fallback: fehlt `DatetimeRange` in der Client-Version → sauberer Python-Nachfilter (mehr Kandidaten holen,
  lexikalisch nach ISO-Datum filtern). Funktionserhaltend.
- Live-Beleg (Intent-Checkpoint im Log): `native_date:true` zeigt, dass der Datetime-Index genutzt wird.

**Payload-Indizes:** `doc_id, title, category, user_id` als `keyword`; `created_at` als `datetime`.

---

## 5. Feste Kategorienliste (Stand 2026-06-23, live aus dem Store)

ASCII-lowercase Schluessel. Stand der 177 importierten Eintraege:

| Kategorie | ~Anzahl | Kategorie | ~Anzahl |
|-----------|---------|-----------|---------|
| `persoenlich` | 29 | `gesundheit` | 5 |
| `ki-arbeitsweise` | 27 | `nem-stack` | 5 |
| `theorie` | 25 | `fahrzeug-strom` | 2 |
| `ziele-2026` | 22 | `drohnen` | 2 |
| `fitness` | 20 | `arbeitsregeln` | 6 |
| `inspiration` | 17 | `geraete` | 10 |
| `leitsaetze` | 7 | | |

**Agenten-Gedaechtnis fuer Kategorien (Phase 4, Frank-Wunsch):** Der Bibliothekar-Agent kennt IMMER
ALLE existierenden Kategorien, kann bei Bedarf NEUE anlegen und fuegt jede neue automatisch seinem
eigenen Kategorien-Wissen hinzu (mitwachsende Liste = kleiner eigener Speicher, GETRENNT vom Inhalt).
So bleibt die Einsortierung konsistent und das Query-Routing kennt jederzeit alle Schubladen.

---

## 6. Query-Routing (fuer den Phase-4-Agenten)

Der Agent waehlt den Abruf-Weg nach der Frage-Art (das "1:1 vs. Zusammenfassung"-Erkennen sitzt im
Agenten/Client, NICHT im Speicher):

| Frage-Art | Weg |
|-----------|-----|
| "Gib mir Direktive 1" (exakter Titel) | `get_by_title` → ganzes Dokument 1:1 |
| "Alle meine Ziele" (ganze Kategorie) | `get_by_category` (VOLLSTAENDIG, nicht semantisch top-5) |
| "Was habe ich am 2026-06-23 gespeichert" | `get_by_date` |
| "War ich letzten Monat angeln" | `recall` mit `category`/`date_from`/`date_to` (gefilterte Suche) |
| inhaltliche/thematische Frage | `recall` (semantisch), Antwort im STIL der Frage |

---

## 7. Embedding

`gemini-embedding-001`, **1536 dim** (`output_dimensionality` EXPLIZIT — defaultet sonst 768),
Distanz **Cosine**. Asymmetrisch: Speichern `task_type=RETRIEVAL_DOCUMENT`, Suchen `RETRIEVAL_QUERY`
(bessere Treffer). Kosten winzig (~0,15 Cent fuer alle 177; nur Input-Token). Defense-in-Depth-Cap
`SB_MAX_EMBED_CALLS_PER_DAY` im Code; harter Kosten-Cap liegt beim Google-Budget.

---

## Zusammenspiel / Bugs

| Thema | Verweis |
|-------|---------|
| Qdrant-Fallen (TLS/WRONG_VERSION_NUMBER, Payload-Index, OOM) | `bugs/server/qdrant.md`, `best-practices/server/qdrant.md` §5 |
| Gemini-Embedding 1536 EXPLIZIT | `best-practices/second-brain/memory-backends.md` §0 |
| Planungs-/Research-Datenmodell (NICHT implementiert) | [[datenmodell]] |
| Routing/Suche-Architektur (Theorie) | [[orchestrator-und-suche]] |
