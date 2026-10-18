"""Claude-Code-Hook für Programmieraufträge von Jarvis (nur in Sitzungen, die OpenLauncher --jarvis-auftrag gestartet hat).

Der OpenLauncher hängt diesen Hook nur an Jarvis-Sitzungen (über --settings) und setzt JARVIS_AUFTRAG_ID. Ohne die Variable
endet der Hook sofort ohne Ausgabe: In normalen Sitzungen passiert nie etwas.

Aufruf: python jarvis_hook.py <post|frage|stop> (macOS: python3), die Hook-Eingabe kommt als JSON auf stdin.
Eine Datei für Windows und macOS; der Mac-Start (OpenLauncherMac --jarvis-auftrag) setzt dieselben Hooks.
  post   PostToolUse (alle Werkzeuge) und PostToolUseFailure (Bash|PowerShell, nur dort stehen fehlgeschlagene Befehle).
         Läuft async. Neue Zwischentexte der KI (aus dem Transkript) gehen sofort als eigene Meldung raus, dazu Advisor,
         Skills und Agenten; geänderte Dateien, Bau, Commit/Push und adb install werden gedrosselt:
         höchstens eine Meldung je DROSSEL_S, Fehler sofort. Lesen und Suchen meldet niemand (anderer Matcher).
  frage  PreToolUse (AskUserQuestion): schickt Fragen und Optionen an Jarvis, wartet bis FRAGE_WARTEN_S auf Franks Antwort
         und gibt sie an Claude zurück. Ohne Antwort (oder wenn Frank in Jarvis „In der Claude-App beantworten“ tippt) gibt
         der Hook frei, und der normale Dialog erscheint (auch per Remote Control). Solange der Hook wartet, zeigt die
         Claude-App die Frage zwar an, nimmt aber keine Antwort an: Claude Code öffnet seinen Dialog erst nach dem Hook.
         Jeder Ausgang geht als stand „frage_status“ an Jarvis (beantwortet, am_pc, erledigt), damit die Frage dort
         überall als erledigt gilt, auch wenn sie am Rechner beantwortet wurde (post erkennt das am AskUserQuestion-Ergebnis).
  puls   PreToolUse (alle Werkzeuge, async): Lebenszeichen für die drehende Anzeige im Jarvis-Chat, höchstens alle PULS_S.
  stop   Stop: meldet den Zwischenstand (letzte Nachricht). Endet sie mit einer Frage, wartet der Hook auf Franks Antwort und
         lässt Claude damit weiterarbeiten (decision block). Liegen Nachträge zu diesem Auftrag bereit, gehen sie danach
         ebenso als decision block in dieselbe Sitzung.
  warte  SessionStart und Stop mit asyncRewake: wartet im Hintergrund am Relay auf Nachträge zu diesem Auftrag (höchstens
         einer je Auftrag, bis WARTER_MAX_S oder bis die Claude-Sitzung endet) und weckt die Sitzung mit Exit 2 und dem
         Nachtrag, auch wenn sie gerade nichts tut. Jeder Nachtrag geht genau einmal hinein (nachtrag_id im Zustand) und
         wird mit stand „nachtrag“ quittiert; erst dann nimmt der Relay ihn aus der Warteschlange.

Wege: Relay https://<host>/pc/meldung und /pc/antwort mit dem Geräte-Token aus ~/SK/Jarvis/relay.properties, nur IPv4
(auf diesem Netz hängt IPv6 zeitweise, siehe bugs/desktop/dotnet-httpclient-ipv6-haenger.md).
Protokoll: %LOCALAPPDATA%/JarvisPcBruecke/hook.log, macOS ~/Library/Logs/JarvisPcBruecke/hook.log
"""
import json
import os
import re
import socket
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import hashlib
import uuid
from pathlib import Path

AUFTRAG = os.environ.get("JARVIS_AUFTRAG_ID", "").strip()
DROSSEL_S = 20
FRAGE_WARTEN_S = 300      # Franks Wahl vom 18.10.2026: höchstens fünf Minuten nur in Jarvis
PULS_S = 10               # Lebenszeichen höchstens so oft
MAX_TEXT = 4000           # so lang darf eine Meldung werden (die Schlusszusammenfassung kommt vollständig)
if os.environ.get("LOCALAPPDATA"):
    ORDNER = Path(os.environ["LOCALAPPDATA"]) / "JarvisPcBruecke"
elif sys.platform == "darwin":
    ORDNER = Path.home() / "Library" / "Logs" / "JarvisPcBruecke"
else:
    ORDNER = Path.home() / "JarvisPcBruecke"
PROTOKOLL = ORDNER / "hook.log"
ZUSTAND = ORDNER / f"hook-{AUFTRAG or 'ohne'}.json"


def schreib(text: str) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        # Wie bruecke.log: ab 1 MB eine Generation zurück, sonst wächst das Protokoll mit jeder Meldung ohne Grenze.
        if PROTOKOLL.exists() and PROTOKOLL.stat().st_size > 1024 * 1024:
            os.replace(PROTOKOLL, PROTOKOLL.with_name("hook.log.alt"))
        with PROTOKOLL.open("a", encoding="utf-8") as f:
            f.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} [{AUFTRAG}] {text}\n")
    except Exception:
        pass


# Nur IPv4: getaddrinfo liefert sonst zuerst IPv6, und die Verbindung hängt bis zum Zeitlimit.
_original = socket.getaddrinfo


def _nur_ipv4(host, port, family=0, *args, **kwargs):
    return _original(host, port, socket.AF_INET, *args, **kwargs)


socket.getaddrinfo = _nur_ipv4


