"""Server-Konsole: Sitzungen sehen und schließen, die laufende CLI bedienen, Modelle starten. Nur im WireGuard-Tunnel (10.8.0.1:8090).

Anmeldung mit dem Fernsteuerungs-Token (~/SK/Jarvis/fernsteuerung-token.txt), den Jarvis auf dem Handy schon hat. Das Terminal hängt per
pty an `tmux attach`; die Sitzung selbst läuft im Dienst jarvis-tmux weiter, auch wenn diese Konsole neu startet.
"""
from __future__ import annotations

import asyncio
import fcntl
import hashlib
import hmac
import json
import os
import pty
import re
import signal
import struct
import subprocess
import sys
import termios
import time
from pathlib import Path

from aiohttp import WSMsgType, web

HIER = Path(__file__).resolve().parent
sys.path.insert(0, str(HIER.parent / "kern"))
import clis  # noqa: E402
import launcher  # noqa: E402

ADRESSE = os.environ.get("KONSOLE_ADRESSE", "10.8.0.1")
PORT = int(os.environ.get("KONSOLE_PORT", "8090"))
TOKEN = (Path.home() / "SK" / "Jarvis" / "fernsteuerung-token.txt").read_text("utf-8").strip()
COOKIE = "konsole"
# Das Cookie ist nicht der Token selbst: Derselbe Token öffnet auch die Bildschirm-Fernsteuerung der Rechner.
COOKIE_WERT = hmac.new(TOKEN.encode(), b"server-konsole", hashlib.sha256).hexdigest()
FEHLVERSUCHE = {"zahl": 0}
LINK = re.compile(r"https?://[^\s\"'<>]+")
ANMELDEN_SEITE = """<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Server-Konsole</title><body style="background:#10131a;color:#e6e9ef;font:16px system-ui;padding:24px">
<form method="post" action="/anmelden"><p>Zugangstoken</p><input name="token" type="password" autofocus style="width:100%;padding:12px;font-size:16px">
<p><button style="padding:12px 20px;font-size:16px">Anmelden</button></p></form></body></html>"""


def _gleich(wert: str) -> bool:
    return bool(TOKEN) and hmac.compare_digest(wert.encode(), TOKEN.encode())


def _angemeldet(request: web.Request) -> bool:
    return hmac.compare_digest(request.cookies.get(COOKIE, "").encode(), COOKIE_WERT.encode())


def _mit_cookie(antwort: web.Response) -> web.Response:
    antwort.set_cookie(COOKIE, COOKIE_WERT, httponly=True, samesite="Strict", max_age=365 * 86400)
    return antwort


@web.middleware
async def schutz(request: web.Request, handler):
    if request.path in ("/ping", "/anmelden"):
        return await handler(request)
    if not _angemeldet(request):
        if request.path == "/":
            return web.Response(text=ANMELDEN_SEITE, content_type="text/html")
        return web.json_response({"fehler": "nicht angemeldet"}, status=401)
    # Ein fremdes Origin darf nichts auslösen, auch nicht mit dem Cookie im selben Browser.
    herkunft = request.headers.get("Origin", "")
    if herkunft and herkunft.split("://", 1)[-1] != request.host:
        return web.json_response({"fehler": "fremde Herkunft"}, status=403)
    try:
        return await handler(request)
    except (ValueError, KeyError, TypeError) as fehler:
        # Unlesbare Anfrage oder ungültige Wahl: verständlich an die Seite, nicht als HTTP 500.
        return web.json_response({"fehler": str(fehler) or "Ungültige Anfrage"}, status=400)


async def ping(_: web.Request) -> web.Response:
    return web.Response(text="ok")


async def anmelden(request: web.Request) -> web.Response:
    daten = await request.post()
    if _gleich(str(daten.get("token", "")).strip()):
        FEHLVERSUCHE["zahl"] = 0
        raise _mit_cookie(web.HTTPFound("/"))
    # Raten kostet Zeit: bis zu 30 s je Versuch, auch im Tunnel.
    FEHLVERSUCHE["zahl"] += 1
    await asyncio.sleep(min(FEHLVERSUCHE["zahl"], 30))
    return web.Response(text=ANMELDEN_SEITE, content_type="text/html", status=403)


async def startseite(_: web.Request) -> web.FileResponse:
    return web.FileResponse(HIER / "static" / "index.html")


async def _im_faden(funktion, *argumente):
    return await asyncio.get_running_loop().run_in_executor(None, funktion, *argumente)


async def sitzungen(_: web.Request) -> web.Response:
    alle = await _im_faden(launcher.sitzungen)
    return web.json_response({"sitzungen": alle, "belegt": launcher.belegt(alle)})


async def schliessen(request: web.Request) -> web.Response:
    daten = await request.json()
    await _im_faden(launcher.schliessen, str(daten.get("name", "")))
    return web.json_response({"ok": True})


async def katalog(_: web.Request) -> web.Response:
    return web.json_response(await _im_faden(launcher.katalog))


