# DLSS 5 Mod (inoffiziell: NVIDIA-Neural-Rendering in fremden Spielen) — Best Practices

> Stand: 14.10.2026 14:40 (Abschnitt 4b RenoDX DLSS), vorher 21.09.2026 16:26 · Versions-Anker: NVIDIA-Treiber 616.92, RTX 5090, OptiScaler-DLSSNR v0.2.0 (03.09.2026), `nvngx_dlssnr.dll` 310.8.0, DLSS5-Feeder 1.16.0-beta.6, DLSS5 Swapper 2.2.7, ReShade 6.8.0 · Anlass: YouTube-„DLSS 5“-Videos nachbauen
> Gegenstück: `bugs/gaming/dlss5-mod-inoffiziell.md` · Kurzcheck: `dlss5-mod-inoffiziell-kurzcheck.md` · Werkzeug: `Werkzeuge/dlss5-mod/` · Nachbarbereich: `nvidia-dlss-override.md` · **Einstieg mit Stand je Spiel und Fahrplan: `Recherchen/dlss5-grafik/README.md`**

## 1. Was der „DLSS 5 Mod“ wirklich ist
- Kein Filter, sondern **NVIDIAs echtes DLSS-5-Modell** `nvngx_dlssnr.dll` (NGX-Feature 18, Dateibeschreibung „NVIDIA DLSSNR“, ca. 160 MB, nur RTX 50). Es stammt aus dem Early-Access-Build von NBA 2K27 und wird per Hook in fremde Spiele eingeschleust. `extern` https://www.heise.de/en/background/DLSS-5-mod-tried-out-Why-all-the-fuss-11434544.html
- Offizielles DLSS 5 gibt es seit 03.09.2026 nur in NBA 2K27. Angekündigt sind u. a. Starfield, Assassin's Creed Shadows und Delta Force, noch ohne Termin. Erkennungsmerkmal im Spielordner: `nvngx_dlssnr.dll`. `offiziell` https://nvidianews.nvidia.com/news/nvidia-dlss-5-delivers-ai-powered-breakthrough-in-visual-fidelity-for-games
- Per NVIDIA App oder Profile Inspector nicht erzwingbar. Der Treiber kennt zwar `DLSS-NR - Enable DLL Override` (0x10E41E04), das wirkt aber nur in Spielen mit Integration, und der NGX-Cache (`C:\ProgramData\NVIDIA\NGX\models`) enthält kein NR-Modell. Nicht setzen.

## 2. Welcher Weg für welches Spiel
| Spiel | Weg |
|-------|-----|
| hat DLSS, FSR oder XeSS; DX12, DX11 oder Vulkan; 64 Bit | **OptiScaler-DLSSNR** (Dagherbou): OptiScaler als `dxgi.dll` + Forwarder `nvngx.dll_dlssnr.dll` + `nvngx_dlssnr.dll` in den Exe-Ordner. Keine ReShade nötig. `extern` https://github.com/Dagherbou/OptiScaler_DLSSNR/releases |
| kein Upscaler, 32 Bit oder Vulkan ohne DLSS | **DLSS5-Feeder** (jlrouzies-fr) + ReShade 6.8 Add-on-Edition + ein Neural-Consumer. `extern` https://github.com/jlrouzies-fr/DLSS5-Feeder |
| Oberfläche gewünscht | **DLSS5 Swapper** (rakanki911), bündelt alle Routen. Kein Silent-Install. `extern` https://github.com/rakanki911/DLSS5-Swapper |
| hat DLSS (Standard seit 14.10.2026 auf diesem PC) | **ShortFuse „RenoDX DLSS“** (`renodx-dlss.addon64`) + ReShade 6.8.0 Add-on-Edition als `dxgi.dll` + `nvngx_dlssnr.dll`. Hook „Upscaled“ nutzt die echten DLSS-Eingaben des Spiels. Abschnitt 4b. |
- ShortFuses `renodx-dlss` steht **nicht** auf GitHub (nur Discord), wird aber mit SHA-256-Digest im Spiegel `RankFTW/rhi-repo` (Tags `renodx-dlss-SF-*`) geführt (Bug M20). Deep Fried Chicken gibt es nur über Discord. Der 616.64-Absturz (Bug M2) betrifft nur Krishs `renodx-dlss5` 4.6/4.7, nicht ShortFuses `renodx-dlss`.

