"""
sb-mcp — MCP-Server fuer Franks "zweites Gehirn".

Macht die mem0-api (store/recall/list) als MCP-Werkzeuge verfuegbar, damit Claude Code
UND OpenCode das Gehirn automatisch nutzen koennen.

Transport:  Streamable HTTP (Best Practice fuer Remote; NIE SSE), gebunden an die
            WireGuard-IP -> nur ueber den VPN-Tunnel erreichbar.
Sicherheit: kein oeffentlicher Port (Docker published nur an 10.8.0.1). DNS-Rebinding-Schutz
            SAUBER via TransportSecuritySettings(allowed_hosts=[10.8.0.1...]) statt per host=0.0.0.0
            abzuschalten (ab mcp 1.23 greift der Auto-Schutz; Almanach mcp-server.md §3.13). So bleibt
            der Schutz AKTIV und der VPN-Zugriff ueber 10.8.0.1 funktioniert. Defensiv: faellt die
            SDK-API anders aus, Fallback auf das bisherige host=0.0.0.0-Verhalten (Container crasht nie).
            OAuth unnoetig: der WireGuard-Tunnel ist der private Kanal (siehe bugs/server/wireguard.md §2).
Observability-First: strukturiertes JSON-Logging auf stderr (docker logs sb-mcp), globaler
            Fehler-Faenger pro Werkzeug (nichts stirbt still), Start-Banner, Logik-Sonde.

Plan/Doku: best-practices/second-brain/UMSETZUNGSPLAN.md
"""
# WICHTIG: KEIN "from __future__ import annotations" — das macht Annotationen zu Strings (PEP 563),
# woran FastMCP 1.12.4 beim Tool-Registrieren scheitert (issubclass(str, Context) -> TypeError).
import json
import logging
import os
import sys
import time
import traceback
from typing import Any

import httpx
from mcp.server.fastmcp import FastMCP

VERSION = "0.1.0"

# ---------------------------------------------------------------------------
# Konfiguration (alles aus Umgebungsvariablen — Secrets nie im Code)
# ---------------------------------------------------------------------------
BRAIN_URL = os.getenv("BRAIN_URL", "http://mem0-api:8000").rstrip("/")  # interner Compose-DNS
SB_API_KEY = os.getenv("SB_API_KEY", "")
USER_ID = os.getenv("SB_USER_ID", "frank")
MCP_HOST = os.getenv("MCP_HOST", "0.0.0.0")
MCP_PORT = int(os.getenv("MCP_PORT", "8001"))

# ---------------------------------------------------------------------------
# Strukturiertes JSON-Logging auf stderr (Docker captured stderr -> `docker logs sb-mcp`)
# ---------------------------------------------------------------------------
class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        entry = {
            "ts": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(record.created)),
            "level": record.levelname,
            "module": "sb-mcp",
            "fn": record.funcName,
            "msg": record.getMessage(),
        }
        if isinstance(getattr(record, "ctx", None), dict):
            entry["ctx"] = record.ctx
        if record.exc_info:
            entry["trace"] = "".join(traceback.format_exception(*record.exc_info))
        return json.dumps(entry, ensure_ascii=False)


log = logging.getLogger("sb-mcp")
log.setLevel(logging.INFO)
_h = logging.StreamHandler(sys.stderr)
_h.setFormatter(JsonFormatter())
log.addHandler(_h)


def _log(level: int, msg: str, **ctx: Any) -> None:
    log.log(level, msg, extra={"ctx": ctx} if ctx else None)


def probe(condition: bool, msg: str, **ctx: Any) -> bool:
    """Logik-Sonde: prueft eine Annahme, loggt WARN bei Verletzung, crasht NIE."""
    if not condition:
        _log(logging.WARNING, f"PROBE verletzt: {msg}", **ctx)
    return condition


probe(bool(SB_API_KEY) and len(SB_API_KEY) >= 32, "SB_API_KEY fehlt/zu kurz")

HEADERS = {"Authorization": f"Bearer {SB_API_KEY}", "Content-Type": "application/json"}

def _build_mcp() -> FastMCP:
    """DNS-Rebinding-Schutz sauber via allowed_hosts (Almanach mcp-server.md §3.13): ab mcp 1.23 greift
    der Auto-Schutz; statt ihn per host=0.0.0.0 abzuschalten, erlauben wir die WireGuard-IP explizit ->
    Schutz AKTIV + VPN-Zugriff ok. Defensiv: faellt die SDK-API anders aus, Fallback auf host=0.0.0.0
    (Security-Config darf den Container NIE crashen, Best-Practice D5/D1)."""
    try:
        from mcp.server.transport_security import TransportSecuritySettings
        ts = TransportSecuritySettings(
            enable_dns_rebinding_protection=True,
            allowed_hosts=[f"10.8.0.1:{MCP_PORT}", "10.8.0.1", f"127.0.0.1:{MCP_PORT}", f"localhost:{MCP_PORT}"],
        )
        m = FastMCP("second-brain", host=MCP_HOST, port=MCP_PORT, transport_security=ts)
        _log(logging.INFO, "MCP mit TransportSecuritySettings gestartet", allowed_host=f"10.8.0.1:{MCP_PORT}")
        return m
    except Exception as e:  # noqa: BLE001 — Security-Config darf den Container NIE crashen
        _log(logging.WARNING, "TransportSecuritySettings nicht gesetzt -> Fallback host=0.0.0.0",
             err=f"{type(e).__name__}: {e}")
        return FastMCP("second-brain", host=MCP_HOST, port=MCP_PORT)