def zugang() -> tuple[str, str]:
    werte = {}
    for zeile in (Path.home() / "SK" / "Jarvis" / "relay.properties").read_text(encoding="utf-8").splitlines():
        m = re.match(r"^\s*([^#=]+?)\s*=\s*(.*)$", zeile)
        if m:
            werte[m.group(1)] = m.group(2).strip()
    return werte["host"], werte["token"]


def rufe(pfad: str, daten: dict | None = None, zeitlimit: float = 5) -> tuple[int, dict]:
    host, token = zugang()
    anfrage = urllib.request.Request(f"https://{host}{pfad}", method="POST" if daten is not None else "GET",
                                     headers={"X-Jarvis-Token": token, "Content-Type": "application/json; charset=utf-8"},
                                     data=json.dumps(daten, ensure_ascii=False).encode("utf-8") if daten is not None else None)
    try:
        with urllib.request.urlopen(anfrage, timeout=zeitlimit) as antwort:
            roh = antwort.read().decode("utf-8")
            return antwort.status, (json.loads(roh) if roh.strip() else {})
    except urllib.error.HTTPError as fehler:
        return fehler.code, {}


def melde(stand: str, schritt: str, frage: dict | None = None, nachtrag_ids: list[str] | None = None) -> int:
    daten = {"auftrag_id": AUFTRAG, "stand": stand, "schritt": schritt[:MAX_TEXT]}
    if frage:
        daten["frage"] = frage
    if nachtrag_ids:
        daten["nachtrag_ids"] = nachtrag_ids
    code, _ = rufe("/pc/meldung", daten)
    schreib(f"Meldung {stand}: {schritt[:120]} -> {code}")
    return code


def melde_sicher(stand: str, schritt: str, frage: dict | None = None, versuche: int = 5) -> bool:
    """Für Rückfragen und ihren Ausgang: mehrmals versuchen (Relay-Neustart, Funkloch). Doppelt schadet nicht, Jarvis und
    der Relay erkennen dieselbe frage_id wieder."""
    for versuch in range(versuche):
        try:
            if melde(stand, schritt, frage) == 200:
                return True
        except Exception as fehler:
            schreib(f"Meldung {stand} Versuch {versuch + 1}: {fehler}")
        time.sleep(min(8, 1 + 2 * versuch))
    schreib(f"Meldung {stand} nach {versuche} Versuchen aufgegeben")
    return False


def frage_status(frage_id: str, status: str, antwort: str = "", grund: str = "") -> None:
    """Ausgang einer Rückfrage an Relay und Jarvis: beantwortet (in Jarvis), am_pc (Dialog am Rechner), erledigt."""
    melde_sicher("frage_status", "", {"frage_id": frage_id, "status": status, "antwort": antwort[:2000], "grund": grund}, versuche=3)


def warte_auf_antwort(frage_id: str) -> dict | None:
    frist = time.monotonic() + FRAGE_WARTEN_S
    while time.monotonic() < frist:
        try:
            code, daten = rufe("/pc/antwort?frage_id=" + urllib.parse.quote(frage_id), zeitlimit=35)
        except Exception as fehler:
            schreib(f"Antwort abholen: {fehler}")
            time.sleep(3)
            continue
        if code == 200:
            if daten.get("freigeben"):
                return {"freigeben": True}
            return daten
        if code in (404, 410):
            return None
        # 204 = noch keine Antwort (der Relay hat schon gewartet); alles andere (etwa 502 beim Neustart des Relays) kommt
        # sofort zurück und darf die Schleife nicht ohne Pause gegen den Server laufen lassen.
        if code != 204:
            time.sleep(3)
    return None


# ---------------------------------------------------------------- Zustand (Drosselung, gesehene Texte)

# Neuer Name (.lock statt .sperre): Die frühere Sperrdatei-Logik noch laufender Warter kommt dieser nie in die Quere.
SPERRE = ORDNER / f"hook-{AUFTRAG or 'ohne'}.lock"
SPERRE_WARTEN_S = 10


class SperreBelegt(Exception):
    pass


