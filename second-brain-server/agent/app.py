"""
sb-agent — Bibliothekar-Agent (Schicht 3) des zweiten Gehirns, SPEICHER-SEITE (Phase 4a).

Aufgabe: Frank schickt Text -> der Agent ordnet ein (Kategorie + Titel), prueft Dubletten,
fragt bei Bedarf zurueck, speichert den Text WORTWOERTLICH 1:1 ueber die brain-api und antwortet
in normalem, menschlichem Deutsch (Smalltalk-Ton). Der Agent VERAENDERT NIE den gespeicherten
Inhalt — das LLM entscheidet nur Kategorie/Titel/Antwort, der Text geht 1:1 in den Speicher.

Gespraech: Kurzzeit-Gedaechtnis pro Sitzung (30 min Inaktivitaet). Danach Logbuch ZWEIFACH:
  (1) 1:1 ins Gehirn (Kategorie 'gespraeche')
  (2) als .txt-Sicherheitskopie auf der Samba-Platte Gedanken: /srv/samba/gedanken/Logbuch/JJJJ/MM/
      Dateiname "TT.MM.JJJJ - H.MM Uhr.txt", Inhalt: Kategorie-Zeile + Datum/Uhrzeit + Verlauf
      (klar getrennt Frank: / Agent:).

Bewusst NICHT hier: Abfrage-Seite (4b), STT/TTS (Frontends), Dashboard. Text rein, Text raus.
Plan: best-practices/second-brain/agent-bibliothekar-plan.md
Observability-First: JSON-Log (stdout + Datei), Fehler-Faenger, Logik-Sonden + Intent-Checkpoints.
"""
from __future__ import annotations

import asyncio
import json
import logging
import os
import time
import traceback
from contextlib import asynccontextmanager
from datetime import datetime
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any
from zoneinfo import ZoneInfo

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

VERSION = "0.1.3"  # 0.1.3: Zeitstempel JE Nachricht wieder RAUS (verwaessern die semantische Suche im Gehirn) - nur Kopf-Datum/Uhrzeit bleibt. Aktueller-Zeitpunkt-im-Prompt (korrekte Titel) bleibt. 0.1.2: Zeitpunkt+Zeitstempel. 0.1.1: /end+Kategorie. 0.1.0: Phase 4a.

# ---------------------------------------------------------------------------
# Konfiguration (alles aus Umgebungsvariablen — Secrets nie im Code)
# ---------------------------------------------------------------------------
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY", "")
SB_API_KEY = os.getenv("SB_API_KEY", "")                 # Bearer fuer brain-api UND fuer diesen Endpunkt
BRAIN_URL = os.getenv("BRAIN_URL", "http://brain-api:8000").rstrip("/")
AGENT_MODEL = os.getenv("AGENT_MODEL", "gemini-3.1-flash-lite")   # austauschbar (spaeter Dashboard-Wahl)
USER_ID = os.getenv("SB_USER_ID", "frank")
SESSION_TIMEOUT_S = int(os.getenv("AGENT_SESSION_TIMEOUT_MIN", "30")) * 60
LOGBOOK_DIR = os.getenv("AGENT_LOGBOOK_DIR", "/logbook")  # gemountet auf /srv/samba/gedanken/Logbuch
TZNAME = os.getenv("AGENT_TZ", "Europe/Berlin")
CONV_CATEGORY = os.getenv("AGENT_CONV_CATEGORY", "gespraeche")
DEDUP_CANDIDATES = int(os.getenv("AGENT_DEDUP_CANDIDATES", "3"))
DEDUP_MIN_SCORE = float(os.getenv("AGENT_DEDUP_MIN_SCORE", "0.70"))  # ab hier dem LLM als Kandidat zeigen
HISTORY_MAX = int(os.getenv("AGENT_HISTORY_MAX", "20"))             # so viele letzte Nachrichten an das LLM
LOG_PATH = os.getenv("AGENT_LOG_PATH", "/app/logs/agent.jsonl")
LOG_LEVEL = os.getenv("AGENT_LOG_LEVEL", "INFO").upper()

try:
    TZ = ZoneInfo(TZNAME)
except Exception:  # noqa: BLE001 — falls tzdata fehlt: UTC-Fallback (Logbuch dann in UTC)
    TZ = ZoneInfo("UTC")

