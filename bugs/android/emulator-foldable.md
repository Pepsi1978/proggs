# Bug-Almanach: Android-Emulator für Foldables

**Stand:** 12.08.2026 · Emulator 36.6.11.0 · System-Image `android-37.0;google_apis_playstore;x86_64`
· Referenzgerät Galaxy Z Fold 8 (SM-F971B, Android 17/API 37, One UI 9.0) · Host Windows 11

Gegenstück (Lösungen/Vorgehen): `best-practices/android/emulator-foldable.md`.
Alle folgenden Punkte wurden am 12.08.2026 auf diesem Rechner **selbst reproduziert**, sofern nicht
als Fremdquelle gekennzeichnet.

---

## 1. Gefalteter Zustand schneidet nur aus, statt neu zu layouten

**Symptom:** Nach `adb emu fold` bzw. `sensor set hinge-angle0 0` wird das Fenster schmal, aber
Inhalte sind abgeschnitten (im Test: Uhr des Sperrbildschirms unten weg).

**Ursache:** `dumpsys window displays` meldet unverändert `cur=1848x2448`. Der Emulator zeigt nur
den `hw.displayRegion.0.1`-Ausschnitt; Android rechnet das Layout **nicht** auf das Cover-Format um.
`dumpsys device_state` wechselt korrekt auf `CLOSED` — der Zustand stimmt also, die Fläche nicht.

**Workaround:** Zweite AVD mit den echten Cover-Maßen (Fold 8: 1248 × 1972 @ 420 dpi) anlegen und
Cover-Darstellung dort prüfen. Die Foldable-AVD bleibt für Posture-/FoldingFeature-Tests.

**Status:** offen (Verhalten von Emulator 36.6.11.0).

---

## 1b. Emulator startet zugeklappt und zeigt die App abgeschnitten

**Symptom:** Die App wirkt "viel zu groß, wie für ein Riesengerät gedacht", links, rechts und unten
fehlt etwas. Das Fenster hat ein schmales Seitenverhältnis (~0,63 statt 0,755), obwohl die AVD auf
1848 × 2448 steht.

**Ursache:** Der Emulator sichert den Faltzustand über Sitzungen hinweg (Schnellstart-Zustand).
Wurde er einmal zugeklappt, startet er wieder zugeklappt — auch nach `emu kill` und Neustart.
`wm size` meldet weiterhin 1848 × 2448, die App wird auch so gerendert, aber nur der
Cover-Ausschnitt wird angezeigt (siehe Punkt 1). Ergebnis: ein vergrößert wirkender Ausschnitt.

**Erkennen:**
```bash
adb -s emulator-5554 emu sensor get hinge-angle0                    # 15 statt 180
adb -s emulator-5554 shell "dumpsys device_state | grep mCommitted" # CLOSED statt OPENED
```
Zweites Indiz: Das Fensterinnenmaß weicht in der **Breite** vom Sollwert ab, während die Höhe passt.

**Fix:** Beim Start immer aufklappen — `adb emu sensor set hinge-angle0 180` — und den Erfolg an
`OPENED` prüfen. In `Start-Fold8.ps1` fest eingebaut.

---

## 2. Neue AVD startet ohne GPU-Beschleunigung

**Symptom:** Träge Darstellung, Grafikeffekte wirken anders als am Gerät.

**Ursache:** `avdmanager create avd` schreibt `hw.gpu.enabled=no` und `hw.gpu.mode=auto` in die
`config.ini`.

**Fix:** `hw.gpu.enabled=yes` und `hw.gpu.mode=host` setzen. Gültige Modi laut `emulator -help-gpu`:
`auto`, `host`, `software`, `lavapipe`, `swiftshader`, `swangle`.

---

## 3. Emulator läuft mit 60 Hz, obwohl das Gerät 120 Hz hat

**Symptom:** Animationen wirken im Emulator flüssiger/anders als in der Hand.

**Ursache:** `hw.lcd.vsync=60` ist Standard, auch bei 120-Hz-Geräteprofilen.

**Fix:** `hw.lcd.vsync=120`. Restrisiko bleibt: Ein Praxisvergleich misst 60 fps im Emulator gegen
48 fps mit Rucklern auf echter Samsung-Hardware; thermisches Drosseln bildet der Emulator nie ab.

---

## 4. `avdmanager` lässt Platzhalter in der config.ini stehen

