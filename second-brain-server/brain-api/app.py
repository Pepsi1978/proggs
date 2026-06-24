"""
brain-api — Franks "zweites Gehirn": wortwoertlicher 1:1-Dokument-Speicher (Schicht 1, "stummer Speicher").

Ablauf:  Text 1:1 rein -> Gemini-Embedding (NUR zum Wiederfinden) -> Qdrant (Vektor + voller Text als Payload)
         Text 1:1 raus. KEINE KI-Bearbeitung, kein Faktenextrahieren, kein Vergleichen beim Speichern.
         (Das war mem0 -> es dichtete/halluzinierte -> komplett entfernt 2026-06-23.) Das "Verstehen/
         Zusammenfassen/Vergleichen" kommt SPAETER in einer separaten Agenten-Schicht, die nur liest und
         nie eigenmaechtig schreibt.

Datenmodell:
  Eintrag = Text (1:1, PFLICHT) + Titel (optional, = Schluessel) + Kategorie (optional).
  - Titel ist eindeutig je Nutzer: gleicher Titel -> alter Eintrag wird ERSETZT ("aktualisiere Direktive 1").
  - Ohne Titel -> neuer Eintrag mit eigener ID (nichts wird ueberschrieben).
  - Lange Texte werden fuer die SUCHE in Chunks zerlegt (Gemini-Embedding-Limit), aber der VOLLE Text
    liegt 1:1 im Payload JEDES Chunks -> exakter Abruf liefert immer das ganze Dokument unveraendert.

Drei Abruf-Wege (alle liefern 1:1):
  GET  /by-title?title=...        exakt per Titel -> ganzes Dokument
  GET  /by-category?category=...  alle Eintraege einer Kategorie
  POST /search   {query,limit}    semantische Suche -> Top-N (dedupliziert auf Dokument-Ebene)
  GET  /list                      alle Eintraege auflisten (Titel/Kategorie/Groesse)
  DELETE /by-title?title=...      Eintrag loeschen

Sicherheit: laeuft nur intern (compose bindet an die WireGuard-IP), Bearer-Token-Auth, nicht als root.
Observability-First: strukturiertes JSON-Logging (stdout + rotierende Datei), globaler Fehler-Faenger,
                     Logik-Sonden (probe) + Intent-Checkpoints (checkpoint, beweisen das 1:1-Prinzip live).

Bekannte Fallen beachtet (Almanach): Qdrant api_key erzwingt sonst TLS -> explizite http://-URL
(bugs/server/qdrant.md §4); Gemini-Embedding defaultet auf 768 -> output_dimensionality EXPLIZIT 1536
(bugs/server/mem0.md §2, best-practices/second-brain/memory-backends.md §0); JSON-Log ensure_ascii=False
+ FileHandler encoding=utf-8 (bugs/claude-tooling/python-windows.md §1.1/§1.5).

Plan/Doku: best-practices/second-brain/UMSETZUNGSPLAN.md
"""
from __future__ import annotations

import hashlib
import json
import logging
import os
import threading
import time
import traceback
import uuid
from datetime import date, datetime, timezone
from logging.handlers import RotatingFileHandler
from typing import Any

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

VERSION = "1.5.0"  # 1.5.0: Eintraege nach Aktualitaet sortiert — /by-category + /list geben das NEUESTE zuerst zurueck (Frank-Wunsch 2026-06-24: Kategorie-Ansicht war unsortiert), via _sort_recent (updated_at, sonst created_at); created_at jetzt in beiden Listen-Antworten. 1.4.0: Papierkorb (Soft-Delete) — DELETE /entry verschiebt jetzt in den Papierkorb (trash.json, persistentes /app/data-Volume) statt endgueltig zu loeschen; GET /trash (neueste zuerst), PUT /trash (Text im Papierkorb editieren, ohne Re-Embed), POST /trash/restore (frisch embedden + unter doc_id zurueck ins Gehirn, created_at erhalten). 1.3.0: DELETE /entry — Eintrag dauerhaft per doc_id loeschen (alle Chunks), fuer den Papierkorb-Button im Dashboard-Drawer (Frank-Wunsch). 1.2.0: PUT /entry — Eintrag per doc_id 1:1 ersetzen (alte Vektoren loeschen, neuen Text frisch embedden, Titel/Kategorie/created_at erhalten); doc_id jetzt in allen Listen-/Abruf-Antworten (Frontend-Editor). 1.1.0: /search um Payload-Filter (Kategorie + Datum/Bereich). 1.0.0: mem0 raus -> direkter 1:1-Speicher.

