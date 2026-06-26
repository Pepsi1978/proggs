"""
sb-mcp — MCP-Server fuer Franks "zweites Gehirn" (1:1-Speicher).

Macht die brain-api (store/by-title/by-category/search/list/forget) als MCP-Werkzeuge verfuegbar,
damit Claude Code UND OpenCode das Gehirn automatisch nutzen koennen. Der Speicher ist wortwoertlich
1:1 (keine KI im Speicher) — diese Werkzeuge spiegeln genau dieses Schema.

Transport:  Streamable HTTP (Best Practice fuer Remote; NIE SSE), gebunden an die WireGuard-IP.
Sicherheit: kein oeffentlicher Port (Docker published nur an 10.8.0.1). DNS-Rebinding-Schutz SAUBER
            via TransportSecuritySettings(allowed_hosts=[10.8.0.1...]) (Almanach mcp-server.md §3.13);
            defensiver Fallback auf host=0.0.0.0, falls die SDK-API anders ausfaellt (crasht nie).
Observability-First: strukturiertes JSON-Logging auf stderr (docker logs sb-mcp), Fehler-Faenger pro
            Werkzeug (nichts stirbt still), Start-Banner, Logik-Sonde.

Plan/Doku: best-practices/second-brain/UMSETZUNGSPLAN.md
"""
# WICHTIG: KEIN "from __future__ import annotations" — das macht Annotationen zu Strings (PEP 563),
# woran FastMCP beim Tool-Registrieren scheitert (issubclass(str, Context) -> TypeError; Almanach §3.11/#16).
import json
import logging
import os
import sys
import time
import traceback
from typing import Any

import httpx
from mcp.server.fastmcp import FastMCP

VERSION = "1.2.0"  # 1.2.0: list_memories warnt bei ready=false (Gehirn laedt nach Neustart noch -> Liste evtl. unvollstaendig; Frank 2026-06-26). 1.1.0: recall um Payload-Filter (category/date/date_from/date_to). 1.0.0: 1:1-Schema (mem0 raus).

# ---------------------------------------------------------------------------
# Konfiguration (alles aus Umgebungsvariablen — Secrets nie im Code)
# ---------------------------------------------------------------------------
BRAIN_URL = os.getenv("BRAIN_URL", "http://brain-api:8000").rstrip("/")  # interner Compose-DNS (Service heisst brain-api)
SB_API_KEY = os.getenv("SB_API_KEY", "")
USER_ID = os.getenv("SB_USER_ID", "frank")
MCP_HOST = os.getenv("MCP_HOST", "0.0.0.0")
MCP_PORT = int(os.getenv("MCP_PORT", "8001"))

# ---------------------------------------------------------------------------
# Strukturiertes JSON-Logging auf stderr (Docker captured stderr -> `docker logs sb-mcp`)
# stdout bleibt frei (bei HTTP unkritisch, aber Disziplin; Almanach §1.1).
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
    """DNS-Rebinding-Schutz sauber via allowed_hosts (Almanach mcp-server.md §3.13): statt ihn per
    host=0.0.0.0 abzuschalten, erlauben wir die WireGuard-IP explizit -> Schutz AKTIV + VPN-Zugriff ok.
    Defensiv: faellt die SDK-API anders aus, Fallback auf host=0.0.0.0 (Security-Config crasht nie)."""
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
# HTTP-Helfer gegen die brain-api (Fehler werden geloggt, NICHT verschluckt)
# ---------------------------------------------------------------------------
def _post(path: str, payload: dict) -> dict:
    r = httpx.post(f"{BRAIN_URL}{path}", json=payload, headers=HEADERS, timeout=120.0)
    r.raise_for_status()
    return r.json()


def _get(path: str, params: dict | None = None) -> dict:
    r = httpx.get(f"{BRAIN_URL}{path}", params=params, headers=HEADERS, timeout=30.0)
    r.raise_for_status()
    return r.json()


def _delete(path: str, params: dict | None = None) -> dict:
    r = httpx.delete(f"{BRAIN_URL}{path}", params=params, headers=HEADERS, timeout=30.0)
    r.raise_for_status()
    return r.json()


