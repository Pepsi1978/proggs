# DLSS 5 Mod (inoffiziell) für Singleplayer-Spiele

> Gesamtstand, beste Einstellungen und Optimierungs-Fahrplan: `Recherchen/dlss5-grafik/README.md`

Rüstet NVIDIAs DLSS-5-Neural-Rendering-Modell (`nvngx_dlssnr.dll`) in Spiele nach, die selbst nur DLSS 2–4 mitbringen. Weg: **OptiScaler-DLSSNR** (Fork von Dagherbou, v0.2.0) fängt die DLSS-Aufrufe des Spiels ab und schickt das Bild danach durch das NVIDIA-Modell, vor dem HUD. Nur RTX 50, Treiber ab 616.56.

**Nicht offiziell.** Die Modell-DLL ist echte NVIDIA-Software (Authenticode „NVIDIA Corporation“), wird aber ohne Lizenz über einen GitHub-Mirror (`RankFTW/rhi-repo`) verteilt. Deshalb liegen **keine Binärdateien in diesem Repo**: Das Skript lädt sie nach `%LOCALAPPDATA%\DLSS5-Mod\downloads`.

| Datei | Zweck |
|-------|-------|
| `installieren.ps1` | Lädt beide Pakete, prüft SHA-256 (fest gepinnt) und die NVIDIA-Signatur des Modells, kopiert alles in den Exe-Ordner jedes Spiels (`OptiScaler.dll` → `dxgi.dll`), schaltet `[DlssNr] Enabled=true` ein und schreibt `dlss5-mod-dateien.txt` als Dateiliste. Blockiert Spiele mit Kernel-Anti-Cheat, laufende Spiele und fremde `dxgi.dll`. `-Spiel 'Name'` für ein einzelnes Spiel. |
| `renodx-installieren.ps1` | **Aktuelle Route (seit 14.10.2026):** ShortFuses „RenoDX DLSS“-Add-on. Lädt ReShade 6.8.0 Add-on-Edition (reshade.me), `renodx-dlss.addon64` SF-26.1003.2350 (Spiegel `RankFTW/rhi-repo`) und das Modell, prüft SHA-256 + NVIDIA-Signatur, entfernt eine vorhandene OptiScaler-Installation und legt `dxgi.dll` (= ReShade64.dll), Add-on, Modell und `ReShade.ini` neben die Exe. Gleiche Schutzregeln wie unten. |
| `entfernen.ps1` | Löscht anhand der Dateiliste alles wieder (`-Spiel` optional, Teil des Ordnernamens). Gilt für beide Routen. |

**Stand 14.10.2026: RenoDX DLSS installiert** in Cyberpunk 2077, Starfield, Manor Lords, Star Trek Voyager, AC Shadows (OptiScaler-DLSSNR dort ersetzt). Im Spiel: DLSS/DLAA an, `Pos1` → Add-ons → „RenoDX DLSS“, Hook Method „Upscaled“, Intensity 0,3–0,5, Character Mask an, Pass Count 1. Läuft, wenn „DLSSNR: RUNNING“ steht. Zurück zu OptiScaler: `entfernen.ps1` + `installieren.ps1`.

OptiScaler-Route installiert (21.09.2026, bis 14.10.2026): Cyberpunk 2077 (`bin\x64`), Starfield, Manor Lords (`ManorLords\Binaries\Win64`), Star Trek Voyager (`STVoyager\Binaries\Win64`), AC Shadows.
**Nie** im Standardlauf: Battlefield 6 (Javelin), CoD BO7/MW3 (Ricochet), Delta Force (ACE), PUBG (BattlEye): Bann-Risiko.
**MW2-Versuch (21.09.2026, auf ausdrücklichen Wunsch, Zweitaccount):** `cod22-cod.exe` stürzt ca. 1 s nach dem OptiScaler-Start ab (`0xc0000005` in `ntdll.dll`), der Manipulationsschutz von CoD verhindert den Mod. Wieder entfernt. CoD ist damit technisch **und** wegen des Bann-Risikos ausgeschlossen.

Im Spiel: DLSS (oder DLAA) als Upscaler wählen. `Einfg` öffnet das OptiScaler-Menü → „DLSS Neural Rendering“: Detail strength (Wirkung), Colour strength (Farbe des Modells), Model resolution (Leistung). Die Debug-Ansicht „Difference“ zeigt, ob das Modell arbeitet (grau = nichts). Kosten laut Community ca. 40–50 % FPS.
Bei Problemen: zuerst im Menü die Meldung unter der Checkbox lesen, `OptiScaler.log` im Spielordner. Hintergrund: `best-practices/gaming/dlss5-mod-inoffiziell.md`, `bugs/gaming/dlss5-mod-inoffiziell.md`.
