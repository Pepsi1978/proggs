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

VERSION = "1.11.0"  # 1.11.0: MEHRFACH-KATEGORIEN pro Eintrag (Multi-Category, Frank-Wunsch 2026-06-25, Etappe 1). Payload-Arrays 'categories'/'parents' (Keyword-Index) zusaetzlich zu primaer 'category'/'parent' (abwaertskompatibel). embed_input() nimmt die Kategorie-LISTE -> ALLE Kategorien + Hierarchie-Ebenen ('A/B/C' -> 'A > B > C') praegen den Vektor (Eintrag in allen Kategorien semantisch auffindbar). Filter by-category/by-parent/search matchen das Array ODER das alte Einzelfeld (nested should, keine Uebergangsluecke). category-counts zaehlt pro Kategorie (Eintrag in jeder seiner) + total_distinct (Eintraege gesamt = doc_id-dedupliziert, Frank: Gesamt zaehlt 1x). NEU POST /entry/categories (volle Liste setzen + re-embed). /reembed-all backfillt categories/parents (= Migration + Re-Embed in einem). Schreibwege store/update/entry-category/trash_restore reichen die Liste durch. 1.10.0: TITEL praegt jetzt das EMBEDDING mit (Frank-Wunsch 2026-06-25). embed_input() stellt '[Titel: T | Kategorie: K]' dem Embed-Input voran (full_text/chunk_text bleiben 1:1); identifizierende Titel sind starke Diskriminatoren (rag-retrieval §4). Alle Speicher-Wege (store/update_entry/entry-category/trash_restore) reichen den Titel ins Embedding durch -> jede Aenderung fuehrt zum neuen Vektor MIT Titel. NEU: POST /reembed-all bettet den Bestand mit dem aktuellen Titel+Kategorie-Schema neu ein (Vektor neu, Payload 1:1, idempotent, 100er-Batches). 1.9.0: Unterkategorien-Fundament (Frank-Wunsch 2026-06-25, Phase 1). (a) 2-Ebenen-Kategorie 'Haupt/Unter' -> zusaetzliches Payload-Feld 'parent' (Teil vor '/') + keyword-Index, weil Qdrant KEINEN Praefix-Operator hat (bugs/server/qdrant.md §7); GET /by-parent (alles unter Haupt), POST /backfill-parent (parent auf Altbestand via set_payload, kein Re-Embed). (b) Kategorie praegt jetzt das EMBEDDING mit: embed_input() stellt '[Kategorie: X]' dem Embed-Input voran (full_text/chunk_text bleiben 1:1) -> bessere Treffer (rag-retrieval §4). Folge: Kategorie-Wechsel (POST /entry/category) embeddet jetzt FRISCH statt set_payload. store/update_entry/trash_restore setzen parent + Kategorie-Praefix. /search um parent-Filter erweitert. 1.8.0: Eintrag-Bearbeitung im Drawer (Frank-Wunsch 2026-06-25). (a) PUT /entry kann jetzt auch den TITEL aendern — UpdateReq.title (optional); bei echter Titel-Aenderung wandert der Eintrag auf die neue (titel-basierte) doc_id (alte doc_id wird geloescht, Ziel-Titel-Kollision wird ersetzt wie /store), created_at/category bleiben; Antwort gibt die neue doc_id + title_changed; Sonde stellt sicher dass keine Geist-doc_id zurueckbleibt. (b) POST /entry/category — Kategorie EINES Eintrags per set_payload aendern (Vektor unangetastet, KEIN Re-Embed), fuer das Kategorie-Dropdown im Drawer; mit Intent-Sonde. 1.7.0: POST /purge {user_id} — HARTES Loeschen ALLER Eintraege eines TEST-Nutzers (qc.delete, kein Papierkorb), fuer die Eval-Aufraeumung. Schutz: nur 'eval*'-Nutzer, NIEMALS 'frank' (403). 1.6.0: Kategorie-Verwaltung — POST /rename-category (set_payload auf allen Chunks, Vektor bleibt; bei existierendem Ziel = Merge), POST /detach-category (Kategorie-Etikett entfernen, Eintraege bleiben 1:1 erhalten — loescht NIE einen Eintrag), GET /category-counts (Payload-Kategorien mit Eintragszahl auf doc_id-Ebene). 1.5.0: Eintraege nach Aktualitaet sortiert — /by-category + /list geben das NEUESTE zuerst zurueck (Frank-Wunsch 2026-06-24: Kategorie-Ansicht war unsortiert), via _sort_recent (updated_at, sonst created_at); created_at jetzt in beiden Listen-Antworten. 1.4.0: Papierkorb (Soft-Delete) — DELETE /entry verschiebt jetzt in den Papierkorb (trash.json, persistentes /app/data-Volume) statt endgueltig zu loeschen; GET /trash (neueste zuerst), PUT /trash (Text im Papierkorb editieren, ohne Re-Embed), POST /trash/restore (frisch embedden + unter doc_id zurueck ins Gehirn, created_at erhalten). 1.3.0: DELETE /entry — Eintrag dauerhaft per doc_id loeschen (alle Chunks), fuer den Papierkorb-Button im Dashboard-Drawer (Frank-Wunsch). 1.2.0: PUT /entry — Eintrag per doc_id 1:1 ersetzen (alte Vektoren loeschen, neuen Text frisch embedden, Titel/Kategorie/created_at erhalten); doc_id jetzt in allen Listen-/Abruf-Antworten (Frontend-Editor). 1.1.0: /search um Payload-Filter (Kategorie + Datum/Bereich). 1.0.0: mem0 raus -> direkter 1:1-Speicher.

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
    for _field in ("doc_id", "title", "category", "categories", "parent", "parents", "user_id"):
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


