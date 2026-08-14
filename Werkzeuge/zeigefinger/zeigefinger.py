#!/usr/bin/env python3
"""Zeigefinger - point at a UI element in the scrcpy mirror, get the source location.

Hover the mouse over any element in the scrcpy window and hold still. The tool
reads the live UI tree from the device, resolves which element is under the
cursor and greps the project for the matching source line.

Hovering (not clicking) is deliberate: a real click would navigate the app, so
the UI dump would already describe the *next* screen.

Windows only for the mouse capture (Win32 via ctypes); everything else is
platform neutral.
"""

from __future__ import annotations

import argparse
import ctypes
import io
import json
import os
import re
import struct
import subprocess
import sys
import time
from ctypes import wintypes
from datetime import datetime
from pathlib import Path
from typing import Literal, overload

VERSION = "0.0.7"
VERSION_STAND = "14.08.2026, 21:44 Uhr"

# Umlaute und Kastenlinien muessen auch durch eine Pipe heil ankommen, und die
# Ausgabe soll zeilenweise erscheinen statt am Ende im Block.
try:
    getattr(sys.stdout, "reconfigure")(encoding="utf-8", line_buffering=True)
    getattr(sys.stderr, "reconfigure")(encoding="utf-8")
except (AttributeError, OSError):
    pass

LOG_PATH = Path(os.environ.get("TEMP", "/tmp")) / "zeigefinger.jsonl"
HOVER_MS = 700          # how long the mouse must hold still
POLL_MS = 80            # mouse polling interval
DUMP_MIN_GAP_S = 1.0    # never dump the UI tree more often than this
_sdk_adb = Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk" / "platform-tools" / "adb.exe"
ADB_COMMAND = str(_sdk_adb) if _sdk_adb.is_file() else "adb"

SOURCE_SUFFIXES = (".kt", ".java", ".xml")
SKIP_DIRS = {"build", ".gradle", ".git", ".idea", "node_modules", "__pycache__"}


# --------------------------------------------------------------------------- log
def log(level: str, msg: str, **ctx) -> None:
    rec = {
        "ts": datetime.now().astimezone().isoformat(timespec="seconds"),
        "level": level,
        "module": "zeigefinger",
        "msg": msg,
        "ctx": ctx,
    }
    try:
        with LOG_PATH.open("a", encoding="utf-8", newline="\n") as fh:
            fh.write(json.dumps(rec, ensure_ascii=False) + "\n")
    except OSError:
        pass  # logging must never break the tool


def probe(condition: bool, msg: str, **ctx) -> bool:
    """Logic probe: records a violated expectation without stopping the run."""
    if not condition:
        log("warn", f"PROBE: {msg}", **ctx)
    return condition


# --------------------------------------------------------------------------- adb
@overload
def adb(serial: str | None, *args: str, binary: Literal[True]) -> bytes: ...


@overload
def adb(serial: str | None, *args: str, binary: Literal[False] = False) -> str: ...


def adb(serial: str | None, *args: str, binary: bool = False) -> bytes | str:
    cmd = [ADB_COMMAND]
    if serial:
        cmd += ["-s", serial]
    cmd += list(args)
    try:
        out = subprocess.run(
            cmd,
            capture_output=True,
            timeout=25,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        log("error", "adb call failed", cmd=" ".join(args), error=str(exc))
        raise SystemExit(f"adb-Aufruf fehlgeschlagen: {exc}")
    if out.returncode != 0:
        fehler = out.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"adb-Aufruf fehlgeschlagen ({out.returncode}): {fehler or 'ohne Meldung'}")
    if binary:
        return out.stdout
    return out.stdout.decode("utf-8", errors="replace")


def ist_emulator(serial: str) -> bool:
    return serial.startswith("emulator-")


def verbundene_geraete() -> list[str]:
    """Alle betriebsbereiten ADB-Ziele, Emulatoren eingeschlossen."""
    zeilen = adb(None, "devices").splitlines()[1:]
    return [z.split()[0] for z in zeilen if re.search(r"\s+device\b", z)]


