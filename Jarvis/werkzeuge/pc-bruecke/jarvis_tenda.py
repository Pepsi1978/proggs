"""Tenda-Mobilfunkrouter (5G03, O2-SIM) über seine lokale Verwaltungsoberfläche http://192.168.0.1 lesen und schalten.

Läuft auf dem Rechner zu Hause (Mac-Brücke, jarvis_pc_bruecke.py). Der Stand geht an den Relay (POST /pc/tenda), das Handy liest
ihn dort (GET /geraet/tenda) und bewertet ihn (TendaFaehigkeit.kt). Plattformneutral (Ping-Aufruf für macOS, Linux und Windows).

Anmeldung wie die Weboberfläche: POST /login/Auth {"username": "admin", "password": md5(Passwort)}; danach gilt das Sitzungs-Cookie.
Der Router sperrt nach 4 Fehlversuchen und erlaubt höchstens 3 angemeldete Geräte: Darum genau ein Anmeldeversuch je Lauf, neu nur
nach errCode 1000 (abgemeldet). Falsches Passwort (errCode 4) merkt sich die Datei, danach kein Versuch mehr, bis Frank es neu eingibt.
Zugang: ~/SK/Tenda/zugang.json {"hash": "<md5 hex>"} (Rechte 600). Nur der Hash, nie das Passwort; er kommt vom Einrichtungsdialog
des Handys über den Relay (befehl tenda, art zugang) und steht nie im Protokoll.

Schreiben: Die Oberfläche schickt immer das ganze simWan-Objekt (Profilliste mit APN). Darum lesen, ein Feld ändern, ganz zurück.
dataOptions: 5 = 5G SA bevorzugt, 4 = 5G NSA, 2 = nur 4G. action: 1 = verbinden, 0 = trennen.
Neustart: POST /goform/setModules {"reboot": {"action": "reboot"}} (Antwort errCode 100 = startet neu; setSystemTools im Skript ist ungenutzt).
"""
from __future__ import annotations

import hashlib
import http.cookiejar
import json
import os
import platform
import random
import re
import statistics
import subprocess
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

ROUTER = "http://192.168.0.1"
ZUGANG = Path.home() / "SK" / "Tenda" / "zugang.json"
PING_ZIEL = "1.1.1.1"
PING_ANZAHL = 20
MODI = {"5": "5G SA bevorzugt", "4": "5G NSA", "2": "nur 4G", "3": "nur 3G"}
# Nie aus dem Router weitergeben: Kennungen der SIM und des Geräts, Adressen, Zugangsdaten.
# Auch Zellkennungen (cell, eNB/gNB, TAC, PCI): Mit öffentlichen Mastkarten verraten sie den Standort.
GEHEIM = re.compile(r"imei|imsi|iccid|msisdn|phone|number|pin|puk|mac|ip|dns|gateway|apn|user|pwd|pass|lat|lon|location|cell|enode|enb|gnb|tac|lac|pci|eci", re.I)
# Zahlen, die am Handy bewertet werden: harmlos und klein.
_SPERRE = threading.Lock()


class TendaFehler(Exception):
    def __init__(self, text: str, art: str = "fehler"):
        super().__init__(text)
        self.art = art


def zugang_lesen() -> dict:
    try:
        return json.loads(ZUGANG.read_text("utf-8"))
    except Exception:
        return {}


def zugang_speichern(hash_hex: str) -> None:
    if not re.fullmatch(r"[0-9a-f]{32}", hash_hex or ""):
        raise TendaFehler("Ungültiger Zugang", "zugang")
    ZUGANG.parent.mkdir(parents=True, exist_ok=True)
    os.chmod(ZUGANG.parent, 0o700)
    zwischen = ZUGANG.with_suffix(".neu")
    zwischen.write_text(json.dumps({"hash": hash_hex}), "utf-8")
    os.chmod(zwischen, 0o600)
    os.replace(zwischen, ZUGANG)


