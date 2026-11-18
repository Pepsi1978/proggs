# De'Longhi Coffee Link — Bug-Almanach

> Stand: 18.11.2026 09:07 · Versions-Anker: ECAM 610.74/610.75 (PrimaDonna Soul, Ayla ADA 1.5.3), Coffee-Link-App 4.9.6, actabi/delonghi_coffeelink v0.3.26, sk7n4k3d/delonghi-ha v1.9.3, cremalink 0.1.0b28, cremaevolution-ha 0.3.9b1, Arbuzov 1.17.26.
> Kurzcheck: `delonghi-coffeelink-kurzcheck.md` · Best Practices: `best-practices/peripherie/delonghi-coffeelink.md`.

## §1 Befehl wird angenommen (200/201), Maschine tut nichts
- **Symptom:** Cloud antwortet OK, kein Kaffee, Monitor eingefroren.
- **Ursache:** (a) Coffee-Link-App hält eine LAN-Sitzung (30-s-Keep-alive), die hat Vorrang; (b) Maschine offline, Cloud antwortet trotzdem 200; (c) falsche Sitzungs-ID/Gerätesignatur; (d) Soul ignoriert Cloud-Brühbefehle ohne App-Sitzung (delonghi-ha #50, v1.6.0–1.8.0).
- **Fix:** App schließen; `connection_status` und Monitor-Zeitstempel prüfen; LAN-Modus nutzen (delonghi-ha ≥ 1.9.0, Fix unbestätigt).
- Quellen: https://github.com/sk7n4k3d/delonghi-ha/issues/50 · https://github.com/actabi/delonghi_coffeelink · https://github.com/miditkl/cremalink/issues/224

## §2 BLE: `Characteristic 00035b03-…-0301 not found`
- **Ursache:** Soul (610.74/610.75) ist eine WLAN-Maschine, BLE-Integrationen (Arbuzov, longshot) passen nicht.
- **Fix:** Ayla-Cloud/LAN statt BLE. Quellen: https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/56 · /issues/25

## §3 Login klappt, „0 Geräte“
- **Ursache:** falsche Ayla-Region (EU/US). **Fix:** EU-Endpunkte (`user-field-eu`, `ads-eu`). Quelle: actabi const.py, Issue #60.

## §4 Ein/Aus-Befehl ohne Wirkung bzw. Timeout
- 0x84 wird nie mit 0x84 beantwortet → nicht darauf warten, Status 0x75 pollen (Arbuzov DEBUG_NOTES, 17.10.2026). Soul: `data_request` erst ohne, dann mit app_id senden (delonghi-ha #34, v1.5.11). Im Standby beantwortet die Maschine 0xA4/0xE2 nicht (Arbuzov #288).

## §5 Falsche Statuswerte
- cremalink 0.1.0b27/28 (ECAM610.75): `is_busy/is_idle` flackern, `is_watertank_empty` wertlos (Issue #212, #249, PR #250 offen). Fix: eigene Auswertung nach longshot `monitor.rs`.
- actabi #74: Zähler der Soul seit 28.09.2026 in der Cloud eingefroren.

## §6 HTTP 429 / Timeouts
- Ayla-Budget ca. 180–200 Requests/h überschritten. **Fix:** Polling drosseln, Keep-alive ~4 min, Token-Refresh statt Neu-Login. Quellen: https://github.com/Styx85/aioecam · delonghi-ha api.py

## §7 LAN-Modus verweigert / erzeugt Cloud-Last
- Nach Neustart des WLAN-Moduls ohne Internet verweigert die Maschine den LAN-Modus, bis sie die Cloud erreicht hat (cremaevolution-ha README).
- LAN-Polling öffnet je Poll eine Cloud-TLS-Verbindung (cremaevolution-ha #36, 16.11.2026) → sparsam pollen.
- LAN-Registrierung kann die App-Verbindung trennen (ioBroker.coffeelink).
- Franks 610.74: im Standby kein offener Port (80/10275/10280) — ungeklärt, nach Test mit wacher Maschine ergänzen.

## §7a Gerät „Offline“ seit Monaten, IP scheinbar erreichbar
- **Symptom:** Ayla meldet `connection_status: Offline`, alle Datenpunkte alt; eine vermutete IP antwortet auf Ping, aber kein Port offen.
- **Ursache (Frank, 18.11.2026):** Die vermutete IP war ein anderes Gerät; die Maschine hing gar nicht mehr im WLAN (seit Router-Wechsel). Prüfen: `mac` aus `GET /apiv1/dsns/<DSN>.json` mit der ARP-Tabelle vergleichen.
- **Fix:** Maschine in der Coffee-Link-App neu mit dem WLAN (2,4 GHz) verbinden.

## §8 Rezeptfelder geändert
- Coffee-Link-App 4.9.6 änderte Rezeptfelder; cremaevolution 0.3.0b9–b11 musste `brew_custom` anpassen (Breaking). Nach App-Updates Rezept-Frames prüfen.
