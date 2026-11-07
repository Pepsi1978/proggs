"""Anhänge eines Programmierauftrags vom Handy: holen, für die Sitzung beschreiben, in die laufende CLI einfügen.

Das Handy legt jede Datei unter ihrem SHA-256 beim Relay ab (PUT /geraet/anhang/<sha>) und nennt im Auftrag oder Nachtrag nur
[{"sha", "name"}]. Die Brücke holt die Dateien vor dem Start, der Hook bei Nachträgen (GET /pc/anhang/<sha>), parallel und mit
Prüfsumme, nach ~/.jarvis-anhaenge/<Auftrag>/. Auf jedem Weg steht der absolute Pfad im Text der Sitzung; das ist der Rückweg,
der auf jedem Rechner und in jeder CLI funktioniert.

Claude Code macht aus einem Bildpfad nur dann ein echtes [Image #n], wenn er als Bracketed Paste in der Eingabe landet. Ein
Hook hat kein eigenes Terminal (Claude Code startet ihn mit setsid, /dev/tty und TIOCSTI scheitern, Test vom 31.10.2026). Darum:
tmux: paste-buffer -p in den Bereich der Sitzung. macOS: AppleScript „do script“ in den Terminal-Tab mit dem tty der Sitzung
(ohne Fokuswechsel). Windows: WriteConsoleInput in die Konsole des Claude-Prozesses. Rein in Python ohne Abhängigkeiten, läuft
auf allen drei Rechnern (Aufruf von der Windows-Brücke: siehe unten).
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import time
import unicodedata
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ORDNER = Path.home() / ".jarvis-anhaenge"
BILDER = {".jpg", ".jpeg", ".png", ".gif", ".webp"}
# Claude Code hängt diese Dateien bei @pfad in der ersten Nachricht selbst an; alles andere öffnet die Sitzung über den Pfad.
ERWAEHNEN = {".pdf", ".txt", ".md", ".csv", ".json", ".xml", ".html", ".htm", ".log", ".kt", ".kts", ".java", ".py", ".js", ".ts", ".yaml", ".yml",
             ".ini", ".conf", ".cfg", ".sql", ".sh", ".ps1", ".cs", ".swift", ".gradle", ".properties", ".srt", ".vtt"}
SHA = re.compile(r"^[0-9a-f]{64}$")
ESC = "\x1b"


def sicherer_name(name: str, nummer: int) -> str:
    """ASCII ohne Leerzeichen: Mit Umlauten oder Leerzeichen bricht sowohl die @-Erwähnung als auch das Erkennen eines eingefügten Pfads."""
    roh = unicodedata.normalize("NFKD", str(name)).encode("ascii", "ignore").decode("ascii")
    sauber = re.sub(r"[^A-Za-z0-9._-]", "_", Path(roh).name).strip("._") or "anhang"
    return f"{nummer:02d}_{sauber[-120:]}"


def _hole_eine(host: str, token: str, sha: str, ziel: Path) -> None:
    pruefung = hashlib.sha256()
    teil = ziel.with_name(ziel.name + ".teil")
    anfrage = urllib.request.Request(f"https://{host}/pc/anhang/{sha}", headers={"X-Jarvis-Token": token})
    with urllib.request.urlopen(anfrage, timeout=30) as antwort, open(teil, "wb") as f:
        while stueck := antwort.read(1024 * 1024):
            pruefung.update(stueck)
            f.write(stueck)
    if pruefung.hexdigest() != sha:
        teil.unlink(missing_ok=True)
        raise ValueError("Prüfsumme passt nicht")
    os.replace(teil, ziel)


def _pruefsumme(pfad: Path) -> str:
    pruefung = hashlib.sha256()
    with open(pfad, "rb") as f:
        while stueck := f.read(1024 * 1024):
            pruefung.update(stueck)
    return pruefung.hexdigest()


def hole(host: str, token: str, nummer: str, anhaenge: list, unterordner: str = "start") -> tuple[list[Path], list[str]]:
    """Lädt die Anhänge parallel. Liefert (Pfade in Franks Reihenfolge, Namen der nicht geladenen)."""
    ordner = ORDNER / re.sub(r"[^A-Za-z0-9_-]", "_", nummer or "ohne") / re.sub(r"[^A-Za-z0-9_-]", "_", unterordner)
    ordner.mkdir(parents=True, exist_ok=True)
    gueltig = [(i + 1, a) for i, a in enumerate(anhaenge or []) if isinstance(a, dict) and SHA.match(str(a.get("sha", "")))]

    def eine(eintrag):
        i, a = eintrag
        ziel = ordner / sicherer_name(a.get("name", ""), i)
        if ziel.is_file() and _pruefsumme(ziel) == a["sha"]:
            return ziel
        for versuch in range(3):
            try:
                _hole_eine(host, token, a["sha"], ziel)
                return ziel
            except Exception:
                time.sleep(1 + versuch)
        return None

    with ThreadPoolExecutor(max_workers=4) as pool:
        ergebnis = list(pool.map(eine, gueltig))
    return [p for p in ergebnis if p], [str(a.get("name", "")) for (_, a), p in zip(gueltig, ergebnis) if not p]


def ist_bild(pfad: Path) -> bool:
    return pfad.suffix.lower() in BILDER


def _erwaehnen(pfad: Path, alle: list[Path]) -> bool:
    """@pfad nur, wo es hilft: nicht bei PDFs mit Seitenbildern (der Inhalt käme doppelt) und nicht bei großen Textdateien (die blähten die erste Nachricht auf)."""
    if pfad.suffix.lower() not in ERWAEHNEN:
        return False
    if pfad.suffix.lower() == ".pdf":
        return not any(f"{pfad.stem.split('_', 1)[-1]}_Seite_" in p.name for p in alle)
    return pfad.stat().st_size <= 200_000


def textblock(pfade: list[Path], fehlend: list[str], einfuegen: bool, erwaehnen: bool = False) -> str:
    """Der Teil für den Text der Sitzung. [einfuegen]: Die Bilder kommen zusätzlich als eigene Eingabe mit [Image #n].
    [erwaehnen]: PDFs und Textdateien als @pfad (nur in der ersten Nachricht: Claude Code hängt sie dort selbst an)."""
    if not pfade and not fehlend:
        return ""
    zeilen = ["ANHÄNGE VON FRANK (vom Handy, liegen auf diesem Rechner, gehören zu genau diesem Auftrag und sind maßgeblich):"]
    zeilen += [f"- {'@' if erwaehnen and _erwaehnen(p, pfade) else ''}{p}" for p in pfade]
    zeilen += [f"- NICHT ÜBERTRAGEN: {n} (melde das Frank)" for n in fehlend]
    if einfuegen and any(ist_bild(p) for p in pfade):
        zeilen.append("Die Bilder kommen als echte Bilder ([Image #n]) in dieser oder der nächsten Eingabe. Sie müssen dir vorliegen, bevor du etwas änderst: "
                      "Fehlen sie nach deinem ersten Schritt, öffne sie mit Read. PDFs und andere Dateien öffnest du über den Pfad.")
    else:
        zeilen.append("Öffne alle Anhänge über den Pfad (Bilder und PDFs mit deinem Lese-Werkzeug), bevor du anfängst.")
    if any("_Seite_" in p.name for p in pfade):
        zeilen.append("Dateien mit _Seite_ im Namen sind die Seiten eines PDFs als Bilder (höchstens die ersten 20 Seiten); weitere Seiten stehen im PDF selbst.")
    return "\n".join(zeilen)


def eingabe_fuer(pfade: list[Path], vorspann: str) -> tuple[str, int]:
    """Die ganze Eingabe in einem Stück (Vorspann und je Bild ein Bracketed Paste) und die Zahl der Bilder. Nur Bilder: PDFs und andere
    Dateien erkennt Claude Code beim Einfügen nicht, und eine @-Erwähnung wertet es in einer eingereihten Eingabe nicht aus."""
    bilder = [p for p in pfade if ist_bild(p)]
    return _kopf(_sauber(vorspann)) + "".join(f"{ESC}[200~{p}{ESC}[201~ " for p in bilder), len(bilder)


def _mehrzeilig(vorspann: str) -> bool:
    return "\n" in vorspann or len(vorspann) > 300


def _sauber(text: str) -> str:
    """Ohne Steuerzeichen: Eine Endmarke oder Steuersequenz im Text bräche sonst aus dem Einfügen aus und wirkte wie Tastatureingaben."""
    return re.sub(r"[\x00-\x08\x0b-\x1f\x7f-\x9f]", "", text.replace("\r\n", "\n"))


def _kopf(vorspann: str) -> str:
    # Ein mehrzeiliger Text ginge getippt beim ersten Zeilenumbruch ab; eingefügt bleibt er ein Teil dieser einen Eingabe.
    return f"{ESC}[200~{vorspann}{ESC}[201~ " if _mehrzeilig(vorspann) else vorspann


def _claude_vorn(tty: str) -> bool:
    """Läuft im Vordergrund dieses Terminals noch Claude Code? Sonst landete die Eingabe samt Enter in der Shell und würde dort ausgeführt."""
    aus = subprocess.run(["ps", "-t", tty.removeprefix("/dev/"), "-o", "stat=,args="], capture_output=True, text=True, timeout=5).stdout
    for zeile in aus.splitlines():
        teile = zeile.split(None, 1)
        if len(teile) == 2 and "+" in teile[0] and re.match(r"^(\S*/)?claude(\s|$)", teile[1]):
            return True
    return False


def _pause_nach(bilder: int) -> float:
    # Claude Code liest die Bilder nach dem Einfügen ein; ein zu frühes Enter schickte die Eingabe ohne sie ab.
    return min(1.0 + 0.5 * bilder, 6.0)


def _warte_nach(bilder: int) -> None:
    time.sleep(_pause_nach(bilder))


def _tmux(vorspann: str, pfade: list[Path]) -> bool:
    bereich = os.environ.get("TMUX_PANE", "")
    programm = shutil.which("tmux")
    if not bereich or not programm:
        return False

    def t(*a) -> str:
        return subprocess.run([programm, *a], check=True, capture_output=True, text=True, timeout=10).stdout

    if not _claude_vorn(t("display-message", "-p", "-t", bereich, "#{pane_tty}").strip()):
        return False
    # Eigener Puffer: Der tmux-Server gehört allen Sitzungen dieses Rechners, sonst fügte eine die Daten der anderen ein.
    puffer = f"jarvis-{os.getpid()}"

    def einfuegen_teil(text: str) -> None:
        t("set-buffer", "-b", puffer, "--", text)
        t("paste-buffer", "-p", "-d", "-b", puffer, "-t", bereich)
        t("send-keys", "-t", bereich, "-l", " ")

    vorspann = _sauber(vorspann)
    if _mehrzeilig(vorspann):
        einfuegen_teil(vorspann)
    else:
        t("send-keys", "-t", bereich, "-l", vorspann)
    bilder = [p for p in pfade if ist_bild(p)]
    for p in bilder:
        einfuegen_teil(str(p))
    _warte_nach(len(bilder))
    t("send-keys", "-t", bereich, "Enter")
    return True


def _mac_tty() -> str:
    """Das Terminal der Sitzung: der nächste Vorfahr mit tty (der Hook selbst hat wegen setsid keins)."""
    pid = os.getpid()
    for _ in range(12):
        ausgabe = subprocess.run(["ps", "-o", "tty=,ppid=", "-p", str(pid)], capture_output=True, text=True, timeout=5).stdout.split()
        if len(ausgabe) < 2:
            return ""
        if ausgabe[0] not in ("??", "?"):
            return "/dev/" + ausgabe[0]
        pid = int(ausgabe[1])
        if pid <= 1:
            return ""
    return ""


MAC_SKRIPT = '''on run argv
tell application "Terminal"
 repeat with f in windows
  repeat with t in tabs of f
   if tty of t is (item 1 of argv) then
    do script (item 2 of argv) in t
    return "ok"
   end if
  end repeat
 end repeat
end tell
return "kein tab"
end run'''


def _mac(vorspann: str, pfade: list[Path]) -> bool:
    tty = _mac_tty()
    if not tty or not _claude_vorn(tty):
        return False

    def tippe(text: str) -> bool:
        r = subprocess.run(["osascript", "-e", MAC_SKRIPT, tty, text], capture_output=True, text=True, timeout=20)
        return r.stdout.strip() == "ok"

    # „do script“ hängt jedem Aufruf einen Zeilenumbruch an: Direkt hinter dem Einfügen schluckt Claude Code ihn, ein eigener
    # Aufruf ohne Text ist das Enter. Mehrere Aufrufe fürs Einfügen schickten Vorspann und Bilder einzeln ab (Test vom 31.10.2026).
    text, bilder = eingabe_fuer(pfade, vorspann)
    if not tippe(text):
        return False
    _warte_nach(bilder)
    return tippe("")


def _windows(vorspann: str, pfade: list[Path], claude_pid: int) -> bool:
    """Schreibt die Eingabe als Tastenereignisse in die Konsole des Claude-Prozesses (WriteConsoleInputW)."""
    text, bilder = eingabe_fuer(pfade, vorspann)
    return konsole_tippen(claude_pid, [(text, _pause_nach(bilder)), ("\r", 0)])


def konsole_tippen(claude_pid: int, schritte: list[tuple[str, float]]) -> bool:
    """Tippt nacheinander jeden Text als Tastenereignisse in die Konsole des Claude-Prozesses und wartet danach die genannten
    Sekunden. ESC (\\x1b) und Enter (\\r) gehen mit ihrem Tastencode raus. False, wenn die Konsole nicht erreichbar ist. Ohne
    Schritte nur die Probe, ob das Einspeisen hier ginge."""
    import ctypes
    from ctypes import wintypes

    class KEY(ctypes.Structure):
        _fields_ = [("bKeyDown", wintypes.BOOL), ("wRepeatCount", wintypes.WORD), ("wVirtualKeyCode", wintypes.WORD), ("wVirtualScanCode", wintypes.WORD),
                    ("UnicodeChar", wintypes.WCHAR), ("dwControlKeyState", wintypes.DWORD)]

    class EREIGNIS(ctypes.Union):
        _fields_ = [("KeyEvent", KEY), ("_platz", ctypes.c_byte * 16)]

    class RECORD(ctypes.Structure):
        _fields_ = [("EventType", wintypes.WORD), ("Event", EREIGNIS)]

    k32 = ctypes.WinDLL("kernel32", use_last_error=True)
    k32.CreateFileW.restype = wintypes.HANDLE
    k32.FreeConsole()
    if not claude_pid or not k32.AttachConsole(claude_pid):
        return False
    griff = k32.CreateFileW("CONIN$", 0xC0000000, 3, None, 3, 0, None)
    if griff in (None, wintypes.HANDLE(-1).value):
        return False

    def tippe(text: str) -> None:
        # In UTF-16-Einheiten (Emoji sind zwei) und in Stücken: Der Aufruf nimmt nicht beliebig viele Ereignisse auf einmal.
        roh = text.encode("utf-16-le", "surrogatepass")
        einheiten = [chr(int.from_bytes(roh[i:i + 2], "little")) for i in range(0, len(roh), 2)]
        for start in range(0, len(einheiten), 256):
            stueck = einheiten[start:start + 256]
            reihe = (RECORD * (len(stueck) * 2))()
            for i, z in enumerate(stueck):
                for j, unten in enumerate((True, False)):
                    r = reihe[i * 2 + j]
                    r.EventType = 1
                    r.Event.KeyEvent.bKeyDown = unten
                    r.Event.KeyEvent.wRepeatCount = 1
                    r.Event.KeyEvent.wVirtualKeyCode = {"\r": 0x0D, "\x1b": 0x1B}.get(z, 0)
                    r.Event.KeyEvent.UnicodeChar = z
            offen, zeiger = len(reihe), 0
            while offen > 0:
                geschrieben = wintypes.DWORD()
                if not k32.WriteConsoleInputW(griff, ctypes.byref(reihe, zeiger * ctypes.sizeof(RECORD)), offen, ctypes.byref(geschrieben)):
                    raise OSError(ctypes.get_last_error())
                zeiger += geschrieben.value
                offen -= geschrieben.value

    try:
        for text, pause in schritte:
            if text:
                tippe(text)
            if pause:
                time.sleep(pause)
        return True
    finally:
        k32.CloseHandle(griff)
        k32.FreeConsole()


def einfuegen(pfade: list[Path], vorspann: str, claude_pid: int = 0) -> str:
    """Fügt die Bilder mit Enter in die Eingabe der laufenden Sitzung ein. Liefert den Weg; ohne Bilder oder wenn es nicht ging leer
    (der Pfad im Text bleibt), mit Fehler „fehler: …“."""
    if not any(ist_bild(p) for p in pfade):
        return ""
    try:
        if os.environ.get("TMUX") and _tmux(vorspann, pfade):
            return "tmux"
        if sys.platform == "darwin" and _mac(vorspann, pfade):
            return "terminal"
        if sys.platform == "win32" and _windows(vorspann, pfade, claude_pid):
            return "konsole"
    except Exception as fehler:
        return f"fehler: {type(fehler).__name__}: {fehler}"
    return ""


def angekommen(transkript: str, seit: int, marke: str, sekunden: float = 8) -> bool:
    """Steht [marke] nach Byte [seit] im Transkript? Die wartende Sitzung übernimmt eine eingefügte Eingabe sofort."""
    if not transkript:
        return False
    ende = time.monotonic() + sekunden
    while time.monotonic() < ende:
        try:
            with open(transkript, "rb") as f:
                f.seek(seit)
                if marke.encode("utf-8") in f.read():
                    return True
        except OSError:
            pass
        time.sleep(0.4)
    return False


def merke_einfuegen(nummer: str, pfade: list[Path]) -> None:
    """Für neue Aufträge: Die Brücke legt ab, was der Warter der neu gestarteten Sitzung (SessionStart) einfügt."""
    if pfade and re.fullmatch(r"[A-Za-z0-9]{1,20}", nummer or ""):
        ordner = ORDNER / nummer
        ordner.mkdir(parents=True, exist_ok=True)
        (ordner / "einfuegen.json").write_text(json.dumps([str(p) for p in pfade], ensure_ascii=False), encoding="utf-8")


def nimm_einfuegen(nummer: str) -> list[Path]:
    """Genau einmal je Auftrag: Stop und SessionStart können mehrere Warter starten, das Umbenennen gewinnt nur einer."""
    datei = ORDNER / nummer / "einfuegen.json"
    genommen = datei.with_name(f"einfuegen-{os.getpid()}.json")
    try:
        os.replace(datei, genommen)
        pfade = [Path(p) for p in json.loads(genommen.read_text(encoding="utf-8"))]
        genommen.unlink(missing_ok=True)
        return pfade
    except (OSError, ValueError):
        return []


def raeume_auf(tage: int = 14) -> None:
    grenze = time.time() - tage * 86400
    if ORDNER.is_dir():
        for ordner in ORDNER.iterdir():
            try:
                if ordner.is_dir() and ordner.stat().st_mtime < grenze:
                    shutil.rmtree(ordner, ignore_errors=True)
            except OSError:
                pass


def fuer_auftrag(auftrag: dict, host: str, token: str, einfuegen_moeglich: bool) -> str:
    """Für die Brücken: holt die Anhänge eines neuen Auftrags und liefert den Text, der hinter den Auftragstext gehört."""
    anhaenge = auftrag.get("anhaenge") or []
    if not anhaenge:
        return ""
    raeume_auf()
    nummer = str(auftrag.get("nummer", ""))
    pfade, fehlend = hole(host, token, nummer, anhaenge)
    if einfuegen_moeglich:
        merke_einfuegen(nummer, pfade)
    return "\n\n" + textblock(pfade, fehlend, einfuegen_moeglich, erwaehnen=einfuegen_moeglich)


def _nur_ipv4() -> None:
    # Einige Netze leiten IPv6 nicht weiter; urllib hinge dann bis zum Zeitlimit (wie in den Brücken).
    alt = socket.getaddrinfo
    socket.getaddrinfo = lambda host, port, family=0, *a, **k: alt(host, port, socket.AF_INET, *a, **k)


def _zugang() -> tuple[str, str]:
    werte = {}
    for zeile in (Path.home() / "SK" / "Jarvis" / "relay.properties").read_text(encoding="utf-8-sig").splitlines():
        m = re.match(r"^\s*([^#=]+?)\s*=\s*(.*)$", zeile)
        if m:
            werte[m.group(1)] = m.group(2).strip()
    return werte["host"], werte["token"]


if __name__ == "__main__":
    # Windows-Brücke: python jarvis_anhaenge.py hole <auftrag.json> <text-ausgabe> <0|1 einfügen>
    if len(sys.argv) >= 5 and sys.argv[1] == "hole":
        _nur_ipv4()
        host, token = _zugang()
        daten = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8-sig"))
        Path(sys.argv[3]).write_text(fuer_auftrag(daten, host, token, sys.argv[4] == "1"), encoding="utf-8")
        sys.exit(0)
    print("Aufruf: python jarvis_anhaenge.py hole <auftrag.json> <text-ausgabe> <0|1>")
    sys.exit(1)
