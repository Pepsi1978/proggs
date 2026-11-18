# R6 – Praxis und Fallen: De'Longhi PrimaDonna Soul ECAM 610.74 steuern (Stand 18.11.2026)

Alle Aussagen mit Quelle. „Nicht belegt“ heißt: ich habe dazu keine Quelle gefunden. Eigene Schlussfolgerungen sind als solche markiert.

## 0. Wichtigster Befund: Bluetooth ist für die Soul NICHT belegt, der belegte Weg ist WLAN (Ayla, LAN-Modus + Cloud)

- Die Bluetooth-Projekte (bleak, longshot, Arbuzov-HA-Integration) wurden an Dinamica Plus, PrimaDonna Elite/Class (ECAM 550/650) getestet. Die Soul (610/612/613) steht in keiner BLE-Modell-Liste.
  - Arbuzov-README: unterstützte Modelle ECAM 550.55, Dinamica Plus 370.85/370.95, Maestosa EPAM 960.75, ECAM 650.85/650.55, ECAM 510.55M. Soul/610 nicht gelistet. https://github.com/Arbuzov/home_assistant_delonghi_primadonna
  - longshot: „nur an der Dinamica Plus über Bluetooth getestet“. https://github.com/mmastrac/longshot
  - Barista (Python/bleak): gebaut und getestet an Dinamica Plus; andere Modelle „evtl. kompatibel, unbestätigt“. https://korben.info/en/barista-control-delonghi-coffee-machine-http.html