# ---------------------------------------------------------------------------
# Strukturiertes JSON-Logging (stdout + rotierende Datei, beide UTF-8)
# ---------------------------------------------------------------------------
class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        entry = {
            "ts": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(record.created)),
            "level": record.levelname,
            "module": "sb-agent",
            "fn": record.funcName,
            "msg": record.getMessage(),
        }
        if isinstance(getattr(record, "ctx", None), dict):
            entry["ctx"] = record.ctx
        if record.exc_info:
            entry["trace"] = "".join(traceback.format_exception(*record.exc_info))
        return json.dumps(entry, ensure_ascii=False)


log = logging.getLogger("sb-agent")
log.setLevel(getattr(logging, LOG_LEVEL, logging.INFO))
_stdout = logging.StreamHandler()
_stdout.setFormatter(JsonFormatter())
log.addHandler(_stdout)
try:
    os.makedirs(os.path.dirname(LOG_PATH), exist_ok=True)
    _file = RotatingFileHandler(LOG_PATH, maxBytes=5_000_000, backupCount=5, encoding="utf-8")
    _file.setFormatter(JsonFormatter())
    log.addHandler(_file)
except OSError as e:
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
    log.log(logging.INFO if ok else logging.WARNING, f"CHECKPOINT {step}",
            extra={"ctx": {"kind": "CHECKPOINT", "step": step, "intent": intent, "ok": ok, **ctx}})


probe(bool(GEMINI_API_KEY), "GEMINI_API_KEY fehlt")
probe(bool(SB_API_KEY) and len(SB_API_KEY) >= 32, "SB_API_KEY fehlt/zu kurz")

# ---------------------------------------------------------------------------
# Gemini-Client (NUR Denken/Einordnen — veraendert NIE den gespeicherten 1:1-Inhalt)
# ---------------------------------------------------------------------------
gclient = None
genai_types = None
init_error: str | None = None
try:
    from google import genai
    from google.genai import types as _gt
    genai_types = _gt
    gclient = genai.Client(api_key=GEMINI_API_KEY)
except Exception as e:  # noqa: BLE001
    init_error = f"{type(e).__name__}: {e}"
    log.error("Gemini-Init fehlgeschlagen", exc_info=True)

_log(logging.INFO, "sb-agent startet", version=VERSION, model=AGENT_MODEL, brain_url=BRAIN_URL,
     log_path=LOG_PATH, logbook_dir=LOGBOOK_DIR, tz=str(TZ), session_timeout_s=SESSION_TIMEOUT_S)

HEADERS = {"Authorization": f"Bearer {SB_API_KEY}", "Content-Type": "application/json"}

# ---------------------------------------------------------------------------
# brain-api-Helfer (der Agent NUTZT den 1:1-Speicher, ersetzt ihn nicht)
# ---------------------------------------------------------------------------
def brain_store(text: str, title: str, category: str) -> dict:
    payload = {"text": text, "user_id": USER_ID}
    if title.strip():
        payload["title"] = title.strip()
    if category.strip():
        payload["category"] = category.strip()
    r = httpx.post(f"{BRAIN_URL}/store", json=payload, headers=HEADERS, timeout=120.0)
    r.raise_for_status()
    return r.json()


def brain_search(query: str, limit: int) -> list[dict]:
    r = httpx.post(f"{BRAIN_URL}/search", json={"query": query, "user_id": USER_ID, "limit": limit},
                   headers=HEADERS, timeout=60.0)
    r.raise_for_status()
    return r.json().get("items", [])


def brain_categories() -> list[str]:
    """Liste aller bestehenden Kategorien (ohne die Gespraechs-Logs — eigene Spur)."""
    try:
        r = httpx.get(f"{BRAIN_URL}/list", params={"user_id": USER_ID, "limit": 1000},
                      headers=HEADERS, timeout=30.0)
        r.raise_for_status()
        cats = {it.get("category") for it in r.json().get("items", []) if it.get("category")}
        cats.discard(CONV_CATEGORY)  # Gespraechs-Logs sind keine Fakten-Kategorie
        return sorted(cats)
    except Exception:  # noqa: BLE001 — Kategorien sind Hilfskontext, kein harter Fehler
        _log(logging.WARNING, "Kategorien-Abruf fehlgeschlagen", exc_info=True)
        return []


# ---------------------------------------------------------------------------
# Sitzungs-Speicher (in-memory, ein laufendes Gespraech je session_id; 30-min-Fenster)
# ---------------------------------------------------------------------------
_sessions: dict[str, dict] = {}
_lock = asyncio.Lock()


