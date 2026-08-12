# Android-Emulator für Foldables (Galaxy Z Fold 8) — Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf die konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann den ENTSCHEIDENDEN Abschnitt im VOLLTEXT lesen
> (`emulator-foldable.md`), nicht nur diese Kurzfassung.

**Stand:** 12.08.2026 · Emulator 36.6.11.0 · Android 17 / API 37 · Gerät SM-F971B (One UI 9.0)

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Fold-AVD anlegen | Basis `-d "7.6in Foldable"`, danach `config.ini` auf echte Gerätewerte umschreiben | §2 |
| 2 | Displaywerte ermitteln | NIE schätzen — am echten Gerät `dumpsys display` lesen (liefert beide Panels + dpi + Hz) | §1 |
| 3 | Faltlinie setzen | `hw.sensor.hinge.areas = <halbe Breite>-0-1-<Höhe>`, Format x-y-Breite-Höhe | §2.2 |
| 4 | Postures | `posture_list = 1,2,3` (1=zu, 2=halb, 3=offen), Winkel via `hinge_angles_posture_definitions` | §2.2 |
| 5 | Klappen per Befehl | `adb emu sensor set hinge-angle0 0` bzw. `180`; `dumpsys device_state` prüft das Ergebnis | §2.3 |
| 6 | Cover-Display testen | `displayRegion` schneidet nur aus, Layout wird NICHT umgerechnet → **zweite AVD** anlegen | §2.4 |
| 7 | Maximale Grafiktreue | `hw.gpu.enabled=yes` + `hw.gpu.mode=host`; Modi: auto/host/software/lavapipe/swiftshader/swangle | §3.1 |
| 8 | Bildrate | `hw.lcd.vsync=120` setzen, sonst 60 — Animationsurteil sonst wertlos | §3.2 |
| 9 | Page Size | Gerät prüfen mit `getconf PAGESIZE`; 4096 → **kein** `ps16k`-Image nehmen | §1.3 |
| 10 | Schriftgröße | `settings get system font_scale` am Gerät lesen und im Emulator setzen | §1.3 |
| 11 | One UI erwarten | Gibt es NICHT als Image; Samsung-Skin liefert nur Rahmen, ausdrücklich "no One UI feature" | §4 |
| 12 | Echte Treue nötig | scrcpy spiegelt das echte Gerät — einzige 100-Prozent-Lösung | §5 |
| 13 | Originalgröße am Monitor | `-scale` ist seit Emulator 2.0 tot → Fenstergröße setzen, Faktor = Monitor-ppi / Geräte-ppi | §6 |
| 14 | Änderung sofort sehen | Live Edit (nur Funktionsrümpfe, API 30+); Compose Hot Reload ist **Desktop-only** | §7 |
| 15 | Element → Quellcode | Layout Inspector: "Toggle Deep Inspect", Klick wählt, **Doppelklick** springt in den Code | §7.2 |
| 16 | Alternative Emulatoren | Keiner kann Foldables; WSA seit 03/2025 eingestellt — der offizielle AVD ist alternativlos | §8 |
