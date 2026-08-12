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
.\Start-Fold8.ps1                            # zugeklappt (Cover-Display) — der Normalfall
.\Start-Fold8.ps1 -Projekt EntropieReductor  # App bauen, installieren, starten
.\Start-Fold8.ps1 -Projekt X -OhneBauen      # vorhandene APK nehmen, nicht neu bauen
.\Start-Fold8.ps1 -Innen                     # großes Innendisplay (aufgeklappt)
.\Start-Fold8.ps1 -MitGeraet                 # zusätzlich das echte Fold live daneben
.\Start-Fold8.ps1 -Zoom 1.5                  # größer als das echte Handy
.\Start-Fold8.ps1 -Apk app.apk               # fertige APK installieren
.\Start-Fold8.ps1 -Kaltstart                 # ohne gespeicherten Zustand starten
```

Startet den Emulator (falls noch keiner läuft), wartet auf den Systemstart, setzt Schriftskalierung
0,9 und Hochformat wie am echten Gerät und stellt die Originalgröße ein.

**Standard ist zugeklappt** — das Cover-Display (5,5 Zoll), so wie man das Fold 8 im Alltag in der
Hand hält. Läuft bereits ein Emulator mit der falschen AVD, wird automatisch gewechselt, statt
stillschweigend das falsche Format zu zeigen.

Zugeklappt und aufgeklappt sind **zwei getrennte AVDs**, kein Scharnierwinkel: Klappt man die
Innen-AVD per Scharnier zu, rechnet Android das Layout nicht um und zeigt nur einen Ausschnitt
(siehe `bugs/android/emulator-foldable.md` #1).

Mit `-Projekt` wird zusätzlich `gradlew assembleDebug` ausgeführt, die frisch gebaute Debug-APK
installiert und die App gleich gestartet. Der Projektname genügt (gesucht wird unter
`~\proggs\<Name>`), ein vollständiger Pfad geht auch. Schlägt der Build fehl, wird **nichts**
installiert — dann steht die alte Version nicht fälschlich für die neue.

## Klappen.ps1

```powershell
.\Klappen.ps1          # in den jeweils anderen Zustand wechseln
.\Klappen.ps1 -Auf     # aufgeklappt (Innendisplay, 7,6 Zoll)
.\Klappen.ps1 -Zu      # zugeklappt (Cover-Display, 5,5 Zoll)
```

Wechselt die AVD, bringt die zuletzt gestartete App mit und setzt die Originalgröße — mit jeder
App, nicht nur mit einer bestimmten. Der Falt-Knopf **im Emulator** taugt dafür nicht: er ändert
nur den Scharnierwinkel und lässt das Layout unverändert.

## Set-Originalgroesse.ps1

```powershell
.\Set-Originalgroesse.ps1              # 1:1 — so groß wie das Handy in der Hand
.\Set-Originalgroesse.ps1 -Zoom 2      # doppelt so groß
.\Set-Originalgroesse.ps1 -Cover       # Maßstab fürs Cover-Display
.\Set-Originalgroesse.ps1 -Ueberwachen # mitlaufen und dauerhaft 1:1 halten
```

Rechnet den Maßstab aus Monitor-ppi (aus dem EDID) und Geräte-ppi und setzt die Fenstergröße.
Auf diesem Rechner (2880 × 1800 auf 30 × 19 cm = 242,9 ppi):
Cover 715 × 1129 px = **7,47 × 11,81 cm**, Innendisplay 1112 × 1473 px = **11,63 × 15,4 cm**.
Zurück zur Originalgröße immer mit dem Aufruf ohne `-Zoom`.

### Der Größen-Wächter (`-Ueberwachen`)

`Start-Fold8.ps1` startet ihn automatisch mit. Einmal setzen genügt nämlich nicht: Beim Drehen
ändert der Emulator die Fenstergröße **selbst**. Der Wächter liest alle 250 ms nach, welche Fläche
Android gerade bespielt (`cur=` aus `dumpsys window displays`) und welche Drehung anliegt, und
stellt das Fenster jedes Mal neu auf 1:1 — egal wie oft und in welche Richtung gedreht wird.

Drei Sicherungen halten ihn stabil:

- **Nur einer gleichzeitig** (systemweiter Mutex). Zwei Wächter würden sich überstimmen und das
  Fenster sichtbar hin- und herspringen lassen.
- **Entprellen + Ruhezeit**: Zwischenzustände beim Drehen lösen kein Setzen aus, und nach jedem
  Eingriff wird 1,2 s nicht gemessen — sonst regelt er gegen sich selbst.
- **Ping-Pong-Bremse**: Hält der Emulator dagegen, pausiert die Nachführung 4 Sekunden. Die Sperre
  löst sich immer von selbst wieder, damit sie sich nie dauerhaft festfahren kann.

Getestet mit je 7 Drehungen nach rechts und links sowie 8 × über den Dreh-Knopf: alle Messungen
1:1 (Toleranz 6 px ≙ 0,6 mm).

## Zeig-Fehler.ps1

```powershell
.\Zeig-Fehler.ps1                  # Abstürze und Fehler der laufenden App
.\Zeig-Fehler.ps1 -Live            # mitlaufen, Fehler sofort melden
.\Zeig-Fehler.ps1 -Geraet R3GL7073MLM   # echtes Handy statt Emulator
```

Zeigt lesbar aufbereitet, was schiefgeht: Abstürze mit Ursache und eigener Codestelle, dazu
Fehlermeldungen der laufenden App. Systemrauschen (Bluetooth, Grafiktreiber) wird ausgeblendet —
das betrifft die App nicht.

`Start-Fold8.ps1` prüft das automatisch: Nach dem Start wird geschaut, ob die App noch läuft.
Ist sie abgestürzt, erscheint die Ursache sofort; schlägt schon der Build fehl, werden die
Compiler-Fehler gezeigt und **nichts** installiert.

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
