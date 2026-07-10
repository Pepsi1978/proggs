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
(bugs/server/qdrant.md §4); gemini-embedding-2 (GA, multimodal): output_dimensionality EXPLIZIT 3072;
task_type entfaellt -> Text-Praefixe (title:|text: / 'task: search result | query:'); eine Liste aggregiert
-> pro Text 1 Call (bugs/apis/google-gemini-api.md §J)
(bugs/server/mem0.md §2, best-practices/second-brain/memory-backends.md §0); JSON-Log ensure_ascii=False
+ FileHandler encoding=utf-8 (bugs/claude-tooling/python-windows.md §1.1/§1.5).

Plan/Doku: best-practices/second-brain/UMSETZUNGSPLAN.md
"""
from __future__ import annotations

import hashlib
import hmac
import json
import logging
import os
import re
import threading
import time
import traceback
import unicodedata
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime, timezone
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

VERSION = "1.17.0"  # 1.17.0: NARRENSICHERES SEQUENTIELLES KATEGORIE-EINLESEN (Frank-Wunsch 2026-06-27, fuer GANZ schwache Modelle). NEU: GET /category-item?category=&index=N -> EIN Eintrag per 1-basiertem Index + 'total'. Der Client iteriert index=1..total, OHNE einen Titel raten oder die Liste parsen zu muessen (eine Zahl kann kein Modell vermasseln). Stabile Titel-Sortierung (konsistent ueber Aufrufe), OOM-sicher (Metadaten fuer die Liste, Volltext nur fuer den einen Eintrag). Loest den OpenCode/DeepSeek-Fehler an der Wurzel (Poka-Yoke Stufe 3), ohne eine Namensstruktur/einen Titel zu aendern. Rein additiv. 1.16.0: TOLERANTER TITEL-ABRUF (Frank-Bug 2026-06-27, OpenCode/DeepSeek). /by-title hasht den Titel exakt -> schwache LLMs klebten dem Titel die Listen-Dekoration ' [Kategorie]' (auch ' — N Zeichen' / fuehrende '92.') aus der list_memories-Zeile an -> Eintrag wurde NIE gefunden (reproduziert: 'Versionszaehler... [Programmierung/Rules]' -> Miss). Fix: resolve_title() macht /by-title UND DELETE /by-title tolerant in 3 Stufen (exakter Hash unveraendert -> Dekoration abstreifen+exakt -> Scroll-Fallback nur 'title'-Feld, eindeutiger normalisierter Vergleich). Rein additiv (schneller Hauptpfad unveraendert), funktionserhaltend, Eindeutigkeits-Pflicht (lieber 'nicht gefunden' als falscher Eintrag). 1.15.0: LADEFENSTER-SCHUTZ (Frank-Wunsch 2026-06-26, Direktive #3). Direkt nach Container-Neustart laedt Qdrant die Collection -> points_count/scroll liefern kurz UNVOLLSTAENDIG OHNE Fehler (Vorfall: brain_health zeigte 193 statt 1224, /list 175 statt 264 -> 'nur die Haelfte angezeigt'). Fix: /health + /list geben jetzt 'collection_status' (Qdrant green/yellow/grey/red) + 'ready' (green/yellow = Daten geladen+abfragbar) zurueck. REIN ADDITIV - kein bestehendes Feld, kein 'status', kein Lese-/Schreibpfad veraendert (0 Regressionsrisiko). MCP list_memories warnt bei ready=false ('Gehirn laedt noch') statt eine halbe Liste als vollstaendig auszugeben. qdrant.md §2c (Status green abwarten). 1.14.0: VOLLSTAENDIGER + SICHERER ABRUF, auch SEHR grosser Dokumente (Frank-Wunsch 2026-06-26, Direktive #3 — 'MCP perfekt, Daten vollstaendig abrufen, auch sehr grosse Dateien'). (a) by-title (= get_by_title im MCP) lud ALLE Chunks eines Docs mit je voller full_text-Kopie -> ein 1,4-Mio-Zeichen-Doc stuerzte brain-api beim Abruf per OOM ab (reproduziert). Jetzt limit=1 (1 Chunk genuegt, full_text 1:1 in jedem Chunk) -> grosse Docs abrufbar. (b) _scroll PAGINIERT jetzt (next_page_offset) -> laedt wirklich ALLE Punkte, auch >10.000 (vorher still abgeschnitten). limit=None=alle, limit=N=hoechstens N. (c) text_len ZENTRAL in _upsert_batched: Dokumentgroesse als kleines Feld -> /list (= list_memories 'X Zeichen') zeigt wieder die echte Groesse OHNE full_text zu laden (behebt die '0 Zeichen'-Regression aus 1.13.2). (d) by-category/by-parent laden nur chunk_index=0 je Doc -> kein N-Chunks-OOM bei grossen Docs. Backfill setzt text_len auf den Bestand. search/recall (begrenzte overfetch) folgt separat. 1.13.2: OOM-LOOP-FIX Teil 2 (Frank-Bug 2026-06-26) — der eigentliche PERIODISCHE Trigger war /list: das Dashboard pollt /api/overview ALLE 20s (index.html setInterval), das brain-api /list ruft; /list lud per _scroll(with_payload=True) ALLE Volltexte in den RAM, nur fuer die im Frontend UNGENUTZTE 'chars'-Laenge. Jetzt /list metadaten-only (with_payload=['doc_id','title','category','created_at','updated_at'], chars=0). Zusammen mit dem category-counts-Fix (1.13.1) ist der OOM-Loop an der WURZEL behoben (Direktive #3). by_category/by_parent geben weiter full_text (Drawer-Anzeige) -> on-demand-Absicherung folgt separat. 1.13.1: OOM-LOOP-FIX (Frank-Bug 2026-06-26) — category-counts lud per _scroll(with_payload=True) ALLE Punkte MIT full_text (1:1 in JEDEM Chunk) in den RAM, nur um Kategorien zu ZAEHLEN. Bei 60-70 grossen Almanachen >2 GB -> Container-OOM -> Neustart-Loop alle ~20s (brain-api war staendig kurz nicht erreichbar -> Dashboard zeigte '0 Eintraege'/'Server nicht erreichbar'). Fix: _scroll bekommt with_payload-Parameter; category-counts laedt NUR ['doc_id','categories','category'] (Zaehl-Felder, KEIN full_text) -> Speicher winzig, funktionserhaltend (zaehlt identisch). fastapi §8/§9 (nicht alles gleichzeitig in den RAM). Daten unberuehrt (990 points). 1.13.0: SEHR grosse Dokumente speichern OHNE Qdrant-32MB-Crash (Frank-Bug 2026-06-25, vom Regressionstest gefunden). Root Cause 2: full_text haengt 1:1 in JEDEM Chunk -> ein 600k-Zeichen-Doc (~150 Chunks) ergab ~102 MB in EINEM Upsert -> Qdrant lehnt > 32 MB ab (400). Fix: _upsert_batched() splittet jeden Upsert in Batches unter UPSERT_MAX_BYTES (Default 24 MB) — funktionserhaltend (dieselben Punkte/Payloads, nur mehrere Requests), kein Lesepfad veraendert. Gilt fuer ALLE Schreibwege (store/entry-category/entry-categories/update/trash_restore/reembed-all). 1.12.0: SEHR grosse Dokumente zuverlaessig (Frank-Wunsch 2026-06-25, Direktive-#3-Debugging). Edit-/Papierkorb-Text-Cap 200_000 -> 1_000_000 (deckt Franks 20-30x-Almanache); StoreReq.text bleibt bewusst UNGECAPPT (= der eigentliche Speicherpfad, chunkt selbst). Diese Caps sind reine OOM-Backstops (fastapi §8), nie stille Kuerzung. Hintergrund: der urspruengliche 'halbe Datei'-Bug sass NICHT hier — full_text wird in jedem Chunk 1:1 gespeichert (Z. 528, Sonde Z. 535) und by_title/by_category/by_parent/search geben full_text 1:1 zurueck; die Kuerzung war ein STILLER text[:8000]-Slice im dashboard VOR der Speicherung (#47233). Regressionstest: tests/large_doc_roundtrip.py. 1.11.0: MEHRFACH-KATEGORIEN pro Eintrag (Multi-Category, Frank-Wunsch 2026-06-25, Etappe 1). Payload-Arrays 'categories'/'parents' (Keyword-Index) zusaetzlich zu primaer 'category'/'parent' (abwaertskompatibel). embed_input() nimmt die Kategorie-LISTE -> ALLE Kategorien + Hierarchie-Ebenen ('A/B/C' -> 'A > B > C') praegen den Vektor (Eintrag in allen Kategorien semantisch auffindbar). Filter by-category/by-parent/search matchen das Array ODER das alte Einzelfeld (nested should, keine Uebergangsluecke). category-counts zaehlt pro Kategorie (Eintrag in jeder seiner) + total_distinct (Eintraege gesamt = doc_id-dedupliziert, Frank: Gesamt zaehlt 1x). NEU POST /entry/categories (volle Liste setzen + re-embed). /reembed-all backfillt categories/parents (= Migration + Re-Embed in einem). Schreibwege store/update/entry-category/trash_restore reichen die Liste durch. 1.10.0: TITEL praegt jetzt das EMBEDDING mit (Frank-Wunsch 2026-06-25). embed_input() stellt '[Titel: T | Kategorie: K]' dem Embed-Input voran (full_text/chunk_text bleiben 1:1); identifizierende Titel sind starke Diskriminatoren (rag-retrieval §4). Alle Speicher-Wege (store/update_entry/entry-category/trash_restore) reichen den Titel ins Embedding durch -> jede Aenderung fuehrt zum neuen Vektor MIT Titel. NEU: POST /reembed-all bettet den Bestand mit dem aktuellen Titel+Kategorie-Schema neu ein (Vektor neu, Payload 1:1, idempotent, 100er-Batches). 1.9.0: Unterkategorien-Fundament (Frank-Wunsch 2026-06-25, Phase 1). (a) 2-Ebenen-Kategorie 'Haupt/Unter' -> zusaetzliches Payload-Feld 'parent' (Teil vor '/') + keyword-Index, weil Qdrant KEINEN Praefix-Operator hat (bugs/server/qdrant.md §7); GET /by-parent (alles unter Haupt), POST /backfill-parent (parent auf Altbestand via set_payload, kein Re-Embed). (b) Kategorie praegt jetzt das EMBEDDING mit: embed_input() stellt '[Kategorie: X]' dem Embed-Input voran (full_text/chunk_text bleiben 1:1) -> bessere Treffer (rag-retrieval §4). Folge: Kategorie-Wechsel (POST /entry/category) embeddet jetzt FRISCH statt set_payload. store/update_entry/trash_restore setzen parent + Kategorie-Praefix. /search um parent-Filter erweitert. 1.8.0: Eintrag-Bearbeitung im Drawer (Frank-Wunsch 2026-06-25). (a) PUT /entry kann jetzt auch den TITEL aendern — UpdateReq.title (optional); bei echter Titel-Aenderung wandert der Eintrag auf die neue (titel-basierte) doc_id (alte doc_id wird geloescht, Ziel-Titel-Kollision wird ersetzt wie /store), created_at/category bleiben; Antwort gibt die neue doc_id + title_changed; Sonde stellt sicher dass keine Geist-doc_id zurueckbleibt. (b) POST /entry/category — Kategorie EINES Eintrags per set_payload aendern (Vektor unangetastet, KEIN Re-Embed), fuer das Kategorie-Dropdown im Drawer; mit Intent-Sonde. 1.7.0: POST /purge {user_id} — HARTES Loeschen ALLER Eintraege eines TEST-Nutzers (qc.delete, kein Papierkorb), fuer die Eval-Aufraeumung. Schutz: nur 'eval*'-Nutzer, NIEMALS 'frank' (403). 1.6.0: Kategorie-Verwaltung — POST /rename-category (set_payload auf allen Chunks, Vektor bleibt; bei existierendem Ziel = Merge), POST /detach-category (Kategorie-Etikett entfernen, Eintraege bleiben 1:1 erhalten — loescht NIE einen Eintrag), GET /category-counts (Payload-Kategorien mit Eintragszahl auf doc_id-Ebene). 1.5.0: Eintraege nach Aktualitaet sortiert — /by-category + /list geben das NEUESTE zuerst zurueck (Frank-Wunsch 2026-06-24: Kategorie-Ansicht war unsortiert), via _sort_recent (updated_at, sonst created_at); created_at jetzt in beiden Listen-Antworten. 1.4.0: Papierkorb (Soft-Delete) — DELETE /entry verschiebt jetzt in den Papierkorb (trash.json, persistentes /app/data-Volume) statt endgueltig zu loeschen; GET /trash (neueste zuerst), PUT /trash (Text im Papierkorb editieren, ohne Re-Embed), POST /trash/restore (frisch embedden + unter doc_id zurueck ins Gehirn, created_at erhalten). 1.3.0: DELETE /entry — Eintrag dauerhaft per doc_id loeschen (alle Chunks), fuer den Papierkorb-Button im Dashboard-Drawer (Frank-Wunsch). 1.2.0: PUT /entry — Eintrag per doc_id 1:1 ersetzen (alte Vektoren loeschen, neuen Text frisch embedden, Titel/Kategorie/created_at erhalten); doc_id jetzt in allen Listen-/Abruf-Antworten (Frontend-Editor). 1.1.0: /search um Payload-Filter (Kategorie + Datum/Bereich). 1.0.0: mem0 raus -> direkter 1:1-Speicher.

# Historischer Runtime-Limit-Bump; die aktive sichtbare Version steht weiter unten nach der 1.22.x-Historie.
VERSION = "1.18.0 (06.07.2026, 13:47 Uhr)"  # 1.18.0: Runtime-Limits fuer Cortex-Einstellungen. /limits liefert und speichert Chunking, dense Overfetch und BM25-Kandidaten persistent in brain-data; /search und chunk_text nutzen die Werte sofort. Defaults bleiben Env-kompatibel; bestehende Eintraege bleiben 1:1 erhalten.

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
ENTITY_COLLECTION = os.getenv("SB_ENTITY_COLLECTION", "brain_entities")   # Entity-Register (Nr. 36)
EMBED_MODEL = os.getenv("GEMINI_EMBED_MODEL", "gemini-embedding-2")
EMBED_DIMS = int(os.getenv("SB_EMBED_DIMS", "3072"))
# gemini-embedding-2 (multimodal, GA): task_type entfaellt -> Text-Praefixe; eine Liste an embed_content
# AGGREGIERT zu EINEM Vektor -> pro Text EIN Call, parallel (bugs/apis/google-gemini-api.md §J23/§J24).
# (gemini-embedding-001-Fallback bewusst entfernt 2026-07-08 auf Frank-Wunsch — reproduzierbar ueber
#  docs/superpowers/specs + plans, falls je wieder gewuenscht.)
EMBED_PARALLEL = int(os.getenv("SB_EMBED_PARALLEL", "8"))             # parallele Einzel-Calls
def _env_int(name: str, default: int) -> int:
    try:
        return int(os.getenv(name, str(default)))
    except (TypeError, ValueError):
        return default


# Chunking fuer die SUCHE. gemini-embedding-2: 8192 Token Input-Limit (~12000 Zeichen konservativ).
# Per Env uebersteuerbar (SB_CHUNK_CHARS / SB_CHUNK_OVERLAP).
CHUNK_CHARS = _env_int("SB_CHUNK_CHARS", 12000)
CHUNK_OVERLAP = _env_int("SB_CHUNK_OVERLAP", 600)
SEARCH_OVERFETCH_FACTOR = _env_int("SB_SEARCH_OVERFETCH_FACTOR", 4)
SEARCH_DATE_OVERFETCH_FACTOR = _env_int("SB_SEARCH_DATE_OVERFETCH_FACTOR", 20)
BM25_CANDIDATE_FACTOR = _env_int("SB_BM25_CANDIDATE_FACTOR", 4)
BM25_MIN_CANDIDATES = _env_int("SB_BM25_MIN_CANDIDATES", 20)
MAX_EMBED_CALLS_PER_DAY = int(os.getenv("SB_MAX_EMBED_CALLS_PER_DAY", "20000"))  # Defense-in-Depth-Cap
# Qdrant lehnt einen Upsert-Request > ~32 MB mit 400 ab. Da der 1:1-Volltext (full_text) in JEDEM
# Chunk haengt, kann EIN grosses Dokument (viele Chunks) ein einzelnes Upsert ueber 32 MB treiben.
# -> Upserts werden in Batches unter dieser Grenze gesplittet (24 MB lassen Headroom fuer Vektoren).
UPSERT_MAX_BYTES = int(os.getenv("SB_UPSERT_MAX_BYTES", str(24 * 1024 * 1024)))
LOG_PATH = os.getenv("SB_LOG_PATH", "/app/logs/brain-api.jsonl")
LOG_LEVEL = os.getenv("SB_LOG_LEVEL", "INFO").upper()
# Papierkorb (Soft-Delete): geloeschte Eintraege landen als JSON 1:1 hier (persistentes Volume),
# damit sie im Dashboard wiederhergestellt/editiert werden koennen. KEINE Vektoren — reiner Textspeicher.
DATA_DIR = os.getenv("SB_DATA_DIR", "/app/data")
TRASH_PATH = os.getenv("SB_TRASH_PATH", os.path.join(DATA_DIR, "trash.json"))
LIMITS_FILE = Path(os.getenv("SB_LIMITS_FILE", os.path.join(DATA_DIR, "runtime-limits.json")))
DEFAULT_BRAIN_LIMITS = {
    "chunk_chars": CHUNK_CHARS,
    "chunk_overlap": CHUNK_OVERLAP,
    "search_overfetch_factor": SEARCH_OVERFETCH_FACTOR,
    "search_date_overfetch_factor": SEARCH_DATE_OVERFETCH_FACTOR,
    "bm25_candidate_factor": BM25_CANDIDATE_FACTOR,
    "bm25_min_candidates": BM25_MIN_CANDIDATES,
}
BRAIN_LIMIT_BOUNDS = {
    "chunk_chars": (500, 20000),
    "chunk_overlap": (0, 5000),
    "search_overfetch_factor": (1, 20),
    "search_date_overfetch_factor": (1, 50),
    "bm25_candidate_factor": (1, 20),
    "bm25_min_candidates": (1, 1000),
}


_CATEGORY_ALIASES: dict[str, str] = {
    "gespräche": "Gespräche",
    "gespraeche": "Gespräche",
}


def category_key(category: str | None) -> str:
    return unicodedata.normalize("NFC", (category or "").strip()).casefold()


def canonical_category(category: str | None) -> str:
    c = unicodedata.normalize("NFC", (category or "").strip())
    return _CATEGORY_ALIASES.get(category_key(c), c)


def category_aliases(category: str | None) -> list[str]:
    c = canonical_category(category)
    if category_key(c) == "gespräche":
        return ["Gespräche", "gespräche", "gespraeche"]
    return [c] if c else []


def _category_match_filter(category: str) -> Filter:
    aliases = category_aliases(category)
    return Filter(should=[cond for alias in aliases for cond in (
        FieldCondition(key="categories", match=MatchValue(value=alias)),
        FieldCondition(key="category", match=MatchValue(value=alias)),
    )])


def _parent_match_filter(parent: str) -> Filter:
    aliases = category_aliases(parent)
    return Filter(should=[cond for alias in aliases for cond in (
        FieldCondition(key="parents", match=MatchValue(value=alias)),
        FieldCondition(key="parent", match=MatchValue(value=alias)),
    )])


def is_overview_total_excluded(category: str | None) -> bool:
    c = category_key(category)
    return c in {"gespräche", "gespraeche"} or c == "bugfixes" or c.startswith("bugfixes/")

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


def _coerce_brain_limit(key: str, value: Any) -> int:
    low, high = BRAIN_LIMIT_BOUNDS[key]
    try:
        n = int(value)
    except (TypeError, ValueError):
        n = DEFAULT_BRAIN_LIMITS[key]
    return max(low, min(high, n))


def current_brain_limits() -> dict[str, int]:
    return {
        "chunk_chars": CHUNK_CHARS,
        "chunk_overlap": CHUNK_OVERLAP,
        "search_overfetch_factor": SEARCH_OVERFETCH_FACTOR,
        "search_date_overfetch_factor": SEARCH_DATE_OVERFETCH_FACTOR,
        "bm25_candidate_factor": BM25_CANDIDATE_FACTOR,
        "bm25_min_candidates": BM25_MIN_CANDIDATES,
    }


def apply_brain_limits(limits: dict[str, Any]) -> dict[str, int]:
    global CHUNK_CHARS, CHUNK_OVERLAP, SEARCH_OVERFETCH_FACTOR, SEARCH_DATE_OVERFETCH_FACTOR
    global BM25_CANDIDATE_FACTOR, BM25_MIN_CANDIDATES
    clean = {k: _coerce_brain_limit(k, limits.get(k, DEFAULT_BRAIN_LIMITS[k])) for k in DEFAULT_BRAIN_LIMITS}
    clean["chunk_overlap"] = min(clean["chunk_overlap"], max(0, clean["chunk_chars"] - 1))
    CHUNK_CHARS = clean["chunk_chars"]
    CHUNK_OVERLAP = clean["chunk_overlap"]
    SEARCH_OVERFETCH_FACTOR = clean["search_overfetch_factor"]
    SEARCH_DATE_OVERFETCH_FACTOR = clean["search_date_overfetch_factor"]
    BM25_CANDIDATE_FACTOR = clean["bm25_candidate_factor"]
    BM25_MIN_CANDIDATES = clean["bm25_min_candidates"]
    return clean


def load_brain_limits() -> dict[str, int]:
    merged = dict(DEFAULT_BRAIN_LIMITS)
    try:
        if LIMITS_FILE.exists():
            stored = json.loads(LIMITS_FILE.read_text(encoding="utf-8"))
            if isinstance(stored, dict):
                merged.update({k: stored[k] for k in DEFAULT_BRAIN_LIMITS if k in stored})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "runtime-limits.json nicht lesbar — nutze Defaults", err=str(e))
    return apply_brain_limits(merged)


def save_brain_limits(update: dict[str, Any]) -> dict[str, int]:
    cur = current_brain_limits()
    for k in DEFAULT_BRAIN_LIMITS:
        if k in update:
            cur[k] = update[k]
    clean = apply_brain_limits(cur)
    LIMITS_FILE.parent.mkdir(parents=True, exist_ok=True)
    tmp = LIMITS_FILE.with_suffix(".tmp")
    tmp.write_text(json.dumps(clean, ensure_ascii=False, indent=2), encoding="utf-8", newline="\n")
    os.replace(tmp, LIMITS_FILE)
    return clean


load_brain_limits()


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
probe(EMBED_DIMS in (768, 1536, 3072), "EMBED_DIMS unerwartet (gemini-embedding-2: 768/1536/3072)", dims=EMBED_DIMS)

# Optionaler Datetime-Range-Filter (qdrant-client >= ~1.8). Fehlt er -> Datum-Filter in /search
# degradiert sauber auf einen Python-Nachfilter (Funktionserhalt, Direktive #3).
try:
    from qdrant_client.models import DatetimeRange
    HAS_DATETIME_RANGE = True
except Exception:  # noqa: BLE001
    DatetimeRange = None  # type: ignore
    HAS_DATETIME_RANGE = False

def _init_store() -> None:
    """Gemini-/Qdrant-Clients verbinden + Collection/Indizes sicherstellen. Laeuft beim Start und
    wird von _require_store NACHGEHOLT, wenn der Start am Qdrant-Boot-Race scheiterte (compose
    depends_on wartet nur auf START, nicht 'bereit' — docker.md §3). Vorher blieb der Container
    dann DAUERHAFT degraded/503 bis zum manuellen Neustart (Self-Healing, Direktive #3)."""
    global qc, gclient, init_error
    # Timeout PFLICHT (fastapi.md Kurzcheck #5 / ai-agent §3.3): ohne haengt ein toter Google-Call
    # unbegrenzt und bindet einen anyio-Threadpool-Worker. google-genai erwartet Millisekunden.
    gclient = genai.Client(api_key=GEMINI_API_KEY,
                           http_options=genai_types.HttpOptions(timeout=60_000))
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
    for _field in ("doc_id", "title", "category", "categories", "parent", "parents", "user_id", "source"):
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
    # Entity-Register (Level-2 Nr. 36, Hub-and-Spoke): zweite, KLEINE Collection — eine Entitaet
    # (Person/Ort/Geraet/Projekt/Praeparat/...) je Punkt, mit verknuepften doc_ids im Payload.
    # Vektor = Embedding des Namens+Typs+Aliases (fuers fuzzy Wiederfinden). KEIN Voll-Graph
    # (belegt Overkill, memory-evolution Kurzcheck #6).
    if ENTITY_COLLECTION not in _existing:
        qc.create_collection(
            collection_name=ENTITY_COLLECTION,
            vectors_config=VectorParams(size=EMBED_DIMS, distance=Distance.COSINE),
        )
        _log(logging.INFO, "Entity-Collection angelegt", collection=ENTITY_COLLECTION, dims=EMBED_DIMS)
    for _field in ("name_key", "type", "user_id"):
        try:
            qc.create_payload_index(collection_name=ENTITY_COLLECTION, field_name=_field, field_schema="keyword")
        except Exception:  # noqa: BLE001 — Index existiert bereits / nicht kritisch
            pass
    init_error = None
    _log(logging.INFO, "Speicher initialisiert", embed_model=EMBED_MODEL, dims=EMBED_DIMS,
         collection=COLLECTION, qdrant_url=QDRANT_URL)


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

    _init_store()
except Exception as e:  # noqa: BLE001 — Init-Fehler vollstaendig festhalten, nicht still
    init_error = f"{type(e).__name__}: {e}"
    qc = None
    gclient = None
    log.error("Speicher-Init fehlgeschlagen (Self-Healing versucht es bei Requests erneut)", exc_info=True)

# Re-Init-Drossel fuer das Self-Healing (max. 1 Versuch alle 5s, thread-sicher — Threadpool-Handler)
_init_retry_lock = threading.Lock()
_init_last_attempt = 0.0

# Sichtbarer Patch-Stand: Detail-/Such-APIs liefern den ursprünglichen Speicherzeitpunkt fuer das Dashboard aus.
# 1.20.0 (Tiefen-Debugging PERFORMANCE 2026-07-02): (a) embed_many — Chunk-Vektoren werden je
# Schreibweg (store/update/entry-category/entry-categories/trash-restore/reembed-all) GEBUENDELT
# in Batches (SB_EMBED_BATCH=16) statt seriell je Chunk embedded: identische Vektoren, aber ein
# 150-Chunk-Dokument macht ~10 statt 150 Gemini-Round-Trips (Speichern grosser Docs: Minuten -> Sekunden);
# (b) category-counts + /list + resolve_title scannen nur noch Chunk 0 je Dokument (Metadaten sind in
# jedem Chunk identisch, Ergebnis doc_id-dedupliziert EXAKT gleich, Scan ~5x kleiner — category-counts
# laeuft bei JEDEM 20s-Dashboard-Poll); (c) Payload-Eingrenzung fuer backfill-parent (nur category/
# parent statt aller full_texts — qdrant-§8-OOM-Klasse), /purge (nur doc_id), _doc_id_exists +
# update-leftover-Probe (with_payload=False), /store-Existenzcheck (nur created_at). Alles
# verhaltensneutral: gleiche Antworten, gleiche Zaehlungen, gleiche Vektoren.
# Alt: 1.19.0 (Tiefen-Debugging 2026-07-02): (a) /by-date OOM-sicher (chunk_index=0 + nativer Datums-Filter
# statt ALLE Chunks ALLER Docs mit full_text zu laden — gleiche Fehlerklasse wie 1.13.x/1.14, qdrant §8);
# (b) rename-category/detach-category pflegen jetzt AUCH das Multi-Category-Array 'categories' +
# 'parent'/'parents' (seit 1.11.0 blieben Eintraege unter dem ALTEN Namen zaehl-/findbar, detach war
# fuer Array-Eintraege wirkungslos — cats_from_payload bevorzugt das Array); (c) Papierkorb speichert
# die Mehrfach-Kategorien mit (Restore verlor sie, nur die primaere blieb); (d) Embedding-Tages-Zaehler
# thread-sicher (Handler laufen im Threadpool); (e) /reembed-all laedt den Bestand seitenweise per
# retrieve statt ALLE Volltexte auf einmal (OOM-Backstop); (f) Self-Healing-Init: scheitert der Start
# am Qdrant-Boot-Race, holt _require_store die Initialisierung throttled nach (vorher blieb der
# Container dauerhaft 'degraded'/503 bis zum manuellen Neustart).
# 1.21.0 (Level-2 Such-Intelligenz, 2026-07-04): (a) HYBRID-SUCHE — /search kombiniert die
# semantische Vektorsuche mit einer BM25-Stichwortsuche ueber ALLE Chunk-Texte (+Titel/Kategorien)
# und fusioniert beide Ranglisten per RRF (Reciprocal Rank Fusion, k=60). Exakte Begriffe
# (Geraetenamen, Praeparate, Projektnamen), die das Embedding "verwaescht", treffen jetzt sicher.
# Bewusst IN-PROZESS statt Qdrant-Sparse-Migration: null Risiko am Produktivbestand, kein
# fastembed/ONNX im Image; Index cached (TTL + Invalidierung bei jedem Schreibweg), laedt NUR
# chunk_text/Metadaten (nie full_text -> kein OOM, qdrant §8). Notaus: SB_HYBRID=0.
# (b) ENTITY-REGISTER (Hub-and-Spoke, leichtes Entity-Linking statt Voll-Graph — memory-evolution
# Kurzcheck #6): zweite Collection 'brain_entities' mit REINEN Speicher-Endpunkten
# /entities/upsert|find|list|docs|delete (KEIN LLM — brain-api bleibt stummer Speicher; die
# Extraktion macht der Agent). Entity = Name+Typ+Aliases+verknuepfte doc_ids, Vektor fuers
# fuzzy Wiederfinden. /entities/docs raeumt tote doc_ids lazy auf (Self-Healing).
# Antwort-Felder /search additiv erweitert: matched_by, dense_score, bm25_rank, retrieval.
# 1.22.0: /entities/list?with_docs=1 liefert zusaetzlich die verknuepften doc_ids je Entitaet —
# der Nachtschicht-Bibliothekar (librarian) braucht sie fuer den Nachzuegler-Lauf (Eintraege OHNE
# Entity-Verknuepfung finden). Rein additiv, Default-Antwort unveraendert.
# Alt: 1.21.1: /purge raeumt jetzt AUCH die Entitaeten des eval-Test-Nutzers auf (der erweiterte
# Eval-Check legt Test-Entitaeten an) + invalidiert den BM25-Cache (purge lief an _delete_doc vorbei).
VERSION = "1.24.0 (06.07.2026, 17:23 Uhr)"  # 1.24.0: Dashboard-Limits vervollstaendigt. Zusaetzlich zu Chunking, dense Overfetch und BM25 ist jetzt auch search_date_overfetch_factor persistent ueber /limits einstellbar; Datumsfragen ohne nativen DatetimeRange-Fallback koennen damit gruendlicher suchen, ohne die uebrigen Suchpfade zu veraendern. Alt: 1.23.0.

VERSION = "1.26.0 (08.07.2026, 18.50 Uhr)"  # 1.26.0: UMSTIEG AUF gemini-embedding-2 (3072 Dim, GA). task_type -> Text-Praefixe (Dokument 'title: … | text: …', Suchanfrage 'task: search result | query: …'); embed_many nutzt bei E2 EINEN Call je Text (eine Liste wuerde AGGREGIEREN, bugs/apis/google-gemini-api.md §J23), parallel ueber ThreadPool (SB_EMBED_PARALLEL); Chunk-Default 12000 Zeichen (8192-Token-Limit statt 2048). Modell-abhaengig: Fallback auf gemini-embedding-001 (1536) bleibt allein per Env moeglich (task_type + Listen-Batch). Blau/Gruen-Migration via migrate_to_embedding2.py. Alt: 1.25.0.
VERSION = "1.27.0 (08.07.2026, 20.06 Uhr)"  # 1.27.0 (Frank-Wunsch 2026-07-08): gemini-embedding-001-Fallback KOMPLETT ausgebaut — nur noch gemini-embedding-2 (3072). IS_EMBED2-Verzweigung + Batch-Pfad (EMBED_BATCH) + '[Titel:…]'-Altpraefix entfernt; embed/embed_many/embed_input/_entity_embed_text sind reine E2-Logik. Verhalten fuer den Nutzer unveraendert (E2 lief bereits live). Wiederherstellung von -001 jederzeit reproduzierbar ueber docs/superpowers/specs + plans. Alt: 1.26.0.
VERSION = "1.27.1 (08.07.2026, 20:14 Uhr)"  # 1.27.1: FIX Kategorie-Doppelung Gespräche. Altbestand 'gespräche' und neue Kategorie 'Gespräche' werden Unicode-normalisiert, case-insensitiv erkannt und kanonisch als 'Gespräche' gezaehlt/abgerufen; neue Schreibwege normalisieren bekannte Varianten. Alt: 1.27.0.
VERSION = "1.27.2 (10.07.2026, 11:34 Uhr)"  # 1.27.2: Tiefen-Debugging-Haertung. Gemini-Embedding-Calls mit 60s-Timeout + Backoff-Retry (nur transiente 429/5xx/Netzfehler); Bearer-Vergleich konstantzeitig (hmac.compare_digest); limit=0-Zaehlpfad dedupliziert ueber chunk_index=0 + serverseitiges qc.count (identisches Ergebnis, ~5x weniger Scan); created_at-None-Guard in /by-date; Embedding-Tages-Cap liest unter Lock. Alt: 1.27.1.

# Startup-Banner (Observability-First: Log-Pfad + Version EINMAL ausgeben)
_log(logging.INFO, "brain-api startet", version=VERSION, log_path=LOG_PATH)

# ---------------------------------------------------------------------------
# Defense-in-Depth-Kostenbremse (harter Cap liegt beim Google-AI-Studio-Budget)
# ---------------------------------------------------------------------------
_embed_calls = {"day": str(date.today()), "count": 0}
_embed_lock = threading.Lock()  # Handler laufen im Threadpool (def) -> Zaehler-Mutation thread-sicher halten


def _guard_embed_budget(n: int = 1) -> None:
    today = str(date.today())
    with _embed_lock:
        if _embed_calls["day"] != today:
            _embed_calls["day"], _embed_calls["count"] = today, 0
        _embed_calls["count"] += n
        current = _embed_calls["count"]   # unter dem Lock lesen (kein Race mit parallelen Handlern)
    if current > MAX_EMBED_CALLS_PER_DAY:
        _log(logging.ERROR, "Embedding-Tages-Cap erreicht", count=current, cap=MAX_EMBED_CALLS_PER_DAY)
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


def _embed_call(content: str):
    """EIN embed_content-Call mit kleinem Retry gegen TRANSIENTE Google-Fehler (429/5xx/Netz/Timeout).
    Permanente Fehler (400/401/403 …) schlagen sofort durch — die soll niemand wegretryen.
    Backoff blockiert nur den eigenen Threadpool-Worker (Handler sind def), nie den Event-Loop.
    (ai-agent-frameworks.md §6.2: Backoff statt Sofort-Retry; fastapi.md §3: Timeouts + begrenzte Retries.)"""
    last: Exception | None = None
    for attempt in range(3):
        try:
            return gclient.models.embed_content(
                model=EMBED_MODEL,
                contents=content,
                config=genai_types.EmbedContentConfig(output_dimensionality=EMBED_DIMS),
            )
        except Exception as e:  # noqa: BLE001 — klassifizieren, gezielt retryen, sonst durchreichen
            code = getattr(e, "code", None) or getattr(getattr(e, "response", None), "status_code", None)
            name = type(e).__name__.lower()
            transient = code in (429, 500, 502, 503, 504) or "timeout" in name or "connect" in name or "transport" in name
            last = e
            if not transient or attempt == 2:
                raise
            wait = 2.0 * (attempt + 1)
            _log(logging.WARNING, "Gemini-Embedding transient fehlgeschlagen — Retry",
                 versuch=attempt + 1, wartezeit_s=wait, fehler=type(e).__name__, code=code)
            time.sleep(wait)
    raise last  # pragma: no cover — logisch unerreichbar (raise oben), Sicherheitsnetz


def embed(text: str, task_type: str) -> list[float]:
    """Text -> Vektor via gemini-embedding-2. task_type ist der interne Modus-Indikator: bei
    'RETRIEVAL_QUERY' wird das offizielle Query-Praefix vorangestellt; Dokumente sind bereits via
    embed_input als 'title: … | text: …' formatiert. task_type wird NICHT an die API gesendet
    (bei gemini-embedding-2 entfernt, bugs/apis/google-gemini-api.md §J24)."""
    content = f"task: search result | query: {text}" if task_type == "RETRIEVAL_QUERY" else text
    resp = _embed_call(content)
    vec = list(resp.embeddings[0].values)
    probe(len(vec) == EMBED_DIMS, "Embedding-Dimension weicht ab", got=len(vec), want=EMBED_DIMS)
    return vec


def embed_many(texts: list[str], task_type: str) -> list[list[float]]:
    """MEHRERE Texte einbetten (gemini-embedding-2): EIN embed_content-Call je Text (eine Liste wuerde
    AGGREGIEREN, bugs/apis/google-gemini-api.md §J23), parallel ueber einen ThreadPool (der Handler
    laeuft selbst als def im anyio-Threadpool -> zulaessig, fastapi §1), Reihenfolge streng an den Index
    gebunden. Vektor-ANZAHL == Eingabe-Anzahl, sonst HART abbrechen (RuntimeError) statt Vektoren still
    falsch zuzuordnen (Direktive #3)."""
    def _one(t: str) -> list[float]:
        content = f"task: search result | query: {t}" if task_type == "RETRIEVAL_QUERY" else t
        r = _embed_call(content)
        v = list(r.embeddings[0].values)
        probe(len(v) == EMBED_DIMS, "Embedding-Dimension weicht ab", got=len(v), want=EMBED_DIMS)
        return v
    with ThreadPoolExecutor(max_workers=max(1, EMBED_PARALLEL)) as ex:
        out = list(ex.map(_one, texts))   # ex.map bewahrt die Eingabe-Reihenfolge
    if len(out) != len(texts):
        raise RuntimeError(f"Einzel-Embedding: {len(out)} Vektoren fuer {len(texts)} Texte")
    return out


def _upsert_batched(points: list, wait: bool = True) -> None:
    """Upsert, der das Qdrant-Request-Limit (~32 MB) NICHT sprengt. Weil der 1:1-Volltext
    (full_text) in JEDEM Chunk haengt, kann ein grosses Dokument (viele Chunks) ein einzelnes
    Upsert ueber 32 MB treiben -> Qdrant lehnt mit 400 ab (Frank-Bug 2026-06-25: 600k-Zeichen-
    Dokument ≈ 102 MB Payload). Wir splitten in Batches unter UPSERT_MAX_BYTES. Funktionserhaltend:
    es werden DIESELBEN Punkte mit DENSELBEN Payloads gespeichert, nur ueber mehrere Requests."""
    batch: list = []
    size = 0
    for p in points:
        pl = p.payload or {}
        # text_len ZENTRAL fuer ALLE Schreibwege: die Dokumentgroesse (= len full_text) als kleines
        # Zahl-Feld mitspeichern, damit Listen/Zaehlungen die Groesse OHNE den vollen Text laden
        # koennen (chars-Anzeige in list_memories; Frank-Bug 2026-06-26 — sonst muesste man full_text
        # in den RAM ziehen = OOM-Gefahr). full_text ist 1:1 in jedem Chunk -> len ist eindeutig.
        pl["text_len"] = len(pl.get("full_text") or "")
        p.payload = pl
        est = len((pl.get("full_text") or "").encode("utf-8")) + len(pl.get("chunk_text") or "") + 8192
        if batch and size + est > UPSERT_MAX_BYTES:
            qc.upsert(collection_name=COLLECTION, points=batch, wait=wait)
            batch, size = [], 0
        batch.append(p)
        size += est
    if batch:
        qc.upsert(collection_name=COLLECTION, points=batch, wait=wait)
    _bm25_invalidate()   # Hybrid-Suche: Stichwort-Index beim naechsten /search frisch bauen


def make_doc_id(user_id: str, title: str | None) -> str:
    """Titel -> deterministische doc_id (gleicher Titel = gleiche ID = ueberschreiben).
    Ohne Titel -> frische UUID (neuer Eintrag)."""
    if title and title.strip():
        h = hashlib.sha1(f"{user_id}::{title.strip().lower()}".encode("utf-8")).hexdigest()[:24]
        return f"t_{h}"
    return f"d_{uuid.uuid4().hex}"


# ---------------------------------------------------------------------------
# Toleranter Titel-Resolver (Frank-Bug 2026-06-27, OpenCode/DeepSeek)
# ---------------------------------------------------------------------------
# make_doc_id hasht den Titel EXAKT (lower+strip) -> jeder Fremdzusatz = Miss. Schwache LLMs
# rekonstruieren den Titel aus der /list- bzw. MCP-list_memories-Zeile "{Titel} [{Kategorie}] —
# {N} Zeichen" und haengen dabei das " [Kategorie]"-Suffix (oder " — N Zeichen" / die fuehrende
# Listennummer "92.") faelschlich AN den Titel -> /by-title fand den Eintrag nie. resolve_title
# macht den Abruf tolerant, OHNE den schnellen exakten Hauptpfad zu aendern (rein additiv,
# funktionserhaltend; Direktive #3). Eindeutigkeits-Pflicht: lieber "nicht gefunden" als der
# falsche Eintrag.
_LIST_NUM_PREFIX = re.compile(r"^\s*\d+\.\s+")                                 # "92. " am Anfang
_SIZE_SUFFIX = re.compile(r"\s*[—–-]\s*\d[\d.,]*\s*Zeichen\s*$", re.IGNORECASE)  # " — 2121 Zeichen" am Ende
_CAT_SUFFIX = re.compile(r"\s*\[[^\[\]]*\]\s*$")                               # " [Programmierung/Rules]" am Ende


def strip_list_decorations(title: str) -> str:
    """Entfernt Listen-Dekorationen, die LLMs faelschlich an den Titel kleben: fuehrende
    Listennummer, abschliessendes ' — N Zeichen' und abschliessendes ' [Kategorie]'. In einer
    Schleife, weil sie kombiniert auftreten ('92. Titel [Kat] — N Zeichen')."""
    prev, out = None, title
    while out != prev:
        prev = out
        out = _SIZE_SUFFIX.sub("", out)
        out = _CAT_SUFFIX.sub("", out)
        out = _LIST_NUM_PREFIX.sub("", out)
        out = out.strip()
    return out


def _norm_cmp(s: str) -> str:
    """Vergleichsnormalform: Dekorationen abstreifen, Whitespace kollabieren, lowercase."""
    return re.sub(r"\s+", " ", strip_list_decorations(s)).strip().lower()


def _doc_id_exists(doc_id: str) -> bool:
    # with_payload=False (Performance 2026-07-02): fuer den Existenz-Check braucht es KEINEN Payload —
    # vorher wurde der volle full_text (bei grossen Docs bis 1,4 MB) nur fuer ein bool geladen.
    return bool(_scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]),
                        limit=1, with_payload=False))


