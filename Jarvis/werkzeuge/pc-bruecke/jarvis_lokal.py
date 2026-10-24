"""Findet am Rechner gestartete Claude-Code-Sitzungen, die (noch) keine Jarvis-Hooks haben, und liest sie für Jarvis mit.

Aufruf: python jarvis_lokal.py scan (macOS: python3), etwa alle 30 s von der PC- bzw. Mac-Brücke. Ein Lauf endet nach Sekunden.

Grundlage ist das Sitzungsregister von Claude Code: <Profilordner>/sessions/<pid>.json je laufender Sitzung (pid, sessionId,
cwd, kind, entrypoint, name, bridgeSessionId), in allen OpenLauncher-Profilen und in ~/.claude. Gezeigt werden nur
interaktive CLI-Sitzungen, deren Prozess noch lebt, die nicht Jarvis gestartet hat (Marke jarvis-<sessionId>.sitzung oder
Name „Jarvis: …“) und die Frank in Jarvis nicht entfernt hat. Sitzungen mit den Profil-Hooks (Marke lokal-<Nummer>.hook,
frisch) meldet jarvis_hook.py selbst; hier nur die übrigen, also die vor dem Update gestarteten.

Mitlesen heißt: Anmeldung in Jarvis (lila, „Gestartet auf dem PC“), neue Texte der KI aus dem Transkript, am Zugende der
letzte als Bericht, solange sie arbeitet ein Lebenszeichen. Rückfragen beantworten und Nachträge schicken geht bei diesen
Sitzungen nicht: Dafür braucht die Sitzung die Hooks, also einen Neustart (claude --resume <Name> setzt sie mit Hooks fort,
unter derselben Nummer). Gleiche Nummer und gleicher Zustand wie der Hook (jarvis_hook.lokale_nummer, hook-<Nummer>.json):
Wechselt eine Sitzung vom Mitlesen zu den Hooks, kommt nichts doppelt.
"""
from __future__ import annotations

import json
import os
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import jarvis_hook as h  # noqa: E402

HOOK_FRISCH_S = 900   # so lange nach dem letzten Hook-Aufruf (der Warter berührt die Marke alle 25 s) gilt die Sitzung als angebunden
SPERRE = h.ORDNER / "lokal-scan.lock"


def registerordner() -> list[Path]:
    profile = Path.home() / "proggs" / "OpenLauncher" / "Profiles"
    return [p for p in list(profile.glob("*/*/sessions")) + [Path.home() / ".claude" / "sessions"] if p.is_dir()]


def transkript(basis: Path, sitzung: str) -> str:
    treffer = sorted((basis / "projects").glob(f"*/{sitzung}.jsonl"), key=lambda p: p.stat().st_mtime, reverse=True)
    return str(treffer[0]) if treffer else ""


def zug_zu_ende(pfad: str) -> bool:
    """Steht die Sitzung an der Eingabe? Der letzte Eintrag von KI oder Nutzer ist eine fertige Antwort (end_turn)."""
    try:
        with open(pfad, "rb") as f:
            groesse = os.fstat(f.fileno()).st_size
            f.seek(max(0, groesse - 400_000))
            zeilen = f.read().split(b"\n")
    except OSError:
        return False
    for zeile in reversed(zeilen):
        if b'"type":"user"' not in zeile and b'"type":"assistant"' not in zeile:
            continue
        try:
            eintrag = json.loads(zeile)
        except Exception:
            continue
        if eintrag.get("isSidechain") or eintrag.get("type") not in ("user", "assistant"):
            continue
        return eintrag.get("type") == "assistant" and (eintrag.get("message") or {}).get("stop_reason") == "end_turn"
    return False


def von_jarvis(pfad: str) -> bool:
    """Beginnt die Sitzung mit einem Programmierauftrag der Jarvis-Brücke (jarvis_pc_bruecke.auftragstext)?"""
    try:
        with open(pfad, "rb") as f:
            kopf = f.read(300_000)
    except OSError:
        return False
    for zeile in kopf.split(b"\n"):
        if b'"type":"user"' not in zeile:
            continue
        try:
            inhalt = (json.loads(zeile).get("message") or {}).get("content")
        except Exception:
            continue
        text = inhalt if isinstance(inhalt, str) else " ".join(b.get("text", "") for b in inhalt or [] if isinstance(b, dict))
        if text.strip():
            return text.lstrip().startswith("Programmierauftrag von Frank über Jarvis")
    return False