class Sperre:
    """
    Async-Hooks laufen gleichzeitig: Zustand und Transkript-Stelle nur einer nach dem anderen. Eine echte Dateisperre des
    Betriebssystems: Endet ein Hook, auch hart an seinem Zeitlimit, gibt das System sie frei, und nie löscht ein Hook die
    Sperre eines anderen. Darin nur Zustand und Transkript lesen und schreiben, nie Netz; gesendet wird danach. Ist sie
    nach SPERRE_WARTEN_S noch belegt, kommt SperreBelegt: lieber diesen Hook auslassen (der nächste holt die Texte nach)
    als ohne Sperre doppelt melden oder einen Nachtrag zweimal übergeben.
    """

    def __enter__(self):
        ORDNER.mkdir(parents=True, exist_ok=True)
        self.fd = os.open(SPERRE, os.O_RDWR | os.O_CREAT)
        frist = time.monotonic() + SPERRE_WARTEN_S
        while True:
            try:
                if os.name == "nt":
                    import msvcrt
                    os.lseek(self.fd, 0, os.SEEK_SET)
                    msvcrt.locking(self.fd, msvcrt.LK_NBLCK, 1)
                else:
                    import fcntl
                    fcntl.flock(self.fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                # Frisch halten: Das Aufräumen der Brücken löscht nur Sperrdateien, die 14 Tage niemand benutzt hat.
                try:
                    os.utime(SPERRE)
                except OSError:
                    pass
                return self
            except OSError:
                if time.monotonic() >= frist:
                    os.close(self.fd)
                    raise SperreBelegt(f"Sperre nach {SPERRE_WARTEN_S} s noch belegt")
                time.sleep(0.05)

    def __exit__(self, *_):
        try:
            if os.name == "nt":
                import msvcrt
                os.lseek(self.fd, 0, os.SEEK_SET)
                msvcrt.locking(self.fd, msvcrt.LK_UNLCK, 1)
            else:
                import fcntl
                fcntl.flock(self.fd, fcntl.LOCK_UN)
        except OSError:
            pass
        finally:
            os.close(self.fd)


def lies_zustand() -> dict:
    try:
        z = json.loads(ZUSTAND.read_text(encoding="utf-8"))
    except Exception:
        z = {}
    for k, v in (("zuletzt", 0), ("dateien", []), ("offen", []), ("gesehen", []), ("stelle", 0), ("beginn", time.time()), ("nachtraege", [])):
        z.setdefault(k, v)
    return z


def dauer_text(sekunden: float) -> str:
    s = int(max(0, sekunden))
    if s < 60:
        return f"{s} s"
    if s < 3600:
        return f"{s // 60} min {s % 60} s"
    return f"{s // 3600} h {s % 3600 // 60} min"


def schreib_zustand(z: dict) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        # Atomar: Ein gleichzeitig lesender Hook darf nie eine halb geschriebene Datei sehen (sonst gälte alles als ungesehen
        # und die ganze Sitzung ginge noch einmal an Jarvis).
        teil = ZUSTAND.with_name(ZUSTAND.name + f".{os.getpid()}.tmp")
        teil.write_text(json.dumps(z, ensure_ascii=False), encoding="utf-8")
        os.replace(teil, ZUSTAND)
    except Exception:
        pass


def sammel(z: dict, neu: str | None, sofort: bool = False, stand: str = "laeuft") -> list[tuple[str, str]]:
    """
    Hängt [neu] an die offenen Punkte; fällig wird eine Meldung, wenn DROSSEL_S um sind oder [sofort]. Liefert die zu
    sendenden Meldungen und vermerkt sie im Zustand schon als gesendet: Gesendet wird erst nach der Sperre (sende).
    """
    if neu:
        z["offen"].append(neu)
    jetzt = time.time()
    if not (sofort or jetzt - z.get("zuletzt", 0) >= DROSSEL_S):
        return []
    teile = []
    if z["dateien"]:
        namen = list(dict.fromkeys(z["dateien"]))
        teile.append("✏️ Geändert: " + ", ".join(namen[:6]) + (f" und {len(namen) - 6} weitere" if len(namen) > 6 else ""))
    teile += z["offen"]
    if not teile:
        return []
    z.update({"zuletzt": jetzt, "dateien": [], "offen": []})
    return [(stand, " · ".join(teile))]


def sende(meldungen: list[tuple[str, str]]) -> None:
    """Sendet außerhalb der Sperre. Höchstens einmal: Ist der Relay weg, fällt der Rest dieses Hooks aus, statt dass
    später alles doppelt kommt."""
    for stand, text in meldungen:
        try:
            melde(stand, text)
        except Exception as fehler:
            schreib(f"Meldung {stand} nicht gesendet, Rest ausgelassen: {fehler}")
            return


def lesbar(text: str) -> str:
    """Code-Zeichen weg, Zeilen bleiben. **Fett** bleibt: Jarvis zeigt es im Chat fett an."""
    return text.replace("`", "").strip()


def schon_gesehen(z: dict, text: str) -> bool:
    """Merkt sich die letzten 200 gemeldeten Texte, damit Zwischentext und Schlussmeldung nicht doppelt kommen."""
    schluessel = hashlib.sha1(text.strip().encode("utf-8")).hexdigest()[:16]
    if schluessel in z["gesehen"]:
        return True
    z["gesehen"] = (z["gesehen"] + [schluessel])[-200:]
    return False


def neue_texte(eingabe: dict, z: dict) -> list[str]:
    """Zwischentexte der KI, die seit dem letzten Lesen im Transkript dazugekommen sind (nur vollständige Zeilen)."""
    pfad = eingabe.get("transcript_path")
    if not pfad:
        return []
    texte = []
    try:
        with open(pfad, "rb") as f:
            groesse = os.fstat(f.fileno()).st_size
            stelle = z["stelle"] if 0 <= z["stelle"] <= groesse else 0
            f.seek(stelle)
            roh = f.read()
    except Exception:
        return []
    ende = roh.rfind(b"\n")
    if ende < 0:
        return []
    z["stelle"] = stelle + ende + 1
    for zeile in roh[:ende].decode("utf-8", "replace").splitlines():
        try:
            eintrag = json.loads(zeile)
        except Exception:
            continue
        nachricht = eintrag.get("message") or {}
        if eintrag.get("type") != "assistant" or nachricht.get("role") != "assistant":
            continue
        inhalt = nachricht.get("content")
        bloecke = [inhalt] if isinstance(inhalt, str) else [b.get("text", "") for b in inhalt or [] if isinstance(b, dict) and b.get("type") == "text"]
        for text in bloecke:
            text = lesbar(text)
            if text and not schon_gesehen(z, text):
                texte.append(text)
    return texte


# ---------------------------------------------------------------- Freie Rückfragen am Zugende

# Ein Aufzählungspunkt mit eindeutigem Trenner: „A)“, „A.“, „A:“, „(A)“, „**A**“, „**A)**“ oder „A — “; nie „Antworte …“.
_OPTION = re.compile(r"^\s*(?:[-*•]\s*)?(?:(?:\*\*|__)\(?(?P<a>[A-H]|[1-9])[).:]?(?:\*\*|__)|\(?(?P<b>[A-H]|[1-9])[).:]|(?P<c>[A-H]|[1-9])\s+[—–-])\s*(?:[—–:-]\s*)?(?P<text>\S.*?)\s*$")


def optionen_im_text(text: str) -> tuple[list[dict], bool]:
    """
    Die Auswahl, die im letzten Absatz einer freien Rückfrage wörtlich aufgezählt ist (A) …, **B** …, 1. …), nur wenn sie
    lückenlos bei A bzw. 1 beginnt und mindestens zwei Einträge hat; nichts wird erfunden. Liefert die Optionen und, ob
    nach der Aufzählung nur noch eine kurze Schlusszeile steht (dann ist die Nachricht eine Auswahlfrage, auch ohne „?“ am Ende).
    """
    zeilen = text.rstrip().splitlines()
    ende = len(zeilen)
    # Höchstens eine kurze Schlusszeile („Antworte mit A, B oder C.“) und Leerzeilen hinter der Aufzählung.
    while ende and not zeilen[ende - 1].strip():
        ende -= 1
    schluss = ende
    if ende and not _OPTION.match(zeilen[ende - 1]) and len(zeilen[ende - 1]) <= 200:
        schluss = ende - 1
        while schluss and not zeilen[schluss - 1].strip():
            schluss -= 1
    gefunden = []
    i = schluss
    while i and _OPTION.match(zeilen[i - 1]):
        m = _OPTION.match(zeilen[i - 1])
        gefunden.insert(0, (m.group("a") or m.group("b") or m.group("c"), m.group("text")))
        i -= 1
    if len(gefunden) < 2:
        return [], False
    start = "A" if gefunden[0][0].isalpha() else "1"
    if any(kennung != chr(ord(start) + nr) for nr, (kennung, _) in enumerate(gefunden)):
        return [], False
    # Davor muss eine Frage stehen (eine Zeile mit „?“ in den letzten Zeilen vor der Aufzählung).
    davor = "\n".join(zeilen[max(0, i - 6):i])
    if "?" not in davor and "?" not in "\n".join(zeilen[schluss:ende]):
        return [], False
    optionen = [{"label": text.replace("**", "").strip(), "kennung": kennung} for kennung, text in gefunden]
    return optionen, True


# ---------------------------------------------------------------- Nachträge

# So lange wartet ein Warter höchstens: zehn Minuten unter dem Hook-Zeitlimit (OpenLauncher und Notwege: 43200 s), damit er
# selbst endet und seine pid-Datei aufräumt, statt am Zeitlimit hart beendet zu werden.
WARTER_MAX_S = 12 * 3600 - 600
WARTER = ORDNER / f"warter-{AUFTRAG or 'ohne'}.pid"


def hole_nachtraege(warten: int) -> list[dict]:
    """Nachträge zu diesem Auftrag vom Relay; mit [warten] Long-Polling. Leer = keiner (oder Relay nicht erreichbar)."""
    code, daten = rufe(f"/pc/nachtrag?auftrag_id={urllib.parse.quote(AUFTRAG)}&warten={warten}", zeitlimit=warten + 10)
    if code != 200:
        # Beim Long-Polling nie ohne Pause neu fragen: 404 (Relay ohne Endpunkt, falsches Token) käme sofort zurück und liefe
        # sonst bis zu zwölf Stunden im Kreis gegen den Server.
        if warten and code != 204:
            time.sleep(3)
        return []
    return [n for n in daten.get("nachtraege") or [] if isinstance(n, dict) and n.get("nachtrag_id") and n.get("text")]


def beanspruche(liste: list[dict]) -> list[dict]:
    """
    Nimmt nur Nachträge, die noch kein anderer Hook dieser Sitzung übergeben hat (Stop und Warter laufen gleichzeitig).
    Schon übergebene, die der Relay noch anbietet (Quittung kam nicht an), werden still nachquittiert.
    """
    with Sperre():
        z = lies_zustand()
        neu = [n for n in liste if n["nachtrag_id"] not in z["nachtraege"]]
        alt = [n["nachtrag_id"] for n in liste if n["nachtrag_id"] in z["nachtraege"]]
        z["nachtraege"] = (z["nachtraege"] + [n["nachtrag_id"] for n in neu])[-200:]
        schreib_zustand(z)
    # Die Quittung darf die Übergabe nie verhindern: Die neuen sind schon als übergeben vermerkt und kämen sonst nie an.
    # Scheitert sie, bietet der Relay sie wieder an, und die nächste Abfrage quittiert sie still nach.
    if alt:
        try:
            melde("nachtrag", "", nachtrag_ids=alt)
        except Exception as fehler:
            schreib(f"Nachquittung: {fehler}")
    return neu


def uebergib(neu: list[dict]) -> str:
    """Quittiert die Übergabe (Relay und Jarvis) und liefert den Text, mit dem die Sitzung weiterarbeitet."""
    kurz = " · ".join(n["text"].replace("\n", " ")[:300] for n in neu)
    try:
        melde("nachtrag", "📨 Nachtrag in der Sitzung angekommen: " + kurz, nachtrag_ids=[n["nachtrag_id"] for n in neu])
    except Exception as fehler:
        schreib(f"Quittung der Übergabe: {fehler}")
    teile = "\n\n".join(f"NACHTRAG {n['nachtrag_id']}:\n{n['text']}" for n in neu)
    return (f"Frank hat über Jarvis {'einen Nachtrag' if len(neu) == 1 else str(len(neu)) + ' Nachträge'} zu diesem Programmierauftrag "
            f"(AUFTRAG-ID {AUFTRAG}) geschickt. Er gehört zu genau diesem Auftrag und dieser Sitzung: Setze ihn hier um, als Teil des "
            f"Auftrags, und schließe danach wieder nach den Profilregeln ab (bauen, Version, Commit, Push, Installation) und melde "
            f"stand=fertig mit programmier_fortschritt. Ist er unklar, frag mit AskUserQuestion.\n\n{teile}")


def _windows_prozesse() -> dict[int, tuple[int, str]]:
    """pid -> (Eltern-pid, Programmname) aller Prozesse (nur Windows)."""
    import ctypes
    from ctypes import wintypes

    class Eintrag(ctypes.Structure):
        _fields_ = [("dwSize", wintypes.DWORD), ("cntUsage", wintypes.DWORD), ("th32ProcessID", wintypes.DWORD),
                    ("th32DefaultHeapID", ctypes.c_size_t), ("th32ModuleID", wintypes.DWORD), ("cntThreads", wintypes.DWORD),
                    ("th32ParentProcessID", wintypes.DWORD), ("pcPriClassBase", ctypes.c_long), ("dwFlags", wintypes.DWORD),
                    ("szExeFile", ctypes.c_wchar * 260)]

    k = ctypes.windll.kernel32
    k.CreateToolhelp32Snapshot.restype = wintypes.HANDLE
    schnappschuss = k.CreateToolhelp32Snapshot(2, 0)
    e = Eintrag()
    e.dwSize = ctypes.sizeof(Eintrag)
    alle = {}
    weiter = k.Process32FirstW(schnappschuss, ctypes.byref(e))
    while weiter:
        alle[e.th32ProcessID] = (e.th32ParentProcessID, e.szExeFile.lower())
        weiter = k.Process32NextW(schnappschuss, ctypes.byref(e))
    k.CloseHandle(schnappschuss)
    return alle


def _posix_eltern(pid: int) -> tuple[int, str]:
    """(Eltern-pid, Programmname) eines Prozesses über ps (macOS, Linux); (0, "") wenn er fehlt."""
    import subprocess
    try:
        aus = subprocess.run(["ps", "-o", "ppid=,comm=", "-p", str(pid)], capture_output=True, text=True, timeout=5).stdout.strip()
        eltern, _, name = aus.partition(" ")
        return int(eltern), os.path.basename(name.strip()).lower()
    except Exception:
        return 0, ""


def claude_prozess() -> int | None:
    """Der Claude-Code-Prozess, der diesen Hook gestartet hat (nächster Vorfahr claude*), oder None."""
    if os.name != "nt":
        # Ohne diesen Zweig merkte der Warter das Ende der Sitzung nicht und wartete bis zu zwölf Stunden weiter.
        pid = os.getpid()
        for _ in range(12):
            eltern, _ = _posix_eltern(pid)
            if eltern <= 1:
                return None
            if _posix_eltern(eltern)[1].startswith("claude"):
                return eltern
            pid = eltern
        return None
    try:
        alle = _windows_prozesse()
        pid = os.getpid()
        for _ in range(12):
            eltern = alle.get(pid, (0, ""))[0]
            if not eltern or eltern not in alle:
                return None
            if alle[eltern][1].startswith("claude"):
                return eltern
            pid = eltern
    except Exception as fehler:
        schreib(f"Claude-Prozess nicht gefunden: {fehler}")
    return None


def startzeit(pid: int) -> str | None:
    """Startzeit eines laufenden Prozesses als Text; None, wenn er nicht (mehr) läuft, "" wenn sie sich nicht ermitteln
    lässt. Zusammen mit der PID kennzeichnet sie den Prozess eindeutig, denn beide Systeme vergeben PIDs neu."""
    if os.name != "nt":
        import subprocess
        try:
            aus = subprocess.run(["ps", "-o", "lstart=", "-p", str(pid)], capture_output=True, text=True, timeout=5).stdout.strip()
        except Exception:
            return ""
        return aus or None
    import ctypes
    from ctypes import wintypes
    k = ctypes.windll.kernel32
    k.OpenProcess.restype = wintypes.HANDLE
    griff = k.OpenProcess(0x1000, False, pid)  # PROCESS_QUERY_LIMITED_INFORMATION
    if not griff:
        return None
    try:
        code = wintypes.DWORD()
        if not k.GetExitCodeProcess(wintypes.HANDLE(griff), ctypes.byref(code)) or code.value != 259:  # STILL_ACTIVE
            return None
        zeiten = [wintypes.FILETIME() for _ in range(4)]
        if not k.GetProcessTimes(wintypes.HANDLE(griff), *[ctypes.byref(z) for z in zeiten]):
            return ""
        return str((zeiten[0].dwHighDateTime << 32) | zeiten[0].dwLowDateTime)
    finally:
        k.CloseHandle(wintypes.HANDLE(griff))


def lebt(pid: int, start: str = "") -> bool:
    """Läuft der Prozess noch, und ist es noch derselbe ([start] wie von startzeit)? Ohne diesen Abgleich gälte ein
    fremder Prozess mit wiederverwendeter PID als alter Warter, und kein neuer Warter stellte Nachträge mehr zu."""
    jetzt = startzeit(pid)
    if jetzt is None:
        return False
    return not start or not jetzt or jetzt == start


def nachtraege_im_stop() -> bool:
    """Stop: Liegen Nachträge bereit, gehen sie als decision block in die Sitzung. True = übergeben."""
    try:
        neu = beanspruche(hole_nachtraege(0))
    except Exception as fehler:
        schreib(f"Nachträge im Stop: {fehler}")
        return False
    if not neu:
        return False
    sys.stdout.write(json.dumps({"decision": "block", "reason": uebergib(neu)}, ensure_ascii=False))
    return True


def warte(eingabe: dict) -> int:
    """asyncRewake: wartet auf Nachträge; Exit 2 mit dem Nachtrag weckt die Sitzung. Nur ein Warter je Auftrag."""
    ORDNER.mkdir(parents=True, exist_ok=True)
    # Die pid-Datei trägt PID und Startzeit („1234 133…“); ältere Fassungen schrieben nur die PID.
    try:
        teile = WARTER.read_text(encoding="utf-8").split()
        alter, alter_start = (int(teile[0]), teile[1] if len(teile) > 1 else "") if teile else (0, "")
    except Exception:
        alter, alter_start = 0, ""
    if alter and alter != os.getpid() and lebt(alter, alter_start):
        return 0
    WARTER.write_text(f"{os.getpid()} {startzeit(os.getpid()) or ''}".strip(), encoding="utf-8")
    claude = claude_prozess()
    claude_start = (startzeit(claude) or "") if claude else ""
    schreib(f"Warter gestartet ({eingabe.get('hook_event_name', '')}, Claude-Prozess {claude})")
    try:
        ende = time.monotonic() + WARTER_MAX_S
        while time.monotonic() < ende:
            if claude and not lebt(claude, claude_start):
                schreib("Warter: Claude-Sitzung beendet")
                return 0
            try:
                liste = hole_nachtraege(25)
            except Exception as fehler:
                schreib(f"Warter: {fehler}")
                time.sleep(5)
                continue
            if not liste:
                continue
            try:
                neu = beanspruche(liste)
            except SperreBelegt as fehler:
                # Nicht übernommen: Der Relay bietet sie gleich wieder an.
                schreib(f"Warter: {fehler}")
                time.sleep(3)
                continue
            if not neu:
                # Schon übergeben (Stop war schneller), Quittung noch unterwegs: nicht im Kreis fragen.
                time.sleep(3)
                continue
            sys.stderr.write(uebergib(neu))
            schreib(f"Warter: {len(neu)} Nachtrag/Nachträge übergeben, Sitzung wird geweckt")
            return 2
        return 0
    finally:
        try:
            if WARTER.read_text(encoding="utf-8").split()[:1] == [str(os.getpid())]:
                WARTER.unlink()
        except Exception:
            pass


# ---------------------------------------------------------------- Ereignisse

def post(eingabe: dict) -> None:
    werkzeug = eingabe.get("tool_name", "")
    ein = eingabe.get("tool_input") or {}
    aus = eingabe.get("tool_response")
    erledigt = []
    with Sperre():
        z = lies_zustand()
        # Zuerst, was die KI seit dem letzten Werkzeug geschrieben hat: sofort, jeder Text als eigene Meldung.
        raus = [("laeuft", "💬 " + text) for text in neue_texte(eingabe, z)]
        # Eine Rückfrage, die am Rechner (Terminal, Claude-App) beantwortet wurde: AskUserQuestion ist gelaufen. Läuft danach
        # ein anderes Werkzeug, ist jede noch am Rechner offene Frage ebenfalls erledigt (abgebrochen oder anders beantwortet).
        if werkzeug == "AskUserQuestion":
            erledigt = [(v["frage_id"], "beantwortet_am_pc", antwort_text(aus)) for _, v in offene_am_pc(z, str(eingabe.get("tool_use_id") or ""))]
        else:
            erledigt = [(v["frage_id"], "erledigt", "") for _, v in offene_am_pc(z)]
        if werkzeug in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
            pfad = ein.get("file_path") or ein.get("notebook_path") or ""
            if pfad:
                z["dateien"].append(Path(pfad).name)
            raus += sammel(z, None)
        elif werkzeug in ("Bash", "PowerShell"):
            befehl = str(ein.get("command", ""))
            text = json.dumps(aus, ensure_ascii=False) if not isinstance(aus, str) else aus
            # Ein Exit-Code ungleich 0 kommt nur als PostToolUseFailure an; dazu Fehler, die mit Exit 0 enden.
            fehlgeschlagen = eingabe.get("hook_event_name") == "PostToolUseFailure" or \
                bool(re.search(r"BUILD FAILED|INSTALL_FAILED|! \[rejected\]|APK_UPDATE_STATUS=fehler", text)) or \
                (isinstance(aus, dict) and bool(aus.get("interrupted")))
            if re.search(r"gradlew(\.bat)?\b.*\b(assemble|install|build|bundle)", befehl):
                raus += sammel(z, "🔴 Bau fehlgeschlagen" if fehlgeschlagen else "🔨 Bau erfolgreich", sofort=True)
            elif re.search(r"\bgit\b.*\bpush\b", befehl):
                raus += sammel(z, "🔴 Push fehlgeschlagen" if fehlgeschlagen else "⬆️ Gepusht", sofort=True)
            elif re.search(r"\bgit\b.*\bcommit\b", befehl):
                raus += sammel(z, "🔴 Commit fehlgeschlagen" if fehlgeschlagen else "📦 Committet", sofort=True)
            elif re.search(r"\badb\b.*\binstall\b", befehl):
                raus += sammel(z, "🔴 Installation fehlgeschlagen" if fehlgeschlagen else "📲 Auf dem Handy installiert", sofort=True)
            elif re.search(r"apk-update", befehl):
                raus += sammel(z, "🔴 APK-Update fehlgeschlagen" if fehlgeschlagen else "☁️ APK-Update für UpdateStation bereit", sofort=True)
            else:
                raus += sammel(z, None)
        else:
            raus += sammel(z, None)
        schreib_zustand(z)
    sende(raus)
    for frage_id, status, text in erledigt:
        frage_status(frage_id, status, antwort=text)


def antwort_text(antwort) -> str:
    """Die Antworten aus dem Ergebnis von AskUserQuestion als lesbarer Text (Format laut Doku: Frage -> label, mehrere mit Komma)."""
    if isinstance(antwort, dict):
        werte = antwort.get("answers") if isinstance(antwort.get("answers"), dict) else None
        if werte:
            return "; ".join(f"{v}" for v in werte.values() if str(v).strip())
        return "; ".join(str(v) for k, v in antwort.items() if isinstance(v, str) and v.strip() and k not in ("questions",))[:2000]
    if isinstance(antwort, list):
        return " ".join(antwort_text(t) if not isinstance(t, dict) or t.get("type") != "text" else str(t.get("text", "")) for t in antwort)[:2000]
    return str(antwort or "")[:2000]


def merke_frage(tool_use_id: str, frage_id: str, status: str) -> None:
    """Welche frage_id zu welchem AskUserQuestion-Aufruf gehört und wo sie steht (jarvis, am_pc): post meldet dann den Ausgang."""
    try:
        with Sperre():
            z = lies_zustand()
            fragen = z.setdefault("fragen", {})
            fragen[tool_use_id or frage_id] = {"frage_id": frage_id, "status": status}
            z["fragen"] = dict(list(fragen.items())[-20:])
            schreib_zustand(z)
    except SperreBelegt as fehler:
        schreib(f"merke_frage: {fehler}")


def offene_am_pc(z: dict, tool_use_id: str | None = None) -> list[tuple[str, dict]]:
    """Fragen, die am Rechner offen sind; mit [tool_use_id] nur diese (auch eine, die noch in Jarvis wartete). Nimmt sie aus
    dem Zustand (einmal melden). Ohne [tool_use_id] nie eine, auf die der Frage-Hook gerade noch wartet."""
    fragen = z.get("fragen") or {}
    erlaubt = ("am_pc", "jarvis") if tool_use_id else ("am_pc",)
    treffer = [(k, v) for k, v in fragen.items() if v.get("status") in erlaubt and (tool_use_id is None or k == tool_use_id)]
    for k, _ in treffer:
        fragen.pop(k, None)
    return treffer


def frage(eingabe: dict) -> None:
    ein = eingabe.get("tool_input") or {}
    fragen = ein.get("questions") or []
    if not fragen:
        return
    raus = []
    try:
        with Sperre():
            z = lies_zustand()
            raus = [("laeuft", "💬 " + text) for text in neue_texte(eingabe, z)]
            raus += sammel(z, None, sofort=True)
            schreib_zustand(z)
    except SperreBelegt as fehler:
        # Die Frage selbst ist wichtiger als die Zwischentexte: Die holt der nächste Hook nach.
        schreib(f"frage: {fehler}")
    sende(raus)
    tool_use_id = str(eingabe.get("tool_use_id") or "")
    frage_id = uuid.uuid4().hex[:12]
    kurz = " / ".join(str(f.get("question", "")) for f in fragen)
    # Mit Frist (Sekunden seit 1970): Jarvis zeigt, wie lange die Frage dort noch beantwortbar ist.
    daten = {"frage_id": frage_id, "fragen": fragen, "bis": int(time.time() + FRAGE_WARTEN_S), "tool_use_id": tool_use_id}
    if not melde_sicher("frage", "❓ " + kurz, daten):
        schreib("Rückfrage nicht an Jarvis zugestellt: normaler Dialog")
        return
    merke_frage(tool_use_id, frage_id, "jarvis")
    antwort = warte_auf_antwort(frage_id)
    if not antwort or antwort.get("freigeben"):
        grund = "freigegeben" if antwort else "abgelaufen"
        schreib(f"Keine Antwort über Jarvis ({grund}): normaler Dialog")
        merke_frage(tool_use_id, frage_id, "am_pc")
        frage_status(frage_id, "am_pc", grund=grund)
        return
    antworten = {str(k): str(v) for k, v in (antwort.get("antworten") or {}).items()}
    zusatz = str(antwort.get("text", "")).strip()
    if zusatz and not antworten:
        # Nur Freitext (Sprache): jede Frage bekommt ihn als „Sonstiges“, Claude ordnet ihn selbst ein.
        antworten = {str(f.get("question", "")): zusatz for f in fragen}
    text = "; ".join(f"{v}" for v in antworten.values())
    # Angekommen: Jarvis zeigt die Frage überall als beantwortet (auch auf einem zweiten Weg, etwa per ChatGPT).
    merke_frage(tool_use_id, frage_id, "beantwortet")
    frage_status(frage_id, "beantwortet", antwort=text)
    melde("laeuft", "💬 Antwort von Frank: " + text)
    ausgabe = {"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "allow",
        "permissionDecisionReason": "Frank hat über Jarvis geantwortet.",
        "updatedInput": {**ein, "answers": antworten},
    }}
    sys.stdout.write(json.dumps(ausgabe))


