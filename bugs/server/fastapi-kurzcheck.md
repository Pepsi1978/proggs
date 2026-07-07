# FastAPI & asynchrone Python-Server (uvicorn) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektuere
> (`Read` mit `limit=80`). Bei JEDEM Fehler im Bereich den VOLLTEXT lesen (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ `async def`-Handler ruft **synchron** httpx/`requests`/qdrant-client-sync/Gemini-SDK/Datei-I/O | Blockiert den **ganzen** Event-Loop (alle Requests + lifespan + /health). Handler als `def` schreiben (Starlette → Threadpool) ODER `await run_in_threadpool(fn,…)` / `await asyncio.to_thread(fn,…)` ODER `httpx.AsyncClient` + `await`. | §1 |
| 2 | ⭐ Blockierender I/O, WAEHREND ein `asyncio.Lock` gehalten wird | Serialisiert ALLES + blockiert den Loop doppelt. Lock NUR um die State-Mutation, blockierende Arbeit AUSSERHALB des `async with _lock`. | §1 |
| 3 | ⭐ Modul-globaler State (dict/Counter/Sessions) + `--workers >1` | Wird NICHT zwischen Prozessen geteilt → Cap/Session pro Worker. **1 Worker pro Container** (offizielle Empfehlung) ODER externer Store (Redis). | §2 |
| 4 | sync `def`-Handler, viele gleichzeitige Requests haengen | Threadpool-Default = **40 Threads** (anyio). Lange sync-Calls erschoepfen ihn → Queue. Echtes async nutzen oder Pool-Limit anheben. | §1 |
| 5 | Ausgehender Call (httpx/qdrant/Gemini) ohne/mit `timeout=None` | httpx-Default ist 5s — `None` = **haengt unbegrenzt**. Immer explizit `timeout=…`; `AsyncClient` via lifespan + wiederverwenden (Pool-Reuse). | §3 |
| 6 | ⭐ Pydantic v2 (FastAPI ≥0.128 erzwingt v2) | `class Config`→`model_config=ConfigDict`; `dict()`→`model_dump()`; `from_orm`→`model_validate(from_attributes=True)`; `json_encoders` deprecated; alte `exc.errors()`-Handler brechen (`ErrorWrapper`). | §4 |
| 7 | `allow_origins=["*"]` + `allow_credentials=True` | Per CORS-Spec **verboten** → Preflight scheitert. Origins explizit listen; `allow_methods`/`allow_headers` ebenso. `CORSMiddleware` als **letztes** registrieren. | §5 |
| 8 | `@app.on_event("startup"/"shutdown")` | **Deprecated** + schliesst `lifespan` gegenseitig aus. `lifespan`-Contextmanager nutzen (vor `yield`=Start, danach=Shutdown). | §6 |
| 9 | ⭐ `asyncio.create_task(...)` fire-and-forget ohne Referenz | Task wird vom **GC eingesammelt** (Loop haelt nur weak refs) → verschwindet mitten im Lauf. Starke Referenz halten (`set` + `add_done_callback(set.discard)`). | §6 |
| 10 | FastAPI `BackgroundTasks` / create_task wirft Exception | Wird **still verschluckt** (Response ist schon raus, kein exception_handler). `add_done_callback` mit Logging ODER try/except IM Task. | §6 |
| 11 | ⭐ unbehandelte Exception → 500 ohne/mit doppeltem Log | Eigener `@app.exception_handler(Exception)` kann Uvicorns Traceback unterdruecken; `uvicorn.error` hat `propagate=True` → **doppelte** Tracebacks. `logger.exception()` + `propagate=False`. Nie `str(exc)` roh an den Client (Leak). | §7 |
| 12 | ⭐ Endpoint liest `await request.body()`/`.json()` ohne Groessen-Limit | Starlette hat **kein** Default-Limit fuer rohe Bodies → 30-GB-JSON sprengt den Worker (OOM). `max_length` am Pydantic-Feld + Content-Length-Check/`client_max_body_size` (nginx). Multipart hat eigene Limits. | §8 |
