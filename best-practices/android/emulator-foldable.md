# Android-Emulator für Foldables (Galaxy Z Fold 8) — Volltext

**Stand:** 12.08.2026
**Versionsanker:** Android Emulator 36.6.11.0 (gfxstream) · System-Image `android-37.0;google_apis_playstore;x86_64`
· Referenzgerät Samsung Galaxy Z Fold 8 (SM-F971B), Android 17 / API 37, One UI 9.0
· Host: Windows 11, Intel Core Ultra 7 258V, 31,5 GB RAM, Monitor 2880×1800 auf 30×19 cm (242,9 ppi)

Gegenstück im Bug-Almanach: noch keiner. Schwesterdatei: `android-platform.md`.

---

## §1 Gerätewerte ermitteln — messen statt schätzen

Web-Datenblätter nennen Diagonale und Auflösung, aber nicht die **Dichte-Klasse**, die Android
tatsächlich meldet, und nicht die Nutzereinstellungen. Beides entscheidet über die Darstellung.
Darum: Werte am angeschlossenen Gerät auslesen.

### §1.1 Beide Panels auslesen

```bash
adb shell "dumpsys display | grep -E 'DisplayDeviceInfo|densityDpi'"
```

Am Fold 8 gemessen:

| | Innendisplay (aufgeklappt) | Cover-Display |
|---|---|---|
| Auflösung | 1848 × 2448 px | 1248 × 1972 px |
| gemeldete Dichte | 420 dpi | 420 dpi |
| physische Dichte | 403,8 × 404,6 dpi | 428,4 × 428,1 dpi |
| Diagonale | 7,6″ | 5,5″ |
| in dp | 704 × 933 dp | 475 × 751 dp |
| Bildrate | 120 Hz (adaptiv 20–120) | 120 Hz |
| HDR | Typen 2/3/4, max. 1351 nits | — |

Gegenprobe: √(1848² + 2448²) / 403,6 ppi = 7,59″ — deckt sich mit den offiziellen 7,6″.
Beide Panels melden **denselben** Dichte-Bucket (420), obwohl sie physisch unterschiedlich dicht
sind. In dp gerechnet stimmen Emulator und Gerät dadurch überein.

### §1.2 `wm size` reicht nicht

`wm size` liefert nur das **gerade aktive** Panel. Ein zugeklapptes Fold meldet das Cover-Display.
Für beide Panels immer `dumpsys display` nehmen.

### §1.3 Zwei Werte, die gern übersehen werden

```bash
adb shell getconf PAGESIZE                    # 4096 = 4 KB, 16384 = 16 KB
adb shell settings get system font_scale      # Nutzereinstellung, oft ungleich 1.0
```

- **Page Size:** Meldet das Gerät 4096, dann ist ein `google_apis_playstore_ps16k`-Image gerade
  **nicht** geräteidentisch. Nur bei 16384 das `ps16k`-Image nehmen.
- **font_scale:** Steht am Gerät z. B. 0.9, erscheint im Emulator jede Schrift zu groß. Angleichen:
  `adb -s emulator-5554 shell settings put system font_scale 0.9`.

---

## §2 Foldable-AVD bauen

### §2.1 Basis anlegen

```powershell
avdmanager create avd -n Fold8 `
  -k "system-images;android-37.0;google_apis_playstore;x86_64" `
  -d "7.6in Foldable"
```

Das eingebaute Profil `7.6in Foldable` bringt bereits eine vollständige, korrekte Scharnier-
Konfiguration mit (1768 × 2208, Cover = linke Hälfte) — das ist die beste Vorlage. Die eigentliche
Arbeit ist danach das Umschreiben der `config.ini` auf die echten Gerätewerte.

Vorhandene Foldable-Profile: `7.6in Foldable`, `6.7in Horizontal Fold-in`, `8in Fold-out`,
`pixel_fold`, `pixel_9_pro_fold`, `pixel_10_pro_fold`, `resizable`.

### §2.2 Die entscheidenden Schlüssel in `config.ini`

