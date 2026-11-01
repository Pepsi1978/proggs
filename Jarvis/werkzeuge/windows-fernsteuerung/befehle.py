# Computer Use auf dem Windows-PC: Befehle des im Jarvis-Chat gewählten Modells, Gegenstück zu mac-fernsteuerung/Befehle.swift.
# Das Handy öffnet je Befehl eine eigene Verbindung (Anmeldung mit art=werkzeug); eine laufende Live-Sitzung bleibt unberührt.
#
#   Handy -> PC: 0x12 Befehl-JSON {t:…}
#   PC -> Handy: 0x05 Antwort-JSON {ok, fehler?, …}, bei Dateien danach 0x06 mit den Rohdaten (Antwort nennt bytes).
# Koordinaten sind wie bei der Live-Bedienung Anteile (0…1) des Hauptbildschirms, die Umrechnung aus Bildpixeln macht das Handy.
#
# Bild und Aufnahme über ffmpeg gdigrab: kein Grafik-Encoder nötig und kein Konflikt mit der Desktop Duplication der Live-Sitzung.

import base64
import ctypes
import ctypes.wintypes as wt
import os
import subprocess
import tempfile
import threading
import time
import uuid

BILD_BREITE = 1280
AUFNAHME_SEKUNDEN_MAX = 120
OHNE_FENSTER = 0x08000000  # CREATE_NO_WINDOW
# Nach dem letzten Befehl bleibt ein virtueller Bildschirm noch so lange (wie am Mac), damit Folgebefehle nicht neu warten.
WERKZEUG_HALTEN_S = 180

user32 = ctypes.WinDLL("user32", use_last_error=True)
kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
shell32 = ctypes.WinDLL("shell32", use_last_error=True)
dwmapi = ctypes.WinDLL("dwmapi", use_last_error=True)

FENSTER_PROC = ctypes.WINFUNCTYPE(wt.BOOL, wt.HWND, wt.LPARAM)
user32.EnumWindows.argtypes = [FENSTER_PROC, wt.LPARAM]
user32.GetWindowTextW.argtypes = [wt.HWND, wt.LPWSTR, ctypes.c_int]
user32.GetWindowTextLengthW.argtypes = [wt.HWND]
user32.IsWindowVisible.argtypes = [wt.HWND]
user32.IsIconic.argtypes = [wt.HWND]
user32.GetWindowRect.argtypes = [wt.HWND, ctypes.POINTER(wt.RECT)]
user32.GetWindow.argtypes = [wt.HWND, wt.UINT]
user32.GetWindow.restype = wt.HWND
user32.GetWindowLongPtrW.argtypes = [wt.HWND, ctypes.c_int]
user32.GetWindowLongPtrW.restype = ctypes.c_ssize_t
user32.SetWindowPos.argtypes = [wt.HWND, wt.HWND, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_int, wt.UINT]
user32.ShowWindow.argtypes = [wt.HWND, ctypes.c_int]
user32.PostMessageW.argtypes = [wt.HWND, wt.UINT, wt.WPARAM, wt.LPARAM]
user32.SetForegroundWindow.argtypes = [wt.HWND]
user32.BringWindowToTop.argtypes = [wt.HWND]
user32.AttachThreadInput.argtypes = [wt.DWORD, wt.DWORD, wt.BOOL]
user32.keybd_event.argtypes = [wt.BYTE, wt.BYTE, wt.DWORD, ctypes.c_size_t]
user32.GetForegroundWindow.restype = wt.HWND
user32.GetWindowThreadProcessId.argtypes = [wt.HWND, ctypes.POINTER(wt.DWORD)]
kernel32.OpenProcess.restype = wt.HANDLE
kernel32.OpenProcess.argtypes = [wt.DWORD, wt.BOOL, wt.DWORD]
kernel32.CloseHandle.argtypes = [wt.HANDLE]
kernel32.QueryFullProcessImageNameW.argtypes = [wt.HANDLE, wt.DWORD, wt.LPWSTR, ctypes.POINTER(wt.DWORD)]
dwmapi.DwmGetWindowAttribute.argtypes = [wt.HWND, wt.DWORD, ctypes.c_void_p, wt.DWORD]
shell32.ShellExecuteW.argtypes = [wt.HWND, wt.LPCWSTR, wt.LPCWSTR, wt.LPCWSTR, wt.LPCWSTR, ctypes.c_int]
shell32.ShellExecuteW.restype = ctypes.c_ssize_t

