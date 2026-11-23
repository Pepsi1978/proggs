"""Hooks der Antigravity CLI (agy) für Programmieraufträge von Jarvis: Nachträge und Meldungen wie bei Claude Code.

agy lädt Hooks global aus ~/.gemini/config/hooks.json (Abschnitt „jarvis-auftrag“, eingerichtet von einrichten.sh). Ohne
JARVIS_AUFTRAG_ID (jede normale agy-Sitzung) endet jeder Aufruf sofort mit {}. agy-Hooks laufen synchron und halten die
Schleife an (Stand agy 1.5.15): Hier wird nur lokal gelesen; alles mit Netz läuft abgekoppelt, außer der Übergabe eines
Nachtrags selbst.

  session  SessionStart: merkt sich die Hauptsitzung (Subagenten bekommen nie einen Nachtrag) und startet den Warter.
  pre      PreInvocation, vor jedem Modellaufruf und damit direkt nach jedem Werkzeug: Liegt ein Nachtrag im Eingang, wird
           er quittiert (Relay und Jarvis, wie bei Claude Code) und geht als Eingabe von Frank in die Sitzung
           (injectSteps userMessage, Test vom 23.11.2026). Dazu abgekoppelt das Lebenszeichen und neue Zwischentexte.
  post     PostToolUse: abgekoppelt neue Zwischentexte, geänderte Dateien, Bau, Commit, Push, Installation an Jarvis
           (jarvis_hook.post mit JARVIS_CLI=antigravity, wie das OpenCode-Plugin).
  stop     Stop: meldet neue Zwischentexte. Liegt ein Nachtrag im Eingang, läuft die Sitzung weiter (decision continue) und
           pre gibt ihn hinein. Sonst ruht die Sitzung ab hier.
  warter   Abgekoppelt (einer je Auftrag, bis agy endet, höchstens 12 h): holt Nachträge vom Relay in den Eingang. Ruht die
           Sitzung, tippt er einen kurzen Weckruf in ihren Terminal-Tab (AppleScript do script); den Nachtrag selbst gibt pre.
  bericht  Abgekoppelter Teil von pre, post und stop.
Protokoll: ~/Library/Logs/JarvisPcBruecke/hook.log (wie jarvis_hook.py).
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

AUFTRAG = os.environ.get("JARVIS_AUFTRAG_ID", "").strip()
HIER = Path(__file__).resolve().parent
WECKRUF = "📨 Nachtrag von Frank über Jarvis"


def antworte(daten: dict | None = None) -> None:
    sys.stdout.write(json.dumps(daten or {}, ensure_ascii=False))
    sys.stdout.flush()


def main() -> int:
    art = sys.argv[1] if len(sys.argv) > 1 else ""
    if not AUFTRAG:
        if art not in ("warter", "bericht"):
            antworte()
        return 0
    # Vor dem Import: jarvis_hook liest JARVIS_CLI beim Laden (Meldungen ohne Rechner, Anhänge als Pfad, Wortlaut des Rahmens).
    os.environ["JARVIS_CLI"] = "antigravity"
    sys.path.insert(0, str(HIER))
    import jarvis_hook as h
    global H
    H = h
    if art == "warter":
        return warter(int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2].isdigit() else 0, sys.argv[3] if len(sys.argv) > 3 else "")
    try:
        eingabe = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    except Exception:
        eingabe = {}
    if art == "bericht":
        try:
            bericht(eingabe)
        except Exception as fehler:
            h.schreib(f"agy bericht: {type(fehler).__name__}: {fehler}")
        return 0
    try:
        ergebnis = {"session": session, "pre": pre, "post": post, "stop": stop}.get(art, lambda _: None)(eingabe)
    except Exception as fehler:
        h.schreib(f"agy {art}: {type(fehler).__name__}: {fehler}")
        ergebnis = None
    antworte(ergebnis)
    return 0


H = None  # jarvis_hook, nach dem Setzen von JARVIS_CLI geladen


def datei(name: str) -> Path:
    return H.ORDNER / f"agy-{name}-{AUFTRAG}"


# ---------------------------------------------------------------- Sitzung, Warter, Eingang

def agy_prozess() -> tuple[int, str]:
    """PID und Terminal (tty) des agy-Prozesses dieser Sitzung: der nächste Vorfahr namens agy."""
    pid = os.getppid()
    for _ in range(8):
        try:
            teile = subprocess.run(["ps", "-o", "ppid=,tty=,comm=", "-p", str(pid)], capture_output=True, text=True,
                                   timeout=3).stdout.split(None, 2)
        except Exception:
            return 0, ""
        if len(teile) < 3:
            return 0, ""
        if Path(teile[2].strip()).name == "agy":
            return pid, ("" if teile[1] in ("??", "?") else "/dev/" + teile[1])
        pid = int(teile[0])
        if pid <= 1:
            break
    return 0, ""


def hauptsitzung(eingabe: dict, beim_start: bool = False) -> bool:
    """True, wenn der Hook aus der Hauptsitzung kommt. Gemerkt wird „agy-PID Gesprächs-ID“: Ein Subagent läuft im selben
    agy-Prozess und überschreibt den Eintrag nicht; ein neuer agy-Prozess für denselben Auftrag schon."""
    gespraech = str(eingabe.get("conversationId") or "")
    agy, _ = agy_prozess()
    pfad = datei("sitzung")
    try:
        alt_agy, alt_gespraech = pfad.read_text(encoding="utf-8").split(None, 1)
    except Exception:
        alt_agy, alt_gespraech = "", ""
    if not alt_gespraech or (beim_start and alt_agy != str(agy)) or (alt_agy != str(agy) and not H.lebt(int(alt_agy or 0))):
        H.ORDNER.mkdir(parents=True, exist_ok=True)
        pfad.write_text(f"{agy} {gespraech}", encoding="utf-8")
        return True
    return alt_gespraech.strip() == gespraech


def lies_eingang() -> list[dict]:
    try:
        return [n for n in json.loads(datei("eingang.json").read_text(encoding="utf-8")) if isinstance(n, dict)]
    except Exception:
        return []


def schreib_eingang(liste: list[dict]) -> None:
    pfad = datei("eingang.json")
    if not liste:
        pfad.unlink(missing_ok=True)
        return
    teil = pfad.with_name(pfad.name + f".{os.getpid()}.tmp")
    teil.write_text(json.dumps(liste, ensure_ascii=False), encoding="utf-8")
    os.replace(teil, pfad)


def sorge_fuer_warter() -> None:
    """Startet den Warter, wenn keiner lebt (abgekoppelt, hält den Hook nicht auf)."""
    try:
        pid, start = datei("warter.pid").read_text(encoding="utf-8").split(None, 1)
        if H.lebt(int(pid), start.strip()):
            return
    except Exception:
        pass
    agy, tty = agy_prozess()
    subprocess.Popen([sys.executable, str(Path(__file__).resolve()), "warter", str(agy), tty], stdin=subprocess.DEVNULL,
                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)


def abgekoppelt(art: str, eingabe: dict) -> None:
    """bericht im Hintergrund: Netz und Transkript-Lesen halten agy nicht auf."""
    p = subprocess.Popen([sys.executable, str(Path(__file__).resolve()), "bericht"], stdin=subprocess.PIPE,
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
    try:
        p.stdin.write(json.dumps({**eingabe, "agy_art": art}, ensure_ascii=False).encode("utf-8"))
        p.stdin.close()
    except Exception:
        pass


def agy_vorn(tty: str) -> bool:
    """Läuft agy im Vordergrund dieses Terminals? Sonst landete der Weckruf samt Enter in der Shell."""
    try:
        aus = subprocess.run(["ps", "-t", tty.removeprefix("/dev/"), "-o", "stat=,args="], capture_output=True, text=True,
                             timeout=5).stdout
    except Exception:
        return False
    return any("+" in z.split(None, 1)[0] and re.match(r"^(\S*/)?agy(\s|$)", z.split(None, 1)[1])
               for z in aus.splitlines() if len(z.split(None, 1)) == 2)


def wecke(tty: str) -> bool:
    """Tippt den Weckruf in den Terminal-Tab der ruhenden Sitzung; „do script“ hängt das Enter selbst an."""
    if not tty or not agy_vorn(tty):
        H.schreib(f"agy Warter: kein Terminal mit agy im Vordergrund ({tty or 'ohne tty'}), Nachtrag wartet auf die nächste Eingabe")
        return False
    import jarvis_anhaenge
    try:
        r = subprocess.run(["osascript", "-e", jarvis_anhaenge.MAC_SKRIPT, tty, WECKRUF], capture_output=True, text=True, timeout=20)
        return r.stdout.strip() == "ok"
    except Exception as fehler:
        H.schreib(f"agy Warter: Wecken {type(fehler).__name__}: {fehler}")
        return False


def warter(agy: int, tty: str) -> int:
    h = H
    h.ORDNER.mkdir(parents=True, exist_ok=True)
    eigen = f"{os.getpid()} {h.startzeit(os.getpid()) or ''}"
    try:
        with h.Sperre():
            try:
                pid, start = datei("warter.pid").read_text(encoding="utf-8").split(None, 1)
                if int(pid) != os.getpid() and h.lebt(int(pid), start.strip()):
                    return 0
            except Exception:
                pass
            datei("warter.pid").write_text(eigen, encoding="utf-8")
    except h.SperreBelegt:
        return 0
    agy_start = (h.startzeit(agy) or "") if agy else ""
    h.schreib(f"agy Warter gestartet (agy {agy}, {tty or 'ohne tty'})")
    try:
        ende = time.monotonic() + h.WARTER_MAX_S
        while time.monotonic() < ende:
            if agy and not h.lebt(agy, agy_start):
                h.schreib("agy Warter: Sitzung beendet")
                return 0
            try:
                liste = h.hole_nachtraege(25)
            except Exception as fehler:
                h.schreib(f"agy Warter: {fehler}")
                time.sleep(5)
                continue
            neu = []
            if liste:
                try:
                    with h.Sperre():
                        z = h.lies_zustand()
                        eingang = lies_eingang()
                        bekannt = {n["nachtrag_id"] for n in eingang} | set(z["nachtraege"])
                        neu = [n for n in liste if n["nachtrag_id"] not in bekannt]
                        if neu:
                            schreib_eingang(eingang + neu)
                except h.SperreBelegt:
                    pass
            if neu:
                h.schreib(f"agy Warter: {len(neu)} Nachtrag/Nachträge im Eingang")
            # Ruht die Sitzung (nach Stop, kein pre seitdem), weckt nur ein Tippen sie auf; pre gibt dann den Nachtrag.
            if lies_eingang() and datei("ruht").exists():
                datei("ruht").unlink(missing_ok=True)
                if wecke(tty):
                    h.schreib("agy Warter: ruhende Sitzung geweckt")
            if liste and not neu:
                # Der Relay bietet sie bis zur Quittung weiter an: nicht im Kreis fragen.
                time.sleep(3)
        return 0
    finally:
        try:
            if datei("warter.pid").read_text(encoding="utf-8").split()[:1] == [str(os.getpid())]:
                datei("warter.pid").unlink()
        except Exception:
            pass


# ---------------------------------------------------------------- Hooks

def session(eingabe: dict) -> dict | None:
    if hauptsitzung(eingabe, beim_start=True):
        sorge_fuer_warter()
    return None


def pre(eingabe: dict) -> dict | None:
    if not hauptsitzung(eingabe):
        return None
    datei("ruht").unlink(missing_ok=True)
    sorge_fuer_warter()
    abgekoppelt("pre", eingabe)
    if not lies_eingang():
        return None
    try:
        with H.Sperre():
            liste = lies_eingang()
            schreib_eingang([])
    except H.SperreBelegt:
        return None
    try:
        neu = H.beanspruche(liste)
    except H.SperreBelegt:
        # Zurück in den Eingang: Der nächste Modellaufruf versucht es wieder.
        schreib_eingang(liste + lies_eingang())
        return None
    if not neu:
        return None
    text = H.uebergib(neu)
    H.schreib(f"agy: {len(neu)} Nachtrag/Nachträge nach dem letzten Werkzeug übergeben")
    return {"injectSteps": [{"userMessage": text}]}


def post(eingabe: dict) -> dict | None:
    if hauptsitzung(eingabe):
        abgekoppelt("post", eingabe)
    return None


def stop(eingabe: dict) -> dict | None:
    if not hauptsitzung(eingabe):
        return None
    # Erst „ruht“ setzen, dann den Eingang prüfen: Kommt ein Nachtrag genau jetzt, sieht ihn der Stop oder der Warter.
    datei("ruht").touch()
    sorge_fuer_warter()
    abgekoppelt("stop", eingabe)
    if lies_eingang():
        datei("ruht").unlink(missing_ok=True)
        return {"decision": "continue",
                "reason": "Frank hat über Jarvis einen Nachtrag zu diesem Auftrag geschickt; er folgt als nächste Nachricht."}
    return None


# ---------------------------------------------------------------- Meldungen an Jarvis (abgekoppelt)

DATEI_WERKZEUGE = {"edit_file", "write_file", "write_to_file", "replace_file_content", "multi_replace_file_content"}


def befehl(roh) -> str:
    """CommandLine kommt teils als JSON-Zeichenkette („\"git …\"“)."""
    text = str(roh or "")
    if len(text) > 1 and text[0] == text[-1] == '"':
        try:
            return str(json.loads(text))
        except Exception:
            pass
    return text


def neue_schritte(pfad: str) -> list[dict]:
    """Die seit dem letzten Bericht dazugekommenen Schritte des agy-Transkripts (nur vollständige Zeilen)."""
    if not pfad:
        return []
    with H.Sperre():
        z = H.lies_zustand()
        try:
            with open(pfad, "rb") as f:
                groesse = os.fstat(f.fileno()).st_size
                stelle = z.get("agy_stelle", 0) if 0 <= z.get("agy_stelle", 0) <= groesse else 0
                f.seek(stelle)
                roh = f.read()
        except OSError:
            return []
        ende = roh.rfind(b"\n")
        if ende < 0:
            return []
        z["agy_stelle"] = stelle + ende + 1
        H.schreib_zustand(z)
    schritte = []
    for zeile in roh[:ende].decode("utf-8", "replace").splitlines():
        try:
            schritte.append(json.loads(zeile))
        except Exception:
            pass
    return schritte


def werkzeug_ausgabe(pfad: str, schritt: int) -> str:
    """Die Ausgabe eines Werkzeugs (Schritt [schritt] im Transkript), für Bau-, Push- und Installationserkennung."""
    try:
        with open(pfad, "rb") as f:
            f.seek(max(0, os.fstat(f.fileno()).st_size - 400_000))
            for zeile in reversed(f.read().decode("utf-8", "replace").splitlines()):
                if f'"step_index":{schritt},' in zeile.replace(" ", ""):
                    return str(json.loads(zeile).get("content") or "")
    except Exception:
        pass
    return ""


def bericht(eingabe: dict) -> None:
    art = eingabe.get("agy_art")
    pfad = str(eingabe.get("transcriptPath") or "")
    texte = [str(s["content"]) for s in neue_schritte(pfad)
             if s.get("type") == "PLANNER_RESPONSE" and s.get("source") == "MODEL" and s.get("content")]
    if art == "pre":
        H.puls({"tool_name": "", "texte": texte})
        return
    if art == "stop":
        H.post({"tool_name": "", "texte": texte})
        return
    aufruf = eingabe.get("toolCall") or {}
    name, args = str(aufruf.get("name") or ""), aufruf.get("args") or {}
    fehler = str(eingabe.get("error") or "")
    if name == "run_command":
        ausgabe = werkzeug_ausgabe(pfad, int(eingabe.get("stepIdx") or -1))
        m = re.search(r"exited with code (\d+)", ausgabe)
        gescheitert = bool(fehler) or bool(m and m.group(1) != "0")
        H.post({"tool_name": "Bash", "tool_input": {"command": befehl(args.get("CommandLine"))}, "tool_response": ausgabe or fehler,
                "hook_event_name": "PostToolUseFailure" if gescheitert else "PostToolUse", "texte": texte})
    elif name in DATEI_WERKZEUGE:
        H.post({"tool_name": "Edit", "tool_input": {"file_path": str(args.get("Path") or args.get("TargetFile") or "")}, "texte": texte})
    else:
        H.post({"tool_name": name, "tool_input": args, "texte": texte})


if __name__ == "__main__":
    sys.exit(main())
