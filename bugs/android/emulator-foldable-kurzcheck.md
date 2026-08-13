# Android-Emulator für Foldables — Bug-Kurzcheck

> **Stufe A.** Trifft ein Punkt zu oder tritt der Fehler auf: den Abschnitt im Volltext lesen
> (`emulator-foldable.md`, gleiche Nummer).

**Stand:** 12.08.2026 · Emulator 36.6.11.0 · Android 17/API 37 · Referenz SM-F971B

| # | Symptom | Kern in einem Satz |
|---|---------|--------------------|
| 1 | Zugeklappt sind Inhalte abgeschnitten | `displayRegion` schneidet nur aus, Layout wird nicht umgerechnet → zweite AVD fürs Cover |
| 1b | App wirkt zu groß, Ränder fehlen | Emulator startete zugeklappt (Zustand überlebt Neustart) → `emu sensor set hinge-angle0 180`, auf `OPENED` prüfen |
| 2 | Emulator träge, Effekte falsch | Neue AVD steht auf `hw.gpu.enabled=no` → auf `yes` + `mode=host` |
| 3 | Animationen wirken anders als am Gerät | `hw.lcd.vsync` steht auf 60 → auf 120 setzen |
| 4 | `avd.name=<build>` in der config.ini | Platzhalter von `avdmanager`, von Hand setzen; `hw.device.hash2` entfernen |
| 5 | `-scale` wirkt nicht | Seit Emulator 2.0 abgeschafft, wird ignoriert → Fenstergröße setzen |
| 6 | Monitor-ppi genau halbiert | `SetProcessDpiAwareness(2)` fehlte **vor** der Abfrage (200 % Skalierung) |
| 7 | Fenstergröße kommt als 0 × 0 | Fenster minimiert (−32000) → vorher `ShowWindow(h, 9)` |
| 8 | Screenshot über Pipe kaputt | PowerShell zerstört Binärdaten → `screencap` auf dem Gerät + `adb pull` |
| 9 | `adb pull` bricht Skript ab | adb meldet Erfolg auf stderr → `ErrorActionPreference = "Continue"` |
| 10 | `Unerwartetes Token "??"` | `powershell -File` ist 5.1, kein pwsh 7 → kein `??`, `?.`, kein Ternär |
| 11 | Kein One UI trotz Samsung-Skin | Skin liefert nur Rahmen, laut Samsung "does not include any One UI feature" |
| 11b | Fenster ragt aus dem Bildschirm, quer statt hochkant | Samsung-Skin ist 2885 × 2261 groß und wird nie skaliert → auf 2880 × 1800 weglassen (`showDeviceFrame=no`) |
| 12 | Falsches System-Image | `getconf PAGESIZE` prüfen: 4096 → **kein** `ps16k`-Image |
| 13 | Schrift größer als am Gerät | `font_scale` des Geräts auslesen und im Emulator setzen |
| 14 | `uiautomator dump` fast leer | Compose ohne `testTag`/`contentDescription` → Layout Inspector nehmen |
| 15 | Recherche liefert 0 Quellen | Firecrawl-Query zu lang → kurze Stichwort-Queries |
| 16 | `getevent` sieht scrcpy-/`input tap`-Klicks nicht | Eingespeiste Events umgehen `/dev/input` → Maus auf der PC-Seite abgreifen; `dumpsys input` `RecentQueue` hat **keine** Koordinaten |
| 17 | Touch-Koordinaten absurd groß | Rohbereich ist 0–4095, nicht Pixel; Fold hat zwei Touchscreens → `roh / 4095 * kante`, `wm size` jedes Mal neu lesen |
| 18 | Emulator startet riesig/außerhalb, ständiges Nachrücken | Er kennt **keine** Startgröße (`-scale` tot, `-window-size` nur Fuchsia, `emulator-user.ini` wird überschrieben) → einmal per `SetWindowPos` setzen, sobald das Fenster da ist |
| 24 | Ton knistert/hackt NUR im Emulator (echtes Geraet sauber) | UNGELOEST. Gast liefert lueckenlos (Underruns=0); `-audio`/DSOUND-Puffer kommen gar nicht an (mit `-TonProtokoll` pruefen); HDA-Karte = gar kein Ton. Klang auf echter Hardware pruefen |
| 24b | Ton-Schalter am Emulator wirkungslos | Erst `Start-Fold8.ps1 -TonProtokoll` und nachsehen, ob der Schalter in der QEMU-Zeile steht — sonst aendert man nichts |
| 24c | Schwarzer Emulator-Bildschirm nach Soundkarten-Wechsel | Schnappschuss passt nicht zur neuen Bestueckung → Kaltstart (wird bei `-TonGeraet` erzwungen); sonst zuerst auf Bildschirmsperre pruefen |
| 18b | Fenster startet oben aus dem Bild (y negativ), Titelleiste nicht greifbar | Kette umgangen — `emulator.exe -avd` direkt statt `Start-Fold8.ps1`; Position war falsch, Größe war es nicht → `emulator-start-guard`-Hook blockt den nackten Start |
| 18c | Fenster vertikal mittig, horizontal daneben | Nach **Soll**maß zentriert; der Emulator zieht das Fenster verzögert auf sein Seitenverhältnis → warten bis die Größe stabil ist, dann nach **Ist**maß nur die Position setzen |
| 19 | "App viel zu groß" trotz korrekter Rechnung | Innendisplay (7,6″, 11,63 cm) mit Cover-Display (5,5″, 7,47 cm) verglichen → Emulator dem echten Gerät folgen lassen |
| 20 | Maßstab falsch auf zweitem Bildschirm | `PrimaryScreen` statt `Screen::FromHandle` → Monitor unter dem Fenster ermitteln |
| 21 | "App abgestürzt", läuft aber (Bluetooth-Zeile) | `logcat -b crash` sammelt **alle** Prozesse → Absturz nur zählen, wenn eine Zeile den eigenen Paketnamen nennt; auf den Prozess pollen statt fester 5 s |
| 22 | Nach dem Drehen falsche Größe, springt, wird nie wieder richtig | Größe war Einmal-Aktion → Wächter (`-Ueberwachen`) rechnet aus `cur=` neu; Mutex (nur einer!) + selbstlösende Ping-Pong-Bremse |
| 22b | "Rotation geht gar nicht" | `accelerometer_rotation 0` macht den Dreh-Knopf wirkungslos → auf 1 lassen, Hochformat über `emu sensor set acceleration 0:9.81:0` |
| 22c | Eine von vier Lagen bleibt falsch (500×790) | 180°: Emulator-Rahmen hochkant, Android quer → Lage per `emu rotate` überspringen |
| 23 | Startet aufgeklappt statt zugeklappt | Standard ist `Fold8_Cover`; läuft die falsche AVD, wird gewechselt; im Betrieb `Klappen.ps1 -Auf`/`-Zu` |
