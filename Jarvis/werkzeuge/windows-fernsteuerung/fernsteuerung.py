# Jarvis-Fernsteuerung (Windows-Seite): zeigt den Windows-Bildschirm live in Jarvis auf dem Handy und nimmt Tippen,
# Wischen und Tastatur von dort entgegen. Gegenstück zur Mac-Seite (werkzeuge/mac-fernsteuerung), gleiches Protokoll.
#
# Bild: ffmpeg ddagrab (Desktop Duplication) -> H.264 über Intel Quick Sync (Hardware, Echtzeit) -> TCP.
# Eingaben: SendInput. Muss in der angemeldeten Benutzersitzung laufen (Aufgabe bei Anmeldung, siehe einrichten.ps1),
# aus Sitzung 0 gäbe es weder Bild noch Eingaben.
#
# Protokoll, beide Richtungen: [1 Byte Typ][4 Byte Länge, Big Endian][Nutzlast]
#   PC -> Handy: 0x01 Info-JSON {w,h,ok}, 0x02 H.264-Bild (Annex B), 0x03 Zustand-JSON {tastatur,geheim,gesperrt},
#                0x04 Meldung-JSON {meldung}
#   Handy -> PC: 0x10 Anmeldung-JSON {token}, 0x11 Eingabe-JSON {t:…}
# Nur lokal; von außen (Tunnel, Port 47800) kommt man über weiterleitung.js (Firewall).

import ctypes
import ctypes.wintypes as wt
import json
import os
import queue
import socket
import struct
import subprocess
import sys
import threading
import time

from virtueller_bildschirm import VirtuellerBildschirm

PORT = int(os.environ.get("FERNSTEUERUNG_PORT", "47801"))
STREAM_BREITE_MAX = 1600
BITRATE = "4M"
BILDER_PRO_SEKUNDE = 30
# Schlüsselbild alle 2 s: ein laufender ffmpeg lässt sich keins auf Zuruf entlocken; so hat ein neuer Dekoder
# (Verbinden, Drehen, Rückstau) spätestens nach 2 s wieder Bild.
SCHLUESSEL_ABSTAND = 60
# Ohne Anzeige (zugeklappt) hängt ddagrab still, ohne Fehler und ohne Ende (28.10.2026). Kommt bis dahin kein Bild,
# gilt der Start als gescheitert: ffmpeg beenden, damit Wecken und virtueller Bildschirm greifen.
ERSTES_BILD_FRIST = 3

TOKEN_PFAD = os.path.join(os.path.expanduser("~"), "SK", "Jarvis", "fernsteuerung-token.txt")
FFMPEG = os.environ.get("FERNSTEUERUNG_FFMPEG", "ffmpeg")


try:
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass


def log(s: str) -> None:
    sys.stderr.write(time.strftime("[%d.%m. %H:%M:%S] ") + s + "\n")
    sys.stderr.flush()


def token() -> str:
    try:
        with open(TOKEN_PFAD, encoding="utf-8") as f:
            return f.read().strip()
    except OSError:
        return ""


# --- Windows-Schnittstellen ---

user32 = ctypes.WinDLL("user32", use_last_error=True)
kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
# Physische Pixel wie die Aufnahme, sonst landen Klicks bei 150 % Skalierung daneben.
try:
    user32.SetProcessDpiAwarenessContext(ctypes.c_void_p(-4))
except Exception:
    pass

ULONG_PTR = ctypes.c_size_t


class MOUSEINPUT(ctypes.Structure):
    _fields_ = [("dx", wt.LONG), ("dy", wt.LONG), ("mouseData", wt.DWORD), ("dwFlags", wt.DWORD),
                ("time", wt.DWORD), ("dwExtraInfo", ULONG_PTR)]


class KEYBDINPUT(ctypes.Structure):
    _fields_ = [("wVk", wt.WORD), ("wScan", wt.WORD), ("dwFlags", wt.DWORD), ("time", wt.DWORD),
                ("dwExtraInfo", ULONG_PTR)]


