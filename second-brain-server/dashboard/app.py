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

VERSION = "0.16.0"  # 0.16.0: Backup-Inhalts-Liste + Anzeige-Fixes (Frank-Wunsch 2026-06-25). (a) Neuer Endpoint GET /api/backup/contents listet GENERISCH alle Top-Level-Ordner des Z->Drive-Backups (Qdrant-Snapshots, Logbuch, Eval-Logs + kuenftige) mit Anzahl/Datum-des-letzten/Groesse -> scrollbare Box unter der Backup-Kachel im Einstellungen-Tab (immer aktueller Stand). compose: read-only Mount /srv/samba/gedanken:/gedanken:ro + DASH_BACKUP_ROOT. (b) Eval-Beschreibung 50->90 Saetze. (c) Kategorie-Tags an Eintraegen zeigen echte Schreibweise (CSS text-transform:lowercase entfernt; cap() macht nur ersten Buchstaben gross, erhaelt Bindestriche). 0.15.0: Eintrag-Bearbeitung im Drawer (Frank-Wunsch 2026-06-25). (a) PUT /api/entry reicht jetzt auch 'title' durch -> Titel im Drawer bearbeitbar (brain migriert die doc_id bei Titel-Aenderung). (b) POST /api/entry/category (Proxy an agent /categories/move-entry) verschiebt EINEN Eintrag in eine andere Kategorie fuers Drawer-Kategorie-Dropdown (neue Kategorie landet kanonisch in der Registry -> synchron mit Einstellungen+Gespraech). Frontend: Drawer-Titel im Bearbeiten-Modus editierbar; Kategorie-Dropdown (Gespraech-Stil, nach unten) neben Loeschen + 'In Kategorie speichern'-Button. 0.14.0: Anklickbare Antwort-Knoepfe im Chat (Frank-Wunsch 2026-06-25) — bei einer Speicher- ODER Kategorie-Rueckfrage (save_confirm/store_clarify) zeigt der Chat jetzt Ja/Nein-Knoepfe (chatOptions), Klick schickt die Antwort; Frank muss nicht tippen. 0.13.0: Kategorie-Verwaltung (Frank-Wunsch 2026-06-25) — Proxys /api/categories/detail (Liste mit Eintragszahl+leer-Flag), /api/categories/rename (umbenennen/mergen), /api/categories/delete (Etikett entfernen, Eintraege bleiben) an den Agenten; Frontend: Einstellungen-Abschnitt 'Kategorien' (Dropdown, Umbenennen+Speichern, Loeschen mit Warnung, Merge, Dublettenwarnung, leere anlegen); Gespraech-Dropdown synchron + deutsche Grossschreibung + leere ausgegraut. 0.12.0: Logbuch<->Gehirn-Sync (Frank-Wunsch) — wird ein 'gespraeche'-Eintrag im Gehirn geloescht, loescht das dashboard via agent auch die zugehoerige .txt auf Platte Z; beim Wiederherstellen aus dem Papierkorb wird die .txt zurueckgeschrieben. Eigene Dropdowns im Seiten-Stil; Dropdown-Scrollbalken ausgeblendet; Papierkorb-Bearbeiten mit Abbrechen-Button; Monats-Toggle. 0.11.0: Papierkorb-Bereich + Logbuch nach Monaten (Frank-Wunsch) — /api/trash (GET Liste, PUT editieren, POST /api/trash/restore wiederherstellen) als Proxy an brain; Logbuch /api/logbook/tree (Jahr/Monat-Baum) + /api/logbook?year=&month= (Lazy-Load eines Monats). Papierkorb + Logbuch teilen die Jahr/Monat-Navigation (aktueller Monat umrandet). 0.10.0: Papierkorb-Button im Eintrags-Drawer (Frank-Wunsch) — DELETE /api/entry (Proxy an brain DELETE /entry per doc_id) loescht einen Eintrag dauerhaft aus dem Gedaechtnis, mit eigenem Ja/Nein-Bestaetigungsdialog im Frontend. 0.9.0: Kategorie-Dropdown beim Senden (Frank-Wunsch) — Dropdown neben dem X-Button (Gespraech-Tab) mit allen Kategorien + 'Kategorie +' zum Anlegen; /api/chat reicht die gewaehlte 'category' an den Agenten weiter (Override). 0.8.0: Kategorie-Registry (Frank-Wunsch) — /api/categories GET/POST (Proxy an Agent); Uebersicht zeigt manuell angelegte (noch leere) Kategorien mit count 0. 0.7.0: Drei umschaltbare System-Prompts (Frank-Wunsch) — /api/prompt reicht role (haupt/speicher/abfrage) an den Agenten weiter; UI bekommt drei Umschalt-Buttons ueber dem Prompt-Textfeld. 0.6.2: Modell-pro-Rolle — drei Dropdowns (Hauptagent/Speicheragent/Abfrageagent), /api/config reicht haupt_model/speicher_model/abfrage_model weiter; System-Prompt/Logbuch-Kacheln wieder volle Breite + sauberer Abstand unter der oberen Reihe. 0.6.1: Backup-Kachel — "Mit Google verbinden"-Button + Token-Dialog (/api/backup/connect schreibt Token ins Steuer-Verzeichnis, Host stellt rclone-Verbindung her); Kacheln Bibliothekar-Agent + Backup wieder in voller Originalgroesse nebeneinander (set-row breiter); Steuer-/Status-/Trigger-Dateien jetzt im dashboard-schreibbaren /control statt auf Z (appuser-Permissions). 0.6.0: Google-Drive-Backup-Kachel (Einstellungen, neben Bibliothekar-Agent) — Status + letzter Sync-Zeitstempel, Buttons "Jetzt sichern"/"Wiederherstellen"; liest Status-Datei aus der Z-Wurzel (/gedanken) und schreibt Trigger-Flags, das eigentliche crash-sichere rclone-Backup laeuft auf dem Host (systemd). 0.5.1: Mikrofon-Hybrid-Diktat — Live-Vorschau via Web Speech API (interim) WAEHREND des Sprechens, finale Groq-Whisper-Fassung (mit Satzzeichen) ERSETZT beim Stopp die Vorschau (previewActive-Riegel verhindert, dass spaete Web-Speech-Events Groqs Endfassung ueberschreiben; Fallback auf Vorschau nur bei Groq-Ausfall, mit sichtbarem Hinweis). 0.5.0: Übersicht-Feinschliff (GEDÄCHTNIS-SPEKTRUM rechtsbündig, grosse Eintragszahl wird nicht mehr abgeschnitten + Tausenderpunkte), Browser-Navigation Zurück/Vor (History API), Kategorie gespraeche wieder als Balken/Legende/Chip sichtbar (anklickbar+bearbeitbar) — zaehlt aber NICHT in die Gesamtsumme, sichtbare Dashboard-Version im Rail-Fuss. 0.4.2: Roter X-Loeschen-Button links neben dem Mikrofon im Gespraech-Tab (leert die Eingabezeile komplett, setzt Hoehe zurueck). 0.4.1: Logbuch-Gespraeche (Kategorie gespraeche) zaehlen NICHT mehr in der Uebersicht (bleiben aber als Vektoren im Gehirn, durchsuchbar/recall). 0.4.0: Eintrags-Editor (PUT /api/entry -> brain), Mikrofon-STT (POST /api/transcribe -> Groq whisper-large-v3-turbo), Prompt-Verbesserung (POST /api/improve -> agent), Logbuch liest die .txt-Protokolle von der Samba-Platte (Z) mit Gehirn-Fallback. 0.3.0: Chat-Tab — /api/chat proxied an den Agenten (store/recall) via asyncio.to_thread (kein Event-Loop-Block, bugs/server/fastapi.md §1). 0.2.1: Einstellungen-Tab (Prompt-Editor + Modell-Wahl)

