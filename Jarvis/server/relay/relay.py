"""Jarvis-Relay: reicht MCP-Aufrufe von ChatGPT an die Jarvis-App auf dem Handy weiter.

Der MCP-Server selbst laeuft auf dem Handy. Dieser Dienst ist nur die oeffentliche Gegenstelle:
  ChatGPT  --POST /j/<Geheimnis>/mcp-->  Relay  --WebSocket-->  Jarvis-App
Die WebSocket baut das Handy von sich aus auf (/geraet/ws, Geraete-Token im Header) und nennt dabei
sein Geheimnis. Nur fuer dieses Geheimnis nimmt der Relay danach Aufrufe an; alles andere ist 404.

Zweite Aufgabe: Franks Tagebuch. Die Eintraege liegen als Markdown-Dateien in einem Google-Drive-Ordner.
Der Relay holt sie mit rclone (Zugang NUR LESEND, auf genau diesen Ordner als Wurzel gestellt) und gibt sie
dem Handy unter /geraet/tagebuch heraus – ebenfalls nur mit dem Geraete-Token.
"""
import asyncio
import hmac
import json
import logging
import os
import re
import time
import uuid
from datetime import date, timedelta
from pathlib import Path

from aiohttp import WSMsgType, web

TOKEN = os.environ["JARVIS_GERAET_TOKEN"]
ZUSTAND = Path(os.environ.get("JARVIS_ZUSTAND", "/data/zustand.json"))
WARTE_AUF_HANDY_S = 8      # kurze Funkloecher ueberbruecken, statt sofort einen Fehler zu melden
ZEITLIMIT_S = 75           # laengster Werkzeugaufruf (der freie Auftrag an Jarvis hat 45 s)
MAX_RUMPF = 1_000_000
# Antworten auf diese Methoden sind immer gleich; aus dem Zwischenspeicher kann ChatGPT sich auch
# dann verbinden, wenn das Handy gerade kurz weg ist.
MERKBAR = {"initialize", "tools/list"}

RCLONE_KONFIG = os.environ.get("JARVIS_RCLONE_KONFIG", "/data/rclone.conf")
TAGEBUCH_ORDNER = Path(os.environ.get("JARVIS_TAGEBUCH", "/data/tagebuch"))
TAGEBUCH_ABSTAND_S = 120   # hoechstens alle zwei Minuten bei Google nachfragen

log = logging.getLogger("jarvis-relay")
_KEINE = object()