def category_parent(category: str | None) -> str:
    """Hauptkategorie aus einer 2-Ebenen-Kategorie 'Haupt/Unter' -> 'Haupt' (Teil vor dem ersten '/').
    Ohne '/' ist die Kategorie selbst die Hauptkategorie. Leer -> ''. Wird als eigenes Payload-Feld
    'parent' gespeichert, damit 'alles unter Haupt' ein exakter MatchValue-Filter ist (Qdrant hat
    KEINEN Praefix-Operator, bugs/server/qdrant.md §7)."""
    c = (category or "").strip()
    if not c:
        return ""
    return c.split("/", 1)[0].strip()


def _dedup(seq: list[str]) -> list[str]:
    """Reihenfolge erhalten, case-insensitiv deduplizieren (erste Schreibweise gewinnt)."""
    seen: set = set(); out: list[str] = []
    for x in seq:
        k = x.casefold()
        if k not in seen:
            seen.add(k); out.append(x)
    return out


def category_parents(categories: list[str]) -> list[str]:
    """Haupt-Teile (vor dem ersten '/') ALLER Kategorien, dedupliziert. Fuer den 'parents'-Filter
    der Uebersicht (alles UNTER einer Hauptkategorie), wenn ein Eintrag mehreren Kategorien angehoert."""
    return _dedup([p for c in categories if (p := category_parent(c))])


def cats_from_payload(pl: dict) -> list[str]:
    """Kategorie-Liste aus einem gespeicherten Payload ableiten — abwaertskompatibel: bevorzugt das
    neue Listen-Feld 'categories', faellt sonst auf das alte Einzel-Feld 'category' zurueck."""
    raw = pl.get("categories")
    if isinstance(raw, list):
        out = _dedup([c.strip() for c in raw if isinstance(c, str) and c.strip()])
        if out:
            return out
    one = (pl.get("category") or "").strip()
    return [one] if one else []


def norm_cats(categories: list[str] | None, category: str | None) -> list[str]:
    """Eingehende Kategorien (Liste ODER einzelner String, beide Request-Formen) -> saubere,
    deduplizierte Liste; Reihenfolge erhalten, erste = primaer."""
    if categories:
        return _dedup([c.strip() for c in categories if isinstance(c, str) and c.strip()])
    if category and category.strip():
        return [category.strip()]
    return []


def embed_input(title: str | None, categories: list[str], text: str) -> str:
    """Eingabe fuer das EMBEDDING (nicht der gespeicherte Text!): Titel UND ALLE Kategorien des Eintrags
    werden dem Text als kurzes Praefix vorangestellt, damit alle das Bedeutungssignal mitpraegen
    (metadata-enriched embeddings, best-practices/server/rag-retrieval.md §4 — identifizierende Felder
    wie Titel/Kategorie sind starke Diskriminatoren). Hierarchische Kategorien 'A/B/C' werden zu
    'A > B > C' expandiert, damit JEDE Ebene mitzaehlt (Frank-Wunsch: Haupt- + Unterkategorie(n) in den
    Vektor). Mehrere Kategorien -> der Eintrag ist semantisch in ALLEN auffindbar. Der 1:1-Volltext
    (full_text) und der angezeigte chunk_text bleiben UNVERAENDERT — nur der Vektor wird angereichert.
    Jede Kategorie-/Titel-Aenderung erzwingt Re-Embed (§4) — 'jede Aenderung fuehrt zum neuen Vektor'."""
    t = (title or "").strip()
    exp = [" > ".join(s.strip() for s in c.split("/") if s.strip()) for c in categories if c and c.strip()]
    parts: list[str] = []
    if t:
        parts.append(f"Titel: {t}")
    if exp:
        parts.append("Kategorien: " + ", ".join(exp))
    return f"[{' | '.join(parts)}]\n\n{text}" if parts else text


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
    category: str | None = Field(default=None, description="Optionale Kategorie (Abwaertskompat: einzelne Kategorie)")
    categories: list[str] | None = Field(default=None, max_length=12, description="Optional MEHRERE Kategorien (Multi-Category); hat Vorrang vor 'category'. Erste = primaer.")
    user_id: str = Field(default="frank", description="Besitzer (aktuell immer 'frank')")


