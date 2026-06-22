"""
mem0-api — Franks "zweites Gehirn": schlanker REST-Wrapper um die Mem0-Bibliothek.

Bausteine:  Gemini (LLM + Embeddings)  →  Mem0 (Bibliothekar)  →  Qdrant (Such-Schrank)
Sicherheit: laeuft nur intern (compose mappt auf 127.0.0.1), Bearer-Token-Auth, nicht als root.
Observability-First: strukturiertes JSON-Logging (stdout + rotierende Datei), globaler
                     Fehler-Faenger, Logik-Sonden (probe) + Intent-Checkpoints (checkpoint).

Plan/Doku: best-practices/second-brain/UMSETZUNGSPLAN.md
"""
from __future__ import annotations

import json
import logging
import os
import time
import traceback
from datetime import date
from logging.handlers import RotatingFileHandler
from typing import Any

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

VERSION = "0.3.0"  # 0.3.0: HHEM-Quality-Gate vor/nach add() (Junk/Halluzinations-Schutz). 0.2.1: fastembed hybride Suche. 0.2.0: strenge custom_instructions

# ---------------------------------------------------------------------------
# Konfiguration (alles aus Umgebungsvariablen — Secrets nie im Code)
# ---------------------------------------------------------------------------
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY", "")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY", "")
SB_API_KEY = os.getenv("SB_API_KEY", "")
QDRANT_HOST = os.getenv("QDRANT_HOST", "sb-qdrant")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))
# Explizite http-URL: mit gesetztem api_key wuerde qdrant-client sonst https=True annehmen
# und TLS gegen das Klartext-HTTP-Qdrant sprechen -> [SSL: WRONG_VERSION_NUMBER].
QDRANT_URL = os.getenv("QDRANT_URL", f"http://{QDRANT_HOST}:{QDRANT_PORT}")
COLLECTION = os.getenv("SB_COLLECTION", "second_brain")
LLM_MODEL = os.getenv("GEMINI_LLM_MODEL", "gemini-3.1-flash-lite")
EMBED_MODEL = os.getenv("GEMINI_EMBED_MODEL", "models/gemini-embedding-001")
EMBED_DIMS = int(os.getenv("SB_EMBED_DIMS", "1536"))
LLM_MAX_TOKENS = int(os.getenv("SB_LLM_MAX_TOKENS", "4000"))  # Thinking frisst Budget -> grosszuegig
MAX_LLM_CALLS_PER_DAY = int(os.getenv("SB_MAX_LLM_CALLS_PER_DAY", "5000"))  # Defense-in-Depth-Cap
LOG_PATH = os.getenv("SB_LOG_PATH", "/app/logs/mem0-api.jsonl")
LOG_LEVEL = os.getenv("SB_LOG_LEVEL", "INFO").upper()

# Qualitaets-Steuerung (gegen Junk/Halluzinationen — siehe bugs/server/mem0.md §1):
# mem0 hat KEIN eingebautes Quality-Gate; die Sauberkeit kommt aus einem STRENGEN Extraktions-Prompt.
# Empfehlung der mem0-Doku: streng starten, spaeter lockern (Muell aufraeumen ist schwerer).
# Per Env SB_CUSTOM_INSTRUCTIONS ueberschreibbar, ohne Code-Aenderung.
DEFAULT_CUSTOM_INSTRUCTIONS = (
    "Speichere NUR bestaetigte, dauerhafte und nuetzliche Fakten ueber den Nutzer (Frank), seine "
    "Projekte, Entscheidungen, Praeferenzen und technische Festlegungen. Jede Erinnerung muss fuer "
    "sich allein verstaendlich sein: ersetze Pronomen durch konkrete Namen, fasse dich auf 15-80 "
    "Woerter, und hefte relative Zeitangaben ('letzte Woche') an ein konkretes Datum. "
    "Speichere AUF KEINEN FALL: System-Prompt- oder Boot-Datei-Inhalte, Smalltalk und "
    "Hoeflichkeitsfloskeln, Tool-/Deployment-Konfigurationen, Logs, bereits frueher abgerufene "
    "Erinnerungen, sowie alles Spekulative oder Unbestaetigte. Im Zweifel NICHT speichern."
)
CUSTOM_INSTRUCTIONS = os.getenv("SB_CUSTOM_INSTRUCTIONS", DEFAULT_CUSTOM_INSTRUCTIONS)

