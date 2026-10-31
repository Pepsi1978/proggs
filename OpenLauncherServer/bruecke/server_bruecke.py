"""Jarvis-Server-Brücke: holt Programmieraufträge vom Jarvis-Relay und startet sie über OpenLauncher Server in tmux.

Gegenstück zu Jarvis/werkzeuge/pc-bruecke/jarvis_pc_bruecke.py (Mac). Meldet sich mit X-Jarvis-Rang: server. Fragt erst ab, wenn der
Relay /pc/faehigkeiten mit dem Rang server beantwortet: Ein älterer Relay führte die Brücke sonst als Windows-PC und schickte ihr
dessen Aufträge. Ist der Server ausgelastet, meldet sie X-Jarvis-Belegt: 1, und der Auftrag geht an den nächsten Rechner.
"""
from __future__ import annotations

import fcntl
import json
import os
import re
import socket
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "kern"))
import launcher  # noqa: E402
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "Jarvis" / "werkzeuge" / "pc-bruecke"))
import jarvis_anhaenge  # noqa: E402

ORDNER = Path.home() / "JarvisPcBruecke"
PROTOKOLL = ORDNER / "bruecke.log"
ANMELDUNG_ALLE_S = 300
ANMELDUNG = {"stand": "unbekannt", "zeit": 0.0}
HOOK = launcher.HOOK

# Nur IPv4, wie an den anderen Brücken (bugs/desktop/dotnet-httpclient-ipv6-haenger.md).
_original = socket.getaddrinfo
socket.getaddrinfo = lambda host, port, family=0, *a, **k: _original(host, port, socket.AF_INET, *a, **k)


def schreib(text: str) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        if PROTOKOLL.exists() and PROTOKOLL.stat().st_size > 1024 * 1024:
            os.replace(PROTOKOLL, PROTOKOLL.with_name("bruecke.log.alt"))
        with PROTOKOLL.open("a", encoding="utf-8") as f:
            f.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} {text}\n")
    except OSError:
        pass


def lies_zugang() -> dict:
    werte = {}
    for zeile in (Path.home() / "SK" / "Jarvis" / "relay.properties").read_text(encoding="utf-8").splitlines():
        m = re.match(r"^\s*([^#=]+?)\s*=\s*(.*)$", zeile)
        if m:
            werte[m.group(1)] = m.group(2).strip()
    if not werte.get("host") or not werte.get("token"):
        raise RuntimeError("host oder token fehlt in relay.properties")
    return werte


def belegt() -> bool:
    try:
        return launcher.belegt()
    except Exception as fehler:
        schreib(f"Auslastung nicht lesbar: {fehler}")
        return False


def rufe(adresse: str, token: str, sekunden: int, rumpf: dict | None = None, kopf: dict | None = None) -> tuple[int, bytes]:
    anfrage = urllib.request.Request(adresse, method="POST" if rumpf is not None else "GET",
                                     data=json.dumps(rumpf, ensure_ascii=False).encode("utf-8") if rumpf is not None else None,
                                     headers={"X-Jarvis-Token": token, "X-Jarvis-Rechner": launcher.RECHNER, "X-Jarvis-Rang": "server", "X-Jarvis-Katalog": "1", **(kopf or {}),
                                              "X-Jarvis-Anmeldung": ANMELDUNG["stand"], "X-Jarvis-Anmeldung-Zeit": str(int(ANMELDUNG["zeit"])),
                                              "Content-Type": "application/json; charset=utf-8"})
    try:
        with urllib.request.urlopen(anfrage, timeout=sekunden) as antwort:
            return antwort.status, antwort.read()
    except urllib.error.HTTPError as fehler:
        return fehler.code, b""


def pruefe_anmeldung() -> None:
    stand = launcher.claude_anmeldung()
    if stand != ANMELDUNG["stand"]:
        schreib(f"Anmeldung: {stand}")
    ANMELDUNG.update(stand=stand, zeit=time.time())


