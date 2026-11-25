"""CLI-Aufräumen am Mac (Mac-Zentrale → Programmier-CLI → „Aufräumen“): beendet eindeutig inaktive Programmier-CLI-Sitzungen,
damit sie keinen Arbeitsspeicher mehr belegen. Verläufe, Transkripte und Projektdaten bleiben unangetastet.

Kandidat ist eine Sitzung nur, wenn ALLES eindeutig feststeht (sonst gilt „offen lassen“):
- Claude Code mit lesbarem Sitzungsregister (<Profil>/sessions/<pid>.json), interaktiv, Startzeit passt zum lebenden Prozess,
- Status im Register genau „idle“ (der Zug ist zu Ende), letzter Eintrag im Transkript eine fertige Antwort ohne offenes Werkzeug,
- letzte tatsächliche Aktivität (Register, Transkript, Jarvis-Hook) mehr als eine Stunde her,
- kein laufendes Werkzeug (Shell-Kindprozess) und kaum CPU im ganzen Prozessbaum,
- keine offene Rückfrage an Frank (Jarvis-Hook-Zustand), ein Jarvis-Auftrag ist mit „fertig“ oder „fehler“ abgeschlossen.
Andere CLIs (Codex, OpenCode, Kimi, Antigravity als „agy“) werden gezählt, haben aber kein Statusregister: nicht bestimmbar, bleiben offen.

Schließen: älteste zuerst, unmittelbar vorher die volle Prüfung noch einmal, dann SIGTERM (nach 20 s SIGKILL) nur an den
CLI-Prozess, danach SIGHUP an die Shells seines Terminal-Reiters, damit Terminal den Reiter schließt. Erst wenn der Prozess nachweislich weg ist, gilt die Sitzung als beendet; ihr Registereintrag (die Liste der
offenen Sitzungen) wird nur dann entfernt, falls die CLI ihn nicht selbst gelöscht hat.

Aufruf zum Prüfen ohne Wirkung: python3 jarvis_cli_aufraeumen.py probe
"""
from __future__ import annotations

import json
import os
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import jarvis_hook as h  # noqa: E402

INAKTIV_S = 3600            # mehr als eine Stunde ohne Aktivität (fest, unabhängig vom Intervall)
CPU_GRENZE_S = 0.5          # so viel CPU-Zeit darf der Prozessbaum in der Messpause höchstens verbrauchen
MESSPAUSE_S = 2.0
CLIS = ("claude", "codex", "opencode", "kimi", "gemini", "antigravity")
# Programmnamen, unter denen eine CLI tatsächlich läuft, wenn er vom CLI-Namen abweicht (Antigravity startet als „agy“).
CLI_ALIASE = {"agy": "antigravity"}
WARTENDE_FRAGEN = ("am_pc", "jarvis", "warter", "tippt")
EINSTELLUNG = Path.home() / "Library" / "Application Support" / "JarvisPcBruecke" / "cli-aufraeumen.json"
SPERRE = threading.Lock()
EINSTELLUNG_SPERRE = threading.Lock()  # jedes Lesen-Ändern-Schreiben der Einstellungsdatei (Auftrag und Lauf gleichzeitig)


# ---------------------------------------------------------------- Einstellung (Schalter, Intervall, nächster Lauf)

def lies_einstellung() -> dict:
    try:
        e = json.loads(EINSTELLUNG.read_text("utf-8"))
    except Exception:
        e = {}
    stunden = e.get("stunden")
    e["automatik"] = e.get("automatik") is True
    e["stunden"] = stunden if isinstance(stunden, int) and 1 <= stunden <= 24 else 6
    return e


def schreib_einstellung(e: dict) -> None:
    EINSTELLUNG.parent.mkdir(parents=True, exist_ok=True)
    teil = EINSTELLUNG.with_suffix(".neu")
    teil.write_text(json.dumps(e, ensure_ascii=False), "utf-8")
    os.replace(teil, EINSTELLUNG)