def resolve_title(user_id: str, title: str) -> "tuple[str, str] | None":
    """Findet den ECHTEN gespeicherten Titel + doc_id zu einem (evtl. LLM-'verschmutzten') Titel.
    Gibt (echter_titel, doc_id) zurueck oder None. Drei Stufen, schnellster Pfad zuerst:
      1. exakter Hash (unveraenderter Hauptpfad).
      2. Listen-Dekorationen abstreifen -> erneut exakt hashen (deckt '[Kategorie]' / '— N Zeichen' / '92.').
      3. Scroll-Fallback: alle Titel des Nutzers (NUR 'title'-Feld, KEIN full_text -> kein OOM, fastapi §8)
         normalisiert case-insensitiv vergleichen; nur bei EINDEUTIGEM Treffer uebernehmen."""
    did = make_doc_id(user_id, title)
    if _doc_id_exists(did):
        return title, did
    stripped = strip_list_decorations(title)
    if stripped and stripped != title:
        did2 = make_doc_id(user_id, stripped)
        if _doc_id_exists(did2):
            return stripped, did2
    needle = _norm_cmp(title)
    if not needle:
        return None
    matches: dict[str, str] = {}   # doc_id -> echter gespeicherter Titel
    # Performance (2026-07-02): nur Chunk 0 je Dokument — der Titel ist in jedem Chunk identisch.
    for p in _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id)),
                                  FieldCondition(key="chunk_index", match=MatchValue(value=0))]),
                     with_payload=["doc_id", "title"]):
        t = (p.payload.get("title") or "").strip()
        if t and _norm_cmp(t) == needle:
            matches[p.payload.get("doc_id")] = t
    if len(matches) == 1:
        did3, real = next(iter(matches.items()))
        return real, did3
    return None