def sortiere_geraete(geraete: list[str], emulator_zuerst: bool) -> list[str]:
    emulatoren = [g for g in geraete if ist_emulator(g)]
    echte = [g for g in geraete if not ist_emulator(g)]
    return emulatoren + echte if emulator_zuerst else echte + emulatoren


def waehle_geraet(wunsch: str | None, emulator_zuerst: bool = False) -> str:
    """Zielgeraet bestimmen — mit Rueckfall auf die jeweils andere Sorte.

    Emulator und angeschlossenes Geraet sind gleichwertige Ziele; nur die
    Reihenfolge unterscheidet sich. Das Overlay bevorzugt den laufenden
    Emulator, faellt aber auf das angesteckte Geraet zurueck, sobald keiner
    laeuft — der Hover-Modus umgekehrt. Ohne Rueckfall waere das Werkzeug an
    genau eine Betriebsart gebunden.
    """
    geraete = verbundene_geraete()
    if wunsch:
        if wunsch not in geraete:
            raise SystemExit(
                f"Geraet '{wunsch}' ist nicht verbunden. Gefunden: {geraete or 'keins'}"
            )
        return wunsch
    reihenfolge = sortiere_geraete(geraete, emulator_zuerst)
    if not reihenfolge:
        raise SystemExit(
            "Kein Android-Ziel gefunden: weder ein laufender Emulator noch ein "
            "angeschlossenes Geraet. Pruefe 'adb devices' und das USB-Debugging."
        )
    if len(reihenfolge) > 1:
        log("info", "mehrere Geraete", geraete=reihenfolge, gewaehlt=reihenfolge[0])
        print(f"Mehrere Ziele: {reihenfolge} — nehme {reihenfolge[0]}. Mit --serial festlegen.")
    return reihenfolge[0]


def finde_geraet(wunsch: str | None) -> str:
    """Hover-Modus: das angeschlossene Geraet gewinnt, der Emulator ist Rueckfall."""
    return waehle_geraet(wunsch, emulator_zuerst=False)


def displaygroesse(serial: str) -> tuple[int, int]:
    """Read the *current* display size, so folding the device is picked up live."""
    text = adb(serial, "shell", "wm", "size")
    treffer = re.findall(r"(\d+)x(\d+)", text)
    if not treffer:
        raise SystemExit("Displaygroesse nicht lesbar (wm size).")
    # "Override size" wins over "Physical size" when both are present.
    b, h = treffer[-1]
    return int(b), int(h)


PNG_KENNUNG = b"\x89PNG\r\n\x1a\n"


def png_zuschneiden(roh: bytes) -> bytes | None:
    """Schneidet alles vor der PNG-Signatur weg.

    Ein Foldable hat zwei Displays. Ohne '-d' stellt 'screencap' dem Bild eine
    mehrzeilige Warnung voran — auf dem Emulator (nur ein Display) fehlt sie.
    Genau daran ist die Erfassung am echten Geraet gescheitert.
    """
    start = roh.find(PNG_KENNUNG)
    return roh[start:] if start != -1 else None


def png_masse(daten: bytes) -> tuple[int, int] | None:
    """Breite und Hoehe aus dem IHDR direkt hinter der Signatur."""
    if len(daten) < 24:
        return None
    breite, hoehe = struct.unpack(">II", daten[16:24])
    return int(breite), int(hoehe)


def display_kennungen(serial: str) -> list[str]:
    try:
        text = adb(serial, "shell", "dumpsys", "SurfaceFlinger", "--display-id")
    except RuntimeError:
        return []
    return re.findall(r"^Display (\d+)", text, re.MULTILINE)


def _screencap(serial: str, kennung: str | None) -> bytes | None:
    args = ["exec-out", "screencap", "-p"]
    if kennung:
        args += ["-d", kennung]
    try:
        return png_zuschneiden(adb(serial, *args, binary=True))
    except RuntimeError:
        return None