SW_MINIMIEREN, SW_WIEDERHERSTELLEN, SW_NORMAL = 6, 9, 1
WM_CLOSE = 0x0010
GW_OWNER = 4
GWL_EXSTYLE = -20
WS_EX_TOOLWINDOW = 0x00000080
SWP_KEINE_GROESSE, SWP_KEINE_LAGE, SWP_KEINE_ORDNUNG, SWP_NICHT_AKTIVIEREN = 0x0001, 0x0002, 0x0004, 0x0010
DWMWA_CLOAKED = 14


def prozess_name(fenster) -> str:
    pid = wt.DWORD()
    user32.GetWindowThreadProcessId(fenster, ctypes.byref(pid))
    griff = kernel32.OpenProcess(0x1000, False, pid.value)  # PROCESS_QUERY_LIMITED_INFORMATION
    if not griff:
        return ""
    try:
        puffer = ctypes.create_unicode_buffer(1024)
        n = wt.DWORD(1024)
        if not kernel32.QueryFullProcessImageNameW(griff, 0, puffer, ctypes.byref(n)):
            return ""
        return os.path.splitext(os.path.basename(puffer.value))[0]
    finally:
        kernel32.CloseHandle(griff)


def _titel(fenster) -> str:
    n = user32.GetWindowTextLengthW(fenster)
    if n <= 0:
        return ""
    puffer = ctypes.create_unicode_buffer(n + 1)
    user32.GetWindowTextW(fenster, puffer, n + 1)
    return puffer.value


def _unsichtbar_verhuellt(fenster) -> bool:
    """Store-Apps im Hintergrund und Fenster anderer virtueller Desktops sind „sichtbar“, aber verhüllt."""
    wert = wt.DWORD()
    return dwmapi.DwmGetWindowAttribute(fenster, DWMWA_CLOAKED, ctypes.byref(wert), ctypes.sizeof(wert)) == 0 and wert.value != 0


def alle_fenster() -> list:
    """Hauptfenster der Programme, oberstes zuerst; minimierte gehören dazu (Lage dann bedeutungslos)."""
    ergebnis = []

    def je_fenster(h, _):
        try:
            if not user32.IsWindowVisible(h) or user32.GetWindow(h, GW_OWNER) or _unsichtbar_verhuellt(h):
                return True
            if user32.GetWindowLongPtrW(h, GWL_EXSTYLE) & WS_EX_TOOLWINDOW:
                return True
            titel = _titel(h)
            if not titel:
                return True
            r = wt.RECT()
            user32.GetWindowRect(h, ctypes.byref(r))
            ergebnis.append({"h": h, "app": prozess_name(h), "titel": titel, "x": r.left, "y": r.top, "w": r.right - r.left, "hoehe": r.bottom - r.top,
                             "minimiert": bool(user32.IsIconic(h))})
        except Exception:
            pass
        return True

    user32.EnumWindows(FENSTER_PROC(je_fenster), 0)
    return ergebnis


