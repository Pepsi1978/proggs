# Kurzcheck: Bugs DLSS 5 Mod inoffiziell (Stand 21.09.2026 16:26, Treiber 616.92)
- M1 Fake-Repos/Repacks mit Malware → nur Original-Releases, Hash + Signatur + Defender.
- M2 renodx-dlss5 4.6/4.7 auf Treiber ≥ 616.64: 0/300 → OptiScaler-DLSSNR, DFC oder classic renodx.
- M3 `0xBAD00001` NGX-Init im Spielprozess (Feeder #47, offen). M4 unsignierte Modell-DLL → `0xBAD00002` → Authenticode-Pflicht.
- M5 165-MB-`nvngx_dlssd.dll` = falsch benanntes Modell, zerstört Ray Reconstruction. M6 zwei Neural-Consumer = stilles Nichts.
- M7 Swapper-Deinstallation lässt Dateien liegen → erst „Restore originals“. M8 Anti-Cheat-Bann. M9 Defender vs. DFC. M10 Smooth Motion aus. M11 AC-Engine verliert Tiefe (nur ReShade-Route).
Volltext: `dlss5-mod-inoffiziell.md`
