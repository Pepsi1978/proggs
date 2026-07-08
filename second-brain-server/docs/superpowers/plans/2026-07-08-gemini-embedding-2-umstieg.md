# Gemini Embedding 2 Umstieg — Implementation Plan

> ⛔ **ARCHIVIERT / ABGESCHLOSSEN (Stand 08.07.2026, 20.57 Uhr).** Der Umstieg ist vollzogen und
> `gemini-embedding-001` wurde am 08.07.2026 VOLLSTAENDIG entfernt — Code (brain-api reine E2-Logik),
> Dashboard-Rueckschalter und die alten 1536-Qdrant-Collections (`brain`, `brain_entities`). Der hier
> beschriebene `-001`-Fallback/Rueckweg existiert im Live-System NICHT mehr. Dieses Dokument bleibt rein
> historisch als Referenz-/Wiederherstellungsanleitung erhalten (der brain-api-Changelog verweist darauf).

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task (inline). Die Kern-Datei `brain-api/app.py` ist >2000 Zeilen → laut Regel `search-and-agent-scope` NICHT per Subagent editieren, sondern direkt per Grep + Read(Ranges) + Edit. Steps nutzen Checkbox (`- [ ]`) Syntax.

**Goal:** Den Cortex-Textspeicher von `gemini-embedding-001` (1536 Dim) auf `gemini-embedding-2` (3072 Dim) umstellen — rückwärtskompatibel (Fallback auf `-001`) und über eine sichere Blau/Grün-Migration ohne Datenverlust.

**Architecture:** Nur `brain-api/app.py` bettet ein; alle anderen Dienste proxien dorthin. Die Embed-Schicht (`embed`, `embed_many`, `embed_input`, `_entity_embed_text`) wird modell-bewusst: für `-001` bleibt alles wie heute (task_type + Listen-Batch), für `gemini-embedding-2` gilt Text-Präfix statt task_type und **ein Call pro Text** (eine Liste würde aggregieren). Ein separates Migrationsskript befüllt neue 3072-Sammlungen aus den alten, während der Server unverändert online bleibt; der Cutover ist ein bewusster Env-Wechsel + Redeploy.

**Tech Stack:** Python 3.11+, FastAPI, `google-genai` SDK, Qdrant (`qdrant-client`), Docker Compose. Deploy auf VPS 168.231.83.205 (`/opt/second-brain`) via `scp` + `docker compose up -d --build`.

## Global Constraints

- Modell-ID gepinnt: **`gemini-embedding-2`** (ohne `-preview`; GA). NIE `-latest`.
- Standard-Dimension: **3072** (Env `SB_EMBED_DIMS`); erlaubte Werte 768/1536/3072.
- `task_type` bei Embedding 2 NICHT als API-Parameter senden → Text-Präfixe (Dokument `title: … | text: …`, Query `task: search result | query: …`).
- Bei Embedding 2 NIEMALS mehrere Texte als Liste an `embed_content` geben (→ 1 aggregierter Vektor). Ein Call pro Text.
- Input-Limit Embedding 2: 8192 Token. Chunk-Default für E2 = 12000 Zeichen, für `-001` = 4000.
- Fallback auf `gemini-embedding-001` muss ohne Code-Änderung allein über Env funktionieren.
- 1:1-Prinzip unangetastet: `full_text`/`chunk_text`/Titel/Kategorie/`created_at`/`doc_id` bleiben wortwörtlich; NUR der Vektor ändert sich.
- Deutsche Umlaute in allen neuen Kommentaren/Logs. Commit+Push VOR jedem Deploy/Build. Version sichtbar hochzählen.
- Sonden erhalten: `probe(len(vec)==EMBED_DIMS, …)` bleibt an jeder Embed-Stelle.

## File Structure

- `brain-api/app.py` (modify) — Config-Defaults + `IS_EMBED2`-Flag; `embed`, `embed_many`, `embed_input`, `_entity_embed_text` modell-bewusst; Chunk-Defaults; probe/Kommentare/Header; VERSION-Bump.
- `migrate_to_embedding2.py` (create, im `second-brain-server/`-Wurzelverzeichnis) — einmaliges Blau/Grün-Migrationsskript (stdlib + `google-genai` + `qdrant-client`).
- `compose.yaml` (modify, NUR im Cutover-Task) — `GEMINI_EMBED_MODEL`, `SB_EMBED_DIMS`, `SB_COLLECTION`, `SB_ENTITY_COLLECTION`.
- `tests/embed_input_prefix_test.py` (create) — reiner Unit-Test der Präfix-Logik (kein Netzwerk).