class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [("uMsg", wt.DWORD), ("wParamL", wt.WORD), ("wParamH", wt.WORD)]


class _EINGABE(ctypes.Union):
    _fields_ = [("mi", MOUSEINPUT), ("ki", KEYBDINPUT), ("hi", HARDWAREINPUT)]


class INPUT(ctypes.Structure):
    _fields_ = [("type", wt.DWORD), ("u", _EINGABE)]


class GUITHREADINFO(ctypes.Structure):
    _fields_ = [("cbSize", wt.DWORD), ("flags", wt.DWORD), ("hwndActive", wt.HWND), ("hwndFocus", wt.HWND),
                ("hwndCapture", wt.HWND), ("hwndMenuOwner", wt.HWND), ("hwndMoveSize", wt.HWND),
                ("hwndCaret", wt.HWND), ("rcCaret", wt.RECT)]


user32.SendInput.argtypes = [wt.UINT, ctypes.POINTER(INPUT), ctypes.c_int]
user32.OpenInputDesktop.restype = wt.HANDLE
user32.GetUserObjectInformationW.argtypes = [wt.HANDLE, ctypes.c_int, ctypes.c_void_p, wt.DWORD, ctypes.POINTER(wt.DWORD)]
user32.CloseDesktop.argtypes = [wt.HANDLE]
user32.GetGUIThreadInfo.argtypes = [wt.DWORD, ctypes.POINTER(GUITHREADINFO)]
user32.GetWindowThreadProcessId.argtypes = [wt.HWND, ctypes.POINTER(wt.DWORD)]
user32.GetForegroundWindow.restype = wt.HWND
user32.GetWindowLongW.argtypes = [wt.HWND, ctypes.c_int]
user32.GetClassNameW.argtypes = [wt.HWND, wt.LPWSTR, ctypes.c_int]

MOUSE, TASTATUR = 0, 1
M_BEWEGEN, M_LINKS_RUNTER, M_LINKS_HOCH, M_RECHTS_RUNTER, M_RECHTS_HOCH = 0x0001, 0x0002, 0x0004, 0x0008, 0x0010
M_RAD, M_RAD_QUER, M_VIRTUELL, M_ABSOLUT = 0x0800, 0x1000, 0x4000, 0x8000
T_ERWEITERT, T_HOCH, T_UNICODE = 0x0001, 0x0002, 0x0004


def senden_eingaben(liste) -> None:
    if not liste:
        return
    feld = (INPUT * len(liste))(*liste)
    user32.SendInput(len(liste), feld, ctypes.sizeof(INPUT))


def maus_eingabe(flags, x=0, y=0, daten=0) -> INPUT:
    i = INPUT(type=MOUSE)
    i.u.mi = MOUSEINPUT(x, y, ctypes.c_ulong(daten & 0xFFFFFFFF).value, flags, 0, 0)
    return i


def taste_eingabe(vk=0, scan=0, flags=0) -> INPUT:
    i = INPUT(type=TASTATUR)
    i.u.ki = KEYBDINPUT(vk, scan, flags, 0, 0)
    return i


user32.SendMessageTimeoutW.argtypes = [wt.HWND, wt.UINT, wt.WPARAM, wt.LPARAM, wt.UINT, wt.UINT, ctypes.c_void_p]


def wach_anfordern() -> None:
    # ES_SYSTEM_REQUIRED | ES_DISPLAY_REQUIRED setzt die Leerlaufzähler zurück (ohne ES_CONTINUOUS: nichts bleibt hängen).
    kernel32.SetThreadExecutionState(0x00000001 | 0x00000002)