def _falsch_merken(benutzt: str) -> None:
    """Nur den Hash als falsch markieren, mit dem gerade angemeldet wurde: Ein inzwischen neu eingegebener bleibt unberührt."""
    z = zugang_lesen()
    if z.get("hash") and z.get("hash") == benutzt:
        z["falsch"] = True
        ZUGANG.write_text(json.dumps(z), "utf-8")


class Router:
    """Eine Sitzung mit Cookie; bleibt im Prozess der Brücke bestehen, damit nicht jede Abfrage neu anmeldet."""

    def __init__(self):
        self.kekse = http.cookiejar.CookieJar()
        self.oeffner = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.kekse), urllib.request.ProxyHandler({}))

    def _rufe(self, pfad: str, rumpf: dict | None = None, sekunden: float = 8) -> dict:
        daten = None if rumpf is None else json.dumps(rumpf).encode("utf-8")
        anfrage = urllib.request.Request(ROUTER + pfad, data=daten, method="GET" if rumpf is None else "POST")
        if daten is not None:
            anfrage.add_header("Content-Type", "application/json; charset=UTF-8")
        with self.oeffner.open(anfrage, timeout=sekunden) as antwort:
            roh = antwort.read(1_000_000).decode("utf-8", "replace")
        try:
            return json.loads(roh)
        except ValueError:
            raise TendaFehler("Router antwortet unverständlich")

    def anmelden(self) -> None:
        z = zugang_lesen()
        if not z.get("hash"):
            raise TendaFehler("Router-Passwort fehlt: bitte in Jarvis auf der Karte „Tenda Internet“ einrichten.", "zugang")
        if z.get("falsch"):
            raise TendaFehler("Router-Passwort ist falsch: bitte in Jarvis neu eingeben (keine weiteren Versuche, sonst sperrt der Router).", "zugang")
        antwort = self._rufe("/login/Auth", {"username": "admin", "password": z["hash"]})
        code = int(antwort.get("errCode", -1))
        if code == 0:
            return
        if code == 4:
            _falsch_merken(z["hash"])
            raise TendaFehler("Router-Passwort ist falsch: bitte in Jarvis neu eingeben.", "zugang")
        if code == 5:
            raise TendaFehler("Am Router sind schon drei Geräte angemeldet, er nimmt gerade keine weitere Anmeldung an.")
        raise TendaFehler(f"Anmeldung am Router gescheitert (Code {code}).")

    def module(self, namen: str) -> dict:
        pfad = f"/goform/getModules?rand={random.random()}&modules={namen}"
        antwort = self._rufe(pfad)
        # errCode kann als Zahl oder Text kommen.
        if str(antwort.get("errCode")) == "1000":
            self.anmelden()
            antwort = self._rufe(pfad)
            if str(antwort.get("errCode")) == "1000":
                raise TendaFehler("Router nimmt die Anmeldung nicht an.")
        return antwort

    def setzen(self, rumpf: dict, pfad: str = "/goform/setModules", sekunden: float = 30) -> int:
        antwort = self._rufe(pfad, rumpf, sekunden)
        if str(antwort.get("errCode")) == "1000":
            self.anmelden()
            antwort = self._rufe(pfad, rumpf, sekunden)
        return int(antwort.get("errCode", -1))


ROUTER_SITZUNG = Router()


def _ohne_geheimes(d) -> dict:
    if not isinstance(d, dict):
        return {}
    return {k: v for k, v in d.items() if not GEHEIM.search(k) and isinstance(v, (str, int, float, bool)) and len(str(v)) <= 80}