BRAIN_URL = os.getenv("BRAIN_URL", "http://brain-api:8000").rstrip("/")
AGENT_URL = os.getenv("AGENT_URL", "http://agent:8002").rstrip("/")
SB_API_KEY = os.getenv("SB_API_KEY", "")
USER_ID = os.getenv("SB_USER_ID", "frank")
HOSTFS = os.getenv("DASH_HOSTFS", "/hostfs")
CONV_CATEGORY = os.getenv("DASH_CONV_CATEGORY", "gespräche")  # deutsche Umlaute (Frank 2026-06-24); Altbestand migriert
# Logbuch-.txt-Protokolle (Samba-Platte = Franks Z:), read-only ins dashboard gemountet.
LOGBOOK_DIR = os.getenv("DASH_LOGBOOK_DIR", os.getenv("AGENT_LOGBOOK_DIR", "/logbook"))
# Z-Wurzel (Samba "gedanken") fuer das Google-Drive-Backup: Status-Datei lesen + Trigger-Flags schreiben.
BACKUP_DIR = os.getenv("DASH_BACKUP_DIR", "/gedanken")
# Komplettes Z-Backup (read-only) fuer die Inhalts-Liste "was liegt aktuell im Drive-Backup".
BACKUP_ROOT = os.getenv("DASH_BACKUP_ROOT", "/gedanken")
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


