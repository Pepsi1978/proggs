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

VERSION = "0.3.0"  # 0.3.0: Chat-Tab — /api/chat proxied an den Agenten (store/recall) via asyncio.to_thread (kein Event-Loop-Block, bugs/server/fastapi.md §1); api_put_prompt/_config ebenfalls nicht-blockierend. 0.2.1: Einstellungen-Tab (Prompt-Editor + Modell-Wahl)

BRAIN_URL = os.getenv("BRAIN_URL", "http://brain-api:8000").rstrip("/")
AGENT_URL = os.getenv("AGENT_URL", "http://agent:8002").rstrip("/")
SB_API_KEY = os.getenv("SB_API_KEY", "")
USER_ID = os.getenv("SB_USER_ID", "frank")
HOSTFS = os.getenv("DASH_HOSTFS", "/hostfs")
CONV_CATEGORY = os.getenv("DASH_CONV_CATEGORY", "gespraeche")
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


@app.get("/api/logbook")
def logbook() -> dict:
    try:
        d = _bget("/by-category", category=CONV_CATEGORY, user_id=USER_ID)
        return {"items": d.get("items", [])}
    except Exception as e:  # noqa: BLE001
        return {"items": [], "detail": str(e)}


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


@app.get("/", response_class=HTMLResponse)
def index() -> str:
    return (STATIC / "index.html").read_text(encoding="utf-8")