# ---------------------------------------------------------------------------
# MCP-Werkzeuge (was Claude Code / OpenCode aufrufen koennen)
# Wenige klare Workflow-Tools, snake_case (Best Practice B6/C1). Speicher ist 1:1 — kein Extrahieren.
# ---------------------------------------------------------------------------
@mcp.tool()
def remember(text: str, title: str = "", category: str = "") -> str:
    """Speichere einen Text WORTWOERTLICH 1:1 im zweiten Gehirn. Der Text wird unveraendert abgelegt,
    keine KI bearbeitet ihn. Optional ein Titel (gleicher Titel ERSETZT den alten Eintrag — so
    aktualisierst du z.B. 'Direktive 1') und eine Kategorie (z.B. 'mentals', 'direktiven')."""
    payload = {"text": text, "user_id": USER_ID}
    if title.strip():
        payload["title"] = title.strip()
    if category.strip():
        payload["category"] = category.strip()
    try:
        data = _post("/store", payload)
    except Exception as e:  # noqa: BLE001 — dem Aufrufer eine klare Meldung geben
        _log(logging.ERROR, "remember fehlgeschlagen", exc_info=True)
        return f"Fehler beim Speichern: {type(e).__name__}: {e}"
    _log(logging.INFO, "remember ok", title=title or None, category=category or None,
         chars=data.get("chars"), replaced=data.get("replaced"))
    bits = []
    if data.get("title"):
        bits.append(f"Titel '{data['title']}'")
    if data.get("category"):
        bits.append(f"Kategorie '{data['category']}'")
    bits.append(f"{data.get('chars', '?')} Zeichen")
    suffix = " (alte Version ersetzt)" if data.get("replaced") else ""
    return f"Gespeichert 1:1: {', '.join(bits)}{suffix}."


@mcp.tool()
def recall(query: str, limit: int = 5, category: str = "", date: str = "",
           date_from: str = "", date_to: str = "") -> str:
    """Finde per Themensuche relevante Eintraege im zweiten Gehirn. Zeigt die passendsten Treffer
    mit Titel, Relevanz und dem Treffer-Abschnitt. Das GANZE Dokument holst du mit get_by_title.
    Optional erst eingrenzen, dann suchen: category (nur diese Kategorie), date (nur dieser Tag,
    YYYY-MM-DD) ODER date_from/date_to (Zeitraum, YYYY-MM-DD)."""
    limit = max(1, min(limit, 50))
    payload: dict[str, Any] = {"query": query, "user_id": USER_ID, "limit": limit}
    if category.strip():
        payload["category"] = category.strip()
    if date.strip():
        payload["date"] = date.strip()
    if date_from.strip():
        payload["date_from"] = date_from.strip()
    if date_to.strip():
        payload["date_to"] = date_to.strip()
    try:
        data = _post("/search", payload)
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "recall fehlgeschlagen", exc_info=True)
        return f"Fehler beim Suchen: {type(e).__name__}: {e}"
    items = data.get("items", [])
    _log(logging.INFO, "recall ok", query_len=len(query), hits=len(items))
    if not items:
        return "Keine passenden Eintraege gefunden."
    lines = []
    for i, it in enumerate(items, 1):
        title = it.get("title") or "(ohne Titel)"
        score = it.get("score")
        rel = f"  (Relevanz {score:.2f})" if isinstance(score, (int, float)) else ""
        cat = f" [{it['category']}]" if it.get("category") else ""
        snippet = (it.get("match") or it.get("text") or "").strip()
        lines.append(f"{i}. {title}{cat}{rel}\n   {snippet}")
    return "\n".join(lines) + "\n\n(Das ganze Dokument: get_by_title mit dem Titel.)"