def _bdelete(path: str, **params):
    r = httpx.delete(f"{BRAIN_URL}{path}", params=params or None, headers=HEADERS, timeout=30.0)
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


def _adelete(path: str, **params):
    r = httpx.delete(f"{AGENT_URL}{path}", params=params or None, headers=HEADERS, timeout=20.0)
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
        # Logbuch-Gespraeche (Kategorie CONV_CATEGORY) ERSCHEINEN als eigene Kategorie (Balken + Legende
        # + Chip, anklickbar und bearbeitbar), zaehlen aber NICHT in die Gesamtsumme "Einträge gesamt"
        # (Frank-Korrektur 2026-06-24: alle Kategorien sichtbar, nur die Summe ohne Gespraeche).
        all_items = lst.get("items", [])
        c = Counter((it.get("category") or "(ohne)") for it in all_items)
        # Manuell angelegte (noch leere) Kategorien aus der Agent-Registry mit count 0 ergaenzen,
        # damit sie in der Uebersicht erscheinen, auch wenn noch kein Eintrag drin ist (Frank-Wunsch).
        try:
            for name in (_aget("/categories").get("categories", []) or []):
                if name and name not in c and name != CONV_CATEGORY:
                    c[name] = 0
        except Exception:  # noqa: BLE001 — Registry ist Hilfskontext; Agent-Ausfall darf die Uebersicht nicht killen
            pass
        out["categories"] = sorted(({"name": k, "count": v} for k, v in c.items()), key=lambda x: -x["count"])
        out["total"] = sum(1 for it in all_items if (it.get("category") or "") != CONV_CATEGORY)
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
    out["dash_version"] = VERSION   # sichtbare Dashboard-Version im Cockpit (Update-Kontrolle)
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


def _logbook_files() -> list:
    """Alle Logbuch-.txt-Dateien (rekursiv), ohne sie zu lesen."""
    base = Path(LOGBOOK_DIR)
    if not base.is_dir():
        return []
    try:
        return [p for p in base.rglob("*.txt") if p.is_file()]
    except Exception:  # noqa: BLE001
        return []


def _ym_of(p) -> tuple[str, str]:
    """Jahr/Monat eines Logbuch-Files — bevorzugt aus dem Ordnerpfad JJJJ/MM, sonst aus der mtime.
    Kein datetime-Import noetig (time.localtime)."""
    parent, gp = p.parent.name, p.parent.parent.name
    if gp.isdigit() and len(gp) == 4 and parent.isdigit() and len(parent) in (1, 2):
        return gp, parent.zfill(2)
    tm = time.localtime(p.stat().st_mtime)
    return f"{tm.tm_year:04d}", f"{tm.tm_mon:02d}"