mcp = _build_mcp()


# ---------------------------------------------------------------------------
# HTTP-Helfer gegen die mem0-api (Fehler werden geloggt, NICHT verschluckt)
# ---------------------------------------------------------------------------
def _post(path: str, payload: dict) -> dict:
    r = httpx.post(f"{BRAIN_URL}{path}", json=payload, headers=HEADERS, timeout=90.0)
    r.raise_for_status()
    return r.json()


def _get(path: str, params: dict | None = None) -> dict:
    r = httpx.get(f"{BRAIN_URL}{path}", params=params, headers=HEADERS, timeout=30.0)
    r.raise_for_status()
    return r.json()


def _extract(result: Any) -> list:
    """mem0 liefert mal {'results':[...]}, mal eine Liste — beides robust behandeln."""
    if isinstance(result, dict):
        return result.get("results", []) or []
    if isinstance(result, list):
        return result
    return []


# ---------------------------------------------------------------------------
# MCP-Werkzeuge (das, was Claude Code / OpenCode aufrufen koennen)
# Wenige Workflow-Tools statt API-1:1-Wrapper (Best Practice C1), snake_case (B6).
# ---------------------------------------------------------------------------
@mcp.tool()
def remember(text: str, category: str = "") -> str:
    """Speichere eine Information dauerhaft im zweiten Gehirn. Mem0 extrahiert selbst die
    wichtigen Fakten aus dem Text. Optional eine Kategorie (z.B. 'projekt', 'gesundheit')."""
    meta = {"category": category} if category.strip() else None
    try:
        data = _post("/store", {"text": text, "user_id": USER_ID, "metadata": meta, "infer": True})
    except Exception as e:  # noqa: BLE001 — bewusst breit: dem Aufrufer eine klare Meldung geben
        _log(logging.ERROR, "remember fehlgeschlagen", exc_info=True)
        return f"Fehler beim Speichern: {type(e).__name__}: {e}"
    added = _extract(data.get("result"))
    _log(logging.INFO, "remember ok", added=len(added), category=category or None)
    if not added:
        return "Gespeichert (Mem0 hat keine neuen Fakten extrahiert — evtl. schon bekannt)."
    facts = "; ".join(str(a.get("memory", a)) for a in added if isinstance(a, dict)) or str(added)
    return f"Gespeichert ({len(added)} Fakt(en)): {facts}"


@mcp.tool()
def recall(query: str, limit: int = 5) -> str:
    """Finde relevante Erinnerungen im zweiten Gehirn zu einer Frage oder einem Stichwort."""
    limit = max(1, min(limit, 50))
    try:
        data = _post("/recall", {"query": query, "user_id": USER_ID, "limit": limit})
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "recall fehlgeschlagen", exc_info=True)
        return f"Fehler beim Abrufen: {type(e).__name__}: {e}"
    hits = _extract(data.get("result"))
    _log(logging.INFO, "recall ok", query_len=len(query), hits=len(hits))
    if not hits:
        return "Keine passenden Erinnerungen gefunden."
    lines = []
    for i, h in enumerate(hits, 1):
        mem = h.get("memory", h) if isinstance(h, dict) else h
        score = h.get("score") if isinstance(h, dict) else None
        suffix = f"  (Relevanz {score:.2f})" if isinstance(score, (int, float)) else ""
        lines.append(f"{i}. {mem}{suffix}")
    return "\n".join(lines)


@mcp.tool()
def list_memories(limit: int = 50) -> str:
    """Liste die gespeicherten Erinnerungen im zweiten Gehirn auf (Standard: bis zu 50,
    damit die Antwort das Tool-Budget nicht sprengt)."""
    limit = max(1, min(limit, 200))
    try:
        data = _get("/memories", {"user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "list_memories fehlgeschlagen", exc_info=True)
        return f"Fehler beim Auflisten: {type(e).__name__}: {e}"
    items = _extract(data.get("result"))
    if not items:
        return "Das zweite Gehirn ist noch leer."
    total = len(items)
    shown = items[:limit]
    lines = [f"{i}. {(it.get('memory', it) if isinstance(it, dict) else it)}" for i, it in enumerate(shown, 1)]
    head = f"{total} Erinnerung(en)" + (f" (zeige die ersten {limit})" if total > limit else "") + ":"
    return head + "\n" + "\n".join(lines)


@mcp.tool()
def brain_health() -> str:
    """Pruefe den Zustand des zweiten Gehirns (Verbindung, Modell, Anzahl heutiger Anfragen)."""
    try:
        data = _get("/health")
    except Exception as e:  # noqa: BLE001
        return f"Gehirn nicht erreichbar: {type(e).__name__}: {e}"
    return json.dumps(data, ensure_ascii=False, indent=2)


# ---------------------------------------------------------------------------
# Start
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    _log(logging.INFO, "sb-mcp startet", version=VERSION, brain_url=BRAIN_URL,
         host=MCP_HOST, port=MCP_PORT, user=USER_ID)
    mcp.run(transport="streamable-http")