def ping_messen(ziel: str = PING_ZIEL, anzahl: int = PING_ANZAHL) -> dict:
    """Latenz, Schwankung (mittlere Abweichung aufeinanderfolgender Pakete) und Verlust über den Heimanschluss."""
    if platform.system() == "Windows":
        befehl = ["ping", "-n", str(anzahl), "-w", "2000", ziel]
    else:
        befehl = ["ping", "-c", str(anzahl), "-i", "0.2", "-W", "2000" if platform.system() == "Darwin" else "2", ziel]
    try:
        aus = subprocess.run(befehl, capture_output=True, text=True, timeout=anzahl * 2 + 10, stdin=subprocess.DEVNULL).stdout
    except Exception as fehler:
        return {"ziel": ziel, "fehler": str(fehler)[:120], "zeit": int(time.time())}
    zeiten = [float(m.replace(",", ".")) for m in re.findall(r"(?:time|Zeit)[=<]\s*([\d.,]+)\s*ms", aus)]
    ergebnis = {"ziel": ziel, "gesendet": anzahl, "empfangen": len(zeiten), "verlust_prozent": round(100 * (anzahl - len(zeiten)) / anzahl, 1),
                "zeit": int(time.time())}
    if zeiten:
        ergebnis["latenz_ms"] = round(statistics.mean(zeiten), 1)
        ergebnis["latenz_min_ms"] = round(min(zeiten), 1)
        ergebnis["latenz_max_ms"] = round(max(zeiten), 1)
        if len(zeiten) >= 2:
            ergebnis["jitter_ms"] = round(statistics.mean(abs(a - b) for a, b in zip(zeiten, zeiten[1:])), 1)
    return ergebnis


def lesen(mit_ping: bool = True, quelle: str = "Mac zu Hause") -> dict:
    """Ein frischer Stand: Funkwerte (simInfo ohne Kennungen), Modus (simWan.dataOptions), Verbindung, Ping."""
    with _SPERRE:
        roh = ROUTER_SITZUNG.module("simInfo,simWan,simStatus,wanCfg,internetStatus")
    sim_wan = roh.get("simWan") or {}
    stand = {
        "zeit": int(time.time()),
        "quelle": quelle,
        "sim_info": _ohne_geheimes(roh.get("simInfo")),
        "sim_wan": {k: sim_wan.get(k) for k in ("dataOptions", "connectType", "internetStatus", "rssi", "bandAggregation", "selectBandResult", "action")
                    if isinstance(sim_wan.get(k), (str, int, float))},
        "sim_status": (roh.get("simStatus") or {}).get("simStatus"),
        "wan": {k: (roh.get("wanCfg") or {}).get(k) for k in ("connectStatus", "connectTime", "wanType") if (roh.get("wanCfg") or {}).get(k) is not None},
        "internet": _ohne_geheimes(roh.get("internetStatus")),
        # Nur die Namen aller simInfo-Felder (ohne Werte), damit Jarvis weiß, was dieses Modell überhaupt liefert.
        "felder": sorted(k for k in (roh.get("simInfo") or {}) if not GEHEIM.search(k)),
    }
    if mit_ping:
        stand["ping"] = ping_messen()
    return stand


def _warten_bis_erreichbar(hoechstens_s: int, erst_weg: bool = False) -> bool:
    """Wartet, bis der Router wieder antwortet (nach Neustart erst, bis er weg war), dann bis das Internet wieder geht."""
    ende = time.time() + hoechstens_s
    if erst_weg:
        while time.time() < ende:
            try:
                ROUTER_SITZUNG._rufe("/goform/getModules?modules=loginLockStatus", sekunden=3)
                time.sleep(3)
            except Exception:
                break
    while time.time() < ende:
        try:
            ROUTER_SITZUNG._rufe("/goform/getModules?modules=loginLockStatus", sekunden=3)
            break
        except Exception:
            time.sleep(4)
    while time.time() < ende:
        if ping_messen(anzahl=2).get("empfangen"):
            return True
        time.sleep(4)
    return False