class SearchReq(BaseModel):
    query: str = Field(..., min_length=1)
    user_id: str = Field(default="frank")
    limit: int = Field(default=5, ge=1, le=50)
    # Optionale Payload-Filter: erst eingrenzen, DANN semantisch suchen (Franks "war letzten Monat angeln").
    category: str | None = Field(default=None, description="Nur in dieser exakten (Unter-)Kategorie suchen ('Haupt/Unter')")
    parent: str | None = Field(default=None, description="Nur unter dieser Hauptkategorie suchen (= alles in 'Haupt/...'), via parent-Feld")
    date: str | None = Field(default=None, description="Nur Eintraege dieses Tages (YYYY-MM-DD)")
    date_from: str | None = Field(default=None, description="Ab diesem Tag (YYYY-MM-DD, inklusive)")
    date_to: str | None = Field(default=None, description="Bis zu diesem Tag (YYYY-MM-DD, inklusive)")


class UpdateReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des zu ersetzenden Eintrags (aus /list, /by-category, /search)")
    text: str = Field(..., min_length=1, max_length=200_000, description="Neuer 1:1-Text — ersetzt den alten Vektor komplett (max_length gegen OOM, fastapi §8)")
    title: str | None = Field(default=None, max_length=200, description="Optionaler NEUER Titel. None -> alter Titel bleibt. Aendert sich der Titel, wandert der Eintrag auf die neue (titel-basierte) doc_id (Frank-Wunsch: Titel im Drawer bearbeiten).")
    categories: list[str] | None = Field(default=None, max_length=12, description="Optional NEUE Kategorie-Liste (Multi-Category). None -> bisherige Kategorien bleiben.")
    user_id: str = Field(default="frank")


class EntryCategoriesReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des Eintrags, dessen Kategorie-Liste gesetzt wird")
    categories: list[str] = Field(..., min_length=1, max_length=12, description="Vollstaendige neue Kategorie-Liste (Multi-Category, 1:1 deutsche Rechtschreibung). Re-Embed, da die Kategorien den Vektor mitpraegen.")
    user_id: str = Field(default="frank")


class TrashEditReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des Papierkorb-Eintrags")
    text: str = Field(..., min_length=1, max_length=200_000, description="Neuer Text IM Papierkorb (kein Re-Embed; gilt erst bei Wiederherstellung)")
    user_id: str = Field(default="frank")


class RestoreReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des wiederherzustellenden Papierkorb-Eintrags")
    user_id: str = Field(default="frank")


class RenameCategoryReq(BaseModel):
    old: str = Field(..., min_length=1, max_length=60, description="Bisheriger Kategorie-Name (exakter Payload-Wert)")
    new: str = Field(..., min_length=1, max_length=60, description="Neuer Kategorie-Name (1:1, deutsche Rechtschreibung)")
    user_id: str = Field(default="frank")


class DetachCategoryReq(BaseModel):
    name: str = Field(..., min_length=1, max_length=60, description="Kategorie, deren Etikett von allen Eintraegen entfernt wird (Eintraege bleiben)")
    user_id: str = Field(default="frank")


class EntryCategoryReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des Eintrags, dessen Kategorie geaendert wird")
    category: str = Field(default="", max_length=60, description="Neue Kategorie (1:1, deutsche Rechtschreibung). Leer = Etikett entfernen.")
    user_id: str = Field(default="frank")