def setze_einstellung(automatik, stunden) -> dict:
    """Neuer Schalter und/oder neues Intervall (nur ganze Stunden 1–24). Der nächste Lauf liegt danach ein Intervall in der Zukunft."""
    with EINSTELLUNG_SPERRE:
        return _setze_einstellung(automatik, stunden)


def _setze_einstellung(automatik, stunden) -> dict:
    e = lies_einstellung()
    if automatik is not None:
        e["automatik"] = automatik is True
    if stunden is not None:
        if not isinstance(stunden, int) or isinstance(stunden, bool) or not 1 <= stunden <= 24:
            raise ValueError("Intervall nur 1 bis 24 Stunden")
        e["stunden"] = stunden
    e["naechster"] = time.time() + e["stunden"] * 3600 if e["automatik"] else None
    schreib_einstellung(e)
    return e


def faellig(e: dict, jetzt: float) -> bool:
    """Geplanter Lauf nur bei eingeschalteter Automatik und erreichtem Zeitpunkt (nach Ruhezustand höchstens einmal)."""
    return e.get("automatik") is True and isinstance(e.get("naechster"), (int, float)) and e["naechster"] <= jetzt


# ---------------------------------------------------------------- Prozesse

def prozesse() -> dict[int, dict]:
    """Alle Prozesse des Nutzers: pid → {ppid, tty, cpu (Sekunden), befehl}."""
    aus = subprocess.run(["ps", "-axo", "pid=,ppid=,tty=,time=,command="], capture_output=True, text=True, timeout=10).stdout
    liste = {}
    for zeile in aus.splitlines():
        teile = zeile.split(None, 4)
        if len(teile) < 5:
            continue
        try:
            liste[int(teile[0])] = {"ppid": int(teile[1]), "tty": teile[2], "cpu": _sekunden(teile[3]), "befehl": teile[4]}
        except ValueError:
            continue
    return liste


def _sekunden(text: str) -> float:
    # ps time: [[dd-]hh:]mm:ss.cc
    tage = 0
    if "-" in text:
        t, text = text.split("-", 1)
        tage = int(t)
    werte = [float(x) for x in text.split(":")]
    s = 0.0
    for w in werte:
        s = s * 60 + w
    return tage * 86400 + s


def cli_name(befehl: str) -> str | None:
    teile = befehl.split()
    if not teile:
        return None
    erstes = os.path.basename(teile[0])
    if erstes in CLIS:
        return erstes
    if erstes in CLI_ALIASE:
        return CLI_ALIASE[erstes]
    if erstes in ("node", "bun") and len(teile) > 1:
        zweites = os.path.basename(teile[1])
        if zweites in CLI_ALIASE:
            return CLI_ALIASE[zweites]
        for c in CLIS:
            if zweites == c or zweites.startswith(c + ".") or f"/{c}/" in teile[1] or f"@openai/{c}" in teile[1]:
                return c
    return None


def nachfahren(pid: int, liste: dict[int, dict]) -> list[int]:
    kinder: dict[int, list[int]] = {}
    for p, d in liste.items():
        kinder.setdefault(d["ppid"], []).append(p)
    ergebnis, offen = [], [pid]
    while offen:
        p = offen.pop()
        for k in kinder.get(p, []):
            if k not in ergebnis:
                ergebnis.append(k)
                offen.append(k)
    return ergebnis


def umgebung(pid: int) -> dict[str, str]:
    """Nur die zwei Variablen, die hier zählen (ps eww zeigt die Umgebung eigener Prozesse)."""
    try:
        aus = subprocess.run(["ps", "eww", "-o", "command=", "-p", str(pid)], capture_output=True, text=True, timeout=5).stdout
    except Exception:
        return {}
    werte = {}
    for teil in aus.split():
        for name in ("CLAUDE_CONFIG_DIR", "JARVIS_AUFTRAG_ID"):
            if teil.startswith(name + "="):
                werte[name] = teil.split("=", 1)[1]
    return werte


def startzeit(pid: int) -> str | None:
    """Startzeit wie im Claude-Register (procStart: englisch, UTC); None = Prozess läuft nicht, "" = nicht ermittelbar."""
    try:
        aus = subprocess.run(["ps", "-o", "lstart=", "-p", str(pid)], capture_output=True, text=True, timeout=5,
                             env={**os.environ, "LC_ALL": "C", "TZ": "UTC"}).stdout
    except Exception:
        return ""
    return " ".join(aus.split()) or None