def _now_local() -> datetime:
    return datetime.now(TZ)


def _new_session(user_id: str) -> dict:
    return {"user_id": user_id, "messages": [], "start_local": _now_local(),
            "last_activity": time.monotonic(), "pending": None}


# ---------------------------------------------------------------------------
# System-Prompt (Rolle / Aufgabe / Kontext / Ausgabeformat / Ton)
# ---------------------------------------------------------------------------
def build_system_prompt(categories: list[str]) -> str:
    cat_line = ", ".join(categories) if categories else "(noch keine)"
    return (
        "Du bist Franks freundlicher Bibliothekar fuer sein persoenliches 'zweites Gehirn'. "
        "Du sprichst ganz normales, menschliches Deutsch wie im Smalltalk — locker, klar, nicht steif.\n\n"
        "DEINE AUFGABE (Speicher-Seite): Frank schickt dir Infos, die du in seinem Gehirn ablegst. "
        "WICHTIG: Der Text wird WORTWOERTLICH 1:1 gespeichert — du schreibst ihn NIE um, kuerzt ihn nicht, "
        "deutest ihn nicht. Du entscheidest nur eine passende KATEGORIE und einen kurzen TITEL und redest mit Frank.\n\n"
        "ZEIT: Wenn du ein Datum oder eine Uhrzeit brauchst (z.B. fuer einen Titel), nimm AUSSCHLIESSLICH den "
        "'AKTUELLEN ZEITPUNKT' aus der Nachricht unten (Zeitzone Europe/Berlin) — erfinde NIEMALS ein Datum/eine Uhrzeit.\n\n"
        f"BESTEHENDE KATEGORIEN (ASCII-klein): {cat_line}\n"
        "- Passt die Info in EINE der bestehenden Kategorien (auch nur grob passend) -> action='store', "
        "ordne sie DIREKT dort ein und frage NICHT nach. Biete KEINE neue Kategorie an, wenn eine bestehende passt. "
        "In 'reply' sagst du dann nur kurz+freundlich, wohin du es gelegt hast.\n"
        "- NUR wenn WIRKLICH KEINE bestehende Kategorie passt -> action='ask', schlage GENAU EINEN kurzen "
        "ASCII-Kleinbuchstaben-Schluessel als NEUE Kategorie vor und frage Frank in 'reply', ob die neu "
        "anzulegende Kategorie ok ist (er darf auch eine andere nennen).\n\n"
        "DUBLETTEN: Du bekommst evtl. aehnliche, schon vorhandene Eintraege gezeigt. Ist einer im Kern "
        "DIESELBE Info -> action='ask', sag in 'reply' kurz, dass es '<Titel>' schon aehnlich gibt, und frage: "
        "ersetzen / als neu speichern / abbrechen. Speichere dann noch nicht.\n\n"
        "ANTWORT AUF EINE RUECKFRAGE: Wenn unten ein 'OFFENER PUNKT' steht, antwortet Frank gerade darauf. "
        "Loese es auf: bestaetigt er Ersetzen -> action='store' und setze replace_title auf den genauen Titel des "
        "alten Eintrags; 'neu' -> action='store' (neuer Eintrag); nennt er eine andere Kategorie -> nimm die; "
        "'abbrechen' -> action='smalltalk' (nichts speichern), bestaetige freundlich.\n\n"
        "KEIN SPEICHER-FALL: Ist die Nachricht nur Smalltalk/Begruessung/Frage an dich (nichts zum Merken) -> "
        "action='smalltalk', antworte einfach natuerlich.\n\n"
        "ANTWORTE AUSSCHLIESSLICH MIT EINEM JSON-OBJEKT, genau diese Felder:\n"
        "{\n"
        '  "action": "store" | "ask" | "smalltalk",\n'
        '  "category": "kategorie_schluessel",        // bei store/ask\n'
        '  "title": "Kurzer Titel",                   // bei store/ask\n'
        '  "replace_title": "",                        // nur beim Ersetzen: exakter Titel des alten Eintrags\n'
        '  "reply": "Deine Antwort an Frank in normalem Deutsch"\n'
        "}\n"
        "Nur das JSON, kein weiterer Text."
    )


