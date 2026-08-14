#!/usr/bin/env python3
"""Klick-Auswahl fuer Android: scrcpy spiegeln, Kontext sichern, an OpenCode uebergeben."""

from __future__ import annotations

import argparse
import ctypes
import json
import os
import re
import subprocess
import sys
import tempfile
import threading
from ctypes import wintypes
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from zeigefinger import (
    BOUNDS_RE,
    VERSION,
    VERSION_STAND,
    adb,
    ausschnitt,
    element_an_punkt,
    elemente_im_bereich,
    elementkette_an_punkt,
    ist_emulator,
    normiere_kasten,
    log,
    screenshot,
    suche_quellcode,
    suchbegriffe,
    ui_baum,
    waehle_geraet,
)


try:
    getattr(sys.stdout, "reconfigure")(
        encoding="utf-8", errors="backslashreplace", line_buffering=True
    )
    getattr(sys.stderr, "reconfigure")(encoding="utf-8", errors="backslashreplace")
except (AttributeError, OSError):
    pass


ACCENT = "#ff5a36"
ACCENT_DARK = "#9f2c16"
PANEL = "#17191d"
TEXT = "#f7f4ee"
MUTEX_NAME = "Local\\ZeigefingerOverlay"
ZUG_SCHWELLE = 8  # Fensterpixel; darunter war es ein Zeigen, kein Aufziehen
# Der Rahmen bekommt ein eigenes Fenster: die Auswahlflaeche liegt mit 11 Prozent
# Deckkraft ueber der App, dort gezeichnetes Schwarz waere ausgewaschenes Grau.
# Die Schluesselfarbe wird im Rahmenfenster durchsichtig gestellt, sodass nur die
# Linie stehen bleibt — und die ist dann wirklich schwarz.
RAHMEN_FARBE = "#000000"
RAHMEN_STAERKE = 4
SCHLUESSELFARBE = "#ff00fe"
# Die Auswahlflaeche faerbt das Bild nicht mehr ein. Sie liegt nur da, um den
# Klick abzufangen, bevor er Android erreicht — dafuer muss man sie nicht sehen.
# Ganz auf 0 geht nicht: ein Fenster mit Deckkraft 0 nimmt keine Maustaste mehr
# an. 1/255 ist unsichtbar und faengt trotzdem.
# Frueher standen hier 28/255; ueber einem dunklen Bildschirm hellte das alles
# rot auf. Dass der Modus an ist, zeigt die Leiste — nicht das Bild der App.
TOENUNG = 1  # von 255

SW_HIDE = 0
SW_MINIMIZE = 6
SW_RESTORE = 9
GWL_EXSTYLE = -20
GWL_STYLE = -16
WS_EX_TOOLWINDOW = 0x00000080
WS_EX_TRANSPARENT = 0x00000020
WS_EX_LAYERED = 0x00080000
WS_CHILD = 0x40000000
WS_POPUP = 0x80000000
HWND_TOP = 0
SWP_NOACTIVATE = 0x0010
VK_CONTROL = 0x11
KEYEVENTF_KEYUP = 0x0002
PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
LWA_ALPHA = 0x00000002
_WIN_API_READY = False


class Point(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]


class Rect(ctypes.Structure):
    _fields_ = [
        ("left", ctypes.c_long),
        ("top", ctypes.c_long),
        ("right", ctypes.c_long),
        ("bottom", ctypes.c_long),
    ]


@dataclass(frozen=True)
class WindowInfo:
    hwnd: int
    title: str
    process: str


def _win_api():
    global _WIN_API_READY
    if sys.platform != "win32":
        raise SystemExit("Das Auswahl-Overlay wird derzeit nur unter Windows unterstuetzt.")
    user32, kernel32 = ctypes.windll.user32, ctypes.windll.kernel32
    if not _WIN_API_READY:
        user32.GetForegroundWindow.restype = wintypes.HWND
        user32.GetWindowThreadProcessId.argtypes = [wintypes.HWND, ctypes.POINTER(wintypes.DWORD)]
        user32.GetWindowThreadProcessId.restype = wintypes.DWORD
        user32.GetWindowLongW.argtypes = [wintypes.HWND, ctypes.c_int]
        user32.SetWindowLongW.argtypes = [wintypes.HWND, ctypes.c_int, ctypes.c_long]
        user32.GetParent.argtypes = [wintypes.HWND]
        user32.GetParent.restype = wintypes.HWND
        user32.SetParent.argtypes = [wintypes.HWND, wintypes.HWND]
        user32.SetParent.restype = wintypes.HWND
        user32.SetLayeredWindowAttributes.argtypes = [
            wintypes.HWND,
            wintypes.COLORREF,
            wintypes.BYTE,
            wintypes.DWORD,
        ]
        user32.SetWindowPos.argtypes = [
            wintypes.HWND,
            wintypes.HWND,
            ctypes.c_int,
            ctypes.c_int,
            ctypes.c_int,
            ctypes.c_int,
            wintypes.UINT,
        ]
        kernel32.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
        kernel32.OpenProcess.restype = wintypes.HANDLE
        kernel32.CloseHandle.argtypes = [wintypes.HANDLE]
        kernel32.QueryFullProcessImageNameW.argtypes = [
            wintypes.HANDLE,
            wintypes.DWORD,
            wintypes.LPWSTR,
            ctypes.POINTER(wintypes.DWORD),
        ]
        kernel32.CreateMutexW.argtypes = [ctypes.c_void_p, wintypes.BOOL, wintypes.LPCWSTR]
        kernel32.CreateMutexW.restype = wintypes.HANDLE
        _WIN_API_READY = True
    return user32, kernel32


