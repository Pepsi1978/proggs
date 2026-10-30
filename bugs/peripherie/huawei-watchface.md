# Huawei-Zifferblätter: bekannte Fallen

Stand: 30.10.2026. Versions-Anker: Watch 3 (HarmonyOS 2 bis 4), Gadgetbridge 0.90 bis 0.91.1, Health-Mod 15.0.x bis 15.1.4.340. Alle Einträge sind Berichte Dritter, an einer Watch 3 nicht geprüft.

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