# ---------------------------------------------------------------------------
# Konfiguration (alles aus Umgebungsvariablen — Secrets nie im Code)
# ---------------------------------------------------------------------------
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY", "")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY", "")
SB_API_KEY = os.getenv("SB_API_KEY", "")
QDRANT_HOST = os.getenv("QDRANT_HOST", "sb-qdrant")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))
# Explizite http-URL: mit gesetztem api_key wuerde qdrant-client sonst https=True annehmen und TLS
# gegen das Klartext-HTTP-Qdrant sprechen -> [SSL: WRONG_VERSION_NUMBER]. (bugs/server/qdrant.md §4)
QDRANT_URL = os.getenv("QDRANT_URL", f"http://{QDRANT_HOST}:{QDRANT_PORT}")
COLLECTION = os.getenv("SB_COLLECTION", "brain")
EMBED_MODEL = os.getenv("GEMINI_EMBED_MODEL", "gemini-embedding-001")
EMBED_DIMS = int(os.getenv("SB_EMBED_DIMS", "1536"))
# Chunking fuer die SUCHE (gemini-embedding-001: ~2048 Token Input-Limit). Konservativ in Zeichen.
CHUNK_CHARS = int(os.getenv("SB_CHUNK_CHARS", "4000"))
CHUNK_OVERLAP = int(os.getenv("SB_CHUNK_OVERLAP", "200"))
MAX_EMBED_CALLS_PER_DAY = int(os.getenv("SB_MAX_EMBED_CALLS_PER_DAY", "20000"))  # Defense-in-Depth-Cap
LOG_PATH = os.getenv("SB_LOG_PATH", "/app/logs/brain-api.jsonl")
LOG_LEVEL = os.getenv("SB_LOG_LEVEL", "INFO").upper()
# Papierkorb (Soft-Delete): geloeschte Eintraege landen als JSON 1:1 hier (persistentes Volume),
# damit sie im Dashboard wiederhergestellt/editiert werden koennen. KEINE Vektoren — reiner Textspeicher.
DATA_DIR = os.getenv("SB_DATA_DIR", "/app/data")
TRASH_PATH = os.getenv("SB_TRASH_PATH", os.path.join(DATA_DIR, "trash.json"))

# ---------------------------------------------------------------------------
# Strukturiertes JSON-Logging (stdout + rotierende Datei, beide UTF-8)
# ---------------------------------------------------------------------------
class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        entry = {
            "ts": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(record.created)),
            "level": record.levelname,
            "module": "brain-api",
            "fn": record.funcName,
            "msg": record.getMessage(),
        }
        if isinstance(getattr(record, "ctx", None), dict):
            entry["ctx"] = record.ctx
        if record.exc_info:
            entry["trace"] = "".join(traceback.format_exception(*record.exc_info))
        return json.dumps(entry, ensure_ascii=False)


log = logging.getLogger("brain-api")
log.setLevel(getattr(logging, LOG_LEVEL, logging.INFO))
_stdout = logging.StreamHandler()
_stdout.setFormatter(JsonFormatter())
log.addHandler(_stdout)
try:
    os.makedirs(os.path.dirname(LOG_PATH), exist_ok=True)
    _file = RotatingFileHandler(LOG_PATH, maxBytes=5_000_000, backupCount=5, encoding="utf-8")
    _file.setFormatter(JsonFormatter())
    log.addHandler(_file)
except OSError as e:  # Datei-Logging optional — stdout bleibt
    log.warning("Datei-Log nicht moeglich, nur stdout", extra={"ctx": {"err": str(e)}})


def _log(level: int, msg: str, **ctx: Any) -> None:
    log.log(level, msg, extra={"ctx": ctx} if ctx else None)


def probe(condition: bool, msg: str, **ctx: Any) -> bool:
    """Logik-Sonde: prueft eine Annahme, loggt WARN bei Verletzung, crasht NIE."""
    if not condition:
        _log(logging.WARNING, f"PROBE verletzt: {msg}", **ctx)
    return condition


def checkpoint(step: str, intent: str, ok: bool, **ctx: Any) -> None:
    """Intent-Checkpoint (erwartet vs. tatsaechlich) — eigener Kanal kind=CHECKPOINT.
    Beweist live, dass die Logik so umgesetzt ist wie gemeint (hier v.a. das 1:1-Prinzip)."""
    log.log(
        logging.INFO if ok else logging.WARNING,
        f"CHECKPOINT {step}",
        extra={"ctx": {"kind": "CHECKPOINT", "step": step, "intent": intent, "ok": ok, **ctx}},
    )


def iso_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


# ---------------------------------------------------------------------------
# Clients: Qdrant + Gemini-Embedding (Init-Fehler verschlucken wir NICHT)
# ---------------------------------------------------------------------------
qc = None            # Qdrant-Client
gclient = None       # Gemini-Client
init_error: str | None = None

# Startbedingungen pruefen (Sonden) — Verletzungen sind Konfig-Fehler, nicht still
probe(bool(GEMINI_API_KEY), "GEMINI_API_KEY fehlt")
probe(bool(SB_API_KEY) and len(SB_API_KEY) >= 32, "SB_API_KEY fehlt/zu kurz")
probe(bool(QDRANT_API_KEY), "QDRANT_API_KEY fehlt")
probe(EMBED_DIMS in (768, 1536, 3072), "EMBED_DIMS unerwartet (gemini-embedding-001: 768/1536/3072)", dims=EMBED_DIMS)