Maßgebliche Referenz aller Schlüssel liegt lokal:
`$ANDROID_HOME/emulator/lib/hardware-properties.ini`.

```ini
hw.lcd.width  = 1848
hw.lcd.height = 2448
hw.lcd.density = 420
hw.lcd.vsync = 120

hw.sensor.hinge = yes
hw.sensor.hinge.count = 1
hw.sensor.hinge.type = 1          ; 0 = horizontal, 1 = vertikal (Buchfaltung)
hw.sensor.hinge.sub_type = 1      ; 0 = Falte auf dem Schirm, 1 = echtes Scharnier
hw.sensor.hinge.ranges = 0-180
hw.sensor.hinge.defaults = 180
hw.sensor.hinge.areas = 924-0-1-2448          ; Format: x-y-Breite-Höhe
hw.sensor.posture_list = 1, 2, 3              ; 1=zu, 2=halb, 3=offen, 4=geflippt
hw.sensor.hinge_angles_posture_definitions = 0-30, 30-150, 150-180
hw.sensor.hinge.fold_to_displayRegion.0.1_at_posture = 1

hw.displayRegion.0.1.width  = 1248            ; Cover-Ausschnitt
hw.displayRegion.0.1.height = 1972
hw.displayRegion.0.1.xOffset = 0
hw.displayRegion.0.1.yOffset = 0
```

`hinge.areas` beschreibt die Faltlinie als Rechteck: halbe Panelbreite, y=0, 1 px breit, volle Höhe.

**Falle:** Wird `hw.device.name`/`hw.device.hash2` beibehalten, während man die Maße ändert, meldet
Android Studio ein abweichendes Geräteprofil. Beide Zeilen entfernen und stattdessen einen eigenen
`hw.device.name` setzen.

**Falle:** `avdmanager` schreibt `avd.id=<build>` und `avd.name=<build>` als Platzhalter — von Hand
auf den AVD-Namen setzen.

Wirksam gewordene Werte prüft man in `<AVD>.avd/hardware-qemu.ini` (wird beim Start erzeugt).

### §2.3 Klappen per Kommandozeile

```bash
adb -s emulator-5554 emu sensor set hinge-angle0 0     # zuklappen
adb -s emulator-5554 emu sensor set hinge-angle0 180   # aufklappen
adb -s emulator-5554 emu sensor get hinge-angle0
adb -s emulator-5554 emu fold / unfold                 # Kurzform
```

Kontrolle, ob die App den Zustand sieht:

```bash
adb shell "dumpsys device_state | grep mCommittedState"
# -> DeviceState{identifier=1, name='CLOSED'} bzw. identifier=3, name='OPENED'
```

Genau diesen Zustand werten Apps über Jetpack WindowManager (`FoldingFeature`) aus.

### §2.4 Grenze: der gefaltete Zustand ist nur ein Ausschnitt

Getestet am 12.08.2026: Nach dem Zuklappen meldet `dumpsys window displays` unverändert
`cur=1848x2448`. Der Emulator zeigt lediglich den `displayRegion`-Ausschnitt — Android rechnet das
**Layout nicht** auf das Cover-Format um, Inhalte werden abgeschnitten.

**Konsequenz:** Für das Klapp-*Verhalten* (FoldingFeature, Posture-Wechsel) die Foldable-AVD nehmen,
für die *Darstellung* auf dem Cover-Display eine **zweite AVD** mit 1248 × 1972 @ 420 dpi.

---

## §3 Grafik- und Bewegungstreue

### §3.1 GPU-Modi

`emulator -help-gpu` nennt die gültigen Werte: `auto` (Standard), `host`, `software`, `lavapipe`,
`swiftshader`, `swangle`. Für maximale Treue **`host`** (echte Treiber des Rechners):

```ini
hw.gpu.enabled = yes
hw.gpu.mode = host
```

Achtung: Ein frisch erzeugter AVD steht auf `hw.gpu.enabled=no` mit `mode=auto`.

### §3.2 Bildrate

Standard ist `hw.lcd.vsync=60`. Ein 120-Hz-Gerät wird damit nie realistisch abgebildet — Animationen
wirken im Emulator anders als in der Hand. Auf `120` setzen.

