# FastAPI & async-Python-Server Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