# Optionaler Datetime-Range-Filter (qdrant-client >= ~1.8). Fehlt er -> Datum-Filter in /search
# degradiert sauber auf einen Python-Nachfilter (Funktionserhalt, Direktive #3).
try:
    from qdrant_client.models import DatetimeRange
    HAS_DATETIME_RANGE = True
except Exception:  # noqa: BLE001
    DatetimeRange = None  # type: ignore
    HAS_DATETIME_RANGE = False

try:
    from google import genai
    from google.genai import types as genai_types
    from qdrant_client import QdrantClient
    from qdrant_client.models import (
        Distance,
        FieldCondition,
        Filter,
        MatchValue,
        PointStruct,
        VectorParams,
    )

    gclient = genai.Client(api_key=GEMINI_API_KEY)
    # url=http://... -> kein TLS (api_key wuerde sonst https=True erzwingen). Almanach qdrant.md §4.
    qc = QdrantClient(url=QDRANT_URL, api_key=QDRANT_API_KEY or None, timeout=30.0)

    # Collection sicherstellen (Cosine: Qdrant normalisiert intern -> keine Eigen-Normalisierung noetig)
    _existing = [c.name for c in qc.get_collections().collections]
    if COLLECTION not in _existing:
        qc.create_collection(
            collection_name=COLLECTION,
            vectors_config=VectorParams(size=EMBED_DIMS, distance=Distance.COSINE),
        )
        _log(logging.INFO, "Collection angelegt", collection=COLLECTION, dims=EMBED_DIMS)
    # Payload-Indizes fuer schnelle/zuverlaessige Filter (idempotent — Fehler ignorieren)
    for _field in ("doc_id", "title", "category", "user_id"):
        try:
            qc.create_payload_index(collection_name=COLLECTION, field_name=_field, field_schema="keyword")
        except Exception:  # noqa: BLE001 — Index existiert bereits / nicht kritisch
            pass
    # Datetime-Index auf created_at -> native, schnelle Datums-Filter in /search ("letzten Monat").
    # Idempotent; arbeitet auf den bestehenden RFC3339-Werten (kein Backfill noetig).
    if HAS_DATETIME_RANGE:
        try:
            qc.create_payload_index(collection_name=COLLECTION, field_name="created_at", field_schema="datetime")
        except Exception:  # noqa: BLE001 — Index existiert bereits / nicht kritisch
            pass
    _log(logging.INFO, "Speicher initialisiert", embed_model=EMBED_MODEL, dims=EMBED_DIMS,
         collection=COLLECTION, qdrant_url=QDRANT_URL)
except Exception as e:  # noqa: BLE001 — Init-Fehler vollstaendig festhalten, nicht still
    init_error = f"{type(e).__name__}: {e}"
    log.error("Speicher-Init fehlgeschlagen", exc_info=True)

# Startup-Banner (Observability-First: Log-Pfad + Version EINMAL ausgeben)
_log(logging.INFO, "brain-api startet", version=VERSION, log_path=LOG_PATH)

# ---------------------------------------------------------------------------
# Defense-in-Depth-Kostenbremse (harter Cap liegt beim Google-AI-Studio-Budget)
# ---------------------------------------------------------------------------
_embed_calls = {"day": str(date.today()), "count": 0}


def _guard_embed_budget(n: int = 1) -> None:
    today = str(date.today())
    if _embed_calls["day"] != today:
        _embed_calls["day"], _embed_calls["count"] = today, 0
    _embed_calls["count"] += n
    if _embed_calls["count"] > MAX_EMBED_CALLS_PER_DAY:
        _log(logging.ERROR, "Embedding-Tages-Cap erreicht", count=_embed_calls["count"], cap=MAX_EMBED_CALLS_PER_DAY)
        raise HTTPException(status_code=429, detail="Taegliches Embedding-Limit erreicht (Sicherheits-Cap)")


# ---------------------------------------------------------------------------
# Kern-Helfer: Chunking, Embedding, IDs
# ---------------------------------------------------------------------------
def chunk_text(text: str) -> list[str]:
    """Zerlegt langen Text fuer die SUCHE in ueberlappende Stuecke. Beruehrt den
    1:1-Volltext NICHT — der wird separat im Payload gehalten."""
    if len(text) <= CHUNK_CHARS:
        return [text]
    out: list[str] = []
    start = 0
    step = max(1, CHUNK_CHARS - CHUNK_OVERLAP)
    while start < len(text):
        out.append(text[start:start + CHUNK_CHARS])
        start += step
    return out


def embed(text: str, task_type: str) -> list[float]:
    """Text -> Vektor via Gemini. task_type='RETRIEVAL_DOCUMENT' beim Speichern,
    'RETRIEVAL_QUERY' beim Suchen (asymmetrische Suche = bessere Treffer)."""
    resp = gclient.models.embed_content(
        model=EMBED_MODEL,
        contents=text,
        config=genai_types.EmbedContentConfig(output_dimensionality=EMBED_DIMS, task_type=task_type),
    )
    vec = list(resp.embeddings[0].values)
    probe(len(vec) == EMBED_DIMS, "Embedding-Dimension weicht ab", got=len(vec), want=EMBED_DIMS)
    return vec