def in_vordergrund(h) -> bool:
    """Windows sperrt SetForegroundWindow für Hintergrundprozesse. Eine kurze Alt-Taste hebt die Sperre auf, sonst hilft das Anhängen
    an den Eingabefaden des vorderen Fensters. Erfolg nur, wenn es vorn liegt UND nicht minimiert ist."""
    if user32.IsIconic(h):
        user32.ShowWindow(h, SW_WIEDERHERSTELLEN)
    user32.keybd_event(0x12, 0, 0, 0)
    user32.keybd_event(0x12, 0, 2, 0)
    user32.SetForegroundWindow(h)
    if user32.GetForegroundWindow() != h:
        vorn = user32.GetForegroundWindow()
        ich, er = kernel32.GetCurrentThreadId(), user32.GetWindowThreadProcessId(vorn, None) if vorn else 0
        if er:
            user32.AttachThreadInput(ich, er, True)
        try:
            user32.BringWindowToTop(h)
            user32.SetForegroundWindow(h)
        finally:
            if er:
                user32.AttachThreadInput(ich, er, False)
    time.sleep(0.1)
    return user32.GetForegroundWindow() == h and not user32.IsIconic(h)


def _start_ziele(name: str) -> list:
    """Verknüpfungen des Startmenüs, deren Name passt; die genaue zuerst."""
    treffer = []
    for wurzel in (os.path.join(os.environ.get("APPDATA", ""), r"Microsoft\Windows\Start Menu\Programs"),
                   os.path.join(os.environ.get("PROGRAMDATA", ""), r"Microsoft\Windows\Start Menu\Programs")):
        for ordner, _, dateien in os.walk(wurzel):
            for d in dateien:
                stamm, endung = os.path.splitext(d)
                if endung.lower() == ".lnk" and name.lower() in stamm.lower():
                    treffer.append((stamm.lower() != name.lower(), len(stamm), os.path.join(ordner, d)))
    return [t[2] for t in sorted(treffer)]


def _store_app(name: str) -> str:
    """Die Kennung einer Store-App (Rechner, Fotos, …) über Get-StartApps; der Name geht als Umgebungsvariable, nie in den Befehl."""
    skript = "Get-StartApps | Where-Object Name -like \"*$env:JARVIS_APP*\" | Select-Object -First 1 -ExpandProperty AppID"
    try:
        r = subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-Command", skript], capture_output=True, text=True, timeout=20,
                           env=dict(os.environ, JARVIS_APP=name.replace("*", "").replace("?", "")), creationflags=OHNE_FENSTER)
        return r.stdout.strip()
    except Exception:
        return ""


def _ausfuehren(ziel: str, argumente: str | None = None) -> bool:
    # ShellExecute kennt registrierte Programme (App Paths), Adressen und Dateien, ohne Eingaben durch eine Shell zu schicken.
    return shell32.ShellExecuteW(None, "open", ziel, argumente, None, SW_NORMAL) > 32


def oeffnen(app: str, ziel: str) -> str | None:
    """Fehlertext oder None."""
    if not app:
        return None if _ausfuehren(ziel) else f"„{ziel}“ ließ sich nicht öffnen."
    # subprocess.list2cmdline: ein Pfad mit Leerzeichen bleibt ein Argument.
    argument = subprocess.list2cmdline([ziel]) if ziel else None
    if _ausfuehren(app, argument):
        return None
    if not app.lower().endswith(".exe") and _ausfuehren(app + ".exe", argument):
        return None
    for lnk in _start_ziele(app)[:1]:
        if _ausfuehren(lnk, argument):
            return None
    kennung = _store_app(app)
    if kennung and _ausfuehren("explorer.exe", "shell:AppsFolder\\" + kennung):
        return None
    return f"Kein Programm „{app}“ gefunden."