## 3. Bezug und Prüfung (Pflicht vor jeder Installation)
- Nur die Original-Releases laden (Tabelle oben, ReShade nur von reshade.me). **imod.gg meiden**: nirgends Bewertungen oder Spuren. `extern` Research 21.09.2026
- SHA-256 gegen das `digest`-Feld der GitHub-API prüfen (`/repos/<owner>/<repo>/releases`), nicht gegen Zahlen aus Artikeln.
- **`nvngx_dlssnr.dll` nur mit gültiger NVIDIA-Signatur:** `Get-AuthenticodeSignature` → `Valid` und `O=NVIDIA Corporation`. Sonst nicht installieren (Bug M4). Der Mirror `github.com/RankFTW/rhi-repo`, Tag `dlssnr-310.8.0`, SHA-256 `388c0a79…fb3ac`, war am 21.09.2026 echt NVIDIA-signiert.
- Unsignierte Community-DLLs (`OptiScaler.dll`, Forwarder) mit Defender prüfen: `MpCmdRun.exe -Scan -ScanType 3 -File <ordner>`. Defender-Ausnahmen nie ungefragt setzen.
- **Keine dieser Binärdateien ins öffentliche Repo.** Die Modell-DLL ist NVIDIA-Eigentum ohne Weitergabelizenz. Nur Skripte committen, Downloads nach `%LOCALAPPDATA%\DLSS5-Mod\downloads`.

## 4. OptiScaler-DLSSNR installieren (verifiziert 21.09.2026, 5 Spiele)
- Ziel ist der Ordner der Spiel-Exe, nicht die Wurzel: Cyberpunk `bin\x64`, Unreal-Spiele `<Projekt>\Binaries\Win64`.
- `setup_windows.bat` macht bei NVIDIA nur eins: `OptiScaler.dll` → `dxgi.dll` umbenennen und `Remove_OptiScaler.bat` schreiben. Das lässt sich ohne Rückfragen nachbauen. Vorher prüfen, dass keine fremde `dxgi.dll` (z. B. ReShade) liegt.
- In `OptiScaler.ini` im Abschnitt `[DlssNr]` `Enabled=true` setzen, dann ist kein Overlay-Klick nötig. Standard ist aus.
- Im Spiel DLSS oder DLAA wählen. `Einfg` öffnet das Overlay → „DLSS Neural Rendering“: *Detail strength* (Wirkung, 0 = aus), *Colour strength* (0 = Spielfarben, 1 = Modellfarben), *Model resolution* (Leistung), Debug-Ansicht „Difference“ (grau = Modell tut nichts). Die Meldung unter der Checkbox nennt den Grund, falls es nicht läuft.
- Kosten laut Community ca. 45–50 % FPS. `extern` https://heldgames.com/guides/dlss-5-mod-risks
- **FPS zurückholen (ergänzt 21.09.2026 16:35):** Die spieleigene Frame Generation bzw. MFG im Spielmenü einschalten. Der Neural Pass läuft einmal pro gerendertem Bild, die erzeugten Bilder übernehmen das Ergebnis. OptiScaler-FG (`FGInput=upscaler`, `FGOutput=dlssg`) nur für Spiele ohne eigene FG. Bei HUD-Fehlern im Overlay „HUD Fix“ einschalten. `extern` https://www.nexusmods.com/site/mods/2224
- **Die NVIDIA-Regler im Abschnitt `[DlssNr]`:** `Intensity` (Gesamtstärke), `LocalTone` (lokaler Kontrast), `LocalStructure` (Mikrodetail, erst moderat), `SkinStructure` (Hautdetail, -1 = wie LocalStructure), `AutoMask` (Haut automatisch erkennen). `Preset`/`Style` wirken nur, wenn die DLL mehrere Netze enthält. Änderungen bauen das Modell kurz neu auf.
- Nexus-Guide 2224 (velasquez3589, Stand 05.09.2026) beschreibt dieselbe OptiScaler-DLSSNR-Route plus eine ReShade/RenoDX-Route und lädt eigene Kopien der NVIDIA-DLLs hoch. Nie beide Routen zugleich (Bug M6), DLLs von dort nur nach Signaturprüfung.

