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

## 18b. Kette umgangen — Fenster startet außerhalb des Bildschirms

**Symptom:** Der Emulator startet halb außerhalb des Bildschirms; oben ragt er hinaus, die
Titelleiste ist mit der Maus nicht mehr greifbar. Gemessen am 13.08.2026: x=1019, **y=−648**
bei einem Bildschirm von 2880 × 1800.

**Ursache — nicht der Emulator, sondern der Startweg:** `emulator.exe -avd Fold8` wurde direkt
aufgerufen statt `Start-Fold8.ps1`. Damit lief weder die Größenkorrektur (#18) noch die
Zentrierung noch der Wächter (#22); der Emulator wählt seine Position dann selbst, und
`emulator-user.ini` (`window.x/y = 100`) wird beim Kaltstart ignoriert. Aufschlussreich: Das
Fenster war mit 666 × 1083 klein genug für den Bildschirm — **nur die Position war falsch**.
Die Absicherung existierte also längst, sie war nur umgehbar.

**Fix (Poka-Yoke Stufe 2 + Regel):**
- Hook `emulator-start-guard.{py,ps1,sh}` (PreToolUse, Matcher `Bash|PowerShell`) blockiert jeden
  Aufruf, der `emulator[.exe]` mit `-avd` (oder der Kurzform `@<avd>`) kombiniert, per
  `permissionDecision=deny` und nennt den richtigen Befehl. `Start-Fold8.ps1` ist ausgenommen.
  Logik in einer gemeinsamen `.py` (kein `jq`, siehe `claude-tooling/claude-hooks.md` §16.2), die
  beiden Wrapper können nicht auseinanderlaufen. Notaus: leere Datei
  `emulator-start-guard-disable.flag` im TEMP.
- Regel `~/.claude/rules/android-emulator-werkzeugkette.md`.

**Falle beim Guard-Bau:** Der erste Regex verlangte eine Wortgrenze vor `emulator` und ließ damit
ausgerechnet die PowerShell-Schreibweise `& $emulator -avd Fold8` durch — `$` (und `&`, `(`, `=`)
gehören mit in die Zeichenklasse. Beim Testen aufgefallen, nicht erst im Betrieb.

---

## 18c. Zentrierung sitzt daneben — nach Sollmaß statt Istmaß gerechnet

**Symptom:** Das Fenster steht vertikal mittig, horizontal aber sichtbar daneben (gemessen: Rand
links 871, rechts 1051 — 90 px aus der Mitte).

**Ursache:** `Set-Originalgroesse.ps1` zentrierte nach der **angeforderten** Fenstergröße. Der
Emulator zieht das Fenster danach auf sein eigenes Seitenverhältnis zusammen (angefordert 1138
breit, tatsächlich 958) — und zwar **verzögert**, also auch nach der einen Nachkorrektur noch.
Die Höhe stimmte zufällig (1544 vs. 1545), deshalb fiel nur die Breite auf. Rechnerisch
nachweisbar: 871 = (2880 − 1138)/2, also mit dem Sollwert gerechnet.

**Fix:** Zum Schluss warten, bis zwei `GetWindowRect`-Messungen dieselbe Größe liefern
(max 10 × 150 ms), dann nach dem **Istmaß** zentrieren — dabei nur die Position setzen
(`SWP_NOSIZE`), nie die Größe, sonst wäre die eben gesetzte 1:1-Größe wieder hin. Zusätzlich
fängt `Mittig()` jetzt allseitig ein (auch rechts und unten, vorher nur oben/links) und deckelt
die Zielgröße auf die Arbeitsfläche minus 24 px Mindestrand; passt die Originalgröße nicht auf
den Monitor, wird proportional verkleinert und das **gemeldet**, statt das Fenster stumm über
den Rand laufen zu lassen. Gemessen nachher: links 961, rechts 961.

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

---

## 22. Fenster wird beim Drehen falsch groß und bleibt es

**Symptom:** Nach einer Drehung stimmt der Maßstab nicht mehr — mal viel zu klein, mal springt das
Fenster zwischen zwei Größen hin und her. Bei mehrfachem Drehen (5-7 ×) wird es nie wieder richtig.

**Ursache (drei Fehler übereinander):**

1. Die Fenstergröße wurde als **Einmal-Aktion** beim Start gesetzt. Der Emulator ändert sie beim
   Drehen aber selbst — danach stimmt der Maßstab nicht mehr, und nichts führt ihn nach.
2. `settings put system accelerometer_rotation 0` ("Hochformat festhalten") macht den **Dreh-Knopf
   des Emulators wirkungslos** — es sieht aus, als ginge die Rotation gar nicht.
   `user_rotation` wird bei eingeschalteter Auto-Drehung ignoriert.
3. In der **180-Grad-Lage** dreht der Emulator seinen Fensterrahmen ins Hochformat, Android bleibt
   aber quer (Hochformat kopfüber gibt es auf Handys nicht). Rahmen und Inhalt passen nicht
   zusammen; das Fenster lässt sich in dieser Lage **nicht** auf die richtige Größe zwingen — der
   Emulator setzt sein Rahmenverhältnis sofort zurück (beobachtet: 500 × 790 statt 1129 × 715).

**Fix (`Set-Originalgroesse.ps1 -Ueberwachen`, von `Start-Fold8.ps1` automatisch gestartet):**

- Ein **Wächter** liest alle 250 ms `cur=BxH` und `mRotation` und setzt das Fenster bei jeder
  Änderung neu. Der Sollwert wird immer frisch aus dem gemeldeten Zustand gerechnet, nie
  fortgeschrieben — deshalb ist die Anzahl der Drehungen egal.
- Ausrichtung über den **Lagesensor** (`emu sensor set acceleration 0:9.81:0` = Hochformat) statt
  über `user_rotation`, und `accelerometer_rotation` bleibt auf 1, damit der Dreh-Knopf wirkt.
- Die kaputte 180-Grad-Lage wird **ins Hochformat aufgelöst**: Der Lage-Abgleich läuft in **jedem**
  Durchlauf (nicht nur beim Zustandswechsel — der Emulator stellt sein Fenster erst kurz *nach* der
  Android-Meldung um) und misst das **unangetastete** Fenster, dessen Verhältnis immer dem
  Emulator-Rahmen entspricht. Steht es länger als 1,2 s quer zu dem, was Android meldet, setzt der
  Wächter den Lagesensor auf Hochformat (`emu sensor set acceleration 0:9.81:0`) — höchstens alle
  6 s und maximal zweimal in Folge.

  **Nicht weiterdrehen!** Ein erster Ansatz sendete stattdessen `emu rotate`. Damit sprang die
  Ansicht nach zwei Drehungen sichtbar ins **Querformat** zurück — das erwartet niemand. Ein echtes
  Handy zeigt kopfüber gehalten **Hochformat**, weil Android Upside-Down nicht unterstützt. Genau
  dieser Zustand wird jetzt hergestellt: Rahmen und Anzeige stehen beide hochkant, das Bild ist
  aufrecht und maßstabsgetreu.

- **Position bleibt erhalten:** Beim Drehen wird nur die Größe gesetzt, nie zentriert. Sonst springt
  das Fenster bei jeder Drehung in die Bildschirmmitte zurück (gemessen: von 120,120 nach 1071,252).
  Mittig gestellt wird ausschließlich einmal beim Start; ragt das Fenster nach dem Vergrößern über
  den Rand, wird es nur so weit hineingeschoben wie nötig.

**Fallstrick — der Fix kann das Problem verschlimmern (zweimal passiert):**

1. *Größe gegen die Rahmenlage setzen:* Zwingt man ein Querformat-Maß auf einen hochkant stehenden
   Emulator-Rahmen, zeichnet der Emulator den Inhalt um **90 Grad gekippt** — der Text liegt
   seitwärts. Das ist deutlich schlimmer als eine zu kleine Anzeige. Deshalb gilt: **In einer
   schiefen Lage wird die Größe nie gesetzt.**
2. *Zur falschen Zeit weiterdrehen:* Ein Übersprung mit untauglichem Kriterium (gemessen nach dem
   eigenen Setzen statt am unangetasteten Fenster) dreht den Emulator zusätzlich zur Drehung des
   Benutzers. Rahmen und Anzeige laufen dann **dauerhaft** auseinander — das Bild bleibt gekippt.

**Stabilität des Wächters (drei Sicherungen):** systemweiter Mutex (zwei Wächter würden sich
überstimmen — das war die Ursache für sichtbares Hin- und Herspringen) · Entprellen über zwei
Messungen plus 1,2 s Ruhe nach jedem Eingriff (sonst regelt er gegen sich selbst) · Ping-Pong-Bremse
mit **selbstlösender** Pause (eine Sperre, die sich nicht von selbst löst, lässt das Fenster nach
mehrfachem Drehen dauerhaft falsch stehen — genau das war ein Zwischenstand dieses Fixes).

**Regressionstest (`Pruefe-Massstab.ps1`):** 10 × Dreh-Knopf sowie 6 × links und 6 × rechts über den
Lagesensor. Geprüft werden **zwei** Dinge pro Drehung — Größe (Fenster-Ist gegen
`cur × Monitor-ppi / Geräte-ppi`, Toleranz 6 px) **und Lage** (haben Fenster und Anzeige dieselbe
Ausrichtung?). 23 von 23 bestanden.

**Warum die Lage mitgeprüft werden MUSS:** Eine Prüfung, die nur Zahlen vergleicht, meldet „alles
1:1", während der Emulator den Inhalt gekippt zeichnet — die Maße stimmen dabei nämlich. Genau
dieser blinde Fleck ließ einen kaputten Zustand als bestanden durchgehen.

**Messfalle bei der Prüfung:** `GetClientRect` liefert ohne `SetProcessDpiAwareness(2)` skalierte
Werte, und die Selbstkontrolle rechnet den Fehler wieder heraus — sie sieht dann richtig aus,
obwohl das Fenster physisch falsch ist. Bei Zweifeln gegen einen Vollbild-Screenshot in echter
Auflösung prüfen (hier: 2880 × 1800, EDID 30 × 19 cm → 242,9 ppi).

---

## 23. Zugeklappt ist der Normalfall, nicht aufgeklappt

**Symptom:** Der Emulator startet immer mit dem großen Innendisplay, obwohl das Gerät im Alltag
zugeklappt benutzt wird.

**Fix:** `Start-Fold8.ps1` startet standardmäßig die AVD `Fold8_Cover`; `-Innen` schaltet auf das
große Display. Läuft bereits ein Emulator mit der anderen AVD, wird sie **gewechselt** (alte
beenden, richtige starten, App mitnehmen) statt stillschweigend das falsche Format zu zeigen.
Zum Wechseln im Betrieb: `Klappen.ps1 -Auf` / `-Zu` — der Falt-Knopf im Emulator taugt dafür
nicht (siehe #1).

---

## 24. Ton knistert und hackt — nur im Emulator, nicht auf echter Hardware

**Symptom:** Die Tonwiedergabe im Emulator klingt zerhackt, als bräche sie ständig kurz ab.
Dieselbe APK klingt auf dem echten Gerät völlig sauber, und anderer Ton auf dem PC (Musik,
Video) läuft ebenfalls störungsfrei — betroffen ist ausschließlich der Emulator.

**Stand 13.08.2026 — UNGELÖST.** Der Abschnitt hält den Ausschlussweg fest, damit niemand ihn
ein zweites Mal durchläuft.

**Was nachweislich NICHT die Ursache ist** (jeweils gemessen, nicht vermutet):

| Verdacht | Widerlegung |
|----------|-------------|
| Die App / ihre Audio-Verstärkung | Ohne jede Verstärkung knistert es identisch; auf echter Hardware nie |
| Zu hoher Pegel / Übersteuerung | Auch bei Gain 0 unverändert |
| Dauerlast durch Poll-Prozesse | Größen-Wächter (4 adb-Abfragen/s) gestoppt → keine Änderung |
| Zu wenig Rechenzeit | Priorität von `qemu-system-x86_64` auf Hoch **und** Echtzeit → keine Änderung |
| Aussetzer im Gast | `dumpsys media.audio_flinger`: `raw underrun counters: partial=0 empty=0`, Track-`Underruns=0` — Android liefert lückenlos |
| Abtastraten-Umrechnung | Windows-Ausgabe 48000 Hz/32 bit = exakt die Emulator-Mixerrate |
| Veralteter Emulator | 37.1.11.0 ist die neueste Version, kein Update verfügbar |
| Windows-Audio allgemein (DPC-Latenz, Realtek-Effekte, Energieplan) | Anderer Ton auf demselben PC ist sauber |

**Zwei Hebel, die gar nicht ankommen** (wichtigste Lehre): `-audio dsound` und die
DirectSound-Umgebungswerte (`QEMU_DSOUND_LATENCY_MILLIS`, `QEMU_DSOUND_BUFSIZE_OUT`) erscheinen
**nicht** in der QEMU-Befehlszeile, die der Emulator tatsächlich startet. Sie sind wirkungslos,
nicht "getestet und verworfen". Sichtbar wird das nur mit `-debug audio,audioout,init`
(`Start-Fold8.ps1 -TonProtokoll`, Log in `%TEMP%\fold8-audio-debug.log`): dort steht die
vollständige Zeile, u. a. `-soundhw virtio-snd-pci`. **Vor jedem Ton-Schalter erst prüfen, ob er
in dieser Zeile auftaucht.**

**Sackgasse Intel-HDA:** Das Emulator-Feature `VirtioSndCard` (in `emulator/lib/advancedFeatures.ini`,
Standard `on`) lässt sich per `-feature -VirtioSndCard` abschalten, dann startet QEMU mit
`-soundhw hda`. Das kommt zwar an, führt auf dem Image `android-37 google_apis_playstore` aber zu
**gar keinem Ton** — Android bringt für die HDA-Karte keinen Treiber mehr mit. Nur zum
Vergleichsmessen brauchbar, nie als Dauerzustand; `Start-Fold8.ps1 -TonGeraet hda` warnt deshalb.

**Nebenwirkung:** Ein Wechsel der Soundkarte ändert die Gerätebestückung. Wird danach der
Schnappschuss geladen, bleibt der Bildschirm schwarz → **Kaltstart Pflicht**. `Start-Fold8.ps1`
erzwingt ihn bei gesetztem `-TonGeraet` selbst.

**Empfohlener Umgang:** Emulator zum Sehen und Bedienen, Klangprüfung auf dem echten Gerät.

**Falsche Fährte bei der Diagnose:** Ein schwarzer Emulator-Bildschirm bedeutet nicht, dass
Android hängt — es kann schlicht die Bildschirmsperre sein. Und ein per Pipe geholter Screenshot
(`adb exec-out screencap -p > datei`) ist unter Git Bash/PowerShell verstümmelt (hier konstant
14 KB statt 1,2 MB); immer `screencap` auf dem Gerät + `adb pull` (siehe #8).

---

## 25. `screencap` stellt dem PNG eine Warnung voran — nur auf echter Foldable-Hardware

**Symptom:** Werkzeuge, die `adb exec-out screencap -p` binär einlesen und auf die PNG-Signatur
prüfen, funktionieren am Emulator und scheitern am echten Fold 8 mit „Screenshot konnte nicht
gelesen werden". Die Daten sind da (hier 418 666 Byte), beginnen aber nicht mit `\x89PNG`.

**Ursache:** Das Fold 8 hat zwei physische Displays. Ohne `-d` schreibt `screencap` **347 Byte
Klartext vor die Bilddaten** — auf **stdout**, nicht auf stderr:

```
[Warning] Multiple displays were found, but no display id was specified! Defaulting to the
first display found, however this default is not guaranteed to be consistent across captures.
A display id should be specified.
```

Der Emulator hat nur ein Display und gibt die Warnung nie aus. Deshalb fällt das erst auf echter
Hardware auf. Gemessen am 14.08.2026 auf SM-F971B.

**Fix:** Nicht auf `startswith(b"\x89PNG")` prüfen, sondern die Signatur **suchen** und davor alles
abschneiden:

```python
start = roh.find(b"\x89PNG\r\n\x1a\n")
bild = roh[start:] if start != -1 else None
```

**Zusatz:** Die Warnung meint es ernst — welches Display „das erste" ist, ist nicht garantiert.
Sicher wird es erst, wenn man die Maße im IHDR (Byte 16–24 des PNG) gegen `wm size` prüft und bei
Abweichung die Display-Kennungen aus `dumpsys SurfaceFlinger --display-id` mit `screencap -d <id>`
durchprobiert. Im Werkzeug `Werkzeuge/zeigefinger` ist das als `screenshot()` umgesetzt.

**Status:** Verhalten von Android 17 / One UI 9 auf Multi-Display-Geräten, kein Fehler im engeren
Sinn — aber eine sichere Falle für jedes Werkzeug, das nur am Emulator entwickelt wurde.
