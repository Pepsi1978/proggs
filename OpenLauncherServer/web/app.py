"""Server-Konsole: Sitzungen sehen und schließen, die laufende CLI bedienen, Modelle starten. Nur im WireGuard-Tunnel (10.8.0.1:8090).

Anmeldung mit dem Fernsteuerungs-Token (~/SK/Jarvis/fernsteuerung-token.txt), den Jarvis auf dem Handy schon hat. Das Terminal hängt per
pty an `tmux attach`; die Sitzung selbst läuft im Dienst jarvis-tmux weiter, auch wenn diese Konsole neu startet.
"""
from __future__ import annotations

import asyncio
import fcntl
import gzip
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
ADRESS_TEIL = re.compile(r"[A-Za-z0-9\-._~:/?#\[\]@!$&()*+,;=%]+")
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
    except (ValueError, KeyError, TypeError, RuntimeError, subprocess.TimeoutExpired) as fehler:
        # Unlesbare Anfrage, ungültige Wahl oder gescheiterter Start: verständlich an die Seite, nicht als HTTP 500.
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


async def shell(_: web.Request) -> web.Response:
    return web.json_response({"name": await _im_faden(launcher.befehl_starten, "PowerShell", "command -v pwsh >/dev/null && pwsh -l || bash -l")})


STATUS_AUFFRISCHEN = {"zeit": 0.0, "laeuft": False}


async def _status_auffrischen() -> None:
    """Anmeldestände frisch prüfen, höchstens jede Minute (jede Prüfung startet mehrere CLIs): Sonst zeigte der Reiter bis zum nächsten Update-Lauf „nicht angemeldet“, obwohl der Login gelang."""
    try:
        await _im_faden(clis.status_schreiben, False)
    finally:
        STATUS_AUFFRISCHEN.update(zeit=time.monotonic(), laeuft=False)


async def system(request: web.Request) -> web.Response:
    if not STATUS_AUFFRISCHEN["laeuft"] and time.monotonic() - STATUS_AUFFRISCHEN["zeit"] > 60:
        STATUS_AUFFRISCHEN["laeuft"] = True
        aufgabe = asyncio.create_task(_status_auffrischen())
        request.app["aufgaben"].add(aufgabe)
        aufgabe.add_done_callback(request.app["aufgaben"].discard)

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
    if not p or not p["anmeldung"]:
        return web.json_response({"fehler": "Dafür gibt es keine Anmeldung"}, status=400)
    umgebung = f"export CLAUDE_CONFIG_DIR={launcher.claude_ordner('minimal')} CODEX_HOME={launcher.codex_zuhause()}\n"
    if d.get("cli") == "claude":
        await _im_faden(launcher.claude_vorbereiten, "minimal", "schnell")
    return web.json_response({"name": await _im_faden(launcher.befehl_starten, f"Anmeldung {p['name']}", umgebung + p["anmeldung"], str(d.get("cli")))})


def _links_aus(text: str) -> list:
    """Adressen aus der Terminalausgabe. Claude bricht lange Adressen selbst um (der Rest steht ohne Leerzeichen in den Folgezeilen); nur die erste
    Zeile ergäbe eine abgeschnittene Adresse, und die Anmeldeseite meldete „redirect_uri fehlt“."""
    gefunden = []
    zeilen = text.splitlines()
    for i, zeile in enumerate(zeilen):
        for treffer in LINK.finditer(zeile):
            adresse = treffer.group(0)
            # Nur am Zeilenende einer langen Zeile kann die Adresse weitergehen.
            if treffer.end() == len(zeile.rstrip()) and len(zeile.rstrip()) >= 40:
                for folge in zeilen[i + 1:]:
                    teil = folge.strip()
                    if not ADRESS_TEIL.fullmatch(teil):
                        break
                    adresse += teil
                    if len(folge.rstrip()) < 40:
                        break
            gefunden.append(adresse)
    return gefunden


async def browser_start(_: web.Request) -> web.Response:
    await _im_faden(launcher.browser_starten)
    return web.json_response({"ok": True})