@app.get("/api/logbook/tree")
def logbook_tree() -> dict:
    """Verfuegbare Jahre/Monate des Logbuchs (nur Zaehlung, KEINE Texte gelesen) — fuer die
    Jahr/Monat-Navigation, damit nicht alle Eintraege auf einmal geladen werden. Sync def -> Threadpool."""
    tree: dict[str, dict[str, int]] = {}
    for p in _logbook_files():
        try:
            y, m = _ym_of(p)
        except Exception:  # noqa: BLE001
            continue
        tree.setdefault(y, {}).setdefault(m, 0)
        tree[y][m] += 1
    return {"ok": True, "tree": tree}


@app.get("/api/logbook")
def logbook(year: str = "", month: str = "") -> dict:
    """Logbuch = die .txt-Gespraechsprotokolle auf der Samba-Platte (Franks Z:), gemountet unter
    LOGBOOK_DIR (JJJJ/MM/*.txt). Mit year+month: NUR die Eintraege dieses Monats (Lazy-Load, damit die
    Seite nicht endlos lang wird). Ohne Filter: die neuesten 80 (+ Gehirn-Fallback). Neueste zuerst.
    Sync def (Datei-I/O) -> Threadpool (fastapi §1)."""
    items: list[dict] = []
    filtered = bool(year and month)
    mm = month.zfill(2) if month else ""
    try:
        files = _logbook_files()
        if filtered:
            files = [p for p in files if _ym_of(p) == (year, mm)]
        files = sorted(files, key=lambda p: p.stat().st_mtime, reverse=True)
        if not filtered:
            files = files[:80]
        for p in files:
            try:
                txt = p.read_text(encoding="utf-8")
            except Exception:  # noqa: BLE001 — eine kaputte Datei darf das Logbuch nicht killen
                txt = ""
            items.append({"title": p.stem, "when": _logbook_when(p.stem, txt), "text": txt})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Logbuch-Ordner nicht lesbar", err=str(e), dir=LOGBOOK_DIR)
    if not items and not filtered:  # Fallback NUR ohne Monatsfilter: aus dem Gehirn (Kategorie gespraeche)
        try:
            d = _bget("/by-category", category=CONV_CATEGORY, user_id=USER_ID)
            for it in d.get("items", []):
                items.append({"title": it.get("title") or "Gespraech",
                              "when": it.get("updated_at") or "", "text": it.get("text") or ""})
        except Exception as e:  # noqa: BLE001
            _log(logging.WARNING, "Logbuch-Gehirn-Fallback fehlgeschlagen", err=str(e))
    _log(logging.INFO, "Logbuch geladen", count=len(items), year=year or None, month=mm or None)
    return {"items": items}


# --- Einstellungen: Proxy an den Agenten (System-Prompt + Modell-Wahl) ------
@app.get("/api/prompt")
def api_get_prompt(role: str = "haupt") -> dict:
    r = role if role in ("haupt", "speicher", "abfrage") else "haupt"
    return _aget(f"/prompt?role={r}")


@app.put("/api/prompt")
async def api_put_prompt(request: Request) -> dict:
    body = await request.json()
    r = (body.get("role") or "haupt")
    r = r if r in ("haupt", "speicher", "abfrage") else "haupt"
    # sync httpx via to_thread -> blockiert den Event-Loop nicht (bugs/server/fastapi.md §1)
    return await asyncio.to_thread(_aput, "/prompt", {"instructions": body.get("instructions", ""), "role": r})


@app.get("/api/config")
def api_get_config() -> dict:
    return _aget("/config")


@app.put("/api/config")
async def api_put_config(request: Request) -> dict:
    body = await request.json()
    # Modell-pro-Rolle weiterreichen (haupt/speicher/abfrage); 'model' bleibt als Abwaertskompat dabei.
    payload = {k: body.get(k) for k in ("haupt_model", "speicher_model", "abfrage_model", "model") if body.get(k)}
    return await asyncio.to_thread(_aput, "/config", payload)