def lebt(eintrag: dict) -> bool:
    try:
        pid = int(eintrag.get("pid") or 0)
    except (TypeError, ValueError):
        return False
    # Die Startzeit im Register ist unter Windows dieselbe FILETIME-Zahl wie in jarvis_hook.startzeit.
    return pid > 0 and h.lebt(pid, str(eintrag.get("procStart") or "") if os.name == "nt" else "")


def lies_mit(basis: Path, eintrag: dict) -> None:
    sitzung = str(eintrag.get("sessionId") or "")
    if not sitzung or eintrag.get("kind") != "interactive" or eintrag.get("entrypoint") != "cli" or not lebt(eintrag):
        return
    if (h.ORDNER / f"jarvis-{sitzung}.sitzung").exists() or str(eintrag.get("name") or "").startswith("Jarvis"):
        return
    h.setze_auftrag(h.lokale_nummer(sitzung))
    h.LOKAL = True
    if h.ist_entfernt():
        return
    marke = h.ORDNER / f"lokal-{h.AUFTRAG}.hook"
    if marke.exists() and time.time() - marke.stat().st_mtime < HOOK_FRISCH_S:
        return
    pfad = transkript(basis, sitzung)
    if not h.ZUSTAND.exists() and von_jarvis(pfad):
        # Von Jarvis gestartet, bevor deren Hooks die Marke setzten: nie als lokale Sitzung zeigen (sonst doppelt).
        h._beruehre(h.ORDNER / f"jarvis-{sitzung}.sitzung")
        return
    h.LOKAL_INFO = h.lokal_angaben(sitzung, str(eintrag.get("cwd") or ""), False, str(eintrag.get("name") or ""), str(eintrag.get("bridgeSessionId") or ""))
    neu = h.registriere(pfad)
    if neu:
        h.melde_still_leer(f"💻 Läuft auf dem PC ({h.rechner()}), schon vor dem Jarvis-Update gestartet: Jarvis liest nur mit. "
                           "Rückfragen und Nachträge gehen erst, wenn die Sitzung dort neu gestartet wird (claude --resume). "
                           "Löschen entfernt sie nur aus Jarvis.")
    if not pfad:
        return
    with h.Sperre():
        z = h.lies_zustand()
        texte = h.neue_texte({"transcript_path": pfad}, z)
        fertig = zug_zu_ende(pfad)
        vorher = z.get("scan_arbeitet", False)
        z["scan_arbeitet"] = not fertig
        h.schreib_zustand(z)
    meldungen = [("laeuft", "💬 " + t) for t in texte]
    if fertig and texte:
        meldungen[-1] = ("info", "🏁 " + texte[-1])
    h.sende(meldungen)
    if fertig and vorher and not texte:
        h.melde_still_leer("⏸️ Wartet am PC auf die nächste Eingabe.")
    elif not fertig and not neu:
        h.melde("puls", "")


def scan() -> int:
    h.ORDNER.mkdir(parents=True, exist_ok=True)
    # Nur ein Lauf zugleich: Hängt einer am Netz, startet die Brücke nach 30 s schon den nächsten.
    fd = os.open(SPERRE, os.O_RDWR | os.O_CREAT)
    try:
        if os.name == "nt":
            import msvcrt
            msvcrt.locking(fd, msvcrt.LK_NBLCK, 1)
        else:
            import fcntl
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        os.close(fd)
        return 0
    try:
        if not (Path.home() / "SK" / "Jarvis" / "relay.properties").exists():
            return 0
        for ordner in registerordner():
            for datei in ordner.glob("*.json"):
                try:
                    lies_mit(ordner.parent, json.loads(datei.read_text(encoding="utf-8")))
                except Exception as fehler:
                    h.schreib(f"Scan {datei.name}: {type(fehler).__name__}: {fehler}")
        return 0
    finally:
        os.close(fd)


if __name__ == "__main__":
    sys.exit(scan() if sys.argv[1:2] == ["scan"] else 0)