def make_doc_id(user_id: str, title: str | None) -> str:
    """Titel -> deterministische doc_id (gleicher Titel = gleiche ID = ueberschreiben).
    Ohne Titel -> frische UUID (neuer Eintrag)."""
    if title and title.strip():
        h = hashlib.sha1(f"{user_id}::{title.strip().lower()}".encode("utf-8")).hexdigest()[:24]
        return f"t_{h}"
    return f"d_{uuid.uuid4().hex}"


def point_id(doc_id: str, idx: int) -> str:
    """Deterministische Qdrant-Point-ID (UUID) je Chunk."""
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"sb/{doc_id}/{idx}"))


def _delete_doc(doc_id: str) -> None:
    qc.delete(collection_name=COLLECTION,
              points_selector=Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]))


# --- Papierkorb (Soft-Delete): thread-sicher + atomar (Handler laufen im Threadpool, fastapi §1) -----
_trash_lock = threading.Lock()


def _trash_load() -> list[dict]:
    """Liest die Papierkorb-Liste. Fehlt die Datei/ist kaputt -> leere Liste (nie crashen)."""
    try:
        with open(TRASH_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data if isinstance(data, list) else []
    except FileNotFoundError:
        return []
    except Exception as e:  # noqa: BLE001 — kaputter Papierkorb darf das Gehirn nicht lahmlegen
        log.warning("Papierkorb nicht lesbar: %s", e)
        return []


def _trash_save(items: list[dict]) -> None:
    """Atomar schreiben (temp + os.replace) mit UTF-8 — kein abgeschnittener Papierkorb bei Crash."""
    os.makedirs(DATA_DIR, exist_ok=True)
    tmp = f"{TRASH_PATH}.tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=2)
    os.replace(tmp, TRASH_PATH)


def _scroll(flt: "Filter | None", limit: int = 10000) -> list:
    points, _next = qc.scroll(collection_name=COLLECTION, scroll_filter=flt, limit=limit, with_payload=True)
    return points


# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------
def require_auth(authorization: str = Header(default="")) -> None:
    expected = f"Bearer {SB_API_KEY}"
    if not SB_API_KEY or authorization != expected:
        raise HTTPException(status_code=401, detail="Unauthorized")


def _require_store() -> None:
    if qc is None or gclient is None:
        raise HTTPException(status_code=503, detail=f"Speicher nicht initialisiert: {init_error}")


# ---------------------------------------------------------------------------
# Request-Modelle
# ---------------------------------------------------------------------------
class StoreReq(BaseModel):
    text: str = Field(..., min_length=1, description="Der Text, der WORTWOERTLICH 1:1 gespeichert wird")
    title: str | None = Field(default=None, description="Optionaler Titel = Schluessel (gleicher Titel ersetzt)")
    category: str | None = Field(default=None, description="Optionale Kategorie (z.B. 'mentals', 'direktiven')")
    user_id: str = Field(default="frank", description="Besitzer (aktuell immer 'frank')")


class SearchReq(BaseModel):
    query: str = Field(..., min_length=1)
    user_id: str = Field(default="frank")
    limit: int = Field(default=5, ge=1, le=50)
    # Optionale Payload-Filter: erst eingrenzen, DANN semantisch suchen (Franks "war letzten Monat angeln").
    category: str | None = Field(default=None, description="Nur in dieser Kategorie suchen")
    date: str | None = Field(default=None, description="Nur Eintraege dieses Tages (YYYY-MM-DD)")
    date_from: str | None = Field(default=None, description="Ab diesem Tag (YYYY-MM-DD, inklusive)")
    date_to: str | None = Field(default=None, description="Bis zu diesem Tag (YYYY-MM-DD, inklusive)")


class UpdateReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des zu ersetzenden Eintrags (aus /list, /by-category, /search)")
    text: str = Field(..., min_length=1, max_length=200_000, description="Neuer 1:1-Text — ersetzt den alten Vektor komplett (max_length gegen OOM, fastapi §8)")
    user_id: str = Field(default="frank")


class TrashEditReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des Papierkorb-Eintrags")
    text: str = Field(..., min_length=1, max_length=200_000, description="Neuer Text IM Papierkorb (kein Re-Embed; gilt erst bei Wiederherstellung)")
    user_id: str = Field(default="frank")


class RestoreReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des wiederherzustellenden Papierkorb-Eintrags")
    user_id: str = Field(default="frank")


# ---------------------------------------------------------------------------
# App
# ---------------------------------------------------------------------------
app = FastAPI(title="Second Brain — brain-api (1:1-Speicher)", version=VERSION)


@app.exception_handler(Exception)
async def unhandled(request: Request, exc: Exception) -> JSONResponse:
    """Globaler Fehler-Faenger: nichts stirbt still — voller Kontext ins Log."""
    log.error("Unbehandelte Ausnahme", exc_info=True, extra={"ctx": {"path": str(request.url.path)}})
    return JSONResponse(status_code=500, content={"error": type(exc).__name__, "detail": str(exc)})


@app.get("/health")
def health() -> dict:
    qdrant_ok, count, detail = False, None, None
    try:
        if qc is not None:
            info = qc.get_collection(COLLECTION)
            qdrant_ok = True
            count = info.points_count
    except Exception as e:  # noqa: BLE001
        detail = f"{type(e).__name__}: {e}"
    status = "ok" if (qc is not None and gclient is not None and qdrant_ok) else "degraded"
    return {
        "status": status,
        "version": VERSION,
        "init_error": init_error,
        "qdrant": "reachable" if qdrant_ok else "unreachable",
        "qdrant_detail": detail,
        "collection": COLLECTION,
        "points": count,
        "embed_model": EMBED_MODEL,
        "embed_dims": EMBED_DIMS,
        "embed_calls_today": _embed_calls["count"],
    }


@app.post("/store", dependencies=[Depends(require_auth)])
def store(req: StoreReq) -> dict:
    """Speichert Text WORTWOERTLICH 1:1. Mit Titel: ersetzt einen vorhandenen Eintrag gleichen Titels."""
    _require_store()
    doc_id = make_doc_id(req.user_id, req.title)
    now = iso_now()
    created_at = now
    replaced = False
    if req.title and req.title.strip():
        existing = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]), limit=1)
        replaced = bool(existing)
        if replaced:
            created_at = existing[0].payload.get("created_at", now)  # Erstellungsdatum erhalten, nur updated_at neu
            _delete_doc(doc_id)  # gleicher Titel -> alte Version komplett raus, dann neu

    chunks = chunk_text(req.text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(ch, "RETRIEVAL_DOCUMENT")
        points.append(PointStruct(id=point_id(doc_id, i), vector=vec, payload={
            "doc_id": doc_id,
            "user_id": req.user_id,
            "title": (req.title or "").strip(),
            "category": (req.category or "").strip(),
            "chunk_index": i,
            "chunk_count": len(chunks),
            "chunk_text": ch,
            "full_text": req.text,   # 1:1 — exakt die Eingabe, in jedem Chunk gehalten
            "created_at": created_at,
            "updated_at": now,
        }))
    qc.upsert(collection_name=COLLECTION, points=points)

    # Intent-Checkpoint: beweist, dass der gespeicherte Volltext EXAKT die Eingabe ist (1:1)
    stored_ok = bool(points) and points[0].payload["full_text"] == req.text
    checkpoint("store", "Text wird WORTWOERTLICH 1:1 gespeichert (keine KI-Bearbeitung)", ok=stored_ok,
               title=req.title or None, category=req.category or None, chunks=len(chunks),
               chars=len(req.text), replaced=replaced, ms=int((time.time() - t0) * 1000))
    return {"ok": True, "doc_id": doc_id, "title": req.title or None,
            "category": req.category or None, "chunks": len(chunks), "chars": len(req.text),
            "replaced": replaced}


def _sort_recent(items) -> list[dict]:
    """Eintraege nach Aktualitaet sortieren — das NEUESTE zuerst (Frank-Wunsch 2026-06-24).
    Schluessel: updated_at, sonst created_at. ISO-8601-Strings sortieren lexikalisch = chronologisch;
    fehlende Daten ('') landen hinten."""
    return sorted(items, key=lambda it: (it.get("updated_at") or it.get("created_at") or ""), reverse=True)


@app.get("/by-title", dependencies=[Depends(require_auth)])
def by_title(title: str, user_id: str = "frank") -> dict:
    """Exakter Abruf per Titel -> liefert das GANZE Dokument 1:1."""
    _require_store()
    doc_id = make_doc_id(user_id, title)
    points = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]), limit=1000)
    if not points:
        checkpoint("by_title", "Abruf per Titel", ok=False, found=False, title=title)
        return {"ok": True, "found": False, "title": title, "text": None}
    full = points[0].payload.get("full_text", "")
    checkpoint("by_title", "Abruf per Titel gibt das GANZE Dokument 1:1 zurueck", ok=bool(full),
               title=title, chars=len(full))
    return {"ok": True, "found": True, "title": title, "doc_id": doc_id,
            "category": points[0].payload.get("category") or None,
            "updated_at": points[0].payload.get("updated_at"), "text": full}


