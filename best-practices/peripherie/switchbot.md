# SwitchBot-Sensoren auslesen (Cloud-API v1.1 und Bluetooth)

> Stand: 15.11.2026 19:22. Versionen: OpenAPI v1.1, Meter Pro CO2 FW V1.9, Hub Mini V6.8, bleak (macOS 27). Bugs: `bugs/peripherie/switchbot.md`.

## Kurzcheck
- Cloud zuerst (Token+Secret, HMAC-SHA256 über token+t+nonce, Base64, Großbuchstaben). deviceId = BLE-MAC ohne Doppelpunkte.
- Werte ohne Hub-Weg kommen als Nullen: erkennen und auf Bluetooth-Rückfall gehen.
- Bluetooth passiv, ohne Pairing: Herstellerdaten Company-ID 0x0969 (Woan). Meter Pro CO2: 16 Byte Nutzdaten.
- Zuordnung über die MAC in Byte 0–5 der Herstellerdaten (funktioniert auch unter macOS, das keine MAC-Adressen nennt).

## Volltext
Byteformat Meter Pro CO2 (offiziell extern: pySwitchbot `switchbot/adv_parsers/meter.py`, https://github.com/sblibs/pySwitchbot):
- 0–5 MAC · 8 Bits 3–0 Zehntel °C · 9 Bit 7 Vorzeichen (1 = positiv), Bits 6–0 ganze °C · 10 Bits 6–0 Feuchte %
- 13–14 CO2 ppm Big Endian, gültig 400–9999 (größer = Artefakt)
- Testvektor: `b0e9fe543215b7e4079ba4003702d500` → 27,7 °C, 36 %, 725 ppm. Selbst gemessen: `…0d6401962b0024029600` → 22,1 °C, 43 %, 662 ppm.
- Gerätetyp nur in Service Data 0xFD3D Byte 0 (0x35/0x15 = Meter Pro CO2), Batterie Byte 2 (nur mit Scan-Response).
- Android: BluetoothLeScanner mit Filter setManufacturerData(0x0969, …), nur BLUETOOTH_SCAN (neverForLocation) nötig.

Muster in Jarvis: Mac-Brücke scannt alle 5 Minuten (Monitor misst höchstens alle 5 Minuten), POST /pc/switchbot an den Relay,
Jarvis nimmt GET /geraet/switchbot als Rückfall, wenn die Cloud nichts liefert, höchstens 20 Minuten alt, mit Messzeit.
Doku: https://raw.githubusercontent.com/OpenWonderLabs/SwitchBotAPI/main/devices/sensors/meter-pro-co2.md
