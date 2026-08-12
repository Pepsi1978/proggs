# Fold-8-Emulator — Werkzeuge

Emulator mit den echten Maßen des Galaxy Z Fold 8 (SM-F971B), dargestellt in Originalgröße,
plus Live-Spiegelung des echten Geräts.

Hintergrund und Fallen: `best-practices/android/emulator-foldable.md` · `bugs/android/emulator-foldable.md`

## Voraussetzungen (einmalig, bereits eingerichtet)

- System-Image `system-images;android-37.0;google_apis_playstore;x86_64` (Android 17 / API 37)
- AVD **Fold8** — 1848 × 2448 @ 420 dpi, 120 Hz, Scharnier-Sensor
- AVD **Fold8_Cover** — 1248 × 1972 @ 420 dpi
- scrcpy (`winget install Genymobile.scrcpy`)

## Start-Fold8.ps1

```powershell
.\Start-Fold8.ps1                  # aufgeklappt, Originalgröße
.\Start-Fold8.ps1 -Cover           # Cover-Display
.\Start-Fold8.ps1 -MitGeraet       # zusätzlich das echte Fold live daneben
.\Start-Fold8.ps1 -Zoom 1.5        # größer als das echte Handy
.\Start-Fold8.ps1 -Apk app.apk     # APK gleich mitinstallieren
.\Start-Fold8.ps1 -Kaltstart       # ohne gespeicherten Zustand starten
```

Startet den Emulator, wartet auf den Systemstart, setzt die Schriftskalierung auf 0,9 wie am
echten Gerät und stellt die Originalgröße ein.

## Set-Originalgroesse.ps1

```powershell
.\Set-Originalgroesse.ps1           # 1:1 — so groß wie das Handy in der Hand
.\Set-Originalgroesse.ps1 -Zoom 2   # doppelt so groß
.\Set-Originalgroesse.ps1 -Cover    # Maßstab fürs Cover-Display
```

Rechnet den Maßstab aus Monitor-ppi (aus dem EDID) und Geräte-ppi und setzt die Fenstergröße.
Auf diesem Rechner: Maßstab 0,6019 → 1112 × 1473 px = **11,63 × 15,4 cm**.
Zurück zur Originalgröße immer mit dem Aufruf ohne `-Zoom`.

## Zeig-Elemente.ps1

```powershell
.\Zeig-Elemente.ps1                      # laufender Emulator
.\Zeig-Elemente.ps1 -Geraet R3GL7073MLM  # echtes Fold 8
.\Zeig-Elemente.ps1 -NurKlickbare        # nur bedienbare Elemente
```

Macht einen Screenshot, liest die Bedienelemente aus und zeichnet nummerierte Rahmen darüber
(rot = bedienbar, blau = Anzeige). Ergebnis liegt in `%TEMP%\fold8-elemente\`:
`elemente.png` und `elemente.txt`.

Danach genügt der Satz **„Nummer 7 soll anders aussehen"** — die Nummer verweist eindeutig auf
Position, Beschriftung und Typ des Elements.

Bei reinen Compose-Oberflächen ohne `Modifier.testTag(...)` oder `contentDescription` bleibt die
Hierarchie dünn; dann ist der Layout Inspector in Android Studio der bessere Weg
(*Toggle Deep Inspect*, Doppelklick springt in die Quellzeile).

## Klappen

```powershell
adb -s emulator-5554 emu sensor set hinge-angle0 0     # zuklappen
adb -s emulator-5554 emu sensor set hinge-angle0 180   # aufklappen
adb -s emulator-5554 shell "dumpsys device_state | grep mCommittedState"
```

Achtung: Der gefaltete Zustand zeigt nur einen Ausschnitt, Android rechnet das Layout nicht auf
das Cover-Format um. Für echte Cover-Darstellung `-Cover` benutzen.

## Ausrichtung

Der Emulator startet im **Hochformat** (Frontansicht des aufgeklappten Geräts) und dreht sich nicht
von selbst — `Start-Fold8.ps1` schaltet die automatische Drehung ab. Manuell drehen über die
Bedienleiste rechts am Emulator oder:

```powershell
adb -s emulator-5554 shell settings put system user_rotation 0   # hochkant
adb -s emulator-5554 shell settings put system user_rotation 1   # quer
```

## Warum kein Geräterahmen?

Samsungs offizieller Galaxy-Z-Fold8-Skin liegt zwar unter
`%LOCALAPPDATA%\Android\Sdk\skins\Galaxy_Z_Fold8\`, ist auf diesem Bildschirm aber **nicht nutzbar**:

- Der Emulator stellt Skins immer 1:1 dar und skaliert sie nicht.
- Die Layouts sind 2885 × 2261 (Hauptbildschirm) und 1701 × 2388 (Cover) groß — beide höher als die
  1800 Pixel dieses Monitors. Das Fenster ragt dann oben heraus und ist nicht mehr greifbar.
- Der Skin überschreibt zusätzlich die Displaymaße und dreht die Ansicht ins Querformat.

Er brächte ohnehin nur den Gehäuserahmen, **kein One UI**. Wer ihn auf einem größeren Bildschirm
testen will, setzt in der `config.ini`:

```ini
showDeviceFrame=yes
skin.dynamic=no
skin.name=Galaxy_Z_Fold8_Main_Screen
skin.path=C:\Users\barwa\AppData\Local\Android\Sdk\skins\Galaxy_Z_Fold8\Galaxy_Z_Fold8_Main_Screen
```

## Grenzen

Der Emulator läuft mit Stock-Android, **nicht** mit One UI. Samsungs Emulator-Skin ändert daran
nichts ("does not include any One UI feature"). Schriftart, Farbsystem, Gestennavigation, Taskbar,
Flex-Mode und Animationsdauern von One UI lassen sich nur am echten Gerät beurteilen — dafür
`-MitGeraet` benutzen.