class PurgeReq(BaseModel):
    user_id: str = Field(..., min_length=1, max_length=40, description="TEST-Nutzer (MUSS mit 'eval' beginnen) — ALLE seine Eintraege werden HART geloescht (kein Papierkorb)")


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

    cats = norm_cats(req.categories, req.category)
    category = cats[0] if cats else ""        # primaere Kategorie (Abwaertskompat)
    parents = category_parents(cats)
    parent = parents[0] if parents else ""    # primaerer Haupt-Teil (Abwaertskompat)
    chunks = chunk_text(req.text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(embed_input(req.title, cats, ch), "RETRIEVAL_DOCUMENT")  # Titel + ALLE Kategorien praegen den Vektor mit (chunk_text bleibt 1:1)
        points.append(PointStruct(id=point_id(doc_id, i), vector=vec, payload={
            "doc_id": doc_id,
            "user_id": req.user_id,
            "title": (req.title or "").strip(),
            "category": category,
            "categories": cats,
            "parent": parent,
            "parents": parents,
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
            "categories": cats_from_payload(points[0].payload),
            "updated_at": points[0].payload.get("updated_at"), "text": full}


@app.get("/by-category", dependencies=[Depends(require_auth)])
def by_category(category: str, user_id: str = "frank") -> dict:
    """Alle Eintraege einer Kategorie (auf Dokument-Ebene dedupliziert), jeweils 1:1."""
    _require_store()
    cat = category.strip()
    points = _scroll(Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=user_id)),
        # Multi-Category: matcht das neue Array 'categories' ODER (Abwaertskompat) das alte 'category'
        Filter(should=[FieldCondition(key="categories", match=MatchValue(value=cat)),
                       FieldCondition(key="category", match=MatchValue(value=cat))]),
    ]))
    seen: dict[str, dict] = {}
    for p in points:
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "text": p.payload.get("full_text", ""), "category": p.payload.get("category") or None,
                         "categories": cats_from_payload(p.payload),
                         "created_at": p.payload.get("created_at"),
                         "updated_at": p.payload.get("updated_at")}
    items = _sort_recent(seen.values())   # Neueste zuerst (nach Aktualitaet)
    return {"ok": True, "category": category, "count": len(items), "items": items}


@app.get("/by-parent", dependencies=[Depends(require_auth)])
def by_parent(parent: str, user_id: str = "frank") -> dict:
    """Alle Eintraege UNTER einer Hauptkategorie (= 'Haupt' und alle 'Haupt/Unter'), via parent-Feld
    (exakter MatchValue — Qdrant hat keinen Praefix-Operator). Auf Dokument-Ebene dedupliziert, 1:1."""
    _require_store()
    par = parent.strip()
    points = _scroll(Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=user_id)),
        # Multi-Category: matcht das neue Array 'parents' ODER (Abwaertskompat) das alte 'parent'
        Filter(should=[FieldCondition(key="parents", match=MatchValue(value=par)),
                       FieldCondition(key="parent", match=MatchValue(value=par))]),
    ]))
    seen: dict[str, dict] = {}
    for p in points:
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "text": p.payload.get("full_text", ""), "category": p.payload.get("category") or None,
                         "parent": p.payload.get("parent") or None,
                         "created_at": p.payload.get("created_at"), "updated_at": p.payload.get("updated_at")}
    items = _sort_recent(seen.values())
    return {"ok": True, "parent": parent, "count": len(items), "items": items}


@app.post("/backfill-parent", dependencies=[Depends(require_auth)])
def backfill_parent(user_id: str = "frank") -> dict:
    """Einmal-Migration: setzt das 'parent'-Feld (Hauptkategorie vor dem '/') auf ALLEN bestehenden
    Eintraegen per set_payload (KEIN Re-Embed — billig). Macht 'alles unter Haupt' auch fuer den
    Altbestand filterbar. Idempotent: kann gefahrlos mehrfach laufen. Sync def -> Threadpool (fastapi §1)."""
    _require_store()
    points = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]))
    # je Hauptkategorie die zugehoerigen Point-IDs sammeln, dann gebuendelt set_payload
    by_par: dict[str, list] = {}
    for p in points:
        par = category_parent(p.payload.get("category"))
        if (p.payload.get("parent") or "") == par:
            continue  # schon korrekt -> ueberspringen
        by_par.setdefault(par, []).append(p.id)
    updated = 0
    for par, ids in by_par.items():
        qc.set_payload(collection_name=COLLECTION, payload={"parent": par}, points=ids, wait=True)
        updated += len(ids)
    checkpoint("backfill_parent", "parent-Feld auf Altbestand gesetzt (set_payload, kein Re-Embed)",
               ok=True, chunks_updated=updated, groups=len(by_par))
    return {"ok": True, "chunks_updated": updated, "groups": len(by_par)}


