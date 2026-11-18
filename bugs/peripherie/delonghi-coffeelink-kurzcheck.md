# De'Longhi Coffee Link Kurzcheck

> Nur Stufe A. Bei Fehlern den Volltext `delonghi-coffeelink.md` lesen. Stand 18.11.2026.

| # | Signal | Sofort-Regel | Volltext |
|---|--------|--------------|----------|
| 1 | Befehl OK (200/201), aber kein Kaffee | Coffee-Link-App schließen, Online-Status prüfen, LAN-Modus | §1 |
| 2 | `Characteristic …0301 not found` | Soul ist WLAN-Maschine, kein BLE | §2 |
| 3 | Login ok, 0 Geräte | Ayla-Region EU/US prüfen | §3 |
| 4 | Ein-Befehl Timeout | Nicht auf 0x84-Antwort warten, Status 0x75 pollen | §4 |
| 5 | Status flackert/Tank falsch | cremalink-Statusbits nicht trauen | §5 |
| 6 | HTTP 429 | Polling drosseln (≤ 180/h) | §6 |
| 7 | LAN-Modus geht nicht | Maschine muss nach Neustart erst die Cloud erreichen | §7 |
| 8 | Rezept bricht nach App-Update | Rezeptfelder neu prüfen (App 4.9.6) | §8 |
| 9 | Halber Kaffee, Tank knapp | Gerät prüft nur Schwelle; in Jarvis Verbrauch mitzählen und vorher warnen | §9 |
| 10 | Spülwasser in der Tasse beim Fernstart | Spülen nicht abschaltbar; wach halten, auf „bereit“ warten | §10 |