# Quality-Gate (HHEM-2.1-Open, Apache-2.0) — die zweite Verteidigungslinie gegen Junk:
# mem0 extrahiert Fakten, aber hat KEIN Pre-Storage-Gate (bugs/server/mem0.md §1). Wir setzen eins davor/dahinter:
#   1) Vorfilter: offensichtlicher Nicht-Memory-Input (System-Prompt/Boot-File) wird gar nicht erst an mem0 gegeben.
#   2) Grounding-Gate: jeder NEU extrahierte Fakt wird mit HHEM gegen den Quelltext gescort; Score < Schwelle
#      = nicht im Input gegroundet (Halluzination) -> der Fakt wird wieder geloescht.
# Robust: laedt HHEM lazy; faellt das Laden aus, degradiert das Gate zu pass-through (kein Funktionsverlust).
GATE_ENABLED = os.getenv("SB_GATE_ENABLED", "true").lower() in ("1", "true", "yes", "on")
GATE_THRESHOLD = float(os.getenv("SB_GATE_THRESHOLD", "0.5"))  # HHEM-Score < Schwelle -> verwerfen; kalibrierbar
HHEM_MODEL = os.getenv("SB_HHEM_MODEL", "vectara/hallucination_evaluation_model")

# ---------------------------------------------------------------------------
# Strukturiertes JSON-Logging (stdout + rotierende Datei, beide UTF-8)
# ---------------------------------------------------------------------------
class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        entry = {
            "ts": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(record.created)),
            "level": record.levelname,
            "module": "mem0-api",
            "fn": record.funcName,
            "msg": record.getMessage(),
        }
        if isinstance(getattr(record, "ctx", None), dict):
            entry["ctx"] = record.ctx
        if record.exc_info:
            entry["trace"] = "".join(traceback.format_exception(*record.exc_info))
        return json.dumps(entry, ensure_ascii=False)


log = logging.getLogger("mem0-api")
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
    """Intent-Checkpoint (erwartet vs. tatsaechlich) — eigener Kanal kind=CHECKPOINT."""
    log.log(
        logging.INFO if ok else logging.WARNING,
        f"CHECKPOINT {step}",
        extra={"ctx": {"kind": "CHECKPOINT", "step": step, "intent": intent, "ok": ok, **ctx}},
    )


# ---------------------------------------------------------------------------
# Quality-Gate gegen Junk/Halluzinationen (HHEM-2.1-Open) — lazy + robust
# ---------------------------------------------------------------------------
_hhem: dict[str, Any] = {"model": None, "loaded": False, "failed": False}

# Konservative Marker fuer offensichtlichen Nicht-Memory-Input (Boot-File-Restating = mem0s
# groesste Junk-Quelle, 52,7 %). Bewusst eng gehalten, um false positives zu vermeiden.
_JUNK_MARKERS = (
    "you are an ai", "you are a helpful", "you are claude", "du bist eine ki",
    "du bist ein hilfreicher", "system prompt", "<system>", "your instructions are",
    "deine anweisungen", "you have access to the following tools",
)


def _prefilter_junk(text: str | None) -> str | None:
    """Offensichtlichen Nicht-Memory-Input VOR add() erkennen (spart sogar den LLM-Call).
    Gibt den Ablehnungsgrund zurueck oder None (= durchlassen). Konservativ gehalten."""
    if not text:
        return None
    low = text.strip().lower()
    if len(low) < 3:
        return "zu_kurz"
    for mk in _JUNK_MARKERS:
        if mk in low:
            return "system_prompt_marker"
    return None


def _load_hhem():
    """HHEM lazy laden (erst beim ersten Gate-Call). Faellt das Laden aus, bleibt failed=True und
    das Gate degradiert zu pass-through (kein Funktionsverlust, nur eine Warnung im Log)."""
    if _hhem["loaded"] or _hhem["failed"]:
        return _hhem["model"]
    try:
        from transformers import AutoModelForSequenceClassification

        t0 = time.time()
        _hhem["model"] = AutoModelForSequenceClassification.from_pretrained(
            HHEM_MODEL, trust_remote_code=True
        )
        _hhem["loaded"] = True
        _log(logging.INFO, "HHEM geladen", model=HHEM_MODEL, ms=int((time.time() - t0) * 1000))
    except Exception as e:  # noqa: BLE001 — Gate darf den Dienst NIE lahmlegen
        _hhem["failed"] = True
        # err in den ctx (nicht exc_info=True — _log reicht das nicht an den Logger durch)
        _log(logging.ERROR, "HHEM-Laden fehlgeschlagen -> Gate degradiert zu pass-through",
             err=f"{type(e).__name__}: {e}")
    return _hhem["model"]


