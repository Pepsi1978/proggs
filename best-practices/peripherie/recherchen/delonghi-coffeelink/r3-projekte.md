# R3 – Fertige Open-Source-Projekte und Integrationen für De'Longhi PrimaDonna Soul ECAM 610.74 („Long Black“)

Stand der Recherche: 18.11.2026. Alle Repo-Daten (Push-Datum, Releases, Issues, Sterne) wurden am 18.11.2026 über die GitHub-API (gh api) gelesen; Quellcode und READMEs direkt aus den Repos. Aussagen mit Quelle (URL) je Punkt.

## 0. Wichtigste Erkenntnisse vorab

1. Die Maschine ist ein WLAN-Gerät (Ayla-Networks-Modul, OEM-Modell `DL-millcore`). Die Modelltabelle der Coffee-Link-App führt „ECAM 610.74.MB / PrimaDonna SOUL ECAM610.74 / appModelId PD_SOUL / connectionType Wi-Fi / 5 Profile / 18 Standardrezepte / 6 eigene Rezepte“, und „Long Black“ (Rezept-ID 25 = 0x19) steht im Katalog dieses Modells. Quelle: https://github.com/FiveElements/delonghi-coffeelink-local/blob/main/src/lib/machine-catalogs.json (Eintrag `models.17054`, aus der Coffee-Link-APK-Tabelle „Machine Template“ v1.510 extrahiert – Sekundärquelle, die APK selbst wurde nicht geprüft).
2. Der bekannteste Treiber, Arbuzov/home_assistant_delonghi_primadonna, ist Bluetooth-only und unterstützt die Maschine NICHT (Issue #25 „ECAM 610.74.MB“ seit 22.11.2022 offen; Arbuzov: „It's impossible to add something without an access“, „This integration supports bluetooth device only“ in #53 „PrimaDonna Soul“). Quellen: https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/25 und https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/53
3. Für die Soul gibt es seit 2026 mehrere neue WLAN-Projekte (Cloud und/oder LAN-Modus). Keine Quelle belegt, dass „Long Black“ auf einer Soul real gebrüht wurde. Die Rezept-ID 0x19 ist in zwei Cloud-Integrationen als Button angelegt (actabi, sk7n4k3d), in den LAN-Bibliotheken (cremalink, aioecam) fehlt das Getränk in der Befehlstabelle.
4. Kein Projekt für Node-RED, Homebridge, ESPHome oder eine eigene Android-App mit Soul-Unterstützung gefunden (siehe Abschnitt 4).

## 1. Zwei Verbindungswege der Soul (Hintergrund für die Bewertung)

- Cloud: Gigya-Login -> JWT -> Ayla-SSO-Token -> Schreiben der Property `app_data_request` (Base64-ECAM-Frame). Beschrieben bei https://github.com/actabi/delonghi_coffeelink (Abschnitt „Technical details“) und https://github.com/sk7n4k3d/delonghi-ha (Abschnitt „Protocol Documentation“).
- LAN-Modus (Ayla): Rollen sind vertauscht – NICHT der Client verbindet sich mit der Maschine, sondern die Maschine verbindet sich zu einem lokalen Server (Port z. B. 10280/3000/10275); AES-256-CBC-Schlüsselaustausch; der LAN-Schlüssel wird einmalig aus dem Coffee-Link-Konto (Cloud) geholt. Quelle: https://github.com/FiveElements/delonghi-coffeelink-local (Abschnitt „Le point qui décide de tout“) und https://github.com/Styx85/cremaevolution-ha (Abschnitt „Network requirement“).
- Gemeinsame Falle: Die Coffee-Link-App muss geschlossen sein, sonst hält sie die LAN-Sitzung (30 s Keep-Alive) und die Maschine ignoriert Cloud-Befehle. Quelle: https://github.com/actabi/delonghi_coffeelink (Abschnitt „IMPORTANT - Coffee Link mobile app must be closed“); ebenso https://github.com/AClerbois/mcp-delonghi.
- Nur Bluetooth (BLE) taugt für die Soul nach aktueller Quellenlage nicht: Die BLE-Projekte nennen die Soul nicht, und Nutzer der Soul schließen aus Tests: „this integration doesn't work with the soul“ (roperstv, https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/53).

## 2. Bewertung der Projekte mit Soul-/WLAN-Bezug

### 2.1 FiveElements/delonghi-coffeelink-local (LAN, Node.js, REST + MCP)
- URL: https://github.com/FiveElements/delonghi-coffeelink-local
- Weg: LAN-Modus (kein Cloud-Betrieb; Cloud nur optional, um den LAN-Schlüssel einmalig zu holen). Node.js >= 26, pnpm, SQLite; Docker-Image `ghcr.io/fiveelements/delonghi-coffeelink-local:edge`. Eigener HTTP-Server mit Web-Oberfläche, Steuer-API `/api/*` (u. a. `/api/command`) und MCP-Endpunkt `/mcp` (Streamable HTTP, Tokens mit Berechtigungen). Quelle: README im Repo.
- Modelle: „Développé et validé sur une Primadonna Soul ECAM 610.75.MB“; Modelltabelle enthält ausdrücklich „ECAM 610.74.MB“ (productCode 0132217054, PD_SOUL, Wi-Fi). „Restent 10 modèles pleinement servis : 5 PD_SOUL (28 boissons, 5 profils)“. Das Modell wird aus der Seriennummer (`d270_serialnumber`, Zeichen 1–5) gelesen.
- Long Black: im Katalog der PD_SOUL-Modelle enthalten (ID 25; Eintrag bei 17054, 17055, 17059, 17062, 17077). Eigene Rezepte: lesen/schreiben der 5 Profile samt Rezepten und Bean Adapt ist implementiert („Lire et écrire les recettes de chacun des 5 profils“).
- Aktivität: Repo angelegt 20.08.2026, letzter Push 13.11.2026, Release v0.1.5 (27.10.2026), 0 Sterne, Lizenz GPL-3.0. Offene Issues: nur Dependabot-PR (#15), keine Fehlermeldungen.
- Wichtige Einschränkung (README, „État“): „Pas encore éprouvé en conditions réelles : la préparation effective d'une boisson via le serveur, la commande d'arrêt, l'écriture d'une recette …“. Das Brühen ist also vom Autor selbst noch nicht live bestätigt; Ein/Aus, Monitor, Katalog, Profile, Statistik sind es. Keine Authentifizierung an `/api/*` (nur im LAN/VPN betreiben).
- Bewertung: einziges Projekt, das die Maschine 610.74 namentlich in der Modelltabelle führt, Long Black im Katalog hat und eine programmierbare Schnittstelle (REST, MCP) bietet. Brühen ungetestet, Projekt sehr jung, ein Einzelentwickler.

### 2.2 actabi/delonghi_coffeelink (Cloud, Home Assistant, HACS)
- URL: https://github.com/actabi/delonghi_coffeelink
- Weg: Coffee-Link-Cloud (Gigya + Ayla); LAN-Server ist „a future version“ geplant. Dienste `delonghi_coffeelink.start_beverage` (z. B. `beverage: espresso`), `stop_beverage`, `send_raw_command`.
- Modelle: „PrimaDonna Soul ECAM610.xx, ECAM612.xx, ECAM613.xx (oem_model = DL-millcore) - works out of the box“, als „Confirmed working“ geführt. Die Referenzmaschine ist laut Code-Kommentar eine PrimaDonna Soul.
- Long Black: Button/Getränk `long_black` (ID 0x19) in `BEVERAGES` (const.py), Zähler `d728_id25_long_black`. Einschränkung im Code: Für die Soul wird ein fester 18-Byte-Befehl mit Standard-Rezeptparametern gesendet (`DEFAULT_RECIPE_PARAMS`, aus einem Heißwasser-Befehl abgeleitet). Ob die Long-Black-Menge/Temperatur der Maschine damit stimmt, ist nicht belegt. Der Katalog liest zwar die eigenen Rezepte der Maschine aus, nutzt sie aber laut README „Currently used to keep the diagnostics and the learn-and-replay path honest; it creates no entities yet“. Eigene Rezepte: nicht per Name startbar; Profil-Buttons vorhanden.
- Aktivität: angelegt 22.04.2026, letzter Push 12.11.2026, Release v0.3.26 (28.10.2026), 19 Sterne, MIT.
- Offene Issues: #74 „Counters not updating on PrimaDonna Soul (DL-millcore)“ (06.11.2026; Cloud-Zähler seit 28.09. eingefroren, v0.3.26), #76 Setup scheitert bei zweitem Gerät (Pinguino), #60 „No DeLonghi devices found“ (US-Region; mit v0.3.26 Region-Feld ergänzt).
- Bewertung: ausdrücklich für ECAM610.xx gebaut und auf der Soul getestet; Long-Black-Button vorhanden, aber Parameter unklar. Nur Home Assistant, kein eigenständiges Programm; die Protokolldokumentation im README ist aber gut für einen eigenen Client (Cloud-Zugriff nachbauen).

### 2.3 sk7n4k3d/delonghi-ha (Cloud + LAN, Home Assistant, HACS)
- URL: https://github.com/sk7n4k3d/delonghi-ha
- Weg: Ayla-Cloud; seit v1.9.0 (17.11.2026) zusätzlich LAN-Socket für Status und Brühen („Brew commands now route over the LAN socket when the session is alive“, Issue #50, Kommentar vom 17.11.2026).
- Modelle: „PrimaDonna Soul (ECAM610.xx) — confirmed working by community“; im Code Produktcode 217055 -> ECAM610.75 bestätigt. „All 50+ beverages — universal recipe-to-brew conversion“, d. h. die Rezepte der Maschine werden gelesen und umgesetzt.
- Long Black: im Code vorhanden (`"long_black": {"name": "Long Black", "drink_id": "25"}` in const.py). Dienst `delonghi_coffee.brew_custom` für Mengen/Stärke/Temperatur/Profil; Profil-Auswahl, Bean-Adapt-Dienste.
- Aktivität: angelegt 24.03.2026, letzter Push 18.11.2026, Release v1.9.3 (18.11.2026, vier Releases in 24 h), 33 Sterne, MIT. Sehr aktiv, aber schnelle Release-Folge = instabil.
- Offene Issues zur Soul: #50 „DL-millcore (PrimaDonna Soul): brew commands accepted but ignored — Coffee Link app brews fine“ (05.11.2026, Firmware ADA 1.5.3, Nutzer mit ECAM610.75.MB bestätigen). Ursache laut Autor: cloud-Monitor (`d302_monitor`) ist eingefroren, solange keine App-Sitzung läuft, und die Soul schluckt Cloud-`data_request`-Befehle. Fix v1.9.0 über LAN-Pfad, noch ohne Bestätigung durch einen Nutzer. Frühere Issues #34 (Ein/Aus ohne Reaktion, später gelöst), #5, #11 „Brewing Buttons not visible for Primadonna Soul“ (geschlossen).
- Bewertung: größter Funktionsumfang (Rezepte aus der Maschine, brew_custom), aber exakt die Soul-Cloud-Brühfunktion war bis v1.8.0 kaputt; Zustand mit v1.9.x unbestätigt.

### 2.4 Styx85/cremaevolution-ha + Styx85/aioecam (LAN-first, Home Assistant + Python-Bibliothek)
- URLs: https://github.com/Styx85/cremaevolution-ha (HA, HACS; Fork von miditkl/cremalink-ha, Domain weiter `cremalink_ha`) und https://github.com/Styx85/aioecam (async Python-Bibliothek mit CLI).
- Weg: lokal (Ayla-LAN-Modus) primär, Cloud nur zum Schlüsselholen/Aufwecken; zusätzlich Cloud-Modus (`CloudLink`). Keine Add-on mehr. `python -m aioecam --device 192.168.1.50 brew espresso`, `on`, `status`, `stats`; LAN-Schlüssel aus `~/.config/aioecam.json`.
- Modelle: aioecam-Profil `ecam61x.json` für ECAM610/612/613 (OEM `DL-millcore`, `DL-pd-soul`). „Live testing has been performed on a PrimaDonna Soul ECAM610.75“ (Wake, Einstellungen, Rezeptlesen, Profilwechsel, 100-ml-Heißwasser). Espresso-Brühen/Aus über die Soul sind laut cremaevolution-ha „verified live: on, espresso, off, full A2 table“. 610.74 nicht genannt (gleiche Familie).
- Long Black: NICHT in der Befehlstabelle (Befehle: americano, caffe_latte, cappuccino, cappuccino_mix/plus, coffee, cortado, doppio_plus, double_espresso, espresso, espresso_macchiato, espresso_soul, flat_white, hot_milk, hot_water, latte_macchiato, long_coffee). `cremalink_ha.brew_custom` erlaubt eigene Mengen (coffee_ml, water_ml, milk, intensity), aber nur für diese Getränke. Long Black müsste als neuer Eintrag mit Frame (ID 0x19) ergänzt werden.
- Aktivität: cremaevolution-ha Release v0.3.9b1 (02.11.2026), letzter Push 02.11.2026, 0 Sterne; aioecam v0.1.7b1 (02.11.2026), angelegt 18.10.2026, 0 offene Issues. Beta. Offen: #36 „LAN polling makes the Wi-Fi module open a cloud TLS connection every poll“ (16.11.2026).
- Bewertung: technisch am sorgfältigsten dokumentiert (Protokollmessungen: https://github.com/Styx85/cremaevolution-ha/blob/main/docs/ecam-lan-protocol.md), am besten als Basis für einen eigenen lokalen Python-Client; Long Black fehlt.

### 2.5 miditkl/cremalink + cremalink-ha (Original; LAN + Cloud, Python-Bibliothek + HA)
- URLs: https://github.com/miditkl/cremalink und https://github.com/miditkl/cremalink-ha
- Weg: LAN-Server (FastAPI, `cremalink-server`, Add-on) oder Cloud-API; PyPI-Paket `cremalink` (Python ≥ 3.13).
- Modelle: „primarily optimized for the De'Longhi PrimaDonna Soul“; Geräteprofile `ECAM610.json`, `ECAM612.json`, `ECAM452.json` (README nennt ECAM612 und ECAM452). Profil ECAM610 mit `support: {cloud: true, local: true}`.
- Long Black: nicht vorhanden (20 Befehle, nur `long_coffee`). Eigene Geräteprofile (.json) sind laut README erweiterbar (Wiki „5. Adding Custom Devices“).
- Aktivität: cremalink v0.1.0-beta.28 (04.09.2026), letzter Push 13.11.2026 (nur Dependabot), 16 Sterne, AGPL-3.0; cremalink-ha v0.1.0-beta.26 (07.05.2026), 25 Sterne, viele offene Dependabot-PRs.
- Offene Issues zur Soul: #249 „ECAM610: busy/idle follow `action`, and `is_watertank_empty` never matches the tank“ (10.10.2026, an ECAM610.75, PR #250 offen); cremalink-ha #65 „502 Bad Gateway - Commands not working“ (offen). Stagnierend; die aktive Weiterentwicklung läuft in Forks (Styx85/cremaevolution-ha, lodzen/cremalink-ha 2.0.0.dev7 vom 16.11.2026).
- Quelle für die Aussage, dass der ursprüngliche Add-on-Aufbau unzuverlässig war: https://github.com/Styx85/cremaevolution-ha (Abschnitt „Why a rewrite“).

### 2.6 chrvidal/ioBroker.coffeelink (ioBroker-Adapter, Cloud lesen + LAN steuern)
- URL: https://github.com/chrvidal/ioBroker.coffeelink – Release v0.2.3 (08.11.2026), 0 Sterne, MIT.
- Cloud nur lesend; LAN-Steuerung (Ein/Aus, Stop, Auto-Off, Wasserhärte, Getränkebuttons je Profil „for manual use“) getestet an PrimaDonna Soul ECAM612.55; „The adapter recognizes the ECAM610, ECAM612 and ECAM613 families as PrimaDonna Soul“. Long Black nicht erwähnt; Roadmap: „Add PrimaDonna Soul recipe buttons for milk drinks, Doppio+ and other presets“. Für diesen Zweck nur wenn ioBroker ohnehin läuft.

### 2.7 Homey: „DeLonghi Coffee Link“ (nl.sterhove.delonghi)
- URL: https://community.homey.app/t/159352 und https://homey.app/de-at/app/nl.sterhove.delonghi/DeLonghi-Coffee-Link/test/ – Test-Modus, kein Open-Source-Repo gefunden.
- Ein Nutzer: „The pairing is working now for my PrimaDonna Soul ECAM610.75“ (Statistiken/Anzeigen sichtbar); Start-/Standby-Funktionen noch nicht bestätigt; Update vom 10.09.2026; „Timeout after 10000ms“ ungelöst. Long Black nicht erwähnt. Nur für Homey-Nutzer relevant.
- Die ältere Homey-App „Delonghi.Coffee“ (https://community.homey.app/t/app-pro-delonghi-coffee/80377) ist Bluetooth-only und unterstützt die Soul nicht („This is a wifi connected machine, I don't have one myself“).

### 2.8 AClerbois/mcp-delonghi (MCP-Server, Cloud, .NET 10)
- URL: https://github.com/AClerbois/mcp-delonghi – letzter Push 15.11.2026, keine Releases, MIT, 1 Stern; NuGet-Paket `AClerbois.MCP.DelonghiConnector`.
- Weg: Coffee-Link-Cloud (Gigya + Ayla); Werkzeuge `power_on`, `brew_beverage` (optional `quantity_ml`), `get_beverages`, `get_profiles`, `stop_beverage`. Primär für Eletta Explore; Voraussetzungen nennen „PrimaDonna Soul, etc.“. Für die Soul ohne eigenen Test (Soul-Cloud-Befehle werden laut sk7n4k3d-Issue #50 teilweise ignoriert). Verwandt: synthe102/delonghi-mcp (Eletta Explore, 11 offene Issues).

### 2.9 Weitere, weniger relevante
- kasiom/ha-delonghi-coffeelink-eletta-explore (v1.3.2-beta.3, 03.11.2026) – nur Eletta Explore.
- sk7n4k3d/delonghi-daedalus-ha – nur „Eletta Ultra / My Coffee Lounge“ (andere Cloud, AWS IoT).
- lodzen/cremalink-ha (Fork, 2.0.0.dev7, 16.11.2026; 4 offene Issues) und lodzen/cremalink – lokaler Betrieb im Prozess, ohne Add-on; wird von Nutzern in sk7n4k3d-Issue #50 und actabi-Issue #74 als Alternative für die Soul empfohlen. Kein Long Black.
- Rsa76d/delonghi-primadonna-ble-mitm, Kannix2005/ha-delonghi-primadonna (BLE, v2.3.2, 10.11.2026), bubez81/delonghi-homekit (BLE/Homebridge), mmastrac/longshot (BLE, Rust, letzter Push 21.05.2025, nur an Dinamica Plus getestet: https://github.com/mmastrac/longshot), Barista (BLE-HTTP-Bridge, https://korben.info/en/barista-control-delonghi-coffee-machine-http.html): alle Bluetooth, Soul nicht genannt/nicht getestet.
- daedalus/mcp-delonghi-ecam (0.1.1, 10.04.2026) und FiveElements-Doku: Protokollreferenz; ältere Basis otto-dev/delonghi-coffee-link-python (2021), jakecrowley/AylaLocalAPI (2024, Ayla-LAN-Protokoll).

## 3. Arbuzov/home_assistant_delonghi_primadonna (Bluetooth, 130 Sterne) im Detail
- URL: https://github.com/Arbuzov/home_assistant_delonghi_primadonna – letzter Push 14.11.2026, Release 1.17.26 (14.11.2026), 130 Sterne, Apache-2.0, 44 offene Issues/PRs laut API (36 Issues + 8 PRs laut Seite).
- Weg: Bluetooth LE, dauerhafte BLE-Verbindung, MAC-Adresse nötig; „Delonghi devices support only one connection“ (die App geht dann nicht).
- Unterstützte Modelle laut README: ECAM 550.55 (inkl. .W), 510.55M, 650.85.MS, 650.55.MS EX:1, Dinamica Plus ECAM 370.85.SB/370.95, Maestosa EPAM 960.75.GLM. ECAM 610.74 / PrimaDonna Soul: NICHT genannt.
- Issues: #25 „Possible to add Delonghi ECAM 610.74.MB ?“ (offen; Autor: nur Bluetooth; Nutzer: „It's a wifi device“) und #53 „PrimaDonna Soul“ (offen; „So we can conclude this integration doesn't work with the soul“, Kommentare bis 28.07.2025). Long Black ist als Getränke-Name im Code vorhanden (`machine_entities.py`, `LONG_BLACK = "Long Black"`), aber wegen der Konnektivität für diese Maschine nutzlos.
- Fazit: nicht nutzbar für ECAM 610.74.

## 4. Fehlanzeigen (gesucht, nichts Brauchbares gefunden)
- ESPHome: nur BLE-Beispiel `myhomeiot-esphome-components/examples/ble_client/delonghi.yaml` (BLE, ältere Modelle) und Dedica-/Klima-Projekte; nichts für die Soul/WLAN.
- Node-RED: kein Paket gefunden. Homebridge: nur BLE-Plugin (bubez81/delonghi-homekit) und Klimageräte-Plugins. Android-App (Open Source) für die Soul: keine; nur Mitschnitt-Repo fireport/delonghi-dump (alter BLE-Mitschnitt). Die Hersteller-App „Coffee Link“ bietet laut Nutzern eine lokale LAN-Option (Hinweis im Homey-Thread, ungeprüft).

## 5. Empfehlung für ECAM 610.74 und „Long Black“

Rangfolge (Ziel: Programm auf Mac/Android/eigener Assistent):

1. Erste Wahl zum Ausprobieren: FiveElements/delonghi-coffeelink-local. Einziges Projekt, das 610.74.MB namentlich führt, Long Black (ID 25) im Katalog für dieses Modell hat und eine programmierbare Schnittstelle (REST `/api/command`, MCP `/mcp`) bietet; rein lokal. Risiken: Brühen laut README noch nicht live bestätigt, sehr junges Einzelprojekt, keine API-Authentifizierung, Node >= 26.
2. Wenn Home Assistant vorhanden ist und Long Black per Knopf/Dienst reichen soll: sk7n4k3d/delonghi-ha (v1.9.x; Rezepte aus der Maschine, `long_black` drin, Cloud + LAN) oder actabi/delonghi_coffeelink (`long_black`-Button, aber feste Standardparameter). Beide: Soul-Brühen hatte Probleme (#50 bei sk7n4k3d offen; actabi #74 Zähler).
3. Für einen robusten eigenen Client: Styx85/aioecam (Python, LAN) als Basis, den Long-Black-Frame (ID 0x19) selbst ergänzen, z. B. durch Mitschnitt der Coffee-Link-App (actabi bietet dafür den „Last Captured Command“-Sensor und `send_raw_command`) und Abgleich mit dem Rezeptformat in https://github.com/actabi/delonghi_coffeelink (Datei `command_builder.py`) und https://github.com/FiveElements/delonghi-coffeelink-local/tree/main/doc (`commandes-cafe.md` – im Repo nicht abgerufen, siehe unten).
4. Nicht geeignet: Arbuzov (BLE), Homey „Delonghi.Coffee“, longshot, Barista, ESPHome-BLE.

Ehrliche Lücke: Kein Projekt belegt, dass „Long Black“ auf einer Soul real gebrüht wurde. Vor einem Einsatz mit Nutzer-Erwartung „ein Kaffee per Programm“ sollte ein Praxistest laufen (Ein/Aus und Espresso gehen laut Styx85 über LAN nachweislich auf einer ECAM610.75; 610.74 ist dieselbe Familie, aber nirgends getestet).

Nicht geprüft / offen:
- FiveElements `doc/commandes-cafe.md` (Pfad lieferte 404; Dokumentation liegt unter `doc/`, genaue Dateinamen nicht geprüft).
- Ob ECAM 610.74.MB sich vom .75 im Protokoll unterscheidet (die Modelltabelle führt beide als PD_SOUL, WLAN, protocolVersion 2.1).
- Bluetooth-Steuerung der Soul (die Maschine hat BLE für die App-Kopplung); ob BLE-Befehle akzeptiert werden, ist nirgends belegt.
- Code des Homey-Projekts (kein Repo gefunden); De'Longhi-Produktseite (Konnektivität ECAM610.74.MB): https://www.delonghi.com/en-ie/p/primadonna-soul-ecam610.74.mb-primadonna-soul-automatic-coffee-maker/ECAM610.74.MB.html (nur in Suchergebnissen gesehen, nicht abgerufen).

## Quellenliste (Auswahl)
- https://github.com/FiveElements/delonghi-coffeelink-local (README, `src/lib/machine-catalogs.json`)
- https://github.com/actabi/delonghi_coffeelink (README, `const.py`, `catalog.py`, Issues #60/#74/#76)
- https://github.com/sk7n4k3d/delonghi-ha (README, `const.py`, Issues #34/#50)
- https://github.com/Styx85/cremaevolution-ha und https://github.com/Styx85/aioecam
- https://github.com/miditkl/cremalink (README, `cremalink/devices/ECAM610.json`, Issue #249) und https://github.com/miditkl/cremalink-ha
- https://github.com/lodzen/cremalink-ha
- https://github.com/chrvidal/ioBroker.coffeelink
- https://github.com/AClerbois/mcp-delonghi
- https://github.com/Arbuzov/home_assistant_delonghi_primadonna (README, Issues #25, #53)
- https://github.com/mmastrac/longshot
- https://community.homey.app/t/159352 und https://community.homey.app/t/app-pro-delonghi-coffee/80377

BEST-PRACTICES-KANDIDATEN:
- Soul/Coffee-Link-WLAN-Maschinen: LAN-Modus von Ayla hat vertauschte Rollen (Maschine verbindet sich zum lokalen Server; Firewall muss Maschine -> Server erlauben; Coffee-Link-App geschlossen halten; Cloud nur für LAN-Schlüssel). https://github.com/FiveElements/delonghi-coffeelink-local (README, Stand 13.11.2026, v0.1.5) und https://github.com/Styx85/cremaevolution-ha/blob/main/docs/ecam-lan-protocol.md (v0.3.9b1)
- Neue Sitzung nur per POST starten, PUT nur als Keep-Alive; Datapoints nach Property-Name routen (Add-on-Fehler vermeiden). https://github.com/Styx85/cremaevolution-ha (Abschnitt „Why a rewrite“, v0.3.9b1)
- Cloud-Zugriffsweg (Gigya -> JWT -> Ayla-Token -> `app_data_request`, EU/US getrennte Zugangsdaten, 180 Requests/Stunde-Budget). https://github.com/actabi/delonghi_coffeelink (v0.3.26), https://github.com/Styx85/aioecam (v0.1.7b1)
- Getränke-IDs der Soul (Long Black = 0x19 / ID 25, Mug to Go 0x1a, Brew Over Ice 0x1b, Cortado 0x18). https://github.com/actabi/delonghi_coffeelink/blob/main/custom_components/delonghi_coffeelink/const.py

BUG-KANDIDATEN:
- sk7n4k3d/delonghi-ha #50: PrimaDonna Soul (DL-millcore, Firmware ADA 1.5.3) – Brühbefehle über die Cloud werden angenommen (201), aber ignoriert; `d302_monitor` ist ohne App-Sitzung eingefroren; Fix über LAN-Pfad in v1.9.0 (17.11.2026), unbestätigt. Betrifft v1.6.0 bis v1.8.0. https://github.com/sk7n4k3d/delonghi-ha/issues/50
- sk7n4k3d/delonghi-ha #34 (v1.5.11): Soul Ein/Aus: `data_request` zuerst ohne app_id senden, dann mit (`("data_request", False)` vor `("data_request", True)`). https://github.com/sk7n4k3d/delonghi-ha/issues/34
- actabi/delonghi_coffeelink #74 (v0.3.26, 06.11.2026): Zähler der Soul seit 28.09.2026 in der Ayla-Cloud eingefroren. https://github.com/actabi/delonghi_coffeelink/issues/74
- actabi/delonghi_coffeelink #15 (laut README): Maschine ignoriert Befehle, wenn Sitzungs-ID/4-Byte-Gerätesignatur nicht stimmt (Cloud antwortet 200). Quelle: README https://github.com/actabi/delonghi_coffeelink (v0.3.26)
- miditkl/cremalink #249 (ECAM610.75): `is_busy`/`is_idle` flackern, weil von `action` abgeleitet; `is_watertank_empty` (alarms[0] Bit 0) ist wertlos. https://github.com/miditkl/cremalink/issues/249 (PR #250 offen; Release v0.1.0-beta.28)
- Styx85/cremaevolution-ha #36: LAN-Polling lässt das WLAN-Modul bei jedem Poll eine Cloud-TLS-Verbindung öffnen (v0.3.9b1). https://github.com/Styx85/cremaevolution-ha/issues/36
- Nach Neustart des WLAN-Moduls ohne Internet verweigert die Maschine den LAN-Modus, bis sie die Cloud erreicht hat. https://github.com/Styx85/cremaevolution-ha (README „Good to know“, v0.3.9b1)
- Arbuzov/home_assistant_delonghi_primadonna #25 und #53: ECAM 610.74/610.75 (WLAN) mit der BLE-Integration nicht nutzbar (Stand 1.17.26). https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/25