def category_parent(category: str | None) -> str:
    """Hauptkategorie aus einer 2-Ebenen-Kategorie 'Haupt/Unter' -> 'Haupt' (Teil vor dem ersten '/').
    Ohne '/' ist die Kategorie selbst die Hauptkategorie. Leer -> ''. Wird als eigenes Payload-Feld
    'parent' gespeichert, damit 'alles unter Haupt' ein exakter MatchValue-Filter ist (Qdrant hat
    KEINEN Praefix-Operator, bugs/server/qdrant.md §7)."""
    c = canonical_category(category)
    if not c:
        return ""
    return c.split("/", 1)[0].strip()


def _dedup(seq: list[str]) -> list[str]:
    """Reihenfolge erhalten, Unicode/case-insensitiv deduplizieren; bekannte Aliase kanonisieren."""
    seen: set = set(); out: list[str] = []
    for x in seq:
        x = canonical_category(x)
        k = category_key(x)
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
        out = _dedup([canonical_category(c) for c in raw if isinstance(c, str) and c.strip()])
        if out:
            return out
    one = canonical_category(pl.get("category"))
    return [one] if one else []


def norm_cats(categories: list[str] | None, category: str | None) -> list[str]:
    """Eingehende Kategorien (Liste ODER einzelner String, beide Request-Formen) -> saubere,
    deduplizierte Liste; Reihenfolge erhalten, erste = primaer."""
    if categories:
        return _dedup([canonical_category(c) for c in categories if isinstance(c, str) and c.strip()])
    if category and category.strip():
        return [canonical_category(category)]
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
    # Offizielles gemini-embedding-2-Dokumentpraefix 'title: {…} | text: {…}' (ohne Titel -> 'none').
    # Kategorien fliessen in den title-Teil -> metadata-enriched retrieval bleibt erhalten (§4).
    head = t
    if exp:
        head = f"{t} · {', '.join(exp)}" if t else ", ".join(exp)
    return f"title: {head or 'none'} | text: {text}"