class Befehle:
    def __init__(self, dienst, ffmpeg: str, gesperrt, fokus, groesse, wecken, ohne_bildschirm, log):
        self.dienst = dienst
        self.ffmpeg = ffmpeg
        self.gesperrt = gesperrt
        self.fokus = fokus
        self.groesse = groesse
        self.wecken = wecken
        self.ohne_bildschirm = ohne_bildschirm
        self.log = log
        self.aufnahme_laeuft = threading.Lock()
        self.werkzeug_bis = 0.0

    def ausfuehren(self, j: dict) -> tuple:
        """(Antwort, Datei oder None). Läuft in einem eigenen Faden je Befehl."""
        t = j.get("t", "")
        self.log(f"Befehl: {t}")
        try:
            if t != "programme" and self.gesperrt():
                return {"ok": False, "gesperrt": True, "fehler": "Der Windows-PC ist gesperrt; entsperren kann ihn nur Frank am Gerät."}, None
            if t == "bildschirm":
                return self._bildschirm(j), None
            if t == "eingabe":
                return self._eingabe(j), None
            if t == "programme":
                return self._programme(j), None
            if t == "aufnahme":
                return self._aufnahme(j)
            return {"ok": False, "fehler": f"Unbekannter Befehl {t}"}, None
        except Exception as e:
            self.log(f"Befehl {t} gescheitert: {e}")
            return {"ok": False, "fehler": f"{type(e).__name__}: {e}"}, None

    # --- Bildschirm ---

    def _ziel_sichern(self) -> None:
        """Anzeige wach; zugeklappt ohne Live-Sitzung hängt der virtuelle Bildschirm an (der Dienst nimmt ihn nach der Haltezeit wieder ab)."""
        self.werkzeug_bis = time.monotonic() + WERKZEUG_HALTEN_S
        wecker = threading.Thread(target=self.wecken, daemon=True, name="wecken")
        wecker.start()
        wecker.join(1.5)
        if self.ohne_bildschirm() and not self.dienst.virtuell.aktiv and self.dienst.virtuell.an():
            time.sleep(2)  # bis Windows ihn als Hauptbildschirm eingerichtet hat

    def _griff(self, zusatz: list, ausgabe: list, frist: float) -> subprocess.CompletedProcess:
        w, h = self.groesse()
        # Nur der Hauptbildschirm: Dorthin gehen auch die Klicks; „desktop“ allein nähme alle Bildschirme zusammen.
        befehl = [self.ffmpeg, "-hide_banner", "-loglevel", "error", "-nostdin", *zusatz, "-f", "gdigrab", "-draw_mouse", "1",
                  "-offset_x", "0", "-offset_y", "0", "-video_size", f"{w}x{h}", "-i", "desktop", *ausgabe]
        return subprocess.run(befehl, capture_output=True, timeout=frist, stdin=subprocess.DEVNULL, creationflags=OHNE_FENSTER)

    def _bildschirm(self, j: dict) -> dict:
        self._ziel_sichern()
        w, h = self.groesse()
        qx, qy, qw, qh = 0, 0, w, h
        b = j.get("bereich")
        if isinstance(b, dict):
            ax, ay = min(max(float(b.get("x", 0)), 0), 0.99), min(max(float(b.get("y", 0)), 0), 0.99)
            aw, ah = min(max(float(b.get("w", 1)), 0.01), 1 - ax), min(max(float(b.get("h", 1)), 0.01), 1 - ay)
            qx, qy, qw, qh = int(ax * w), int(ay * h), max(int(aw * w), 2), max(int(ah * h), 2)
        hoehe = max(2, int(BILD_BREITE * qh / qw)) & ~1
        r = self._griff(["-y"], ["-frames:v", "1", "-vf", f"crop={qw}:{qh}:{qx}:{qy},scale={BILD_BREITE}:{hoehe}", "-q:v", "5", "-f", "image2pipe", "-c:v", "mjpeg", "pipe:1"], 10)
        if r.returncode != 0 or not r.stdout:
            return {"ok": False, "fehler": "Bildschirmfoto gescheitert: " + r.stderr.decode("utf-8", "replace").strip()[:200]}
        text, geheim = self.fokus()
        vorne = user32.GetForegroundWindow()
        return {"ok": True, "bild": base64.b64encode(r.stdout).decode("ascii"), "w": BILD_BREITE, "h": hoehe, "vorne": prozess_name(vorne) if vorne else "",
                "textfeld": text, "geheim": geheim}

    # --- Eingaben ---

    def _eingabe(self, j: dict) -> dict:
        schritte = j.get("schritte") or []
        for s in schritte:
            if s.get("t") == "taste" and not self.dienst.eingabe_.kennt(str(s.get("k", ""))):
                return {"ok": False, "fehler": f"Unbekannte Taste {s.get('k')}"}
        self._ziel_sichern()
        fertig = threading.Event()
        fehler = []

        def lauf():
            try:
                for s in schritte:
                    if "warten_ms" in s:
                        time.sleep(min(max(int(s["warten_ms"]), 0), 10_000) / 1000)
                        continue
                    self.dienst.eingabe_.ausfuehren(s)
                    time.sleep(0.03)
            except Exception as e:
                fehler.append(str(e))
            finally:
                fertig.set()

        # In dieselbe Warteschlange wie die Live-Bedienung, damit sich nichts überholt.
        self.dienst.eingaben.put(lauf)
        fertig.wait(120)
        return {"ok": False, "fehler": fehler[0]} if fehler else {"ok": True} if fertig.is_set() else {"ok": False, "fehler": "Die Eingabe läuft noch nach 2 Minuten."}

    # --- Programme und Fenster ---

    def _fenster_von(self, name: str, suche: str = "") -> list:
        n, s = name.lower(), suche.lower()
        alle = alle_fenster()
        genau = [f for f in alle if f["app"].lower() == n]
        # Windows-Programme heißen oft anders als ihre Datei (chrome, Google Chrome): zuerst die Datei, dann auch der Fenstertitel.
        passend = genau or [f for f in alle if n and (n in f["app"].lower() or f["app"].lower() in n and f["app"] or n in f["titel"].lower())]
        return [f for f in passend if not s or s in f["titel"].lower()]

    def _programme(self, j: dict) -> dict:
        aktion = j.get("aktion") or "liste"
        name = str(j.get("app") or "").strip()
        ziel = str(j.get("ziel") or "").strip()
        if aktion == "liste":
            w, h = self.groesse()
            alle = alle_fenster()
            vorne = user32.GetForegroundWindow()
            return {"ok": True, "gesperrt": self.gesperrt(), "vorne": prozess_name(vorne) if vorne else "",
                    "programme": list(dict.fromkeys(f["app"] for f in alle if f["app"])),
                    "fenster": [{"app": f["app"], "titel": f["titel"], "x": f["x"] / w, "y": f["y"] / h, "w": f["w"] / w, "h": f["hoehe"] / h}
                                for f in alle if not f["minimiert"] and f["w"] > 40 and f["hoehe"] > 40]}
        if self.gesperrt():
            return {"ok": False, "gesperrt": True, "fehler": "Der Windows-PC ist gesperrt; entsperren kann ihn nur Frank am Gerät."}
        if aktion in ("oeffnen", "vordergrund"):
            if not name and not ziel:
                return {"ok": False, "fehler": "Programm oder Ziel fehlt."}
            if name and not ziel:
                # Wie am Mac: Läuft es schon, kommt es nur nach vorn.
                fenster = self._fenster_von(name)
                if fenster:
                    return {"ok": True} if in_vordergrund(fenster[0]["h"]) else {"ok": False, "fehler": f"{name} ließ sich nicht nach vorn holen."}
            fehler = oeffnen(name, ziel)
            return {"ok": False, "fehler": fehler} if fehler else {"ok": True}
        fenster = self._fenster_von(name, str(j.get("fenster") or ""))
        if aktion == "beenden":
            fenster = self._fenster_von(name)
            if not fenster:
                return {"ok": False, "fehler": f"{name} läuft nicht oder hat kein Fenster."}
            # Wie „Fenster schließen“ von Hand: Das Programm darf nach ungespeicherten Änderungen fragen.
            for f in fenster:
                user32.PostMessageW(f["h"], WM_CLOSE, 0, 0)
            return {"ok": True}
        if aktion not in ("fenster_verschieben", "fenster_groesse", "fenster_minimieren", "fenster_schliessen", "fenster_vorn"):
            return {"ok": False, "fehler": f"Unbekannte Aktion {aktion}"}
        if not fenster:
            return {"ok": False, "fehler": f"Kein passendes Fenster von {name}."}
        h = fenster[0]["h"]
        bw, bh = self.groesse()
        ergebnis = True
        if aktion == "fenster_verschieben":
            ergebnis = user32.SetWindowPos(h, None, int(float(j.get("x", 0)) * bw), int(float(j.get("y", 0)) * bh), 0, 0, SWP_KEINE_GROESSE | SWP_KEINE_ORDNUNG | SWP_NICHT_AKTIVIEREN)
        elif aktion == "fenster_groesse":
            ergebnis = user32.SetWindowPos(h, None, 0, 0, int(float(j.get("w", 0.5)) * bw), int(float(j.get("h", 0.5)) * bh), SWP_KEINE_LAGE | SWP_KEINE_ORDNUNG | SWP_NICHT_AKTIVIEREN)
        elif aktion == "fenster_minimieren":
            user32.ShowWindow(h, SW_MINIMIEREN)
        elif aktion == "fenster_vorn":
            ergebnis = in_vordergrund(h)
        else:
            ergebnis = user32.PostMessageW(h, WM_CLOSE, 0, 0)
        return {"ok": True} if ergebnis else {"ok": False, "fehler": "Das Fenster nimmt das nicht an."}

    # --- Aufnahme ---

    def _aufnahme(self, j: dict) -> tuple:
        """Nimmt [sekunden] den Bildschirm auf (MP4). Dazu vier Standbilder gleichmäßig über die Zeit, damit das Modell sieht, was passiert ist."""
        if not self.aufnahme_laeuft.acquire(blocking=False):
            return {"ok": False, "fehler": "Es läuft schon eine Aufnahme."}, None
        datei = os.path.join(tempfile.gettempdir(), f"jarvis-aufnahme-{uuid.uuid4().hex}.mp4")
        try:
            sekunden = min(max(int(j.get("sekunden", 10)), 1), AUFNAHME_SEKUNDEN_MAX)
            self._ziel_sichern()
            w, h = self.groesse()
            hoehe = max(2, int(BILD_BREITE * h / w)) & ~1
            r = self._griff(["-y", "-framerate", "15"], ["-t", str(sekunden), "-vf", f"scale={BILD_BREITE}:{hoehe},format=yuv420p", "-c:v", "libx264", "-preset", "veryfast",
                                                         "-crf", "28", "-movflags", "+faststart", datei], sekunden + 30)
            if r.returncode != 0 or not os.path.isfile(datei):
                return {"ok": False, "fehler": "Aufnahme gescheitert: " + r.stderr.decode("utf-8", "replace").strip()[:200]}, None
            bilder = []
            for i in range(4):
                stand = max(sekunden * i / 3 * 0.98 - (0.2 if i == 3 else 0), 0)
                s = subprocess.run([self.ffmpeg, "-hide_banner", "-loglevel", "error", "-nostdin", "-ss", f"{stand:.2f}", "-i", datei, "-frames:v", "1",
                                    "-q:v", "5", "-f", "image2pipe", "-c:v", "mjpeg", "pipe:1"], capture_output=True, timeout=20, stdin=subprocess.DEVNULL, creationflags=OHNE_FENSTER)
                if s.returncode == 0 and s.stdout:
                    bilder.append(base64.b64encode(s.stdout).decode("ascii"))
            with open(datei, "rb") as f:
                video = f.read()
            if not video:
                return {"ok": False, "fehler": "Aufnahme gescheitert: keine Datei"}, None
            return {"ok": True, "sekunden": sekunden, "bilder": bilder, "bytes": len(video)}, video
        finally:
            self.aufnahme_laeuft.release()
            try:
                os.unlink(datei)
            except OSError:
                pass
