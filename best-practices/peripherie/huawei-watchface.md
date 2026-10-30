# Huawei-Zifferblätter (.hwt): Format und Wege

Stand: 30.10.2026. Versions-Anker: Huawei Watch 3 (HarmonyOS 2 bis 4), Beispieldateien aus Theme Studio von Oktober 2024. Theme Studio 11.0.20.310 nur aus Suchausschnitt.

## §1 Hardware (extern)
Watch 3 und Watch 3 Pro: 1,43 Zoll AMOLED, rund, 466 × 466, 326 ppi.
Quellen: https://m.gsmarena.com/huawei_watch_3-10940.php, https://www.xda-developers.com/huawei-watch-3-pro/
Die Angabe 461 ppi auf gsmchoice ist falsch gerechnet. Die GT-3-Beispiele nutzen ebenfalls eine 466er-Leinwand und `screen` = `HWHD09`; für die Watch 3 ist dieser Code nicht offiziell belegt.

## §2 Dateiformat (eigene Analyse, kein offizielles Dokument)
Geprüft: `WatchPhase/hwt.py` baut `watchface.bin` eines Theme-Studio-Exports (Beispiel V200) Byte für Byte gleich nach; alle 12 Nicht-Widget-Ebenen eines weiteren Exports stimmen ebenfalls. Widget-Container (`container`) sind nicht nachgebaut. Beispiele: https://github.com/leeorichi/huawei-watch-face-maker

- `.hwt` = ZIP mit `ability.xml`, `com.huawei.watchface`, `description.xml`, `hashCode`, `preview/{cover,aod}.jpg` (960²), `preview/icon_small.jpg` (390²).
- `com.huawei.watchface` = ZIP mit `watchface/res/A100_nnn.png`, `watchface/watch_face_config.xml`, `watchface/watch_face_info.xml` (gleich `description.xml`) und `watchface.bin` im Wurzelverzeichnis.
- `hashCode` = SHA-256 groß von `description.xml`, `com.huawei.watchface`, `ability.xml` und ein in allen Beispielen gleicher vierter Wert, kommagetrennt.
- `watchface.bin`: Kopf `<HHIII` (2, Länge Beschreibung, Länge Tabelle, Länge Bilddaten, 0), Protobuf-Beschreibung, Tabelle aus Offset/Länge je Bild, Bilddaten. Bilddaten beginnen mit `55 55 55 55 01 00 00 01`.
- Bild: `45 23 88 88`, Breite und Höhe als u16, dann BGRA mit gerader Alpha. Läufe ab 4 gleichen Pixeln: `89 67 45 23`, Pixel, Anzahl als u32.
- `A100_001.png` ist in allen Beispielen die 250²-Vorschau.

## §3 Konfiguration (aus Beispielen abgeleitet)
- draw_type im Protobuf: text 0, single_res 1, selected_res 2, hand_res 5, text2_res 10.
- value_type: 70 Tag Zehner (4 Bilder), 71 Tag Einer (10 Bilder), 51 und 52 aus dem Hublot-Beispiel nicht sicher zugeordnet, 150/153/154 Zeiger.
- `ability.xml`, valueType: Bitliste mit je 32 Stellen. Beobachtete Lage: 70→Stelle 64, 71→65, 150→141, 153→144, 154→145. Die Bedeutung von `xmlType` ist offen.
- Zeiger: Spitze oben im Bild, `rotate_rect` = Quadrat um die Mitte mit Seite 2 × Drehpunkt-y.
- Eine Always-on-Datum-Ebene (`selected_res` mit ambient 1) kommt in den Beispielen nicht vor.

## §4 Wege auf die Uhr (Stand der Recherche, an einer Watch 3 nicht getestet)
- Offiziell: Theme Studio, Export `.hwt`, Import in Huawei Health mit Entwickler-HUAWEI-ID ("Add watch faces" nur mit Berechtigung "Themes certified designer"). https://developer.huawei.com/consumer/en/codelab/theme-Watchface/
- Gadgetbridge listet Watch 3 mit "Uploading watch faces": https://gadgetbridge.org/basics/topics/huawei-honor/
- Modifizierte Huawei Health (Community, Test-Signatur, auf Samsung mehrfach fehlgeschlagen): https://faces4watch.com/download/how-to-install-custom-watch-faces-on-huawei-watch-gt-gt2-gt2e-gt2-pro-honor-watches
- Gallery-Zifferblatt in Huawei Health ab 14.0.8.300 erlaubt nur Fotos als Hintergrund: https://consumer.huawei.com/ca/support/content/en-us15972335
- Theme Studio für macOS ist nicht bestätigt, das Codelab verlangt Windows 10.