def screenshot(serial: str, erwartet: tuple[int, int] | None = None) -> bytes:
    """Bild des *aktiven* Displays.

    Das Standard-Display ist laut screencap-Warnung nicht verlaesslich dasselbe
    wie das gerade benutzte. Deshalb wird gegen die bekannte Displaygroesse
    geprueft und notfalls jede Display-Kennung durchprobiert.
    """
    bild = _screencap(serial, None)
    if bild and (erwartet is None or png_masse(bild) in (erwartet, tuple(reversed(erwartet)))):
        return bild
    for kennung in display_kennungen(serial):
        andere = _screencap(serial, kennung)
        if andere and png_masse(andere) in (erwartet, tuple(reversed(erwartet))):
            log("info", "Display gezielt gewaehlt", display=kennung)
            return andere
    if bild:
        probe(False, "Screenshot passt nicht zur Displaygroesse", erwartet=erwartet,
              gemessen=png_masse(bild))
        return bild
    raise RuntimeError("Android-Screenshot konnte nicht gelesen werden.")


def ausschnitt(bild: bytes, kasten: tuple[int, int, int, int]) -> bytes | None:
    """Schneidet den aufgezogenen Kasten aus dem Screenshot.

    Braucht Pillow. Fehlt es, bleibt es beim Vollbild — die Auswahl selbst
    haengt nicht daran, nur das zusaetzliche Ausschnittsbild entfaellt.
    """
    try:
        from PIL import Image
    except ImportError:
        log("warn", "Pillow fehlt — Auswahl ohne Ausschnittsbild")
        return None
    kx1, ky1, kx2, ky2 = kasten
    try:
        with Image.open(io.BytesIO(bild)) as offen:
            breite, hoehe = offen.size
            gekappt = (max(0, kx1), max(0, ky1), min(breite, kx2), min(hoehe, ky2))
            if gekappt[2] - gekappt[0] < 2 or gekappt[3] - gekappt[1] < 2:
                return None
            puffer = io.BytesIO()
            offen.crop(gekappt).save(puffer, format="PNG")
            return puffer.getvalue()
    except OSError as exc:
        log("warn", "Ausschnitt nicht erzeugbar", error=str(exc))
        return None


def ui_baum(serial: str) -> str:
    try:
        roh = adb(serial, "exec-out", "uiautomator", "dump", "/dev/tty", binary=True)
    except RuntimeError:
        roh = b""
    text = roh.decode("utf-8", errors="replace")
    ende = text.rfind("</hierarchy>")
    if ende != -1:
        start = text.find("<?xml")
        return text[start if start != -1 else 0 : ende + len("</hierarchy>")]
    # Fallback: some ROMs refuse /dev/tty — go via the sdcard.
    adb(serial, "shell", "uiautomator", "dump", "/sdcard/zeigefinger.xml")
    return adb(serial, "exec-out", "cat", "/sdcard/zeigefinger.xml", binary=True).decode(
        "utf-8", errors="replace"
    )


# ----------------------------------------------------------------------- element
BOUNDS_RE = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


def _als_dict(knoten) -> dict:
    return {
        "text": knoten.get("text", ""),
        "desc": knoten.get("content-desc", ""),
        "res_id": knoten.get("resource-id", ""),
        "klasse": knoten.get("class", ""),
        "klickbar": knoten.get("clickable", "false") == "true",
        "bounds": knoten.get("bounds", ""),
    }