---

### Task 1: Modell-bewusste Embed-Schicht in brain-api (rückwärtskompatibel)

**Files:**
- Modify: `second-brain-server/brain-api/app.py` (Config ~Z.73-89; `embed` ~Z.421-431; `embed_many` ~Z.434-459; `embed_input` ~Z.620-636; `_entity_embed_text` ~Z.1940-1946; probe ~Z.254; Header ~Z.28-31; VERSION)
- Test: `second-brain-server/tests/embed_input_prefix_test.py`

**Interfaces:**
- Produces: `IS_EMBED2: bool`; `embed(text, task_type) -> list[float]`, `embed_many(texts, task_type) -> list[list[float]]` (Signaturen UNVERÄNDERT — alle ~10 Aufrufstellen bleiben); `embed_input(title, categories, text) -> str`; `_entity_embed_text(name, etype, aliases) -> str`.

- [ ] **Step 1: Config-Flag + Chunk-Defaults modell-abhängig** — direkt nach `EMBED_DIMS` (Z.74) einfügen und die `CHUNK_CHARS`-Defaults (Z.83-84) modell-abhängig machen.

```python
EMBED_MODEL = os.getenv("GEMINI_EMBED_MODEL", "gemini-embedding-2")   # war: gemini-embedding-001
EMBED_DIMS = int(os.getenv("SB_EMBED_DIMS", "3072"))                  # war: 1536
# Embedding 2 (multimodal, GA): task_type entfaellt -> Text-Praefixe; eine Liste aggregiert zu EINEM
# Vektor -> pro Text EIN Call (bugs/apis/google-gemini-api.md §J23/§J24). -001 bleibt als Fallback.
IS_EMBED2 = EMBED_MODEL.startswith("gemini-embedding-2")
EMBED_PARALLEL = int(os.getenv("SB_EMBED_PARALLEL", "8"))             # parallele Einzel-Calls (nur E2)
```

Und die Chunk-Defaults (Z.83):
```python
# Chunking fuer die SUCHE. Embedding 2: 8192 Token Input-Limit (~12000 Zeichen konservativ);
# gemini-embedding-001: 2048 Token (~4000 Zeichen). Modell-abhaengiger Default, per Env uebersteuerbar.
CHUNK_CHARS = _env_int("SB_CHUNK_CHARS", 12000 if IS_EMBED2 else 4000)
CHUNK_OVERLAP = _env_int("SB_CHUNK_OVERLAP", 600 if IS_EMBED2 else 200)
```

- [ ] **Step 2: `ThreadPoolExecutor` importieren** — bei den Imports (Z.35-49) ergänzen:

```python
from concurrent.futures import ThreadPoolExecutor
```

- [ ] **Step 3: `probe`-Meldung anpassen** (Z.254) — Text auf Embedding 2 aktualisieren (3072 bleibt gültig):

```python
probe(EMBED_DIMS in (768, 1536, 3072), "EMBED_DIMS unerwartet (gemini-embedding-2: 768/1536/3072)", dims=EMBED_DIMS)
```

- [ ] **Step 4: `embed_input` modell-bewusst** (Z.620-636) — für E2 offizielles `title: … | text: …`, sonst altes Format. Body ab Z.629 ersetzen:

```python
    t = (title or "").strip()
    exp = [" > ".join(s.strip() for s in c.split("/") if s.strip()) for c in categories if c and c.strip()]
    if IS_EMBED2:
        # Offizielles Embedding-2-Dokumentpraefix: 'title: {…} | text: {…}' (ohne Titel -> 'none').
        # Kategorien fliessen in den title-Teil (metadata-enriched retrieval bleibt erhalten).
        head = t
        if exp:
            head = f"{t} · {', '.join(exp)}" if t else ", ".join(exp)
        return f"title: {head or 'none'} | text: {text}"
    parts: list[str] = []
    if t:
        parts.append(f"Titel: {t}")
    if exp:
        parts.append("Kategorien: " + ", ".join(exp))
    return f"[{' | '.join(parts)}]\n\n{text}" if parts else text
```

- [ ] **Step 5: `_entity_embed_text` modell-bewusst** (Z.1940-1946) — für E2 als Dokumentpraefix:

```python
def _entity_embed_text(name: str, etype: str, aliases: list[str]) -> str:
    parts = [f"Entität: {name}"]
    if etype:
        parts.append(f"Typ: {etype}")
    if aliases:
        parts.append("Auch bekannt als: " + ", ".join(aliases))
    body = " | ".join(parts)
    return f"title: {name} | text: {body}" if IS_EMBED2 else body
```

- [ ] **Step 6: `embed` modell-bewusst** (Z.421-431) — Query-Praefix + kein task_type bei E2:

```python
def embed(text: str, task_type: str) -> list[float]:
    """Text -> Vektor via Gemini. task_type ist der INTERNE Modus-Indikator
    ('RETRIEVAL_DOCUMENT' beim Speichern, 'RETRIEVAL_QUERY' beim Suchen). Uebersetzung modell-abhaengig:
    -001 -> API-Parameter; gemini-embedding-2 -> Text-Praefix (Query) bzw. schon via embed_input (Dokument)."""
    if IS_EMBED2:
        content = f"task: search result | query: {text}" if task_type == "RETRIEVAL_QUERY" else text
        resp = gclient.models.embed_content(
            model=EMBED_MODEL, contents=content,
            config=genai_types.EmbedContentConfig(output_dimensionality=EMBED_DIMS),
        )
    else:
        resp = gclient.models.embed_content(
            model=EMBED_MODEL, contents=text,
            config=genai_types.EmbedContentConfig(output_dimensionality=EMBED_DIMS, task_type=task_type),
        )
    vec = list(resp.embeddings[0].values)
    probe(len(vec) == EMBED_DIMS, "Embedding-Dimension weicht ab", got=len(vec), want=EMBED_DIMS)
    return vec
```

- [ ] **Step 7: `embed_many` modell-bewusst** (Z.434-459) — E2: parallele Einzel-Calls (Liste aggregiert!); -001: bestehender Batch. Kommentar + Funktion ersetzen:

```python
# Batch-Groesse fuer embed_many auf dem -001-Pfad (mehrere contents in EINEM Call -> eine embeddings-
# Liste). Bei gemini-embedding-2 wuerde eine Liste zu EINEM aggregierten Vektor verschmelzen
# (bugs/apis/google-gemini-api.md §J23) -> dort EIN Call pro Text, parallel.
EMBED_BATCH = int(os.getenv("SB_EMBED_BATCH", "16"))


def embed_many(texts: list[str], task_type: str) -> list[list[float]]:
    """MEHRERE Texte einbetten. gemini-embedding-2: EIN embed_content-Call je Text (eine Liste wuerde
    aggregieren), parallel ueber ThreadPool, Reihenfolge streng an den Index gebunden. -001: gebuendelte
    Batch-Requests wie bisher. Beide: Vektor-ANZAHL == Eingabe, sonst HART abbrechen (Direktive #3)."""
    if IS_EMBED2:
        def _one(t: str) -> list[float]:
            content = f"task: search result | query: {t}" if task_type == "RETRIEVAL_QUERY" else t
            r = gclient.models.embed_content(
                model=EMBED_MODEL, contents=content,
                config=genai_types.EmbedContentConfig(output_dimensionality=EMBED_DIMS),
            )
            v = list(r.embeddings[0].values)
            probe(len(v) == EMBED_DIMS, "Embedding-Dimension weicht ab", got=len(v), want=EMBED_DIMS)
            return v
        with ThreadPoolExecutor(max_workers=max(1, EMBED_PARALLEL)) as ex:
            out = list(ex.map(_one, texts))   # ex.map bewahrt die Eingabe-Reihenfolge
        if len(out) != len(texts):
            raise RuntimeError(f"Einzel-Embedding: {len(out)} Vektoren fuer {len(texts)} Texte")
        return out
    out: list[list[float]] = []
    for start in range(0, len(texts), EMBED_BATCH):
        batch = texts[start:start + EMBED_BATCH]
        resp = gclient.models.embed_content(
            model=EMBED_MODEL, contents=batch,
            config=genai_types.EmbedContentConfig(output_dimensionality=EMBED_DIMS, task_type=task_type),
        )
        vecs = [list(e.values) for e in (resp.embeddings or [])]
        if len(vecs) != len(batch):
            raise RuntimeError(f"Batch-Embedding: {len(vecs)} Vektoren fuer {len(batch)} Texte")
        for v in vecs:
            probe(len(v) == EMBED_DIMS, "Embedding-Dimension weicht ab", got=len(v), want=EMBED_DIMS)
        out.extend(vecs)
    return out
```