**Symptom:** `avd.id=<build>` und `avd.name=<build>` statt des AVD-Namens.

**Fix:** Beide Zeilen von Hand auf den AVD-Namen setzen. Ebenso `hw.device.hash2` entfernen, wenn
man die Maße gegenüber dem Basisprofil ändert — sonst meldet Android Studio ein abweichendes Profil.

---

## 5. `-scale` wird stillschweigend ignoriert

**Symptom:** `emulator -avd X -scale 0.6` hat keine Wirkung.

**Meldung:** "the '-scale <scale>' option is obsolete as of Emulator 2.0 and will be ignored".

**Fix:** Maßstab über die Fenstergröße setzen (`SetWindowPos` auf berechnete Innenmaße).

---

## 6. Windows meldet die halbe Auflösung — Maßstab wird Faktor 2 falsch

**Symptom:** Berechneter Monitor-ppi ist genau halb so groß wie erwartet (gemessen: 121,5 statt
242,9), das Fenster wird doppelt zu klein.

**Ursache:** Bei 200 % Anzeigeskalierung liefert `[System.Windows.Forms.Screen]` skalierte statt
echter Pixel, solange der Prozess nicht DPI-bewusst ist.

**Fix:** `SetProcessDpiAwareness(2)` aufrufen, **bevor** irgendeine Bildschirm- oder Fenstergröße
abgefragt wird. Reihenfolge ist entscheidend — ein späterer Aufruf wirkt nicht mehr.

---

## 7. Minimiertes Fenster liefert Größe 0 × 0

**Symptom:** `GetClientRect` gibt 0 × 0 zurück, `GetWindowRect` meldet Position −32000/−32000.

**Ursache:** Das Fenster ist minimiert.

**Fix:** Vorher `IsIconic()` prüfen und mit `ShowWindow(h, 9)` (SW_RESTORE) wiederherstellen.

---

## 8. Screenshot über die PowerShell-Pipe wird zerstört

**Symptom:** `adb exec-out screencap -p | Set-Content -Encoding Byte` bricht ab
("Die Bytecodierung kann nicht fortgesetzt werden") bzw. liefert unbrauchbare Dateien.

**Ursache:** Die PowerShell-Pipe behandelt die Binärdaten als Text.

**Fix:** Umweg über das Gerät: `adb shell "screencap -p /sdcard/shot.png"` und danach `adb pull`.

---

## 9. `adb pull` gilt als Fehler, obwohl es funktioniert hat

**Symptom:** Bei `$ErrorActionPreference = "Stop"` bricht das Skript mit `NativeCommandError` ab —
obwohl die Meldung "1 file pulled" lautet.

**Ursache:** adb schreibt auch Erfolgsmeldungen auf die Fehlerausgabe.

**Fix:** `$ErrorActionPreference = "Continue"` und Ergebnisse selbst prüfen (`Test-Path`, Dateigröße).

---

## 10. PowerShell 5.1 kennt `??` nicht

**Symptom:** `Unerwartetes Token "??" in Ausdruck oder Anweisung`.

**Ursache:** Skripte, die über `powershell -File` laufen, landen in Windows PowerShell 5.1, nicht in
pwsh 7 — der Null-Coalescing-Operator existiert dort nicht.

**Fix:** `if (-not $x) { $x = $ersatz }` statt `$x ?? $ersatz`. Gilt auch für `?.` und den
Ternär-Operator.

---

## 11. Samsungs Emulator-Skin bringt kein One UI

**Symptom:** Erwartung, mit dem Galaxy-Z-Fold8-Skin die echte Samsung-Oberfläche zu sehen.

**Fremdquelle (Samsung Developer, Guide):** "The Galaxy Emulator Skin defines only the appearance
and controls of an Android virtual device, which still runs on a stock Android OS. **It does not
include any One UI feature**."

**Konsequenz:** Schriftart, Farbsystem, Gestennavigation, Taskbar/Flex-Mode und Animationsdauern von
One UI sind im Emulator grundsätzlich nicht prüfbar. Skin-Grenzen zusätzlich: keine Telefonate,
kein USB, keine Rückkamera, kein Bluetooth. Für echte Treue das Gerät spiegeln (scrcpy).

---

## 11b. Samsung-Skin sprengt den Bildschirm und macht den Emulator unbedienbar

