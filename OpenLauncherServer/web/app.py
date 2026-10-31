"""Server-Konsole: Sitzungen sehen und schließen, die laufende CLI bedienen, Modelle starten. Nur im WireGuard-Tunnel (10.8.0.1:8090).

Anmeldung mit dem Fernsteuerungs-Token (~/SK/Jarvis/fernsteuerung-token.txt), den Jarvis auf dem Handy schon hat. Das Terminal hängt per
pty an `tmux attach`; die Sitzung selbst läuft im Dienst jarvis-tmux weiter, auch wenn diese Konsole neu startet.
"""
from __future__ import annotations

import asyncio
import fcntl
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
LINK = re.compile(r"https?://[^\s\"'<>]+")
ANMELDEN_SEITE = """<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Server-Konsole</title><body style="background:#10131a;color:#e6e9ef;font:16px system-ui;padding:24px">
<form method="post" action="/anmelden"><p>Zugangstoken</p><input name="token" type="password" autofocus style="width:100%;padding:12px;font-size:16px">
<p><button style="padding:12px 20px;font-size:16px">Anmelden</button></p></form></body></html>"""


def _gleich(wert: str) -> bool:
    return bool(TOKEN) and hmac.compare_digest(wert.encode(), TOKEN.encode())


def _angemeldet(request: web.Request) -> bool:
    return _gleich(request.cookies.get(COOKIE, ""))


def _mit_cookie(antwort: web.Response) -> web.Response:
    antwort.set_cookie(COOKIE, TOKEN, httponly=True, samesite="Strict", max_age=365 * 86400)
    return antwort


@web.middleware
async def schutz(request: web.Request, handler):
    if request.path in ("/ping", "/anmelden"):
        return await handler(request)
    if not _angemeldet(request):
        wert = request.query.get("token", "")
        if request.path == "/" and _gleich(wert):
            # Jarvis öffnet mit ?token=…: danach gilt das Cookie, der Token verschwindet aus der Adresse.
            raise _mit_cookie(web.HTTPFound("/"))
        if request.path == "/":
            return web.Response(text=ANMELDEN_SEITE, content_type="text/html")
        return web.json_response({"fehler": "nicht angemeldet"}, status=401)
    # Ein fremdes Origin darf nichts auslösen, auch nicht mit dem Cookie im selben Browser.
    herkunft = request.headers.get("Origin", "")
    if herkunft and herkunft.split("://", 1)[-1] != request.host:
        return web.json_response({"fehler": "fremde Herkunft"}, status=403)
    return await handler(request)


async def ping(_: web.Request) -> web.Response:
    return web.Response(text="ok")


async def anmelden(request: web.Request) -> web.Response:
    daten = await request.post()
    if _gleich(str(daten.get("token", "")).strip()):
        raise _mit_cookie(web.HTTPFound("/"))
    return web.Response(text=ANMELDEN_SEITE, content_type="text/html", status=403)


async def startseite(_: web.Request) -> web.FileResponse:
    return web.FileResponse(HIER / "static" / "index.html", headers={"Cache-Control": "no-cache"})


async def _im_faden(funktion, *argumente):
    return await asyncio.get_running_loop().run_in_executor(None, funktion, *argumente)


async def sitzungen(_: web.Request) -> web.Response:
    return web.json_response({"sitzungen": await _im_faden(launcher.sitzungen), "belegt": await _im_faden(launcher.belegt)})


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


async def system(_: web.Request) -> web.Response:
    def lesen():
        try:
            daten = json.loads(clis.STATUS.read_text("utf-8"))
        except (OSError, ValueError):
            daten = {"programme": {}}
        speicher = subprocess.run(["free", "-m"], capture_output=True, text=True, timeout=5).stdout.splitlines()
        wert = speicher[1].split() if len(speicher) > 1 else []
        daten["speicher"] = {"gesamt_mb": int(wert[1]), "frei_mb": int(wert[6])} if len(wert) > 6 else {}
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
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,80}", name):
        return web.json_response({"links": []})
    aus = await _im_faden(lambda: subprocess.run(launcher.TMUX + ["capture-pane", "-p", "-J", "-S", "-200", "-t", f"={name}:"],
                                                 capture_output=True, text=True, timeout=5).stdout)
    # Die Anzeige umbricht auch ohne -J-Treffer, wenn die CLI selbst umbricht: zusammenhängende Zeilen zusätzlich verbunden prüfen.
    gefunden = LINK.findall(aus) + LINK.findall(re.sub(r"\n(?=[^\s])", "", aus))
    einmal = list(dict.fromkeys(g.rstrip(".,)") for g in gefunden if len(g) > 12))
    return web.json_response({"links": einmal[-5:]})


async def terminal(request: web.Request) -> web.WebSocketResponse:
    name = request.match_info["name"]
    ws = web.WebSocketResponse(heartbeat=25, max_msg_size=4 * 1024 * 1024)
    await ws.prepare(request)
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,80}", name):
        await ws.close()
        return ws
    herr, knecht = pty.openpty()
    umgebung = dict(os.environ, TERM="xterm-256color", LANG="C.UTF-8", LC_ALL="C.UTF-8")
    prozess = subprocess.Popen(launcher.TMUX + ["attach-session", "-t", f"={name}"], stdin=knecht, stdout=knecht, stderr=knecht,
                               start_new_session=True, env=umgebung, close_fds=True)
    os.close(knecht)
    schleife = asyncio.get_running_loop()
    os.set_blocking(herr, False)
    warteschlange: asyncio.Queue = asyncio.Queue(maxsize=256)

    def lesbar():
        try:
            daten = os.read(herr, 65536)
        except BlockingIOError:
            return
        except OSError:
            daten = b""
        if not daten:
            schleife.remove_reader(herr)
        try:
            warteschlange.put_nowait(daten)
        except asyncio.QueueFull:
            # Der Empfänger kommt nicht nach (Mobilfunk): tmux zeichnet beim nächsten Bildaufbau ohnehin alles neu.
            pass

    schleife.add_reader(herr, lesbar)

    async def senden():
        while True:
            daten = await warteschlange.get()
            if not daten:
                await ws.close()
                return
            await ws.send_bytes(daten)

    sender = asyncio.create_task(senden())
    try:
        async for nachricht in ws:
            if nachricht.type != WSMsgType.TEXT:
                continue
            d = json.loads(nachricht.data)
            if d.get("t") == "i":
                os.write(herr, str(d.get("d", "")).encode("utf-8"))
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


def anwendung() -> web.Application:
    a = web.Application(middlewares=[schutz])
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