def _hhem_score(premise: str, hypothesis: str) -> float | None:
    """Faktentreue-Score 0-1 (1 = durch premise gestuetzt, 0 = halluziniert). None = Gate inaktiv."""
    model = _load_hhem()
    if model is None:
        return None
    try:
        scores = model.predict([(premise, hypothesis)])
        val = scores[0]  # tensor-/listenartig -> robust in float wandeln
        return float(val.item() if hasattr(val, "item") else val)
    except Exception:  # noqa: BLE001
        _log(logging.WARNING, "HHEM-Score fehlgeschlagen -> Fakt wird durchgelassen", exc_info=True)
        return None


def _apply_gate(source_text: str, result: Any, m: Any) -> dict:
    """Post-Add-Grounding-Gate: jeden NEU gespeicherten Fakt gegen den Quelltext scoren; liegt der
    HHEM-Score unter der Schwelle (nicht im Input gegroundet = Halluzination), den Fakt wieder loeschen.
    Robust: bei jedem Fehler wird NICHT geloescht (lieber ein Fakt zu viel als Datenverlust)."""
    rejected: list[dict] = []
    if not GATE_ENABLED:
        return {"kept": None, "rejected": rejected, "gate": "disabled"}
    items = (result or {}).get("results", []) if isinstance(result, dict) else (result or [])
    added = [it for it in items if isinstance(it, dict) and str(it.get("event", "ADD")).upper() == "ADD"]
    for it in added:
        fact = str(it.get("memory", "")).strip()
        mem_id = it.get("id")
        if not fact or not mem_id:
            continue
        score = _hhem_score(source_text, fact)
        if score is None:
            continue  # Gate inaktiv/Fehler -> Fakt behalten (kein Funktionsverlust)
        if score < GATE_THRESHOLD:
            try:
                m.delete(memory_id=mem_id)
                rejected.append({"id": mem_id, "memory": fact, "score": round(score, 3)})
                _log(logging.INFO, "Gate verwirft ungegroundeten Fakt", score=round(score, 3),
                     threshold=GATE_THRESHOLD, memory_id=mem_id)
            except Exception:  # noqa: BLE001
                _log(logging.WARNING, "Gate-delete fehlgeschlagen -> Fakt bleibt", memory_id=mem_id, exc_info=True)
    # result bereinigen: verworfene Fakten raus, damit der Aufrufer nur die behaltenen sieht
    if rejected and isinstance(result, dict) and isinstance(result.get("results"), list):
        rej_ids = {r["id"] for r in rejected}
        result["results"] = [it for it in result["results"] if it.get("id") not in rej_ids]
    kept = len(added) - len(rejected)
    checkpoint("quality_gate", "ungegroundete Fakten werden vor dem Speichern verworfen",
               ok=True, added=len(added), kept=kept, rejected=len(rejected), threshold=GATE_THRESHOLD)
    return {"kept": kept, "rejected": rejected, "gate": "active"}


# ---------------------------------------------------------------------------
# Mem0 initialisieren (Gemini + Qdrant) — Fehler verschlucken wir NICHT
# ---------------------------------------------------------------------------
MEM0_CONFIG = {
    "llm": {
        "provider": "gemini",
        "config": {
            "model": LLM_MODEL,
            "api_key": GEMINI_API_KEY,
            "temperature": 0.1,
            "max_tokens": LLM_MAX_TOKENS,
        },
    },
    "embedder": {
        "provider": "gemini",
        "config": {
            "model": EMBED_MODEL,
            "api_key": GEMINI_API_KEY,
            "embedding_dims": EMBED_DIMS,
        },
    },
    "vector_store": {
        "provider": "qdrant",
        "config": {
            "collection_name": COLLECTION,
            "url": QDRANT_URL,  # http://... -> kein TLS (api_key wuerde sonst https=True erzwingen)
            "api_key": QDRANT_API_KEY,
            "embedding_model_dims": EMBED_DIMS,  # MUSS == embedder embedding_dims
        },
    },
    # Strenger Extraktions-Prompt gegen Junk/Halluzinationen (mem0 hat kein Quality-Gate;
    # Audit mem0#4573 fand 97,8 % Junk). Siehe bugs/server/mem0.md §1.
    "custom_instructions": CUSTOM_INSTRUCTIONS,
}