def point_id(doc_id: str, idx: int) -> str:
    """Deterministische Qdrant-Point-ID (UUID) je Chunk."""
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"sb/{doc_id}/{idx}"))


def _delete_doc(doc_id: str) -> None:
    qc.delete(collection_name=COLLECTION,
              points_selector=Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]))
    _bm25_invalidate()   # Hybrid-Suche: geloeschte Eintraege sofort aus dem Stichwort-Index


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


def _scroll(flt: "Filter | None", limit: "int | None" = None, with_payload=True, page: int = 1000) -> list:
    # Paginiert ueber ALLE passenden Punkte (Frank 2026-06-26: 'vollstaendig abrufen' — auch >10.000
    # Chunks, die ein EINZELNER scroll-Aufruf still abschneiden wuerde). limit=None -> ALLE (in
    # page-grossen Seiten via next_page_offset). limit=N -> hoechstens N (z.B. by-title limit=1).
    # with_payload: True = ganzer Payload (inkl. full_text, der 1:1 in JEDEM Chunk haengt);
    # Feldliste = nur diese Felder -> OOM-Schutz bei Zaehl-/Listen-Operationen (nicht alle Volltexte
    # gleichzeitig in den RAM; Frank-Bug 2026-06-26, fastapi §8/§9 + qdrant §8).
    out: list = []
    off = None
    while True:
        want = page if limit is None else min(page, limit - len(out))
        if want <= 0:
            break
        pts, off = qc.scroll(collection_name=COLLECTION, scroll_filter=flt, limit=want,
                             offset=off, with_payload=with_payload)
        out.extend(pts)
        if off is None or not pts:
            break
    return out


# ---------------------------------------------------------------------------
# Hybrid-Suche Teil 1: BM25-Stichwort-Index (in-Prozess, gecacht) + RRF-Fusion
# ---------------------------------------------------------------------------
# WARUM in-Prozess statt Qdrant-Sparse-Vektoren: Sparse-Vektoren erfordern eine Collection-
# Migration des Produktivbestands + fastembed/ONNX im Image (bewusst entfernt 2026-06-23).
# Bei Franks Korpusgroesse (hunderte Eintraege, wenige tausend Chunks) laeuft ein Python-BM25
# in Millisekunden — gleiche Funktion (exakte Begriffe treffen), null Migrationsrisiko.
# OOM-SICHER: der Index laedt NUR chunk_text (max ~4 KB je Chunk) + kleine Metadaten,
# NIEMALS full_text (qdrant §8 / fastapi §8). Cache: TTL + Invalidierung bei jedem Schreibweg.
HYBRID_ENABLED = os.getenv("SB_HYBRID", "1").strip() not in ("0", "false", "no")
BM25_TTL_S = float(os.getenv("SB_BM25_TTL_S", "60"))    # Index-Cache-Lebensdauer
BM25_K1 = float(os.getenv("SB_BM25_K1", "1.5"))
BM25_B = float(os.getenv("SB_BM25_B", "0.75"))
RRF_K = int(os.getenv("SB_RRF_K", "60"))                # Standard-Konstante der RRF-Formel

_TOKEN_RE = re.compile(r"[a-z0-9äöüß]+")


def _bm25_tokens(text: str) -> list[str]:
    """Einfache, deutsch-freundliche Tokenisierung (lowercase, Wortzeichen inkl. Umlaute)."""
    return _TOKEN_RE.findall((text or "").lower())


class _Bm25Index:
    """BM25 ueber alle Chunks eines Nutzers. Dokumente = Chunks (Titel+Kategorien+chunk_text),
    Ergebnis wird auf doc_id-Ebene dedupliziert (bestes Chunk gewinnt)."""

    def __init__(self, chunks: list[dict]):
        import math
        self.chunks = chunks                       # je: {doc_id,title,category,categories,parent,parents,created_at,updated_at,text}
        self.tf: list[dict] = []                   # Term-Frequenzen je Chunk
        self.dl: list[int] = []                    # Dokumentlaengen
        df: dict[str, int] = {}                    # Dokument-Frequenz je Term
        for c in chunks:
            toks = _bm25_tokens(f"{c.get('title') or ''} {' '.join(c.get('categories') or [])} {c.get('text') or ''}")
            freq: dict[str, int] = {}
            for t in toks:
                freq[t] = freq.get(t, 0) + 1
            self.tf.append(freq)
            self.dl.append(len(toks))
            for t in freq:
                df[t] = df.get(t, 0) + 1
        n = max(1, len(chunks))
        self.avgdl = (sum(self.dl) / n) if chunks else 0.0
        # IDF nach BM25+ (nie negativ — haeufige Woerter zaehlen einfach wenig)
        self.idf = {t: math.log(1.0 + (n - d + 0.5) / (d + 0.5)) for t, d in df.items()}

    def search(self, query: str, limit: int, allow) -> list[tuple[dict, float]]:
        """Top-Chunks fuer die Query, doc_id-dedupliziert. 'allow' = Praedikat ueber die
        Chunk-Metadaten (Kategorie-/Parent-/Datums-Scope wie im dense-Pfad)."""
        q = _bm25_tokens(query)
        if not q or not self.chunks:
            return []
        best: dict[str, tuple[dict, float]] = {}
        for i, c in enumerate(self.chunks):
            if not allow(c):
                continue
            freq, dl = self.tf[i], self.dl[i]
            score = 0.0
            for t in q:
                f = freq.get(t)
                if not f:
                    continue
                idf = self.idf.get(t, 0.0)
                score += idf * (f * (BM25_K1 + 1)) / (f + BM25_K1 * (1 - BM25_B + BM25_B * dl / max(1.0, self.avgdl)))
            if score <= 0.0:
                continue
            did = c.get("doc_id")
            if did not in best or score > best[did][1]:
                best[did] = (c, score)
        ranked = sorted(best.values(), key=lambda x: x[1], reverse=True)
        return ranked[:limit]


_bm25_cache: dict[str, tuple[float, "_Bm25Index"]] = {}   # user_id -> (built_at, index)
_bm25_lock = threading.Lock()


def _bm25_invalidate() -> None:
    """Nach JEDEM Schreibweg (Upsert/Delete) den Index verwerfen — naechste Suche baut frisch."""
    with _bm25_lock:
        _bm25_cache.clear()


def _bm25_index(user_id: str) -> "_Bm25Index":
    """Gecachter BM25-Index (TTL + Schreib-Invalidierung). Baut aus NUR chunk_text + Metadaten."""
    now = time.monotonic()
    with _bm25_lock:
        hit = _bm25_cache.get(user_id)
        if hit and now - hit[0] < BM25_TTL_S:
            return hit[1]
    # Build AUSSERHALB des Locks (Scroll/Bau nicht serialisieren, fastapi §2-Prinzip)
    pts = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]),
                  with_payload=["doc_id", "title", "category", "categories", "parent", "parents",
                                "created_at", "updated_at", "chunk_text"])
    chunks = [{
        "doc_id": p.payload.get("doc_id"), "title": p.payload.get("title") or "",
        "category": p.payload.get("category") or "", "categories": cats_from_payload(p.payload),
        "parent": p.payload.get("parent") or "", "parents": p.payload.get("parents") or [],
        "created_at": p.payload.get("created_at") or "", "updated_at": p.payload.get("updated_at"),
        "text": p.payload.get("chunk_text") or "",
    } for p in pts]
    idx = _Bm25Index(chunks)
    with _bm25_lock:
        _bm25_cache[user_id] = (time.monotonic(), idx)
    probe(len(chunks) < 200_000, "BM25-Index ungewoehnlich gross", chunks=len(chunks))
    return idx


def _rrf_fuse(ranked_lists: list[list[str]], k: int = None) -> dict[str, float]:
    """Reciprocal Rank Fusion: doc_id -> Summe 1/(k+rank) ueber alle Ranglisten (rang-basiert,
    score-frei — genau darum robust gegen unterschiedliche Score-Skalen von dense vs. BM25)."""
    kk = k if k is not None else RRF_K
    fused: dict[str, float] = {}
    for lst in ranked_lists:
        for rank, did in enumerate(lst, start=1):
            fused[did] = fused.get(did, 0.0) + 1.0 / (kk + rank)
    return fused


# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------
def require_auth(authorization: str = Header(default="")) -> None:
    expected = f"Bearer {SB_API_KEY}"
    # hmac.compare_digest: konstantzeitiger Vergleich (kein Timing-Seitenkanal aufs Token)
    if not SB_API_KEY or not hmac.compare_digest(authorization.encode(), expected.encode()):
        raise HTTPException(status_code=401, detail="Unauthorized")


def _require_store() -> None:
    global _init_last_attempt, init_error
    if qc is not None and gclient is not None:
        return
    # Self-Healing (Direktive #3): der Start scheiterte (typisch: Qdrant beim Boot noch nicht bereit,
    # depends_on wartet nur auf START) -> throttled neu initialisieren statt dauerhaft 503 zu bleiben.
    with _init_retry_lock:
        if (qc is None or gclient is None) and time.monotonic() - _init_last_attempt >= 5.0:
            _init_last_attempt = time.monotonic()
            try:
                _init_store()
                _log(logging.INFO, "Speicher-Init nachgeholt (Self-Healing)")
            except Exception as e:  # noqa: BLE001 — naechster Request versucht es erneut (gedrosselt)
                init_error = f"{type(e).__name__}: {e}"
                log.error("Speicher-Re-Init fehlgeschlagen", exc_info=True)
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
    source: str | None = Field(default="manual", max_length=40, description="Herkunft des Schreibers, z.B. manual, entropyreductor oder librarian")
    user_id: str = Field(default="frank", description="Besitzer (aktuell immer 'frank')")


class SearchReq(BaseModel):
    query: str = Field(..., min_length=1)
    user_id: str = Field(default="frank")
    limit: int = Field(default=5, ge=0, description="0 = alle Treffer im gefilterten Suchraum; sonst Top-N")
    # Optionale Payload-Filter: erst eingrenzen, DANN semantisch suchen (Franks "war letzten Monat angeln").
    category: str | None = Field(default=None, description="Nur in dieser exakten (Unter-)Kategorie suchen ('Haupt/Unter')")
    parent: str | None = Field(default=None, description="Nur unter dieser Hauptkategorie suchen (= alles in 'Haupt/...'), via parent-Feld")
    date: str | None = Field(default=None, description="Nur Eintraege dieses Tages (YYYY-MM-DD)")
    date_from: str | None = Field(default=None, description="Ab diesem Tag (YYYY-MM-DD, inklusive)")
    date_to: str | None = Field(default=None, description="Bis zu diesem Tag (YYYY-MM-DD, inklusive)")


