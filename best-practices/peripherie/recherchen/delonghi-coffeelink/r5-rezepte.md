# R5 – Getränke und Rezepte: De'Longhi PrimaDonna Soul ECAM 610.74 (Stand 18.11.2026)

Kennzeichnung: **[Beleg]** = direkt in der Quelle gelesen, **[Schluss]** = eigene Ableitung aus Quellen, **[offen]** = nicht belegbar gefunden.

## 1. Getränkeliste der ECAM 610.74

**Produktseite** (https://www.delonghi.com/en/ecam610-74-mb-primadonna-soul-automatic-coffee-maker/p/ECAM610.74.MB) [Beleg]:
- Heiße Kaffeegetränke: Americano, Coffee, Coffee Pot, Doppio+, Espresso, Long, **Long Black**, Over Ice
- Milchgetränke: Caffelatte, Cappuccino, Cappuccino Mix, Cappuccino +, Cortado, Espresso Macchiato, Flat White, Hot Milk, Latte Macchiato
- Sonstiges: Hot Water, Mug To Go, Tea Function
- „5 user profiles“, Rezepte anlegen/anpassen „Yes through App“ (Coffee Link), Bean Adapt, 4,3"-TFT-Touch.
- Auf der Seite nicht erwähnt: Bluetooth, Wi-Fi, Favoriten, Tassensensor.

**Händler-Datenblatt (FR)** (https://media1.coffee-webstore.com/img/cms/Fiche%20produit/Machine%20Delonghi/PRIMADONNA/soul_610-74-mb/fche_produit_610-74-mb.pdf) [Beleg]:
- „21 boissons dont : eau chaude et 4 nouvelles recettes : Long Black, Brew Over Ice, Cortado & Mug à emporter“.
- „3 modes d'utilisation : direct, personnalisé, profils“, „5 profils utilisateurs“.
- „Machine connectée avec application dédiée“, Espresso „Soul“ über die App-Funktion Bean Adapt.

**Offizielle Bedienungsanleitung** (EN, Dokument 57132C8859_01_0325, gilt für „ECAM61X.3Y – 61X.5Y – 61X.7Y“, also auch .74): https://dls.delonghigroup.com/files/docs/5/EN-57132C8859.pdf [Beleg]
- Abschnitt „Beverage quantity table“ (Kap. 14), Zeile **„LONG BLACK 200 ml Standard, 90–480 ml programmierbar“**, mit dem Vermerk „61x.7y models only“. Gleiche Tabelle: Americano 150 ml (70–480), Long 160 ml (115–250), Coffee 180 ml (100–240), Espresso 40 ml (20–180), Doppio+ 120 ml (80–180), Tea 150 ml, Hot Water 250 ml (20–420), Over Ice 100 ml (40–240).
- Es gibt zusätzlich: „Espresso SOUL“ (nur nach Einrichtung von Bean Adapt in der App, Kap. 7.5), Coffee Pot (Kap. 8, Aroma pro Tasse Extra mild/Mild/Medium/Strong), Over Ice (Kap. 9), Mug To Go (Kap. 11), Tea (Kap. 12), Hot Water (Kap. 13).

## 2. Wie ist „Long Black“ definiert?

- **Die Bedienungsanleitung der 610.74 definiert Long Black nicht inhaltlich**: nur Standard 200 ml und Bereich 90–480 ml [Beleg]. Keine Angabe zur Reihenfolge, zur Kaffeemenge oder zur Bohnenmenge (Bohnenmenge generell nur: Mühle/„Adjust mill“, Brüheinheit variabel 7–15 g laut Händlerdatenblatt).
- Aus De'Longhi-Anleitungen **anderer** Maschinen (Siebträger-Geräte): „Long Black: you should deliver hot water first and then coffee“ (Americano umgekehrt: erst Kaffee, dann Heißwasser) (https://dls.delonghigroup.com/files/docs/5/EN-57132C9313.pdf, Kap. 5) und „This long coffee is a favourite in Australia and New Zealand. Hot water is delivered first, followed by the espresso directly into the cup. Only one cup can be made at a time.“ (https://dls.delonghigroup.com/files/docs/5/EN-5713238271.pdf) [Beleg, aber fremde Modelle].
- [Schluss] Wahrscheinlich ist Long Black auf der Soul ebenfalls „Heißwasser zuerst, dann Espresso“; **für die 610.74 selbst ist das nicht belegt** [offen]. Die Zweiphasigkeit steckt in der Maschine (Rezept-Firmware), im Protokoll gibt es nur Parameter, siehe 3.
- Mengen im Rezept (Kaffee ml vs. Wasser ml) für die Soul: [offen]. Die Siebträger-Tabelle in EN-57132C9313 nennt für „Long Black (single)“ Kaffee 20–110 ml und Wasser 25–240 ml, wieder fremdes Modell.
- Die Anleitung der 610.74 sagt: Temperatur ist pro Profil einstellbar („Coffee temperature“, Kap. 6.5), „most effect for long beverages“. Aroma/Taste und Menge pro Profil in „My“-Anpassung (Kap. 14), gespeichert nur im gewählten Profil.

## 3. Kodierung im Protokoll

Quellen: Reverse-Engineering-Projekte. Keine offizielle Dokumentation.

**Rahmen/Brew-Paket (BLE, Dinamica Plus)** – https://grack.com/blog/2022/12/02/hacking-bluetooth-to-brew-coffee-on-github-actions-part-2/ [Beleg]:
- Start `0x0d` (Host→Maschine), Länge, Nutzdaten, 2 Byte Prüfsumme (Startwert 0x1D0F). Befehl `0x83` = Getränkeausgabe. Beispiel Cappuccino: `83 f0 07 01 01 00 78 09 01 77 02 04 02` = Befehl, Konstante f0, Getränke-ID 07, Trigger START(01), Parameter-Paare (ID, Wert): Coffee 0x01 = 120, Milk 0x09 = 375, Taste 0x02 = 4, Modus.
- Mengenparameter (Coffee, Milk, HotWater) 2 Byte, alles andere 1 Byte. Hinweis des Autors: einige IDs seien Vermutungen aus dem Disassembly.

**Enums aus longshot 0.1.13** (Rust, https://docs.rs/crate/longshot/0.1.13/source/src/protocol/hardware_enums.rs) [Beleg]:
- `EcamBeverageId`: EspressoCoffee=1, RegularCoffee=2, LongCoffee=3, EspressoCoffee2X=4, DoppioPlus=5, Americano=6, Cappuccino=7, LatteMacchiato=8, CaffeLatte=9, FlatWhite=10, EspressoMacchiato=11, HotMilk=12, CappuccinoDoppioPlus=13, ColdMilk=14, CappuccinoReverse=15, HotWater=16, Steam=17, Ciocco=18, Ristretto=19, LongEspresso=20, CoffeeCream=21, Tea=22, CoffeePot=23, Cortado=24, **LongBlack=25**, TravelMug=26, BrewOverIce=27, Custom01..10=230..239.
- `EcamIngredients` (Parameter-IDs): Temp=0, Coffee=1, Taste=2, Granulometry=3, Blend=4, InfusionSpeed=5, Preinfusion=6, Crema=7, DueXPer=8, Milk=9, MilkTemp=10, MilkFroth=11, **Inversion=12**, TheTemp=13, TheProfile=14, HotWater=15, …, Programmable=24, Visible=25, VisibleInProgramming=26, IndexLength=27, Accessorio=28. Breit (2 Byte): Coffee, Milk, HotWater.
- `EcamBeverageTaste`: Preground=0, ExtraMild=1, Mild=2, Normal=3, Strong=4, ExtraStrong=5. `EcamTemperature`: Low=0, Mid=1, High=2, VeryHigh=3.
- `EcamOperationTrigger`: Start=1, StartProgramOrStopV2=2, NextStep=3, Stop=4, … `EcamBeverageTasteType` (Modus): Delete=0, Save=1, Prepare=2, PrepareAndSave=3, SaveInversion=5, PrepareInversion=6, PrepareAndSaveInversion=7.
- Anfrage-IDs: BeverageDispensingMode=131 (0x83), AppControl=132 (0x84; (2,1)=einschalten), RecipeQuantityRead=166 (0xA6, Standardrezept lesen), RecipeMinMaxSync=176, ProfileSelection=169, ProfileNameRead/Write=164/165, SetFavoriteBeverages=173, RecipeNameRead/Write=170/171, BeanSystemRead/Write=186/187, PinRead=210, PinSet=177.
- Maschinenstatus: StandBy=0, TurningOn=1, ShuttingDown=2, Descaling=4, ReadyOrDispensing=7, Rinsing=8, MilkPreparation=10, HotWaterDelivery=11, MilkCleaning=12.
- Das Projekt ist nur an einer Dinamica Plus über BLE getestet (https://github.com/mmastrac/longshot).

**Wi-Fi/Cloud-Weg, speziell Soul** – cremalink 0.1.0b28 (https://pypi.org/project/cremalink/, Quellcode https://github.com/miditkl/cremalink) [Beleg]:
- „primarily optimized for the De'Longhi PrimaDonna Soul“, unterstützt „PrimaDonna Soul (ECAM612)“, Datei `devices/ECAM610.json` mit identischem Befehlssatz liegt ebenfalls im Repo. Steuerung per Ayla-Networks-Cloud-API oder lokalem LAN-Server (Port 10280). Der Befehl wird als Hex-Paket + Zeitstempel (+ App-ID) Base64-kodiert an die Ayla-Property gesendet; die App-ID wird bei Bedarf registriert (im Code 7 s Wartezeit).
- Befehlstabelle `ECAM610.json` enthält: americano, caffe_latte, cappuccino, cappuccino_mix, cappuccino_plus, coffee, cortado, doppio_plus, double_espresso, espresso, espresso_macchiato, espresso_soul, flat_white, hot_milk, hot_water, latte_macchiato, long_coffee, standby (`0d07840f01010041`), wakeup (`0d07840f02015512`), stop. **Es gibt dort keinen Eintrag für Long Black, Over Ice, Mug To Go oder Tea** [Beleg].
- Beispiel Americano `0d1483f006010100280f006e1b010203040006ecfe`. Entschlüsselt nach den longshot-Enums [Schluss]: 0x83, f0, Getränke-ID 06, Trigger 01 (Start), Coffee(01)=0x0028=40, HotWater(0f)=0x006e=110, IndexLength(1b)=1, Taste(02)=3, Blend(04)=0, Modus 06 (PrepareInversion), Prüfsumme. Espresso entsprechend: ID 01, Coffee 0x7e=126. Cappuccino/Flat White/Cortado tragen zusätzlich `0c 01` (Inversion=1) und `1c 02` (Accessorio=2 = Milchbehälter).
- [Schluss] Ein Long-Black-Paket müsste analog gebaut werden: Getränke-ID 0x19 (25), Parameter Coffee (01), HotWater (0f), Taste (02) usw., vermutlich mit Inversion (0c)=1 für „Wasser zuerst“ – **Parameter und genaue Zahlen nicht belegt**. Sicherster Weg: Standardrezept per `RecipeQuantityRead` (0xA6) für ID 25 von der Maschine lesen bzw. den Befehl aus dem Verkehr der Coffee-Link-App mitschneiden (Mitschnitt der App ist die einzige gesicherte Quelle der echten Werte). Ob die Prüfsumme/Länge identisch zu BLE berechnet wird, ist aus den Beispielen plausibel (gleiches Frame `0d len 83 f0 …`), aber nicht separat belegt.
- Taste-Werte, Temperatur: Der Americano-Befehl enthält keinen Temperaturparameter; die Temperatur kommt aus der Profil-/Maschineneinstellung [Schluss, aus dem Fehlen im Paket].

## 4. Benutzerprofile und Lieblingsgetränke

- Anleitung Kap. 15 [Beleg]: je nach Modell 3 oder 5 Profile, jedes mit Icon; angepasste Getränkeeinstellungen werden je Profil gespeichert; Getränkereihenfolge nach Auswahlhäufigkeit; Profilname nur über die Coffee-Link-App änderbar; Profil-Icon in Menü 6.7 „Edit profile“. Kap. 6.5: Kaffeetemperatur pro Profil.
- „Lieblingsgetränke“ im Sinne einer eigenen Funktion sind in der Anleitung **nicht** dokumentiert [offen]; es gibt „My“-Anpassung (Kap. 14) und App-erstellte Getränke (mit eigenem Symbol auf dem Display, „This symbol appears on the beverages created from the App“). Im Protokoll existieren `SetFavoriteBeverages` (173) und `ProfileSelection` (169) (longshot, Enums) [Beleg Existenz, Verhalten offen].
- Ob der 0x83-Befehl ein Profil enthält oder die Maschine das aktuell aktive Profil verwendet, ist nicht belegt [offen]; im Beispielpaket gibt es kein Profilbyte (TheProfile=14 ist ein Rezeptparameter). Für „Standardkaffee per Programm“ ist daher wahrscheinlich ein explizit gesendetes Rezept (mit Mengen) der robusteste Weg; ein Auslösen eines gespeicherten Profil-Rezepts per ID ist nicht belegt.
- App-erstellte Getränke liegen als Custom01–Custom10 (IDs 230–239) im Gerät [Beleg Enum]; Fernstart eines solchen Custom-Getränks per ID wäre denkbar [Schluss, ungeprüft].

## 5. Voraussetzungen für einen Fernstart

Aus der Anleitung (EN-57132C8859) [Beleg]:
- Hauptschalter hinten (A7) muss gedrückt sein (Kap. 4).
- Maschine muss eingeschaltet und fertig sein: „Each time the appliance is turned on, it performs an automatic preheat and rinse cycle which cannot be interrupted. The appliance is ready for use only after completion of this cycle.“ Beim Ausschalten folgt ebenfalls ein Spülgang (Kap. 5).
- Fernbedienung muss am Gerät freigegeben sein: Einstellungen > „Connectivity“ > Wi-Fi und Fernsteuerung aktivieren; PIN wird angezeigt (Kap. 6.3). „If remote connection is off, a number of the App's functions will not be available (e.g. beverage delivery).“ Statussymbol auf der Startseite zeigt: verbunden / WLAN aber kein Netz/Cloud nicht erreichbar / deaktiviert.
- Ist „Energy Saving“ aktiv, kann der erste Kaffee einige Sekunden dauern (Kap. 7.6, 13).
- Nach >3–4 Tagen Stillstand wird 2–3x Spülen empfohlen (Kap. 6.1).
- Blockierende Zustände (Meldungen, Kap. 21): Wassertank leer, Satzbehälter voll („Empty grounds container“ – auch 72 h nach erstem Kaffee erzwungen, wenn nicht per Hauptschalter getrennt), Tropfschale voll, Bohnenbehälter leer, Entkalkung/Reinigung. Genaue Sperrliste der Meldungen nicht vollständig ausgewertet.
- Tasse: Die Anleitung verlangt, eine Tasse unterzustellen; **ein Tassensensor ist weder in der Anleitung noch auf der Produktseite erwähnt** [Beleg durch Fehlen]. Die Tasse muss also physisch bereitstehen, sonst läuft das Getränk in die Tropfschale.
- Aus dem Standby aufwecken: per Protokoll möglich (`wakeup` = `0d07840f02015512`, cremalink; AppControl 132 bei longshot). Nach dem Wecken Statuswechsel „waking_up“ (1) -> Aufheiz-/Spülphase -> „ready“ (7) abwarten, erst dann Getränk senden [Schluss aus Status-Enums + Anleitung].
- BLE-Weg: Die Maschine unterstützt nur eine Verbindung gleichzeitig; parallel läuft keine App (siehe Arbuzov-Integration, https://github.com/Arbuzov/home_assistant_delonghi_primadonna) [Beleg für Dinamica/ECAM 550 etc., nicht für Soul geprüft].

## 6. Sicherheitsfunktionen gegen Fernstart ohne Bestätigung

- In keiner gefundenen Quelle gibt es eine Bestätigung an der Maschine für einen Fernstart [offen/negativer Befund]. Die Anleitung nennt als „Gate“ nur: Fernsteuerung in den Einstellungen aktiviert, PIN-basiertes Koppeln der App (Kap. 6.3), Maschine bereit, keine Alarmzustände. Das Brew-Paket von longshot/cremalink wird ohne Handshake pro Getränk ausgeführt (cremalink-Code: Paket + Zeitstempel + App-ID).
- Kein Tassen-/Sensor-Check dokumentiert (siehe 5).
- Sicherheitshinweis der Anleitung (Coffee Pot): Nach einer Kanne 5 Minuten abkühlen lassen, „Risk of overheating“ – ein Gerätelimit, das Getränke blockieren könnte [Beleg Hinweis, Verhalten nicht geprüft].
- Hinweis: Eine echte Bestätigung gibt es ggf. bei Getränken mit Zubehör (Milchbehälter, Kaffeekanne, Mug), die manuell bereitstehen müssen; Long Black braucht laut Anleitung kein Zubehör.

## 7. Offene Punkte (bitte beim Bauen beachten)

1. Reihenfolge/Mengen von Long Black auf der 610.74: nicht in der Anleitung; Befehl aus App-Mitschnitt oder `RecipeQuantityRead` für ID 25 ermitteln.
2. Ob Wi-Fi-Pakete der Soul exakt die BLE-Rahmenkodierung nutzen (Prüfsumme, Länge) – nur indirekt aus cremalink-Hexbefehlen ersichtlich.
3. Ob ein Profil im Befehl steckt oder das aktive Profil gilt.
4. Ob ein Fernstart über die Cloud (Ayla) dieselben Voraussetzungen hat wie lokal (Ayla-Property `data_request`, Monitor `d302_monitor`).

BEST-PRACTICES-KANDIDATEN:
- longshot 0.1.13: Enum-/Paketstruktur der ECAM-Protokolls (BeverageId LongBlack=25, Ingredient-IDs, Trigger, Modus), https://docs.rs/crate/longshot/0.1.13/source/src/protocol/hardware_enums.rs und https://grack.com/blog/2022/12/02/hacking-bluetooth-to-brew-coffee-on-github-actions-part-2/
- cremalink 0.1.0b28 (PrimaDonna Soul, Ayla-Cloud/Local-LAN, Befehlsrahmen, App-ID-Registrierung), https://github.com/miditkl/cremalink und https://pypi.org/project/cremalink/
- Offizielle Anleitung ECAM61X.7Y (Stand 03/2025, 57132C8859_01_0325): Connectivity-Aktivierung, Bereit-Zustand nach Einschalten, Long-Black-Mengen: https://dls.delonghigroup.com/files/docs/5/EN-57132C8859.pdf

BUG-KANDIDATEN:
- cremalink 0.1.0b28: Befehlstabelle für ECAM610/612 enthält kein Long Black, Over Ice, Mug To Go, Tea; Code „tightly coupled“ an Soul laut README (https://github.com/miditkl/cremalink, Beta-Stand).
- Home-Assistant-Integration Arbuzov/home_assistant_delonghi_primadonna: ECAM 610 nicht als unterstützt gelistet; nur eine gleichzeitige Verbindung; native App währenddessen nicht nutzbar (https://github.com/Arbuzov/home_assistant_delonghi_primadonna).
- Homey-App „Delonghi.Coffee“: ein Nutzer meldet, die PrimaDonna Soul (ECAM610.75) sei nicht kompatibel (https://community.homey.app/t/app-pro-delonghi-coffee/80377).
- longshot 0.1.13: nur an Dinamica Plus über BLE getestet; Protokoll nur in Quelltext dokumentiert; einige Parameter-IDs laut grack.com „guesses“.