@app.get("/category-counts", dependencies=[Depends(require_auth)])
def category_counts(user_id: str = "frank") -> dict:
    """Alle BEFUELLTEN Kategorien mit Anzahl EINTRAEGE (auf doc_id dedupliziert, nicht Chunks).
    Quelle der Payload-Kategorien; leere (eintragslose) Kategorien kennt nur die Agent-Registry."""
    _require_store()
    points = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]))
    seen: dict[str, set] = {}
    all_docs: set = set()
    for p in points:
        did = p.payload.get("doc_id")
        all_docs.add(did)
        for c in cats_from_payload(p.payload):   # Multi-Category: Eintrag zaehlt in JEDER seiner Kategorien
            seen.setdefault(c, set()).add(did)
    counts = {c: len(dids) for c, dids in seen.items()}
    # total_distinct = Anzahl EINTRAEGE (jeder doc_id 1x) -> 'Eintraege Gesamt' bleibt korrekt,
    # auch wenn die Summe der Kategorie-Balken groesser ist (Mehrfachzuordnung, Frank-Wunsch).
    checkpoint("category_counts", "Kategorien mit Eintragszahl (Multi-Category, doc_id-dedupliziert) + total_distinct",
               ok=True, categories=len(counts), total=len(all_docs))
    return {"ok": True, "counts": counts, "total_distinct": len(all_docs)}


@app.post("/rename-category", dependencies=[Depends(require_auth)])
def rename_category(req: RenameCategoryReq) -> dict:
    """Benennt eine Kategorie in ALLEN Payloads um (set_payload — der Vektor bleibt unangetastet,
    KEIN Re-Embedding). Existiert 'new' bereits, ist das Ergebnis ein Merge (alle 'old'-Eintraege
    wandern nach 'new'). Loescht NIE einen Eintrag."""
    _require_store()
    old, new = req.old.strip(), req.new.strip()
    if not old or not new:
        raise HTTPException(status_code=400, detail="old/new duerfen nicht leer sein")
    flt = Filter(must=[
        FieldCondition(key="category", match=MatchValue(value=old)),
        FieldCondition(key="user_id", match=MatchValue(value=req.user_id)),
    ])
    pts = _scroll(flt)
    docs = {p.payload.get("doc_id") for p in pts}
    if old != new and pts:
        # wait=True: Operation abgeschlossen bevor das Dashboard neu laedt (sonst alte Werte sichtbar)
        qc.set_payload(collection_name=COLLECTION, payload={"category": new}, points=flt, wait=True)
    merged = old != new and pts  # informativ: war 'new' schon belegt -> de facto Merge (Aufrufer weiss es)
    checkpoint("rename_category", "Kategorie in allen Payloads umbenannt (Vektor unangetastet, kein Re-Embed)",
               ok=True, old=old, new=new, points=len(pts), entries=len(docs))
    return {"ok": True, "old": old, "new": new, "points": len(pts), "entries": len(docs)}


@app.post("/detach-category", dependencies=[Depends(require_auth)])
def detach_category(req: DetachCategoryReq) -> dict:
    """Loescht die KATEGORIE, nicht die Eintraege: setzt category auf '' bei allen Eintraegen
    dieser Kategorie. Die Eintraege (Vektoren + Volltext) bleiben vollstaendig erhalten — nur das
    Kategorie-Etikett faellt weg. NIEMALS ein Eintrag geloescht."""
    _require_store()
    name = req.name.strip()
    if not name:
        raise HTTPException(status_code=400, detail="name darf nicht leer sein")
    flt = Filter(must=[
        FieldCondition(key="category", match=MatchValue(value=name)),
        FieldCondition(key="user_id", match=MatchValue(value=req.user_id)),
    ])
    pts = _scroll(flt)
    docs = {p.payload.get("doc_id") for p in pts}
    if pts:
        qc.set_payload(collection_name=COLLECTION, payload={"category": ""}, points=flt, wait=True)
    checkpoint("detach_category", "Kategorie-Etikett entfernt — Eintraege bleiben 1:1 erhalten",
               ok=True, name=name, points=len(pts), entries=len(docs))
    return {"ok": True, "name": name, "points": len(pts), "entries": len(docs)}