def bildschirm_wecken() -> None:
    """Anzeige einschalten. Bei zugeklapptem Deckel (Deckelaktion „Nichts unternehmen“) schaltet Windows sie nach der
    Bildschirm-Zeitsperre ab; Desktop Duplication liefert dann „access denied“ oder gar kein Bild (28.10.2026). Der
    Wachhalter verhindert nur das Abschalten; SC_MONITORPOWER „an“ schaltet sie wieder ein (am 28.10.2026 bei
    zugeklapptem Deckel geprüft: danach wieder Bilder). Kann bei hängenden Fenstern dauern, daher nie unter einer Sperre."""
    senden_eingaben([maus_eingabe(M_BEWEGEN, 0, 0)])
    user32.SendMessageTimeoutW(0xFFFF, 0x0112, 0xF170, -1, 0x0002, 300, None)  # HWND_BROADCAST, WM_SYSCOMMAND, SC_MONITORPOWER an
    wach_anfordern()


def bildschirm_groesse():
    """Hauptbildschirm in physischen Pixeln (ddagrab output_idx=0)."""
    return user32.GetSystemMetrics(0), user32.GetSystemMetrics(1)


def gesperrt() -> bool:
    """Sperrbildschirm oder UAC-Abfrage: der Eingabe-Desktop heißt dann nicht „Default“ oder ist nicht zugänglich."""
    h = user32.OpenInputDesktop(0, False, 0x0100)  # DESKTOP_READOBJECTS
    if not h:
        return True
    try:
        puffer = ctypes.create_unicode_buffer(64)
        n = wt.DWORD()
        if not user32.GetUserObjectInformationW(h, 2, puffer, ctypes.sizeof(puffer), ctypes.byref(n)):  # UOI_NAME
            return False
        return puffer.value.lower() != "default"
    finally:
        user32.CloseDesktop(h)


def fokus():
    """(Texteingabe aktiv, Passwortfeld): blinkender Textcursor im Vordergrundfenster; ES_PASSWORD bei klassischen Feldern."""
    fenster = user32.GetForegroundWindow()
    if not fenster:
        return False, False
    faden = user32.GetWindowThreadProcessId(fenster, None)
    info = GUITHREADINFO(cbSize=ctypes.sizeof(GUITHREADINFO))
    if not user32.GetGUIThreadInfo(faden, ctypes.byref(info)):
        return False, False
    text = bool(info.hwndCaret)
    geheim = False
    if text and info.hwndFocus:
        # ES_PASSWORD (0x20) gilt nur für Edit-Felder; bei anderen Klassen bedeutet das Bit etwas anderes.
        klasse = ctypes.create_unicode_buffer(32)
        user32.GetClassNameW(info.hwndFocus, klasse, 32)
        geheim = klasse.value.lower() == "edit" and bool(user32.GetWindowLongW(info.hwndFocus, -16) & 0x0020)
    return text, geheim


class Wachhalter:
    """Während einer Verbindung Rechner und Bildschirm wach halten; danach gelten wieder Windows' eigene Einstellungen."""

    def __init__(self):
        self.aktiv = False
        threading.Thread(target=self._schleife, daemon=True, name="wach").start()

    def _schleife(self):
        while True:
            if self.aktiv:
                wach_anfordern()
            time.sleep(20)


# --- Eingaben ---

VK = {
    "enter": 0x0D, "tab": 0x09, "leer": 0x20, "zurueck": 0x08, "esc": 0x1B, "entf": 0x2E,
    "links": 0x25, "hoch": 0x26, "rechts": 0x27, "runter": 0x28, "komma": 0xBC,
    "pos1": 0x24, "ende": 0x23, "bildhoch": 0x21, "bildrunter": 0x22, "f4": 0x73, "win": 0x5B,
    **{c: ord(c.upper()) for c in "abcdefghijklmnopqrstuvwxyz"},
}
ERWEITERT = {0x25, 0x26, 0x27, 0x28, 0x2E, 0x24, 0x23, 0x21, 0x22, 0x5B}
# „cmd“ vom Mac-Gegenstück entspricht unter Windows Strg (Kopieren, Einfügen, Rückgängig …).
MODS = [("cmd", 0x11), ("ctrl", 0x11), ("shift", 0x10), ("alt", 0x12), ("win", 0x5B)]