async def starten(request: web.Request) -> web.Response:
    d = await request.json()
    try:
        name = await _im_faden(lambda: launcher.starten(str(d.get("cli")), str(d.get("modell")), str(d.get("anbieter", "")), str(d.get("effort", "")),
                                                        str(d.get("profil", "minimal")), str(d.get("modus", "schnell")), str(d.get("titel", "")), str(d.get("prompt", ""))))
    except (ValueError, RuntimeError) as fehler:
        return web.json_response({"fehler": str(fehler)}, status=400)
    return web.json_response({"name": name})


async def shell(request: web.Request) -> web.Response:
    d = await request.json()
    art = "pwsh" if d.get("art") == "pwsh" else "bash"
    return web.json_response({"name": await _im_faden(launcher.befehl_starten, f"Terminal {art}", f"{art} -l" if art == "bash" else "pwsh")})


STATUS_AUFFRISCHEN = {"zeit": 0.0, "laeuft": False}


async def _status_auffrischen() -> None:
    """Anmeldestände frisch prüfen, höchstens alle 20 s: Sonst zeigte der Reiter bis zum nächsten Update-Lauf „nicht angemeldet“, obwohl der Login gelang."""
    try:
        await _im_faden(clis.status_schreiben, False)
    finally:
        STATUS_AUFFRISCHEN.update(zeit=time.monotonic(), laeuft=False)


async def system(request: web.Request) -> web.Response:
    if not STATUS_AUFFRISCHEN["laeuft"] and time.monotonic() - STATUS_AUFFRISCHEN["zeit"] > 20:
        STATUS_AUFFRISCHEN["laeuft"] = True
        request.app["aufgaben"].add(asyncio.create_task(_status_auffrischen()))
        request.app["aufgaben"] = {a for a in request.app["aufgaben"] if not a.done()}

    def lesen():
        try:
            daten = json.loads(clis.STATUS.read_text("utf-8"))
        except (OSError, ValueError):
            daten = {"programme": {}}
        speicher = subprocess.run(["free", "-m"], capture_output=True, text=True, timeout=5).stdout.splitlines()
        wert = speicher[1].split() if len(speicher) > 1 else []
        daten["speicher"] = {"gesamt_mb": int(wert[1]), "frei_mb": int(wert[6])} if len(wert) > 6 else {}
        try:
            daten["repo"] = json.loads((launcher.ZUSTAND / "repo-status.json").read_text("utf-8"))
        except (OSError, ValueError):
            daten["repo"] = {}
        try:
            daten["version"] = (HIER.parent / "VERSION").read_text("utf-8").strip()
        except OSError:
            daten["version"] = ""
        return daten
    return web.json_response(await _im_faden(lesen))


async def aktualisieren(_: web.Request) -> web.Response:
    # Eigene Sitzung statt Hintergrund: Frank sieht den Lauf im Terminal und die Status-Datei ändert sich danach.
    skript = HIER.parent / "kern" / "clis.py"
    return web.json_response({"name": await _im_faden(launcher.befehl_starten, "CLI-Updates", f"python3 {skript} aktualisieren")})


async def anmeldung(request: web.Request) -> web.Response:
    d = await request.json()
    p = clis.PROGRAMME.get(str(d.get("cli")))
    if not p:
        return web.json_response({"fehler": "Unbekanntes CLI"}, status=400)
    umgebung = f"export CLAUDE_CONFIG_DIR={launcher.claude_ordner('minimal')} CODEX_HOME={launcher.codex_zuhause()}\n"
    if d.get("cli") == "claude":
        await _im_faden(launcher.claude_vorbereiten, "minimal", "schnell")
    return web.json_response({"name": await _im_faden(launcher.befehl_starten, f"Anmeldung {p['name']}", umgebung + p["anmeldung"], str(d.get("cli")))})


async def links(request: web.Request) -> web.Response:
    """Adressen aus der Terminalausgabe: Im Handy-Terminal bricht eine lange Anmelde-Adresse um und lässt sich nicht antippen."""
    name = request.match_info["name"]
    launcher.pruefe_name(name)
    aus = await _im_faden(lambda: subprocess.run(launcher.TMUX + ["capture-pane", "-p", "-J", "-S", "-200", "-t", f"={name}:"],
                                                 capture_output=True, text=True, timeout=5).stdout)
    einmal = list(dict.fromkeys(g.rstrip(".,;") for g in LINK.findall(aus) if len(g) > 12))
    return web.json_response({"links": einmal[-5:]})


