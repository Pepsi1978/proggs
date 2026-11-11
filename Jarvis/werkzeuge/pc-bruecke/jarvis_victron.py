"""Liest Franks Victron-Geräte (SmartSolar MPPT, BMV-712 Smart) per Bluetooth „Instant Readout“ und meldet den Stand an den Relay.

Die Geräte senden ihre Messwerte verschlüsselt (AES-CTR) in ihren BLE-Werbepaketen (Hersteller 0x02E1). Die Schlüssel stehen in
~/SK/Victron/victron.json (aus VictronConnect → Produktinfo → Instant Readout). Jarvis auf Handy und Tablet liest dieselben
Pakete selbst; unterwegs fragt es den Relay (GET /geraet/victron), den dieses Skript füllt (POST /pc/victron).

Aufruf:  python jarvis_victron.py          einmal scannen (bis 12 s) und melden (so startet es die PC-Brücke jede Minute)
         python jarvis_victron.py zeigen   nur scannen und ausgeben, nichts melden
"""

import asyncio
import json
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from bleak import BleakScanner
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

VICTRON = 0x02E1
SK = Path.home() / "SK"
LADEZUSTAND = {0: "aus", 1: "Schwachlast", 2: "Fehler", 3: "Bulk", 4: "Absorption", 5: "Erhaltung", 6: "Speicher", 7: "Ausgleich",
               9: "Wechselrichter", 11: "Netzteil", 245: "Start", 247: "Ausgleich", 252: "extern gesteuert"}


PROTOKOLL = Path(os.environ.get("LOCALAPPDATA", tempfile.gettempdir())) / "JarvisPcBruecke" / "victron.log"


def protokoll(text: str) -> None:
    """Fehler landen hier statt im Nichts (die Brücke startet das Skript unsichtbar). Hält höchstens etwa 200 KB."""
    try:
        PROTOKOLL.parent.mkdir(parents=True, exist_ok=True)
        if PROTOKOLL.exists() and PROTOKOLL.stat().st_size > 200_000:
            PROTOKOLL.write_text("", "utf-8")
        with PROTOKOLL.open("a", encoding="utf-8") as d:
            d.write(time.strftime("%Y-%m-%d %H:%M:%S ") + text + "\n")
    except Exception:
        pass


def lade_geraete() -> dict:
    daten = json.loads((SK / "Victron" / "victron.json").read_text("utf-8"))
    geraete = {}
    for g in daten["geraete"]:
        try:
            schluessel = bytes.fromhex(g["schluessel"])
            if len(schluessel) != 16:
                raise ValueError("Schlüssel nicht 16 Byte")
            geraete[g["mac"].replace(":", "").lower()] = {**g, "schluessel_roh": schluessel}
        except Exception as fehler:
            protokoll(f"Eintrag {g.get('name', '?')} unbrauchbar: {fehler}")
    return geraete


