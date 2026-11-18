# R2 – Bluetooth-LE-Protokoll der De'Longhi-ECAM-Maschinen (Stand 18.11.2026)

## 0. Kernergebnis in vier Punkten

1. Das öffentlich reverse-engineerte BLE-Protokoll (UUIDs, Paketformat 0x0D/0xD0 + CRC16, Befehle 0x75/0x83/0x84) ist gut belegt – aber nur für Maschinen mit BLE-Anbindung (Dinamica Plus, PrimaDonna Class/Elite, Maestosa, Primadonna S 510.55 u. a.).
2. Die **PrimaDonna Soul ECAM 610.74 ist laut De'Longhis eigenem App-Modellkatalog eine Wi-Fi-Maschine** (`connectionType: "Wi-Fi"`, `protocolVersion: 2`, `protocol_minor_version: 1`). Dort läuft die Steuerung nicht über das BLE-GATT-Profil, sondern über die Coffee-Link-/Ayla-Cloud (bzw. das Ayla-LAN-Protokoll). Der Nutzlast-Dialekt (0x0D-Frame, 0x83-Brew, 0x84-Power, CRC16, Long Black = Getränke-ID 0x19) ist derselbe, der Transport ein anderer (siehe Abschnitt 6).
3. Ein Beleg direkt von einer Soul (ECAM 610.75): Beim BLE-Verbinden mit der Arbuzov-Integration kommt `BleakError: Characteristic 00035b03-58e6-07dd-021a-08123a000301 not found!` – die Steuer-Characteristic existiert dort nicht. Ob die Soul überhaupt irgendeine nutzbare BLE-Steuerschnittstelle hat (die App nutzt BLE vermutlich nur zum Einrichten), ist **nicht belegt**.
4. **Long Black = Getränke-ID 25 (0x19)** – das steht in longshot, in De'Longhis App-Katalog (Eintrag der 610.74 enthält „Long Black“, id 25) und im Soul-Cloud-Projekt von actabi.

## 1. GATT-UUIDs (BLE-Maschinen)

