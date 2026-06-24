"""
sb-dashboard — privates Web-Cockpit fuer Franks "zweites Gehirn".
Erreichbar NUR ueber den WireGuard-Tunnel (10.8.0.1:8003), nicht oeffentlich. Der Tunnel IST der
Schutz (wie brain-api/agent). Das Backend traegt Gehirn-, Agent- und Server-Infos zusammen und
liefert die Oberflaeche (static/index.html). Observability-First: schlankes JSON-Log + Fehler-Faenger.
"""
from __future__ import annotations

import asyncio
import json
import logging
import os
import time
import traceback
from collections import Counter
from pathlib import Path

import httpx
import psutil
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, JSONResponse

VERSION = "0.4.0"  # 0.4.0: Eintrags-Editor (PUT /api/entry -> brain), Mikrofon-STT (POST /api/transcribe -> Groq whisper-large-v3-turbo), Prompt-Verbesserung (POST /api/improve -> agent), Logbuch liest die .txt-Protokolle von der Samba-Platte (Z) mit Gehirn-Fallback. 0.3.0: Chat-Tab — /api/chat proxied an den Agenten (store/recall) via asyncio.to_thread (kein Event-Loop-Block, bugs/server/fastapi.md §1). 0.2.1: Einstellungen-Tab (Prompt-Editor + Modell-Wahl)

BRAIN_URL = os.getenv("BRAIN_URL", "http://brain-api:8000").rstrip("/")
AGENT_URL = os.getenv("AGENT_URL", "http://agent:8002").rstrip("/")
SB_API_KEY = os.getenv("SB_API_KEY", "")
USER_ID = os.getenv("SB_USER_ID", "frank")
HOSTFS = os.getenv("DASH_HOSTFS", "/hostfs")
CONV_CATEGORY = os.getenv("DASH_CONV_CATEGORY", "gespraeche")
# Logbuch-.txt-Protokolle (Samba-Platte = Franks Z:), read-only ins dashboard gemountet.
LOGBOOK_DIR = os.getenv("DASH_LOGBOOK_DIR", os.getenv("AGENT_LOGBOOK_DIR", "/logbook"))
# Groq Whisper (Sprache->Text) — Key fest im Server (.env). whisper-large-v3-turbo wie im Voice-Overlay.
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GROQ_STT_URL = os.getenv("GROQ_STT_URL", "https://api.groq.com/openai/v1/audio/transcriptions")
GROQ_STT_MODEL = os.getenv("GROQ_STT_MODEL", "whisper-large-v3-turbo")
MAX_AUDIO_BYTES = int(os.getenv("DASH_MAX_AUDIO_BYTES", str(24 * 1024 * 1024)))  # Groq-Limit ~24 MB (Almanach §4)
STATIC = Path(__file__).parent / "static"
HEADERS = {"Authorization": f"Bearer {SB_API_KEY}", "Content-Type": "application/json"}

logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("sb-dashboard")


def _log(level: int, msg: str, **ctx) -> None:
    try:
        log.log(level, json.dumps({"ts": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime()),
                                   "module": "sb-dashboard", "msg": msg, "ctx": ctx}, ensure_ascii=False))
    except Exception:  # noqa: BLE001
        pass


def _bget(path: str, **params):
    r = httpx.get(f"{BRAIN_URL}{path}", params=params or None, headers=HEADERS, timeout=20.0)
    r.raise_for_status()
    return r.json()


def _bpost(path: str, payload: dict):
    r = httpx.post(f"{BRAIN_URL}{path}", json=payload, headers=HEADERS, timeout=40.0)
    r.raise_for_status()
    return r.json()


def _bput(path: str, payload: dict):
    # 60s: ein Eintrags-Ersatz re-embedded den ganzen Text (mehrere Chunks moeglich).
    r = httpx.put(f"{BRAIN_URL}{path}", json=payload, headers=HEADERS, timeout=60.0)
    r.raise_for_status()
    return r.json()


