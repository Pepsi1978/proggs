#!/usr/bin/env python3
"""Blau/Gruen-Migration: brain / brain_entities (1536, gemini-embedding-001)
-> brain__e2 / brain_entities__e2 (3072, gemini-embedding-2).

Der Server bleibt WAEHRENDDESSEN online auf dem alten Stand (echtes Blau/Gruen) — dieses Skript
schreibt NUR die neuen Ziel-Sammlungen. Der Cutover (compose.yaml auf gemini-embedding-2 / 3072 /
brain__e2 + Redeploy) passiert erst DANACH, nach erfolgreicher Verifikation.

Was 1:1 erhalten bleibt: Point-ID, gesamter Payload (full_text, chunk_text, title, category/categories,
parent/parents, created_at, doc_id, source, …). NUR der Vektor wird mit gemini-embedding-2 neu berechnet.
Die Chunk-Struktur bleibt unveraendert (kein Re-Chunking) — jeder bestehende Chunk bekommt seinen
E2-Vektor. Groessere Chunks (12000) gelten erst fuer NEUE Eintraege nach dem Cutover.

Idempotent: Ziel-Punkte werden per gleicher ID ueberschrieben; erneuter Lauf = gleiches Ergebnis.

Lauf (im Docker-Netz, wo sb-qdrant erreichbar ist), z.B.:
    docker compose run --rm \
        -e GEMINI_API_KEY="$GEMINI_API_KEY" -e QDRANT_API_KEY="$QDRANT_API_KEY" \
        brain-api python3 /opt/second-brain/migrate_to_embedding2.py
(Skript-Pfad im Container ggf. per Volume/Build-Context anpassen.)

Env:
    GEMINI_API_KEY (Pflicht) · QDRANT_URL (Default http://sb-qdrant:6333) · QDRANT_API_KEY
    GEMINI_EMBED_MODEL (Default gemini-embedding-2) · EMBED_DIMS (Default 3072) · EMBED_PARALLEL (Default 8)
    SRC_COLLECTION=brain · DST_COLLECTION=brain__e2 · SRC_ENTITY=brain_entities · DST_ENTITY=brain_entities__e2
Exit 0 = alle Sammlungen sauber (src==dst), 1 = Mismatch (NICHT cutovern).
"""
from __future__ import annotations

import os
import sys
from concurrent.futures import ThreadPoolExecutor

from google import genai
from google.genai import types as gt
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, PointStruct, VectorParams

DIMS = int(os.getenv("EMBED_DIMS", "3072"))
MODEL = os.getenv("GEMINI_EMBED_MODEL", "gemini-embedding-2")
PAR = int(os.getenv("EMBED_PARALLEL", "8"))
QURL = os.getenv("QDRANT_URL", "http://sb-qdrant:6333")

gc = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
qc = QdrantClient(url=QURL, api_key=os.getenv("QDRANT_API_KEY") or None, timeout=60.0)


def embed_doc(text: str) -> list[float]:
    """Ein Dokument-Praefix-Text -> ein E2-Vektor (kein task_type; Praefix steckt schon im Text)."""
    r = gc.models.embed_content(model=MODEL, contents=text,
                                config=gt.EmbedContentConfig(output_dimensionality=DIMS))
    return list(r.embeddings[0].values)


def _cats(pl: dict) -> list[str]:
    cs = pl.get("categories") or ([pl["category"]] if pl.get("category") else [])
    return [c for c in cs if c and c.strip()]


def _doc_prefix(pl: dict) -> str:
    """Spiegelt embed_input() (E2-Zweig) aus brain-api/app.py: 'title: {Titel · Kategorien} | text: {chunk}'."""
    t = (pl.get("title") or "").strip()
    exp = [" > ".join(s.strip() for s in c.split("/") if s.strip()) for c in _cats(pl)]
    head = t
    if exp:
        head = f"{t} · {', '.join(exp)}" if t else ", ".join(exp)
    body = pl.get("chunk_text", pl.get("full_text", ""))
    return f"title: {head or 'none'} | text: {body}"


def _entity_prefix(pl: dict) -> str:
    """Spiegelt _entity_embed_text() (E2-Zweig): 'title: {Name} | text: Entität: … | Typ: … | …'."""
    name = pl.get("name", "")
    parts = [f"Entität: {name}"]
    if pl.get("type"):
        parts.append(f"Typ: {pl['type']}")
    if pl.get("aliases"):
        parts.append("Auch bekannt als: " + ", ".join(pl["aliases"]))
    return f"title: {name} | text: {' | '.join(parts)}"


def _scroll(col: str):
    off = None
    while True:
        pts, off = qc.scroll(collection_name=col, limit=256, offset=off,
                             with_payload=True, with_vectors=False)
        for p in pts:
            yield p
        if off is None or not pts:
            break


def _ensure(col: str, indexes: list[tuple[str, str]]) -> None:
    if col not in [c.name for c in qc.get_collections().collections]:
        qc.create_collection(collection_name=col,
                             vectors_config=VectorParams(size=DIMS, distance=Distance.COSINE))
    for field, schema in indexes:
        try:
            qc.create_payload_index(collection_name=col, field_name=field, field_schema=schema)
        except Exception:  # noqa: BLE001 — Index existiert bereits / datetime nicht verfuegbar: unkritisch
            pass


def migrate(src: str, dst: str, prefix_fn, indexes: list[tuple[str, str]]) -> bool:
    _ensure(dst, indexes)
    pts = list(_scroll(src))
    print(f"[{src}] {len(pts)} Punkte -> {dst} (dim {DIMS}, Modell {MODEL})", flush=True)
    done = 0
    for i in range(0, len(pts), 128):
        chunk = pts[i:i + 128]
        with ThreadPoolExecutor(max_workers=max(1, PAR)) as ex:
            vecs = list(ex.map(lambda p: embed_doc(prefix_fn(p.payload or {})), chunk))
        qc.upsert(collection_name=dst, wait=True, points=[
            PointStruct(id=p.id, vector=v, payload=p.payload) for p, v in zip(chunk, vecs)])
        done += len(chunk)
        print(f"  {done}/{len(pts)}", flush=True)
    src_n = qc.count(collection_name=src, exact=True).count
    dst_n = qc.count(collection_name=dst, exact=True).count
    ok = src_n == dst_n
    print(f"VERIFY {dst}: src={src_n} dst={dst_n} -> {'OK' if ok else 'MISMATCH!'}", flush=True)
    return ok


def main() -> int:
    doc_idx: list[tuple[str, str]] = [
        (f, "keyword") for f in
        ("doc_id", "title", "category", "categories", "parent", "parents", "user_id", "source")]
    doc_idx.append(("created_at", "datetime"))
    ent_idx: list[tuple[str, str]] = [(f, "keyword") for f in ("name_key", "type", "user_id")]

    ok1 = migrate(os.getenv("SRC_COLLECTION", "brain"),
                  os.getenv("DST_COLLECTION", "brain__e2"), _doc_prefix, doc_idx)
    ok2 = migrate(os.getenv("SRC_ENTITY", "brain_entities"),
                  os.getenv("DST_ENTITY", "brain_entities__e2"), _entity_prefix, ent_idx)
    print("MIGRATION OK — bereit fuer Cutover" if ok1 and ok2
          else "MIGRATION MISMATCH — NICHT cutovern, Ursache pruefen (Skript ist idempotent, erneut lauffaehig)")
    return 0 if (ok1 and ok2) else 1


if __name__ == "__main__":
    sys.exit(main())