async def terminal(request: web.Request) -> web.WebSocketResponse:
    name = request.match_info["name"]
    ws = web.WebSocketResponse(heartbeat=25, max_msg_size=4 * 1024 * 1024)
    await ws.prepare(request)
    # Die Sitzung gibt es nicht (mehr): 4404 sagt der Seite, dass neu Verbinden nichts bringt.
    if not launcher.NAME.fullmatch(name) or await _im_faden(lambda: subprocess.run(launcher.TMUX + ["has-session", "-t", f"={name}"], capture_output=True, timeout=5).returncode) != 0:
        await ws.close(code=4404)
        return ws
    herr, knecht = pty.openpty()
    umgebung = dict(os.environ, TERM="xterm-256color", LANG="C.UTF-8", LC_ALL="C.UTF-8")
    # Der pty muss das steuernde Terminal von tmux sein: Nur dann kommt beim Falten und Drehen das Signal für die neue Größe an.
    prozess = subprocess.Popen(launcher.TMUX + ["attach-session", "-t", f"={name}"], stdin=knecht, stdout=knecht, stderr=knecht,
                               start_new_session=True, env=umgebung, close_fds=True,
                               preexec_fn=lambda: fcntl.ioctl(0, termios.TIOCSCTTY, 0))
    os.close(knecht)
    schleife = asyncio.get_running_loop()
    os.set_blocking(herr, False)
    warteschlange: asyncio.Queue = asyncio.Queue()
    angehalten = [False]

    def lesbar():
        try:
            daten = os.read(herr, 65536)
        except BlockingIOError:
            return
        except OSError:
            daten = b""
        if not daten:
            schleife.remove_reader(herr)
        warteschlange.put_nowait(daten)
        # Der Empfänger kommt nicht nach (Mobilfunk): nicht mehr lesen statt Bytes zu verwerfen, tmux wartet dann.
        if daten and warteschlange.qsize() > 64:
            schleife.remove_reader(herr)
            angehalten[0] = True

    schleife.add_reader(herr, lesbar)

    async def senden():
        while True:
            daten = await warteschlange.get()
            if not daten:
                await ws.close()
                return
            await ws.send_bytes(daten)
            if angehalten[0] and warteschlange.qsize() < 16:
                angehalten[0] = False
                schleife.add_reader(herr, lesbar)

    async def schreiben(daten: bytes):
        # Der pty ist nichtblockierend: Lange Eingaben gehen nur in Teilen hinein.
        while daten:
            try:
                daten = daten[os.write(herr, daten):]
            except BlockingIOError:
                await asyncio.sleep(0.01)

    sender = asyncio.create_task(senden())
    try:
        async for nachricht in ws:
            if nachricht.type != WSMsgType.TEXT:
                continue
            d = json.loads(nachricht.data)
            if d.get("t") == "i":
                await schreiben(str(d.get("d", "")).encode("utf-8"))
            elif d.get("t") == "r":
                fcntl.ioctl(herr, termios.TIOCSWINSZ, struct.pack("HHHH", max(int(d["r"]), 2), max(int(d["c"]), 2), 0, 0))
    finally:
        sender.cancel()
        schleife.remove_reader(herr)
        # Nur die Anzeige trennen: Die CLI läuft in tmux weiter.
        try:
            os.killpg(prozess.pid, signal.SIGHUP)
        except ProcessLookupError:
            pass
        os.close(herr)
        await _im_faden(prozess.wait)
    return ws


async def neu_laden_bei_aenderung(_: web.Application):
    """Neue Fassung aus dem Repo: beenden, systemd startet neu."""
    dateien = [Path(__file__), Path(launcher.__file__), Path(clis.__file__)]
    stand = [d.stat().st_mtime for d in dateien]

    async def wache():
        while True:
            await asyncio.sleep(30)
            if [d.stat().st_mtime for d in dateien] != stand:
                os._exit(0)

    aufgabe = asyncio.create_task(wache())
    yield
    aufgabe.cancel()


async def ohne_zwischenspeicher(_: web.Request, antwort: web.StreamResponse) -> None:
    # Nach einem Vorspulen darf kein altes JavaScript gegen den neuen Server laufen.
    antwort.headers["Cache-Control"] = "no-cache"


def anwendung() -> web.Application:
    a = web.Application(middlewares=[schutz])
    a["aufgaben"] = set()
    a.on_response_prepare.append(ohne_zwischenspeicher)
    a.cleanup_ctx.append(neu_laden_bei_aenderung)
    a.router.add_get("/ping", ping)
    a.router.add_post("/anmelden", anmelden)
    a.router.add_get("/", startseite)
    a.router.add_get("/api/sitzungen", sitzungen)
    a.router.add_post("/api/schliessen", schliessen)
    a.router.add_get("/api/katalog", katalog)
    a.router.add_post("/api/starten", starten)
    a.router.add_post("/api/shell", shell)
    a.router.add_get("/api/system", system)
    a.router.add_post("/api/aktualisieren", aktualisieren)
    a.router.add_post("/api/anmeldung", anmeldung)
    a.router.add_get("/api/links/{name}", links)
    a.router.add_get("/ws/{name}", terminal)
    a.router.add_static("/static", HIER / "static", append_version=False)
    return a


if __name__ == "__main__":
    web.run_app(anwendung(), host=ADRESSE, port=PORT, access_log=None, print=None)
