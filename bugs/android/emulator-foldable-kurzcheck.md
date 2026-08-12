# Android-Emulator für Foldables — Bug-Kurzcheck

> **Stufe A.** Trifft ein Punkt zu oder tritt der Fehler auf: den Abschnitt im Volltext lesen
> (`emulator-foldable.md`, gleiche Nummer).

**Stand:** 12.08.2026 · Emulator 36.6.11.0 · Android 17/API 37 · Referenz SM-F971B

| # | Symptom | Kern in einem Satz |
|---|---------|--------------------|
| 1 | Zugeklappt sind Inhalte abgeschnitten | `displayRegion` schneidet nur aus, Layout wird nicht umgerechnet → zweite AVD fürs Cover |
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
| 12 | Falsches System-Image | `getconf PAGESIZE` prüfen: 4096 → **kein** `ps16k`-Image |
| 13 | Schrift größer als am Gerät | `font_scale` des Geräts auslesen und im Emulator setzen |
| 14 | `uiautomator dump` fast leer | Compose ohne `testTag`/`contentDescription` → Layout Inspector nehmen |
| 15 | Recherche liefert 0 Quellen | Firecrawl-Query zu lang → kurze Stichwort-Queries |