class LimitsReq(BaseModel):
    limits: dict[str, int] = Field(default_factory=dict, description="Runtime-Limits fuer Suche und Chunking")


class UpdateReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des zu ersetzenden Eintrags (aus /list, /by-category, /search)")
    text: str = Field(..., min_length=1, max_length=1_000_000, description="Neuer 1:1-Text — ersetzt den alten Vektor komplett. Sehr grosse Dokumente OK (brain-api chunkt selbst); max_length ist NUR ein OOM-Backstop (fastapi §8), KEINE stille Kuerzung — ueber der Grenze lehnt Pydantic laut via 422 ab.")
    title: str | None = Field(default=None, max_length=200, description="Optionaler NEUER Titel. None -> alter Titel bleibt. Aendert sich der Titel, wandert der Eintrag auf die neue (titel-basierte) doc_id (Frank-Wunsch: Titel im Drawer bearbeiten).")
    categories: list[str] | None = Field(default=None, max_length=12, description="Optional NEUE Kategorie-Liste (Multi-Category). None -> bisherige Kategorien bleiben.")
    user_id: str = Field(default="frank")


class EntryCategoriesReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des Eintrags, dessen Kategorie-Liste gesetzt wird")
    categories: list[str] = Field(..., min_length=1, max_length=12, description="Vollstaendige neue Kategorie-Liste (Multi-Category, 1:1 deutsche Rechtschreibung). Re-Embed, da die Kategorien den Vektor mitpraegen.")
    user_id: str = Field(default="frank")


class TrashEditReq(BaseModel):
    doc_id: str = Field(..., min_length=1, description="ID des Papierkorb-Eintrags")
    text: str = Field(..., min_length=1, max_length=1_000_000, description="Neuer Text IM Papierkorb (kein Re-Embed; gilt erst bei Wiederherstellung). Sehr grosse Dokumente OK; max_length nur OOM-Backstop (fastapi §8), keine stille Kuerzung.")
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
    col_status, ready = None, False
    try:
        if qc is not None:
            info = qc.get_collection(COLLECTION)
            qdrant_ok = True
            count = info.points_count
            # Qdrant-Collection-Status (green/yellow/grey/red): green/yellow = Daten geladen + abfragbar;
            # grey/red = laedt noch (direkt nach Container-Neustart) bzw. Fehler. WICHTIG (Frank-Bug
            # 2026-06-26): waehrend des Ladefensters liefert points_count/scroll UNVOLLSTAENDIGE Werte
            # (zeigte 193 statt 1224). 'ready' macht das Ladefenster fuer Clients erkennbar, statt eine
            # halbe Zahl als Wahrheit zu nehmen (qdrant.md §2c: Status green abwarten). points ist erst
            # bei ready verlaesslich. Rein additiv -> bestehende Felder/'status' unveraendert.
            col_status = getattr(info.status, "value", str(info.status)).lower()
            ready = col_status in ("green", "yellow")
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
        "collection_status": col_status,   # NEU: Qdrant-Ladezustand (green/yellow=bereit)
        "ready": ready,                    # NEU: True = Daten vollstaendig geladen -> Zahlen verlaesslich
        "points": count,
        "embed_model": EMBED_MODEL,
        "embed_dims": EMBED_DIMS,
        "embed_calls_today": _embed_calls["count"],
    }


@app.get("/limits", dependencies=[Depends(require_auth)])
def get_limits() -> dict:
    return {"ok": True, "limits": current_brain_limits(), "defaults": DEFAULT_BRAIN_LIMITS}


@app.put("/limits", dependencies=[Depends(require_auth)])
def put_limits(req: LimitsReq) -> dict:
    limits = save_brain_limits(req.limits or {})
    _log(logging.INFO, "Brain-API-Limits gespeichert", **limits)
    return {"ok": True, "limits": limits, "defaults": DEFAULT_BRAIN_LIMITS}


@app.post("/store", dependencies=[Depends(require_auth)])
def store(req: StoreReq) -> dict:
    """Speichert Text WORTWOERTLICH 1:1. Mit Titel: ersetzt einen vorhandenen Eintrag gleichen Titels."""
    _require_store()
    doc_id = make_doc_id(req.user_id, req.title)
    now = iso_now()
    created_at = now
    replaced = False
    if req.title and req.title.strip():
        # Nur created_at laden (Performance 2026-07-02): der volle Payload (inkl. full_text des
        # ALTEN Dokuments) wurde hier nur fuer das Erstellungsdatum in den RAM gezogen.
        existing = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]),
                           limit=1, with_payload=["created_at"])
        replaced = bool(existing)
        if replaced:
            created_at = existing[0].payload.get("created_at", now)  # Erstellungsdatum erhalten, nur updated_at neu
            _delete_doc(doc_id)  # gleicher Titel -> alte Version komplett raus, dann neu

    cats = norm_cats(req.categories, req.category)
    category = cats[0] if cats else ""        # primaere Kategorie (Abwaertskompat)
    parents = category_parents(cats)
    parent = parents[0] if parents else ""    # primaerer Haupt-Teil (Abwaertskompat)
    source = (req.source or "manual").strip().lower() or "manual"
    chunks = chunk_text(req.text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    # Batch-Embedding (Performance 2026-07-02): alle Chunk-Vektoren gebuendelt statt seriell je Chunk
    vecs = embed_many([embed_input(req.title, cats, ch) for ch in chunks], "RETRIEVAL_DOCUMENT")
    for i, (ch, vec) in enumerate(zip(chunks, vecs)):
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
            "source": source,
        }))
    _upsert_batched(points)

    # Intent-Checkpoint: beweist, dass der gespeicherte Volltext EXAKT die Eingabe ist (1:1)
    stored_ok = bool(points) and points[0].payload["full_text"] == req.text
    checkpoint("store", "Text wird WORTWOERTLICH 1:1 gespeichert (keine KI-Bearbeitung)", ok=stored_ok,
               title=req.title or None, category=req.category or None, chunks=len(chunks),
               chars=len(req.text), replaced=replaced, source=source, ms=int((time.time() - t0) * 1000))
    return {"ok": True, "doc_id": doc_id, "title": req.title or None,
            "category": req.category or None, "chunks": len(chunks), "chars": len(req.text),
            "source": source, "replaced": replaced}


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
    real_title = title
    # NUR 1 Chunk laden: full_text ist 1:1 in JEDEM Chunk -> ein Chunk genuegt fuer das GANZE Dokument.
    # limit=1000 lud bei einem sehr grossen Doc (z.B. 1,4 Mio Zeichen, viele Chunks) ALLE Chunks mit je
    # einer vollen full_text-Kopie -> hunderte MB gleichzeitig im RAM -> OOM BEIM ABRUF (Frank-Bug
    # 2026-06-26: 1,4M-Doc liess sich nicht abrufen, brain-api stuerzte ab). limit=1 = sicher + vollstaendig.
    points = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]), limit=1)
    if not points:
        # Toleranter Fallback: LLM hat dem Titel Listen-Dekorationen angeklebt ('[Kategorie]',
        # '— N Zeichen', '92.'). resolve_title liefert den ECHTEN Titel + doc_id (Frank-Bug 2026-06-27).
        resolved = resolve_title(user_id, title)
        if resolved:
            real_title, doc_id = resolved
            points = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]), limit=1)
            checkpoint("by_title", "Toleranter Fallback hat verschmutzten Titel aufgeloest", ok=True,
                       angefragt=title, aufgeloest=real_title)
    if not points:
        checkpoint("by_title", "Abruf per Titel", ok=False, found=False, title=title)
        return {"ok": True, "found": False, "title": title, "text": None}
    full = points[0].payload.get("full_text", "")
    checkpoint("by_title", "Abruf per Titel gibt das GANZE Dokument 1:1 zurueck", ok=bool(full),
               title=real_title, chars=len(full))
    return {"ok": True, "found": True, "title": real_title, "doc_id": doc_id,
            "category": points[0].payload.get("category") or None,
            "categories": cats_from_payload(points[0].payload),
            "source": points[0].payload.get("source") or None,
            "created_at": points[0].payload.get("created_at"),
            "updated_at": points[0].payload.get("updated_at"), "text": full}


@app.get("/by-category", dependencies=[Depends(require_auth)])
def by_category(category: str, user_id: str = "frank") -> dict:
    """Alle Eintraege einer Kategorie (auf Dokument-Ebene dedupliziert), jeweils 1:1."""
    _require_store()
    cat = canonical_category(category)
    points = _scroll(Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=user_id)),
        # NUR Chunk 0 je Dokument: full_text ist 1:1 in JEDEM Chunk -> 1 Chunk genuegt; verhindert,
        # dass ALLE Chunks (jeder mit voller full_text-Kopie) grosser Docs in den RAM kommen (OOM-Schutz,
        # Frank-Bug 2026-06-26 — ein 1,4M-Doc je Kategorie haette sonst N Kopien geladen).
        FieldCondition(key="chunk_index", match=MatchValue(value=0)),
        # Multi-Category: matcht das neue Array 'categories' ODER (Abwaertskompat) das alte 'category'
        _category_match_filter(cat),
    ]))
    seen: dict[str, dict] = {}
    for p in points:
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                          "text": p.payload.get("full_text", ""), "category": canonical_category(p.payload.get("category")) or None,
                          "categories": cats_from_payload(p.payload),
                          "source": p.payload.get("source") or None,
                          "created_at": p.payload.get("created_at"),
                          "updated_at": p.payload.get("updated_at")}
    items = _sort_recent(seen.values())   # Neueste zuerst (nach Aktualitaet)
    return {"ok": True, "category": cat, "count": len(items), "items": items}


@app.get("/category-item", dependencies=[Depends(require_auth)])
def category_item(category: str, index: int = 1, user_id: str = "frank") -> dict:
    """EINEN Eintrag einer Kategorie per 1-basiertem Index -> der GANZE Text 1:1, plus 'total'.
    Zweck (Frank-Wunsch 2026-06-27): narrensicheres, sequentielles Einlesen einer ganzen Kategorie
    (z.B. der Arbeitsregeln) fuer JEDES Modell — auch ganz schwache. Der Client iteriert index=1..total,
    OHNE einen Titel raten oder eine Liste parsen zu muessen (eine simple Zahl kann kein Modell vermasseln).
    STABILE Reihenfolge (nach Titel sortiert), damit die Iteration ueber mehrere Aufrufe konsistent bleibt
    (unabhaengig von updated_at). OOM-sicher: fuer die Liste NUR Metadaten, den Volltext NUR fuer den
    EINEN gewuenschten Eintrag (fastapi §8, wie /by-title)."""
    _require_store()
    cat = canonical_category(category)
    # 1) NUR Metadaten aller Eintraege der Kategorie (KEIN full_text -> kein OOM auch bei grossen Kategorien)
    points = _scroll(Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=user_id)),
        FieldCondition(key="chunk_index", match=MatchValue(value=0)),   # 1 Chunk je Doc -> 1 Zeile je Eintrag
        _category_match_filter(cat),
    ]), with_payload=["doc_id", "title"])
    seen: dict[str, str] = {}   # doc_id -> Titel (dedupliziert)
    for p in points:
        did = p.payload.get("doc_id")
        if did and did not in seen:
            seen[did] = (p.payload.get("title") or "").strip()
    # Stabile, deterministische Reihenfolge ueber alle Aufrufe (nach Titel, dann doc_id)
    ordered = sorted(seen.items(), key=lambda kv: (kv[1].lower(), kv[0]))
    total = len(ordered)
    if total == 0:
        return {"ok": True, "found": False, "category": cat, "index": index, "total": 0, "text": None}
    if index < 1 or index > total:
        return {"ok": True, "found": False, "category": cat, "index": index, "total": total,
                "text": None, "error": f"index {index} ausserhalb 1..{total}"}
    doc_id, title = ordered[index - 1]
    # 2) Volltext NUR fuer diesen einen Eintrag (limit=1; full_text liegt 1:1 in jedem Chunk)
    pts = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=doc_id))]), limit=1)
    full = pts[0].payload.get("full_text", "") if pts else ""
    checkpoint("category_item", "Sequentieller Kategorie-Abruf per Index (1:1)", ok=bool(full),
               category=cat, index=index, total=total, title=title, chars=len(full))
    return {"ok": True, "found": True, "category": cat, "index": index, "total": total,
            "title": title, "doc_id": doc_id,
            "categories": cats_from_payload(pts[0].payload) if pts else [],
            "source": (pts[0].payload.get("source") or None) if pts else None, "text": full}


@app.get("/by-parent", dependencies=[Depends(require_auth)])
def by_parent(parent: str, user_id: str = "frank") -> dict:
    """Alle Eintraege UNTER einer Hauptkategorie (= 'Haupt' und alle 'Haupt/Unter'), via parent-Feld
    (exakter MatchValue — Qdrant hat keinen Praefix-Operator). Auf Dokument-Ebene dedupliziert, 1:1."""
    _require_store()
    par = canonical_category(parent)
    points = _scroll(Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=user_id)),
        # NUR Chunk 0 je Dokument (full_text 1:1 in jedem Chunk) -> OOM-Schutz bei grossen Docs (s. by_category).
        FieldCondition(key="chunk_index", match=MatchValue(value=0)),
        # Multi-Category: matcht das neue Array 'parents' ODER (Abwaertskompat) das alte 'parent'
        _parent_match_filter(par),
    ]))
    seen: dict[str, dict] = {}
    for p in points:
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "text": p.payload.get("full_text", ""), "category": canonical_category(p.payload.get("category")) or None,
                          "categories": cats_from_payload(p.payload),
                          "parent": canonical_category(p.payload.get("parent")) or None,
                          "source": p.payload.get("source") or None,
                          "created_at": p.payload.get("created_at"), "updated_at": p.payload.get("updated_at")}
    items = _sort_recent(seen.values())
    return {"ok": True, "parent": par, "count": len(items), "items": items}


