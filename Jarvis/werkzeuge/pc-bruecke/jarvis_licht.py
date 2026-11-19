"""Schaltet Franks LED-Streifen (App „KS Lite“, Gerätenamen „KS03~…“) per Bluetooth direkt, ohne die App.

Das Handy schickt über den Relay (POST /geraet/licht → GET /pc/mac-befehl) die Befehle je Namensvorsatz, etwa
{"KS03~": ["5BF000B5", "5A0001FF0000006400A5"]}. Dieses Skript sucht bis 6 s nach Streifen, verbindet sich mit allen
gleichzeitig, schreibt die Befehle nacheinander und gibt {"geschaltet": [Namen], "gesehen": [Namen]} als JSON auf stdout aus.
Protokoll aus der KS-Lite-App 1.8 (unverschlüsselt); dieselbe Tabelle steht in LichtFaehigkeit.kt auf dem Handy.

Aufruf:  python jarvis_licht.py '<befehle als JSON>'      schalten
         python jarvis_licht.py an | aus                   zum Ausprobieren am Rechner
Plattformneutral (bleak): läuft am Mac (Brücke) und unter Windows.
"""

import asyncio
import json
import sys

from bleak import BleakClient, BleakScanner

# Vorsatz → (Dienst, Schreibkanal); der erste passende Vorsatz gilt.
TYPEN = [
    ("KS01-", "ae00", "ae01"), ("KS02-", "ae00", "ae01"), ("KS03-", "fff0", "fff3"), ("KS03~", "afd0", "afd1"),
    ("KS04-", "fff0", "fff3"), ("KS15~", "afd0", "afd1"), ("KS", "ae00", "ae01"),
]
SUCHE_S = 8.0
ERWARTET = 6  # Franks Streifen (Stand 19.11.2026): Wohnzimmer, Wohnzimmer 2, Wohnzimmer 3, Flur, Küche, Bad


def uuid(kurz: str) -> str:
    return f"0000{kurz}-0000-1000-8000-00805f9b34fb"


def typ(name: str):
    return next((t for t in TYPEN if name.startswith(t[0])), TYPEN[-1])


def befehle_fuer(name: str, befehle: dict) -> list:
    t = typ(name)[0]
    return befehle.get(t) or befehle.get("KS") or []


async def schalten_eins(geraet, name: str, befehle: dict) -> bool:
    liste = befehle_fuer(name, befehle)
    if not liste:
        return False
    _, dienst, kanal = typ(name)
    for versuch in range(2):
        try:
            async with BleakClient(geraet, timeout=6.0) as client:
                k = client.services.get_characteristic(uuid(kanal))
                if k is None:  # anderer Typ: ersten bekannten Kanal nehmen
                    k = next((client.services.get_characteristic(uuid(c)) for _, _, c in TYPEN
                              if client.services.get_characteristic(uuid(c)) is not None), None)
                if k is None:
                    return False
                mit_antwort = "write" in k.properties
                for hexwert in liste:
                    await client.write_gatt_char(k, bytes.fromhex(hexwert), response=mit_antwort)
                    await asyncio.sleep(0.05)
                await asyncio.sleep(0.15)
                return True
        except Exception as fehler:
            print(f"{name}: {fehler}", file=sys.stderr)
            await asyncio.sleep(0.4)
    return False


async def schalten(befehle: dict) -> dict:
    gefunden = {}

    def gesehen(geraet, werbung):
        name = werbung.local_name or geraet.name or ""
        if name.startswith("KS"):
            gefunden.setdefault(name, geraet)

    async with BleakScanner(gesehen):
        # Früher fertig nur, wenn alle erwarteten Streifen da sind; manche werben selten und kommen erst nach Sekunden.
        ende = asyncio.get_event_loop().time() + SUCHE_S
        while asyncio.get_event_loop().time() < ende and len(gefunden) < ERWARTET:
            await asyncio.sleep(0.2)
    namen = sorted(gefunden)
    ergebnisse = await asyncio.gather(*(schalten_eins(gefunden[n], n, befehle) for n in namen))
    return {"geschaltet": [n for n, ok in zip(namen, ergebnisse) if ok], "gesehen": namen}


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    arg = sys.argv[1]
    if arg in ("an", "aus"):
        befehle = {"KS": ["5BF000B5" if arg == "an" else "5B0F00B5"]}
    else:
        befehle = json.loads(arg)
    print(json.dumps(asyncio.run(schalten(befehle)), ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