def _process_name(hwnd: int) -> str:
    user32, kernel32 = _win_api()
    pid = wintypes.DWORD()
    user32.GetWindowThreadProcessId(wintypes.HWND(hwnd), ctypes.byref(pid))
    handle = kernel32.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, False, pid.value)
    if not handle:
        return ""
    try:
        size = wintypes.DWORD(32768)
        buffer = ctypes.create_unicode_buffer(size.value)
        if not kernel32.QueryFullProcessImageNameW(handle, 0, buffer, ctypes.byref(size)):
            return ""
        return Path(buffer.value).name
    finally:
        kernel32.CloseHandle(handle)


def fenster_liste() -> list[WindowInfo]:
    user32, _ = _win_api()
    ergebnis: list[WindowInfo] = []

    @ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
    def callback(hwnd, _):
        if not user32.IsWindowVisible(hwnd):
            return True
        laenge = user32.GetWindowTextLengthW(hwnd)
        if laenge <= 0:
            return True
        buffer = ctypes.create_unicode_buffer(laenge + 1)
        user32.GetWindowTextW(hwnd, buffer, laenge + 1)
        ergebnis.append(WindowInfo(int(hwnd), buffer.value, _process_name(int(hwnd))))
        return True

    user32.EnumWindows(callback, 0)
    return ergebnis


def finde_fenster(titel: str) -> int | None:
    titel = titel.casefold()
    for fenster in fenster_liste():
        if titel in fenster.title.casefold():
            return fenster.hwnd
    return None


def finde_eingabefenster(titel: str = "") -> int | None:
    fenster = fenster_liste()
    terminals = [
        f for f in fenster
        if f.process.casefold() in {"windowsterminal.exe", "opencode.exe"}
        and "emulator.exe" not in f.title.casefold()
        and "android\\sdk\\emulator" not in f.title.casefold()
    ]
    if titel:
        passende = [f for f in terminals if titel.casefold() in f.title.casefold()]
    else:
        passende = terminals
    if not passende:
        return None
    user32, _ = _win_api()
    aktiv = int(user32.GetForegroundWindow())
    aktiver_treffer = next((f.hwnd for f in passende if f.hwnd == aktiv), None)
    if aktiver_treffer:
        return aktiver_treffer
    return passende[0].hwnd if len(passende) == 1 else None


def client_bereich(hwnd: int) -> tuple[int, int, int, int] | None:
    user32, _ = _win_api()
    rect = Rect()
    punkt = Point(0, 0)
    if not user32.GetClientRect(wintypes.HWND(hwnd), ctypes.byref(rect)):
        return None
    if not user32.ClientToScreen(wintypes.HWND(hwnd), ctypes.byref(punkt)):
        return None
    breite, hoehe = rect.right - rect.left, rect.bottom - rect.top
    if breite <= 0 or hoehe <= 0:
        return None
    return punkt.x, punkt.y, breite, hoehe


def videobereich(
    client_breite: int,
    client_hoehe: int,
    display_breite: int,
    display_hoehe: int,
) -> tuple[int, int, int, int]:
    """Berechnet die echte Videoflaeche innerhalb moeglicher schwarzer Raender."""
    faktor = min(client_breite / display_breite, client_hoehe / display_hoehe)
    breite = max(1, round(display_breite * faktor))
    hoehe = max(1, round(display_hoehe * faktor))
    return (client_breite - breite) // 2, (client_hoehe - hoehe) // 2, breite, hoehe


def ist_kasten(x1: int, y1: int, x2: int, y2: int, schwelle: int = ZUG_SCHWELLE) -> bool:
    """Gezogen oder nur gezeigt?

    Eine Achse reicht: eine flache Textzeile zu umfahren ist genauso ein
    Aufziehen wie ein grosses Rechteck. Verlangte man Weg auf beiden Achsen,
    kippte so ein Zug faelschlich in eine Punktauswahl.
    """
    return abs(x2 - x1) >= schwelle or abs(y2 - y1) >= schwelle


def fenstergroesse(
    display_breite: int,
    display_hoehe: int,
    max_breite: int = 1200,
    max_hoehe: int = 1129,
) -> tuple[int, int]:
    """Fenstermass fuer scrcpy, massstabsgetreu zum Geraet.

    Feste Masse passten nur zum zugeklappten Fold 8. Am aufgeklappten Geraet
    (oder an einem beliebigen anderen) entstuenden sonst breite schwarze
    Raender, in die hinein man nicht zeigen kann.
    """
    faktor = min(max_breite / display_breite, max_hoehe / display_hoehe)
    return max(1, round(display_breite * faktor)), max(1, round(display_hoehe * faktor))


def geraetepunkt(
    maus_x: int,
    maus_y: int,
    video_breite: int,
    video_hoehe: int,
    display_breite: int,
    display_hoehe: int,
) -> tuple[int, int]:
    x = max(0, min(display_breite - 1, int(maus_x / video_breite * display_breite)))
    y = max(0, min(display_hoehe - 1, int(maus_y / video_hoehe * display_hoehe)))
    return x, y


def aktuelle_displaygroesse(serial: str) -> tuple[int, int]:
    fenster = adb(serial, "shell", "dumpsys", "window", "displays")
    treffer = re.findall(r"\bcur=(\d+)x(\d+)", fenster)
    if treffer:
        return int(treffer[-1][0]), int(treffer[-1][1])
    groesse = adb(serial, "shell", "wm", "size")
    treffer = re.findall(r"(\d+)x(\d+)", groesse)
    if not treffer:
        raise RuntimeError("Android-Displaygroesse nicht lesbar.")
    return int(treffer[-1][0]), int(treffer[-1][1])


def _bounds_werte(bounds: str) -> tuple[int, int, int, int] | None:
    treffer = BOUNDS_RE.fullmatch(bounds)
    if not treffer:
        return None
    return (
        int(treffer.group(1)),
        int(treffer.group(2)),
        int(treffer.group(3)),
        int(treffer.group(4)),
    )