- [ ] **Step 8: Header-Doku + VERSION** — Header-Hinweis (Z.28-31) auf Embedding 2 aktualisieren (3072, Praefixe statt task_type). Aktuelle sichtbare VERSION per `Grep -n "VERSION = "` in app.py ermitteln, auf die nächste Minor erhöhen, echte Uhr abfragen (`Get-Date -Format 'dd.MM.yyyy, HH.mm'`), Changelog-Zeile:

```python
VERSION = "X.Y.0 (08.07.2026, HH.MM Uhr)"  # X.Y.0: UMSTIEG AUF gemini-embedding-2 (3072 Dim). task_type -> Text-Praefixe (title:|text: / task: search result|query:), embed_many nutzt bei E2 EINEN Call je Text (Liste aggregiert sonst, §J23), Chunk-Default 12000 (8192-Token-Limit). Modell-abhaengig: Fallback auf gemini-embedding-001 (1536) bleibt allein per Env moeglich. Blau/Gruen-Migration via migrate_to_embedding2.py.
```

- [ ] **Step 9: Unit-Test der Präfix-Logik schreiben** (kein Netzwerk) — `tests/embed_input_prefix_test.py`:

```python
#!/usr/bin/env python3
"""Reiner Unit-Test der modell-abhaengigen Praefix-Logik (kein Netz, kein Qdrant/Gemini).
Lauf: SB_EMBED_DIMS=3072 GEMINI_EMBED_MODEL=gemini-embedding-2 python3 tests/embed_input_prefix_test.py"""
import importlib.util, os, sys
from pathlib import Path

def _load(model):
    os.environ["GEMINI_EMBED_MODEL"] = model
    os.environ["SB_EMBED_DIMS"] = "3072" if model.startswith("gemini-embedding-2") else "1536"
    os.environ.setdefault("GEMINI_API_KEY", "x"); os.environ.setdefault("SB_API_KEY", "y"*32)
    os.environ.setdefault("QDRANT_API_KEY", "z")
    spec = importlib.util.spec_from_file_location(
        f"appmod_{model.replace('-','_').replace('.','_')}",
        str(Path(__file__).resolve().parent.parent / "brain-api" / "app.py"))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)   # _init_store faengt Fehler intern ab (init_error), Import bleibt OK
    return m

def main():
    e2 = _load("gemini-embedding-2")
    assert e2.IS_EMBED2 is True
    assert e2.embed_input("Titel A", ["Programmierung/Rules"], "Hallo") == "title: Titel A · Programmierung > Rules | text: Hallo"
    assert e2.embed_input(None, [], "Nur Text") == "title: none | text: Nur Text"

    o1 = _load("gemini-embedding-001")
    assert o1.IS_EMBED2 is False
    assert o1.embed_input("Titel A", [], "Hallo") == "[Titel: Titel A]\n\nHallo"
    print("PASS: embed_input Praefix-Logik korrekt (E2 + -001)")

if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 10: Unit-Test ausführen** — Run: `cd second-brain-server && python3 tests/embed_input_prefix_test.py`
Expected: `PASS: embed_input Praefix-Logik korrekt (E2 + -001)` (Exit 0). Bei Import-Fehlern: sicherstellen, dass `_init_store()` Init-Fehler intern in `init_error` fängt (Z.316-333) und NICHT beim Import wirft.

- [ ] **Step 11: `py_compile`** — Run: `python3 -m py_compile second-brain-server/brain-api/app.py second-brain-server/tests/embed_input_prefix_test.py`
Expected: keine Ausgabe (Exit 0).

- [ ] **Step 12: Commit** (VOR Deploy, commit-before-build)

```bash
git add second-brain-server/brain-api/app.py second-brain-server/tests/embed_input_prefix_test.py
git commit -m "#NNNN - brain-api X.Y.0: Embed-Schicht modell-bewusst (gemini-embedding-2, Praefixe, Einzel-Calls) - rueckwaertskompatibel zu -001"
git fetch origin && git rebase origin/main && git push
```

---

### Task 2: Rückwärtskompatibilität live absichern (Deploy mit ALTEM Env, noch `-001`)

Ziel: Der neue Code läuft mit der UNVERÄNDERTEN `compose.yaml` (weiter `gemini-embedding-001`/1536) — beweist, dass der `-001`-Pfad keine Regression hat, bevor migriert wird.

**Files:** keine Code-Änderung. `compose.yaml` bleibt vorerst auf `-001`/1536.

- [ ] **Step 1: Deploy auf VPS** (Code aus Task 1, Env unverändert)

```bash
scp -r second-brain-server/brain-api root@168.231.83.205:/opt/second-brain/
ssh root@168.231.83.205 "cd /opt/second-brain && docker compose up -d --build brain-api"
```

- [ ] **Step 2: Health + Regressionscheck** — Run gegen den laufenden Server (WireGuard/localhost):

```bash
ssh root@168.231.83.205 "curl -s localhost:8000/health | python3 -m json.tool | grep -E 'embed_model|embed_dims|ready'"
```
Expected: `embed_model: gemini-embedding-001`, `embed_dims: 1536`, `ready: true` (unverändert, keine Regression).

- [ ] **Step 3: Roundtrip-Regressionstest** — Run:
```bash
ssh root@168.231.83.205 "cd /opt/second-brain && BRAIN_URL=http://localhost:8000 SB_API_KEY=<key> python3 tests/large_doc_roundtrip.py"
```
Expected: PASS (Exit 0) — großes Dokument 1:1, keine stille Kürzung. Testsuche über `/search` liefert weiterhin plausible Treffer.

---

### Task 3: Blau/Grün-Migrationsskript schreiben

**Files:**
- Create: `second-brain-server/migrate_to_embedding2.py`

**Interfaces:**
- Consumes: Env `GEMINI_API_KEY`, `QDRANT_URL` (Default `http://sb-qdrant:6333`), `QDRANT_API_KEY`.
- Produces: gefüllte Sammlungen `brain__e2` + `brain_entities__e2` (size 3072), Konsole meldet `points_count` alt/neu.

