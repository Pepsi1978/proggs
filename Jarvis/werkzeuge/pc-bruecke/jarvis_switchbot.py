"""Liest Franks SwitchBot Meter Pro CO2 per Bluetooth (passiv, ohne Pairing) und meldet den Stand an den Relay.

Warum: Der Hub Mini reicht den CO2-Monitor nicht in die SwitchBot-Cloud weiter (die Cloud liefert nur Nullen, 15.11.2026).
Der Mac zu Hause hört die Werbepakete mit; Jarvis fragt unterwegs den Relay (GET /geraet/switchbot), den dieses Skript füllt
(POST /pc/switchbot). Jarvis ordnet die Werte über die MAC zu, die zugleich die deviceId in der SwitchBot-Cloud ist.

Format (pySwitchbot adv_parsers/meter.py, Hersteller 0x0969, Nutzdaten nach der Company-ID, 16 Byte beim Meter Pro CO2):
  0–5 MAC · 8 Bits 3–0 Zehntel °C · 9 Bit 7 Vorzeichen (1 = positiv), Bits 6–0 ganze °C · 10 Bits 6–0 Luftfeuchte %
  13–14 CO2 ppm (Big Endian, gültig 400–9999).

Aufruf:  python jarvis_switchbot.py          einmal scannen (bis 40 s) und melden; Exit 3 = nichts empfangen
         (die Brücke startet es alle fünf Minuten, nach einem Fehlschlag in der nächsten Minute erneut;
         der Monitor misst ohnehin höchstens alle fünf Minuten neu)
         python jarvis_switchbot.py zeigen   nur scannen und ausgeben, nichts melden
"""

import asyncio
import json
import sys
import time

from bleak import BleakScanner

from jarvis_victron import melden_an, protokoll_in

WOAN = 0x0969


def auswerten(roh: bytes) -> dict | None:
    if len(roh) != 16:
        return None
    zehntel = roh[8] & 0x0F
    ganz = roh[9] & 0x7F
    temperatur = round((ganz + zehntel / 10) * (1 if roh[9] & 0x80 else -1), 1)
    feuchte = roh[10] & 0x7F
    co2 = int.from_bytes(roh[13:15], "big")
    if not 400 <= co2 <= 9999 or not 0 < feuchte <= 100 or not -30 <= temperatur <= 60:
        return None
    mac = roh[0:6].hex()
    return {"art": mac, "mac": mac, "temperatur_c": temperatur, "luftfeuchtigkeit_prozent": feuchte, "co2_ppm": co2,
            "zeit": int(time.time())}


async def scannen(sekunden: float = 40.0) -> dict:
    """Der Meter Pro CO2 sendet nur alle paar Sekunden bis eine halbe Minute ein Messpaket: lange lauschen, beim ersten Treffer aufhören."""
    gefunden: dict = {}
    fertig = asyncio.Event()

    def paket(_geraet, werbung):
        roh = werbung.manufacturer_data.get(WOAN)
        if roh is None:
            return
        werte = auswerten(bytes(roh))
        if werte:
            gefunden[werte["mac"]] = werte
            fertig.set()

    async with BleakScanner(detection_callback=paket):
        try:
            await asyncio.wait_for(fertig.wait(), sekunden)
        except asyncio.TimeoutError:
            pass
    return gefunden


def main() -> None:
    try:
        gefunden = asyncio.run(scannen())
    except Exception as fehler:
        protokoll_in("switchbot.log", f"Bluetooth-Scan gescheitert: {fehler!r}")
        raise
    stand = {"geraete": list(gefunden.values()), "zeit": int(time.time())}
    if len(sys.argv) > 1 and sys.argv[1] == "zeigen":
        print(json.dumps(stand, ensure_ascii=False, indent=2))
        return
    if not gefunden:
        sys.exit(3)
    try:
        melden_an("/pc/switchbot", stand)
    except Exception as fehler:
        protokoll_in("switchbot.log", f"Meldung an den Relay gescheitert: {fehler!r}")
        raise


if __name__ == "__main__":
    main()