@app.get("/by-category", dependencies=[Depends(require_auth)])
def by_category(category: str, user_id: str = "frank") -> dict:
    """Alle Eintraege einer Kategorie (auf Dokument-Ebene dedupliziert), jeweils 1:1."""
    _require_store()
    points = _scroll(Filter(must=[
        FieldCondition(key="category", match=MatchValue(value=category.strip())),
        FieldCondition(key="user_id", match=MatchValue(value=user_id)),
    ]))
    seen: dict[str, dict] = {}
    for p in points:
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "text": p.payload.get("full_text", ""), "category": p.payload.get("category") or None,
                         "created_at": p.payload.get("created_at"),
                         "updated_at": p.payload.get("updated_at")}
    items = _sort_recent(seen.values())   # Neueste zuerst (nach Aktualitaet)
    return {"ok": True, "category": category, "count": len(items), "items": items}


@app.get("/by-date", dependencies=[Depends(require_auth)])
def by_date(date: str, user_id: str = "frank") -> dict:
    """Alle Eintraege, die an einem bestimmten Tag gespeichert wurden (Datum YYYY-MM-DD,
    Praefix-Filter auf created_at). Auf Dokument-Ebene dedupliziert."""
    _require_store()
    points = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]))
    seen: dict[str, dict] = {}
    for p in points:
        created = p.payload.get("created_at", "")
        if not created.startswith(date):
            continue
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "category": p.payload.get("category") or None,
                         "created_at": created, "updated_at": p.payload.get("updated_at"),
                         "text": p.payload.get("full_text", "")}
    items = list(seen.values())
    checkpoint("by_date", "Abruf nach Speicherdatum", ok=True, date=date, hits=len(items))
    return {"ok": True, "date": date, "count": len(items), "items": items}