def elementkette_an_punkt(xml: str, x: int, y: int) -> list[dict]:
    """Return all UI nodes under a point, ordered from smallest to largest."""
    import xml.etree.ElementTree as ET

    try:
        wurzel = ET.fromstring(xml)
    except ET.ParseError as exc:
        log("error", "UI-XML nicht parsebar", error=str(exc))
        return []

    kette: list[tuple[int, object]] = []
    for knoten in wurzel.iter("node"):
        m = BOUNDS_RE.fullmatch(knoten.get("bounds", ""))
        if not m:
            continue
        l, t, r, b = (int(v) for v in m.groups())
        if not (l <= x <= r and t <= y <= b):
            continue
        flaeche = (r - l) * (b - t)
        if flaeche > 0:
            kette.append((flaeche, knoten))

    kette.sort(key=lambda p: p[0])
    return [_als_dict(knoten) for _, knoten in kette]


def _knoten_bounds(knoten) -> tuple[int, int, int, int] | None:
    m = BOUNDS_RE.fullmatch(knoten.get("bounds", ""))
    if not m:
        return None
    l, t, r, b = (int(v) for v in m.groups())
    return (l, t, r, b) if r > l and b > t else None


def normiere_kasten(x1: int, y1: int, x2: int, y2: int) -> tuple[int, int, int, int]:
    """Ecken sortieren — gezogen wird in jede der vier Richtungen."""
    return min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2)


def elemente_im_bereich(xml: str, x1: int, y1: int, x2: int, y2: int) -> list[dict]:
    """Alle Elemente, die der aufgezogene Kasten beruehrt.

    Ausgegeben wird in Leserichtung (oben nach unten, dann links nach rechts),
    denn der Kasten umfasst meist eine Karte oder Zeilengruppe — deren
    Reihenfolge auf dem Schirm ist die verstaendlichste. Ob ein Element ganz
    im Kasten liegt oder nur hineinragt, steht als Merkmal dabei: der Rahmen
    einer Liste ragt fast immer heraus, ihre Eintraege nicht.
    """
    import xml.etree.ElementTree as ET

    kx1, ky1, kx2, ky2 = normiere_kasten(x1, y1, x2, y2)
    try:
        wurzel = ET.fromstring(xml)
    except ET.ParseError as exc:
        log("error", "UI-XML nicht parsebar", error=str(exc))
        return []

    treffer: list[tuple[int, int, dict]] = []
    gesehen: set[tuple[str, str, str]] = set()
    for knoten in wurzel.iter("node"):
        werte = _knoten_bounds(knoten)
        if not werte:
            continue
        l, t, r, b = werte
        if r < kx1 or l > kx2 or b < ky1 or t > ky2:
            continue
        eintrag = _als_dict(knoten)
        schluessel = (eintrag["text"], eintrag["desc"], eintrag["bounds"])
        if schluessel in gesehen:
            continue
        gesehen.add(schluessel)
        eintrag["vollstaendig_im_kasten"] = l >= kx1 and t >= ky1 and r <= kx2 and b <= ky2
        eintrag["beschriftet"] = bool(eintrag["text"] or eintrag["desc"] or eintrag["res_id"])
        treffer.append((t, l, eintrag))

    treffer.sort(key=lambda p: (p[0], p[1]))
    return [eintrag for _, _, eintrag in treffer]


def element_an_punkt(xml: str, x: int, y: int) -> tuple[dict, dict | None] | None:
    """Innermost node under the point, plus the nearest clickable container.

    Both matter: the innermost node carries the look (colour, icon, label), the
    clickable ancestor carries the behaviour (onClick). Sorting every containing
    node by area gives us the ancestor chain without building a parent map —
    ancestors are always larger than what they contain.
    """
    kette = elementkette_an_punkt(xml, x, y)
    if not kette:
        return None
    innerstes = kette[0]
    klickbar = None
    for kandidat in kette:
        if kandidat["klickbar"]:
            if kandidat["bounds"] != innerstes["bounds"]:
                klickbar = kandidat
            break
    return innerstes, klickbar


def suchbegriffe(el: dict, zusatz: dict | None = None) -> list[str]:
    """Search keys, strongest identifier first."""
    begriffe = []
    for quelle in (el, zusatz):
        if not quelle:
            continue
        if quelle["res_id"]:
            begriffe.append(quelle["res_id"].split("/")[-1])
        if quelle["desc"]:
            begriffe.append(quelle["desc"])
        if quelle["text"]:
            begriffe.append(quelle["text"])
    return [b for b in dict.fromkeys(begriffe) if len(b.strip()) >= 2]