# ---------------------------------------------------------------- Transkript und Hook-Zustand

def transkript(basis: Path, sitzung: str) -> Path | None:
    treffer = []
    for p in (basis / "projects").glob(f"*/{sitzung}.jsonl"):
        try:
            treffer.append((p.stat().st_mtime, p))
        except OSError:
            continue
    return max(treffer)[1] if treffer else None


def _zeitstempel(text) -> float:
    try:
        from datetime import datetime
        return datetime.fromisoformat(str(text).replace("Z", "+00:00")).timestamp()
    except Exception:
        return 0.0


def letzter_zug(pfad: Path) -> tuple[str, float]:
    """(„fertig“ = letzte Antwort ohne offenes Werkzeug, „offen“ = Werkzeug oder Eingabe unbeantwortet, „unklar“) und die Zeit
    dieses letzten echten Eintrags. Die Dateizeit taugt nicht: Claude Code hängt auch ohne Arbeit Metadaten an."""
    try:
        with open(pfad, "rb") as f:
            f.seek(0, 2)
            f.seek(max(0, f.tell() - 400_000))
            zeilen = f.read().split(b"\n")
    except OSError:
        return "unklar", 0.0
    for zeile in reversed(zeilen):
        if b'"type":"user"' not in zeile and b'"type":"assistant"' not in zeile:
            continue
        try:
            e = json.loads(zeile)
        except Exception:
            continue
        # Zusammenfassungen nach einer Verdichtung und Metazeilen sind keine Eingabe, auf die eine Antwort fehlt.
        if e.get("isSidechain") or e.get("type") not in ("user", "assistant") or e.get("isCompactSummary") or e.get("isMeta") \
                or e.get("isVisibleInTranscriptOnly"):
            continue
        zeit = _zeitstempel(e.get("timestamp"))
        if e["type"] == "user":
            inhalt = (e.get("message") or {}).get("content")
            text = inhalt if isinstance(inhalt, str) else " ".join(
                str(b.get("text") or "") for b in inhalt or [] if isinstance(b, dict) and b.get("type") == "text")
            # Von Frank abgebrochen oder nur ein lokaler Befehl (/clear, /model …): darauf antwortet niemand mehr.
            if text.lstrip().startswith(("[Request interrupted by user", "<local-command-stdout>", "<command-name>", "<local-command-caveat>")):
                return "fertig", zeit
            return "offen", zeit
        inhalt = (e.get("message") or {}).get("content")
        if isinstance(inhalt, list) and any(isinstance(b, dict) and b.get("type") == "tool_use" for b in inhalt):
            return "offen", zeit
        return "fertig", zeit
    return "unklar", 0.0


def nebenarbeit_zeit(pfad: Path) -> float:
    """Letzte Änderung an Transkripten von Hintergrund-Agenten dieser Sitzung (<sitzung>/subagents/*.jsonl)."""
    try:
        return max((p.stat().st_mtime for p in (pfad.parent / pfad.stem).glob("**/*.jsonl")), default=0.0)
    except OSError:
        return time.time()


_MERKMALE: dict[str, tuple[float, int, dict]] = {}


# Ein Weckruf gilt bis zu seiner Weckzeit plus Spielraum, ein Cron höchstens so lange, wie Claude Code ihn hält (Review 21.11.2026:
# ein längst ausgelöster Weckruf oder ein Einmal-Cron ohne CronDelete hielt die Sitzung sonst für immer offen).
WECKRUF_SPIEL_S = 15 * 60
CRON_HOECHSTENS_S = 7 * 86400
CRON_EINMAL_S = 86400


def _zeit(eintrag: dict) -> float:
    """Zeitpunkt einer Transkriptzeile (ISO „timestamp“); ohne lesbare Zeit gilt jetzt, also weiter geplant (offen lassen)."""
    try:
        from datetime import datetime
        return datetime.fromisoformat(str(eintrag["timestamp"]).replace("Z", "+00:00")).timestamp()
    except Exception:
        return time.time()