**Symptom:** Nach dem Einbinden des Galaxy-Z-Fold8-Skins startet das Emulator-Fenster teilweise
außerhalb des Bildschirms. Die Titelleiste liegt oberhalb des sichtbaren Bereichs, das Fenster lässt
sich nicht mehr verschieben, die seitliche Bedienleiste fällt aus dem Bild.

**Ursache:** Der Emulator stellt Skins **1:1 in Skin-Auflösung** dar und skaliert sie nicht
(`-scale` ist abgeschafft, siehe Punkt 5). Samsungs Skin-Layouts sind größer als übliche Bildschirme:

| Skin | Layoutgröße | passt auf 2880 × 1800? |
|---|---|---|
| Galaxy_Z_Fold8_Main_Screen | 2885 × 2261 | nein (zu breit **und** zu hoch) |
| Galaxy_Z_Fold8_Cover_Screen | 1701 × 2388 | nein (zu hoch) |

**Zweiter Effekt:** Der Skin überschreibt die Displaymaße der AVD. Aus 1848 × 2448 (hochkant) wird
2448 × 1848 — der Emulator zeigt dann quer, obwohl `hw.initialOrientation=portrait` gesetzt ist.

**Fix:** Auf diesen Bildschirmgrößen den Skin weglassen (`showDeviceFrame=no`, keine `skin.path`-
Zeile). Ein Herunterskalieren der Skin-Grafiken hilft nicht, weil dann auch die Display-Definition
im `layout` kleiner werden müsste — das senkt die Emulator-Auflösung unter die des echten Geräts.
Der Skin liefert ohnehin nur den Gehäuserahmen, kein One UI (Punkt 11).

**Ergänzend:** Automatische Drehung abschalten, damit nichts von selbst ins Querformat kippt:
`adb shell settings put system accelerometer_rotation 0`.

---

## 12. Falsches System-Image bei 16-KB-Seiten

**Symptom:** `ps16k`-Image gewählt, obwohl das Zielgerät mit 4-KB-Seiten läuft (oder umgekehrt).

**Prüfung:** `adb shell getconf PAGESIZE` — 4096 = 4 KB, 16384 = 16 KB. Das Fold 8 meldet 4096.

**Konsequenz:** Nur bei 16384 ein `ps16k`-Image nehmen; sonst weicht die Speicherseiten-Größe von
der des Geräts ab (relevant für nativen Code/NDK).

---

## 13. Schrift im Emulator größer als am Gerät

**Symptom:** Texte brechen im Emulator anders um als in der Hand.

**Ursache:** Nutzer-Schriftskalierung am Gerät ungleich 1.0 (hier gemessen: 0.9), Emulator startet
mit 1.0.

**Fix:** `adb shell settings get system font_scale` am Gerät lesen, im Emulator per
`settings put system font_scale <Wert>` setzen.

---

## 14. Compose-Oberflächen liefern kaum UI-Hierarchie

**Symptom:** `uiautomator dump` zeigt bei Compose-Bildschirmen im Wesentlichen nur
`AndroidComposeView` ohne benennbare Kinder.

**Ursache:** Compose erzeugt keine klassischen Views mit `resource-id`.

**Fix:** `Modifier.testTag("...")` bzw. `contentDescription` setzen — dann tauchen die Elemente in
der Hierarchie auf. Alternativ Layout Inspector nutzen (liest die Compose-Hierarchie direkt).

---

## 15. Firecrawl-Recherche liefert 0 Quellen bei langen Fragen

**Symptom:** Researcher meldet "0 Quellen geholt", die Auswertung ist wertlos.

**Ursache:** Die komplette Frage wird als Suchanfrage benutzt; sehr lange, mehrsätzige Fragen
finden nichts.

**Fix:** Für Engine A kurze Stichwort-Queries formulieren (5–10 Wörter). Lange, präzise
Formulierungen gehören in den Auswertungs-Prompt, nicht in die Suchanfrage.

---

## 16. `getevent` sieht Klicks aus scrcpy und `input tap` nicht

**Symptom:** Ein Werkzeug soll mithören, wo der Benutzer tippt, und horcht dafür auf
`adb shell getevent`. Bei echten Fingertipps auf dem Glas kommen Ereignisse an — bei Mausklicks
im scrcpy-Fenster und bei `adb shell input tap` bleibt der Strom stumm.

