"""Jarvis-Relay: reicht MCP-Aufrufe von ChatGPT an die Jarvis-App auf dem Handy weiter.

Der MCP-Server selbst laeuft auf dem Handy. Dieser Dienst ist nur die oeffentliche Gegenstelle:
  ChatGPT  --POST /j/<Geheimnis>/mcp-->  Relay  --WebSocket-->  Jarvis-App
Die WebSocket baut das Handy von sich aus auf (/geraet/ws, Geraete-Token im Header) und nennt dabei
sein Geheimnis. Nur fuer dieses Geheimnis nimmt der Relay danach Aufrufe an; alles andere ist 404.

Zweite Aufgabe: Franks Tagebuch. Die Eintraege liegen als Markdown-Dateien in einem Google-Drive-Ordner.
Der Relay holt sie mit rclone (Zugang NUR LESEND, auf genau diesen Ordner als Wurzel gestellt) und gibt sie
dem Handy unter /geraet/tagebuch heraus – ebenfalls nur mit dem Geraete-Token. Solange das Handy verbunden ist,
gleicht der Relay den Ordner alle zwei Minuten selbst ab und schickt bei einem neuen oder geaenderten Eintrag
sofort {"signal": "tagebuch"} ueber die WebSocket: So steht ein neuer Eintrag nach Sekunden im Gedaechtnis von Jarvis.

Dritte Aufgabe: die PC-Bruecke fuer Programmierauftraege. Das Skript Jarvis/werkzeuge/pc-bruecke auf Franks PC fragt
/pc/auftrag in einer Dauerschleife ab (Long-Polling, Geraete-Token). Solange es das tut, gilt der PC als da. Das Handy
schickt einen Auftrag an /geraet/pc-auftrag; ist der PC da, holt die Bruecke ihn ab, startet eine Claude-Code-Sitzung mit
Remote Control und quittiert ueber /pc/quittung. Ist der PC nicht da, antwortet der Relay 409 und das Handy nimmt die Cloud.

Vierte Aufgabe: Meldungen und Rueckfragen dieser Sitzungen (Hook Jarvis/werkzeuge/pc-bruecke/jarvis_hook.py). Der Hook schickt
POST /pc/meldung; der Relay reicht sie als {"signal": "programmieren", ...} ans Handy (ohne Handy: Warteschlange, beim naechsten
Verbinden zugestellt). Eine Rueckfrage traegt eine frage_id; der Hook fragt GET /pc/antwort?frage_id=… ab, bis Frank in Jarvis
geantwortet hat (POST /geraet/antwort) oder sein Zeitlimit ablaeuft.

Fuenfte Aufgabe: Nachtraege zu einem laufenden Auftrag. Das Handy legt einen freien Nachtrag fuer genau eine auftrag_id ab
(POST /geraet/nachtrag, mit nachtrag_id gegen doppelte Zustellung). Der Hook dieser Sitzung holt ihn ab (GET /pc/nachtrag,
Long-Polling) und quittiert die Zustellung mit einer Meldung (stand "nachtrag", nachtrag_ids); erst dann faellt er aus der
Warteschlange. Die Warteschlange ueberlebt Neustarts (/data/nachtraege.json) und haelt hoechstens sieben Tage.
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
NACH_TRENNUNG_S = 15       # so lange duerfen offene Aufrufe nach einer Trennung auf die neue Verbindung warten
ZEITLIMIT_S = 75           # laengster Werkzeugaufruf (der freie Auftrag an Jarvis hat 45 s)
MAX_RUMPF = 1_000_000
# Antworten auf diese Methoden sind immer gleich; aus dem Zwischenspeicher kann ChatGPT sich auch
# dann verbinden, wenn das Handy gerade kurz weg ist.
MERKBAR = {"initialize", "tools/list"}

RCLONE_KONFIG = os.environ.get("JARVIS_RCLONE_KONFIG", "/data/rclone.conf")
TAGEBUCH_ORDNER = Path(os.environ.get("JARVIS_TAGEBUCH", "/data/tagebuch"))
TAGEBUCH_ABSTAND_S = 120   # hoechstens alle zwei Minuten bei Google nachfragen

log = logging.getLogger("jarvis-relay")
_KEINE = object()   # der Aufruf hat das Handy nie erreicht: nichts wurde geaendert
_UNKLAR = object()  # der Aufruf war beim Handy, die Antwort kam aber nicht an: ob er ausgefuehrt wurde, ist offen

# Laufende Hintergrundaufgaben brauchen eine feste Referenz, sonst kann asyncio sie mitten im Lauf einsammeln.
_aufgaben = set()


def _im_hintergrund(koroutine) -> None:
    aufgabe = asyncio.create_task(koroutine)
    _aufgaben.add(aufgabe)
    aufgabe.add_done_callback(_aufgaben.discard)


class Relay:
    def __init__(self):
        self.geheimnis = ""          # zuletzt vom Handy gemeldetes Geheimnis
        self.gemerkt = {}            # Methode -> result-Objekt
        self.ws = None
        self.ausstehend = None       # Signal, das ohne verbundenes Handy kam: geht beim naechsten Verbinden raus
        self.meldungen = []          # Programmier-Meldungen ohne verbundenes Handy (hoechstens 100, aelteste fallen weg)
        self.offen = {}              # Aufruf-id -> (Future, WebSocket, ueber die der Aufruf ging)
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
            # Im Hintergrund: Haengt die alte Verbindung am toten Netz, wartete close() bis zu 10 s auf die Gegenstelle,
            # und so lange laege die neue (mit den nachgereichten Antworten) ungelesen.
            _im_hintergrund(alt.close())
        if geheimnis != self.geheimnis:
            self.geheimnis, self.gemerkt = geheimnis, {}
            self._sichere()
        self.da.set()
        log.info("Handy verbunden")
        while self.meldungen:
            meldung = self.meldungen.pop(0)
            try:
                await ws.send_str(json.dumps(meldung))
            except (ConnectionError, RuntimeError):
                self.meldungen.insert(0, meldung)
                break
        if self.ausstehend is not None:
            signal, self.ausstehend = self.ausstehend, None
            try:
                await ws.send_str(json.dumps(signal))
                log.info("Ausstehendes Signal %s nachgereicht", signal.get("signal"))
            except (ConnectionError, RuntimeError):
                self.ausstehend = signal
        try:
            async for nachricht in ws:
                if nachricht.type != WSMsgType.TEXT:
                    continue
                try:
                    daten = json.loads(nachricht.data)
                except ValueError:
                    continue
                # Die Antwort darf ueber eine neuere Verbindung kommen als der Aufruf: zugeordnet wird ueber die id.
                eintrag = self.offen.pop(daten.get("id"), None)
                if eintrag is not None and not eintrag[0].done():
                    eintrag[0].set_result(daten.get("antwort"))
        finally:
            if self.ws is ws:
                self.ws = None
                self.da.clear()
                log.info("Handy getrennt")
                _im_hintergrund(self._nach_trennung(ws))
        return ws

    async def _nach_trennung(self, alt) -> None:
        """Kommt das Handy nicht bald zurueck, warten offene Aufrufe nicht stur bis zum Zeitlimit."""
        try:
            await asyncio.wait_for(self.da.wait(), NACH_TRENNUNG_S)
            return  # wieder da: Die Antworten kommen ueber die neue Verbindung
        except asyncio.TimeoutError:
            pass
        for wartend, ws in list(self.offen.values()):
            if ws is alt and not wartend.done():
                wartend.set_result(_UNKLAR)

    async def signal(self, request: web.Request) -> web.Response:
        """Ein Signal von aussen (z. B. "update" vom apk-update-Skript am PC) sofort ans Handy weiterreichen."""
        if not hmac.compare_digest(request.headers.get("X-Jarvis-Token", "").encode(), TOKEN.encode()):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            assert isinstance(daten, dict) and isinstance(daten.get("signal"), str) and daten["signal"]
        except Exception:
            return web.Response(status=400, text="Signal fehlt")
        ws = self.ws
        if ws is not None and not ws.closed:
            try:
                await ws.send_str(json.dumps(daten))
                log.info("Signal %s ans Handy", daten["signal"])
                return web.json_response({"zugestellt": True})
            except (ConnectionError, RuntimeError):
                pass
        self.ausstehend = daten
        log.info("Signal %s gemerkt: Handy nicht verbunden", daten["signal"])
        return web.json_response({"zugestellt": False})

    async def programmier_meldung(self, daten: dict) -> bool:
        """Reicht eine Programmier-Meldung ans Handy; ohne Handy kommt sie in die Warteschlange. True = sofort zugestellt."""
        ws = self.ws
        if ws is not None and not ws.closed:
            try:
                await ws.send_str(json.dumps(daten))
                return True
            except (ConnectionError, RuntimeError):
                pass
        # Lebenszeichen nur live: nachgereicht hätten sie keinen Wert und verdrängten echte Meldungen aus der Warteschlange.
        if daten.get("stand") != "puls":
            self.meldungen.append(daten)
            del self.meldungen[:-100]
        return False

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

        # Benachrichtigungen (ohne id, z. B. notifications/initialized) beantwortet das Handy nie: keine Rundreise dafuer.
        if einzeln is not None and einzeln.get("id") is None:
            return web.Response(status=202)
        if isinstance(anfrage, list) and anfrage and all(isinstance(x, dict) and x.get("id") is None for x in anfrage):
            return web.Response(status=202)
        # Ping prueft nur, ob der Dienst lebt: bei verbundenem Handy sofort antworten.
        if methode == "ping" and self.ws is not None and not self.ws.closed:
            return self._json({"jsonrpc": "2.0", "id": einzeln["id"], "result": {}})

        if self.ws is None:
            try:
                await asyncio.wait_for(self.da.wait(), WARTE_AUF_HANDY_S)
            except asyncio.TimeoutError:
                return self._ohne_handy(einzeln, methode)

        antwort = await self._frage_handy(rumpf)
        if antwort is _UNKLAR and methode == "tools/call":
            # Nicht "nichts geaendert" melden: Der Auftrag kam an, ein blindes Wiederholen legte Termine, Ideen oder Mails doppelt an.
            log.warning("Antwort des Handys blieb aus (%s)", (einzeln.get("params") or {}).get("name", ""))
            text = ("Jarvis hat den Auftrag erhalten, aber die Antwort kam nicht an. Ob er ausgeführt wurde, ist unklar. "
                    "Prüfe es zuerst mit dem passenden _lesen-Werkzeug und wiederhole schreibende Aufrufe nicht blind; "
                    "einen rein lesenden Aufruf kannst du einfach noch einmal stellen.")
            return self._json({"jsonrpc": "2.0", "id": einzeln["id"],
                               "result": {"content": [{"type": "text", "text": text}], "isError": True}})
        if antwort is _KEINE or antwort is _UNKLAR:
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
            # Die Verbindung schliesst gerade: kurz auf die neue warten (das Handy verbindet sich nach wenigen Sekunden neu).
            schleife = asyncio.get_running_loop()
            frist = schleife.time() + WARTE_AUF_HANDY_S
            while (ws is None or ws.closed) and schleife.time() < frist:
                await asyncio.sleep(0.25)
                ws = self.ws
            if ws is None or ws.closed:
                return _KEINE
        nummer = uuid.uuid4().hex
        wartend = asyncio.get_running_loop().create_future()
        self.offen[nummer] = (wartend, ws)
        try:
            try:
                await ws.send_str(json.dumps({"id": nummer, "rumpf": rumpf}))
            except (ConnectionError, RuntimeError):
                return _KEINE          # nie angekommen: es ist sicher nichts passiert
            try:
                return await asyncio.wait_for(wartend, ZEITLIMIT_S)
            except asyncio.TimeoutError:
                return _UNKLAR         # angekommen, aber keine Antwort: ob es lief, ist offen
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
        self.fingerabdruck = None   # Stand des lokalen Ordners beim letzten Blick (Name, Groesse, Aenderungszeit)

    def _fingerabdruck(self) -> str:
        if not TAGEBUCH_ORDNER.exists():
            return ""
        teile = []
        for datei in sorted(TAGEBUCH_ORDNER.glob("*.md")):
            stat = datei.stat()
            teile.append(f"{datei.name}:{stat.st_size}:{int(stat.st_mtime)}")
        return str(hash("|".join(teile)))

    async def wache(self, relay) -> None:
        """Gleicht den Drive-Ordner regelmaessig ab, solange das Handy verbunden ist, und meldet jede Aenderung sofort."""
        while True:
            try:
                await asyncio.sleep(TAGEBUCH_ABSTAND_S + 1)
                ws = relay.ws
                if ws is None or ws.closed:
                    continue
                await self.hole()
                neu = self._fingerabdruck()
                if self.fingerabdruck is None or neu == self.fingerabdruck:
                    self.fingerabdruck = neu
                    continue
                # hole() kann bis zu 90 s dauern: Inzwischen kann sich das Handy neu verbunden haben, also neu lesen.
                ws = relay.ws
                if ws is None or ws.closed:
                    continue  # alten Fingerabdruck stehen lassen: Die naechste Runde meldet die Aenderung
                await ws.send_str(json.dumps({"signal": "tagebuch"}))
                log.info("Tagebuch geaendert: Signal ans Handy")
                self.fingerabdruck = neu
            except asyncio.CancelledError:
                raise
            except Exception as ausnahme:
                log.warning("Tagebuch-Waechter: %s", ausnahme)

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
                try:
                    _, fehler = await asyncio.wait_for(prozess.communicate(), 90)
                except asyncio.TimeoutError:
                    # wait_for bricht nur das Warten ab: rclone liefe sonst weiter, und der naechste Lauf startete einen zweiten
                    # Abgleich in denselben Ordner.
                    prozess.kill()
                    await prozess.wait()
                    raise
                if prozess.returncode == 0:
                    self.zuletzt, self.fehler = time.time(), ""
                else:
                    self.fehler = "Drive nicht lesbar: " + fehler.decode("utf-8", "replace").strip().splitlines()[-1][:200]
                    log.warning("Tagebuch: %s", self.fehler)
            except Exception as ausnahme:
                self.fehler = "Drive nicht lesbar: " + str(ausnahme)[:200]
                log.warning("Tagebuch: %s", self.fehler)

    async def neuer_zugang(self, request: web.Request) -> web.Response:
        """Nimmt einen frisch erteilten Nur-Lese-Zugang vom Handy an und traegt ihn in die rclone-Konfiguration ein."""
        if not hmac.compare_digest(request.headers.get("X-Jarvis-Token", "").encode(), TOKEN.encode()):
            return web.Response(status=404, text="Not found")
        try:
            zugang = (await request.json()).get("token")
            assert isinstance(zugang, dict) and zugang.get("refresh_token") and zugang.get("access_token")
            konfig = Path(RCLONE_KONFIG)
            zeilen = konfig.read_text("utf-8").splitlines()
            assert any(z.startswith("token = ") for z in zeilen), "In der Konfiguration fehlt die Zeile token."
            zeile = "token = " + json.dumps({k: zugang[k] for k in ("access_token", "token_type", "refresh_token", "expiry") if k in zugang})
            konfig.write_text("\n".join(zeile if z.startswith("token = ") else z for z in zeilen) + "\n", "utf-8")
        except Exception as ausnahme:
            log.warning("Drive-Zugang nicht uebernommen: %s", ausnahme)
            return web.Response(status=400, text="Zugang nicht uebernommen")
        self.zuletzt, self.fehler = 0.0, ""
        # Den Abgleich nicht abwarten: Er kann bis zu 90 s dauern, das Handy gaebe nach 30 s auf und meldete faelschlich einen Fehler.
        _im_hintergrund(self._abgleich_nach_zugang())
        log.info("Drive-Zugang erneuert, Abgleich laeuft im Hintergrund")
        return web.json_response({"fehler": ""})

    async def _abgleich_nach_zugang(self) -> None:
        await self.hole()
        log.info("Abgleich nach neuem Drive-Zugang fertig, Fehler: %r", self.fehler)

    async def antwort(self, request: web.Request) -> web.Response:
        if not hmac.compare_digest(request.headers.get("X-Jarvis-Token", "").encode(), TOKEN.encode()):
            return web.Response(status=404, text="Not found")
        # Laeuft gerade ein Abgleich (bis 90 s), den lokalen Stand liefern statt danach womoeglich noch einen zu starten:
        # Zusammen laege das ueber Caddys 90 s, und das Handy bekaeme 502.
        if not self.sperre.locked():
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


PC_FRISCH_S = 45      # so lange nach seiner letzten Abfrage gilt der PC als da
PC_WARTEN_S = 25      # eine Abfrage der Bruecke wartet hoechstens so lange auf einen Auftrag
PC_QUITTUNG_S = 45    # so lange wartet das Handy auf die Bestaetigung; die Bruecke antwortet spaetestens nach 30 s


FRAGE_HALTEN_S = 3600     # so lange bleibt eine unbeantwortete Rueckfrage abrufbar
ANTWORT_WARTEN_S = 25     # eine Abfrage des Hooks wartet hoechstens so lange auf Franks Antwort

NACHTRAG_HALTEN_S = 7 * 86400   # so lange bleibt ein nicht zugestellter Nachtrag eingereiht
NACHTRAG_WARTEN_S = 25          # eine Abfrage des Hooks wartet hoechstens so lange auf einen Nachtrag
NACHTRAG_ZUSTELLEN_S = 8        # so lange wartet das Handy auf die Quittung, wenn die Sitzung gerade abfragt
WARTER_FRISCH_S = 40            # so lange nach seiner letzten Abfrage gilt der Warter einer Sitzung als da
NACHTRAEGE = ZUSTAND.with_name("nachtraege.json")


class PcBruecke:
    """Programmierauftraege vom Handy an Franks PC (siehe Kopf der Datei)."""

    def __init__(self, relay):
        self.relay = relay
        self.fragen = {}      # frage_id -> {"zeit": monotonic, "antwort": dict|None, "da": asyncio.Event}
        self.zuletzt = 0.0
        self.rechner = ""
        self.auftraege = asyncio.Queue()
        self.quittungen = {}  # Auftrag-id -> Future mit der Quittung der Bruecke
        self.nachtraege = {}  # AUFTRAG-ID -> [{"nachtrag_id", "text", "zeit"}], aelteste zuerst
        self.erledigt = {}    # nachtrag_id -> Zeit der Zustellung (Schutz gegen doppelte Zustellung)
        self.kontakt = {}     # AUFTRAG-ID -> monotonic der letzten Meldung der Sitzung (Schritt, Puls): Zeichen, dass sie arbeitet
        self.warter = {}      # AUFTRAG-ID -> monotonic der letzten Nachtrags-Abfrage des Warters
        self.neu_da = {}      # AUFTRAG-ID -> asyncio.Event: ein Nachtrag kam dazu
        self.zugestellt = {}  # nachtrag_id -> asyncio.Event: der Hook hat quittiert
        self._lade_nachtraege()

    @staticmethod
    def _erlaubt(request: web.Request) -> bool:
        return hmac.compare_digest(request.headers.get("X-Jarvis-Token", "").encode(), TOKEN.encode())

    # ---------------------------------------------------------------- Nachtraege

    def _lade_nachtraege(self) -> None:
        try:
            daten = json.loads(NACHTRAEGE.read_text("utf-8"))
            self.nachtraege = {k: v for k, v in (daten.get("offen") or {}).items() if isinstance(v, list)}
            self.erledigt = dict(daten.get("erledigt") or {})
        except Exception:
            pass

    def _sichere_nachtraege(self) -> None:
        grenze = time.time() - NACHTRAG_HALTEN_S
        self.nachtraege = {k: [n for n in v if n.get("zeit", 0) >= grenze] for k, v in self.nachtraege.items()}
        self.nachtraege = {k: v for k, v in self.nachtraege.items() if v}
        self.erledigt = dict(sorted(self.erledigt.items(), key=lambda e: e[1])[-500:])
        try:
            NACHTRAEGE.parent.mkdir(parents=True, exist_ok=True)
            NACHTRAEGE.write_text(json.dumps({"offen": self.nachtraege, "erledigt": self.erledigt}, ensure_ascii=False), "utf-8")
        except Exception as fehler:
            log.warning("Nachtraege nicht gesichert: %s", fehler)

    def _ereignis(self, auftrag_id: str) -> asyncio.Event:
        return self.neu_da.setdefault(auftrag_id, asyncio.Event())

    def _wartet(self, auftrag_id: str) -> bool:
        return time.monotonic() - self.warter.get(auftrag_id, -1e9) < WARTER_FRISCH_S

    def _quittiert(self, auftrag_id: str, ids: list) -> None:
        """Der Hook hat diese Nachtraege an seine Sitzung gegeben: aus der Warteschlange, nie wieder ausliefern."""
        ids = {str(i) for i in ids}
        jetzt = time.time()
        for i in ids:
            self.erledigt[i] = jetzt
            ereignis = self.zugestellt.get(i)
            if ereignis is not None:
                ereignis.set()
        self.nachtraege[auftrag_id] = [n for n in self.nachtraege.get(auftrag_id, []) if n.get("nachtrag_id") not in ids]
        self._sichere_nachtraege()

    async def nachtrag_geben(self, request: web.Request) -> web.Response:
        """POST /geraet/nachtrag (Handy): {"auftrag_id", "nachtrag_id", "text"}. Antwort: angenommen, zugestellt, wartet, kontakt_vor_s, pc."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            auftrag_id = str(daten["auftrag_id"]).strip().upper()
            nachtrag_id = str(daten["nachtrag_id"]).strip()
            text = str(daten["text"]).strip()
            assert auftrag_id and nachtrag_id and text
        except Exception:
            return web.Response(status=400, text="Nachtrag unlesbar")
        liste = self.nachtraege.setdefault(auftrag_id, [])
        if nachtrag_id not in self.erledigt and not any(n.get("nachtrag_id") == nachtrag_id for n in liste):
            liste.append({"nachtrag_id": nachtrag_id, "text": text[:20_000], "zeit": time.time()})
            self._sichere_nachtraege()
            log.info("Nachtrag fuer %s eingereiht", auftrag_id)
        self._ereignis(auftrag_id).set()
        # Wartet die Sitzung gerade, holt ihr Hook den Nachtrag sofort: kurz auf die Quittung warten, damit Jarvis
        # „zugestellt“ statt nur „angenommen“ sagen kann.
        if nachtrag_id not in self.erledigt and self._wartet(auftrag_id):
            ereignis = self.zugestellt.setdefault(nachtrag_id, asyncio.Event())
            try:
                await asyncio.wait_for(ereignis.wait(), NACHTRAG_ZUSTELLEN_S)
            except asyncio.TimeoutError:
                pass
            finally:
                self.zugestellt.pop(nachtrag_id, None)
        jetzt = time.monotonic()
        kontakt, warter = self.kontakt.get(auftrag_id), self.warter.get(auftrag_id)
        # kontakt = letzte Arbeit (Meldung, Puls), warter = letzte Nachtrags-Abfrage im Leerlauf. Fragte der Warter nach der
        # letzten Arbeit noch ab und tut es jetzt nicht mehr, ist die Sitzung weg; Jarvis unterscheidet das an beiden Zeiten.
        return web.json_response({"angenommen": True, "zugestellt": nachtrag_id in self.erledigt, "wartet": self._wartet(auftrag_id),
                                  "kontakt_vor_s": int(jetzt - kontakt) if kontakt else None,
                                  "warter_vor_s": int(jetzt - warter) if warter else None, "pc": self.da()})

    async def nachtrag_abholen(self, request: web.Request) -> web.Response:
        """GET /pc/nachtrag?auftrag_id=…&warten=… (Hook): 200 {"nachtraege": [...]}, 204 = keiner. Entfernt nichts (erst die Quittung)."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        auftrag_id = request.query.get("auftrag_id", "").strip().upper()
        if not auftrag_id:
            return web.Response(status=400, text="auftrag_id fehlt")
        try:
            warten = max(0, min(int(request.query.get("warten", "0")), NACHTRAG_WARTEN_S))
        except ValueError:
            warten = 0
        if warten:
            self.warter[auftrag_id] = time.monotonic()
        if not self.nachtraege.get(auftrag_id) and warten:
            ereignis = self._ereignis(auftrag_id)
            ereignis.clear()
            try:
                await asyncio.wait_for(ereignis.wait(), warten)
            except asyncio.TimeoutError:
                pass
            self.warter[auftrag_id] = time.monotonic()
        offen = self.nachtraege.get(auftrag_id) or []
        if not offen:
            return web.Response(status=204)
        return web.json_response({"nachtraege": offen}, dumps=lambda d: json.dumps(d, ensure_ascii=False))

    def da(self) -> bool:
        return time.monotonic() - self.zuletzt < PC_FRISCH_S

    async def abholen(self, request: web.Request) -> web.Response:
        """GET /pc/auftrag (Bruecke): wartet bis zu PC_WARTEN_S auf einen Auftrag, sonst 204."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        if not self.da():
            log.info("PC-Bruecke verbunden")
        self.zuletzt = time.monotonic()
        self.rechner = request.headers.get("X-Jarvis-Rechner", "PC")[:60]
        frist = asyncio.get_running_loop().time() + PC_WARTEN_S
        while True:
            rest = frist - asyncio.get_running_loop().time()
            if rest <= 0:
                self.zuletzt = time.monotonic()
                return web.Response(status=204)
            try:
                auftrag = await asyncio.wait_for(self.auftraege.get(), rest)
            except asyncio.TimeoutError:
                self.zuletzt = time.monotonic()
                return web.Response(status=204)
            self.zuletzt = time.monotonic()
            # Ein Auftrag, auf dessen Quittung das Handy nicht mehr wartet, darf nie mehr starten (sonst doppelt mit der Cloud).
            wartend = self.quittungen.get(auftrag["id"])
            if wartend is None or wartend.done():
                continue
            # Ist die Abfrage der Bruecke inzwischen abgerissen (Ruhezustand, Netzwechsel), ginge der Auftrag ins Leere:
            # zuruecklegen, die naechste Abfrage holt ihn.
            if request.transport is None or request.transport.is_closing():
                self.auftraege.put_nowait(auftrag)
                return web.Response(status=204)
            return web.json_response(auftrag)

    async def quittung(self, request: web.Request) -> web.Response:
        """POST /pc/quittung (Bruecke): {"id", "ok", "text"}."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            wartend = self.quittungen.get(daten["id"])
        except Exception:
            return web.Response(status=400, text="Quittung unlesbar")
        if wartend is not None and not wartend.done():
            wartend.set_result(daten)
        return web.json_response({"angenommen": wartend is not None})

    async def auftrag(self, request: web.Request) -> web.Response:
        """POST /geraet/pc-auftrag (Handy): {"app", "auftrag", "review"}. 409 = kein PC da, 504 = keine Quittung."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        if not self.da():
            return web.json_response({"pc": False}, status=409)
        try:
            daten = await request.json()
            assert isinstance(daten.get("app"), str) and isinstance(daten.get("auftrag"), str) and daten["auftrag"].strip()
        except Exception:
            return web.Response(status=400, text="Auftrag unlesbar")
        nummer = uuid.uuid4().hex
        wartend = asyncio.get_running_loop().create_future()
        self.quittungen[nummer] = wartend
        try:
            await self.auftraege.put({"id": nummer, "app": daten["app"], "auftrag": daten["auftrag"], "review": bool(daten.get("review")),
                                      "nummer": str(daten.get("nummer", ""))[:20]})
            log.info("Programmierauftrag an den PC: %s", daten["app"][:40])
            quittung = await asyncio.wait_for(wartend, PC_QUITTUNG_S)
        except asyncio.TimeoutError:
            log.info("PC hat den Auftrag nicht quittiert")
            return web.json_response({"pc": True, "ok": False, "fehler": "Der PC hat den Start nicht bestaetigt."}, status=504)
        finally:
            self.quittungen.pop(nummer, None)
        return web.json_response({"pc": True, "ok": bool(quittung.get("ok")), "text": str(quittung.get("text", ""))[:500], "rechner": self.rechner})

    def _aufraeumen(self) -> None:
        grenze = time.monotonic() - FRAGE_HALTEN_S
        for frage_id in [k for k, v in self.fragen.items() if v["zeit"] < grenze]:
            self.fragen.pop(frage_id, None)

    async def meldung(self, request: web.Request) -> web.Response:
        """POST /pc/meldung (Hook am PC): {"auftrag_id", "stand", "schritt", "frage"?: {"frage_id", "fragen", "frei"}}."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            assert isinstance(daten.get("auftrag_id"), str) and daten["auftrag_id"] and isinstance(daten.get("schritt"), str)
        except Exception:
            return web.Response(status=400, text="Meldung unlesbar")
        self._aufraeumen()
        auftrag_id = daten["auftrag_id"].strip().upper()
        self.kontakt[auftrag_id] = time.monotonic()
        if daten.get("stand") == "nachtrag" and isinstance(daten.get("nachtrag_ids"), list):
            self._quittiert(auftrag_id, daten["nachtrag_ids"])
        frage = daten.get("frage")
        if isinstance(frage, dict) and isinstance(frage.get("frage_id"), str) and frage["frage_id"]:
            self.fragen.setdefault(frage["frage_id"], {"zeit": time.monotonic(), "antwort": None, "da": asyncio.Event()})
        zugestellt = await self.relay.programmier_meldung({"signal": "programmieren", **daten})
        return web.json_response({"zugestellt": zugestellt})

    async def antwort_abholen(self, request: web.Request) -> web.Response:
        """GET /pc/antwort?frage_id=… (Hook): 200 mit Antwort, 204 = noch keine, 404 = Frage unbekannt oder verfallen."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        eintrag = self.fragen.get(request.query.get("frage_id", ""))
        if eintrag is None:
            return web.Response(status=404, text="Frage unbekannt")
        if eintrag["antwort"] is None:
            try:
                await asyncio.wait_for(eintrag["da"].wait(), ANTWORT_WARTEN_S)
            except asyncio.TimeoutError:
                return web.Response(status=204)
        return web.json_response(eintrag["antwort"])

    async def antwort_geben(self, request: web.Request) -> web.Response:
        """POST /geraet/antwort (Handy): {"frage_id", "antworten": {Frage: Antwort}, "text"?}."""
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            eintrag = self.fragen[daten["frage_id"]]
        except Exception:
            return web.json_response({"angenommen": False, "grund": "Die Rueckfrage ist nicht mehr offen."}, status=404)
        if eintrag["antwort"] is not None:
            return web.json_response({"angenommen": False, "grund": "Die Rueckfrage ist schon beantwortet."}, status=409)
        eintrag["antwort"] = {"antworten": daten.get("antworten") or {}, "text": str(daten.get("text", ""))[:4000]}
        eintrag["da"].set()
        log.info("Rueckfrage beantwortet")
        return web.json_response({"angenommen": True})

    async def status(self, request: web.Request) -> web.Response:
        if not self._erlaubt(request):
            return web.Response(status=404, text="Not found")
        vor = int(time.monotonic() - self.zuletzt) if self.zuletzt else None
        return web.json_response({"pc": self.da(), "rechner": self.rechner, "zuletzt_vor_s": vor})


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
    tagebuch = Tagebuch()

    async def tagebuch_waechter(_anwendung):
        _im_hintergrund(tagebuch.wache(relay))

    anwendung.on_startup.append(tagebuch_waechter)
    anwendung.router.add_get("/geraet/tagebuch", tagebuch.antwort)
    anwendung.router.add_post("/geraet/drive-zugang", tagebuch.neuer_zugang)
    anwendung.router.add_post("/geraet/signal", relay.signal)
    pc = PcBruecke(relay)
    anwendung.router.add_post("/geraet/pc-auftrag", pc.auftrag)
    anwendung.router.add_get("/geraet/pc", pc.status)
    anwendung.router.add_get("/pc/auftrag", pc.abholen)
    anwendung.router.add_post("/pc/quittung", pc.quittung)
    anwendung.router.add_post("/pc/meldung", pc.meldung)
    anwendung.router.add_get("/pc/antwort", pc.antwort_abholen)
    anwendung.router.add_post("/geraet/antwort", pc.antwort_geben)
    anwendung.router.add_post("/geraet/nachtrag", pc.nachtrag_geben)
    anwendung.router.add_get("/pc/nachtrag", pc.nachtrag_abholen)
    anwendung.router.add_route("*", "/j/{geheimnis}/mcp", relay.mcp)
    anwendung.router.add_get("/gesund", gesund)
    anwendung.router.add_route("*", "/{rest:.*}", unbekannt)
    return anwendung


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    web.run_app(app(), host="0.0.0.0", port=8080, access_log=None)