@app.post("/backfill-parent", dependencies=[Depends(require_auth)])
def backfill_parent(user_id: str = "frank") -> dict:
    """Einmal-Migration: setzt das 'parent'-Feld (Hauptkategorie vor dem '/') auf ALLEN bestehenden
    Eintraegen per set_payload (KEIN Re-Embed — billig). Macht 'alles unter Haupt' auch fuer den
    Altbestand filterbar. Idempotent: kann gefahrlos mehrfach laufen. Sync def -> Threadpool (fastapi §1)."""
    _require_store()
    # OOM-Schutz (2026-07-02, gleiche Klasse wie qdrant §8): NUR die Kategorie-Felder laden —
    # der Default (voller Payload) zog hier ALLE full_texts ALLER Chunks in den RAM (bei grossen
    # Almanachen hunderte MB). KEIN chunk_index-Filter: das parent-Feld muss auf JEDEN Chunk.
    points = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]),
                     with_payload=["category", "parent"])
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
    # OOM-Schutz: NUR die Zaehl-Felder laden (doc_id + Kategorien), NICHT full_text — sonst zieht
    # ein einzelner category-counts-Abruf ALLE Volltexte gleichzeitig in den RAM (Frank-Bug
    # 2026-06-26: 60-70 grosse Almanache -> >2 GB -> OOM-Loop). fastapi §8/§9 (nicht alles in den RAM).
    # Performance (2026-07-02): NUR Chunk 0 je Dokument scannen — Kategorien/doc_id sind in JEDEM
    # Chunk identisch, das Ergebnis (doc_id-dedupliziert) bleibt EXAKT gleich, aber der Scan laeuft
    # ueber ~5x weniger Punkte. Wichtig: dieser Endpoint laeuft bei JEDEM 20s-Dashboard-Poll.
    points = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id)),
                                  FieldCondition(key="chunk_index", match=MatchValue(value=0))]),
                     with_payload=["doc_id", "categories", "category"])
    seen: dict[str, set] = {}
    countable_docs: set = set()
    for p in points:
        did = p.payload.get("doc_id")
        cats = cats_from_payload(p.payload)
        if any(not is_overview_total_excluded(c) for c in cats):
            countable_docs.add(did)
        for c in cats:   # Multi-Category: Eintrag zaehlt in JEDER seiner Kategorien
            seen.setdefault(c, set()).add(did)
    counts = {c: len(dids) for c, dids in seen.items()}
    # total_distinct = Anzahl sichtbarer Nutz-Eintraege (jeder doc_id 1x). Gespraeche und bugfixes/*
    # bleiben als Kategorien sichtbar, zaehlen aber nicht in 'Eintraege gesamt'.
    checkpoint("category_counts", "Kategorien mit Eintragszahl (Multi-Category, doc_id-dedupliziert) + total_distinct",
               ok=True, categories=len(counts), total=len(countable_docs))
    return {"ok": True, "counts": counts, "total_distinct": len(countable_docs)}


@app.post("/rename-category", dependencies=[Depends(require_auth)])
def rename_category(req: RenameCategoryReq) -> dict:
    """Benennt eine Kategorie in ALLEN Payloads um (set_payload — der Vektor bleibt unangetastet,
    KEIN Re-Embedding). Existiert 'new' bereits, ist das Ergebnis ein Merge (alle 'old'-Eintraege
    wandern nach 'new'). Loescht NIE einen Eintrag."""
    _require_store()
    old_raw = (req.old or "").strip()
    old, new = canonical_category(old_raw), canonical_category(req.new)
    if not old or not new:
        raise HTTPException(status_code=400, detail="old/new duerfen nicht leer sein")
    flt = Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=req.user_id)),
        # Multi-Category (seit 1.11.0): auch Eintraege matchen, die 'old' NUR im Array 'categories'
        # tragen (z.B. sekundaere Kategorie) — vorher blieben die komplett unangetastet.
        _category_match_filter(old),
    ])
    # OOM-sicher: NUR die Kategorie-Felder laden, KEIN full_text (qdrant §8 / Frank-Bug 2026-06-26)
    pts = _scroll(flt, with_payload=["doc_id", "category", "categories"])
    docs = {p.payload.get("doc_id") for p in pts}
    if (old_raw != new or old != new) and pts:
        # KERN-FIX (Tiefen-Debugging 2026-07-02): das komplette Kategorie-Feld-Set je Punkt neu
        # ableiten — primaeres 'category', das Array 'categories' UND 'parent'/'parents'. Vorher
        # wurde NUR 'category' umgeschrieben; cats_from_payload bevorzugt aber das Array, daher
        # blieb der Eintrag unter dem ALTEN Namen zaehl-/findbar (category-counts, by-category,
        # by-parent). Gebuendelt je identischem Ziel-Payload (wenige set_payload-Requests).
        groups: dict[str, tuple[dict, list]] = {}
        for p in pts:
            pl = p.payload or {}
            cats = cats_from_payload(pl)
            new_cats = _dedup([new if category_key(c) == category_key(old) else c for c in cats]) or [new]
            old_primary = canonical_category(pl.get("category"))
            cat_primary = new if category_key(old_primary) == category_key(old) else old_primary
            parents = category_parents(new_cats)
            payload = {"category": cat_primary, "categories": new_cats,
                       "parent": category_parent(cat_primary) or (parents[0] if parents else ""),
                       "parents": parents}
            key = json.dumps(payload, sort_keys=True, ensure_ascii=False)
            groups.setdefault(key, (payload, []))[1].append(p.id)
        for payload, ids in groups.values():
            # wait=True: Operation abgeschlossen bevor das Dashboard neu laedt (sonst alte Werte sichtbar)
            qc.set_payload(collection_name=COLLECTION, payload=payload, points=ids, wait=True)
    checkpoint("rename_category", "Kategorie in allen Payloads umbenannt (inkl. categories-Array + parent/parents; Vektor unangetastet, kein Re-Embed)",
               ok=True, old=old, new=new, points=len(pts), entries=len(docs))
    return {"ok": True, "old": old, "new": new, "points": len(pts), "entries": len(docs)}


@app.post("/detach-category", dependencies=[Depends(require_auth)])
def detach_category(req: DetachCategoryReq) -> dict:
    """Loescht die KATEGORIE, nicht die Eintraege: setzt category auf '' bei allen Eintraegen
    dieser Kategorie. Die Eintraege (Vektoren + Volltext) bleiben vollstaendig erhalten — nur das
    Kategorie-Etikett faellt weg. NIEMALS ein Eintrag geloescht."""
    _require_store()
    name = canonical_category(req.name)
    if not name:
        raise HTTPException(status_code=400, detail="name darf nicht leer sein")
    flt = Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=req.user_id)),
        # Multi-Category (seit 1.11.0): auch Eintraege matchen, die das Etikett NUR im Array tragen
        _category_match_filter(name),
    ])
    # OOM-sicher: NUR die Kategorie-Felder laden, KEIN full_text (qdrant §8)
    pts = _scroll(flt, with_payload=["doc_id", "category", "categories"])
    docs = {p.payload.get("doc_id") for p in pts}
    if pts:
        # KERN-FIX (Tiefen-Debugging 2026-07-02): das Etikett auch aus dem Array 'categories'
        # entfernen + 'parent'/'parents' neu ableiten. Vorher wurde nur 'category' geleert;
        # cats_from_payload bevorzugt das Array -> der Eintrag blieb in category-counts/by-category
        # unter dem Namen sichtbar, detach war fuer Array-Eintraege faktisch WIRKUNGSLOS.
        # Traegt ein Eintrag mehrere Kategorien, wird die erste verbleibende zur primaeren.
        groups: dict[str, tuple[dict, list]] = {}
        for p in pts:
            pl = p.payload or {}
            new_cats = [c for c in cats_from_payload(pl) if category_key(c) != category_key(name)]
            cat_primary = canonical_category(pl.get("category"))
            if category_key(cat_primary) == category_key(name):
                cat_primary = new_cats[0] if new_cats else ""
            parents = category_parents(new_cats)
            payload = {"category": cat_primary, "categories": new_cats,
                       "parent": category_parent(cat_primary) or (parents[0] if parents else ""),
                       "parents": parents}
            key = json.dumps(payload, sort_keys=True, ensure_ascii=False)
            groups.setdefault(key, (payload, []))[1].append(p.id)
        for payload, ids in groups.values():
            qc.set_payload(collection_name=COLLECTION, payload=payload, points=ids, wait=True)
    checkpoint("detach_category", "Kategorie-Etikett entfernt (auch aus dem categories-Array) — Eintraege bleiben 1:1 erhalten",
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
    ]), limit=1)   # nur 1 Chunk: full_text/Metadaten sind 1:1 in jedem Chunk -> OOM-Schutz bei grossen Docs (Frank 2026-06-26)
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
    source = (pl.get("source") or "manual").strip().lower() or "manual"
    now = iso_now()

    _delete_doc(req.doc_id)  # alte Chunks (mit altem Kategorie-Vektor) raus
    chunks = chunk_text(full_text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    vecs = embed_many([embed_input(title, new_cats, ch) for ch in chunks], "RETRIEVAL_DOCUMENT")  # gebuendelt statt seriell
    for i, (ch, vec) in enumerate(zip(chunks, vecs)):
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": req.user_id, "title": title,
            "category": new_cat, "categories": new_cats, "parent": new_parent, "parents": new_parents, "chunk_index": i, "chunk_count": len(chunks),
            "chunk_text": ch, "full_text": full_text, "created_at": created_at, "updated_at": now, "source": source,
        }))
    _upsert_batched(points, wait=True)
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
    ]), limit=1)   # nur 1 Chunk: full_text/Metadaten sind 1:1 in jedem Chunk -> OOM-Schutz bei grossen Docs (Frank 2026-06-26)
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
    source = (pl.get("source") or "manual").strip().lower() or "manual"
    now = iso_now()

    _delete_doc(req.doc_id)  # alte Chunks (mit alten Kategorie-Vektoren) raus
    chunks = chunk_text(full_text)
    _guard_embed_budget(len(chunks))
    t0 = time.time()
    points = []
    vecs = embed_many([embed_input(title, cats, ch) for ch in chunks], "RETRIEVAL_DOCUMENT")  # gebuendelt statt seriell
    for i, (ch, vec) in enumerate(zip(chunks, vecs)):
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": req.user_id, "title": title,
            "category": cats[0], "categories": cats,
            "parent": (parents[0] if parents else ""), "parents": parents,
            "chunk_index": i, "chunk_count": len(chunks), "chunk_text": ch,
            "full_text": full_text, "created_at": created_at, "updated_at": now, "source": source,
        }))
    _upsert_batched(points, wait=True)
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
    # Nur doc_id laden (Performance 2026-07-02): die Volltexte wurden hier nur fuer die Zaehlung geladen.
    pts = _scroll(flt, with_payload=["doc_id"])
    docs = {p.payload.get("doc_id") for p in pts}
    if pts:
        qc.delete(collection_name=COLLECTION, points_selector=flt, wait=True)
        _bm25_invalidate()   # Hybrid-Index frisch (purge laeuft an _delete_doc vorbei)
    # Level-2 (1.21.1): auch die ENTITAETEN des Test-Nutzers loeschen — der Eval legt jetzt
    # Test-Entitaeten an; ohne diese Zeile blieben sie nach jedem Lauf im Register liegen.
    ents = 0
    try:
        ent_pts = _scroll_col(ENTITY_COLLECTION, flt, with_payload=False)
        ents = len(ent_pts)
        if ent_pts:
            qc.delete(collection_name=ENTITY_COLLECTION, points_selector=flt, wait=True)
    except Exception:  # noqa: BLE001 — Entity-Aufraeumung darf den purge nie kippen
        _log(logging.WARNING, "purge: Entity-Aufraeumung fehlgeschlagen", exc_info=True)
    checkpoint("purge", "Test-Nutzer HART geloescht (kein Papierkorb, inkl. Entitaeten)", ok=True,
               user_id=uid, points=len(pts), entries=len(docs), entities=ents)
    _log(logging.INFO, "Eval-Test-Nutzer gepurged", user_id=uid, points=len(pts), entries=len(docs), entities=ents)
    return {"ok": True, "user_id": uid, "points": len(pts), "entries": len(docs), "entities": ents}


