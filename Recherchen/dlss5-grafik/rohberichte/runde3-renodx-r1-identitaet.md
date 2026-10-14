# R1 - Identität, Bezugsquelle, neueste Version (RenoDX DLSS, ShortFuse) - Stand 14.10.2026

## 1. Was ist das Add-on?
- Datei: `renodx-dlss.addon64` (ReShade-Add-on, 64 Bit). In ReShade unter Add-ons als Panel "RenoDX DLSS" (Quelle: https://github.com/faisalkindi/DLSS5oneclick README, "RenoDX DLSS for ShortFuse's add-on").
- Autor: ShortFuse (clshortfuse), Autor von RenoDX. Das Add-on bettet im Binary selbst ein: https://github.com/clshortfuse/renodx , https://github.com/clshortfuse/renodx/wiki/Mods , https://ko-fi.com/shortfuse , Discord-Invite F6AUTeWJHM (per Discord-API geprüft: Inviter = clshortfuse / "ShortFuse"; https://discord.gg/F6AUTeWJHM).
- Technik (aus den Strings des Binarys, selbst extrahiert): "Direct Neural Rendering" = DLSS-NR (nvngx_dlssnr, Feature Reserved18) über NGX/Streamline; Hook Method Render/Upscaled/Present; interne Parameter DLSSNR.Intensity, DLSSNR.LocalToneStrength, DLSSNR.LocalStructureStrength, DLSSNR.GlobalToneStrength, DLSSNR.UseAutoMask, DLSSNR.SkinStructureStrength, DLSSNR.Style, DLSSNR.UICorrection.
- Statuszeile "presentation backbuffer with dummy temporal inputs" = Present-Hook ohne echte DLSS-Eingaben (Binary-String: "Without either, On skips processing; Off allows dummy temporals"). Beschreibung auch in https://github.com/clshortfuse/renodx/issues/651
- Das Foto-Build "2026-09-04T08:48:41" ist exakt der Build-Zeitstempel, den Issue #651 (5.9.2026) für `renodx-dlss.addon64` nennt (Dateiversion 0.0.0.0): https://github.com/clshortfuse/renodx/issues/651

## 2. OFFIZIELLE Bezugsquelle - wichtige Erkenntnis
- Das Add-on ist NICHT in den GitHub-Releases von clshortfuse/renodx. Per GitHub-API geprüft, Tag `snapshot` (RenoDX Snapshot Build, 13.10.2026 23:41 UTC, Commit aa7c0ca, 545 Assets): enthält nur `renodx-dlssfix.addon32/.addon64/.pdb` (ein anderes Tool), KEIN `renodx-dlss.addon64`, KEIN `renodx-dlss5*`. Auch im Repo-Tree (src/addons) existiert nur `dlssfix`, kein DLSS-NR-Add-on-Quellcode (nur src/utils/dlss/*, dlss_hook.hpp). Die Download-URL https://github.com/clshortfuse/renodx/releases/download/snapshot/renodx-dlss.addon64 EXISTIERT NICHT (Dateiliste geprüft).
  - Quellen: https://github.com/clshortfuse/renodx/releases/tag/snapshot , https://github.com/clshortfuse/renodx/releases (Nightlies nightly-20261004 bis 20261014, je 532 Assets; Changelogs: cross-API GPU fences, command_action-Refactor, FFXIV, cmake - nichts zu DLSS-NR).
- Offizieller Verteilweg laut Guides: ShortFuses Discord ("Discord-only"). https://dlss5mod.com/guides/install nennt nur den Discord-Thread https://discord.com/channels/1408098019194310818/1543796222475767918/threads/1543975158937821315 und sagt, DLSS5Mod spiegele keine Discord-only Builds. Nexus-Stellar-Blade-Guide: renodx-dlss5 nicht auf GitHub, sondern im RenoDX-Discord: https://www.nexusmods.com/stellarblade/mods/3706
- Community-Spiegel (inoffiziell, aber versionierte Direktdownloads von ShortFuses Datei): GitHub RankFTW/rhi-repo, Tags `renodx-dlss-SF-*`: https://github.com/RankFTW/rhi-repo/releases . Der RHI-Installer (https://www.nexusmods.com/site/mods/1710, "ShortFuse DLSS Tool: swaps renodx-dlss.addon64"; Seite per WebFetch 403, nur Suchtreffer) und DLSS5oneclick ziehen daraus ("rhi-repo renodx-dlss-SF-*": https://github.com/faisalkindi/DLSS5oneclick).

## 3. NEUESTE Version (Stand 14.10.2026)
Neuester SF-Build im rhi-repo: **renodx-dlss-SF-26.1003.2350**, veröffentlicht 04.10.2026 07:59 UTC. Die enthaltene renodx-dlss.addon64 hat den eingebetteten Build-Zeitstempel **2026-10-03T23:50:12** (selbst aus der DLL gelesen), 3.010.048 Bytes, SHA256 1310119c87e4ab5ad0af41511411bd4608ff3c3030a29196ae0e4bba32c84585.
- Direkter Download (ZIP mit renodx-dlss.addon64; Download erfolgreich getestet): https://github.com/RankFTW/rhi-repo/releases/download/renodx-dlss-SF-26.1003.2350/renodx-dlss_SF_26.1003.2350.zip
- Release-Seite: https://github.com/RankFTW/rhi-repo/releases/tag/renodx-dlss-SF-26.1003.2350
- Seit 04.10.2026 kein neuerer SF-Build im rhi-repo (letzte Veröffentlichungen 13.10.: streamline-2.14.16.0, dlss/dlssd/dlssg-310.9.16; 08.10.: DLSS-Enabler-4.10.0.8). Ob ShortFuse im Discord Neueres verteilt hat, ist per Web nicht prüfbar.

SF-Build-Historie im rhi-repo (Tag / Veröffentlichung; DLL-Stempel wo selbst ermittelt):
- renodx-dlss-SF-0.54 - 08.09.2026 (DLL-Stempel 2026-09-07T17:23:53)
- renodx-dlss-SF-0.55 - 11.09.2026 (DLL-Stempel 2026-09-10T15:42:15)
- renodx-dlss-SF-26.0917.1145 / 26.0917.1436 / 26.0917.1904 - 17./18.09.
- renodx-dlss-SF-26.0919.2025 - 20.09.
- renodx-dlss-SF-26.0922.0041 - 23.09.
- renodx-dlss-SF-26.0927.2125 - 28.09.
- renodx-dlss-SF-26.0928.0205 - 29.09.
- renodx-dlss-SF-26.1003.2350 - 04.10.2026 (neuester)
Das Nutzer-Build (04.09.2026 08:48:41) ist ÄLTER als alle rhi-repo-SF-Builds (frühester, 0.54, hat Stempel 07.09.): mindestens 8 Builds und rund 1 Monat hinter dem neuesten.

Changelog: Weder rhi-repo-Releases (Body = nur Titel) noch GitHub-Releases liefern Changelog-Punkte zum DLSS-Add-on. Aus dem Vergleich der Binaries (Strings, selbst ermittelt, NICHT offiziell):
- Build 2026-09-07 (0.54): Hook Method mit Upscaled/Present (plus Auto); Stil-Auswahl "Model" vorhanden; "Require DLSS" (Present nutzt FG-Ressourcen wenn FG aktiv, sonst Upscaled; Off erlaubt Dummy-Temporals).
- Build 2026-10-03 (26.1003.2350): Hook Method hat Render / Upscaled / Present (Render = verarbeitet nativen D3D12-Input VOR DLSS SR/DLAA/RR); statt "Require DLSS" On/Off jetzt "Use DLSS Upscaling Resources" (Optional/Required/Never); neu: Resolution Scale, Scaling Domain (Signed PQ/Linear), Down-/Upsample Filter, Scale Reconstruction, NR Signal Encoding, Encoding-Liste (Linear BT.709, sRGB, HDR10, scRGB), Diffuse White (nits), Streamline-v2-Ordnungshinweis. Das Foto-Menü (Hook Method Off/Auto/Upscaled/Present, Require DLSS Off/On) entspricht dem ALTEN Menü; das neueste Build ist deutlich verändert.
- Issue #651 (offen, 5.9.2026): Require DLSS = On bleibt bei DLAA über DLSS-5-Bridge dauerhaft "Waiting": https://github.com/clshortfuse/renodx/issues/651 (keine Antwort von ShortFuse sichtbar).

## 4. Abgrenzung der Add-ons
- ShortFuse: `renodx-dlss.addon64`, Panel "RenoDX DLSS", eigener Hook auf NGX/Streamline-Aufrufe (Binary: "Present supports D3D9, D3D11, and D3D12"), für DX11 kein Bridge nötig (https://github.com/faisalkindi/DLSS5oneclick).
- `renodx-dlss5.addon64` (Panel "DLSS 5 Neural Rendering", ReShade.ini-Sektion [RenoDX.DLSS5]): laut DLSS5oneclick "leaked DLSS 5 add-on ... closed-source and community-distributed", Verteilung über rhi-repo (Tags renodx-dlss5-3.3.4 ... 4.5/4.55/4.60/4.70 [Aug/Sep], 5.2.1 [11.09.], 6.4.1/6.5.3 [18./19.09.], 7.0.0-rc1/rc8 [23.09.], 8.5.0-rc4/rc5/rc10 [26.09.]) und yumlevi/renodx-dlss-installer (Release "latest" 28.08.2026 mit renodx-dlss5.addon64 und renodx-dlss5-v2.5.addon64): https://github.com/RankFTW/rhi-repo/releases , https://github.com/yumlevi/renodx-dlss-installer/releases , https://github.com/faisalkindi/DLSS5oneclick . Issue #651 nennt als Autor "Krish" ("older, no longer updated"). Zu "Lecram": der Tag `dlssnr-310.8.Lecram` ist ein NR-Modell (nvngx_dlssnr_310.8.Lecram.zip, 116 MB), keine Add-on-Version (https://github.com/RankFTW/rhi-repo/releases/tag/dlssnr-310.8.Lecram). Die Zuordnung "renodx-dlss5 v4.6/4.7 = Lecram" ist NICHT bestätigt (4.60 und 4.70 existieren als Tags, Autor dort nicht genannt).
- Beide Add-ons nie gleichzeitig im Spielordner laden (Konflikt): https://dlss5mod.com/guides/install und Suchtreffer.
- "Deep Fried Chicken": in keiner gefundenen Quelle aufgetaucht; nicht verifiziert, keine Aussage möglich.
- ShortFuses generisches RenoDX (HDR/Tonemapping: `renodx-generic.addon64`, `renodx-unrealengine.addon64`, `renodx-<spiel>.addon64`) ist ein anderes Produkt und liegt im Snapshot-Release; DLSS-NR ist nicht Teil davon.

## 5. Model A / B / C
- Im Binary (Build 2026-09-07): "Selects Neural Rendering Model A, Model B, or Model C through the prerelease DLSSNR.Style field." Es ist der Parameter `DLSSNR.Style` des DLSS-NR-Features (3 Stilvarianten), NICHT die Wahl verschiedener nvngx_dlssnr.dll-Dateien/Versionen.
- NVIDIA-Hintergrund (Sekundärquelle, nur Suchzusammenfassung, Seite nicht abgerufen): drei Neural-Rendering-Netze A/B/C, unterscheiden sich im Bildstil, nicht in Frame Time; keine Qualitätsstufen: https://www.guru3d.com/story/nvidia-refines-dlss-5-neural-rendering-with-greater-developer-control-at-siggraph-2026/
- Modell-DLL separat: ShortFuses `nvngx_dlssnr.dll 310.8.SF` für RTX 20/30 (FP16) laut DLSS5oneclick-README und https://www.guru3d.com/story/leaked-dlss-5-runs-on-rtx-20-and-rtx-30-gpus/ ; Lecram-Variante: https://github.com/RankFTW/rhi-repo/releases/tag/dlssnr-310.8.Lecram

## 6. Weitere Hinweise
- Spiele ohne DLSS: Guide sagt "Load DLSS Libraries = On" und "Hook Method = On Present"; weniger zuverlässig; FG beim Test aus: https://dlss5mod.com/guides/install
- Plattform laut Binary/Guide: 64-Bit DX9/11/12 (kein Vulkan/OpenGL). ReShade 6.8.0 Full Add-on Support nötig (https://reshade.me/).
- Nicht prüfbar: Discord-Threads, Nexus-Seiten (403), YouTube "ShortFuse RenoDX/Menu version - New renodx-dlss.addon64 file for DLSS 5" https://www.youtube.com/watch?v=O--HwZ1oNWw (Inhalt nicht gesehen).

BEST-PRACTICES-KANDIDATEN:
- ShortFuse-DLSS-NR-Add-on `renodx-dlss.addon64` kommt aus dem RenoDX-Discord, nicht aus GitHub-Releases (snapshot enthält nur dlssfix); versionierter Spiegel RankFTW/rhi-repo `renodx-dlss-SF-*`, neuester 26.1003.2350 (Build 2026-10-03T23:50:12): https://github.com/RankFTW/rhi-repo/releases/tag/renodx-dlss-SF-26.1003.2350
- Nie renodx-dlss.addon64 und renodx-dlss5.addon64 gleichzeitig laden: https://dlss5mod.com/guides/install
- Build-Zeitstempel (Panel About > Build) zum Versionsabgleich nutzen, Dateiversion ist 0.0.0.0: https://github.com/clshortfuse/renodx/issues/651
- Spiele ohne DLSS: Hook Method = Present, Load DLSS Libraries = On: https://dlss5mod.com/guides/install

BUG-KANDIDATEN:
- renodx-dlss.addon64 Build 2026-09-04T08:48:41: "Require DLSS = On" bleibt bei DLAA über DLSS-5-Bridge dauerhaft "Waiting" (alle Hook-Methoden); Require DLSS = Off läuft nur mit Dummy-Temporals auf dem Backbuffer: https://github.com/clshortfuse/renodx/issues/651 (offen; Status in neueren Builds ungeprüft)
