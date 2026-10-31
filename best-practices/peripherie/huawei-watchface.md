# Huawei-Zifferblätter (.hwt): Format und Wege

Stand: 31.10.2026. Versions-Anker: Huawei Watch 3 GLL-AL04, Firmware 3.0.0.179, Huawei Health 17.0.8.300, Gadgetbridge 0.95.1, Samsung S23 Ultra mit Android 16. §0 ist auf dieser Uhr selbst geprüft, der Rest stammt aus Exporten und Recherche.

## §0 Geprüfter Weg auf die Watch 3 (31.10.2026, läuft auf der Uhr)
Kurzcheck: Watch 3 braucht `<version>1.1.1</version>`, `screen` HWHD09 und ein inneres Paket OHNE `watchface.bin`. Eine GT-Datei mit `watchface.bin` lädt Gadgetbridge hoch, die Uhr verwirft sie still.

1. Kontonummer: `adb logcat -c`, Health öffnen, in `adb logcat -d` nach `huid='` suchen. Kein Huawei-Login nötig. Nie ausgeben, nur verdeckt in eine Datei schreiben und per `adb shell input text "$(cat datei)"` eintippen.
2. Gadgetbridge per adb installieren, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, Standort und `POST_NOTIFICATIONS` per `pm grant`.
3. Health nur `BLUETOOTH_CONNECT` und `BLUETOOTH_SCAN` entziehen, dann `am force-stop`. Das reicht, die rollengebundenen Rechte bleiben unberührt. Nach `pm grant` ist der Stand einschließlich Flags identisch (mit `dumpsys package` geprüft).
4. In Gadgetbridge Uhr lange drücken, „Authentifizierungseinstellungen“, „Huawei Account“ = Kontonummer. Danach antippen, „Bereits gebunden“ mit OK bestätigen und „Nicht koppeln“ wählen: Die Android-Kopplung bleibt, Gadgetbridge verbindet trotzdem.
5. Installieren: `.hwt` nach `/sdcard/Documents/...` legen, Mediascan, dann `am start -a android.intent.action.VIEW --grant-read-uri-permission -d content://media/external/file/<id> -t application/octet-stream -n nodomain.freeyourgadget.gadgetbridge/.activities.install.FileInstallerActivity`.
6. Beleg ist nur die Liste: App-Manager neu öffnen (sonst steht die alte Liste da) und nach dem Titel suchen. „Firmwareinstallation abgeschlossen“ belegt nichts. Dort auch „Aktivieren“ und „Löschen“ (Löschen hat einen zweiten Dialog „Ok“).
7. Rückbau: Gadgetbridge `am force-stop` und `pm disable-user --user 0`, in Gadgetbridge nie das Gerät löschen. `pm grant` für beide Rechte, Health öffnen, „Verbinden“ tippen. Gleicher Schlüssel, keine neue Kopplung. Die Uhr kann danach auch im Health-Protokoll geprüft werden: `connectState==1`, `handleFaceInfo() size`, `mDeviceCurrentWatchFace` = der Zahlenteil des Upload-Namens.

Aufbau, der läuft: außen `com.huawei.watchface`, `description.xml`, `preview/{cover,aod,icon_small}.jpg`, ohne `ability.xml` und `hashCode`. Innen `watchface/res/A100_000.png` (466² Vorschau) und folgende, `watchface/watch_face_config.xml`, `watchface/watch_face_info.xml`. Elemente in der Reihenfolge time, date, background. Tag: `combined_res` mit `value_type="17"`, zehn Ziffernbilder. `res_position` meint dort die Mitte der Zahl, x und y (auf der Uhr korrigiert und von Frank bestätigt, siehe Almanach §0b). Ein Zeiger hat `value_type` 150/153/154. `HWHD09` ist von der Uhr angenommen.

Ohne Datum geht es auch: Das Element `date` darf fehlen (EIGER, BERING). Ohne Sekundenzeiger ebenso, dann entfällt die Ebene mit `value_type` 154 (BERING). Beispiel für mehrere Zifferblätter: `WatchPhase/klassiker.py`.

Die Uhr meldet in Health ihre Grenzen: `WatchFace_MaxVersion:1.2`, `OtherThemeVersion:1.1,1.2`, `getSupportVersion:1.1,2.1`, `SupportFileType:[1]`. Das deckt sich mit Huaweis Tabelle (offiziell): https://developer.huawei.com/consumer/en/doc/content/resolution-version-0000001252603441 . Muster für den Aufbau: Watch-3-Dateien (extern) unter https://faces4watch.com , Kategorie „Huawei Watch 3-4-5 Series“. Gadgetbridge sendet das innere Paket als Ganzes (extern): https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/3910

## §1 Hardware (extern)
Watch 3 und Watch 3 Pro: 1,43 Zoll AMOLED, rund, 466 × 466, 326 ppi.
Quellen: https://m.gsmarena.com/huawei_watch_3-10940.php, https://www.xda-developers.com/huawei-watch-3-pro/
Die Angabe 461 ppi auf gsmchoice ist falsch gerechnet. Die GT-3-Beispiele nutzen ebenfalls eine 466er-Leinwand und `screen` = `HWHD09`; für die Watch 3 ist dieser Code nicht offiziell belegt.

## §2 Dateiformat (eigene Analyse, kein offizielles Dokument)
Gilt für die GT-Serie, nicht für die Watch 3 (siehe §0). Geprüft: `WatchPhase/hwt.py` baut `watchface.bin` eines Theme-Studio-Exports (Beispiel V200) Byte für Byte gleich nach; alle 12 Nicht-Widget-Ebenen eines weiteren Exports stimmen ebenfalls. Widget-Container (`container`) sind nicht nachgebaut. Beispiele: https://github.com/leeorichi/huawei-watch-face-maker

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

## §4 Weitere Wege (Stand der Recherche, an der Watch 3 nicht getestet; geprüfter Weg siehe §0)
- Offiziell: Theme Studio, Export `.hwt`, Import in Huawei Health mit Entwickler-HUAWEI-ID ("Add watch faces" nur mit Berechtigung "Themes certified designer"). https://developer.huawei.com/consumer/en/codelab/theme-Watchface/
- Gadgetbridge listet Watch 3 mit "Uploading watch faces": https://gadgetbridge.org/basics/topics/huawei-honor/
- Kopplung behalten: Gadgetbridge auf dem Handy mit Huawei Health nutzen, Auth key = 17-stellige Account-ID (`userId` im Huawei-Cloud-Cookie), Health-Berechtigungen vorher entziehen, Uhr in Health nie entkoppeln oder löschen. Danach ist beides nutzbar, auf der Watch 3 bestätigt. Vom Handy mit Health aus geprüft, nicht von einem zweiten Handy aus. https://gadgetbridge.org/basics/pairing/huawei-honor-pairing/
- Modifizierte Huawei Health (Community, Test-Signatur, auf Samsung mehrfach fehlgeschlagen): https://faces4watch.com/download/how-to-install-custom-watch-faces-on-huawei-watch-gt-gt2-gt2e-gt2-pro-honor-watches
- Gallery-Zifferblatt in Huawei Health ab 14.0.8.300 erlaubt nur Fotos als Hintergrund: https://consumer.huawei.com/ca/support/content/en-us15972335
- Theme Studio für macOS ist nicht bestätigt, das Codelab verlangt Windows 10.