def _nahe_elemente(xml: str, x: int, y: int, limit: int = 12) -> list[dict]:
    import xml.etree.ElementTree as ET

    try:
        wurzel = ET.fromstring(xml)
    except ET.ParseError:
        return []
    ergebnis: list[tuple[int, dict]] = []
    gesehen: set[tuple[str, str, str]] = set()
    for knoten in wurzel.iter("node"):
        text = knoten.get("text", "")
        desc = knoten.get("content-desc", "")
        res_id = knoten.get("resource-id", "")
        if not (text or desc or res_id):
            continue
        bounds = knoten.get("bounds", "")
        werte = _bounds_werte(bounds)
        if not werte:
            continue
        l, t, r, b = werte
        dx = 0 if l <= x <= r else min(abs(x - l), abs(x - r))
        dy = 0 if t <= y <= b else min(abs(y - t), abs(y - b))
        abstand = dx * dx + dy * dy
        schluessel = (text, desc, bounds)
        if schluessel in gesehen:
            continue
        gesehen.add(schluessel)
        ergebnis.append((abstand, {
            "text": text,
            "desc": desc,
            "res_id": res_id,
            "klasse": knoten.get("class", ""),
            "klickbar": knoten.get("clickable", "false") == "true",
            "bounds": bounds,
        }))
    ergebnis.sort(key=lambda eintrag: eintrag[0])
    return [element for _, element in ergebnis[:limit]]


def _atomar_schreiben(pfad: Path, daten: bytes) -> None:
    pfad.parent.mkdir(parents=True, exist_ok=True)
    temp = pfad.with_name(f".{pfad.name}.{os.getpid()}.tmp")
    try:
        with temp.open("wb") as datei:
            datei.write(daten)
            datei.flush()
            os.fsync(datei.fileno())
        os.replace(temp, pfad)
    finally:
        try:
            temp.unlink()
        except FileNotFoundError:
            pass


def _atomar_json(pfad: Path, inhalt: dict) -> None:
    text = json.dumps(inhalt, ensure_ascii=False, indent=2) + "\n"
    _atomar_schreiben(pfad, text.encode("utf-8"))


def _aktiver_screen(serial: str) -> str:
    ausgabe = adb(serial, "shell", "dumpsys", "window")
    treffer = re.search(r"mCurrentFocus=.*?\bu\d+\s+([^}\s]+)", ausgabe)
    return treffer.group(1) if treffer else ""


def _quelle_als_dict(projekt: Path, fund: tuple[Path, int, str]) -> dict:
    datei, zeile, inhalt = fund
    try:
        relativ = datei.relative_to(projekt)
    except ValueError:
        relativ = datei
    return {
        "datei": str(datei),
        "relativ": str(relativ),
        "zeile": zeile,
        "inhalt": inhalt,
    }


ANWEISUNG_PUNKT = (
    "Beziehe Woerter wie 'dort', 'da' oder 'diese Stelle' in derselben "
    "Benutzernachricht auf diese Auswahl. Pruefe Screenshot, UI-Kontext und "
    "Quellcodekandidaten, bevor du Code aenderst."
)
ANWEISUNG_KASTEN = (
    "Beziehe Woerter wie 'dort', 'das', 'dieser Bereich' oder 'diese Karte' in "
    "derselben Benutzernachricht auf den aufgezogenen Kasten — gemeint ist alles "
    "darin, nicht ein einzelnes Element. 'elemente_im_kasten' listet den Inhalt in "
    "Leserichtung; 'screenshot_ausschnitt' zeigt genau diesen Bereich. Pruefe beides "
    "und die Quellcodekandidaten, bevor du Code aenderst."
)