**Ursache:** scrcpy und `input` speisen Ereignisse über die InputManager-Schnittstelle ein. Die
laufen am Kernel-Eingabegerät (`/dev/input/eventN`) vorbei, das `getevent` ausliest. Der
InputDispatcher sieht sie, der Kernel nicht.

**Nachweis (12.08.2026, Galaxy Z Fold 8 / One UI 8):** `getevent -lc 6` im Hintergrund starten,
dann `input tap 600 430` — die Ausgabe enthält nur die Geräteliste, kein einziges Ereignis.

**Fix:** Nicht auf dem Gerät mithören, sondern die Mausposition auf der PC-Seite abgreifen
(Win32 `GetCursorPos` + `ScreenToClient` auf das scrcpy-Fenster) und über `wm size` in
Gerätekoordinaten umrechnen. Umgesetzt in `Werkzeuge/zeigefinger/`.

**Sackgasse, die man sich sparen kann:** `dumpsys input` → `RecentQueue` listet die letzten
MotionEvents zwar auf (auch die eingespeisten), aber **ohne x/y-Koordinaten** — nur `age=…ms`.

---

## 17. Touch-Rohwerte sind 0–4095, nicht Pixel — und Foldables haben zwei Touchscreens

**Symptom:** Aus `getevent` gelesene `ABS_MT_POSITION_X/Y` ergeben absurde Koordinaten weit
außerhalb des Bildschirms.

**Ursache:** Der Touch-Controller meldet einen geräteeigenen Wertebereich. Am Fold 8 ist er für
**beide** Achsen 0–4095, obwohl das Display 1248×1972 (zugeklappt) bzw. 2448×1848 (aufgeklappt)
hat. Zusätzlich existieren zwei getrennte Eingabegeräte — `sec_touchscreen` und
`sec_touchscreen2` — mit identischem Rohbereich, aber unterschiedlichen Zieldisplays.

**Fix:** Linear skalieren mit der **aktuellen** Displaygröße: `pixel = roh / 4095 * kante`, und
`adb shell wm size` bei jeder Messung neu abfragen — der Wert ändert sich beim Auf- und
Zuklappen mitten in der Sitzung. Die Zuordnung Eingabegerät→Display steht in `dumpsys input`
(Achsen mit `source=TOUCHSCREEN` gegen `max=`-Werte vergleichen), lässt sich aber ganz umgehen,
wenn man ohnehin von der PC-Seite her rechnet (siehe §16).

---

## 18. Der Emulator kennt keine Startgröße — Fenster erscheint immer bildschirmfüllend

**Symptom:** Der Emulator startet riesig und teils außerhalb des Bildschirms; erst ein
nachträgliches Zurechtrücken bringt ihn in Form. Das sichtbare Springen wirkt wie "30 Resize-
Versuche".

**Ursache — alle drei denkbaren Wege sind Sackgassen (am 12.08.2026 durchgetestet):**

| Weg | Ergebnis |
|---|---|
| `-scale 0.6` | "obsolete as of Emulator 2.0 and will be ignored" |
| `-window-size` | laut `emulator -help` **nur für Fuchsia** |
| `emulator-user.ini` (`window.x/y`, `window.scale`) | wird beim Kaltstart **ignoriert** und beim Beenden mit `window.scale = -1.000000`, `window.x = 100`, `window.y = 100` **überschrieben** |

Gemessen: Vorgabe `window.scale = 0.5725` → tatsächliche Darstellung mit Faktor 0,891, also
bildschirmfüllend (1972 px × 0,891 = 1757 px bei 1800 px Bildschirmhöhe), Position −172 (oben raus).

**Fix (Poka-Yoke Stufe 2, Stufe 3 ist hier nicht erreichbar):** Nach dem Start **einmal** per
`SetWindowPos` Größe **und** Position gemeinsam setzen — und zwar so früh wie möglich, nämlich
sobald das Fenster existiert (`-Warten`), nicht erst nach `sys.boot_completed`. Danach genau eine
Kontrollmessung und höchstens eine Nachkorrektur. Umgesetzt in `Set-Originalgroesse.ps1`.

**Gegenbeispiel scrcpy:** Dort geht Stufe 3 — `--window-width/--window-height/--window-x/--window-y`
werden beim Start akzeptiert, die Spiegelung steht sofort maßstabsgetreu.

---

## 19. Falsches Display verglichen: Innendisplay gegen Cover-Display