@app.post("/entry/category", dependencies=[Depends(require_auth)])
def set_entry_category(req: EntryCategoryReq) -> dict:
    """Aendert die Kategorie EINES Eintrags (per doc_id). Da die Kategorie das EMBEDDING mitpraegt
    (metadata-enriched, best-practices/server/rag-retrieval.md §4), reicht set_payload NICHT mehr —
    der Eintrag wird mit dem neuen Kategorie-Praefix FRISCH eingebettet (full_text/Titel/created_at
    bleiben 1:1). Setzt zugleich das abgeleitete 'parent'-Feld. Sync def -> Threadpool (fastapi §1)."""
    _require_store()
    pts = _scroll(Filter(must=[
        FieldCondition(key="doc_id", match=MatchValue(value=req.doc_id)),
        FieldCondition(key="user_id", match=MatchValue(value=req.user_id)),
    ]), limit=1000)
    if not pts:
        raise HTTPException(status_code=404, detail="Eintrag nicht gefunden")
    pl = pts[0].payload
    old_cat = (pl.get("category") or "").strip()
    new_cat = (req.category or "").strip()
    new_cats = [new_cat] if new_cat else []   # /entry/category setzt GENAU eine Kategorie (Einzel-Wechsel); Multi -> /entry/categories
    new_parents = category_parents(new_cats)
    new_parent = new_parents[0] if new_parents else ""
    title = (pl.get("title") or "").strip()
    full_text = pl.get("full_text", "")
    created_at = pl.get("created_at", iso_now())
    now = iso_now()

    _delete_doc(req.doc_id)  # alte Chunks (mit altem Kategorie-Vektor) raus
    chunks = chunk_text(full_text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(embed_input(title, new_cats, ch), "RETRIEVAL_DOCUMENT")  # Titel + neue Kategorie praegen den Vektor mit
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": req.user_id, "title": title,
            "category": new_cat, "categories": new_cats, "parent": new_parent, "parents": new_parents, "chunk_index": i, "chunk_count": len(chunks),
            "chunk_text": ch, "full_text": full_text, "created_at": created_at, "updated_at": now,
        }))
    qc.upsert(collection_name=COLLECTION, points=points, wait=True)
    after = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=req.doc_id))]), limit=1)
    applied = bool(after) and (after[0].payload.get("category") or "").strip() == new_cat
    probe(applied, "Kategorie-Verschiebung nicht angekommen", doc_id=req.doc_id, want=new_cat)
    checkpoint("set_entry_category", "Kategorie EINES Eintrags geaendert (frisch eingebettet mit neuem Kategorie-Praefix + parent)",
               ok=applied, doc_id=req.doc_id, old=old_cat or None, new=new_cat or None,
               parent=new_parent or None, chunks=len(chunks), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "doc_id": req.doc_id, "old": old_cat or None, "new": new_cat or None}


@app.post("/entry/categories", dependencies=[Depends(require_auth)])
def set_entry_categories(req: EntryCategoriesReq) -> dict:
    """Setzt die VOLLSTAENDIGE Kategorie-Liste EINES Eintrags (Multi-Category, hinter dem Drawer-Plus).
    Da die Kategorien das EMBEDDING mitpraegen, wird FRISCH neu eingebettet (full_text/Titel/created_at
    bleiben 1:1). Sync def -> Threadpool (fastapi §1)."""
    _require_store()
    pts = _scroll(Filter(must=[
        FieldCondition(key="doc_id", match=MatchValue(value=req.doc_id)),
        FieldCondition(key="user_id", match=MatchValue(value=req.user_id)),
    ]), limit=1000)
    if not pts:
        raise HTTPException(status_code=404, detail="Eintrag nicht gefunden")
    cats = norm_cats(req.categories, None)
    if not cats:
        raise HTTPException(status_code=400, detail="Mindestens eine gueltige Kategorie noetig")
    parents = category_parents(cats)
    pl = pts[0].payload
    title = (pl.get("title") or "").strip()
    full_text = pl.get("full_text", "")
    created_at = pl.get("created_at", iso_now())
    now = iso_now()

    _delete_doc(req.doc_id)  # alte Chunks (mit alten Kategorie-Vektoren) raus
    chunks = chunk_text(full_text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(embed_input(title, cats, ch), "RETRIEVAL_DOCUMENT")  # Titel + ALLE Kategorien praegen den Vektor mit
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": req.user_id, "title": title,
            "category": cats[0], "categories": cats,
            "parent": (parents[0] if parents else ""), "parents": parents,
            "chunk_index": i, "chunk_count": len(chunks), "chunk_text": ch,
            "full_text": full_text, "created_at": created_at, "updated_at": now,
        }))
    qc.upsert(collection_name=COLLECTION, points=points, wait=True)
    after = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=req.doc_id))]), limit=1)
    applied = bool(after) and set(cats_from_payload(after[0].payload)) == set(cats)
    probe(applied, "Kategorie-Liste nicht angekommen", doc_id=req.doc_id, want=cats)
    checkpoint("set_entry_categories", "Kategorie-LISTE eines Eintrags gesetzt (Multi-Category, frisch eingebettet)",
               ok=applied, doc_id=req.doc_id, categories=cats, parents=parents, chunks=len(chunks),
               ms=int((time.time() - t0) * 1000))
    return {"ok": True, "doc_id": req.doc_id, "categories": cats, "parents": parents}