def stop(eingabe: dict) -> None:
    letzte = lesbar(str(eingabe.get("last_assistant_message") or ""))
    raus = []
    erledigt = []
    try:
        with Sperre():
            z = lies_zustand()
            erledigt = [v["frage_id"] for _, v in offene_am_pc(z)]
            # Zwischentexte dieses Zuges, die noch kein Werkzeug-Hook gemeldet hat. Die letzte Nachricht steht oft schon im
            # Transkript und wird dabei als gesehen markiert: Sie kommt trotzdem gleich gesondert (Fehler vom 15.10.2026).
            texte = neue_texte(eingabe, z)
            raus = [("laeuft", "💬 " + text) for text in texte if text != letzte]
            raus += sammel(z, None, sofort=True)
            neu = bool(letzte) and (letzte in texte or not schon_gesehen(z, letzte))
            dauer = dauer_text(time.time() - z["beginn"])
            schreib_zustand(z)
    except SperreBelegt as fehler:
        # Der Abschluss darf nie ausfallen: nur lesen, nichts vermerken (die Zwischentexte holt der nächste Hook nach).
        schreib(f"stop: {fehler}")
        z = lies_zustand()
        neu = bool(letzte) and not schon_gesehen(dict(z, gesehen=list(z["gesehen"])), letzte)
        dauer = dauer_text(time.time() - z["beginn"])
    sende(raus)
    for frage_id in erledigt:
        frage_status(frage_id, "erledigt")
    if not neu:
        nachtraege_im_stop()
        return
    # Eine echte Rückfrage steht am Ende der Nachricht (Fragezeichen oder eine wörtlich aufgezählte Auswahl A/B/C … bzw.
    # 1/2/3 nach einer Frage, etwa Frage 1 des Research-Skills ohne AskUserQuestion); nur dann wartet der Hook (nie zweimal).
    ende = letzte.rstrip().rstrip("*_ )\"“”").strip()
    optionen, auswahl = optionen_im_text(letzte)
    if (ende.endswith("?") or auswahl) and not eingabe.get("stop_hook_active"):
        frage_id = uuid.uuid4().hex[:12]
        daten = {"frage_id": frage_id, "frei": True, "bis": int(time.time() + FRAGE_WARTEN_S),
                 "fragen": [{"question": letzte[-3000:], "options": optionen}]}
        if not melde_sicher("frage", "❓ " + letzte[-MAX_TEXT:], daten):
            schreib("Freie Rückfrage nicht an Jarvis zugestellt")
            nachtraege_im_stop()
            return
        antwort = warte_auf_antwort(frage_id)
        if antwort and not antwort.get("freigeben"):
            inhalt = str(antwort.get("text", "")).strip() or "; ".join(str(v) for v in (antwort.get("antworten") or {}).values())
            frage_status(frage_id, "beantwortet", antwort=inhalt)
            melde("laeuft", "💬 Antwort von Frank: " + inhalt)
            sys.stdout.write(json.dumps({"decision": "block", "reason": f"Frank hat über Jarvis geantwortet: {inhalt}"}, ensure_ascii=False))
            return
        # Ohne Antwort endet der Zug: Frank antwortet dann am Rechner oder in der Claude-App wie auf jede Nachricht.
        frage_status(frage_id, "am_pc", grund="freigegeben" if antwort else "abgelaufen")
        schreib("Freie Rückfrage ohne Antwort über Jarvis")
        nachtraege_im_stop()
        return
    # Zugende: die ganze letzte Nachricht, so wie sie im Terminal steht (am Schluss die Zusammenfassung), dazu die Dauer
    # seit dem Start des Auftrags (wie „Cooked for …“ im Terminal). Der Stand „info“ ist das Merkmal des ausführlichen
    # Abschlussberichts: Jarvis zeigt ihn mit voller Aktionsleiste, alle anderen Stände als kurze Statusmeldung.
    melde("info", "🏁 " + letzte + f"\n\n⏱️ Dauer seit Start: {dauer}")
    # Danach (nicht davor: „info“ markiert in Jarvis die zuvor übergebenen Nachträge als bearbeitet) die neuen Nachträge.
    nachtraege_im_stop()