def _date_bounds(req: SearchReq) -> tuple[str | None, str | None]:
    """Leitet (gte, lte) als RFC3339-Tagesgrenzen aus date / date_from / date_to ab (oder None, None).
    'date' (einzelner Tag) hat Vorrang; sonst date_from..date_to als Bereich."""
    if req.date and req.date.strip():
        d = req.date.strip()
        return f"{d}T00:00:00Z", f"{d}T23:59:59Z"
    gte = f"{req.date_from.strip()}T00:00:00Z" if req.date_from and req.date_from.strip() else None
    lte = f"{req.date_to.strip()}T23:59:59Z" if req.date_to and req.date_to.strip() else None
    return gte, lte


@app.post("/search", dependencies=[Depends(require_auth)])
def search(req: SearchReq) -> dict:
    """Semantische Suche -> Top-N (auf Dokument-Ebene dedupliziert), je voller 1:1-Text + Treffer-Abschnitt.
    Optional erst per Payload-Filter eingrenzen (Kategorie / Speicherdatum), DANN semantisch darin suchen
    (Franks "war ich letzten Monat angeln"): der Filter schraenkt den Suchraum ein, BEVOR der Vektor-
    Vergleich laeuft — nicht hinterher. Ohne Filter exakt wie bisher."""
    _require_store()
    _guard_embed_budget(1)
    t0 = time.time()
    qvec = embed(req.query, "RETRIEVAL_QUERY")

    # Payload-Filter aufbauen (immer user_id; optional Kategorie + Datum)
    must = [FieldCondition(key="user_id", match=MatchValue(value=req.user_id))]
    if req.category and req.category.strip():
        must.append(FieldCondition(key="category", match=MatchValue(value=req.category.strip())))
    gte, lte = _date_bounds(req)
    py_date_filter = False  # Fallback, falls DatetimeRange in der Client-Version fehlt (Funktionserhalt)
    if gte or lte:
        if HAS_DATETIME_RANGE:
            must.append(FieldCondition(key="created_at", range=DatetimeRange(gte=gte, lte=lte)))
        else:
            py_date_filter = True

    # Bei Python-Datums-Nachfilter mehr Kandidaten holen, damit datumspassende Treffer nicht durchrutschen.
    overfetch = req.limit * (20 if py_date_filter else 4)
    raw = qc.query_points(
        collection_name=COLLECTION, query=qvec,
        query_filter=Filter(must=must),
        limit=overfetch, with_payload=True,
    ).points

    best: dict[str, dict] = {}
    for h in raw:
        if py_date_filter:
            created = (h.payload.get("created_at") or "")[:10]  # ISO sortiert lexikalisch = chronologisch
            if (gte and created < gte[:10]) or (lte and created > lte[:10]):
                continue
        did = h.payload.get("doc_id")
        if did not in best or h.score > best[did]["score"]:
            best[did] = {"doc_id": did, "title": h.payload.get("title") or None,
                         "category": h.payload.get("category") or None,
                         "score": float(h.score), "match": h.payload.get("chunk_text", ""),
                         "text": h.payload.get("full_text", "")}
    items = sorted(best.values(), key=lambda x: x["score"], reverse=True)[:req.limit]

    applied = {"category": req.category or None, "date": req.date or None,
               "date_from": req.date_from or None, "date_to": req.date_to or None}
    has_filter = any(applied.values())
    checkpoint("search", "Erst Payload-Filter (Kategorie/Datum) eingrenzen, DANN semantisch suchen",
               ok=isinstance(items, list), query_len=len(req.query), hits=len(items),
               filters=applied if has_filter else None,
               native_date=bool((gte or lte) and not py_date_filter), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "count": len(items), "items": items, "filters": applied if has_filter else None}


