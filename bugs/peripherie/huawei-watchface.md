# Huawei-Zifferblätter: bekannte Fallen

Stand: 31.10.2026. Versions-Anker: Watch 3 GLL-AL04 mit Firmware 3.0.0.179, Health 17.0.8.300, Gadgetbridge 0.95.1, Health-Mod 15.0.x bis 15.1.4.340. §0 und §0a sind auf dieser Uhr erlebt, alle anderen Einträge sind Berichte Dritter.

## §0 Watch 3 verwirft eine Datei mit `watchface.bin` still
- Symptom: Der Balken läuft durch, Gadgetbridge meldet „Firmwareinstallation abgeschlossen“, die Liste bleibt bei 50 Zifferblättern, auf der Uhr erscheint nichts.
- Ursache: Bei einer Datei mit innerer `watchface.bin` sendet Gadgetbridge nur diese. Das ist das Format der GT-Serie. Die Watch 3 braucht das innere Paket ohne `watchface.bin`, mit Version 1.1.1.
- Betroffen: Watch 3 GLL-AL04, Firmware 3.0.0.179, Gadgetbridge 0.95.1, erlebt am 31.10.2026.
- Fix: Inneres Paket ohne `watchface.bin` bauen, Version 1.1.1. Nachweis im Log: `Unable to get watchfaceZip, it seems older already watchface.bin` steht trotz des Wortlauts für den richtigen Weg (ganzes Paket).
- Gadgetbridge wertet den Status im Abschlusspaket nicht aus, „abgeschlossen“ belegt also nichts (extern, Quelltext `AsynchronousResponse.handleFileUpload`): https://codeberg.org/Freeyourgadget/Gadgetbridge

## §0a Nach dem Rückbau zeigt Health „Nicht verbunden“
- Symptom: Rechte zurückgegeben, Health offen, die Uhr bleibt getrennt.
- Ursache: Health verbindet nach dem Force-Stop nicht von selbst.
- Fix: In Health „Verbinden“ tippen. Nichts entkoppeln. Eine Abfrage „Standort immer zulassen“ gehört nicht dazu, sie lässt sich abbrechen.

## §0b Tag steht über dem Datumsfenster
- Symptom: Die Zahl sitzt auf der Uhr oberhalb des Felds, in der Vorschau passt sie.
- Ursache: Bei `combined_res` ist die y-Angabe in `res_position` die Mitte der Zahl, nicht die obere Kante.
- Betroffen: Watch 3 GLL-AL04, Firmware 3.0.0.179, erlebt am 31.10.2026.
- Fix: `res_position` auf die Mitte des Felds setzen, x und y.

## §1 Upload meldet Erfolg, Zifferblatt erscheint nicht
- Symptom: Gadgetbridge zeigt "installation complete", das Zifferblatt taucht nie auf.
- Ursache: Ab HarmonyOS 6.1 bzw. Firmware nach 6.0.0.328 prüft die Uhr ein serversigniertes `contentSign`; der Schlüssel liegt bei Huawei.
- Betroffen: GT 6, GT Runner 2, Fit 4/5. Die Watch 3 steht dort nicht, ihre gefundenen Builds liegen bei 3.0.x und 4.0.0.386.
- Fix: keiner bekannt ("dead end" laut Entwickler).
- Quellen: https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/6005, https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/5968

## §2 "update your device" oder "newer version"
- Ursache: vermutlich zu hohe `version` in `description.xml`.
- Fix: Version senken, bei GT 2 half 2.8 → 2.7 (Nutzerbericht). Danach `hashCode` neu berechnen.
- Quelle: https://xdaforums.com/t/huawei-watch-gt-2-custom-faces.3989271/post-84178301

## §3 Health-Mod auf Samsung scheitert
- Symptom: "App not installed", "package invalid", Login ohne Reaktion.
- Betroffen: u. a. 15.0.12.300 auf Galaxy Fold 4 mit Android 15. Zu Android 16 keine Berichte.
- Fix: andere Mod-Version oder Gadgetbridge. Die `.hwt` nicht im Download-Ordner ablegen, sondern in einem eigenen Ordner.
- Quelle: https://faces4watch.com/download/how-to-install-custom-watch-faces-on-huawei-watch-gt-gt2-gt2e-gt2-pro-honor-watches

## §4 Kein lokaler Import in der normalen Health-App
- Ursache: "Add watch faces" erscheint nur mit HUAWEI-ID mit Berechtigung "Themes certified designer (watch face)" (Suchausschnitt der Huawei-Doku, Seite nicht lesbar).
- Fix: Designer-Konto beantragen oder einen anderen Weg nehmen.