@app.post("/purge", dependencies=[Depends(require_auth)])
def purge_user(req: PurgeReq) -> dict:
    """HARTES Loeschen ALLER Eintraege eines TEST-Nutzers (qc.delete, KEIN Papierkorb) — fuer die
    Eval-Aufraeumung. SCHUTZ: nur user_ids die mit 'eval' beginnen; 'frank' und alles andere ist
    gesperrt (verhindert versehentliches Loeschen des echten Gehirns)."""
    _require_store()
    uid = req.user_id.strip()
    if not uid.startswith("eval") or uid == "frank":
        raise HTTPException(status_code=403, detail="purge ist NUR fuer eval-Test-Nutzer erlaubt (Schutz des echten Gehirns)")
    flt = Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=uid))])
    pts = _scroll(flt)
    docs = {p.payload.get("doc_id") for p in pts}
    if pts:
        qc.delete(collection_name=COLLECTION, points_selector=flt, wait=True)
    checkpoint("purge", "Test-Nutzer HART geloescht (kein Papierkorb)", ok=True, user_id=uid, points=len(pts), entries=len(docs))
    _log(logging.INFO, "Eval-Test-Nutzer gepurged", user_id=uid, points=len(pts), entries=len(docs))
    return {"ok": True, "user_id": uid, "points": len(pts), "entries": len(docs)}


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
        _c = req.category.strip()   # Multi-Category: neues Array 'categories' ODER altes 'category'
        must.append(Filter(should=[FieldCondition(key="categories", match=MatchValue(value=_c)),
                                   FieldCondition(key="category", match=MatchValue(value=_c))]))
    elif req.parent and req.parent.strip():   # 'alles unter Haupt' (nur wenn keine exakte Kategorie gesetzt)
        _p = req.parent.strip()
        must.append(Filter(should=[FieldCondition(key="parents", match=MatchValue(value=_p)),
                                   FieldCondition(key="parent", match=MatchValue(value=_p))]))
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

    applied = {"category": req.category or None, "parent": req.parent or None, "date": req.date or None,
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
    old_title = (pl.get("title") or "").strip()
    cats = norm_cats(req.categories, None) or cats_from_payload(pl)   # req.categories ueberschreibt; sonst bisherige Kategorien behalten
    category = cats[0] if cats else ""
    created_at = pl.get("created_at", iso_now())
    now = iso_now()

    # Titel-Aenderung (Frank-Wunsch): req.title=None -> alter Titel bleibt. Sonst neuer Titel.
    # Die doc_id eines betitelten Eintrags HAENGT vom Titel ab (make_doc_id) -> bei echter Titel-
    # aenderung MUSS der Eintrag auf die neue doc_id wandern (sonst findet /by-title ihn nicht mehr).
    title = old_title if req.title is None else req.title.strip()
    title_changed = req.title is not None and title != old_title
    target_doc_id = req.doc_id
    if title_changed and title:
        target_doc_id = make_doc_id(req.user_id, title)

    _delete_doc(req.doc_id)  # alte Vektoren komplett raus (alle Chunks der bisherigen doc_id)
    if target_doc_id != req.doc_id:
        _delete_doc(target_doc_id)  # Ziel-Titel evtl. schon belegt -> dessen Chunks ersetzen (wie /store)

    parents = category_parents(cats)
    parent = parents[0] if parents else ""
    chunks = chunk_text(req.text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(embed_input(title, cats, ch), "RETRIEVAL_DOCUMENT")  # Titel + ALLE Kategorien praegen den Vektor mit
        points.append(PointStruct(id=point_id(target_doc_id, i), vector=vec, payload={
            "doc_id": target_doc_id, "user_id": req.user_id, "title": title, "category": category, "categories": cats, "parent": parent, "parents": parents,
            "chunk_index": i, "chunk_count": len(chunks), "chunk_text": ch,
            "full_text": req.text, "created_at": created_at, "updated_at": now,
        }))
    qc.upsert(collection_name=COLLECTION, points=points)

    replaced_ok = bool(points) and points[0].payload["full_text"] == req.text
    # Sonde: nach einer Titel-Migration darf die alte doc_id NICHT mehr existieren (kein Geist-Duplikat)
    if title_changed and target_doc_id != req.doc_id:
        leftover = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=req.doc_id))]), limit=1)
        probe(not leftover, "Alte doc_id nach Titel-Migration noch vorhanden", old_doc_id=req.doc_id, new_doc_id=target_doc_id)
    checkpoint("update_entry", "Alten Vektor loeschen + neuen Text 1:1 speichern (Titel-Aenderung -> neue doc_id)",
               ok=replaced_ok, doc_id=target_doc_id, old_doc_id=req.doc_id if title_changed else None,
               title=title or None, title_changed=title_changed, chunks=len(chunks),
               chars=len(req.text), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "doc_id": target_doc_id, "title": title or None, "category": category or None,
            "title_changed": title_changed, "chunks": len(chunks), "chars": len(req.text), "replaced": True}


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
    cats = cats_from_payload(entry)   # Trash-Eintrag kann 'categories' ODER altes 'category' haben
    category = cats[0] if cats else ""
    created_at = entry.get("created_at") or iso_now()
    if not text.strip():
        raise HTTPException(status_code=400, detail="Eintrag hat keinen Text")

    parents = category_parents(cats)
    parent = parents[0] if parents else ""
    _delete_doc(req.doc_id)  # eventuelle Reste gleicher doc_id raus, dann frisch
    chunks = chunk_text(text)
    _guard_embed_budget(len(chunks))
    now = iso_now()
    points = []
    for i, ch in enumerate(chunks):
        vec = embed(embed_input(title, cats, ch), "RETRIEVAL_DOCUMENT")  # Titel + ALLE Kategorien praegen den Vektor mit
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": entry.get("user_id") or req.user_id,
            "title": title, "category": category, "categories": cats, "parent": parent, "parents": parents, "chunk_index": i, "chunk_count": len(chunks),
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