# Startbedingungen pruefen (Sonden) — Verletzungen sind Konfig-Fehler, nicht still
probe(bool(GEMINI_API_KEY), "GEMINI_API_KEY fehlt")
probe(bool(SB_API_KEY) and len(SB_API_KEY) >= 32, "SB_API_KEY fehlt/zu kurz")
probe(bool(QDRANT_API_KEY), "QDRANT_API_KEY fehlt")
probe(EMBED_DIMS in (768, 1536, 3072), "EMBED_DIMS unerwartet (gemini-embedding-001: 768/1536/3072)", dims=EMBED_DIMS)
probe(
    MEM0_CONFIG["embedder"]["config"]["embedding_dims"]
    == MEM0_CONFIG["vector_store"]["config"]["embedding_model_dims"],
    "DIM-INVARIANTE verletzt: embedder != qdrant",
)

memory = None
init_error: str | None = None
try:
    from mem0 import Memory

    memory = Memory.from_config(MEM0_CONFIG)
    _log(logging.INFO, "Mem0 initialisiert", llm=LLM_MODEL, embedder=EMBED_MODEL, dims=EMBED_DIMS, collection=COLLECTION)
except Exception as e:  # noqa: BLE001 — bewusst breit: Init-Fehler vollstaendig festhalten
    init_error = f"{type(e).__name__}: {e}"
    log.error("Mem0-Init fehlgeschlagen", exc_info=True)

# Startup-Banner (Observability-First: Log-Pfad + Version EINMAL ausgeben)
_log(logging.INFO, "mem0-api startet", version=VERSION, log_path=LOG_PATH)

# ---------------------------------------------------------------------------
# Defense-in-Depth-Kostenbremse (harter Cap liegt bei Google AI Studio Budget)
# ---------------------------------------------------------------------------
_llm_calls = {"day": str(date.today()), "count": 0}


def _guard_llm_budget() -> None:
    today = str(date.today())
    if _llm_calls["day"] != today:
        _llm_calls["day"], _llm_calls["count"] = today, 0
    _llm_calls["count"] += 1
    if _llm_calls["count"] > MAX_LLM_CALLS_PER_DAY:
        _log(logging.ERROR, "LLM-Tages-Cap erreicht", count=_llm_calls["count"], cap=MAX_LLM_CALLS_PER_DAY)
        raise HTTPException(status_code=429, detail="Taegliches LLM-Limit erreicht (Sicherheits-Cap)")


# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------
def require_auth(authorization: str = Header(default="")) -> None:
    expected = f"Bearer {SB_API_KEY}"
    if not SB_API_KEY or authorization != expected:
        raise HTTPException(status_code=401, detail="Unauthorized")


# ---------------------------------------------------------------------------
# Request-/Response-Modelle
# ---------------------------------------------------------------------------
class StoreReq(BaseModel):
    text: str | None = Field(default=None, description="Roher Text, der gespeichert wird")
    messages: list[dict] | None = Field(default=None, description="Alternativ: Chat-Nachrichten")
    user_id: str = Field(default="frank", description="Besitzer der Erinnerung")
    metadata: dict | None = Field(default=None, description="Freie Metadaten (z.B. category)")
    infer: bool = Field(default=True, description="True = Mem0 extrahiert Fakten via LLM; False = Rohtext")


class RecallReq(BaseModel):
    query: str
    user_id: str = Field(default="frank")
    limit: int = Field(default=5, ge=1, le=50)


# ---------------------------------------------------------------------------
# App
# ---------------------------------------------------------------------------
app = FastAPI(title="Second Brain — mem0-api", version=VERSION)


@app.exception_handler(Exception)
async def unhandled(request: Request, exc: Exception) -> JSONResponse:
    """Globaler Fehler-Faenger: nichts stirbt still — voller Kontext ins Log."""
    log.error("Unbehandelte Ausnahme", exc_info=True, extra={"ctx": {"path": str(request.url.path)}})
    return JSONResponse(status_code=500, content={"error": type(exc).__name__, "detail": str(exc)})