@app.get("/list", dependencies=[Depends(require_auth)])
def list_docs(user_id: str = "frank", limit: int = 500) -> dict:
    """Listet die gespeicherten Eintraege (Titel/Kategorie/Groesse) — ohne die Volltexte (kompakt)."""
    _require_store()
    points = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]))
    seen: dict[str, dict] = {}
    for p in points:
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "category": p.payload.get("category") or None,
                         "chars": len(p.payload.get("full_text", "")),
                         "created_at": p.payload.get("created_at"),
                         "updated_at": p.payload.get("updated_at")}
    items = _sort_recent(seen.values())[:limit]   # Neueste zuerst (nach Aktualitaet)
    return {"ok": True, "count": len(seen), "items": items}


@app.delete("/by-title", dependencies=[Depends(require_auth)])
def forget(title: str, user_id: str = "frank") -> dict:
    """Loescht den Eintrag mit diesem Titel komplett."""
    _require_store()
    doc_id = make_doc_id(user_id, title)
    existing = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]), limit=1)
    if not existing:
        return {"ok": True, "deleted": False, "title": title}
    _delete_doc(doc_id)
    checkpoint("forget", "Eintrag per Titel geloescht", ok=True, title=title)
    return {"ok": True, "deleted": True, "title": title}


@app.delete("/entry", dependencies=[Depends(require_auth)])
def delete_entry(doc_id: str, user_id: str = "frank") -> dict:
    """Soft-Delete: verschiebt den Eintrag in den Papierkorb (trash.json — 1:1-Text + Metadaten +
    deleted_at) und entfernt ihn aus dem aktiven Gehirn (Qdrant). So bleibt er im Dashboard
    wiederherstellbar/editierbar. Sync def -> Threadpool (fastapi §1). Idempotent: nicht vorhanden
    -> deleted:false (kein 404, damit der Button nie ins Leere laeuft)."""
    _require_store()
    existing = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]), limit=1)
    if not existing:
        return {"ok": True, "deleted": False, "doc_id": doc_id}
    pl = existing[0].payload
    entry = {
        "doc_id": doc_id,
        "user_id": pl.get("user_id") or user_id,
        "title": (pl.get("title") or "").strip(),
        "category": (pl.get("category") or "").strip(),
        "text": pl.get("full_text", ""),
        "created_at": pl.get("created_at") or iso_now(),
        "deleted_at": iso_now(),
    }
    with _trash_lock:
        items = [t for t in _trash_load() if t.get("doc_id") != doc_id]  # alten Trash gleicher doc_id ersetzen
        items.append(entry)
        _trash_save(items)        # ERST sichern ...
        _delete_doc(doc_id)       # ... DANN aus dem aktiven Gehirn entfernen (kein Datenverlust)
    checkpoint("delete_entry", "Eintrag in den Papierkorb verschoben (Soft-Delete) + aus Qdrant entfernt",
               ok=True, doc_id=doc_id, title=entry["title"] or None)
    return {"ok": True, "deleted": True, "doc_id": doc_id, "title": entry["title"] or None,
            "category": entry["category"] or None}


@app.put("/entry", dependencies=[Depends(require_auth)])
def update_entry(req: UpdateReq) -> dict:
    """Ersetzt einen bestehenden Eintrag (per doc_id) 1:1 durch neuen Text: die alten Vektoren/Chunks
    werden GELOESCHT, der neue Text frisch embedded und unter DERSELBEN doc_id gespeichert. Titel,
    Kategorie und Erstellungsdatum bleiben erhalten (nur updated_at neu). Genau Franks 'alten Vektor
    durch den neuen ersetzen — wortwoertlich der Text aus dem Feld'. Sync def -> Threadpool (fastapi §1)."""
    _require_store()
    old = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=req.doc_id))]), limit=1)
    if not old:
        raise HTTPException(status_code=404, detail="Eintrag nicht gefunden")
    pl = old[0].payload
    title = (pl.get("title") or "").strip()
    category = (pl.get("category") or "").strip()
    created_at = pl.get("created_at", iso_now())
    now = iso_now()
    _delete_doc(req.doc_id)  # alte Vektoren komplett raus (alle Chunks dieser doc_id)

    chunks = chunk_text(req.text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(ch, "RETRIEVAL_DOCUMENT")
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": req.user_id, "title": title, "category": category,
            "chunk_index": i, "chunk_count": len(chunks), "chunk_text": ch,
            "full_text": req.text, "created_at": created_at, "updated_at": now,
        }))
    qc.upsert(collection_name=COLLECTION, points=points)

    replaced_ok = bool(points) and points[0].payload["full_text"] == req.text
    checkpoint("update_entry", "Alten Vektor loeschen + neuen Text 1:1 unter derselben doc_id speichern",
               ok=replaced_ok, doc_id=req.doc_id, title=title or None, chunks=len(chunks),
               chars=len(req.text), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "doc_id": req.doc_id, "title": title or None, "category": category or None,
            "chunks": len(chunks), "chars": len(req.text), "replaced": True}