class Relay:
    def __init__(self):
        self.geheimnis = ""          # zuletzt vom Handy gemeldetes Geheimnis
        self.gemerkt = {}            # Methode -> result-Objekt
        self.ws = None
        self.offen = {}              # Aufruf-id -> Future
        self.da = asyncio.Event()
        self._lade()

    def _lade(self):
        try:
            daten = json.loads(ZUSTAND.read_text("utf-8"))
            self.geheimnis = daten.get("geheimnis", "")
            self.gemerkt = daten.get("gemerkt", {})
        except Exception:
            pass

    def _sichere(self):
        try:
            ZUSTAND.parent.mkdir(parents=True, exist_ok=True)
            ZUSTAND.write_text(json.dumps({"geheimnis": self.geheimnis, "gemerkt": self.gemerkt}), "utf-8")
        except Exception as fehler:
            log.warning("Zustand nicht gesichert: %s", fehler)

    def passt(self, geheimnis: str) -> bool:
        return len(self.geheimnis) >= 32 and hmac.compare_digest(geheimnis.encode(), self.geheimnis.encode())

    # ---------------------------------------------------------------- Handy

    async def geraet(self, request: web.Request) -> web.StreamResponse:
        token = request.headers.get("X-Jarvis-Token", "")
        geheimnis = request.headers.get("X-Jarvis-Geheimnis", "")
        if not hmac.compare_digest(token.encode(), TOKEN.encode()) or len(geheimnis) < 32:
            return web.Response(status=404, text="Not found")
        ws = web.WebSocketResponse(heartbeat=20, max_msg_size=MAX_RUMPF * 2)
        await ws.prepare(request)
        alt = self.ws
        self.ws = ws
        if alt is not None and not alt.closed:
            await alt.close()
        if geheimnis != self.geheimnis:
            self.geheimnis, self.gemerkt = geheimnis, {}
            self._sichere()
        self.da.set()
        log.info("Handy verbunden")
        try:
            async for nachricht in ws:
                if nachricht.type != WSMsgType.TEXT:
                    continue
                try:
                    daten = json.loads(nachricht.data)
                except ValueError:
                    continue
                wartend = self.offen.pop(daten.get("id"), None)
                if wartend is not None and not wartend.done():
                    wartend.set_result(daten.get("antwort"))
        finally:
            if self.ws is ws:
                self.ws = None
                self.da.clear()
                log.info("Handy getrennt")
        return ws

    # ---------------------------------------------------------------- ChatGPT

    async def mcp(self, request: web.Request) -> web.Response:
        if not self.passt(request.match_info["geheimnis"]):
            return web.Response(status=404, text="Not found")
        if request.method != "POST":
            return web.Response(status=405, text="Method not allowed", headers={"Allow": "POST"})
        rumpf = await request.text()
        try:
            anfrage = json.loads(rumpf)
        except ValueError:
            return self._json({"jsonrpc": "2.0", "id": None, "error": {"code": -32700, "message": "Ungueltiges JSON"}}, 400)
        einzeln = anfrage if isinstance(anfrage, dict) else None
        methode = einzeln.get("method", "") if einzeln else ""

        if self.ws is None:
            try:
                await asyncio.wait_for(self.da.wait(), WARTE_AUF_HANDY_S)
            except asyncio.TimeoutError:
                return self._ohne_handy(einzeln, methode)

        antwort = await self._frage_handy(rumpf)
        if antwort is _KEINE:
            return self._ohne_handy(einzeln, methode)
        if antwort is None:                      # reine Benachrichtigung
            return web.Response(status=202)
        if methode in MERKBAR:
            try:
                ergebnis = json.loads(antwort).get("result")
                if ergebnis is not None and self.gemerkt.get(methode) != ergebnis:
                    self.gemerkt[methode] = ergebnis
                    self._sichere()
            except ValueError:
                pass
        return web.Response(text=antwort, content_type="application/json")

    async def _frage_handy(self, rumpf: str):
        ws = self.ws
        if ws is None or ws.closed:
            return _KEINE
        nummer = uuid.uuid4().hex
        wartend = asyncio.get_running_loop().create_future()
        self.offen[nummer] = wartend
        try:
            await ws.send_str(json.dumps({"id": nummer, "rumpf": rumpf}))
            return await asyncio.wait_for(wartend, ZEITLIMIT_S)
        except (asyncio.TimeoutError, ConnectionError, RuntimeError):
            return _KEINE
        finally:
            self.offen.pop(nummer, None)

    def _ohne_handy(self, einzeln, methode: str) -> web.Response:
        if einzeln is None:
            return web.Response(status=503, text="Handy nicht erreichbar")
        if einzeln.get("id") is None:
            return web.Response(status=202)
        if methode in self.gemerkt:
            return self._json({"jsonrpc": "2.0", "id": einzeln["id"], "result": self.gemerkt[methode]})
        if methode == "ping":
            return self._json({"jsonrpc": "2.0", "id": einzeln["id"], "result": {}})
        if methode == "tools/call":
            text = ("Jarvis ist gerade nicht erreichbar: Franks Handy hat keine Verbindung. "
                    "Nichts wurde geändert. Sage das Frank und bitte ihn, es gleich noch einmal zu versuchen.")
            return self._json({"jsonrpc": "2.0", "id": einzeln["id"],
                               "result": {"content": [{"type": "text", "text": text}], "isError": True}})
        return self._json({"jsonrpc": "2.0", "id": einzeln["id"],
                           "error": {"code": -32000, "message": "Handy nicht erreichbar"}})

    @staticmethod
    def _json(daten, status: int = 200) -> web.Response:
        return web.Response(text=json.dumps(daten, ensure_ascii=False), status=status, content_type="application/json")