# --- Kategorien: volle Liste (mit + ohne Eintrag) abrufen / neue anlegen (Proxy an den Agenten) ----
@app.get("/api/categories")
def api_get_categories() -> dict:
    """Volle Kategorienliste fuers Dropdown (inkl. leerer, vorab angelegter). Sync def -> Threadpool."""
    try:
        return _aget("/categories")
    except Exception as e:  # noqa: BLE001 — Agent offline: leere Liste statt 500
        _log(logging.WARNING, "Kategorien-Abruf fehlgeschlagen", err=str(e))
        return {"categories": []}


@app.post("/api/categories")
async def api_post_category(request: Request) -> dict:
    body = await request.json()
    name = (body.get("name") or "").strip()
    if not name:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Kategoriename"})
    try:
        return await asyncio.to_thread(_apost, "/categories", {"name": name})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorie anlegen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Anlegen fehlgeschlagen: {type(e).__name__}"})


@app.get("/api/categories/detail")
def api_get_categories_detail() -> dict:
    """Kategorien mit Eintragszahl + leer-Flag (fuer Verwaltung + Gespraech-Dropdown). Sync def -> Threadpool."""
    try:
        return _aget("/categories/detail")
    except Exception as e:  # noqa: BLE001 — Agent offline: leere Liste statt 500
        _log(logging.WARNING, "Kategorie-Detail-Abruf fehlgeschlagen", err=str(e))
        return {"ok": False, "categories": []}


@app.post("/api/categories/rename")
async def api_rename_category(request: Request) -> dict:
    """Kategorie umbenennen (Proxy an Agent -> brain set_payload, Vektor bleibt). Existiert das Ziel -> Merge."""
    body = await request.json()
    old = (body.get("old") or "").strip()
    new = (body.get("new") or "").strip()
    if not old or not new:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "old/new fehlen"})
    try:
        return await asyncio.to_thread(_apost, "/categories/rename", {"old": old, "new": new})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorie umbenennen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Umbenennen fehlgeschlagen: {type(e).__name__}"})


@app.post("/api/categories/delete")
async def api_delete_category(request: Request) -> dict:
    """Kategorie loeschen: Etikett von allen Eintraegen entfernen (Eintraege BLEIBEN). Proxy an Agent."""
    body = await request.json()
    name = (body.get("name") or "").strip()
    if not name:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Name"})
    try:
        return await asyncio.to_thread(_apost, "/categories/delete", {"name": name})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorie loeschen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Loeschen fehlgeschlagen: {type(e).__name__}"})