def _history_text(session: dict) -> str:
    msgs = session["messages"][-HISTORY_MAX:]
    if not msgs:
        return "(noch nichts)"
    return "\n".join(("Frank" if m["role"] == "frank" else "Agent") + ": " + m["text"] for m in msgs)


def llm_decide(session: dict, user_text: str, candidates: list[dict], categories: list[str]) -> dict:
    """Fragt Gemini um eine strukturierte Entscheidung (JSON). Veraendert NIE den 1:1-Inhalt."""
    if gclient is None:
        raise RuntimeError(f"Gemini nicht initialisiert: {init_error}")
    cand_txt = "(keine)"
    if candidates:
        cand_txt = "\n".join(
            f"- Titel: {c.get('title') or '(ohne Titel)'} | Kategorie: {c.get('category') or '-'} "
            f"| Aehnlichkeit: {c.get('score', 0):.2f} | Auszug: {(c.get('match') or c.get('text') or '')[:200]}"
            for c in candidates
        )
    pending = session.get("pending")
    pending_txt = "(keiner)"
    if pending:
        pending_txt = (f"Frank wurde gefragt zu dieser Info: \"{pending['fact_text']}\" "
                       f"(Vorschlag Kategorie '{pending.get('category','')}', Titel '{pending.get('title','')}', "
                       f"moegliche Dublette: '{pending.get('dup_title','')}'). Seine aktuelle Nachricht ist die Antwort darauf.")

    now = _now_local()
    _wd = ["Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"][now.weekday()]
    now_line = (f"AKTUELLER ZEITPUNKT: {_wd}, {now.strftime('%d.%m.%Y')}, {now.strftime('%H:%M')} Uhr "
                "(Zeitzone Europe/Berlin).")
    user_block = (
        f"{now_line}\n\n"
        f"BISHERIGES GESPRAECH:\n{_history_text(session)}\n\n"
        f"OFFENER PUNKT (Rueckfrage):\n{pending_txt}\n\n"
        f"AEHNLICHE VORHANDENE EINTRAEGE:\n{cand_txt}\n\n"
        f"AKTUELLE NACHRICHT VON FRANK:\n{user_text}"
    )
    resp = gclient.models.generate_content(
        model=AGENT_MODEL,
        contents=user_block,
        config=genai_types.GenerateContentConfig(
            system_instruction=build_system_prompt(categories),
            response_mime_type="application/json",
            temperature=0.3,
        ),
    )
    raw = (resp.text or "").strip()
    try:
        data = json.loads(raw)
        if not isinstance(data, dict):
            raise ValueError("kein Objekt")
    except Exception:  # noqa: BLE001 — defensiv: nie crashen, sauber zurueckfallen
        _log(logging.WARNING, "LLM-JSON nicht parsebar", raw=raw[:300])
        return {"action": "smalltalk", "reply": "Sorry, das habe ich nicht ganz verstanden — sag es nochmal?"}
    data.setdefault("action", "smalltalk")
    data.setdefault("category", "")
    data.setdefault("title", "")
    data.setdefault("replace_title", "")
    data.setdefault("reply", "")
    return data


# ---------------------------------------------------------------------------
# Logbuch: Gespraech ZWEIFACH sichern (Gehirn + .txt auf der Samba-Platte)
# ---------------------------------------------------------------------------
def _unique_path(folder: Path, base: str) -> Path:
    p = folder / f"{base}.txt"
    i = 2
    while p.exists():
        p = folder / f"{base} ({i}).txt"
        i += 1
    return p


