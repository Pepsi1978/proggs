# Umbau-Plan: Mehrfach-Kategorien pro Eintrag (Multi-Category)

> Frank-Wunsch 2026-06-25. Status: **GEPLANT, noch nicht umgesetzt.**
> Umsetzung erst, wenn die parallele Unterkategorien-Arbeit (agent ≥ v0.26.0, brain-api parent-Backfill,
> dashboard Hierarchie-UI) vollständig durch und gemergt ist — beide bauen am selben `category`-Feld.
> Bezug: `bugs/server/qdrant.md` §7/§10/§12, `best-practices/server/rag-retrieval.md` §1–§4.

---

## 1. Ziel & gewünschtes Verhalten (Franks Wortlaut sinngemäß)

- Ein **bestehender** Eintrag kann **mehreren Kategorien** zugeordnet werden (z.B. „Persönlich" UND „Gesundheit").
- **UI:** Im Eintrags-Drawer steht oben die Kategorie (z.B. „Persönlich"); **davor ein Plus**. Klick aufs Plus →
  der **gesamte Kategoriebaum** klappt auf → man wählt **eine weitere Kategorie** dazu. Beliebig erweiterbar.
- **Payload:** mehrere Kategorien werden gespeichert.
- **Vektor:** **alle** Kategorien fließen ins Embedding mit ein — inkl. **Hierarchie-Ebenen** (Hauptkategorie →
  Unterkategorie → ggf. weitere) UND Titel (wie schon heute Titel + Kategorie).
- **Suche/Filter:** der Eintrag ist in **allen** seinen Kategorien auffindbar (Eingrenzung der semantischen Suche).
- **Statistik:** „Einträge Gesamt" zählt ihn trotzdem nur **einmal** (dedupliziert auf `doc_id`) — wie heute.
  Die einzelnen Kategorie-Balken zeigen ihn in **jeder** seiner Kategorien (Summe der Balken darf > Gesamt sein).

---

## 2. Datenmodell (additiv & abwärtskompatibel)

Heute (Payload je Qdrant-Point):
`doc_id, user_id, title, category (str), parent (str), chunk_index, chunk_count, chunk_text, full_text, created_at, updated_at`

**Neu — zwei Listen-Felder dazu, alte Felder bleiben (primär = erste Kategorie):**

| Feld | Typ | Bedeutung |
|------|-----|-----------|
| `categories` | **Liste[str]** (Keyword-Index) | ALLE Kategorien des Eintrags, z.B. `["Persönlich", "Gesundheit", "Programmieren/Best-Practices"]` |
| `parents` | **Liste[str]** (Keyword-Index) | Haupt-Teile ALLER Kategorien (vor dem `/`), dedupliziert, z.B. `["Persönlich", "Gesundheit", "Programmieren"]` |
| `category` | str (bleibt) | **Primär** = `categories[0]` — für Anzeige/Abwärtskompat/`make_doc_id`-Unabhängigkeit |
| `parent` | str (bleibt) | **Primär** = `parents[0]` — Abwärtskompat |

**Warum additiv statt `category`→`categories` hart ersetzen:** abwärtskompatibel (174 Altbestand + alle Endpoints
funktionieren weiter), und Qdrant matcht `MatchValue(key="categories", value="X")` nativ, wenn `X` in der Liste
steht (Keyword-Index auf Array — KEIN `nested` nötig, das ist nur für Array-von-Objekten, `qdrant.md` §12).

**`doc_id` bleibt titel-basiert** (`make_doc_id(user_id, title)`) — Mehrfach-Kategorien ändern die Identität
NICHT. Ein Eintrag = ein `doc_id`, egal wie viele Kategorien.

---

## 3. Embedding (alle Kategorien + Hierarchie-Ebenen in den Vektor)

`embed_input(title, category, text)` → **`embed_input(title, categories: list[str], text)`**.

Präfix-Beispiel (eine flache + eine hierarchische Kategorie):
```
[Titel: Bandscheibe L4 L5 | Kategorien: Gesundheit, Persönlich, Programmieren > Best-Practices]

<text 1:1>
```
- Jede hierarchische Kategorie `Haupt/Unter` wird zu `Haupt > Unter` expandiert, damit **beide Ebenen** das
  Signal prägen (Frank: „Hauptkategorie, dann Unterkategorie … das soll mit rein in den Vektor").
- `full_text` und `chunk_text` bleiben **1:1** (nur der Vektor wird angereichert — wie heute bei Titel+Kategorie).
- **Folge:** Jede Kategorie-Änderung erzwingt **Re-Embed** des Eintrags (steht so schon in `rag-retrieval.md` §4) —
  greift in den bestehenden „bei jeder Änderung neuer Vektor"-Mechanismus.

---

## 4. brain-api — konkrete Änderungen (`brain-api/app.py`)

1. **Helfer:**
   - `category_parents(categories: list[str]) -> list[str]`: Haupt-Teile aller Kategorien, dedupliziert (nutzt `category_parent`).
   - `embed_input(title, categories, text)`: Präfix mit allen Kategorien + Hierarchie-Expansion (§3).
   - `_norm_categories(req) -> list[str]`: nimmt entweder `categories` (Liste) ODER `category` (str, Abwärtskompat) → saubere, deduplizierte, kanonische Liste; erste = primär.
2. **Schreib-Wege** (alle reichen die Liste durch + setzen `categories`/`parents` UND primär `category`/`parent`):
   `store`, `update_entry (PUT /entry)`, `set_entry_category (/entry/category)`, `trash_restore`, `reembed_all`.
   - `StoreReq`/`UpdateReq`: optionales Feld `categories: list[str] | None` (zusätzlich zu `category`).
3. **NEU: `POST /entry/categories`** `{doc_id, categories: [...], user_id}` — setzt die komplette Kategorie-Liste
   EINES Eintrags und **bettet frisch neu ein** (Vektor enthält die Kategorien). Analog zum bestehenden
   `/entry/category`, nur mit Liste. (Das ist der Endpoint hinter dem Drawer-Plus.)
4. **Filter-Endpoints auf die Liste umstellen** (MatchValue auf Array-Feld):
   - `by_category`: `FieldCondition(key="categories", match=MatchValue(value=category))`
   - `by_parent`: `FieldCondition(key="parents", match=MatchValue(value=parent))`
   - `/search` (Zeile ~754): `category`-Filter → `categories`, `parent`-Filter → `parents`.
   - `rename_category`/`detach_category`/`category_counts`: über `categories` zählen/ändern (set_payload muss die
     Liste pflegen: Element ersetzen/entfernen, nicht das ganze Feld).
5. **`category_counts`:** pro Kategorie die distinct `doc_id` zählen, die die Kategorie in `categories` haben
   (Multi-Cat-Eintrag erscheint in mehreren) + zusätzlich ein **`total_distinct`** (distinct doc_id gesamt) für
   die korrekte „Einträge Gesamt"-Zahl (Summe der Balken ≠ Gesamt).
6. **Ausgabe ergänzen:** `by_category`/`by_parent`/`by_title`/`list`/`search` geben `categories` (+ `parents`) mit
   aus (heute fehlt selbst `parent` in der Ausgabe — war die irreführende Diagnose beim parent-Bug).
7. **Migration: `POST /migrate-multi-category`** — für alle 174 Punkte ohne `categories`: `categories=[category]`,
   `parents=[parent or category_parent(category)]` per `set_payload` (kein Re-Embed nötig, solange embed_input
   für 1 Kategorie denselben Präfix erzeugt wie heute). Idempotent. Danach **einmal `/reembed-all`** (weil
   embed_input jetzt die Liste nutzt → konsistente Vektoren).
8. **Payload-Index:** `categories` und `parents` als Keyword-Index VOR dem Befüllen anlegen (Startup-Init, wie
   schon `parent`) — sonst zieht der filterable HNSW keine Filter-Kanten (`qdrant.md` §11). Reindex einplanen.
9. **Version-Bump** brain-api (Minor, neues Feature).

---

## 5. agent — Änderungen (`agent/app.py`)

- **Minimal für Franks Kernwunsch** (bestehende Einträge im Drawer mehreren Kategorien zuordnen): der Agent muss
  hier NICHTS können — das läuft über das Dashboard → brain `/entry/categories`.
- **Optional (spätere Etappe):** Speicheragent darf beim Ablegen NEUER Einträge mehrere passende Kategorien
  vorschlagen (statt genau einer). Dann `speicheragent_decide` → `categories: [...]`, Rückfrage bei Frank.
  Vorerst: eine Kategorie beim Senden (wie heute), Mehrfachzuordnung danach im Drawer.
- `brain_store`/`brain_set_entry_category`-Helfer um die Listen-Variante erweitern (Proxy-Durchreichung).

---

## 6. dashboard — Änderungen (`dashboard/app.py` + `static/index.html`)

1. **Drawer-UI (Kern):** vor der Kategorie-Anzeige ein **Plus-Knopf**. Klick → der bestehende **Kategoriebaum**
   (Haupt aufklappbar, Unter eingerückt — gibt es schon aus der Unterkategorien-Arbeit) als Auswahl-Popover.
   Gewählte Kategorie wird zur Liste hinzugefügt → als **entfernbare Chips** angezeigt (jede mit kleinem ×).
   Speichern → `POST /api/entry/categories {doc_id, categories}`.
2. **Proxy `dashboard/app.py`:** neue Route `POST /api/entry/categories` → brain `/entry/categories` (Liste durchreichen, `categories[i]` ≤ 60 Zeichen, max. sinnvoll begrenzen z.B. 6).
3. **Übersicht:** Balken/Legende über `categories` (Eintrag erscheint in jeder seiner Kategorien); die große Zahl
   „Einträge Gesamt" aus `total_distinct` (distinct doc_id), NICHT aus der Balkensumme.
4. **Eintrags-Anzeige (Such-/Kategorie-Liste):** Kategorie-Chip(s) statt eines einzelnen Tags — zeigt alle
   Kategorien des Eintrags.
5. **Version-Bump** dashboard.

---

## 7. Migration & Re-Embed (Reihenfolge, wie beim Titel-Feature)

1. brain-api deployen (neues Modell + Endpoints + Index).
2. `POST /migrate-multi-category` → `categories`/`parents` auf alle 174 setzen (set_payload).
3. `POST /reembed-all` → Vektoren mit dem neuen `embed_input` (Liste) neu (idempotent; Texte 1:1).
4. Verifizieren: `by-category`/`by-parent` == heute (keine Regression bei 1-Kategorie-Einträgen); ein Test-Eintrag
   mit 2 Kategorien ist über BEIDE findbar; „Einträge Gesamt" unverändert.

---

## 8. Eval-Cases (Regressionsschutz — ins agent-Eval-Set, id ab 101)

- `store_multicat`: Eintrag mit 2–3 Kategorien ablegen → per `by-category` in **jeder** Kategorie auffindbar,
  per `by-title` exakt 1×, `total_distinct` zählt 1.
- `query`: über einen Begriff finden, der nur in der Zweit-Kategorie steckt → Treffer (Kategorie prägt Vektor).
- Negativ: Kategorie wieder entfernen → Eintrag verschwindet aus genau dieser Kategorie, bleibt in den anderen.

---

## 9. Etappen (jede committen + deployen + verifizieren)

| Etappe | Inhalt | Dateien |
|--------|--------|---------|
| 0 | **Vorbedingung:** Unterkategorien-Arbeit der Parallel-Session ist durch + gemergt | — |
| 1 | brain-api: Datenmodell, `embed_input(Liste)`, Filter auf `categories`/`parents`, `/entry/categories`, `category_counts` + `total_distinct`, `/migrate-multi-category`, Index | `brain-api/app.py`, `compose.yaml` (Index ggf.) |
| 2 | Migration + Re-Embed ausführen + verifizieren | (Server-Calls) |
| 3 | dashboard: Drawer-Plus + Kategoriebaum-Mehrfachauswahl + Chips + Übersicht (`total_distinct`) + Proxy | `dashboard/app.py`, `static/index.html` |
| 4 | (optional) agent: Speicheragent schlägt mehrere Kategorien vor | `agent/app.py` |
| 5 | Eval-Cases + Voll-Verifikation | `agent/app.py` |

---

## 10. Risiken & Gegenmaßnahmen

- **Kollision mit der Unterkategorien-Session** → Etappe 0 als harte Vorbedingung; vorher NICHT starten.
- **Abwärtskompat** → `category`/`parent` bleiben als primär; alte Clients/MCP funktionieren weiter.
- **Qdrant Array-Filter** → MatchValue auf Keyword-Array ist nativ; Index VOR Befüllung anlegen (`qdrant.md` §11);
  KEIN `nested` (§12).
- **Recall/Übersicht** → `total_distinct` getrennt von Balkensumme; Balkensumme > Gesamt ist gewollt.
- **Funktionserhalt (Direktive #3)** → 1-Kategorie-Einträge müssen sich exakt wie heute verhalten (Regressionstest
  in Etappe 2/5, Baseline vor Umbau festhalten).
- **Embedding-Budget** → ein Re-Embed-Lauf (174 Punkte), unkritisch (< Tages-Cap).

---

## 11. Offene Detail-Entscheidungen (bei Umsetzung kurz mit Frank klären)

- Max. Anzahl Kategorien pro Eintrag (Vorschlag: 6).
- Reihenfolge der Kategorien im Vektor-Präfix (Vorschlag: primär zuerst, dann Reihenfolge der Zuordnung).
- Soll `category_counts` die Hierarchie-Zwischenebene (`parents`) als eigene Balken zeigen oder nur die volle Kategorie?