## 4a. Cyberpunk 2077 (2.31) — verifiziert 21.09.2026 18:34
- **Läuft:** `OptiScaler.log` meldet je Bild `DlssNr_Dx12::Dispatch DLSS-NR cost: ~7,7 ms` (7,5 ms Modell + 0,2 ms Mod), zusammen mit Ray Reconstruction (DLSSD) und Streamline. Das ist der schnellste Beweis, dass der Pass arbeitet.
- **Spiel-Grundeinstellung:** Path Tracing + Ray Reconstruction **an** (ohne RR „boilt“ das Bild), DLSS Quality als Eingang (die Kosten des Passes hängen an der Ausgabe-, nicht an der Renderauflösung; DLAA nur bei FPS-Reserve), DLSS-Preset K statt L erzwingen (L stört das RT-Denoising), MFG 3x–4x (Reflex geht automatisch an), Bewegungsunschärfe/Filmkörnung/chromatische Aberration aus, SDR. `extern` https://en.gamegpu.com/news/zhelezo/dlss-4-5-ili-ray-reconstruction-v-cyberpunk-2077
- **Regler gegen Flimmern (Community, kein offizielles Preset):** `TransferStrength` ≈ 0,85, `MaxRatio` ≈ 1,8, `Intensity` < 1,5, `ColourStrength` bei 1,0, `WorkingScale` nicht unter 1,0 (in Cyberpunk flimmert es bei ~70 % beim Schwenken). Wirken Gesichter wächsern: `SkinStructure` unter `LocalStructure` setzen. Im Overlay den Proxy-Modus „Hybrid proxy + composed“ nehmen, nicht „Replace“ (Neon flackert). `extern` https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17
- **HDR:** Der automatische Belichtungs-Scan sieht Cyberpunks Belichtung nicht. In HDR den Paper-White-Regler (`WhitePointScale`) von Hand kalibrieren, sonst SDR nutzen.
- **Mods:** Reine Textur-Mods (HD Reworked, ETO, Haut-Texturen) sind problemlos. Mods über CET/RED4ext/RedScript (Ultra+, Nova City, DreamPunk, LUT Switcher) brechen mit OptiScaler als `dxgi.dll` (offenes Issue). ReShade-Presets kollidieren über `dxgi.dll` und müssten als `d3d12.dll` laufen. Fertige Cyberpunk-DLSS-5-Presets gibt es nicht. Nexus 33280 und das GitHub-Konto „rrdlss5renoxdx“ sind unseriös.

## 5. Anti-Cheat: harte Grenze
- **Nie** in Spielen mit Kernel-Anti-Cheat: Battlefield 6 (EA Javelin), Call of Duty inkl. Kampagne (Ricochet), Delta Force (ACE), PUBG (BattlEye blockiert den Start). Kein Hersteller erlaubt die ReShade-Add-on-Edition, Feeder und Swapper warnen selbst vor Bans. `extern` https://help.ea.com/en/articles/battlefield/battlefield-6/play-by-the-rules/ · https://github.com/rakanki911/DLSS5-Swapper
- Schutz über eine **feste Spieleliste**, nicht nur über Dateisuche: Javelin und ACE liegen als Systemtreiber außerhalb des Spielordners, eine Ordnersuche findet sie nicht zuverlässig (bei Delta Force findet sie nichts).

