"""Liest Franks SwitchBot-Sensoren (Thermometer, Außensensoren, Meter Pro CO2) per Bluetooth (passiv, ohne Pairing) und meldet den Stand an den Relay.

Warum: Der Hub Mini reicht den CO2-Monitor nicht in die SwitchBot-Cloud weiter (die Cloud liefert nur Nullen, 15.11.2026).
Der Mac zu Hause hört die Werbepakete mit; Jarvis fragt unterwegs den Relay (GET /geraet/switchbot), den dieses Skript füllt
(POST /pc/switchbot). Jarvis ordnet die Werte über die MAC zu, die zugleich die deviceId in der SwitchBot-Cloud ist.

Rückfall für alle Sensoren, wenn die Cloud ausfällt oder alte Werte liefert (Frank, 15.11.2026). Gemeldet wird jeder gehörte
SwitchBot-Sensor; Jarvis nimmt nur die, deren MAC eine deviceId in Franks Konto ist (fremde Geräte in Reichweite fallen dort weg).

Format (pySwitchbot adv_parsers/meter.py, Hersteller 0x0969, Nutzdaten nach der Company-ID; mindestens 11 Byte, 16 beim Meter Pro CO2):
  0–5 MAC · 8 Bits 3–0 Zehntel °C · 9 Bit 7 Vorzeichen (1 = positiv), Bits 6–0 ganze °C · 10 Bits 6–0 Luftfeuchte %
  nur Meter Pro CO2: 13–14 CO2 ppm (Big Endian, gültig 400–9999).

Aufruf:  python jarvis_switchbot.py          einmal scannen (bis 30 s) und melden; Exit 3 = nichts empfangen
         (die Brücke startet es alle fünf Minuten, nach einem Fehlschlag in der nächsten Minute erneut;
         der Monitor misst ohnehin höchstens alle fünf Minuten neu)
         python jarvis_switchbot.py schnell  Aktualisieren-Knopf am Handy: bis 30 s, endet 1 s nach dem CO2-Monitor (sendet am Mac nur alle
                                             10–25 s), frühestens nach 8 s (für die Thermometer); meldet auch „nichts gehört“ (sofort = true)
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
    if len(roh) < 11:
        return None
    zehntel = roh[8] & 0x0F
    ganz = roh[9] & 0x7F
    temperatur = round((ganz + zehntel / 10) * (1 if roh[9] & 0x80 else -1), 1)
    feuchte = roh[10] & 0x7F
    if not 0 < feuchte <= 100 or not -40 <= temperatur <= 70:
        return None
    mac = roh[0:6].hex()
    # „art“ mit Vorsilbe: Der Relay führt je art den jüngsten Wert; so verdrängt der Mac nie die Webhook-Werte der Cloud (art = MAC).
    werte = {"art": f"mac-{mac}", "mac": mac, "temperatur_c": temperatur, "luftfeuchtigkeit_prozent": feuchte, "zeit": int(time.time())}
    # CO2 ab 15 Byte (pySwitchbot); ob das Gerät ein CO2-Monitor ist, entscheidet Jarvis über den Typ aus der Cloud.
    if len(roh) >= 15:
        co2 = int.from_bytes(roh[13:15], "big")
        if 400 <= co2 <= 9999:
            werte["co2_ppm"] = co2
    return werte


async def scannen(sekunden: float = 30.0, nachlauf: float = 5.0, mindestens: float = 0.0) -> dict:
    """Thermometer senden alle paar Sekunden, der Meter Pro CO2 nur alle paar Sekunden bis eine halbe Minute: lauschen, bis der CO2-Monitor
    gehört ist (dann noch [nachlauf] s für die übrigen, insgesamt mindestens [mindestens] s), höchstens [sekunden]."""
    beginn = time.monotonic()
    gefunden: dict = {}
    fertig = asyncio.Event()

    def paket(_geraet, werbung):
        roh = werbung.manufacturer_data.get(WOAN)
        if roh is None:
            return
        werte = auswerten(bytes(roh))
        if werte:
            gefunden[werte["mac"]] = werte
            if "co2_ppm" in werte:
                fertig.set()

    async with BleakScanner(detection_callback=paket):
        try:
            await asyncio.wait_for(fertig.wait(), sekunden)
            await asyncio.sleep(max(nachlauf, mindestens - (time.monotonic() - beginn)))
        except asyncio.TimeoutError:
            pass
    return gefunden


def main() -> None:
    schnell = len(sys.argv) > 1 and sys.argv[1] == "schnell"
    try:
        gefunden = asyncio.run(scannen(30.0, 1.0, 8.0) if schnell else scannen())
    except Exception as fehler:
        protokoll_in("switchbot.log", f"Bluetooth-Scan gescheitert: {fehler!r}")
        if schnell:  # die Frage des Handys nicht bis zur Frist warten lassen
            try:
                melden_an("/pc/switchbot", {"geraete": [], "zeit": int(time.time()), "sofort": True})
            except Exception:
                pass
        raise
    stand = {"geraete": list(gefunden.values()), "zeit": int(time.time())}
    if schnell:
        stand["sofort"] = True   # der Relay beantwortet damit die wartende Frage des Handys, auch wenn nichts gehört wurde
    if len(sys.argv) > 1 and sys.argv[1] == "zeigen":
        print(json.dumps(stand, ensure_ascii=False, indent=2))
        return
    if not gefunden and not schnell:
        sys.exit(3)
    try:
        melden_an("/pc/switchbot", stand)
    except Exception as fehler:
        protokoll_in("switchbot.log", f"Meldung an den Relay gescheitert: {fehler!r}")
        raise


if __name__ == "__main__":
    main()
