# De'Longhi Coffee Link (PrimaDonna Soul u. a.) per Programm steuern

> Stand: 18.11.2026 09:07 · Versions-Anker: Maschine ECAM 610.74 (PrimaDonna Soul, WLAN-Modul Ayla AY008ESP1 „DL-millcore“, Agent ADA 1.5.3), Coffee-Link-App 4.9.x, Projekte: actabi/delonghi_coffeelink v0.3.26, sk7n4k3d/delonghi-ha v1.9.3, miditkl/cremalink 0.1.0b28, Styx85/aioecam 0.1.7b1, cremaevolution-ha 0.3.9b1, FiveElements/delonghi-coffeelink-local 0.1.5, longshot 0.1.13.
> Kurzcheck: `delonghi-coffeelink-kurzcheck.md` · Bugs: `bugs/peripherie/delonghi-coffeelink.md` · Rohberichte: `recherchen/delonghi-coffeelink/r1…r6`.
> Franks Maschine: 192.168.0.110, MAC 98:17:3c:75:bf:16 (Ayla-Modul, kein offener Port im Standby, Stand 18.11.2026).

## 1. Welcher Weg für welche Maschine
- **Modellkatalog zuerst:** In der App-Datei `MachinesModels.json` steht je Modell `connectionType` (BT / Wi-Fi), `protocolVersion`. PrimaDonna Soul ECAM 610/612 = **Wi-Fi**; Elite 650/656, Dinamica Plus 370 = BT. `extern` https://github.com/Arbuzov/home_assistant_delonghi_primadonna
- **Soul: kein Bluetooth-Steuerweg.** Die BLE-Characteristic fehlt (`BleakError … 0301 not found`, Arbuzov Issue #56/#25). Steuerung nur über Ayla (Cloud oder LAN-Modus).
- Das **Frame-Format ist auf allen Wegen gleich** (BLE, Cloud, LAN): `0x0D` (App→Maschine) / `0xD0` (Maschine→App), Länge (Gesamt−1), Befehl, Nutzdaten, CRC16 AUG-CCITT (Init 0x1D0F, Poly 0x1021, Big Endian). Befehle: 0x83 Brühen, 0x84 Ein/Standby (`0d 07 84 0f 02 01 55 12`), 0x75 Status/Monitor. `extern` longshot `hardware_enums.rs` https://docs.rs/crate/longshot/0.1.13

## 2. Cloud-Weg (Ayla EU)
1. Login über SAP Gigya (OIDC) → `POST https://user-field-eu.aylanetworks.com/api/v1/token_sign_in` → Ayla `auth_token` + `refresh_token` (24 h, refreshen). **Erprobt 18.11.2026:** Der OIDC-Browserweg (authorize mit client_id 1S8q1WJEs-emOB43Z0-66WnL, redirect_uri https://google.it) funktioniert auch für Konten mit Google-Login, wenn man per „Passwort vergessen“ ein Passwort setzt (die OIDC-Seite hat keinen Google-Knopf). Tauschen: das OIDC-**access_token** mit app_id `DeLonghiCoffeeLink2-DQ-id` geht, das id_token liefert 422 „Unable to login“; ohne Header `Accept: application/json` antwortet Ayla 406. google.it leitet auf google.com weiter und verliert den Code: Code aus dem Chrome-Verlauf (History-DB) lesen.
2. Geräte: `GET https://ads-eu.aylanetworks.com/apiv1/devices.json` → DSN.
3. Befehl: `POST /apiv1/dsns/<DSN>/properties/data_request/datapoints.json`, Wert = base64 des Binär-Frames (+ Zeitstempel/Sitzungsbytes). Soul = `data_request`; Eletta Explore = `app_data_request` + Heartbeat `app_device_connected`.
4. Region muss zum Konto passen (EU/US), sonst „0 Geräte“.
5. Budget ca. 180–200 Requests/Stunde, Latenz 2–5 s. Vor Befehlen `connection_status` prüfen (Cloud sagt 200 auch bei offline Maschine).
`extern` https://github.com/actabi/delonghi_coffeelink · https://github.com/sk7n4k3d/delonghi-ha · https://github.com/Styx85/aioecam

## 3. LAN-Modus (Ayla „LAN mode“) — bevorzugt
- Cloud nur einmal für den LAN-Schlüssel (`lan.json` des Geräts), danach lokal.
- Ablauf mit **vertauschten Rollen**: Client sendet `POST http://<maschine>/local_reg.json` mit eigener IP und Port (10275/10280) → Maschine verbindet sich **zum Client** zurück, AES-Schlüsseltausch, Keep-alive per PUT ca. alle 30 s, Kommandos holt die Maschine per Polling. Neue Sitzung nur per POST, PUT nur als Keep-alive. Firewall muss Maschine → Client erlauben.
- Laut Quellen lauscht die Maschine dafür auf Port 80; bei Franks Maschine war Port 80 im Standby zu (offen, nach Test eintragen).
`extern` https://github.com/FiveElements/delonghi-coffeelink-local · https://github.com/Styx85/cremaevolution-ha/blob/main/docs/ecam-lan-protocol.md · https://github.com/ioBroker/AdapterRequests/issues/664 · `offiziell` https://docs.aylanetworks.com/docs/ayla-embedded-agent

## 4. Getränke und Rezepte
- IDs: Kaffee 2, Americano 6, Heißwasser 16, Cortado 0x18, **Long Black 25 (0x19)**, Mug to Go 0x1A, Brew over Ice 0x1B; App-Eigenkreationen 230–239. Rezeptparameter als (ID, Wert)-Paare (Kaffeemenge, Wasser, Taste, „Inversion“ = Reihenfolge).
- ECAM 610.74: 21 Getränke inkl. Long Black (200 ml Standard, 90–480 ml). 5 Profile, Rezepte je Profil. `offiziell` Anleitung 57132C8859 (03/2025) https://dls.delonghigroup.com/files/docs/5/EN-57132C8859.pdf
- cremalink/aioecam haben **kein Long Black** in der Soul-Tabelle → Frame selbst bauen (Muster Americano) oder App-Verkehr mitschneiden.

## 5. Fernstart-Voraussetzungen
- Hauptschalter an, in den Maschineneinstellungen „Connectivity“ + Fernsteuerung aktiv.
- Aufwecken (0x84) → nicht auf 0x84-Antwort warten, sondern Status 0x75 bis „Ready“ (7) pollen; Aufheiz-/Spülgang ist nicht unterbrechbar. Erst dann 0x83 senden.
- Kein Tassensensor, keine Bestätigung an der Maschine: Tasse vorher unterstellen. Alarme (Tank leer, Satzbehälter voll) blockieren.

## 6. Architektur-Empfehlung (für Jarvis)
- **Ein** Dienst im Heimnetz hält die einzige Verbindung zur Maschine (LAN-Modus, Cloud nur als Rückfall); Jarvis, Mac, Sprachbefehl rufen nur dessen HTTP/MCP-Schnittstelle. Keine direkten Cloud-Aufrufe von jedem Client.
- Coffee-Link-App am Handy geschlossen halten, sonst hat ihre LAN-Sitzung Vorrang und Befehle werden still ignoriert.
- Kandidaten: FiveElements/delonghi-coffeelink-local (nennt 610.74.MB, REST + MCP, ohne Auth, Brühen nicht live erprobt) · aioecam/cremaevolution-ha (LAN, Python, an 610.75 erprobt) · sk7n4k3d/delonghi-ha (HA, Long-Black-Knopf, Cloud + LAN ab 1.9.0). Quellcode vor Eingabe der Zugangsdaten prüfen (junge Beta-Projekte).