# --- Eval-Check: Selbsttest aller 3 Agenten (Proxy an den Agenten) -----------
@app.post("/api/eval/run")
async def api_eval_run() -> dict:
    """Eval-Check ausloesen. LANGER Timeout — 50 Test-Saetze x mehrere LLM-Calls dauern Minuten.
    Sync httpx via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    def _run():
        r = httpx.post(f"{AGENT_URL}/eval-run", headers=HEADERS, timeout=600.0)
        r.raise_for_status()
        return r.json()
    try:
        return await asyncio.to_thread(_run)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eval-Lauf fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Eval-Lauf fehlgeschlagen: {type(e).__name__}"})


@app.get("/api/eval/logs")
def api_eval_logs() -> dict:
    """Liste der Eval-Log-Dateien (neueste zuerst). Sync def -> Threadpool."""
    try:
        return _aget("/eval-logs")
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eval-Logs-Liste fehlgeschlagen", err=str(e))
        return {"ok": False, "logs": []}


@app.get("/api/eval/log")
def api_eval_log(name: str) -> dict:
    """Eine Eval-Log-Datei (Markdown) lesen. Sync def -> Threadpool."""
    try:
        r = httpx.get(f"{AGENT_URL}/eval-log", params={"name": name}, headers=HEADERS, timeout=20.0)
        r.raise_for_status()
        return r.json()
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eval-Log lesen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Log lesen fehlgeschlagen: {type(e).__name__}"})


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
    cat = (body.get("category") or "").strip()
    if cat:                       # im Dropdown gewaehlte Kategorie (Override) an den Agenten durchreichen
        payload["category"] = cat[:60]
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
    payload = {"doc_id": doc_id, "text": text, "user_id": USER_ID}
    # Optionaler neuer Titel (Frank-Wunsch: Titel im Drawer bearbeiten). Nur durchreichen, wenn das
    # Frontend ihn mitschickt -> brain migriert dann die doc_id; ohne title bleibt der alte erhalten.
    if "title" in body and body.get("title") is not None:
        payload["title"] = str(body.get("title")).strip()
    try:
        return await asyncio.to_thread(_bput, "/entry", payload)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eintrag-Ersatz fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Speichern fehlgeschlagen: {type(e).__name__}"})


# --- Eintrag dauerhaft loeschen (Papierkorb-Button im Drawer): Proxy an brain DELETE /entry ---------
@app.delete("/api/entry")
async def api_delete_entry(doc_id: str = "") -> dict:
    """Loescht einen Gehirn-Eintrag dauerhaft (per doc_id). Frank-Wunsch: Papierkorb neben Bearbeiten.
    Sync httpx-Call via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    doc_id = (doc_id or "").strip()
    if not doc_id:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id erforderlich"})
    try:
        res = await asyncio.to_thread(_bdelete, "/entry", doc_id=doc_id, user_id=USER_ID)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eintrag-Loeschen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Loeschen fehlgeschlagen: {type(e).__name__}"})
    # Gespraech geloescht -> zugehoerige Logbuch-.txt auf Platte Z mitloeschen (Logbuch == Gehirn synchron)
    if isinstance(res, dict) and res.get("category") == CONV_CATEGORY and res.get("title"):
        try:
            await asyncio.to_thread(_adelete, "/logbook", title=res["title"])
        except Exception as e:  # noqa: BLE001 — .txt-Sync best-effort; die Gehirn-Loeschung steht schon
            _log(logging.WARNING, "Logbuch-.txt-Sync (loeschen) fehlgeschlagen", err=str(e), title=res.get("title"))
    return res