def auswahl_erfassen(
    serial: str,
    projekt: Path,
    x: int,
    y: int,
    ausgabe: Path,
    nummer: int,
    kasten: tuple[int, int, int, int] | None = None,
) -> tuple[Path, dict]:
    """Friert den vollstaendigen Kontext einer Auswahl unveraenderlich ein.

    Zwei Arten teilen sich denselben Weg: der Punkt und der aufgezogene Kasten.
    Auch beim Kasten wird die Mitte als Punkt mit ausgewertet — so bleibt jedes
    Feld des Schemas belegt, und ein Kasten um ein einzelnes Bedienelement
    liefert weiterhin dessen Kette und klickbaren Rahmen.
    """
    zeit = datetime.now().astimezone()
    kennung = f"auswahl-{zeit:%Y%m%d-%H%M%S}-{zeit.microsecond // 1000:03d}"
    json_pfad = ausgabe / f"{kennung}.json"
    bild_pfad = ausgabe / f"{kennung}.png"
    xml_pfad = ausgabe / f"{kennung}.xml"

    breite, hoehe = aktuelle_displaygroesse(serial)
    xml = ui_baum(serial)
    gefunden = element_an_punkt(xml, x, y)
    kette = elementkette_an_punkt(xml, x, y)
    direkt, klickbar = gefunden if gefunden else ({}, None)
    nahe = _nahe_elemente(xml, x, y)
    im_kasten = elemente_im_bereich(xml, *kasten) if kasten else []

    # Beim Kasten fuehrt sein Inhalt; der Punkt in der Mitte ergaenzt nur.
    # Nur beschriftete Elemente taugen als Suchbegriff, und was ganz im Kasten
    # liegt, ist eher gemeint als ein Rahmen, der bloss hineinragt.
    ergiebig = sorted(
        (e for e in im_kasten if e.get("beschriftet")),
        key=lambda e: not e["vollstaendig_im_kasten"],
    )
    begriffe: list[str] = []
    for element in [*ergiebig[:14], direkt, klickbar, *kette[:5], *nahe[:5]]:
        if element:
            begriffe.extend(suchbegriffe(element))
    begriffe = list(dict.fromkeys(begriffe))
    funde = suche_quellcode(projekt, begriffe, limit=20 if kasten else 12)

    bild = screenshot(serial, (breite, hoehe))
    _atomar_schreiben(bild_pfad, bild)
    _atomar_schreiben(xml_pfad, xml.encode("utf-8"))

    inhalt = {
        "schema": 2,
        "werkzeug": "Zeigefinger",
        "werkzeug_version": VERSION,
        "auswahl_nummer": nummer,
        "art": "kasten" if kasten else "punkt",
        "erfasst_am": zeit.isoformat(timespec="milliseconds"),
        "anweisung_an_opencode": ANWEISUNG_KASTEN if kasten else ANWEISUNG_PUNKT,
        "geraet": serial,
        "projekt": str(projekt),
        "screen": _aktiver_screen(serial),
        "display": {"breite": breite, "hoehe": hoehe},
        "klick": {"x": x, "y": y},
        "direktes_element": direkt or None,
        "klickbarer_rahmen": klickbar,
        "elementkette_klein_nach_gross": kette,
        "elemente_in_der_naehe": nahe,
        "quellcodekandidaten": [_quelle_als_dict(projekt, fund) for fund in funde],
        "screenshot": str(bild_pfad),
        "ui_hierarchie": str(xml_pfad),
    }

    if kasten:
        kx1, ky1, kx2, ky2 = kasten
        schnipsel = ausschnitt(bild, kasten)
        if schnipsel:
            schnipsel_pfad = ausgabe / f"{kennung}-kasten.png"
            _atomar_schreiben(schnipsel_pfad, schnipsel)
            inhalt["screenshot_ausschnitt"] = str(schnipsel_pfad)
        inhalt["kasten"] = {
            "x1": kx1, "y1": ky1, "x2": kx2, "y2": ky2,
            "breite": kx2 - kx1, "hoehe": ky2 - ky1,
        }
        inhalt["elemente_im_kasten"] = im_kasten
        inhalt["elemente_im_kasten_anzahl"] = len(im_kasten)
        # Kurzfassung zum Lesen: was im Kasten steht, in Leserichtung.
        inhalt["text_im_kasten"] = [
            e["text"] or e["desc"] for e in im_kasten if e["text"] or e["desc"]
        ]

    _atomar_json(json_pfad, inhalt)
    _atomar_json(ausgabe / "letzte-auswahl.json", inhalt)
    log("info", "Auswahl gespeichert", auswahl=str(json_pfad), art=inhalt["art"],
        x=x, y=y, kasten=kasten, elemente=len(im_kasten))
    return json_pfad, inhalt


def befehlstext(auswahl_pfad: Path, art: str = "punkt") -> str:
    if art == "kasten":
        return (
            'Lies zuerst dieses Zeigefinger-Ziel und beziehe "dort" auf den ganzen '
            f'darin aufgezogenen Kasten: "{auswahl_pfad}". '
        )
    return f'Lies zuerst dieses Zeigefinger-Ziel und beziehe "dort" darauf: "{auswahl_pfad}". '


def _fensterstil(hwnd: int, klickdurchlaessig: bool, transparent: bool = True) -> None:
    user32, _ = _win_api()
    stil = user32.GetWindowLongW(wintypes.HWND(hwnd), GWL_EXSTYLE)
    stil |= WS_EX_TOOLWINDOW
    if transparent:
        stil |= WS_EX_LAYERED
    else:
        stil &= ~WS_EX_LAYERED
    if klickdurchlaessig:
        stil |= WS_EX_TRANSPARENT
    else:
        stil &= ~WS_EX_TRANSPARENT
    user32.SetWindowLongW(wintypes.HWND(hwnd), GWL_EXSTYLE, stil)


def _tk_fenster(widget) -> int:
    """Tk liefert unter Windows ein Kindfenster; gebraucht wird dessen Top-Level-Wrapper."""
    user32, _ = _win_api()
    hwnd = int(widget.winfo_id())
    parent = int(user32.GetParent(wintypes.HWND(hwnd)) or 0)
    return parent or hwnd


def _an_fenster_koppeln(hwnd: int, eigentuemer: int) -> None:
    """Bettet ein Tk-Fenster als echtes Kind in die scrcpy-Clientflaeche ein."""
    user32, _ = _win_api()
    stil = user32.GetWindowLongW(wintypes.HWND(hwnd), GWL_STYLE)
    user32.SetWindowLongW(
        wintypes.HWND(hwnd),
        GWL_STYLE,
        (stil | WS_CHILD) & ~WS_POPUP,
    )
    user32.SetParent(wintypes.HWND(hwnd), wintypes.HWND(eigentuemer))
    user32.SetLayeredWindowAttributes(wintypes.HWND(hwnd), 0, TOENUNG, LWA_ALPHA)


def _fenster_platzieren(hwnd: int, x: int, y: int, breite: int, hoehe: int) -> None:
    user32, _ = _win_api()
    user32.SetWindowPos(
        wintypes.HWND(hwnd),
        wintypes.HWND(HWND_TOP),
        x,
        y,
        breite,
        hoehe,
        SWP_NOACTIVATE,
    )