def wahl(auftrag: dict) -> dict:
    """Welches CLI mit welchem Modell: Standards wie auf Mac und Windows, bei modell=katalog genau Franks geprüfte Angaben."""
    modell = auftrag.get("modell")
    if modell == "katalog":
        return {"cli": str(auftrag.get("cli") or "claude"), "modell": str(auftrag.get("slug") or ""), "anbieter": str(auftrag.get("anbieter") or ""),
                "effort": str(auftrag.get("effort") or "high"), "profil": str(auftrag.get("profil") or "minimal"), "modus": str(auftrag.get("modus") or "schnell"),
                "name": str(auftrag.get("name") or auftrag.get("slug") or "")}
    if modell == "sol":
        cli = "opencode" if auftrag.get("cli") == "opencode" else "codex"
        return {"cli": cli, "modell": "gpt-6.1-sol", "anbieter": "openai", "effort": "high", "profil": "minimal", "modus": "schnell", "name": "Sol 6.1"}
    return {"cli": "claude", "modell": "claude-opus-5-5[1m]", "anbieter": "anthropic", "effort": "high", "profil": "minimal", "modus": "schnell", "name": ""}


def auftragstext(auftrag: dict, cli: str) -> str:
    """Wie auftragstext() der Mac-Brücke, mit dem Abschluss dieses Servers. Der Auftrag geht zeilengenau hinein."""
    text = (auftrag.get("auftrag") or "").replace("\r\n", "\n").replace("\r", "\n")
    kopf = ("Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); die Sitzung läuft auf seinem Hostinger-Server, Frank verfolgt sie in Jarvis und in der Server-Konsole.\n"
            f'MELDEN (statt programmier_fortschritt, das es hier nicht gibt, auch wenn der Auftrag es nennt): python3 "{HOOK}" melde <laeuft|fertig|fehler> "<ein Satz>"')
    unklar1 = "Nicht eindeutig: Frank mit AskUserQuestion fragen und die Kandidaten nennen."
    unklar2 = "2. Wesentliche Unklarheiten mit AskUserQuestion klären, Kleinigkeiten selbst entscheiden."
    review = ""
    if cli == "opencode":
        unklar1 = "Nicht eindeutig: Frank mit dem Werkzeug question fragen und die Kandidaten nennen."
        unklar2 = "2. Wesentliche Unklarheiten mit dem Werkzeug question klären, Kleinigkeiten selbst entscheiden."
    elif cli in ("codex", "kimi", "antigravity"):
        unklar1 = "Nicht eindeutig: die wahrscheinlichste App nehmen und das in der ersten Meldung nennen."
        unklar2 = "2. Niemand antwortet in dieser Sitzung: Unklarheiten selbst sinnvoll entscheiden und in der Meldung nennen, nie auf eine Antwort warten."
        review = ("REVIEW: Ja (vor dem Abschluss den gesamten Diff gründlich selbst auf Fehler prüfen und Befunde beheben)\n"
                  if auftrag.get("review") else "REVIEW: Nein\n")
    return f"""{kopf}

APP: {auftrag.get("app") or ""}
{review}AUFTRAG:
{text}

VORGEHEN
1. App im Repo finden. Bekannt: Geniale Aufgaben = Aufgaben, Genialer Wecker = GenialerWeckerAndroid, Geniale Ideen = GenialeIdeen, Entropie Reductor = EntropieReductor, Jarvis = Jarvis, UpdateStation = UpdateStation. {unklar1}
{unklar2}
3. Nach den Profilregeln und den Server-Regeln vollständig abschließen: bauen, Version, Commit, Push, Installation über den Tunnel (die App Jarvis nie mit nacktem adb install, sondern immer über Werkzeuge/jarvis-install). Den Skill apk-update hier nicht starten: Der Push vom Server löst den Android-Cloud-Bau aus, der die APK in den Drive-Ordner legt. Abschlussmeldung auf Deutsch.
"""