async def bildschirm(request: web.Request) -> web.WebSocketResponse:
    """Reicht den VNC-Bildschirm des Browsers als WebSocket durch; er lauscht nur auf localhost, die Anmeldung der Konsole schützt ihn."""
    ws = web.WebSocketResponse(protocols=("binary",), heartbeat=25, max_msg_size=16 * 1024 * 1024)
    await ws.prepare(request)
    try:
        leser, schreiber = await asyncio.open_connection("127.0.0.1", 5900)
    except OSError:
        await ws.close(code=4404)
        return ws

    async def zum_browser():
        async for nachricht in ws:
            if nachricht.type == WSMsgType.BINARY:
                schreiber.write(nachricht.data)
                await schreiber.drain()
        schreiber.close()

    async def zur_seite():
        while daten := await leser.read(65536):
            await ws.send_bytes(daten)
        await ws.close()

    aufgaben = [asyncio.create_task(zum_browser()), asyncio.create_task(zur_seite())]
    try:
        await asyncio.wait(aufgaben, return_when=asyncio.FIRST_COMPLETED)
    finally:
        for aufgabe in aufgaben:
            aufgabe.cancel()
        # Abrisse sind im Mobilfunk normal; ihre Ausnahmen gehören nicht ins Protokoll.
        await asyncio.gather(*aufgaben, return_exceptions=True)
        schreiber.close()
    return ws


async def browser_wecken(leser: asyncio.StreamReader, schreiber: asyncio.StreamWriter) -> None:
    """Port 9221 nur auf localhost: Die Browser-Steuerung der Sitzungen kommt hierher. Beim ersten Zugriff startet der Browser, dann werden die Bytes durchgereicht."""
    ziel = None
    try:
        await _im_faden(launcher.browser_starten)
        ziel_leser, ziel = await asyncio.open_connection("127.0.0.1", 9222)

        async def pumpe(von: asyncio.StreamReader, nach: asyncio.StreamWriter):
            while daten := await von.read(65536):
                nach.write(daten)
                await nach.drain()

        aufgaben = [asyncio.create_task(pumpe(leser, ziel)), asyncio.create_task(pumpe(ziel_leser, schreiber))]
        await asyncio.wait(aufgaben, return_when=asyncio.FIRST_COMPLETED)
        for aufgabe in aufgaben:
            aufgabe.cancel()
        await asyncio.gather(*aufgaben, return_exceptions=True)
    except (OSError, RuntimeError, subprocess.TimeoutExpired):
        pass
    finally:
        for w in (schreiber, ziel):
            if w is not None:
                w.close()


async def browser_wecker_starten(_: web.Application):
    server = await asyncio.start_server(browser_wecken, "127.0.0.1", 9221)
    yield
    server.close()


async def links(request: web.Request) -> web.Response:
    """Adressen aus der Terminalausgabe: Im Handy-Terminal bricht eine lange Anmelde-Adresse um und lässt sich nicht antippen."""
    name = request.match_info["name"]
    launcher.pruefe_name(name)
    aus = await _im_faden(lambda: subprocess.run(launcher.TMUX + ["capture-pane", "-p", "-J", "-S", "-200", "-t", f"={name}:"],
                                                 capture_output=True, text=True, timeout=5).stdout)
    einmal = list(dict.fromkeys(g.rstrip(".,;") for g in _links_aus(aus) if len(g) > 12))
    return web.json_response({"links": einmal[-5:]})


async def terminal(request: web.Request) -> web.WebSocketResponse:
    name = request.match_info["name"]
    ws = web.WebSocketResponse(heartbeat=25, max_msg_size=4 * 1024 * 1024)
    await ws.prepare(request)
    # Die Sitzung gibt es nicht (mehr), oder sie gehört dem Dienst: 4404 sagt der Seite, dass neu Verbinden nichts bringt.
    if not launcher.NAME.fullmatch(name) or name in launcher.INTERN or await _im_faden(lambda: subprocess.run(launcher.TMUX + ["has-session", "-t", f"={name}"], capture_output=True, timeout=5).returncode) != 0:
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
    # Der Verlauf-Modus gehört zur Sitzung, nicht zu dieser Verbindung: Beim ersten Eingeben also immer verlassen, auch nach einem Neustart der Konsole.
    gerollt = [True]

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
                if gerollt[0]:
                    gerollt[0] = False
                    await _im_faden(launcher.verlauf_ende, name)
                await schreiben(str(d.get("d", "")).encode("utf-8"))
            elif d.get("t") == "s":
                gerollt[0] = True
                await _im_faden(launcher.rollen, name, max(-500, min(500, int(d.get("n", 0)))))
            elif d.get("t") == "e":
                gerollt[0] = False
                await _im_faden(launcher.verlauf_ende, name, True)
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
        # Beim nächsten Öffnen steht die Sitzung wieder am aktuellen Stand, und Eingaben von Jarvis landen nicht im Verlauf-Modus.
        try:
            await _im_faden(launcher.verlauf_ende, name)
        except (subprocess.TimeoutExpired, OSError, ValueError):
            pass
    return ws