@app.get("/trash", dependencies=[Depends(require_auth)])
def trash_list(user_id: str = "frank") -> dict:
    """Papierkorb-Liste fuer das Dashboard — neueste Loeschung zuerst. Reiner 1:1-Text (keine Vektoren)."""
    with _trash_lock:
        items = _trash_load()
    items = [t for t in items if (t.get("user_id") or "frank") == user_id]
    items.sort(key=lambda t: t.get("deleted_at") or "", reverse=True)
    return {"ok": True, "items": items, "count": len(items)}


@app.put("/trash", dependencies=[Depends(require_auth)])
def trash_edit(req: TrashEditReq) -> dict:
    """Aendert den Text eines Eintrags IM Papierkorb (vor der Wiederherstellung). Kein Embedding —
    der Papierkorb ist nicht durchsuchbar; das Re-Embed passiert erst beim Wiederherstellen."""
    found = False
    with _trash_lock:
        items = _trash_load()
        for t in items:
            if t.get("doc_id") == req.doc_id:
                t["text"] = req.text
                t["edited_at"] = iso_now()
                found = True
                break
        if found:
            _trash_save(items)
    if not found:
        raise HTTPException(status_code=404, detail="Eintrag nicht im Papierkorb")
    checkpoint("trash_edit", "Text eines Papierkorb-Eintrags geaendert (ohne Re-Embed)",
               ok=True, doc_id=req.doc_id, chars=len(req.text))
    return {"ok": True, "doc_id": req.doc_id, "chars": len(req.text)}


@app.delete("/trash/all", dependencies=[Depends(require_auth)])
def trash_empty(user_id: str = "frank") -> dict:
    """Leert den Papierkorb dieses Nutzers KOMPLETT und UNWIDERRUFLICH (Frank-Wunsch: 'Papierkorb leeren').
    Reiner Textspeicher — es gibt keine Vektoren mehr zu loeschen (die wurden beim Soft-Delete entfernt).
    Atomar + thread-sicher. Sync def -> Threadpool (fastapi §1)."""
    with _trash_lock:
        items = _trash_load()
        before = len(items)
        remaining = [t for t in items if (t.get("user_id") or "frank") != user_id]
        _trash_save(remaining)
    removed = before - len(remaining)
    checkpoint("trash_empty", "Papierkorb komplett geleert (unwiderruflich)", ok=True, removed=removed)
    return {"ok": True, "removed": removed}


@app.post("/trash/restore", dependencies=[Depends(require_auth)])
def trash_restore(req: RestoreReq) -> dict:
    """Stellt einen Papierkorb-Eintrag wieder her: der (ggf. editierte) Text wird frisch embedded und
    unter der URSPRUENGLICHEN doc_id ins Gehirn gespeichert (created_at erhalten), dann aus dem
    Papierkorb entfernt. Embed/Upsert laufen AUSSERHALB des Locks (langsame Calls nicht serialisieren,
    fastapi §2); der Trash-Eintrag wird erst NACH erfolgreichem Upsert entfernt (kein Datenverlust)."""
    _require_store()
    with _trash_lock:
        entry = next((t for t in _trash_load() if t.get("doc_id") == req.doc_id), None)
    if entry is None:
        raise HTTPException(status_code=404, detail="Eintrag nicht im Papierkorb")
    text = entry.get("text") or ""
    title = (entry.get("title") or "").strip()
    category = (entry.get("category") or "").strip()
    created_at = entry.get("created_at") or iso_now()
    if not text.strip():
        raise HTTPException(status_code=400, detail="Eintrag hat keinen Text")

    _delete_doc(req.doc_id)  # eventuelle Reste gleicher doc_id raus, dann frisch
    chunks = chunk_text(text)
    _guard_embed_budget(len(chunks))
    now = iso_now()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(ch, "RETRIEVAL_DOCUMENT")
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": entry.get("user_id") or req.user_id,
            "title": title, "category": category, "chunk_index": i, "chunk_count": len(chunks),
            "chunk_text": ch, "full_text": text, "created_at": created_at, "updated_at": now,
        }))
    qc.upsert(collection_name=COLLECTION, points=points)

    with _trash_lock:  # erst NACH erfolgreichem Upsert aus dem Papierkorb nehmen
        items = [t for t in _trash_load() if t.get("doc_id") != req.doc_id]
        _trash_save(items)

    restored_ok = bool(points) and points[0].payload["full_text"] == text
    checkpoint("trash_restore", "Papierkorb-Eintrag wiederhergestellt (frisch embedded, doc_id/created_at erhalten)",
               ok=restored_ok, doc_id=req.doc_id, title=title or None, chunks=len(chunks))
    return {"ok": True, "doc_id": req.doc_id, "title": title or None, "category": category or None,
            "chunks": len(chunks), "chars": len(text), "text": text}


@app.get("/")
def root() -> dict:
    return {"service": "Second Brain — brain-api (1:1-Speicher)", "version": VERSION,
            "endpoints": ["/health", "/store", "/by-title", "/by-category", "/by-date", "/search", "/list", "/entry", "/forget"]}
