# Bekannte Bugs & Fallen: FastAPI & asynchrone Python-Server (uvicorn)

> **PFLICHT-LESEN vor Arbeit an einem FastAPI/async-Python-Backend** (Endpoints, Middleware,
> lifespan, BackgroundTasks, ausgehende API-Calls, Request-Body-Verarbeitung) — z.B. die
> `brain-api`, der `agent` und das `dashboard` im second-brain-server-Stack. Loesungen sind
> **funktionserhaltend** (Direktive #3 — nie "Feature/Handler weglassen"). **Zweite Seite der
> Medaille:** `best-practices/server/fastapi.md`.
>
> **Stand:** recherchiert am **2026-06-24** (Firecrawl + MiniMax M3, quellentreu; offizielle
> fastapi.tiangolo.com / starlette.dev / docs.python.org + GitHub-Issues — gh-OPEN/CLOSED-verifiziert;
> Engine-B-Eskalation OpenRouter `:online` fuer Threadpool/Body-Limit/BackgroundTasks).
> **Ergaenzt 2026-07-02** (Tiefen-Debugging second-brain-server): §1-Live-Befunde gefixt (agent 0.48.1);
> NEU §9 Modul-Level-Init ohne Re-Init → Container dauerhaft degraded (Kurzcheck #13).
> **Anker:** fastapi=0.138.0 · uvicorn=0.49.0 · starlette=1.0.0 · pydantic=v2 · python=3.13.13 ·
> httpx>=0.27 · qdrant-client>=1.18 · google-genai>=2.9  <!-- maschinenlesbar fuer check-version-anchor.py -->
> (projekt-gepinnt in `*/requirements.txt`; FastAPI 0.138 hat **keinen** Pydantic-v1-Support mehr).
> Abgrenzung: reine Windows-Scripting-/Encoding-Fallen → `claude-tooling/python-windows.md`;
> MCP-Server-Bau → `claude-tooling/mcp-server.md`; serverseitige KI-Agenten-Logik (Loop/Tools/State)
> → `server/ai-agent-frameworks.md`; Vektor-DB → `server/qdrant.md`. DIESE Datei = die FastAPI/async-
> **Web-Schicht** (Event-Loop, Worker, Pydantic, CORS, lifespan, Logging, Body-Handling).

---

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
| 13 | Modul-Level-Init verbindet zu einem abhaengigen Dienst (DB/Qdrant) und faengt den Fehler nur | Boot-Race (compose `depends_on` wartet nur auf START): schlaegt die Init einmal fehl, bleibt der Container DAUERHAFT degraded/503 — /health gibt weiter HTTP 200 → kein Docker-Restart. Init in Funktion kapseln + beim ersten Request throttled NACHHOLEN (Self-Healing). | §9 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt (hier) | Best-Practice (`best-practices/server/fastapi.md`) |
|----------------------|----------------------------------------------------|
| §1 Event-Loop / sync-in-async | §1 `def` vs `async def` richtig waehlen |
| §2 Worker / In-Memory-State | §2 Skalierung & geteilter State |
| §3 Timeouts / ausgehende Calls | §3 HTTP-Client & Timeouts |
| §4 Pydantic v2 | §4 Modelle & Validierung |
| §5 CORS | §5 CORS richtig konfigurieren |
| §6 lifespan / Tasks / Shutdown | §6 Lebenszyklus & Hintergrundarbeit |
| §7 Exceptions / Logging | §7 Fehlerbehandlung & Logging |
| §8 Request-Body / Speicher | §8 Eingabe-Limits & Streaming |

---

## 1. ⭐ Blockierende synchrone Aufrufe in `async def` blockieren den GANZEN Event-Loop
**Symptom:** Unter Last serialisieren Requests; `/health` antwortet erst nach Sekunden; waehrend ein
LLM-/DB-Call laeuft, "haengt" der ganze Server (auch andere Endpoints, der lifespan-Loop und der
Healthcheck). Bei nur 2 gleichzeitigen Clients muss einer warten.
**Ursache:** Ein `async def`-Handler laeuft auf dem **einzigen** Event-Loop-Thread. Jeder darin
**synchron** aufgerufene blockierende Code haelt diesen Thread an: `requests`/**sync** `httpx`,
`qdrant-client` (Sync-API), das Google-Gemini-SDK (`embed_content`/`generate_content`),
`time.sleep`, blockierendes Datei-I/O. Im Gegensatz dazu lagert FastAPI/Starlette einen **`def`**-Handler
automatisch in einen Threadpool aus (`anyio.to_thread.run_sync`) — der blockiert den Loop NICHT.
**Versionen:** strukturell (alle FastAPI/Starlette-Versionen, per Design). GitHub `fastapi/fastapi#5351`
(CLOSED/completed — bestaetigt das Verhalten: "async def routes run on the main (single) thread … the
server processes the requests sequentially"). Kein eingebauter Schalter "async def trotzdem im Threadpool":
`fastapi/fastapi#10768` ist weiterhin **OPEN**.
**Betrifft unseren Code (live geprueft 2026-06-24; NACHTRAG 2026-07-02: ALLE Befunde gefixt):**
- `dashboard/app.py`: `async def api_put_prompt` / `async def api_put_config` rufen `_aput(...)` =
  **synchrones** `httpx.put` → blockiert den Loop. (Die GET-Handler sind `def` → unkritisch.)
  → GEFIXT (asyncio.to_thread, dashboard 0.3.0+).
- `agent/app.py`: `async def chat` ruft `llm_decide` (**synchroner**, potenziell langsamer Gemini-Call!),
  `brain_search`/`brain_categories`/`brain_store` (synchrones `httpx`) → ein einziger Chat-Request
  blockiert waehrend des LLM-Calls den kompletten Agent-Prozess. `async def end_session` ruft
  `flush_session_to_logbook` (sync httpx + Datei-I/O).
  → GEFIXT: /chat via to_thread (agent 0.5.0); `/end` + der Lock-Sonderfall `_flush_loop` (unten)
  erst am **2026-07-02 (agent 0.48.1, Tiefen-Debugging)** — der Timeout-Flush lief bis dahin
  WEITER synchron IM `_lock` direkt im Event-Loop (haengendes brain-api = ganzer Agent eingefroren).
- `brain-api/app.py`: die Handler sind durchgehend **`def`** (kein `async def`) → korrekt, laufen im
  Threadpool. Genau richtig fuer die synchronen Gemini-/Qdrant-Aufrufe.
**FIX (funktionserhaltend, drei gleichwertige Wege):**
1. **Handler als `def` deklarieren** (kein `async def`) — Starlette schiebt ihn in den Threadpool.
   Einfachster Fix, wenn der Handler ohnehin nur synchrone Libs nutzt (so macht es `brain-api` richtig).
2. **Blockierenden Aufruf explizit auslagern** und den Handler `async` lassen:
   ```python
   from starlette.concurrency import run_in_threadpool   # = Wrapper um anyio.to_thread.run_sync
   decision = await run_in_threadpool(llm_decide, session, req.text, candidates, categories)
   # alternativ (Python >=3.9): decision = await asyncio.to_thread(llm_decide, ...)
   ```
3. **Native async-Clients** verwenden: `httpx.AsyncClient` + `await client.post(...)`, Qdrant
   `AsyncQdrantClient`. Dann ist KEIN Offload noetig.
**Sonderfalle — Lock + Blockieren (§Kurzcheck 2):** Im `agent` laeuft `_flush_loop` so:
`async with _lock: … flush_session_to_logbook(s)` — der **synchrone** Flush (httpx+Datei) liegt INNERHALB
des Locks und blockiert Loop UND Lock gleichzeitig. Regel: das `async with _lock` nur um die reine
State-Mutation (pop/append) legen, die blockierende Arbeit DANACH ausserhalb des Locks erledigen
(ggf. via `run_in_threadpool`).
**Threadpool-Limit:** Der Default-Threadpool fasst **40 Worker** (anyio). Viele gleichzeitige `def`-Handler
mit langen sync-Calls erschoepfen ihn → weitere Requests warten in der Queue. Echtes async vermeidet das.
**Quelle:** fastapi.tiangolo.com/async/ · starlette.dev/threadpool/ · github.com/fastapi/fastapi/issues/5351 ·
github.com/fastapi/fastapi/discussions/10768 · reddit.com/r/FastAPI „Default thread limit of 40".

## 2. ⭐ uvicorn/gunicorn-Worker vs. geteilter In-Memory-State
**Symptom:** Ein Rate-Limit/Counter zaehlt scheinbar zu langsam; In-Memory-Sessions sind „mal da, mal
weg"; ein im RAM gehaltener Cache ist inkonsistent — sobald mit mehr als einem Worker gefahren wird.
**Ursache:** `uvicorn --workers N` (bzw. gunicorn `-w N`) startet **N separate Prozesse** (multiprocessing).
Nach dem Fork teilen sie **keinen** Speicher: ein modul-globales `dict`/`Counter`/`set` ist in jedem Worker
**eine eigene Kopie**. Der Initialwert (zur Modulladezeit) ist ueberall gleich, aber jede spaetere Mutation
bleibt prozess-lokal („Future modifications will not be … they happen in separate processes").
**Versionen:** strukturell (uvicorn/gunicorn, alle Versionen).
**Betrifft unseren Code:** Alle drei Services starten `uvicorn app:app … --no-access-log` **ohne** `--workers`
→ aktuell **1 Worker/Prozess** → korrekt, der State stimmt. ABER die Fallen lauern beim Skalieren:
- `brain-api`: `_embed_calls` (Tages-Cap) waere bei N Workern faktisch **N×Cap**.
- `agent`: `_sessions` + `_lock` — bei N Workern landet jeder Request auf einem zufaelligen Worker →
  „keine aktive Sitzung"/doppelte Sitzungen; das `_flush_loop` liefe N-fach.
**FIX (funktionserhaltend):**
- **Default beibehalten: 1 uvicorn-Prozess pro Container**, horizontal ueber den Orchestrator (Docker/
  K8s) skalieren — exakt die offizielle FastAPI-Empfehlung (NICHT Worker-Manager IM Container).
- Wird echtes Multi-Worker gebraucht: geteilten State in einen **externen Store** (Redis o.ae.) ziehen;
  In-Memory nur fuer prozess-lokale Caches.
- Den 1-Worker-Stand bewusst dokumentieren, damit ihn niemand „aus Performance-Gruenden" hochdreht und
  dabei Cap/Sessions zerlegt.
**Quelle:** fastapi.tiangolo.com/deployment/server-workers/ · StackOverflow „uvicorn workers share memory".

## 3. Fehlende/falsche Timeouts & Connection-Pool bei ausgehenden Calls → haengende Requests
**Symptom:** Sporadisch sehr lange Antwortzeiten; ein langsamer Upstream (Gemini/Qdrant/brain-api) zieht
den ganzen Service mit; Server „haengt", obwohl der Client laengst abgebrochen hat.
**Ursache:** httpx **erzwingt** zwar einen Default-Timeout von **5s** — aber `timeout=None` (oder ein sehr
hoher Wert) deaktiviert ihn → unbegrenztes Blockieren, erschoepfter Connection-/Threadpool. Ein **neuer
Client pro Request** verschenkt Connection-Pooling (kein Keep-Alive-Reuse). Server-seitig schliesst uvicorn
Keep-Alive-Verbindungen nach `--timeout-keep-alive` (Default **5s**).
**Versionen:** httpx >=0.27 (Default-Timeout-Verhalten stabil); uvicorn 0.49.
**Betrifft unseren Code:** `dashboard` und `agent` setzen explizite Timeouts (gut: 8-120s), erzeugen aber
pro Request einen neuen `httpx`-Aufruf (kein wiederverwendeter Client). `brain-api` nutzt
`QdrantClient(..., timeout=30.0)` (gut). `agent.brain_store` hat `timeout=120.0` (sehr lang — bei einem
haengenden brain-api blockiert der Agent 2 Min).
**FIX (funktionserhaltend):**
- Einen `httpx.AsyncClient` **einmal im lifespan** anlegen + wiederverwenden, beim Shutdown `aclose()`:
  ```python
  @asynccontextmanager
  async def lifespan(app):
      app.state.client = httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=5.0))
      yield
      await app.state.client.aclose()
  ```
- Nie `timeout=None`. Granular: `httpx.Timeout(read, connect=, write=, pool=)`.
- Retries fuer idempotente Calls via **Tenacity** (`@retry(retry=retry_if_exception_type(ConnectTimeout),
  stop=stop_after_attempt(3))`).
- `--timeout-keep-alive` nicht extrem hochsetzen (Zombie-Connections).
**Quelle:** www.python-httpx.org/advanced/timeouts/ · fastapi.tiangolo.com (httpx-Client via lifespan) ·
github.com/fastapi/fastapi/discussions/6806 (Server haengt trotz Client-Timeout).

## 4. ⭐ Pydantic-v2-Fallen (FastAPI ≥0.128 erzwingt v2)
**Symptom:** `AttributeError: 'ErrorWrapper' object has no attribute 'get'` in einem Exception-Handler;
`from_orm`/`.dict()`/`.json()` existieren nicht mehr; innere `class Config` wird ignoriert; 422 bei
scheinbar korrektem Body; `GenericModel`/`__root__` brechen.
**Ursache:** Pydantic v2 ist ein Breaking-Change-Sprung; FastAPI hat v1 schrittweise entfernt.
**Versionen (gh-verifiziert):** FastAPI **0.128.0 hat `pydantic.v1` entfernt** (`fastapi/fastapi`-Release
0.128.0, PR #14609); 0.126.0 entfernte schon das v1-Hauptpaket. Auf **0.138.0 ist Pydantic v2 Pflicht**.
Pydantic v1 wird ab Python 3.14 nicht mehr unterstuetzt.
**FIX (Migrations-Mapping, funktionserhaltend):**
| v1 | v2 |
|----|----|
| innere `class Config:` | `model_config = ConfigDict(...)` |
| `.dict()` / `.json()` | `model_dump()` / `model_dump_json()` (kompakter, ohne Spaces) |
| `@validator` | `@field_validator(...)` |
| `from_orm(obj)` | `model_validate(obj, from_attributes=True)` |
| `json_encoders` (Config) | `@field_serializer` / `@model_serializer` |
| `pydantic.generics.GenericModel` | `class M(BaseModel, Generic[T])` |
| `__root__` | `RootModel` (kein `arbitrary_types_allowed`) |
| `exc.errors()[i].get("msg")` | v2 liefert `ErrorWrapper`/anderes Format → Zugriff anpassen |
- Tool `bump-pydantic` + Tests in CI. **Nicht** v1- und v2-Modelle mischen (auch nicht in Generics/Nested).
- 422 = Schema-Mismatch (oft Body als Query deklariert) → `BaseModel`-Body-Parameter verwenden.
**Betrifft unseren Code:** Modelle (`StoreReq`, `SearchReq`, `ChatReq`, …) sind bereits sauberes v2
(`Field(..., min_length=…)`). Der globale `@app.exception_handler(Exception)` nutzt `type(exc).__name__`/
`str(exc)` — NICHT das alte `exc.errors().get(...)` → nicht betroffen. Beim Erweitern auf
Validierungs-Handler das v2-Fehlerformat beachten.
**Quelle:** docs.pydantic.dev/latest/migration/ · fastapi.tiangolo.com/release-notes (0.126/0.128) ·
gh `fastapi/fastapi` Release 0.128.0 (PR #14609, verifiziert).

## 5. CORS falsch konfiguriert (Wildcard + Credentials, Middleware-Reihenfolge)
**Symptom:** Browser meldet „Response to preflight request doesn't pass access control check: No
'Access-Control-Allow-Origin' header"; `curl` funktioniert, der Browser nicht.
**Ursache:** Die CORS-Spec **verbietet** `Access-Control-Allow-Origin: *` zusammen mit Credentials
(Cookies/`Authorization`-Bearer). FastAPIs `CORSMiddleware` ist fail-safe: `allow_origins=["*"]` +
`allow_credentials=True` → Preflight scheitert. Gleiches gilt fuer `allow_methods=["*"]`/`allow_headers=["*"]`
in Kombination mit Credentials. Zudem: wird eine andere Middleware **nach** `CORSMiddleware` registriert,
umschliesst sie den CORS-Handler → CORS-Header kommen nicht beim Client an. `curl` sendet keinen
`Origin`-Header → zeigt das Problem nicht.
**Versionen:** strukturell (Starlette CORSMiddleware, alle Versionen).
**Betrifft unseren Code:** **Kein** Service nutzt CORS — alle laufen intern hinter WireGuard, das Dashboard
liefert sein Frontend von derselben Origin (kein Cross-Origin). Aktuell **korrekt** (CORS waere unnoetige
Angriffsflaeche). Erst relevant, falls je eine externe Web-Origin auf eine der APIs zugreifen soll.
**FIX (wenn CORS gebraucht wird):** Origins **explizit** auflisten (kein `*` mit Credentials);
`CORSMiddleware` als **letztes** `app.add_middleware(...)` registrieren; `allow_origin_regex` nur vorsichtig
(Regex-Fehler = haeufige CORS-Luecke); Safari unterstuetzt `*` in manchen Feldern nicht.
**Quelle:** fastapi.tiangolo.com/tutorial/cors/ · github.com/fastapi/fastapi/discussions/7319 ·
developer.mozilla.org „credentialed_requests_and_wildcards".

## 6. lifespan vs. on_event · BackgroundTasks/Tasks · Graceful Shutdown
**Symptom (a):** `on_event`-Handler werden ignoriert; **(b)** ein fire-and-forget-Task verschwindet
zufaellig mitten im Lauf; **(c)** ein Fehler in einem Background-Task taucht nirgends auf; **(d)** der
Server laesst sich nur mit `kill -9` beenden / schneidet laufende Requests ab.
**Ursachen & FIX (funktionserhaltend):**
- **(a) `@app.on_event("startup"/"shutdown")` ist deprecated** und schliesst `lifespan` **gegenseitig
  aus** („It's all `lifespan` or all events, not both" — setzt man `lifespan`, werden `on_event` ignoriert).
  → den `lifespan`-Contextmanager nutzen (Code vor `yield` = Start, danach = Shutdown).
  *Unser `agent` macht das bereits korrekt (`@asynccontextmanager async def lifespan`).*
- **(b) `asyncio.create_task(...)` ohne starke Referenz** → der Event-Loop haelt nur **weak refs**, der GC
  sammelt den Task „at any time, even before it's done" ein. → Referenz halten:
  ```python
  _bg = set()
  t = asyncio.create_task(coro()); _bg.add(t); t.add_done_callback(_bg.discard)
  ```
  *Unser `agent` haelt den `_flush_loop`-Task in einer lokalen `task`-Variable im lifespan und cancelt ihn
  beim Shutdown → korrekt (kein GC-Risiko). Wuerde die Referenz fehlen, waere es ein Bug.*
- **(c) Exceptions in FastAPI `BackgroundTasks` / create_task werden still verschluckt** — die Response ist
  schon raus, der `exception_handler` greift NICHT (`fastapi/fastapi#2505`, CLOSED). → Fehler IM Task
  abfangen (`try/except` + `logger.exception`) ODER `task.add_done_callback`, der `task.exception()` loggt.
- **(d) Graceful Shutdown:** Haengt ein Request in einer Endlosschleife, wird der Shutdown nie erreicht
  (uvicorn wartet auf das Request-Ende → Deadlock, nur `kill -9` hilft). → `uvicorn`-Option
  `timeout_graceful_shutdown=N` setzen; im lifespan **`BaseException`** fangen (auch `CancelledError`/
  `SystemExit`) und laufende Tasks sauber canceln + `await`en.
**Versionen:** strukturell; `on_event`-Deprecation seit FastAPI 0.93/Lifespan-Einfuehrung.
**Quelle:** fastapi.tiangolo.com/advanced/events/ · docs.python.org/3/library/asyncio-task.html
(„Save a reference … to avoid a task disappearing mid-execution") · github.com/fastapi/fastapi/issues/2505 ·
github.com/fastapi/fastapi/discussions/11321.

## 7. ⭐ Unbehandelte Exceptions (500 ohne Log) & doppelte/verschluckte Logs unter uvicorn
**Symptom:** Ein 500er erscheint ohne Stacktrace im Log — ODER jeder Traceback steht **doppelt** im Log;
interne Fehlerdetails landen in der HTTP-Antwort beim Client.
**Ursachen & FIX (funktionserhaltend):**
- **Eigener `@app.exception_handler(Exception)` kann Uvicorns Traceback unterdruecken:** Starlettes
  eingebaute `ServerErrorMiddleware` laesst normalerweise Uvicorn den Traceback loggen. Faengt ein
  Catch-all-Handler den Fehler ab, MUSS er selbst `logger.exception(...)` (ERROR + vollstaendiger Trace)
  rufen — sonst „stirbt" der Fehler still. *Unser `brain-api`/`agent`/`dashboard` loggen im Handler mit
  `exc_info=True`/`traceback.format_exc()` → korrekt.*
- **`str(exc)` roh an den Client** (wie aktuell `{"detail": str(exc)}`) kann interne Details/Pfade leaken.
  → generische Client-Meldung zurueckgeben, Details nur ins Log.
- **Doppelte Logs:** `uvicorn.error` hat `propagate=True` → Records „bubblen" zum Root-Logger; haengt man
  einen eigenen `StreamHandler` an einen benannten Logger UND laeuft uvicorns Logger parallel, erscheint
  jede Zeile (und jeder Traceback) **doppelt**. → `logging.getLogger("uvicorn.error").propagate = False`
  bzw. eine `LOGGING_CONFIG` mit `"propagate": false` fuer `uvicorn`/`uvicorn.access`; bei eigener
  Request-Logging-Middleware uvicorns Access-Log abschalten (`--no-access-log` — *machen alle drei Services
  bereits*). Reihenfolge: Logging VOR den Clients initialisieren.
**Versionen:** strukturell (uvicorn/Python-logging).
**Betrifft unseren Code:** `brain-api`/`agent` haengen einen `StreamHandler` an den benannten Logger
(`brain-api`/`sb-agent`) — solange dieser Logger **nicht** zum Root propagiert UND uvicorn separat loggt,
kann es doppelte Zeilen geben. `--no-access-log` ist gesetzt (gut). Beim Bauen einer `LOGGING_CONFIG`
`propagate` bewusst setzen.
**Quelle:** github (uvicorn duplicate logs / `uvicorn.error` propagate) · fastapi.tiangolo.com
(`logger.exception()` im Handler) · starlette `ServerErrorMiddleware`.

## 8. ⭐ Request-Body ohne Groessen-Limit → Speicher-Spike / OOM
**Symptom:** Ein einzelner grosser POST (riesiges JSON / grosser `text`) treibt den RAM hoch oder
crasht den Worker (OOM); viele parallele grosse Uploads ebenso.
**Ursache:** `await request.body()` / `.json()` / `.form()` lesen den **kompletten** Body in den Speicher.
Starlette liefert **kein** Default-Limit fuer rohe Bodies — ein 30-GB-JSON kann den Prozess sprengen
(`Kludex/starlette#890`). `request.stream()` puffert dagegen NICHT (Chunks).
**Fix-Status (gh-verifiziert, ehrlich):** `Kludex/starlette#890` ist **CLOSED/COMPLETED** — geliefert wurde
aber NUR der **Multipart-Schutz**: der `MultiPartParser` hat Limits (`max_files=1000`, `max_fields=1000`,
`max_part_size=1 MB`, Spool auf Disk >1 MB → `MultiPartException`/413). Fuer **rohe** `request.body()`/
`request.json()` gibt es in Starlette 1.0.0 **weiterhin kein** Default-Limit (lokal in der installierten
Quelle verifiziert: keine Body-Size-Middleware in `starlette/middleware/`). `fastapi/fastapi#362`
(„Strategies for limiting upload file size") ist CLOSED — Empfehlung bleibt: Limit ausserhalb (nginx) ODER
manueller Check.
**Betrifft unseren Code:** `brain-api /store` nimmt `StoreReq.text` mit `min_length=1`, aber **ohne
`max_length`** → ein riesiger `text` = Speicher-Spike + Chunk-Explosion + viele Embedding-Calls (Kosten).
Der `_guard_embed_budget`-Cap bremst die Kosten, aber NICHT den Speicher beim Body-Lesen.
**FIX (funktionserhaltend):**
- `max_length` am Pydantic-Feld setzen (`text: str = Field(..., min_length=1, max_length=200_000)`) →
  uebergrosse Bodies werden mit 422 abgelehnt, BEVOR gechunkt/embedded wird.
- Optional Content-Length-Check (411/413) oder eine kleine `MaxBodySize`-Dependency.
- Auf Reverse-Proxy-Ebene `client_max_body_size` (nginx) / Buffering-Limit (Traefik) — der robusteste Schutz.
- Grosse Antworten/Uploads streamen (`StreamingResponse` / `request.stream()` / `UploadFile`-Chunks) statt
  in den RAM zu materialisieren.
**Quelle:** starlette.dev/requests/ · github.com/Kludex/starlette/issues/890 (CLOSED, nur Multipart) ·
github.com/fastapi/fastapi/discussions/8167 · lokale Starlette-1.0.0-Quelle (`formparsers.py` Limits).

---

## 🔧 Fix-Status (was ist schon gefixt, was bleibt — gh-verifiziert 2026-06-24)

| Frueheres Problem | Status | Bezug |
|-------------------|--------|-------|
| Pydantic v1 in FastAPI | **Entfernt** ab FastAPI **0.128.0** (`pydantic.v1` raus, PR #14609); auf 0.138 ist v2 Pflicht | §4 |
| Multipart-DoS (unbegrenzte Felder/Dateien/Part-Groesse) | **Gefixt**: `max_files`/`max_fields`=1000, `max_part_size`=1 MB im `MultiPartParser` (verifiziert in Starlette 1.0.0) | §8 |
| `fastapi#5351` „async ops auf separate Threads" | CLOSED/completed — aendert NICHT das Design: `async def` blockt weiterhin den Loop (Verhalten bestaetigt) | §1 |
| `fastapi#2505` „Exceptions mit Handler in Background-Task" | CLOSED/completed — Verhalten dokumentiert: Background-Exceptions gehen NICHT durch `exception_handler` | §6 |
| `fastapi#10768` „async def trotzdem im Threadpool (Schalter)" | **OPEN** — es gibt KEINEN eingebauten Toggle; Offload bleibt manuell (`run_in_threadpool`) | §1 |
| `starlette#890` „max request size limit" | CLOSED/completed — aber NUR Multipart-Limits; **kein** Default-Limit fuer rohe `request.body()`/`.json()` | §8 |
| `fastapi#11321` AsyncSession nach Response (GC) | CLOSED — kein DB-/Session-Handle nach gesendeter Response weiterverwenden | §6 |

**Ehrlichkeit zur Methodik:** Die meisten Eintraege sind **per-Design-Fallen** (Event-Loop-Modell,
Worker-Isolation, fehlendes Body-Limit) — keine „in Version X behobenen" Bugs, sondern dauerhaftes
Verhalten, das man richtig handhaben muss. Issue-Stati per `gh issue view` direkt geprueft; wo eine Quelle
nur einen Vorschlag/eine Diskussion war, ist das oben als solche markiert.

---

## 9. Modul-Level-Init gegen abhaengigen Dienst ohne Re-Init → Container dauerhaft degraded
**Symptom:** Nach einem (Server-)Reboot antwortet der Dienst nur noch mit 503 „nicht initialisiert",
OBWOHL die Abhaengigkeit (Qdrant/DB) laengst wieder laeuft; erst ein manueller Container-Neustart heilt.
**Ursache:** Der Verbindungsaufbau (Client + Collection/Schema sicherstellen) laeuft beim **Modul-Import**
in einem `try/except`, das den Fehler nur festhaelt (`init_error`) — es gibt KEINEN zweiten Versuch.
Compose `depends_on` (Kurzform) wartet nur auf den START der Abhaengigkeit, nicht auf „bereit"
(docker.md §3) → beim Boot-Race verliert der abhaengige Dienst. Tueckisch: der `/health`-Endpoint gibt
trotz „degraded" **HTTP 200** zurueck → der Docker-Healthcheck bleibt gruen, `restart: unless-stopped`
greift nie (der Prozess lebt ja).
**Versionen:** strukturell (jedes FastAPI/uvicorn-Setup mit Import-Zeit-Init).
**Betrifft unseren Code:** `brain-api/app.py` (Qdrant-Init beim Import) — GEFIXT 2026-07-02 (1.19.0,
Tiefen-Debugging): Init in `_init_store()` gekapselt; `_require_store()` holt sie bei jedem Request
**throttled** (max. 1 Versuch/5s, thread-sicher) nach, bis sie gelingt.
**FIX (funktionserhaltend):** Init-Funktion + Lazy-Retry im Request-Pfad (throttled, unter Lock) ODER
`lifespan`-Retry-Schleife; zusaetzlich compose `depends_on`-Langform mit `condition: service_healthy`,
wo die Abhaengigkeit einen Healthcheck hat. Nie darauf verlassen, dass „restart: unless-stopped" einen
logisch-degradeden (aber lebenden) Prozess heilt.
**Quelle:** Tiefen-Debugging second-brain-server 2026-07-02 (statischer Fund, Boot-Race-Analyse).

## ✅ Pflicht-Checkliste vor dem Commit eines FastAPI/async-Endpoints

- [ ] **Kein** blockierender sync-Call (httpx/requests/qdrant-sync/Gemini-SDK/Datei-I/O) direkt in einem
      `async def`-Handler — Handler `def` machen ODER `await run_in_threadpool(...)`/`asyncio.to_thread(...)`
      ODER native async-Clients. (§1)
- [ ] Kein blockierender I/O **innerhalb** eines gehaltenen `asyncio.Lock`. (§1/§2)
- [ ] Modul-globaler State nur mit **1 Worker** sicher (so laeuft der Stack) — beim Skalieren externer Store. (§2)
- [ ] Jeder ausgehende Call hat einen **expliziten Timeout** (nie `None`); `AsyncClient` wiederverwenden. (§3)
- [ ] Modelle sind **Pydantic v2** (`ConfigDict`, `model_dump`, `field_validator`); kein altes `exc.errors().get`. (§4)
- [ ] CORS (falls genutzt) ohne `*`+Credentials; `CORSMiddleware` zuletzt. (§5)
- [ ] `lifespan` statt `on_event`; create_task-Referenz halten; Background-Exceptions abfangen/loggen. (§6)
- [ ] Catch-all-`exception_handler` **loggt** (`exc_info=True`) und gibt **keine** rohen `str(exc)` an den Client;
      `uvicorn.error`-Propagation geklaert (keine Doppel-Logs). (§7)
- [ ] Endpoints mit freiem Text/JSON haben ein **`max_length`/Body-Limit** (Pydantic-Feld bzw. Proxy). (§8)