- [ ] **Step 1: Skript schreiben** — spiegelt die E2-Präfix-Logik von `embed_input`/`_entity_embed_text` bewusst (einmaliges Wartungsskript; läuft, während der Server auf `-001` online bleibt):

```python
#!/usr/bin/env python3
"""Blau/Gruen-Migration: brain/brain_entities (1536, gemini-embedding-001)
-> brain__e2/brain_entities__e2 (3072, gemini-embedding-2). Der Server bleibt WAEHRENDDESSEN
online auf dem alten Stand (echtes Blau/Gruen). Cutover erst danach per compose.yaml + Redeploy.

Lauf (im Docker-Netz, wo sb-qdrant erreichbar ist), z.B.:
    docker compose run --rm -e GEMINI_API_KEY=$GEMINI_API_KEY -e QDRANT_API_KEY=$QDRANT_API_KEY \
        brain-api python3 /app/../migrate_to_embedding2.py
Env: GEMINI_API_KEY, QDRANT_URL (Default http://sb-qdrant:6333), QDRANT_API_KEY,
     SRC_COLLECTION=brain, DST_COLLECTION=brain__e2, SRC_ENTITY=brain_entities, DST_ENTITY=brain_entities__e2,
     EMBED_DIMS=3072, EMBED_PARALLEL=8. Idempotent: Ziel-Punkte werden per gleicher ID ueberschrieben.
"""
from __future__ import annotations
import os, sys
from concurrent.futures import ThreadPoolExecutor
from google import genai
from google.genai import types as gt
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct

DIMS = int(os.getenv("EMBED_DIMS", "3072"))
MODEL = os.getenv("GEMINI_EMBED_MODEL", "gemini-embedding-2")
PAR = int(os.getenv("EMBED_PARALLEL", "8"))
QURL = os.getenv("QDRANT_URL", "http://sb-qdrant:6333")
gc = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
qc = QdrantClient(url=QURL, api_key=os.getenv("QDRANT_API_KEY") or None, timeout=60.0)

def embed_doc(text: str) -> list[float]:
    r = gc.models.embed_content(model=MODEL, contents=text,
                                config=gt.EmbedContentConfig(output_dimensionality=DIMS))
    return list(r.embeddings[0].values)

def _cats(pl: dict) -> list[str]:
    cs = pl.get("categories") or ([pl["category"]] if pl.get("category") else [])
    return [c for c in cs if c and c.strip()]

def _doc_prefix(pl: dict) -> str:
    t = (pl.get("title") or "").strip()
    exp = [" > ".join(s.strip() for s in c.split("/") if s.strip()) for c in _cats(pl)]
    head = t
    if exp:
        head = f"{t} · {', '.join(exp)}" if t else ", ".join(exp)
    body = pl.get("chunk_text", pl.get("full_text", ""))
    return f"title: {head or 'none'} | text: {body}"

def _entity_prefix(pl: dict) -> str:
    name = pl.get("name", ""); parts = [f"Entität: {name}"]
    if pl.get("type"): parts.append(f"Typ: {pl['type']}")
    if pl.get("aliases"): parts.append("Auch bekannt als: " + ", ".join(pl["aliases"]))
    return f"title: {name} | text: {' | '.join(parts)}"

def _scroll(col: str):
    off = None
    while True:
        pts, off = qc.scroll(collection_name=col, limit=256, offset=off, with_payload=True, with_vectors=False)
        for p in pts: yield p
        if off is None or not pts: break

def _ensure(col: str, indexes: list[tuple[str, str]]):
    if col not in [c.name for c in qc.get_collections().collections]:
        qc.create_collection(col, vectors_config=VectorParams(size=DIMS, distance=Distance.COSINE))
    for field, schema in indexes:
        try: qc.create_payload_index(collection_name=col, field_name=field, field_schema=schema)
        except Exception: pass

def migrate(src: str, dst: str, prefix_fn, indexes):
    _ensure(dst, indexes)
    pts = list(_scroll(src))
    print(f"[{src}] {len(pts)} Punkte -> {dst} (dim {DIMS}, modell {MODEL})", flush=True)
    done = 0
    for i in range(0, len(pts), 128):
        chunk = pts[i:i+128]
        with ThreadPoolExecutor(max_workers=PAR) as ex:
            vecs = list(ex.map(lambda p: embed_doc(prefix_fn(p.payload or {})), chunk))
        qc.upsert(collection_name=dst, points=[
            PointStruct(id=p.id, vector=v, payload=p.payload) for p, v in zip(chunk, vecs)], wait=True)
        done += len(chunk); print(f"  {done}/{len(pts)}", flush=True)
    src_n = qc.count(src, exact=True).count; dst_n = qc.count(dst, exact=True).count
    print(f"VERIFY {dst}: src={src_n} dst={dst_n} -> {'OK' if src_n == dst_n else 'MISMATCH!'}", flush=True)
    return src_n == dst_n

def main():
    doc_idx = [(f, "keyword") for f in ("doc_id","title","category","categories","parent","parents","user_id","source")]
    doc_idx.append(("created_at", "datetime"))
    ent_idx = [(f, "keyword") for f in ("name_key","type","user_id")]
    ok1 = migrate(os.getenv("SRC_COLLECTION","brain"), os.getenv("DST_COLLECTION","brain__e2"), _doc_prefix, doc_idx)
    ok2 = migrate(os.getenv("SRC_ENTITY","brain_entities"), os.getenv("DST_ENTITY","brain_entities__e2"), _entity_prefix, ent_idx)
    print("MIGRATION OK" if ok1 and ok2 else "MIGRATION MISMATCH — NICHT cutovern!")
    return 0 if ok1 and ok2 else 1

if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 2: `py_compile`** — Run: `python3 -m py_compile second-brain-server/migrate_to_embedding2.py`
Expected: Exit 0.

- [ ] **Step 3: Commit** (Skript, noch keine Ausführung)

```bash
git add second-brain-server/migrate_to_embedding2.py
git commit -m "#NNNN - Blau/Gruen-Migrationsskript brain->brain__e2 (gemini-embedding-2, 3072)"
git fetch origin && git rebase origin/main && git push
```

---

### Task 4: Migration ausführen + verifizieren (Server bleibt online auf `-001`)

**Files:** keine.

- [ ] **Step 1: Skript aufs VPS bringen + starten** (Server läuft weiter auf `-001`):

```bash
scp second-brain-server/migrate_to_embedding2.py root@168.231.83.205:/opt/second-brain/
ssh root@168.231.83.205 "cd /opt/second-brain && docker compose run --rm -e GEMINI_API_KEY=$GEMINI_API_KEY -e QDRANT_API_KEY=$QDRANT_API_KEY brain-api python3 /opt/second-brain/migrate_to_embedding2.py"
```
(Falls der Pfad im Container abweicht: Skript in den brain-api-Build-Context legen oder als Volume mounten.)

- [ ] **Step 2: Verifikation** — Ausgabe muss enden mit `VERIFY brain__e2: src=N dst=N -> OK`, `VERIFY brain_entities__e2: … -> OK`, `MIGRATION OK`. Bei MISMATCH NICHT cutovern → Ursache prüfen (Rate-Limit? Fehler in `_scroll`?), Skript ist idempotent → erneut lauffähig.

- [ ] **Step 3: Blind-Testsuche gegen die neue Collection** (optional, Sicherheit) — kurzer Python-Einzeiler auf dem VPS, der `brain__e2` mit einem E2-Query-Vektor (`task: search result | query: <bekannter Begriff>`) durchsucht und prüft, dass ein erwarteter Eintrag unter den Top-Treffern ist.

---

### Task 5: Cutover (compose.yaml umstellen + Redeploy)

**Files:**
- Modify: `second-brain-server/compose.yaml`

- [ ] **Step 1: Hinweis an Frank** — kurz ansagen: „Cutover jetzt, ~10s Downtime; bitte währenddessen nichts speichern." (Delta-Risiko aus der Spec.)

- [ ] **Step 2: `compose.yaml` umstellen** (Env-Block der brain-api):

```yaml
      SB_EMBED_DIMS: "3072"                       # gemini-embedding-2 @ 3072 (MUSS zur Qdrant-Collection passen)
      GEMINI_EMBED_MODEL: gemini-embedding-2      # multimodal-faehig (Text-Phase); Fallback: gemini-embedding-001 + SB_EMBED_DIMS
      SB_COLLECTION: brain__e2
      SB_ENTITY_COLLECTION: brain_entities__e2