class Tagebuch:
    """Holt die Tagebuch-Dateien aus Drive in einen lokalen Ordner und liest von dort."""

    def __init__(self):
        self.zuletzt = 0.0
        self.sperre = asyncio.Lock()
        self.fehler = ""

    async def hole(self) -> None:
        async with self.sperre:
            if time.time() - self.zuletzt < TAGEBUCH_ABSTAND_S:
                return
            if not Path(RCLONE_KONFIG).exists():
                self.fehler = "Der Drive-Zugang ist auf dem Server nicht eingerichtet."
                return
            TAGEBUCH_ORDNER.mkdir(parents=True, exist_ok=True)
            try:
                prozess = await asyncio.create_subprocess_exec(
                    "rclone", "sync", "gdrive-lesen:", str(TAGEBUCH_ORDNER), "--include", "*.md", "--max-depth", "1",
                    "--config", RCLONE_KONFIG, "--contimeout", "15s", "--timeout", "40s", "--retries", "2",
                    stdout=asyncio.subprocess.DEVNULL, stderr=asyncio.subprocess.PIPE)
                _, fehler = await asyncio.wait_for(prozess.communicate(), 90)
                if prozess.returncode == 0:
                    self.zuletzt, self.fehler = time.time(), ""
                else:
                    self.fehler = "Drive nicht lesbar: " + fehler.decode("utf-8", "replace").strip().splitlines()[-1][:200]
                    log.warning("Tagebuch: %s", self.fehler)
            except Exception as ausnahme:
                self.fehler = "Drive nicht lesbar: " + str(ausnahme)[:200]
                log.warning("Tagebuch: %s", self.fehler)

    async def antwort(self, request: web.Request) -> web.Response:
        if not hmac.compare_digest(request.headers.get("X-Jarvis-Token", "").encode(), TOKEN.encode()):
            return web.Response(status=404, text="Not found")
        await self.hole()
        try:
            tage = max(1, min(int(request.query.get("tage", "60")), 4000))
        except ValueError:
            tage = 60
        ab = (date.today() - timedelta(days=tage)).isoformat()
        dateien = []
        if TAGEBUCH_ORDNER.exists():
            for datei in sorted(TAGEBUCH_ORDNER.glob("*.md")):
                treffer = re.match(r"(\d{4}-\d{2}-\d{2})", datei.name)
                # Dateien ohne Datum im Namen kommen immer mit; sie sind selten und sonst unauffindbar.
                if treffer and treffer.group(1) < ab:
                    continue
                dateien.append({"name": datei.name, "datum": treffer.group(1) if treffer else "",
                                "geaendert": int(datei.stat().st_mtime), "text": datei.read_text("utf-8", "replace")[:200_000]})
        return web.json_response({"dateien": dateien, "stand": int(self.zuletzt), "fehler": self.fehler},
                                 dumps=lambda daten: json.dumps(daten, ensure_ascii=False))


async def gesund(_: web.Request) -> web.Response:
    return web.Response(text="ok")


async def unbekannt(_: web.Request) -> web.Response:
    return web.Response(status=404, text="Not found")


@web.middleware
async def mitschrift(request: web.Request, handler):
    """Eine Zeile je Anfrage, damit sich nachvollziehen laesst, was ChatGPT fragt. Das Geheimnis wird nie geloggt."""
    teile = request.path.split("/")
    if len(teile) > 2 and teile[1] == "j":
        teile[2] = "<geheim>" if len(teile[2]) >= 32 else "<falsch>"
    pfad = "/".join(teile)[:120]
    rpc = ""
    if request.method == "POST" and request.can_read_body:
        try:
            daten = json.loads(await request.text())
            rpc = daten.get("method", "") if isinstance(daten, dict) else "stapel"
            if rpc == "tools/call":
                rpc += ":" + str((daten.get("params") or {}).get("name", ""))
        except Exception:
            rpc = "?"
    start = asyncio.get_running_loop().time()
    status = 500
    try:
        antwort = await handler(request)
        status = antwort.status
        return antwort
    except web.HTTPException as fehler:
        status = fehler.status
        raise
    finally:
        if pfad != "/gesund":
            dauer = int((asyncio.get_running_loop().time() - start) * 1000)
            log.info("%s %s %s -> %s (%s ms) ua=%s accept=%s", request.method, pfad, rpc, status, dauer,
                     request.headers.get("User-Agent", "")[:60], request.headers.get("Accept", "")[:60])


def app() -> web.Application:
    relay = Relay()
    anwendung = web.Application(client_max_size=MAX_RUMPF, middlewares=[mitschrift])
    anwendung.router.add_get("/geraet/ws", relay.geraet)
    anwendung.router.add_get("/geraet/tagebuch", Tagebuch().antwort)
    anwendung.router.add_route("*", "/j/{geheimnis}/mcp", relay.mcp)
    anwendung.router.add_get("/gesund", gesund)
    anwendung.router.add_route("*", "/{rest:.*}", unbekannt)
    return anwendung


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    web.run_app(app(), host="0.0.0.0", port=8080, access_log=None)