@app.get("/health")
def health() -> dict:
    qdrant_ok = False
    detail = None
    try:
        if memory is not None:
            # leichter Lebenscheck gegen Qdrant ueber den Mem0-Client
            memory.vector_store.client.get_collections()
            qdrant_ok = True
    except Exception as e:  # noqa: BLE001
        detail = f"{type(e).__name__}: {e}"
    status = "ok" if (memory is not None and qdrant_ok) else "degraded"
    return {
        "status": status,
        "version": VERSION,
        "mem0": "ok" if memory is not None else "init_failed",
        "init_error": init_error,
        "qdrant": "reachable" if qdrant_ok else "unreachable",
        "qdrant_detail": detail,
        "llm_model": LLM_MODEL,
        "embed_model": EMBED_MODEL,
        "embed_dims": EMBED_DIMS,
        "llm_calls_today": _llm_calls["count"],
        "gate": {
            "enabled": GATE_ENABLED,
            "threshold": GATE_THRESHOLD,
            "hhem_loaded": _hhem["loaded"],
            "hhem_failed": _hhem["failed"],
        },
    }


def _require_memory():
    if memory is None:
        raise HTTPException(status_code=503, detail=f"Mem0 nicht initialisiert: {init_error}")
    return memory


@app.post("/store", dependencies=[Depends(require_auth)])
def store(req: StoreReq) -> dict:
    m = _require_memory()
    if not req.text and not req.messages:
        raise HTTPException(status_code=400, detail="text oder messages erforderlich")

    # Quality-Gate Stufe 1 — Vorfilter: offensichtlichen Nicht-Memory-Input (System-Prompt/Boot-File)
    # gar nicht erst an mem0 geben (spart sogar den LLM-Call). Nur bei Text + Extraktion.
    if GATE_ENABLED and req.infer and req.text:
        reason = _prefilter_junk(req.text)
        if reason:
            checkpoint("prefilter", "offensichtlicher Nicht-Memory-Input wird abgelehnt", ok=True,
                       reason=reason, user_id=req.user_id)
            return {"ok": True, "result": {"results": []}, "gate": {"prefiltered": reason}}

    if req.infer:
        _guard_llm_budget()  # nur bei LLM-Extraktion faellt ein Modell-Call an
    payload = req.messages if req.messages else req.text
    t0 = time.time()
    result = m.add(payload, user_id=req.user_id, metadata=req.metadata or {}, infer=req.infer)

    # Quality-Gate Stufe 2 — Grounding: ungegroundete (halluzinierte) Fakten wieder verwerfen.
    # Nur bei infer=True (Rohtext-Pfad hat keine Extraktion -> nichts zu pruefen).
    gate_info = None
    if req.infer:
        source = req.text or json.dumps(req.messages, ensure_ascii=False)
        gate_info = _apply_gate(source, result, m)

    added = len((result or {}).get("results", []) if isinstance(result, dict) else result or [])
    rejected = len((gate_info or {}).get("rejected", []))
    checkpoint("store", "Eingabe wird als Erinnerung persistiert", ok=added >= 0,
               user_id=req.user_id, infer=req.infer, added=added, rejected=rejected,
               ms=int((time.time() - t0) * 1000))
    return {"ok": True, "result": result, "gate": gate_info}


@app.post("/recall", dependencies=[Depends(require_auth)])
def recall(req: RecallReq) -> dict:
    m = _require_memory()
    _guard_llm_budget()  # Suche embeddet die Query (Embedding-Call)
    t0 = time.time()
    result = m.search(req.query, filters={"user_id": req.user_id}, top_k=req.limit)
    hits = (result or {}).get("results", []) if isinstance(result, dict) else (result or [])
    checkpoint("recall", "Anfrage liefert relevante Erinnerungen zurueck", ok=isinstance(hits, list),
               user_id=req.user_id, query_len=len(req.query), hits=len(hits), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "count": len(hits), "result": result}


@app.get("/memories", dependencies=[Depends(require_auth)])
def memories(user_id: str = "frank") -> dict:
    m = _require_memory()
    result = m.get_all(filters={"user_id": user_id})
    items = (result or {}).get("results", []) if isinstance(result, dict) else (result or [])
    return {"ok": True, "count": len(items), "result": result}


@app.get("/")
def root() -> dict:
    return {"service": "Second Brain — mem0-api", "version": VERSION, "endpoints": ["/health", "/store", "/recall", "/memories"]}