| Element | UUID | Quelle |
|---|---|---|
| Service | `00035b03-58e6-07dd-021a-08123a000300` | longshot `ecam_bt.rs` (https://github.com/mmastrac/longshot/blob/main/src/ecam/ecam_bt.rs), grack Teil 1 (https://grack.com/blog/2022/12/01/hacking-bluetooth-to-brew-coffee-on-github-actions-part-1/), Arbuzov `const.py` (SERVICE) |
| Characteristic (Schreiben + Notify/Indicate) | `00035b03-58e6-07dd-021a-08123a000301` | dieselben Quellen |
| Gerätename | Standard `0x2A00` (nur zum Auslesen des Namens) | Arbuzov `const.py` (NAME_CHARACTERISTIC) |

- Eigenschaften der Characteristic: `READ | WRITE | INDICATE` (grack Teil 1; longshot `connect()` hart codiert `WRITE | READ | INDICATE`).
- Schreibtyp: longshot nutzt `WriteType::WithResponse` mit Kommentar „We must set WithResponse to get data back from the device“ (`ecam_bt.rs`). grack Teil 1 schrieb den Brew-Befehl dagegen mit `WithoutResponse`. Widerspruch in den Quellen; `WithResponse` ist die später im gepflegten Projekt (longshot 0.1.13) verwendete Variante.
- Scan: longshot scannt mit `ScanFilter { services: [SERVICE_UUID] }` (Advertisement des Service-UUID). Kein fester Gerätename belegt.
- Antworten kommen als Notifications/Indications; aktivieren per `subscribe`.

## 2. Paketformat

Belegt in longshot `packet.rs` (https://github.com/mmastrac/longshot/blob/main/src/protocol/packet.rs), grack Teil 2 (https://grack.com/blog/2022/12/02/hacking-bluetooth-to-brew-coffee-on-github-actions-part-2/) und Arbuzov `DEBUG_NOTES.md`.

```
[Start] [Länge] [Befehls-ID] [0x0F | 0xF0] [Nutzdaten …] [CRC_hi] [CRC_lo]
```

- Start: `0x0D` = App → Maschine, `0xD0` = Maschine → App.
- Länge = Gesamtlänge des Pakets minus 1 (= Anzahl der Bytes nach dem Startbyte inkl. Längen- und CRC-Bytes). longshot: `len = nutzdaten.len() + 3`, wobei Nutzdaten = Befehls-ID + Folgebytes.
- Byte 3 (nach der Befehls-ID): `0xF0` bei Befehlen, auf die eine Antwort erwartet wird, `0x0F` bei Befehlen ohne Antwort. longshot `request/mod.rs`: `if self.is_response_required() { 0xf0 } else { 0x0f }`; ohne Antwort sind AppControl (0x84) und MonitorV0/V1/V2 (0x75), StatisticsRead.
- CRC: CRC-16/SPI-FUJITSU alias AUG-CCITT, Polynom `0x1021`, Startwert `0x1D0F`, keine Reflektion, Big Endian, über alle Bytes außer den letzten zwei (longshot: „equivalent to the CRC_16_SPI_FUJITSU definition (initial 0x1d0f, poly 0x1021)“; Arbuzov: `crc_hqx(packet[:-2], 0x1D0F)`; actabi `crc16_aug_ccitt`). Von mir nachgerechnet und bestätigt (Python), siehe Beispiele.
- Hinweis: Das Projekt Rsa76d/delonghi-primadonna-ble-mitm (https://github.com/Rsa76d/delonghi-primadonna-ble-mitm) nennt für die ECAM 656.75 „Kontrollsumme = XOR aller Bytes XOR 0x55“; das widerspricht allen anderen Quellen und ist dort nicht belegt – nicht verwenden.
- Mehrere Pakete: Die Characteristic liefert Pakete über mehrere Notifications (longshot hat `packet_stream.rs` zum Zusammensetzen); MTU wird in longshot nicht explizit ausgehandelt.

## 3. Befehls-IDs (longshot `hardware_enums.rs`, Enum `EcamRequestId`)

| Dez | Hex | Name | Zweck |
|---|---|---|---|
| 17 | 0x11 | SetBtMode | – |
| 96/112 | 0x60/0x70 | MonitorV0/V1 | ältere Statusformate |
| 117 | 0x75 | MonitorV2 | Statusabfrage (einziges getestetes Monitor-Format) |
| 131 | 0x83 | BeverageDispensingMode | Getränk brühen / abbrechen / Profil sichern |
| 132 | 0x84 | AppControl | (2,1) = Einschalten, (1,1) = Standby (nur in Arbuzov per Probing, nur für Allow-List) |
| 144/149/161 | 0x90/0x95/0xA1 | Parameter schreiben/lesen | Einstellungen |
| 162 | 0xA2 | StatisticsRead | Zähler |
| 164 | 0xA4 | ProfileNameRead | Profilnamen |
| 166 | 0xA6 | RecipeQuantityRead | Standardrezept lesen |
| 169 | 0xA9 | ProfileSelection | Profil wechseln |
| 170 | 0xAA | RecipeNameRead | Rezeptnamen |
| 176 | 0xB0 | RecipeMinMaxSync | Min/Max je Rezept |
| 226 | 0xE2 | SetTime | Uhrzeit setzen |

## 4. Konkrete Befehle mit Byte-Beispielen

Alle Hex-Pakete unten habe ich selbst mit CRC16 (init 0x1D0F, poly 0x1021) nachgerechnet; „verifiziert“ heißt: Prüfsumme stimmt mit der in der Quelle genannten überein.

### 4.1 Aufwecken / Einschalten

- Nutzdaten `84 0F 02 01`, ganzes Paket: **`0d 07 84 0f 02 01 55 12`** (verifiziert).
- Quellen: longshot `app_control.rs` („(2, 1) for turn on“, `AppControl::TurnOn => [2, 1]`), Arbuzov `const.py` (`BYTES_POWER = [0x0d,0x07,0x84,0x0f,0x02,0x01,0x55,0x12]`) und `DEBUG_NOTES.md` (`TX 0d 07 84 0f 02 01 55 12 power on`, Mitschnitt vom 17.10.2026 an einer ECAM 656.55.MS).
- Die Maschine antwortet **nicht** mit einem 0x84-Paket (Arbuzov `DEBUG_NOTES.md`: „never answered with a 0x84 frame“); danach kommt ein Fremdframe (0x70) und der 0x75-Statusstrom. Power-Befehl daher nicht auf Antwort warten.
- Standby: `0d 07 84 0f 01 01 00 41` (verifiziert; mit dem Hinweis in Arbuzov: nur für eine Allow-List von Modellen bestätigt, bei Dinamica Plus 370.95 nicht).
- Vor dem Brühen nur im Zustand „Ready“ (State 7); longshot lehnt sonst ab und wartet nach TurnOn auf `Ready`.

### 4.2 Statusabfrage (MonitorV2, 0x75)

- Anfrage longshot-Stil: **`0d 05 75 f0 c4 d5`** (grack Teil 2, verifiziert); Arbuzov-Stil: `0d 05 75 0f da 25` (verifiziert; Byte 3 = 0x0F).
- Antwort (19 Byte): `d0 12 75 0f` + 15 Byte Daten + CRC. Aufbau laut longshot `monitor.rs` (`MonitorV2Response`), gerechnet ab Datenbeginn nach `d0 12 75 0f`:
  - Byte 0: Zubehör (`EcamAccessory`: 0 keins, 1 Wasser, 2 Milch, 3 Schoko, 4 Milch-Reinigung)
  - Byte 1–2: Schalter-Bitmaske (u16 LE): Bit 0 Wasserhahn, 1/2 Motor, 3 **Satzbehälter** (`CoffeeWasteContainer`), 4 **Wassertank fehlt** (`WaterTankAbsent`), 5 Knob, 6 **Wasserstand niedrig** (`WaterLevelLow`), 7 Kaffeekanne, 13 Tür offen, 14 Vormahl-Fach offen
  - Byte 3–4: Alarm-Bitmaske (u16 LE): 0 **EmptyWaterTank**, 1 **CoffeeWasteContainerFull**, 2 DescaleAlarm, 3 ReplaceWaterFilter, 5 CoffeeBeansEmpty, 11 EmptyDripTray u. a.
  - Byte 5: Maschinenzustand (`EcamMachineState`): 0 StandBy, 1 TurningOn, 2 ShuttingDown, 4 Descaling, 5 SteamPreparation, 6 Recovery, **7 ReadyOrDispensing**, 8 Rinsing, 10 MilkPreparation, 11 HotWaterDelivery, 12 MilkCleaning, 16 ChocolatePreparation
  - Byte 6: Fortschritt (Stufe), Byte 7: Prozent (0–100)
  - Rest unbekannt/immer 0.
- Beispiele aus Arbuzov `DEBUG_NOTES.md` (Elite/Dinamica, nicht Soul):
  - `d0 12 75 0f 01 05 00 00 00 07 00 00 00 00 00 00 00 9d 61` = alles gut/bereit
  - `d0 12 75 0f 01 15 00 00 00 07 …` = Wassertank fehlt
  - `d0 12 75 0f 01 0d 00 00 00 07 …` = Satzbehälter fehlt
  - `d0 12 75 0f 01 01 00 00 00 00 03 00 …` = Standby
- Achtung: Die Arbuzov-Integration und das Fork Kannix2005/ha-delonghi-primadonna (https://github.com/Kannix2005/ha-delonghi-primadonna) widersprechen sich bei den Byte-Positionen (Kannix beschreibt „fundamentally incorrect byte parsing“ der Originalintegration und folgt longshot). Für eigene Implementierung longshot als Referenz nehmen.

### 4.3 Getränk brühen (0x83)

Nutzdaten laut longshot (`request/mod.rs` `BeverageDispensingMode`):

```
83 F0 | <Getränke-ID> | <Trigger> | { <Zutat-ID> <Wert 1 oder 2 Byte> } … | <Modus>
```

- Trigger: 1 = Start, 2 = Stop/Abbruch (`EcamOperationTrigger`).
- Modus (`EcamBeverageTasteType`): 2 = Prepare, 6 = PrepareInversion, 3/7 = PrepareAndSave(+Inversion).
- Zutaten-IDs (`EcamIngredients`): 0 Temp (1 Byte), **1 Coffee (2 Byte, ml)**, 2 Taste (1 Byte, 0 Vormahl … 3 Normal … 5 ExtraStark), 4 Blend, 9 Milk (2 Byte), 12 Inversion (1 Byte), **15 HotWater (2 Byte, ml)**, 24 Programmable, 25 Visible, 27 IndexLength, 28 Accessorio (1 Byte). Zwei-Byte-Werte Big Endian.
- Verifizierte Referenzpakete:
  - longshot-Test `test_brew_coffee`: Regular Coffee, Coffee=103, Taste=2, Temp=0, Modus PrepareInversion → Nutzdaten **`83 f0 02 01 01 00 67 02 02 00 00 06`**. Die Arbuzov-Konstante `COFFE_ON` entspricht dem plus CRC: `0d 0f 83 f0 02 01 01 00 67 02 02 00 00 06 77 ff` (verifiziert).
  - Arbuzov `AMERICANO_ON`: `0d 12 83 f0 06 01 01 00 28 02 03 0f 00 6e 00 00 06 47 8b` = Americano (ID 6), Kaffee 40 ml, Taste 3, Heißwasser 110 ml, Temp 0, Modus 6 (verifiziert; von mir neu gerechnet identisch).
  - Arbuzov `HOTWATER_ON`: `0d 0d 83 f0 10 01 0f 00 fa 1c 01 06 04 b4` (Heißwasser, ID 16, 250 ml, Accessorio 1).
  - Abbruch: `0d 08 83 f0 07 02 06 2f 41` (grack Teil 2, Cappuccino stoppen, verifiziert) bzw. Arbuzov `COFFE_OFF = 0d 08 83 f0 02 02 06 c4 b1` (verifiziert).
  - Antwort auf Brew: `d0 07 83 f0 01 00 64 d9` (longshot-Testvektor `RESPONSE_BREW_RECEIVED`).
- Das Cappuccino-Beispiel `0d1483f007010100410900be02030c001c0206dc` aus der Seitenzusammenfassung von grack Teil 2 besteht meine CRC-Prüfung **nicht** (vermutlich Abschreibfehler in der von mir gelesenen Zusammenfassung). Nicht als Referenz verwenden; Vorlage dafür bei Bedarf direkt im Blog nachlesen.

### 4.4 Getränke-IDs (longshot `EcamBeverageId`)

| ID | Getränk | | ID | Getränk |
|---|---|---|---|---|
| 1 | EspressoCoffee | | 16 | **HotWater** |
| 2 | **RegularCoffee** (Kaffee) | | 17 | Steam |
| 3 | LongCoffee | | 19 | Ristretto |
| 4 | EspressoCoffee2X | | 20 | LongEspresso |
| 5 | DoppioPlus | | 22 | Tea |
| **6** | **Americano** | | 23 | CoffeePot |
| 7 | Cappuccino | | 24 | Cortado |
| 8 | LatteMacchiato | | **25 (0x19)** | **LongBlack** |
| 9 | CaffeLatte | | 26 | TravelMug (Mug to Go) |
| 10 | FlatWhite | | 27 | BrewOverIce |
| 12 | HotMilk | | 230–239 | Custom01–Custom10 |

(Quelle: https://github.com/mmastrac/longshot/blob/main/src/protocol/hardware_enums.rs; die ID 25 = Long Black bestätigt außerdem actabi `const.py` `(0x19, "long_black", "Long Black")` und De'Longhis App-Katalog, siehe Abschnitt 6.)

### 4.5 Long Black – was belegt ist und was nicht

- Belegt: ID 0x19 = 25. Der App-Katalog (Eintrag ECAM 610.74.MB) führt „Long Black“ mit den Zutaten `["1","2","4","12","15","24","25","27"]` = Coffee, Taste, Blend, Inversion, HotWater, Programmable, Visible, IndexLength.
- Aus dem Soul-Fixture von actabi (`tests/fixtures/soul_recipes_full.json`, Property `d019_rec_long_black`, Base64 → `d0 25 b0 f0 19 08 00 00 01 18 00 01 01 01 00 14 00 28 00 b4 1b 00 01 04 02 00 04 05 04 00 00 00 19 00 01 01 97 21`, CRC stimmt): enthält für Zutat 0x01 (Coffee) die Tripel min 20 / Standard 40 / max 180 ml (meine Dekodierung nach longshot `RecipeMinMaxInfo`; Standard 40 ml, erste Bytes vor 0x01 nicht sicher gedeutet).
- **Kein echter, mitgeschnittener BLE-Frame für Long Black** in einer öffentlichen Quelle gefunden. Das einzige Long-Black-Frame, das ich gefunden habe, ist der Cloud-Frame der Soul (siehe unten) – dessen Rezeptparameter weichen vom BLE-Format ab.
- Ableitung (ungeprüft, nur Rechenbeispiel, nicht an Maschine getestet): Nach dem Muster des verifizierten Americano-Frames würde ein BLE-Long-Black so aussehen: Nutzdaten `83 f0 19 01 | 01 00 28 | 02 03 | 0f 00 78 | 00 00 | 06`, ganzes Paket `0d 12 83 f0 19 01 01 00 28 02 03 0f 00 78 00 00 06 ac 53`. Abbruch: `0d 08 83 f0 19 02 06 77 23`. Das ist **ungeprüft**; die tatsächlich akzeptierten Zutaten (z. B. ob Inversion 0x0C nötig ist) hängen vom Rezept der Maschine ab. Sicherer Weg: das Rezept zuerst mit `RecipeQuantityRead` (0xA6) / `RecipeMinMaxSync` (0xB0) von der Maschine lesen, wie longshot es tut (`brew.rs`), und das Standardrezept zurückschicken.

## 5. Pairing / Authentifizierung / Schlüssel

- **Kein Pairing, Bonding oder Schlüssel für die Steuerbefehle belegt.** longshot `connect()` ruft nur `peripheral.connect()` auf, ohne Pairing-Logik (Auswertung von `ecam_bt.rs`). grack Teil 1 erwähnt kein Pairing/PIN.
- Homey-Forum (https://community.homey.app/t/app-pro-delonghi-coffee/80377, Beitrag 12 vom 03.10.2023, j.tops): „The pin is not used by the coffeemaker“ für den Verbindungsaufbau; die in der Maschinenkonfiguration (`pin_settings: true`) vorgesehene PIN betrifft evtl. nur das Ändern von Einstellungen (PinSet 0xB1 / PinRead 0xD2 im Protokoll).
- **Nur eine Verbindung gleichzeitig:** Arbuzov README: „Delonghi devices support only one connection. You cannot connect to the device using the native application while this integration is active.“ Außerdem: die Maschine merkt einen Verbindungsabbruch teilweise nicht („may still think it's connected“).
- Praxis-Falle (grack Teil 1): Die offizielle App findet die Maschine oft nicht und verbindet sich nach einem Verbindungsabbruch evtl. erst nach Löschen der App-Daten wieder; Verbindungsaufbau per nRF Connect funktionierte trotzdem.

## 6. Gilt das für die PrimaDonna Soul ECAM 610.74?

Belege, nach Stärke:

1. **De'Longhis App-Modellkatalog** (in Arbuzov `MachinesModels.json`, 117 Maschinen, Katalogversion 1.510, https://github.com/Arbuzov/home_assistant_delonghi_primadonna/blob/master/custom_components/delonghi_primadonna/MachinesModels.json): Alle ECAM-610/612-Einträge (610.35, 610.55, 610.74, 610.75, 612.55) haben `connectionType: "Wi-Fi"`, `protocolVersion: 2`, `protocol_minor_version: 1`, `pin_settings: false`. Die BLE-Maschinen (ECAM 370.95, 650.85, 656.75, 550.x …) haben `connectionType: "BT"`, `protocolVersion: 2` (656.75.MS: 1), `protocol_minor_version: 0`, `pin_settings: true`. Statistik im Katalog: 87 BT, 17 Wi-Fi, 13 NC. Eintrag 610.74.MB: 18 Standardrezepte, 6 eigene Rezepte, 5 Profile, `appModelId: "PD_SOUL"`.
2. **Issue #56 Arbuzov** (https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/56, 04.08.2023, ECAM 610.75): `BleakError: Characteristic 00035b03-58e6-07dd-021a-08123a000301 not found!` – das BLE-Steuerprofil ist auf der Soul nicht vorhanden.
3. **Issue #53 Arbuzov** (…/issues/53, 2023–2025; ECAM 610.75): Maschine reagiert nach dem Verbinden auf nichts; Maintainer: „This integration supports bluetooth device only.“ Nutzer: „Bluetooth is needed to connect the app, but the final connection is via Wi-Fi“ (Styx85, 28.05.2023) – Forennutzerangabe, nicht technisch belegt.
4. **Issue #25 Arbuzov** (…/issues/25, ECAM 610.74.MB, 21.11.2022–22.04.2026): Soul ist „a wifi device“; am 22.04.2026 schreibt actabi: „your ECAM 610.74.MB (PrimaDonna Soul) actually communicates through the Coffee Link cloud (Ayla Networks) rather than BLE“ und verweist auf https://github.com/actabi/delonghi_coffeelink.
5. **actabi/delonghi_coffeelink** (https://github.com/actabi/delonghi_coffeelink): bestätigt funktionierend an PrimaDonna Soul ECAM610.xx/612.xx/613.xx (`oem_model = DL-millcore`). Die Befehlsframes dort nutzen **denselben Frame-Dialekt**: Startbyte `0x0D`, `0x83 0xF0 <bev_id> <action> …`, CRC16 AUG-CCITT (init 0x1D0F, poly 0x1021) – zusätzlich folgt dem CRC ein 4-Byte-Zeitstempel bzw. eine Session-Kennung, der Transport ist die Ayla-Cloud (Datapoint-Property, Base64). Power: Familie `84 0f`, Params `02 01` (Wake) bzw. `01 01` (Standby). Details der Cloud-Anbindung sind Thema von R-Cloud, nicht R2.
6. **Homey** (https://community.homey.app/t/delonghi-espresso-primadonna-soul/158913, 29.08.2026, Antoine_Chaulieu): Die Homey-App für die Soul „communicates with the machine through the De'Longhi cloud“; Long Black wird dort nicht erwähnt.
7. **Homey Delonghi.Coffee** (…/80377, 11.05.2023): „These machines are using Ble and version 2 of the delonghi Ecam protocol“ – die Soul steht nicht in der unterstützten Liste; j.tops (02.06.2023): „a wifi connected machine“, App „uses BLE to communicate with the coffee makers“, Wi-Fi-Modelle nutzen das Ayla-SDK.
8. Gegenstimme (unbelegt): AlCalzone im ioBroker-Issue (https://github.com/ioBroker/AdapterRequests/issues/664, 13.09.2021): „(Zumindest meine Maschine) kann per BLE angesteuert werden“ – gemeint ist offenbar seine eigene Maschine in diesem Soul-Thread; Modell und Beleg fehlen, es gibt keinen veröffentlichten Code dazu.

Schlussfolgerung (nach Beleglage): **Das BLE-GATT-Protokoll (Service 00035b03…) ist für die ECAM 610.74 nicht belegt und laut Issue #56 am Schwestermodell ECAM 610.75 nicht vorhanden.** Der Nutzdaten-Dialekt (0x83/0x84, Getränke-ID 0x19 für Long Black) darf als übertragbar gelten, wird aber auf der Soul über Cloud/LAN statt über GATT transportiert. Für die Soul bleibt als Weg Coffee-Link-Cloud (Ayla) oder das Ayla-LAN-Protokoll; BLE nur zur Ersteinrichtung (Vermutung, nicht belegt). Prüfung an der echten Maschine: mit nRF Connect / LightBlue scannen und nachsehen, ob der Service `00035b03-…0300` angeboten wird – das kostet zwei Minuten und beendet die Unsicherheit.

## 7. Weitere genannte Projekte (BLE-Maschinen)

| Projekt | Modelle | Hinweis |
|---|---|---|
| mmastrac/longshot (Rust, https://github.com/mmastrac/longshot) | „getestet nur Dinamica Plus ECAM 370.95“ | Referenzimplementierung, README-Aussage „has only been tested on the Dinamica Plus over Bluetooth“ |
| Arbuzov/home_assistant_delonghi_primadonna | ECAM 550.55, Dinamica Plus 370.85/370.95, Maestosa EPAM 960.75, ECAM 650.85, 650.55, 510.55 | HACS, 130 Sterne, letzter Push 14.11.2026; Soul ausdrücklich nicht unterstützt |
| Kannix2005/ha-delonghi-primadonna | Primadonna BLE | Protokoll-Rewrite nach longshot, Push 10.11.2026 |
| manekinekko/cafy (Node, https://github.com/manekinekko/cafy) | Primadonna Elite ECAM65075MS | letzter Push 18.08.2021; Seite per Fetch nicht lesbar (403) |
| Rsa76d/delonghi-primadonna-ble-mitm | ECAM 656.75 | ESP32-C6 als BLE-MITM zwischen Maschine und App; abweichende Checksum-Angabe (siehe oben) |
| Barista (Python BLE→HTTP, laut https://korben.info/en/barista-control-delonghi-coffee-machine-http.html) | getestet nur Dinamica Plus | Repo nicht gefunden/lesbar (Korben-Seite 403), daher nicht ausgewertet |

## 8. Offene Punkte / Empfehlung für den Nutzer

- Frage „Bietet meine ECAM 610.74 den GATT-Service 00035b03…0300 an?“ ist per Beleg nur indirekt beantwortet (Katalog sagt Wi-Fi, Soul 610.75 hat die Characteristic nicht). Ein BLE-Scan an der Maschine selbst liefert die Gewissheit.
- Für den Zielfall (Long Black programmatisch starten) ist die Beleglage für die Soul: Getränke-ID 25 und Frame-Dialekt gesichert, Transport = Cloud/LAN. Ein echter Long-Black-Frame der Soul wurde nicht veröffentlicht gefunden; die Rezeptparameter müssen aus dem Rezept-Datapoint `d019_rec_long_black` bzw. per Mitschnitt gewonnen werden.
- Falls doch eine BLE-Maschine (Elite/Dinamica) im Spiel wäre: Ablauf = verbinden → Notify aktivieren → `0d 07 84 0f 02 01 55 12` → auf Status 7 (Ready) warten (Statusabfrage `0d 05 75 f0 c4 d5`) → Rezept lesen (0xA6) → Brew-Frame mit ID 25 senden → Fortschritt per 0x75 beobachten.

BEST-PRACTICES-KANDIDATEN:
- BLE-Steuerung De'Longhi ECAM (BLE-Maschinen): Service `00035b03-58e6-07dd-021a-08123a000300`, Characteristic `…0301`, Frame 0x0D/0xD0 + Länge (Gesamt−1) + CRC16 AUG-CCITT (init 0x1D0F, poly 0x1021, BE), Power-on `0d 07 84 0f 02 01 55 12` (nicht auf Antwort warten), Status 0x75, Brew 0x83; WithResponse schreiben; nur eine Verbindung gleichzeitig. Quellen: https://github.com/mmastrac/longshot (longshot 0.1.13, Docs.rs), https://github.com/Arbuzov/home_assistant_delonghi_primadonna (Stand 14.11.2026, DEBUG_NOTES.md), https://grack.com/blog/2022/12/02/hacking-bluetooth-to-brew-coffee-on-github-actions-part-2/
- Vor jeder Gerätewahl in den App-Modellkatalog sehen: Feld `connectionType` (BT/Wi-Fi/NC), `protocolVersion`, `protocol_minor_version` je Modell; Datei `MachinesModels.json` (Katalogversion 1.510) in https://github.com/Arbuzov/home_assistant_delonghi_primadonna. PrimaDonna Soul ECAM 610/612 = Wi-Fi, Elite 650/656 und Dinamica Plus 370 = BT.
- Getränke-ID-Tabelle (Long Black = 25 = 0x19, Americano = 6, Kaffee = 2, Heißwasser = 16) aus longshot `hardware_enums.rs`; wird vom App-Katalog und von actabi/delonghi_coffeelink (https://github.com/actabi/delonghi_coffeelink) bestätigt.

BUG-KANDIDATEN:
- De'Longhi PrimaDonna Soul (ECAM 610.74/610.75): `BleakError: Characteristic 00035b03-58e6-07dd-021a-08123a000301 not found!` – BLE-Integrationen (Arbuzov home_assistant_delonghi_primadonna, Issue #56 vom 04.08.2023, https://github.com/Arbuzov/home_assistant_delonghi_primadonna/issues/56) funktionieren nicht an Wi-Fi-Maschinen; Soul braucht Coffee-Link-Cloud (Ayla), z. B. actabi/delonghi_coffeelink (Stand Release 2026, https://github.com/actabi/delonghi_coffeelink).
- De'Longhi BLE-Maschinen erlauben nur eine Verbindung: Solange eine Integration (Arbuzov, Stand 14.11.2026) verbunden ist, funktioniert die offizielle Coffee-Link-App nicht; nach Verbindungsabbruch hält die Maschine die Verbindung evtl. für aktiv (README, https://github.com/Arbuzov/home_assistant_delonghi_primadonna).
- Power-Befehl 0x84 wird nie mit 0x84-Frame beantwortet (Arbuzov DEBUG_NOTES.md, Mitschnitt 17.10.2026, ECAM 656.55.MS): Wer auf eine gleichnamige Antwort wartet, läuft in den Timeout; Befehl ohne Wartezeit senden und den 0x75-Status abwarten. Ebenso: 0xA4 (Profilliste) und 0xE2 (Uhr) werden in Standby nicht beantwortet.
- Inkonsistente Beschreibungen des Status-Frames (0x75): Arbuzov-Originalparser vs. longshot/Kannix2005-Fork (Kannix2005/ha-delonghi-primadonna, Push 10.11.2026): Schalter/Alarm/Zustand-Offsets unterschiedlich beschrieben; longshot (`monitor.rs`) als Referenz verwenden.
- grack-Teil-1 schreibt mit `WriteType::WithoutResponse`, longshot 0.1.13 verlangt `WithResponse`, „to get data back from the device“.
