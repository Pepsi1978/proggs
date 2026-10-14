# R2 - Installation RenoDX DLSS (ShortFuse), Stand Recherche 14.10.2026

WICHTIG: Es gibt KEINE offizielle Installationsanleitung von ShortFuse. README/Wiki von clshortfuse/renodx (https://github.com/clshortfuse/renodx) erwähnt das DLSS-Addon nicht; renodx.com (https://renodx.com/) ebenfalls nicht (nur Verweis auf Mod-Liste + Discord https://discord.com/invite/renodx). Alles unten stammt aus Community-Guides und GitHub-Issues. Nicht verifizierbar (Discord nicht lesbar): Pins in #dlss5.

## 1. ReShade
- Aktuelle Version: 6.8.0, veröffentlicht 02.08.2026; Add-on-Edition "with full add-on support": /downloads/ReShade_Setup_6.8.0_Addon.exe auf https://reshade.me . "add-on support is only enabled in the ReShade build with full add-on support, which is unsigned." (https://reshade.me)
- Standardbuild lädt keine Add-ons (https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/ ; https://dlss5mod.com/guides/install).
- Reale Nutzer-Logs: ReShade "6.8.0.2155 (64-bit)" als dxgi.dll (https://github.com/clshortfuse/renodx/issues/668 ; /issues/651 ; /issues/697). => 6.8.0 ist die belegte Mindest-/Arbeitsversion. Eine ältere Version (6.0+) nennt nur ein einzelner Fremd-Guide (https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/issues/87) - nicht belastbar.
- Installer: Spiel-EXE wählen (nicht Launcher), Ordner z.B. bin\x64, Binaries\Win64; API wählen: DX12 für DX12-Spiele, DX11 für DX11 (igorslab). ShortFuse-Build unterstützt nur 64-bit DX9/DX11/DX12, kein Vulkan, OpenGL, 32-bit-DX9 (https://dlss5mod.com/guides/install ; Issue #687 32-bit DX9 funktioniert nicht, https://github.com/clshortfuse/renodx/issues).
- DLL-Name: ReShade als dxgi.dll (DX12/DX11-Standard) - Belege: Issue #668 (ReShade "as dxgi.dll"), https://github.com/faisalkindi/DLSS5oneclick (ReShade als dxgi.dll; Fallback version.dll/winmm.dll/dinput8.dll bei Konflikten; D3D9.dll bei dgVoodoo; ReShade64.dll bei OptiScaler-Kombi). Linux-Fork: dxgi für D3D12, d3d11 für D3D11, Vulkan = ReShade64.dll+json (https://github.com/Endebert/reshade-dlss5-linux). d3d12.dll wird in keiner Quelle für Windows genannt.

## 2. Dateien und Ablageort (alles in den Ordner der Spiel-EXE)
- renodx-dlss.addon64 (ShortFuse, "current ShortFuse tool"), neben ReShade.ini / Spiel-EXE (https://dlss5mod.com/guides/install).
- renodx-dlss5.addon64 ist ein ANDERER, älterer Branch (Krish/Community), NICHT gleichzeitig laden; entfernen oder umbenennen z.B. .addon64x (https://dlss5mod.com/guides/install ; Issue #651 "older, no longer updated"). Mehrere NR-Addons gleichzeitig vermeiden (igorslab).
- nvngx_dlssnr.dll (~158 MB bzw. 165.840.496 Byte beim getesteten Build, https://github.com/Endebert/reshade-dlss5-linux) neben die Spiel-EXE.
- nvngx_dlss.dll (und je nach Spiel nvngx_dlssd.dll, sl.interposer.dll, sl.common.dll) - Streamline/DLSS-Set; bei Spielen ohne DLSS oft zusätzlich nötig: "the Feeder's NGX session fails without one next to the game" (https://github.com/faisalkindi/DLSS5oneclick). Reale Setups: nvngx_dlss.dll/dlssd 310.9.1.0, Streamline 2.14.1.0 (Issue #668; #697 "DLSS SR/RR/FG: 310.9.1").
- Die Dateien sind NICHT im GitHub-Repo von clshortfuse. Verteilung: nur RenoDX-Discord, #dlss5-Kanal, gepinnte Beiträge ("distributed only through the RenoDX Discord pinned posts"; Mirror "renodx-dlss-SF-0.x" im rhi-repo, laut Owner nur Mirror) (https://github.com/faisalkindi/DLSS5oneclick/issues/18). Download-Mirror-Quellen: RankFTW/rhi-repo-Releases (renodx-dlss-SF-*, dlssnr-*, dlss-*) (https://github.com/faisalkindi/DLSS5oneclick). nvngx_dlssnr.dll wird dort als "leak runtime" beschrieben (https://dlss5feeder.com/renodx) -> Quelle vor Nutzung prüfen.
- Hugging-Face-Spiegel der Runtimes (Ada-gepatcht): https://huggingface.co/Bandukids/DLSS-Runtimes (nur aus Suchtreffer, nicht geöffnet).

## 3. nvngx_dlssnr.dll Versionen
- Herkunft: NBA 2K27 Early-Access-Build, Windows zeigt "NVIDIA DLSSNR 310.8.0.0" (Suchzusammenfassung zu https://mezha.ua/en/news/nvidia-dlss-5-nba-2k27-control-gta-5-314621/ ; nicht geöffnet).
- Beleg mit exakt Nutzer-System: Issue #668 Report 1: RTX 5090, Treiber 616.92, ReShade 6.8.0.2155 (dxgi.dll), nvngx_dlssnr.dll 310.8.0 "Authenticode Valid", nvngx_dlss/dlssd 310.9.1.0, Streamline 2.14.1.0 (https://github.com/clshortfuse/renodx/issues/668). Report 2: RTX 5070, Treiber 617.14, renodx-dlss.addon64 v0.2026.922.310, dlssnr 310.8.0 (auch 310.8.3 probiert).
- Weitere Stände: 310.8.SF = ShortFuse-Repack mit Ada-/Turing-Pfaden (FP16 für RTX 20/30); Stock 310.8 läuft auf Ada und älter nicht (NVIDIA-Treiber liefert 0xBAD00001) (Suchzusammenfassung; https://github.com/faisalkindi/DLSS5oneclick: "adds patched binaries for RTX 40 and an FP16 path for RTX 20/30"). Nexus zeigt "310.8.2 (20/30/40/50)" (Suchtreffer https://www.nexusmods.com/stellarblade/mods/3706, 403 beim Abruf). Issue #666 (RTX 3090 Ti): 310.8.SF.0, Addon 0.55. Issue #643: 310.8.0, "Discord 4.55 build".
- Für RTX 50 (Nutzer): unmodifizierte Route, KEINE Patch-DLL nötig (https://dlss5mod.com/guides/install: "RTX 50 uses the unmodified route"; Ada/Ampere/Turing brauchen gepatchte DLL).
- Modell-Mismatch kann still fehlschlagen / später crashen: "A different model version may not fail cleanly" (https://github.com/Endebert/reshade-dlss5-linux).
- Model A/B/C: laut Linux-Fork "Style (Model A/B/C)" ein UI-Setting, nicht separate Dateien (https://github.com/Endebert/reshade-dlss5-linux). Keine separaten Model-Dateien belegt.

## 4. Treiber
- Kein konkreter Mindesttreiber genannt. Belegter funktionierender Treiber auf RTX 5090: 616.92 (Issue #668). RTX 5070 mit 617.14 ebenfalls. Linux-Fork: "a recent NVIDIA driver branch that supports DLSS Neural Rendering". NVIDIA-App-Overrides können die Route brechen (https://dlss5mod.com/guides/install). Treiber erzwingt Architektur-Sperre für Ada und älter (s.o.).

## 5. Aktivierung / Verifikation
- Start, Home/Pos1 -> ReShade Add-ons-Tab -> "RenoDX DLSS" (je Guide auch "DLSS 5 Neural Rendering") aktivieren. Status: "DLSSNR: RUNNING", Zähler "Successful NR frames" steigt, "Latest NR NGX result" 0x00000000 (igorslab; https://dlss5feeder.com/renodx). STANDBY/FAILED: "Reset NR feature and clear failure latch" (igorslab). In ReShade.log: Registrierung, Library-Laden, NGX-Feature-Create, Evaluate (dlss5mod). "WAITING FOR NGX MODULES": Runtime-Platzierung/API/doppelte Addons prüfen.
- Fallback: [ADDON] LoadFromDllMain=renodx-dlss.addon64 in ReShade.ini, "not a universal requirement" (dlss5mod). Bei TLOU2 brachte es nichts (Issue #666).
- Neural Rendering startet im RHI-Installer-Setup deaktiviert (StartDisabled=1) (https://dlss5swapper.org/renodx-dlss).
- Einstellungen werden in ReShade.ini gespeichert (Linux-Fork: [ADDON_DLSSNR_LINUX]; Windows-Keys z.B. DirectNeuralRenderingHookMethod, DirectNeuralRenderingHookStage, DirectNeuralRenderingRequireDlss, DirectNeuralRenderingReuseDlssUpscalingResources, Issue #668 / #648).

## 6. Braucht das Spiel eigenes DLSS ("Require DLSS")?
- Spiel mit DLSS: eigene Integration nutzen, kein Feeder/Bridge/2. Streamline-Paket, Frame Generation und Dynamic Resolution für ersten Test aus (dlss5mod).
- Spiel ohne DLSS: "Load DLSS Libraries" = On und Hook Method = Present; "less reliable" (dlss5mod). Dann "Require DLSS"/"Use DLSS Upscaling Resources" auf Never/Off, sonst Dauer-"Waiting": Issue #668 (PCSX2 D3D12, "Required ... can never be satisfied here" -> Never) und #651 (Require DLSS an => "waits for matching DLSS SR/AA/RR temporal inputs", hängt bei Waiting; aus => läuft mit Dummy-Inputs).

## 7. Hook-Methoden (Off/Auto/Upscaled/Present) - nur indirekt belegt
- Present: Backbuffer, "uses dummy temporal inputs" (Aussage des Reporters in Issue #651, nicht von ShortFuse). "Present is the only viable stage on a host with no DLSS" (#668).
- Upscaled: hängt am DLSS-Aufruf des Spiels (Hook von NGX CreateFeature/EvaluateFeature, nutzt Output/Depth/Motion Vectors des Spiels) - Linux-Fork beschreibt den Mechanismus (https://github.com/Endebert/reshade-dlss5-linux); "Upscaled: Active, native DLSS/DLAA output" im RE-Requiem-Guide (https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/issues/87: Options Mode DLSS-NR, Hook Method Upscaled, Model A, Require DLSS On, Pass Count 2, Structure 1.20, REFramework in Spielordner, Lilium-HDR-Shader aus; Nutzerguide, nicht offiziell).
- Render: laut DLSS5oneclick-Release "NRHookPoint=1 (Render) läuft vor dem DLSS-Upscaling, weniger Pixel; mit Ray Reconstruction automatisch zurück auf Upscaled" (https://newreleases.io/project/github/faisalkindi/DLSS5oneclick/release/v0.14.4).
- Eine offizielle Definition von ShortFuse existiert nicht (Issues ohne Maintainer-Antwort).
- Build-Version im Issue #651: "2026-09-04T08:48:41" (= Build des Nutzers vom 04.09.2026), meldet Version "0.0.0.0".
- Options Mode DLSS-G: "Reshade before Frame Generation"; Issue #648 ohne Maintainer-Antwort; "presentation evaluation requires one live swapchain per device".

## 8. Alternative Installer (Drittanbieter, nicht ShortFuse)
- RHI (RankFTW): Methode "DLSS Tool (ShortFuse)" schreibt HookStreamline=1 und HookDirectX=1 in reshade.ini für FrameGen (https://newreleases.io/project/github/RankFTW/RHI/release/RHI-2.6.0 ; https://www.nexusmods.com/site/mods/1710, 403).
- yumlevi/renodx-dlss-installer (RenoDX-DLSS-Setup.exe; installiert ReShade 6.8.0 + renodx-dlss5-v2.5.addon64 + streamline.zip; ACHTUNG: dlss5-Branch, nicht ShortFuse) (https://dlss5swapper.org/renodx-dlss).
- DLSS5oneclick (faisalkindi): Standard renodx-dlss5, Fallback ShortFuse renodx-dlss (https://github.com/faisalkindi/DLSS5oneclick).
- Warnung: Nur Offline/Singleplayer; Anticheat blockt Injektion (dlss5mod, dlss5swapper).

## Offene Lücken
- Offizielle ShortFuse-Doku/Discord-Pins nicht abrufbar. Exakte Hook-Method-Definitionen unbestätigt. Ob nvngx_dlssnr.dll 310.8.0 vs. 310.8.3 für Build 2026-09-04 vorgeschrieben ist: nicht belegt; 310.8.0 läuft nachweislich auf RTX 5090 + 616.92 (#668). 403 bei Nexus-Seiten.

BEST-PRACTICES-KANDIDATEN:
- ReShade 6.8.0 "Addon"-Edition (ReShade_Setup_6.8.0_Addon.exe, reshade.me) + renodx-dlss.addon64 neben Spiel-EXE; ReShade als dxgi.dll; nur EIN NR-Addon (renodx-dlss5.addon64 entfernen/umbenennen) - https://dlss5mod.com/guides/install , https://reshade.me
- RTX 5090: unmodifizierte nvngx_dlssnr.dll 310.8.0 reicht (Beleg Treiber 616.92) - https://github.com/clshortfuse/renodx/issues/668
- Spiele ohne DLSS: Hook Method Present + Require DLSS aus/Never; Spiele mit DLSS: Upscaled/Auto, FG zunächst aus - https://dlss5mod.com/guides/install , Issues #651/#668
- Vor Ersetzen Backup von nvngx_dlss*.dll, sl.*.dll, ReShade.ini, *.addon64 - https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/

BUG-KANDIDATEN:
- #697 MHWilds Absturz bei Hook Render/Upscaled (Present ok), v0.2026.1003.2333, ReShade 6.8.0.2155 - https://github.com/clshortfuse/renodx/issues/697
- #651/#668 Dauer-"Waiting"/idle bei Require DLSS ohne echtes DLSS (hooks 6/8, evaluate_c=0), Build 2026-09-04 / 2026-09-17 - https://github.com/clshortfuse/renodx/issues/651 , https://github.com/clshortfuse/renodx/issues/668
- #666 TLOU2 Render schwarzes Bild (Addon 0.55, dlssnr 310.8.SF.0) - https://github.com/clshortfuse/renodx/issues/666
- #643 Dying Light The Beast "output format 9 not supported" (dlssnr 310.8.0) - https://github.com/clshortfuse/renodx/issues/643
- #642/#648 Flackern/Swapchain-Ablehnung mit Frame Generation - https://github.com/clshortfuse/renodx/issues/648
- Stock-310.8 auf Ada/älter: 0xBAD00001 (nur für Nicht-RTX-50 relevant) - https://github.com/faisalkindi/DLSS5oneclick
- Opening ReShade UI senkt FPS; NVIDIA-App-Overrides brechen die Route; Auflösungs-/Modus-Wechsel brauchen Neustart - https://dlss5mod.com/guides/install
