"""Jarvis-Relay: reicht MCP-Aufrufe von ChatGPT an die Jarvis-App auf dem Handy weiter.

Der MCP-Server selbst laeuft auf dem Handy. Dieser Dienst ist nur die oeffentliche Gegenstelle:
  ChatGPT  --POST /j/<Geheimnis>/mcp-->  Relay  --WebSocket-->  Jarvis-App
Die WebSocket baut das Handy von sich aus auf (/geraet/ws, Handy-Token im Header; Rollen siehe HANDY_TOKEN) und nennt dabei
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

Sechste Aufgabe: Anmeldestand von Claude Code je Rechner, getrennt von der Erreichbarkeit. Die Bruecke meldet bei jeder
Abfrage den lokalen Stand (X-Jarvis-Anmeldung: lokal = Anmeldedaten liegen vor, abgemeldet, unbekannt; ohne Kopfzeile
unbekannt) mit Pruefzeit. Ob die Anmeldung wirklich gilt, zeigen nur die Sitzungen: Meldet ein Hook „anmeldung“ (Login
expired), ist sie abgelaufen; jede spaetere Arbeit einer Sitzung (Puls, Schritt, „anmeldung_ok“), die nach dem Fehler begann,
bestaetigt sie wieder. /geraet/pc liefert beides je Rechner und je Auftrag die Lebenszeichen (kontakt, warter).

Siebte Aufgabe: am Rechner direkt gestartete Sitzungen (Meldungen mit dem Feld „lokal“, jarvis_hook.py lokal und
jarvis_lokal.py). Sie laufen wie Auftraege durch dieselben Wege. Entfernt Frank eine in Jarvis (POST /geraet/lokal-entfernen),
merkt sich der Relay die Nummer (/data/lokal-entfernt.json, 30 Tage): Ihre Meldungen gehen nicht mehr ans Handy, die Antwort
traegt „entfernt“, und /pc/nachtrag antwortet 410. Die Sitzung am Rechner laeuft davon unberuehrt weiter. Eine freie Rueckfrage
einer lokalen Sitzung (frage.als_nachtrag) wartet nicht: Franks Antwort wird zum Nachtrag „antwort-<frage_id>“, den der Warter zustellt.
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

# Zwei Rollen (Befund Token im APK): Der bisherige Geraete-Token gilt fuer die PC-Bruecken (/pc/*) und fuer /geraet/signal
# (apk-update, Cloud-Bau). Das Handy bekommt einen eigenen Token (JARVIS_HANDY_TOKEN) fuer seine /geraet/*-Routen.
# Fehlt JARVIS_HANDY_TOKEN, gilt der alte Token ueberall wie bisher. Uebergang: JARVIS_ALT_TOKEN_HANDY=1 laesst an den
# Handy-Routen zusaetzlich den alten Token zu (mit Logzeile), bis das Handy den neuen hat.
TOKEN = os.environ["JARVIS_GERAET_TOKEN"]
HANDY_TOKEN = os.environ.get("JARVIS_HANDY_TOKEN", "").strip()
ALT_TOKEN_HANDY = os.environ.get("JARVIS_ALT_TOKEN_HANDY", "").strip() == "1"
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


def _gleich(wert: str, token: str) -> bool:
    return bool(token) and hmac.compare_digest(wert.encode(), token.encode())


def _token(request: web.Request) -> str:
    return request.headers.get("X-Jarvis-Token", "")


def darf_pc(request: web.Request) -> bool:
    """Rolle „pc+signal“: der bisherige Geraete-Token."""
    return _gleich(_token(request), TOKEN)


_alt_gemeldet = {}  # Pfad -> monotonic der letzten Meldung „alter Token“


def darf_handy(request: web.Request) -> bool:
    """Rolle „handy“. Ohne JARVIS_HANDY_TOKEN wie bisher der alte Token; im Uebergang beide."""
    wert = _token(request)
    if not HANDY_TOKEN:
        return _gleich(wert, TOKEN)
    if _gleich(wert, HANDY_TOKEN):
        return True
    if ALT_TOKEN_HANDY and _gleich(wert, TOKEN):
        jetzt = time.monotonic()
        if jetzt - _alt_gemeldet.get(request.path, -1e9) >= 60:
            _alt_gemeldet[request.path] = jetzt
            log.info("alter Token an %s", request.path)
        return True
    return False


def darf_signal(request: web.Request) -> bool:
    """/geraet/signal: beide Rollen (apk-update und Cloud-Bau mit dem alten Token, das Handy mit seinem)."""
    return darf_pc(request) or darf_handy(request)


class Relay:
    def __init__(self):
        self.geheimnis = ""          # zuletzt vom Handy gemeldetes Geheimnis
        self.gemerkt = {}            # Methode -> result-Objekt
        self.ws = None
        # Signale, die ohne verbundenes Handy kamen (hoechstens 20): gehen beim naechsten Verbinden raus. Eine Liste statt
        # eines Platzes: Zwei apk-update-Laeufe im Funkloch verloren sonst das Signal des ersten.
        self.ausstehend = []
        self.meldungen = []          # Programmier-Meldungen ohne verbundenes Handy (hoechstens 1000, aelteste fallen weg)
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
        geheimnis = request.headers.get("X-Jarvis-Geheimnis", "")
        if not darf_handy(request) or len(geheimnis) < 32:
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
        while self.ausstehend:
            signal = self.ausstehend.pop(0)
            try:
                await ws.send_str(json.dumps(signal))
                log.info("Ausstehendes Signal %s nachgereicht", signal.get("signal"))
            except (ConnectionError, RuntimeError):
                self.ausstehend.insert(0, signal)
                break
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
        if not darf_signal(request):
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
        self.ausstehend.append(daten)
        del self.ausstehend[:-20]
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
            # 1000 statt 100: Eine am PC gestartete Sitzung soll auch nach längerer Funkstille des Handys komplett in Jarvis stehen.
            del self.meldungen[:-1000]
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
            request["rpc"] = "?"
            return self._json({"jsonrpc": "2.0", "id": None, "error": {"code": -32700, "message": "Ungueltiges JSON"}}, 400)
        einzeln = anfrage if isinstance(anfrage, dict) else None
        methode = einzeln.get("method", "") if einzeln else ""
        # Fuer die Mitschrift: erst hier, nach der Pruefung des Geheimnisses (fremde Ruempfe werden nie geparst).
        parameter = einzeln.get("params") if einzeln is not None else None
        name = parameter.get("name", "") if isinstance(parameter, dict) else ""
        request["rpc"] = (str(methode) + (":" + str(name) if methode == "tools/call" else "")) if einzeln is not None else "stapel"

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
        if not darf_handy(request):
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
        if not darf_handy(request):
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
PC_HAUPT_LUECKE_S = 3  # so kurz nach einer Abfrage einer Bruecke gilt sie noch als „holt gleich ab“
PC_WARTEN_S = 25      # eine Abfrage der Bruecke wartet hoechstens so lange auf einen Auftrag
PC_QUITTUNG_S = 45    # so lange wartet das Handy auf die Bestaetigung (die Bruecke braucht mitunter laenger, siehe unten)
# Die Bruecke braucht vor dem Start mitunter laenger (Anmeldepruefung bis 35 s, dann bis 30 s OpenLauncher; am 24.10.2026
# 51 s): Die Quittung eines schon abgeholten Auftrags nimmt der Relay danach noch so lange an und reicht sie dem Handy nach.
PC_NACHQUITTUNG_S = 150


FRAGE_HALTEN_S = 48 * 3600  # so lange bleibt eine Rueckfrage samt Ausgang abrufbar (fuer den Abgleich der App)
ANTWORT_WARTEN_S = 25     # eine Abfrage des Hooks wartet hoechstens so lange auf Franks Antwort
HOOK_FRISCH_S = 90        # so lange nach seiner letzten Abfrage gilt der Frage-Hook als wartend
FRAGEN = ZUSTAND.with_name("fragen.json")
# Ausgang einer Rueckfrage: Danach nimmt der Relay keine Antwort mehr an.
FRAGE_ZU = ("beantwortet", "am_pc", "beantwortet_am_pc", "erledigt", "freigegeben")

NACHTRAG_HALTEN_S = 7 * 86400   # so lange bleibt ein nicht zugestellter Nachtrag eingereiht
NACHTRAG_WARTEN_S = 25          # eine Abfrage des Hooks wartet hoechstens so lange auf einen Nachtrag
NACHTRAG_ZUSTELLEN_S = 8        # so lange wartet das Handy auf die Quittung, wenn die Sitzung gerade abfragt
WARTER_FRISCH_S = 40            # so lange nach seiner letzten Abfrage gilt der Warter einer Sitzung als da
NACHTRAEGE = ZUSTAND.with_name("nachtraege.json")
ENTFERNT = ZUSTAND.with_name("lokal-entfernt.json")
ENTFERNT_HALTEN_S = 30 * 86400  # so lange bleibt eine in Jarvis entfernte lokale Sitzung stumm


class PcBruecke:
    """Programmierauftraege vom Handy an Franks PC (siehe Kopf der Datei)."""

    def __init__(self, relay):
        self.relay = relay
        # frage_id -> {"zeit", "auftrag_id", "frage", "schritt", "status", "antwort", "antwort_id", "antwort_text", "abgefragt"}
        # (Zeiten in Sekunden seit 1970), dauerhaft in fragen.json: Ein Neustart des Relays verliert keine wartende Frage mehr.
        self.fragen = {}
        self.frage_da = {}    # frage_id -> asyncio.Event: Franks Antwort oder Freigabe kam
        self.zuletzt = 0.0
        self.rechner = ""
        # Zwei Bruecken: Die Windows-Bruecke fragt ohne Rang ab („haupt“), die Mac-Bruecke mit X-Jarvis-Rang: ersatz. Welche
        # zuerst drankommt, schickt das Handy mit jedem Auftrag („vorrang“: mac oder windows, Einstellung in Jarvis; Standard
        # seit 25.10.2026 der Mac). Der andere Rechner bekommt Opus-Auftraege nur, solange der Vorrangrechner nicht abfragt.
        # Sol 6.1 bleibt immer beim Windows-PC (nur dort laeuft das Codex CLI). Die Rang-Namen bleiben fuer Bruecken und App.
        self.haupt_zuletzt = 0.0
        self.haupt_rechner = ""
        self.ersatz_zuletzt = 0.0
        self.mac_katalog = False
        self.ersatz_rechner = ""
        self.haupt_abfragen = {}  # id(request) -> request: offene Abfragen des Windows-PCs, nur lebende zaehlen
        self.mac_abfragen = {}    # id(request) -> request: offene Abfragen des Macs, nur lebende zaehlen
        self.zugestellt_an = {}      # Auftrag-id -> Rechner, der ihn abgeholt hat
        # Wartende Auftraege, aelteste zuerst. Eine Liste statt Queue: Jede Bruecke nimmt den aeltesten, der ihr gehoert, und
        # laesst die anderen liegen (kein Herausnehmen und Zuruecklegen: das brachte die Reihenfolge durcheinander und konnte
        # einen Sol- und einen Opus-Auftrag endlos zwischen den Rechnern kreisen lassen).
        self.auftraege = []
        self.neuer_auftrag = asyncio.Event()
        self.quittungen = {}  # Auftrag-id -> Future mit der Quittung der Bruecke
        self.spaete = set()   # laufende Warter auf die spaete Quittung eines schon abgeholten Auftrags
        self.nachtraege = {}  # AUFTRAG-ID -> [{"nachtrag_id", "text", "zeit"}], aelteste zuerst
        self.erledigt = {}    # nachtrag_id -> Zeit der Zustellung (Schutz gegen doppelte Zustellung)
        self.kontakt = {}     # AUFTRAG-ID -> monotonic der letzten Meldung der Sitzung (Schritt, Puls): Zeichen, dass sie arbeitet
        self.warter = {}      # AUFTRAG-ID -> monotonic der letzten Nachtrags-Abfrage des Warters
        self.neu_da = {}      # AUFTRAG-ID -> asyncio.Event: ein Nachtrag kam dazu
        self.bruecke_anmeldung = {}  # Rechner -> {"stand": lokal|abgemeldet|unbekannt, "geprueft": Sekunden seit 1970 (0 = nie)}
        # Rechner -> {"stand": abgelaufen|bestaetigt, "zeit": Sekunden seit 1970, "auftrag_id"}: was die Sitzungen zuletzt bewiesen
        self.sitzung_anmeldung = {}
        self.zugestellt = {}  # nachtrag_id -> asyncio.Event: der Hook hat quittiert
        self.entfernt = {}    # AUFTRAG-ID einer lokalen Sitzung -> Zeit, zu der Frank sie in Jarvis entfernt hat
        self._lade_nachtraege()
        self._lade_fragen()
        try:
            self.entfernt = {k: v for k, v in json.loads(ENTFERNT.read_text("utf-8")).items() if isinstance(v, (int, float))}
        except Exception:
            pass

    # ---------------------------------------------------------------- Lokale Sitzungen

    def _ist_entfernt(self, auftrag_id: str) -> bool:
        return time.time() - self.entfernt.get(auftrag_id, -1e12) < ENTFERNT_HALTEN_S

    async def lokal_entfernen(self, request: web.Request) -> web.Response:
        """POST /geraet/lokal-entfernen (Handy): {"auftrag_id"}. Nur Jarvis vergisst die Sitzung, am Rechner laeuft sie weiter."""
        if not darf_handy(request):
            return web.Response(status=404, text="Not found")
        try:
            auftrag_id = str((await request.json())["auftrag_id"]).strip().upper()
            assert auftrag_id
        except Exception:
            return web.Response(status=400, text="auftrag_id fehlt")
        jetzt = time.time()
        self.entfernt[auftrag_id] = jetzt
        self.entfernt = {k: v for k, v in self.entfernt.items() if jetzt - v < ENTFERNT_HALTEN_S}
        # Offene Nachtraege dieser Sitzung kommen nie mehr an: weg damit; ein wartender Warter erfaehrt es sofort (410).
        if self.nachtraege.pop(auftrag_id, None) is not None:
            self._sichere_nachtraege()
        self._ereignis(auftrag_id).set()
        # Eine offene Rueckfrage dieser Sitzung beantwortet in Jarvis niemand mehr: an den Rechner freigeben, ihr Dialog erscheint dort.
        for frage_id, eintrag in self.fragen.items():
            if eintrag.get("auftrag_id") == auftrag_id and eintrag.get("status") == "offen":
                eintrag["status"] = "freigegeben"
                self._da(frage_id).set()
        self._sichere_fragen()
        try:
            ENTFERNT.parent.mkdir(parents=True, exist_ok=True)
            ENTFERNT.write_text(json.dumps(self.entfernt), "utf-8")
        except Exception as fehler:
            log.warning("Entfernte Sitzungen nicht gesichert: %s", fehler)
        log.info("Lokale Sitzung %s in Jarvis entfernt", auftrag_id)
        return web.json_response({"entfernt": True})

    # ---------------------------------------------------------------- Rueckfragen

    def _lade_fragen(self) -> None:
        try:
            daten = json.loads(FRAGEN.read_text("utf-8"))
            self.fragen = {k: v for k, v in daten.items() if isinstance(v, dict) and isinstance(v.get("zeit"), (int, float))}
        except Exception:
            pass
        # Nach einem Neustart: Ein wartender Hook fragt gleich wieder ab. Bis dahin nicht als verwaist gelten (abgefragt
        # steht nur im Speicher aktuell, in der Datei ist es alt).
        jetzt = time.time()
        for eintrag in self.fragen.values():
            if eintrag.get("status") in ("offen", "angenommen"):
                eintrag["abgefragt"] = jetzt

    def _sichere_fragen(self) -> None:
        grenze = time.time() - FRAGE_HALTEN_S
        self.fragen = dict(sorted(((k, v) for k, v in self.fragen.items() if v.get("zeit", 0) >= grenze), key=lambda e: e[1]["zeit"])[-100:])
        for frage_id in [k for k in self.frage_da if k not in self.fragen]:
            self.frage_da.pop(frage_id, None)
        inhalt = json.dumps(self.fragen, ensure_ascii=False)

        def schreiben():
            try:
                FRAGEN.parent.mkdir(parents=True, exist_ok=True)
                teil = FRAGEN.with_name(FRAGEN.name + ".tmp")
                teil.write_text(inhalt, "utf-8")
                os.replace(teil, FRAGEN)
            except Exception as fehler:
                log.warning("Rueckfragen nicht gesichert: %s", fehler)
        # Abseits der Ereignisschleife: Das Schreiben haelt sonst jede andere Anfrage auf.
        try:
            asyncio.get_running_loop().run_in_executor(None, schreiben)
        except RuntimeError:
            schreiben()

    def _da(self, frage_id: str) -> asyncio.Event:
        return self.frage_da.setdefault(frage_id, asyncio.Event())

    def _wartet_hook(self, eintrag: dict) -> bool:
        # Freie Rueckfrage einer lokalen Sitzung: Niemand fragt die Antwort ab, sie geht als Nachtrag an den Warter.
        if (eintrag.get("frage") or {}).get("als_nachtrag"):
            return self._wartet(str(eintrag.get("auftrag_id", "")))
        return time.time() - eintrag.get("abgefragt", eintrag.get("zeit", 0)) < HOOK_FRISCH_S

    def _frage_merken(self, auftrag_id: str, stand: str, schritt: str, frage: dict) -> None:
        """Neue Rueckfrage (stand frage) oder ihr Ausgang (stand frage_status) vom Hook."""
        frage_id = frage["frage_id"]
        jetzt = time.time()
        if stand == "frage_status":
            eintrag = self.fragen.get(frage_id)
            if eintrag is None:
                return
            status = str(frage.get("status", ""))
            if status in FRAGE_ZU:
                eintrag["status"] = status
                if frage.get("antwort"):
                    eintrag["antwort_text"] = str(frage["antwort"])[:2000]
                if frage.get("grund"):
                    eintrag["grund"] = str(frage["grund"])[:40]
                eintrag["ausgang"] = jetzt
        elif frage_id not in self.fragen:
            # Der Hook wiederholt eine Meldung, wenn die Antwort ausblieb: dieselbe frage_id nie zweimal anlegen.
            nutzlast = {k: v for k, v in frage.items() if k in ("frage_id", "fragen", "frei", "bis", "tool_use_id", "als_nachtrag")}
            self.fragen[frage_id] = {"zeit": jetzt, "abgefragt": jetzt, "auftrag_id": auftrag_id, "frage": nutzlast, "schritt": schritt[:2000],
                                     "status": "offen", "antwort": None, "antwort_id": "", "antwort_text": ""}
        self._sichere_fragen()

    def _frage_fuer_handy(self, frage_id: str, e: dict) -> dict:
        offen = e.get("status") == "offen"
        aus = {"frage_id": frage_id, "auftrag_id": e.get("auftrag_id", ""), "zeit": int(e.get("zeit", 0)), "status": e.get("status", "offen"),
               "wartet": e.get("status") in ("offen", "angenommen") and self._wartet_hook(e),
               "antwort_text": e.get("antwort_text", ""), "grund": e.get("grund", "")}
        # Den Inhalt braucht die App nur, um eine verlorene offene Frage nachzutragen.
        if offen:
            aus.update({"frage": e.get("frage") or {}, "schritt": e.get("schritt", "")})
        return aus

    async def fragen_liste(self, request: web.Request) -> web.Response:
        """GET /geraet/fragen (Handy): alle Rueckfragen der letzten 48 Stunden mit Stand. Die App gleicht damit ab, falls eine
        Meldung ueber die WebSocket verloren ging (halb tote Verbindung, Handy im Ruhezustand)."""
        if not darf_handy(request):
            return web.Response(status=404, text="Not found")
        grenze = time.time() - 24 * 3600
        return web.json_response({"fragen": [self._frage_fuer_handy(k, v) for k, v in self.fragen.items() if v.get("zeit", 0) >= grenze]})

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
        if not darf_handy(request):
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
        if not darf_pc(request):
            return web.Response(status=404, text="Not found")
        auftrag_id = request.query.get("auftrag_id", "").strip().upper()
        if not auftrag_id:
            return web.Response(status=400, text="auftrag_id fehlt")
        if self._ist_entfernt(auftrag_id):
            return web.Response(status=410, text="In Jarvis entfernt")
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
        if warten:
            # Der Warter uebergibt jetzt und endet: Er wartet nicht mehr, bis er (falls nur schon Uebergebenes kam) neu abfragt.
            self.warter.pop(auftrag_id, None)
        return web.json_response({"nachtraege": offen}, dumps=lambda d: json.dumps(d, ensure_ascii=False))

    def da(self) -> bool:
        return time.monotonic() - self.zuletzt < PC_FRISCH_S

    def _haupt_da(self) -> bool:
        return time.monotonic() - self.haupt_zuletzt < PC_FRISCH_S

    def _nimmt(self, mac: bool) -> bool:
        """
        Der Mac (mac=True) bzw. der Windows-PC holt einen Auftrag gleich selbst: Er wartet gerade mit einer lebenden Abfrage,
        oder seine letzte Abfrage endete eben erst (die naechste folgt sofort). Nicht die 45 s von PC_FRISCH_S: Solange wartete
        das Handy schon auf die Quittung, der andere Rechner griffe erst zu, wenn das Handy auf die Cloud ausweicht (zwei Sitzungen).
        """
        abfragen = self.mac_abfragen if mac else self.haupt_abfragen
        if any(a.transport is not None and not a.transport.is_closing() for a in abfragen.values()):
            return True
        return time.monotonic() - (self.ersatz_zuletzt if mac else self.haupt_zuletzt) < PC_HAUPT_LUECKE_S

    def _anmeldung_merken(self, rechner: str, request: web.Request) -> None:
        stand = request.headers.get("X-Jarvis-Anmeldung", "").strip().lower()
        try:
            geprueft = int(request.headers.get("X-Jarvis-Anmeldung-Zeit", "0"))
        except ValueError:
            geprueft = 0
        # Eine aeltere Bruecke ohne Kopfzeile: unbekannt, nie „angemeldet“ (erreichbar heisst nicht angemeldet).
        self.bruecke_anmeldung[rechner] = {"stand": stand if stand in ("lokal", "abgemeldet") else "unbekannt",
                                           "geprueft": geprueft if stand else 0}

    def _rechner_da(self, mac: bool) -> bool:
        return time.monotonic() - (self.ersatz_zuletzt if mac else self.haupt_zuletzt) < PC_FRISCH_S

    def _naechster_ist_mac(self, mac_zuerst: bool) -> bool:
        """Wer einen Opus-Auftrag bekaeme: der Vorrangrechner, wenn er gleich abholt oder der andere nicht da ist."""
        return mac_zuerst if self._nimmt(mac=mac_zuerst) or not self._rechner_da(not mac_zuerst) else not mac_zuerst

    def _darf(self, mac: bool, auftrag: dict) -> bool:
        # Sol 6.1 (Codex CLI) kann nur der Windows-PC. Opus gehoert dem Vorrangrechner; der andere nimmt ihn nur, solange
        # der Vorrangrechner nicht gleich selbst abholt.
        if auftrag.get("modell") == "sol":
            return not mac
        # Ein frei gewaehltes Modell aus dem OpenLauncher-Angebot (Effort, Profil, Modus, CLI) startet nur die Mac-Bruecke:
        # Die Windows-Bruecke kennt diese Felder nicht und startete sonst still Opus.
        if auftrag.get("modell") == "katalog":
            return mac and self.mac_katalog
        mac_zuerst = auftrag.get("vorrang") != "windows"
        return mac == mac_zuerst or not self._nimmt(mac=mac_zuerst)

    def _auftrag_fuer(self, mac: bool) -> dict | None:
        """Der aelteste Auftrag, den diese Bruecke holen darf; verwaiste (das Handy wartet nicht mehr) fallen dabei weg."""
        for auftrag in list(self.auftraege):
            # Ein Auftrag, auf dessen Quittung das Handy nicht mehr wartet, darf nie mehr starten (sonst doppelt mit der Cloud).
            wartend = self.quittungen.get(auftrag["id"])
            if wartend is None or wartend.done():
                self.auftraege.remove(auftrag)
            elif self._darf(mac, auftrag):
                return auftrag
        return None

    def _melde_dich(self, ersatz: bool, rechner: str, request: web.Request | None = None) -> None:
        if request is not None:
            self._anmeldung_merken(rechner, request)
        jetzt = time.monotonic()
        self.zuletzt = jetzt
        if ersatz:
            self.ersatz_zuletzt, self.ersatz_rechner = jetzt, rechner
        else:
            self.haupt_zuletzt, self.haupt_rechner = jetzt, rechner

    async def abholen(self, request: web.Request) -> web.Response:
        """GET /pc/auftrag (Bruecke): wartet bis zu PC_WARTEN_S auf einen Auftrag, sonst 204."""
        if not darf_pc(request):
            return web.Response(status=404, text="Not found")
        if not self.da():
            log.info("PC-Bruecke verbunden")
        ersatz = request.headers.get("X-Jarvis-Rang", "").strip().lower() == "ersatz"
        rechner = request.headers.get("X-Jarvis-Rechner", "PC")[:60]
        if ersatz:
            # Nur eine Mac-Bruecke, die modell=katalog kennt, bekommt solche Auftraege (eine aeltere startete still Opus).
            self.mac_katalog = request.headers.get("X-Jarvis-Katalog") == "1"
        self._melde_dich(ersatz, rechner, request)
        abfragen = self.mac_abfragen if ersatz else self.haupt_abfragen
        abfragen[id(request)] = request
        try:
            return await self._warte_auf_auftrag(request, ersatz, rechner)
        finally:
            abfragen.pop(id(request), None)

    async def _warte_auf_auftrag(self, request: web.Request, ersatz: bool, rechner: str) -> web.Response:
        frist = asyncio.get_running_loop().time() + PC_WARTEN_S
        while True:
            rest = frist - asyncio.get_running_loop().time()
            if rest <= 0:
                self._melde_dich(ersatz, rechner)
                return web.Response(status=204)
            auftrag = self._auftrag_fuer(ersatz)
            if auftrag is None:
                # In kurzen Stuecken warten: So merkt der zweite Rechner binnen Sekunden, wenn der erste wegfaellt.
                self.neuer_auftrag.clear()
                try:
                    await asyncio.wait_for(self.neuer_auftrag.wait(), min(rest, 1))
                except asyncio.TimeoutError:
                    pass
                continue
            self._melde_dich(ersatz, rechner)
            # Ist die Abfrage der Bruecke inzwischen abgerissen (Ruhezustand, Netzwechsel), ginge der Auftrag ins Leere:
            # liegen lassen, die naechste Abfrage holt ihn.
            if request.transport is None or request.transport.is_closing():
                return web.Response(status=204)
            self.auftraege.remove(auftrag)
            self.rechner = rechner
            self.zugestellt_an[auftrag["id"]] = rechner
            return web.json_response(auftrag)

    async def quittung(self, request: web.Request) -> web.Response:
        """POST /pc/quittung (Bruecke): {"id", "ok", "text"}."""
        if not darf_pc(request):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            wartend = self.quittungen.get(daten["id"])
        except Exception:
            return web.Response(status=400, text="Quittung unlesbar")
        if wartend is not None and not wartend.done():
            wartend.set_result(daten)
        # Die Quittung kommt nach der frischen Pruefung vor dem Start: nur den Anmeldestand merken (die Erreichbarkeit
        # zaehlt allein ueber die Abfragen, sonst gaelte der Hauptrechner kurz als „holt gleich ab“).
        rechner = request.headers.get("X-Jarvis-Rechner", "")[:60]
        if rechner and request.headers.get("X-Jarvis-Anmeldung"):
            self._anmeldung_merken(rechner, request)
        return web.json_response({"angenommen": wartend is not None})

    async def auftrag(self, request: web.Request) -> web.Response:
        """POST /geraet/pc-auftrag (Handy): {"app", "auftrag", "review", "modell"?, "cli"?, bei modell=katalog auch "slug", "anbieter",
        "name", "effort", "profil", "modus"}. 409 = kein PC da, 504 = keine Quittung."""
        if not darf_handy(request):
            return web.Response(status=404, text="Not found")
        if not self.da():
            return web.json_response({"pc": False}, status=409)
        try:
            daten = await request.json()
            assert isinstance(daten.get("app"), str) and isinstance(daten.get("auftrag"), str) and daten["auftrag"].strip()
        except Exception:
            return web.Response(status=400, text="Auftrag unlesbar")
        # Sol nur mit dem Windows-PC (Hauptrechner); fragt nur der Mac ab, ist fuer Sol kein Rechner da.
        if daten.get("modell") == "sol" and not self._haupt_da():
            return web.json_response({"pc": False}, status=409)
        if daten.get("modell") == "katalog" and not (self._rechner_da(mac=True) and self.mac_katalog):
            return web.json_response({"pc": False, "mac": False}, status=409)
        nummer = uuid.uuid4().hex
        jarvis_id = str(daten.get("nummer", ""))[:20]
        wartend = asyncio.get_running_loop().create_future()
        self.quittungen[nummer] = wartend
        spaeter = False
        try:
            # modell: Franks Wahl beim Start (opus = Claude Code wie bisher, sol = Sol 6.1); alles andere gilt als opus.
            # cli: womit Sol laeuft (codex = Codex CLI wie bisher, opencode = OpenCode CLI); alles andere gilt als codex.
            # katalog: ein Modell aus dem OpenLauncher-Angebot mit slug, anbieter, effort, profil, modus und cli
            # (claude, codex, opencode, kimi, antigravity); nur der Mac startet es.
            katalog = daten.get("modell") == "katalog"
            eintrag = {"id": nummer, "app": daten["app"], "auftrag": daten["auftrag"], "review": bool(daten.get("review")),
                       "nummer": jarvis_id, "modell": "katalog" if katalog else "sol" if daten.get("modell") == "sol" else "opus",
                       "cli": "opencode" if daten.get("cli") == "opencode" else "codex",
                       "vorrang": "windows" if daten.get("vorrang") == "windows" else "mac"}
            if katalog:
                for feld in ("slug", "anbieter", "name", "effort", "profil", "modus"):
                    eintrag[feld] = str(daten.get(feld, ""))[:80]
                eintrag["cli"] = daten.get("cli") if daten.get("cli") in ("claude", "codex", "opencode", "kimi", "antigravity") else "claude"
            self.auftraege.append(eintrag)
            self.neuer_auftrag.set()
            log.info("Programmierauftrag an den PC: %s", daten["app"][:40])
            # shield: wait_for braeche die Future sonst ab, und eine spaete Quittung kaeme nie mehr an.
            quittung = await asyncio.wait_for(asyncio.shield(wartend), PC_QUITTUNG_S)
        except asyncio.TimeoutError:
            if nummer in self.zugestellt_an:
                # Abgeholt, nur noch nicht quittiert: Die Bruecke startet die Sitzung womoeglich gerade. Nie „nicht gestartet“
                # (sonst sendet Frank neu oder das Handy nimmt die Cloud: zwei Sitzungen). ok=true haelt auch ein aelteres
                # Handy von der Cloud ab; das neue liest „unbestaetigt“. Die spaete Quittung geht als Meldung hinterher.
                spaeter = True
                rechner = self.zugestellt_an[nummer]
                log.info("PC hat den Auftrag abgeholt, Quittung steht noch aus")
                aufgabe = asyncio.create_task(self._spaete_quittung(nummer, jarvis_id, wartend))
                self.spaete.add(aufgabe)
                aufgabe.add_done_callback(self.spaete.discard)
                return web.json_response({"pc": True, "ok": True, "unbestaetigt": True, "rechner": rechner,
                                          "text": "Der PC hat den Auftrag abgeholt, die Startbestaetigung steht noch aus."}, status=202)
            # Nie abgeholt: Er startet auch nie mehr (_warte_auf_auftrag ueberspringt ihn ohne wartende Future).
            log.info("PC hat den Auftrag nicht abgeholt")
            return web.json_response({"pc": True, "ok": False, "fehler": "Der PC hat den Auftrag nicht abgeholt."}, status=504)
        finally:
            if not spaeter:
                rechner = self._quittung_abschliessen(nummer, wartend)
        return web.json_response({"pc": True, "ok": bool(quittung.get("ok")), "text": str(quittung.get("text", ""))[:500], "rechner": rechner,
                                  "anmeldung_fehlt": bool(quittung.get("anmeldung_fehlt"))})

    def _quittung_abschliessen(self, nummer: str, wartend: asyncio.Future) -> str:
        """Raeumt die Quittung eines Auftrags weg und nennt den Rechner, der ihn abgeholt hat."""
        wartend.cancel()
        self.quittungen.pop(nummer, None)
        return self.zugestellt_an.pop(nummer, self.rechner)

    async def _spaete_quittung(self, nummer: str, jarvis_id: str, wartend: asyncio.Future) -> None:
        """
        Quittung eines abgeholten Auftrags nach PC_QUITTUNG_S: Das Handy fuehrt ihn als „Start unbestaetigt“; die Quittung
        geht als Programmier-Meldung hinterher. „spaet“: Das Handy wendet sie nur an, solange der Eintrag noch unbestaetigt
        ist (hat sich die Sitzung inzwischen selbst gemeldet, gilt ihr Stand). Bleibt die Quittung ganz aus und meldet sich
        auch die Sitzung nicht, endet „unbestaetigt“ nach PC_NACHQUITTUNG_S mit einem Hinweis, statt fuer immer zu bleiben.
        """
        try:
            quittung = await asyncio.wait_for(asyncio.shield(wartend), PC_NACHQUITTUNG_S)
        except asyncio.TimeoutError:
            quittung = None
        finally:
            rechner = self._quittung_abschliessen(nummer, wartend) or "Der PC"
        auftrag_id = jarvis_id.strip().upper()
        if not auftrag_id or self._ist_entfernt(auftrag_id):
            return
        if quittung is None:
            log.info("Auch spaet keine Quittung vom PC")
            meldung = {"stand": "fehler", "schritt": f"{rechner} hat den Start auch nach über drei Minuten nicht bestätigt, und die Sitzung "
                                                      "hat sich nicht gemeldet: vermutlich nicht gestartet. Bitte am PC nachsehen, bevor du neu startest."}
        else:
            log.info("Spaete Quittung vom PC: ok=%s", bool(quittung.get("ok")))
            text = str(quittung.get("text", ""))[:300]
            if quittung.get("anmeldung_fehlt"):
                # Keine Sitzung, also auch keine Sperre, die nach /login von selbst weiterliefe: nicht gestartet.
                meldung = {"stand": "fehler", "schritt": "Doch nicht gestartet: " + (text or "Claude Code auf dem Rechner ist nicht angemeldet.")}
            elif not quittung.get("ok"):
                meldung = {"stand": "fehler", "schritt": "Doch nicht gestartet: " + (text or "Der Rechner konnte die Sitzung nicht starten.")}
            else:
                meldung = {"stand": "laeuft", "schritt": f"{rechner} hat den Start nachträglich bestätigt, der Auftrag läuft."}
            # Das Handy legt den Eintrag erst beim Lesen der 202-Antwort an: Vorsprung lassen (Mobilfunk, neues Gespraech).
            await asyncio.sleep(5)
        await self.relay.programmier_meldung({"signal": "programmieren", "auftrag_id": auftrag_id, "spaet": True, "von": "jarvis", **meldung})

    async def meldung(self, request: web.Request) -> web.Response:
        """POST /pc/meldung (Hook am PC): {"auftrag_id", "stand", "schritt", "frage"?: {"frage_id", "fragen", "frei"}}."""
        if not darf_pc(request):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            assert isinstance(daten.get("auftrag_id"), str) and daten["auftrag_id"] and isinstance(daten.get("schritt"), str)
        except Exception:
            return web.Response(status=400, text="Meldung unlesbar")
        auftrag_id = daten["auftrag_id"].strip().upper()
        if self._ist_entfernt(auftrag_id):
            # In Jarvis entfernt: nicht mehr ans Handy (sonst erschiene sie sofort wieder); der Hook schweigt danach.
            return web.json_response({"zugestellt": False, "entfernt": True})
        self.kontakt[auftrag_id] = time.monotonic()
        self._anmeldung_aus_meldung(auftrag_id, daten)
        if daten.get("stand") == "nachtrag" and isinstance(daten.get("nachtrag_ids"), list):
            self._quittiert(auftrag_id, daten["nachtrag_ids"])
        frage = daten.get("frage")
        if isinstance(frage, dict) and isinstance(frage.get("frage_id"), str) and frage["frage_id"] and daten.get("stand") in ("frage", "frage_status"):
            self._frage_merken(auftrag_id, daten["stand"], daten["schritt"], frage)
        zugestellt = await self.relay.programmier_meldung({"signal": "programmieren", **daten})
        return web.json_response({"zugestellt": zugestellt})

    # Diese Staende kommen nur, wenn die Sitzung nach einer Antwort des Modells arbeitet: Beweis einer gueltigen Anmeldung.
    BEWEIS = ("puls", "laeuft", "info", "frage", "fertig", "anmeldung_ok", "weiter")

    def _anmeldung_aus_meldung(self, auftrag_id: str, daten: dict) -> None:
        rechner = str(daten.get("rechner") or "")[:60]
        if not rechner:
            return
        stand = daten.get("stand")
        bisher = self.sitzung_anmeldung.get(rechner) or {}
        if stand == "anmeldung":
            self.sitzung_anmeldung[rechner] = {"stand": "abgelaufen", "zeit": int(daten.get("seit") or time.time()), "auftrag_id": auftrag_id}
        elif stand in self.BEWEIS:
            # Ein Hook, der vor dem Fehler begann (async, spaet dran), beweist nichts.
            beginn = int(daten.get("hook_beginn") or 0)
            if bisher.get("stand") == "abgelaufen" and beginn <= bisher.get("zeit", 0):
                return
            self.sitzung_anmeldung[rechner] = {"stand": "bestaetigt", "zeit": int(time.time()), "auftrag_id": auftrag_id}

    async def antwort_abholen(self, request: web.Request) -> web.Response:
        """GET /pc/antwort?frage_id=… (Hook): 200 mit Antwort (oder {"freigeben": true}: Frank will am Rechner antworten),
        204 = noch keine, 404 = Frage unbekannt oder verfallen, 410 = schon anders ausgegangen."""
        if not darf_pc(request):
            return web.Response(status=404, text="Not found")
        frage_id = request.query.get("frage_id", "")
        eintrag = self.fragen.get(frage_id)
        if eintrag is None:
            return web.Response(status=404, text="Frage unbekannt")
        eintrag["abgefragt"] = time.time()
        if eintrag.get("status") == "freigegeben":
            return web.json_response({"freigeben": True})
        if eintrag.get("antwort") is None and eintrag.get("status") in FRAGE_ZU:
            return web.Response(status=410, text="Frage schon ausgegangen")
        if eintrag.get("antwort") is None:
            try:
                await asyncio.wait_for(self._da(frage_id).wait(), ANTWORT_WARTEN_S)
            except asyncio.TimeoutError:
                return web.Response(status=204)
            eintrag = self.fragen.get(frage_id) or eintrag
            if eintrag.get("status") == "freigegeben":
                return web.json_response({"freigeben": True})
        # Der Hook hat die Antwort: beantwortet, auch wenn seine eigene Bestaetigung (frage_status) danach verloren ginge.
        if eintrag.get("status") == "angenommen":
            eintrag["status"] = "beantwortet"
            eintrag["ausgang"] = time.time()
            self._sichere_fragen()
        return web.json_response(eintrag["antwort"])

    async def antwort_geben(self, request: web.Request) -> web.Response:
        """POST /geraet/antwort (Handy): {"frage_id", "antworten": {Frage: Antwort}, "text"?, "antwort_id"?, "freigeben"?}.
        Dieselbe antwort_id noch einmal (Antwort kam beim Handy nicht an) gilt als angenommen, nie doppelt."""
        if not darf_handy(request):
            return web.Response(status=404, text="Not found")
        try:
            daten = await request.json()
            frage_id = str(daten["frage_id"])
            eintrag = self.fragen[frage_id]
        except Exception:
            return web.json_response({"angenommen": False, "status": "unbekannt", "grund": "Die Rückfrage ist nicht mehr offen."}, status=404)
        antwort_id = str(daten.get("antwort_id", ""))[:64]
        if antwort_id and antwort_id == eintrag.get("antwort_id"):
            return web.json_response({"angenommen": True, "status": eintrag.get("status"), "wiederholt": True})
        status = eintrag.get("status", "offen")
        if eintrag.get("antwort") is not None or status != "offen":
            grund = {
                "beantwortet": "Die Rückfrage ist schon beantwortet",
                "angenommen": "Die Rückfrage ist schon beantwortet",
                "beantwortet_am_pc": "Die Rückfrage wurde schon am Rechner beantwortet",
                "am_pc": "Die Rückfrage liegt jetzt am Rechner und in der Claude-App; dort beantworten",
                "freigegeben": "Die Rückfrage liegt jetzt am Rechner und in der Claude-App; dort beantworten",
                "erledigt": "Die Sitzung hat ohne diese Antwort weitergearbeitet",
            }.get(status, "Die Rückfrage ist nicht mehr offen")
            text = eintrag.get("antwort_text", "")
            return web.json_response({"angenommen": False, "status": status, "antwort_text": text,
                                      "grund": grund + (f": {text}" if text else ".")}, status=409)
        if not self._wartet_hook(eintrag):
            return web.json_response({"angenommen": False, "status": "verwaist",
                                      "grund": "Die Sitzung wartet nicht mehr auf diese Antwort (vermutlich geschlossen oder am Rechner weiter)."}, status=410)
        eintrag["antwort_id"] = antwort_id
        if daten.get("freigeben"):
            eintrag["status"] = "freigegeben"
            log.info("Rueckfrage an den Rechner freigegeben")
        else:
            eintrag["antwort"] = {"antworten": daten.get("antworten") or {}, "text": str(daten.get("text", ""))[:4000]}
            eintrag["status"] = "angenommen"
            werte = [str(v) for v in (daten.get("antworten") or {}).values() if str(v).strip()]
            eintrag["antwort_text"] = ("; ".join(werte) or str(daten.get("text", "")))[:2000]
            log.info("Rueckfrage beantwortet")
            if (eintrag.get("frage") or {}).get("als_nachtrag"):
                # Lokale Sitzung: Die Antwort geht als Nachtrag in die Sitzung; der Warter weckt sie damit.
                auftrag_id = str(eintrag.get("auftrag_id", ""))
                nachtrag_id = f"antwort-{frage_id}"
                liste = self.nachtraege.setdefault(auftrag_id, [])
                if nachtrag_id not in self.erledigt and not any(n.get("nachtrag_id") == nachtrag_id for n in liste):
                    liste.append({"nachtrag_id": nachtrag_id, "zeit": time.time(),
                                  "text": f"Antwort von Frank (über Jarvis) auf deine letzte Frage: {eintrag['antwort_text']}"})
                    self._sichere_nachtraege()
                self._ereignis(auftrag_id).set()
        self._sichere_fragen()
        self._da(frage_id).set()
        return web.json_response({"angenommen": True, "status": eintrag["status"]})

    async def status(self, request: web.Request) -> web.Response:
        if not darf_handy(request):
            return web.Response(status=404, text="Not found")
        jetzt = time.monotonic()
        vor = int(jetzt - self.zuletzt) if self.zuletzt else None
        rechner = []
        for rang, zuletzt, name in (("haupt", self.haupt_zuletzt, self.haupt_rechner), ("ersatz", self.ersatz_zuletzt, self.ersatz_rechner)):
            if zuletzt:
                rechner.append({"rechner": name, "rang": rang, "da": jetzt - zuletzt < PC_FRISCH_S, "zuletzt_vor_s": int(jetzt - zuletzt)})
        # „rechner“ bleibt fuer die App: der Rechner, der den naechsten Auftrag bekaeme.
        # ?vorrang=windows: Franks Reihenfolge aus der App (Standard Mac zuerst).
        aktiv = self.ersatz_rechner if self._naechster_ist_mac(request.query.get("vorrang") != "windows") else self.haupt_rechner
        jetzt_s = time.time()
        for r in rechner:
            b = self.bruecke_anmeldung.get(r["rechner"]) or {"stand": "unbekannt", "geprueft": 0}
            r["anmeldung_bruecke"] = {"stand": b["stand"], "geprueft_vor_s": int(jetzt_s - b["geprueft"]) if b["geprueft"] else None}
            s = self.sitzung_anmeldung.get(r["rechner"])
            r["anmeldung_sitzung"] = {"stand": s["stand"], "vor_s": int(jetzt_s - s["zeit"]), "auftrag_id": s["auftrag_id"]} if s else None
        # Je Auftrag (der letzten 24 h): letzte Meldung oder Puls (Arbeit) und letzte Nachtrags-Abfrage des Warters (Sitzung lebt).
        sitzungen = {}
        for auftrag_id in set(self.kontakt) | set(self.warter):
            kontakt, warter = self.kontakt.get(auftrag_id), self.warter.get(auftrag_id)
            if max(kontakt or 0, warter or 0) > jetzt - 86_400:
                sitzungen[auftrag_id] = {"kontakt_vor_s": int(jetzt - kontakt) if kontakt else None,
                                         "warter_vor_s": int(jetzt - warter) if warter else None, "lebt": self._wartet(auftrag_id)}
        return web.json_response({"pc": self.da(), "rechner": aktiv, "zuletzt_vor_s": vor, "rechner_alle": rechner, "sitzungen": sitzungen})


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
            # Die RPC-Methode setzt der MCP-Handler erst nach gelungener Pruefung des Geheimnisses (sonst leer).
            rpc = request.get("rpc", "")
            log.info("%s %s %s -> %s (%s ms) ua=%s accept=%s", request.method, pfad, rpc, status, dauer,
                     request.headers.get("User-Agent", "")[:60], request.headers.get("Accept", "")[:60])


def app() -> web.Application:
    if not HANDY_TOKEN:
        log.warning("JARVIS_HANDY_TOKEN fehlt: Der Geraete-Token gilt wie bisher fuer alle Routen (auch die des Handys).")
    elif ALT_TOKEN_HANDY:
        log.warning("Uebergang aktiv (JARVIS_ALT_TOKEN_HANDY=1): Die Handy-Routen nehmen auch den alten Geraete-Token an.")
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
    anwendung.router.add_get("/geraet/fragen", pc.fragen_liste)
    anwendung.router.add_post("/geraet/nachtrag", pc.nachtrag_geben)
    anwendung.router.add_get("/pc/nachtrag", pc.nachtrag_abholen)
    anwendung.router.add_post("/geraet/lokal-entfernen", pc.lokal_entfernen)
    anwendung.router.add_route("*", "/j/{geheimnis}/mcp", relay.mcp)
    anwendung.router.add_get("/gesund", gesund)
    anwendung.router.add_route("*", "/{rest:.*}", unbekannt)
    return anwendung


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    web.run_app(app(), host="0.0.0.0", port=8080, access_log=None)