def _in_vordergrund(hwnd: int) -> bool:
    user32, kernel32 = _win_api()
    if user32.IsIconic(wintypes.HWND(hwnd)):
        user32.ShowWindow(wintypes.HWND(hwnd), SW_RESTORE)
    ziel_thread = user32.GetWindowThreadProcessId(wintypes.HWND(hwnd), None)
    vordergrund = user32.GetForegroundWindow()
    vordergrund_thread = user32.GetWindowThreadProcessId(vordergrund, None)
    aktuell_thread = kernel32.GetCurrentThreadId()
    verbindungen: list[int] = []
    if vordergrund_thread and vordergrund_thread != aktuell_thread:
        if user32.AttachThreadInput(aktuell_thread, vordergrund_thread, True):
            verbindungen.append(vordergrund_thread)
    if ziel_thread and ziel_thread != aktuell_thread:
        if user32.AttachThreadInput(aktuell_thread, ziel_thread, True):
            verbindungen.append(ziel_thread)
    try:
        user32.BringWindowToTop(wintypes.HWND(hwnd))
        return bool(user32.SetForegroundWindow(wintypes.HWND(hwnd)))
    finally:
        for thread in reversed(verbindungen):
            user32.AttachThreadInput(aktuell_thread, thread, False)


def _steuerung_v_einfuegen() -> None:
    user32, _ = _win_api()
    user32.keybd_event(VK_CONTROL, 0, 0, 0)
    user32.keybd_event(ord("V"), 0, 0, 0)
    user32.keybd_event(ord("V"), 0, KEYEVENTF_KEYUP, 0)
    user32.keybd_event(VK_CONTROL, 0, KEYEVENTF_KEYUP, 0)


def _einzelinstanz() -> object:
    _, kernel32 = _win_api()
    handle = kernel32.CreateMutexW(None, False, MUTEX_NAME)
    if not handle or kernel32.GetLastError() == 183:
        ctypes.windll.user32.MessageBoxW(
            None,
            "Das Zeigefinger-Overlay laeuft bereits.",
            "Zeigefinger",
            0x40,
        )
        raise SystemExit(0)
    return handle