## 6. Zusammenspiel mit dem Treiber-Override
- Der Mod hängt nicht am Treiber-Override. OptiScaler fängt die DLSS-Aufrufe im Spiel ab, egal welche DLSS-Version dahinter steckt.
- Treiber-Overrides über Profile Inspector oder NVAPI wirken bei Treiber 616.92 vermutlich nur mit den „Override Reserved Keys“ der NVIDIA App (siehe `bugs/gaming/nvidia-dlss-override.md` B9). Prüfen mit `Werkzeuge/dlss-override/werte-lesen.ps1`.
- Smooth Motion aus lassen, sie kollidiert mit dem Neural Pass (Feeder #1).

## 7. Stärkste legale Hebel ohne Mod
- DLAA statt DLSS Quality plus Preset K/M, Ray Reconstruction und Path Tracing, wo vorhanden. Multi Frame Generation bringt nur FPS, kein schöneres Bild, und nur mit Reflex nutzen. `extern` https://www.techpowerup.com/review/nvidia-dlss-4-5-ray-reconstruction/2.html
- Filter der NVIDIA App (Freestyle, RTX HDR, Dynamic Vibrance) werden in Anti-Cheat-Spielen abgeschaltet, sie bergen kein Bann-Risiko.

## 4b. ShortFuse „RenoDX DLSS“ (Stand 14.10.2026 14:40, installiert in 5 Spielen, ungetestet im Spiel)
- **Was:** ReShade-Add-on von ShortFuse (RenoDX-Autor), Panel „RenoDX DLSS“, Links Discord/GitHub/„ShortFuse's Ko-Fi“. Neuestes Build **SF-26.1003.2350** (04.10.2026, SHA-256 `34ef92d6…7f0f`); das Build 2026-09-04 ist veraltet (Bug M16). `extern` https://github.com/RankFTW/rhi-repo/releases/tag/renodx-dlss-SF-26.1003.2350
- **Dateien neben der Spiel-Exe:** `dxgi.dll` (= `ReShade64.dll` 6.8.0.2155 aus `ReShade_Setup_6.8.0_Addon.exe`, nur die Add-on-Edition lädt Add-ons), `renodx-dlss.addon64`, `nvngx_dlssnr.dll` 310.8.0 (NVIDIA-signiert, auf RTX 50 ohne Patch). Nur DX11/DX12, 64 Bit. `extern` https://dlss5mod.com/guides/install · https://github.com/clshortfuse/renodx/issues/668 (RTX 5090 + 616.92 läuft)
- **Ohne Klicks installieren:** `Werkzeuge/dlss5-mod/renodx-installieren.ps1`. Der ReShade-Installer ist eine Exe mit angehängtem ZIP: `Expand-Archive` scheitert still, `tar -xf setup.exe ReShade64.dll` (Windows-bsdtar) klappt. `ReShade.ini` mit `[OVERLAY] TutorialProgress=4` überspringt das Tutorial.
- **Bedienung:** `Pos1` öffnet ReShade → Reiter „Add-ons“ → „RenoDX DLSS“. Läuft, wenn „DLSSNR: RUNNING“ steht und der Zähler „Successful NR frames“ steigt.
- **Startwerte:** Im Spiel DLSS oder DLAA an. Hook Method **Upscaled** (Present nur als Notweg für Spiele ohne DLSS oder bei Absturz, M17). Overall Intensity 0,3–0,5 (über 0,4 wird es wachsig), Model A/B/C = Stil Default/Natural/Cinematic (C am fotorealistischsten), Structure 0,1–0,25, Character Mask an, Skin Structure unter Structure, UI Correction an, Pass Count 1 (jeder Pass kostet voll). Encoding Auto; Diffuse White nur im HDR auf das Paper White des Spiels (Default 203). `extern` https://github.com/RianGoossens/reshade-dlss5-linux · https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/
- **Kosten:** Cyberpunk RTX 5090 4K ca. 63 → 42 FPS. Frame Generation kann kollidieren (M18), RTSS/Afterburner und Smooth Motion aus, wenn es abstürzt.
- **Zurück zu OptiScaler-DLSSNR:** `entfernen.ps1 -Spiel <Ordnerteil>` + `installieren.ps1 -Spiel '<Name>'`. Beide Skripte schreiben `dlss5-mod-dateien.txt`, `renodx-installieren.ps1` entfernt eine vorhandene OptiScaler-Installation selbst.

## 8. Rückbau
- `Werkzeuge/dlss5-mod/entfernen.ps1` (liest `dlss5-mod-dateien.txt` je Spiel) oder `Remove_OptiScaler.bat`. Beim Swapper vor dem Deinstallieren „Restore originals“ ausführen, sonst bleiben die Dateien in den Spielordnern (Bug M7).