def aktion(art: str, wert: str = "") -> dict:
    """modus (wert 5/4/2), verbinden, neustart. Ein Schreibvorgang wird nie wiederholt; danach wird der neue Zustand geprüft.
    Ergebnis: {"ergebnis": "fertig" | "unklar" | "fehler", "text": …, "stand": …}."""
    with _SPERRE:
        try:
            if art == "neustart":
                code = ROUTER_SITZUNG.setzen({"reboot": {"action": "reboot"}}, "/goform/setModules", 15)
                if code not in (0, 100):
                    return {"ergebnis": "fehler", "text": f"Der Router hat den Neustart abgelehnt (Code {code})."}
            else:
                sim_wan = ROUTER_SITZUNG.module("simWan").get("simWan")
                if not isinstance(sim_wan, dict) or "list" not in sim_wan:
                    return {"ergebnis": "fehler", "text": "Die Mobilfunk-Einstellungen des Routers ließen sich nicht vollständig lesen; nichts geändert."}
                if art == "modus":
                    if wert not in ("5", "4", "2"):
                        return {"ergebnis": "fehler", "text": "Unbekannter Modus."}
                    sim_wan["dataOptions"] = wert
                    sim_wan["action"] = "1"
                    code = ROUTER_SITZUNG.setzen({"simWan": sim_wan})
                elif art == "verbinden":
                    trennen = dict(sim_wan, action="0")
                    code = ROUTER_SITZUNG.setzen({"simWan": trennen})
                    if code == 0:
                        time.sleep(3)
                        # Getrennt ist er jetzt: Das Verbinden darf nie ausbleiben, sonst bliebe das Heim-Internet weg.
                        # Ein zweites „verbinden“ ist dabei unschädlich (kein erneutes Trennen).
                        try:
                            code = ROUTER_SITZUNG.setzen({"simWan": dict(sim_wan, action="1")})
                        except Exception:
                            code = -1
                        if code != 0:
                            time.sleep(3)
                            code = ROUTER_SITZUNG.setzen({"simWan": dict(sim_wan, action="1")})
                            if code != 0:
                                return {"ergebnis": "fehler", "text": f"Getrennt, aber der Router nimmt das Verbinden nicht an (Code {code}); das Heim-Internet ist gerade aus. Bitte Connect erneut drücken."}
                else:
                    return {"ergebnis": "fehler", "text": "Unbekannte Aktion."}
                if code != 0:
                    return {"ergebnis": "fehler", "text": f"Der Router hat die Änderung abgelehnt (Code {code})."}
        except TendaFehler as f:
            return {"ergebnis": "fehler", "text": str(f)}
        except Exception as f:
            # Antwort verloren (beim Neustart üblich): Ob der Router geschaltet hat, ist offen. Nicht wiederholen, nur nachsehen.
            unklar = f"Keine klare Antwort vom Router ({type(f).__name__}); nicht erneut geschaltet, Zustand danach geprüft."
        else:
            unklar = None
    wieder = _warten_bis_erreichbar(240 if art == "neustart" else 120, erst_weg=art == "neustart")
    # Direkt nach dem Wiederverbinden meldet der Router kurz „unknown“ und der Ping scheitert (Test 20.11.2026): bis zu 90 s
    # nachlesen, bis Technik und Ping stehen. Gelesen wird nur, nie erneut geschaltet.
    stand, ende = None, time.time() + 90
    while True:
        try:
            stand = lesen()
        except Exception as f:
            if time.time() >= ende:
                return {"ergebnis": "unklar", "text": f"Befehl gesendet, der Router ist danach aber noch nicht lesbar ({f}).", "wieder_online": wieder}
            time.sleep(10)
            continue
        technik = str(stand.get("sim_info", {}).get("mobileNet") or "").lower()
        if (technik not in ("", "unknown") and (stand.get("ping") or {}).get("empfangen")) or time.time() >= ende:
            break
        time.sleep(10)
    if unklar:
        return {"ergebnis": "unklar", "text": unklar, "stand": stand, "wieder_online": wieder}
    return {"ergebnis": "fertig", "text": "Befehl ausgeführt, neuer Zustand gelesen." if wieder else "Befehl ausgeführt, das Internet ist aber noch nicht wieder da.",
            "stand": stand, "wieder_online": wieder}


def md5(text: str) -> str:
    return hashlib.md5(text.encode("utf-8")).hexdigest()


if __name__ == "__main__":
    # Probelauf: nur lesen, Werte ohne Kennungen ausgeben.
    print(json.dumps(lesen(), ensure_ascii=False, indent=1))