def _aget(path: str):
    r = httpx.get(f"{AGENT_URL}{path}", headers=HEADERS, timeout=15.0)
    r.raise_for_status()
    return r.json()


def _aput(path: str, payload: dict):
    r = httpx.put(f"{AGENT_URL}{path}", json=payload, headers=HEADERS, timeout=20.0)
    r.raise_for_status()
    return r.json()


def _apost(path: str, payload: dict):
    # 60s: ein recall macht ZWEI LLM-Aufrufe (entscheiden + antworten) — grosszuegig timen.
    r = httpx.post(f"{AGENT_URL}{path}", json=payload, headers=HEADERS, timeout=60.0)
    r.raise_for_status()
    return r.json()


app = FastAPI(title="Second Brain — Dashboard", version=VERSION)
_log(logging.INFO, "sb-dashboard gestartet", version=VERSION, brain_url=BRAIN_URL, agent_url=AGENT_URL)


@app.exception_handler(Exception)
async def unhandled(request: Request, exc: Exception) -> JSONResponse:
    log.error(json.dumps({"module": "sb-dashboard", "msg": "Unbehandelte Ausnahme",
                          "path": str(request.url.path), "trace": traceback.format_exc()}, ensure_ascii=False))
    return JSONResponse(status_code=500, content={"error": type(exc).__name__, "detail": str(exc)})


@app.get("/api/health")
def health() -> dict:
    return {"status": "ok", "version": VERSION}