class AuswahlOverlay:
    def __init__(
        self,
        args: argparse.Namespace,
        serial: str,
        display: tuple[int, int],
    ) -> None:
        import tkinter as tk
        from tkinter import messagebox

        self.tk = tk
        self.messagebox = messagebox
        self.args = args
        self.serial = serial
        self.projekt = Path(args.projekt).expanduser().resolve()
        self.ausgabe = Path(args.ausgabe).expanduser().resolve()
        self.eingabe_hwnd = finde_eingabefenster(args.eingabefenster)
        self.scrcpy_hwnd: int | None = finde_fenster(args.fenster)
        self.scrcpy_prozess: subprocess.Popen | None = None
        self.emulator_fenster: list[int] = []
        self.auswahlmodus = False
        self.beschaeftigt = False
        self.nummer = 0
        self.video_geometrie = (0, 0, 1, 1)
        self.display: tuple[int, int] | None = display
        self.fenster_gekoppelt = False
        self.hatte_scrcpy = bool(self.scrcpy_hwnd)
        self.emulator_minimiert = not ist_emulator(serial)
        self.toolbar_sichtbar = False
        self.letzte_root_geometrie = ""
        self.letzte_overlay_geometrie = ""
        self.zug_start: tuple[int, int] | None = None

        self.root = tk.Tk()
        self.root.withdraw()
        self.root.overrideredirect(True)
        self.root.resizable(False, False)
        self.root.attributes("-topmost", True)
        self.root.configure(bg=PANEL)
        self.root.protocol("WM_DELETE_WINDOW", self.beenden)

        self.status = tk.Label(
            self.root,
            text="AUS",
            bg=PANEL,
            fg=TEXT,
            padx=18,
            font=("Segoe UI", 16, "bold"),
        )
        self.status.pack(side="left", fill="y")
        self.schalter = tk.Button(
            self.root,
            text="AUSWAHL AN",
            command=self.auswahl_umschalten,
            bg=ACCENT,
            fg="white",
            activebackground="#ff7658",
            activeforeground="white",
            relief="flat",
            padx=22,
            pady=14,
            font=("Segoe UI", 15, "bold"),
        )
        self.schalter.pack(side="left", padx=(0, 4), pady=4)
        self.schliessen = tk.Button(
            self.root,
            text="×",
            command=self.beenden,
            bg=PANEL,
            fg="#aaaeb7",
            activebackground="#2b2e35",
            activeforeground="white",
            relief="flat",
            width=4,
            font=("Segoe UI", 18, "bold"),
        )
        self.schliessen.pack(side="left", pady=4, padx=(0, 4))

        self.overlay = tk.Toplevel(self.root)  # die getoente Auswahlflaeche
        self.overlay.withdraw()
        self.overlay.overrideredirect(True)
        self.overlay.attributes("-alpha", TOENUNG / 255)
        self.canvas = tk.Canvas(
            self.overlay,
            bg=ACCENT,
            highlightthickness=0,
            cursor="crosshair",
        )
        self.canvas.pack(fill="both", expand=True)
        self.canvas.bind("<Button-1>", self.zug_beginnen)
        self.canvas.bind("<B1-Motion>", self.zug_ziehen)
        self.canvas.bind("<ButtonRelease-1>", self.zug_beenden)
        self.overlay.bind("<Escape>", lambda _: self.auswahl_abbrechen())

        # Der Ziehrahmen bekommt ein eigenes Fenster: nur so bleibt seine Linie
        # schwarz, statt von der Auswahlflaeche verwaschen zu werden.
        self.band, self.band_flaeche = self._klarfenster()
        self.band_sichtbar = False

        self.root.update_idletasks()
        self.root_hwnd = _tk_fenster(self.root)
        self.overlay_hwnd = _tk_fenster(self.overlay)
        _fensterstil(self.root_hwnd, False, transparent=False)
        _fensterstil(self.overlay_hwnd, False)
        # Klickdurchlaessig: beide Rahmen liegen ueber der Auswahlflaeche, duerfen
        # ihr aber weder Maustaste noch Bewegung wegnehmen.
        _fensterstil(_tk_fenster(self.band), True)
        self._scrcpy_starten()
        self.root.after(100, self._fenster_verfolgen)

    def _scrcpy_starten(self) -> None:
        if self.scrcpy_hwnd:
            return
        breite, hoehe = fenstergroesse(*(self.display or (1248, 1972)))
        argumente = [
            "scrcpy",
            "--serial", self.serial,
            "--window-title", self.args.fenster,
            "--window-width", str(breite),
            "--window-height", str(hoehe),
            "--max-fps", "60",
            "--stay-awake",
            "--no-audio",
            "--no-clipboard-autosync",
            "--no-mouse-hover",
        ]
        try:
            self.scrcpy_prozess = subprocess.Popen(
                argumente,
                stdin=subprocess.DEVNULL,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
            )
        except OSError as exc:
            raise SystemExit(f"scrcpy konnte nicht gestartet werden: {exc}") from exc

    def _emulator_minimieren(self) -> bool:
        """Nur sinnvoll, wenn gespiegelt wird, was der Emulator zeigt."""
        user32, _ = _win_api()
        if not ist_emulator(self.serial):
            return True
        if not self.emulator_fenster:
            self.emulator_fenster = [
                fenster.hwnd
                for fenster in fenster_liste()
                if fenster.title.startswith("Android Emulator -")
            ]
        for hwnd in self.emulator_fenster:
            if user32.IsWindow(wintypes.HWND(hwnd)) and not user32.IsIconic(wintypes.HWND(hwnd)):
                user32.ShowWindow(wintypes.HWND(hwnd), SW_MINIMIZE)
        return bool(self.emulator_fenster) and all(
            not user32.IsWindow(wintypes.HWND(hwnd)) or user32.IsIconic(wintypes.HWND(hwnd))
            for hwnd in self.emulator_fenster
        )

    def _fenster_verfolgen(self) -> None:
        if not self.scrcpy_hwnd or not ctypes.windll.user32.IsWindow(self.scrcpy_hwnd):
            self.scrcpy_hwnd = None
            self.fenster_gekoppelt = False
            if self.hatte_scrcpy:
                if self.beschaeftigt:
                    self.root.after(250, self._fenster_verfolgen)
                else:
                    self.beenden()
                return
            if self.scrcpy_prozess and self.scrcpy_prozess.poll() is not None:
                self.messagebox.showerror(
                    "Zeigefinger",
                    "Die scrcpy-Spiegelung konnte nicht gestartet werden.",
                )
                self.beenden()
                return
            self.scrcpy_hwnd = finde_fenster(self.args.fenster)
            if not self.scrcpy_hwnd:
                self.root.after(150, self._fenster_verfolgen)
                return
            self.hatte_scrcpy = True
            self.status.configure(text="AUS")
        if not self.fenster_gekoppelt:
            _an_fenster_koppeln(self.overlay_hwnd, self.scrcpy_hwnd)
            self.fenster_gekoppelt = True
            self.status.configure(text="AUS")
        if not self.emulator_minimiert:
            self.emulator_minimiert = self._emulator_minimieren()

        bereich = client_bereich(self.scrcpy_hwnd)
        if not bereich:
            self.root.after(150, self._fenster_verfolgen)
            return
        if self.display is None:
            self.root.after(150, self._fenster_verfolgen)
            return
        cx, cy, cb, ch = bereich
        if (cb >= ch) != (self.display[0] >= self.display[1]):
            self.display = self.display[1], self.display[0]
        ox, oy, vb, vh = videobereich(cb, ch, *self.display)
        vx, vy = cx + ox, cy + oy
        self.video_geometrie = (vx, vy, vb, vh)

        overlay_geometrie = f"{vb}x{vh}+{ox}+{oy}"
        if overlay_geometrie != self.letzte_overlay_geometrie:
            _fenster_platzieren(self.overlay_hwnd, ox, oy, vb, vh)
            self.letzte_overlay_geometrie = overlay_geometrie
        toolbar_breite = max(460, self.root.winfo_reqwidth())
        toolbar_hoehe = max(78, self.root.winfo_reqheight())
        tx = 20
        ty = 80
        root_geometrie = f"{toolbar_breite}x{toolbar_hoehe}+{tx}+{ty}"
        if root_geometrie != self.letzte_root_geometrie:
            self.root.geometry(root_geometrie)
            self.letzte_root_geometrie = root_geometrie

        if ctypes.windll.user32.IsIconic(wintypes.HWND(self.scrcpy_hwnd)):
            if self.toolbar_sichtbar:
                self.root.withdraw()
                self.overlay.withdraw()
                self.band_verstecken()
                self.toolbar_sichtbar = False
        elif not self.toolbar_sichtbar:
            self.root.deiconify()
            self.toolbar_sichtbar = True
        self.root.after(250, self._fenster_verfolgen)

    def auswahl_umschalten(self) -> None:
        if self.beschaeftigt:
            return
        if self.auswahlmodus:
            self.auswahl_abbrechen()
            return
        self.auswahlmodus = True
        self.status.configure(text="AN · STELLE ANKLICKEN")
        self.schalter.configure(text="AUSWAHL AUS", bg=ACCENT_DARK)
        self.canvas.delete("all")
        self.band_verstecken()
        self.zug_start = None
        self.overlay.deiconify()
        self.overlay.lift()
        self.overlay.focus_force()

    def auswahl_abbrechen(self) -> None:
        self.auswahlmodus = False
        self.zug_start = None
        self.band_verstecken()
        self.overlay.withdraw()
        self.status.configure(text="AUS")
        self.schalter.configure(text="AUSWAHL AN", bg=ACCENT, state="normal")

    def _klarfenster(self):
        """Randloses Fenster, in dem nur das Gezeichnete steht — der Rest ist Luft.

        Die Schluesselfarbe wird durchsichtig gestellt; so bleibt jede Linie in
        ihrer echten Farbe, statt von der Toenung der Auswahlflaeche verwaschen
        zu werden.
        """
        fenster = self.tk.Toplevel(self.root)
        fenster.withdraw()
        fenster.overrideredirect(True)
        fenster.attributes("-topmost", True)
        fenster.configure(bg=SCHLUESSELFARBE)
        fenster.attributes("-transparentcolor", SCHLUESSELFARBE)
        flaeche = self.tk.Canvas(fenster, bg=SCHLUESSELFARBE, highlightthickness=0)
        flaeche.pack(fill="both", expand=True)
        return fenster, flaeche

    def band_zeichnen(self, x1: int, y1: int, x2: int, y2: int, staerke: int) -> None:
        """Setzt das Rahmenfenster auf den aufgezogenen Kasten.

        Gerechnet wird in Bildschirmkoordinaten: die Auswahlflaeche haengt als
        Kindfenster in scrcpy, das Rahmenfenster steht frei darueber.
        """
        vx, vy, _, _ = self.video_geometrie
        kx1, ky1, kx2, ky2 = normiere_kasten(x1, y1, x2, y2)
        rand = staerke + 1
        breite = max(kx2 - kx1, 1) + 2 * rand
        hoehe = max(ky2 - ky1, 1) + 2 * rand
        self.band.geometry(f"{breite}x{hoehe}+{vx + kx1 - rand}+{vy + ky1 - rand}")
        self.band_flaeche.delete("all")
        self.band_flaeche.create_rectangle(
            rand, rand, breite - rand, hoehe - rand,
            outline=RAHMEN_FARBE,
            width=staerke,
        )
        if not self.band_sichtbar:
            self.band.deiconify()
            self.band.lift()
            self.band_sichtbar = True

    def band_verstecken(self) -> None:
        if self.band_sichtbar:
            self.band.withdraw()
            self.band_sichtbar = False

    def zug_beginnen(self, ereignis) -> None:
        if not self.auswahlmodus or self.beschaeftigt:
            return
        self.zug_start = (ereignis.x, ereignis.y)
        self.band_verstecken()

    def zug_ziehen(self, ereignis) -> None:
        if self.zug_start is None or self.beschaeftigt:
            return
        x1, y1 = self.zug_start
        self.band_zeichnen(x1, y1, ereignis.x, ereignis.y, RAHMEN_STAERKE)

    def zug_beenden(self, ereignis) -> None:
        """Ein Zeigen ist ein Punkt, ein Ziehen ein Kasten — entschieden wird am Weg."""
        if self.zug_start is None or not self.auswahlmodus or self.beschaeftigt:
            return
        x1, y1 = self.zug_start
        self.zug_start = None
        x2, y2 = ereignis.x, ereignis.y
        gezogen = ist_kasten(x1, y1, x2, y2)

        self.beschaeftigt = True
        self.schalter.configure(state="disabled")
        self.status.configure(text="KASTEN WIRD GELESEN ..." if gezogen else "WIRD ÜBERGEBEN ...")
        _, _, vb, vh = self.video_geometrie
        if gezogen:
            # Beim Loslassen bleibt der Rahmen schwarz, wird nur etwas kraeftiger:
            # das quittiert die Auswahl, ohne sie schlechter sichtbar zu machen.
            self.band_zeichnen(x1, y1, x2, y2, RAHMEN_STAERKE + 2)
        else:
            self.band_verstecken()
            self.canvas.create_oval(x2 - 12, y2 - 12, x2 + 12, y2 + 12, outline=ACCENT, width=5)

        self.nummer += 1
        threading.Thread(
            target=self._auswahl_worker,
            args=((x2, y2), (x1, y1, x2, y2) if gezogen else None, vb, vh, self.nummer),
            daemon=True,
        ).start()

    def _auswahl_worker(
        self,
        punkt: tuple[int, int],
        kasten_fenster: tuple[int, int, int, int] | None,
        video_breite: int,
        video_hoehe: int,
        nummer: int,
    ) -> None:
        try:
            display = aktuelle_displaygroesse(self.serial)
            x, y = geraetepunkt(*punkt, video_breite, video_hoehe, *display)
            kasten = None
            if kasten_fenster:
                fx1, fy1, fx2, fy2 = kasten_fenster
                gx1, gy1 = geraetepunkt(fx1, fy1, video_breite, video_hoehe, *display)
                gx2, gy2 = geraetepunkt(fx2, fy2, video_breite, video_hoehe, *display)
                kasten = normiere_kasten(gx1, gy1, gx2, gy2)
                # Die Mitte des Kastens vertritt ihn als Punkt.
                x, y = (kasten[0] + kasten[2]) // 2, (kasten[1] + kasten[3]) // 2
            pfad, inhalt = auswahl_erfassen(
                self.serial,
                self.projekt,
                x,
                y,
                self.ausgabe,
                nummer,
                kasten=kasten,
            )
            self.root.after(0, self._auswahl_fertig, pfad, inhalt)
        except (Exception, SystemExit) as exc:
            log("error", "Auswahl fehlgeschlagen", error=str(exc))
            self.root.after(0, self._auswahl_fehler, str(exc))

    def _auswahl_fertig(self, pfad: Path, inhalt: dict) -> None:
        if inhalt.get("art") == "kasten":
            beschriftet = sum(1 for e in inhalt.get("elemente_im_kasten", []) if e.get("beschriftet"))
            name = f"Kasten · {inhalt.get('elemente_im_kasten_anzahl', 0)} Elemente ({beschriftet} benannt)"
        else:
            element = inhalt.get("direktes_element") or {}
            name = element.get("text") or element.get("desc") or element.get("klasse", "Element")
        self.status.configure(text=f"#{self.nummer}: {name[:34]}")
        self.root.clipboard_clear()
        self.root.clipboard_append(befehlstext(pfad, inhalt.get("art", "punkt")))
        self.root.update()
        self.auswahlmodus = False
        self.beschaeftigt = False
        self.schalter.configure(text="Neue Auswahl", bg=ACCENT, state="normal")
        self.root.after(900, self.overlay.withdraw)
        self.root.after(900, self.band_verstecken)

        self.eingabe_hwnd = finde_eingabefenster(self.args.eingabefenster)
        if (
            self.args.nur_zwischenablage
            or not self.eingabe_hwnd
        ):
            self.status.configure(text=f"#{self.nummer} kopiert · Strg+V")
            return
        if _in_vordergrund(self.eingabe_hwnd):
            self.root.after(250, self._einfuegen_wenn_aktiv)
        else:
            self.status.configure(text=f"#{self.nummer} kopiert · Strg+V")

    def _einfuegen_wenn_aktiv(self) -> None:
        user32, _ = _win_api()
        if (
            int(user32.GetForegroundWindow()) != self.eingabe_hwnd
        ):
            self.status.configure(text=f"#{self.nummer} kopiert · Strg+V")
            return
        _steuerung_v_einfuegen()

    def _auswahl_fehler(self, meldung: str) -> None:
        self.beschaeftigt = False
        self.status.configure(text=f"Fehler: {meldung[:30]}")
        self.schalter.configure(text="Erneut", bg=ACCENT, state="normal")

    def beenden(self) -> None:
        if self.beschaeftigt:
            self.status.configure(text="Auswahl wird noch gespeichert ...")
            return
        user32, _ = _win_api()
        for hwnd in self.emulator_fenster:
            if user32.IsWindow(wintypes.HWND(hwnd)):
                user32.ShowWindow(wintypes.HWND(hwnd), SW_RESTORE)
        if self.scrcpy_prozess and self.scrcpy_prozess.poll() is None:
            self.scrcpy_prozess.terminate()
        self.root.destroy()

    def run(self) -> int:
        self.root.mainloop()
        return 0


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--projekt", default=".", help="Android-Projekt mit dem Quellcode")
    parser.add_argument(
        "--serial",
        default="",
        help="ADB-Seriennummer; leer = laufender Emulator, sonst angeschlossenes Geraet",
    )
    parser.add_argument("--fenster", default="Experimente Live", help="Titel der scrcpy-Spiegelung")
    parser.add_argument("--eingabefenster", default="", help="Titelteil des OpenCode-Fensters")
    parser.add_argument(
        "--ausgabe",
        default=str(Path(tempfile.gettempdir()) / "opencode" / "zeigefinger"),
        help="Ordner fuer unveraenderliche Auswahlkontexte",
    )
    parser.add_argument(
        "--nur-zwischenablage",
        action="store_true",
        help="Referenz kopieren, aber nicht automatisch in OpenCode einfuegen",
    )
    parser.add_argument(
        "--einmal",
        nargs=2,
        type=int,
        metavar=("X", "Y"),
        help="Kontext einmalig an einer Android-Koordinate erfassen (ohne Overlay)",
    )
    parser.add_argument(
        "--kasten",
        nargs=4,
        type=int,
        metavar=("X1", "Y1", "X2", "Y2"),
        help="Kontext einmalig fuer einen Android-Bereich erfassen (ohne Overlay)",
    )
    parser.add_argument("--version", action="version", version=f"Zeigefinger {VERSION} ({VERSION_STAND})")
    return parser.parse_args(argv)