# --- Eintrag in andere Kategorie verschieben (Kategorie-Dropdown im Drawer): Proxy an agent ---------
@app.post("/api/entry/category")
async def api_move_entry_category(request: Request) -> dict:
    """Verschiebt einen Eintrag (doc_id) in eine andere Kategorie. Geht ueber den AGENTEN, damit eine
    NEUE Ziel-Kategorie kanonisch in der Registry landet (sofort synchron mit Einstellungen->Kategorien
    + Gespraech-Dropdown). Sync httpx-Call via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    category = (body.get("category") or "").strip()
    if not doc_id or not category:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id und category erforderlich"})
    try:
        return await asyncio.to_thread(_apost, "/categories/move-entry", {"doc_id": doc_id, "category": category[:60]})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eintrag-Verschieben fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Verschieben fehlgeschlagen: {type(e).__name__}"})


# --- Papierkorb (Soft-Delete): Liste / im Papierkorb editieren / wiederherstellen (Proxy an brain) ---
@app.get("/api/trash")
def api_trash() -> dict:
    """Papierkorb-Liste (neueste Loeschung zuerst). Sync def -> Threadpool (fastapi §1)."""
    try:
        return _bget("/trash", user_id=USER_ID)
    except Exception as e:  # noqa: BLE001 — brain offline: leerer Papierkorb statt 500
        _log(logging.WARNING, "Papierkorb-Abruf fehlgeschlagen", err=str(e))
        return {"ok": False, "items": [], "count": 0}


@app.put("/api/trash")
async def api_trash_edit(request: Request) -> dict:
    """Aendert den Text eines Eintrags IM Papierkorb (kein Re-Embed). Proxy an brain PUT /trash."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    text = (body.get("text") or "").strip()
    if not doc_id or not text:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id und text erforderlich"})
    try:
        return await asyncio.to_thread(_bput, "/trash", {"doc_id": doc_id, "text": text, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Papierkorb-Edit fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Speichern fehlgeschlagen: {type(e).__name__}"})


@app.post("/api/trash/restore")
async def api_trash_restore(request: Request) -> dict:
    """Stellt einen Papierkorb-Eintrag wieder her (re-embed + zurueck ins Gehirn). Proxy an brain
    POST /trash/restore. Sync httpx via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    if not doc_id:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id erforderlich"})
    try:
        res = await asyncio.to_thread(_bpost, "/trash/restore", {"doc_id": doc_id, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Wiederherstellen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Wiederherstellen fehlgeschlagen: {type(e).__name__}"})
    # Gespraech wiederhergestellt -> Logbuch-.txt auf Platte Z zurueckschreiben (Logbuch == Gehirn synchron)
    if isinstance(res, dict) and res.get("category") == CONV_CATEGORY and res.get("title") and res.get("text"):
        try:
            await asyncio.to_thread(_apost, "/logbook", {"title": res["title"], "content": res["text"]})
        except Exception as e:  # noqa: BLE001 — .txt-Sync best-effort; der Gehirn-Eintrag ist schon zurueck
            _log(logging.WARNING, "Logbuch-.txt-Sync (schreiben) fehlgeschlagen", err=str(e), title=res.get("title"))
    res.pop("text", None)   # grossen Volltext nicht an den Browser zurueckgeben
    return res


@app.delete("/api/trash/all")
async def api_trash_empty() -> dict:
    """Leert den Papierkorb komplett (unwiderruflich). Proxy an brain DELETE /trash/all.
    Sync httpx via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    try:
        return await asyncio.to_thread(_bdelete, "/trash/all", user_id=USER_ID)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Papierkorb-Leeren fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Leeren fehlgeschlagen: {type(e).__name__}"})


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


# --- Google-Drive-Backup (Z -> Drive): Status lesen + Buttons. Das eigentliche Backup laeuft auf dem
#     HOST (rclone + systemd, crash-sicher). Das Dashboard liest nur die Status-Datei und schreibt
#     Trigger-Flags in die Z-Wurzel, die von Host-systemd-.path-Units ausgefuehrt werden. -----------
@app.get("/api/backup/status")
def backup_status() -> dict:
    """Liest die Status-Datei, die das Host-Backup-Skript in die Z-Wurzel schreibt. Fehlt sie -> 'unbekannt'."""
    p = Path(BACKUP_DIR) / ".gdrive-backup-status.json"
    try:
        if p.is_file():
            return json.loads(p.read_text(encoding="utf-8"))
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Backup-Status nicht lesbar", err=str(e))
    return {"state": "unbekannt", "detail": "Noch kein Backup gelaufen oder Status nicht verfuegbar."}


def _touch_trigger(name: str) -> bool:
    """Schreibt eine Trigger-Flag-Datei in die Z-Wurzel; die Host-systemd-.path-Unit fuehrt die Aktion aus."""
    try:
        (Path(BACKUP_DIR) / name).write_text(time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime()), encoding="utf-8")
        return True
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Trigger schreiben fehlgeschlagen", name=name, err=str(e))
        return False


@app.post("/api/backup/run")
def backup_run() -> dict:
    """'Jetzt sichern' — stoesst das Host-Backup an (Spiegel Z -> Drive, crash-sicher)."""
    ok = _touch_trigger(".backup-trigger")
    return {"ok": ok, "detail": "Sicherung angestossen." if ok else "Konnte Sicherung nicht anstossen (Schreibrecht?)."}


@app.post("/api/backup/restore")
def backup_restore() -> dict:
    """'Wiederherstellen' (Notfall) — holt das Drive-Backup additiv zurueck auf Z (loescht lokal nichts)."""
    ok = _touch_trigger(".restore-trigger")
    return {"ok": ok, "detail": "Wiederherstellung angestossen." if ok else "Konnte Wiederherstellung nicht anstossen (Schreibrecht?)."}


@app.post("/api/backup/connect")
async def backup_connect(request: Request) -> dict:
    """'Mit Google verbinden' — nimmt das rclone-OAuth-Token (Frank erzeugt es einmalig per
    `rclone authorize "drive"` auf seinem PC) und legt es im Steuer-Verzeichnis ab; die Host-systemd-
    Unit baut daraus die rclone-Verbindung. Das Token wird NICHT geloggt (Secret)."""
    body = await request.json()
    token = (body.get("token") or "").strip()
    if "access_token" not in token:
        return JSONResponse(status_code=400, content={"ok": False,
            "detail": "Bitte die komplette Ausgabe von 'rclone authorize \"drive\"' einfuegen (ein JSON mit access_token)."})
    try:
        (Path(BACKUP_DIR) / ".rclone-token").write_text(token, encoding="utf-8")
        return {"ok": True, "detail": "Token erhalten — Verbindung wird hergestellt..."}
    except Exception as e:  # noqa: BLE001 — Token NIE loggen, nur Fehlertyp
        _log(logging.WARNING, "Token ablegen fehlgeschlagen", err=type(e).__name__)
        return JSONResponse(status_code=500, content={"ok": False, "detail": "Konnte Token nicht ablegen (Schreibrecht?)."})


# --- Backup-Inhalts-Liste: was liegt AKTUELL im Z->Drive-Backup? GENERISCH ueber alle Top-Level-
#     Ordner, damit auch kuenftig dazukommende Komponenten automatisch mit erscheinen (Frank-Wunsch). -
_BACKUP_LABELS = {                        # Ordnername -> (Anzeigename, Einheit, Sortier-Rang)
    "qdrant-snapshot": ("Qdrant-Snapshots (Gehirn)", "Snapshots", 0),
    "Logbuch":         ("Logbuch-Protokolle",        "Protokolle", 1),
    "Eval-Logs":       ("Eval-Check-Logs",           "Logs",       2),
}


@app.get("/api/backup/contents")
def backup_contents() -> dict:
    """Komplettes Abbild des Z->Drive-Backups: pro Top-Level-Ordner Anzahl Dateien, Datum der
    neuesten Datei und Groesse. GENERISCH — neue Ordner erscheinen automatisch. Sync def ->
    Threadpool (fastapi §1: blockierendes Datei-I/O nie in einem async-Handler)."""
    root = Path(BACKUP_ROOT)
    try:
        dirs = [p for p in root.iterdir() if p.is_dir() and not p.name.startswith(".")]
    except Exception as e:  # noqa: BLE001 — Backup nicht gemountet/lesbar -> leere Liste statt 500
        _log(logging.WARNING, "Backup-Inhalt nicht lesbar", err=str(e), root=str(root))
        return {"ok": False, "items": [], "detail": "Backup-Verzeichnis nicht lesbar."}
    items: list[dict] = []
    total_files = total_bytes = 0
    for d in dirs:
        count = 0
        newest = 0.0
        size = 0
        try:
            for f in d.rglob("*"):
                if f.is_file() and not f.name.startswith("."):
                    try:
                        st = f.stat()
                    except OSError:
                        continue          # einzelne unlesbare Datei ueberspringen, nicht die Liste killen
                    count += 1
                    size += st.st_size
                    newest = max(newest, st.st_mtime)
        except Exception as e:  # noqa: BLE001 — ein kaputter Unterordner darf die Liste nicht killen
            _log(logging.WARNING, "Backup-Komponente nicht vollstaendig lesbar", comp=d.name, err=str(e))
        label, unit, rank = _BACKUP_LABELS.get(d.name, (d.name, "Dateien", 99))
        items.append({
            "key": d.name, "label": label, "unit": unit, "count": count,
            "last": (time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(newest)) if newest else None),
            "size_mb": round(size / 1048576, 2), "_rank": rank,
        })
        total_files += count
        total_bytes += size
    items.sort(key=lambda it: (it["_rank"], it["label"].lower()))
    for it in items:
        it.pop("_rank", None)
    return {"ok": True, "items": items,
            "total_files": total_files, "total_mb": round(total_bytes / 1048576, 1)}


@app.get("/", response_class=HTMLResponse)
def index() -> str:
    return (STATIC / "index.html").read_text(encoding="utf-8")