def merkmale(pfad: Path) -> dict:
    """Aus dem ganzen Transkript (zwischengespeichert nach Größe und Änderungszeit): letzter gemeldeter Jarvis-Stand
    (programmier_fortschritt), seit wann noch Sitzungs-Crons bestehen (CronCreate ohne passendes CronDelete) und bis wann der
    letzte Weckruf (ScheduleWakeup, /loop) geplant ist. Nicht lesbar heißt: plant weiter (offen lassen)."""
    try:
        st = pfad.stat()
    except OSError:
        return {"stand": None, "crons": [], "weckruf_bis": float("inf")}
    alt = _MERKMALE.get(str(pfad))
    if alt and alt[0] == st.st_mtime and alt[1] == st.st_size:
        return alt[2]
    stand, crons, weckruf_bis = None, [], 0.0
    try:
        with open(pfad, "rb") as f:
            for zeile in f:
                if b'"tool_use"' not in zeile or not any(w in zeile for w in (b"programmier_fortschritt", b"CronCreate", b"CronDelete", b"ScheduleWakeup")):
                    continue
                try:
                    eintrag = json.loads(zeile)
                    inhalt = (eintrag.get("message") or {}).get("content") or []
                except Exception:
                    continue
                for blk in inhalt:
                    if not isinstance(blk, dict) or blk.get("type") != "tool_use":
                        continue
                    name, eingabe = str(blk.get("name") or ""), blk.get("input") or {}
                    if "programmier_fortschritt" in name:
                        stand = eingabe.get("stand", stand)
                    elif name == "CronCreate":
                        # Einmal-Crons (recurring: false) löschen sich nach dem Auslösen selbst: höchstens einen Tag werten.
                        crons.append(_zeit(eintrag) + (CRON_HOECHSTENS_S if eingabe.get("recurring") is not False else CRON_EINMAL_S))
                    elif name == "CronDelete":
                        if crons:
                            crons.pop(0)
                    elif name == "ScheduleWakeup":
                        if eingabe.get("stop") is True:
                            weckruf_bis = 0.0
                        else:
                            try:
                                verzug = float(eingabe["delaySeconds"]) if eingabe.get("delaySeconds") is not None else 3600.0
                            except (TypeError, ValueError):
                                verzug = 3600.0
                            weckruf_bis = _zeit(eintrag) + verzug + WECKRUF_SPIEL_S
    except OSError:
        return {"stand": None, "crons": [], "weckruf_bis": float("inf")}
    wert = {"stand": stand, "crons": crons, "weckruf_bis": weckruf_bis}
    _MERKMALE[str(pfad)] = (st.st_mtime, st.st_size, wert)
    return wert


def jarvis_abgeschlossen(pfad: Path) -> bool:
    """Hat ein Jarvis-Auftrag zuletzt „fertig“ oder „fehler“ gemeldet (programmier_fortschritt im Transkript)?"""
    return merkmale(pfad)["stand"] in ("fertig", "fehler")


def plant_weiter(pfad: Path) -> bool:
    """Bestehen Sitzungs-Crons oder ein geplanter Weckruf? Dann ruht die Sitzung absichtlich: nicht eindeutig inaktiv."""
    m, jetzt = merkmale(pfad), time.time()
    return m["weckruf_bis"] > jetzt or any(bis > jetzt for bis in m["crons"])


def hook_zustand(nummer: str) -> dict:
    try:
        return json.loads((h.ORDNER / f"hook-{nummer}.json").read_text("utf-8"))
    except Exception:
        return {}


# ---------------------------------------------------------------- Bewertung

