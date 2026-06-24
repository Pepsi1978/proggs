# FastAPI & async-Python-Server — Best Practices (Stand 2026-06-24)

> **Zweite Seite der Medaille zu `bugs/server/fastapi.md`.** Der Almanach sagt *was schiefgeht und wie
> man es loest*; hier steht, *wie man es von vornherein richtig baut, damit der Bug nie entsteht.*
> Beide Kurzchecks VOR der Arbeit lesen (erst Almanach, dann Best Practices). Funktionserhaltend
> (Direktive #3). Bezug: `brain-api` / `agent` / `dashboard` im second-brain-server-Stack.
>
> **Anker:** fastapi=0.138.0 · uvicorn=0.49.0 · starlette=1.0.0 · pydantic=v2 · python=3.13.13.
> Quellen: offizielle fastapi.tiangolo.com / starlette.dev / docs.python.org + GitHub-Issues
> (gh-verifiziert), Firecrawl+MiniMax-Recherche 2026-06-24 (+ OpenRouter-`:online`-Eskalation).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Regel | Detail |
|---|-------|--------|
| 1 | **`def` fuer sync-Arbeit, `async def` nur fuer echtes await** | Handler mit synchronen Libs als `def` schreiben (Threadpool); im `async def` blockierende Calls per `run_in_threadpool`/`asyncio.to_thread` auslagern oder async-Clients nutzen. |
| 2 | **Lock klein halten** | `async with _lock` nur um die State-Mutation; blockierende/lange Arbeit ausserhalb. |
| 3 | **1 uvicorn-Worker/Container** | Horizontal ueber den Orchestrator skalieren; geteilter State (falls Multi-Worker) in Redis, nicht im Modul-Dict. |
| 4 | **Explizite Timeouts + Client-Reuse** | `httpx.AsyncClient` einmal im lifespan, `aclose()` beim Shutdown; nie `timeout=None`; Tenacity fuer idempotente Retries. |
| 5 | **Pydantic v2 von Anfang an** | `ConfigDict`, `model_dump`, `field_validator`, `model_validate(from_attributes=True)`; Felder mit `min_length`/`max_length`. |
| 6 | **CORS nur wenn noetig, dann eng** | Kein `*`+Credentials; Origins explizit; `CORSMiddleware` zuletzt. Intern (VPN/gleiche Origin) → gar kein CORS. |
| 7 | **lifespan + saubere Tasks** | `lifespan` statt `on_event`; create_task-Referenz halten + canceln; `timeout_graceful_shutdown`. |
| 8 | **Strukturiertes Logging, propagate bewusst** | JSON-Logs, `logger.exception()` im Catch-all, `uvicorn.error.propagate=False`, `--no-access-log` bei eigener Middleware; nie `str(exc)` an den Client. |
| 9 | **Eingabe-Limits** | `max_length` an Text-/JSON-Feldern; grosse Bodies via `request.stream()`/`UploadFile`; Proxy-Limit (`client_max_body_size`). |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug
| Best-Practice (hier) | Bug-Abschnitt (`bugs/server/fastapi.md`) |
|----------------------|------------------------------------------|
| §1 def/async waehlen | §1 Event-Loop / sync-in-async |
| §2 Skalierung & State | §2 Worker / In-Memory-State |
| §3 HTTP-Client & Timeouts | §3 Timeouts / ausgehende Calls |
| §4 Modelle & Validierung | §4 Pydantic v2 |
| §5 CORS | §5 CORS |
| §6 Lebenszyklus & Hintergrundarbeit | §6 lifespan / Tasks / Shutdown |
| §7 Fehlerbehandlung & Logging | §7 Exceptions / Logging |
| §8 Eingabe-Limits & Streaming | §8 Request-Body / Speicher |

---

## §1 — `def` vs. `async def` richtig waehlen
- **Faustregel:** Nutzt der Handler nur **synchrone** Libs (sync httpx, qdrant-client, Gemini-SDK, Datei-I/O)
  → als **`def`** deklarieren. Starlette schiebt ihn automatisch in den Threadpool, der Event-Loop bleibt frei.
  (So macht es `brain-api` — vorbildlich.)
- Nutzt er **echte** awaitables (async-DB, `httpx.AsyncClient`) → `async def` + `await`.
- Mischfall (async-Handler MUSS einen sync-Call machen): `await run_in_threadpool(fn, *args)` (aus
  `starlette.concurrency`) oder `await asyncio.to_thread(fn, *args)`.
- Threadpool fasst 40 Threads (anyio) — bei vielen langen sync-Calls echtes async bevorzugen.

## §2 — Skalierung & geteilter State
- **1 uvicorn-Prozess pro Container**, Replikation ueber Docker/K8s (offizielle FastAPI-Empfehlung).
- In-Memory-`dict`/`Counter`/Sessions sind prozess-lokal → bei `--workers >1` brechen Cap/Sessions.
  Geteilten State in Redis/DB auslagern; In-Memory nur fuer prozess-lokale Caches.
- Den 1-Worker-Stand dokumentieren, damit ihn niemand versehentlich hochdreht.

## §3 — HTTP-Client & Timeouts
- Einen `httpx.AsyncClient` im `lifespan` anlegen, in `app.state` ablegen, beim Shutdown `aclose()` →
  Connection-Pool-Reuse + Keep-Alive.
- IMMER explizite Timeouts (`httpx.Timeout(read, connect=, write=, pool=)`); nie `None`.
- Idempotente Calls mit Tenacity retryen (Exponential Backoff, begrenzte Versuche).
- `--timeout-keep-alive` moderat lassen.

## §4 — Modelle & Validierung (Pydantic v2)
- `model_config = ConfigDict(...)`, `model_dump()`/`model_dump_json()`, `@field_validator`,
  `model_validate(obj, from_attributes=True)`, `RootModel` statt `__root__`.
- Felder klar typisieren + begrenzen (`Field(..., min_length=1, max_length=…, ge=…, le=…)`) — die
  Validierung ist die erste Verteidigungslinie (auch gegen §8).
- `response_model` setzen, um Ausgaben bewusst zu formen (kein Over-Exposure).
- Keine v1/v2-Mischung. `bump-pydantic` + CI-Tests bei Migration.

## §5 — CORS richtig konfigurieren
- Intern (WireGuard/gleiche Origin) → **kein** CORS (kleinere Angriffsflaeche). So aktuell im Stack.
- Falls Cross-Origin noetig: Origins explizit auflisten, **nie** `*` mit `allow_credentials=True`;
  `allow_methods`/`allow_headers` ebenfalls explizit; `CORSMiddleware` als **letztes** `add_middleware`.

## §6 — Lebenszyklus & Hintergrundarbeit
- `@asynccontextmanager async def lifespan(app)` statt `@app.on_event` (gegenseitig ausschliessend).
- Langlebige Tasks: Referenz in einem Modul-`set` halten + `add_done_callback(set.discard)`; beim Shutdown
  canceln + `await`en. (`agent._flush_loop` haelt die Referenz im lifespan + cancelt — Vorbild.)
- Exceptions in BackgroundTasks/create_task SELBST abfangen + `logger.exception` (kein automatisches
  Error-Surfacing). `timeout_graceful_shutdown` setzen; im lifespan `BaseException` sauber behandeln.

## §7 — Fehlerbehandlung & Logging
- Strukturiertes JSON-Logging (Observability-First): fester Log-Pfad beim Start ausgeben, Rotation,
  stdout-Spiegelung. (brain-api/agent: vorbildlich mit `JsonFormatter` + `RotatingFileHandler`.)
- Catch-all-`exception_handler` MUSS `logger.exception(...)`/`exc_info=True` loggen; an den Client nur eine
  **generische** Meldung (keine rohen `str(exc)`/Pfade).
- `logging.getLogger("uvicorn.error").propagate = False` bzw. `LOGGING_CONFIG` mit `propagate:false` →
  keine doppelten Tracebacks. `--no-access-log` bei eigener Request-Logging-Middleware.
- Logik-/Intent-Sonden (`probe`/`checkpoint`) wie im Stack beibehalten — sie machen stille Logikfehler sichtbar.

## §8 — Eingabe-Limits & Streaming
- Text-/JSON-Felder mit `max_length` begrenzen (lehnt uebergrosse Bodies mit 422 ab, BEVOR teure Arbeit
  laeuft) — z.B. `brain-api StoreReq.text`.
- Starlette hat **kein** Default-Limit fuer rohe `request.body()`/`.json()` → grosse Bodies = OOM-Risiko.
  Multipart hat Limits (`max_files`/`max_fields`=1000, `max_part_size`=1 MB).
- Robustestes Limit auf Proxy-Ebene (`client_max_body_size` in nginx / Traefik-Buffering).
- Grosse Daten streamen (`StreamingResponse`, `request.stream()`, `UploadFile`-Chunks) statt in den RAM.