def entschluesseln(daten: bytes, schluessel: bytes) -> bytes | None:
    # Aufbau: 0x10, ?, Modell (2), Art (1), Zähler (2, little endian), erstes Schlüsselbyte, verschlüsselte Werte.
    if len(daten) < 9 or daten[0] != 0x10 or daten[7] != schluessel[0]:
        return None
    zaehler = daten[5] | (daten[6] << 8)
    roh = daten[8:]
    # Der Zähler zählt little endian hoch (pycryptodome Counter little_endian=True); bei bis zu 16 Byte reicht der erste Block.
    aes = Cipher(algorithms.AES(schluessel), modes.ECB()).encryptor()
    strom = b""
    for i in range((len(roh) + 15) // 16):
        strom += aes.update((zaehler + i).to_bytes(16, "little"))
    return bytes(a ^ b for a, b in zip(roh, strom))


def auswerten(art: int, d: bytes) -> dict | None:
    if art == 0x01 and len(d) >= 10:  # Laderegler
        spannung = int.from_bytes(d[2:4], "little", signed=True)
        strom = int.from_bytes(d[4:6], "little", signed=True)
        ertrag = int.from_bytes(d[6:8], "little")
        leistung = int.from_bytes(d[8:10], "little")
        return {"art": "laderegler", "zustand": LADEZUSTAND.get(d[0], f"Zustand {d[0]}"), "fehler": d[1],
                "spannung_v": None if spannung == 0x7FFF else spannung / 100,
                "ladestrom_a": None if strom == 0x7FFF else strom / 10,
                "ertrag_heute_kwh": None if ertrag == 0xFFFF else ertrag / 100,
                "solarleistung_w": None if leistung == 0xFFFF else leistung}
    if art == 0x02 and len(d) >= 15:  # Batteriemonitor
        rest = int.from_bytes(d[0:2], "little")
        spannung = int.from_bytes(d[2:4], "little", signed=True)
        strom = int.from_bytes(d[8:11], "little", signed=True) >> 2
        verbraucht = int.from_bytes(d[11:14], "little") & 0xFFFFF
        soc = (int.from_bytes(d[13:15], "little") >> 4) & 0x3FF
        return {"art": "batteriemonitor",
                "ladezustand_prozent": None if soc == 0x3FF else soc / 10,
                "spannung_v": None if spannung == 0x7FFF else spannung / 100,
                # Ungültig ist wie bei 0x7FFF der größte positive Wert des 22-Bit-Felds.
                "strom_a": None if strom == 0x1FFFFF else round(strom / 1000, 3),
                "verbraucht_ah": None if verbraucht == 0xFFFFF else -verbraucht / 10,
                "restlaufzeit_min": None if rest == 0xFFFF else rest}
    return None


async def scannen(geraete: dict, sekunden: float = 12.0) -> dict:
    gefunden: dict = {}
    fertig = asyncio.Event()

    def paket(geraet, werbung):
        roh = werbung.manufacturer_data.get(VICTRON)
        mac = geraet.address.replace(":", "").lower()
        g = geraete.get(mac)
        if roh is None or g is None:
            return
        try:
            klar = entschluesseln(bytes(roh), g["schluessel_roh"])
            werte = auswerten(roh[4], klar) if klar else None
        except Exception as fehler:
            protokoll(f"Paket von {g['name']} unlesbar: {fehler}")
            return
        if werte:
            werte["name"] = g["name"]
            werte["zeit"] = int(time.time())
            gefunden[mac] = werte
            if len(gefunden) == len(geraete):
                fertig.set()

    async with BleakScanner(detection_callback=paket):
        try:
            await asyncio.wait_for(fertig.wait(), sekunden)
        except asyncio.TimeoutError:
            pass
    return gefunden


def lies_zugang() -> tuple[str, str]:
    werte = {}
    for zeile in (SK / "Jarvis" / "relay.properties").read_text("utf-8").splitlines():
        if "=" in zeile and not zeile.lstrip().startswith(("#", "!")):
            k, v = zeile.split("=", 1)
            werte[k.strip()] = v.strip()
    return werte["host"], werte["token"]


def melden(stand: dict) -> None:
    host, token = lies_zugang()
    # curl.exe -4 wie die Brücke (IPv6 hängt auf diesem Netz zeitweise); der Token geht über eine Datei, nicht die Befehlszeile.
    with tempfile.TemporaryDirectory() as ordner:
        kopf = Path(ordner) / "kopf.txt"
        # write_bytes: write_text machte unter Windows aus \n ein \r\n, das \r im Kopf lehnt der Relay mit 400 ab.
        kopf.write_bytes(f"X-Jarvis-Token: {token}\nContent-Type: application/json; charset=utf-8\n".encode())
        rumpf = Path(ordner) / "rumpf.json"
        rumpf.write_text(json.dumps(stand, ensure_ascii=False), "utf-8")
        subprocess.run(["curl.exe", "-4", "-s", "-S", "--fail", "--max-time", "15", "-o", "NUL", "-H", f"@{kopf}", "-X", "POST",
                        "--data-binary", f"@{rumpf}", f"https://{host}/pc/victron"], check=True, creationflags=0x08000000)


def main() -> None:
    geraete = lade_geraete()
    try:
        gefunden = asyncio.run(scannen(geraete))
    except Exception as fehler:
        protokoll(f"Bluetooth-Scan gescheitert: {fehler!r}")
        raise
    stand = {"geraete": list(gefunden.values()), "quelle": "PC", "zeit": int(time.time())}
    if len(sys.argv) > 1 and sys.argv[1] == "zeigen":
        print(json.dumps(stand, ensure_ascii=False, indent=2))
        return
    if gefunden:
        try:
            melden(stand)
        except Exception as fehler:
            protokoll(f"Meldung an den Relay gescheitert: {fehler!r}")
            raise


if __name__ == "__main__":
    main()