def bewerte(s: dict, jetzt: float) -> tuple[str, str]:
    """Reine Entscheidung aus gesammelten Angaben. Ergebnis (zustand, grund); nur zustand „inaktiv“ darf geschlossen werden.
    Zustände: aktiv, wartet, kuerzlich, unklar, inaktiv."""
    if s.get("cli") != "claude":
        return "unklar", "kein Statusregister (nicht bestimmbar)"
    r = s.get("register")
    if not isinstance(r, dict):
        return "unklar", "kein Sitzungsregister"
    if r.get("kind") != "interactive":
        return "unklar", "keine interaktive Sitzung"
    if not s.get("start") or " ".join(str(r.get("procStart") or "").split()) != s["start"]:
        return "unklar", "Register passt nicht zum Prozess"
    status = r.get("status")
    if status == "busy":
        return "aktiv", "arbeitet gerade"
    if status != "idle":
        return ("wartet", "wartet auf eine Antwort") if status in ("waiting", "blocked", "input") else ("unklar", f"Status „{status}“ unklar")
    if s.get("werkzeug_laeuft"):
        return "aktiv", "ein Werkzeug läuft"
    if s.get("cpu_delta") is None or s["cpu_delta"] > CPU_GRENZE_S:
        return ("aktiv", "rechnet gerade") if s.get("cpu_delta") is not None else ("unklar", "CPU nicht messbar")
    if s.get("frage_offen"):
        return "wartet", "Rückfrage an Frank offen"
    zug = s.get("zug")
    if zug == "offen":
        return "wartet", "Zug nicht abgeschlossen"
    if zug != "fertig":
        return "unklar", "Transkript nicht lesbar"
    if s.get("auftrag") and not s.get("auftrag_fertig"):
        return "aktiv", "Jarvis-Auftrag läuft noch"
    if s.get("plant_weiter"):
        return "unklar", "hat einen späteren Lauf geplant"
    letzte = s.get("letzte")
    if not isinstance(letzte, (int, float)) or letzte <= 0:
        return "unklar", "letzte Aktivität unbekannt"
    if jetzt - letzte <= INAKTIV_S:
        return "kuerzlich", "in der letzten Stunde genutzt"
    return "inaktiv", "seit mehr als einer Stunde inaktiv"


def sammle(pid: int, liste: dict[int, dict], vorher: dict[int, float] | None, nachher: dict[int, dict]) -> dict:
    """Alle Angaben zu einer CLI-Sitzung für [bewerte]."""
    d = liste[pid]
    s: dict = {"pid": pid, "cli": cli_name(d["befehl"]), "start": startzeit(pid), "name": cli_name(d["befehl"]) or "CLI"}
    baum = [pid] + nachfahren(pid, liste)
    s["werkzeug_laeuft"] = any("shell-snapshots/snapshot-" in liste[k]["befehl"] for k in baum[1:] if k in liste)
    if vorher is not None:
        s["cpu_delta"] = sum(nachher.get(k, {}).get("cpu", 0.0) for k in baum) - sum(vorher.get(k, 0.0) for k in baum)
    if s["cli"] != "claude":
        return s
    env = umgebung(pid)
    basis = Path(env.get("CLAUDE_CONFIG_DIR") or Path.home() / ".claude")
    s["basis"] = str(basis)
    try:
        r = json.loads((basis / "sessions" / f"{pid}.json").read_text("utf-8"))
    except Exception:
        return s
    s["register"] = r
    sitzung = str(r.get("sessionId") or "")
    s["name"] = str(r.get("name") or s["name"])
    zeiten = [float(r.get(k) or 0) / 1000 for k in ("statusUpdatedAt", "updatedAt")]
    pfad = transkript(basis, sitzung) if sitzung else None
    if pfad:
        s["zug"], zeit = letzter_zug(pfad)
        zeiten += [zeit, nebenarbeit_zeit(pfad)]
        s["plant_weiter"] = plant_weiter(pfad)
    # Tippt Frank gerade (noch ohne Enter), ändert sich nur die Lesezeit des Terminals.
    try:
        zeiten.append(os.stat("/dev/" + d["tty"]).st_atime)
    except OSError:
        s["letzte"] = None
        return s
    nummer = env.get("JARVIS_AUFTRAG_ID") or (h.lokale_nummer(sitzung) if sitzung else "")
    hz = hook_zustand(nummer) if nummer else {}
    zeiten.append(float(hz.get("zuletzt") or 0))
    s["frage_offen"] = bool(hz.get("frei_offen")) or any(
        isinstance(v, dict) and v.get("status") in WARTENDE_FRAGEN for v in (hz.get("fragen") or {}).values())
    if env.get("JARVIS_AUFTRAG_ID"):
        s["auftrag"] = env["JARVIS_AUFTRAG_ID"]
        s["auftrag_fertig"] = bool(pfad) and jarvis_abgeschlossen(pfad)
    s["letzte"] = max(zeiten)
    return s