@app.get("/api/overview")
def overview() -> dict:
    out: dict = {"brain": None, "agent": None, "server": None, "categories": [], "total": None}
    try:
        h = _bget("/health")
        out["brain"] = {"status": h.get("status"), "points": h.get("points"),
                        "version": h.get("version"), "embed_model": h.get("embed_model")}
    except Exception as e:  # noqa: BLE001
        out["brain"] = {"status": "error", "detail": str(e)}
    try:
        lst = _bget("/list", user_id=USER_ID, limit=2000)
        items = lst.get("items", [])
        c = Counter((it.get("category") or "(ohne)") for it in items)
        out["categories"] = sorted(({"name": k, "count": v} for k, v in c.items()), key=lambda x: -x["count"])
        out["total"] = lst.get("count", len(items))
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorien-Abruf fehlgeschlagen", err=str(e))
    try:
        a = httpx.get(f"{AGENT_URL}/health", timeout=8.0).json()
        out["agent"] = {"status": a.get("status"), "version": a.get("version"),
                        "model": a.get("model"), "sessions": a.get("aktive_sitzungen")}
    except Exception:  # noqa: BLE001
        out["agent"] = {"status": "offline"}
    try:
        root = HOSTFS if os.path.exists(HOSTFS) else "/"
        du = psutil.disk_usage(root)
        vm = psutil.virtual_memory()
        # Frontend (Cortex) erwartet Speicher/Disk in MB (fmtBytes rechnet MB->GB).
        MB = 1024 * 1024
        out["server"] = {"cpu_pct": psutil.cpu_percent(interval=0.25),
                         "mem_used": vm.used // MB, "mem_total": vm.total // MB, "mem_pct": vm.percent,
                         "disk_used": du.used // MB, "disk_total": du.total // MB, "disk_pct": du.percent}
    except Exception as e:  # noqa: BLE001
        out["server"] = {"detail": str(e)}
    return out


@app.get("/api/entries")
def entries(q: str = "", category: str = "", limit: int = 40) -> dict:
    limit = max(1, min(limit, 200))
    if category.strip():
        d = _bget("/by-category", category=category.strip(), user_id=USER_ID)
        return {"mode": "category", "items": d.get("items", [])[:limit]}
    if q.strip():
        d = _bpost("/search", {"query": q.strip(), "user_id": USER_ID, "limit": limit})
        return {"mode": "search", "items": d.get("items", [])}
    d = _bget("/list", user_id=USER_ID, limit=limit)
    return {"mode": "list", "items": d.get("items", [])}


@app.get("/api/entry")
def entry(title: str) -> dict:
    return _bget("/by-title", title=title, user_id=USER_ID)


def _logbook_when(stem: str, txt: str) -> str:
    """Datum/Uhrzeit eines Logbuch-Protokolls — aus der Kopfzeile 'Datum/Uhrzeit: ...', sonst der
    Dateiname (der ist bereits 'TT.MM.JJJJ - H.MM Uhr')."""
    for line in txt.splitlines()[:5]:
        low = line.lower()
        if low.startswith("datum") and ":" in line:
            return line.split(":", 1)[1].strip()
    return stem


@app.get("/api/logbook")
def logbook() -> dict:
    """Logbuch = die .txt-Gespraechsprotokolle auf der Samba-Platte (Franks Z:), gemountet unter
    LOGBOOK_DIR (JJJJ/MM/*.txt) — das ist die Quelle, die Frank tatsaechlich sieht. Neueste zuerst.
    Fallback auf die Gehirn-Kategorie 'gespraeche', falls kein Ordner/keine .txt vorliegt. Sync def
    (Datei-I/O) -> Threadpool (fastapi §1)."""
    items: list[dict] = []
    try:
        base = Path(LOGBOOK_DIR)
        if base.is_dir():
            files = sorted((p for p in base.rglob("*.txt") if p.is_file()),
                           key=lambda p: p.stat().st_mtime, reverse=True)[:80]
            for p in files:
                try:
                    txt = p.read_text(encoding="utf-8")
                except Exception:  # noqa: BLE001 — eine kaputte Datei darf das Logbuch nicht killen
                    txt = ""
                items.append({"title": p.stem, "when": _logbook_when(p.stem, txt), "text": txt})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Logbuch-Ordner nicht lesbar", err=str(e), dir=LOGBOOK_DIR)
    if not items:  # Fallback: aus dem Gehirn (Kategorie gespraeche)
        try:
            d = _bget("/by-category", category=CONV_CATEGORY, user_id=USER_ID)
            for it in d.get("items", []):
                items.append({"title": it.get("title") or "Gespraech",
                              "when": it.get("updated_at") or "", "text": it.get("text") or ""})
        except Exception as e:  # noqa: BLE001
            _log(logging.WARNING, "Logbuch-Gehirn-Fallback fehlgeschlagen", err=str(e))
    _log(logging.INFO, "Logbuch geladen", count=len(items), quelle=("datei" if items else "leer"))
    return {"items": items}


# --- Einstellungen: Proxy an den Agenten (System-Prompt + Modell-Wahl) ------
@app.get("/api/prompt")
def api_get_prompt() -> dict:
    return _aget("/prompt")


@app.put("/api/prompt")
async def api_put_prompt(request: Request) -> dict:
    body = await request.json()
    # sync httpx via to_thread -> blockiert den Event-Loop nicht (bugs/server/fastapi.md §1)
    return await asyncio.to_thread(_aput, "/prompt", {"instructions": body.get("instructions", "")})


@app.get("/api/config")
def api_get_config() -> dict:
    return _aget("/config")


@app.put("/api/config")
async def api_put_config(request: Request) -> dict:
    body = await request.json()
    return await asyncio.to_thread(_aput, "/config", {"model": body.get("model", "")})


# --- Chat: Proxy an den Agenten (ablegen ODER nachschlagen) ------------------
@app.post("/api/chat")
async def api_chat(request: Request) -> dict:
    """Ein Eingang zum Bibliothekar-Agenten: store/ask/recall/smalltalk entscheidet der Agent.
    Der synchrone httpx-Call laeuft via asyncio.to_thread, damit ein langer recall (zwei
    LLM-Aufrufe, bis ~60s) den Event-Loop NICHT blockiert (bugs/server/fastapi.md §1)."""
    body = await request.json()
    text = (body.get("text") or "").strip()
    if not text:
        return {"ok": False, "reply": "Leere Nachricht — schreib mir was zum Ablegen oder eine Frage."}
    if len(text) > 8000:   # Eingabe-Limit (fastapi §8) — eine Chat-Nachricht ist nie so lang
        text = text[:8000]
    payload = {"text": text, "user_id": USER_ID}
    sid = (body.get("session_id") or "").strip()
    if sid:
        payload["session_id"] = sid
    try:
        return await asyncio.to_thread(_apost, "/chat", payload)
    except Exception as e:  # noqa: BLE001 — Agent offline/Fehler: sauberer Fehler statt 500
        _log(logging.WARNING, "Chat-Proxy fehlgeschlagen", err=str(e))
        return {"ok": False, "reply": "Der Agent ist gerade nicht erreichbar — versuch es bitte gleich nochmal."}


# --- Eintrag editieren: alten Vektor 1:1 durch neuen Text ersetzen (Proxy an brain PUT /entry) ----
@app.put("/api/entry")
async def api_put_entry(request: Request) -> dict:
    """Ersetzt einen Gehirn-Eintrag (per doc_id) durch neuen Text. Der sync httpx-Call laeuft via
    asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    text = (body.get("text") or "").strip()
    if not doc_id or not text:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id und text erforderlich"})
    try:
        return await asyncio.to_thread(_bput, "/entry", {"doc_id": doc_id, "text": text, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eintrag-Ersatz fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Speichern fehlgeschlagen: {type(e).__name__}"})


# --- Prompt verbessern (G-Button): Text sprachlich verbessern (Proxy an agent /improve) ------------
@app.post("/api/improve")
async def api_improve(request: Request) -> dict:
    body = await request.json()
    text = (body.get("text") or "").strip()
    if not text:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Text"})
    if len(text) > 8000:
        text = text[:8000]
    try:
        return await asyncio.to_thread(_apost, "/improve", {"text": text})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Verbesserung fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Verbesserung fehlgeschlagen: {type(e).__name__}"})


# --- Sprache -> Text (Mikrofon): Audio-Body -> Groq Whisper. Browser schickt das Audio-Blob als ROHEN
#     Body (kein python-multipart noetig); wir bauen das multipart erst hier fuer Groq. --------------
def _groq_transcribe(audio: bytes, content_type: str) -> dict:
    files = {"file": ("audio.webm", audio, content_type or "audio/webm")}
    data = {"model": GROQ_STT_MODEL, "language": "de", "response_format": "json"}
    r = httpx.post(GROQ_STT_URL, files=files, data=data,
                   headers={"Authorization": f"Bearer {GROQ_API_KEY}"}, timeout=90.0)
    r.raise_for_status()
    return r.json()


@app.post("/api/transcribe")
async def api_transcribe(request: Request) -> dict:
    """Audio (roher Request-Body, z.B. audio/webm) -> Groq whisper-large-v3-turbo -> deutscher Text.
    GROQ_API_KEY fest im Server (.env). Body-Limit gegen OOM (fastapi §8); sync httpx via to_thread."""
    if not GROQ_API_KEY:
        return JSONResponse(status_code=503, content={"ok": False, "detail": "GROQ_API_KEY fehlt im Server"})
    audio = await request.body()
    if not audio:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Audio empfangen"})
    if len(audio) > MAX_AUDIO_BYTES:
        return JSONResponse(status_code=413, content={"ok": False, "detail": "Audio zu gross (>24 MB)"})
    ctype = request.headers.get("content-type", "audio/webm")
    try:
        out = await asyncio.to_thread(_groq_transcribe, audio, ctype)
        return {"ok": True, "text": (out.get("text") or "").strip()}
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Transkription fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Transkription fehlgeschlagen: {type(e).__name__}"})


@app.get("/", response_class=HTMLResponse)
def index() -> str:
    return (STATIC / "index.html").read_text(encoding="utf-8")