@app.get("/by-date", dependencies=[Depends(require_auth)])
def by_date(date: str, user_id: str = "frank") -> dict:
    """Alle Eintraege, die an einem bestimmten Tag gespeichert wurden (Datum YYYY-MM-DD,
    Praefix-Filter auf created_at). Auf Dokument-Ebene dedupliziert."""
    _require_store()
    # OOM-Schutz (gleiche Fehlerklasse wie 1.13.x/1.14, qdrant §8): NUR Chunk 0 je Dokument laden
    # (full_text ist 1:1 in JEDEM Chunk) und — wenn der Client DatetimeRange kann — den Tag schon
    # SERVERSEITIG eingrenzen, statt ALLE Chunks ALLER Dokumente mit Volltext in den RAM zu ziehen.
    # Der Python-Praefix-Check unten bleibt als Fallback/zweite Sicherung (identisches Ergebnis).
    must = [FieldCondition(key="user_id", match=MatchValue(value=user_id)),
            FieldCondition(key="chunk_index", match=MatchValue(value=0))]
    if HAS_DATETIME_RANGE and re.fullmatch(r"\d{4}-\d{2}-\d{2}", (date or "").strip()):
        d = date.strip()
        must.append(FieldCondition(key="created_at", range=DatetimeRange(gte=f"{d}T00:00:00Z", lte=f"{d}T23:59:59Z")))
    points = _scroll(Filter(must=must))
    seen: dict[str, dict] = {}
    for p in points:
        created = p.payload.get("created_at", "") or ""   # or "": Key koennte explizit None sein
        if not created.startswith(date):
            continue
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "category": canonical_category(p.payload.get("category")) or None,
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
        _c = canonical_category(req.category)   # Multi-Category: neues Array 'categories' ODER altes 'category'
        must.append(_category_match_filter(_c))
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

    effective_limit = req.limit
    search_point_limit = 0
    if effective_limit <= 0:
        if py_date_filter:
            # Fallback-Pfad (alte qdrant-client-Version ohne DatetimeRange): Datums-Nachfilter muss in
            # Python laufen -> hier weiterhin alle Punkte scannen (identisches Ergebnis wie bisher).
            count_pts = _scroll(Filter(must=must), with_payload=["doc_id", "created_at"])
            count_pts = [p for p in count_pts
                         if not ((gte and (p.payload.get("created_at") or "")[:10] < gte[:10])
                                 or (lte and (p.payload.get("created_at") or "")[:10] > lte[:10]))]
            effective_limit = max(1, len({p.payload.get("doc_id") for p in count_pts if p.payload.get("doc_id")}))
            search_point_limit = max(1, len(count_pts))
        else:
            # qdrant.md §10: distinkte doc_ids NUR ueber chunk_index=0 zaehlen (identisches Ergebnis,
            # ~5x weniger Scan); die Gesamt-PUNKT-Zahl liefert qc.count serverseitig ohne Payload-Transfer.
            first_chunks = _scroll(Filter(must=must + [FieldCondition(key="chunk_index", match=MatchValue(value=0))]),
                                   with_payload=["doc_id"])
            effective_limit = max(1, len({p.payload.get("doc_id") for p in first_chunks if p.payload.get("doc_id")}))
            search_point_limit = max(1, int(qc.count(collection_name=COLLECTION, count_filter=Filter(must=must), exact=True).count))

    # Bei Python-Datums-Nachfilter mehr Kandidaten holen, damit datumspassende Treffer nicht durchrutschen.
    overfetch = search_point_limit or (effective_limit * (SEARCH_DATE_OVERFETCH_FACTOR if py_date_filter else SEARCH_OVERFETCH_FACTOR))
    raw = qc.query_points(
        collection_name=COLLECTION, query=qvec,
        query_filter=Filter(must=must),
        limit=overfetch,
        with_payload=["doc_id", "title", "category", "categories", "created_at", "updated_at", "chunk_text"],
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
                         "created_at": h.payload.get("created_at"),
                         "updated_at": h.payload.get("updated_at"),
                         "text": "",
                         "dense_score": float(h.score), "matched_by": ["dense"], "bm25_rank": None}
    dense_ranked = sorted(best.values(), key=lambda x: x["score"], reverse=True)

    # --- HYBRID (Level-2 Nr. 34): BM25-Stichwortsuche + RRF-Fusion mit der Vektorsuche ---------
    # Die Vektorsuche findet BEDEUTUNG, BM25 findet EXAKTE WOERTER (Geraetenamen, Praeparate,
    # Projektnamen, Zahlen). RRF fusioniert beide Ranglisten rein rang-basiert. Der BM25-Pfad
    # respektiert DENSELBEN Scope (Kategorie/Parent/Datum) via Praedikat ueber die Chunk-Metadaten.
    bm25_used = False
    if HYBRID_ENABLED:
        try:
            _cat = canonical_category(req.category) if req.category and req.category.strip() else ""
            _par = canonical_category(req.parent) if req.parent and req.parent.strip() else ""
            _cat_keys = {category_key(a) for a in category_aliases(_cat)} if _cat else set()
            _par_keys = {category_key(a) for a in category_aliases(_par)} if _par else set()

            def _allow(c: dict) -> bool:
                c_cat_keys = {category_key(x) for x in (c.get("categories") or [])}
                if c.get("category"):
                    c_cat_keys.add(category_key(c.get("category")))
                if _cat and not (_cat_keys & c_cat_keys):
                    return False
                c_par_keys = {category_key(x) for x in (c.get("parents") or [])}
                if c.get("parent"):
                    c_par_keys.add(category_key(c.get("parent")))
                if not _cat and _par and not (_par_keys & c_par_keys):
                    return False
                if gte or lte:
                    created = (c.get("created_at") or "")[:10]
                    if (gte and created < gte[:10]) or (lte and created > lte[:10]):
                        return False
                return True

            bm_hits = _bm25_index(req.user_id).search(req.query, limit=max(effective_limit * BM25_CANDIDATE_FACTOR, BM25_MIN_CANDIDATES), allow=_allow)
            bm25_used = True
            bm_ranked_ids = [c.get("doc_id") for c, _s in bm_hits]
            fused = _rrf_fuse([[d["doc_id"] for d in dense_ranked], bm_ranked_ids])
            # BM25-only-Treffer brauchen ein Item-Geruest; full_text wird NUR fuer die finalen
            # Top-N nachgeladen (limit=1 je doc — full_text 1:1 in jedem Chunk, OOM-sicher).
            bm_meta = {c.get("doc_id"): (c, rank) for rank, (c, _s) in enumerate(bm_hits, start=1)}
            order = sorted(fused.items(), key=lambda kv: kv[1], reverse=True)[:effective_limit]
            items = []
            for did, _f in order:
                it = best.get(did)
                if it is None:
                    c, rank = bm_meta[did]
                    pts = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=did))]), limit=1)
                    full = pts[0].payload.get("full_text", "") if pts else ""
                    it = {"doc_id": did, "title": c.get("title") or None,
                          "category": c.get("category") or None,
                          "score": 0.0, "dense_score": None, "match": c.get("text") or "",
                          "created_at": c.get("created_at") or None, "updated_at": c.get("updated_at"),
                          "text": full, "matched_by": ["bm25"], "bm25_rank": rank}
                elif did in bm_meta:
                    it["matched_by"] = ["dense", "bm25"]
                    it["bm25_rank"] = bm_meta[did][1]
                items.append(it)
        except Exception:  # noqa: BLE001 — Hybrid darf die Suche NIE brechen: sauber auf dense zurueckfallen
            _log(logging.ERROR, "Hybrid/BM25-Pfad fehlgeschlagen — Fallback auf reine Vektorsuche", exc_info=True)
            items = dense_ranked[:effective_limit]
    else:
        items = dense_ranked[:effective_limit]

    for it in items:
        if not it.get("text") and it.get("doc_id"):
            pts = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=it["doc_id"]))]), limit=1)
            if pts:
                it["text"] = pts[0].payload.get("full_text", "")

    applied = {"category": req.category or None, "parent": req.parent or None, "date": req.date or None,
               "date_from": req.date_from or None, "date_to": req.date_to or None}
    has_filter = any(applied.values())
    checkpoint("search", "Hybrid: Payload-Scope -> Vektorsuche + BM25 -> RRF-Fusion",
               ok=isinstance(items, list), query_len=len(req.query), hits=len(items),
               filters=applied if has_filter else None, hybrid=bm25_used,
               requested_limit=req.limit, effective_limit=effective_limit, search_point_limit=search_point_limit or overfetch,
               dense_factor=SEARCH_OVERFETCH_FACTOR, bm25_factor=BM25_CANDIDATE_FACTOR,
               native_date=bool((gte or lte) and not py_date_filter), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "count": len(items), "items": items, "filters": applied if has_filter else None,
            "retrieval": "hybrid" if bm25_used else "dense"}


@app.get("/list", dependencies=[Depends(require_auth)])
def list_docs(user_id: str = "frank", limit: int = 500) -> dict:
    """Listet die gespeicherten Eintraege (Titel/Kategorie) — ohne die Volltexte (kompakt)."""
    _require_store()
    # OOM-SCHUTZ (Frank-Bug 2026-06-26): NUR Metadaten laden, KEIN full_text. /list wird von
    # /api/overview ALLE 20s gepollt; full_text haengt 1:1 in JEDEM Chunk -> with_payload=True zog
    # bei 60-70 grossen Almanachen ALLE Volltexte gleichzeitig in den RAM (>2 GB) -> Container-OOM
    # -> Neustart-Loop. 'chars' (Volltext-Laenge) war im Frontend ungenutzt -> entfaellt (0).
    # fastapi §8/§9 (nicht alles gleichzeitig in den RAM laden).
    # Performance (2026-07-02): NUR Chunk 0 je Dokument — Metadaten sind in jedem Chunk identisch,
    # Ergebnis (doc_id-dedupliziert) exakt gleich, Scan ~5x kleiner.
    points = _scroll(Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id)),
                                  FieldCondition(key="chunk_index", match=MatchValue(value=0))]),
                     with_payload=["doc_id", "title", "category", "created_at", "updated_at", "text_len"])
    seen: dict[str, dict] = {}
    for p in points:
        did = p.payload.get("doc_id")
        if did not in seen:
            seen[did] = {"doc_id": did, "title": p.payload.get("title") or None,
                         "category": p.payload.get("category") or None,
                         "chars": int(p.payload.get("text_len") or 0),   # Dokumentgroesse aus kleinem Feld (KEIN full_text-Laden -> kein OOM)
                         "created_at": p.payload.get("created_at"),
                         "updated_at": p.payload.get("updated_at")}
    items = _sort_recent(seen.values())[:limit]   # Neueste zuerst (nach Aktualitaet)
    # Ladefenster-Schutz (Frank-Bug 2026-06-26): nach Container-Neustart laedt Qdrant die Collection ->
    # _scroll liefert dann UNVOLLSTAENDIG (zeigte 175 statt 264) OHNE Exception. 'ready' (Status
    # green/yellow) sagt Clients/MCP, ob die Liste vollstaendig ist (qdrant.md §2c). Default True:
    # wenn _scroll oben durchlief, sind Daten abfragbar -> ein Status-Abfrage-Hickup loest keinen
    # Fehlalarm aus. Rein additiv -> bestehende Felder unveraendert.
    ready = True
    try:
        ready = getattr(qc.get_collection(COLLECTION).status, "value", "").lower() in ("green", "yellow")
    except Exception:  # noqa: BLE001 — Status-Abfrage darf /list nie crashen
        pass
    return {"ok": True, "ready": ready, "count": len(seen), "items": items}


@app.delete("/by-title", dependencies=[Depends(require_auth)])
def forget(title: str, user_id: str = "frank") -> dict:
    """Loescht den Eintrag mit diesem Titel komplett."""
    _require_store()
    # Toleranter Resolver: gleicher '[Kategorie]'/'— N Zeichen'/'92.'-Schutz wie bei /by-title, damit
    # 'forget' nicht still ins Leere laeuft, wenn das LLM den Titel aus der Liste verschmutzt hat.
    resolved = resolve_title(user_id, title)
    if not resolved:
        return {"ok": True, "deleted": False, "title": title}
    real_title, doc_id = resolved
    _delete_doc(doc_id)
    checkpoint("forget", "Eintrag per Titel geloescht", ok=True, title=real_title, angefragt=title)
    return {"ok": True, "deleted": True, "title": real_title}


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
        # Multi-Category mit in den Papierkorb (Tiefen-Debugging 2026-07-02): vorher ging beim
        # Restore jede sekundaere Kategorie verloren (trash_restore liest via cats_from_payload).
        "categories": cats_from_payload(pl),
        "source": (pl.get("source") or "manual").strip().lower() or "manual",
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
    source = (pl.get("source") or "manual").strip().lower() or "manual"
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
    vecs = embed_many([embed_input(title, cats, ch) for ch in chunks], "RETRIEVAL_DOCUMENT")  # gebuendelt statt seriell
    for i, (ch, vec) in enumerate(zip(chunks, vecs)):
        points.append(PointStruct(id=point_id(target_doc_id, i), vector=vec, payload={
            "doc_id": target_doc_id, "user_id": req.user_id, "title": title, "category": category, "categories": cats, "parent": parent, "parents": parents,
            "chunk_index": i, "chunk_count": len(chunks), "chunk_text": ch,
            "full_text": req.text, "created_at": created_at, "updated_at": now, "source": source,
        }))
    _upsert_batched(points)

    replaced_ok = bool(points) and points[0].payload["full_text"] == req.text
    # Sonde: nach einer Titel-Migration darf die alte doc_id NICHT mehr existieren (kein Geist-Duplikat)
    if title_changed and target_doc_id != req.doc_id:
        leftover = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=req.doc_id))]),
                           limit=1, with_payload=False)   # nur Existenz-Probe, kein Payload noetig
        probe(not leftover, "Alte doc_id nach Titel-Migration noch vorhanden", old_doc_id=req.doc_id, new_doc_id=target_doc_id)
    checkpoint("update_entry", "Alten Vektor loeschen + neuen Text 1:1 speichern (Titel-Aenderung -> neue doc_id)",
               ok=replaced_ok, doc_id=target_doc_id, old_doc_id=req.doc_id if title_changed else None,
               title=title or None, title_changed=title_changed, chunks=len(chunks),
               chars=len(req.text), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "doc_id": target_doc_id, "title": title or None, "category": category or None,
            "updated_at": now, "title_changed": title_changed, "chunks": len(chunks),
            "chars": len(req.text), "replaced": True}


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
    source = (entry.get("source") or "manual").strip().lower() or "manual"
    if not text.strip():
        raise HTTPException(status_code=400, detail="Eintrag hat keinen Text")

    parents = category_parents(cats)
    parent = parents[0] if parents else ""
    _delete_doc(req.doc_id)  # eventuelle Reste gleicher doc_id raus, dann frisch
    chunks = chunk_text(text)
    _guard_embed_budget(len(chunks))
    now = iso_now()
    points = []
    vecs = embed_many([embed_input(title, cats, ch) for ch in chunks], "RETRIEVAL_DOCUMENT")  # gebuendelt statt seriell
    for i, (ch, vec) in enumerate(zip(chunks, vecs)):
        points.append(PointStruct(id=point_id(req.doc_id, i), vector=vec, payload={
            "doc_id": req.doc_id, "user_id": entry.get("user_id") or req.user_id,
            "title": title, "category": category, "categories": cats, "parent": parent, "parents": parents, "chunk_index": i, "chunk_count": len(chunks),
            "chunk_text": ch, "full_text": text, "created_at": created_at, "updated_at": now, "source": source,
        }))
    _upsert_batched(points)

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
    # OOM-Schutz (Tiefen-Debugging 2026-07-02, gleiche Klasse wie 1.13.x/qdrant §8): erst NUR die
    # Punkt-IDs einsammeln (kein full_text), dann je 100er-Batch die vollen Payloads per retrieve
    # nachladen — statt ALLE Volltexte ALLER Punkte gleichzeitig in den RAM zu ziehen.
    id_pts = _scroll(flt, limit=req.limit, with_payload=["doc_id"])
    if not id_pts:
        return {"ok": True, "reembedded": 0, "total": 0, "note": "keine Punkte"}
    all_ids = [p.id for p in id_pts]
    _guard_embed_budget(len(all_ids))
    t0 = time.time()
    done = 0
    for start in range(0, len(all_ids), 100):
        batch = qc.retrieve(collection_name=COLLECTION, ids=all_ids[start:start + 100],
                            with_payload=True, with_vectors=False)
        prepared = []   # (point_id, payload, embed_input) je Punkt — Vektoren danach GEBUENDELT
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
            prepared.append((p.id, pl, embed_input(title, cats, ch)))
        vecs = embed_many([inp for _, _, inp in prepared], "RETRIEVAL_DOCUMENT")  # gebuendelt statt seriell je Punkt
        points = [PointStruct(id=pid, vector=vec, payload=pl)
                  for (pid, pl, _), vec in zip(prepared, vecs)]   # Payload inkl. categories/parents
        _upsert_batched(points, wait=True)
        done += len(points)
    checkpoint("reembed_all", "Bestand mit aktuellem Titel+Kategorie-Schema neu eingebettet (Vektor neu, Payload 1:1)",
               ok=(done == len(all_ids)), reembedded=done, total=len(all_ids), ms=int((time.time() - t0) * 1000))
    return {"ok": True, "reembedded": done, "total": len(all_ids)}