```

- [ ] **Step 3: Commit VOR Deploy**

```bash
git add second-brain-server/compose.yaml
git commit -m "#NNNN - Cutover: brain-api auf gemini-embedding-2 / 3072 / brain__e2 (Blau-Gruen)"
git fetch origin && git rebase origin/main && git push
```

- [ ] **Step 4: Redeploy**

```bash
scp second-brain-server/compose.yaml root@168.231.83.205:/opt/second-brain/
ssh root@168.231.83.205 "cd /opt/second-brain && docker compose up -d brain-api"
```

- [ ] **Step 5: Live-Verifikation** — Run:
```bash
ssh root@168.231.83.205 "curl -s localhost:8000/health | python3 -m json.tool | grep -E 'embed_model|embed_dims|ready|collection_status'"
```
Expected: `embed_model: gemini-embedding-2`, `embed_dims: 3072`, `ready: true`. Dann: `/list`-Anzahl == vor der Migration; eine bekannte Suche über `/search` liefert den erwarteten Eintrag; ein neuer `/store` + `/by-title`-Roundtrip 1:1.

- [ ] **Step 6: Dashboard/MCP-Sichtprüfung** — Dashboard zeigt `embed_model=gemini-embedding-2`; MCP `list_memories` liefert vollständige Liste (`ready=true`).

---

### Task 6: Aufräumen (nach Franks Bestätigung, separat)

**Files:** keine.

- [ ] **Step 1: Bestätigung abwarten** — erst wenn Frank bestätigt, dass Suche/Abruf mehrere Tage sauber laufen.
- [ ] **Step 2: Alte Sammlungen löschen** — `qc.delete_collection("brain")` + `"brain_entities"` (1536). Bis dahin bleiben sie als sofortiger Rückweg (Env zurück auf `-001`/1536/`brain` + Redeploy).

---

## Self-Review

- **Spec-Abdeckung:** Konfig (Task 1/5) ✓, Präfixe (Task 1) ✓, Batch-Umbau/Parallel/Chunk (Task 1) ✓, Blau/Grün-Migration (Task 3-5) ✓, Fallback (modell-abhängiger Code, Task 1 + compose-Kommentar) ✓, Sonden/Kommentare/VERSION (Task 1) ✓, Verifikation (Task 2/4/5) ✓, Nicht-Ziel Multimodal ausgeschlossen ✓.
- **Platzhalter:** `#NNNN` bewusst (fortlaufende Commit-Nr zur Laufzeit) und `<key>`/`X.Y.0`/`HH.MM` (Secrets/echte-Uhr zur Laufzeit) — keine offenen TODOs in der Logik.
- **Typ-Konsistenz:** `IS_EMBED2`, `EMBED_PARALLEL`, `embed`/`embed_many`-Signaturen über alle Tasks konsistent; Skript spiegelt `embed_input`-Präfix (title:|text:) exakt.