def starte(auftrag: dict, w: dict) -> str:
    app = re.sub(r"[^\w ._-]", "", str(auftrag.get("app", ""))).strip() or "App"
    titel = f"Jarvis: {app}" + (f" ({re.sub(r'[^\w ._()-]', '', w['name'])}" + ("" if w["cli"] == "claude" else f", {w['cli']}") + ")" if w["name"] else "")
    nummer = str(auftrag.get("nummer", ""))
    launcher.platz_schaffen()
    if auftrag.get("anhaenge"):
        # Vor dem Start vollständig auf dem Server; die Bilder fügt der SessionStart-Hook über tmux als [Image #n] ein.
        zugang = lies_zugang()
        auftrag = {**auftrag, "auftrag": (auftrag.get("auftrag") or "") + jarvis_anhaenge.fuer_auftrag(auftrag, zugang["host"], zugang["token"],
                                                                                                       w["cli"] == "claude" and bool(re.fullmatch(r"[A-Za-z0-9]{1,20}", nummer)))}
    name = launcher.starten(w["cli"], w["modell"], w["anbieter"], w["effort"], w["profil"], w["modus"], titel, auftragstext(auftrag, w["cli"]),
                            nummer if re.fullmatch(r"[A-Za-z0-9]{1,20}", nummer) else "")
    if w["cli"] != "claude":
        # Nur Claude Code lässt sich vorab fragen; die übrigen zeigen den fehlenden Login erst beim Start.
        time.sleep(8)
        if launcher.wartet_auf_anmeldung(name):
            launcher.schliessen(name)
            raise RuntimeError(f"{w['cli']} auf dem Hostinger-Server ist nicht angemeldet (Server-Konsole, Reiter System).")
    return titel


def bearbeite(auftrag: dict, zugang: dict) -> None:
    w = wahl(auftrag)
    try:
        if launcher.belegt():
            raise RuntimeError("Der Hostinger-Server ist gerade ausgelastet.")
        # Nicht angemeldet: sicher nicht gestartet, also darf der nächste Rechner oder die Cloud übernehmen. Der Server läuft unbeaufsichtigt;
        # anders als am eigenen Rechner bliebe sonst jeder Auftrag liegen, bis Frank die Konsole öffnet.
        if w["cli"] == "claude":
            pruefe_anmeldung()
            if ANMELDUNG["stand"] == "abgemeldet":
                raise RuntimeError("Claude Code auf dem Hostinger-Server ist nicht angemeldet (Server-Konsole, Reiter System).")
        titel = starte(auftrag, w)
        quittung = {"id": auftrag.get("id"), "ok": True, "text": f"Sitzung '{titel}' gestartet"}
        schreib(f"Sitzung '{titel}' gestartet")
    except Exception as fehler:
        # „weiter“: Der Relay gibt den Auftrag dem nächsten Rechner; gibt es keinen, geht diese Absage ans Handy.
        quittung = {"id": auftrag.get("id"), "ok": False, "weiter": True, "text": f"Nicht gestartet: {fehler}"}
        schreib(quittung["text"])
    _quittiere(quittung, zugang)


def _quittiere(quittung: dict, zugang: dict) -> None:
    for versuch in range(1, 4):
        try:
            code, _ = rufe(f"https://{zugang['host']}/pc/quittung", zugang["token"], 20, quittung)
            if code < 300:
                return
            raise RuntimeError(f"HTTP {code}")
        except Exception as fehler:
            schreib(f"Quittung, Versuch {versuch}: {fehler}")
            time.sleep(2)