def puls(eingabe: dict) -> None:
    """
    PreToolUse (alle Werkzeuge, async), also VOR dem Werkzeug: neue Zwischentexte der KI sofort melden (nicht erst, wenn das
    Werkzeug fertig ist), Advisor, Skills und Agenten beim Start ankündigen, und ein Lebenszeichen für die drehende Anzeige
    in Jarvis (höchstens alle PULS_S).
    """
    werkzeug = str(eingabe.get("tool_name", ""))
    ein = eingabe.get("tool_input") or {}
    with Sperre():
        z = lies_zustand()
        raus = [("laeuft", "💬 " + text) for text in neue_texte(eingabe, z)]
        schreib_zustand(z)
    if werkzeug == "advisor":
        raus.append(("laeuft", "🧠 Advisor wird befragt …"))
    elif werkzeug == "Skill":
        raus.append(("laeuft", f"🧩 Skill {ein.get('skill', '')} geladen".replace("  ", " ")))
    elif werkzeug in ("Agent", "Task"):
        raus.append(("laeuft", "🤖 Agent: " + str(ein.get("description") or ein.get("subagent_type") or "gestartet")))
    sende(raus)
    datei = ORDNER / f"puls-{AUFTRAG}.txt"
    try:
        if time.time() - datei.stat().st_mtime < PULS_S:
            return
    except FileNotFoundError:
        pass
    ORDNER.mkdir(parents=True, exist_ok=True)
    datei.write_text(str(time.time()), encoding="utf-8")
    melde("puls", str(eingabe.get("tool_name", "")))


def main() -> int:
    if not AUFTRAG:
        return 0
    art = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        eingabe = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    except Exception:
        eingabe = {}
    try:
        # Nur warte liefert einen Exit-Code (2 = Sitzung wecken); alle anderen enden mit 0.
        return {"post": post, "frage": frage, "stop": stop, "puls": puls, "warte": warte}.get(art, lambda _: None)(eingabe) or 0
    except Exception as fehler:
        schreib(f"{art}: {type(fehler).__name__}: {fehler}")
        return 0


if __name__ == "__main__":
    sys.exit(main())