class Eingabe:
    def __init__(self):
        self.gedrueckt = False
        self.meldung = lambda text: None

    def _punkt(self, j):
        w, h = bildschirm_groesse()
        x = min(max(float(j.get("x", 0.5)), 0.0), 0.9999) * w
        y = min(max(float(j.get("y", 0.5)), 0.0), 0.9999) * h
        vx, vy = user32.GetSystemMetrics(76), user32.GetSystemMetrics(77)
        vw, vh = max(user32.GetSystemMetrics(78), 2), max(user32.GetSystemMetrics(79), 2)
        return int((x - vx) * 65535 / (vw - 1)), int((y - vy) * 65535 / (vh - 1))

    def _bewegen(self, j):
        x, y = self._punkt(j)
        return maus_eingabe(M_BEWEGEN | M_ABSOLUT | M_VIRTUELL, x, y)

    def ausfuehren(self, j):
        t = j.get("t")
        if t not in ("bewegen", "ziehen", "text", "entsperren", "scrollen"):
            log(f"Eingabe: {t} {j.get('k', '')}")
        if t in ("bewegen", "ziehen"):
            senden_eingaben([self._bewegen(j)])
        elif t == "klick":
            n = min(max(int(j.get("n", 1)), 1), 3)
            if j.get("doppel"):
                n = 1  # zweiter Klick eines Doppelklicks: der erste kam schon beim ersten Tippen
            liste = [self._bewegen(j)]
            for _ in range(n):
                liste += [maus_eingabe(M_LINKS_RUNTER), maus_eingabe(M_LINKS_HOCH)]
            senden_eingaben(liste)
        elif t == "rechtsklick":
            senden_eingaben([self._bewegen(j), maus_eingabe(M_RECHTS_RUNTER), maus_eingabe(M_RECHTS_HOCH)])
        elif t == "runter":
            senden_eingaben([self._bewegen(j), maus_eingabe(M_LINKS_RUNTER)])
            self.gedrueckt = True
        elif t == "hoch":
            senden_eingaben([self._bewegen(j), maus_eingabe(M_LINKS_HOCH)])
            self.gedrueckt = False
        elif t == "scrollen":
            # Finger nach unten schiebt den Inhalt nach unten (wie am Mac); 120 = eine Rastung, etwa 50 Pixel.
            dx, dy = float(j.get("dx", 0)), float(j.get("dy", 0))
            liste = [self._bewegen(j)] if "x" in j else []
            if dy:
                liste.append(maus_eingabe(M_RAD, daten=int(dy * 2.4)))
            if dx:
                liste.append(maus_eingabe(M_RAD_QUER, daten=int(-dx * 2.4)))
            senden_eingaben(liste)
        elif t == "text":
            self.schreiben(str(j.get("s", "")))
        elif t == "taste":
            self.taste(str(j.get("k", "")), [str(m) for m in j.get("m", [])])
        elif t == "loslassen":
            # Verbindung mitten im Ziehen abgerissen: linke Taste nicht gedrückt hängen lassen.
            if self.gedrueckt:
                senden_eingaben([maus_eingabe(M_LINKS_HOCH)])
                self.gedrueckt = False
        elif t == "entsperren":
            self.meldung("Den Windows-Sperrbildschirm lässt Windows aus der Ferne nicht bedienen. Bitte den PC einmal am Gerät entsperren.")

    def schreiben(self, text: str):
        for z in text:
            if z == "\n":
                self.taste("enter", [])
                continue
            if z == "\t":
                self.taste("tab", [])
                continue
            # UTF-16, damit auch Zeichen außerhalb der Grundebene (Emoji) als Ersatzpaar ankommen.
            einheiten = struct.unpack("<%dH" % (len(z.encode("utf-16-le")) // 2), z.encode("utf-16-le"))
            liste = []
            for e in einheiten:
                liste += [taste_eingabe(0, e, T_UNICODE), taste_eingabe(0, e, T_UNICODE | T_HOCH)]
            senden_eingaben(liste)
            time.sleep(0.004)

    def taste(self, name: str, mods):
        vk = VK.get(name)
        if vk is None:
            return
        gewaehlt = []
        for m, code in MODS:
            if m in mods and code not in gewaehlt:
                gewaehlt.append(code)
        liste = [taste_eingabe(c, 0, T_ERWEITERT if c in ERWEITERT else 0) for c in gewaehlt]
        f = T_ERWEITERT if vk in ERWEITERT else 0
        if vk not in gewaehlt:
            liste += [taste_eingabe(vk, 0, f), taste_eingabe(vk, 0, f | T_HOCH)]
        liste += [taste_eingabe(c, 0, (T_ERWEITERT if c in ERWEITERT else 0) | T_HOCH) for c in reversed(gewaehlt)]
        senden_eingaben(liste)


# --- Aufnahme und Kodierung ---

def stream_groesse():
    w, h = bildschirm_groesse()
    if w > STREAM_BREITE_MAX:
        h = h * STREAM_BREITE_MAX // w
        w = STREAM_BREITE_MAX
    return w & ~1, h & ~1


class Aufnahme:
    """ffmpeg-Prozess je Verbindung. Liefert vollständige Bilder (alle NAL-Einheiten eines Bildes, Annex B mit
    4-Byte-Startcodes, ohne AUD/SEI), SPS und PPS stehen vor jedem Schlüsselbild."""

    def __init__(self, w, h, bild, ende):
        self.w, self.h = w, h
        self.bild, self.ende = bild, ende
        self.proc = None
        self.gestoppt = False

    def start(self):
        filter_kette = f"hwmap=derive_device=qsv,scale_qsv=w={self.w}:h={self.h}:format=nv12"
        befehl = [
            FFMPEG, "-hide_banner", "-loglevel", "error", "-nostdin",
            "-init_hw_device", "d3d11va=d3d:0", "-init_hw_device", "qsv=q@d3d", "-filter_hw_device", "q",
            "-fflags", "nobuffer", "-f", "lavfi", "-i", f"ddagrab=output_idx=0:framerate={BILDER_PRO_SEKUNDE}:draw_mouse=1",
            "-vf", filter_kette,
            "-c:v", "h264_qsv", "-profile:v", "high", "-preset", "veryfast", "-low_delay_brc", "1", "-async_depth", "1",
            "-b:v", BITRATE, "-maxrate", "8M", "-bufsize", "4M", "-bf", "0", "-g", str(SCHLUESSEL_ABSTAND),
            "-bsf:v", "dump_extra=freq=keyframe,h264_metadata=aud=insert",
            "-flush_packets", "1", "-f", "h264", "pipe:1",
        ]
        self.proc = subprocess.Popen(befehl, stdout=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=0,
                                     creationflags=0x08000000)  # CREATE_NO_WINDOW
        threading.Thread(target=self._lesen, daemon=True, name="aufnahme").start()
        threading.Thread(target=self._fehler, daemon=True, name="aufnahme-log").start()

    def _fehler(self):
        for zeile in self.proc.stderr:
            log("ffmpeg: " + zeile.decode("utf-8", "replace").rstrip())

    def _lesen(self):
        rest = b""
        einheiten = []  # NAL-Einheiten des aktuellen Bildes (ohne Startcode)
        aus = self.proc.stdout
        try:
            while not self.gestoppt:
                d = aus.read(1 << 16)
                if not d:
                    break
                rest += d
                # Vollständige NAL-Einheiten herausschneiden; die letzte bleibt, bis der nächste Startcode da ist.
                pos = rest.find(b"\x00\x00\x01")
                if pos < 0:
                    continue
                while True:
                    naechster = rest.find(b"\x00\x00\x01", pos + 3)
                    if naechster < 0:
                        break
                    nal = rest[pos + 3:naechster].rstrip(b"\x00") if rest[naechster - 1:naechster] == b"\x00" else rest[pos + 3:naechster]
                    self._nal(nal, einheiten)
                    pos = naechster
                rest = rest[pos:]
        except Exception as e:
            if not self.gestoppt:
                log(f"Aufnahme: {e}")
        finally:
            if not self.gestoppt:
                self.ende()

    def _nal(self, nal, einheiten):
        if not nal:
            return
        typ = nal[0] & 0x1F
        if typ == 9:  # Bildgrenze: das bisher gesammelte Bild ist fertig
            if einheiten:
                self.bild(b"".join(b"\x00\x00\x00\x01" + e for e in einheiten))
                einheiten.clear()
            return
        if typ == 6:  # SEI: braucht der Dekoder nicht, und Schlüsselbilder sollen vorn erkennbar bleiben
            return
        einheiten.append(nal)

    def stop(self):
        self.gestoppt = True
        if self.proc and self.proc.poll() is None:
            try:
                self.proc.kill()
            except OSError:
                pass


# --- Verbindung zum Handy ---

class Sitzung:
    def __init__(self, sock: socket.socket, dienst):
        self.sock = sock
        self.dienst = dienst
        self.angemeldet = False
        self.zuletzt = time.monotonic()
        self.offen = True
        self.schlange = queue.Queue(maxsize=8)
        self.wartet_auf_schluessel = False
        self.sperre = threading.Lock()

    def start(self):
        threading.Thread(target=self._lesen, daemon=True, name="sitzung-lesen").start()
        threading.Thread(target=self._schreiben, daemon=True, name="sitzung-schreiben").start()

    def _genau(self, n):
        d = b""
        while len(d) < n:
            teil = self.sock.recv(n - len(d))
            if not teil:
                raise ConnectionError("getrennt")
            d += teil
        return d

    def _lesen(self):
        try:
            while self.offen:
                kopf = self._genau(5)
                typ, n = kopf[0], struct.unpack(">I", kopf[1:])[0]
                if n >= 1_000_000:
                    raise ValueError("Rahmen zu groß")
                daten = self._genau(n) if n else b""
                try:
                    j = json.loads(daten or b"{}")
                except ValueError:
                    j = {}
                if typ == 0x10:
                    t = token()
                    if t and j.get("token") == t:
                        self.angemeldet = True
                        self.dienst.angemeldet(self)
                    else:
                        log("Anmeldung abgelehnt")
                        break
                elif typ == 0x11 and self.angemeldet:
                    self.dienst.eingabe(self, j)
        except Exception:
            pass
        finally:
            self.stop()

    def _schreiben(self):
        try:
            while self.offen:
                d = self.schlange.get()
                if d is None:
                    break
                self.sock.sendall(d)
        except OSError:
            pass
        finally:
            self.stop()

    def senden(self, typ, nutz: bytes):
        if not self.offen:
            return
        d = bytes([typ]) + struct.pack(">I", len(nutz)) + nutz
        try:
            self.schlange.put_nowait(d)
        except queue.Full:
            pass

    def senden_json(self, typ, obj):
        self.senden(typ, json.dumps(obj).encode("utf-8"))

    def bild(self, au: bytes):
        """Bild nur senden, wenn die Leitung mitkommt; bei Rückstau bis zum nächsten Schlüsselbild verwerfen
        (ein fehlendes P-Bild würde das Bild bis dahin ohnehin verschmieren)."""
        schluessel = any((au[i + 4] & 0x1F) == 5 for i in range(0, min(len(au) - 4, 256)) if au[i:i + 4] == b"\x00\x00\x00\x01")
        if self.wartet_auf_schluessel and not schluessel:
            return
        if self.schlange.qsize() > 3:
            self.wartet_auf_schluessel = True
            self.dienst.verworfen += 1
            return
        self.wartet_auf_schluessel = False
        self.senden(0x02, au)

    def stop(self):
        with self.sperre:
            if not self.offen:
                return
            self.offen = False
        try:
            self.sock.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        try:
            self.sock.close()
        except OSError:
            pass
        try:
            self.schlange.put_nowait(None)
        except queue.Full:
            pass
        self.dienst.beendet(self)


class Dienst:
    def __init__(self):
        self.sitzung = None
        self.aufnahme = None
        self.eingabe_ = Eingabe()
        self.wach = Wachhalter()
        self.virtuell = VirtuellerBildschirm(log)
        self.sperre = threading.RLock()
        self.verworfen = 0
        self.fehlstarts = 0
        self.zustand = (False, False, gesperrt())
        self.eingaben = queue.Queue()
        threading.Thread(target=self._eingaben, daemon=True, name="eingabe").start()
        threading.Thread(target=self._beobachten, daemon=True, name="zustand").start()

    def _eingaben(self):
        # Der Reihe nach, aber nicht im Lesefaden (langes Tippen soll Puls und Bild nicht anhalten).
        while True:
            j = self.eingaben.get()
            try:
                self.eingabe_.ausfuehren(j)
            except Exception as e:
                log(f"Eingabe fehlgeschlagen: {e}")

    def angemeldet(self, s: Sitzung):
        log("angemeldet")
        with self.sperre:
            alt = self.sitzung
            self.sitzung = s
            self.eingabe_.meldung = lambda text, s=s: s.senden_json(0x04, {"meldung": text})
            self.wach.aktiv = True
            self.fehlstarts = 0
        if alt is not None and alt is not s:
            alt.stop()
        t, g, gs = self.zustand
        s.senden_json(0x03, {"tastatur": t, "geheim": g, "gesperrt": gs})
        # Nicht im Lesefaden warten: Ist die Anzeige aus, scheitert der erste Versuch, der zweite (2 s) hat Bild.
        threading.Thread(target=bildschirm_wecken, daemon=True, name="wecken").start()
        self.aufnahme_starten()

    def _nachhelfen(self):
        """Kein einziges Bild: Anzeige wecken; hilft das zweimal nicht (kein Bildschirm mehr im Windows-Bild, etwa
        zugeklappt), wie am Mac einen virtuellen Bildschirm anhängen. Außerhalb der Sperre (Treiber bis 5 s)."""
        bildschirm_wecken()
        if self.fehlstarts < 2 or self.virtuell.aktiv or not self.virtuell.an():
            return
        time.sleep(2)
        # Bleibt ein zweiter (dunkler) Bildschirm im Windows-Bild, nur noch den virtuellen zeigen: Dann ist er Haupt-
        # bildschirm, Aufnahme (output_idx=0) und Eingaben (Hauptbildschirm) treffen ihn. Verschwindet er nach der
        # Sitzung, stellt Windows die vorige Anordnung selbst wieder her.
        if user32.GetSystemMetrics(80) > 1:  # SM_CMONITORS
            log("Nur virtueller Bildschirm (DisplaySwitch /external)")
            subprocess.run(["DisplaySwitch.exe", "/external"], creationflags=0x08000000)
            time.sleep(2)

    def aufnahme_starten(self):
        with self.sperre:
            if self.aufnahme:
                self.aufnahme.stop()
                self.aufnahme = None
            s = self.sitzung
            if s is None:
                return
            w, h = stream_groesse()
            erstes = [True]

            def bild(au, s=s):
                if erstes[0]:
                    erstes[0] = False
                    self.fehlstarts = 0
                    log(f"Aufnahme läuft {w}x{h}")
                s.bild(au)

            def ende(s=s):
                log("Aufnahme beendet")
                with self.sperre:
                    # Eine neuere Aufnahme (z. B. nach dem Entsperren gestartet) nicht wieder abschießen.
                    if self.sitzung is not s or self.aufnahme is not neu:
                        return
                    if erstes[0]:
                        s.senden_json(0x01, {"w": w, "h": h, "ok": False})
                    if self.zustand[2]:
                        return  # gesperrt: _beobachten startet die Aufnahme nach dem Entsperren neu
                    self.fehlstarts += 1
                    warten = 2 if self.fehlstarts <= 3 else 5
                    ohne_bild = erstes[0]

                # Bildschirm umgebaut, Grafiktreiber neu: nach kurzer Pause neu versuchen, solange verbunden.
                def nochmal():
                    if self.sitzung is not s or self.aufnahme is not neu:
                        return
                    if ohne_bild:
                        self._nachhelfen()
                    if self.sitzung is s and self.aufnahme is neu:
                        self.aufnahme_starten()
                threading.Timer(warten, nochmal).start()

            def frist():
                if erstes[0] and self.aufnahme is neu and neu.proc and neu.proc.poll() is None:
                    log(f"kein Bild nach {ERSTES_BILD_FRIST} s, Aufnahme neu")
                    neu.proc.kill()  # nicht stop(): ende() soll laufen und nachhelfen

            neu = Aufnahme(w, h, bild, ende)
            self.aufnahme = neu
            neu.start()
            threading.Timer(ERSTES_BILD_FRIST, frist).start()
            # Info schon beim Start (Quick Sync braucht etwa 1 s bis zum ersten Bild): So steht der Dekoder am Handy,
            # wenn das erste Schlüsselbild kommt; sonst ginge es verloren und das Bild bliebe bis zum nächsten schwarz.
            s.senden_json(0x01, {"w": w, "h": h, "ok": True})

    def eingabe(self, s: Sitzung, j):
        if self.sitzung is not s:
            return
        s.zuletzt = time.monotonic()
        t = j.get("t")
        if t == "puls" or t == "schluesselbild":
            # Schlüsselbilder kommen ohnehin alle 2 s (siehe SCHLUESSEL_ABSTAND).
            return
        self.eingaben.put(j)

    def beendet(self, s: Sitzung):
        with self.sperre:
            if self.sitzung is not s:
                return
            log("Verbindung beendet")
            self.sitzung = None
            self.wach.aktiv = False
            self.eingaben.put({"t": "loslassen"})
            if self.aufnahme:
                self.aufnahme.stop()
                self.aufnahme = None
            # Nach der Sitzung weg damit, sonst landen beim Aufklappen Fenster auf einem unsichtbaren Bildschirm.
            self.virtuell.aus()

    def _beobachten(self):
        war_gesperrt = self.zustand[2]
        while True:
            time.sleep(0.35)
            try:
                gs = gesperrt()
                t, g = (False, False) if gs else fokus()
            except Exception:
                continue
            neu = (t, g, gs)
            s = self.sitzung
            if neu != self.zustand:
                self.zustand = neu
                if s:
                    s.senden_json(0x03, {"tastatur": t, "geheim": g, "gesperrt": gs})
            if war_gesperrt and not gs and s:
                log("entsperrt, Aufnahme neu")
                self.aufnahme_starten()
            war_gesperrt = gs
            # Lebenszeichen alle 3 s vom Handy; bleibt es 12 s aus (Tunnel weg, Handy aus), Sitzung beenden.
            if s and time.monotonic() - s.zuletzt > 12:
                log("kein Lebenszeichen vom Handy, Sitzung beendet")
                s.stop()

    def start(self):
        if not token():
            log(f"Kein Token in {TOKEN_PFAD}")
            sys.exit(1)
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        # Exklusiv: ein zweiter Start (Wächter der Aufgabe) scheitert hier und beendet sich.
        server.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
        try:
            server.bind(("127.0.0.1", PORT))
        except OSError as e:
            log(f"Port {PORT} belegt ({e}), läuft wohl schon")
            sys.exit(0)
        server.listen(4)
        log(f"Jarvis-Fernsteuerung lauscht auf Port {PORT}, Bildschirm {bildschirm_groesse()}, Stream {stream_groesse()}")
        while True:
            sock, adresse = server.accept()
            # Nicht protokollieren: Jarvis prüft alle 10 s ohne Anmeldung, ob der Dienst antwortet (Symbol unten rechts).
            sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 1 << 20)
            s = Sitzung(sock, self)
            s.start()
            # Ohne Anmeldung binnen 10 s wieder schließen.
            threading.Timer(10, lambda s=s: (not s.angemeldet) and s.stop()).start()


if __name__ == "__main__":
    Dienst().start()