Trotzdem bleibt der Emulator bei Bewegung systematisch zu gutmütig: Ein Praxisvergleich misst
60 fps im Emulator gegen 48 fps mit Rucklern auf echter Samsung-Hardware. Thermisches Drosseln
bildet er gar nicht ab. **Bewegungsqualität immer am echten Gerät abnehmen.**

---

## §4 Was der Emulator prinzipiell nicht kann: One UI

Samsung bietet unter `developer.samsung.com/galaxy-emulator-skin` kostenlose Emulator-Skins an,
darunter **Galaxy Z Fold8**, Z Fold8 Ultra, Z Flip8, Z TriFold, S26-Reihe, Tab S11 Ultra.
Der Download erfordert einen (kostenlosen) Samsung-Account.

Samsung schreibt dazu ausdrücklich:

> "The Galaxy Emulator Skin defines only the appearance and controls of an Android virtual device,
> which still runs on a stock Android OS. **It does not include any One UI feature**, since it only
> serves as skins for the virtual device."

Ein One-UI-System-Image für den Emulator existiert nicht. Nicht reproduzierbar sind daher:
Samsungs Schriftart, Samsungs Farbsystem-Eingriffe (eigenes Material-You-Derivat seit One UI 4),
Gestennavigation, Taskbar und Flex-Mode auf Foldables, abweichende Animationsdauern,
Quick-Panel-Aufteilung (seit One UI 7), Hersteller-Eigenheiten bei Hintergrundprozessen.

Skin einbinden: entpacken, in der AVD `skin.path` auf den Ordner zeigen lassen bzw. im Device
Manager unter *Default Skin* auswählen. Samsung empfiehlt zusätzlich, unter
*Settings > Tools > Emulator* die Option *Launch in the Running Devices tool window* abzuschalten.
Skin-Einschränkungen: keine Telefonate, kein USB, keine Rückkamera, kein Bluetooth.

---

## §5 Der einzige Weg zu vollständiger Treue: das echte Gerät spiegeln

```powershell
winget install Genymobile.scrcpy
scrcpy --serial <Seriennummer> --window-title Fold8-echt --max-size 1000
```

scrcpy zeigt das angeschlossene Gerät live im Fenster, bedienbar mit Maus und Tastatur — inklusive
One UI, echter Schriftart, echter Farbwiedergabe und echtem Bewegungsverhalten. Für die Frage
"sieht es wirklich so aus?" ist das die Referenz; der Emulator ist das schnelle Arbeitsmittel.

Alternativen mit echten Geräten ohne eigenes Gerät:
- **Samsung Remote Test Lab** (`developer.samsung.com/remote-test-lab`) — kostenlos, echte
  Galaxy-Geräte im Browser fernsteuern, Z Fold8 im Angebot.
- **Android Device Streaming** — echte Geräte in Googles Rechenzentren, direkt aus Android Studio
  (Device Manager → Firebase-Projekt wählen), kann rotieren und falten.

---

## §6 Originalgröße auf dem Monitor (1:1 in Zentimetern)