# ------------------------------------------------------------------------ quelle
def quelldateien(projekt: Path):
    for ordner, unter, dateien in os.walk(projekt):
        unter[:] = [u for u in unter if u not in SKIP_DIRS and not u.startswith(".")]
        for d in dateien:
            if d.endswith(SOURCE_SUFFIXES):
                yield Path(ordner) / d


KOMMENTAR_RE = re.compile(r"^\s*(//|\*|/\*|<!--)")
UI_KONTEXT_RE = re.compile(
    r"\b(Text|Icon|Button|IconButton|Label|Tab|contentDescription|stringResource"
    r"|title|label|placeholder|android:text|android:contentDescription)\b"
)
UI_ORDNER_RE = re.compile(r"[\\/](presentation|ui|screens?|compose|view)[\\/]", re.IGNORECASE)


def _bewerte(datei: Path, zeile: str, stark: bool) -> int:
    """Rank a hit: real UI definitions should beat prose that mentions the word."""
    score = 10 if stark else 0
    if KOMMENTAR_RE.match(zeile):
        score -= 8
    if UI_KONTEXT_RE.search(zeile):
        score += 5
    if UI_ORDNER_RE.search(str(datei)):
        score += 3
    if datei.suffix in (".kt", ".java"):
        score += 1
    return score


def suche_quellcode(projekt: Path, begriffe: list[str], limit: int = 6) -> list[tuple[Path, int, str]]:
    """Find where the element is defined.

    Two passes. First only string literals ("Text") and XML values (>Text<) —
    that alone filters out prose in comments. Only if that finds nothing do we
    fall back to a free text search, scored so comments sink to the bottom.
    Hits inside strings.xml are resolved further to their R.string.<name> use.
    """
    if not begriffe:
        return []

    bewertet: list[tuple[int, Path, int, str]] = []
    string_keys: set[str] = set()

    stark = [(re.compile(r'"' + re.escape(b) + r'"'),
              re.compile(r">\s*" + re.escape(b) + r"\s*<")) for b in begriffe]
    frei = [re.compile(re.escape(b)) for b in begriffe]

    dateien = list(quelldateien(projekt))

    def durchsuchen(muster_fuer_datei, ist_stark: bool) -> None:
        for datei in dateien:
            try:
                inhalt = datei.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            rx_liste = muster_fuer_datei(datei)
            for nr, zeile in enumerate(inhalt.splitlines(), 1):
                if not any(rx.search(zeile) for rx in rx_liste):
                    continue
                bewertet.append((_bewerte(datei, zeile, ist_stark), datei, nr, zeile.strip()))
                if datei.name == "strings.xml":
                    key = re.search(r'name="([^"]+)"', zeile)
                    if key:
                        string_keys.add(key.group(1))

    durchsuchen(lambda d: [m for paar in stark for m in paar], True)
    if not bewertet:
        durchsuchen(lambda d: frei, False)

    # Resolve string resources to where they are actually used.
    if string_keys:
        rx2 = re.compile(r"R\.string\.(" + "|".join(map(re.escape, string_keys)) + r")\b")
        for datei in dateien:
            if datei.suffix not in (".kt", ".java"):
                continue
            try:
                inhalt = datei.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            for nr, zeile in enumerate(inhalt.splitlines(), 1):
                if rx2.search(zeile):
                    bewertet.append((_bewerte(datei, zeile, True) + 4, datei, nr, zeile.strip()))

    bewertet.sort(key=lambda t: (-t[0], str(t[1]), t[2]))
    return [(d, n, z) for _, d, n, z in bewertet[:limit]]


# -------------------------------------------------------------------- maus (win)
class _POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]