def flush_session_to_logbook(session: dict) -> None:
    """Schreibt den Gespraechsverlauf 1:1 ins Gehirn (Kategorie gespraeche) UND als .txt-Kopie."""
    if not session["messages"]:
        return
    start = session["start_local"]
    date_str = start.strftime("%d.%m.%Y")
    time_str = f"{start.hour}.{start.minute:02d} Uhr"
    header = f"Kategorie: Gespraeche\nDatum/Uhrzeit: {date_str} - {time_str}\n\n"
    body = "\n".join(("Frank" if m["role"] == "frank" else "Agent") + ": " + m["text"]
                     for m in session["messages"])
    content = header + body + "\n"

    txt_ok = False
    try:
        folder = Path(LOGBOOK_DIR) / start.strftime("%Y") / start.strftime("%m")
        folder.mkdir(parents=True, exist_ok=True)
        path = _unique_path(folder, f"{date_str} - {time_str}")
        path.write_text(content, encoding="utf-8")
        txt_ok = True
        _log(logging.INFO, "Logbuch-.txt geschrieben", path=str(path), chars=len(content))
    except Exception:  # noqa: BLE001 — .txt-Kopie darf das Gehirn-Logbuch nicht verhindern
        _log(logging.ERROR, "Logbuch-.txt fehlgeschlagen", exc_info=True)

    brain_ok = False
    try:
        brain_store(text=content, title=f"Gespraech {date_str} - {time_str}", category=CONV_CATEGORY)
        brain_ok = True
    except Exception:  # noqa: BLE001
        _log(logging.ERROR, "Logbuch ins Gehirn fehlgeschlagen", exc_info=True)

    checkpoint("logbuch", "Gespraech ZWEIFACH gesichert (Gehirn + .txt-Kopie)",
               ok=(txt_ok or brain_ok), txt=txt_ok, brain=brain_ok,
               nachrichten=len(session["messages"]), start=f"{date_str} - {time_str}")


async def _flush_loop() -> None:
    """Hintergrund: alle 60s pruefen, welche Sitzungen >30 min inaktiv sind -> Logbuch + schliessen."""
    while True:
        try:
            await asyncio.sleep(60)
            now = time.monotonic()
            async with _lock:
                stale = [sid for sid, s in _sessions.items() if now - s["last_activity"] > SESSION_TIMEOUT_S]
                for sid in stale:
                    s = _sessions.pop(sid)
                    try:
                        flush_session_to_logbook(s)
                        _log(logging.INFO, "Sitzung nach Inaktivitaet geschlossen", session=sid)
                    except Exception:  # noqa: BLE001
                        _log(logging.ERROR, "Flush fehlgeschlagen", exc_info=True, session=sid)
        except asyncio.CancelledError:
            break
        except Exception:  # noqa: BLE001 — der Loop darf nie sterben
            _log(logging.ERROR, "Flush-Loop-Fehler", exc_info=True)


# ---------------------------------------------------------------------------
# Auth + App
# ---------------------------------------------------------------------------
def require_auth(authorization: str = Header(default="")) -> None:
    if not SB_API_KEY or authorization != f"Bearer {SB_API_KEY}":
        raise HTTPException(status_code=401, detail="Unauthorized")


@asynccontextmanager
async def lifespan(app: FastAPI):
    task = asyncio.create_task(_flush_loop())
    _log(logging.INFO, "Flush-Loop gestartet")
    try:
        yield
    finally:
        task.cancel()


app = FastAPI(title="Second Brain — sb-agent (Bibliothekar, Speicher-Seite)", version=VERSION, lifespan=lifespan)


@app.exception_handler(Exception)
async def unhandled(request: Request, exc: Exception) -> JSONResponse:
    log.error("Unbehandelte Ausnahme", exc_info=True, extra={"ctx": {"path": str(request.url.path)}})
    return JSONResponse(status_code=500, content={"error": type(exc).__name__, "detail": str(exc)})


class ChatReq(BaseModel):
    text: str = Field(..., min_length=1, description="Franks Textnachricht")
    session_id: str | None = Field(default=None, description="Gespraechs-ID (sonst pro Nutzer eine laufende Sitzung)")
    user_id: str = Field(default="frank")


class EndReq(BaseModel):
    session_id: str | None = Field(default=None, description="Gespraechs-ID (sonst pro Nutzer)")
    user_id: str = Field(default="frank")


@app.get("/health")
def health() -> dict:
    brain = "unreachable"
    try:
        r = httpx.get(f"{BRAIN_URL}/health", timeout=8.0)
        brain = r.json().get("status", "?") if r.status_code == 200 else f"http {r.status_code}"
    except Exception as e:  # noqa: BLE001
        brain = f"{type(e).__name__}"
    return {"status": "ok" if (gclient is not None and init_error is None) else "degraded",
            "version": VERSION, "model": AGENT_MODEL, "init_error": init_error,
            "brain": brain, "aktive_sitzungen": len(_sessions), "session_timeout_s": SESSION_TIMEOUT_S}