@mcp.tool()
def get_by_title(title: str) -> str:
    """Hole einen Eintrag EXAKT per Titel — liefert das GANZE Dokument 1:1 (unveraendert) zurueck."""
    try:
        data = _get("/by-title", {"title": title, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "get_by_title fehlgeschlagen", exc_info=True)
        return f"Fehler beim Abrufen: {type(e).__name__}: {e}"
    if not data.get("found"):
        return f"Kein Eintrag mit Titel '{title}'."
    cat = f" [{data['category']}]" if data.get("category") else ""
    return f"{title}{cat}:\n\n{data.get('text', '')}"


@mcp.tool()
def get_by_category(category: str) -> str:
    """Liste alle Eintraege einer Kategorie (z.B. 'mentals') — jeweils der volle Text 1:1."""
    try:
        data = _get("/by-category", {"category": category, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "get_by_category fehlgeschlagen", exc_info=True)
        return f"Fehler beim Abrufen: {type(e).__name__}: {e}"
    items = data.get("items", [])
    if not items:
        return f"Keine Eintraege in Kategorie '{category}'."
    blocks = []
    for it in items:
        title = it.get("title") or "(ohne Titel)"
        blocks.append(f"### {title}\n{it.get('text', '')}")
    return f"{len(items)} Eintrag(e) in '{category}':\n\n" + "\n\n".join(blocks)


@mcp.tool()
def get_by_date(date: str) -> str:
    """Liste alle Eintraege, die an einem bestimmten Tag gespeichert wurden. Datum als JJJJ-MM-TT
    (z.B. 2026-06-23). Zeigt Titel + Kategorie der an dem Tag abgelegten Eintraege."""
    try:
        data = _get("/by-date", {"date": date, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "get_by_date fehlgeschlagen", exc_info=True)
        return f"Fehler beim Abrufen: {type(e).__name__}: {e}"
    items = data.get("items", [])
    if not items:
        return f"Keine Eintraege am {date} gespeichert."
    lines = []
    for i, it in enumerate(items, 1):
        title = it.get("title") or "(ohne Titel)"
        cat = f" [{it['category']}]" if it.get("category") else ""
        lines.append(f"{i}. {title}{cat}")
    return f"{len(items)} Eintrag(e) am {date} gespeichert:\n" + "\n".join(lines)


@mcp.tool()
def list_memories(limit: int = 200) -> str:
    """Liste die gespeicherten Eintraege (Titel, Kategorie, Groesse) — ohne die Volltexte (kompakt)."""
    limit = max(1, min(limit, 1000))
    try:
        data = _get("/list", {"user_id": USER_ID, "limit": limit})
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "list_memories fehlgeschlagen", exc_info=True)
        return f"Fehler beim Auflisten: {type(e).__name__}: {e}"
    items = data.get("items", [])
    if not items:
        return "Das zweite Gehirn ist noch leer."
    total = data.get("count", len(items))
    # Ladefenster-Hinweis (Frank 2026-06-26): direkt nach einem brain-api/Qdrant-Neustart kann die
    # Liste UNVOLLSTAENDIG sein, ohne dass ein Fehler auftritt. brain-api meldet das via 'ready'.
    # NUR bei explizitem False warnen (aeltere brain-api ohne Feld -> None -> keine Warnung; abwaertskompat).
    warn = ""
    if data.get("ready") is False:
        warn = ("⚠️ Das zweite Gehirn laedt gerade noch (Qdrant-Status nicht 'green') — diese Liste ist "
                "moeglicherweise UNVOLLSTAENDIG. Bitte in ein paar Sekunden erneut abrufen.\n\n")
        _log(logging.WARNING, "list_memories bei nicht-bereitem Gehirn", count=total)
    lines = []
    for i, it in enumerate(items, 1):
        title = it.get("title") or "(ohne Titel)"
        cat = f" [{it['category']}]" if it.get("category") else ""
        lines.append(f"{i}. {title}{cat} — {it.get('chars', '?')} Zeichen")
    head = f"{total} Eintrag(e)" + (f" (zeige {len(items)})" if total > len(items) else "") + ":"
    return warn + head + "\n" + "\n".join(lines)


@mcp.tool()
def forget(title: str) -> str:
    """Loesche den Eintrag mit diesem Titel KOMPLETT aus dem zweiten Gehirn."""
    try:
        data = _delete("/by-title", {"title": title, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.ERROR, "forget fehlgeschlagen", exc_info=True)
        return f"Fehler beim Loeschen: {type(e).__name__}: {e}"
    if data.get("deleted"):
        return f"Geloescht: '{title}'."
    return f"Kein Eintrag mit Titel '{title}' (nichts geloescht)."


@mcp.tool()
def brain_health() -> str:
    """Pruefe den Zustand des zweiten Gehirns (Verbindung, Anzahl Eintraege, Embedding-Modell)."""
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