def unter_cli(pid: int, liste: dict[int, dict]) -> bool:
    """Läuft die Sitzung irgendwo unterhalb einer anderen offenen Terminal-Sitzung (per Werkzeug gestartete CLI)? Dann zählt auf der
    Karte nur die äußere, jede offene Sitzung also genau einmal. Nur fürs Zählen, das Aufräumen behandelt sie wie bisher."""
    gesehen, p = {pid}, liste.get(pid, {}).get("ppid")
    while p in liste and p not in gesehen:
        if cli_name(liste[p]["befehl"]) and liste[p]["tty"] not in ("??", ""):
            return True
        gesehen.add(p)
        p = liste[p]["ppid"]
    return False


def kandidaten(liste: dict[int, dict]) -> list[int]:
    """CLI-Prozesse mit Terminal, deren direkter Elternprozess keine CLI ist (Hilfsprozesse einer CLI fallen weg)."""
    return [p for p, d in liste.items()
            if cli_name(d["befehl"]) and d["tty"] not in ("??", "") and not cli_name(liste.get(d["ppid"], {}).get("befehl", ""))]


def zaehlbar(liste: dict[int, dict]) -> frozenset[int]:
    """Billige Erkennung ohne Bewertung: die PIDs der Sitzungen, die die Karte zählt."""
    return frozenset(p for p in kandidaten(liste) if not unter_cli(p, liste))


def sitzungen(ohne: int | None = None) -> list[dict]:
    """Alle offenen interaktiven CLI-Sitzungen mit Zustand, älteste Aktivität zuerst. [ohne] = eigene Sitzung (nie schließen).
    „verschachtelt“ = läuft unter einer anderen Sitzung: zählt nicht auf der Karte, wird aber wie bisher bewertet und aufgeräumt."""
    vorher_liste = prozesse()
    pids = kandidaten(vorher_liste)
    vorher = {p: d["cpu"] for p, d in vorher_liste.items()}
    time.sleep(MESSPAUSE_S)
    nachher = prozesse()
    jetzt = time.time()
    ergebnis = []
    for pid in pids:
        if pid not in nachher or pid == ohne:
            continue
        try:
            s = sammle(pid, nachher, vorher, nachher)
            s["zustand"], s["grund"] = bewerte(s, jetzt)
        except Exception as fehler:
            s = {"pid": pid, "name": cli_name(nachher[pid]["befehl"]) or "CLI", "zustand": "unklar",
                 "grund": f"nicht lesbar ({type(fehler).__name__})"}
        s["verschachtelt"] = unter_cli(pid, nachher)
        ergebnis.append(s)
    ergebnis.sort(key=lambda s: s.get("letzte") or jetzt)
    return ergebnis