**Symptom:** "Die App ist viel zu groß, als wäre sie für ein Riesengerät gedacht" — obwohl die
Skalierung rechnerisch stimmt.

**Ursache:** Verglichen wurde der Emulator (aufgeklapptes **Innendisplay**, 7,6″, 11,63 cm breit,
704 dp) mit dem echten Handy **im zugeklappten Zustand** (**Cover-Display**, 5,5″, 7,47 cm breit,
475 dp). Zwei verschiedene Bildschirme desselben Geräts — das Innendisplay hat Tablet-Fläche.

**Fix (Poka-Yoke Stufe 3):** Der Emulator folgt dem angeschlossenen Gerät. `Start-Fold8.ps1` liest
`adb shell wm size` des echten Fold und wählt die passende AVD:
`1248x1972` → `Fold8_Cover`, `1848x2448`/`2448x1848` → `Fold8`. Mit `-Cover`/`-Innen` überschreibbar.
Damit kann das falsche Display strukturell nicht mehr gezeigt werden.

---

## 20. Maßstab stimmt nur auf dem Hauptmonitor

**Symptom:** Auf einem zweiten Bildschirm ist das Fenster falsch groß.

**Ursache:** Die Monitor-ppi wurde aus `Screen::PrimaryScreen` und dem ersten EDID-Eintrag
berechnet — unabhängig davon, auf welchem Bildschirm das Fenster tatsächlich liegt.

**Fix:** `Screen::FromHandle(fensterHandle)` verwenden und den Maßstab über das Verhältnis der
Bildschirmdiagonalen anpassen. Präventiv eingebaut, bevor der Fehler auftrat.

---

## 21. Falscher Absturz-Alarm: fremder Systemabsturz wird der App zugeschrieben

**Symptom:** `Start-Fold8.ps1 -Projekt X` meldet "DIE APP IST ABGESTUERZT" und zeigt eine
Bluetooth-Zeile (`system/gd/hci/hci_layer.cc:565 on_hardware_error: Hardware Error Event with
code 0x42`) — die App läuft in Wahrheit einwandfrei im Vordergrund.

**Ursache (zwei Fehler zugleich):**

1. Der Puffer `logcat -b crash` sammelt die Abstürze **aller** Prozesse des Systems. Beim Boot
   stürzt auf dem Emulator regelmäßig `com.android.bluetooth` ab. Die Prüfung suchte nach
   `beginning of crash` — diese Trennzeile steht im Puffer, sobald **irgendein** Prozess abstürzt.
2. Nach dem Start wurde genau 5 Sekunden gewartet und einmal `pidof` abgefragt. Ein Kaltstart
   dauert auf dem Emulator länger; die leere Antwort galt dann fälschlich als Absturz.

Verstärkend: Der Zeilenfilter `Error:` trifft wegen des case-insensitiven `-match` auch
`on_hardware_error:` — deshalb wurde ausgerechnet die Bluetooth-Zeile als Ursache angezeigt.

**Fix (Poka-Yoke Stufe 3 — falscher Alarm ist strukturell nicht mehr möglich):**
Ein Absturz zählt nur noch, wenn eine Zeile des Puffers den **eigenen Paketnamen** nennt
(`Process: <paket>` bei Java-Abstürzen, `>>> <paket> <<<` bei nativen Tombstones). Fremde
Prozesse können die Bedingung nicht mehr erfüllen — unabhängig davon, welche Wörter ihre
Meldung enthält. Statt der festen Wartezeit wird bis zu 20 s im Sekundentakt auf den Prozess
gewartet. Fehlt der Prozess ohne eigenen Crash-Eintrag, lautet die Meldung "DIE APP LAEUFT NICHT"
(nicht gestartet) statt "abgestürzt".

**Funktionserhaltung geprüft:** Ein echter App-Absturz (`FATAL EXCEPTION` mit
`Process: de.frank.entropyreducer.debug`) wird weiterhin erkannt und mit Stacktrace gezeigt.

**Verwandte Stelle:** `Zeig-Fehler.ps1` filtert dasselbe Rauschen bereits über `$nativesRauschen`
(`F DEBUG|libbluetooth|…`) — die Absturz-Erkennung in `Start-Fold8.ps1` war eine zweite,
schwächere Kopie dieser Logik. Wer eine der beiden ändert, prüft immer auch die andere.
