# Kurzcheck: Bugs DLSS 5 Mod inoffiziell (Stand 21.09.2026 16:26, Treiber 616.92)
- M1 Fake-Repos/Repacks mit Malware → nur Original-Releases, Hash + Signatur + Defender.
- M2 renodx-dlss5 4.6/4.7 auf Treiber ≥ 616.64: 0/300 → OptiScaler-DLSSNR, DFC oder classic renodx.
- M3 `0xBAD00001` NGX-Init im Spielprozess (Feeder #47, offen). M4 unsignierte Modell-DLL → `0xBAD00002` → Authenticode-Pflicht.
- M5 165-MB-`nvngx_dlssd.dll` = falsch benanntes Modell, zerstört Ray Reconstruction. M6 zwei Neural-Consumer = stilles Nichts.
- M7 Swapper-Deinstallation lässt Dateien liegen → erst „Restore originals“. M8 Anti-Cheat-Bann. M9 Defender vs. DFC. M10 Smooth Motion aus. M11 AC-Engine verliert Tiefe (nur ReShade-Route).
- M12 CoD MW2: Absturz `0xc0000005` in `ntdll.dll` 1 s nach dem OptiScaler-Start (Manipulationsschutz) → Mod raus, CoD bleibt gesperrt.
- M13 Cyberpunk: CET/RED4ext-Mods brechen mit `dxgi.dll`-OptiScaler. M14 Belichtungs-Scan sieht Cyberpunk nicht → HDR-Paper-White manuell. M15 Model Resolution < 1,0 flimmert.
Volltext: `dlss5-mod-inoffiziell.md`