def kurz(s: dict, jetzt: float | None = None) -> dict:
    jetzt = jetzt or time.time()
    letzte = s.get("letzte")
    return {"name": s.get("name"), "cli": s.get("cli"), "zustand": s.get("zustand"), "grund": s.get("grund"),
            "inaktiv_min": int((jetzt - letzte) // 60) if isinstance(letzte, (int, float)) and letzte > 0 else None,
            **({"reiter_zu": s["reiter_zu"]} if "reiter_zu" in s else {})}


# ---------------------------------------------------------------- Schließen

def _weg(pid: int, start: str) -> bool:
    jetzt = startzeit(pid)
    return jetzt is None or (bool(jetzt) and jetzt != start)


def beende(s: dict) -> bool:
    """SIGTERM an den CLI-Prozess, nach 20 s SIGKILL; True nur, wenn er nachweislich nicht mehr läuft."""
    pid, start = s["pid"], s["start"]
    if not start:
        return False
    # Vorher merken: Nach einem SIGKILL blieben MCP-Server und Hintergrund-Shells sonst als Waisen im Speicher.
    baum = [(k, startzeit(k) or "") for k in nachfahren(pid, prozesse())]
    try:
        os.kill(pid, signal.SIGTERM)
    except ProcessLookupError:
        return _weg(pid, start)
    except OSError:
        return False
    for warte, sig in ((20, signal.SIGKILL), (5, None)):
        ende = time.monotonic() + warte
        while time.monotonic() < ende:
            if _weg(pid, start):
                return True
            time.sleep(0.5)
        if sig is None or _weg(pid, start):
            break
        try:
            os.kill(pid, sig)
        except OSError:
            pass
    if not _weg(pid, start):
        return False
    waisen_beenden(baum)
    return True


def waisen_beenden(baum: list[tuple[int, str]]) -> None:
    """Übrig gebliebene Kindprozesse der beendeten CLI (nur dieselben Prozesse: PID und Startzeit) beenden."""
    for sig, warte in ((signal.SIGTERM, 3), (signal.SIGKILL, 1)):
        lebend = [(k, st) for k, st in baum if st and not _weg(k, st)]
        if not lebend:
            return
        for k, _ in lebend:
            try:
                os.kill(k, sig)
            except OSError:
                pass
        ende = time.monotonic() + warte
        while time.monotonic() < ende and any(not _weg(k, st) for k, st in lebend):
            time.sleep(0.2)


SHELLS = ("zsh", "bash", "sh", "fish")


def reiter_shells(pid: int, liste: dict[int, dict]) -> list[tuple[int, str]]:
    """Die Shells des Terminal-Reiters über der CLI (innerste zuerst), nur wenn die Kette im selben tty bei „login“ endet
    (Terminal.app-Reiter). Sonst (tmux, fremdes Terminal) leer: Dann wird nur die CLI beendet."""
    d = liste.get(pid)
    if not d:
        return []
    tty, kette, p = d["tty"], [], d["ppid"]
    while p in liste and liste[p]["tty"] == tty:
        teile = liste[p]["befehl"].split()
        name = os.path.basename(teile[0]).lstrip("-") if teile else ""
        if name == "login":
            return [(k, startzeit(k) or "") for k in kette]
        if name not in SHELLS:
            return []
        kette.append(p)
        p = liste[p]["ppid"]
    return []


def schliesse_reiter(shells: list[tuple[int, str]], liste_vorher: dict[int, dict]) -> bool:
    """Nach bestätigtem Ende der CLI: die Shells des Reiters mit SIGHUP beenden (wie beim Schließen des Fensters), innerste zuerst.
    Endet die letzte Shell, schließt Terminal den Reiter (Profil „Beim Beenden der Shell: Fenster schließen“). Nur Shells ohne
    weitere Kindprozesse, sonst bleibt der Reiter offen. True = alle Shells des Reiters sind weg."""
    if not shells:
        return False
    for pid, start in shells:
        if _weg(pid, start):
            continue
        jetzt = prozesse()
        fremde = [k for k in nachfahren(pid, jetzt) if not any(k == q for q, _ in shells)]
        if fremde:
            return False
        try:
            os.kill(pid, signal.SIGHUP)
        except OSError:
            pass
        ende = time.monotonic() + 5
        while time.monotonic() < ende and not _weg(pid, start):
            time.sleep(0.2)
    return all(_weg(pid, start) for pid, start in shells)


def aus_liste_entfernen(s: dict) -> None:
    """Erst nach bestätigtem Ende: den Registereintrag der Sitzung (Liste der offenen Sitzungen), falls die CLI ihn nicht selbst
    entfernt hat. Transkript, Verlauf und Projektdaten bleiben."""
    basis = s.get("basis")
    if not basis:
        return
    ordner = Path(basis) / "sessions"
    for datei in [ordner / f"{s['pid']}.json"] + list(ordner.glob(f"{s['pid']}.*.key")):
        try:
            r = json.loads(datei.read_text("utf-8")) if datei.suffix == ".json" else None
            if r is not None and str(r.get("sessionId")) != str((s.get("register") or {}).get("sessionId")):
                continue
            datei.unlink()
        except FileNotFoundError:
            pass
        except Exception:
            pass


def aufraeumen(art: str, ohne: int | None = None, trocken: bool = False) -> dict:
    """Ein Lauf (art „hand“ oder „auto“). Gibt das Ergebnis für die Karte zurück; nie zwei Läufe gleichzeitig."""
    if not SPERRE.acquire(blocking=False):
        return {"art": art, "zeit": time.time(), "text": "Ein Aufräumen läuft bereits.", "beendet": [], "offen": []}
    try:
        alle = sitzungen(ohne)
        beendet, fehlgeschlagen = [], []
        for s in [x for x in alle if x["zustand"] == "inaktiv"]:
            # Unmittelbar vor dem Schließen alles noch einmal frisch prüfen.
            liste_a = prozesse()
            if s["pid"] not in liste_a:
                s["zustand"] = "weg"
                continue
            time.sleep(MESSPAUSE_S)
            liste_b = prozesse()
            if s["pid"] not in liste_b:
                s["zustand"] = "weg"
                continue
            try:
                neu = sammle(s["pid"], liste_b, {p: d["cpu"] for p, d in liste_a.items()}, liste_b)
                neu["zustand"], neu["grund"] = bewerte(neu, time.time())
            except Exception as fehler:
                s.update(zustand="unklar", grund=f"nicht lesbar ({type(fehler).__name__})")
                continue
            if neu["zustand"] != "inaktiv" or neu.get("start") != s.get("start"):
                s.update(neu)
                continue
            if trocken:
                s["grund"] = "würde beendet (Probelauf)"
                continue
            reiter = reiter_shells(neu["pid"], liste_b)
            if beende(neu):
                aus_liste_entfernen(neu)
                s["zustand"] = "beendet"
                s["reiter_zu"] = schliesse_reiter(reiter, liste_b)
                beendet.append(s)
            else:
                s["grund"] = "Ende nicht bestätigt, bleibt offen"
                s["zustand"] = "unklar"
                fehlgeschlagen.append(s)
        jetzt = time.time()
        # Für die Karte zählt jede Sitzung genau einmal: unter einer anderen gestartete fallen aus Liste und Zählung.
        offen = [x for x in alle if x["zustand"] not in ("beendet", "weg") and not x.get("verschachtelt")]
        zaehl = {z: sum(1 for x in offen if x["zustand"] == z) for z in ("aktiv", "wartet", "kuerzlich", "unklar", "inaktiv")}
        teile = [f"{n} {name}" for name, n in (("aktiv", zaehl["aktiv"]), ("warten auf Antwort", zaehl["wartet"]),
                                               ("kürzlich genutzt", zaehl["kuerzlich"]), ("unklar", zaehl["unklar"]),
                                               ("würden beendet (Probelauf)" if trocken else "inaktiv", zaehl["inaktiv"])) if n]
        text = (f"{len(beendet)} von {len(beendet) + len(offen)} Sitzungen beendet" if beendet else f"Keine Sitzung beendet ({len(offen)} offen)")
        if offen:
            text += ". Offen: " + (", ".join(teile) if teile else f"{len(offen)}")
        if fehlgeschlagen:
            text += f". {len(fehlgeschlagen)} ließ sich nicht bestätigt beenden"
        offene_reiter = sum(1 for x in beendet if not x.get("reiter_zu"))
        if offene_reiter:
            text += f". {offene_reiter} Reiter blieb offen (kein Terminal-Reiter oder noch andere Prozesse darin)"
        return {"art": art, "zeit": jetzt, "text": text + ".", "beendet": [kurz(x, jetzt) for x in beendet],
                "offen": [kurz(x, jetzt) for x in offen]}
    finally:
        SPERRE.release()


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "probe":
        print(json.dumps(aufraeumen("probe", ohne=int(sys.argv[2]) if len(sys.argv) > 2 else None, trocken=True), ensure_ascii=False, indent=1))