- De'Longhi selbst nennt für die Soul nur WLAN: „connect your PrimaDonna Soul via Wi-Fi“, Bluetooth wird nicht erwähnt. https://delonghi.com/en-sg/faqs/How-to-connect-PrimaDonna-Soul-to-the-Coffee-Link-APP./a/575585
- Homey-Entwickler (Mai/Juni 2023): BLE-Maschinen nutzen ECAM-Protokoll v2, WLAN-Maschinen „aylanetworks sdk“; die Soul (ECAM612.55.SB) sei WLAN, er habe keine Hardware. Ein Nutzer (Okt. 2023) behauptete, die Soul habe auch Bluetooth; der Entwickler konnte das nicht bestätigen („rest of the domotica hubs don't support it“). https://community.homey.app/t/app-pro-delonghi-coffee/80377
- ioBroker-Forum: Ein Soul-Besitzer berichtet, die App laufe „mit Bluetooth allein und mit WLAN allein“ (eigene Einschätzung, nicht gründlich getestet, älterer Stand). https://forum.iobroker.net/post/630571 – schwache Quelle.
- Kein Soul-Projekt (cremalink, cremaevolution-ha, aioecam, delonghi_coffeelink) nutzt BLE; alle nutzen Ayla-LAN oder Ayla-Cloud. Eine funktionierende Soul-BLE-Steuerung habe ich nirgends gefunden.
- Schlussfolgerung (eigene): Die Frage „hat die 610.74 überhaupt ein per BLE ansteuerbares ECAM-Protokoll“ ist offen und lässt sich nur mit nRF Connect / bleak-Scan am echten Gerät klären (Wird die Maschine im Scan als „D17…“ o. ä. mit De'Longhi-Service gesehen?). Als Plan A ist BLE nicht zu empfehlen.

## 1. Bluetooth-Weg (falls die 610.74 BLE doch bietet) – Fallen

1. Nur eine BLE-Verbindung gleichzeitig: Arbuzov-README „Delonghi devices support only one connection. You could not connect to device using the native application if you use this integration.“ https://github.com/Arbuzov/home_assistant_delonghi_primadonna
2. Hängende Verbindung: „If HA loses connection, the machine may still think it's connected and may not accept a new one.“ (gleiche README). Maintainer in Issue #189: eine Maschine mit nur einer BLE-Verbindung kann „connects but nothing comes back“ erzeugen. https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/189 (ECAM 650.55, offen, Stand 14.11.2026, Integration v1.17.23, Timeouts nach 10 s bei jedem Befehl, auch per nRF Connect am Pixel 7; keine bestätigte Lösung).
3. Standby: Im Standby antwortet die Maschine nicht auf bestimmte Befehle. Issue #288 (ECAM 656.55.MS, 17.10.2026): Uhr-Sync 0xE2 im Standby ohne Antwort in 10 s, im Wachzustand Antwort nach 267 ms. https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/288 ; Issue #290: Profilnamen werden nach HA-Neustart im Standby zu Platzhaltern. https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/290
   - Aufwecken per BLE: In den README/Issues, die ich lesen konnte, nicht belegt (Arbuzov: „Wake from standby: not covered“). Power-Befehl 0x84 wird laut einem Beitrag in #189 nie beantwortet.
4. Der Maschine meldet immer nur einen Status zugleich (z. B. erst „Wassertank“, dann nicht „Satzbehälter“). Arbuzov-README.
5. Reichweite: Keine Messwerte zu De'Longhi gefunden. Nicht belegt. Praktisch (eigene Annahme): BLE nur im selben Raum; deshalb ESP32-Brücke nahe der Maschine.
6. Mac/bleak (CoreBluetooth): macOS zeigt keine MAC-Adresse, sondern eine pro-Mac-UUID; diese UUID ist je Rechner anders. Per Name/Service-UUID scannen, nie MAC fest verdrahten. `use_bdaddr` ist undokumentiert und fragil. https://bleak.readthedocs.io/en/stable/backends/macos.html
   - Geräte, die keine Service-UUID im Advertising haben, werden unter macOS nicht gefunden (Bleak-Doku/Issues, siehe gleiche Seite).
7. Home-Assistant-Bluetooth-Proxy:
   - Aktive Verbindungen (nötig für Kaffeemaschine) sind auf ESP32 Standard; maximal 3 gleichzeitige aktive Verbindungen. https://www.esphome.io/components/bluetooth_proxy.html
   - Shelly-Geräte als Proxy unterstützen keine aktiven Verbindungen, es muss ein echter ESP32 sein (Forum, 1. Jan. 2026). https://community.home-assistant.io/t/setting-up-coffee-machine-via-bluetooth/965738
   - Dort löste ein Nutzer das Problem „MAC nicht gefunden“ mit einem Raspberry Pi, der den BT-Request auf Netzwerk-Trigger hin stellt (27. Dez. 2025) – das ist faktisch eine „Brücke im WLAN“.
8. Ein ESPHome-BLE-Client-Beispiel für PrimaDonna Elite (ECAM 650.85.MS) existiert: https://git.sudo.is/home-assistant/myhomeiot-esphome-components/src/branch/main/examples/ble_client/delonghi.yaml (Inhalt konnte ich nicht abrufen; nur über Suchtreffer bekannt).
9. Protokoll-Fallen: Längenbyte zählt alles nach dem Startbyte inkl. Prüfsumme; CRC-16/CCITT; manche Befehls-IDs sind laut Autor „Vermutungen“; nach dem Brühen läuft eine Reinigung, vorher keine neuen Befehle. https://grack.com/blog/2022/12/02/hacking-bluetooth-to-brew-coffee-on-github-actions-part-2/
10. Android (eigene App): Zur Android-BLE-Spezifik (Reichweite, Koexistenz mit der offiziellen App) habe ich keine De'Longhi-spezifische Quelle. Nicht belegt. Es gilt dieselbe Ein-Verbindungs-Regel der Maschine.

## 2. WLAN-Weg (Ayla) – belegte Soul-Projekte

| Projekt | Modus | Soul-Status | Quelle |
|---|---|---|---|
| Styx85/cremaevolution-ha (früher Cremalink for HA) + aioecam | LAN lokal (Maschine verbindet sich per TCP 10280 zu HA), Cloud optional | „PrimaDonna Soul (ECAM610.75): einziges Modell mit Live-Test des lokalen Protokolls“; aioecam: ECAM610/612 | https://github.com/Styx85/cremaevolution-ha , https://github.com/Styx85/aioecam |
| cvanvliet/delonghi_coffeelink (Fork; auch rasmusbe) | Cloud (Gigya + Ayla) | ECAM610/612/613 „works out of the box“, 21 Getränke-Buttons, Wake/Standby | https://github.com/cvanvliet/delonghi_coffeelink |
| actabi/delonghi_coffeelink | Cloud | getestet nur ECAM 612.55.SB | https://community.home-assistant.io/t/delonghi-coffee-link-support/501813 |
| sk7n4k3d/delonghi-ha | Cloud, ab 1.9.0 auch LAN-Status/Brühen | Eletta, Dinamica, Primadonna | https://github.com/sk7n4k3d/delonghi-ha |
| miditkl/cremalink-ha | lokal (Add-on) oder Cloud | „primär für PrimaDonna Soul optimiert“ | https://github.com/miditkl/cremalink-ha |

Hinweis zum Modell: Die belegten Tests laufen an ECAM610.75 bzw. 612.55. Die 610.74 des Nutzers ist in keiner Quelle genannt; „ECAM610“ ist als Klasse (oem_model DL-millcore) angegeben (cvanvliet). Dass 610.74 identisch funktioniert, ist plausibel, aber nicht belegt.

### 2.1 Konflikt mit der offiziellen App (anders als bei BLE: gleiche Regel, anderer Mechanismus)
- Die Maschine bevorzugt die LAN-Sitzung; eine offene Coffee-Link-App hält eine LAN-Sitzung mit 30-s-Keep-alive, solange ignoriert die Maschine Cloud-Befehle. Workaround: App komplett schließen (aus „Zuletzt benutzt“ wischen). https://github.com/cvanvliet/delonghi_coffeelink , https://community.home-assistant.io/t/delonghi-coffee-link-support/501813
- Öffnet man die App wieder, übernimmt sie die Kontrolle (cvanvliet).
- Beim reinen LAN-Weg (cremaevolution/aioecam) ist der App-Konflikt in den gelesenen README-Teilen nicht beschrieben. Nicht belegt.

### 2.2 Aufwecken / Standby / Auto-Off
- Soul: „Wake“ und „Standby“ funktionieren direkt (cvanvliet). Bei Eletta Explore muss die Maschine erst einmal über die App eingeschaltet werden.
- cremaevolution: Ein/Aus und Status laufen lokal auch ohne Internet; im Standby wird alle 15 min abgefragt, Profilwechsel im Standby nicht möglich; Cloud ist „optionaler Fallback zum Aufwecken“, Mechanismus nicht beschrieben.
- cremaevolution 0.3.0b12: Ohne Internet zeigte ein einfaches Lesen nach dem Aufwachen weiter „Standby“; jetzt wird bei jedem Poll ein frischer Status angefordert (aioecam 0.1.0b9). 0.3.6b1: Setup scheitert nicht mehr, wenn der erste Statuslesevorgang in die Aufwach-Spülung fällt; 60-s-Polling beim Aufwachen. https://github.com/Styx85/cremaevolution-ha/blob/main/CHANGELOG.md
- Wichtig für „Long Black per Befehl“: Befehl muss warten, bis die Maschine „Ready“ ist (Spülung beim Aufwachen); delonghi-ha: Brühen nur im Zustand „Ready“. https://github.com/sk7n4k3d/delonghi-ha
- Auto-Off-Werte (u. a. 3 h) sind schreibbar (0.3.3b1). Auto-Off-Verhalten als Weckproblem: bleibt das WLAN-Modul im Standby/Auto-Off erreichbar? Für den LAN-Pfad belegt: Wake aus Standby ja; aus komplettem Auto-Off (Netzteil-Aus) nicht belegt.

### 2.3 Risiken Cloud-Weg (Kontosperre, Rate-Limits, API-Änderungen)
- Kontosperre: In keiner gelesenen Quelle (HA-Thread, Homey-Threads, cvanvliet, sk7n4k3d, cremaevolution) als Problem berichtet. Nicht belegt; Risiko aber nicht ausgeschlossen. Projekte speichern nur Refresh-Token statt Passwort (cremaevolution) bzw. nutzen die Anmeldedaten der App über Gigya (actabi, cvanvliet).
- Rate-Limit: Ayla erlaubt etwa 200 Anfragen pro Stunde; aioecam begrenzt auf 180/h je Anfrageart, Befehle gehen immer raus; Cloud-Session braucht Keep-alive etwa alle 4 min, sonst relaiet die Cloud keine Schreibbefehle. https://github.com/Styx85/aioecam ; delonghi-ha: „Possible (Ayla per-IP limits)“, Access-Token 24 h gültig, mit Refresh/Backoff. https://github.com/sk7n4k3d/delonghi-ha
- Cloud-Latenz 2–5 s je Befehl (delonghi-ha); cloud-Modus bei cremaevolution: Aktualisierung ca. 60 s.
- Stumme Fehler: „Commands can be accepted by the cloud with no error while the machine does nothing“ (nicht-Soul, cvanvliet); bei Eletta nur mit der Session-ID der App (cremaevolution 0.3.8b1).
- API-Änderungen: Die API-Schlüssel sind aus der Android-App ausgelesen („public app-level keys“); ändert De'Longhi sie, kann es brechen (delonghi-ha, nicht dokumentiert). Alle Projekte warnen, De'Longhi/Ayla könnten das Protokoll jederzeit ändern (cvanvliet, actabi). cremaevolution wertete Coffee-Link-App 4.9.6 aus und musste Rezeptfelder anpassen (0.3.0b9–b11: `temperature` entfernt, `milk` jetzt Dauer in 1/10 s, `intensity` 1–5) – Beleg, dass App-Versionen das Befehlsformat verschieben. https://github.com/Styx85/cremaevolution-ha/blob/main/CHANGELOG.md
- Firmware: Soul-Firmwarestring im Test „Millcore_demo 2.0.0 Jun 18 2020“ (wahrscheinlich Maschine, nicht WLAN-Modul). Konkrete Firmware-Updates, die Integrationen brachen, habe ich für die Soul nicht gefunden. Bei Eletta: Statistik-Zähler kommen je nach Firmware nur bei seltenen Boot-Ereignissen; Cloud-Monitor friert bei manchen Firmwares ein, solange die App geschlossen ist (delonghi-ha).
- Token-Rotation: 0.3.0b2: Entitäten blieben nach Cloud-Token-Rotation „unavailable“ (behoben).
- Offline-Grenze: Nach Neustart des WLAN-Moduls akzeptiert die Maschine lokale Verbindungen erst wieder, nachdem sie die Cloud erreicht hat; „Offline-Betrieb deckt nur kurze Ausfälle ab“ (cremaevolution). Das heißt: der „lokale“ Weg hängt für Setup (LAN-Key) und nach Neustart an der De'Longhi/Ayla-Cloud.
- Aktuelles offenes Thema: cremaevolution Issue #36 (16.11.2026): LAN-Polling lässt das WLAN-Modul bei jedem Poll eine Cloud-TLS-Verbindung öffnen (Label „needs more info“). https://github.com/Styx85/cremaevolution-ha/issues
- Projektreife: cremaevolution und aioecam sind Beta (Versionen 0.3.x-b / 0.1.x-b); „24-h-Soak-Test geplant, nicht erledigt“.
- Projekte sind jung und teils KI-gestützt (HA-Forum, 24.03.2026: Forderung nach Offenlegung von KI-Anteilen; actabi hat sie im Beitrag nicht). Quellcode vor Eingabe der De'Longhi-Zugangsdaten prüfen. https://community.home-assistant.io/t/delonghi-coffee-link-support/501813

### 2.4 Weitere dokumentierte Bugs (Soul / ECAM-WLAN)
- Soul-spezifisch: kein Wassertank-leer-Sensor; „Busy“ nur abgeleitet, weil die Maschine während eines Getränks „ready“ meldet; aktives Profil wird nicht gemeldet (nur zuletzt in HA gewähltes). https://github.com/Styx85/cremaevolution-ha , https://github.com/rasmusbe/delonghi_coffeelink
- Eletta Explore: Wake scheitert mit HTTP 404 „set_property data_request“ (actabi, 26.04.2026); jedes Getränk muss einmal in der App „angelernt“ werden (cvanvliet).
- Docker: Host-Netzwerk nutzen oder Port 10280 veröffentlichen; Port muss von der Maschine aus erreichbar sein (cremaevolution) – relevant, wenn der Dienst nicht im selben VLAN läuft.
- Homey (Sept. 2026): Soul ECAM610.75 – Pairing-Fehler „this.getManifest is not a function“ nach Update behoben; wiederkehrende „Timeout after 10000ms“. https://community.homey.app/t/159352

## 3. Rechtliches / Garantie (kurz, keine Rechtsberatung)
- Alle Projekte sind inoffiziell und „not affiliated with De'Longhi or Ayla“; Nutzung auf eigenes Risiko (cvanvliet, actabi, cremaevolution).
- Garantie: Ich habe keine De'Longhi-Aussage zu Drittsoftware/Garantie gefunden (Suche ohne Treffer). Nicht belegt. Es wird nur ein Befehl per vom Hersteller vorgesehener Funktion gesendet, aber die Maschine selbst wird nicht verändert; Risiko eher bei Konto/ToS.
- Nutzungsbedingungen: Ayla-ToS (2015) und -Datenschutz (2020) allgemein; konkrete Coffee-Link-Bedingungen nicht gefunden (De'Longhi-Seiten nennen teils „My Coffee Lounge“). https://www.aylanetworks.com/terms-of-service
- EU Data Act: Seit 12.09.2025 haben Nutzer vernetzter Produkte (inkl. Haushaltsgeräte) Anspruch auf Zugang zu „readily available data“, auch Weitergabe an Dritte; ob das für Steuerbefehle gilt, ist nicht belegt (betrifft Daten, nicht Steuerung). https://www.alston.com/en/insights/publications/2025/09/eu-data-act-connected-products

## 4. Architektur-Empfehlung (eigene Bewertung auf Basis der Quellen)

Rangfolge nach Robustheit für „Long Black per Befehl“ (Mac, Android, Sprachassistent):

1. Home Assistant (Raspberry Pi / Mini-PC / Docker, im selben WLAN/VLAN) mit lokalem Soul-Pfad (cremaevolution-ha + aioecam; Fallback cvanvliet-Cloud-Integration). Mac, Android-App und Sprachassistent rufen dann nur eine HA-REST-/Webhook-/Skript-Schnittstelle auf („Long Black“ als HA-Skript, das Wake, Warten auf Ready und Brühbefehl kapselt). Vorteil: eine einzige Verbindung zur Maschine, Zugangsdaten nur an einer Stelle, Reife und Community; Nachteil: HA-Betrieb, Beta-Software, App-Konflikt (App schließen).
2. Eigener kleiner Dienst (Python) mit aioecam im Heimnetz (Mac mini/Pi), der HTTP anbietet; gleiche Vorteile ohne HA, aber alle Wartung (Library ist Beta, AGPL-3.0) bei dir.
3. Direkter Cloud-Aufruf von jedem Client: schlechteste Wahl. Pro Client Login/Token/Rate-Limit (ca. 200 Anfragen/h), 2–5 s Latenz, Abhängigkeit von Gigya/Ayla-Schlüsseln und App-Versionen, und stumme Fehler möglich.
4. BLE-Direktsteuerung (Mac/bleak oder Android-App): nur, wenn ein Scan bestätigt, dass die 610.74 das ECAM-BLE-Protokoll spricht; dann besser mit festem ESP32 (ESPHome ble_client oder Proxy mit aktiven Verbindungen, kein Shelly) neben der Maschine als Brücke im WLAN statt von Mac/Handy direkt, weil (a) nur eine BLE-Verbindung möglich ist (kollidiert mit App und mit jedem zweiten Client), (b) Reichweite und Standby-Verhalten am festen Gerät kontrollierbar sind. Für die Soul fehlt dafür jede Praxisbelegung.

Empfohlene erste Schritte: (a) mit nRF Connect prüfen, ob die 610.74 per BLE sichtbar ist; (b) Router prüfen, ob die Maschine eine IP hat und TCP 10280 vom HA-Host erreichbar ist; (c) Test mit aioecam `status --watch`, `on`, `brew` (CLI vorhanden); (d) Rezeptfelder für Long Black an der 610.74 prüfen (Getränkeliste je Variante verschieden, nicht Gegenstand von R6).

## Quellen (Auswahl)
- https://github.com/Arbuzov/home_assistant_delonghi_primadonna (+ Issues #189, #288, #290)
- https://github.com/Styx85/cremaevolution-ha (+ CHANGELOG, Issues)
- https://github.com/Styx85/aioecam
- https://github.com/cvanvliet/delonghi_coffeelink
- https://github.com/actabi/delonghi_coffeelink
- https://github.com/sk7n4k3d/delonghi-ha
- https://github.com/miditkl/cremalink-ha (+ Discussion #5)
- https://community.home-assistant.io/t/delonghi-coffee-link-support/501813
- https://community.home-assistant.io/t/setting-up-coffee-machine-via-bluetooth/965738
- https://community.homey.app/t/app-pro-delonghi-coffee/80377 und https://community.homey.app/t/159352
- https://grack.com/blog/2022/12/02/hacking-bluetooth-to-brew-coffee-on-github-actions-part-2/
- https://bleak.readthedocs.io/en/stable/backends/macos.html
- https://www.esphome.io/components/bluetooth_proxy.html
- https://delonghi.com/en-sg/faqs/How-to-connect-PrimaDonna-Soul-to-the-Coffee-Link-APP./a/575585
- https://forum.iobroker.net/post/630571

BEST-PRACTICES-KANDIDATEN:
- Kaffeevollautomat per Smart-Home steuern: Nur eine Instanz hält die Verbindung (HA/Dienst), alle Clients gehen über dessen API; App schließen. Quelle: https://github.com/cvanvliet/delonghi_coffeelink , https://github.com/Arbuzov/home_assistant_delonghi_primadonna
- BLE-Proxy für aktive Verbindungen: echter ESP32 (ESPHome bluetooth_proxy, max. 3 aktive Verbindungen), kein Shelly. https://www.esphome.io/components/bluetooth_proxy.html , https://community.home-assistant.io/t/setting-up-coffee-machine-via-bluetooth/965738
- bleak auf macOS: pro-Rechner-UUID statt MAC, per Name/Service-UUID scannen. https://bleak.readthedocs.io/en/stable/backends/macos.html
- Ayla-Cloud: Request-Budget (~200/h, aioecam 180/h), Keep-alive ca. alle 4 min, Token 24 h mit Refresh. https://github.com/Styx85/aioecam , https://github.com/sk7n4k3d/delonghi-ha
- Brühbefehle erst im Zustand „Ready“ senden (Aufwach-Spülung abwarten). https://github.com/sk7n4k3d/delonghi-ha , CHANGELOG 0.3.6b1 https://github.com/Styx85/cremaevolution-ha/blob/main/CHANGELOG.md

BUG-KANDIDATEN:
- Arbuzov/home_assistant_delonghi_primadonna v1.17.23, Issue #189 (ECAM 650.55, offen): verbindet, aber alle Befehle Timeout 10 s; Ein-Verbindungs-Limit vermutet. https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/189
- Arbuzov Issue #288 (ECAM 656.55.MS, 17.10.2026): Uhr-Sync 0xE2 im Standby ohne Antwort (10 s Timeout); #290 Profilnamen nach HA-Start im Standby zurückgesetzt; #287 Status nur durch (standardmäßig deaktivierten) Device-Tracker aktualisiert. https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/288 , /290 , /287
- Arbuzov-README: Maschine hält Verbindung nach HA-Abbruch weiter für belegt (stale BLE-Verbindung), nur eine Verbindung, App blockiert. https://github.com/Arbuzov/home_assistant_delonghi_primadonna
- cremaevolution-ha Issue #36 (16.11.2026): LAN-Polling löst bei jedem Poll Cloud-TLS-Verbindung des WLAN-Moduls aus. https://github.com/Styx85/cremaevolution-ha/issues
- cremaevolution 0.3.0b2: Entitäten nach Cloud-Token-Rotation „unavailable“; 0.3.0b12: Standby-Anzeige nach Aufwachen ohne Internet; 0.3.0b9–b11: Rezeptfelder an Coffee-Link-App 4.9.6 angepasst (Breaking für brew_custom). https://github.com/Styx85/cremaevolution-ha/blob/main/CHANGELOG.md
- Coffee-Link-Cloud (cvanvliet): App-LAN-Sitzung (30-s-Keep-alive) blockiert Cloud-Befehle; stumme Annahme ohne Wirkung bei Nicht-Soul-Modellen. https://github.com/cvanvliet/delonghi_coffeelink
- actabi/delonghi_coffeelink an Eletta Explore: Wake HTTP 404 (26.04.2026). https://community.home-assistant.io/t/delonghi-coffee-link-support/501813
- Homey DeLonghi Coffee Link (Sept. 2026): Soul-Pairing „this.getManifest is not a function“ (behoben), „Timeout after 10000ms“. https://community.homey.app/t/159352
