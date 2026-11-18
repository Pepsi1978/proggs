# De'Longhi Coffee Link Kurzcheck

> Nur Stufe A. Stand 18.11.2026. Volltext: `delonghi-coffeelink.md`.

| # | Situation | Sofort-Regel | Volltext |
|---|-----------|--------------|----------|
| 1 | Maschine wählen/ansprechen | Modellkatalog: Soul ECAM 610/612 = WLAN (Ayla), kein BLE | §1 |
| 2 | Frame bauen | 0x0D, Länge, Befehl, Daten, CRC16 (0x1D0F/0x1021); 0x83 Brühen, 0x84 Ein, 0x75 Status | §1 |
| 3 | Long Black | Getränke-ID 0x19 (25); fehlt in cremalink/aioecam, selbst bauen | §4 |
| 4 | Weg wählen | LAN-Modus (Schlüssel einmal aus der Cloud) vor Cloud-Polling | §3 |
| 5 | Vor dem Brühen | 0x84 senden, auf Status Ready (7) warten, Tasse steht? | §5 |
| 6 | Mehrere Clients | Ein Dienst hält die Verbindung, alle anderen über dessen API | §6 |