class _RECT(ctypes.Structure):
    _fields_ = [("left", ctypes.c_long), ("top", ctypes.c_long),
                ("right", ctypes.c_long), ("bottom", ctypes.c_long)]


def _user32():
    if sys.platform != "win32":
        raise SystemExit(
            "Die Maus-Erfassung gibt es bisher nur unter Windows.\n"
            "Auf macOS/Linux stattdessen --punkt X Y benutzen."
        )
    return ctypes.windll.user32


def finde_fenster(titel_teil: str) -> int:
    u = _user32()
    gefunden: list[int] = []

    @ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
    def rueckruf(hwnd, _):
        laenge = u.GetWindowTextLengthW(hwnd)
        if laenge:
            puffer = ctypes.create_unicode_buffer(laenge + 1)
            u.GetWindowTextW(hwnd, puffer, laenge + 1)
            if titel_teil.lower() in puffer.value.lower():
                gefunden.append(hwnd)
        return True

    u.EnumWindows(rueckruf, 0)
    if not gefunden:
        raise SystemExit(
            f"Kein Fenster mit '{titel_teil}' im Titel gefunden.\n"
            f"Laeuft scrcpy? Start z.B.: scrcpy --serial <SN> --window-title Fold8Live --max-size 1000"
        )
    return gefunden[0]


def maus_im_fenster(hwnd: int) -> tuple[int, int, int, int] | None:
    """Cursor position relative to the window's client area, plus its size."""
    u = _user32()
    p = _POINT()
    if not u.GetCursorPos(ctypes.byref(p)):
        return None
    if not u.ScreenToClient(hwnd, ctypes.byref(p)):
        return None
    r = _RECT()
    if not u.GetClientRect(hwnd, ctypes.byref(r)):
        return None
    breite, hoehe = r.right, r.bottom
    if breite <= 0 or hoehe <= 0:
        return None
    if not (0 <= p.x < breite and 0 <= p.y < hoehe):
        return None
    return p.x, p.y, breite, hoehe


# -------------------------------------------------------------------- ausgabe
def _name(el: dict) -> str:
    return el["text"] or el["desc"] or el["res_id"].split("/")[-1] or "(ohne Beschriftung)"


def zeige(el: dict, klickbar: dict | None, gx: int, gy: int,
          funde: list[tuple[Path, int, str]], projekt: Path) -> None:
    print()
    print("─" * 78)
    knopf = "🔘" if el["klickbar"] or klickbar else "⬜"
    print(f"{knopf}  {_name(el)}")
    print(f"    Position {gx},{gy}   Bereich {el['bounds']}   {el['klasse'].split('.')[-1]}")
    if el["desc"] and el["desc"] != _name(el):
        print(f"    Beschreibung: {el['desc']}")
    if klickbar:
        print(f"    Klickbarer Rahmen: {_name(klickbar)}  {klickbar['bounds']}"
              f"  {klickbar['klasse'].split('.')[-1]}")
    if not funde:
        if el["text"] or el["desc"]:
            print("    ⚠ Keine Codestelle — die Beschriftung steht so nicht im Quellcode.")
            print("      Sie kommt vermutlich aus der Datenbank oder vom Benutzer. Such")
            print("      stattdessen die umgebende Liste oder Karte.")
        else:
            print("    ⚠ Keine Codestelle — das Element hat weder Text noch contentDescription.")
            print("      Beschreib es mir in Worten, dann finde ich es ueber den Aufbau.")
    else:
        print()
        for datei, nr, zeile in funde:
            try:
                rel = datei.relative_to(projekt)
            except ValueError:
                rel = datei
            gekuerzt = zeile if len(zeile) <= 68 else zeile[:65] + "..."
            print(f"    → {rel}:{nr}")
            print(f"      {gekuerzt}")
    print("─" * 78)