def _hinweis(meldung: str) -> None:
    """Unter pythonw.exe gibt es keine Konsole — der Fehler braucht ein Fenster."""
    print(meldung)
    ctypes.windll.user32.MessageBoxW(None, meldung, "Zeigefinger", 0x10)


def geraet_und_display(wunsch: str) -> tuple[str, tuple[int, int]]:
    """Ziel bestimmen und gleich vermessen, bevor die Oberflaeche steht."""
    serial = waehle_geraet(wunsch or None, emulator_zuerst=True)
    display = aktuelle_displaygroesse(serial)
    log("info", "Ziel gewaehlt", serial=serial, display=f"{display[0]}x{display[1]}")
    return serial, display


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    projekt = Path(args.projekt).expanduser().resolve()
    if not projekt.is_dir():
        raise SystemExit(f"Projektordner gibt es nicht: {projekt}")
    if args.einmal or args.kasten:
        kasten = normiere_kasten(*args.kasten) if args.kasten else None
        if kasten:
            punkt = ((kasten[0] + kasten[2]) // 2, (kasten[1] + kasten[3]) // 2)
        else:
            punkt = (args.einmal[0], args.einmal[1])
        pfad, _ = auswahl_erfassen(
            waehle_geraet(args.serial or None, emulator_zuerst=True),
            projekt,
            punkt[0],
            punkt[1],
            Path(args.ausgabe).expanduser().resolve(),
            1,
            kasten=kasten,
        )
        print(pfad)
        return 0
    mutex = _einzelinstanz()
    try:
        try:
            serial, display = geraet_und_display(args.serial)
        except SystemExit as exc:
            _hinweis(str(exc) or "Kein Android-Ziel gefunden.")
            return 1
        except Exception as exc:
            _hinweis(f"Das Android-Ziel ist nicht erreichbar.\n\n{exc}")
            return 1
        return AuswahlOverlay(args, serial, display).run()
    finally:
        ctypes.windll.kernel32.CloseHandle(mutex)


if __name__ == "__main__":
    raise SystemExit(main())