# ---------------------------------------------------------------------------
# Entity-Register (Level-2 Nr. 36): leichtes Entity-Linking, Hub-and-Spoke
# ---------------------------------------------------------------------------
# REINE Speicher-Endpunkte (kein LLM — brain-api bleibt "stummer Speicher"; die Extraktion
# uebernimmt der Agent). Eine Entitaet = ein Punkt: Name + Typ + Aliases + verknuepfte doc_ids.
# Der Vektor (Embedding von Name/Typ/Aliases) dient NUR dem fuzzy Wiederfinden ("Whoop-Band"
# findet "Whoop"). "Alles ueber X"-Fragen werden damit VOLLSTAENDIG (alle verknuepften Eintraege)
# statt "die 5 aehnlichsten".
def _entity_key(name: str) -> str:
    return re.sub(r"\s+", " ", (name or "").strip()).casefold()


def _entity_point_id(user_id: str, name: str) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"sb-entity/{user_id}/{_entity_key(name)}"))


def _entity_embed_text(name: str, etype: str, aliases: list[str]) -> str:
    parts = [f"Entität: {name}"]
    if etype:
        parts.append(f"Typ: {etype}")
    if aliases:
        parts.append("Auch bekannt als: " + ", ".join(aliases))
    body = " | ".join(parts)
    # gemini-embedding-2-Dokumentpraefix 'title: {Name} | text: {…}' (task_type entfaellt, §J24).
    return f"title: {name} | text: {body}"


def _entity_get(user_id: str, name: str):
    """Exakter Lookup per name_key ODER Alias. Gibt den Qdrant-Punkt oder None."""
    key = _entity_key(name)
    if not key:
        return None
    pts = _scroll_col(ENTITY_COLLECTION, Filter(must=[
        FieldCondition(key="user_id", match=MatchValue(value=user_id)),
        FieldCondition(key="name_key", match=MatchValue(value=key))]), limit=1)
    if pts:
        return pts[0]
    # Alias-Suche: Aliases liegen normalisiert in 'alias_keys' (Keyword-Array waere moeglich,
    # aber der Bestand ist klein -> Metadaten-Scan reicht und bleibt schema-einfach)
    for p in _scroll_col(ENTITY_COLLECTION, Filter(must=[
            FieldCondition(key="user_id", match=MatchValue(value=user_id))]),
            with_payload=["name", "name_key", "alias_keys"]):
        if key in (p.payload.get("alias_keys") or []):
            return _scroll_col(ENTITY_COLLECTION, Filter(must=[
                FieldCondition(key="user_id", match=MatchValue(value=user_id)),
                FieldCondition(key="name_key", match=MatchValue(value=p.payload.get("name_key")))]), limit=1)[0]
    return None


def _scroll_col(collection: str, flt: "Filter | None", limit: "int | None" = None,
                with_payload=True, page: int = 1000) -> list:
    """Wie _scroll, aber fuer eine waehlbare Collection (Entity-Register)."""
    out: list = []
    off = None
    while True:
        want = page if limit is None else min(page, limit - len(out))
        if want <= 0:
            break
        pts, off = qc.scroll(collection_name=collection, scroll_filter=flt, limit=want,
                             offset=off, with_payload=with_payload)
        out.extend(pts)
        if off is None or not pts:
            break
    return out


class EntityUpsertReq(BaseModel):
    name: str = Field(..., min_length=1, max_length=120, description="Anzeigename der Entitaet (z.B. 'Whoop', 'EntropieReductor')")
    type: str = Field(default="", max_length=40, description="Typ: Person/Ort/Geraet/Projekt/Praeparat/Organisation/Sonstiges")
    aliases: list[str] = Field(default_factory=list, max_length=20, description="Alternative Namen/Schreibweisen")
    doc_ids: list[str] = Field(default_factory=list, max_length=500, description="Zu verknuepfende Eintrags-IDs (werden mit dem Bestand VEREINIGT)")
    user_id: str = Field(default="frank")


class EntityUnlinkReq(BaseModel):
    name: str = Field(..., min_length=1, max_length=120)
    doc_id: str = Field(..., min_length=1)
    user_id: str = Field(default="frank")


@app.post("/entities/upsert", dependencies=[Depends(require_auth)])
def entities_upsert(req: EntityUpsertReq) -> dict:
    """Legt eine Entitaet an ODER erweitert sie (merge): doc_ids/aliases werden VEREINIGT, nie
    ueberschrieben. Re-Embed nur, wenn sich das Profil (Name/Typ/Aliases) aendert — reine
    doc_id-Verknuepfungen sind ein billiges set_payload."""
    _require_store()
    name = req.name.strip()
    existing = _entity_get(req.user_id, name)
    now = iso_now()
    new_aliases = _dedup([a.strip() for a in req.aliases if a and a.strip()])
    if existing is not None:
        pl = existing.payload or {}
        aliases = _dedup((pl.get("aliases") or []) + new_aliases)
        doc_ids = _dedup([d for d in (pl.get("doc_ids") or []) if d] + [d.strip() for d in req.doc_ids if d and d.strip()])
        etype = (req.type or "").strip() or (pl.get("type") or "")
        profile_changed = (set(aliases) != set(pl.get("aliases") or [])) or (etype != (pl.get("type") or ""))
        payload = {"user_id": req.user_id, "name": pl.get("name") or name, "name_key": pl.get("name_key") or _entity_key(name),
                   "type": etype, "aliases": aliases, "alias_keys": [_entity_key(a) for a in aliases],
                   "doc_ids": doc_ids, "doc_count": len(doc_ids),
                   "created_at": pl.get("created_at") or now, "updated_at": now}
        if profile_changed:
            _guard_embed_budget(1)
            vec = embed(_entity_embed_text(payload["name"], etype, aliases), "RETRIEVAL_DOCUMENT")
            qc.upsert(collection_name=ENTITY_COLLECTION,
                      points=[PointStruct(id=existing.id, vector=vec, payload=payload)], wait=True)
        else:
            qc.set_payload(collection_name=ENTITY_COLLECTION, payload=payload, points=[existing.id], wait=True)
        checkpoint("entity_upsert", "Entitaet erweitert (merge doc_ids/aliases)", ok=True,
                   name=payload["name"], docs=len(doc_ids), reembedded=profile_changed)
        return {"ok": True, "created": False, "name": payload["name"], "type": etype,
                "aliases": aliases, "doc_ids": doc_ids}
    doc_ids = _dedup([d.strip() for d in req.doc_ids if d and d.strip()])
    etype = (req.type or "").strip()
    _guard_embed_budget(1)
    vec = embed(_entity_embed_text(name, etype, new_aliases), "RETRIEVAL_DOCUMENT")
    payload = {"user_id": req.user_id, "name": name, "name_key": _entity_key(name), "type": etype,
               "aliases": new_aliases, "alias_keys": [_entity_key(a) for a in new_aliases],
               "doc_ids": doc_ids, "doc_count": len(doc_ids), "created_at": now, "updated_at": now}
    qc.upsert(collection_name=ENTITY_COLLECTION,
              points=[PointStruct(id=_entity_point_id(req.user_id, name), vector=vec, payload=payload)], wait=True)
    checkpoint("entity_upsert", "Neue Entitaet angelegt", ok=True, name=name, type=etype or None, docs=len(doc_ids))
    return {"ok": True, "created": True, "name": name, "type": etype, "aliases": new_aliases, "doc_ids": doc_ids}


@app.get("/entities/list", dependencies=[Depends(require_auth)])
def entities_list(user_id: str = "frank", with_docs: bool = False) -> dict:
    """Alle Entitaeten (Metadaten, ohne Vektoren) — fuers Dashboard/den Agenten.
    with_docs=1 (Nachtschicht-Bibliothekar): liefert zusaetzlich die verknuepften doc_ids je
    Entitaet, damit der Nachzuegler-Lauf Eintraege OHNE Verknuepfung finden kann (rein additiv)."""
    _require_store()
    pts = _scroll_col(ENTITY_COLLECTION,
                      Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]),
                      with_payload=["name", "type", "aliases", "doc_ids", "doc_count", "created_at", "updated_at"])
    items = [{"name": p.payload.get("name"), "type": p.payload.get("type") or None,
              "aliases": p.payload.get("aliases") or [], "doc_count": int(p.payload.get("doc_count") or 0),
              "created_at": p.payload.get("created_at"), "updated_at": p.payload.get("updated_at"),
              **({"doc_ids": p.payload.get("doc_ids") or []} if with_docs else {})}
             for p in pts]
    items.sort(key=lambda x: (-(x["doc_count"]), (x["name"] or "").lower()))
    return {"ok": True, "count": len(items), "items": items}


@app.get("/entities/find", dependencies=[Depends(require_auth)])
def entities_find(name: str, user_id: str = "frank", semantic: bool = True) -> dict:
    """Entitaet finden: exakt (Name/Alias, case-insensitiv), sonst optional per Vektorsuche (fuzzy).
    Fuzzy gibt max. 3 Kandidaten mit Score — der AGENT entscheidet, ob ein Kandidat passt."""
    _require_store()
    hit = _entity_get(user_id, name)
    if hit is not None:
        pl = hit.payload or {}
        return {"ok": True, "found": True, "exact": True,
                "entity": {"name": pl.get("name"), "type": pl.get("type") or None,
                           "aliases": pl.get("aliases") or [], "doc_ids": pl.get("doc_ids") or []}}
    if not semantic:
        return {"ok": True, "found": False, "exact": False, "candidates": []}
    _guard_embed_budget(1)
    qvec = embed(_entity_embed_text(name.strip(), "", []), "RETRIEVAL_QUERY")
    raw = qc.query_points(collection_name=ENTITY_COLLECTION, query=qvec,
                          query_filter=Filter(must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]),
                          limit=3, with_payload=True).points
    cands = [{"name": h.payload.get("name"), "type": h.payload.get("type") or None,
              "score": float(h.score), "aliases": h.payload.get("aliases") or [],
              "doc_ids": h.payload.get("doc_ids") or []} for h in raw]
    return {"ok": True, "found": False, "exact": False, "candidates": cands}


@app.get("/entities/docs", dependencies=[Depends(require_auth)])
def entities_docs(name: str, user_id: str = "frank", limit: int = 10) -> dict:
    """Die mit einer Entitaet verknuepften EINTRAEGE (je Volltext 1:1, neueste zuerst, gedeckelt).
    Self-Healing: doc_ids, deren Eintrag nicht mehr existiert (geloescht), werden lazy aus der
    Entitaet entfernt — das Register heilt sich beim Lesen selbst."""
    _require_store()
    hit = _entity_get(user_id, name)
    if hit is None:
        return {"ok": True, "found": False, "name": name, "items": []}
    pl = hit.payload or {}
    doc_ids = [d for d in (pl.get("doc_ids") or []) if d]
    items, alive = [], []
    for did in doc_ids:
        pts = _scroll(Filter(must=[FieldCondition(key="doc_id", match=MatchValue(value=did))]), limit=1)
        if not pts:
            continue   # toter Verweis -> faellt aus 'alive' raus (lazy cleanup unten)
        alive.append(did)
        p = pts[0].payload
        items.append({"doc_id": did, "title": p.get("title") or None,
                      "category": p.get("category") or None, "categories": cats_from_payload(p),
                      "created_at": p.get("created_at"), "updated_at": p.get("updated_at"),
                      "text": p.get("full_text", "")})
    if len(alive) != len(doc_ids):   # Self-Healing: tote doc_ids austragen
        qc.set_payload(collection_name=ENTITY_COLLECTION,
                       payload={"doc_ids": alive, "doc_count": len(alive), "updated_at": iso_now()},
                       points=[hit.id], wait=False)
        _log(logging.INFO, "Entity-Register: tote doc_ids entfernt", name=pl.get("name"),
             entfernt=len(doc_ids) - len(alive))
    items = _sort_recent(items)
    if limit > 0:
        items = items[:limit]
    checkpoint("entity_docs", "Alles-ueber-X: verknuepfte Eintraege einer Entitaet (vollstaendig statt Top-5)",
               ok=True, name=pl.get("name"), docs=len(items))
    return {"ok": True, "found": True, "name": pl.get("name"), "type": pl.get("type") or None,
            "aliases": pl.get("aliases") or [], "count": len(items), "items": items}


@app.post("/entities/unlink", dependencies=[Depends(require_auth)])
def entities_unlink(req: EntityUnlinkReq) -> dict:
    """Traegt EINE doc_id aus einer Entitaet aus (der Eintrag selbst bleibt unberuehrt)."""
    _require_store()
    hit = _entity_get(req.user_id, req.name)
    if hit is None:
        return {"ok": True, "found": False, "name": req.name}
    pl = hit.payload or {}
    doc_ids = [d for d in (pl.get("doc_ids") or []) if d and d != req.doc_id]
    qc.set_payload(collection_name=ENTITY_COLLECTION,
                   payload={"doc_ids": doc_ids, "doc_count": len(doc_ids), "updated_at": iso_now()},
                   points=[hit.id], wait=True)
    return {"ok": True, "found": True, "name": pl.get("name"), "doc_ids": doc_ids}


@app.delete("/entities", dependencies=[Depends(require_auth)])
def entities_delete(name: str, user_id: str = "frank") -> dict:
    """Loescht eine Entitaet aus dem Register (NUR den Registereintrag — nie einen Gehirn-Eintrag)."""
    _require_store()
    hit = _entity_get(user_id, name)
    if hit is None:
        return {"ok": True, "deleted": False, "name": name}
    qc.delete(collection_name=ENTITY_COLLECTION, points_selector=[hit.id], wait=True)
    checkpoint("entity_delete", "Entitaet aus dem Register geloescht (Eintraege bleiben 1:1)", ok=True, name=name)
    return {"ok": True, "deleted": True, "name": (hit.payload or {}).get("name") or name}


@app.get("/")
def root() -> dict:
    return {"service": "Second Brain — brain-api (1:1-Speicher)", "version": VERSION,
            "endpoints": ["/health", "/store", "/by-title", "/by-category", "/by-parent", "/by-date", "/search", "/list", "/entry", "/entry/categories", "/reembed-all", "/forget",
                          "/entities/upsert", "/entities/find", "/entities/list", "/entities/docs", "/entities/unlink"]}