# ----------------------------------------------------------------------- ablauf
def main() -> int:
    ap = argparse.ArgumentParser(
        description="Zeigt zu einem UI-Element im scrcpy-Fenster die passende Codestelle."
    )
    ap.add_argument("--projekt", default=".", help="Projektordner mit dem Quellcode")
    ap.add_argument("--fenster", default="Fold8Live", help="Titel(teil) des scrcpy-Fensters")
    ap.add_argument("--serial", default=None, help="ADB-Seriennummer (sonst automatisch)")
    ap.add_argument("--punkt", nargs=2, type=int, metavar=("X", "Y"),
                    help="Einmalig: Geraete-Koordinate statt Maus (auch ohne Windows)")
    ap.add_argument("--version", action="version",
                    version=f"Zeigefinger {VERSION} ({VERSION_STAND})")
    args = ap.parse_args()

    print(f"Zeigefinger {VERSION} ({VERSION_STAND})")

    projekt = Path(args.projekt).expanduser().resolve()
    if not projekt.is_dir():
        raise SystemExit(f"Projektordner gibt es nicht: {projekt}")

    serial = finde_geraet(args.serial)
    breite, hoehe = displaygroesse(serial)
    log("info", "gestartet", serial=serial, display=f"{breite}x{hoehe}", projekt=str(projekt))
    print(f"Log: {LOG_PATH}")
    print(f"Geraet {serial} — Display {breite}x{hoehe} — Projekt {projekt.name}")

    # Single shot: caller already knows the device coordinate.
    if args.punkt:
        gx, gy = args.punkt
        gefunden = element_an_punkt(ui_baum(serial), gx, gy)
        if not gefunden:
            print(f"Kein Element an {gx},{gy}.")
            return 1
        el, klickbar = gefunden
        zeige(el, klickbar, gx, gy, suche_quellcode(projekt, suchbegriffe(el, klickbar)), projekt)
        return 0

    hwnd = finde_fenster(args.fenster)
    print(f"Fenster '{args.fenster}' gefunden.\n")
    print("Fahr mit der Maus auf ein Element und halt kurz still (kein Klick noetig).")
    print("Beenden mit Strg+C.\n")

    letzte_pos: tuple[int, int] | None = None
    ruht_seit: float | None = None
    letztes_element: str | None = None
    letzter_dump = 0.0

    try:
        while True:
            time.sleep(POLL_MS / 1000)
            treffer = maus_im_fenster(hwnd)
            if treffer is None:
                letzte_pos, ruht_seit = None, None
                continue

            mx, my, fb, fh = treffer
            if letzte_pos is None or abs(mx - letzte_pos[0]) > 2 or abs(my - letzte_pos[1]) > 2:
                letzte_pos, ruht_seit = (mx, my), time.time()
                continue
            if ruht_seit is None or (time.time() - ruht_seit) * 1000 < HOVER_MS:
                continue
            if time.time() - letzter_dump < DUMP_MIN_GAP_S:
                continue

            ruht_seit = None
            letzter_dump = time.time()

            # Re-read the size every time: folding changes it mid-session.
            breite, hoehe = displaygroesse(serial)
            gx = int(mx / fb * breite)
            gy = int(my / fh * hoehe)
            probe(0 <= gx <= breite and 0 <= gy <= hoehe,
                  "Koordinate ausserhalb des Displays", gx=gx, gy=gy, w=breite, h=hoehe)

            gefunden = element_an_punkt(ui_baum(serial), gx, gy)
            if gefunden is None:
                continue
            el, klickbar = gefunden
            kennung = f"{el['bounds']}|{el['text']}|{el['desc']}"
            if kennung == letztes_element:
                continue
            letztes_element = kennung

            funde = suche_quellcode(projekt, suchbegriffe(el, klickbar))
            log("info", "element aufgeloest", element=kennung, treffer=len(funde))
            zeige(el, klickbar, gx, gy, funde, projekt)
    except KeyboardInterrupt:
        print("\nBeendet.")
        return 0


if __name__ == "__main__":
    sys.exit(main())