def lange_befehle(gemeldet: dict, zugang: dict) -> None:
    """Hooks melden erst, wenn ein Werkzeug fertig ist. Hier dauert ein Gradle-Bau Minuten, und in Jarvis stünde so lange nichts: Läuft ein Werkzeug länger als
    eine Minute, sagt die Brücke einmal was, und alle 5 Minuten, dass es noch läuft. Quelle ist die Merkdatei des PreToolUse-Hooks. Ohne „rechner“: Der Relay
    wertete die Meldung sonst als Beweis einer gültigen Anmeldung, dabei hat hier kein Modell geantwortet."""
    jetzt = time.time()
    for schluessel in [k for k, v in gemeldet.items() if jetzt - v > 3600]:
        del gemeldet[schluessel]
    for s in launcher.sitzungen():
        nummer = s.get("auftrag")
        if not nummer or s.get("cli") != "claude" or s["stand"] != "arbeitet":
            continue
        try:
            datei = launcher.SITZUNGEN / f"{s['name']}.werkzeug"
            start = datei.stat().st_mtime
            werkzeug = json.loads(datei.read_text("utf-8"))
        except (OSError, ValueError):
            continue
        name = str(werkzeug.get("tool_name", ""))
        # Eine Rückfrage wartet auf Frank, und der sieht sie schon.
        if jetzt - start < 60 or name == "AskUserQuestion" or jetzt - gemeldet.get((nummer, start), 0) < 300:
            continue
        eingabe = werkzeug.get("tool_input") or {}
        was = str(eingabe.get("description") or eingabe.get("command") or eingabe.get("prompt") or name)[:150]
        schritt = f"⏳ Läuft seit {int(jetzt - start) // 60} min: {was}"
        try:
            code, _ = rufe(f"https://{zugang['host']}/pc/meldung", zugang["token"], 20, {"auftrag_id": nummer, "stand": "laeuft", "schritt": schritt})
        except Exception as fehler:
            schreib(f"Fortschritt {nummer}: {fehler}")
            continue
        schreib(f"Fortschritt {nummer}: {schritt} -> {code}")
        if code < 300:
            gemeldet[(nummer, start)] = jetzt


def fortschritt_faden() -> None:
    gemeldet: dict = {}
    while True:
        time.sleep(30)
        try:
            lange_befehle(gemeldet, lies_zugang())
        except Exception as fehler:
            schreib(f"Fortschritt: {fehler}")


def relay_kennt_server(zugang: dict) -> bool:
    code, roh = rufe(f"https://{zugang['host']}/pc/faehigkeiten", zugang["token"], 20)
    return code == 200 and "server" in json.loads(roh.decode("utf-8")).get("raenge", [])


def main() -> int:
    ORDNER.mkdir(parents=True, exist_ok=True)
    sperre = open(ORDNER / "bruecke.sperre", "w")
    try:
        fcntl.flock(sperre, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        return 0
    schreib("Brücke gestartet")
    threading.Thread(target=fortschritt_faden, daemon=True).start()
    dateien = [Path(__file__), Path(launcher.__file__)]
    stand = [d.stat().st_mtime for d in dateien]
    pause, geprueft = 2, False
    letzter_scan = 0.0
    while True:
        try:
            # Neue Fassung (git pull): beenden, systemd startet die neue.
            if [d.stat().st_mtime for d in dateien] != stand:
                schreib("Neue Fassung, Neustart")
                return 0
            zugang = lies_zugang()
            if not geprueft:
                if not relay_kennt_server(zugang):
                    schreib("Der Relay kennt den Rang server nicht, warte")
                    time.sleep(60)
                    continue
                geprueft = True
            if time.time() - ANMELDUNG["zeit"] >= ANMELDUNG_ALLE_S:
                ANMELDUNG["zeit"] = time.time()
                threading.Thread(target=pruefe_anmeldung, daemon=True).start()
            if time.time() - letzter_scan >= 600:
                letzter_scan = time.time()
                launcher.aufraeumen()
            code, roh = rufe(f"https://{zugang['host']}/pc/auftrag", zugang["token"], 40, kopf={"X-Jarvis-Belegt": "1" if belegt() else "0"})
            pause = 2
            if code == 204:
                continue
            if code != 200:
                raise RuntimeError(f"HTTP {code}")
            auftrag = json.loads(roh.decode("utf-8"))
            schreib(f"Auftrag für {auftrag.get('app')}")
            bearbeite(auftrag, zugang)
        except FileNotFoundError as fehler:
            schreib(f"Datei fehlt: {fehler.filename}")
            time.sleep(60)
        except Exception as fehler:
            schreib(f"Fehler: {fehler}")
            time.sleep(pause)
            pause = min(pause * 2, 10)


if __name__ == "__main__":
    sys.exit(main())