async def neu_laden_bei_aenderung(_: web.Application):
    """Neue Fassung aus dem Repo: beenden, systemd startet neu."""
    # Auch die Seiten: Ihre gepackten Fassungen entstehen beim Start, eine geänderte app.js käme sonst nie an.
    dateien = [Path(__file__), Path(launcher.__file__), Path(clis.__file__)] + [HIER / "static" / n for n in ("app.js", "style.css", "index.html")]
    stand = [d.stat().st_mtime for d in dateien]

    async def wache():
        while True:
            await asyncio.sleep(30)
            if [d.stat().st_mtime for d in dateien] != stand:
                os._exit(0)

    aufgabe = asyncio.create_task(wache())
    yield
    aufgabe.cancel()


async def ohne_zwischenspeicher(request: web.Request, antwort: web.StreamResponse) -> None:
    # Nach einem Vorspulen darf kein altes JavaScript gegen den neuen Server laufen. xterm hat eine feste Version: Es kommt aus dem Speicher des
    # Handys, ohne jede Rückfrage über den Tunnel.
    fest = request.path in ("/static/xterm.js", "/static/xterm.css", "/static/addon-fit.js")
    # noVNC sind rund 60 Dateien aus einem Systempaket: Ohne das fragte jedes Öffnen jede einzeln über den Tunnel nach.
    if antwort.status == 200 and (fest or request.path.startswith("/vnc/")):
        antwort.headers["Cache-Control"] = "max-age=2592000" if fest else "max-age=86400"
    else:
        antwort.headers["Cache-Control"] = "no-cache"


def gepackt_ablegen() -> None:
    """aiohttp liefert <datei>.gz von selbst aus: Das Terminal schrumpft von 290 auf rund 70 KB, das merkt man im Mobilfunk beim ersten Laden."""
    for name in ("xterm.js", "xterm.css", "addon-fit.js", "app.js", "style.css"):
        quelle, ziel = HIER / "static" / name, HIER / "static" / f"{name}.gz"
        try:
            if not ziel.exists() or ziel.stat().st_mtime < quelle.stat().st_mtime:
                neu = ziel.with_name(f"{ziel.name}.{os.getpid()}.neu")
                neu.write_bytes(gzip.compress(quelle.read_bytes(), 9))
                os.replace(neu, ziel)
        except OSError:
            # Nur eine Beschleunigung: Ohne sie läuft die Konsole, und eine veraltete Packung darf nie ausgeliefert werden.
            ziel.unlink(missing_ok=True)


def anwendung() -> web.Application:
    a = web.Application(middlewares=[schutz])
    a["aufgaben"] = set()
    a.on_response_prepare.append(ohne_zwischenspeicher)
    a.cleanup_ctx.append(neu_laden_bei_aenderung)
    a.cleanup_ctx.append(browser_wecker_starten)
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
    a.router.add_post("/api/browser", browser_start)
    a.router.add_get("/vnc-ws", bildschirm)
    if Path("/usr/share/novnc").is_dir():
        a.router.add_static("/vnc", "/usr/share/novnc", append_version=False, follow_symlinks=True)
    a.router.add_static("/static", HIER / "static", append_version=False)
    return a


if __name__ == "__main__":
    gepackt_ablegen()
    web.run_app(anwendung(), host=ADRESSE, port=PORT, access_log=None, print=None)