`emulator -scale` ist **seit Emulator 2.0 abgeschafft** und wird ignoriert ("the '-scale' option is
obsolete as of Emulator 2.0 and will be ignored"). Die Skalierung läuft daher über die Fenstergröße:

```
Fensterinnenbreite [Monitorpixel] = Gerätebreite [Gerätepixel] × (Monitor-ppi / Geräte-ppi)
```

Monitor-ppi aus dem EDID: `Get-CimInstance -Namespace root\wmi -ClassName WmiMonitorBasicDisplayParams`
liefert `MaxHorizontalImageSize`/`MaxVerticalImageSize` in **Zentimetern**.

**Zwei Fallen unter Windows:**
1. `SetProcessDpiAwareness(2)` muss aufgerufen werden, **bevor** die Bildschirmauflösung abgefragt
   wird — sonst meldet ein 2880×1800-Monitor bei 200 % Skalierung nur 1440×900, und der Maßstab
   wird um den Faktor 2 falsch.
2. Ein **minimiertes** Fenster liefert bei `GetClientRect` 0×0 und steht bei −32000/−32000. Vorher
   mit `ShowWindow(h, 9)` wiederherstellen.

Für das Fold 8 auf einem 242,9-ppi-Monitor ergibt sich Maßstab 0,6019 → Fensterinnenbereich
1112 × 1473 px = 11,63 × 15,4 cm. Fertiges Skript: `Werkzeuge/fold8-emulator/Set-Originalgroesse.ps1`.

---

## §7 Änderungen schnell sehen und Elemente zuordnen

### §7.1 Live-Aktualisierung

| Weg | Läuft auf Android? | Grenzen |
|---|---|---|
| **Live Edit** (Android Studio) | ja, Emulator + echtes Gerät | nur Funktionsrümpfe, keine neuen Methoden/Signaturen; App-Zustand geht beim ersten Edit verloren; API 30+, Compose Runtime 1.3+, AGP 8.1+ |
| **Apply Changes** | ja | Praxisurteil: "nowhere near as reliable as Flutter's Hot Reload" |
| **Compose Hot Reload** (JetBrains) | **nein** — Desktop-Target | stabil in Compose Multiplatform 1.10.0, Android-Support laut Doku (07/2026) erst in Erkundung |
| **HotSwan** (Drittanbieter) | laut Eigenwerbung ja, mit Zustandserhalt | Reifegrad nicht belegt |

### §7.2 Vom Bedienelement zum Quellcode

- **Layout Inspector:** *Toggle Deep Inspect* aktivieren → einfacher Klick wählt das Composable und
  zeigt seine Attribute, **Doppelklick** springt in die Quellzeile (Component Tree oder Vorschau).
- **KI-Agent in Android Studio:** *Transform UI* ändert die Oberfläche per natürlicher Sprache aus
  der Vorschau heraus; *Match UI to target image* gleicht an ein Referenzbild an.
- **Einen echten WYSIWYG-Editor für Compose gibt es nicht** und gab es nie.
- **Maschinell zuordnen:** `adb shell uiautomator dump` liefert alle Elemente mit `bounds`,
  `resource-id`, `text`, `content-desc`. Bei reinen Compose-Oberflächen bleibt das dünn, solange
  keine `Modifier.testTag(...)` oder `contentDescription` gesetzt sind — dann liefert die Hierarchie
  im Wesentlichen nur `AndroidComposeView`.
  Fertiges Werkzeug: `Werkzeuge/fold8-emulator/Zeig-Elemente.ps1` (nummeriert alle Elemente im Bild).

---

## §8 Alternativen zum offiziellen Emulator

Für Foldables gibt es keine: In der Recherche ließ sich für **kein** Fremdprodukt eine
Foldable-Unterstützung belegen.

| Produkt | Einschätzung |
|---|---|
| Genymotion | Für Entwickler/QA gebaut, Sensorsimulation, Appium/CI — Foldables nicht belegt |
| Waydroid | Container unter Linux/WSL2, nahe an nativer Geschwindigkeit, aufwendige Einrichtung, kein CI/Appium |
| BlueStacks / LDPlayer / NoxPlayer / MEmu / MuMu | Spiele-Emulatoren, Android 9–13, Werbung, kein CI/Appium |
| Windows Subsystem for Android | **im März 2025 eingestellt** |
| Real-Device-Clouds (Pcloudy u. a.) | Echte Hardware, aber kostenpflichtig |

---

## Quellen

- Lokal ausgelesen: `emulator/lib/hardware-properties.ini`, `emulator -help-gpu`, `emulator -help-scale`,
  `avdmanager list device`, `dumpsys display` / `dumpsys device_state` am SM-F971B
- Samsung Developer: Galaxy Emulator Skin (Übersicht + Guide), Remote Test Lab
- developer.android.com: Layout Inspector, Debug your Compose UI, Live Edit, Emulator Release Notes
- Firebase: Android Device Streaming
- Gist mhazard31 (Beispiel-`config.ini` für Foldable-Controls), Praxisberichte zu Emulator-vs-Gerät