@app.post("/chat", dependencies=[Depends(require_auth)])
async def chat(req: ChatReq) -> dict:
    """Speicher-Seite: Text rein -> einordnen/Dublette/Rueckfrage -> 1:1 speichern -> Text raus."""
    if gclient is None:
        raise HTTPException(status_code=503, detail=f"Agent nicht bereit: {init_error}")
    sid = (req.session_id or req.user_id).strip()
    t0 = time.time()

    async with _lock:
        session = _sessions.get(sid)
        if session is None:
            session = _new_session(req.user_id)
            _sessions[sid] = session
        session["messages"].append({"role": "frank", "text": req.text})
        resolving = session.get("pending") is not None
        pending = session.get("pending")

    # Dubletten-Kandidaten nur bei einem FRISCHEN Fakt holen (nicht bei einer Rueckfrage-Antwort)
    fact_for_search = (pending["fact_text"] if resolving and pending else req.text)
    candidates: list[dict] = []
    try:
        hits = brain_search(fact_for_search, DEDUP_CANDIDATES)
        candidates = [h for h in hits if h.get("category") != CONV_CATEGORY and h.get("score", 0) >= DEDUP_MIN_SCORE]
    except Exception:  # noqa: BLE001 — Dedup ist Hilfe, kein harter Fehler
        _log(logging.WARNING, "Dubletten-Suche fehlgeschlagen", exc_info=True)

    categories = brain_categories()
    decision = llm_decide(session, req.text, candidates, categories)

    action = decision.get("action", "smalltalk")
    cat = (decision.get("category") or "").strip().lower()
    title = (decision.get("title") or "").strip()
    replace_title = (decision.get("replace_title") or "").strip()
    reply = (decision.get("reply") or "").strip()
    is_new_cat = bool(cat) and cat not in categories

    # Sicherheits-Gitter: eine NEUE Kategorie wird NIE still angelegt — nur nach Rueckfrage/Bestaetigung.
    if action == "store" and is_new_cat and not resolving:
        action = "ask"
        if not reply:
            reply = f"Dafuer gibt es noch keine Kategorie — ich wuerde '{cat}' anlegen. Passt das, oder lieber eine andere?"

    stored = None
    if action == "store":
        fact_text = pending["fact_text"] if (resolving and pending) else req.text
        use_title = replace_title or title
        try:
            stored = brain_store(text=fact_text, title=use_title, category=cat)
        except Exception as e:  # noqa: BLE001
            _log(logging.ERROR, "Speichern fehlgeschlagen", exc_info=True)
            reply = f"Das Speichern hat gerade nicht geklappt ({type(e).__name__}). Versuch es bitte gleich nochmal."
            action = "error"
        else:
            if not reply:
                reply = (f"Gespeichert{' (alte Version ersetzt)' if stored.get('replaced') else ''}: "
                         f"Kategorie '{cat}', Titel '{use_title}'.")
            async with _lock:
                session["pending"] = None

    elif action == "ask":
        async with _lock:
            session["pending"] = {"fact_text": req.text, "category": cat, "title": title,
                                  "dup_title": (candidates[0]["title"] if candidates else "")}
    else:  # smalltalk / error
        if action != "error":
            async with _lock:
                session["pending"] = None

    async with _lock:
        session["messages"].append({"role": "agent", "text": reply})
        session["last_activity"] = time.monotonic()

    checkpoint("chat", "Text 1:1 einordnen+speichern bzw. natuerlich antworten",
               ok=(action in ("store", "ask", "smalltalk")), action=action, category=cat or None,
               new_category=is_new_cat, replaced=bool(stored and stored.get("replaced")),
               resolving=resolving, candidates=len(candidates), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "reply": reply, "action": action, "session_id": sid,
            "category": cat or None, "title": (replace_title or title) or None,
            "stored": bool(stored), "replaced": bool(stored and stored.get("replaced"))}


@app.post("/end", dependencies=[Depends(require_auth)])
async def end_session(req: EndReq) -> dict:
    """Gespraech bewusst beenden + sofort ins Logbuch sichern (statt auf den 30-min-Timeout zu warten)."""
    sid = (req.session_id or req.user_id).strip()
    async with _lock:
        session = _sessions.pop(sid, None)
    if session is None:
        return {"ok": True, "gesichert": False, "grund": "keine aktive Sitzung"}
    flush_session_to_logbook(session)
    return {"ok": True, "gesichert": True, "nachrichten": len(session["messages"])}


@app.get("/")
def root() -> dict:
    return {"service": "Second Brain — sb-agent (Bibliothekar, Speicher-Seite)", "version": VERSION,
            "endpoints": ["/health", "/chat", "/end"]}