class ReembedReq(BaseModel):
    user_id: str = Field(default="frank", description="Nur Punkte dieses Besitzers neu einbetten (leer = alle)")
    limit: int = Field(default=100000, ge=1, le=1000000, description="Sicherheitsdeckel fuer die Anzahl Punkte")


@app.post("/reembed-all", dependencies=[Depends(require_auth)])
def reembed_all(req: ReembedReq) -> dict:
    """Einmaliger Wartungslauf: bettet ALLE bestehenden Punkte mit dem AKTUELLEN embed_input-Schema
    (Titel + Kategorie im Praefix) NEU ein. full_text, Titel, Kategorie, parent, doc_id, created_at und
    der angezeigte chunk_text bleiben 1:1 — NUR der Vektor wird neu berechnet (gleiche Point-ID -> Upsert
    ueberschreibt). Idempotent (nochmal laufen = gleiches Ergebnis). Sync def -> Threadpool (fastapi §1).
    Auth-Pflicht. 100er-Batches -> beherrschbarer Speicher + Teil-Fortschritt bei vielen Punkten."""
    _require_store()
    flt = (Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=req.user_id))])
           if req.user_id.strip() else None)
    pts = _scroll(flt, limit=req.limit)
    if not pts:
        return {"ok": True, "reembedded": 0, "total": 0, "note": "keine Punkte"}
    _guard_embed_budget(len(pts))
    t0 = time.time()
    done = 0
    for start in range(0, len(pts), 100):
        batch = pts[start:start + 100]
        points = []
        for p in batch:
            pl = dict(p.payload or {})
            title = (pl.get("title") or "").strip()
            cats = cats_from_payload(pl)                 # Migration mit: aus 'categories' ODER altem 'category'
            pl["categories"] = cats                      # Multi-Category-Felder im Payload sicherstellen (Backfill)
            pl["parents"] = category_parents(cats)
            if cats and not (pl.get("category") or "").strip():
                pl["category"] = cats[0]
            if pl["parents"] and not (pl.get("parent") or "").strip():
                pl["parent"] = pl["parents"][0]
            ch = pl.get("chunk_text", pl.get("full_text", ""))
            vec = embed(embed_input(title, cats, ch), "RETRIEVAL_DOCUMENT")  # Titel + ALLE Kategorien praegen den Vektor mit
            points.append(PointStruct(id=p.id, vector=vec, payload=pl))   # Payload inkl. categories/parents
        qc.upsert(collection_name=COLLECTION, points=points, wait=True)
        done += len(points)
    checkpoint("reembed_all", "Bestand mit aktuellem Titel+Kategorie-Schema neu eingebettet (Vektor neu, Payload 1:1)",
               ok=(done == len(pts)), reembedded=done, total=len(pts), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "reembedded": done, "total": len(pts)}


@app.get("/")
def root() -> dict:
    return {"service": "Second Brain — brain-api (1:1-Speicher)", "version": VERSION,
            "endpoints": ["/health", "/store", "/by-title", "/by-category", "/by-parent", "/by-date", "/search", "/list", "/entry", "/entry/categories", "/reembed-all", "/forget"]}
