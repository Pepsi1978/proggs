"""Jarvis-Relay: reicht MCP-Aufrufe von ChatGPT an die Jarvis-App auf dem Handy weiter.

Der MCP-Server selbst laeuft auf dem Handy. Dieser Dienst ist nur die oeffentliche Gegenstelle:
  ChatGPT  --POST /j/<Geheimnis>/mcp-->  Relay  --WebSocket-->  Jarvis-App
Die WebSocket baut das Handy von sich aus auf (/geraet/ws, Geraete-Token im Header) und nennt dabei
sein Geheimnis. Nur fuer dieses Geheimnis nimmt der Relay danach Aufrufe an; alles andere ist 404.
"""
import asyncio
import hmac
import json
import logging
import os
import uuid
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


async def gesund(_: web.Request) -> web.Response:
    return web.Response(text="ok")


def app() -> web.Application:
    relay = Relay()
    anwendung = web.Application(client_max_size=MAX_RUMPF)
    anwendung.router.add_get("/geraet/ws", relay.geraet)
    anwendung.router.add_route("*", "/j/{geheimnis}/mcp", relay.mcp)
    anwendung.router.add_get("/gesund", gesund)
    return anwendung


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    web.run_app(app(), host="0.0.0.0", port=8080, access_log=None)
