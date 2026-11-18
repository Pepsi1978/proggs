# R1 – Cloud-Weg der De'Longhi Coffee Link (PrimaDonna Soul ECAM 610.74 / WLAN-ECAMs)

Stand: 18.11.2026. Methode: Quellcode der Projekte selbst gelesen (geklont und durchsucht), dazu PyPI/GitHub-API für Aktivität. Keine Zugangsdaten verwendet, Secrets der Projekte hier bewusst nicht wiedergegeben (stehen öffentlich in den Repos).

## 1. Kurzfazit

- Hinter der Coffee-Link-App steckt die **Ayla Networks IoT-Cloud** (Hersteller-Cloud-Plattform) mit **SAP Gigya (CDC) als Login/Identity-Provider** (OIDC). Beide Fehlermeldungen der App bestätigen das: L001 = ungültiges Token von SAP Gigya, L002 = ungültiges Token von Ayla Login (De'Longhi-FAQ: https://delonghi.com/en-nz/faqs/What-to-do-in-case-of-error-code-L002/a/278643 und https://delonghi-s2.delonghigroup.com/en-nz/faqs/What-to-do-in-case-of-error-code-L001/a/278640).
- Es gibt **keine offizielle, offene API**. Aber mehrere aktive Reverse-Engineering-Projekte (2026) dokumentieren und nutzen den Cloud-Weg **ausdrücklich für die PrimaDonna Soul** inkl. Brühbefehlen.
- Ein Brüh-Befehl = ein kleiner **Binär-Frame, base64-kodiert, als Datapoint auf die Ayla-Property `data_request`** (Soul) bzw. `app_data_request` (Eletta Explore & Co.) geschrieben.
- **Long Black** = Getränke-ID **0x19 (25)**. Die Soul kennt das Getränk intern (Rezept-Property `d019_rec_long_black`, Zähler `d728_id25_long_black`). Fertige Befehls-Tabellen der Projekte enthalten Long Black allerdings nur teils (siehe Abschnitt 6).

## 2. Cloud-Architektur und Anmeldung

Quelle: https://github.com/miditkl/cremalink (cremalink/clients/auth.py, resources/api_config.json), https://github.com/rtfpessoa/delonghi-comfort-client (cli.py), https://github.com/actabi/delonghi_coffeelink (README "Technical details", const.py), https://github.com/sk7n4k3d/delonghi-ha

Ablauf (identisch in cremalink, delonghi-comfort-client, actabi; ursprünglich aus ECAMpy https://github.com/duckwc/ECAMpy):

1. **Gigya OIDC** (SAP Customer Data Cloud), Region EU: `https://fidm.eu1.gigya.com/oidc/op/v1.0/<GIGYA_API_KEY>/authorize` (Scope: `openid email profile UID comfort en alexa`, redirect_uri `https://google.it`), Hilfsaufrufe `https://socialize.eu1.gigya.com/socialize.getIDs`, `https://accounts.eu1.gigya.com/accounts.login`, `socialize.getUserInfo`, dann `.../authorize/continue` und `.../token` (HTTP Basic mit Gigya CLIENT_ID:CLIENT_SECRET). Seite für Consent: `https://aylaopenid.delonghigroup.com/`.
2. Ergebnis ist ein **IDP-Token (id_token/JWT)**. Dieses wird bei Ayla getauscht: `POST https://user-field-eu.aylanetworks.com/api/v1/token_sign_in` mit `app_id`, `app_secret`, `token`. Ergebnis: Ayla `access_token` + **`refresh_token`**.
3. Refresh: `https://user-field-eu.aylanetworks.com/users/refresh_token.json` (delonghi-comfort-client). Das Refresh-Token genügt danach dauerhaft (cremalink: `token.json` mit nur `refresh_token`).
4. API-Aufrufe: Basis `https://ads-eu.aylanetworks.com/apiv1`, Header `Authorization: auth_token <access_token>`.
5. **Region**: EU-Konto -> `user-field-eu` / `ads-eu`; US/Kanada -> `user-field.aylanetworks.com` / `ads-field.aylanetworks.com` (anderes `app_id`: im Repo `DLonghiCoffeeIdKit-sQ-id` für EU bzw. `DeLonghiCoffeeIdKit-yA-id` für US; `app_secret` je Region steckt in der Coffee-Link-APK und steht im actabi-Repo). Gigya ist global; ein Konto in falscher Region meldet sich an und findet "0 Geräte" (actabi README/const.py). sk7n4k3d nennt zusätzlich CN.
6. Konto: dasselbe De'Longhi-Konto wie Website/Comfort-App; Social-Login (Apple/Google/Facebook) in der App möglich (https://www.delonghi.com/en-gb/faqs/Which-login-credential-should-I-use-in-the-Coffee-Link-App/a/583925). Für Skripte braucht man ein **E-Mail/Passwort-Konto** (bei Social-Login-Konten ggf. Passwort über "Passwort vergessen" setzen: Annahme, nicht belegt).

Hinweis: Die Maschine muss vorher per App im Konto registriert sein (comfort-client README).

## 3. Endpunkte (Ayla ADS, Gerätesicht)

Belegt durch cremalink `cremalink/transports/cloud/transport.py` und `clients/cloud.py`:

- Geräteliste: `GET /apiv1/devices.json` (über `client.get_devices()` -> DSNs)
- Gerätedaten: `GET /apiv1/dsns/<DSN>.json` (Felder: `key`, `model`, `lan_enabled`, `connection_status` Online/Offline, `lan_ip`)
- LAN-Schlüssel: `GET /apiv1/dsns/<DSN>/lan.json` (`lanip.lanip_key`) – für Lokalbetrieb
- Alle Properties: `GET /apiv1/dsns/<DSN>/properties.json`; einzelne: `.../properties.json?names[]=<name>`
- **Befehl senden**: `POST /apiv1/dsns/<DSN>/properties/data_request/datapoints.json` mit Body `{"datapoint": {"value": "<base64>"}}`
- Seriennummer: `properties/d270_serialnumber.json`
- Status: Property `d302_monitor` (Soul) bzw. `d302_monitor_machine` (Eletta Explore), base64-Binärframe; Antworten der Maschine: `data_response` / `app_data_response`.

## 4. Property-Namen je Modellfamilie

Quellen: cremalink `devices/ECAM610.json`, `ECAM612.json`, `ECAM452.json`; actabi `const.py`; sk7n4k3d `api.py`.

| Familie (oem_model) | Befehlskanal | Verbunden-Heartbeat |
|---|---|---|
| PrimaDonna Soul (DL-millcore, DL-pd-*) ECAM610/612/613 | `data_request` / `data_response` | `device_connected`; Befehl geht ohne Zusatz-Heartbeat (cremalink-Profil `ECAM610`/`ECAM612`: nur `data_request` + `d302_monitor`) |
| Eletta Explore (DL-striker-cb) ECAM45x | `app_data_request` / `app_data_response` | `app_device_connected` + `app_id` – ohne Heartbeat werden Getränkebefehle von der Cloud akzeptiert, aber von der Maschine **still ignoriert** (cremalink-Issue #224 https://github.com/miditkl/cremalink/issues/224, 17.08.2026) |

Wichtig: Die Frage "app_data_request" im Auftrag gilt also für Eletta Explore & Co.; die **Soul nutzt `data_request`**. sk7n4k3d probiert beim Senden mehrere Varianten durch (data_request ohne/mit app_id, app_data_request) – Beleg, dass Firmware-Stände der Soul abweichen (https://github.com/sk7n4k3d/delonghi-ha, api.py Zeilen ~276-286, 917-936).

## 5. Aufbau eines Brüh-Befehls (Soul)

Quelle: actabi README "Technical details" (https://github.com/actabi/delonghi_coffeelink), cremalink `domain/device.py` (`_encode_command`), Bluetooth-Vorarbeit https://github.com/otto-dev/delonghi-coffee-link-python und https://grack.com/blog/2022/12/02/hacking-bluetooth-to-brew-coffee-on-github-actions-part-2/ .

Frame (Bytes, hex):

```
0d <len> 83 f0 <getränk-id> <aktion 01=Start 02=Stop> <Rezept-Parameter...> <CRC16 AUG-CCITT> [<Unix-Zeit 4 Byte BE>]
```
- Präfix `0d`, Länge, Befehlsfamilie `83 f0` (Getränk), Getränke-ID, Aktion, Rezept-TLV (z. B. Tag 01 = Kaffeemenge 16 Bit in ml, 02 = Aroma/Taste, 1b/Temperatur usw.), Schluss `06` (Zubereitungsmodus), CRC16 (Polynom 0x1021, Init 0x1D0F = "AUG-CCITT") über den Frame, danach Zeitstempel. Dann **base64** und als Datapoint-Value an `data_request`.
- Beispiele aus cremalink `devices/ECAM612.json` (Hex vor dem Anhängen von Zeitstempel):
  - Wake/Einschalten: Familie `84 0f` (`0d0784 0f02015512` im BLE-Beispiel); Standby: `84 0f` mit Parametern `01 01`.
  - Long Coffee: `0d1183f003010100a01b010203040006fc08` (ID 0x03)
  - Espresso: `0d1183f0010101007e1b030202040006afe2`
  - Hot Water: ID 0x10
- actabi README: Soul-Frame "fixed 18 bytes" (ID, Aktion, 6 Byte Rezeptparameter, CRC, Zeitstempel).
- Getränke-IDs (longshot `src/protocol/hardware_enums.rs`, https://github.com/mmastrac/longshot): 1 Espresso, 2 Coffee, 3 Long Coffee, 4 2x Espresso, 5 Doppio+, 6 Americano, 7 Cappuccino, 8 Latte Macchiato, 9 Caffe Latte, 10 Flat White, 11 Espresso Macchiato, 12 Hot Milk, 15 Cappuccino Reverse, 16 Hot Water, 17 Steam, 24 Cortado, **25 Long Black**, 26 Travel Mug, 27 Brew over Ice, 230-239 Custom 01-10.
- **Rezept-Properties der Soul** (actabi, tests/fixtures/soul_properties.json): `d019_rec_long_black` (Werksrezept) und je Profil `d057_1_rec_long_black`, `d078_2_...`, `d099_3_...`, `d120_4_...`, `d141_5_...` (jeweils base64-Binärblob mit Familie `a6 f0`/`b0 f0`). Beim Eletta andere Nummerierung (`d059_rec_1_espresso` usw.). Diese Blobs enthalten Menge/Aroma/Temperatur, die der Brüh-Frame übernimmt.
- Es gibt zusätzlich: **Session/Heartbeat** (`device_connected`/`app_device_connected`, Wert = base64(Unix-Zeit 4 Byte + app_id 4 Byte), Konstante `C0FFEE..` im Projekt); **Konflikt mit der App**: Solange die Coffee-Link-App im selben WLAN offen ist, hält sie eine LAN-Sitzung (30 s Keepalive) und die Maschine **ignoriert Cloud-Befehle** (actabi README "IMPORTANT").
- Die Cloud nimmt Befehle auch für **offline Maschinen** mit 200 OK an, die Maschine bekommt sie nie (actabi README). Vorher `connection_status` prüfen.
- Aus Standby bis zum fertigen Espresso: ca. 50 s (Issue #224).

## 6. Long Black konkret

- Getränke-ID 0x19 (25): in actabi `const.py` Zeile ~285 als "Long Black" in der BEVERAGES-Liste (Button vorhanden); Zähler `d728_id25_long_black` (CHANGELOG actabi); in sk7n4k3d `const.py` `"long_black": {"drink_id": "25"}` mit "Recipe-to-Brew-Konvertierung für alle 50+ Getränke".
- cremalink hat Long Black **nicht** in der `command_map` für ECAM610/612 (nur americano, coffee, long_coffee, espresso, espresso_soul, cortado ...). Eigene Aufnahme wäre möglich ("Adding Custom Devices", https://github.com/miditkl/cremalink/wiki).
- actabi-Hinweis: Long Black (0x19), Mug to Go (0x1a), Brew over Ice (0x1b) stehen auf der Referenz-Soul in **keiner** der Profil-Kurzlisten (`a8 f0`), sind aber mit Werksrezept und Zählern vorhanden und "brühbar" (catalog.py Kommentar). Ob die **ECAM 610.74** (nicht 610.75) Long Black in der App-Auswahl anbietet, ist nicht belegt; De'Longhi-Produktseiten der 610.75 nennen Long Black als Getränk (Suche, Händlerseiten). **Das ist vor der Umsetzung am echten Gerät zu prüfen**: in der App ein Long Black starten und mit dem actabi-Sensor "Last Captured Command" den echten Frame mitlesen (`matches_integration`).

## 7. Reverse-Engineering-Projekte und Aktualität (gelesen am 18.11.2026)

| Projekt | Zweck | Soul? | Aktivität |
|---|---|---|---|
| https://github.com/actabi/delonghi_coffeelink (HA, MIT) | Ayla-Cloud, Soul zuerst, Eletta per "Teach"; Services `start_beverage`, `send_raw_command` mit base64 | **Ja, "works out of the box"** (ECAM610/612/613, DL-millcore) | seit 20.08.2026, Release v0.3.26 vom 28.10.2026, 19 Sterne |
| https://github.com/sk7n4k3d/delonghi-ha (HA) | Cloud + LAN-Socket, 50+ Getränke, EU/US/CN; nennt Eletta Explore, Dinamica Plus (ECAM370), PrimaDonna Class/Elite/S, Maestosa; "PrimaDonna Soul confirmed by community"; **Rivelia und Perfetto: unbestätigt** | Ja | seit 11.11.2026, Release v1.9.3 am 18.11.2026 (sehr aktiv) |
| https://github.com/miditkl/cremalink (PyPI `cremalink` 0.1.0b28, AGPL-3.0, Python >= 3.13) | Python-Bibliothek + lokaler FastAPI-Server (Port 10280), Cloud- und LAN-Modus; Profile ECAM612, ECAM610, ECAM452 | **Ja** (Entwicklung primär für PrimaDonna Soul) | seit 01.01.2026, letzter Commit 04.09.2026 (+Dependabot bis 13.11.2026), b28 vom 04.09.2026; Issues #212 (Aug 2026) bestätigt ECAM610.75 funktioniert lokal mit b27; Issue #224 Eletta Explore Cloud ok (Aug 2026) |
| https://github.com/rtfpessoa/delonghi-comfort-client | Minimaler Ayla-Login-CLI (Entfeuchter, "kann auch Kaffeemaschinen") | indirekt | Commit 01-02.01.2025 |
| https://github.com/duckwc/ECAMpy | Ursprung des Gigya->Ayla-Codes, lokaler Webserver | PrimaDonna Soul | letzter Push 26.02.2024 |
| https://github.com/mmastrac/longshot | Rust, BLE-Protokoll der ECAMs (IDs, Enums) | BLE | letzter Push 21.05.2025 |
| https://github.com/otto-dev/delonghi-coffee-link-python | BLE-Frames, CRC | BLE | 2021 |
| https://github.com/Arbuzov/home_assistant_delonghi_primadonna | BLE-HA-Integration (Soul laut Liste nicht enthalten) | nein | – |
| https://pypi.org/project/mcp-delonghi-ecam/ (daedalus, 0.1.1 vom 10.04.2026) | MCP-Server, lokale LAN-API Port 10280; nennt ECAM 550, Dinamica Plus, PrimaDonna Soul | ja (lokal) | Apr 2026 |
| https://github.com/sk7n4k3d/delonghi-daedalus-ha | Neue "Daedalus"-Maschinen (Eletta Ultra / My Coffee Lounge): **anderer Stack Gigya + AWS IoT statt Ayla** | nein | aktiv |
| Homey-App https://community.homey.app/t/app-pro-delonghi-coffee-link-unofficially-monitor-and-control-delonghi-wi-fi-coffee-machines-from-homey/159352 | WLAN-Coffee-Link, PrimaDonna Soul ECAM610.75 koppelbar, Getränke starten | ja | Sep 2026; Wi-Fi-Modelle ohne offene API ("Ayla SDK, Zertifikate") laut älterem Thread https://community.homey.app/t/app-pro-delonghi-coffee/80377 (Dez 2023) |

Folgerung zur Aktualität: Cloud-Weg funktioniert nach den Bestätigungen **2026** (Aug-Nov) für Soul und Eletta Explore. Für Rivelia (EXAM440) und Dinamica Plus gilt: Ayla-basiert laut sk7n4k3d, Rivelia unbestätigt. Neue Maschinen (Eletta Ultra, "Daedalus") laufen auf einer anderen Cloud (AWS IoT).

## 8. Risiken / Grenzen

- Inoffiziell: laut actabi-Disclaimer können De'Longhi/Ayla das Protokoll jederzeit ändern. `app_secret`/Client-Secrets sind aus der App extrahiert (im Repo öffentlich) – Nutzung geschieht auf eigene Verantwortung (AGB/ToS nicht geprüft).
- Ayla begrenzt Anfragen: HTTP 429 mit Backoff (sk7n4k3d api.py ~Zeile 132, 209).
- Coffee-Link-App muss geschlossen sein, sonst gewinnt die LAN-Sitzung der App.
- Soul-Maschine muss online sein (Standby ist okay, die Maschine bleibt online und ist per Wake weckbar – actabi).
- Befehle aus der Cloud und lokale Wege: Alternative **lokaler LAN-Weg** (cremalink-Server, LAN-Key aus `lan.json`, AES-verschlüsselt) ohne Cloud-Latenz; der LAN-Key kommt aber aus der Cloud (einmalig).
- Python 3.13+ nötig für cremalink; actabi/sk7n4k3d sind HA-Komponenten (der Kern `ayla_client.py`/`command_builder.py` ist aber eigenständig lesbar und portierbar, z. B. nach Swift/Kotlin).

## 9. Empfehlung für das Ziel "Long Black per Programm starten"

1. Cloud-Weg ist machbar und aktuell belegt (Soul).
2. Am schnellsten: Login-/Befehlscode aus actabi (`ayla_client.py`, `command_builder.py`, MIT-Lizenz) oder cremalink (AGPL!) als Vorlage nehmen. Frame für ID 0x19 bauen (Rezeptparameter aus `d019_rec_long_black` bzw. Profilrezept lesen) oder per "Last Captured Command" einmal den App-Frame mitschneiden und wiederholen ("learn and replay", Zeitstempel erneuern, CRC neu).
3. Coffee-Link-App am Telefon vorher schließen. Maschine ggf. vorher wecken (`84 0f`), ~45-50 s warten, dann Befehl.

---

BEST-PRACTICES-KANDIDATEN:
- De'Longhi Coffee Link per Ayla-Cloud ansprechen (Gigya OIDC -> Ayla token_sign_in -> refresh_token -> `POST /apiv1/dsns/<DSN>/properties/data_request/datapoints.json`, base64-Binärframe mit CRC16 AUG-CCITT + Zeitstempel; Soul = `data_request`, Eletta = `app_data_request` + `app_device_connected`-Heartbeat; Coffee-Link-App schließen; Long Black = ID 0x19). Quellen: https://github.com/actabi/delonghi_coffeelink (v0.3.26, 28.10.2026), https://github.com/miditkl/cremalink (0.1.0b28, 04.09.2026), https://github.com/sk7n4k3d/delonghi-ha (v1.9.3, 18.11.2026), https://github.com/rtfpessoa/delonghi-comfort-client
- Vor Cloud-Befehl `connection_status` prüfen (Cloud meldet 200 OK auch für Offline-Geräte); Monitor-Frame-Zeitstempel als Staleness-Test (cremalink Issue #224).

BUG-KANDIDATEN:
- Eletta Explore (ECAM452, cremalink 0.1.0b27): Getränkebefehle werden von Ayla akzeptiert, von der Maschine ignoriert, wenn kein frischer `app_device_connected`-Heartbeat geschrieben wird; Monitor friert dann ein. https://github.com/miditkl/cremalink/issues/224
- Coffee-Link-App im selben WLAN offen -> Maschine ignoriert Cloud-Befehle (LAN-Sitzung hat Vorrang). https://github.com/actabi/delonghi_coffeelink (README, v0.3.26)
- Falsche Ayla-Region (EU/US) -> Login klappt, "0 Geräte". actabi const.py / Issue #60, v0.3.26
- PrimaDonna Soul ECAM610.75, cremalink 0.1.0b27: `is_watertank_empty` und `is_busy/is_idle` falsch dekodiert (Profil ECAM612). https://github.com/miditkl/cremalink/issues/212 und #249 (offen)
- Ayla HTTP 429 (Rate Limit) bei zu häufigem Polling: https://github.com/sk7n4k3d/delonghi-ha (api.py, v1.9.3)
- Homey-App: "Timeout after 10000ms" bei wiederkehrenden Requests, Eletta Explore 450.65.g nicht gefunden (Sep 2026): https://community.homey.app/t/app-pro-delonghi-coffee-link-unofficially-monitor-and-control-delonghi-wi-fi-coffee-machines-from-homey/159352
